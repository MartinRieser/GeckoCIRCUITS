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

import ch.technokrat.gecko.geckocircuits.control.NotCalculateableMarker;

/**
 * Calculator that outputs a single constant value. Since it never needs to be
 * evaluated during simulation, it implements {@link NotCalculateableMarker}.
 */
public final class ConstantCalculator extends AbstractControlCalculatable implements NotCalculateableMarker {

    /**
     * @param constValue the constant value to output
     */
    public ConstantCalculator(final double constValue) {
        super(0, 1);
        setConst(constValue);
    }

    public void setConst(final double constValue) {
        // since this calculator is "not calculatable), yout will never be executed. Therefore,
        // when setting the const value, the output has to be updated immediately!
        _outputSignal[0][0] = constValue;
    }

    @Override
    public void calculateYOUT(final double deltaT) { // this is notCalculatable!
        assert false;
    }
}
