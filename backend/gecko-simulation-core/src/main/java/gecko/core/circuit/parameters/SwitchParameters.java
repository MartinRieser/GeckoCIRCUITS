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
 * Parameter slot layout for ideal switches and MOSFET components.
 *
 * <p>Applicable component types: {@code LK_S}, {@code LK_MOSFET}.
 */
public final class SwitchParameters {

    /** Current dynamic resistance [Ohms], index 0. */
    public static final int INDEX_CURRENT_RESISTANCE = 0;

    /** ON-state resistance [Ohms], index 1 for LK_S, index 2 for MOSFET. */
    public static final int INDEX_R_ON = 1;

    /** OFF-state resistance [Ohms], index 2 for LK_S, index 3 for MOSFET. */
    public static final int INDEX_R_OFF = 2;

    /** Branch current through switch [Amperes], index 4. */
    public static final int INDEX_CURRENT = 4;

    /** Branch voltage across switch [Volts], index 5. */
    public static final int INDEX_VOLTAGE = 5;

    /** Gate control signal slot (0.0 = open, 1.0 = closed), index 8. */
    public static final int INDEX_GATE_SIGNAL = 8;

    /** Default switch ON resistance [Ohms]. */
    public static final double DEFAULT_R_ON = 10e-3;

    /** Default switch OFF resistance [Ohms]. */
    public static final double DEFAULT_R_OFF = 1.0e7;

    private SwitchParameters() {
        // Constant container - not instantiable
    }
}
