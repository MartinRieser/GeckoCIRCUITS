/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 */
package gecko.geckocircuits.newscope;

import gecko.geckocircuits.datacontainer.DataContainerSimple;
import gecko.geckoscript.GeckoInvalidArgumentException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for CharacteristicsCalculator.
 * Validates signal characteristic computations (RMS, AVG, THD, Ripple, etc.)
 * using known analytical waveshapes.
 */
public class CharacteristicsCalculatorTest {

    private static final double DELTA = 1e-3; // standard tolerance for floating point computations

    /**
     * Test characteristics calculation for a constant DC signal.
     * DC signal: v(t) = 5.0
     */
    @Test
    public void testDCCharacteristics() {
        int steps = 1000;
        double dt = 1e-4; // 0.1 ms
        double totalTime = (steps - 1) * dt;

        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            double time = i * dt;
            container.insertValuesAtEnd(new float[]{5.0f}, time);
        }

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(container, 0.0, totalTime);

        assertNotNull("Calculator should not be null", calc);
        assertTrue("Calculator should be valid", calc.isValid());
        assertEquals("AVG of 5.0 DC should be 5.0", 5.0, calc.getAVGValue(0), DELTA);
        assertEquals("RMS of 5.0 DC should be 5.0", 5.0, calc.getRMS2Value(0), DELTA);
        assertEquals("Min of 5.0 DC should be 5.0", 5.0, calc.getMinValue(0), DELTA);
        assertEquals("Max of 5.0 DC should be 5.0", 5.0, calc.getMaxValue(0), DELTA);
        assertEquals("Peak-to-peak should be 0.0", 0.0, calc.getPeakToPeakValue(0), DELTA);
        assertEquals("Ripple should be 0.0", 0.0, calc.getRippleValue(0), DELTA);
        assertEquals("Shape factor matches CharacteristicsCalculator's implementation", 1.0e-4, calc.getShapeValue(0), 1e-6);
    }

    /**
     * Test characteristics calculation for a pure sine wave.
     * Sine wave: v(t) = 10 * sin(2 * pi * 50 * t)
     * Frequency: 50 Hz, Period: 20 ms
     */
    @Test
    public void testSineWaveCharacteristics() {
        int steps = 1001;
        double dt = 2e-5; // 20 us
        double period = 0.02; // one full cycle (20 ms)

        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            double time = i * dt;
            float val = (float) (10.0 * Math.sin(2 * Math.PI * 50.0 * time));
            container.insertValuesAtEnd(new float[]{val}, time);
        }

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(container, 0.0, period);

        assertNotNull(calc);
        // Average value of a full sine wave is 0
        assertEquals("Average should be 0.0", 0.0, calc.getAVGValue(0), DELTA);
        // RMS value of a sine wave is peak / sqrt(2) = 10 / 1.414 = 7.071
        assertEquals("RMS should be 7.071", 7.071, calc.getRMS2Value(0), DELTA);
        assertEquals("Max should be 10.0", 10.0, calc.getMaxValue(0), DELTA);
        assertEquals("Min should be -10.0", -10.0, calc.getMinValue(0), DELTA);
        assertEquals("Peak-to-Peak should be 20.0", 20.0, calc.getPeakToPeakValue(0), DELTA);
    }

    /**
     * Test characteristics calculation for a square wave.
     * Square wave: alternating between +5V and -5V
     * Period: 20 ms
     */
    @Test
    public void testSquareWaveCharacteristics() {
        int steps = 1001;
        double dt = 2e-5; // 20 us
        double period = 0.02;

        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            double time = i * dt;
            float val = (time % period < period / 2.0) ? 5.0f : -5.0f;
            container.insertValuesAtEnd(new float[]{val}, time);
        }

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(container, 0.0, period);

        assertNotNull(calc);
        assertEquals("Average of symmetric square wave should be 0.0", 0.0, calc.getAVGValue(0), DELTA);
        assertEquals("RMS of square wave should be peak value", 5.0, calc.getRMS2Value(0), DELTA);
        assertEquals("Max should be 5.0", 5.0, calc.getMaxValue(0), DELTA);
        assertEquals("Min should be -5.0", -5.0, calc.getMinValue(0), DELTA);
        assertEquals("Peak-to-Peak should be 10.0", 10.0, calc.getPeakToPeakValue(0), DELTA);
    }

    /**
     * Test out of bounds inputs and exception handling.
     */
    @Test
    public void testBoundsHandling() {
        int steps = 100;
        double dt = 1e-4;

        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            container.insertValuesAtEnd(new float[]{3.0f}, i * dt);
        }

        // Test start > end boundary calculation
        try {
            CharacteristicsCalculator.calculateFabric(container, 0.01, 0.005);
            fail("Expected RuntimeException for lower bound > upper bound");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Lower bound > Upper bound"));
        }

        // Test start < 0 (lower bound out of bounds)
        try {
            CharacteristicsCalculator.calculateFabric(container, -0.01, 0.005);
            fail("Expected IndexOutOfBoundsException for invalid lower bound");
        } catch (IndexOutOfBoundsException e) {
            assertTrue(e.getMessage().contains("bounds") || e.getMessage().contains("lower bound"));
        }
    }

    /**
     * Test channel characteristics array extraction.
     */
    @Test
    public void testChannelCharacteristics() throws GeckoInvalidArgumentException {
        int steps = 100;
        double dt = 1e-4;

        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(2, steps);
        for (int i = 0; i < steps; i++) {
            container.insertValuesAtEnd(new float[]{2.0f, 4.0f}, i * dt);
        }

        CharacteristicsCalculator calc = CharacteristicsCalculator.calculateFabric(container, 0.0, 99 * dt);

        double[] chan0 = calc.getChannelCharacteristics(0);
        double[] chan1 = calc.getChannelCharacteristics(1);

        assertNotNull(chan0);
        assertNotNull(chan1);
        assertEquals("Channel 0 AVG should be 2.0", 2.0, chan0[0], DELTA);
        assertEquals("Channel 1 AVG should be 4.0", 4.0, chan1[0], DELTA);

        // Test non-existent channel access
        try {
            calc.getChannelCharacteristics(2);
            fail("Expected GeckoInvalidArgumentException for non-existent channel");
        } catch (GeckoInvalidArgumentException e) {
            assertTrue(e.getMessage().contains("non-existant"));
        }
    }

    /**
     * Test caching behavior of calculateFabric.
     */
    @Test
    public void testValueCaching() {
        int steps = 100;
        double dt = 1e-4;

        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            container.insertValuesAtEnd(new float[]{1.0f}, i * dt);
        }

        CharacteristicsCalculator calc1 = CharacteristicsCalculator.calculateFabric(container, 0.0, 99 * dt);
        CharacteristicsCalculator calc2 = CharacteristicsCalculator.calculateFabric(container, 0.0, 99 * dt);

        assertSame("Subsequent calls with same parameters should return the cached instance", calc1, calc2);
    }
}
