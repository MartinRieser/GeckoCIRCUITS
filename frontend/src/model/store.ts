/**
 * Editor State Machine & useReducer store.
 *
 * Implements the core editor interaction states:
 * - 'idle': normal selection, rubber band, component dragging
 * - 'placing': ghost component follows cursor / keyboard arrows, waiting to be placed
 * - 'wiring': wire drafting from a start terminal to target point/terminal
 * - 'rubber': multi-component selection rectangle active
 * - 'dragging': moving selected components on grid
 */
import type { EditorComponent, EditorWire, Point, EditorSnapshot } from './types';
import { ORIENTATION_CYCLE, terminalPositions } from './geometry';

export type Mode = 'idle' | 'placing' | 'wiring' | 'rubber' | 'dragging';
export type EditorMode = Mode;

export interface Ghost {
  type: number;
  family: string;
  x: number;
  y: number;
  orientation: number;
}

export interface WireDraft {
  start: Point;
  cursor: Point;
  /** locked horizontal preference once draft moves away from start */
  preferHorizontal: boolean | null;
}

export interface RubberBand {
  x0: number;
  y0: number;
  x1: number;
  y1: number;
}

/**
 * A wire point that sits on a terminal of a component being dragged; the
 * point follows the drag and is restored from the original coordinates on
 * cancel (mirrors the server-side wire-follow in CircuitEditService).
 */
export interface ConnectedWirePoint {
  wireIndex: number;
  pointIndex: number;
  originalX: number;
  originalY: number;
}

export interface DragState {
  /** name -> original position (before the drag), for undo and delta math */
  origins: Record<string, { x: number; y: number }>;
  startX: number;
  startY: number;
  /** wire points captured on drag terminals at their pre-drag coordinates */
  connectedWirePoints: ConnectedWirePoint[];
}

export interface FocusedTerminal {
  componentName: string;
  terminalIndex: number;
  label: string;
  x: number;
  y: number;
}

export interface EditorState {
  circuitId: string | null;
  filename: string;
  modelVersion: number;
  dpix: number;
  sheetWidth: number;
  sheetHeight: number;
  components: EditorComponent[];
  wires: EditorWire[];
  mode: Mode;
  ghost: Ghost | null;
  wireDraft: WireDraft | null;
  rubber: RubberBand | null;
  drag: DragState | null;
  selection: string[];
  selectedWire: number | null;
  panelFor: string | null;
  focusedTerminal: FocusedTerminal | null;
  status: string;
  busy: boolean;
  wireFamily: string | null;
}

export const initialState: EditorState = {
  circuitId: null,
  filename: '',
  modelVersion: 0,
  dpix: 16,
  sheetWidth: 600,
  sheetHeight: 600,
  components: [],
  wires: [],
  mode: 'idle',
  ghost: null,
  wireDraft: null,
  rubber: null,
  drag: null,
  selection: [],
  selectedWire: null,
  panelFor: null,
  focusedTerminal: null,
  status: 'Open a .ipes file or example to begin',
  busy: false,
  wireFamily: null,
};

export type Action =
  | { type: 'SNAPSHOT'; snapshot: EditorSnapshot }
  | { type: 'STATUS'; status: string }
  | { type: 'BUSY'; busy: boolean }
  | { type: 'ARM'; componentType: number; family: string }
  | { type: 'GHOST_MOVE'; x: number; y: number }
  | { type: 'GHOST_NUDGE'; dx: number; dy: number }
  | { type: 'GHOST_ROTATE'; ccw?: boolean }
  | { type: 'CANCEL' }
  | { type: 'COMPONENT_UPSERT'; component: EditorComponent; version: number }
  | { type: 'COMPONENT_DELETED'; name: string; version: number }
  | { type: 'WIRE_CREATED'; wire: EditorWire; version: number }
  | { type: 'WIRE_PATCHED'; index: number; points: number[][]; label: string; version: number }
  | { type: 'WIRE_DELETED'; index: number; version: number }
  | { type: 'SELECT'; name: string; additive: boolean }
  | { type: 'SELECT_WIRE'; index: number | null }
  | { type: 'CLEAR_SELECTION' }
  | { type: 'SELECTION_NUDGE'; dx: number; dy: number }
  | { type: 'WIRE_START'; x: number; y: number; family?: string }
  | { type: 'WIRE_CURSOR'; x: number; y: number }
  | { type: 'WIRE_CURSOR_NUDGE'; dx: number; dy: number }
  | { type: 'TERMINAL_FOCUS_CYCLE'; reverse?: boolean }
  | { type: 'SET_FOCUSED_TERMINAL'; terminal: FocusedTerminal | null }
  | { type: 'WIRE_DRAFT_ABORT' }
  | { type: 'WIRE_DRAFT_END' }
  | { type: 'RUBBER_START'; x: number; y: number }
  | { type: 'RUBBER_MOVE'; x: number; y: number }
  | { type: 'RUBBER_END' }
  | { type: 'DRAG_START'; names: string[]; x: number; y: number }
  | { type: 'DRAG_MOVE'; x: number; y: number }
  | { type: 'DRAG_END' }
  | { type: 'PANEL_FOR'; name: string | null }
  | { type: 'TOGGLE_WIRE_MODE' };

export function editorReducer(state: EditorState, action: Action): EditorState {
  switch (action.type) {
    case 'SNAPSHOT': {
      const snap = action.snapshot as unknown as Record<string, unknown>;
      let sheetWidth = 600;
      let sheetHeight = 600;
      if (typeof snap.sheetWidth === 'number' && typeof snap.sheetHeight === 'number') {
        sheetWidth = snap.sheetWidth;
        sheetHeight = snap.sheetHeight;
      } else if (typeof snap.worksheetSize === 'string') {
        const parts = snap.worksheetSize.replace('_', 'x').split('x');
        sheetWidth = Number(parts[0]) || 600;
        sheetHeight = Number(parts[1]) || 600;
      }

      const wires = (Array.isArray(snap.wires) ? snap.wires : Array.isArray(snap.connections) ? snap.connections : []) as EditorWire[];
      const components = (Array.isArray(snap.components) ? snap.components : []) as EditorComponent[];

      return {
        ...state,
        circuitId: (snap.circuitId as string) || null,
        filename: (snap.filename as string) || '',
        modelVersion: typeof snap.modelVersion === 'number' ? snap.modelVersion : 0,
        dpix: typeof snap.dpix === 'number' ? snap.dpix : 16,
        sheetWidth,
        sheetHeight,
        components,
        wires,
        mode: 'idle',
        ghost: null,
        wireDraft: null,
        rubber: null,
        drag: null,
        selection: [],
        selectedWire: null,
        panelFor: null,
        focusedTerminal: null,
        status: `Loaded ${(snap.filename as string) || 'circuit'} (${components.length} components)`,
        busy: false,
      };
    }

    case 'STATUS':
      return { ...state, status: action.status };

    case 'BUSY':
      return { ...state, busy: action.busy };

    case 'ARM':
      return {
        ...state,
        mode: 'placing',
        ghost: {
          type: action.componentType,
          family: action.family,
          x: 10,
          y: 10,
          orientation: 503,
        },
        wireDraft: null,
        rubber: null,
        drag: null,
        status: 'Placing component — Click or press Enter to place, R to rotate, Esc to cancel',
      };

    case 'GHOST_MOVE':
      if (!state.ghost) return state;
      return {
        ...state,
        ghost: { ...state.ghost, x: action.x, y: action.y },
      };

    case 'GHOST_NUDGE': {
      if (!state.ghost) return state;
      const maxWidth = state.sheetWidth || 600;
      const maxHeight = state.sheetHeight || 600;
      const x = Math.max(2, Math.min(maxWidth - 2, state.ghost.x + action.dx));
      const y = Math.max(2, Math.min(maxHeight - 2, state.ghost.y + action.dy));
      return {
        ...state,
        ghost: { ...state.ghost, x, y },
        status: `Ghost at (${x}, ${y}) — Enter to place, R to rotate, Esc to cancel`,
      };
    }

    case 'GHOST_ROTATE': {
      if (!state.ghost) return state;
      const cycle = ORIENTATION_CYCLE;
      const curIdx = cycle.indexOf(state.ghost.orientation as (typeof cycle)[number]);
      const nextIdx = action.ccw
        ? (curIdx - 1 + cycle.length) % cycle.length
        : (curIdx + 1) % cycle.length;
      return { ...state, ghost: { ...state.ghost, orientation: cycle[nextIdx] } };
    }

    case 'CANCEL': {
      // aborting a drag restores components AND the wire points that
      // followed them to their pre-drag coordinates
      if (state.mode === 'dragging' && state.drag) {
        const origins = state.drag.origins;
        const connected = state.drag.connectedWirePoints;
        const wires = state.wires.map((wire, wireIndex) => {
          const points = wire.points.map((pt) => [...pt]);
          let changed = false;
          for (const cp of connected) {
            if (cp.wireIndex === wireIndex && points[cp.pointIndex]) {
              points[cp.pointIndex][0] = cp.originalX;
              points[cp.pointIndex][1] = cp.originalY;
              changed = true;
            }
          }
          return changed ? { ...wire, points } : wire;
        });

        return {
          ...state,
          components: state.components.map((c) =>
            origins[c.name] ? { ...c, position: [origins[c.name].x, origins[c.name].y] } : c,
          ),
          wires,
          mode: 'idle',
          drag: null,
          status: 'Move cancelled',
        };
      }
      if (state.mode === 'wiring' && state.wireDraft) {
        return { ...state, wireDraft: null, status: 'Wire mode — click a terminal or grid point to start a wire' };
      }
      return {
        ...state,
        mode: 'idle',
        ghost: null,
        wireDraft: null,
        rubber: null,
        drag: null,
        status: '',
      };
    }

    case 'COMPONENT_UPSERT': {
      const exists = state.components.some((c) => c.name === action.component.name);
      const components = exists
        ? state.components.map((c) => (c.name === action.component.name ? action.component : c))
        : [...state.components, action.component];
      return {
        ...state,
        components,
        modelVersion: action.version,
        status: `${action.component.name} updated`,
      };
    }

    case 'COMPONENT_DELETED': {
      return {
        ...state,
        components: state.components.filter((c) => c.name !== action.name),
        selection: state.selection.filter((n) => n !== action.name),
        panelFor: state.panelFor === action.name ? null : state.panelFor,
        modelVersion: action.version,
        status: `${action.name} deleted`,
      };
    }

    case 'WIRE_CREATED':
      return {
        ...state,
        wires: [...state.wires, action.wire],
        modelVersion: action.version,
        status: `Wire #${action.wire.index} added`,
      };

    case 'WIRE_PATCHED':
      return {
        ...state,
        wires: state.wires.map((w) =>
          w.index === action.index ? { ...w, points: action.points, label: action.label } : w,
        ),
        modelVersion: action.version,
        status: `Wire #${action.index} updated`,
      };

    case 'WIRE_DELETED':
      return {
        ...state,
        wires: state.wires.filter((w) => w.index !== action.index),
        selectedWire: state.selectedWire === action.index ? null : state.selectedWire,
        modelVersion: action.version,
        status: `Wire #${action.index} deleted`,
      };

    case 'SELECT': {
      const selection = action.additive
        ? state.selection.includes(action.name)
          ? state.selection.filter((n) => n !== action.name)
          : [...state.selection, action.name]
        : [action.name];
      return { ...state, selection, selectedWire: null };
    }

    case 'SELECT_WIRE':
      return { ...state, selectedWire: action.index, selection: [] };

    case 'CLEAR_SELECTION':
      return { ...state, selection: [], selectedWire: null };

    case 'SELECTION_NUDGE': {
      if (state.selection.length === 0) return state;
      const maxWidth = state.sheetWidth || 600;
      const maxHeight = state.sheetHeight || 600;
      const set = new Set(state.selection);

      // wire points on terminals of the nudged components (at their
      // pre-nudge positions) travel along with the selection
      const movedTerminalSet = new Set<string>();
      for (const comp of state.components) {
        if (set.has(comp.name)) {
          const terms = terminalPositions(comp);
          for (const t of [...terms.input, ...terms.output]) {
            movedTerminalSet.add(`${t.x},${t.y}`);
          }
        }
      }

      const components = state.components.map((c) => {
        if (!set.has(c.name)) return c;
        const x = Math.max(2, Math.min(maxWidth - 2, c.position[0] + action.dx));
        const y = Math.max(2, Math.min(maxHeight - 2, c.position[1] + action.dy));
        return { ...c, position: [x, y] };
      });

      const wires = state.wires.map((wire) => {
        const points = wire.points.map((pt) => {
          if (movedTerminalSet.has(`${pt[0]},${pt[1]}`)) {
            return [pt[0] + action.dx, pt[1] + action.dy];
          }
          return pt;
        });
        return { ...wire, points };
      });

      return { ...state, components, wires };
    }

    case 'WIRE_START':
      return {
        ...state,
        mode: 'wiring',
        wireFamily: action.family || 'LK',
        wireDraft: { start: { x: action.x, y: action.y }, cursor: { x: action.x, y: action.y }, preferHorizontal: null },
        status: 'Wiring — click or press Enter to set end point, Esc to abort, W to leave wire mode',
      };

    case 'WIRE_CURSOR': {
      if (!state.wireDraft) return state;
      const draft = state.wireDraft;
      let preferHorizontal = draft.preferHorizontal;
      if (action.x === draft.start.x && action.y === draft.start.y) {
        preferHorizontal = null;
      } else if (preferHorizontal === null) {
        preferHorizontal = Math.abs(action.x - draft.start.x) >= Math.abs(action.y - draft.start.y);
      }
      return {
        ...state,
        wireDraft: { ...draft, cursor: { x: action.x, y: action.y }, preferHorizontal },
      };
    }

    case 'WIRE_CURSOR_NUDGE': {
      if (!state.wireDraft) return state;
      const cur = state.wireDraft.cursor;
      const maxWidth = state.sheetWidth || 600;
      const maxHeight = state.sheetHeight || 600;
      const x = Math.max(0, Math.min(maxWidth, cur.x + action.dx));
      const y = Math.max(0, Math.min(maxHeight, cur.y + action.dy));
      const draft = state.wireDraft;
      let preferHorizontal = draft.preferHorizontal;
      if (x === draft.start.x && y === draft.start.y) {
        preferHorizontal = null;
      } else if (preferHorizontal === null) {
        preferHorizontal = Math.abs(x - draft.start.x) >= Math.abs(y - draft.start.y);
      }
      return {
        ...state,
        wireDraft: { ...draft, cursor: { x, y }, preferHorizontal },
        status: `Wire draft cursor at (${x}, ${y}) — Enter to commit, Esc to abort`,
      };
    }

    case 'TERMINAL_FOCUS_CYCLE': {
      if (state.components.length === 0) return state;
      const allTerminals: FocusedTerminal[] = [];
      for (const comp of state.components) {
        const terms = terminalPositions(comp);
        terms.input.forEach((p, idx) => {
          allTerminals.push({ componentName: comp.name, terminalIndex: idx, label: 'in', x: p.x, y: p.y });
        });
        terms.output.forEach((p, idx) => {
          allTerminals.push({ componentName: comp.name, terminalIndex: terms.input.length + idx, label: 'out', x: p.x, y: p.y });
        });
      }
      if (allTerminals.length === 0) return state;

      let nextIndex = 0;
      if (state.focusedTerminal) {
        const curIdx = allTerminals.findIndex(
          (t) =>
            t.componentName === state.focusedTerminal?.componentName &&
            t.terminalIndex === state.focusedTerminal?.terminalIndex,
        );
        if (curIdx >= 0) {
          nextIndex = action.reverse
            ? (curIdx - 1 + allTerminals.length) % allTerminals.length
            : (curIdx + 1) % allTerminals.length;
        }
      }

      const focused = allTerminals[nextIndex];
      const wireDraft =
        state.mode === 'wiring' && state.wireDraft
          ? { ...state.wireDraft, cursor: { x: focused.x, y: focused.y } }
          : state.wireDraft;

      return {
        ...state,
        focusedTerminal: focused,
        wireDraft,
        status: `Focused ${focused.componentName} pin ${focused.label} (${focused.x}, ${focused.y})`,
      };
    }

    case 'SET_FOCUSED_TERMINAL':
      return { ...state, focusedTerminal: action.terminal };

    case 'WIRE_DRAFT_ABORT':
      if (!state.wireDraft) return state;
      return { ...state, wireDraft: null, status: 'Wire draft aborted' };

    case 'WIRE_DRAFT_END':
      if (!state.wireDraft) return state;
      return { ...state, wireDraft: null, status: 'Wire mode — click or press Enter to draw the next wire' };

    case 'RUBBER_START':
      return {
        ...state,
        mode: 'rubber',
        rubber: { x0: action.x, y0: action.y, x1: action.x, y1: action.y },
      };

    case 'RUBBER_MOVE':
      if (!state.rubber) return state;
      return {
        ...state,
        rubber: { ...state.rubber, x1: action.x, y1: action.y },
      };

    case 'RUBBER_END': {
      if (!state.rubber) return state;
      const r = state.rubber;
      const xMin = Math.min(r.x0, r.x1);
      const xMax = Math.max(r.x0, r.x1);
      const yMin = Math.min(r.y0, r.y1);
      const yMax = Math.max(r.y0, r.y1);

      const selected = state.components
        .filter((c) => c.position[0] >= xMin && c.position[0] <= xMax && c.position[1] >= yMin && c.position[1] <= yMax)
        .map((c) => c.name);

      return {
        ...state,
        mode: 'idle',
        rubber: null,
        selection: selected,
        status: selected.length ? `${selected.length} components selected` : '',
      };
    }

    case 'DRAG_START': {
      // capture origins plus every wire point sitting on a terminal of the
      // dragged components, so DRAG_MOVE/CANCEL can shift or restore them
      const origins: Record<string, { x: number; y: number }> = {};
      const movedTerminalSet = new Set<string>();
      for (const name of action.names) {
        const comp = state.components.find((c) => c.name === name);
        if (comp) {
          origins[name] = { x: comp.position[0], y: comp.position[1] };
          const terms = terminalPositions(comp);
          for (const t of [...terms.input, ...terms.output]) {
            movedTerminalSet.add(`${t.x},${t.y}`);
          }
        }
      }

      const connectedWirePoints: ConnectedWirePoint[] = [];
      state.wires.forEach((wire, wireIndex) => {
        wire.points.forEach((pt, pointIndex) => {
          if (movedTerminalSet.has(`${pt[0]},${pt[1]}`)) {
            connectedWirePoints.push({
              wireIndex,
              pointIndex,
              originalX: pt[0],
              originalY: pt[1],
            });
          }
        });
      });

      return {
        ...state,
        mode: 'dragging',
        drag: { origins, startX: action.x, startY: action.y, connectedWirePoints },
      };
    }

    case 'DRAG_MOVE': {
      if (!state.drag) return state;
      const dx = action.x - state.drag.startX;
      const dy = action.y - state.drag.startY;
      const origins = state.drag.origins;
      const connected = state.drag.connectedWirePoints;

      const components = state.components.map((c) => {
        if (origins[c.name]) {
          return {
            ...c,
            position: [origins[c.name].x + dx, origins[c.name].y + dy],
          };
        }
        return c;
      });

      const wires = state.wires.map((wire, wireIndex) => {
        const points = wire.points.map((pt) => [...pt]);
        let changed = false;
        for (const cp of connected) {
          if (cp.wireIndex === wireIndex && points[cp.pointIndex]) {
            points[cp.pointIndex][0] = cp.originalX + dx;
            points[cp.pointIndex][1] = cp.originalY + dy;
            changed = true;
          }
        }
        return changed ? { ...wire, points } : wire;
      });

      return { ...state, components, wires };
    }

    case 'DRAG_END':
      if (!state.drag) return state;
      return { ...state, mode: 'idle', drag: null };

    case 'PANEL_FOR':
      return { ...state, panelFor: action.name };

    case 'TOGGLE_WIRE_MODE': {
      if (state.mode === 'wiring') {
        return { ...state, mode: 'idle', wireDraft: null, status: '' };
      }
      return {
        ...state,
        mode: 'wiring',
        wireDraft: null,
        ghost: null,
        selection: [],
        status: 'Wire mode active — Click or Tab/Enter on a terminal to start',
      };
    }

    default:
      return state;
  }
}
