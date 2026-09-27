// @vitest-environment jsdom
import { describe, expect, it, vi, afterEach } from 'vitest';
import { cleanup, fireEvent, render, waitFor, screen } from '@testing-library/react';
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

const catalogBody = {
  types: [
    { type: LkComponentType.RESISTOR, name: 'LK_R', family: 'LK' },
    { type: ControlComponentType.SCOPE, name: 'CTRL_SCOPE', family: 'CONTROL' },
    { type: ControlComponentType.SCRIPT, name: 'CTRL_SCRIPT', family: 'CONTROL' },
  ],
};

const circuitSnapshot = {
  circuitId: 'circuit-multi-tab',
  modelVersion: 1,
  filename: 'multitab_test.ipes',
  dpix: 16,
  worksheetSize: '600x600',
  components: [
    {
      type: LkComponentType.RESISTOR,
      name: 'R1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: { R: '100' },
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: ControlComponentType.SCOPE,
      name: 'SCOPE.1',
      family: 'CONTROL',
      position: [16, 10],
      orientation: 503,
      parameters: { no_signals: '1' },
      inputLabels: ['V_out'],
      outputLabels: [],
    },
    {
      type: ControlComponentType.SCRIPT,
      name: 'SCRIPT.1',
      family: 'CONTROL',
      position: [20, 10],
      orientation: 502,
      parameters: {
        sourceCode: 'yOUT[0] = xIN[0] * 5;',
        anzXIN: '1',
        anzYOUT: '1',
      },
      inputLabels: ['sig_in'],
      outputLabels: ['sig_out'],
    },
  ],
  connections: [],
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
    return Promise.resolve(jsonResponse({ circuitId: 'circuit-multi-tab', status: 'loaded' }, 201));
  }
  if (url.includes('/model')) {
    return Promise.resolve(jsonResponse(circuitSnapshot));
  }
  if (url.endsWith('/simulations') && init?.method === 'POST') {
    return Promise.resolve(jsonResponse({ simulationId: 'sim-test-1', status: 'RUNNING' }, 201));
  }
  if (url.includes('/simulations/sim-test-1/status')) {
    return Promise.resolve(jsonResponse({ status: 'FINISHED', progress: 1.0, results: { V_out: [0, 1, 2] } }));
  }
  return Promise.resolve(jsonResponse({ detail: `unmocked ${init?.method ?? 'GET'} ${url}` }, 500));
}

describe('Multi-Tab Workspace & Unified Simulation Setup', () => {
  afterEach(() => {
    cleanup();
    fetchMock.mockReset();
  });

  it('renders dynamic tabs for schematic, scope, and script components', async () => {
    fetchMock.mockImplementation(routeFetch);
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('multitab_test.ipes')).not.toBeNull();
    });

    // Check workspace tabs
    expect(screen.getByRole('button', { name: /Schematic/i })).not.toBeNull();
    expect(screen.getByRole('button', { name: /SCOPE.1/i })).not.toBeNull();
    expect(screen.getByRole('button', { name: /SCRIPT.1/i })).not.toBeNull();
  });

  it('switches to scope tab and script tab upon clicking their tabs', async () => {
    fetchMock.mockImplementation(routeFetch);
    const { container } = render(<App />);

    await waitFor(() => {
      expect(screen.getByText('multitab_test.ipes')).not.toBeNull();
    });

    // Click SCOPE.1 tab
    const scopeTab = screen.getByRole('button', { name: /SCOPE.1/i });
    fireEvent.click(scopeTab);

    await waitFor(() => {
      expect(screen.getByText(/Scope: SCOPE.1/i)).not.toBeNull();
    });

    // Click SCRIPT.1 tab
    const scriptTab = screen.getByRole('button', { name: /SCRIPT.1/i });
    fireEvent.click(scriptTab);

    await waitFor(() => {
      expect(screen.getByText('Function Block (Script)')).not.toBeNull();
      const textarea = container.querySelector('textarea');
      expect(textarea?.value).toBe('yOUT[0] = xIN[0] * 5;');
    });
  });

  it('opens simulation setup modal via gear icon', async () => {
    fetchMock.mockImplementation(routeFetch);
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('multitab_test.ipes')).not.toBeNull();
    });

    const setupGearBtn = screen.getByRole('button', { name: /Simulation Parameters Setup/i });
    fireEvent.click(setupGearBtn);

    expect(screen.getByText('⚙ Simulation Configuration')).not.toBeNull();
    expect(screen.getByLabelText(/Total Duration/i)).not.toBeNull();
  });

  it('triggers quick run simulation on F5 or Run button', async () => {
    fetchMock.mockImplementation(routeFetch);
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('multitab_test.ipes')).not.toBeNull();
    });

    const runBtn = screen.getByRole('button', { name: /▶ Run/i });
    fireEvent.click(runBtn);

    // Verify simulation POST was triggered
    await waitFor(() => {
      const simCalls = fetchMock.mock.calls.filter((c) => String(c[0]).includes('/simulations'));
      expect(simCalls.length).toBeGreaterThan(0);
    });
  });
});
