import { describe, expect, it } from 'vitest';
import {
  isWireEndPointConnected,
  validateCircuitForSimulation,
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
      { points: [[16, 9], [16, 8], [20, 8], [20, 12]] },
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
