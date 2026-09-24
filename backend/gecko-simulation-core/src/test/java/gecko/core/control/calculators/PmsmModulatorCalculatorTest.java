/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PmsmModulatorCalculatorTest {

    private PmsmModulatorCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PmsmModulatorCalculator();
        for (int i = 0; i < 4; i++) {
            calculator._inputSignal[i] = new double[1];
        }
    }

    private void setInputs(double vAlpha, double vBeta, double triangle, double vDc) {
        calculator._inputSignal[0][0] = vAlpha;
        calculator._inputSignal[1][0] = vBeta;
        calculator._inputSignal[2][0] = triangle;
        calculator._inputSignal[3][0] = vDc;
    }

    @Test
    void initialization_hasFourInputsAndThreeOutputs() {
        assertEquals(4, calculator._inputSignal.length);
        assertEquals(3, calculator._outputSignal.length);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -10.0, -400.0, Double.NaN})
    void calculateYOUT_nonPositiveOrInvalidVdc_outputsZeroWithoutCrashing(double vdc) {
        setInputs(100.0, 50.0, 0.5, vdc);
        calculator.calculateYOUT(1e-6);

        assertEquals(0.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[2][0], 1e-9);
    }

    @Test
    void calculateYOUT_zeroReferenceVector_symmetricDutyCycleAtMidpoint() {
        // Zero reference vector: dwell times deltaVector1=0, deltaVector2=0, deltaZero=0.5
        // Comparison thresholds are all 0.5
        final double vDc = 560.0;
        final double deltaT = 1e-6;

        // Carrier below 0.5 -> all switches HIGH (active state 1, 1, 1)
        setInputs(0.0, 0.0, 0.2, vDc);
        calculator.calculateYOUT(deltaT);
        assertEquals(1.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(1.0, calculator._outputSignal[1][0], 1e-9);
        assertEquals(1.0, calculator._outputSignal[2][0], 1e-9);

        // Carrier above 0.5 -> all switches LOW (zero state 0, 0, 0)
        setInputs(0.0, 0.0, 0.8, vDc);
        calculator.calculateYOUT(deltaT);
        assertEquals(0.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[2][0], 1e-9);
    }

    @Test
    void calculateYOUT_allSixSectors_producesBinarySwitchingSignals() {
        final double vDc = 600.0;
        final double radius = 200.0;
        final double deltaT = 1e-6;

        // Test angles in sectors 0 to 5: 30°, 90°, 150°, 210°, 270°, 330°
        final double[] testAnglesDeg = {30.0, 90.0, 150.0, 210.0, 270.0, 330.0};

        for (double angleDeg : testAnglesDeg) {
            final double rad = Math.toRadians(angleDeg);
            final double vAlpha = radius * Math.cos(rad);
            final double vBeta = radius * Math.sin(rad);

            // Test carrier at bottom, middle, and top of triangle
            for (double triangle : new double[]{0.0, 0.3, 0.5, 0.7, 1.0}) {
                setInputs(vAlpha, vBeta, triangle, vDc);
                calculator.calculateYOUT(deltaT);

                final double u = calculator._outputSignal[0][0];
                final double v = calculator._outputSignal[1][0];
                final double w = calculator._outputSignal[2][0];

                assertTrue(u == 0.0 || u == 1.0, "U must be binary at " + angleDeg + " deg: " + u);
                assertTrue(v == 0.0 || v == 1.0, "V must be binary at " + angleDeg + " deg: " + v);
                assertTrue(w == 0.0 || w == 1.0, "W must be binary at " + angleDeg + " deg: " + w);
            }
        }
    }

    @Test
    void calculateYOUT_overmodulation_clampsMagnitudeSafely() {
        final double vDc = 400.0;
        // Request magnitude 800V > vDc 400V
        setInputs(800.0, 0.0, 0.5, vDc);
        calculator.calculateYOUT(1e-6);

        final double u = calculator._outputSignal[0][0];
        final double v = calculator._outputSignal[1][0];
        final double w = calculator._outputSignal[2][0];

        assertTrue(u == 0.0 || u == 1.0);
        assertTrue(v == 0.0 || v == 1.0);
        assertTrue(w == 0.0 || w == 1.0);
    }

    @Test
    void calculateYOUT_carrierExtremes_consistentComplementaryOutput() {
        final double vDc = 500.0;
        final double vAlpha = 150.0;
        final double vBeta = 100.0;

        // At carrier = 0.0 (minimum), all PWM signals are strictly 1.0
        setInputs(vAlpha, vBeta, 0.0, vDc);
        calculator.calculateYOUT(1e-6);
        assertEquals(1.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(1.0, calculator._outputSignal[1][0], 1e-9);
        assertEquals(1.0, calculator._outputSignal[2][0], 1e-9);

        // At carrier = 1.0 (maximum), all PWM signals are strictly 0.0
        setInputs(vAlpha, vBeta, 1.0, vDc);
        calculator.calculateYOUT(1e-6);
        assertEquals(0.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[2][0], 1e-9);
    }

    @Test
    void calculateYOUT_exactPiBoundary_robustAcrossDiscontinuity() {
        final double vDc = 500.0;
        final double deltaT = 1e-6;

        // Exactly at pi: atan2(+0.0, -200.0) == +pi. angle / (pi/3) rounds to exactly 3.0,
        // so the refactored computation assigns sector 3 with a relative angle of ~0 and
        // dwell times deltaVector1 = 0.75*M, deltaVector2 = 0. At carrier 0.5 the correct
        // dwell mapping yields the switching state (U,V,W) = (1,0,0): the comparison
        // thresholds 0.5*(1 -+ 0.75*M) = 0.327/0.673 are far away from the carrier value.
        setInputs(-200.0, 0.0, 0.5, vDc);
        calculator.calculateYOUT(deltaT);
        assertEquals(1.0, calculator._outputSignal[0][0], 1e-9, "U at exact pi, carrier 0.5");
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9, "V at exact pi, carrier 0.5");
        assertEquals(0.0, calculator._outputSignal[2][0], 1e-9, "W at exact pi, carrier 0.5");

        // Carrier extremes at the boundary must stay fully complementary
        setInputs(-200.0, 0.0, 0.0, vDc);
        calculator.calculateYOUT(deltaT);
        assertEquals(1.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(1.0, calculator._outputSignal[1][0], 1e-9);
        assertEquals(1.0, calculator._outputSignal[2][0], 1e-9);

        setInputs(-200.0, 0.0, 1.0, vDc);
        calculator.calculateYOUT(deltaT);
        assertEquals(0.0, calculator._outputSignal[0][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9);
        assertEquals(0.0, calculator._outputSignal[2][0], 1e-9);

        // Continuity across the discontinuity: just below pi (sector 2) and just above pi
        // (sector 3 after wrap-around) the reference sits on the same boundary vector, so
        // both adjacent sectors must produce the identical switching state at carrier 0.5.
        final double eps = 1e-4;
        setInputs(-200.0, eps, 0.5, vDc);
        calculator.calculateYOUT(deltaT);
        assertEquals(1.0, calculator._outputSignal[0][0], 1e-9, "U just below pi");
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9, "V just below pi");
        assertEquals(0.0, calculator._outputSignal[2][0], 1e-9, "W just below pi");

        setInputs(-200.0, -eps, 0.5, vDc);
        calculator.calculateYOUT(deltaT);
        assertEquals(1.0, calculator._outputSignal[0][0], 1e-9, "U just above pi");
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9, "V just above pi");
        assertEquals(0.0, calculator._outputSignal[2][0], 1e-9, "W just above pi");
    }
}
