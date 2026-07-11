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
 * PD controller implementing numerical differentiation: output = gain/dt * (input - oldValue).
 */
public final class PDCalculator extends AbstractControlCalculatable {

    private double _gain;
    private double _oldValue = 0;
    
    /**
     * Creates a PDCalculator with the specified gain.
     *
     * @param gain the derivative gain factor
     */
    public PDCalculator(final double gain) {
        super(1, 1);
        setGain(gain);
    }    

    /**
     * Computes the derivative output using backward difference: gain/dt * (input - oldValue).
     *
     * @param deltaT the time step size
     */
    @Override
    public void calculateYOUT(final double deltaT) {
        // // simplified formula without yalt --> becomes numerically much more robust
        _outputSignal[0][0] = _gain / deltaT * (_inputSignal[0][0] - _oldValue);  
        _oldValue = _inputSignal[0][0];
    }

    /**
     * Sets the derivative gain factor.
     *
     * @param gain the new gain value
     */
    public void setGain(final double gain) {
        _gain = gain;
    }
}
