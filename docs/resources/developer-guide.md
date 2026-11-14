---
title: Developer Guide
description: How to extend GeckoCIRCUITS with new control calculators, MNA matrix stampers, and frontend/API features
---

# Developer Guide

This guide explains how to extend GeckoCIRCUITS across the simulation core (`backend/gecko-simulation-core`), REST API (`backend/gecko-rest-api`), MCP server (`backend/gecko-mcp`), frontend editor (`frontend/`), and desktop shell (`desktop/`).

For high-level system architecture and build commands, see the [Architecture Guide](../architecture.md).

---

## Repository Modules

| Module | Stack | Purpose |
|--------|-------|---------|
| `backend/gecko-simulation-core/` | Java 25 | GUI-free MNA simulation engine, stampers, calculators, `.ipes` I/O, DRC, NativeC FFM |
| `backend/gecko-rest-api/` | Spring Boot 4.1.1 | HTTP REST, SSE, STOMP WebSocket API, and embedded static frontend |
| `backend/gecko-mcp/` | Java 25, MCP SDK | Stdio JSON-RPC 2.0 MCP server (16 tools) and vector SVG schematic exporter |
| `frontend/` | React 19, TypeScript, Vite | Schematic canvas, oscilloscope plots, FFT, loss analysis, circuit DRC UI |
| `desktop/` | Rust, Tauri 2 | Native desktop wrapper managing the bundled `jlink` runtime and OS file hooks |
| `geckopy/` | Python 3 | High-level Python SDK over `gecko-mcp.jar` |

### Core Engine Extension Points (`gecko-simulation-core`)

| Domain | Package | Key Interface / Base Class |
|--------|---------|----------------------------|
| **Control Calculators** | `gecko.core.control.calculators` | `AbstractControlCalculatable`, `InitializableAtSimulationStart` |
| **Circuit Stampers** | `gecko.core.circuit.matrix` | `IMatrixStamper`, `IStatefulStamper`, `StamperRegistry` |
| **Solvers & Coupling** | `gecko.core.simulation`, `gecko.core.simulation.solver` | `HeadlessSimulationEngine`, `MatrixSolver`, `DomainCoupler` |
| **Thermal / Losses** | `gecko.core.circuit.losscalculation` | `VerlustBerechnungDetailed`, `LossFileAccessor` |

---

## Adding a Control Block Calculator

Control calculators process wired input signals and produce output values on each simulation step in `gecko.core.control.calculators`:

- **Base class**: `AbstractControlCalculatable(int noInputs, int noOutputs)` (or package-local helpers such as `AbstractSingleInputSingleOutputCalculator`).
- **Step execution**: Override `public void calculateYOUT(final double deltaT)`.
- **Signal arrays**: Inputs sit in `_inputSignal[port][0]` and outputs are written to `_outputSignal[port][0]`.
- **Initialization**: Implement `InitializableAtSimulationStart` (`initializeAtSimulationStart(final double deltaT)`) when a block needs state reset at `t = 0`.

### Example: Gain Amplifier (`GainCalculator`)

```java
package gecko.core.control.calculators;

public final class GainCalculator extends AbstractSingleInputSingleOutputCalculator {
    private double _gain;

    public GainCalculator(final double gain) {
        super();
        setGain(gain);
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        _outputSignal[0][0] = _gain * _inputSignal[0][0];
    }

    public void setGain(final double gain) {
        _gain = gain;
    }
}
```

### Example: Stateful Integrator Pattern

```java
package gecko.core.control.calculators;

public class SimpleIntegratorCalculator extends AbstractSingleInputSingleOutputCalculator
        implements InitializableAtSimulationStart {

    private double state;

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        state = 0.0;
        _outputSignal[0][0] = 0.0;
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        state += _inputSignal[0][0] * deltaT;
        _outputSignal[0][0] = state;
    }
}
```

### Writing Calculator Unit Tests

```java
import gecko.core.control.calculators.GainCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GainCalculatorTest {
    @Test
    void scalesInputSignal() {
        GainCalculator calc = new GainCalculator(2.5);
        calc._inputSignal[0][0] = 4.0;
        calc.calculateYOUT(1e-6);
        assertEquals(10.0, calc._outputSignal[0][0], 1e-12);
    }
}
```

---

## Adding a Circuit Matrix Stamper

Circuit elements stamp Modified Nodal Analysis (**A · x = b**) contributions into `MatrixAccumulator` (wrapped around dense or sparse solver matrices) and vector `b`:

### `IMatrixStamper` & `IStatefulStamper`

- `stampMatrixA(MatrixAccumulator a, int nodeX, int nodeY, int nodeZ, double[] parameter, double dt)`
- `stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ, double[] parameter, double dt, double time, double[] previousValues)`
- `calculateCurrent(double nodeVoltageX, double nodeVoltageY, double[] parameter, double dt, double previousCurrent)`
- `getAdmittanceWeight(double parameterValue, double dt)`

Switching elements (diodes, ideal switches, IGBTs, MOSFETs, thyristors) implement `IStatefulStamper` (`updateState`, `isStateChanged`, `resetStateChange`, `isOn`, `setState`, `getCurrentResistance`).

### Example: Resistor Stamper Pattern

```java
package gecko.core.circuit.matrix;

public class CustomResistorStamper implements IMatrixStamper {
    @Override
    public void stampMatrixA(final MatrixAccumulator a, final int nodeX, final int nodeY,
                             final int nodeZ, final double[] parameter, final double dt) {
        double admittance = getAdmittanceWeight(parameter[0], dt);
        a.add(nodeX, nodeX, admittance);
        a.add(nodeY, nodeY, admittance);
        a.add(nodeX, nodeY, -admittance);
        a.add(nodeY, nodeX, -admittance);
    }

    @Override
    public void stampVectorB(final double[] b, final int nodeX, final int nodeY, final int nodeZ,
                             final double[] parameter, final double dt, final double time,
                             final double[] previousValues) {
        // Purely passive element
    }

    @Override
    public double calculateCurrent(final double nodeVoltageX, final double nodeVoltageY,
                                   final double[] parameter, final double dt,
                                   final double previousCurrent) {
        return (nodeVoltageX - nodeVoltageY) / parameter[0];
    }

    @Override
    public double getAdmittanceWeight(final double parameterValue, final double dt) {
        return 1.0 / parameterValue;
    }
}
```

Register stampers in `StamperRegistry` keyed by `CircuitTypCore` (for example `StamperRegistry.createDefault(SolverType.SOLVER_BE)`).

### Writing Stamper Unit Tests

```java
import gecko.core.circuit.matrix.DenseMatrixAccumulator;
import gecko.core.circuit.matrix.ResistorStamper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomResistorStamperTest {
    @Test
    void stampsAdmittanceEntries() {
        ResistorStamper stamper = new ResistorStamper();
        double[][] matrix = new double[3][3];
        DenseMatrixAccumulator acc = new DenseMatrixAccumulator(matrix, 3);

        stamper.stampMatrixA(acc, 0, 1, 2, new double[]{100.0}, 1e-6);

        assertEquals(0.01, matrix[0][0], 1e-12);
        assertEquals(0.01, matrix[1][1], 1e-12);
        assertEquals(-0.01, matrix[0][1], 1e-12);
        assertEquals(-0.01, matrix[1][0], 1e-12);
    }
}
```

---

## Quality Gates & Local Verification

### Backend (`backend/`)

```bash
# Build, unit/integration tests, GUI-free enforcer check, and >=30% core JaCoCo coverage gate
mvn clean verify

# View JaCoCo coverage report
# backend/gecko-simulation-core/target/site/jacoco/index.html

# Checkstyle & PMD checks (failOnViolation enabled on gecko-mcp and gecko-rest-api)
mvn checkstyle:check pmd:check \
  -D"checkstyle.config.location=../../checkstyle.xml" \
  -D"pmd.rulesets=../../pmd-ruleset.xml" \
  -pl backend/gecko-mcp,backend/gecko-rest-api
```

### Frontend, Desktop & Python

```bash
# Frontend (React / TypeScript)
npm --prefix frontend run lint
npx --prefix frontend tsc -p frontend/tsconfig.json --noEmit
npm --prefix frontend test

# Desktop shell (Rust / Tauri 2)
cargo fmt --manifest-path desktop/Cargo.toml --all --check
cargo clippy --manifest-path desktop/Cargo.toml --workspace -- -D warnings
cargo test --manifest-path desktop/Cargo.toml -p gecko-engine

# Python SDK & tooling tests
python -m unittest discover -s tests

# Documentation strict link & build check
mkdocs build --strict
```

---

## Coding Conventions

- Keep `backend/gecko-simulation-core` strictly free of UI toolkit dependencies (`maven-enforcer-plugin` enforces this boundary).
- Add Javadoc comments to public API classes, constructors, and methods; explain non-obvious physics or solver constraints inline.
- Avoid magic numbers in component models, canvas geometry, and stampers — extract named constants.
- Follow `checkstyle.xml` and `pmd-ruleset.xml` rules for new code in `backend/gecko-mcp` and `backend/gecko-rest-api`.

## Next Steps

- [Core Simulation Engine API](../api/core/index.md) — Simulation loop, stampers, and calculators
- [REST API Reference](../api/rest-api.md) — HTTP, SSE, and STOMP endpoints under `/gecko`
- [MCP Interface](../mcp.md) — Tool schemas and `geckopy` integration
- [Desktop Application](../desktop-app.md) — Bundled Tauri 2 architecture and CI matrix
