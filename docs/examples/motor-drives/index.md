---
title: Motor Drive Examples
description: Electric motor control systems for industrial, automotive, and robotic applications
---

# Motor Drive Examples

Electric motor control systems for industrial, automotive, and renewable energy applications in GeckoCIRCUITS.

## Available Examples

| Example | Description | Difficulty | Documentation |
|---------|-------------|------------|---------------|
| [BLDC Control](bldc.md) | Brushless DC motor with 6-step trapezoidal commutation | Intermediate | [View Example](bldc.md) |
| [PMSM FOC](pmsm-foc.md) | Permanent Magnet Synchronous Motor with Field-Oriented Control | Advanced | [View Example](pmsm-foc.md) |
| [Induction Motor](induction.md) | Three-phase induction motor with scalar (V/f) and vector control | Advanced | [View Example](induction.md) |

## Quick Reference

### Motor Types Comparison

| Motor | Commutation / Control | Sensor Feedback | Efficiency | Relative Cost |
|-------|----------------------|-----------------|------------|---------------|
| **BLDC** | 6-step trapezoidal | Hall sensors (or sensorless) | Good (85–90%) | Low |
| **PMSM** | Field-Oriented Control (dq) | Encoder / Resolver | High (92–97%) | Medium–High |
| **Induction Motor (IM)** | V/f scalar or FOC | None or encoder | Good (85–94%) | Low–Medium |

### Field-Oriented Control (FOC) Architecture

```
Speed Ref ──►[PI]──► Iq_ref ──►[PI]──► Vq ──►[Inverse]──► Va,Vb,Vc ──►[SVPWM]──► Inverter
                       │                 │     Park                       │
             Id_ref=0  │                 │                                │
               ──────►[PI]──► Vd ────────┘                                │
                       │                                                  │
                       └◄───────[Park]◄───────[Clarke]◄───────[Current]◄──┘
                                  dq             αβ           Sense
                                   │
                              θe ──┘ (from resolver/encoder)
```

## Related Tutorials

- [Three-Phase Inverter Tutorial](../../tutorials/dcac/three-phase.md)
- [Mechanical Systems Simulation](../../tutorials/magnetics/mechanical-systems.md)
- [Advanced Motor Drives Guide](../../tutorials/advanced/index.md#802-motor-drives-pmsm)
