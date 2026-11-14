---
title: Scripting & Automation
description: Programmatic simulation control, in-circuit script blocks, and external tool integration
---

# Scripting & Automation

Programmatic control, firmware-in-the-loop simulation, and external tool integration with GeckoCIRCUITS.

## Available Guides

| Guide | Title | Difficulty | Description |
|-------|-------|------------|-------------|
| [701](geckoscript.md) | [Script Blocks & Legacy GeckoSCRIPT](geckoscript.md) | Intermediate | Inline control `SCRIPT_BLOCK` (typ 61) and archived GeckoSCRIPT notes |
| [702](matlab.md) | [MATLAB Integration](matlab.md) | Intermediate | Interfacing MATLAB/Octave with GeckoCIRCUITS via the REST API |
| [703](python.md) | [Python Integration](python.md) | Intermediate | Controlling simulations with Python (`geckopy` MCP SDK and REST API) |
| [704](java-blocks.md) | [Custom Control Blocks & NativeC](java-blocks.md) | Advanced | Legacy Classic Java blocks vs modern `NativeC` C/C++ FFM blocks and core calculators |

## Integration Options Overview

| Method | Best For | Status | Interface |
|--------|----------|--------|-----------|
| **Script Block (`SCRIPT_BLOCK`)** | Inline control logic evaluated every time step | Active | `ScriptBlockCalculator` (pure Java) |
| **NativeC Block (`NATIVE_C`)** | Microcontroller C/C++ firmware-in-the-loop | Active | Java FFM (`gecko_c_block.h`) |
| **Python (`geckopy` / REST)** | Sweeps, SciPy optimization, post-processing | Active | MCP stdio (`gecko-mcp.jar`) or REST API |
| **MATLAB** | Control engineering, matrix analysis | Active | HTTP (`webwrite`/`webread` → REST API) |
| **REST API** | CI/CD pipelines, web apps, batch sweeps | Active | HTTP / SSE / WebSocket (`/gecko/api/v1`) |
| **MCP Server** | AI coding assistants and local Python SDK | Active | JSON-RPC 2.0 stdio (`gecko-mcp.jar`) |
| **GeckoSCRIPT / Java RMI / Java Blocks** | Standalone GraalVM scripts, RMI, Swing plugins | Retired | Archived on tag `classic-v1.0-final` |

## Quick Start: Python Automation Example

### Option A: Using `geckopy` (Repository Python SDK)

```python
import geckopy

ckt = geckopy.open("resources/projects/buck_sampled_50v_to_10v.ipes")
ckt.set_parameter("R_load", "resistance", 12.0)
res = ckt.simulate(duration=0.01, dt=1e-6)
print(res.metrics)
```

### Option B: Using the REST API (`requests`)

Start the server (`run-gecko.bat` or `./run-gecko.sh`) so it listens on `http://localhost:8080/gecko`:

```python
import time
import requests

base = "http://localhost:8080/gecko/api/v1"
resp = requests.post(f"{base}/simulations", json={
    "circuitFile": "resources/projects/buck_sampled_50v_to_10v.ipes",
    "simulationTime": 0.01,
    "timeStep": 1e-6,
    "solverType": "backward-euler",
})
resp.raise_for_status()
sim_id = resp.json()["simulationId"]

while True:
    status = requests.get(f"{base}/simulations/{sim_id}").json()
    if status["status"] in ("COMPLETED", "FAILED", "CANCELLED"):
        print(f"Simulation finished: {status['status']}")
        break
    time.sleep(0.1)
```

## Related Resources

- [Running Simulations](../../getting-started/running-simulations.md) — Solver configurations
- [Analysis Tools](../../getting-started/analysis-tools.md) — Waveform post-processing
- [NativeC C/C++ Blocks](../../native-c-blocks.md) — Firmware-in-the-loop C/C++ shared libraries
- [REST API Reference](../../api/rest-api.md) — Comprehensive HTTP & WebSocket API documentation
- [MCP Interface](../../mcp.md) — Model Context Protocol tools
