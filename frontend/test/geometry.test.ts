import { describe, expect, it } from 'vitest';
import {
  nextOrientation,
  terminalPositions,
  terminalNear,
  allTerminals,
  findPlacementConflict,
  flowVector,
  controlFlowVector,
} from '../src/model/geometry';
import { Orientation, LkComponentType, ControlComponentType } from '../src/model/constants';

describe('Orientation Vectors & Cycling', () => {
  it('cycles through standard orientations in clockwise order', () => {
    expect(nextOrientation(Orientation.NORTH_SOUTH)).toBe(Orientation.EAST_WEST);
    expect(nextOrientation(Orientation.EAST_WEST)).toBe(Orientation.SOUTH_NORTH);
    expect(nextOrientation(Orientation.SOUTH_NORTH)).toBe(Orientation.WEST_EAST);
    expect(nextOrientation(Orientation.WEST_EAST)).toBe(Orientation.NORTH_SOUTH);
  });

  it('calculates correct LK flow vectors for all orientations', () => {
    expect(flowVector(Orientation.WEST_EAST)).toEqual({ x: 1, y: 0 });
    expect(flowVector(Orientation.EAST_WEST)).toEqual({ x: -1, y: 0 });
    expect(flowVector(Orientation.NORTH_SOUTH)).toEqual({ x: 0, y: 1 });
    expect(flowVector(Orientation.SOUTH_NORTH)).toEqual({ x: 0, y: -1 });
    expect(flowVector(9999)).toEqual({ x: 0, y: 1 }); // fallback
  });

  it('calculates correct CONTROL flow vectors for all orientations', () => {
    expect(controlFlowVector(Orientation.NORTH_SOUTH)).toEqual({ x: 1, y: 0 });
    expect(controlFlowVector(Orientation.SOUTH_NORTH)).toEqual({ x: -1, y: 0 });
    expect(controlFlowVector(Orientation.WEST_EAST)).toEqual({ x: 0, y: -1 });
    expect(controlFlowVector(Orientation.EAST_WEST)).toEqual({ x: 0, y: 1 });
  });
});

describe('terminalPositions: Standard Two-Port Components', () => {
  const comp = { type: LkComponentType.RESISTOR, position: [100, 200], orientation: Orientation.WEST_EAST };

  it('places two-port terminals 2 units along the flow direction for WEST_EAST', () => {
    const t = terminalPositions(comp);
    expect(t.input).toEqual([{ x: 98, y: 200 }]);
    expect(t.output).toEqual([{ x: 102, y: 200 }]);
  });

  it('places terminals correctly for NORTH_SOUTH', () => {
    const t = terminalPositions({ ...comp, orientation: Orientation.NORTH_SOUTH });
    expect(t.input).toEqual([{ x: 100, y: 198 }]);
    expect(t.output).toEqual([{ x: 100, y: 202 }]);
  });

  it('places terminals correctly for SOUTH_NORTH', () => {
    const t = terminalPositions({ ...comp, orientation: Orientation.SOUTH_NORTH });
    expect(t.input).toEqual([{ x: 100, y: 202 }]);
    expect(t.output).toEqual([{ x: 100, y: 198 }]);
  });

  it('places terminals correctly for EAST_WEST', () => {
    const t = terminalPositions({ ...comp, orientation: Orientation.EAST_WEST });
    expect(t.input).toEqual([{ x: 102, y: 200 }]);
    expect(t.output).toEqual([{ x: 98, y: 200 }]);
  });
});

describe('terminalPositions: Special LK Components (Transformer & BJT)', () => {
  it('places 4 terminals for Ideal Transformer (LkComponentType.TRANSFORMER)', () => {
    const center = [50, 50];
    const trans = {
      type: LkComponentType.TRANSFORMER,
      family: 'LK',
      position: center,
      orientation: Orientation.NORTH_SOUTH,
    };
    const t = terminalPositions(trans);
    expect(t.input).toHaveLength(2);
    expect(t.output).toHaveLength(2);
    // Primary pair: x - 1, secondary pair: x + 1
    expect(t.input).toEqual([
      { x: 49, y: 48 },
      { x: 49, y: 52 },
    ]);
    expect(t.output).toEqual([
      { x: 51, y: 48 },
      { x: 51, y: 52 },
    ]);
  });

  it('places 3 terminals for BJT transistor (collector, base, emitter)', () => {
    const center = [50, 50];
    const bjt = {
      type: LkComponentType.BJT,
      family: 'LK',
      position: center,
      orientation: Orientation.WEST_EAST,
    };
    const t = terminalPositions(bjt);
    expect(t.input).toHaveLength(2); // Collector and Base
    expect(t.output).toHaveLength(1); // Emitter
    // Collector at input lead (-2, 0), Base at (-2, 0) offset, Emitter at (+2, 0)
    expect(t.output[0]).toEqual({ x: 52, y: 50 });
  });
});

describe('terminalPositions: CONTROL blocks', () => {
  const control = { family: 'CONTROL', position: [100, 200], orientation: Orientation.NORTH_SOUTH };

  it('signal source and constant have a single output terminal', () => {
    for (const type of [
      ControlComponentType.LEGACY_CONSTANT,
      ControlComponentType.CONSTANT,
      ControlComponentType.SIGNAL_SOURCE,
    ]) {
      const t = terminalPositions({ ...control, type });
      expect(t.input).toEqual([]);
      expect(t.output.length, `type ${type}`).toBe(1);
      expect(t.output[0]).toEqual({ x: 102, y: 200 });
    }
  });

  it('legacy gate and single-channel scope have a single input terminal', () => {
    for (const type of [
      ControlComponentType.LEGACY_GATE,
      ControlComponentType.LEGACY_SCOPE,
      ControlComponentType.SCOPE,
    ]) {
      const t = terminalPositions({ ...control, type });
      expect(t.input.length, `type ${type}`).toBe(1);
      expect(t.input[0]).toEqual({ x: 98, y: 200 });
      expect(t.output).toEqual([]);
    }
  });

  it('modern gate driver (1000) operates as a standard two-port control element', () => {
    const t = terminalPositions({ ...control, type: ControlComponentType.GATE });
    expect(t.input).toEqual([{ x: 98, y: 200 }]);
    expect(t.output).toEqual([{ x: 102, y: 200 }]);
  });

  it('renders multi-channel scope inputs spaced evenly across the input side', () => {
    const multiScope = {
      ...control,
      type: ControlComponentType.SCOPE,
      inputLabels: ['SIG1', 'SIG2', 'SIG3'],
    };
    const t = terminalPositions(multiScope);
    expect(t.input).toHaveLength(3);
    expect(t.output).toHaveLength(0);
    // Spaced along Y by 2 units
    expect(t.input[0].x).toBe(98);
    expect(t.input[1].x).toBe(98);
    expect(t.input[2].x).toBe(98);
    expect(t.input[1].y - t.input[0].y).toBe(2);
    expect(t.input[2].y - t.input[1].y).toBe(2);
  });

  it('handles Function / Script Block with dynamic N inputs and M outputs', () => {
    const scriptBlock = {
      ...control,
      type: ControlComponentType.SCRIPT,
      parameters: {
        anzXIN: 2,
        anzYOUT: 3,
      },
    };
    const t = terminalPositions(scriptBlock);
    expect(t.input).toHaveLength(2);
    expect(t.output).toHaveLength(3);
  });
});

describe('terminalNear and allTerminals', () => {
  const components = [
    {
      type: LkComponentType.RESISTOR,
      name: 'R1',
      family: 'LK',
      position: [10, 10],
      orientation: Orientation.WEST_EAST,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: LkComponentType.RESISTOR,
      name: 'R2',
      family: 'LK',
      position: [30, 10],
      orientation: Orientation.WEST_EAST,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
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

describe('findPlacementConflict', () => {
  const existing = [
    {
      type: LkComponentType.RESISTOR,
      name: 'R1',
      family: 'LK',
      position: [20, 20],
      orientation: Orientation.WEST_EAST,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
  ];

  it('detects collision when candidate center coincides with existing center', () => {
    const candidate = {
      type: LkComponentType.CAPACITOR,
      position: [20, 20],
      orientation: Orientation.WEST_EAST,
    };
    expect(findPlacementConflict(candidate, existing)).toBe('R1');
  });

  it('detects collision when candidate terminal coincides with existing center', () => {
    // R1 is at (20,20). Candidate at (18, 20) with WEST_EAST has output terminal at (20, 20)
    const candidate = {
      type: LkComponentType.CAPACITOR,
      position: [18, 20],
      orientation: Orientation.WEST_EAST,
    };
    expect(findPlacementConflict(candidate, existing)).toBe('R1');
  });

  it('detects collision when candidate center coincides with existing terminal', () => {
    // R1 output terminal is at (22, 20)
    const candidate = {
      type: LkComponentType.CAPACITOR,
      position: [22, 20],
      orientation: Orientation.WEST_EAST,
    };
    expect(findPlacementConflict(candidate, existing)).toBe('R1');
  });

  it('allows safe placement where terminals connect (pin-to-pin junction)', () => {
    // R1 output is at (22, 20). Candidate at (24, 20) has input terminal at (22, 20)
    // Centers are at 20 and 24, neither center sits on another's terminal!
    const candidate = {
      type: LkComponentType.CAPACITOR,
      position: [24, 20],
      orientation: Orientation.WEST_EAST,
    };
    expect(findPlacementConflict(candidate, existing)).toBeNull();
  });
});
