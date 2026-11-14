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
 * Edge-triggered pulse counter with an active-high synchronous reset on the
 * second (lower) input: while RESET stays at or below the logic threshold
 * ({@code <= 0.5}), rising edges on input 0 increment the count; whenever RESET
 * rises above {@code 0.5}, the counter resets to {@code 0}.
 */
public final class CounterCalculatable extends AbstractTwoInputsOneOutputCalculator {
    private double _lastValue = 0;

    @Override
    public void calculateYOUT(final double deltaT) {
        if ((_inputSignal[0][0] >= SIGNAL_THRESHOLD) && (_lastValue < SIGNAL_THRESHOLD)) {
            _outputSignal[0][0]++;
        }
        if (_inputSignal[1][0] > SIGNAL_THRESHOLD) {
            // Active-high reset at 0.5 threshold so an unwired RESET pin stays low and lets the counter run
            _outputSignal[0][0] = 0;
        }
        _lastValue = _inputSignal[0][0];
        
    }
}
