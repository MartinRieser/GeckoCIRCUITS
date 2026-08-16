import { describe, expect, it } from 'vitest';
import { editorReducer, initialState } from '../src/model/store';
import type { EditorSnapshot } from '../src/model/types';

const snapshot: EditorSnapshot = {
  circuitId: 'c1',
  modelVersion: 0,
  filename: 'test.ipes',
  dpix: 16,
  worksheetSize: '600x400',
  components: [
    {
      type: 1,
      name: 'R1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: { param0: 100 },
      inputLabels: ['a'],
      outputLabels: ['b'],
    },
    {
      type: 3,
      name: 'C1',
      family: 'LK',
      position: [30, 10],
      orientation: 503,
      parameters: { param0: 1e-6 },
      inputLabels: ['b'],
      outputLabels: [],
    },
  ],
  connections: [
    { index: 0, type: 'LK', label: 'w1', points: [[10, 10], [20, 10], [20, 20]] },
  ],
};

describe('store: snapshot loading', () => {
  it('loads components, wires, dpix and worksheet', () => {
    const state = editorReducer(initialState, { type: 'SNAPSHOT', snapshot });
    expect(state.circuitId).toBe('c1');
    expect(state.components).toHaveLength(2);
    expect(state.wires).toHaveLength(1);
    expect(state.dpix).toBe(16);
    expect(state.sheetWidth).toBe(600);
    expect(state.sheetHeight).toBe(400);
  });
});

describe('store: ghost placement mode', () => {
  const armed = editorReducer(initialState, { type: 'SNAPSHOT', snapshot });

  it('arms with default orientation NORTH_SOUTH', () => {
    const state = editorReducer(armed, { type: 'ARM', componentType: 2, family: 'LK' });
    expect(state.mode).toBe('placing');
    expect(state.ghost?.orientation).toBe(503);
  });

  it('moves and rotates the ghost', () => {
    let state = editorReducer(armed, { type: 'ARM', componentType: 2, family: 'LK' });
    state = editorReducer(state, { type: 'GHOST_MOVE', x: 42, y: 17 });
    state = editorReducer(state, { type: 'GHOST_ROTATE' });
    expect(state.ghost).toMatchObject({ x: 42, y: 17, orientation: 504 });
  });

  it('cancel returns to idle', () => {
    let state = editorReducer(armed, { type: 'ARM', componentType: 2, family: 'LK' });
    state = editorReducer(state, { type: 'CANCEL' });
    expect(state.mode).toBe('idle');
    expect(state.ghost).toBeNull();
  });
});

describe('store: component upsert/delete', () => {
  const loaded = editorReducer(initialState, { type: 'SNAPSHOT', snapshot });

  it('appends new components and bumps version', () => {
    const state = editorReducer(loaded, {
      type: 'COMPONENT_UPSERT',
      component: {
        type: 2,
        name: 'L1',
        family: 'LK',
        position: [50, 50],
        orientation: 503,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      },
      version: 1,
    });
    expect(state.components.map((c) => c.name)).toContain('L1');
    expect(state.modelVersion).toBe(1);
  });

  it('updates existing components in place', () => {
    const state = editorReducer(loaded, {
      type: 'COMPONENT_UPSERT',
      component: { ...snapshot.components[0], position: [11, 11] },
      version: 2,
    });
    expect(state.components[0].position).toEqual([11, 11]);
    expect(state.components).toHaveLength(2);
  });

  it('delete removes the component and prunes selection', () => {
    let state = editorReducer(loaded, { type: 'SELECT', name: 'C1', additive: false });
    state = editorReducer(state, { type: 'COMPONENT_DELETED', name: 'C1', version: 3 });
    expect(state.components.map((c) => c.name)).toEqual(['R1']);
    expect(state.selection).toEqual([]);
  });
});

describe('store: selection and rubber band', () => {
  const loaded = editorReducer(initialState, { type: 'SNAPSHOT', snapshot });

  it('shift-click toggles additive selection', () => {
    let state = editorReducer(loaded, { type: 'SELECT', name: 'R1', additive: false });
    state = editorReducer(state, { type: 'SELECT', name: 'C1', additive: true });
    expect(state.selection).toEqual(['R1', 'C1']);
    state = editorReducer(state, { type: 'SELECT', name: 'R1', additive: true });
    expect(state.selection).toEqual(['C1']);
  });

  it('rubber band selects components inside the rectangle', () => {
    let state = editorReducer(loaded, { type: 'RUBBER_START', x: 5, y: 5 });
    state = editorReducer(state, { type: 'RUBBER_MOVE', x: 35, y: 15 });
    state = editorReducer(state, { type: 'RUBBER_END' });
    expect(state.selection.sort()).toEqual(['C1', 'R1']);
    expect(state.mode).toBe('idle');
  });

  it('rubber band excludes components outside', () => {
    let state = editorReducer(loaded, { type: 'RUBBER_START', x: 25, y: 5 });
    state = editorReducer(state, { type: 'RUBBER_MOVE', x: 35, y: 15 });
    state = editorReducer(state, { type: 'RUBBER_END' });
    expect(state.selection).toEqual(['C1']);
  });
});

describe('store: dragging', () => {
  const loaded = editorReducer(initialState, { type: 'SNAPSHOT', snapshot });

  it('moves selected components by the drag delta', () => {
    let state = editorReducer(loaded, { type: 'SELECT', name: 'R1', additive: false });
    state = editorReducer(state, { type: 'DRAG_START', names: ['R1'], x: 12, y: 12 });
    state = editorReducer(state, { type: 'DRAG_MOVE', x: 15, y: 10 });
    expect(state.components[0].position).toEqual([13, 8]);
    expect(state.components[1].position).toEqual([30, 10]);
  });

  it('drag without DRAG_START is a no-op', () => {
    const state = editorReducer(loaded, { type: 'DRAG_MOVE', x: 15, y: 10 });
    expect(state.components[0].position).toEqual([10, 10]);
  });

  it('CANCEL (Esc) restores the components to their drag origins, classic deselectViaESCAPE', () => {
    let state = editorReducer(loaded, { type: 'DRAG_START', names: ['R1'], x: 12, y: 12 });
    state = editorReducer(state, { type: 'DRAG_MOVE', x: 15, y: 10 });
    expect(state.components[0].position).toEqual([13, 8]);
    state = editorReducer(state, { type: 'CANCEL' });
    expect(state.components[0].position).toEqual([10, 10]);
    expect(state.mode).toBe('idle');
    expect(state.drag).toBeNull();
  });

  it('DRAG_END keeps the moved positions and returns to idle', () => {
    let state = editorReducer(loaded, { type: 'DRAG_START', names: ['R1'], x: 12, y: 12 });
    state = editorReducer(state, { type: 'DRAG_MOVE', x: 15, y: 10 });
    state = editorReducer(state, { type: 'DRAG_END' });
    expect(state.components[0].position).toEqual([13, 8]);
    expect(state.mode).toBe('idle');
    expect(state.drag).toBeNull();
  });
});

describe('store: wiring', () => {
  const loaded = editorReducer(initialState, { type: 'SNAPSHOT', snapshot });

  it('wire mode start and cursor tracking', () => {
    let state = editorReducer(loaded, { type: 'WIRE_START', x: 12, y: 10 });
    expect(state.mode).toBe('wiring');
    state = editorReducer(state, { type: 'WIRE_CURSOR', x: 20, y: 30 });
    expect(state.wireDraft?.cursor).toEqual({ x: 20, y: 30 });
  });

  it('picks the routing axis once when leaving the start point and keeps it (classic)', () => {
    let state = editorReducer(loaded, { type: 'WIRE_START', x: 10, y: 10 });
    // small horizontal move first: |dx| >= |dy| -> horizontal preferred
    state = editorReducer(state, { type: 'WIRE_CURSOR', x: 11, y: 10 });
    expect(state.wireDraft?.preferHorizontal).toBe(true);
    // now the dominant axis flips, but the preference must stay sticky
    state = editorReducer(state, { type: 'WIRE_CURSOR', x: 12, y: 40 });
    expect(state.wireDraft?.preferHorizontal).toBe(true);
    // returning to the start point resets the preference
    state = editorReducer(state, { type: 'WIRE_CURSOR', x: 10, y: 10 });
    expect(state.wireDraft?.preferHorizontal).toBeNull();
    state = editorReducer(state, { type: 'WIRE_CURSOR', x: 10, y: 12 });
    expect(state.wireDraft?.preferHorizontal).toBe(false);
  });

  it('WIRE_DRAFT_END keeps the wire pen armed for the next wire', () => {
    let state = editorReducer(loaded, { type: 'TOGGLE_WIRE_MODE' });
    state = editorReducer(state, { type: 'WIRE_START', x: 1, y: 1 });
    state = editorReducer(state, { type: 'WIRE_CURSOR', x: 5, y: 5 });
    state = editorReducer(state, { type: 'WIRE_DRAFT_END' });
    expect(state.mode).toBe('wiring');
    expect(state.wireDraft).toBeNull();
  });

  it('CANCEL (Esc) aborts the draft but stays in wire mode, like the classic wire pen', () => {
    let state = editorReducer(loaded, { type: 'TOGGLE_WIRE_MODE' });
    state = editorReducer(state, { type: 'WIRE_START', x: 1, y: 1 });
    state = editorReducer(state, { type: 'CANCEL' });
    expect(state.mode).toBe('wiring');
    expect(state.wireDraft).toBeNull();
    // a second Esc leaves wire mode entirely
    state = editorReducer(state, { type: 'CANCEL' });
    expect(state.mode).toBe('idle');
  });

  it('wire created appends to list', () => {
    const state = editorReducer(loaded, {
      type: 'WIRE_CREATED',
      wire: { index: 1, type: 'LK', label: '', points: [[1, 1], [2, 2]] },
      version: 5,
    });
    expect(state.wires).toHaveLength(2);
    expect(state.modelVersion).toBe(5);
  });

  it('wire deleted removes by index', () => {
    const state = editorReducer(loaded, { type: 'WIRE_DELETED', index: 0, version: 6 });
    expect(state.wires).toHaveLength(0);
  });

  it('toggle wire mode twice cancels an active draft', () => {
    let state = editorReducer(loaded, { type: 'TOGGLE_WIRE_MODE' });
    expect(state.mode).toBe('wiring');
    state = editorReducer(state, { type: 'WIRE_START', x: 1, y: 1 });
    state = editorReducer(state, { type: 'TOGGLE_WIRE_MODE' });
    expect(state.mode).toBe('idle');
    expect(state.wireDraft).toBeNull();
  });
});

describe('store: P3 keyboard actions', () => {
  const loaded = editorReducer(initialState, { type: 'SNAPSHOT', snapshot });

  it('GHOST_NUDGE moves the ghost by dx, dy', () => {
    let state = editorReducer(loaded, { type: 'ARM', componentType: 1, family: 'LK' });
    expect(state.ghost).toMatchObject({ x: 10, y: 10 });
    state = editorReducer(state, { type: 'GHOST_NUDGE', dx: 5, dy: -2 });
    expect(state.ghost).toMatchObject({ x: 15, y: 8 });
  });

  it('GHOST_ROTATE with ccw rotates counter-clockwise', () => {
    let state = editorReducer(loaded, { type: 'ARM', componentType: 1, family: 'LK' });
    expect(state.ghost?.orientation).toBe(503);
    state = editorReducer(state, { type: 'GHOST_ROTATE', ccw: true });
    expect(state.ghost?.orientation).toBe(502);
  });

  it('SELECTION_NUDGE moves only selected components by dx, dy', () => {
    let state = editorReducer(loaded, { type: 'SELECT', name: 'R1', additive: false });
    state = editorReducer(state, { type: 'SELECTION_NUDGE', dx: 2, dy: 3 });
    const r1 = state.components.find((c) => c.name === 'R1');
    const c1 = state.components.find((c) => c.name === 'C1');
    expect(r1?.position).toEqual([12, 13]);
    expect(c1?.position).toEqual([30, 10]); // untouched
  });

  it('TERMINAL_FOCUS_CYCLE cycles forward and backward across component terminals', () => {
    let state = editorReducer(loaded, { type: 'TERMINAL_FOCUS_CYCLE' });
    expect(state.focusedTerminal).not.toBeNull();
    const firstTerm = state.focusedTerminal;
    expect(firstTerm?.componentName).toBe('R1');

    // Cycle next
    state = editorReducer(state, { type: 'TERMINAL_FOCUS_CYCLE' });
    expect(state.focusedTerminal?.terminalIndex).not.toBe(firstTerm?.terminalIndex);

    // Cycle reverse
    state = editorReducer(state, { type: 'TERMINAL_FOCUS_CYCLE', reverse: true });
    expect(state.focusedTerminal?.terminalIndex).toBe(firstTerm?.terminalIndex);
  });

  it('WIRE_CURSOR_NUDGE steers the wire draft cursor', () => {
    let state = editorReducer(loaded, { type: 'TOGGLE_WIRE_MODE' });
    state = editorReducer(state, { type: 'WIRE_START', x: 10, y: 10 });
    state = editorReducer(state, { type: 'WIRE_CURSOR_NUDGE', dx: 4, dy: 0 });
    expect(state.wireDraft?.cursor).toEqual({ x: 14, y: 10 });
    expect(state.wireDraft?.preferHorizontal).toBe(true);
  });
});
