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

    it('keeps dimensionless ratios 0.1..1 as plain decimals (k, duty)', () => {
      expect(formatEngineeringValue(0.999)).toBe('0.999');
      expect(formatEngineeringValue(0.98)).toBe('0.98');
      expect(formatEngineeringValue(0.4)).toBe('0.4');
      expect(formatEngineeringValue(0.05)).toBe('50 m');
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
    expect(disabledTypes).toEqual([14, 15, 16, 17, 18, 20, 21, 25, 41, 42, 48, 51, 52]);

    const simulated = new Set([1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 23, 24, 26, 28, 33, 44, 45, 46, 47]);
    for (const meta of Object.values(COMPONENT_METAS)) {
      if (meta.disabled) {
        expect(simulated.has(meta.type)).toBe(false);
      }
    }
  });

  describe('Control Blocks 1017..1037', () => {
    it('defines metadata for all 21 restored control blocks', () => {
      const types = [
        ControlComponentType.SUB,
        ControlComponentType.ADD,
        ControlComponentType.MUL,
        ControlComponentType.DIV,
        ControlComponentType.LIMIT,
        ControlComponentType.ABS,
        ControlComponentType.SQRT,
        ControlComponentType.EXP,
        ControlComponentType.LN,
        ControlComponentType.SIN,
        ControlComponentType.COS,
        ControlComponentType.MIN,
        ControlComponentType.MAX,
        ControlComponentType.HYS,
        ControlComponentType.PT2,
        ControlComponentType.PD,
        ControlComponentType.SAMPLEHOLD,
        ControlComponentType.TIME,
        ControlComponentType.XOR,
        ControlComponentType.GE,
        ControlComponentType.DEADTIME,
        ControlComponentType.ASIN,
        ControlComponentType.ACOS,
        ControlComponentType.TAN,
        ControlComponentType.ATAN,
        ControlComponentType.SQR,
        ControlComponentType.POW,
        ControlComponentType.ROUND,
        ControlComponentType.SIGN,
        ControlComponentType.EQ,
        ControlComponentType.NE,
        ControlComponentType.COUNTER,
        ControlComponentType.ABCDQ,
        ControlComponentType.DQABC,
        ControlComponentType.THYR_CTRL,
        ControlComponentType.PMSM_CONTROL,
        ControlComponentType.PMSM_MODULATOR,
        ControlComponentType.DEMUX,
        ControlComponentType.SPACE_VECTOR,
        ControlComponentType.SDFT,
        ControlComponentType.SPARSEMATRIX,
      ];

      for (const t of types) {
        const meta = COMPONENT_METAS[t];
        expect(meta, `Metadata for type ${t} should exist`).toBeDefined();
        expect(meta.family).toBe('CONTROL');
        expect(meta.displayName.length).toBeGreaterThan(0);
        expect(['control', 'logic']).toContain(meta.category);
      }
    });

    it('configures SUB block with 2 inputs (+, −) for error calculation and 1 output', () => {
      const meta = COMPONENT_METAS[ControlComponentType.SUB];
      expect(meta.terminals.input).toHaveLength(2);
      expect(meta.terminals.output).toHaveLength(1);
      expect(meta.terminals.input[0].label).toBe('+');
      expect(meta.terminals.input[1].label).toBe('−');
    });

    it('configures DEADTIME block with 1 PWM input, 2 outputs (hi, lo), and t_dead parameter', () => {
      const meta = COMPONENT_METAS[ControlComponentType.DEADTIME];
      expect(meta.terminals.input).toHaveLength(1);
      expect(meta.terminals.output).toHaveLength(2);
      expect(meta.terminals.output[0].label).toBe('hi');
      expect(meta.terminals.output[1].label).toBe('lo');
      expect(meta.parameters).toHaveLength(1);
      expect(meta.parameters[0].key).toBe('param0');
    });

    it('configures ABCDQ and DQABC transforms with correct pin counts', () => {
      const abcdq = COMPONENT_METAS[ControlComponentType.ABCDQ];
      expect(abcdq.terminals.input).toHaveLength(4);
      expect(abcdq.terminals.output).toHaveLength(3);

      const dqabc = COMPONENT_METAS[ControlComponentType.DQABC];
      expect(dqabc.terminals.input).toHaveLength(4);
      expect(dqabc.terminals.output).toHaveLength(3);
    });

    it('configures THYR_CTRL and PMSM_CONTROL blocks with correct pin counts', () => {
      const thc = COMPONENT_METAS[ControlComponentType.THYR_CTRL];
      expect(thc.terminals.input).toHaveLength(2);
      expect(thc.terminals.output).toHaveLength(6);

      const foc = COMPONENT_METAS[ControlComponentType.PMSM_CONTROL];
      expect(foc.terminals.input).toHaveLength(4);
      expect(foc.terminals.output).toHaveLength(2);
    });

    it('maps legacy control types through getComponentMeta', () => {
      expect(getComponentMeta(13, 'CONTROL').type).toBe(ControlComponentType.SUB);
      expect(getComponentMeta(12, 'CONTROL').type).toBe(ControlComponentType.ADD);
      expect(getComponentMeta(14, 'CONTROL').type).toBe(ControlComponentType.MUL);
      expect(getComponentMeta(15, 'CONTROL').type).toBe(ControlComponentType.DIV);
      expect(getComponentMeta(27, 'CONTROL').type).toBe(ControlComponentType.LIMIT);
      expect(getComponentMeta(21, 'CONTROL').type).toBe(ControlComponentType.XOR);
      expect(getComponentMeta(33, 'CONTROL').type).toBe(ControlComponentType.ASIN);
      expect(getComponentMeta(35, 'CONTROL').type).toBe(ControlComponentType.ACOS);
      expect(getComponentMeta(37, 'CONTROL').type).toBe(ControlComponentType.TAN);
      expect(getComponentMeta(38, 'CONTROL').type).toBe(ControlComponentType.ATAN);
      expect(getComponentMeta(39, 'CONTROL').type).toBe(ControlComponentType.SQR);
      expect(getComponentMeta(42, 'CONTROL').type).toBe(ControlComponentType.POW);
      expect(getComponentMeta(44, 'CONTROL').type).toBe(ControlComponentType.ROUND);
      expect(getComponentMeta(47, 'CONTROL').type).toBe(ControlComponentType.SIGN);
      expect(getComponentMeta(48, 'CONTROL').type).toBe(ControlComponentType.EQ);
      expect(getComponentMeta(51, 'CONTROL').type).toBe(ControlComponentType.NE);
      expect(getComponentMeta(53, 'CONTROL').type).toBe(ControlComponentType.COUNTER);
      expect(getComponentMeta(58, 'CONTROL').type).toBe(ControlComponentType.TIME);
      expect(getComponentMeta(59, 'CONTROL').type).toBe(ControlComponentType.ABCDQ);
      expect(getComponentMeta(63, 'CONTROL').type).toBe(ControlComponentType.DQABC);
      expect(getComponentMeta(65, 'CONTROL').type).toBe(ControlComponentType.THYR_CTRL);
      expect(getComponentMeta(66, 'CONTROL').type).toBe(ControlComponentType.PMSM_CONTROL);
      expect(getComponentMeta(72, 'CONTROL').type).toBe(ControlComponentType.PMSM_MODULATOR);
      expect(getComponentMeta(76, 'CONTROL').type).toBe(ControlComponentType.DEMUX);
      expect(getComponentMeta(77, 'CONTROL').type).toBe(ControlComponentType.SPACE_VECTOR);
      expect(getComponentMeta(82, 'CONTROL').type).toBe(ControlComponentType.SDFT);
      expect(getComponentMeta(85, 'CONTROL').type).toBe(ControlComponentType.SPARSEMATRIX);
    });
  });
});
