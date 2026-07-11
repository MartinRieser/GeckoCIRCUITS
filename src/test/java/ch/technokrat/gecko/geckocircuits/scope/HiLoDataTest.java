package ch.technokrat.gecko.geckocircuits.scope;

import org.junit.Test;
import static org.junit.Assert.*;

public class HiLoDataTest {

    @Test
    public void testInsertCompareValue() {
        HiLoData data = new HiLoData();
        assertEquals(1E30f, data.yLo, 1e-9);
        assertEquals(-1E30f, data.yHi, 1e-9);

        data.insertCompare(5.5f);
        assertEquals(5.5f, data.yLo, 1e-6f);
        assertEquals(5.5f, data.yHi, 1e-6f);

        data.insertCompare(-3.2f);
        data.insertCompare(10.0f);
        assertEquals(-3.2f, data.yLo, 1e-6f);
        assertEquals(10.0f, data.yHi, 1e-6f);
    }

    @Test
    public void testInsertCompareData() {
        HiLoData d1 = new HiLoData();
        d1.insertCompare(2.0f);

        HiLoData d2 = new HiLoData();
        d2.insertCompare(-1.0f);
        d2.insertCompare(5.0f);

        d1.insertCompare(d2);
        assertEquals(-1.0f, d1.yLo, 1e-6f);
        assertEquals(5.0f, d1.yHi, 1e-6f);
    }
}
