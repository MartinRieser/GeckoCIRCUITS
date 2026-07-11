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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.general.MainWindow;
import ch.technokrat.gecko.geckocircuits.general.GeckoFile.StorageType;
import ch.technokrat.gecko.geckocircuits.general.GeckoFileChooser;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Diode;
import ch.technokrat.gecko.geckocircuits.newscope.GeckoDialog;
import ch.technokrat.gecko.i18n.GuiFabric;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Modal editor dialog for detailed loss curve data of semiconductor components.
 * Provides tabs for conduction and switching loss curves, with support for
 * loading, editing, saving (internal/external), and creating test curves.
 */
public final class DialogLossesDetail extends GeckoDialog {

    private static final long serialVersionUID = 1L;
    private final transient LossCalculationDetailed _lossCalculation;
    private final JPanel _lowerPanel = new JPanel();
    private final JPanel _exitPanel = new JPanel();
    private final DetailedSwitchingLossesPanel _switchingLossPanel = new DetailedSwitchingLossesPanel();
    private final DetailedConductionLossPanel _conductionLossPanel = new DetailedConductionLossPanel();
    private final JButton _jButtonSaveChanged = GuiFabric.getJButton(I18nKeys.OVERWRITE_AND_SAVE_CHANGES);
    private final JButton _jButtonSaveNew = GuiFabric.getJButton(I18nKeys.SAVE_AS_NEW);
    private final JButton _jButtonCancel = GuiFabric.getJButton(I18nKeys.CANCEL);
    private final JPanelLossDataInterpolationSettings _jpTest = new JPanelLossDataInterpolationSettings(this);
    private final JTabbedPane _tabbedPane = new JTabbedPane();
    private StorageType _storageType = StorageType.INTERNAL;

    public static DialogLossesDetail fabricCreateNew(final LossCalculationDetailed lossCalculation, final Window parent) {
        DialogLossesDetail returnValue = new DialogLossesDetail(lossCalculation, parent);
        returnValue._jButtonSaveChanged.setEnabled(false);        
        return returnValue;
    }

    public static DialogLossesDetail fabricCreateExisting(final LossCalculationDetailed lossCalculation, final Window parent) {
        DialogLossesDetail returnValue = new DialogLossesDetail(lossCalculation, parent);
        returnValue._jButtonSaveChanged.setEnabled(true);
        return returnValue;
    }
    
    private DialogLossesDetail(final LossCalculationDetailed lossCalculation, final Window parent) {
        super(parent, true);
        getContentPane().setLayout(new BorderLayout());

        _tabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int tabIndex = _tabbedPane.getSelectedIndex();
                switch(tabIndex) {
                    case 0:
                        // disable voltage selection test curve field!
                        _jpTest.setVoltageSelectionVisible(false);
                        break;
                    case 1:
                        _jpTest.setVoltageSelectionVisible(true);
                        break;
                    default:
                        assert false;
                }
            }
        });
        // // existing loss details are loaded for editing
        _lossCalculation = lossCalculation;
        // // Read in curve data from the measurement curves:
        _switchingLossPanel._lossCurves.addAll(_lossCalculation.getCopyOfSwitchingLossMeasurementCurvesArray());        
        _conductionLossPanel._lossCurves.addAll(_lossCalculation.getCopyOfConductionLossMeasurementCurvesArray());        

        if (_lossCalculation.lossFile != null) {
            this.setTitle(" " + _lossCalculation.lossFile.getName());
        }

        add(_tabbedPane, BorderLayout.CENTER);
        _conductionLossPanel.buildGUI();
        _tabbedPane.addTab("Conduction Losses", _conductionLossPanel);

        _switchingLossPanel.buildGUI();
        _tabbedPane.addTab("Switching Losses", _switchingLossPanel);

        if(_lossCalculation._parent instanceof Diode) {
            _conductionLossPanel.useNonlinearInElectric((Diode) _lossCalculation._parent);
        }
        
        this.buildMainPanel();

        this.pack();
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);        
    }

    private final transient ActionListener _saveChangeListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {

            if (_lossCalculation.lossFile == null) {
                // no loss file has been defined yet!
                doSaveAsNew();
            } else {
                applyChanges(_lossCalculation.lossFile.getCurrentAbsolutePath());
            }
        }
    };
    private final transient ActionListener _saveNewActionListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent actionEvent) {
            doSaveAsNew();
        }
    };
    
    /**
     * Prompts the user for a file name (external or model-internal) and saves
     * the current loss curves as a new file.
     */
    private void doSaveAsNew() {
        String fileName = getNewFileNameDialog();
            if (fileName == null) {
                return;
            }

            applyChanges(fileName);
    }

    private void buildMainPanel() {
        _jButtonSaveChanged.addActionListener(_saveChangeListener);
        _jButtonSaveNew.addActionListener(_saveNewActionListener);
        _jButtonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                dispose();
            }
        });

        populateSaveExitPanel();
        populateLowerPanel();
    }

    /**
     * Creates test curves in both the conduction and switching loss panels
     * at the specified operating point.
     *
     * @param temperature the junction temperature for the test curve
     * @param voltage the blocking voltage for the test curve
     */
    public void createTestCurve(final double temperature, final double voltage) {
        _conductionLossPanel.createTestCurve(temperature, voltage);
        _switchingLossPanel.createTestCurve(temperature, voltage);
    }

    private void populateLowerPanel() {
        _lowerPanel.setLayout(new BorderLayout());
        _lowerPanel.add(_exitPanel, BorderLayout.WEST);
        _lowerPanel.add(_jpTest, BorderLayout.EAST);
        add(_lowerPanel, BorderLayout.SOUTH);
    }
    private static final int SAVE_PANEL_COMPONENTS = 3;

    private void populateSaveExitPanel() {
        _exitPanel.setLayout(new GridLayout(SAVE_PANEL_COMPONENTS, 1));
        _exitPanel.add(_jButtonSaveChanged);
        _exitPanel.add(_jButtonSaveNew);
        _exitPanel.add(_jButtonCancel);
    }

    /**
     * Applies the edited loss curve data to the loss calculation object and
     * persists it to the specified file (external or model-internal).
     *
     * @param fileName the file path or internal identifier to save to
     */
    private void applyChanges(final String fileName) {

        // // (1) Losses are reset:
        _lossCalculation.setzeNeueParameter(_switchingLossPanel._lossCurves, _conductionLossPanel._lossCurves);        
        // // (2) Corresponding new file is saved:
        //Making detailed loss file use the Gecko File Manager (Andrija):
        //NOTE - HERE I PASS "FALSE" BY DEFAULT ("FALSE" means losses are saved into .ipes file)
        //MODIFY GUI FOR DETAILED LOSSES TO GIVE USER A CHOICE
        //here pass 'false' for 'update' flag as this is a brand new file
        _lossCalculation.writeDetailedLossesToFile(fileName, _switchingLossPanel._lossCurves,
                _conductionLossPanel._lossCurves, _storageType);
        //System.out.println("new loss file OK: " + OK);
        // // (3) accept the new data:
        dispose();
    }

    /**
     * Shows a dialog prompting the user to choose between an external file or
     * a model-internal file for saving loss curve data.
     *
     * @return the selected file path or internal identifier, or null if cancelled
     */
    private String getNewFileNameDialog() {
        final StringBuffer fileName = new StringBuffer();
        // // Create array of data type Object, add options
        Object[] options = {"External File", "Model-intern File"};

        int selected = JOptionPane.showOptionDialog(null,
                "Do you like to create a real file on your harddisk or a \n"
                + "file that is internal to the simulation model?",
                "Select file type",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        switch (selected) {
            case -1: // window closed by cross-escape
                return null;
            case 0:
                _storageType = StorageType.EXTERNAL;
                GeckoFileChooser fileChooser = GeckoFileChooser.createSaveFileChooser(".scl", "Semiconductor Loss Files (*.scl)", this, null);
                if (fileChooser.getUserResult() == GeckoFileChooser.FileChooserResult.CANCEL) {
                    return null;
                }
                fileName.append(fileChooser.getFileWithCheckedEnding());
                break;
            case 1:
                _storageType = StorageType.INTERNAL;
                // // Call the static method showMessageDialog()
                fileName.append(JOptionPane.showInputDialog(null, "Please select a file name identifier:",
                        "Choose file name",
                        JOptionPane.PLAIN_MESSAGE));
                break;
            default:
                assert false;
        }
        return fileName.toString();
    }
}
