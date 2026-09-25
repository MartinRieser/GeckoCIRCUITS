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
 * Immutable linear reluctance of one magnetic circuit branch.
 *
 * <p>The reluctance of a core section of length {@code l}, cross-section
 * {@code A} and relative permeability {@code mu_r} is
 * {@code R_m = l / (mu_0 * mu_r * A)} in 1/H; an air gap of length
 * {@code l_g} uses {@code mu_r = 1}. The magnetic MNA stamps the branch as a
 * conductance with the permeance {@code P_m = 1 / R_m}, so the branch
 * "current" of the solved system is the magnetic flux in webers.
 *
 * @see MagneticNetworkSolver
 * @see NonlinearReluctance
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 3: Magnetic Domain Engine)
 */
public final class ReluctanceBranch {

    /** Vacuum permeability mu_0 in H/m (engineering value 4*pi*1e-7). */
    public static final double VACUUM_PERMEABILITY = 4.0e-7 * Math.PI;

    /** Reluctance of the branch in 1/H. */
    private final double reluctance;

    private ReluctanceBranch(final double reluctance) {
        this.reluctance = reluctance;
    }

    /**
     * Creates a linear reluctance branch from a core section geometry.
     *
     * @param lengthM core section length in meters, positive
     * @param crossSectionM2 core cross-section area in square meters, positive
     * @param relativePermeability relative permeability of the material, greater than 1
     * @return reluctance branch
     *
     * @throws IllegalArgumentException if any argument is not finite, the
     *         length or area is non-positive, or the permeability is not
     *         greater than 1
     */
    public static ReluctanceBranch ofCore(final double lengthM, final double crossSectionM2,
                                          final double relativePermeability) {
        if (!Double.isFinite(lengthM) || lengthM <= 0.0) {
            throw new IllegalArgumentException("Core length must be finite and positive, got: "
                    + lengthM);
        }
        if (!Double.isFinite(crossSectionM2) || crossSectionM2 <= 0.0) {
            throw new IllegalArgumentException("Core cross-section must be finite and positive,"
                    + " got: " + crossSectionM2);
        }
        if (!Double.isFinite(relativePermeability) || relativePermeability <= 1.0) {
            throw new IllegalArgumentException("Relative permeability must be finite and greater"
                    + " than 1, got: " + relativePermeability);
        }
        return new ReluctanceBranch(
                lengthM / (VACUUM_PERMEABILITY * relativePermeability * crossSectionM2));
    }

    /**
     * Creates a linear reluctance branch for an air gap
     * ({@code R_gap = l_g / (mu_0 * A)}).
     *
     * @param lengthM gap length in meters, positive
     * @param crossSectionM2 gap cross-section area in square meters, positive
     * @return reluctance branch
     *
     * @throws IllegalArgumentException if any argument is not finite or the
     *         length or area is non-positive
     */
    public static ReluctanceBranch ofAirGap(final double lengthM, final double crossSectionM2) {
        if (!Double.isFinite(lengthM) || lengthM <= 0.0) {
            throw new IllegalArgumentException("Gap length must be finite and positive, got: "
                    + lengthM);
        }
        if (!Double.isFinite(crossSectionM2) || crossSectionM2 <= 0.0) {
            throw new IllegalArgumentException("Gap cross-section must be finite and positive,"
                    + " got: " + crossSectionM2);
        }
        return new ReluctanceBranch(lengthM / (VACUUM_PERMEABILITY * crossSectionM2));
    }

    /**
     * Creates a linear reluctance branch from an explicit reluctance value,
     * e.g. for return paths through air assembled by hand.
     *
     * @param reluctancePerPerHenry reluctance in 1/H, positive
     * @return reluctance branch
     *
     * @throws IllegalArgumentException if the reluctance is not finite or not positive
     */
    public static ReluctanceBranch ofReluctance(final double reluctancePerPerHenry) {
        if (!Double.isFinite(reluctancePerPerHenry) || reluctancePerPerHenry <= 0.0) {
            throw new IllegalArgumentException("Reluctance must be finite and positive, got: "
                    + reluctancePerPerHenry);
        }
        return new ReluctanceBranch(reluctancePerPerHenry);
    }

    /**
     * Gets the reluctance of the branch.
     *
     * @return reluctance in 1/H
     */
    public double getReluctance() {
        return reluctance;
    }

    /**
     * Gets the permeance of the branch (MNA stamp value).
     *
     * @return permeance in H
     */
    public double getPermeance() {
        return 1.0 / reluctance;
    }
}
