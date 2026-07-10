/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.technokrat.gecko.geckocircuits.nativec;

import java.io.File;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

/**
 * Comment: The Native C Test Libraries were compiled on a Windows x86_64 machine 
 *          with GCC. To use them, you need to run the JUnit test under Windows 64bit.
 *          Otherwise, please recompile them and if necessary edit the fileName.
 * @author DIEHL Controls Ricardo Richter
 */
public class NativeCTest {
    private static final double DELTA_T = 5e-4;
    private static final double END_TIME = 1;
    private NativeCBlock _nativeCBlock;
    private NativeCLibraryFile _libFile;
    String _libFilePath, _libName;
    
    /**
     * 
     * @param fileName  Name of Native Library
     * @return the absolute Path to the Native Library in the test directory
     */
    public String constructAbsolutPath(String fileName) {
        String absPath = new File(".").getAbsolutePath(); // get current directory
        if (absPath.endsWith(".")) {    // check for "."
            absPath = absPath.substring(0, absPath.length() - 1);
        }
        if (absPath.endsWith(File.separator)) {    // check for path ending (with "\" or without)
            absPath = absPath.substring(0, absPath.length() - 1);
        }
        absPath = absPath + File.separator
                + "src" + File.separator
                + "test" + File.separator
                + "java" + File.separator
                + "ch" + File.separator
                + "technokrat" + File.separator
                + "gecko" + File.separator
                + "geckocircuits" + File.separator
                + "nativec" + File.separator
                + "testJNI_DLL" + File.separator
                + fileName;
         return absPath;
    }
    
    @BeforeEach
    public void setUp() {
        try {
            // Detect platform and select appropriate library extension
            String osName = System.getProperty("os.name").toLowerCase();
            String libExtension;
            if (osName.contains("win")) {
                libExtension = ".dll";
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                libExtension = ".dylib";
            } else {
                libExtension = ".so";
            }
            _libName = "libtestJNI_DLL" + libExtension;
            // construct an absolute file path to the test library
            // this is needed for the System.load() to work
            _libFilePath = constructAbsolutPath(_libName);
            File libFile = new File(_libFilePath);
            if (libFile.exists()) {
                _libFile = new NativeCLibraryFile(_libFilePath);
            } else {
                _libFile = null;
            }
        } catch (Exception exc) {
            _libFile = null;
        }
    }
    
    @Test
    public void testNativeCLibraryFile_NotFound() {
        FileNotFoundException exception = Assertions.assertThrows(FileNotFoundException.class, () -> {
            new NativeCLibraryFile("..\\.dll");
        });
        Assertions.assertTrue(exception.getMessage().contains("Could not find Library File"));
    }
    
    @Test
    public void testNativeCLibraryFile_Found() {
        Assumptions.assumeTrue(_libFile != null, "Native library not available for this platform");
        try {
            NativeCLibraryFile testLibFile = new NativeCLibraryFile(_libFilePath);
            Assertions.assertNotNull(testLibFile);
            Assertions.assertNotNull(testLibFile.getFile());
            Assertions.assertNotNull(testLibFile.getFileName());
        } catch (FileNotFoundException exc) {
            Assertions.fail("Test File was not found!");
        }
    }
    
    @Test
    public void testLoadAndExecuteNativeLibrary() {
        Assumptions.assumeTrue(_libFile != null, "Native library not available for this platform");
        _nativeCBlock = new NativeCBlock();
        double[][] testInput = {{1, 2, 3, 4, 5}};
        double[][] testOutput = {{0, 0, 0}};
        Assertions.assertNotNull(_nativeCBlock);

        boolean loaded = _nativeCBlock.loadLibraries(_libFilePath);
        if (!loaded) {
            System.out.println("WARNING: Native library could not be loaded. " +
                "This test requires native libraries compiled for the current platform. Skipping test.");
            Assumptions.assumeTrue(false, "Native library not available for this platform");
            return;
        }

        try {
            for (double time = 0; time < END_TIME; time+=DELTA_T) {
                _nativeCBlock.calculateYOUT(time, DELTA_T, testInput, testOutput);
                double tmpOut = 0;
                for (int i = 0; i < testOutput.length; i++) {
                    tmpOut = tmpOut + testInput[0][i];
                    Assertions.assertEquals(testOutput[0][i], tmpOut, 1e-6);
                }
            }
            _nativeCBlock.unloadLibraries();
            Assertions.assertNull(_nativeCBlock._customCClassLoader);
            _nativeCBlock = null;
            Assertions.assertNull(_nativeCBlock);
        } catch (Exception exc) {
            Assertions.fail(exc.getMessage());
        }
    }

    @Test
    public void testLoadAndExecuteNLAgain() {
        // execute again with same library
        testLoadAndExecuteNativeLibrary();
        // test with different library
        String testLib2 = _libName.replace("libtestJNI_DLL", "libtestJNI_DLL2");
        _libFilePath = constructAbsolutPath(testLib2);
        Assertions.assertNotNull(_libFilePath);
        testLoadAndExecuteNativeLibrary();
    }
}
