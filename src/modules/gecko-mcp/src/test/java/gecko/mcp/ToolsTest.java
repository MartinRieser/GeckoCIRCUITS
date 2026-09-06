package gecko.mcp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tool-level behavior tests on the bundled .ipes fixtures.
 */
class ToolsTest {

    private static Path fixture(String name) throws IOException {
        try (InputStream in = ToolsTest.class.getResourceAsStream("/fixtures/" + name)) {
            Path file = Files.createTempFile("gecko-mcp-test-", "-" + name);
            Files.write(file, in.readAllBytes());
            file.toFile().deleteOnExit();
            return file;
        }
    }

    @Test
    void inspectParsesScriptCircuit() throws IOException {
        Map<String, Object> result = CircuitInspector.inspect(fixture("interleaved_pfc_50v.ipes"));
        assertEquals(15, result.get("lk_component_count"));
        assertEquals(5, result.get("control_component_count"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> controls = (List<Map<String, Object>>) result.get("control_components");
        assertTrue(controls.stream().anyMatch(c -> "CTRL_MCU".equals(c.get("name"))),
                "CTRL_MCU block should be listed");
        assertTrue(controls.stream().anyMatch(c -> c.containsKey("sourceCode")),
                "script block should expose its source code");
    }

    @Test
    void patchComponentUpdatesParameterAndRereads() throws IOException {
        Path circuit = fixture("rc-lowpass.ipes");
        Map<String, Object> result = CircuitPatcher.patchComponent(
                circuit.toString(), "R.1", Map.of("param0", 25.0), null);
        assertEquals("SUCCESS", result.get("status"));

        Map<String, Object> inspected = CircuitInspector.inspect(circuit);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components =
                (List<Map<String, Object>>) inspected.get("lk_components");
        double resistance = components.stream()
                .filter(c -> "R.1".equals(c.get("name")))
                .findFirst()
                .map(c -> ((List<Double>) c.get("parameters")).get(0))
                .orElseThrow();
        assertEquals(25.0, resistance, 1e-9, "param0 of R.1 should now be 25");
    }

    @Test
    void setScriptCodeReplacesSourceAndSurvivesReparsing() throws IOException {
        Path circuit = fixture("interleaved_pfc_50v.ipes");
        String newCode = "yOUT[0] = xIN[0] * 2.0;";
        CircuitPatcher.setScriptCode(circuit.toString(), "CTRL_MCU", newCode, "", "", null);

        Map<String, Object> inspected = CircuitInspector.inspect(circuit);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> controls =
                (List<Map<String, Object>>) inspected.get("control_components");
        String source = controls.stream()
                .filter(c -> "CTRL_MCU".equals(c.get("name")))
                .findFirst()
                .map(c -> String.valueOf(c.get("sourceCode")))
                .orElseThrow();
        assertTrue(source.contains(newCode), "new source should be stored, got: " + source);
        assertFalse(source.contains("v_ref"), "old source should be gone");
    }

    @Test
    void waveformMetricsMatchPythonGolden() throws IOException {
        Path circuit = fixture("rc-lowpass.ipes");
        SimulationService.ParsedCsv csv = SimulationService.simulateToCsv(circuit, 0.02, 1e-6);
        Map<String, Object> result = WaveformAnalysis.analyse(csv,
                IpesSupport.readIpesText(circuit), null, 2000);

        String golden = new String(ToolsTest.class.getResourceAsStream(
                "/fixtures/rc-lowpass-metrics.json").readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> expected = new tools.jackson.databind.json.JsonMapper()
                .readValue(golden, Map.class);

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(20001, result.get("total_time_steps"));
        assertEquals(2001, result.get("returned_points"));
        assertMetricsEqual(castMap(expected.get("metrics")), castMap(result.get("metrics")));
    }

    @SuppressWarnings("unchecked")
    private static void assertMetricsEqual(Map<String, Object> expected, Map<String, Object> actual) {
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            Object expectedValue = entry.getValue();
            Object actualValue = actual.get(entry.getKey());
            if (expectedValue instanceof Map<?, ?> nested) {
                assertMetricsEqual((Map<String, Object>) nested, castMap(actualValue));
            } else if (expectedValue instanceof Number number) {
                assertEquals(number.doubleValue(), ((Number) actualValue).doubleValue(), 1e-9,
                        "metric " + entry.getKey());
            } else {
                assertEquals(expectedValue, actualValue, "metric " + entry.getKey());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value != null ? (Map<String, Object>) value : Map.of();
    }

    @Test
    void tunePfcEvaluatesRegulationAgainstTarget() throws Exception {
        Path circuit = fixture("interleaved_pfc_50v.ipes");
        GeckoTools.ToolSpec tune = GeckoTools.all().stream()
                .filter(tool -> tool.name().equals("gecko_tune_pfc"))
                .findFirst().orElseThrow();
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("circuit_path", circuit.toString());
        args.put("target_voltage", 50.0);
        args.put("simulation_time", 0.02);
        args.put("dt", 2e-6);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tune.handler().apply(args);
        assertEquals(50.0, result.get("target_voltage_volts"));
        double measured = ((Number) result.get("measured_voltage_volts")).doubleValue();
        assertTrue(measured > 40.0 && measured < 65.0,
                "boost output should be in a plausible DC-bus range at 20 ms: " + measured);
        assertTrue(result.get("voltage_error_volts") instanceof Number);
        @SuppressWarnings("unchecked")
        java.util.List<String> evaluation = (java.util.List<String>) result.get("evaluation");
        assertTrue(evaluation.stream().anyMatch(line -> line.contains("regulation accurate")
                || line.contains("below target") || line.contains("above target")),
                "evaluation should contain a regulation statement: " + evaluation);
    }

    @Test
    void registryHasExactlyThePythonToolNames() {
        List<String> names = GeckoTools.all().stream().map(GeckoTools.ToolSpec::name).toList();
        assertEquals(List.of(
                "gecko_server_status", "gecko_catalog", "gecko_setup_pfc_project",
                "gecko_setup_llc_project", "gecko_inspect_circuit", "gecko_patch_component",
                "gecko_set_script_code", "gecko_simulate", "gecko_get_waveforms",
                "gecko_tune_pfc"), names);
    }

    @Test
    void setupAndSimulateEndToEndOnGeneratedPfc() throws IOException {
        Path output = Files.createTempDirectory("gecko-mcp-pfc").resolve("pfc.ipes");
        Map<String, Object> setup = PfcProject.setup(output.toString(), PfcProject.Params.defaults());
        assertEquals("SUCCESS", setup.get("status"));
        assertTrue(Files.exists(output));

        SimulationService.RunResult run = SimulationService.simulate(output, 0.002, 1e-6, "be");
        assertTrue(run.totalSteps() >= 2000, "script-block PFC should simulate");
    }

    @Test
    void ipesSupportResolveRejectsNullAndBlank() {
        IllegalArgumentException e1 = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> IpesSupport.resolve(null));
        assertTrue(e1.getMessage().contains("circuit_path is required"));

        IllegalArgumentException e2 = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> IpesSupport.resolve("   "));
        assertTrue(e2.getMessage().contains("circuit_path is required"));
    }

    @Test
    void tunePfcWithCustomGains() throws Exception {
        Path circuit = fixture("interleaved_pfc_50v.ipes");
        GeckoTools.ToolSpec tune = GeckoTools.all().stream()
                .filter(tool -> tool.name().equals("gecko_tune_pfc"))
                .findFirst().orElseThrow();
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("circuit_path", circuit.toString());
        args.put("target_voltage", 50.0);
        args.put("simulation_time", 0.005);
        args.put("kp", 0.025);
        args.put("ki", 5.0);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tune.handler().apply(args);
        assertEquals(50.0, result.get("target_voltage_volts"));
        assertTrue(result.containsKey("measured_voltage_volts"));
    }

    @Test
    void setScriptCodeInsertsMissingTags() throws IOException {
        String minimalIpes = "<ElementCONTROL>\n"
                + "typ 61\n"
                + "idStringDialog CTRL_MIN\n"
                + "<sourceCode>\nyOUT[0] = 1;\n<\\sourceCode>\n"
                + "<\\ElementCONTROL>\n";
        Path temp = Files.createTempFile("gecko-test-min-", ".ipes");
        Files.writeString(temp, minimalIpes);
        try {
            CircuitPatcher.setScriptCode(temp.toString(), "CTRL_MIN", "yOUT[0] = 2;",
                    "double v = 10.0;", "init();", null);
            String updated = IpesSupport.readIpesText(temp);
            assertTrue(updated.contains("<staticVariables>\ndouble v = 10.0;\n<\\staticVariables>"));
            assertTrue(updated.contains("<staticCode>\ninit();\n<\\staticCode>"));
            assertTrue(updated.contains("<sourceCode>\nyOUT[0] = 2;\n<\\sourceCode>"));
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
