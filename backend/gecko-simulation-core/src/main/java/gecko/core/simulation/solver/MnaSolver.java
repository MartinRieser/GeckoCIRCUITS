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
import gecko.core.circuit.netlist.INetList;

import java.util.List;

/**
 * Solver abstraction for MNA (Modified Nodal Analysis) circuit simulations.
 *
 * <p>Decouples the simulation engine and the component-current calculations
 * from the concrete linear-algebra backend. The dense reference
 * implementation is {@link MatrixSolver}; the sparse drop-in is
 * {@code SparseMatrixSolver}. Both share the same stamping orchestration and
 * differ only in how the system matrix is stored and factorized.
 *
 * <p>Per step, the engine drives a solver instance through:
 * <ol>
 *   <li>{@link #initializeMatrices(int, int, int)} once before the loop,</li>
 *   <li>{@link #buildMatrixA(INetList, double, double, boolean)} and
 *       {@link #buildVectorB(INetList, double, double, boolean)} per attempt,</li>
 *   <li>{@link #solve()} to obtain the node potentials in {@link #getP()},</li>
 *   <li>{@link #updateNodePotentials(double, double)} once the step is accepted,
 *       shifting the history vectors for the multi-step integration methods.</li>
 * </ol>
 *
 * @see MatrixSolver
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public interface MnaSolver {

    /**
     * Initializes all matrices and vectors for MNA simulation.
     *
     * @param nodeCount the number of nodes in the circuit
     * @param voltageSourceCount the number of voltage sources
     * @param elementCount the total number of circuit elements
     */
    void initializeMatrices(int nodeCount, int voltageSourceCount, int elementCount);

    /**
     * Builds the system matrix A from the netlist's component stamps.
     *
     * @param netlist the circuit netlist containing components and topology
     * @param dt the time step size (used for dynamic components like capacitors)
     * @param time the current simulation time
     * @param capacitorError flag reserved for capacitor error correction
     */
    void buildMatrixA(INetList netlist, double dt, double time, boolean capacitorError);

    /**
     * Builds the right-hand side vector b for the MNA system (Ax=b).
     *
     * @param netlist the circuit netlist containing component types, node connections, and parameters
     * @param dt the time step (integration time interval in seconds)
     * @param time the current simulation time (used for time-dependent sources)
     * @param capacitorError flag for capacitor error correction (reserved for future use)
     */
    void buildVectorB(INetList netlist, double dt, double time, boolean capacitorError);

    /**
     * Solves the linear system Ax=b for the node potentials.
     *
     * @throws IllegalStateException if matrices have not been initialized
     * @throws RuntimeException if the system matrix A is singular (no unique solution)
     */
    void solve();

    /**
     * Marks the system matrix as changed, forcing a refactorization on the
     * next {@link #solve()} call.
     */
    void setMatrixChanged();

    /**
     * Shifts the node-potential and component-current history vectors by one
     * step, promoting the current solution into the one-step-back slot.
     *
     * @param dt the time step size (seconds)
     * @param time the current simulation time (seconds)
     */
    void updateNodePotentials(double dt, double time);

    /**
     * Gets the matrix order (node count + voltage source count + 1).
     *
     * @return matrix size N of the MNA system
     */
    int getMatrixSize();

    /**
     * Gets the numerical integration method this solver stamps for.
     *
     * @return the solver type (Backward Euler, Trapezoidal, Gear-Shichman)
     */
    SolverType getSolverType();

    /**
     * Gets the current node potentials (solution of the last {@link #solve()}).
     *
     * @return node potential array indexed by node number (0 = ground reference)
     */
    double[] getP();

    /**
     * Gets the one-step-back node potentials.
     *
     * @return previous time-step node potentials
     */
    double[] getPALT();

    /**
     * Gets the two-steps-back node potentials.
     *
     * @return node potentials from two steps back
     */
    double[] getPALTALT();

    /**
     * Gets the three-steps-back node potentials.
     *
     * @return node potentials from three steps back
     */
    double[] getPALTALTALT();

    /**
     * Gets the one-step-back component currents.
     *
     * @return component currents of the previous time step
     */
    double[] getIALT();

    /**
     * Gets the current-step component currents, filled by
     * {@link ComponentCurrentCalculator}; the next
     * {@link #updateNodePotentials(double, double)} promotes them into
     * {@link #getIALT()}.
     *
     * @return current-step component currents
     */
    double[] getICurrent();

    /**
     * Gets the two-steps-back component currents.
     *
     * @return component currents from two steps back
     */
    double[] getIALTALT();

    /**
     * Gets the three-steps-back component currents.
     *
     * @return component currents from three steps back
     */
    double[] getIALTALTALT();

    /**
     * Warnings for elements skipped during stamping (no stamper for their type).
     *
     * @return list of skip warnings collected since initialization
     */
    List<String> getSkippedElementWarnings();
}
