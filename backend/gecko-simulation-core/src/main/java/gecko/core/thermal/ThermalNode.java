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
package gecko.core.thermal;

/**
 * Strongly typed identifier of a thermal network node, wrapping the non-negative
 * node index used by the {@link ThermalNetworkSolver}.
 *
 * <p>Thermal nodes and heat source names are both plain identifiers, so mixing
 * them up in solver calls compiles without complaint. Wrapping the index in this
 * record makes such mix-ups a compile-time error while the solver keeps operating
 * on raw {@code int} node indices in its MNA loops.
 *
 * <p>Index {@link #AMBIENT_VALUE} is the ambient reference of the thermal network;
 * {@link #AMBIENT} is the singleton constant for it and {@link #isAmbient()} tests
 * against it. The solver maps thermal node indices onto its internal MNA node
 * numbering (which reserves one extra node as the return terminal of the ambient
 * temperature source), so a {@code ThermalNode} index must not be used as a raw
 * MNA matrix row index.
 *
 * <p>Usage:
 * <pre>
 * ThermalNode junction = ThermalNode.of(1);
 * if (node.isAmbient()) { ... }
 * </pre>
 *
 * @param value non-negative thermal node index (0 = ambient reference)
 *
 * @see ThermalNetworkSolver
 * @see ThermalRCModel
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 1: Thermal Domain Engine)
 */
public record ThermalNode(int value) implements Comparable<ThermalNode> {

    /** Thermal node index of the ambient reference node. */
    public static final int AMBIENT_VALUE = 0;

    /** Ambient reference node (index 0) of a thermal network. */
    public static final ThermalNode AMBIENT = new ThermalNode(AMBIENT_VALUE);

    /**
     * Canonical constructor validating the thermal node index.
     *
     * @param value non-negative thermal node index (0 = ambient reference)
     *
     * @throws IllegalArgumentException if the node index is negative
     */
    public ThermalNode {
        if (value < 0) {
            throw new IllegalArgumentException("Thermal node index cannot be negative: " + value);
        }
    }

    /**
     * Creates a thermal node identifier for the given thermal node index.
     *
     * @param value non-negative thermal node index (0 = ambient reference)
     * @return thermal node identifier
     *
     * @throws IllegalArgumentException if the node index is negative
     */
    public static ThermalNode of(final int value) {
        return new ThermalNode(value);
    }

    /**
     * Checks whether this node is the ambient reference of the thermal network.
     *
     * @return true if this node is the ambient reference (index 0)
     */
    public boolean isAmbient() {
        return value == AMBIENT_VALUE;
    }

    @Override
    public int compareTo(final ThermalNode other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return "ThermalNode[" + value + (isAmbient() ? " (ambient)" : "") + "]";
    }
}
