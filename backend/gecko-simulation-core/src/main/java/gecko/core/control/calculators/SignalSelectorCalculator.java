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
 * Selector multiplexer (CTRL_MUX, typ 1014): passes one of two data inputs to
 * the output, chosen by a binary selector signal.
 *
 * <p>Input layout: {@code input 0 = selector}, {@code input 1 = data in0},
 * {@code input 2 = data in1}. With the selector below the switching threshold
 * the output carries in0, at or above the threshold it carries in1.</p>
 */
public final class SignalSelectorCalculator extends AbstractControlCalculatable {

    /** Index of the selector input among the block inputs. */
    static final int SELECTOR_INPUT = 0;
    /** Index of the data input passed through while the selector is low. */
    static final int DATA_INPUT_LOW = 1;
    /** Index of the data input passed through while the selector is high. */
    static final int DATA_INPUT_HIGH = 2;

    public SignalSelectorCalculator() {
        super(3, 1);
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        final boolean selectHigh = _inputSignal[SELECTOR_INPUT][0] > SIGNAL_THRESHOLD;
        _outputSignal[0][0] = selectHigh ? _inputSignal[DATA_INPUT_HIGH][0]
                : _inputSignal[DATA_INPUT_LOW][0];
    }
}
