/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
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
        String path = System.getenv("PATH");
        if (path == null) {
            return Optional.empty();
        }
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
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
        try (CLibraryCalculator calculator = new CLibraryCalculator(1, 1, "Z:/definitely/missing.dll")) {
            calculator._inputSignal[0] = new double[]{7.0};
            calculator._outputSignal[0] = new double[]{0.0};
            calculator.initializeAtSimulationStart(1e-6);
            assertNotNull(calculator.getLoadError(), "missing library must set a load error");
            calculator.calculateYOUT(1e-6);
            assertEquals(0.0, calculator._outputSignal[0][0], 1e-12,
                    "failure contract: outputs hold at their initial value");
        }
    }

    @Test
    void relativeLibraryPathResolvesAgainstCircuitFile() throws Exception {
        Path library = buildFixtureLibrary(false);
        Path fakeCircuit = library.getParent().resolve("test_circuit.ipes");
        String relativeName = library.getFileName().toString();

        try (CLibraryCalculator calculator = new CLibraryCalculator(1, 2, relativeName, fakeCircuit.toString())) {
            calculator.initializeAtSimulationStart(1e-6);
            assertNull(calculator.getLoadError(), () -> "load error: " + calculator.getLoadError());
            calculator._inputSignal[0] = new double[]{4.25};
            calculator._outputSignal[0] = new double[]{0.0};
            calculator._outputSignal[1] = new double[]{0.0};
            AbstractControlCalculatable.setTime(0.0);
            calculator.calculateYOUT(1e-6);
            assertEquals(8.5, calculator._outputSignal[0][0], 1e-12);
        }
    }

    @Test
    void nativeCIpesRoundTripAndHeadlessSimulation() throws Exception {
        Path library = buildFixtureLibrary(false);
        String relativeName = library.getFileName().toString();
        Path circuitFile = library.getParent().resolve("nativec_roundtrip.ipes");

        String ipes = """
                verbindungLeistungskreisANZAHL 0
                verbindungControlANZAHL 2
                verbindungCONTROL (0)
                <Verbindung>
                label in_sig
                x[] 12 14
                y[] 10 10
                enabled true
                connectorType 1
                <\\Verbindung>
                verbindungCONTROL (1)
                <Verbindung>
                label out_doubled
                x[] 18 22
                y[] 10 10
                enabled true
                connectorType 1
                <\\Verbindung>
                elementANZAHL 0
                controlANZAHL 2
                c (0)
                <ElementCONTROL>
                labelAnfangsKnoten[]
                labelEndKnoten[] /in_sig
                enabledShorted 1
                typ 1005
                uniqueObjectIdentifier 101
                x 10
                y 10
                parameter[] 3.5
                parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
                orientierung 503
                idStringDialog CONST.1
                <\\ElementCONTROL>
                c (1)
                <ElementCONTROL>
                labelAnfangsKnoten[] /in_sig
                labelEndKnoten[] /out_doubled
                enabledShorted 1
                typ 88
                uniqueObjectIdentifier 102
                x 16
                y 10
                parameter[] 0.0
                parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
                orientierung 503
                idStringDialog CNATC.1
                anzXIN 1
                anzYOUT 2
                libraryPath %s
                <\\ElementCONTROL>
                tDURATION 1.0E-4
                dt 1.0E-5
                solverType 0
                dataContainerSignals[] /out_doubled
                """.formatted(relativeName);

        CircuitFileParser parser = new CircuitFileParser();
        CircuitModel model = parser.parse(new ByteArrayInputStream(ipes.getBytes(StandardCharsets.UTF_8)), circuitFile.toString());
        byte[] serialized = CircuitFileWriter.write(model);
        Files.write(circuitFile, serialized);

        CircuitModel reparsed = parser.parse(circuitFile.toString());
        CircuitModel.ComponentData nativeC = reparsed.getControlComponents().stream()
                .filter(c -> c.getType() == 88)
                .findFirst()
                .orElseThrow();
        assertEquals(relativeName, nativeC.getParameters().get("libraryPath"));
        assertEquals(1.0, ((Number) nativeC.getParameters().get("anzXIN")).doubleValue());
        assertEquals(2.0, ((Number) nativeC.getParameters().get("anzYOUT")).doubleValue());

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationResult result = engine.runSimulation(SimulationConfig.builder()
                .circuitFile(circuitFile.toString())
                .simulationDuration(1.0e-4)
                .stepWidth(1.0e-5)
                .build());
        assertTrue(result.isSuccess(), "Simulation must succeed: " + result.getErrorMessage());
        assertTrue(result.getWarnings().isEmpty(), "Expected no warnings, got: " + result.getWarnings());
        int idx = List.of(result.getSignalNames()).indexOf("out_doubled");
        assertTrue(idx >= 0, "out_doubled must be recorded");
        float[] data = result.getSignalData(idx);
        assertTrue(data.length > 0);
        assertEquals(7.0, data[data.length - 1], 1e-5, "NativeC output should be 2 * 3.5 = 7.0");
    }

    @Test
    void missingLibraryInHeadlessEngineSurfacesWarning() throws Exception {
        Path circuitFile = Files.createTempDirectory("nativec_missing").resolve("missing.ipes");
        String ipes = """
                verbindungLeistungskreisANZAHL 0
                verbindungControlANZAHL 0
                elementANZAHL 0
                controlANZAHL 1
                c (0)
                <ElementCONTROL>
                labelAnfangsKnoten[]
                labelEndKnoten[]
                enabledShorted 1
                typ 88
                uniqueObjectIdentifier 101
                x 16
                y 10
                parameter[] 0.0
                parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
                orientierung 503
                idStringDialog CNATC.1
                anzXIN 1
                anzYOUT 1
                libraryPath does_not_exist_12345.dll
                <\\ElementCONTROL>
                tDURATION 2.0E-5
                dt 1.0E-5
                solverType 0
                """;
        Files.writeString(circuitFile, ipes);
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                .circuitFile(circuitFile.toString())
                .simulationDuration(2.0e-5)
                .stepWidth(1.0e-5)
                .build());
        assertTrue(result.isSuccess());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("NATIVEC_LOAD_ERROR")),
                "Missing DLL should surface NATIVEC_LOAD_ERROR warning, got: " + result.getWarnings());
        assertEquals(1L, result.getWarnings().stream().filter(w -> w.contains("NATIVEC_LOAD_ERROR")).count(),
                "NATIVEC_LOAD_ERROR warning should not be duplicated: " + result.getWarnings());
    }

    @Test
    void stagedNativeLibraryDirResolvesWhenCircuitFilePathIsBareFilename() throws Exception {
        Path library = buildFixtureLibrary(false);
        String relativeName = library.getFileName().toString();
        String stagedDir = library.getParent().toString();

        try (CLibraryCalculator calculator = new CLibraryCalculator(
                1, 2, relativeName, "browser_only.ipes", stagedDir)) {
            calculator.initializeAtSimulationStart(1e-6);
            assertNull(calculator.getLoadError(), () -> "load error: " + calculator.getLoadError());
            calculator._inputSignal[0] = new double[]{3.0};
            calculator._outputSignal[0] = new double[]{0.0};
            calculator._outputSignal[1] = new double[]{0.0};
            AbstractControlCalculatable.setTime(0.0);
            calculator.calculateYOUT(1e-6);
            assertEquals(6.0, calculator._outputSignal[0][0], 1e-12);
        }
    }

    @Test
    void bareBrowserFilenameMissingLibraryIncludesAttachHint() {
        try (CLibraryCalculator calculator = new CLibraryCalculator(
                1, 1, "does_not_exist_12345.dll", "browser_only.ipes", null)) {
            calculator.initializeAtSimulationStart(1e-6);
            assertNotNull(calculator.getLoadError());
            assertTrue(calculator.getLoadError().contains("Attach Library"),
                    "Expected browser guidance hint in load error, got: " + calculator.getLoadError());
        }
    }
}
