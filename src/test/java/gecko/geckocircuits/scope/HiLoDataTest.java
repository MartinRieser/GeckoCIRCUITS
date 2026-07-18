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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for legacy {@link HiLoData} in the scope package.
 */
public class HiLoDataTest {

    private static final double DELTA = 1e-9;

    /**
     * Verifies that the initial yLo and yHi values are correctly defined.
     */
    @Test
    public void testInitialValues() {
        HiLoData data = new HiLoData();
        assertEquals("Initial low boundary should be 1E30", 1E30f, data.yLo, DELTA);
        assertEquals("Initial high boundary should be -1E30", -1E30f, data.yHi, DELTA);
    }

    /**
     * Verifies insertion of numeric values updating boundaries correctly.
     */
    @Test
    public void testInsertCompareValues() {
        HiLoData data = new HiLoData();
        
        data.insertCompare(3.0f);
        data.insertCompare(8.0f);
        data.insertCompare(1.0f);
        
        assertEquals("yLo must track the minimum value inserted", 1.0f, data.yLo, DELTA);
        assertEquals("yHi must track the maximum value inserted", 8.0f, data.yHi, DELTA);
    }

    /**
     * Verifies merging another HiLoData instance.
     */
    @Test
    public void testMergeHiLoData() {
        HiLoData data1 = new HiLoData();
        data1.insertCompare(5.0f);
        data1.insertCompare(10.0f);

        HiLoData data2 = new HiLoData();
        data2.insertCompare(2.0f);
        data2.insertCompare(7.0f);

        data1.insertCompare(data2);

        assertEquals("Merged yLo must match minimum of both", 2.0f, data1.yLo, DELTA);
        assertEquals("Merged yHi must match maximum of both", 10.0f, data1.yHi, DELTA);
    }
}
