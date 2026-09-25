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
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.parameters.DiodeParameters;
import gecko.core.simulation.DomainCoupler;
import gecko.core.simulation.solver.SolverConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * High-performance orchestrator for semiconductor conduction and switching loss calculations.
 *
 * <p>Discovers semiconductor switches ({@code LK_D}, {@code LK_S}, {@code LK_THYR},
 * {@code LK_IGBT}, {@code LK_MOSFET}, {@code LK_BJT}) in a {@link CircuitNetlist},
 * evaluates instantaneous and time-averaged power dissipation at each simulation time step,
 * and transfers heat flow to the thermal domain via {@link DomainCoupler}.</p>
 *
 * @author GeckoCIRCUITS Team
 * @since v2.18.0 Task L2
 */
public final class SemiconductorLossEngine {

    /** Aggregate loss signal: total power loss of all registered devices [W]. */
    public static final String SIGNAL_TOTAL_LOSS = "P_loss_total";

    /** Aggregate loss signal: total conduction power loss of all registered devices [W]. */
    public static final String SIGNAL_TOTAL_CONDUCTION = "P_cond_total";

    /** Aggregate loss signal: total switching power loss of all registered devices [W]. */
    public static final String SIGNAL_TOTAL_SWITCHING = "P_sw_total";

    /** Aggregate loss signal: cumulative energy of all registered devices [J]. */
    public static final String SIGNAL_TOTAL_ENERGY = "E_loss_total";

    /** Per-device loss signal prefix followed by the device name (total power). */
    public static final String PREFIX_DEVICE_LOSS = "P_loss_";

    /** Per-device loss signal prefix followed by the device name (conduction power). */
    public static final String PREFIX_DEVICE_CONDUCTION = "P_cond_";

    /** Per-device loss signal prefix followed by the device name (switching power). */
    public static final String PREFIX_DEVICE_SWITCHING = "P_sw_";

    /** Indexed loss signal prefix followed by the device list index in brackets. */
    public static final String PREFIX_INDEXED_LOSS = "P_loss[";

    /** Default turn-on reference energy for auto-created models [J]. */
    private static final double DEFAULT_TURN_ON_ENERGY = 1e-3;

    /** Default turn-off reference energy for auto-created models [J]. */
    private static final double DEFAULT_TURN_OFF_ENERGY = 1e-3;

    /** Default reference current for auto-created switching energy models [A]. */
    private static final double DEFAULT_SWITCHING_REFERENCE_CURRENT = 10.0;

    /** Default reference voltage for auto-created switching energy models [V]. */
    private static final double DEFAULT_SWITCHING_REFERENCE_VOLTAGE = 600.0;

    private final Map<Integer, SemiconductorDeviceLossModel> modelsByIndex = new LinkedHashMap<>();
    private final Map<String, SemiconductorDeviceLossModel> modelsByName = new LinkedHashMap<>();
    private final List<SemiconductorDeviceLossModel> deviceList = new ArrayList<>();

    // Cached component loss array passed to DomainCoupler [W]
    private double[] powerLossArray = new double[0];

    /**
     * Creates an empty semiconductor loss engine.
     */
    public SemiconductorLossEngine() {
    }

    /**
     * Initializes the loss engine from a circuit netlist, automatically detecting
     * all semiconductor switches and creating default loss models for each.
     *
     * @param netlist circuit netlist containing topology and parameters
     * @return this engine for method chaining
     */
    public SemiconductorLossEngine initializeFromNetlist(final CircuitNetlist netlist) {
        modelsByIndex.clear();
        modelsByName.clear();
        deviceList.clear();

        if (netlist == null || netlist.getElementCount() == 0) {
            powerLossArray = new double[0];
            return this;
        }

        final int count = netlist.getElementCount();
        for (int i = 0; i < count; i++) {
            final CircuitTypCore type = netlist.getType(i);
            if (type != null && type.isSemiconductor()) {
                final String name = determineDeviceName(netlist, i, type);
                final SemiconductorDeviceLossModel model = createDefaultModel(i, name, type, netlist);
                registerDeviceModel(model);
            }
        }

        powerLossArray = new double[deviceList.size()];
        return this;
    }

    /**
     * Registers a custom loss model for a device. Re-registering a model with an
     * element index or name that is already registered replaces the previous entry,
     * so lookups, step evaluation, and aggregate totals always refer to the same model.
     *
     * @param model configured device loss model
     */
    public void registerDeviceModel(final SemiconductorDeviceLossModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Device loss model must not be null");
        }
        deviceList.removeIf(existing -> existing.getElementIndex() == model.getElementIndex()
                || existing.getName().equals(model.getName()));
        modelsByIndex.put(model.getElementIndex(), model);
        modelsByName.put(model.getName(), model);
        deviceList.add(model);
        powerLossArray = new double[deviceList.size()];
    }

    /**
     * Evaluates conduction and switching losses across all registered devices for one time step.
     *
     * <p>Junction temperatures are resolved per device through the explicit
     * device→thermal-node mapping configured via
     * {@link DomainCoupler#configureThermToLkDeviceMapping}; the device ordering is the
     * same one in which power losses are pushed back via
     * {@link DomainCoupler#setLkPowerLosses}. Devices without a mapped thermal node fall
     * back to the default junction temperature.</p>
     *
     * @param netlist circuit netlist containing the latest solver voltages and currents
     * @param domainCoupler domain coupler for reading temperatures and pushing power losses (can be null)
     * @param dt time step size in seconds [s]
     * @param time current simulation time in seconds [s]
     */
    public void calculateStep(
        final CircuitNetlist netlist,
        final DomainCoupler domainCoupler,
        final double dt,
        final double time
    ) {
        calculateStep(netlist, domainCoupler, dt, time,
                SemiconductorDeviceLossModel.DEFAULT_TEMPERATURE);
    }

    /**
     * Calculates conduction and switching losses of all devices for one time
     * step with a configurable fallback junction temperature for devices
     * without a thermal coupling.
     *
     * @param netlist circuit netlist after the solve
     * @param domainCoupler domain coupler for reading temperatures and pushing power losses (can be null)
     * @param dt time step in seconds
     * @param time current simulation time in seconds
     * @param defaultTemperature fallback junction temperature in degrees
     *        Celsius for devices without a thermal coupling
     */
    public void calculateStep(
        final CircuitNetlist netlist,
        final DomainCoupler domainCoupler,
        final double dt,
        final double time,
        final double defaultTemperature
    ) {
        if (netlist == null || deviceList.isEmpty()) {
            return;
        }

        for (int i = 0; i < deviceList.size(); i++) {
            final SemiconductorDeviceLossModel model = deviceList.get(i);
            final int elemIdx = model.getElementIndex();
            final double[] parameters = netlist.getParameters(elemIdx);

            final double current = extractCurrent(parameters);
            final double voltage = extractVoltage(parameters);
            final boolean conducting = isConducting(model.getComponentType(), parameters, voltage, current);
            final double temperature = domainCoupler != null
                    ? domainCoupler.getDeviceTemperature(i, defaultTemperature)
                    : defaultTemperature;

            model.calculateStep(current, voltage, conducting, temperature, dt, time);
            powerLossArray[i] = model.getTotalLoss();
        }

        // Pipe losses directly into DomainCoupler for thermal domain coupling
        if (domainCoupler != null) {
            domainCoupler.setLkPowerLosses(powerLossArray);
        }
    }

    /**
     * Checks whether a named signal represents a recognized semiconductor loss channel.
     *
     * @param signalName signal name to check (e.g., "P_loss_total", "P_loss[0]", "P_cond_D1")
     * @return true if the engine can supply this signal
     */
    public boolean hasLossSignal(final String signalName) {
        if (signalName == null || signalName.isBlank()) {
            return false;
        }
        if (signalName.equals(SIGNAL_TOTAL_LOSS) || signalName.equals(SIGNAL_TOTAL_CONDUCTION)
                || signalName.equals(SIGNAL_TOTAL_SWITCHING) || signalName.equals(SIGNAL_TOTAL_ENERGY)) {
            return true;
        }
        if (signalName.startsWith(PREFIX_DEVICE_LOSS)) {
            return modelsByName.containsKey(signalName.substring(PREFIX_DEVICE_LOSS.length()));
        }
        if (signalName.startsWith(PREFIX_DEVICE_CONDUCTION)) {
            return modelsByName.containsKey(signalName.substring(PREFIX_DEVICE_CONDUCTION.length()));
        }
        if (signalName.startsWith(PREFIX_DEVICE_SWITCHING)) {
            return modelsByName.containsKey(signalName.substring(PREFIX_DEVICE_SWITCHING.length()));
        }
        if (signalName.startsWith(PREFIX_INDEXED_LOSS) && signalName.endsWith("]")) {
            try {
                final int idx = Integer.parseInt(signalName.substring(PREFIX_INDEXED_LOSS.length(),
                        signalName.length() - 1));
                return idx >= 0 && idx < deviceList.size();
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Evaluates the current value of a recognized loss signal channel.
     *
     * @param signalName signal name to evaluate
     * @return current power loss [W] or energy [J], or 0.0 if not found
     */
    public double evaluateLossSignal(final String signalName) {
        if (signalName == null) {
            return 0.0;
        }
        if (signalName.equals(SIGNAL_TOTAL_LOSS)) {
            return getTotalLosses();
        }
        if (signalName.equals(SIGNAL_TOTAL_CONDUCTION)) {
            return getTotalConductionLosses();
        }
        if (signalName.equals(SIGNAL_TOTAL_SWITCHING)) {
            return getTotalSwitchingLosses();
        }
        if (signalName.equals(SIGNAL_TOTAL_ENERGY)) {
            return getCumulativeEnergy();
        }

        if (signalName.startsWith(PREFIX_DEVICE_LOSS)) {
            final SemiconductorDeviceLossModel model =
                    modelsByName.get(signalName.substring(PREFIX_DEVICE_LOSS.length()));
            return model != null ? model.getTotalLoss() : 0.0;
        }
        if (signalName.startsWith(PREFIX_DEVICE_CONDUCTION)) {
            final SemiconductorDeviceLossModel model =
                    modelsByName.get(signalName.substring(PREFIX_DEVICE_CONDUCTION.length()));
            return model != null ? model.getConductionLoss() : 0.0;
        }
        if (signalName.startsWith(PREFIX_DEVICE_SWITCHING)) {
            final SemiconductorDeviceLossModel model =
                    modelsByName.get(signalName.substring(PREFIX_DEVICE_SWITCHING.length()));
            return model != null ? model.getSwitchingLoss() : 0.0;
        }

        if (signalName.startsWith(PREFIX_INDEXED_LOSS) && signalName.endsWith("]")) {
            try {
                final int idx = Integer.parseInt(signalName.substring(PREFIX_INDEXED_LOSS.length(),
                        signalName.length() - 1));
                if (idx >= 0 && idx < deviceList.size()) {
                    return deviceList.get(idx).getTotalLoss();
                }
            } catch (NumberFormatException ignored) {
                // Return fallback 0.0
            }
        }

        return 0.0;
    }

    private static String determineDeviceName(
        final CircuitNetlist netlist,
        final int index,
        final CircuitTypCore type
    ) {
        if (netlist.getLabelResolver() != null) {
            final String label = netlist.getLabelResolver().getLabel(index);
            if (label != null && !label.isBlank()) {
                return label;
            }
        }
        return type.name() + "_" + index;
    }

    private static SemiconductorDeviceLossModel createDefaultModel(
        final int index,
        final String name,
        final CircuitTypCore type,
        final CircuitNetlist netlist
    ) {
        final SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(index, name, type);

        // Default conduction loss is the instantaneous product of the solved
        // branch voltage and current, so the loss always reflects the current
        // netlist state (including electro-thermal parameter feedback).
        // Datasheet PWL or lookup-table models should be registered per device
        // via registerDeviceModel for calibrated results.
        model.configureInstantaneousProductConduction();

        // Neutral default switching energies: real datasheet values should be
        // configured per device via registerDeviceModel for accurate results
        model.configureScaledEnergySwitching(DEFAULT_TURN_ON_ENERGY, DEFAULT_TURN_OFF_ENERGY,
                DEFAULT_SWITCHING_REFERENCE_CURRENT, DEFAULT_SWITCHING_REFERENCE_VOLTAGE, 0.0);

        return model;
    }

    private static double extractCurrent(final double[] parameters) {
        if (parameters == null || parameters.length <= DiodeParameters.INDEX_CURRENT) {
            return 0.0;
        }
        return parameters[DiodeParameters.INDEX_CURRENT];
    }

    private static double extractVoltage(final double[] parameters) {
        if (parameters == null || parameters.length <= DiodeParameters.INDEX_VOLTAGE) {
            return 0.0;
        }
        return parameters[DiodeParameters.INDEX_VOLTAGE];
    }

    private static boolean isConducting(
        final CircuitTypCore type,
        final double[] parameters,
        final double voltage,
        final double current
    ) {
        if (parameters == null || parameters.length == 0) {
            return false;
        }
        final double rD = parameters[0];
        // Low dynamic resistance indicates conducting state in MNA piecewise-linear model
        if (rD < SolverConstants.RD_OFF_THRESHOLD) {
            return true;
        }
        // Fallback: forward diode current with positive voltage
        return type == CircuitTypCore.LK_D && current > 0.0 && voltage > 0.0;
    }

    // --- Accessors ---

    /**
     * Gets the loss model for an element index.
     *
     * @param elementIndex netlist element index
     * @return model, or null if element not a registered semiconductor
     */
    public SemiconductorDeviceLossModel getModel(final int elementIndex) {
        return modelsByIndex.get(elementIndex);
    }

    /**
     * Gets the loss model for a device name.
     *
     * @param name device label or identifier
     * @return model, or null if name not registered
     */
    public SemiconductorDeviceLossModel getModel(final String name) {
        return modelsByName.get(name);
    }

    /**
     * Gets an unmodifiable list of all registered device loss models.
     *
     * @return list of models
     */
    public List<SemiconductorDeviceLossModel> getDeviceList() {
        return Collections.unmodifiableList(deviceList);
    }

    /**
     * Gets the instantaneous total conduction loss in watts [W].
     *
     * @return total conduction power loss
     */
    public double getTotalConductionLosses() {
        double sum = 0.0;
        for (SemiconductorDeviceLossModel model : deviceList) {
            sum += model.getConductionLoss();
        }
        return sum;
    }

    /**
     * Gets the instantaneous total switching loss in watts [W].
     *
     * @return total switching power loss
     */
    public double getTotalSwitchingLosses() {
        double sum = 0.0;
        for (SemiconductorDeviceLossModel model : deviceList) {
            sum += model.getSwitchingLoss();
        }
        return sum;
    }

    /**
     * Gets the instantaneous total power loss across all semiconductors in watts [W].
     *
     * @return total power loss
     */
    public double getTotalLosses() {
        double sum = 0.0;
        for (SemiconductorDeviceLossModel model : deviceList) {
            sum += model.getTotalLoss();
        }
        return sum;
    }

    /**
     * Gets the cumulative total energy dissipated by all semiconductors in joules [J].
     *
     * @return cumulative energy
     */
    public double getCumulativeEnergy() {
        double sum = 0.0;
        for (SemiconductorDeviceLossModel model : deviceList) {
            sum += model.getCumulativeEnergy();
        }
        return sum;
    }

    /**
     * Gets a copy of all individual device power losses in watts [W].
     *
     * @return array of component power losses
     */
    public double[] getAllPowerLosses() {
        return powerLossArray.clone();
    }

    /**
     * Packages current losses for all components into an immutable map of {@link LossContainer}s.
     *
     * @return map of device name to loss container
     */
    public Map<String, LossContainer> getLossBreakdown() {
        final Map<String, LossContainer> breakdown = new LinkedHashMap<>();
        for (SemiconductorDeviceLossModel model : deviceList) {
            breakdown.put(model.getName(), model.toLossContainer());
        }
        return Collections.unmodifiableMap(breakdown);
    }
}
