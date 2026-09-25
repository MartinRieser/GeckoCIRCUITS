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

import java.util.Arrays;

/**
 * Square matrix in compressed sparse column (CSC) storage.
 *
 * <p>Built from the coordinate triplets collected by
 * {@link TripletMatrixAccumulator}: triplets are grouped by column, each
 * column's entries are sorted by row index, and duplicate positions are
 * summed - mirroring the accumulating {@code +=} semantics of a dense matrix
 * build.
 *
 * @see SparseLU
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public final class SparseMatrix {

    private final int size;
    private final int[] columnPointers;
    private final int[] rowIndices;
    private final double[] values;

    private SparseMatrix(final int size, final int[] columnPointers,
                         final int[] rowIndices, final double[] values) {
        this.size = size;
        this.columnPointers = columnPointers;
        this.rowIndices = rowIndices;
        this.values = values;
    }

    /**
     * Builds a CSC matrix from accumulated coordinate triplets.
     *
     * @param size matrix order
     * @param triplets the accumulated stamps
     * @return CSC matrix with duplicates summed and columns row-sorted
     */
    public static SparseMatrix fromTriplets(final int size, final TripletMatrixAccumulator triplets) {
        final int tripletCount = triplets.getEntryCount();

        // Count entries per column
        final int[] columnCount = new int[size];
        final int[] tripletColumns = triplets.getColumnIndices();
        for (int i = 0; i < tripletCount; i++) {
            columnCount[tripletColumns[i]]++;
        }

        // Prefix sums give the column pointers
        final int[] columnPointers = new int[size + 1];
        for (int c = 0; c < size; c++) {
            columnPointers[c + 1] = columnPointers[c] + columnCount[c];
        }

        // Scatter triplets into their column slots (stable)
        final int[] rowIndices = new int[tripletCount];
        final double[] values = new double[tripletCount];
        final int[] tripletRows = triplets.getRowIndices();
        final double[] tripletValues = triplets.getValues();
        final int[] fillPosition = Arrays.copyOf(columnPointers, size);
        for (int i = 0; i < tripletCount; i++) {
            final int col = tripletColumns[i];
            final int pos = fillPosition[col];
            rowIndices[pos] = tripletRows[i];
            values[pos] = tripletValues[i];
            fillPosition[col] = pos + 1;
        }

        sortAndSumDuplicatesPerColumn(size, columnPointers, rowIndices, values);

        return new SparseMatrix(size, columnPointers, rowIndices, values);
    }

    /**
     * Sorts each column's entries by row index and sums duplicate positions.
     *
     * <p>Columns of a circuit MNA matrix are narrow, so an in-place insertion
     * sort per column is the simplest exact approach.
     */
    private static void sortAndSumDuplicatesPerColumn(final int size, final int[] columnPointers,
                                                      final int[] rowIndices, final double[] values) {
        final int[] compactedPointers = new int[size + 1];
        int write = 0;
        for (int col = 0; col < size; col++) {
            final int start = columnPointers[col];
            final int end = columnPointers[col + 1];
            // Insertion sort by row index (columns are narrow)
            for (int i = start + 1; i < end; i++) {
                final int row = rowIndices[i];
                final double value = values[i];
                int j = i - 1;
                while (j >= start && rowIndices[j] > row) {
                    rowIndices[j + 1] = rowIndices[j];
                    values[j + 1] = values[j];
                    j--;
                }
                rowIndices[j + 1] = row;
                values[j + 1] = value;
            }
            // Merge duplicates into the global write position; the write
            // pointer never overtakes the read position, so unread entries of
            // later columns are safe
            compactedPointers[col] = write;
            for (int read = start; read < end; read++) {
                if (write > compactedPointers[col] && rowIndices[write - 1] == rowIndices[read]) {
                    values[write - 1] += values[read];
                } else {
                    rowIndices[write] = rowIndices[read];
                    values[write] = values[read];
                    write++;
                }
            }
        }
        compactedPointers[size] = write;
        System.arraycopy(compactedPointers, 0, columnPointers, 0, size + 1);
    }

    /**
     * Gets the matrix order.
     *
     * @return matrix size N
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the CSC column pointers (length N+1).
     *
     * @return column pointers array
     */
    public int[] getColumnPointers() {
        return columnPointers;
    }

    /**
     * Gets the CSC row indices.
     *
     * @return row indices array
     */
    public int[] getRowIndices() {
        return rowIndices;
    }

    /**
     * Gets the CSC values.
     *
     * @return values array
     */
    public double[] getValues() {
        return values;
    }

    /**
     * Gets the number of stored (possibly explicit-zero) entries.
     *
     * @return non-zero count of the CSC storage
     */
    public int getNonZeroCount() {
        return columnPointers[size];
    }

    /**
     * Materializes this matrix densely; test/diagnostic aid only.
     *
     * @return dense {@code N x N} copy
     */
    public double[][] toDense() {
        final double[][] dense = new double[size][size];
        for (int col = 0; col < size; col++) {
            for (int i = columnPointers[col]; i < columnPointers[col + 1]; i++) {
                dense[rowIndices[i]][col] += values[i];
            }
        }
        return dense;
    }
}
