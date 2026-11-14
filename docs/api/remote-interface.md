---
title: Remote Interface (Legacy RMI — Retired)
description: Historical MATLAB RMI reference and modern REST API / MCP alternatives
---

# Remote Interface (Legacy RMI — Retired)

!!! warning "Retired Feature"
    The Java RMI remote interface (`GeckoRemoteInterface`, `--remote`, `--mmf`, `GeckoCustomMMF`) belonged to the retired Classic Swing application (archived at git tag `classic-v1.0-final`). It is **not** included in `gecko-rest-api`, `gecko-simulation-core`, or `gecko-mcp`.

For remote programmatic control from MATLAB, Octave, Python, or external tools in current GeckoCIRCUITS versions, use the **REST API** (`http://localhost:8080/gecko/api/v1/...`) or the **MCP stdio interface** (`gecko-mcp.jar`). See:

- [REST API Reference](rest-api.md)
- [MATLAB Integration Tutorial](../tutorials/scripting/matlab.md)
- [Python Integration Tutorial](../tutorials/scripting/python.md)
- [MCP Interface](../mcp.md)

---

## Modern Replacement Example (MATLAB via HTTP)

Start the REST server (`run-gecko.bat` / `./run-gecko.sh` or `java -jar backend/gecko-rest-api/target/gecko-rest-api.jar`), then call `/gecko/api/v1/simulations` using MATLAB's built-in `webwrite` and `webread`:

```matlab
baseUrl = 'http://localhost:8080/gecko/api/v1';

req = struct( ...
    'circuitFile', 'resources/projects/buck_sampled_50v_to_10v.ipes', ...
    'simulationTime', 0.01, ...
    'timeStep', 1e-6, ...
    'solverType', 'backward-euler' ...
);
opts = weboptions('MediaType', 'application/json');
sim = webwrite([baseUrl '/simulations'], req, opts);

% Poll until finished
status = sim.status;
while strcmp(status, 'PENDING') || strcmp(status, 'RUNNING')
    pause(0.1);
    sim = webread([baseUrl '/simulations/' sim.simulationId]);
    status = sim.status;
end

results = webread([baseUrl '/simulations/' sim.simulationId '/results']);
```

---

## Historical RMI Reference (Classic `classic-v1.0-final` Only)

If you specifically check out the archival tag `classic-v1.0-final`, `GeckoRemoteInterface` exposed RMI control on default port `1099` with methods including `openFile`, `saveFile`, `setParameter`, `getParameter`, `startSimulation`, `stopSimulation`, `setSimulationTime`, `setTimeStep`, `getMeasurement`, and `getSignalData`, plus memory-mapped transfers via `GeckoCustomMMF`.

## See Also

- [REST API Reference](rest-api.md) — Current HTTP / SSE / WebSocket interface
- [Python Integration](../tutorials/scripting/python.md) — `geckopy` SDK & REST examples
- [MCP Interface](../mcp.md) — Native JSON-RPC 2.0 server
