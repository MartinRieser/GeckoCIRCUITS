# NativeC Blocks — C/C++ Firmware-in-the-Loop

The **C Library Block** (control block, typ 88) lets you run your real
microcontroller control code inside a GeckoCIRCUITS simulation *before* flashing
it to the device. You write the control algorithm in C or C++, build it as a
host shared library with your own toolchain (the same one that targets your
MCU), and the simulation calls it once per timestep — exactly like the control
ISR on the real hardware.

GeckoCIRCUITS **never compiles C for you** and never needs a C compiler: it
only *loads* the shared library you built. That keeps the toolchain ownership
where it belongs — with your firmware project.

---

## The interface contract

The repository ships the header [`gecko_c_block.h`](https://github.com/MartinRieser/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/resources/gecko/nativec/gecko_c_block.h)
(it is also copied into every desktop installation under `engine/gecko_c_block.h`).
Include it in your project; it provides the `GECKO_EXPORT` macro and the
`extern "C"` guards so the same file works from C and C++.

You implement up to three functions:

| Function | Required | Called |
|----------|----------|--------|
| `gecko_init(void)` | optional | once when a simulation run starts — reset controller state |
| `gecko_step(const double* xIN, int n_in, double* yOUT, int n_out, double t, double dt)` | **yes** | once per simulation timestep |
| `gecko_deinit(void)` | optional | once when the simulation run ends |

`xIN[0..n_in-1]` carries the block's input signals, `yOUT[0..n_out-1]` is where
you write the outputs; `t` is the simulation time and `dt` the timestep, both
in seconds.

**Full header** (copy this as `gecko_c_block.h` if you don't have the
repository or installation at hand):

```c
#ifndef GECKO_C_BLOCK_H
#define GECKO_C_BLOCK_H

/*
 * This header exists to build the library, so exports are unconditional on
 * Windows (dllexport). Never dllimport: users define these functions here.
 */
#if defined(_WIN32)
  #define GECKO_EXPORT __declspec(dllexport)
#else
  #define GECKO_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

GECKO_EXPORT void gecko_init(void);

GECKO_EXPORT void gecko_step(const double* xIN, int n_in,
                             double* yOUT, int n_out,
                             double t, double dt);

GECKO_EXPORT void gecko_deinit(void);

#ifdef __cplusplus
}
#endif

#endif /* GECKO_C_BLOCK_H */
```

## C example

```c
#include "gecko_c_block.h"

static double integral = 0.0;   /* statics persist across timesteps */

void gecko_init(void) {
    integral = 0.0;             /* power-on reset, called every run */
}

void gecko_step(const double* xIN, int n_in,
                double* yOUT, int n_out,
                double t, double dt) {
    integral += xIN[0] * dt;
    if (n_out > 0) yOUT[0] = integral;
}

void gecko_deinit(void) { }
```

## C++ example

C++ is fully supported: the boundary is a C ABI, everything behind it can be
classes, templates and STL. Three rules:

1. Keep the hooks `extern "C"` (the header's guard handles it).
2. Never let an exception escape `gecko_step` — catch it and write fallback
   outputs. An exception crossing the boundary is undefined behavior.
3. C++ globals/statics construct at library load — with GeckoCIRCUITS loading
   a fresh copy of your library per simulation run, they construct exactly
   once per run: power-on-reset semantics like the real device.

```cpp
#include "gecko_c_block.h"
#include <algorithm>

namespace {
class PiController {
public:
    void reset() { integral_ = 0.0; }
    double step(double error, double dt) {
        integral_ = std::clamp(integral_ + error * dt, -100.0, 100.0);
        return kp_ * error + ki_ * integral_;
    }
private:
    double kp_ = 0.5, ki_ = 40.0, integral_ = 0.0;
};
PiController ctrl;   // constructs fresh per simulation run
}

extern "C" GECKO_EXPORT void gecko_init(void) { ctrl.reset(); }

extern "C" GECKO_EXPORT void gecko_step(const double* xIN, int n_in,
                                        double* yOUT, int n_out,
                                        double t, double dt) {
    try {
        if (n_out > 0) yOUT[0] = ctrl.step(n_in > 0 ? xIN[0] : 0.0, dt);
    } catch (...) {
        if (n_out > 0) yOUT[0] = 0.0;   // never throw across the boundary
    }
}

extern "C" GECKO_EXPORT void gecko_deinit(void) { }
```

## Building the library

| Toolchain | Command |
|-----------|---------|
| MSVC | `cl /LD /EHsc my_control.cpp /Fe:my_control.dll` |
| MinGW gcc | `gcc -shared -fPIC -o my_control.dll my_control.c` |
| g++ (C++) | `g++ -shared -fPIC -o my_control.dll my_control.cpp` |
| Linux gcc/clang | `gcc -shared -fPIC -o my_control.so my_control.c` |
| macOS clang++ | `clang++ -shared -fPIC -o my_control.dylib my_control.cpp` |

On Windows the file must be 64-bit to match the application. If your firmware
is C++, compile the *same* control source files you target the MCU with —
only the hardware-access layer is replaced by the three exported functions.

## Using the block in the web GUI

1. Open the **control palette** and place a **C Library Block (NativeC)**
   (the palette entry `C_NATIVE_C_FUNCTION`, typ 88).
2. In the properties panel set **Inputs** and **Outputs** — these are the
   `n_in` / `n_out` your `gecko_step` expects — and the **Library Path**.
   The path may be absolute or relative to the circuit file.
3. Wire the inputs to probes/signal sources and the outputs to gates or
   scopes, exactly like a script block.
4. Run the simulation.

Behavior details worth knowing:

- **Per-run power-on reset**: each run loads a fresh copy of your library, so
  C/C++ statics (and anything constructed at load time) restart from zero —
  matching device power-on.
- **Rebuild freely**: your original `.dll`/`.so` is never locked. Recompile
  the firmware and simply run again; the next run picks up the new build.
- **Validation**: the DRC validator flags blocks with no library configured
  (`NATIVEC_NO_LIBRARY`).

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| Outputs stay constant, engine log shows "cannot load ..." | Library path wrong, or a dependency of your library is missing (e.g. built for the wrong architecture — must be 64-bit) |
| "required symbol gecko_step not found" | Functions not exported (Windows: add `GECKO_EXPORT` / `__declspec(dllexport)`), or C++ symbols without `extern "C"` |
| Simulation crashes at step | An exception escaped `gecko_step` — wrap the body in `try/catch` (C++) and never `throw` across the boundary |
| Outputs frozen after a change | The library failed to load *this* run — check the engine log
  (`%APPDATA%\com.geckocircuits.desktop\logs\engine\` on Windows) |

Engine logs are described in the [Desktop App guide](desktop-app.md).

## Security & MCP notes

- The bundled MCP server can **run and validate** circuits that contain C
  library blocks, but it deliberately **cannot create or modify** them —
  prompt-injection into native code is excluded by design. Place and
  configure the block in the GUI.
- Your library runs in-process with the simulation: only load libraries you
  built yourself.
