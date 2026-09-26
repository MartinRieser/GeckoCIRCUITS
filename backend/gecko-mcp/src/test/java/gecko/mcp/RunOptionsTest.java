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
package gecko.mcp;

import gecko.core.allg.SolverType;
import gecko.core.simulation.MatrixSolverKind;
import gecko.core.simulation.SemiconductorModelKind;
import gecko.core.simulation.SimulationConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link RunOptions} JSON-to-config mapping and the signal
 * discovery: the full simulation surface (overrides, matrix solver, adaptive
 * stepping, semiconductor model, thermal and magnetic domains) must be
 * reachable from the MCP argument maps.
 */
class RunOptionsTest {

    private static final double DT = 1e-6;

    /** Divider (24 V source, 2 ohm + load resistor) written as an .ipes file. */
    private static Path writeDividerCircuit(double loadResistance) throws Exception {
        Path file = Files.createTempFile("gecko-runopts", ".ipes");
        String content = """
            tDURATION 0.002
            dt 1e-06
            e (0)
            <ElementLK>
            labelAnfangsKnoten[] /vin
            labelEndKnoten[] /0
            typ 4
            uniqueObjectIdentifier 100
            x 10
            y 16
            parameter[] 401.0 24.0
            orientierung 503
            idStringDialog U.1
            <\\ElementLK>
            e (1)
            <ElementLK>
            labelAnfangsKnoten[] /vin
            labelEndKnoten[] /vmid
            typ 1
            uniqueObjectIdentifier 101
            x 10
            y 28
            parameter[] 2.0
            orientierung 503
            idStringDialog R.1
            <\\ElementLK>
            e (2)
            <ElementLK>
            labelAnfangsKnoten[] /vmid
            labelEndKnoten[] /0
            typ 1
            uniqueObjectIdentifier 102
            x 10
            y 40
            parameter[] %s
            orientierung 503
            idStringDialog R.load
            <\\ElementLK>
            verbindungLK (0)
            <Connection>
            label NIX_NIX_NIX
            x[] 10 6 6 10
            y[] 14 14 26 26
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 0
            <\\Connection>
            verbindungLK (1)
            <Connection>
            label NIX_NIX_NIX
            x[] 10 10
            y[] 30 38
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 0
            <\\Connection>
            verbindungLK (2)
            <Connection>
            label NIX_NIX_NIX
            x[] 10 4 4 10
            y[] 18 18 42 42
            enabledShorted 1
            parentSheetIdentifier 0
            connectorType 0
            <\\Connection>
            """.formatted(loadResistance);
        Files.writeString(file, content);
        return file;
    }

    @Test
    void runOptions_mapOntoTheSimulationConfig() throws Exception {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("matrix_solver", "sparse");
        args.put("semiconductor_model", "shockley_nr");
        args.put("data_logging_interval", 2);
        args.put("adaptive", Map.of("enabled", true, "relative_tolerance", 1e-4));

        SimulationConfig.Builder builder = SimulationConfig.builder()
                .circuitFile("dummy.ipes")
                .solverType(SolverType.SOLVER_TRZ);
        RunOptions.applyOptions(builder, args);
        SimulationConfig config = builder
                .stepWidth(DT)
                .simulationDuration(0.001)
                .build();

        assertEquals(SolverType.SOLVER_TRZ, config.getSolverSettings().getSolverType());
        assertEquals(MatrixSolverKind.SPARSE, config.getMatrixSolverKind());
        assertEquals(SemiconductorModelKind.SHOCKLEY_NEWTON_RAPHSON, config.getSemiconductorModel());
        assertEquals(2, config.getDataLoggingInterval());
        assertTrue(config.isAdaptiveStepSize());
        assertEquals(1e-4, config.getRelativeTolerance(), 1e-15);
    }

    @Test
    void runOptions_parameterOverrides_reachTheResult() throws Exception {
        // the load resistor is authored with 10 ohm and overridden to 22 ohm
        Path circuit = writeDividerCircuit(10.0);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("duration", 0.002);
        args.put("dt", DT);
        args.put("signals", List.of("vmid"));
        args.put("parameter_overrides", Map.of("R.load.resistance", 22.0));

        RunOptions.RunResult run = RunOptions.run(circuit, args);
        assertTrue(run.metadata().containsKey("parameterOverrides"),
                "metadata must report the overrides: " + run.metadata());
        // 24 V * 22/(2+22) = 22 V across the overridden load
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(circuit, args);
        List<Double> vmid = csv.columns().get("vmid");
        assertEquals(22.0, vmid.get(vmid.size() - 1), 0.05);
    }

    @Test
    void runOptions_invalidValues_areRejected() throws Exception {
        Path circuit = writeDividerCircuit(10.0);

        Map<String, Object> badSolver = Map.of("solver", "sor");
        assertThrows(IllegalArgumentException.class, () -> RunOptions.buildConfig(circuit, badSolver));

        Map<String, Object> badMatrix = Map.of("matrix_solver", "quantum");
        SimulationConfig.Builder builder = SimulationConfig.builder().circuitFile("x.ipes");
        assertThrows(IllegalArgumentException.class, () -> RunOptions.applyOptions(builder, badMatrix));

        Map<String, Object> badOverrides = Map.of("parameter_overrides", Map.of("R.1.resistance", "high"));
        SimulationConfig.Builder builder2 = SimulationConfig.builder().circuitFile("x.ipes");
        assertThrows(IllegalArgumentException.class, () -> RunOptions.applyOptions(builder2, badOverrides));

        Map<String, Object> twoNetworks = Map.of("magnetic", Map.of("networks",
                List.of(Map.of(), Map.of())));
        SimulationConfig.Builder builder3 = SimulationConfig.builder().circuitFile("x.ipes");
        assertThrows(IllegalArgumentException.class, () -> RunOptions.applyOptions(builder3, twoNetworks));
    }

    @Test
    void runOptions_thermalAndMagneticConfig_buildTheDomains() throws Exception {
        Map<String, Object> thermal = Map.of("thermal", Map.of(
                "ambient_temperature", 40.0,
                "couplings", List.of(Map.of(
                        "device", "R.1", "kind", "resistor",
                        "model", Map.of("kind", "foster", "r_th", List.of(1.5, 0.8),
                                "tau", List.of(0.002, 0.05)),
                        "temperature_coefficient", 0.0039))));
        SimulationConfig.Builder builder = SimulationConfig.builder().circuitFile("x.ipes");
        RunOptions.applyOptions(builder, thermal);
        SimulationConfig config = builder.build();
        assertTrue(config.isThermalDomainEnabled());
        assertEquals(40.0, config.getAmbientTemperature(), 1e-12);
        assertEquals(1, config.getThermalCouplings().size());

        Map<String, Object> magnetic = Map.of("magnetic", Map.of("networks", List.of(Map.of(
                "windings", List.of(Map.of("name", "L1", "turns", 50, "node_a", 1, "node_b", 0)),
                "branches", List.of(Map.of(
                        "name", "core", "node_a", 1, "node_b", 0,
                        "nonlinear", Map.of("curve", "tanh", "permeance", 1e-3,
                                "saturation_flux", 1.5e-2)))))));
        SimulationConfig.Builder builder2 = SimulationConfig.builder().circuitFile("x.ipes");
        RunOptions.applyOptions(builder2, magnetic);
        SimulationConfig config2 = builder2.build();
        assertTrue(config2.isMagneticDomainEnabled());
    }

    @Test
    void listSignals_enumeratesProbesLabelsAndLosses() throws Exception {
        Path circuit = writeDividerCircuit(10.0);
        List<Map<String, Object>> signals = RunOptions.listSignals(circuit, Map.of());

        List<String> names = signals.stream().map(s -> String.valueOf(s.get("name"))).toList();
        assertTrue(names.contains("vmid"), "net label channel missing: " + names);
        assertTrue(names.contains("P_loss_total"), "total loss channel missing: " + names);
        assertTrue(names.stream().noneMatch(n -> n.startsWith("P_loss_R.")),
                "a purely resistive circuit has no semiconductor loss devices");
        assertTrue(names.stream().noneMatch(n -> n.startsWith("Tj_")),
                "no thermal channels without configuration");

        Map<String, Object> withThermal = Map.of("thermal", Map.of(
                "couplings", List.of(Map.of("device", "R.1", "kind", "resistor",
                        "model", Map.of("kind", "cauer", "r_th", List.of(2.0), "c_th", List.of(0.005))))));
        List<String> withThermalNames = RunOptions.listSignals(circuit, withThermal)
                .stream().map(s -> String.valueOf(s.get("name"))).toList();
        assertTrue(withThermalNames.contains("Tj_R.1"), "thermal channel missing: " + withThermalNames);
    }

    @Test
    void waveformAnalysis_genericStatistics_areAdditive() throws Exception {
        Path circuit = writeDividerCircuit(22.0);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("duration", 0.002);
        args.put("dt", DT);
        args.put("signals", List.of("vmid"));
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(circuit, args);

        Map<String, Object> analysis = WaveformAnalysis.analyse(
                csv, "", List.of("vmid"), 2000);
        assertTrue(analysis.containsKey("metrics"), "legacy metrics block must remain");
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) analysis.get("signal_statistics");
        @SuppressWarnings("unchecked")
        Map<String, Object> vmidStats = (Map<String, Object>) stats.get("vmid");
        assertEquals(22.0, ((Number) vmidStats.get("mean")).doubleValue(), 0.05,
                "steady-state mean of the 22 V divider");
        assertTrue(((Number) vmidStats.get("peak_to_peak")).doubleValue() < 1.0,
                "settled DC signal has a small window ripple");
        assertFalse(vmidStats.containsKey("time"));
    }
}
