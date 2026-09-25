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

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.parameters.SourceParameters;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.MnaSolver;
import gecko.core.simulation.solver.MatrixSolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated MNA solver for thermal RC networks, computed as a co-simulation
 * domain next to the electrical circuit.
 *
 * <p>The thermal network is assembled from the classic GeckoCIRCUITS thermal
 * element types and solved by the same {@link MnaSolver} machinery as the
 * electrical domain, with the heat-flow analogy 1:1 between the domains:
 * temperature [°C] plays the role of voltage, heat flow [W] the role of
 * current, thermal resistance [K/W] the role of resistance and heat
 * capacitance [J/K] the role of capacitance. The numerical integration
 * method (backward Euler, trapezoidal, Gear-Shichman) is shared with the
 * electrical domain via {@link SolverType}.
 *
 * <p>Network layout for a model with m stages:
 * <ul>
 *   <li>MNA node 0: ground reference (return terminal of the temperature and
 *       heat flow sources, pinned by the singularity handling)</li>
 *   <li>MNA node 1: {@link ThermalNode#AMBIENT ambient node}, held at the
 *       ambient temperature by a TH_TEMP temperature source; its z-row
 *       current is the total heat flow dissipated into the environment</li>
 *   <li>MNA nodes 2..m+1: the ladder nodes of the {@link ThermalRCModel},
 *       node 2 being the junction</li>
 *   <li>TH_RTH (thermal resistance), TH_CTH (heat capacitance), TH_FLOW
 *       (heat flow source) and TH_TEMP (temperature boundary) elements
 *       stamp the network</li>
 * </ul>
 *
 * <p>Per time step the solver runs the same sequence as the electrical
 * engine: matrix assembly, right-hand-side assembly, LU solve, component
 * current update (fills the capacitor history for the trapezoidal method)
 * and history shift.
 *
 * <p>Heat sources are registered per device name (e.g. the loss device name
 * from the {@code SemiconductorLossEngine}) and mapped to a thermal node;
 * their dissipated power is updated each step before stepping.
 *
 * @see ThermalRCModel
 * @see ThermalNode
 * @since v2.18.0 Task L1 - Multi-Domain Coupling (Step 1: Thermal Domain Engine)
 */
public final class ThermalNetworkSolver {

    /** MNA node reserved as return terminal of the temperature sources. */
    private static final int MNA_GROUND_NODE = 0;

    /** Offset between thermal node indices and MNA node indices. */
    private static final int THERMAL_TO_MNA_NODE_OFFSET = 1;

    /** Source type of the DC heat flow and temperature sources. */
    private static final double DC_SOURCE_TYPE = SourceType.QUELLE_DC_NEW;

    private final MnaSolver matrixSolver;
    private final ComponentCurrentCalculator currentCalculator = new ComponentCurrentCalculator();
    private final CircuitNetlist netlist;
    private final ThermalNode junctionNode;
    private final double ambientTemperature;
    private final int thermalNodeCount;
    private final Map<String, Integer> heatSourceElementByDevice = new HashMap<>();
    private final Map<Integer, Double> initialTemperatureOverrides = new HashMap<>();
    private int ambientSourceZRow;
    private boolean matricesInitialized;

    /**
     * Creates a thermal network solver for the given RC model.
     *
     * <p>All network nodes start at the ambient temperature; individual node
     * temperatures can be overridden via {@link #setInitialTemperature(ThermalNode, double)}
     * before the first {@link #step(double, double)} call. The MNA matrices are
     * initialized lazily on the first step (or re-initialized when heat sources
     * are registered), so the solver always matches the final netlist size.
     *
     * @param model thermal RC model (Foster or Cauer)
     * @param solverType numerical integration method shared with the electrical domain
     *
     * @throws IllegalArgumentException if model or solverType is null
     */
    public ThermalNetworkSolver(final ThermalRCModel model, final SolverType solverType) {
        if (model == null) {
            throw new IllegalArgumentException("Thermal RC model must not be null");
        }
        if (solverType == null) {
            throw new IllegalArgumentException("Solver type must not be null");
        }
        this.ambientTemperature = model.getAmbientTemperature();
        this.thermalNodeCount = model.getStageCount() + 1;
        this.junctionNode = ThermalNode.of(1);
        this.netlist = assembleNetlist(model);
        this.matrixSolver = new MatrixSolver(solverType);
    }

    /**
     * Initializes (or re-initializes after netlist growth) the MNA matrices,
     * seeds all node temperatures to the ambient temperature and re-applies
     * the {@link #setInitialTemperature} overrides.
     */
    private void initializeMatricesIfRequired() {
        if (matricesInitialized) {
            return;
        }
        final int mnaNodeMax = thermalNodeCount;
        final int voltageSourceCount = 1;
        this.ambientSourceZRow = mnaNodeMax + voltageSourceCount;
        matrixSolver.initializeMatrices(mnaNodeMax, voltageSourceCount, netlist.getElementCount());
        seedPotentialsToAmbient(mnaNodeMax);
        for (final Map.Entry<Integer, Double> override : initialTemperatureOverrides.entrySet()) {
            writeInitialTemperature(ThermalNode.of(override.getKey()), override.getValue());
        }
        matricesInitialized = true;
    }

    /**
     * Assembles the thermal MNA netlist of the model: ladder elements, the
     * ambient temperature source and (later, via {@link #addHeatSource}) the
     * device heat flow sources.
     *
     * @param model thermal RC model to assemble
     * @return populated thermal netlist
     */
    private CircuitNetlist assembleNetlist(final ThermalRCModel model) {
        final int stageCount = model.getStageCount();
        final int mnaNodeMax = stageCount + 1;
        final List<CircuitTypCore> types = new ArrayList<>();
        final List<Integer> nodeX = new ArrayList<>();
        final List<Integer> nodeY = new ArrayList<>();
        final List<Integer> voltageSourceNumbers = new ArrayList<>();
        final List<double[]> params = new ArrayList<>();

        // Ladder thermal resistances: stage i connects ladder node i to
        // node i+1, the last stage connects to the ambient node; the X
        // terminal is the hot side, so the stamped heat flow is positive
        // towards the ambient
        for (int stage = 1; stage <= stageCount; stage++) {
            final int towardAmbientNode = stage < stageCount ? stage + 1 : ThermalNode.AMBIENT_VALUE;
            addElement(types, nodeX, nodeY, voltageSourceNumbers, params,
                    CircuitTypCore.TH_RTH, mnaNode(stage), mnaNode(towardAmbientNode), -1,
                    new double[]{model.getResistance(stage - 1)});
        }

        // Heat capacitances: CAUER ladders have one shunt capacitance per
        // ladder node to the ambient; FOSTER chains have one capacitance per
        // series cell, spanning the same nodes as the cell resistance
        for (int stage = 1; stage <= stageCount; stage++) {
            final int capacitanceReturnNode;
            if (model.getKind() == ThermalRCModel.NetworkKind.CAUER) {
                capacitanceReturnNode = ThermalNode.AMBIENT_VALUE;
            } else {
                capacitanceReturnNode = stage < stageCount ? stage + 1 : ThermalNode.AMBIENT_VALUE;
            }
            addElement(types, nodeX, nodeY, voltageSourceNumbers, params,
                    CircuitTypCore.TH_CTH, mnaNode(stage), mnaNode(capacitanceReturnNode), -1,
                    new double[]{model.getCapacitance(stage - 1)});
        }

        // Ambient temperature boundary: classic TH_TEMP source between the
        // ambient node and the MNA ground (the classic TH_AMBIENT block is the
        // passive thermal zero-reference and stamps nothing, so the boundary
        // is realized with the active temperature source type)
        addElement(types, nodeX, nodeY, voltageSourceNumbers, params,
                CircuitTypCore.TH_TEMP, mnaNode(ThermalNode.AMBIENT_VALUE), MNA_GROUND_NODE, 1,
                new double[]{DC_SOURCE_TYPE, ambientTemperature});

        final CircuitNetlist assembled = new CircuitNetlist();
        assembled.initNetlist(
                types.toArray(new CircuitTypCore[0]),
                nodeX.stream().mapToInt(Integer::intValue).toArray(),
                nodeY.stream().mapToInt(Integer::intValue).toArray(),
                voltageSourceNumbers.stream().mapToInt(Integer::intValue).toArray(),
                params.toArray(new double[0][]),
                mnaNodeMax, 1, types.size());
        assembled.setSingularityEntries(new int[]{MNA_GROUND_NODE});
        return assembled;
    }

    /**
     * Maps a thermal node index onto its MNA node index.
     *
     * @param thermalNode thermal node index (0 = ambient)
     * @return MNA node index (the MNA ground 0 is reserved and not mapped)
     */
    private static int mnaNode(final int thermalNode) {
        return thermalNode + THERMAL_TO_MNA_NODE_OFFSET;
    }

    /**
     * Appends one two-terminal element to the growing netlist arrays.
     *
     * @param types component types accumulator
     * @param nodeX X terminal nodes accumulator (MNA node indices)
     * @param nodeY Y terminal nodes accumulator (MNA node indices)
     * @param voltageSourceNumbers voltage source numbers accumulator
     * @param params parameter arrays accumulator
     * @param type component type to append
     * @param mnaNodeX first terminal (MNA node index)
     * @param mnaNodeY second terminal (MNA node index)
     * @param voltageSourceNumber voltage source number (-1 for non-sources)
     * @param elementParams element parameter array
     */
    private static void addElement(final List<CircuitTypCore> types,
                                   final List<Integer> nodeX,
                                   final List<Integer> nodeY,
                                   final List<Integer> voltageSourceNumbers,
                                   final List<double[]> params,
                                   final CircuitTypCore type, final int mnaNodeX,
                                   final int mnaNodeY,
                                   final int voltageSourceNumber, final double[] elementParams) {
        types.add(type);
        nodeX.add(mnaNodeX);
        nodeY.add(mnaNodeY);
        voltageSourceNumbers.add(voltageSourceNumber);
        params.add(elementParams);
    }

    /**
     * Seeds all MNA potential vectors (current and history) with the ambient
     * temperature, so the network starts thermally unexcited and the first
     * step of every integration method sees a consistent history.
     *
     * @param mnaNodeMax highest MNA node index of the network
     */
    private void seedPotentialsToAmbient(final int mnaNodeMax) {
        final int vectorLength = ambientSourceZRow + 1;
        final double[][] vectors = {matrixSolver.getP(), matrixSolver.getPALT(),
                matrixSolver.getPALTALT(), matrixSolver.getPALTALTALT()};
        for (final double[] vector : vectors) {
            for (int i = 0; i < vectorLength; i++) {
                vector[i] = i <= mnaNodeMax ? ambientTemperature : 0.0;
            }
        }
    }

    /**
     * Registers a named heat source dissipating power into the given thermal
     * node (e.g. a loss device of the electrical domain into the junction).
     * The source starts at zero power; update it each step via
     * {@link #setDeviceHeatFlow(String, double)}.
     *
     * <p>Registering a source grows the netlist and re-initializes the MNA
     * matrices, so the thermal state restarts from the initial temperatures -
     * register all heat sources before stepping.
     *
     * @param deviceName unique name of the heat source (e.g. loss device name)
     * @param node thermal node receiving the dissipated power
     *
     * @throws IllegalArgumentException if the device name is null, blank,
     *         already registered, or the node is null
     */
    public void addHeatSource(final String deviceName, final ThermalNode node) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new IllegalArgumentException("Heat source device name must not be blank");
        }
        if (heatSourceElementByDevice.containsKey(deviceName)) {
            throw new IllegalArgumentException("Heat source '" + deviceName
                    + "' is already registered");
        }
        if (node == null) {
            throw new IllegalArgumentException("Heat source node must not be null");
        }
        if (node.value() >= thermalNodeCount) {
            throw new IllegalArgumentException("Unknown thermal node " + node
                    + " (network has " + thermalNodeCount + " nodes)");
        }
        // TH_FLOW stamps as a current source: positive power leaves the X
        // terminal and enters the Y terminal. The X return terminal is the
        // MNA ground (whose KCL row is discarded by the reference pinning),
        // so the injected loss re-appears as heat flow into the ambient
        // temperature source instead of circulating inside the network
        final int elementIndex = appendHeatFlowElement(node);
        heatSourceElementByDevice.put(deviceName, elementIndex);
        // Netlist growth invalidated the matrix allocation
        matricesInitialized = false;
    }

    /**
     * Appends one TH_FLOW heat flow source element to the netlist.
     *
     * @param node thermal node receiving the heat flow
     * @return index of the appended element
     */
    private int appendHeatFlowElement(final ThermalNode node) {
        final CircuitTypCore[] types =
                Arrays.copyOf(netlistTypes(), netlist.getElementCount() + 1);
        final int[] nodeX = Arrays.copyOf(netlistNodeX(), netlist.getElementCount() + 1);
        final int[] nodeY = Arrays.copyOf(netlistNodeY(), netlist.getElementCount() + 1);
        final int[] voltageSourceNumbers =
                Arrays.copyOf(netlistVoltageSourceNumbers(), netlist.getElementCount() + 1);
        final double[][] params = Arrays.copyOf(netlistParams(), netlist.getElementCount() + 1);

        final int elementIndex = netlist.getElementCount();
        types[elementIndex] = CircuitTypCore.TH_FLOW;
        nodeX[elementIndex] = MNA_GROUND_NODE;
        nodeY[elementIndex] = mnaNode(node.value());
        voltageSourceNumbers[elementIndex] = -1;
        params[elementIndex] = new double[]{DC_SOURCE_TYPE, 0.0};

        // Grow the netlist in place: initNetlist re-validates and re-normalizes
        // the extended arrays (this also clears the coupling registries, which
        // thermal networks never use)
        netlist.initNetlist(types, nodeX, nodeY, voltageSourceNumbers, params,
                thermalNodeCount, 1, types.length);
        netlist.setSingularityEntries(new int[]{MNA_GROUND_NODE});
        return elementIndex;
    }

    /** Snapshot of the netlist component types array. */
    private CircuitTypCore[] netlistTypes() {
        final CircuitTypCore[] types = new CircuitTypCore[netlist.getElementCount()];
        for (int i = 0; i < types.length; i++) {
            types[i] = netlist.getType(i);
        }
        return types;
    }

    /** Snapshot of the netlist X terminal node array. */
    private int[] netlistNodeX() {
        final int[] nodes = new int[netlist.getElementCount()];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = netlist.getNodeX(i);
        }
        return nodes;
    }

    /** Snapshot of the netlist Y terminal node array. */
    private int[] netlistNodeY() {
        final int[] nodes = new int[netlist.getElementCount()];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = netlist.getNodeY(i);
        }
        return nodes;
    }

    /** Snapshot of the netlist voltage source number array. */
    private int[] netlistVoltageSourceNumbers() {
        final int[] numbers = new int[netlist.getElementCount()];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = netlist.getVoltageSourceNumber(i);
        }
        return numbers;
    }

    /** Snapshot of the netlist parameter arrays. */
    private double[][] netlistParams() {
        final double[][] params = new double[netlist.getElementCount()][];
        for (int i = 0; i < params.length; i++) {
            params[i] = netlist.getParameter(i).clone();
        }
        return params;
    }

    /**
     * Sets the dissipated power of a registered heat source for the upcoming
     * time step.
     *
     * @param deviceName name the heat source was registered under
     * @param powerWatt dissipated power in watts (negative values model cooling)
     *
     * @throws IllegalArgumentException if the device name is unknown or the
     *         power is not finite
     */
    public void setDeviceHeatFlow(final String deviceName, final double powerWatt) {
        final Integer elementIndex = heatSourceElementByDevice.get(deviceName);
        if (elementIndex == null) {
            throw new IllegalArgumentException("Unknown heat source device: " + deviceName);
        }
        if (!Double.isFinite(powerWatt)) {
            throw new IllegalArgumentException("Heat flow must be finite, got: " + powerWatt);
        }
        netlist.getParameter(elementIndex)[SourceParameters.INDEX_VALUE_DC] = powerWatt;
    }

    /**
     * Overrides the initial temperature of a thermal node. Before the first
     * step the override is stored and applied when the matrices initialize;
     * after stepping, the potential vectors carry the time-step history and
     * a new override can only take effect after the next re-initialization
     * (i.e. when a heat source is registered).
     *
     * @param node thermal node to override
     * @param temperatureC initial temperature in degrees Celsius
     *
     * @throws IllegalArgumentException if the node is null, unknown, or the
     *         temperature is not finite
     */
    public void setInitialTemperature(final ThermalNode node, final double temperatureC) {
        if (node == null) {
            throw new IllegalArgumentException("Thermal node must not be null");
        }
        if (node.value() >= thermalNodeCount) {
            throw new IllegalArgumentException("Unknown thermal node " + node
                    + " (network has " + thermalNodeCount + " nodes)");
        }
        if (!Double.isFinite(temperatureC)) {
            throw new IllegalArgumentException("Initial temperature must be finite, got: "
                    + temperatureC);
        }
        initialTemperatureOverrides.put(node.value(), temperatureC);
        if (matricesInitialized) {
            writeInitialTemperature(node, temperatureC);
        }
    }

    /**
     * Writes a temperature into all four potential vectors (current and
     * history) of the node's MNA row, so every integration method sees a
     * consistent initial state.
     *
     * @param node thermal node to write
     * @param temperatureC initial temperature in degrees Celsius
     */
    private void writeInitialTemperature(final ThermalNode node, final double temperatureC) {
        final int mnaNode = node.value() + THERMAL_TO_MNA_NODE_OFFSET;
        final double[][] vectors = {matrixSolver.getP(), matrixSolver.getPALT(),
                matrixSolver.getPALTALT(), matrixSolver.getPALTALTALT()};
        for (final double[] vector : vectors) {
            vector[mnaNode] = temperatureC;
        }
    }

    /**
     * Performs one thermal time step: assembles and solves the thermal MNA
     * system, updates the element heat flows and shifts the history vectors.
     * The step width may differ per step (multi-rate coupling) as long as it
     * stays constant between two calls of the electrical engine's history
     * integration - the thermal companion models are rebuilt from the passed
     * {@code dt} each step.
     *
     * @param dt time step width in seconds, positive
     * @param time current simulation time in seconds
     *
     * @throws IllegalArgumentException if dt is not finite or not positive
     */
    public void step(final double dt, final double time) {
        if (!Double.isFinite(dt) || dt <= 0.0) {
            throw new IllegalArgumentException("Time step must be finite and positive, got: " + dt);
        }
        initializeMatricesIfRequired();
        matrixSolver.buildMatrixA(netlist, dt, time, false);
        matrixSolver.buildVectorB(netlist, dt, time, false);
        matrixSolver.solve();
        // Fills the heat capacitance companion history (trapezoidal method)
        // and the thermal resistance heat flows; a pure thermal netlist has
        // no semiconductors, so no state flip re-solve can be requested
        currentCalculator.calculateComponentCurrents(matrixSolver, netlist, 1.0, dt, time, false);
        matrixSolver.updateNodePotentials(dt, time);
    }

    /**
     * Gets the current temperature of a thermal node.
     *
     * @param node thermal node to read
     * @return node temperature in degrees Celsius
     *
     * @throws IllegalArgumentException if the node is null or does not belong
     *         to this network
     */
    public double getTemperature(final ThermalNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Thermal node must not be null");
        }
        if (node.value() >= thermalNodeCount) {
            throw new IllegalArgumentException("Unknown thermal node " + node
                    + " (network has " + thermalNodeCount + " nodes)");
        }
        initializeMatricesIfRequired();
        return matrixSolver.getP()[node.value() + THERMAL_TO_MNA_NODE_OFFSET];
    }

    /**
     * Gets the current junction temperature (ladder node 1).
     *
     * @return junction temperature in degrees Celsius
     */
    public double getJunctionTemperature() {
        return getTemperature(junctionNode);
    }

    /**
     * Gets the junction node of the network.
     *
     * @return junction thermal node
     */
    public ThermalNode getJunctionNode() {
        return junctionNode;
    }

    /**
     * Gets the current total heat flow dissipated into the ambient environment
     * (the z-row current of the ambient temperature source).
     *
     * @return heat flow into ambient in watts
     */
    public double getHeatFlowIntoAmbient() {
        initializeMatricesIfRequired();
        return matrixSolver.getP()[ambientSourceZRow];
    }

    /**
     * Gets the number of thermal nodes including the ambient node.
     *
     * @return thermal node count
     */
    public int getThermalNodeCount() {
        return thermalNodeCount;
    }

    /**
     * Gets the ambient temperature of the network.
     *
     * @return ambient temperature in degrees Celsius
     */
    public double getAmbientTemperature() {
        return ambientTemperature;
    }

    /**
     * Gets the numerical integration method of this solver.
     *
     * @return solver type
     */
    public SolverType getSolverType() {
        return matrixSolver.getSolverType();
    }

    /**
     * Gets the underlying thermal netlist, e.g. for result logging of the
     * thermal channels (node temperatures, element heat flows).
     *
     * @return the thermal circuit netlist
     */
    public CircuitNetlist getNetlist() {
        return netlist;
    }
}
