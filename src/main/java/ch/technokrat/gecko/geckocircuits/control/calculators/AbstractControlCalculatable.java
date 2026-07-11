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

/**
 * Base-Class for all Control-Calculators.
 *
 * @author andreas
 */
public abstract class AbstractControlCalculatable {
    /** Threshold used by logic-level control blocks to interpret boolean signals (0 = false, 1 = true). */
    public static final double SIGNAL_THRESHOLD = 0.5;
    /**
     * Global simulation time shared across all calculators. Mutating this
     * static field is <em>not</em> thread-safe; it is set once per time step.
     */
    public static double _time = 0;

    /**
     * Sets the global simulation time for all calculators.
     * Note: this mutates a public static field and is not thread-safe.
     *
     * @param time the current simulation time
     */
    public static void setTime(final double time) {
        _time = time;
    }

    /**
     * Input signal array where the first dimension indexes the input port
     * and the second dimension (typically length 1) holds the signal value(s).
     */
    public final double[][] _inputSignal;
    /**
     * Output signal array where the first dimension indexes the output port
     * and the second dimension (typically length 1) holds the signal value(s).
     */
    public final double[][] _outputSignal;

    /**
     * @param noInputs number of input ports
     * @param noOutputs number of output ports
     */
    @SuppressWarnings("this-escape")
    public AbstractControlCalculatable(final int noInputs, final int noOutputs) {        
        _inputSignal = new double[noInputs][]; // careful: the array value of the input
        // signal is set when all components are connected within the netlist.
        _outputSignal = createOutputSignal(noOutputs);
    }

    /**
     * Performs one calculation step, reading from {@code _inputSignal} and
     * writing results to {@code _outputSignal}.
     *
     * @param deltaT the simulation time step in seconds
     */
    public abstract void calculateYOUT(final double deltaT);

    /**
     * Connects an input port to the output port of another calculator by
     * sharing the underlying array reference.
     *
     * @param inputIndex the input port index to connect
     * @param output the source calculator providing the signal
     * @param outputIndex the output port index of the source calculator
     * @throws Exception if the input port is already connected
     */
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

    /**
     * Creates the output signal array with the specified number of output ports.
     *
     * @param noOutputs number of output ports
     * @return the initialized output signal array
     */
    protected double[][] createOutputSignal(final int noOutputs) {
        return new double[noOutputs][1];
    }
}
