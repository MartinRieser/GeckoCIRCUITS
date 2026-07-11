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

import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import java.util.List;
import java.util.Stack;

/**
 * Base class for control blocks that interface with Simulink, providing variable terminal
 * management and ordered block lookup used during co-simulation.
 */
abstract class ControlBlockSimulink extends ControlBlock {

    private static final long serialVersionUID = 1L;

    public ControlBlockSimulink() {
        super();
    }
    
    public ControlBlockSimulink(final int noInputs, final int noOutputs) {
        super(noInputs, noOutputs);
    }
    
    abstract Stack<AbstractTerminal> getVariableTerminals();
    
    abstract List<ControlBlock> getOrderList();
    
}
