---
title: FAQ
---

# Frequently Asked Questions

## General

### What is GeckoCIRCUITS?

GeckoCIRCUITS is an open-source power electronics circuit simulator written in Java 25 with a React/TypeScript schematic editor, Tauri 2 desktop shell, Spring Boot REST API, and Model Context Protocol (MCP) server. It supports multi-domain simulation across electrical, thermal, magnetic, mechanical, and control domains. Originally developed at ETH Zurich, it is used in education and research.

### What Java version do I need?

- **Desktop Installers:** Java 25 is **bundled** inside the desktop application (`jlink` runtime) — no separate Java install is required.
- **Running or Building from Source:** **JDK 25** or later (we recommend [Eclipse Temurin](https://adoptium.net/)).

### Which operating systems are supported?

GeckoCIRCUITS runs natively on **Windows**, **Linux**, and **macOS** via desktop installers or from source (`run-gecko.bat`, `run-gecko.ps1`, `./run-gecko.sh`).

### Is it really free?

Yes. GeckoCIRCUITS is dual-licensed under GPL v3 (free for open-source and academic use) and a commercial license for proprietary applications.

## Simulation

### What solver does GeckoCIRCUITS use?

GeckoCIRCUITS uses **Modified Nodal Analysis (MNA)** with fixed time-step solvers:

- **Backward Euler (BE)** — Default, unconditionally stable first-order method
- **Trapezoidal (TRZ)** — Second-order accuracy (can exhibit numerical ringing on stiff switching edges)
- **Gear-Shichman (GS)** — Variable-order implicit integration for stiff systems

### How do I choose the right time step?

Rule of thumb: the time step should be at least **100x smaller** than the switching period.

| Switching Frequency | Recommended Time Step |
|--------------------|-----------------------|
| 10 kHz | < 1 µs |
| 100 kHz | < 100 ns |
| 1 MHz | < 10 ns |

### Why is my simulation unstable?

Common causes:

1. **Time step too large** — Reduce it (see table above)
2. **Algebraic loops** — Add a small series resistance or inductance
3. **Ideal switches** — Add snubber circuits or non-zero on/off resistance
4. **Stiff system** — Try the Backward Euler solver instead of Trapezoidal

### Can I simulate thermal effects?

Yes. GeckoCIRCUITS supports coupled electro-thermal simulation:

1. Enable thermal nodes on semiconductor components
2. Define thermal resistance/capacitance networks (`Rth`, `Cth`)
3. The simulator solves electrical and thermal equations simultaneously

See [Thermal Simulation Tutorials](../tutorials/thermal/index.md).

### What's the maximum circuit size?

There is no hard limit, though practical limits depend on system RAM and time step count. For very long simulations or small time steps, increase the JVM heap (`-Xmx`) when running `gecko-rest-api.jar` directly.

## Integration

### Can I use GeckoCIRCUITS with MATLAB or Octave?

Yes, via the **REST API** (`http://localhost:8080/gecko/api/v1`) using `webwrite`/`webread` or HTTP requests. Legacy Java RMI (`GeckoRemoteInterface`) was retired with the Classic Swing UI.

See the [MATLAB Integration Tutorial](../tutorials/scripting/matlab.md).

### Can I use it with Python?

Yes, through:

- **`geckopy` SDK** — Built-in Python wrapper communicating with `gecko-mcp.jar` over stdio JSON-RPC 2.0
- **REST API** — `backend/gecko-rest-api` HTTP, SSE, and WebSocket endpoints
- **Headless CLI** — `gecko.core.GeckoHeadless` invoked via Spring Boot's `PropertiesLauncher`

See the [Python Integration Tutorial](../tutorials/scripting/python.md).

### Can I use it with Simulink?

Archival R2010a Simulink S-function files from ETH Zurich remain in `resources/tutorials/7xx_scripting_automation/703_simulink_cosimulation/` for historical reference against the retired Classic `classic-v1.0-final` codebase. For current versions, integrate via MATLAB HTTP calls to the REST API or `SCRIPT_BLOCK` / `NativeC` C/C++ blocks inside the circuit.

## File Formats

### What is an `.ipes` file?

The native GeckoCIRCUITS circuit file format. Modern `.ipes` files in this repository are **plain UTF-8 line-oriented text** describing components, coordinates, parameters, and wires; gzip-compressed `.ipes` / `.ipes.gz` files are also supported by the parser.

### Can I import SPICE netlists?

Not directly. GeckoCIRCUITS uses its own `.ipes` format. Rebuild the circuit in the GeckoCIRCUITS editor or synthesize it programmatically via `gecko_create_circuit` / `geckopy`.

### Can I export simulation data?

Yes:

- Export CSV waveforms directly from the **Scope** view in the editor
- Or call `POST /gecko/api/v1/simulations/{simulationId}/export` from the REST API

## Troubleshooting

### The application won't start

See [Troubleshooting](troubleshooting.md) for desktop launcher, Java 25, and simulation diagnostics.

### Where do I report bugs?

Open an issue on [GitHub](https://github.com/MartinRieser/GeckoCIRCUITS/issues).

### How do I get help?

- [GitHub Issues](https://github.com/MartinRieser/GeckoCIRCUITS/issues) — Bug reports and feature requests
- [GitHub Discussions](https://github.com/MartinRieser/GeckoCIRCUITS/discussions) — Questions and community help
- [Tutorials](../tutorials/index.md) — Step-by-step learning guides
