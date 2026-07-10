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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitType;
import ch.technokrat.gecko.geckocircuits.general.AbstractComponentType;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCapacitor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractInductor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.InductorCoupable;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.MutualInductance;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SourceType;
import ch.technokrat.gecko.geckocircuits.control.AbstractPotentialMeasurement;
import java.util.*;

public class NetListLK {

    public int knotenMAX, spgQuelleMAX;  // // nodeMAX ... total number of nodes minus 'Ground';  spgSourceMAX ... Sum of the SpgSources plus LKOP2 elements
    public CircuitType[] typ;
    public int[] knotenX, knotenY;
    public int[][] nodePairDirVoltContSrc;
    public double[][] parameter;  // // component value; Type 'sinus', 'triangle', ... for current/voltage source; init values ​​iL_ALT and uC_ALT; ...
    public int[] spgQuelleNr;  // // not only counts the voltage sources starting from 1, but also the LKOP2 elements, where mixing with the voltage sources is possible
    protected int[][] gemeinsameKnoten;  // // Element nodes plus all connections that have the same potential
    protected int gesamtzahlKnotenNr;
    public Connection[] v;
    public int verbindungANZAHL;
    public AbstractCircuitBlockInterface[] elements, eLKneu, eLK_M;
    public int elementANZAHL, elementANZAHLneu;
    public String[] labelListe;
    //
    public PotentialArea[] potLab;
    public double t;  // // Current time in the simulation
    // // to describe magnetic couplings -->
    private AbstractCircuitBlockInterface[] alleGekoppeltenLc;  // // a listing of all the different coupled Lc elements
    private AbstractCircuitBlockInterface[][] partnerLc;  // // The coupling partner Lc is assigned to each listed Lc element
    private double[][] kopplungen;    // // these are the associated coupling values
    private PostCalculatable[] _postCalculatables = new PostCalculatable[0];
    public int[] _singularityEntries = new int[0];

    
    public double getSimulationTime() {
        return t;
    }
    

    public int[][] getGemeinsameKnoten() {
        return gemeinsameKnoten;
    }

    public int getGesamtzahlKnotenNr() {
        return gesamtzahlKnotenNr;
    }

    public Connection[] getConnectionen() {
        return v;
    }

    public int getConnectionANZAHL() {
        return verbindungANZAHL;
    }

    public int getElementANZAHL() {
        return elementANZAHL;
    }

    public int getElementANZAHLinklusiveSubcircuit() {
        return elementANZAHLneu;
    }  // // Couplings M are not counted!

    // // the number of an ElementLK in the netlist is often not identical to the ID number,
    // // e.g. if coupling k is installed and then additional ElementLK are added -->
    // // but for the coupling of inductances you need the number in the current net list for assignment -->
    private int getNetlistnNummer(AbstractCircuitBlockInterface search) {
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            if (search.equals(eLKneu[i1])) {
                return i1;
            }
        }
        System.out.println("Fehler qer^08gj03qhg4");
        return -1;
    }

    // // all couplings M that are evaluated in LK matrices in the algorithm -->
    public double[][][] getAlleKopplungenM() {
        double[][] zuLKOP2gehoerigeM_spgQnr = new double[elementANZAHLneu][];
        double[][] zuLKOP2gehoerigeM_kWerte = new double[elementANZAHLneu][];
        this.definiere_magnetischeKopplungen_im_LK();  // // what are the coupling partners (and coupling values) of the individual inductances? -->
        //--------------------
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            if (eLKneu[i1] instanceof InductorCoupable) {
                for (int i2 = 0; i2 < alleGekoppeltenLc.length; i2++) {
                    if (alleGekoppeltenLc[i2] == eLKneu[i1]) {
                        int anzahlKoppelPartner = partnerLc[i2].length;
                        double[] temp_spgQnr = new double[anzahlKoppelPartner];
                        double[] temp_kWerte = new double[anzahlKoppelPartner];
                        for (int i3 = 0; i3 < anzahlKoppelPartner; i3++) {
                            int koppelPartnerID = this.getNetlistnNummer(partnerLc[i2][i3]);
                            for (int i4 = 0; i4 < elementANZAHLneu; i4++) {
                                if (koppelPartnerID == this.getNetlistnNummer(eLKneu[i4])) {
                                    temp_spgQnr[i3] = spgQuelleNr[i4];
                                }
                            }
                            temp_kWerte[i3] = kopplungen[i2][i3];
                        }
                        zuLKOP2gehoerigeM_spgQnr[i1] = temp_spgQnr;
                        zuLKOP2gehoerigeM_kWerte[i1] = temp_kWerte;
                    }
                }
            }
        }

        return new double[][][]{zuLKOP2gehoerigeM_spgQnr, zuLKOP2gehoerigeM_kWerte};
    }

    /**
     * this fabric exists, since the constructor name makes it difficult to
     * distinguish between the full netlist and the netlist without subcircuits
     *
     * @param v
     * @param e
     * @return
     */
    public static NetListLK fabricIncludingSubcircuits(final Set<Connection> v, List<? extends AbstractBlockInterface> e) {                        
        NetlistGeneral nl = NetlistGeneral.fabricNetzlistDisabledParentSubsRemoved(v, e);
        nl.deSingularizeIsolatedPotentials();
        return new NetListLK(nl, true);
    }

    public static NetListLK fabricExcludingSubcircuits(final Collection<Connection> v, List<? extends AbstractBlockInterface> e) {
        return new NetListLK(NetlistGeneral.fabricNetzlistLabelUpdate(v, e), false);
    }

    private NetListLK(NetlistGeneral nlLK, boolean includeSubCircuits) {
        _singularityEntries = nlLK._singularityIndices;
                
        this.v = nlLK._connections.toArray(new Connection[0]);
        this.verbindungANZAHL = v.length;
        this.potLab = nlLK.getPotentiale();

        List<AbstractBlockInterface> elements = new ArrayList<AbstractBlockInterface>(nlLK.getElemente());

        this.elements = new AbstractCircuitBlockInterface[elements.size()];
        List<PostCalculatable> tmpPostCalculatables = new ArrayList<PostCalculatable>();
        for (int i = 0; i < this.elements.length; i++) {
            this.elements[i] = (AbstractCircuitBlockInterface) elements.get(i);
            if (this.elements[i] instanceof PostCalculatable) {
                tmpPostCalculatables.add((PostCalculatable) this.elements[i]);
            }
        }        
                
        this.elementANZAHL = this.elements.length;
        this.elementANZAHLneu = this.elementANZAHL;  // // will be corrected below in the case of possible subcircuits
        if (includeSubCircuits) {
            this.initialisiereMitSubcircuit();
            this.defineNodePairDirVoltContSrc();
        }
        
        this._postCalculatables = tmpPostCalculatables.toArray(new PostCalculatable[tmpPostCalculatables.size()]);
        for (PostCalculatable calc : _postCalculatables) {
            calc.doInitialization();
        }
        
        labelListe = new String[potLab.length];
        for (int i1 = 0; i1 < potLab.length; i1++) {
            String label = potLab[i1].getLabel();
            labelListe[i1] = label;
        } 
        
        

    }

    /**
     * this version is only for the initalization/ internal replacement of
     * capacitors with voltage sources.
     */
    private NetListLK(CircuitType[] typ, int[] knotenX, int[] knotenY, double[][] parameter, int[] spgQuelleNr) {
        // // Requirement 1: Nodes are numbered continuously from zero
        //------------------------------
        this.typ = typ;
        this.knotenX = knotenX;
        this.knotenY = knotenY;
        this.parameter = parameter;
        
        this.spgQuelleNr = spgQuelleNr;
        this.elementANZAHL = typ.length;
        this.elementANZAHLneu = this.elementANZAHL;
        //------------------------------
        // // Requirement 2: Voltage source numbers are numbered continuously and in ascending order starting from one
        // -->
        knotenMAX = 0;  // // Number of (different) nodes
        spgQuelleMAX = 0;  // // Number of (different) voltage sources
        for (int i1 = 0; i1 < elementANZAHL; i1++) {
            if (knotenX[i1] > knotenMAX) {
                knotenMAX = knotenX[i1];
            }
            if (knotenY[i1] > knotenMAX) {
                knotenMAX = knotenY[i1];
            }
            if (spgQuelleNr[i1] > spgQuelleMAX) {
                spgQuelleMAX = spgQuelleNr[i1];
            }
        }
    }

    // in case of direct-voltage-control of sources the nodes of the element, where the voltage is measured,
    // have to be found in the following methode;
    // this is employed in LKMatrizen() to set up matrix A >>
    private void defineNodePairDirVoltContSrc() {
        nodePairDirVoltContSrc = new int[elementANZAHLneu][2];
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            AbstractComponentType circuitTyp = eLKneu[i1].getTypeEnum();
            if (circuitTyp == CircuitType.LK_U || circuitTyp == CircuitType.LK_I || circuitTyp == CircuitType.REL_MMF) {
                ComponentCoupable compCoupable = (ComponentCoupable) eLKneu[i1];
                ComponentCoupling coupling = compCoupable.getComponentCoupling();
                for (int i2 = 0; i2 < elementANZAHLneu; i2++) {
                    if (eLKneu[i2] == coupling._coupledElements[0]) {
                        nodePairDirVoltContSrc[i1][0] = knotenX[i2];
                        nodePairDirVoltContSrc[i1][1] = knotenY[i2];
                    }
                }
            }
        }
    }

    public boolean updateNonlinearCapacitancesAndResistors() {
        boolean returnValue = false;
        for (AbstractCircuitBlockInterface elem : eLKneu) {
            if (elem instanceof AbstractCapacitor) {
                if (((AbstractCapacitor) elem).updateNonlinearCapacitances()) {
                    returnValue = true;
                }
            }
        }
        return returnValue;
    }

    // // at each time step in the simulation loop in 'SimulationKernel' analytical components of the SubCircuit are calculated,
    // // (i.e. not as a netlist to reduce computing effort)
    public void calculateSubCircuitAsDifferentialEquation(double dt, double t) {
        this.t = t;
        for (PostCalculatable calc : _postCalculatables) {                        
            calc.doCalculation(dt, t);
        }
    }

    // // e.g. the elementsLK ​​defined in the SubCircuit are integrated into the LK netlist -->
    public final void integriereSubCircuits() {
        Set<AbstractBlockInterface> eLKneuSet = new LinkedHashSet<AbstractBlockInterface>();
        ArrayList<AbstractCircuitBlockInterface> eLK_M_vec = new ArrayList<AbstractCircuitBlockInterface>();

        for (AbstractCircuitBlockInterface elem : elements) {
            if (elem instanceof HiddenSubCircuitable) {
                // // Element is broken down into its individual LK elements -->
                
                HiddenSubCircuitable subCircuitable = (HiddenSubCircuitable) elem;             
                if (subCircuitable.includeParentInSimulation()) {
                    eLKneuSet.add(elem);
                }
                eLKneuSet.addAll(subCircuitable.getHiddenSubCircuitElements());                
                
            } else {
                if (elem instanceof MutualInductance) {
                    eLK_M_vec.add(elem);
                } else {
                    eLKneuSet.add(elem);
                }
            }
        }

        elementANZAHLneu = eLKneuSet.size();
        eLKneu = eLKneuSet.toArray(new AbstractCircuitBlockInterface[eLKneuSet.size()]);
        eLK_M = eLK_M_vec.toArray(new AbstractCircuitBlockInterface[eLK_M_vec.size()]);
    }

    protected void initialisiereMitSubcircuit() {
        this.integriereSubCircuits();
        Set<Connection> connections = new LinkedHashSet<Connection>();
        for (Connection verb : v) {
            connections.add(verb);
        }

        List<AbstractBlockInterface> eLKneuList = new ArrayList<AbstractBlockInterface>();
        for (int i = 0; i < eLKneu.length; i++) {
            if (eLKneu[i] != null) {
                eLKneuList.add(eLKneu[i]);
            }
        }
        NetlistGeneral netList = NetlistGeneral.fabricNetzlistComplete(connections, eLKneuList);
        netList.deSingularizeIsolatedPotentials();
        _singularityEntries = netList._singularityIndices;                
        
        this.potLab = netList.getPotentiale();
        
        //***********************************************************
        // LK-Knotenliste -->
        typ = new CircuitType[elementANZAHLneu];
        knotenX = new int[elementANZAHLneu];
        knotenY = new int[elementANZAHLneu];
        parameter = new double[elementANZAHLneu][];
        spgQuelleNr = new int[elementANZAHLneu];
        int spgQuelleZaehler = 1;
        //***********************************************************
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            AbstractCircuitBlockInterface elem = eLKneu[i1];
            //------------------
            // allgemein:
            CircuitType circuitTyp = (CircuitType) elem.getTypeEnum();
            typ[i1] = circuitTyp;
            parameter[i1] = elem.getParameter();
            spgQuelleNr[i1] = (circuitTyp == CircuitType.REL_MMF || circuitTyp == CircuitType.LK_U
                    || circuitTyp == CircuitType.LK_LKOP2) || circuitTyp == CircuitType.TH_TEMP ? (spgQuelleZaehler++) : -1;
            //------------------
            // Anfangsknoten:            
            List<AbstractTerminal> startTerminals = elem.XIN;
            int[] nrAnfangsKn = new int[startTerminals.size()];  // // this array must be filled with node numbers
            for (int i2 = 0; i2 < startTerminals.size(); i2++) {
                for (int i3 = 0; i3 < potLab.length; i3++) {
                    if (potLab[i3].isTerminalOnPotential(startTerminals.get(i2))) {
                        nrAnfangsKn[i2] = i3;
                    }
                }
            }

            // Endknoten:
            List<AbstractTerminal> endTerminals = elem.YOUT;
            int[] nrEndKn = new int[endTerminals.size()];  // // this array must be filled with node numbers
            for (int i2 = 0; i2 < endTerminals.size(); i2++) {
                for (int i3 = 0; i3 < potLab.length; i3++) {
                    if (potLab[i3].isTerminalOnPotential(endTerminals.get(i2))) {
                        nrEndKn[i2] = i3;
                    }
                }
            }

            // // Assignment of the node numbers for the LK net list (there is only one start and end node for the LK elements):
            
            try {
                knotenX[i1] = nrAnfangsKn[0];
                knotenY[i1] = nrEndKn[0];
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }                
        
//        System.out.println("yyyyyyyyyyyyyyyyyyyyyyy repair!!!");
//        int[] saveKnotenX = new int[]{5, 2, 3, 4, 0, 5, 1};
//        int[] saveKnotenY = new int[]{6, 3, 2, 6, 1, 4, 0};
//        
//        knotenX = saveKnotenX;
//        knotenY = saveKnotenY;
        //------------------
        // // Requirement 1: Nodes are numbered continuously from zero
        // // Requirement 2: Voltage source numbers are numbered continuously and in ascending order starting from one
        // -->
        knotenMAX = 0;  // // Number of (different) nodes
        spgQuelleMAX = 0;  // // Number of (different) voltage sources
        for (int i1 = 0; i1 < elementANZAHLneu; i1++) {
            knotenMAX = Math.max(knotenMAX, knotenX[i1]);
            knotenMAX = Math.max(knotenMAX, knotenY[i1]);

            if (spgQuelleNr[i1] > spgQuelleMAX) {
                spgQuelleMAX = spgQuelleNr[i1];
            }
        }
    }

    // // the netlist used here must not contain any subcircuits; these must already be integrated into the netlistLK
    public static NetListLK ersetze_C_durch_Udc_Fuer_init(NetListLK nl) {
        // // for the new netlist:
        CircuitType[] i_typ = new CircuitType[nl.typ.length];
        int[] i_knotenX = new int[nl.knotenX.length];
        int[] i_knotenY = new int[nl.knotenY.length];
        double[][] i_parameter = new double[nl.parameter.length][];
        int[] i_spgQuelleNr = new int[nl.spgQuelleNr.length];
        int spgQuelleZaehler = 0;
        //--------------
        // // existing voltage sources are registered, the voltage source counter then runs from there for the C components -->
        for (int i1 = 0; i1 < nl.typ.length; i1++) {
            if (nl.spgQuelleNr[i1] > spgQuelleZaehler) {
                spgQuelleZaehler = nl.spgQuelleNr[i1];
            }
        }
        spgQuelleZaehler++;
                
        //--------------
        for (int i1 = 0; i1 < nl.typ.length; i1++) {
            switch (nl.typ[i1]) {
                case LK_C:
                    // // is replaced by DC voltage source for initialization -->
                    // // but only if the initial condition uC(0) was set to be non-zero
                    if (nl.parameter[i1][1] != 0) {
                        i_typ[i1] = CircuitType.LK_U;
                        i_knotenX[i1] = nl.knotenX[i1];
                        i_knotenY[i1] = nl.knotenY[i1];
                        i_parameter[i1] = new double[]{SourceType.QUELLE_DC_NEW, nl.parameter[i1][1], -1, -1, -1, -1, 0, 0, 0, 0};   // TypQuelle - uNmax - frequ - offset - phase - tastverh.
                        i_spgQuelleNr[i1] = spgQuelleZaehler;
                        spgQuelleZaehler++;
                    } else {
                        // bleibt unveraendert -->
                        i_typ[i1] = nl.typ[i1];
                        i_knotenX[i1] = nl.knotenX[i1];
                        i_knotenY[i1] = nl.knotenY[i1];
                        i_parameter[i1] = new double[nl.parameter[i1].length];
                        for (int i2 = 0; i2 < nl.parameter[i1].length; i2++) {
                            i_parameter[i1][i2] = nl.parameter[i1][i2];
                        }
                        i_spgQuelleNr[i1] = nl.spgQuelleNr[i1];
                    }
                    break;
                case TH_CTH:
                    // // is replaced by DC voltage source for initialization -->
                    // // but only if the initial condition uC(0) was set to be non-zero
                    if (nl.parameter[i1][1] != 0) {
                        i_typ[i1] = CircuitType.TH_TEMP;
                        i_knotenX[i1] = nl.knotenX[i1];
                        i_knotenY[i1] = nl.knotenY[i1];
                        i_parameter[i1] = new double[]{SourceType.QUELLE_DC_NEW, nl.parameter[i1][1], -1, -1, -1, -1, 0, 0, 0, 0};   // TypQuelle - uNmax - frequ - offset - phase - tastverh.
                        i_spgQuelleNr[i1] = spgQuelleZaehler;
                        spgQuelleZaehler++;
                    } else {
                        // bleibt unveraendert -->
                        i_typ[i1] = nl.typ[i1];
                        i_knotenX[i1] = nl.knotenX[i1];
                        i_knotenY[i1] = nl.knotenY[i1];
                        i_parameter[i1] = new double[nl.parameter[i1].length];
                        for (int i2 = 0; i2 < nl.parameter[i1].length; i2++) {
                            i_parameter[i1][i2] = nl.parameter[i1][i2];
                        }
                        i_spgQuelleNr[i1] = nl.spgQuelleNr[i1];
                    }
                    break;
                default:
                    // bleibt unveraendert -->
                    i_typ[i1] = nl.typ[i1];
                    i_knotenX[i1] = nl.knotenX[i1];
                    i_knotenY[i1] = nl.knotenY[i1];
                    i_parameter[i1] = new double[nl.parameter[i1].length];
                    for (int i2 = 0; i2 < nl.parameter[i1].length; i2++) {
                        i_parameter[i1][i2] = nl.parameter[i1][i2];
                    }
                    i_spgQuelleNr[i1] = nl.spgQuelleNr[i1];
                    break;
            }
        }
        //--------------
        NetListLK nl_C_ersetzt = new NetListLK(i_typ, i_knotenX, i_knotenY, i_parameter, i_spgQuelleNr);
        nl_C_ersetzt._singularityEntries = nl._singularityEntries;
        
        nl_C_ersetzt.elements = nl.elements;
        nl_C_ersetzt.eLKneu = nl.eLKneu;
        nl_C_ersetzt.nodePairDirVoltContSrc = nl.nodePairDirVoltContSrc;
        //nl_C_ersetzt.ausgebenTest();
        //
        return nl_C_ersetzt;
    }

    private void definiere_magnetischeKopplungen_im_LK() {

        // M -->   []{ k - xL1(Koord.) - yL1(Koord.) - xL2(Koord.) - yL2(Koord.) - ID-Nr_L1 - ID-Nr_L2 {
        List<AbstractMap.SimpleEntry<AbstractCircuitBlockInterface, AbstractCircuitBlockInterface>> kLc = new ArrayList<AbstractMap.SimpleEntry<AbstractCircuitBlockInterface, AbstractCircuitBlockInterface>>();
        List<Double> kM = new ArrayList<Double>();

        // (1) Einlesen aller gekoppelten LK-Lc-Paare -->
        for (AbstractCircuitBlockInterface search : elements) {
            if (search instanceof MutualInductance) {
                double[] parM = search.getParameter();
                
                for (int i2 = 0; i2 < this.getElementANZAHL(); i2++) {
                    if (((ComponentCoupable) search).getComponentCoupling()._coupledElements[0].equals(elements[i2])) {
                        parM[5] = i2;
                    }
                    if (((ComponentCoupable) search).getComponentCoupling()._coupledElements[1].equals(elements[i2])) {
                        parM[6] = i2;
                    }
                }

                AbstractInductor inductor1 = (AbstractInductor) elements[(int) parM[5]];
                AbstractInductor inductor2 = (AbstractInductor) elements[(int) parM[6]];
                if (inductor1 != null && inductor2 != null) {
                    double kValue = parM[0];
                    double M = kValue * Math.sqrt(inductor1.getStartInductance() * inductor2.getStartInductance());
                    kM.add(M);
                    kLc.add(new AbstractMap.SimpleEntry<>(inductor1, inductor2));
                } //else one or two couplings are not defined
            }
        }

        //-----------------------------
        // // (3) Processing: Which coupled LK-Lc elements does the respective LK-Lc element see?  -->
        // // (a) first a listing of all the different coupled Lc elements -->

        Set<AbstractCircuitBlockInterface> alleGekoppeltenLcSet = new LinkedHashSet<AbstractCircuitBlockInterface>();
        for (AbstractMap.SimpleEntry<AbstractCircuitBlockInterface, AbstractCircuitBlockInterface> pair : kLc) {
            alleGekoppeltenLcSet.add(pair.getKey());
            alleGekoppeltenLcSet.add(pair.getValue());
        }
        alleGekoppeltenLc = alleGekoppeltenLcSet.toArray(new AbstractCircuitBlockInterface[0]);
        //
        // // (b) each listed Lc element is assigned the coupling partner Lc -->
        partnerLc = new AbstractCircuitBlockInterface[alleGekoppeltenLc.length][alleGekoppeltenLc.length];
        kopplungen = new double[alleGekoppeltenLc.length][alleGekoppeltenLc.length];
        int ix = 0;
        for (int i1 = 0; i1 < alleGekoppeltenLc.length; i1++) {
            ix = 0;
            AbstractCircuitBlockInterface[] partnerTemp = new AbstractCircuitBlockInterface[alleGekoppeltenLc.length];
            double[] kpTemp = new double[alleGekoppeltenLc.length];
            for (AbstractMap.SimpleEntry<AbstractCircuitBlockInterface, AbstractCircuitBlockInterface> pair : kLc) {
                if (alleGekoppeltenLc[i1].equals(pair.getKey())) {
                    partnerTemp[ix] = pair.getValue();
                    kpTemp[ix] = kM.get(kLc.indexOf(pair));
                    ix++;

                }
                if (alleGekoppeltenLc[i1].equals(pair.getValue())) {
                    partnerTemp[ix] = pair.getKey();
                    kpTemp[ix] = kM.get(kLc.indexOf(pair));
                    ix++;
                }
            }

            AbstractCircuitBlockInterface[] partner = new AbstractCircuitBlockInterface[ix];
            for (int i2 = 0; i2 < ix; i2++) {
                partner[i2] = partnerTemp[i2];
            }
            partnerLc[i1] = partner;
            double[] kp = new double[ix];
            System.arraycopy(kpTemp, 0, kp, 0, kp.length);
            kopplungen[i1] = kp;
        }
    }

    public int findIndexFromLabelInSheet(final String searchLabel, final AbstractPotentialMeasurement measurement) {
        final CircuitSheet parentCircuitSheet = measurement.getParentCircuitSheet();

        for (int i = 0; i < eLKneu.length; i++) {
            AbstractCircuitBlockInterface block = eLKneu[i];

            if (block.getParentCircuitSheet() != parentCircuitSheet) {
                continue;
            }

            for (AbstractTerminal termIn : block.XIN) {
                if (termIn.getLabelObject().getLabelString().equals(searchLabel)) {
                    if (block._isEnabled.getValue() == Enabled.DISABLED) {
                        continue;
                    }
                    return knotenX[i];
                }
            }

            for (AbstractTerminal termOut : block.YOUT) {
                if (termOut.getLabelObject().getLabelString().equals(searchLabel)) {
                    if (block._isEnabled.getValue() == Enabled.DISABLED) {
                        continue;
                    }
                    return knotenY[i];
                }
            }
        }


        throw new RuntimeException("Error in measurement component " + measurement.getStringID()
                + "\nThe label reference \"" + searchLabel + "\" references to a disabled component.\n"
                + "Please disable the measurement component " + measurement.getStringID() + " to run\n"
                + "the simulation. Aborting.");

    }
}
