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
package ch.technokrat.gecko.geckocircuits.circuit;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A sparse symmetric matrix implementation using nested HashMaps for
 * row-column storage. Only the upper triangle (column >= row) is stored;
 * diagonal entries are always non-zero for Pardiso solver compatibility.
 */
public class SymmetricDoubleSparseMatrix {

    public HashMap<Integer, HashSet<Integer>> rowEntries = new HashMap<Integer, HashSet<Integer>>();
    public HashMap<Integer, HashMap<Integer, Double>> rowEntriesValue = new HashMap<Integer, HashMap<Integer, Double>>();
    private final int _N;

    /**
     * Creates an N x N symmetric sparse matrix. Diagonal entries are
     * initialized to a small non-zero value for Pardiso compatibility.
     * @param N the matrix dimension
     */
    @SuppressWarnings("this-escape")
    public SymmetricDoubleSparseMatrix(int N) {
        _N = N;

        // set the diagonal to non-zero values - this is required for the
        // pardiso sparse solver - otherwise the solver will not work!
        for (int i = 0; i < N; i++) {
            setValue(i, i, 1e-70);
        }
    }

    /**
     * Sets the value at (row, column). Only upper-triangular entries
     * (column >= row) are stored.
     * @param row the row index
     * @param column the column index
     * @param value the value to set
     */
    public void setValue(int row, int column, double value) {
        // assert row >= 0 : "row:  " + row;
        // assert column >= 0 : "column: " + column;
        assert row < _N;
        assert column < _N;
            if (column < row) {
                assert false;
                return;
            }

        HashSet<Integer> rowE = rowEntries.get(row);
        HashMap<Integer, Double> rowEValue = rowEntriesValue.get(row);
        if (rowE == null) {
            rowE = new HashSet<Integer>();
        }

        if (rowEValue == null) {
            rowEValue = new HashMap<Integer, Double>();
        }

        rowE.add(column);
        rowEntries.put(row, rowE);

        rowEValue.put(column, value);
        rowEntriesValue.put(row, rowEValue);

    }

    /**
     * Gets the value at (row, column), returning 0 if no entry exists.
     * @param row the row index
     * @param column the column index
     * @return the value, or 0 if not stored
     */
    public double getValue(int row, int column) {
        HashMap<Integer, Double> rowValues = rowEntriesValue.get(row);
        if (rowValues == null) {
            return 0;
        } else {
            if (rowValues.containsKey(column)) {
                return rowValues.get(column);
            } else {
                return 0;
            }
        }
    }

    /**
     * Removes a zero entry from the matrix. Diagonal entries are preserved
     * even if zero, as required by the Pardiso solver.
     * @param row the row index
     * @param column the column index
     */
    void removeZeroEntry(int row, int column) {

        if (row == column) {// in symmetric matrices, the diagonal value MUST be present,
                // even if it is "0".
                return;
        }

        if (rowEntriesValue.containsKey(row)) {
            HashMap<Integer, Double> roweValues = rowEntriesValue.get(row);
            if (roweValues.containsKey(column)) {
                roweValues.remove(column);
                HashSet<Integer> rows = rowEntries.get(row);
                HashMap<Integer, Double> rowsValue = rowEntriesValue.get(row);
                rowsValue.remove(column);
                rows.remove(column);
                if (rows.isEmpty()) {
                    rowEntries.remove(rows);
                    rowEntriesValue.remove(rowsValue);
                }
            }
        }
    }

    /**
     * Prints the full matrix to standard output for debugging.
     */
    public void print() {

        System.out.println("----------------------");
        for (int i = 0; i < _N; i++) {
            System.out.print(i + "\t");
        }

        System.out.println("\n [");

        for (int i = 0; i < _N; i++) {
            for (int j = 0; j < _N; j++) {
                System.out.print(getValue(i, j) + ",\t");
            }
            System.out.println(";");
        }
        System.out.println("]");
    }

    /**
     * Returns the total number of non-zero entries stored in the matrix.
     * @return the count of non-zero entries
     */
    public int getNumberOfNonZeros() {

        int counter = 0;
        for (int i = 0; i < _N; i++) {
            if (rowEntries.containsKey(i)) {
                for (int j : rowEntries.get(i)) {
                    counter++;
                }
            }
        }

        return counter;
    }

    /**
     * Returns the matrix dimension (N x N).
     * @return the matrix size
     */
    int getMatrixSize() {
        return _N;
    }
}
