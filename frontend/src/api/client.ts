/**
 * REST and WebSocket client for the GeckoCIRCUITS REST API (gecko-rest-api).
 *
 * Implements typed fetch wrappers, error wrapping, circuit persistence,
 * server-sent events (SSE) for simulation progress, and STOMP-over-WebSocket
 * live schematic synchronization.
 */

import { isDesktop, saveFileNative } from '../desktop';
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

/** Size in bytes of chunks processed during binary-to-base64 conversion. */
const BASE64_CHUNK_SIZE = 0x8000;

/** Initial reconnection backoff delay for WebSockets in milliseconds. */
const INITIAL_WS_RECONNECT_DELAY_MS = 1000;

/** Maximum reconnection backoff delay for WebSockets in milliseconds. */
const MAX_WS_RECONNECT_DELAY_MS = 15000;

/**
 * Custom error thrown when a REST API endpoint responds with a non-2xx status code.
 */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly endpoint: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * Backend origin injected by the desktop shell via initialization script
 * (window.__GECKO_BACKEND__, e.g. "http://127.0.0.1:54321"). Empty string
 * denotes same-origin, which is what standard web browser deployments use.
 */
export function backendOrigin(): string {
  return (globalThis as { __GECKO_BACKEND__?: string }).__GECKO_BACKEND__ ?? '';
}

/**
 * Base URL for the GeckoCIRCUITS REST API v1 endpoints.
 * Evaluated dynamically to accommodate late injection from the desktop shell.
 */
export function apiBase(): string {
  return backendOrigin() + '/gecko/api/v1';
}

/**
 * Performs an HTTP request against the Gecko REST API, parsing JSON or text responses.
 *
 * @template T Expected response body type
 * @param path Endpoint path relative to `/gecko/api/v1`
 * @param init Standard RequestInit options (method, headers, body, signal)
 * @returns Parsed response body
 * @throws {ApiError} When response status is outside the 2xx range
 */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const url = apiBase() + path;
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!response.ok) {
    throw new ApiError(await errorMessage(response), response.status, path);
  }
  const contentType = response.headers.get('content-type') ?? '';
  return (contentType.includes('json') ? response.json() : response.text()) as Promise<T>;
}

/**
 * Extracts a descriptive error message string from a failed HTTP response.
 */
async function errorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json();
    return body.detail ?? body.message ?? body.error ?? `HTTP ${response.status}`;
  } catch {
    return `HTTP ${response.status}`;
  }
}

/**
 * Retrieves the catalog of available component types supported by the server engine.
 */
export function getCatalog(): Promise<{ types: CatalogEntry[] }> {
  return request('/circuits/catalog');
}

/**
 * Fetches the complete editor model snapshot for an active circuit session.
 *
 * @param circuitId Unique circuit session identifier
 */
export function getEditorModel(circuitId: string): Promise<EditorSnapshot> {
  return request(`/circuits/${circuitId}/model`);
}

/**
 * Uploads a local .ipes file (gzip compressed or plain ASCII) and returns a new session circuit ID.
 *
 * @param file File object from browser file input or drag-and-drop
 */
export async function uploadIpes(file: File): Promise<string> {
  const content = await fileToBase64(file);
  return uploadIpesBase64(content, file.name);
}

/**
 * Uploads raw ASCII or Base64 string content and returns a new session circuit ID.
 *
 * @param content String content of .ipes file
 * @param filename File name descriptor (defaults to 'circuit.ipes')
 */
export async function uploadIpesString(content: string, filename = 'circuit.ipes'): Promise<string> {
  return uploadIpesBase64(toBase64(new TextEncoder().encode(content)), filename);
}

/**
 * Uploads a Base64-encoded .ipes file payload and initializes an editor model session.
 *
 * @param base64Content Base64 encoded file data
 * @param filename File name descriptor
 */
export async function uploadIpesBase64(base64Content: string, filename: string): Promise<string> {
  const body = JSON.stringify({ content: base64Content, filename });
  const response = await fetch(apiBase() + '/circuits/parse', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
  });
  if (!response.ok) {
    throw new ApiError(await errorMessage(response), response.status, '/circuits/parse');
  }
  const json = (await response.json()) as { circuitId?: string; errorMessage?: string };
  if (json.errorMessage) {
    throw new ApiError(json.errorMessage, response.status, '/circuits/parse');
  }
  return json.circuitId!;
}

/**
 * Instantiates a new component on the schematic canvas.
 *
 * @param circuitId Active circuit ID
 * @param component Component placement specifications
 */
export function createComponent(circuitId: string, component: ComponentCreate): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/components`, { method: 'POST', body: JSON.stringify(component) });
}

/**
 * Updates parameters, orientation, or coordinates of an existing component.
 *
 * @param circuitId Active circuit ID
 * @param name Component instance name (e.g. "R.1")
 * @param patch Patch parameters
 */
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

/**
 * Removes a component from the schematic canvas.
 *
 * @param circuitId Active circuit ID
 * @param name Component instance name
 */
export function deleteComponent(circuitId: string, name: string): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/components/${encodeURIComponent(name)}`, { method: 'DELETE' });
}

/**
 * Creates a new wire connection between coordinates.
 *
 * @param circuitId Active circuit ID
 * @param wire Connection definition
 */
export function createConnection(circuitId: string, wire: ConnectionCreate): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/connections`, { method: 'POST', body: JSON.stringify(wire) });
}

/**
 * Updates routing coordinates or net label of an existing wire connection.
 *
 * @param circuitId Active circuit ID
 * @param index Wire index in circuit connections list
 * @param patch Wire patch specifications
 */
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

/**
 * Deletes a wire connection by its index.
 *
 * @param circuitId Active circuit ID
 * @param index Wire index in circuit connections list
 */
export function deleteConnection(circuitId: string, index: number): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/connections/${index}`, { method: 'DELETE' });
}

/**
 * Assigns a net label directly to a component terminal node.
 *
 * @param circuitId Active circuit ID
 * @param componentName Target component name
 * @param terminalIndex Zero-based terminal index
 * @param side Terminal port side ('x' for input, 'y' for output)
 * @param label Assigned net label string
 */
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

/**
 * Reverts the most recent model mutation on the server.
 *
 * @param circuitId Active circuit ID
 */
export function undo(circuitId: string): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/undo`, { method: 'POST' });
}

/**
 * Re-applies the most recently reverted model mutation on the server.
 *
 * @param circuitId Active circuit ID
 */
export function redo(circuitId: string): Promise<ChangeMessage> {
  return request(`/circuits/${circuitId}/redo`, { method: 'POST' });
}

/**
 * Exports and downloads the circuit as a .ipes file.
 * Uses native desktop file dialogs when hosted inside Tauri, or browser Blob download otherwise.
 *
 * @param circuitId Active circuit ID
 * @param filename Suggested download file name
 */
export async function downloadIpes(circuitId: string, filename: string): Promise<void> {
  const response = await fetch(apiBase() + `/circuits/${circuitId}/ipes`);
  if (!response.ok) {
    throw new ApiError(await errorMessage(response), response.status, `/circuits/${circuitId}/ipes`);
  }
  const blob = await response.blob();
  const name = filename.endsWith('.ipes') ? filename : filename + '.ipes';
  if (isDesktop()) {
    await saveFileNative(toBase64(new Uint8Array(await blob.arrayBuffer())), name);
    return;
  }
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}

// ========== Analysis Endpoints ==========

export interface FourierRequest {
  data: number[];
  sampleRate: number;
  startTime?: number;
  endTime?: number;
}

export interface FourierResult {
  baseFrequency: number;
  harmonics: number;
  signalName: string;
  anCoefficients: number[];
  bnCoefficients: number[];
  cnAmplitudes: number[];
  jnPhases: number[];
  dcComponent: number;
  fundamentalAmplitude: number;
  fundamentalPhaseDegrees: number;
}

/**
 * Computes Fourier harmonic decomposition (An/Bn/Cn/Jn) of a signal window.
 *
 * @param req Signal data and sample rate configuration
 * @param harmonics Number of harmonics to decompose
 */
export function computeFourier(req: FourierRequest, harmonics: number): Promise<FourierResult> {
  return request(`/analysis/fourier?harmonics=${harmonics}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

// ========== Simulation Endpoints ==========

/**
 * Submits a circuit simulation task to the backend engine.
 *
 * @param req Simulation parameters, solver choice, and signals
 */
export function submitSimulation(req: SimulationRequest): Promise<SimulationResponse> {
  return request('/simulations', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

/**
 * Queries current status and execution metrics of an active or completed simulation.
 *
 * @param simulationId Simulation run ID
 */
export function getSimulation(simulationId: string): Promise<SimulationResponse> {
  return request(`/simulations/${simulationId}`);
}

/**
 * Retrieves calculated signal waveforms for a completed simulation.
 *
 * @param simulationId Simulation run ID
 * @returns Signal dictionary mapping signal name to array of numeric sample values
 */
export function getSimulationResults(simulationId: string): Promise<Record<string, number[]>> {
  return request(`/simulations/${simulationId}/results`);
}

/**
 * Cancels an in-progress simulation run on the server.
 *
 * @param simulationId Simulation run ID
 */
export function cancelSimulation(simulationId: string): Promise<void> {
  return request(`/simulations/${simulationId}`, { method: 'DELETE' });
}

/**
 * Temporarily pauses an active simulation run.
 *
 * @param simulationId Simulation run ID
 */
export function pauseSimulation(simulationId: string): Promise<SimulationResponse> {
  return request(`/simulations/${simulationId}/pause`, { method: 'POST' });
}

/**
 * Resumes execution of a paused simulation run.
 *
 * @param simulationId Simulation run ID
 */
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
 * Subscribes to the server's Server-Sent Events (SSE) progress stream for an active simulation.
 *
 * @param simulationId Simulation run ID
 * @param handlers Callback handlers for progress, completion, and error events
 * @returns Disposer function to cleanly terminate the SSE stream
 */
export function streamSimulationProgress(
  simulationId: string,
  handlers: SimulationStreamHandlers,
): () => void {
  const source = new EventSource(`${apiBase()}/simulations/${simulationId}/stream`);
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

/**
 * Converts a byte array to Base64 encoded string using chunked processing.
 */
function toBase64(bytes: Uint8Array): string {
  let binary = '';
  const chunk = BASE64_CHUNK_SIZE;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

/**
 * Reads a File object as a Base64-encoded string.
 */
async function fileToBase64(file: File): Promise<string> {
  return toBase64(new Uint8Array(await file.arrayBuffer()));
}

/**
 * Minimal STOMP-over-WebSocket subscription to `/topic/circuits/{id}`.
 * Auto-reconnects with exponential backoff on disconnect.
 *
 * @param circuitId Active circuit ID
 * @param onMessage Message callback invoked on receiving incoming ChangeMessage
 * @param onStatus Connection status change callback
 * @returns Disposer function to cleanly disconnect and cancel reconnect attempts
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
    socket = new WebSocket(wsOrigin() + '/gecko/ws-raw');

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
        const delay = Math.min(
          INITIAL_WS_RECONNECT_DELAY_MS * 2 ** Math.min(attempt, 4),
          MAX_WS_RECONNECT_DELAY_MS,
        );
        setTimeout(connect, delay);
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

/**
 * Computes WebSocket connection URL, honoring injected desktop origin or browser location.
 */
function wsOrigin(): string {
  const base = backendOrigin();
  if (base) {
    return base.replace(/^http/, 'ws');
  }
  const proto = location.protocol === 'https:' ? 'wss' : 'ws';
  return `${proto}://${location.host}`;
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
