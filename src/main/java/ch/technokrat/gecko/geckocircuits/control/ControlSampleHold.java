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

import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.SampleHoldCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;

/**
 * Sample-and-hold control block. When the control input z > 0.5, it samples input x1;
 * otherwise it holds the last sampled value.
 */
public final class ControlSampleHold extends ControlBlock {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlSampleHold.class, "SHLD", I18nKeys.SAMPLE_HOLD);
    private static final double THRESHOLD = 0.5;
    
    /**
     * Creates a sample-and-hold block with 2 inputs (signal + control) and 1 output.
     */
    public ControlSampleHold() {
        super(2, 1);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"sample"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.SAMPLED_INPUT};
    }        

    /**
     * @return a new {@link SampleHoldCalculator} instance for simulation
     */
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new SampleHoldCalculator();
    }

    @Override
    protected String getCenteredDrawString() {
        return "S-H";
    }
    
    
    /**
     * @return a dialog displaying the sample-and-hold behavior description
     */
    @Override
    protected  Window openDialogWindow() {
        final String message = "<html>if (z > " + THRESHOLD + ")  ...  y1 = x1;  sh = x1<br>"
                + "if (z < " + THRESHOLD + ")  ...  y1 = sh";
        return new DialogSimpleInfoMessage(this, message);        
    }
}
