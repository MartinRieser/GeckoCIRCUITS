/** Types mirroring the REST JSON of the gecko-rest-api editing and simulation endpoints. */

export interface Point {
  x: number;
  y: number;
}

export interface CatalogEntry {
  type: number;
  name: string;
  family: string;
}

export interface EditorComponent {
  type: number;
  name: string;
  family: string;
  position: number[];
  orientation: number;
  parameters: Record<string, number | string | boolean>;
  inputLabels: string[];
  outputLabels: string[];
}

export interface EditorWire {
  index: number;
  type: string;
  label: string;
  points: number[][];
}

export interface SimulationDefaults {
  timeStep: number;
  duration: number;
  solverType: string;
  signals: string[];
}

export interface EditorSnapshot {
  circuitId: string;
  modelVersion: number;
  filename: string;
  dpix: number;
  worksheetSize?: string;
  sheetWidth?: number;
  sheetHeight?: number;
  components: EditorComponent[];
  connections?: EditorWire[];
  wires?: EditorWire[];
  simulationDefaults?: SimulationDefaults;
}

export interface ChangeMessage {
  circuitId: string;
  modelVersion: number;
  operation: string;
  payload?: unknown;
}

/** Payload of createComponent/patchComponent change messages. */
export interface ComponentPayload {
  type: number;
  name: string;
  domain: string;
  position: number[];
  orientation: number;
  parameters: Record<string, number | string | boolean>;
}

/** Payload of createConnection/patchConnection change messages. */
export interface WirePayload {
  index: number;
  type: string;
  label: string;
  points: number[][];
}

export interface ComponentCreate {
  family: string;
  type: number;
  name?: string;
  x: number;
  y: number;
  orientation?: number;
  parameters?: Record<string, number | string | boolean>;
}

export interface ComponentPatch {
  x?: number;
  y?: number;
  orientation?: number;
  newName?: string;
  parameters?: Record<string, number | string | boolean>;
}

export interface ConnectionCreate {
  type: string;
  points: number[][];
  label?: string;
}

export interface ConnectionPatch {
  points?: number[][];
  label?: string;
}

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

export type SimulationStatus = 'PENDING' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface SimulationResponse {
  simulationId: string;
  status: SimulationStatus;
  progress?: number;
  errorMessage?: string;
  results?: Record<string, number[]>;
  executionTimeMs?: number;
  progressDetails?: {
    currentStep: number;
    totalSteps: number;
    currentTime: number;
    endTime: number;
  };
}
