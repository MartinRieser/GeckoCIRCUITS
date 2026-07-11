package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class NComplexTest {

    @Test
    public void testConstructorsAndGetters() {
        NComplex c1 = new NComplex(2.5f, -3.5f);
        assertEquals(2.5f, c1.getRe(), 1e-6f);
        assertEquals(-3.5f, c1.getIm(), 1e-6f);

        NComplex c2 = new NComplex(4.0f);
        assertEquals(4.0f, c2.getRe(), 1e-6f);
        assertEquals(0.0f, c2.getIm(), 1e-6f);

        NComplex c3 = new NComplex();
        assertEquals(0.0f, c3.getRe(), 1e-6f);
        assertEquals(0.0f, c3.getIm(), 1e-6f);
    }

    @Test
    public void testToString() {
        NComplex c = new NComplex(1.5f, -2.5f);
        assertEquals("1.5 + -2.5i", c.toString());
    }

    @Test
    public void testAdd() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, -4.0f);
        NComplex result = NComplex.add(a, b);
        assertEquals(4.0f, result.getRe(), 1e-6f);
        assertEquals(-2.0f, result.getIm(), 1e-6f);
    }

    @Test
    public void testSub() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, -4.0f);
        NComplex result = NComplex.sub(a, b);
        assertEquals(-2.0f, result.getRe(), 1e-6f);
        assertEquals(6.0f, result.getIm(), 1e-6f);
    }

    @Test
    public void testMul() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        // (1+2i)*(3+4i) = 3 + 4i + 6i - 8 = -5 + 10i
        NComplex result = NComplex.mul(a, b);
        assertEquals(-5.0f, result.getRe(), 1e-6f);
        assertEquals(10.0f, result.getIm(), 1e-6f);
    }

    @Test
    public void testConj() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex result = NComplex.conj(a);
        assertEquals(1.0f, result.getRe(), 1e-6f);
        assertEquals(-2.0f, result.getIm(), 1e-6f);
    }

    @Test
    public void testDiv() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        // (1+2i)/(3+4i) = (1+2i)*(3-4i)/25 = (3 - 4i + 6i + 8)/25 = (11 + 2i)/25 = 0.44 + 0.08i
        NComplex result = NComplex.div(a, b);
        assertEquals(0.44f, result.getRe(), 1e-6f);
        assertEquals(0.08f, result.getIm(), 1e-6f);

        // Also cover alternate division path (b.re < b.im)
        NComplex c = new NComplex(3.0f, 4.0f);
        NComplex d = new NComplex(1.0f, 2.0f);
        NComplex result2 = NComplex.div(c, d);
        // (3+4i)/(1+2i) = (3+4i)*(1-2i)/5 = (3 - 6i + 4i + 8)/5 = (11 - 2i)/5 = 2.2 - 0.4i
        assertEquals(2.2f, result2.getRe(), 1e-6f);
        assertEquals(-0.4f, result2.getIm(), 1e-6f);
    }

    @Test
    public void testAbs() {
        NComplex a = new NComplex(3.0f, 4.0f);
        assertEquals(5.0f, NComplex.abs(a), 1e-6f);

        NComplex zero = new NComplex();
        assertEquals(0.0f, NComplex.abs(zero), 1e-6f);
    }

    @Test
    public void testSqrt() {
        NComplex zero = new NComplex();
        NComplex sqrtZero = NComplex.sqrt(zero);
        assertEquals(0.0f, sqrtZero.getRe(), 1e-6f);
        assertEquals(0.0f, sqrtZero.getIm(), 1e-6f);

        NComplex a = new NComplex(3.0f, 4.0f); // sqrt(3+4i) = 2+i
        NComplex result1 = NComplex.sqrt(a);
        assertEquals(2.0f, result1.getRe(), 1e-6f);
        assertEquals(1.0f, result1.getIm(), 1e-6f);

        NComplex b = new NComplex(-3.0f, 4.0f); // sqrt(-3+4i) = 1+2i
        NComplex result2 = NComplex.sqrt(b);
        assertEquals(1.0f, result2.getRe(), 1e-6f);
        assertEquals(2.0f, result2.getIm(), 1e-6f);
    }

    @Test
    public void testMultiplyByScalar() {
        NComplex a = new NComplex(1.5f, -2.5f);
        NComplex result = NComplex.multiplyByScalar(2.0f, a);
        assertEquals(3.0f, result.getRe(), 1e-6f);
        assertEquals(-5.0f, result.getIm(), 1e-6f);
    }

    @Test
    public void testNicePrint() {
        NComplex a = new NComplex(1.0f, 2.0f);
        assertNotNull(a.nicePrint());

        NComplex b = new NComplex(1.0f, -1.0f);
        assertNotNull(b.nicePrint());

        NComplex c = new NComplex(0.0f, 1.0f);
        assertEquals("i", c.nicePrint());

        NComplex d = new NComplex(0.0f, -1.0f);
        assertEquals("-i", d.nicePrint());
    }

    @Test
    public void testEqualsAndHashCode() {
        NComplex a = new NComplex(1.5f, 2.5f);
        NComplex b = new NComplex(1.5f, 2.5f);
        NComplex c = new NComplex(1.5f, -2.5f);

        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals("not a complex"));

        assertEquals(a.hashCode(), b.hashCode());
    }
}
