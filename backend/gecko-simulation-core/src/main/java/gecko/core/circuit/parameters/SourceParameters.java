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
 * Parameter slot layout for voltage and current sources (DC, AC, and controlled).
 *
 * <p>Applicable component types: {@code LK_U}, {@code LK_I}, {@code TH_TEMP}, {@code TH_FLOW}, {@code REL_MMF}.
 */
public final class SourceParameters {

    /** Source type selector (maps to {@code SourceType}), index 0. */
    public static final int INDEX_SOURCE_TYPE = 0;

    /** Primary DC amplitude or control value [Volts or Amperes], index 1. */
    public static final int INDEX_VALUE_DC = 1;

    /** AC frequency [Hertz], index 2. */
    public static final int INDEX_FREQUENCY = 2;

    /** DC offset [Volts or Amperes], index 3. */
    public static final int INDEX_OFFSET = 3;

    /** AC phase angle [degrees], index 4. */
    public static final int INDEX_PHASE_DEG = 4;

    /** Controlled source gain factor, index 11. */
    public static final int INDEX_GAIN = 11;

    /** Measured reference component index, index 12. */
    public static final int INDEX_MEASURED_ELEMENT = 12;

    /** Sinusoidal source peak amplitude [Volts or Amperes], index 20. */
    public static final int INDEX_AMPLITUDE_SIN = 20;

    /** Saved state current [Amperes or Watts], index 6. */
    public static final int INDEX_SAVED_CURRENT = 6;

    /** Saved positive terminal potential [Volts or K], index 8. */
    public static final int INDEX_SAVED_VX = 8;

    /** Saved negative terminal potential [Volts or K], index 9. */
    public static final int INDEX_SAVED_VY = 9;

    private SourceParameters() {
        // Constant container - not instantiable
    }
}
