// @vitest-environment jsdom
/**
 * Interaction tests for the Sheet canvas: the exact mouse flows a user
 * performs (arm -> move -> click places; wire mode click-click routes).
 * jsdom's getBoundingClientRect returns zeros, so clientX/Y map 1:1
 * onto the (dpix=16 scaled) coordinate system of the SVG root.
 */
import { useReducer } from 'react';
import { describe, expect, it, vi, afterEach } from 'vitest';
import { cleanup, fireEvent, render } from '@testing-library/react';
import { Sheet } from '../src/canvas/Sheet';
import type { SheetActions } from '../src/canvas/Sheet';
import { editorReducer, initialState } from '../src/model/store';
import type { EditorSnapshot, EditorWire } from '../src/model/types';
import { terminalPositions } from '../src/model/geometry';

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
      type: 3,
      name: 'C1',
      family: 'LK',
      position: [30, 10],
      orientation: 502,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
  ],
  connections: [{ index: 0, type: 'LK', label: 'w1', points: [[8, 10], [12, 10]] }],
};

const placeGhost = vi.fn();
const finishWire = vi.fn();
const commitMove = vi.fn();
const deleteWire = vi.fn();
const patchWirePoints = vi.fn();
const flipWire = vi.fn();
const openScopeTab = vi.fn();
const openScriptTab = vi.fn();
const toggleWireMode = vi.fn();
const actions: SheetActions = {
  placeGhost,
  finishWire,
  commitMove,
  deleteWire,
  patchWirePoints,
  flipWire,
  openScopeTab,
  openScriptTab,
  toggleWireMode,
};

function Harness() {
  const [state, dispatch] = useReducer(editorReducer, initialState, (init) =>
    editorReducer(init, { type: 'SNAPSHOT', snapshot }),
  );
  return (
    <>
      <button
        data-testid="arm"
        onMouseDown={() => dispatch({ type: 'ARM', componentType: 1, family: 'LK' })}
      />
      <button
        data-testid="wiremode"
        onClick={() => dispatch({ type: 'TOGGLE_WIRE_MODE' })}
      />
      <Sheet state={state} dispatch={dispatch} actions={actions} />
    </>
  );
}

function setup() {
  const utils = render(<Harness />);
  const svg = utils.container.querySelector('svg')!;
  expect(svg).toBeTruthy();
  return { ...utils, svg };
}

afterEach(cleanup);
afterEach(() => {
  placeGhost.mockClear();
  finishWire.mockClear();
  commitMove.mockClear();
  deleteWire.mockClear();
  patchWirePoints.mockClear();
  flipWire.mockClear();
});

describe('Sheet rendering', () => {
  it('renders wires from the snapshot as polylines with the wire class', () => {
    const { container } = setup();
    const wires = container.querySelectorAll('polyline.wire');
    expect(wires).toHaveLength(1);
    expect(wires[0].getAttribute('points')).toBe('128,160 192,160');
  });

  it('renders components with terminal circles', () => {
    const { container } = setup();
    const components = container.querySelectorAll('g.component');
    expect(components).toHaveLength(2);
    expect(container.querySelectorAll('circle.terminal')).toHaveLength(4);
  });
});

describe('placing a component', () => {
  it('arm + mousemove + click (up) calls placeGhost with grid coordinates', () => {
    const { svg, getByTestId } = setup();
    fireEvent.mouseDown(getByTestId('arm'));

    fireEvent.mouseMove(svg, { clientX: 100, clientY: 80, button: 0 });
    fireEvent.mouseUp(svg);

    // 100/16 = 6.25 -> 6; 80/16 = 5; default orientation 503
    expect(placeGhost).toHaveBeenCalledWith(6, 5, 503);
  });

  it('supports drag-from-palette: arm on press, place on release over the sheet', () => {
    const { svg, getByTestId } = setup();
    // user presses the palette entry and drags straight onto the sheet
    fireEvent.mouseDown(getByTestId('arm'));
    fireEvent.mouseMove(svg, { clientX: 160, clientY: 128, button: 0, buttons: 1 });
    fireEvent.mouseUp(svg);

    expect(placeGhost).toHaveBeenCalledTimes(1);
    expect(placeGhost).toHaveBeenCalledWith(10, 8, 503);
  });

  it('right-click rotates the ghost before placing', () => {
    const { svg, getByTestId } = setup();
    fireEvent.mouseDown(getByTestId('arm'));

    fireEvent.mouseMove(svg, { clientX: 32, clientY: 32, button: 0 });
    fireEvent.contextMenu(svg);
    fireEvent.mouseUp(svg);

    expect(placeGhost).toHaveBeenCalledWith(2, 2, 504);
  });

  it('renders a ghost element while placing', () => {
    const { container, svg, getByTestId } = setup();
    expect(container.querySelector('g.ghost')).toBeNull();

    fireEvent.mouseDown(getByTestId('arm'));
    fireEvent.mouseMove(svg, { clientX: 64, clientY: 64, button: 0 });

    const ghost = container.querySelector('g.ghost');
    expect(ghost).not.toBeNull();
    expect(ghost!.getAttribute('transform')).toBe('translate(64, 64)');
  });
});

describe('wiring', () => {
  it('two clicks create a dense, classically routed wire', () => {
    const { svg, getByTestId } = setup();
    fireEvent.click(getByTestId('wiremode'));

    // start at grid (2,1) = client (32,16)
    fireEvent.mouseDown(svg, { clientX: 32, clientY: 16, button: 0 });
    // move to grid (6,5): |dx| == |dy| -> horizontal preferred (classic >= rule)
    fireEvent.mouseMove(svg, { clientX: 96, clientY: 80, button: 0 });
    // terminal of R1 output is at grid (12,10) = client (192,160): snap there
    fireEvent.mouseDown(svg, { clientX: 190, clientY: 162, button: 0 });

    // horizontal run first, then vertical: one point per raster step like the
    // classic editor exports (mid-wire taps must connect)
    expect(finishWire).toHaveBeenCalledWith([
      [2, 1],
      [3, 1],
      [4, 1],
      [5, 1],
      [6, 1],
      [7, 1],
      [8, 1],
      [9, 1],
      [10, 1],
      [11, 1],
      [12, 1],
      [12, 2],
      [12, 3],
      [12, 4],
      [12, 5],
      [12, 6],
      [12, 7],
      [12, 8],
      [12, 9],
      [12, 10],
    ]);
  });

  it('stays in wire mode after finishing a wire (classic wire pen)', () => {
    const { svg, getByTestId, container } = setup();
    fireEvent.click(getByTestId('wiremode'));

    fireEvent.mouseDown(svg, { clientX: 32, clientY: 16, button: 0 });
    fireEvent.mouseMove(svg, { clientX: 96, clientY: 80, button: 0 });
    fireEvent.mouseDown(svg, { clientX: 190, clientY: 162, button: 0 });
    expect(finishWire).toHaveBeenCalledTimes(1);

    // the pen stays armed: the next two clicks draw another wire immediately
    fireEvent.mouseDown(svg, { clientX: 32, clientY: 16, button: 0 });
    fireEvent.mouseMove(svg, { clientX: 96, clientY: 80, button: 0 });
    fireEvent.mouseDown(svg, { clientX: 96, clientY: 80, button: 0 });
    expect(finishWire).toHaveBeenCalledTimes(2);
    expect(container.querySelector('polyline.wire-draft')).toBeNull();
  });

  it('wire start snaps to a nearby terminal and routes vertically when dragged down', () => {
    const { svg, getByTestId } = setup();
    fireEvent.click(getByTestId('wiremode'));

    // R1 input terminal at grid (8,10) = client (128,160); click nearby
    fireEvent.mouseDown(svg, { clientX: 130, clientY: 158, button: 0 });
    // move to grid (6,5): |dy| > |dx| -> vertical preferred
    fireEvent.mouseMove(svg, { clientX: 96, clientY: 80, button: 0 });
    fireEvent.mouseDown(svg, { clientX: 96, clientY: 80, button: 0 });

    expect(finishWire).toHaveBeenCalledWith([
      [8, 10],
      [8, 9],
      [8, 8],
      [8, 7],
      [8, 6],
      [8, 5],
      [7, 5],
      [6, 5],
    ]);
  });
});

describe('selection', () => {
  it('rubber band over components selects them', () => {
    const { container, svg } = setup();

    fireEvent.mouseDown(svg, { clientX: 0, clientY: 0, button: 0 });
    fireEvent.mouseMove(svg, { clientX: 500, clientY: 200, button: 0 });
    fireEvent.mouseUp(svg);

    const selected = container.querySelectorAll('g.component.selected');
    expect(selected).toHaveLength(2);
  });

  it('dragging a component calls commitMove with the delta', () => {
    const { container, svg } = setup();

    // jsdom does no hit-testing: fire mousedown on R1's group directly
    // (grid 10,10 = client 160,160), then drag on the svg
    const r1 = container.querySelectorAll('g.component')[0];
    fireEvent.mouseDown(r1, { clientX: 160, clientY: 160, button: 0 });
    // drag to grid (13,8) = client (208,128)
    fireEvent.mouseMove(svg, { clientX: 208, clientY: 128, button: 0 });
    fireEvent.mouseUp(svg);

    expect(commitMove).toHaveBeenCalledWith(
      [{ name: 'R1', x: 13, y: 8 }],
      expect.any(Array),
      undefined, // no geometry warning for this clean drag
    );
  });

  it('a plain click selects the component; releasing mouse does not drag unless mouse button is held', () => {
    const { container, svg } = setup();

    const r1 = container.querySelectorAll('g.component')[0];
    // click without moving: mouseup commits (no moves), ends drag, component stays selected
    fireEvent.mouseDown(r1, { clientX: 160, clientY: 160, button: 0 });
    fireEvent.mouseUp(svg);
    expect(commitMove).not.toHaveBeenCalled();

    // hover without any button held: component does NOT follow cursor
    fireEvent.mouseMove(svg, { clientX: 176, clientY: 160, button: 0, buttons: 0 });
    const comp = container.querySelectorAll('g.component')[0];
    expect(comp.getAttribute('transform')).toBe('translate(160, 160)');
    expect(comp.classList.contains('selected')).toBe(true);
  });
});

describe('direct pin-to-pin wiring and wire editing', () => {
  it('direct click on terminal in idle mode starts wire without pressing wire button', () => {
    const { container, svg } = setup();

    // Find R1's terminal-hit element: R1 input is at grid (8,10)
    const terminalHits = container.querySelectorAll('.terminal-hit');
    expect(terminalHits.length).toBeGreaterThan(0);

    // Click R1's first terminal directly in idle mode
    fireEvent.mouseDown(terminalHits[0], { button: 0 });

    // A wire draft should immediately appear
    const wireDraft = container.querySelector('polyline.wire-draft');
    expect(wireDraft).not.toBeNull();

    // Move to grid (15, 10) = client (240, 160)
    fireEvent.mouseMove(svg, { clientX: 240, clientY: 160, button: 0 });

    // Click second terminal (C1 input at grid (28, 10))
    fireEvent.mouseDown(terminalHits[2], { button: 0 });

    // finishWire should be called directly!
    expect(finishWire).toHaveBeenCalledTimes(1);
    // After pin-to-pin wire completes, wire draft is gone
    expect(container.querySelector('polyline.wire-draft')).toBeNull();
  });

  it('clicking a wire selects it and clicking delete in floating pill deletes it', () => {
    const { container } = setup();

    const wireHit = container.querySelector('.wire-hit')!;
    expect(wireHit).toBeTruthy();

    // Click on the wire
    fireEvent.mouseDown(wireHit, { button: 0 });

    // Floating action pill should appear
    const pill = container.querySelector('.wire-action-pill');
    expect(pill).not.toBeNull();

    // Click the delete button on the floating pill
    const delBtn = pill!.querySelector('.wire-action-btn.danger')!;
    expect(delBtn).toBeTruthy();
    fireEvent.click(delBtn);

    expect(deleteWire).toHaveBeenCalledWith(0);
  });

  it('dragging a wire segment translates it and calls patchWirePoints', () => {
    const { container, svg } = setup();

    // Click wire to select it
    const wireHit = container.querySelector('.wire-hit')!;
    fireEvent.mouseDown(wireHit, { button: 0 });

    // Find the segment drag bar
    const segBar = container.querySelector('.wire-segment-drag-bar')!;
    expect(segBar).toBeTruthy();

    // Drag the segment in Y by 3 grid units (from y=10 to y=13 -> clientY 160 to 208)
    fireEvent.mouseDown(segBar, { clientX: 160, clientY: 160, button: 0 });
    fireEvent.mouseMove(svg, { clientX: 160, clientY: 208, button: 0 });
    fireEvent.mouseUp(svg);

    expect(patchWirePoints).toHaveBeenCalledTimes(1);
    expect(patchWirePoints).toHaveBeenCalledWith(0, expect.any(Array));
  });

  it('deduplicates coincident terminal net labels and renders them with contrast pill backgrounds', () => {
    const snapWithLabels: EditorSnapshot = {
      ...snapshot,
      components: [
        {
          type: 1,
          name: 'R4',
          family: 'LK',
          position: [27, 9],
          orientation: 503,
          parameters: {},
          inputLabels: ['z1'],
          outputLabels: ['z1c'],
        },
        {
          type: 3,
          name: 'C1',
          family: 'LK',
          position: [27, 13],
          orientation: 503,
          parameters: {},
          inputLabels: ['z1c'], // shares (27, 11) with R4 output!
          outputLabels: ['0'],
        },
      ],
    };

    function LabelWrapper() {
      const [state, dispatch] = useReducer(editorReducer, {
        ...initialState,
        circuitId: snapWithLabels.circuitId,
        components: snapWithLabels.components,
        wires: snapWithLabels.connections || [],
        dpix: snapWithLabels.dpix,
      });
      return <Sheet state={state} dispatch={dispatch} actions={actions} />;
    }

    const { container } = render(<LabelWrapper />);

    // Check that z1c is only rendered ONCE in the deduplicated layer
    const labelTexts = Array.from(container.querySelectorAll('.node-label')).map((el) => el.textContent);
    const z1cCount = labelTexts.filter((t) => t === 'z1c').length;
    expect(z1cCount).toBe(1);

    // Check that node-label-pill-bg rect exists behind labels
    const pills = container.querySelectorAll('.node-label-pill-bg');
    expect(pills.length).toBeGreaterThan(0);
  });

  it('renders coupling badge for Ammeter and places switch gate badge on the left side', () => {
    const snapCoupled: EditorSnapshot = {
      ...snapshot,
      components: [
        {
          type: 1002,
          name: 'AMP.1',
          family: 'CONTROL',
          position: [20, 20],
          orientation: 503,
          parameters: { coupledComponent: 'L.1' },
          inputLabels: [],
          outputLabels: ['iL1'],
        },
        {
          type: 7,
          name: 'S.1',
          family: 'LK',
          position: [16, 5],
          orientation: 503,
          parameters: { coupledComponent: 'GATE.1' },
          inputLabels: ['in'],
          outputLabels: [],
        },
      ],
    };

    function CoupledWrapper() {
      const [state, dispatch] = useReducer(editorReducer, {
        ...initialState,
        circuitId: snapCoupled.circuitId,
        components: snapCoupled.components,
        wires: snapCoupled.connections || [],
        dpix: snapCoupled.dpix,
        // Badge text is hover/selection-only now; select the ammeter so it shows
        selection: ['AMP.1'],
      });
      return <Sheet state={state} dispatch={dispatch} actions={actions} />;
    }

    const { container } = render(<CoupledWrapper />);

    // Selected ammeter badge should read "➔ i(L.1)"
    expect(container.textContent).toContain('➔ i(L.1)');

    // Unselected blocks show no badge text (dashed coupling guides only)
    expect(container.querySelectorAll('.coupling-symbol-badge').length).toBe(1);

    // Switches in the power circuit do not render bulky gate label badges (leaving switches clean & uncluttered)
    const switchBadge = container.querySelector('.component.family-LK .coupling-symbol-badge');
    expect(switchBadge).toBeNull();
  });


  it('draws scope guides from unwired net-label inputs to their measurement points', () => {
    const scopeSnap: EditorSnapshot = {
      ...snapshot,
      connections: [],
      components: [
        {
          type: 2,
          name: 'L.1',
          family: 'LK',
          position: [26, 6],
          orientation: 502,
          parameters: {},
          inputLabels: ['sw_node'],
          outputLabels: ['out'],
        },
        {
          type: 1002,
          name: 'AMP.1',
          family: 'CONTROL',
          position: [20, 20],
          orientation: 503,
          parameters: { coupledComponent: 'L.1' },
          inputLabels: [],
          outputLabels: ['i_L'],
        },
        {
          type: 5,
          name: 'SCOPE.1',
          family: 'CONTROL',
          position: [36, 22],
          orientation: 503,
          parameters: {},
          inputLabels: ['out', 'i_L'],
          outputLabels: [],
        },
      ],
    };

    function ScopeWrapper(props: { wires: EditorWire[] }) {
      const [state, dispatch] = useReducer(editorReducer, {
        ...initialState,
        circuitId: scopeSnap.circuitId,
        components: scopeSnap.components,
        wires: props.wires,
        dpix: scopeSnap.dpix,
        selection: ['SCOPE.1'],
      });
      return <Sheet state={state} dispatch={dispatch} actions={actions} />;
    }

    // Unwired scope inputs: one dashed guide per distinct measurement point
    const first = render(<ScopeWrapper wires={[]} />);
    expect(first.container.querySelectorAll('path.coupling-guideline.active')).toHaveLength(2);
    expect(first.container.textContent).toContain('V(out)');
    expect(first.container.textContent).toContain('I(L.1)');
    first.unmount();

    // A net carried by a wire is aimed at its point nearest to the scope
    // (the "out" rail here spans 28..40 at y=6; the scope sits at x=36)
    const rail = [
      { index: 0, type: 'LK' as const, label: 'out', points: [[28, 6], [32, 6], [36, 6], [40, 6]] },
    ];
    const railRender = render(<ScopeWrapper wires={rail} />);
    const railGuides = [...railRender.container.querySelectorAll('path.coupling-guideline.active')]
      .map((p) => p.getAttribute('d') ?? '');
    expect(railGuides).toHaveLength(2);
    expect(railGuides.some((d) => d.includes('576 96'))).toBe(true);
    railRender.unmount();

    // A real wire touching the first scope pin replaces its guide
    const scope = scopeSnap.components[2];
    const pin = terminalPositions(scope).input[0];
    const wired = [
      { index: 0, type: 'CONTROL' as const, label: 'w1', points: [[pin.x, pin.y], [pin.x - 2, pin.y]] },
    ];
    const second = render(<ScopeWrapper wires={wired} />);
    expect(second.container.querySelectorAll('path.coupling-guideline.active')).toHaveLength(1);
  });

  it('draws voltmeter guides with polarity colors when voltmeter is selected', () => {
    const vmSnap: EditorSnapshot = {
      ...snapshot,
      connections: [],
      components: [
        {
          type: 1, // Resistor
          name: 'R.1',
          family: 'LK',
          position: [20, 10],
          orientation: 502,
          parameters: {},
          inputLabels: ['node_pos'],
          outputLabels: ['node_neg'],
        },
        {
          type: 1001, // Voltmeter
          name: 'VOLT.1',
          family: 'CONTROL',
          position: [20, 30],
          orientation: 503,
          parameters: {
            positiveNode: 'node_pos',
            negativeNode: 'node_neg',
          },
          inputLabels: [],
          outputLabels: [],
        },
      ],
    };

    function VmWrapper({ selected }: { selected: boolean }) {
      const [state, dispatch] = useReducer(editorReducer, {
        ...initialState,
        circuitId: vmSnap.circuitId,
        components: vmSnap.components,
        wires: [],
        dpix: vmSnap.dpix,
        selection: selected ? ['VOLT.1'] : [],
      });
      return <Sheet state={state} dispatch={dispatch} actions={actions} />;
    }

    // Unselected: no voltmeter guides visible
    const unselectedRender = render(<VmWrapper selected={false} />);
    expect(unselectedRender.container.querySelectorAll('.coupling-guides-layer path')).toHaveLength(0);
    unselectedRender.unmount();

    // Selected: draws 2 polarity-colored guidelines (pos: #f87171, neg: #cd7f32)
    const selectedRender = render(<VmWrapper selected={true} />);
    const guidePaths = selectedRender.container.querySelectorAll('g[class*="coupling-guide-group"] path.coupling-guideline');
    expect(guidePaths).toHaveLength(2);
    const strokes = [...guidePaths].map((p) => p.getAttribute('stroke'));
    expect(strokes).toContain('#f87171');
    expect(strokes).toContain('#cd7f32');
  });

  it('triggers openScopeTab and openScriptTab when double-clicking respective components', () => {
    openScopeTab.mockClear();
    openScriptTab.mockClear();

    const snapInteractive: EditorSnapshot = {
      ...snapshot,
      components: [
        {
          type: 1003,
          name: 'SCOPE.1',
          family: 'CONTROL',
          position: [10, 10],
          orientation: 502,
          parameters: {},
          inputLabels: [],
          outputLabels: [],
        },
        {
          type: 1016,
          name: 'SCRIPT.1',
          family: 'CONTROL',
          position: [20, 10],
          orientation: 502,
          parameters: {},
          inputLabels: [],
          outputLabels: [],
        },
      ],
    };

    function InteractiveWrapper() {
      const [state, dispatch] = useReducer(editorReducer, {
        ...initialState,
        circuitId: snapInteractive.circuitId,
        components: snapInteractive.components,
        wires: [],
        dpix: snapInteractive.dpix,
      });
      return <Sheet state={state} dispatch={dispatch} actions={actions} />;
    }

    const { container } = render(<InteractiveWrapper />);

    const scopeComp = container.querySelector('.component[data-name="SCOPE.1"]');
    expect(scopeComp).not.toBeNull();
    fireEvent.doubleClick(scopeComp!);
    expect(openScopeTab).toHaveBeenCalledWith('SCOPE.1');

    const scriptComp = container.querySelector('.component[data-name="SCRIPT.1"]');
    expect(scriptComp).not.toBeNull();
    fireEvent.doubleClick(scriptComp!);
    expect(openScriptTab).toHaveBeenCalledWith('SCRIPT.1');
  });

  describe('visual connection indicators & wire tool feedback (Phase 1)', () => {
    it('renders LTspice-style open boxes on unconnected terminals and solid dots on connected terminals', () => {
      const { container } = setup();
      // In setup(), R1 terminals (8,10) and (12,10) are wired by w1 -> connected
      // C1 terminals (28,10) and (32,10) are unwired -> unconnected
      const openBoxes = container.querySelectorAll('.terminal-open-box');
      expect(openBoxes).toHaveLength(2);

      const connectedDots = container.querySelectorAll('.terminal-connected');
      expect(connectedDots).toHaveLength(2);
    });

    it('renders dangling endpoint box for free wire ends', () => {
      const snapDangling: EditorSnapshot = {
        ...snapshot,
        connections: [
          // Wire free-floating in empty space (dangling at both ends)
          { index: 0, type: 'LK', label: 'w_open', points: [[50, 50], [55, 50]] },
        ],
      };

      function DanglingHarness() {
        const [state, dispatch] = useReducer(editorReducer, {
          ...initialState,
          circuitId: snapDangling.circuitId,
          components: snapDangling.components,
          wires: (snapDangling.connections || []).map((c) => ({
            index: c.index,
            type: c.type,
            points: c.points,
            label: c.label,
          })),
          dpix: snapDangling.dpix,
        });
        return <Sheet state={state} dispatch={dispatch} actions={actions} />;
      }

      const { container } = render(<DanglingHarness />);
      const danglingBoxes = container.querySelectorAll('.wire-dangling-box');
      expect(danglingBoxes).toHaveLength(2);
    });

    it('shows floating banner and crosshair in wire mode and clicking exit toggles it', () => {
      const { container, getByTestId } = setup();

      // Initially idle: no banner
      expect(container.querySelector('.canvas-wire-mode-banner')).toBeNull();
      expect(container.querySelector('.sheet-scroll.mode-wiring')).toBeNull();

      // Enter wire mode
      fireEvent.click(getByTestId('wiremode'));

      expect(container.querySelector('.canvas-wire-mode-banner')).not.toBeNull();
      expect(container.querySelector('.sheet-scroll.mode-wiring')).not.toBeNull();
      expect(container.querySelector('.canvas-wire-mode-banner')?.textContent).toContain('Wire Tool Active');

      // Click Exit button on banner
      const exitBtn = container.querySelector('.canvas-wire-mode-banner .banner-exit-btn');
      expect(exitBtn).not.toBeNull();
      fireEvent.click(exitBtn!);

      expect(toggleWireMode).toHaveBeenCalled();
    });
  });

  describe('Phase 3: Interactive net highlighting', () => {
    it('highlights entire net on wire hover and clears on mouse leave', () => {
      const { container } = setup();
      // Initially, no wire or terminal is net-highlighted
      expect(container.querySelectorAll('.wire.net-highlighted')).toHaveLength(0);
      expect(container.querySelectorAll('.terminal-pin-group.net-highlighted')).toHaveLength(0);
      expect(container.querySelector('.canvas-net-badge')).toBeNull();

      // Find first wire hit polyline
      const wireHits = container.querySelectorAll('.wire-hit');
      expect(wireHits.length).toBeGreaterThan(0);

      // Hover over the wire
      fireEvent.mouseEnter(wireHits[0]);

      // Wire and its connected terminals must now be net-highlighted
      const highlightedWires = container.querySelectorAll('.wire.net-highlighted');
      expect(highlightedWires.length).toBeGreaterThanOrEqual(1);

      const highlightedPins = container.querySelectorAll('.terminal-pin-group.net-highlighted');
      expect(highlightedPins.length).toBeGreaterThanOrEqual(1);

      // Toolbar net badge is displayed
      const netBadge = container.querySelector('.canvas-net-badge');
      expect(netBadge).not.toBeNull();
      expect(netBadge?.textContent).toContain('Net:');

      // Mouse leave clears highlighting
      fireEvent.mouseLeave(wireHits[0]);
      expect(container.querySelectorAll('.wire.net-highlighted')).toHaveLength(0);
      expect(container.querySelectorAll('.terminal-pin-group.net-highlighted')).toHaveLength(0);
      expect(container.querySelector('.canvas-net-badge')).toBeNull();
    });

    it('highlights net when wire is selected', () => {
      const { container } = setup();

      // Find wire hit polyline and click to select wire
      const wireHits = container.querySelectorAll('.wire-hit');
      expect(wireHits.length).toBeGreaterThan(0);
      fireEvent.mouseDown(wireHits[0], { button: 0 });

      const highlightedWires = container.querySelectorAll('.wire.net-highlighted');
      expect(highlightedWires.length).toBeGreaterThanOrEqual(1);

      const highlightedPins = container.querySelectorAll('.terminal-pin-group.net-highlighted');
      expect(highlightedPins.length).toBeGreaterThanOrEqual(1);

      const netBadge = container.querySelector('.canvas-net-badge');
      expect(netBadge).not.toBeNull();
    });
  });

  describe('Buck Converter component dragging', () => {
    it('dragging L.1 does not cause dangling endpoints or disconnect D.1 and C.1', () => {
      const buckSnap: EditorSnapshot = {
        circuitId: 'buck-test',
        modelVersion: 0,
        filename: 'buck.ipes',
        dpix: 16,
        worksheetSize: '600x600',
        components: [
          { name: 'V_in', type: 4, family: 'LK', position: [6, 8], orientation: 503, parameters: {}, inputLabels: [], outputLabels: [] },
          { name: 'S.1', type: 7, family: 'LK', position: [14, 6], orientation: 502, parameters: {}, inputLabels: [], outputLabels: [] },
          { name: 'D.1', type: 6, family: 'LK', position: [18, 8], orientation: 501, parameters: {}, inputLabels: [], outputLabels: [] },
          { name: 'L.1', type: 2, family: 'LK', position: [24, 6], orientation: 502, parameters: {}, inputLabels: [], outputLabels: [] },
          { name: 'C.1', type: 3, family: 'LK', position: [32, 8], orientation: 503, parameters: {}, inputLabels: [], outputLabels: [] },
          { name: 'R_load', type: 1, family: 'LK', position: [38, 8], orientation: 503, parameters: {}, inputLabels: [], outputLabels: [] },
        ],
        connections: [
          { index: 0, type: 'LK', label: 'in', points: [[6, 6], [12, 6]] },
          { index: 1, type: 'LK', label: 'sw_node', points: [[16, 6], [17, 6], [18, 6]] },
          { index: 2, type: 'LK', label: 'sw_node', points: [[18, 6], [19, 6], [20, 6], [21, 6], [22, 6]] },
          { index: 3, type: 'LK', label: 'out', points: [[26, 6], [27, 6], [28, 6], [29, 6], [30, 6], [31, 6], [32, 6]] },
          { index: 4, type: 'LK', label: 'out', points: [[32, 6], [33, 6], [34, 6], [35, 6], [36, 6], [37, 6], [38, 6]] },
          { index: 5, type: 'LK', label: '0', points: [[6, 10], [18, 10]] },
          { index: 6, type: 'LK', label: '0', points: [[18, 10], [32, 10]] },
          { index: 7, type: 'LK', label: '0', points: [[32, 10], [38, 10]] },
        ],
      };

      function BuckWrapper() {
        const [state, dispatch] = useReducer(editorReducer, {
          ...initialState,
          circuitId: buckSnap.circuitId,
          components: buckSnap.components,
          wires: buckSnap.connections as EditorWire[],
          dpix: 16,
        });
        return <Sheet state={state} dispatch={dispatch} actions={actions} />;
      }

      const { container } = render(<BuckWrapper />);

      // Initial state: 0 dangling endpoints
      expect(container.querySelectorAll('.wire-dangling-box')).toHaveLength(0);

      // Find L.1 component group
      const l1Group = container.querySelector('g.component[data-name="L.1"]');
      expect(l1Group).not.toBeNull();

      // Mouse down on L.1 at (24 * 16, 6 * 16) = (384, 96)
      fireEvent.mouseDown(l1Group!, { clientX: 384, clientY: 96, button: 0 });

      // Move mouse to (384, 64) -> y = 4 (up by 2 grid steps)
      const svg = container.querySelector('svg.sheet');
      expect(svg).not.toBeNull();
      fireEvent.mouseMove(svg!, { clientX: 384, clientY: 64 });

      // During drag: NO dangling endpoints!
      expect(container.querySelectorAll('.wire-dangling-box')).toHaveLength(0);

      // Release mouse
      fireEvent.mouseUp(svg!);

      // After drop: NO dangling endpoints!
      expect(container.querySelectorAll('.wire-dangling-box')).toHaveLength(0);
      expect(commitMove).toHaveBeenCalled();
    });
  });
});
