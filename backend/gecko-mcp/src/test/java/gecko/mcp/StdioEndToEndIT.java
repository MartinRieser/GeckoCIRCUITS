package gecko.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test over real stdio: spawns the server main class in a fresh
 * JVM (with this test's classpath) and talks MCP to it, exactly like
 * Claude Desktop / Cursor would.
 */
class StdioEndToEndIT {

    @Test
    void stdioRoundTripListToolsAndSimulate() throws IOException {
        Path circuit = tempFixture("rc-lowpass.ipes");
        String classpath = System.getProperty("java.class.path");
        String javaExe = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                .toString();

        Path argFile = Files.createTempFile("mcp-args-", ".args");
        Files.writeString(argFile, "-cp\n\"" + classpath.replace("\\", "\\\\") + "\"\ngecko.mcp.GeckoMcpServer\n");
        argFile.toFile().deleteOnExit();

        ServerParameters params = ServerParameters.builder(javaExe)
                .args(List.of("@" + argFile.toAbsolutePath()))
                .build();
        McpSyncClient client = McpClient.sync(new StdioClientTransport(params,
                new JacksonMcpJsonMapper(JsonMapper.builder().build())))
                .requestTimeout(Duration.ofSeconds(120))
                .initializationTimeout(Duration.ofSeconds(30))
                .build();
        try {
            client.initialize();

            McpSchema.ListToolsResult tools = client.listTools();
            assertEquals(14, tools.tools().size(), "server should expose the 14 tools");

            McpSchema.ListResourcesResult resources = client.listResources();
            assertEquals(7, resources.resources().size(), "server should expose 7 MCP resources");

            McpSchema.ReadResourceResult catRes = client.readResource(
                    McpSchema.ReadResourceRequest.builder("gecko://catalog/components").build());
            assertFalse(catRes.contents().isEmpty(), "catalog resource should return content");

            McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder(
                    "gecko_simulate")
                    .arguments(Map.of("circuit_path", circuit.toString(),
                            "duration", 0.002, "dt", 1e-6))
                    .build());
            assertFalse(Boolean.TRUE.equals(result.isError()));
            String json = ((McpSchema.TextContent) result.content().get(0)).text();
            assertTrue(json.contains("\"status\":\"COMPLETED\""), "simulate result: " + json);
            assertTrue(json.contains("u_out"), "signal names should be present");
            assertTrue(json.contains("\"metadata\""), "engine metadata should be present");

            McpSchema.CallToolResult signals = client.callTool(McpSchema.CallToolRequest.builder(
                    "gecko_list_signals")
                    .arguments(Map.of("circuit_path", circuit.toString()))
                    .build());
            assertFalse(Boolean.TRUE.equals(signals.isError()), "list_signals result: "
                    + ((McpSchema.TextContent) signals.content().get(0)).text());
        } finally {
            client.closeGracefully();
        }
    }

    private static Path tempFixture(String name) throws IOException {
        try (InputStream in = StdioEndToEndIT.class.getResourceAsStream("/fixtures/" + name)) {
            Path file = Files.createTempFile("gecko-mcp-e2e-", "-" + name);
            Files.write(file, in.readAllBytes());
            file.toFile().deleteOnExit();
            return file;
        }
    }
}
