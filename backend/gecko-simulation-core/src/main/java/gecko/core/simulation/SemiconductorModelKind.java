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
package gecko.core.simulation;

/**
 * Convergence model for the nonlinear semiconductor elements.
 *
 * <ul>
 *   <li><b>CLASSIC_PIECEWISE_LINEAR</b>: the legacy state machine flips diodes,
 *       thyristors, and IGBTs between on/off resistances until stable - exact
 *       legacy parity.</li>
 *   <li><b>SHOCKLEY_NEWTON_RAPHSON</b>: diodes additionally converge through
 *       Newton-Raphson iteration on the smooth Shockley junction equation
 *       (auto-calibrated to the element's forward voltage at its rated
 *       current); latching thyristors and gated IGBTs keep the state machine.</li>
 * </ul>
 *
 * @since v2.18.0 Task L3 - High-Performance Solver & Advanced Numerics
 */
public enum SemiconductorModelKind {

    /** Legacy piecewise-linear on/off state machine. */
    CLASSIC_PIECEWISE_LINEAR,

    /** Shockley junction equation converged by Newton-Raphson iteration (diodes). */
    SHOCKLEY_NEWTON_RAPHSON
}
