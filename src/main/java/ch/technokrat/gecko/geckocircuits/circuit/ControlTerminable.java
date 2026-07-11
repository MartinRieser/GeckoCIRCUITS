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
 * Interface for control terminals that participate in node numbering for netlist generation.
 * Each terminal is assigned a unique node number that identifies its signal connection in the
 * control execution order graph.
 *
 * @author andreas
 */
public interface ControlTerminable {
    /**
     * Returns the node number assigned to this terminal.
     *
     * @return the node number
     */
    public int getNodeNumber();

    /**
     * Sets the node number for this terminal.
     *
     * @param newValue the node number to assign
     */
    void setNodeNumber(final int newValue);

    /**
     * Clears the node number, resetting it to an unassigned state.
     */
    void clearNodeNumber();
}
