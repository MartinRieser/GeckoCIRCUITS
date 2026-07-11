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
 * Used to maintain the Classloader and call Native Functions
 * 
 * @author DIEHL Controls Ricardo Richter
 */
public class NativeCBlock {
    NativeCClassLoader _customCClassLoader;
    Class<?> _nativeCWrapperClass;
    InterfaceNativeCWrapper _nativeCWrapperObj;
    private double[] _xINVector;
    private double[] _xOUTVector;
    
    /**
     * Constructs a new NativeCBlock and initializes a NativeCClassLoader.
     */
    public NativeCBlock () {
        _customCClassLoader = new NativeCClassLoader();
    }
    
    /**
     * Computes the output signals from the input signals by calling the native C/C++ wrapper.
     *
     * @param time          current simulation time
     * @param deltaT        time step size
     * @param inputSignals  array of input signal vectors
     * @param outputSignals array of output signal vectors
     * @throws Exception if native calculation fails
     */
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidArrayLoops"})
    void calculateYOUT(final double time, final double deltaT, final double[][] inputSignals,
            final double[][] outputSignals) throws Exception {
        

        if ( _xINVector == null ) {
            _xINVector = new double[inputSignals.length];
        }
        
        if (_xOUTVector == null) {
            _xOUTVector = new double[outputSignals.length];
        }
        
        if (time == 0) {
            _nativeCWrapperObj.initParameters();
        }

        for (int i = 0; i < _xINVector.length; i++) {
            _xINVector[i] = inputSignals[i][0];
        }
        
        _nativeCWrapperObj.calcOutputs(_xINVector, _xOUTVector, outputSignals.length, time, deltaT);
        

        for (int i = 0; i < _xOUTVector.length; i++) {
            outputSignals[i][0] = _xOUTVector[i];
        }

        checkOutputsForNANorINFValues(outputSignals);
    }
    
    /**
     * Loads the native library with the specified name using a fresh class loader.
     *
     * @param name the full path and name of the native library
     * @return true if the library was loaded successfully, false otherwise
     */
    public boolean loadLibraries (final String name) {
        try {
            _customCClassLoader = new NativeCClassLoader();
            _nativeCWrapperClass = _customCClassLoader.findClass("ch.technokrat.gecko.geckocircuits.nativec.NativeCWrapper");
            _nativeCWrapperObj = (InterfaceNativeCWrapper) _nativeCWrapperClass.getDeclaredConstructor().newInstance();
            _nativeCWrapperObj.loadLibrary(name);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Unloads the native library by clearing all references and invoking garbage collection.
     */
    public void unloadLibraries () {
        try {
            _nativeCWrapperObj = null;
            _nativeCWrapperClass = null;
            _customCClassLoader = null;
            _xINVector = null;
            _xOUTVector = null;
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Checks all output signals for NaN or infinite values and throws an exception if any are found.
     *
     * @param signal the array of output signals to check
     * @throws ArithmeticException if any output value is NaN or infinite
     */
    private void checkOutputsForNANorINFValues(double[][] signal) {
        for (int i = 0; i < signal.length; i++) {
            if (Double.isNaN(signal[i][0]) || Double.isInfinite(signal[i][0])) {
                throw new ArithmeticException("Output value yOUT[" + i + "] is not a number: " + signal[i][0]);
            }
        }
    }
}