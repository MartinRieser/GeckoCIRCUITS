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
 * A special "short connector" connection that directly bridges two adjacent terminals
 * without participating in the normal parent-sheet lookup. The parent sheet is stored
 * explicitly so the connector can reference it even before the standard parent chain
 * is established.
 *
 * @author andreas
 */
public class ConnectionShortConnector extends Connection {
    /** The explicitly assigned parent sheet, bypassing the normal parent resolution. */
    private final CircuitSheet _parentSheet;
    
    public ConnectionShortConnector(final ConnectorType connectorType, final CircuitSheet parentSheet) {
        super(connectorType, parentSheet);        
        _parentSheet = parentSheet;
    }

    /**
     * Returns the explicitly stored parent sheet rather than using the normal parent
     * resolution logic, because short connectors are created before the standard
     * parent chain is set up.
     *
     * @return the stored parent circuit sheet
     */
    @Override
    public CircuitSheet getParentCircuitSheet() {
        return _parentSheet;        
    }
    
    
}
