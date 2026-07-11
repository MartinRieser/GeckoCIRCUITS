package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;
import ch.technokrat.gecko.geckocircuits.control.SSAShape;

public class SmallSignalCalculatorTest {

    @Test
    public void testSmallSignalCalculations() {
        // Construct with 1 input and 1 output
        SmallSignalCalculator calc = new SmallSignalCalculator(
            1.0, 10.0, 1000.0, SSAShape.RECTANGLE, 1, 1, false
        );

        calc._inputSignal[0] = new double[]{0.0};
        calc.initializeAtSimulationStart(1e-5);

        // Run calculation a few times
        for (int i = 0; i < 10; i++) {
            calc.externalSetTime(i * 1e-5);
            calc.calculateYOUT(1e-5);
        }

        // Verify output array is allocated
        assertNotNull(calc._outputSignal);
        assertEquals(1, calc._outputSignal.length);
    }
}
