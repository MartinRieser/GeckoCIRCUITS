package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;

public class NothingToDoCalculatorTest {

    @Test
    public void testCalculator() {
        NothingToDoCalculator calculator = new NothingToDoCalculator(2, 3);
        assertEquals(2, calculator._inputSignal.length);
        assertEquals(3, calculator._outputSignal.length);

        // Inputs
        calculator._inputSignal[0] = new double[]{1.5};
        calculator._inputSignal[1] = new double[]{2.5};

        // Output remains default or unchanged
        calculator._outputSignal[0] = new double[]{10.0};
        calculator._outputSignal[1] = new double[]{20.0};
        calculator._outputSignal[2] = new double[]{30.0};

        calculator.calculateYOUT(1e-6);

        // Verification: outputs unchanged
        assertEquals(10.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(20.0, calculator._outputSignal[1][0], 1e-9);
        assertEquals(30.0, calculator._outputSignal[2][0], 1e-9);
    }
}
