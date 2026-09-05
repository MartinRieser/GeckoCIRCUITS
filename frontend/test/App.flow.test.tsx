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
  it('auto-creates a blank workspace on load, then palette arm + place works end to end', async () => {
    fetchMock.mockImplementation(routeFetch);
    const { container } = render(<App />);

    // palette loads from the catalog
    await waitFor(() => {
      expect(container.querySelectorAll('.palette-entry')).toHaveLength(2);
    });

    // a blank workspace auto-initializes on load, so the snapshot's
    // resistor and wire render without any user file open
    await waitFor(() => {
      expect(container.querySelectorAll('g.component')).toHaveLength(1);
    });
    expect(container.querySelectorAll('polyline.wire')).toHaveLength(1);

    // arming a palette entry works right away on the auto-created workspace;
    // arm() awaits workspace initialization (async) before dispatching, so
    // retry until the ghost renders
    fireEvent.click(container.querySelectorAll('.palette-entry')[0]);
    const svg = (container.querySelector('svg.sheet') || container.querySelector('svg'))!;
    await waitFor(() => {
      fireEvent.mouseMove(svg, { clientX: 100, clientY: 80, button: 0 });
      expect(container.querySelector('g.ghost')).not.toBeNull();
    });

    // the ghost that appeared may carry ARM's default position; move it to the
    // target cell now that placing mode is active, then click the sheet to place
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

    // opening a file through the hidden input reloads the model from the server
    const fileInput = container.querySelector('input[type=file]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [new File(['x'], 'test.ipes')] } });

    await waitFor(() => {
      expect(container.querySelector('.status-bar')!.textContent).toContain('Loaded test.ipes');
    });
    // the reload replaced the workspace with the file's snapshot content
    expect(container.querySelectorAll('g.component')).toHaveLength(1);
  });

  it('toggles theme between dark and light', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      json: async () => catalogBody,
    });

    const { container } = render(<App />);
    const themeBtn = container.querySelector('.nav-btn.theme-toggle') as HTMLButtonElement;
    expect(themeBtn).toBeDefined();

    // Default dark theme
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    // Click to toggle to light
    fireEvent.click(themeBtn);
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('gecko-theme')).toBe('light');

    // Click to toggle back to dark
    fireEvent.click(themeBtn);
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('gecko-theme')).toBe('dark');
  });
});
