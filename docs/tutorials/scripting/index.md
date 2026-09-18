---
title: Scripting & Automation
description: Programmatic simulation control, scripting, and external tool integration
---

# Scripting & Automation

Programmatic control and external tool integration with GeckoCIRCUITS.

## Available Tutorials

| Tutorial | Title | Difficulty | Description |
|----------|-------|------------|-------------|
| [701](geckoscript.md) | [GeckoSCRIPT Basics](geckoscript.md) | Intermediate | Built-in JavaScript scripting for parameter sweeps and optimization |
| [702](matlab.md) | [MATLAB Integration](matlab.md) | Intermediate | Interfacing MATLAB with GeckoCIRCUITS for automated analysis |
| [703](python.md) | [Python Integration](python.md) | Intermediate | Controlling simulations via Python, NumPy, SciPy, and REST API |
| [704](java-blocks.md) | [Java Blocks](java-blocks.md) | Advanced | Implementing custom compiled algorithmic control blocks |

## Learning Objectives

- Automate batch simulations with GeckoSCRIPT
- Integrate with MATLAB for parameter optimization
- Control simulations and analyze output waveforms in Python
- Create custom Java control blocks for specialized algorithms
- Leverage the modern REST API and MCP server for remote simulation

## Integration Options Overview

| Method | Best For | Complexity | Interface |
|--------|----------|------------|-----------|
| **GeckoSCRIPT** | Quick in-editor sweeps | Low | Built-in engine |
| **Python** | Data science, machine learning, optimization | Medium | REST API / CLI |
| **MATLAB** | Control engineering, matrix analysis | Medium | REST API / Scripting |
| **Java Blocks** | High-performance custom components | Advanced | Java / JVM |
| **REST API** | CI/CD pipelines, web apps, microservices | Medium | HTTP / WebSocket |
| **MCP Server** | LLM assistants (Claude, Cursor, etc.) | Low | JSON-RPC standard |

## Quick Start: Python Automation Example

```python
import requests
import numpy as np

# Trigger simulation via REST API
response = requests.post("http://localhost:8080/api/v1/simulations/run", json={
    "circuitPath": "circuits/buck.ipes",
    "parameters": {"dutyCycle": 0.5}
})

data = response.json()
print(f"Simulation completed with status: {data['status']}")
```

## Related Resources

- [Running Simulations](../../getting-started/running-simulations.md) — Solver configurations
- [Analysis Tools](../../getting-started/analysis-tools.md) — Waveform post-processing
- [REST API Reference](../../api/rest-api.md) — Comprehensive API documentation
- [MCP Interface](../../mcp.md) — Model Context Protocol tools
