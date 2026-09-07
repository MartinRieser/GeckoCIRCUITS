---
title: User Interface
description: Guide to the GeckoCIRCUITS editor and workspace
---

# User Interface Guide

Overview of the GeckoCIRCUITS editor layout and workspace.

## Main Window Layout

The editor is organized into four areas:

```
┌────────────────────────────────────────────────┐
│  Top Bar (Examples, actions, search, theme)     │
├────────────┬─────────────────────────┬─────────┤
│  Palette   │                         │ Props / │
│  (left)    │   Schematic Canvas      │ Sim     │
│            │   (grid, drag & drop)   │ Settings│
│            │                         │ (right) │
├────────────┴─────────────────────────┴─────────┤
│  Workspace Tabs: Schematic ▦ | Simulation 📈    │
└────────────────────────────────────────────────┘
```

| Area | Purpose |
|------|---------|
| **Top bar** | Examples menu, save, undo/redo, wire mode, search (Ctrl+K), theme toggle |
| **Palette** (left) | All placeable components by category, with search |
| **Canvas** (center) | Schematic grid — place, move, wire, and select components |
| **Properties panel** (right) | Parameters of the selected component; simulation settings |
| **Workspace tabs** | Switch between *Schematic* editing and the *Simulation* scope view |

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Command palette (search components & commands) | ++ctrl+k++ or ++slash++ |
| Save circuit | ++ctrl+s++ |
| Undo / Redo | ++ctrl+z++ / ++ctrl+y++ |
| Rotate selection 90° (clockwise / counter-cw) | ++r++ / ++shift+r++ |
| Duplicate selection | ++d++ |
| Delete selection | ++delete++ / ++backspace++ |
| Wire mode on/off | ++w++ |
| Abort wire / cancel / deselect | ++escape++ |
| Move ghost/selection by one grid unit | Arrow keys |

The full, searchable shortcut list is available via the **command palette**
(++ctrl+k++).

## Editing Model

- **The engine owns the circuit.** Every edit (place, move, wire, delete,
  parameter change, undo, redo) is sent to the bundled engine, which is the
  single source of truth. The UI mirrors its state — so undo/redo and
  multi-view consistency always match the simulation model.
- **Wires connect by net labels.** A wire endpoint attached to a component
  pin stays attached when you move the component; multi-pin components
  (scopes, motors, C blocks) spread their pins vertically.
- **Simulation results are separate state.** Results, cursors, and zoom are
  cleared when you switch circuits, never mixed between them.

## Examples Menu

The **Examples** dropdown loads complete, ready-to-run circuits directly —
the fastest way to explore what GeckoCIRCUITS can do. See
[Quick Start](quickstart.md).

## Themes

Toggle between dark and light themes from the top bar; the setting is
remembered per machine.
