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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitTerminal;
import ch.technokrat.gecko.geckocircuits.general.GlobalColors;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.ControlTERMINAL;
import ch.technokrat.gecko.geckocircuits.control.SubCircuitSheet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

/**
 * This is the "block"-terminal of the subcircuit block, which has the terminal
 * name as label.
 *
 * @author andreas
 */
public final class TerminalSubCircuitBlock extends AbstractTerminal implements ControlTerminable {

    private final SubcircuitBlock _subcircuitBlock;
    private SubCircuitTerminable _lkTerminal;
    private int relativeX;
    private int relativeY;
    private int _nodeNumber;

    /**
     * Constructs a subcircuit block terminal linked to a subcircuit terminal.
     * @param relatedComponent the parent subcircuit block
     * @param lkTerminal the subcircuit terminal this block terminal represents
     */
    public TerminalSubCircuitBlock(final SubcircuitBlock relatedComponent, SubCircuitTerminable lkTerminal) {
        super(relatedComponent);
        _subcircuitBlock = relatedComponent;
        _lkTerminal = lkTerminal;        
    }

    /**
     * Returns the absolute position of the block terminal on the circuit sheet.
     * @return position computed from subcircuit block position plus relative offset
     */
    @Override
    public Point getPosition() {
        return new Point(_subcircuitBlock.getSheetPosition().x + relativeX, _subcircuitBlock.getSheetPosition().y + relativeY);
    }

    /**
     * Returns the relative X offset within the subcircuit block.
     * @return the relative X coordinate
     */
    public int getRelativeX() {
        return relativeX;
    }

    /**
     * Returns the relative Y offset within the subcircuit block.
     * @return the relative Y coordinate
     */
    public int getRelativeY() {
        return relativeY;
    }

    /**
     * Paints the terminal using the linked subcircuit terminal's foreground color.
     * @param graphics the graphics context to paint on
     */
    @Override
    public void paintComponent(final Graphics graphics) {
        Color oldColor = graphics.getColor();
        graphics.setColor(_lkTerminal.getForeGroundColor());        
        super.paintComponent(graphics);
        graphics.setColor(oldColor);
    }

    /**
     * Returns the simulation domain category from the linked subcircuit terminal.
     * @return the connector type (LK, CONTROL, etc.)
     */
    @Override
    public ConnectorType getCategory() {

        if (_lkTerminal instanceof AbstractCircuitTerminal) {
            AbstractCircuitTerminal lkTerminal = (AbstractCircuitTerminal) _lkTerminal;
            return lkTerminal.getSimulationDomain();
        }

        if (_lkTerminal instanceof ControlTERMINAL) {
            return ConnectorType.CONTROL;
        }
        assert false;
        return ConnectorType.LK;
    }

    /**
     * Creates a copy of this block terminal, linked to the corresponding subcircuit terminal.
     * @param relatedComponent the new parent subcircuit block
     * @return a new TerminalSubCircuitBlock with the same label and position
     */
    @Override
    public AbstractTerminal createCopy(final AbstractBlockInterface relatedComponent) {        
        final SubCircuitTerminable terminable = (SubCircuitTerminable) relatedComponent;             
        assert terminable.getParentCircuitSheet() instanceof SubCircuitSheet;                
        final SubCircuitSheet subSheet = (SubCircuitSheet) terminable.getParentCircuitSheet();
        final SubcircuitBlock subBlock = subSheet._subBlock;        
        final TerminalSubCircuitBlock returnValue = new TerminalSubCircuitBlock(subBlock, terminable);
        
        returnValue.getLabelObject().setLabel(_label.getLabelString());
        returnValue.relativeX = relativeX;
        returnValue.relativeY = relativeY;
        return returnValue;
    }

    /**
     * Sets the relative position offset within the subcircuit block.
     * @param relX the relative X offset
     * @param relY the relative Y offset
     */
    public void setRelativePosition(final int relX, int relY) {
        relativeX = relX;
        relativeY = relY;
    }

    /**
     * Paints the terminal label and the linked subcircuit terminal name, handling
     * all four terminal locations (LEFT, RIGHT, BOTTOM, UP) with appropriate rotations.
     * @param graphics the graphics context to paint on
     */
    @Override
    public void paintLabelString(final Graphics2D graphics) {        
        final int dpix = AbstractCircuitSheetComponent.dpix;
        Color oldColor = graphics.getColor(); 
        
        graphics.setColor(_lkTerminal.getForeGroundColor());        
        
        if (!_label.getLabelString().isEmpty()) {
            graphics.drawString(_label.getLabelString(), dpix * getPosition().x + DX_IN, dpix * getPosition().y + DY_TEXT);
        }

        String terminalName = _lkTerminal.getStringID();
        FontRenderContext frc = graphics.getFontRenderContext();
        final int stringHeight = (int) graphics.getFont().getStringBounds(terminalName, frc).getHeight();
        final int stringWidth = (int) graphics.getFont().getStringBounds(terminalName, frc).getWidth();

        AffineTransform oldTrans = graphics.getTransform();
        AffineTransform newTrans = new AffineTransform();

        switch (_lkTerminal.getTerminalLocation()) {
            case LEFT:
                graphics.drawString(_lkTerminal.getStringID(), dpix * getPosition().x + dpix / 2 + 2,
                        dpix * getPosition().y + stringHeight / 2 - 1);
                break;
            case RIGHT:
                graphics.drawString(_lkTerminal.getStringID(), dpix * getPosition().x - dpix / 2 - 2 - stringWidth,
                        dpix * getPosition().y + stringHeight / 2 - 1);
                break;
            case BOTTOM:
                newTrans.translate(oldTrans.getTranslateX() + dpix * getPosition().x,
                        oldTrans.getTranslateY() + dpix * getPosition().y);
                newTrans.rotate(-Math.PI / 2);
                graphics.setTransform(newTrans);
                graphics.drawString(_lkTerminal.getStringID(), dpix / 2, stringHeight / 2 - 1);

                break;
            case UP:

                newTrans.translate(oldTrans.getTranslateX() + dpix * getPosition().x,
                        oldTrans.getTranslateY() + dpix * getPosition().y);
                newTrans.rotate(-Math.PI / 2);
                graphics.setTransform(newTrans);
                graphics.drawString(_lkTerminal.getStringID(), -stringWidth - dpix / 2 - 2, stringHeight / 2 - 1);
                break;
            default:
                assert false;
        }

        graphics.setTransform(oldTrans);
        graphics.setColor(oldColor);

    }

    /**
     * Returns the node number assigned to this terminal.
     * @return the node number, or -1 if not assigned
     */
    @Override
    public int getNodeNumber() {
        return _nodeNumber;
    }
    
    /**
     * Sets the node number for this terminal.
     * @param newValue the node number to assign
     */
    @Override
    public void setNodeNumber(final int newValue) {
        _nodeNumber = newValue;
    }
    
    /**
     * Clears the node number, resetting it to -1.
     */
    @Override
    public void clearNodeNumber() {
        _nodeNumber = -1;
    }
}
