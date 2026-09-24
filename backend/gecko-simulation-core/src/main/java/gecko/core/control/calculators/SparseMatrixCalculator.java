/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.control.calculators;

/**
 * Space Vector Pulse-Width Modulator (SVPWM) for an Indirect Sparse Matrix Converter (ISMC).
 *
 * <p>The Indirect Sparse Matrix Converter performs direct AC-AC power conversion through a two-stage
 * topology consisting of an input current-source rectifier and an output voltage-source inverter,
 * without bulky intermediate electrolytic DC-link energy storage capacitors.</p>
 *
 * <p><strong>Input Signals (8):</strong></p>
 * <ul>
 *   <li>Signal 0: {@code fDR} - Carrier / switching frequency reference for pulse period detection</li>
 *   <li>Signal 1: {@code ur} - Input grid phase R instantaneous voltage</li>
 *   <li>Signal 2: {@code us} - Input grid phase S instantaneous voltage</li>
 *   <li>Signal 3: {@code ut} - Input grid phase T instantaneous voltage</li>
 *   <li>Signal 4: {@code uNmax} - Peak amplitude of the input AC mains voltage</li>
 *   <li>Signal 5: {@code uOUTmax} - Desired peak output voltage amplitude</li>
 *   <li>Signal 6: {@code fOUT} - Desired fundamental output frequency in Hz</li>
 *   <li>Signal 7: {@code phi2} - Output voltage vector reference angle in radians (used when {@code fOUT <= 0})</li>
 * </ul>
 *
 * <p><strong>Output Signals (9):</strong></p>
 * <ul>
 *   <li>Signals 0..5: {@code sRp, sSp, sTp, sRm, sSm, sTm} - Rectifier stage gate switching commands (0.0 or 1.0)</li>
 *   <li>Signals 6..8: {@code s1, s2, s3} - Inverter stage gate switching commands (0.0 or 1.0)</li>
 * </ul>
 *
 * <p>The modulation algorithm identifies the active 30-degree sector (1 to 12) for both the input
 * grid voltage vectors and output reference voltage vectors, computes the respective active vector
 * duty ratios, and synthesizes symmetrical gate pulses with zero-current switching (ZCS) transitions
 * in the rectifier stage.</p>
 */
public final class SparseMatrixCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {

    private static final int NO_INPUTS = 8;
    private static final int NO_OUTPUTS = 9;

    private static final double NUMERIC_EPSILON = 1e-12;
    private static final double DEFAULT_DUTY_RATIO = 0.5;
    private static final double DEFAULT_SWITCHING_FREQUENCY = 25_000.0; // 25 kHz
    private static final double DEFAULT_PULSE_PERIOD = 1.0 / DEFAULT_SWITCHING_FREQUENCY;
    private static final int PULSE_RESOLUTION = 1000;
    private static final int HALF_PULSE_RESOLUTION = PULSE_RESOLUTION / 2;
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double SECTOR_SPAN_OUTPUT = Math.PI / 3.0; // 60 degrees

    // Pulse period detection state
    private double fDrPreviousOld = 0.0;
    private double fDrPrevious = 0.0;
    private boolean newPulsePeriodBegins = true;
    private double localTime = 0.0;

    // Converter sector information (1..12)
    private int inputSector = -1;
    private int outputSector = -1;
    private double pulsePeriod = DEFAULT_PULSE_PERIOD;

    // Relative duty cycles: dutyIn=[da, db], dutyOut=[d1..d5]
    private final double[] dutyIn = new double[2];
    private final double[] dutyOut = new double[5];

    // Gate control signals (0.0 or 1.0)
    private double sRp;
    private double sSp;
    private double sTp;
    private double sRm;
    private double sSm;
    private double sTm;
    private double s1;
    private double s2;
    private double s3;

    public SparseMatrixCalculator() {
        super(NO_INPUTS, NO_OUTPUTS);
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        fDrPreviousOld = 0.0;
        fDrPrevious = 0.0;
        pulsePeriod = DEFAULT_PULSE_PERIOD;
        localTime = 0.0;
        newPulsePeriodBegins = true;
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        final double ur = _inputSignal[1][0];
        final double us = _inputSignal[2][0];
        final double ut = _inputSignal[3][0];
        final double uNmax = _inputSignal[4][0];
        final double uOUTmax = _inputSignal[5][0];
        final double fOUT = _inputSignal[6][0];
        final double fDR = _inputSignal[0][0];
        final double phi2 = _inputSignal[7][0];

        // Detect falling edge / peak transition of carrier frequency indicating a new pulse period
        if ((fDrPreviousOld < fDrPrevious) && (fDrPrevious > fDR)) {
            newPulsePeriodBegins = true;
        }
        fDrPreviousOld = fDrPrevious;
        fDrPrevious = fDR;

        if (newPulsePeriodBegins) {
            if (localTime != 0.0) {
                pulsePeriod = localTime;
            }
            localTime = 0.0;
            sectorDetection(ur, us, ut, fOUT, phi2);
            calculateSwitchingTimes(ur, us, ut, uNmax, uOUTmax, fOUT, phi2);
            newPulsePeriodBegins = false;
        }

        final double switchingFrequency = safeDivide(1.0, pulsePeriod, safeDivide(1.0, DEFAULT_PULSE_PERIOD, 0.0));
        setPulseWidths(dutyOut[0], dutyOut[1], dutyOut[2], dutyOut[3], dutyOut[4], dutyIn[0], dutyIn[1], switchingFrequency);
        localTime += deltaT;

        _outputSignal[0][0] = sRp;
        _outputSignal[1][0] = sSp;
        _outputSignal[2][0] = sTp;
        _outputSignal[3][0] = sRm;
        _outputSignal[4][0] = sSm;
        _outputSignal[5][0] = sTm;
        _outputSignal[6][0] = s1;
        _outputSignal[7][0] = s2;
        _outputSignal[8][0] = s3;
    }

    /**
     * Synthesizes the 9 gate signals for the current local time within the pulse period.
     *
     * @param d1 duty cycle of output vector 1
     * @param d2 duty cycle of output vector 2
     * @param d3 duty cycle of zero vector
     * @param d4 duty cycle of output vector 4
     * @param d5 duty cycle of output vector 5
     * @param da input duty ratio part A
     * @param db input duty ratio part B (1.0 - da)
     * @param fDR instantaneous switching frequency
     */
    public void setPulseWidths(final double d1, final double d2, final double d3, final double d4, final double d5,
                               final double da, final double db, final double fDR) {
        final double localX = PULSE_RESOLUTION * fDR * localTime;
        final int w = (int) (da * HALF_PULSE_RESOLUTION);

        // Rectifier stage switching pattern: one switch is clamped to 1, while the opposing rail switches
        switch (inputSector) {
            case 1 -> {
                sRp = 1.0; sSp = 0.0; sTp = 0.0;
                sRm = 0.0; sSm = centerPulse(localX, w); sTm = edgePulse(localX, w);
            }
            case 2 -> {
                sRp = edgePulse(localX, w); sSp = centerPulse(localX, w); sTp = 0.0;
                sRm = 0.0; sSm = 0.0; sTm = 1.0;
            }
            case 3 -> {
                sRp = centerPulse(localX, w); sSp = edgePulse(localX, w); sTp = 0.0;
                sRm = 0.0; sSm = 0.0; sTm = 1.0;
            }
            case 4 -> {
                sRp = 0.0; sSp = 1.0; sTp = 0.0;
                sRm = centerPulse(localX, w); sSm = 0.0; sTm = edgePulse(localX, w);
            }
            case 5 -> {
                sRp = 0.0; sSp = 1.0; sTp = 0.0;
                sRm = edgePulse(localX, w); sSm = 0.0; sTm = centerPulse(localX, w);
            }
            case 6 -> {
                sRp = 0.0; sSp = edgePulse(localX, w); sTp = centerPulse(localX, w);
                sRm = 1.0; sSm = 0.0; sTm = 0.0;
            }
            case 7 -> {
                sRp = 0.0; sSp = centerPulse(localX, w); sTp = edgePulse(localX, w);
                sRm = 1.0; sSm = 0.0; sTm = 0.0;
            }
            case 8 -> {
                sRp = 0.0; sSp = 0.0; sTp = 1.0;
                sRm = edgePulse(localX, w); sSm = centerPulse(localX, w); sTm = 0.0;
            }
            case 9 -> {
                sRp = 0.0; sSp = 0.0; sTp = 1.0;
                sRm = centerPulse(localX, w); sSm = edgePulse(localX, w); sTm = 0.0;
            }
            case 10 -> {
                sRp = centerPulse(localX, w); sSp = 0.0; sTp = edgePulse(localX, w);
                sRm = 0.0; sSm = 1.0; sTm = 0.0;
            }
            case 11 -> {
                sRp = edgePulse(localX, w); sSp = 0.0; sTp = centerPulse(localX, w);
                sRm = 0.0; sSm = 1.0; sTm = 0.0;
            }
            case 12 -> {
                sRp = 1.0; sSp = 0.0; sTp = 0.0;
                sRm = 0.0; sSm = edgePulse(localX, w); sTm = centerPulse(localX, w);
            }
            default -> {
                sRp = 0.0; sSp = 0.0; sTp = 0.0;
                sRm = 0.0; sSm = 0.0; sTm = 0.0;
            }
        }

        // Inverter stage switching pattern: symmetric double pulses
        final int pA = (int) (d1 * HALF_PULSE_RESOLUTION);
        final int pB = (int) ((1.0 - d5) * HALF_PULSE_RESOLUTION);
        final int pC = (int) ((d1 + d2) * HALF_PULSE_RESOLUTION);
        final int pD = (int) ((d1 + d2 + d3) * HALF_PULSE_RESOLUTION);

        switch (outputSector) {
            case 1 -> {
                s1 = 1.0;
                s2 = doublePulse(localX, pA, pB);
                s3 = doublePulse(localX, pC, pD);
            }
            case 2 -> {
                s1 = inverseDoublePulse(localX, pC, pD);
                s2 = inverseDoublePulse(localX, pA, pB);
                s3 = 0.0;
            }
            case 3 -> {
                s1 = inverseDoublePulse(localX, pA, pB);
                s2 = inverseDoublePulse(localX, pC, pD);
                s3 = 0.0;
            }
            case 4 -> {
                s1 = doublePulse(localX, pA, pB);
                s2 = 1.0;
                s3 = doublePulse(localX, pC, pD);
            }
            case 5 -> {
                s1 = doublePulse(localX, pC, pD);
                s2 = 1.0;
                s3 = doublePulse(localX, pA, pB);
            }
            case 6 -> {
                s1 = 0.0;
                s2 = inverseDoublePulse(localX, pC, pD);
                s3 = inverseDoublePulse(localX, pA, pB);
            }
            case 7 -> {
                s1 = 0.0;
                s2 = inverseDoublePulse(localX, pA, pB);
                s3 = inverseDoublePulse(localX, pC, pD);
            }
            case 8 -> {
                s1 = doublePulse(localX, pC, pD);
                s2 = doublePulse(localX, pA, pB);
                s3 = 1.0;
            }
            case 9 -> {
                s1 = doublePulse(localX, pA, pB);
                s2 = doublePulse(localX, pC, pD);
                s3 = 1.0;
            }
            case 10 -> {
                s1 = inverseDoublePulse(localX, pA, pB);
                s2 = 0.0;
                s3 = inverseDoublePulse(localX, pC, pD);
            }
            case 11 -> {
                s1 = inverseDoublePulse(localX, pC, pD);
                s2 = 0.0;
                s3 = inverseDoublePulse(localX, pA, pB);
            }
            case 12 -> {
                s1 = 1.0;
                s2 = doublePulse(localX, pC, pD);
                s3 = doublePulse(localX, pA, pB);
            }
            default -> {
                s1 = 0.0;
                s2 = 0.0;
                s3 = 0.0;
            }
        }
    }

    /**
     * Generates a symmetrical center pulse of width (1000 - 2 * width) centered at 500.
     */
    private static double centerPulse(final double localX, final int width) {
        return (localX >= width && localX < (PULSE_RESOLUTION - width)) ? 1.0 : 0.0;
    }

    /**
     * Generates symmetrical edge pulses at [0, width) and [1000 - width, 1000).
     */
    private static double edgePulse(final double localX, final int width) {
        return (localX < width || localX >= (PULSE_RESOLUTION - width)) ? 1.0 : 0.0;
    }

    /**
     * Generates a symmetrical double pulse active on [a, b) and [1000 - b, 1000 - a).
     */
    private static double doublePulse(final double localX, final int a, final int b) {
        return ((localX >= a && localX < b) || (localX >= (PULSE_RESOLUTION - b) && localX < (PULSE_RESOLUTION - a))) ? 1.0 : 0.0;
    }

    /**
     * Generates the complement of a symmetrical double pulse.
     */
    private static double inverseDoublePulse(final double localX, final int a, final int b) {
        return 1.0 - doublePulse(localX, a, b);
    }

    private void calculateSwitchingTimes(final double ur, final double us, final double ut,
                                        final double uNmax, final double uOUTmax, final double fOUT, final double phi2) {
        switch (inputSector) {
            case 1, 7 -> dutyIn[0] = safeDutyRatio(-ut, ur);
            case 2, 8 -> dutyIn[0] = safeDutyRatio(-ur, ut);
            case 3, 9 -> dutyIn[0] = safeDutyRatio(-us, ut);
            case 4, 10 -> dutyIn[0] = safeDutyRatio(-ut, us);
            case 5, 11 -> dutyIn[0] = safeDutyRatio(-ur, us);
            case 6, 12 -> dutyIn[0] = safeDutyRatio(-us, ur);
            default -> dutyIn[0] = DEFAULT_DUTY_RATIO;
        }
        dutyIn[0] = clampDutyRatio(dutyIn[0]);
        dutyIn[1] = 1.0 - dutyIn[0];

        // Output stage duty cycle calculation
        final double k = safeDivide(uOUTmax, uNmax * uNmax, 0.0) / SQRT_3;
        double phiOUT = (TWO_PI * fOUT * getSimulationTime()) - (Math.PI / 2.0);
        if (fOUT <= 0.0) {
            phiOUT = phi2 - (Math.PI / 2.0);
        }
        while (phiOUT >= SECTOR_SPAN_OUTPUT) {
            phiOUT -= SECTOR_SPAN_OUTPUT;
        }

        final double ua = sanitizeFinite(k * Math.cos(phiOUT + Math.PI / 6.0), 0.0);
        final double ub = sanitizeFinite(k * Math.sin(phiOUT), 0.0);

        double x1 = 0.0;
        double x2 = 0.0;
        switch (inputSector) {
            case 1 -> { x1 = -2.0 * ut; x2 = -2.0 * us; }
            case 2 -> { x1 = 2.0 * ur; x2 = 2.0 * us; }
            case 3 -> { x1 = 2.0 * us; x2 = 2.0 * ur; }
            case 4 -> { x1 = -2.0 * ut; x2 = -2.0 * ur; }
            case 5 -> { x1 = -2.0 * ur; x2 = -2.0 * ut; }
            case 6 -> { x1 = 2.0 * us; x2 = 2.0 * ut; }
            case 7 -> { x1 = 2.0 * ut; x2 = 2.0 * us; }
            case 8 -> { x1 = -2.0 * ur; x2 = -2.0 * us; }
            case 9 -> { x1 = -2.0 * us; x2 = -2.0 * ur; }
            case 10 -> { x1 = 2.0 * ut; x2 = 2.0 * ur; }
            case 11 -> { x1 = 2.0 * ur; x2 = 2.0 * ut; }
            case 12 -> { x1 = -2.0 * us; x2 = -2.0 * ut; }
            default -> {}
        }

        if (outputSector % 2 != 0) {
            dutyOut[0] = ua * x1;
            dutyOut[1] = ub * x1;
            dutyOut[3] = ub * x2;
            dutyOut[4] = ua * x2;
        } else {
            dutyOut[0] = ub * x1;
            dutyOut[1] = ua * x1;
            dutyOut[3] = ua * x2;
            dutyOut[4] = ub * x2;
        }
        dutyOut[2] = 1.0 - (dutyOut[0] + dutyOut[1] + dutyOut[3] + dutyOut[4]);
        sanitizeDutyArray(dutyOut);
    }

    private void sectorDetection(final double ur, final double us, final double ut, final double fOUT, final double phi2) {
        // Input voltage vector sector detection (30-degree sectors 1..12)
        if ((us <= 0.0) && (ut <= us)) {
            inputSector = 1;
        } else if ((us >= 0.0) && (ur >= us)) {
            inputSector = 2;
        } else if ((ur >= 0.0) && (us >= ur)) {
            inputSector = 3;
        } else if ((ur <= 0.0) && (ut <= ur)) {
            inputSector = 4;
        } else if ((ut <= 0.0) && (ur <= ut)) {
            inputSector = 5;
        } else if ((ut >= 0.0) && (us >= ut)) {
            inputSector = 6;
        } else if ((us >= 0.0) && (ut >= us)) {
            inputSector = 7;
        } else if ((us <= 0.0) && (ur <= us)) {
            inputSector = 8;
        } else if ((ur <= 0.0) && (us <= ur)) {
            inputSector = 9;
        } else if ((ur >= 0.0) && (ut >= ur)) {
            inputSector = 10;
        } else if ((ut >= 0.0) && (ur >= ut)) {
            inputSector = 11;
        } else if ((ut <= 0.0) && (us <= ut)) {
            inputSector = 12;
        }

        // Output voltage vector sector detection (30-degree sectors 1..12)
        double phiOUT = TWO_PI * fOUT * getSimulationTime();
        if (fOUT <= 0.0) {
            phiOUT = phi2;
        }
        final double u1 = Math.sin(phiOUT);
        final double u2 = Math.sin(phiOUT - 2.0 * Math.PI / 3.0);
        final double u3 = Math.sin(phiOUT - 4.0 * Math.PI / 3.0);

        if ((u2 < 0.0) && (u3 < u2)) {
            outputSector = 1;
        } else if ((u2 > 0.0) && (u1 > u2)) {
            outputSector = 2;
        } else if ((u1 > 0.0) && (u2 > u1)) {
            outputSector = 3;
        } else if ((u1 < 0.0) && (u3 < u1)) {
            outputSector = 4;
        } else if ((u3 < 0.0) && (u1 < u3)) {
            outputSector = 5;
        } else if ((u3 > 0.0) && (u2 > u3)) {
            outputSector = 6;
        } else if ((u2 > 0.0) && (u3 > u2)) {
            outputSector = 7;
        } else if ((u2 < 0.0) && (u1 < u2)) {
            outputSector = 8;
        } else if ((u1 < 0.0) && (u2 < u1)) {
            outputSector = 9;
        } else if ((u1 > 0.0) && (u3 > u1)) {
            outputSector = 10;
        } else if ((u3 > 0.0) && (u1 > u3)) {
            outputSector = 11;
        } else if ((u3 < 0.0) && (u2 < u3)) {
            outputSector = 12;
        }
    }

    private double safeDutyRatio(final double numerator, final double denominator) {
        return clampDutyRatio(safeDivide(numerator, denominator, DEFAULT_DUTY_RATIO));
    }

    private double clampDutyRatio(final double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_DUTY_RATIO;
        }
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private double safeDivide(final double numerator, final double denominator, final double fallback) {
        if (!Double.isFinite(numerator) || !Double.isFinite(denominator) || Math.abs(denominator) <= NUMERIC_EPSILON) {
            return fallback;
        }
        return sanitizeFinite(numerator / denominator, fallback);
    }

    private double sanitizeFinite(final double value, final double fallback) {
        if (Double.isFinite(value)) {
            return value;
        }
        return fallback;
    }

    private void sanitizeDutyArray(final double[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = sanitizeFinite(values[i], 0.0);
        }
    }

    // Package-private accessors for testing and diagnostic inspection
    int getInputSector() {
        return inputSector;
    }

    int getOutputSector() {
        return outputSector;
    }

    double[] getDutyIn() {
        return dutyIn.clone();
    }

    double[] getDutyOut() {
        return dutyOut.clone();
    }

    double getPulsePeriod() {
        return pulsePeriod;
    }
}
