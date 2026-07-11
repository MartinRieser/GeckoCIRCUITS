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
 * Base abstraction for scope signals, providing access to signal metadata
 * such as the display name and subcircuit path.
 *
 * @author andreas
 */
public abstract class AbstractScopeSignal {

    /**
     * Returns the human-readable name of this signal.
     *
     * @return the signal name
     */
    public abstract String getSignalName();

    /**
     * Returns the subcircuit path for this signal, or an empty string
     * if the signal is in the top-level circuit.
     *
     * @return the subcircuit path string
     */
    public String getSubcircuitPath() {
        return "";        
    }
        
}
