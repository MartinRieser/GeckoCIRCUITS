# MCP Interface (LLM Tools)

GeckoCIRCUITS ships a [Model Context Protocol](https://modelcontextprotocol.io)
server so LLM clients — Claude Desktop, Cursor, ZCode & co. — can build,
simulate, and analyze power-electronics circuits autonomously.

There are two implementations of the same interface:

| | Bundled server (Java) | Development server (Python) |
|---|---|---|
| Location | `src/modules/gecko-mcp` | `tools/mcp/gecko_mcp` |
| Ships with | The desktop app (`engine/gecko-mcp.jar`) | Repository only |
| Requires | Nothing — runs on the app's bundled Java runtime | Python 3 + `uv`, JDK 25 for the engine |
| Transports | stdio | stdio, SSE, streamable-HTTP |
| Simulation | In-process headless engine | Subprocess `java -cp … gecko.core.GeckoHeadless` |
| Extra tools | — | `web_server.py` LLC studio |

Both expose the **same 10 tools** with identical names, parameters, and result
shapes; byte-exact golden equivalence tests (`gecko-mcp` module:
`ProjectGoldenTest`, `ToolsTest`, `StdioEndToEndTest`) keep the port faithful.

## Connecting a client

### Desktop app (no prerequisites)

Run the launcher generator once per installation:

```sh
python scripts/desktop/write-mcp-launchers.py --dest <install-dir>
```

It writes `gecko-mcp.bat` / `gecko-mcp.sh` plus a ready-made
`mcp-client-config.json`. Register the server in your client, e.g.
Claude Desktop (`claude_desktop_config.json`) or Cursor:

```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "C:/Program Files/GeckoCIRCUITS/gecko-mcp.bat"
    }
  }
}
```

### Repository development

`.agents/mcp_config.json` already registers the Python server for ZCode:

```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "uv run --with \"mcp<2\" python tools/mcp/gecko_mcp/server.py"
    }
  }
}
```

Environment overrides: `GECKO_HOME` (workspace root), `GECKO_GUI_JAR`,
`GECKO_JAVA_HOME`, `GECKO_REST_PORT`. Relative circuit paths resolve against
the workspace root.

## Tools

| Tool | Purpose |
|------|---------|
| `gecko_server_status` | Engine/Java environment status and capabilities |
| `gecko_catalog` | Placeable component types + parameter schemas (power and control domains) |
| `gecko_setup_pfc_project` | Generate a 2-phase interleaved boost PFC project with an MCU script block (PI loop, interleaved PWM) and dynamic load step |
| `gecko_setup_llc_project` | Generate a half-bridge LLC resonant converter with ZVS snubber and tank analytics (f0, Z0, Q, k) |
| `gecko_inspect_circuit` | Inspect a `.ipes` file: simulation parameters, power components, control blocks, script code |
| `gecko_patch_component` | Patch component parameters, e.g. `{"param0": 25.0}` sets R.1's resistance |
| `gecko_set_script_code` | Replace the source code / static variables of a typ-61 microcontroller block |
| `gecko_simulate` | Run a headless simulation; returns step count and signal names |
| `gecko_get_waveforms` | Run + downsample waveforms, compute metrics: steady-state DC, ripple, RMS/power factor, LLC ZVS detection, load-step regulation |
| `gecko_tune_pfc` | Evaluate PI tuning against a target voltage and produce tuning recommendations |

A typical LLM workflow: `gecko_catalog` → `gecko_setup_pfc_project` →
`gecko_set_script_code` (adjust controller) → `gecko_simulate` →
`gecko_get_waveforms` → iterate on gains via `gecko_patch_component` /
`gecko_set_script_code`.

## Security notes

- The bundled server speaks **stdio only** — it is started by the LLM client
  as a child process, not a network service.
- The engine it drives binds to `127.0.0.1` exclusively.
- Circuit files are read/written inside the configured workspace root
  (`GECKO_HOME` or the working directory); the desktop app passes absolute
  user-chosen paths.

## Implementation notes

- MCP Java SDK 2.0.1 (`mcp-core` + `mcp-json-jackson3`), shaded into
  `gecko-mcp-1.0.0-jar-with-dependencies.jar` (main class
  `gecko.mcp.GeckoMcpServer`).
- The PFC/LLC `.ipes` generators are template resources extracted from the
  Python originals (`scripts/desktop/extract-templates.py`); golden tests
  compare Java output byte-for-byte against Python-generated fixtures.
- One deliberate deviation from bug-for-bug parity: the component patcher's
  block regex cannot match across element boundaries, so it always patches the
  named component (the Python regex could hit the wrong one).
- The desktop app exposes the same MCP server through its install directory —
  see [Desktop App](desktop-app.md).
