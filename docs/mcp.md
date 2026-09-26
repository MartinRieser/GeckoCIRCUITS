# GeckoCIRCUITS Model Context Protocol (MCP) Interface

GeckoCIRCUITS ships a native [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that enables AI and LLM agents — such as Google Antigravity, Claude Desktop, Cursor, and GitHub Copilot — to autonomously design, inspect, validate, simulate, and refine power electronics circuits without opening a GUI or inspecting simulator source code. All knowledge an LLM needs (components, parameter slots, pinouts, geometry, wiring rules, multi-domain configuration, worked examples) is delivered through the catalog tool and the MCP resources.

---

## Server Implementations

| Attribute | Bundled Production Server (Java) | Development Server (Python) |
|---|---|---|
| **Location** | `backend/gecko-mcp` | `tools/mcp/gecko_mcp` |
| **Packaging** | Shaded into `desktop/app/engine/gecko-mcp.jar` | Standalone Python module (`uv run`) |
| **Requirements** | Bundled Java 25 runtime (zero external dependencies) | Python 3 + `uv`, JDK 25 for engine |
| **Transports** | `stdio` | `stdio`, `sse`, `streamable-http` |
| **Simulation** | In-process headless simulation engine | Subprocess engine call |
| **Tools Exposed** | **14 tools** (full authoring, multi-domain simulation, native control blocks) | 10 tools (template & simulation tools, legacy) |
| **Resources** | 7 (catalog, script language, control blocks, workflow, multi-domain, topologies, examples) | 3 |

The production Java server (`launch-mcp.py` / `gecko-mcp.jar`) is recommended for all LLM workflows: simulations run in-process, the full `SimulationConfig` surface is exposed (parameter overrides, matrix solver selection, adaptive stepping, thermal and magnetic domains), and circuits are authored through the same `CircuitModel` the engine consumes.

---

## Client Configuration

### 1. Google Antigravity
The server is pre-configured in `.agents/mcp_config.json` for workspace use:
```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "python",
      "args": [
        "scripts/desktop/launch-mcp.py"
      ]
    }
  }
}
```
Or in global Antigravity config (`~/.gemini/config/mcp_config.json`):
```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "python",
      "args": [
        "C:/path/to/GeckoCIRCUITS/scripts/desktop/launch-mcp.py"
      ]
    }
  }
}
```

### 2. Claude Desktop
Add to `claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "python",
      "args": [
        "C:/path/to/GeckoCIRCUITS/scripts/desktop/launch-mcp.py"
      ]
    }
  }
}
```

### 3. Cursor
Add to `.cursor/mcp.json`:
```json
{
  "mcpServers": {
    "gecko-circuits": {
      "command": "python",
      "args": [
        "scripts/desktop/launch-mcp.py"
      ]
    }
  }
}
```

### 4. GitHub Copilot (VS Code Agent Mode)
Add to `.vscode/settings.json`:
```json
{
  "github.copilot.chat.mcp.servers": {
    "gecko-circuits": {
      "command": "python",
      "args": [
        "${workspaceFolder}/scripts/desktop/launch-mcp.py"
      ]
    }
  }
}
```

### 5. Desktop Application (Installed Package)
Run the launcher generator in your install directory:
```sh
python scripts/desktop/write-mcp-launchers.py --dest <install-dir>
```
It generates `gecko-mcp.bat` (Windows) or `gecko-mcp.sh` (Linux/macOS) which uses the bundled JRE directly. Point your MCP client to the generated `.bat` or `.sh`.

---

## Complete Tools Reference (14 Tools)

| Tool | Category | Description |
|---|---|---|
| `gecko_server_status` | System | Engine version, solvers, catalog size, workspace root. |
| `gecko_catalog` | Discovery | Authoritative component catalog: power, control and thermal components with type numbers, pins, parameter slots, units and defaults, plus orientation codes, grid/wiring rules, signal naming, override syntax and solver options. |
| `gecko_create_circuit` | Authoring | Synthesizes `.ipes` projects from a JSON netlist: power, thermal and control components (probes, gates, native control blocks, script blocks), automatic layout and wire routing. |
| `gecko_validate_circuit` | DRC & Diagnostics | `level: "lint"` regex design-rule check; `level: "compile"` additionally builds the real MNA netlist and control graph and reports genuine errors (unknown labels, failed couplings, build warnings). |
| `gecko_inspect_circuit` | Introspection | Structured parse of a `.ipes` file: simulation settings, per-domain components with semantic + raw parameters, control blocks with script sources, connections. |
| `gecko_patch_component` | Tuning | Edit component parameters by catalog name (`{"resistance": 25.0}`) or raw slot (`{"param0": 25.0}`); rewrites the file losslessly. |
| `gecko_set_script_code` | Controller | Updates the script source, static variables and static code of a script block. |
| `gecko_list_signals` | Discovery | Enumerates all recordable channels of a circuit (probe outputs, node labels, loss channels, `Tj_*`, `Phi_*`) without running it. |
| `gecko_simulate` | Execution | Runs the headless engine with the full simulation surface: signals, parameter overrides, solver, matrix solver, adaptive stepping, semiconductor model, thermal and magnetic domain configuration. Returns metadata (loss totals, max junction temperature, peak flux). |
| `gecko_get_waveforms` | Waveforms | Same options as simulate; returns downsampled time series plus per-signal statistics (min/max/mean/RMS/ripple). |
| `gecko_measure_metrics` | Analytics | DC average, peak-to-peak ripple, RMS, power factor, power and efficiency figures of merit. |
| `gecko_setup_pfc_project` | Parametric | Generates a 2-phase interleaved boost PFC project with MCU script controller and dynamic load step. |
| `gecko_setup_llc_project` | Parametric | Generates a half-bridge resonant LLC converter with ZVS snubber and tank analytics (f0, Z0, Q, k). |
| `gecko_tune_pfc` | Evaluation | Simulates a PFC project and evaluates DC regulation and ripple against a target (single evaluation; use `parameter_overrides` on the simulate family for sweeps). |

---

## Resources (Self-Contained Knowledge)

| URI | Content |
|---|---|
| `gecko://catalog/components` | The full catalog JSON (same as `gecko_catalog`). |
| `gecko://reference/script-blocks` | ScriptBlockCalculator language manual (variables, functions, syntax, safety rules). |
| `gecko://reference/control-blocks` | Native control blocks (typ 1000–1016): parameter slots, terminal geometry, wiring rules, canonical regulated-buck pattern. |
| `gecko://reference/workflow` | End-to-end authoring workflow with complete JSON cookbook payloads (RLC divider, closed-loop buck, thermal rectifier). |
| `gecko://reference/multi-domain` | Electro-thermal feedback, in-file thermal networks, magnetic reluctance networks, loss channels. |
| `gecko://reference/topologies` | Design equations for Vienna rectifier, buck and LLC. |
| `gecko://examples/list` | Index of example `.ipes` projects in the workspace. |

---

## Key Tool Schemas

### `gecko_create_circuit`
```json
{
  "output_path": "resources/projects/my_buck.ipes",
  "simulation": {"duration": 0.005, "dt": 1e-6, "solver": 0},
  "components": [
    {"name": "V_IN", "type": "DC_VOLTAGE", "nodes": ["vin", "0"], "parameters": {"voltage": 12.0}},
    {"name": "S1",   "type": "IDEAL_SWITCH", "nodes": ["vin", "sw"], "parameters": {"r_on": 0.01}},
    {"name": "D1",   "type": "DIODE", "nodes": ["0", "sw"]},
    {"name": "L1",   "type": "INDUCTOR", "nodes": ["sw", "vout"], "parameters": {"inductance": 1e-4}},
    {"name": "C1",   "type": "CAPACITOR", "nodes": ["vout", "0"], "parameters": {"capacitance": 1e-4}},
    {"name": "RL",   "type": "RESISTOR", "nodes": ["vout", "0"], "parameters": {"resistance": 6.0}}
  ],
  "control": {
    "probes":  [{"name": "VOLT", "type": "VOLTMETER", "target_component": "RL", "signal_name": "vout"}],
    "blocks":  [
      {"type": "CONSTANT",   "name": "REF", "parameters": {"value": 6.0}, "output_label": "vref"},
      {"type": "COMPARATOR", "name": "CMP", "output_label": "pwm"}
    ],
    "wires":   [
      {"from": "REF",  "to": "CMP", "to_input": 0},
      {"from": "VOLT", "to": "CMP", "to_input": 1}
    ],
    "gates":   [{"name": "GATE", "target_switch": "S1", "in_signal": "pwm"}],
    "script_blocks": [
      {"name": "CTRL_MCU", "in_signals": ["vout"], "out_signals": ["pwm"],
       "static_variables": "double integ = 0.0;",
       "code": "double err = 6.0 - xIN[0]; integ += err * dt; yOUT[0] = (0.004 * err + 0.4 * integ > 0.8) ? 1.0 : 0.0; return yOUT;"}
    ]
  },
  "thermal_components": [
    {"name": "RTH_HS", "type": "TH_RTH", "nodes": ["t_j", "t_amb"], "parameters": {"r_th": 2.0}},
    {"name": "CTH_J",  "type": "TH_CTH", "nodes": ["t_j", "t_amb"], "parameters": {"c_th": 0.005, "t_init": 25.0}}
  ]
}
```
Components without documented parameter names accept `parameters_raw` (raw slot vector).

### `gecko_simulate`
```json
{
  "circuit_path": "resources/projects/my_buck.ipes",
  "duration": 0.01, "dt": 1e-6, "solver": "be",
  "signals": ["vout", "pwm"],
  "parameter_overrides": {"RL.resistance": 4.0},
  "matrix_solver": "auto",
  "adaptive": {"enabled": false, "relative_tolerance": 1e-3},
  "semiconductor_model": "classic_pwl",
  "thermal": {
    "ambient_temperature": 25.0,
    "couplings": [
      {"device": "D1", "kind": "diode",
       "model": {"kind": "foster", "r_th": [1.5, 0.8], "tau": [0.002, 0.05]},
       "forward_voltage_slope": -0.002}
    ]
  }
}
```
Result: `status`, `total_steps`, `signal_names`, `execution_time_ms`, `metadata` (solver, matrix_solver, losses, `maxJunctionTemperature`, `peakFlux`), `warnings`.

---

## Scripting Reference

See the `gecko://reference/script-blocks` resource — it is the authoritative manual for the `ScriptBlockCalculator` language (input/output arrays `xIN[]`/`yOUT[]`, simulation variables `t`/`dt`, persistent `static_variables`, math functions, and divide-guard/windup safety rules).

## Security & Concurrency

- The server is a plain stdio process: it only touches files under the resolved workspace root (`GECKO_HOME` environment variable or the current working directory).
- One simulation runs at a time per server process; the engine rejects concurrent runs with a clear error.
- Script blocks are interpreted by the built-in sandboxed calculator; native C blocks (`typ 88`) load only the library path explicitly configured on the block.
