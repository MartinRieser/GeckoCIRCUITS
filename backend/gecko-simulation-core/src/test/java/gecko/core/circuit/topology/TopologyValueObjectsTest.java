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
package gecko.core.circuit.topology;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the strongly typed topology value objects {@link NodeId},
 * {@link ElementId}, and {@link TerminalPair}: equality, hashing, ordering,
 * ground semantics, shorted-pair semantics, and invalid-argument rejection.
 */
class TopologyValueObjectsTest {

    // ========== NodeId ==========

    @Test
    void nodeId_of_wrapsIndex() {
        assertEquals(3, NodeId.of(3).value());
    }

    @Test
    void nodeId_zeroIsValid() {
        assertEquals(0, NodeId.of(0).value());
        assertEquals(0, new NodeId(0).value());
    }

    @Test
    void nodeId_negativeIndexRejected() {
        assertThrows(IllegalArgumentException.class, () -> NodeId.of(-1));
        assertThrows(IllegalArgumentException.class, () -> new NodeId(-7));
    }

    @Test
    void nodeId_groundSingleton_hasIndexZero() {
        assertEquals(0, NodeId.GROUND.value());
        assertEquals(NodeId.of(0), NodeId.GROUND,
                "of(0) must equal the GROUND singleton (value equality)");
    }

    @Test
    void nodeId_isGround_trueOnlyForIndexZero() {
        assertTrue(NodeId.GROUND.isGround());
        assertTrue(NodeId.of(0).isGround());
        assertFalse(NodeId.of(1).isGround());
        assertFalse(NodeId.of(Integer.MAX_VALUE).isGround());
    }

    @Test
    void nodeId_equalityAndHashCode() {
        NodeId a = NodeId.of(5);
        NodeId b = NodeId.of(5);
        NodeId c = NodeId.of(6);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, ElementId.of(5), "distinct value-object types must never be equal");
    }

    @Test
    void nodeId_ordering_byIndex() {
        assertTrue(NodeId.of(1).compareTo(NodeId.of(2)) < 0);
        assertEquals(0, NodeId.of(2).compareTo(NodeId.of(2)));
        assertTrue(NodeId.of(3).compareTo(NodeId.of(2)) > 0);

        List<NodeId> nodes = new ArrayList<>(List.of(NodeId.of(9), NodeId.GROUND, NodeId.of(4)));
        Collections.sort(nodes);
        assertEquals(List.of(NodeId.GROUND, NodeId.of(4), NodeId.of(9)), nodes);
    }

    // ========== ElementId ==========

    @Test
    void elementId_of_wrapsIndex() {
        assertEquals(7, ElementId.of(7).value());
    }

    @Test
    void elementId_zeroIsValid() {
        assertEquals(0, ElementId.of(0).value());
    }

    @Test
    void elementId_negativeIndexRejected() {
        assertThrows(IllegalArgumentException.class, () -> ElementId.of(-1));
        assertThrows(IllegalArgumentException.class, () -> new ElementId(-42));
    }

    @Test
    void elementId_equalityAndHashCode() {
        ElementId a = ElementId.of(2);
        ElementId b = ElementId.of(2);
        ElementId c = ElementId.of(3);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, NodeId.of(2), "distinct value-object types must never be equal");
    }

    @Test
    void elementId_ordering_byIndex() {
        assertTrue(ElementId.of(1).compareTo(ElementId.of(2)) < 0);
        assertEquals(0, ElementId.of(2).compareTo(ElementId.of(2)));
        assertTrue(ElementId.of(3).compareTo(ElementId.of(2)) > 0);
    }

    // ========== TerminalPair ==========

    @Test
    void terminalPair_of_exposesTerminals() {
        TerminalPair pair = TerminalPair.of(NodeId.of(1), NodeId.GROUND);

        assertEquals(NodeId.of(1), pair.positiveNode());
        assertEquals(NodeId.GROUND, pair.negativeNode());
    }

    @Test
    void terminalPair_nullTerminalsRejected() {
        assertThrows(NullPointerException.class, () -> TerminalPair.of(null, NodeId.GROUND));
        assertThrows(NullPointerException.class, () -> TerminalPair.of(NodeId.GROUND, null));
        assertThrows(NullPointerException.class, () -> new TerminalPair(null, null));
    }

    @Test
    void terminalPair_isShorted_onlyWhenTerminalsMatch() {
        assertTrue(TerminalPair.of(NodeId.of(2), NodeId.of(2)).isShorted(),
                "X == Y means the element is shorted");
        assertFalse(TerminalPair.of(NodeId.of(1), NodeId.of(2)).isShorted());
        assertFalse(TerminalPair.of(NodeId.of(1), NodeId.GROUND).isShorted());
    }

    @Test
    void terminalPair_equalityAndHashCode() {
        TerminalPair a = TerminalPair.of(NodeId.of(1), NodeId.of(2));
        TerminalPair b = TerminalPair.of(NodeId.of(1), NodeId.of(2));
        TerminalPair swapped = TerminalPair.of(NodeId.of(2), NodeId.of(1));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, swapped, "terminal order must be significant");
        assertNotEquals(a, null);
    }

    @Test
    void terminalPair_ordering_positiveNodeFirstThenNegative() {
        assertTrue(TerminalPair.of(NodeId.of(1), NodeId.of(2))
                .compareTo(TerminalPair.of(NodeId.of(1), NodeId.of(3))) < 0);
        assertTrue(TerminalPair.of(NodeId.of(1), NodeId.of(5))
                .compareTo(TerminalPair.of(NodeId.of(2), NodeId.of(0))) < 0,
                "positive node dominates the ordering");
        assertEquals(0, TerminalPair.of(NodeId.of(4), NodeId.of(7))
                .compareTo(TerminalPair.of(NodeId.of(4), NodeId.of(7))));
        assertTrue(TerminalPair.of(NodeId.of(3), NodeId.of(0))
                .compareTo(TerminalPair.of(NodeId.of(1), NodeId.of(9))) > 0);
    }
}
