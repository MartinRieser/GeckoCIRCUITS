/**
 * Terminal geometry calculations: determines spatial raster coordinates of component
 * terminals on the schematic grid, derived from component type, family, and orientation.
 *
 * .ipes semantics (matching the classic GeckoCIRCUITS Swing editor):
 * - Coordinates are integer grid units (DPIX defines only the pixel rendering scale).
 * - Orientation codes:
 *     501: SOUTH_NORTH (flow points up)
 *     502: WEST_EAST (flow points right, default base orientation)
 *     503: NORTH_SOUTH (flow points down)
 *     504: EAST_WEST (flow points left)
 * - Standard two-port components place the input terminal TWO_PORT_DIST units against
 *   flow and output terminal TWO_PORT_DIST units along flow.
 * - Standard rotation cycle: NORTH_SOUTH (503) -> EAST_WEST (504) -> SOUTH_NORTH (501) -> WEST_EAST (502).
 */
import type { EditorComponent, Point } from './types';
import { CTRL_TYPE, resolveComponentPinCounts } from './componentSchema';
import {
  Orientation,
  ORIENTATION_CYCLE,
  CANVAS_METRICS,
  LkComponentType,
} from './constants';

export { ORIENTATION_CYCLE };

/**
 * Distance in grid units from component origin to terminals for standard two-port components.
 */
export const TWO_PORT_DIST = CANVAS_METRICS.TWO_PORT_DIST;

/** CONTROL types carrying a single output terminal (probes, constant, signal source). */
const CONTROL_OUTPUT_ONLY = new Set<number>([
  CTRL_TYPE.LEGACY_VOLTMETER,
  CTRL_TYPE.VOLTMETER,
  CTRL_TYPE.LEGACY_AMMETER,
  CTRL_TYPE.AMMETER,
  CTRL_TYPE.LEGACY_SIGNAL_SOURCE,
  CTRL_TYPE.SIGNAL_SOURCE,
  CTRL_TYPE.LEGACY_CONSTANT,
  CTRL_TYPE.CONSTANT,
]);

/** CONTROL types carrying a single input terminal (gate, scope). */
const CONTROL_INPUT_ONLY = new Set<number>([
  CTRL_TYPE.LEGACY_GATE,
  CTRL_TYPE.LEGACY_SCOPE,
  CTRL_TYPE.SCOPE,
]);

/**
 * Returns the next orientation in the standard rotation cycle (triggered by 'R' or right-click).
 *
 * @param orientation Current orientation code
 * @returns Next orientation code in cycle (503 -> 504 -> 501 -> 502)
 */
export function nextOrientation(orientation: number): number {
  const i = ORIENTATION_CYCLE.indexOf(orientation as (typeof ORIENTATION_CYCLE)[number]);
  return ORIENTATION_CYCLE[(i + 1) % ORIENTATION_CYCLE.length];
}

/**
 * Unit flow-direction vector for a standard electrical (LK) component orientation (from input to output).
 *
 * @param orientation Spatial orientation code
 * @returns 2D direction vector with magnitude 1 along primary axis
 */
export function flowVector(orientation: number): Point {
  switch (orientation) {
    case Orientation.WEST_EAST:
      return { x: 1, y: 0 };
    case Orientation.EAST_WEST:
      return { x: -1, y: 0 };
    case Orientation.NORTH_SOUTH:
      return { x: 0, y: 1 };
    case Orientation.SOUTH_NORTH:
      return { x: 0, y: -1 };
    default:
      return { x: 0, y: 1 };
  }
}

/**
 * Flow vector of CONTROL blocks. The classic editor maps control terminal
 * offsets through TerminalRelativePosition, so e.g. NORTH_SOUTH places the
 * output at (x+2, y) — horizontal flow, the default drawing direction of
 * control blocks (a quarter turn against the LK two-port vector).
 *
 * @param orientation Spatial orientation code
 * @returns 2D direction vector for control block input-to-output flow
 */
export function controlFlowVector(orientation: number): Point {
  switch (orientation) {
    case Orientation.SOUTH_NORTH:
      return { x: -1, y: 0 };
    case Orientation.WEST_EAST:
      return { x: 0, y: -1 };
    case Orientation.EAST_WEST:
      return { x: 0, y: 1 };
    case Orientation.NORTH_SOUTH:
    default:
      return { x: 1, y: 0 };
  }
}

/**
 * Spatial coordinate groupings for a component's input and output terminals.
 */
export interface TerminalPositions {
  /** Array of input terminal coordinates in grid raster units. */
  input: Point[];
  /** Array of output terminal coordinates in grid raster units. */
  output: Point[];
}

/**
 * Calculates absolute terminal grid coordinates for a component based on its
 * center position, orientation, family, and type.
 *
 * Handles standard two-port elements, single-port probes/sources, multi-channel scopes,
 * dynamic N-input/M-output function blocks, 4-pin ideal transformers, and 3-pin BJTs.
 *
 * @param component Component definition with type, position, orientation, and optional pin labels/parameters
 * @returns Object containing arrays of input and output terminal coordinates
 */
export function terminalPositions(component: {
  type: number;
  family?: string;
  position: number[];
  orientation: number;
  inputLabels?: string[];
  outputLabels?: string[];
  inputs?: unknown[];
  parameters?: Record<string, number | string | boolean>;
}): TerminalPositions {
  const center = { x: component.position[0], y: component.position[1] };
  const family = component.family || 'LK';
  const dir = family === 'CONTROL'
    ? controlFlowVector(component.orientation)
    : flowVector(component.orientation);

  if (family === 'CONTROL') {
    // Constant & signal sources: 0 inputs, 1 output on the output side
    if (CONTROL_OUTPUT_ONLY.has(component.type)) {
      return {
        input: [],
        output: [{ x: center.x + dir.x * TWO_PORT_DIST, y: center.y + dir.y * TWO_PORT_DIST }],
      };
    }

    // Oscilloscope probe: can have dynamic multiple inputs!
    if (component.type === CTRL_TYPE.SCOPE || component.type === CTRL_TYPE.LEGACY_SCOPE) {
      const count = Math.max(1, component.inputLabels?.length || component.inputs?.length || 1);
      if (count <= 1) {
        return {
          input: [{ x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST }],
          output: [],
        };
      }
      const inputs: Point[] = [];
      const step = CANVAS_METRICS.MULTI_PIN_STEP;
      const startOffset = -((count - 1) * step) / 2;
      for (let i = 0; i < count; i++) {
        const offset = startOffset + i * step;
        inputs.push({
          x: center.x - dir.x * TWO_PORT_DIST - dir.y * offset,
          y: center.y - dir.y * TWO_PORT_DIST + dir.x * offset,
        });
      }
      return {
        input: inputs,
        output: [],
      };
    }

    // Function Block / Classic Java Block: dynamic N inputs, M outputs
    if (component.type === CTRL_TYPE.SCRIPT || component.type === CTRL_TYPE.LEGACY_JAVA_FUNCTION) {
      const { inputCount, outputCount } = resolveComponentPinCounts(component);
      const step = CANVAS_METRICS.MULTI_PIN_STEP;

      const inputs: Point[] = [];
      const inStart = inputCount > 1 ? -((inputCount - 1) * step) / 2 : 0;
      for (let i = 0; i < inputCount; i++) {
        const offset = inStart + i * step;
        inputs.push({
          x: center.x - dir.x * TWO_PORT_DIST - dir.y * offset,
          y: center.y - dir.y * TWO_PORT_DIST + dir.x * offset,
        });
      }

      const outputs: Point[] = [];
      const outStart = outputCount > 1 ? -((outputCount - 1) * step) / 2 : 0;
      for (let j = 0; j < outputCount; j++) {
        const offset = outStart + j * step;
        outputs.push({
          x: center.x + dir.x * TWO_PORT_DIST - dir.y * offset,
          y: center.y + dir.y * TWO_PORT_DIST + dir.x * offset,
        });
      }

      return { input: inputs, output: outputs };
    }

    // Gate driver & single-input control blocks: 1 input, 0 outputs
    if (CONTROL_INPUT_ONLY.has(component.type)) {
      return {
        input: [{ x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST }],
        output: [],
      };
    }
  }

  // Ideal Transformer (classic LK_TRANS): four pins — primary pair one grid
  // pitch against the flow, secondary pair one pitch along it. Pin offsets
  // follow the classic TerminalRelativePosition rotation.
  if (component.type === LkComponentType.TRANSFORMER && family !== 'CONTROL') {
    return {
      input: [
        classicPinOffset(center, component.orientation, -1, 2),
        classicPinOffset(center, component.orientation, -1, -2),
      ],
      output: [
        classicPinOffset(center, component.orientation, 1, 2),
        classicPinOffset(center, component.orientation, 1, -2),
      ],
    };
  }

  // BJT (classic LK_BJT): collector at the two-port input, base sticking out
  // sideways at (-2, 0), emitter at the two-port output.
  if (component.type === LkComponentType.BJT && family !== 'CONTROL') {
    return {
      input: [
        { x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST },
        classicPinOffset(center, component.orientation, -2, 0),
      ],
      output: [{ x: center.x + dir.x * TWO_PORT_DIST, y: center.y + dir.y * TWO_PORT_DIST }],
    };
  }

  // Standard two-port component fallback
  return {
    input: [{ x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST }],
    output: [{ x: center.x + dir.x * TWO_PORT_DIST, y: center.y + dir.y * TWO_PORT_DIST }],
  };
}

/**
 * Absolute grid point of a classic TerminalRelativePosition pin offset
 * (posX, posY with posY pointing "up") for the given LK orientation.
 * Must stay in sync with the core's rotatePinOffset (NetlistBuilder).
 */
function classicPinOffset(center: Point, orientation: number, posX: number, posY: number): Point {
  switch (orientation) {
    case Orientation.EAST_WEST:
      return { x: center.x + posY, y: center.y + posX };
    case Orientation.SOUTH_NORTH:
      return { x: center.x - posX, y: center.y + posY };
    case Orientation.WEST_EAST:
      return { x: center.x - posY, y: center.y - posX };
    case Orientation.NORTH_SOUTH:
    default:
      return { x: center.x + posX, y: center.y - posY };
  }
}

/** Point-addressable reference to a specific terminal on a named component. */
export interface TerminalRef {
  /** Name of the parent component (e.g., "R.1"). */
  component: string;
  /** Terminal port side: 'x' for input terminals, 'y' for output terminals. */
  side: 'x' | 'y';
  /** Zero-based pin index on this port side. */
  index: number;
  /** Absolute coordinates of the terminal in grid raster units. */
  point: Point;
}

/**
 * Returns a flat, point-addressable list of all terminals across all components in the circuit.
 *
 * @param components Array of editor components
 * @returns Array of TerminalRef records
 */
export function allTerminals(components: EditorComponent[]): TerminalRef[] {
  const refs: TerminalRef[] = [];
  for (const c of components) {
    const t = terminalPositions(c);
    t.input.forEach((point, index) =>
      refs.push({ component: c.name, side: 'x', index, point }),
    );
    t.output.forEach((point, index) =>
      refs.push({ component: c.name, side: 'y', index, point }),
    );
  }
  return refs;
}

/**
 * Searches for a component terminal within snap distance of a given test coordinate.
 *
 * @param components Array of editor components
 * @param point Test point in grid raster units
 * @param maxDistance Maximum Euclidean snap distance in grid units (defaults to CANVAS_METRICS.DEFAULT_SNAP_DISTANCE)
 * @returns Nearest TerminalRef or null if no terminal lies within maxDistance
 */
export function terminalNear(
  components: EditorComponent[],
  point: Point,
  maxDistance = CANVAS_METRICS.DEFAULT_SNAP_DISTANCE,
): TerminalRef | null {
  let best: TerminalRef | null = null;
  let bestDist = maxDistance;
  for (const ref of allTerminals(components)) {
    const d = Math.hypot(ref.point.x - point.x, ref.point.y - point.y);
    if (d <= bestDist) {
      best = ref;
      bestDist = d;
    }
  }
  return best;
}

const sameGridPoint = (a: Point, b: Point) => a.x === b.x && a.y === b.y;

/**
 * Checks whether placing a component at candidate coordinates collides with any existing component.
 *
 * A collision occurs when:
 * 1. Both component origins coincide.
 * 2. A terminal of the candidate sits on the center of an existing component.
 * 3. The center of the candidate sits on a terminal of an existing component.
 *
 * Note: Terminal-to-terminal coincidence is NOT a conflict (pin-to-pin junction placement is supported).
 *
 * @param candidate Candidate placement parameters (type, family, position, orientation)
 * @param existing Array of components already placed on the sheet
 * @returns Name of the colliding existing component, or null if no collision
 */
export function findPlacementConflict(
  candidate: { type: number; family?: string; position: number[]; orientation: number },
  existing: EditorComponent[],
): string | null {
  const candCenter = { x: candidate.position[0], y: candidate.position[1] };
  const candTerms = terminalPositions({ ...candidate, position: candidate.position });
  const candTermPoints = [...candTerms.input, ...candTerms.output];

  for (const other of existing) {
    const otherCenter = { x: other.position[0], y: other.position[1] };
    if (sameGridPoint(candCenter, otherCenter)) {
      return other.name;
    }
    const otherTerms = terminalPositions(other);
    if (
      [...otherTerms.input, ...otherTerms.output].some((t) => sameGridPoint(t, candCenter))
    ) {
      return other.name;
    }
    if (candTermPoints.some((t) => sameGridPoint(t, otherCenter))) {
      return other.name;
    }
  }
  return null;
}
