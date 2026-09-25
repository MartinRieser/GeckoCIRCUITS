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
package gecko.core.circuit.losscalculation;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Configuration and stateful loss calculator for an individual semiconductor device.
 *
 * <p>Supports piecewise-linear ({@code V_th + R_on * I}), instantaneous product
 * ({@code v(t) * i(t)}), and detailed 3D lookup table models for conduction losses,
 * along with datasheet energy-scaled ({@code E_on, E_off}) and table-interpolated
 * models for switching losses with moving window thermal averaging.</p>
 *
 * @author GeckoCIRCUITS Team
 * @since v2.18.0 Task L2
 */
public final class SemiconductorDeviceLossModel {

    /** Conduction loss calculation formulation. */
    public enum ConductionModelType {
        /** Instantaneous product p(t) = v(t) * i(t) during forward conduction. */
        INSTANTANEOUS_PRODUCT,
        /** Piecewise-linear model P = V_th * |I| + R_on(T) * I^2. */
        PIECEWISE_LINEAR,
        /** Detailed 2D/3D lookup table V_on(I, T) * |I|. */
        LOOKUP_TABLE
    }

    /** Switching loss calculation formulation. */
    public enum SwitchingModelType {
        /** No switching losses modeled. */
        NONE,
        /** Scaled energy model based on reference E_on, E_off, V_ref, I_ref. */
        ENERGY_SCALED,
        /** 3D lookup table from device datasheet measurements. */
        LOOKUP_TABLE
    }

    /** Default junction temperature used when no thermal domain temperature is available [°C]. */
    static final double DEFAULT_TEMPERATURE = 25.0;

    /** Conduction loss gating threshold below which a device is treated as non-conducting [A]. */
    private static final double CURRENT_NOISE_FLOOR = 1e-4; // 0.1 mA

    /** Tolerance for sliding-window cutoff time comparisons [s]. */
    private static final double WINDOW_CUTOFF_EPSILON = 1e-12;

    private final int elementIndex;
    private final String name;
    private final CircuitTypCore componentType;

    private ConductionModelType conductionModelType = ConductionModelType.PIECEWISE_LINEAR;
    private SwitchingModelType switchingModelType = SwitchingModelType.ENERGY_SCALED;

    private ConductionLossCalculator conductionCalculator;
    private SwitchingLossCalculator switchingCalculator;
    private DetailedLossLookupTable conductionTable;
    private DetailedLossLookupTable turnOnTable;
    private DetailedLossLookupTable turnOffTable;

    // Thermal averaging window configuration [s]
    private double averagingWindowDuration = 0.0; // 0 = instantaneous

    // Rolling window history for power averaging
    private final Deque<TimeLossSample> windowSamples = new ArrayDeque<>();
    private double windowEnergySum = 0.0;

    // Previous step state for switching transition detection
    private double previousCurrent = 0.0;
    private double previousVoltage = 0.0;
    private boolean previousConducting = false;

    // Latest computed losses [W] and cumulative energy [J]
    private double currentConductionLoss = 0.0;
    private double currentSwitchingLoss = 0.0;
    private double currentTotalLoss = 0.0;
    private double cumulativeEnergy = 0.0;

    private record TimeLossSample(double time, double duration, double totalLoss) {}

    /**
     * Creates a loss model for a circuit semiconductor device.
     *
     * @param elementIndex element index in the circuit netlist
     * @param name human-readable component name / label
     * @param componentType semiconductor circuit type
     */
    public SemiconductorDeviceLossModel(
        final int elementIndex,
        final String name,
        final CircuitTypCore componentType
    ) {
        if (elementIndex < 0) {
            throw new IllegalArgumentException("Element index cannot be negative: " + elementIndex);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be null or blank");
        }
        if (componentType == null) {
            throw new IllegalArgumentException("Component type must not be null");
        }
        this.elementIndex = elementIndex;
        this.name = name;
        this.componentType = componentType;
    }

    /**
     * Configures a piecewise-linear conduction model.
     *
     * @param thresholdVoltage threshold forward voltage drop [V]
     * @param onResistance on-state resistance [Ohms]
     * @param tempCoeff temperature coefficient per °C
     * @return this model for fluent configuration
     */
    public SemiconductorDeviceLossModel configurePiecewiseLinearConduction(
        final double thresholdVoltage,
        final double onResistance,
        final double tempCoeff
    ) {
        this.conductionCalculator = new ConductionLossCalculator(thresholdVoltage, onResistance, tempCoeff);
        this.conductionModelType = ConductionModelType.PIECEWISE_LINEAR;
        return this;
    }

    /**
     * Configures instantaneous product conduction modeling (p(t) = v(t) * i(t)).
     *
     * @return this model for fluent configuration
     */
    public SemiconductorDeviceLossModel configureInstantaneousProductConduction() {
        this.conductionModelType = ConductionModelType.INSTANTANEOUS_PRODUCT;
        return this;
    }

    /**
     * Configures lookup-table-based conduction loss modeling.
     *
     * @param table detailed lookup table for V_on(I, T)
     * @return this model for fluent configuration
     */
    public SemiconductorDeviceLossModel configureLookupTableConduction(final DetailedLossLookupTable table) {
        if (table == null) {
            throw new IllegalArgumentException("Lookup table must not be null");
        }
        this.conductionTable = table;
        this.conductionModelType = ConductionModelType.LOOKUP_TABLE;
        return this;
    }

    /**
     * Configures scaled energy switching loss modeling.
     *
     * @param eOnRef turn-on reference energy [J]
     * @param eOffRef turn-off reference energy [J]
     * @param iRef reference current [A]
     * @param vRef reference voltage [V]
     * @param tempCoeff temperature coefficient per °C
     * @return this model for fluent configuration
     */
    public SemiconductorDeviceLossModel configureScaledEnergySwitching(
        final double eOnRef,
        final double eOffRef,
        final double iRef,
        final double vRef,
        final double tempCoeff
    ) {
        this.switchingCalculator = new SwitchingLossCalculator(eOnRef, eOffRef, iRef, vRef, tempCoeff);
        this.switchingModelType = SwitchingModelType.ENERGY_SCALED;
        return this;
    }

    /**
     * Configures lookup-table-based switching loss modeling. The tables are expected to
     * hold switching energies normalized to the measurement blocking voltage (energy per
     * volt), which is the convention of {@link DetailedLossLookupTable#fabric} for
     * {@link SwitchingLossCurve}s; the actual blocking voltage is multiplied in at
     * evaluation time.
     *
     * @param turnOnTable table for E_on(I, T) per volt of blocking voltage
     * @param turnOffTable table for E_off(I, T) per volt of blocking voltage
     * @return this model for fluent configuration
     */
    public SemiconductorDeviceLossModel configureLookupTableSwitching(
        final DetailedLossLookupTable turnOnTable,
        final DetailedLossLookupTable turnOffTable
    ) {
        if (turnOnTable == null || turnOffTable == null) {
            throw new IllegalArgumentException("Turn-on and turn-off lookup tables must not be null");
        }
        this.turnOnTable = turnOnTable;
        this.turnOffTable = turnOffTable;
        this.switchingModelType = SwitchingModelType.LOOKUP_TABLE;
        return this;
    }

    /**
     * Sets the thermal averaging window duration.
     *
     * @param windowDurationSeconds duration in seconds (<= 0 for instantaneous)
     * @return this model for fluent configuration
     */
    public SemiconductorDeviceLossModel setAveragingWindowDuration(final double windowDurationSeconds) {
        this.averagingWindowDuration = Math.max(0.0, windowDurationSeconds);
        return this;
    }

    /**
     * Evaluates conduction and switching losses for the current time step.
     *
     * @param current branch current through the device [A]
     * @param voltage branch voltage across the device [V]
     * @param conducting true if the device is currently in forward conduction
     * @param temperature junction temperature [°C]
     * @param dt time step size [s]
     * @param time current simulation time [s]
     */
    public void calculateStep(
        final double current,
        final double voltage,
        final boolean conducting,
        final double temperature,
        final double dt,
        final double time
    ) {
        if (dt <= 0.0 || !Double.isFinite(dt)) {
            throw new IllegalArgumentException("Time step dt must be positive and finite, got: " + dt);
        }

        final double temp = Double.isFinite(temperature) ? temperature : DEFAULT_TEMPERATURE;

        // 1. Evaluate instantaneous conduction loss
        currentConductionLoss = calculateConductionLoss(current, voltage, conducting, temp);

        // 2. Evaluate switching transitions and energy
        currentSwitchingLoss = calculateSwitchingLoss(current, voltage, conducting, temp, dt);

        // 3. Compute instantaneous total loss for this step
        final double stepInstantaneousLoss = currentConductionLoss + currentSwitchingLoss;
        cumulativeEnergy += stepInstantaneousLoss * dt;

        // 4. Update rolling averaging window if configured
        if (averagingWindowDuration > 0.0) {
            updateAveragingWindow(stepInstantaneousLoss, dt, time);
            currentTotalLoss = windowEnergySum / averagingWindowDuration;
        } else {
            currentTotalLoss = stepInstantaneousLoss;
        }

        // 5. Update history state for next step
        previousCurrent = current;
        previousVoltage = voltage;
        previousConducting = conducting;
    }

    private double calculateConductionLoss(
        final double current,
        final double voltage,
        final boolean conducting,
        final double temperature
    ) {
        if (!conducting || Math.abs(current) < CURRENT_NOISE_FLOOR) {
            return 0.0;
        }

        return switch (conductionModelType) {
            case INSTANTANEOUS_PRODUCT -> instantaneousProduct(voltage, current);
            case PIECEWISE_LINEAR -> {
                if (conductionCalculator != null) {
                    yield conductionCalculator.calculateLoss(current, temperature);
                }
                yield instantaneousProduct(voltage, current);
            }
            case LOOKUP_TABLE -> {
                if (conductionTable != null) {
                    final double vOn = conductionTable.getInterpolatedYValue(temperature, current);
                    yield Math.abs(vOn * current);
                }
                yield instantaneousProduct(voltage, current);
            }
        };
    }

    /**
     * Fallback conduction loss when no dedicated model is configured: forward power
     * p(t) = v(t) * i(t), clamped to non-negative values.
     *
     * @param voltage device voltage [V]
     * @param current device current [A]
     * @return non-negative instantaneous power [W]
     */
    private static double instantaneousProduct(final double voltage, final double current) {
        return Math.max(0.0, voltage * current);
    }

    private double calculateSwitchingLoss(
        final double current,
        final double voltage,
        final boolean conducting,
        final double temperature,
        final double dt
    ) {
        if (switchingModelType == SwitchingModelType.NONE) {
            return 0.0;
        }

        double eventEnergy = 0.0;

        // Turn-on transition: was OFF, now ON
        if (!previousConducting && conducting) {
            final double onCurrent = Math.abs(current);
            final double blockVoltage = Math.abs(previousVoltage);
            eventEnergy += computeTurnOnEnergy(onCurrent, blockVoltage, temperature);
        }

        // Turn-off transition: was ON, now OFF
        if (previousConducting && !conducting) {
            final double offCurrent = Math.abs(previousCurrent);
            final double blockVoltage = Math.abs(voltage);
            eventEnergy += computeTurnOffEnergy(offCurrent, blockVoltage, temperature);
        }

        return eventEnergy / dt;
    }

    private double computeTurnOnEnergy(final double current, final double voltage, final double temp) {
        return switch (switchingModelType) {
            case ENERGY_SCALED -> switchingCalculator != null
                ? switchingCalculator.calculateTurnOnEnergy(current, voltage, temp)
                : 0.0;
            case LOOKUP_TABLE -> turnOnTable != null
                ? turnOnTable.getInterpolatedYValue(temp, current) * voltage
                : 0.0;
            case NONE -> 0.0;
        };
    }

    private double computeTurnOffEnergy(final double current, final double voltage, final double temp) {
        return switch (switchingModelType) {
            case ENERGY_SCALED -> switchingCalculator != null
                ? switchingCalculator.calculateTurnOffEnergy(current, voltage, temp)
                : 0.0;
            case LOOKUP_TABLE -> turnOffTable != null
                ? turnOffTable.getInterpolatedYValue(temp, current) * voltage
                : 0.0;
            case NONE -> 0.0;
        };
    }

    private void updateAveragingWindow(final double loss, final double dt, final double time) {
        final TimeLossSample sample = new TimeLossSample(time, dt, loss);
        windowSamples.addLast(sample);
        windowEnergySum += loss * dt;

        final double cutoffTime = time - averagingWindowDuration;
        while (!windowSamples.isEmpty() && windowSamples.peekFirst().time() <= cutoffTime + WINDOW_CUTOFF_EPSILON) {
            final TimeLossSample oldest = windowSamples.removeFirst();
            windowEnergySum -= oldest.totalLoss() * oldest.duration();
        }

        if (windowEnergySum < 0.0) {
            windowEnergySum = 0.0;
        }
    }

    /**
     * Gets the element index in the circuit netlist.
     *
     * @return netlist element index
     */
    public int getElementIndex() {
        return elementIndex;
    }

    /**
     * Gets the device name.
     *
     * @return component label / identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the semiconductor circuit component type.
     *
     * @return component type enum
     */
    public CircuitTypCore getComponentType() {
        return componentType;
    }

    /**
     * Gets the latest calculated conduction loss in watts [W].
     *
     * @return conduction power loss
     */
    public double getConductionLoss() {
        return currentConductionLoss;
    }

    /**
     * Gets the latest calculated switching loss in watts [W].
     *
     * @return switching power loss
     */
    public double getSwitchingLoss() {
        return currentSwitchingLoss;
    }

    /**
     * Gets the latest total power loss in watts [W].
     *
     * @return total power loss (windowed if averaging window configured, else instantaneous)
     */
    public double getTotalLoss() {
        return currentTotalLoss;
    }

    /**
     * Gets the cumulative energy dissipated by this device in joules [J].
     *
     * @return total dissipated energy
     */
    public double getCumulativeEnergy() {
        return cumulativeEnergy;
    }

    /**
     * Packages current losses into an immutable {@link LossContainer}.
     *
     * @return loss container with conduction and switching losses
     */
    public LossContainer toLossContainer() {
        return new LossContainer(currentConductionLoss, currentSwitchingLoss);
    }
}
