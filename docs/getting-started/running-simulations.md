---
title: Running Simulations
description: Simulation settings, solvers, and execution modes
---

# Running Simulations

How to configure and run simulations in the editor.

**Duration:** 10 minutes

**Prerequisites:** [Building Circuits](building-circuits.md)

## Simulation Settings

Open the **Simulation Settings** panel on the right sidebar.

### Essential Parameters

| Parameter | Description | How to Choose |
|-----------|-------------|---------------|
| **Duration (t_end)** | Simulation duration | 10–100× switching period |
| **Time step (dt)** | Computation interval | < T_sw / 100 |
| **Solver** | Numerical method | See solver table below |

### Time Step Selection

The time step is the most critical setting. Too large causes errors; too
small wastes computation time.

**Rule of thumb:** dt < 1/(100 × f_sw)

| Switching Freq | Max Time Step | Recommended |
|---------------|---------------|-------------|
| 10 kHz | 1 µs | 500 ns |
| 50 kHz | 200 ns | 100 ns |
| 100 kHz | 100 ns | 50 ns |
| 500 kHz | 20 ns | 10 ns |

### Simulation Duration

Choose enough time for the circuit to reach steady state:

- **Fast circuits** (> 100 kHz): 0.1 – 1 ms
- **Medium circuits** (10–100 kHz): 1 – 10 ms
- **Slow circuits** (< 10 kHz, motors): 10 – 100 ms

### Solvers

| Solver | Best for | Notes |
|--------|----------|-------|
| **Backward Euler** (default) | Switching converters | Most stable, first-order accuracy |
| **Trapezoidal** | Smooth waveforms | Less damping, can ring |
| **Gear-Shichman** | Stiff circuits | Strong numerical damping |

## Running

1. Click **▶ Run Simulation** in the settings panel.
2. Live progress streams to the scope view (time, percentage, current step).
3. Pause/resume a running simulation, or **cancel** it — controls appear next
   to the progress display.
4. When complete, the scope shows the recorded waveforms.

!!! tip "Zoom into the interesting part"
    After the run, use the mouse wheel to zoom into switching periods and
    drag to pan — see [Analysis Tools](analysis-tools.md).

## Results

Results appear in the **Simulation** workspace tab:

- **Scope view** with one lane per signal (or overlay mode)
- **Cursor measurements** — place cursors A/B for Δt and frequency
- **Statistics table** — min, max, peak-to-peak, RMS, mean per signal
- **CSV export** of the full dataset

If a simulation fails (numerical breakdown, non-convergence), the scope shows
the error and the values that triggered it — typically fixed by reducing dt.

## Steady State

To see steady-state behavior:

1. **Run longer** — simulate enough cycles for settling
2. **Set initial conditions** — pre-set capacitor voltages and inductor
   currents on the components
3. **Zoom** into the last few periods and read ripple/duty directly

## Convergence Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| NaN values | Numerical overflow | Reduce time step |
| Oscillating output | Trapezoidal ringing | Switch to Backward Euler |
| Very slow | Time step too small | Increase dt if accuracy allows |
| Wrong results | Time step too large | Decrease dt |

## Next Steps

- [Analysis Tools](analysis-tools.md) — measuring and exporting results
- [NativeC Blocks](../native-c-blocks.md) — run your C/C++ control code
- [Tutorials](../tutorials/index.md) — application-specific simulations
