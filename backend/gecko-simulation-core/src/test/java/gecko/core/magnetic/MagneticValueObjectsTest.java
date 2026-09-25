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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the magnetic domain value objects: node indices, reluctance
 * geometry factories (physics of {@code R = l / (mu_0 * mu_r * A)}) and
 * winding descriptors.
 */
class MagneticValueObjectsTest {

    /** Relative tolerance of the reluctance physics comparisons. */
    private static final double PHYSICS_TOLERANCE = 1e-12;

    @Test
    void magneticNode_validatesIndexAndReference() {
        assertEquals(0, MagneticNode.REFERENCE.value());
        assertTrue(MagneticNode.REFERENCE.isReference());
        assertTrue(MagneticNode.of(3).compareTo(MagneticNode.of(4)) < 0);
        assertEquals("MagneticNode[2]", MagneticNode.of(2).toString());

        assertThrows(IllegalArgumentException.class, () -> MagneticNode.of(-1));
        assertThrows(IllegalArgumentException.class, () -> new MagneticNode(-5));
    }

    @Test
    void reluctanceBranch_coreGeometryFollowsAmpereLaw() {
        final double length = 0.2;
        final double area = 1e-4;
        final double permeability = 2000.0;

        final ReluctanceBranch core = ReluctanceBranch.ofCore(length, area, permeability);
        final double expected = length / (ReluctanceBranch.VACUUM_PERMEABILITY * permeability * area);
        assertEquals(expected, core.getReluctance(), PHYSICS_TOLERANCE * expected);
        assertEquals(1.0 / expected, core.getPermeance(), PHYSICS_TOLERANCE / expected);
    }

    @Test
    void reluctanceBranch_airGapUsesVacuumPermeability() {
        final ReluctanceBranch gap = ReluctanceBranch.ofAirGap(1e-3, 1e-4);
        final double expected = 1e-3 / (ReluctanceBranch.VACUUM_PERMEABILITY * 1e-4);
        assertEquals(expected, gap.getReluctance(), PHYSICS_TOLERANCE * expected);
    }

    @Test
    void reluctanceBranch_rejectsInvalidGeometry() {
        assertThrows(IllegalArgumentException.class,
                () -> ReluctanceBranch.ofCore(0.0, 1e-4, 2000.0));
        assertThrows(IllegalArgumentException.class,
                () -> ReluctanceBranch.ofCore(0.2, -1e-4, 2000.0));
        assertThrows(IllegalArgumentException.class,
                () -> ReluctanceBranch.ofCore(0.2, 1e-4, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> ReluctanceBranch.ofCore(Double.NaN, 1e-4, 2000.0));
        assertThrows(IllegalArgumentException.class,
                () -> ReluctanceBranch.ofAirGap(-1e-3, 1e-4));
        assertThrows(IllegalArgumentException.class,
                () -> ReluctanceBranch.ofAirGap(1e-3, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> ReluctanceBranch.ofReluctance(0.0));
        assertThrows(IllegalArgumentException.class,
                () -> ReluctanceBranch.ofReluctance(Double.NaN));
    }

    @Test
    void magneticWinding_validatesDescriptor() {
        final MagneticWinding winding = new MagneticWinding("w1", 100,
                MagneticNode.of(1), MagneticNode.REFERENCE);
        assertEquals("w1", winding.getName());
        assertEquals(100, winding.getTurns());
        assertEquals(MagneticNode.of(1), winding.getNodeA());
        assertEquals(MagneticNode.REFERENCE, winding.getNodeB());

        assertThrows(IllegalArgumentException.class,
                () -> new MagneticWinding(null, 100, MagneticNode.of(1), MagneticNode.REFERENCE));
        assertThrows(IllegalArgumentException.class,
                () -> new MagneticWinding("  ", 100, MagneticNode.of(1), MagneticNode.REFERENCE));
        assertThrows(IllegalArgumentException.class,
                () -> new MagneticWinding("w", 0, MagneticNode.of(1), MagneticNode.REFERENCE));
        assertThrows(IllegalArgumentException.class,
                () -> new MagneticWinding("w", -3, MagneticNode.of(1), MagneticNode.REFERENCE));
        assertThrows(IllegalArgumentException.class,
                () -> new MagneticWinding("w", 100, null, MagneticNode.REFERENCE));
        assertThrows(IllegalArgumentException.class,
                () -> new MagneticWinding("w", 100, MagneticNode.of(1), null));
    }
}
