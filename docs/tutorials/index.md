---
title: GeckoCIRCUITS Tutorials
description: Comprehensive power electronics simulation tutorials
---

# GeckoCIRCUITS Tutorials

Comprehensive tutorials for learning power electronics simulation with GeckoCIRCUITS, organized by topic and difficulty level.

## Quick Start

**New to GeckoCIRCUITS?** Start with the [Getting Started series](../getting-started/index.md).

## Tutorial Series Overview

| Series | Topic | Highlights | Difficulty |
|--------|-------|------------|------------|
| **[Getting Started](../getting-started/index.md)** | Simulator Fundamentals | Installation, interface, first simulation, PWM | Beginner |
| **[DC-DC](dcdc/index.md)** | DC-DC Converters | Buck, Boost, Buck-Boost, Flyback, Forward | Beginner-Intermediate |
| **[AC-DC](acdc/index.md)** | AC-DC Rectifiers | Diode Rectifiers, Boost PFC, Vienna Rectifier | Intermediate |
| **[DC-AC](dcac/index.md)** | DC-AC Inverters | Single-Phase, Three-Phase VSI, NPC, MMC | Intermediate-Advanced |
| **[Thermal](thermal/index.md)** | Thermal Simulation | Conduction/switching loss, Tj calculation, heatsink design | Intermediate-Advanced |
| **[Magnetics](magnetics/index.md)** | Magnetics & Mechanical | Magnetic domain, transformer design, saturation, mechanical | Advanced |
| **[Scripting](scripting/index.md)** | Scripting & Automation | GeckoSCRIPT, MATLAB, Python, Java blocks | Intermediate-Advanced |
| **[EMI/EMC](emi/index.md)** | EMI & Filtering | CM and DM filter design, optimizer | Advanced |
| **[Advanced](advanced/index.md)** | Advanced Topologies | Matrix converters, PMSM motor drives, thyristor rectifiers | Advanced |

## Learning Paths

### Path 1: Power Electronics Fundamentals
For students and engineers new to power electronics simulation:

1. [First Simulation](../getting-started/first-simulation.md)
2. [Building Circuits](../getting-started/building-circuits.md)
3. [PWM Basics](../getting-started/pwm-basics.md)
4. [Running Simulations](../getting-started/running-simulations.md)
5. [Buck Converter](dcdc/buck-converter.md)
6. [Boost Converter](dcdc/boost-converter.md)
7. [Buck-Boost Topologies](dcdc/buck-boost.md)

### Path 2: Grid-Tied Power Conversion
For inverter and rectifier applications:

1. [Diode Rectifier](acdc/diode-rectifier.md)
2. [PFC Basics](acdc/pfc-basics.md)
3. [Vienna Rectifier](acdc/vienna-rectifier.md)
4. [Single-Phase Inverter](dcac/single-phase.md)
5. [Three-Phase Inverter](dcac/three-phase.md)
6. [NPC Multilevel Inverter](dcac/npc-inverter.md)

### Path 3: Thermal & Reliability Design
For thermal management and loss analysis:

1. [Buck Converter](dcdc/buck-converter.md)
2. [Loss Calculation](thermal/loss-calculation.md)
3. [Junction Temperature](thermal/junction-temperature.md)
4. [Heatsink Design](thermal/heatsink-design.md)
5. [Magnetic Domain](magnetics/magnetic-domain.md)

### Path 4: Automation & Scripting
For batch simulations, optimization, and external tool integration:

1. [Running Simulations](../getting-started/running-simulations.md)
2. [GeckoSCRIPT Basics](scripting/geckoscript.md)
3. [MATLAB Integration](scripting/matlab.md)
4. [Python Integration](scripting/python.md)
5. [Java Blocks](scripting/java-blocks.md)

## Complete Tutorial Index

### Getting Started
| Topic | Guide | Description |
|---|---|-------------|
| 101 | [First Simulation](../getting-started/first-simulation.md) | Launch, open, run, view results |
| 102 | [Building Circuits](../getting-started/building-circuits.md) | Component library, wiring |
| 103 | [PWM Basics](../getting-started/pwm-basics.md) | Duty cycle, carrier comparison |
| 104 | [Running Simulations](../getting-started/running-simulations.md) | Solvers, time step, export |
| 105 | [Analysis Tools](../getting-started/analysis-tools.md) | Oscilloscope, FFT, cursors |

### DC-DC Converters
| Topic | Guide | Description |
|---|---|-------------|
| 201 | [Buck Converter](dcdc/buck-converter.md) | Step-down, CCM/DCM |
| 202 | [Boost Converter](dcdc/boost-converter.md) | Step-up, RHP zero |
| 203 | [Buck-Boost Topologies](dcdc/buck-boost.md) | SEPIC, Cuk, inverting |
| 204 | [Flyback Converter](dcdc/flyback.md) | Isolated buck-boost, transformer storage |
| 205 | [Forward Converter](dcdc/forward.md) | Isolated step-down, core reset |

### AC-DC Rectifiers
| Topic | Guide | Description |
|---|---|-------------|
| 301 | [Diode Rectifiers](acdc/diode-rectifier.md) | Single/three-phase bridges |
| 302 | [PFC Basics](acdc/pfc-basics.md) | Boost PFC, current control |
| 303 | [Vienna Rectifier](acdc/vienna-rectifier.md) | Three-phase, three-level PFC |

### DC-AC Inverters
| Topic | Guide | Description |
|---|---|-------------|
| 401 | [Single-Phase Inverter](dcac/single-phase.md) | H-bridge PWM inverter |
| 402 | [Three-Phase Inverter](dcac/three-phase.md) | VSI, SPWM, and space-vector |
| 403 | [NPC Inverter](dcac/npc-inverter.md) | 3-level neutral-point-clamped |
| 404 | [MMC Converter](dcac/mmc-converter.md) | Modular multilevel for HVDC |

### Thermal Simulation
| Topic | Guide | Description |
|---|---|-------------|
| 501 | [Loss Calculation](thermal/loss-calculation.md) | Conduction and switching losses |
| 502 | [Junction Temperature](thermal/junction-temperature.md) | Thermal networks, Foster/Cauer models |
| 503 | [Heatsink Design](thermal/heatsink-design.md) | Thermal resistance budget, cooling |

### Magnetics & Mechanical
| Topic | Guide | Description |
|---|---|-------------|
| 901 | [Magnetic Domain](magnetics/magnetic-domain.md) | Permeance-capacitance analogy |
| 902 | [Transformer Design](magnetics/transformer-design.md) | High-frequency transformer modeling |
| 903 | [Inductor Saturation](magnetics/inductor-saturation.md) | Non-linear inductance characteristics |
| 904 | [Mechanical Systems](magnetics/mechanical-systems.md) | Motor-load electromechanical dynamics |

### Scripting & Automation
| Topic | Guide | Description |
|---|---|-------------|
| 701 | [GeckoSCRIPT Basics](scripting/geckoscript.md) | Built-in JavaScript automation |
| 702 | [MATLAB Integration](scripting/matlab.md) | Scripting and co-simulation |
| 703 | [Python Integration](scripting/python.md) | Parameter sweeps with NumPy/SciPy |
| 704 | [Java Blocks](scripting/java-blocks.md) | Custom algorithmic components |

## Related Resources

- [**Examples**](../examples/index.md) — Complete circuit applications
- [**Articles**](../articles/index.md) — Technical papers and design notes
- [**API Reference**](../api/index.md) — REST API and automation endpoints
