/**
 * Thin REST client for the gecko-rest-api. Hand-rolled fetch wrappers,
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
  const body = JSON.stringify({ content, filename: file.name });
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

async function fileToBase64(file: File): Promise<string> {
  const bytes = new Uint8Array(await file.arrayBuffer());
  let binary = '';
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

/**
 * Minimal STOMP-over-WebSocket subscription to /topic/circuits/{id}.
 * Uses the raw WS endpoint; auto-reconnects with a small backoff.
 * Returns a disposer. If the initial connection fails, retries silently —
 * the editor works without live updates (status is reported via onStatus).
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
    socket = new WebSocket(`${proto}://${location.host}/gecko/ws-raw/websocket`);

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
