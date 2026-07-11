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
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * Constant (DC) time function that always returns the same configured value.
 * The stepBack() method is intentionally empty since a constant does not need
 * state restoration for back-stepping.
 */
public class TimeFunctionConstant extends TimeFunction {

    public double _value;

    /**
     * Constructs a constant time function with the given value.
     * @param value the constant output value
     */
    public TimeFunctionConstant(double value) {
        _value = value;
    }


    /**
     * Sets the constant output value.
     * @param value the new constant value
     */
    public final void setValue(double value) {
        _value = value;
    }

    /**
     * Returns the constant value regardless of time.
     * @param t current simulation time (ignored)
     * @param dt current time step (ignored)
     * @return the constant value
     */
    @Override
    public double calculate(double t, double dt) {
        return _value;
    }

    /**
     * Intentionally empty - a constant function has no state to restore.
     */
    public void stepBack() { }

}
