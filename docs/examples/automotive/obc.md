---
title: Onboard Charger (OBC)
description: EV onboard charger with power factor correction and resonant DC-DC stage
---

# Onboard Charger (OBC)

## Overview

The onboard charger is integrated into the electric vehicle and converts AC grid power to DC for battery charging. Modern OBCs feature bidirectional operation for vehicle-to-grid (V2G) capability.

**Difficulty:** Advanced

## Specifications

| Parameter | Value | Unit |
|-----------|-------|------|
| Input Voltage | 85-265 (1φ), 340-530 (3φ) | VAC |
| Output Voltage | 250-450 (400V) or 500-920 (800V) | VDC |
| Output Power | 11-22 | kW |
| Power Factor | >0.99 | - |
| Efficiency | >94% (peak >96%) | - |
| Power Density | >2.5 | kW/L |

## Topology Options

### Option 1: Totem-Pole PFC + LLC
```
AC ─[EMI]─[Totem-Pole PFC]─[DC Link]─[LLC Resonant]─ DC
```
- High efficiency (>96%)
- Soft switching
- Unidirectional

### Option 2: Vienna Rectifier + DAB
```
3φ AC ─[EMI]─[Vienna Rectifier]─[DC Link]─[DAB]─ DC
```
- Three-phase input
- Bidirectional capable
- Higher power density

### Option 3: Integrated Charger (Motor Windings)
```
AC ─── Motor Windings (as filter) ─── Inverter (as rectifier) ─── Battery
```
- Uses traction components
- Lower cost
- Reduced power

## Key Features

| Feature | Implementation |
|---------|----------------|
| Wide input range | Boost PFC (universal input) |
| Isolation | HF transformer (LLC/DAB) |
| Bidirectional | DAB or CLLC topology |
| Thermal management | Liquid cooling |

## Control Architecture

```
┌─────────────────────────────────────────────────┐
│  CAN/LIN Interface ◄─── Vehicle ECU            │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│  Charge Control (CC/CV profile, battery SOC)    │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼────────┬──────────────────────┐
│  PFC Control             │  DC-DC Control       │
│  (Vbus, PF, THD)         │  (Vbat, Ibat)        │
└──────────────────────────┴──────────────────────┘
```

## Related Resources

- [EV Charger (Level 2)](ev-charger.md)
- [DC Fast Charger (Level 3)](dcfc.md)
- [PFC Basics Tutorial](../../tutorials/acdc/pfc-basics.md)
- [LLC Resonant Example](../power-supplies/llc.md)
