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
 * Calculates hysteresis output signal based on a fixed internal boundary threshold value.
 *
 * <p><strong>Assumptions:</strong>
 * This calculator assumes that the boundary threshold value {@code hValue} is non-negative.
 * If {@code hValue} is negative, the comparisons will yield unexpected results.
 * </p>
 */
public final class HysteresisCalculatorInternal extends AbstractControlCalculatable {

    private double _hValue;

    /**
     * Constructs a new internal hysteresis calculator with a given threshold.
     *
     * @param hValue the hysteresis boundary value
     */
    public HysteresisCalculatorInternal(final double hValue) {
        super(1, 1);
        setHValue(hValue);
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        if (_inputSignal[0][0] > +_hValue) {
            _outputSignal[0][0] = 1;
        } else if (_inputSignal[0][0] < -_hValue) {
            _outputSignal[0][0] = -1;
        } else if (_inputSignal[0][0] == _hValue) {
            _outputSignal[0][0] = Math.signum(_inputSignal[0][0]);
        } 
    }

    public void setHValue(final double hValue) {
        _hValue = hValue;
    }
}
