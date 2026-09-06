/*
 * gecko_c_block.h — interface contract for GeckoCIRCUITS NativeC blocks (typ 88).
 *
 * Implement this contract in C or C++ and build it as a host shared library
 * (Windows: .dll, Linux: .so, macOS: .dylib/.jnilib) with your own toolchain —
 * ideally the same one that builds your real microcontroller firmware, so the
 * simulation exercises the same control code you will flash to the device.
 *
 * GeckoCIRCUITS loads the library and calls, per simulation timestep:
 *
 *   gecko_step(xIN, n_in, yOUT, n_out, t, dt)
 *
 * with xIN[0..n_in-1] holding the block input signals and yOUT[0..n_out-1]
 * carrying the block output signals back into the simulation.
 *
 * C++ is fully supported: keep the three hooks extern "C" (the guard below
 * does this for you) and implement them in any C++ style you like. Exceptions
 * must NOT cross the boundary — catch them inside your hooks and write
 * fallback outputs.
 *
 * Build example (Windows / MSVC):
 *   cl /LD my_control.cpp /Fe:my_control.dll
 * (Linux):
 *   gcc -shared -fPIC -o my_control.so my_control.c
 * (macOS):
 *   clang -shared -fPIC -o my_control.dylib my_control.cpp
 */

#ifndef GECKO_C_BLOCK_H
#define GECKO_C_BLOCK_H

#if defined(_WIN32)
  #if defined(GECKO_BUILD_LIBRARY)
    #define GECKO_EXPORT __declspec(dllexport)
  #else
    #define GECKO_EXPORT __declspec(dllimport)
  #endif
#else
  #define GECKO_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Optional. Called once when a simulation run starts. Use it to reset
 * controller state (integral terms, filters, counters) so every simulation
 * run begins from a defined power-on state.
 */
GECKO_EXPORT void gecko_init(void);

/*
 * Required. Called once per simulation timestep.
 *
 *   xIN   input signal values, xIN[0] .. xIN[n_in-1]
 *   n_in  number of inputs (block's anzXIN)
 *   yOUT  output signal values to write, yOUT[0] .. yOUT[n_out-1]
 *   n_out number of outputs (block's anzYOUT)
 *   t     current simulation time in seconds
 *   dt    simulation timestep in seconds
 *
 * Keep this function fast and deterministic: it runs at every timestep.
 * Never block, never spawn threads that outlive the call, and do not call
 * back into GeckoCIRCUITS.
 */
GECKO_EXPORT void gecko_step(const double* xIN, int n_in,
                             double* yOUT, int n_out,
                             double t, double dt);

/*
 * Optional. Called once when the simulation run ends. Release any resources
 * your implementation allocated.
 */
GECKO_EXPORT void gecko_deinit(void);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* GECKO_C_BLOCK_H */
