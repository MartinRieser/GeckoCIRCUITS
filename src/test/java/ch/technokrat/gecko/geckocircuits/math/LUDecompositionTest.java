package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class LUDecompositionTest {

    @Test
    public void testSquareDecomposition() {
        double[][] vals = {
            {4.0, 3.0},
            {6.0, 3.0}
        };
        Matrix a = new Matrix(vals);
        LUDecomposition lu = new LUDecomposition(a);

        assertTrue(lu.isNonsingular());

        Matrix l = lu.getL();
        Matrix u = lu.getU();
        int[] piv = lu.getPivot();

        // L should be unit lower triangular
        assertEquals(1.0, l.get(0, 0), 1e-9);
        assertEquals(0.0, l.get(0, 1), 1e-9);
        assertEquals(1.0, l.get(1, 1), 1e-9);

        // U should be upper triangular
        assertEquals(0.0, u.get(1, 0), 1e-9);

        // Test reconstruction A(piv,:) = L * U
        Matrix luProduct = l.times(u);
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                assertEquals(a.get(piv[i], j), luProduct.get(i, j), 1e-9);
            }
        }
    }

    @Test
    public void testSingularMatrix() {
        double[][] vals = {
            {1.0, 2.0},
            {2.0, 4.0}
        };
        Matrix a = new Matrix(vals);
        LUDecomposition lu = new LUDecomposition(a);
        assertFalse(lu.isNonsingular());
    }
}
