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
