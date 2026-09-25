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
package gecko.core.circuit.matrix;

import java.util.Arrays;

/**
 * Dense {@code double[][]} implementation of {@link MatrixAccumulator}.
 *
 * <p>Wraps the solver's system-matrix storage so the dense reference solver
 * behaves exactly as before the accumulator seam: adds become {@code +=}
 * writes, {@link #reset()} and {@link #zeroRow(int)} clear the wrapped array.
 *
 * @see MatrixAccumulator
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public final class DenseMatrixAccumulator implements MatrixAccumulator {

    /** The wrapped system-matrix storage. */
    private final double[][] matrix;

    /** Matrix order; bounds every row/column access. */
    private final int size;

    /**
     * Creates a dense accumulator wrapping the given matrix storage.
     *
     * @param matrix system-matrix storage to write through to
     * @param size matrix order (valid row/column range is [0, size))
     */
    public DenseMatrixAccumulator(final double[][] matrix, final int size) {
        this.matrix = matrix;
        this.size = size;
    }

    @Override
    public void reset() {
        for (int i = 0; i < size; i++) {
            Arrays.fill(matrix[i], 0.0);
        }
    }

    @Override
    public void add(final int row, final int col, final double value) {
        matrix[row][col] += value;
    }

    @Override
    public void zeroRow(final int row) {
        Arrays.fill(matrix[row], 0.0);
    }
}
