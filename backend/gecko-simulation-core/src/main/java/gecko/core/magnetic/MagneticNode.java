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
package gecko.core.magnetic;

/**
 * Strongly typed identifier of a magnetic network node, wrapping the
 * non-negative node index used by the {@link MagneticNetworkSolver}. The node
 * potential is the scalar magnetic potential (MMF) in ampere-turns.
 *
 * <p>Index {@link #REFERENCE_VALUE} is the MMF reference of the magnetic
 * network; {@link #REFERENCE} is the singleton constant for it and
 * {@link #isReference()} tests against it. The solver pins exactly one node
 * per connected magnetic island automatically, so the reference node is a
 * modeling anchor (e.g. the flux-return leg of a core) rather than a solver
 * requirement.
 *
 * <p>Usage:
 * <pre>
 * MagneticNode leg = MagneticNode.of(1);
 * if (node.isReference()) { ... }
 * </pre>
 *
 * @param value non-negative magnetic node index (0 = MMF reference)
 *
 * @see MagneticNetworkSolver
 * @see ReluctanceBranch
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 3: Magnetic Domain Engine)
 */
public record MagneticNode(int value) implements Comparable<MagneticNode> {

    /** Magnetic node index of the MMF reference node. */
    public static final int REFERENCE_VALUE = 0;

    /** MMF reference node (index 0) of a magnetic network. */
    public static final MagneticNode REFERENCE = new MagneticNode(REFERENCE_VALUE);

    /**
     * Canonical constructor validating the magnetic node index.
     *
     * @param value non-negative magnetic node index (0 = MMF reference)
     *
     * @throws IllegalArgumentException if the node index is negative
     */
    public MagneticNode {
        if (value < 0) {
            throw new IllegalArgumentException("Magnetic node index cannot be negative: " + value);
        }
    }

    /**
     * Creates a magnetic node identifier for the given magnetic node index.
     *
     * @param value non-negative magnetic node index (0 = MMF reference)
     * @return magnetic node identifier
     *
     * @throws IllegalArgumentException if the node index is negative
     */
    public static MagneticNode of(final int value) {
        return new MagneticNode(value);
    }

    /**
     * Checks whether this node is the MMF reference of the magnetic network.
     *
     * @return true if this node is the reference (index 0)
     */
    public boolean isReference() {
        return value == REFERENCE_VALUE;
    }

    @Override
    public int compareTo(final MagneticNode other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return "MagneticNode[" + value + (isReference() ? " (reference)" : "") + "]";
    }
}
