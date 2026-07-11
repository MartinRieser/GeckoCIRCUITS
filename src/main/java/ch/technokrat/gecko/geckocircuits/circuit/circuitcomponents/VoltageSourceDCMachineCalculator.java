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

import ch.technokrat.gecko.geckocircuits.circuit.TimeFunctionConstant;

// TODO: Ath the moment, the machine equations are implemented somewhere else
// We have to merge the two approaches, soon!

/**
 * Calculator for a DC machine EMF voltage source. It models the
 * electromechanical interaction by computing back-EMF from armature
 * current, field current, and mechanical speed.
 */
public class VoltageSourceDCMachineCalculator extends VoltageSourceCalculator implements BStampable, PostProcessable {

    private double phi;
    private double emf;
    private double rotationalSpeed;
    private double omegaOld;
    private double Fr;
    private double omega;
    private double electricalTorque;
    private double _J;
    private double momentLast;
    private double _cM;
    private InductorCouplingCalculator _le;
    private InductorCouplingCalculator _la;
    private final TimeFunctionConstant _timeFunction;
    private double _Ne;

    public VoltageSourceDCMachineCalculator(TimeFunctionConstant timeFunction, InductorCouplingCalculator le, 
            InductorCouplingCalculator la, final AbstractVoltageSource parent) {        
        super(timeFunction, parent);
        _timeFunction = timeFunction;
        _la = la;
        _le = le;
    }

    public void setInertia(double value) {
        _J = value;
    }

    public void setFr(double value){
        Fr = value;
    }

    public void setNe(double value) {
        _Ne = value;
    }

    public void setCm(double value) {
        _cM = value;
    }

    public void setTorque(double value) {
        momentLast = value;
    }

    /**
     * Solves the mechanical equation for the DC machine: computes flux,
     * electrical torque, rotational speed, and back-EMF from the armature
     * and field currents.
     * @param dt the current time step
     * @param time the current simulation time
     */
    public void doPostProcess(double dt, double time) {
        // // from the internal subcircuit -->
        double ia = - _la._current;  // Ankerstrom
        double ie = _le._current;  // Erregerstrom

        // Motor-Gleichungen durchrechnen -->
        phi = _le.getInductance() / _Ne * ie;  // Erregerfluss
        electricalTorque = _cM * phi * ia;  // elektrisches Moment
        omega = (_J / dt * omegaOld + electricalTorque - momentLast) / (_J / dt + Fr);

        rotationalSpeed = (60.0 / (2 * Math.PI)) * omega;
        
        emf = _cM * phi * omega;  // // internal tension of the machine
        _timeFunction.setValue(emf);  // // DC value of the internal voltage source
        omegaOld = omega;
    }
}
