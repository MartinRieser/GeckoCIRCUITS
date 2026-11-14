---
title: Core Module API
---

# Core Module API Reference

The `gecko-simulation-core` module provides GUI-free simulation classes for headless operation, REST APIs, and embedded use. All classes in this module are validated to have no GUI imports (`java.awt`, `javax.swing`).

## Package Overview

| Package | Files | Description |
|---------|-------|-------------|
| [`simulation`](simulation.md) | 10 | Headless simulation engine, zero-crossing detection |
| [`circuit.matrix`](matrix-stampers.md) | 17 | MNA matrix stampers and `MatrixAccumulator` |
| `circuit.netlist` | 5 | Netlist building and node indexing |
| `circuit.parameters` | 9 | Component parameter structures |
| [`control.calculators`](calculators.md) | 74 | Control block calculators (PI, PID, math, logic, SPWM) |
| `datacontainer` | 11 | Signal data storage and containers |
| `math` | 7 | Matrix operations, LU decomposition, FFT |

## Architecture

```
┌──────────────────────────────────────────────────┐
│              HeadlessSimulationEngine             │
│  ┌────────────────┐  ┌────────────────────────┐  │
│  │ SimulationConfig│  │  ZeroCrossingDetector  │  │
│  └────────────────┘  └────────────────────────┘  │
├──────────────────────────────────────────────────┤
│                  Circuit Layer                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │  Matrix   │  │ Netlist  │  │  Components  │   │
│  │ Stampers  │  │ Builder  │  │              │   │
│  └──────────┘  └──────────┘  └──────────────┘   │
├──────────────────────────────────────────────────┤
│               Control & Math Layer               │
│  ┌──────────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Calculators  │  │   Math   │  │   Data   │   │
│  │  (74 classes) │  │ (LU,FFT) │  │Container │   │
│  └──────────────┘  └──────────┘  └──────────┘   │
└──────────────────────────────────────────────────┘
```

## Maven Coordinates

```xml
<dependency>
    <groupId>gecko</groupId>
    <artifactId>gecko-simulation-core</artifactId>
    <version>1.0</version>
</dependency>
```

## Quality Metrics

- JaCoCo coverage enforcement: 30%+ instruction coverage (configured in root `pom.xml`)
- Build & test: `mvn test -pl backend/gecko-simulation-core`

## Related

- [Simulation Engine](simulation.md) - HeadlessSimulationEngine, ZeroCrossingDetector
- [Matrix Stampers](matrix-stampers.md) - IMatrixStamper, StamperRegistry
- [Control Calculators](calculators.md) - PI, PID, math, and logic blocks
