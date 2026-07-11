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
package ch.technokrat.gecko.geckoscript;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.general.MainWindow;
import ch.technokrat.gecko.geckocircuits.general.GeckoFile;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.*;
import ch.technokrat.gecko.geckocircuits.control.DataSaver;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Provides script (GeckoSCRIPT) access to the simulation engine, enabling
 * starting, continuing, and controlling simulations from external scripts.
 * <p>
 * Thread safety: Methods that modify UI state must be called on the Event
 * Dispatch Thread (EDT). Simulation control methods ({@link #startSim()},
 * {@link #continueSim()}, etc.) are typically called from script threads.
 */
public class SimulationAccess implements GeckoFileable {

    final static long DUMMY_BLOCK_ID = -1231231987;
    final List<GeckoFile> _additionalSourceFiles = new ArrayList<GeckoFile>();
    private boolean _populateFileList = false;
    private final Set<String> _additionalFilesHashKeys = new TreeSet<>();

    private ScriptWindow scriptwindow;
    public SchematicEditor2 se;
    private MainWindow mainWindow;

    @SuppressWarnings("this-escape")
    /**
     * Creates a new SimulationAccess instance linked to the given main
     * window.
     * @param fenster the main application window
     */
    public SimulationAccess(final MainWindow fenster) {
        se = SchematicEditor2.Singleton;
        mainWindow = fenster;
        assert mainWindow != null;
        try {
            scriptwindow = new ScriptWindow(this);

        } catch (Throwable ex) {
            System.out.println("Could not find editor library jsyntaxpane.jar. Scripting tool disabled.");
            // ex.printStackTrace();
        }

    }

    /**
     * Returns whether the scripting window is available (JSyntaxPane
     * library found on classpath).
     * @return true if scripting is enabled
     */
    public boolean isScripterEnabled() {
        return scriptwindow != null;
    }

    /**
     * Starts a new simulation from the beginning. Blocking call; waits
     * for data savers to complete.
     * @throws RuntimeException if simulation fails to start
     */
    public void startSim() {
        try {
            mainWindow.getSimulationRunner().startCalculation(false, MainWindow.getSolverSettings());
            waitForDataSavers();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    /**
     * Continues a paused or finished simulation.
     * @throws RuntimeException if continuation fails
     */
    public void continueSim() {
        try {
            mainWindow.continueCalculation(false);
            waitForDataSavers();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    private void waitForDataSavers() {
        int counter = 0;
        while (DataSaver.WAIT_COUNTER.get() != 0 && counter < 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimulationAccess.class.getName()).log(Level.SEVERE, null, ex);
            }
            counter++;
        }
    }

    /**
     * Initializes the simulation with default dt and end time from solver
     * settings.
     * @throws RuntimeException if initialization fails
     */
    public void initializeSimulation() {
        try {
            mainWindow.getSimulationRunner().initSim();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Initializes the simulation with the given time step and end time.
     * @param dt the simulation time step
     * @param endTime the end time for the simulation
     * @throws RuntimeException if initialization fails
     */
    public void initializeSimulation(double dt, double endTime) {
        try {
            mainWindow.getSimulationRunner().initSim(dt, endTime);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Simulates a single time step.
     * @throws Exception if the end time has been reached
     */
    public void simulateOneStep() throws Exception {
        mainWindow.getSimulationRunner().simKern.simulateOneStep();
    }

    /**
     * Simulates for the specified duration (in seconds) from the current
     * time.
     * @param time the duration to simulate
     * @throws Exception if the specified time exceeds the end time
     */
    public void simulateSpecifiedTime(double time) throws Exception {
        mainWindow.getSimulationRunner().simKern.simulateTime(time);
    }

    /**
     * Ends the current simulation.
     */
    public void endSimulation() {
        mainWindow.endSim();
    }

    /**
     * Makes the script window visible. Must be called on the EDT.
     */
    public void makeVisible() {
        if (scriptwindow != null) {
            scriptwindow.setVisible(true);
        }
    }

    /**
     * Returns all circuit components of the specified type.
     * @param <T> the component type
     * @param searchClass the class to search for
     * @return list of matching components
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractCircuitBlockInterface> List<T> getComponentsOfType(Class<T> searchClass) {
        List<T> returnValue = new ArrayList<T>();
        for (AbstractCircuitBlockInterface elem : se.getElementLK()) {
            AbstractCircuitBlockInterface block = elem;
            if (searchClass.isAssignableFrom(block.getClass())) {
                returnValue.add((T) block);
            }
        }
        return returnValue;
    }

    /**
     * Returns all elements (LK, control, thermal, special) sorted by type.
     * @return list of lists, each containing elements of the same type
     */
    public List<List<AbstractBlockInterface>> getElementsSorted() {
        List<List<AbstractBlockInterface>> returnValue = doListSort(se.getElementLK());
        returnValue.addAll(doListSort(se.getElementCONTROL()));
        returnValue.addAll(doListSort(se.getElementTHERM()));
        returnValue.addAll(doListSort(se.getElementSpecial()));
        return returnValue;
    }

    private static List<List<AbstractBlockInterface>> doListSort(final List<? extends AbstractBlockInterface> unsorted) {
        List<List<AbstractBlockInterface>> sortedListofLists = new ArrayList<>();
        List<Class<?>> types = new ArrayList<>();
        HashMap<Class<?>, List<AbstractBlockInterface>> listsByType = new HashMap<>();
        List<AbstractBlockInterface> currentList;

        for (AbstractBlockInterface elem : unsorted) {
            Class<?> elementClass = elem.getClass();
            currentList = listsByType.get(elementClass);
            if (currentList == null) {
                currentList = new ArrayList<>();
                currentList.add(elem);
                listsByType.put(elementClass, currentList);
                types.add(elementClass);
            } else {
                currentList.add(elem);
            }
        }

        for (Class<?> elemType : types) {
            sortedListofLists.add(listsByType.get(elemType));
        }

        return sortedListofLists;
    }

    /**
     * Sets the script code in the script window.
     * @param scripterCode the script source code
     */
    public void setScriptCode(String scripterCode) {
        if (scriptwindow != null) {
            scriptwindow.setScripterCode(scripterCode);
        }
    }

    /**
     * Returns the declaration code from the script window.
     * @return the declaration code, or empty string if scripting disabled
     */
    public String getDeclarationCode() {
        if (scriptwindow != null) {
            return scriptwindow.getDeclarationCode();
        } else {
            return "";
        }

    }

    /**
     * Returns the import code from the script window.
     * @return the import code, or empty string if scripting disabled
     */
    public String getImportCode() {
        if (scriptwindow != null) {
            return scriptwindow.getImportCode();
        } else {
            return "";
        }

    }

    /**
     * Sets the declaration code in the script window.
     * @param code the declaration source code
     */
    public void setDeclarationCode(String code) {
        if (scriptwindow != null) {
            scriptwindow.setDeclarationCode(code);
        }

    }

    /**
     * Sets the import code in the script window.
     * @param code the import source code
     */
    public void setImportCode(String code) {
        if (scriptwindow != null) {
            scriptwindow.setImportCode(code);
        }

    }

    /**
     * Returns the script source code from the script window.
     * @return the script source, or empty string if scripting disabled
     */
    public String getScriptCode() {
        if (scriptwindow != null) {
            return scriptwindow.getSourceCode();
        } else {
            return "";
        }

    }

    void set_Tend(double Tend) {
        MainWindow.getSolverSettings()._tDURATION.setValueWithoutUndo(Tend);
    }

    void set_Tend_pre(double Tend) {
        MainWindow.getSolverSettings()._T_pre.setValueWithoutUndo(Tend);
    }

    void set_dt(double value) {
        MainWindow.getSolverSettings().dt.setValue(value);
    }

    void set_dt_pre(double value) {
        MainWindow.getSolverSettings()._dt_pre.setValueWithoutUndo(value);
    }

    void saveFileAs(String fileName) {
        mainWindow.rawSaveFile(new File(fileName));
    }

    void openFile(String fileName) throws FileNotFoundException {
        mainWindow.openFile(fileName);
    }

    /**
     * Imports components from an external file into a subcircuit.
     * @param fileName the path to the file to import
     * @param importIntoSubcircuit the name of the target subcircuit
     * @throws FileNotFoundException if the file does not exist
     */
    public final void importFromFile(final String fileName, final String importIntoSubcircuit)
            throws FileNotFoundException {
        MainWindow.importComponentsFromFile(fileName, importIntoSubcircuit);
    }

    File getCurrentModelFile() {
        return new File(MainWindow.getOpenFileName());
    }

    /**
     * Returns signal characteristics for a scope channel.
     * @param scopename the name of the scope component
     * @param port the channel port index
     * @param start_time the start time for analysis
     * @param end_time the end time for analysis
     * @return signal characteristic data
     * @throws Exception if the scope is not found or analysis fails
     */
    public double[] getSignalCharacteristics(String scopename, int port, double start_time, double end_time)
            throws Exception {
        AbstractBlockInterface block = IDStringDialog.getComponentByName(scopename);

        if (!(block instanceof ControlOSZI)) {
            throw new Exception("Supplied element " + scopename + "to getSignalCharacteristics function is not a SCOPE");
        } else {
            ControlOSZI scope = (ControlOSZI) block;
            return scope.getChannelCharacteristics(port, start_time, end_time);
        }

    }

    /**
     * Performs Fourier analysis on a scope channel.
     * @param scopename the name of the scope component
     * @param port the channel port index
     * @param start_time the start time for analysis
     * @param end_time the end time for analysis
     * @param harmonics the number of harmonics to compute
     * @return frequency and magnitude data
     * @throws Exception if the scope is not found or analysis fails
     */
    public double[][] doFourierAnalysis(String scopename, int port, double start_time, double end_time, int harmonics) throws Exception {
        AbstractBlockInterface block = IDStringDialog.getComponentByName(scopename);

        if (!(block instanceof ControlOSZI)) {
            throw new Exception("Supplied element " + scopename + "to getSignalCharacteristics function is not a SCOPE");
        } else {
            ControlOSZI scope = (ControlOSZI) block;
            return scope.doFourierAnalysis(port, start_time, end_time, harmonics);
        }
    }

    /**
     * Returns the current simulation time step.
     * @return the time step dt
     */
    public double get_dt() {
        return MainWindow.getSolverSettings().dt.getValue();
    }

    double get_dt_pre() {
        return MainWindow.getSolverSettings()._dt_pre.getValue();
    }

    double get_Tend_pre() {
        return MainWindow.getSolverSettings()._T_pre.getValue();
    }

    double get_Tend() {
        return MainWindow.getSolverSettings()._tDURATION.getValue();
    }

    /**
     * Clears the GeckoCustom script object, typically called after opening
     * a new file.
     */
    public void clearData() {
        if (scriptwindow != null) {
            scriptwindow.clearObject();
        }

    }

    /**
     * Moves a circuit element to the specified sheet position.
     * @param element the element to move
     * @param x the target x coordinate
     * @param y the target y coordinate
     * @throws Exception if the position is outside the worksheet
     */
    public void setElementPosition(AbstractBlockInterface element, int x, int y) throws Exception {
        boolean positionOK = isPositionValid(/*
                 * element,
                 */x, y);

        if (positionOK) {
            Point originalPoint = element.getPositionBeforeMoving();
            element.moveComponent(new Point(x - originalPoint.x, y - originalPoint.y));
            element.absetzenElement();
        }
    }
        

    private boolean isPositionValid(/*
             * ElementInterface element,
             */int x, int y) throws Exception {
        boolean valid = false;
        int worksheetSizeX = MainWindow.getSchematicEditor()._circuitSheet._worksheetSize.getSizeX();
        int worksheetSizeY = MainWindow.getSchematicEditor()._circuitSheet._worksheetSize.getSizeY();
        if (x >= worksheetSizeX || y >= worksheetSizeY) {
            throw new Exception("Given position is outside defined drawing area! Sheet size is " + worksheetSizeX + "x" + worksheetSizeY + " and given new position is " + x + "x" + y + ".");
        } else {
            //here ideally we should check full size of component
            //this we can to by calling the component's getAnfangsKnoten() and getEndKnoten() methods
            //BUT - to update this, we first have to set the new coordinates and then re-paint the component
            //this is too complicated for now, so just give a "buffer zone" around the component and check if position is far enough away from the sides
            //of the sheet
            int bufferSpace = 3;
            if (y < bufferSpace || x < bufferSpace || (worksheetSizeY - y) < bufferSpace || (worksheetSizeX - x) < bufferSpace) {
                valid = true;
                //throw new Exception("Given position is too close to worksheet sides. Sheet size is " + worksheetSizeX + "x" + worksheetSizeY + " and given new position is " + x + "x" + y + ".");
            } else {
                valid = true;
            }
        }
        return valid;
    }

    /**
     * Deletes a circuit element from the schematic.
     * @param element the element to delete
     */
    public void deleteElement(AbstractBlockInterface element) {
        se.deleteComponent(element);
    }

    void createNewConnector(String elementName, int xStart, int yStart, int xEnd, int yEnd, boolean startHorizontal) throws Exception {
        boolean startPositionOK = isPositionValid(xStart, yStart);
        boolean endPositionOK = isPositionValid(xEnd, yEnd);
        AbstractCircuitSheetComponent newElement = null;
        if (startPositionOK && endPositionOK) {
            newElement = se.externalCreateAndPlaceNewConnector(elementName, xStart, yStart, xEnd, yEnd, startHorizontal);
            if (newElement == null) {
                throw new Exception("New element not created - error!");
            }
        }
    }

    /**
     * Creates and places a new circuit element on the sheet.
     * @param elementCategory the type category of the element
     * @param elementName the display name for the new element
     * @param x the x position on the sheet
     * @param y the y position on the sheet
     * @return the created element
     * @throws Exception if the position is invalid or creation fails
     */
    public AbstractBlockInterface createNewElement(final AbstractTypeInfo elementCategory,
            final String elementName, final int x, final int y) throws Exception {

        boolean positionOK = isPositionValid(x, y);
        AbstractBlockInterface newElement = null;
        if (positionOK) {
            newElement = se.externalCreateAndPlaceNewElement(elementName, elementCategory, x, y);
            if (newElement == null) {
                throw new Exception("New element not created - error!");
            }
        }
        return newElement;
    }

    /**
     * Renames a circuit element with a new unique name.
     * @param element the element to rename
     * @param newName the new name for the element
     * @throws NameAlreadyExistsException if the name is already in use
     */
    public void renameElement(AbstractBlockInterface element, String newName) throws NameAlreadyExistsException {

        if (newName.isEmpty()) {
            throw new IllegalArgumentException("Error: cannot insert emtyp name!");
        }
        String oldName = element.getStringID();
        element.setNewNameChecked(newName);
        se.updateComponentCouplings(oldName, newName);
    }

    /**
     * Sets the label on a circuit element's terminal node.
     * @param element the circuit element
     * @param labelType whether the terminal is a start (input) or stop (output) node
     * @param nodeIndex the index of the terminal
     * @param labelName the new label name
     * @throws Exception if the label type is invalid
     */
    public void setElementNodeLabel(final AbstractBlockInterface element, final AbstractGeckoCustom.StartOrStopNode labelType,
            final int nodeIndex, final String labelName) throws Exception {

        AbstractTerminal terminal = null;
        switch (labelType) {
            case START_NODE:
                terminal = element.XIN.get(nodeIndex);
                break;
            case STOP_NODE:
                terminal = element.YOUT.get(nodeIndex);
                break;
            default:
                throw new Exception("Invalid label type: neither output nor input node!");
        }

        final ConnectorType terminalType = terminal.getCategory();
        
               
        final CircuitLabel label = terminal.getLabelObject();
        final String oldLabel = label.getLabelString();        
        label.setLabelFromUserDialog(labelName);
                
        final NetlistGeneral netzlisteAllg1 = NetlistGeneral.fabricNetzlistComponentLabelUpdate(element, terminalType);        
                                        
        se.updateRenamedLabel(oldLabel, labelName, terminalType);                
        se.setDirtyFlag();
    }
    
    
    /**
     * Returns the label on a circuit element's terminal node.
     * @param element the circuit element
     * @param labelType whether the terminal is a start (input) or stop (output) node
     * @param nodeIndex the index of the terminal
     * @return the label string
     * @throws Exception if the label type is invalid
     */
    public String getElementNodeLabel(final AbstractBlockInterface element, final AbstractGeckoCustom.StartOrStopNode labelType,
            final int nodeIndex) throws Exception {

        AbstractTerminal terminal = null;
        switch (labelType) {
            case START_NODE:
                terminal = element.XIN.get(nodeIndex);
                break;
            case STOP_NODE:
                terminal = element.YOUT.get(nodeIndex);
                break;
            default:
                throw new Exception("Invalid label type: neither output nor input node!");
        }
        
                       
        final CircuitLabel label = terminal.getLabelObject();        
        return label.getLabelString();        
    }

    /**
     * Returns the current simulation time from the simulation kernel.
     * @return the current simulation time in seconds
     */
    public double getSimulationTime() {        
        return GeckoSim._win.getSimulationRunner().simKern.getCurrentTime();
    }

    /**
     * Adds external source files to the script environment.
     * @param newFiles the files to add
     */
    @Override
    public void addFiles(List<GeckoFile> newFiles) {
        for (GeckoFile newFile : newFiles) {
            _additionalSourceFiles.add(newFile);
            newFile.setUser(DUMMY_BLOCK_ID);
            MainWindow.getFileManager().addFile(newFile);
        }
        scriptwindow._extSourceWindow.addNewFiles(newFiles);
    }

    /**
     * Returns the list of additional source files.
     * @return the list of GeckoFile objects
     */
    @Override
    public List<GeckoFile> getFiles() {
        return _additionalSourceFiles;
    }

    /**
     * Removes external source files and cleans up references.
     * @param filesToRemove the files to remove
     */
    @Override
    public void removeLocalComponentFiles(List<GeckoFile> filesToRemove) {
        for (GeckoFile removedFile : filesToRemove) {
            _additionalSourceFiles.remove(removedFile);
            removedFile.removeUser(DUMMY_BLOCK_ID);
            MainWindow.getFileManager().maintain(removedFile);
        }

        scriptwindow._extSourceWindow.removeFilesFromList(filesToRemove);
    }

    /**
     * Returns the hash values of all additional source files.
     * @return concatenated hash values
     */
    public String getExtraFilesHashes() {
        String returnValue = "";
        for (GeckoFile gFile : _additionalSourceFiles) {
            returnValue += gFile.getHashValue();
        }
        return returnValue;
    }

    /**
     * Sets the hash block for identifying additional source files on init.
     * @param extraFilesHashString newline-separated hash strings
     */
    public void setExtraFilesHashBlock(final String extraFilesHashString) {
        _additionalFilesHashKeys.clear();
        _additionalFilesHashKeys.addAll(Arrays.asList(extraFilesHashString.split("\\r?\\n")));
        _populateFileList = true;
    }

    /**
     * Initializes additional source files from stored hash keys.
     */
    @Override
    public void initExtraFiles() {
        if (scriptwindow != null) {
            scriptwindow._extSourceWindow.removeFilesFromList(_additionalSourceFiles);
            _additionalSourceFiles.clear();
        }

        if (_additionalFilesHashKeys.isEmpty()) {
            return;
        }

        if (_populateFileList) {
            long hashValue;
            GeckoFile file;
            boolean fileMissing = false;
            int filesMissing = 0;

            for (String hash : _additionalFilesHashKeys) {
                if (hash.trim().isEmpty()) {
                    continue;
                }
                hashValue = Long.valueOf(hash.trim());
                try {
                    file = MainWindow.getFileManager().getFile(hashValue);
                    _additionalSourceFiles.add(file);
                } catch (Exception e) {
                    fileMissing = true;
                    filesMissing++;
                }
            }
            if (fileMissing) {
                final String errorMessage = filesMissing + " additional source files missing in GeckoSCRIPT code";
                final String errorTitle = "ERROR - File(s) not found";
                JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            }
        }
        if (scriptwindow != null) {
            scriptwindow._extSourceWindow.addNewFiles(_additionalSourceFiles);
        }
    }

    void setWorksheetSize(int sizeX, int sizeY) {
        MainWindow.getSchematicEditor()._circuitSheet._worksheetSize.setNewWorksheetSize(sizeX, sizeY);
    }

    int[] getWorksheetSize() {
        int sizeX = MainWindow.getSchematicEditor()._circuitSheet._worksheetSize.getSizeX();
        int sizeY = MainWindow.getSchematicEditor()._circuitSheet._worksheetSize.getSizeY();
        return new int[]{sizeX, sizeY};
    }

}
