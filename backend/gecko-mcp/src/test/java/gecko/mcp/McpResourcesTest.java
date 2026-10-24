package gecko.mcp;

import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpResourcesTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void allResourcesHaveValidMetadataAndUris() {
        List<SyncResourceSpecification> specs = McpResources.all();
        assertEquals(7, specs.size(), "Should expose 7 MCP resources");
        for (SyncResourceSpecification spec : specs) {
            McpSchema.Resource res = spec.resource();
            assertNotNull(res.uri());
            assertTrue(res.uri().startsWith("gecko://"), "URI should start with gecko://: " + res.uri());
            assertNotNull(res.name());
            assertNotNull(res.description());
            assertNotNull(res.mimeType());
            assertNotNull(spec.readHandler());
        }
    }

    @Test
    void readsComponentCatalogResource() throws Exception {
        SyncResourceSpecification spec = findSpec("gecko://catalog/components");
        McpSchema.ReadResourceResult result = readSpec(spec);
        assertNotNull(result);
        assertFalse(result.contents().isEmpty());
        McpSchema.TextResourceContents content = (McpSchema.TextResourceContents) result.contents().get(0);
        assertEquals("application/json", content.mimeType());

        @SuppressWarnings("unchecked")
        Map<String, Object> map = JSON.readValue(content.text(), Map.class);
        assertTrue((Integer) map.get("total_component_count") > 60);
        assertTrue(map.containsKey("power_components"));
        assertTrue(map.containsKey("control_components"));
        assertTrue(map.containsKey("thermal_components"));
        assertTrue(map.containsKey("special_components"));
        assertTrue(map.containsKey("aliases"));
    }

    @Test
    void readsScriptBlockReferenceResource() {
        SyncResourceSpecification spec = findSpec("gecko://reference/script-blocks");
        McpSchema.ReadResourceResult result = readSpec(spec);
        assertNotNull(result);
        McpSchema.TextResourceContents content = (McpSchema.TextResourceContents) result.contents().get(0);
        assertEquals("text/markdown", content.mimeType());
        assertTrue(content.text().contains("ScriptBlockCalculator"));
        assertTrue(content.text().contains("xIN"));
        assertTrue(content.text().contains("yOUT"));
    }

    @Test
    void readsControlBlocksReferenceResource() {
        SyncResourceSpecification spec = findSpec("gecko://reference/control-blocks");
        McpSchema.ReadResourceResult result = readSpec(spec);
        assertNotNull(result);
        McpSchema.TextResourceContents content = (McpSchema.TextResourceContents) result.contents().get(0);
        assertEquals("text/markdown", content.mimeType());
        assertTrue(content.text().contains("MUL"));
        assertTrue(content.text().contains("DIV"));
        assertTrue(content.text().contains("PT2"));
        assertTrue(content.text().contains("HYS"));
        assertTrue(content.text().contains("DEADTIME"));
    }

    @Test
    void readsWorkflowReferenceResource() {
        SyncResourceSpecification spec = findSpec("gecko://reference/workflow");
        McpSchema.ReadResourceResult result = readSpec(spec);
        McpSchema.TextResourceContents content = (McpSchema.TextResourceContents) result.contents().get(0);
        assertEquals("text/markdown", content.mimeType());
        assertTrue(content.text().contains("Workflow") || content.text().contains("workflow") || content.text().contains("simulation"));
    }

    @Test
    void readsMultiDomainReferenceResource() {
        SyncResourceSpecification spec = findSpec("gecko://reference/multi-domain");
        McpSchema.ReadResourceResult result = readSpec(spec);
        McpSchema.TextResourceContents content = (McpSchema.TextResourceContents) result.contents().get(0);
        assertEquals("text/markdown", content.mimeType());
        assertTrue(content.text().contains("Thermal") || content.text().contains("thermal") || content.text().contains("Losses"));
    }

    @Test
    void readsTopologiesReferenceResource() {
        SyncResourceSpecification spec = findSpec("gecko://reference/topologies");
        McpSchema.ReadResourceResult result = readSpec(spec);
        McpSchema.TextResourceContents content = (McpSchema.TextResourceContents) result.contents().get(0);
        assertEquals("text/markdown", content.mimeType());
        assertTrue(content.text().contains("Buck") || content.text().contains("PFC") || content.text().contains("LLC"));
    }

    @Test
    void readsExamplesListResource() {
        SyncResourceSpecification spec = findSpec("gecko://examples/list");
        McpSchema.ReadResourceResult result = readSpec(spec);
        McpSchema.TextResourceContents content = (McpSchema.TextResourceContents) result.contents().get(0);
        assertTrue(content.text().contains("ipes") || content.text().contains("circuit"));
    }

    private static SyncResourceSpecification findSpec(String uri) {
        return McpResources.all().stream()
                .filter(s -> s.resource().uri().equals(uri))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing resource spec for: " + uri));
    }

    private static McpSchema.ReadResourceResult readSpec(SyncResourceSpecification spec) {
        McpSchema.ReadResourceRequest req = McpSchema.ReadResourceRequest.builder(spec.resource().uri()).build();
        return spec.readHandler().apply(null, req);
    }
}
