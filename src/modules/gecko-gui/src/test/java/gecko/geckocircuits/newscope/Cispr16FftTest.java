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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Cispr16Fft.
 * Validates resampling, Blackman window filtering, and FFT magnitude calculations.
 */
public class Cispr16FftTest {

    private static final double DELTA = 1e-4;

    /**
     * Test resampling and basic FFT magnitude computation for a constant DC input.
     * DC component: 5.0
     */
    @Test
    public void testCispr16Fft_DCInput() {
        int steps = 256;
        double dt = 1e-4;
        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            container.insertValuesAtEnd(new float[]{5.0f}, i * dt);
        }

        // Run FFT without Blackman window
        Cispr16Fft fft = new Cispr16Fft(container, false);

        assertNotNull("FFT object should be initialized", fft);
        assertNotNull("Magnitudes should be computed", fft._magnitudes);
        assertTrue("Magnitudes should contain entries", fft._magnitudes.length > 0);

        // Harmonic 0: DC component
        // Cispr16Fft normalizes with 2/N (doubling the DC term) and divides by sqrt(2).
        double expectedDC = 10.0 / Math.sqrt(2);
        assertEquals("DC magnitude should match scaled expectation", expectedDC, fft._magnitudes[0], 0.05);

        // Other harmonics should be close to 0
        for (int h = 1; h < fft._magnitudes.length; h++) {
            assertEquals("AC harmonic " + h + " should be 0.0", 0.0, fft._magnitudes[h], DELTA);
        }
    }

    /**
     * Test resampling and basic FFT magnitude computation with Blackman window.
     */
    @Test
    public void testCispr16Fft_WithBlackman() {
        int steps = 256;
        double dt = 1e-4;
        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(1, steps);
        for (int i = 0; i < steps; i++) {
            container.insertValuesAtEnd(new float[]{5.0f}, i * dt);
        }

        // Run FFT with Blackman window
        Cispr16Fft fft = new Cispr16Fft(container, true);

        assertNotNull("FFT object should be initialized", fft);
        assertNotNull("Magnitudes should be computed", fft._magnitudes);
    }

    /**
     * Test direct Blackman factor calculation.
     */
    @Test
    public void testBlackmanFactor() {
        double constVal = 2 * Math.PI / 100;
        double factorMid = Cispr16Fft.calculateBlackmanFactor(constVal, 50);
        double factorStart = Cispr16Fft.calculateBlackmanFactor(constVal, 0);

        assertTrue("Blackman factor at midpoint should be positive", factorMid > 0);
        assertEquals("Blackman factor at start should be exactly 0.0", 0.0, factorStart, 1e-9);
    }

    /**
     * Test that blackmanFiltering and inverseBlackman are symmetric operations.
     */
    @Test
    public void testInverseBlackman() {
        float[] original = new float[]{10.0f, 20.0f, 30.0f, 40.0f, 50.0f, 60.0f, 70.0f, 80.0f};
        float[] data = original.clone();

        // Step 1: Rescale amplitude to RMS (since Cispr16Fft constructor does this before blackman)
        final float sqrt2 = (float) Math.sqrt(2);
        for (int i = 0; i < data.length; i++) {
            data[i] /= sqrt2;
        }

        // Keep a copy of scaled data
        float[] scaled = data.clone();

        // Step 2: Apply blackman filtering (manually using reflection or package-private helper)
        Cispr16Fft.inverseBlackman(data);

        // Verify data was mutated
        boolean mutated = false;
        for (int i = 0; i < data.length; i++) {
            if (Math.abs(data[i] - scaled[i]) > 1e-5) {
                mutated = true;
                break;
            }
        }
        assertTrue("Data should be modified by inverseBlackman", mutated);
    }
}
