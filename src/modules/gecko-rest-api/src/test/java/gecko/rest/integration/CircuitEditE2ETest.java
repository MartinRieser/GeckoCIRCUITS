package gecko.rest.integration;

import gecko.rest.model.SimulationRequest;
import gecko.rest.model.circuit.CircuitLoadRequest;
import gecko.rest.model.circuit.CircuitLoadResponse;
import gecko.rest.model.circuit.ComponentListResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for the P1 editing workflow: upload, edit via REST,
 * verify persistence via the P0 round-trip endpoint, and simulate the
 * edited circuit by circuitId.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CircuitEditE2ETest {

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
    void editUndoAndRoundTripPersisted() throws Exception {
        String circuitId = uploadFixture();
        int before = componentCount(circuitId);

        // Create
        ResponseEntity<String> created = restTemplate.postForEntity(
                "/api/v1/circuits/{id}/components",
                editRequest(), String.class, circuitId);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long versionAfterCreate = objectMapper.readTree(created.getBody()).get("modelVersion").asLong();
        assertThat(versionAfterCreate).isEqualTo(1);

        String createdName = objectMapper.readTree(created.getBody())
                .get("payload").get("name").asText();
        assertThat(componentCount(circuitId)).isEqualTo(before + 1);

        // Undo via REST
        ResponseEntity<String> undone = restTemplate.postForEntity(
                "/api/v1/circuits/{id}/undo", null, String.class, circuitId);
        assertThat(undone.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(componentCount(circuitId)).isEqualTo(before);

        // Redo via REST
        restTemplate.postForEntity("/api/v1/circuits/{id}/redo", null, String.class, circuitId);
        assertThat(componentCount(circuitId)).isEqualTo(before + 1);

        // Set node labels on the new component so it is electrically connected
        HttpEntity<String> labelEntity = jsonEntity(
                "{\"terminalIndex\": 0, \"side\": \"x\", \"label\": \"in\"}");
        restTemplate.exchange("/api/v1/circuits/{id}/nodes/{name}", HttpMethod.PUT,
                labelEntity, String.class, circuitId, createdName);
        restTemplate.exchange("/api/v1/circuits/{id}/nodes/{name}", HttpMethod.PUT,
                jsonEntity("{\"terminalIndex\": 0, \"side\": \"y\", \"label\": \"gnd\"}"),
                String.class, circuitId, createdName);

        // P0 round-trip must carry the edit
        byte[] ipes = restTemplate.getForEntity(
                "/api/v1/circuits/{id}/ipes", byte[].class, circuitId).getBody();
        String reloadedId = restTemplate.postForEntity("/api/v1/circuits/parse",
                new CircuitLoadRequest(Base64.getEncoder().encodeToString(ipes), "rt.ipes"),
                CircuitLoadResponse.class).getBody().circuitId();

        ResponseEntity<ComponentListResponse> reloadedComponents = restTemplate.getForEntity(
                "/api/v1/circuits/{id}/components", ComponentListResponse.class, reloadedId);
        assertThat(reloadedComponents.getBody().components()).hasSize(before + 1);
        assertThat(reloadedComponents.getBody().components().stream()
                .anyMatch(c -> createdName.equals(c.name()))).isTrue();

        restTemplate.delete("/api/v1/circuits/" + circuitId);
        restTemplate.delete("/api/v1/circuits/" + reloadedId);
    }

    @Test
    void simulateEditedCircuitByCircuitId() throws Exception {
        String circuitId = uploadFixture();
        restTemplate.postForEntity("/api/v1/circuits/{id}/components",
                editRequest(), String.class, circuitId);

        SimulationRequest request = new SimulationRequest(null, 1e-4, 1e-6);
        request.setCircuitId(circuitId);

        ResponseEntity<String> submitted = restTemplate.postForEntity(
                "/api/v1/simulations", jsonEntity(objectMapper.writeValueAsString(request)),
                String.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String simulationId = objectMapper.readTree(submitted.getBody())
                .get("simulationId").asText();

        awaitCompletion(simulationId);
        ResponseEntity<String> results = restTemplate.getForEntity(
                "/api/v1/simulations/{id}/results", String.class, simulationId);
        assertThat(results.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(results.getBody()).has("time")).isTrue();

        restTemplate.delete("/api/v1/circuits/" + circuitId);
    }

    @Test
    void unknownCircuit_404() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/circuits/no-such/components", editRequest(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpEntity<String> editRequest() {
        return jsonEntity("{\"family\": \"LK\", \"type\": 1, \"name\": \"R_e2e\", \"x\": 96, \"y\": 96, "
                + "\"orientation\": 0, \"parameters\": {\"param0\": 100.0}}");
    }

    private int componentCount(String circuitId) {
        return restTemplate.getForEntity("/api/v1/circuits/{id}/components",
                ComponentListResponse.class, circuitId).getBody().components().size();
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private void awaitCompletion(String simulationId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                String body = restTemplate.getForEntity(
                        "/api/v1/simulations/{id}", String.class, simulationId).getBody();
                String status = objectMapper.readTree(body).get("status").asText();
                if ("COMPLETED".equals(status)) {
                    return;
                }
                if ("FAILED".equals(status)) {
                    throw new AssertionError("simulation failed: " + body);
                }
            } catch (AssertionError e) {
                throw e;
            } catch (Exception ignored) {
                // retry until deadline
            }
            Thread.sleep(100);
        }
        throw new AssertionError("simulation did not complete in time");
    }
}
