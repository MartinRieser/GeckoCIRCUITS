---
title: GeckoSCRIPT Reference
description: Control scripting options in modern and classic GeckoCIRCUITS
---

# GeckoSCRIPT Reference

!!! warning "Legacy Classic UI Feature"
    Standalone **GeckoSCRIPT** (`Tools > GeckoSCRIPT`, GraalVM JavaScript with functions like `runSimulation()` / `setParameter()`) belonged to the retired Classic Swing application (archived on git tag `classic-v1.0-final`).

In current GeckoCIRCUITS releases, use one of these automation interfaces instead:

| Interface | Best For | Documentation |
|-----------|----------|---------------|
| **Script Block (`SCRIPT_BLOCK`, typ 61)** | Inline per-step control expressions in the schematic and headless engine (`ScriptBlockCalculator`) | [Scripting Overview](../tutorials/scripting/index.md) |
| **NativeC Block (`NATIVE_C`, typ 88)** | Loading compiled C/C++ firmware shared libraries via Java FFM (`gecko_c_block.h`) | [NativeC C/C++ Blocks](../native-c-blocks.md) |
| **Python SDK (`geckopy`) & MCP** | Headless sweeps, metric computation, and LLM/JSON-RPC automation via `gecko-mcp.jar` | [MCP Interface](../mcp.md), [Python Tutorial](../tutorials/scripting/python.md) |
| **REST API** | HTTP/SSE/WebSocket automation and batch sweeps (`gecko-rest-api.jar`) | [REST API Reference](rest-api.md) |

---

## Historical Classic Reference (Archived `classic-v1.0-final`)

In the archived Classic Swing UI, GeckoSCRIPT used JavaScript (ES5+) via GraalVM with helper functions bound to the open Swing worksheet:

- **Simulation control**: `runSimulation()`, `stopSimulation()`, `setSimulationTime(t)`, `setTimeStep(dt)`
- **Component parameters**: `setParameter(comp, param, val)`, `getParameter(comp, param)`
- **Scope measurements**: `getMeasurement(scope, key)`
- **File & console helpers**: `openFile(path)`, `saveFile(path)`, `print(msg)`

For active automation workflows in the current headless engine, prefer the Python SDK (`geckopy`) or REST API examples in [Python Integration](../tutorials/scripting/python.md) and [REST API Reference](rest-api.md).

## Tutorial Files

Historical scripting tutorial `.ipes` models remain in the repository under:

- `resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.ipes`
- `resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.pdf`

## See Also

- [Scripting & Automation Overview](../tutorials/scripting/index.md)
- [REST API Reference](rest-api.md)
- [MCP Interface](../mcp.md)
