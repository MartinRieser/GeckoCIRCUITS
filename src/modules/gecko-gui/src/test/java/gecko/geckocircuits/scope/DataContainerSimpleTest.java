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
package gecko.geckocircuits.scope;

import gecko.geckocircuits.scope.HiLoData;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link DataContainerSimple}, verifying data storage, retrieval,
 * resolution intervals, and range calculations.
 */
public class DataContainerSimpleTest {

    private static final double DELTA = 1e-9;

    /**
     * Verifies that the constructor sets up the dimensions and sets the initial maximum index.
     */
    @Test
    public void testConstructorAndDimensions() {
        int rows = 3;
        int columns = 10;
        DataContainerSimple container = new DataContainerSimple(rows, columns);
        
        assertEquals("Row length must match constructor argument", rows, container.getRowLength());
        assertEquals("Column length must match constructor argument", columns, container.getColumnLength());
        assertEquals("Initial maximum index should be -1", -1, container.getMaximumTimeIndex());
    }

    /**
     * Verifies setting and getting a single value at a valid position.
     */
    @Test
    public void testSetAndGetValue() {
        DataContainerSimple container = new DataContainerSimple(2, 5);
        
        container.setValue(1.5, 0, 0);
        container.setValue(3.7, 1, 3);
        
        assertEquals("Value at (0,0) should match the set value", 1.5, container.getValue(0, 0), DELTA);
        assertEquals("Value at (1,3) should match the set value", 3.7, container.getValue(1, 3), DELTA);
        assertEquals("Maximum index should update when setting row 0", 0, container.getMaximumTimeIndex());
    }

    /**
     * Verifies that setting a value out of bounds does not crash and is ignored.
     */
    @Test
    public void testSetValueOutOfBounds() {
        DataContainerSimple container = new DataContainerSimple(2, 5);
        
        // These out-of-bounds operations should be safely ignored
        container.setValue(9.9, 1, 10);
        container.setValue(9.9, 1, -1);
        
        assertEquals("Value should remain default 0.0", 0.0, container.getValue(1, 4), DELTA);
    }

    /**
     * Verifies retrieval of estimated time values from row 0.
     */
    @Test
    public void testGetEstimatedTimeValue() {
        DataContainerSimple container = new DataContainerSimple(2, 5);
        
        container.setValue(0.01, 0, 0);
        container.setValue(0.02, 0, 1);
        
        assertEquals("Estimated time at column 0 should match", 0.01, container.getEstimatedTimeValue(0), DELTA);
        assertEquals("Estimated time at column 1 should match", 0.02, container.getEstimatedTimeValue(1), DELTA);
    }

    /**
     * Verifies calculation of time interval resolution between columns.
     */
    @Test
    public void testGetTimeIntervalResolution() {
        DataContainerSimple container = new DataContainerSimple(2, 5);
        
        container.setValue(0.0, 0, 0);
        container.setValue(0.005, 0, 1);
        container.setValue(0.010, 0, 2);
        
        assertEquals("Time interval resolution should match spacing", 0.005, container.getTimeIntervalResolution(), DELTA);
    }

    /**
     * Verifies setting and getting whole columns (rows in the underlying array).
     */
    @Test
    public void testGetAndSetColumn() {
        DataContainerSimple container = new DataContainerSimple(2, 5);
        double[] columnData = {10.0, 20.0, 30.0, 40.0, 50.0};
        
        container.setColumn(columnData, 1);
        
        assertSame("Retrieved array should be the same instance", columnData, container.getColumn(1));
        assertEquals("Values from the column array should be queryable", 30.0, container.getValue(1, 2), DELTA);
    }

    /**
     * Verifies minimum and maximum range calculations via HiLoData.
     */
    @Test
    public void testGetHiLoValue() {
        DataContainerSimple container = new DataContainerSimple(2, 5);
        
        container.setValue(4.0, 1, 0);
        container.setValue(9.5, 1, 1);
        container.setValue(-2.0, 1, 2);
        container.setValue(3.0, 1, 3);
        
        // Scan index range [0, 4)
        HiLoData hiLo = container.getHiLoValue(1, 0, 4);
        
        assertEquals("HiLo min should match lowest value in scan range", -2.0f, hiLo.yLo, DELTA);
        assertEquals("HiLo max should match highest value in scan range", 9.5f, hiLo.yHi, DELTA);
    }

    /**
     * Verifies inserting values at the end of the container.
     */
    @Test
    public void testInsertValuesAtEnd() {
        DataContainerSimple container = new DataContainerSimple(3, 5);
        double[] newValues = {100.5, 200.7};
        
        container.insertValuesAtEnd(0.01, newValues);
        
        assertEquals("Maximum index should increment to 0", 0, container.getMaximumTimeIndex());
        assertEquals("Time value at row 0 should match", 0.01, container.getValue(0, 0), DELTA);
        assertEquals("Value at row 1 should match inserted value", 100.5, container.getValue(1, 0), DELTA);
        assertEquals("Value at row 2 should match inserted value", 200.7, container.getValue(2, 0), DELTA);
    }

    /**
     * Verifies that inserting values beyond the column capacity throws an exception.
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testInsertValuesAtEndOutOfBounds() {
        DataContainerSimple container = new DataContainerSimple(2, 1);
        double[] values = {55.5};
        
        // First insertion takes index 0 (capacity limit)
        container.insertValuesAtEnd(0.1, values);
        
        // Second insertion will increment maximum index to 1, causing OutOfBounds
        container.insertValuesAtEnd(0.2, values);
    }
}
