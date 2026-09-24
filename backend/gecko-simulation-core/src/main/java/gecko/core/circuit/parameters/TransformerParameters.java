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
 * Parameter slot layout and defaults for ideal transformer elements.
 *
 * <p>Applicable component type: {@code LK_TRANS}.
 */
public final class TransformerParameters {

    /** Primary winding turns ratio N1, index 0. */
    public static final int INDEX_N1 = 0;

    /** Secondary winding turns ratio N2, index 1. */
    public static final int INDEX_N2 = 1;

    /** Winding polarity (+1.0 or -1.0), index 2. */
    public static final int INDEX_POLARITY = 2;

    /** Default primary turns ratio N1. */
    public static final double DEFAULT_N1 = 10.0;

    /** Default secondary turns ratio N2. */
    public static final double DEFAULT_N2 = 2.0;

    /** Default polarity multiplier (-1.0). */
    public static final double DEFAULT_POLARITY = -1.0;

    private TransformerParameters() {
        // Constant container - not instantiable
    }
}
