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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Class used as a wrapper for the native function calls
 * @author DIEHL Controls Ricardo Richter
 */
public class NativeCWrapper implements InterfaceNativeCWrapper {

    /**
     * Load the Native Library with the specified path.
     *
     * <p>The library is first copied to a unique temp file and that copy is
     * what gets passed to {@link System#load(String)}. This is the
     * cross-platform workaround for the well-known "JNI library already loaded
     * in another classloader" / ".dll in use" problem: by loading from a fresh
     * per-run copy, the user's source {@code .dll}/{@code .so}/{@code .dylib}
     * is never locked by the OS, so it can be recompiled and replaced freely
     * without restarting the JVM.
     *
     * <p>The temp copy IS locked until the owning {@link NativeCClassLoader}
     * is garbage-collected (same unload behaviour as before); only the source
     * file benefits from being unlocked. Temp copies are registered for
     * deletion on JVM exit via {@link java.io.File#deleteOnExit()}.
     *
     * @param name the full path and name of the native library
     */
    @Override
    public void loadLibrary(String name) {
        try {
            System.load(copyToTemp(name).toAbsolutePath().toString());
        } catch (IOException ioe) {
            // Fall back to direct load if temp-copy fails for any reason
            // (read-only temp dir, out of disk space, etc.). This preserves
            // the legacy single-load behaviour at the cost of locking the
            // source file.
            System.load(name);
        }
    }

    /**
     * Copy the source library to a unique temp file under
     * {@code ${java.io.tmpdir}/geckocircuits-native/} and register it for
     * deletion on JVM exit.
     */
    private static Path copyToTemp(final String sourcePath) throws IOException {
        final Path source = Paths.get(sourcePath);
        final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "geckocircuits-native");
        Files.createDirectories(tempDir);
        final String uniqueName = source.getFileName().toString() + "." + System.nanoTime();
        final Path tempCopy = tempDir.resolve(uniqueName);
        Files.copy(source, tempCopy, StandardCopyOption.REPLACE_EXISTING);
        tempCopy.toFile().deleteOnExit();
        return tempCopy;
    }

    /**
     * function is called every timestep
     * @param xINVector the input vector
     * @param numberOfOuts number of Outputs of the Native C/C++ Block
     * @param time  current time
     * @param deltaT    time difference
     * @return Array with dimension of numberOfOuts, with the computed outputs
     */
    @Override
    public native void calcOutputs(double[] xINVector, double[] xOUTVector, int numberOfOuts, double time, double deltaT);

    /**
     * function called at time t=0 to initialize parameters
     */
    @Override
    public native void initParameters();
}
