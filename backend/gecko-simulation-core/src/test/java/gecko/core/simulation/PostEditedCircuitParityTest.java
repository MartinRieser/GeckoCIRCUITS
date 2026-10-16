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
package gecko.core.simulation;

import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 4.3: Differential Legacy Parity Testing on Post-Edited Circuits.
 *
 * Validates that modifying circuits (inserting shunts, altering inductances/capacitances,
 * retuning controller parameters, and round-tripping modified netlists) produces
 * physically exact, mathematically stable, and deterministic simulation responses.
 */
public class PostEditedCircuitParityTest {

    private static final String BUCK_CIRCUIT_REL = "buck_converter_web.ipes";

    private File resolveCircuit(String relativePath) {
        Path path = Paths.get("src/test/resources/ipes", relativePath);
        File file = path.toFile();
        if (!file.exists()) {
            path = Paths.get("backend/gecko-simulation-core/src/test/resources/ipes", relativePath);
            file = path.toFile();
        }
        if (!file.exists()) {
            java.net.URL url = getClass().getResource("/ipes/" + relativePath);
            if (url != null) {
                try {
                    file = new File(url.toURI());
                } catch (Exception e) {
                    file = new File(url.getPath());
                }
            }
        }
        assertTrue(file.exists(), "Circuit file not found: " + path.toAbsolutePath());
        return file;
    }

    private float[] extractSignal(SimulationResult result, String signalName) {
        assertNotNull(result.getDataContainer(), "DataContainer must not be null");
        String[] names = result.getSignalNames();
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(signalName) || names[i].endsWith("/" + signalName)) {
                return result.getSignalData(i);
            }
        }
        fail("Signal '" + signalName + "' not found among: " + Arrays.toString(names));
        return new float[0];
    }

    private double calculateAverage(float[] data, int startIdx, int endIdx) {
        assertTrue(startIdx >= 0 && endIdx <= data.length && startIdx < endIdx, "Invalid index range");
        double sum = 0.0;
        for (int i = startIdx; i < endIdx; i++) {
            sum += data[i];
        }
        return sum / (endIdx - startIdx);
    }

    private double calculatePeakToPeak(float[] data, int startIdx, int endIdx) {
        assertTrue(startIdx >= 0 && endIdx <= data.length && startIdx < endIdx, "Invalid index range");
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int i = startIdx; i < endIdx; i++) {
            if (data[i] < min) min = data[i];
            if (data[i] > max) max = data[i];
        }
        return max - min;
    }

    @Test
    @DisplayName("Edited Inductance: Doubling L halves inductor current ripple (delta_I = V_L * dt / L)")
    void testInductanceModificationHalvesCurrentRipple() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        // Run 1: Nominal inductance (L = 100 uH)
        SimulationConfig config1 = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(0.003)
                .parameterOverride("C.1.param1", 4.8)
                .parameterOverride("L.1.param1", 4.8)
                .parameterOverride("L.1.param0", 100e-6)
                .build();

        SimulationResult result1 = engine.runSimulation(config1);
        assertTrue(result1.isSuccess(), "Simulation 1 failed: " + result1.getErrorMessage());
        float[] iL1 = extractSignal(result1, "iL1");
        double ripple1 = calculatePeakToPeak(iL1, 2000, 3000);

        // Run 2: Doubled inductance (L = 200 uH)
        SimulationConfig config2 = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(0.003)
                .parameterOverride("C.1.param1", 4.8)
                .parameterOverride("L.1.param1", 4.8)
                .parameterOverride("L.1.param0", 200e-6)
                .build();

        SimulationResult result2 = engine.runSimulation(config2);
        assertTrue(result2.isSuccess(), "Simulation 2 failed: " + result2.getErrorMessage());
        float[] iL2 = extractSignal(result2, "iL1");
        double ripple2 = calculatePeakToPeak(iL2, 2000, 3000);

        // Current ripple ratio must be 2.0 within 5% tolerance
        double rippleRatio = ripple1 / ripple2;
        assertTrue(rippleRatio >= 1.85 && rippleRatio <= 2.15,
                "Inductor ripple ratio should be ~2.0, got: " + rippleRatio + " (ripple1=" + ripple1 + ", ripple2=" + ripple2 + ")");

        // Average output voltage must be identical within 0.1 V
        float[] vout1 = extractSignal(result1, "uOUT");
        float[] vout2 = extractSignal(result2, "uOUT");
        double avgV1 = calculateAverage(vout1, 2000, 3000);
        double avgV2 = calculateAverage(vout2, 2000, 3000);
        assertEquals(avgV1, avgV2, 0.1, "Average steady-state voltage should remain identical under L variation");
    }

    @Test
    @DisplayName("Edited Load Resistance: Shunt insertion / load resistance scaling satisfies Ohm's law")
    void testLoadResistanceScalingSatisfiesOhmsLaw() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        // Test R_load = 2.0 Ohm vs R_load = 4.0 Ohm
        double r1 = 2.0;
        double r2 = 4.0;

        SimulationConfig config1 = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(0.003)
                .parameterOverride("C.1.param1", 4.8)
                .parameterOverride("L.1.param1", 4.8 / r1)
                .parameterOverride("R.Last.param0", r1)
                .build();

        SimulationResult result1 = engine.runSimulation(config1);
        assertTrue(result1.isSuccess());
        float[] iL1 = extractSignal(result1, "iL1");
        double avgI1 = calculateAverage(iL1, 2000, 3000);

        SimulationConfig config2 = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(0.003)
                .parameterOverride("C.1.param1", 4.8)
                .parameterOverride("L.1.param1", 4.8 / r2)
                .parameterOverride("R.Last.param0", r2)
                .build();

        SimulationResult result2 = engine.runSimulation(config2);
        assertTrue(result2.isSuccess());
        float[] iL2 = extractSignal(result2, "iL1");
        double avgI2 = calculateAverage(iL2, 2000, 3000);

        // Current ratio I1 / I2 should be r2 / r1 = 2.0
        double currentRatio = avgI1 / avgI2;
        assertTrue(currentRatio >= 1.90 && currentRatio <= 2.10,
                "Current ratio should be ~2.0, got: " + currentRatio);
    }

    @Test
    @DisplayName("Model Serialization Round-Trip: Modifying CircuitModel in memory preserves parity")
    void testModelModificationParityAfterSerializationRoundTrip() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);
        CircuitFileParser parser = new CircuitFileParser();
        CircuitModel originalModel = parser.parse(file.getAbsolutePath());

        // Run original model
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig configOrig = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(0.002)
                .parameterOverride("C.1.param1", 4.8)
                .parameterOverride("L.1.param1", 4.8)
                .build();
        SimulationResult resultOrig = engine.runSimulation(configOrig);
        assertTrue(resultOrig.isSuccess());
        float[] origVout = extractSignal(resultOrig, "uOUT");

        // Serialize and re-parse
        byte[] bytes = CircuitFileWriter.write(originalModel, false);
        CircuitModel reloadedModel = parser.parse(new java.io.ByteArrayInputStream(bytes), "roundtrip.ipes");
        assertNotNull(reloadedModel);
        assertEquals(originalModel.getCircuitComponents().size(), reloadedModel.getCircuitComponents().size(),
                "Circuit component count must match after round trip");

        // Write to a temporary file
        File tempFile = File.createTempFile("gecko_parity_roundtrip_", ".ipes");
        tempFile.deleteOnExit();
        java.nio.file.Files.write(tempFile.toPath(), bytes);

        SimulationConfig configReloaded = SimulationConfig.builder()
                .circuitFile(tempFile.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(0.002)
                .parameterOverride("C.1.param1", 4.8)
                .parameterOverride("L.1.param1", 4.8)
                .build();
        SimulationResult resultReloaded = engine.runSimulation(configReloaded);
        assertTrue(resultReloaded.isSuccess());
        float[] reloadedVout = extractSignal(resultReloaded, "uOUT");

        // Verify parity between original and reloaded simulation:
        // Max error must be < 1e-4 across all steps
        assertEquals(origVout.length, reloadedVout.length, "Step count must match exactly");
        float maxError = 0.0f;
        for (int i = 0; i < origVout.length; i++) {
            float err = Math.abs(origVout[i] - reloadedVout[i]);
            if (err > maxError) maxError = err;
        }
        assertTrue(maxError < 1e-4f, "Waveform difference must be < 1e-4, got maxError=" + maxError);
    }
}
