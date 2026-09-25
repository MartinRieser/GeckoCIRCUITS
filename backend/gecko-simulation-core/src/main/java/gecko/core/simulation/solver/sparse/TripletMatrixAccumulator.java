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

import gecko.core.circuit.matrix.MatrixAccumulator;

import java.util.Arrays;

/**
 * Coordinate-triplet collector implementing {@link MatrixAccumulator}.
 *
 * <p>Receives the per-element stamps of a matrix build as (row, col, value)
 * triplets in a growable primitive store, so no dense {@code N x N} array is
 * ever materialized. Repeated adds to the same position accumulate later
 * during the compressed-sparse-column conversion
 * ({@link SparseMatrix#fromTriplets}).
 *
 * @see SparseMatrix
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public final class TripletMatrixAccumulator implements MatrixAccumulator {

    /** Initial capacity of the triplet store. */
    private static final int INITIAL_CAPACITY = 256;

    private final int size;
    private int[] rows = new int[INITIAL_CAPACITY];
    private int[] cols = new int[INITIAL_CAPACITY];
    private double[] values = new double[INITIAL_CAPACITY];
    private int count;

    /**
     * Creates a triplet accumulator for a matrix of the given order.
     *
     * @param size matrix order (valid row/column range is [0, size))
     */
    public TripletMatrixAccumulator(final int size) {
        this.size = size;
    }

    @Override
    public void reset() {
        count = 0;
    }

    @Override
    public void add(final int row, final int col, final double value) {
        if (count == rows.length) {
            final int newCapacity = rows.length * 2;
            rows = Arrays.copyOf(rows, newCapacity);
            cols = Arrays.copyOf(cols, newCapacity);
            values = Arrays.copyOf(values, newCapacity);
        }
        rows[count] = row;
        cols[count] = col;
        values[count] = value;
        count++;
    }

    @Override
    public void zeroRow(final int row) {
        // In-place compaction dropping every triplet of the row; used to pin
        // island reference potentials after stamping
        int kept = 0;
        for (int i = 0; i < count; i++) {
            if (rows[i] != row) {
                rows[kept] = rows[i];
                cols[kept] = cols[i];
                values[kept] = values[i];
                kept++;
            }
        }
        count = kept;
    }

    /**
     * Gets the matrix order this accumulator stamps into.
     *
     * @return matrix order
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the number of accumulated triplets.
     *
     * @return triplet count
     */
    public int getEntryCount() {
        return count;
    }

    /**
     * Gets the row index per triplet (valid up to {@link #getEntryCount()}).
     *
     * @return row indices array
     */
    public int[] getRowIndices() {
        return rows;
    }

    /**
     * Gets the column index per triplet (valid up to {@link #getEntryCount()}).
     *
     * @return column indices array
     */
    public int[] getColumnIndices() {
        return cols;
    }

    /**
     * Gets the value per triplet (valid up to {@link #getEntryCount()}).
     *
     * @return values array
     */
    public double[] getValues() {
        return values;
    }
}
