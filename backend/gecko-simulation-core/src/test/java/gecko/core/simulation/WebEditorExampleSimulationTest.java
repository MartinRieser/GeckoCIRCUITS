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
}
