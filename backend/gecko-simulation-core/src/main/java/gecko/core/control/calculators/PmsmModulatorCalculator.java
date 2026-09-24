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

/**
 * Two-level, three-phase Space Vector Pulse-Width Modulation (SVPWM) calculator for
 * Permanent Magnet Synchronous Machines (PMSM) and AC drives.
 *
 * <p><strong>Input Signals:</strong></p>
 * <ul>
 *   <li>Signal 0: {@code v_alpha} - Stationary alpha-axis reference voltage</li>
 *   <li>Signal 1: {@code v_beta} - Stationary beta-axis reference voltage</li>
 *   <li>Signal 2: {@code triangle} - Triangular carrier wave (range [0.0, 1.0])</li>
 *   <li>Signal 3: {@code v_dc} - DC-link voltage</li>
 * </ul>
 *
 * <p><strong>Output Signals:</strong></p>
 * <ul>
 *   <li>Signal 0: {@code U} - Phase U gate signal (0.0 or 1.0)</li>
 *   <li>Signal 1: {@code V} - Phase V gate signal (0.0 or 1.0)</li>
 *   <li>Signal 2: {@code W} - Phase W gate signal (0.0 or 1.0)</li>
 * </ul>
 *
 * <p>The modulator determines the reference voltage vector magnitude and angle in the
 * stationary alpha-beta frame, identifies the active 60-degree sector (0 to 5),
 * computes the dwell times of the adjacent active space vectors and zero vectors,
 * and generates complementary switching commands via triangular carrier comparison.</p>
 */
public final class PmsmModulatorCalculator extends AbstractControlCalculatable {

    private static final int NO_INPUTS = 4;
    private static final int NO_OUTPUTS = 3;

    private static final double SECTOR_ANGLE = Math.PI / 3.0; // 60 degrees
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double SQRT_3_DIV_2 = SQRT_3 / 2.0;
    private static final int MAX_SECTOR_INDEX = 5;

    public PmsmModulatorCalculator() {
        super(NO_INPUTS, NO_OUTPUTS);
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        final double valpha = _inputSignal[0][0];
        final double vbeta = _inputSignal[1][0];
        final double triangle = _inputSignal[2][0];
        final double vdc = _inputSignal[3][0];

        // Guard against non-positive or non-finite DC-link voltage
        if (vdc <= 0.0 || !Double.isFinite(vdc)) {
            _outputSignal[0][0] = 0.0;
            _outputSignal[1][0] = 0.0;
            _outputSignal[2][0] = 0.0;
            return;
        }

        double vabs = Math.sqrt(valpha * valpha + vbeta * vbeta);
        if (vabs >= vdc) {
            vabs = vdc; // Overmodulation clamp
        }

        final double modulationIndex = 2.0 * vabs / (SQRT_3 * vdc);

        // Normalize angle to [0, 2*pi)
        double angle = Math.atan2(vbeta, valpha);
        if (angle < 0.0) {
            angle += TWO_PI;
        }

        int sector = (int) (angle / SECTOR_ANGLE);
        if (sector > MAX_SECTOR_INDEX) {
            sector = MAX_SECTOR_INDEX;
        }

        final double angleRel = angle - sector * SECTOR_ANGLE;

        // Relative dwell times of the active space vectors in a switching half-period
        final double deltaVector1 = SQRT_3_DIV_2 * modulationIndex * Math.sin(SECTOR_ANGLE - angleRel);
        final double deltaVector2 = SQRT_3_DIV_2 * modulationIndex * Math.sin(angleRel);
        final double deltaZero = 0.5 * (1.0 - deltaVector1 - deltaVector2); // Symmetrical zero-vector allocation

        final double comp0 = deltaZero;
        final double comp1a = deltaZero + deltaVector1;
        final double comp1b = deltaZero + deltaVector2;
        final double comp2 = deltaZero + deltaVector1 + deltaVector2;

        final double pwm0 = (triangle >= comp0) ? 0.0 : 1.0;
        final double pwm1a = (triangle >= comp1a) ? 0.0 : 1.0;
        final double pwm1b = (triangle >= comp1b) ? 0.0 : 1.0;
        final double pwm2 = (triangle >= comp2) ? 0.0 : 1.0;

        final double u;
        final double v;
        final double w;

        switch (sector) {
            case 0 -> {
                u = pwm0;
                v = pwm1a;
                w = pwm2;
            }
            case 1 -> {
                u = pwm1b;
                v = pwm0;
                w = pwm2;
            }
            case 2 -> {
                u = pwm2;
                v = pwm0;
                w = pwm1a;
            }
            case 3 -> {
                u = pwm2;
                v = pwm1b;
                w = pwm0;
            }
            case 4 -> {
                u = pwm1a;
                v = pwm2;
                w = pwm0;
            }
            case 5 -> {
                u = pwm0;
                v = pwm2;
                w = pwm1b;
            }
            default -> {
                u = 0.0;
                v = 0.0;
                w = 0.0;
            }
        }

        _outputSignal[0][0] = u;
        _outputSignal[1][0] = v;
        _outputSignal[2][0] = w;
    }
}
