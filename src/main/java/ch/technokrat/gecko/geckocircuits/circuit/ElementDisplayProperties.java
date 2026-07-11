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
 * Controls display and visibility flags for circuit element rendering.
 * @author andreas
 */
public class ElementDisplayProperties {
    /** Whether the element name is displayed. */
    public boolean showName = true;
    /** Whether the element parameter value is displayed. */
    public boolean showParameter = true;
    /** Whether the flow direction symbol is displayed. */
    public boolean showFlowSymbol = false;
    /** Whether additional text lines are displayed. */
    public boolean showTextLine = true;
}
