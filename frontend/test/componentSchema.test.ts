import { describe, it, expect } from 'vitest';
import {
  getComponentMeta,
  parseEngineeringValue,
  formatEngineeringValue,
  COMPONENT_METAS,
  isGateDriver,
  isSwitchComponent,
  isAmmeterComponent,
  isVoltmeterComponent,
  getCoupledComponentName,
  extractAvailableSignals,
  resolveComponentPinCounts,
} from '../src/model/componentSchema';
import {
  LkComponentType,
  ControlComponentType,
  SENTINEL_UNSET,
} from '../src/model/constants';

describe('componentSchema', () => {
  it('defines metadata for all standard component types', () => {
    expect(COMPONENT_METAS[LkComponentType.RESISTOR]).toBeDefined();
    expect(COMPONENT_METAS[LkComponentType.RESISTOR].displayName).toBe('Resistor');
    expect(COMPONENT_METAS[LkComponentType.RESISTOR].category).toBe('passives');

    expect(COMPONENT_METAS[LkComponentType.INDUCTOR]).toBeDefined();
    expect(COMPONENT_METAS[LkComponentType.CAPACITOR]).toBeDefined();
    expect(COMPONENT_METAS[LkComponentType.VOLTAGE_SOURCE]).toBeDefined();
    expect(COMPONENT_METAS[LkComponentType.DIODE]).toBeDefined();
    expect(COMPONENT_METAS[LkComponentType.IDEAL_SWITCH]).toBeDefined();
    expect(COMPONENT_METAS[LkComponentType.MOSFET]).toBeDefined();
  });

  it('provides fallback metadata for unknown component types', () => {
    const meta = getComponentMeta(999, 'LK', 'CUSTOM_999');
    expect(meta).toBeDefined();
    expect(meta.type).toBe(999);
    expect(meta.parameters.length).toBeGreaterThan(0);
  });

  describe('resolveComponentPinCounts', () => {
    it('resolves defaults for components without explicit pin counts', () => {
      const pins = resolveComponentPinCounts({});
      expect(pins.inputCount).toBe(1);
      expect(pins.outputCount).toBe(1);
    });

    it('resolves pins from inputLabels and outputLabels', () => {
      const pins = resolveComponentPinCounts({
        inputLabels: ['IN1', 'IN2', 'IN3'],
        outputLabels: ['OUT1', 'OUT2'],
      });
      expect(pins.inputCount).toBe(3);
      expect(pins.outputCount).toBe(2);
    });

    it('prefers anzXIN and anzYOUT parameters when present', () => {
      const pins = resolveComponentPinCounts({
        parameters: { anzXIN: 4, anzYOUT: 2 },
        inputLabels: ['L1'],
        outputLabels: ['L2'],
      });
      expect(pins.inputCount).toBe(4);
      expect(pins.outputCount).toBe(2);
    });

    it('safely handles zero or negative pin counts', () => {
      const pins = resolveComponentPinCounts({
        parameters: { anzXIN: 0, anzYOUT: -1 },
      });
      expect(pins.inputCount).toBe(0);
      expect(pins.outputCount).toBe(1); // Min 1 output
    });
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
      expect(parseEngineeringValue('20%')).toBeCloseTo(0.2);
      expect(parseEngineeringValue('50%')).toBeCloseTo(0.5);
      expect(parseEngineeringValue('0.5%')).toBeCloseTo(0.005);
      expect(parseEngineeringValue('100%')).toBe(1.0);
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
      expect(isGateDriver({ type: ControlComponentType.GATE, name: 'GATE.1' })).toBe(true);
      expect(isGateDriver({ type: ControlComponentType.LEGACY_GATE, family: 'CONTROL', name: 'GATE.1' })).toBe(true);
      expect(isGateDriver({ type: LkComponentType.RESISTOR, name: 'R.1' })).toBe(false);

      expect(isSwitchComponent({ type: LkComponentType.IDEAL_SWITCH, name: 'S.1' })).toBe(true);
      expect(isSwitchComponent({ type: LkComponentType.THYRISTOR, name: 'TH.1' })).toBe(true);
      expect(isSwitchComponent({ type: LkComponentType.IGBT, name: 'IGBT.1' })).toBe(true);
      expect(isSwitchComponent({ type: LkComponentType.MOSFET, name: 'MOS.1' })).toBe(true);
      expect(isSwitchComponent({ type: LkComponentType.BJT, name: 'BJT.1' })).toBe(true);
      expect(isSwitchComponent({ type: LkComponentType.RESISTOR, name: 'R.1' })).toBe(false);
    });

    it('identifies ammeters and voltmeters', () => {
      expect(isAmmeterComponent({ type: ControlComponentType.AMMETER, name: 'AMP.1' })).toBe(true);
      expect(isAmmeterComponent({ type: ControlComponentType.LEGACY_AMMETER, family: 'CONTROL' })).toBe(true);
      expect(isAmmeterComponent({ type: LkComponentType.RESISTOR, name: 'R.1' })).toBe(false);

      expect(isVoltmeterComponent({ type: ControlComponentType.VOLTMETER, name: 'VOLT.1' })).toBe(true);
      expect(isVoltmeterComponent({ type: ControlComponentType.LEGACY_VOLTMETER, family: 'CONTROL' })).toBe(true);
      expect(isVoltmeterComponent({ type: LkComponentType.RESISTOR, name: 'R.1' })).toBe(false);
    });

    it('extracts coupled component name normalizing leading slash and ignoring sentinel values', () => {
      expect(getCoupledComponentName({ parameters: { coupledComponent: '/S.1' } })).toBe('S.1');
      expect(getCoupledComponentName({ parameters: { coupledComponent: 'GATE.1' } })).toBe('GATE.1');
      expect(getCoupledComponentName({ parameters: { coupledComponent: 'none' } })).toBe('');
      expect(getCoupledComponentName({ parameters: { coupledComponent: SENTINEL_UNSET } })).toBe('');
      expect(getCoupledComponentName(null)).toBe('');
    });

    it('extracts unique available signals from components and wires ignoring SENTINEL_UNSET', () => {
      const components = [
        { name: 'GATE.1', inputLabels: ['gt', SENTINEL_UNSET], outputLabels: [] },
        { name: 'VOLT.1', type: ControlComponentType.VOLTMETER, inputLabels: [], outputLabels: ['uOUT'] },
        { name: 'AMP.1', type: ControlComponentType.AMMETER, inputLabels: [], outputLabels: ['il1'] },
      ];
      const wires = [{ label: 'uIN' }, { label: SENTINEL_UNSET }];
      const signals = extractAvailableSignals(components, wires);
      expect(signals).toContain('gt');
      expect(signals).toContain('uOUT');
      expect(signals).toContain('il1');
      expect(signals).toContain('uIN');
      expect(signals).not.toContain(SENTINEL_UNSET);
    });

    it('offers a measurement component name only when it has no output label', () => {
      const components = [
        { name: 'VOLT.1', type: ControlComponentType.VOLTMETER, inputLabels: [], outputLabels: ['uOUT'] },
        { name: 'VOLT.2', type: ControlComponentType.VOLTMETER, inputLabels: [], outputLabels: [] },
      ];
      const signals = extractAvailableSignals(components, []);
      expect(signals).not.toContain('VOLT.1');
      expect(signals).toContain('VOLT.2');
    });
  });

  it('marks motors and unsimulated thermal modules as disabled', () => {
    const disabledTypes = Object.values(COMPONENT_METAS)
      .filter((m) => m.disabled)
      .map((m) => m.type)
      .sort((a, b) => a - b);
    expect(disabledTypes).toEqual([14, 15, 16, 17, 18, 20, 21, 41, 42, 48, 51]);

    const simulated = new Set([1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 23, 24, 26, 28, 33, 44, 45, 46, 47]);
    for (const meta of Object.values(COMPONENT_METAS)) {
      if (meta.disabled) {
        expect(simulated.has(meta.type)).toBe(false);
      }
    }
  });
});
