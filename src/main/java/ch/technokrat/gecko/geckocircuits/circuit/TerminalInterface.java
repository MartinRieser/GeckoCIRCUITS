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

/**
 * Base interface for all terminal types in the circuit sheet.
 * Defines the common methods that every terminal must implement for positioning,
 * painting, and determining its connection category.
 */
public interface TerminalInterface extends Labable {    
    /**
     * Returns the position of this terminal on the circuit sheet.
     * @return the terminal position point
     */
    Point getPosition();
    
    /**
     * Returns the circuit sheet that this terminal belongs to.
     * @return the parent circuit sheet
     */
    CircuitSheet getCircuitSheet();
    
    /**
     * Paints this terminal component on the circuit sheet.
     * @param graphics the graphics context to paint on
     */
    void paintComponent(final Graphics graphics);    
    
    /**
     * Returns the connection category (e.g., LK, CONTROL, THERMAL) of this terminal.
     * @return the connector type category
     */
    public ConnectorType getCategory();
}
