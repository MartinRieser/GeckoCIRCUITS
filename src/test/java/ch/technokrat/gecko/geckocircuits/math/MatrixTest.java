package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class MatrixTest {

    @Test
    public void testConstructorsAndAccessors() {
        double[][] vals = {{1.0, 2.0}, {3.0, 4.0}};
        Matrix m = new Matrix(vals);
        assertEquals(2, m.getRowDimension());
        assertEquals(2, m.getColumnDimension());
        assertEquals(1.0, m.get(0, 0), 1e-9);
        assertEquals(4.0, m.get(1, 1), 1e-9);

        Matrix m2 = new Matrix(3, 4, 7.0);
        assertEquals(3, m2.getRowDimension());
        assertEquals(4, m2.getColumnDimension());
        assertEquals(7.0, m2.get(0, 0), 1e-9);
        assertEquals(7.0, m2.get(2, 3), 1e-9);

        m2.set(1, 1, 9.5);
        assertEquals(9.5, m2.get(1, 1), 1e-9);
    }

    @Test
    public void testArithmetic() {
        double[][] aVals = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] bVals = {{5.0, 6.0}, {7.0, 8.0}};
        Matrix a = new Matrix(aVals);
        Matrix b = new Matrix(bVals);

        Matrix c = a.plus(b);
        assertEquals(6.0, c.get(0, 0), 1e-9);
        assertEquals(12.0, c.get(1, 1), 1e-9);

        Matrix d = a.minus(b);
        assertEquals(-4.0, d.get(0, 0), 1e-9);
        assertEquals(-4.0, d.get(1, 1), 1e-9);

        Matrix e = a.times(b);
        // (1*5 + 2*7) = 19
        // (3*6 + 4*8) = 50
        assertEquals(19.0, e.get(0, 0), 1e-9);
        assertEquals(50.0, e.get(1, 1), 1e-9);
    }

    @Test
    public void testTranspose() {
        double[][] vals = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
        Matrix m = new Matrix(vals);
        Matrix t = m.transpose();
        assertEquals(3, t.getRowDimension());
        assertEquals(2, t.getColumnDimension());
        assertEquals(2.0, t.get(1, 0), 1e-9);
        assertEquals(4.0, t.get(0, 1), 1e-9);
    }

    @Test
    public void testSolveAndInverse() {
        double[][] aVals = {{2.0, 1.0}, {1.0, 2.0}}; // nonsingular
        double[][] bVals = {{5.0}, {4.0}};
        Matrix a = new Matrix(aVals);
        Matrix b = new Matrix(bVals);

        Matrix x = a.solve(b); // 2x + y = 5, x + 2y = 4 -> x = 2, y = 1
        assertEquals(2.0, x.get(0, 0), 1e-9);
        assertEquals(1.0, x.get(1, 0), 1e-9);

        Matrix inv = a.inverse();
        Matrix identity = a.times(inv);
        assertEquals(1.0, identity.get(0, 0), 1e-9);
        assertEquals(0.0, identity.get(0, 1), 1e-9);
        assertEquals(0.0, identity.get(1, 0), 1e-9);
        assertEquals(1.0, identity.get(1, 1), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSolveNonSquareThrows() {
        double[][] aVals = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
        double[][] bVals = {{1.0}, {2.0}};
        Matrix a = new Matrix(aVals);
        Matrix b = new Matrix(bVals);
        a.solve(b);
    }
}
