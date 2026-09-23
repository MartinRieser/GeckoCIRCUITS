import { describe, expect, it } from 'vitest';
import {
  isWireEndPointConnected,
  validateCircuitForSimulation,
  findWireGeometryWarnings,
  type ComponentLike,
} from '../src/model/validation';

function comp(partial: Partial<ComponentLike> & { name: string; type: number }): ComponentLike {
  return {
    position: [10, 10],
    orientation: 503,
    family: 'LK',
    parameters: {},
    inputLabels: [],
    outputLabels: [],
    ...partial,
  };
}

describe('isWireEndPointConnected', () => {
  const resistor = comp({ name: 'R', type: 1, position: [18, 9], orientation: 504 });

  it('accepts an endpoint on a component terminal', () => {
    expect(isWireEndPointConnected({ x: 16, y: 9 }, [resistor], [])).toBe(true);
    expect(isWireEndPointConnected({ x: 20, y: 9 }, [resistor], [])).toBe(true);
  });

  it('rejects a free floating grid point', () => {
    expect(isWireEndPointConnected({ x: 16, y: 12 }, [resistor], [])).toBe(false);
  });

  it('accepts an endpoint on any raster point of an existing wire', () => {
    const wires = [{ points: [[5, 5], [5, 6], [5, 7]] }];
    expect(isWireEndPointConnected({ x: 5, y: 6 }, [resistor], wires)).toBe(true);
    expect(isWireEndPointConnected({ x: 6, y: 6 }, [resistor], wires)).toBe(false);
  });

  it('accepts control block pins (gate input, scope channel)', () => {
    const scope = comp({ name: 'SCOPE', type: 1003, family: 'CONTROL', position: [30, 20] });
    expect(isWireEndPointConnected({ x: 28, y: 20 }, [scope], [])).toBe(true);
  });
});

describe('validateCircuitForSimulation', () => {
  it('flags an empty sheet', () => {
    const warnings = validateCircuitForSimulation([], []);
    expect(warnings).toHaveLength(1);
    expect(warnings[0]).toMatch(/empty/i);
  });

  it('reports a dangling wire end as an open circuit', () => {
    const resistor = comp({ name: 'R', type: 1, position: [18, 9], orientation: 504 });
    const wires = [{ points: [[16, 9], [16, 10]] }]; // start on R.1, end free
    const warnings = validateCircuitForSimulation([resistor], wires);
    expect(warnings.some((w) => /open circuit/i.test(w))).toBe(true);
    expect(warnings.some((w) => /\(16, 10\)/.test(w))).toBe(true);
  });

  it('is happy with a fully wired sourced loop', () => {
    const source = comp({ name: 'U', type: 4, position: [20, 14] }); // terminals (20,12)/(20,16)
    const resistor = comp({ name: 'R', type: 1, position: [18, 9], orientation: 504 }); // (16,9)/(20,9)
    const wires = [
      { points: [[20, 12], [20, 11], [20, 10], [20, 9]] },
      // approaches the source-top terminal from the right so it does not
      // run along wire 1 (which occupies x=20 between y=9 and y=12)
      { points: [[16, 9], [16, 7], [24, 7], [24, 12], [20, 12]] },
      { points: [[20, 16], [20, 15], [16, 15], [16, 9]] },
    ];
    expect(validateCircuitForSimulation([source, resistor], wires)).toEqual([]);
  });

  it('warns when no source exists', () => {
    const resistor = comp({ name: 'R', type: 1, position: [18, 9], orientation: 504 });
    const warnings = validateCircuitForSimulation([resistor], []);
    expect(warnings.some((w) => /no source/i.test(w))).toBe(true);
  });

  it('warns about components the engine cannot simulate, with counts', () => {
    const source = comp({ name: 'U', type: 4, position: [10, 10] });
    const motor = comp({
      name: 'PMSM',
      type: 15,
      displayName: 'PMSM Motor',
      position: [14, 10],
    });
    const bjt = comp({ name: 'Q', type: 33, displayName: 'BJT Transistor', position: [18, 10] });
    const warnings = validateCircuitForSimulation([source, motor, bjt], []);
    expect(warnings.some((w) => /not yet simulated/i.test(w))).toBe(true);
    expect(warnings.some((w) => w.includes('PMSM Motor'))).toBe(true);
    // the BJT is simulated by the core engine (hidden-subcircuit expansion)
    expect(warnings.some((w) => w.includes('BJT Transistor'))).toBe(false);
  });

  it('does not flag control blocks as unsupported', () => {
    const source = comp({ name: 'U', type: 4, position: [10, 10] });
    const scope = comp({ name: 'SCOPE', type: 1003, family: 'CONTROL', position: [14, 10] });
    expect(validateCircuitForSimulation([source, scope], [])).toEqual([]);
  });
});

describe('findWireGeometryWarnings', () => {
  const resistor = comp({ name: 'R1', type: 1, position: [16, 16], orientation: 502 }); // terminals (14,16)/(18,16)
  const w = (points: number[][]) => ({ points });

  it('flags two wires running along the same cells as an overlap', () => {
    // The moved-wire bug: top wire slid onto the bottom rail row
    const rail = w([[4, 16], [28, 16]]);
    const moved = w([[12, 16], [14, 16]]);
    const warnings = findWireGeometryWarnings([], [rail, moved]);
    expect(warnings).toHaveLength(1);
    expect(warnings[0]).toMatch(/wires 1 and 2 overlap/i);
    expect(warnings[0]).toContain('(12, 16)');
  });

  it('flags a wire passing through a terminal that another wire wires (hidden short)', () => {
    // w2 explicitly ends on R1's left terminal from a free lane; the rail
    // sweeps through that same terminal, silently merging the two nets
    const rail = w([[4, 16], [28, 16]]);
    const wired = w([[14, 10], [14, 16]]);
    const warnings = findWireGeometryWarnings([resistor], [rail, wired]);
    expect(warnings).toHaveLength(1);
    expect(warnings[0]).toMatch(/wire 1 passes through the R1 terminal/i);
    expect(warnings[0]).toMatch(/wire 2 also connects/i);
  });

  it('stays silent for the classic rail-through-terminal hookup with no endpoint wire', () => {
    // A bottom rail collecting terminals is a legitimate idiom: it only
    // connects pins nothing else wires.
    const rail = w([[4, 16], [28, 16]]);
    const top = w([[4, 10], [28, 10]]);
    expect(findWireGeometryWarnings([resistor], [rail, top])).toEqual([]);
  });

  it('stays silent for a plain crossing of two wires', () => {
    const horizontal = w([[2, 10], [20, 10]]);
    const vertical = w([[10, 4], [10, 18]]);
    expect(findWireGeometryWarnings([], [horizontal, vertical])).toEqual([]);
  });

  it('is surfaced by the pre-run validation', () => {
    const rail = w([[4, 16], [28, 16]]);
    const moved = w([[12, 16], [14, 16]]);
    const source = comp({ name: 'U', type: 4, position: [40, 16] });
    const warnings = validateCircuitForSimulation([source], [rail, moved]);
    expect(warnings.some((x) => /overlap/i.test(x))).toBe(true);
  });

  it('tolerates duplicate consecutive wire points without hanging (crash regression)', () => {
    const dup = w([[5, 5], [5, 5], [9, 5], [9, 5]]);
    expect(() => findWireGeometryWarnings([], [dup, w([[5, 7], [9, 7]])])).not.toThrow();
  });
});
