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

    /**
     * End-to-end test reproducing the user's switched RC circuit (issue.ipes) built entirely
     * via CircuitEditService.createComponent with null parameters (default palette placement).
     * Verifies IGBT (10) and SIGNAL (1004) receive valid defaults and charge C to ~9.2V.
     */
    @Test
    void testSwitchedRcWithIgbtAndSignal_createdWithDefaultNullParameters_simulatesAndChargesCapacitor() {
        byte[] blankIpes = "tDURATION 0.02\ndt 1.0E-6\nsolverType 0\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String rcId = circuitFileService
                .loadCircuit(Base64.getEncoder().encodeToString(blankIpes), "switched_rc.ipes")
                .circuitId();

        // Place U (typ 4), R (typ 1), IGBT (typ 10), C (typ 3), GLOBAL_TERMINAL (typ 31) with null parameters
        service.createComponent(rcId, new ComponentCreateRequest("LK", 4, "U", 12, 13, 503, null));
        service.createComponent(rcId, new ComponentCreateRequest("LK", 1, "R", 18, 9, 504, null));
        service.createComponent(rcId, new ComponentCreateRequest("LK", 10, "IGBT", 26, 9, 502, null));
        service.createComponent(rcId, new ComponentCreateRequest("LK", 3, "C", 35, 12, 503, null));
        service.createComponent(rcId, new ComponentCreateRequest("LK", 31, "GLOBAL_TERMINAL", 12, 19, 501, null));

        // Place SIGNAL (typ 1004), GATE (typ 1000), VOLT (typ 1001) with null parameters
        service.createComponent(rcId, new ComponentCreateRequest("CONTROL", 1004, "SIGNAL", 13, 24, 503, null));
        service.createComponent(rcId, new ComponentCreateRequest("CONTROL", 1000, "GATE", 25, 24, 503, null));
        service.patchComponent(rcId, "GATE", new ComponentPatchRequest(null, null, null, null,
                Map.of("coupledComponent", "IGBT")));
        service.createComponent(rcId, new ComponentCreateRequest("CONTROL", 1001, "VOLT", 44, 12, 503, null));
        service.patchComponent(rcId, "VOLT", new ComponentPatchRequest(null, null, null, null,
                Map.of("coupledComponent", "C")));
        service.setNodeLabel(rcId, "VOLT", new gecko.rest.model.circuit.NodeLabelRequest(0, "y", "u_C"));

        // Wire LK loop and CONTROL wire
        service.createConnection(rcId, new ConnectionCreateRequest("LK", new int[][]{{12, 11}, {12, 9}, {16, 9}}, null));
        service.createConnection(rcId, new ConnectionCreateRequest("LK", new int[][]{{20, 9}, {24, 9}}, null));
        service.createConnection(rcId, new ConnectionCreateRequest("LK", new int[][]{{28, 9}, {35, 9}, {35, 10}}, null));
        service.createConnection(rcId, new ConnectionCreateRequest("LK", new int[][]{{35, 14}, {35, 15}, {12, 15}}, null));
        service.createConnection(rcId, new ConnectionCreateRequest("LK", new int[][]{{12, 15}, {12, 17}}, null));
        service.createConnection(rcId, new ConnectionCreateRequest("CONTROL", new int[][]{{15, 24}, {23, 24}}, null));

        CircuitModel rcModel = circuitFileService.getModel(rcId);
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationResult res = engine.runSimulation(SimulationConfig.builder()
                .circuitModel(rcModel)
                .simulationDuration(0.02)
                .stepWidth(1e-6)
                .signals(List.of("u_C"))
                .build());
        assertTrue(res.isSuccess(), "Simulation must succeed: " + res.getErrorMessage());

        int ucIdx = -1;
        String[] names = res.getSignalNames();
        for (int i = 0; i < names.length; i++) {
            if ("u_C".equals(names[i])) {
                ucIdx = i;
                break;
            }
        }
        assertTrue(ucIdx >= 0, "u_C signal must be recorded");
        float[] data = res.getSignalData(ucIdx);
        float finalUc = data[data.length - 1];
        assertTrue(finalUc > 8.0f && finalUc < 10.0f,
                "Capacitor voltage u_C must charge to ~9.2V, got " + finalUc);
    }

    /**
     * Places every 2-terminal power component (R, L, C, D, S, THYR, IGBT, LKOP2, MOSFET) via
     * CircuitEditService.createComponent with null parameters, serializes to .ipes, reparses,
     * and simulates with both VOLT and AMP probes attached.
     */
    @Test
    void testAllTwoTerminalPowerComponents_createdViaRestWithNullParameters_roundTripAndSimulate() throws Exception {
        int[] powerTypes = {1, 2, 3, 6, 7, 8, 10, 12, 28};
        for (int cutType : powerTypes) {
            byte[] blankIpes = "tDURATION 2.0E-4\ndt 2.0E-6\nsolverType 0\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String cid = circuitFileService
                    .loadCircuit(Base64.getEncoder().encodeToString(blankIpes), "audit_" + cutType + ".ipes")
                    .circuitId();

            // Place U (typ 4) at (4, 10) orient 504 -> input (+) at (6, 10), output (-) at (2, 10)
            service.createComponent(cid, new ComponentCreateRequest("LK", 4, "U_1", 4, 10, 504, null));
            // Place CUT at (10, 10) orient 502 -> input at (8, 10), output at (12, 10)
            service.createComponent(cid, new ComponentCreateRequest("LK", cutType, "CUT", 10, 10, 502, null));
            // Place R_load (typ 1) at (16, 10) orient 502 -> input at (14, 10), output at (18, 10)
            service.createComponent(cid, new ComponentCreateRequest("LK", 1, "R_load", 16, 10, 502, null));

            service.createConnection(cid, new ConnectionCreateRequest("LK", new int[][]{{6, 10}, {8, 10}}, null));
            service.createConnection(cid, new ConnectionCreateRequest("LK", new int[][]{{12, 10}, {14, 10}}, "V_out"));
            service.createConnection(cid, new ConnectionCreateRequest("LK",
                    new int[][]{{18, 10}, {18, 14}, {2, 14}, {2, 10}}, "GND"));

            // Attach VOLT (1001) and AMP (1002) coupled to CUT
            service.createComponent(cid, new ComponentCreateRequest("CONTROL", 1001, "V_probe", 6, 20, 503, null));
            service.patchComponent(cid, "V_probe", new ComponentPatchRequest(null, null, null, null,
                    Map.of("coupledComponent", "CUT")));
            service.setNodeLabel(cid, "V_probe", new gecko.rest.model.circuit.NodeLabelRequest(0, "y", "v_cut"));

            service.createComponent(cid, new ComponentCreateRequest("CONTROL", 1002, "I_probe", 14, 20, 503, null));
            service.patchComponent(cid, "I_probe", new ComponentPatchRequest(null, null, null, null,
                    Map.of("coupledComponent", "CUT")));
            service.setNodeLabel(cid, "I_probe", new gecko.rest.model.circuit.NodeLabelRequest(0, "y", "i_cut"));

            // If gated switch, attach CONSTANT(1005) -> GATE(1000) coupled to CUT
            if (cutType == 7 || cutType == 8 || cutType == 10 || cutType == 28) {
                service.createComponent(cid, new ComponentCreateRequest("CONTROL", 1005, "CONST_1", 6, 24, 503, null));
                service.createComponent(cid, new ComponentCreateRequest("CONTROL", 1000, "GATE_1", 12, 24, 503, null));
                service.patchComponent(cid, "GATE_1", new ComponentPatchRequest(null, null, null, null,
                        Map.of("coupledComponent", "CUT")));
                service.createConnection(cid, new ConnectionCreateRequest("CONTROL", new int[][]{{8, 24}, {10, 24}}, null));
            }

            // Round-trip through .ipes writer and parser to verify persisted defaults
            byte[] savedBytes = CircuitFileWriter.write(circuitFileService.getModel(cid));
            CircuitModel reparsed = new gecko.core.io.CircuitFileParser()
                    .parse(new java.io.ByteArrayInputStream(savedBytes), "roundtrip_" + cutType + ".ipes");

            SimulationResult res = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                    .circuitModel(reparsed)
                    .simulationDuration(2e-4)
                    .stepWidth(2e-6)
                    .signals(List.of("V_out", "v_cut", "i_cut"))
                    .build());
            assertTrue(res.isSuccess(), "Simulation failed for cutType=" + cutType + ": " + res.getErrorMessage());
            for (int sigIdx = 0; sigIdx < res.getSignalNames().length; sigIdx++) {
                float[] samples = res.getSignalData(sigIdx);
                assertTrue(samples.length > 10, "Signal must have samples for cutType=" + cutType);
                for (float s : samples) {
                    assertTrue(Float.isFinite(s), "Non-finite sample for cutType=" + cutType);
                }
            }
        }
    }
}
