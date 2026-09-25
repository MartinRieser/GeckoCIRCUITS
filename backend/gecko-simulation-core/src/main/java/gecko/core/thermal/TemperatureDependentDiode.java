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

/**
 * Temperature dependence of a semiconductor diode, feeding the solved
 * thermal-domain junction temperature back into the electrical netlist:
 *
 * <p>Forward voltage (negative temperature coefficient - silicon diodes drop
 * about 1..2 mV per kelvin):
 * <pre>{@code V_f(T) = V_f0 - k_T * (T - T_0) }</pre>
 *
 * <p>Thermal voltage of the Shockley equation (grows linearly with the
 * absolute temperature):
 * <pre>{@code V_t(T) = k_B * (T + 273.15) / q }</pre>
 *
 * <p>with the reference forward voltage {@code V_f0} at the reference
 * temperature {@code T_0}, the forward-voltage slope {@code k_T} in V/K,
 * Boltzmann constant {@code k_B} and elementary charge {@code q}. The model
 * recomputes from the reference values each step (stateless in the
 * temperature), so the forward voltage follows the thermal solution without
 * drift.
 *
 * @see TemperatureDependentResistor
 * @see ThermalNetworkSolver
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 2: Electro-Thermal Feedback)
 */
public final class TemperatureDependentDiode {

    /** Boltzmann constant in J/K (SI defined value). */
    public static final double BOLTZMANN_CONSTANT = 1.380649e-23;

    /** Elementary charge in C (SI defined value). */
    public static final double ELEMENTARY_CHARGE = 1.602176634e-19;

    /** Offset between the Celsius and Kelvin scales in K. */
    public static final double CELSIUS_TO_KELVIN_OFFSET = 273.15;

    /** Reference forward voltage at the reference temperature in volts. */
    private final double referenceForwardVoltage;

    /** Forward-voltage temperature slope in V/K (positive = decreasing V_f). */
    private final double forwardVoltageSlope;

    /** Reference temperature in degrees Celsius. */
    private final double referenceTemperature;

    /**
     * Creates a temperature-dependent diode model.
     *
     * @param referenceForwardVoltage forward voltage at the reference temperature in volts
     * @param forwardVoltageSlope forward-voltage decrease per kelvin in V/K
     *        (typical silicon: {@code 1e-3}..{@code 2e-3} V/K)
     * @param referenceTemperature reference temperature in degrees Celsius
     *
     * @throws IllegalArgumentException if any argument is not finite
     */
    public TemperatureDependentDiode(final double referenceForwardVoltage,
                                     final double forwardVoltageSlope,
                                     final double referenceTemperature) {
        if (!Double.isFinite(referenceForwardVoltage)) {
            throw new IllegalArgumentException("Reference forward voltage must be finite, got: "
                    + referenceForwardVoltage);
        }
        if (!Double.isFinite(forwardVoltageSlope)) {
            throw new IllegalArgumentException("Forward voltage slope must be finite, got: "
                    + forwardVoltageSlope);
        }
        if (!Double.isFinite(referenceTemperature)) {
            throw new IllegalArgumentException("Reference temperature must be finite, got: "
                    + referenceTemperature);
        }
        this.referenceForwardVoltage = referenceForwardVoltage;
        this.forwardVoltageSlope = forwardVoltageSlope;
        this.referenceTemperature = referenceTemperature;
    }

    /**
     * Computes the forward voltage at the given junction temperature.
     *
     * @param junctionTemperature junction temperature in degrees Celsius
     * @return forward voltage in volts
     */
    public double forwardVoltageAt(final double junctionTemperature) {
        return referenceForwardVoltage
                - forwardVoltageSlope * (junctionTemperature - referenceTemperature);
    }

    /**
     * Computes the Shockley thermal voltage at the given temperature.
     * {@code V_t = k_B * T_abs / q} with {@code T_abs} in kelvin; at 25 C
     * this is about {@code 25.7 mV}.
     *
     * @param temperature temperature in degrees Celsius
     * @return thermal voltage in volts
     */
    public static double thermalVoltage(final double temperature) {
        return BOLTZMANN_CONSTANT * (temperature + CELSIUS_TO_KELVIN_OFFSET) / ELEMENTARY_CHARGE;
    }

    /**
     * Gets the reference forward voltage.
     *
     * @return forward voltage at the reference temperature in volts
     */
    public double getReferenceForwardVoltage() {
        return referenceForwardVoltage;
    }

    /**
     * Gets the forward-voltage temperature slope.
     *
     * @return forward-voltage slope in V/K
     */
    public double getForwardVoltageSlope() {
        return forwardVoltageSlope;
    }

    /**
     * Gets the reference temperature.
     *
     * @return reference temperature in degrees Celsius
     */
    public double getReferenceTemperature() {
        return referenceTemperature;
    }
}
