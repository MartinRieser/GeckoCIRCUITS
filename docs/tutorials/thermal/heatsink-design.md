---
title: "503 - Heatsink Design"
---

# 503 - Heatsink Design

Thermal resistance calculation and heatsink selection.

## Overview

Heatsink design ensures junction temperature stays within safe limits under all operating conditions.

## Thermal Budget

### Required Heatsink Thermal Resistance

$$R_{th,ha} = \frac{T_{j,max} - T_a}{P_{loss}} - R_{th,jc} - R_{th,ch}$$

### Example Calculation

Given:
- Tj,max = 125°C (design limit)
- Ta,max = 50°C (ambient)
- Ploss = 50W
- Rth,jc = 0.5 K/W
- Rth,ch = 0.2 K/W (with thermal paste)

Required:

$$R_{th,ha} = \frac{125 - 50}{50} - 0.5 - 0.2 = 0.8 \text{ K/W}$$

## Heatsink Types

### Natural Convection

| Type | Rth Range | Application |
|------|-----------|-------------|
| TO-220 clip | 10-30 K/W | <5W |
| Extruded profile | 1-10 K/W | 5-50W |
| Large finned | 0.3-2 K/W | 50-200W |

### Forced Air Cooling

Thermal resistance with airflow:

$$R_{th}(v) = R_{th,natural} \cdot \sqrt{\frac{v_0}{v}}$$

Where v₀ ≈ 0.5 m/s (natural convection equivalent)

### Liquid Cooling

| Type | Rth Range |
|------|-----------|
| Cold plate | 0.05-0.2 K/W |
| Microchannel | 0.02-0.1 K/W |
| Jet impingement | 0.01-0.05 K/W |

## Thermal Interface Materials

### TIM Thermal Resistance

$$R_{th,ch} = \frac{t_{TIM}}{k_{TIM} \cdot A} + R_{contact}$$

### Material Comparison

| TIM Type | k (W/m·K) | Rth (typical) |
|----------|-----------|---------------|
| Thermal grease | 1-5 | 0.1-0.3 K/W |
| Phase change | 3-5 | 0.1-0.2 K/W |
| Thermal pad | 1-6 | 0.2-0.5 K/W |
| Solder | 50 | 0.01-0.05 K/W |

## Multi-Device Heatsinks

### Thermal Coupling

Devices share heatsink → thermal interaction:

$$\begin{bmatrix} T_{j1} \\ T_{j2} \end{bmatrix} = \begin{bmatrix} Z_{11} & Z_{12} \\ Z_{21} & Z_{22} \end{bmatrix} \begin{bmatrix} P_1 \\ P_2 \end{bmatrix} + \begin{bmatrix} T_a \\ T_a \end{bmatrix}$$

Self-heating: Z₁₁, Z₂₂
Cross-coupling: Z₁₂ = Z₂₁

## Design Procedure

### Step 1: Loss Budget
- Calculate losses at worst-case operating point
- Include all devices on heatsink

### Step 2: Thermal Budget
- Determine maximum ambient temperature
- Set junction temperature target
- Calculate required Rth,ha

### Step 3: Heatsink Selection
- Select heatsink meeting Rth requirement
- Add margin for:
  - Manufacturing variation (10-20%)
  - TIM degradation over life
  - Altitude derating

### Step 4: Verification
- Simulate thermal transient
- Check at startup (maximum losses)
- Check at overload conditions

## GeckoCIRCUITS Thermal Setup

### Single Device

1. Enable thermal on device
2. Set Rth,jc from datasheet
3. Set Rth,ch (interface)
4. Add thermal node for heatsink
5. Set Rth,ha and Cth,ha

### Multiple Devices

1. Connect devices to common thermal node
2. Set individual Rth,jc
3. Single heatsink Rth,ha
4. Enable thermal coupling if needed

## Simulation Project: Heatsink Sizing & Thermal Verification

The companion project file [`heatsink-design.ipes`](heatsink-design.ipes) models a complete power stage with thermal budgeting, semiconductor loss injection, and heatsink thermal mass in GeckoCIRCUITS.

### Circuit & Simulation Specifications

| Parameter | Value | Description |
| :--- | :--- | :--- |
| **Circuit File** | [`heatsink-design.ipes`](heatsink-design.ipes) | Validated heatsink design model |
| **Input Voltage ($V_{in}$)** | $300\text{ V DC}$ | Operating DC supply |
| **Ambient Temperature ($T_a$)** | $50.0^\circ\text{C}$ | Worst-case high ambient environment (`Temp.1`) |
| **Power Loss ($P_{loss}$)** | $50\text{ W}$ | Average dissipated heat flux |
| **Junction-to-Case Resistance ($R_{th,jc}$)** | $0.5\text{ K/W}$ | Internal semiconductor die thermal barrier |
| **Case-to-Heatsink Resistance ($R_{th,ch}$)** | $0.2\text{ K/W}$ | Thermal interface material (TIM / thermal grease) |
| **Heatsink Thermal Resistance ($R_{th,ha}$)** | $0.8\text{ K/W}$ | Sized for $T_j \le 125^\circ\text{C}$ design budget (`Rth.ha`) |
| **Heatsink Thermal Capacitance ($C_{th,h}$)** | $25.0\text{ J/K}$ | Heatsink aluminum thermal mass (`Cth.6`) |
| **Simulation Step ($\Delta t$)** | $1.0\,\mu\text{s}$ | Fast MNA solver step |
| **Simulation Duration** | $0.01\text{ s}$ ($10\text{ ms}$) | High-resolution initial transient verification |

### Thermal Verification

Using the steady-state thermal chain formula:

$$
T_{sink} = T_a + P_{loss} \cdot R_{th,ha} = 50^\circ\text{C} + 50\text{ W} \cdot 0.8\text{ K/W} = 90.0^\circ\text{C}
$$

$$
T_j = T_{sink} + P_{loss} \cdot (R_{th,jc} + R_{th,ch}) = 90.0^\circ\text{C} + 50\text{ W} \cdot (0.5 + 0.2)\text{ K/W} = 125.0^\circ\text{C}
$$

The selected heatsink ($0.8\text{ K/W}$) guarantees the junction temperature remains strictly within the $125^\circ\text{C}$ safe design envelope.

### How to Run Simulation

=== "GeckoCIRCUITS GUI"
    1. Open `heatsink-design.ipes` in GeckoCIRCUITS desktop.
    2. Click **Start Simulation** (or press **F5**).
    3. Open the **Scope** window and inspect the thermal nodes `Tj` and `TSink`.

=== "MCP Interface (Command Line)"
    ```bash
    # Validate circuit integrity
    python -c "from scripts.desktop.mcp_client import GeckoMcpClient; c = GeckoMcpClient(); c.start(); print(c.call_tool('gecko_validate_circuit', {'circuit_path': 'docs/tutorials/thermal/heatsink-design.ipes'})); c.close()"

    # Run simulation
    python -c "from scripts.desktop.mcp_client import GeckoMcpClient; c = GeckoMcpClient(); c.start(); print(c.call_tool('gecko_simulate', {'circuit_path': 'docs/tutorials/thermal/heatsink-design.ipes', 'total_time': 0.01, 'time_step': 1e-6})); c.close()"
    ```

## Simulation Exercises

1. **Natural vs Forced Convection:** Change $R_{th,ha}$ from $0.8\text{ K/W}$ (forced air at $2\text{ m/s}$) to $2.5\text{ K/W}$ (natural convection). Observe the dramatic increase in $T_j$ exceeding the $150^\circ\text{C}$ maximum limit.
2. **Thermal Mass Dynamics:** Double $C_{th,h}$ to $50.0\text{ J/K}$ and observe how the heatsink temperature rate-of-rise slows down during a cold start.
3. **Transient Overload:** Apply a $2\times$ power pulse ($100\text{ W}$) for $100\text{ ms}$ and evaluate whether the thermal capacitances prevent $T_j$ from exceeding $T_{j,max}$.

## Related Resources

- [501 - Loss Calculation](loss-calculation.md) — Semiconductor loss calculation and thermal modeling
- [502 - Junction Temperature](junction-temperature.md) — Dynamic thermal impedance networks (Foster/Cauer)

