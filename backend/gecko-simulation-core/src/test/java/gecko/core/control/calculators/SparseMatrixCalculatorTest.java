/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparseMatrixCalculatorTest {

    private SparseMatrixCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SparseMatrixCalculator();
        for (int i = 0; i < 8; i++) {
            calculator._inputSignal[i] = new double[1];
        }
    }

    private void setInputs(double fDr, double ur, double us, double ut,
                           double uNmax, double uOutMax, double fOut, double phi2) {
        calculator._inputSignal[0][0] = fDr;
        calculator._inputSignal[1][0] = ur;
        calculator._inputSignal[2][0] = us;
        calculator._inputSignal[3][0] = ut;
        calculator._inputSignal[4][0] = uNmax;
        calculator._inputSignal[5][0] = uOutMax;
        calculator._inputSignal[6][0] = fOut;
        calculator._inputSignal[7][0] = phi2;
    }

    @Test
    void initialization_hasEightInputsAndNineOutputs() {
        assertEquals(8, calculator._inputSignal.length);
        assertEquals(9, calculator._outputSignal.length);
        assertEquals(1.0 / 25000.0, calculator.getPulsePeriod(), 1e-9);
    }

    @Test
    void initializeAtSimulationStart_resetsPulseState() {
        setInputs(100.0, 325.0, -162.5, -162.5, 325.0, 230.0, 50.0, 0.0);
        calculator.calculateYOUT(1e-5);

        calculator.initializeAtSimulationStart(1e-6);
        assertEquals(1.0 / 25000.0, calculator.getPulsePeriod(), 1e-9);
    }

    @Test
    void sectorDetection_allTwelveInputSectors_detectedCorrectly() {
        final double deltaT = 1e-6;
        final double[][] sectorVoltages = {
            // Sector 1: (us <= 0) && (ut <= us)
            {0.866, 0.0, -0.866},
            // Sector 2: (us >= 0) && (ur >= us)
            {0.5, 0.5, -1.0},
            // Sector 3: (ur >= 0) && (us >= ur)
            {0.0, 0.866, -0.866},
            // Sector 4: (ur <= 0) && (ut <= ur)
            {-0.5, 1.0, -0.5},
            // Sector 5: (ut <= 0) && (ur <= ut)
            {-0.866, 0.866, 0.0},
            // Sector 6: (ut >= 0) && (us >= ut)
            {-1.0, 0.5, 0.5},
            // Sector 7: (us >= 0) && (ut >= us)
            {-0.866, 0.0, 0.866},
            // Sector 8: (us <= 0) && (ur <= us)
            {-0.5, -0.5, 1.0},
            // Sector 9: (ur <= 0) && (us <= ur)
            {0.0, -0.866, 0.866},
            // Sector 10: (ur >= 0) && (ut >= ur)
            {0.5, -1.0, 0.5},
            // Sector 11: (ut >= 0) && (ur >= ut)
            {0.866, -0.866, 0.0},
            // Sector 12: (ut <= 0) && (us <= ut)
            {1.0, -0.6, -0.4}
        };

        for (int expectedSector = 1; expectedSector <= 12; expectedSector++) {
            final double[] v = sectorVoltages[expectedSector - 1];
            // Carrier edge pattern to trigger pulse period
            setInputs(0.0, v[0], v[1], v[2], 325.0, 230.0, 50.0, 0.0);
            calculator.calculateYOUT(deltaT);

            setInputs(1.0, v[0], v[1], v[2], 325.0, 230.0, 50.0, 0.0);
            calculator.calculateYOUT(deltaT);

            setInputs(0.0, v[0], v[1], v[2], 325.0, 230.0, 50.0, 0.0);
            calculator.calculateYOUT(deltaT);

            assertEquals(expectedSector, calculator.getInputSector(),
                    "Failed for input sector " + expectedSector);
        }
    }

    @Test
    void pulseGeneration_rectifierStageClampingAndComplementarity() {
        // In all valid operating states, exactly one top switch and one bottom switch are active
        final double deltaT = 1e-6;
        final double uNmax = 325.0;
        final double uOutMax = 200.0;

        // Run across a full electric cycle of input voltages
        for (int angleDeg = 0; angleDeg < 360; angleDeg += 15) {
            final double rad = Math.toRadians(angleDeg);
            final double ur = uNmax * Math.cos(rad);
            final double us = uNmax * Math.cos(rad - 2.0 * Math.PI / 3.0);
            final double ut = uNmax * Math.cos(rad - 4.0 * Math.PI / 3.0);

            // Trigger new pulse period
            setInputs(0.0, ur, us, ut, uNmax, uOutMax, 50.0, 0.0);
            calculator.calculateYOUT(deltaT);
            setInputs(1.0, ur, us, ut, uNmax, uOutMax, 50.0, 0.0);
            calculator.calculateYOUT(deltaT);
            setInputs(0.0, ur, us, ut, uNmax, uOutMax, 50.0, 0.0);
            calculator.calculateYOUT(deltaT);

            // Step through several fractions of the pulse period
            for (int step = 0; step < 10; step++) {
                calculator.calculateYOUT(deltaT);

                final double sRp = calculator._outputSignal[0][0];
                final double sSp = calculator._outputSignal[1][0];
                final double sTp = calculator._outputSignal[2][0];
                final double sRm = calculator._outputSignal[3][0];
                final double sSm = calculator._outputSignal[4][0];
                final double sTm = calculator._outputSignal[5][0];

                // Positive rail: exactly one switch conducts
                final double topSum = sRp + sSp + sTp;
                assertEquals(1.0, topSum, 1e-6, "Top rail must have exactly 1 active switch at angle " + angleDeg);

                // Negative rail: exactly one switch conducts
                final double bottomSum = sRm + sSm + sTm;
                assertEquals(1.0, bottomSum, 1e-6, "Bottom rail must have exactly 1 active switch at angle " + angleDeg);

                // Inverter signals must be binary 0 or 1
                for (int inv = 6; inv <= 8; inv++) {
                    final double s = calculator._outputSignal[inv][0];
                    assertTrue(s == 0.0 || s == 1.0, "Inverter switch " + inv + " must be binary: " + s);
                }
            }
        }
    }

    @Test
    void calculateSwitchingTimes_dutyRatiosNormalized() {
        final double deltaT = 1e-6;
        setInputs(0.0, 325.0, -162.5, -162.5, 325.0, 200.0, 50.0, 0.0);
        calculator.calculateYOUT(deltaT);
        setInputs(1.0, 325.0, -162.5, -162.5, 325.0, 200.0, 50.0, 0.0);
        calculator.calculateYOUT(deltaT);
        setInputs(0.0, 325.0, -162.5, -162.5, 325.0, 200.0, 50.0, 0.0);
        calculator.calculateYOUT(deltaT);

        final double[] dutyIn = calculator.getDutyIn();
        assertEquals(1.0, dutyIn[0] + dutyIn[1], 1e-9);
        assertTrue(dutyIn[0] >= 0.0 && dutyIn[0] <= 1.0);
        assertTrue(dutyIn[1] >= 0.0 && dutyIn[1] <= 1.0);

        final double[] dutyOut = calculator.getDutyOut();
        for (double d : dutyOut) {
            assertTrue(Double.isFinite(d), "Duty out elements must be finite");
        }
    }

    @Test
    void pulsePeriodDetection_fallingEdgeUpdatesPeriod() {
        final double deltaT = 1e-5;
        // Step 1: Carrier at 0.0 -> sets previous state and advances localTime by deltaT (1e-5)
        setInputs(0.0, 325.0, -162.5, -162.5, 325.0, 200.0, 50.0, 0.0);
        calculator.calculateYOUT(deltaT);

        // Step 2: Rising edge to 1.0 -> advances localTime by deltaT (accumulated 2e-5)
        setInputs(1.0, 325.0, -162.5, -162.5, 325.0, 200.0, 50.0, 0.0);
        calculator.calculateYOUT(deltaT);

        // Step 3: Falling edge to 0.0: old (0.0) < prev (1.0) && prev (1.0) > current (0.0)
        // Detects peak, triggers new pulse period, and captures accumulated localTime (2e-5)
        setInputs(0.0, 325.0, -162.5, -162.5, 325.0, 200.0, 50.0, 0.0);
        calculator.calculateYOUT(deltaT);

        assertEquals(2e-5, calculator.getPulsePeriod(), 1e-12,
                "Pulse period must be updated to the measured localTime of 2e-5 s");
    }

    @Test
    void pulseGeneration_goldenTestForSpecificSectorAndTimingPoints() {
        // Deterministic simulation time so output-stage results do not depend on legacy static time
        calculator.setSimulationTime(0.0);
        final double deltaT = 1e-6;

        // Input sector 1 voltages (us <= 0 && ut <= us) with -ut/ur = 0.3 -> input duty da = 0.3
        final double ur = 1.0;
        final double us = 0.0;
        final double ut = -0.3;

        // Capture a pulse period of 2e-5 s: 19 steps at fDR=0, one step at fDR=1, then the
        // falling edge. The edge captures the accumulated localTime (20 * deltaT) as the period.
        setInputs(0.0, ur, us, ut, 325.0, 200.0, 50.0, 0.0);
        for (int i = 0; i < 19; i++) {
            calculator.calculateYOUT(deltaT);
        }
        setInputs(1.0, ur, us, ut, 325.0, 200.0, 50.0, 0.0);
        calculator.calculateYOUT(deltaT);
        setInputs(0.0, ur, us, ut, 325.0, 200.0, 50.0, 0.0);
        calculator.calculateYOUT(deltaT);

        assertEquals(1, calculator.getInputSector());
        assertEquals(2e-5, calculator.getPulsePeriod(), 1e-12);

        final double[] dutyIn = calculator.getDutyIn();
        assertEquals(0.3, dutyIn[0], 1e-12, "da = -ut/ur in sector 1");
        assertEquals(0.7, dutyIn[1], 1e-12, "db = 1 - da");

        // The new period starts at the edge with localX = 0. In sector 1 the top switch sRp is
        // clamped to 1 and the opposing rail alternates: sSm is the center pulse, high on
        // localX in [150, 850) for da = 0.3, and sTm is its edge-pulse complement.
        assertEquals(1.0, calculator._outputSignal[0][0], 1e-9, "sRp clamped to 1 in sector 1");
        assertEquals(0.0, calculator._outputSignal[1][0], 1e-9, "sSp = 0 in sector 1");
        assertEquals(0.0, calculator._outputSignal[2][0], 1e-9, "sTp = 0 in sector 1");
        assertEquals(0.0, calculator._outputSignal[3][0], 1e-9, "sRm = 0 in sector 1");
        assertEquals(0.0, calculator._outputSignal[4][0], 1e-9, "sSm = 0 at localX = 0");
        assertEquals(1.0, calculator._outputSignal[5][0], 1e-9, "sTm = 1 at localX = 0");

        // Sweep the captured period: localX = 1000 * (1/2e-5) * localTime advances 50 per step.
        // Skip k = 3 and k = 17: their localX values (150, 850) sit on the flip thresholds,
        // where floating-point noise of the accumulated localTime decides which side is hit.
        for (int k = 1; k <= 18; k++) {
            calculator.calculateYOUT(deltaT);
            final double localX = 50.0 * k;
            if (localX == 150.0 || localX == 850.0) {
                continue;
            }
            final boolean centerActive = localX > 150.0 && localX < 850.0;
            assertEquals(centerActive ? 1.0 : 0.0, calculator._outputSignal[4][0], 1e-9,
                    "sSm at localX=" + localX);
            assertEquals(centerActive ? 0.0 : 1.0, calculator._outputSignal[5][0], 1e-9,
                    "sTm at localX=" + localX);
            assertEquals(1.0, calculator._outputSignal[0][0], 1e-9, "sRp clamped to 1");
            assertEquals(0.0, calculator._outputSignal[1][0], 1e-9, "sSp = 0");
            assertEquals(0.0, calculator._outputSignal[2][0], 1e-9, "sTp = 0");
            assertEquals(0.0, calculator._outputSignal[3][0], 1e-9, "sRm = 0");
        }
    }
}
