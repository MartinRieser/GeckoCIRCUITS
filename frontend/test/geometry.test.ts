import { describe, expect, it } from 'vitest';
import {
  nextOrientation,
  terminalPositions,
  terminalNear,
  allTerminals,
} from '../src/model/geometry';

describe('nextOrientation', () => {
  it('cycles in the GUI rotation order', () => {
    expect(nextOrientation(503)).toBe(504); // NORTH_SOUTH -> EAST_WEST
    expect(nextOrientation(504)).toBe(501); // EAST_WEST -> SOUTH_NORTH
    expect(nextOrientation(501)).toBe(502); // SOUTH_NORTH -> WEST_EAST
    expect(nextOrientation(502)).toBe(503); // WEST_EAST -> NORTH_SOUTH
  });
});

describe('terminalPositions', () => {
  const comp = { type: 1, position: [100, 200], orientation: 502 };

  it('places two-port terminals 2 units along the flow direction', () => {
    const t = terminalPositions(comp); // WEST_EAST
    expect(t.input).toEqual([{ x: 98, y: 200 }]);
    expect(t.output).toEqual([{ x: 102, y: 200 }]);
  });

  it('vertical orientation NORTH_SOUTH puts input north', () => {
    const t = terminalPositions({ ...comp, orientation: 503 });
    expect(t.input).toEqual([{ x: 100, y: 198 }]);
    expect(t.output).toEqual([{ x: 100, y: 202 }]);
  });

  it('EAST_WEST flips input and output', () => {
    const t = terminalPositions({ ...comp, orientation: 504 });
    expect(t.input).toEqual([{ x: 102, y: 200 }]);
    expect(t.output).toEqual([{ x: 98, y: 200 }]);
  });
});

describe('terminalNear', () => {
  const components = [
    { type: 1, name: 'R1', family: 'LK', position: [10, 10], orientation: 502, parameters: {}, inputLabels: [], outputLabels: [] },
    { type: 1, name: 'R2', family: 'LK', position: [30, 10], orientation: 502, parameters: {}, inputLabels: [], outputLabels: [] },
  ];

  it('finds a terminal within snap distance', () => {
    const hit = terminalNear(components, { x: 12.3, y: 10.4 });
    expect(hit).not.toBeNull();
    expect(hit!.component).toBe('R1');
    expect(hit!.point).toEqual({ x: 12, y: 10 });
  });

  it('returns null when nothing is near', () => {
    expect(terminalNear(components, { x: 20, y: 20 })).toBeNull();
  });

  it('allTerminals covers both sides of all components', () => {
    const refs = allTerminals(components);
    expect(refs).toHaveLength(4);
    expect(refs.map((r) => r.component).sort()).toEqual(['R1', 'R1', 'R2', 'R2']);
  });
});
