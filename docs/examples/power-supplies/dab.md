---
title: Dual Active Bridge Converter Example
---

# Dual Active Bridge (DAB) Converter Example

Bidirectional isolated DC-DC converter.

## Overview

The DAB converter provides:
- Bidirectional power flow
- Galvanic isolation
- Soft switching capability
- High power density

## Specifications

| Parameter | Value |
|-----------|-------|
| Port 1 Voltage | 400V DC |
| Port 2 Voltage | 48V DC |
| Power Rating | ±3 kW |
| Switching Frequency | 100 kHz |
| Transformer Ratio | 8:1 |
| Leakage Inductance | 50 µH |

## Circuit Files

The following related H-bridge and isolated converter models are available in the repository:

- `resources/projects/llc_resonant_400v_24v.ipes` - Isolated DC-DC converter with high-frequency transformer stage
- `resources/tutorials/4xx_dcac_inverters/401_single_phase_inverter/singlePhase_PWM_converter.ipes` - Full-bridge H-bridge PWM converter
- `resources/tutorials/3xx_acdc_rectifiers/301_diode_rectifier/diode_RL_singlePH_trafo.ipes` - Transformer-coupled bridge rectifier circuit

## Theory

### Operating Principle

Two H-bridges connected through high-frequency transformer:

```
       H-Bridge 1      Transformer     H-Bridge 2
V1 ──┤           ├──────┤ n:1 ├──────┤           ├── V2
     └───────────┘      └─────┘      └───────────┘
```

### Phase Shift Modulation

Power transfer controlled by phase angle φ between bridges:

$$P = \frac{V_1 \cdot V_2'}{2\pi f_s L} \cdot \phi \cdot (1 - \frac{|\phi|}{\pi})$$

Where V2' = n × V2

### Key Equations

**Maximum Power:**
$$P_{max} = \frac{V_1 \cdot V_2'}{8 f_s L}$$ (at φ = π/2)

**ZVS Condition:**
Sufficient current at switching instant for capacitor discharge.

## Modulation Strategies

### Single Phase Shift (SPS)
- Simplest control
- 50% duty cycle on both bridges
- Phase angle controls power

### Extended Phase Shift (EPS)
- Inner phase shift added
- Wider ZVS range
- Reduced circulating current

### Dual Phase Shift (DPS)
- Both bridges have inner shift
- Optimized for efficiency
- More complex control

## Bidirectional Operation

| Direction | φ Sign | Power Flow |
|-----------|--------|------------|
| Forward | φ > 0 | V1 → V2 |
| Reverse | φ < 0 | V2 → V1 |

## Exercises

1. **Power vs Phase Shift:** Verify transfer characteristic
2. **ZVS Boundary:** Find minimum load for ZVS
3. **Bidirectional:** Reverse power flow
4. **Efficiency Comparison:** SPS vs EPS

## Applications

- Battery energy storage systems
- EV charging (V2G capable)
- Solid-state transformers
- DC microgrids

## Related Resources

- [LLC Resonant](llc.md)
- [Onboard Charger](../automotive/obc.md)
