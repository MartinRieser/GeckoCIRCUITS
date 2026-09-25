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
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.parameters.SourceParameters;
import gecko.core.simulation.MatrixSolverKind;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.simulation.solver.MnaSolverFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the sparse MNA solver reproduces the dense reference solver
 * on hand-built netlists: resistive dividers with a z-slot voltage source,
 * dynamic (L/C) elements with history, island pinning, factorization caching,
 * and the AUTO backend selection threshold.
 */
class SparseMatrixSolverTest {

    /** Numeric tolerance of the dense-vs-sparse parity checks. */
    private static final double PARITY_TOLERANCE = 1e-9;

    /** Supply voltage of the divider netlist. */
    private static final double SUPPLY_VOLTAGE = 10.0;

    /** Divider resistances. */
    private static final double R1 = 100.0;
    private static final double R2 = 300.0;

    /** Dynamic-element values for the L/C parity netlist. */
    private static final double INDUCTANCE = 1e-3;
    private static final double CAPACITANCE = 1e-6;
    private static final double TIME_STEP = 1e-6;

    /**
     * Divider netlist: source (node 1 - 0), R1 (1 - 2), R2 (2 - 0).
     * One voltage source consumes the z-slot (nodeMax + 1).
     */
    private static CircuitNetlist dividerNetlist() {
        final CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(
                new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_R},
                new int[]{1, 1, 2},
                new int[]{0, 2, 0},
                new int[]{1, -1, -1},
                new double[][]{
                        {SourceType.QUELLE_DC_NEW, SUPPLY_VOLTAGE},
                        {R1},
                        {R2}},
                2, 1, 3);
        return netlist;
    }

    private static double[] solveWith(final gecko.core.simulation.solver.MnaSolver solver,
                                      final CircuitNetlist netlist) {
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(),
                netlist.getElementCount());
        solver.buildMatrixA(netlist, TIME_STEP, 0.0, false);
        solver.buildVectorB(netlist, TIME_STEP, 0.0, false);
        solver.solve();
        return solver.getP().clone();
    }

    @Test
    void sparseMatchesDense_onResistiveDividerWithVoltageSource() {
        final double[] dense = solveWith(new MatrixSolver(SolverType.SOLVER_BE), dividerNetlist());
        final double[] sparse = solveWith(new SparseMatrixSolver(SolverType.SOLVER_BE), dividerNetlist());

        assertArrayEquals(dense, sparse, PARITY_TOLERANCE, "sparse must match dense solution");

        // Physics: v1 = supply, v2 = supply * R2 / (R1 + R2)
        assertEquals(SUPPLY_VOLTAGE, dense[1], PARITY_TOLERANCE);
        assertEquals(SUPPLY_VOLTAGE * R2 / (R1 + R2), dense[2], PARITY_TOLERANCE);
    }

    @Test
    void sparseMatchesDense_withDynamicElementsAndHistory() {
        final CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(
                new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_L, CircuitTypCore.LK_C},
                new int[]{1, 1, 2},
                new int[]{0, 2, 0},
                new int[]{1, -1, -1},
                new double[][]{
                        {SourceType.QUELLE_DC_NEW, SUPPLY_VOLTAGE},
                        {INDUCTANCE},
                        {CAPACITANCE}},
                2, 1, 3);

        final MatrixSolver dense = new MatrixSolver(SolverType.SOLVER_TRZ);
        final SparseMatrixSolver sparse = new SparseMatrixSolver(SolverType.SOLVER_TRZ);

        // Two steps with a history shift between them exercise the companion terms
        for (int step = 0; step < 2; step++) {
            final double time = step * TIME_STEP;
            final double[] denseP = solveWith(dense, netlist);
            final double[] sparseP = solveWith(sparse, netlist);
            if (step == 1) {
                assertArrayEquals(denseP, sparseP, PARITY_TOLERANCE,
                        "sparse must match dense on the post-history step");
            }
            dense.updateNodePotentials(TIME_STEP, time);
            sparse.updateNodePotentials(TIME_STEP, time);
        }
    }

    @Test
    void sparseMatchesDense_withIslandPinning() {
        // Floating island (nodes 1-2 with R3 only, no connection to ground)
        final CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(
                new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                        CircuitTypCore.LK_R, CircuitTypCore.LK_R},
                new int[]{1, 1, 2, 3},
                new int[]{0, 2, 0, 4},
                new int[]{1, -1, -1, -1},
                new double[][]{
                        {SourceType.QUELLE_DC_NEW, SUPPLY_VOLTAGE},
                        {R1}, {R2}, {50.0}},
                4, 1, 4);
        netlist.setSingularityEntries(new int[]{0, 4});

        final double[] dense = solveWith(new MatrixSolver(SolverType.SOLVER_BE), netlist);
        final double[] sparse = solveWith(new SparseMatrixSolver(SolverType.SOLVER_BE), netlist);

        assertArrayEquals(dense, sparse, PARITY_TOLERANCE, "island pinning must match");
        assertEquals(0.0, dense[4], PARITY_TOLERANCE, "island reference potential pinned to zero");
    }

    @Test
    void factorizationCaching_reusesFactorsUntilMatrixChanges() {
        final SparseMatrixSolver sparse = new SparseMatrixSolver(SolverType.SOLVER_BE);
        final CircuitNetlist netlist = dividerNetlist();
        solveWith(sparse, netlist);

        // Solve again without rebuilding: cached factorization gives the same result
        sparse.buildVectorB(netlist, TIME_STEP, 0.0, false);
        sparse.solve();
        assertEquals(SUPPLY_VOLTAGE, sparse.getP()[1], PARITY_TOLERANCE);
    }

    @Test
    void autoKind_selectsSparseAboveThresholdAndDenseBelow() {
        assertInstanceOf(MatrixSolver.class,
                MnaSolverFactory.create(SolverType.SOLVER_BE, MatrixSolverKind.AUTO,
                        MnaSolverFactory.SPARSE_AUTO_SIZE_THRESHOLD - 1),
                "below the threshold AUTO must stay dense");
        assertInstanceOf(SparseMatrixSolver.class,
                MnaSolverFactory.create(SolverType.SOLVER_BE, MatrixSolverKind.AUTO,
                        MnaSolverFactory.SPARSE_AUTO_SIZE_THRESHOLD),
                "at the threshold AUTO must switch to sparse");
        assertInstanceOf(SparseMatrixSolver.class,
                MnaSolverFactory.create(SolverType.SOLVER_BE, MatrixSolverKind.SPARSE, 3));
        assertInstanceOf(MatrixSolver.class,
                MnaSolverFactory.create(SolverType.SOLVER_BE, MatrixSolverKind.DENSE, 100));
    }

    @Test
    void sparseSolver_reportsNoDenseStorage() {
        final SparseMatrixSolver sparse = new SparseMatrixSolver(SolverType.SOLVER_BE);
        sparse.initializeMatrices(2, 1, 1);
        assertEquals(4, sparse.getMatrixSize());
        assertTrue(sparse.getSystemMatrix() == null,
                "sparse backend must not allocate dense N x N storage");
    }
}
