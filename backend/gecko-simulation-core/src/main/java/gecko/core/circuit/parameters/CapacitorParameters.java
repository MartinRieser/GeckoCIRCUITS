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
 * Parameter slot layout for capacitor and thermal capacitance elements.
 *
 * <p>Applicable component types: {@code LK_C}, {@code TH_CTH}.
 */
public final class CapacitorParameters {

    /** Nominal capacitance [Farads or J/K], index 0. */
    public static final int INDEX_CAPACITANCE = 0;

    /** Initial voltage setting from component dialog / UI [Volts or K], index 1. */
    public static final int INDEX_INITIAL_VOLTAGE = 1;

    /** Initial or saved current [Amperes or Watts], index 2. */
    public static final int INDEX_SAVED_CURRENT = 2;

    /** Saved terminal X potential [Volts or K], index 4. */
    public static final int INDEX_SAVED_VX = 4;

    /** Saved terminal Y potential [Volts or K], index 5. */
    public static final int INDEX_SAVED_VY = 5;

    /** Effective dynamic capacitance used by matrix solver [Farads], index 6. */
    public static final int INDEX_EFFECTIVE_C = 6;

    /** Non-linear capacitance factor, index 7. */
    public static final int INDEX_NONLINEAR_FACTOR = 7;

    /** Non-linear charge companion current compensation, index 10. */
    public static final int INDEX_COMPANION_CURRENT = 10;

    /** Default capacitance value [Farads]. */
    public static final double DEFAULT_CAPACITANCE = 1.0e-6;

    private CapacitorParameters() {
        // Constant container - not instantiable
    }
}
