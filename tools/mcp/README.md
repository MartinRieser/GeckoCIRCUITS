# GeckoCIRCUITS Model Context Protocol (MCP) Server

Seamless integration between **GeckoCIRCUITS** and LLM harnesses:
- **Google Antigravity**
- **GitHub Copilot (VS Code Agent Mode)**
- **Claude Code / Anthropic Desktop**
- **Cursor / Continue**

## Capabilities

The MCP server exposes high-level semantic tools for circuit design and closed-loop tuning:
1. `gecko_server_status`: Verifies JDK 25 environment, simulation jar readiness, and engine health.
2. `gecko_catalog`: Returns the library of power electronics components (sources, semiconductors, passives, transformers, sensors, script blocks) and parameter schemas.
3. `gecko_inspect_circuit`: Parses `.ipes` circuit models, listing components, control blocks, wire topology, and simulation settings.
4. `gecko_patch_component`: Modifies component parameters (inductance, capacitance, resistance, switching frequencies, voltages).
5. `gecko_set_script_code`: Updates microcontroller control algorithms inside `CTRL_SCRIPT` (`TYP_JAVA_FUNCTION` / `TYP_SCRIPT`) blocks.
6. `gecko_simulate`: Submits headless simulation runs and reports step count, simulation time, and execution performance.
7. `gecko_get_waveforms`: Runs simulations and extracts time-series waveforms along with key power electronics metrics (steady-state DC output, ripple $\Delta V_{pp}$, RMS, power factor, THD).
8. `gecko_tune_pfc`: Runs closed-loop tuning sweeps for active PFC controllers, adjusting PI gains to reach target voltage and minimize ripple.

---

## Configuration Guide

### 1. Google Antigravity
The server is automatically registered via `.agents/mcp_config.json` in the workspace root:
```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "uv",
      "args": [
        "run",
        "--with", "mcp<2",
        "python",
        "tools/mcp/gecko_mcp/server.py"
      ]
    }
  }
}
```

Or in global Antigravity config (`~/.gemini/config/mcp_config.json`), specify the directory of your local checkout:
```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "uv",
      "args": [
        "--directory", "/path/to/GeckoCIRCUITS",
        "run",
        "--with", "mcp<2",
        "python",
        "tools/mcp/gecko_mcp/server.py"
      ]
    }
  }
}
```

### 2. GitHub Copilot (VS Code)
In `.vscode/settings.json`:
```json
{
  "github.copilot.chat.mcp.servers": {
    "gecko-circuits": {
      "command": "uv",
      "args": [
        "run",
        "--with", "mcp<2",
        "python",
        "${workspaceFolder}/tools/mcp/gecko_mcp/server.py"
      ]
    }
  }
}
```

### 3. Claude Code / Claude Desktop
In `claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "uv",
      "args": [
        "--directory", "/path/to/GeckoCIRCUITS",
        "run",
        "--with", "mcp<2",
        "python",
        "tools/mcp/gecko_mcp/server.py"
      ]
    }
  }
}
```

---

## Remote & Cross-Machine Access (Network Port via SSE)

You can run the MCP server on a remote Linux server, workstation, or Docker container and connect to it over a network port:

### Starting the Server on a Remote Machine:
```bash
# Listen on port 8000 (accessible across the local network)
uv run --with "mcp<2" python tools/mcp/gecko_mcp/server.py --transport sse --host 0.0.0.0 --port 8000
```
Or with environment variables:
```bash
MCP_TRANSPORT=sse MCP_HOST=0.0.0.0 MCP_PORT=8000 uv run --with "mcp<2" python tools/mcp/gecko_mcp/server.py
```

### Connecting MCP Clients via SSE:
In any MCP-compatible client config (Antigravity, Claude Desktop, Cursor, etc.):
```json
{
  "mcpServers": {
    "gecko-circuits": {
      "url": "http://<remote-host-ip>:8000/sse"
    }
  }
}
```

---

## Cross-Platform Discovery & Environment Variables

`server.py` automatically discovers Java and paths on Windows, Linux, and macOS without requiring any hardcoded paths. You can customize behavior using environment variables or CLI flags:

| Variable / Flag | Default | Description |
| --- | --- | --- |
| `JAVA_HOME` | Auto-detected | Path to JDK 25+ installation. Auto-detects in `~/.jdks`, `/usr/lib/jvm`, `/Library/Java/...`, SDKMAN, PATH. |
| `GECKO_HOME` / `--gecko-home` | Auto-detected | Root directory of the GeckoCIRCUITS repository. |
| `GECKO_GUI_JAR` / `--gui-jar` | Auto-detected | Path to `gecko-1.0-jar-with-dependencies.jar` in `target/`. |
| `GECKO_REST_PORT` | `8080` | Port for the optional Gecko REST API health check. |
| `GECKO_REST_URL` | `http://localhost:8080/gecko/api/v1/health` | Full health-check endpoint for Gecko REST API. |
| `MCP_TRANSPORT` / `--transport` | `stdio` | Transport type: `stdio` (local process) or `sse` / `streamable-http` (HTTP port). |
| `MCP_HOST` / `--host` | `127.0.0.1` | Network host interface to bind in SSE mode (`0.0.0.0` for all interfaces). |
| `MCP_PORT` / `--port` | `8000` | Network port to bind in SSE mode. |

---

## Running Standalone for Verification

### Standard local mode (stdio):
```bash
uv run --with "mcp<2" python tools/mcp/gecko_mcp/server.py
```

### Run automated verification client:
```bash
uv run --with "mcp<2" python tools/mcp/gecko_mcp/run_mcp_client.py
```
