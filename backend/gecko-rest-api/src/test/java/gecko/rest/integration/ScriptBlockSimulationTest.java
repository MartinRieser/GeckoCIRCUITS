package gecko.rest.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import gecko.core.allg.SolverType;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that a circuit containing a typ-61 script block (microcontroller
 * control logic) simulates through the headless engine — the path the REST
 * API and the desktop sidecar use, without the Swing GUI jar or a JS engine.
 * Script blocks are executed by the core's own ScriptBlockCalculator.
 */
class ScriptBlockSimulationTest {

    private static final String FIXTURE = "/circuits/interleaved_pfc_50v.ipes";
    private static final double DT = 1e-6;
    private static final double DURATION = 20e-3;

    private static Path materializeFixture(boolean blankScript) throws IOException {
        byte[] raw;
        try (InputStream in = ScriptBlockSimulationTest.class.getResourceAsStream(FIXTURE)) {
            raw = in.readAllBytes();
        }
        if (!blankScript) {
            // fixtures are gzip-compressed; the parser detects the magic bytes itself
            Path temp = Files.createTempFile("gecko-script-test-", ".ipes");
            Files.write(temp, raw);
            return temp;
        }
        String text;
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw))) {
            text = new String(gz.readAllBytes(), StandardCharsets.UTF_8);
        }
        int start = text.indexOf("<sourceCode>");
        int end = text.indexOf("<\\sourceCode>", start);
        if (start < 0 || end < 0) {
            throw new IOException("fixture has no script block");
        }
        String patched = text.substring(0, start) + "<sourceCode>yOUT[0] = 0;" + text.substring(end);
        Path blanked = Files.createTempFile("gecko-script-test-blanked-", ".ipes");
        Files.writeString(blanked, patched, StandardCharsets.UTF_8);
        return blanked;
    }

    private static SimulationResult simulate(Path circuitFile) {
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(circuitFile.toString())
                .solverType(SolverType.SOLVER_BE)
                .stepWidth(DT)
                .simulationDuration(DURATION)
                .build();
        return new HeadlessSimulationEngine().runSimulation(config);
    }

    @Test
    void scriptBlockCircuitSimulatesHeadlessly() throws IOException {
        Path circuit = materializeFixture(false);
        try {
            SimulationResult result = simulate(circuit);
            assertTrue(result.isSuccess(), () -> "simulation failed: " + result.getErrorMessage());
            assertTrue(result.getSignalNames().length > 0, "no signals recorded");
            assertTrue(result.getTotalTimeSteps() >= (long) (DURATION / DT),
                    "expected at least " + (long) (DURATION / DT) + " steps but got " + result.getTotalTimeSteps());
        } finally {
            Files.deleteIfExists(circuit);
        }
    }

    @Test
    void scriptBlockInfluencesTheResult() throws IOException {
        Path controlled = materializeFixture(false);
        Path blanked = materializeFixture(true);
        try {
            SimulationResult withScript = simulate(controlled);
            SimulationResult withoutScript = simulate(blanked);
            assertTrue(withScript.isSuccess(), withScript::getErrorMessage);
            assertTrue(withoutScript.isSuccess(), withoutScript::getErrorMessage);
            assertEquals(withScript.getSignalNames().length, withoutScript.getSignalNames().length);

            // if the script failed to compile, its outputs stay at their initial
            // value and both runs would produce identical waveforms
            boolean differs = false;
            for (int i = 0; i < withScript.getSignalNames().length && !differs; i++) {
                float[] a = withScript.getSignalData(i);
                float[] b = withoutScript.getSignalData(i);
                differs = !Arrays.equals(a, b);
            }
            assertNotEquals(false, differs,
                    "script block had no effect on any signal — it probably did not execute");
        } finally {
            Files.deleteIfExists(controlled);
            Files.deleteIfExists(blanked);
        }
    }
}
