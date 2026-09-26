/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

/**
 * Callback invoked by {@link ScriptBlockCalculator} immediately before a
 * script statement executes. Implementations may block the calling
 * (simulation) thread to realize breakpoints and single stepping; they must
 * eventually return so the simulation can make progress or observe a
 * cancellation request.
 *
 * <p>The hook is only consulted when one is attached to the calculator via
 * {@link ScriptBlockCalculator#setDebugHook(ScriptDebugHook)}; attaching
 * nothing keeps the interpreter overhead at zero.
 */
@FunctionalInterface
public interface ScriptDebugHook {

    /**
     * Called before the statement on {@code line} (1-based, referring to the
     * block's original source code) executes.
     *
     * @param context live view of the script's execution state
     * @param line source line of the statement about to execute
     */
    void beforeStatement(ScriptDebugContext context, int line);
}
