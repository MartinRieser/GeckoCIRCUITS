package gecko.core.simulation;

import gecko.core.circuit.ComponentTerminals;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.ControlCalculatorBuilder;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strategy 1 (Backend): Parameter & Terminal Contract Invariant Tests.
 * Asserts that:
 * 1. Every CircuitTypCore type produces valid terminal coordinates across all 4 orientations (501..504).
 * 2. Control block terminal counts strictly match ControlCalculatorBuilder layout.
 * 3. Parameter changes to components reliably propagate to CircuitNetlist without being ignored or overwritten.
 */
class ParameterAndTerminalContractTest {

    private static final int[] ALL_ORIENTATIONS = {
            ComponentTerminals.SOUTH_NORTH,
            ComponentTerminals.WEST_EAST,
            ComponentTerminals.NORTH_SOUTH,
            ComponentTerminals.EAST_WEST
    };

    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class)
    void allCircuitTypesProduceConsistentTerminalCoordinatesAcrossOrientations(CircuitTypCore typ) {
        if (typ == CircuitTypCore.C_JAVA_FUNCTION) {
            return;
        }

        String family = typ.isControl() ? "CONTROL" : (typ.isThermal() ? "THERM" : "LK");
        CircuitModel.ComponentData comp = new CircuitModel.ComponentData(
                typ.getTypeNumber(), "TEST_" + typ.name(), 20, 30, ComponentTerminals.NORTH_SOUTH);
        comp.setFamily(family);

        for (int orientation : ALL_ORIENTATIONS) {
            comp.setOrientation(orientation);
            List<int[]> terminals = ComponentTerminals.terminalsOf(comp, comp.getPosition(), orientation);
            assertNotNull(terminals, "terminalsOf must not return null for " + typ);

            if (typ == CircuitTypCore.TEXTBOX || typ == CircuitTypCore.LK_M) {
                // Annotations and SPICE-style K couplers have no wired pins
                assertTrue(terminals.isEmpty());
                continue;
            }

            assertTrue(terminals.size() >= 1,
                    () -> "Component " + typ + " should have at least 1 terminal, got " + terminals.size());

            for (int[] pt : terminals) {
                assertEquals(2, pt.length);
                double dist = Math.hypot(pt[0] - 20, pt[1] - 30);
                assertTrue(dist >= 1.0 && dist <= 20.0,
                        () -> "Terminal point (" + pt[0] + ", " + pt[1] + ") of " + typ + " has invalid distance " + dist);
            }

            // Verify control components match ControlCalculatorBuilder layout
            if (typ.isControl() && typ != CircuitTypCore.CTRL_SCOPE) {
                int[] layout = ControlCalculatorBuilder.getTerminalLayout(comp);
                if (layout != null) {
                    int expectedCount = layout[0] + layout[1];
                    assertEquals(expectedCount, terminals.size(),
                            () -> "Terminal count mismatch for " + typ + " in orientation " + orientation);
                }
            }
        }
    }

    @Test
    void parameterChangesPropagateToNetlistWithoutBeingLostOrZeroed() {
        int[] testedTypes = {1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 24, 26, 28, 44, 45, 46, 47};

        for (int type : testedTypes) {
            CircuitModel model = new CircuitModel();
            CircuitModel.ComponentData comp = new CircuitModel.ComponentData(
                    type, "COMP_" + type, 10, 10, ComponentTerminals.WEST_EAST);
            comp.setFamily(type >= 41 && type <= 48 ? "THERM" : "LK");
            comp.setUniqueObjectIdentifier(1000L + type);

            // Set custom values: param0 = 123.45, param1 = 67.89
            double[] custom = new double[25];
            custom[0] = (type == 4 || type == 5 || type == 26 || type == 44 || type == 45) ? 401.0 : 123.45;
            custom[1] = 67.89;
            comp.setRawParameters(custom);
            comp.setParameter("param0", custom[0]);
            comp.setParameter("param1", custom[1]);

            if (type >= 41 && type <= 48) {
                model.getThermalComponents().add(comp);
            } else {
                model.getCircuitComponents().add(comp);
            }

            // Wire in a loop so it's a branch component
            model.getConnections().add(new CircuitModel.ConnectionData("LK", new int[][]{{8, 10}, {12, 10}}));
            CircuitModel.ConnectionData gnd = new CircuitModel.ConnectionData("LK", new int[][]{{12, 10}, {12, 14}, {8, 14}, {8, 10}});
            gnd.setLabel("GND");
            model.getConnections().add(gnd);

            CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
            assertTrue(netlist.getElementCount() >= 1, "Element must be added to netlist for type " + type);

            double[] netlistParams = netlist.getParameter(0);
            assertNotNull(netlistParams);

            if (type == 1 || type == 24 || type == 46) { // Resistors / Reluctance / Rth
                assertEquals(123.45, netlistParams[0], 1e-6, "Resistance parameter must be preserved");
            } else if (type == 2 || type == 12) { // Inductors
                assertEquals(123.45, netlistParams[0], 1e-6, "Inductance parameter must be preserved");
            } else if (type == 3 || type == 47) { // Capacitors
                assertEquals(123.45, netlistParams[0], 1e-6, "Capacitance parameter must be preserved in slot 0");
                assertEquals(123.45, netlistParams[6], 1e-6, "Capacitance parameter must be preserved in slot 6");
            } else if (type == 4 || type == 5 || type == 26 || type == 44 || type == 45) { // Sources
                assertEquals(401.0, netlistParams[0], 1e-6, "Source type must be preserved");
                assertEquals(67.89, netlistParams[1], 1e-6, "Source DC value must be preserved");
            }
        }
    }
}

