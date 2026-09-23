/**
 * Component metadata, categorized groupings, parameter schemas,
 * physical units and engineering SI prefix notation parser/formatter.
 *
 * Mapped to GeckoCIRCUITS classic parameter indexing, types and units.
 */

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
  options?: { label: string; value: number }[];
}

export interface ComponentMeta {
  type: number;
  family: string;
  name: string;
  displayName: string;
  category: 'passives' | 'sources' | 'semiconductors' | 'switches' | 'transformers' | 'machines' | 'thermal' | 'terminals' | 'measurement' | 'control' | 'logic';
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
 * CONTROL block type numbers with special terminal geometry.
 *
 * The classic editor serializes control blocks with legacy numbers
 * (ControlTyp in the Swing code); the web catalog uses the 1001+ range
 * (CircuitTypCore on the server). Both ranges must render and connect:
 * constant and signal source have a single output terminal, gate and
 * scope a single input terminal (see terminalPositions in geometry.ts).
 */
export const CTRL_TYPE = {
  /** Voltmeter probe, legacy classic-editor number. */
  LEGACY_VOLTMETER: 1,
  /** Ammeter probe, legacy classic-editor number. */
  LEGACY_AMMETER: 2,
  /** Constant block, legacy classic-editor number. */
  LEGACY_CONSTANT: 3,
  /** Signal source, legacy classic-editor number. */
  LEGACY_SIGNAL_SOURCE: 4,
  /** Scope, legacy classic-editor number. */
  LEGACY_SCOPE: 5,
  /** Gate input, legacy classic-editor number. */
  LEGACY_GATE: 6,
  /** Gate driver, web catalog number (CircuitTypCore.CTRL_GATE). */
  GATE: 1000,
  /** Voltmeter probe, web catalog number (CircuitTypCore.CTRL_VOLT). */
  VOLTMETER: 1001,
  /** Ammeter probe, web catalog number (CircuitTypCore.CTRL_AMP). */
  AMMETER: 1002,
  /** Scope, web catalog number (CircuitTypCore.CTRL_SCOPE). */
  SCOPE: 1003,
  /** Signal source, web catalog number (CircuitTypCore.CTRL_SIGNAL). */
  SIGNAL_SOURCE: 1004,
  /** Constant block, web catalog number (CircuitTypCore.CTRL_CONSTANT). */
  CONSTANT: 1005,
  /** Classic Java code block, legacy classic-editor number. */
  LEGACY_JAVA_FUNCTION: 61,
  /** Script / Function block, web catalog number (CircuitTypCore.CTRL_SCRIPT). */
  SCRIPT: 1016,
} as const;

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
          { label: 'DC Voltage (401)', value: 401 },
          { label: 'Sinusoidal AC (402)', value: 402 },
          { label: 'Signal Controlled (400)', value: 400 },
          { label: 'Voltage Controlled (399)', value: 399 },
        ],
      },
      {
        index: 1,
        key: 'param1',
        label: 'Voltage (Vdc / Vpk)',
        description: 'DC voltage or AC peak amplitude in Volts',
        defaultValue: 10,
        unit: 'V',
      },
      {
        index: 2,
        key: 'param2',
        label: 'Frequency (f)',
        description: 'AC frequency in Hertz',
        defaultValue: 50,
        unit: 'Hz',
        min: 0,
      },
      {
        index: 3,
        key: 'param3',
        label: 'DC Offset (Voffset)',
        description: 'Constant DC offset added to waveform',
        defaultValue: 0,
        unit: 'V',
      },
      {
        index: 4,
        key: 'param4',
        label: 'Phase Shift (φ)',
        description: 'Phase shift in degrees',
        defaultValue: 0,
        unit: '°',
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
          { label: 'DC Current (401)', value: 401 },
          { label: 'Sinusoidal AC (402)', value: 402 },
          { label: 'Signal Controlled (400)', value: 400 },
        ],
      },
      {
        index: 1,
        key: 'param1',
        label: 'Current (Idc / Ipk)',
        description: 'DC current or AC peak amplitude in Amperes',
        defaultValue: 1,
        unit: 'A',
      },
      {
        index: 2,
        key: 'param2',
        label: 'Frequency (f)',
        description: 'AC frequency in Hertz',
        defaultValue: 50,
        unit: 'Hz',
        min: 0,
      },
      {
        index: 3,
        key: 'param3',
        label: 'DC Offset (Ioffset)',
        description: 'Constant DC offset added to current',
        defaultValue: 0,
        unit: 'A',
      },
      {
        index: 4,
        key: 'param4',
        label: 'Phase Shift (φ)',
        description: 'Phase shift in degrees',
        defaultValue: 0,
        unit: '°',
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
    description: 'Declares magnetic coupling factor k between two coupled inductors (classic LK_M). Mainly from imported classic circuits.',
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
      input: [{ label: 'k', description: 'Coupling declaration' }],
      output: [{ label: 'k', description: 'Coupling declaration' }],
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
    description: 'Inductor winding coupled into the reluctance network',
    defaultPrefix: 'Lrel',
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
      input: [{ label: '1', description: 'Net Connection Pin' }],
      output: [{ label: '1', description: 'Net Connection Pin' }],
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
          { label: 'Rectangle / PWM (404)', value: 404 },
          { label: 'Sine (402)', value: 402 },
          { label: 'Triangle (403)', value: 403 },
          { label: 'Random Noise (405)', value: 405 },
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
      },
      {
        index: 3,
        key: 'param3',
        label: 'DC Offset',
        description: 'Constant vertical offset',
        defaultValue: 0.0,
        unit: 'V',
        step: 0.1,
      },
      {
        index: 4,
        key: 'param4',
        label: 'Phase',
        description: 'Initial phase angle in radians',
        defaultValue: 0.0,
        unit: 'rad',
        step: 0.1,
      },
      {
        index: 5,
        key: 'param5',
        label: 'Duty Cycle (D)',
        description: 'Pulse duty ratio (0..1) for PWM rectangle signal',
        defaultValue: 0.5,
        unit: '',
        min: 0.0,
        max: 1.0,
        step: 0.01,
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
    description: 'Time integration of input signal (∫)',
    defaultPrefix: 'INT',
    parameters: [
      { index: 0, key: 'param0', label: 'Initial Value', description: 'Initial integrator state', defaultValue: 0, unit: '' },
    ],
    terminals: {
      input: [{ label: 'in', description: 'Signal input' }],
      output: [{ label: 'out', description: 'Integrated output' }],
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
    displayName: 'Multiplexer',
    category: 'control',
    description: 'Selects one of N inputs based on a selector signal',
    defaultPrefix: 'MUX',
    parameters: [],
    terminals: {
      input: [
        { label: 'sel', description: 'Selector' },
        { label: 'in0', description: 'Input 0' },
        { label: 'in1', description: 'Input 1' },
      ],
      output: [{ label: 'out', description: 'Selected output' }],
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
};

/**
 * Returns metadata for a component, falling back to a generic definition if unknown.
 */
export function getComponentMeta(
  type: number,
  family: string = 'LK',
  nameHint: string = '',
): ComponentMeta {
  let lookupType = type;
  if (family === 'CONTROL') {
    if (type === 1) lookupType = 1001; // Voltmeter
    else if (type === 2) lookupType = 1002; // Ammeter
    else if (type === 3) lookupType = 1005; // Constant
    else if (type === 4) lookupType = 1004; // Signal Source
    else if (type === 5) lookupType = 1003; // Scope
    else if (type === 6) lookupType = 1000; // Gate Driver
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

  // Check for number + optional SI prefix + optional unit (e.g. 10k, 10kΩ, 100uF, 24V, 50Hz, 10A)
  const match = /^([+-]?[0-9]*\.?[0-9]+(?:[eE][+-]?[0-9]+)?)([fpnuµmkKMGT])?([a-zA-ZΩ°%]+)?$/.exec(trimmed);
  if (match) {
    const baseVal = Number(match[1]);
    const prefix = match[2];
    if (Number.isFinite(baseVal)) {
      if (prefix && SI_PREFIXES[prefix] !== undefined) {
        return baseVal * SI_PREFIXES[prefix];
      }
      return baseVal;
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
    component.type === 7 ||
    component.type === 8 ||
    component.type === 10 ||
    component.type === 11 ||
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
    component.type === CTRL_TYPE.AMMETER ||
    component.type === 1002 ||
    (component.family === 'CONTROL' && (component.type === CTRL_TYPE.LEGACY_AMMETER || component.type === 2)) ||
    component.name?.startsWith('AMP') === true ||
    component.name?.startsWith('AMR') === true
  );
}

export function isVoltmeterComponent(component: { type: number; family?: string; name?: string } | null | undefined): boolean {
  if (!component) return false;
  return (
    component.type === CTRL_TYPE.VOLTMETER ||
    component.type === 1001 ||
    (component.family === 'CONTROL' && (component.type === CTRL_TYPE.LEGACY_VOLTMETER || component.type === 1)) ||
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
  if (str === 'none' || str === 'NIX_NIX_NIX') return '';
  return str;
}

export function extractAvailableSignals(
  components: Array<{
    name: string;
    type?: number;
    family?: string;
    inputLabels?: string[];
    outputLabels?: string[];
  }>,
  wires?: Array<{ label?: string }>,
): string[] {
  const set = new Set<string>();

  for (const comp of components) {
    if (comp.inputLabels) {
      for (const lbl of comp.inputLabels) {
        const trimmed = lbl?.trim();
        if (trimmed && trimmed !== 'NIX_NIX_NIX') set.add(trimmed);
      }
    }
    if (comp.outputLabels) {
      for (const lbl of comp.outputLabels) {
        const trimmed = lbl?.trim();
        if (trimmed && trimmed !== 'NIX_NIX_NIX') set.add(trimmed);
      }
    }
    if (
      // legacy .ipes type numbers collide with LK types (1 = resistor AND
      // voltmeter) - without the family guard every R/L/C name leaked into
      // the signal list
      (!comp.family || comp.family === 'CONTROL') &&
      (comp.type === CTRL_TYPE.VOLTMETER ||
        comp.type === CTRL_TYPE.LEGACY_VOLTMETER ||
        comp.type === CTRL_TYPE.AMMETER ||
        comp.type === CTRL_TYPE.LEGACY_AMMETER ||
        comp.type === CTRL_TYPE.SIGNAL_SOURCE ||
        comp.type === CTRL_TYPE.LEGACY_SIGNAL_SOURCE ||
        comp.name.startsWith('V_meas') ||
        comp.name.startsWith('VOLT') ||
        comp.name.startsWith('AMP') ||
        comp.name.startsWith('SIGNAL'))
    ) {
      // A measurement block is reachable under its component name too, but
      // when it has an output label that label is the canonical signal name.
      // Offering both lets the identical curve show up twice under different
      // names in scope-channel pickers, so only label-less blocks fall back
      // to their component name.
      const hasOutputLabel = (comp.outputLabels ?? []).some(
        (l) => l?.trim() && l.trim() !== 'NIX_NIX_NIX',
      );
      if (!hasOutputLabel) {
        set.add(comp.name);
      }
    }
  }

  if (wires) {
    for (const wire of wires) {
      const trimmed = wire.label?.trim();
      if (trimmed && trimmed !== 'NIX_NIX_NIX') {
        set.add(trimmed);
      }
    }
  }

  set.add('0');
  return Array.from(set).sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
}

