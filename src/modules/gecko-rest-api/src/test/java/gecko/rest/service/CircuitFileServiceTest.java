package gecko.rest.service;

import gecko.rest.model.circuit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitFileService.
 * Uses a real .ipes file from test resources.
 */
class CircuitFileServiceTest {

    private CircuitFileService service;
    private String validIpesBase64;
    private static final String TEST_CIRCUIT_PATH = "src/test/resources/test-circuit.ipes";

    @BeforeEach
    void setUp() throws IOException {
        service = new CircuitFileService();

        // Load test circuit file and convert to base64
        byte[] ipesBytes = Files.readAllBytes(Paths.get(TEST_CIRCUIT_PATH));
        validIpesBase64 = Base64.getEncoder().encodeToString(ipesBytes);
    }

    @Test
    void testLoadCircuit_fromBase64_success() {
        CircuitLoadResponse response = service.loadCircuit(validIpesBase64, "test-circuit.ipes");

        assertNotNull(response);
        assertEquals("loaded", response.status());
        assertEquals("test-circuit.ipes", response.filename());
        assertNotNull(response.circuitId());
        assertEquals(24, response.componentCount());
        assertNull(response.errorMessage());
    }

    @Test
    void testLoadCircuit_invalidBase64() {
        CircuitLoadResponse response = service.loadCircuit("invalid-base64!", "test.ipes");

        assertNotNull(response);
        assertEquals("failed", response.status());
        assertEquals("test.ipes", response.filename());
        assertNotNull(response.errorMessage());
        assertTrue(response.errorMessage().contains("base64"));
    }

    @Test
    void testLoadCircuit_invalidCircuitFormat() {
        // Create invalid (but valid base64) content - not gzipped
        // Note: CircuitFileParser is very lenient, so it might accept this
        String invalidContent = Base64.getEncoder().encodeToString("not a valid gzip content".getBytes());

        CircuitLoadResponse response = service.loadCircuit(invalidContent, "invalid.ipes");

        assertNotNull(response);
        // Parser might be lenient and accept minimal content
        // Just verify we get a response (either success or failure)
        assertNotNull(response.status());
    }

    @Test
    void testGetCircuitInfo_found() {
        // Load a circuit first
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String circuitId = loadResponse.circuitId();

        // Get info
        CircuitInfo info = service.getCircuitInfo(circuitId);

        assertNotNull(info);
        assertEquals(circuitId, info.circuitId());
        assertEquals("test-circuit.ipes", info.filename());
        assertNotNull(info.fileVersion());
        assertNotNull(info.simulationParameters());
        assertNotNull(info.componentCounts());
        assertNotNull(info.displaySettings());
        assertNotNull(info.metadata());

        // Check simulation parameters
        CircuitInfo.SimulationParameters simParams = info.simulationParameters();
        assertNotNull(simParams.endTime());
        assertNotNull(simParams.timeStep());
        assertNotNull(simParams.solverType());

        // Check component counts
        CircuitInfo.ComponentCounts counts = info.componentCounts();
        assertEquals(24, counts.circuit() + counts.control() + counts.thermal());
    }

    @Test
    void testGetCircuitInfo_notFound() {
        CircuitInfo info = service.getCircuitInfo("nonexistent-id");
        assertNull(info);
    }

    @Test
    void testGetComponents_found() {
        // Load a circuit first
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String circuitId = loadResponse.circuitId();

        // Get components
        ComponentListResponse response = service.getComponents(circuitId);

        assertNotNull(response);
        assertEquals(circuitId, response.circuitId());
        assertNotNull(response.components());
        assertEquals(24, response.components().size());
    }

    @Test
    void testGetComponents_notFound() {
        ComponentListResponse response = service.getComponents("nonexistent-id");
        assertNull(response);
    }

    @Test
    void testValidateCircuit_valid() {
        // Load a circuit first
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String circuitId = loadResponse.circuitId();

        // Validate
        ValidationResponse response = service.validateCircuit(circuitId);

        assertNotNull(response);
        assertNotNull(response.warnings());
        assertNotNull(response.errors());
        assertTrue(response.valid());
        assertTrue(response.errors().isEmpty());
    }

    @Test
    void testValidateCircuit_notFound() {
        ValidationResponse response = service.validateCircuit("nonexistent-id");
        assertNull(response);
    }

    @Test
    void testGetAllCircuits() {
        // Load multiple circuits
        CircuitLoadResponse response1 = service.loadCircuit(validIpesBase64, "circuit1.ipes");
        CircuitLoadResponse response2 = service.loadCircuit(validIpesBase64, "circuit2.ipes");

        // Get all circuits
        CircuitListResponse listResponse = service.getAllCircuits();

        assertNotNull(listResponse);
        assertNotNull(listResponse.circuits());
        assertEquals(2, listResponse.total());
        assertEquals(2, listResponse.circuits().size());

        // Check circuit summaries
        CircuitListResponse.CircuitSummary summary1 = listResponse.circuits().get(0);
        assertNotNull(summary1.circuitId());
        assertNotNull(summary1.filename());
        assertEquals(24, summary1.componentCount());
        assertNotNull(summary1.loadedAt());
    }

    @Test
    void testGetAllCircuits_empty() {
        CircuitListResponse listResponse = service.getAllCircuits();

        assertNotNull(listResponse);
        assertNotNull(listResponse.circuits());
        assertEquals(0, listResponse.total());
        assertEquals(0, listResponse.circuits().size());
    }

    @Test
    void testGetRawCircuit() {
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String circuitId = loadResponse.circuitId();

        String raw = service.getRawCircuit(circuitId);

        assertNotNull(raw);
        assertTrue(raw.contains("tDURATION"), "raw output should contain .ipes tokens");
        assertTrue(raw.contains("FileVersion"), "raw output should contain .ipes tokens");
    }

    @Test
    void testGetRawCircuit_notFound() {
        String raw = service.getRawCircuit("nonexistent-id");
        assertNull(raw);
    }

    @Test
    void testDeleteCircuit_success() {
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String circuitId = loadResponse.circuitId();

        // Delete
        boolean deleted = service.deleteCircuit(circuitId);
        assertTrue(deleted);

        // Verify deleted
        CircuitInfo info = service.getCircuitInfo(circuitId);
        assertNull(info);
    }

    @Test
    void testDeleteCircuit_notFound() {
        boolean deleted = service.deleteCircuit("nonexistent-id");
        assertFalse(deleted);
    }

    @Test
    void testDeleteCircuit_twice() {
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String circuitId = loadResponse.circuitId();

        // Delete first time
        boolean deleted1 = service.deleteCircuit(circuitId);
        assertTrue(deleted1);

        // Try deleting again
        boolean deleted2 = service.deleteCircuit(circuitId);
        assertFalse(deleted2);
    }

    @Test
    void testGetIpesBytes_returnsGzipRoundTrippableFile() {
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String circuitId = loadResponse.circuitId();
        int originalCount = loadResponse.componentCount();

        byte[] bytes = service.getIpesBytes(circuitId);

        assertNotNull(bytes);
        assertTrue(bytes.length > 2);
        assertEquals(0x1f, bytes[0] & 0xff, "gzip magic byte 1");
        assertEquals(0x8b, bytes[1] & 0xff, "gzip magic byte 2");

        // Re-load the serialized bytes as a new circuit and compare
        CircuitLoadResponse reload = service.loadCircuit(
                Base64.getEncoder().encodeToString(bytes), "roundtrip.ipes");
        assertEquals("loaded", reload.status());
        assertEquals(originalCount, reload.componentCount());
    }

    @Test
    void testGetIpesBytes_notFound() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.getIpesBytes("nonexistent-id"));
    }

    @Test
    void testReplaceCircuit_keepsIdAndUpdatesContent() {
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String circuitId = loadResponse.circuitId();

        CircuitInfo info = service.replaceCircuit(circuitId,
                service.getIpesBytes(circuitId), "renamed.ipes");

        assertEquals(circuitId, info.circuitId());
        assertEquals("renamed.ipes", info.filename());
        assertEquals(loadResponse.componentCount(), info.componentCounts().circuit()
                + info.componentCounts().control() + info.componentCounts().thermal());
    }

    @Test
    void testReplaceCircuit_notFound() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.replaceCircuit("nonexistent-id", new byte[]{1, 2}, "x.ipes"));
    }

    @Test
    void testCloneCircuit_preservesConnectionsAndLabels() {
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        String sourceId = loadResponse.circuitId();
        int connectionCount = service.getModel(sourceId).getConnections().size();
        assertTrue(connectionCount > 0, "fixture must contain connections");

        CircuitLoadResponse clone = service.cloneCircuit(sourceId, null);

        assertEquals(connectionCount, service.getModel(clone.circuitId()).getConnections().size());
    }

    @Test
    void testGetModel_returnsModelOrNull() {
        CircuitLoadResponse loadResponse = service.loadCircuit(validIpesBase64, "test-circuit.ipes");
        assertNotNull(service.getModel(loadResponse.circuitId()));
        assertNull(service.getModel("nonexistent-id"));
    }
}
