---
title: Building Circuits
description: Learn to create circuits from scratch in GeckoCIRCUITS
---

# Building Circuits

Create power electronics circuits from scratch in the editor.

**Duration:** 20 minutes

**Prerequisites:** [First Simulation](first-simulation.md)

## Overview

Building a circuit is four steps:

1. **Place components** from the palette
2. **Wire them together**
3. **Set parameters** in the properties panel
4. **Add measurement** points (scopes, probes)

## Step 1: Start a New Circuit

**File ▸ New** creates an empty schematic (a blank workspace is also created
automatically at app start).

## Step 2: Place Components

1. Find a component in the **palette** on the left — filter by category or
   search by name.
2. Click it: the component becomes a "ghost" that follows the cursor.
3. Move it to a grid position and **click** to place; press ++r++ to rotate
   before placing.
4. **Escape** cancels; after placing you can keep placing more of the same
   type.

Useful while placing: arrow keys nudge the ghost by one grid unit.

### What a Basic Circuit Needs

| Category | Components | Why |
|----------|-----------|-----|
| Sources | Voltage source | Provides input power |
| Switches | MOSFET/IGBT + Diode | Switching elements |
| Passive | L, C, R | Energy storage and filtering |
| Control | PWM/Constant/Script block | Gate signals |
| Measurement | Scope, probes | View results |

## Step 3: Wire the Components

1. Press **W** (or click the wire-mode toggle) to enter wire mode.
2. **Click** a component pin to start a wire.
3. **Click** along the path to set waypoints (orthogonal routing is automatic;
   arrow keys step the current waypoint on the grid).
4. **Click** the destination pin to finish; **Escape** aborts.

Wires attach to pins by **net label** — move a component later and its wires
stay attached.

## Step 4: Set Parameters

Select a component and edit its parameters in the **properties panel** on the
right. Values accept engineering notation; the panel shows units and valid
ranges for every parameter.

## Step 5: Add Measurement Points

- Place **VOLTMETER**/**AMMETER** probes and couple them to the component
  they measure.
- Place a **SCOPE** and wire probe outputs into its inputs.
- Recorded signals appear in the **Simulation** view tab after the next run.

## Pre-Run Checklist

- [ ] Every voltage source has a return path (ground/reference node)
- [ ] Every switch has a gate signal
- [ ] Scope/probe connected to the points you want to see
- [ ] Time step appropriate (< T_sw / 100)

## Simulation Settings

Set duration, dt, and solver in the **Simulation Settings** panel:

| Parameter | Example Value | Why |
|-----------|---------------|-----|
| Duration | 1 ms | ~100 switching periods |
| Time step | 50 ns | 200 steps per period |
| Solver | Backward Euler | Stable for switching circuits |

## Undo / Redo

Every edit is tracked by the engine: **Ctrl+Z** reverts, **Ctrl+Y** reapplies
— including moves, parameter changes, and deletions.

## Next Steps

- [Running Simulations](running-simulations.md) — solvers, settings, execution
- [Analysis Tools](analysis-tools.md) — cursors, FFT, losses, export
