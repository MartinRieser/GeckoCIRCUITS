/**
 * Editor store: a useReducer state machine with the same interaction modes
 * as the Swing editor's MouseMoveMode (idle / placing / wiring / rubber /
 * dragging). Pure reducer — API calls happen in event handlers, their
 * results are fed back as actions (optimistic updates apply the payloads
 * locally; failures trigger a REFRESH with a fresh server snapshot).
 */
import type { EditorComponent, EditorSnapshot, EditorWire } from './types';

export type Mode = 'idle' | 'placing' | 'wiring' | 'rubber' | 'dragging';

export interface Ghost {
  type: number;
  family: string;
  name: string | null;
  x: number;
  y: number;
  orientation: number;
}

export interface WireDraft {
  start: { x: number; y: number };
  cursor: { x: number; y: number };
  /**
   * Sticky routing axis, port of Connection._movementWestEast: chosen once
   * when the draft leaves the start point, kept until the cursor returns to it.
   */
  preferHorizontal: boolean | null;
}

export interface RubberBand {
  x0: number;
  y0: number;
  x1: number;
  y1: number;
}

export interface DragState {
  /** name -> original position (before the drag), for undo and delta math */
  origins: Record<string, { x: number; y: number }>;
  startX: number;
  startY: number;
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
  status: string;
  busy: boolean;
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
  status: 'Open a .ipes file to begin',
  busy: false,
};

export type Action =
  | { type: 'SNAPSHOT'; snapshot: EditorSnapshot }
  | { type: 'STATUS'; status: string }
  | { type: 'BUSY'; busy: boolean }
  | { type: 'ARM'; componentType: number; family: string }
  | { type: 'GHOST_MOVE'; x: number; y: number }
  | { type: 'GHOST_ROTATE' }
  | { type: 'CANCEL' }
  | { type: 'COMPONENT_UPSERT'; component: EditorComponent; version: number }
  | { type: 'COMPONENT_DELETED'; name: string; version: number }
  | { type: 'WIRE_CREATED'; wire: EditorWire; version: number }
  | { type: 'WIRE_PATCHED'; index: number; points: number[][]; label: string; version: number }
  | { type: 'WIRE_DELETED'; index: number; version: number }
  | { type: 'SELECT'; name: string; additive: boolean }
  | { type: 'SELECT_WIRE'; index: number | null }
  | { type: 'CLEAR_SELECTION' }
  | { type: 'WIRE_START'; x: number; y: number }
  | { type: 'WIRE_CURSOR'; x: number; y: number }
  /** Aborts the current wire draft but keeps the wire pen armed (classic Esc). */
  | { type: 'WIRE_DRAFT_ABORT' }
  /** A wire was committed: clear the draft, keep the wire pen armed for the next one. */
  | { type: 'WIRE_DRAFT_END' }
  | { type: 'RUBBER_START'; x: number; y: number }
  | { type: 'RUBBER_MOVE'; x: number; y: number }
  | { type: 'RUBBER_END' }
  | { type: 'DRAG_START'; names: string[]; x: number; y: number }
  | { type: 'DRAG_MOVE'; x: number; y: number }
  /** Ends the drag keeping the current positions (commit); CANCEL restores them. */
  | { type: 'DRAG_END' }
  | { type: 'PANEL_FOR'; name: string | null }
  | { type: 'TOGGLE_WIRE_MODE' };

export function editorReducer(state: EditorState, action: Action): EditorState {
  switch (action.type) {
    case 'SNAPSHOT': {
      const [w, h] = parseWorksheet(action.snapshot.worksheetSize);
      return {
        ...state,
        circuitId: action.snapshot.circuitId,
        filename: action.snapshot.filename,
        modelVersion: action.snapshot.modelVersion,
        dpix: action.snapshot.dpix,
        sheetWidth: w,
        sheetHeight: h,
        components: action.snapshot.components,
        wires: action.snapshot.connections,
        selection: [],
        selectedWire: null,
        mode: 'idle',
        ghost: null,
        wireDraft: null,
        rubber: null,
        drag: null,
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
          name: null,
          x: state.ghost?.x ?? 0,
          y: state.ghost?.y ?? 0,
          orientation: 503,
        },
        selection: [],
        selectedWire: null,
        panelFor: null,
        status: 'Placing component — click to place, right-click or R to rotate, Esc to cancel',
      };
    case 'GHOST_MOVE':
      if (!state.ghost) return state;
      return { ...state, ghost: { ...state.ghost, x: action.x, y: action.y } };
    case 'GHOST_ROTATE': {
      if (!state.ghost) return state;
      const cycle = [503, 504, 501, 502];
      const next = cycle[(cycle.indexOf(state.ghost.orientation) + 1) % cycle.length];
      return { ...state, ghost: { ...state.ghost, orientation: next } };
    }
    case 'CANCEL': {
      // classic behaviors: Esc restores a moved component to its drag origin
      // (deselectViaESCAPE) and only kills the wire being drawn, not the wire pen
      if (state.mode === 'dragging' && state.drag) {
        const origins = state.drag.origins;
        return {
          ...state,
          components: state.components.map((c) =>
            origins[c.name] ? { ...c, position: [origins[c.name].x, origins[c.name].y] } : c,
          ),
          mode: 'idle',
          drag: null,
          status: '',
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
        status: '',
      };
    }
    case 'COMPONENT_DELETED':
      return {
        ...state,
        components: state.components.filter((c) => c.name !== action.name),
        selection: state.selection.filter((n) => n !== action.name),
        panelFor: state.panelFor === action.name ? null : state.panelFor,
        modelVersion: action.version,
        status: '',
      };
    case 'WIRE_CREATED':
      return {
        ...state,
        wires: [...state.wires, action.wire],
        modelVersion: action.version,
        status: '',
      };
    case 'WIRE_PATCHED':
      return {
        ...state,
        wires: state.wires.map((w) =>
          w.index === action.index ? { ...w, points: action.points, label: action.label } : w,
        ),
        modelVersion: action.version,
      };
    case 'WIRE_DELETED':
      return {
        ...state,
        wires: state.wires.filter((w) => w.index !== action.index),
        selectedWire: state.selectedWire === action.index ? null : state.selectedWire,
        modelVersion: action.version,
        status: '',
      };
    case 'SELECT': {
      const selection = action.additive
        ? state.selection.includes(action.name)
          ? state.selection.filter((n) => n !== action.name)
          : [...state.selection, action.name]
        : [action.name];
      return {
        ...state,
        selection,
        selectedWire: null,
        panelFor: action.additive ? state.panelFor : action.name,
      };
    }
    case 'SELECT_WIRE':
      return { ...state, selectedWire: action.index, selection: [] };
    case 'CLEAR_SELECTION':
      return { ...state, selection: [], selectedWire: null, panelFor: null };
    case 'WIRE_START':
      return {
        ...state,
        mode: 'wiring',
        wireDraft: { start: { x: action.x, y: action.y }, cursor: { x: action.x, y: action.y }, preferHorizontal: null },
        status: 'Wiring — click to set the end point, Esc to abort, W to leave wire mode',
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
    case 'WIRE_DRAFT_ABORT':
      if (!state.wireDraft) return state;
      return { ...state, wireDraft: null };
    case 'WIRE_DRAFT_END':
      if (!state.wireDraft) return state;
      return { ...state, wireDraft: null, status: 'Wire mode — click to draw the next wire' };
    case 'RUBBER_START':
      return {
        ...state,
        mode: 'rubber',
        rubber: { x0: action.x, y0: action.y, x1: action.x, y1: action.y },
      };
    case 'RUBBER_MOVE':
      if (!state.rubber) return state;
      return { ...state, rubber: { ...state.rubber, x1: action.x, y1: action.y } };
    case 'RUBBER_END': {
      if (!state.rubber) return state;
      const x0 = Math.min(state.rubber.x0, state.rubber.x1);
      const x1 = Math.max(state.rubber.x0, state.rubber.x1);
      const y0 = Math.min(state.rubber.y0, state.rubber.y1);
      const y1 = Math.max(state.rubber.y0, state.rubber.y1);
      const hit = state.components
        .filter((c) => inBox(c.position, x0, y0, x1, y1))
        .map((c) => c.name);
      return {
        ...state,
        mode: 'idle',
        rubber: null,
        selection: hit,
        selectedWire: null,
        panelFor: hit.length === 1 ? hit[0] : null,
        status: hit.length ? `${hit.length} selected` : '',
      };
    }
    case 'DRAG_START': {
      const origins: Record<string, { x: number; y: number }> = {};
      for (const name of action.names) {
        const comp = state.components.find((c) => c.name === name);
        if (comp) {
          origins[name] = { x: comp.position[0], y: comp.position[1] };
        }
      }
      return { ...state, mode: 'dragging', drag: { origins, startX: action.x, startY: action.y } };
    }
    case 'DRAG_MOVE': {
      if (!state.drag) return state;
      const dx = action.x - state.drag.startX;
      const dy = action.y - state.drag.startY;
      const components = state.components.map((c) => {
        const origin = state.drag!.origins[c.name];
        return origin
          ? { ...c, position: [origin.x + dx, origin.y + dy] }
          : c;
      });
      return { ...state, components };
    }
    case 'DRAG_END':
      if (!state.drag) return state;
      return { ...state, mode: 'idle', drag: null };
    case 'PANEL_FOR':
      return { ...state, panelFor: action.name };
    case 'TOGGLE_WIRE_MODE':
      if (state.wireDraft) {
        return { ...state, mode: 'idle', wireDraft: null, status: '' };
      }
      return {
        ...state,
        mode: 'wiring',
        ghost: null,
        wireDraft: null,
        status: 'Wire mode — click a terminal or grid point to start a wire',
      };
    default:
      return state;
  }
}

function inBox(position: number[], x0: number, y0: number, x1: number, y1: number): boolean {
  return position[0] >= x0 && position[0] <= x1 && position[1] >= y0 && position[1] <= y1;
}

function parseWorksheet(size: string): [number, number] {
  const match = /(\d+)\D+(\d+)/.exec(size);
  return match ? [Number(match[1]), Number(match[2])] : [600, 600];
}
