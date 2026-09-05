package gecko.rest.integration;

import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse;
import gecko.rest.service.SimulationService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for the simulation REST API.
 * Tests the full request/response flow through all layers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        simulationService.clearAll();
    }

    @Test
    void healthEndpoint_isAccessible() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void fullSimulationWorkflow_submitAndRetrieve() throws Exception {
        // 1. Submit a simulation
        SimulationRequest request = new SimulationRequest("test.ipes", 0.001, 1e-6);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulationId").exists())
                .andExpect(jsonPath("$.status").exists()) // May transition from PENDING before serialization
                .andReturn();

        // Extract simulation ID
        String responseJson = submitResult.getResponse().getContentAsString();
        SimulationResponse submitResponse = objectMapper.readValue(responseJson, SimulationResponse.class);
        String simulationId = submitResponse.getSimulationId();

        assertNotNull(simulationId);

        // 2. Check simulation status
        mockMvc.perform(get("/api/v1/simulations/{id}", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value(simulationId));

        // 3. Wait for simulation to complete (short simulation)
        TimeUnit.MILLISECONDS.sleep(500);

        // 4. Check progress
        mockMvc.perform(get("/api/v1/simulations/{id}/progress", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value(simulationId));

        // 5. List all simulations
        mockMvc.perform(get("/api/v1/simulations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.simulations").exists());
    }

    @Test
    void submitSimulation_withParameters_acceptsRequest() throws Exception {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.01, 1e-6);
        request.setParameters(java.util.Map.of(
                "R1", 100.0,
                "C1", 1e-6
        ));

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulationId").exists());
    }

    @Test
    void validationErrors_returnBadRequest() throws Exception {
        // Missing required fields
        String invalidJson = "{}";

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonExistentSimulation_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/{id}", "non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelSimulation_workflow() throws Exception {
        // Submit a longer simulation
        SimulationRequest request = new SimulationRequest("test.ipes", 1.0, 1e-6);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = submitResult.getResponse().getContentAsString();
        SimulationResponse response = objectMapper.readValue(responseJson, SimulationResponse.class);
        String simulationId = response.getSimulationId();

        // Brief wait to let simulation start
        TimeUnit.MILLISECONDS.sleep(100);

        // Cancel the simulation - may return 200 (cancelled) or 409 (already completed/failed)
        MvcResult cancelResult = mockMvc.perform(delete("/api/v1/simulations/{id}", simulationId))
                .andReturn();
        int cancelStatus = cancelResult.getResponse().getStatus();
        assertTrue(cancelStatus == 200 || cancelStatus == 409,
                "Expected 200 or 409 but got " + cancelStatus);
    }

    @Test
    void listSimulations_withStatusFilter() throws Exception {
        // Submit multiple simulations
        SimulationRequest request = new SimulationRequest("test.ipes", 0.001, 1e-6);

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // List with status filter
        mockMvc.perform(get("/api/v1/simulations")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulations").exists());
    }

    @Test
    void apiDocumentation_isAccessible() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.swagger-ui").exists())
                .andExpect(jsonPath("$.api-docs").exists());
    }

    @Test
    void apiInfo_returnsMetadata() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("GeckoCIRCUITS REST API"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.status").value("running"));
    }

    /**
     * P0 acceptance test: submit a simulation by circuitId of a previously
     * uploaded circuit, with explicit time overrides.
     */
    @Test
    void simulationByCircuitId_completesWithResults() throws Exception {
        String circuitId = uploadTestCircuit();

        SimulationRequest request = new SimulationRequest(null, 1e-4, 1e-6);
        request.setCircuitId(circuitId);

        String simulationId = submitAndAwaitCompletion(request, 15);

        mockMvc.perform(get("/api/v1/simulations/{id}/results", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time").exists());
    }

    /**
     * Submit by circuitId without time fields: defaults come from the circuit file.
     */
    @Test
    void simulationByCircuitId_defaultsTimesFromCircuit() throws Exception {
        String circuitId = uploadTestCircuit();

        SimulationRequest request = new SimulationRequest();
        request.setCircuitId(circuitId);

        submitAndAwaitCompletion(request, 30);
    }

    @Test
    void simulationByUnknownCircuitId_failsWithClearError() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setCircuitId("no-such-circuit");

        MvcResult submitResult = mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String simulationId = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), SimulationResponse.class).getSimulationId();

        SimulationResponse.SimulationStatus status = awaitTerminalStatus(simulationId, 10);
        assertEquals(SimulationResponse.SimulationStatus.FAILED, status);
        MvcResult statusResult = mockMvc.perform(get("/api/v1/simulations/{id}", simulationId)).andReturn();
        SimulationResponse response = objectMapper.readValue(
                statusResult.getResponse().getContentAsString(), SimulationResponse.class);
        assertTrue(response.getErrorMessage().contains("no-such-circuit"));
    }

    @Test
    void simulationRequest_withoutAnyCircuitReference_rejected() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setSimulationTime(0.01);
        request.setTimeStep(1e-6);

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulationRequest_filePathWithoutTimes_rejected() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setCircuitFile("test.ipes");

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private String uploadTestCircuit() throws Exception {
        byte[] ipesBytes = java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("src/test/resources/test-circuit.ipes"));
        String base64 = java.util.Base64.getEncoder().encodeToString(ipesBytes);

        MvcResult result = mockMvc.perform(post("/api/v1/circuits/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new gecko.rest.model.circuit.CircuitLoadRequest(base64, "test-circuit.ipes"))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(),
                gecko.rest.model.circuit.CircuitLoadResponse.class).circuitId();
    }

    private String submitAndAwaitCompletion(SimulationRequest request, int timeoutSeconds) throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String simulationId = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), SimulationResponse.class).getSimulationId();
        SimulationResponse.SimulationStatus status = awaitTerminalStatus(simulationId, timeoutSeconds);
        assertEquals(SimulationResponse.SimulationStatus.COMPLETED, status);
        return simulationId;
    }

    private SimulationResponse.SimulationStatus awaitTerminalStatus(String simulationId, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/v1/simulations/{id}", simulationId)).andReturn();
            SimulationResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), SimulationResponse.class);
            if (response.getStatus() == SimulationResponse.SimulationStatus.COMPLETED
                    || response.getStatus() == SimulationResponse.SimulationStatus.FAILED) {
                return response.getStatus();
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        throw new AssertionError("Simulation did not finish within " + timeoutSeconds + "s");
    }
}
