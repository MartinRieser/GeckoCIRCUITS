---
title: API Reference
description: Core engine, REST/WebSocket API, MCP server, and Python SDK documentation
---

# API Reference

Programmatic interfaces for embedding, automating, and extending GeckoCIRCUITS.

## Available Interfaces

<div class="grid cards" markdown>

-   :material-engine:{ .lg .middle } **[Simulation Core (`gecko-simulation-core`)](core/index.md)**

    ---

    GUI-free Java 25 MNA solver, circuit model I/O, matrix stampers, and control calculators

-   :material-api:{ .lg .middle } **[REST & WebSocket API (`gecko-rest-api`)](rest-api.md)**

    ---

    Spring Boot 4.1.1 HTTP, SSE, and STOMP WebSocket API under `/gecko` (`Swagger UI` at `/gecko/swagger-ui.html`)

-   :material-robot:{ .lg .middle } **[MCP Server (`gecko-mcp`) & `geckopy`](../mcp.md)**

    ---

    16 JSON-RPC 2.0 stdio tools for LLM assistants plus the repository Python SDK (`geckopy`)

-   :material-archive:{ .lg .middle } **[Legacy RMI & GeckoSCRIPT](remote-interface.md)**

    ---

    Retirement notices for Classic Swing RMI (`GeckoRemoteInterface`) and GraalVM `GeckoSCRIPT`

</div>

## Quick Comparison

| Interface | Module / Location | Protocol | Best For |
|-----------|-------------------|----------|----------|
| **Core Java API** | `backend/gecko-simulation-core` | In-process JVM | Embedding solver in Java applications or tests |
| **REST + WebSocket API** | `backend/gecko-rest-api` | HTTP / SSE / STOMP WS | Web UI, CI pipelines, MATLAB, remote clients |
| **MCP + `geckopy` SDK** | `backend/gecko-mcp`, `geckopy/` | JSON-RPC 2.0 stdio | Python automation, SciPy sweeps, LLM coding agents |
| **NativeC FFM Blocks** | `gecko_c_block.h` | C ABI (`java.lang.foreign`) | Firmware-in-the-loop C/C++ shared libraries |

## Quick Examples

### Python via REST API (`requests`)

Start the REST server (`run-gecko.bat`, `./run-gecko.sh`, or `java -jar backend/gecko-rest-api/target/gecko-rest-api.jar`):

```python
import time
import requests

base = "http://localhost:8080/gecko/api/v1"
sim = requests.post(f"{base}/simulations", json={
    "circuitFile": "resources/projects/buck_sampled_50v_to_10v.ipes",
    "simulationTime": 0.01,
    "timeStep": 1e-6,
    "solverType": "backward-euler",
}).json()

while sim["status"] in ("PENDING", "RUNNING"):
    time.sleep(0.1)
    sim = requests.get(f"{base}/simulations/{sim['simulationId']}").json()

results = requests.get(f"{base}/simulations/{sim['simulationId']}/results").json()
```

### Python via `geckopy` (MCP SDK)

```python
import geckopy

ckt = geckopy.open("resources/projects/buck_sampled_50v_to_10v.ipes")
res = ckt.simulate(duration=0.01, dt=1e-6)
print(res.metrics)
```

### cURL

```bash
curl http://localhost:8080/gecko/api/health

curl -X POST http://localhost:8080/gecko/api/v1/simulations \
  -H "Content-Type: application/json" \
  -d '{"circuitFile": "tools/parity/circuits/rc-lowpass.ipes", "simulationTime": 0.01, "timeStep": 1e-5}'
```

## Related Documentation

- [REST API Reference](rest-api.md)
- [Core Module Overview](core/index.md)
- [Python Integration Tutorial](../tutorials/scripting/python.md)
- [MATLAB Integration Tutorial](../tutorials/scripting/matlab.md)
