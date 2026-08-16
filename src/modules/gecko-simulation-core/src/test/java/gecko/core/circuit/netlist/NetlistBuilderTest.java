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
        model.addControlComponent(new CircuitModel.ComponentData(3, "PI1"));
        model.addThermalComponent(new CircuitModel.ComponentData(4, "Th1"));
        // Total: 4 components

        // Act
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);

        // Assert
        assertNotNull(netlist, "Netlist should not be null");
        assertEquals(4, netlist.getElementCount(), "Element count should be 4");
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

    // ========== Wire-tracing topology (web editor circuits without labels) ==========

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
}
