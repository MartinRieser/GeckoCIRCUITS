---
title: Magnetics & Mechanical
description: Magnetic component modeling, non-linear saturation, and mechanical system simulation
---

# Magnetics & Mechanical

Magnetic component modeling and electromechanical system simulation in GeckoCIRCUITS.

## Available Tutorials and Project Files

| Tutorial | Title | Difficulty | Validated Circuit File | Description |
|:---|:---|:---|:---|:---|
| [901](magnetic-domain.md) | [Magnetic Domain Introduction](magnetic-domain.md) | Intermediate | [`magnetic-domain.ipes`](magnetic-domain.ipes) | Permeance-capacitance analogy, winding gyrator interface, and core coupling |
| [902](transformer-design.md) | [Transformer Design](transformer-design.md) | Advanced | [`transformer-design.ipes`](transformer-design.ipes) | Practical transformer with leakage, winding resistance, core loss, and 4:1 turns ratio |
| [903](inductor-saturation.md) | [Inductor Saturation](inductor-saturation.md) | Advanced | [`inductor-saturation.ipes`](inductor-saturation.ipes) | Non-linear core saturation comparison with knee at $I_{sat} = 7\text{ A}$ |
| [904](mechanical-systems.md) | [Mechanical Systems](mechanical-systems.md) | Advanced | [`mechanical-systems.ipes`](mechanical-systems.ipes) | Two-mass torsional electromechanical drivetrain, shaft stiffness, and resonance |

## Learning Objectives

- Model magnetic circuits using the permeance-capacitance analogy
- Simulate transformer core saturation, leakage flux, and hysteresis effects
- Design inductors with air gaps and non-linear saturation characteristics
- Couple electrical circuits with rotational mechanical loads
- Inspect magnetic, electromechanical, and electrical waveforms via MCP tools and GUI scopes

## Overview

### Magnetic Domain
The magnetic domain enables detailed modeling of:
- Transformer saturation effects and inrush currents
- Core losses (hysteresis and eddy currents)
- Coupled inductors with mutual leakage paths
- Custom non-linear B-H material characteristics

### Mechanical Domain
The mechanical domain enables simulation of:
- Motor-load torque dynamics and rotational inertia
- Gearbox reduction ratios and friction models
- Speed and position feedback loops

## Related Resources

- [Flyback Converter Tutorial](../dcdc/flyback.md) — Coupled inductor modeling
- [Forward Converter Tutorial](../dcdc/forward.md) — Transformer reset mechanisms
- [Motor Drives Examples](../../examples/motor-drives/index.md) — Electromechanical drive systems
