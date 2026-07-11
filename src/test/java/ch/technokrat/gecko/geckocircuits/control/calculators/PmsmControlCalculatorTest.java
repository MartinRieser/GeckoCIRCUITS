package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;

public class PmsmControlCalculatorTest {

    @Test
    public void testPmsmControl() {
        PmsmControlCalculator calc = new PmsmControlCalculator();
        assertEquals(12, calc._inputSignal.length);
        assertEquals(8, calc._outputSignal.length);

        // Setup typical inputs
        calc._inputSignal[0] = new double[]{1.0};   // ia
        calc._inputSignal[1] = new double[]{-0.5};  // ib
        calc._inputSignal[2] = new double[]{100.0}; // w (actual speed)
        calc._inputSignal[3] = new double[]{0.5};   // phi
        calc._inputSignal[4] = new double[]{1200.0};// n_ref
        calc._inputSignal[5] = new double[]{2.0};   // Kp_n
        calc._inputSignal[6] = new double[]{0.05};  // T_n
        calc._inputSignal[7] = new double[]{10.0};  // n_limit
        calc._inputSignal[8] = new double[]{5.0};   // Kp_i
        calc._inputSignal[9] = new double[]{0.01};  // T_i
        calc._inputSignal[10] = new double[]{20.0}; // i_limit
        calc._inputSignal[11] = new double[]{1.0};  // fP

        // Run calculation
        calc.calculateYOUT(1e-4);

        // Output check: verify that calculation ran and output arrays are populated
        // Outputs are: [valpha_last, vbeta_last, vq_ref, vd_ref, iq_ref, id_ref, iq, id]
        assertTrue(calc._outputSignal[0][0] != 0.0 || calc._outputSignal[1][0] != 0.0);
        assertEquals(0.0, calc._outputSignal[5][0], 1e-9); // id_ref is constant 0
    }
}
