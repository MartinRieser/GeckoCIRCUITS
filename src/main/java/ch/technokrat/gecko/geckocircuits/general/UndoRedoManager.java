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

import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Coordinates undo and redo operations using ring buffers of serialized state snapshots.
 * Manages a configurable number of undo states with circular buffer overflow handling.
 */
public class UndoRedoManager {

    //------------------------------------------------------
    private int undoZustaendeMAX = 20;
    private byte[][] undoZustaende, redoZustaende;
    private int zeigerAufUndoZustaende, zeigerAufRedoZustaende;
    private boolean undoRingspeicherErstmalsUeberschritten;
    private int undoAnzahl, redoAnzahl;  // // as many undo and redo actions are possible
    //------------------------------------------------------

    @SuppressWarnings("this-escape")
    public UndoRedoManager() {
        this.init();
    }

    // // New File, Open File etc. -->
    public void init() {
        undoZustaende = new byte[undoZustaendeMAX][];
        redoZustaende = new byte[undoZustaendeMAX][];
        zeigerAufUndoZustaende = 0;
        zeigerAufRedoZustaende = 0;
        undoRingspeicherErstmalsUeberschritten = false;
        undoAnzahl = 0;
        redoAnzahl = 0;
    }

    public void speichereAutomatischAktuellenZustandFuerUndoRedo(Object daten) {
        zeigerAufRedoZustaende = 0;  // // ie. Redo not possible (redo only possible after undo)
        redoAnzahl = 0;
        //--------------
        // // ATTENTION: The following area is temporarily commented out so that the undo/redo buttons are deactivated
        // // the complete undo/redo mechanism needs to be revised because the storage of the complete
        // // Conditions always lead to massive problems (especially RAM memory) because the RAM memory is connected to the OSZI ControlBlock
        // // is linked
        // // --> DO NOT DELETE lower area!!!
        /*try {
        ByteArrayOutputStream outByteArray= new ByteArrayOutputStream();
        ObjectOutputStream out= new ObjectOutputStream(new DeflaterOutputStream(outByteArray));
        out.writeObject(data); 
        out.flush();
        out.close();
        byte[] state= outByteArray.toByteArray();
        //---------
        if (undoNumber<undoStatesMAX-1) undoNumber++; 
        undoStates[pointerToUndoStates]= state; 
        pointerToUndoStates++; 
        if (pointerToUndoStates==undoStatesMAX) {
        undoRingmemoryFirstExceeded= true; 
        pointerToUndoStates= 0; 
        }
        //---------
        } catch (Exception e) { 
        // This is where you end up when the SCOPE is open and you add something new because SCOPE is a swing element 
        System.out.println(e+" e0finv'");  
        this.init();  
        }*/
        //System.out.println("zeigerAufUndoZustaende= "+zeigerAufUndoZustaende+"\t\tzeigerAufRedoZustaende= "+zeigerAufRedoZustaende); 
        //--------------
    }

    public Object undo() {
        undoAnzahl--;
        zeigerAufUndoZustaende--;
        int zeiger = zeigerAufUndoZustaende - 1;
        if ((undoRingspeicherErstmalsUeberschritten) && (zeigerAufUndoZustaende == -1)) {
            zeigerAufUndoZustaende = undoZustaendeMAX - 1;
            zeiger = zeigerAufUndoZustaende - 1;
        } else if ((undoRingspeicherErstmalsUeberschritten) && (zeigerAufUndoZustaende == 0)) {
            zeiger = undoZustaendeMAX - 1;
        }
        byte[] zustand = undoZustaende[zeiger];
        Object daten = null;
        //---------
        try {
            ByteArrayInputStream inByteArray = new ByteArrayInputStream(zustand);
            ObjectInputStream in = new ObjectInputStream(new InflaterInputStream(inByteArray));
            daten = in.readObject();
            in.close();
        } catch (Exception e) {
            System.out.println(e + "   e0oiv00'er");
        }
        //---------
        //System.out.println("zeigerAufUndoZustaende= "+zeigerAufUndoZustaende+"\t\tzeigerAufRedoZustaende= "+zeigerAufRedoZustaende); 
        if (redoAnzahl < undoZustaendeMAX - 1) {
            redoAnzahl++;
        }
        redoZustaende[zeigerAufRedoZustaende] = undoZustaende[zeigerAufUndoZustaende];
        zeigerAufRedoZustaende++;
        //---------
        return daten;
    }

    public Object redo() {
        redoAnzahl--;
        zeigerAufRedoZustaende--;
        byte[] zustand = redoZustaende[zeigerAufRedoZustaende];
        Object daten = null;
        //---------
        try {
            ByteArrayInputStream inByteArray = new ByteArrayInputStream(zustand);
            ObjectInputStream in = new ObjectInputStream(new InflaterInputStream(inByteArray));
            daten = in.readObject();
            in.close();
        } catch (Exception e) {
            System.out.println(e + "   e0oiv00'er");
        }
        //---------
        if (undoAnzahl < undoZustaendeMAX - 1) {
            undoAnzahl++;
        }
        undoZustaende[zeigerAufUndoZustaende] = redoZustaende[zeigerAufRedoZustaende];
        zeigerAufUndoZustaende++;
        if (zeigerAufUndoZustaende == undoZustaendeMAX) {
            zeigerAufUndoZustaende = 0;
        }
        //---------
        return daten;
    }

    public boolean undoMoeglich() {
        if ((!undoRingspeicherErstmalsUeberschritten) && (undoAnzahl <= 1)) {
            return false;
        }
        if (undoAnzahl > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean redoMoeglich() {
        if (redoAnzahl > 0) {
            return true;
        } else {
            return false;
        }
    }
}
