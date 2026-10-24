package gecko.mcp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBuilderTest {

    @Test
    void buildsAndSimulatesRlcCircuitFromNetlist() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-builder-test");
        Path outputPath = tempDir.resolve("rlc_filter.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.05,
                        "dt", 1e-6,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_IN", "type", "DC_VOLTAGE", "nodes", List.of("n_in", "0"),
                                "parameters", Map.of("voltage", 24.0)),
                        Map.of("name", "R_SERIES", "type", "RESISTOR", "nodes", List.of("n_in", "n_mid"),
                                "parameters", Map.of("resistance", 2.0)),
                        Map.of("name", "L_FILTER", "type", "INDUCTOR", "nodes", List.of("n_mid", "n_out"),
                                "parameters", Map.of("inductance", 1e-3)),
                        Map.of("name", "C_FILTER", "type", "CAPACITOR", "nodes", List.of("n_out", "0"),
                                "parameters", Map.of("capacitance", 100e-6)),
                        Map.of("name", "R_LOAD", "type", "RESISTOR", "nodes", List.of("n_out", "0"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "control", Map.of(
                        "probes", List.of(
                                Map.of("name", "VM_OUT", "type", "VOLTMETER", "target_component", "R_LOAD", "signal_name", "u_out")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate generated circuit with DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "Synthesized circuit should pass DRC: " + valResult.get("diagnostics"));

        // Simulate synthesized circuit
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.05, 1e-6);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 5000, "Simulation should produce time steps: " + csv.rowCount());
        assertTrue(csv.columns().containsKey("u_out"), "u_out probe signal should be recorded");

        // Verify voltage reaches positive DC level
        List<Double> vout = csv.columns().get("u_out");
        double finalV = vout.get(vout.size() - 1);
        assertTrue(finalV > 18.0 && finalV < 22.0, "Vout should settle near resistive divider 24V * 10/12 = 20V, was: " + finalV);
    }

    @Test
    void buildsAndSimulatesScriptControlledChopper() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-script-test");
        Path outputPath = tempDir.resolve("chopper.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.005,
                        "dt", 1e-6,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "DC_VOLTAGE", "nodes", List.of("in", "0"),
                                "parameters", Map.of("voltage", 48.0)),
                        Map.of("name", "SW", "type", "SWITCH", "nodes", List.of("in", "sw_out"),
                                "parameters", Map.of("initial_state", 0.0, "r_on", 0.01, "r_off", 1e6)),
                        Map.of("name", "D_FREEWHEEL", "type", "DIODE", "nodes", List.of("0", "sw_out")),
                        Map.of("name", "R_LOAD", "type", "RESISTOR", "nodes", List.of("sw_out", "0"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "control", Map.of(
                        "probes", List.of(
                                Map.of("name", "VM_LOAD", "type", "VOLTMETER", "target_component", "R_LOAD", "signal_name", "u_load")
                        ),
                        "script_blocks", List.of(
                                Map.of(
                                        "name", "PWM_GEN",
                                        "in_signals", List.of("u_load"),
                                        "out_signals", List.of("s_gate"),
                                        "static_variables", "double phase = 0.0;",
                                        "code", """
                                                double f_sw = 10000.0;
                                                phase += dt * f_sw;
                                                if (phase >= 1.0) phase -= 1.0;
                                                yOUT[0] = (phase < 0.5) ? 1.0 : 0.0;
                                                return yOUT;
                                                """
                                )
                        ),
                        "gates", List.of(
                                Map.of("name", "G_SW", "target_switch", "SW", "in_signal", "s_gate")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "Chopper should pass DRC: " + valResult.get("diagnostics") + " compile_checks: " + valResult.get("compile_checks"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.002, 1e-6);
        assertTrue(csv.rowCount() > 500);
        assertTrue(csv.columns().containsKey("u_load"));
    }

    @Test
    void buildsAndSimulatesCoupledInductors() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-coupled-test");
        Path outputPath = tempDir.resolve("coupled_inductors.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.04,
                        "dt", 1e-5,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_AC", "type", "VOLTAGE_SOURCE_AC", "nodes", List.of("in", "0"),
                                "parameters", Map.of("amplitude", 100.0, "frequency", 50.0)),
                        Map.of("name", "R_PRI", "type", "RESISTOR", "nodes", List.of("in", "n1"),
                                "parameters", Map.of("resistance", 1.0)),
                        Map.of("name", "L1", "type", "INDUCTOR", "nodes", List.of("n1", "0"),
                                "parameters", Map.of("inductance", 10e-3)),
                        Map.of("name", "L2", "type", "INDUCTOR", "nodes", List.of("n2", "0"),
                                "parameters", Map.of("inductance", 10e-3)),
                        Map.of("name", "R_SEC", "type", "RESISTOR", "nodes", List.of("n2", "0"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "coupled_inductors", List.of(
                        Map.of("name", "K1", "inductor1", "L1", "inductor2", "L2", "k", 0.95)
                ),
                "control", Map.of(
                        "probes", List.of(
                                Map.of("name", "VM_SEC", "type", "VOLTMETER", "target_component", "R_SEC", "signal_name", "u_sec")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "Coupled inductor circuit should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.04, 1e-5);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 1000);
        assertTrue(csv.columns().containsKey("u_sec"));

        // Verify mutual coupling induces secondary voltage
        List<Double> uSec = csv.columns().get("u_sec");
        double maxAbs = uSec.stream().mapToDouble(Math::abs).max().orElse(0.0);
        assertTrue(maxAbs > 20.0, "Secondary voltage should show induced AC coupling (> 20V), was: " + maxAbs);
    }

    @Test
    void buildsAndSimulatesElectroThermalCircuit() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-thermal-test");
        Path outputPath = tempDir.resolve("electro_thermal.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.02,
                        "dt", 1e-5,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "DC_VOLTAGE", "nodes", List.of("in", "0"),
                                "parameters", Map.of("voltage", 48.0)),
                        Map.of("name", "R_LOAD", "type", "RESISTOR", "nodes", List.of("in", "n_sw"),
                                "parameters", Map.of("resistance", 5.0)),
                        Map.of("name", "SW", "type", "SWITCH", "nodes", List.of("n_sw", "0"),
                                "parameters", Map.of("initial_state", 1.0, "r_on", 0.2, "r_off", 1e6))
                ),
                "thermal_networks", List.of(
                        Map.of(
                                "name", "TH_SW",
                                "target_switch", "SW",
                                "heat_flow", 20.0,
                                "model_type", "foster",
                                "ambient_temperature", 25.0,
                                "stages", List.of(
                                        Map.of("r", 2.0, "c", 0.005)
                                )
                        )
                ),
                "control", Map.of(
                        "probes", List.of(
                                Map.of("name", "VM_TJ", "type", "VOLTMETER", "nodeA", "Tj_SW", "nodeB", "0", "signal_name", "t_junction")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "Electro-thermal circuit should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.02, 1e-5);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 500);
        assertTrue(csv.columns().containsKey("t_junction"));

        // Verify temperature rises above ambient 25 degC due to switch conduction losses
        List<Double> tj = csv.columns().get("t_junction");
        double finalTj = tj.get(tj.size() - 1);
        assertTrue(finalTj > 25.0, "Junction temperature should rise above 25.0 degC, was: " + finalTj);
    }

    @Test
    void buildsAndSimulatesNonlinearSaturatingInductor() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-nonlin-test");
        Path outputPath = tempDir.resolve("nonlin_inductor.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.01,
                        "dt", 1e-5,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "DC_VOLTAGE", "nodes", List.of("in", "0"),
                                "parameters", Map.of("voltage", 24.0)),
                        Map.of("name", "R_SERIES", "type", "RESISTOR", "nodes", List.of("in", "n_mid"),
                                "parameters", Map.of("resistance", 2.0)),
                        Map.of("name", "L_SAT", "type", "NONLINEAR_INDUCTOR", "nodes", List.of("n_mid", "out"),
                                "parameters", Map.of("inductance", 5e-3),
                                "saturation_currents", List.of(-10.0, -5.0, 0.0, 5.0, 10.0),
                                "saturation_inductances", List.of(1e-3, 2e-3, 5e-3, 2e-3, 1e-3)),
                        Map.of("name", "R_LOAD", "type", "RESISTOR", "nodes", List.of("out", "0"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "control", Map.of(
                        "probes", List.of(
                                Map.of("name", "VM_OUT", "type", "VOLTMETER", "target_component", "R_LOAD", "signal_name", "u_out")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "Nonlinear inductor circuit should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.01, 1e-5);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 500);
        assertTrue(csv.columns().containsKey("u_out"));

        List<Double> vout = csv.columns().get("u_out");
        double finalV = vout.get(vout.size() - 1);
        assertTrue(finalV > 18.0 && finalV < 22.0, "Vout should settle near 20V (24V * 10/12), was: " + finalV);
    }

    @Test
    void buildsAndSimulatesSpwmGeneratorSinglePhase() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-spwm-test");
        Path outputPath = tempDir.resolve("spwm_inverter.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.04,
                        "dt", 1e-6,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "DC_VOLTAGE", "nodes", List.of("dc_p", "0"),
                                "parameters", Map.of("voltage", 100.0)),
                        Map.of("name", "S1", "type", "MOSFET", "nodes", List.of("dc_p", "sw_a"),
                                "parameters", Map.of("r_on", 0.01, "r_off", 1e6)),
                        Map.of("name", "S2", "type", "MOSFET", "nodes", List.of("sw_a", "0"),
                                "parameters", Map.of("r_on", 0.01, "r_off", 1e6)),
                        Map.of("name", "S3", "type", "MOSFET", "nodes", List.of("dc_p", "sw_b"),
                                "parameters", Map.of("r_on", 0.01, "r_off", 1e6)),
                        Map.of("name", "S4", "type", "MOSFET", "nodes", List.of("sw_b", "0"),
                                "parameters", Map.of("r_on", 0.01, "r_off", 1e6)),
                        Map.of("name", "L_F", "type", "INDUCTOR", "nodes", List.of("sw_a", "out_a"),
                                "parameters", Map.of("inductance", 2e-3)),
                        Map.of("name", "C_F", "type", "CAPACITOR", "nodes", List.of("out_a", "sw_b"),
                                "parameters", Map.of("capacitance", 20e-6)),
                        Map.of("name", "R_LOAD", "type", "RESISTOR", "nodes", List.of("out_a", "sw_b"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "control", Map.of(
                        "parameterized_blocks", List.of(
                                Map.of(
                                        "name", "SPWM",
                                        "type", "SPWM_GENERATOR",
                                        "carrier_frequency", 20000.0,
                                        "grid_frequency", 50.0,
                                        "modulation_index", 0.85,
                                        "switches", List.of("S1", "S2", "S3", "S4")
                                )
                        ),
                        "probes", List.of(
                                Map.of("name", "VM_AC", "type", "VOLTMETER", "target_component", "R_LOAD", "signal_name", "v_ac"),
                                Map.of("name", "VM_G1", "type", "VOLTMETER", "target_component", "S1", "signal_name", "v_s1")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "SPWM inverter should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.04, 1e-6);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 10000);
        assertTrue(csv.columns().containsKey("v_ac"));

        // Verify AC swing: Vdc = 100V, ma = 0.85 -> Vpk ~ 85V -> Vpp ~ 170V
        List<Double> vac = csv.columns().get("v_ac");
        double maxV = vac.stream().mapToDouble(v -> v).max().orElse(0.0);
        double minV = vac.stream().mapToDouble(v -> v).min().orElse(0.0);
        double vpp = maxV - minV;
        assertTrue(vpp > 140.0 && vpp < 200.0, "Vpp should be ~170 V, was: " + vpp);
    }

    @Test
    void buildsAndSimulatesSpwmGeneratorThreePhase() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-spwm3ph-test");
        Path outputPath = tempDir.resolve("three_phase_inverter.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.04,
                        "dt", 1e-6,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "DC_VOLTAGE", "nodes", List.of("dc_p", "0"),
                                "parameters", Map.of("voltage", 300.0)),
                        Map.of("name", "S1", "type", "MOSFET", "nodes", List.of("dc_p", "sw_a")),
                        Map.of("name", "S4", "type", "MOSFET", "nodes", List.of("sw_a", "0")),
                        Map.of("name", "S3", "type", "MOSFET", "nodes", List.of("dc_p", "sw_b")),
                        Map.of("name", "S6", "type", "MOSFET", "nodes", List.of("sw_b", "0")),
                        Map.of("name", "S5", "type", "MOSFET", "nodes", List.of("dc_p", "sw_c")),
                        Map.of("name", "S2", "type", "MOSFET", "nodes", List.of("sw_c", "0")),
                        Map.of("name", "RA", "type", "RESISTOR", "nodes", List.of("sw_a", "star_n"),
                                "parameters", Map.of("resistance", 10.0)),
                        Map.of("name", "RB", "type", "RESISTOR", "nodes", List.of("sw_b", "star_n"),
                                "parameters", Map.of("resistance", 10.0)),
                        Map.of("name", "RC", "type", "RESISTOR", "nodes", List.of("sw_c", "star_n"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "control", Map.of(
                        "parameterized_blocks", List.of(
                                Map.of(
                                        "name", "SPWM_3PH",
                                        "type", "SPWM_GENERATOR",
                                        "three_phase", true,
                                        "carrier_frequency", 10000.0,
                                        "grid_frequency", 50.0,
                                        "modulation_index", 0.8,
                                        "switches", List.of("S1", "S4", "S3", "S6", "S5", "S2")
                                )
                        ),
                        "probes", List.of(
                                Map.of("name", "VM_PH_A", "type", "VOLTMETER", "target_component", "RA", "signal_name", "v_phase_a"),
                                Map.of("name", "VM_PH_B", "type", "VOLTMETER", "target_component", "RB", "signal_name", "v_phase_b")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "Three-phase inverter should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.04, 1e-6);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 10000);
        assertTrue(csv.columns().containsKey("v_phase_a"));
        assertTrue(csv.columns().containsKey("v_phase_b"));
    }

    @Test
    void buildsAndSimulatesClosedLoopPiBuck() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-pi-test");
        Path outputPath = tempDir.resolve("pi_buck.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.015,
                        "dt", 1e-6,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_IN", "type", "DC_VOLTAGE", "nodes", List.of("in", "0"),
                                "parameters", Map.of("voltage", 24.0)),
                        Map.of("name", "SW", "type", "SWITCH", "nodes", List.of("in", "sw_node"),
                                "parameters", Map.of("initial_state", 0.0, "r_on", 0.01, "r_off", 1e6)),
                        Map.of("name", "D_FW", "type", "DIODE", "nodes", List.of("0", "sw_node")),
                        Map.of("name", "L1", "type", "INDUCTOR", "nodes", List.of("sw_node", "out"),
                                "parameters", Map.of("inductance", 200e-6)),
                        Map.of("name", "C1", "type", "CAPACITOR", "nodes", List.of("out", "0"),
                                "parameters", Map.of("capacitance", 220e-6)),
                        Map.of("name", "R_LOAD", "type", "RESISTOR", "nodes", List.of("out", "0"),
                                "parameters", Map.of("resistance", 6.0))
                ),
                "control", Map.of(
                        "parameterized_blocks", List.of(
                                Map.of(
                                        "name", "PI_REG",
                                        "type", "PI_CONTROLLER",
                                        "in_signals", List.of("v_err"),
                                        "out_signals", List.of("v_ctrl"),
                                        "kp", 0.02,
                                        "ki", 150.0,
                                        "limit_min", 0.05,
                                        "limit_max", 0.95,
                                        "anti_windup", true
                                ),
                                Map.of(
                                        "name", "PWM_MOD",
                                        "type", "SPWM_GENERATOR",
                                        "in_signals", List.of("v_ctrl"),
                                        "out_signals", List.of("pwm"),
                                        "carrier_frequency", 50000.0,
                                        "switches", List.of("SW")
                                )
                        ),
                        "script_blocks", List.of(
                                Map.of(
                                        "name", "ERR_CALC",
                                        "in_signals", List.of("v_out"),
                                        "out_signals", List.of("v_err"),
                                        "code", "yOUT[0] = 12.0 - xIN[0]; return yOUT;"
                                )
                        ),
                        "probes", List.of(
                                Map.of("name", "VM_OUT", "type", "VOLTMETER", "target_component", "R_LOAD", "signal_name", "v_out")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "PI Buck circuit should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.015, 1e-6);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 5000);
        assertTrue(csv.columns().containsKey("v_out"));

        // Verify output settles near target 12.0 V
        List<Double> vout = csv.columns().get("v_out");
        double finalV = vout.get(vout.size() - 1);
        assertTrue(finalV > 11.0 && finalV < 13.0, "Regulated Vout should settle near 12V, was: " + finalV);
    }

    @Test
    void buildsAndSimulatesDeadTimeGenerator() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-dt-test");
        Path outputPath = tempDir.resolve("deadtime_leg.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.001,
                        "dt", 1e-7,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "DC_VOLTAGE", "nodes", List.of("dc_p", "0"),
                                "parameters", Map.of("voltage", 48.0)),
                        Map.of("name", "S_HI", "type", "MOSFET", "nodes", List.of("dc_p", "sw_mid")),
                        Map.of("name", "S_LO", "type", "MOSFET", "nodes", List.of("sw_mid", "0")),
                        Map.of("name", "R_LOAD", "type", "RESISTOR", "nodes", List.of("sw_mid", "0"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "control", Map.of(
                        "script_blocks", List.of(
                                Map.of(
                                        "name", "PWM_SRC",
                                        "out_signals", List.of("pwm_in"),
                                        "code", "double f = 10000.0; double phase = (t * f) % 1.0; yOUT[0] = (phase < 0.5) ? 1.0 : 0.0; return yOUT;"
                                )
                        ),
                        "parameterized_blocks", List.of(
                                Map.of(
                                        "name", "DT_LEG",
                                        "type", "DEADTIME_GENERATOR",
                                        "in_signals", List.of("pwm_in"),
                                        "out_signals", List.of("g_hi", "g_lo"),
                                        "dead_time", 2.0e-6,
                                        "switches", List.of("S_HI", "S_LO")
                                )
                        ),
                        "probes", List.of(
                                Map.of("name", "VM_MID", "type", "VOLTMETER", "target_component", "R_LOAD", "signal_name", "v_mid")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertTrue(Files.exists(outputPath));

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "Dead-time circuit should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.001, 1e-7);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 5000);
    }

    @Test
    void catalogContainsParameterizedControlBlocks() {
        assertNotNull(ComponentCatalog.get("SPWM_GENERATOR"));
        assertNotNull(ComponentCatalog.get("SPWM"));
        assertNotNull(ComponentCatalog.get("PI_CONTROLLER"));
        assertNotNull(ComponentCatalog.get("DEADTIME"));
        assertNotNull(ComponentCatalog.get("DEADTIME_GENERATOR"));

        ComponentCatalog.ComponentDef dtDef = ComponentCatalog.get("DEADTIME");
        assertEquals(1037, dtDef.typeNumber());
        assertEquals("CONTROL", dtDef.domain());
        assertEquals(1, dtDef.xPinCount());
    }

    @Test
    void synthesizesCircuitWithCatalogComponentsAndNotes() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-catalog-build-test");
        Path outputPath = tempDir.resolve("catalog_demo.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of(
                        "duration", 0.001,
                        "dt", 1e-6,
                        "solver", 0
                ),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "DC_VOLTAGE", "nodes", List.of("in", "0"),
                                "parameters", Map.of("voltage", 15.0)),
                        Map.of("name", "R1", "type", "RESISTOR", "nodes", List.of("in", "out"),
                                "parameters", Map.of("resistance", 1000.0)),
                        Map.of("name", "R2", "type", "RESISTOR", "nodes", List.of("out", "0"),
                                "parameters", Map.of("resistance", 2000.0)),
                        Map.of("name", "GND1", "type", "GND", "nodes", List.of("0", "0"))
                ),
                "control", Map.of(
                        "blocks", List.of(
                                Map.of("name", "MULT", "type", "MUL"),
                                Map.of("name", "DIV1", "type", "DIV"),
                                Map.of("name", "HYST", "type", "HYS", "parameters", Map.of("u_on", 2.5, "u_off", 1.5))
                        ),
                        "probes", List.of(
                                Map.of("name", "VM_OUT", "type", "VOLTMETER", "target_component", "R2", "signal_name", "u_out")
                        )
                ),
                "notes", List.of(
                        Map.of("name", "TITLE_CARD", "text", "# Resistive Divider with Math Blocks\nBuilt via gecko-circuits MCP")
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));
        assertEquals(4, createResult.get("power_components_count"));
        assertEquals(1, createResult.get("special_components_count"));
        assertTrue(Files.exists(outputPath));

        // Read synthesized file content to ensure TEXTBOX and generic control blocks are present
        String ipesText = IpesSupport.readIpesText(outputPath);
        assertTrue(ipesText.contains("typ 2001"), "Special TEXTBOX typ 2001 should be serialized");
        assertTrue(ipesText.contains("# Resistive Divider with Math Blocks"), "Markdown note text should be preserved");
        assertTrue(ipesText.contains("typ 1019"), "MUL typ 1019 should be serialized in control elements");
        assertTrue(ipesText.contains("typ 1020"), "DIV typ 1020 should be serialized in control elements");
        assertTrue(ipesText.contains("typ 1030"), "HYS typ 1030 should be serialized in control elements");

        // Validate DRC
        Map<String, Object> valResult = CircuitValidator.validate(outputPath);
        assertTrue((Boolean) valResult.get("valid"), "Synthesized circuit should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.001, 1e-6);
        assertNotNull(csv);
        assertTrue(csv.rowCount() > 500);
        assertTrue(csv.columns().containsKey("u_out"));
        List<Double> vout = csv.columns().get("u_out");
        double lastV = vout.get(vout.size() - 1);
        assertTrue(Math.abs(lastV - 10.0) < 0.1, "15V * 2k/(1k+2k) = 10V, got: " + lastV);
    }

    @Test
    void buildsCircuitWithExplicitPowerConnections() throws IOException {
        Path tempDir = Files.createTempDirectory("gecko-power-conns-test");
        Path outputPath = tempDir.resolve("explicit_conns.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of("duration", 0.001, "dt", 1e-6),
                "components", List.of(
                        Map.of("name", "V1", "type", "DC_VOLTAGE", "x", 6, "y", 10, "orientation", 503,
                                "nodes", List.of("pos", "0"), "parameters", Map.of("voltage", 24.0)),
                        Map.of("name", "R1", "type", "RESISTOR", "x", 16, "y", 10, "orientation", 503,
                                "nodes", List.of("pos", "0"), "parameters", Map.of("resistance", 12.0))
                ),
                "power_connections", List.of(
                        Map.of("net", "pos", "points", List.of(List.of(6, 8), List.of(16, 8))),
                        Map.of("net", "0", "points", List.of(List.of(6, 12), List.of(16, 12)))
                ),
                "control", Map.of(
                        "probes", List.of(
                                Map.of("name", "VM", "type", "VOLTMETER", "target_component", "R1", "signal_name", "v_r1")
                        )
                )
        );

        Map<String, Object> createResult = CircuitBuilder.create(circuitDef);
        assertEquals("CREATED", createResult.get("status"));

        String ipes = IpesSupport.readIpesText(outputPath);
        assertTrue(ipes.contains("verbindungLK"));
        assertTrue(ipes.contains("x[] 6 16"));
        assertTrue(ipes.contains("y[] 8 8"));
        assertTrue(ipes.contains("y[] 12 12"));

        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.001, 1e-6);
        assertNotNull(csv);
        assertTrue(csv.columns().containsKey("v_r1"));
        List<Double> vr1 = csv.columns().get("v_r1");
        double lastV = vr1.get(vr1.size() - 1);
        assertEquals(24.0, lastV, 1e-3);
    }
}

