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

import java.net.URL;

/**
 *
 * @author andy
 */
public class GlobalFilePathes {
    // // the most recently opened files, RECENT_1 is the most recent entry
    // // the paths are saved in the properties and loaded when the program starts -->
    public static String RECENT_CIRCUITS_1 = "", RECENT_CIRCUITS_2 = "", RECENT_CIRCUITS_3 = "", RECENT_CIRCUITS_4 = "";    
    //------------------------
    // // Path for storing all images used:
    public static URL PFAD_PICS_URL;  // gleich wie 'PFAD_PICS'
    // // Path in which the current JAR file is located -->
    public static String PFAD_JAR_HOME;
    
    // // Path and name of the current file for the circuit simulation (*.ipes):
    public static String DATNAM;
    // // Path and name of the circuit simulation file loaded last time (*.ipes):
    // // --> is important if the path structure has been changed --> this will update local paths, see ProjectData.localizeRelativePath()
    public static String DATNAM_NOT_DEFINED = "not_defined";
        
    // this is the file path from where the original ipes file was loades. Be cautious, here:
    // this file path does not change when the user saves the file to another location. It shows
    // only the file path from where the stuff was originally loades.
    // This can maybe removed in the future. I keep it here for backwards-compatibility, since
    // somebody is using this field at the moment in a Java-Block.
    public static String datnamAbsLoadIPES;
}
