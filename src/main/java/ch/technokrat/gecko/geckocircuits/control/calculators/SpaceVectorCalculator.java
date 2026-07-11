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

import ch.technokrat.gecko.geckocircuits.control.SpaceVectorDisplay;

/**
 * Feeds 9 input signals (3 phases x 3 space vectors) to a
 * {@link SpaceVectorDisplay} for real-time visualization.
 */
public final class SpaceVectorCalculator extends AbstractControlCalculatable {
    private final SpaceVectorDisplay _svd;
    /**
     * Number of inputs: 3 phases (R, S, T) for each of the 3 space vectors.
     * Inputs 0-2: vector 1, inputs 3-5: vector 2, inputs 6-8: vector 3.
     */
    private static final int NO_INPUTS = 9;
    
    public SpaceVectorCalculator(final SpaceVectorDisplay display) {
        super(NO_INPUTS, 0);
        _svd = display;
    }
    
    @Override
    public void calculateYOUT(final double deltaT) {
        _svd.drawVector(_time, _inputSignal, deltaT);
    }
}
