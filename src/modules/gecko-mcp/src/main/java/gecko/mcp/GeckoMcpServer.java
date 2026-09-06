package gecko.mcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * Stdio MCP server exposing the GeckoCIRCUITS headless engine to LLM clients.
 * Bundled with the desktop app and runnable standalone:
 * <pre>java -jar gecko-mcp.jar</pre>
 * All diagnostics go to stderr; stdout carries only the MCP protocol.
 */
public final class GeckoMcpServer {

    private static final String SERVER_NAME = "gecko-circuits";
    private static final String SERVER_VERSION = "1.0.0";

    private GeckoMcpServer() {
    }

    public static void main(String[] args) {
        JacksonMcpJsonMapper json = new JacksonMcpJsonMapper(JsonMapper.builder().build());
        var transport = new StdioServerTransportProvider(json);
        var builder = McpServer.sync(transport)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build());

        for (GeckoTools.ToolSpec tool : GeckoTools.all()) {
            builder.tools(toSpecification(json, tool));
            System.err.println("[gecko-mcp] registered tool: " + tool.name());
        }
        builder.build();
        System.err.println("[gecko-mcp] server ready on stdio");
    }

    private static McpServerFeatures.SyncToolSpecification toSpecification(
            JacksonMcpJsonMapper json, GeckoTools.ToolSpec tool) {
        McpSchema.Tool schema = McpSchema.Tool.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(tool.inputSchema())
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(schema)
                .callHandler((exchange, request) -> {
                    try {
                        Object result = tool.handler().apply(request.arguments() == null
                                ? Map.of() : request.arguments());
                        return McpSchema.CallToolResult.builder()
                                .addTextContent(json.writeValueAsString(result))
                                .build();
                    } catch (Exception e) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent(String.valueOf(e.getMessage()))
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }
}
