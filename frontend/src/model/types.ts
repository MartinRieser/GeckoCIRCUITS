/** Types mirroring the REST JSON of the gecko-rest-api editing endpoints. */

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

export interface EditorSnapshot {
  circuitId: string;
  modelVersion: number;
  filename: string;
  dpix: number;
  worksheetSize: string;
  components: EditorComponent[];
  connections: EditorWire[];
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
  parameters?: Record<string, number>;
}

export interface ComponentPatch {
  x?: number;
  y?: number;
  orientation?: number;
  newName?: string;
  parameters?: Record<string, number>;
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
