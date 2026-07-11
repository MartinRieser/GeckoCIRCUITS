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

package ch.technokrat.gecko.geckocircuits.nativec;

/**
 * Class used as a wrapper for the native function calls
 * @author DIEHL Controls Ricardo Richter
 */
public class NativeCWrapper implements InterfaceNativeCWrapper {

    /**
     * Load the Native Library with the specified path
     * @param name the full path and name of the native library
     */
    @Override
    public void loadLibrary(String name) {
        System.load(name);
    }

    /**
     * Calculates the output values from the input vector using the native library.
     *
     * @param xINVector    the input vector
     * @param xOUTVector   the output vector to be filled by the native method
     * @param numberOfOuts number of outputs of the native C/C++ block
     * @param time         current simulation time
     * @param deltaT       simulation time step
     */
    @Override
    public native void calcOutputs(double[] xINVector, double[] xOUTVector, int numberOfOuts, double time, double deltaT);

    /**
     * Initializes the native C/C++ block parameters at the start of the simulation (t=0).
     * This is a native method implemented in the external C/C++ library.
     */
    @Override
    public native void initParameters();
}
