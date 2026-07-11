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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.general.ProjectData;
import ch.technokrat.gecko.geckocircuits.general.MainWindow;
import ch.technokrat.gecko.geckocircuits.general.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.general.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.Enabled;
import ch.technokrat.gecko.geckocircuits.circuit.SchematicEditor2;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.datacontainer.*;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("deprecation")
public final class ControlSaveData extends ControlBlock implements Operationable {

    private static final long serialVersionUID = 1L;
    transient final UserParameter<Boolean> _printHeader = UserParameter.Builder.
            <Boolean>start("printHeader", true).                       
            longName(I18nKeys.IF_TRUE_PRINT_HEADER).
            shortName("printHeader").            
            arrayIndex(this, -1).
            build();                               
    
    transient final UserParameter<Boolean> _transposeData = UserParameter.Builder.
            <Boolean>start("transposeData", false).                       
            longName(I18nKeys.IF_TRUE_TRANSPOSE_DATA).
            shortName("transposeData").            
            arrayIndex(this, -1).
            build();                               
    
    transient final UserParameter<Integer> _skipDataPoints = UserParameter.Builder.
            <Integer>start("skipDataPoints", 1).                       
            longName(I18nKeys.SKIP_DATA_POINTS).
            shortName("skipDataPoints").            
            arrayIndex(this, -1).
            build();             
    
    transient final UserParameter<Integer> _significDigits = UserParameter.Builder.
            <Integer>start("significantDigits", DEFAULT_DIGITS).                       
            longName(I18nKeys.SIGNIFICANT_DIGITS).
            shortName("significantDigits").            
            arrayIndex(this, -1).
            build();     
    
    transient final UserParameter<String> _file = UserParameter.Builder.
            <String>start("filename", findInitialFile()).                  
            longName(I18nKeys.FILENAME).
            shortName("fileName").            
            arrayIndex(this, -1).
            build();             
    
    private static final int HUNDRED_PERCENT = 100;
    private static final int BLOCK_HEIGHT = 3;
    private static final int BLOCK_WIDTH = 6;
    private static final int MAX_FILE_COUNTER = 100;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlSaveData.class, "DataExport", I18nKeys.DATA_EXPORT_TO_FILE);
    private transient DataSaver _dataSaver;
    private String _statusTxt = "waiting";

    
    public ControlSaveData() {
        super(0, 0);
        _textInfo.setTextNeverVisible();
    }
    
    enum OutputType {

        BINARY,
        TEXT;
    }

    enum SaveModus {

        MANUAL,
        SIMULATION_END,
        DURING_SIMULATION;

        static SaveModus getFromOrdinal(final int testOrdinal) {
            for (SaveModus modus : SaveModus.values()) {
                if (modus.ordinal() == testOrdinal) {
                    return modus;
                }
            }
            assert false;
            return null;
        }
    }

    enum FileOverwrite {

        OVERWRITE,
        DO_NUMBERING;

        private static FileOverwrite getFromOrdinal(final int testOrdinal) {
            for (FileOverwrite modus : FileOverwrite.values()) {
                if (modus.ordinal() == testOrdinal) {
                    return modus;
                }
            }
            assert false;
            return null;
        }
    }
    TextSeparator _itemSeparator = TextSeparator.SPACE;
    HeaderSymbol _headerSymbol = HeaderSymbol.HASH;
    private static final int DEFAULT_DIGITS = 4;
    SaveModus _saveModus = SaveModus.MANUAL;
    OutputType _outputType = OutputType.TEXT;
    
    FileOverwrite _fileOverwrite = FileOverwrite.OVERWRITE;
    private final transient List<String> _selectedSignalNames = new ArrayList<String>();
    private final transient List<Integer> _selectedSignalIndices = new ArrayList<Integer>();


    public void setSelectedSignals(final DataIndexItem[] listItems) {
        _selectedSignalNames.clear();
        _selectedSignalIndices.clear();

        for (DataIndexItem item : listItems) {
            _selectedSignalNames.add(item.toString());
            _selectedSignalIndices.add(item.getIndex());
        }
    }

    void setSelectedSignal(int j, int i) {
        _selectedSignalIndices.set(i, j);
    }

    void setSignalName(final int index, final String dataSignalNameNew) {
        _selectedSignalNames.set(index, dataSignalNameNew);
    }

    public List<String> getSelectedNames() {
        return Collections.unmodifiableList(_selectedSignalNames);
    }

    public List<Integer> getSelectedSignalIndices() {
        return Collections.unmodifiableList(_selectedSignalIndices);
    }

    public boolean isSaveDuringSimulation() {
        return _saveModus == SaveModus.DURING_SIMULATION;
    }

    void removeSignal(final int removeIndex) {
        _selectedSignalNames.remove(removeIndex);
        _selectedSignalIndices.remove(removeIndex);
    }

    private String findInitialFile() {
        if (GlobalFilePathes.DATNAM != null) {
            File ipesFile = new File(GlobalFilePathes.DATNAM);
            String parentDirectory = ipesFile.getParent();
            int testCounter = 1;
            
            while (testCounter < MAX_FILE_COUNTER) {
                String filePath = parentDirectory + "/data" + testCounter + ".txt";
                File file = new File(filePath);
                if(!file.exists()) {
                    return filePath;
                }
                testCounter++;
                
            }
        }
        
        return "data.txt";
    }            

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public int getBlockHeight() {
        return dpix * BLOCK_HEIGHT;
    }

    @Override
    public int getBlockWidth() {
        return dpix * BLOCK_WIDTH;
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (_isEnabled.getValue() == Enabled.ENABLED) {
            _dataSaver = new DataSaver(NetlistControl.globalData, this);
            _dataSaver.addObserver(new Observer() {
                @Override
                public void update(final Observable obs, final Object arg) {
                    setPercentageText(_dataSaver.getPercentage());
                }
            });
        }
        return null;
    }

    private void setPercentageText(final int percentage) {
        if (isSaveDuringSimulation()) {
            _statusTxt = "Continuous Saving";

        } else {
            if (percentage == HUNDRED_PERCENT) {
                _statusTxt = "Finished";
            } else {
                _statusTxt = "Saving: " + percentage + "%";
            }
        }

        SchematicEditor2.Singleton._circuitSheet.repaint();
    }

    @Override
    protected String getCenteredDrawString() {
        return new File(_file.getValue()).getName() + "\n" + _statusTxt;
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {        
        ProjectData.appendAsString(ascii.append("\nselectedSignalNames"), _selectedSignalNames.toArray(
                new String[_selectedSignalNames.size()]));
        ProjectData.appendAsString(ascii.append("\nselectedSignalIndices"), _selectedSignalIndices);
        ProjectData.appendAsString(ascii.append("\nitemSeparator"), _itemSeparator.ordinal());
        ProjectData.appendAsString(ascii.append("\nheaderSymbol"), _headerSymbol.ordinal());
        ProjectData.appendAsString(ascii.append("\nsaveModus"), _saveModus.ordinal());
        ProjectData.appendAsString(ascii.append("\nfileOverwrite"), _fileOverwrite.ordinal());
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {        
        _itemSeparator = TextSeparator.getFromOrdinal(tokenMap.readDataLine("itemSeparator", _itemSeparator.ordinal()));
        _headerSymbol = HeaderSymbol.getFromOrdinal(tokenMap.readDataLine("headerSymbol", _headerSymbol.ordinal()));
        _selectedSignalNames.clear();
        _selectedSignalNames.addAll(Arrays.asList(tokenMap.readDataLine("selectedSignalNames[]", new String[0])));

        _selectedSignalIndices.clear();
        int[] tmp = new int[0];
        tmp = tokenMap.readDataLine("selectedSignalIndices[]", tmp);
        for (int value : tmp) {
            _selectedSignalIndices.add(value);
        }
        _saveModus = SaveModus.getFromOrdinal(tokenMap.readDataLine("saveModus", _saveModus.ordinal()));
        _fileOverwrite = FileOverwrite.getFromOrdinal(tokenMap.readDataLine("fileOverwrite", _fileOverwrite.ordinal()));        
    }
    

    @Override
    protected Window openDialogWindow() {
        final List<AbstractDataContainer> selectableContainers = new ArrayList<AbstractDataContainer>();
        selectableContainers.add(NetlistControl.globalData);
        return new DialogDataExport(null, true, this, selectableContainers, _dataSaver);
    }

    @Override
    public List<OperationInterface> getOperationEnumInterfaces() {
        final List<OperationInterface> returnValue = new ArrayList<OperationInterface>();
        
        returnValue.add(new OperationInterface("doSaveFile", I18nKeys.SAVE_DATA_DOC) {
            @Override            
            public Object doOperation(final Object parameterValue) {
                _dataSaver.doManualSave();
                return null;
            }
        });
        
        returnValue.add(new OperationInterface("doSaveFileBlocking", I18nKeys.SAVE_DATA_DOC_BLOCKING) {
            @Override            
            public Object doOperation(final Object parameterValue) {                
                _dataSaver.doManualSaveBlocking();
                return null;
            }
        });
        
        returnValue.add(new OperationInterface("setOutputFileName", I18nKeys.SAVE_DATA_DOC_BLOCKING) {
            @Override            
            public Object doOperation(final Object parameterValue) {
                _file.setUserValue(parameterValue.toString());
                return null;
            }
        });
        
        return Collections.unmodifiableList(returnValue);
    }
}
