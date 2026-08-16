import { afterEach, describe, expect, it, vi } from 'vitest';
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
