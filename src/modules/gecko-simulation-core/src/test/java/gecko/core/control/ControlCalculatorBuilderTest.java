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

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.GateCalculator;
import gecko.core.control.calculators.SignalCalculatorRectangle;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the headless CONTROL domain construction: calculator
 * creation, wire-based input wiring, coupledReferenceID resolution and
 * gate-to-switch parameter application.
 */
class ControlCalculatorBuilderTest {

    private static final String BUCK_WITH_CONTROL = """
        tDURATION 0.005
        dt 5e-07
        e (0)
        <ElementLK>
        labelAnfangsKnoten[] /1
        labelEndKnoten[] /0
        typ 4
        uniqueObjectIdentifier 100
        x 10
        y 18
        parameter[] 401.0 100.0
        orientierung 503
        idStringDialog U.1
        <\\ElementLK>
        e (1)
        <ElementLK>
        labelAnfangsKnoten[] /1
        labelEndKnoten[] /2
        typ 10
        uniqueObjectIdentifier 101
        x 15
        y 13
        parameter[] 10000000.0 0.6 0.01 10000000.0
        orientierung 502
        idStringDialog IGBT.1
        <\\ElementLK>
        e (2)
        <ElementLK>
        labelAnfangsKnoten[] /2
        labelEndKnoten[] /0
        typ 1
        uniqueObjectIdentifier 102
        x 30
        y 18
        parameter[] 10.0
        orientierung 503
        idStringDialog R.1
        <\\ElementLK>
        c (0)
        <ElementCONTROL>
        typ 4
        uniqueObjectIdentifier 200
        x 25
        y 46
        parameter[] 404.0 1.0 1000.0 0.0 0.0 0.5
        orientierung 503
        idStringDialog SIGNAL.1
        <\\ElementCONTROL>
        c (1)
        <ElementCONTROL>
        typ 6
        uniqueObjectIdentifier 201
        x 37
        y 46
        parameter[] 0.0
        coupledReferenceID[] 101
        orientierung 503
        idStringDialog GATE.1
        <\\ElementCONTROL>
        c (2)
        <ElementCONTROL>
        typ 1
        uniqueObjectIdentifier 202
        x 67
        y 21
        parameter[] 0.0
        coupledReferenceID[] 102
        orientierung 503
        idStringDialog VOLT.1
        <\\ElementCONTROL>
        verbindungCONTROL (0)
        <Connection>
        label NIX_NIX_NIX
        x[] 27 35
        y[] 46 46
        enabledShorted 1
        parentSheetIdentifier 0
        connectorType 1
        <\\Connection>
        """;

    private CircuitModel model;
    private CircuitNetlist netlist;
    private ControlCalculatorBuilder.ControlCoupling coupling;

    @BeforeEach
    void setUp() throws Exception {
        model = new CircuitFileParser().parse(
                new BufferedReader(new StringReader(BUCK_WITH_CONTROL)), "test.ipes");
        netlist = NetlistBuilder.buildFromCircuitModel(model);
        coupling = ControlCalculatorBuilder.build(model, netlist);
    }

    @Test
    void calculators_containsOnlyTheExecutableSource() {
        // gate is NotCalculateableMarker, voltmeter holds its output,
        // so only the rectangle source is executed
        assertEquals(1, coupling.calculators().size());
        assertInstanceOf(SignalCalculatorRectangle.class, coupling.calculators().get(0));
    }

    @Test
    void gateInput_isAliasedToTheSignalSourceOutput() {
        assertEquals(1, coupling.gateDrives().size());
        ControlCalculatorBuilder.GateDrive drive = coupling.gateDrives().get(0);

        coupling.initialize(1e-6);
        coupling.calculators().get(0).calculateYOUT(1e-6);
        double sourceValue = coupling.calculators().get(0)._outputSignal[0][0];
        // rectangle, duty 0.5: the first half period is high
        assertEquals(1.0, sourceValue, 1e-12);
        assertEquals(sourceValue, drive.gateSignal(), 1e-12,
                "the gate must read the source output live through the aliased array");
    }

    @Test
    void gateDrive_targetsTheIgbtElement() {
        ControlCalculatorBuilder.GateDrive drive = coupling.gateDrives().get(0);
        assertEquals(1, drive.elementIndex(), "IGBT.1 is the second circuit component");
        assertEquals(CircuitTypCore.LK_IGBT, drive.switchType());
    }

    @Test
    void applyGateSignals_writesGateStatusForTheStateMachine() {
        ControlCalculatorBuilder.GateDrive drive = coupling.gateDrives().get(0);
        SignalCalculatorRectangle source = (SignalCalculatorRectangle) coupling.calculators().get(0);
        assertEquals(CircuitTypCore.LK_IGBT, netlist.getType(drive.elementIndex()));

        // high signal -> gate status 1 in slot 8. The resistance flip of an
        // IGBT is owned by the piecewise-linear state machine in
        // ComponentCurrentCalculator (legacy semantics), not by the gate drive.
        // Like the classic engine, the applied gate lags the computed signal
        // by one step: the first apply seeds the previous value.
        coupling.initialize(1e-6);
        source.calculateYOUT(1e-6);
        assertEquals(1.0, source._outputSignal[0][0], 1e-12);
        coupling.applyGateSignals(netlist);
        coupling.applyGateSignals(netlist);
        assertEquals(1.0, netlist.getParameter(drive.elementIndex())[8], 1e-12);

        // advance into the low half of the period -> gate status 0 one step later
        boolean reachedLow = false;
        for (int i = 0; i < 2000 && !reachedLow; i++) {
            source.calculateYOUT(1e-6);
            reachedLow = source._outputSignal[0][0] == 0.0;
        }
        assertTrue(reachedLow, "rectangle must reach its low phase within one period");
        coupling.applyGateSignals(netlist);
        coupling.applyGateSignals(netlist);
        assertEquals(0.0, netlist.getParameter(drive.elementIndex())[8], 1e-12);
    }

    @Test
    void voltmeterProbe_readsVoltageAcrossTheCoupledResistor() {
        assertEquals(1, coupling.probes().size());
        ControlCalculatorBuilder.Probe probe = coupling.probes().get(0);
        assertEquals(2, probe.elementIndex(), "R.1 is the third circuit component");
        assertEquals("VOLT.1", probe.name());

        // node 2 at 50 V, node 0 grounded
        double[] nodeVoltages = new double[netlist.getNodeMax() + 1];
        nodeVoltages[2] = 50.0;
        coupling.updateProbes(netlist, nodeVoltages);
        assertEquals(50.0, probe.outputHolder()._outputSignal[0][0], 1e-12);
        assertTrue(coupling.probeSignalNames().contains("VOLT.1"));
    }

    @Test
    void ammeterProbe_readsCurrentThroughCoupledResistor() throws Exception {
        String content = BUCK_WITH_CONTROL.replace("typ 1\r\nuniqueObjectIdentifier 202", "typ 2\r\nuniqueObjectIdentifier 202")
                .replace("typ 1\nuniqueObjectIdentifier 202", "typ 2\nuniqueObjectIdentifier 202")
                .replace("idStringDialog VOLT.1", "idStringDialog AMP.1");
        CircuitModel ammeterModel = new CircuitFileParser().parse(
                new BufferedReader(new StringReader(content)), "test.ipes");
        CircuitNetlist ammeterNetlist = NetlistBuilder.buildFromCircuitModel(ammeterModel);
        ControlCalculatorBuilder.ControlCoupling ammeterCoupling = ControlCalculatorBuilder.build(ammeterModel, ammeterNetlist);

        assertEquals(1, ammeterCoupling.probes().size());
        ControlCalculatorBuilder.Probe probe = ammeterCoupling.probes().get(0);
        assertTrue(probe.current());
        assertEquals(2, probe.elementIndex());
        assertEquals("AMP.1", probe.name());

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
                .circuitModel(ammeterModel)
                .stepWidth(1e-6)
                .simulationDuration(0.005)
                .solverType(SolverType.SOLVER_BE)
                .build();
        SimulationResult result = engine.runSimulation(config);
        assertTrue(result.isSuccess());
        String[] sigs = result.getSignalNames();
        assertTrue(java.util.Arrays.asList(sigs).contains("AMP.1"));
        int ampIdx = java.util.Arrays.asList(sigs).indexOf("AMP.1");
        float[] values = result.getSignalData(ampIdx);
        assertNotNull(values);
        float maxI = 0f;
        for (float v : values) {
            if (v > maxI) maxI = v;
        }
        System.out.println("AMP.1 max current through R.1: " + maxI);
        assertTrue(maxI > 0.1f, "Expected non-zero current through resistor, got " + maxI);
    }

    @Test
    void buckConverterWeb_simulatesWithAmmeterOnRL() throws Exception {
        java.io.InputStream is = getClass().getResourceAsStream("/ipes/buck_converter_web.ipes");
        assertNotNull(is);
        CircuitModel buckModel = new CircuitFileParser().parse(new BufferedReader(new java.io.InputStreamReader(is)), "buck.ipes");

        // First simulate as-is (AMP.1 on L.1)
        HeadlessSimulationEngine engine1 = new HeadlessSimulationEngine();
        SimulationResult res1 = engine1.runSimulation(SimulationConfig.builder()
                .circuitModel(buckModel)
                .stepWidth(1e-6)
                .simulationDuration(0.005)
                .solverType(SolverType.SOLVER_BE)
                .build());
        assertTrue(res1.isSuccess());
        int iL1Idx = java.util.Arrays.asList(res1.getSignalNames()).indexOf("iL1");
        assertTrue(iL1Idx >= 0);
        float[] iL1Values = res1.getSignalData(iL1Idx);
        float maxIL1 = 0f;
        for (float v : iL1Values) if (v > maxIL1) maxIL1 = v;

        // Now patch AMP.1 to couple to R.L
        for (CircuitModel.ComponentData c : buckModel.getControlComponents()) {
            if ("AMP.1".equals(c.getName())) {
                c.getParameterStrings()[0] = "R.L";
            }
        }

        HeadlessSimulationEngine engine2 = new HeadlessSimulationEngine();
        SimulationResult res2 = engine2.runSimulation(SimulationConfig.builder()
                .circuitModel(buckModel)
                .stepWidth(1e-6)
                .simulationDuration(0.005)
                .solverType(SolverType.SOLVER_BE)
                .build());
        assertTrue(res2.isSuccess());
        int rlIdx = java.util.Arrays.asList(res2.getSignalNames()).indexOf("iL1");
        assertTrue(rlIdx >= 0, "Signal iL1 should be recorded: " + java.util.Arrays.toString(res2.getSignalNames()));
        float[] rlValues = res2.getSignalData(rlIdx);
        float maxRL = 0f;
        for (float v : rlValues) if (Math.abs(v) > maxRL) maxRL = Math.abs(v);
        System.out.println("Buck AMP.1 on R.L max magnitude: " + maxRL);
        assertEquals(maxIL1, maxRL, 0.05f, "Current through series resistor R.L must match inductor current iL1!");
    }

    @Test
    void gateWithoutCoupledComponent_isSkipped() throws Exception {
        String content = BUCK_WITH_CONTROL.replace("coupledReferenceID[] 101", "coupledReferenceID[] 999");
        CircuitModel broken = new CircuitFileParser().parse(
                new BufferedReader(new StringReader(content)), "test.ipes");
        CircuitNetlist brokenNetlist = NetlistBuilder.buildFromCircuitModel(broken);

        ControlCalculatorBuilder.ControlCoupling result = ControlCalculatorBuilder.build(broken, brokenNetlist);

        assertEquals(0, result.gateDrives().size(), "unresolvable references must not crash the build");
        assertEquals(1, result.probes().size());
    }

    @Test
    void emptyModel_yieldsEmptyCoupling() {
        ControlCalculatorBuilder.ControlCoupling result =
                ControlCalculatorBuilder.build(new CircuitModel(), null);
        assertTrue(result.calculators().isEmpty());
        assertTrue(result.gateDrives().isEmpty());
        assertTrue(result.probes().isEmpty());
    }

    @Test
    void initialize_preparesPeriodicSourcesWithoutError() {
        GateCalculator unused = new GateCalculator();
        assertEquals(1, unused._inputSignal.length);
        coupling.initialize(1e-6);
        AbstractControlCalculatable source = coupling.calculators().get(0);
        source.calculateYOUT(1e-6);
        assertEquals(1.0, source._outputSignal[0][0], 1e-12);
    }
}
