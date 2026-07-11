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

import java.util.ArrayList;
import java.util.List;

/**
 * Calculator for generating 6-pulse thyristor converter gate signals.
 * Synchronizes to the line voltage zero-crossings and generates firing pulses
 * with configurable phase shift and on-time for six thyristors.
 */
public final class ThyristorControlCalculator extends AbstractControlCalculatable {
    /** Number of input terminals (2: phase shift and synchronization signals). */
    private static final int TN_X = 2;
    /** Number of output terminals (6: one gate signal per thyristor). */
    private static final int TN_Y = 6;
    /** Constant 3 used for three-phase pulse distribution. */
    private static final double THREE = 3;
    /** Constant 1.5: half of THREE for offset calculation. */
    private static final double THREE_HALF = 1.5;
    /** Time of the last falling zero-crossing of the synchronization signal. */
    private double _lastFallingZero = -1;
    /** Time of the most recent synchronization event (rising zero-crossing). */
    private double _synchTime;
    /** Previous value of the synchronization signal for edge detection. */
    private double _synchOld = 0;
    /** Measured synchronization frequency derived from zero-crossing intervals. */
    private double _synchFreq;
    /** Gate on-time duration in seconds. */
    private double _onTime;
    /** Phase shift in degrees applied to the firing angle. */
    private double _phaseShift;
        
    
    /** List of scheduled gate events for all six thyristor channels. */
    private final List<GateEvent> _gateEvents = new ArrayList<GateEvent>();
    /** Last computed on-time point for each of the six output channels. */
    private double[] _lastOnTimePoint = new double[]{-1, -1, -1, -1, -1, -1};
    
    /**
     * 
     * @param phaseShift measured in degrees
     * @param initFreq in Hz
     * @param onTime in seconds
     */
    public ThyristorControlCalculator(final double phaseShift, final double initFreq, final double onTime) {
        super(TN_X, TN_Y);
        _onTime = onTime;
        _phaseShift = phaseShift;
        _synchFreq = initFreq;        
        _lastOnTimePoint = new double[]{-1, -1, -1, -1, -1, -1};
        _lastFallingZero = -1;        
        _synchTime = 0;
        _synchOld = 0;        
    }

    public void setOnTime(final double onTime) {
        _onTime = onTime;
    }

    public void setPhaseShift(final double phaseShift) {
        _phaseShift = phaseShift;
    }
    
    

    @Override
    public void calculateYOUT(final double deltaT) {
        if (_synchOld <= 0 && _inputSignal[1][0] >= 0 && _synchOld != _inputSignal[1][0]) {
            _synchTime = _time;


            if (_lastFallingZero > 0 && (_time - _lastFallingZero) != 0) {
                _synchFreq = 1.0 / (_time - _lastFallingZero);
            }

            if (-1 > _lastFallingZero) {
                _synchFreq = 1 / (2.0 * (_time - -1));
            }

            _lastFallingZero = _time;
        }


        final double alpha = Math.toRadians(_phaseShift + _inputSignal[0][0]);

        for (int i = 0; i < TN_Y; i++) {
            double onTimePoint = 
                    (_synchTime + 1 / _synchFreq * alpha / (2 * Math.PI) 
                    + (i - TN_Y) * 1 / (THREE * _synchFreq));

            if (i > 2) {                
                onTimePoint -= THREE_HALF / _synchFreq;                
            }

            if (_lastOnTimePoint[i] != onTimePoint) {
                _gateEvents.add(new GateEvent(onTimePoint, onTimePoint + _onTime, i));
            }

            _lastOnTimePoint[i] = onTimePoint;
        }

        final List<GateEvent> removeEvents = new ArrayList<GateEvent>();
        for (GateEvent ge : _gateEvents) {
            final GateEvent toRemove = ge.processEvent(_time, _outputSignal);
            if (toRemove != null) {
                removeEvents.add(toRemove);
            }
        }
        _gateEvents.removeAll(removeEvents);

        _synchOld = _inputSignal[1][0];
    }
    
    
    /**
     * Represents a scheduled gate firing event for a single thyristor.
     * Handles on/off timing and wraps events across period boundaries.
     */
    class GateEvent {
        private static final double SMALL_VALUE = 1e-20;
        private final double _onTime;
        private final double _offTime;
        private final int _gateNumber;
        private static final double EPSILON = 1e-10;

        public GateEvent(final double onTime, final double offTime, final int gateNumber) {
            double tmpOnTime = onTime;
            double tmpOffTime = offTime;

            if (tmpOnTime <= 0 && _time == 0) {
                _gateEvents.add(new GateEvent(SMALL_VALUE, offTime - onTime, gateNumber));
            }

            while (_time > tmpOnTime + EPSILON) {
                tmpOnTime += 1.0 / _synchFreq;
                tmpOffTime += 1.0 / _synchFreq;
            }

            _offTime = tmpOffTime;
            _onTime = tmpOnTime;
            _gateNumber = gateNumber;
        }

        public GateEvent processEvent(final double time, final double[][] yOUT) {

            if (time > _onTime) {
                yOUT[_gateNumber][0] = 1;
            }

            if (time > _offTime) {
                yOUT[_gateNumber][0] = 0;
                return this;
            }
            return null;
        }
    }
}
