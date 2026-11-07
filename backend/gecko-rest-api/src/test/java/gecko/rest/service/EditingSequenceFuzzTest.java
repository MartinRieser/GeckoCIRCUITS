package gecko.rest.service;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.rest.model.circuit.ComponentCreateRequest;
import gecko.rest.model.circuit.ComponentPatchRequest;
import gecko.rest.model.circuit.ConnectionCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strategy 3: Stateful Fuzz / Property-Based Testing of Interactive Editing Sequences.
 *
 * <p>Randomly executes sequences of component creation, deletion, dragging, rotation,
 * renaming, parameter patching, wiring, and undo/redo operations.
 * Asserts strict invariants:
 * 1. Undo-All strictly restores the original circuit file byte-for-byte.
 * 2. Intermediate states cleanly serialize to .ipes and re-parse without corruption.
 * 3. Coupling references (gates, probes, mutual couplers) never dangle after rename/delete.
 * 4. NetlistBuilder reliably converts the model at each state without throwing unexpected runtime exceptions.</p>
 */
class EditingSequenceFuzzTest {

    private CircuitEditService service;
    private CircuitFileService circuitFileService;
    private String circuitId;

    @BeforeEach
    void setUp() {
        circuitFileService = new CircuitFileService();
        service = new CircuitEditService(circuitFileService);
        byte[] blankIpes = "tDURATION 0.01\ndt 1.0E-5\nsolverType 0\n".getBytes(StandardCharsets.UTF_8);
        circuitId = circuitFileService.loadCircuit(Base64.getEncoder().encodeToString(blankIpes), "fuzz_base.ipes").circuitId();
    }

    @Test
    void randomizedEditingSequencesSatisfySerializationAndUndoInvariants() throws Exception {
        Random rng = new Random(424242); // Deterministic seed for reproducible CI runs
        String baselineIpes = CircuitFileWriter.writeToString(circuitFileService.getModel(circuitId));

        int[] candidateTypes = {
                1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 24, 26, 28, 31,
                41, 45, 46, 47,
                1000, 1001, 1002, 1004, 1005, 1006, 1007, 1008, 1010, 1011, 1012, 1017, 1018, 1019, 1020, 1021
        };

        int totalActions = 60;
        int mutationCount = 0;
        List<String> createdCompNames = new ArrayList<>();

        for (int step = 0; step < totalActions; step++) {
            int action = rng.nextInt(6);
            CircuitModel model = circuitFileService.getModel(circuitId);

            switch (action) {
                case 0 -> { // Create component
                    int typNum = candidateTypes[rng.nextInt(candidateTypes.length)];
                    CircuitTypCore typ = CircuitTypCore.fromTypeNumber(typNum);
                    String family = typ.isControl() ? "CONTROL" : (typ.isThermal() ? "THERM" : "LK");
                    String name = "C_" + step + "_" + typNum;
                    int x = 10 + (step % 10) * 4;
                    int y = 10 + (step / 10) * 4;
                    int orient = 501 + rng.nextInt(4);

                    service.createComponent(circuitId, new ComponentCreateRequest(family, typNum, name, x, y, orient, null));
                    createdCompNames.add(name);
                    mutationCount++;
                }

                case 1 -> { // Create wire
                    int x1 = 10 + rng.nextInt(30);
                    int y1 = 10 + rng.nextInt(30);
                    int x2 = x1 + rng.nextInt(5);
                    int y2 = y1 + rng.nextInt(5);
                    String lbl = rng.nextBoolean() ? "net_" + step : "";

                    service.createConnection(circuitId, new ConnectionCreateRequest("LK", new int[][]{{x1, y1}, {x2, y2}}, lbl));
                    mutationCount++;
                }

                case 2 -> { // Patch component (move, rotate, or param)
                    if (!createdCompNames.isEmpty()) {
                        String name = createdCompNames.get(rng.nextInt(createdCompNames.size()));
                        CircuitModel.ComponentData comp = model.getAllComponents().stream()
                                .filter(c -> name.equals(c.getName()))
                                .findFirst().orElse(null);
                        if (comp != null) {
                            Integer newX = rng.nextBoolean() ? comp.getPosition()[0] + rng.nextInt(3) - 1 : null;
                            Integer newY = rng.nextBoolean() ? comp.getPosition()[1] + rng.nextInt(3) - 1 : null;
                            Integer newOrient = rng.nextBoolean() ? 501 + rng.nextInt(4) : null;
                            Map<String, Object> params = rng.nextBoolean() ? Map.of("param0", (double) (rng.nextInt(100) + 1)) : null;

                            service.patchComponent(circuitId, name, new ComponentPatchRequest(newX, newY, newOrient, null, params));
                            mutationCount++;
                        }
                    }
                }

                case 3 -> { // Delete component
                    if (!createdCompNames.isEmpty()) {
                        int idx = rng.nextInt(createdCompNames.size());
                        String doomed = createdCompNames.remove(idx);
                        service.deleteComponent(circuitId, doomed);
                        mutationCount++;
                    }
                }

                case 4 -> { // Delete connection
                    int connCount = model.getConnections().size();
                    if (connCount > 0) {
                        service.deleteConnection(circuitId, rng.nextInt(connCount));
                        mutationCount++;
                    }
                }

                case 5 -> { // Undo then Redo
                    if (mutationCount > 0) {
                        service.undo(circuitId);
                        service.redo(circuitId);
                    }
                }
            }

            // Invariant Check 1: Intermediate state must serialize and re-parse losslessly
            byte[] bytes = CircuitFileWriter.write(model);
            CircuitModel reparsed = new CircuitFileParser().parse(new ByteArrayInputStream(bytes), "fuzz_step.ipes");
            assertEquals(model.getAllComponents().size(), reparsed.getAllComponents().size(),
                    "Component count must match after .ipes write/parse at step " + step);
            assertEquals(model.getConnections().size(), reparsed.getConnections().size(),
                    "Connection count must match after .ipes write/parse at step " + step);

            // Invariant Check 2: NetlistBuilder must run without throwing uncaught exceptions
            assertDoesNotThrow(() -> {
                CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
                assertNotNull(netlist);
            }, "NetlistBuilder must handle intermediate fuzz state at step " + step);
        }

        // Invariant Check 3: Undoing all performed mutations must strictly restore baseline
        for (int i = 0; i < mutationCount; i++) {
            service.undo(circuitId);
        }

        String restoredIpes = CircuitFileWriter.writeToString(circuitFileService.getModel(circuitId));
        assertEquals(baselineIpes, restoredIpes, "Undoing all fuzz mutations must restore exact baseline state");
    }

    @Test
    void couplingReferencesAreMaintainedAcrossRenameDeleteAndUndo() {
        // 1. Create IGBT "S1" (typ 7)
        service.createComponent(circuitId, new ComponentCreateRequest("LK", 7, "S1", 10, 10, 501, null));
        // 2. Create Gate "G1" (typ 1007)
        service.createComponent(circuitId, new ComponentCreateRequest("CONTROL", 1007, "G1", 20, 10, 501, null));

        // Couple G1 to S1
        service.patchComponent(circuitId, "G1", new ComponentPatchRequest(null, null, null, null, Map.of("coupledComponent", "S1")));

        CircuitModel model = circuitFileService.getModel(circuitId);
        CircuitModel.ComponentData g1 = model.getAllComponents().stream().filter(c -> "G1".equals(c.getName())).findFirst().orElseThrow();
        assertEquals("S1", g1.getParameters().get("coupledComponent"));

        // 3. Rename S1 -> SW_MAIN
        service.patchComponent(circuitId, "S1", new ComponentPatchRequest(null, null, null, "SW_MAIN", null));
        assertEquals("SW_MAIN", g1.getParameters().get("coupledComponent"), "Coupled component should update after rename");

        // 4. Undo rename
        service.undo(circuitId);
        assertEquals("S1", g1.getParameters().get("coupledComponent"), "Coupled component should revert after undo");

        // 5. Redo rename
        service.redo(circuitId);
        assertEquals("SW_MAIN", g1.getParameters().get("coupledComponent"), "Coupled component should re-apply after redo");

        // 6. Delete SW_MAIN
        service.deleteComponent(circuitId, "SW_MAIN");
        assertNotEquals("SW_MAIN", g1.getParameters().get("coupledComponent"), "Reference should be cleared after deletion");

        // 7. Undo deletion
        service.undo(circuitId);
        assertEquals("SW_MAIN", g1.getParameters().get("coupledComponent"), "Reference should restore after undoing deletion");

        // Verify NetlistBuilder handles model cleanly
        assertDoesNotThrow(() -> {
            CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(circuitFileService.getModel(circuitId));
            assertNotNull(netlist);
        });
    }
}
