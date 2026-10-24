package gecko.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration and error-handling tests for all 16 MCP tools registered in GeckoTools.
 */
class AllGeckoToolsTest {

    private static Path fixture(String name) throws IOException {
        try (InputStream in = AllGeckoToolsTest.class.getResourceAsStream("/fixtures/" + name)) {
            assertNotNull(in, "Fixture resource not found: " + name);
            Path file = Files.createTempFile("gecko-tool-test-", "-" + name);
            Files.write(file, in.readAllBytes());
            file.toFile().deleteOnExit();
            return file;
        }
    }

    private static GeckoTools.ToolSpec getTool(String name) {
        return GeckoTools.all().stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing tool in GeckoTools.all(): " + name));
    }

    @Test
    void testServerStatus() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_server_status");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of());
        assertEquals("READY", result.get("status"));
        assertNotNull(result.get("platform"));
        assertNotNull(result.get("java_version"));
        assertNotNull(result.get("workspace_root"));
        assertTrue((Integer) result.get("catalog_component_count") > 60);
        @SuppressWarnings("unchecked")
        List<String> caps = (List<String>) result.get("capabilities");
        assertTrue(caps.contains("headless_simulation"));
        assertTrue(caps.contains("electro_thermal_feedback"));
        assertTrue(caps.contains("magnetic_reluctance_networks"));
    }

    @Test
    void testCatalogToolQueries() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_catalog");

        // 1. Full catalog
        @SuppressWarnings("unchecked")
        Map<String, Object> full = (Map<String, Object>) tool.handler().apply(Map.of());
        assertTrue((Integer) full.get("total_component_count") > 60);
        assertTrue(full.containsKey("power_components"));
        assertTrue(full.containsKey("control_components"));
        assertTrue(full.containsKey("special_components"));
        assertTrue(full.containsKey("aliases"));

        // 2. Domain filtered
        @SuppressWarnings("unchecked")
        Map<String, Object> powerOnly = (Map<String, Object>) tool.handler().apply(Map.of("domain", "power"));
        assertEquals("power", powerOnly.get("filtered_domain"));
        assertTrue((Integer) powerOnly.get("count") >= 25);

        @SuppressWarnings("unchecked")
        Map<String, Object> controlOnly = (Map<String, Object>) tool.handler().apply(Map.of("domain", "control"));
        assertEquals("control", controlOnly.get("filtered_domain"));
        assertTrue((Integer) controlOnly.get("count") >= 40);

        // 3. Single component lookup
        @SuppressWarnings("unchecked")
        Map<String, Object> opamp = (Map<String, Object>) tool.handler().apply(Map.of("component", "OPAMP"));
        assertEquals(Boolean.TRUE, opamp.get("found"));
        @SuppressWarnings("unchecked")
        Map<String, Object> comp = (Map<String, Object>) opamp.get("component");
        assertEquals("OPAMP", comp.get("type"));
        assertEquals(22, comp.get("type_number"));
        assertNotNull(opamp.get("usage_example"));

        // 4. Alias lookup
        @SuppressWarnings("unchecked")
        Map<String, Object> gnd = (Map<String, Object>) tool.handler().apply(Map.of("component", "GND"));
        assertEquals(Boolean.TRUE, gnd.get("found"));
        @SuppressWarnings("unchecked")
        Map<String, Object> gndComp = (Map<String, Object>) gnd.get("component");
        assertEquals("GLOBAL_TERMINAL", gndComp.get("type"));

        // 5. Unknown query
        @SuppressWarnings("unchecked")
        Map<String, Object> unknown = (Map<String, Object>) tool.handler().apply(Map.of("component", "NOT_A_COMPONENT"));
        assertEquals(Boolean.FALSE, unknown.get("found"));
        assertNotNull(unknown.get("suggestions"));
    }

    @Test
    void testCreateCircuitTool(@TempDir Path tempDir) throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_create_circuit");
        Path outputPath = tempDir.resolve("synth_test.ipes");

        Map<String, Object> circuitDef = Map.of(
                "output_path", outputPath.toString(),
                "simulation", Map.of("duration", 0.001, "dt", 1e-6),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "DC_VOLTAGE", "nodes", List.of("in", "0"),
                                "parameters", Map.of("voltage", 12.0)),
                        Map.of("name", "R_L", "type", "RESISTOR", "nodes", List.of("in", "0"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "notes", List.of(
                        Map.of("name", "NOTE_1", "text", "Synthesized via MCP tool")
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(circuitDef);
        assertEquals("CREATED", result.get("status"));
        assertEquals(2, result.get("power_components_count"));
        assertEquals(1, result.get("special_components_count"));
        assertTrue(Files.exists(outputPath));

        // Error handling for empty components
        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("output_path", outputPath.toString(), "components", List.of())));
    }

    @Test
    void testValidateCircuitTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_validate_circuit");
        Path circuit = fixture("rc-lowpass.ipes");

        // 1. File path validation (compile level)
        @SuppressWarnings("unchecked")
        Map<String, Object> fileVal = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "level", "compile"
        ));
        assertTrue((Boolean) fileVal.get("valid"));
        assertEquals(0L, fileVal.get("error_count"));

        // 2. Inline content validation (lint level)
        String rawContent = IpesSupport.readIpesText(circuit);
        @SuppressWarnings("unchecked")
        Map<String, Object> inlineVal = (Map<String, Object>) tool.handler().apply(Map.of(
                "content", rawContent,
                "level", "lint"
        ));
        assertTrue((Boolean) inlineVal.get("valid"));

        // 3. Error on missing path and content
        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("level", "compile")));

        // 4. Error on non-existent file
        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("circuit_path", "non_existent_file.ipes")));
    }

    @Test
    void testMeasureMetricsTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_measure_metrics");
        Path circuit = fixture("rc-lowpass.ipes");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "duration", 0.005,
                "dt", 1e-6,
                "v_out_signal", "uOUT"
        ));
        assertNotNull(result);
        assertTrue(result.containsKey("sample_count"));
        assertTrue(result.containsKey("signals"));
        assertTrue(result.containsKey("system_metrics"));

        // Error on nonexistent file
        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("circuit_path", "ghost.ipes")));
    }

    @Test
    void testSetupPfcProjectTool(@TempDir Path tempDir) throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_setup_pfc_project");
        Path out = tempDir.resolve("pfc_gen.ipes");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "output_path", out.toString(),
                "target_voltage", 48.0,
                "duration", 0.01
        ));
        assertEquals("SUCCESS", result.get("status"));
        assertTrue(Files.exists(out));
        assertTrue(Files.size(out) > 0);
    }

    @Test
    void testSetupLlcProjectTool(@TempDir Path tempDir) throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_setup_llc_project");
        Path out = tempDir.resolve("llc_gen.ipes");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "output_path", out.toString(),
                "v_in", 400.0,
                "v_out", 48.0,
                "p_out", 3000.0,
                "f_sw", 100000.0
        ));
        assertEquals("SUCCESS", result.get("status"));
        assertTrue(Files.exists(out));
        assertTrue(((Number) result.get("resonant_frequency_hz")).doubleValue() > 0);
        assertTrue(((Number) result.get("characteristic_impedance_ohms")).doubleValue() > 0);
        assertTrue(((Number) result.get("quality_factor_q")).doubleValue() > 0);
    }

    @Test
    void testInspectCircuitTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_inspect_circuit");
        Path circuit = fixture("interleaved_pfc_50v.ipes");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString()
        ));
        assertNotNull(result);
        assertTrue((Integer) result.get("lk_component_count") > 0);
        assertTrue((Integer) result.get("control_component_count") > 0);
        assertTrue(result.containsKey("net_labels"));

        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("circuit_path", "non_existent.ipes")));
    }

    @Test
    void testPatchComponentTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_patch_component");
        Path circuit = fixture("rc-lowpass.ipes");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "component_name", "R.1",
                "parameters", Map.of("param0", 33.0)
        ));
        assertEquals("SUCCESS", result.get("status"));

        // Verify value was actually written
        GeckoTools.ToolSpec inspect = getTool("gecko_inspect_circuit");
        @SuppressWarnings("unchecked")
        Map<String, Object> inspected = (Map<String, Object>) inspect.handler().apply(Map.of("circuit_path", circuit.toString()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lks = (List<Map<String, Object>>) inspected.get("lk_components");
        double rVal = lks.stream()
                .filter(c -> "R.1".equals(c.get("name")))
                .findFirst()
                .map(c -> ((Number) ((List<?>) c.get("parameters")).get(0)).doubleValue())
                .orElseThrow();
        assertEquals(33.0, rVal, 1e-9);
    }

    @Test
    void testSetScriptCodeTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_set_script_code");
        Path circuit = fixture("interleaved_pfc_50v.ipes");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "block_name", "CTRL_MCU",
                "source_code", "yOUT[0] = 42.0;",
                "static_variables", "double test_v = 1.0;",
                "static_code", "init();"
        ));
        assertEquals("SUCCESS", result.get("status"));

        String updatedText = IpesSupport.readIpesText(circuit);
        assertTrue(updatedText.contains("yOUT[0] = 42.0;"));
        assertTrue(updatedText.contains("double test_v = 1.0;"));
        assertTrue(updatedText.contains("init();"));
    }

    @Test
    void testSimulateTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_simulate");
        Path circuit = fixture("rc-lowpass.ipes");

        // 1. Basic simulation
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "duration", 0.005,
                "dt", 1e-6,
                "solver", "be"
        ));
        assertEquals("COMPLETED", result.get("status"));
        assertTrue(((Number) result.get("total_steps")).longValue() > 1000);
        assertNotNull(result.get("signal_names"));
        assertNotNull(result.get("metadata"));

        // 2. Simulation with parameter overrides
        @SuppressWarnings("unchecked")
        Map<String, Object> sweepResult = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "duration", 0.002,
                "dt", 1e-6,
                "parameter_overrides", Map.of("R.1.resistance", 100.0)
        ));
        assertEquals("COMPLETED", sweepResult.get("status"));

        // 3. Error on nonexistent file
        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("circuit_path", "missing.ipes")));
    }

    @Test
    void testGetWaveformsTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_get_waveforms");
        Path circuit = fixture("rc-lowpass.ipes");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "duration", 0.005,
                "dt", 1e-6,
                "max_points", 100
        ));
        assertEquals("SUCCESS", result.get("status"));
        assertTrue((Integer) result.get("total_time_steps") > 1000);
        assertTrue((Integer) result.get("returned_points") <= 101);
        assertNotNull(result.get("waveforms"));
        assertNotNull(result.get("metrics"));

        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("circuit_path", "no_such_circuit.ipes")));
    }

    @Test
    void testTunePfcTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_tune_pfc");
        Path circuit = fixture("interleaved_pfc_50v.ipes");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "target_voltage", 50.0,
                "simulation_time", 0.005,
                "dt", 2e-6
        ));
        assertEquals(50.0, result.get("target_voltage_volts"));
        assertTrue(result.containsKey("measured_voltage_volts"));
        assertTrue(result.containsKey("voltage_error_volts"));
        assertNotNull(result.get("evaluation"));
    }

    @Test
    void testListSignalsTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_list_signals");
        Path circuit = fixture("rc-lowpass.ipes");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> signals = (List<Map<String, Object>>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString()
        ));
        assertNotNull(signals);
        assertFalse(signals.isEmpty());
        assertTrue(signals.stream().anyMatch(s -> "u_out".equals(s.get("name"))), "Should contain probe signal u_out: " + signals);

        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("circuit_path", "does_not_exist.ipes")));
    }

    @Test
    void testExportSchematicSvgTool(@TempDir Path tempDir) throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_export_schematic_svg");
        Path circuit = fixture("rc-lowpass.ipes");
        Path outSvg = tempDir.resolve("schematic.svg");

        // 1. Export with file output and dark theme
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "output_path", outSvg.toString(),
                "theme", "dark",
                "scale", 1.5
        ));
        assertEquals("SUCCESS", result.get("status"));
        assertTrue(Files.exists(outSvg));
        String fileSvg = Files.readString(outSvg);
        assertTrue(fileSvg.contains("<svg"));
        assertTrue(fileSvg.contains("theme-dark"));

        // 2. Export with in-memory string output (light theme)
        @SuppressWarnings("unchecked")
        Map<String, Object> lightResult = (Map<String, Object>) tool.handler().apply(Map.of(
                "circuit_path", circuit.toString(),
                "theme", "light"
        ));
        assertEquals("SUCCESS", lightResult.get("status"));
        String svg = (String) lightResult.get("svg");
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("theme-light"));

        // 3. Error on nonexistent file
        assertThrows(IllegalArgumentException.class, () ->
                tool.handler().apply(Map.of("circuit_path", "nonexistent.ipes")));
    }

    @Test
    void testServerReloadTool() throws Exception {
        GeckoTools.ToolSpec tool = getTool("gecko_server_reload");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.handler().apply(Map.of());
        assertEquals("RELOADED", result.get("status"));
        assertTrue((Long) result.get("timestamp") > 0);
        assertTrue((Integer) result.get("catalog_component_count") > 60);
        assertTrue(((Number) result.get("free_memory_mb")).longValue() > 0);
        assertTrue(((Number) result.get("total_memory_mb")).longValue() > 0);
    }
}
