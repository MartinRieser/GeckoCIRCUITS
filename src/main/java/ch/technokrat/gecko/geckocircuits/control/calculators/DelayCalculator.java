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

/**
 * Implements a discrete time delay using a circular buffer.  The input signal
 * is delayed by a fixed number of time steps corresponding to the configured
 * delay time.
 */
public final class DelayCalculator extends AbstractSingleInputSingleOutputCalculator
        implements InitializableAtSimulationStart, IsDtChangeSensitive {

    /** The original simulation time step. */
    private double _originalDt;
    /** Buffer for delayed output history (youtVerzoegert). */
    private double[] _youtVerzoegert = null;
    /** Current index pointer in the delayed output buffer (zeigerYOUT). */
    private int _zeigerYOUT = -1;
    /** Flag indicating whether the delay memory buffer is still empty. */
    private boolean _speicherLeer;
    
    /*
     * the delay time value can only be changed at simulation start or at stop->continue.
     * This is the reason for this helper "init" variable!
     */
    private double _initDelayTime;
    private double _delayTime;
    
    public DelayCalculator(final double delayTime) {
        super();
        setDelayTime(delayTime);
    }

    /**
     * Re-samples the delay buffer when the simulation time step changes,
     * preserving the existing delay history by interpolating between the
     * old and new sample rates.
     *
     * @param deltaT the new simulation time step
     */
    @Override
    public void initWithNewDt(final double deltaT) {
        _delayTime = _initDelayTime;
        double[] youtVerzoegertNew = new double[(int) (_delayTime / deltaT)];
        final double ratio = deltaT / _originalDt;
        final int newStartIndex = Math.min(Math.max(0, (int) (_zeigerYOUT / ratio)), youtVerzoegertNew.length - 1);

        for (int i = 0; i < youtVerzoegertNew.length; i++) {
            try {
                youtVerzoegertNew[i] = _youtVerzoegert[(int) (ratio * i)];
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        _zeigerYOUT = newStartIndex;
        _youtVerzoegert = youtVerzoegertNew;
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        _delayTime = _initDelayTime;
        _originalDt = deltaT;
        _speicherLeer = true;
        _youtVerzoegert = new double[(int) (_delayTime / deltaT)];
        _zeigerYOUT = 0;

    }

    @Override
    public void calculateYOUT(final double deltaT) {
        if (_speicherLeer) {  // Speicher initial auffuellen
            if (deltaT > _delayTime) { // minimal delay, just feed the input signal to the output!
                _outputSignal[0][0] = _inputSignal[0][0];
                return;
            }
            _outputSignal[0][0] = 0;
            _youtVerzoegert[_zeigerYOUT] = _inputSignal[0][0];
            _zeigerYOUT++;
            if (_zeigerYOUT == _youtVerzoegert.length) {
                _speicherLeer = false;
                _zeigerYOUT = 0;
            }

        } else {
            _outputSignal[0][0] = _youtVerzoegert[_zeigerYOUT];
            _youtVerzoegert[_zeigerYOUT] = _inputSignal[0][0];  // laufendes Nachfuellen des Speichers
            _zeigerYOUT++;
            _zeigerYOUT %= _youtVerzoegert.length; // // Pointer runs 'in circles', this prevents the
            // // Data in memory must be shifted at each time step
        }
    }

    public void setDelayTime(final double delayTime) {
        if (delayTime < 0) {
            throw new IllegalArgumentException("Error: Delay time must be positive!");
        }
        _initDelayTime = delayTime;
    }
}
