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

import ch.technokrat.gecko.geckocircuits.general.SolverType;
import ch.technokrat.modelviewcontrol.ModelMVC;

/**
 * User-configurable solver parameters including solver type, step sizes,
 * pre-calculation settings, and simulation time boundaries.
 */
public class SolverSettings {
    
    /**
     * The integration solver type (backward Euler, trapezoidal, Gear-Shichman).
     * Mutable to allow runtime switching via the simulation dialog.
     */
    public ModelMVC<SolverType> SOLVER_TYPE = new ModelMVC<SolverType>(SolverType.SOLVER_BE ,"solver");
    /** Pre-calculation time duration (negative means disabled). */
    public final ModelMVC<Double> _T_pre = new ModelMVC<Double>(-100e-3, "pre-calculation time");
    /** Pre-calculation step width. */
    public final ModelMVC<Double> _dt_pre = new ModelMVC<Double>(100e-9, "pre-calculation step width");
    /** Simulation time step width (dt). */
    public final ModelMVC<Double> dt = new ModelMVC<Double>(0.1e-6, "simulation step width dt");
    /** Total simulation duration. */
    public final ModelMVC<Double> _tDURATION = new ModelMVC<Double>(20e-3, "simulation time");
    /** Pause time during simulation (negative means no pause). */
    public final ModelMVC<Double> _tPAUSE = new ModelMVC<Double>(-1.0, "simulation pause time");
    
    public double _dt_ALT;  // // Remember the old values ​​after a change in 'DialogSimParameter'
    // flag used for solver start
    public boolean inPreCalculationMode = false;
}
