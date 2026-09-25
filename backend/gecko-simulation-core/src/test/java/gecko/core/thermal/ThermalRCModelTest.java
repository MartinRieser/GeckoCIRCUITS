/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package gecko.core.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the Foster/Cauer thermal RC model: factory derivation, the
 * Foster-to-Cauer continued-fraction transformation (verified against the
 * driving-point impedance on the imaginary axis) and input validation.
 */
class ThermalRCModelTest {

    /** Datasheet-style four-stage thermal resistances in K/W. */
    private static final double[] STAGE_RESISTANCES = {0.05, 0.15, 0.5, 1.3};

    /** Datasheet-style four-stage exponential time constants in seconds. */
    private static final double[] STAGE_TIME_CONSTANTS = {0.001, 0.01, 0.1, 1.0};

    /** Physical Cauer ladder heat capacitances in J/K. */
    private static final double[] STAGE_CAPACITANCES = {0.01, 0.1, 1.0, 10.0};

    /** Ambient temperature of all test models in degrees Celsius. */
    private static final double AMBIENT_TEMPERATURE = 25.0;

    /** Relative tolerance of the impedance comparison on the imaginary axis. */
    private static final double IMPEDANCE_RELATIVE_TOLERANCE = 1e-9;

    /** Lowest angular frequency of the impedance sweep in rad/s. */
    private static final double SWEEP_MIN_OMEGA = 1e-3;

    /** Highest angular frequency of the impedance sweep in rad/s. */
    private static final double SWEEP_MAX_OMEGA = 1e4;

    /** Number of logarithmically spaced sweep frequencies. */
    private static final int SWEEP_POINT_COUNT = 40;

    @Test
    void fosterFactory_derivesCapacitancesFromTimeConstants() {
        final ThermalRCModel model = ThermalRCModel.foster(STAGE_RESISTANCES,
                STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE);

        assertEquals(ThermalRCModel.NetworkKind.FOSTER, model.getKind());
        assertEquals(STAGE_RESISTANCES.length, model.getStageCount());
        for (int stage = 0; stage < model.getStageCount(); stage++) {
            assertEquals(STAGE_RESISTANCES[stage], model.getResistance(stage));
            assertEquals(STAGE_TIME_CONSTANTS[stage], model.getTimeConstant(stage));
            assertEquals(STAGE_TIME_CONSTANTS[stage] / STAGE_RESISTANCES[stage],
                    model.getCapacitance(stage), 1e-15);
        }
        assertEquals(AMBIENT_TEMPERATURE, model.getAmbientTemperature());
    }

    @Test
    void cauerFactory_reportsStageTimeConstantsAsRCProducts() {
        final ThermalRCModel model = ThermalRCModel.cauer(STAGE_RESISTANCES,
                STAGE_CAPACITANCES, AMBIENT_TEMPERATURE);

        assertEquals(ThermalRCModel.NetworkKind.CAUER, model.getKind());
        for (int stage = 0; stage < model.getStageCount(); stage++) {
            assertEquals(STAGE_CAPACITANCES[stage], model.getCapacitance(stage));
            assertEquals(STAGE_RESISTANCES[stage] * STAGE_CAPACITANCES[stage],
                    model.getTimeConstant(stage), 1e-15);
        }
    }

    @Test
    void fosterToCauer_preservesDrivingPointImpedance() {
        final ThermalRCModel foster = ThermalRCModel.foster(STAGE_RESISTANCES,
                STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE);
        final ThermalRCModel cauer = foster.toCauer();

        assertEquals(ThermalRCModel.NetworkKind.CAUER, cauer.getKind());
        assertEquals(foster.getStageCount(), cauer.getStageCount());
        assertEquals(foster.getAmbientTemperature(), cauer.getAmbientTemperature());

        double omega = SWEEP_MIN_OMEGA;
        for (int point = 0; point < SWEEP_POINT_COUNT; point++) {
            final double[] fosterImpedance = fosterImpedance(foster, omega);
            final double[] cauerImpedance = cauerImpedance(cauer, omega);
            final double difference = cAbs(cSub(fosterImpedance, cauerImpedance));
            assertTrue(difference <= IMPEDANCE_RELATIVE_TOLERANCE * cAbs(fosterImpedance),
                    "Driving-point impedance mismatch at omega=" + omega + ": Foster |Z|="
                            + cAbs(fosterImpedance) + ", Cauer |Z|=" + cAbs(cauerImpedance));
            omega *= Math.pow(SWEEP_MAX_OMEGA / SWEEP_MIN_OMEGA, 1.0 / (SWEEP_POINT_COUNT - 1));
        }
    }

    @Test
    void fosterToCauer_yieldsPositivePhysicalElements() {
        final ThermalRCModel cauer = ThermalRCModel.foster(STAGE_RESISTANCES,
                STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE).toCauer();

        for (int stage = 0; stage < cauer.getStageCount(); stage++) {
            assertTrue(cauer.getResistance(stage) > 0.0,
                    "Cauer resistance " + stage + " must be positive");
            assertTrue(cauer.getCapacitance(stage) > 0.0,
                    "Cauer capacitance " + stage + " must be positive");
        }
    }

    @Test
    void fosterToCauer_returnsSameInstanceForCauerModel() {
        final ThermalRCModel model = ThermalRCModel.cauer(STAGE_RESISTANCES,
                STAGE_CAPACITANCES, AMBIENT_TEMPERATURE);

        assertSame(model, model.toCauer());
    }

    @Test
    void fosterToCauer_rejectsDegenerateRepeatedPoleFit() {
        final ThermalRCModel degenerate = ThermalRCModel.foster(new double[]{1.0, 1.0},
                new double[]{1.0, 1.0}, AMBIENT_TEMPERATURE);

        assertThrows(IllegalStateException.class, degenerate::toCauer);
    }

    @Test
    void junctionStepResponse_matchesFosterFormula() {
        final ThermalRCModel model = ThermalRCModel.foster(STAGE_RESISTANCES,
                STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE);
        final double power = 10.0;

        assertEquals(AMBIENT_TEMPERATURE, model.junctionStepResponse(0.0, power), 1e-12);
        for (final double time : new double[]{0.001, 0.01, 0.1, 1.0, 5.0}) {
            double expectedRise = 0.0;
            for (int stage = 0; stage < model.getStageCount(); stage++) {
                expectedRise += STAGE_RESISTANCES[stage]
                        * (1.0 - Math.exp(-time / STAGE_TIME_CONSTANTS[stage]));
            }
            assertEquals(AMBIENT_TEMPERATURE + power * expectedRise,
                    model.junctionStepResponse(time, power), 1e-12);
        }
    }

    @Test
    void fosterFactory_rejectsNullArrays() {
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.foster(null, STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.foster(STAGE_RESISTANCES, null, AMBIENT_TEMPERATURE));
    }

    @Test
    void fosterFactory_rejectsEmptyArrays() {
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.foster(new double[0], new double[0], AMBIENT_TEMPERATURE));
    }

    @Test
    void fosterFactory_rejectsLengthMismatch() {
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.foster(STAGE_RESISTANCES,
                        new double[]{0.001, 0.01, 0.1}, AMBIENT_TEMPERATURE));
    }

    @Test
    void fosterFactory_rejectsNonPositiveEntries() {
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.foster(new double[]{0.05, -0.15, 0.5, 1.3},
                        STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.foster(STAGE_RESISTANCES,
                        new double[]{0.001, 0.0, 0.1, 1.0}, AMBIENT_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.foster(STAGE_RESISTANCES,
                        new double[]{0.001, Double.NaN, 0.1, 1.0}, AMBIENT_TEMPERATURE));
    }

    @Test
    void cauerFactory_rejectsNonFiniteAmbientTemperature() {
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.cauer(STAGE_RESISTANCES, STAGE_CAPACITANCES, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ThermalRCModel.cauer(STAGE_RESISTANCES, STAGE_CAPACITANCES,
                        Double.POSITIVE_INFINITY));
    }

    @Test
    void junctionStepResponse_rejectsCauerModelAndInvalidInputs() {
        final ThermalRCModel cauer = ThermalRCModel.cauer(STAGE_RESISTANCES,
                STAGE_CAPACITANCES, AMBIENT_TEMPERATURE);
        assertThrows(IllegalStateException.class, () -> cauer.junctionStepResponse(1.0, 10.0));

        final ThermalRCModel foster = ThermalRCModel.foster(STAGE_RESISTANCES,
                STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE);
        assertThrows(IllegalArgumentException.class,
                () -> foster.junctionStepResponse(-1.0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> foster.junctionStepResponse(Double.NaN, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> foster.junctionStepResponse(1.0, Double.NaN));
    }

    @Test
    void accessors_returnDefensiveCopies() {
        final ThermalRCModel model = ThermalRCModel.foster(STAGE_RESISTANCES,
                STAGE_TIME_CONSTANTS, AMBIENT_TEMPERATURE);

        model.getResistances()[0] = 0.0;
        model.getCapacitances()[0] = 0.0;
        model.getTimeConstants()[0] = 0.0;

        assertEquals(STAGE_RESISTANCES[0], model.getResistance(0));
        assertEquals(STAGE_TIME_CONSTANTS[0] / STAGE_RESISTANCES[0], model.getCapacitance(0));
        assertEquals(STAGE_TIME_CONSTANTS[0], model.getTimeConstant(0));
        assertFalse(model.getResistances() == model.getResistances());
        assertTrue(model.getStageCount() > 0);
    }

    // ===== complex arithmetic helpers for the impedance sweep =====

    /** Evaluates the Foster driving-point impedance Z(j*omega). */
    private static double[] fosterImpedance(final ThermalRCModel model, final double omega) {
        double[] impedance = {0.0, 0.0};
        for (int stage = 0; stage < model.getStageCount(); stage++) {
            final double[] onePlusJOmegaTau = cAdd(new double[]{1.0, 0.0},
                    cMul(new double[]{0.0, omega}, new double[]{model.getTimeConstant(stage), 0.0}));
            impedance = cAdd(impedance,
                    cMul(new double[]{model.getResistance(stage), 0.0},
                            cDiv(new double[]{1.0, 0.0}, onePlusJOmegaTau)));
        }
        return impedance;
    }

    /** Evaluates the Cauer ladder driving-point impedance Z(j*omega). */
    private static double[] cauerImpedance(final ThermalRCModel model, final double omega) {
        double[] towardAmbient = {0.0, 0.0};
        for (int stage = model.getStageCount(); stage >= 1; stage--) {
            final double[] seriesResistance = cAdd(new double[]{model.getResistance(stage - 1), 0.0},
                    towardAmbient);
            final double[] shuntAdmittance = cAdd(
                    cMul(new double[]{0.0, omega}, new double[]{model.getCapacitance(stage - 1), 0.0}),
                    cDiv(new double[]{1.0, 0.0}, seriesResistance));
            towardAmbient = cDiv(new double[]{1.0, 0.0}, shuntAdmittance);
        }
        return towardAmbient;
    }

    /** Complex sum a + b, represented as {re, im}. */
    private static double[] cAdd(final double[] a, final double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1]};
    }

    /** Complex difference a - b, represented as {re, im}. */
    private static double[] cSub(final double[] a, final double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1]};
    }

    /** Complex product a * b, represented as {re, im}. */
    private static double[] cMul(final double[] a, final double[] b) {
        return new double[]{a[0] * b[0] - a[1] * b[1], a[0] * b[1] + a[1] * b[0]};
    }

    /** Complex quotient a / b, represented as {re, im}. */
    private static double[] cDiv(final double[] a, final double[] b) {
        final double denominator = b[0] * b[0] + b[1] * b[1];
        return new double[]{(a[0] * b[0] + a[1] * b[1]) / denominator,
                (a[1] * b[0] - a[0] * b[1]) / denominator};
    }

    /** Complex magnitude |a|. */
    private static double cAbs(final double[] a) {
        return Math.hypot(a[0], a[1]);
    }
}
