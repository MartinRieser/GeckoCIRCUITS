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

import ch.technokrat.gecko.geckocircuits.general.MainWindow;
import ch.technokrat.gecko.geckocircuits.general.OperatingMode;
import ch.technokrat.gecko.geckocircuits.newscope.Cispr16Fft;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deprecated wrapper for external access (e.g., from MATLAB). Provides static
 * delegate methods to GeckoCIRCUITS functionality. New code should use
 * GeckoRemote instead.
 * <p>
 * Note: getThyristors(), createComponent(), and createConnector() are instance
 * methods while all others are static -- this inconsistency is preserved for
 * backwards compatibility.
 *
 * @author andy
 * @author anstupar
 */
@SuppressWarnings("deprecation")
public class GeckoExternal {

    protected static ExternalGeckoCustom external;
    private static double[][] _globalDoubleMatrix;
    private static float[][] _globalFloatMatrix;

    public static void startGui() {

        System.out.println("***WARNING: GeckoExternal is a DEPRECATED API***");
        System.out.println("GeckoExternal is deprecated as of GeckoCIRCUITS version 1.6.");
        System.out.println("Please switch to using GeckoRemote (as explained in the Appendix of the GeckoSCRIPT tutorial included with your GeckoCIRCUITS distribution).\n");
        System.out.println("Reasons to switch: ");
        System.out.println("Using GeckoRemote runs GeckoCIRCUITS in its own JVM, not inside MATLAB, this means that");
        System.out.println("1) You can close GeckoCIRCUITS without having to close all of MATLAB;");
        System.out.println("2) You don't have to worry about memory allocation for GeckoCIRCUITS in the MATLAB JVM;");
        System.out.println("3) You will not have problems with compiling Java blocks in your model as was the case with some MATLAB installations.\n");
        System.out.println("GeckoExternal continues to function as before for backwards compatibility. However it is no longer maintained.");
        System.out.println("This means that:");
        System.out.println("1) Any new problems with using GeckoExternal in MATLAB will not be addressed;");
        System.out.println("2) Any new GeckoSCRIPT functions (version 1.6 and later) will not be available through GeckoExternal.");
        System.out.println("***WARNING: GeckoExternal is a DEPRECATED API***");

        GeckoSim.operatingmode = OperatingMode.EXTERNAL;
        if (external == null) {
            Thread guiThread = new Thread() {
                @Override
                public void run() {
                    GeckoSim.main(new String[]{});
                    checkExternal();
                }
            };
            guiThread.setPriority(Thread.MIN_PRIORITY);
            guiThread.start();
        }

        while (!GeckoSim.mainLoaded) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(GeckoExternal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public static void startGui(String filePath) {
        GeckoSim.operatingmode = OperatingMode.EXTERNAL;
        if (external == null) {
            GeckoSim.main(new String[]{filePath});
            checkExternal();
        }
    }

    protected static void checkExternal() {
        if (external == null) {
            external = new ExternalGeckoCustom(MainWindow.getScripter());
            MainWindow.setExternal(external);
        }
    }

    /** Runs the full simulation from start to end. */
    public static void runSimulation() {
        checkExternal();
        external.runSimulation();
    }

    /** Returns the names of all control circuit elements. */
    public static String[] getControlElements() {
        checkExternal();
        return external.getControlElements();
    }

    /** Returns the names of all power circuit elements. */
    public static String[] getCircuitElements() {
        checkExternal();
        return external.getCircuitElements();
    }

    /** Returns the names of all thermal elements. */
    public static String[] getThermalElements() {
        checkExternal();
        return external.getThermalElements();
    }
    
    /** Returns the names of all special elements. */
    public static String[] getSpecialElements() {
        checkExternal();
        return external.getSpecialElements();
    }

    /** Returns the names of all IGBT elements. */
    public static String[] getIGBTs() {
        checkExternal();
        return external.getIGBTs();
    }

    /** Returns the names of all diode elements. */
    public static String[] getDiodes() {
        checkExternal();
        return external.getDiodes();
    }

    /** Stores a global double matrix for external access. */
    public static void setGlobalDoubleMatrix(double[][] matrix) {
        _globalDoubleMatrix = matrix;
    }

    /** Stores a global float matrix for external access. */
    public static void setGlobalFloatMatrix(float[][] matrix) {
        _globalFloatMatrix = matrix;
    }

    /** Returns the stored global double matrix. */
    public static double[][] getGlobalDoubleMatrix() {
        return _globalDoubleMatrix;
    }

    /** Returns the stored global float matrix. */
    public static float[][] getGlobalFloatMatrix() {
        return _globalFloatMatrix;
    }
        

    /** Sets a global optimizer parameter value by name. */
    public static void setGlobalParameterValue(final String parameterName, final double value) {
        checkExternal();
        MainWindow.getOptimizerParameterData().setNumberFromName(parameterName, value);
    }

    /** Gets a global optimizer parameter value by name. */
    public static double getGlobalParameterValue(final String parameterName) {
        checkExternal();
        return MainWindow.getOptimizerParameterData().getNumberFromName(parameterName);
    }

    /**
     * Returns the names of all thyristor elements. Note: this is an instance
     * method while most other accessors are static.
     */
    public String[] getThyristors() {
        checkExternal();
        return external.getThyristors();
    }

    /** Returns the names of all ideal switch elements. */
    public static String[] getIdealSwitches() {
        checkExternal();
        return external.getIdealSwitches();
    }

    /** Returns the names of all resistor elements. */
    public static String[] getResistors() {
        checkExternal();
        return external.getResistors();
    }

    /** Returns the names of all inductor elements. */
    public static String[] getInductors() {
        checkExternal();
        return external.getInductors();
    }

    /** Returns the names of all capacitor elements. */
    public static String[] getCapacitors() {
        checkExternal();
        return external.getCapacitors();
    }

    /** Sets a single parameter on an element. */
    public static void setParameter(String elementName, String parameterName, double value) {
        checkExternal();
        external.setParameter(elementName, parameterName, value);
    }

    /** Sets multiple parameters on an element at once. */
    public static void setParameters(final String elementName, final String[] parameterNames, final double[] values) {
        checkExternal();
        external.setParameters(elementName, parameterNames, values);
    }   
    
    /** Performs an operation on an element and returns the result. */
    public static Object doOperation(final String elementName, final String operationName, final Object parameterValue) {
        checkExternal();
        return external.doOperation(elementName, operationName, parameterValue);
    }

    /** Gets the value of a parameter from an element. */
    public static double getParameter(final String elementName, final String parameterName) {
        checkExternal();
        return external.getParameter(elementName, parameterName);
    }

    /** Gets the value of an output from an element. */
    public static double getOutput(final String elementName, final String outputName) {
        checkExternal();
        return external.getOutput(elementName, outputName);
    }

    /** Gets the first output value of an element. */
    public static double getOutput(String elementName) {
        checkExternal();
        return external.getOutput(elementName);
    }

    /** Initializes the simulation with default timing. */
    public static void initSimulation() {
        checkExternal();
        external.initSimulation();
    }

    /** Initializes the simulation with given time step and end time. */
    public static void initSimulation(double dt, double endTime) {
        checkExternal();
        external.initSimulation(dt, endTime);
    }
    
    /** Returns the current simulation time. */
    public static double getSimulationTime() {
        checkExternal();
        return external.getSimulationTime();
    }

    /** Continues the simulation from the current time. */
    public static void continueSimulation() {
        checkExternal();
        external.continueSimulation();
    }

    /** Advances the simulation by one time step. */
    public static void simulateStep() {
        checkExternal();
        external.simulateStep();
    }

    /** Advances the simulation by the given number of steps. */
    public static void simulateSteps(int steps) {
        checkExternal();
        external.simulateSteps(steps);
    }

    /** Advances the simulation by the given amount of time. */
    public static void simulateTime(double time) {
        checkExternal();
        external.simulateTime(time);
    }
    
    /** Sets the worksheet dimensions. */
    public static void setWorksheetSize(int sizeX, int sizeY) {
        checkExternal();
        external.setWorksheetSize(sizeX, sizeY);
    }
    
    /** Returns the worksheet dimensions. */
    public static int[] getWorksheetSize() {
        checkExternal();
        return external.getWorksheetSize();
    }
        

    /** Ends the simulation and releases resources. */
    public static void endSimulation() {
        checkExternal();
        external.endSimulation();
    }

    /** Saves the current model to the given file path. */
    public static void saveFileAs(String fileName) {
        checkExternal();
        external.saveFileAs(fileName);
    }

    /** Opens a model file. */
    public static void openFile(final String fileName) throws RemoteException, FileNotFoundException {
        checkExternal();
        external.openFile(fileName);
    }

    /** Returns the current simulation time step. */
    public static double get_dt() {
        checkExternal();
        return external.get_dt();
    }

    /** Sets the simulation time step. */
    public static void set_dt(double value) {
        checkExternal();
        external.set_dt(value);
    }

    /** Sets the simulation end time. */
    public static void set_Tend(double value) {
        checkExternal();
        external.set_Tend(value);
    }

    /** Returns the simulation end time. */
    public static double get_Tend() {
        checkExternal();
        return external.get_Tend();
    }

    /** Returns the preferred simulation time step. */
    public static double get_dt_pre() {
        checkExternal();
        return external.get_dt_pre();
    }

    /** Sets the preferred simulation time step. */
    public static void set_dt_pre(double value) {
        checkExternal();
        external.set_dt_pre(value);
    }

    /** Sets the preferred simulation end time. */
    public static void set_Tend_pre(double value) {
        checkExternal();
        external.set_Tend_pre(value);
    }

    /** Returns the preferred simulation end time. */
    public static double get_Tend_pre() {
        checkExternal();
        return external.get_Tend_pre();
    }

    /** Returns signal characteristics (min, max, avg, etc.) for a scope port. */
    public static double[] getSignalCharacteristics(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getSignalCharacteristics(scopeName, scopePort, startTime, endTime);
    }

    /** Returns the average value of a scope signal. */
    public static double getAvg(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getAvg(scopeName, scopePort, startTime, endTime);
    }

    /** Returns the RMS value of a scope signal. */
    public static double getRMS(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getRMS(scopeName, scopePort, startTime, endTime);
    }

    /** Returns the THD of a scope signal. */
    public static double getTHD(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getTHD(scopeName, scopePort, startTime, endTime);
    }

    /** Returns the minimum value of a scope signal. */
    public static double getMin(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getMin(scopeName, scopePort, startTime, endTime);
    }

    /** Returns the maximum value of a scope signal. */
    public static double getMax(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getMax(scopeName, scopePort, startTime, endTime);
    }

    /** Returns the ripple of a scope signal. */
    public static double getRipple(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getRipple(scopeName, scopePort, startTime, endTime);
    }

    /** Returns the klirr factor of a scope signal. */
    public static double getKlirr(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getKlirr(scopeName, scopePort, startTime, endTime);
    }

    /** Returns the shape factor of a scope signal. */
    public static double getShape(String scopeName, int scopePort, double startTime, double endTime) {
        checkExternal();
        return external.getShape(scopeName, scopePort, startTime, endTime);
    }

    /** Convenience overload for scope port 0 signal characteristics. */
    public static double[] getSignalCharacteristics(String scopeName, double startTime, double endTime) {
        return getSignalCharacteristics(scopeName, 0, startTime, endTime);
    }

    /** Convenience overload for average on scope port 0. */
    public static double getAvg(String scopeName, double startTime, double endTime) {
        return getAvg(scopeName, 0, startTime, endTime);
    }

    /** Convenience overload for RMS on scope port 0. */
    public static double getRMS(String scopeName, double startTime, double endTime) {
        return getRMS(scopeName, 0, startTime, endTime);
    }

    /** Convenience overload for min on scope port 0. */
    public static double getMin(String scopeName, double startTime, double endTime) {
        return getMin(scopeName, 0, startTime, endTime);
    }

    /** Convenience overload for max on scope port 0. */
    public static double getMax(String scopeName, double startTime, double endTime) {
        return getMax(scopeName, 0, startTime, endTime);
    }

    /** Convenience overload for THD on scope port 0. */
    public static double getTHD(String scopeName, double startTime, double endTime) {
        return getTHD(scopeName, 0, startTime, endTime);
    }

    /** Convenience overload for shape on scope port 0. */
    public static double getShape(String scopeName, double startTime, double endTime) {
        return getShape(scopeName, 0, startTime, endTime);
    }

    /** Convenience overload for klirr on scope port 0. */
    public static double getKlirr(String scopeName, double startTime, double endTime) {
        return getKlirr(scopeName, 0, startTime, endTime);
    }

    /** Convenience overload for ripple on scope port 0. */
    public static double getRipple(String scopeName, double startTime, double endTime) {
        return getRipple(scopeName, 0, startTime, endTime);
    }

    /** Returns Fourier analysis results for a scope signal. */
    public static double[][] getFourier(String scopeName, int scopePort, double startTime, double endTime, int harmonics) {
        checkExternal();
        return external.getFourier(scopeName, scopePort, startTime, endTime, harmonics);
    }

    /** Convenience overload for Fourier on scope port 0. */
    public static double[][] getFourier(final String scopeName, final double startTime, final double endTime,
            final int harmonics) {
        return getFourier(scopeName, 0, startTime, endTime, harmonics);
    }
    
    /** Initializes steady-state detection with the given parameters. */
    public static void initSteadyStateDetection(final String[] stateVariables, final double[] frequencies,
            final double deltaT, final double simulationTime) {
        checkExternal();
        external.initSteadyStateDetection(stateVariables, frequencies, deltaT, simulationTime);
    }

    /** Runs the simulation until steady state is detected. */
    public static double[] simulateUntilSteadyState(final boolean suppressMessages) {
        checkExternal();
        return external.simulateUntilSteadyState(suppressMessages);
    }

    /** Sets a loss file for a semiconductor element. */
    public static void setLossFile(final String elementName, final String lossFileName) throws FileNotFoundException {
        checkExternal();
        external.setLossFile(elementName, lossFileName);
    }

    /** Returns the time array for a signal. */
    public static double[] getTimeArray(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        checkExternal();
        return external.getTimeArray(signalName, tStart, tEnd, skipPoints);
    }

    /** Returns the signal data as a float array. */
    public static float[] getSignalData(final String signalName, final double tStart, final double tEnd, final int skipPoints) {
        checkExternal();
        return external.getSignalData(signalName, tStart, tEnd, skipPoints);
    }

    /** Imports a subcircuit from an external file. */
    public static void importFromFile(final String fileName, final String subCircuitName) throws FileNotFoundException {
        checkExternal();
        try {
            external.importFromFile(fileName, subCircuitName);
        } catch (RemoteException ex) {
            Logger.getLogger(GeckoExternal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Creates a new component in the schematic. Note: this is an instance
     * method while most other accessors are static.
     */
    public final void createComponent(final String elementType, final String elementName, final int xPosition, final int yPosition) {        
        checkExternal();
        external.createComponent(elementType, elementName, xPosition, yPosition);                
    }
    
    /**
     * Creates a connector (wire) in the schematic. Note: this is an instance
     * method while most other accessors are static.
     */
    public void createConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal) {
        checkExternal();
        external.createConnector(elementName, xStart, yStart, xEnd, yEnd, startHorizontal);
    }
    
    /** Deletes a component from the schematic. */
    public static void deleteComponent(final String elementName) {
        checkExternal();
        external.deleteComponent(elementName);
    }
    
    /** Deletes all components from the given subcircuit. */
    public static void deleteAllComponents(final String subcircuitName) {
        checkExternal();
        external.deleteAllComponents(subcircuitName);
    }
    

    /**
     * Rename a component with a given name.
     *
     * @param oldName for selection of component
     * @param newName the new name that should be given. Throws an Exception, if
     * name is already in use!
     * @throws Exception Setting the name was not possible, since another
     * component uses this name already.
     */
    public static void setComponentName(final String oldName, final String newName) throws Exception {
        checkExternal();
        try {
            external.setComponentName(oldName, newName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /** Sets the position of a component. */
    public static void setPosition(final String elementName, final int xCoord, final int yCoord) {
        checkExternal();
        external.setPosition(elementName, xCoord, yCoord);
    }
    
    /** Returns the position of a component. */
    public static int[] getPosition(final String elementName) {
        checkExternal();
        return external.getPosition(elementName);
    }
    
    /** Sets the name of an output node. */
    public static void setOutputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        checkExternal();
        external.setOutputNodeName(elementName, nodeIndex, nodeName);
    }

    /** Sets the name of an input node. */
    public static void setInputNodeName(final String elementName, final int nodeIndex, final String nodeName) {
        checkExternal();
        external.setInputNodeName(elementName, nodeIndex, nodeName);
    }
    
    /** Returns the name of an output node. */
    public static String getOutputNodeName(final String elementName, final int nodeIndex) {
        checkExternal();
        return external.getOutputNodeName(elementName, nodeIndex);
    }

    /** Returns the name of an input node. */
    public static String getInputNodeName(final String elementName, final int nodeIndex) {
        checkExternal();
        return external.getInputNodeName(elementName, nodeIndex);
    }
    
    /**
     * Performs an in-place forward FFT on a float array. The array length must
     * be a power of 2.
     */
    public static float[] realFFT(final float[] timeValues) {
        Cispr16Fft.realft(timeValues, 1);
        return timeValues;
    }
}
