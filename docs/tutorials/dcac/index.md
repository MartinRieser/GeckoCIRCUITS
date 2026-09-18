---
title: DC-AC Inverters
description: DC to AC power conversion, modulation schemes, and multilevel inverters
---

# DC-AC Inverters

DC to AC power conversion and PWM control in GeckoCIRCUITS.

## Available Tutorials

| Tutorial | Title | Difficulty | Description |
|----------|-------|------------|-------------|
| [401](single-phase.md) | [Single-Phase Inverter](single-phase.md) | Intermediate | H-bridge PWM inverter, bipolar vs. unipolar modulation |
| [402](three-phase.md) | [Three-Phase Inverter](three-phase.md) | Intermediate | Voltage source inverter (VSI), SPWM, and space vector PWM |
| [403](npc-inverter.md) | [NPC Multilevel Inverter](npc-inverter.md) | Advanced | 3-level Neutral-Point-Clamped inverter topology |
| [404](mmc-converter.md) | [MMC Converter](mmc-converter.md) | Advanced | Modular Multilevel Converter for HVDC and grid systems |

## Learning Objectives

- Design single-phase and three-phase PWM voltage source inverters
- Compare sinusoidal PWM (SPWM), third-harmonic injection, and space vector modulation (SVPWM)
- Analyze voltage source rectifier (VSR) bidirectional operation
- Understand multilevel voltage synthesis and neutral-point balance

## Quick Reference

### Inverter Topologies

| Type | Levels | Applications |
|------|--------|-------------|
| H-Bridge | 3 | Single-phase UPS, solar microinverters |
| 3-Phase 2-Level | 2 | Industrial motor drives, EV traction |
| NPC 3-Level | 3 | Medium-voltage drives, grid-tied solar |
| MMC | N+1 | High-Voltage DC (HVDC), large STATCOM |

### Modulation Comparison

| Method | Max Modulation Index | THD | Notes |
|--------|---------------------|-----|-------|
| SPWM | 1.00 | Higher | Simple implementation |
| Third Harmonic Injection | 1.15 | Medium | +15% DC bus utilization |
| SVPWM | 1.15 | Lower | Optimal harmonic performance |

## Prerequisites

- Switching fundamentals: [PWM Basics](../../getting-started/pwm-basics.md)
- DC-DC converters: [DC-DC Series](../dcdc/index.md)

## Related Examples

- [Motor Drives Examples](../../examples/motor-drives/index.md)
- [Renewable Energy Solar Inverter](../../examples/renewable/solar.md)
