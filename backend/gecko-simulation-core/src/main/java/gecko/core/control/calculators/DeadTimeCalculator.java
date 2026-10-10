package gecko.core.control.calculators;

/**
 * Complementary gate driver with dead-time insertion for half-bridge legs.
 *
 * <p><strong>Input:</strong></p>
 * <ul>
 *   <li>Signal 0: {@code PWM} - logic signal (0.0 or 1.0)</li>
 * </ul>
 *
 * <p><strong>Outputs:</strong></p>
 * <ul>
 *   <li>Signal 0: {@code G_hi} - high-side switch gate signal with delayed turn-on</li>
 *   <li>Signal 1: {@code G_lo} - low-side switch gate signal with delayed turn-on</li>
 * </ul>
 *
 * <p>When PWM switches high, low-side turns off immediately; high-side turns on after {@code t_dead}.
 * When PWM switches low, high-side turns off immediately; low-side turns on after {@code t_dead}.
 * During switching transients, both outputs are guaranteed 0.0 for duration {@code t_dead}.</p>
 */
public final class DeadTimeCalculator extends AbstractControlCalculatable {

    private final double deadTime;
    private double riseTimer = 1e9;
    private double fallTimer = 1e9;
    private double lastPwm = 0.0;
    private boolean initialized = false;

    public DeadTimeCalculator(final double deadTime) {
        super(1, 2);
        this.deadTime = Math.max(0.0, deadTime);
    }

    public double getDeadTime() {
        return deadTime;
    }

    @Override
    public void calculateYOUT(final double deltaT) {
        final double pwm = (_inputSignal[0] != null && _inputSignal[0].length > 0 && _inputSignal[0][0] > 0.5)
                ? 1.0 : 0.0;

        if (!initialized) {
            lastPwm = pwm;
            if (pwm > 0.5) {
                riseTimer = deadTime;
                fallTimer = 0.0;
            } else {
                fallTimer = deadTime;
                riseTimer = 0.0;
            }
            initialized = true;
        } else {
            if (pwm > 0.5 && lastPwm <= 0.5) {
                // Rising edge: low-side turns off immediately, start high-side dead-time timer
                riseTimer = 0.0;
            } else if (pwm <= 0.5 && lastPwm > 0.5) {
                // Falling edge: high-side turns off immediately, start low-side dead-time timer
                fallTimer = 0.0;
            } else {
                riseTimer += deltaT;
                fallTimer += deltaT;
            }
            lastPwm = pwm;
        }

        final double eps = 1e-12;
        final boolean hi = (pwm > 0.5) && ((riseTimer + eps) >= deadTime);
        final boolean lo = (pwm <= 0.5) && ((fallTimer + eps) >= deadTime);

        _outputSignal[0][0] = hi ? 1.0 : 0.0;
        _outputSignal[1][0] = lo ? 1.0 : 0.0;
    }
}
