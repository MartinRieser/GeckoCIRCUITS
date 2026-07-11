package ch.technokrat.gecko.geckocircuits.scope;

import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import org.junit.Test;
import static org.junit.Assert.*;

public class HiLoDataTest {

    @Test
    public void testInsertCompareValue() {
        HiLoData data = null;

        data = HiLoData.mergeFromValue(data, 5.5f);
        assertEquals(5.5f, data._yLo, 1e-6f);
        assertEquals(5.5f, data._yHi, 1e-6f);

        data = HiLoData.mergeFromValue(data, -3.2f);
        data = HiLoData.mergeFromValue(data, 10.0f);
        assertEquals(-3.2f, data._yLo, 1e-6f);
        assertEquals(10.0f, data._yHi, 1e-6f);
    }

    @Test
    public void testInsertCompareData() {
        HiLoData d1 = HiLoData.mergeFromValue(null, 2.0f);
        HiLoData d2 = HiLoData.mergeFromValue(null, -1.0f);
        d2 = HiLoData.mergeFromValue(d2, 5.0f);

        HiLoData merged = HiLoData.merge(d1, d2);
        assertEquals(-1.0f, merged._yLo, 1e-6f);
        assertEquals(5.0f, merged._yHi, 1e-6f);
    }
}
