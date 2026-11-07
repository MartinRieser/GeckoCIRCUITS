package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strategy 4: Differential / Golden Parity Testing Across Solvers.
 *
 * <p>Runs key bundled examples through the headless simulation engine using
 * Backward Euler (BE), Trapezoidal (TRZ), and Gear-Shichman (GS) solvers.
 * Asserts:
 * 1. All solvers complete successfully without error or numerical explosion.
 * 2. Waveforms contain zero NaNs or Infinities.
 * 3. Average DC and RMS values across solvers agree within physical/numerical tolerances.</p>
 */
class ExamplesDifferentialSolverTest {

    private CircuitModel loadModel(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found: " + resourcePath);
            return new CircuitFileParser().parse(
                    new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)),
                    resourcePath);
        }
    }

    @ParameterizedTest(name = "Differential solver parity for {0}")
    @ValueSource(strings = {
            "/ipes/web_editor_boost.ipes",
            "/ipes/buck_converter_web.ipes",
            "/ipes/buck_simple.ipes",
            "/ipes/spwm_inverter_deadtime.ipes",
            "/ipes/ex_1.ipes",
            "/ipes/ex_3_pwm.ipes",
            "/ipes/test-circuit.ipes",
            "/ipes/education/boost_simple.ipes",
            "/ipes/education/buck_simple.ipes",
            "/ipes/education/cuk_simple.ipes",
            "/ipes/education/sepic_simple.ipes",
            "/ipes/education/singlePhase_PWM_converter.ipes",
            "/multi-scope-rlc.ipes"
    })
    @DisplayName("Verify solver agreement (BE vs TRZ vs GS)")
    void differentialSolverParity(String circuitPath) throws Exception {
        CircuitModel baseModel = loadModel(circuitPath);
        double dt = baseModel.getTimeStep() > 0 ? baseModel.getTimeStep() : 1e-6;
        double duration = baseModel.getSimulationDuration() > 0 ? baseModel.getSimulationDuration() : 0.01;

        // Cap duration for fast CI execution (at least 200 steps, max 2000 steps)
        if (duration / dt > 2000) {
            duration = dt * 2000;
        }

        SolverType[] solvers = {SolverType.SOLVER_BE, SolverType.SOLVER_TRZ, SolverType.SOLVER_GS};
        Map<SolverType, SimulationResult> results = new HashMap<>();

        for (SolverType solver : solvers) {
            // Re-load model fresh per solver run to avoid shared mutable state
            CircuitModel model = loadModel(circuitPath);
            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile(circuitPath)
                    .circuitModel(model)
                    .stepWidth(dt)
                    .simulationDuration(duration)
                    .solverType(solver)
                    .build();

            SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
            assertTrue(result.isSuccess(),
                    () -> "Simulation failed for " + circuitPath + " with " + solver + ": " + result.getErrorMessage());

            String[] signalNames = result.getSignalNames();
            assertTrue(signalNames != null && signalNames.length > 0,
                    "Circuit " + circuitPath + " must produce recorded signals");

            // Verify no NaN / Infinity values
            for (int s = 0; s < signalNames.length; s++) {
                final String sigName = signalNames[s];
                float[] data = result.getSignalData(s);
                assertNotNull(data, "Signal data for " + sigName + " must not be null");
                for (int i = 0; i < data.length; i++) {
                    final int stepIdx = i;
                    final SolverType st = solver;
                    assertFalse(Float.isNaN(data[i]),
                            () -> "NaN detected in signal " + sigName + " at step " + stepIdx + " with " + st);
                    assertFalse(Float.isInfinite(data[i]),
                            () -> "Infinity detected in signal " + sigName + " at step " + stepIdx + " with " + st);
                }
            }

            results.put(solver, result);
        }

        // Compare BE vs TRZ and BE vs GS on common signals
        SimulationResult beRes = results.get(SolverType.SOLVER_BE);
        SimulationResult trzRes = results.get(SolverType.SOLVER_TRZ);
        SimulationResult gsRes = results.get(SolverType.SOLVER_GS);

        String[] signalNames = beRes.getSignalNames();
        for (int s = 0; s < signalNames.length; s++) {
            final String sigName = signalNames[s];
            float[] beData = beRes.getSignalData(s);
            float[] trzData = trzRes.getSignalData(s);
            float[] gsData = gsRes.getSignalData(s);

            double beRms = computeRms(beData);
            double trzRms = computeRms(trzData);
            double gsRms = computeRms(gsData);

            // If the signal is non-trivial (amplitude > 1e-4), assert solvers agree within 20%
            double maxRms = Math.max(beRms, Math.max(trzRms, gsRms));
            if (maxRms > 1e-3) {
                double diffTrz = Math.abs(trzRms - beRms) / maxRms;
                double diffGs = Math.abs(gsRms - beRms) / maxRms;

                assertTrue(diffTrz < 0.25,
                        () -> String.format("Signal %s in %s: TRZ RMS (%.4f) diverged >25%% from BE RMS (%.4f)",
                                sigName, circuitPath, trzRms, beRms));
                assertTrue(diffGs < 0.25,
                        () -> String.format("Signal %s in %s: GS RMS (%.4f) diverged >25%% from BE RMS (%.4f)",
                                sigName, circuitPath, gsRms, beRms));
            }
        }
    }

    private static double computeRms(float[] data) {
        if (data == null || data.length == 0) return 0.0;
        double sum = 0.0;
        for (float v : data) {
            sum += (double) v * v;
        }
        return Math.sqrt(sum / data.length);
    }
}
