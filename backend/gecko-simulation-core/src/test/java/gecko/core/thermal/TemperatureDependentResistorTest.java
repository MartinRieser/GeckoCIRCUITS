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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of the linear temperature-dependent resistance model
 * {@code R(T) = R_0 * (1 + alpha * (T - T_0))}.
 */
class TemperatureDependentResistorTest {

    /** Reference resistance of the test model in ohms. */
    private static final double REFERENCE_RESISTANCE = 10.0;

    /** Copper-like temperature coefficient in 1/K. */
    private static final double ALPHA_COPPER = 3.9e-3;

    /** Reference temperature in degrees Celsius. */
    private static final double REFERENCE_TEMPERATURE = 25.0;

    @Test
    void resistanceAt_followsLinearLaw() {
        final TemperatureDependentResistor model = new TemperatureDependentResistor(
                REFERENCE_RESISTANCE, ALPHA_COPPER, REFERENCE_TEMPERATURE);

        assertEquals(REFERENCE_RESISTANCE, model.resistanceAt(REFERENCE_TEMPERATURE), 1e-12);
        assertEquals(REFERENCE_RESISTANCE * (1.0 + ALPHA_COPPER * 100.0),
                model.resistanceAt(REFERENCE_TEMPERATURE + 100.0), 1e-9);
        assertEquals(REFERENCE_RESISTANCE * (1.0 - ALPHA_COPPER * 50.0),
                model.resistanceAt(REFERENCE_TEMPERATURE - 50.0), 1e-9);
    }

    @Test
    void resistanceAt_negativeCoefficientModelsNtcBehavior() {
        final double negativeAlpha = -2e-3;
        final TemperatureDependentResistor model = new TemperatureDependentResistor(
                REFERENCE_RESISTANCE, negativeAlpha, REFERENCE_TEMPERATURE);

        final double hotResistance = model.resistanceAt(REFERENCE_TEMPERATURE + 100.0);
        assertTrue(hotResistance < REFERENCE_RESISTANCE,
                "negative temperature coefficient must reduce the resistance when heating");
    }

    @Test
    void resistanceAt_rejectsNonPhysicalRange() {
        final TemperatureDependentResistor model = new TemperatureDependentResistor(
                REFERENCE_RESISTANCE, -0.02, REFERENCE_TEMPERATURE);

        // 1 + alpha * (T - T_0) = 1 - 1.2 < 0 at 60 K above the reference
        assertThrows(IllegalStateException.class, () -> model.resistanceAt(REFERENCE_TEMPERATURE + 60.0));
    }

    @Test
    void constructor_rejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new TemperatureDependentResistor(0.0, ALPHA_COPPER, REFERENCE_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> new TemperatureDependentResistor(-1.0, ALPHA_COPPER, REFERENCE_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> new TemperatureDependentResistor(Double.NaN, ALPHA_COPPER, REFERENCE_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> new TemperatureDependentResistor(REFERENCE_RESISTANCE, Double.NaN,
                        REFERENCE_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> new TemperatureDependentResistor(REFERENCE_RESISTANCE, ALPHA_COPPER, Double.NaN));
    }

    @Test
    void accessors_returnConstructorValues() {
        final TemperatureDependentResistor model = new TemperatureDependentResistor(
                REFERENCE_RESISTANCE, ALPHA_COPPER, REFERENCE_TEMPERATURE);

        assertEquals(REFERENCE_RESISTANCE, model.getReferenceResistance());
        assertEquals(ALPHA_COPPER, model.getTemperatureCoefficient());
        assertEquals(REFERENCE_TEMPERATURE, model.getReferenceTemperature());
    }
}
