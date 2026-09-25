/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit.matrix;

import gecko.core.allg.SolverType;

/**
 * Matrix stamper implementation for capacitor components.
 *
 * A capacitor C between nodes x and y is modeled using the companion model of
 * the configured integration method, matching the b-vector history terms of
 * {@code MatrixSolver.buildVectorB}:
 * <ul>
 *   <li>Backward Euler: G = C/dt</li>
 *   <li>Trapezoidal: G = 2C/dt</li>
 *   <li>Gear-Shichman (BDF2): G = 1.5C/dt</li>
 * </ul>
 *
 * A matrix stamps (conductance):
 * - a[x][x] += G
 * - a[y][y] += G
 * - a[x][y] -= G
 * - a[y][x] -= G
 *
 * GUI-free version for use in headless simulation core.
 */
public class CapacitorStamper implements IMatrixStamper {

    /** Minimum capacitance to avoid numerical issues */
    private static final double MIN_CAPACITANCE = 1e-15;

    /** Index for capacitance in parameter array */
    private static final int PARAM_CAPACITANCE = 0;

    /** Index for previous voltage in previousValues array */
    private static final int PREV_VOLTAGE = 0;

    /** Index for previous current in previousValues array */
    private static final int PREV_CURRENT = 1;

    /** Companion-model factor of the configured integration method (G = factor * C/dt). */
    private final double companionFactor;

    /**
     * Creates a backward-Euler capacitor stamper.
     */
    public CapacitorStamper() {
        this(SolverType.SOLVER_BE);
    }

    /**
     * Creates a capacitor stamper for the given integration method.
     *
     * @param solverType integration method whose companion model is stamped
     */
    public CapacitorStamper(final SolverType solverType) {
        this.companionFactor = switch (solverType) {
            case SOLVER_TRZ -> 2.0;
            case SOLVER_GS -> 1.5;
            default -> 1.0;
        };
    }

    @Override
    public void stampMatrixA(MatrixAccumulator a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt) {
        double conductance = getAdmittanceWeight(parameter[PARAM_CAPACITANCE], dt);

        // Stamp the standard two-terminal conductance pattern
        a.add(nodeX, nodeX, conductance);
        a.add(nodeY, nodeY, conductance);
        a.add(nodeX, nodeY, -(conductance));
        a.add(nodeY, nodeX, -(conductance));
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        double capacitance = Math.max(parameter[PARAM_CAPACITANCE], MIN_CAPACITANCE);
        double conductance = capacitance / dt;

        // Get previous voltage across capacitor
        double vPrev = 0.0;
        if (previousValues != null && previousValues.length > PREV_VOLTAGE) {
            vPrev = previousValues[PREV_VOLTAGE];
        }

        // History current source: I_hist = G * v_prev = (C/dt) * v_prev
        double historySource = conductance * vPrev;

        // Stamp as current source from node Y to node X
        b[nodeX] += historySource;
        b[nodeY] -= historySource;
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        double capacitance = Math.max(parameter[PARAM_CAPACITANCE], MIN_CAPACITANCE);

        // For capacitor: i = C * dv/dt = C * (v - v_prev) / dt
        // Using the current voltage difference and the conductance model:
        // i = G * (Vx - Vy) where part of this is the actual capacitor current
        double conductance = capacitance / dt;
        double voltage = nodeVoltageX - nodeVoltageY;

        // The actual current is computed from the capacitor equation
        // This is a simplified model; actual implementation may need previous voltage
        return conductance * voltage;
    }

    @Override
    public double getAdmittanceWeight(double capacitance, double dt) {
        // Clamp capacitance to minimum value
        double safeCapacitance = Math.max(capacitance, MIN_CAPACITANCE);
        return companionFactor * safeCapacitance / dt;
    }

    /**
     * Calculates the equivalent conductance for trapezoidal integration.
     * For trapezoidal rule: G = 2C/dt
     *
     * @param capacitance the capacitance value
     * @param dt time step size
     * @return equivalent conductance for trapezoidal integration
     */
    public double getAdmittanceWeightTrapezoidal(double capacitance, double dt) {
        double safeCapacitance = Math.max(capacitance, MIN_CAPACITANCE);
        return 2.0 * safeCapacitance / dt;
    }
}
