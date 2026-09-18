---
title: Power Supply Examples
description: Complete power supply designs including LLC resonant, DAB, and PFC converters
---

# Power Supply Examples

Complete power supply designs for industrial, telecommunication, and consumer electronics applications in GeckoCIRCUITS.

## Available Examples

| Example | Description | Difficulty | Documentation |
|---------|-------------|------------|---------------|
| [PFC Converters](pfc.md) | Single-phase and bridgeless power factor correction | Intermediate | [View Example](pfc.md) |
| [LLC Resonant Converter](llc.md) | High-efficiency resonant DC-DC with zero-voltage switching | Advanced | [View Example](llc.md) |
| [DAB Converter](dab.md) | Dual Active Bridge bidirectional isolated DC-DC converter | Advanced | [View Example](dab.md) |
| [Flyback Converter](../basic/flyback.md) | Isolated low-power flyback power supply | Beginner | [View Example](../basic/flyback.md) |
| [Forward Converter](../basic/forward.md) | Isolated medium-power forward converter | Intermediate | [View Example](../basic/forward.md) |

## Quick Reference

### Topology Selection by Power Rating

| Power Range | Topology | Typical Efficiency | Soft Switching |
|-------------|----------|--------------------|----------------|
| 5–75 W | Flyback | 85–90% | No (hard-switched) |
| 75–300 W | Forward / Flyback | 88–92% | Partial |
| 300 W – 3 kW | LLC Resonant | 94–97% | ZVS primary, ZCS secondary |
| 1 kW – 50 kW | Dual Active Bridge (DAB) | 95–98% | ZVS bidirectional |

### Key Applications

| Application | Typical Power | Front-End Stage | Isolated DC-DC Stage |
|-------------|---------------|-----------------|----------------------|
| Server Power Supply | 800 W – 3 kW | Boost / Totem-Pole PFC | Half/Full-Bridge LLC |
| EV Onboard Charger | 3.3 kW – 22 kW | Vienna / Totem-Pole PFC | CLLC / DAB |
| Battery Energy Storage | 5 kW – 50 kW | Active Front End | Bidirectional DAB |
| Telecom Rectifier | 1 kW – 4 kW | Boost PFC | LLC Resonant |

## Related Tutorials

- [PFC Basics Tutorial](../../tutorials/acdc/pfc-basics.md)
- [Transformer Design Tutorial](../../tutorials/magnetics/transformer-design.md)
- [Loss Calculation Tutorial](../../tutorials/thermal/loss-calculation.md)
