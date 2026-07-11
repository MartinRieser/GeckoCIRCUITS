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
 * Enumerates the available control signal source types (sine, triangle, rectangle, random, import)
 * with mappings for both legacy and current GeckoCIRCUITS file format IDs.
 */
public enum ControlSourceType {
    QUELLE_SIN(402, 0, "SINE"),
    QUELLE_DREIECK(403, 1, "TRIANGLE"),    
    QUELLE_RECHTECK(404, 2, "RECTANGLE"),
    SOURCE_RANDOM(405, 3, "RANDOM"),
    QUELLE_IMPORT(406,4, "IMPORT");
    
    private final String _outputString;
    private final int _oldGeckoID;
    private final int _newGeckoID;
    
    /**
     * Creates a source type with legacy and current IDs and a display string.
     *
     * @param oldGeckoID the legacy file format identifier
     * @param newGeckoID the current file format identifier
     * @param outputString the human-readable name
     */
    ControlSourceType(final int oldGeckoID, final int newGeckoID, final String outputString) {
        _oldGeckoID = oldGeckoID;
        _newGeckoID = newGeckoID;
        _outputString = outputString;
    }
    
    /**
     * Resolves a source type from its legacy or current numeric ID.
     *
     * @param idValue the ID to look up
     * @return the matching ControlSourceType, or QUELLE_RECHTECK if not found
     */
    public static ControlSourceType getFromID(final int idValue) {
        for(ControlSourceType tmp : ControlSourceType.values()) {
            if(idValue == tmp._oldGeckoID) {
                    return tmp;
            }
        }
        for(ControlSourceType tmp : ControlSourceType.values()) {
            if(idValue == tmp._newGeckoID) {
                    return tmp;
            }
        }
        assert false;
        return ControlSourceType.QUELLE_RECHTECK;
    }
    
    public int getOldGeckoID() {
        return _oldGeckoID;
    }       
    
    @Override
    public String toString() {
        return _outputString;
    }

    /**
     * @return the current format numeric ID for this source type
     */
    public int getNewID() {
        return _newGeckoID;
    }
}
