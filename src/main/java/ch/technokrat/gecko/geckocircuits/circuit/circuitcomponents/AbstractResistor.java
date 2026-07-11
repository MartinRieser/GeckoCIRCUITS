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
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.AbstractLossCalculator;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.AbstractLossCalculatorFabric;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculatable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossCalculatorResistor;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Graphics2D;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract base class for resistor components that can operate across multiple
 * simulation domains (electrical LK, reluctance, and thermal). The resistance
 * parameter is mapped to domain-specific units (ohm, Amp-Turns/Weber, K/W).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractResistor extends AbstractTwoPortPowerCircuitBlock
        implements CurrentMeasurable, LossCalculatable, DirectVoltageMeasurable {

    private static final double DEFAULT_RESISTANCE = 1000.0;
    private static final double WIDTH = 0.35;
    private static final double HEIGHT = 0.8;

    final UserParameter<Double> _resistance = UserParameter.Builder.
            <Double>start("resistance", DEFAULT_RESISTANCE).
            mapDomains(ConnectorType.LK, ConnectorType.RELUCTANCE, ConnectorType.THERMAL).
            longName(I18nKeys.RESISTOR_R_OHM, I18nKeys.RELUCTANCE, I18nKeys.RESISTOR_RTH_K_W).
            shortName("R", "Rel", "Rth").
            unit("ohm", "Amp-Turns/Weber", "K/W").
            showInTextInfo(TextInfoType.SHOW_WHEN_DISPLAYPARAMETERS).
            arrayIndex(this, 0).            
            build();
    

    @SuppressWarnings("this-escape")
    protected AbstractResistor() {
    }

    @Override
    public final void setzeParameterZustandswerteAufNULL() {
        parameter[1] = 0;
        parameter[2] = 0;
    }

    @Override
    protected final void drawForeground(final Graphics2D graphics) {
        graphics.drawRect((int) (-dpix * WIDTH), (int) (-dpix * HEIGHT), (int) (dpix * 2 * WIDTH), (int) (dpix * 2 * HEIGHT));
    }

    @Override
    protected final void drawBackground(final Graphics2D graphics) {
        graphics.fillRect((int) (-dpix * WIDTH), (int) (-dpix * HEIGHT), (int) (dpix * 2 * WIDTH), (int) (dpix * 2 * HEIGHT));
    }


    @Override
    protected final Window openDialogWindow() {
        return new ResistorDialog(this);
    }

    /**
     * Creates the loss calculation fabric for this resistor.
     *
     * @return a fabric that produces a {@link LossCalculatorResistor} bound to this component
     */
    @Override
    public final AbstractLossCalculatorFabric getLossCalculation() {
        return new AbstractLossCalculatorFabric() {
            @Override
            public AbstractLossCalculator lossCalculatorFabric() {
                return new LossCalculatorResistor(AbstractResistor.this);
            }
        };
    }
    
    @Override
    public final List<? extends CircuitComponent> getCircuitCalculatorsForSimulationStart() {
        return Arrays.asList(new ResistorCalculator(this));
    }
}
