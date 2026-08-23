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
import gecko.rest.model.circuit.EditorModelResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
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
        // coordinates are grid units and stay as provided
        assertArrayEquals(new int[]{100, 50}, info.position());
        assertEquals(503, info.orientation(), "default orientation is NORTH_SOUTH (503)");

        assertTrue(model().getCircuitComponents().stream()
                .anyMatch(c -> c.getName().equals(info.name())));
    }

    @Test
    void createComponent_seedsClassicDefaults() {
        // resistor: classic default 1 kOhm, usable in simulation without manual editing
        CircuitChangeMessage r = service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "R_def", 16, 16, null, null));
        assertEquals(Map.of("param0", 1000.0, "resistance", 1000.0),
                payloadComponent(r).parameters());
        assertArrayEquals(new double[]{1000.0}, findByName("R_def").getRawParameters(), 1e-12);

        // voltage source: classic layout [0]=401 (DC), [1]=10 V, [2]=50 Hz, [20]=325 V amplitude
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 4, "U_def", 16, 16, null, null));
        CircuitModel.ComponentData source = findByName("U_def");
        assertEquals(401.0, source.getRawParameters()[0], 1e-12);
        assertEquals(10.0, source.getRawParameters()[1], 1e-12);
        assertEquals(50.0, source.getRawParameters()[2], 1e-12);
        assertEquals(325.0, source.getRawParameters()[20], 1e-12);

        // explicit parameters override the defaults
        service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "R_ovr", 16, 16, null, Map.of("param0", 47.0)));
        assertArrayEquals(new double[]{47.0}, findByName("R_ovr").getRawParameters(), 1e-12);
    }

    @Test
    void createComponent_customNameAndParameters() {
        CircuitChangeMessage result = service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "R_test", 32, 32, 502, Map.of("param0", 47.5)));

        ComponentInfo info = payloadComponent(result);
        assertEquals("R_test", info.name());
        assertEquals(502, info.orientation());

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
                new ComponentCreateRequest("LK", 1, "R9", 16, 16, 2, null)), "orientation out of range");
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
        service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "Rp", 32, 32, 503, Map.of("param0", 10.0)));

        CircuitChangeMessage result = service.patchComponent(circuitId, "Rp",
                new ComponentPatchRequest(80, 40, 504, "Rp_new", Map.of("param0", 20.0)));

        ComponentInfo info = payloadComponent(result);
        assertEquals("Rp_new", info.name());
        assertEquals(504, info.orientation());
        assertArrayEquals(new int[]{80, 40}, info.position());

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

    @Test
    void patchComponent_moveShiftsWirePointsOnItsTerminals() {
        // LK resistor at (30,30), orientation 503 (NORTH_SOUTH):
        // terminals at (30,28) input and (30,32) output
        service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "Rw", 30, 30, 503, null));
        service.createConnection(circuitId,
                new ConnectionCreateRequest("LK", new int[][]{{30, 28}, {20, 28}}, "attached"));
        service.createConnection(circuitId,
                new ConnectionCreateRequest("LK", new int[][]{{5, 5}, {6, 6}}, "detached"));

        service.patchComponent(circuitId, "Rw", new ComponentPatchRequest(40, 30, null, null, null));

        assertArrayEquals(new int[][]{{40, 28}, {20, 28}}, wireByLabel("attached").getPoints(),
                "wire point on the input terminal must shift with the component");
        assertArrayEquals(new int[][]{{5, 5}, {6, 6}}, wireByLabel("detached").getPoints(),
                "unrelated wire points must stay untouched");
    }

    @Test
    void patchComponent_undoRedoRestoresAndReappliesWirePoints() {
        service.createComponent(circuitId,
                new ComponentCreateRequest("LK", 1, "Ru2", 30, 30, 503, null));
        service.createConnection(circuitId,
                new ConnectionCreateRequest("LK", new int[][]{{30, 32}, {30, 40}}, "out"));

        service.patchComponent(circuitId, "Ru2", new ComponentPatchRequest(30, 36, null, null, null));
        assertArrayEquals(new int[][]{{30, 38}, {30, 40}}, wireByLabel("out").getPoints());

        service.undo(circuitId);
        assertArrayEquals(new int[][]{{30, 32}, {30, 40}}, wireByLabel("out").getPoints(),
                "undo must restore the wire point to its pre-move coordinates");

        service.redo(circuitId);
        assertArrayEquals(new int[][]{{30, 38}, {30, 40}}, wireByLabel("out").getPoints(),
                "redo must re-apply the wire shift");
    }

    @Test
    void patchComponent_moveShiftsSingleTerminalControlWires() {
        // catalog signal source (1004) at (8,14), orientation 502: single
        // output terminal at (10,14); scope (1003) at (24,14): single
        // input terminal at (22,14). Legacy CONTROL numbers enter the model
        // only through .ipes files; their geometry is covered by
        // ComponentTerminalsTest in gecko-simulation-core.
        service.createComponent(circuitId,
                new ComponentCreateRequest("CONTROL", 1004, "Sig1", 8, 14, 502, null));
        service.createComponent(circuitId,
                new ComponentCreateRequest("CONTROL", 1003, "Scope1", 24, 14, 502, null));
        service.createConnection(circuitId,
                new ConnectionCreateRequest("CONTROL", new int[][]{{10, 14}, {22, 14}}, "ctrl"));

        service.patchComponent(circuitId, "Sig1", new ComponentPatchRequest(12, 14, null, null, null));
        assertArrayEquals(new int[][]{{14, 14}, {22, 14}}, wireByLabel("ctrl").getPoints(),
                "signal source must move the wire point on its output terminal");

        service.patchComponent(circuitId, "Scope1", new ComponentPatchRequest(28, 14, null, null, null));
        assertArrayEquals(new int[][]{{14, 14}, {26, 14}}, wireByLabel("ctrl").getPoints(),
                "scope must move the wire point on its input terminal");
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
                new ComponentCreateRequest("LK", 1, "Ru", 32, 32, 503, Map.of("param0", 11.0)));
        service.patchComponent(circuitId, "Ru", new ComponentPatchRequest(64, 64, 501, null, Map.of("param0", 99.0)));

        service.undo(circuitId);

        CircuitModel.ComponentData comp = findByName("Ru");
        assertArrayEquals(new int[]{32, 32}, comp.getPosition());
        assertEquals(503, comp.getOrientation());
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
        assertEquals(100, wire.points()[0][0]);
        assertEquals(260, wire.points()[2][1]);
        assertEquals(before + 1, model().getConnections().size());

        CircuitChangeMessage patched = service.patchConnection(circuitId, wire.index(),
                new ConnectionPatchRequest(new int[][]{{96, 200}, {160, 210}}, "w1b"));
        assertEquals("w1b", payloadWire(patched).label());
        assertArrayEquals(new int[]{160, 210}, payloadWire(patched).points()[1]);

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
                new ComponentCreateRequest("LK", 1, "Rser", 16, 16, 502, Map.of("param0", 123.0)));
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
        assertEquals(502, rser.get().getOrientation());
        assertEquals("n1", rser.get().getTerminalXLabels()[0]);
        assertEquals("n2", rser.get().getTerminalYLabels()[0]);
        assertEquals(123.0, rser.get().getRawParameters()[0], 1e-12);
    }

    @Test
    void rotationCycleMatchesGuiOrder() {
        // GUI: NORTH_SOUTH -> EAST_WEST -> SOUTH_NORTH -> WEST_EAST
        assertEquals(504, CircuitEditService.rotateOrientation(503));
        assertEquals(501, CircuitEditService.rotateOrientation(504));
        assertEquals(502, CircuitEditService.rotateOrientation(501));
        assertEquals(503, CircuitEditService.rotateOrientation(502));
    }

    // ========== Editor snapshot ==========

    @Test
    void editorModelContainsComponentsWiresAndVersion() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "Rem", 16, 16, null, null));
        service.setNodeLabel(circuitId, "Rem", new NodeLabelRequest(0, "x", "n1"));
        service.createConnection(circuitId,
                new ConnectionCreateRequest("LK", new int[][]{{14, 14}, {16, 14}}, null));

        EditorModelResponse snapshot = service.getEditorModel(circuitId);

        assertEquals(circuitId, snapshot.circuitId());
        assertEquals(3, snapshot.modelVersion());
        assertTrue(snapshot.dpix() > 0);
        EditorModelResponse.Component em = snapshot.components().stream()
                .filter(c -> "Rem".equals(c.name())).findFirst().orElseThrow();
        assertEquals("LK", em.family());
        assertEquals("n1", em.inputLabels()[0]);

        assertFalse(snapshot.connections().isEmpty());
        EditorModelResponse.Wire lastWire = snapshot.connections().get(snapshot.connections().size() - 1);
        assertEquals("LK", lastWire.type());
        assertEquals(2, lastWire.points().length);
        assertEquals(snapshot.connections().size() - 1, lastWire.index(), "wire index matches list position");
    }

    @Test
    void editorModel_unknownCircuit_404() {
        assertThrows(ResponseStatusException.class, () -> service.getEditorModel("no-such"));
    }

    @Test
    void editorModelCarriesSimulationDefaultsAndDerivedSignals() {
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "Rdef", 16, 16, null, null));
        service.setNodeLabel(circuitId, "Rdef", new NodeLabelRequest(0, "x", "dc_link"));
        service.setNodeLabel(circuitId, "Rdef", new NodeLabelRequest(0, "y", "gnd"));

        EditorModelResponse.SimulationDefaults defaults = service.getEditorModel(circuitId)
                .simulationDefaults();

        assertThat(defaults).isNotNull();
        assertThat(defaults.timeStep()).isGreaterThan(0);
        assertThat(defaults.duration()).isGreaterThan(0);
        assertThat(defaults.solverType()).isIn("backward-euler", "trapezoidal", "gear-shichman");
        // fixture has no dataContainerSignals: derived from node labels, ground excluded
        assertThat(defaults.signals()).contains("dc_link").doesNotContain("gnd");
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

    private CircuitModel.ConnectionData wireByLabel(String label) {
        return model().getConnections().stream()
                .filter(c -> label.equals(c.getLabel()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("wire not found: " + label));
    }

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
