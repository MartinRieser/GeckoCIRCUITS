package ch.technokrat.gecko.geckocircuits.newscope;

import org.junit.Test;
import static org.junit.Assert.*;

public class HiLoDataTest {

    @Test
    public void testFabricAndGetters() {
        HiLoData data = HiLoData.hiLoDataFabric(1.5f, 5.5f);
        assertEquals(1.5f, data._yLo, 1e-6f);
        assertEquals(5.5f, data._yHi, 1e-6f);

        // Test static caching
        HiLoData zero = HiLoData.hiLoDataFabric(0, 0);
        HiLoData zeroCheck = HiLoData.hiLoDataFabric(0, 0);
        assertSame(zero, zeroCheck);

        HiLoData zeroOne = HiLoData.hiLoDataFabric(0, 1);
        HiLoData zeroOneCheck = HiLoData.hiLoDataFabric(0, 1);
        assertSame(zeroOne, zeroOneCheck);
    }

    @Test
    public void testCompare() {
        HiLoData d1 = HiLoData.hiLoDataFabric(1.0f, 2.0f);
        HiLoData d2 = HiLoData.hiLoDataFabric(1.0f, 2.0f);
        HiLoData d3 = HiLoData.hiLoDataFabric(1.0f, 3.0f);

        assertTrue(d1.compare(d2));
        assertFalse(d1.compare(d3));
    }

    @Test
    public void testMergeFromValue() {
        HiLoData initial = null;
        HiLoData merged = HiLoData.mergeFromValue(initial, 5.0f);
        assertNotNull(merged);
        assertEquals(5.0f, merged._yLo, 1e-6f);
        assertEquals(5.0f, merged._yHi, 1e-6f);

        HiLoData merged2 = HiLoData.mergeFromValue(merged, 3.0f);
        assertEquals(3.0f, merged2._yLo, 1e-6f);
        assertEquals(5.0f, merged2._yHi, 1e-6f);

        HiLoData merged3 = HiLoData.mergeFromValue(merged2, 8.0f);
        assertEquals(3.0f, merged3._yLo, 1e-6f);
        assertEquals(8.0f, merged3._yHi, 1e-6f);

        // Value inside interval should not change bounds
        HiLoData merged4 = HiLoData.mergeFromValue(merged3, 6.0f);
        assertSame(merged3, merged4);
    }
}
