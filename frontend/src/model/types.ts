/**
 * Types mirroring the REST JSON data models of the gecko-rest-api
 * editing, persistence, and simulation endpoints.
 */

import type { Orientation, LkComponentType, ControlComponentType } from './constants';

/**
 * 2D cartesian coordinate point in integer grid raster units (or canvas pixels when explicitly noted).
 */
export interface Point {
  /** X coordinate (grid units or pixels). */
  x: number;
  /** Y coordinate (grid units or pixels). */
  y: number;
}

/**
 * Coordinate pair tuple [x, y] in grid raster units.
 */
export type PointTuple = [x: number, y: number];

/**
 * Catalog entry describing an available component archetype in GeckoCIRCUITS.
 */
export interface CatalogEntry {
  /** Numeric component type ID. */
  type: number | LkComponentType | ControlComponentType;
  /** Canonical component class name (e.g., "Resistor", "Voltmeter"). */
  name: string;
  /** Domain family: 'LK' (electric), 'CONTROL' (signal/control), or 'THERMAL'. */
  family: string;
}

/**
 * In-memory representation of a schematic circuit component on the canvas.
 */
export interface EditorComponent {
  /** Numeric component type ID (from LK or CONTROL catalogs). */
  type: number | LkComponentType | ControlComponentType;
  /** Unique instance name in the circuit (e.g., "R.1", "D.2", "SCOPE.1"). */
  name: string;
  /** Domain family ('LK', 'CONTROL', 'THERMAL'). */
  family: string;
  /** Center position [x, y] in integer grid raster units. */
  position: number[] | PointTuple;
  /** Spatial rotation code (501: SOUTH_NORTH, 502: WEST_EAST, 503: NORTH_SOUTH, 504: EAST_WEST). */
  orientation: number | Orientation;
  /** Component parameter dictionary keyed by parameter name. */
  parameters: Record<string, number | string | boolean>;
  /** Net labels attached to input terminals. */
  inputLabels: string[];
  /** Net labels attached to output terminals. */
  outputLabels: string[];
  /** Optional legacy inputs array. */
  inputs?: unknown[];
}

/**
 * In-memory representation of a schematic electrical or control connection wire.
 */
export interface EditorWire {
  /** Sequential wire index in the schematic wire list. */
  index: number;
  /** Domain type (typically 'LK' or 'CONTROL'). */
  type: string;
  /** User-assigned net label, or 'NIX_NIX_NIX' if unassigned. */
  label: string;
  /** Array of dense integer grid raster points [x, y] traversed by this wire. */
  points: number[][];
}

/**
 * Circuit simulation default solver and time parameters.
 */
export interface SimulationDefaults {
  /** Numerical integration step size in seconds (e.g., 1e-6 for 1µs). */
  timeStep: number;
  /** Simulation end / duration time in seconds (e.g., 0.05 for 50ms). */
  duration: number;
  /** Solver integration method (e.g., 'TRAPEZOIDAL', 'EULER', 'GEAR'). */
  solverType: string;
  /** Signal names selected for recording during simulation. */
  signals: string[];
}

/**
 * Complete snapshot of a circuit's editor model serialized by the REST backend.
 */
export interface EditorSnapshot {
  /** Unique session circuit ID allocated by the backend. */
  circuitId: string;
  /** Monotonically increasing revision version of the model. */
  modelVersion: number;
  /** Source file name (e.g., "buck_converter.ipes"). */
  filename: string;
  /** Pixels per grid unit (DPIX scale, default 10). */
  dpix: number;
  /** Worksheet standard size string (e.g., "A4_LANDSCAPE"). */
  worksheetSize?: string;
  /** Canvas width in grid units. */
  sheetWidth?: number;
  /** Canvas height in grid units. */
  sheetHeight?: number;
  /** Array of all components in the circuit. */
  components: EditorComponent[];
  /** Optional legacy connection alias for wires. */
  connections?: EditorWire[];
  /** Array of all connection wires in the circuit. */
  wires?: EditorWire[];
  /** Default simulation configuration parameters for this circuit. */
  simulationDefaults?: SimulationDefaults;
}

/**
 * Server-sent change or mutation message dispatched over REST or WebSocket.
 */
export interface ChangeMessage {
  /** Circuit session ID. */
  circuitId: string;
  /** Revision version resulting from this mutation. */
  modelVersion: number;
  /** Operation descriptor ('createComponent', 'patchComponent', 'deleteComponent', etc.). */
  operation: string;
  /** Payload associated with this operation. */
  payload?: unknown;
}

/** Payload of createComponent/patchComponent change messages. */
export interface ComponentPayload {
  type: number | LkComponentType | ControlComponentType;
  name: string;
  domain: string;
  position: number[];
  orientation: number | Orientation;
  parameters: Record<string, number | string | boolean>;
}

/** Payload of createConnection/patchConnection change messages. */
export interface WirePayload {
  index: number;
  type: string;
  label: string;
  points: number[][];
}

/** Parameters required to spawn a new component on the schematic. */
export interface ComponentCreate {
  family: string;
  type: number | LkComponentType | ControlComponentType;
  name?: string;
  x: number;
  y: number;
  orientation?: number | Orientation;
  parameters?: Record<string, number | string | boolean>;
}

/** Delta patch payload applied to an existing component. */
export interface ComponentPatch {
  x?: number;
  y?: number;
  orientation?: number | Orientation;
  newName?: string;
  parameters?: Record<string, number | string | boolean>;
}

/** Parameters required to instantiate a new connection wire. */
export interface ConnectionCreate {
  type: string;
  points: number[][];
  label?: string;
}

/** Delta patch payload applied to an existing connection wire. */
export interface ConnectionPatch {
  points?: number[][];
  label?: string;
}

/** Simulation execution request payload. */
export interface SimulationRequest {
  circuitId?: string;
  base64Circuit?: string;
  circuitFile?: string;
  simulationTime?: number;
  timeStep?: number;
  solverType?: string;
  /** "legacy" runs the original file in the classic engine via RMI. */
  backend?: string;
  parameters?: Record<string, number>;
  signals?: string[];
}

/** Execution status state machine states for simulation runs. */
export type SimulationStatus = 'PENDING' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

/** Simulation progress and result payload returned by backend endpoints. */
export interface SimulationResponse {
  simulationId: string;
  status: SimulationStatus;
  progress?: number;
  errorMessage?: string;
  results?: Record<string, number[]>;
  executionTimeMs?: number;
  /** Non-fatal engine warnings (component types skipped during simulation). */
  warnings?: string[];
  progressDetails?: {
    currentStep: number;
    totalSteps: number;
    currentTime: number;
    endTime: number;
  };
}
