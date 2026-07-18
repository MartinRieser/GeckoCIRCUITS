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
 * Unit tests for FourierGUIless.
 * Validates discrete Fourier transform calculations for signal harmonics
 * without GUI components.
 */
public class FourierGUIlessTest {

    private static final double DELTA = 1e-2;

    /**
     * Test Fourier transformation of a constant DC signal.
     * DC signal: v(t) = 5.0
     * Expected: DC term (harmonic 0) has amplitude 5.0, other harmonics 0.
     */
    @Test
    public void testDCFourier() throws GeckoInvalidArgumentException {
        int steps = 256; // power of 2 for FFT
        double dt = 1e-4; // 0.1 ms
        double totalTime = (steps - 1) * dt;

        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            container.insertValuesAtEnd(new float[]{5.0f}, i * dt);
        }

        // Run Fourier analysis for up to 5 harmonics
        FourierGUIless fourier = new FourierGUIless(container, 0.0, totalTime, 5);
        double[][][] result = fourier.doFourier();

        assertNotNull("Result should not be null", result);
        assertEquals("Result should contain 4 matrix types (an, bn, cn, jn)", 4, result.length);

        double[][] an = result[0];
        double[][] bn = result[1];
        double[][] cn = result[2];

        // Harmonic 0: DC component
        // Note: evaluate() sets cn[0] = an[0] / 2
        assertEquals("DC component amplitude should be 5.0", 5.0, cn[0][0], DELTA);

        // Other harmonics should be 0
        for (int h = 1; h <= 5; h++) {
            assertEquals("Harmonic " + h + " amplitude should be 0.0", 0.0, cn[0][h], DELTA);
        }
    }

    /**
     * Test Fourier transformation of a sine wave.
     * Sine wave: v(t) = 10 * sin(2 * pi * 50 * t)
     * Period: 20 ms
     */
    @Test
    public void testSineWaveFourier() throws GeckoInvalidArgumentException {
        int steps = 512;
        double dt = 4e-5; // 40 us
        double period = 0.02; // one full cycle (20 ms)

        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            double time = i * dt;
            float val = (float) (10.0 * Math.sin(2 * Math.PI * 50.0 * time));
            container.insertValuesAtEnd(new float[]{val}, time);
        }

        FourierGUIless fourier = new FourierGUIless(container, 0.0, period, 5);
        double[][][] result = fourier.doFourier();

        assertNotNull(result);
        double[][] cn = result[2]; // total amplitudes

        // Harmonic 1: fundamental frequency (50 Hz) amplitude should be ~10.0
        assertEquals("Fundamental harmonic amplitude should be 10.0", 10.0, cn[0][1], 0.2);

        // Other harmonics should be close to 0
        assertEquals("Harmonic 2 amplitude should be close to 0.0", 0.0, cn[0][2], 0.05);
        assertEquals("Harmonic 3 amplitude should be close to 0.0", 0.0, cn[0][3], 0.05);
    }

    /**
     * Test invalid parameters handling.
     */
    @Test
    public void testInvalidParameters() {
        int steps = 100;
        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            container.insertValuesAtEnd(new float[]{1.0f}, i * 1e-4);
        }

        // Test invalid range (startTime == endTime)
        FourierGUIless fourierInvalidRange = new FourierGUIless(container, 0.0, 0.0, 5);
        try {
            fourierInvalidRange.doFourier();
            fail("Expected IllegalArgumentException for invalid range");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("greater than 0"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }
}
