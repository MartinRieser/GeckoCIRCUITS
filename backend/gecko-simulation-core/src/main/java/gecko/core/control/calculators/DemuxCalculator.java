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
 * Demultiplexer block extracting individual scalar output channels from an input vector signal.
 */
public final class DemuxCalculator extends AbstractControlCalculatable {

    public DemuxCalculator(int numOutputs) {
        super(1, Math.max(1, numOutputs));
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        if (_inputSignal[0] != null) {
            for (int i = 0; i < _outputSignal.length; i++) {
                _outputSignal[i][0] = (i < _inputSignal[0].length) ? _inputSignal[0][i] : 0.0;
            }
        }
    }
}
