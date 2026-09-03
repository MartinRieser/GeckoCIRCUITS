/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control;

import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.ConstantCalculator;
import gecko.core.control.calculators.ScriptBlockCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptBlockCalculatorTest {

    @Test
    void testBasicFormulaSingleExpression() throws Exception {
        ScriptBlockCalculator calc = new ScriptBlockCalculator(1, 1, "u1 * 2 + 5");
        calc.setInputSignal(0, new ConstantCalculator(10), 0);

        calc.calculateYOUT(1e-6);
        assertEquals(25.0, calc._outputSignal[0][0], 1e-12);
    }

    @Test
    void testTrigonometryAndMathFunctions() throws Exception {
        ScriptBlockCalculator calc = new ScriptBlockCalculator(2, 2, """
            yOUT[0] = sin(xIN[0]) * 10;
            yOUT[1] = sqrt(abs(xIN[1]));
            """);
        calc.setInputSignal(0, new ConstantCalculator(Math.PI / 2), 0);
        calc.setInputSignal(1, new ConstantCalculator(-16.0), 0);

        calc.calculateYOUT(1e-6);
        assertEquals(10.0, calc._outputSignal[0][0], 1e-12);
        assertEquals(4.0, calc._outputSignal[1][0], 1e-12);
    }

    @Test
    void testTimeDependentSignal() throws Exception {
        ScriptBlockCalculator calc = new ScriptBlockCalculator(0, 1, "sin(2 * PI * 50 * t)");
        AbstractControlCalculatable.setTime(0.005); // 1/4 of 50 Hz period (T = 0.02s) -> sin(pi/2) = 1.0

        calc.calculateYOUT(1e-6);
        assertEquals(1.0, calc._outputSignal[0][0], 1e-9);
    }

    @Test
    void testConditionalsAndTernary() throws Exception {
        ScriptBlockCalculator calc = new ScriptBlockCalculator(1, 2, """
            if (u1 > 0) {
                yOUT[0] = 1.0;
            } else {
                yOUT[0] = -1.0;
            }
            yOUT[1] = u1 >= 5 ? 100 : 0;
            """);

        // Test with u1 = 10
        calc.setInputSignal(0, new ConstantCalculator(10.0), 0);
        calc.calculateYOUT(1e-6);
        assertEquals(1.0, calc._outputSignal[0][0], 1e-12);
        assertEquals(100.0, calc._outputSignal[1][0], 1e-12);

        // Test with u1 = -5
        calc._inputSignal[0][0] = -5.0;
        calc.calculateYOUT(1e-6);
        assertEquals(-1.0, calc._outputSignal[0][0], 1e-12);
        assertEquals(0.0, calc._outputSignal[1][0], 1e-12);
    }

    @Test
    void testStateVariablesAcrossSteps() throws Exception {
        ScriptBlockCalculator calc = new ScriptBlockCalculator(1, 1, """
            counter = counter + u1;
            yOUT[0] = counter;
            """, "counter = 0;", "");

        calc.setInputSignal(0, new ConstantCalculator(2.5), 0);
        calc.initializeAtSimulationStart(1e-6);

        calc.calculateYOUT(1e-6);
        assertEquals(2.5, calc._outputSignal[0][0], 1e-12);

        calc.calculateYOUT(1e-6);
        assertEquals(5.0, calc._outputSignal[0][0], 1e-12);

        calc.calculateYOUT(1e-6);
        assertEquals(7.5, calc._outputSignal[0][0], 1e-12);
    }

    @Test
    void testClassicDemoJavaBlockExactSnippet() throws Exception {
        // Exact code snippet from demo_JAVA_Block.ipes
        String classicSnippet = """
            yOUT[0]= xIN[0];
            yOUT[1]= xIN[0]*xIN[1];
            yOUT[2]= Math.sqrt(Math.abs(xIN[0]));
            yOUT[3]= Math.sqrt(Math.abs(xIN[1]));
            yOUT[4]= yOUT[1] +yOUT[2] +yOUT[3];

            return yOUT;
            """;

        ScriptBlockCalculator calc = new ScriptBlockCalculator(2, 5, classicSnippet);
        calc.setInputSignal(0, new ConstantCalculator(4.0), 0);
        calc.setInputSignal(1, new ConstantCalculator(9.0), 0);

        calc.calculateYOUT(1e-6);
        assertEquals(4.0, calc._outputSignal[0][0], 1e-12);
        assertEquals(36.0, calc._outputSignal[1][0], 1e-12);
        assertEquals(2.0, calc._outputSignal[2][0], 1e-12);
        assertEquals(3.0, calc._outputSignal[3][0], 1e-12);
        assertEquals(41.0, calc._outputSignal[4][0], 1e-12); // 36 + 2 + 3 = 41
    }

    @Test
    void testClassicPmsmFrameTransformExactSnippet() throws Exception {
        // Exact snippet from JavaBlockPMSM.ipes
        String pmsmSnippet = """
            double alpha, beta, d, q, theta;

            d=xIN[0];
            q=xIN[1];
            theta=xIN[2];

            alpha= d*Math.cos(theta) - q*Math.sin(theta);
            beta = d*Math.sin(theta) + q*Math.cos(theta);

            yOUT[0]=alpha;
            yOUT[1]=beta;
            return yOUT;
            """;

        ScriptBlockCalculator calc = new ScriptBlockCalculator(3, 2, pmsmSnippet);
        calc.setInputSignal(0, new ConstantCalculator(10.0), 0); // d = 10
        calc.setInputSignal(1, new ConstantCalculator(5.0), 0);  // q = 5
        calc.setInputSignal(2, new ConstantCalculator(0.0), 0);  // theta = 0

        calc.calculateYOUT(1e-6);
        // At theta = 0: alpha = 10*1 - 5*0 = 10; beta = 10*0 + 5*1 = 5
        assertEquals(10.0, calc._outputSignal[0][0], 1e-12);
        assertEquals(5.0, calc._outputSignal[1][0], 1e-12);

        // At theta = pi/2: alpha = 10*0 - 5*1 = -5; beta = 10*1 + 5*0 = 10
        calc._inputSignal[2][0] = Math.PI / 2;
        calc.calculateYOUT(1e-6);
        assertEquals(-5.0, calc._outputSignal[0][0], 1e-12);
        assertEquals(10.0, calc._outputSignal[1][0], 1e-12);
    }
}
