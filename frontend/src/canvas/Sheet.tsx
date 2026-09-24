/**
 * The schematic sheet: SVG rendering of grid, components, wires, ghost preview,
 * wire draft and rubber band, with pan/zoom, terminal snap indicators, wire junction
 * connection dots, context menu, and floating canvas controls.
 */
import { useRef, useState, useMemo, useEffect } from 'react';
import type { Dispatch, MouseEvent as ReactMouseEvent, WheelEvent as ReactWheelEvent, DragEvent as ReactDragEvent } from 'react';
import type { EditorState, Action } from '../model/store';
import { terminalPositions, terminalNear, findPlacementConflict } from '../model/geometry';
import { routeAvoidingObstacles, routingBlockedCells, densePoints, denseCellsOf, orthogonalizePolyline, simplifyCorners, translateWireSegment } from './WireRouter';
import { isWireEndPointConnected, findWireGeometryWarnings } from '../model/validation';
import { ComponentSymbol } from './symbols';
import { isScopeComponent } from '../simulation/scopes';
import {
  isGateDriver,
  isSwitchComponent,
  isAmmeterComponent,
  isVoltmeterComponent,
  getCoupledComponentName,
} from '../model/componentSchema';
import type { Point } from '../model/types';
import { ContextMenu } from './ContextMenu';
import type { ContextMenuTarget } from './ContextMenu';
import { Orientation, CANVAS_METRICS } from '../model/constants';

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
  patchWirePoints?(index: number, points: number[][]): void;
  flipWire?(index: number): void;
  commitMove(
    moves: { name: string; x: number; y: number }[],
    wirePatches?: { index: number; points: number[][] }[],
    postStatus?: string,
  ): void;
  rotateComponent?: (name: string) => void;
  deleteComponent?: (name: string) => void;
  deleteWire?: (index: number) => void;
  deleteSelection?: () => void;
  openProperties?: (name: string) => void;
  openScopeTab?: (name: string) => void;
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

  // Hovered component for guideline emphasis
  const [hoveredComponentName, setHoveredComponentName] = useState<string | null>(null);

  // Net label visibility mode: 'smart' (virtual always show; wired on hover/selection), 'all' (always show all), or 'hover' (only on hover/selection)
  const [labelDisplayMode, setLabelDisplayMode] = useState<'smart' | 'all' | 'hover'>(() => {
    try {
      return (localStorage.getItem('gecko_label_mode') as 'smart' | 'all' | 'hover') || 'smart';
    } catch {
      return 'smart';
    }
  });

  // Active software coupling pairs (Gate Driver ➔ Switch, Ammeter ➔ Target Component)
  const couplingPairs = useMemo(() => {
    const pairs: Array<{
      sourceComp: (typeof state.components)[0];
      targetComp: (typeof state.components)[0];
      label: string;
      isHoveredOrSelected: boolean;
    }> = [];

    for (const comp of state.components) {
      if (isGateDriver(comp) || isAmmeterComponent(comp)) {
        const targetName = getCoupledComponentName(comp);
        if (targetName) {
          const target = state.components.find((c) => c.name === targetName);
          if (target) {
            const isHoveredOrSelected =
              state.selection.includes(comp.name) ||
              state.selection.includes(target.name) ||
              hoveredComponentName === comp.name ||
              hoveredComponentName === target.name;
            const label = isGateDriver(comp) ? 'GATE DRIVE ➔' : 'MEASURE I ➔';
            pairs.push({ sourceComp: comp, targetComp: target, label, isHoveredOrSelected });
          }
        }
      }
    }
    return pairs;
  }, [state.components, state.selection, hoveredComponentName]);

  // Pre-computed, deduplicated terminal net labels across all circuit components
  const terminalLabelItems = useMemo(() => {
    const items: Array<{
      key: string;
      label: string;
      gx: number;
      gy: number;
      componentName: string;
      dirX: number;
      dirY: number;
      isWired: boolean;
    }> = [];
    const seen = new Set<string>();

    for (const component of state.components) {
      const terminals = terminalPositions(component);
      const inLabels = component.inputLabels || [];
      const outLabels = component.outputLabels || [];

      terminals.input.forEach((t, i) => {
        const raw = inLabels[i]?.trim();
        if (!raw || raw === 'NIX_NIX_NIX') return;
        const pointKey = `${t.x},${t.y}:${raw}`;
        if (seen.has(pointKey)) return;
        seen.add(pointKey);
        const isWired = state.wires.some((w) =>
          w.points.some((p) => Math.hypot(p[0] - t.x, p[1] - t.y) < 0.25),
        );
        items.push({
          key: `in-${component.name}-${i}-${raw}`,
          label: raw,
          gx: t.x,
          gy: t.y,
          componentName: component.name,
          dirX: t.x - component.position[0],
          dirY: t.y - component.position[1],
          isWired,
        });
      });

      terminals.output.forEach((t, i) => {
        const raw = outLabels[i]?.trim();
        if (!raw || raw === 'NIX_NIX_NIX') return;
        const pointKey = `${t.x},${t.y}:${raw}`;
        if (seen.has(pointKey)) return;
        seen.add(pointKey);
        const isWired = state.wires.some((w) =>
          w.points.some((p) => Math.hypot(p[0] - t.x, p[1] - t.y) < 0.25),
        );
        items.push({
          key: `out-${component.name}-${i}-${raw}`,
          label: raw,
          gx: t.x,
          gy: t.y,
          componentName: component.name,
          dirX: t.x - component.position[0],
          dirY: t.y - component.position[1],
          isWired,
        });
      });
    }
    return items;
  }, [state.components, state.wires]);

  // Wire segment and endpoint drag state
  const [wireDrag, setWireDrag] = useState<{
    wireIndex: number;
    segmentIndex?: number;
    endpointIndex?: 0 | 1;
    originalCorners: number[][];
    startGrid: Point;
    currentDensePoints?: number[][];
  } | null>(null);

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
    const handleGlobalMouseUp = () => {
      setIsPanning(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    window.addEventListener('mouseup', handleGlobalMouseUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
      window.removeEventListener('mouseup', handleGlobalMouseUp);
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

  // Find wire junction connection dots:
  // - nodes where 3 or more wire points coincide (drawn junctions), and
  // - endpoint taps: a wire end lying on another wire's interior. Those are
  //   electrically junctions too, and after a move they are the only visible
  //   hint that two wires touch at all.
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
    const dots: Point[] = [];
    const seen = new Set<string>();
    const addDot = (pt: Point) => {
      const key = `${pt.x},${pt.y}`;
      if (!seen.has(key)) {
        seen.add(key);
        dots.push(pt);
      }
    };
    for (const entry of pointCounts.values()) {
      if (entry.count >= 3) {
        addDot(entry.pt);
      }
    }
    // Endpoint-on-interior taps (endpoint-on-endpoint is a plain butt joint)
    const interiors = state.wires.map((w) => {
      const cells = denseCellsOf(w.points);
      if (w.points.length >= 2) {
        for (const end of [w.points[0], w.points[w.points.length - 1]]) {
          cells.delete(`${end[0]},${end[1]}`);
        }
      }
      return cells;
    });
    state.wires.forEach((w, i) => {
      if (!w.points || w.points.length < 2) return;
      for (const end of [w.points[0], w.points[w.points.length - 1]]) {
        const key = `${end[0]},${end[1]}`;
        if (interiors.some((cells, j) => j !== i && cells.has(key))) {
          addDot({ x: end[0], y: end[1] });
        }
      }
    });
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
    // Same body-on-body guard as placement: dropping a moved component on top
    // of another one would silently short pins, so the move bounces back.
    const staticComponents = state.components.filter((c) => !drag.origins[c.name]);
    const conflicts = Object.keys(drag.origins).filter((name) => {
      const comp = state.components.find((c) => c.name === name);
      return comp ? findPlacementConflict(comp, staticComponents) !== null : false;
    });
    if (conflicts.length > 0) {
      dispatch({ type: 'CANCEL' }); // dragging mode: restores origins and original wires
      dispatch({
        type: 'STATUS',
        status: `⚠️ Move blocked — ${conflicts.join(', ')} would sit on top of another component`,
      });
      return;
    }
    const moves = Object.entries(drag.origins)
      .map(([name]) => {
        const comp = state.components.find((c) => c.name === name)!;
        return { name, x: comp.position[0], y: comp.position[1] };
      })
      .filter((m) => m.x !== drag.origins[m.name].x || m.y !== drag.origins[m.name].y);
    const wirePatches = (drag.draggedWires || [])
      .map((dw) => {
        const wire = state.wires.find((w) => w.index === dw.wireIndex);
        return wire ? { index: wire.index, points: wire.points } : null;
      })
      .filter((wp): wp is { index: number; points: number[][] } => wp !== null);

    // Residual geometry defects (e.g. a terminal the user dropped onto a wire)
    // become a sticky status warning instead of failing the move.
    const geometryWarnings = findWireGeometryWarnings(state.components, state.wires);
    const postStatus =
      geometryWarnings.length > 0
        ? `⚠️ ${geometryWarnings[0]}${geometryWarnings.length > 1 ? ` (+${geometryWarnings.length - 1} more)` : ''}`
        : undefined;

    dispatch({ type: 'DRAG_END' });
    if (moves.length) {
      actions.commitMove(moves, wirePatches, postStatus);
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
          const route = routeAvoidingObstacles(
            state.wireDraft.start, end,
            routingBlockedCells(state.components),
            state.wireDraft.preferHorizontal);
          const dense = densePoints(route).map((pt) => [pt.x, pt.y] as [number, number]);
          dispatch({ type: 'WIRE_DRAFT_END' });
          actions.finishWire(dense);
          const endPoint = { x: dense[dense.length - 1][0], y: dense[dense.length - 1][1] };
          if (!isWireEndPointConnected(endPoint, state.components, state.wires)) {
            dispatch({
              type: 'STATUS',
              status: `⚠️ Wire end (${endPoint.x}, ${endPoint.y}) is not connected to a terminal or wire — it will not conduct`,
            });
          }
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

    if (wireDrag) {
      const dx = p.x - wireDrag.startGrid.x;
      const dy = p.y - wireDrag.startGrid.y;
      let newCorners: number[][];
      if (wireDrag.segmentIndex !== undefined) {
        newCorners = translateWireSegment(wireDrag.originalCorners, wireDrag.segmentIndex, { dx, dy });
      } else if (wireDrag.endpointIndex !== undefined) {
        const ep = wireDrag.endpointIndex;
        const target = snappedToTerminal(p);
        newCorners = wireDrag.originalCorners.map((pt) => [...pt]);
        if (ep === 0) {
          newCorners[0] = [target.x, target.y];
          if (newCorners.length > 2) {
            if (newCorners[1][1] === wireDrag.originalCorners[0][1]) {
              newCorners[1][1] = target.y;
            } else {
              newCorners[1][0] = target.x;
            }
          }
        } else {
          const lastIdx = newCorners.length - 1;
          newCorners[lastIdx] = [target.x, target.y];
          if (newCorners.length > 2) {
            if (newCorners[lastIdx - 1][1] === wireDrag.originalCorners[lastIdx][1]) {
              newCorners[lastIdx - 1][1] = target.y;
            } else {
              newCorners[lastIdx - 1][0] = target.x;
            }
          }
        }
        newCorners = simplifyCorners(newCorners);
      } else {
        newCorners = wireDrag.originalCorners;
      }
      const dense = densePoints(newCorners.map(([x, y]) => ({ x, y }))).map((pt) => [pt.x, pt.y]);
      setWireDrag((prev) => (prev ? { ...prev, currentDensePoints: dense } : null));
      dispatch({ type: 'WIRE_POINTS_UPDATE', index: wireDrag.wireIndex, points: dense });
      return;
    }

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

    if (wireDrag) {
      if (wireDrag.currentDensePoints) {
        actions.patchWirePoints?.(wireDrag.wireIndex, wireDrag.currentDensePoints);
      }
      setWireDrag(null);
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
    } else if (state.mode === 'dragging') {
      // Dragging ends immediately when mouse button is released
      commitDrag();
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
        actions.placeGhost(p.x, p.y, Orientation.NORTH_SOUTH, data.type, data.family || 'LK');
      }
    } catch {
      // ignore
    }
  };

  const handleZoomIn = () => setZoom((prev) => Math.min(CANVAS_METRICS.MAX_ZOOM, prev * CANVAS_METRICS.ZOOM_STEP_FACTOR));
  const handleZoomOut = () => setZoom((prev) => Math.max(CANVAS_METRICS.MIN_ZOOM, prev / CANVAS_METRICS.ZOOM_STEP_FACTOR));
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

  const selectedWireObj = useMemo(() => {
    if (state.selectedWire === null) return null;
    return state.wires.find((w) => w.index === state.selectedWire) || null;
  }, [state.selectedWire, state.wires]);

  const pillCoord = useMemo(() => {
    if (!selectedWireObj || !selectedWireObj.points || selectedWireObj.points.length === 0) return null;
    const corners = simplifyCorners(selectedWireObj.points);
    if (corners.length === 0) return null;
    const midCorner = corners[Math.floor(corners.length / 2)];
    return { x: midCorner[0], y: midCorner[1] };
  }, [selectedWireObj]);

  const wireDraftPoints = state.wireDraft
    ? routeAvoidingObstacles(
          state.wireDraft.start, state.wireDraft.cursor,
          routingBlockedCells(state.components),
          state.wireDraft.preferHorizontal)
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
        <button
          type="button"
          className={`canvas-ctrl-btn ${labelDisplayMode === 'smart' ? 'active' : ''}`}
          onClick={() => {
            const next = labelDisplayMode === 'smart' ? 'all' : labelDisplayMode === 'all' ? 'hover' : 'smart';
            setLabelDisplayMode(next);
            try {
              localStorage.setItem('gecko_label_mode', next);
            } catch {
              // ignore
            }
          }}
          title={`Net Labels: ${
            labelDisplayMode === 'smart'
              ? 'Smart Mode (Virtual labels always visible; wired labels on hover/select)'
              : labelDisplayMode === 'all'
              ? 'All Labels Visible (click to show on hover only)'
              : 'On Hover / Selection Only (click for Smart mode)'
          }`}
          style={{ fontSize: '11px', minWidth: '32px' }}
        >
          {labelDisplayMode === 'smart' ? '🏷️' : labelDisplayMode === 'all' ? '🏷️⁺' : '🏷️⋯'}
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
            <marker
              id="coupling-arrow"
              viewBox="0 0 10 10"
              refX="8"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 1.5 L 9 5 L 0 8.5 z" fill="#38bdf8" />
            </marker>
          </defs>

          {/* Grid Background */}
          <rect
            width={rawWidth}
            height={rawHeight}
            fill={showGrid ? 'url(#grid-dots)' : '#1e2227'}
          />

          {/* Wires */}
          <g className="wires">
            {/* Casing pass: a sheet-colored underlay under every wire, so two
                wires that do overlap read as a stroke/casing/stroke sandwich
                instead of one line. Painted before all visible strokes. */}
            {state.wires.map((wire) => {
              const casingPts = orthogonalizePolyline(wire.points);
              const casingStr = casingPts.map((p) => `${p[0] * dpix},${p[1] * dpix}`).join(' ');
              return (
                <polyline
                  key={`casing-${wire.index}`}
                  points={casingStr}
                  className="wire-casing"
                />
              );
            })}
            {state.wires.map((wire) => {
              const orthoPts = orthogonalizePolyline(wire.points);
              const pointsStr = orthoPts.map((p) => `${p[0] * dpix},${p[1] * dpix}`).join(' ');
              const isSelected = wire.index === state.selectedWire;
              const corners = isSelected ? simplifyCorners(wire.points) : [];
              return (
                <g key={wire.index} className={`wire-group${isSelected ? ' selected' : ''}`}>
                  <polyline
                    points={pointsStr}
                    className={`wire wire-${wire.type || 'LK'}${isSelected ? ' selected' : ''}`}
                  />
                  <polyline
                    points={pointsStr}
                    className="wire-hit"
                    onMouseDown={(e) => {
                      if (e.button === 0) {
                        e.stopPropagation();
                        if (state.mode === 'wiring' && state.wireDraft) {
                          const end = toGrid(e);
                          const route = routeAvoidingObstacles(
                            state.wireDraft.start, end,
                            routingBlockedCells(state.components),
                            state.wireDraft.preferHorizontal);
                          dispatch({ type: 'WIRE_DRAFT_END' });
                          actions.finishWire(densePoints(route).map((pt) => [pt.x, pt.y]));
                        } else {
                          dispatch({ type: 'SELECT_WIRE', index: wire.index });
                        }
                      }
                    }}
                    onContextMenu={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      const p = toGrid(e);
                      dispatch({ type: 'SELECT_WIRE', index: wire.index });
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

                  {/* Handles and Segment Drag Hit Bars for Selected Wire */}
                  {isSelected && corners.length >= 2 && (
                    <g className="wire-handles">
                      {/* Segment hit bars for sliding orthogonal segments */}
                      {corners.slice(0, -1).map((p1, segIdx) => {
                        const p2 = corners[segIdx + 1];
                        const isH = p1[1] === p2[1];
                        return (
                          <line
                            key={`seg-${segIdx}`}
                            x1={p1[0] * dpix}
                            y1={p1[1] * dpix}
                            x2={p2[0] * dpix}
                            y2={p2[1] * dpix}
                            className="wire-segment-drag-bar"
                            stroke="transparent"
                            strokeWidth={14}
                            style={{ cursor: isH ? 'ns-resize' : 'ew-resize' }}
                            onMouseDown={(e) => {
                              if (e.button !== 0) return;
                              e.stopPropagation();
                              const grid = toGrid(e);
                              setWireDrag({
                                wireIndex: wire.index,
                                segmentIndex: segIdx,
                                originalCorners: corners,
                                startGrid: grid,
                              });
                            }}
                          />
                        );
                      })}

                      {/* Start Endpoint handle */}
                      <circle
                        cx={corners[0][0] * dpix}
                        cy={corners[0][1] * dpix}
                        r={5.5}
                        className="wire-handle wire-endpoint-handle"
                        style={{ cursor: 'move' }}
                        onMouseDown={(e) => {
                          if (e.button !== 0) return;
                          e.stopPropagation();
                          const grid = toGrid(e);
                          setWireDrag({
                            wireIndex: wire.index,
                            endpointIndex: 0,
                            originalCorners: corners,
                            startGrid: grid,
                          });
                        }}
                      />

                      {/* End Endpoint handle */}
                      <circle
                        cx={corners[corners.length - 1][0] * dpix}
                        cy={corners[corners.length - 1][1] * dpix}
                        r={5.5}
                        className="wire-handle wire-endpoint-handle"
                        style={{ cursor: 'move' }}
                        onMouseDown={(e) => {
                          if (e.button !== 0) return;
                          e.stopPropagation();
                          const grid = toGrid(e);
                          setWireDrag({
                            wireIndex: wire.index,
                            endpointIndex: 1,
                            originalCorners: corners,
                            startGrid: grid,
                          });
                        }}
                      />
                    </g>
                  )}
                </g>
              );
            })}
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

          {/* Active Software Coupling Guidelines (Gate Driver ➔ Switch, Ammeter ➔ Component) */}
          <g className="coupling-guides-layer" pointerEvents="none">
            {couplingPairs.map(({ sourceComp, targetComp, label, isHoveredOrSelected }, i) => {
              const x1 = sourceComp.position[0] * dpix;
              const y1 = sourceComp.position[1] * dpix;
              const x2 = targetComp.position[0] * dpix;
              const y2 = targetComp.position[1] * dpix;
              const dx = x2 - x1;
              const cx1 = x1 + dx * 0.4;
              const cy1 = y1;
              const cx2 = x1 + dx * 0.6;
              const cy2 = y2;
              const pathD = `M ${x1} ${y1} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${x2} ${y2}`;
              const badgeW = Math.max(label.length * 6.5 + 16, 80);

              return (
                <g key={`coupling-${i}`} className={`coupling-guide-group${isHoveredOrSelected ? ' active' : ''}`}>
                  {isHoveredOrSelected && (
                    <path
                      d={pathD}
                      fill="none"
                      stroke="rgba(56, 189, 248, 0.25)"
                      strokeWidth={8}
                      strokeLinecap="round"
                    />
                  )}
                  <path
                    d={pathD}
                    fill="none"
                    stroke={isHoveredOrSelected ? '#38bdf8' : 'rgba(56, 189, 248, 0.45)'}
                    strokeWidth={isHoveredOrSelected ? 2.5 : 1.5}
                    strokeDasharray={isHoveredOrSelected ? '6 4' : '4 4'}
                    className={isHoveredOrSelected ? 'coupling-guideline active' : 'coupling-guideline'}
                    markerEnd="url(#coupling-arrow)"
                  />
                  {isHoveredOrSelected && (
                    <g transform={`translate(${(x1 + x2) / 2}, ${(y1 + y2) / 2})`}>
                      <rect x={-badgeW / 2} y={-10} width={badgeW} height={20} rx={4} fill="#0f172a" stroke="#38bdf8" strokeWidth={1} />
                      <text x={0} y={3.5} textAnchor="middle" fill="#38bdf8" fontSize={9} fontWeight="bold">
                        {label}
                      </text>
                    </g>
                  )}
                </g>
              );
            })}
          </g>

          {/* Components */}
          <g className="components" pointerEvents={state.mode === 'placing' ? 'none' : 'auto'}>
            {state.components.map((component) => {
              const selected = state.selection.includes(component.name);
              const terminals = terminalPositions(component);
              return (
                <g
                  key={component.name}
                  transform={`translate(${component.position[0] * dpix}, ${component.position[1] * dpix})`}
                  className={`component family-${component.family || 'LK'}${selected ? ' selected' : ''}`}
                  onMouseEnter={() => setHoveredComponentName(component.name)}
                  onMouseLeave={() => setHoveredComponentName((curr) => (curr === component.name ? null : curr))}
                  onMouseDown={(e) => {
                    if (e.button !== 0) return;
                    if (state.mode !== 'idle') return;
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
                    if (isScopeComponent(component)) {
                      actions.openScopeTab?.(component.name);
                    } else {
                      dispatch({ type: 'PANEL_FOR', name: component.name });
                    }
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
                  {/* Bounding dimensions sized to the drawn symbol (not the
                      terminal spread), so densely placed blocks do not
                      visually overlap; multi-pin scopes grow to cover pins. */}
                  {(() => {
                    const maxPins = Math.max(terminals.input.length, terminals.output.length, 1);
                    const halfH = Math.max(1.3, maxPins - 1 + 0.8) * dpix;
                    const halfW = 1.3 * dpix;
                    return (
                      <>
                        {/* Full-area hit target so clicking anywhere on the component bounding area selects it */}
                        <rect
                          x={-halfW}
                          y={-halfH}
                          width={halfW * 2}
                          height={halfH * 2}
                          className="component-hit-area"
                          fill="transparent"
                          stroke="none"
                          pointerEvents="all"
                          style={{ cursor: 'pointer' }}
                        />

                        {/* Selection box halo */}
                        {selected && (
                          <rect
                            x={-halfW}
                            y={-halfH}
                            width={halfW * 2}
                            height={halfH * 2}
                            className="selection-box"
                            rx={4}
                            pointerEvents="none"
                          />
                        )}

                        <ComponentSymbol component={component} dpix={dpix} />

                        {/* Interactive Terminals with direct wire drafting */}
                        {[...terminals.input, ...terminals.output].map((t, i) => {
                          const cx = t.x * dpix - component.position[0] * dpix;
                          const cy = t.y * dpix - component.position[1] * dpix;
                          return (
                            <g key={i} className="terminal-pin-group">
                              <circle
                                cx={cx}
                                cy={cy}
                                r={2.5}
                                className="terminal"
                              />
                              <circle
                                cx={cx}
                                cy={cy}
                                r={8}
                                fill="none"
                                stroke="none"
                                className="terminal-hit"
                                style={{ cursor: 'crosshair', pointerEvents: 'all' }}
                                onMouseDown={(e) => {
                                  if (e.button !== 0) return;
                                  e.stopPropagation();
                                  if (state.mode === 'wiring' && state.wireDraft) {
                                    const end = { x: t.x, y: t.y };
                                    const route = routeAvoidingObstacles(
                                      state.wireDraft.start, end,
                                      routingBlockedCells(state.components),
                                      state.wireDraft.preferHorizontal);
                                    dispatch({ type: 'WIRE_DRAFT_END' });
                                    actions.finishWire(densePoints(route).map((pt) => [pt.x, pt.y]));
                                  } else {
                                    dispatch({
                                      type: 'WIRE_START',
                                      x: t.x,
                                      y: t.y,
                                      family: component.family,
                                      autoReturnToIdle: true,
                                    });
                                  }
                                }}
                              />
                            </g>
                          );
                        })}

                        {/* Component identifier label */}
                        {(() => {
                          const isCtrlProbe = isVoltmeterComponent(component) || isAmmeterComponent(component);
                          const isControlBlock = component.family === 'CONTROL';
                          const isHorizontalTwoPort =
                            (component.orientation === Orientation.WEST_EAST ||
                              component.orientation === Orientation.EAST_WEST) &&
                            !isCtrlProbe;

                          if (isCtrlProbe) {
                            // Probes: place identifier to the left of the symbol so output labels to the right never collide
                            return (
                              <text x={-halfW - 6} y={4} textAnchor="end" className="component-name">
                                {component.name}
                              </text>
                            );
                          }
                          if (isControlBlock || isHorizontalTwoPort) {
                            // Control blocks (SIGNAL.1, etc.) & Horizontal components (L.1, horizontal resistors): place name centered above
                            return (
                              <text x={0} y={-halfH - 5} textAnchor="middle" className="component-name">
                                {component.name}
                              </text>
                            );
                          }
                          // Vertical LK / default components: to the right
                          return (
                            <text x={halfW + 6} y={4} textAnchor="start" className="component-name">
                              {component.name}
                            </text>
                          );
                        })()}

                        {/* Coupling Badge Tag on Component */}
                        {(() => {
                          const coupled = getCoupledComponentName(component);
                          const isSw = isSwitchComponent(component);
                          const isGd = isGateDriver(component);
                          const isAm = isAmmeterComponent(component);
                          const isVm = isVoltmeterComponent(component);

                          if (!isSw && !isGd && !isAm && !isVm) return null;

                          let badgeText = '';
                          const clickTarget: string | null = coupled || null;

                          if (isSw && coupled) {
                            badgeText = `⮡ gate: ${coupled}`;
                          } else if (isAm && coupled) {
                            badgeText = `➔ i(${coupled})`;
                          } else if (isGd && coupled) {
                            badgeText = `➔ ${coupled}`;
                          } else if (isVm) {
                            if (coupled) {
                              badgeText = `➔ u(${coupled})`;
                            } else {
                              const nodeA =
                                (component.parameters?.nodeA as string) ||
                                (component.parameters?.positiveNode as string);
                              const nodeB =
                                (component.parameters?.nodeB as string) ||
                                (component.parameters?.negativeNode as string) ||
                                '0';
                              if (nodeA) {
                                badgeText = `➔ V(${nodeA}, ${nodeB})`;
                              }
                            }
                          }

                          if (!badgeText) return null;

                          const badgeW = Math.max(badgeText.length * 6.2 + 14, 52);

                          // Position:
                          // For switches: place on the left (gate pin) side so it never collides with bottom wire/diode!
                          // For gate drivers, ammeters & voltmeters: place below the block
                          const badgeTransform = isSw
                            ? `translate(${-halfW - badgeW / 2 - 4}, 0)`
                            : `translate(0, ${halfH + 12})`;

                          return (
                            <g
                              className="coupling-symbol-badge"
                              transform={badgeTransform}
                              onClick={(e) => {
                                e.stopPropagation();
                                if (clickTarget) {
                                  dispatch({ type: 'SELECT', name: clickTarget, additive: false });
                                  dispatch({ type: 'PANEL_FOR', name: clickTarget });
                                } else {
                                  dispatch({ type: 'SELECT', name: component.name, additive: false });
                                  dispatch({ type: 'PANEL_FOR', name: component.name });
                                }
                              }}
                              style={{ cursor: 'pointer' }}
                            >
                              <rect
                                x={-badgeW / 2}
                                y={-8}
                                width={badgeW}
                                height={16}
                                rx={8}
                                className="coupling-pill-bg"
                              />
                              <text
                                x={0}
                                y={3.5}
                                textAnchor="middle"
                                className="coupling-pill-text"
                              >
                                {badgeText}
                              </text>
                            </g>
                          );
                        })()}
                      </>
                    );
                  })()}
                </g>
              );
            })}
          </g>

          {/* Deduplicated Terminal Net Labels Layer */}
          <g className="terminal-net-labels-layer" pointerEvents="none">
            {terminalLabelItems.map((item) => {
              const isCompHoveredOrSelected =
                state.selection.includes(item.componentName) ||
                hoveredComponentName === item.componentName;
              const isVisible =
                labelDisplayMode === 'all' ||
                (labelDisplayMode === 'smart' && !item.isWired) ||
                isCompHoveredOrSelected;
              if (!isVisible) return null;

              const px = item.gx * dpix;
              const py = item.gy * dpix;
              let textX = px + 4;
              let textY = py - 4;
              let anchor: 'start' | 'end' | 'middle' = 'start';

              if (item.dirX > 0) {
                // Terminal on the right of component
                textX = px + 4;
                textY = py + 3;
                anchor = 'start';
              } else if (item.dirX < 0) {
                // Terminal on the left of component
                textX = px - 4;
                textY = py + 3;
                anchor = 'end';
              } else if (item.dirY < 0) {
                // Terminal on top of component
                textX = px;
                textY = py - 6;
                anchor = 'middle';
              } else if (item.dirY > 0) {
                // Terminal on bottom of component
                textX = px;
                textY = py + 12;
                anchor = 'middle';
              }

              const rectW = Math.max(item.label.length * 5.8 + 6, 14);
              const rectH = 13;
              const rectY = textY - 9.5;
              let rectX = textX - 3;
              if (anchor === 'end') {
                rectX = textX - rectW + 3;
              } else if (anchor === 'middle') {
                rectX = textX - rectW / 2;
              }

              return (
                <g key={item.key} className="terminal-net-label-pill">
                  <rect
                    x={rectX}
                    y={rectY}
                    width={rectW}
                    height={rectH}
                    rx={3}
                    className="node-label-pill-bg"
                  />
                  <text
                    x={textX}
                    y={textY}
                    textAnchor={anchor}
                    className="node-label"
                  >
                    {item.label}
                  </text>
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

        {/* Floating Wire Action Pill */}
        {selectedWireObj && pillCoord && (
          <div
            className="wire-action-pill"
            style={{
              left: `${pillCoord.x * dpix}px`,
              top: `${pillCoord.y * dpix - 36}px`,
            }}
            onClick={(e) => e.stopPropagation()}
            onMouseDown={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              className="wire-action-btn danger"
              title="Delete Wire (Delete / Backspace)"
              onClick={() => actions.deleteWire?.(selectedWireObj.index)}
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              </svg>
              <span>Delete</span>
            </button>
            <button
              type="button"
              className="wire-action-btn"
              title="Flip Route Orientation (R)"
              onClick={() => actions.flipWire?.(selectedWireObj.index)}
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <path d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
              </svg>
              <span>Flip</span>
            </button>
            <button
              type="button"
              className="wire-action-btn"
              title="Set Net Label"
              onClick={() => {
                const label = window.prompt('Net label (empty = none, GND = ground):', selectedWireObj.label ?? '');
                if (label !== null) {
                  actions.labelWire?.(selectedWireObj.index, label.trim());
                }
              }}
            >
              <span>Label</span>
            </button>
          </div>
        )}
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
