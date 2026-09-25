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
 * An electrical winding coupled to a magnetic network branch: the winding
 * current drives the magnetomotive force
 *
 * <p>{@code F = N * i}
 *
 * <p>across its magnetic branch (Ampere's law, stamped as a REL_MMF source
 * between the two magnetic nodes), and the branch flux links back into the
 * electrical circuit as the induced EMF
 *
 * <p>{@code v = N * dPhi/dt}
 *
 * <p>(Faraday's law of induction). Both directions are handled by the
 * {@link MagneticNetworkSolver}: the electrical side supplies the winding
 * current per time step, the magnetic side returns the winding flux and the
 * induced EMF.
 *
 * @see MagneticNetworkSolver
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 3: Magnetic Domain Engine)
 */
public final class MagneticWinding {

    /** Unique winding name (also the heat-flow-style handle for readouts). */
    private final String name;

    /** Number of turns. */
    private final int turns;

    /** First magnetic node (MMF rise side for positive current). */
    private final MagneticNode nodeA;

    /** Second magnetic node. */
    private final MagneticNode nodeB;

    /**
     * Creates a winding descriptor.
     *
     * @param name unique winding name
     * @param turns number of turns, at least 1
     * @param nodeA first magnetic node
     * @param nodeB second magnetic node
     *
     * @throws IllegalArgumentException if the name is null or blank, the turn
     *         count is below 1, or a node is null
     */
    public MagneticWinding(final String name, final int turns, final MagneticNode nodeA,
                           final MagneticNode nodeB) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Winding name must not be blank");
        }
        if (turns < 1) {
            throw new IllegalArgumentException("Winding needs at least one turn, got: " + turns);
        }
        if (nodeA == null || nodeB == null) {
            throw new IllegalArgumentException("Winding nodes must not be null");
        }
        this.name = name;
        this.turns = turns;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }

    /**
     * Gets the winding name.
     *
     * @return winding name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the number of turns.
     *
     * @return turn count
     */
    public int getTurns() {
        return turns;
    }

    /**
     * Gets the first magnetic node.
     *
     * @return node A
     */
    public MagneticNode getNodeA() {
        return nodeA;
    }

    /**
     * Gets the second magnetic node.
     *
     * @return node B
     */
    public MagneticNode getNodeB() {
        return nodeB;
    }
}
