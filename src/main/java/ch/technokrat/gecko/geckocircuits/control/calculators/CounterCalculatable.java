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
package ch.technokrat.gecko.geckocircuits.control.calculators;

/**
 * The internal counter can be reset to '0' by the second (lower) RESET input:
 * as long as the RESET input is at zero, the internal counter is set to zero.
 * As soon as the RESET input is set to one, the internal counter starts running and is output directly at the output.
 * @author andreas
 */
public final class CounterCalculatable extends AbstractTwoInputsOneOutputCalculator {
    /** Stores the previous value of the counting input for rising-edge detection. */
    private double _lastValue = 0;            

    @Override
    public void calculateYOUT(final double deltaT) {
        if ((_inputSignal[0][0] >= SIGNAL_THRESHOLD) && (_lastValue < SIGNAL_THRESHOLD)) {
            _outputSignal[0][0]++;
        }
        if (_inputSignal[1][0] > SIGNAL_THRESHOLD) {
            _outputSignal[0][0] = 0;  // // logic threshold --> 0.5;  RESET at input '1' (so you need
            // // do not assign a const=1 block to the connection so that the counter runs)
        }
        _lastValue = _inputSignal[0][0];
        
    }
}
