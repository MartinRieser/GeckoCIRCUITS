/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.control.calculators;

import ch.technokrat.gecko.geckocircuits.control.IsDtChangeSensitive;
import ch.technokrat.gecko.geckocircuits.control.ControlSlidingDFT;
import ch.technokrat.gecko.geckocircuits.control.ControlSlidingDFT.FrequencyData;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a Sliding Discrete Fourier Transform (SDFT) for real-time
 * spectral analysis of a time-domain input signal. Uses a recursive update
 * formula for O(1) per-sample computation.
 */
public final class SlidingDFTCalculator extends AbstractControlCalculatable 
    implements InitializableAtSimulationStart, IsDtChangeSensitive {

    /** Maps output indices to compressed frequency set indices. */
    private int[] _frequencyIndicesMap;
    /** Unique set of frequency bin indices to compute. */
    private int[] _frequencyIndicesSet;
    /** The averaging span in seconds for the DFT window. */
    private final double _averageSpanSecs;
    /** Number of samples in the DFT window. */
    private int _size;
    /** Circular buffer index for input and output signals. */
    private int _idx;
    /** Circular buffer of input time-domain samples. */
    private double[] _timeData;
    /** Oldest sample being removed from the window. */
    private double _oldestDataReal;
    /** Newest sample being added to the window. */
    private double _newestDataReal;
    /** Real parts of the frequency-domain bins. */
    private double[] _freqsReal;
    /** Imaginary parts of the frequency-domain bins. */
    private double[] _freqsImag;
    /** Frequency output configuration data from the control block. */
    private final List<FrequencyData> _data;

    public SlidingDFTCalculator(final int noOutputs, final double avgSpan, final List<ControlSlidingDFT.FrequencyData> freqData) {
        super(1, noOutputs);
        _averageSpanSecs = avgSpan;
        _data = new ArrayList<ControlSlidingDFT.FrequencyData>(freqData);
    }

    @Override
    public void calculateYOUT(final double deltaT) {

        _oldestDataReal = _timeData[_idx];
        _timeData[_idx] = _inputSignal[0][0];
        _newestDataReal = _inputSignal[0][0];

        doSlidingFourierStep();

        if (++_idx == _size) {
            _idx = 0; // bump global index
        }
        for (int i = 0; i < _frequencyIndicesMap.length; i++) {
            final int index = _frequencyIndicesSet[_frequencyIndicesMap[i]];
            
            switch (_data.get(i)._outputData) {
                case ABS:
                    _outputSignal[i][0] = 2 * Math.sqrt((_freqsReal[index] * _freqsReal[index]
                            + _freqsImag[index] * _freqsImag[index])) / _size;
                    break;
                case REAL:
                    _outputSignal[i][0] = 2 * _freqsReal[index] / _size;
                    break;
                case IMAG:
                    _outputSignal[i][0] = 2 * _freqsImag[index] / _size;
                    break;
                case PHASE:
                    _outputSignal[i][0] = Math.atan2(_freqsImag[index], _freqsReal[index]);
                    break;
                default:
                    assert false;
                    _outputSignal[i][0] = 0;
            }
        }
    }
    
    /**
     * Performs one recursive DFT update: the oldest data point is subtracted
     * from the frequency bins and the newest data point is added, using the
     * formula: X_k(t) = (X_k(t-1) + (x_new - x_old) * e^(-j*2*pi*k/N)) .
     */
    private void doSlidingFourierStep() {
        final double deltaReal = _newestDataReal - _oldestDataReal;        
        for (int i : _frequencyIndicesSet) {
            final int index = (i * _idx) % _size;
            final double argument = -Math.PI *2* index / ((double) _size);            
            _freqsReal[i] += deltaReal * Math.cos(argument);
            _freqsImag[i] += deltaReal * Math.sin(argument);
        }
    }

    /**
     * Initializes the circular buffer, frequency arrays, and index at
     * simulation start based on the given time step.
     * @param deltaT the simulation time step
     */
    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        _size = (int) Math.round(_averageSpanSecs / deltaT);
        _timeData = new double[_size];        
        _freqsReal = new double[_size + 1];
        _freqsImag = new double[_size + 1];
        calculateFrequencyIndices();

        _oldestDataReal = 0;
        _newestDataReal = 0;
        _idx = 0;
    }

    private void calculateFrequencyIndices() {
        _frequencyIndicesMap = new int[_data.size()];
        final List<Integer> freqIndicesReduced = new ArrayList<Integer>();
        for (int i = 0; i < _data.size(); i++) {
            final double freq = _data.get(i)._frequency.getDoubleValue();
            final int index = (int) Math.round((freq * _averageSpanSecs));
            if (!freqIndicesReduced.contains(index)) {
                freqIndicesReduced.add(index);
            }
            _frequencyIndicesMap[i] = freqIndicesReduced.indexOf(index);
        }
        _frequencyIndicesSet = new int[freqIndicesReduced.size()];
        for (int i = 0; i < freqIndicesReduced.size(); i++) {
            _frequencyIndicesSet[i] = freqIndicesReduced.get(i);
        }

    }

    /**
     * Re-initializes the calculator with a new time step, rescaling
     * frequency bins and resampling the input buffer.
     * @param deltaT the new simulation time step
     */
    @Override
    public void initWithNewDt(final double deltaT) {
        final int oldSize = _size;
        _size = (int) Math.round(_averageSpanSecs / deltaT);
        final double scalingFactor = 1.0 * _size / oldSize;
        final double[] inRealOld = _timeData;
        final double[] freqsRealOld = _freqsReal;
        final double[] freqsImagOld = _freqsImag;

        initializeAtSimulationStart(deltaT);
        _freqsReal = freqsRealOld;
        _freqsImag = freqsImagOld;

        //System.out.println("factor: " + oldSize / _size);
        for (int i = 0; i < _freqsReal.length; i++) {
            _freqsReal[i] *= scalingFactor;
            _freqsImag[i] *= scalingFactor;
        }



        if (inRealOld.length < _size) {
            for (int i = 0; i < _size; i++) {
                _timeData[i] = inRealOld[(int) (i / scalingFactor)];
            }
        } else {
            assert _size == _timeData.length;
            for (int i = 0; i < _size; i++) {
                _timeData[i] = inRealOld[(int) (i / (scalingFactor))];
            }
        }
    }
}