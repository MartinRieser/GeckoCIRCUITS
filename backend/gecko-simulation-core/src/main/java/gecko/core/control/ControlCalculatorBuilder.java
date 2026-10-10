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
import gecko.core.control.calculators.AbsCalculator;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.AddCalculator;
import gecko.core.control.calculators.AndTwoPortCalculator;
import gecko.core.control.calculators.ConstantCalculator;
import gecko.core.control.calculators.CosCalculator;
import gecko.core.control.calculators.DeadTimeCalculator;
import gecko.core.control.calculators.DelayCalculator;
import gecko.core.control.calculators.DivCalculator;
import gecko.core.control.calculators.ExpCalculator;
import gecko.core.control.calculators.GainCalculator;
import gecko.core.control.calculators.GateCalculator;
import gecko.core.control.calculators.GreaterEqualCalculator;
import gecko.core.control.calculators.GreaterThanCalculator;
import gecko.core.control.calculators.HysteresisCalculatorInternal;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import gecko.core.control.calculators.IntegratorCalculation;
import gecko.core.control.calculators.CLibraryCalculator;
import gecko.core.control.calculators.LimitCalculatorInternal;
import gecko.core.control.calculators.LnCalculator;
import gecko.core.control.calculators.MaxCalculatorTwoInputs;
import gecko.core.control.calculators.MinCalculatorTwoInputs;
import gecko.core.control.calculators.MulCalculator;
import gecko.core.control.calculators.NotCalculator;
import gecko.core.control.calculators.OrCalculatorTwoInputs;
import gecko.core.control.calculators.PDCalculator;
import gecko.core.control.calculators.PICalculator;
import gecko.core.control.calculators.PT1Calculator;
import gecko.core.control.calculators.PT2Calculator;
import gecko.core.control.calculators.SampleHoldCalculator;
import gecko.core.control.calculators.ScriptBlockCalculator;
import gecko.core.control.calculators.SignalCalculatorRandom;
import gecko.core.control.calculators.SignalCalculatorRectangle;
import gecko.core.control.calculators.SignalCalculatorSinus;
import gecko.core.control.calculators.SignalCalculatorTriangle;
import gecko.core.control.calculators.SignalSelectorCalculator;
import gecko.core.control.calculators.SinCalculator;
import gecko.core.control.calculators.SqrtCalculator;
import gecko.core.control.calculators.SubtractionTwoParameter;
import gecko.core.control.calculators.TimeCalculator;
import gecko.core.control.calculators.XORCalculator;
import gecko.core.io.CircuitModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /** Phase as stored in the .ipes file: radians (classic import applies toDegrees for the dialog). */
    private static final int SIGNAL_PHASE_RADIANS = 4;
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
    private static final int TYP_LEGACY_GAIN = 7;
    private static final int TYP_LEGACY_PT1 = 8;
    private static final int TYP_LEGACY_PT2 = 9;
    private static final int TYP_LEGACY_PI = 10;
    private static final int TYP_LEGACY_HYS = 11;
    private static final int TYP_LEGACY_ADD = 12;
    private static final int TYP_LEGACY_SUB = 13;
    private static final int TYP_LEGACY_MUL = 14;
    private static final int TYP_LEGACY_DIV = 15;
    private static final int TYP_LEGACY_NOT = 18;
    private static final int TYP_LEGACY_AND = 19;
    private static final int TYP_LEGACY_OR = 20;
    private static final int TYP_LEGACY_XOR = 21;
    private static final int TYP_LEGACY_DELAY = 25;
    private static final int TYP_LEGACY_SAMPLEHOLD = 26;
    private static final int TYP_LEGACY_LIMIT = 27;
    private static final int TYP_LEGACY_PD = 29;
    private static final int TYP_LEGACY_ABS = 32;
    private static final int TYP_LEGACY_SIN = 34;
    private static final int TYP_LEGACY_COS = 36;
    private static final int TYP_LEGACY_EXP = 40;
    private static final int TYP_LEGACY_LN = 41;
    private static final int TYP_LEGACY_SQRT = 43;
    private static final int TYP_LEGACY_GE = 45;
    private static final int TYP_LEGACY_GT = 46;
    private static final int TYP_LEGACY_MIN = 49;
    private static final int TYP_LEGACY_MAX = 50;
    private static final int TYP_LEGACY_TIME = 58;
    private static final int TYP_LEGACY_INT = 64;
    private static final int TYP_JAVA_FUNCTION = 61;
    private static final int TYP_SCRIPT = 1016;
    private static final int TYP_NATIVE_C = 88;

    // Web catalog control block types (CircuitTypCore CTRL_* range); the web
    // editor places these type numbers, the classic editor used 1..6. Literal
    // values (switch-case constants) — kept in sync with the enum by
    // NativeControlBlockTest.
    private static final int TYP_GATE_WEB = 1000;
    private static final int TYP_VOLT_WEB = 1001;
    private static final int TYP_AMP_WEB = 1002;
    private static final int TYP_SCOPE_WEB = 1003;
    private static final int TYP_SIGNAL_WEB = 1004;
    private static final int TYP_CONSTANT_WEB = 1005;
    private static final int TYP_GAIN = 1006;
    private static final int TYP_PI = 1007;
    private static final int TYP_PT1 = 1008;
    private static final int TYP_INTEGRATOR = 1009;
    private static final int TYP_COMPARATOR = 1010;
    private static final int TYP_AND = 1011;
    private static final int TYP_OR = 1012;
    private static final int TYP_NOT = 1013;
    private static final int TYP_SELECTOR = 1014;
    private static final int TYP_DELAY = 1015;
    private static final int TYP_SUB = 1017;
    private static final int TYP_ADD = 1018;
    private static final int TYP_MUL = 1019;
    private static final int TYP_DIV = 1020;
    private static final int TYP_LIMIT = 1021;
    private static final int TYP_ABS = 1022;
    private static final int TYP_SQRT = 1023;
    private static final int TYP_EXP = 1024;
    private static final int TYP_LN = 1025;
    private static final int TYP_SIN = 1026;
    private static final int TYP_COS = 1027;
    private static final int TYP_MIN = 1028;
    private static final int TYP_MAX = 1029;
    private static final int TYP_HYS = 1030;
    private static final int TYP_PT2 = 1031;
    private static final int TYP_PD = 1032;
    private static final int TYP_SAMPLEHOLD = 1033;
    private static final int TYP_TIME = 1034;
    private static final int TYP_XOR = 1035;
    private static final int TYP_GE = 1036;
    private static final int TYP_DEADTIME = 1037;

    // Parameter slot layouts of the web catalog control blocks
    /** CTRL_CONSTANT: constant output value. */
    private static final int CONSTANT_VALUE = 0;
    /** CTRL_GAIN: multiplication factor. */
    private static final int GAIN_FACTOR = 0;
    /** CTRL_PI: proportional gain Kp (series form y = Kp·(x + 1/Ti·∫x dt)). */
    private static final int PI_PROPORTIONAL_GAIN = 0;
    /** CTRL_PI: integration time Ti in seconds; a non-positive Ti degrades to P-only. */
    private static final int PI_INTEGRATION_TIME = 1;
    /** CTRL_PT1: filter time constant in seconds (unity DC gain). */
    private static final int PT1_TIME_CONSTANT = 0;
    /** CTRL_PT1: the web catalog exposes only the time constant; DC gain is fixed. */
    private static final double PT1_DC_GAIN = 1.0;
    /** CTRL_INTEGRATOR: initial integrator state. */
    private static final int INTEGRATOR_INITIAL_VALUE = 0;
    /** CTRL_INTEGRATOR: integration gain a1 of G(s) = a1/s (classic default). */
    private static final double INTEGRATOR_GAIN = 1.0;
    /** CTRL_INTEGRATOR: symmetric bound making the classic limiter ineffective. */
    private static final double UNLIMITED_INTEGRATOR_BOUND = Double.MAX_VALUE;
    /** CTRL_DELAY: transport delay time in seconds. */
    private static final int DELAY_TIME = 0;

    /**
     * Terminal layout per control type: {inputs, outputs, output x-offset}.
     * Inputs sit at rel (-2, -i); outputs at (xPos, -j) — the xPos=2 cases
     * follow {@code RegelBlock}'s classic rules. The web catalog integrator
     * declares two inputs: input 1 is the optional reset (a value &ge; 1
     * resets the state to the initial value; unwired resets to 0).
     */
    private static final Map<Integer, int[]> TERMINALS_BY_TYPE = Map.ofEntries(
            // Classic blocks
            Map.entry(TYP_VOLTMEETER, new int[]{0, 1, 2}),
            Map.entry(TYP_AMMETER, new int[]{0, 1, 2}),
            Map.entry(TYP_CONSTANT, new int[]{0, 1, 2}),
            Map.entry(TYP_SIGNAL_SOURCE, new int[]{0, 1, 2}),
            Map.entry(TYP_SCOPE, new int[]{3, 0, 2}),
            Map.entry(TYP_GATE, new int[]{1, 0, 2}),
            Map.entry(TYP_LEGACY_GAIN, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_PT1, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_PT2, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_PI, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_HYS, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_ADD, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_SUB, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_MUL, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_DIV, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_NOT, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_AND, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_OR, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_XOR, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_DELAY, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_SAMPLEHOLD, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_LIMIT, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_PD, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_ABS, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_SIN, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_COS, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_EXP, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_LN, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_SQRT, new int[]{1, 1, 2}),
            Map.entry(TYP_LEGACY_GE, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_GT, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_MIN, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_MAX, new int[]{2, 1, 2}),
            Map.entry(TYP_LEGACY_TIME, new int[]{0, 1, 2}),
            Map.entry(TYP_LEGACY_INT, new int[]{2, 1, 2}),

            // Web catalog blocks
            Map.entry(TYP_GATE_WEB, new int[]{1, 0, 2}),
            Map.entry(TYP_VOLT_WEB, new int[]{0, 1, 2}),
            Map.entry(TYP_AMP_WEB, new int[]{0, 1, 2}),
            Map.entry(TYP_SCOPE_WEB, new int[]{3, 0, 2}),
            Map.entry(TYP_SIGNAL_WEB, new int[]{0, 1, 2}),
            Map.entry(TYP_CONSTANT_WEB, new int[]{0, 1, 2}),
            Map.entry(TYP_GAIN, new int[]{1, 1, 2}),
            Map.entry(TYP_PI, new int[]{1, 1, 2}),
            Map.entry(TYP_PT1, new int[]{1, 1, 2}),
            Map.entry(TYP_INTEGRATOR, new int[]{2, 1, 2}),
            Map.entry(TYP_COMPARATOR, new int[]{2, 1, 2}),
            Map.entry(TYP_AND, new int[]{2, 1, 2}),
            Map.entry(TYP_OR, new int[]{2, 1, 2}),
            Map.entry(TYP_NOT, new int[]{1, 1, 2}),
            Map.entry(TYP_SELECTOR, new int[]{3, 1, 2}),
            Map.entry(TYP_DELAY, new int[]{1, 1, 2}),
            Map.entry(TYP_SUB, new int[]{2, 1, 2}),
            Map.entry(TYP_ADD, new int[]{2, 1, 2}),
            Map.entry(TYP_MUL, new int[]{2, 1, 2}),
            Map.entry(TYP_DIV, new int[]{2, 1, 2}),
            Map.entry(TYP_LIMIT, new int[]{1, 1, 2}),
            Map.entry(TYP_ABS, new int[]{1, 1, 2}),
            Map.entry(TYP_SQRT, new int[]{1, 1, 2}),
            Map.entry(TYP_EXP, new int[]{1, 1, 2}),
            Map.entry(TYP_LN, new int[]{1, 1, 2}),
            Map.entry(TYP_SIN, new int[]{1, 1, 2}),
            Map.entry(TYP_COS, new int[]{1, 1, 2}),
            Map.entry(TYP_MIN, new int[]{2, 1, 2}),
            Map.entry(TYP_MAX, new int[]{2, 1, 2}),
            Map.entry(TYP_HYS, new int[]{1, 1, 2}),
            Map.entry(TYP_PT2, new int[]{1, 1, 2}),
            Map.entry(TYP_PD, new int[]{1, 1, 2}),
            Map.entry(TYP_SAMPLEHOLD, new int[]{2, 1, 2}),
            Map.entry(TYP_TIME, new int[]{0, 1, 2}),
            Map.entry(TYP_XOR, new int[]{2, 1, 2}),
            Map.entry(TYP_GE, new int[]{2, 1, 2}),
            Map.entry(TYP_DEADTIME, new int[]{1, 2, 2}));

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
            List<Probe> probes,
            List<SignalTap> signalTaps,
            List<String> warnings) {

        public ControlCoupling {
            warnings = warnings != null ? warnings : List.of();
        }

        /**
         * Convenience constructor for couplings without build issues
         * (empty control section, tests).
         */
        public ControlCoupling(List<AbstractControlCalculatable> calculators,
                               List<GateDrive> gateDrives,
                               List<Probe> probes,
                               List<SignalTap> signalTaps) {
            this(calculators, gateDrives, probes, signalTaps, List.of());
        }

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
         * the switched resistance. Like the classic engine, the matrix for
         * time step t is stamped with the control value computed at t-1: the
         * gates applied here lag the logged values by one step.
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
     * alias of the producer's output array and is read live each step. The
     * drive carries the PREVIOUS step's signal: the classic engine stamps the
     * matrix for step t with the control value from t-1.
     */
    public static final class GateDrive {
        private final int elementIndex;
        private final CircuitTypCore switchType;
        private final GateCalculator gate;
        private double previousSignal;

        GateDrive(int elementIndex, CircuitTypCore switchType, GateCalculator gate) {
            this.elementIndex = elementIndex;
            this.switchType = switchType;
            this.gate = gate;
        }

        public int elementIndex() {
            return elementIndex;
        }

        public CircuitTypCore switchType() {
            return switchType;
        }

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
            double signal = previousSignal;
            previousSignal = gateSignal();
            boolean on = signal > AbstractControlCalculatable.SIGNAL_THRESHOLD;
            if (switchType == CircuitTypCore.LK_THYR || switchType == CircuitTypCore.LK_IGBT) {
                // The piecewise-linear state machine in ComponentCurrentCalculator
                // owns the resistance flip; it only sees the 0/1 gate status.
                if (params.length > PARAM_GATE) {
                    params[PARAM_GATE] = on ? 1.0 : 0.0;
                }
                return;
            }
            if (params.length > PARAM_GATE) {
                params[PARAM_GATE] = signal;
            }
            int onSlot = onResistanceSlot();
            int offSlot = offResistanceSlot();
            double resistance = on && onSlot < params.length ? params[onSlot]
                    : (!on && offSlot < params.length ? params[offSlot] : params[PARAM_RESISTANCE]);
            params[PARAM_RESISTANCE] = resistance;
        }
    }

    /**
     * A voltmeter or ammeter block: either attached to a power component via
     * {@code coupledReferenceID} or a component name, or measuring between two
     * node labels. Its output array is written from the solved circuit each
     * step (voltage across the element / nodes, current through it).
     */
    public record Probe(int elementIndex, boolean current, String name, String componentName,
                        AbstractControlCalculatable outputHolder, int nodeX, int nodeY) {

        public Probe(int elementIndex, boolean current, String name,
                     AbstractControlCalculatable outputHolder, int nodeX, int nodeY) {
            this(elementIndex, current, name, name, outputHolder, nodeX, nodeY);
        }

        private void update(CircuitNetlist netlist, double[] nodeVoltages) {
            double value;
            if (current) {
                double[] currents = netlist.getLastComponentCurrentsRef();
                value = elementIndex < currents.length ? currents[elementIndex] : 0.0;
            } else if (elementIndex >= 0) {
                int x = netlist.getNodeX(elementIndex);
                int y = netlist.getNodeY(elementIndex);
                value = (x < nodeVoltages.length ? nodeVoltages[x] : 0.0)
                        - (y < nodeVoltages.length ? nodeVoltages[y] : 0.0);
            } else {
                value = (nodeX < nodeVoltages.length ? nodeVoltages[nodeX] : 0.0)
                        - (nodeY < nodeVoltages.length ? nodeVoltages[nodeY] : 0.0);
            }
            outputHolder._outputSignal[0][0] = value;
        }
    }

    /**
     * A recordable control-domain output: a signal source or constant whose
     * output terminal carries a user label ({@code labelEndKnoten}), logged
     * under that label like the classic scope curve (e.g. "gate").
     */
    public record SignalTap(String name, AbstractControlCalculatable source, int outputIndex) {
        public SignalTap(String name, AbstractControlCalculatable source) {
            this(name, source, 0);
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
        if (controlComponents.isEmpty()) {
            return new ControlCoupling(List.of(), List.of(), List.of(), List.of());
        }
        Map<Long, Integer> elementIndexByUid = new HashMap<>();
        if (netlist != null) {
            for (int i = 0; i < netlist.getElementCount(); i++) {
                long uid = netlist.getElementUids()[i];
                if (uid != 0) {
                    elementIndexByUid.put(uid, i);
                }
            }
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
        List<SignalTap> signalTaps = new ArrayList<>();
        Set<String> usedTapNames = new HashSet<>();
        // Dropped gates/probes and unknown voltmeter nodes are user-visible
        // degradation, not internal errors — collect them so the engine can
        // surface them with the other run warnings.
        List<String> warnings = new ArrayList<>();

        for (CircuitModel.ComponentData comp : controlComponents) {
            // scopes are pure display instruments with nothing to calculate —
            // skipping them is expected behavior, not degradation worth a warning
            if (isDisplayOnlyType(comp.getType())) {
                continue;
            }
            AbstractControlCalculatable calculator = createCalculator(comp);
            if (calculator == null) {
                warnings.add("Control block '" + comp.getName() + "' has an unsupported type and is skipped");
                continue;
            }
            calculatorByComp.put(keyOf(comp), calculator);
            if (calculator instanceof GateCalculator) {
                Integer elementIndex = resolveCoupledElementIndex(comp, model, netlist, elementIndexByUid);
                if (elementIndex != null && isSwitch(netlist.getType(elementIndex))) {
                    gateDrives.add(new GateDrive(elementIndex, netlist.getType(elementIndex), gate0(comp, calculatorByComp)));
                } else {
                    String msg = "Gate '" + comp.getName() + "' references an unknown or non-switch power component and never switches";
                    warnings.add(msg);
                    LOGGER.warn("Gate '{}' references unknown power component", comp.getName());
                }
            }
            if (isVoltageProbe(comp.getType()) || isCurrentProbe(comp.getType())) {
                Probe probe = buildProbe(comp, calculator, elementIndexByUid, model, netlist, warnings);
                if (probe != null) {
                    probes.add(probe);
                } else {
                    String msg = "Measurement '" + comp.getName()
                            + "' references an unknown power component or labels and exports 0";
                    warnings.add(msg);
                    LOGGER.warn("Measurement '{}' references unknown power component or labels",
                            comp.getName());
                }
            }
            // recordable outputs: like the classic scope, the exported name is
            // the block's output terminal label (labelEndKnoten)
            if (isLabeledTapType(comp.getType())) {
                String label = outputLabel(comp);
                if (!label.equals(displayName(comp)) && usedTapNames.add(label)) {
                    signalTaps.add(new SignalTap(label, calculator));
                }
            } else if (comp.getType() == TYP_JAVA_FUNCTION || comp.getType() == TYP_SCRIPT
                    || comp.getType() == TYP_NATIVE_C || comp.getType() == TYP_DEADTIME) {
                int[] layout = getTerminalLayout(comp);
                String[] yLabels = comp.getTerminalYLabels();
                for (int j = 0; j < (layout != null ? layout[1] : 1); j++) {
                    String label = (yLabels != null && j < yLabels.length && yLabels[j] != null && !yLabels[j].trim().isEmpty() && !yLabels[j].equals("NIX_NIX_NIX"))
                            ? yLabels[j].trim()
                            : (displayName(comp) + (layout != null && layout[1] > 1 ? ("." + (j + 1)) : ""));
                    if (usedTapNames.add(label)) {
                        signalTaps.add(new SignalTap(label, calculator, j));
                    }
                }
            }
        }

        wireInputs(controlComponents, calculatorByComp, parent);

        return new ControlCoupling(topologicalOrder(calculatorByComp), gateDrives, probes,
                signalTaps, warnings);
    }

    /** Output terminal label ({@code labelEndKnoten}) or the block name fallback. */
    private static String outputLabel(CircuitModel.ComponentData comp) {
        String[] yLabels = comp.getTerminalYLabels();
        if (yLabels.length > 0 && yLabels[0] != null) {
            String label = yLabels[0].trim();
            if (!label.isEmpty() && !label.equals("NIX_NIX_NIX")) {
                return label;
            }
        }
        return displayName(comp);
    }

    private static GateCalculator gate0(CircuitModel.ComponentData comp,
                                        Map<String, AbstractControlCalculatable> calculatorByComp) {
        return (GateCalculator) calculatorByComp.get(keyOf(comp));
    }

    /**
     * Resolves the power element a control block couples to: uid coupling
     * ({@code coupledReferenceID}) or coupled component name
     * ({@code parameterString[0]}, the classic GUI fallback).
     */
    private static Integer resolveCoupledElementIndex(CircuitModel.ComponentData comp,
                                                      CircuitModel model, CircuitNetlist netlist,
                                                      Map<Long, Integer> elementIndexByUid) {
        String[] measured = comp.getParameterStrings();
        if (measured != null && measured.length > 0) {
            String target = measured[0] == null ? "" : measured[0].trim();
            if (target.startsWith("/")) {
                target = target.substring(1);
            }
            if (!target.isEmpty() && !target.equalsIgnoreCase("NIX_NIX_NIX")) {
                Integer byName = elementIndexByName(model, netlist, target);
                if (byName != null) {
                    return byName;
                }
            }
        }
        if (comp.getCoupledReferenceID() != 0) {
            Integer byUid = elementIndexByUid.get(comp.getCoupledReferenceID());
            if (byUid != null) {
                return byUid;
            }
        }
        Object coupledParam = comp.getParameters().get("coupledComponent");
        if (coupledParam != null) {
            String target = coupledParam.toString().trim();
            if (target.startsWith("/")) {
                target = target.substring(1);
            }
            if (!target.isEmpty() && !target.equalsIgnoreCase("NIX_NIX_NIX")) {
                Integer byName = elementIndexByName(model, netlist, target);
                if (byName != null) {
                    return byName;
                }
            }
        }
        return null;
    }

    /**
     * Resolves a measurement block to its power-domain target: uid coupling,
     * coupled component name ({@code parameterString[0]}), or for voltmeters
     * the measured node-label pair ({@code parameterString[0..1]}).
     */
    private static Probe buildProbe(CircuitModel.ComponentData comp,
                                    AbstractControlCalculatable calculator,
                                    Map<Long, Integer> elementIndexByUid,
                                    CircuitModel model, CircuitNetlist netlist,
                                    List<String> warnings) {
        boolean current = isCurrentProbe(comp.getType());
        String name = outputLabel(comp);
        String compName = comp.getName() != null ? comp.getName().trim() : "";
        Integer coupled = resolveCoupledElementIndex(comp, model, netlist, elementIndexByUid);
        if (current) {
            if (coupled != null) {
                return new Probe(coupled, true, name, compName, calculator, -1, -1);
            }
            return null;
        }
        if (coupled != null) {
            return new Probe(coupled, false, name, compName, calculator, -1, -1);
        }
        String[] measured = comp.getParameterStrings() != null
                ? comp.getParameterStrings() : new String[0];
        if (measured.length < 2) {
            Object nA = comp.getParameters().get("nodeA");
            Object nB = comp.getParameters().get("nodeB");
            if (nA != null && nB != null) {
                measured = new String[]{nA.toString(), nB.toString()};
            }
        }
        if (measured.length >= 2) {
            int nodeX = nodeForLabel(netlist, measured[0]);
            int nodeY = nodeForLabel(netlist, measured[1]);
            if (nodeX < 0 || nodeY < 0) {
                String bad = nodeX < 0 ? measured[0] : measured[1];
                warnings.add("Voltmeter '" + comp.getName() + "' references unknown node label '"
                        + bareLabel(bad) + "' — treated as ground, so it reads 0");
                return new Probe(-1, false, name, compName, calculator,
                        Math.max(nodeX, 0), Math.max(nodeY, 0));
            }
            return new Probe(-1, false, name, compName, calculator, nodeX, nodeY);
        }
        return null;
    }

    private static int nodeForLabel(CircuitNetlist netlist, String label) {
        if (netlist == null || label == null) {
            return 0;
        }
        String trimmed = label.trim();
        if (trimmed.isEmpty() || trimmed.equals("NIX_NIX_NIX")) {
            return 0;
        }
        // .ipes writes labels with a leading '/' (parameterString[] /V_in/0/0)
        // while the label resolver keys them bare, so a verbatim lookup always
        // missed and every voltmeter probed 0-0. Prefer the bare form, keep the
        // raw form as fallback for labels genuinely stored with the slash.
        String bare = bareLabel(trimmed);
        if (bare.isEmpty() || bare.equals("0") || bare.equalsIgnoreCase("gnd") || bare.equalsIgnoreCase("ground")) {
            return 0;
        }
        int index = netlist.getLabelResolver().getIndex(bare);
        if (index < 0 && !bare.equals(trimmed)) {
            index = netlist.getLabelResolver().getIndex(trimmed);
        }
        // -1 marks an unknown label so the caller can warn; ground and unset
        // sentinels stay 0 by design.
        return index;
    }

    /** Leading-'/' classic label token ("/V_in") → bare form ("V_in"). */
    private static String bareLabel(String label) {
        String trimmed = label.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1).trim() : trimmed;
    }

    private static Integer elementIndexByName(CircuitModel model, CircuitNetlist netlist, String name) {
        if (model == null || netlist == null || name == null) {
            return null;
        }
        for (CircuitModel.ComponentData comp : model.getCircuitComponents()) {
            if (name.equals(comp.getName())) {
                int index = netlist.indexOfUid(comp.getUniqueObjectIdentifier());
                return index >= 0 ? index : null;
            }
        }
        return null;
    }

    /**
     * Resolves the typ-88 library path: new circuits carry it in the
     * 'libraryPath' parameter; classic circuits store the selected library
     * name in 'nativeCLibrary' and the candidate paths (';'-separated) in
     * 'nativeCLibraries'.
     */
    private static String nativeLibraryPath(CircuitModel.ComponentData comp) {
        String direct = getStringParam(comp, "libraryPath", "");
        if (!direct.isBlank()) {
            return direct;
        }
        String selected = getStringParam(comp, "nativeCLibrary", "");
        String candidates = getStringParam(comp, "nativeCLibraries", "");
        if (selected.isBlank() || candidates.isBlank()) {
            return "";
        }
        for (String candidate : candidates.split(";")) {
            if (candidate.isBlank()) {
                continue;
            }
            String fileName = candidate.replace('\\', '/');
            int slash = fileName.lastIndexOf('/');
            if (slash >= 0) {
                fileName = fileName.substring(slash + 1);
            }
            if (fileName.equals(selected)) {
                return candidate;
            }
        }
        // fall back to the selected name itself (classic behavior: it may be
        // a bare file name resolved against the working directory)
        return selected;
    }

    private static int[] getTerminalLayout(CircuitModel.ComponentData comp) {
        if (comp.getType() == TYP_NATIVE_C) {
            // classic NativeC defaults: 3 inputs, 2 outputs
            int numIn = getIntParam(comp, "anzXIN", -1);
            int numOut = getIntParam(comp, "anzYOUT", -1);
            if (numIn < 0) {
                String[] xLabels = comp.getRawTerminalXLabels();
                numIn = (xLabels != null && xLabels.length > 0) ? xLabels.length : 3;
            }
            if (numOut < 0) {
                String[] yLabels = comp.getRawTerminalYLabels();
                numOut = (yLabels != null && yLabels.length > 0) ? yLabels.length : 2;
            }
            return new int[]{Math.max(0, numIn), Math.max(1, numOut), 2};
        }
        if (comp.getType() == TYP_JAVA_FUNCTION || comp.getType() == TYP_SCRIPT) {
            int numIn = getIntParam(comp, "anzXIN", -1);
            if (numIn < 0) {
                numIn = getIntParam(comp, "numberInputTerminals", -1);
            }
            if (numIn < 0) {
                String[] xLabels = comp.getRawTerminalXLabels();
                numIn = (xLabels != null && xLabels.length > 0) ? xLabels.length : 1;
            }

            int numOut = getIntParam(comp, "anzYOUT", -1);
            if (numOut < 0) {
                numOut = getIntParam(comp, "numberOutputTerminals", -1);
            }
            if (numOut < 0) {
                String[] yLabels = comp.getRawTerminalYLabels();
                numOut = (yLabels != null && yLabels.length > 0) ? yLabels.length : 1;
            }
            return new int[]{Math.max(0, numIn), Math.max(1, numOut), 2};
        }
        return TERMINALS_BY_TYPE.get(comp.getType());
    }

    private static int getIntParam(CircuitModel.ComponentData comp, String key, int def) {
        Object v = comp.getParameters().get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        return def;
    }

    private static String getStringParam(CircuitModel.ComponentData comp, String key, String def) {
        Object v = comp.getParameters().get(key);
        if (v instanceof String s) {
            return s;
        }
        return def;
    }

    private static AbstractControlCalculatable createCalculator(CircuitModel.ComponentData comp) {
        int[] layout = getTerminalLayout(comp);
        if (layout == null) {
            LOGGER.warn("Control block '{}' has unsupported typ {} - skipped",
                    comp.getName(), comp.getType());
            return null;
        }
        if (comp.getType() == TYP_NATIVE_C) {
            return new CLibraryCalculator(layout[0], layout[1], nativeLibraryPath(comp));
        }
        if (comp.getType() == TYP_JAVA_FUNCTION || comp.getType() == TYP_SCRIPT) {
            String sourceCode = getStringParam(comp, "sourceCode", "");
            String staticCode = getStringParam(comp, "staticCode", "");
            String staticVariables = getStringParam(comp, "staticVariables", "");
            ScriptBlockCalculator script =
                    new ScriptBlockCalculator(layout[0], layout[1], sourceCode, staticCode, staticVariables);
            script.setBlockName(comp.getName());
            return script;
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
            // --- web catalog range (CircuitTypCore CTRL_*) ---
            case TYP_GATE_WEB -> new GateCalculator();
            case TYP_VOLT_WEB, TYP_AMP_WEB -> new ConstantCalculator(0);
            case TYP_SCOPE_WEB -> null;
            case TYP_SIGNAL_WEB -> createSignalSource(params);
            case TYP_CONSTANT_WEB -> new ConstantCalculator(param(params, CONSTANT_VALUE));
            case TYP_GAIN -> new GainCalculator(param(params, GAIN_FACTOR));
            case TYP_PI -> createPiCalculator(params);
            case TYP_PT1 -> new PT1Calculator(param(params, PT1_TIME_CONSTANT), PT1_DC_GAIN);
            case TYP_INTEGRATOR -> createIntegrator(params);
            case TYP_COMPARATOR -> new GreaterThanCalculator();
            case TYP_AND, TYP_LEGACY_AND -> new AndTwoPortCalculator();
            case TYP_OR, TYP_LEGACY_OR -> new OrCalculatorTwoInputs();
            case TYP_NOT, TYP_LEGACY_NOT -> new NotCalculator();
            case TYP_SELECTOR -> new SignalSelectorCalculator();
            case TYP_DELAY, TYP_LEGACY_DELAY -> createDelayCalculator(params);
            case TYP_SUB, TYP_LEGACY_SUB -> new SubtractionTwoParameter();
            case TYP_ADD, TYP_LEGACY_ADD -> new AddCalculator();
            case TYP_MUL, TYP_LEGACY_MUL -> new MulCalculator();
            case TYP_DIV, TYP_LEGACY_DIV -> new DivCalculator();
            case TYP_LIMIT, TYP_LEGACY_LIMIT -> createLimitCalculator(params);
            case TYP_ABS, TYP_LEGACY_ABS -> new AbsCalculator();
            case TYP_SQRT, TYP_LEGACY_SQRT -> new SqrtCalculator();
            case TYP_EXP, TYP_LEGACY_EXP -> new ExpCalculator();
            case TYP_LN, TYP_LEGACY_LN -> new LnCalculator();
            case TYP_SIN, TYP_LEGACY_SIN -> new SinCalculator();
            case TYP_COS, TYP_LEGACY_COS -> new CosCalculator();
            case TYP_MIN, TYP_LEGACY_MIN -> new MinCalculatorTwoInputs();
            case TYP_MAX, TYP_LEGACY_MAX -> new MaxCalculatorTwoInputs();
            case TYP_HYS, TYP_LEGACY_HYS -> createHysteresisCalculator(params);
            case TYP_PT2, TYP_LEGACY_PT2 -> createPt2Calculator(params);
            case TYP_PD, TYP_LEGACY_PD -> createPdCalculator(params);
            case TYP_SAMPLEHOLD, TYP_LEGACY_SAMPLEHOLD -> new SampleHoldCalculator();
            case TYP_TIME, TYP_LEGACY_TIME -> new TimeCalculator();
            case TYP_XOR, TYP_LEGACY_XOR -> new XORCalculator();
            case TYP_GE, TYP_LEGACY_GE -> new GreaterEqualCalculator();
            case TYP_DEADTIME -> createDeadTimeCalculator(params);
            case TYP_LEGACY_GAIN -> new GainCalculator(param(params, GAIN_FACTOR));
            case TYP_LEGACY_PT1 -> new PT1Calculator(param(params, PT1_TIME_CONSTANT), PT1_DC_GAIN);
            case TYP_LEGACY_PI -> createPiCalculator(params);
            case TYP_LEGACY_GT -> new GreaterThanCalculator();
            case TYP_LEGACY_INT -> createIntegrator(params);
            default -> null;
        };
    }

    private static AbstractControlCalculatable createLimitCalculator(double[] params) {
        double min = params != null && params.length > 0 ? params[0] : -1.0;
        double max = params != null && params.length > 1 ? params[1] : 1.0;
        if (min >= max) {
            min = -1.0;
            max = 1.0;
        }
        return new LimitCalculatorInternal(min, max);
    }

    private static AbstractControlCalculatable createHysteresisCalculator(double[] params) {
        double h = param(params, 0);
        if (h <= 0.0) {
            h = 0.1;
        }
        return new HysteresisCalculatorInternal(h);
    }

    private static AbstractControlCalculatable createPt2Calculator(double[] params) {
        double tau = param(params, 0);
        if (tau <= 0.0) {
            tau = 0.001;
        }
        double gain = params != null && params.length > 1 ? params[1] : 1.0;
        return new PT2Calculator(tau, gain);
    }

    private static AbstractControlCalculatable createPdCalculator(double[] params) {
        double gain = param(params, 0);
        if (gain == 0.0 && (params == null || params.length == 0)) {
            gain = 1.0;
        }
        return new PDCalculator(gain);
    }

    private static AbstractControlCalculatable createDeadTimeCalculator(double[] params) {
        double tDead = param(params, 0);
        if (tDead <= 0.0 && (params == null || params.length == 0)) {
            tDead = 200e-9;
        }
        return new DeadTimeCalculator(tDead);
    }

    /**
     * Creates the CTRL_PI calculator from its web catalog slots. The block
     * implements the series form {@code y = Kp·(x + (1/Ti)·∫x dt)}: slot 0
     * carries Kp, slot 1 the integration time Ti in seconds. A non-positive
     * Ti degrades the block to pure proportional action.
     */
    private static AbstractControlCalculatable createPiCalculator(double[] params) {
        double proportionalGain = param(params, PI_PROPORTIONAL_GAIN);
        double integrationTime = param(params, PI_INTEGRATION_TIME);
        double integralGain = integrationTime > 0.0 ? proportionalGain / integrationTime : 0.0;
        return new PICalculator(proportionalGain, integralGain);
    }

    /**
     * Creates the CTRL_INTEGRATOR calculator: {@code G(s) = 1/s} with the
     * initial state from slot 0 and no limiting. Input 1 is the optional
     * reset (a value &ge; 1 resets the state); unwired resets stay 0.
     */
    private static AbstractControlCalculatable createIntegrator(double[] params) {
        return new IntegratorCalculation(INTEGRATOR_GAIN, param(params, INTEGRATOR_INITIAL_VALUE),
                -UNLIMITED_INTEGRATOR_BOUND, UNLIMITED_INTEGRATOR_BOUND);
    }

    /** Creates the CTRL_DELAY calculator, degrading invalid delay times to a zero source. */
    private static AbstractControlCalculatable createDelayCalculator(double[] params) {
        try {
            return new DelayCalculator(param(params, DELAY_TIME));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid delay time: {}", e.getMessage());
            return new ConstantCalculator(0);
        }
    }

    private static AbstractControlCalculatable createSignalSource(double[] params) {
        int sourceType = (int) param(params, SIGNAL_SOURCE_TYPE);
        double amplitude = param(params, SIGNAL_AMPLITUDE);
        double frequency = param(params, SIGNAL_FREQUENCY);
        double offset = param(params, SIGNAL_DC_OFFSET);
        double phase = param(params, SIGNAL_PHASE_RADIANS);
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
            int[] layout = getTerminalLayout(comp);
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
            int[] layout = getTerminalLayout(comp);
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

    /** Voltage probe blocks: the classic type 1 and the web catalog CTRL_VOLT. */
    private static boolean isVoltageProbe(int type) {
        return type == TYP_VOLTMEETER || type == TYP_VOLT_WEB;
    }

    /** Current probe blocks: the classic type 2 and the web catalog CTRL_AMP. */
    private static boolean isCurrentProbe(int type) {
        return type == TYP_AMMETER || type == TYP_AMP_WEB;
    }

    /** Display-only blocks: the classic scope type 5 and the web catalog CTRL_SCOPE. */
    private static boolean isDisplayOnlyType(int type) {
        return type == TYP_SCOPE || type == TYP_SCOPE_WEB;
    }

    /**
     * Control blocks whose labeled output becomes a recordable signal tap:
     * sources/constants (classic + web) and the web catalog blocks with a
     * single executable output. Latching on an explicit output label keeps
     * unlabeled internal blocks out of the signal list.
     */
    private static boolean isLabeledTapType(int type) {
        return type == TYP_SIGNAL_SOURCE || type == TYP_SIGNAL_WEB
                || type == TYP_CONSTANT || type == TYP_CONSTANT_WEB
                || type == TYP_GAIN || type == TYP_LEGACY_GAIN
                || type == TYP_PI || type == TYP_LEGACY_PI
                || type == TYP_PT1 || type == TYP_LEGACY_PT1
                || type == TYP_INTEGRATOR || type == TYP_LEGACY_INT
                || type == TYP_COMPARATOR || type == TYP_LEGACY_GT
                || type == TYP_AND || type == TYP_LEGACY_AND
                || type == TYP_OR || type == TYP_LEGACY_OR
                || type == TYP_NOT || type == TYP_LEGACY_NOT
                || type == TYP_SELECTOR || type == TYP_DELAY || type == TYP_LEGACY_DELAY
                || type == TYP_SUB || type == TYP_LEGACY_SUB
                || type == TYP_ADD || type == TYP_LEGACY_ADD
                || type == TYP_MUL || type == TYP_LEGACY_MUL
                || type == TYP_DIV || type == TYP_LEGACY_DIV
                || type == TYP_LIMIT || type == TYP_LEGACY_LIMIT
                || type == TYP_ABS || type == TYP_LEGACY_ABS
                || type == TYP_SQRT || type == TYP_LEGACY_SQRT
                || type == TYP_EXP || type == TYP_LEGACY_EXP
                || type == TYP_LN || type == TYP_LEGACY_LN
                || type == TYP_SIN || type == TYP_LEGACY_SIN
                || type == TYP_COS || type == TYP_LEGACY_COS
                || type == TYP_MIN || type == TYP_LEGACY_MIN
                || type == TYP_MAX || type == TYP_LEGACY_MAX
                || type == TYP_HYS || type == TYP_LEGACY_HYS
                || type == TYP_PT2 || type == TYP_LEGACY_PT2
                || type == TYP_PD || type == TYP_LEGACY_PD
                || type == TYP_SAMPLEHOLD || type == TYP_LEGACY_SAMPLEHOLD
                || type == TYP_TIME || type == TYP_LEGACY_TIME
                || type == TYP_XOR || type == TYP_LEGACY_XOR
                || type == TYP_GE || type == TYP_LEGACY_GE;
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
