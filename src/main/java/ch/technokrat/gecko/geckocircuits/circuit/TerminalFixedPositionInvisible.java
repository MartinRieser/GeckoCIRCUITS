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
import java.awt.Graphics2D;

/**
 * Invisible terminal with fixed position, used by ThermPvChip and other thermal components
 * to reference the zero temperature point without painting a visible terminal on the circuit sheet.
 * Exporting to SVG would make a regular terminal visible, hence this workaround class.
 */
public class TerminalFixedPositionInvisible extends TerminalFixedPosition {

    /**
     * Constructs an invisible fixed-position terminal.
     * @param parent the parent block this terminal belongs to
     * @param position the fixed position point (typically ThermAmbient.THERMAL_ZERO)
     */
    public TerminalFixedPositionInvisible(final AbstractBlockInterface parent, final Point position) {
        super(parent, position);
    }
    
    
    @Override
    public void paintComponent(final Graphics graphics) {
        // nothing to paint!
    }
    
    /**
     * Overloaded paint method that accepts a dpix parameter; intentionally does nothing.
     * @param graphics the graphics context
     * @param dpix pixel scaling factor
     */
    public void paintLabelString(final Graphics2D graphics, final int dpix) {
        // nothing to paint!!!
    }
    
    
}
