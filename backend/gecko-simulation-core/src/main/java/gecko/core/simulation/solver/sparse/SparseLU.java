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

/**
 * Sparse LU factorization with partial pivoting for square CSC matrices.
 *
 * <p>Left-looking (IKJ) elimination: column k of U and L is computed by
 * propagating the scattered A(:,k) through the previously finished L columns,
 * then a partial pivot selects the largest-magnitude candidate among the
 * remaining rows. Row interchanges are bookkept as
 * {@code PA = L U} with L unit lower triangular (implicit unit diagonal):
 * <ul>
 *   <li>the permutation {@code rowOfPermutedRow} maps a permuted equation
 *       position to its original row, and</li>
 *   <li>every swap exchanges the row labels k/ip in all stored L and U
 *       columns, exactly like the row interchange of a dense LU panel.</li>
 * </ul>
 *
 * <p>Factorization cost is O(N²) loop overhead plus work proportional to the
 * filled-in non-zeros, versus O(N³) for the dense factorization - the win the
 * sparse MNA path is built for. No fill-reducing column ordering is applied;
 * circuit MNA matrices are narrow-banded enough that plain partial pivoting
 * keeps fill moderate while remaining simple and exact.
 *
 * <p>Solves: forward substitution {@code L y = P b}, back substitution
 * {@code U x = y}. Pivoting permutes the equations only, so the solution is
 * already in original unknown coordinates.
 *
 * @see SparseMatrix
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public final class SparseLU {

    /** Initial capacity per stored L/U column. */
    private static final int INITIAL_COLUMN_CAPACITY = 4;

    private final int size;

    /** L columns below the diagonal (unit diagonal implicit). */
    private final int[][] lowerRows;
    private final double[][] lowerValues;

    /** U columns including the diagonal pivot. */
    private final int[][] upperRows;
    private final double[][] upperValues;

    /** Entry counts per stored column. */
    private final int[] lowerCount;
    private final int[] upperCount;

    /** rowOfPermutedRow[permutedPosition] = original row (equation order). */
    private final int[] rowOfPermutedRow;

    private SparseLU(final int size, final int[][] lowerRows, final double[][] lowerValues,
                     final int[][] upperRows, final double[][] upperValues,
                     final int[] lowerCount, final int[] upperCount,
                     final int[] rowOfPermutedRow) {
        this.size = size;
        this.lowerRows = lowerRows;
        this.lowerValues = lowerValues;
        this.upperRows = upperRows;
        this.upperValues = upperValues;
        this.lowerCount = lowerCount;
        this.upperCount = upperCount;
        this.rowOfPermutedRow = rowOfPermutedRow;
    }

    /**
     * Factorizes the given CSC matrix with partial pivoting.
     *
     * @param matrix CSC matrix to factorize
     * @return LU factorization supporting repeated solves
     *
     * @throws RuntimeException if the matrix is singular (a pivot column has
     *                          no non-zero candidate)
     */
    public static SparseLU factorize(final SparseMatrix matrix) {
        final int n = matrix.getSize();
        final int[] columnPointers = matrix.getColumnPointers();
        final int[] rowIndices = matrix.getRowIndices();
        final double[] values = matrix.getValues();

        final Column[] lower = new Column[n];
        final Column[] upper = new Column[n];

        // Dense work vector for the column being eliminated, indexed by
        // permuted row position
        final double[] work = new double[n];
        final boolean[] marked = new boolean[n];
        final int[] markedRows = new int[n];

        final int[] permutedRowOfRow = new int[n];
        final int[] rowOfPermutedRow = new int[n];
        for (int i = 0; i < n; i++) {
            permutedRowOfRow[i] = i;
            rowOfPermutedRow[i] = i;
        }

        for (int k = 0; k < n; k++) {
            // Scatter A(:,k) into the work vector (original rows -> permuted)
            int markedCount = 0;
            for (int i = columnPointers[k]; i < columnPointers[k + 1]; i++) {
                final int permuted = permutedRowOfRow[rowIndices[i]];
                if (!marked[permuted]) {
                    marked[permuted] = true;
                    markedRows[markedCount++] = permuted;
                }
                work[permuted] += values[i];
            }

            // Left-looking update: propagate through finished L columns
            for (int i = 0; i < k; i++) {
                final double uIk = work[i];
                if (uIk == 0.0) {
                    continue;
                }
                final Column lowerColumn = lower[i];
                for (int e = 0; e < lowerColumn.count; e++) {
                    final int j = lowerColumn.rows[e];
                    if (!marked[j]) {
                        marked[j] = true;
                        markedRows[markedCount++] = j;
                    }
                    work[j] -= lowerColumn.values[e] * uIk;
                }
            }

            // Partial pivot: largest candidate among rows k..n-1
            int pivotPosition = -1;
            double pivotMagnitude = 0.0;
            for (int position = k; position < n; position++) {
                final double magnitude = Math.abs(work[position]);
                if (magnitude > pivotMagnitude) {
                    pivotMagnitude = magnitude;
                    pivotPosition = position;
                }
            }
            if (pivotPosition < 0 || pivotMagnitude == 0.0) {
                throw new RuntimeException(
                        "Matrix is singular: no non-zero pivot in column " + k);
            }

            // Row interchange k <-> pivotPosition: perm maps, work values,
            // marked flags, and the row labels of all stored L/U entries
            if (pivotPosition != k) {
                swapRows(k, pivotPosition, work, marked, markedRows,
                         permutedRowOfRow, rowOfPermutedRow, lower, upper);
            }

            final double pivot = work[k];

            // Store U column k (rows 0..k) and L column k (rows k+1..n-1)
            final Column uColumn = new Column();
            for (int position = 0; position <= k; position++) {
                if (work[position] != 0.0) {
                    uColumn.add(position, work[position]);
                }
                work[position] = 0.0;
                marked[position] = false;
            }
            upper[k] = uColumn;

            final Column lColumn = new Column();
            for (int position = k + 1; position < n; position++) {
                if (work[position] != 0.0) {
                    lColumn.add(position, work[position] / pivot);
                }
                work[position] = 0.0;
                marked[position] = false;
            }
            lower[k] = lColumn;
        }

        return new SparseLU(n, extractRows(lower), extractValues(lower), extractRows(upper),
                extractValues(upper), extractCounts(lower), extractCounts(upper),
                rowOfPermutedRow);
    }

    /**
     * Swaps two permuted rows during pivoting: work values, marked flags,
     * permutation bookkeeping, and the row labels of all stored L/U columns.
     */
    private static void swapRows(final int positionA, final int positionB, final double[] work,
                                 final boolean[] marked, final int[] markedRows,
                                 final int[] permutedRowOfRow, final int[] rowOfPermutedRow,
                                 final Column[] lower, final Column[] upper) {
        double value = work[positionA];
        work[positionA] = work[positionB];
        work[positionB] = value;

        boolean flag = marked[positionA];
        marked[positionA] = marked[positionB];
        marked[positionB] = flag;

        for (int m = 0; m < markedRows.length; m++) {
            if (markedRows[m] == positionA) {
                markedRows[m] = positionB;
            } else if (markedRows[m] == positionB) {
                markedRows[m] = positionA;
            }
        }

        final int originalA = rowOfPermutedRow[positionA];
        final int originalB = rowOfPermutedRow[positionB];
        rowOfPermutedRow[positionA] = originalB;
        rowOfPermutedRow[positionB] = originalA;
        permutedRowOfRow[originalA] = positionB;
        permutedRowOfRow[originalB] = positionA;

        for (Column column : lower) {
            if (column != null) {
                column.swapRowLabels(positionA, positionB);
            }
        }
        for (Column column : upper) {
            if (column != null) {
                column.swapRowLabels(positionA, positionB);
            }
        }
    }

    /**
     * Solves A x = b using the factorization.
     *
     * @param b right-hand side vector (indexed by original row)
     * @param out destination vector receiving x (indexed by original row)
     */
    public void solve(final double[] b, final double[] out) {
        // Forward substitution L y = P b, column-oriented (scatter): the
        // finished y[k] updates all rows below it through L column k
        final double[] y = new double[size];
        for (int k = 0; k < size; k++) {
            y[k] = b[rowOfPermutedRow[k]];
        }
        for (int k = 0; k < size; k++) {
            final double yk = y[k];
            if (yk == 0.0) {
                continue;
            }
            final int[] rows = lowerRows[k];
            final double[] vals = lowerValues[k];
            for (int e = 0; e < lowerCount[k]; e++) {
                y[rows[e]] -= vals[e] * yk;
            }
        }

        // Back substitution U x = y, column-oriented: the finished x[k]
        // updates all rows above it through U column k (diagonal excluded)
        final double[] solution = new double[size];
        for (int k = size - 1; k >= 0; k--) {
            final double xk = y[k] / diagonal(k);
            solution[k] = xk;
            final int[] rows = upperRows[k];
            final double[] vals = upperValues[k];
            for (int e = 0; e < upperCount[k]; e++) {
                final int row = rows[e];
                if (row != k) {
                    y[row] -= vals[e] * xk;
                }
            }
        }

        // Row pivoting permutes the equations, not the unknowns: solving
        // (PA) x = (Pb) yields x already in original node/unknown coordinates
        System.arraycopy(solution, 0, out, 0, size);
    }

    /**
     * Gets the diagonal (pivot) entry of U at the given column.
     *
     * <p>Row labels in a stored column may be unsorted after pivot row
     * interchanges, so the diagonal is located by a linear scan.
     *
     * @param column column index
     * @return pivot value
     */
    private double diagonal(final int column) {
        final int[] rows = upperRows[column];
        for (int e = 0; e < upperCount[column]; e++) {
            if (rows[e] == column) {
                return upperValues[column][e];
            }
        }
        throw new RuntimeException("Matrix is singular: zero pivot at column " + column);
    }

    private static int[][] extractRows(final Column[] columns) {
        final int[][] out = new int[columns.length][];
        for (int i = 0; i < columns.length; i++) {
            out[i] = columns[i].rows;
        }
        return out;
    }

    private static double[][] extractValues(final Column[] columns) {
        final double[][] out = new double[columns.length][];
        for (int i = 0; i < columns.length; i++) {
            out[i] = columns[i].values;
        }
        return out;
    }

    private static int[] extractCounts(final Column[] columns) {
        final int[] out = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            out[i] = columns[i].count;
        }
        return out;
    }

    /**
     * Growable primitive column store for one L or U column.
     */
    private static final class Column {
        private int[] rows = new int[INITIAL_COLUMN_CAPACITY];
        private double[] values = new double[INITIAL_COLUMN_CAPACITY];
        private int count;

        private void add(final int row, final double value) {
            if (count == rows.length) {
                final int newCapacity = rows.length * 2;
                rows = java.util.Arrays.copyOf(rows, newCapacity);
                values = java.util.Arrays.copyOf(values, newCapacity);
            }
            rows[count] = row;
            values[count] = value;
            count++;
        }

        private void swapRowLabels(final int positionA, final int positionB) {
            for (int e = 0; e < count; e++) {
                if (rows[e] == positionA) {
                    rows[e] = positionB;
                } else if (rows[e] == positionB) {
                    rows[e] = positionA;
                }
            }
        }
    }
}
