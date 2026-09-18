---
title: Renewable Energy Examples
description: Power electronics for solar PV, wind turbines, and grid-connected energy storage
---

# Renewable Energy Examples

Power electronics for solar, wind, and energy storage applications in GeckoCIRCUITS.

## Available Examples

| Example | Description | Difficulty | Documentation |
|---------|-------------|------------|---------------|
| [Solar PV Inverter](solar.md) | Grid-tied photovoltaic inverter with Maximum Power Point Tracking (MPPT) | Advanced | [View Example](solar.md) |
| [Wind Turbine Converter](wind.md) | Permanent Magnet Synchronous Generator (PMSG) back-to-back converter | Advanced | [View Example](wind.md) |

## Quick Reference

### Solar PV Power Flow

```
PV Array ──► [DC-DC Boost (MPPT)] ──► DC Bus ──► [Grid Inverter] ──► AC Grid
                                         │
                                         ▼
                               [Battery Storage / BMS]
```

### Wind Power Conversion Architecture

```
Wind Turbine ──► [PMSG Generator] ──► [Active Rectifier] ──► DC Bus ──► [Grid Inverter] ──► AC Grid
```

## Related Tutorials

- [Three-Phase Inverter Tutorial](../../tutorials/dcac/three-phase.md)
- [PFC Basics Tutorial](../../tutorials/acdc/pfc-basics.md)
- [MMC Converter Tutorial](../../tutorials/dcac/mmc-converter.md)
