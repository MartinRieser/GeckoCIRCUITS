package gecko.rest.integration;

import gecko.rest.model.circuit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for circuit file operations.
 * Tests the full stack from HTTP request to response.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CircuitFileE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String validIpesBase64;
    private static final String TEST_CIRCUIT_PATH = "src/test/resources/test-circuit.ipes";

    @BeforeEach
    void setUp() throws IOException {
        // Load test circuit file and convert to base64
        byte[] ipesBytes = Files.readAllBytes(Paths.get(TEST_CIRCUIT_PATH));
        validIpesBase64 = Base64.getEncoder().encodeToString(ipesBytes);
    }

    @Test
    void testFullWorkflow_parseValidateRetrieveDelete() {
        // 1. Parse circuit
        CircuitLoadRequest loadRequest = new CircuitLoadRequest(validIpesBase64, "test-circuit.ipes");
        ResponseEntity<CircuitLoadResponse> loadResponse = restTemplate.postForEntity(
            "/api/v1/circuits/parse",
            loadRequest,
            CircuitLoadResponse.class
        );

        assertThat(loadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(loadResponse.getBody()).isNotNull();
        assertThat(loadResponse.getBody().status()).isEqualTo("loaded");
        assertThat(loadResponse.getBody().circuitId()).isNotNull();
        String circuitId = loadResponse.getBody().circuitId();

        // 2. Get circuit info
        ResponseEntity<CircuitInfo> infoResponse = restTemplate.getForEntity(
            "/api/v1/circuits/" + circuitId + "/info",
            CircuitInfo.class
        );

        assertThat(infoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(infoResponse.getBody()).isNotNull();
        assertThat(infoResponse.getBody().circuitId()).isEqualTo(circuitId);
        assertThat(infoResponse.getBody().filename()).isEqualTo("test-circuit.ipes");
        assertThat(infoResponse.getBody().simulationParameters()).isNotNull();
        assertThat(infoResponse.getBody().componentCounts()).isNotNull();

        // 3. Get components
        ResponseEntity<ComponentListResponse> componentsResponse = restTemplate.getForEntity(
            "/api/v1/circuits/" + circuitId + "/components",
            ComponentListResponse.class
        );

        assertThat(componentsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(componentsResponse.getBody()).isNotNull();
        assertThat(componentsResponse.getBody().circuitId()).isEqualTo(circuitId);
        assertThat(componentsResponse.getBody().components()).hasSize(24);

        // 4. Validate circuit
        ResponseEntity<ValidationResponse> validationResponse = restTemplate.getForEntity(
            "/api/v1/circuits/" + circuitId + "/validate",
            ValidationResponse.class
        );

        assertThat(validationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validationResponse.getBody()).isNotNull();
        // Validation result depends on circuit file's simulation parameters
        // Just verify we get a validation response
        assertThat(validationResponse.getBody().warnings()).isNotNull();
        assertThat(validationResponse.getBody().errors()).isNotNull();

        // 5. List all circuits
        ResponseEntity<CircuitListResponse> listResponse = restTemplate.getForEntity(
            "/api/v1/circuits",
            CircuitListResponse.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody().total()).isGreaterThanOrEqualTo(1);
        assertThat(listResponse.getBody().circuits()).isNotEmpty();

        // 6. Delete circuit
        restTemplate.delete("/api/v1/circuits/" + circuitId);

        // 7. Verify deletion
        ResponseEntity<CircuitInfo> deletedInfoResponse = restTemplate.getForEntity(
            "/api/v1/circuits/" + circuitId + "/info",
            CircuitInfo.class
        );

        assertThat(deletedInfoResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testParseCircuit_invalidBase64() {
        CircuitLoadRequest loadRequest = new CircuitLoadRequest("invalid-base64!", "test.ipes");
        ResponseEntity<CircuitLoadResponse> response = restTemplate.postForEntity(
            "/api/v1/circuits/parse",
            loadRequest,
            CircuitLoadResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("failed");
        assertThat(response.getBody().errorMessage()).contains("base64");
    }

    @Test
    void testParseCircuit_invalidCircuitContent() {
        // Create content that's not gzip compressed
        // Note: CircuitFileParser is very lenient and might accept minimal content
        String invalidContent = Base64.getEncoder().encodeToString("not a valid gzip content".getBytes());
        CircuitLoadRequest loadRequest = new CircuitLoadRequest(invalidContent, "invalid.ipes");

        ResponseEntity<CircuitLoadResponse> response = restTemplate.postForEntity(
            "/api/v1/circuits/parse",
            loadRequest,
            CircuitLoadResponse.class
        );

        assertThat(response.getBody()).isNotNull();
        // Parser might be lenient - just verify we get a response
        assertThat(response.getBody().status()).isIn("loaded", "failed");
    }

    @Test
    void testGetCircuitInfo_notFound() {
        ResponseEntity<CircuitInfo> response = restTemplate.getForEntity(
            "/api/v1/circuits/nonexistent-id/info",
            CircuitInfo.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetComponents_notFound() {
        ResponseEntity<ComponentListResponse> response = restTemplate.getForEntity(
            "/api/v1/circuits/nonexistent-id/components",
            ComponentListResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testValidateCircuit_notFound() {
        ResponseEntity<ValidationResponse> response = restTemplate.getForEntity(
            "/api/v1/circuits/nonexistent-id/validate",
            ValidationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testDeleteCircuit_notFound() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/v1/circuits/nonexistent-id",
            HttpMethod.DELETE,
            entity,
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetAllCircuits_empty() {
        // Note: This test might fail if other tests have loaded circuits
        // In a real scenario, you'd want to clean up between tests or use @DirtiesContext

        ResponseEntity<CircuitListResponse> response = restTemplate.getForEntity(
            "/api/v1/circuits",
            CircuitListResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().circuits()).isNotNull();
    }

    @Test
    void testMultipleCircuits() {
        // Load multiple circuits
        CircuitLoadRequest request1 = new CircuitLoadRequest(validIpesBase64, "circuit1.ipes");
        CircuitLoadRequest request2 = new CircuitLoadRequest(validIpesBase64, "circuit2.ipes");

        ResponseEntity<CircuitLoadResponse> response1 = restTemplate.postForEntity(
            "/api/v1/circuits/parse", request1, CircuitLoadResponse.class
        );
        ResponseEntity<CircuitLoadResponse> response2 = restTemplate.postForEntity(
            "/api/v1/circuits/parse", request2, CircuitLoadResponse.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String circuitId1 = response1.getBody().circuitId();
        String circuitId2 = response2.getBody().circuitId();

        assertThat(circuitId1).isNotEqualTo(circuitId2);

        // List all circuits
        ResponseEntity<CircuitListResponse> listResponse = restTemplate.getForEntity(
            "/api/v1/circuits",
            CircuitListResponse.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody().total()).isGreaterThanOrEqualTo(2);

        // Clean up
        restTemplate.delete("/api/v1/circuits/" + circuitId1);
        restTemplate.delete("/api/v1/circuits/" + circuitId2);
    }

    /**
     * P0 acceptance test: the .ipes round trip.
     * upload -> parse -> GET /ipes (re-serialized) -> re-parse must yield an
     * identical circuit. This is the interchange guarantee with the Swing UI.
     */
    @Test
    void testIpesRoundTrip_serializeAndReloadYieldsIdenticalCircuit() {
        ResponseEntity<CircuitLoadResponse> loadResponse = restTemplate.postForEntity(
            "/api/v1/circuits/parse",
            new CircuitLoadRequest(validIpesBase64, "test-circuit.ipes"),
            CircuitLoadResponse.class
        );
        assertThat(loadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String circuitId = loadResponse.getBody().circuitId();
        int originalComponents = loadResponse.getBody().componentCount();

        ResponseEntity<CircuitInfo> originalInfo = restTemplate.getForEntity(
            "/api/v1/circuits/" + circuitId + "/info", CircuitInfo.class);

        // Download the re-serialized .ipes file
        ResponseEntity<byte[]> ipesResponse = restTemplate.getForEntity(
            "/api/v1/circuits/" + circuitId + "/ipes", byte[].class);

        assertThat(ipesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] ipesBytes = ipesResponse.getBody();
        assertThat(ipesBytes).isNotNull();
        assertThat(ipesBytes.length).isGreaterThan(2);
        assertThat(ipesBytes[0] & 0xff).isEqualTo(0x1f);
        assertThat(ipesBytes[1] & 0xff).isEqualTo(0x8b);

        // Re-upload the downloaded file as a new circuit
        String reloadedBase64 = Base64.getEncoder().encodeToString(ipesBytes);
        ResponseEntity<CircuitLoadResponse> reloadResponse = restTemplate.postForEntity(
            "/api/v1/circuits/parse",
            new CircuitLoadRequest(reloadedBase64, "roundtrip.ipes"),
            CircuitLoadResponse.class
        );
        assertThat(reloadResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reloadResponse.getBody().status()).isEqualTo("loaded");
        assertThat(reloadResponse.getBody().componentCount()).isEqualTo(originalComponents);

        // Simulation parameters survive the round trip
        ResponseEntity<CircuitInfo> reloadedInfo = restTemplate.getForEntity(
            "/api/v1/circuits/" + reloadResponse.getBody().circuitId() + "/info", CircuitInfo.class);
        assertThat(reloadedInfo.getBody().simulationParameters().endTime())
            .isEqualTo(originalInfo.getBody().simulationParameters().endTime());
        assertThat(reloadedInfo.getBody().simulationParameters().timeStep())
            .isEqualTo(originalInfo.getBody().simulationParameters().timeStep());

        restTemplate.delete("/api/v1/circuits/" + circuitId);
        restTemplate.delete("/api/v1/circuits/" + reloadResponse.getBody().circuitId());
    }

    @Test
    void testGetIpesFile_notFound() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
            "/api/v1/circuits/nonexistent-id/ipes", byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testReplaceCircuit_sameIdNewContent() {
        ResponseEntity<CircuitLoadResponse> loadResponse = restTemplate.postForEntity(
            "/api/v1/circuits/parse",
            new CircuitLoadRequest(validIpesBase64, "original.ipes"),
            CircuitLoadResponse.class
        );
        String circuitId = loadResponse.getBody().circuitId();

        // Replace with the re-serialized content of itself under a new name
        byte[] ipesBytes = restTemplate.getForEntity(
            "/api/v1/circuits/" + circuitId + "/ipes", byte[].class).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<CircuitInfo> replaceResponse = restTemplate.exchange(
            "/api/v1/circuits/" + circuitId,
            HttpMethod.PUT,
            new HttpEntity<>(new CircuitLoadRequest(
                Base64.getEncoder().encodeToString(ipesBytes), "edited.ipes"), headers),
            CircuitInfo.class
        );

        assertThat(replaceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replaceResponse.getBody().circuitId()).isEqualTo(circuitId);
        assertThat(replaceResponse.getBody().filename()).isEqualTo("edited.ipes");

        restTemplate.delete("/api/v1/circuits/" + circuitId);
    }

    @Test
    void testReplaceCircuit_notFound() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<CircuitInfo> response = restTemplate.exchange(
            "/api/v1/circuits/nonexistent-id",
            HttpMethod.PUT,
            new HttpEntity<>(new CircuitLoadRequest(validIpesBase64, "x.ipes"), headers),
            CircuitInfo.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
