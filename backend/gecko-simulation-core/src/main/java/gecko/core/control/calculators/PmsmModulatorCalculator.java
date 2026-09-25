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

import gecko.core.math.SpaceVectorSector;

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

        final SpaceVectorSector.DwellTimes dwell = SpaceVectorSector.calculateDwellTimes(valpha, vbeta, vdc);
        final SpaceVectorSector.SwitchingStates states = SpaceVectorSector.computeSwitchingCommands(dwell, triangle);

        _outputSignal[0][0] = states.u();
        _outputSignal[1][0] = states.v();
        _outputSignal[2][0] = states.w();
    }
}
