import { describe, expect, it } from 'vitest';
import { editorReducer, initialState } from '../src/model/store';
import type { EditorSnapshot } from '../src/model/types';
import { simplifyCorners, denseCellsOf } from '../src/canvas/WireRouter';

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

  it('rotates component and updates attached wires in store', () => {
    const initialWithWire = {
      ...snapshot,
      wires: [{ index: 0, type: 'LK', points: [[10, 8], [5, 8]], label: '' }],
    };
    let state = editorReducer(initialState, { type: 'SNAPSHOT', snapshot: initialWithWire });
    state = editorReducer(state, {
      type: 'ROTATE_COMPONENT',
      name: 'R1',
      orientation: 502,
      wires: [{ index: 0, type: 'LK', points: [[8, 10], [5, 8]], label: '' }],
      version: 5,
    });
    expect(state.components.find((c) => c.name === 'R1')?.orientation).toBe(502);
    expect(state.wires[0].points[0]).toEqual([8, 10]);
    expect(state.modelVersion).toBe(5);
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

describe('store: wire points follow dragged and nudged components', () => {
  // R1 (type 1, orientation 502 at (10,10)) has terminals at (8,10)/(12,10);
  // wire w2 starts exactly on the input terminal, w1 does not touch it
  const snap: EditorSnapshot = {
    ...snapshot,
    connections: [
      { index: 0, type: 'LK', label: 'w1', points: [[10, 10], [20, 10], [20, 20]] },
      { index: 1, type: 'LK', label: 'w2', points: [[8, 10], [20, 10]] },
    ],
  };
  const loaded = editorReducer(initialState, { type: 'SNAPSHOT', snapshot: snap });

  it('DRAG_MOVE shifts wire points captured on the dragged terminals', () => {
    let state = editorReducer(loaded, { type: 'DRAG_START', names: ['R1'], x: 12, y: 12 });
    state = editorReducer(state, { type: 'DRAG_MOVE', x: 15, y: 10 }); // dx=3, dy=-2
    const corners = simplifyCorners(state.wires[1].points);
    expect(corners[0]).toEqual([11, 8]);
    expect(corners[corners.length - 1]).toEqual([20, 10]);
    // W1 does not touch R1 terminals and is untouched
    expect(state.wires[0].points).toEqual([[10, 10], [20, 10], [20, 20]]);
  });

  it('CANCEL restores wire points to their pre-drag coordinates', () => {
    let state = editorReducer(loaded, { type: 'DRAG_START', names: ['R1'], x: 12, y: 12 });
    state = editorReducer(state, { type: 'DRAG_MOVE', x: 15, y: 10 });
    state = editorReducer(state, { type: 'CANCEL' });
    expect(state.wires[1].points).toEqual([[8, 10], [20, 10]]);
    expect(state.components[0].position).toEqual([10, 10]);
  });

  it('SELECTION_NUDGE moves wire endpoints on nudged terminals only', () => {
    let state = editorReducer(loaded, { type: 'SELECT', name: 'R1', additive: false });
    state = editorReducer(state, { type: 'SELECTION_NUDGE', dx: 2, dy: 0 });
    expect(state.components[0].position).toEqual([12, 10]);
    // The slid straight route would run through R1's body (now at (12,10)),
    // so the deconfliction pass routes w2 around it on a free lane.
    expect(simplifyCorners(state.wires[1].points)).toEqual([[10, 10], [10, 9], [20, 9], [20, 10]]);
    expect(state.wires[0].points).toEqual([[10, 10], [20, 10], [20, 20]]);
  });

  it('dragging both components shifts wire points on either end once', () => {
    // C1 (orientation 503 at (30,10)) has terminals at (30,8)/(30,12)
    const both: EditorSnapshot = {
      ...snap,
      connections: [
        { index: 0, type: 'LK', label: 'w3', points: [[8, 10], [30, 8]] },
      ],
    };
    let state = editorReducer(initialState, { type: 'SNAPSHOT', snapshot: both });
    state = editorReducer(state, { type: 'DRAG_START', names: ['R1', 'C1'], x: 0, y: 0 });
    state = editorReducer(state, { type: 'DRAG_MOVE', x: 4, y: 3 }); // dx=4, dy=3
    expect(state.components[0].position).toEqual([14, 13]);
    expect(state.components[1].position).toEqual([34, 13]);
    expect(state.wires[0].points).toEqual([[12, 13], [34, 11]]);
  });
});

describe('store: DRAG_MOVE deconflicts moved wires against untouched wires', () => {  // The reported bug: dragging R1 down onto the bottom rail row slid its
  // wire along the rail, so distinct nets were drawn as one line.
  const railSnap: EditorSnapshot = {
    ...snapshot,
    components: [{ ...snapshot.components[0], position: [16, 10] }], // R1, terminals (14,10)/(18,10)
    connections: [
      { index: 0, type: 'LK', label: 'rail', points: [[4, 16], [28, 16]] },
      { index: 1, type: 'LK', label: 'w1', points: [[14, 10], [20, 10]] },
    ],
  };

  it('routes the moved wire onto a free lane instead of along the rail', () => {
    let state = editorReducer(initialState, { type: 'SNAPSHOT', snapshot: railSnap });
    state = editorReducer(state, { type: 'DRAG_START', names: ['R1'], x: 16, y: 10 });
    state = editorReducer(state, { type: 'DRAG_MOVE', x: 16, y: 16 }); // dy=+6 onto the rail row
    expect(state.components[0].position).toEqual([16, 16]);
    expect(simplifyCorners(state.wires[1].points)).toEqual([[14, 16], [14, 10], [20, 10]]);
    // untouched rail stays put
    expect(state.wires[0].points).toEqual([[4, 16], [28, 16]]);
    // the moved wire touches the rail at its pinned endpoint only
    const railCells = denseCellsOf(state.wires[0].points);
    const shared = [...denseCellsOf(state.wires[1].points)].filter((c) => railCells.has(c));
    expect(shared).toEqual(['14,16']);
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

  it('wire deleted removes by index and re-indexes remaining wires', () => {
    let state = editorReducer(loaded, {
      type: 'WIRE_CREATED',
      wire: { index: 1, type: 'LK', label: 'w2', points: [[1, 1], [2, 1]] },
      version: 5,
    });
    state = editorReducer(state, {
      type: 'WIRE_CREATED',
      wire: { index: 2, type: 'LK', label: 'w3', points: [[2, 1], [3, 1]] },
      version: 6,
    });
    expect(state.wires).toHaveLength(3);

    // Delete wire 0
    state = editorReducer(state, { type: 'WIRE_DELETED', index: 0, version: 7 });
    expect(state.wires).toHaveLength(2);
    // Remaining wires should now have indices 0 and 1
    expect(state.wires[0].index).toBe(0);
    expect(state.wires[0].label).toBe('w2');
    expect(state.wires[1].index).toBe(1);
    expect(state.wires[1].label).toBe('w3');
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

describe('store: sticky wire warning status', () => {
  const loaded = editorReducer(initialState, { type: 'SNAPSHOT', snapshot });

  it('preserves a wire warning across snapshots of the same circuit', () => {
    const warned = editorReducer(loaded, {
      type: 'STATUS',
      status: '⚠️ Wire end (5, 5) is not connected to a terminal or wire',
    } as never);
    const after = editorReducer(warned, {
      type: 'SNAPSHOT',
      snapshot: { ...snapshot, modelVersion: 7 },
    });
    expect(after.status.startsWith('⚠️')).toBe(true);
  });

  it('clears the warning when a different circuit is loaded', () => {
    const warned = editorReducer(loaded, {
      type: 'STATUS',
      status: '⚠️ Wire end (5, 5) is not connected',
    } as never);
    const after = editorReducer(warned, {
      type: 'SNAPSHOT',
      snapshot: { ...snapshot, circuitId: 'c2', modelVersion: 7 },
    });
    expect(after.status.startsWith('⚠️')).toBe(false);
  });
});

describe('store: dragging a multi-channel scope does not hang (crash regression)', () => {
  // The user repro: open the Multi-Scope RLC example ("two scope example")
  // and try to move SCOPE.2 — the editor froze on the first mouse move.
  // A 2+ input scope has terminals DIAGONAL from its center; the blocked-cell
  // spoke walker looped forever on them once drag started running it.
  const scopeSnap: EditorSnapshot = {
    ...snapshot,
    components: [
      {
        type: 1003,
        name: 'SCOPE.2',
        family: 'CONTROL',
        position: [30, 20],
        orientation: 503,
        parameters: {},
        inputLabels: ['v_R1', 'v_L1'],
        outputLabels: [],
      },
    ],
    connections: [
      { index: 0, type: 'CONTROL', label: 'sig', points: [[12, 10], [28, 10], [28, 19]] },
    ],
  };

  it('DRAG_MOVE of a wired scope completes and the wire follows', () => {
    let state = editorReducer(initialState, { type: 'SNAPSHOT', snapshot: scopeSnap });
    state = editorReducer(state, { type: 'DRAG_START', names: ['SCOPE.2'], x: 30, y: 20 });
    state = editorReducer(state, { type: 'DRAG_MOVE', x: 33, y: 20 });
    expect(state.components[0].position).toEqual([33, 20]);
    const corners = simplifyCorners(state.wires[0].points);
    expect(corners[0]).toEqual([12, 10]);
    expect(corners[corners.length - 1]).toEqual([31, 19]);
    // strictly orthogonal result
    for (let i = 0; i < corners.length - 1; i++) {
      expect(corners[i][0] === corners[i + 1][0] || corners[i][1] === corners[i + 1][1]).toBe(true);
    }
  });
});
