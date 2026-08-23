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
        // legacy classic-editor numbers
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_SIGNAL_SOURCE, 10, 14,
                        ComponentTerminals.WEST_EAST).toArray(),
                "legacy signal source: output only");
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_CONSTANT, 10, 14,
                        ComponentTerminals.WEST_EAST).toArray(),
                "legacy constant: output only");
        assertArrayEquals(new int[][]{{8, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_GATE, 10, 14,
                        ComponentTerminals.WEST_EAST).toArray(),
                "legacy gate: input only");
        assertArrayEquals(new int[][]{{8, 14}},
                terminals("CONTROL", ComponentTerminals.CONTROL_SCOPE, 10, 14,
                        ComponentTerminals.WEST_EAST).toArray(),
                "legacy scope: input only");

        // web catalog numbers
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", 1004, 10, 14, ComponentTerminals.WEST_EAST).toArray(),
                "catalog signal source: output only");
        assertArrayEquals(new int[][]{{12, 14}},
                terminals("CONTROL", 1005, 10, 14, ComponentTerminals.WEST_EAST).toArray(),
                "catalog constant: output only");
        assertArrayEquals(new int[][]{{8, 14}},
                terminals("CONTROL", 1003, 10, 14, ComponentTerminals.WEST_EAST).toArray(),
                "catalog scope: input only");
    }

    @Test
    void controlBlocks_withTwoPorts_fallBackToTwoPortGeometry() {
        assertArrayEquals(new int[][]{{8, 14}, {12, 14}},
                terminals("CONTROL", 1006, 10, 14, ComponentTerminals.WEST_EAST).toArray(),
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
    void constants_matchSerializedCodes() {
        assertEquals(501, ComponentTerminals.SOUTH_NORTH);
        assertEquals(502, ComponentTerminals.WEST_EAST);
        assertEquals(503, ComponentTerminals.NORTH_SOUTH);
        assertEquals(504, ComponentTerminals.EAST_WEST);
    }
}
