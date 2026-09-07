// C++ NativeC fixture: a controller class with state (RAII + C++ statics)
// behind the extern-"C" gecko_c_block.h hooks. Proves the C++ path end to end:
// C++ statics power-on-reset per run (fresh library copy), C linkage symbols
// resolve, and exceptions are contained inside gecko_step.
#include "gecko_c_block.h"

#include <algorithm>

namespace {

class PidController {
public:
    void reset() {
        integral_ = 0.0;
        steps_ = 0;
    }

    double step(double input) {
        integral_ = std::max(-1e9, std::min(1e9, integral_ + input));
        ++steps_;
        return 2.0 * input + 0.25 * integral_;
    }

    long steps() const { return steps_; }

private:
    double integral_ = 0.0;
    long steps_ = 0;
};

PidController controller;  // constructs at library load = power-on reset

}  // namespace

extern "C" GECKO_EXPORT void gecko_init(void) {
    controller.reset();
}

extern "C" GECKO_EXPORT void gecko_step(const double* xIN, int n_in,
                                        double* yOUT, int n_out,
                                        double t, double dt) {
    static_cast<void>(t);
    static_cast<void>(dt);
    try {
        const double input = (n_in > 0) ? xIN[0] : 0.0;
        const double value = controller.step(input);
        if (n_out > 0) yOUT[0] = value;
        if (n_out > 1) yOUT[1] = static_cast<double>(controller.steps());
    } catch (...) {
        // exceptions must never cross the C boundary
        if (n_out > 0) yOUT[0] = 0.0;
    }
}

extern "C" GECKO_EXPORT void gecko_deinit(void) {
    controller.reset();
}
