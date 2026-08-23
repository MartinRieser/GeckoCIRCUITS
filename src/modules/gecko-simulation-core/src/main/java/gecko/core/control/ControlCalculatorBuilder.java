/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.control;

import gecko.core.circuit.ComponentTerminals;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.ConstantCalculator;
import gecko.core.control.calculators.GateCalculator;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import gecko.core.control.calculators.SignalCalculatorRandom;
import gecko.core.control.calculators.SignalCalculatorRectangle;
import gecko.core.control.calculators.SignalCalculatorSinus;
import gecko.core.control.calculators.SignalCalculatorTriangle;
import gecko.core.io.CircuitModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the executable CONTROL domain from a parsed {@link CircuitModel}:
 * instantiates one calculator per control block, wires inputs to producer
 * outputs via the CONTROL wire topology (union-find over shared wire points,
 * mirroring the LK {@code NetlistBuilder}), and resolves the classic
 * {@code coupledReferenceID[]} links that attach gates to switches and
 * voltmeter/ammeter blocks to power components.
 *
 * <p>Headless counterpart of the classic GUI's
 * {@code NetzlisteCONTROL}/{@code RegelBlock} graph; terminal geometry follows
 * {@link ComponentTerminals#controlFlowVector}.</p>
 */
public final class ControlCalculatorBuilder {

    private static final Logger LOGGER = LogManager.getLogger(ControlCalculatorBuilder.class);

    /** Classic signal source types ({@code ControlSourceType} in the GUI). */
    private static final int SOURCE_SINUS = 402;
    private static final int SOURCE_TRIANGLE = 403;
    private static final int SOURCE_RECTANGLE = 404;
    private static final int SOURCE_RANDOM = 405;

    /** Signal source parameter layout ({@code ControlSignalSource}). */
    private static final int SIGNAL_SOURCE_TYPE = 0;
    private static final int SIGNAL_AMPLITUDE = 1;
    private static final int SIGNAL_FREQUENCY = 2;
    private static final int SIGNAL_DC_OFFSET = 3;
    private static final int SIGNAL_PHASE_DEGREES = 4;
    private static final int SIGNAL_DUTY = 5;

    /** Gate signal parameter slot shared with the stampers. */
    private static final int PARAM_GATE = 8;
    /** Parameter slot holding the current switch resistance. */
    private static final int PARAM_RESISTANCE = 0;

    // Classic control block types (ControlTyp in the GUI)
    private static final int TYP_VOLTMEETER = 1;
    private static final int TYP_AMMETER = 2;
    private static final int TYP_CONSTANT = 3;
    private static final int TYP_SIGNAL_SOURCE = 4;
    private static final int TYP_SCOPE = 5;
    private static final int TYP_GATE = 6;

    /**
     * Terminal layout per classic control type: {inputs, outputs, output
     * x-offset}. Inputs sit at rel (-2, -i); outputs at (xPos, -j) — the xPos=1
     * cases follow {@code RegelBlock}'s 1-in/1-out and 2-in/1-out rules.
     */
    private static final Map<Integer, int[]> TERMINALS_BY_TYPE = Map.of(
            TYP_VOLTMEETER, new int[]{0, 1, 2},
            TYP_AMMETER, new int[]{0, 1, 2},
            TYP_CONSTANT, new int[]{0, 1, 2},
            TYP_SIGNAL_SOURCE, new int[]{0, 1, 2},
            TYP_SCOPE, new int[]{3, 0, 2},
            TYP_GATE, new int[]{1, 0, 2});

    /**
     * Wired control domain plus its LK couplings, ready to be driven by the
     * simulation loop: calculators in execution order, gate drives that must
     * re-stamp switch resistances before each matrix build, and measurement
     * probes whose outputs are refreshed from the solved circuit after each
     * step.
     */
    public record ControlCoupling(
            List<AbstractControlCalculatable> calculators,
            List<GateDrive> gateDrives,
            List<Probe> probes) {

        /** Prepares stateful calculators (periodic signal sources) for a run. */
        public void initialize(double dt) {
            for (AbstractControlCalculatable calc : calculators) {
                if (calc instanceof InitializableAtSimulationStart initializable) {
                    initializable.initializeAtSimulationStart(dt);
                }
            }
        }

        /**
         * Writes the current gate signals into the switch elements' parameter
         * arrays (resistance slot + gate slot), so the next buildMatrixA stamps
         * the switched resistance.
         */
        public void applyGateSignals(CircuitNetlist netlist) {
            for (GateDrive drive : gateDrives) {
                drive.applyTo(netlist);
            }
        }

        /** Refreshes voltmeter/ammeter outputs from the solved circuit. */
        public void updateProbes(CircuitNetlist netlist, double[] nodeVoltages) {
            for (Probe probe : probes) {
                probe.update(netlist, nodeVoltages);
            }
        }

        /** Signal names under which probe outputs can be logged. */
        public List<String> probeSignalNames() {
            List<String> names = new ArrayList<>();
            for (Probe probe : probes) {
                names.add(probe.name());
            }
            return names;
        }
    }

    /**
     * A gate block driving one switch element. The gate calculator itself is
     * never executed (classic {@code NotCalculateableMarker}); its input is an
     * alias of the producer's output array and is read live each step.
     */
    public record GateDrive(int elementIndex, CircuitTypCore switchType, GateCalculator gate) {

        /** Parameter slot of the ON resistance per switch type. */
        private int onResistanceSlot() {
            return switchType == CircuitTypCore.LK_S ? 1 : 2;
        }

        /** Parameter slot of the OFF resistance per switch type. */
        private int offResistanceSlot() {
            return switchType == CircuitTypCore.LK_S ? 2 : 3;
        }

        /** Current gate signal value (0 when the gate input is unwired). */
        public double gateSignal() {
            double[][] input = gate._inputSignal;
            if (input.length > 0 && input[0] != null && input[0].length > 0) {
                return input[0][0];
            }
            return 0.0;
        }

        private void applyTo(CircuitNetlist netlist) {
            double[] params = netlist.getParameter(elementIndex);
            double signal = gateSignal();
            params[PARAM_GATE] = signal;
            boolean on = signal > AbstractControlCalculatable.SIGNAL_THRESHOLD;
            int onSlot = onResistanceSlot();
            int offSlot = offResistanceSlot();
            double resistance = on && onSlot < params.length ? params[onSlot]
                    : (!on && offSlot < params.length ? params[offSlot] : params[PARAM_RESISTANCE]);
            params[PARAM_RESISTANCE] = resistance;
        }
    }

    /**
     * A voltmeter or ammeter block attached to a power component via
     * {@code coupledReferenceID}; its output array is written from the solved
     * circuit each step (voltage across the element / current through it).
     */
    public record Probe(int elementIndex, boolean current, String name,
                        AbstractControlCalculatable outputHolder) {

        private void update(CircuitNetlist netlist, double[] nodeVoltages) {
            double value;
            if (current) {
                double[] currents = netlist.getLastComponentCurrentsRef();
                value = elementIndex < currents.length ? currents[elementIndex] : 0.0;
            } else {
                int nodeX = netlist.getNodeX(elementIndex);
                int nodeY = netlist.getNodeY(elementIndex);
                value = (nodeX < nodeVoltages.length ? nodeVoltages[nodeX] : 0.0)
                        - (nodeY < nodeVoltages.length ? nodeVoltages[nodeY] : 0.0);
            }
            outputHolder._outputSignal[0][0] = value;
        }
    }

    private ControlCalculatorBuilder() {
    }

    /**
     * Builds the control coupling for a parsed model. Element indices refer to
     * the {@link CircuitNetlist} built by
     * {@code NetlistBuilder.buildFromCircuitModel} (same component order).
     */
    public static ControlCoupling build(CircuitModel model, CircuitNetlist netlist) {
        List<CircuitModel.ComponentData> controlComponents =
                model != null ? model.getControlComponents() : List.of();
        if (controlComponents.isEmpty() || netlist == null || netlist.getElementCount() == 0) {
            return new ControlCoupling(List.of(), List.of(), List.of());
        }
        Map<Long, Integer> elementIndexByUid = new HashMap<>();
        List<CircuitModel.ComponentData> circuitComponents = model.getCircuitComponents();
        for (int i = 0; i < circuitComponents.size() && i < netlist.getElementCount(); i++) {
            elementIndexByUid.put(circuitComponents.get(i).getUniqueObjectIdentifier(), i);
        }

        // union-find over the CONTROL wire topology
        Map<String, String> parent = new HashMap<>();
        for (CircuitModel.ConnectionData conn : model.getConnections()) {
            if (!"CONTROL".equalsIgnoreCase(conn.getType()) || conn.getPoints() == null) {
                continue;
            }
            String previous = null;
            for (int[] point : conn.getPoints()) {
                String key = pointKey(point[0], point[1]);
                parent.putIfAbsent(key, key);
                if (previous != null) {
                    union(parent, previous, key);
                }
                previous = key;
            }
        }

        Map<String, AbstractControlCalculatable> calculatorByComp = new LinkedHashMap<>();
        List<GateDrive> gateDrives = new ArrayList<>();
        List<Probe> probes = new ArrayList<>();

        for (CircuitModel.ComponentData comp : controlComponents) {
            AbstractControlCalculatable calculator = createCalculator(comp);
            if (calculator == null) {
                continue;
            }
            calculatorByComp.put(keyOf(comp), calculator);
            if (calculator instanceof GateCalculator gate && comp.getCoupledReferenceID() != 0) {
                Integer elementIndex = elementIndexByUid.get(comp.getCoupledReferenceID());
                if (elementIndex != null && isSwitch(netlist.getType(elementIndex))) {
                    gateDrives.add(new GateDrive(elementIndex, netlist.getType(elementIndex), gate));
                } else {
                    LOGGER.warn("Gate '{}' references unknown power component uid {}",
                            comp.getName(), comp.getCoupledReferenceID());
                }
            }
            if ((comp.getType() == TYP_VOLTMEETER || comp.getType() == TYP_AMMETER)
                    && comp.getCoupledReferenceID() != 0) {
                Integer elementIndex = elementIndexByUid.get(comp.getCoupledReferenceID());
                if (elementIndex != null) {
                    probes.add(new Probe(elementIndex, comp.getType() == TYP_AMMETER,
                            displayName(comp), calculator));
                } else {
                    LOGGER.warn("Measurement '{}' references unknown power component uid {}",
                            comp.getName(), comp.getCoupledReferenceID());
                }
            }
        }

        wireInputs(controlComponents, calculatorByComp, parent);

        return new ControlCoupling(topologicalOrder(calculatorByComp), gateDrives, probes);
    }

    private static AbstractControlCalculatable createCalculator(CircuitModel.ComponentData comp) {
        int[] layout = TERMINALS_BY_TYPE.get(comp.getType());
        if (layout == null) {
            LOGGER.warn("Control block '{}' has unsupported typ {} - skipped",
                    comp.getName(), comp.getType());
            return null;
        }
        double[] params = comp.getRawParameters();
        return switch (comp.getType()) {
            case TYP_CONSTANT -> new ConstantCalculator(param(params, 0));
            case TYP_SIGNAL_SOURCE -> createSignalSource(params);
            case TYP_GATE -> new GateCalculator();
            // measurement blocks hold their output; the engine writes it each step
            case TYP_VOLTMEETER, TYP_AMMETER -> new ConstantCalculator(0);
            // scopes only display; nothing to calculate headlessly
            case TYP_SCOPE -> null;
            default -> null;
        };
    }

    private static AbstractControlCalculatable createSignalSource(double[] params) {
        int sourceType = (int) param(params, SIGNAL_SOURCE_TYPE);
        double amplitude = param(params, SIGNAL_AMPLITUDE);
        double frequency = param(params, SIGNAL_FREQUENCY);
        double offset = param(params, SIGNAL_DC_OFFSET);
        double phase = Math.toRadians(param(params, SIGNAL_PHASE_DEGREES));
        double duty = param(params, SIGNAL_DUTY);
        try {
            return switch (sourceType) {
                case SOURCE_SINUS -> new SignalCalculatorSinus(0, amplitude, frequency, phase, offset, duty);
                case SOURCE_TRIANGLE -> new SignalCalculatorTriangle(0, amplitude, frequency, phase, offset, duty);
                case SOURCE_RECTANGLE -> new SignalCalculatorRectangle(0, amplitude, frequency, phase, offset, duty);
                case SOURCE_RANDOM -> new SignalCalculatorRandom();
                // unknown source types degrade to a constant offset signal
                default -> new ConstantCalculator(amplitude + offset);
            };
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid signal source parameters: {}", e.getMessage());
            return new ConstantCalculator(0);
        }
    }

    private static void wireInputs(List<CircuitModel.ComponentData> controlComponents,
                                   Map<String, AbstractControlCalculatable> calculatorByComp,
                                   Map<String, String> parent) {
        // producer map: signal node -> (calculator, output index)
        Map<String, Object[]> producerByNode = new HashMap<>();
        for (CircuitModel.ComponentData comp : controlComponents) {
            int[] layout = TERMINALS_BY_TYPE.get(comp.getType());
            AbstractControlCalculatable calculator = calculatorByComp.get(keyOf(comp));
            if (layout == null || calculator == null || layout[1] == 0) {
                continue;
            }
            for (int j = 0; j < layout[1]; j++) {
                int[] point = terminalPoint(comp, -1, j, layout[2]);
                producerByNode.putIfAbsent(find(parent, pointKey(point[0], point[1])),
                        new Object[]{calculator, j});
            }
        }

        for (CircuitModel.ComponentData comp : controlComponents) {
            int[] layout = TERMINALS_BY_TYPE.get(comp.getType());
            AbstractControlCalculatable calculator = calculatorByComp.get(keyOf(comp));
            if (layout == null || calculator == null || layout[0] == 0) {
                continue;
            }
            for (int i = 0; i < layout[0]; i++) {
                int[] point = terminalPoint(comp, i, -1, layout[2]);
                Object[] producer = producerByNode.get(find(parent, pointKey(point[0], point[1])));
                if (producer != null) {
                    try {
                        calculator.setInputSignal(i, (AbstractControlCalculatable) producer[0],
                                (Integer) producer[1]);
                    } catch (Exception e) {
                        LOGGER.warn("Input {} of '{}' already connected", i, comp.getName());
                    }
                } else {
                    calculator.checkInputWithoutConnectionAndFill(i);
                }
            }
        }
    }

    /**
     * Absolute grid point of a control terminal: input {@code i} at rel
     * {@code (-2, -i)}, output {@code j} at {@code (xPos, -j)}, mapped through
     * the classic {@code TerminalRelativePosition.getPointFromDirection}.
     */
    private static int[] terminalPoint(CircuitModel.ComponentData comp, int inputIndex, int outputIndex, int xPos) {
        int px = inputIndex >= 0 ? -2 : xPos;
        int py = inputIndex >= 0 ? -inputIndex : -outputIndex;
        int x = comp.getPosition().length > 0 ? comp.getPosition()[0] : 0;
        int y = comp.getPosition().length > 1 ? comp.getPosition()[1] : 0;
        int orientation = comp.getOrientation() != 0 ? comp.getOrientation() : ComponentTerminals.NORTH_SOUTH;
        int dx;
        int dy;
        switch (orientation) {
            case ComponentTerminals.EAST_WEST -> {
                dx = py;
                dy = px;
            }
            case ComponentTerminals.SOUTH_NORTH -> {
                dx = -px;
                dy = py;
            }
            case ComponentTerminals.WEST_EAST -> {
                dx = -py;
                dy = -px;
            }
            default -> { // NORTH_SOUTH
                dx = px;
                dy = -py;
            }
        }
        return new int[]{x + dx, y + dy};
    }

    /** Kahn topological order over the calculable (non-marker) calculators. */
    private static List<AbstractControlCalculatable> topologicalOrder(
            Map<String, AbstractControlCalculatable> calculatorByComp) {
        List<AbstractControlCalculatable> calculable = new ArrayList<>();
        for (AbstractControlCalculatable calc : calculatorByComp.values()) {
            if (!(calc instanceof NotCalculateableMarker)) {
                calculable.add(calc);
            }
        }
        // dependency edges: consumer -> producers read from its input aliases
        Map<AbstractControlCalculatable, List<AbstractControlCalculatable>> producers = new HashMap<>();
        Map<AbstractControlCalculatable, Integer> inDegree = new LinkedHashMap<>();
        for (AbstractControlCalculatable calc : calculable) {
            inDegree.put(calc, 0);
        }
        for (AbstractControlCalculatable consumer : calculable) {
            for (double[] input : consumer._inputSignal) {
                if (input == null) {
                    continue;
                }
                for (AbstractControlCalculatable producer : calculable) {
                    if (producer != consumer && producer._outputSignal != null) {
                        for (double[] output : producer._outputSignal) {
                            if (output == input) {
                                producers.computeIfAbsent(producer, k -> new ArrayList<>()).add(consumer);
                                inDegree.merge(consumer, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }
        Deque<AbstractControlCalculatable> ready = new ArrayDeque<>();
        for (Map.Entry<AbstractControlCalculatable, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }
        List<AbstractControlCalculatable> ordered = new ArrayList<>(calculable.size());
        while (!ready.isEmpty()) {
            AbstractControlCalculatable calc = ready.poll();
            ordered.add(calc);
            for (AbstractControlCalculatable consumer : producers.getOrDefault(calc, List.of())) {
                if (inDegree.merge(consumer, -1, Integer::sum) == 0) {
                    ready.add(consumer);
                }
            }
        }
        // cycles (feedback loops) are valid in control circuits: append the rest
        for (AbstractControlCalculatable calc : calculable) {
            if (!ordered.contains(calc)) {
                ordered.add(calc);
            }
        }
        return ordered;
    }

    private static boolean isSwitch(CircuitTypCore type) {
        return type == CircuitTypCore.LK_S || type == CircuitTypCore.LK_IGBT
                || type == CircuitTypCore.LK_MOSFET || type == CircuitTypCore.LK_THYR;
    }

    private static String displayName(CircuitModel.ComponentData comp) {
        return comp.getName() != null && !comp.getName().isBlank()
                ? comp.getName() : "CTRL_" + comp.getUniqueObjectIdentifier();
    }

    private static String keyOf(CircuitModel.ComponentData comp) {
        return comp.getFamily() + "#" + comp.getUniqueObjectIdentifier() + "#" + comp.getName();
    }

    private static String pointKey(int x, int y) {
        return x + "," + y;
    }

    private static double param(double[] params, int index) {
        return params != null && index < params.length ? params[index] : 0.0;
    }

    private static String find(Map<String, String> parent, String key) {
        String root = parent.get(key);
        if (root == null) {
            return key;
        }
        while (!root.equals(key)) {
            key = root;
            root = parent.get(key);
        }
        return root;
    }

    private static void union(Map<String, String> parent, String a, String b) {
        String rootA = find(parent, a);
        String rootB = find(parent, b);
        if (!rootA.equals(rootB)) {
            parent.put(rootA, rootB);
        }
    }
}
