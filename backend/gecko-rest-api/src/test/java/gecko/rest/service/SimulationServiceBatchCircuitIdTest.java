package gecko.rest.service;

import gecko.core.io.CircuitModel;
import gecko.rest.model.BatchJobStatus;
import gecko.rest.model.BatchSimulationRequest;
import gecko.rest.model.BatchSimulationResponse;
import gecko.rest.model.SimulationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies batch parametric sweeps targeting an active {@code circuitId} execute
 * cleanly without mutating the stored live {@link CircuitModel}.
 */
class SimulationServiceBatchCircuitIdTest {

    private CircuitFileService circuitFileService;
    private SimulationService simulationService;
    private String circuitId;

    @BeforeEach
    void setUp() throws Exception {
        circuitFileService = new CircuitFileService();
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

    @Test
    void submitBatch_withCircuitIdAndLinearSweep_variesCurrentWithoutMutatingStoredModel() throws Exception {
        CircuitModel.ComponentData originalLoad = findResistor(circuitFileService.getModel(circuitId), "R.Last");
        double originalParam0 = ((Number) originalLoad.getParameters().get("param0")).doubleValue();

        BatchSimulationRequest req = new BatchSimulationRequest();
        req.setCircuitId(circuitId);
        req.setMaxConcurrent(3);

        BatchSimulationRequest.LinearSweep sweep = new BatchSimulationRequest.LinearSweep();
        sweep.setParameterPath("R.Last.param0");
        sweep.setStartValue(1.0);
        sweep.setEndValue(10.0);
        sweep.setPoints(3);
        req.setLinearSweep(sweep);

        BatchSimulationRequest.SimulationSettings settings = new BatchSimulationRequest.SimulationSettings();
        settings.setSimulationTime(0.002);
        settings.setTimeStep(1e-6);
        req.setSimulationSettings(settings);

        BatchSimulationResponse batch = simulationService.submitBatch(req);
        assertEquals(3, batch.getTotalSimulations());

        long deadline = System.currentTimeMillis() + 30_000L;
        BatchJobStatus status = simulationService.getBatchStatus(batch.getBatchId());
        while (status != null && !status.isDone() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L);
            status = simulationService.getBatchStatus(batch.getBatchId());
        }

        assertNotNull(status);
        assertTrue(status.isDone(), "Batch job should finish within 30s");
        assertEquals(3, status.getCompleted());
        assertEquals(0, status.getFailed());

        double[] avgCurrents = new double[3];
        for (int r = 0; r < 3; r++) {
            SimulationResponse simResp = simulationService.getSimulation(batch.getSimulationIds().get(r));
            assertNotNull(simResp);
            assertEquals(SimulationResponse.SimulationStatus.COMPLETED, simResp.getStatus());
            double[] il = findSignal(simResp, "iL1");
            double sum = 0.0;
            for (double v : il) {
                sum += v;
            }
            avgCurrents[r] = sum / il.length;
        }

        // Higher load resistance yields noticeably lower inductor current
        assertTrue(avgCurrents[0] > avgCurrents[1], "1 Ohm mean iL1 should exceed 5.5 Ohm mean iL1");
        assertTrue(avgCurrents[1] > avgCurrents[2], "5.5 Ohm mean iL1 should exceed 10 Ohm mean iL1");

        // Original in-memory model must remain untouched
        CircuitModel.ComponentData afterLoad = findResistor(circuitFileService.getModel(circuitId), "R.Last");
        double afterParam0 = ((Number) afterLoad.getParameters().get("param0")).doubleValue();
        assertEquals(originalParam0, afterParam0, 1e-12, "Stored live CircuitModel must not be mutated");
    }

    private static CircuitModel.ComponentData findResistor(CircuitModel model, String name) {
        return model.getCircuitComponents().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static double[] findSignal(SimulationResponse resp, String signalName) {
        return resp.getResults().entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(signalName) || e.getKey().endsWith("/" + signalName))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing signal: " + signalName));
    }
}
