/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end test of NativeC v2: compiles the committed C fixture into a host
 * shared library with the system C compiler and drives {@link CLibraryCalculator}
 * through init/step. Skipped (assumption) when no C compiler exists — CI
 * provides one on all three OSes.
 */
class CLibraryCalculatorTest {

    private static Path compiler(boolean cpp) {
        final List<String> candidates = cpp
                ? List.of("g++", "clang++", "c++")
                : List.of("gcc", "clang", "cc");
        for (String candidate : candidates) {
            Optional<Path> found = findOnPath(candidate);
            if (found.isPresent()) {
                return found.get();
            }
        }
        // windows runners ship MinGW via chocolatey
        for (String dir : List.of(
                "C:/ProgramData/chocolatey/lib/mingw/tools/install/mingw64/bin",
                "C:/mingw64/bin")) {
            for (String exe : (cpp ? List.of("g++.exe", "clang++.exe") : List.of("gcc.exe", "clang.exe"))) {
                Path found = Path.of(dir, exe);
                if (Files.isRegularFile(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Optional<Path> findOnPath(String executable) {
        for (String dir : System.getenv("PATH").split(";")) {
            Path candidate = Path.of(dir.trim(), executable);
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
            Path withExt = Path.of(dir.trim(), executable + ".exe");
            if (Files.isRegularFile(withExt)) {
                return Optional.of(withExt);
            }
        }
        return Optional.empty();
    }

    private static java.net.URL classResource(String name) {
        return CLibraryCalculatorTest.class.getResource("/gecko/nativec/" + name);
    }

    private static Path buildFixtureLibrary(boolean cpp) throws Exception {
        Path compiler = compiler(cpp);
        assumeTrue(compiler != null, "no " + (cpp ? "C++" : "C") + " compiler on PATH - skipping NativeC test");
        String sourceName = cpp ? "gecko_test_block.cpp" : "gecko_test_block.c";
        Path cSource = Path.of(classResource(sourceName).toURI());
        assumeTrue(Files.isRegularFile(cSource), "fixture source missing");
        boolean mac = System.getProperty("os.name").toLowerCase().contains("mac");
        String ext = mac ? ".dylib" : System.getProperty("os.name").toLowerCase().contains("win") ? ".dll" : ".so";
        Path output = Files.createTempFile("gecko-test-lib-", ext);
        List<String> cmd = new java.util.ArrayList<>(List.of(compiler.toString(), "-shared", "-fPIC"));
        if (cpp) {
            Path includeDir = Path.of(classResource("gecko_c_block.h").toURI()).getParent();
            cmd.add("-I" + includeDir);
        }
        cmd.add("-o");
        cmd.add(output.toAbsolutePath().toString());
        cmd.add(cSource.toAbsolutePath().toString());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        assumeTrue(finished, "fixture compile timed out");
        assumeTrue(process.exitValue() == 0, "fixture compile failed with exit " + process.exitValue());
        return output;
    }

    @Test
    void doublesFirstInputAndCountsSteps() throws Exception {
        Path library = buildFixtureLibrary(false);

        CLibraryCalculator calculator = new CLibraryCalculator(2, 2, library.toString());
        try {
            calculator.initializeAtSimulationStart(1e-6);
            assertNull(calculator.getLoadError(), () -> "load error: " + calculator.getLoadError());

            calculator._inputSignal[0] = new double[]{1.5};
            calculator._inputSignal[1] = new double[]{0.0};
            calculator._outputSignal[0] = new double[]{0.0};
            calculator._outputSignal[1] = new double[]{0.0};

            AbstractControlCalculatable.setTime(0.0);
            calculator.calculateYOUT(1e-6);
            assertEquals(3.0, calculator._outputSignal[0][0], 1e-12, "gecko_step must double xIN[0]");
            assertEquals(1.0, calculator._outputSignal[1][0], 1e-12, "step counter should count");

            calculator._inputSignal[0][0] = -2.0;
            calculator.calculateYOUT(1e-6);
            assertEquals(-4.0, calculator._outputSignal[0][0], 1e-12);
            assertEquals(2.0, calculator._outputSignal[1][0], 1e-12, "step counter should increment");
        } finally {
            calculator.close();
        }
    }

    @Test
    void cppFixtureClassStateAndExceptionsContained() throws Exception {
        Path library = buildFixtureLibrary(true);

        CLibraryCalculator calculator = new CLibraryCalculator(1, 2, library.toString());
        try {
            calculator.initializeAtSimulationStart(1e-6);
            assertNull(calculator.getLoadError(), () -> "load error: " + calculator.getLoadError());

            calculator._inputSignal[0] = new double[]{1.0};
            calculator._outputSignal[0] = new double[]{0.0};
            calculator._outputSignal[1] = new double[]{0.0};

            // gecko_step: y0 = 2*x + 0.25*integral, y1 = C++ step counter
            AbstractControlCalculatable.setTime(0.0);
            calculator.calculateYOUT(1e-6);
            assertEquals(2.25, calculator._outputSignal[0][0], 1e-12, "C++ controller state must accumulate");
            assertEquals(1.0, calculator._outputSignal[1][0], 1e-12);

            calculator._inputSignal[0][0] = 3.0;
            calculator.calculateYOUT(1e-6);
            assertEquals(6.75 + 0.25, calculator._outputSignal[0][0], 1e-12);
            assertEquals(2.0, calculator._outputSignal[1][0], 1e-12);
        } finally {
            calculator.close();
        }
    }

    @Test
    void missingLibraryHoldsOutputsAndReports() {
        CLibraryCalculator calculator = new CLibraryCalculator(1, 1, "Z:/definitely/missing.dll");
        calculator._inputSignal[0] = new double[]{7.0};
        calculator._outputSignal[0] = new double[]{0.0};
        calculator.initializeAtSimulationStart(1e-6);
        assertNotNull(calculator.getLoadError(), "missing library must set a load error");
        calculator.calculateYOUT(1e-6);
        assertEquals(0.0, calculator._outputSignal[0][0], 1e-12,
                "failure contract: outputs hold at their initial value");
    }
}
