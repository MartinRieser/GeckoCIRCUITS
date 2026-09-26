/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

import java.util.Map;

/**
 * Live view into a script block's execution state, handed to a
 * {@link ScriptDebugHook} immediately before a statement executes. All views
 * are read on the simulation thread while the statement is paused, so the
 * hook may build an immutable {@link ScriptDebugSnapshot} from them.
 */
public interface ScriptDebugContext {

    /** Name of the script block (component name) being executed. */
    String blockName();

    /** Current simulation time in seconds. */
    double timeSeconds();

    /** Current time step width in seconds. */
    double stepWidth();

    /**
     * Live map of user (persistent state) variables. The map is only stable
     * while the simulation thread is blocked inside the hook.
     */
    Map<String, Double> userVariables();

    /** Copy-safe input signal values {@code u1..uN}. */
    double[] inputSignals();

    /** Copy-safe output signal values {@code y1..yN}. */
    double[] outputSignals();
}
