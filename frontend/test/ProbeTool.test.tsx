// @vitest-environment jsdom
import { describe, expect, it, vi, afterEach } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { Sheet, findNextAvailableNetLabel } from '../src/canvas/Sheet';
import type { SheetActions } from '../src/canvas/Sheet';
import { editorReducer, initialState } from '../src/model/store';
import type { EditorComponent, EditorWire } from '../src/model/types';

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('Phase 3: Probe Tool & Cursor (LTspice style)', () => {
  describe('findNextAvailableNetLabel', () => {
    it('returns n1 when no wires or components exist', () => {
      expect(findNextAvailableNetLabel([], [])).toBe('n1');
    });

    it('returns n2 when n1 already exists in wire labels', () => {
      const wires: EditorWire[] = [
        { index: 0, type: 'LK', label: 'n1', points: [[0, 0], [1, 0]] },
      ];
      expect(findNextAvailableNetLabel(wires, [])).toBe('n2');
    });

    it('ignores NIX_NIX_NIX default unassigned label', () => {
      const wires: EditorWire[] = [
        { index: 0, type: 'LK', label: 'NIX_NIX_NIX', points: [[0, 0], [1, 0]] },
      ];
      expect(findNextAvailableNetLabel(wires, [])).toBe('n1');
    });

    it('avoids colliding with component names or pin labels', () => {
      const comps: EditorComponent[] = [
        {
          type: 1,
          name: 'N1',
          family: 'LK',
          position: [0, 0],
          orientation: 502,
          parameters: {},
          inputLabels: ['N2'],
          outputLabels: [],
        },
      ];
      expect(findNextAvailableNetLabel([], comps)).toBe('n3');
    });
  });

  describe('Editor reducer probe mode transitions', () => {
    it('toggles probe mode from idle to probing', () => {
      const s1 = editorReducer(initialState, { type: 'TOGGLE_PROBE_MODE' });
      expect(s1.mode).toBe('probing');
      expect(s1.probeRefNet).toBeNull();
      expect(s1.status).toContain('Probe mode');

      const s2 = editorReducer(s1, { type: 'TOGGLE_PROBE_MODE' });
      expect(s2.mode).toBe('idle');
      expect(s2.probeRefNet).toBeNull();
    });

    it('sets and clears probeRefNet', () => {
      let state = editorReducer(initialState, { type: 'TOGGLE_PROBE_MODE' });
      state = editorReducer(state, { type: 'SET_PROBE_REF_NET', netName: 'NET_A' });
      expect(state.probeRefNet).toBe('NET_A');

      state = editorReducer(state, { type: 'SET_PROBE_REF_NET', netName: null });
      expect(state.probeRefNet).toBeNull();
    });

    it('exits probe mode on CANCEL (Esc key)', () => {
      let state = editorReducer(initialState, { type: 'TOGGLE_PROBE_MODE' });
      state = editorReducer(state, { type: 'SET_PROBE_REF_NET', netName: 'NET_A' });
      state = editorReducer(state, { type: 'CANCEL' });

      expect(state.mode).toBe('idle');
      expect(state.probeRefNet).toBeNull();
      expect(state.status).toBe('Exited probe mode');
    });
  });

  describe('Sheet Canvas Probe Interactions', () => {
    const mockComponent: EditorComponent = {
      type: 1, // Resistor
      name: 'R1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: { R: 10 },
      inputLabels: [],
      outputLabels: [],
    };

    const mockWire: EditorWire = {
      index: 0,
      type: 'LK',
      label: 'OUT',
      points: [[8, 10], [12, 10]],
    };

    const mockUnlabeledWire: EditorWire = {
      index: 1,
      type: 'LK',
      label: 'NIX_NIX_NIX',
      points: [[30, 30], [35, 30]],
    };

    function renderProbeCanvas(mode: 'idle' | 'probing' = 'probing', probeRefNet: string | null = null) {
      const toggleProbeMode = vi.fn();
      const addSignalToScope = vi.fn();
      const setProbeRefNet = vi.fn();
      const labelWire = vi.fn();

      const actions: SheetActions = {
        placeGhost: vi.fn(),
        finishWire: vi.fn(),
        commitMove: vi.fn(),
        toggleProbeMode,
        addSignalToScope,
        setProbeRefNet,
        labelWire,
      };

      const testState = {
        ...initialState,
        mode,
        probeRefNet,
        components: [mockComponent],
        wires: [mockWire, mockUnlabeledWire],
      };

      const dispatch = vi.fn();
      const utils = render(<Sheet state={testState} dispatch={dispatch} actions={actions} />);

      return {
        ...utils,
        actions,
        dispatch,
      };
    }

    it('renders the floating probe banner and toolbar button in probing mode', () => {
      const { actions } = renderProbeCanvas('probing');

      // Banner should be visible
      expect(screen.getByText(/Probe Mode:/i)).toBeDefined();
      expect(screen.getByText('Exit (Esc)')).toBeDefined();

      // Probe button should have active class
      const btn = screen.getByTestId('canvas-probe-tool-btn');
      expect(btn.classList.contains('active')).toBe(true);

      // Clicking exit button in banner calls toggleProbeMode and clears ref net
      fireEvent.click(screen.getByText('Exit (Esc)'));
      expect(actions.setProbeRefNet).toHaveBeenCalledWith(null);
      expect(actions.toggleProbeMode).toHaveBeenCalled();
    });

    it('clicking toolbar probe button toggles probe mode', () => {
      const { actions } = renderProbeCanvas('idle');

      const btn = screen.getByTestId('canvas-probe-tool-btn');
      expect(btn.classList.contains('active')).toBe(false);

      fireEvent.click(btn);
      expect(actions.toggleProbeMode).toHaveBeenCalled();
    });

    it('clicking a component records its current I(comp) on graph 2', () => {
      const { container, actions } = renderProbeCanvas('probing');

      const compGroup = container.querySelector('g.component[data-name="R1"]');
      expect(compGroup).not.toBeNull();

      fireEvent.mouseDown(compGroup!, { button: 0 });
      expect(actions.addSignalToScope).toHaveBeenCalledWith('I(R1)', 2);
    });

    it('Alt+clicking a component records its branch voltage V(comp) on graph 1', () => {
      const { container, actions } = renderProbeCanvas('probing');

      const compGroup = container.querySelector('g.component[data-name="R1"]');
      expect(compGroup).not.toBeNull();

      fireEvent.mouseDown(compGroup!, { button: 0, altKey: true });
      expect(actions.addSignalToScope).toHaveBeenCalledWith('V(R1)', 1);
    });

    it('clicking a labeled wire initiates 2-step voltage probing and records single-ended V(net) on Enter or Ref to GND', () => {
      const { container, actions } = renderProbeCanvas('probing');

      const wireHits = container.querySelectorAll('polyline.wire-hit');
      expect(wireHits.length).toBeGreaterThanOrEqual(2);

      // Wire 0 has label 'OUT' - first click anchors V(+)
      fireEvent.mouseDown(wireHits[0], { button: 0 });
      expect(actions.setProbeRefNet).toHaveBeenCalledWith('OUT');

      // Click "Ref to GND (0)" button or press Enter to complete single-ended probe
      const gndBtn = screen.getByText(/Ref to GND \(0\)/i);
      expect(gndBtn).toBeDefined();
      fireEvent.click(gndBtn);

      expect(actions.addSignalToScope).toHaveBeenCalledWith('V(OUT)', 1);
    });

    it('clicking an unlabeled wire auto-labels it and records V(autoLabel)', () => {
      const { container, actions } = renderProbeCanvas('probing');

      const wireHits = container.querySelectorAll('polyline.wire-hit');
      // Wire 1 is unlabeled ('NIX_NIX_NIX')
      fireEvent.mouseDown(wireHits[1], { button: 0 });

      expect(actions.labelWire).toHaveBeenCalledWith(1, 'n1');
      expect(actions.setProbeRefNet).toHaveBeenCalledWith('n1');

      const gndBtn = screen.getByText(/Ref to GND \(0\)/i);
      fireEvent.click(gndBtn);
      expect(actions.addSignalToScope).toHaveBeenCalledWith('V(n1)', 1);
    });

    it('clicking a wire anchors it as probeRefNet for differential voltage', () => {
      const { container, actions } = renderProbeCanvas('probing');

      const wireHits = container.querySelectorAll('polyline.wire-hit');
      // Click wire 0 ('OUT')
      fireEvent.mouseDown(wireHits[0], { button: 0 });

      expect(actions.setProbeRefNet).toHaveBeenCalledWith('OUT');
      expect(actions.addSignalToScope).not.toHaveBeenCalled();
    });

    it('subsequent click with active probeRefNet records differential voltage V(netA|netB)', () => {
      // probeRefNet is already set to 'V_DC'
      const { container, actions } = renderProbeCanvas('probing', 'V_DC');

      // Banner shows guidance
      expect(screen.getByText(/Voltage Probe: V\(\+\) is 'V_DC'/i)).toBeDefined();

      const wireHits = container.querySelectorAll('polyline.wire-hit');
      // Click wire 0 ('OUT')
      fireEvent.mouseDown(wireHits[0], { button: 0 });

      expect(actions.addSignalToScope).toHaveBeenCalledWith('V(V_DC|OUT)', 1);
      expect(actions.setProbeRefNet).toHaveBeenCalledWith(null);
    });

    it('hovering over a component displays the SPICE badge tooltip', () => {
      const { container } = renderProbeCanvas('probing');

      const compGroup = container.querySelector('g.component[data-name="R1"]');
      expect(compGroup).not.toBeNull();

      fireEvent.mouseEnter(compGroup!);
      // Mouse move to establish cursorCoord
      const svg = container.querySelector('svg.sheet');
      fireEvent.mouseMove(svg!, { clientX: 160, clientY: 160 });

      const badge = container.querySelector('g.probe-cursor-badge');
      expect(badge).not.toBeNull();
      expect(badge?.textContent).toContain('+ I(R1)');
    });

    it('clicking a CONTROL wire records bare control signal name on graph 3', () => {
      const addSignalToScope = vi.fn();
      const ctrlWire: EditorWire = {
        index: 5,
        type: 'CONTROL',
        label: 'gate_hi',
        points: [
          [20, 20],
          [25, 20],
        ],
      };
      const testState = {
        ...initialState,
        mode: 'probing' as const,
        components: [],
        wires: [ctrlWire],
      };
      const actions = {
        ...({} as ReturnType<typeof import('../src/hooks/useEditor').useEditor>['actions']),
        commitMove: vi.fn(),
        addSignalToScope,
      };
      const { container } = render(<Sheet state={testState} dispatch={vi.fn()} actions={actions} />);
      const wireHit = container.querySelector('polyline.wire-hit');
      expect(wireHit).not.toBeNull();
      fireEvent.mouseDown(wireHit!, { button: 0 });
      expect(addSignalToScope).toHaveBeenCalledWith('gate_hi', 3);
    });

    it('clicking a non-branch coupler (K_coupler) does not add invalid I(K_coupler) probe', () => {
      const addSignalToScope = vi.fn();
      const coupler: EditorComponent = {
        name: 'K_coupler',
        type: 9,
        family: 'LK',
        position: [15, 15],
        orientation: 503,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      };
      const testState = {
        ...initialState,
        mode: 'probing' as const,
        components: [coupler],
        wires: [],
      };
      const actions = {
        ...({} as ReturnType<typeof import('../src/hooks/useEditor').useEditor>['actions']),
        commitMove: vi.fn(),
        addSignalToScope,
      };
      const { container } = render(<Sheet state={testState} dispatch={vi.fn()} actions={actions} />);
      const compGroup = container.querySelector('g.component[data-name="K_coupler"]');
      expect(compGroup).not.toBeNull();
      fireEvent.mouseDown(compGroup!, { button: 0 });
      expect(addSignalToScope).not.toHaveBeenCalled();
    });

    it('renders redesigned canvas controls toolbar with Wire, Probe, Probes toggle, Grid, and Labels', () => {
      const toggleWireMode = vi.fn();
      const toggleProbeMode = vi.fn();
      const actions = {
        ...({} as ReturnType<typeof import('../src/hooks/useEditor').useEditor>['actions']),
        toggleWireMode,
        toggleProbeMode,
      };
      const scopeBlock: EditorComponent = {
        name: 'SCOPE1',
        type: 51,
        family: 'CONTROL',
        position: [5, 5],
        orientation: 503,
        parameters: { signals: 'V(OUT)' },
        inputLabels: [],
        outputLabels: [],
      };
      const wire: EditorWire = {
        index: 0,
        type: 'LK',
        label: 'OUT',
        points: [[10, 10], [15, 10]],
      };
      const testState = {
        ...initialState,
        mode: 'idle' as const,
        components: [scopeBlock],
        wires: [wire],
      };

      const { container } = render(<Sheet state={testState} dispatch={vi.fn()} actions={actions} />);

      // 1. Wire button
      const wireBtn = screen.getByTestId('canvas-wire-tool-btn');
      expect(wireBtn.textContent).toContain('Wire');
      expect(wireBtn.textContent).toContain('W');
      fireEvent.click(wireBtn);
      expect(toggleWireMode).toHaveBeenCalledTimes(1);

      // 2. Probe button
      const probeBtn = screen.getByTestId('canvas-probe-tool-btn');
      expect(probeBtn.textContent).toContain('Probe');
      expect(probeBtn.textContent).toContain('P');
      fireEvent.click(probeBtn);
      expect(toggleProbeMode).toHaveBeenCalledTimes(1);

      // 3. Probes toggle button
      const probesToggleBtn = screen.getByTestId('canvas-toggle-probes-btn');
      expect(probesToggleBtn.textContent).toContain('Probes');
      // Initially probes are visible
      expect(container.querySelector('.active-probes-layer')).not.toBeNull();

      // Click to toggle off (hide probes)
      fireEvent.click(probesToggleBtn);
      expect(probesToggleBtn.textContent).toContain('OFF');
      expect(container.querySelector('.active-probes-layer')).toBeNull();

      // Click again to toggle on (show probes)
      fireEvent.click(probesToggleBtn);
      expect(container.querySelector('.active-probes-layer')).not.toBeNull();

      // 4. Grid toggle button
      const gridBtn = screen.getByTestId('canvas-grid-toggle-btn');
      expect(gridBtn.textContent).toContain('Grid');
      fireEvent.click(gridBtn);

      // 5. Labels mode button
      const labelsBtn = screen.getByTestId('canvas-labels-mode-btn');
      expect(labelsBtn.textContent).toContain('Labels');
      fireEvent.click(labelsBtn);
    });
  });
});
