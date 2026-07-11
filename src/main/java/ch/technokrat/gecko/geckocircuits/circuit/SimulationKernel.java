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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractMotor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractVoltageSource;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCurrentSource;
import ch.technokrat.gecko.geckocircuits.general.DialogWarningNodeNumber;
import ch.technokrat.gecko.geckocircuits.general.MainWindow;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.ReluctanceInductor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SourceType;
import ch.technokrat.gecko.geckocircuits.control.*;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.datacontainer.CompressorIntMatrix;
import ch.technokrat.gecko.geckocircuits.datacontainer.IntegerMatrixCache;
import ch.technokrat.gecko.geckocircuits.datacontainer.ShortMatrixCache;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeFrame;
import java.util.ArrayList;
import java.util.List;

/**
 * Core time-stepping simulation engine that manages the coupled power
 * (LK), thermal (THERM), and control circuit simulations. Handles matrix
 * assembly, LU decomposition caching, switch actions, and data transfers
 * between domains at each time step.
 */
public class SimulationKernel {
    private static final int MAX_ITERATIONS = 10000;
    private static final double PERTURBATION_FACTOR = 0.99;
    private static final double PERTURBATION_INITIAL = 0.9999999;
    private static final double SWITCH_THRESHOLD = 0.5;

    /** Indicates a diode switching/recalculation error. */
    private boolean diodenSchaltfehler;
    
    /** The simulation time step (dt), current simulation time (t), and pause time. */
    private double dt, t, tPAUSE;
    /**
     * The start and end time of the simulation. Publicly mutable; the
     * simulation runs from tSTART to tEND.
     */
    public static double tSTART, tEND;
    
    /** Power circuit matrices. */
    private LKMatrices lkmLK;  
    /** Thermal circuit matrices. */
    private LKMatrices lkmTHERM;  
    /** Power circuit netlist. */
    private NetListLK nl;  
    /** Control circuit netlist. */
    private NetlistControl controlNL;  
    /** Thermal circuit netlist. */
    private NetListLK thermNL;  
    
    /** Flag to simulate power circuit (false if no power circuit components exist). */
    private boolean simuliereLeistungskreis;  
    /** Flag to simulate control circuit (false if no control components exist). */
    private boolean simuliereControlkreis;  
    /** Flag to simulate thermal circuit (false if no thermal components exist). */
    private boolean simuliereThermKreis;  
    
    /** System state variables from the previous step for power (LK) and thermal (THERM) circuits. */
    private double[] pLK_ALT, pTHERM_ALT;  
    
    /** Nodes of interest in the power circuit (for voltage plotting). */
    private int[] interessanteKnotenLK;   
    /** Nodes of interest in the thermal circuit (for temperature plotting). */
    private int[] interessanteKnotenTHERM;   
    
    /** Pointers/mappings from power circuit elements to control elements. */
    private int[] zeigerAufControlElement;  
    /** Pointers/mappings from thermal circuit elements to control elements. */
    private int[] zeigerAufControlElementTHERM;  
    /** Pointers/mappings for machine parameters from VIEWMOT to power circuit. */
    private int[][] zeiger_VIEWMOT_MaschineLK;  
    private int jjZeiger;
    
    /** Multiplier and adder for bounds expansion in lookup vectors. */
    private int mult = 20, add = 50;  
    
    /** Mapping: power circuit switch components controlled by SWITCH control blocks. */
    private int[][] zuordnung_SchalterLK_SWITCH;  
    /** Mapping: power circuit source components controlled by CONTROL signal sources. */
    private int[][] zuordnung_QuelleLK_signalCONTROL;  
    /** Mapping: thermal circuit source components controlled by CONTROL signal sources. */
    private int[][] zuordnung_QuelleTHERM_signalCONTROL;  
    /** Mapping: machine components defined by mechanical loads in CONTROL. */
    private int[][] zuordnung_MaschineLK_LoadParameterInCONTROL;  
    
    /** Control block components. */
    private ControlBlock[] c;
    /** Parameters/state values of the control blocks. */
    private double[][] controlParameters;
    /** The total count of control blocks. */
    private int controlANZAHL;
    //-------------------------------
    private AbstractCachedMatrix _lkCachedMatrix;
    LUDecompositionCache _luDecompCache;
    private AbstractCachedMatrix _thCachedMatrix;
    LUDecompositionCache _thLuDecompCache;
    private AbstractControlCalculatable[] sortedCalculators;
    private AbstractControlCalculatable[] unsortedCalculators;

    /**
     * Represents the current status of the simulation lifecycle.
     */
    public enum SimulationStatus {

        NOT_INIT,
        RUNNING,
        PAUSED,
        FINISHED
    };
    public SimulationStatus _simulationStatus = SimulationStatus.NOT_INIT;

    public SimulationKernel() {
        _thLuDecompCache = new LUDecompositionCache();
        _luDecompCache = new LUDecompositionCache();
    }

    public void pauseSimulation() {
    }

    public double getCurrentTime() {
        return t;
    }

    public double getTEND() {
        return tEND;
    }

    public double getTSTART() {
        return tSTART;
    }

    public double getdt() {
        return dt;
    }
    /** Global step counter, incremented on each time step. */
    static int counter = 0;

    private void simulateOneTimeStep() {
        counter++;
        if (simuliereLeistungskreis) {
            nl.updateNonlinearCapacitancesAndResistors();
            final boolean mindestensEineAktiveSchalthandlung = checkForSwitchAction();

            if (mindestensEineAktiveSchalthandlung) {
                lkmLK.schreibeMatrix_A(dt, t, false);
                _lkCachedMatrix = _luDecompCache.getCachedLUDecomposition(lkmLK.a, t);
            }

            lkmLK.schreibeMatrix_B(dt, t, false);
            lkmLK.p = _lkCachedMatrix.solve(lkmLK.bVector);            
            doDiodeErrorsRecalculations();
            nl.calculateSubCircuitAsDifferentialEquation(dt, t);  // interne Berechungen in SubCircuits diverser LK-Elemente
            lkmLK.aktualisiereKnotenpotentiale(dt, t);     // pALT=p;
            dataTransferLK_Control();
        }

        if (simuliereThermKreis) {
            lkmTHERM.schreibeMatrix_B(dt, t, false);
            // // Solving the matrix equations (power circuit):

            lkmTHERM.p = _thCachedMatrix.solve(lkmTHERM.bVector);
            lkmTHERM.calculateComponentCurrents(-1, dt, t, false, 0);  // // disturbance size '-1' only relevant for the diodes, values ​​do not matter here
            thermNL.calculateSubCircuitAsDifferentialEquation(dt, t);  // // possible analytical calculations in subcircuits of the THERM elements
            lkmTHERM.aktualisiereKnotenpotentiale(dt, t);  // pALT_THERM= pTHERM;
            dataTransferTherm();
        }

        //===================================
        // Control:
        //===================================        
        if (simuliereControlkreis) {
            controlNL.calculateTimeStep(dt, t);
        }

        //lkmLK.schreibeRechendatenNachEinemZeitschritt(t);
        //
        // // Pause is forced at a certain time -->
        if ((t - dt / 2 <= tPAUSE) && (tPAUSE <= t + dt / 2)) {
            _simulationStatus = SimulationStatus.PAUSED;
        }
    }

    private void doDiodeErrorsRecalculations() {
        int switchingErrorCounter = 0;   // // 'stoersize<1.0' prevents the algorithm from getting stuck when switching diodes between states
        double stoergroesse = 1;//PERTURBATION_INITIAL
        boolean isNewIteration = false;

        while (diodenSchaltfehler = lkmLK.calculateComponentCurrents(stoergroesse, dt, t, isNewIteration, switchingErrorCounter)) {
            isNewIteration = true;
            if (switchingErrorCounter > MAX_ITERATIONS) {
                //new DialogDiodenError(switchingErrorCounter, t);
                this.lastUpdateOfScope();  // // the final simulation will be updated again to be on the safe side
                throw new Error("Numerical instablity of switch!\nAborting simulation.");
            }

            //this.ausgebenDiodenzustaende(t);
            if ((switchingErrorCounter++) > 2) {
                stoergroesse *= PERTURBATION_FACTOR;
            }

            _lkCachedMatrix = _luDecompCache.getCachedLUDecomposition(lkmLK.a, t);
            lkmLK.p = _lkCachedMatrix.solve(lkmLK.bVector);
        }
    }

    private void setControlledSourcesFromControlValue() {
        // // Control of the signal-controlled LK sources using a signal from the control circuit:
        for (int i1 = 0; i1 < zuordnung_QuelleLK_signalCONTROL.length; i1++) {
            int controlIndex = zuordnung_QuelleLK_signalCONTROL[i1][1];
            int outputIndex = zuordnung_QuelleLK_signalCONTROL[i1][2];

            AbstractCircuitBlockInterface e = nl.elements[zuordnung_QuelleLK_signalCONTROL[i1][0]];
            double[] par = e.parameter;
            double[][] blockOutput = unsortedCalculators[controlIndex]._outputSignal;

            if (blockOutput != null) {  // // because yout was not yet defined at the first time step
                par[1] = blockOutput[outputIndex][0];  // // for signal-controlled sources, parameter[1] is the signal
            }
        }
    }

    private void setThermalControlledSourcesFromControlValues() {
        // // Control of the signal-controlled THERM sources using a signal from the control circuit:
        for (int i1 = 0; i1 < zuordnung_QuelleTHERM_signalCONTROL.length; i1++) {
            int controlIndex = zuordnung_QuelleTHERM_signalCONTROL[i1][1];
            int outputIndex = zuordnung_QuelleTHERM_signalCONTROL[i1][2];
            AbstractCircuitBlockInterface e = thermNL.elements[zuordnung_QuelleTHERM_signalCONTROL[i1][0]];
            double[] par = e.parameter;            
            double[][] blockOutput = unsortedCalculators[controlIndex]._outputSignal;
            if (blockOutput != null) {  // // because yout was not yet defined at the first time step
                par[1] = blockOutput[outputIndex][0];  // // for signal-controlled sources, parameter[1] is the signal
            }
        }
    }

    private void dataTransferTherm() {
        // Potentialdifferenzen im THERM-Kreis an Temperatur-Sensor TEMP uebergeben:
        for (int i1 = 0; i1 < interessanteKnotenTHERM.length; i1 += 2) {
            double potentialdifferenz = -1;
            if (interessanteKnotenTHERM[i1 + 1] == 0) {
                potentialdifferenz = lkmTHERM.p[interessanteKnotenTHERM[i1]] - 0;
            } else if (interessanteKnotenTHERM[i1] == 0) {
                potentialdifferenz = 0 - lkmTHERM.p[interessanteKnotenTHERM[i1 + 1]];
            } else if ((interessanteKnotenTHERM[i1 + 1] == 0) && (interessanteKnotenTHERM[i1] == 0)) {
                potentialdifferenz = 0 - 0;
            } else {
                potentialdifferenz = lkmTHERM.p[interessanteKnotenTHERM[i1]] - lkmTHERM.p[interessanteKnotenTHERM[i1 + 1]];
            }
            // // Set signal at TEMP:
            try {
                sortedCalculators[zeigerAufControlElementTHERM[i1 / 2]]._outputSignal[0][0] = potentialdifferenz;
            } catch (Exception ex) {
                System.err.println(controlParameters[zeigerAufControlElementTHERM[i1 / 2]].length + " " + zeigerAufControlElementTHERM[i1 / 2]);
                ex.printStackTrace();
            }

        }
        setThermalControlledSourcesFromControlValues();
    }

    private void dataTransferLK_Control() {

        setControlledSourcesFromControlValue();
        //------------------
        // // Control of the external machine values ​​(e.g. load torque) using a signal from the control circuit:
        for (int i1 = 0; i1 < zuordnung_MaschineLK_LoadParameterInCONTROL.length; i1++) {
            AbstractCircuitBlockInterface e = nl.elements[zuordnung_MaschineLK_LoadParameterInCONTROL[i1][0]];
            int controlIndex = zuordnung_MaschineLK_LoadParameterInCONTROL[i1][1];
            int outputIndex = zuordnung_MaschineLK_LoadParameterInCONTROL[i1][2];
            double[] par = e.parameter;
            double[][] blockOutput = unsortedCalculators[controlIndex]._outputSignal;
            par[((AbstractMotor) e).getIndexForLoadTorque()] = blockOutput[outputIndex][0];  // // for signal-controlled sources, parameter[1] is the signal
        }
        //-------------------------------------------------------------------
        // Potentialdifferenzen im LK-Kreis an Controlkreiselement VOLT uebergeben:
        for (int i1 = 0; i1 < interessanteKnotenLK.length; i1 += 2) {
            double potentialdifferenz = -1;
            if (interessanteKnotenLK[i1 + 1] == 0) {
                potentialdifferenz = (lkmLK.p[1 + interessanteKnotenLK[i1] - 1]);
            } else if (interessanteKnotenLK[i1] == 0) {
                potentialdifferenz = (0 - lkmLK.p[interessanteKnotenLK[i1 + 1]]);
            } else if ((interessanteKnotenLK[i1 + 1] == 0) && (interessanteKnotenLK[i1] == 0)) {
                potentialdifferenz = (0 - 0);
            } else {
                potentialdifferenz = (lkmLK.p[1 + interessanteKnotenLK[i1] - 1] - lkmLK.p[1 + interessanteKnotenLK[i1 + 1] - 1]);
            }

            // // Set signal at VOLT:
            // System.out.println("getting voltage: " + potentialdifferenz);
            sortedCalculators[zeigerAufControlElement[i1 / 2]]._outputSignal[0][0] = potentialdifferenz;

        }

        //------------------
        // // apply the internal machine parameter selected in VIEWMOT to the CONTROL output of C_VIEWMOT:
        for (int i1 = 0; i1 < zeiger_VIEWMOT_MaschineLK.length; i1++) {
            AbstractCircuitBlockInterface lkBlock = nl.elements[zeiger_VIEWMOT_MaschineLK[i1][1]];
            double interneMaschienenGroesse = lkBlock.parameter[zeiger_VIEWMOT_MaschineLK[i1][2]];
            sortedCalculators[zeiger_VIEWMOT_MaschineLK[i1][0]]._outputSignal[0][0] = interneMaschienenGroesse;
        }
    }

    private boolean checkForSwitchAction() {

        boolean mindestensEineAktiveSchalthandlung = false;

        for (int i1 = 0; i1 < zuordnung_SchalterLK_SWITCH.length; i1++) {
            double schaltSignal = sortedCalculators[zuordnung_SchalterLK_SWITCH[i1][0]]._inputSignal[0][0];
            AbstractCircuitBlockInterface e = nl.elements[zuordnung_SchalterLK_SWITCH[i1][1]];
            final double[] par = e.parameter;
            switch (e.getCircuitType()) {
                case LK_S:
                    // rDS(t) - rDS,on - rDS,off - i(t) - u(t) - uDSon[V] - k_on[Ws] - k_off[Ws]    -->
                    double rS_vorher = par[0];
                    if (schaltSignal > SWITCH_THRESHOLD) {
                        par[0] = par[1];  // // --> on, threshold '0.5' for switching
                    } else {
                        par[0] = par[2];  // --> off
                    }
                    double rS_nachher = par[0];
                    if (rS_vorher != rS_nachher) {
                        mindestensEineAktiveSchalthandlung = true;
                    }
                    break;
                case LK_MOSFET:
                    // rDS(t) - rDS,on - rDS,off - i(t) - u(t) - uDSon[V] - k_on[Ws] - k_off[Ws]    -->
                    rS_vorher = par[0];
                    if (schaltSignal > SWITCH_THRESHOLD) {
                        par[0] = par[2];  // // --> on, threshold '0.5' for switching
                    } else {
                        par[0] = par[3];  // --> off
                    }
                    rS_nachher = par[0];
                    if (rS_vorher != rS_nachher) {
                        mindestensEineAktiveSchalthandlung = true;
                    }
                    break;
                case LK_THYR:
                    // // THYR is treated like a diode (see below!!), the current gate state is stored here:
                    if (schaltSignal > SWITCH_THRESHOLD) {
                        par[8] = 1;
                    } else {
                        par[8] = 0;  // // --> on, threshold '0.5' for switching
                    }
                    break;
                case LK_IGBT:

                    // // IGBT is treated similarly to THYR, the current gate state is saved here:
                    // rD(t) - uf - rON - rOFF - i(t) - u(t) - xxx - xxx - gateStatusOnOff   --> aehnlich wie THYR
                    if (schaltSignal > SWITCH_THRESHOLD) {
                        par[8] = 1;
                    } else {
                        par[8] = 0;  // // --> on, threshold '0.5' for switching
                    }
                    break;
            }
        }
        return mindestensEineAktiveSchalthandlung;
    }

    public void runSimulation() {        
        while ((t <= tEND) && (_simulationStatus != SimulationStatus.PAUSED)) {
            simulateOneTimeStep();
            t += dt;
        }

        this.lastUpdateOfScope();

        // // Save the last solution in order to be able to continue correctly in the case of CONTINUE -->$
        pLK_ALT = new double[lkmLK.p.length];
        System.arraycopy(lkmLK.p, 0, pLK_ALT, 0, pLK_ALT.length);
        pTHERM_ALT = new double[lkmTHERM.p.length];
        System.arraycopy(lkmTHERM.p, 0, pTHERM_ALT, 0, pTHERM_ALT.length);
    }

    public void simulateOneStep() throws Exception {
        if (t + dt > tEND) {
            throw new Exception("Specified end of simulation reached! Cannot simulate another step.");
        }
        simulateOneTimeStep();
        t += dt;
    }

    public void simulateTime(double time) throws Exception {
        double simtime = t + time;
        boolean overReach = false;
        if (simtime > tEND) {
            simtime = tEND;
            overReach = true;
        }
        while (t <= simtime) {
            simulateOneTimeStep();
            t += dt;
        }

        if (overReach) {
            throw new Exception("Specified simulation time goes beyond specified simulated end time; simulated only up to end time");
        }
    }

    public void endSim() {
        _simulationStatus = SimulationStatus.FINISHED;
        this.lastUpdateOfScope();
        //controlNL.tearDownOnPause();
        //-------------------------------
        // // Save the last solution in order to be able to continue correctly in the case of CONTINUE -->$
        pLK_ALT = new double[lkmLK.p.length];
        System.arraycopy(lkmLK.p, 0, pLK_ALT, 0, pLK_ALT.length);
        pTHERM_ALT = new double[lkmTHERM.p.length];
        System.arraycopy(lkmTHERM.p, 0, pTHERM_ALT, 0, pTHERM_ALT.length);
        //-------------------------------
    }

    // // set the initial conditions according to the last calculation when CONTINUE was pressed -->
    public void setInitialConditionsFromContinue() {
        if ((lkmLK.p.length != pLK_ALT.length) || (lkmTHERM.p.length != pTHERM_ALT.length)) {
            //System.out.println("Warning: Node-Number has been changed!");
            return;
        }
        lkmLK.p = pLK_ALT;
        lkmTHERM.p = pTHERM_ALT;
    }

    public void setZeiten(double tSTART, double tEND, double dt) {
        SimulationKernel.tSTART = tSTART;
        SimulationKernel.tEND = tEND;
        this.dt = dt;
    }

    public void initSimulation(
            double dt, double tSTART, double tAktuell, double tEND, double tPAUSE,
            boolean getAnfangsbedVomDialogfenster,
            NetListContainer nlContainer, boolean recalculateMatrixFromDifferentDt) {
        _simulationStatus = SimulationStatus.RUNNING;

        this.dt = dt;
        SimulationKernel.tSTART = tSTART;
        SimulationKernel.tEND = tEND;
        this.tPAUSE = tPAUSE;
        this.t = tAktuell;
        //            
        this.controlNL = nlContainer._nlControl;
        controlNL.doMemorInits(dt);
        if (recalculateMatrixFromDifferentDt) {
            controlNL.doDtChangeInit(dt);
        }

        this.nl = nlContainer._nlLK;
        nl.updateNonlinearCapacitancesAndResistors();
        this.thermNL = nlContainer._nlTH;

        this.c = controlNL._orderedControlBlocks;
        sortedCalculators = controlNL.getSortedControlCalculators();
        unsortedCalculators = controlNL._allUnSortedControlCalculators;

        controlParameters = new double[c.length][];
        for (int i = 0; i < c.length; i++) {
            if (c[i] != null) {
                controlParameters[i] = c[i].parameter;
            }
        }

        this.controlANZAHL = c.length;

        //***************************
        // // Setting up the couplings between [ LK - CONTROL - THERM ] -->
        //
        definiereInteraktion_VOLT_AMP_LK();  // // how do I measure currents and voltages in the LK with CONTROL-AMP or CONTROL-VOLT?
        definiereInteraktion_MaschineLK_VIEWMOT();  // // for measuring internal machine parameters
        //
        zuordnung_SchalterLK_SWITCH = defineInteractionSwitchController();  // welches CONTROL-GATE steuert welchen LK_SWITCH (LK_S,LK_IGBT,LK_THYR) an?
        zuordnung_QuelleLK_signalCONTROL = definiereInteraktion_SignalgesteuerteQuelle_Control(nl);  // welches allg. CONTROL-Signal steuert welche LK_QUELLE (LK_U, LK_I) an?
        zuordnung_QuelleTHERM_signalCONTROL = definiereInteraktion_SignalgesteuerteQuelle_Control(thermNL);  // welches allg. CONTROL-Signal steuert welche THERM_QUELLE (LK_TEMP, LK_FLOW) an?

        zuordnung_MaschineLK_LoadParameterInCONTROL = definiereInteraktion_MaschineLK_LoadParameterInCONTROL();  // Welcher CONTROL-Knoten definiert welche mechanischen Signale (zB. externes Moment) des Motors?            
        definiereInteraktion_TEMP_FLOW_THERM();  // // how do I measure thermal flow and temperature differences in THERM with CONTROL-FLOW or CONTROL-TEMP?
        setControlledSourcesFromControlValue();
        setThermalControlledSourcesFromControlValues();

        if (recalculateMatrixFromDifferentDt) {
            lkmLK.schreibeMatrix_A(dt, tAktuell, false);
            lkmTHERM.schreibeMatrix_A(dt, tAktuell, false);
        }
        //
        // Leistungskreis:
        if (getAnfangsbedVomDialogfenster) {
            lkmLK = new LKMatrices(MainWindow.getSolverSettings().SOLVER_TYPE.getValue());
            lkmLK.initMatrizen(nl, getAnfangsbedVomDialogfenster, true, MainWindow.getSolverSettings().SOLVER_TYPE.getValue());  // pALT= new double[..];   iALT= new double[..];
            lkmLK.schreibeMatrix_A(dt, tAktuell, false);

            //
            // thermischer Kreis:
            lkmTHERM = new LKMatrices(MainWindow.getSolverSettings().SOLVER_TYPE.getValue());
            lkmTHERM.initMatrizen(thermNL, getAnfangsbedVomDialogfenster, false, MainWindow.getSolverSettings().SOLVER_TYPE.getValue());
            lkmTHERM.schreibeMatrix_A(dt, tAktuell, false);
        }
        //=============================
        if (lkmLK.matrixSize < 2) { // // if the power circuit does not exist, it is consequently not simulated:
            simuliereLeistungskreis = false;
        } else {
            simuliereLeistungskreis = true;
            _lkCachedMatrix = _luDecompCache.getCachedLUDecomposition(lkmLK.a, t);
        }

        if (lkmTHERM.matrixSize < 2) { // // if the thermal circuit does not exist, it is consequently not simulated:
            simuliereThermKreis = false;
        } else {
            simuliereThermKreis = true;
            _thCachedMatrix = _thLuDecompCache.getCachedLUDecomposition(lkmTHERM.a, t);
        }
        if (controlANZAHL < 1) {
            simuliereControlkreis = false;
        } else {
            simuliereControlkreis = true;
        }

        diodenSchaltfehler = false;
    }

    public void setScopeMenuesStartStop() {
        // ganz am Anfang sofort einmal auffrischen:
        for (int i1 = 0; i1 < c.length; i1++) {
            try {
                if (c[i1] instanceof ControlOSZI) {
                    ((ControlOSZI) c[i1])._scopeFrame.setScopeMenueEnabled(true);
                }
                if (c[i1] instanceof ControlCISPR16) {
                    ((ControlCISPR16) c[i1]).setTestReceiverCISPR16MenueEnabled(false);
                }
            } catch (NullPointerException e) {
            }
        }
    }

    public void initialisiereCONTROLatSimulationStart(final double dt) {
        controlNL.initializeAtSimulationStart(dt);
        ShortMatrixCache.clearCache();
        IntegerMatrixCache.clearCache();
        CompressorIntMatrix.clearCache();
    }

    private int[][] definiereInteraktion_SignalgesteuerteQuelle_Control(final NetListLK netlist) {
        //---------------------------------------
        // // Which signal-controlled source in the LK (VOLT, AMP, ...) is controlled by which CONTROL signal? -->
        //
        int[][] zuordnung_QuelleLK_signalCONTROL = new int[mult * controlANZAHL + add][];
        int counter = 0;
        AbstractCircuitBlockInterface[] allElements = netlist.elements; // careful: we DON't use elements including subcircuits. In this case,
        // the direct component value is defined somewhere else!
        for (int i = 0; i < allElements.length; i++) {
            AbstractCircuitBlockInterface element = allElements[i];
            if (element instanceof AbstractVoltageSource || element instanceof AbstractCurrentSource) {
                if (element.getParameter()[0] == SourceType.QUELLE_SIGNALGESTEUERT_NEW || element.getParameter()[0] == SourceType.QUELLE_SIGNALGESTEUERT) {
                    NetlistControl.IndexConnection li = controlNL.getIndexConnection(element.getParentCircuitSheet(), element.getParameterString()[0]);
                    if (li != null) { // this can happen when no control signal is selected/assigned!
                        zuordnung_QuelleLK_signalCONTROL[counter]
                                = new int[]{i, li._elementIndex, li._inBlockIndex_outputIndex};
                        counter++;
                    }
                }
            }
        }

        int[][] zuordTEMP = new int[counter][3];
        for (int i1 = 0; i1 < counter; i1++) {
            for (int i2 = 0; i2 < 3; i2++) {
                zuordTEMP[i1][i2] = zuordnung_QuelleLK_signalCONTROL[i1][i2];
            }
        }
        return zuordTEMP;
    }

    private int[][] defineInteractionSwitchController() {
        // // Which switch is controlled by which SWITCH control block? -->
        int[][] zuordnung_SchalterLK_SWITCH = new int[mult * controlANZAHL + add][];
        int zuordnungANZAHL_SchalterLK_SWITCH = 0;
        for (int iC = 0; iC < controlANZAHL; iC++) {
            if (controlNL._orderedControlBlocks[iC] instanceof ControlGate) {
                ControlGate controlGate = (ControlGate) controlNL._orderedControlBlocks[iC];
                AbstractBlockInterface controlledSwitch = controlGate.getComponentCoupling()._coupledElements[0];
                if (controlledSwitch != null) {
                    for (int iLK = 0; iLK < nl.elements.length; iLK++) {
                        AbstractCircuitBlockInterface compareElement = nl.elements[iLK];
                        if (controlledSwitch.equals(compareElement)) {
                            zuordnung_SchalterLK_SWITCH[zuordnungANZAHL_SchalterLK_SWITCH] = new int[]{iC, iLK};
                            zuordnungANZAHL_SchalterLK_SWITCH++;
                        }
                    }
                }
            }
        }
        int[][] zuordTEMP = new int[zuordnungANZAHL_SchalterLK_SWITCH][2];
        for (int i1 = 0; i1 < zuordnungANZAHL_SchalterLK_SWITCH; i1++) {
            for (int i2 = 0; i2 < 2; i2++) {
                zuordTEMP[i1][i2] = zuordnung_SchalterLK_SWITCH[i1][i2];
            }
        }
        return zuordTEMP;
    }

    private int[][] definiereInteraktion_MaschineLK_LoadParameterInCONTROL() {
        //---------------------------------------
        // // Which CONTROL signal defines the mechanical load on the machine? -->
        //
        int[][] zuordnung_MaschineLK_LoadParameterInCONTROL = new int[mult * controlANZAHL + add][];
        int zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL = 0;
        for (int iLK = 0; iLK < nl.elements.length; iLK++) {
            AbstractCircuitBlockInterface block = nl.elements[iLK];
            if (block instanceof AbstractMotor) {
                NetlistControl.IndexConnection li = controlNL.getIndexConnection(nl.elements[iLK].getParentCircuitSheet(), nl.elements[iLK].getParameterString()[0]);
                if (li != null) {
                    zuordnung_MaschineLK_LoadParameterInCONTROL[zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL] = new int[]{iLK, li._elementIndex, li._inBlockIndex_outputIndex};
                    zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL++;
                }
            }
        }
        int[][] zuordTEMP = new int[zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL][3];
        for (int i1 = 0; i1 < zuordnungANZAHL_MaschineLK_LoadParameterInCONTROL; i1++) {
            for (int i2 = 0; i2 < 3; i2++) {
                zuordTEMP[i1][i2] = zuordnung_MaschineLK_LoadParameterInCONTROL[i1][i2];
            }
        }
        //
        return zuordTEMP;
        //---------------------------------------
    }

    private void definiereInteraktion_MaschineLK_VIEWMOT() {
        //--------
        String iA = "", iA1 = "", iA2 = "";
        int[][] zeiger_VIEWMOT_MaschineLK_temp = new int[controlANZAHL][3];
        jjZeiger = 0;
        for (int i1 = 0; i1 < controlANZAHL; i1++) {
            if (c[i1] instanceof ControlVIEWMOT) {
                ControlVIEWMOT controlVIEWMOT = (ControlVIEWMOT) c[i1];
                AbstractBlockInterface selectedMotor = controlVIEWMOT.getComponentCoupling()._coupledElements[0];
                if (selectedMotor != null) {
                    iA = c[i1].getParameterString()[0];         // zB. "M-DC.4.omega"
                    iA1 = c[i1].getParameterString()[1];  // --> "M-DC.4"
                    iA2 = c[i1].getParameterString()[2];  // --> "omega"                
                    for (int i2 = 0; i2 < nl.elements.length; i2++) {
                        AbstractCircuitBlockInterface el = nl.elements[i2];
                        if (selectedMotor.equals(el)) {
                            List<String> parameterStringIntern = el.getParameterStringIntern();
                            for (int i3 = 0; i3 < parameterStringIntern.size(); i3++) {
                                if (iA2.equals(parameterStringIntern.get(i3))) {  // --> internen Motor-Parameter gefunden                                                                 
                                    zeiger_VIEWMOT_MaschineLK_temp[jjZeiger] = new int[]{i1, i2, i3};
                                    jjZeiger++;
                                }
                            }
                        }
                    }
                }
            }
        }
        zeiger_VIEWMOT_MaschineLK = new int[jjZeiger][3];
        System.arraycopy(zeiger_VIEWMOT_MaschineLK_temp, 0, zeiger_VIEWMOT_MaschineLK, 0, jjZeiger);
    }

    private void definiereInteraktion_VOLT_AMP_LK() {
        // // interesting node potentials (for storage or output):
        List<Integer> iKn = new ArrayList<Integer>();
        int[] zeigerACE = new int[c.length];

        jjZeiger = 0;
        for (int i1 = 0; i1 < controlANZAHL; i1++) {
            if (c[i1] instanceof ControlVOLT || c[i1] instanceof ControlMMF) {
                AbstractPotentialMeasurement controlVOLT = (AbstractPotentialMeasurement) c[i1];
                AbstractBlockInterface directComponent = controlVOLT.getComponentCoupling()._coupledElements[0];

                String uA = c[i1].getParameterString()[0];
                String uB = c[i1].getParameterString()[1];
                if ((!uA.isEmpty()) && (!uB.isEmpty())) {
                    zeigerACE[jjZeiger] = i1;
                    jjZeiger++;

                    int firstIndex = nl.findIndexFromLabelInSheet(uA, controlVOLT);
                    iKn.add(firstIndex);

                    int secondIndex = nl.findIndexFromLabelInSheet(uB, controlVOLT);
                    iKn.add(secondIndex);
                } else if (directComponent != null) {
                    if (controlVOLT instanceof ControlMMF
                            && (directComponent instanceof ReluctanceInductor)) {
                        directComponent = ((ReluctanceInductor) directComponent)._secondarySource;
                    }
                    zeigerACE[jjZeiger] = i1;
                    jjZeiger++;
                    int counter = 0;
                    boolean returnOK = false;
                    for (AbstractCircuitBlockInterface elem : nl.eLKneu) {
                        if (elem.equals(directComponent)) {
                            returnOK = true;
                            iKn.add(nl.knotenX[counter]);
                            iKn.add(nl.knotenY[counter]);
                        }
                        counter++;
                    }
                    if (!returnOK) {
                        throw new ArrayIndexOutOfBoundsException("\nMeasurement of component " + c[i1].getStringID() + "\nhas a missing reference!");
                    }

                }
            }
        }

        interessanteKnotenLK = new int[iKn.size()];
        for (int i = 0; i < iKn.size(); i++) {
            interessanteKnotenLK[i] = iKn.get(i);
        }
        zeigerAufControlElement = new int[jjZeiger];
        System.arraycopy(zeigerACE, 0, zeigerAufControlElement, 0, jjZeiger);
    }

    private void definiereInteraktion_TEMP_FLOW_THERM() {
        // // interesting node potentials (for storage or output):
        int[] iKn = new int[thermNL.getElementANZAHLinklusiveSubcircuit() * 2];
        int jj = 0;
        int[] zeigerACE = new int[c.length];
        jjZeiger = 0;
        for (int i1 = 0; i1 < controlANZAHL; i1++) {
            if (c[i1] instanceof ControlTEMP) {
                ControlTEMP controlTEMP = (ControlTEMP) c[i1];
                AbstractBlockInterface directComponent = controlTEMP.getComponentCoupling()._coupledElements[0];

                String uA = c[i1].getParameterString()[0];
                String uB = c[i1].getParameterString()[1];

                if ((!uA.isEmpty()) && (!uB.isEmpty())) {
                    zeigerACE[jjZeiger] = i1;
                    jjZeiger++;

                    int firstIndex = thermNL.findIndexFromLabelInSheet(uA, controlTEMP);
                    if (firstIndex >= 0) {
                        iKn[jj] = firstIndex;
                        jj++;
                    }

                    int secondIndex = thermNL.findIndexFromLabelInSheet(uB, controlTEMP);
                    if (secondIndex >= 0) {
                        iKn[jj] = secondIndex;
                        jj++;
                    }

                } else if (directComponent != null) {
                    zeigerACE[jjZeiger] = i1;
                    jjZeiger++;
                    int counter = 0;
                    boolean returnOK = false;

                    for (AbstractCircuitBlockInterface elem : thermNL.eLKneu) {
                        if (elem != null) {                            
                            if (directComponent.equalsPossibleSubComponent(elem)) {
                                returnOK = true;
                                iKn[jj] = thermNL.knotenX[counter];
                                jj++;
                                iKn[jj] = thermNL.knotenY[counter];
                                jj++;
                            }
                        }
                        counter++;
                    }
                    if (!returnOK) {
                        throw new ArrayIndexOutOfBoundsException("\nMeasurement of component " + c[i1].getStringID() + "\nhas a missing reference!");
                    }
                }
            }
        }
        interessanteKnotenTHERM = new int[jj];
        System.arraycopy(iKn, 0, interessanteKnotenTHERM, 0, jj);

        zeigerAufControlElementTHERM = new int[jjZeiger];
        System.arraycopy(zeigerACE, 0, zeigerAufControlElementTHERM, 0, jjZeiger);
    }

    private void lastUpdateOfScope() {
        this.lastUpdateOfScope(300);
    }

    private void lastUpdateOfScope(long sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
        }  // // so that there is no 'RacingCondition' with a possible ongoing update from takteFreshungScope()
        //---------------------------
        for (int i1 = 0; i1 < c.length; i1++) {
            try {
                if (c[i1] instanceof ControlOSZI) {
                    ScopeFrame sf = ((ControlOSZI) c[i1])._scopeFrame;
                    sf.setScopeMenueEnabled(false);
                }
                if (c[i1] instanceof ControlCISPR16) {
                    ((ControlCISPR16) c[i1]).setTestReceiverCISPR16MenueEnabled(true);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    // Externe SIMULINK-KOPPLUNG  -  Zugriff erfolgt ueber 'SimLE.java'
    //
    public void external_step(double time) {
        t = time;
        if (dt == 0) {
            dt = 1e-6;  // passiert beim Start?
        }
        simulateOneTimeStep();
    }


    public double getTimeStep() {
        return dt;
    }

    public void external_end() {
        _simulationStatus = SimulationStatus.FINISHED;
        this.lastUpdateOfScope();  // // the final simulation will be updated again to be on the safe side
        //-------------------------------
        // // Save the last solution in order to be able to continue correctly in the case of CONTINUE -->
        pLK_ALT = new double[lkmLK.p.length];
        System.arraycopy(lkmLK.p, 0, pLK_ALT, 0, pLK_ALT.length);
        pTHERM_ALT = new double[lkmTHERM.p.length];
        System.arraycopy(lkmTHERM.p, 0, pTHERM_ALT, 0, pTHERM_ALT.length);
        //-------------------------------
        //System.out.println("\nexternal ended");
    }

    public void tearDownOnPause() {
        if (_simulationStatus == SimulationStatus.FINISHED) {
            controlNL.tearDownOnPause();
        }
    }
}
