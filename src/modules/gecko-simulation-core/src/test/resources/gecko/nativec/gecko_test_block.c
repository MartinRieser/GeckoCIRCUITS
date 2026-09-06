/*
 * gecko_c_block.h test fixture: doubles the first input into the first
 * output, counts steps in a second output (yOUT[1] if present).
 * Built at test time into a host shared library.
 */
#include <math.h>

static long stepCount = 0;

void gecko_init(void) {
    stepCount = 0;
}

void gecko_step(const double* xIN, int n_in,
                double* yOUT, int n_out,
                double t, double dt) {
    (void) dt;
    if (n_out > 0) {
        yOUT[0] = (n_in > 0) ? 2.0 * xIN[0] : 0.0;
    }
    if (n_out > 1) {
        stepCount += 1;
        yOUT[1] = (double) stepCount;
    }
}

void gecko_deinit(void) {
    stepCount = 0;
}
