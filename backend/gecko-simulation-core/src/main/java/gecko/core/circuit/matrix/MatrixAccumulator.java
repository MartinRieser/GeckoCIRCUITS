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

/**
 * Write-through target for MNA system-matrix stamps.
 *
 * <p>Component stampers add their conductance/admittance contributions through
 * this abstraction instead of writing into a dense {@code double[][]}, so the
 * same stamping code feeds either the dense reference solver or a sparse
 * triplet collector. Repeated adds to the same (row, col) position accumulate,
 * exactly like the {@code +=} writes of the dense matrix.
 *
 * @see IMatrixStamper
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public interface MatrixAccumulator {

    /**
     * Clears all previously accumulated contributions for a fresh matrix build.
     */
    void reset();

    /**
     * Accumulates a value at the given matrix position.
     *
     * @param row row index (0-based)
     * @param col column index (0-based)
     * @param value value to add to the position
     */
    void add(int row, int col, double value);

    /**
     * Zeroes every entry of the given row.
     *
     * <p>Used to pin island reference potentials: the row is cleared after
     * stamping and only the diagonal constraint entry is re-added.
     *
     * @param row row index (0-based) to clear
     */
    void zeroRow(int row);
}
