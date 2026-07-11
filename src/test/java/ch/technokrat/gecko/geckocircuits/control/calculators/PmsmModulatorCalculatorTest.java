package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;

public class PmsmModulatorCalculatorTest {

    @Test
    public void testModulator() {
        PmsmModulatorCalculator calc = new PmsmModulatorCalculator();
        assertEquals(4, calc._inputSignal.length);
        assertEquals(3, calc._outputSignal.length);

        calc._inputSignal[0] = new double[]{200.0};  // valpha
        calc._inputSignal[1] = new double[]{100.0};  // vbeta
        calc._inputSignal[2] = new double[]{0.5};    // triangle carrier
        calc._inputSignal[3] = new double[]{400.0};  // vdc

        calc.calculateYOUT(1e-6);

        // Verify output signals (U, V, W switching states: should be 0 or 1)
        double u = calc._outputSignal[0][0];
        double v = calc._outputSignal[1][0];
        double w = calc._outputSignal[2][0];

        assertTrue(u == 0.0 || u == 1.0);
        assertTrue(v == 0.0 || v == 1.0);
        assertTrue(w == 0.0 || w == 1.0);
    }
}
