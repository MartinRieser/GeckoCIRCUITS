# DC Fast Charger (Level 3)

## Overview

DC fast charging stations deliver high-power DC directly to the vehicle battery, enabling rapid charging (20-40 minutes to 80% SOC). These systems require sophisticated power conversion and thermal management.

**Difficulty:** Advanced

**Status:** Placeholder

## Specifications

| Parameter | Value | Unit |
|-----------|-------|------|
| Input Voltage | 480 VAC 3φ | VAC |
| Output Voltage | 200-1000 | VDC |
| Output Power | 50-350 | kW |
| Output Current | 0-500 | A |
| Efficiency | >95% | - |

## Architecture

### Modular Design

```
       ┌────────────────┐
480V ──┤ Power Module 1 ├──┬──► 50kW
3φ AC  │    (50kW)      │  │
       └────────────────┘  │
       ┌────────────────┐  │
       │ Power Module 2 ├──┼──► +50kW
       │    (50kW)      │  │
       └────────────────┘  │   Paralleled Output
       ┌────────────────┐  │
       │ Power Module N ├──┼──► +50kW
       │    (50kW)      │  │
       └────────────────┘  │
                           ▼
                     [Output Filter]
                           │
                     ─── CCS/CHAdeMO Connector
```

### Single Module Topology

```
3φ AC ─[EMI]─[Vienna/AFE]─[DC Link]─[Isolated DC-DC]─ DC Out
                 │            │
            PFC Stage    LLC/DAB/PSFB
```

## Power Module Options

| Topology | Power | Efficiency | Bidirectional |
|----------|-------|------------|---------------|
| Vienna + PSFB | 50-100 kW | 95% | No |
| AFE + DAB | 50-100 kW | 96% | Yes |
| Vienna + LLC | 25-50 kW | 97% | No |

## Control Requirements

### Output Characteristics

```
Voltage (V)
    │
1000├────────────────────┐
    │                    │
    │    Constant        │ Constant
    │    Current         │ Voltage
    │    Region          │ Region
    │                    │
 200├────────────────────┘
    └────────────────────────► Current (A)
                         500
```

### Communication Protocols

| Standard | Protocol | Use |
|----------|----------|-----|
| CCS | ISO 15118, DIN 70121 | Power negotiation |
| CHAdeMO | CHAdeMO Protocol | Legacy DC charging |
| OCPP | OCPP 1.6/2.0 | Backend communication |

## Safety Features

- Ground fault detection (GFCI)
- Isolation monitoring
- Emergency stop
- Cable temperature monitoring
- Connector locking
- Insulation testing before charge

## Circuit Files

The following verified circuit files are included:

- `11kw_three_phase_vienna_charger.ipes` - Complete 11 kW 3-phase Vienna active PFC DC fast charger with closed-loop voltage/current regulation
- `../../../tutorials/3xx_acdc_rectifiers/303_vienna_rectifier/three-phase_ViennaRectifier_simpleControl_250kW.ipes` - 250 kW Vienna rectifier with control

