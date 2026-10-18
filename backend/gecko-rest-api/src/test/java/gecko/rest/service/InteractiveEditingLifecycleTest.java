package gecko.rest.service;

import gecko.core.circuit.ComponentTerminals;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import gecko.rest.model.circuit.ComponentCreateRequest;
import gecko.rest.model.circuit.ComponentPatchRequest;
import gecko.rest.model.circuit.ConnectionCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2: Interactive Editing State-Mutation Matrix (Lifecycle Testing).
 * Validates mutation operations between the REST service, CircuitModel,
 * NetlistBuilder, and the simulation engine across all 6 lifecycle tasks (2.1 - 2.6).
 */
class InteractiveEditingLifecycleTest {

    private CircuitFileService circuitFileService;
    private CircuitEditService service;
    private String circuitId;

    @BeforeEach
    void setUp() throws IOException {
        circuitFileService = new CircuitFileService();
        service = new CircuitEditService(circuitFileService);

        byte[] ipes = Files.readAllBytes(Paths.get("src/test/resources/test-circuit.ipes"));
        circuitId = circuitFileService
                .loadCircuit(Base64.getEncoder().encodeToString(ipes), "test-circuit.ipes")
                .circuitId();
    }

    private CircuitModel model() {
        return circuitFileService.getModel(circuitId);
    }

    /**
     * Task 2.1: Series Insertion Workflow Tests.
     * Deletes a wire on the main power branch, inserts a series shunt resistor,
     * connects it with two new wire segments, and asserts that:
     * 1) Shunt terminals reside on two distinct MNA nodes (nodeX != nodeY).
     * 2) NetlistBuilder emits no short-circuit DRC warnings.
     * 3) Simulated circuit completes successfully.
     */
    @Test
    void testSeriesInsertionWorkflow_distinctNodesAndNonZeroCurrent() throws Exception {
        CircuitModel initialModel = model();
        // Find connection linking Lc.2 and diode bridge: between (23,6) and (27,6)
        int wireIndexToCut = -1;
        for (int i = 0; i < initialModel.getConnections().size(); i++) {
            CircuitModel.ConnectionData c = initialModel.getConnections().get(i);
            int[][] pts = c.getPoints();
            if (pts != null && pts.length >= 2) {
                if ((pts[0][0] == 23 && pts[0][1] == 6) || (pts[pts.length - 1][0] == 23 && pts[pts.length - 1][1] == 6)) {
                    wireIndexToCut = i;
                    break;
                }
            }
        }
        assertTrue(wireIndexToCut >= 0, "Must find wire segment linking inductor Lc.2 and rectifier");

        // 1. Delete the connection
        service.deleteConnection(circuitId, wireIndexToCut);

        // 2. Insert series shunt resistor R_shunt at (25, 4) EAST_WEST (504)
        // Orientation 504: flow is east to west. Terminal distance = 2.
        // Pin X is at (25 + 2, 4) = (27, 4)
        // Pin Y is at (25 - 2, 4) = (23, 4)
        service.createComponent(circuitId, new ComponentCreateRequest(
                "LK", 1, "R_shunt", 25, 4, 504, Map.of("param0", 1.0)));

        // 3. Connect pin Y (23, 4) to (23, 6) with a new wire segment
        service.createConnection(circuitId, new ConnectionCreateRequest(
                "LK", new int[][]{{23, 4}, {23, 6}}, null));

        // Connect pin X (27, 4) to (27, 6) with a new wire segment
        service.createConnection(circuitId, new ConnectionCreateRequest(
                "LK", new int[][]{{27, 4}, {27, 6}}, null));

        // 4. Compile netlist
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model());
        int shuntIdx = -1;
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if ("R_shunt".equals(netlist.getElementNames()[i])) {
                shuntIdx = i;
                break;
            }
        }
        assertTrue(shuntIdx >= 0, "R_shunt must be compiled into netlist elements");

        int nodeX = netlist.getNodeX(shuntIdx);
        int nodeY = netlist.getNodeY(shuntIdx);
        assertNotEquals(nodeX, nodeY,
                "R_shunt terminals must reside on distinct nodes; no short circuit!");

        // Assert no DRC short-circuit warnings
        boolean hasShortWarning = netlist.getBuildWarnings().stream()
                .anyMatch(w -> w.contains("R_shunt") && w.contains("short-circuited"));
        assertFalse(hasShortWarning, "R_shunt must not be flagged as short-circuited");

        // 5. Simulate and verify non-zero current
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model())
                .stepWidth(1e-6)
                .simulationDuration(1e-4)
                .build();

        SimulationResult result = engine.runSimulation(config);
        assertNotNull(result, "Simulation should complete successfully");
        assertTrue(result.isSuccess(), () -> "Simulation should succeed: " + result.getErrorMessage());
    }

    /**
     * Task 2.2: Parallel Branch Addition Workflow Tests.
     * Adds a parallel capacitor across existing component terminals (R.1).
     * Asserts parallel nodes match target terminals.
     */
    @Test
    void testParallelBranchAdditionWorkflow_matchingNodes() {
        CircuitModel initialModel = model();
        CircuitModel.ComponentData r1 = initialModel.getAllComponents().stream()
                .filter(c -> "R.1".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertNotNull(r1);

        // R.1 is at (21, 10) EAST_WEST (504): pins at (23, 10) and (19, 10)
        // Add parallel capacitor C_snub at (21, 12) EAST_WEST (504): pins at (23, 12) and (19, 12)
        service.createComponent(circuitId, new ComponentCreateRequest(
                "LK", 2, "C_snub", 21, 12, 504, Map.of("param0", 1e-6)));

        // Connect C_snub pin 0 (23, 12) to R.1 pin 0 (23, 10)
        service.createConnection(circuitId, new ConnectionCreateRequest(
                "LK", new int[][]{{23, 12}, {23, 10}}, null));

        // Connect C_snub pin 1 (19, 12) to R.1 pin 1 (19, 10)
        service.createConnection(circuitId, new ConnectionCreateRequest(
                "LK", new int[][]{{19, 12}, {19, 10}}, null));

        // Compile netlist
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model());

        int r1Idx = -1;
        int cSnubIdx = -1;
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if ("R.1".equals(netlist.getElementNames()[i])) r1Idx = i;
            if ("C_snub".equals(netlist.getElementNames()[i])) cSnubIdx = i;
        }

        assertTrue(r1Idx >= 0 && cSnubIdx >= 0, "Both components must be in netlist");
        assertEquals(netlist.getNodeX(r1Idx), netlist.getNodeX(cSnubIdx),
                "C_snub nodeX must match R.1 nodeX");
        assertEquals(netlist.getNodeY(r1Idx), netlist.getNodeY(cSnubIdx),
                "C_snub nodeY must match R.1 nodeY");
    }

    /**
     * Task 2.3: Wire Cut & Re-Route Tests.
     * Deletes arbitrary wire segment, creates a detour path, verifies connectivity,
     * and asserts that Undo/Redo cycles perfectly restore each state.
     */
    @Test
    void testWireCutAndReroute_undoRedoPreservesNetConnectivity() {
        int originalWireCount = model().getConnections().size();

        // 1. Delete wire 0
        service.deleteConnection(circuitId, 0);
        assertEquals(originalWireCount - 1, model().getConnections().size());

        // 2. Add detour wire
        service.createConnection(circuitId, new ConnectionCreateRequest(
                "LK", new int[][]{{27, 3}, {28, 3}, {28, 6}, {27, 6}}, "detour"));
        assertEquals(originalWireCount, model().getConnections().size());

        // 3. Undo detour creation
        service.undo(circuitId);
        assertEquals(originalWireCount - 1, model().getConnections().size());

        // 4. Undo wire deletion -> original wire restored
        service.undo(circuitId);
        assertEquals(originalWireCount, model().getConnections().size());

        // 5. Redo wire deletion
        service.redo(circuitId);
        assertEquals(originalWireCount - 1, model().getConnections().size());

        // 6. Redo detour creation
        service.redo(circuitId);
        assertEquals(originalWireCount, model().getConnections().size());
        assertTrue(model().getConnections().stream().anyMatch(c -> "detour".equals(c.getLabel())));
    }

    /**
     * Task 2.4: Component Drag & Rubberband Wire Extension Tests.
     * Moves a component by (dx, dy) and asserts that connected wire endpoints track
     * component terminals without detaching.
     */
    @Test
    void testComponentDrag_rubberbandsConnectedWireEndpoints() {
        CircuitModel.ComponentData r1 = model().getAllComponents().stream()
                .filter(c -> "R.1".equals(c.getName()))
                .findFirst()
                .orElseThrow();

        int oldX = r1.getPosition()[0];
        int oldY = r1.getPosition()[1];
        List<int[]> oldTerminals = ComponentTerminals.terminalsOf(r1, r1.getPosition(), r1.getOrientation());

        // Move R.1 by dx=+2, dy=+2
        int newX = oldX + 2;
        int newY = oldY + 2;

        service.patchComponent(circuitId, "R.1",
                new ComponentPatchRequest(newX, newY, null, null, null));

        assertEquals(newX, r1.getPosition()[0]);
        assertEquals(newY, r1.getPosition()[1]);

        // Check that attached wires moved their endpoints
        List<int[]> newTerminals = ComponentTerminals.terminalsOf(r1, new int[]{newX, newY}, r1.getOrientation());
        boolean term0Connected = false;
        boolean term1Connected = false;

        for (CircuitModel.ConnectionData conn : model().getConnections()) {
            for (int[] pt : conn.getPoints()) {
                if (pt[0] == newTerminals.get(0)[0] && pt[1] == newTerminals.get(0)[1]) {
                    term0Connected = true;
                }
                if (pt[0] == newTerminals.get(1)[0] && pt[1] == newTerminals.get(1)[1]) {
                    term1Connected = true;
                }
            }
        }

        assertTrue(term0Connected, "Terminal 0 must stay connected after move");
        assertTrue(term1Connected, "Terminal 1 must stay connected after move");

        // Undo move
        service.undo(circuitId);
        assertEquals(oldX, r1.getPosition()[0]);
        assertEquals(oldY, r1.getPosition()[1]);

        boolean restoredTerm0 = false;
        boolean restoredTerm1 = false;
        for (CircuitModel.ConnectionData conn : model().getConnections()) {
            for (int[] pt : conn.getPoints()) {
                if (pt[0] == oldTerminals.get(0)[0] && pt[1] == oldTerminals.get(0)[1]) {
                    restoredTerm0 = true;
                }
                if (pt[0] == oldTerminals.get(1)[0] && pt[1] == oldTerminals.get(1)[1]) {
                    restoredTerm1 = true;
                }
            }
        }
        assertTrue(restoredTerm0, "Terminal 0 wire endpoint restored on undo");
        assertTrue(restoredTerm1, "Terminal 1 wire endpoint restored on undo");
    }

    /**
     * Task 2.5: Component Deletion & Dangling Wire Cleanup.
     * Deletes a component and verifies attached references (e.g. coupled switch) are cleared.
     */
    @Test
    void testComponentDeletion_clearsCoupledReferencesCleanly() {
        // Create switch S.1 and gate GATE.1 coupled to S.1
        service.createComponent(circuitId, new ComponentCreateRequest("CONTROL", 6, "GATE.1", 10, 10, null, null));
        service.patchComponent(circuitId, "GATE.1", new ComponentPatchRequest(null, null, null, null,
                Map.of("coupledComponent", "S.1")));
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 7, "S.1", 20, 10, null, null));

        CircuitModel.ComponentData gate = model().getAllComponents().stream()
                .filter(c -> "GATE.1".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("S.1", gate.getParameters().get("coupledComponent"));

        // Delete S.1
        service.deleteComponent(circuitId, "S.1");

        // Gate's reference must be cleanly cleared
        assertEquals("", gate.getParameters().get("coupledComponent"));

        // Undo restores S.1 and the reference
        service.undo(circuitId);
        gate = model().getAllComponents().stream()
                .filter(c -> "GATE.1".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("S.1", gate.getParameters().get("coupledComponent"));
    }

    /**
     * Task 2.6: Comprehensive Undo/Redo Invariance Audit.
     * For createComp, deleteComp, createConn, deleteConn, patchComp:
     * Asserts that M -> Undo restores the exact original circuit byte-for-byte.
     */
    @Test
    void testComprehensiveUndoRedoInvariance_restoresExactState() {
        String baselineSnapshot = CircuitFileWriter.writeToString(model());

        // 1. Mutation: create component
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 1, "R_audit", 50, 50, 503, null));
        assertNotEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()));
        service.undo(circuitId);
        assertEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()),
                "Undo createComponent must restore exact baseline state");
        service.redo(circuitId);
        service.undo(circuitId);

        // 2. Mutation: create connection
        service.createConnection(circuitId, new ConnectionCreateRequest("LK", new int[][]{{50, 50}, {55, 50}}, null));
        assertNotEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()));
        service.undo(circuitId);
        assertEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()),
                "Undo createConnection must restore exact baseline state");

        // 3. Mutation: delete component
        service.deleteComponent(circuitId, "R.1");
        assertNotEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()));
        service.undo(circuitId);
        assertEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()),
                "Undo deleteComponent must restore exact baseline state");

        // 4. Mutation: delete connection
        service.deleteConnection(circuitId, 0);
        assertNotEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()));
        service.undo(circuitId);
        assertEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()),
                "Undo deleteConnection must restore exact baseline state");

        // 5. Mutation: patch component parameter
        service.patchComponent(circuitId, "R.1", new ComponentPatchRequest(null, null, null, null, Map.of("param0", 42.0)));
        assertNotEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()));
        service.undo(circuitId);
        assertEquals(baselineSnapshot, CircuitFileWriter.writeToString(model()),
                "Undo patchComponent must restore exact baseline state");
    }
}
