// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { App } from '../src/App';
import { ControlComponentType, LkComponentType } from '../src/model/constants';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);
vi.stubGlobal(
  'WebSocket',
  class {
    close() {}
    send() {}
  },
);

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: new Headers({ 'content-type': 'application/json' }),
    json: () => Promise.resolve(body),
  } as unknown as Response;
}

const snapshot = {
  circuitId: 'c-drawer',
  modelVersion: 1,
  filename: 'drawer_test.ipes',
  dpix: 16,
  worksheetSize: '600x600',
  components: [
    {
      type: LkComponentType.RESISTOR,
      name: 'R.1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: { param0: 10 },
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: ControlComponentType.SCOPE,
      name: 'SCOPE.1',
      family: 'CONTROL',
      position: [16, 10],
      orientation: 503,
      parameters: {},
      inputLabels: ['V_out'],
      outputLabels: [],
    },
  ],
  connections: [],
};

function routeFetch(url: string, init?: RequestInit): Promise<Response> {
  if (url.endsWith('/gecko/api/health')) return Promise.resolve(jsonResponse({ status: 'UP' }));
  if (url.endsWith('/circuits/catalog')) {
    return Promise.resolve(
      jsonResponse({
        types: [
          { type: LkComponentType.RESISTOR, name: 'LK_R', family: 'LK' },
          { type: ControlComponentType.SCOPE, name: 'CTRL_SCOPE', family: 'CONTROL' },
        ],
      }),
    );
  }
  if (url.endsWith('/circuits/parse')) {
    return Promise.resolve(jsonResponse({ circuitId: 'c-drawer', status: 'loaded' }, 201));
  }
  if (url.endsWith('/circuits/c-drawer/model')) {
    return Promise.resolve(jsonResponse(snapshot));
  }
  if (url.endsWith('/simulations') && init?.method === 'POST') {
    return Promise.resolve(jsonResponse({ simulationId: 'sim-drawer-1', status: 'RUNNING' }, 201));
  }
  if (url.includes('/simulations/sim-drawer-1')) {
    return Promise.resolve(
      jsonResponse({
        status: 'COMPLETED',
        progress: 1.0,
        results: { time: [0, 1e-3], V_out: [0, 12] },
      }),
    );
  }
  return Promise.resolve(jsonResponse({ detail: 'unmocked' }, 500));
}

describe('Collapsible Bottom Waveforms Drawer', () => {
  beforeEach(() => {
    localStorage.removeItem('gecko-waveforms-drawer-ratio');
    fetchMock.mockReset();
    fetchMock.mockImplementation(routeFetch);
  });

  afterEach(() => {
    cleanup();
    localStorage.removeItem('gecko-waveforms-drawer-ratio');
  });

  it('starts clean on full-screen Schematic, toggles bottom Waveforms drawer, expands to tab, and auto-opens on Run', async () => {
    render(<App />);
    await waitFor(() => expect(screen.getByText('drawer_test.ipes')).toBeTruthy());

    const schematicPane = screen.getByTestId('workspace-pane-schematic');
    const scopePane = screen.getByTestId('workspace-pane-scope');

    // Starts clean: Schematic full-screen, Waveforms drawer hidden
    expect(schematicPane.style.display).toBe('flex');
    expect(scopePane.style.display).toBe('none');
    expect(screen.queryByTestId('workspace-split-resizer')).toBeNull();

    // Toggle Waveforms drawer open
    fireEvent.click(screen.getByTestId('toggle-waveforms-drawer-btn'));
    expect(schematicPane.style.display).toBe('flex');
    expect(scopePane.style.display).toBe('flex');
    expect(screen.getByTestId('workspace-split-resizer')).toBeTruthy();

    // Collapse drawer via close button
    fireEvent.click(screen.getByTestId('collapse-waveforms-drawer-btn'));
    expect(scopePane.style.display).toBe('none');
    expect(screen.queryByTestId('workspace-split-resizer')).toBeNull();

    // Clicking Run auto-opens the bottom Waveforms drawer
    fireEvent.click(screen.getByRole('button', { name: /▶ Run/i }));
    await waitFor(() => expect(scopePane.style.display).toBe('flex'));
    expect(schematicPane.style.display).toBe('flex');

    // Expanding to full-screen Osci tab hides Schematic and shows Scope full-screen
    fireEvent.click(screen.getByTestId('expand-waveforms-drawer-btn'));
    expect(schematicPane.style.display).toBe('none');
    expect(scopePane.style.display).toBe('flex');
    expect(screen.queryByTestId('workspace-split-resizer')).toBeNull();
  });
});
