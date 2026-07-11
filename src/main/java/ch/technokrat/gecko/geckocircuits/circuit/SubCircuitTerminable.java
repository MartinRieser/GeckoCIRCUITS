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
import java.awt.Color;

/**
 * Represents a terminal that bridges between a subcircuit sheet and its
 * parent block, defining the physical connection point on the subcircuit
 * boundary.
 */
public interface SubCircuitTerminable {
    /**
     * Returns the terminal block that represents this terminal on the
     * subcircuit boundary.
     * @return the block terminal instance
     */
    public TerminalSubCircuitBlock getBlockTerminal();
    /**
     * Returns which side of the subcircuit block the terminal is located on.
     * @return the terminal location (UP, DOWN, LEFT, RIGHT)
     */
    public EnumTerminalLocation getTerminalLocation();
    /**
     * Returns the position of this terminal on the internal subcircuit sheet.
     * @return the sheet position
     */
    public Point getSheetPosition();
    /**
     * Sets the sheet position without registering an undo action.
     * @param sheetPosition the new position
     */
    public void setSheetPositionWithoutUndo(Point sheetPosition);
    /**
     * Returns the unique identifier string of this terminal.
     * @return the string ID
     */
    public String getStringID();
    /**
     * Returns the parent circuit sheet that contains this terminal.
     * @return the parent circuit sheet
     */
    public CircuitSheet getParentCircuitSheet();
    /**
     * Returns the foreground color for rendering this terminal.
     * @return the foreground color
     */
    public Color getForeGroundColor();
}
