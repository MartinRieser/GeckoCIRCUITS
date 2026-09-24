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
 * Parameter slot layout and defaults for linear resistor and thermal resistance elements.
 *
 * <p>Applicable component types: {@code LK_R}, {@code TH_RTH}, {@code REL_RELUCTANCE}, {@code TH_AMBIENT}.
 */
public final class ResistorParameters {

    /** Resistance value [Ohm or K/W or A-t/Wb], index 0. */
    public static final int INDEX_RESISTANCE = 0;

    /** Calculated branch current [A or W or Wb], index 1. */
    public static final int INDEX_CURRENT = 1;

    /** Minimum default resistance to prevent numerical singularity [Ohm]. */
    public static final double DEFAULT_RESISTANCE = 1.0;

    private ResistorParameters() {
        // Constant container - not instantiable
    }
}
