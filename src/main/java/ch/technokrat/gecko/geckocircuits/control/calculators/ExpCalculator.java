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
 * Calculates e^x. The input is limited to 100 to prevent double overflow.
 */
public final class ExpCalculator extends AbstractSingleInputSingleOutputCalculator {    
    /** Maximum input value to prevent double overflow (exp(100) ~ 2.7e43 is safe). */
    private static final double LARGEST_POSSIBLE = 100;
    @Override
    public void calculateYOUT(final double deltaT) {
        assert _inputSignal[0][0] < LARGEST_POSSIBLE;
        _outputSignal[0][0] = Math.exp(_inputSignal[0][0]);
    }
}
