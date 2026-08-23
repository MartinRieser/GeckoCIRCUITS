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

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.GateCalculator;
import gecko.core.control.calculators.SignalCalculatorRectangle;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
    void applyGateSignals_switchesResistanceBetweenRonAndRoff() {
        ControlCalculatorBuilder.GateDrive drive = coupling.gateDrives().get(0);
        SignalCalculatorRectangle source = (SignalCalculatorRectangle) coupling.calculators().get(0);
        assertEquals(CircuitTypCore.LK_IGBT, netlist.getType(drive.elementIndex()));

        // high signal -> ON resistance (slot 2 = 0.01)
        coupling.initialize(1e-6);
        source.calculateYOUT(1e-6);
        assertEquals(1.0, source._outputSignal[0][0], 1e-12);
        coupling.applyGateSignals(netlist);
        assertEquals(0.01, netlist.getParameter(drive.elementIndex())[0], 1e-12);
        assertEquals(1.0, netlist.getParameter(drive.elementIndex())[8], 1e-12);

        // advance into the low half of the period -> OFF resistance (slot 3 = 1e7)
        boolean reachedLow = false;
        for (int i = 0; i < 2000 && !reachedLow; i++) {
            source.calculateYOUT(1e-6);
            reachedLow = source._outputSignal[0][0] == 0.0;
        }
        assertTrue(reachedLow, "rectangle must reach its low phase within one period");
        coupling.applyGateSignals(netlist);
        assertEquals(1.0E7, netlist.getParameter(drive.elementIndex())[0], 1e-3);
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
