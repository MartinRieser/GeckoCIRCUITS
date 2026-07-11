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
 * Pass-through/no-op calculator that performs no computation.
 * Used as a placeholder for non-calculable control blocks.
 */
public class NothingToDoCalculator extends AbstractControlCalculatable implements NotCalculateableMarker {

    /**
     * Creates a NothingToDoCalculator with the specified number of inputs and outputs.
     *
     * @param noInputs  number of input signals
     * @param noOutput  number of output signals
     */
    public NothingToDoCalculator(int noInputs, int noOutput) {
        super(noInputs, noOutput);
    }
    
    @Override
    public void calculateYOUT(final double deltaT) {
        // nothing todo, as the class name says!
    }
    
}
