---
title: Custom Control Blocks & NativeC
description: Custom control algorithms via NativeC C/C++ shared libraries, core Java calculators, and notes on legacy Java blocks
---

# 704 - Custom Control Blocks & NativeC

GeckoCIRCUITS provides two supported ways to integrate custom algorithmic control blocks into simulations, depending on whether you want **external compiled C/C++ firmware** or a **built-in Java calculator** in `gecko-simulation-core`:

| Approach | Best For | Documentation |
|----------|----------|---------------|
| **NativeC Block (`NATIVE_C`, typ 88)** | Running microcontroller C/C++ control firmware via shared libraries (`.dll` / `.so` / `.dylib`) without modifying GeckoCIRCUITS | [NativeC C/C++ Blocks](../../native-c-blocks.md) |
| **Core Java Calculator (`AbstractControlCalculatable`)** | Adding first-class built-in control blocks to `backend/gecko-simulation-core` | [Control Calculators API](../../api/core/calculators.md), [Developer Guide](../../resources/developer-guide.md) |

!!! warning "Legacy Classic UI Java Blocks (Retired)"
    Runtime in-GUI compilation of standalone `.java` files inheriting `AbstractControlBlock` belonged to the retired Classic Swing application (archived at git tag `classic-v1.0-final`). Current releases do not ship a runtime Java compiler or `AbstractControlBlock`; use **NativeC** (`NATIVE_C`) for user-supplied compiled blocks or **`SCRIPT_BLOCK`** (`typ 61`) for inline scripted control logic.

---

## Option 1: Firmware-in-the-Loop with NativeC (`NATIVE_C`)

With the **C Library Block** (`typ 88`), you compile your C or C++ control algorithm into a shared library against `gecko_c_block.h` using your own toolchain (`gcc`, `clang`, or MSVC). GeckoCIRCUITS loads it per simulation run using JDK 25's Foreign Function & Memory (FFM) API:

```c
#include "gecko_c_block.h"

void gecko_init(void) {
    /* Optional: reset controller state at t = 0 */
}

void gecko_step(const double* xIN, int n_in,
                double* yOUT, int n_out,
                double t, double dt) {
    if (n_out > 0 && n_in > 1) {
        double error = xIN[0] - xIN[1];
        yOUT[0] = 2.0 * error;
    }
}
```

See [NativeC C/C++ Blocks](../../native-c-blocks.md) for the full ABI specification (`gecko_init`, `gecko_step`, `gecko_deinit`), Windows/Linux/macOS build commands, and troubleshooting.

---

## Option 2: Core Simulation Java Calculators

When extending GeckoCIRCUITS itself with a new built-in block, control logic lives in `backend/gecko-simulation-core/src/main/java/gecko/core/control/calculators/` as a subclass of `AbstractControlCalculatable` (or `AbstractSingleInputSingleOutputCalculator` / `AbstractTwoInputsOneOutputCalculator`):

```java
package gecko.core.control.calculators;

public final class ClampGainCalculator extends AbstractSingleInputSingleOutputCalculator {
    private final double gain;
    private final double maxOutput;

    public ClampGainCalculator(final double gain, final double maxOutput) {
        super();
        this.gain = gain;
        this.maxOutput = maxOutput;
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        double y = gain * _inputSignal[0][0];
        _outputSignal[0][0] = Math.max(-maxOutput, Math.min(maxOutput, y));
    }
}
```

See [Control Calculators API](../../api/core/calculators.md) and the [Developer Guide](../../resources/developer-guide.md) for signal wiring, `InitializableAtSimulationStart`, and unit testing instructions.

---

## Historical Reference Models

Legacy Swing Java-block demonstration files from ETH Zurich remain in the repository for reference:

- `resources/tutorials/7xx_scripting_automation/704_java_blocks/demo_JAVA_Block.ipes`
- `resources/tutorials/7xx_scripting_automation/704_java_blocks/JavaBlockPMSM.ipes`

## Related Guides

- [NativeC C/C++ Blocks](../../native-c-blocks.md) — C/C++ firmware-in-the-loop blocks (`typ 88`)
- [Script Blocks & Legacy GeckoSCRIPT](geckoscript.md) — Inline control scripts (`typ 61`)
- [Python Integration](python.md) — External sweeps and `geckopy` SDK
- [MATLAB Integration](matlab.md) — REST API integration from MATLAB/Octave
