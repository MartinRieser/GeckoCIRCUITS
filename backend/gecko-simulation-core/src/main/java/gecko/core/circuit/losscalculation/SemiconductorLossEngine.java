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
import gecko.core.circuit.parameters.SwitchParameters;
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
     * Registers a custom loss model for a device.
     *
     * @param model configured device loss model
     */
    public void registerDeviceModel(final SemiconductorDeviceLossModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Device loss model must not be null");
        }
        modelsByIndex.put(model.getElementIndex(), model);
        modelsByName.put(model.getName(), model);
        if (!deviceList.contains(model)) {
            deviceList.add(model);
        }
        powerLossArray = new double[deviceList.size()];
    }

    /**
     * Evaluates conduction and switching losses across all registered devices for one time step.
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
        if (netlist == null || deviceList.isEmpty()) {
            return;
        }

        final double[] thermTemps = domainCoupler != null ? domainCoupler.getThermTemperatures() : new double[0];

        for (int i = 0; i < deviceList.size(); i++) {
            final SemiconductorDeviceLossModel model = deviceList.get(i);
            final int elemIdx = model.getElementIndex();
            final double[] parameters = netlist.getParameters(elemIdx);

            final double current = extractCurrent(model.getComponentType(), parameters);
            final double voltage = extractVoltage(model.getComponentType(), parameters);
            final boolean conducting = isConducting(model.getComponentType(), parameters, voltage, current);
            final double temperature = i < thermTemps.length ? thermTemps[i] : 25.0;

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
        if (signalName.equals("P_loss_total") || signalName.equals("P_cond_total") || signalName.equals("P_sw_total")
                || signalName.equals("E_loss_total")) {
            return true;
        }
        if (signalName.startsWith("P_loss_") || signalName.startsWith("P_cond_") || signalName.startsWith("P_sw_")) {
            final String deviceName = signalName.substring(signalName.lastIndexOf('_') + 1);
            return modelsByName.containsKey(deviceName);
        }
        if (signalName.startsWith("P_loss[") && signalName.endsWith("]")) {
            try {
                final int idx = Integer.parseInt(signalName.substring(7, signalName.length() - 1));
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
        if (signalName.equals("P_loss_total")) {
            return getTotalLosses();
        }
        if (signalName.equals("P_cond_total")) {
            return getTotalConductionLosses();
        }
        if (signalName.equals("P_sw_total")) {
            return getTotalSwitchingLosses();
        }
        if (signalName.equals("E_loss_total")) {
            return getCumulativeEnergy();
        }

        if (signalName.startsWith("P_loss_")) {
            final String name = signalName.substring("P_loss_".length());
            final SemiconductorDeviceLossModel model = modelsByName.get(name);
            return model != null ? model.getTotalLoss() : 0.0;
        }
        if (signalName.startsWith("P_cond_")) {
            final String name = signalName.substring("P_cond_".length());
            final SemiconductorDeviceLossModel model = modelsByName.get(name);
            return model != null ? model.getConductionLoss() : 0.0;
        }
        if (signalName.startsWith("P_sw_")) {
            final String name = signalName.substring("P_sw_".length());
            final SemiconductorDeviceLossModel model = modelsByName.get(name);
            return model != null ? model.getSwitchingLoss() : 0.0;
        }

        if (signalName.startsWith("P_loss[") && signalName.endsWith("]")) {
            try {
                final int idx = Integer.parseInt(signalName.substring(7, signalName.length() - 1));
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
        final double[] parameters = netlist.getParameters(index);

        double threshold = 0.0;
        double onResistance = 10e-3;

        if (type == CircuitTypCore.LK_D || type == CircuitTypCore.LK_THYR || type == CircuitTypCore.LK_IGBT) {
            if (parameters.length > DiodeParameters.INDEX_FORWARD_VOLTAGE) {
                threshold = parameters[DiodeParameters.INDEX_FORWARD_VOLTAGE];
            }
            if (parameters.length > DiodeParameters.INDEX_R_ON && parameters[DiodeParameters.INDEX_R_ON] > 0.0) {
                onResistance = parameters[DiodeParameters.INDEX_R_ON];
            }
        } else if (type == CircuitTypCore.LK_MOSFET || type == CircuitTypCore.LK_S) {
            if (parameters.length > SwitchParameters.INDEX_R_ON && parameters[SwitchParameters.INDEX_R_ON] > 0.0) {
                onResistance = parameters[SwitchParameters.INDEX_R_ON];
            }
        }

        model.configurePiecewiseLinearConduction(threshold, onResistance, 0.0);

        // Default switching loss model: 1 mJ turn-on, 1 mJ turn-off @ 600V, 10A
        model.configureScaledEnergySwitching(1e-3, 1e-3, 10.0, 600.0, 0.0);

        return model;
    }

    private static double extractCurrent(final CircuitTypCore type, final double[] parameters) {
        if (parameters == null || parameters.length <= DiodeParameters.INDEX_CURRENT) {
            return 0.0;
        }
        return parameters[DiodeParameters.INDEX_CURRENT];
    }

    private static double extractVoltage(final CircuitTypCore type, final double[] parameters) {
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
