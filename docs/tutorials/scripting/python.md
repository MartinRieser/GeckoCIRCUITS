---
title: Python Integration
description: Automate GeckoCIRCUITS from Python via geckopy (MCP SDK), the REST API, or headless CLI
---

# Python Integration

Automate GeckoCIRCUITS simulations from Python and integrate with NumPy, SciPy, Pandas, and Matplotlib using the built-in **`geckopy` SDK**, the **REST API**, or the **headless CLI**.

## Integration Methods

| Method | Best For | Transport |
|--------|----------|-----------|
| **`geckopy` SDK** (recommended) | In-process scripts, parameter sweeps, SciPy optimization, DRC validation | `gecko-mcp.jar` over JSON-RPC 2.0 stdio |
| **REST API (`requests`)** | Remote servers, async/batch sweeps, WebSocket/SSE progress streaming | HTTP (`http://localhost:8080/gecko/api/v1`) |
| **Headless CLI** | Shell/CI batch CSV generation without a long-lived server | `gecko.core.GeckoHeadless` via `PropertiesLauncher` |

---

## Method 1: Using `geckopy` (Recommended)

The repository includes `geckopy/` (`Circuit`, `SimulationResult`, `Metrics`, `Waveforms`, `ValidationReport`), which communicates directly with `backend/gecko-mcp/target/gecko-mcp.jar` (or the bundled desktop JAR).

### Build the MCP Server JAR (if running from source)

```bash
mvn -pl backend/gecko-mcp -am package -DskipTests
```

### Quick Start

```python
import geckopy

# Open an existing circuit file (.ipes)
ckt = geckopy.open("resources/projects/buck_sampled_50v_to_10v.ipes")

# Pre-flight Design Rule Check (DRC)
report = ckt.validate()
if not report.is_valid:
    print("Validation errors:", report.errors)

# Modify parameters and run headless simulation
ckt.set_parameter("R_load", "resistance", 12.0)
result = ckt.simulate(duration=0.01, dt=1e-6, solver="backward-euler")

print("Metrics:", result.metrics)
waveforms = ckt.get_waveforms(duration=0.01, dt=1e-6, max_points=2000)
```

### Parameter Sweep & Optimization with NumPy/SciPy

```python
import numpy as np
from scipy.optimize import minimize_scalar
import geckopy

ckt = geckopy.open("resources/projects/buck_sampled_50v_to_10v.ipes")

def objective(resistance: float) -> float:
    ckt.set_parameter("R_load", "resistance", float(resistance))
    metrics = ckt.measure_metrics()
    v_out = metrics.get("v_out", {}).get("steady_state_dc", 0.0)
    return abs(v_out - 10.0)

opt = minimize_scalar(objective, bounds=(2.0, 20.0), method="bounded")
print(f"Best R_load = {opt.x:.2f} Ohm, error = {opt.fun:.4f} V")
```

---

## Method 2: REST API (`requests`)

Start the REST server (`run-gecko.bat`, `./run-gecko.sh`, or `java -jar backend/gecko-rest-api/target/gecko-rest-api.jar`). All endpoints are served under the `/gecko` context path.

```python
import time
import requests

BASE_URL = "http://localhost:8080/gecko/api/v1"

# 1. Submit an asynchronous simulation
resp = requests.post(f"{BASE_URL}/simulations", json={
    "circuitFile": "resources/projects/buck_sampled_50v_to_10v.ipes",
    "simulationTime": 0.01,
    "timeStep": 1e-6,
    "solverType": "backward-euler",
    "parameters": {"R_load.resistance": 10.0},
})
resp.raise_for_status()
sim_id = resp.json()["simulationId"]

# 2. Poll status until completion
while True:
    status = requests.get(f"{BASE_URL}/simulations/{sim_id}").json()
    if status["status"] in ("COMPLETED", "FAILED", "CANCELLED"):
        break
    time.sleep(0.1)

# 3. Fetch results and signal analysis
results = requests.get(f"{BASE_URL}/simulations/{sim_id}/results").json()
metrics = requests.post(f"{BASE_URL}/analysis/characteristics", json={
    "simulationId": sim_id,
    "signalName": "V_out",
}).json()
print("Characteristics:", metrics)
```

For multi-point sweeps in one call, see `POST /gecko/api/v1/simulations/batch` in the [REST API Reference](../../api/rest-api.md).

---

## Method 3: Headless CLI Subprocess

Run single `.ipes` simulations directly to CSV using Spring Boot's `PropertiesLauncher`:

```python
import subprocess

cmd = [
    "java",
    "-Dloader.main=gecko.core.GeckoHeadless",
    "-cp", "backend/gecko-rest-api/target/gecko-rest-api.jar",
    "org.springframework.boot.loader.launch.PropertiesLauncher",
    "--circuit", "tools/parity/circuits/rc-lowpass.ipes",
    "--output", "rc_out.csv",
    "--quiet",
]
subprocess.run(cmd, check=True)
```

## Notes on `.ipes` File Format

`.ipes` files are plain-text line-oriented UTF-8 circuit files (`<ElementLK>`, `<ElementCONTROL>`, `tDURATION`, etc.). Some historical or exported files may also be gzip-compressed (`.ipes` or `.ipes.gz`). Both `geckopy` and `CircuitFileParser` handle plain-text and gzip-compressed `.ipes` files automatically; prefer `geckopy` / REST endpoints over hand-editing raw `.ipes` files so DRC rules and terminal wiring stay valid.

## Related Guides

- [MCP Interface](../../mcp.md) — MCP tools powering `geckopy`
- [REST API Reference](../../api/rest-api.md) — Full HTTP, batch, SSE, and WebSocket endpoints
- [MATLAB Integration](matlab.md) — Using the REST API from MATLAB/Octave
- [Custom Control Blocks & NativeC](java-blocks.md) — Writing C/C++ or Java control algorithms
