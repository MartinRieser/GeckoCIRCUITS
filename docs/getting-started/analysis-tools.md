---
title: Analysis Tools
description: Measuring, analyzing, and exporting simulation results
---

# Analysis Tools

How to measure, analyze, and export simulation results in the editor.

**Duration:** 15 minutes

**Prerequisites:** [Running Simulations](running-simulations.md)

## The Scope View

The **Simulation** workspace tab is the primary tool for viewing results.
Each recorded signal appears as a channel; scopes in the circuit group
channels into instruments, and the scope selector at the top filters to one
instrument or shows everything.

### Navigation

| Action | How |
|--------|-----|
| Zoom in/out | Mouse wheel (zooms around the cursor) |
| Pan | Left-click and drag |
| Fit whole simulation | ⟲ button in the toolbar |
| Overlay / one lane per signal | Display toggle in the scope header |

When you zoom, the value axis rescales to the visible window — so switching
ripples become readable at any depth.

## Measurement Tools

### Cursors

Click on the plot to place cursor **A**, click again to place cursor **B**.
The measurement card below the chart shows:

- **t_A** and **t_B** positions
- **Δt** between the cursors
- **Frequency** = 1/Δt

*Clear Cursors* removes them.

### Signal Statistics

The metrics table computes min, max, peak-to-peak, RMS, and mean for every
signal over the whole run. Click a table row to toggle the trace.

## FFT — Harmonic Analysis

Click **FFT** in the scope toolbar:

1. Choose the signal and the number of harmonics.
2. Click **Compute** — the analysis runs on the **currently visible window**
   (zoom first to exclude startup transients).
3. Read base frequency, fundamental amplitude, DC component, and **THD**,
   with a spectrum chart and a per-harmonic table (amplitude and phase).

This is the fastest way to check grid power factor correction, output ripple
spectrum, or switching harmonics.

## Loss Calculation

Click **Losses** in the scope toolbar for semiconductor loss estimation:

- **Switching losses** from Eon/Eoff energies at your operating current,
  voltage, and junction temperature (scaled to a reference).
- **Conduction losses** from I, R_on, and threshold voltage with duty cycle.

Enter the datasheet energies and the operating point from your simulation —
the panel returns switching, conduction, and total loss in watts.

## Export

**CSV export** in the results drawer writes every recorded signal (time +
one column per channel) so you can post-process in Python, MATLAB, Excel, or
your script block source of truth.

## Tips for Good Analysis

1. **Wait for steady state** — don't measure during startup transients
2. **Use enough cycles** — average over multiple switching periods
3. **Check time step** — too large a step hides high-frequency content
4. **Compare with theory** — verify results match analytical predictions
5. **Document settings** — record simulation parameters with results

## Next Steps

- [Tutorials](../tutorials/index.md) — apply analysis to specific topologies
- [NativeC Blocks](../native-c-blocks.md) — test real C/C++ control code
- [MCP Interface](../mcp.md) — let an LLM run the measurements for you
