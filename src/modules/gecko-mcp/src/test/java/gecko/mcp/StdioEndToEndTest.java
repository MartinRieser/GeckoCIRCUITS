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
class StdioEndToEndTest {

    @Test
    void stdioRoundTripListToolsAndSimulate() throws IOException {
        Path circuit = tempFixture("rc-lowpass.ipes");
        String classpath = System.getProperty("java.class.path");
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                .toString();

        Path argFile = Files.createTempFile("mcp-args-", ".args");
        Files.writeString(argFile, "-cp\n\"" + classpath.replace("\\", "\\\\") + "\"\ngecko.mcp.GeckoMcpServer\n");
        argFile.toFile().deleteOnExit();

        ServerParameters params = ServerParameters.builder(java)
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
            assertEquals(13, tools.tools().size(), "server should expose the 13 tools");

            McpSchema.ListResourcesResult resources = client.listResources();
            assertEquals(4, resources.resources().size(), "server should expose 4 MCP resources");

            McpSchema.ReadResourceResult catRes = client.readResource(
                    new McpSchema.ReadResourceRequest("gecko://catalog/components"));
            assertFalse(catRes.contents().isEmpty(), "catalog resource should return content");

            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                    "gecko_simulate",
                    Map.of("circuit_path", circuit.toString(),
                            "duration", 0.002, "dt", 1e-6)));
            assertFalse(Boolean.TRUE.equals(result.isError()));
            String json = ((McpSchema.TextContent) result.content().get(0)).text();
            assertTrue(json.contains("\"status\":\"COMPLETED\""), "simulate result: " + json);
            assertTrue(json.contains("u_out"), "signal names should be present");
        } finally {
            client.closeGracefully();
        }
    }

    private static Path tempFixture(String name) throws IOException {
        try (InputStream in = StdioEndToEndTest.class.getResourceAsStream("/fixtures/" + name)) {
            Path file = Files.createTempFile("gecko-mcp-e2e-", "-" + name);
            Files.write(file, in.readAllBytes());
            file.toFile().deleteOnExit();
            return file;
        }
    }
}
