/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.control.calculators;

import gecko.core.math.ClarkeTransform;
import gecko.core.math.ParkTransform;

/**
 * Field-Oriented Control (FOC) calculator for Permanent Magnet Synchronous Machines (PMSM).
 *
 * <p>Implements cascaded speed and direct/quadrature current loops using PI controllers,
 * transforming phase currents via Clarke and Park transformations, and producing voltage
 * references via inverse Park transformation.</p>
 *
 * <p><strong>Input Signals (12):</strong></p>
 * <ul>
 *   <li>Signal 0: {@code ia} - Stator phase A instantaneous current [A]</li>
 *   <li>Signal 1: {@code ib} - Stator phase B instantaneous current [A]</li>
 *   <li>Signal 2: {@code w} - Rotor mechanical angular speed [rad/s]</li>
 *   <li>Signal 3: {@code phi} - Rotor electrical angle [rad]</li>
 *   <li>Signal 4: {@code n_ref} - Reference speed [rpm]</li>
 *   <li>Signal 5: {@code Kp_n} - Speed PI controller proportional gain</li>
 *   <li>Signal 6: {@code T_n} - Speed PI controller integral time constant [s]</li>
 *   <li>Signal 7: {@code n_limit} - Speed controller output saturation limit (max iq_ref)</li>
 *   <li>Signal 8: {@code Kp_i} - Current PI controller proportional gain</li>
 *   <li>Signal 9: {@code T_i} - Current PI controller integral time constant [s]</li>
 *   <li>Signal 10: {@code i_limit} - Current controller output saturation limit (max voltage)</li>
 *   <li>Signal 11: {@code fP} - Sampling / pulse period latch trigger flag</li>
 * </ul>
 *
 * <p><strong>Output Signals (8):</strong></p>
 * <ul>
 *   <li>Signal 0: {@code valpha} - Alpha-axis voltage command [V]</li>
 *   <li>Signal 1: {@code vbeta} - Beta-axis voltage command [V]</li>
 *   <li>Signal 2: {@code vq_ref} - Synchronous q-axis voltage command [V]</li>
 *   <li>Signal 3: {@code vd_ref} - Synchronous d-axis voltage command [V]</li>
 *   <li>Signal 4: {@code iq_ref} - Synchronous q-axis current reference [A]</li>
 *   <li>Signal 5: {@code id_ref} - Synchronous d-axis current reference [A]</li>
 *   <li>Signal 6: {@code iq} - Measured synchronous q-axis current [A]</li>
 *   <li>Signal 7: {@code id} - Measured synchronous d-axis current [A]</li>
 * </ul>
 *
 * @author GeckoCIRCUITS Team
 * @since v2.18.0 Task L4
 */
public final class PmsmControlCalculator extends AbstractControlCalculatable {

    private static final int NO_INPUTS = 12;
    private static final int NO_OUTPUTS = 8;

    private static final double SECONDS_PER_MINUTE = 60.0;
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double INTEGRATOR_WEIGHT = 0.5;
    private static final double PULSE_TRIGGER_THRESHOLD = 0.999;

    // State variables for PI controllers and latched outputs
    private double valphaLast = 0.0;
    private double vbetaLast = 0.0;
    private double intNLast = 0.0;
    private double intIqLast = 0.0;
    private double intIdLast = 0.0;
    private double xNLast = 0.0;
    private double xIqLast = 0.0;
    private double xIdLast = 0.0;

    public PmsmControlCalculator() {
        super(NO_INPUTS, NO_OUTPUTS);
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        final double ia = _inputSignal[0][0];
        final double ib = _inputSignal[1][0];
        final double w = _inputSignal[2][0];
        final double phi = _inputSignal[3][0];
        final double nRef = _inputSignal[4][0];
        final double kpN = _inputSignal[5][0];
        final double tN = _inputSignal[6][0];
        final double nLimit = _inputSignal[7][0];
        final double kpI = _inputSignal[8][0];
        final double tI = _inputSignal[9][0];
        final double iLimit = _inputSignal[10][0];
        final double fP = _inputSignal[11][0];

        final double a1N = (tN > 0.0) ? kpN / tN : 0.0;
        final double a1I = (tI > 0.0) ? kpI / tI : 0.0;
        final double wRef = (nRef / SECONDS_PER_MINUTE) * TWO_PI;

        // Clarke transformation (3-phase balanced to stationary orthogonal alpha-beta)
        final ClarkeTransform.AlphaBeta iAlphaBeta = ClarkeTransform.forwardBalanced(ia, ib);

        // Park transformation (alpha-beta to synchronous rotating d-q frame)
        final ParkTransform.DirectQuadrature iDq = ParkTransform.forward(iAlphaBeta.alpha(), iAlphaBeta.beta(), phi);
        final double id = iDq.d();
        final double iq = iDq.q();

        // Speed control loop (PI with trapezoidal integration)
        final double xN = wRef - w;
        double intN = intNLast + a1N * deltaT * INTEGRATOR_WEIGHT * (xN + xNLast);
        if (intN > nLimit) {
            intN = nLimit;
        } else if (intN < -nLimit) {
            intN = -nLimit;
        }

        double iqRef = kpN * xN + intN;
        if (iqRef > nLimit) {
            iqRef = nLimit;
        } else if (iqRef < -nLimit) {
            iqRef = -nLimit;
        }

        // Iq current control loop
        final double xIq = iqRef - iq;
        double intIq = intIqLast + a1I * deltaT * INTEGRATOR_WEIGHT * (xIq + xIqLast);
        if (intIq > iLimit) {
            intIq = iLimit;
        } else if (intIq < -iLimit) {
            intIq = -iLimit;
        }

        double vqRef = kpI * xIq + intIq;
        if (vqRef > iLimit) {
            vqRef = iLimit;
        } else if (vqRef < -iLimit) {
            vqRef = -iLimit;
        }

        // Id current control loop (idRef = 0 for standard surface-mounted PMSM)
        final double idRef = 0.0;
        final double xId = idRef - id;
        double intId = intIdLast + a1I * deltaT * INTEGRATOR_WEIGHT * (xId + xIdLast);
        if (intId > iLimit) {
            intId = iLimit;
        } else if (intId < -iLimit) {
            intId = -iLimit;
        }

        double vdRef = kpI * xId + intId;
        if (vdRef > iLimit) {
            vdRef = iLimit;
        } else if (vdRef < -iLimit) {
            vdRef = -iLimit;
        }

        // Inverse Park transformation (rotating d-q back to stationary alpha-beta)
        final ClarkeTransform.AlphaBeta vAlphaBeta = ParkTransform.inverse(vdRef, vqRef, phi);
        final double valpha = vAlphaBeta.alpha();
        final double vbeta = vAlphaBeta.beta();

        if (fP > PULSE_TRIGGER_THRESHOLD) {
            valphaLast = valpha;
            vbetaLast = vbeta;
        }

        xNLast = xN;
        intNLast = intN;
        xIqLast = xIq;
        intIqLast = intIq;
        xIdLast = xId;
        intIdLast = intId;

        _outputSignal[0][0] = valphaLast;
        _outputSignal[1][0] = vbetaLast;
        _outputSignal[2][0] = vqRef;
        _outputSignal[3][0] = vdRef;
        _outputSignal[4][0] = iqRef;
        _outputSignal[5][0] = idRef;
        _outputSignal[6][0] = iq;
        _outputSignal[7][0] = id;
    }
}
