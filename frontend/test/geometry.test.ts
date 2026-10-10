import { describe, expect, it } from 'vitest';
import {
  nextOrientation,
  terminalPositions,
  legacyTerminalPositions,
  terminalNear,
  allTerminals,
  findPlacementConflict,
  flowVector,
  controlFlowVector,
  rebindWireEndpointOrthogonally,
  rebindWireEndpointToPin,
  anchoredPinOffsets,
  legacyCenteredPinOffsets,
  planChannelRemovalWireEdits,
  planTerminalCountWireEdits,
  normalizeLegacyMultiPinWires,
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

  it('returns empty terminals for Mutual Inductance coupler (LkComponentType.MUTUAL_INDUCTANCE)', () => {
    const coupler = {
      type: LkComponentType.MUTUAL_INDUCTANCE,
      family: 'LK',
      position: [50, 50],
      orientation: Orientation.NORTH_SOUTH,
    };
    const t = terminalPositions(coupler);
    expect(t.input).toEqual([]);
    expect(t.output).toEqual([]);
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

  it('gate driver (legacy and modern) and single-channel scope have a single input terminal', () => {
    for (const type of [
      ControlComponentType.LEGACY_GATE,
      ControlComponentType.GATE,
      ControlComponentType.LEGACY_SCOPE,
      ControlComponentType.SCOPE,
    ]) {
      const t = terminalPositions({ ...control, type });
      expect(t.input.length, `type ${type}`).toBe(1);
      expect(t.input[0]).toEqual({ x: 98, y: 200 });
      expect(t.output).toEqual([]);
    }
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

  it('handles Comparator (1010) with 2 input pins spaced at unit step matching simulation engine', () => {
    // Comparator at (53, 29) in NORTH_SOUTH orientation
    const comp = {
      ...control,
      type: 1010,
      position: [53, 29],
      orientation: Orientation.NORTH_SOUTH,
    };
    const t = terminalPositions(comp);
    expect(t.input).toEqual([
      { x: 51, y: 29 }, // (+) input 0 at (x-2, y)
      { x: 51, y: 30 }, // (-) input 1 at (x-2, y+1)
    ]);
    expect(t.output).toEqual([
      { x: 55, y: 29 }, // output at (x+2, y)
    ]);
  });

  it('handles AND Gate (1011) and MUX (1014) with correct multi-pin terminal layout', () => {
    const andGate = { ...control, type: 1011, position: [50, 50], orientation: Orientation.NORTH_SOUTH };
    const tAnd = terminalPositions(andGate);
    expect(tAnd.input).toEqual([
      { x: 48, y: 50 },
      { x: 48, y: 51 },
    ]);
    expect(tAnd.output).toEqual([{ x: 52, y: 50 }]);

    const mux = { ...control, type: 1014, position: [50, 50], orientation: Orientation.NORTH_SOUTH };
    const tMux = terminalPositions(mux);
    expect(tMux.input).toHaveLength(3);
    expect(tMux.input).toEqual([
      { x: 48, y: 50 },
      { x: 48, y: 51 },
      { x: 48, y: 52 },
    ]);
    expect(tMux.output).toEqual([{ x: 52, y: 50 }]);
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

  it('respects custom maxDistance thresholds (tighter and wider)', () => {
    // Distance from (12.3, 10.4) to (12, 10) is sqrt(0.09 + 0.16) = 0.5
    // With tight maxDistance = 0.4 -> should be null
    expect(terminalNear(components, { x: 12.3, y: 10.4 }, 0.4)).toBeNull();

    // With wide maxDistance = 0.6 -> should hit R1
    const hit = terminalNear(components, { x: 12.3, y: 10.4 }, 0.6);
    expect(hit).not.toBeNull();
    expect(hit!.component).toBe('R1');
  });

  it('selects the strictly closer terminal when multiple candidates are in range', () => {
    // Terminals of R1 are at (8, 10) and (12, 10).
    // Test point at (10.1, 10):
    // Distance to (12, 10) is 1.9, distance to (8, 10) is 2.1. Both within maxDistance = 3.0.
    const hitCloserRight = terminalNear(components, { x: 10.1, y: 10 }, 3.0);
    expect(hitCloserRight).not.toBeNull();
    expect(hitCloserRight!.point).toEqual({ x: 12, y: 10 });

    // Test point at (9.9, 10):
    // Distance to (8, 10) is 1.9, distance to (12, 10) is 2.1.
    const hitCloserLeft = terminalNear(components, { x: 9.9, y: 10 }, 3.0);
    expect(hitCloserLeft).not.toBeNull();
    expect(hitCloserLeft!.point).toEqual({ x: 8, y: 10 });
  });

  it('correctly handles boundary conditions around maxDistance', () => {
    // Terminal at (12, 10). Point at (12, 10.5) has exact distance 0.5.
    const exactHit = terminalNear(components, { x: 12, y: 10.5 }, 0.5);
    expect(exactHit).not.toBeNull();
    expect(exactHit!.point).toEqual({ x: 12, y: 10 });

    // Point at (12, 10.501) exceeds distance 0.5
    const outside = terminalNear(components, { x: 12, y: 10.501 }, 0.5);
    expect(outside).toBeNull();
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

describe('rebindWireEndpointOrthogonally (anchored pin-block wire re-bind)', () => {
  it('returns the same array when the endpoint already sits on the new pin', () => {
    const pts = [
      [10, 5],
      [14, 5],
    ];
    expect(rebindWireEndpointOrthogonally(pts, pts.length - 1, [14, 5])).toBe(pts);
  });

  it('jogs at the neighbor column, never on the pin column (horizontal approach)', () => {
    // Removing a scope channel shifts the pins below it up one slot. The
    // wire approached the pin horizontally; the vertical jog must happen on
    // the APPROACH column (x=10), because a jog on the pin column (x=14)
    // would run along the pin block and electrically touch other pins.
    const pts = [
      [10, 5],
      [14, 5],
    ];
    expect(rebindWireEndpointOrthogonally(pts, 1, [14, 4])).toEqual([
      [10, 5],
      [10, 4],
      [14, 4],
    ]);
  });

  it('keeps a straight swap when the neighbor already shares an axis with the new pin', () => {
    const pts = [
      [14, 8],
      [14, 5],
    ];
    expect(rebindWireEndpointOrthogonally(pts, 1, [14, 4])).toEqual([
      [14, 8],
      [14, 4],
    ]);
  });

  it('rebounds the START endpoint with the corner in the right order', () => {
    const pts = [
      [14, 5],
      [10, 5],
    ];
    expect(rebindWireEndpointOrthogonally(pts, 0, [14, 4])).toEqual([
      [14, 4],
      [10, 4],
      [10, 5],
    ]);
  });

  it('preserves interior points of longer wires', () => {
    const pts = [
      [10, 9],
      [14, 9],
      [14, 5],
    ];
    expect(rebindWireEndpointOrthogonally(pts, 2, [14, 4])).toEqual([
      [10, 9],
      [14, 9],
      [14, 4],
    ]);
  });

  it('inserts the corner for a long horizontal approach across intermediate points', () => {
    // Fan-out wires typically run several points horizontally into a scope
    // pin; the last segment is horizontal, the pin shifts up one row. The
    // jog turns at the last intermediate point, then runs clean into the pin.
    const pts = [
      [12, 21],
      [14, 21],
      [18, 21],
      [36, 21],
    ];
    expect(rebindWireEndpointOrthogonally(pts, 3, [36, 20])).toEqual([
      [12, 21],
      [14, 21],
      [18, 21],
      [18, 20],
      [36, 20],
    ]);
  });

  it('repairs an already-diagonal final segment instead of keeping it diagonal', () => {
    const pts = [
      [10, 9],
      [14, 5],
    ];
    expect(rebindWireEndpointOrthogonally(pts, 1, [14, 4])).toEqual([
      [10, 9],
      [14, 9],
      [14, 4],
    ]);
  });

  it('inserts a clean corner when the endpoint moves sideways off a vertical run', () => {
    const pts = [
      [14, 5],
      [14, 8],
    ];
    expect(rebindWireEndpointOrthogonally(pts, 1, [10, 8])).toEqual([
      [14, 5],
      [10, 5],
      [10, 8],
    ]);
  });
});

describe('anchored multi-pin layout (append-only channel blocks)', () => {
  it('anchoredPinOffsets starts at 0 and steps by the given step', () => {
    expect(anchoredPinOffsets(1, 2)).toEqual([0]);
    expect(anchoredPinOffsets(3, 2)).toEqual([0, 2, 4]);
    expect(anchoredPinOffsets(3, 1)).toEqual([0, 1, 2]);
    expect(anchoredPinOffsets(0, 2)).toEqual([]);
  });

  it('legacyCenteredPinOffsets mirrors the pre-anchoring symmetric block', () => {
    expect(legacyCenteredPinOffsets(1, 2)).toEqual([0]);
    expect(legacyCenteredPinOffsets(2, 2)).toEqual([-1, 1]);
    expect(legacyCenteredPinOffsets(3, 2)).toEqual([-2, 0, 2]);
  });

  it('adding a scope channel never moves an existing pin (the original wire-corruption bug)', () => {
    const base = {
      family: 'CONTROL',
      type: ControlComponentType.SCOPE,
      position: [30, 20],
      orientation: Orientation.NORTH_SOUTH,
    };
    const pinsFor = (n: number) =>
      terminalPositions({ ...base, inputLabels: Array.from({ length: n }, (_, i) => `s${i}`) }).input;
    for (let n = 1; n <= 5; n++) {
      const fewer = pinsFor(n);
      const more = pinsFor(n + 1);
      // every pin of the smaller block keeps its exact position
      fewer.forEach((p, i) => {
        expect(more[i], `pin ${i} at count ${n} -> ${n + 1}`).toEqual(p);
      });
    }
    // channel 1 sits on the anchor row, channels append downward
    expect(pinsFor(3)).toEqual([
      { x: 28, y: 20 },
      { x: 28, y: 22 },
      { x: 28, y: 24 },
    ]);
  });

  it('script pins coincide with the simulation core terminal grid in every orientation', () => {
    // Engine (ControlCalculatorBuilder.terminalPoint): input i at rel
    // (-2, -i), output j at rel (+2, -j), rotated by orientation.
    const cases: { ori: number; inPin: (x: number, y: number, i: number) => { x: number; y: number }; outPin: (x: number, y: number, j: number) => { x: number; y: number } }[] = [
      {
        ori: Orientation.NORTH_SOUTH,
        inPin: (x, y, i) => ({ x: x - 2, y: y + i }),
        outPin: (x, y, j) => ({ x: x + 2, y: y + j }),
      },
      {
        ori: Orientation.SOUTH_NORTH,
        inPin: (x, y, i) => ({ x: x + 2, y: y - i }),
        outPin: (x, y, j) => ({ x: x - 2, y: y - j }),
      },
      {
        ori: Orientation.WEST_EAST,
        inPin: (x, y, i) => ({ x: x + i, y: y + 2 }),
        outPin: (x, y, j) => ({ x: x + j, y: y - 2 }),
      },
      {
        ori: Orientation.EAST_WEST,
        inPin: (x, y, i) => ({ x: x - i, y: y - 2 }),
        outPin: (x, y, j) => ({ x: x - j, y: y + 2 }),
      },
    ];
    for (const { ori, inPin, outPin } of cases) {
      const t = terminalPositions({
        family: 'CONTROL',
        type: ControlComponentType.SCRIPT,
        position: [40, 30],
        orientation: ori,
        parameters: { anzXIN: 3, anzYOUT: 2 },
      });
      expect(t.input).toEqual([inPin(40, 30, 0), inPin(40, 30, 1), inPin(40, 30, 2)]);
      expect(t.output).toEqual([outPin(40, 30, 0), outPin(40, 30, 1)]);
    }
  });

  it('planChannelRemovalWireEdits deletes the removed channel wire and shifts the ones below', () => {
    const scope = {
      family: 'CONTROL',
      type: ControlComponentType.SCOPE,
      position: [30, 20],
      orientation: Orientation.NORTH_SOUTH,
      inputLabels: ['a', 'b', 'c'],
    };
    // before-pins: (28,20) (28,22) (28,24); wires approach horizontally
    const wires = [
      { points: [[10, 20], [27, 20], [28, 20]] }, // channel 1
      { points: [[10, 22], [27, 22], [28, 22]] }, // channel 2 (removed)
      { points: [[10, 24], [27, 24], [28, 24]] }, // channel 3
    ];
    const plan = planChannelRemovalWireEdits(scope, 1, wires);
    expect(plan.deletions).toEqual([1]);
    expect(plan.rebinds).toHaveLength(1);
    expect(plan.rebinds[0].index).toBe(2);
    // channel 3's wire now ends on the new channel-2 pin (28,22),
    // approaching orthogonally without touching any other pin
    const pts = plan.rebinds[0].points;
    expect(pts[pts.length - 1]).toEqual([28, 22]);
    const touched = new Set(pts.map((p) => `${p[0]},${p[1]}`));
    expect(touched.has('28,20')).toBe(false);
    expect(touched.has('28,24')).toBe(false);
  });

  it('planTerminalCountWireEdits only deletes wires of the pins that cease to exist', () => {
    const script = {
      family: 'CONTROL',
      type: ControlComponentType.SCRIPT,
      position: [20, 10],
      orientation: Orientation.NORTH_SOUTH,
      parameters: { anzXIN: 3, anzYOUT: 1 },
    };
    const wires = [
      { points: [[12, 10], [17, 10], [18, 10]] }, // input 1
      { points: [[12, 12], [17, 12], [18, 12]] }, // input 3 (dies when 3 -> 2)
    ];
    const shrink = planTerminalCountWireEdits(script, 'x', 2, wires);
    expect(shrink.deletions).toEqual([1]);
    expect(shrink.rebinds).toEqual([]);
    const grow = planTerminalCountWireEdits(script, 'x', 4, wires);
    expect(grow.deletions).toEqual([]);
    expect(grow.rebinds).toEqual([]);
  });

  it('normalizeLegacyMultiPinWires rebinds legacy centered endpoints and is idempotent', () => {
    const scope = {
      family: 'CONTROL',
      type: ControlComponentType.SCOPE,
      position: [36, 20],
      orientation: Orientation.NORTH_SOUTH,
      inputLabels: ['a', 'b', 'c'],
    };
    // legacy pins: (34,18) (34,20) (34,22) -> anchored: (34,20) (34,22) (34,24)
    const wires = [{ points: [[20, 18], [33, 18], [34, 18]] }];
    const edits = normalizeLegacyMultiPinWires([scope], wires);
    expect(edits).toHaveLength(1);
    const pts = edits[0].points;
    expect(pts[pts.length - 1]).toEqual([34, 20]);
    // no diagonal segments and no crossing of the other new pins
    const touched = new Set(pts.map((p) => `${p[0]},${p[1]}`));
    expect(touched.has('34,22')).toBe(false);
    // second pass over the migrated geometry is a no-op
    expect(normalizeLegacyMultiPinWires([scope], [{ points: pts }])).toEqual([]);
  });

  it('legacyTerminalPositions reproduces the centered layout for n>=2', () => {
    const scope = {
      family: 'CONTROL',
      type: ControlComponentType.SCOPE,
      position: [36, 20],
      orientation: Orientation.NORTH_SOUTH,
      inputLabels: ['a', 'b', 'c'],
    };
    expect(legacyTerminalPositions(scope).input.map((p) => [p.x, p.y])).toEqual([
      [34, 18],
      [34, 20],
      [34, 22],
    ]);
  });
});

describe('rebindWireEndpointToPin (no wire may touch a foreign pin)', () => {
  // Scope at (36,22): anchored pins (34,22) (34,24) (34,26) (34,28)
  const pins = [
    { x: 34, y: 22 },
    { x: 34, y: 24 },
    { x: 34, y: 26 },
    { x: 34, y: 28 },
  ];
  const center = { x: 36, y: 25 };

  const foreignTouches = (pts: number[][]) => {
    const cells = new Set<string>();
    cells.add(`${pts[0][0]},${pts[0][1]}`);
    for (let i = 1; i < pts.length; i++) {
      let x = pts[i - 1][0];
      let y = pts[i - 1][1];
      const tx = pts[i][0];
      const ty = pts[i][1];
      while (x !== tx || y !== ty) {
        if (x !== tx) x += Math.sign(tx - x);
        else y += Math.sign(ty - y);
        cells.add(`${x},${y}`);
      }
    }
    return pins.filter((p) => cells.has(`${p.x},${p.y}`));
  };

  it('keeps the generic orthogonal result when it crosses no pin', () => {
    const pts = [
      [26, 26],
      [30, 26],
      [33, 26],
      [34, 26],
    ];
    const out = rebindWireEndpointToPin(pts, pts.length - 1, { x: 34, y: 24 }, pins, center);
    expect(out).toEqual([
      [26, 26],
      [30, 26],
      [33, 26],
      [33, 24],
      [34, 24],
    ]);
    expect(foreignTouches(out)).toEqual([{ x: 34, y: 24 }]);
  });

  it('rebuilds the tail off the pin column when a vertical approach would cross sibling pins', () => {
    // Wire approached down the pin column: [...,(34,18),(34,26)] — moving the
    // endpoint to (34,24) must not leave the run crossing (34,22).
    const pts = [
      [26, 18],
      [33, 18],
      [34, 18],
      [34, 26],
    ];
    const out = rebindWireEndpointToPin(pts, pts.length - 1, { x: 34, y: 24 }, pins, center);
    expect(out[out.length - 1]).toEqual([34, 24]);
    expect(foreignTouches(out)).toEqual([{ x: 34, y: 24 }]);
    // strictly orthogonal
    for (let i = 0; i < out.length - 1; i++) {
      expect(out[i][0] === out[i + 1][0] || out[i][1] === out[i + 1][1]).toBe(true);
    }
  });

  it('handles a dirty tail that retraces on the pin column (the false-Wired repro)', () => {
    // Stored shape seen in the wild: ...(33,26),(34,26),(34,25),(34,26).
    // Re-binding to (34,24) must produce a clean off-column approach.
    const pts = [
      [26, 26],
      [30, 26],
      [33, 26],
      [34, 26],
      [34, 25],
      [34, 26],
    ];
    const out = rebindWireEndpointToPin(pts, pts.length - 1, { x: 34, y: 24 }, pins, center);
    expect(out[out.length - 1]).toEqual([34, 24]);
    expect(foreignTouches(out)).toEqual([{ x: 34, y: 24 }]);
    // the vertical jog happens on the entry column (33), never on x=34
    expect(out.some((p, i) => i > 0 && p[0] === 34 && out[i - 1][0] === 34 && p[1] !== out[i - 1][1] && p[1] !== 24)).toBe(false);
  });

  it('planChannelRemovalWireEdits produces pin-clean rebinds for a dirty vertical tail', () => {
    const scope = {
      family: 'CONTROL',
      type: ControlComponentType.SCOPE,
      position: [36, 22],
      orientation: Orientation.NORTH_SOUTH,
      inputLabels: ['a', 'b', 'c'],
    };
    // channel 2's wire approaches down the pin column from above
    const wires = [
      { points: [[10, 20], [33, 20], [34, 22]] },
      { points: [[10, 18], [33, 18], [34, 18], [34, 24]] },
    ];
    const plan = planChannelRemovalWireEdits(scope, 0, wires);
    expect(plan.deletions).toEqual([0]);
    expect(plan.rebinds).toHaveLength(1);
    const pts = plan.rebinds[0].points;
    expect(pts[pts.length - 1]).toEqual([34, 22]); // new channel-1 pin
    // new pin set after removal: (34,22) (34,24) — the path may touch (34,22) only
    const cells = new Set(pts.map((p) => `${p[0]},${p[1]}`));
    expect(cells.has('34,24')).toBe(false);
  });
});
