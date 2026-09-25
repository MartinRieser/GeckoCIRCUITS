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

import java.util.Objects;

/**
 * Strongly typed (X, Y) terminal pair of a netlist element.
 *
 * <p>Every two-terminal netlist element connects its positive (X) terminal to
 * {@link #positiveNode} and its negative (Y) terminal to {@link #negativeNode}.
 * Passing the two node indices in the wrong order is a classic netlist bug;
 * naming them in this type makes the assignment sites self-documenting.
 *
 * <p>A pair whose terminals reference the same node is shorted; the element
 * then sees zero voltage regardless of the node potential.
 *
 * <p>Usage:
 * <pre>
 * TerminalPair terminals = netlist.getTerminals(ElementId.of(0));
 * if (terminals.isShorted()) { ... }
 * </pre>
 *
 * @param positiveNode the element's positive (X) terminal node
 * @param negativeNode the element's negative (Y) terminal node
 *
 * @see NodeId
 * @see ElementId
 * @since v2.18.0 Section 5 - Architectural Enhancements
 */
public record TerminalPair(NodeId positiveNode, NodeId negativeNode) implements Comparable<TerminalPair> {

    /**
     * Canonical constructor validating the terminal nodes.
     *
     * @param positiveNode the element's positive (X) terminal node
     * @param negativeNode the element's negative (Y) terminal node
     *
     * @throws NullPointerException if either terminal node is null
     */
    public TerminalPair {
        Objects.requireNonNull(positiveNode, "Positive node must not be null");
        Objects.requireNonNull(negativeNode, "Negative node must not be null");
    }

    /**
     * Creates a terminal pair connecting the given positive and negative nodes.
     *
     * @param positiveNode the element's positive (X) terminal node
     * @param negativeNode the element's negative (Y) terminal node
     * @return terminal pair between the two nodes
     *
     * @throws NullPointerException if either terminal node is null
     */
    public static TerminalPair of(final NodeId positiveNode, final NodeId negativeNode) {
        return new TerminalPair(positiveNode, negativeNode);
    }

    /**
     * Checks whether both terminals connect to the same node.
     *
     * <p>A shorted element sees zero voltage regardless of the shared node
     * potential.
     *
     * @return true if both terminals reference the same node
     */
    public boolean isShorted() {
        return positiveNode.equals(negativeNode);
    }

    @Override
    public int compareTo(final TerminalPair other) {
        final int positiveComparison = positiveNode.compareTo(other.positiveNode);
        return positiveComparison != 0 ? positiveComparison : negativeNode.compareTo(other.negativeNode);
    }
}
