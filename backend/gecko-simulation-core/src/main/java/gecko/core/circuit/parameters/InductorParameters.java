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
 * Parameter slot layout for inductors and coupled inductors.
 *
 * <p>Applicable component types: {@code LK_L}, {@code LK_LKOP2}, {@code NONLIN_REL}.
 */
public final class InductorParameters {

    /** Nominal inductance [Henries], index 0. */
    public static final int INDEX_INDUCTANCE = 0;

    /** Initial current setting from component dialog / UI [Amperes], index 1. */
    public static final int INDEX_INITIAL_CURRENT = 1;

    /** Saved current from previous run / checkpoint [Amperes], index 2. */
    public static final int INDEX_SAVED_CURRENT = 2;

    /** Inductance stored in Matrix A formulation (for LKOP2 companion stamp), index 10. */
    public static final int INDEX_EFFECTIVE_L = 10;

    /** Default inductance value [Henries]. */
    public static final double DEFAULT_INDUCTANCE = 1.0e-3;

    private InductorParameters() {
        // Constant container - not instantiable
    }
}
