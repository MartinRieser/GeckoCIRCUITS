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
package gecko.core.simulation.solver;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.parameters.DiodeParameters;
import gecko.core.circuit.semiconductors.ShockleyDiodeModel;
import gecko.core.circuit.netlist.INetList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Newton-Raphson convergence controller for the smooth Shockley diode model.
 *
 * <p>After each MNA solve the controller re-linearizes every diode's junction
 * equation at the latest junction voltage v0 and writes the linearization
 * into the element's parameter slots, which the existing diode stamps
 * implement exactly:
 * <pre>
 *   i_lin(v) = g(v0) * v + (i(v0) - g(v0) * v0)  =  (v - uF) / rD
 *   rD = 1 / g(v0)
 *   uF = v0 - i(v0) / g(v0)
 * </pre>
 * Iteration continues until the junction voltage change falls below the
 * tolerance. The piecewise-linear state machine keeps handling latching
 * (thyristor) and gated (IGBT) devices; if the Newton iteration does not
 * converge within the cap, the controller flags a fallback and the
 * piecewise-linear machine takes the diodes back for that step.
 *
 * @see ShockleyDiodeModel
 * @see ComponentCurrentCalculator
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public final class NonlinearConvergenceController {

    private static final Logger LOGGER = LogManager.getLogger(NonlinearConvergenceController.class);

    /** Junction-voltage convergence tolerance in volts. */
    public static final double VOLTAGE_TOLERANCE_V = 1e-6;

    /** Thermal voltage kT/q at 25 degrees Celsius, in volts. */
    public static final double DEFAULT_THERMAL_VOLTAGE_V = 25.85e-3;

    /** Default ideality factor (emission coefficient) of the junction. */
    public static final double DEFAULT_EMISSION_COEFFICIENT = 1.0;

    /** Fallback rated current when the netlist carries no rated current. */
    public static final double DEFAULT_REFERENCE_CURRENT_A = 1.0;

    /** Newton iterations per time step before falling back to piecewise-linear. */
    public static final int MAX_NEWTON_ITERATIONS = 50;

    /** Conductance floor; its inverse matches the classic OFF resistance. */
    public static final double MIN_CONDUCTANCE_S = 1.0 / SolverConstants.RD_OFF_THRESHOLD;

    /** Conductance ceiling bounding the on-state resistance from below. */
    public static final double MAX_CONDUCTANCE_S = 1.0e6;

    private final int[] diodeElements;
    private final ShockleyDiodeModel[] models;
    private final double[] previousVoltages;
    private int newtonIterations;
    private boolean fallbackToPiecewiseLinear;
    private boolean fallbackWarned;

    private NonlinearConvergenceController(final int[] diodeElements,
                                           final ShockleyDiodeModel[] models) {
        this.diodeElements = diodeElements;
        this.models = models;
        this.previousVoltages = new double[diodeElements.length];
    }

    /**
     * Creates the controller from the netlist's initial diode parameters:
     * one Shockley model per LK_D element, calibrated to the element's
     * forward voltage at its rated current.
     *
     * @param netlist the circuit netlist
     * @return controller over the netlist's diodes (possibly empty)
     */
    public static NonlinearConvergenceController createFromNetlist(final INetList netlist) {
        int count = 0;
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if (netlist.getType(i) == CircuitTypCore.LK_D) {
                count++;
            }
        }
        final int[] elements = new int[count];
        final ShockleyDiodeModel[] models = new ShockleyDiodeModel[count];
        int index = 0;
        for (int i = 0; i < netlist.getElementCount(); i++) {
            if (netlist.getType(i) == CircuitTypCore.LK_D) {
                elements[index] = i;
                final double[] params = netlist.getParameter(i);
                final double referenceCurrent =
                        params[DiodeParameters.INDEX_CURRENT] > 0.0
                                ? params[DiodeParameters.INDEX_CURRENT]
                                : DEFAULT_REFERENCE_CURRENT_A;
                models[index] = ShockleyDiodeModel.fromOperatingPoint(
                        params[DiodeParameters.INDEX_FORWARD_VOLTAGE], referenceCurrent,
                        DEFAULT_EMISSION_COEFFICIENT, DEFAULT_THERMAL_VOLTAGE_V);
                index++;
            }
        }
        return new NonlinearConvergenceController(elements, models);
    }

    /**
     * Resets the per-step iteration state (previous voltages, fallback flag).
     */
    public void beginStep() {
        for (int k = 0; k < previousVoltages.length; k++) {
            previousVoltages[k] = Double.NaN;
        }
        newtonIterations = 0;
        fallbackToPiecewiseLinear = false;
    }

    /**
     * Re-linearizes all diodes at the latest junction voltages and reports
     * the Newton convergence state.
     *
     * @param nodePotentials the latest node potentials from the MNA solve
     * @param netlist the circuit netlist receiving the updated stamps
     * @return true when all diode junction voltages changed less than the
     *         tolerance since the previous iteration (or the fallback is active)
     */
    public boolean updateDiodeStamps(final double[] nodePotentials, final INetList netlist) {
        if (diodeElements.length == 0 || fallbackToPiecewiseLinear) {
            return true;
        }
        if (++newtonIterations > MAX_NEWTON_ITERATIONS) {
            fallbackToPiecewiseLinear = true;
            if (!fallbackWarned) {
                fallbackWarned = true;
                LOGGER.warn("Shockley Newton-Raphson did not converge within {} iterations; "
                        + "falling back to the piecewise-linear diode state machine",
                        MAX_NEWTON_ITERATIONS);
            }
            return false;
        }
        double maxDeltaV = 0.0;
        for (int k = 0; k < diodeElements.length; k++) {
            final int element = diodeElements[k];
            final int nodeX = netlist.getNodeX(element);
            final int nodeY = netlist.getNodeY(element);
            final double voltage = nodePotentials[nodeX] - nodePotentials[nodeY];

            final double conductance = clampConductance(models[k].conductance(voltage));
            final double current = models[k].current(voltage);
            final double[] params = netlist.getParameter(element);
            params[DiodeParameters.INDEX_CURRENT_RESISTANCE] = 1.0 / conductance;
            params[DiodeParameters.INDEX_FORWARD_VOLTAGE] = voltage - current / conductance;
            params[DiodeParameters.INDEX_CURRENT] = current;
            params[DiodeParameters.INDEX_VOLTAGE] = voltage;

            final double deltaV = Double.isNaN(previousVoltages[k])
                    ? Double.POSITIVE_INFINITY : Math.abs(voltage - previousVoltages[k]);
            maxDeltaV = Math.max(maxDeltaV, deltaV);
            previousVoltages[k] = voltage;
        }
        return maxDeltaV < VOLTAGE_TOLERANCE_V;
    }

    /**
     * Checks whether the Newton iteration gave up for the current step and
     * the piecewise-linear machine must take the diodes back.
     *
     * @return true when the fallback is active for the current step
     */
    public boolean isFallbackToPiecewiseLinear() {
        return fallbackToPiecewiseLinear;
    }

    /**
     * Gets the number of LK_D elements under Newton-Raphson control.
     *
     * @return diode count
     */
    public int getDiodeCount() {
        return diodeElements.length;
    }

    private static double clampConductance(final double conductance) {
        return Math.min(Math.max(conductance, MIN_CONDUCTANCE_S), MAX_CONDUCTANCE_S);
    }
}
