// @vitest-environment jsdom
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
      inputLabels: ['1'],
      outputLabels: ['0'],
    },
    {
      type: 3,
      name: 'C1',
      family: 'LK',
      position: [20, 10],
      orientation: 503,
      parameters: {},
      inputLabels: ['0'],
      outputLabels: [],
    },
  ],
  connections: [{ index: 0, type: 'LK', label: 'w1', points: [[12, 10], [18, 10]] }],
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
  if (url.endsWith('/circuits/catalog') || url.endsWith('/catalog')) {
    return Promise.resolve(jsonResponse(catalogBody));
  }
  if (url.endsWith('/circuits/parse') || url.endsWith('/circuits/upload')) {
    return Promise.resolve(jsonResponse({ circuitId: 'c1', status: 'loaded' }, 201));
  }
  if (url.endsWith('/circuits/c1/model') || url.includes('/editor-model')) {
    return Promise.resolve(jsonResponse(snapshotBody));
  }
  if (url.includes('/circuits/c1/components') && init?.method === 'POST') {
    const body = JSON.parse(init.body as string);
    return Promise.resolve(
      jsonResponse(
        {
          circuitId: 'c1',
          modelVersion: 2,
          operation: 'createComponent',
          payload: {
            type: body.type,
            name: body.name || 'R2',
            domain: body.family || 'LK',
            position: [body.x, body.y],
            orientation: body.orientation || 503,
            parameters: body.parameters || {},
          },
        },
        201,
      ),
    );
  }
  if (url.includes('/circuits/c1/connections') && init?.method === 'POST') {
    const body = JSON.parse(init.body as string);
    return Promise.resolve(
      jsonResponse(
        {
          circuitId: 'c1',
          modelVersion: 3,
          operation: 'createConnection',
          payload: {
            index: 1,
            type: body.type,
            label: body.label || '',
            points: body.points,
          },
        },
        201,
      ),
    );
  }
  if (url.includes('/undo') || url.includes('/redo')) {
    return Promise.resolve(
      jsonResponse({
        circuitId: 'c1',
        modelVersion: 4,
        operation: 'undo',
      }),
    );
  }
  return Promise.resolve(jsonResponse({ detail: `unmocked ${init?.method ?? 'GET'} ${url}` }, 500));
}

afterEach(() => {
  cleanup();
  fetchMock.mockReset();
});

describe('Keyboard-first navigation and workflows (P3)', () => {
  it('loads circuit and navigates terminals using Tab / Shift+Tab', async () => {
    fetchMock.mockImplementation(routeFetch);
    const { container } = render(<App />);

    const fileInput = container.querySelector('input[type=file]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [new File(['x'], 'test.ipes')] } });

    await waitFor(() => {
      expect(container.querySelectorAll('g.component')).toHaveLength(2);
    });

    // Press Tab to cycle focused terminal
    fireEvent.keyDown(window, { key: 'Tab' });
    expect(container.querySelector('.focused-terminal-ring')).not.toBeNull();

    // Press Shift+Tab to cycle back
    fireEvent.keyDown(window, { key: 'Tab', shiftKey: true });
    expect(container.querySelector('.focused-terminal-ring')).not.toBeNull();
  });

  it('toggles wire mode with W and steers wire draft using Arrow keys and Enter', async () => {
    fetchMock.mockImplementation(routeFetch);
    const { container } = render(<App />);

    const fileInput = container.querySelector('input[type=file]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [new File(['x'], 'test.ipes')] } });

    await waitFor(() => {
      expect(container.querySelectorAll('g.component')).toHaveLength(2);
    });

    // Press W to enter wire mode
    fireEvent.keyDown(window, { key: 'w' });
    expect(container.querySelector('.status-mode-pill')?.textContent).toBe('WIRING');

    // Press Tab to focus a terminal
    fireEvent.keyDown(window, { key: 'Tab' });

    // Press Enter to start wire at focused terminal
    fireEvent.keyDown(window, { key: 'Enter' });
    expect(container.querySelector('.wire-draft')).not.toBeNull();

    // Press ArrowRight to extend wire
    fireEvent.keyDown(window, { key: 'ArrowRight' });
    fireEvent.keyDown(window, { key: 'ArrowRight', shiftKey: true });

    // Press Enter to commit wire
    fireEvent.keyDown(window, { key: 'Enter' });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/connections'),
        expect.objectContaining({ method: 'POST' }),
      );
    });
  });

  it('nudges and duplicates selected components with Arrow keys and Ctrl+D', async () => {
    fetchMock.mockImplementation(routeFetch);
    const { container } = render(<App />);

    const fileInput = container.querySelector('input[type=file]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [new File(['x'], 'test.ipes')] } });

    await waitFor(() => {
      expect(container.querySelectorAll('g.component')).toHaveLength(2);
    });

    // Click on component R1 to select it
    const comp = container.querySelector('g.component')!;
    fireEvent.mouseDown(comp, { clientX: 160, clientY: 160, button: 0 });

    // Nudge with Arrow keys
    fireEvent.keyDown(window, { key: 'ArrowRight' });
    fireEvent.keyDown(window, { key: 'ArrowDown', shiftKey: true });

    // Duplicate with Ctrl+D
    fireEvent.keyDown(window, { key: 'd', ctrlKey: true });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/components'),
        expect.objectContaining({ method: 'POST' }),
      );
    });
  });

  it('opens and closes shortcuts cheatsheet modal on ? key', async () => {
    fetchMock.mockImplementation(routeFetch);
    const { container } = render(<App />);

    expect(container.querySelector('.shortcuts-modal')).toBeNull();

    // Press ?
    fireEvent.keyDown(window, { key: '?' });
    expect(container.querySelector('.shortcuts-modal')).not.toBeNull();
    expect(container.textContent).toContain('Keyboard Shortcuts Cheatsheet');

    // Press Escape to close
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(container.querySelector('.shortcuts-modal')).toBeNull();
  });
});
