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

import ch.technokrat.gecko.geckocircuits.control.calculators.SqrtCalculator;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;

/**
 * Square root control block. Computes the square root of its input: y = sqrt(x).
 */
public final class ControlSQRT extends AbstractControlSingleInputSingleOutput {
    /** Registration metadata for the control framework type registry. */
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlSQRT.class, "SQRT", I18nKeys.SQRT);

    /**
     * @return the output signal name for the square root result
     */
    @Override
    public String[] getOutputNames() {
        return new String[]{"root"};
    }   
    
    /**
     * @return a new {@link SqrtCalculator} instance for simulation
     */
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new SqrtCalculator();        
    }
    
    /**
     * @return a dialog displaying the formula y1 = sqrt(x1)
     */
    @Override
    protected Window openDialogWindow() {
        return new DialogSimpleInfoMessage(this, "y1 = sqrt (x1)");
    }
    
    /**
     * @return the output description for the square root signal
     */
    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.SQUARE_ROOT_OF_INPUT};
    }
}
