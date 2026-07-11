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

package ch.technokrat.gecko.geckocircuits.nativec;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Wrapper Class for a Library File that checks if the file exists
 * 
 * @author DIEHL Controls Ricardo Richter
 */
public class NativeCLibraryFile {
    private long _timeStamp;
    private String _libPathName;
    private File _libFile;
    
    /**
     * Creates an empty NativeCLibraryFile with no file reference.
     */
    public NativeCLibraryFile () {
    }
    
    /**
     * Creates a NativeCLibraryFile from the given file path string.
     *
     * @param fileName the absolute path to the library file
     * @throws FileNotFoundException if the file does not exist
     */
    @SuppressWarnings("this-escape")
    public NativeCLibraryFile (final String fileName) throws FileNotFoundException {
        setFile(fileName);
    }
    
    /**
     * Creates a NativeCLibraryFile from the given File object.
     *
     * @param file the library file
     * @throws FileNotFoundException if the file does not exist
     */
    @SuppressWarnings("this-escape")
    public NativeCLibraryFile (final File file) throws FileNotFoundException {
        setFile(file);
    }
    
    /**
     * Returns the absolute path name of the library file.
     *
     * @return the library path name, or null if not set
     */
    public String getFileName () {
        return _libPathName;
    }
    
    /**
     * Sets the library file and updates its timestamp from the given File object.
     *
     * @param file the library file to set
     * @throws FileNotFoundException if the file does not exist
     */
    public void setFile (final File file) throws FileNotFoundException {
        if (file != null) {
            if (!file.exists()) {
                throw new FileNotFoundException("Could not find Library File " + file.getAbsolutePath());
            } else {
                _libFile = file;
                _libPathName = _libFile.getAbsolutePath();
                _timeStamp = _libFile.lastModified();
            }
        }
    }
    
    /**
     * Sets the library file from a file path string and updates its timestamp.
     *
     * @param fileName the absolute path to the library file
     * @throws FileNotFoundException if the file does not exist
     */
    public void setFile (final String fileName) throws FileNotFoundException {
        _libFile = new File(fileName);
        if (!_libFile.exists()) {
            throw new FileNotFoundException("Could not find Library File " + fileName);
        } else {
            _libPathName = _libFile.getAbsolutePath();
            _timeStamp = _libFile.lastModified();
        }
    }
    
    /**
     * Clears the library file reference and resets the timestamp to zero.
     */
    public void setFile () {
        _libFile = null;
        _libPathName = null;
        _timeStamp = 0;
    }
    
    /**
     * Returns the underlying File object for the library file.
     *
     * @return the library File, or null if not set
     */
    public File getFile() {
        return _libFile;
    }
    
    public File safeGetFile () throws FileNotFoundException {
        if (_libFile == null || _libPathName == null) {
            throw new FileNotFoundException("No Library File was selected!");
        } else {
            return _libFile;
        }
    }
    
    public String safeGetFileName () throws FileNotFoundException {
        if (_libFile == null || _libPathName == null) {
            throw new FileNotFoundException("No Library File was selected!");
        } else {
            return _libPathName;
        }
    }
    
    /**
     * The TimeStamp can be used in future as an easy implementation for 
     * revision control.
     * @return the last-modified timestamp of the library file
     */
    public long getTimeStamp () {
        return _timeStamp;
    }
    
    /**
     * Updates the timestamp to the current last-modified time of the library file.
     *
     * @return true if the timestamp changed, false otherwise
     * @throws FileNotFoundException if no library file is set or it cannot be found
     */
    public boolean updateTimeStamp () throws FileNotFoundException {
        long newTimeStamp;
        if (_libFile == null) {
            throw new FileNotFoundException("No Library File was selected!");
        } else {
            if (!_libFile.exists()) {
                throw new FileNotFoundException("Could not find Library File " + _libPathName);
            }
            newTimeStamp = _libFile.lastModified();
            if (newTimeStamp != _timeStamp) {
                _timeStamp = newTimeStamp;
                return true;
            }
        }
        return false;
    }
}
