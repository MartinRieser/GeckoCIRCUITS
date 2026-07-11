/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko;

/**
 * This an interface for using RMI to control GeckoCIRCUITS from e.g. MATLAB, or
 * remotely from another machine. Warning: the methods of this interface MUST have 
 * identical method names as GeckoRemoteInterface class. I am checking this within
 * a static final Object via assertions. But: this interface does not declare any exception.
 * 
 * This is the exception-free counterpart of {@link GeckoRemoteInterface}. Methods here do not
 * declare {@code RemoteException}, making it suitable for use with dynamic proxies that wrap
 * checked exceptions into runtime exceptions.
 * 
 * @author  andy.
 *
 */
public interface GeckoRemoteIntWithoutExc {
    
    static final MethodNameChecker CHECKER = 
            MethodNameChecker.checkFabric(GeckoRemoteIntWithoutExc.class, GeckoRemoteInterface.class);
    
    
    /**
     * Runs the simulation from start to end.
     */
    void runSimulation();

    /**
     * Returns the names of all available control elements.
     * @return array of control element names
     */
    String[] getControlElements();

    /**
     * Returns the names of all available circuit elements.
     * @return array of circuit element names
     */
    String[] getCircuitElements();

    /**
     * Returns the names of all available thermal elements.
     * @return array of thermal element names
     */
    String[] getThermalElements();
    
    /**
     * Returns the names of all available special elements.
     * @return array of special element names
     */
    String[] getSpecialElements();

    /**
     * Returns the names of all available IGBT elements.
     * @return array of IGBT element names
     */
    String[] getIGBTs();

    /**
     * Returns the names of all available diode elements.
     * @return array of diode element names
     */
    String[] getDiodes();

    /**
     * Returns the names of all available thyristor elements.
     * @return array of thyristor element names
     */
    String[] getThyristors();

    /**
     * Returns the names of all available ideal switch elements.
     * @return array of ideal switch element names
     */
    String[] getIdealSwitches();

    /**
     * Returns the names of all available resistor elements.
     * @return array of resistor element names
     */
    String[] getResistors();

    /**
     * Returns the names of all available inductor elements.
     * @return array of inductor element names
     */
    String[] getInductors();

    /**
     * Returns the names of all available capacitor elements.
     * @return array of capacitor element names
     */
    String[] getCapacitors();

    /**
     * Performs a custom operation on a component.
     * @param elementName the name of the component
     * @param operationName the operation to perform
     * @param parameterValue the parameter value for the operation
     * @return the result of the operation
     */
    Object doOperation(String elementName, String operationName, Object parameterValue);
    
    /**
     * Sets a single parameter on a component.
     * @param elementName the name of the component
     * @param parameterName the name of the parameter
     * @param value the value to set
     */
    void setParameter(String elementName, String parameterName, double value);    
    
    /**
     * Sets multiple parameters on a component at once.
     * @param elementName the name of the component
     * @param parameterNames the names of the parameters
     * @param values the values to set
     */
    void setParameters(String elementName, String[] parameterNames, double[] values);

    /**
     * Gets a single parameter value from a component.
     * @param elementName the name of the component
     * @param parameterName the name of the parameter
     * @return the parameter value
     */
    double getParameter(String elementName, String parameterName);

    /**
     * Gets an output value from a component by output name.
     * @param elementName the name of the component
     * @param outputName the name of the output
     * @return the output value
     */
    double getOutput(String elementName, String outputName) ;

    /**
     * Gets the first output value from a component.
     * @param elementName the name of the component
     * @return the output value
     */
    double getOutput(String elementName) ;

    /**
     * Initializes the simulation with previously set parameters.
     */
    void initSimulation() ;

    /**
     * Initializes the simulation with the given time step and end time.
     * @param deltaT the simulation time step
     * @param endTime the simulation end time
     */
    void initSimulation(final double deltaT, final double endTime) ;

    /**
     * Continues a previously paused simulation.
     */
    void continueSimulation() ;
    
    /**
     * Simulates for the specified amount of time.
     * @param time the amount of time to simulate
     */
    void simulateTime(double time) ;

    /**
     * Ends the simulation and releases resources.
     */
    void endSimulation() ;

    /**
     * Saves the current circuit to a file.
     * @param fileName the path to save the file to
     */
    void saveFileAs(String fileName) ;

    /**
     * Opens a circuit file.
     * @param fileName the path to the file to open
     */
    void openFile(String fileName);

    /**
     * Imports a circuit from a file into a subcircuit.
     * @param fileName the path to the file to import
     * @param importIntoSubcircuit the name of the subcircuit to import into
     */
    void importFromFile(String fileName, String importIntoSubcircuit);
    
    /**
     * Returns the current simulation time step.
     * @return the simulation time step
     */
    double get_dt() ;
    /**
     * Returns the simulation end time.
     * @return the simulation end time
     */
    double get_Tend() ;

    /**
     * Returns the pre-initialization simulation time step.
     * @return the pre-initialization time step
     */
    double get_dt_pre() ;
    /**
     * Returns the pre-initialization simulation end time.
     * @return the pre-initialization end time
     */
    double get_Tend_pre() ;
    
    /**
     * Sets the simulation time step.
     * @param value the time step value
     */
    void set_dt(double value) ;
    /**
     * Sets the pre-initialization simulation time step.
     * @param value the pre-initialization time step value
     */
    void set_dt_pre(double value) ;
    /**
     * Sets the simulation end time.
     * @param value the end time value
     */
    void set_Tend(double value) ;
    /**
     * Sets the pre-initialization simulation end time.
     * @param value the pre-initialization end time value
     */
    void set_Tend_pre(double value) ;
    
    /**
     * @deprecated Use {@link #getSignalAvg(String, double, double)} instead.
     */
    @Deprecated
    double[] getSignalCharacteristics(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalAvg(String, double, double)} instead.
     */
    @Deprecated
    double getAvg(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalRMS(String, double, double)} instead.
     */
    @Deprecated
    double getRMS(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalTHD(String, double, double)} instead.
     */
    @Deprecated
    double getTHD(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalMin(String, double, double)} instead.
     */
    @Deprecated
    double getMin(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalMax(String, double, double)} instead.
     */
    @Deprecated
    double getMax(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalRipple(String, double, double)} instead.
     */
    @Deprecated
    double getRipple(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalKlirr(String, double, double)} instead.
     */
    @Deprecated
    double getKlirr(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalShape(String, double, double)} instead.
     */
    @Deprecated
    double getShape(String scopeName, int scopePort, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalAvg(String, double, double)} instead.
     */
    @Deprecated
    double[] getSignalCharacteristics(String scopeName, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalAvg(String, double, double)} instead.
     */
    @Deprecated
    double getAvg(String scopeName, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalRMS(String, double, double)} instead.
     */
    @Deprecated
    double getRMS(String scopeName, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalMin(String, double, double)} instead.
     */
    @Deprecated
    double getMin(String scopeName, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalMax(String, double, double)} instead.
     */
    @Deprecated
    double getMax(String scopeName, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalTHD(String, double, double)} instead.
     */
    @Deprecated
    double getTHD(String scopeName, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalShape(String, double, double)} instead.
     */
    @Deprecated
    double getShape(String scopeName, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalKlirr(String, double, double)} instead.
     */
    @Deprecated
    double getKlirr(String scopeName, double startTime, double endTime) ;

    /**
     * @deprecated Use {@link #getSignalRipple(String, double, double)} instead.
     */
    @Deprecated
    double getRipple(String scopeName, double startTime, double endTime) ;

    /**
     * Performs Fourier analysis on a scope port.
     * @param scopeName the name of the scope
     * @param scopePort the port index on the scope
     * @param startTime the start time for analysis
     * @param endTime the end time for analysis
     * @param harmonics the number of harmonics to compute
     * @return 2D array of frequency and magnitude data
     */
    double[][] getFourier(String scopeName, int scopePort, double startTime, double endTime, int harmonics) ;

    /**
     * Performs Fourier analysis on a scope (port 0).
     * @param scopeName the name of the scope
     * @param startTime the start time for analysis
     * @param endTime the end time for analysis
     * @param harmonics the number of harmonics to compute
     * @return 2D array of frequency and magnitude data
     */
    double[][] getFourier(String scopeName, double startTime, double endTime, int harmonics) ;

    /**
     * Returns the average value of a signal over a time range.
     * @param signalName the name of the signal
     * @param startTime the start time
     * @param endTime the end time
     * @return the average value
     */
    double getSignalAvg(String signalName, double startTime, double endTime) ;

    /**
     * Returns the RMS value of a signal over a time range.
     * @param signalName the name of the signal
     * @param startTime the start time
     * @param endTime the end time
     * @return the RMS value
     */
    double getSignalRMS(String signalName, double startTime, double endTime) ;

    /**
     * Returns the minimum value of a signal over a time range.
     * @param signalName the name of the signal
     * @param startTime the start time
     * @param endTime the end time
     * @return the minimum value
     */
    double getSignalMin(String signalName, double startTime, double endTime) ;

    /**
     * Returns the maximum value of a signal over a time range.
     * @param signalName the name of the signal
     * @param startTime the start time
     * @param endTime the end time
     * @return the maximum value
     */
    double getSignalMax(String signalName, double startTime, double endTime) ;

    /**
     * Returns the total harmonic distortion of a signal over a time range.
     * @param signalName the name of the signal
     * @param startTime the start time
     * @param endTime the end time
     * @return the THD value
     */
    double getSignalTHD(String signalName, double startTime, double endTime) ;

    /**
     * Returns the shape factor of a signal over a time range.
     * @param signalName the name of the signal
     * @param startTime the start time
     * @param endTime the end time
     * @return the shape factor
     */
    double getSignalShape(String signalName, double startTime, double endTime) ;

    /**
     * Returns the Klirr factor of a signal over a time range.
     * @param signalName the name of the signal
     * @param startTime the start time
     * @param endTime the end time
     * @return the Klirr factor
     */
    double getSignalKlirr(String signalName, double startTime, double endTime) ;

    /**
     * Returns the ripple of a signal over a time range.
     * @param signalName the name of the signal
     * @param startTime the start time
     * @param endTime the end time
     * @return the ripple value
     */
    double getSignalRipple(String signalName, double startTime, double endTime) ;

    /**
     * Performs Fourier analysis on a named signal.
     * @param signalName the name of the signal
     * @param startTime the start time for analysis
     * @param endTime the end time for analysis
     * @param harmonics the number of harmonics to compute
     * @return 2D array of frequency and magnitude data
     */
    double[][] getSignalFourier(String signalName, double startTime, double endTime, int harmonics) ;
    
    /**
     * Initializes steady-state detection with a single frequency.
     * @param stateVariables the state variables to monitor
     * @param frequency the fundamental frequency
     * @param deltaT the simulation time step
     * @param simulationTime the total simulation time
     */
    void initSteadyStateDetection(final String[] stateVariables, final double frequency, final double deltaT, 
            final double simulationTime) ;
    
    /**
     * @deprecated Use {@link #initSteadyStateDetection(String[], double, double, double)} instead.
     */
    @Deprecated
    void initSteadyStateDetection(final String[] stateVariables, final double[] frequencies, final double deltaT, 
            final double simulationTime) ;

    /**
     * Simulates until steady state is reached with default thresholds.
     * @param suppressMessages if true, suppresses console output
     * @return array of steady-state metrics
     */
    double[] simulateToSteadyState(boolean suppressMessages) ;
    
    /**
     * Simulates until steady state is reached with custom thresholds.
     * @param suppressMessages if true, suppresses console output
     * @param targetCorrelation the target correlation threshold
     * @param targetMeanPctDiff the target mean percentage difference threshold
     * @return array of steady-state metrics
     */
    double[] simulateToSteadyState(boolean suppressMessages, double targetCorrelation, double targetMeanPctDiff) ;
    
    /**
     * @deprecated Use {@link #simulateToSteadyState(boolean)} instead.
     */
    @Deprecated
    double[] simulateUntilSteadyState(boolean suppressMessages) ;

    /**
     * @deprecated Use {@link #setParameter(String, String, double)} instead.
     */
    @Deprecated
    void setLossFile(String elementName, String lossFileName);

    /**
     * @deprecated Use {@link #setParameter(String, String, double)} instead.
     */
    @Deprecated
    void setNonLinear(String elementName, String characteristicFileName);

    /**
     * Sets the position of a component on the worksheet.
     * @param elementName the name of the component
     * @param xPosition the x coordinate
     * @param yPosition the y coordinate
     */
    void setPosition(String elementName, int xPosition, int yPosition) ;
    /**
     * Returns the position of a component on the worksheet.
     * @param elementName the name of the component
     * @return array of [x, y] coordinates
     */
    int[] getPosition(String elementName) ;

    /**
     * @deprecated Use {@link #deleteComponent(String)} instead.
     */
    @Deprecated
    void delete(String elementName) ;
    /**
     * Deletes a component from the circuit.
     * @param elementName the name of the component to delete
     */
    void deleteComponent(String elementName) ;    
    /**
     * Deletes all components in a subcircuit.
     * @param subcircuitName the name of the subcircuit
     */
    void deleteAllComponents(String subcircuitName) ;
    
    /**
     * Creates a connector (wire) between two points.
     * @param elementName the name of the connector
     * @param xStart the start x coordinate
     * @param yStart the start y coordinate
     * @param xEnd the end x coordinate
     * @param yEnd the end y coordinate
     * @param startHorizontal whether the connector starts horizontally
     */
    void createConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal);
    
    /**
     * Creates a new component on the worksheet.
     * @param elementType the type of component to create
     * @param elementName the name for the new component
     * @param xPosition the x position on the worksheet
     * @param yPosition the y position on the worksheet
     */
    void createComponent(String elementType, String elementName, int xPosition, int yPosition) ;
    /**
     * @deprecated Use {@link #createComponent(String, String, int, int)} instead.
     */
    @Deprecated
    void create(String elementType, String elementName, int xPosition, int yPosition) ;

    /**
     * Sets the output node name for a component.
     * @param elementName the name of the component
     * @param nodeIndex the index of the output node
     * @param nodeName the name to set
     */
    void setOutputNodeName(String elementName, int nodeIndex, String nodeName) ;
    /**
     * Sets the input node name for a component.
     * @param elementName the name of the component
     * @param nodeIndex the index of the input node
     * @param nodeName the name to set
     */
    void setInputNodeName(String elementName, int nodeIndex, String nodeName) ;
    
    /**
     * Returns the output node name of a component.
     * @param elementName the name of the component
     * @param nodeIndex the index of the output node
     * @return the output node name
     */
    String getOutputNodeName(String elementName, int nodeIndex) ;
    /**
     * Returns the input node name of a component.
     * @param elementName the name of the component
     * @param nodeIndex the index of the input node
     * @return the input node name
     */
    String getInputNodeName(String elementName, int nodeIndex) ;

    /**
     * Rotates a component on the worksheet.
     * @param elementName the name of the component to rotate
     */
    void rotate(String elementName) ;

    /**
     * Sets the orientation of a component.
     * @param elementName the name of the component
     * @param direction the orientation direction
     */
    void setOrientation(String elementName, String direction) ;

    /**
     * Shuts down the GeckoCIRCUITS instance.
     */
    void shutdown() ;

    /**
     * Returns signal data points over a time range.
     * @param signalName the name of the signal
     * @param tStart the start time
     * @param tEnd the end time
     * @param skipPoints number of points to skip between samples
     * @return array of signal data points
     */
    float[] getSignalData(String signalName, double tStart, double tEnd, int skipPoints) ;

    /**
     * Returns time array values for a signal over a time range.
     * @param signalName the name of the signal
     * @param tStart the start time
     * @param tEnd the end time
     * @param skipPoints number of points to skip between samples
     * @return array of time values
     */
    double[] getTimeArray(String signalName, double tStart, double tEnd, int skipPoints) ;

    /**
     * Renames a component.
     * @param oldName the current name of the component
     * @param newName the new name for the component
     */
    void setComponentName(final String oldName, final String newName) ;

    /**
     * Returns the current simulation time.
     * @return the simulation time
     */
     double getSimulationTime() ;

    /**
     * Returns the global float matrix.
     * @return the global float matrix
     */
     float[][] getGlobalFloatMatrix() ;
    /**
     * Returns the global double matrix.
     * @return the global double matrix
     */
     double[][] getGlobalDoubleMatrix() ;
    /**
     * Sets the global float matrix.
     * @param matrix the float matrix to set
     */
     void setGlobalFloatMatrix(final float[][] matrix) ;
    /**
     * Sets the global double matrix.
     * @param matrix the double matrix to set
     */
     void setGlobalDoubleMatrix(final double[][] matrix) ;
    /**
     * Performs FFT on float time-domain data.
     * @param timeValues the time-domain float array
     * @return the FFT result as a float array
     */
    float[] floatFFT(final float[] timeValues) ;    
    /**
     * @deprecated Use {@link #floatFFT(float[])} instead.
     */
    @Deprecated
    float[] realFFT(final float[] timeValues) ;    
    /**
     * @deprecated Use {@link #getAccessibleParameters(String)} instead.
     */
    @Deprecated
    String[] getParametersNames(String componentName) ;
    /**
     * Returns the accessible parameter names for a component.
     * @param componentName the name of the component
     * @return array of parameter names
     */
    String[] getAccessibleParameters(String componentName) ;
    
    /**
     * Sets the worksheet dimensions for signal data storage.
     * @param sizeX the width of the worksheet
     * @param sizeY the height of the worksheet
     */
    void setWorksheetSize(int sizeX, int sizeY);
    
    /**
     * Returns the worksheet dimensions.
     * @return array containing [width, height] of the worksheet
     */
    int[] getWorksheetSize();    
    
    /**
     * Sets a global parameter value.
     * @param parameterName the name of the global parameter
     * @param value the value to set
     */
    void setGlobalParameterValue(String parameterName, double value) ;
    /**
     * Returns a global parameter value.
     * @param parameterName the name of the global parameter
     * @return the parameter value
     */
    double getGlobalParameterValue(String parameterName) ;

    /**
     * Allows additional clients to connect simultaneously.
     * @param numberOfExtraConnections the number of additional connections allowed
     */
    void acceptExtraConnections(int numberOfExtraConnections);

}
