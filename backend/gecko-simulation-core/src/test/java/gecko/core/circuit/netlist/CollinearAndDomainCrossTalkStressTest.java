/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit.netlist;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5: Topology & Wiring Stress Tests.
 *
 * Validates:
 * - Task 5.2: Collinear & Orthogonal Crossings Stress Test: Perpendicular crossing wires
 *   never merge electrical nodes unless an explicit T-junction terminal vertex is present.
 * - Task 5.3: Domain Cross-Talk Stress Test: Electrical (LK), Thermal, and Control domains
 *   sharing coordinates or labels are completely isolated without matrix cross-talk.
 */
public class CollinearAndDomainCrossTalkStressTest {

    private static CircuitModel.ComponentData component(int type, String name, int x, int y, int orientation) {
        CircuitModel.ComponentData comp = new CircuitModel.ComponentData(type, name, x, y, orientation);
        comp.setParameter("param0", 100.0);
        return comp;
    }

    private static int[][] dense(String points) {
        String[] tokens = points.split(" ");
        int[][] result = new int[tokens.length / 2][2];
        for (int i = 0; i < result.length; i++) {
            result[i][0] = Integer.parseInt(tokens[2 * i]);
            result[i][1] = Integer.parseInt(tokens[2 * i + 1]);
        }
        return result;
    }

    @Test
    @DisplayName("Task 5.2: Perpendicular crossing wires without vertex endpoints create distinct electrical nodes")
    void testPerpendicularCrossingWiresDoNotShort() {
        CircuitModel model = new CircuitModel();

        // Circuit 1 (Horizontal bus):
        // V1 at (10, 10) WEST_EAST (502): input at (8, 10), output at (12, 10)
        CircuitModel.ComponentData v1 = component(4, "V1", 10, 10, gecko.core.circuit.ComponentTerminals.WEST_EAST);
        model.addCircuitComponent(v1);

        // R1 at (20, 10) WEST_EAST (502): input at (18, 10), output at (22, 10)
        CircuitModel.ComponentData r1 = component(1, "R1", 20, 10, gecko.core.circuit.ComponentTerminals.WEST_EAST);
        model.addCircuitComponent(r1);

        // Horizontal wire linking (12, 10) to (18, 10)
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                dense("12 10 13 10 14 10 15 10 16 10 17 10 18 10")));

        // Circuit 2 (Vertical bus):
        // V2 at (15, 5) NORTH_SOUTH (503): input at (15, 3), output at (15, 7)
        CircuitModel.ComponentData v2 = component(4, "V2", 15, 5, gecko.core.circuit.ComponentTerminals.NORTH_SOUTH);
        model.addCircuitComponent(v2);

        // R2 at (15, 15) NORTH_SOUTH (503): input at (15, 13), output at (15, 17)
        CircuitModel.ComponentData r2 = component(1, "R2", 15, 15, gecko.core.circuit.ComponentTerminals.NORTH_SOUTH);
        model.addCircuitComponent(r2);

        // Vertical wire linking (15, 7) to (15, 13)
        // Notice: crosses horizontal wire at (15, 10) — an INTERIOR point of both wires!
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                dense("15 7 15 8 15 9 15 10 15 11 15 12 15 13")));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertNotNull(netlist);

        // In CircuitNetlist:
        // V1 is element 0, R1 is element 1
        // V2 is element 2, R2 is element 3
        int v1OutNode = netlist.getNodeY(0);
        int r1InNode = netlist.getNodeX(1);
        assertEquals(v1OutNode, r1InNode, "V1 output must connect to R1 input across horizontal wire");

        int v2OutNode = netlist.getNodeY(2);
        int r2InNode = netlist.getNodeX(3);
        assertEquals(v2OutNode, r2InNode, "V2 output must connect to R2 input across vertical wire");

        // CRITICAL INVARIANT: The horizontal and vertical wires crossed at (15, 10),
        // but because (15, 10) is not an endpoint of either wire, they MUST NOT SHORT!
        assertNotEquals(v1OutNode, v2OutNode,
                "Perpendicular crossing wires without an endpoint T-junction MUST NOT merge electrical nodes!");
    }

    @Test
    @DisplayName("Task 5.2: Explicit T-junction endpoint on crossing wire successfully merges nodes")
    void testExplicitTJunctionMergesNodes() {
        CircuitModel model = new CircuitModel();

        // V1 at (10, 10) WEST_EAST (502): output terminal at (12, 10)
        CircuitModel.ComponentData v1 = component(4, "V1", 10, 10, gecko.core.circuit.ComponentTerminals.WEST_EAST);
        model.addCircuitComponent(v1);

        // R1 at (20, 10) WEST_EAST (502): input terminal at (18, 10)
        CircuitModel.ComponentData r1 = component(1, "R1", 20, 10, gecko.core.circuit.ComponentTerminals.WEST_EAST);
        model.addCircuitComponent(r1);

        // Trunk wire: (12, 10) to (18, 10)
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                dense("12 10 13 10 14 10 15 10 16 10 17 10 18 10")));

        // R2 at (15, 15) NORTH_SOUTH (503): input terminal at (15, 13)
        CircuitModel.ComponentData r2 = component(1, "R2", 15, 15, gecko.core.circuit.ComponentTerminals.NORTH_SOUTH);
        model.addCircuitComponent(r2);

        // Tap wire: Starts AT (15, 10) (an ENDPOINT of this wire touching trunk wire!) and goes to (15, 13)
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                dense("15 10 15 11 15 12 15 13")));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertNotNull(netlist);

        int v1OutNode = netlist.getNodeY(0);
        int r1InNode = netlist.getNodeX(1);
        int r2InNode = netlist.getNodeX(2);

        assertEquals(v1OutNode, r1InNode, "V1 output connects to R1 input");
        assertEquals(v1OutNode, r2InNode,
                "Explicit T-junction where wire endpoint touches trunk wire MUST merge R2 into the same node!");
    }

    @Test
    @DisplayName("Task 5.3: Domain Isolation: Electrical (LK), Thermal, and Control domains sharing coordinates never cross-talk")
    void testDomainCrossTalkIsolation() {
        CircuitModel model = new CircuitModel();

        // 1. Electrical resistor at (10, 10) WEST_EAST: terminals at (8, 10) and (12, 10)
        CircuitModel.ComponentData lkR = component(1, "R1", 10, 10, 502);
        model.addCircuitComponent(lkR);

        // 2. Control block placed at identical coordinate (10, 10)
        CircuitModel.ComponentData ctrlBlock = new CircuitModel.ComponentData(1, "CTRL_PROBE", 10, 10, 502);
        model.addControlComponent(ctrlBlock);

        // 3. Thermal component at (20, 10)
        CircuitModel.ComponentData thermR = new CircuitModel.ComponentData(1, "R_TH", 20, 10, 502);
        model.addThermalComponent(thermR);

        // 4. Interleaved wires at (10, 10) with distinct domains:
        // LK wire: (8, 10) -> (12, 10)
        model.getConnections().add(new CircuitModel.ConnectionData("LK", dense("8 10 9 10 10 10 11 10 12 10")));
        // CONTROL wire touching (10, 10)
        model.getConnections().add(new CircuitModel.ConnectionData("CONTROL", dense("10 10 10 15")));
        // THERMAL wire touching (20, 10)
        model.getConnections().add(new CircuitModel.ConnectionData("THERMAL", dense("18 10 20 10")));

        // Build netlist
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertNotNull(netlist);

        // Control blocks must NEVER be stamped as electrical branches
        assertEquals(2, netlist.getElementCount(), "Only LK and THERMAL elements must be in Netlist; CONTROL is excluded");
        assertEquals(CircuitTypCore.LK_R, netlist.getType(0));
        assertEquals(CircuitTypCore.LK_R, netlist.getType(1));
    }
}
