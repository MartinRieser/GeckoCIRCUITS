---
title: Quick Start
description: Run your first simulation in 5 minutes
---

# Quick Start

Get from install to your first waveform in five minutes.

## 1. Launch GeckoCIRCUITS

Start the **GeckoCIRCUITS** desktop app (see [Installation](installation.md)).
The editor opens after a few seconds — the bundled simulation engine boots in
the background.

!!! tip "No install? Use the web editor"
    From a repository checkout, `run-web-editor.bat|.sh` starts the engine and
    opens the same editor in your browser at `http://localhost:8080`.

## 2. Open an Example Circuit

Open the **Examples** menu in the top bar and pick **DC-DC Buck Converter**.
A complete buck converter loads onto the schematic:

- **MOSFET switch** — controlled by a PWM signal
- **Diode** — freewheeling path for inductor current
- **Inductor (L)** and **capacitor (C)** — output filter
- **Resistive load (R)**
- A **scope** watching the output

Click any component to inspect and edit its parameters in the **properties
panel** on the right.

## 3. Run the Simulation

Open the **Simulation Settings** panel on the right sidebar and click the
**▶ Run Simulation** button.

The scope view switches to the simulation tab and shows live progress, then
the waveforms:

- **Output voltage** settling to the target value
- **Inductor current** with triangular ripple
- **Switch node** toggling between input voltage and ground

## 4. Explore the Scope

The scope view is a full instrument:

| Action | How |
|--------|-----|
| Zoom in/out | Mouse wheel (zooms around the cursor) |
| Pan | Left-click and drag |
| Fit whole simulation | ⟲ button in the scope toolbar |
| Measure Δt / frequency | Click to place cursors **A** and **B** |
| Harmonics / THD | **FFT** button (analyzes the visible window) |
| Export data | **CSV export** in the results drawer |

## 5. Modify the Circuit

Experiment — this is where the tool pays off:

1. Select the **switch** and change its switching frequency in the
   properties panel.
2. Re-run and compare: higher frequency ⇒ smaller output ripple.
3. Try **Undo** (++ctrl+z++) / **Redo** (++ctrl+y++) — every edit is tracked
   server-side.

The buck relationship is **V_out = D × V_in**, where D is the duty cycle.

## 6. Save Your Work

**Save** writes your circuit as a `.ipes` file — in the desktop app a native
save dialog appears, in the browser the file downloads. The `.ipes` format is
fully portable between the desktop app, the web editor, and the classic UI.

## Try More Examples

The **Examples** menu includes ready-to-run circuits:

| Example | What you'll learn |
|---------|-------------------|
| Multi-Scope RLC | Multi-channel scopes, transient oscillation |
| RLC Resonant Circuit | Second-order step response |
| RC Low-Pass Filter | First-order charging curve |

Over 100 application circuits (converters, inverters, PFC, motor drives,
thermal demos) are in the repository under `resources/tutorials/` and
`resources/examples/` — open them with **File ▸ Open** or by double-clicking
the `.ipes` file.

## Next Steps

- [First Simulation](first-simulation.md) — guided walkthrough
- [Building Circuits](building-circuits.md) — create circuits from scratch
- [User Interface](interface.md) — editor layout and shortcuts
- [NativeC Blocks](../native-c-blocks.md) — run your real C/C++ control code
- [MCP Interface](../mcp.md) — let an LLM drive the simulator
