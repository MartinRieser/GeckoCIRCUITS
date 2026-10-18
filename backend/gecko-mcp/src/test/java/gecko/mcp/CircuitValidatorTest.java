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

    @Test
    void recognizesThermalComponentsAndAllowsCoupledProbes() {
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
                
                ElementTHERMAnzahl 1
                c (0)
                <ElementTHERM>
                labelAnfangsKnoten[] /Tj
                labelEndKnoten[] /Tamb
                typ 46
                uniqueObjectIdentifier 3001
                idStringDialog R_TH_JC
                parameter[] 0.5 0.0 0.0
                <\\ElementTHERM>

                ElementCONTROLAnzahl 1
                c (0)
                <ElementCONTROL>
                typ 1
                uniqueObjectIdentifier 2001
                idStringDialog VM_TH
                coupledReferenceID[] 3001
                <\\ElementCONTROL>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_thermal");
        assertTrue((Boolean) result.get("valid"), "Thermal component and coupled probe should be valid");
        assertEquals(1, result.get("thermal_components"));
    }

    @Test
    void recognizesWireBasedGroundReference() {
        String content = """
                ElementLKAnzahl 1
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R1
                parameter[] 10.0
                <\\ElementLK>
                
                verbindungLK (0)
                <Verbindung>
                label 0
                zeigerAktuell 2
                x[] 5 5
                y[] 5 6
                <\\Verbindung>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_wire_ground");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertFalse(diags.stream().anyMatch(d -> "MISSING_GROUND".equals(d.get("rule"))),
                "Wire labeled '0' should satisfy ground requirement");
    }

    @Test
    void emptyTerminalNodeDoesNotSwallowNextLineAttribute() {
        String content = """
                ElementLKAnzahl 1
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /0
                labelEndKnoten[] 
                enabledShorted 1
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R1
                parameter[] 10.0
                <\\ElementLK>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_empty_node");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertFalse(diags.stream().anyMatch(d -> d.get("message").contains("enabledShorted")),
                "Empty labelEndKnoten should not swallow 'enabledShorted 1' as node name");
    }

    @Test
    void detectsWireBasedShortCircuit() {
        String content = """
                ElementLKAnzahl 1
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R_LOOP
                x 6
                y 6
                orientierung 503
                parameter[] 10.0
                <\\ElementLK>
                
                verbindungLK (0)
                <Verbindung>
                label net_loop
                x[] 6 6
                y[] 4 8
                connectorType 0
                <\\Verbindung>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_wire_short");
        assertFalse((Boolean) result.get("valid"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertTrue(diags.stream().anyMatch(d -> "SHORT_CIRCUIT".equals(d.get("rule"))
                && d.get("component").equals("R_LOOP")),
                "DRC should detect component shorted across wire loop");
    }

    @Test
    void mapsWireLabelsToComponentTerminals() {
        String content = """
                ElementLKAnzahl 2
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R1
                x 6
                y 6
                orientierung 503
                parameter[] 10.0
                <\\ElementLK>
                
                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 1
                uniqueObjectIdentifier 1002
                idStringDialog R2
                x 10
                y 6
                orientierung 503
                parameter[] 20.0
                <\\ElementLK>

                verbindungLK (0)
                <Verbindung>
                label v_in
                x[] 6 10
                y[] 4 4
                connectorType 0
                <\\Verbindung>

                verbindungLK (1)
                <Verbindung>
                label 0
                x[] 6 10
                y[] 8 8
                connectorType 0
                <\\Verbindung>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_wire_mapping");
        assertTrue((Boolean) result.get("valid"), "Both resistors connected by wires to v_in and ground should be valid");
        assertEquals(0L, result.get("error_count"));
        assertEquals(0L, result.get("warning_count"));
    }

    @Test
    void detectsWireBasedFloatingPin() {
        String content = """
                ElementLKAnzahl 1
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R_FLOAT
                x 6
                y 6
                orientierung 503
                parameter[] 10.0
                <\\ElementLK>
                
                verbindungLK (0)
                <Verbindung>
                label 0
                x[] 6 10
                y[] 4 4
                connectorType 0
                <\\Verbindung>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_floating_pin");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertTrue(diags.stream().anyMatch(d -> "FLOATING_NET".equals(d.get("rule"))
                && d.get("component").equals("R_FLOAT")),
                "Floating output pin at (6, 8) should trigger FLOATING_NET warning");
    }

    @Test
    void resolvesMultiHopIntersectingWires() {
        String content = """
                ElementLKAnzahl 2
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 4
                uniqueObjectIdentifier 1001
                idStringDialog V1
                x 2
                y 4
                orientierung 503
                parameter[] 10.0
                <\\ElementLK>

                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 1
                uniqueObjectIdentifier 1002
                idStringDialog R1
                x 2
                y 8
                orientierung 503
                parameter[] 10.0
                <\\ElementLK>

                verbindungLK (0)
                <Verbindung>
                label NIX_NIX_NIX
                x[] 2 5
                y[] 6 6
                connectorType 0
                <\\Verbindung>

                verbindungLK (1)
                <Verbindung>
                label 0
                x[] 5 5
                y[] 6 10
                connectorType 0
                <\\Verbindung>

                verbindungLK (2)
                <Verbindung>
                label v_top
                x[] 2 2
                y[] 2 10
                connectorType 0
                <\\Verbindung>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_multihop_wires");
        assertTrue((Boolean) result.get("valid"), "Multi-hop connected circuit should be valid");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertFalse(diags.stream().anyMatch(d -> "MISSING_GROUND".equals(d.get("rule"))));
    }

    @Test
    void resolvesNameBasedGateCoupling() {
        String content = """
                ElementLKAnzahl 2
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /in
                labelEndKnoten[] /0
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R1
                parameter[] 10.0
                <\\ElementLK>
                
                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /in
                labelEndKnoten[] /0
                typ 7
                uniqueObjectIdentifier 1002
                idStringDialog S_MAIN
                parameter[] 1.0E7 0.001 0.0 0.0
                <\\ElementLK>

                ElementCONTROLAnzahl 1
                c (0)
                <ElementCONTROL>
                typ 6
                uniqueObjectIdentifier 2001
                idStringDialog GATE_DRV
                coupledReferenceID[] 0
                parameterString[] /S_MAIN/0/0
                <\\ElementCONTROL>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_name_gate");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertFalse(diags.stream().anyMatch(d -> "DANGLING_GATE".equals(d.get("rule"))),
                "Gate referencing switch by name in parameterString should resolve without DANGLING_GATE");
    }

    @Test
    void resolvesProbeWithParameterStringNodeLabels() {
        String content = """
                ElementLKAnzahl 1
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /u_load
                labelEndKnoten[] /0
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R_LOAD
                parameter[] 50.0
                <\\ElementLK>

                ElementCONTROLAnzahl 1
                c (0)
                <ElementCONTROL>
                typ 1
                uniqueObjectIdentifier 2001
                idStringDialog VM_OUT
                coupledReferenceID[] 0
                parameterString[] /in1/u_load/0
                <\\ElementCONTROL>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_probe_nodes");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertFalse(diags.stream().anyMatch(d -> "DANGLING_PROBE".equals(d.get("rule"))),
                "Voltmeter referencing /in1/u_load/0 in parameterString should resolve without DANGLING_PROBE");
    }

    @Test
    void doesNotShortCircuitOpAmpWithNegativeFeedback() {
        String content = """
                ElementLKAnzahl 2
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /0
                labelEndKnoten[] /v_in
                typ 4
                uniqueObjectIdentifier 1001
                idStringDialog V_IN
                parameter[] 5.0
                <\\ElementLK>

                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX/v_out
                labelEndKnoten[] /v_out/0
                typ 22
                uniqueObjectIdentifier 1002
                idStringDialog OPV_BUF
                parameter[] 100000.0 0.01 1.0E7
                <\\ElementLK>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_opamp_feedback");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertFalse(diags.stream().anyMatch(d -> "SHORT_CIRCUIT".equals(d.get("rule")) && "OPV_BUF".equals(d.get("component"))),
                "Op-Amp in voltage follower configuration should not trigger SHORT_CIRCUIT");
    }

    @Test
    void wiresMergeOnlyAtEndpointJunctionsNotMidCrossings() {
        String content = """
                ElementLKAnzahl 4
                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 1
                uniqueObjectIdentifier 1001
                idStringDialog R_HORIZ
                x 5
                y 5
                orientierung 502
                parameter[] 10.0
                <\\ElementLK>

                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /NIX_NIX_NIX
                labelEndKnoten[] /NIX_NIX_NIX
                typ 1
                uniqueObjectIdentifier 1002
                idStringDialog R_VERT
                x 15
                y 15
                orientierung 503
                parameter[] 10.0
                <\\ElementLK>

                c (2)
                <ElementLK>
                labelAnfangsKnoten[] /0
                labelEndKnoten[] /0
                typ 31
                uniqueObjectIdentifier 1003
                x 5
                y 1
                <\\ElementLK>

                c (3)
                <ElementLK>
                labelAnfangsKnoten[] /0
                labelEndKnoten[] /0
                typ 31
                uniqueObjectIdentifier 1004
                x 1
                y 15
                <\\ElementLK>

                verbindungLK (0)
                <Verbindung>
                label net_horizontal
                x[] 2 20
                y[] 15 15
                connectorType 0
                <\\Verbindung>

                verbindungLK (1)
                <Verbindung>
                label net_vertical
                x[] 10 10
                y[] 2 25
                connectorType 0
                <\\Verbindung>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_crossing_wires");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertFalse(diags.stream().anyMatch(d -> "SHORT_CIRCUIT".equals(d.get("rule"))),
                "Mid-crossing wires without an endpoint junction must remain separate nets");
    }

    @Test
    void detectsVoltageSourceLoop() {
        String content = """
                ElementLKAnzahl 2

                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /vcc
                labelEndKnoten[] /0
                typ 4
                uniqueObjectIdentifier 1001
                idStringDialog V1
                parameter[] 1 10.0 50.0 0.0 0.0
                <\\ElementLK>

                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /vcc
                labelEndKnoten[] /0
                typ 4
                uniqueObjectIdentifier 1002
                idStringDialog V2
                parameter[] 1 12.0 50.0 0.0 0.0
                <\\ElementLK>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_vsource_loop");
        assertFalse((Boolean) result.get("valid"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        Map<String, String> diag = diags.stream()
                .filter(d -> "VOLTAGE_SOURCE_LOOP".equals(d.get("rule")))
                .findFirst()
                .orElse(null);
        assertNotNull(diag, "Voltage source loop diagnostic should be triggered");
        assertEquals("ERROR", diag.get("level"));
        assertEquals("V2", diag.get("component"));
        assertNotNull(diag.get("suggested_fix"));
        assertTrue(diag.get("suggested_fix").contains("damping resistor"));
    }

    @Test
    void detectsInductiveCutsetWithCurrentSource() {
        String content = """
                ElementLKAnzahl 2

                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /vcc
                labelEndKnoten[] /mid
                typ 2
                uniqueObjectIdentifier 1001
                idStringDialog L1
                parameter[] 0.001
                <\\ElementLK>

                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /mid
                labelEndKnoten[] /0
                typ 5
                uniqueObjectIdentifier 1002
                idStringDialog I1
                parameter[] 1 5.0 50.0 0.0 0.0
                <\\ElementLK>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_ind_cutset_cs");
        assertFalse((Boolean) result.get("valid"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        Map<String, String> diag = diags.stream()
                .filter(d -> "INDUCTIVE_CUTSET".equals(d.get("rule")) && "ERROR".equals(d.get("level")))
                .findFirst()
                .orElse(null);
        assertNotNull(diag, "Inductive cutset error should be triggered for inductor in series with current source");
        assertEquals("L1", diag.get("component"));
        assertNotNull(diag.get("suggested_fix"));
        assertTrue(diag.get("suggested_fix").contains("damping resistor"));
    }

    @Test
    void detectsInductiveCutsetSeriesInductors() {
        String content = """
                ElementLKAnzahl 2

                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /vcc
                labelEndKnoten[] /mid
                typ 2
                uniqueObjectIdentifier 1001
                idStringDialog L1
                parameter[] 0.001
                <\\ElementLK>

                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /mid
                labelEndKnoten[] /0
                typ 2
                uniqueObjectIdentifier 1002
                idStringDialog L2
                parameter[] 0.002
                <\\ElementLK>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_ind_cutset_series");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        Map<String, String> diag = diags.stream()
                .filter(d -> "INDUCTIVE_CUTSET".equals(d.get("rule")) && "WARNING".equals(d.get("level")))
                .findFirst()
                .orElse(null);
        assertNotNull(diag, "Series inductors warning should be triggered");
        assertNotNull(diag.get("suggested_fix"));
        assertTrue(diag.get("suggested_fix").contains("Combine the two series inductors"));
    }

    @Test
    void detectsUnconnectedGateOnActiveSwitch() {
        String content = """
                ElementLKAnzahl 2

                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /vcc
                labelEndKnoten[] /mid
                typ 28
                uniqueObjectIdentifier 1001
                idStringDialog S_MOSFET
                parameter[] 0.01 100.0
                <\\ElementLK>

                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /mid
                labelEndKnoten[] /0
                typ 1
                uniqueObjectIdentifier 1002
                idStringDialog R1
                parameter[] 10.0
                <\\ElementLK>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_unconnected_gate");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        Map<String, String> diag = diags.stream()
                .filter(d -> "UNCONNECTED_GATE".equals(d.get("rule")))
                .findFirst()
                .orElse(null);
        assertNotNull(diag, "Unconnected gate warning should be triggered for active MOSFET");
        assertEquals("WARNING", diag.get("level"));
        assertEquals("S_MOSFET", diag.get("component"));
        assertNotNull(diag.get("suggested_fix"));
        assertTrue(diag.get("suggested_fix").contains("GATE block"));
    }

    @Test
    void gateBlockSuppressesUnconnectedGateWarning() {
        String content = """
                ElementLKAnzahl 2

                c (0)
                <ElementLK>
                labelAnfangsKnoten[] /vcc
                labelEndKnoten[] /mid
                typ 28
                uniqueObjectIdentifier 1001
                idStringDialog S_MOSFET
                parameter[] 0.01 100.0
                <\\ElementLK>

                c (1)
                <ElementLK>
                labelAnfangsKnoten[] /mid
                labelEndKnoten[] /0
                typ 1
                uniqueObjectIdentifier 1002
                idStringDialog R1
                parameter[] 10.0
                <\\ElementLK>

                ElementCONTROLAnzahl 1

                c (0)
                <ElementCONTROL>
                typ 6
                uniqueObjectIdentifier 2001
                idStringDialog GATE1
                coupledReferenceID[] 1001
                parameterString[] /S_MOSFET
                <\\ElementCONTROL>
                """;

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_connected_gate");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        assertFalse(diags.stream().anyMatch(d -> "UNCONNECTED_GATE".equals(d.get("rule"))),
                "Connected gate should suppress UNCONNECTED_GATE warning");
    }

    @Test
    void diagnosticsIncludeActionableSuggestedFix() {
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

        Map<String, Object> result = CircuitValidator.validateContent(content, "test_suggested_fix");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> diags = (List<Map<String, String>>) result.get("diagnostics");
        for (Map<String, String> d : diags) {
            String fix = d.get("suggested_fix");
            assertNotNull(fix, "Diagnostic " + d.get("rule") + " should have suggested_fix populated");
            assertFalse(fix.isBlank(), "Suggested fix should not be blank");
        }
    }

    @Test
    void validatesTutorialCircuitsWithZeroErrors() throws IOException {
        Path tutorialsDir = Path.of("docs/tutorials");
        if (!Files.exists(tutorialsDir)) return;

        try (var stream = Files.walk(tutorialsDir)) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".ipes")).toList();
            for (Path ipes : files) {
                Map<String, Object> res = CircuitValidator.validate(ipes);
                @SuppressWarnings("unchecked")
                List<Map<String, String>> diags = (List<Map<String, String>>) res.get("diagnostics");
                List<Map<String, String>> errors = diags.stream()
                        .filter(d -> "ERROR".equals(d.get("level")))
                        .toList();
                assertEquals(0, errors.size(),
                        "Tutorial circuit " + ipes.getFileName() + " must have 0 validation errors: " + errors);
            }
        }
    }
}


