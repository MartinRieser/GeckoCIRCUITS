---
title: Script Blocks & Legacy GeckoSCRIPT
description: Inline SCRIPT_BLOCK control blocks and notes on legacy GeckoSCRIPT
---

# 701 - Script Blocks & Legacy GeckoSCRIPT

GeckoCIRCUITS separates **in-circuit custom scripting** (executed every simulation time step inside a circuit) from **external automation**:

- **In-circuit control logic**: Use the **Script Block** (`SCRIPT_BLOCK`, component `typ 61`), supported natively in the desktop editor, REST API, MCP server (`gecko_set_script_code`), and headless simulation engine (`ScriptBlockCalculator`) via a pure-Java interpreter without GraalVM dependencies.
- **External parameter sweeps & batch runs**: Use the Python SDK ([`geckopy`](python.md)) or the [REST API](../../api/rest-api.md).

!!! warning "Legacy GeckoSCRIPT (Retired in v1.0+)"
    Standalone **GeckoSCRIPT** (`Tools > GeckoSCRIPT`, GraalVM JavaScript macros with `runSimulation()` / `setParameter()` and CLI `--script`) was part of the Classic Swing GUI, which is archived on git tag `classic-v1.0-final`. Use **Python (`geckopy`)** or the **REST API** for external automation in current versions.

---

## Using `SCRIPT_BLOCK` (`typ 61`)

A **Script Block** evaluates custom expressions at every simulation time step using wired control inputs (`xIN[0]`, `xIN[1]`, ...) and drives output signals (`yOUT[0]`, ...).

### Inspecting or Updating a Script Block via Python (`geckopy`)

```python
import geckopy

ckt = geckopy.open("resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.ipes")
ckt.set_script_code(
    block_name="SCRIPT.1",
    source_code="yOUT[0] = (xIN[0] > 0.5) ? 1.0 : 0.0;",
)
report = ckt.validate()
assert report.is_valid
res = ckt.simulate()
```

You can inspect or patch `SCRIPT_BLOCK` code from MCP assistants via `gecko_set_script_code` and `gecko_validate_circuit` (see [MCP Interface](../../mcp.md)).

### Headless CLI Batch Execution

To run a circuit containing script blocks directly from the command line without opening the GUI:

```bash
java -Dloader.main=gecko.core.GeckoHeadless \
     -cp backend/gecko-rest-api/target/gecko-rest-api.jar \
     org.springframework.boot.loader.launch.PropertiesLauncher \
     --circuit resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/buck_control.ipes \
     --output results.csv --quiet
```

---

## Historical GeckoSCRIPT Notes (`classic-v1.0-final`)

For users opening historical tutorial archives (`resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.pdf`), Classic Swing GeckoSCRIPT exposed JavaScript helpers such as `setParameter()`, `runSimulation()`, `getMeasurement()`, `setSimulationTime()`, and `setTimeStep()`. In modern releases, equivalent sweeps are performed through Python `for` loops calling `geckopy` or `POST /gecko/api/v1/simulations/batch`.

## Reference Files

- `resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.ipes`
- `resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/buck_control.ipes`
- `resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.pdf`

## Related Guides

- [Python Integration](python.md) — Recommended replacement for parameter sweeps and optimization
- [MATLAB Integration](matlab.md) — HTTP sweeps via REST API
- [NativeC C/C++ Blocks](../../native-c-blocks.md) — Compiled C/C++ firmware control blocks
