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
 * Parameter slot layout and default thresholds for piecewise-linear diode components.
 *
 * <p>Applicable component types: {@code LK_D}, {@code LK_THYR}, {@code LK_IGBT}.
 */
public final class DiodeParameters {

    /** Current dynamic resistance evaluated by the state machine [Ohms], index 0. */
    public static final int INDEX_CURRENT_RESISTANCE = 0;

    /** Forward threshold voltage drop U_f [Volts], index 1. */
    public static final int INDEX_FORWARD_VOLTAGE = 1;

    /** Conducting ON-state resistance r_on [Ohms], index 2. */
    public static final int INDEX_R_ON = 2;

    /** Blocking OFF-state resistance r_off [Ohms], index 3. */
    public static final int INDEX_R_OFF = 3;

    /** Calculated branch current through diode [Amperes], index 4. */
    public static final int INDEX_CURRENT = 4;

    /** Calculated branch voltage across diode [Volts], index 5. */
    public static final int INDEX_VOLTAGE = 5;

    /** Gate control signal (for thyristor/IGBT: 0 = off, 1 = on), index 8. */
    public static final int INDEX_GATE_SIGNAL = 8;

    /** Thyristor turn-off delay time t_q [seconds], index 9. */
    public static final int INDEX_TURN_OFF_DELAY = 9;

    /** Timestamp of last zero-current crossing, index 11. */
    public static final int INDEX_LAST_CROSSING_TIME = 11;

    /** Default ON resistance: 10 milli-Ohms [Ohms]. */
    public static final double DEFAULT_R_ON = 10e-3;

    /** Default forward voltage: 0.7 Volts. */
    public static final double DEFAULT_U_FORWARD = 0.7;

    /** Default OFF resistance: 10 Mega-Ohms [Ohms]. */
    public static final double DEFAULT_R_OFF = 1.0e7;

    private DiodeParameters() {
        // Constant container - not instantiable
    }
}
