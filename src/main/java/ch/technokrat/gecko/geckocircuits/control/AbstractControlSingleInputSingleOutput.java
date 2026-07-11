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

import ch.technokrat.gecko.i18n.resources.I18nKeys;

/**
 * Base class for control blocks with exactly one input and one output terminal.
 */
public abstract class AbstractControlSingleInputSingleOutput extends ControlBlock {
    private static final long serialVersionUID = 1L;

    public AbstractControlSingleInputSingleOutput() {
        super(1,1);
    }
    
    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{getTypeDescription()};
    }
    
}
