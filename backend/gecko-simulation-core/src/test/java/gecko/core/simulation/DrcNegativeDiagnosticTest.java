package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.ControlCalculatorBuilder;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Strategy 5: Silent-Failure & Negative DRC Diagnostic Audit.
 *
 * <p>Audits deliberate misconfigurations and edge-case circuits to ensure
 * the engine, netlist builder, and DRC diagnostics surface loud, human-readable
 * error messages and warnings instead of silently failing, blanking scopes, or
 * crashing with unhandled runtime exceptions.</p>
 */
class DrcNegativeDiagnosticTest {

    private static CircuitModel.ComponentData createComp(
            String family, int type, String name, int x, int y, int orient,
            double[] rawParams, String[] paramStrs, Map<String, Object> params, long uid) {
        CircuitModel.ComponentData comp = new CircuitModel.ComponentData(type, name, x, y, orient);
        comp.setFamily(family);
        if (rawParams != null) comp.setRawParameters(rawParams);
        if (paramStrs != null) comp.setParameterStrings(paramStrs);
        if (params != null) {
            params.forEach(comp::setParameter);
        }
        comp.setUniqueObjectIdentifier(uid);
        return comp;
    }

    @Test
    @DisplayName("Unconnected Voltmeter probe emits DRC_PROBE_ERROR diagnostic")
    void unconnectedVoltmeterEmitsDrcWarning() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(1e-3);

        // Voltmeter placed in space with no wires or coupled nodes
        CircuitModel.ComponentData volt = createComp(
                "CONTROL", 1001, "VOLT_FLOATING", 20, 20, 501,
                new double[]{}, new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX"},
                Map.of(), 1001L);
        model.addControlComponent(volt);

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        ControlCalculatorBuilder.ControlCoupling coupling = ControlCalculatorBuilder.build(model, netlist);

        assertTrue(coupling.warnings().stream().anyMatch(w -> w.contains("DRC_PROBE_ERROR") && w.contains("VOLT_FLOATING")),
                () -> "Unconnected voltmeter must emit DRC_PROBE_ERROR, got: " + coupling.warnings());
    }

    @Test
    @DisplayName("Gate driver referencing non-switch or non-existent component emits DRC_PROBE_ERROR")
    void gateReferencingNonSwitchEmitsDrcWarning() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(1e-3);

        // Resistor R1
        CircuitModel.ComponentData r1 = createComp(
                "LK", 1, "R1", 10, 10, 503,
                new double[]{10.0}, new String[]{"NIX_NIX_NIX", "0"},
                Map.of(), 101L);
        model.addCircuitComponent(r1);

        // Gate G1 erroneously coupled to R1
        CircuitModel.ComponentData gate = createComp(
                "CONTROL", 1000, "G1", 15, 10, 501,
                new double[]{}, new String[]{"R1", "0"},
                Map.of("coupledComponent", "R1"), 102L);
        model.addControlComponent(gate);

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        ControlCalculatorBuilder.ControlCoupling coupling = ControlCalculatorBuilder.build(model, netlist);

        assertTrue(coupling.warnings().stream().anyMatch(w -> w.contains("DRC_PROBE_ERROR") && w.contains("G1")),
                () -> "Gate coupled to non-switch must emit DRC_PROBE_ERROR, got: " + coupling.warnings());
    }

    @Test
    @DisplayName("Floating subcircuit without ground reference emits DRC_FLOATING_GROUND")
    void floatingSubcircuitWithoutGroundEmitsDrcWarning() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(1e-3);

        // Isolated R1 and C1 loop with no ground and no voltage source
        // R1 placed at (10, 10) NORTH_SOUTH: term X at (10, 8), term Y at (10, 12)
        CircuitModel.ComponentData r1 = createComp(
                "LK", 1, "R1", 10, 10, 503,
                new double[]{10.0}, null, Map.of(), 201L);
        CircuitModel.ComponentData c1 = createComp(
                "LK", 3, "C1", 14, 10, 503,
                new double[]{1e-6}, null, Map.of(), 202L);
        model.addCircuitComponent(r1);
        model.addCircuitComponent(c1);

        // Wire 1: connects top pins (10, 8) to (14, 8)
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{10, 8}, {14, 8}}));
        // Wire 2: connects bottom pins (10, 12) to (14, 12)
        model.getConnections().add(new CircuitModel.ConnectionData("LK",
                new int[][]{{10, 12}, {14, 12}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        List<String> warnings = netlist.getBuildWarnings();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("DRC_FLOATING_GROUND")),
                () -> "Floating subcircuit must trigger DRC_FLOATING_GROUND warning, got: " + warnings);
    }

    @Test
    @DisplayName("Direct short-circuit across branch component emits DRC short-circuit warning")
    void shortCircuitedBranchComponentEmitsDrcWarning() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(1e-3);

        // Resistor R_SHORT at (10, 10) with orientation 503: leads at (10, 8) and (10, 12)
        CircuitModel.ComponentData rShort = createComp(
                "LK", 1, "R_SHORT", 10, 10, 503,
                new double[]{100.0}, null, Map.of(), 301L);
        model.addCircuitComponent(rShort);

        // A wire looping from term X (10, 8) to term Y (10, 12), directly shorting R_SHORT
        model.addConnection(new CircuitModel.ConnectionData("LK",
                new int[][]{{10, 8}, {14, 8}, {14, 12}, {10, 12}}));

        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        List<String> warnings = netlist.getBuildWarnings();

        assertTrue(warnings.stream().anyMatch(w -> w.contains("DRC: Branch component 'R_SHORT'") && w.contains("short-circuited")),
                () -> "Directly shorted component must trigger DRC short-circuit warning, got: " + warnings);
    }

    @Test
    @DisplayName("Division by zero in control blocks is numerically safe and does not crash")
    void divisionByZeroSafety() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(1e-3);

        // Constant 5.0 at (10, 10)
        CircuitModel.ComponentData numer = createComp(
                "CONTROL", 1005, "NUMER", 10, 10, 501,
                new double[]{5.0}, new String[]{},
                Map.of("param0", 5.0), 401L);
        numer.setTerminalYLabels(new String[]{"n_out"});
        model.addControlComponent(numer);

        // Constant 0.0 at (10, 14)
        CircuitModel.ComponentData denom = createComp(
                "CONTROL", 1005, "DENOM", 10, 14, 501,
                new double[]{0.0}, new String[]{},
                Map.of("param0", 0.0), 402L);
        denom.setTerminalYLabels(new String[]{"d_out"});
        model.addControlComponent(denom);

        // Div block at (16, 12) taking n_out and d_out
        CircuitModel.ComponentData div = createComp(
                "CONTROL", 1020, "DIV1", 16, 12, 501,
                new double[]{}, new String[]{},
                Map.of(), 403L);
        div.setTerminalXLabels(new String[]{"n_out", "d_out"});
        div.setTerminalYLabels(new String[]{"div_out"});
        model.addControlComponent(div);

        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .stepWidth(1e-6)
                .simulationDuration(1e-4)
                .signals(List.of("div_out"))
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), "Simulation with division by zero must not throw or fail");

        float[] divOut = result.getSignalData(0);
        assertNotNull(divOut);
        for (int i = 0; i < divOut.length; i++) {
            assertFalse(Float.isNaN(divOut[i]), "div_out at step " + i + " must not be NaN");
            assertFalse(Float.isInfinite(divOut[i]), "div_out at step " + i + " must not be Infinite");
        }
    }

    @Test
    @DisplayName("Circuit with zero resolvable signals emits DRC_NO_SIGNALS warning")
    void zeroObservableSignalsEmitsDrcWarning() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(1e-4);

        // V1 at (10, 10, 503): leads at (10, 8) and (10, 12)
        CircuitModel.ComponentData v1 = createComp(
                "LK", 4, "V1", 10, 10, 503,
                new double[]{401.0, 12.0, 50.0}, null, Map.of(), 501L);
        // R1 at (14, 10, 503): leads at (14, 8) and (14, 12)
        CircuitModel.ComponentData r1 = createComp(
                "LK", 1, "R1", 14, 10, 503,
                new double[]{10.0}, null, Map.of(), 502L);
        // GND at (10, 12)
        CircuitModel.ComponentData gnd = createComp(
                "LK", 31, "GND", 10, 12, 503,
                new double[]{}, null, Map.of(), 503L);

        model.addCircuitComponent(v1);
        model.addCircuitComponent(r1);
        model.addCircuitComponent(gnd);

        // Wire top leads: (10, 8) to (14, 8)
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{10, 8}, {14, 8}}));
        // Wire bottom leads: (10, 12) to (14, 12)
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{10, 12}, {14, 12}}));

        // Request a signal name that cannot be resolved to any node or tap
        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .stepWidth(1e-6)
                .simulationDuration(1e-4)
                .signals(List.of("phantom_nonexistent_signal"))
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), "Simulation should still solve physics successfully: " + result.getErrorMessage());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("DRC_UNRESOLVED_SIGNAL") || w.contains("DRC_NO_SIGNALS")),
                () -> "Must warn when requested signals cannot be resolved, got: " + result.getWarnings());
    }

    @Test
    @DisplayName("issue.ipes circuit configuration emits loud DRC diagnostics")
    void issueIpesCircuitConfigurationEmitsLoudDrcDiagnostics() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(1e-3);

        // The user's exact issue.ipes layout:
        // DC source + R + IGBT + C in loop with ground
        CircuitModel.ComponentData u = createComp(
                "LK", 4, "U", 10, 10, 501,
                new double[]{401.0, 24.0, 50.0}, null,
                Map.of(), 601L);
        u.setTerminalXLabels(new String[]{"0"});
        u.setTerminalYLabels(new String[]{"n1"});

        CircuitModel.ComponentData r = createComp(
                "LK", 1, "R", 14, 10, 501,
                new double[]{10.0}, null,
                Map.of(), 602L);
        r.setTerminalXLabels(new String[]{"n1"});
        r.setTerminalYLabels(new String[]{"n2"});

        CircuitModel.ComponentData igbt = createComp(
                "LK", 10, "IGBT", 18, 10, 501,
                new double[]{}, null,
                Map.of(), 603L);
        igbt.setTerminalXLabels(new String[]{"n2"});
        igbt.setTerminalYLabels(new String[]{"n3"});

        CircuitModel.ComponentData c = createComp(
                "LK", 3, "C", 22, 10, 501,
                new double[]{1e-6}, null,
                Map.of(), 604L);
        c.setTerminalXLabels(new String[]{"n3"});
        c.setTerminalYLabels(new String[]{"0"});

        CircuitModel.ComponentData gnd = createComp(
                "LK", 31, "GLOBAL_TERMINAL", 10, 14, 501,
                new double[]{}, null,
                Map.of(), 605L);
        gnd.setTerminalXLabels(new String[]{"0"});

        // Control: Signal -> Gate (uncoupled to IGBT)
        CircuitModel.ComponentData sig = createComp(
                "CONTROL", 1004, "SIGNAL", 10, 18, 501,
                new double[]{401.0, 1.0, 1000.0}, new String[]{},
                Map.of(), 606L);
        CircuitModel.ComponentData gate = createComp(
                "CONTROL", 1000, "GATE", 14, 18, 501,
                new double[]{}, new String[]{"NIX_NIX_NIX", "0"},
                Map.of(), 607L);

        // Floating Voltmeter (the reason user saw no scope signals)
        CircuitModel.ComponentData volt = createComp(
                "CONTROL", 1001, "VOLT", 26, 10, 501,
                new double[]{}, new String[]{"NIX_NIX_NIX", "NIX_NIX_NIX"},
                Map.of(), 608L);

        model.addCircuitComponent(u);
        model.addCircuitComponent(r);
        model.addCircuitComponent(igbt);
        model.addCircuitComponent(c);
        model.addCircuitComponent(gnd);
        model.addControlComponent(sig);
        model.addControlComponent(gate);
        model.addControlComponent(volt);

        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .stepWidth(1e-6)
                .simulationDuration(1e-4)
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), "Valid RC loop in issue.ipes should solve without singular matrix error: " + result.getErrorMessage());
        List<String> warnings = result.getWarnings();

        // 1. Must warn about Gate having no coupled switch
        assertTrue(warnings.stream().anyMatch(w -> w.contains("DRC_PROBE_ERROR") && w.contains("GATE")),
                () -> "Must warn about uncoupled Gate, got: " + warnings);

        // 2. Must warn about Voltmeter having no connections
        assertTrue(warnings.stream().anyMatch(w -> w.contains("DRC_PROBE_ERROR") && w.contains("VOLT")),
                () -> "Must warn about unconnected Voltmeter, got: " + warnings);
    }

    @Test
    @DisplayName("Pin-to-pin touching components without wire segments build real netlist and solve")
    void pinToPinCoincidentComponentsSolveWithoutExplicitWires() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(1e-4);

        // V1 at (10, 10, 501) SOUTH_NORTH: input at (10, 12) (-), output at (10, 8) (+)
        CircuitModel.ComponentData v1 = createComp(
                "LK", 4, "V1", 10, 10, 501,
                new double[]{401.0, 20.0, 50.0}, null, Map.of(), 701L);
        // R1 at (10, 10, 503) NORTH_SOUTH: input at (10, 8), output at (10, 12) — pins coincide directly with V1!
        CircuitModel.ComponentData r1 = createComp(
                "LK", 1, "R1", 10, 10, 503,
                new double[]{10.0}, null, Map.of(), 702L);
        // GND at (10, 14, 503) NORTH_SOUTH: pin at (10, 12)
        CircuitModel.ComponentData gnd = createComp(
                "LK", 31, "GND", 10, 14, 503,
                new double[]{}, null, Map.of(), 703L);

        // Ammeter coupled to R1
        CircuitModel.ComponentData amp = createComp(
                "CONTROL", 1002, "AMP1", 20, 10, 501,
                new double[]{}, new String[]{"R1"}, Map.of("coupledComponent", "R1"), 704L);
        amp.setCoupledReferenceID(702L);
        amp.setTerminalYLabels(new String[]{"I_R1"});

        model.addCircuitComponent(v1);
        model.addCircuitComponent(r1);
        model.addCircuitComponent(gnd);
        model.addControlComponent(amp);

        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(model)
                .stepWidth(1e-5)
                .simulationDuration(5e-5)
                .signals(List.of("I_R1"))
                .solverType(SolverType.SOLVER_BE)
                .build();

        SimulationResult result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), "Pin-to-pin wire-free circuit must solve cleanly: " + result.getErrorMessage());
        float[] iR1 = result.getSignalData(0);
        assertNotNull(iR1);
        assertEquals(2.0f, Math.abs(iR1[iR1.length - 1]), 1e-3f, "20V across 10 Ohm resistor must yield 2.0 A");
    }
}
