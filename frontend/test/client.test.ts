// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as client from '../src/api/client';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);

afterEach(() => {
  fetchMock.mockReset();
});

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'content-type': 'application/json' }),
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  } as unknown as Response;
}

describe('api client', () => {
  it('getCatalog hits the right URL', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ types: [{ type: 1, name: 'LK_R', family: 'LK' }] }));
    const result = await client.getCatalog();
    expect(fetchMock.mock.calls[0][0]).toBe('/gecko/api/v1/circuits/catalog');
    expect(result.types[0].name).toBe('LK_R');
  });

  it('getEditorModel fetches the snapshot', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ circuitId: 'c1', modelVersion: 3, components: [], connections: [] }),
    );
    const snapshot = await client.getEditorModel('c1');
    expect(fetchMock.mock.calls[0][0]).toBe('/gecko/api/v1/circuits/c1/model');
    expect(snapshot.modelVersion).toBe(3);
  });

  it('createComponent posts the body and returns the change message', async () => {
    const change = { circuitId: 'c1', modelVersion: 1, operation: 'createComponent', payload: {} };
    fetchMock.mockResolvedValue(jsonResponse(change, 201));
    const result = await client.createComponent('c1', {
      family: 'LK',
      type: 1,
      x: 10,
      y: 20,
      orientation: 503,
    });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/gecko/api/v1/circuits/c1/components');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ family: 'LK', type: 1, x: 10, y: 20, orientation: 503 });
    expect(result.operation).toBe('createComponent');
  });

  it('patchComponent URL-encodes the component name', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ circuitId: 'c1', modelVersion: 2, operation: 'patchComponent' }));
    await client.patchComponent('c1', 'R load', { x: 5 });
    const [url] = fetchMock.mock.calls[0];
    expect(url).toBe('/gecko/api/v1/circuits/c1/components/R%20load');
  });

  it('deleteConnection uses the index path', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ circuitId: 'c1', modelVersion: 4, operation: 'deleteConnection' }));
    await client.deleteConnection('c1', 7);
    expect(fetchMock.mock.calls[0][0]).toBe('/gecko/api/v1/circuits/c1/connections/7');
    expect(fetchMock.mock.calls[0][1].method).toBe('DELETE');
  });

  it('patchConnection sends label changes for wire net labels', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ circuitId: 'c1', modelVersion: 9, operation: 'patchConnection', payload: { index: 3, label: 'GND', points: [] } }),
    );
    await client.patchConnection('c1', 3, { label: 'GND' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/gecko/api/v1/circuits/c1/connections/3');
    expect(init.method).toBe('PATCH');
    expect(JSON.parse(init.body)).toEqual({ label: 'GND' });
  });

  it('setNodeLabel puts terminalIndex, side and label', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ circuitId: 'c1', modelVersion: 5, operation: 'setNodeLabel' }));
    await client.setNodeLabel('c1', 'R1', 0, 'y', 'gnd');
    const [, init] = fetchMock.mock.calls[0];
    expect(init.method).toBe('PUT');
    expect(JSON.parse(init.body)).toEqual({ terminalIndex: 0, side: 'y', label: 'gnd' });
  });

  it('undo posts without a body', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ circuitId: 'c1', modelVersion: 6, operation: 'undo' }));
    await client.undo('c1');
    expect(fetchMock.mock.calls[0][0]).toBe('/gecko/api/v1/circuits/c1/undo');
    expect(fetchMock.mock.calls[0][1].method).toBe('POST');
  });

  it('surfaces the server detail message on errors', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ detail: 'Circuit not found: nope' }, 404));
    await expect(client.getEditorModel('nope')).rejects.toThrow('Circuit not found: nope');
  });

  it('falls back to the HTTP status when the body is not JSON', async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 502,
      headers: new Headers(),
      json: () => Promise.reject(new Error('no json')),
    } as unknown as Response);
    await expect(client.undo('c1')).rejects.toThrow('HTTP 502');
  });

  it('uploadIpes resolves the circuitId from the load response', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ circuitId: 'abc', status: 'loaded', filename: 'x.ipes', componentCount: 3 }, 201),
    );
    const id = await client.uploadIpes(new File(['fake'], 'x.ipes'));
    expect(id).toBe('abc');
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/gecko/api/v1/circuits/parse');
    expect(JSON.parse(init.body).filename).toBe('x.ipes');
    expect(JSON.parse(init.body).content.length).toBeGreaterThan(0);
  });

  it('uploadIpes rejects when the server reports a parse failure', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ status: 'failed', filename: 'x.ipes', errorMessage: 'bad gzip' }, 400),
    );
    await expect(client.uploadIpes(new File(['x'], 'x.ipes'))).rejects.toThrow();
  });
});

describe('downloadIpes', () => {
  afterEach(() => {
    delete (globalThis as { __TAURI__?: unknown }).__TAURI__;
  });

  function blobResponse(): Response {
    return {
      ok: true,
      status: 200,
      blob: () => Promise.resolve(new Blob([new Uint8Array([1, 2, 3])])),
    } as unknown as Response;
  }

  it('uses the browser anchor download without __TAURI__ (jsdom)', async () => {
    URL.createObjectURL = vi.fn().mockReturnValue('blob:x');
    URL.revokeObjectURL = vi.fn();
    const clicks: string[] = [];
    const originalCreate = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
      const el = originalCreate(tag);
      if (tag === 'a') {
        const anchor = el as HTMLAnchorElement;
        anchor.click = () => clicks.push(anchor.download);
      }
      return el;
    });
    fetchMock.mockResolvedValue(blobResponse());
    await client.downloadIpes('c1', 'circ');
    expect(fetchMock.mock.calls[0][0]).toBe('/gecko/api/v1/circuits/c1/ipes');
    expect(clicks).toEqual(['circ.ipes']);
    vi.restoreAllMocks();
  });

  it('routes through the native save dialog on the desktop', async () => {
    const invoke = vi.fn().mockResolvedValue('C:/x/chosen.ipes');
    (globalThis as { __TAURI__?: unknown }).__TAURI__ = {
      core: { invoke: invoke as never },
    };
    fetchMock.mockResolvedValue(blobResponse());
    await client.downloadIpes('c1', 'circ.ipes');
    expect(invoke).toHaveBeenCalledWith('save_file_dialog', {
      base64: expect.any(String),
      suggestedName: 'circ.ipes',
    });
    expect(invoke.mock.calls[0][1].base64.length).toBeGreaterThan(0);
  });
});

describe('api client with injected backend origin', () => {
  beforeEach(() => {
    // API base is computed at module load; force re-evaluation per test
    vi.resetModules();
  });

  afterEach(() => {
    delete (globalThis as { __GECKO_BACKEND__?: string }).__GECKO_BACKEND__;
    vi.resetModules();
  });

  it('prefixes API calls with the injected origin', async () => {
    (globalThis as { __GECKO_BACKEND__?: string }).__GECKO_BACKEND__ = 'http://127.0.0.1:54321';
    const injected = await import('../src/api/client');
    fetchMock.mockResolvedValue(jsonResponse({ types: [] }));
    await injected.getCatalog();
    expect(fetchMock.mock.calls[0][0]).toBe('http://127.0.0.1:54321/gecko/api/v1/circuits/catalog');
  });

  it('keeps relative URLs when no origin is injected', async () => {
    const injected = await import('../src/api/client');
    fetchMock.mockResolvedValue(jsonResponse({ types: [] }));
    await injected.getCatalog();
    expect(fetchMock.mock.calls[0][0]).toBe('/gecko/api/v1/circuits/catalog');
  });

  it('derives the WS origin from the injected backend origin', async () => {
    (globalThis as { __GECKO_BACKEND__?: string }).__GECKO_BACKEND__ = 'https://127.0.0.1:54321';
    const injected = await import('../src/api/client');
    const urls: string[] = [];
    class FakeWebSocket {
      constructor(url: string) {
        urls.push(url);
      }
      onopen: (() => void) | null = null;
      onmessage: ((event: unknown) => void) | null = null;
      onclose: (() => void) | null = null;
      onerror: (() => void) | null = null;
      send() {}
      close() {}
    }
    vi.stubGlobal('WebSocket', FakeWebSocket);
    const dispose = injected.subscribeCircuitChanges('c1', () => {});
    dispose();
    expect(urls[0]).toBe('wss://127.0.0.1:54321/gecko/ws-raw');
  });
});
