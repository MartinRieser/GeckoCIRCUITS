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
 * Configuration of one electro-thermal device coupling: a named electrical
 * component dissipates its losses into the junction of a thermal RC model,
 * and the solved junction temperature feeds back into the component's
 * electrical parameters.
 *
 * <p>The dissipation source depends on the component type: semiconductor
 * devices contribute their {@code SemiconductorLossEngine} loss (conduction
 * + switching), resistors contribute their ohmic dissipation {@code I^2 R}.
 * The feedback law is selected by the factory method:
 * <ul>
 *   <li>{@link #forResistor(String, ThermalRCModel, double)}:
 *       {@code R(T) = R_0 * (1 + alpha * (T - T_0))}</li>
 *   <li>{@link #forDiode(String, ThermalRCModel, double)}:
 *       {@code V_f(T) = V_f0 - k_T * (T - T_0)}</li>
 * </ul>
 *
 * <p>Each coupling owns an independent thermal network solved synchronously
 * with the electrical time step.
 *
 * @see TemperatureDependentResistor
 * @see TemperatureDependentDiode
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 2: Electro-Thermal Feedback)
 */
public final class ThermalCoupling {

    /** Feedback law of the coupled electrical component. */
    public enum FeedbackKind {

        /** Resistor: R(T) = R_0 * (1 + alpha * (T - T_0)). */
        RESISTOR,

        /** Diode: V_f(T) = V_f0 - k_T * (T - T_0). */
        DIODE
    }

    private final String deviceName;
    private final ThermalRCModel model;
    private final double temperatureCoefficient;
    private final FeedbackKind kind;

    private ThermalCoupling(final String deviceName, final ThermalRCModel model,
                            final double temperatureCoefficient, final FeedbackKind kind) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new IllegalArgumentException("Coupled device name must not be blank");
        }
        if (model == null) {
            throw new IllegalArgumentException("Thermal RC model must not be null");
        }
        if (!Double.isFinite(temperatureCoefficient)) {
            throw new IllegalArgumentException("Temperature coefficient must be finite, got: "
                    + temperatureCoefficient);
        }
        this.deviceName = deviceName;
        this.model = model;
        this.temperatureCoefficient = temperatureCoefficient;
        this.kind = kind;
    }

    /**
     * Creates a resistor coupling with linear resistance temperature
     * dependence (e.g. {@code 3.9e-3} 1/K for copper).
     *
     * @param deviceName name of the electrical resistor component
     * @param model thermal RC model of the resistor's heat path
     * @param alphaPerKelvin resistance temperature coefficient in 1/K
     * @return resistor coupling
     *
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static ThermalCoupling forResistor(final String deviceName, final ThermalRCModel model,
                                              final double alphaPerKelvin) {
        return new ThermalCoupling(deviceName, model, alphaPerKelvin, FeedbackKind.RESISTOR);
    }

    /**
     * Creates a diode coupling with linear forward-voltage temperature
     * dependence (typical silicon: {@code 1e-3}..{@code 2e-3} V/K).
     *
     * @param deviceName name of the electrical diode component
     * @param model thermal RC model of the diode's heat path
     * @param forwardVoltageSlopePerKelvin forward-voltage decrease per kelvin in V/K
     * @return diode coupling
     *
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static ThermalCoupling forDiode(final String deviceName, final ThermalRCModel model,
                                           final double forwardVoltageSlopePerKelvin) {
        return new ThermalCoupling(deviceName, model, forwardVoltageSlopePerKelvin, FeedbackKind.DIODE);
    }

    /**
     * Gets the name of the coupled electrical component.
     *
     * @return electrical component name
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Gets the thermal RC model of the device's heat path.
     *
     * @return thermal RC model
     */
    public ThermalRCModel getModel() {
        return model;
    }

    /**
     * Gets the feedback law selected by the factory method.
     *
     * @return feedback kind
     */
    public FeedbackKind getFeedbackKind() {
        return kind;
    }

    /**
     * Gets the temperature coefficient. Interpretation depends on the
     * component type: resistance coefficient in 1/K for resistors,
     * forward-voltage slope in V/K for diodes.
     *
     * @return temperature coefficient (1/K or V/K)
     */
    public double getTemperatureCoefficient() {
        return temperatureCoefficient;
    }
}
