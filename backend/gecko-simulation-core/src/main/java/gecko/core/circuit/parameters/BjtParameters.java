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
package gecko.core.circuit.parameters;

/**
 * Parameter slot layout and defaults for Bipolar Junction Transistor (BJT) models.
 *
 * <p>Applicable component type: {@code LK_BJT}.
 */
public final class BjtParameters {

    /** Forward common-emitter current gain Beta_F, index 1. */
    public static final int INDEX_BETA_F = 1;

    /** Reverse common-emitter current gain Beta_R, index 2. */
    public static final int INDEX_BETA_B = 2;

    /** Intrinsic base spreading resistance R_base [Ohms], index 3. */
    public static final int INDEX_R_BASE = 3;

    /** Polarity selector (positive = NPN, negative = PNP), index 4. */
    public static final int INDEX_POLARITY = 4;

    /** Number of internal two-terminal elements generated in subcircuit expansion (5 elements). */
    public static final int SUBCIRCUIT_ELEMENT_COUNT = 5;

    /** Default forward current gain Beta_F. */
    public static final double DEFAULT_BETA_F = 100.0;

    /** Default reverse current gain Beta_R. */
    public static final double DEFAULT_BETA_B = 60.0;

    /** Default base resistance [Ohms]. */
    public static final double DEFAULT_R_BASE = 0.1;

    /** Junction diode default ON resistance [Ohms]. */
    public static final double JUNCTION_R_ON = 10e-3;

    /** Junction diode forward voltage drop [Volts]. */
    public static final double JUNCTION_U_FORWARD = 0.6;

    /** Junction diode default OFF resistance [Ohms]. */
    public static final double JUNCTION_R_OFF = 1.0e7;

    private BjtParameters() {
        // Constant container - not instantiable
    }
}
