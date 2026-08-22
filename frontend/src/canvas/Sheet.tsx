/**
 * The schematic sheet: SVG rendering of grid, components, wires, ghost preview,
 * wire draft and rubber band, with pan/zoom, terminal snap indicators, wire junction
 * connection dots, context menu, and floating canvas controls.
 */
import { useRef, useState, useMemo, useEffect } from 'react';
import type { Dispatch, MouseEvent as ReactMouseEvent, WheelEvent as ReactWheelEvent, DragEvent as ReactDragEvent } from 'react';
import type { EditorState, Action } from '../model/store';
import { terminalPositions, terminalNear } from '../model/geometry';
import { routeL, densePoints } from './WireRouter';
import { ComponentSymbol } from './symbols';
import type { Point } from '../model/types';
import { ContextMenu } from './ContextMenu';
import type { ContextMenuTarget } from './ContextMenu';

export interface SheetActions {
  placeGhost(
    x: number,
    y: number,
    orientation?: number,
    typeOverride?: number,
    familyOverride?: string,
  ): void;
  finishWire(points: number[][]): void;
  labelWire?(index: number, label: string): void;
  commitMove(moves: { name: string; x: number; y: number }[]): void;
  rotateComponent?: (name: string) => void;
  deleteComponent?: (name: string) => void;
  deleteWire?: (index: number) => void;
  deleteSelection?: () => void;
  openProperties?: (name: string) => void;
  toggleWireMode?: () => void;
  openCommandPalette?: () => void;
}

interface SheetProps {
  state: EditorState;
  dispatch: Dispatch<Action>;
  actions: SheetActions;
}

export function Sheet({ state, dispatch, actions }: SheetProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);

  const dpix = state.dpix;
  const rawWidth = state.sheetWidth * dpix;
  const rawHeight = state.sheetHeight * dpix;

  // Viewport zoom & pan
  const [zoom, setZoom] = useState(1.0);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isPanning, setIsPanning] = useState(false);
  const [panStart, setPanStart] = useState({ x: 0, y: 0 });
  const [isSpacePressed, setIsSpacePressed] = useState(false);
  const [showGrid, setShowGrid] = useState(true);

  // Near-terminal hover snap halo
  const [hoveredTerminal, setHoveredTerminal] = useState<Point | null>(null);

  // Live cursor grid coordinate
  const [cursorCoord, setCursorCoord] = useState<Point | null>(null);

  // Context menu state
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    target: ContextMenuTarget;
  } | null>(null);

  // Track spacebar for pan
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (
        e.code === 'Space' &&
        !(e.target instanceof HTMLInputElement) &&
        !(e.target instanceof HTMLTextAreaElement)
      ) {
        setIsSpacePressed(true);
      }
    };
    const handleKeyUp = (e: KeyboardEvent) => {
      if (e.code === 'Space') {
        setIsSpacePressed(false);
        setIsPanning(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, []);

  const toGrid = (e: ReactMouseEvent): Point => {
    const rect = svgRef.current!.getBoundingClientRect();
    const clientX = (e.clientX - rect.left) / zoom;
    const clientY = (e.clientY - rect.top) / zoom;
    return {
      x: Math.round(clientX / dpix),
      y: Math.round(clientY / dpix),
    };
  };

  const snappedToTerminal = (point: Point): Point => {
    const near = terminalNear(state.components, point);
    return near ? near.point : point;
  };

  // Find wire junction connection dots (nodes where 3 or more wire endpoints meet)
  const junctionDots = useMemo(() => {
    const pointCounts = new Map<string, { pt: Point; count: number }>();
    for (const wire of state.wires) {
      for (const p of wire.points) {
        const key = `${p[0]},${p[1]}`;
        const existing = pointCounts.get(key);
        if (existing) {
          existing.count += 1;
        } else {
          pointCounts.set(key, { pt: { x: p[0], y: p[1] }, count: 1 });
        }
      }
    }
    // Also consider points where a wire touches a component terminal
    const dots: Point[] = [];
    for (const entry of pointCounts.values()) {
      if (entry.count >= 3) {
        dots.push(entry.pt);
      }
    }
    return dots;
  }, [state.wires]);

  const handleWheel = (e: ReactWheelEvent) => {
    if (e.ctrlKey || e.metaKey || isSpacePressed) {
      e.preventDefault();
      const zoomFactor = e.deltaY < 0 ? 1.15 : 0.87;
      setZoom((prev) => Math.max(0.3, Math.min(3.0, prev * zoomFactor)));
    }
  };

  /**
   * Classic drop (mouseReleaseSelectedGroup): persist the dragged positions.
   */
  const commitDrag = () => {
    const drag = state.drag;
    if (!drag) return;
    const moves = Object.entries(drag.origins)
      .map(([name]) => {
        const comp = state.components.find((c) => c.name === name)!;
        return { name, x: comp.position[0], y: comp.position[1] };
      })
      .filter((m) => m.x !== drag.origins[m.name].x || m.y !== drag.origins[m.name].y);
    dispatch({ type: 'DRAG_END' });
    if (moves.length) {
      actions.commitMove(moves);
    }
  };

  const handleMouseDown = (e: ReactMouseEvent) => {
    // Close context menu if open
    if (contextMenu) setContextMenu(null);

    // Middle-click pan OR Spacebar + Left-click pan
    if (e.button === 1 || (e.button === 0 && isSpacePressed)) {
      e.preventDefault();
      setIsPanning(true);
      setPanStart({ x: e.clientX - pan.x, y: e.clientY - pan.y });
      return;
    }

    if (e.button !== 0) return;
    const p = toGrid(e);

    switch (state.mode) {
      case 'placing':
        if (state.ghost) {
          actions.placeGhost(p.x, p.y, state.ghost.orientation);
        }
        return;
      case 'wiring':
        if (!state.wireDraft) {
          const snapped = snappedToTerminal(p);
          const near = terminalNear(state.components, p);
          let family = 'LK';
          if (near) {
            const comp = state.components.find((c) => c.name === near.component);
            if (comp) {
              family = comp.family;
            }
          }
          dispatch({ type: 'WIRE_START', x: snapped.x, y: snapped.y, family });
        } else {
          const end = snappedToTerminal(p);
          const route = routeL(state.wireDraft.start, end, state.wireDraft.preferHorizontal);
          dispatch({ type: 'WIRE_DRAFT_END' });
          actions.finishWire(densePoints(route).map((pt) => [pt.x, pt.y]));
        }
        break;
      case 'dragging':
        // classic behavior: a press while a grabbed group follows the cursor drops it
        commitDrag();
        dispatch({ type: 'CLEAR_SELECTION' });
        return;
      default:
        dispatch({ type: 'RUBBER_START', x: p.x, y: p.y });
        if (!e.shiftKey) {
          dispatch({ type: 'CLEAR_SELECTION' });
        }
        break;
    }
  };

  const handleMouseMove = (e: ReactMouseEvent) => {
    if (isPanning) {
      setPan({
        x: e.clientX - panStart.x,
        y: e.clientY - panStart.y,
      });
      return;
    }

    const p = toGrid(e);
    setCursorCoord(p);

    // Update snap indicator
    const near = terminalNear(state.components, p);
    setHoveredTerminal(near ? near.point : null);

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

  const handleMouseUp = () => {
    if (isPanning) {
      setIsPanning(false);
      return;
    }

    if (state.mode === 'placing') {
      // release after a press that started outside the sheet (palette drag)
      if (state.ghost) {
        actions.placeGhost(state.ghost.x, state.ghost.y, state.ghost.orientation);
      }
      return;
    }

    if (state.mode === 'rubber') {
      dispatch({ type: 'RUBBER_END' });
    } else if (state.mode === 'dragging' && state.drag) {
      // classic semantics: a click that never moved leaves the group grabbed
      // (it then follows the cursor); a completed drag gesture commits here
      const moved = Object.entries(state.drag.origins).some(([name, origin]) => {
        const comp = state.components.find((c) => c.name === name);
        return comp && (comp.position[0] !== origin.x || comp.position[1] !== origin.y);
      });
      if (moved) {
        commitDrag();
      }
    }
  };

  const handleContextMenu = (e: ReactMouseEvent) => {
    e.preventDefault();
    if (state.mode === 'placing') {
      dispatch({ type: 'GHOST_ROTATE' });
      return;
    }

    const p = toGrid(e);
    setContextMenu({
      x: e.clientX,
      y: e.clientY,
      target: {
        type: 'canvas',
        gridX: p.x,
        gridY: p.y,
      },
    });
  };

  const handleDragOver = (e: ReactDragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
  };

  const handleDrop = (e: ReactDragEvent) => {
    e.preventDefault();
    try {
      const data = JSON.parse(e.dataTransfer.getData('text/plain'));
      if (data && data.type !== undefined) {
        const p = toGrid(e as unknown as ReactMouseEvent);
        actions.placeGhost(p.x, p.y, 503, data.type, data.family || 'LK');
      }
    } catch {
      // ignore
    }
  };

  const handleZoomIn = () => setZoom((prev) => Math.min(3.0, prev * 1.2));
  const handleZoomOut = () => setZoom((prev) => Math.max(0.3, prev / 1.2));
  const handleZoomReset = () => {
    setZoom(1.0);
    setPan({ x: 0, y: 0 });
  };
  const handleZoomFit = () => {
    if (!containerRef.current) return;
    const container = containerRef.current.getBoundingClientRect();
    const fitZoom = Math.min(
      (container.width - 40) / rawWidth,
      (container.height - 40) / rawHeight,
      1.2,
    );
    setZoom(Math.max(0.4, fitZoom));
    setPan({ x: 20, y: 20 });
  };

  const interactiveLayer = state.mode === 'idle';

  const wireDraftPoints = state.wireDraft
    ? routeL(state.wireDraft.start, state.wireDraft.cursor, state.wireDraft.preferHorizontal)
        .map((p) => `${p.x * dpix},${p.y * dpix}`)
        .join(' ')
    : null;

  return (
    <div
      className={`sheet-scroll ${isSpacePressed ? 'space-grab' : ''} ${isPanning ? 'panning' : ''}`}
      ref={containerRef}
      onWheel={handleWheel}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      onMouseLeave={() => setCursorCoord(null)}
    >
      {/* Floating Canvas View Controls */}
      <div className="canvas-view-controls">
        <button
          type="button"
          className="canvas-ctrl-btn"
          onClick={handleZoomIn}
          title="Zoom In (Ctrl + Scroll Up)"
        >
          +
        </button>
        <span className="canvas-zoom-text">{Math.round(zoom * 100)}%</span>
        <button
          type="button"
          className="canvas-ctrl-btn"
          onClick={handleZoomOut}
          title="Zoom Out (Ctrl + Scroll Down)"
        >
          −
        </button>
        <button
          type="button"
          className="canvas-ctrl-btn"
          onClick={handleZoomReset}
          title="Reset Zoom (100%)"
        >
          1:1
        </button>
        <button
          type="button"
          className="canvas-ctrl-btn"
          onClick={handleZoomFit}
          title="Fit to Screen"
        >
          Fit
        </button>
        <div className="canvas-ctrl-sep" />
        <button
          type="button"
          className={`canvas-ctrl-btn ${state.mode === 'wiring' ? 'active' : ''}`}
          onClick={actions.toggleWireMode}
          title="Wire Tool (W)"
        >
          W
        </button>
        <button
          type="button"
          className={`canvas-ctrl-btn ${showGrid ? 'active' : ''}`}
          onClick={() => setShowGrid(!showGrid)}
          title="Toggle Grid"
        >
          #
        </button>
        {cursorCoord && (
          <>
            <div className="canvas-ctrl-sep" />
            <span className="canvas-coord-badge" title="Cursor Grid Coordinate">
              X:{cursorCoord.x} Y:{cursorCoord.y}
            </span>
          </>
        )}
      </div>

      <div
        className="canvas-viewport"
        style={{
          transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
          transformOrigin: '0 0',
        }}
      >
        <svg
          ref={svgRef}
          width={rawWidth}
          height={rawHeight}
          className="sheet"
          onMouseMove={handleMouseMove}
          onMouseDown={handleMouseDown}
          onMouseUp={handleMouseUp}
          onContextMenu={handleContextMenu}
        >
          <defs>
            <pattern
              id="grid-dots"
              width={dpix}
              height={dpix}
              patternUnits="userSpaceOnUse"
            >
              <circle cx={1} cy={1} r={1} />
            </pattern>
          </defs>

          {/* Grid Background */}
          <rect
            width={rawWidth}
            height={rawHeight}
            fill={showGrid ? 'url(#grid-dots)' : '#1e2227'}
          />

          {/* Wires */}
          <g className="wires" pointerEvents={interactiveLayer ? 'auto' : 'none'}>
            {state.wires.map((wire) => (
              <g key={wire.index}>
                <polyline
                  points={wire.points.map((p) => `${p[0] * dpix},${p[1] * dpix}`).join(' ')}
                  className={`wire wire-${wire.type || 'LK'}${wire.index === state.selectedWire ? ' selected' : ''}`}
                />
                <polyline
                  points={wire.points.map((p) => `${p[0] * dpix},${p[1] * dpix}`).join(' ')}
                  className="wire-hit"
                  onMouseDown={(e) => {
                    if (e.button === 0) {
                      e.stopPropagation();
                      dispatch({ type: 'SELECT_WIRE', index: wire.index });
                    }
                  }}
                  onContextMenu={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    const p = toGrid(e);
                    setContextMenu({
                      x: e.clientX,
                      y: e.clientY,
                      target: {
                        type: 'wire',
                        wireIndex: wire.index,
                        gridX: p.x,
                        gridY: p.y,
                      },
                    });
                  }}
                />
              </g>
            ))}
          </g>

          {/* Wire Junction Connection Dots */}
          <g className="junction-dots" pointerEvents="none">
            {junctionDots.map((pt, i) => (
              <circle
                key={i}
                cx={pt.x * dpix}
                cy={pt.y * dpix}
                r={3.6}
                className="junction-dot"
              />
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
                  className={`component family-${component.family || 'LK'}${selected ? ' selected' : ''}`}
                  onMouseDown={(e) => {
                    if (e.button !== 0) return;
                    e.stopPropagation();
                    // note: while placing/dragging the components layer has
                    // pointer-events none, so this only fires in idle mode
                    const grid = toGrid(e);
                    dispatch({ type: 'SELECT', name: component.name, additive: e.shiftKey });
                    dispatch({ type: 'PANEL_FOR', name: component.name });
                    const names = state.selection.includes(component.name)
                      ? state.selection
                      : e.shiftKey
                        ? [...state.selection, component.name]
                        : [component.name];
                    dispatch({ type: 'DRAG_START', names, x: grid.x, y: grid.y });
                  }}
                  onDoubleClick={() => {
                    dispatch({ type: 'PANEL_FOR', name: component.name });
                  }}
                  onContextMenu={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    const p = toGrid(e);
                    dispatch({ type: 'SELECT', name: component.name, additive: false });
                    setContextMenu({
                      x: e.clientX,
                      y: e.clientY,
                      target: {
                        type: 'component',
                        name: component.name,
                        gridX: p.x,
                        gridY: p.y,
                      },
                    });
                  }}
                >
                  {/* Selection box halo */}
                  {selected && (
                    <rect
                      x={-2.4 * dpix}
                      y={-2.4 * dpix}
                      width={4.8 * dpix}
                      height={4.8 * dpix}
                      className="selection-box"
                      rx={3}
                    />
                  )}

                  <ComponentSymbol component={component} dpix={dpix} />

                  {/* Terminals */}
                  {[...terminals.input, ...terminals.output].map((t, i) => (
                    <circle
                      key={i}
                      cx={t.x * dpix - component.position[0] * dpix}
                      cy={t.y * dpix - component.position[1] * dpix}
                      r={3}
                      className="terminal"
                    />
                  ))}

                  {/* Component identifier label */}
                  <text x={0} y={2.4 * dpix} className="component-name">
                    {component.name}
                  </text>

                  {/* Terminal net labels */}
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

          {/* Terminal Snap Hover Halo */}
          {hoveredTerminal && (
            <circle
              cx={hoveredTerminal.x * dpix}
              cy={hoveredTerminal.y * dpix}
              r={7}
              className="terminal-snap-halo"
              pointerEvents="none"
            />
          )}

          {/* Focused Terminal indicator (Keyboard Tab navigation) */}
          {state.focusedTerminal && (
            <g
              className="focused-terminal-ring"
              transform={`translate(${state.focusedTerminal.x * dpix}, ${state.focusedTerminal.y * dpix})`}
              pointerEvents="none"
            >
              <circle r={8} fill="none" stroke="#38bdf8" strokeWidth={2} strokeDasharray="3 2" />
              <circle r={3} fill="#38bdf8" />
              <rect x={10} y={-8} width={24} height={15} rx={3} fill="rgba(15, 23, 42, 0.9)" stroke="#38bdf8" strokeWidth={1} />
              <text x={22} y={3} textAnchor="middle" fill="#38bdf8" fontSize={9} fontWeight="bold">
                {state.focusedTerminal.label}
              </text>
            </g>
          )}

          {/* Wire draft preview */}
          {wireDraftPoints && (
            <polyline
              points={wireDraftPoints}
              className={`wire-draft wire-${state.wireFamily || 'LK'}`}
              pointerEvents="none"
            />
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

          {/* Rubber band selection */}
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

      {/* Floating Context Menu */}
      {contextMenu && (
        <ContextMenu
          x={contextMenu.x}
          y={contextMenu.y}
          target={contextMenu.target}
          onClose={() => setContextMenu(null)}
          onRotate={actions.rotateComponent}
          onDeleteComponent={actions.deleteComponent}
          onDeleteWire={actions.deleteWire}
          onLabelWire={(index) => {
            const wire = state.wires.find((w) => w.index === index);
            const label = window.prompt('Net label (empty = none, GND = ground):', wire?.label ?? '');
            if (label !== null) {
              actions.labelWire?.(index, label.trim());
            }
          }}
          onOpenProperties={actions.openProperties}
          onToggleWireMode={actions.toggleWireMode}
          onOpenCommandPalette={actions.openCommandPalette}
          onZoomFit={handleZoomFit}
        />
      )}
    </div>
  );
}
