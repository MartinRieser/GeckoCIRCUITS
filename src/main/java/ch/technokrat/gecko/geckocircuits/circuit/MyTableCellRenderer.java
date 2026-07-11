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

import ch.technokrat.gecko.geckocircuits.general.TechFormat;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Table cell renderer that displays numeric values using engineering notation
 * via {@link TechFormat#formatENG(double, int)}.
 */
class MyTableCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    private final TechFormat tf = new TechFormat();
    public MyTableCellRenderer() {
    }

    /**
     * Sets the displayed text by formatting a numeric value with engineering notation.
     * If the value is null, the cell is left blank.
     * @param value the value to format and display
     */
    public void setValue(Object value) {
        if(value == null) {
            setText("");
        } else {
            setText(tf.formatENG(Double.parseDouble(value.toString()), 3));        
        }        
    }    
}