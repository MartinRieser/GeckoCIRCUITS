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
package gecko.rest.service;

import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse;
import gecko.rest.model.circuit.ComponentPatchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import gecko.core.io.CircuitModel;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test verifying that component edits in CircuitEditService
 * (e.g. setting C.1 to 0V and L.1 to 0A, or varying duty cycle / load resistance)
 * directly and physically affect the simulation results.
 */
class SimulationParameterSweepTest {

    private CircuitFileService circuitFileService;
    private CircuitEditService editService;
    private SimulationService simulationService;
    private String circuitId;

    @BeforeEach
    void setUp() throws Exception {
        circuitFileService = new CircuitFileService();
        editService = new CircuitEditService(circuitFileService);
        simulationService = new SimulationService(circuitFileService);

        Path ipesPath = Paths.get("../gecko-simulation-core/src/test/resources/ipes/buck_converter_web.ipes");
        if (!Files.exists(ipesPath)) {
            ipesPath = Paths.get("backend/gecko-simulation-core/src/test/resources/ipes/buck_converter_web.ipes");
        }
        assertTrue(Files.exists(ipesPath), "buck_converter_web.ipes must exist");

        byte[] bytes = Files.readAllBytes(ipesPath);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        circuitId = circuitFileService.loadCircuit(base64, "buck_converter_web.ipes").circuitId();
    }

    private SimulationResult runSimulationSynchronously(double timeStep, double duration) {
        SimulationRequest req = new SimulationRequest();
        req.setCircuitId(circuitId);
        req.setTimeStep(timeStep);
        req.setSimulationTime(duration);
        req.setBackend("core");
        SimulationConfig config = simulationService.buildSimulationConfig(req);
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        return engine.runSimulation(config);
    }

    private float[] extractSignal(SimulationResult result, String name) {
        DataContainerGlobal dc = result.getDataContainer();
        assertNotNull(dc);
        for (int i = 0; i < dc.getRowLength(); i++) {
            if (dc.getSignalName(i).equalsIgnoreCase(name) || dc.getSignalName(i).endsWith("/" + name)) {
                return result.getSignalData(i);
            }
        }
        fail("Signal not found: " + name);
        return new float[0];
    }

    @Test
    @DisplayName("Patching C.1 to 0V and L.1 to 0A causes simulation to cleanly cold-start from 0V")
    void testPatchingInitialConditionsToZeroDirectlyAffectsSimulation() {
        // 1. Initial run without modification (preloaded tutorial with uC0=5V, iL0=4.25A)
        SimulationResult defaultResult = runSimulationSynchronously(1e-6, 0.002);
        assertTrue(defaultResult.isSuccess());
        float[] defaultVout = extractSignal(defaultResult, "uOUT");
        float[] defaultIL = extractSignal(defaultResult, "iL1");

        // The default tutorial circuit starts around 5V and 4.25A
        assertEquals(5.0f, defaultVout[0], 0.3f, "Default tutorial circuit starts near 5V");
        assertEquals(4.25f, defaultIL[0], 0.35f, "Default tutorial circuit starts near 4.25A");

        // 2. User changes C.1 to 0V and L.1 to 0A via edit service (the exact user bug report scenario)
        editService.patchComponent(circuitId, "C.1",
                new ComponentPatchRequest(null, null, null, null, Map.of("param1", 0.0)));
        editService.patchComponent(circuitId, "L.1",
                new ComponentPatchRequest(null, null, null, null, Map.of("param1", 0.0)));

        // 3. Re-run simulation
        SimulationResult coldResult = runSimulationSynchronously(1e-6, 0.002);
        assertTrue(coldResult.isSuccess());
        float[] coldVout = extractSignal(coldResult, "uOUT");
        float[] coldIL = extractSignal(coldResult, "iL1");

        // Step 0 must now be 0.0V and 0.0A!
        assertEquals(0.0f, coldVout[0], 0.05f, "After setting C.1 to 0V, output voltage at t=0 must be 0V");
        assertEquals(0.0f, coldIL[0], 0.05f, "After setting L.1 to 0A, inductor current at t=0 must be 0A");

        // Early cold trajectory is distinctly different from the warm-start default
        assertTrue(defaultVout[0] - coldVout[0] > 4.5f, "Default and cold start must differ by >4.5V at step 0");
    }

    @Test
    @DisplayName("Patching load resistor scales load current inversely (1 Ohm vs 10 Ohm)")
    void testPatchingLoadResistanceScalesCurrent() {
        // Run with default 1 Ohm load
        SimulationResult r1Result = runSimulationSynchronously(1e-6, 0.003);
        assertTrue(r1Result.isSuccess());
        float[] iL1 = extractSignal(r1Result, "iL1");
        double avgI1 = 0.0;
        for (int i = 2000; i < iL1.length; i++) avgI1 += iL1[i];
        avgI1 /= (iL1.length - 2000);

        // Patch load resistance from 1 Ohm to 10 Ohm
        editService.patchComponent(circuitId, "R.Last",
                new ComponentPatchRequest(null, null, null, null, Map.of("param0", 10.0)));

        // Re-run simulation
        SimulationResult r10Result = runSimulationSynchronously(1e-6, 0.003);
        assertTrue(r10Result.isSuccess());
        float[] iL10 = extractSignal(r10Result, "iL1");
        double avgI10 = 0.0;
        for (int i = 2000; i < iL10.length; i++) avgI10 += iL10[i];
        avgI10 /= (iL10.length - 2000);

        // Current should drop approximately 10-fold (Vin ~ 12V, D ~ 0.5 -> Vout ~ 6V, I1 ~ 6A, I10 ~ 0.6A)
        assertTrue(avgI1 > 4.5 && avgI1 < 7.5, "1 Ohm current should be ~5.9A, got: " + avgI1);
        assertTrue(avgI10 > 0.45 && avgI10 < 0.75, "10 Ohm current should be ~0.59A, got: " + avgI10);
        assertTrue(avgI1 / avgI10 >= 8.0, "Current ratio should be ~10x");
    }

    @Test
    @DisplayName("Patching non-zero capacitor initial voltage (8V) reflects in initial output voltage")
    void testPatchingCustomInitialVoltage() {
        editService.patchComponent(circuitId, "C.1",
                new ComponentPatchRequest(null, null, null, null, Map.of("param1", 8.0)));

        SimulationResult result = runSimulationSynchronously(1e-6, 0.001);
        assertTrue(result.isSuccess());
        float[] vout = extractSignal(result, "uOUT");

        // Capacitor discharges from 8V by e^(-1/20) ~= 7.6V
        assertEquals(8.0f * 0.952f, vout[0], 0.2f, "Capacitor initial voltage of 8V must be applied");
    }
}
