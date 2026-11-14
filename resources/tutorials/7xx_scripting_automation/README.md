# 7xx - Scripting & Automation

Programmatic control and external tool integration.

| Tutorial | Title | Difficulty | Status |
|----------|-------|------------|--------|
| [701](701_gecko_script_basics/) | GeckoSCRIPT Basics (Legacy Archive) | 2/3 | Legacy `.ipes` / docs |
| [702](702_matlab_integration/) | MATLAB Integration (Legacy Archive) | 2/3 | Legacy `.m` / `.scl` / `.ipes` |
| [703](703_simulink_cosimulation/) | Simulink Co-Simulation | 3/3 | Legacy S-Function files |
| [704](704_java_blocks/) | Java / `SCRIPT_BLOCK` Examples | 3/3 | Available |
| [705](705_api_integration/) | API Integration | 3/3 | Available |

## Learning Objectives

- Create custom Java/`SCRIPT_BLOCK` control logic
- Use the REST API and WebSockets for remote simulation control
- Automate parameter sweeps and analysis from Python (`geckopy`)
- Consult legacy GeckoSCRIPT, MATLAB, and Simulink reference archives

## Integration Options Overview

| Method | Best For | Documentation |
|--------|----------|---------------|
| Python (`geckopy`) | Parameter sweeps, optimization, data analysis | [Python SDK Guide](../../../docs/tutorials/scripting/python.md) |
| REST API / WebSocket | Web UI, headless automation, remote streaming | [REST API Reference](../../../docs/api/rest-api.md) |
| Java / `SCRIPT_BLOCK` | Inline per-step custom control logic | [Java Control Blocks Guide](../../../docs/tutorials/scripting/java-blocks.md) |
| Native C (`NativeC`) | Compiled C shared libraries | [Native C Blocks Guide](../../../docs/native-c-blocks.md) |
| Legacy Archives (`701`–`703`) | Historical GeckoSCRIPT, MATLAB RMI, and Simulink files | [Scripting Overview](../../../docs/tutorials/scripting/index.md) |

> [!NOTE]
> The legacy Swing `GeckoSCRIPT` window and RMI `GeckoRemoteInterface` (`port 1099`) are retired in modern GeckoCIRCUITS; see the `classic-v1.0-final` tag for the archived Swing codebase. Use the REST API, `geckopy`, `SCRIPT_BLOCK`, or `NativeC` for active automation.

## Contents

### 701 - Legacy GeckoSCRIPT Material
- `GeckoSCRIPT.pdf` — Historical scripting manual
- `GeckoSCRIPT.ipes`, `buck_control.ipes`, `buck_control_tuning.m` — Historical circuit and MATLAB files

### 702 - Legacy MATLAB / GeckoSCRIPT Examples
- `GeckoSCRIPT_ example_matlab/` — Historical MATLAB frequency sweep (`outputlegfrequencysweep.m`), semiconductor loss curves (`Diodeloss.scl`, `IGBTloss.scl`), and `outputstagemodel.ipes`
- For modern MATLAB interop via the REST API, see the [MATLAB Interop Guide](../../../docs/tutorials/scripting/matlab.md)

### 703 - Simulink Co-Simulation
- `GeckoCIRCUITS_simulink_tutorial.pdf` — Legacy S-function tutorial guide
- `Gecko_VR1.mdl` — Legacy Simulink model
- `cj.h`, `gecko-research_simulink_tutorial.*` — Historical co-simulation files

### 704 - Java / `SCRIPT_BLOCK` Examples
- `demo_JAVA_Block.ipes` — Control block example
- `JavaBlockPMSM.ipes` — PMSM control example

### 705 - API Integration
- `websocket-client.html` — SockJS/STOMP WebSocket progress test client (`/gecko/ws`)
- `ExternalSignalSource.ipes`, `signalsourceExternal.ipes` — External signal source examples

## Quick Start: Modern Automation

### Python (`geckopy`)
```python
import geckopy

ckt = geckopy.open("examples/buck.ipes")
for r_load in (5.0, 10.0, 15.0):
    ckt.set_parameter("R_load", "resistance", r_load)
    res = ckt.simulate(duration=0.01, dt=1e-6, solver="backward-euler")
    print(f"R={r_load} Ohm -> metrics={res.metrics}")
```

### REST API (`curl`)
Check status and inspect endpoints at `http://localhost:8080/gecko/swagger-ui.html` (see [REST API Reference](../../../docs/api/rest-api.md)).

## Use Case Selection Guide

| Task | Recommended Tool |
|------|------------------|
| Custom cycle-by-cycle control | `SCRIPT_BLOCK` (or Java `AbstractControlCalculatable`) |
| Optimization / ML / sweeps | Python (`geckopy`) |
| Web dashboard / remote CI | REST API (`/gecko/api/v1`) |
| Legacy Swing scripts | Archived `classic-v1.0-final` tag |

## Related Tutorials

- [104 - Running Simulations](../1xx_getting_started/104_running_simulations/) - Solver settings and batch simulation basics
- [Scripting & Automation Docs](../../../docs/tutorials/scripting/index.md) - Modern automation hub
