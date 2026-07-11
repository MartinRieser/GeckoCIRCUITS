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
package ch.technokrat.gecko.geckocircuits.newscope;

/**
 * Enum selecting between linear and logarithmic axis scaling for scope diagrams.
 */
enum AxisLinLog {

    AXIS_LINEAR(-111111114),
    AXIS_LOGARITHMIC(-111111115);
    private int _code;

    AxisLinLog(final int code) {
        _code = code;
    }

    public int getCode() {
        return _code;
    }

    static AxisLinLog getFromOrdinal(final int ordinal) {
        for (AxisLinLog val : AxisLinLog.values()) {
            if (val.ordinal() == ordinal) {
                return val;
            }
        }
        throw new IllegalArgumentException("Invalid ordinal: " + ordinal);
    }
    
    /**
     * Returns the AxisLinLog matching the given integer code, defaulting to
     * {@link #AXIS_LINEAR} for unrecognized codes.
     *
     * @param code the integer code to look up
     * @return the matching AxisLinLog, or AXIS_LINEAR if not found
     */
    static AxisLinLog getFromCode(final int code) {
        for (AxisLinLog val : AxisLinLog.values()) {
            if (val._code == code) {
                return val;
            }
        }
        return AxisLinLog.AXIS_LINEAR;
    }
};
