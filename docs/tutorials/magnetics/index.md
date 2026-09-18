---
title: Magnetics & Mechanical
description: Magnetic component modeling, non-linear saturation, and mechanical system simulation
---

# Magnetics & Mechanical

Magnetic component modeling and electromechanical system simulation in GeckoCIRCUITS.

## Available Tutorials

| Tutorial | Title | Difficulty | Description |
|----------|-------|------------|-------------|
| [901](magnetic-domain.md) | [Magnetic Domain Introduction](magnetic-domain.md) | Intermediate | Permeance-capacitance analogy and magnetic circuit modeling |
| [902](transformer-design.md) | [Transformer Design](transformer-design.md) | Advanced | High-frequency transformer modeling, leakage, and core loss |
| [903](inductor-saturation.md) | [Inductor Saturation](inductor-saturation.md) | Advanced | Non-linear B-H curve and current-dependent inductance |
| [904](mechanical-systems.md) | [Mechanical Systems](mechanical-systems.md) | Advanced | Rotational dynamics, motor-load coupling, and friction models |

## Learning Objectives

- Model magnetic circuits using the permeance-capacitance analogy
- Simulate transformer core saturation, leakage flux, and hysteresis effects
- Design inductors with air gaps and non-linear saturation characteristics
- Couple electrical circuits with rotational mechanical loads

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
