package gecko.core.control.calculators;

/**
 * Two-input addition block: {@code out = in0 + in1}.
 */
public final class AddCalculator extends AbstractTwoInputsOneOutputCalculator {

    @Override
    public void calculateYOUT(final double deltaT) {
        _outputSignal[0][0] = _inputSignal[0][0] + _inputSignal[1][0];
    }
}
