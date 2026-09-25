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
package gecko.core.circuit.semiconductors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the smooth Shockley junction model: operating-point calibration,
 * current/conductance values, overflow clamping, and argument validation.
 */
class ShockleyDiodeModelTest {

    private static final double FORWARD_VOLTAGE = 0.7;
    private static final double REFERENCE_CURRENT = 1.0;
    private static final double EMISSION_COEFFICIENT = 1.0;
    private static final double THERMAL_VOLTAGE = 25.85e-3;

    private ShockleyDiodeModel newModel() {
        return ShockleyDiodeModel.fromOperatingPoint(
                FORWARD_VOLTAGE, REFERENCE_CURRENT, EMISSION_COEFFICIENT, THERMAL_VOLTAGE);
    }

    @Test
    void calibration_passesThroughTheOperatingPoint() {
        assertEquals(REFERENCE_CURRENT, newModel().current(FORWARD_VOLTAGE), 1e-9 * REFERENCE_CURRENT,
                "the calibrated curve must carry exactly the rated current at V_f");
    }

    @Test
    void current_matchesShockleyEquation() {
        final ShockleyDiodeModel model = newModel();
        final double saturationCurrent = model.getSaturationCurrent();
        assertEquals(saturationCurrent * (Math.exp(0.3 / THERMAL_VOLTAGE) - 1.0),
                model.current(0.3), 1e-18, "forward current follows the exponential law");
        assertEquals(-saturationCurrent, model.current(-5.0), 1e-24,
                "reverse current saturates at -Is");
    }

    @Test
    void conductance_matchesNumericDerivative() {
        final ShockleyDiodeModel model = newModel();
        final double v = 0.6;
        final double deltaV = 1e-9;
        final double numericDerivative =
                (model.current(v + deltaV) - model.current(v - deltaV)) / (2.0 * deltaV);
        assertEquals(numericDerivative, model.conductance(v), 1e-6 * numericDerivative,
                "conductance must be the derivative of the current");
    }

    @Test
    void strongForwardBias_isClampedWithoutOverflow() {
        final double clampedCurrent = newModel().current(1000.0);
        assertTrue(Double.isFinite(clampedCurrent), "clamping must prevent overflow");
        assertEquals(REFERENCE_CURRENT * Math.exp(ShockleyDiodeModel.MAX_EXPONENT
                        - FORWARD_VOLTAGE / THERMAL_VOLTAGE),
                clampedCurrent, clampedCurrent * 1e-12,
                "the clamp bounds the exponential argument at MAX_EXPONENT");
    }

    @Test
    void constructor_rejectsInvalidOperatingPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> ShockleyDiodeModel.fromOperatingPoint(-1.0, 1.0, 1.0, THERMAL_VOLTAGE));
        assertThrows(IllegalArgumentException.class,
                () -> ShockleyDiodeModel.fromOperatingPoint(FORWARD_VOLTAGE, 0.0, 1.0, THERMAL_VOLTAGE));
        assertThrows(IllegalArgumentException.class,
                () -> ShockleyDiodeModel.fromOperatingPoint(FORWARD_VOLTAGE, 1.0, 0.0, THERMAL_VOLTAGE));
        assertThrows(IllegalArgumentException.class,
                () -> ShockleyDiodeModel.fromOperatingPoint(FORWARD_VOLTAGE, 1.0, 1.0, 0.0));
    }
}
