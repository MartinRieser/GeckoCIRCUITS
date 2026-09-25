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

/**
 * Strongly typed identifier of a circuit node, wrapping the non-negative node
 * index used by the MNA solver.
 *
 * <p>Node indices and element indices are both plain {@code int} values in the
 * netlist arrays, so passing one where the other is expected compiles without
 * complaint. Wrapping the index in this type makes such mix-ups a compile-time
 * error while the hot solver loops keep operating on the raw primitives.
 *
 * <p>Node 0 is the ground reference of the MNA system; {@link #GROUND} is the
 * singleton constant for it and {@link #isGround()} tests against it.
 *
 * <p>Usage:
 * <pre>
 * NodeId node = NodeId.of(3);
 * if (node.isGround()) { ... }
 * </pre>
 *
 * @param value non-negative node index (0 = ground reference)
 *
 * @see ElementId
 * @see TerminalPair
 * @since v2.18.0 Section 5 - Architectural Enhancements
 */
public record NodeId(int value) implements Comparable<NodeId> {

    /** Ground reference node (index 0) of the MNA system. */
    public static final NodeId GROUND = new NodeId(0);

    /**
     * Canonical constructor validating the node index.
     *
     * @param value non-negative node index (0 = ground reference)
     *
     * @throws IllegalArgumentException if the node index is negative
     */
    public NodeId {
        if (value < 0) {
            throw new IllegalArgumentException("Node index cannot be negative: " + value);
        }
    }

    /**
     * Creates a node identifier for the given node index.
     *
     * @param value non-negative node index (0 = ground reference)
     * @return node identifier wrapping the index
     *
     * @throws IllegalArgumentException if the node index is negative
     */
    public static NodeId of(int value) {
        return new NodeId(value);
    }

    /**
     * Checks whether this node is the ground reference (index 0).
     *
     * @return true if this node is the ground reference
     */
    public boolean isGround() {
        return value == 0;
    }

    @Override
    public int compareTo(final NodeId other) {
        return Integer.compare(value, other.value);
    }
}
