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
import { CTRL_TYPE } from './componentSchema';

export const ORIENTATION_CYCLE = [503, 504, 501, 502] as const;

export const TWO_PORT_DIST = 2;

/** CONTROL types carrying a single output terminal (constant, signal source). */
const CONTROL_OUTPUT_ONLY = new Set<number>([
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

/**
 * Flow vector of CONTROL blocks. The classic editor maps control terminal
 * offsets through TerminalRelativePosition, so e.g. NORTH_SOUTH places the
 * output at (x+2, y) — horizontal flow, the default drawing direction of
 * control blocks (a quarter turn against the LK two-port vector).
 */
export function controlFlowVector(orientation: number): Point {
  switch (orientation) {
    case 501:
      return { x: -1, y: 0 }; // SOUTH_NORTH
    case 502:
      return { x: 0, y: -1 }; // WEST_EAST
    case 504:
      return { x: 0, y: 1 }; // EAST_WEST
    case 503:
    default:
      return { x: 1, y: 0 }; // NORTH_SOUTH
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
  family?: string;
  position: number[];
  orientation: number;
  inputLabels?: string[];
  inputs?: unknown[];
}): TerminalPositions {
  const center = { x: component.position[0], y: component.position[1] };
  const family = component.family || 'LK';
  const dir = family === 'CONTROL'
    ? controlFlowVector(component.orientation)
    : flowVector(component.orientation);

  if (family === 'CONTROL') {
    // constant & signal source: 0 inputs, 1 output on the output side
    if (CONTROL_OUTPUT_ONLY.has(component.type)) {
      return {
        input: [],
        output: [{ x: center.x + dir.x * TWO_PORT_DIST, y: center.y + dir.y * TWO_PORT_DIST }],
      };
    }
    // Scope: can have multiple inputs!
    if (component.type === CTRL_TYPE.SCOPE || component.type === CTRL_TYPE.LEGACY_SCOPE) {
      const count = Math.max(1, component.inputLabels?.length || component.inputs?.length || 1);
      if (count <= 1) {
        return {
          input: [{ x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST }],
          output: [],
        };
      }
      const inputs: Point[] = [];
      const step = 2; // 2 grid units per input channel
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
      const compParams = (component as any).parameters || {};
      const inCount = Math.max(0, Number(compParams.anzXIN) ?? (component.inputLabels?.length || 1));
      const outCount = Math.max(1, Number(compParams.anzYOUT) || 1);
      const step = 2; // 2 grid units between pins

      const inputs: Point[] = [];
      const inStart = inCount > 1 ? -((inCount - 1) * step) / 2 : 0;
      for (let i = 0; i < inCount; i++) {
        const offset = inStart + i * step;
        inputs.push({
          x: center.x - dir.x * TWO_PORT_DIST - dir.y * offset,
          y: center.y - dir.y * TWO_PORT_DIST + dir.x * offset,
        });
      }

      const outputs: Point[] = [];
      const outStart = outCount > 1 ? -((outCount - 1) * step) / 2 : 0;
      for (let j = 0; j < outCount; j++) {
        const offset = outStart + j * step;
        outputs.push({
          x: center.x + dir.x * TWO_PORT_DIST - dir.y * offset,
          y: center.y + dir.y * TWO_PORT_DIST + dir.x * offset,
        });
      }

      return { input: inputs, output: outputs };
    }

    // gate & other single-input control blocks: 1 input on the input side, 0 outputs
    if (CONTROL_INPUT_ONLY.has(component.type)) {
      return {
        input: [{ x: center.x - dir.x * TWO_PORT_DIST, y: center.y - dir.y * TWO_PORT_DIST }],
        output: [],
      };
    }
  }

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
