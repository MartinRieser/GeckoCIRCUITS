/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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

package ch.technokrat.gecko.geckocircuits.scope;

/**
 * This is a deprecated class, in future, replace with datacontainer from "newscope" package
 * @author andy
 */
@Deprecated
public interface DataContainer {

    /**
     * Returns the value at the given row and column.
     *
     * @param row    the data row (signal) index
     * @param column the time-column index
     * @return the stored value
     */
    double getValue(final int row, final int column);

    /**
     * Returns the high/low extreme values for the given row over a column range.
     *
     * @param row       the data row (signal) index
     * @param column    the start column index
     * @param columnOld the end column index
     * @return the hi/lo data for the range
     */
    HiLoData getHiLoValue(final int row, final int column, final int columnOld);

    /**
     * Stores a value at the given row and column.
     *
     * @param value  the value to store
     * @param row    the data row (signal) index
     * @param column the time-column index
     */
    void setValue(final double value, final int row, final int column);

    /**
     * Returns the number of rows (signals) in the container.
     *
     * @return the row count
     */
    int getRowLength();

    /**
     * Returns the number of columns (time steps) in the container.
     *
     * @return the column count
     */
    int getColumnLength();

    /**
     * Replaces an entire column of data.
     *
     * @param data  the new column values
     * @param index the column index
     */
    void setColumn(final double[] data, final int index);

    /**
     * Returns all values in a single column.
     *
     * @param index the column index
     * @return the column values
     */
    double[] getColumn(final int index);

    /**
     * Returns the time resolution (delta-t) between columns.
     *
     * @return the time interval resolution
     */
    double getTimeIntervalResolution();

    /**
     * Returns the estimated time value at the given column index.
     *
     * @param index the column index
     * @return the estimated time value
     */
    double getEstimatedTimeValue(final int index);

    /**
     * Returns the maximum valid time-column index.
     *
     * @return the maximum time index
     */
    int getMaximumTimeIndex();

    /**
     * Appends another row of data points at the end of the container.
     *
     * @param timeValue the time stamp for the new row
     * @param values    the signal values for the new row
     */
    void insertValuesAtEnd(final double timeValue, final double[] values);
    
}
