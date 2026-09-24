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

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Creative physics and parameter variation integration tests.
 *
 * Systematically tests:
 * 1. Cold start (0V / 0A) vs. precharged/warm start operating points.
 * 2. Component parameter variations (load resistance, inductance, capacitance).
 * 3. Control block parameter variations (duty cycle, frequency).
 * 4. Simulation resolutions (step widths dt from 0.5 us to 5 us).
 * 5. Simulation durations and runtime scaling.
 * 6. Physical laws (V_out ~= D * V_in, I = V / R, tau = R * C step responses).
 */
class SimulationPhysicsAndParametersTest {

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

    @Test
    @DisplayName("Buck converter: cold start (0V, 0A) starts cleanly from 0V, differing from warm start")
    void testBuckConverterColdStartVsWarmStart() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);

        // 1. Cold start: C.1 initial voltage = 0.0 V, L.1 initial current = 0.0 A
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig coldConfig = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(0.003)
                .parameterOverride("C.1.param1", 0.0)
                .parameterOverride("L.1.param1", 0.0)
                .build();

        SimulationResult coldResult = engine.runSimulation(coldConfig);
        assertTrue(coldResult.isSuccess(), "Cold start simulation failed: " + coldResult.getErrorMessage());

        float[] coldVout = extractSignal(coldResult, "uOUT");
        float[] coldIL = extractSignal(coldResult, "iL1");

        // Verify cold start initial values at step 0
        assertEquals(0.0f, coldVout[0], 0.05f, "Cold start output voltage at t=0 must start at 0V");
        assertEquals(0.0f, coldIL[0], 0.05f, "Cold start inductor current at t=0 must start at 0A");

        // Over first 5 us (step 5), cold start voltage must be in earliest transient rise (< 1.0V)
        assertTrue(coldVout[5] < 1.0f, "Cold start output voltage at 5us should be in early rise (< 1.0V), got: " + coldVout[5]);

        // 2. Warm start: C.1 initial voltage = 5.0 V, L.1 initial current = 4.25 A
        SimulationConfig warmConfig = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(1e-6)
                .simulationDuration(0.003)
                .parameterOverride("C.1.param1", 5.0)
                .parameterOverride("L.1.param1", 4.25)
                .build();

        SimulationResult warmResult = engine.runSimulation(warmConfig);
        assertTrue(warmResult.isSuccess(), "Warm start simulation failed: " + warmResult.getErrorMessage());

        float[] warmVout = extractSignal(warmResult, "uOUT");
        float[] warmIL = extractSignal(warmResult, "iL1");

        // Verify warm start initial values at step 0
        assertEquals(5.0f, warmVout[0], 0.3f, "Warm start output voltage at t=0 must start near 5V");
        assertEquals(4.25f, warmIL[0], 0.35f, "Warm start inductor current at t=0 must start near 4.25A");

        // Cold start and warm start trajectories must be distinctly different in the initial interval (first 30 steps = 30us)
        double earlyColdAvg = calculateAverage(coldVout, 0, 30);
        double earlyWarmAvg = calculateAverage(warmVout, 0, 30);
        assertTrue(earlyWarmAvg - earlyColdAvg > 1.5,
                "Warm start early average (" + earlyWarmAvg + "V) must be significantly higher than cold start (" + earlyColdAvg + "V)");
    }

    @Test
    @DisplayName("Buck converter: duty cycle variations scale steady-state output voltage (Vout ~= D * Vin)")
    void testBuckConverterDutyCycleVariation() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);

        // Input voltage is 12 V DC. We test D = 0.20, 0.40, and 0.70
        double[] duties = {0.20, 0.40, 0.70};
        double[] steadyVoltages = new double[duties.length];

        for (int i = 0; i < duties.length; i++) {
            double d = duties[i];
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile(file.getAbsolutePath())
                    .stepWidth(1e-6)
                    .simulationDuration(0.004) // 4 ms to settle into steady state
                    .parameterOverride("SIGNAL.1.param5", d) // duty cycle
                    .parameterOverride("C.1.param1", d * 12.0) // pre-seed near expected operating point
                    .parameterOverride("L.1.param1", (d * 12.0) / 1.0)
                    .build();

            SimulationResult result = engine.runSimulation(config);
            assertTrue(result.isSuccess(), "Simulation with duty " + d + " failed: " + result.getErrorMessage());

            float[] vout = extractSignal(result, "uOUT");
            // Measure average over the last 1 ms (steps 3000 to 4000)
            steadyVoltages[i] = calculateAverage(vout, 3000, 4000);
        }

        // Check strict monotonic increase with duty cycle
        assertTrue(steadyVoltages[0] < steadyVoltages[1],
                "Vout(0.20)=" + steadyVoltages[0] + " must be < Vout(0.40)=" + steadyVoltages[1]);
        assertTrue(steadyVoltages[1] < steadyVoltages[2],
                "Vout(0.40)=" + steadyVoltages[1] + " must be < Vout(0.70)=" + steadyVoltages[2]);

        // Physical bounds checking (duty cycle D scales average output voltage)
        assertTrue(steadyVoltages[0] >= 2.0 && steadyVoltages[0] <= 3.5,
                "D=0.20 output should be in [2.0V, 3.5V], got: " + steadyVoltages[0]);
        assertTrue(steadyVoltages[1] >= 4.5 && steadyVoltages[1] <= 6.5,
                "D=0.40 output should be in [4.5V, 6.5V], got: " + steadyVoltages[1]);
        assertTrue(steadyVoltages[2] >= 7.5 && steadyVoltages[2] <= 9.5,
                "D=0.70 output should be in [7.5V, 9.5V], got: " + steadyVoltages[2]);
    }

    @Test
    @DisplayName("Buck converter: load resistance variations scale load current per Ohm's law (I = V / R)")
    void testBuckConverterLoadResistanceVariation() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);

        // Load resistor R.Last (LK component 4)
        double[] resistances = {1.0, 2.5, 10.0};
        double[] avgCurrents = new double[resistances.length];

        for (int i = 0; i < resistances.length; i++) {
            double rLoad = resistances[i];
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile(file.getAbsolutePath())
                    .stepWidth(1e-6)
                    .simulationDuration(0.003)
                    .parameterOverride("R.Last.param0", rLoad)
                    .parameterOverride("C.1.param1", 4.8)
                    .parameterOverride("L.1.param1", 4.8 / rLoad)
                    .build();

            SimulationResult result = engine.runSimulation(config);
            assertTrue(result.isSuccess(), "Simulation with R=" + rLoad + " failed: " + result.getErrorMessage());

            float[] iL = extractSignal(result, "iL1");
            // Inductor current average in steady state equals load current
            avgCurrents[i] = calculateAverage(iL, 2000, 3000);
        }

        // Higher resistance must result in lower current
        assertTrue(avgCurrents[0] > avgCurrents[1], "I(1.0 Ohm) must be > I(2.5 Ohm)");
        assertTrue(avgCurrents[1] > avgCurrents[2], "I(2.5 Ohm) must be > I(10.0 Ohm)");

        // Quantitative check: R=1 Ohm has ~4.8 A, R=10 Ohm has ~0.48 A (approx 10x ratio)
        double ratio = avgCurrents[0] / avgCurrents[2];
        assertTrue(ratio >= 8.0 && ratio <= 12.0, "Current ratio should be ~10x, got: " + ratio);
    }

    @ParameterizedTest(name = "Step width dt = {0} s")
    @ValueSource(doubles = {5.0e-7, 1.0e-6, 2.0e-6, 4.0e-6})
    @DisplayName("Simulation resolution convergence: varying step size preserves stability and voltage accuracy")
    void testSimulationTimeStepResolutionConvergence(double dt) throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(dt)
                .simulationDuration(0.002) // 2 ms
                .parameterOverride("C.1.param1", 4.9)
                .parameterOverride("L.1.param1", 4.2)
                .build();

        SimulationResult result = engine.runSimulation(config);
        assertTrue(result.isSuccess(), "Simulation failed at dt=" + dt + ": " + result.getErrorMessage());

        float[] vout = extractSignal(result, "uOUT");
        assertNotNull(vout);
        assertTrue(vout.length > 0);

        // Assert no NaN or Infinity
        for (int i = 0; i < vout.length; i++) {
            assertFalse(Float.isNaN(vout[i]), "NaN detected at step " + i);
            assertFalse(Float.isInfinite(vout[i]), "Infinity detected at step " + i);
        }

        // Average output voltage in the second half of the run should consistently be within steady operating range
        int midPoint = vout.length / 2;
        double avgV = calculateAverage(vout, midPoint, vout.length);
        assertTrue(avgV >= 4.5 && avgV <= 6.5,
                "At dt=" + dt + ", steady voltage should be in [4.5V, 6.5V], got: " + avgV);
    }

    @Test
    @DisplayName("Simulation duration scaling: step count matches duration / dt and ripple remains bounded")
    void testSimulationDurationScaling() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);
        double dt = 1e-6;

        double[] durations = {0.001, 0.002, 0.005};

        for (double duration : durations) {
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile(file.getAbsolutePath())
                    .stepWidth(dt)
                    .simulationDuration(duration)
                    .parameterOverride("C.1.param1", 4.9)
                    .parameterOverride("L.1.param1", 4.2)
                    .build();

            SimulationResult result = engine.runSimulation(config);
            assertTrue(result.isSuccess(), "Run for duration " + duration + " failed");

            float[] vout = extractSignal(result, "uOUT");
            int expectedSteps = (int) Math.round(duration / dt);
            // Allow +/- 2 steps for loop boundary rounding
            assertTrue(Math.abs(vout.length - expectedSteps) <= 2,
                    "Expected ~" + expectedSteps + " steps, got " + vout.length);

            // Output voltage must remain bounded within ripple bounds
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;
            for (float v : vout) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
            assertTrue(min >= 3.8f && max <= 6.8f,
                    "Waveform out of expected steady ripple bounds: min=" + min + ", max=" + max);
        }
    }

    @Test
    @DisplayName("Capacitor initial voltage: positive (+6V), negative (-6V), and zero (0V) polarities")
    void testCapacitorInitialConditionsPolarities() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);

        double[] initialVoltages = {6.0, 0.0, -6.0};

        for (double v0 : initialVoltages) {
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile(file.getAbsolutePath())
                    .stepWidth(1e-6)
                    .simulationDuration(0.001)
                    .parameterOverride("C.1.param1", v0)
                    .parameterOverride("L.1.param1", 0.0)
                    .build();

            SimulationResult result = engine.runSimulation(config);
            assertTrue(result.isSuccess(), "Simulation with v0=" + v0 + " failed: " + result.getErrorMessage());

            float[] vout = extractSignal(result, "uOUT");
            // After 1 step (1 us), the capacitor discharges through load resistor (RC = 20 us) by e^(-1/20) ~= 0.952
            assertEquals((float) (v0 * 0.952), vout[0], 0.2f,
                    "First-step capacitor voltage should reflect initial voltage v0=" + v0 + " with 1us RC discharge");
        }
    }

    @Test
    @DisplayName("Inductor initial current: positive (+3A), zero (0A), and reverse (-3A) currents")
    void testInductorInitialConditionsPolarities() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);

        double[] initialCurrents = {3.0, 0.0, -3.0};

        for (double i0 : initialCurrents) {
            HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile(file.getAbsolutePath())
                    .stepWidth(1e-6)
                    .simulationDuration(0.001)
                    .parameterOverride("C.1.param1", 5.0)
                    .parameterOverride("L.1.param1", i0)
                    .build();

            SimulationResult result = engine.runSimulation(config);
            assertTrue(result.isSuccess(), "Simulation with i0=" + i0 + " failed: " + result.getErrorMessage());

            float[] iL = extractSignal(result, "iL1");
            if (i0 > 0) {
                // Forward inductor current discharges through load resistor (L/R = 20 us) by e^(-1/20) ~= 0.918
                assertEquals((float) (i0 * 0.918), iL[0], 0.35f,
                        "First-step forward inductor current should reflect initial current i0=" + i0 + " with 1us L/R decay");
            } else if (i0 == 0.0) {
                assertEquals(0.0f, iL[0], 0.05f, "Zero initial inductor current must start at 0A");
            } else {
                // Reverse current is immediately blocked by the freewheeling diode and switch
                assertTrue(Math.abs(iL[0]) < 0.05f,
                        "Reverse initial current i0=" + i0 + " must be blocked by diode to ~0A, got: " + iL[0]);
            }
        }
    }

    @Test
    @DisplayName("Switching frequency variation (50kHz vs 200kHz): higher frequency halves peak-to-peak ripple")
    void testSwitchingFrequencyScalingRipple() throws Exception {
        File file = resolveCircuit(BUCK_CIRCUIT_REL);

        // Run at 50 kHz
        HeadlessSimulationEngine engine50 = new HeadlessSimulationEngine();
        SimulationConfig config50 = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(0.5e-6)
                .simulationDuration(0.002)
                .parameterOverride("SIGNAL.1.param2", 50000.0)
                .build();
        SimulationResult result50 = engine50.runSimulation(config50);
        assertTrue(result50.isSuccess());
        float[] iL50 = extractSignal(result50, "iL1");

        // Run at 200 kHz
        HeadlessSimulationEngine engine200 = new HeadlessSimulationEngine();
        SimulationConfig config200 = SimulationConfig.builder()
                .circuitFile(file.getAbsolutePath())
                .stepWidth(0.5e-6)
                .simulationDuration(0.002)
                .parameterOverride("SIGNAL.1.param2", 200000.0)
                .build();
        SimulationResult result200 = engine200.runSimulation(config200);
        assertTrue(result200.isSuccess());
        float[] iL200 = extractSignal(result200, "iL1");

        // Calculate peak-to-peak ripple in steady state (last 1000 steps = 0.5 ms)
        float min50 = Float.MAX_VALUE, max50 = -Float.MAX_VALUE;
        for (int i = iL50.length - 1000; i < iL50.length; i++) {
            if (iL50[i] < min50) min50 = iL50[i];
            if (iL50[i] > max50) max50 = iL50[i];
        }
        float ripple50 = max50 - min50;

        float min200 = Float.MAX_VALUE, max200 = -Float.MAX_VALUE;
        for (int i = iL200.length - 1000; i < iL200.length; i++) {
            if (iL200[i] < min200) min200 = iL200[i];
            if (iL200[i] > max200) max200 = iL200[i];
        }
        float ripple200 = max200 - min200;

        // Peak-to-peak ripple delta_I = (Vin - Vout) / (L * f) * D
        // At 50 kHz, ripple should be ~4x larger than at 200 kHz
        assertTrue(ripple50 > ripple200 * 2.5f,
                "50kHz ripple (" + ripple50 + "A) must be significantly larger than 200kHz ripple (" + ripple200 + "A)");
    }
}

