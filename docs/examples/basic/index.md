---
title: Basic Topologies Examples
description: Fundamental DC-DC converter circuits demonstrating essential power conversion principles
---

# Basic Topologies Examples

Fundamental DC-DC converter circuits demonstrating essential power conversion principles in GeckoCIRCUITS.

## Available Examples

| Example | Description | Difficulty | Documentation |
|---------|-------------|------------|---------------|
| [Buck Converter](buck.md) | Step-down DC-DC converter with current mode control | Beginner | [View Example](buck.md) |
| [Boost Converter](boost.md) | Step-up DC-DC converter with CCM/DCM operation | Beginner | [View Example](boost.md) |
| [Buck-Boost](../../tutorials/dcdc/buck-boost.md) | Inverting buck-boost, SEPIC, and Cuk topologies | Intermediate | [View Tutorial](../../tutorials/dcdc/buck-boost.md) |
| [Flyback](flyback.md) | Isolated step-up/down with coupled inductor storage | Intermediate | [View Example](flyback.md) |
| [Forward](forward.md) | Isolated step-down with tertiary reset winding | Intermediate | [View Example](forward.md) |

## Quick Reference

### Non-Isolated Topologies

| Topology | Conversion | Voltage Gain ($V_{out}/V_{in}$) | Key Characteristic |
|----------|------------|---------------------------------|-------------------|
| Buck | Step-down | $D$ | Continuous output current |
| Boost | Step-up | $\frac{1}{1-D}$ | Continuous input current |
| Buck-Boost | Inverting | $-\frac{D}{1-D}$ | Inverted polarity, step-up/down |
| Cuk | Inverting | $-\frac{D}{1-D}$ | Continuous input and output current |
| SEPIC | Non-inverting | $\frac{D}{1-D}$ | Non-inverting step-up/down |

### Isolated Topologies

| Topology | Conversion | Voltage Gain ($V_{out}/V_{in}$) | Power Range |
|----------|------------|---------------------------------|-------------|
| Flyback | Step-up/down | $\frac{N_s}{N_p} \cdot \frac{D}{1-D}$ | 5–150 W |
| Forward | Step-down | $\frac{N_s}{N_p} \cdot D$ | 50–500 W |

## Related Tutorials

- [Tutorial 201: Buck Converter](../../tutorials/dcdc/buck-converter.md)
- [Tutorial 202: Boost Converter](../../tutorials/dcdc/boost-converter.md)
- [Tutorial 203: Buck-Boost Topologies](../../tutorials/dcdc/buck-boost.md)
- [Tutorial 204: Flyback Converter](../../tutorials/dcdc/flyback.md)
- [Tutorial 205: Forward Converter](../../tutorials/dcdc/forward.md)
