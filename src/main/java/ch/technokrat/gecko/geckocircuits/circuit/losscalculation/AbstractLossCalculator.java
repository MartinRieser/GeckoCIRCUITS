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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

/**
 * Contract for semiconductor loss calculators. Implementations compute
 * conduction and switching losses per simulation step and report the
 * cumulative total.
 */
public interface AbstractLossCalculator {
    /**
     * Calculates conduction and switching losses for one simulation step.
     *
     * @param current actual current value in amperes
     * @param temperature semiconductor temperature in degrees Celsius
     * @param deltaT simulation time step in seconds
     */
    void calcLosses(final double current, final double temperature, final double deltaT);

    /**
     * careful: before, you have to call "calcLosses"!
     * @return the loss power in Watts
     */
    double getTotalLosses();
}