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

import ch.technokrat.gecko.GeckoRuntimeException;
import ch.technokrat.gecko.geckocircuits.general.ProjectData;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.SubCircuitSheet;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.swing.JOptionPane;


/**
 * I made this component to be wrapped from ControlTerminal and LKreisterminal.
 * Otherwise, I cannot avoid code duplication. This is maybe not really
 * "beautiful", however DRY: don't repeat yourself!
 * @author andy
 */
public final class TerminalToWrap {

    private final AbstractBlockInterface _parentComponent;
    private final SubCircuitTerminable _subTerminable;
    private EnumTerminalLocation _terminalLocation = EnumTerminalLocation.LEFT;
    private TerminalSubCircuitBlock _blockTerminal;
    
    /**
     * Constructs a TerminalToWrap for the given parent component.
     * @param parentComponent the parent component, must implement SubCircuitTerminable
     */
    public TerminalToWrap(final AbstractBlockInterface parentComponent) {
        _parentComponent = parentComponent;
        assert parentComponent instanceof SubCircuitTerminable;
        _subTerminable = (SubCircuitTerminable) _parentComponent;
    }

    /**
     * Recalculates the terminal location based on the move-to point and worksheet boundaries.
     * Determines whether the terminal should be placed LEFT, RIGHT, BOTTOM, or UP.
     * @param moveToPoint the target point for relocation
     */
    public void reCalculateLocation(final Point moveToPoint) {
        final int wsSizeX = _parentComponent.getParentCircuitSheet()._worksheetSize.getSizeX();
        final int wsSizeY = _parentComponent.getParentCircuitSheet()._worksheetSize.getSizeY();
        final int checkedPointX = Math.min(moveToPoint.x + _parentComponent.getPositionBeforeMoving().x, wsSizeX - 1);
        final int checkedPointY = Math.min(moveToPoint.y + _parentComponent.getPositionBeforeMoving().y, wsSizeY - 1);
        if (checkedPointY > checkedPointX * 1.0 * wsSizeY / wsSizeX) {
            if (checkedPointY < wsSizeY - checkedPointX * 1.0 * wsSizeY / wsSizeX) {
                _parentComponent.setSheetPositionWithoutUndo(new Point(1, checkedPointY));
                _terminalLocation = EnumTerminalLocation.LEFT;
            } else {
                _parentComponent.setSheetPositionWithoutUndo(new Point(checkedPointX, wsSizeY - 1));
                _terminalLocation = EnumTerminalLocation.BOTTOM;
            }

        } else {
            if (checkedPointY < wsSizeY - checkedPointX * 1.0 * wsSizeY / wsSizeX) {
                _parentComponent.setSheetPositionWithoutUndo(new Point(checkedPointX, 2));
                _terminalLocation = EnumTerminalLocation.UP;
            } else {
                _parentComponent.setSheetPositionWithoutUndo(new Point(wsSizeX - 1, checkedPointY));
                _terminalLocation = EnumTerminalLocation.RIGHT;
            }
        }

        if (_parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet) {
            ((SubCircuitSheet) _parentComponent.getParentCircuitSheet())._subBlock.recalculateTerminalPositions();
        }
    }

    /**
     * Draws the background circle of the terminal.
     * @param graphics the graphics context to paint on
     */
    public void drawBackground(final Graphics2D graphics) {
        final int diameter = AbstractBlockInterface.dpix;
        graphics.fillOval(-diameter / 2 - 1, -diameter / 2 - 1, diameter + 1, diameter + 1);
    }

    /**
     * Validates terminal positions after placement and shows a warning dialog
     * if any terminals share the same position within the subcircuit block.
     */
    public void absetzenElement() {
        
        if (_parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet
                && !((SubCircuitSheet) _parentComponent.getParentCircuitSheet())._subBlock.areTerminalPositionsOK()) {
            String problematicList = "";
            
            for(SubCircuitTerminable term : ((SubCircuitSheet) _parentComponent.getParentCircuitSheet())._subBlock.getTerminalsWithWrongPosition()) {
                problematicList += term.getStringID() + " position (" + term.getBlockTerminal().getRelativeX() + " " + term.getBlockTerminal().getRelativeY() + ")\n";
            }
            
            JOptionPane.showMessageDialog(null,
                    "The following terminals have an identical position within the subcircuit block:\n"
                    + problematicList
                    + "Please move these terminals to other locations on the subcircuit sheet.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);            
        }

    }

    /**
     * Moves the terminal component; note the suspicious condition that returns early
     * when moveToPoint.x equals moveToPoint.y (likely a safeguard against diagonal movement).
     * @param moveToPoint the target point for the move
     */
    public void moveComponent(final Point moveToPoint) {
        if (moveToPoint.x == moveToPoint.y) {
            return;
        }
        reCalculateLocation(moveToPoint);
    }

    /**
     * Performs individual cleanup on delete: removes the terminal from the subcircuit block.
     */
    public void deleteActionIndividual() {
        if (_parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet) {
            final SubCircuitSheet sub = (SubCircuitSheet) _parentComponent.getParentCircuitSheet();
            sub._subBlock._myTerminals.remove(_subTerminable);
            sub._subBlock.XIN.remove(_subTerminable.getBlockTerminal());
        }
    }

    /**
     * Draws the foreground circle and, when selected, draws a connector line to the subcircuit block.
     * @param graphics the graphics context to paint on
     */
    public void drawForeground(final Graphics2D graphics) {
        final int dpix = AbstractBlockInterface.dpix;
        final int diameter = dpix / 2;
        graphics.fillOval(-diameter / 2 - 1, -diameter / 2 - 1, diameter + 1, diameter + 1);        
        
        if (_parentComponent.getModus() == ComponentState.SELECTED 
                && _parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet) {
            final AffineTransform oldTrans = graphics.getTransform();
            final SubcircuitBlock subBlock = ((SubCircuitSheet) _parentComponent.getParentCircuitSheet())._subBlock;
            AffineTransform newtransform = new AffineTransform(oldTrans);
            graphics.setTransform(newtransform);
            graphics.translate((- subBlock.getSheetPosition().x - getBlockTerminal().getRelativeX()) * dpix,
                    (- subBlock.getSheetPosition().y - getBlockTerminal().getRelativeY()) * dpix);
            subBlock.paintGeckoComponent(graphics);
            graphics.setTransform(oldTrans);
        }
    }
    

    /**
     * Creates or retrieves the block terminal and inserts it into the subcircuit block.
     */
    public void createBlockTerminal() {
        if (_parentComponent.getParentCircuitSheet() instanceof SubCircuitSheet) {
            final SubCircuitSheet subSheet = (SubCircuitSheet) _parentComponent.getParentCircuitSheet();
            final SubcircuitBlock subBlock = subSheet._subBlock;
            final String oldLabel = _parentComponent.YOUT.pop().getLabelObject().getLabelString();
            if (_subTerminable.getBlockTerminal() == null) {
                _blockTerminal = new TerminalSubCircuitBlock(subBlock, _subTerminable);
            }
            _subTerminable.getBlockTerminal().getLabelObject().setLabel(oldLabel);
            _parentComponent.YOUT.add(_subTerminable.getBlockTerminal());
            subBlock.insertTerminal(_subTerminable);
        }
    }

    /**
     * Exports the terminal location to the ASCII data format.
     * @param ascii the string buffer to append to
     */
    public void exportAsciiIndividual(final StringBuffer ascii) {
        ProjectData.appendAsString(ascii.append("\nterminalLocation"), _terminalLocation.ordinal());
    }

    /**
     * Imports the terminal location from a token map.
     * @param tokenMap the token map to read from
     */
    public void importIndividual(final TokenMap tokenMap) {
        _terminalLocation = EnumTerminalLocation.getFromOrdinal(tokenMap.readDataLine("terminalLocation",
                _terminalLocation.ordinal()));
    }

    /**
     * Copies additional parameters (terminal location) from an original terminal.
     * @param originalTerminal the terminal to copy from
     */
    public void copyAdditionalParameters(final TerminalToWrap originalTerminal) {
        _terminalLocation = originalTerminal._terminalLocation;
    }

    /**
     * Checks whether two subcircuit terminables have the same block terminal position.
     * @param terminable1 the first terminable
     * @param terminable2 the second terminable
     * @return true if both have identical relative X and Y positions
     */
    public static boolean sameBlockPosition(final SubCircuitTerminable terminable1, final SubCircuitTerminable terminable2) {
        final TerminalSubCircuitBlock termSub1 = terminable1.getBlockTerminal();
        final TerminalSubCircuitBlock termSub2 = terminable2.getBlockTerminal();
        
        final int relX1 = termSub1.getRelativeX();
        final int relX2 = termSub2.getRelativeX();
        final int relY1 = termSub1.getRelativeY();
        final int relY2 = termSub2.getRelativeY();
        return relX1 == relX2 && relY1 == relY2;
    }

    /**
     * Returns the current terminal location.
     * @return the terminal location enum
     */
    public EnumTerminalLocation getTerminalLocation() {
        return _terminalLocation;
    }

    /**
     * Returns the associated block terminal.
     * @return the block terminal instance
     */
    public TerminalSubCircuitBlock getBlockTerminal() {
        return _blockTerminal;
    }
}
