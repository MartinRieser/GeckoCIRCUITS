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
package gecko.core.simulation;

/**
 * Linear-algebra backend of the MNA system solver.
 *
 * <ul>
 *   <li><b>DENSE</b>: full {@code N x N} storage with dense LU factorization
 *       (reference implementation, best for small circuits).</li>
 *   <li><b>SPARSE</b>: compressed sparse column storage with sparse LU
 *       factorization and partial pivoting; removes the O(N³) dense
 *       factorization overhead for large networks.</li>
 *   <li><b>AUTO</b>: sparse above the automatic size threshold, dense
 *       below - the default.</li>
 * </ul>
 *
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public enum MatrixSolverKind {

    /** Dense N x N storage with dense LU factorization. */
    DENSE,

    /** Compressed sparse column storage with sparse LU factorization. */
    SPARSE,

    /** Sparse above the automatic size threshold, dense below. */
    AUTO
}
