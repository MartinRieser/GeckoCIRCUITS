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
package ch.technokrat.gecko.geckocircuits.circuit;

/**
 * Defines the four possible terminal positions on a circuit component: top,
 * left, right, and bottom.
 * @author andreas
 */
public enum EnumTerminalLocation {
    /** Terminal located at the top of the component. */
    UP,
    /** Terminal located on the left side of the component. */
    LEFT,
    /** Terminal located on the right side of the component. */
    RIGHT,
    /** Terminal located at the bottom of the component. */
    BOTTOM;

    /**
     * Returns the enum constant for the given ordinal value.
     * @param ordinal the ordinal to look up
     * @return the matching EnumTerminalLocation constant
     */
    public static EnumTerminalLocation getFromOrdinal(final int ordinal) {
        for(EnumTerminalLocation val : EnumTerminalLocation.values()) {
            if(val.ordinal() == ordinal) {
                return val;
            }
        }
        assert false;
        return null;
    }
}

    
