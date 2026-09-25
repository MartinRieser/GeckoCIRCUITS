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
 * Linear temperature dependence of an electrical resistance, feeding the
 * solved thermal-domain temperatures back into the electrical netlist:
 *
 * <p>{@code R(T) = R_0 * (1 + alpha * (T - T_0))}
 *
 * <p>with the reference resistance {@code R_0} at the reference temperature
 * {@code T_0} and the temperature coefficient {@code alpha} in 1/K (copper
 * windings: about {@code 3.9e-3} 1/K). Negative coefficients model
 * negative-temperature-coefficient behavior; the model recomputes from the
 * reference values each step (stateless in the temperature), so the
 * resistance follows the thermal solution without drift.
 *
 * @see TemperatureDependentDiode
 * @see ThermalNetworkSolver
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 2: Electro-Thermal Feedback)
 */
public final class TemperatureDependentResistor {

    /** Reference resistance at the reference temperature in ohms. */
    private final double referenceResistance;

    /** Temperature coefficient in 1/K. */
    private final double temperatureCoefficient;

    /** Reference temperature in degrees Celsius. */
    private final double referenceTemperature;

    /**
     * Creates a temperature-dependent resistance model.
     *
     * @param referenceResistance resistance at the reference temperature in ohms, positive
     * @param temperatureCoefficient temperature coefficient in 1/K (negative
     *        values model NTC behavior)
     * @param referenceTemperature reference temperature in degrees Celsius
     *
     * @throws IllegalArgumentException if the reference resistance is not
     *         finite and positive, or the coefficient or reference temperature
     *         are not finite
     */
    public TemperatureDependentResistor(final double referenceResistance,
                                        final double temperatureCoefficient,
                                        final double referenceTemperature) {
        if (!Double.isFinite(referenceResistance) || referenceResistance <= 0.0) {
            throw new IllegalArgumentException("Reference resistance must be finite and positive,"
                    + " got: " + referenceResistance);
        }
        if (!Double.isFinite(temperatureCoefficient)) {
            throw new IllegalArgumentException("Temperature coefficient must be finite, got: "
                    + temperatureCoefficient);
        }
        if (!Double.isFinite(referenceTemperature)) {
            throw new IllegalArgumentException("Reference temperature must be finite, got: "
                    + referenceTemperature);
        }
        this.referenceResistance = referenceResistance;
        this.temperatureCoefficient = temperatureCoefficient;
        this.referenceTemperature = referenceTemperature;
    }

    /**
     * Computes the resistance at the given temperature.
     *
     * @param temperature device temperature in degrees Celsius
     * @return resistance in ohms
     *
     * @throws IllegalStateException if the linear model leaves the physically
     *         valid range (non-positive or non-finite resistance)
     */
    public double resistanceAt(final double temperature) {
        final double resistance = referenceResistance
                * (1.0 + temperatureCoefficient * (temperature - referenceTemperature));
        if (!Double.isFinite(resistance) || resistance <= 0.0) {
            throw new IllegalStateException("Temperature-dependent resistance left the physical"
                    + " range: R(" + temperature + ") = " + resistance
                    + " (R_0=" + referenceResistance + ", alpha=" + temperatureCoefficient + ")");
        }
        return resistance;
    }

    /**
     * Gets the reference resistance.
     *
     * @return resistance at the reference temperature in ohms
     */
    public double getReferenceResistance() {
        return referenceResistance;
    }

    /**
     * Gets the temperature coefficient.
     *
     * @return temperature coefficient in 1/K
     */
    public double getTemperatureCoefficient() {
        return temperatureCoefficient;
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
