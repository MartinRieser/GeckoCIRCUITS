/**
 * Component metadata, categorized groupings, parameter schemas,
 * physical units and engineering SI prefix notation parser/formatter.
 *
 * Mapped to GeckoCIRCUITS classic parameter indexing, types and units.
 */

import {
  CTRL_TYPE,
  ControlComponentType,
  LkComponentType,
  SpecialComponentType,
  SPECIAL_TYPE,
  SENTINEL_UNSET,
} from './constants';

export { CTRL_TYPE, SPECIAL_TYPE, SpecialComponentType };

export interface ParameterOption {
  label: string;
  value: number;
  /** Short explanation shown under the select while this option is active
   *  (e.g. what a controlled source mode actually does in the engine). */
  hint?: string;
}

export interface ParameterDef {
  index: number;
  key: string;
  label: string;
  description: string;
  defaultValue: number | string;
  unit?: string;
  min?: number;
  max?: number;
  step?: number;
  options?: ParameterOption[];
  /** Show this field only when the referenced sibling parameter currently
   *  holds one of the listed values (e.g. AC fields only for Sinusoidal AC
   *  sources). Absent = always visible. */
  visibleWhen?: { key: string; values: (number | string)[] };
  /** Mode-dependent display override for one storage slot: when the sibling
   *  parameter `key` holds one of the numeric map keys, label/description
   *  replace the base ones. Classic behavior: source slot 1 is the DC value
   *  in DC mode and the peak amplitude in Sinusoidal mode. */
  labelByOption?: { key: string; labels: Record<number, { label: string; description?: string }> };
}

/**
 * Whether a parameter field should be rendered for the given component
 * parameter map, evaluating its {@link ParameterDef.visibleWhen} constraint.
 */
export function isParameterVisible(
  def: ParameterDef,
  parameters: Record<string, unknown> | undefined,
): boolean {
  if (!def.visibleWhen || !parameters) {
    return true;
  }
  if (parameters[def.visibleWhen.key] === undefined) {
    // Mode selector value unknown — show everything rather than hide fields.
    return true;
  }
  const current = Number(parameters[def.visibleWhen.key]);
  return def.visibleWhen.values.some((v) => Number(v) === current);
}

/**
 * Applies a parameter's {@link ParameterDef.labelByOption} override for the
 * currently selected mode, returning the def to render. Falls back to the
 * base label/description when the mode value has no override (or the mode
 * selector value is not stored yet).
 */
export function resolveParameterDef(
  def: ParameterDef,
  parameters: Record<string, unknown> | undefined,
): ParameterDef {
  if (!def.labelByOption || !parameters) {
    return def;
  }
  const mode = Number(parameters[def.labelByOption.key]);
  const override = def.labelByOption.labels[mode];
  if (!override) {
    return def;
  }
  return { ...def, label: override.label, description: override.description ?? def.description };
}

export interface ComponentMeta {
  type: number;
  family: string;
  name: string;
  displayName: string;
  category: 'passives' | 'sources' | 'semiconductors' | 'switches' | 'transformers' | 'machines' | 'thermal' | 'terminals' | 'measurement' | 'control' | 'logic' | 'documentation';
  description: string;
  /** Not simulated by the engine yet: shown greyed out in the palette and
   *  cannot be armed/placed until implemented. */
  disabled?: boolean;
  shortcut?: string;
  defaultPrefix: string;
  parameters: ParameterDef[];
  terminals: {
    input: { label: string; description: string }[];
    output: { label: string; description: string }[];
  };
}

export const CATEGORIES = [
  { id: 'all', label: 'All Components' },
  { id: 'documentation', label: 'Documentation & Notes' },
  { id: 'passives', label: 'Passives' },
  { id: 'sources', label: 'Sources' },
  { id: 'semiconductors', label: 'Semiconductors' },
  { id: 'switches', label: 'Switches' },
  { id: 'transformers', label: 'Transformers & Coupled' },
  { id: 'machines', label: 'Motors & OpAmps' },
  { id: 'measurement', label: 'Measurement' },
  { id: 'control', label: 'Control & Signal' },
  { id: 'logic', label: 'Logic Gates' },
  { id: 'thermal', label: 'Thermal Domain' },
  { id: 'terminals', label: 'Terminals & Net' },
] as const;

export type CategoryId = (typeof CATEGORIES)[number]['id'];

/**
 * Resolves the number of input and output terminals for a component, handling
 * dynamic multi-port components like function blocks (anzXIN, anzYOUT) and scopes.
 * Single source of truth shared between geometry calculations and symbol SVG rendering.
 */
export function resolveComponentPinCounts(component: {
  type?: number;
  family?: string;
  name?: string;
  parameters?: Record<string, unknown>;
  inputLabels?: string[];
  outputLabels?: string[];
  inputs?: unknown[];
}): { inputCount: number; outputCount: number } {
  const compParams = component.parameters ?? {};
  const rawIn = compParams.anzXIN;
  const rawOut = compParams.anzYOUT;

  // Resolve static terminal counts from component metadata if available
  const meta = component.type != null ? getComponentMeta(component.type, component.family, component.name) : null;
  const metaIn = meta?.terminals?.input?.length;
  const metaOut = meta?.terminals?.output?.length;

  const inCount =
    rawIn !== undefined && rawIn !== null && rawIn !== '' && !isNaN(Number(rawIn))
      ? Math.max(0, Number(rawIn))
      : (component.inputLabels && component.inputLabels.length > 0
          ? component.inputLabels.length
          : (component.inputs && component.inputs.length > 0
              ? component.inputs.length
              : (metaIn ?? 1)));

  let outCount =
    rawOut !== undefined && rawOut !== null && rawOut !== '' && !isNaN(Number(rawOut))
      ? Math.max(1, Number(rawOut))
      : (component.outputLabels && component.outputLabels.length > 0
          ? component.outputLabels.length
          : (metaOut ?? 1));

  // Dynamic output count for DEMUX (modern 1054 / legacy 85)
  if (
    component.type === ControlComponentType.DEMUX ||
    component.type === ControlComponentType.LEGACY_DEMUX ||
    component.type === 85 ||
    component.type === 1054
  ) {
    const demuxOut = compParams.param0 ?? compParams.num_outputs;
    if (demuxOut !== undefined && demuxOut !== null && !isNaN(Number(demuxOut))) {
      outCount = Math.max(1, Math.min(32, Math.round(Number(demuxOut))));
    }
  }

  return { inputCount: inCount, outputCount: outCount };
}


export const COMPONENT_METAS: Record<number, ComponentMeta> = {
  // Resistor
  1: {
    type: 1,
    family: 'LK',
    name: 'LK_R',
    displayName: 'Resistor',
    category: 'passives',
    description: 'Linear resistor obeying Ohm\'s Law (V = I · R)',
    shortcut: 'R',
    defaultPrefix: 'R',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Resistance (R)',
        description: 'Resistance in Ohms',
        defaultValue: 1000,
        unit: 'Ω',
        min: 1e-9,
        step: 1,
      },
    ],
    terminals: {
      input: [{ label: '1', description: 'Terminal 1' }],
      output: [{ label: '2', description: 'Terminal 2' }],
    },
  },

  // Inductor
  2: {
    type: 2,
    family: 'LK',
    name: 'LK_L',
    displayName: 'Inductor',
    category: 'passives',
    description: 'Linear ideal inductor (V = L · di/dt)',
    shortcut: 'L',
    defaultPrefix: 'L',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Inductance (L)',
        description: 'Inductance in Henrys',
        defaultValue: 3e-4,
        unit: 'H',
        min: 1e-12,
        step: 1e-4,
      },
      {
        index: 1,
        key: 'param1',
        label: 'Initial Current (iL₀)',
        description: 'Initial inductor current at t=0',
        defaultValue: 0,
        unit: 'A',
      },
    ],
    terminals: {
      input: [{ label: '1', description: 'Terminal 1' }],
      output: [{ label: '2', description: 'Terminal 2' }],
    },
  },

  // Capacitor
  3: {
    type: 3,
    family: 'LK',
    name: 'LK_C',
    displayName: 'Capacitor',
    category: 'passives',
    description: 'Linear ideal capacitor (i = C · dv/dt)',
    shortcut: 'C',
    defaultPrefix: 'C',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Capacitance (C)',
        description: 'Capacitance in Farads',
        defaultValue: 100e-9,
        unit: 'F',
        min: 1e-15,
        step: 1e-6,
      },
      {
        index: 1,
        key: 'param1',
        label: 'Initial Voltage (uC₀)',
        description: 'Initial capacitor voltage at t=0',
        defaultValue: 0,
        unit: 'V',
      },
    ],
    terminals: {
      input: [{ label: '+', description: 'Positive Terminal' }],
      output: [{ label: '−', description: 'Negative Terminal' }],
    },
  },

  // Voltage Source
  4: {
    type: 4,
    family: 'LK',
    name: 'LK_U',
    displayName: 'Voltage Source',
    category: 'sources',
    description: 'Independent DC or AC voltage source',
    shortcut: 'V',
    defaultPrefix: 'V',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Source Type',
        description: 'Waveform mode (DC, Sinusoidal, etc.)',
        defaultValue: 401,
        unit: '',
        options: [
          { label: 'DC Voltage', value: 401 },
          { label: 'Sinusoidal AC', value: 402 },
          {
            label: 'Signal Controlled',
            value: 400,
            hint: 'Value is meant to be driven by a control signal (classic Gecko). This engine currently outputs the constant value below.',
          },
          {
            label: 'Voltage Controlled',
            value: 399,
            hint: 'Advanced: follows the sensed voltage of a referenced element (used internally by the Ideal Transformer). A standalone source in this mode outputs 0 V.',
          },
        ],
      },
      {
        index: 1,
        key: 'param1',
        label: 'Voltage (Vdc)',
        description: 'Constant DC output voltage (fallback value in signal-controlled mode)',
        defaultValue: 10,
        unit: 'V',
        visibleWhen: { key: 'param0', values: [401, 400, 402] },
        labelByOption: {
          key: 'param0',
          labels: {
            402: { label: 'Amplitude (Vpk)', description: 'Sinusoidal peak amplitude in Volts' },
          },
        },
      },
      {
        index: 2,
        key: 'param2',
        label: 'Frequency (f)',
        description: 'AC frequency in Hertz',
        defaultValue: 50,
        unit: 'Hz',
        min: 0,
        visibleWhen: { key: 'param0', values: [402] },
      },
      {
        index: 3,
        key: 'param3',
        label: 'DC Offset (Voffset)',
        description: 'Constant DC offset added to the AC waveform',
        defaultValue: 0,
        unit: 'V',
        visibleWhen: { key: 'param0', values: [402] },
      },
      {
        index: 4,
        key: 'param4',
        label: 'Phase Shift (φ)',
        description: 'Phase shift in degrees',
        defaultValue: 0,
        unit: '°',
        visibleWhen: { key: 'param0', values: [402] },
      },
    ],
    terminals: {
      input: [{ label: '+', description: 'Positive Output' }],
      output: [{ label: '−', description: 'Negative Reference' }],
    },
  },

  // Current Source
  5: {
    type: 5,
    family: 'LK',
    name: 'LK_I',
    displayName: 'Current Source',
    category: 'sources',
    description: 'Independent DC or AC current source',
    shortcut: 'I',
    defaultPrefix: 'I',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Source Type',
        description: 'Current source waveform mode',
        defaultValue: 401,
        unit: '',
        options: [
          { label: 'DC Current', value: 401 },
          { label: 'Sinusoidal AC', value: 402 },
          {
            label: 'Signal Controlled',
            value: 400,
            hint: 'Value is meant to be driven by a control signal (classic Gecko). This engine currently outputs the constant value below.',
          },
        ],
      },
      {
        index: 1,
        key: 'param1',
        label: 'Current (Idc)',
        description: 'Constant DC output current (fallback value in signal-controlled mode)',
        defaultValue: 1,
        unit: 'A',
        labelByOption: {
          key: 'param0',
          labels: {
            402: { label: 'Amplitude (Apk)', description: 'Sinusoidal peak amplitude in Amperes' },
          },
        },
      },
      {
        index: 2,
        key: 'param2',
        label: 'Frequency (f)',
        description: 'AC frequency in Hertz',
        defaultValue: 50,
        unit: 'Hz',
        min: 0,
        visibleWhen: { key: 'param0', values: [402] },
      },
      {
        index: 3,
        key: 'param3',
        label: 'DC Offset (Ioffset)',
        description: 'Constant DC offset added to the AC waveform',
        defaultValue: 0,
        unit: 'A',
        visibleWhen: { key: 'param0', values: [402] },
      },
      {
        index: 4,
        key: 'param4',
        label: 'Phase Shift (φ)',
        description: 'Phase shift in degrees',
        defaultValue: 0,
        unit: '°',
        visibleWhen: { key: 'param0', values: [402] },
      },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Current Flow Entry' }],
      output: [{ label: 'out', description: 'Current Flow Exit' }],
    },
  },

  // Diode
  6: {
    type: 6,
    family: 'LK',
    name: 'LK_D',
    displayName: 'Diode',
    category: 'semiconductors',
    description: 'Piecewise linear diode model with forward drop & on/off resistances',
    shortcut: 'D',
    defaultPrefix: 'D',
    parameters: [
      {
        index: 1,
        key: 'param1',
        label: 'Forward Voltage Drop (uF)',
        description: 'Forward threshold voltage drop in Volts',
        defaultValue: 0.6,
        unit: 'V',
        min: 0,
      },
      {
        index: 2,
        key: 'param2',
        label: 'On-Resistance (rON)',
        description: 'Slope resistance in forward conducting state',
        defaultValue: 10e-3,
        unit: 'Ω',
        min: 1e-6,
      },
      {
        index: 3,
        key: 'param3',
        label: 'Off-Resistance (rOFF)',
        description: 'Leakage resistance in reverse blocking state',
        defaultValue: 1e7,
        unit: 'Ω',
        min: 100,
      },
    ],
    terminals: {
      input: [{ label: 'A', description: 'Anode (+)' }],
      output: [{ label: 'K', description: 'Cathode (−)' }],
    },
  },

  // Ideal Switch
  7: {
    type: 7,
    family: 'LK',
    name: 'LK_S',
    displayName: 'Ideal Switch',
    category: 'switches',
    description: 'Ideal voltage/signal-controlled bilateral switch',
    shortcut: 'S',
    defaultPrefix: 'S',
    parameters: [
      // param0 is the solver's dynamic resistance state — not user-editable,
      // matching the classic IdealSwitch parameter layout
      {
        index: 1,
        key: 'param1',
        label: 'On-Resistance (rON)',
        description: 'Resistance when closed / conducting',
        defaultValue: 10e-3,
        unit: 'Ω',
        min: 1e-6,
      },
      {
        index: 2,
        key: 'param2',
        label: 'Off-Resistance (rOFF)',
        description: 'Resistance when open / blocking',
        defaultValue: 1e7,
        unit: 'Ω',
        min: 100,
      },
    ],
    terminals: {
      input: [{ label: '1', description: 'Terminal 1' }],
      output: [{ label: '2', description: 'Terminal 2' }],
    },
  },

  // Thyristor
  8: {
    type: 8,
    family: 'LK',
    name: 'LK_TH',
    displayName: 'Thyristor (SCR)',
    category: 'semiconductors',
    description: 'Gate turn-on thyristor with natural current zero turn-off',
    shortcut: 'Y',
    defaultPrefix: 'TH',
    parameters: [
      {
        index: 1,
        key: 'param1',
        label: 'Forward Voltage Drop (uF)',
        description: 'Forward threshold voltage drop in Volts',
        defaultValue: 0.8,
        unit: 'V',
        min: 0,
      },
      {
        index: 2,
        key: 'param2',
        label: 'On-Resistance (rON)',
        description: 'On-state slope resistance',
        defaultValue: 10e-3,
        unit: 'Ω',
      },
      {
        index: 3,
        key: 'param3',
        label: 'Off-Resistance (rOFF)',
        description: 'Off-state blocking resistance',
        defaultValue: 1e7,
        unit: 'Ω',
      },
    ],
    terminals: {
      input: [{ label: 'A', description: 'Anode' }],
      output: [{ label: 'K', description: 'Cathode' }],
    },
  },

  // Mutual inductance coupler (classic LK_M): declares coupling factor k
  // between two coupled inductors. Simulates via the mutual-coupling solver;
  // primarily produced by imported classic circuits.
  9: {
    type: 9,
    family: 'LK',
    name: 'LK_M',
    displayName: 'Mutual Coupling (k)',
    category: 'transformers',
    description:
      'Declares the magnetic coupling factor k between two coupled inductors (LK_LKOP2). ' +
      'Pinless by design — like a SPICE K statement it connects by reference, not by wires: ' +
      'choose the two inductors in Properties and the sheet draws dashed coupling guides.',
    shortcut: 'T',
    defaultPrefix: 'K',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Coupling Factor (k)',
        description: 'Coupling coefficient between the two inductors (0..1)',
        defaultValue: 0.98,
        unit: '',
        min: 0,
        max: 1,
      },
    ],
    terminals: {
      input: [],
      output: [],
    },
  },

  // IGBT
  10: {
    type: 10,
    family: 'LK',
    name: 'LK_IGBT',
    displayName: 'IGBT',
    category: 'semiconductors',
    description: 'Insulated Gate Bipolar Transistor with saturation drop',
    shortcut: 'G',
    defaultPrefix: 'T',
    parameters: [
      {
        index: 1,
        key: 'param1',
        label: 'Saturation Drop (Vce,sat / uF)',
        description: 'Forward saturation voltage drop in Volts',
        defaultValue: 0.8,
        unit: 'V',
        min: 0,
      },
      {
        index: 2,
        key: 'param2',
        label: 'On-Resistance (rON)',
        description: 'On-state differential slope resistance',
        defaultValue: 10e-3,
        unit: 'Ω',
      },
      {
        index: 3,
        key: 'param3',
        label: 'Off-Resistance (rOFF)',
        description: 'Off-state blocking leakage resistance',
        defaultValue: 1e7,
        unit: 'Ω',
      },
    ],
    terminals: {
      input: [{ label: 'C', description: 'Collector' }],
      output: [{ label: 'E', description: 'Emitter' }],
    },
  },

  // Coupable Inductor (LKOP2) — engine: inductor stamper
  12: {
    type: 12,
    family: 'LK',
    name: 'LK_LKOP2',
    displayName: 'Coupled Inductor',
    category: 'transformers',
    description: 'Inductor supporting mutual magnetic coupling matrix (couple via the coupledComponent parameter)',
    defaultPrefix: 'Lcop',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Self-Inductance (L)',
        description: 'Self inductance in Henrys',
        defaultValue: 1e-3,
        unit: 'H',
        min: 1e-12,
      },
      {
        index: 1,
        key: 'param1',
        label: 'Initial Current (iL0)',
        description: 'Initial current in Amperes',
        defaultValue: 0,
        unit: 'A',
      },
    ],
    terminals: {
      input: [{ label: '1', description: 'Terminal 1' }],
      output: [{ label: '2', description: 'Terminal 2' }],
    },
  },

  // LISN — Line Impedance Stabilization Network (not yet stamped by the core engine)
  13: {
    type: 13,
    family: 'LK',
    name: 'LK_LISN',
    displayName: 'LISN',
    category: 'passives',
    description: 'Line Impedance Stabilization Network for conducted EMC measurements (not yet simulated by the web engine)',
    defaultPrefix: 'LISN',
    parameters: [],
    terminals: {
      input: [{ label: '1', description: 'Line Input' }],
      output: [{ label: '2', description: 'Equipment Under Test' }],
    },
  },

  // Separately / series excited DC motor (not yet stamped by the core engine)
  14: {
    type: 14,
    family: 'LK',
    name: 'LK_MOTOR',
    displayName: 'DC Motor (Excited)',
    category: 'machines',
    description: 'Separately or series excited DC machine (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'M_DC',
    parameters: [],
    terminals: {
      input: [{ label: 'A1', description: 'Armature +' }],
      output: [{ label: 'A2', description: 'Armature −' }],
    },
  },

  // Permanent Magnet Synchronous Machine (not yet stamped by the core engine)
  15: {
    type: 15,
    family: 'LK',
    name: 'LK_MOTOR_PMSM',
    displayName: 'PMSM Motor',
    category: 'machines',
    description: 'Permanent Magnet Synchronous Machine, dq model (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'PMSM',
    parameters: [
      { index: 0, key: 'param0', label: 'Stator Resistance (Rs)', description: 'Stator phase resistance in Ohms', defaultValue: 0.2, unit: 'Ω' },
      { index: 1, key: 'param1', label: 'd-axis Inductance (Ld)', description: 'Direct-axis inductance in Henrys', defaultValue: 1e-3, unit: 'H' },
      { index: 2, key: 'param2', label: 'q-axis Inductance (Lq)', description: 'Quadrature-axis inductance in Henrys', defaultValue: 1.5e-3, unit: 'H' },
    ],
    terminals: {
      input: [{ label: 'U', description: 'Phase U' }],
      output: [{ label: 'V', description: 'Phase V' }],
    },
  },

  // Synchronous machine, salient pole (not yet stamped by the core engine)
  16: {
    type: 16,
    family: 'LK',
    name: 'LK_MOTOR_SMSALIENT',
    displayName: 'Sync Motor (Salient)',
    category: 'machines',
    description: 'Synchronous machine with salient poles (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'SM',
    parameters: [],
    terminals: {
      input: [{ label: 'U', description: 'Phase U' }],
      output: [{ label: 'V', description: 'Phase V' }],
    },
  },

  // Synchronous machine, round rotor (not yet stamped by the core engine)
  17: {
    type: 17,
    family: 'LK',
    name: 'LK_MOTOR_SMROUND',
    displayName: 'Sync Motor (Round)',
    category: 'machines',
    description: 'Synchronous machine with round rotor (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'SM',
    parameters: [],
    terminals: {
      input: [{ label: 'U', description: 'Phase U' }],
      output: [{ label: 'V', description: 'Phase V' }],
    },
  },

  // Induction machine, cage rotor (not yet stamped by the core engine)
  18: {
    type: 18,
    family: 'LK',
    name: 'LK_MOTOR_IMA',
    displayName: 'Induction Motor (Cage)',
    category: 'machines',
    description: 'Induction machine with cage rotor (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'IM',
    parameters: [],
    terminals: {
      input: [{ label: 'U', description: 'Phase U' }],
      output: [{ label: 'V', description: 'Phase V' }],
    },
  },

  // Induction machine, generic (not yet stamped by the core engine)
  20: {
    type: 20,
    family: 'LK',
    name: 'LK_MOTOR_IMC',
    displayName: 'Induction Motor',
    category: 'machines',
    description: 'Generic induction machine (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'IM',
    parameters: [],
    terminals: {
      input: [{ label: 'U', description: 'Phase U' }],
      output: [{ label: 'V', description: 'Phase V' }],
    },
  },

  // Induction machine with saturation (not yet stamped by the core engine)
  21: {
    type: 21,
    family: 'LK',
    name: 'LK_MOTOR_IMSAT',
    displayName: 'Induction Motor (Sat.)',
    category: 'machines',
    description: 'Induction machine with magnetic saturation (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'IM',
    parameters: [],
    terminals: {
      input: [{ label: 'U', description: 'Phase U' }],
      output: [{ label: 'V', description: 'Phase V' }],
    },
  },

  // Operational amplifier (OPV1) — not yet stamped by the core engine
  22: {
    type: 22,
    family: 'LK',
    name: 'LK_OPV1',
    displayName: 'Op-Amp',
    category: 'machines',
    description: 'Macro-model operational amplifier with input/output impedance & limits (not yet simulated by the web engine)',
    defaultPrefix: 'OP',
    parameters: [
      { index: 0, key: 'param0', label: 'Input Resistance (rIN)', description: 'Differential input resistance in Ohms', defaultValue: 90000, unit: 'Ω' },
      { index: 1, key: 'param1', label: 'Output Resistance (rOUT)', description: 'Output series resistance in Ohms', defaultValue: 0.12, unit: 'Ω' },
      { index: 4, key: 'param4', label: 'Corner Frequency (fp)', description: 'Open loop dominant pole frequency parameter', defaultValue: 1e5, unit: 'Hz' },
      { index: 5, key: 'param5', label: 'Open-Loop DC Gain (Av)', description: 'Differential open-loop DC voltage gain', defaultValue: 10000, unit: 'V/V' },
      { index: 7, key: 'param7', label: 'Max Output Voltage (U_max)', description: 'Positive saturation output rail limit', defaultValue: 12.0, unit: 'V' },
      { index: 8, key: 'param8', label: 'Min Output Voltage (U_min)', description: 'Negative saturation output rail limit', defaultValue: -12.0, unit: 'V' },
    ],
    terminals: {
      input: [{ label: '+', description: 'Non-Inverting Input' }],
      output: [{ label: 'out', description: 'Amplifier Output' }],
    },
  },

  // Ideal transformer (LK_TRANS) — not yet stamped by the core engine
  23: {
    type: 23,
    family: 'LK',
    name: 'LK_TRANS',
    displayName: 'Ideal Transformer',
    category: 'transformers',
    description: 'Lossless two-winding ideal transformer (N1:N2) — simulated by the web engine via a coupled voltage-source pair',
    defaultPrefix: 'TR',
    parameters: [
      { index: 0, key: 'param0', label: 'Primary Turns (n1)', description: 'Number of turns on primary winding', defaultValue: 10, unit: 'turns', min: 1 },
      { index: 1, key: 'param1', label: 'Secondary Turns (n2)', description: 'Number of turns on secondary winding', defaultValue: 2, unit: 'turns', min: 1 },
      { index: 2, key: 'param2', label: 'Polarity', description: 'Winding polarity (+1 normal, -1 inverted)', defaultValue: -1, unit: '', options: [ { label: 'Inverted (-1)', value: -1 }, { label: 'Normal (+1)', value: 1 } ] },
    ],
    terminals: {
      input: [
        { label: 'P1', description: 'Primary Top' },
        { label: 'P2', description: 'Primary Bottom' },
      ],
      output: [
        { label: 'S1', description: 'Secondary Top' },
        { label: 'S2', description: 'Secondary Bottom' },
      ],
    },
  },

  // Magnetic reluctance (REL_RELUCTANCE) — engine: resistor stamper
  24: {
    type: 24,
    family: 'LK',
    name: 'REL_RELUCTANCE',
    displayName: 'Reluctance',
    category: 'transformers',
    description: 'Magnetic reluctance element of the reluctance network (Rm = F / Φ)',
    defaultPrefix: 'Rm',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Reluctance (Rm)',
        description: 'Magnetic reluctance in inverse Henrys',
        defaultValue: 1.0,
        unit: '1/H',
        min: 1e-9,
      },
    ],
    terminals: {
      input: [{ label: '1', description: 'Reluctance Terminal 1' }],
      output: [{ label: '2', description: 'Reluctance Terminal 2' }],
    },
  },

  // Reluctance network coil (REL_INDUCTOR) — not yet stamped by the core engine
  25: {
    type: 25,
    family: 'LK',
    name: 'REL_INDUCTOR',
    displayName: 'Reluctance Coil',
    category: 'transformers',
    description: 'Inductor winding coupled into the reluctance network (not yet simulated by the web engine)',
    defaultPrefix: 'Lrel',
    disabled: true,
    parameters: [
      { index: 0, key: 'param0', label: 'Inductance (L)', description: 'Winding inductance in Henrys', defaultValue: 1e-3, unit: 'H', min: 1e-12 },
      { index: 1, key: 'param1', label: 'Initial Current (iL0)', description: 'Initial winding current in Amperes', defaultValue: 0, unit: 'A' },
    ],
    terminals: {
      input: [{ label: '1', description: 'Coil Terminal 1' }],
      output: [{ label: '2', description: 'Coil Terminal 2' }],
    },
  },

  // Magnetomotive force source (REL_MMF) — engine: voltage source stamper
  26: {
    type: 26,
    family: 'LK',
    name: 'REL_MMF',
    displayName: 'MMF Source',
    category: 'transformers',
    description: 'Magnetomotive force source of the reluctance network (Θ = N·I)',
    defaultPrefix: 'MMF',
    parameters: [
      { index: 0, key: 'param0', label: 'Source Type', description: 'MMF source mode (401 = constant)', defaultValue: 401, unit: '' },
      { index: 1, key: 'param1', label: 'MMF (Θ)', description: 'Magnetomotive force in ampere-turns', defaultValue: 100, unit: 'A' },
    ],
    terminals: {
      input: [{ label: '1', description: 'MMF Entry' }],
      output: [{ label: '2', description: 'MMF Exit' }],
    },
  },

  // Power MOSFET with antiparallel body diode — engine: MOSFET stamper
  28: {
    type: 28,
    family: 'LK',
    name: 'LK_MOSFET',
    displayName: 'Power MOSFET',
    category: 'semiconductors',
    description: 'Power MOSFET switch with antiparallel body diode',
    shortcut: 'M',
    defaultPrefix: 'M',
    parameters: [
      {
        index: 1,
        key: 'param1',
        label: 'Body Diode Drop (uF)',
        description: 'Body diode forward voltage drop in Volts',
        defaultValue: 0.8,
        unit: 'V',
        min: 0,
      },
      {
        index: 2,
        key: 'param2',
        label: 'On-Resistance (Rds,on)',
        description: 'Drain-source on-state channel resistance',
        defaultValue: 10e-3,
        unit: 'Ω',
        min: 1e-6,
      },
      {
        index: 3,
        key: 'param3',
        label: 'Off-Resistance (Rds,off)',
        description: 'Drain-source off-state blocking resistance',
        defaultValue: 1e7,
        unit: 'Ω',
      },
    ],
    terminals: {
      input: [{ label: 'D', description: 'Drain' }],
      output: [{ label: 'S', description: 'Source' }],
    },
  },

  // Subcircuit terminal
  29: {
    type: 29,
    family: 'LK',
    name: 'LK_TERMINAL',
    displayName: 'Subcircuit Terminal',
    category: 'terminals',
    description: 'Subcircuit boundary terminal pin',
    defaultPrefix: 'PIN',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Terminal Pin' }],
      output: [{ label: 'out', description: 'Terminal Pin' }],
    },
  },

  // Global terminal / net label
  31: {
    type: 31,
    family: 'LK',
    name: 'LK_GLOBAL_TERMINAL',
    displayName: 'Global Net / Ground',
    category: 'terminals',
    description: 'Global named circuit net or reference node (e.g. GND)',
    defaultPrefix: 'GND',
    parameters: [],
    terminals: {
      input: [{ label: '1', description: 'Ground Connection Pin' }],
      output: [],
    },
  },

  // BJT transistor — not yet stamped by the core engine
  33: {
    type: 33,
    family: 'LK',
    name: 'LK_BJT',
    displayName: 'BJT Transistor',
    category: 'semiconductors',
    description: 'Bipolar junction transistor (NPN/PNP): base resistor, junction diodes and beta current sources — simulated by the web engine',
    shortcut: 'B',
    defaultPrefix: 'Q',
    parameters: [
      {
        index: 1,
        key: 'param1',
        label: 'Forward Beta (βF)',
        description: 'Forward current gain from base to collector',
        defaultValue: 100,
        unit: '',
        min: 1,
      },
      {
        index: 2,
        key: 'param2',
        label: 'Backward Beta (βR)',
        description: 'Reverse (inverse-mode) current gain',
        defaultValue: 60,
        unit: '',
        min: 1,
      },
      {
        index: 3,
        key: 'param3',
        label: 'Base Resistance (rB)',
        description: 'Internal series resistance of the base connection',
        defaultValue: 0.1,
        unit: 'Ω',
        min: 0.001,
      },
      {
        index: 4,
        key: 'param4',
        label: 'Polarity',
        description: 'Transistor polarity',
        defaultValue: 1,
        unit: '',
        options: [
          { label: 'NPN (+1)', value: 1 },
          { label: 'PNP (-1)', value: -1 },
        ],
      },
    ],
    terminals: {
      input: [
        { label: 'C', description: 'Collector' },
        { label: 'B', description: 'Base' },
      ],
      output: [{ label: 'E', description: 'Emitter' }],
    },
  },

  // Reluctance network terminal
  30: {
    type: 30,
    family: 'LK',
    name: 'REL_TERMINAL',
    displayName: 'Reluctance Terminal',
    category: 'terminals',
    description: 'Reluctance network boundary terminal pin',
    defaultPrefix: 'RPIN',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Terminal Pin' }],
      output: [{ label: 'out', description: 'Terminal Pin' }],
    },
  },

  // Thermal chip loss (TH_PvCHIP) — not yet stamped by the core engine
  41: {
    type: 41,
    family: 'THERM',
    name: 'TH_PvCHIP',
    displayName: 'Thermal Chip (Pv)',
    category: 'thermal',
    description: 'Chip-level power loss injection of a semiconductor module (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'Pv',
    parameters: [
      { index: 0, key: 'param0', label: 'Chip Power Loss (Pv)', description: 'Dissipated chip power in Watts', defaultValue: 10.0, unit: 'W', min: 0 },
    ],
    terminals: {
      input: [{ label: '1', description: 'Thermal Node 1' }],
      output: [{ label: '2', description: 'Thermal Node 2' }],
    },
  },

  // Thermal module (TH_MODUL) — not yet stamped by the core engine
  42: {
    type: 42,
    family: 'THERM',
    name: 'TH_MODUL',
    displayName: 'Thermal Module',
    category: 'thermal',
    description: 'Module housing thermal model: Rth/Cth pair between chip and heatsink (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'MOD',
    parameters: [
      { index: 0, key: 'param0', label: 'Thermal Resistance (Rth)', description: 'Module thermal resistance in Kelvin per Watt', defaultValue: 1.0, unit: 'K/W', min: 1e-6 },
      { index: 1, key: 'param1', label: 'Thermal Capacitance (Cth)', description: 'Module heat capacity in Joules per Kelvin', defaultValue: 1.0, unit: 'J/K', min: 1e-6 },
    ],
    terminals: {
      input: [{ label: '1', description: 'Chip Side' }],
      output: [{ label: '2', description: 'Heatsink Side' }],
    },
  },

  // Thermal heat flow source (TH_FLOW) — engine: current source stamper
  44: {
    type: 44,
    family: 'THERM',
    name: 'TH_FLOW',
    displayName: 'Heat Flow Source (Ploss)',
    category: 'thermal',
    description: 'Injected heat power source in Watts',
    defaultPrefix: 'Ploss',
    parameters: [
      { index: 0, key: 'param0', label: 'Source Type', description: 'Heat flow source mode (401 = constant)', defaultValue: 401, unit: '' },
      { index: 1, key: 'param1', label: 'Power Loss (P)', description: 'Heat dissipation power in Watts', defaultValue: 10.0, unit: 'W' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Heat Flow Entry' }],
      output: [{ label: 'out', description: 'Heat Flow Exit' }],
    },
  },

  // Thermal temperature source (TH_TEMP) — engine: voltage source stamper
  45: {
    type: 45,
    family: 'THERM',
    name: 'TH_TEMP',
    displayName: 'Temperature Source',
    category: 'thermal',
    description: 'Fixed or modulated temperature boundary condition',
    defaultPrefix: 'Tsrc',
    parameters: [
      { index: 0, key: 'param0', label: 'Source Type', description: 'Temperature source mode (401 = constant)', defaultValue: 401, unit: '' },
      { index: 1, key: 'param1', label: 'Temperature (T)', description: 'Fixed temperature in °C or K', defaultValue: 25.0, unit: '°C' },
    ],
    terminals: {
      input: [{ label: '+', description: 'Temperature Output' }],
      output: [{ label: '−', description: 'Thermal Reference' }],
    },
  },

  // Thermal resistance (TH_RTH) — engine: resistor stamper
  46: {
    type: 46,
    family: 'THERM',
    name: 'TH_RTH',
    displayName: 'Thermal Resistor',
    category: 'thermal',
    description: 'Thermal resistance for heat conduction (ΔT = P · Rth)',
    defaultPrefix: 'Rth',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Thermal Resistance (Rth)',
        description: 'Thermal resistance in Kelvin per Watt',
        defaultValue: 1.0,
        unit: 'K/W',
        min: 1e-6,
      },
    ],
    terminals: {
      input: [{ label: '1', description: 'Thermal Node 1' }],
      output: [{ label: '2', description: 'Thermal Node 2' }],
    },
  },

  // Thermal capacitance (TH_CTH) — engine: capacitor stamper
  47: {
    type: 47,
    family: 'THERM',
    name: 'TH_CTH',
    displayName: 'Thermal Capacitor',
    category: 'thermal',
    description: 'Thermal heat capacitance (P = Cth · dT/dt)',
    defaultPrefix: 'Cth',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Thermal Capacitance (Cth)',
        description: 'Heat capacity in Joules per Kelvin',
        defaultValue: 1.0,
        unit: 'J/K',
        min: 1e-6,
      },
      {
        index: 1,
        key: 'param1',
        label: 'Initial Temp (T0)',
        description: 'Initial temperature in °C or K',
        defaultValue: 25.0,
        unit: '°C',
      },
    ],
    terminals: {
      input: [{ label: '1', description: 'Thermal Node 1' }],
      output: [{ label: '2', description: 'Thermal Node 2' }],
    },
  },

  // Ambient temperature (TH_AMBIENT) — not yet stamped by the core engine
  48: {
    type: 48,
    family: 'THERM',
    name: 'TH_AMBIENT',
    displayName: 'Ambient Temperature',
    category: 'thermal',
    description: 'Ambient environment temperature boundary reference (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'Tamb',
    parameters: [
      { index: 0, key: 'param0', label: 'Source Type', description: 'Ambient temperature type (401 = constant)', defaultValue: 401, unit: '' },
      { index: 1, key: 'param1', label: 'Ambient Temp (Tamb)', description: 'Ambient environment temperature in °C or K', defaultValue: 25.0, unit: '°C' },
    ],
    terminals: {
      input: [{ label: '1', description: 'Ambient Reference' }],
      output: [{ label: '1', description: 'Ambient Reference' }],
    },
  },

  // Permanent magnet DC motor (LK_MOTOR_PERM) — not yet stamped by the core engine
  51: {
    type: 51,
    family: 'LK',
    name: 'LK_MOTOR_PERM',
    displayName: 'DC Motor (PM)',
    category: 'machines',
    description: 'Permanent magnet DC machine (not yet simulated by the web engine)',
    disabled: true,
    defaultPrefix: 'M_DC',
    parameters: [
      { index: 0, key: 'param0', label: 'Armature Resistance (Ra)', description: 'Armature circuit resistance in Ohms', defaultValue: 0.5, unit: 'Ω' },
      { index: 1, key: 'param1', label: 'Armature Inductance (La)', description: 'Armature circuit inductance in Henrys', defaultValue: 2e-3, unit: 'H' },
      { index: 2, key: 'param2', label: 'Torque Constant (Km)', description: 'Back-EMF and torque constant (V·s/rad or N·m/A)', defaultValue: 0.1, unit: 'N·m/A' },
    ],
    terminals: {
      input: [{ label: 'A1', description: 'Armature +' }],
      output: [{ label: 'A2', description: 'Armature −' }],
    },
  },

  // Non-linear reluctance (NONLIN_REL) — not yet stamped by the core engine
  52: {
    type: 52,
    family: 'LK',
    name: 'NONLIN_REL',
    displayName: 'Non-linear Reluctance',
    category: 'transformers',
    description: 'Reluctance element with non-linear B-H characteristic (not yet simulated by the web engine)',
    defaultPrefix: 'RmNL',
    disabled: true,
    parameters: [
      { index: 0, key: 'param0', label: 'Nominal Reluctance (Rm)', description: 'Reluctance at the linear operating point', defaultValue: 1.0, unit: '1/H', min: 1e-9 },
    ],
    terminals: {
      input: [{ label: '1', description: 'Reluctance Terminal 1' }],
      output: [{ label: '2', description: 'Reluctance Terminal 2' }],
    },
  },

  // Gate Driver Block (CONTROL)
  1000: {
    type: 1000,
    family: 'CONTROL',
    name: 'CTRL_GATE',
    displayName: 'Gate Driver',
    category: 'control',
    description: 'Coupled gate drive input block for semiconductor switches',
    defaultPrefix: 'GATE',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Initial State',
        description: 'Initial switch gate state',
        defaultValue: 0,
        unit: '',
        options: [
          { label: 'OFF (0)', value: 0 },
          { label: 'ON (1)', value: 1 },
        ],
      },
    ],
    terminals: {
      input: [{ label: 'gt', description: 'Gate Drive Signal' }],
      output: [],
    },
  },

  // ========== CONTROL-domain: Measurement ==========

  1001: {
    type: 1001,
    family: 'CONTROL',
    name: 'CTRL_VOLT',
    displayName: 'Voltmeter',
    category: 'measurement',
    description: 'Measures voltage between two electrical nodes',
    shortcut: 'U',
    defaultPrefix: 'V_meas',
    parameters: [],
    terminals: {
      input: [{ label: '+', description: 'Positive probe' }],
      output: [{ label: '−', description: 'Negative probe' }],
    },
  },

  1002: {
    type: 1002,
    family: 'CONTROL',
    name: 'CTRL_AMP',
    displayName: 'Ammeter',
    category: 'measurement',
    description: 'Measures current through an electrical branch',
    shortcut: 'A',
    defaultPrefix: 'I_meas',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Current flow in' }],
      output: [{ label: 'out', description: 'Measurement output' }],
    },
  },

  1003: {
    type: 1003,
    family: 'CONTROL',
    name: 'CTRL_SCOPE',
    displayName: 'Scope',
    category: 'measurement',
    description: 'Oscilloscope display — records and visualises signals',
    defaultPrefix: 'SCOPE',
    parameters: [],
    terminals: {
      input: [
        { label: 'ch1', description: 'Channel 1' },
        { label: 'ch2', description: 'Channel 2' },
        { label: 'ch3', description: 'Channel 3' },
      ],
      output: [],
    },
  },

  // ========== CONTROL-domain: Signal Sources & Processing ==========

  1004: {
    type: 1004,
    family: 'CONTROL',
    name: 'CTRL_SIGNAL',
    displayName: 'Signal Source',
    category: 'control',
    description: 'Generates sine, triangle, PWM pulse/rectangle, or noise waveform signals',
    defaultPrefix: 'SIG',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Waveform Type',
        description: 'Signal waveform function',
        defaultValue: 404,
        unit: '',
        options: [
          { label: 'Rectangle / PWM', value: 404 },
          { label: 'Sine', value: 402 },
          { label: 'Triangle', value: 403 },
          {
            label: 'Random Noise',
            value: 405,
            hint: 'Random noise ignores amplitude, frequency, offset, phase, and duty cycle.',
          },
        ],
      },
      {
        index: 1,
        key: 'param1',
        label: 'Amplitude',
        description: 'Peak signal amplitude',
        defaultValue: 1.0,
        unit: 'V',
        step: 0.1,
        visibleWhen: { key: 'param0', values: [402, 403, 404] },
      },
      {
        index: 2,
        key: 'param2',
        label: 'Frequency (f)',
        description: 'Repetition frequency in Hz',
        defaultValue: 100000,
        unit: 'Hz',
        min: 1e-3,
        step: 1000,
        visibleWhen: { key: 'param0', values: [402, 403, 404] },
      },
      {
        index: 3,
        key: 'param3',
        label: 'DC Offset',
        description: 'Constant vertical offset',
        defaultValue: 0.0,
        unit: 'V',
        step: 0.1,
        visibleWhen: { key: 'param0', values: [402, 403, 404] },
      },
      {
        index: 4,
        key: 'param4',
        label: 'Phase',
        description: 'Initial phase angle in radians',
        defaultValue: 0.0,
        unit: 'rad',
        step: 0.1,
        visibleWhen: { key: 'param0', values: [402, 403, 404] },
      },
      {
        index: 5,
        key: 'param5',
        label: 'Duty Cycle (D)',
        description: 'Pulse duty ratio (0..1) for PWM rectangle, rise fraction for triangle',
        defaultValue: 0.5,
        unit: '',
        min: 0.0,
        max: 1.0,
        step: 0.01,
        visibleWhen: { key: 'param0', values: [403, 404] },
      },
    ],
    terminals: {
      input: [],
      output: [{ label: 'out', description: 'Signal output' }],
    },
  },

  1005: {
    type: 1005,
    family: 'CONTROL',
    name: 'CTRL_CONSTANT',
    displayName: 'Constant',
    category: 'control',
    description: 'Outputs a constant value',
    defaultPrefix: 'K',
    parameters: [
      { index: 0, key: 'param0', label: 'Value (k)', description: 'Constant output value', defaultValue: 1, unit: '' },
    ],
    terminals: {
      input: [],
      output: [{ label: 'out', description: 'Constant output' }],
    },
  },

  1006: {
    type: 1006,
    family: 'CONTROL',
    name: 'CTRL_GAIN',
    displayName: 'Gain',
    category: 'control',
    description: 'Multiplies input signal by a constant factor k',
    defaultPrefix: 'GAIN',
    parameters: [
      { index: 0, key: 'param0', label: 'Gain (k)', description: 'Multiplication factor', defaultValue: 1, unit: '' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Amplified output' }],
    },
  },

  1007: {
    type: 1007,
    family: 'CONTROL',
    name: 'CTRL_PI',
    displayName: 'PI Controller',
    category: 'control',
    description: 'Proportional-Integral controller with anti-windup',
    defaultPrefix: 'PI',
    parameters: [
      { index: 0, key: 'param0', label: 'Kp', description: 'Proportional gain', defaultValue: 1, unit: '' },
      { index: 1, key: 'param1', label: 'Ti', description: 'Integration time constant', defaultValue: 0.01, unit: 's' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Error input' }],
      output: [{ label: 'out', description: 'Controller output' }],
    },
  },

  1008: {
    type: 1008,
    family: 'CONTROL',
    name: 'CTRL_PT1',
    displayName: 'PT1 Low-Pass',
    category: 'control',
    description: 'First-order low-pass filter (PT1 element)',
    defaultPrefix: 'PT1',
    parameters: [
      { index: 0, key: 'param0', label: 'Time Constant (τ)', description: 'Filter time constant', defaultValue: 0.001, unit: 's' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Filtered output' }],
    },
  },

  1009: {
    type: 1009,
    family: 'CONTROL',
    name: 'CTRL_INTEGRATOR',
    displayName: 'Integrator',
    category: 'control',
    description: 'Time integration of input signal (∫): y = y0 + ∫ in dt. Input 0 is the signal to integrate; Input 1 is the reset trigger (when rst >= 1, output resets to initial value; unwired resets stay 0).',
    defaultPrefix: 'INT',
    parameters: [
      { index: 0, key: 'param0', label: 'Initial Value (y0)', description: 'Initial integrator state upon simulation start or reset', defaultValue: 0, unit: '' },
    ],
    terminals: {
      input: [
        { label: 'in', description: 'Signal to integrate' },
        { label: 'rst', description: 'Reset trigger (resets state to initial value when >= 1; unwired stays 0)' },
      ],
      output: [{ label: 'out', description: 'Integrated output signal' }],
    },
  },

  1010: {
    type: 1010,
    family: 'CONTROL',
    name: 'CTRL_COMPARATOR',
    displayName: 'Comparator',
    category: 'control',
    description: 'Compares two signals — outputs 1 if in1 > in2, else 0',
    defaultPrefix: 'CMP',
    parameters: [],
    terminals: {
      input: [
        { label: '+', description: 'Non-inverting input' },
        { label: '−', description: 'Inverting input' },
      ],
      output: [{ label: 'out', description: 'Binary output (0 or 1)' }],
    },
  },

  // ========== CONTROL-domain: Logic Gates ==========

  1011: {
    type: 1011,
    family: 'CONTROL',
    name: 'CTRL_AND',
    displayName: 'AND Gate',
    category: 'logic',
    description: 'Logical AND — output = 1 only if ALL inputs > 0.5',
    defaultPrefix: 'AND',
    parameters: [],
    terminals: {
      input: [
        { label: 'A', description: 'Input A' },
        { label: 'B', description: 'Input B' },
      ],
      output: [{ label: 'Q', description: 'AND output' }],
    },
  },

  1012: {
    type: 1012,
    family: 'CONTROL',
    name: 'CTRL_OR',
    displayName: 'OR Gate',
    category: 'logic',
    description: 'Logical OR — output = 1 if ANY input > 0.5',
    defaultPrefix: 'OR',
    parameters: [],
    terminals: {
      input: [
        { label: 'A', description: 'Input A' },
        { label: 'B', description: 'Input B' },
      ],
      output: [{ label: 'Q', description: 'OR output' }],
    },
  },

  1013: {
    type: 1013,
    family: 'CONTROL',
    name: 'CTRL_NOT',
    displayName: 'NOT Gate',
    category: 'logic',
    description: 'Logical NOT / Inverter — output = 1 if input ≤ 0.5',
    defaultPrefix: 'NOT',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input' }],
      output: [{ label: 'Q', description: 'Inverted output' }],
    },
  },

  1014: {
    type: 1014,
    family: 'CONTROL',
    name: 'CTRL_MUX',
    displayName: 'Multiplexer (2-to-1)',
    category: 'control',
    description: '2-to-1 multiplexer / signal selector: switches between two data inputs based on the selector control signal. When sel < 0.5, passes in0; when sel >= 0.5, passes in1.',
    defaultPrefix: 'MUX',
    parameters: [],
    terminals: {
      input: [
        { label: 'sel', description: 'Selector control input (< 0.5 selects in0, >= 0.5 selects in1)' },
        { label: 'in0', description: 'Data input 0 (passed to output when sel < 0.5)' },
        { label: 'in1', description: 'Data input 1 (passed to output when sel >= 0.5)' },
      ],
      output: [{ label: 'out', description: 'Selected output signal' }],
    },
  },

  1015: {
    type: 1015,
    family: 'CONTROL',
    name: 'CTRL_DELAY',
    displayName: 'Time Delay',
    category: 'control',
    description: 'Delays input signal by a configurable time τ',
    defaultPrefix: 'DELAY',
    parameters: [
      { index: 0, key: 'param0', label: 'Delay Time (τ)', description: 'Signal delay time', defaultValue: 0.001, unit: 's' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Delayed output' }],
    },
  },

  1016: {
    type: 1016,
    family: 'CONTROL',
    name: 'CTRL_SCRIPT',
    displayName: 'Function Block (Script)',
    category: 'control',
    description: 'Custom programmable function block executing math expressions or script logic',
    defaultPrefix: 'FUNC',
    parameters: [
      { index: 0, key: 'sourceCode', label: 'Script / Formula', description: 'Calculates yOUT from xIN signals, t, dt, math functions', defaultValue: 'yOUT[0] = xIN[0];' },
      { index: 1, key: 'anzXIN', label: 'Inputs', description: 'Number of input signals', defaultValue: 1 },
      { index: 2, key: 'anzYOUT', label: 'Outputs', description: 'Number of output signals', defaultValue: 1 },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Signal output' }],
    },
  },

  1017: {
    type: 1017,
    family: 'CONTROL',
    name: 'CTRL_SUB',
    displayName: 'Subtract',
    category: 'control',
    description: 'Subtracts input 1 from input 0: out = in0 − in1 (ideal for error calculation)',
    defaultPrefix: 'SUB',
    parameters: [],
    terminals: {
      input: [
        { label: '+', description: 'Minuend (reference/target)' },
        { label: '−', description: 'Subtrahend (actual/feedback)' },
      ],
      output: [{ label: 'out', description: 'Difference / Error signal' }],
    },
  },

  1018: {
    type: 1018,
    family: 'CONTROL',
    name: 'CTRL_ADD',
    displayName: 'Add',
    category: 'control',
    description: 'Adds two signals: out = in0 + in1',
    defaultPrefix: 'ADD',
    parameters: [],
    terminals: {
      input: [
        { label: 'in0', description: 'Input 0' },
        { label: 'in1', description: 'Input 1' },
      ],
      output: [{ label: 'out', description: 'Sum output' }],
    },
  },

  1019: {
    type: 1019,
    family: 'CONTROL',
    name: 'CTRL_MUL',
    displayName: 'Multiply',
    category: 'control',
    description: 'Multiplies two signals: out = in0 × in1',
    defaultPrefix: 'MUL',
    parameters: [],
    terminals: {
      input: [
        { label: 'in0', description: 'Input 0' },
        { label: 'in1', description: 'Input 1' },
      ],
      output: [{ label: 'out', description: 'Product output' }],
    },
  },

  1020: {
    type: 1020,
    family: 'CONTROL',
    name: 'CTRL_DIV',
    displayName: 'Divide',
    category: 'control',
    description: 'Divides input 0 by input 1: out = in0 / in1',
    defaultPrefix: 'DIV',
    parameters: [],
    terminals: {
      input: [
        { label: 'num', description: 'Numerator (in0)' },
        { label: 'den', description: 'Denominator (in1)' },
      ],
      output: [{ label: 'out', description: 'Quotient output' }],
    },
  },

  1021: {
    type: 1021,
    family: 'CONTROL',
    name: 'CTRL_LIMIT',
    displayName: 'Limiter',
    category: 'control',
    description: 'Limits signal amplitude between Min and Max bounds',
    defaultPrefix: 'LIM',
    parameters: [
      { index: 0, key: 'param0', label: 'Min', description: 'Lower limit bound', defaultValue: -1, unit: '' },
      { index: 1, key: 'param1', label: 'Max', description: 'Upper limit bound', defaultValue: 1, unit: '' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Limited output' }],
    },
  },

  1022: {
    type: 1022,
    family: 'CONTROL',
    name: 'CTRL_ABS',
    displayName: 'Absolute Value',
    category: 'control',
    description: 'Computes absolute value: out = |in|',
    defaultPrefix: 'ABS',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Absolute output' }],
    },
  },

  1023: {
    type: 1023,
    family: 'CONTROL',
    name: 'CTRL_SQRT',
    displayName: 'Square Root',
    category: 'control',
    description: 'Computes square root: out = √(in)',
    defaultPrefix: 'SQRT',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Square root output' }],
    },
  },

  1024: {
    type: 1024,
    family: 'CONTROL',
    name: 'CTRL_EXP',
    displayName: 'Exponential',
    category: 'control',
    description: 'Natural exponential function: out = e^(in)',
    defaultPrefix: 'EXP',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Exponential output' }],
    },
  },

  1025: {
    type: 1025,
    family: 'CONTROL',
    name: 'CTRL_LN',
    displayName: 'Natural Log',
    category: 'control',
    description: 'Natural logarithm: out = ln(in)',
    defaultPrefix: 'LN',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Logarithmic output' }],
    },
  },

  1026: {
    type: 1026,
    family: 'CONTROL',
    name: 'CTRL_SIN',
    displayName: 'Sine',
    category: 'control',
    description: 'Trigonometric sine: out = sin(in)',
    defaultPrefix: 'SIN',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input angle (rad)' }],
      output: [{ label: 'out', description: 'Sine output' }],
    },
  },

  1027: {
    type: 1027,
    family: 'CONTROL',
    name: 'CTRL_COS',
    displayName: 'Cosine',
    category: 'control',
    description: 'Trigonometric cosine: out = cos(in)',
    defaultPrefix: 'COS',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input angle (rad)' }],
      output: [{ label: 'out', description: 'Cosine output' }],
    },
  },

  1028: {
    type: 1028,
    family: 'CONTROL',
    name: 'CTRL_MIN',
    displayName: 'Minimum',
    category: 'control',
    description: 'Selects the smaller of two signals: out = min(in0, in1)',
    defaultPrefix: 'MIN',
    parameters: [],
    terminals: {
      input: [
        { label: 'in0', description: 'Input 0' },
        { label: 'in1', description: 'Input 1' },
      ],
      output: [{ label: 'out', description: 'Minimum output' }],
    },
  },

  1029: {
    type: 1029,
    family: 'CONTROL',
    name: 'CTRL_MAX',
    displayName: 'Maximum',
    category: 'control',
    description: 'Selects the larger of two signals: out = max(in0, in1)',
    defaultPrefix: 'MAX',
    parameters: [],
    terminals: {
      input: [
        { label: 'in0', description: 'Input 0' },
        { label: 'in1', description: 'Input 1' },
      ],
      output: [{ label: 'out', description: 'Maximum output' }],
    },
  },

  1030: {
    type: 1030,
    family: 'CONTROL',
    name: 'CTRL_HYS',
    displayName: 'Hysteresis',
    category: 'control',
    description: 'Hysteresis comparator with threshold window ±H: outputs +1 when in > +H, -1 when in < -H, and retains previous state inside the [-H, +H] deadband.',
    defaultPrefix: 'HYS',
    parameters: [
      { index: 0, key: 'param0', label: 'Hysteresis Window (H)', description: 'Switching threshold window (switches at +H and -H)', defaultValue: 0.1, unit: '' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input to compare' }],
      output: [{ label: 'out', description: 'Hysteresis state output (+1, -1, or latched)' }],
    },
  },

  1031: {
    type: 1031,
    family: 'CONTROL',
    name: 'CTRL_PT2',
    displayName: 'PT2 Filter',
    category: 'control',
    description: 'Second-order low-pass filter element: G(s) = K / (1 + (τ·s)²).',
    defaultPrefix: 'PT2',
    parameters: [
      { index: 0, key: 'param0', label: 'Time Constant (τ)', description: 'Filter time constant', defaultValue: 0.001, unit: 's' },
      { index: 1, key: 'param1', label: 'Gain (K)', description: 'DC Gain', defaultValue: 1, unit: '' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Filtered output' }],
    },
  },

  1032: {
    type: 1032,
    family: 'CONTROL',
    name: 'CTRL_PD',
    displayName: 'Derivative / PD Element',
    category: 'control',
    description: 'Discrete derivative differentiator: computes discrete time derivative y = (Kd / Δt) · (in - in_old).',
    defaultPrefix: 'PD',
    parameters: [
      { index: 0, key: 'param0', label: 'Derivative Gain (Kd)', description: 'Gain for discrete derivative calculation: y = Kd · dx/dt', defaultValue: 1, unit: '' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Differentiated output' }],
    },
  },

  1033: {
    type: 1033,
    family: 'CONTROL',
    name: 'CTRL_SAMPLEHOLD',
    displayName: 'Sample & Hold',
    category: 'control',
    description: 'Samples input signal on rising edge of clock: in0=data, in1=clock',
    defaultPrefix: 'SH',
    parameters: [],
    terminals: {
      input: [
        { label: 'd', description: 'Data input' },
        { label: 'clk', description: 'Clock / Trigger' },
      ],
      output: [{ label: 'out', description: 'Sampled output' }],
    },
  },

  1034: {
    type: 1034,
    family: 'CONTROL',
    name: 'CTRL_TIME',
    displayName: 'Simulation Time',
    category: 'control',
    description: 'Outputs current simulation elapsed time t in seconds',
    defaultPrefix: 'TIME',
    parameters: [],
    terminals: {
      input: [],
      output: [{ label: 't', description: 'Current simulation time (s)' }],
    },
  },

  1035: {
    type: 1035,
    family: 'CONTROL',
    name: 'CTRL_XOR',
    displayName: 'XOR Gate',
    category: 'logic',
    description: 'Logical XOR — output = 1 when inputs differ',
    defaultPrefix: 'XOR',
    parameters: [],
    terminals: {
      input: [
        { label: 'A', description: 'Input A' },
        { label: 'B', description: 'Input B' },
      ],
      output: [{ label: 'Q', description: 'XOR output' }],
    },
  },

  1036: {
    type: 1036,
    family: 'CONTROL',
    name: 'CTRL_GE',
    displayName: 'Greater-or-Equal (≥)',
    category: 'control',
    description: 'Compares two signals — outputs 1 if in0 ≥ in1, else 0',
    defaultPrefix: 'GE',
    parameters: [],
    terminals: {
      input: [
        { label: 'in0', description: 'Input 0' },
        { label: 'in1', description: 'Input 1' },
      ],
      output: [{ label: 'out', description: 'Binary output (0 or 1)' }],
    },
  },

  1037: {
    type: 1037,
    family: 'CONTROL',
    name: 'CTRL_DEADTIME',
    displayName: 'PWM Dead-Time',
    category: 'control',
    description: 'Generates complementary high-side and low-side gate signals with dead-time band',
    defaultPrefix: 'DT',
    parameters: [
      { index: 0, key: 'param0', label: 'Dead Time (t_dead)', description: 'Dead time window in seconds', defaultValue: 200e-9, unit: 's' },
    ],
    terminals: {
      input: [{ label: 'pwm', description: 'Input PWM signal' }],
      output: [
        { label: 'hi', description: 'High-side gate drive' },
        { label: 'lo', description: 'Low-side gate drive' },
      ],
    },
  },

  61: {
    type: 61,
    family: 'CONTROL',
    name: 'C_JAVA_FUNCTION',
    displayName: 'Java Block (Classic)',
    category: 'control',
    description: 'Classic GeckoCIRCUITS Java programmable function block',
    defaultPrefix: 'JAVA',
    parameters: [
      { index: 0, key: 'sourceCode', label: 'Java Code', description: 'Calculates yOUT from xIN signals in Java syntax', defaultValue: 'yOUT[0] = xIN[0];' },
      { index: 1, key: 'anzXIN', label: 'Inputs', description: 'Number of input signals', defaultValue: 1 },
      { index: 2, key: 'anzYOUT', label: 'Outputs', description: 'Number of output signals', defaultValue: 1 },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Signal output' }],
    },
  },
  88: {
    type: 88,
    family: 'CONTROL',
    name: 'C_NATIVE_C_FUNCTION',
    displayName: 'C Library Block (NativeC)',
    category: 'control',
    description: 'Firmware-in-the-loop: binds a self-built host shared library (dll/so/dylib) implementing the gecko_c_block.h contract (gecko_init/gecko_step/gecko_deinit). Build your MCU control code with your own toolchain; the library file is copied per simulation run, giving C statics a power-on reset.',
    defaultPrefix: 'CNATC',
    parameters: [
      { index: 0, key: 'libraryPath', label: 'Library Path', description: 'Path to the shared library (dll/so/dylib) implementing gecko_step; relative paths resolve against the circuit file', defaultValue: '' },
      { index: 1, key: 'anzXIN', label: 'Inputs', description: 'Number of input signals (n_in)', defaultValue: 3 },
      { index: 2, key: 'anzYOUT', label: 'Outputs', description: 'Number of output signals (n_out)', defaultValue: 2 },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Signal output' }],
    },
  },

  1038: {
    type: 1038,
    family: 'CONTROL',
    name: 'CTRL_ASIN',
    displayName: 'Arc Sine (asin)',
    category: 'control',
    description: 'Trigonometric arc sine function: y = asin(u) in radians',
    defaultPrefix: 'ASIN',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input signal [-1..1]' }],
      output: [{ label: 'out', description: 'Output angle in radians' }],
    },
  },
  1039: {
    type: 1039,
    family: 'CONTROL',
    name: 'CTRL_ACOS',
    displayName: 'Arc Cosine (acos)',
    category: 'control',
    description: 'Trigonometric arc cosine function: y = acos(u) in radians',
    defaultPrefix: 'ACOS',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input signal [-1..1]' }],
      output: [{ label: 'out', description: 'Output angle in radians' }],
    },
  },
  1040: {
    type: 1040,
    family: 'CONTROL',
    name: 'CTRL_TAN',
    displayName: 'Tangent (tan)',
    category: 'control',
    description: 'Trigonometric tangent function: y = tan(u)',
    defaultPrefix: 'TAN',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input angle in radians' }],
      output: [{ label: 'out', description: 'Tangent output' }],
    },
  },
  1041: {
    type: 1041,
    family: 'CONTROL',
    name: 'CTRL_ATAN',
    displayName: 'Arc Tangent (atan)',
    category: 'control',
    description: 'Trigonometric arc tangent function: y = atan(u) in radians',
    defaultPrefix: 'ATAN',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input signal' }],
      output: [{ label: 'out', description: 'Output angle in radians' }],
    },
  },
  1042: {
    type: 1042,
    family: 'CONTROL',
    name: 'CTRL_SQR',
    displayName: 'Square (x²)',
    category: 'control',
    description: 'Square function: y = u²',
    defaultPrefix: 'SQR',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input signal' }],
      output: [{ label: 'out', description: 'Squared output' }],
    },
  },
  1043: {
    type: 1043,
    family: 'CONTROL',
    name: 'CTRL_POW',
    displayName: 'Power (xʸ)',
    category: 'control',
    description: 'Power function: y = in0^in1',
    defaultPrefix: 'POW',
    parameters: [],
    terminals: {
      input: [
        { label: 'in0', description: 'Base input' },
        { label: 'in1', description: 'Exponent input' },
      ],
      output: [{ label: 'out', description: 'Power output' }],
    },
  },
  1044: {
    type: 1044,
    family: 'CONTROL',
    name: 'CTRL_ROUND',
    displayName: 'Round',
    category: 'control',
    description: 'Rounds input signal to the nearest integer: y = round(u)',
    defaultPrefix: 'RND',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input signal' }],
      output: [{ label: 'out', description: 'Rounded integer output' }],
    },
  },
  1045: {
    type: 1045,
    family: 'CONTROL',
    name: 'CTRL_SIGN',
    displayName: 'Signum (sgn)',
    category: 'control',
    description: 'Sign function: returns 1 for u > 0, -1 for u < 0, 0 for u == 0',
    defaultPrefix: 'SGN',
    parameters: [],
    terminals: {
      input: [{ label: 'in', description: 'Input signal' }],
      output: [{ label: 'out', description: 'Sign output (-1, 0, 1)' }],
    },
  },
  1046: {
    type: 1046,
    family: 'CONTROL',
    name: 'CTRL_EQ',
    displayName: 'Equal (==)',
    category: 'control',
    description: 'Equality comparator: outputs 1 if in0 == in1, else 0',
    defaultPrefix: 'EQ',
    parameters: [],
    terminals: {
      input: [
        { label: 'in0', description: 'Input 0' },
        { label: 'in1', description: 'Input 1' },
      ],
      output: [{ label: 'out', description: 'Binary output (0 or 1)' }],
    },
  },
  1047: {
    type: 1047,
    family: 'CONTROL',
    name: 'CTRL_NE',
    displayName: 'Not Equal (!=)',
    category: 'control',
    description: 'Inequality comparator: outputs 1 if in0 != in1, else 0',
    defaultPrefix: 'NE',
    parameters: [],
    terminals: {
      input: [
        { label: 'in0', description: 'Input 0' },
        { label: 'in1', description: 'Input 1' },
      ],
      output: [{ label: 'out', description: 'Binary output (0 or 1)' }],
    },
  },
  1048: {
    type: 1048,
    family: 'CONTROL',
    name: 'CTRL_COUNTER',
    displayName: 'Counter',
    category: 'control',
    description: 'Digital pulse counter: increments on rising edges of clock (threshold 0.5); resets to zero when reset > 0.5.',
    defaultPrefix: 'CNT',
    parameters: [],
    terminals: {
      input: [
        { label: 'clk', description: 'Clock / pulse input (increments count on rising edge >= 0.5)' },
        { label: 'reset', description: 'Reset trigger input (resets count to 0 when > 0.5)' },
      ],
      output: [{ label: 'cnt', description: 'Integer counter value output' }],
    },
  },
  1049: {
    type: 1049,
    family: 'CONTROL',
    name: 'CTRL_ABCDQ',
    displayName: 'abc → dq0 Transform',
    category: 'control',
    description: 'Park/Clarke transformation from 3-phase stationary coordinates (a, b, c) to rotating orthogonal coordinates (d, q, 0) using rotor angle theta.',
    defaultPrefix: 'DQ',
    parameters: [],
    terminals: {
      input: [
        { label: 'a', description: 'Phase a input signal' },
        { label: 'b', description: 'Phase b input signal' },
        { label: 'c', description: 'Phase c input signal' },
        { label: 'theta', description: 'Rotor transformation angle theta in radians' },
      ],
      output: [
        { label: 'd', description: 'Direct-axis output (d)' },
        { label: 'q', description: 'Quadrature-axis output (q)' },
        { label: 'zero', description: 'Zero-sequence output (0)' },
      ],
    },
  },
  1050: {
    type: 1050,
    family: 'CONTROL',
    name: 'CTRL_DQABC',
    displayName: 'dq0 → abc Transform',
    category: 'control',
    description: 'Inverse Park/Clarke transformation reconstructing 3-phase stationary signals (a, b, c) from rotating orthogonal coordinates (d, q, 0) using rotor angle theta.',
    defaultPrefix: 'IQ',
    parameters: [],
    terminals: {
      input: [
        { label: 'd', description: 'Direct-axis input (d)' },
        { label: 'q', description: 'Quadrature-axis input (q)' },
        { label: 'zero', description: 'Zero-sequence input (0)' },
        { label: 'theta', description: 'Rotor transformation angle theta in radians' },
      ],
      output: [
        { label: 'a', description: 'Phase a reconstructed output' },
        { label: 'b', description: 'Phase b reconstructed output' },
        { label: 'c', description: 'Phase c reconstructed output' },
      ],
    },
  },
  1051: {
    type: 1051,
    family: 'CONTROL',
    name: 'CTRL_THYR_CTRL',
    displayName: 'Thyristor Control',
    category: 'control',
    description: 'Firing angle generator for line-commutated 3-phase thyristor bridge (B6C). Synchronizes to AC mains via uSync and outputs 6 gate pulses for thyristors 1 to 6.',
    defaultPrefix: 'THC',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Gate On Time',
        description: 'Duration of gate firing pulses in seconds',
        defaultValue: 0.004,
        unit: 's',
        min: 0.0001,
        max: 0.1,
      },
      {
        index: 1,
        key: 'param1',
        label: 'Line Frequency',
        description: 'AC mains synchronization frequency in Hertz',
        defaultValue: 50.0,
        unit: 'Hz',
        min: 1.0,
        max: 1000.0,
      },
      {
        index: 2,
        key: 'param2',
        label: 'Phase Shift',
        description: 'Synchronization phase shift angle in degrees',
        defaultValue: 30.0,
        unit: 'deg',
        min: -180.0,
        max: 180.0,
      },
    ],
    terminals: {
      input: [
        { label: 'uSync', description: 'Line synchronization phase voltage (e.g. Thyristor A1 phase voltage)' },
        { label: 'alpha', description: 'Firing delay angle alpha in degrees' },
      ],
      output: [
        { label: 'gate1', description: 'Thyristor 1 (Phase A positive) gate pulse' },
        { label: 'gate2', description: 'Thyristor 2 (Phase B positive) gate pulse' },
        { label: 'gate3', description: 'Thyristor 3 (Phase C positive) gate pulse' },
        { label: 'gate4', description: 'Thyristor 4 (Phase A negative) gate pulse' },
        { label: 'gate5', description: 'Thyristor 5 (Phase B negative) gate pulse' },
        { label: 'gate6', description: 'Thyristor 6 (Phase C negative) gate pulse' },
      ],
    },
  },
  1052: {
    type: 1052,
    family: 'CONTROL',
    name: 'CTRL_PMSM_CONTROL',
    displayName: 'PMSM FOC Controller',
    category: 'control',
    description: 'Field-Oriented Control (FOC) for permanent magnet synchronous machines. Cascaded speed and d-q current PI loops with Clarke & Park transformations. Gains can be wired or set via parameters.',
    defaultPrefix: 'FOC',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Speed Kp (kpN)',
        description: 'Speed PI controller proportional gain (used if pin kpN unwired)',
        defaultValue: 2.0,
        min: 0,
      },
      {
        index: 1,
        key: 'param1',
        label: 'Speed Tn (tN)',
        description: 'Speed PI controller integral time constant in seconds (used if pin tN unwired)',
        defaultValue: 0.05,
        unit: 's',
        min: 0.0001,
      },
      {
        index: 2,
        key: 'param2',
        label: 'Iq Limit (nLimit)',
        description: 'Speed controller output limit / maximum q-axis reference current in Amperes',
        defaultValue: 20.0,
        unit: 'A',
        min: 0,
      },
      {
        index: 3,
        key: 'param3',
        label: 'Current Kp (kpI)',
        description: 'Current PI controller proportional gain (used if pin kpI unwired)',
        defaultValue: 5.0,
        min: 0,
      },
      {
        index: 4,
        key: 'param4',
        label: 'Current Ti (tI)',
        description: 'Current PI controller integral time constant in seconds (used if pin tI unwired)',
        defaultValue: 0.01,
        unit: 's',
        min: 0.0001,
      },
      {
        index: 5,
        key: 'param5',
        label: 'Voltage Limit (iLimit)',
        description: 'Current controller output limit / maximum voltage command in Volts',
        defaultValue: 400.0,
        unit: 'V',
        min: 0,
      },
    ],
    terminals: {
      input: [
        { label: 'ia', description: 'Stator current phase A (A)' },
        { label: 'ib', description: 'Stator current phase B (A)' },
        { label: 'omega', description: 'Mechanical rotor speed ω in rad/s' },
        { label: 'phi', description: 'Electrical rotor angle φ in radians' },
        { label: 'nRef', description: 'Rotor speed setpoint n* in rpm' },
        { label: 'kpN', description: 'Speed PI proportional gain (or configure via Parameters)' },
        { label: 'tN', description: 'Speed PI integration time Tn in s (or configure via Parameters)' },
        { label: 'nLim', description: 'Speed controller output limit / Iq max in A' },
        { label: 'kpI', description: 'Current PI proportional gain (or configure via Parameters)' },
        { label: 'tI', description: 'Current PI integration time Ti in s (or configure via Parameters)' },
        { label: 'iLim', description: 'Current controller output limit / V max in V' },
        { label: 'fP', description: 'Controller enable flag (> 0.999 = active, <= 0.999 = freeze)' },
      ],
      output: [
        { label: 'valpha', description: 'Stator voltage command v_alpha (V)' },
        { label: 'vbeta', description: 'Stator voltage command v_beta (V)' },
        { label: 'vqRef', description: 'q-axis voltage command Vq* (V)' },
        { label: 'vdRef', description: 'd-axis voltage command Vd* (V)' },
        { label: 'iqRef', description: 'q-axis current setpoint Iq* (A)' },
        { label: 'idRef', description: 'd-axis current setpoint Id* = 0 (A)' },
        { label: 'iq', description: 'Measured q-axis current Iq (A)' },
        { label: 'id', description: 'Measured d-axis current Id (A)' },
      ],
    },
  },
  1053: {
    type: 1053,
    family: 'CONTROL',
    name: 'CTRL_PMSM_MODULATOR',
    displayName: 'PMSM Modulator',
    category: 'control',
    description: 'Space vector PWM modulator for three-phase PMSM inverters. Compares sector dwell times against triangular carrier to synthesize switching commands.',
    defaultPrefix: 'MOD',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'DC-Link Voltage (Vdc)',
        description: 'DC link voltage in Volts (used if vdc pin is unwired)',
        defaultValue: 600.0,
        unit: 'V',
        min: 1.0,
      },
    ],
    terminals: {
      input: [
        { label: 'valpha', description: 'Stationary alpha voltage command (V)' },
        { label: 'vbeta', description: 'Stationary beta voltage command (V)' },
        { label: 'tri', description: 'Triangular carrier waveform (-1 to +1)' },
        { label: 'vdc', description: 'DC-link voltage Vdc in Volts' },
      ],
      output: [
        { label: 'sA', description: 'Phase A / U gate switching command (0 or 1)' },
        { label: 'sB', description: 'Phase B / V gate switching command (0 or 1)' },
        { label: 'sC', description: 'Phase C / W gate switching command (0 or 1)' },
      ],
    },
  },
  1054: {
    type: 1054,
    family: 'CONTROL',
    name: 'CTRL_DEMUX',
    displayName: 'Demultiplexer (Demux)',
    category: 'control',
    description: 'Demultiplexer / bus splitter: extracts individual scalar output channels from an input vector signal (from script blocks or multi-channel buses).',
    defaultPrefix: 'DMX',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Output Channels',
        description: 'Number of scalar output channels extracted from the input vector',
        defaultValue: 3,
        min: 1,
        max: 16,
      },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Multiplexed vector input signal' }],
      output: [
        { label: 'out0', description: 'Channel 0 scalar output' },
        { label: 'out1', description: 'Channel 1 scalar output' },
        { label: 'out2', description: 'Channel 2 scalar output' },
      ],
    },
  },
  1055: {
    type: 1055,
    family: 'CONTROL',
    name: 'CTRL_SPACE_VECTOR',
    displayName: 'Space Vector Modulator',
    category: 'control',
    description: 'Space vector PWM modulator for three-phase converter systems: converts stationary orthogonal alpha-beta voltage commands into inverter gate switching states.',
    defaultPrefix: 'SVM',
    parameters: [],
    terminals: {
      input: [
        { label: 'valpha', description: 'Stationary alpha voltage command (V)' },
        { label: 'vbeta', description: 'Stationary beta voltage command (V)' },
      ],
      output: [
        { label: 'sA', description: 'Phase A gate switching command' },
        { label: 'sB', description: 'Phase B gate switching command' },
        { label: 'sC', description: 'Phase C gate switching command' },
      ],
    },
  },
  1056: {
    type: 1056,
    family: 'CONTROL',
    name: 'CTRL_SDFT',
    displayName: 'Sliding DFT',
    category: 'control',
    description: 'Sliding Discrete Fourier Transform (SDFT) tracking the fundamental harmonic amplitude in real time with single-step circular buffer recursion.',
    defaultPrefix: 'SDFT',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Averaging Span',
        description: 'Sliding window integration time span in seconds (typically 1 / f0 = 0.02s)',
        defaultValue: 0.02,
        unit: 's',
        min: 0.0001,
      },
      {
        index: 1,
        key: 'param1',
        label: 'Frequency (f0)',
        description: 'Fundamental tracking harmonic frequency in Hertz',
        defaultValue: 50.0,
        unit: 'Hz',
        min: 1.0,
        max: 10000.0,
      },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Input signal to analyze' }],
      output: [{ label: 'mag', description: 'Fundamental harmonic magnitude output' }],
    },
  },
  1058: {
    type: 1058,
    family: 'CONTROL',
    name: 'CTRL_SPARSEMATRIX',
    displayName: 'Sparse Matrix Converter Modulator',
    category: 'control',
    description: 'Space Vector PWM Modulator for Indirect Sparse Matrix Converters (ISMC). Generates 6 rectifier and 3 inverter switching commands with zero-current switching (ZCS).',
    defaultPrefix: 'SPM',
    parameters: [],
    terminals: {
      input: [
        { label: 'fDR', description: 'Carrier / switching frequency reference' },
        { label: 'ur', description: 'Input mains phase R voltage' },
        { label: 'us', description: 'Input mains phase S voltage' },
        { label: 'ut', description: 'Input mains phase T voltage' },
        { label: 'uNmax', description: 'Input AC mains peak amplitude' },
        { label: 'uOUTmax', description: 'Desired peak output voltage amplitude' },
        { label: 'fOUT', description: 'Desired output frequency in Hz' },
        { label: 'phi2', description: 'Output vector reference angle in radians' },
      ],
      output: [
        { label: 'sRp', description: 'Rectifier stage R positive switch' },
        { label: 'sSp', description: 'Rectifier stage S positive switch' },
        { label: 'sTp', description: 'Rectifier stage T positive switch' },
        { label: 'sRm', description: 'Rectifier stage R negative switch' },
        { label: 'sSm', description: 'Rectifier stage S negative switch' },
        { label: 'sTm', description: 'Rectifier stage T negative switch' },
        { label: 's1', description: 'Inverter stage phase 1 switch' },
        { label: 's2', description: 'Inverter stage phase 2 switch' },
        { label: 's3', description: 'Inverter stage phase 3 switch' },
      ],
    },
  },
  2001: {
    type: 2001,
    family: 'SPECIAL',
    name: 'TEXTBOX',
    displayName: 'Note / Textbox',
    category: 'documentation',
    description: 'Rich Markdown documentation note rendered directly on the schematic canvas for circuit explanations and diagrams.',
    shortcut: 'N',
    defaultPrefix: 'NOTE',
    parameters: [
      {
        index: 0,
        key: 'text',
        label: 'Markdown Text',
        description: 'Formatted markdown documentation content displayed inside the note card.',
        defaultValue: '## Circuit Notes\nWrite markdown documentation here.',
      },
      {
        index: 1,
        key: 'width',
        label: 'Box Width',
        description: 'Card width in grid units.',
        defaultValue: 26,
        min: 6,
        max: 100,
        step: 1,
      },
      {
        index: 2,
        key: 'height',
        label: 'Box Height',
        description: 'Card height in grid units.',
        defaultValue: 14,
        min: 4,
        max: 80,
        step: 1,
      },
    ],
    terminals: {
      input: [],
      output: [],
    },
  },
};

/**
 * Checks whether a component is a rich Markdown documentation note / textbox.
 */
export function isTextboxComponent(component: {
  type: number;
  family?: string;
  name?: string;
} | null | undefined): boolean {
  if (!component) return false;
  return (
    component.type === 2001 ||
    component.type === SpecialComponentType.TEXTBOX ||
    component.family === 'SPECIAL' ||
    component.family === 'ANNOTATION' ||
    (typeof component.name === 'string' && component.name.startsWith('NOTE'))
  );
}

/**
 * Returns metadata for a component, falling back to a generic definition if unknown.
 */
export function getComponentMeta(
  type: number,
  family: string = 'LK',
  nameHint: string = '',
): ComponentMeta {
  let lookupType = type;
  if (family === 'SPECIAL' || family === 'ANNOTATION' || (nameHint && nameHint.startsWith('NOTE'))) {
    lookupType = 2001;
  } else if (family === 'CONTROL') {
    if (type === 1) lookupType = 1001; // Voltmeter
    else if (type === 2) lookupType = 1002; // Ammeter
    else if (type === 3) lookupType = 1005; // Constant
    else if (type === 4) lookupType = 1004; // Signal Source
    else if (type === 5) lookupType = 1003; // Scope
    else if (type === 6) lookupType = 1000; // Gate Driver
    else if (type === 7) lookupType = 1006; // Gain
    else if (type === 8) lookupType = 1008; // PT1
    else if (type === 9) lookupType = 1031; // PT2
    else if (type === 10) lookupType = 1007; // PI
    else if (type === 11) lookupType = 1030; // Hysteresis
    else if (type === 12) lookupType = 1018; // Add
    else if (type === 13) lookupType = 1017; // Sub
    else if (type === 14) lookupType = 1019; // Mul
    else if (type === 15) lookupType = 1020; // Div
    else if (type === 18) lookupType = 1013; // Not
    else if (type === 19) lookupType = 1011; // And
    else if (type === 20) lookupType = 1012; // Or
    else if (type === 21) lookupType = 1035; // Xor
    else if (type === 25) lookupType = 1015; // Delay
    else if (type === 26) lookupType = 1033; // SampleHold
    else if (type === 27) lookupType = 1021; // Limit
    else if (type === 29) lookupType = 1032; // PD
    else if (type === 32) lookupType = 1022; // Abs
    else if (type === 33) lookupType = 1044; // Round
    else if (type === 34) lookupType = 1026; // Sin
    else if (type === 35) lookupType = 1038; // ASIN
    else if (type === 36) lookupType = 1027; // Cos
    else if (type === 37) lookupType = 1039; // ACOS
    else if (type === 38) lookupType = 1040; // TAN
    else if (type === 39) lookupType = 1041; // ATAN
    else if (type === 40) lookupType = 1024; // Exp
    else if (type === 41) lookupType = 1025; // Ln
    else if (type === 42) lookupType = 1042; // SQR
    else if (type === 43) lookupType = 1023; // Sqrt
    else if (type === 44) lookupType = 1043; // POW
    else if (type === 45) lookupType = 1036; // GE
    else if (type === 46) lookupType = 1010; // GT (Comparator)
    else if (type === 47) lookupType = 1046; // EQ
    else if (type === 48) lookupType = 1047; // NE
    else if (type === 49) lookupType = 1028; // Min
    else if (type === 50) lookupType = 1029; // Max
    else if (type === 51) lookupType = 1045; // SIGN
    else if (type === 53) lookupType = 1048; // COUNTER
    else if (type === 58) lookupType = 1034; // Time
    else if (type === 59) lookupType = 1058; // SPARSEMATRIX
    else if (type === 63) lookupType = 1055; // SPACE_VECTOR
    else if (type === 64) lookupType = 1009; // Integrator
    else if (type === 65) lookupType = 1049; // ABCDQ
    else if (type === 66) lookupType = 1050; // DQABC
    else if (type === 72) lookupType = 1051; // THYR_CTRL
    else if (type === 76) lookupType = 1052; // PMSM_CONTROL
    else if (type === 77) lookupType = 1053; // PMSM_MODULATOR
    else if (type === 82) lookupType = 1056; // SDFT
    else if (type === 84) lookupType = 1014; // Selector / Mux
    else if (type === 85) lookupType = 1054; // DEMUX
  }
  const existing = COMPONENT_METAS[lookupType] ?? COMPONENT_METAS[type];
  if (existing) {
    return existing;
  }

  // Generic fallback
  const cleanName = nameHint ? nameHint.replace(/^[a-zA-Z]+_/, '') : `TYPE_${type}`;
  return {
    type,
    family,
    name: nameHint || `${family}_${type}`,
    displayName: cleanName.charAt(0).toUpperCase() + cleanName.slice(1),
    category: family === 'THERM' ? 'thermal' : 'passives',
    description: `Generic component of type ${type} in domain ${family}`,
    defaultPrefix: 'COMP',
    parameters: [
      {
        index: 0,
        key: 'param0',
        label: 'Parameter 0',
        description: 'Primary value',
        defaultValue: 10.0,
        unit: '',
      },
      {
        index: 1,
        key: 'param1',
        label: 'Parameter 1',
        description: 'Secondary value',
        defaultValue: 0.0,
        unit: '',
      },
    ],
    terminals: {
      input: [{ label: '1', description: 'Terminal 1' }],
      output: [{ label: '2', description: 'Terminal 2' }],
    },
  };
}

/**
 * Common engineering multipliers for SI units.
 */
const SI_PREFIXES: Record<string, number> = {
  f: 1e-15,
  p: 1e-12,
  n: 1e-9,
  u: 1e-6,
  µ: 1e-6,
  m: 1e-3,
  k: 1e3,
  K: 1e3,
  M: 1e6,
  G: 1e9,
  T: 1e12,
};

/**
 * Parses user input strings with engineering SI notation into standard double.
 * Examples: '10k' -> 10000, '4.7u' -> 4.7e-6, '100n' -> 1e-7, '1.5M' -> 1.5e6, '22m' -> 0.022.
 */
export function parseEngineeringValue(input: string): number | null {
  if (!input || typeof input !== 'string') return null;
  const trimmed = input.trim().replace(/\s+/g, '').replace(/−/g, '-');
  if (!trimmed) return null;

  // Direct number or exponential notation (e.g. 1e-3, 0.05, -12.4)
  const num = Number(trimmed);
  if (Number.isFinite(num)) {
    return num;
  }

  // Engineering shorthand with implicit decimal point: 4k7 = 4.7k = 4700,
  // 100n1 = 100.1n. The digit after the SI prefix is the first decimal.
  const shorthand = /^([+-]?[0-9]+)([fpnuµmkKMGT])([0-9])([0-9]*)$/.exec(trimmed);
  if (shorthand) {
    const baseVal = Number(shorthand[1]);
    const prefix = shorthand[2];
    const decimals = Number("0." + shorthand[3] + (shorthand[4] || ""));
    if (Number.isFinite(baseVal) && SI_PREFIXES[prefix] !== undefined) {
      return (baseVal + decimals) * SI_PREFIXES[prefix];
    }
  }

  // Check for number + optional SI prefix + optional unit (e.g. 10k, 10kΩ, 100uF, 24V, 50Hz, 10A, 20%)
  const match = /^([+-]?[0-9]*\.?[0-9]+(?:[eE][+-]?[0-9]+)?)([fpnuµmkKMGT])?([a-zA-ZΩ°%]+)?$/.exec(trimmed);
  if (match) {
    const baseVal = Number(match[1]);
    const prefix = match[2];
    const unit = match[3];
    if (Number.isFinite(baseVal)) {
      let val = baseVal;
      if (prefix && SI_PREFIXES[prefix] !== undefined) {
        val *= SI_PREFIXES[prefix];
      }
      if (unit === '%') {
        val /= 100;
      }
      return val;
    }
  }

  return null;
}

/**
 * Formats a numeric value into a concise human-readable string with SI prefix.
 * e.g. 10000 -> '10 k', 0.0000047 -> '4.7 µ', 0.001 -> '1 m'.
 */
export function formatEngineeringValue(value: number, unit = ''): string {
  if (!Number.isFinite(value)) return '—';
  if (value === 0) return unit ? `0 ${unit}` : '0';

  const abs = Math.abs(value);
  const sign = value < 0 ? '−' : '';

  if (abs >= 1e9) {
    return `${sign}${roundDec(abs / 1e9, 3)} G${unit ? ' ' + unit : ''}`;
  }
  if (abs >= 1e6) {
    return `${sign}${roundDec(abs / 1e6, 3)} M${unit ? ' ' + unit : ''}`;
  }
  if (abs >= 1e3) {
    return `${sign}${roundDec(abs / 1e3, 3)} k${unit ? ' ' + unit : ''}`;
  }
  if (abs >= 1) {
    return `${sign}${roundDec(abs, 3)}${unit ? ' ' + unit : ''}`;
  }
  // 0.1 … 1 stays a plain decimal: these are usually dimensionless ratios
  // (coupling factor k, duty cycle) where "999 m" would read as milli-units.
  if (abs >= 0.1) {
    return `${sign}${roundDec(abs, 3)}${unit ? ' ' + unit : ''}`;
  }
  if (abs >= 1e-3) {
    return `${sign}${roundDec(abs * 1e3, 3)} m${unit ? ' ' + unit : ''}`;
  }
  if (abs >= 1e-6) {
    return `${sign}${roundDec(abs * 1e6, 3)} µ${unit ? ' ' + unit : ''}`;
  }
  if (abs >= 1e-9) {
    return `${sign}${roundDec(abs * 1e9, 3)} n${unit ? ' ' + unit : ''}`;
  }
  if (abs >= 1e-12) {
    return `${sign}${roundDec(abs * 1e12, 3)} p${unit ? ' ' + unit : ''}`;
  }

  return `${sign}${value.toExponential(2)}${unit ? ' ' + unit : ''}`;
}

function roundDec(val: number, decimals: number): number {
  const factor = 10 ** decimals;
  return Math.round(val * factor) / factor;
}

export function isGateDriver(component: { type: number; family?: string; name?: string } | null | undefined): boolean {
  if (!component) return false;
  return (
    component.type === CTRL_TYPE.GATE ||
    component.type === 1000 ||
    (component.family === 'CONTROL' && component.type === CTRL_TYPE.LEGACY_GATE) ||
    component.name?.startsWith('GATE') === true
  );
}

export function isSwitchComponent(component: { type: number; family?: string; name?: string } | null | undefined): boolean {
  if (!component) return false;
  return (
    component.type === LkComponentType.IDEAL_SWITCH ||
    component.type === LkComponentType.THYRISTOR ||
    component.type === LkComponentType.IGBT ||
    component.type === LkComponentType.MOSFET ||
    component.type === LkComponentType.BJT ||
    // type 9 (LK_M mutual inductance / transformer) is NOT a gate-controlled
    // switch and must not show the gate-drive coupling section.
    component.name?.startsWith('S.') === true ||
    component.name?.startsWith('S_') === true ||
    component.name?.startsWith('TH.') === true ||
    component.name?.startsWith('IGBT') === true ||
    component.name?.startsWith('MOS') === true
  );
}

export function isAmmeterComponent(component: { type: number; family?: string; name?: string } | null | undefined): boolean {
  if (!component) return false;
  return (
    component.type === ControlComponentType.AMMETER ||
    (component.family === 'CONTROL' && component.type === ControlComponentType.LEGACY_AMMETER) ||
    component.name?.startsWith('AMP') === true ||
    component.name?.startsWith('AMR') === true
  );
}

export function isVoltmeterComponent(component: { type: number; family?: string; name?: string } | null | undefined): boolean {
  if (!component) return false;
  return (
    component.type === ControlComponentType.VOLTMETER ||
    (component.family === 'CONTROL' && component.type === ControlComponentType.LEGACY_VOLTMETER) ||
    component.name?.startsWith('VOLT') === true ||
    component.name?.startsWith('V_meas') === true
  );
}

export function getCoupledComponentName(component: { parameters?: Record<string, unknown> } | null | undefined): string {
  if (!component?.parameters) return '';
  const val = component.parameters.coupledComponent;
  if (!val) return '';
  let str = String(val).trim();
  if (str.startsWith('/')) {
    str = str.substring(1);
  }
  if (str === 'none' || str === SENTINEL_UNSET) return '';
  return str;
}

/**
 * True for the classic mutual-inductance coupler (LK_M, type 9). It is a
 * pinless declaration element — like a SPICE `K` statement, it has no wires;
 * it references the uids of two coupled inductors in raw parameter slots
 * 1 and 2 and declares the coupling factor k in slot 0.
 */
export function isMutualCoupler(
  component: { type: number; family?: string } | null | undefined,
): boolean {
  if (!component) return false;
  return component.family !== 'CONTROL' && component.type === LkComponentType.MUTUAL_INDUCTANCE;
}

/**
 * True for the coupled inductor (LK_LKOP2, type 12), the only element the
 * netlist accepts as a target of a mutual coupler.
 */
export function isCoupledInductor(
  component: { type: number; family?: string } | null | undefined,
): boolean {
  if (!component) return false;
  return component.family !== 'CONTROL' && component.type === LkComponentType.COUPLED_INDUCTOR;
}

/**
 * Raw-parameter uids of the two inductors a mutual coupler references
 * ([param1, param2]). Zeros mean "not configured".
 */
export function getMutualCouplingUids(
  component: { parameters?: Record<string, unknown> } | null | undefined,
): [number, number] {
  const a = Number(component?.parameters?.param1);
  const b = Number(component?.parameters?.param2);
  return [Number.isFinite(a) ? a : 0, Number.isFinite(b) ? b : 0];
}

export interface AvailableSignalMetadata {
  name: string;
  domain: 'electrical' | 'control' | 'thermal' | 'other';
  category: 'voltage' | 'current' | 'control' | 'other';
  sourceDescription: string;
}

export function extractAvailableSignalDetails(
  components: Array<{
    name: string;
    type?: number;
    family?: string;
    parameters?: Record<string, unknown>;
    inputLabels?: string[];
    outputLabels?: string[];
  }>,
  wires?: Array<{ label?: string; type?: string; domain?: string }>,
): AvailableSignalMetadata[] {
  const signalMap = new Map<string, AvailableSignalMetadata>();

  const register = (
    name: string | undefined,
    domain: 'electrical' | 'control' | 'thermal' | 'other',
    category: 'voltage' | 'current' | 'control' | 'other',
    sourceDescription: string,
  ) => {
    if (!name) return;
    const trimmed = name.trim().replace(/^\/+/, '');
    if (!trimmed || trimmed === SENTINEL_UNSET) return;
    if (!signalMap.has(trimmed)) {
      signalMap.set(trimmed, { name: trimmed, domain, category, sourceDescription });
    }
  };

  if (wires) {
    for (const wire of wires) {
      if (wire.label && wire.label.trim() && wire.label.trim() !== SENTINEL_UNSET) {
        const lbl = wire.label.trim();
        const isControl = wire.type === 'CONTROL' || wire.domain === 'CONTROL';
        const domain = isControl ? 'control' : 'electrical';
        let category: 'voltage' | 'current' | 'control' | 'other' = isControl ? 'control' : 'voltage';
        const lower = lbl.toLowerCase();
        if (lower.startsWith('i_') || lower.startsWith('i(') || lower.startsWith('il')) {
          category = 'current';
        } else if (lower.startsWith('v_') || lower.startsWith('v(') || lower.startsWith('u')) {
          category = 'voltage';
        }
        register(lbl, domain, category, `${isControl ? 'Control' : 'Electrical'} Net "${lbl}"`);
      }
    }
  }

  for (const comp of components) {
    const isControl =
      comp.family === 'CONTROL' ||
      (!comp.family &&
        (comp.name.startsWith('VOLT') ||
          comp.name.startsWith('AMP') ||
          comp.name.startsWith('GATE') ||
          comp.name.startsWith('CTRL') ||
          comp.name.startsWith('PI')));
    const isThermal = comp.family === 'THERM';
    const domain = isThermal ? 'thermal' : isControl ? 'control' : 'electrical';

    if (comp.inputLabels) {
      for (const lbl of comp.inputLabels) {
        register(lbl, domain, isControl ? 'control' : 'other', `Terminal on ${comp.name}`);
      }
    }

    if (comp.outputLabels) {
      for (const lbl of comp.outputLabels) {
        let cat: 'voltage' | 'current' | 'control' | 'other' = isControl ? 'control' : 'other';
        if (
          comp.type === ControlComponentType.VOLTMETER ||
          comp.type === ControlComponentType.LEGACY_VOLTMETER ||
          comp.name.startsWith('VOLT') ||
          comp.name.startsWith('V_meas')
        ) {
          cat = 'voltage';
        } else if (
          comp.type === ControlComponentType.AMMETER ||
          comp.type === ControlComponentType.LEGACY_AMMETER ||
          comp.name.startsWith('AMP')
        ) {
          cat = 'current';
        }
        register(lbl, domain, cat, `Output of ${comp.name}`);
      }
    }

    if (
      (!comp.family || comp.family === 'CONTROL') &&
      (comp.type === ControlComponentType.VOLTMETER ||
        comp.type === ControlComponentType.LEGACY_VOLTMETER ||
        comp.type === ControlComponentType.AMMETER ||
        comp.type === ControlComponentType.LEGACY_AMMETER ||
        comp.type === ControlComponentType.SIGNAL_SOURCE ||
        comp.type === ControlComponentType.LEGACY_SIGNAL_SOURCE ||
        comp.type === ControlComponentType.SCRIPT ||
        comp.type === ControlComponentType.LEGACY_JAVA_FUNCTION ||
        comp.type === ControlComponentType.DEADTIME ||
        comp.name.startsWith('V_meas') ||
        comp.name.startsWith('VOLT') ||
        comp.name.startsWith('AMP') ||
        comp.name.startsWith('SIGNAL') ||
        comp.name.startsWith('SCRIPT') ||
        comp.name.startsWith('JAVA'))
    ) {
      const hasOutputLabel = (comp.outputLabels ?? []).some(
        (l) => l?.trim() && l.trim() !== SENTINEL_UNSET,
      );
      if (!hasOutputLabel) {
        const isVolt =
          comp.type === ControlComponentType.VOLTMETER ||
          comp.type === ControlComponentType.LEGACY_VOLTMETER ||
          comp.name.startsWith('VOLT') ||
          comp.name.startsWith('V_meas');
        const isAmp =
          comp.type === ControlComponentType.AMMETER ||
          comp.type === ControlComponentType.LEGACY_AMMETER ||
          comp.name.startsWith('AMP');
        const cat = isVolt ? 'voltage' : isAmp ? 'current' : 'control';
        const isScriptOrDeadtime =
          comp.type === ControlComponentType.SCRIPT ||
          comp.type === ControlComponentType.LEGACY_JAVA_FUNCTION ||
          comp.type === ControlComponentType.DEADTIME ||
          comp.name.startsWith('SCRIPT') ||
          comp.name.startsWith('JAVA');
        const { outputCount } = resolveComponentPinCounts(comp);
        if (isScriptOrDeadtime && outputCount > 1) {
          for (let j = 0; j < outputCount; j++) {
            register(`${comp.name}.${j + 1}`, 'control', cat, `Output ${j + 1} of ${comp.name}`);
          }
        } else {
          register(comp.name, 'control', cat, `Output of ${comp.name}`);
        }
      }
    }
  }

  if (!signalMap.has('0')) {
    signalMap.set('0', {
      name: '0',
      domain: 'electrical',
      category: 'voltage',
      sourceDescription: 'Circuit Ground Reference (0V)',
    });
  }

  return Array.from(signalMap.values()).sort((a, b) =>
    a.name.localeCompare(b.name, undefined, { numeric: true }),
  );
}

export function extractAvailableSignals(
  components: Array<{
    name: string;
    type?: number;
    family?: string;
    inputLabels?: string[];
    outputLabels?: string[];
  }>,
  wires?: Array<{ label?: string; type?: string; domain?: string }>,
): string[] {
  return extractAvailableSignalDetails(components, wires).map((s) => s.name);
}


