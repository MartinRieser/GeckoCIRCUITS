/**
 * The schematic sheet: SVG rendering of grid, components, wires, ghost
 * preview, wire draft and rubber band, plus all mouse interaction.
 * Mirrors the Swing editor's canvas behavior (SchematicEditor2).
 */
import { useRef } from 'react';
import type { Dispatch, MouseEvent } from 'react';
import type { EditorState, Action } from '../model/store';
import { terminalPositions, terminalNear } from '../model/geometry';
import { routeL } from './WireRouter';
import { ComponentSymbol } from './symbols';
import type { Point } from '../model/types';

export interface SheetActions {
  placeGhost(x: number, y: number, orientation: number): void;
  finishWire(points: number[][]): void;
  commitMove(moves: { name: string; x: number; y: number }[]): void;
  deleteSelection(): void;
}

interface SheetProps {
  state: EditorState;
  dispatch: Dispatch<Action>;
  actions: SheetActions;
}

export function Sheet({ state, dispatch, actions }: SheetProps) {
  const svgRef = useRef<SVGSVGElement>(null);
  const dpix = state.dpix;
  const width = state.sheetWidth * dpix;
  const height = state.sheetHeight * dpix;

  const toGrid = (e: MouseEvent): Point => {
    const rect = svgRef.current!.getBoundingClientRect();
    return {
      x: Math.round((e.clientX - rect.left) / dpix),
      y: Math.round((e.clientY - rect.top) / dpix),
    };
  };

  const snappedToTerminal = (point: Point): Point => {
    const near = terminalNear(state.components, point);
    return near ? near.point : point;
  };

  const handleMouseMove = (e: MouseEvent) => {
    const p = toGrid(e);
    switch (state.mode) {
      case 'placing':
        dispatch({ type: 'GHOST_MOVE', x: p.x, y: p.y });
        break;
      case 'wiring':
        if (state.wireDraft) {
          dispatch({ type: 'WIRE_CURSOR', ...snappedToTerminal(p) });
        }
        break;
      case 'rubber':
        dispatch({ type: 'RUBBER_MOVE', x: p.x, y: p.y });
        break;
      case 'dragging':
        dispatch({ type: 'DRAG_MOVE', x: p.x, y: p.y });
        break;
    }
  };

  const handleMouseDown = (e: MouseEvent) => {
    if (e.button !== 0) return;
    const p = toGrid(e);
    switch (state.mode) {
      case 'placing':
        if (state.ghost) {
          actions.placeGhost(state.ghost.x, state.ghost.y, state.ghost.orientation);
        }
        break;
      case 'wiring':
        if (!state.wireDraft) {
          dispatch({ type: 'WIRE_START', ...snappedToTerminal(p) });
        } else {
          const end = snappedToTerminal(p);
          const points = routeL(state.wireDraft.start, end);
          dispatch({ type: 'CANCEL' });
          actions.finishWire(points.map((pt) => [pt.x, pt.y]));
        }
        break;
      default:
        dispatch({ type: 'RUBBER_START', x: p.x, y: p.y });
        if (!e.shiftKey) {
          dispatch({ type: 'CLEAR_SELECTION' });
        }
        break;
    }
  };

  const handleMouseUp = () => {
    if (state.mode === 'rubber') {
      dispatch({ type: 'RUBBER_END' });
    } else if (state.mode === 'dragging' && state.drag) {
      const origins = state.drag.origins;
      const moves = Object.entries(origins)
        .map(([name]) => {
          const comp = state.components.find((c) => c.name === name)!;
          return { name, x: comp.position[0], y: comp.position[1] };
        })
        .filter((m) => m.x !== origins[m.name].x || m.y !== origins[m.name].y);
      dispatch({ type: 'CANCEL' });
      if (moves.length) {
        actions.commitMove(moves);
      }
    }
  };

  const handleContextMenu = (e: MouseEvent) => {
    e.preventDefault();
    if (state.mode === 'placing') {
      dispatch({ type: 'GHOST_ROTATE' });
    }
  };

  const interactiveLayer = state.mode === 'idle';

  const wireDraftPoints = state.wireDraft
    ? routeL(state.wireDraft.start, state.wireDraft.cursor)
        .map((p) => `${p.x * dpix},${p.y * dpix}`)
        .join(' ')
    : null;

  return (
    <div className="sheet-scroll">
      <svg
        ref={svgRef}
        width={width}
        height={height}
        className="sheet"
        onMouseMove={handleMouseMove}
        onMouseDown={handleMouseDown}
        onMouseUp={handleMouseUp}
        onContextMenu={handleContextMenu}
      >
        <defs>
          <pattern id="grid-dots" width={dpix} height={dpix} patternUnits="userSpaceOnUse">
            <circle cx={1} cy={1} r={1} />
          </pattern>
        </defs>
        <rect width={width} height={height} fill="url(#grid-dots)" />

        {/* Wires */}
        <g className="wires" pointerEvents={interactiveLayer ? 'auto' : 'none'}>
          {state.wires.map((wire) => (
            <g key={wire.index}>
              <polyline
                points={wire.points.map((p) => `${p[0] * dpix},${p[1] * dpix}`).join(' ')}
                className={wire.index === state.selectedWire ? 'wire selected' : 'wire'}
              />
              <polyline
                points={wire.points.map((p) => `${p[0] * dpix},${p[1] * dpix}`).join(' ')}
                className="wire-hit"
                onMouseDown={(e) => {
                  if (e.button === 0) dispatch({ type: 'SELECT_WIRE', index: wire.index });
                }}
              />
            </g>
          ))}
        </g>

        {/* Components */}
        <g className="components" pointerEvents={interactiveLayer ? 'auto' : 'none'}>
          {state.components.map((component) => {
            const selected = state.selection.includes(component.name);
            const terminals = terminalPositions(component);
            return (
              <g
                key={component.name}
                transform={`translate(${component.position[0] * dpix}, ${component.position[1] * dpix})`}
                className={selected ? 'component selected' : 'component'}
                onMouseDown={(e) => {
                  if (e.button !== 0) return;
                  e.stopPropagation();
                  const grid = toGrid(e);
                  dispatch({ type: 'SELECT', name: component.name, additive: e.shiftKey });
                  // drag the (new) selection: the clicked component plus,
                  // when it was already selected, the whole selection
                  const names = state.selection.includes(component.name)
                    ? state.selection
                    : e.shiftKey
                      ? [...state.selection, component.name]
                      : [component.name];
                  dispatch({ type: 'DRAG_START', names, x: grid.x, y: grid.y });
                }}
                onDoubleClick={() => dispatch({ type: 'PANEL_FOR', name: component.name })}
              >
                {selected && (
                  <rect
                    x={-2.5 * dpix}
                    y={-2.5 * dpix}
                    width={5 * dpix}
                    height={5 * dpix}
                    className="selection-box"
                  />
                )}
                <ComponentSymbol component={component} dpix={dpix} />
                {[...terminals.input, ...terminals.output].map((t, i) => (
                  <circle key={i} cx={t.x * dpix - component.position[0] * dpix} cy={t.y * dpix - component.position[1] * dpix} r={3} className="terminal" />
                ))}
                <text x={0} y={2.4 * dpix} className="component-name">
                  {component.name}
                </text>
                {terminals.input
                  .map((t, i) => ({ t, label: component.inputLabels[i] }))
                  .filter(({ label }) => label)
                  .map(({ t, label }, i) => (
                    <text
                      key={'in' + i}
                      x={(t.x - component.position[0]) * dpix - 4}
                      y={(t.y - component.position[1]) * dpix - 6}
                      className="node-label"
                    >
                      {label}
                    </text>
                  ))}
                {terminals.output
                  .map((t, i) => ({ t, label: component.outputLabels[i] }))
                  .filter(({ label }) => label)
                  .map(({ t, label }, i) => (
                    <text
                      key={'out' + i}
                      x={(t.x - component.position[0]) * dpix + 4}
                      y={(t.y - component.position[1]) * dpix - 6}
                      className="node-label"
                    >
                      {label}
                    </text>
                  ))}
              </g>
            );
          })}
        </g>

        {/* Wire draft preview */}
        {wireDraftPoints && (
          <polyline points={wireDraftPoints} className="wire-draft" pointerEvents="none" />
        )}

        {/* Ghost preview while placing */}
        {state.ghost && state.mode === 'placing' && (
          <g
            transform={`translate(${state.ghost.x * dpix}, ${state.ghost.y * dpix})`}
            pointerEvents="none"
            className="ghost"
          >
            <ComponentSymbol
              component={{
                type: state.ghost.type,
                name: '',
                family: state.ghost.family,
                position: [0, 0],
                orientation: state.ghost.orientation,
                parameters: {},
                inputLabels: [],
                outputLabels: [],
              }}
              dpix={dpix}
            />
          </g>
        )}

        {/* Rubber band */}
        {state.rubber && (
          <rect
            x={Math.min(state.rubber.x0, state.rubber.x1) * dpix}
            y={Math.min(state.rubber.y0, state.rubber.y1) * dpix}
            width={Math.abs(state.rubber.x1 - state.rubber.x0) * dpix}
            height={Math.abs(state.rubber.y1 - state.rubber.y0) * dpix}
            className="rubber-band"
            pointerEvents="none"
          />
        )}
      </svg>
    </div>
  );
}
