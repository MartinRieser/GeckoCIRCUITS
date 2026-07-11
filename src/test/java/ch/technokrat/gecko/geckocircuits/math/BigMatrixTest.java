package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigDecimal;

public class BigMatrixTest {

    @Test
    public void testConstructorsAndAccessors() {
        BigDecimal[][] vals = {
            {BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0)},
            {BigDecimal.valueOf(3.0), BigDecimal.valueOf(4.0)}
        };
        BigMatrix m = new BigMatrix(vals);
        assertEquals(2, m.getRowDimension());
        assertEquals(2, m.getColumnDimension());
        assertEquals(1.0, m.getArray()[0][0].doubleValue(), 1e-9);
        assertEquals(4.0, m.getArray()[1][1].doubleValue(), 1e-9);

        BigMatrix m2 = new BigMatrix(3, 4);
        assertEquals(3, m2.getRowDimension());
        assertEquals(4, m2.getColumnDimension());

        m2.getArray()[1][1] = BigDecimal.valueOf(9.5);
        assertEquals(9.5, m2.getArray()[1][1].doubleValue(), 1e-9);
    }

    @Test
    public void testNorms() {
        BigDecimal[][] vals = {
            {BigDecimal.valueOf(1.0), BigDecimal.valueOf(-2.0)},
            {BigDecimal.valueOf(-3.0), BigDecimal.valueOf(4.0)}
        };
        BigMatrix m = new BigMatrix(vals);
        // norm1 is max column sum of absolute values
        // col 0: |1| + |-3| = 4
        // col 1: |-2| + |4| = 6
        assertEquals(6.0, m.norm1(), 1e-9);

        // normInf is max row sum of absolute values
        // row 0: |1| + |-2| = 3
        // row 1: |-3| + |4| = 7
        assertEquals(7.0, m.normInf(), 1e-9);
    }

    @Test
    public void testSolveAndLU() {
        BigDecimal[][] aVals = {
            {BigDecimal.valueOf(2.0), BigDecimal.valueOf(1.0)},
            {BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0)}
        };
        BigDecimal[][] bVals = {
            {BigDecimal.valueOf(5.0)},
            {BigDecimal.valueOf(4.0)}
        };
        BigMatrix a = new BigMatrix(aVals);
        BigMatrix b = new BigMatrix(bVals);

        BigMatrix x = a.solve(b);
        assertNotNull(x);
        assertEquals(2.0, x.getArray()[0][0].doubleValue(), 1e-9);
        assertEquals(1.0, x.getArray()[1][0].doubleValue(), 1e-9);

        BigLUDecomposition lu = a.lu();
        assertTrue(lu.isNonsingular());
    }
}
