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

import ch.technokrat.gecko.geckocircuits.control.Point;

/**
 * Represents the four cardinal orientations of a component on the schematic sheet.
 * Each direction also stores a legacy ordinal code (501&ndash;504) for backward
 * file-format compatibility.
 */
public enum ComponentDirection {

    NORTH_SOUTH(503),
    EAST_WEST(504),
    SOUTH_NORTH(501),
    WEST_EAST(502);
    int _oldOrdinal;

    /**
     * @param oldOrdinal legacy orientation code (501&ndash;504 from the old format)
     */
    ComponentDirection(int oldOrdinal) {
        _oldOrdinal = oldOrdinal;
    }

    /** @return the legacy ordinal code for file serialization. */
    public int code() {
        return _oldOrdinal;
    }

    /**
     * Resolves a direction from its legacy code, defaulting to {@code NORTH_SOUTH}
     * if the code is unrecognized.
     *
     * @param code the legacy ordinal code
     * @return the matching direction
     */
    static ComponentDirection getFromCode(final int code) {
        for (ComponentDirection val : ComponentDirection.values()) {
            if (val._oldOrdinal == code) {
                return val;
            }
        }
        return ComponentDirection.NORTH_SOUTH;
    }

    /** @return the next orientation in the rotation cycle (90&deg; clockwise). */
    ComponentDirection nextOrientation() {
        switch (this) {
            case NORTH_SOUTH:
                return EAST_WEST;
            case EAST_WEST:
                return SOUTH_NORTH;
            case SOUTH_NORTH:
                return WEST_EAST;
            case WEST_EAST:
                return NORTH_SOUTH;
            default:
                assert false;

        }
        return ComponentDirection.NORTH_SOUTH;
    }
    
    /**
     * Determines the direction from two grid points.
     *
     * @param start the starting point
     * @param end   the ending point
     * @return the direction from start to end
     */
    public static ComponentDirection getDirection(final Point start, final Point end) {
        if (start.x == end.x) {
            if (start.y > end.y) {
                return SOUTH_NORTH;
            } else {
                return NORTH_SOUTH;
            }

        } else {
            if (start.x > end.x) {
                return EAST_WEST;
            } else {
                return WEST_EAST;
            }
        }
    }
    
    /** @return {@code true} if this orientation is horizontal (EAST_WEST or WEST_EAST). */
    public boolean isHorizontal() {
        return this == WEST_EAST || this == EAST_WEST;
    }
}
