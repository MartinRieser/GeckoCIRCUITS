/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

/**
 * Abstract calculator for controlled (dependent) voltage sources. Extends the
 * independent voltage source stamping with a gain factor and a reference to
 * the controlling current component.
 */
public abstract class AbstractVoltageSourceControlledCalculator extends AbstractVoltageSourceCalculator {
    protected double _gain = 1;
    protected DirectCurrentCalculatable _currentControl;

   public AbstractVoltageSourceControlledCalculator(final AbstractVoltageSource parent) {
       super(parent);
   }

    /**
     * Sets the gain factor applied to the controlling signal.
     *
     * @param value the gain value
     */
    public final void setGain(final double value) {
        _gain = value;
    }

    /**
     * Sets the component whose current value controls this voltage source.
     *
     * @param value the controlling current-calculatable component
     */
    public final void setCurrentControlComponent(final DirectCurrentCalculatable value) {
        _currentControl = value;
    }

}
