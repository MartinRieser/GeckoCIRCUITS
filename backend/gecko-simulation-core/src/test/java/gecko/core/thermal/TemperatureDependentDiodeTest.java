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
 * Unit tests of the temperature-dependent diode model: linear forward-voltage
 * decrease with the junction temperature and the Shockley thermal voltage.
 */
class TemperatureDependentDiodeTest {

    /** Reference forward voltage in volts. */
    private static final double REFERENCE_FORWARD_VOLTAGE = 0.7;

    /** Forward-voltage slope in V/K (typical silicon). */
    private static final double FORWARD_VOLTAGE_SLOPE = 1.5e-3;

    /** Reference temperature in degrees Celsius. */
    private static final double REFERENCE_TEMPERATURE = 25.0;

    /** Thermal voltage at 25 C in volts (k_B * 298.15 K / q). */
    private static final double THERMAL_VOLTAGE_AT_25C = 0.0256926;

    @Test
    void forwardVoltageAt_decreasesLinearlyWithTemperature() {
        final TemperatureDependentDiode model = new TemperatureDependentDiode(
                REFERENCE_FORWARD_VOLTAGE, FORWARD_VOLTAGE_SLOPE, REFERENCE_TEMPERATURE);

        assertEquals(REFERENCE_FORWARD_VOLTAGE, model.forwardVoltageAt(REFERENCE_TEMPERATURE), 1e-12);
        assertEquals(REFERENCE_FORWARD_VOLTAGE - FORWARD_VOLTAGE_SLOPE * 100.0,
                model.forwardVoltageAt(REFERENCE_TEMPERATURE + 100.0), 1e-12);
        assertEquals(REFERENCE_FORWARD_VOLTAGE + FORWARD_VOLTAGE_SLOPE * 25.0,
                model.forwardVoltageAt(REFERENCE_TEMPERATURE - 25.0), 1e-12);
    }

    @Test
    void forwardVoltageAt_heatedDiodeHasLowerForwardVoltage() {
        final TemperatureDependentDiode model = new TemperatureDependentDiode(
                REFERENCE_FORWARD_VOLTAGE, FORWARD_VOLTAGE_SLOPE, REFERENCE_TEMPERATURE);

        assertTrue(model.forwardVoltageAt(REFERENCE_TEMPERATURE + 50.0) < REFERENCE_FORWARD_VOLTAGE,
                "silicon diode forward voltage must drop with rising junction temperature");
    }

    @Test
    void thermalVoltage_matchesBoltzmannLaw() {
        assertEquals(THERMAL_VOLTAGE_AT_25C,
                TemperatureDependentDiode.thermalVoltage(REFERENCE_TEMPERATURE), 1e-6);

        // linear growth with the absolute temperature: V_t(125 C) / V_t(25 C)
        // = (125 + 273.15) / (25 + 273.15)
        final double ratio = TemperatureDependentDiode.thermalVoltage(125.0)
                / TemperatureDependentDiode.thermalVoltage(REFERENCE_TEMPERATURE);
        assertEquals(398.15 / 298.15, ratio, 1e-12);
    }

    @Test
    void constructor_rejectsNonFiniteParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new TemperatureDependentDiode(Double.NaN, FORWARD_VOLTAGE_SLOPE,
                        REFERENCE_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> new TemperatureDependentDiode(REFERENCE_FORWARD_VOLTAGE, Double.NaN,
                        REFERENCE_TEMPERATURE));
        assertThrows(IllegalArgumentException.class,
                () -> new TemperatureDependentDiode(REFERENCE_FORWARD_VOLTAGE, FORWARD_VOLTAGE_SLOPE,
                        Double.POSITIVE_INFINITY));
    }

    @Test
    void accessors_returnConstructorValues() {
        final TemperatureDependentDiode model = new TemperatureDependentDiode(
                REFERENCE_FORWARD_VOLTAGE, FORWARD_VOLTAGE_SLOPE, REFERENCE_TEMPERATURE);

        assertEquals(REFERENCE_FORWARD_VOLTAGE, model.getReferenceForwardVoltage());
        assertEquals(FORWARD_VOLTAGE_SLOPE, model.getForwardVoltageSlope());
        assertEquals(REFERENCE_TEMPERATURE, model.getReferenceTemperature());
    }
}
