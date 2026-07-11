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
import ch.technokrat.gecko.geckocircuits.control.calculators.SubtractionMoreParameter;
import ch.technokrat.gecko.geckocircuits.control.calculators.SubtractionTwoParameter;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

/**
 * Subtraction control block. With 2 inputs it computes x1 - x2; with more inputs it subtracts
 * all subsequent inputs from the first.
 */
public final class ControlSubtraction extends AbstractControlVariableInputs {
    /** Registration metadata for the control framework type registry. */
    static ControlTypeInfo tinfo = new ControlTypeInfo(ControlSubtraction.class, "SUB", I18nKeys.SUBTRACTION);

    public ControlSubtraction() {
        super(2);
    }

    /**
     * @return the output signal name for the subtraction result
     */
    @Override
    public String[] getOutputNames() {
        return new String[]{"difference"};
    }

    /**
     * @return the output description for the difference signal
     */
    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.INPUT_1_MINUS_INPUT_2};
    }            

    /**
     * Returns the appropriate subtraction calculator based on the number of inputs:
     * {@link SubtractionTwoParameter} for 2 inputs, {@link SubtractionMoreParameter} for more.
     *
     * @return the subtraction calculator for simulation
     */
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (XIN.size() == 2) {            
            return new SubtractionTwoParameter();
        } else {
            return new SubtractionMoreParameter(XIN.size());
        }
    }
}
