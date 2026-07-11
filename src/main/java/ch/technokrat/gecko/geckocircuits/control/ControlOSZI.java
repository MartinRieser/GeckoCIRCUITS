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
import ch.technokrat.gecko.geckocircuits.general.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerNullData;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerScopeWrapper;
import ch.technokrat.gecko.geckocircuits.datacontainer.ScopeWrapperIndices;
import ch.technokrat.gecko.geckocircuits.newscope.*;
import ch.technokrat.gecko.geckoscript.GeckoInvalidArgumentException;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Oscilloscope (SCOPE) component for real-time visualization of simulation data.
 * Displays waveforms from multiple input terminals with configurable diagrams,
 * mean signals, and Fourier analysis support.
 *
 * @author andreas
 */
@SuppressWarnings("deprecation")
public final class ControlOSZI extends ControlBlock implements VariableTerminalNumber,
        SpecialNameVisible {

    private static final long serialVersionUID = 1L;

    private static final int TERM_POS_X = -2;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlOSZI.class, "SCOPE", I18nKeys.SCOPE, I18nKeys.COMPONENT_FOR_DATA_VISUALIZATION);
    
    transient final UserParameter<Integer> _inputTerminalNumber = UserParameter.Builder.
            <Integer>start("tn", 0).
            longName(I18nKeys.NO_INPUT_TERMINALS).
            shortName("numberInputTerminals").
            arrayIndex(this, -1).
            build();
    /** Click areas for red triangles --> change the number of terminals*/
    private int _xKlickMinTerm, _xKlickMaxTerm, _yKlickMinTermADD,
            _yKlickMaxTermADD, _yKlickMinTermSUB, _yKlickMaxTermSUB;
    // // for access to SCOPE and the ability to update the labels when the number of terminals is changed
    // // all ZV data not compressed for possible hard disk storage --> storage critical
    private transient AbstractDataContainer _zvDatenRAM;
    //for use with GeckoSCRIPT - waveform characteristic
    /** Calculator for waveform characteristics (rise time, overshoot, etc.) used by GeckoSCRIPT. */
    private transient CharacteristicsCalculator _waveformChar;
    /** Start time of the waveform characteristic analysis window. */
    private double _charStart = 0;
    /** End time of the waveform characteristic analysis window. */
    private double _charEnd = 1;
    //for use with GeckoSCRIPT - Fourier analysis
    /** Cached Fourier analysis results: [coefficient][channel][harmonic value]. */
    private double[][][] _fourier = null;
    /** Start time of the Fourier analysis window. */
    private double _fourStart = 0;
    /** End time of the Fourier analysis window. */
    private double _fourEnd = 1;
    private static final int DELTA = 3;  // // Distance from the red triangle to the SCOPE block (up or down)
    private static final int INSIDE_RECT = 2;
    private static final int FOUR_CHAN_DEPTH = 4;
    public static final int DEF_TERM_NUMBER = 3;
    //for reading the correct rows from the global DataContainer
    private transient ScopeWrapperIndices _scopeWrapperIndices;
    private String[] _saveLoadSignalNames;
    private final ScopeSettings _scopeSettings = new ScopeSettings();  // initiale ScopeSettings definieren   ;    
    private final GraferV4 _grafer = new GraferV4(_scopeSettings);
    public ScopeFrame _scopeFrame = new ScopeFrame(_grafer);
    private boolean _isShowName;
    Stack<AbstractScopeSignal> _scopeInputSignals = new Stack<AbstractScopeSignal>();
    private final transient DefinedMeanSignals _meanSignals = new DefinedMeanSignals(_scopeInputSignals);
    
    private static final int DIAMETER = 4;
    private static final double HEIGHT = 0.6;
    private static final double WIDTH = 0.5;


    @SuppressWarnings("deprecation")
    public ControlOSZI() {
        super(DEF_TERM_NUMBER, 0);
        _inputTerminalNumber.setValueWithoutUndo(DEF_TERM_NUMBER);                        
        
        for (int i = 0; i < DEF_TERM_NUMBER; i++) {
            _scopeInputSignals.push(new ScopeSignalRegular(_scopeInputSignals.size(), this));
        }
        _grafer.getManager().setInputSignals(_scopeInputSignals);
        _meanSignals.setGrafer(_grafer);        
                
        _inputTerminalNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setInputTerminalNumber(_inputTerminalNumber.getValue());
            }
        });
        initScope();
    }



    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }

    /**
     * Initializes the scope by registering null data references, setting up the scope frame,
     * and invalidating any cached waveform/Fourier analysis.
     */
    public void initScope() {
        // // is called once during SCOPE initialization
        // // References for SCOPE are being registered...
        final DataContainerNullData nullData = new DataContainerNullData(_grafer.getManager().getAllScopeSignals());
        nullData.setDefinedMeanSignals(_meanSignals);        
        _zvDatenRAM = nullData;
        _scopeFrame._scope.setDataContainer(_zvDatenRAM);

        _scopeFrame.setReferenzAufControlBlock(this);
        _scopeFrame.setTitle(" " + getStringID());
        if (_waveformChar != null) {
            _waveformChar.setInvalid();
        }
        _fourier = null;
        _grafer.refreshComponentPane();
    }

    public AbstractDataContainer getZVDataInRAM() {
        return _zvDatenRAM;
    }

    public void setTerminalNodeLabel(final String newLabel, final int nodeIndex) {
        // TODO ??? _zvDatenRAM.setSignalName(nodeIndex, newLabel);
    }

    public void setDataContainerIndices(final int[] indices) {                
        final List<Integer> globalIndices = new ArrayList<Integer>();
        for (int index : indices) {
            globalIndices.add(index);
        }
        _scopeWrapperIndices = new ScopeWrapperIndices(globalIndices, NetlistControl.globalData);
    }

    /**
     * Handles mouse click detection on the SCOPE symbol and the red triangles
     * used to increase or decrease the number of input terminals.
     *
     * @param mouseX the x-coordinate of the mouse click
     * @param mouseY the y-coordinate of the mouse click
     * @return 1 if the symbol was clicked, 2 if a terminal triangle was clicked, 0 otherwise
     */
    @Override
    public int isClicked(final int mouseX, final int mouseY) {

        final boolean symbolClicked = xKlickMin <= mouseX && mouseX <= xKlickMax
                && yKlickMin <= mouseY && mouseY <= yKlickMax;
        final boolean upperTriClicked = _xKlickMinTerm <= mouseX && mouseX <= _xKlickMaxTerm
                && _yKlickMinTermSUB <= mouseY && mouseY <= _yKlickMaxTermSUB;
        final boolean lowerTriClicked = _xKlickMinTerm <= mouseX && mouseX <= _xKlickMaxTerm
                && _yKlickMinTermADD <= mouseY && mouseY <= _yKlickMaxTermADD;
        if (symbolClicked) {
            return 1;  // // SCOPE symbol has been clicked --> Dialog or editing mode
        } else {
            if (lowerTriClicked) {
                // // increase number of terminals by one and update SCOPE
                _inputTerminalNumber.setUserValue(_inputTerminalNumber.getValue() + 1);
                setInputTerminalNumber(_inputTerminalNumber.getValue());
                _grafer.setInitalCurveConnection(_inputTerminalNumber.getValue());
                return 2;
            } else if (upperTriClicked && _inputTerminalNumber.getValue() >= 2) {
                // // reduce number of terminals by one and update SCOPE
                _inputTerminalNumber.setUserValue(_inputTerminalNumber.getValue() - 1);
                setInputTerminalNumber(_inputTerminalNumber.getValue());
                return 2;
            }
            return 0;  // // SCOPE symbol was not clicked, therefore 'false'
        }
    }

    @Override
    public void deleteActionIndividual() {
        try {
            _scopeFrame.dispose();
            if (_zvDatenRAM instanceof DataContainerScopeWrapper) {
                ((DataContainerScopeWrapper) _zvDatenRAM).deregisterObserver();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.deleteActionIndividual();
    }

    /**
     * the scope has many, many settings, Therefore, we just use the export
     * function/save to file to create a copy of the scope.
     *
     * @return a copy object of this scope
     */
    @Override
    public AbstractBlockInterface copyFabric(final long shiftValue) {
        final ControlOSZI returnValue = new ControlOSZI();
        returnValue.getIdentifier().createNewIdentifier(this.getUniqueObjectIdentifier() + shiftValue);
        returnValue.setTyp(_controlTyp);
        final StringBuffer exportString = new StringBuffer();
        this.exportASCII(exportString);
        final TokenMap tokenMap = new TokenMap(exportString.toString().split("\n"), false);
        returnValue.setParentCircuitSheet(this.getParentCircuitSheet());
        returnValue.importASCII(tokenMap);
        return returnValue;
    }

    public String getSubCircuitPath() {
        String pathToComponent = "";
        CircuitSheet cs = getParentCircuitSheet();

        while (cs != null && cs instanceof SubCircuitSheet) {
            SubCircuitSheet subSheet = (SubCircuitSheet) cs;
            pathToComponent = subSheet._subBlock.getStringID() + "#" + pathToComponent;
            cs = subSheet._subBlock.getParentCircuitSheet();
        }
        return pathToComponent;
    }

    public class ControlOSZICalculator extends AbstractControlCalculatable implements NotCalculateableMarker, MemoryInitializable {

        public ControlOSZICalculator(final int noInputs) {
            super(noInputs, 0);
        }

        @Override
        public void calculateYOUT(final double deltaT) {
            assert false;
        }

        @Override
        public void doInit(double deltaT) {
            doInitialCalculation();
        }
    }

    @Override
    public void doOperationAfterNewConstruction() {
        if (_grafer._manager.getDiagrams().isEmpty()) {
            createInitialDiagram();
        }
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new ControlOSZICalculator(XIN.size());
    }

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {

        int x = getSheetPosition().x;
        int y = getSheetPosition().y;

        // Klickbereich SCOPE-Symbol:
        xKlickMin = (int) (dpix * (x - WIDTH));
        xKlickMax = (int) (dpix * (x + WIDTH));
        yKlickMin = (int) (dpix * (y - WIDTH));
        yKlickMax = (int) (dpix * (y - WIDTH + _inputTerminalNumber.getValue()));
        Color origColor = graphics.getColor();
        graphics.setColor(getBackgroundColor());

        graphics.fillRect((int) (dpix * (x - WIDTH)), (int) (dpix * (y - WIDTH)), (int) (dpix * (2 * WIDTH)), (int) (dpix * (1.0 * _inputTerminalNumber.getValue())));
        graphics.setColor(origColor);
        graphics.drawRect((int) (dpix * (x - WIDTH)), (int) (dpix * (y - WIDTH)), (int) (dpix * (2 * WIDTH)), (int) (dpix * (1.0 * _inputTerminalNumber.getValue())));

        graphics.drawRect((int) (dpix * (x - WIDTH)) + INSIDE_RECT, (int) (dpix * (y - WIDTH)) + 2 * INSIDE_RECT, (int) (dpix * (2 * WIDTH)) - 2 * INSIDE_RECT,
                (int) (dpix * (1.0 * _inputTerminalNumber.getValue())) - 4 * INSIDE_RECT);

        // // Red triangles to click --> Change the number of terminals:
        graphics.setColor(Color.red);


        final int[] triXCoords = new int[]{dpix * x, dpix * (x) + DIAMETER, dpix * (x) - DIAMETER};
        final int yp0 = (int) (dpix * (y - WIDTH - HEIGHT) - DELTA), yp1 = (int) (dpix * (y - WIDTH) - DELTA);
        final int ym1 = (int) (dpix * (y - WIDTH + _inputTerminalNumber.getValue()) + DELTA),
                ym0 = (int) (dpix * (y - WIDTH + _inputTerminalNumber.getValue() + HEIGHT) + DELTA);
        final int[] triYCoords = new int[]{(int) (dpix * (y - WIDTH - HEIGHT) - DELTA),
            (int) (dpix * (y - WIDTH) - DELTA), (int) (dpix * (y - WIDTH) - DELTA)};

        graphics.fillPolygon(triXCoords, triYCoords, triYCoords.length);
        graphics.fillPolygon(triXCoords, new int[]{ym0, ym1, ym1}, 3);
        // // Click area red triangles for terminal number change:
        _xKlickMinTerm = triXCoords[2];
        _xKlickMaxTerm = triXCoords[1];
        _yKlickMinTermSUB = yp0;  // // upper triangle --> SUB / reduction of the number of terminals
        _yKlickMaxTermSUB = yp1;
        _yKlickMinTermADD = ym1;  // // lower triangle --> ADD / increase the number of terminals
        _yKlickMaxTermADD = ym0;
        graphics.setColor(origColor);
    }

    @Override
    public void exportAsciiIndividual(final StringBuffer ascii) {
        // somewhere is a bug hidden in the save routine for the scope
        // this was probably the reason why the data file was corrupted
        // to 10 bytes. Here, I append the final scope String only
        // at the end, when everything went fine!
        final StringBuffer appendLater = new StringBuffer();
        try {
            _scopeSettings.exportASCII(appendLater);
            super.exportAsciiIndividual(appendLater);
            appendLater.append("\ntn");
            ProjectData.appendAsString(appendLater.append("\nisShowName"), _isShowName);

            _saveLoadSignalNames = new String[_zvDatenRAM.getRowLength()];
            for (int i = 0; i < _zvDatenRAM.getRowLength(); i++) {
                _saveLoadSignalNames[i] = _zvDatenRAM.getSignalName(i);
            }
            ProjectData.appendAsString(appendLater.append("\nsavedSignalNames"), _saveLoadSignalNames);

            _meanSignals.exportIndividualCONTROL(appendLater);
            appendLater.append("\n<ScopeSettings>\n");
            _grafer.exportIndividualCONTROL(appendLater);
            appendLater.append("\n<\\ScopeSettings>\n");

            appendLater.append("\n<ScopeWindowSettings>\n");
            _scopeFrame.exportIndividualCONTROL(appendLater);
            appendLater.append("\n<\\ScopeWindowSettings>\n");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        ascii.append(appendLater);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {

        _scopeInputSignals.clear();
        for (int i = 0; i < _inputTerminalNumber.getValue(); i++) {
            _scopeInputSignals.push(new ScopeSignalRegular(_scopeInputSignals.size(), this));
        }

        _grafer.getManager().setInputSignals(_scopeInputSignals);

        _meanSignals.importIndividualCONTROL(tokenMap);
        // JMC

        if (tokenMap.containsToken("savedSignalNames[]")) {
            _saveLoadSignalNames = tokenMap.readDataLine("savedSignalNames[]", _saveLoadSignalNames);
        } else { //otherwise we get a null pointer exception during loading old version of the file
            _saveLoadSignalNames = new String[0];
        }

        if (tokenMap.containsToken("isShowName")) {
            _isShowName = tokenMap.readDataLine("isShowName", _isShowName);
        }

        final TokenMap settingsMap = tokenMap.getBlockTokenMap("<ScopeSettings>");
        if (settingsMap != null) {
            _grafer.importIndividualCONTROL(settingsMap);
        }

        final TokenMap windowSettingsMap = tokenMap.getBlockTokenMap("<ScopeWindowSettings>");
        if (windowSettingsMap != null) {
            _scopeFrame.importIndividualCONTROL(windowSettingsMap);
        }

        final TokenMap scopeMap = tokenMap.getBlockTokenMap("<scopeSettings>");

        if (scopeMap != null) {
            importScopeSettings(scopeMap);
        }

    }

    //for use with GeckoSCRIPT - get waveform characteristics for a particular channel
    public double[] getChannelCharacteristics(final int channel,
            final double start, final double end) throws GeckoInvalidArgumentException {

        if (_waveformChar == null || !_waveformChar.isValid() || (start != _charStart) || (end != _charEnd)) {
            _waveformChar = CharacteristicsCalculator.calculateFabric(_zvDatenRAM, start, end);
            _charStart = start;
            _charEnd = end;
        }

        return _waveformChar.getChannelCharacteristics(channel);
    }

    //for use with GeckoSCRIPT - Fourier analysis
    public double[][] doFourierAnalysis(final int channel,
            final double start, final double end, final int harmonics) throws Exception {

        if ((_fourier == null) || (start != _fourStart) || (end != _fourEnd) || (harmonics > _fourier[0][0].length + 1)) {
            final FourierGUIless fourier = new FourierGUIless(_zvDatenRAM, start, end, harmonics);
            _fourier = fourier.doFourier();
            _fourStart = start;
            _fourEnd = end;
        }

        if (channel >= (XIN.size()) || (channel < 0)) {
            throw new Exception("Invalid scope port supplied for Fourier analysis!");
        }

        double[][] channelFourier = new double[FOUR_CHAN_DEPTH][harmonics + 1];

        // _fourier is a double[][][] of format [coefficient][channel][value for nth harmonic]
        // channelFourier is double[][] of format [coefficient][value for nth harmonic]

        for (int i = 0; i < FOUR_CHAN_DEPTH; i++) {
            for (int j = 0; j <= harmonics; j++) {
                channelFourier[i][j] = _fourier[i][channel][j];
            }
        }
        return channelFourier;
    }

    public void doInitialCalculation() {
        List<ExternalSignal> externalSignals;
        // // (b) when the simulation is restarted a second, third, etc. time
        if (_zvDatenRAM != null) {
            _zvDatenRAM.deleteObservers();
        }

        final DataContainerScopeWrapper scopeWrapper;
        scopeWrapper = new DataContainerScopeWrapper(NetlistControl.globalData,
                _scopeWrapperIndices,
                _meanSignals,                
                _grafer.getManager().getAllScopeSignals());
        

        if (_zvDatenRAM instanceof DataContainerScopeWrapper) {
            ((DataContainerScopeWrapper) _zvDatenRAM).deregisterObserver();
        }

        _zvDatenRAM = scopeWrapper;

        _scopeFrame.clearZVDaten();
        _scopeFrame._scope.setDataContainer(_zvDatenRAM);
        //------------
        // // every time a new SCOPE MainWindow 'runs', ZV data storage begins again
        if (_waveformChar != null) {
            _waveformChar.setInvalid();
        }
        _fourier = null;
    }

    public void setSimulationTimeBoundaries(final double tStart, final double tEnd) {
        _grafer.setSimulationTimeBoundaries(tStart, tEnd);
    }

    void importScopeSettings(final TokenMap scopeMap) {
        _scopeSettings.importASCII(scopeMap);
        _scopeSettings.loadSettings(_scopeFrame.getGrafer());  // // 'this' is parameterized here
    }

    public boolean isAntiAliasing() {
        return _grafer.isAntiAliasing();
    }

    public void setAntiAliasing(final boolean value) {
        _grafer.setAntiAliasing(value);
    }

    @Override
    public void setInputTerminalNumber(final int number) {
        while (XIN.size() > number) {
            XIN.pop();
        }

        while (XIN.size() < number) {
            XIN.add(new TerminalControlInput(this, TERM_POS_X, -XIN.size()));
        }

        try {
            if (_grafer != null) {
                _grafer.getManager().defineNewSignalNumber(this, XIN.size(), _meanSignals);
                _scopeFrame.setNewTerminalNumber(XIN.size());
            }

        } catch (Exception ex) {
        }


    }

    @Override
    public void setOutputTerminalNumber(final int number) {
        // scope does not have output terminals
    }

    void createInitialDiagram() {
        _grafer.createInitialDiagram();
    }

    @Override
    public boolean isNameVisible() {
        return _isShowName;
    }

    @Override
    public void setNameVisible(final boolean newValue) {
        _isShowName = newValue;
    }

    @Override
    protected final Window openDialogWindow() {
        _scopeFrame.setTitle(" " + getStringID());
        return _scopeFrame;
    }
}
