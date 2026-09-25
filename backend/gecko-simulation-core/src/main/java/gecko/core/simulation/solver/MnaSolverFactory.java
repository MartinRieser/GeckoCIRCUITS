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
import gecko.core.simulation.MatrixSolverKind;
import gecko.core.simulation.solver.sparse.SparseMatrixSolver;

/**
 * Factory selecting the MNA solver backend for a simulation run.
 *
 * @see MnaSolver
 * @see MatrixSolver
 * @see gecko.core.simulation.solver.sparse.SparseMatrixSolver
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public final class MnaSolverFactory {

    /** Matrix order at or above which the AUTO kind selects the sparse backend. */
    public static final int SPARSE_AUTO_SIZE_THRESHOLD = 50;

    private MnaSolverFactory() {
    }

    /**
     * Creates an MNA solver for the given backend selection.
     *
     * @param solverType the numerical integration method
     * @param kind dense/sparse backend selection; AUTO resolves against
     *             {@code expectedMatrixSize}
     * @param expectedMatrixSize expected matrix order (node count + voltage
     *                           source count + 1); only read for AUTO
     * @return the selected solver instance
     */
    public static MnaSolver create(final SolverType solverType, final MatrixSolverKind kind,
                                   final int expectedMatrixSize) {
        return resolveKind(kind, expectedMatrixSize) == MatrixSolverKind.SPARSE
                ? new SparseMatrixSolver(solverType)
                : new MatrixSolver(solverType);
    }

    /**
     * Creates a dense MNA solver (reference backend, also used for shadow
     * evaluations).
     *
     * @param solverType the numerical integration method
     * @return dense solver instance
     */
    public static MnaSolver createDense(final SolverType solverType) {
        return new MatrixSolver(solverType);
    }

    /**
     * Resolves the AUTO kind against the expected matrix size.
     *
     * @param kind requested kind
     * @param expectedMatrixSize expected matrix order
     * @return the concrete backend kind
     */
    private static MatrixSolverKind resolveKind(final MatrixSolverKind kind,
                                                final int expectedMatrixSize) {
        if (kind == MatrixSolverKind.AUTO) {
            return expectedMatrixSize >= SPARSE_AUTO_SIZE_THRESHOLD
                    ? MatrixSolverKind.SPARSE : MatrixSolverKind.DENSE;
        }
        return kind;
    }
}
