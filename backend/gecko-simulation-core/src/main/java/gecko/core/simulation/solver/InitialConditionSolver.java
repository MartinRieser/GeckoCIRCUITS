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

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.INetList;
import gecko.core.circuit.parameters.CapacitorParameters;
import gecko.core.circuit.parameters.InductorParameters;
import gecko.core.circuit.parameters.SourceParameters;

/**
 * Solver for setting initial conditions in circuit simulation.
 *
 * <p>Initializes node potentials and component currents for MNA simulation,
 * supporting both dialog presets and continuation from previous states.</p>
 */
public class InitialConditionSolver {

    private final SolverType solverType;

    /**
     * Constructs an InitialConditionSolver with the specified solver type.
     *
     * @param solverType the numerical integration method
     */
    public InitialConditionSolver(SolverType solverType) {
        this.solverType = solverType;
    }

    /**
     * Sets initial conditions for the simulation.
     *
     * Initializes node potentials (pALT) and component currents (iALT) based on:
     * - Capacitor initial voltages from netlist parameters
     * - Inductor initial currents
     * - Voltage and current source parameters
     *
     * @param matrixSolver the matrix solver containing state arrays to initialize
     * @param netlist the circuit netlist interface
     * @param solverType the solver type for coefficient calculations
     * @throws IllegalStateException if initialization fails
     */
    public void setInitialConditions(
        MnaSolver matrixSolver,
        INetList netlist,
        SolverType solverType
    ) {
        if (matrixSolver == null) {
            throw new IllegalStateException("MnaSolver cannot be null");
        }
        if (netlist == null) {
            throw new IllegalStateException("INetList cannot be null");
        }

        // Initialize all potential arrays to zero
        initializePotentials(matrixSolver);

        // Initialize all current arrays to zero
        initializeCurrents(matrixSolver, netlist);

        // Restore saved potentials from netlist parameters (for CONTINUE mode)
        restorePotentialsFromNetlist(matrixSolver, netlist);

        // Restore saved currents from netlist parameters
        restoreCurrentsFromNetlist(matrixSolver, netlist);
    }

    /**
     * Initializes all potential arrays to zero.
     *
     * @param matrixSolver the matrix solver
     */
    private void initializePotentials(MnaSolver matrixSolver) {
        double[] pALT = matrixSolver.getPALT();
        double[] pALTALT = matrixSolver.getPALTALT();
        double[] pALTALTALT = matrixSolver.getPALTALTALT();

        for (int i = 0; i < pALT.length; i++) {
            pALT[i] = 0;
            pALTALT[i] = 0;
            pALTALTALT[i] = 0;
        }
    }

    /**
     * Initializes all current arrays to zero.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void initializeCurrents(MnaSolver matrixSolver, INetList netlist) {
        double[] iALT = matrixSolver.getIALT();
        double[] iALTALT = matrixSolver.getIALTALT();
        double[] iALTALTALT = matrixSolver.getIALTALTALT();

        for (int i = 0; i < iALT.length; i++) {
            iALT[i] = 0;
            iALTALT[i] = 0;
            iALTALTALT[i] = 0;
        }
    }

    /**
     * Restores potentials from netlist parameters (saved initial conditions).
     *
     * Handles capacitor voltages and other potential sources.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void restorePotentialsFromNetlist(MnaSolver matrixSolver, INetList netlist) {
        double[] pALT = matrixSolver.getPALT();
        double[] pALTALT = matrixSolver.getPALTALT();
        double[] pALTALTALT = matrixSolver.getPALTALTALT();

        for (int i = 0; i < netlist.getElementCount(); i++) {
            CircuitTypCore type = netlist.getType(i);
            int nodeX = netlist.getNodeX(i);
            int nodeY = netlist.getNodeY(i);
            double[] params = netlist.getParameter(i);

            if (type == CircuitTypCore.LK_C || type == CircuitTypCore.TH_CTH) {
                // Capacitor: dialog initial voltage is in param[1], saved state in param[4]/[5]
                double u0 = (params.length > CapacitorParameters.INDEX_INITIAL_VOLTAGE && Double.isFinite(params[CapacitorParameters.INDEX_INITIAL_VOLTAGE]))
                        ? params[CapacitorParameters.INDEX_INITIAL_VOLTAGE] : (stateSlot(params, CapacitorParameters.INDEX_SAVED_VX) - stateSlot(params, CapacitorParameters.INDEX_SAVED_VY));
                if (nodeY == 0 && nodeX < pALT.length) {
                    pALT[nodeX] = u0;
                } else if (nodeX == 0 && nodeY < pALT.length) {
                    pALT[nodeY] = -u0;
                } else if (nodeX < pALT.length && nodeY < pALT.length) {
                    pALT[nodeX] = u0;
                    pALT[nodeY] = 0.0;
                }
            } else if (type == CircuitTypCore.REL_MMF || type == CircuitTypCore.TH_TEMP) {
                // MMF/Temperature source: restore potentials
                // param[8] = voltage at X node, param[9] = voltage at Y node
                if (nodeX < pALT.length) pALT[nodeX] = stateSlot(params, SourceParameters.INDEX_SAVED_VX);
                if (nodeY < pALT.length) pALT[nodeY] = stateSlot(params, SourceParameters.INDEX_SAVED_VY);
            } else if (type == CircuitTypCore.LK_LKOP2) {
                // Coupled inductor: restore source current in potential vector
                // param[2] = source current
                int voltageSourceNumber = netlist.getVoltageSourceNumber(i);
                if (voltageSourceNumber > 0 && netlist.getNodeMax() + voltageSourceNumber < pALT.length) {
                    pALT[netlist.getNodeMax() + voltageSourceNumber] = stateSlot(params, InductorParameters.INDEX_SAVED_CURRENT);
                }
            }
        }
        if (pALTALT != null && pALTALT.length >= pALT.length) {
            System.arraycopy(pALT, 0, pALTALT, 0, pALT.length);
        }
        if (pALTALTALT != null && pALTALTALT.length >= pALT.length) {
            System.arraycopy(pALT, 0, pALTALTALT, 0, pALT.length);
        }
        if (matrixSolver.getP() != null && matrixSolver.getP().length >= pALT.length) {
            System.arraycopy(pALT, 0, matrixSolver.getP(), 0, pALT.length);
        }
    }

    /**
     * Saved-state slot of a netlist parameter array. Files written by the
     * classic GUI always carry the full state array, but web-authored
     * circuits store short parameter arrays - the missing/NaN slots mean
     * "no saved state" and must default to 0, never to NaN (a single NaN
     * here poisons the whole solution).
     */
    private static double stateSlot(double[] params, int index) {
        return index < params.length && Double.isFinite(params[index]) ? params[index] : 0.0;
    }

    /**
     * Restores currents from netlist parameters (saved initial conditions).
     *
     * Handles inductor currents, current sources, and coupled inductors.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void restoreCurrentsFromNetlist(MnaSolver matrixSolver, INetList netlist) {
        double[] iALT = matrixSolver.getIALT();
        double[] iALTALT = matrixSolver.getIALTALT();
        double[] iALTALTALT = matrixSolver.getIALTALTALT();

        for (int i = 0; i < netlist.getElementCount(); i++) {
            CircuitTypCore type = netlist.getType(i);
            double[] params = netlist.getParameter(i);

            if (type == CircuitTypCore.LK_C || type == CircuitTypCore.TH_CTH) {
                // Capacitor: restore initial current
                iALT[i] = stateSlot(params, CapacitorParameters.INDEX_SAVED_CURRENT);
            } else if (type == CircuitTypCore.LK_L || type == CircuitTypCore.NONLIN_REL) {
                // Inductor: dialog initial current is in param[1], saved current in param[2]
                double i0 = (params.length > InductorParameters.INDEX_INITIAL_CURRENT && Double.isFinite(params[InductorParameters.INDEX_INITIAL_CURRENT]))
                        ? params[InductorParameters.INDEX_INITIAL_CURRENT] : stateSlot(params, InductorParameters.INDEX_SAVED_CURRENT);
                iALT[i] = i0;
                iALTALT[i] = i0;
                if (iALTALTALT != null && i < iALTALTALT.length) {
                    iALTALTALT[i] = i0;
                }
            } else if (type == CircuitTypCore.LK_LKOP2) {
                // Coupled inductor: restore initial current
                double i0 = (params.length > InductorParameters.INDEX_INITIAL_CURRENT && Double.isFinite(params[InductorParameters.INDEX_INITIAL_CURRENT]))
                        ? params[InductorParameters.INDEX_INITIAL_CURRENT] : stateSlot(params, InductorParameters.INDEX_SAVED_CURRENT);
                iALT[i] = i0;
                iALTALT[i] = i0;
                if (iALTALTALT != null && i < iALTALTALT.length) {
                    iALTALTALT[i] = i0;
                }
            } else if (type == CircuitTypCore.LK_I || type == CircuitTypCore.TH_FLOW) {
                // Current source: restore initial current
                double i0 = (params.length > SourceParameters.INDEX_VALUE_DC && Double.isFinite(params[SourceParameters.INDEX_VALUE_DC]))
                        ? params[SourceParameters.INDEX_VALUE_DC] : stateSlot(params, SourceParameters.INDEX_SAVED_CURRENT);
                iALT[i] = i0;
            }
        }
    }

    /**
     * Calculates the inductance coefficient for matrix stamping.
     *
     * Extracted from LKMatrices.getAWForInductance (lines 1504-1527).
     * Calculates the coefficient that relates inductance to matrix elements
     * for different numerical integration methods.
     *
     * @param inductance the inductance value
     * @param timeStep the simulation time step (dt)
     * @return the calculated coefficient aW for matrix stamping
     */
    public double getAWForInductance(double inductance, double timeStep) {
        final double effectiveL = Math.max(inductance, SolverConstants.FAST_NULL_L);
        return switch (solverType) {
            case SOLVER_BE -> timeStep / effectiveL;
            case SOLVER_TRZ -> SolverConstants.TRAPEZOIDAL_INTEGRATION_FACTOR * timeStep / effectiveL;
            case SOLVER_GS -> SolverConstants.GEAR_SHICHMAN_MAIN_COEFF * (timeStep / effectiveL);
            default -> timeStep / effectiveL;
        };
    }
}
