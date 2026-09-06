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
        assertTrue((Boolean) valResult.get("valid"), "Chopper should pass DRC: " + valResult.get("diagnostics"));

        // Simulate
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(outputPath, 0.002, 1e-6);
        assertTrue(csv.rowCount() > 500);
        assertTrue(csv.columns().containsKey("u_load"));
    }
}
