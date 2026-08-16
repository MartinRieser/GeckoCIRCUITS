// @vitest-environment jsdom
/**
 * App-level integration test with mocked fetch: the full user flow of
 * loading a circuit and placing a component. WebSocket is stubbed (the
 * live subscription is not under test here).
 */
import { describe, expect, it, vi, afterEach } from 'vitest';
import { cleanup, fireEvent, render, waitFor } from '@testing-library/react';
import { App } from '../src/App';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);
vi.stubGlobal(
  'WebSocket',
  class {
    close() {}
    send() {}
  },
);

const catalogBody = {
  types: [
    { type: 1, name: 'LK_R', family: 'LK' },
    { type: 3, name: 'LK_C', family: 'LK' },
  ],
};

const snapshotBody = {
  circuitId: 'c1',
  modelVersion: 0,
  filename: 'test.ipes',
  dpix: 16,
  worksheetSize: '600x600',
  components: [
    {
      type: 1,
      name: 'R1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
  ],
  connections: [{ index: 0, type: 'LK', label: 'w1', points: [[8, 10], [12, 10]] }],
};

const createResponse = {
  circuitId: 'c1',
  modelVersion: 1,
  operation: 'createComponent',
  payload: {
    type: 3,
    name: 'C',
    domain: 'LK',
    position: [6, 5],
    orientation: 503,
    parameters: {},
  },
};

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'content-type': 'application/json' }),
    json: () => Promise.resolve(body),
  } as unknown as Response;
}

function routeFetch(url: string, init?: RequestInit): Promise<Response> {
  if (url.endsWith('/circuits/catalog')) {
    return Promise.resolve(jsonResponse(catalogBody));
  }
  if (url.endsWith('/circuits/parse')) {
    return Promise.resolve(jsonResponse({ circuitId: 'c1', status: 'loaded' }, 201));
  }
  if (url.endsWith('/circuits/c1/model')) {
    return Promise.resolve(jsonResponse(snapshotBody));
  }
  if (url.endsWith('/circuits/c1/components') && init?.method === 'POST') {
    return Promise.resolve(jsonResponse(createResponse, 201));
  }
  return Promise.resolve(jsonResponse({ detail: `unmocked ${init?.method ?? 'GET'} ${url}` }, 500));
}

afterEach(() => {
  cleanup();
  fetchMock.mockReset();
});

describe('App user flow', () => {
  it('palette is armed only after a circuit is opened; then placing works end to end', async () => {
    fetchMock.mockImplementation(routeFetch);
    const { container } = render(<App />);

    // palette loads from the catalog
    await waitFor(() => {
      expect(container.querySelectorAll('.palette-entry')).toHaveLength(2);
    });

    // clicking a palette entry without an open circuit shows a hint instead of arming
    fireEvent.click(container.querySelectorAll('.palette-entry')[0]);
    const svg = (container.querySelector('svg.sheet') || container.querySelector('svg'))!;
    fireEvent.mouseMove(svg, { clientX: 64, clientY: 64, button: 0 });
    expect(container.querySelector('g.ghost')).toBeNull();
    expect(container.querySelector('.statusbar')!.textContent).toContain('Open a .ipes file');

    // open a file through the hidden input
    const fileInput = container.querySelector('input[type=file]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [new File(['x'], 'test.ipes')] } });

    await waitFor(() => {
      expect(container.querySelectorAll('g.component')).toHaveLength(1);
    });
    // wires are rendered from the snapshot
    expect(container.querySelectorAll('polyline.wire')).toHaveLength(1);

    // arm the resistor from the palette, then click the sheet to place
    fireEvent.click(container.querySelectorAll('.palette-entry')[0]);
    fireEvent.mouseMove(svg, { clientX: 100, clientY: 80, button: 0 });
    fireEvent.mouseUp(svg);

    await waitFor(() => {
      expect(container.querySelectorAll('g.component')).toHaveLength(2);
    });
    const placed = container.querySelectorAll('g.component')[1];
    expect(placed.getAttribute('transform')).toBe('translate(96, 80)');

    // the POST carried the grid coordinates and default orientation
    const createCall = fetchMock.mock.calls.find(
      ([url, init]) => url.endsWith('/circuits/c1/components') && init?.method === 'POST',
    );
    expect(JSON.parse(createCall![1]!.body)).toEqual({
      family: 'LK',
      type: 1,
      x: 6,
      y: 5,
      orientation: 503,
    });
  });
});
