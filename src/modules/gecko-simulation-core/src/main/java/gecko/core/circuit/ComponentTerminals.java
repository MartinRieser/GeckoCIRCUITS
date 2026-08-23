/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.circuit;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Terminal geometry of .ipes components in integer grid units.
 *
 * <p>Two-port LK/THERMAL components place their input terminal
 * {@link #TERMINAL_DISTANCE} units against the flow direction and their output
 * terminal {@link #TERMINAL_DISTANCE} units along it, mirroring the classic
 * Swing editor (compare {@code AbstractTwoPortLKreisBlock} and
 * {@code TerminalRelativePosition} in gecko-gui). Single-terminal CONTROL
 * blocks carry their terminal only on the active side: constant and signal
 * source have one output, gate and scope have one input (compare
 * {@code RegelBlock} / {@code ControlTyp} in gecko-gui). Both the legacy
 * CONTROL type numbers (3-6) and the web catalog range
 * ({@link CircuitTypCore#CTRL_SCOPE} etc.) are recognized.</p>
 *
 * <p>The web frontend keeps an equivalent TypeScript implementation in
 * {@code frontend/src/model/geometry.ts}; the two must stay in sync.</p>
 */
public final class ComponentTerminals {

    /** Orientation codes as serialized in .ipes files ({@code ComponentDirection} in gecko-gui). */
    public static final int SOUTH_NORTH = 501;
    /** Orientation code: terminals point west then east ({@code ComponentDirection} in gecko-gui). */
    public static final int WEST_EAST = 502;
    /** Orientation code: terminals point north then south ({@code ComponentDirection} in gecko-gui). */
    public static final int NORTH_SOUTH = 503;
    /** Orientation code: terminals point east then west ({@code ComponentDirection} in gecko-gui). */
    public static final int EAST_WEST = 504;

    /** Distance of a two-port terminal from the component center, in grid units. */
    public static final int TERMINAL_DISTANCE = 2;

    // Legacy CONTROL block types of the classic editor (ControlTyp in gecko-gui)
    /** Classic control block type: constant value output. */
    public static final int CONTROL_CONSTANT = 3;
    /** Classic control block type: signal source. */
    public static final int CONTROL_SIGNAL_SOURCE = 4;
    /** Classic control block type: oscilloscope / scope. */
    public static final int CONTROL_SCOPE = 5;
    /** Classic control block type: gate input. */
    public static final int CONTROL_GATE = 6;

    private ComponentTerminals() {
    }

    /**
     * Unit flow vector (pointing from input to output) of an orientation code.
     * Unknown codes default to {@link #NORTH_SOUTH}, like the classic editor.
     */
    public static int[] flowVector(int orientation) {
        return switch (orientation) {
            case EAST_WEST -> new int[]{-1, 0};
            case WEST_EAST -> new int[]{1, 0};
            case SOUTH_NORTH -> new int[]{0, -1};
            default -> new int[]{0, 1}; // NORTH_SOUTH
        };
    }

    /**
     * Flow vector of CONTROL blocks, which orient their terminals differently
     * from LK two-ports: the classic editor maps the block-relative terminal
     * coordinates through {@code TerminalRelativePosition.getPointFromDirection},
     * so e.g. NORTH_SOUTH places the output terminal at {@code (x+2, y)} —
     * horizontal flow, the default drawing direction of control blocks.
     */
    public static int[] controlFlowVector(int orientation) {
        return switch (orientation) {
            case SOUTH_NORTH -> new int[]{-1, 0};
            case WEST_EAST -> new int[]{0, -1};
            case EAST_WEST -> new int[]{0, 1};
            default -> new int[]{1, 0}; // NORTH_SOUTH
        };
    }

    /**
     * All terminals (inputs and outputs) of a component placed at the given
     * position with the given orientation, as {@code {x, y}} grid points.
     * CONTROL blocks use {@link #controlFlowVector}; everything else uses
     * the LK two-port layout.
     */
    public static List<int[]> terminalsOf(CircuitModel.ComponentData comp, int[] position, int orientation) {
        int x = position.length > 0 ? position[0] : 0;
        int y = position.length > 1 ? position[1] : 0;
        int[] dir = isControlFamily(comp) ? controlFlowVector(orientation) : flowVector(orientation);
        int type = comp.getType();

        if (isControlFamily(comp)) {
            // constant and signal source: 0 inputs, 1 output on the output side
            if (type == CONTROL_SIGNAL_SOURCE || type == CONTROL_CONSTANT
                    || type == CircuitTypCore.CTRL_SIGNAL.getTypeNumber()
                    || type == CircuitTypCore.CTRL_CONSTANT.getTypeNumber()) {
                return List.of(offset(x, y, dir, TERMINAL_DISTANCE));
            }
            // gate and scope: 1 input on the input side, 0 outputs
            if (type == CONTROL_GATE || type == CONTROL_SCOPE
                    || type == CircuitTypCore.CTRL_SCOPE.getTypeNumber()) {
                return List.of(offset(x, y, dir, -TERMINAL_DISTANCE));
            }
        }

        return List.of(offset(x, y, dir, -TERMINAL_DISTANCE), offset(x, y, dir, TERMINAL_DISTANCE));
    }

    private static boolean isControlFamily(CircuitModel.ComponentData comp) {
        return comp.getFamily() != null && "CONTROL".equalsIgnoreCase(comp.getFamily());
    }

    private static int[] offset(int x, int y, int[] dir, int distance) {
        return new int[]{x + dir[0] * distance, y + dir[1] * distance};
    }
}
