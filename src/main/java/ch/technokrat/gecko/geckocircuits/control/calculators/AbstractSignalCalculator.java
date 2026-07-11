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
 * Abstract base class for signal-source calculators. Each signal source
 * produces a single output signal.
 */
public abstract class AbstractSignalCalculator extends AbstractControlCalculatable {

    public AbstractSignalCalculator(final int noInputs) {
        super(noInputs, 1);
    }

    /** Constant representing 2&pi;, used for phase calculations in radians. */
    static final double TWO_PI = 2 * Math.PI;
    
}