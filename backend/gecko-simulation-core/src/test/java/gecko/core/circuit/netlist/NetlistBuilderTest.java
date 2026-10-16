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

import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NetlistBuilder factory class.
 *
 * <p>Validates that the factory correctly creates empty netlists and handles
 * CircuitModel conversion without errors.</p>
 */
public class NetlistBuilderTest {

    @Test
    public void testBuildEmptyCreatesNetlistWithCorrectDimensions() {
        // Arrange
        int nodeCount = 5;
        int voltageSourceCount = 2;
        int elementCount = 8;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(nodeCount, voltageSourceCount, elementCount);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(nodeCount - 1, netlist.getNodeMax(), "Node count should match");
        assertEquals(voltageSourceCount, netlist.getVoltageSourceMax(), "Voltage source count should match");
        assertEquals(elementCount, netlist.getElementCount(), "Element count should match");
    }

    @Test
    public void testBuildEmptyWithZeroDimensions() {
        // Arrange - Edge case: empty circuit
        int nodeCount = 0;
        int voltageSourceCount = 0;
        int elementCount = 0;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(nodeCount, voltageSourceCount, elementCount);

        // Assert
        assertNotNull(netlist, "Empty netlist should not be null");
        assertEquals(0, netlist.getNodeMax(), "Zero node count");
        assertEquals(0, netlist.getVoltageSourceMax(), "Zero voltage source count");
        assertEquals(0, netlist.getElementCount(), "Zero element count");
    }

    @Test
    public void testBuildEmptyWithNegativeNodeCountThrowsException() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> NetlistBuilder.buildEmpty(-1, 0, 0),
            "Negative nodeCount should throw IllegalArgumentException"
        );
    }

    @Test
    public void testBuildEmptyWithNegativeVoltageSourceCountThrowsException() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> NetlistBuilder.buildEmpty(1, -1, 0),
            "Negative voltageSourceCount should throw IllegalArgumentException"
        );
    }

    @Test
    public void testBuildEmptyWithNegativeElementCountThrowsException() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> NetlistBuilder.buildEmpty(1, 0, -1),
            "Negative elementCount should throw IllegalArgumentException"
        );
    }

    @Test
    public void testBuildEmptyCreatesValidNetlist() {
        // Arrange
        int nodeCount = 3;
        int voltageSourceCount = 1;
        int elementCount = 4;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(nodeCount, voltageSourceCount, elementCount);

        // Assert - Verify netlist is valid
        assertTrue(netlist.isValid(), "Built netlist should be valid");
        assertTrue(netlist.toString().length() > 0, "Netlist toString should not be null");
    }

    @Test
    public void testBuildFromCircuitModelWithNullReturnsEmptyNetlist() {
        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(null);

        // Assert
        assertNotNull(netlist, "Netlist should not be null for null model");
        assertEquals(0, netlist.getNodeMax(), "Should have zero nodes");
        assertEquals(0, netlist.getVoltageSourceMax(), "Should have zero voltage sources");
        assertEquals(0, netlist.getElementCount(), "Should have zero elements");
    }

    @Test
    public void testBuildFromCircuitModelEstimatesDimensions() {
        // Arrange
        CircuitModel model = new CircuitModel();
        for (int i = 0; i < 10; i++) {
            model.addCircuitComponent(new CircuitModel.ComponentData(1, "R" + i));
        }

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(10, netlist.getElementCount(), "Element count should match component count");
        // Estimated nodes: (10 / 2) + 1 = 6, so nodeMax = 5
        assertEquals(5, netlist.getNodeMax(), "Nodes should be estimated");
        // Estimated voltage sources: 10 / 5 = 2
        assertEquals(2, netlist.getVoltageSourceMax(), "Voltage sources should be estimated");
    }

    @Test
    public void testBuildFromCircuitModelWithEmptyModel() {
        // Arrange
        CircuitModel model = new CircuitModel();
        // Model has no components

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(0, netlist.getElementCount(), "Element count should be zero");
    }

    @Test
    public void testBuildFromCircuitModelMixedComponents() {
        // Arrange
        CircuitModel model = new CircuitModel();
        model.addCircuitComponent(new CircuitModel.ComponentData(1, "R1"));
        model.addCircuitComponent(new CircuitModel.ComponentData(2, "L1"));
        model.addThermalComponent(new CircuitModel.ComponentData(4, "Th1"));
        // Control blocks are excluded from the electrical netlist

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(3, netlist.getElementCount(), "Element count should be 3");
    }

    @Test
    public void controlComponentsAreExcludedFromElectricalNetlist() {
        // Control file types share the number space with LK types (typ 1 = VOLT
        // probe vs. LK_R); they must not become electrical elements.
        CircuitModel model = new CircuitModel();
        CircuitModel.ComponentData r = new CircuitModel.ComponentData(1, "R1", 10, 10, 503);
        r.setParameter("param0", 100.0);
        model.addCircuitComponent(r);
        model.addControlComponent(new CircuitModel.ComponentData(1, "VOLT.1", 30, 10, 503));
        model.addControlComponent(new CircuitModel.ComponentData(5, "SCOPE.1", 34, 10, 503));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        assertEquals(1, netlist.getElementCount(), "only the resistor becomes an element");
        assertEquals(gecko.core.circuit.circuitcomponents.CircuitTypCore.LK_R, netlist.getType(0));
    }

    @Test
    public void testBuildEmptyNetlistAllComponentsAreResistors() {
        // Arrange
        int elementCount = 3;

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildEmpty(2, 0, elementCount);

        // Assert
        for (int i = 0; i < elementCount; i++) {
            assertNotNull(netlist.getType(i), "Component type should not be null");
            // All components should be resistors
            assertEquals(
                gecko.core.circuit.circuitcomponents.CircuitTypCore.LK_R,
                netlist.getType(i),
                "All components should be resistors"
            );
        }
    }

    // ========== Wire-tracing topology (editor circuits without labels) ==========

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
    public void testWiresConnectComponentTerminalsByCoincidentGridPoints() {
        CircuitModel model = new CircuitModel();
        // V source at (10,10) WEST_EAST: terminals (8,10) and (12,10)
        model.addCircuitComponent(component(4, "V1", 10, 10, 502));
        // resistor at (20,10) WEST_EAST: terminals (18,10) and (22,10)
        model.addCircuitComponent(component(1, "R1", 20, 10, 502));
        // dense wire linking only source output to resistor input
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                dense("12 10 13 10 14 10 15 10 16 10 17 10 18 10")));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        assertEquals(2, netlist.getElementCount());
        assertEquals(1, netlist.getVoltageSourceMax(), "voltage source gets a source number");
        // source output node == resistor input node (connected through the wire);
        // without explicit ground the source's negative terminal becomes node 0
        assertEquals(0, netlist.getNodeY(0), "default ground is the first component's output");
        assertEquals(netlist.getNodeY(0), netlist.getNodeX(1), "wire joins source and resistor");
        assertNotEquals(netlist.getNodeX(0), netlist.getNodeY(1), "free terminals stay separate");
    }

    @Test
    public void testTerminalOnMidWirePointConnectsLikeTheClassicEditor() {
        CircuitModel model = new CircuitModel();
        // dense horizontal wire from (8,10) to (12,10) passes through (10,10)
        model.getConnections().add(new CircuitModel.ConnectionData("LK", dense("8 10 9 10 10 10 11 10 12 10")));
        // resistor above, SOUTH_NORTH orientation: terminals (10,12) and (10,10)
        model.addCircuitComponent(component(1, "R1", 10, 12, 501));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // R1's output (10,10) is a mid-wire raster point: it shares the wire's node,
        // which (absent any ground) becomes the default ground node 0. The free
        // input terminal (10,14) must be a separate node.
        assertEquals(0, netlist.getNodeY(0), "tapped terminal joins the wire node");
        assertNotEquals(0, netlist.getNodeX(0), "free terminal is a separate node");
    }

    @Test
    public void testWireLabelGndDefinesGroundNode() {
        CircuitModel model = new CircuitModel();
        model.addCircuitComponent(component(1, "R1", 10, 10, 502));
        model.addCircuitComponent(component(1, "R2", 20, 10, 502));
        CircuitModel.ConnectionData groundWire =
                new CircuitModel.ConnectionData("LK", dense("8 10 9 10 10 10 11 10 12 10"));
        groundWire.setLabel("GND");
        model.getConnections().add(groundWire);
        // wire touching only R2's input terminal; R2's output stays floating
        model.getConnections().add(new CircuitModel.ConnectionData("LK", dense("18 10 19 10")));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        assertEquals(0, netlist.getNodeX(0), "terminal on the GND-labeled wire is node 0");
        assertEquals(0, netlist.getNodeY(0), "both terminals on the ground wire are node 0");
        assertNotEquals(0, netlist.getNodeX(1), "separate wire is a separate node");
        assertNotEquals(0, netlist.getNodeY(1), "floating terminal is not ground");
    }

    @Test
    public void testFullyLabeledCircuitStillUsesLabelMatching() {
        CircuitModel model = new CircuitModel();
        CircuitModel.ComponentData v1 = component(4, "V1", 10, 10, 502);
        v1.setTerminalXLabels(new String[]{"plus"});
        v1.setTerminalYLabels(new String[]{"minus"});
        CircuitModel.ComponentData r1 = component(1, "R1", 20, 10, 502);
        r1.setTerminalXLabels(new String[]{"plus"});
        r1.setTerminalYLabels(new String[]{"minus"});
        model.addCircuitComponent(v1);
        model.addCircuitComponent(r1);

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        assertEquals(netlist.getNodeX(0), netlist.getNodeX(1), "equal labels connect");
        assertEquals(netlist.getNodeY(0), netlist.getNodeY(1));
        assertEquals(1, netlist.getVoltageSourceMax());
    }

    @Test
    public void testOpticallyDisconnectedComponentWithStaleLabelsTreatsAsOpenCircuit() {
        CircuitModel model = new CircuitModel();
        // V1 at (10,10) WEST_EAST: terminals at (8,10) and (12,10)
        CircuitModel.ComponentData v1 = component(4, "V1", 10, 10, 502);
        v1.setTerminalXLabels(new String[]{"net_a"});
        v1.setTerminalYLabels(new String[]{"0"});
        model.addCircuitComponent(v1);

        // R1 at (30,30) WEST_EAST: terminals at (28,30) and (32,30)
        // R1 still has stale export labels matching V1
        CircuitModel.ComponentData r1 = component(1, "R1", 30, 30, 502);
        r1.setTerminalXLabels(new String[]{"net_a"});
        r1.setTerminalYLabels(new String[]{"0"});
        model.addCircuitComponent(r1);

        // Schematic has LK wire connected only to V1
        model.getConnections().add(new CircuitModel.ConnectionData("LK", dense("8 10 9 10 10 10 11 10 12 10")));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // R1 is floating in space: its terminals have optically NO connection
        assertNotEquals(netlist.getNodeX(0), netlist.getNodeX(1),
                "Optically disconnected R1 must not connect to V1 via stale labels");
        assertNotEquals(netlist.getNodeY(0), netlist.getNodeY(1),
                "Optically disconnected terminal must not connect to ground via stale labels");
    }

    @Test
    public void testCoincidentTerminalsWithoutWireConnectDirectly() {
        CircuitModel model = new CircuitModel();
        // R1 at (10,10) WEST_EAST: output at (12,10)
        model.addCircuitComponent(component(1, "R1", 10, 10, 502));
        // R2 at (14,10) WEST_EAST: input at (12,10) - touching R1's output directly
        model.addCircuitComponent(component(1, "R2", 14, 10, 502));
        // An LK wire touching R1's input at (8,10)
        model.getConnections().add(new CircuitModel.ConnectionData("LK", dense("7 10 8 10")));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        assertEquals(netlist.getNodeY(0), netlist.getNodeX(1),
                "Coincident pin-to-pin terminals must connect directly without wire");
    }

    @Test
    public void testRotatingComponentAwayBreaksOpticalConnection() {
        CircuitModel model = new CircuitModel();
        // V1 at (10,10) WEST_EAST: output at (12,10)
        CircuitModel.ComponentData v1 = component(4, "V1", 10, 10, 502);
        v1.setTerminalXLabels(new String[]{"0"});
        v1.setTerminalYLabels(new String[]{"net_out"});
        model.addCircuitComponent(v1);

        // Wire from V1 output (12,10) to (18,10)
        model.getConnections().add(new CircuitModel.ConnectionData("LK", dense("12 10 13 10 14 10 15 10 16 10 17 10 18 10")));

        // R1 rotated to NORTH_SOUTH (503) at (20,10): terminals at (20,8) and (20,12)
        // Wire ends at (18,10), so R1's terminals no longer touch the wire
        CircuitModel.ComponentData r1 = component(1, "R1", 20, 10, 503);
        r1.setTerminalXLabels(new String[]{"net_out"});
        r1.setTerminalYLabels(new String[]{"0"});
        model.addCircuitComponent(r1);

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        assertNotEquals(netlist.getNodeY(0), netlist.getNodeX(1),
                "Rotated component whose terminals do not touch wire must be treated as open/interrupted");
    }

    @Test
    public void testInsertingComponentBetweenCutWiresDoesNotShortCircuit() {
        CircuitModel model = new CircuitModel();
        // V1 at (10,10) WEST_EAST: terminal X at (8,10), terminal Y at (12,10)
        CircuitModel.ComponentData v1 = component(4, "V1", 10, 10, 502);
        v1.setTerminalXLabels(new String[]{"ac1"});
        v1.setTerminalYLabels(new String[]{"0"});
        model.addCircuitComponent(v1);

        // D1 at (30,10) WEST_EAST: terminal X at (28,10), terminal Y at (32,10)
        // D1 has residual label "ac1" on X from when it was previously wired to V1
        CircuitModel.ComponentData d1 = component(6, "D1", 30, 10, 502);
        d1.setTerminalXLabels(new String[]{"ac1"});
        d1.setTerminalYLabels(new String[]{"out"});
        model.addCircuitComponent(d1);

        // Wire 1 connects V1.X (8,10) to R1.X (18,10)
        // Wire 2 connects R1.Y (22,10) to D1.X (28,10)
        // R1 is placed at (20,10) WEST_EAST: terminal X at (18,10), terminal Y at (22,10)
        // R1 is newly created, so it has no terminal labels
        CircuitModel.ComponentData r1 = component(1, "R1", 20, 10, 502);
        model.addCircuitComponent(r1);

        model.getConnections().add(new CircuitModel.ConnectionData("LK", dense("8 10 18 10")));
        model.getConnections().add(new CircuitModel.ConnectionData("LK", dense("22 10 28 10")));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // R1 is element index 2:
        // nodeX (terminal at 18,10) must be the node of Wire 1 (connected to V1.X)
        // nodeY (terminal at 22,10) must be the node of Wire 2 (connected to D1.X)
        // R1 must NOT be short-circuited!
        assertNotEquals(netlist.getNodeX(2), netlist.getNodeY(2),
                "R1 terminals must be on distinct nodes; R1 must not be shorted by residual labels");
        assertEquals(netlist.getNodeX(0), netlist.getNodeX(2), "V1.X and R1.X must be on Wire 1 node");
        assertEquals(netlist.getNodeX(1), netlist.getNodeY(2), "D1.X and R1.Y must be on Wire 2 node");
    }

    @Test
    public void testBranchShortCircuitDetectorEmitsDRCWarning() {
        CircuitModel model = new CircuitModel();
        // R1 placed at (10, 10) NORTH_SOUTH: term X at (10, 8), term Y at (10, 12)
        CircuitModel.ComponentData r1 = component(1, "R1", 10, 10, 503);
        model.addCircuitComponent(r1);

        // A wire that loops from term X (10, 8) to term Y (10, 12), shorting R1 across itself
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{10, 8}, {14, 8}, {14, 12}, {10, 12}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Verify that both terminals are indeed on the same node
        assertEquals(netlist.getNodeX(0), netlist.getNodeY(0),
                "R1 terminals are physically shorted by the loop wire");

        // Verify that NetlistBuilder detected this and emitted the DRC warning
        boolean hasShortWarning = netlist.getBuildWarnings().stream()
                .anyMatch(w -> w.contains("DRC: Branch component 'R1'") && w.contains("short-circuited across itself"));
        assertTrue(hasShortWarning,
                "NetlistBuilder must emit DRC warning for short-circuited branch component, got: "
                        + netlist.getBuildWarnings());
    }

    @Test
    public void testFloatingUnreferencedSubcircuitEmitsDRCWarning() {
        CircuitModel model = new CircuitModel();
        // Isolated R1 and C1 loop with no ground and no voltage source
        // R1 placed at (10, 10) NORTH_SOUTH: term X at (10, 8), term Y at (10, 12)
        CircuitModel.ComponentData r1 = component(1, "R1", 10, 10, 503);
        // C1 placed at (14, 10) NORTH_SOUTH: term X at (14, 8), term Y at (14, 12)
        CircuitModel.ComponentData c1 = component(2, "C1", 14, 10, 503);
        model.addCircuitComponent(r1);
        model.addCircuitComponent(c1);

        // Wire 1: connects top pins (10, 8) to (14, 8)
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{10, 8}, {14, 8}}));
        // Wire 2: connects bottom pins (10, 12) to (14, 12)
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{10, 12}, {14, 12}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Verify DRC_FLOATING_GROUND is emitted
        boolean hasFloatingGroundWarning = netlist.getBuildWarnings().stream()
                .anyMatch(w -> w.contains("DRC_FLOATING_GROUND") && w.contains("no connection to ground"));
        assertTrue(hasFloatingGroundWarning,
                "NetlistBuilder must emit DRC_FLOATING_GROUND warning for isolated loop with no ground, got: "
                        + netlist.getBuildWarnings());
    }

    @Test
    public void testIsolatedSecondarySubcircuitWithoutGroundEmitsDRCWarning() {
        CircuitModel model = new CircuitModel();
        // Subcircuit 1: V1 + R1 with explicit GND
        CircuitModel.ComponentData v1 = component(4, "V1", 10, 10, 502);
        CircuitModel.ComponentData r1 = component(1, "R1", 20, 10, 502);
        model.addCircuitComponent(v1);
        model.addCircuitComponent(r1);

        CircuitModel.ConnectionData gndWire = new CircuitModel.ConnectionData("LK",
                new int[][]{{8, 10}, {18, 10}});
        gndWire.setLabel("GND");
        model.getConnections().add(gndWire);
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{12, 10}, {22, 10}}));

        // Subcircuit 2: Isolated R2 + C1 loop in another region, completely disconnected
        CircuitModel.ComponentData r2 = component(1, "R2", 40, 10, 503);
        CircuitModel.ComponentData c1 = component(2, "C1", 44, 10, 503);
        model.addCircuitComponent(r2);
        model.addCircuitComponent(c1);

        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{40, 8}, {44, 8}}));
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{40, 12}, {44, 12}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Subcircuit 1 is properly grounded and has V1, but Subcircuit 2 is floating
        boolean hasFloatingWarning = netlist.getBuildWarnings().stream()
                .anyMatch(w -> w.contains("DRC_FLOATING_GROUND") && w.contains("R2"));
        assertTrue(hasFloatingWarning,
                "NetlistBuilder must emit DRC_FLOATING_GROUND for isolated subcircuit R2, got: "
                        + netlist.getBuildWarnings());
    }

    @Test
    public void testUnattachedTerminalWithCollidingLabelEmitsDRCWarning() {
        CircuitModel model = new CircuitModel();
        // V1 at (10, 10), R1 at (20, 10) connected by wire labeled "NET_A"
        CircuitModel.ComponentData v1 = component(4, "V1", 10, 10, 502);
        CircuitModel.ComponentData r1 = component(1, "R1", 20, 10, 502);
        model.addCircuitComponent(v1);
        model.addCircuitComponent(r1);

        CircuitModel.ConnectionData wire = new CircuitModel.ConnectionData("LK",
                new int[][]{{12, 10}, {18, 10}});
        wire.setLabel("NET_A");
        model.getConnections().add(wire);

        CircuitModel.ConnectionData gndWire = new CircuitModel.ConnectionData("LK",
                new int[][]{{8, 10}, {22, 10}});
        gndWire.setLabel("GND");
        model.getConnections().add(gndWire);

        // R2 floating at (50, 50) with stale/colliding label "NET_A" on terminal X
        CircuitModel.ComponentData r2 = component(1, "R2", 50, 50, 503);
        r2.setTerminalXLabels(new String[]{"NET_A"});
        model.addCircuitComponent(r2);

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Verify DRC_LABEL_COLLISION warning is emitted
        boolean hasCollisionWarning = netlist.getBuildWarnings().stream()
                .anyMatch(w -> w.contains("DRC_LABEL_COLLISION") && w.contains("R2") && w.contains("NET_A"));
        assertTrue(hasCollisionWarning,
                "NetlistBuilder must emit DRC_LABEL_COLLISION for unattached terminal carrying colliding label NET_A, got: "
                        + netlist.getBuildWarnings());
    }
}


