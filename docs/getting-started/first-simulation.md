---
title: First Simulation
description: Step-by-step walkthrough of your first GeckoCIRCUITS simulation
---

# Your First Simulation

This walkthrough opens, runs, and analyzes a buck converter simulation.

**Duration:** 15 minutes

**Prerequisites:** [Installation](installation.md) complete

## Step 1: Launch GeckoCIRCUITS

Start the desktop app (or open the web editor). After a few seconds the
editor opens with an empty schematic.

## Step 2: Open the Example Circuit

Open the **Examples** menu in the top bar and choose **DC-DC Buck Converter**
(or **File ▸ Open** and pick `resources/tutorials/2xx_dcdc_converters/201_buck_converter/buck_simple.ipes`).

The schematic shows the classic buck topology:

- **Power components** — switch, diode, L, C, load resistor
- **Control blocks** — PWM generation, signal sources
- **Scope blocks** — measurement points

## Step 3: Understand the Circuit

| Component | Role |
|-----------|------|
| DC voltage source | Provides input power |
| MOSFET switch | Controlled switching element |
| Diode | Freewheeling current path |
| Inductor (L) | Stores energy, smooths current |
| Capacitor (C) | Filters output voltage |
| Resistor (R) | Load |
| SCOPE | Records measurement signals |

Click any component to see and edit its parameters in the **properties
panel** on the right.

## Step 4: Configure the Simulation

Open the **Simulation Settings** panel on the right sidebar:

| Parameter | Meaning | Typical Value |
|-----------|---------|---------------|
| **Duration (t_end)** | How long to simulate | 10 ms |
| **Time step (dt)** | Computation interval | 1 µs |
| **Solver** | Numerical method | Backward Euler |

!!! tip "Time Step Rule"
    The time step should be **100× smaller** than the switching period.
    For 100 kHz switching: dt < 0.1 µs.

## Step 5: Run

Click **▶ Run Simulation** and watch the live progress. Typical runtime for
simple circuits: **1–5 seconds**.

## Step 6: View the Results

The **Simulation** workspace tab shows the recorded waveforms — output
voltage, inductor current, switch node.

### Scope Navigation

| Action | Control |
|--------|---------|
| Zoom in/out | Mouse wheel (around the cursor) |
| Pan | Left-click + drag |
| Fit whole simulation | ⟲ toolbar button |
| Y-axis | Auto-rescales to the visible window |
| Cursors | Click to place **A**, click again for **B** — Δt and frequency readout below the chart |

## Step 7: Analyze the Results

For a buck converter you should observe:

1. **Output voltage** settling to V_out = D × V_in
2. **Inductor current** — triangular ripple waveform
3. **Switch voltage** — rectangular pulses between V_in and 0

### What to Check

- Does the output reach steady state?
- Is the ripple within acceptable limits?
- Any unexpected spikes or oscillations?

## Step 8: Experiment

1. **Change duty cycle** — select the control block, adjust D
2. **Change load** — select the resistor, change R
3. **Change inductance** — select the inductor, change L
4. Re-run and compare — **Undo** (++ctrl+z++) reverts anything

## What You've Learned

- Launching the app and opening example circuits
- Configuring time step, duration, and solver
- Running a simulation and reading waveforms
- Navigating the scope with zoom, pan, and cursors

**Next:** [Building Circuits](building-circuits.md) — create your own circuit.
