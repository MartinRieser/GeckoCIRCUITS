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

import ch.technokrat.gecko.geckocircuits.circuit.ComponentCoupling;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.DirectVoltageMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LossProperties;
import ch.technokrat.gecko.geckocircuits.control.ControlGate;

/**
 * Abstract base class for switch components (ideal switch, diode, thyristor, etc.).
 * Switches connect to a control gate and support loss calculation.
 */
public abstract class AbstractSwitch extends AbstractSemiconductor implements CurrentMeasurable, DirectVoltageMeasurable {

    public static final double UF_DEFAULT = 0.60;
    public static final double RD_ON_DEFAULT = 10e-3;
    public static final double RD_OFF_DEFAULT = 1e7;
    /** The control gate block that drives this switch, or {@code null} if unconnected. */
    ControlGate _connectedGateBlock;
    
    protected final LossProperties verluste = new LossProperties(this);

    @SuppressWarnings("this-escape")
    protected AbstractSwitch() {
    }

    /**
     * Called when a component reference is added to this switch; captures the
     * connected control gate.
     *
     * @param added the coupling that was added
     */
    @Override
    public final void doReferenceAddAction(final ComponentCoupling added) {
        if (added.getParent() instanceof ControlGate) {            
            _connectedGateBlock = ((ControlGate) added.getParent());            
        }
    }

    /**
     * Called when a component reference is removed from this switch; clears the
     * connected control gate if the removed reference was a gate.
     *
     * @param removed the coupling that was removed
     */
    @Override
    public final void doReferenceRemoveAction(final ComponentCoupling removed) {
        if (removed.getParent() instanceof ControlGate) {            
            _connectedGateBlock = null;
        }
    }

    public final LossProperties getLossCalculation() {
        return verluste;
    }

    @Override
    public final void initExtraFiles() {
        verluste.getDetailedLosses().initLossFile();
    }             
    
    /**
     * Adds gate-signal text information to the component's text info display,
     * showing an error when no gate is connected.
     */
    void addGateTextInfo() {
        if (_connectedGateBlock == null) {
            _textInfo.addErrorValue("no gate-signal");
        } else {
            final String gateString = _connectedGateBlock.getIDStringDialog() + " >>";
            _textInfo.addParameter(gateString);
        }
    }
    
    @Override
    public void setToolbarPaintProperties() {
        _connectedGateBlock = new ControlGate();        
    }
}
