package gecko.rest.service;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.rest.model.circuit.CatalogResponse;
import gecko.rest.model.circuit.CircuitChangeMessage;
import gecko.rest.model.circuit.ComponentCreateRequest;
import gecko.rest.model.circuit.ComponentInfo;
import gecko.rest.model.circuit.ComponentPatchRequest;
import gecko.rest.model.circuit.ConnectionCreateRequest;
import gecko.rest.model.circuit.ConnectionPatchRequest;
import gecko.rest.model.circuit.NodeLabelRequest;
import gecko.rest.model.circuit.WireInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitEditService, using the real CircuitFileService
 * with the .ipes test fixture.
 */
class CircuitEditServiceTest {

    private CircuitFileService circuitFileService;
    private CircuitEditService service;
    private String circuitId;

    @BeforeEach
    void setUp() throws IOException {
        circuitFileService = new CircuitFileService();
        service = new CircuitEditService(circuitFileService);

        byte[] ipes = Files.readAllBytes(Paths.get("src/test/resources/test-circuit.ipes"));
        circuitId = circuitFileService
                .loadCircuit(java.util.Base64.getEncoder().encodeToString(ipes), "test-circuit.ipes")
                .circuitId();
    }

    private CircuitModel model() {
        return circuitFileService.getModel(circuitId);
    }

    private static ComponentInfo payloadComponent(CircuitChangeMessage message) {
        return (ComponentInfo) message.payload();
    }

    private static WireInfo payloadWire(CircuitChangeMessage message) {
        return (WireInfo) message.payload();
    }

    // ========== Create ==========

    @Test
    void createComponent_autoNameAndSnap() {
        CircuitChangeMessage result = service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, null, 100, 50, null, null));

        assertEquals("createComponent", result.operation());
        assertEquals(1, result.modelVersion());
        ComponentInfo info = payloadComponent(result);
        assertEquals(1, info.type());
        assertEquals("LK", info.domain());
        assertNotNull(info.name());
        // dpix raster 16: 100 -> 96, 50 -> 48
        assertArrayEquals(new int[]{96, 48}, info.position());
        assertEquals(0, info.orientation());

        assertTrue(model().getCircuitComponents().stream()
                .anyMatch(c -> c.getName().equals(info.name())));
    }

    @Test
    void createComponent_customNameAndParameters() {
        CircuitChangeMessage result = service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "R_test", 32, 32, 2, Map.of("param0", 47.5)));

        ComponentInfo info = payloadComponent(result);
        assertEquals("R_test", info.name());
        assertEquals(2, info.orientation());

        CircuitModel.ComponentData created = findByName("R_test");
        assertArrayEquals(new double[]{47.5}, created.getRawParameters(), 1e-12);
        assertEquals(47.5, created.getParameters().get("param0"));
    }

    @Test
    void createComponent_duplicateCustomName_conflict() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "dup", 16, 16, null, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createComponent(circuitId,
                        new ComponentCreateRequest("LK", 1, "dup", 16, 16, null, null)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void createComponent_invalidInputs() {
        assertThrows(ResponseStatusException.class, () -> service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 999, null, 16, 16, null, null)), "unknown type");
        assertThrows(ResponseStatusException.class, () -> service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 46, null, 16, 16, null, null)), "thermal type in LK family");
        assertThrows(ResponseStatusException.class, () -> service.createComponent(circuitId,
                new ComponentCreateRequest("CONTROL", 101, null, 16, 16, null, null)), "CONTROL not supported");
        assertThrows(ResponseStatusException.class, () -> service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "R9", 16, 16, 7, null)), "orientation out of range");
        assertThrows(ResponseStatusException.class, () -> service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "R9", 16, 16, null, Map.of("resistance", 10.0))),
                "non-index parameter key");
    }

    @Test
    void createComponent_thermalFamily() {
        CircuitChangeMessage result = service.createComponent(circuitId,
                new ComponentCreateRequest("THERM", 46, "RTH1", 16, 16, null, null));
        assertEquals("THERM", payloadComponent(result).domain());
        assertTrue(model().getThermalComponents().stream()
                .anyMatch(c -> c.getName().equals("RTH1")));
    }

    @Test
    void createComponent_unknownCircuit_404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createComponent("no-such", new ComponentCreateRequest("LK", 1, null, 16, 16, null, null)));
        assertEquals(404, ex.getStatusCode().value());
    }

    // ========== Patch ==========

    @Test
    void patchComponent_moveRotateRenameAndParams() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "Rp", 32, 32, 0, Map.of("param0", 10.0)));

        CircuitChangeMessage result = service.patchComponent(circuitId, "Rp",
                new ComponentPatchRequest(80, 40, 1, "Rp_new", Map.of("param0", 20.0)));

        ComponentInfo info = payloadComponent(result);
        assertEquals("Rp_new", info.name());
        assertEquals(1, info.orientation());
        assertArrayEquals(new int[]{80, 48}, info.position()); // 40 snapped to 48

        CircuitModel.ComponentData patched = findByName("Rp_new");
        assertEquals(20.0, patched.getRawParameters()[0], 1e-12);
    }

    @Test
    void patchComponent_renameConflict_409() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "A", 16, 16, null, null));
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "B", 16, 16, null, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.patchComponent(circuitId, "A", new ComponentPatchRequest(null, null, null, "B", null)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void patchComponent_unknown_404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.patchComponent(circuitId, "ghost", new ComponentPatchRequest(16, 16, null, null, null)));
        assertEquals(404, ex.getStatusCode().value());
    }

    // ========== Delete + Undo/Redo ==========

    @Test
    void deleteAndUndoRestore() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "Rdel", 16, 16, null, null));

        service.deleteComponent(circuitId, "Rdel");
        assertNull(findByNameOrNull("Rdel"));

        CircuitChangeMessage undone = service.undo(circuitId);
        assertEquals("undo", undone.operation());
        assertNotNull(findByNameOrNull("Rdel"), "undo must restore the component with all fields");
    }

    @Test
    void undoRestoresFullStateAfterPatch() {
        service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "Ru", 32, 32, 0, Map.of("param0", 11.0)));
        service.patchComponent(circuitId, "Ru", new ComponentPatchRequest(64, 64, 3, null, Map.of("param0", 99.0)));

        service.undo(circuitId);

        CircuitModel.ComponentData comp = findByName("Ru");
        assertArrayEquals(new int[]{32, 32}, comp.getPosition());
        assertEquals(0, comp.getOrientation());
        assertEquals(11.0, comp.getRawParameters()[0], 1e-12);
    }

    @Test
    void undoDeleteOfLoadedFixtureComponent_restoresEverything() {
        CircuitModel.ComponentData original = model().getCircuitComponents().get(0);
        String name = original.getName();
        int[] pos = original.getPosition().clone();
        String[] labels = original.getTerminalXLabels().clone();

        service.deleteComponent(circuitId, name);
        service.undo(circuitId);

        CircuitModel.ComponentData restored = findByName(name);
        assertArrayEquals(pos, restored.getPosition());
        assertArrayEquals(labels, restored.getTerminalXLabels());
        assertEquals(original.getUniqueObjectIdentifier(), restored.getUniqueObjectIdentifier());
    }

    @Test
    void redoReappliesEdit() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "Rrd", 16, 16, null, null));
        service.undo(circuitId);
        assertNull(findByNameOrNull("Rrd"));

        CircuitChangeMessage redone = service.redo(circuitId);
        assertEquals("redo", redone.operation());
        assertNotNull(findByNameOrNull("Rrd"));
    }

    @Test
    void newEditClearsRedoStack() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "R1x", 16, 16, null, null));
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "R2x", 16, 16, null, null));
        service.undo(circuitId); // undo R2x
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "R3x", 16, 16, null, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.redo(circuitId));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void undoWithoutHistory_409() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.undo(circuitId));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void versionIncrementsPerMutation() {
        long v0 = 0;
        CircuitChangeMessage c1 = service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "Rv", 16, 16, null, null));
        CircuitChangeMessage c2 = service.patchComponent(circuitId, "Rv", new ComponentPatchRequest(32, 32, null, null, null));
        CircuitChangeMessage c3 = service.undo(circuitId);

        assertEquals(v0 + 1, c1.modelVersion());
        assertEquals(v0 + 2, c2.modelVersion());
        assertEquals(v0 + 3, c3.modelVersion());
    }

    // ========== Connections ==========

    @Test
    void connectionCrud() {
        int before = model().getConnections().size();

        CircuitChangeMessage created = service.createConnection(circuitId,
                new ConnectionCreateRequest("LK", new int[][]{{100, 200}, {150, 200}, {150, 260}}, "w1"));
        WireInfo wire = payloadWire(created);
        assertEquals(before, wire.index());
        assertEquals("w1", wire.label());
        assertEquals(96, wire.points()[0][0]); // snapped
        assertEquals(256, wire.points()[2][1]); // 260 -> 256
        assertEquals(before + 1, model().getConnections().size());

        CircuitChangeMessage patched = service.patchConnection(circuitId, wire.index(),
                new ConnectionPatchRequest(new int[][]{{96, 200}, {160, 210}}, "w1b"));
        assertEquals("w1b", payloadWire(patched).label());
        assertArrayEquals(new int[]{160, 208}, payloadWire(patched).points()[1]);

        service.deleteConnection(circuitId, wire.index());
        assertEquals(before, model().getConnections().size());

        service.undo(circuitId);
        assertEquals(before + 1, model().getConnections().size());
        assertEquals("w1b", model().getConnections().get(wire.index()).getLabel());
    }

    @Test
    void createConnection_invalidInputs() {
        assertThrows(ResponseStatusException.class, () -> service.createConnection(circuitId,
                new ConnectionCreateRequest("LK", new int[][]{{16, 16}}, null)), "single point");
        assertThrows(ResponseStatusException.class, () -> service.createConnection(circuitId,
                new ConnectionCreateRequest("LK", new int[][]{{16, 16}, null}, null)), "null point");
        assertThrows(ResponseStatusException.class, () -> service.createConnection(circuitId,
                new ConnectionCreateRequest("RELUCTANCE", new int[][]{{16, 16}, {32, 32}}, null)), "bad type");
    }

    @Test
    void connectionIndexBounds_404() {
        int count = model().getConnections().size();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.patchConnection(circuitId, count, new ConnectionPatchRequest(null, "x")));
        assertEquals(404, ex.getStatusCode().value());
    }

    // ========== Node labels ==========

    @Test
    void setNodeLabel_growsArraysAndUndoRestores() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "Rnl", 16, 16, null, null));

        CircuitChangeMessage result = service.setNodeLabel(circuitId, "Rnl",
                new NodeLabelRequest(0, "x", "dc_plus"));
        assertEquals("setNodeLabel", result.operation());

        CircuitModel.ComponentData comp = findByName("Rnl");
        assertEquals("dc_plus", comp.getTerminalXLabels()[0]);

        service.setNodeLabel(circuitId, "Rnl", new NodeLabelRequest(0, "y", "gnd"));
        assertEquals("gnd", comp.getTerminalYLabels()[0]);

        service.undo(circuitId); // undo the y-side label
        assertEquals(0, comp.getTerminalYLabels().length, "undo restores the original empty array");
        assertEquals("dc_plus", comp.getTerminalXLabels()[0], "x side untouched by y-side undo");
    }

    @Test
    void setNodeLabel_invalidSide_400() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "Rnl2", 16, 16, null, null));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.setNodeLabel(circuitId, "Rnl2", new NodeLabelRequest(0, "z", "x")));
        assertEquals(400, ex.getStatusCode().value());
    }

    // ========== Serializability after mutations (P1 acceptance) ==========

    @Test
    void everyMutationLeavesModelSerializable() throws Exception {
        service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "Rser", 16, 16, 1, Map.of("param0", 123.0)));
        service.patchComponent(circuitId, "Rser", new ComponentPatchRequest(48, 48, null, null, null));
        service.setNodeLabel(circuitId, "Rser", new NodeLabelRequest(0, "x", "n1"));
        service.setNodeLabel(circuitId, "Rser", new NodeLabelRequest(0, "y", "n2"));
        service.createConnection(circuitId,
                new ConnectionCreateRequest("LK", new int[][]{{16, 16}, {48, 48}}, null));
        service.deleteComponent(circuitId, "U.1");
        service.undo(circuitId);

        byte[] bytes = CircuitFileWriter.write(model());
        CircuitModel reparsed = new CircuitFileParser().parse(new ByteArrayInputStream(bytes), "rt.ipes");

        assertNotNull(reparsed);
        Optional<CircuitModel.ComponentData> rser = reparsed.getAllComponents().stream()
                .filter(c -> "Rser".equals(c.getName())).findFirst();
        assertTrue(rser.isPresent(), "edited component must survive save/load");
        assertArrayEquals(new int[]{48, 48}, rser.get().getPosition());
        assertEquals(1, rser.get().getOrientation());
        assertEquals("n1", rser.get().getTerminalXLabels()[0]);
        assertEquals("n2", rser.get().getTerminalYLabels()[0]);
        assertEquals(123.0, rser.get().getRawParameters()[0], 1e-12);
    }

    // ========== Catalog ==========

    @Test
    void catalogContainsElectricalAndThermalTypes() {
        CatalogResponse catalog = service.getCatalog();

        assertTrue(catalog.types().stream().anyMatch(t -> t.type() == 1 && "LK_R".equals(t.name()) && "LK".equals(t.family())));
        assertTrue(catalog.types().stream().anyMatch(t -> t.type() == 46 && "THERM".equals(t.family())));
        assertTrue(catalog.types().stream().noneMatch(t -> "LK_TERMINAL".equals(t.name())), "terminals excluded");
    }

    // ========== Helpers ==========

    private CircuitModel.ComponentData findByName(String name) {
        CircuitModel.ComponentData comp = findByNameOrNull(name);
        assertNotNull(comp, "component not found: " + name);
        return comp;
    }

    private CircuitModel.ComponentData findByNameOrNull(String name) {
        return model().getAllComponents().stream()
                .filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }
}
