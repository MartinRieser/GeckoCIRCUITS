package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;

public class ABCDQCalculatorTest {

    @Test
    public void testCalculateYOUT() {
        ABCDQCalculator calculator = new ABCDQCalculator();
        // Setup inputs
        calculator._inputSignal[0] = new double[]{10.0}; // a
        calculator._inputSignal[1] = new double[]{-5.0}; // b
        calculator._inputSignal[2] = new double[]{-5.0}; // c
        calculator._inputSignal[3] = new double[]{0.0};  // theta = 0

        calculator.calculateYOUT(1e-6);

        // a=10, b=-5, c=-5, theta=0
        // dVal = (2 * (10 - (-5)) * 1 + (-5 - (-5)) * (1 + 0)) / 3 = (2 * 15) / 3 = 10.0
        // qVal = -(2 * (10 - (-5)) * 0 + (-5 - (-5)) * (0 - sqrt(3))) / 3 = 0.0
        assertEquals(10.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9);
    }
}
