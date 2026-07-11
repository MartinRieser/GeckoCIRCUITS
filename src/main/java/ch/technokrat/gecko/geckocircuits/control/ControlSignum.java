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
import ch.technokrat.gecko.geckocircuits.control.calculators.SignumCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

/**
 * Signum (sign) function control block. Returns +1 for positive input, -1 for negative input, and 0 for zero.
 */
public final class ControlSignum extends SimpleControlBlock {
    /** Registration metadata for the control framework type registry. */
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlSignum.class, "SGN", I18nKeys.SIGNUM);

    /**
     * Creates a signum block with 1 input and 1 output.
     */
    public ControlSignum() {
        super(1, 1);
    }    
    
    @Override
    public String[] getOutputNames() {
        return new String[]{"sign"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.SIGN_OF_INPUT};
    }

    /**
     * @return a new {@link SignumCalculator} instance for simulation
     */
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new SignumCalculator();        
    }          
    

    /**
     * @return an HTML string describing the signum function behavior
     */
    @Override
    String getDialogMessage() {
        return "<html>x1 >  0  ...  y1 = +1<br>"
            + "x1 <  0  ...  y1 = -1<br>"
            + "x1 == 0 ...  y1 =  0</html>";
    }
}
