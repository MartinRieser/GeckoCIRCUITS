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
import java.awt.Graphics;
import java.util.List;

/**
 * A {@link TerminalInterface} implementation that adapts a Connection's
 * start or end point as a terminal, enabling label and position access
 * on connection endpoints.
 */
public class TerminalConnection implements TerminalInterface {
    private final Location _loc;
    private final Connection _verb;
    private final List<Point> _connectorPoints;
    

    /**
     * Returns the simulation domain (connector type) of the parent connection.
     * @return the connector type
     */
    @Override
    public ConnectorType getCategory() {
        return _verb.getSimulationDomain();
    }

    /**
     * Returns the parent Connection that this terminal belongs to.
     * @return the parent connection
     */
    public Connection getParentConnection() {
        return _verb;
    }

    /**
     * Returns the label object from the parent connection.
     * @return the circuit label
     */
    @Override
    public CircuitLabel getLabelObject() {
        return _verb.getLabelObject();
    }

    /**
     * Identifies which endpoint of a Connection this terminal represents.
     */
    enum Location {
        /** The start point of the connection. */
        START,
        /** The end point of the connection. */
        END;
    }
    
    /**
     * Creates a terminal wrapper for a connection endpoint.
     * @param verb the parent connection
     * @param connectorPoints the list of all points defining the connection path
     * @param loc which endpoint (START or END) this terminal represents
     */
    public TerminalConnection(final Connection verb, final List<Point> connectorPoints, final Location loc) {
        _loc = loc;
        _verb = verb;
        _connectorPoints = connectorPoints;
    }

    /**
     * Sets the label rendering priority on the parent connection.
     * @param labelPriority the priority level
     */
    public final void setLabelPriority(final LabelPriority labelPriority) {
        _verb.setLabelPriority(labelPriority);
    }        
    
    /**
     * Returns the position of this terminal (start or end of the connection).
     * @return the terminal position
     */
    @Override
    public Point getPosition() {
        switch(_loc) {
            case START:
                return _connectorPoints.get(0);
            case END:
                return _connectorPoints.get(_connectorPoints.size()-1);
            default:
                assert false;
                return null;
        }
    }

    /**
     * Returns the circuit sheet that contains the parent connection.
     * @return the parent circuit sheet
     */
    @Override
    public CircuitSheet getCircuitSheet() {
        return _verb._parentCircuitSheet;
    }

    /**
     * Paints the terminal as a filled circle at its position.
     * @param graphics the graphics context
     */
    @Override
    public void paintComponent(final Graphics graphics) {
        final int dpix = AbstractCircuitSheetComponent.dpix;
        graphics.fillOval(dpix * getPosition().x - AbstractTerminal.POINT_DIAMETER / 2, 
                dpix * getPosition().y - AbstractTerminal.POINT_DIAMETER / 2,
                AbstractTerminal.POINT_DIAMETER, AbstractTerminal.POINT_DIAMETER);
    }
    
    
    
}
