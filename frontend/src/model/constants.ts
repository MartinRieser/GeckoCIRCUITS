/**
 * Domain constants, enumeration types, raster metrics, and sentinel values
 * for the GeckoCIRCUITS schematic editor and simulation runtime.
 */

/**
 * Standard four-way spatial orientations used by GeckoCIRCUITS (.ipes format).
 * The codes match the legacy Swing editor's Orientation enum values:
 * 501 SOUTH_NORTH, 502 WEST_EAST, 503 NORTH_SOUTH, 504 EAST_WEST.
 */
export enum Orientation {
  /** Input terminal at bottom, output at top (flow directed south-to-north). */
  SOUTH_NORTH = 501,
  /** Input terminal at left, output at right (flow directed west-to-east). Default standard. */
  WEST_EAST = 502,
  /** Input terminal at top, output at bottom (flow directed north-to-south). */
  NORTH_SOUTH = 503,
  /** Input terminal at right, output at left (flow directed east-to-west). */
  EAST_WEST = 504,
}

/** Standard rotation cycle when pressing 'R' or right-clicking to rotate. */
export const ORIENTATION_CYCLE = [
  Orientation.NORTH_SOUTH,
  Orientation.EAST_WEST,
  Orientation.SOUTH_NORTH,
  Orientation.WEST_EAST,
] as const;

/**
 * Electrical (LK), magnetic reluctance, and thermal component type IDs.
 * Aligns with classic CircuitTypCore / LK_* definitions in the core engine.
 */
export enum LkComponentType {
  RESISTOR = 1,
  INDUCTOR = 2,
  CAPACITOR = 3,
  VOLTAGE_SOURCE = 4,
  CURRENT_SOURCE = 5,
  DIODE = 6,
  IDEAL_SWITCH = 7,
  THYRISTOR = 8,
  MUTUAL_INDUCTANCE = 9,
  IGBT = 10,
  COUPLED_INDUCTOR = 12,
  TRANSFORMER = 23,
  RELUCTANCE = 24,
  MMF = 26,
  MOSFET = 28,
  GLOBAL_TERMINAL = 31,
  BJT = 33,
  THERMAL_FLOW = 44,
  THERMAL_TEMP = 45,
  THERMAL_RTH = 46,
  THERMAL_CTH = 47,
}

/**
 * Control (signal domain) component type IDs.
 * Contains both legacy .ipes numbers (< 100) and modern catalog numbers (>= 1000).
 */
export enum ControlComponentType {
  /** Voltmeter probe (legacy .ipes numeric code). */
  LEGACY_VOLTMETER = 1,
  /** Ammeter probe (legacy .ipes numeric code). */
  LEGACY_AMMETER = 2,
  /** Constant source (legacy .ipes numeric code). */
  LEGACY_CONSTANT = 3,
  /** Signal source (legacy .ipes numeric code). */
  LEGACY_SIGNAL_SOURCE = 4,
  /** Oscilloscope probe (legacy .ipes numeric code). */
  LEGACY_SCOPE = 5,
  /** Gate driver input (legacy .ipes numeric code). */
  LEGACY_GATE = 6,
  /** Classic Java block (legacy .ipes numeric code). */
  LEGACY_JAVA_FUNCTION = 61,

  /** Gate driver (modern catalog CircuitTypCore.CTRL_GATE). */
  GATE = 1000,
  /** Voltmeter probe (modern catalog CircuitTypCore.CTRL_VOLT). */
  VOLTMETER = 1001,
  /** Ammeter probe (modern catalog CircuitTypCore.CTRL_AMP). */
  AMMETER = 1002,
  /** Oscilloscope probe (modern catalog CircuitTypCore.CTRL_SCOPE). */
  SCOPE = 1003,
  /** Signal source (modern catalog CircuitTypCore.CTRL_SIGNAL). */
  SIGNAL_SOURCE = 1004,
  /** Constant source (modern catalog CircuitTypCore.CTRL_CONSTANT). */
  CONSTANT = 1005,
  /** Script / Function block (modern catalog CircuitTypCore.CTRL_SCRIPT). */
  SCRIPT = 1016,
  /** Subtraction / Error calculation block (CTRL_SUB). */
  SUB = 1017,
  /** Addition block (CTRL_ADD). */
  ADD = 1018,
  /** Multiplication block (CTRL_MUL). */
  MUL = 1019,
  /** Division block (CTRL_DIV). */
  DIV = 1020,
  /** Limiter block (CTRL_LIMIT). */
  LIMIT = 1021,
  /** Absolute value block (CTRL_ABS). */
  ABS = 1022,
  /** Square root block (CTRL_SQRT). */
  SQRT = 1023,
  /** Exponential block (CTRL_EXP). */
  EXP = 1024,
  /** Natural logarithm block (CTRL_LN). */
  LN = 1025,
  /** Sine function block (CTRL_SIN). */
  SIN = 1026,
  /** Cosine function block (CTRL_COS). */
  COS = 1027,
  /** Minimum function block (CTRL_MIN). */
  MIN = 1028,
  /** Maximum function block (CTRL_MAX). */
  MAX = 1029,
  /** Hysteresis block (CTRL_HYS). */
  HYS = 1030,
  /** PT2 second-order filter block (CTRL_PT2). */
  PT2 = 1031,
  /** PD controller block (CTRL_PD). */
  PD = 1032,
  /** Sample & Hold block (CTRL_SAMPLEHOLD). */
  SAMPLEHOLD = 1033,
  /** Simulation time source block (CTRL_TIME). */
  TIME = 1034,
  /** XOR logic gate block (CTRL_XOR). */
  XOR = 1035,
  /** Greater or equal comparator block (CTRL_GE). */
  GE = 1036,
  /** PWM dead-time generator block (CTRL_DEADTIME). */
  DEADTIME = 1037,
  /** Arc sine function block (CTRL_ASIN). */
  ASIN = 1038,
  /** Arc cosine function block (CTRL_ACOS). */
  ACOS = 1039,
  /** Tangent function block (CTRL_TAN). */
  TAN = 1040,
  /** Arc tangent function block (CTRL_ATAN). */
  ATAN = 1041,
  /** Square function block (CTRL_SQR). */
  SQR = 1042,
  /** Power function block (CTRL_POW). */
  POW = 1043,
  /** Round function block (CTRL_ROUND). */
  ROUND = 1044,
  /** Signum function block (CTRL_SIGN). */
  SIGN = 1045,
  /** Equality comparator block (CTRL_EQ). */
  EQ = 1046,
  /** Inequality comparator block (CTRL_NE). */
  NE = 1047,
  /** Counter block (CTRL_COUNTER). */
  COUNTER = 1048,
  /** abc to dq transformation block (CTRL_ABCDQ). */
  ABCDQ = 1049,
  /** dq to abc transformation block (CTRL_DQABC). */
  DQABC = 1050,
  /** Thyristor control block (CTRL_THYR_CTRL). */
  THYR_CTRL = 1051,
  /** PMSM controller block (CTRL_PMSM_CONTROL). */
  PMSM_CONTROL = 1052,
  /** PMSM modulator block (CTRL_PMSM_MODULATOR). */
  PMSM_MODULATOR = 1053,
  /** Demultiplexer / bus splitter block (CTRL_DEMUX). */
  DEMUX = 1054,
  /** Space vector modulator block (CTRL_SPACE_VECTOR). */
  SPACE_VECTOR = 1055,
  /** Sliding DFT block (CTRL_SDFT). */
  SDFT = 1056,
  /** Sparse state-space matrix block (CTRL_SPARSEMATRIX). */
  SPARSEMATRIX = 1058,

  /** Legacy ASIN block code. */
  LEGACY_ASIN = 33,
  /** Legacy ACOS block code. */
  LEGACY_ACOS = 35,
  /** Legacy TAN block code. */
  LEGACY_TAN = 37,
  /** Legacy ATAN block code. */
  LEGACY_ATAN = 38,
  /** Legacy SQR block code. */
  LEGACY_SQR = 39,
  /** Legacy POW block code. */
  LEGACY_POW = 42,
  /** Legacy ROUND block code. */
  LEGACY_ROUND = 44,
  /** Legacy SIGN block code. */
  LEGACY_SIGN = 47,
  /** Legacy EQ block code. */
  LEGACY_EQ = 48,
  /** Legacy NE block code. */
  LEGACY_NE = 51,
  /** Legacy COUNTER block code. */
  LEGACY_COUNTER = 53,
  /** Legacy ABCDQ block code. */
  LEGACY_ABCDQ = 59,
  /** Legacy DQABC block code. */
  LEGACY_DQABC = 63,
  /** Legacy THYR_CTRL block code. */
  LEGACY_THYR_CTRL = 65,
  /** Legacy PMSM_CONTROL block code. */
  LEGACY_PMSM_CONTROL = 66,
  /** Legacy PMSM_MODULATOR block code. */
  LEGACY_PMSM_MODULATOR = 72,
  /** Legacy DEMUX block code. */
  LEGACY_DEMUX = 76,
  /** Legacy SPACE_VECTOR block code. */
  LEGACY_SPACE_VECTOR = 77,
  /** Legacy SDFT block code. */
  LEGACY_SDFT = 82,
  /** Legacy SPARSEMATRIX block code. */
  LEGACY_SPARSEMATRIX = 85,
}

/**
 * Backward compatibility alias for CTRL_TYPE, preserving exact object shape
 * while mapping to the strongly typed ControlComponentType enum.
 */
export const CTRL_TYPE = {
  LEGACY_VOLTMETER: ControlComponentType.LEGACY_VOLTMETER,
  LEGACY_AMMETER: ControlComponentType.LEGACY_AMMETER,
  LEGACY_CONSTANT: ControlComponentType.LEGACY_CONSTANT,
  LEGACY_SIGNAL_SOURCE: ControlComponentType.LEGACY_SIGNAL_SOURCE,
  LEGACY_SCOPE: ControlComponentType.LEGACY_SCOPE,
  LEGACY_GATE: ControlComponentType.LEGACY_GATE,
  GATE: ControlComponentType.GATE,
  VOLTMETER: ControlComponentType.VOLTMETER,
  AMMETER: ControlComponentType.AMMETER,
  SCOPE: ControlComponentType.SCOPE,
  SIGNAL_SOURCE: ControlComponentType.SIGNAL_SOURCE,
  CONSTANT: ControlComponentType.CONSTANT,
  LEGACY_JAVA_FUNCTION: ControlComponentType.LEGACY_JAVA_FUNCTION,
  SCRIPT: ControlComponentType.SCRIPT,
  SUB: ControlComponentType.SUB,
  ADD: ControlComponentType.ADD,
  MUL: ControlComponentType.MUL,
  DIV: ControlComponentType.DIV,
  LIMIT: ControlComponentType.LIMIT,
  ABS: ControlComponentType.ABS,
  SQRT: ControlComponentType.SQRT,
  EXP: ControlComponentType.EXP,
  LN: ControlComponentType.LN,
  SIN: ControlComponentType.SIN,
  COS: ControlComponentType.COS,
  MIN: ControlComponentType.MIN,
  MAX: ControlComponentType.MAX,
  HYS: ControlComponentType.HYS,
  PT2: ControlComponentType.PT2,
  PD: ControlComponentType.PD,
  SAMPLEHOLD: ControlComponentType.SAMPLEHOLD,
  TIME: ControlComponentType.TIME,
  XOR: ControlComponentType.XOR,
  GE: ControlComponentType.GE,
  DEADTIME: ControlComponentType.DEADTIME,
  ASIN: ControlComponentType.ASIN,
  ACOS: ControlComponentType.ACOS,
  TAN: ControlComponentType.TAN,
  ATAN: ControlComponentType.ATAN,
  SQR: ControlComponentType.SQR,
  POW: ControlComponentType.POW,
  ROUND: ControlComponentType.ROUND,
  SIGN: ControlComponentType.SIGN,
  EQ: ControlComponentType.EQ,
  NE: ControlComponentType.NE,
  COUNTER: ControlComponentType.COUNTER,
  ABCDQ: ControlComponentType.ABCDQ,
  DQABC: ControlComponentType.DQABC,
  THYR_CTRL: ControlComponentType.THYR_CTRL,
  PMSM_CONTROL: ControlComponentType.PMSM_CONTROL,
  PMSM_MODULATOR: ControlComponentType.PMSM_MODULATOR,
  DEMUX: ControlComponentType.DEMUX,
  SPACE_VECTOR: ControlComponentType.SPACE_VECTOR,
  SDFT: ControlComponentType.SDFT,
  SPARSEMATRIX: ControlComponentType.SPARSEMATRIX,
  LEGACY_ASIN: ControlComponentType.LEGACY_ASIN,
  LEGACY_ACOS: ControlComponentType.LEGACY_ACOS,
  LEGACY_TAN: ControlComponentType.LEGACY_TAN,
  LEGACY_ATAN: ControlComponentType.LEGACY_ATAN,
  LEGACY_SQR: ControlComponentType.LEGACY_SQR,
  LEGACY_POW: ControlComponentType.LEGACY_POW,
  LEGACY_ROUND: ControlComponentType.LEGACY_ROUND,
  LEGACY_SIGN: ControlComponentType.LEGACY_SIGN,
  LEGACY_EQ: ControlComponentType.LEGACY_EQ,
  LEGACY_NE: ControlComponentType.LEGACY_NE,
  LEGACY_COUNTER: ControlComponentType.LEGACY_COUNTER,
  LEGACY_ABCDQ: ControlComponentType.LEGACY_ABCDQ,
  LEGACY_DQABC: ControlComponentType.LEGACY_DQABC,
  LEGACY_THYR_CTRL: ControlComponentType.LEGACY_THYR_CTRL,
  LEGACY_PMSM_CONTROL: ControlComponentType.LEGACY_PMSM_CONTROL,
  LEGACY_PMSM_MODULATOR: ControlComponentType.LEGACY_PMSM_MODULATOR,
  LEGACY_DEMUX: ControlComponentType.LEGACY_DEMUX,
  LEGACY_SPACE_VECTOR: ControlComponentType.LEGACY_SPACE_VECTOR,
  LEGACY_SDFT: ControlComponentType.LEGACY_SDFT,
  LEGACY_SPARSEMATRIX: ControlComponentType.LEGACY_SPARSEMATRIX,
} as const;

/**
 * Grid raster, geometry, hit-testing, and rendering dimensions.
 */
export const CANVAS_METRICS = {
  /** Distance from component origin to terminals in standard two-port elements (grid units). */
  TWO_PORT_DIST: 2,
  /** Distance between multi-pin terminals (such as function block pins, scope channels) (grid units). */
  MULTI_PIN_STEP: 2,
  /** Max Euclidean distance (in grid units) to snap wire cursor or candidate placement to a terminal. */
  DEFAULT_SNAP_DISTANCE: 0.75,
  /** Distance threshold (in grid units) to consider a wire vertex coincident with a terminal or point. */
  TERMINAL_TOUCH_TOLERANCE: 0.25,
  /** Standard lead line length in symbol rendering (grid units). */
  LEAD_LENGTH: 2.0,
  /** Default scale factor for standalone preview symbols in palette/dialogs (pixels). */
  PREVIEW_SYMBOL_U: 10,
  /** Minimum zoom factor supported on the schematic sheet. */
  MIN_ZOOM: 0.2,
  /** Maximum zoom factor supported on the schematic sheet. */
  MAX_ZOOM: 5.0,
  /** Multiplier applied per scroll step or zoom button click. */
  ZOOM_STEP_FACTOR: 1.15,
  /** Default canvas raster grid step in pixels per grid unit. */
  DEFAULT_DPIX: 10,
  /** Default worksheet width in grid units. */
  DEFAULT_SHEET_WIDTH: 100,
  /** Default worksheet height in grid units. */
  DEFAULT_SHEET_HEIGHT: 70,
} as const;

/**
 * Sentinel value used in GeckoCIRCUITS .ipes files to denote an unassigned,
 * floating, or disconnected signal / label.
 */
export const SENTINEL_UNSET = 'NIX_NIX_NIX';

/**
 * Component parameter keys used to persist per-scope display settings into
 * the circuit file (.ipes <scopeSettings> sub-block). Must stay in sync with
 * the backend's ScopeSettingsKeys — the parser, writer and REST whitelist
 * all derive from that single definition.
 */
export const SCOPE_SETTING_KEYS = {
  /** Display layout mode: 'overlay' or 'stacked'. */
  scopeLayout: 'scopeLayout',
  /** Vertical scale mode: 'auto' or 'fixed'. */
  yScaleMode: 'yScaleMode',
  /** Comma-separated list of hidden channel names. */
  hiddenSignals: 'hiddenSignals',
  /** Whether measurement cursors are enabled. */
  cursorsEnabled: 'cursorsEnabled',
} as const;

/** Maximum number of terminals a script/function block can expose per side. */
export const SCRIPT_TERMINAL_LIMIT = 16;

/**
 * Simulation runner default settings.
 */
export const SIMULATION_DEFAULTS = {
  /** Default transient simulation stop time in seconds. */
  DURATION_SEC: 0.05,
  /** Default transient calculation time step in seconds. */
  TIME_STEP_SEC: 1e-6,
  /** Default numeric integration solver. */
  DEFAULT_SOLVER: 'TRAPEZOIDAL',
  /** Polling interval (in milliseconds) when monitoring asynchronous simulation runs. */
  POLL_INTERVAL_MS: 250,
  /** Max history depth retained for undo / redo operations. */
  MAX_UNDO_HISTORY: 50,
} as const;
