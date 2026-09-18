# EV Charger (Level 2 AC)

## Overview

A Level 2 electric vehicle charger converts single-phase or three-phase AC to DC for battery charging. This example demonstrates a basic AC-DC conversion topology with power factor correction.

**Difficulty:** Intermediate

**Status:** Placeholder

## Specifications

| Parameter | Value | Unit |
|-----------|-------|------|
| Input Voltage | 208-240 | VAC |
| Output Voltage | 250-450 | VDC |
| Output Power | 7.2-19.2 | kW |
| Power Factor | >0.99 | - |
| Efficiency | >94% | - |

## Topology: Boost PFC + Isolated DC-DC

```
                    PFC Stage                    Isolated Stage
AC ─[EMI]─[Rectifier]─[Boost PFC]─[DC Link]─[Full Bridge]─[Transformer]─[Rectifier]─ DC Out
     │                    │            │                                      │
   Filter            L, D, S        C_link                                  C_out
```

## Key Design Points

1. **EMI Filter:** Meet CISPR 25 emissions
2. **Boost PFC:** Unity power factor, regulated DC link
3. **Isolation:** Transformer provides galvanic isolation
4. **Output Regulation:** CC/CV charging profile

## Circuit Files

The following verified simulation models in the repository implement the EV charger front-end and full battery charging topologies:

- `../../automotive/dc_fast_charger/11kw_three_phase_vienna_charger.ipes` - Complete 11 kW 3-phase Vienna active PFC charger
- `../../../tutorials/3xx_acdc_rectifiers/303_vienna_rectifier/three-phase_ViennaRectifier_simpleControl_250kW.ipes` - 250 kW Vienna active rectifier
- `../../../tutorials/3xx_acdc_rectifiers/302_pfc_basics/boostPFC_currentControl.ipes` - Single-phase boost PFC with closed-loop current control
- `../../../projects/llc_resonant_400v_24v.ipes` - Isolated resonant DC-DC converter stage

