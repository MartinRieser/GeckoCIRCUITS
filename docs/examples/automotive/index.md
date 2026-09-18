---
title: Automotive Examples
description: Power electronics for electric vehicles, hybrid vehicles, and charging infrastructure
---

# Automotive Examples

Power electronics for electric vehicles (EV), hybrid vehicles (HEV), and EV charging infrastructure in GeckoCIRCUITS.

## Available Examples

| Example | Description | Difficulty | Documentation |
|---------|-------------|------------|---------------|
| [EV Charger (Level 2)](ev-charger.md) | Single-phase and three-phase AC Level 2 charging infrastructure | Intermediate | [View Example](ev-charger.md) |
| [Onboard Charger (OBC)](obc.md) | Bidirectional OBC with PFC front-end and isolated DC-DC resonant stage | Advanced | [View Example](obc.md) |
| [DC Fast Charger (Level 3)](dcfc.md) | High-power (50–350 kW) modular DC fast charging station | Advanced | [View Example](dcfc.md) |
| [Traction Inverter](traction.md) | 400V/800V SiC traction inverter with field-oriented PMSM control | Advanced | [View Example](traction.md) |

## Quick Reference

### EV Charging Levels

| Level | Voltage | Power | Standard Connector | Charging Time (0–80%) |
|-------|---------|-------|--------------------|-----------------------|
| Level 1 | 120 VAC (1φ) | 1.4–1.9 kW | J1772 | 15–25 hours |
| Level 2 | 208–240 VAC (1φ/3φ) | 3.3–19.2 kW | J1772 / Type 2 | 4–8 hours |
| Level 3 (DCFC) | 200–1000 VDC | 50–350 kW | CCS / NACS / CHAdeMO | 15–30 minutes |

### Vehicle Powertrain Architecture

```
Grid ──► [OBC / DCFC] ──► [HV Battery: 400V/800V] ──► [Traction Inverter] ──► [PMSM Motor]
                                   │
                                   ▼
                           [HV to LV DC-DC] ──► 12V / 48V Auxiliary Systems
```

## Related Tutorials

- [PFC Basics Tutorial](../../tutorials/acdc/pfc-basics.md)
- [Three-Phase Inverter Tutorial](../../tutorials/dcac/three-phase.md)
- [LLC Resonant Converter Example](../power-supplies/llc.md)
- [Motor Drive Examples](../motor-drives/index.md)
