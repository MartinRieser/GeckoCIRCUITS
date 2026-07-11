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
package ch.technokrat.gecko;

/**
 * RMI callback interface for propagating messages (system output, errors,
 * status) from the GeckoCIRCUITS server back to connected clients such as
 * MATLAB.
 */
public interface CallbackClientInterface
  extends java.rmi.Remote{

    /**
     * Displays a system-level message on the client.
     *
     * @param message the message string to process
     * @throws java.rmi.RemoteException if a remote communication error occurs
     */
    void printSystemMessage(String message) throws java.rmi.RemoteException;

    /**
     * Displays an error-level message on the client.
     *
     * @param message the error message string to process
     * @throws java.rmi.RemoteException if a remote communication error occurs
     */
    void printErrorMessage(String message) throws java.rmi.RemoteException;

    /**
     * Returns client identification information (user, hostname, connection date).
     *
     * @return a string containing client status info
     * @throws java.rmi.RemoteException if a remote communication error occurs
     */
    String ping() throws java.rmi.RemoteException;
}
