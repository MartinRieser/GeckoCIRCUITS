/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 */
package gecko.core.control.calculators;

import java.util.Map;

/**
 * Immutable capture of a script block's state at a point in execution: the
 * paused statement's source line (or -1 when captured outside a statement),
 * the simulation time, the time step width, inputs, outputs and the user
 * (persistent state) variables. Produced when a breakpoint pauses the block
 * and by on-demand watch queries.
 *
 * @param blockName name of the script block (component name)
 * @param line 1-based source line of the paused statement, -1 if not paused at a statement
 * @param time simulation time in seconds
 * @param dt time step width in seconds
 * @param variables user (persistent state) variables
 * @param inputs input signal values u1..uN
 * @param outputs output signal values y1..yN
 */
public record ScriptDebugSnapshot(
        String blockName,
        int line,
        double time,
        double dt,
        Map<String, Double> variables,
        double[] inputs,
        double[] outputs) {
}
