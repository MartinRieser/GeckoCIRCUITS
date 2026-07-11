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
package ch.technokrat.gecko.geckocircuits.control.javablock;


/**
 * Represents the outcome of a Java block or script compilation attempt.
 */
public enum CompileStatus {

    /** The source has not been compiled yet. */
    NOT_COMPILED,
    /** Compilation completed without errors. */
    COMPILED_SUCCESSFUL,
    /** Compilation failed with one or more errors. */
    COMPILE_ERROR;

    /**
     * Resolves the enum constant matching the given ordinal value.
     *
     * @param ordinal the ordinal position to look up
     * @return the matching compile status
     */
    public static CompileStatus getFromOrdinal(final int ordinal) {
        for (CompileStatus val : CompileStatus.values()) {
            if (val.ordinal() == ordinal) {
                return val;
            }
        }
        throw new IllegalArgumentException("Invalid ordinal: " + ordinal);
    }
};
