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
package ch.technokrat.gecko;

/**
 * WARNING: this interface is only used within the Java-Block. However, it
 * should not be moved to this package. This interface is NOT allowed to
 * be obfuscated. Otherwise, the java-Block has a problem.
 * @author andy
 */
public interface ControlCalculatable {
    static final long serialVersionUID = 364747364514L;
    /**
     * Computes the output values for the current simulation step.
     *
     * @param xIN     array of input signal values for the current step
     * @param time    absolute simulation time at the current step
     * @param deltaT  time step between successive simulation iterations
     * @return        array of computed output signal values
     */
    @SuppressWarnings("PMD")
    double[] calculateYOUT(double[] xIN, double time, double deltaT) throws Exception;
    /**
     * Called once at the start of a simulation to initialise internal state before the first
     * calculation step is performed.
     */
    void init();
}
