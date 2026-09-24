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
package gecko.core.control.calculators;

/**
 * Base-Class for all Control-Calculators.
 *
 * @author andreas
 */
public abstract class AbstractControlCalculatable {
    /** Threshold for binary control signal comparisons. */
    public static final double SIGNAL_THRESHOLD = 0.5;

    /** Legacy static time used as fallback for standalone calculators in legacy unit tests. */
    private static volatile double legacyStaticTime = 0.0;

    /** Current simulation time for this calculator instance in seconds. */
    private double _time = 0.0;
    private boolean timeExplicitlySet = false;

    /**
     * Gets the current simulation time for this calculator instance.
     *
     * @return simulation time in seconds
     */
    public double getSimulationTime() {
        return timeExplicitlySet ? _time : legacyStaticTime;
    }

    /**
     * Sets the simulation time for this calculator instance.
     *
     * <p><strong>Note for composite/delegating calculators:</strong> If a subclass wraps or delegates
     * calculation to another {@link AbstractControlCalculatable} instance, it must override this method
     * to forward the simulation time to all wrapped child calculators that evaluate time-dependent signals.</p>
     *
     * @param time simulation time in seconds
     */
    public void setSimulationTime(final double time) {
        this._time = time;
        this.timeExplicitlySet = true;
    }

    /**
     * Legacy time setter retained for backward compatibility with standalone unit tests.
     *
     * @param time simulation time in seconds
     */
    public static void setTime(final double time) {
        legacyStaticTime = time;
    }
    
    public final double[][] _inputSignal;
    public final double[][] _outputSignal;

    public AbstractControlCalculatable(final int noInputs, final int noOutputs) {        
        _inputSignal = new double[noInputs][]; // careful: the array value of the input
        // signal is set when all components are connected within the netlist.
        _outputSignal = new double[noOutputs][1];
    }

    public abstract void calculateYOUT(final double deltaT);

    public void setInputSignal(final int inputIndex, final AbstractControlCalculatable output, 
            final int outputIndex) throws Exception {
        if (_inputSignal[inputIndex] != null) {            
            throw new Exception("Signal already connected: " + getClass());
        }        
        _inputSignal[inputIndex] = output._outputSignal[outputIndex];                
    }

    /**
     * check if input port has no connection. If this is the case, fill the
     * input port variable with a dummy double[].
     * @param inputIndex
     * @return true if input port has no connection
     */
    public boolean checkInputWithoutConnectionAndFill(final int inputIndex) {
        if(_inputSignal[inputIndex] == null) {
            _inputSignal[inputIndex] = new double[1];
            return true;
        } else {
            return false;
        }    
    }
    
    /**
     * TearDownOnPause will by called if the Simulation is paused or finished.
     * Intended to be overwritten by subclasses to free resources if necessary.
     */
    public void tearDownOnPause() {
    }
}
