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
import { CTRL_TYPE, resolveComponentPinCounts, COMPONENT_METAS } from './componentSchema';
import {
  Orientation,
  ORIENTATION_CYCLE,
  CANVAS_METRICS,
  LkComponentType,
} from './constants';

export { ORIENTATION_CYCLE };

/**
 * Pin step (grid units) for multi-terminal SCRIPT / function blocks.
 * The simulation core places script inputs at rel (-2, -i) — one row per
 * input, anchored at the block's center row (ControlCalculatorBuilder.
 * terminalPoint). Using the same unit step here makes the editor's pins and
 * the engine's electrical pins the SAME grid points, so a wire that visibly
 * reaches a pin is also the wire the simulation uses. Scopes keep the wider
 * MULTI_PIN_STEP: their pins are a visual affordance only (channels bind by
 * signal name), so their spacing is free.
 */
export const SCRIPT_PIN_STEP = 1;

/**
 * Offsets of a multi-pin block whose FIRST pin sits on the anchor row and
 * further pins extend in +perp direction: adding a channel appends a pin and
 * NEVER moves existing pins, so wires never need re-binding when a block
 * grows.
 */
export function anchoredPinOffsets(count: number, step: number): number[] {
  return Array.from({ length: Math.max(0, count) }, (_, i) => i * step);
}

/**
 * Offsets of the LEGACY (pre-anchoring) centered block: pins symmetric about
 * the anchor row. Kept only to migrate stored circuits — adding or removing a
 * channel shifted EVERY pin, which repeatedly corrupted attached wires.
 */
export function legacyCenteredPinOffsets(count: number, step: number): number[] {
  if (count <= 1) {
    return [0];
  }
  const start = -((count - 1) * step) / 2;
  return Array.from({ length: count }, (_, i) => start + i * step);
}

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
  CTRL_TYPE.TIME,
  58,
]);

/** CONTROL types carrying a single input terminal (gate, scope). */
const CONTROL_INPUT_ONLY = new Set<number>([
  CTRL_TYPE.LEGACY_GATE,
  CTRL_TYPE.GATE,
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
  return terminalPositionsWithPinMode(component, 'anchored');
}

/**
 * Legacy multi-pin layout (centered block) of a component, used exclusively
 * by {@link normalizeLegacyMultiPinWires} to migrate stored circuits whose
 * wires end on the pre-anchoring pin positions.
 */
export function legacyTerminalPositions(component: Parameters<typeof terminalPositions>[0]): TerminalPositions {
  return terminalPositionsWithPinMode(component, 'legacy-centered');
}

function terminalPositionsWithPinMode(
  component: Parameters<typeof terminalPositions>[0],
  pinMode: 'anchored' | 'legacy-centered',
): TerminalPositions {
  const center = { x: component.position[0], y: component.position[1] };
  const family = component.family || 'LK';
  const dir = family === 'CONTROL'
    ? controlFlowVector(component.orientation)
    : flowVector(component.orientation);
  const offsetsFor = (count: number, step: number) =>
    pinMode === 'anchored'
      ? anchoredPinOffsets(count, step)
      : legacyCenteredPinOffsets(count, step);

  if (family === 'CONTROL') {
    // Constant & signal sources: 0 inputs, 1 output on the output side
    if (CONTROL_OUTPUT_ONLY.has(component.type)) {
      return {
        input: [],
        output: [{ x: center.x + dir.x * TWO_PORT_DIST, y: center.y + dir.y * TWO_PORT_DIST }],
      };
    }

    // Oscilloscope: dynamic channel count. Pins are a visual affordance —
    // channels bind by signal NAME — but they must stay put when a channel
    // is added, hence the anchored block (channel 1 on the anchor row,
    // further channels appended in +perp direction).
    if (component.type === CTRL_TYPE.SCOPE || component.type === CTRL_TYPE.LEGACY_SCOPE) {
      const count = Math.max(1, component.inputLabels?.length || component.inputs?.length || 1);
      const offsets = offsetsFor(count, CANVAS_METRICS.MULTI_PIN_STEP);
      return {
        input: offsets.map((offset) => ({
          x: center.x - dir.x * TWO_PORT_DIST - dir.y * offset,
          y: center.y - dir.y * TWO_PORT_DIST + dir.x * offset,
        })),
        output: [],
      };
    }

    // Function Block / Classic Java Block: dynamic N inputs, M outputs.
    // Unit step, anchored — bit-identical to the core engine's terminal
    // grid (inputs rel (-2, -i), outputs rel (+2, -j)), so wires drawn to
    // these pins are the wires the simulation connects. (The legacy editor
    // centered these pins with step 2 — the migration layout.)
    if (component.type === CTRL_TYPE.SCRIPT || component.type === CTRL_TYPE.LEGACY_JAVA_FUNCTION) {
      const { inputCount, outputCount } = resolveComponentPinCounts(component);
      const step = pinMode === 'anchored' ? SCRIPT_PIN_STEP : CANVAS_METRICS.MULTI_PIN_STEP;

      const inputs: Point[] = offsetsFor(inputCount, step).map((offset) => ({
        x: center.x - dir.x * TWO_PORT_DIST - dir.y * offset,
        y: center.y - dir.y * TWO_PORT_DIST + dir.x * offset,
      }));

      const outputs: Point[] = offsetsFor(outputCount, step).map((offset) => ({
        x: center.x + dir.x * TWO_PORT_DIST - dir.y * offset,
        y: center.y + dir.y * TWO_PORT_DIST + dir.x * offset,
      }));

      return { input: inputs, output: outputs };
    }

    // Gate driver & single-input control blocks: 1 input, 0 outputs
    if (CONTROL_INPUT_ONLY.has(component.type)) {
      return {
        input: [{ x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST }],
        output: [],
      };
    }

    // Standard / multi-terminal CONTROL blocks (Comparator, Logic gates, MUX, Regulators, etc.):
    // Position terminal pins using SCRIPT_PIN_STEP (1 grid unit) anchored from pin 0,
    // bit-identical to the simulation engine's ControlCalculatorBuilder.terminalPoint layout
    // (inputs rel (-2, -i), outputs rel (+2, -j)).
    const meta = COMPONENT_METAS[component.type];
    const inputCount = meta?.terminals?.input?.length ?? 1;
    const outputCount = meta?.terminals?.output?.length ?? 1;
    const step = SCRIPT_PIN_STEP;

    const inputs: Point[] = offsetsFor(inputCount, step).map((offset) => ({
      x: center.x - dir.x * TWO_PORT_DIST - dir.y * offset,
      y: center.y - dir.y * TWO_PORT_DIST + dir.x * offset,
    }));

    const outputs: Point[] = offsetsFor(outputCount, step).map((offset) => ({
      x: center.x + dir.x * TWO_PORT_DIST - dir.y * offset,
      y: center.y + dir.y * TWO_PORT_DIST + dir.x * offset,
    }));

    return { input: inputs, output: outputs };
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

  // Mutual Inductance Coupler (classic LK_M): magnetic coupling declaration without electrical wire pins
  if (component.type === LkComponentType.MUTUAL_INDUCTANCE && family !== 'CONTROL') {
    return {
      input: [],
      output: [],
    };
  }

  // Global Net / Ground Terminal (classic LK_GLOBAL_TERMINAL): single terminal pin at the stem end
  if ((component.type === LkComponentType.GLOBAL_TERMINAL || component.type === 31) && family !== 'CONTROL') {
    return {
      input: [{ x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST }],
      output: [],
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
  maxDistance: number = CANVAS_METRICS.DEFAULT_SNAP_DISTANCE,
): TerminalRef | null {
  let best: TerminalRef | null = null;
  let bestDist: number = maxDistance;
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
      [...otherTerms.input, ...otherTerms.output].some((t) => sameGridPoint(t, otherCenter))
    ) {
      return other.name;
    }
    if (candTermPoints.some((t) => sameGridPoint(t, otherCenter))) {
      return other.name;
    }
  }
  return null;
}

/**
 * Re-binds one endpoint of a wire polyline to a new grid point while keeping
 * every segment axis-parallel. Rewriting only the endpoint coordinate (a
 * plain map) turns the final segment diagonal whenever the pin moved
 * perpendicular to the wire's approach — exactly what happens when a scope
 * channel is added or removed and the centered pin block shifts every pin by
 * one grid row. This helper instead inserts the missing corner point,
 * preserving the axis of the original final segment.
 *
 * @param points Wire polyline in grid raster units (length >= 2).
 * @param endIndex Which endpoint moves: 0 or {@code points.length - 1}.
 * @param newPoint New grid position for that endpoint.
 * @returns New polyline with the endpoint re-bound orthogonally and
 *          consecutive duplicate points collapsed; the SAME array reference
 *          when the endpoint already sits at {@code newPoint}.
 */
export function rebindWireEndpointOrthogonally(
  points: number[][],
  endIndex: number,
  newPoint: number[],
): number[][] {
  const old = points[endIndex];
  if (!old || (old[0] === newPoint[0] && old[1] === newPoint[1])) {
    return points;
  }
  const neighborIdx = endIndex === 0 ? 1 : points.length - 2;
  const prev = points[neighborIdx];

  // Replacement tail (end) respectively head (start) of the polyline.
  let replacement: number[][];
  if (!prev) {
    replacement = [[newPoint[0], newPoint[1]]];
  } else if (prev[0] === newPoint[0] || prev[1] === newPoint[1]) {
    // The neighbor already shares an axis with the target: a straight swap
    // keeps everything orthogonal.
    replacement = [[newPoint[0], newPoint[1]]];
  } else {
    // Insert a corner NEXT TO THE NEIGHBOR, so the segment adjacent to the
    // endpoint stays on the endpoint's own axis. Turning at the neighbor —
    // not at the endpoint's column/row — keeps the jog off a pin column the
    // wire may terminate on: a vertical jog on the pin column would cross
    // every other pin of the block (electrically tying one signal to
    // several inputs).
    const corner =
      prev[1] === old[1]
        ? [prev[0], newPoint[1]]
        : [newPoint[0], prev[1]];
    replacement =
      endIndex === 0
        ? [[newPoint[0], newPoint[1]], corner]
        : [corner, [newPoint[0], newPoint[1]]];
  }

  const body =
    endIndex === 0 ? points.slice(1) : points.slice(0, -1);
  const merged =
    endIndex === 0
      ? [...replacement, ...body.map((p) => [p[0], p[1]])]
      : [...body.map((p) => [p[0], p[1]]), ...replacement];

  // Collapse consecutive duplicates (e.g. corner coinciding with the neighbor).
  const out: number[][] = [];
  for (const p of merged) {
    const last = out[out.length - 1];
    if (!last || last[0] !== p[0] || last[1] !== p[1]) {
      out.push(p);
    }
  }
  return out.length >= 2 ? out : [merged[0], merged[merged.length - 1]];
}

/**
 * Dense raster cells a stored polyline covers, stepping one axis at a time
 * (x first, like the renderer's elbow insertion). Used for pin-crossing
 * checks; input is expected orthogonal or near-orthogonal.
 */
function denseCellsOfPolyline(points: number[][]): Set<string> {
  const cells = new Set<string>();
  if (points.length === 0) {
    return cells;
  }
  cells.add(`${points[0][0]},${points[0][1]}`);
  for (let i = 1; i < points.length; i++) {
    let x = points[i - 1][0];
    let y = points[i - 1][1];
    const tx = points[i][0];
    const ty = points[i][1];
    while (x !== tx || y !== ty) {
      if (x !== tx) {
        x += Math.sign(tx - x);
      } else {
        y += Math.sign(ty - y);
      }
      cells.add(`${x},${y}`);
    }
  }
  return cells;
}

/**
 * Re-binds a wire endpoint onto a multi-pin block pin, guaranteeing the
 * wiring invariant: the wire touches no pin cell except the one it
 * terminates on. The generic orthogonal rebind is used when its result is
 * clean; if the result would run along the pin line (e.g. a dirty tail with
 * a retrace, or a vertical approach down the pin column — which crosses
 * sibling pins, renders phantom junctions and falsely lights their
 * "wired" badges), the tail is rebuilt canonically: vertical jog on the
 * entry column adjacent to the pin block, then a single-cell entry into
 * the pin.
 *
 * @param points Wire polyline in grid raster units.
 * @param endIndex Which endpoint moves: 0 or {@code points.length - 1}.
 * @param newPin The pin the endpoint must terminate on.
 * @param siblingPins All pins of the block after the change (may include
 *                    {@code newPin}); the result may touch only {@code newPin}.
 * @param center Component center, to pick the entry side away from the body.
 */
export function rebindWireEndpointToPin(
  points: number[][],
  endIndex: number,
  newPin: Point,
  siblingPins: Point[],
  center: Point,
): number[][] {
  const candidate = rebindWireEndpointOrthogonally(points, endIndex, [newPin.x, newPin.y]);
  if (candidate === points) {
    return points;
  }

  const foreign = siblingPins.filter((p) => !(p.x === newPin.x && p.y === newPin.y));
  const cells = denseCellsOfPolyline(candidate);
  if (!foreign.some((p) => cells.has(`${p.x},${p.y}`))) {
    return candidate;
  }

  // Canonical rebuild: approach the pin through the entry cell one step
  // beyond the pin, away from the component body.
  const verticalStack = foreign.length === 0 || foreign.every((p) => p.x === newPin.x);
  const entryX = verticalStack
    ? newPin.x + (Math.sign(newPin.x - center.x) || -1)
    : newPin.x;
  const entryY = verticalStack
    ? newPin.y
    : newPin.y + (Math.sign(newPin.y - center.y) || -1);

  const body = endIndex === 0 ? candidate.slice(1) : candidate.slice(0, -1);
  const onPinLine = (p: number[]) => (verticalStack ? p[0] === newPin.x : p[1] === newPin.y);
  while (body.length > 1 && onPinLine(endIndex === 0 ? body[0] : body[body.length - 1])) {
    if (endIndex === 0) {
      body.shift();
    } else {
      body.pop();
    }
  }
  const anchor = endIndex === 0 ? body[0] : body[body.length - 1];
  if (!anchor) {
    return candidate;
  }

  // anchor -> (jog on the entry line) -> single-cell entry into the pin
  const approach: number[][] = [];
  const ap = (p: number[]) => {
    const last = approach[approach.length - 1];
    if (!last || last[0] !== p[0] || last[1] !== p[1]) {
      approach.push(p);
    }
  };
  if (verticalStack) {
    ap([entryX, anchor[1]]);
    ap([entryX, entryY]);
  } else {
    ap([anchor[0], entryY]);
    ap([entryX, entryY]);
  }
  ap([newPin.x, newPin.y]);

  const rebuilt =
    endIndex === 0
      ? [...approach.reverse(), ...body.map((p) => [p[0], p[1]])]
      : [...body.map((p) => [p[0], p[1]]), ...approach];
  const out: number[][] = [];
  for (const p of rebuilt) {
    const last = out[out.length - 1];
    if (!last || last[0] !== p[0] || last[1] !== p[1]) {
      out.push(p);
    }
  }
  return out.length >= 2 ? out : candidate;
}

/* ------------------------------------------------------------------ */
/* Multi-pin channel surgery: deterministic wire edits for adding and  */
/* removing channels / terminals of scopes and script blocks.          */
/*                                                                     */
/* Invariant these helpers maintain: every wire touches a multi-pin    */
/* block's pin cells only at its terminating endpoint, and every pin   */
/* is terminated by at most one wire. Because the anchored layout is   */
/* append-only, GROWING a block requires no wire edits at all.         */
/* ------------------------------------------------------------------ */

/** Minimal wire shape the pin-surgery helpers operate on. */
export interface WireLike {
  points: number[][];
}

/** Wire edits a channel/terminal-count change prescribes. */
export interface MultiPinWirePlan {
  /** Indices of wires to delete entirely (they fed removed pins). */
  deletions: number[];
  /** Wires whose endpoint must move to a shifted pin, with new geometry. */
  rebinds: { index: number; points: number[][] }[];
}

const EMPTY_PLAN: MultiPinWirePlan = { deletions: [], rebinds: [] };

const samePoint = (a: number[], b: Point) => a[0] === b.x && a[1] === b.y;

/**
 * Wire edits for removing input channel {@code removedIndex} of a multi-pin
 * component: the removed channel's own wire is deleted; wires on the pins
 * BELOW it shift up one slot (the anchored block compacts). Wires on pins
 * above the removed slot are untouched — nothing above ever moves.
 */
export function planChannelRemovalWireEdits(
  component: Parameters<typeof terminalPositions>[0],
  removedIndex: number,
  wires: WireLike[],
): MultiPinWirePlan {
  const before = terminalPositions(component).input;
  const count = before.length;
  if (removedIndex < 0 || removedIndex >= count || count <= 1) {
    return EMPTY_PLAN;
  }
  const after = pinsWithCount(component, count - 1).input;

  const deletions: number[] = [];
  const rebinds: { index: number; points: number[][] }[] = [];
  const center = { x: component.position[0], y: component.position[1] };
  wires.forEach((w, index) => {
    const pts = w.points ?? [];
    if (pts.length < 2) return;
    for (const end of [0, pts.length - 1] as const) {
      if (samePoint(pts[end], before[removedIndex])) {
        deletions.push(index);
        return;
      }
      for (let i = removedIndex + 1; i < count; i++) {
        if (samePoint(pts[end], before[i])) {
          const next = after[i - 1];
          const updated = rebindWireEndpointToPin(pts, end, next, after, center);
          if (updated !== pts) {
            rebinds.push({ index, points: updated });
          }
          return;
        }
      }
    }
  });
  return { deletions, rebinds };
}

/**
 * Wire edits for changing a script block's terminal count (anzXIN /
 * anzYOUT). The anchored block is append-only, so growing changes nothing;
 * shrinking deletes only the wires of the pins that cease to exist.
 */
export function planTerminalCountWireEdits(
  component: Parameters<typeof terminalPositions>[0],
  side: 'x' | 'y',
  newCount: number,
  wires: WireLike[],
): MultiPinWirePlan {
  const before = side === 'x' ? terminalPositions(component).input : terminalPositions(component).output;
  if (newCount >= before.length || before.length <= 1) {
    return EMPTY_PLAN;
  }
  const deadPins: Point[] = before.slice(newCount);
  const deletions: number[] = [];
  wires.forEach((w, index) => {
    const pts = w.points ?? [];
    if (pts.length < 2) return;
    for (const end of [0, pts.length - 1] as const) {
      if (deadPins.some((p) => samePoint(pts[end], p))) {
        deletions.push(index);
        return;
      }
    }
  });
  return { deletions, rebinds: [] };
}

/**
 * Migrates wires of stored circuits from the legacy centered multi-pin
 * layout to the anchored one: a wire endpoint sitting exactly on a legacy
 * pin moves to the same channel's anchored pin, orthogonally. Index-
 * preserving, idempotent (anchored endpoints no longer match legacy pins),
 * and conservative — only endpoints ON legacy pins are touched.
 */
export function normalizeLegacyMultiPinWires(
  components: Parameters<typeof terminalPositions>[0][],
  wires: WireLike[],
): { index: number; points: number[][] }[] {
  const moves: { from: Point; to: Point; siblings: Point[]; center: Point }[] = [];
  const anchoredAll = new Set<string>();
  for (const c of components) {
    const anchored = terminalPositions(c);
    [...anchored.input, ...anchored.output].forEach((p) => anchoredAll.add(`${p.x},${p.y}`));
  }
  for (const c of components) {
    const anchored = terminalPositions(c);
    const legacy = legacyTerminalPositions(c);
    if (legacy.input.length < 2 && legacy.output.length < 2) {
      continue;
    }
    const center = { x: c.position[0], y: c.position[1] };
    const siblings = [...anchored.input, ...anchored.output];
    const addMove = (from: Point, to: Point | undefined) => {
      if (!to || (from.x === to.x && from.y === to.y)) return;
      // Legacy pin j of the centered layout can coincide with anchored
      // pin j-1. A wire endpoint already sitting on a VALID anchored pin
      // is never migrated — that both resolves the ambiguity for the
      // (already migrated) wire and keeps this pass idempotent.
      if (anchoredAll.has(`${from.x},${from.y}`)) return;
      moves.push({ from, to, siblings, center });
    };
    legacy.input.forEach((from, i) => addMove(from, anchored.input[i]));
    legacy.output.forEach((from, i) => addMove(from, anchored.output[i]));
  }
  if (moves.length === 0) {
    return [];
  }

  const edits: { index: number; points: number[][] }[] = [];
  wires.forEach((w, index) => {
    let pts = w.points ?? [];
    if (pts.length < 2) return;
    for (const end of [0, pts.length - 1] as const) {
      const move = moves.find((m) => samePoint(pts[end], m.from));
      if (!move) continue;
      const updated = rebindWireEndpointToPin(
        pts, end, move.to, move.siblings, move.center,
      );
      if (updated !== pts) {
        pts = updated;
      }
    }
    if (pts !== w.points) {
      edits.push({ index, points: pts });
    }
  });
  return edits;
}

/** terminalPositions with an explicit input-pin count override. */
function pinsWithCount(
  component: Parameters<typeof terminalPositions>[0],
  inputCount: number,
): TerminalPositions {
  if (component.type === CTRL_TYPE.SCOPE || component.type === CTRL_TYPE.LEGACY_SCOPE) {
    const labels = Array.from({ length: Math.max(1, inputCount) }, (_, i) =>
      component.inputLabels?.[i] ?? '');
    return terminalPositions({ ...component, inputLabels: labels });
  }
  if (component.type === CTRL_TYPE.SCRIPT || component.type === CTRL_TYPE.LEGACY_JAVA_FUNCTION) {
    return terminalPositions({
      ...component,
      parameters: { ...(component.parameters ?? {}), anzXIN: inputCount },
    });
  }
  return terminalPositions(component);
}
