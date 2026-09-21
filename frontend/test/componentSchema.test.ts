import { describe, it, expect } from 'vitest';
import {
  getComponentMeta,
  parseEngineeringValue,
  formatEngineeringValue,
  COMPONENT_METAS,
  isGateDriver,
  isSwitchComponent,
  getCoupledComponentName,
  extractAvailableSignals,
} from '../src/model/componentSchema';

describe('componentSchema', () => {
  it('defines metadata for all standard component types', () => {
    expect(COMPONENT_METAS[1]).toBeDefined(); // Resistor
    expect(COMPONENT_METAS[1].displayName).toBe('Resistor');
    expect(COMPONENT_METAS[1].category).toBe('passives');

    expect(COMPONENT_METAS[2]).toBeDefined(); // Inductor
    expect(COMPONENT_METAS[3]).toBeDefined(); // Capacitor
    expect(COMPONENT_METAS[4]).toBeDefined(); // Voltage source
    expect(COMPONENT_METAS[6]).toBeDefined(); // Diode
    expect(COMPONENT_METAS[7]).toBeDefined(); // Switch
    expect(COMPONENT_METAS[28]).toBeDefined(); // MOSFET
  });

  it('provides fallback metadata for unknown component types', () => {
    const meta = getComponentMeta(999, 'LK', 'CUSTOM_999');
    expect(meta).toBeDefined();
    expect(meta.type).toBe(999);
    expect(meta.parameters.length).toBeGreaterThan(0);
  });

  describe('parseEngineeringValue', () => {
    it('parses standard integers and decimals', () => {
      expect(parseEngineeringValue('10')).toBe(10);
      expect(parseEngineeringValue('3.14')).toBe(3.14);
      expect(parseEngineeringValue('-5.5')).toBe(-5.5);
      expect(parseEngineeringValue('−5.5')).toBe(-5.5); // Unicode minus U+2212
      expect(parseEngineeringValue('−20m')).toBe(-0.02);
      expect(parseEngineeringValue('1e-3')).toBe(0.001);
    });

    it('round-trips formatted negative values', () => {
      const formatted = formatEngineeringValue(-0.02);
      expect(parseEngineeringValue(formatted)).toBeCloseTo(-0.02);
    });

    it('parses SI prefixes', () => {
      expect(parseEngineeringValue('10k')).toBe(10000);
      expect(parseEngineeringValue('4.7k')).toBe(4700);
      expect(parseEngineeringValue('1M')).toBe(1000000);
      expect(parseEngineeringValue('2.2M')).toBe(2200000);
      expect(parseEngineeringValue('100m')).toBe(0.1);
      expect(parseEngineeringValue('4.7u')).toBeCloseTo(4.7e-6);
      expect(parseEngineeringValue('100n')).toBeCloseTo(1e-7);
      expect(parseEngineeringValue('22p')).toBeCloseTo(22e-12);
      expect(parseEngineeringValue('1G')).toBe(1e9);
    });

    it('handles SI prefixes with unit suffix', () => {
      expect(parseEngineeringValue('10kΩ')).toBe(10000);
      expect(parseEngineeringValue('100uF')).toBeCloseTo(1e-4);
      expect(parseEngineeringValue('24V')).toBe(24);
      expect(parseEngineeringValue('50Hz')).toBe(50);
    });

    it('returns null for invalid inputs', () => {
      expect(parseEngineeringValue('')).toBeNull();
      expect(parseEngineeringValue('abc')).toBeNull();
    });
  });

  describe('formatEngineeringValue', () => {
    it('formats values with appropriate SI prefix', () => {
      expect(formatEngineeringValue(10000)).toBe('10 k');
      expect(formatEngineeringValue(10000, 'Ω')).toBe('10 k Ω');
      expect(formatEngineeringValue(0.001)).toBe('1 m');
      expect(formatEngineeringValue(0.00001, 'F')).toBe('10 µ F');
      expect(formatEngineeringValue(0.0000001, 'F')).toBe('100 n F');
      expect(formatEngineeringValue(24, 'V')).toBe('24 V');
      expect(formatEngineeringValue(0)).toBe('0');
    });
  });

  describe('coupling and signal helpers', () => {
    it('identifies gate drivers and switches', () => {
      expect(isGateDriver({ type: 1000, name: 'GATE.1' })).toBe(true);
      expect(isGateDriver({ type: 6, family: 'CONTROL', name: 'GATE.1' })).toBe(true);
      expect(isGateDriver({ type: 1, name: 'R.1' })).toBe(false);

      expect(isSwitchComponent({ type: 7, name: 'S.1' })).toBe(true);
      expect(isSwitchComponent({ type: 8, name: 'TH.1' })).toBe(true);
      expect(isSwitchComponent({ type: 1, name: 'R.1' })).toBe(false);
    });

    it('extracts coupled component name normalizing leading slash', () => {
      expect(getCoupledComponentName({ parameters: { coupledComponent: '/S.1' } })).toBe('S.1');
      expect(getCoupledComponentName({ parameters: { coupledComponent: 'GATE.1' } })).toBe('GATE.1');
      expect(getCoupledComponentName({ parameters: { coupledComponent: 'none' } })).toBe('');
      expect(getCoupledComponentName(null)).toBe('');
    });

    it('extracts unique available signals from components and wires', () => {
      const components = [
        { name: 'GATE.1', inputLabels: ['gt'], outputLabels: [] },
        { name: 'VOLT.1', type: 1001, inputLabels: [], outputLabels: ['uOUT'] },
        { name: 'AMP.1', type: 1002, inputLabels: [], outputLabels: ['il1'] },
      ];
      const wires = [{ label: 'uIN' }];
      const signals = extractAvailableSignals(components, wires);
      expect(signals).toContain('gt');
      expect(signals).toContain('uOUT');
      expect(signals).toContain('il1');
      expect(signals).toContain('uIN');
      expect(signals).toContain('VOLT.1');
      expect(signals).toContain('AMP.1');
    });
  });

  it('marks motors and unsimulated thermal modules as disabled', () => {
    const disabledTypes = Object.values(COMPONENT_METAS)
      .filter((m) => m.disabled)
      .map((m) => m.type)
      .sort((a, b) => a - b);
    // all motors (types 14-21 except 19 which does not exist, plus 51) and
    // the thermal components without an engine model (41 PvCHIP, 42 MODUL,
    // 48 AMBIENT)
    expect(disabledTypes).toEqual([14, 15, 16, 17, 18, 20, 21, 41, 42, 48, 51]);

    // nothing that the engine actually simulates may be disabled
    const simulated = new Set([1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 23, 24, 26, 28, 33, 44, 45, 46, 47]);
    for (const meta of Object.values(COMPONENT_METAS)) {
      if (meta.disabled) {
        expect(simulated.has(meta.type)).toBe(false);
      }
    }
  });
});

