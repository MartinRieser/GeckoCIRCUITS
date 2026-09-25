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

/**
 * Geometric and modulation helpers for two-level, three-phase Space Vector Pulse-Width
 * Modulation (SVPWM) sectors.
 *
 * <p>Divides the stationary $(\alpha, \beta)$ space vector plane into six 60-degree
 * sectors (indexed 0 to 5) and computes symmetrical dwell times for the adjacent active
 * voltage vectors and zero vectors.</p>
 *
 * @author GeckoCIRCUITS Team
 * @since v2.18.0 Task L4
 */
public final class SpaceVectorSector {

    /** Angular span of a single space vector sector: 60 degrees (pi / 3 rad). */
    public static final double SECTOR_SPAN = Math.PI / 3.0;

    /** Complete circle angle: 360 degrees (2 * pi rad). */
    public static final double TWO_PI = 2.0 * Math.PI;

    /** Mathematical constant sqrt(3). */
    public static final double SQRT_3 = Math.sqrt(3.0);

    /** Mathematical constant sqrt(3) / 2. */
    public static final double SQRT_3_DIV_2 = SQRT_3 / 2.0;

    /** Number of sectors in the standard two-level hexagonal space vector diagram. */
    public static final int NUM_SECTORS = 6;

    /** Maximum valid sector index (0-based: 0 to 5). */
    public static final int MAX_SECTOR_INDEX = NUM_SECTORS - 1;

    private SpaceVectorSector() {
        // Prevent instantiation of static utility class
    }

    /**
     * Dwell times of active space vectors and symmetrical zero vectors in a half-period.
     *
     * @param sector active sector index (0 to 5)
     * @param deltaVector1 relative dwell time of the first adjacent active vector [0.0, 1.0]
     * @param deltaVector2 relative dwell time of the second adjacent active vector [0.0, 1.0]
     * @param deltaZero relative dwell time of the zero vector [0.0, 0.5]
     */
    public record DwellTimes(
        int sector,
        double deltaVector1,
        double deltaVector2,
        double deltaZero
    ) {}

    /**
     * Symmetrical three-phase switching commands (0.0 or 1.0).
     *
     * @param u phase U gate command
     * @param v phase V gate command
     * @param w phase W gate command
     */
    public record SwitchingStates(double u, double v, double w) {}

    /**
     * Normalizes an angle in radians to the standard interval [0, 2*pi).
     *
     * @param angle input angle in radians
     * @return normalized angle in [0, 2*pi)
     */
    public static double normalizeAngle(final double angle) {
        double normalized = angle % TWO_PI;
        if (normalized < 0.0) {
            normalized += TWO_PI;
        }
        return normalized;
    }

    /**
     * Determines the space vector sector index (0 to 5) from an electrical angle.
     *
     * @param angle electrical angle in radians
     * @return sector index between 0 and 5 inclusive
     */
    public static int determineSector(final double angle) {
        final double normalized = normalizeAngle(angle);
        int sector = (int) (normalized / SECTOR_SPAN);
        if (sector > MAX_SECTOR_INDEX) {
            sector = MAX_SECTOR_INDEX;
        }
        return sector;
    }

    /**
     * Calculates the relative angle within the active sector [0, pi/3).
     *
     * @param angle electrical angle in radians
     * @param sector active sector index (0 to 5)
     * @return relative angle within the sector in [0, pi/3)
     */
    public static double relativeSectorAngle(final double angle, final int sector) {
        final double normalized = normalizeAngle(angle);
        return normalized - sector * SECTOR_SPAN;
    }

    /**
     * Computes the normalized dwell times of active space vectors and zero vectors.
     *
     * @param valpha stationary direct axis reference voltage [V]
     * @param vbeta stationary quadrature axis reference voltage [V]
     * @param vdc DC-link voltage [V] (must be positive and finite)
     * @return calculated dwell times and active sector index
     * @throws IllegalArgumentException if {@code vdc} is not positive or not finite
     */
    public static DwellTimes calculateDwellTimes(
        final double valpha,
        final double vbeta,
        final double vdc
    ) {
        if (vdc <= 0.0 || !Double.isFinite(vdc)) {
            throw new IllegalArgumentException("DC-link voltage must be positive and finite, got: " + vdc);
        }

        double vabs = Math.sqrt(valpha * valpha + vbeta * vbeta);
        if (vabs >= vdc) {
            vabs = vdc; // Overmodulation clamp
        }

        final double modulationIndex = 2.0 * vabs / (SQRT_3 * vdc);
        final double angle = normalizeAngle(Math.atan2(vbeta, valpha));
        final int sector = determineSector(angle);
        final double angleRel = relativeSectorAngle(angle, sector);

        final double deltaVector1 = SQRT_3_DIV_2 * modulationIndex * Math.sin(SECTOR_SPAN - angleRel);
        final double deltaVector2 = SQRT_3_DIV_2 * modulationIndex * Math.sin(angleRel);
        final double deltaZero = 0.5 * (1.0 - deltaVector1 - deltaVector2);

        return new DwellTimes(sector, deltaVector1, deltaVector2, deltaZero);
    }

    /**
     * Generates three-phase complementary switching commands by comparing carrier wave to dwell thresholds.
     *
     * @param dwellTimes calculated sector dwell times
     * @param triangle carrier wave value in range [0.0, 1.0]
     * @return switching commands (u, v, w) each 0.0 or 1.0
     */
    public static SwitchingStates computeSwitchingCommands(
        final DwellTimes dwellTimes,
        final double triangle
    ) {
        final double comp0 = dwellTimes.deltaZero();
        final double comp1a = dwellTimes.deltaZero() + dwellTimes.deltaVector1();
        final double comp1b = dwellTimes.deltaZero() + dwellTimes.deltaVector2();
        final double comp2 = dwellTimes.deltaZero() + dwellTimes.deltaVector1() + dwellTimes.deltaVector2();

        final double pwm0 = (triangle >= comp0) ? 0.0 : 1.0;
        final double pwm1a = (triangle >= comp1a) ? 0.0 : 1.0;
        final double pwm1b = (triangle >= comp1b) ? 0.0 : 1.0;
        final double pwm2 = (triangle >= comp2) ? 0.0 : 1.0;

        return switch (dwellTimes.sector()) {
            case 0 -> new SwitchingStates(pwm0, pwm1a, pwm2);
            case 1 -> new SwitchingStates(pwm1b, pwm0, pwm2);
            case 2 -> new SwitchingStates(pwm2, pwm0, pwm1a);
            case 3 -> new SwitchingStates(pwm2, pwm1b, pwm0);
            case 4 -> new SwitchingStates(pwm1a, pwm2, pwm0);
            case 5 -> new SwitchingStates(pwm0, pwm2, pwm1b);
            default -> new SwitchingStates(0.0, 0.0, 0.0);
        };
    }
}
