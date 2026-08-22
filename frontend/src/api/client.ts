/**
 * REST client for the gecko-rest-api. Hand-rolled fetch wrappers,
 * no SDK generation. All functions throw Error with the server's detail
 * message on non-2xx responses.
 */
import type {
  CatalogEntry,
  ChangeMessage,
  ComponentCreate,
  ComponentPatch,
  ConnectionCreate,
  ConnectionPatch,
  EditorSnapshot,
  SimulationRequest,
  SimulationResponse,
} from '../model/types';

const API = '/gecko/api/v1';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  const contentType = response.headers.get('content-type') ?? '';
  return (contentType.includes('json') ? response.json() : response.text()) as Promise<T>;
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json();
    return body.detail ?? body.message ?? body.error ?? `HTTP ${response.status}`;
  } catch {
    return `HTTP ${response.status}`;
  }
}

export function getCatalog(): Promise<{ types: CatalogEntry[] }> {
  return request('/circuits/catalog');
}

export function getEditorModel(circuitId: string): Promise<EditorSnapshot> {
  return request(`/circuits/${circuitId}/model`);
}

/** Uploads a .ipes file (gzip or plain) and returns the new circuit ID. */
export async function uploadIpes(file: File): Promise<string> {
  const content = await fileToBase64(file);
  return uploadIpesBase64(content, file.name);
}

/** Uploads raw ASCII or Base64 string content and returns the new circuit ID. */
export async function uploadIpesString(content: string, filename = 'circuit.ipes'): Promise<string> {
  return uploadIpesBase64(toBase64(new TextEncoder().encode(content)), filename);
}

/** Uploads base64 encoded .ipes file. */
export async function uploadIpesBase64(base64Content: string, filename: string): Promise<string> {
  const body = JSON.stringify({ content: base64Content, filename });
  const response = await fetch(API + '/circuits/parse', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
  });
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  const json = (await response.json()) as { circuitId?: string; errorMessage?: string };
  if (json.errorMessage) {
    throw new Error(json.errorMessage);
  }
  return json.circuitId!;
}

export function createComponent(circuitId: string, component: ComponentCreate): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/components`, { method: 'POST', body: JSON.stringify(component) });
}

export function patchComponent(
  circuitId: string,
  name: string,
  patch: ComponentPatch,
): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/components/${encodeURIComponent(name)}`, {
    method: 'PATCH',
    body: JSON.stringify(patch),
  });
}

export function deleteComponent(circuitId: string, name: string): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/components/${encodeURIComponent(name)}`, { method: 'DELETE' });
}

export function createConnection(circuitId: string, wire: ConnectionCreate): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/connections`, { method: 'POST', body: JSON.stringify(wire) });
}

export function patchConnection(
  circuitId: string,
  index: number,
  patch: ConnectionPatch,
): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/connections/${index}`, {
    method: 'PATCH',
    body: JSON.stringify(patch),
  });
}

export function deleteConnection(circuitId: string, index: number): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/connections/${index}`, { method: 'DELETE' });
}

export function setNodeLabel(
  circuitId: string,
  componentName: string,
  terminalIndex: number,
  side: 'x' | 'y',
  label: string,
): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/nodes/${encodeURIComponent(componentName)}`, {
    method: 'PUT',
    body: JSON.stringify({ terminalIndex, side, label }),
  });
}

export function undo(circuitId: string): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/undo`, { method: 'POST' });
}

export function redo(circuitId: string): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/redo`, { method: 'POST' });
}

/** Downloads the circuit as .ipes file (browser download). */
export async function downloadIpes(circuitId: string, filename: string): Promise<void> {
  const response = await fetch(API + `/circuits/${circuitId}/ipes`);
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename.endsWith('.ipes') ? filename : filename + '.ipes';
  link.click();
  URL.revokeObjectURL(url);
}

// ========== Simulation Endpoints ==========

export function submitSimulation(req: SimulationRequest): Promise<SimulationResponse> {
  return request('/simulations', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export function getSimulation(simulationId: string): Promise<SimulationResponse> {
  return request(`/simulations/${simulationId}`);
}

export function getSimulationResults(simulationId: string): Promise<Record<string, number[]>> {
  return request(`/simulations/${simulationId}/results`);
}

export function cancelSimulation(simulationId: string): Promise<void> {
  return request(`/simulations/${simulationId}`, { method: 'DELETE' });
}

export function pauseSimulation(simulationId: string): Promise<SimulationResponse> {
  return request(`/simulations/${simulationId}/pause`, { method: 'POST' });
}

export function resumeSimulation(simulationId: string): Promise<SimulationResponse> {
  return request(`/simulations/${simulationId}/resume`, { method: 'POST' });
}

export interface SimulationStreamHandlers {
  onProgress?: (progress: number, currentTime: number, endTime: number) => void;
  onComplete?: () => void;
  onSimError?: (message: string) => void;
  /** Called when the SSE connection itself fails (simulation outcome unknown). */
  onConnectionError?: () => void;
}

/**
 * Subscribes to the server's SSE progress stream for a simulation.
 * Returns a disposer. Event names match the backend: progress / complete / error.
 */
export function streamSimulationProgress(
  simulationId: string,
  handlers: SimulationStreamHandlers,
): () => void {
  const source = new EventSource(`${API}/simulations/${simulationId}/stream`);
  let done = false;
  const finish = () => {
    if (!done) {
      done = true;
      source.close();
    }
  };

  source.addEventListener('progress', (event) => {
    try {
      const data = JSON.parse((event as MessageEvent).data);
      handlers.onProgress?.(
        data.progress ?? 0,
        data.currentTime ?? 0,
        data.endTime ?? 0,
      );
    } catch {
      // ignore malformed payloads
    }
  });

  source.addEventListener('complete', () => {
    finish();
    handlers.onComplete?.();
  });

  source.addEventListener('error', (event) => {
    // server-sent "error" events carry data; bare connection errors do not
    if (done) return;
    if (event instanceof MessageEvent && typeof event.data === 'string' && event.data) {
      finish();
      let message = event.data;
      try {
        message = JSON.parse(event.data).errorMessage || message;
      } catch {
        // keep raw payload
      }
      handlers.onSimError?.(message);
    } else {
      finish();
      handlers.onConnectionError?.();
    }
  });

  return finish;
}

function toBase64(bytes: Uint8Array): string {
  let binary = '';
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

async function fileToBase64(file: File): Promise<string> {
  return toBase64(new Uint8Array(await file.arrayBuffer()));
}

/**
 * Minimal STOMP-over-WebSocket subscription to /topic/circuits/{id}.
 * Uses the raw WS endpoint; auto-reconnects with a small backoff.
 * Returns a disposer.
 */
export function subscribeCircuitChanges(
  circuitId: string,
  onMessage: (msg: ChangeMessage) => void,
  onStatus?: (connected: boolean) => void,
): () => void {
  let socket: WebSocket | null = null;
  let disposed = false;
  let attempt = 0;

  const connect = () => {
    if (disposed) return;
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    socket = new WebSocket(`${proto}://${location.host}/gecko/ws-raw`);

    socket.onopen = () => {
      attempt = 0;
      socket!.send(encodeFrame('CONNECT', 'accept-version:1.2\nhost:gecko-editor'));
      socket!.send(
        encodeFrame('SUBSCRIBE', `id:sub-0\ndestination:/topic/circuits/${circuitId}`),
      );
      onStatus?.(true);
    };

    socket.onmessage = (event) => {
      for (const frame of decodeFrames(event.data as string)) {
        if (frame.command === 'MESSAGE' && frame.body) {
          try {
            onMessage(JSON.parse(frame.body));
          } catch {
            // ignore malformed frames
          }
        }
      }
    };

    socket.onclose = () => {
      onStatus?.(false);
      if (!disposed) {
        attempt += 1;
        setTimeout(connect, Math.min(1000 * 2 ** Math.min(attempt, 4), 15000));
      }
    };

    socket.onerror = () => socket?.close();
  };

  connect();
  return () => {
    disposed = true;
    socket?.close();
  };
}

function encodeFrame(command: string, headers: string, body = ''): string {
  return `${command}\n${headers}\n\n${body}\0`;
}

interface StompFrame {
  command: string;
  body?: string;
}

function decodeFrames(data: string): StompFrame[] {
  return data
    .split('\0')
    .filter((frame) => frame.trim().length > 0)
    .map((frame) => {
      const [head, ...rest] = frame.split('\n\n');
      const [command] = head.split('\n');
      return { command: command.trim(), body: rest.join('\n\n') };
    });
}
