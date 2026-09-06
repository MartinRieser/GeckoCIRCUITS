/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit;

import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the shared terminal geometry (grid positions of component
 * terminals per family, type and orientation).
 */
class ComponentTerminalsTest {

    private static CircuitModel.ComponentData comp(String family, int type) {
        CircuitModel.ComponentData comp = new CircuitModel.ComponentData(type, "UT", 0, 0,
                ComponentTerminals.NORTH_SOUTH);
        comp.setFamily(family);
        return comp;
    }

    private static List<int[]> terminals(String family, int type, int x, int y, int orientation) {
        return ComponentTerminals.terminalsOf(comp(family, type), new int[]{x, y}, orientation);
    }

    private static List<int[]> terminals(String family, int type, int x, int y, int orientation,
                                         String... inputLabels) {
        CircuitModel.ComponentData comp = comp(family, type);
        comp.setTerminalXLabels(inputLabels);
        return ComponentTerminals.terminalsOf(comp, new int[]{x, y}, orientation);
    }

    @Test
    void twoPortTerminals_allOrientations() {
        assertArrayEquals(new int[][]{{28, 30}, {32, 30}},
                terminals("LK", 1, 30, 30, ComponentTerminals.WEST_EAST).toArray(),
                "WEST_EAST: input west, output east");
        assertArrayEquals(new int[][]{{32, 30}, {28, 30}},
                terminals("LK", 1, 30, 30, ComponentTerminals.EAST_WEST).toArray(),
                "EAST_WEST: input east, output west");
        assertArrayEquals(new int[][]{{30, 28}, {30, 32}},
                terminals("LK", 1, 30, 30, ComponentTerminals.NORTH_SOUTH).toArray(),
                "NORTH_SOUTH: input north, output south");
        assertArrayEquals(new int[][]{{30, 32}, {30, 28}},
                terminals("LK", 1, 30, 30, ComponentTerminals.SOUTH_NORTH).toArray(),
                "SOUTH_NORTH: input south, output north");
    }

    @Test
    void twoPortGeometry_forThermalAndNullFamily() {
        assertArrayEquals(new int[][]{{8, 14}, {12, 14}},
                terminals("THERM", 46, 10, 14, ComponentTerminals.WEST_EAST).toArray(),
                "thermal blocks use the same two-port geometry");
        assertArrayEquals(new int[][]{{8, 14}, {12, 14}},
                terminals(null, 1, 10, 14, ComponentTerminals.WEST_EAST).toArray(),
                "missing family defaults to two-port");
    }

    @Test
    void controlBlocks_singleTerminalOnActiveSide() {
        // classic truth (TerminalRelativePosition): control blocks orient
        // their terminals horizontally for NORTH_SOUTH, i.e. output east,
        // input west — unlike LK two-ports
        // legacy classic-editor numbers
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_SIGNAL_SOURCE, 10, 14,
                        ComponentTerminals.NORTH_SOUTH).toArray(),
                "legacy signal source: output only, east for NORTH_SOUTH");
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_CONSTANT, 10, 14,
                        ComponentTerminals.NORTH_SOUTH).toArray(),
                "legacy constant: output only");
        assertArrayEquals(new int[][]{{8, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_GATE, 10, 14,
                        ComponentTerminals.NORTH_SOUTH).toArray(),
                "legacy gate: input only, west for NORTH_SOUTH");
        assertArrayEquals(new int[][]{{8, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_SCOPE, 10, 14,
                        ComponentTerminals.NORTH_SOUTH).toArray(),
                "legacy scope: input only");

        // web catalog numbers
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", 1004, 10, 14, ComponentTerminals.NORTH_SOUTH).toArray(),
                "catalog signal source: output only");
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", 1005, 10, 14, ComponentTerminals.NORTH_SOUTH).toArray(),
                "catalog constant: output only");
        assertArrayEquals(new int[][]{{8, 14}},
                terminals("CONTROL", 1003, 10, 14, ComponentTerminals.NORTH_SOUTH).toArray(),
                "catalog scope: input only");
    }

    @Test
    void scopeTerminals_spreadOnePerInputLikeTheWebEditor() {
        // real geometry of the Multi-Scope example: SCOPE.1 at (34,5), 2 inputs,
        // NORTH_SOUTH (503) -> pins west of the body, 2 grid units apart
        assertArrayEquals(new int[][]{{32, 4}, {32, 6}},
                terminals("CONTROL", ComponentTerminals.CONTROL_SCOPE, 34, 5,
                        ComponentTerminals.NORTH_SOUTH, "v_in", "v_out").toArray(),
                "2-input scope: two terminals, vertically centered spread");

        // SCOPE.2 at (34,12) with 3 inputs
        assertArrayEquals(new int[][]{{32, 10}, {32, 12}, {32, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_SCOPE, 34, 12,
                        ComponentTerminals.NORTH_SOUTH, "v_R1", "v_L1", "v_C1").toArray(),
                "3-input scope: three terminals");

        // spread follows the flow direction: EAST_WEST stacks horizontally
        assertArrayEquals(new int[][]{{34, 5}, {32, 5}},
                terminals("CONTROL", ComponentTerminals.CONTROL_SCOPE, 33, 7,
                        ComponentTerminals.EAST_WEST, "a", "b").toArray(),
                "EAST_WEST: input side north, spread along x");
    }

    @Test
    void controlBlocks_allOrientationsFollowClassicRotation() {
        // classic getPointFromDirection: output terminal rel (2,0) maps to
        // NORTH_SOUTH (x+2,y), EAST_WEST (x,y+2), SOUTH_NORTH (x-2,y),
        // WEST_EAST (x,y-2)
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", 1004, 10, 14, ComponentTerminals.NORTH_SOUTH).toArray());
        assertArrayEquals(new int[][]{{10, 16}},
                terminals("CONTROL", 1004, 10, 14, ComponentTerminals.EAST_WEST).toArray());
        assertArrayEquals(new int[][]{{8, 14}},
                terminals("CONTROL", 1004, 10, 14, ComponentTerminals.SOUTH_NORTH).toArray());
        assertArrayEquals(new int[][]{{10, 12}},
                terminals("CONTROL", 1004, 10, 14, ComponentTerminals.WEST_EAST).toArray());
    }

    @Test
    void controlBlocks_withTwoPorts_fallBackToTwoPortGeometry() {
        assertArrayEquals(new int[][]{{8, 14}, {12, 14}},
                terminals("CONTROL", 1006, 10, 14, ComponentTerminals.NORTH_SOUTH).toArray(),
                "gain and other two-terminal control blocks keep input and output");
    }

    @Test
    void flowVector_allCodesPlusDefault() {
        assertArrayEquals(new int[]{1, 0}, ComponentTerminals.flowVector(ComponentTerminals.WEST_EAST));
        assertArrayEquals(new int[]{-1, 0}, ComponentTerminals.flowVector(ComponentTerminals.EAST_WEST));
        assertArrayEquals(new int[]{0, 1}, ComponentTerminals.flowVector(ComponentTerminals.NORTH_SOUTH));
        assertArrayEquals(new int[]{0, -1}, ComponentTerminals.flowVector(ComponentTerminals.SOUTH_NORTH));
        assertArrayEquals(new int[]{0, 1}, ComponentTerminals.flowVector(0),
                "unknown orientation falls back to NORTH_SOUTH like the classic editor");
    }

    @Test
    void controlFlowVector_rotatedQuarterTurnAgainstLk() {
        // control blocks draw horizontally for NORTH_SOUTH (classic default),
        // i.e. the control flow vector is the LK vector of the previous
        // orientation in the rotation cycle
        assertArrayEquals(new int[]{1, 0}, ComponentTerminals.controlFlowVector(ComponentTerminals.NORTH_SOUTH));
        assertArrayEquals(new int[]{0, 1}, ComponentTerminals.controlFlowVector(ComponentTerminals.EAST_WEST));
        assertArrayEquals(new int[]{-1, 0}, ComponentTerminals.controlFlowVector(ComponentTerminals.SOUTH_NORTH));
        assertArrayEquals(new int[]{0, -1}, ComponentTerminals.controlFlowVector(ComponentTerminals.WEST_EAST));
        assertArrayEquals(new int[]{1, 0}, ComponentTerminals.controlFlowVector(0),
                "unknown orientation falls back to NORTH_SOUTH flow like the classic editor");
    }

    @Test
    void constants_matchSerializedCodes() {
        assertEquals(501, ComponentTerminals.SOUTH_NORTH);
        assertEquals(502, ComponentTerminals.WEST_EAST);
        assertEquals(503, ComponentTerminals.NORTH_SOUTH);
        assertEquals(504, ComponentTerminals.EAST_WEST);
    }
}
