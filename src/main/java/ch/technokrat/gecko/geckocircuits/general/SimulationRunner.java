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
package ch.technokrat.gecko.geckocircuits.general;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.NetListContainer;
import ch.technokrat.gecko.geckocircuits.circuit.SchematicEditor2;
import ch.technokrat.gecko.geckocircuits.circuit.SimulationKernel;
import ch.technokrat.gecko.geckocircuits.circuit.SimulationKernel.SimulationStatus;
import ch.technokrat.gecko.geckocircuits.circuit.SolverSettings;
import ch.technokrat.gecko.geckocircuits.control.DataSaver;
import ch.technokrat.gecko.geckocircuits.control.NetlistControl;
import ch.technokrat.gecko.geckocircuits.control.ControlOSZI;
import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckoscript.SimulationAccess;


/**
 * Manages the full simulation lifecycle: initialization, start, continue,
 * pause, and stop. Delegates time-stepping to {@link SimulationKernel} and
 * runs the simulation in a separate thread or synchronously.
 */
public final class SimulationRunner {

    final MainWindow _mainwindow;
    final SchematicEditor2 _se;
    public SimulationKernel simKern;
    private NetListContainer nlContainer;

    public SimulationRunner(final MainWindow mainwindow, final SchematicEditor2 schematicEntry) {
        _mainwindow = mainwindow;
        _se = schematicEntry;
    }

    /**
     * Initializes and starts a new simulation.
     * @param createNewSimThread if true, runs simulation in a new thread
     * @param solverSettings the solver configuration to use
     * @throws Exception if simulation initialization fails
     */
    public void startCalculation(boolean createNewSimThread, SolverSettings solverSettings) throws Exception {
        boolean getAnfangsbedVomDialogfenster = true;
        _mainwindow.setMenuDuringSimulation(true, false);

        simKern = new SimulationKernel();
        double tSTART = 0, tAktuell = tSTART;
        double tEND = solverSettings._tDURATION.getValue();
        double dtLoc = solverSettings.dt.getValue();

        if (solverSettings._T_pre.getValue() > 0) {
            solverSettings.inPreCalculationMode = true;
        }

        if (solverSettings.inPreCalculationMode) {
            tEND = solverSettings._T_pre.getValue();
            dtLoc = solverSettings._dt_pre.getValue();
        }

        nlContainer = NetListContainer.fabricStartSimulation(_se);
        

        simKern.initSimulation(
                dtLoc, tSTART, tAktuell, tEND, solverSettings._tPAUSE.getValue(),
                getAnfangsbedVomDialogfenster, nlContainer, false);
        simKern.setScopeMenuesStartStop();
        solverSettings._dt_ALT = dtLoc;
        simKern.initialisiereCONTROLatSimulationStart(dtLoc);  // // not done when 'Continue' is enabled

        RunThreadRun _runThread = new RunThreadRun();

        if (createNewSimThread) {
            final Thread calc = new Thread(_runThread);
            calc.setName("simulationThread");
            calc.start();
        } else {
            _runThread.setRunWithoutThread();
            _runThread.run();
        }
    }
    
        

    /**
     * Continues a paused or finished simulation from its current state.
     * @param createNewSimThread if true, runs continuation in a new thread
     * @param solverSettings the solver configuration to use
     * @throws Exception if continuation initialization fails
     */
    void continueCalculation(final boolean createNewSimThread, final SolverSettings solverSettings) throws Exception {
        boolean getAnfangsbedVomDialogfenster = false;
        _mainwindow.setMenuDuringSimulation(true, false);

        double tAktuell = simKern.getCurrentTime();  // // we are currently simulating
        double tSTART = simKern.getTSTART();
        double tEND = simKern.getTEND();

        if (tAktuell >= tEND) {  // // next simulation window will be opened
            tSTART = tEND;
            tEND += solverSettings._tDURATION.getValue();
            simKern.setZeiten(tSTART, tEND, solverSettings.dt.getValue());
            boolean recalculateMatrix = false;
            if (solverSettings.dt.getValue() != solverSettings._dt_ALT) {
                recalculateMatrix = true;
            }
            solverSettings._dt_ALT = solverSettings.dt.getValue();

            nlContainer = NetListContainer.fabricContinueSimulation(_se, nlContainer);

            simKern.initSimulation(solverSettings.dt.getValue(), tSTART, tAktuell, tEND,
                    solverSettings._tPAUSE.getValue(),
                    getAnfangsbedVomDialogfenster, nlContainer, recalculateMatrix);
            simKern.setScopeMenuesStartStop();
            simKern.setInitialConditionsFromContinue();  // // Fill LK_Matrix with the last calculated values

        } else { // Pause-Continue
            simKern._simulationStatus = SimulationStatus.RUNNING;
            simKern.setScopeMenuesStartStop();
            NetlistControl.globalData.setContainerStatus(ContainerStatus.RUNNING);
        }

        Thread calc = new Thread(new RunThreadRun());
        if (createNewSimThread) {
            calc.start();
        } else {
            calc.run();
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
     * Pauses an actively running simulation by setting the kernel status
     * to {@link SimulationStatus#PAUSED}.
     */
    void pauseSimulation() {
        try {
            if (simKern != null) {
                simKern._simulationStatus = SimulationStatus.PAUSED;
            } else {
                return;
            }

            _mainwindow.setMenuDuringSimulation(false, true);
            NetlistControl.globalData.setContainerStatus(ContainerStatus.PAUSED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initSim(double dtLoc, double tEND) {
        
        // Ganz am Anfang: t0=0
        _mainwindow.setMenuDuringSimulation(true, false);
        //
        boolean getAnfangsbedVomDialogfenster = true;
        //---------------------

        simKern = new SimulationKernel();
        double tSTART = 0, tAktuell = tSTART;
        MainWindow.getSolverSettings()._tDURATION.setValueWithoutUndo(tEND);
        MainWindow.getSolverSettings().dt.setValueWithoutUndo(dtLoc);
        //double tEND = tDURATION;
        //double dtLoc = dt;
 
        if (MainWindow.getSolverSettings().inPreCalculationMode) {
            tEND = MainWindow.getSolverSettings()._T_pre.getValue();
            dtLoc = MainWindow.getSolverSettings()._dt_pre.getValue();
        }
 
        nlContainer = NetListContainer.fabricStartSimulation(_se);
 
        simKern.initSimulation(
                dtLoc, tSTART, tAktuell, tEND, MainWindow.getSolverSettings()._tPAUSE.getValue(),
                getAnfangsbedVomDialogfenster, nlContainer, false);
        MainWindow.getSolverSettings()._dt_ALT = dtLoc;
        simKern.initialisiereCONTROLatSimulationStart(dtLoc);  // // not done when 'Continue' is enabled
        _mainwindow.jtfStatus.setText("Starting Simulation ... ");

        for (AbstractBlockInterface block : _se.getElementCONTROL()) {
            if (block instanceof ControlOSZI) {
                ((ControlOSZI) block).setSimulationTimeBoundaries(SimulationKernel.tSTART, SimulationKernel.tEND);
            }
        }

    }

    //for initializing simulation to be controlled step by step from GeckoSCRIPT
    public void initSim() {
        this.initSim(MainWindow.getSolverSettings().dt.getValue(), MainWindow.getSolverSettings()._tDURATION.getValue());
    }
    

    private class RunThreadRun implements Runnable {

        long q1;
        long q2;
        private boolean _runWithoutThread = false;

        public void setRunWithoutThread() {
            _runWithoutThread = true;
        }

        public void run() {
            try {
                for (AbstractBlockInterface block : _se.getElementCONTROL()) {
                    if (block instanceof ControlOSZI) {
                        ((ControlOSZI) block).setSimulationTimeBoundaries(SimulationKernel.tSTART, SimulationKernel.tEND);
                    }
                }

                q1 = System.currentTimeMillis();
                q2 = 0;
                _mainwindow.jtfStatus.setText("Starting Simulation ... ");

                
                try {
                    simKern.runSimulation();

                    if (MainWindow.isBranded()) {
                        _mainwindow.mItemNew.setEnabled(false);
                        _mainwindow.mItemOpen.setEnabled(false);
                    } else {
                        _mainwindow.mItemNew.setEnabled(true);
                        _mainwindow.mItemOpen.setEnabled(true);
                    }
                    _mainwindow.mItemExit.setEnabled(true);
                    _mainwindow.mItemSave.setEnabled(true);
                    _mainwindow.mItemSaveAs.setEnabled(true);
                    _mainwindow.mItemSaveView.setEnabled(true);



                } catch (java.lang.OutOfMemoryError err) {
                    throw new OutOfMemoryError("Could not allocate enough java RAM memory for the simulation!");
                } finally {
                    if (!MainWindow.getSolverSettings().inPreCalculationMode) {
                        endRun();
                    } else {
                        MainWindow.getSolverSettings().inPreCalculationMode = false;
                        try {
                            _mainwindow.continueCalculation(false);
                        } catch (Throwable error) {                            
                            error.printStackTrace();
                            throw new RuntimeException(error);
                        }
                    }
                }
            } catch (Throwable error) {
                GeckoSim._win.pauseSimulation();
                GeckoSim._win.getSimulationRunner().simKern._simulationStatus = SimulationKernel.SimulationStatus.FINISHED;
                GeckoSim._win.jtfStatus.setText("Simulation aborted.");
                if (!_runWithoutThread) {
                    error.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            error.getMessage(),
                            "Severe error!",
                            JOptionPane.ERROR_MESSAGE);
                } else {                    
                    throw new RuntimeException(error);
                }
            }
        }

        public void endRun() {
            q2 = System.currentTimeMillis();
            _mainwindow.pauseSimulation();
            simKern._simulationStatus = SimulationStatus.FINISHED;
            simKern.tearDownOnPause();
            _mainwindow.jtfStatus.setComputeTimeStatus(q2 - q1);
            waitForDataSavers();
            _mainwindow.setMenuDuringSimulation(false, true);
        }
    }

    //========================================================================
    // SIMULINK-KOPPLUNG  -  Zugriff erfolgt ueber 'GeckoSim.java'
    //
    public void external_init(double tEnd) {
        _mainwindow.jtfStatus.setText("Starting Simulation ... ");
        boolean getAnfangsbedVomDialogfenster = true;  // // Collect AB from Simulink!
        simKern = new SimulationKernel();
        double tSTART = 0, tAktuell = tSTART;
        nlContainer = NetListContainer.fabricStartSimulation(_se);
        simKern.initSimulation(
                MainWindow.getSolverSettings().dt.getValue(), tSTART, tAktuell, tEnd, MainWindow.getSolverSettings()._tPAUSE.getValue(),
                getAnfangsbedVomDialogfenster, nlContainer, false);
        simKern.initialisiereCONTROLatSimulationStart(MainWindow.getSolverSettings().dt.getValue());
    }
}
