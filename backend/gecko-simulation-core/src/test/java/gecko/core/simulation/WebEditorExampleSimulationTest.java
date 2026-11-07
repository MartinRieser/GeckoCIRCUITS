/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end regression tests running the web editor's bundled examples
 * through the headless engine, the same parse-and-simulate path the REST
 * import uses. The classic .ipes test resources carry no scope blocks, so
 * without these the display-only scope handling never sees a full run.
 */
class WebEditorExampleSimulationTest {

    /**
     * The bundled "DC-DC Boost Converter (Step-Up)" example (PWM source, gate,
     * voltmeters, SCOPE.1). A healthy shipped example must simulate without a
     * single engine warning — in particular the display-only SCOPE.1 must not
     * surface as an "unsupported type" validation notice.
     */
    @Test
    void boostConverterExample_runsCleanWithoutWarnings() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/ipes/web_editor_boost.ipes")) {
            assertNotNull(is, "web editor boost example must be on the test classpath");
            CircuitModel model = new CircuitFileParser().parse(
                    new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)),
                    "web_editor_boost.ipes");

            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile("web_editor_boost.ipes")
                    .circuitModel(model)
                    .stepWidth(1e-6)
                    .simulationDuration(0.01)
                    .solverType(SolverType.SOLVER_BE)
                    .build();
            SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);

            assertTrue(result.isSuccess(), () -> "simulation failed: " + result.getErrorMessage());
            assertTrue(result.getWarnings().isEmpty(),
                    () -> "a healthy example circuit must run warning-free: " + result.getWarnings());
        }
    }

    @Test
    void spwmInverterExample_simulatesSuccessfully() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/ipes/spwm_inverter_deadtime.ipes")) {
            assertNotNull(is, "spwm_inverter_deadtime.ipes must be on the test classpath");
            CircuitModel model = new CircuitFileParser().parse(
                    new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)),
                    "spwm_inverter_deadtime.ipes");

            SimulationConfig config = SimulationConfig.builder()
                    .circuitFile("spwm_inverter_deadtime.ipes")
                    .circuitModel(model)
                    .stepWidth(1e-6)
                    .simulationDuration(0.04)
                    .signals(java.util.List.of("v_meas", "v_ref", "v_err", "gate_hi", "gate_lo", "I(L_filter)", "V(L_filter)", "I(S.1)"))
                    .solverType(SolverType.SOLVER_BE)
                    .build();
            SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);

            assertTrue(result.isSuccess(), () -> "simulation failed: " + result.getErrorMessage());
            assertTrue(result.getWarnings().isEmpty(),
                    () -> "example must run warning-free with zero DRC warnings: " + result.getWarnings());
            float[] vMeas = result.getSignalData(0);
            assertNotNull(vMeas, "v_meas signal data must exist");
            for (int i = 0; i < vMeas.length; i++) {
                assertFalse(Float.isNaN(vMeas[i]), "v_meas at step " + i + " must not be NaN");
            }
        }
    }

    @Test
    void allShippedExamples_simulateWithWildcardProbesAndZeroWarnings() throws Exception {
        java.nio.file.Path repoExamplesDir = java.nio.file.Paths.get("..", "..", "examples").normalize();
        if (!java.nio.file.Files.isDirectory(repoExamplesDir)) {
            repoExamplesDir = java.nio.file.Paths.get("examples").normalize();
        }
        assertTrue(java.nio.file.Files.isDirectory(repoExamplesDir),
                "examples directory must exist at " + repoExamplesDir.toAbsolutePath());

        try (var stream = java.nio.file.Files.list(repoExamplesDir)) {
            java.util.List<java.nio.file.Path> ipesFiles = stream
                    .filter(p -> p.toString().endsWith(".ipes"))
                    .sorted()
                    .toList();
            assertFalse(ipesFiles.isEmpty(), "must find shipped .ipes examples");

            for (java.nio.file.Path path : ipesFiles) {
                CircuitModel model = new CircuitFileParser().parse(path.toString());
                double dt = model.getTimeStep() > 0 ? model.getTimeStep() : 1e-6;
                // Run 200 steps of each example with wildcard '*' virtual probe expansion
                // plus all saved dataContainerSignals and graphRegister signals
                double duration = Math.min(model.getSimulationDuration() > 0 ? model.getSimulationDuration() : 1e-3, dt * 200);
                java.util.List<String> requestedSignals = new java.util.ArrayList<>();
                requestedSignals.add("*");
                if (model.getDataContainerSignals() != null) {
                    java.util.Collections.addAll(requestedSignals, model.getDataContainerSignals());
                }
                if (model.getGraphRegisters() != null) {
                    for (var reg : model.getGraphRegisters()) {
                        if (reg.signals() != null) {
                            requestedSignals.addAll(reg.signals());
                        }
                    }
                }
                SimulationConfig config = SimulationConfig.builder()
                        .circuitFile(path.getFileName().toString())
                        .circuitModel(model)
                        .stepWidth(dt)
                        .simulationDuration(duration)
                        .signals(requestedSignals)
                        .solverType(model.getSolverType() != null ? model.getSolverType() : SolverType.SOLVER_BE)
                        .build();
                SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);

                assertTrue(result.isSuccess(),
                        () -> path.getFileName() + " failed: " + result.getErrorMessage());
                assertTrue(result.getWarnings().isEmpty(),
                        () -> path.getFileName() + " emitted unexpected DRC warnings with '*' probes: " + result.getWarnings());
            }
        }
    }
}
