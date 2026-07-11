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
import ch.technokrat.gecko.geckocircuits.control.calculators.RoundCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;

/**
 * Rounding control block. Rounds its input to the nearest integer using {@link RoundCalculator}.
 */
public final class ControlRound extends AbstractControlSingleInputSingleOutput {
    /** Registration metadata for the control framework type registry. */
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlRound.class, "ROUND", I18nKeys.ROUND);

    /**
     * @return the output signal name for the rounded result
     */
    @Override
    public String[] getOutputNames() {
        return new String[]{"rounded"};
    }
    
    /**
     * @return a new {@link RoundCalculator} instance for simulation
     */
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new RoundCalculator();        
    }


    /**
     * @return the centered label string drawn on the block symbol
     */
    @Override
    protected String getCenteredDrawString() {
        return "RND";
    }                

    /**
     * @return the dialog window for this block (no parameters to configure)
     */
    @Override
    protected final Window openDialogWindow() {
        return new DialogWindowWithoutInput(this);
    }
    
    
    /**
     * @return the output description for the rounded signal
     */
    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.INPUT_ROUNDED_TO_INTEGER_DESCRIPTION};
    }
    
}
