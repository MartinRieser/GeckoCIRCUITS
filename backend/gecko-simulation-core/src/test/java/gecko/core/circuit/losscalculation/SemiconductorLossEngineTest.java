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
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemiconductorLossEngineTest {

    private static final double EPSILON = 1e-6;

    private static File resolveCircuit(String relativePath) {
        Path path = Paths.get("src/test/resources/ipes", relativePath);
        File file = path.toFile();
        if (!file.exists()) {
            path = Paths.get("backend/gecko-simulation-core/src/test/resources/ipes", relativePath);
            file = path.toFile();
        }
        return file;
    }

    @Test
    @DisplayName("Engine discovers only semiconductor switches from mixed circuit netlist")
    void discoverSemiconductorsFromNetlist() {
        CircuitTypCore[] types = {
            CircuitTypCore.LK_R,      // 0: Resistor
            CircuitTypCore.LK_D,      // 1: Diode
            CircuitTypCore.LK_L,      // 2: Inductor
            CircuitTypCore.LK_MOSFET, // 3: MOSFET
            CircuitTypCore.LK_U       // 4: Voltage source
        };
        int[] nodeX = new int[5];
        int[] nodeY = new int[5];
        int[] vsNr = {-1, -1, -1, -1, 1};
        double[][] parameters = new double[5][15];

        // Diode parameters: forward drop 0.7V, r_on 0.02 Ohm
        parameters[1][DiodeParameters.INDEX_FORWARD_VOLTAGE] = 0.7;
        parameters[1][DiodeParameters.INDEX_R_ON] = 0.02;

        // MOSFET parameters: r_on 0.05 Ohm
        parameters[3][SwitchParameters.INDEX_R_ON] = 0.05;

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, vsNr, parameters, 3, 1, 5);

        SemiconductorLossEngine engine = new SemiconductorLossEngine().initializeFromNetlist(netlist);

        assertEquals(2, engine.getDeviceList().size());
        assertNotNull(engine.getModel(1), "Diode must be indexed");
        assertNotNull(engine.getModel(3), "MOSFET must be indexed");
        assertEquals(CircuitTypCore.LK_D, engine.getModel(1).getComponentType());
        assertEquals(CircuitTypCore.LK_MOSFET, engine.getModel(3).getComponentType());
    }

    @Test
    @DisplayName("Step evaluation calculates losses and updates DomainCoupler power array")
    void stepEvaluationAndDomainCoupling() {
        CircuitTypCore[] types = {CircuitTypCore.LK_D};
        int[] nodeX = new int[1];
        int[] nodeY = new int[1];
        int[] vsNr = {-1};
        double[][] parameters = new double[1][15];

        parameters[0][DiodeParameters.INDEX_CURRENT_RESISTANCE] = 0.01; // Conducting
        parameters[0][DiodeParameters.INDEX_FORWARD_VOLTAGE] = 0.7;
        parameters[0][DiodeParameters.INDEX_R_ON] = 0.01;
        parameters[0][DiodeParameters.INDEX_CURRENT] = 10.0;
        parameters[0][DiodeParameters.INDEX_VOLTAGE] = 0.8;

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, vsNr, parameters, 2, 0, 1);

        SemiconductorLossEngine engine = new SemiconductorLossEngine().initializeFromNetlist(netlist);
        DomainCoupler coupler = new DomainCoupler();
        coupler.configurePowerLossArray(1);

        engine.calculateStep(netlist, coupler, 1e-4, 1e-4);

        // P_cond = 0.7 * 10 + 0.01 * 10^2 = 7.0 + 1.0 = 8.0 W
        assertEquals(8.0, engine.getTotalConductionLosses(), EPSILON);
        assertEquals(8.0, engine.getTotalLosses(), EPSILON);
        assertEquals(8.0 * 1e-4, engine.getCumulativeEnergy(), EPSILON);

        // Verify DomainCoupler received the dissipated power
        double[] couplerLosses = coupler.getLkPowerLosses();
        assertEquals(1, couplerLosses.length);
        assertEquals(8.0, couplerLosses[0], EPSILON);
    }

    @Test
    @DisplayName("Junction temperatures resolve through the explicit device-to-thermal-node mapping")
    void thermalMappingResolvesPerDeviceTemperatures() {
        CircuitTypCore[] types = {CircuitTypCore.LK_D, CircuitTypCore.LK_D};
        int[] nodeX = new int[2];
        int[] nodeY = new int[2];
        int[] vsNr = {-1, -1};
        double[][] parameters = new double[2][15];

        for (int i = 0; i < 2; i++) {
            parameters[i][DiodeParameters.INDEX_CURRENT_RESISTANCE] = 0.01; // Conducting
            parameters[i][DiodeParameters.INDEX_FORWARD_VOLTAGE] = 0.7;
            parameters[i][DiodeParameters.INDEX_R_ON] = 0.01;
            parameters[i][DiodeParameters.INDEX_CURRENT] = 10.0;
            parameters[i][DiodeParameters.INDEX_VOLTAGE] = 0.8;
        }

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, vsNr, parameters, 2, 0, 2);

        SemiconductorLossEngine engine = new SemiconductorLossEngine().initializeFromNetlist(netlist);

        // Reconfigure with temperature-dependent R_on (alpha = 1 %/°C) and no switching losses
        for (SemiconductorDeviceLossModel model : engine.getDeviceList()) {
            model.configurePiecewiseLinearConduction(0.7, 0.01, 0.01);
            model.configureScaledEnergySwitching(0.0, 0.0, 10.0, 600.0, 0.0);
        }

        DomainCoupler coupler = new DomainCoupler();
        coupler.configureThermArray(1);
        coupler.setThermNodeTemperature(0, 125.0);
        // Device 0 mapped to the 125 °C thermal node, device 1 without a thermal model
        coupler.configureThermToLkDeviceMapping(new int[] {0, -1});

        engine.calculateStep(netlist, coupler, 1e-4, 1e-4);

        // Device 0 at 125 °C: R_on(T) = 0.01 * (1 + 0.01 * 100) = 0.02 Ohm
        // P_cond = 0.7 * 10 + 0.02 * 10^2 = 9.0 W
        assertEquals(9.0, engine.getModel(0).getConductionLoss(), EPSILON,
                "Mapped device must evaluate at its thermal node's temperature");
        // Device 1 unmapped: default 25 °C -> R_on = 0.01 Ohm -> P_cond = 8.0 W
        assertEquals(8.0, engine.getModel(1).getConductionLoss(), EPSILON,
                "Unmapped device must evaluate at the default junction temperature");

        assertArrayEquals(new double[] {9.0, 8.0}, engine.getAllPowerLosses(), EPSILON);
        assertArrayEquals(new double[] {9.0, 8.0}, coupler.getLkPowerLosses(), EPSILON);
    }

    @Test
    @DisplayName("Loss signal recognition and query evaluation")
    void lossSignalsRecognitionAndEvaluation() {
        SemiconductorDeviceLossModel model = new SemiconductorDeviceLossModel(0, "D1", CircuitTypCore.LK_D);
        model.configurePiecewiseLinearConduction(0.7, 0.01, 0.0);
        model.calculateStep(10.0, 0.8, true, 25.0, 1e-4, 1e-4);

        SemiconductorLossEngine engine = new SemiconductorLossEngine();
        engine.registerDeviceModel(model);

        // Device name containing an underscore must round-trip through both
        // recognition and evaluation with identical prefix handling
        SemiconductorDeviceLossModel underscoreModel =
                new SemiconductorDeviceLossModel(1, "T1_low", CircuitTypCore.LK_IGBT);
        underscoreModel.configurePiecewiseLinearConduction(1.0, 0.1, 0.0);
        underscoreModel.calculateStep(10.0, 1.5, true, 25.0, 1e-4, 1e-4); // P = 10 + 10 = 20 W
        engine.registerDeviceModel(underscoreModel);

        assertTrue(engine.hasLossSignal("P_loss_total"));
        assertTrue(engine.hasLossSignal("P_cond_total"));
        assertTrue(engine.hasLossSignal("P_sw_total"));
        assertTrue(engine.hasLossSignal("E_loss_total"));
        assertTrue(engine.hasLossSignal("P_loss_D1"));
        assertTrue(engine.hasLossSignal("P_cond_D1"));
        assertTrue(engine.hasLossSignal("P_loss_T1_low"), "Device names with underscores must be recognized");
        assertTrue(engine.hasLossSignal("P_loss[0]"));
        assertFalse(engine.hasLossSignal("unknown_signal"));

        // Aggregates span both devices: D1 8 W + T1_low 20 W = 28 W
        assertEquals(28.0, engine.evaluateLossSignal("P_loss_total"), EPSILON);
        assertEquals(28.0, engine.evaluateLossSignal("P_cond_total"), EPSILON);
        assertEquals(0.0, engine.evaluateLossSignal("P_sw_total"), EPSILON);
        assertEquals(28.0 * 1e-4, engine.evaluateLossSignal("E_loss_total"), EPSILON);
        assertEquals(8.0, engine.evaluateLossSignal("P_loss_D1"), EPSILON);
        assertEquals(8.0, engine.evaluateLossSignal("P_loss[0]"), EPSILON);
        assertEquals(20.0, engine.evaluateLossSignal("P_loss_T1_low"), EPSILON);

        Map<String, LossContainer> breakdown = engine.getLossBreakdown();
        assertTrue(breakdown.containsKey("D1"));
        assertEquals(8.0, breakdown.get("D1").getTotalLosses(), EPSILON);
    }

    @Test
    @DisplayName("Re-registering a model replaces the previous entry for the same device")
    void reRegistrationReplacesPreviousModel() {
        SemiconductorDeviceLossModel original = new SemiconductorDeviceLossModel(0, "D1", CircuitTypCore.LK_D);
        original.configurePiecewiseLinearConduction(0.7, 0.01, 0.0);
        original.calculateStep(10.0, 0.8, true, 25.0, 1e-4, 1e-4); // P = 7 + 1 = 8 W

        SemiconductorLossEngine engine = new SemiconductorLossEngine();
        engine.registerDeviceModel(original);

        SemiconductorDeviceLossModel replacement = new SemiconductorDeviceLossModel(0, "D1", CircuitTypCore.LK_D);
        replacement.configurePiecewiseLinearConduction(0.7, 0.02, 0.0);
        replacement.calculateStep(10.0, 0.8, true, 25.0, 1e-4, 1e-4); // P = 7 + 2 = 9 W
        engine.registerDeviceModel(replacement);

        assertEquals(1, engine.getDeviceList().size(), "Device list must not keep the stale model");
        assertSame(replacement, engine.getModel(0));
        assertSame(replacement, engine.getModel("D1"));
        assertEquals(9.0, engine.getTotalLosses(), EPSILON);
        assertEquals(9.0, engine.evaluateLossSignal("P_loss_D1"), EPSILON);
    }

    @Test
    @DisplayName("HeadlessSimulationEngine records loss signals into DataContainerGlobal")
    void headlessSimulationEngineWithLossChannels() {
        File file = resolveCircuit("buck_converter_web.ipes");
        assertTrue(file.exists(), "buck_converter_web.ipes must exist at " + file.getAbsolutePath());

        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        SimulationConfig config = SimulationConfig.builder()
            .circuitFile(file.getAbsolutePath())
            .stepWidth(1e-6)
            .simulationDuration(1e-3)
            .signals(List.of("uOUT", "P_loss_total", "E_loss_total"))
            .build();

        SimulationResult result = engine.runSimulation(config);

        assertTrue(result.isSuccess(), "Simulation must succeed: " + result.getErrorMessage());
        assertNotNull(result.getDataContainer());
        String[] signals = result.getSignalNames();
        assertTrue(signals.length >= 3);
        List<String> signalList = Arrays.asList(signals);
        assertTrue(signalList.contains("P_loss_total"), "Signal list must contain P_loss_total");
        assertTrue(signalList.contains("E_loss_total"), "Signal list must contain E_loss_total");

        // Loss should be recorded in metadata
        assertNotNull(result.getMetadata().get("totalConductionLoss"));
        assertNotNull(result.getMetadata().get("totalSwitchingLoss"));
        assertNotNull(result.getMetadata().get("totalLossEnergy"));
        double totalLossEnergy = (double) result.getMetadata().get("totalLossEnergy");
        assertTrue(totalLossEnergy >= 0.0);
    }
}
