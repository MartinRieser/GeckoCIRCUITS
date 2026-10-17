---
title: "502 - Junction Temperature"
---

# 502 - Junction Temperature

Thermal network modeling and transient analysis.

## Overview

Junction temperature directly affects:
- Semiconductor reliability (Arrhenius law)
- Conduction losses (RDS(on) increases)
- Switching characteristics
- Safe Operating Area (SOA)

## Thermal Equivalent Circuit

### Electrical-Thermal Analogy

| Thermal | Electrical | Unit |
|---------|------------|------|
| Temperature T | Voltage V | °C / V |
| Heat Flow P | Current I | W / A |
| Thermal Resistance Rth | Resistance R | K/W / Ω |
| Thermal Capacitance Cth | Capacitance C | J/K / F |

### Thermal Network Representation

![Dynamic Thermal Impedance Networks: Foster vs. Cauer](thermal-network-schematic.svg)

## Thermal Network Models

### Foster Network

- Series connection of parallel $R_i \parallel C_i$ branches
- Parameters obtained directly from datasheet transient thermal impedance curve fitting
- **Cannot be cascaded** directly (internal nodes have no physical temperature meaning)
- Mathematically efficient for fast simulation

### Cauer Network

- Ladder network with series thermal resistances and shunt thermal capacitances to ambient ground
- Physically meaningful nodes corresponding to semiconductor material layers (junction, silicon die, solder, copper baseplate, heatsink, ambient)
- **Can be cascaded** modularly (junction $\to$ case $\to$ heatsink $\to$ ambient)
- Mathematically transformed from Foster coefficients or derived from finite element analysis (FEA)

## Setting Up Thermal Models

### Datasheet Parameters

From semiconductor datasheet:
- $R_{th,j-c}$ (junction to case)
- $R_{th,c-h}$ (case to heatsink) - depends on mounting / thermal grease
- Transient thermal impedance curve $Z_{th}(t)$

### Extracting Foster Parameters

From $Z_{th}$ curve fitting:

$$
Z_{th}(t) = \sum_{i=1}^{n} R_i \cdot (1 - e^{-t/\tau_i})
$$

Where $\tau_i = R_i \times C_i$

## Temperature Calculations

### Steady-State

$$
T_j = T_a + P_{loss} \cdot (R_{th,jc} + R_{th,ch} + R_{th,ha})
$$

### Transient Response

For step power change:

$$
T_j(t) = T_a + P \cdot Z_{th}(t)
$$

### Periodic Power

Average temperature + ripple:

$$T_{j,avg} = T_a + P_{avg} \cdot R_{th,total}$$

$$\Delta T_{j,ripple} = P_{peak} \cdot Z_{th}(t_{on})$$

## Design Guidelines

### Maximum Junction Temperature

| Device | Tj,max | Recommended Max |
|--------|--------|-----------------|
| Si MOSFET | 150-175°C | 125°C |
| Si IGBT | 150-175°C | 125°C |
| SiC MOSFET | 175-200°C | 150°C |
| GaN HEMT | 150°C | 125°C |

### Derating

Temperature margin for reliability:

$$T_{j,design} = T_{j,max} - \Delta T_{margin}$$

Typical margin: 25-50°C

## Simulation Exercises

1. Build Cauer thermal network from datasheet
2. Simulate startup thermal transient
3. Compare Foster vs Cauer response
4. Analyze temperature at different switching frequencies

## Related Resources

- [501 - Loss Calculation](loss-calculation.md)
- [503 - Heatsink Design](heatsink-design.md)
