/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.general.ProjectData;
import ch.technokrat.gecko.geckocircuits.general.StartupWindow;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlInput;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public final class ControlToEXTERNAL extends ControlBlockSimulink implements Comparable<ControlToEXTERNAL>, VariableTerminalNumber {

    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlToEXTERNAL.class, "ToEXT", I18nKeys.EXPORT_DATA_TO_SIMULINK);
    public static final ArrayList<ControlBlock> toExternals = new ArrayList<ControlBlock>();
    private String _externalName = "";
    private static final double WIDTH = 0.3;
    // careful: this variable is only used when the model is read
    // it is not updated when the terminal is changed in the current model.
    private int externalOrderNumber = -1;
    public double[] dataVector;

    public ControlToEXTERNAL() {
        super(3, 0);
        setInputTerminalNumber(3);  // // default: 3 connection to the outside
        toExternals.add(this);
    }

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    public void deleteActionIndividual() {
        super.deleteActionIndividual();
        ControlToEXTERNAL.toExternals.remove(this);
    }

    @Override
    List<ControlBlock> getOrderList() {
        return toExternals;
    }

    @Override
    Stack<AbstractTerminal> getVariableTerminals() {
        return XIN;
    }

    @Override
    public void setInputTerminalNumber(final int number) {
        while (XIN.size() > number) {
            XIN.pop();
        }

        while (XIN.size() < number) {
            XIN.add(new TerminalControlInput(this, -2, -XIN.size()));
        }
        this.dataVector = new double[XIN.size()];
    }

    @Override
    public void setOutputTerminalNumber(final int number) {
        // here, we don't have output terminals
    }

    public void insertOrderCorrect(final int orderNo) {
        externalOrderNumber = orderNo;
        toExternals.sort(Comparator.comparingInt(cb -> {
            if (cb instanceof ControlToEXTERNAL) {
                return ((ControlToEXTERNAL) cb).externalOrderNumber;
            }
            return 0;
        }));
    }

    public void setExternalName(final String name) {
        _externalName = name;
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (StartupWindow.testDialogOpenSourceVersion("Simulink interface")) {
            return new AbstractControlCalculatable(XIN.size(), 0) {
                @Override
                public void calculateYOUT(double deltaT) {
                    
                }
            };
        } else {

            return new AbstractControlCalculatable(XIN.size(), 0) {
                @Override
                public void calculateYOUT(final double deltaT) {
                    try {
                        for (int i1 = 0; i1 < XIN.size(); i1++) {
                            dataVector[i1] = _inputSignal[i1][0];
                            // // from 'parameter[]' the vector with the signal values ​​from the external
                            // // Program to be picked up
                        }
                    } catch (java.lang.ArrayIndexOutOfBoundsException ex) {
                        dataVector = new double[XIN.size()];
                        for (int i1 = 0; i1 < XIN.size(); i1++) {
                            dataVector[i1] = _inputSignal[i1][0];
                            // // from 'parameter[]' the vector with the signal values ​​from the external
                            // // Program to be picked up
                        }
                    }
                }
            };
        }
    }

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        final int posX = getSheetPosition().x;
        final int posY = getSheetPosition().y;
        final Color origColor = graphics.getColor();
        // Klickbereich:
        xKlickMin = (int) (dpix * (posX - WIDTH));
        xKlickMax = (int) (dpix * (posX + WIDTH));
        yKlickMin = (int) (dpix * (posY - WIDTH));
        yKlickMax = dpix * (posY + XIN.size());
        graphics.setColor(getBackgroundColor());

        graphics.fillRect((int) (dpix * (posX - WIDTH)), (int) (dpix * (posY - WIDTH)),
                (int) (dpix * (2 * WIDTH)), dpix * XIN.size());

        graphics.setColor(origColor);
        graphics.drawRect((int) (dpix * (posX - WIDTH)), (int) (dpix * (posY - WIDTH)),
                (int) (dpix * (2 * WIDTH)), dpix * XIN.size());
        // Pfeil-Symbol:
        int d1 = 10, d2 = 4, dpfx = 8, dpfy = 3;
        double pf = 2.0;  // Pfeilspitzen-X-Abstand
        double pfym = posY - WIDTH + XIN.size() / 2;  // Pfeil-Y-Koordinate
        graphics.drawPolygon(new int[]{(int) (dpix * (posX + pf)) - dpfx, (int) (dpix * (posX + pf)) - dpfx,
            (int) (dpix * (posX + pf))}, new int[]{(int) (dpix * pfym) - dpfy, (int) (dpix * pfym) + dpfy, (int) (dpix * pfym)}, 3);
        graphics.drawString("To", (int) (dpix * (posX + WIDTH) + d2), (int) (dpix * pfym - 1 * graphics.getFont().getSize() / 2));
        graphics.drawString("EXTERN", (int) (dpix * (posX + WIDTH) + d2),
                (int) (dpix * pfym + 3 * graphics.getFont().getSize() / 2));
        graphics.drawLine((int) (dpix * (posX + WIDTH)), (int) (dpix * pfym), (int) (dpix * (posX + pf)),
                (int) (dpix * pfym));  // // belonging to the arrow
        graphics.setColor(Color.black);
        graphics.drawString(_externalName, (int) (dpix * (posX + WIDTH) + d2), (int) (dpix * (1 + pfym)
                + 3 * graphics.getFont().getSize() / 2));
        graphics.setColor(origColor);

    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }

    protected void exportAsciiIndividual(final StringBuffer ascii) {
        ProjectData.appendAsString(ascii.append("\ntorder"), toExternals.indexOf(this));
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        insertOrderCorrect(tokenMap.readDataLine("torder", externalOrderNumber));
    }

    @Override
    public int compareTo(final ControlToEXTERNAL otherToExtern) {
        return Integer.compare(this.externalOrderNumber, otherToExtern.externalOrderNumber);
    }

    @Override
    protected final Window openDialogWindow() {
        return new DialogExternal(this);
    }
}
