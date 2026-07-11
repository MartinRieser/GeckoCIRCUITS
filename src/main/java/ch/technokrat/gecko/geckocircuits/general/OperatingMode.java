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

/**
 * Defines the operating mode of the GeckoCIRCUITS application.
 */
public enum OperatingMode {

    /** Running as a standalone desktop application. */
    STANDALONE,
    /** Running as a Simulink S-function. */
    SIMULINK,
    /** Running with external interface. */
    EXTERNAL,
    /** Running in remote control mode. */
    REMOTE,
    /** Running with magneto-motive force (MMF) coupling. */
    MMF
}
