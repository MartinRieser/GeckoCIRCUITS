/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 */
package gecko.geckocircuits.general;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for UndoRedoManager.
 * Validates undo/redo stacks, ring buffer limits, and serialization error handling.
 */
public class UndoRedoManagerTest {

    @Test
    public void testInitialState() {
        UndoRedoManager manager = new UndoRedoManager();
        assertFalse("Undo should not be possible initially", manager.undoMoeglich());
        assertFalse("Redo should not be possible initially", manager.redoMoeglich());
    }

    @Test
    public void testSaveAndUndo() {
        UndoRedoManager manager = new UndoRedoManager();
        String state1 = "State 1";
        String state2 = "State 2";

        manager.speichereAutomatischAktuellenZustandFuerUndoRedo(state1);
        // Initially, only 1 state: undo needs at least 2 states (current + previous) to undo back to previous
        assertFalse("Undo needs at least 2 states to go back", manager.undoMoeglich());

        manager.speichereAutomatischAktuellenZustandFuerUndoRedo(state2);
        assertTrue("Undo should be possible with 2 states saved", manager.undoMoeglich());

        Object undone = manager.undo();
        assertEquals("Undo should restore State 1", state1, undone);
        assertFalse("Undo should no longer be possible", manager.undoMoeglich());
        assertTrue("Redo should be possible after Undo", manager.redoMoeglich());
    }

    @Test
    public void testUndoAndRedo() {
        UndoRedoManager manager = new UndoRedoManager();
        String state1 = "State 1";
        String state2 = "State 2";
        String state3 = "State 3";

        manager.speichereAutomatischAktuellenZustandFuerUndoRedo(state1);
        manager.speichereAutomatischAktuellenZustandFuerUndoRedo(state2);
        manager.speichereAutomatischAktuellenZustandFuerUndoRedo(state3);

        assertTrue(manager.undoMoeglich());
        assertEquals("First undo should restore State 2", state2, manager.undo());
        
        assertTrue(manager.undoMoeglich());
        assertEquals("Second undo should restore State 1", state1, manager.undo());
        assertFalse(manager.undoMoeglich());

        assertTrue(manager.redoMoeglich());
        assertEquals("First redo should restore State 2", state2, manager.redo());

        assertTrue(manager.redoMoeglich());
        assertEquals("Second redo should restore State 3", state3, manager.redo());
        assertFalse(manager.redoMoeglich());
    }

    @Test
    public void testRingBufferOverrun() {
        UndoRedoManager manager = new UndoRedoManager();
        
        // Save more than the max (20 states)
        for (int i = 0; i < 30; i++) {
            manager.speichereAutomatischAktuellenZustandFuerUndoRedo("State " + i);
        }

        assertTrue("Undo should be possible after overrun", manager.undoMoeglich());
        
        // We should be able to undo up to 19 times (the buffer size - 1)
        int undoCount = 0;
        while (manager.undoMoeglich()) {
            Object undone = manager.undo();
            assertNotNull(undone);
            undoCount++;
        }
        
        assertTrue("Should support at least 18 undos in the ring buffer", undoCount >= 18);
    }

    @Test
    public void testSaveNonSerializable() {
        UndoRedoManager manager = new UndoRedoManager();
        manager.speichereAutomatischAktuellenZustandFuerUndoRedo("Valid State");
        
        // Attempting to save a non-serializable object (like a Thread)
        // should log an error internally and re-initialize gracefully without throwing
        manager.speichereAutomatischAktuellenZustandFuerUndoRedo(new Thread());
        
        assertFalse("Manager should be re-initialized and empty", manager.undoMoeglich());
    }
}
