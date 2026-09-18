---
title: Thermal Analysis Examples
description: Power semiconductor thermal modeling and heatsink design for reliable operation
---

# Thermal Analysis Examples

Power semiconductor thermal modeling and heatsink design in GeckoCIRCUITS.

## Available Tutorials & Examples

| Guide | Description | Difficulty | Documentation |
|-------|-------------|------------|---------------|
| [Loss Calculation](../../tutorials/thermal/loss-calculation.md) | Conduction and switching loss calculation in power switches | Intermediate | [View Tutorial](../../tutorials/thermal/loss-calculation.md) |
| [Junction Temperature](../../tutorials/thermal/junction-temperature.md) | Dynamic thermal impedance modeling and junction temperature estimation | Intermediate | [View Tutorial](../../tutorials/thermal/junction-temperature.md) |
| [Heatsink Design](../../tutorials/thermal/heatsink-design.md) | Thermal resistance budgeting and cooling system sizing | Advanced | [View Tutorial](../../tutorials/thermal/heatsink-design.md) |

## Quick Reference

### Thermal Resistance Chain

```
Tj ──[Rth,jc]──► Tc ──[Rth,ch]──► Ts ──[Rth,ha]──► Ta
Junction         Case          Interface   Heatsink   Ambient
```

### Typical Thermal Parameters

| Component | $R_{th}$ Range (K/W) | Notes |
|-----------|----------------------|-------|
| $R_{th,jc}$ (Discrete TO-247) | 0.5 – 1.5 | From manufacturer datasheet |
| $R_{th,jc}$ (Power Module) | 0.1 – 0.4 | From manufacturer datasheet |
| $R_{th,ch}$ (Thermal Paste) | 0.05 – 0.2 | Depends on mounting torque and contact area |
| $R_{th,ch}$ (Sil-Pad) | 0.2 – 0.6 | Provides electrical isolation |
| $R_{th,ha}$ (Natural Convection) | 1.0 – 5.0 | Passive heatsink |
| $R_{th,ha}$ (Forced Air) | 0.1 – 1.0 | Requires fan cooling |

## Related Tutorials

- [Loss Calculation Tutorial](../../tutorials/thermal/loss-calculation.md)
- [Junction Temperature Tutorial](../../tutorials/thermal/junction-temperature.md)
- [Heatsink Design Tutorial](../../tutorials/thermal/heatsink-design.md)
