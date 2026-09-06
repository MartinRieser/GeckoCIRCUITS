package gecko.mcp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CircuitValidatorTest {

    private static Path fixture(String name) throws IOException {
        try (InputStream in = CircuitValidatorTest.class.getResourceAsStream("/fixtures/" + name)) {
            Path file = Files.createTempFile("gecko-val-test-", "-" + name);
            Files.write(file, in.readAllBytes());
            file.toFile().deleteOnExit();
            return file;
        }
    }

    @Test
    void validatesHealthyFixtureWithoutErrors() throws IOException {
        Path circuit = fixture("rc-lowpass.ipes");
        Map<String, Object> result = CircuitValidator.validate(circuit);
        assertTrue((Boolean) result.get("valid"), "Healthy fixture should be valid: " + result.get("diagnostics"));
        assertEquals(0L, result.get("error_count"));
    }

    @Test
    void detectsMissingGroundReference() {
        String content = """
                ElementLKAnzahl 2
                
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /node1
                labelEndKnoten[] /node2
                typ 1
                uniqueObjectIdentifier 1001
                parameter[] 10.0
                <\\ElementLK>
                
                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /node2
                labelEndKnoten[] /node1
                typ 4
                uniqueObjectIdentifier 1002
                parameter[] 1 10.0 50.0 0.0 0.0
                <\\ElementLK>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_no_ground");
        assertFalse((Boolean) result.get("valid"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertTrue(diags.stream().anyMatch(d -> "MISSING_GROUND".equals(d.get("rule"))),
                "Missing ground diagnostic should be triggered");
    }

    @Test
    void detectsShortCircuitedComponent() {
        String content = """
                ElementLKAnzahl 1
                
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /0
                labelEndKnoten[] /0
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R_SHORT
                parameter[] 10.0
                <\\ElementLK>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_short");
        assertFalse((Boolean) result.get("valid"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertTrue(diags.stream().anyMatch(d -> "SHORT_CIRCUIT".equals(d.get("rule"))),
                "Short circuit diagnostic should be triggered");
    }

    @Test
    void detectsDanglingProbeAndGate() {
        String content = """
                ElementLKAnzahl 1
                
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /in
                labelEndKnoten[] /0
                typ 1
                uniqueObjectIdentifier 1001
                parameter[] 10.0
                <\\ElementLK>
                
                ElementCONTROLAnzahl 2
                
                c (0)
                <ElementCONTROL>
                typ 1
                uniqueObjectIdentifier 2001
                idStringDialog VOLT_DANGLING
                coupledReferenceID[] 9999
                <\\ElementCONTROL>
                
                c (1)
                <ElementCONTROL>
                typ 6
                uniqueObjectIdentifier 2002
                idStringDialog GATE_DANGLING
                coupledReferenceID[] 8888
                <\\ElementCONTROL>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_dangling");
        assertFalse((Boolean) result.get("valid"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertTrue(diags.stream().anyMatch(d -> "DANGLING_PROBE".equals(d.get("rule"))));
        assertTrue(diags.stream().anyMatch(d -> "DANGLING_GATE".equals(d.get("rule"))));
    }

    @Test
    void detectsScriptBlockUnbalancedBraces() {
        String content = """
                ElementLKAnzahl 1
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /in
                labelEndKnoten[] /0
                typ 1
                uniqueObjectIdentifier 1001
                parameter[] 10.0
                <\\ElementLK>
                
                ElementCONTROLAnzahl 1
                c (0)
                <ElementCONTROL>
                typ 61
                uniqueObjectIdentifier 2001
                idStringDialog SCRIPT_ERR
                <sourceCode>
                if (xIN[0] > 0) {
                    yOUT[0] = 1.0;
                // Missing closing brace
                return yOUT;
                <\\sourceCode>
                <\\ElementCONTROL>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_script_brace");
        assertFalse((Boolean) result.get("valid"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertTrue(diags.stream().anyMatch(d -> "UNBALANCED_BRACES".equals(d.get("rule"))));
    }
}
