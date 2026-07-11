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
 * Defines three component enable states: enabled, disabled, or shorted.
 * @author andreas
 */
public enum Enabled {
    /** Component is disabled (not active in simulation). */
    DISABLED,
    /** Component is enabled and active in simulation. */
    ENABLED,
    /** Component is disabled by being shorted. */
    DISABLED_SHORT;
    
    /**
     * Returns the enum constant for the given ordinal value.
     * @param ordinal the ordinal to look up
     * @return the matching Enabled constant
     */
    public static Enabled getFromOrdinal(final int ordinal) {
        for(Enabled val : Enabled.values()) {
            if(val.ordinal() == ordinal) {
                return val;
            }
        }
        assert false;
        return null;
    }

    @Override
    public String toString() {
        switch(this) {
            case DISABLED:
                return "disabled";
            case ENABLED:
                return "enabled";
            case DISABLED_SHORT:
                return "shorted";
            default:
                assert false;
                return "";
        }
            
    }
    
    
    
}
