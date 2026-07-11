package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class CholeskyDecompositionTest {

    @Test
    public void testSPDDecomposition() {
        double[][] vals = {
            {4.0, 12.0},
            {12.0, 37.0}
        };
        Matrix a = new Matrix(vals);
        CholeskyDecomposition chol = new CholeskyDecomposition(a);

        assertTrue(chol.isSPD());

        Matrix l = chol.getL();
        // L should be lower triangular
        assertEquals(2.0, l.get(0, 0), 1e-9);
        assertEquals(0.0, l.get(0, 1), 1e-9);
        assertEquals(6.0, l.get(1, 0), 1e-9);
        assertEquals(1.0, l.get(1, 1), 1e-9);

        // Verify A = L * L'
        Matrix reconstruction = l.times(l.transpose());
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                assertEquals(a.get(i, j), reconstruction.get(i, j), 1e-9);
            }
        }
    }

    @Test
    public void testNonSPDMatrix() {
        // Not positive definite
        double[][] vals = {
            {-4.0, 12.0},
            {12.0, 37.0}
        };
        Matrix a = new Matrix(vals);
        CholeskyDecomposition chol = new CholeskyDecomposition(a);
        assertFalse(chol.isSPD());
    }
}
