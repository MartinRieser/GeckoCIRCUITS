package gecko.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComponentCatalogTest {

    @Test
    void catalogContainsEssentialPowerComponents() {
        assertNotNull(ComponentCatalog.get("RESISTOR"));
        assertNotNull(ComponentCatalog.get("INDUCTOR"));
        assertNotNull(ComponentCatalog.get("CAPACITOR"));
        assertNotNull(ComponentCatalog.get("DC_VOLTAGE"));
        assertNotNull(ComponentCatalog.get("AC_VOLTAGE"));
        assertNotNull(ComponentCatalog.get("DIODE"));
        assertNotNull(ComponentCatalog.get("SWITCH"));
        assertNotNull(ComponentCatalog.get("IGBT"));
    }

    @Test
    void catalogContainsEssentialControlComponents() {
        assertNotNull(ComponentCatalog.get("VOLTMETER"));
        assertNotNull(ComponentCatalog.get("AMMETER"));
        assertNotNull(ComponentCatalog.get("CONSTANT"));
        assertNotNull(ComponentCatalog.get("GATE"));
        assertNotNull(ComponentCatalog.get("SCRIPT_BLOCK"));
    }

    @Test
    void caseInsensitiveLookup() {
        assertEquals("RESISTOR", ComponentCatalog.get("resistor").id());
        assertEquals("CAPACITOR", ComponentCatalog.get("Capacitor").id());
        assertEquals("VOLTMETER", ComponentCatalog.get("voltMETER").id());
        assertNull(ComponentCatalog.get("NON_EXISTENT_TYPE"));
    }

    @Test
    void capacitorHasSlots6And7Note() {
        ComponentCatalog.ComponentDef cap = ComponentCatalog.get("CAPACITOR");
        assertEquals(3, cap.typeNumber());
        assertEquals(List.of("p", "n"), cap.pins());
        assertTrue(cap.description().contains("Slots 6 and 7"),
                "Capacitor description should document MNA slot 6/7 sync");
    }

    @Test
    void toCatalogJsonExposesStructuredData() {
        Map<String, Object> json = ComponentCatalog.toCatalogJson();
        assertTrue(json.containsKey("power_components"));
        assertTrue(json.containsKey("control_components"));
        assertTrue(json.containsKey("thermal_components"));
        assertTrue(json.containsKey("special_components"));
        assertTrue(json.containsKey("all_components"));
        assertTrue(json.containsKey("total_component_count"));
        assertTrue(json.containsKey("aliases"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> power = (List<Map<String, Object>>) json.get("power_components");
        assertFalse(power.isEmpty());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> control = (List<Map<String, Object>>) json.get("control_components");
        assertFalse(control.isEmpty());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> thermal = (List<Map<String, Object>>) json.get("thermal_components");
        assertFalse(thermal.isEmpty());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> special = (List<Map<String, Object>>) json.get("special_components");
        assertFalse(special.isEmpty());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> all = (List<Map<String, Object>>) json.get("all_components");
        assertEquals(power.size() + control.size() + thermal.size() + special.size(), all.size());
        assertEquals(ComponentCatalog.all().size(), json.get("total_component_count"));

        Map<String, Object> res = power.stream()
                .filter(c -> "RESISTOR".equals(c.get("type")))
                .findFirst()
                .orElseThrow();
        assertEquals("R", res.get("prefix"));
        assertEquals(1, res.get("type_number"));
    }

    @Test
    void aliasResolution_resolvesCommonVariants() {
        assertEquals("IDEAL_SWITCH", ComponentCatalog.get("SW").id());
        assertEquals("IDEAL_SWITCH", ComponentCatalog.get("SWITCH").id());
        assertEquals("RESISTOR", ComponentCatalog.get("R").id());
        assertEquals("INDUCTOR", ComponentCatalog.get("L").id());
        assertEquals("CAPACITOR", ComponentCatalog.get("C").id());
        assertEquals("GLOBAL_TERMINAL", ComponentCatalog.get("GND").id());
        assertEquals("GLOBAL_TERMINAL", ComponentCatalog.get("GROUND").id());
        assertEquals("OPAMP", ComponentCatalog.get("OP").id());
        assertEquals("RELUCTANCE", ComponentCatalog.get("RM").id());
        assertEquals("MMF_SOURCE", ComponentCatalog.get("MMF").id());
        assertEquals("TEXTBOX", ComponentCatalog.get("NOTE").id());
        assertEquals("MUL", ComponentCatalog.get("MULTIPLY").id());
        assertEquals("DIV", ComponentCatalog.get("DIVIDE").id());
        assertEquals("TH_MODUL", ComponentCatalog.get("MOD").id());
    }

    @Test
    void allComponents_haveValidMetadata() {
        for (ComponentCatalog.ComponentDef def : ComponentCatalog.all().values()) {
            assertNotNull(def.id(), "id null");
            assertNotNull(def.displayName(), "displayName null on " + def.id());
            assertNotNull(def.domain(), "domain null on " + def.id());
            assertTrue(def.typeNumber() > 0, "invalid typeNumber on " + def.id());
            assertNotNull(def.defaultPrefix(), "prefix null on " + def.id());
            assertNotNull(def.pins(), "pins null on " + def.id());
            assertTrue(def.xPinCount() >= 0 && def.xPinCount() <= def.pins().size(),
                    "xPinCount out of bounds on " + def.id());
            assertNotNull(def.description(), "description null on " + def.id());
            for (ComponentCatalog.ParameterDef p : def.parameters()) {
                assertNotNull(p.name(), "param name null on " + def.id());
                assertNotNull(p.type(), "param type null on " + def.id());
                assertTrue(p.targetSlot() >= 0, "param slot negative on " + def.id() + "." + p.name());
            }
        }
    }

    @Test
    void toCatalogJsonSupportsDomainFiltering() {
        Map<String, Object> powerOnly = ComponentCatalog.toCatalogJson("power", null);
        assertEquals("power", powerOnly.get("filtered_domain"));
        assertTrue((Integer) powerOnly.get("count") >= 25);
        assertNotNull(powerOnly.get("components"));
        assertNull(powerOnly.get("power_components")); // only filtered components list

        Map<String, Object> controlOnly = ComponentCatalog.toCatalogJson("control", null);
        assertEquals("control", controlOnly.get("filtered_domain"));
        assertTrue((Integer) controlOnly.get("count") >= 40);

        Map<String, Object> thermalOnly = ComponentCatalog.toCatalogJson("thermal", null);
        assertEquals("thermal", thermalOnly.get("filtered_domain"));
        assertTrue((Integer) thermalOnly.get("count") >= 8);

        Map<String, Object> specialOnly = ComponentCatalog.toCatalogJson("special", null);
        assertEquals("special", specialOnly.get("filtered_domain"));
        assertTrue((Integer) specialOnly.get("count") >= 1);
    }

    @Test
    void toCatalogJsonSupportsSingleComponentQueryWithExamples() {
        Map<String, Object> opamp = ComponentCatalog.toCatalogJson(null, "OPAMP");
        assertEquals(Boolean.TRUE, opamp.get("found"));
        assertNotNull(opamp.get("component"));
        @SuppressWarnings("unchecked")
        Map<String, Object> opComp = (Map<String, Object>) opamp.get("component");
        assertEquals("OPAMP", opComp.get("type"));
        assertEquals(22, opComp.get("type_number"));
        assertEquals(List.of("in_plus", "in_minus", "out"), opComp.get("pins"));

        @SuppressWarnings("unchecked")
        List<String> aliases = (List<String>) opamp.get("aliases");
        assertTrue(aliases.contains("OP"));

        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) opamp.get("usage_example");
        assertEquals("components", usage.get("placement_section"));

        // Ground terminal lookup by alias
        Map<String, Object> gnd = ComponentCatalog.toCatalogJson(null, "GND");
        assertEquals(Boolean.TRUE, gnd.get("found"));
        @SuppressWarnings("unchecked")
        Map<String, Object> gndComp = (Map<String, Object>) gnd.get("component");
        assertEquals("GLOBAL_TERMINAL", gndComp.get("type"));

        // Control block lookup
        Map<String, Object> mul = ComponentCatalog.toCatalogJson(null, "MUL");
        assertEquals(Boolean.TRUE, mul.get("found"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mulComp = (Map<String, Object>) mul.get("component");
        assertEquals(1019, mulComp.get("type_number"));

        // Unknown component query
        Map<String, Object> unknown = ComponentCatalog.toCatalogJson(null, "UNKNOWN_FOOBAR");
        assertEquals(Boolean.FALSE, unknown.get("found"));
        assertNotNull(unknown.get("suggestions"));
    }
}
