// @vitest-environment jsdom
/**
 * Tests for the GUI usability upgrade: canvas quick-place strip, select tool,
 * floating selection pill, on-canvas warning badges, oscilloscope right-click
 * menu, top toolbar buttons, diagnostics badge/drawer toggle and workspace
 * tab context menu.
 */
import { useReducer } from 'react';
import { describe, expect, it, vi, afterEach } from 'vitest';
import { act, cleanup, fireEvent, render, waitFor, screen } from '@testing-library/react';
import { Sheet } from '../src/canvas/Sheet';
import type { SheetActions } from '../src/canvas/Sheet';
import { editorReducer, initialState } from '../src/model/store';
import type { EditorSnapshot } from '../src/model/types';
import { ScopeViewTab } from '../src/simulation/ScopeViewTab';
import { App } from '../src/App';

const snapshot: EditorSnapshot = {
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
    {
      type: 1000,
      name: 'GATE.1',
      family: 'CONTROL',
      position: [30, 20],
      orientation: 502,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
  ],
  connections: [],
};

function makeActions() {
  return {
    placeGhost: vi.fn(),
    finishWire: vi.fn(),
    commitMove: vi.fn(),
    setSelectMode: vi.fn(),
    armQuickComponent: vi.fn(),
    rotateComponent: vi.fn(),
    duplicateSelection: vi.fn(),
    deleteComponent: vi.fn(),
    openProperties: vi.fn(),
    addSignalToScope: vi.fn(),
  } satisfies SheetActions;
}

function SheetHarness({ actions }: { actions: SheetActions }) {
  const [state, dispatch] = useReducer(editorReducer, initialState, (init) =>
    editorReducer(init, { type: 'SNAPSHOT', snapshot }),
  );
  return (
    <>
      <button
        data-testid="select-r1"
        onClick={() => dispatch({ type: 'SELECT', name: 'R1', additive: false })}
      />
      <Sheet state={state} dispatch={dispatch} actions={actions} />
    </>
  );
}

afterEach(cleanup);

describe('Schematic canvas controls', () => {
  it('select tool button invokes action and removes redundant top quick-place strip', () => {
    const actions = makeActions();
    render(<SheetHarness actions={actions} />);

    fireEvent.click(screen.getByTestId('canvas-select-tool-btn'));
    expect(actions.setSelectMode).toHaveBeenCalled();
    expect(screen.queryByTestId('canvas-quick-place-strip')).toBeNull();
  });

  it('bottom canvas view controls toolbar can be dragged via its grip', () => {
    localStorage.removeItem('gecko_viewctrl_pos');
    render(<SheetHarness actions={makeActions()} />);
    const viewBar = screen.getByTestId('canvas-view-controls');
    fireEvent.mouseDown(screen.getByTestId('view-controls-grip'), { clientX: 20, clientY: 500 });
    fireEvent.mouseMove(window, { clientX: 180, clientY: 420 });
    fireEvent.mouseUp(window);
    expect(viewBar.style.left).not.toBe('');
    expect(localStorage.getItem('gecko_viewctrl_pos')).not.toBeNull();
  });

  it('shows the floating action pill for a selected component and dispatches its actions', () => {
    const actions = makeActions();
    render(<SheetHarness actions={actions} />);
    expect(screen.queryByTestId('component-action-pill')).toBeNull();

    fireEvent.click(screen.getByTestId('select-r1'));
    const pill = screen.getByTestId('component-action-pill');
    expect(pill).toBeTruthy();

    fireEvent.click(screen.getByTitle('Rotate 90° CW (R)'));
    expect(actions.rotateComponent).toHaveBeenCalledWith('R1');
    fireEvent.click(screen.getByTitle('Rotate 90° CCW (Shift+R)'));
    expect(actions.rotateComponent).toHaveBeenCalledWith('R1', true);
    fireEvent.click(screen.getByTitle('Duplicate (Ctrl+D)'));
    expect(actions.duplicateSelection).toHaveBeenCalled();
    fireEvent.click(screen.getByTitle('Plot Voltage V(R1)'));
    expect(actions.addSignalToScope).toHaveBeenCalledWith('V(R1)', 1);
    fireEvent.click(screen.getByTitle('Plot Current I(R1)'));
    expect(actions.addSignalToScope).toHaveBeenCalledWith('I(R1)', 2);
    fireEvent.click(screen.getByTitle('Edit Properties (Enter)'));
    expect(actions.openProperties).toHaveBeenCalledWith('R1');
  });

  it('renders an on-canvas warning badge on an uncoupled gate driver', () => {
    render(<SheetHarness actions={makeActions()} />);
    expect(screen.getByTestId('component-warning-badge-GATE.1')).toBeTruthy();
    expect(screen.queryByTestId('component-warning-badge-R1')).toBeNull();
    fireEvent.click(screen.getByTestId('component-warning-badge-GATE.1'));
  });
});

describe('Oscilloscope DSO right-click menu', () => {
  const results = {
    time: [0, 1e-4, 2e-4, 3e-4, 4e-4, 5e-4],
    V_out: [0, 50, 100, 150, 200, 250],
  };
  const comps = [
    {
      type: 1003,
      name: 'SCOPE.1',
      family: 'SCOPE',
      position: [100, 100] as [number, number],
      orientation: 0,
      parameters: {},
      inputLabels: ['V_out'],
      outputLabels: [],
    },
  ];

  it('opens the menu on right-click and runs menu actions', () => {
    const { container } = render(
      <ScopeViewTab
        selectedScope="SCOPE.1"
        components={comps}
        results={results}
        displayLayout="overlay"
      />,
    );
    const svg = container.querySelector('svg.waveform-svg, .dso-screen svg, svg') as SVGElement;
    expect(svg).toBeTruthy();
    fireEvent.contextMenu(svg, { clientX: 120, clientY: 90 });

    const menu = screen.getByTestId('scope-context-menu');
    expect(menu).toBeTruthy();
    expect(screen.getByText(/Place Cursor A Here/)).toBeTruthy();
    expect(screen.getByText(/Place Cursor B Here/)).toBeTruthy();

    fireEvent.click(screen.getByText(/Y-Axis Scale/));
    expect(screen.queryByTestId('scope-context-menu')).toBeNull();

    fireEvent.contextMenu(svg, { clientX: 120, clientY: 90 });
    fireEvent.click(screen.getByText(/Clear Cursors/));
    expect(screen.queryByTestId('scope-context-menu')).toBeNull();

    fireEvent.contextMenu(svg, { clientX: 120, clientY: 90 });
    fireEvent.click(screen.getByText(/Zoom to Fit Full Waveform/));
    expect(screen.queryByTestId('scope-context-menu')).toBeNull();
  });
});

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

function routeFetch(url: string): Promise<Response> {
  if (url.endsWith('/gecko/api/health')) return Promise.resolve(jsonResponse({ status: 'UP' }));
  if (url.endsWith('/circuits/catalog')) {
    return Promise.resolve(jsonResponse({ types: [{ type: 1, name: 'LK_R', family: 'LK' }] }));
  }
  if (url.endsWith('/circuits/parse')) {
    return Promise.resolve(jsonResponse({ circuitId: 'c1', status: 'loaded' }, 201));
  }
  if (url.endsWith('/circuits/c1/model')) return Promise.resolve(jsonResponse(snapshot));
  return Promise.resolve(jsonResponse({ detail: 'unmocked' }, 500));
}

describe('App toolbar, diagnostics and tab context menu', () => {
  it('toggles the diagnostics drawer from the navbar badge', async () => {
    fetchMock.mockReset();
    fetchMock.mockImplementation(routeFetch);
    render(<App />);
    const badge = await screen.findByTestId('navbar-diagnostics-badge');
    expect(screen.queryByTestId('diagnostics-drawer')).toBeNull();
    fireEvent.click(badge);
    expect(screen.getByTestId('diagnostics-drawer')).toBeTruthy();
    fireEvent.click(badge);
    expect(screen.queryByTestId('diagnostics-drawer')).toBeNull();
    fireEvent.click(screen.getByTestId('statusbar-diagnostics-badge'));
    expect(screen.getByTestId('diagnostics-drawer')).toBeTruthy();
  });

  it('declutters duplicate selection buttons from navbar and shows on-canvas action pill on selection', async () => {
    fetchMock.mockReset();
    fetchMock.mockImplementation(routeFetch);
    const { container } = render(<App />);
    await waitFor(() => expect(container.querySelectorAll('g.component').length).toBe(2));
    expect(screen.queryByTestId('nav-rotate-cw-btn')).toBeNull();
    expect(screen.queryByTestId('nav-rotate-ccw-btn')).toBeNull();
    expect(screen.queryByTestId('nav-duplicate-btn')).toBeNull();
    expect(screen.queryByTestId('nav-delete-btn')).toBeNull();

    // Compact Help menu provides Command Palette & Shortcuts
    fireEvent.click(screen.getByTestId('nav-help-btn'));
    expect(screen.getByTestId('nav-help-menu')).toBeTruthy();
    expect(screen.getByTestId('nav-open-command-palette-btn')).toBeTruthy();
    expect(screen.getByTestId('nav-open-shortcuts-btn')).toBeTruthy();
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByTestId('nav-help-menu')).toBeNull();

    const comp = container.querySelectorAll('g.component')[0];
    const svg = container.querySelector('svg.sheet') as SVGElement;
    fireEvent.mouseDown(comp, { clientX: 160, clientY: 160, button: 0 });
    fireEvent.mouseUp(svg, { clientX: 160, clientY: 160 });
    await waitFor(() => expect(screen.getByTestId('component-action-pill')).toBeTruthy());
  });

  it('right-click on workspace tabs opens the tab context menu and edits Osci layout', async () => {
    fetchMock.mockReset();
    fetchMock.mockImplementation(routeFetch);
    render(<App />);
    const schematicTab = await screen.findByText('📐 Schematic');
    fireEvent.contextMenu(schematicTab);
    expect(screen.getByTestId('workspace-tab-context-menu')).toBeTruthy();
    expect(screen.getByText(/Run Simulation \(F5\)/)).toBeTruthy();

    await act(async () => {
      fireEvent.click(screen.getByText(/Add New Osci Tab/));
    });
    expect(screen.queryByTestId('workspace-tab-context-menu')).toBeNull();

    const osciTabs = await screen.findAllByTitle(/Oscilloscope Register: Osci/);
    await waitFor(() =>
      expect(screen.getAllByTitle(/Oscilloscope Register: Osci/).length).toBe(2),
    );
    fireEvent.contextMenu(osciTabs[0]);
    expect(screen.getByText(/Duplicate Osci Tab/)).toBeTruthy();
    await act(async () => {
      fireEvent.click(screen.getByText(/Layout: 2 Graphs/));
    });
    await waitFor(() => expect(screen.getAllByText('2G').length).toBeGreaterThanOrEqual(1));

    fireEvent.contextMenu(screen.getAllByTitle(/Oscilloscope Register: Osci/)[0]);
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByTestId('workspace-tab-context-menu')).toBeNull();
  });
});
