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

import ch.technokrat.gecko.geckocircuits.circuit.TimeFunction;
import ch.technokrat.gecko.geckocircuits.circuit.TimeFunctionConstant;

/**
 * Calculator for a function-driven voltage source. The output voltage is
 * determined by a time function, and it supports history-based step-back
 * operations for transient simulation.
 */
public class VoltageSourceCalculator extends AbstractVoltageSourceCalculator implements BStampable,
        DirectCurrentCalculatable, HistoryUpdatable {

    private TimeFunction _function;
    private static final int HISTORY_CURRENT_INDEX = 3;
    private static final int HISTORY_VOLTAGE_INDEX = 4;
    
    public VoltageSourceCalculator(final TimeFunction timeFunction, final AbstractVoltageSource parent) {
        super(parent);
        _function = timeFunction;
        _z = -1;
    }

    VoltageSourceCalculator(final double initialValue, final int mat0, final int mat1, 
            final int zValue, final int compNumber, final AbstractTwoPortPowerCircuitBlock parent) {
        super(parent);
        _function = new TimeFunctionConstant(-initialValue);
        matrixIndices[0] = mat0;
        matrixIndices[1] = mat1;
        _componentNumber = compNumber;
        _z = zValue;
    }

    /**
     * Stamps the voltage source contribution into the right-hand side vector B.
     * @param bVector the system B vector
     * @param time the current simulation time
     * @param deltaT the current time step
     */
    @Override
    public final void stampVectorB(final double[] bVector, final double time, final double deltaT) {
        bVector[_z] += _function.calculate(time, deltaT);
    }

    public final void setFunction(final TimeFunction function) {
        _function = function;
    }

    @Override
    public final boolean isBasisStampable() {
        return false;
    }

    @Override
    public final void registerBVector(final BVector bvector) {
        // nothing todo!
    }

    /**
     * Reverts the source state by one time step using the saved history,
     * allowing the solver to re-evaluate the step.
     */
    @Override
    public final void stepBack() {
        if ((!stepped_back && (steps_reversed == 0)) || (stepped_back && (steps_reversed < steps_saved))) {
            if (stepped_back) {
                historyBackward();
            }
            prev_time = var_history[0][0];
            _potential1 = var_history[0][1];
            _potential2 = var_history[0][2];
            _current = var_history[0][HISTORY_CURRENT_INDEX];
            _voltage = var_history[0][HISTORY_VOLTAGE_INDEX];

            _function.stepBack();

            stepped_back = true;
            steps_reversed++;
        }
    }
}
