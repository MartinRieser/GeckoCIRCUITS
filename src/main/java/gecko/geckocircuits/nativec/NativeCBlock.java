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

package gecko.geckocircuits.nativec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Used to maintain the Classloader and call Native Functions
 *
 * @author DIEHL Controls Ricardo Richter
 */
@SuppressFBWarnings(value = {"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", "DM_GC"},
        justification = "ClassLoader creation and explicit System.gc() call are intentional to load and unload native C code")
public class NativeCBlock {
    private static final Logger LOGGER = LogManager.getLogger(NativeCBlock.class);

    NativeCClassLoader _customCClassLoader;
    Class _nativeCWrapperClass;
    InterfaceNativeCWrapper _nativeCWrapperObj;
    private double[] _xINVector;
    private double[] _xOUTVector;

    public NativeCBlock () {
        _customCClassLoader = new NativeCClassLoader();
    }

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

    public boolean loadLibraries (final String name) {
        try {
            _customCClassLoader = new NativeCClassLoader();
            _nativeCWrapperClass = _customCClassLoader.findClass("gecko.geckocircuits.nativec.NativeCWrapper");
            _nativeCWrapperObj = (InterfaceNativeCWrapper) _nativeCWrapperClass.newInstance();
            _nativeCWrapperObj.loadLibrary(name);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load native library: " + name, e);
            return false;
        }
    }

    /**
     * Unload the native library and release all associated references.
     *
     * <p>Strategy: drop all references (the {@link NativeCClassLoader}, the
     * wrapper class and instance, the IO vectors) and call {@code System.gc()}
     * to encourage the JVM to collect the ClassLoader and release its hold on
     * the loaded library file.
     *
     * <p>Note: since {@code NativeCWrapper.loadLibrary} now loads from a
     * per-run temp copy (see that method's javadoc), the user's source
     * {@code .dll}/{@code .so}/{@code .dylib} is never locked in the first
     * place, so the determinism of this unload matters only for cleaning up
     * temp files - not for unblocking user recompile workflows.
     *
     * <p>The old approach (Java 8 era) reflected into the private
     * {@code ClassLoader.nativeLibraries} vector and called {@code finalize()}
     * on each entry. That field became inaccessible on Java 9+ and the
     * reflection was disabled. {@code System.gc()} is load-bearing here:
     * removing it caused real Windows .dll-lock issues (see commit 71ac8ddd
     * on {@code feature/fix-jni-and-spotbugs-for-gecko2026}).
     */
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    public void unloadLibraries () {
        try {
            _nativeCWrapperObj = null;
            _nativeCWrapperClass = null;
            _customCClassLoader = null;
            _xINVector = null;
            _xOUTVector = null;
            System.gc();
        } catch (Exception e) {
            LOGGER.error("Failed to unload native libraries", e);
        }
    }

    private void checkOutputsForNANorINFValues(double[][] signal) {
        for (int i = 0; i < signal.length; i++) {
            if (Double.isNaN(signal[i][0])) {
                throw new ArithmeticException("Output value yOUT[" + i + "] is not a number: " + signal[i]);
            }
        }
    }
}