package gecko.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchematicSvgRendererTest {

    @Test
    void rendersSchematicToSvgString(@TempDir Path tempDir) throws Exception {
        Path circuitPath = tempDir.resolve("buck.ipes");
        Map<String, Object> circuitDef = Map.of(
                "output_path", circuitPath.toString(),
                "simulation", Map.of("duration", 0.001, "dt", 1e-6),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "VOLTAGE_SOURCE_DC", "nodes", List.of("in", "0"),
                                "parameters", Map.of("voltage", 24.0)),
                        Map.of("name", "S1", "type", "MOSFET", "nodes", List.of("in", "sw_node")),
                        Map.of("name", "D1", "type", "DIODE", "nodes", List.of("0", "sw_node")),
                        Map.of("name", "L1", "type", "INDUCTOR", "nodes", List.of("sw_node", "out"),
                                "parameters", Map.of("inductance", 100e-6)),
                        Map.of("name", "C1", "type", "CAPACITOR", "nodes", List.of("out", "0"),
                                "parameters", Map.of("capacitance", 47e-6)),
                        Map.of("name", "R1", "type", "RESISTOR", "nodes", List.of("out", "0"),
                                "parameters", Map.of("resistance", 10.0))
                ),
                "control", Map.of(
                        "script_blocks", List.of(
                                Map.of("name", "PWM", "out_signals", List.of("gate_sig"),
                                        "code", "yOUT[0] = 1.0; return yOUT;")
                        ),
                        "gates", List.of(
                                Map.of("name", "G_S1", "target_switch", "S1", "in_signal", "gate_sig")
                        ),
                        "probes", List.of(
                                Map.of("name", "VM_OUT", "type", "VOLTMETER", "target_component", "R1", "signal_name", "v_out")
                        )
                )
        );

        CircuitBuilder.create(circuitDef);
        assertTrue(Files.exists(circuitPath));

        // Test Light Theme
        Map<String, Object> lightResult = SchematicSvgRenderer.exportSvg(circuitPath, null, "light", 1.0);
        assertNotNull(lightResult);
        assertEquals("SUCCESS", lightResult.get("status"));
        String lightSvg = (String) lightResult.get("svg");
        assertNotNull(lightSvg);
        assertTrue(lightSvg.contains("<svg"), "Output should be an SVG root");
        assertTrue(lightSvg.contains("theme-light"));
        assertTrue(lightSvg.contains("wire-lk"), "Should contain power wires");
        assertTrue(lightSvg.contains("V_DC"), "Should label DC source");
        assertTrue(lightSvg.contains("L1"), "Should label inductor");
        assertTrue(lightSvg.contains("R1"), "Should label resistor");
        assertTrue(lightSvg.contains("G_S1"), "Should render gate block");
        assertTrue(lightSvg.contains("VM_OUT"), "Should render probe");

        // Test Dark Theme
        Path svgFile = tempDir.resolve("buck_dark.svg");
        Map<String, Object> darkResult = SchematicSvgRenderer.exportSvg(circuitPath, svgFile, "dark", 1.2);
        assertEquals("SUCCESS", darkResult.get("status"));
        assertTrue(Files.exists(svgFile));
        assertTrue(Files.size(svgFile) > 1000);
        String darkSvg = Files.readString(svgFile);
        assertTrue(darkSvg.contains("theme-dark"));
    }

    @Test
    void toolsExportSchematicSvgAndReload(@TempDir Path tempDir) throws Exception {
        Path circuitPath = tempDir.resolve("test_tool.ipes");
        Map<String, Object> circuitDef = Map.of(
                "output_path", circuitPath.toString(),
                "simulation", Map.of("duration", 0.001, "dt", 1e-6),
                "components", List.of(
                        Map.of("name", "V1", "type", "VOLTAGE_SOURCE_DC", "nodes", List.of("p", "0"),
                                "parameters", Map.of("voltage", 12.0)),
                        Map.of("name", "R1", "type", "RESISTOR", "nodes", List.of("p", "0"),
                                "parameters", Map.of("resistance", 10.0))
                )
        );
        CircuitBuilder.create(circuitDef);

        GeckoTools.ToolSpec exportTool = GeckoTools.all().stream()
                .filter(t -> t.name().equals("gecko_export_schematic_svg"))
                .findFirst().orElseThrow();
        assertNotNull(exportTool);

        Path svgOut = tempDir.resolve("exported.svg");
        @SuppressWarnings("unchecked")
        Map<String, Object> exportRes = (Map<String, Object>) exportTool.handler().apply(Map.of(
                "circuit_path", circuitPath.toString(),
                "output_path", svgOut.toString(),
                "theme", "light"
        ));
        assertEquals("SUCCESS", exportRes.get("status"));
        assertTrue(Files.exists(svgOut));

        GeckoTools.ToolSpec reloadTool = GeckoTools.all().stream()
                .filter(t -> t.name().equals("gecko_server_reload"))
                .findFirst().orElseThrow();
        assertNotNull(reloadTool);

        @SuppressWarnings("unchecked")
        Map<String, Object> reloadRes = (Map<String, Object>) reloadTool.handler().apply(Map.of());
        assertEquals("RELOADED", reloadRes.get("status"));
        assertTrue((Long) reloadRes.get("free_memory_mb") > 0);
    }

    @Test
    void places2DTopologyWithOrthogonalWiresAndParallelBranches(@TempDir Path tempDir) throws Exception {
        Path circuitPath = tempDir.resolve("sync_buck.ipes");
        Map<String, Object> circuitDef = Map.of(
                "output_path", circuitPath.toString(),
                "compress", false,
                "simulation", Map.of("duration", 0.001, "dt", 1e-6),
                "components", List.of(
                        Map.of("name", "V_DC", "type", "VOLTAGE_SOURCE_DC", "nodes", List.of("in", "0"),
                                "parameters", Map.of("voltage", 600.0)),
                        Map.of("name", "Q_HI", "type", "IGBT", "nodes", List.of("in", "sw_node")),
                        Map.of("name", "D_HI", "type", "DIODE", "nodes", List.of("sw_node", "in")),
                        Map.of("name", "Q_LO", "type", "IGBT", "nodes", List.of("0", "sw_node")),
                        Map.of("name", "D_LO", "type", "DIODE", "nodes", List.of("0", "sw_node")),
                        Map.of("name", "L1", "type", "INDUCTOR", "nodes", List.of("sw_node", "out"),
                                "parameters", Map.of("inductance", 5e-3)),
                        Map.of("name", "C1", "type", "CAPACITOR", "nodes", List.of("out", "0"),
                                "parameters", Map.of("capacitance", 100e-6)),
                        Map.of("name", "R_LOAD", "type", "RESISTOR", "nodes", List.of("out", "0"),
                                "parameters", Map.of("resistance", 256.0)),
                        Map.of("name", "GND1", "type", "GND", "nodes", List.of("0"))
                ),
                "control", Map.of(
                        "probes", List.of(
                                Map.of("name", "VM_OUT", "type", "VOLTMETER", "target_component", "R_LOAD", "signal_name", "v_out")
                        ),
                        "script_blocks", List.of(
                                Map.of("name", "PI_PWM_CTRL", "in_signals", List.of("v_out"), "out_signals", List.of("g_hi", "g_lo"),
                                        "code", "yOUT[0] = 0.5; yOUT[1] = 0.5; return yOUT;")
                        ),
                        "gates", List.of(
                                Map.of("name", "GATE_HI", "target_switch", "Q_HI", "in_signal", "g_hi"),
                                Map.of("name", "GATE_LO", "target_switch", "Q_LO", "in_signal", "g_lo")
                        )
                )
        );

        CircuitBuilder.create(circuitDef);

        // Verify circuit passes full DRC + compile check
        Map<String, Object> val = CircuitValidator.validate(circuitPath);
        assertEquals(Boolean.TRUE, val.get("valid"), () -> "Sync buck must pass DRC: " + val);

        // Verify CircuitPatcher preserves uncompressed plain-text format
        CircuitPatcher.patchComponent(circuitPath.toString(), "R_LOAD", Map.of("resistance", 320.0), null);
        assertFalse(IpesSupport.isGzipped(circuitPath), "Patching uncompressed .ipes must keep plain-text format");

        // Verify SVG has strictly orthogonal polylines (no diagonal segments)
        Map<String, Object> svgRes = SchematicSvgRenderer.exportSvg(circuitPath, null, "dark", 1.0);
        String svg = (String) svgRes.get("svg");
        assertTrue(svg.contains("POWER STAGE"), "Should render power stage separator");
        assertTrue(svg.contains("CONTROL &amp; MODULATION"), "Should render control stage separator");
        assertTrue(svg.contains("y1=\"-44.0\"") && svg.contains("y2=\"44.0\""),
                "2-port component leads must span the full 2-grid-unit (44px) terminal offset so wires connect without gaps");

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<polyline class=\"wire-[^\"]+\" points=\"([^\"]+)\"")
                .matcher(svg);
        int wireCount = 0;
        while (m.find()) {
            wireCount++;
            String[] coords = m.group(1).trim().split("\\s+");
            for (int i = 0; i < coords.length - 1; i++) {
                String[] a = coords[i].split(",");
                String[] b = coords[i + 1].split(",");
                double x1 = Double.parseDouble(a[0]);
                double y1 = Double.parseDouble(a[1]);
                double x2 = Double.parseDouble(b[0]);
                double y2 = Double.parseDouble(b[1]);
                assertTrue(Math.abs(x1 - x2) < 1e-3 || Math.abs(y1 - y2) < 1e-3,
                        "Every wire segment must be orthogonal, found diagonal: (" + x1 + "," + y1 + ") -> (" + x2 + "," + y2 + ")");
            }
        }
        assertTrue(wireCount >= 6, "Should render power and control wire polylines");
    }
}
