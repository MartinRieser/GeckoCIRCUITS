package gecko.mcp;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * N-terminal component support: the builder accepts components whose catalog
 * definition carries more than two pins (e.g. the 3-phase PMSM) and writes
 * the multi-label .ipes terminal arrays the classic editor uses.
 */
class CircuitBuilderMultiTerminalTest {

    private static Path build(String suffix, Map<String, Object> circuitDef) throws IOException {
        Path output = Files.createTempDirectory("gecko-builder-nterm").resolve(suffix);
        Map<String, Object> def = new java.util.LinkedHashMap<>(circuitDef);
        def.put("output_path", output.toString());
        Map<String, Object> result = CircuitBuilder.create(def);
        assertEquals("CREATED", result.get("status"));
        return output;
    }

    /** 3-phase grid (three phase-shifted AC sources) feeding a PMSM. */
    private static Map<String, Object> pmsmCircuit() {
        return Map.of(
                "simulation", Map.of("duration", 0.01, "dt", 1e-5),
                "components", List.of(
                        Map.of("name", "U_A", "type", "VOLTAGE_SOURCE_AC",
                                "nodes", List.of("gA", "0"),
                                "parameters", Map.of("phase_deg", 0.0)),
                        Map.of("name", "U_B", "type", "VOLTAGE_SOURCE_AC",
                                "nodes", List.of("gB", "0"),
                                "parameters", Map.of("phase_deg", 120.0)),
                        Map.of("name", "U_C", "type", "VOLTAGE_SOURCE_AC",
                                "nodes", List.of("gC", "0"),
                                "parameters", Map.of("phase_deg", -120.0)),
                        Map.of("name", "M1", "type", "PMSM_MOTOR",
                                "nodes", List.of("gA", "gB", "gC"))));
    }

    @Test
    void threePhasePmsmWritesMultiLabelTerminalArrays() throws Exception {
        Path output = build("pmsm.ipes", pmsmCircuit());

        CircuitModel model = new CircuitFileParser().parse(
                new ByteArrayInputStream(Files.readAllBytes(output)), "pmsm.ipes");
        CircuitModel.ComponentData pmsm = model.getAllComponents().stream()
                .filter(c -> "M1".equals(c.getName()))
                .findFirst().orElseThrow();

        assertEquals(15, pmsm.getType());
        assertArrayEquals(new String[]{"gA", "gB", "gC"}, pmsm.getTerminalXLabels(),
                "all three phases must land on the input-side label array");
        assertEquals(0, pmsm.getTerminalYLabels().length,
                "PMSM has no output-side terminals");

        // each phase net is shared between its source and the motor
        for (String phase : List.of("gA", "gB", "gC")) {
            long uses = model.getAllComponents().stream()
                    .filter(c -> List.of(c.getTerminalXLabels()).contains(phase)
                            || List.of(c.getTerminalYLabels()).contains(phase))
                    .count();
            assertEquals(2, uses, "net " + phase + " should connect source and motor");
        }
    }

    @Test
    void generatedPmsmCircuitPassesDrcValidation() throws IOException {
        Path output = build("pmsm.ipes", pmsmCircuit());
        Map<String, Object> report = CircuitValidator.validate(output);
        assertEquals(Boolean.TRUE, report.get("valid"),
                () -> "DRC should pass, got: " + report.get("errors"));
    }

    @Test
    void nodeCountMustMatchCatalogPins() {
        Map<String, Object> bad = new java.util.LinkedHashMap<>(pmsmCircuit());
        bad.put("components", List.of(
                Map.of("name", "M1", "type", "PMSM_MOTOR", "nodes", List.of("gA", "gB"))));
        bad.put("output_path", "unused.ipes");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CircuitBuilder.create(bad));
        assertTrue(error.getMessage().contains("3 nodes"), "message should state the pin count: "
                + error.getMessage());
    }

    @Test
    void parametersRawOverridesPresetSlots() throws IOException {
        Map<String, Object> def = new java.util.LinkedHashMap<>(pmsmCircuit());
        def.put("components", List.of(
                Map.of("name", "M1", "type", "PMSM_MOTOR",
                        "nodes", List.of("gA", "gB", "gC"),
                        "parameters_raw", List.of(1.5, 2.5, 3.5))));
        Path output = build("pmsm_raw.ipes", def);

        String content = Files.readString(output, StandardCharsets.UTF_8);
        int pmsmBlock = content.indexOf("idStringDialog M1");
        String parameters = content.substring(0, pmsmBlock)
                .substring(content.substring(0, pmsmBlock).lastIndexOf("parameter[]"));
        assertTrue(parameters.contains("1.5") && parameters.contains("2.5") && parameters.contains("3.5"),
                "raw values must override the preset: " + parameters.trim());
    }

    @Test
    void twoTerminalComponentsStillWriteSingleLabels() throws IOException {
        Path output = build("rlc.ipes", Map.of(
                "components", List.of(
                        Map.of("name", "R1", "type", "RESISTOR",
                                "nodes", List.of("a", "b"),
                                "parameters", Map.of("resistance", 10.0)))));
        String content = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(content.contains("labelAnfangsKnoten[] /a\n"));
        assertTrue(content.contains("labelEndKnoten[] /b\n"));
    }
}
