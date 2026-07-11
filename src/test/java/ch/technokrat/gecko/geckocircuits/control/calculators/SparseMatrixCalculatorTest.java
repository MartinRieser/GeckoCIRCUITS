package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;

public class SparseMatrixCalculatorTest {

    @Test
    public void testSparseMatrixCalculation() {
        SparseMatrixCalculator calc = new SparseMatrixCalculator();
        assertEquals(8, calc._inputSignal.length);
        assertEquals(9, calc._outputSignal.length);

        calc.initializeAtSimulationStart(1e-6);

        // Setup typical inputs
        calc._inputSignal[0] = new double[]{25e3};   // fDR
        calc._inputSignal[1] = new double[]{325.0};  // ur
        calc._inputSignal[2] = new double[]{-162.5}; // us
        calc._inputSignal[3] = new double[]{-162.5}; // ut
        calc._inputSignal[4] = new double[]{325.0};  // uNmax
        calc._inputSignal[5] = new double[]{300.0};  // uOUTmax
        calc._inputSignal[6] = new double[]{50.0};   // fOUT
        calc._inputSignal[7] = new double[]{0.0};    // phi2

        calc.calculateYOUT(1e-6);

        // Output checks: all output signals are switching signals, should be 0 or 1
        for (int i = 0; i < 9; i++) {
            double outVal = calc._outputSignal[i][0];
            assertTrue(outVal == 0.0 || outVal == 1.0);
        }
    }
}
