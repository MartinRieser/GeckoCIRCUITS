---
title: Troubleshooting
---

# Troubleshooting

Common issues and solutions when running GeckoCIRCUITS desktop installers, building from source, or debugging simulations.

## Startup & Runtime Issues

### Desktop app fails to start engine

In installed desktop builds, the Tauri shell spawns `gecko-rest-api.jar` on a bundled JDK 25 runtime and waits up to 90 seconds for `GECKO_READY`. If startup times out:

1. Open the engine log directory (`Help → Open Engine Logs`, or `<app log dir>/engine/`):
   - Windows: `%LOCALAPPDATA%\com.geckocircuits.desktop\logs\engine\`
   - macOS: `~/Library/Logs/com.geckocircuits.desktop/engine/`
   - Linux: `~/.local/share/com.geckocircuits.desktop/logs/engine/`
2. Inspect `engine.log` (shell launcher output) and `rest-api.log` (Spring Boot server log).
3. See [Desktop Application](../desktop-app.md#troubleshooting) for OS Gatekeeper/SmartScreen notes and WebView requirements.

### "Unsupported class file major version 69" when running from source

GeckoCIRCUITS compiles targeting **Java 25** (bytecode major version 69). Ensure `java -version` and `JAVA_HOME` point to JDK 25+ (for example [Eclipse Temurin 25](https://adoptium.net/)).

### OutOfMemoryError on large or long simulations

When launching the standalone REST API JAR directly with a large channel count or small time step, increase the heap limit:

```bash
java -Xmx4G -jar backend/gecko-rest-api/target/gecko-rest-api.jar
```

## Simulation Issues

### Simulation runs but produces unexpected results

| Check | Solution |
|-------|----------|
| Time step too large | Reduce `dt` below `1 / (100 × f_switch)` |
| Numerical ringing | Switch from Trapezoidal to **Backward Euler** |
| Missing ground | Add a ground terminal (`LK_GLOBAL_TERMINAL`) to every electrical island |
| Open nodes / floating wires | Run DRC validation (`gecko_validate_circuit` or editor validation) |

### Simulation diverges / `NaN` values

Usually caused by:

1. **Algebraic loops or ideal voltage-source parallels** — Add small series resistance (e.g., `1 mΩ`) to ideal voltage sources or small damping elements.
2. **Zero-valued parameters** — Check that resistances, inductances, and capacitances are strictly positive where required.
3. **Time step too large** for fast switching transitions.

### Simulation is slow

- **Reduce simulation duration (`tDURATION`)** while tuning topology or controller gains.
- **Increase time step (`dt`)** as much as switching accuracy allows.
- **Adjust max plot points / signals recorded** when fetching waveforms via MCP/REST.

## Build Issues

### Maven build or test failures

```bash
# Clean rebuild and run all backend unit/integration tests
mvn clean verify
```

Ensure `mvn -version` shows **Java 25** and **Maven 3.8+**.

### Frontend lint / type / test checks

```bash
npm --prefix frontend ci
npm --prefix frontend run lint
npx --prefix frontend tsc -p frontend/tsconfig.json --noEmit
npm --prefix frontend test
```

### Desktop Rust crate checks

On Windows, make sure the MSVC toolchain is installed (`rustup toolchain install stable-x86_64-pc-windows-msvc`):

```bash
cargo fmt --manifest-path desktop/Cargo.toml --all --check
cargo clippy --manifest-path desktop/Cargo.toml --workspace -- -D warnings
cargo test --manifest-path desktop/Cargo.toml -p gecko-engine
```

## File Issues

### Can't open an `.ipes` file

- Check whether the file is valid UTF-8 `.ipes` text (or a valid gzip-compressed `.ipes` archive).
- Inspect structural errors via the REST validation endpoint (`GET /gecko/api/v1/circuits/{circuitId}/validate`) or `geckopy` (`ckt.validate()`).

## Still Stuck?

1. Check the [FAQ](faq.md)
2. Search [existing issues](https://github.com/MartinRieser/GeckoCIRCUITS/issues)
3. Open a [new issue](https://github.com/MartinRieser/GeckoCIRCUITS/issues/new) with your OS, Java version, steps to reproduce, and `engine.log` / `rest-api.log` excerpts.
