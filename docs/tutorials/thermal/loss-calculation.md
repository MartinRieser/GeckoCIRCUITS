---
title: "501 - Loss Calculation"
---

# 501 - Loss Calculation

Semiconductor conduction and switching loss modeling.

## Overview

Accurate loss calculation is essential for:
- Thermal design and heatsink sizing
- Efficiency optimization
- Component selection
- Reliability prediction

## Loss Components

### Total Semiconductor Losses

$$P_{total} = P_{cond} + P_{sw}$$

### Conduction Losses

**MOSFET:**

$$P_{cond,MOS} = I_{rms}^2 \cdot R_{DS(on)}$$

**IGBT:**

$$P_{cond,IGBT} = V_{CE(sat)} \cdot I_{avg} + r_{CE} \cdot I_{rms}^2$$

**Diode:**

$$P_{cond,D} = V_F \cdot I_{avg} + r_D \cdot I_{rms}^2$$

### Switching Losses

$$P_{sw} = (E_{on} + E_{off}) \cdot f_{sw}$$

Where switching energies depend on:
- Current at switching instant
- Voltage being switched
- Temperature (increases with temp)

## Temperature Dependence

### RDS(on) vs Temperature

$$R_{DS(on)}(T) = R_{DS(on)}(25°C) \cdot \left[1 + \alpha(T - 25)\right]$$

Typical α ≈ 0.004 to 0.006 /°C (doubles at 150°C)

### Switching Energy vs Temperature

$$E_{sw}(T) = E_{sw}(25°C) \cdot \left[1 + \beta(T - 25)\right]$$

## GeckoCIRCUITS Loss Models

### Setting Up Loss Parameters

1. **Open semiconductor properties**
2. **Enable thermal model**
3. **Enter datasheet values:**
   - RDS(on) or VCE(sat)
   - Eon, Eoff at reference conditions
   - Temperature coefficients

### Data Entry Table

| Parameter | MOSFET | IGBT | Diode |
|-----------|--------|------|-------|
| V_on | - | VCE(sat) | VF |
| R_on | RDS(on) | rCE | rD |
| E_on | Eon | Eon | Qrr×V |
| E_off | Eoff | Eoff | - |
| E_rr | - | - | Err |

## Example: Buck Converter Losses

### High-Side MOSFET

**Conduction:**

$$P_{cond,HS} = D \cdot I_L^2 \cdot R_{DS(on)}$$

**Switching:**

$$P_{sw,HS} = \frac{1}{2}V_{in} \cdot I_L \cdot (t_r + t_f) \cdot f_{sw}$$

### Low-Side Diode/MOSFET

**Conduction:**

$$P_{cond,LS} = (1-D) \cdot I_L \cdot V_F$$

### Inductor Losses

**DC Copper Loss:**

$$P_{Cu,DC} = I_{DC}^2 \cdot R_{DC}$$

**AC Copper Loss (skin effect):**

$$P_{Cu,AC} = I_{AC,rms}^2 \cdot R_{AC}$$

**Core Loss (Steinmetz):**

$$P_{core} = k \cdot f^{\alpha} \cdot B^{\beta} \cdot V_{core}$$

## Simulation Project: Buck-Boost Loss Model

The companion project file [`loss-calculation.ipes`](loss-calculation.ipes) implements an authentic buck-boost converter with active semiconductor loss calculation and electro-thermal feedback in GeckoCIRCUITS.

### Circuit & Simulation Specifications

| Parameter | Value | Description |
| :--- | :--- | :--- |
| **Circuit File** | [`loss-calculation.ipes`](loss-calculation.ipes) | Complete validated simulation model |
| **Input Voltage ($V_{in}$)** | $300\text{ V DC}$ | High-voltage DC rail |
| **Switching Frequency ($f_{sw}$)** | $20\text{ kHz}$ | Period $T_{sw} = 50\,\mu\text{s}$ |
| **Duty Cycle ($D$)** | $0.40$ | Gate drive command |
| **Switch Device (`IGBT.1`)** | $R_{on} = 44\,\text{m}\Omega$, $V_f = 0.8\text{ V}$, $k_{on} = 97\,\mu\text{J/A}$, $k_{off} = 120\,\mu\text{J/A}$ | Thermal loss calculator `Pv.1` coupled to thermal network |
| **Diode Device (`D.1`)** | $R_{on} = 33\,\text{m}\Omega$, $V_f = 0.8\text{ V}$ | Thermal loss calculator `Pv.2` |
| **Thermal Network** | 4-layer Foster RC network | $R_{th,jc}$ chain coupled to heatsink $R_{th,ha} = 0.8\text{ K/W}$ |
| **Ambient Temperature ($T_a$)** | $25^\circ\text{C}$ | Thermal voltage source `Temp.1` |
| **Simulation Step ($\Delta t$)** | $1.0\,\mu\text{s}$ | 10,001 time steps |
| **Total Duration** | $0.01\text{ s}$ ($10\text{ ms}$) | Captures switching cycle losses and initial thermal transient |

### Monitored Signals

| Signal | Domain | Expected Value | Description |
| :--- | :--- | :--- | :--- |
| `Tj` | Thermal | Rises from $25^\circ\text{C}$ | Semiconductor junction temperature at die layer |
| `TSink` | Thermal | $\approx 25^\circ\text{C} \to 28^\circ\text{C}$ | Heatsink surface temperature |
| `v_sw` | Power | $0\text{ V} \leftrightarrow 300\text{ V}$ | Switch voltage across `IGBT.1` |
| `i_sw` | Power | Triangular current | Conduction current through switch |
| `P_loss_total` | Loss | Average loss power | Total dissipated electrical power converted to heat [W] |

### How to Run Simulation

=== "GeckoCIRCUITS GUI"
    1. Open `loss-calculation.ipes` in GeckoCIRCUITS desktop.
    2. Click **Start Simulation** (or press **F5**).
    3. Open the **Scope** window and observe `Tj` temperature trajectory alongside switching pulses.

=== "MCP Interface (Command Line)"
    ```bash
    # Validate circuit integrity
    python -c "from scripts.desktop.mcp_client import GeckoMcpClient; c = GeckoMcpClient(); c.start(); print(c.call_tool('gecko_validate_circuit', {'circuit_path': 'docs/tutorials/thermal/loss-calculation.ipes'})); c.close()"

    # Run simulation
    python -c "from scripts.desktop.mcp_client import GeckoMcpClient; c = GeckoMcpClient(); c.start(); print(c.call_tool('gecko_simulate', {'circuit_path': 'docs/tutorials/thermal/loss-calculation.ipes', 'total_time': 0.01, 'time_step': 1e-6})); c.close()"
    ```

## Simulation Exercises

1. Measure losses at different load currents ($10\text{ A}$ vs $30\text{ A}$) and plot the quadratic conduction loss curve.
2. Compare switching losses at $20\text{ kHz}$ vs $50\text{ kHz}$ to verify the linear frequency scaling $P_{sw} \propto f_{sw}$.
3. Analyze the loss breakdown between `IGBT.1` and freewheeling diode `D.1`.
4. Correlate total dissipated power with the resulting steady-state junction temperature rise: $\Delta T_j = P_{loss} \cdot R_{th,total}$.

## Related Resources

- [502 - Junction Temperature](junction-temperature.md) — Dynamic thermal impedance networks (Foster/Cauer)
- [503 - Heatsink Design](heatsink-design.md) — Thermal resistance budgeting and cooling system selection

