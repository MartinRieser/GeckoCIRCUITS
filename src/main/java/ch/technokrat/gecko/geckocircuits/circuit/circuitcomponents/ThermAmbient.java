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

import ch.technokrat.gecko.geckocircuits.general.UserParameter;
import static ch.technokrat.gecko.geckocircuits.circuit.AbstractCircuitSheetComponent.dpix;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTypeInfo;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentDirection;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalFixedPosition;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalTwoPortRelativeFixedDirection;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.List;

// // Defined reference temperature specifically for internal subcircuits, is actually NOT an ambient temperature
// // --> implemented as a low-resistance with only one accessible node on a
// Benutzer-definierbares Bezugstemperaturpotential
public final class ThermAmbient extends AbstractCircuitBlockInterface {
    public static final AbstractTypeInfo TYPE_INFO = 
            new ThermalTypeInfo(ThermAmbient.class, "TREF", I18nKeys.REFERENCE_TEMPERATURE);

    private static final double LINE_LENTH = 0.7;
    private static final int RECT_WIDTH = 3;
    
    // // the TEMP and FLOW elements generated 'behind' a MODULE (or PvCHIP) are with a from
    // SchematicEntry aus unerreichbaren Punkt auf einen Bezugpunkt (Potential 'Null') gelegt -->    
    public static final Point THERMAL_ZERO = new Point(-4711, -4711);
    // // you can only specify a 'global' reference temperature, this is always
    // // Zero and cannot be changed (for now).
    public static final double T_ZERO = 0;  
    
    final UserParameter<Double> _ambientTemp = UserParameter.Builder.
            <Double>start("tRef", 0.0).
            longName(I18nKeys.REFERENCE_TEMPERATURE).
            shortName("t_ref").
            unit("°C").
            showInTextInfo(TextInfoType.SHOW_NEVER). // nonlinearity: custom display!
            arrayIndex(this, 0).
            build();

    ThermAmbient() {
        super();
        XIN.add(new TerminalTwoPortRelativeFixedDirection(this, -1, ComponentDirection.NORTH_SOUTH));
        YOUT.add(new TerminalFixedPosition(this, THERMAL_ZERO));
    }

    @Override
    public void setzeParameterZustandswerteAufNULL() {
        parameter[1] = 0;
        parameter[2] = 0;
    }

    @Override
    protected void drawForeground(final Graphics2D graphics) {        
        graphics.fillRect(-dpix / 2, (int) (dpix * LINE_LENTH), dpix, RECT_WIDTH);
    }
    
    @Override
    void drawConnectorLines(final Graphics2D graphics) {
        graphics.drawLine(0, (int) (dpix * LINE_LENTH), 0, -dpix);
    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();
        _textInfo.addParameter(tcf.formatENG(ThermAmbient.T_ZERO, 2));
    }

    @Override
    public void setComponentDirection(final ComponentDirection orientierung) {
        super.setComponentDirection(ComponentDirection.NORTH_SOUTH);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        // this is special: you cannot rotate this component!
        setComponentDirection(ComponentDirection.NORTH_SOUTH);
    }        

    @Override
    protected Window openDialogWindow() {
        return new ThermAmbientDialog(this);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
