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

import ch.technokrat.gecko.geckocircuits.general.ProjectData;
import java.util.Random;

/**
 * Generates and manages unique object identifiers for circuit components.
 * Combines hash code with random number generator to produce unique IDs
 * for serialization and cross-referencing in saved circuit files.
 */
public class UniqueObjectIdentifier {

    private static Random generator = new Random();    
    private long identifier = 0;    

    /**
     * Constructs a UniqueObjectIdentifier with an initial identifier of 0.
     */
    public UniqueObjectIdentifier() {
        
    }
    
    /**
     * Returns the current unique identifier.
     * @return the identifier value
     */
    public long getIdentifier() {
        return identifier;
    }
    
    /**
     * Creates a new unique identifier using hash code plus random number.
     * Asserts that the current identifier is 0 before generating.
     */
    public void createNewIdentifier() {
        assert identifier == 0;
        identifier = this.hashCode() + generator.nextInt();
    }
    
    /**
     * Creates a new identifier from a specific value (used during import).
     * @param value the identifier value to assign
     */
    public void createNewIdentifier(final long value) {        
        identifier = value;
    }

    /**
     * Imports the identifier from an ASCII token map.
     * @param tokenMap the token map to read from
     */
    public void importASCII(TokenMap tokenMap) {        
        if(tokenMap.containsToken("uniqueObjectIdentifier")) {
            identifier = tokenMap.readDataLine("uniqueObjectIdentifier", identifier);            
            if(identifier == 0) {
                identifier = this.hashCode() + generator.nextInt();
            }                                    
        } else {
            identifier = this.hashCode() + generator.nextInt();
        }
        
    }

    /**
     * Exports the identifier to an ASCII string buffer.
     * @param ascii the string buffer to append to
     */
    public void exportASCII(final StringBuffer ascii) {        
        ProjectData.appendAsString(ascii.append("\nuniqueObjectIdentifier"), identifier);        
    }                
}
