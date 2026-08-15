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
import type { EditorSnapshot } from '../src/model/types';

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
const deleteSelection = vi.fn();
const actions: SheetActions = { placeGhost, finishWire, commitMove, deleteSelection };

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
  deleteSelection.mockClear();
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
  it('two clicks create an L-routed wire', () => {
    const { svg, getByTestId } = setup();
    fireEvent.click(getByTestId('wiremode'));

    // start at grid (2,1) = client (32,16)
    fireEvent.mouseDown(svg, { clientX: 32, clientY: 16, button: 0 });
    // move to grid (6,5) = client (96,80)
    fireEvent.mouseMove(svg, { clientX: 96, clientY: 80, button: 0 });
    // terminal of R1 output is at grid (12,10) = client (192,160): snap there
    fireEvent.mouseDown(svg, { clientX: 190, clientY: 162, button: 0 });

    expect(finishWire).toHaveBeenCalledWith([
      [2, 1],
      [12, 1],
      [12, 10],
    ]);
  });

  it('wire start snaps to a nearby terminal', () => {
    const { svg, getByTestId } = setup();
    fireEvent.click(getByTestId('wiremode'));

    // R1 input terminal at grid (8,10) = client (128,160); click nearby
    fireEvent.mouseDown(svg, { clientX: 130, clientY: 158, button: 0 });
    fireEvent.mouseMove(svg, { clientX: 96, clientY: 80, button: 0 });
    fireEvent.mouseDown(svg, { clientX: 96, clientY: 80, button: 0 });

    expect(finishWire).toHaveBeenCalledWith([
      [8, 10],
      [8, 5],
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

    expect(commitMove).toHaveBeenCalledWith([{ name: 'R1', x: 13, y: 8 }]);
  });
});
