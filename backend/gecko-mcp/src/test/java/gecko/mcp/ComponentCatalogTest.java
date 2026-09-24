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

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> power = (List<Map<String, Object>>) json.get("power_components");
        assertFalse(power.isEmpty());

        Map<String, Object> res = power.stream()
                .filter(c -> "RESISTOR".equals(c.get("type")))
                .findFirst()
                .orElseThrow();
        assertEquals("R", res.get("prefix"));
        assertEquals(1, res.get("type_number"));
    }
}
