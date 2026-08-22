package gecko.rest.integration;

import gecko.rest.model.SimulationRequest;
import gecko.rest.model.circuit.CircuitLoadRequest;
import gecko.rest.model.circuit.CircuitLoadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for P4 pause/resume: submits a deliberately long-running
 * simulation by circuitId, pauses it mid-run, verifies that the simulation
 * time freezes, resumes it, and finally cancels it from the paused state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimulationPauseResumeE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String uploadFixture() throws Exception {
        byte[] ipes = Files.readAllBytes(Paths.get("src/test/resources/test-circuit.ipes"));
        ResponseEntity<CircuitLoadResponse> response = restTemplate.postForEntity(
                "/api/v1/circuits/parse",
                new CircuitLoadRequest(Base64.getEncoder().encodeToString(ipes), "test-circuit.ipes"),
                CircuitLoadResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().circuitId();
    }

    @Test
    void pauseFreezesRun_resumeContinues_cancelFromPausedStops() throws Exception {
        String circuitId = uploadFixture();
        SimulationRequest request = new SimulationRequest(null, 0.02, 1e-9);
        request.setCircuitId(circuitId);

        ResponseEntity<String> submitted = restTemplate.postForEntity(
                "/api/v1/simulations", request, String.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String simulationId = objectMapper.readTree(submitted.getBody())
                .get("simulationId").asText();

        assertThat(waitForRunning(simulationId)).isTrue();

        ResponseEntity<String> paused = pauseWithRetry(simulationId);
        assertThat(paused.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(paused.getBody()).get("status").asText())
                .isEqualTo("PAUSED");

        double timeWhenPaused = currentTime(simulationId);
        Thread.sleep(200);
        assertThat(currentTime(simulationId)).isEqualTo(timeWhenPaused);

        ResponseEntity<String> resumed = restTemplate.postForEntity(
                "/api/v1/simulations/{id}/resume", null, String.class, simulationId);
        assertThat(resumed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(resumed.getBody()).get("status").asText())
                .isEqualTo("RUNNING");

        // Re-pause and cancel from the paused state to bound the test runtime
        assertThat(pauseWithRetry(simulationId).getStatusCode()).isEqualTo(HttpStatus.OK);
        restTemplate.delete("/api/v1/simulations/" + simulationId);
        assertThat(awaitStatus(simulationId, "FAILED")).isTrue();

        restTemplate.delete("/api/v1/circuits/" + circuitId);
    }

    @Test
    void pause_unknownSimulation_returns404() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/simulations/no-such-id/pause", null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> resumeResponse = restTemplate.postForEntity(
                "/api/v1/simulations/no-such-id/resume", null, String.class);
        assertThat(resumeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void pause_completedSimulation_returns409() throws Exception {
        String circuitId = uploadFixture();
        SimulationRequest request = new SimulationRequest(null, 1e-4, 1e-6);
        request.setCircuitId(circuitId);

        ResponseEntity<String> submitted = restTemplate.postForEntity(
                "/api/v1/simulations", request, String.class);
        String simulationId = objectMapper.readTree(submitted.getBody())
                .get("simulationId").asText();

        assertThat(awaitStatus(simulationId, "COMPLETED")).isTrue();

        ResponseEntity<String> paused = restTemplate.postForEntity(
                "/api/v1/simulations/{id}/pause", null, String.class, simulationId);
        assertThat(paused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        restTemplate.delete("/api/v1/simulations/" + simulationId);
        restTemplate.delete("/api/v1/circuits/" + circuitId);
    }

    /**
     * Small retry loop: the REST status flips to RUNNING slightly before the
     * engine registers in the service's running-engine map.
     */
    private ResponseEntity<String> pauseWithRetry(String simulationId) throws Exception {
        ResponseEntity<String> response = null;
        for (int i = 0; i < 20; i++) {
            response = restTemplate.postForEntity(
                    "/api/v1/simulations/{id}/pause", null, String.class, simulationId);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response;
            }
            Thread.sleep(25);
        }
        return response;
    }

    private boolean waitForRunning(String simulationId) throws Exception {
        return awaitStatus(simulationId, "RUNNING");
    }

    private boolean awaitStatus(String simulationId, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            String body = restTemplate.getForEntity(
                    "/api/v1/simulations/{id}", String.class, simulationId).getBody();
            String status = objectMapper.readTree(body).get("status").asText();
            if (expected.equals(status)) {
                return true;
            }
            if ("FAILED".equals(status) && !"FAILED".equals(expected)) {
                throw new AssertionError("simulation failed unexpectedly: " + body);
            }
            Thread.sleep(50);
        }
        return false;
    }

    private double currentTime(String simulationId) throws Exception {
        String body = restTemplate.getForEntity(
                "/api/v1/simulations/{id}", String.class, simulationId).getBody();
        com.fasterxml.jackson.databind.JsonNode details = objectMapper.readTree(body)
                .get("progressDetails");
        assertThat(details).isNotNull();
        return details.get("currentTime").asDouble();
    }
}
