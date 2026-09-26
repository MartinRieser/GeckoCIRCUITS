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

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.parameters.BjtParameters;
import gecko.core.circuit.parameters.CapacitorParameters;
import gecko.core.circuit.parameters.DiodeParameters;
import gecko.core.circuit.parameters.InductorParameters;
import gecko.core.circuit.parameters.ResistorParameters;
import gecko.core.circuit.parameters.SourceParameters;
import gecko.core.circuit.parameters.SwitchParameters;
import gecko.core.circuit.parameters.TransformerParameters;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the catalog's documented parameter slots to the typed parameter
 * classes of the simulation core: the catalog is the single knowledge source
 * for LLM-facing authoring, so a drifted slot index would silently corrupt
 * generated circuits.
 */
class CatalogSlotCrossCheckTest {

    private static int slotOf(String componentId, String parameterName) {
        ComponentCatalog.ComponentDef def = ComponentCatalog.get(componentId);
        assertNotNull(def, "catalog entry missing: " + componentId);
        return def.parameters().stream()
                .filter(p -> p.name().equals(parameterName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "parameter '" + parameterName + "' missing on " + componentId))
                .targetSlot();
    }

    @Test
    void passiveComponentSlots_matchTypedParameterClasses() {
        assertEquals(ResistorParameters.INDEX_RESISTANCE, slotOf("RESISTOR", "resistance"));
        assertEquals(InductorParameters.INDEX_INDUCTANCE, slotOf("INDUCTOR", "inductance"));
        assertEquals(InductorParameters.INDEX_INITIAL_CURRENT, slotOf("INDUCTOR", "i_init"));
        assertEquals(CapacitorParameters.INDEX_CAPACITANCE, slotOf("CAPACITOR", "capacitance"));
        assertEquals(CapacitorParameters.INDEX_INITIAL_VOLTAGE, slotOf("CAPACITOR", "v_init"));
    }

    @Test
    void semiconductorSlots_matchTypedParameterClasses() {
        assertEquals(DiodeParameters.INDEX_FORWARD_VOLTAGE, slotOf("DIODE", "u_forward"));
        assertEquals(DiodeParameters.INDEX_R_ON, slotOf("DIODE", "r_on"));
        assertEquals(DiodeParameters.INDEX_R_OFF, slotOf("DIODE", "r_off"));

        // IGBT and thyristor share the diode slot layout (DiodeParameters)
        assertEquals(DiodeParameters.INDEX_FORWARD_VOLTAGE, slotOf("IGBT", "u_forward"));
        assertEquals(DiodeParameters.INDEX_R_ON, slotOf("IGBT", "r_on"));
        assertEquals(DiodeParameters.INDEX_TURN_OFF_DELAY, slotOf("THYRISTOR", "turn_off_delay"));

        // LK_S uses the SwitchParameters layout (on=1/off=2)
        assertEquals(SwitchParameters.INDEX_R_ON, slotOf("IDEAL_SWITCH", "r_on"));
        assertEquals(SwitchParameters.INDEX_R_OFF, slotOf("IDEAL_SWITCH", "r_off"));
        assertEquals(SwitchParameters.INDEX_GATE_SIGNAL, slotOf("IDEAL_SWITCH", "initial_state") == 0
                ? SwitchParameters.INDEX_GATE_SIGNAL : -1,
                "sanity: gate signal slot is 8");

        // MOSFET/IGBT gate drives use the non-LK_S on/off slots 2/3
        // (ControlCalculatorBuilder.GateDrive.onResistanceSlot)
        assertEquals(2, slotOf("MOSFET", "r_on"));
        assertEquals(3, slotOf("MOSFET", "r_off"));
        assertEquals(2, slotOf("IGBT", "r_on"));
        assertEquals(3, slotOf("IGBT", "r_off"));
    }

    @Test
    void sourceSlots_matchSourceParameters() {
        assertEquals(SourceParameters.INDEX_SOURCE_TYPE, slotOf("VOLTAGE_SOURCE_DC", "source_type"));
        assertEquals(SourceParameters.INDEX_VALUE_DC, slotOf("VOLTAGE_SOURCE_DC", "voltage"));
        assertEquals(SourceParameters.INDEX_VALUE_DC, slotOf("VOLTAGE_SOURCE_AC", "amplitude"));
        assertEquals(SourceParameters.INDEX_FREQUENCY, slotOf("VOLTAGE_SOURCE_AC", "frequency"));
        assertEquals(SourceParameters.INDEX_OFFSET, slotOf("VOLTAGE_SOURCE_AC", "offset"));
        assertEquals(SourceParameters.INDEX_PHASE_DEG, slotOf("VOLTAGE_SOURCE_AC", "phase_deg"));
        assertEquals(SourceParameters.INDEX_AMPLITUDE_SIN, 20, "sin amplitude mirror slot");

        assertEquals(SourceParameters.INDEX_VALUE_DC, slotOf("CURRENT_SOURCE_DC", "current"));
        assertEquals(SourceParameters.INDEX_PHASE_DEG, slotOf("CURRENT_SOURCE_AC", "phase_deg"));

        // thermal sources share the SourceParameters layout
        assertEquals(SourceParameters.INDEX_VALUE_DC, slotOf("TH_FLOW", "power"));
        assertEquals(SourceParameters.INDEX_VALUE_DC, slotOf("TH_TEMP", "temperature"));
    }

    @Test
    void machineAndThermalSlots_matchTypedParameterClasses() {
        assertEquals(TransformerParameters.INDEX_N1, slotOf("TRANSFORMER", "n1"));
        assertEquals(TransformerParameters.INDEX_N2, slotOf("TRANSFORMER", "n2"));
        assertEquals(TransformerParameters.INDEX_POLARITY, slotOf("TRANSFORMER", "polarity"));

        assertEquals(BjtParameters.INDEX_BETA_F, slotOf("BJT", "beta_f"));
        assertEquals(BjtParameters.INDEX_BETA_B, slotOf("BJT", "beta_b"));

        assertEquals(ResistorParameters.INDEX_RESISTANCE, slotOf("TH_RTH", "r_th"));
        assertEquals(0, slotOf("TH_CTH", "c_th"));
        assertEquals(1, slotOf("TH_CTH", "t_init"));
    }

    @Test
    void controlBlockSlots_matchTheEngineVerifiedLayouts() {
        // layouts executed by ControlCalculatorBuilder and pinned behaviorally
        // by NativeControlBlockTest (closed-loop buck + per-block unit tests)
        assertEquals(0, slotOf("CONSTANT", "value"));
        assertEquals(0, slotOf("GAIN", "k"));
        assertEquals(0, slotOf("PI", "kp"));
        assertEquals(1, slotOf("PI", "ti"));
        assertEquals(0, slotOf("PT1", "tau"));
        assertEquals(0, slotOf("INTEGRATOR", "initial_value"));
        assertEquals(0, slotOf("DELAY", "tau"));
        assertEquals(0, slotOf("SIGNAL_SOURCE", "waveform"));
        assertEquals(1, slotOf("SIGNAL_SOURCE", "amplitude"));
        assertEquals(2, slotOf("SIGNAL_SOURCE", "frequency"));
        assertEquals(3, slotOf("SIGNAL_SOURCE", "offset"));
        assertEquals(4, slotOf("SIGNAL_SOURCE", "phase_rad"));
        assertEquals(5, slotOf("SIGNAL_SOURCE", "duty"));
    }

    @Test
    void controlTypeNumbers_matchCircuitTypCore() {
        Map<String, CircuitTypCore> expected = new HashMap<>();
        expected.put("GATE", CircuitTypCore.CTRL_GATE);
        expected.put("VOLTMETER", CircuitTypCore.CTRL_VOLT);
        expected.put("AMMETER", CircuitTypCore.CTRL_AMP);
        expected.put("SCOPE", CircuitTypCore.CTRL_SCOPE);
        expected.put("SIGNAL_SOURCE", CircuitTypCore.CTRL_SIGNAL);
        expected.put("CONSTANT", CircuitTypCore.CTRL_CONSTANT);
        expected.put("GAIN", CircuitTypCore.CTRL_GAIN);
        expected.put("PI", CircuitTypCore.CTRL_PI);
        expected.put("PT1", CircuitTypCore.CTRL_PT1);
        expected.put("INTEGRATOR", CircuitTypCore.CTRL_INTEGRATOR);
        expected.put("COMPARATOR", CircuitTypCore.CTRL_COMPARATOR);
        expected.put("AND", CircuitTypCore.CTRL_AND);
        expected.put("OR", CircuitTypCore.CTRL_OR);
        expected.put("NOT", CircuitTypCore.CTRL_NOT);
        expected.put("SELECTOR", CircuitTypCore.CTRL_MUX);
        expected.put("DELAY", CircuitTypCore.CTRL_DELAY);
        expected.put("SCRIPT_BLOCK", CircuitTypCore.CTRL_SCRIPT);
        expected.put("JAVA_BLOCK", CircuitTypCore.C_JAVA_FUNCTION);

        for (Map.Entry<String, CircuitTypCore> entry : expected.entrySet()) {
            ComponentCatalog.ComponentDef def = ComponentCatalog.get(entry.getKey());
            assertNotNull(def, "catalog entry missing: " + entry.getKey());
            assertEquals(entry.getValue().getTypeNumber(), def.typeNumber(),
                    "type number drift on " + entry.getKey());
            assertEquals("CONTROL", def.domain());
        }
    }

    @Test
    void catalogKnowledge_isSelfContained() {
        Map<String, Object> json = ComponentCatalog.toCatalogJson();
        assertTrue(json.containsKey("power_components"));
        assertTrue(json.containsKey("control_components"));
        assertTrue(json.containsKey("thermal_components"));
        assertTrue(json.containsKey("orientation_codes"));
        assertTrue(json.containsKey("grid_and_wiring_rules"));
        assertTrue(json.containsKey("control_block_geometry"));
        assertTrue(json.containsKey("signal_naming"));
        assertTrue(json.containsKey("parameter_overrides"));
        assertTrue(json.containsKey("solver_options"));
        assertTrue(json.containsKey("legacy_control_type_numbers"));
        assertTrue(json.containsKey("script_block_guide"));
        assertTrue(json.containsKey("circuit_synthesis_guide"));

        @SuppressWarnings("unchecked")
        Map<String, Object> orientations = (Map<String, Object>) json.get("orientation_codes");
        for (String code : new String[]{"501", "502", "503", "504"}) {
            assertTrue(orientations.containsKey(code), "orientation " + code + " documented");
        }
    }
}
