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

import ch.technokrat.gecko.geckocircuits.general.GeckoFile;
import java.util.List;

/**
 * Interface for circuit components that support external file attachments
 * (e.g., Java block source files, loss model files).
 * @author andy
 */
public interface GeckoFileable {
    /** Initializes the list of extra files for this component. */
    void initExtraFiles();
    /** Adds new files to this component's file list. */
    void addFiles(final List<GeckoFile> newFilesToAdd);
    /** Returns the list of files attached to this component. */
    List<GeckoFile> getFiles();        
    /** Removes the specified files from this component. */
    void removeLocalComponentFiles(final List<GeckoFile> filesToRemove);

}
