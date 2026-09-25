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

import gecko.core.math.Matrix;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the sparse LU factorization with partial pivoting against the dense
 * reference solver on structured and random systems, including duplicate
 * triplet summation, pivoting cases, singular detection, and small sizes.
 */
class SparseLUTest {

    /** Numeric tolerance versus the dense reference solution. */
    private static final double SOLUTION_TOLERANCE = 1e-9;

    /** Order of the random test systems. */
    private static final int RANDOM_SYSTEM_SIZE = 40;

    /** Entries per row of the random sparse pattern. */
    private static final int ENTRIES_PER_ROW = 4;

    /** Diagonal dominance shift keeping the random systems non-singular. */
    private static final double DIAGONAL_DOMINANCE = (double) RANDOM_SYSTEM_SIZE;

    private static double[] solveDense(final SparseMatrix matrix, final double[] b) {
        final Matrix dense = new Matrix(matrix.toDense(), matrix.getSize(), matrix.getSize());
        final double[][] bColumn = new double[matrix.getSize()][1];
        for (int i = 0; i < b.length; i++) {
            bColumn[i][0] = b[i];
        }
        final double[][] x = dense.solve(new Matrix(bColumn)).getArray();
        final double[] out = new double[matrix.getSize()];
        for (int i = 0; i < out.length; i++) {
            out[i] = x[i][0];
        }
        return out;
    }

    private static double[] solveSparse(final SparseMatrix matrix, final double[] b) {
        final double[] out = new double[matrix.getSize()];
        SparseLU.factorize(matrix).solve(b, out);
        return out;
    }

    private static void assertSolutionsMatch(final SparseMatrix matrix, final double[] b) {
        final double[] dense = solveDense(matrix, b);
        final double[] sparse = solveSparse(matrix, b);
        assertArrayEquals(dense, sparse, SOLUTION_TOLERANCE,
                "sparse LU must reproduce the dense reference solution");
    }

    /** Deterministic, diagonally dominant sparse system with a known pattern. */
    private static SparseMatrix randomDominantSystem(final long seed) {
        final Random random = new Random(seed);
        final TripletMatrixAccumulator triplets = new TripletMatrixAccumulator(RANDOM_SYSTEM_SIZE);
        for (int row = 0; row < RANDOM_SYSTEM_SIZE; row++) {
            triplets.add(row, row, DIAGONAL_DOMINANCE + random.nextDouble());
            for (int e = 0; e < ENTRIES_PER_ROW; e++) {
                final int col = random.nextInt(RANDOM_SYSTEM_SIZE);
                if (col != row) {
                    triplets.add(row, col, random.nextDouble());
                }
            }
        }
        return SparseMatrix.fromTriplets(RANDOM_SYSTEM_SIZE, triplets);
    }

    @Test
    void factorizeAndSolve_matchesDenseReferenceOnRandomSystem() {
        final SparseMatrix matrix = randomDominantSystem(42L);
        final double[] b = new double[RANDOM_SYSTEM_SIZE];
        for (int i = 0; i < b.length; i++) {
            b[i] = 1.0 + i;
        }
        assertSolutionsMatch(matrix, b);
    }

    @Test
    void duplicateTriplets_areSummedLikeDenseAccumulation() {
        final TripletMatrixAccumulator triplets = new TripletMatrixAccumulator(2);
        triplets.add(0, 0, 1.0);
        triplets.add(0, 0, 2.0);
        triplets.add(1, 0, 3.0);
        triplets.add(0, 1, 4.0);

        final double[][] dense = SparseMatrix.fromTriplets(2, triplets).toDense();

        assertEquals(3.0, dense[0][0], 0.0, "duplicate adds must accumulate");
        assertEquals(3.0, dense[1][0], 0.0);
        assertEquals(4.0, dense[0][1], 0.0);
        assertEquals(0.0, dense[1][1], 0.0);
        assertEquals(3, SparseMatrix.fromTriplets(2, triplets).getNonZeroCount(),
                "storage keeps one entry per summed position (explicit zero stays)");
    }

    @Test
    void partialPivoting_permutesRowsToAvoidZeroPivot() {
        // Zero diagonal forces a row interchange during factorization
        final TripletMatrixAccumulator triplets = new TripletMatrixAccumulator(2);
        triplets.add(0, 1, 2.0);
        triplets.add(1, 0, 3.0);
        triplets.add(1, 1, 1.0);
        final SparseMatrix matrix = SparseMatrix.fromTriplets(2, triplets);

        assertSolutionsMatch(matrix, new double[]{2.0, 4.0});
    }

    @Test
    void singularMatrix_throwsRuntimeException() {
        final TripletMatrixAccumulator triplets = new TripletMatrixAccumulator(2);
        triplets.add(0, 0, 1.0);
        triplets.add(1, 0, 2.0);
        // Column 1 entirely zero -> singular
        final SparseMatrix matrix = SparseMatrix.fromTriplets(2, triplets);

        assertThrows(RuntimeException.class, () -> SparseLU.factorize(matrix));
    }

    @Test
    void minimalSizes_solveExactly() {
        final TripletMatrixAccumulator one = new TripletMatrixAccumulator(1);
        one.add(0, 0, 2.0);
        final double[] x1 = new double[1];
        SparseLU.factorize(SparseMatrix.fromTriplets(1, one)).solve(new double[]{4.0}, x1);
        assertEquals(2.0, x1[0], SOLUTION_TOLERANCE);

        final TripletMatrixAccumulator two = new TripletMatrixAccumulator(2);
        two.add(0, 0, 4.0);
        two.add(0, 1, 1.0);
        two.add(1, 0, 1.0);
        two.add(1, 1, 3.0);
        assertSolutionsMatch(SparseMatrix.fromTriplets(2, two), new double[]{1.0, 2.0});
    }

    @Test
    void repeatedSolves_reuseFactorization() {
        final SparseMatrix matrix = randomDominantSystem(7L);
        final SparseLU factorization = SparseLU.factorize(matrix);

        final double[] x1 = new double[RANDOM_SYSTEM_SIZE];
        final double[] x2 = new double[RANDOM_SYSTEM_SIZE];
        factorization.solve(new double[RANDOM_SYSTEM_SIZE], x1);
        final double[] b2 = new double[RANDOM_SYSTEM_SIZE];
        b2[0] = 5.0;
        factorization.solve(b2, x2);

        final double[] reference1 = solveDense(matrix, new double[RANDOM_SYSTEM_SIZE]);
        final double[] reference2 = solveDense(matrix, b2);
        assertArrayEquals(reference1, x1, SOLUTION_TOLERANCE);
        assertArrayEquals(reference2, x2, SOLUTION_TOLERANCE);
    }

    @Test
    void cscConversion_rowSortsColumnsAndCountsNonZeros() {
        final TripletMatrixAccumulator triplets = new TripletMatrixAccumulator(3);
        triplets.add(2, 1, 1.0);
        triplets.add(0, 1, 2.0);
        triplets.add(1, 0, 3.0);
        final SparseMatrix matrix = SparseMatrix.fromTriplets(3, triplets);

        assertEquals(3, matrix.getSize());
        assertEquals(3, matrix.getNonZeroCount());
        // Column 0: row 1; column 1: rows 0 and 2 (row-sorted); column 2: empty
        assertArrayEquals(new int[]{0, 1, 3, 3}, matrix.getColumnPointers());
        assertEquals(1, matrix.getRowIndices()[0]);
        assertEquals(0, matrix.getRowIndices()[1]);
        assertEquals(2, matrix.getRowIndices()[2]);
        assertTrue(matrix.toDense()[2][1] == 1.0);
    }
}
