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
package gecko.core.simulation.solver.sparse;

import gecko.core.allg.SolverType;
import gecko.core.circuit.matrix.MatrixAccumulator;
import gecko.core.simulation.solver.MatrixSolver;

/**
 * Sparse MNA solver: drop-in {@link MatrixSolver} subclass storing the system
 * matrix in compressed sparse column form and factorizing with
 * {@link SparseLU} (partial pivoting).
 *
 * <p>All stamping orchestration, the b-vector, and the history-vector
 * handling are inherited unchanged; only the matrix storage (triplet
 * accumulator instead of a dense {@code N x N} array) and the
 * factorization/substitution steps are overridden. This removes the O(N³)
 * dense factorization and the O(N²) dense storage for large power
 * electronic networks.
 *
 * @see MatrixSolver
 * @see SparseLU
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public class SparseMatrixSolver extends MatrixSolver {

    /** Accumulator collecting this build's stamps as triplets. */
    private TripletMatrixAccumulator tripletAccumulator;

    /** Factorization of the current system matrix. */
    private SparseLU factorization;

    /**
     * Constructs a sparse matrix solver with the given solver type.
     *
     * @param solverType the numerical integration method (e.g., Backward Euler)
     */
    public SparseMatrixSolver(final SolverType solverType) {
        super(solverType);
    }

    @Override
    protected double[][] createSystemMatrixStorage(final int size) {
        // No dense storage: the matrix lives in the triplet accumulator
        return null;
    }

    @Override
    protected MatrixAccumulator createMatrixAccumulator(final int size) {
        tripletAccumulator = new TripletMatrixAccumulator(size);
        return tripletAccumulator;
    }

    @Override
    protected void factorize() {
        factorization = SparseLU.factorize(
                SparseMatrix.fromTriplets(getMatrixSize(), tripletAccumulator));
    }

    @Override
    protected void substitute() {
        factorization.solve(getb(), getP());
    }
}
