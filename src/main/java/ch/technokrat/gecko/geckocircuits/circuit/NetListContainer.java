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
import ch.technokrat.gecko.geckocircuits.control.NetlistControl;

/**
 * Container aggregating three netlists: control, power circuit (LK), and thermal (TH).
 */
public class NetListContainer {    

    /** Control-domain netlist. */
    public final NetlistControl _nlControl;
    /** Power circuit (LK) netlist. */
    public final NetListLK _nlLK;
    /** Thermal (TH) netlist. */
    public final NetListLK _nlTH;
    
    /**
     * Factory method for creating a NetListContainer at simulation start.
     *
     * @param schematicEntry the schematic editor containing the circuit
     * @return a new NetListContainer initialized for simulation
     */
    public static NetListContainer fabricStartSimulation(final SchematicEditor2 schematicEntry) {        
        schematicEntry.checkNameOptParameters();               
        NetlistGeneral nlC1 = NetlistGeneral.fabricNetzlistDisabledParentSubsRemoved(schematicEntry.getConnection(ConnectorType.CONTROL), schematicEntry.getElementCONTROL());
        NetListLK nlL = schematicEntry.getNetlist(ConnectorType.LK_AND_RELUCTANCE);
        
        NetListLK nlT = schematicEntry.getNetlist(ConnectorType.THERMAL);
        NetlistControl nlC = NetlistControl.FabricRunSimulation(nlC1);
        
        for(AbstractCircuitSheetComponent elem : schematicEntry._circuitSheet.getAllElements()) {
            if(elem instanceof AbstractCircuitBlockInterface) {
                AbstractCircuitBlockInterface comp = (AbstractCircuitBlockInterface) elem;
                comp._currentInAmps = 0;
                comp._voltage = 0;
            }
            if(elem instanceof AbstractCircuitBlockInterface) {
                ((AbstractCircuitBlockInterface) elem).setzeParameterZustandswerteAufNULL();
            }
            
        }
        
        return new NetListContainer(nlC, nlL, nlT);
    }
    
    /**
     * Factory method for continuing a simulation, reusing the previous LK and TH netlists.
     *
     * @param schematicEntry the schematic editor
     * @param oldNetlist     the previous NetListContainer to carry forward
     * @return a new NetListContainer for continued simulation
     */
    public static NetListContainer fabricContinueSimulation(final SchematicEditor2 schematicEntry,
            NetListContainer oldNetlist) {
        schematicEntry.checkNameOptParameters();               
        return new NetListContainer(NetlistControl.FabricContinueSimulation(oldNetlist._nlControl), oldNetlist._nlLK, oldNetlist._nlTH);
    }
    
    
    
    /**
     * Factory method for creating a NetListContainer for GUI updates.
     *
     * @param circuitNL the power circuit netlist
     * @param thermNL   the thermal netlist
     * @param nlCONTROL the control netlist
     * @return a new NetListContainer
     */
    public static NetListContainer fabricGuiUpdate(NetListLK circuitNL, NetListLK thermNL, NetlistControl nlCONTROL) {
        return new NetListContainer(nlCONTROL, circuitNL, thermNL);
    }
    
    private NetListContainer(NetlistControl nlc, NetListLK nlk, NetListLK nlTH) {
        _nlControl = nlc;
        _nlLK = nlk;
        _nlTH = nlTH;
        // use the fabric methods!
    }
    
}
