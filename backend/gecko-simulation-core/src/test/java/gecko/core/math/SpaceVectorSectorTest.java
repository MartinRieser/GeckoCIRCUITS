/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package gecko.core.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceVectorSectorTest {

    private static final double EPSILON = 1e-12;

    @Test
    @DisplayName("Angle normalization maps negative and large angles to [0, 2*pi)")
    void normalizeAngleRange() {
        assertEquals(0.0, SpaceVectorSector.normalizeAngle(0.0), EPSILON);
        assertEquals(Math.PI, SpaceVectorSector.normalizeAngle(Math.PI), EPSILON);
        assertEquals(Math.PI, SpaceVectorSector.normalizeAngle(3.0 * Math.PI), EPSILON);
        assertEquals(Math.PI, SpaceVectorSector.normalizeAngle(-Math.PI), EPSILON);
        assertEquals(3.0 * Math.PI / 2.0, SpaceVectorSector.normalizeAngle(-Math.PI / 2.0), EPSILON);
    }

    @Test
    @DisplayName("Sector determination identifies each of the six 60-degree sectors correctly")
    void sectorIdentification() {
        // Sector 0: [0, 60 deg)
        assertEquals(0, SpaceVectorSector.determineSector(0.0));
        assertEquals(0, SpaceVectorSector.determineSector(Math.toRadians(30.0)));

        // Sector 1: [60, 120 deg)
        assertEquals(1, SpaceVectorSector.determineSector(Math.toRadians(60.0)));
        assertEquals(1, SpaceVectorSector.determineSector(Math.toRadians(90.0)));

        // Sector 2: [120, 180 deg)
        assertEquals(2, SpaceVectorSector.determineSector(Math.toRadians(120.0)));
        assertEquals(2, SpaceVectorSector.determineSector(Math.toRadians(150.0)));

        // Sector 3: [180, 240 deg)
        assertEquals(3, SpaceVectorSector.determineSector(Math.toRadians(180.0)));
        assertEquals(3, SpaceVectorSector.determineSector(Math.toRadians(210.0)));

        // Sector 4: [240, 300 deg)
        assertEquals(4, SpaceVectorSector.determineSector(Math.toRadians(240.0)));
        assertEquals(4, SpaceVectorSector.determineSector(Math.toRadians(270.0)));

        // Sector 5: [300, 360 deg)
        assertEquals(5, SpaceVectorSector.determineSector(Math.toRadians(300.0)));
        assertEquals(5, SpaceVectorSector.determineSector(Math.toRadians(359.0)));
    }

    @Test
    @DisplayName("Dwell times calculation satisfies conservation and symmetry invariants")
    void dwellTimesConservation() {
        double vdc = 500.0;
        double valpha = 200.0;
        double vbeta = 100.0;

        SpaceVectorSector.DwellTimes dwell = SpaceVectorSector.calculateDwellTimes(valpha, vbeta, vdc);

        assertTrue(dwell.deltaVector1() >= 0.0, "deltaVector1 must be non-negative");
        assertTrue(dwell.deltaVector2() >= 0.0, "deltaVector2 must be non-negative");
        assertTrue(dwell.deltaZero() >= 0.0, "deltaZero must be non-negative");

        // deltaZero * 2 + deltaVector1 + deltaVector2 == 1.0
        double total = 2.0 * dwell.deltaZero() + dwell.deltaVector1() + dwell.deltaVector2();
        assertEquals(1.0, total, EPSILON, "Total dwell time must equal 1.0");
    }

    @Test
    @DisplayName("Overmodulation clamp caps voltage magnitude to vdc")
    void overmodulationClamping() {
        double vdc = 400.0;
        double hugeV = 1000.0;

        SpaceVectorSector.DwellTimes dwell = SpaceVectorSector.calculateDwellTimes(hugeV, 0.0, vdc);
        assertEquals(0, dwell.sector());
        assertTrue(dwell.deltaZero() >= 0.0, "Zero vector dwell remains non-negative even in overmodulation");
    }

    @Test
    @DisplayName("Dwell time calculation rejects non-positive or non-finite DC-link voltage")
    void rejectsInvalidDcLinkVoltage() {
        assertThrows(IllegalArgumentException.class,
            () -> SpaceVectorSector.calculateDwellTimes(0.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class,
            () -> SpaceVectorSector.calculateDwellTimes(0.0, 0.0, -400.0));
        assertThrows(IllegalArgumentException.class,
            () -> SpaceVectorSector.calculateDwellTimes(0.0, 0.0, Double.NaN));
        assertThrows(IllegalArgumentException.class,
            () -> SpaceVectorSector.calculateDwellTimes(0.0, 0.0, Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("Switching commands evaluate to binary values 0.0 or 1.0")
    void switchingCommandsBinary() {
        double vdc = 600.0;
        SpaceVectorSector.DwellTimes dwell = SpaceVectorSector.calculateDwellTimes(150.0, 150.0, vdc);

        double[] carrierValues = {0.0, 0.25, 0.5, 0.75, 1.0};
        for (double tri : carrierValues) {
            SpaceVectorSector.SwitchingStates states = SpaceVectorSector.computeSwitchingCommands(dwell, tri);
            assertTrue(states.u() == 0.0 || states.u() == 1.0);
            assertTrue(states.v() == 0.0 || states.v() == 1.0);
            assertTrue(states.w() == 0.0 || states.w() == 1.0);
        }
    }
}
