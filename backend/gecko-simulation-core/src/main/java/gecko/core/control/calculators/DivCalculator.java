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
 * Divide the first input by the second input. Do some error check, e.g. 0/0 should return 0
 * @author andreas
 */
public final class DivCalculator extends AbstractTwoInputsOneOutputCalculator {

    // Fits comfortably in IEEE-754 single-precision float (max ~3.4e38) without overflowing to Infinity
    private static final double LARGE_NUMBER = 1e30;

    @Override
    public void calculateYOUT(final double deltaT) {
        final double denom = _inputSignal[1][0];
        final double numer = _inputSignal[0][0];
        if (denom == 0.0) {
            if (numer == 0.0 || Double.isNaN(numer)) {
                _outputSignal[0][0] = 0.0;
            } else {
                _outputSignal[0][0] = Math.signum(numer) * LARGE_NUMBER;
            }
            return;
        }

        final double result = numer / denom;
        if (Double.isNaN(result)) {
            _outputSignal[0][0] = 0.0;
        } else if (Double.isInfinite(result)) {
            _outputSignal[0][0] = Math.signum(numer) * LARGE_NUMBER;
        } else {
            _outputSignal[0][0] = result;
        }
    }
}
