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
package gecko.core.simulation.solver;

/**
 * Global numerical tolerances, damping constants, and thresholds for the MNA simulation engine.
 *
 * <p>Centralizes all magic numbers from the numerical solver loops, state machines,
 * and semiconductor convergence iterations.
 */
public final class SolverConstants {

    /** Minimum non-zero resistance [Ohms] to prevent matrix division by zero. */
    public static final double FAST_NULL_R = 1.0e-9;

    /** Minimum inductance [Henries] below which an inductor is treated as an ideal wire connection. */
    public static final double FAST_NULL_L = 1.0e-12;

    /** Resistance threshold [Ohms] above which a semiconductor switch is considered blocking (OFF). */
    public static final double RD_OFF_THRESHOLD = 1.0e7;

    /** Maximum allowed semiconductor state re-solves per time step before aborting as numerically unstable. */
    public static final int MAX_SEMICONDUCTOR_ITERATIONS = 10_000;

    /** First iteration count threshold after which semiconductor oscillation damping begins. */
    public static final int OSCILLATION_DAMPING_ITERATION_START = 2;

    /** Damping factor multiplied to disturbance size during semiconductor switching oscillations. */
    public static final double OSCILLATION_DAMPING_FACTOR = 0.99;

    /** Threshold of state flips after which small acceptance threshold relaxation (0.1 V) is applied. */
    public static final int RELAXATION_THRESHOLD_TIER_1 = 300;

    /** Threshold of state flips after which larger acceptance threshold relaxation (0.2 V) is applied. */
    public static final int RELAXATION_THRESHOLD_TIER_2 = 600;

    /** Acceptance threshold voltage relaxation for Tier 1 [Volts]. */
    public static final double RELAXATION_VOLTAGE_TIER_1 = 0.1;

    /** Acceptance threshold voltage relaxation for Tier 2 [Volts]. */
    public static final double RELAXATION_VOLTAGE_TIER_2 = 0.2;

    /** Gear-Shichman 2nd order companion history coefficient (4/3). */
    public static final double GEAR_SHICHMAN_COEFF_4_3 = 4.0 / 3.0;

    /** Gear-Shichman 2nd order companion history coefficient (1/3). */
    public static final double GEAR_SHICHMAN_COEFF_1_3 = 1.0 / 3.0;

    /** Gear-Shichman leading coefficient (2/3). */
    public static final double GEAR_SHICHMAN_COEFF_2_3 = 2.0 / 3.0;

    /** Gear-Shichman main integration factor (2/3). */
    public static final double GEAR_SHICHMAN_MAIN_COEFF = GEAR_SHICHMAN_COEFF_2_3;

    /** Trapezoidal integration companion factor (0.5). */
    public static final double TRAPEZOIDAL_INTEGRATION_FACTOR = 0.5;

    private SolverConstants() {
        // Constant container - not instantiable
    }
}
