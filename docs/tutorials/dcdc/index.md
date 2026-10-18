---
title: DC-DC Converters
description: DC-DC power conversion topologies, simulation, and control
---

# DC-DC Converters

DC-DC power conversion topologies and control techniques in GeckoCIRCUITS.

## Available Tutorials

| Tutorial | Title | Difficulty | Project File | Description |
|----------|-------|------------|--------------|-------------|
| [201](buck-converter.md) | [Buck Converter](buck-converter.md) | Beginner | [buck-converter.ipes](buck-converter.ipes) | Step-down converter (24V $\to$ 12V / 50kHz), CCM/DCM modes, ripple calculation |
| [202](boost-converter.md) | [Boost Converter](boost-converter.md) | Intermediate | [boost-converter.ipes](boost-converter.ipes) | Step-up conversion (12V $\to$ 24V / 50kHz), inductor sizing, current ripple |
| [203](buck-boost.md) | [Buck-Boost Topologies](buck-boost.md) | Intermediate | [buck-boost-converter.ipes](buck-boost-converter.ipes) | Inverting buck-boost (24V $\to$ -16V / 50kHz), SEPIC, and Cuk topologies |
| [204](flyback.md) | [Flyback Converter](flyback.md) | Intermediate | [flyback-converter.ipes](flyback-converter.ipes) | Isolated buck-boost with coupled inductor energy storage and secondary rectifier |
| [205](forward.md) | [Forward Converter](forward.md) | Intermediate | [forward-converter.ipes](forward-converter.ipes) | Isolated step-down (48V $\to$ 16.8V / 50kHz) with transformer core demagnetization |

## Learning Objectives

- Understand step-down (buck) and step-up (boost) conversion principles
- Analyze continuous conduction mode (CCM) vs. discontinuous conduction mode (DCM)
- Compare non-isolated (buck, boost, SEPIC, Cuk) and isolated (flyback, forward) topologies
- Design closed-loop voltage and current feedback controllers
- Model component parasitics and evaluate efficiency

## Related Examples

- [Buck Converter Example](../../examples/basic/buck.md)
- [Boost Converter Example](../../examples/basic/boost.md)
- [Flyback Example](../../examples/basic/flyback.md)
- [Forward Example](../../examples/basic/forward.md)
