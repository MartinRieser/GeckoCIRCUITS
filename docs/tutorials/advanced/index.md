---
title: Advanced Topics
description: Complex converter topologies, motor drives, and optimization techniques
---

# Advanced Topics

Complex power electronic topologies, advanced motor drives, and optimization in GeckoCIRCUITS.

## Overview

| Section | Title | Difficulty | Reference |
|---------|-------|------------|-----------|
| [801](#801-matrix-converters) | Matrix Converters | Advanced | [AC/AC Conversion Article](../../articles/20090611-acac-part-1.md) |
| [802](#802-motor-drives-pmsm) | Motor Drives (PMSM) | Advanced | [PMSM FOC Example](../../examples/motor-drives/pmsm-foc.md) |
| [803](#803-optimization) | Optimization | Advanced | [Swiss Rectifier Article](../../articles/20120705-the-swiss-rectifier.md) |
| [804](#804-thyristor-control) | Thyristor Control | Advanced | Phase-controlled rectifiers |

## Learning Objectives

- Understand direct AC-AC conversion with sparse and matrix converters
- Implement field-oriented control (FOC) for permanent magnet synchronous motors (PMSM)
- Optimize converter designs using automated scripting workflows
- Control thyristor-based line-commutated rectifiers and evaluate commutation margins

## Contents

### 801 - Matrix Converters
- Direct AC-AC conversion without bulky electrolytic DC-link capacitors
- Venturini and space-vector modulation algorithms
- Thermal analysis under varying output frequencies

### 802 - Motor Drives (PMSM)
- Field-oriented current and torque control ($i_d = 0$ and MTPA)
- Resolver and encoder feedback modeling
- Inverter dead-time effects and compensation

### 803 - Optimization
- Automated multi-objective design optimization
- Pare-to-front trade-offs between efficiency, volume, and THD
- Swiss rectifier topology optimization

### 804 - Thyristor Control
- Line-commutated single-phase and three-phase thyristor bridges
- Firing angle control ($\alpha$) and commutation overlap ($\mu$)
- Inverter operation and loss-of-commutation protection

## Related Resources

- [DC-AC Inverters Tutorial Series](../dcac/index.md)
- [Motor Drive Examples](../../examples/motor-drives/index.md)
- [Technical Articles Archive](../../articles/index.md)
