package gecko.core.control.calculators;

/**
 * Two-input multiplication block: {@code out = in0 * in1}.
 */
public final class MulCalculator extends AbstractTwoInputsOneOutputCalculator {

    @Override
    public void calculateYOUT(final double deltaT) {
        _outputSignal[0][0] = _inputSignal[0][0] * _inputSignal[1][0];
    }
}
