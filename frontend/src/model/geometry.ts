/**
 * Terminal geometry: where a component's terminals sit on the grid,
 * derived from its type and orientation.
 *
 * .ipes semantics (matching the Swing editor):
 * - coordinates are integer grid units; dpix is only the pixel render scale
 * - orientation codes: 501 SOUTH_NORTH, 502 WEST_EAST, 503 NORTH_SOUTH,
 *   504 EAST_WEST
 * - two-port components have their input terminal 2 units "against" the
 *   flow direction and the output terminal 2 units along it
 *   (TWO_PORT_DIST = 2 in AbstractTwoPortLKreisBlock)
 * - rotation cycle (right-click / 'r'): 503 -> 504 -> 501 -> 502
 */
import type { EditorComponent, Point } from './types';

export const ORIENTATION_CYCLE = [503, 504, 501, 502] as const;

export const TWO_PORT_DIST = 2;

export function nextOrientation(orientation: number): number {
  const i = ORIENTATION_CYCLE.indexOf(orientation as (typeof ORIENTATION_CYCLE)[number]);
  return ORIENTATION_CYCLE[(i + 1) % ORIENTATION_CYCLE.length];
}

/** Unit flow-direction vector of an orientation (from input to output). */
export function flowVector(orientation: number): Point {
  switch (orientation) {
    case 502:
      return { x: 1, y: 0 }; // WEST_EAST
    case 504:
      return { x: -1, y: 0 }; // EAST_WEST
    case 503:
      return { x: 0, y: 1 }; // NORTH_SOUTH
    case 501:
      return { x: 0, y: -1 }; // SOUTH_NORTH
    default:
      return { x: 0, y: 1 };
  }
}

export interface TerminalPositions {
  input: Point[];
  output: Point[];
}

/**
 * Terminal positions of a component in grid units. Two-port geometry for
 * all LK/thermal standard components; unknown types fall back to it too
 * (motors/transformers get refined geometry in a later phase).
 */
export function terminalPositions(component: {
  type: number;
  position: number[];
  orientation: number;
}): TerminalPositions {
  const center = { x: component.position[0], y: component.position[1] };
  const dir = flowVector(component.orientation);
  return {
    input: [{ x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST }],
    output: [{ x: center.x + dir.x * TWO_PORT_DIST, y: center.y + dir.y * TWO_PORT_DIST }],
  };
}

/** All terminals of all components as a flat, point-addressable list. */
export interface TerminalRef {
  component: string;
  side: 'x' | 'y';
  index: number;
  point: Point;
}

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

/** Terminal within snap distance (default 0.75 grid units) of a point. */
export function terminalNear(
  components: EditorComponent[],
  point: Point,
  maxDistance = 0.75,
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
