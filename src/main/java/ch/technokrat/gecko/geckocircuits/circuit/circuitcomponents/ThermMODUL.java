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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.general.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Window;
import java.util.List;

/**
 * Thermal power module component that manages multiple semiconductor chips
 * and their heat flow to the cooler. Draws a module housing with chip inputs
 * and a single heat flow output.
 */
final class ThermMODUL extends AbstractCircuitBlockInterface {
    public static final AbstractTypeInfo TYPE_INFO = 
            new ThermalTypeInfo(ThermMODUL.class, "MOD", I18nKeys.POWER_MODULE_THERMAL_MODEL);

    private final double _height;
    // pro befestigtem Halbleiter(modul) gibt es einen Anschluss
    private double[] _xPrev = new double[MAX_INPUT_NO], _yPrev = new double[MAX_INPUT_NO];
    private final int _noOfChips;
    private double _xOUT, _yOUT;  // // there is 1 signal output (heat flow to the cooler)
    // // to draw a cooler (pixel point data measured from ICEPAK screenshot):
    private final double _scaling;  // Skalierung
    private static final int X_CENTER = 430, Y_CENTER = 250;  // Zentrum des Modul-Bildes in PixelPunkten
    // Dateiname (inkl. Pfad) des Modul-ESBs:
    private String _fileName = GlobalFilePathes.DATNAM_NOT_DEFINED;
    // // ID strings of the SubCircuit elements -->
    private static final int D_E = 5;
    private static final double B_R = 2.5;
    private static final int MAX_INPUT_NO = 99;

    ThermMODUL() {
        super();
        _scaling = B_R / X_CENTER;
        _height = Y_CENTER * _scaling;
        _noOfChips = 2;  // vorerst einmal
    }

    public int getChipCount() {
        return _noOfChips;
    }

    public void setFileName(final String datnam) {
        _fileName = datnam;
    }

    public String getDateiname() {
        return _fileName;
    }
    

    @Override
    protected void drawConnectorLines(final Graphics2D graphics) {
        for (int i1 = 0; i1 < _noOfChips; i1++) {
            _xPrev[i1] = 0 + 1;
            _yPrev[i1] = 0 - _noOfChips - 1 + i1;
            graphics.drawLine((int) (dpix * _xPrev[i1]), (int) (dpix * _yPrev[i1]), dpix * 0 + D_E,
                    (int) (dpix * _yPrev[i1]));
        }
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {
        _xOUT = 0;
        _yOUT = (int) (_height + 1);
        graphics.drawLine((int) (dpix * _xOUT), dpix * (0 + 1), (int) (dpix * _xOUT), (int) (dpix * _yOUT));

        PowerModulePainter.zeichne(graphics, this, graphics.getColor(), dpix);
        if (_noOfChips > 0) {
            drawInputs(graphics, graphics.getColor());
        }
    }

    private void drawInputs(final Graphics graphics, final Color color1) {        
        graphics.setColor(Color.lightGray);
        graphics.fillRect(dpix * getSheetPosition().x - D_E, (int) (dpix * (_yPrev[0] - 1 / 2.0)),
                2 * D_E, (int) (dpix * (_yPrev[_noOfChips - 1] - _yPrev[0] + 1)));
        if (color1.equals(Color.gray)) {
            graphics.setColor(Color.white);
        } else {
            graphics.setColor(Color.darkGray);
        }
        graphics.drawRect(dpix * getSheetPosition().x - D_E, (int) (dpix * (_yPrev[0] - 1 / 2.0)), 2 * D_E,
                (int) (dpix * (_yPrev[_noOfChips - 1] - _yPrev[0] + 1)));
        if (color1.equals(Color.gray)) {
            graphics.setColor(Color.lightGray);
        } else {
            graphics.setColor(color1);
        }
        graphics.drawLine(dpix * getSheetPosition().x, dpix * getSheetPosition().y, 
                dpix * getSheetPosition().x, (int) (dpix * (_yPrev[_noOfChips - 1] + 1 / 2.0)));
    }

    @Override
    public void doDoubleClickAction(final Point clickedPoint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Window openDialogWindow() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
