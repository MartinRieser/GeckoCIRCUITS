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
 * Calculator base class for PT (proportional time-delay / lag) transfer
 * function elements (PT1, PT2). Provides the time constant and gain
 * parameters used by subclasses to compute the dynamic response.
 *
 * @author andreas
 */
public abstract class AbstractPTCalculator extends AbstractSingleInputSingleOutputCalculator {

    /** Time constant T of the transfer function (seconds). */
    protected double _TVal;
    /** Gain a1 of the transfer function. */
    protected double _a1Val;

    /**
     * @param timeConstant the time constant T
     * @param gainFactor the gain a1
     */
    public AbstractPTCalculator(final double timeConstant, final double gainFactor) {
        super();
        _TVal = timeConstant;
        _a1Val = gainFactor;
    }

    /**
     * @param value the new time constant T
     */
    public void setTimeConstant(final double value) {
        _TVal = value;
    }

    /**
     * @param value the new gain a1
     */
    public void setGain(final double value) {
        _a1Val = value;
    }
}
