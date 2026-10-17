/**
 * The schematic sheet: SVG rendering of grid, components, wires, ghost preview,
 * wire draft and rubber band, with pan/zoom, terminal snap indicators, wire junction
 * connection dots, context menu, and floating canvas controls.
 */
import { useRef, useState, useMemo, useEffect, useCallback } from 'react';
import type { Dispatch, MouseEvent as ReactMouseEvent, WheelEvent as ReactWheelEvent, DragEvent as ReactDragEvent } from 'react';
import type { EditorState, Action } from '../model/store';
import { terminalPositions, terminalNear, findPlacementConflict } from '../model/geometry';
import { routeAvoidingObstacles, routingBlockedCells, densePoints, denseCellsOf, orthogonalizePolyline, simplifyCorners, translateWireSegment } from './WireRouter';
import { isWireEndPointConnected, findWireGeometryWarnings } from '../model/validation';
import { ComponentSymbol } from './symbols';
import { isScopeComponent, isScriptComponent } from '../simulation/scopes';
import { channelColorByIndex } from '../simulation/traceColors';
import {
  isGateDriver,
  isAmmeterComponent,
  isVoltmeterComponent,
  isTextboxComponent,
  getCoupledComponentName,
  getComponentMeta,
} from '../model/componentSchema';
import { SchematicNoteCard } from './SchematicNoteCard';
import {
  buildCouplingPairs,
  buildCurrentDirectionHints,
  buildMutualCouplingGuides,
  buildNetTerminalPoints,
  buildVoltmeterGuides,
  buildSignalEmitters,
  buildSignalDescriptions,
  buildNetWirePoints,
  buildScopeGuides,
  buildTerminalLabelItems,
} from './sheetGuides';
import { analyzeElectricalNets } from './netHighlight';
import type { Point, EditorComponent } from '../model/types';
import { ContextMenu } from './ContextMenu';
import type { ContextMenuTarget } from './ContextMenu';
import { Orientation, CANVAS_METRICS, LkComponentType } from '../model/constants';
import {
  isMutualCoupler,
  isCoupledInductor,
  getMutualCouplingUids,
} from '../model/componentSchema';

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
  resizeComponent?: (name: string, width: number, height: number, x?: number, y?: number) => void;
  rotateComponent?: (name: string) => void;
  deleteComponent?: (name: string) => void;
  deleteWire?: (index: number) => void;
  deleteSelection?: () => void;
  openProperties?: (name: string) => void;
  openScopeTab?: (name: string) => void;
  openScriptTab?: (name: string) => void;
  toggleWireMode?: () => void;
  openCommandPalette?: () => void;
}

export type NoteResizeHandle = 'nw' | 'n' | 'ne' | 'e' | 'se' | 's' | 'sw' | 'w';

export function getResizeCursor(handle: NoteResizeHandle): string {
  switch (handle) {
    case 'nw':
    case 'se':
      return 'nwse-resize';
    case 'ne':
    case 'sw':
      return 'nesw-resize';
    case 'n':
    case 's':
      return 'ns-resize';
    case 'e':
    case 'w':
      return 'ew-resize';
  }
}

const RESIZE_HANDLES: {
  id: NoteResizeHandle;
  cursor: string;
  getPos: (w: number, h: number) => [number, number];
}[] = [
  { id: 'nw', cursor: 'nwse-resize', getPos: () => [0, 0] },
  { id: 'n', cursor: 'ns-resize', getPos: (w) => [w / 2, 0] },
  { id: 'ne', cursor: 'nesw-resize', getPos: (w) => [w, 0] },
  { id: 'e', cursor: 'ew-resize', getPos: (w, h) => [w, h / 2] },
  { id: 'se', cursor: 'nwse-resize', getPos: (w, h) => [w, h] },
  { id: 's', cursor: 'ns-resize', getPos: (w, h) => [w / 2, h] },
  { id: 'sw', cursor: 'nesw-resize', getPos: (_w, h) => [0, h] },
  { id: 'w', cursor: 'ew-resize', getPos: (_w, h) => [0, h / 2] },
];

interface NoteResizeState {
  name: string;
  handle: NoteResizeHandle;
  startMouseGrid: Point;
  startPos: [number, number];
  startWidth: number;
  startHeight: number;
  currentPos: [number, number];
  currentWidth: number;
  currentHeight: number;
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

  // Viewport container dimensions for scrollbar metrics
  const [viewportSize, setViewportSize] = useState({ width: 800, height: 600 });
  const [scrollDrag, setScrollDrag] = useState<{
    axis: 'x' | 'y';
    startMouse: number;
    startPan: number;
  } | null>(null);

  // Measure and track container viewport size
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const updateSize = () => {
      const rect = container.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        setViewportSize({ width: rect.width, height: rect.height });
      }
    };

    updateSize();

    if (typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver(() => updateSize());
      observer.observe(container);
      return () => observer.disconnect();
    } else {
      window.addEventListener('resize', updateSize);
      return () => window.removeEventListener('resize', updateSize);
    }
  }, []);

  // Circuit content bounds in grid units, including components, note cards, and wires
  const contentBounds = useMemo(() => {
    let minX = 0;
    let minY = 0;
    let maxX = 40;
    let maxY = 30;

    let hasElements = false;

    for (const comp of state.components) {
      const w = isTextboxComponent(comp) ? Number(comp.parameters?.width) || 26 : 6;
      const h = isTextboxComponent(comp) ? Number(comp.parameters?.height) || 14 : 6;
      if (!hasElements) {
        minX = comp.position[0];
        minY = comp.position[1];
        maxX = comp.position[0] + w;
        maxY = comp.position[1] + h;
        hasElements = true;
      } else {
        minX = Math.min(minX, comp.position[0]);
        minY = Math.min(minY, comp.position[1]);
        maxX = Math.max(maxX, comp.position[0] + w);
        maxY = Math.max(maxY, comp.position[1] + h);
      }
    }

    for (const wire of state.wires) {
      if (!wire.points) continue;
      for (const [px, py] of wire.points) {
        if (!hasElements) {
          minX = px;
          maxX = px;
          minY = py;
          maxY = py;
          hasElements = true;
        } else {
          minX = Math.min(minX, px);
          maxX = Math.max(maxX, px);
          minY = Math.min(minY, py);
          maxY = Math.max(maxY, py);
        }
      }
    }

    const MARGIN = 6;
    if (!hasElements) {
      return {
        minGridX: 0,
        minGridY: 0,
        maxGridX: 40,
        maxGridY: 30,
      };
    }
    return {
      minGridX: minX - MARGIN,
      minGridY: minY - MARGIN,
      maxGridX: maxX + MARGIN,
      maxGridY: maxY + MARGIN,
    };
  }, [state.components, state.wires]);

  // Near-terminal hover snap halo and tooltip info
  const [hoveredTerminal, setHoveredTerminal] = useState<(Point & { label?: string; desc?: string; compName?: string }) | null>(null);

  // Live cursor grid coordinate
  const [cursorCoord, setCursorCoord] = useState<Point | null>(null);

  // Hovered component for guideline emphasis
  const [hoveredComponentName, setHoveredComponentName] = useState<string | null>(null);

  // Hovered wire index for whole-net highlighting (Phase 3)
  const [hoveredWireIndex, setHoveredWireIndex] = useState<number | null>(null);

  // Electrical net topological analysis (Phase 3 net highlighting)
  const netAnalysis = useMemo(
    () => analyzeElectricalNets(state.wires, state.components),
    [state.wires, state.components],
  );

  // Currently active electrical net to highlight across schematic
  const activeNet = useMemo(() => {
    if (hoveredWireIndex !== null) {
      return netAnalysis.wireIndexToNet.get(hoveredWireIndex) || null;
    }
    if (state.selectedWire !== null) {
      return netAnalysis.wireIndexToNet.get(state.selectedWire) || null;
    }
    if (hoveredTerminal !== null) {
      return netAnalysis.terminalKeyToNet.get(`${hoveredTerminal.x},${hoveredTerminal.y}`) || null;
    }
    if (state.focusedTerminal !== null) {
      return netAnalysis.terminalKeyToNet.get(`${state.focusedTerminal.x},${state.focusedTerminal.y}`) || null;
    }
    return null;
  }, [hoveredWireIndex, state.selectedWire, hoveredTerminal, state.focusedTerminal, netAnalysis]);

  // Net label visibility mode: 'smart' (virtual always show; wired on hover/selection), 'all' (always show all), or 'hover' (only on hover/selection)
  const [labelDisplayMode, setLabelDisplayMode] = useState<'smart' | 'all' | 'hover'>(() => {
    try {
      return (localStorage.getItem('gecko_label_mode') as 'smart' | 'all' | 'hover') || 'smart';
    } catch {
      return 'smart';
    }
  });

  // Active software coupling pairs (Gate Driver ➔ Switch, Ammeter ➔ Target Component)
  const couplingPairs = useMemo(
    () => buildCouplingPairs(state.components, state.selection, hoveredComponentName),
    [state.components, state.selection, hoveredComponentName],
  );

  // Ammeter reference current direction hints on measured components
  const currentDirectionHints = useMemo(
    () => buildCurrentDirectionHints(state.components, state.selection, hoveredComponentName),
    [state.components, state.selection, hoveredComponentName],
  );

  // Mutual couplers (LK_M) are pinless: their uid references to the two
  // coupled inductors are their only "wiring", so these guides stay visible
  // in the idle sheet, not just on hover.
  const mutualCouplingGuides = useMemo(
    () => buildMutualCouplingGuides(state.components, state.selection, hoveredComponentName),
    [state.components, state.selection, hoveredComponentName],
  );

  // First terminal position per net label — used to aim voltmeter
  // measurement guides at the nodes they measure.
  const netTerminalPoints = useMemo(
    () => buildNetTerminalPoints(state.components),
    [state.components],
  );

  // Voltmeter ➔ measured-node guides, shown on hover/selection instead of
  // the permanent badge text. Lines are coloured by polarity: red for the
  // positive node, brown for the negative node.
  const voltmeterGuides = useMemo(
    () =>
      buildVoltmeterGuides(
        state.components,
        state.selection,
        hoveredComponentName,
        netTerminalPoints,
      ),
    [state.components, state.selection, hoveredComponentName, netTerminalPoints],
  );

  // Emitted meter signal → the meter that emits it (for tracing scope channels
  // back to their measurement point)
  const signalEmitters = useMemo(
    () => buildSignalEmitters(state.components),
    [state.components],
  );

  // Emitted meter signal → human-readable measurement description, so scope
  // channel pills show WHAT is measured ("V(out)", "I(L.1)"), not just the
  // raw signal name. Nets that only exist on LK terminals are node voltages.
  const signalDescriptions = useMemo(
    () => buildSignalDescriptions(state.components),
    [state.components],
  );

  // Net label → all grid points of the wires carrying that net, so guides can
  // aim at the nearest point of the actual rail instead of an arbitrary pin
  // at the far end of the circuit.
  const netWirePoints = useMemo(
    () => buildNetWirePoints(state.wires),
    [state.wires],
  );

  // Scope ➔ measurement-point guides, shown on hover/selection, decided per
  // input pin: pins with a real wire show the connection already; unwired
  // pins bound by net name (bare power net, or a meter's emitted signal)
  // get a dashed line to the point they measure — aiming at the nearest
  // point of that net's wiring. Each line keeps its channel's trace color
  // and a label at the target so the fan of lines stays attributable.
  const scopeGuides = useMemo(
    () =>
      buildScopeGuides({
        components: state.components,
        wires: state.wires,
        selection: state.selection,
        hoveredComponentName,
        netTerminalPoints,
        netWirePoints,
        signalEmitters,
        signalDescriptions,
      }),
    [
      state.components,
      state.wires,
      state.selection,
      hoveredComponentName,
      netTerminalPoints,
      netWirePoints,
      signalEmitters,
      signalDescriptions,
    ],
  );

  // Pre-computed, deduplicated terminal net labels across all circuit components
  const terminalLabelItems = useMemo(
    () =>
      buildTerminalLabelItems(
        state.components,
        state.wires,
        signalDescriptions,
        netTerminalPoints,
      ),
    [state.components, state.wires, signalDescriptions, netTerminalPoints],
  );

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

  // Note resize drag state (Microsoft Word style 8-point resizing)
  const [noteResize, setNoteResize] = useState<NoteResizeState | null>(null);

  const clientToGrid = useCallback(
    (clientX: number, clientY: number): Point => {
      if (!svgRef.current) return { x: 0, y: 0 };
      const rect = svgRef.current.getBoundingClientRect();
      const x = (clientX - rect.left) / zoom;
      const y = (clientY - rect.top) / zoom;
      return {
        x: Math.round(x / dpix),
        y: Math.round(y / dpix),
      };
    },
    [dpix, zoom],
  );

  const toGrid = useCallback(
    (e: ReactMouseEvent): Point => clientToGrid(e.clientX, e.clientY),
    [clientToGrid],
  );

  const startNoteResize = useCallback(
    (e: ReactMouseEvent, comp: EditorComponent, handle: NoteResizeHandle) => {
      if (e.button !== 0) return;
      e.stopPropagation();
      e.preventDefault();

      const p = clientToGrid(e.clientX, e.clientY);
      const startW = Number(comp.parameters?.width) || 26;
      const startH = Number(comp.parameters?.height) || 14;

      if (!state.selection.includes(comp.name)) {
        dispatch({ type: 'SELECT', name: comp.name, additive: false });
      }

      setNoteResize({
        name: comp.name,
        handle,
        startMouseGrid: p,
        startPos: [comp.position[0], comp.position[1]],
        startWidth: startW,
        startHeight: startH,
        currentPos: [comp.position[0], comp.position[1]],
        currentWidth: startW,
        currentHeight: startH,
      });
    },
    [clientToGrid, state.selection, dispatch],
  );

  const updateNoteResize = useCallback(
    (clientX: number, clientY: number) => {
      setNoteResize((prev) => {
        if (!prev) return null;
        const p = clientToGrid(clientX, clientY);
        const dx = p.x - prev.startMouseGrid.x;
        const dy = p.y - prev.startMouseGrid.y;

        const MIN_W = 8;
        const MIN_H = 4;
        const MAX_W = 100;
        const MAX_H = 80;

        const startX = prev.startPos[0];
        const startY = prev.startPos[1];
        const startW = prev.startWidth;
        const startH = prev.startHeight;

        let newX = startX;
        let newY = startY;
        let newW = startW;
        let newH = startH;

        const rightPinned = startX + startW;
        const bottomPinned = startY + startH;

        switch (prev.handle) {
          case 'e':
            newW = Math.max(MIN_W, Math.min(MAX_W, startW + dx));
            break;
          case 'se':
            newW = Math.max(MIN_W, Math.min(MAX_W, startW + dx));
            newH = Math.max(MIN_H, Math.min(MAX_H, startH + dy));
            break;
          case 's':
            newH = Math.max(MIN_H, Math.min(MAX_H, startH + dy));
            break;
          case 'sw': {
            const candX = startX + dx;
            const minAllowedX = Math.max(0, rightPinned - MAX_W);
            const maxAllowedX = rightPinned - MIN_W;
            newX = Math.max(minAllowedX, Math.min(maxAllowedX, candX));
            newW = rightPinned - newX;
            newH = Math.max(MIN_H, Math.min(MAX_H, startH + dy));
            break;
          }
          case 'w': {
            const candX = startX + dx;
            const minAllowedX = Math.max(0, rightPinned - MAX_W);
            const maxAllowedX = rightPinned - MIN_W;
            newX = Math.max(minAllowedX, Math.min(maxAllowedX, candX));
            newW = rightPinned - newX;
            break;
          }
          case 'nw': {
            const candX = startX + dx;
            const minAllowedX = Math.max(0, rightPinned - MAX_W);
            const maxAllowedX = rightPinned - MIN_W;
            newX = Math.max(minAllowedX, Math.min(maxAllowedX, candX));
            newW = rightPinned - newX;

            const candY = startY + dy;
            const minAllowedY = Math.max(0, bottomPinned - MAX_H);
            const maxAllowedY = bottomPinned - MIN_H;
            newY = Math.max(minAllowedY, Math.min(maxAllowedY, candY));
            newH = bottomPinned - newY;
            break;
          }
          case 'n': {
            const candY = startY + dy;
            const minAllowedY = Math.max(0, bottomPinned - MAX_H);
            const maxAllowedY = bottomPinned - MIN_H;
            newY = Math.max(minAllowedY, Math.min(maxAllowedY, candY));
            newH = bottomPinned - newY;
            break;
          }
          case 'ne': {
            newW = Math.max(MIN_W, Math.min(MAX_W, startW + dx));
            const candY = startY + dy;
            const minAllowedY = Math.max(0, bottomPinned - MAX_H);
            const maxAllowedY = bottomPinned - MIN_H;
            newY = Math.max(minAllowedY, Math.min(maxAllowedY, candY));
            newH = bottomPinned - newY;
            break;
          }
        }

        if (
          prev.currentWidth === newW &&
          prev.currentHeight === newH &&
          prev.currentPos[0] === newX &&
          prev.currentPos[1] === newY
        ) {
          return prev;
        }
        return {
          ...prev,
          currentPos: [newX, newY],
          currentWidth: newW,
          currentHeight: newH,
        };
      });
    },
    [clientToGrid],
  );

  const commitNoteResize = useCallback(() => {
    setNoteResize((prev) => {
      if (!prev) return null;
      const changed =
        prev.currentWidth !== prev.startWidth ||
        prev.currentHeight !== prev.startHeight ||
        prev.currentPos[0] !== prev.startPos[0] ||
        prev.currentPos[1] !== prev.startPos[1];

      if (changed) {
        actions.resizeComponent?.(
          prev.name,
          prev.currentWidth,
          prev.currentHeight,
          prev.currentPos[0],
          prev.currentPos[1],
        );
      }
      return null;
    });
  }, [actions]);

  useEffect(() => {
    if (!noteResize) return;

    const originalCursor = document.body.style.cursor;
    document.body.style.cursor = getResizeCursor(noteResize.handle);

    const handleWindowMouseMove = (e: MouseEvent) => {
      updateNoteResize(e.clientX, e.clientY);
    };

    const handleWindowMouseUp = () => {
      commitNoteResize();
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setNoteResize(null);
      }
    };

    window.addEventListener('mousemove', handleWindowMouseMove);
    window.addEventListener('mouseup', handleWindowMouseUp);
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.cursor = originalCursor;
      window.removeEventListener('mousemove', handleWindowMouseMove);
      window.removeEventListener('mouseup', handleWindowMouseUp);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [noteResize, updateNoteResize, commitNoteResize]);

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

  // Set of terminal coordinates "x,y" that are connected (LTspice-style):
  // A terminal is connected if a wire touches it or another component's terminal is coincident.
  const connectedTerminalKeys = useMemo(() => {
    const keys = new Set<string>();
    // 1. All wire raster points
    for (const wire of state.wires) {
      if (!wire.points || wire.points.length === 0) continue;
      for (const p of wire.points) {
        keys.add(`${p[0]},${p[1]}`);
      }
    }
    // 2. Coincident component terminals (pin-to-pin abutment)
    const pinCounts = new Map<string, number>();
    for (const comp of state.components) {
      const terms = terminalPositions(comp);
      for (const t of [...terms.input, ...terms.output]) {
        const k = `${t.x},${t.y}`;
        pinCounts.set(k, (pinCounts.get(k) || 0) + 1);
      }
    }
    for (const [k, count] of pinCounts.entries()) {
      if (count > 1) {
        keys.add(k);
      }
    }
    return keys;
  }, [state.wires, state.components]);

  // Free (open-circuit) wire endpoints that touch neither a terminal nor any other wire.
  const danglingEndpoints = useMemo(() => {
    const terminalKeys = new Set<string>();
    for (const comp of state.components) {
      const terms = terminalPositions(comp);
      for (const t of [...terms.input, ...terms.output]) {
        terminalKeys.add(`${t.x},${t.y}`);
      }
    }

    const wireCells = state.wires.map((w) => denseCellsOf(w.points || []));
    const dangling: Point[] = [];
    const seen = new Set<string>();

    state.wires.forEach((w, i) => {
      if (!w.points || w.points.length < 2) return;
      const ends = [w.points[0], w.points[w.points.length - 1]];
      ends.forEach((end) => {
        const key = `${end[0]},${end[1]}`;
        if (terminalKeys.has(key)) return;
        const touchesOther = wireCells.some((cells, j) => j !== i && cells.has(key));
        if (!touchesOther && !seen.has(key)) {
          seen.add(key);
          dangling.push({ x: end[0], y: end[1] });
        }
      });
    });

    return dangling;
  }, [state.wires, state.components]);

  // CAD Scrollbar metrics (track sizes, thumb sizes, positions)
  const scrollMetrics = useMemo(() => {
    const SCROLLBAR_SIZE = 14;
    const STEPPER_SIZE = 14;
    const MIN_THUMB_SIZE = 28;

    const trackWidth = Math.max(20, viewportSize.width - SCROLLBAR_SIZE - 2 * STEPPER_SIZE);
    const trackHeight = Math.max(20, viewportSize.height - SCROLLBAR_SIZE - 2 * STEPPER_SIZE);

    const contentPixelWidth = Math.max(1, (contentBounds.maxGridX - contentBounds.minGridX) * dpix * zoom);
    const contentPixelHeight = Math.max(1, (contentBounds.maxGridY - contentBounds.minGridY) * dpix * zoom);

    const upperPanX = -contentBounds.minGridX * dpix * zoom + 40;
    const lowerPanX = Math.min(upperPanX, viewportSize.width - 40 - contentBounds.maxGridX * dpix * zoom);
    const spanX = Math.max(1, upperPanX - lowerPanX);

    const upperPanY = -contentBounds.minGridY * dpix * zoom + 40;
    const lowerPanY = Math.min(upperPanY, viewportSize.height - 40 - contentBounds.maxGridY * dpix * zoom);
    const spanY = Math.max(1, upperPanY - lowerPanY);

    const ratioX = Math.max(0, Math.min(1, (upperPanX - pan.x) / spanX));
    const ratioY = Math.max(0, Math.min(1, (upperPanY - pan.y) / spanY));

    const visibleFractionX = Math.min(1, viewportSize.width / Math.max(viewportSize.width, contentPixelWidth));
    const visibleFractionY = Math.min(1, viewportSize.height / Math.max(viewportSize.height, contentPixelHeight));

    const thumbWidth = Math.max(MIN_THUMB_SIZE, Math.min(trackWidth, visibleFractionX * trackWidth));
    const thumbHeight = Math.max(MIN_THUMB_SIZE, Math.min(trackHeight, visibleFractionY * trackHeight));

    const thumbLeft = ratioX * (trackWidth - thumbWidth);
    const thumbTop = ratioY * (trackHeight - thumbHeight);

    return {
      trackWidth,
      trackHeight,
      upperPanX,
      lowerPanX,
      spanX,
      upperPanY,
      lowerPanY,
      spanY,
      thumbWidth,
      thumbHeight,
      thumbLeft,
      thumbTop,
    };
  }, [contentBounds, dpix, zoom, pan.x, pan.y, viewportSize.width, viewportSize.height]);

  // Window drag listener for CAD scrollbar thumbs
  useEffect(() => {
    if (!scrollDrag) return;

    const handleWindowMouseMove = (e: MouseEvent) => {
      if (scrollDrag.axis === 'x') {
        const deltaClient = e.clientX - scrollDrag.startMouse;
        const availableTrack = scrollMetrics.trackWidth - scrollMetrics.thumbWidth;
        if (availableTrack > 0) {
          const deltaRatio = deltaClient / availableTrack;
          const newPanX = scrollDrag.startPan - deltaRatio * scrollMetrics.spanX;
          setPan((prev) => ({ ...prev, x: Math.round(newPanX) }));
        }
      } else {
        const deltaClient = e.clientY - scrollDrag.startMouse;
        const availableTrack = scrollMetrics.trackHeight - scrollMetrics.thumbHeight;
        if (availableTrack > 0) {
          const deltaRatio = deltaClient / availableTrack;
          const newPanY = scrollDrag.startPan - deltaRatio * scrollMetrics.spanY;
          setPan((prev) => ({ ...prev, y: Math.round(newPanY) }));
        }
      }
    };

    const handleWindowMouseUp = () => {
      setScrollDrag(null);
    };

    window.addEventListener('mousemove', handleWindowMouseMove);
    window.addEventListener('mouseup', handleWindowMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleWindowMouseMove);
      window.removeEventListener('mouseup', handleWindowMouseUp);
    };
  }, [scrollDrag, scrollMetrics]);

  const handleHTrackClick = (e: ReactMouseEvent<HTMLDivElement>) => {
    if ((e.target as HTMLElement).classList.contains('scrollbar-thumb')) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const availableTrack = scrollMetrics.trackWidth - scrollMetrics.thumbWidth;
    if (availableTrack <= 0) return;
    const targetThumbLeft = Math.max(0, Math.min(availableTrack, clickX - scrollMetrics.thumbWidth / 2));
    const targetRatio = targetThumbLeft / availableTrack;
    setPan((prev) => ({
      ...prev,
      x: Math.round(scrollMetrics.upperPanX - targetRatio * scrollMetrics.spanX),
    }));
  };

  const handleVTrackClick = (e: ReactMouseEvent<HTMLDivElement>) => {
    if ((e.target as HTMLElement).classList.contains('scrollbar-thumb')) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const clickY = e.clientY - rect.top;
    const availableTrack = scrollMetrics.trackHeight - scrollMetrics.thumbHeight;
    if (availableTrack <= 0) return;
    const targetThumbTop = Math.max(0, Math.min(availableTrack, clickY - scrollMetrics.thumbHeight / 2));
    const targetRatio = targetThumbTop / availableTrack;
    setPan((prev) => ({
      ...prev,
      y: Math.round(scrollMetrics.upperPanY - targetRatio * scrollMetrics.spanY),
    }));
  };

  const handleWheel = (e: ReactWheelEvent) => {
    if (e.ctrlKey || e.metaKey) {
      e.preventDefault();
      const zoomFactor = e.deltaY < 0 ? 1.15 : 0.87;
      setZoom((prev) => Math.max(0.3, Math.min(3.0, prev * zoomFactor)));
    } else {
      // Mouse wheel / trackpad scrolling: pan the canvas
      e.preventDefault();
      const dx = e.shiftKey ? e.deltaY : e.deltaX;
      const dy = e.shiftKey ? 0 : e.deltaY;
      setPan((prev) => ({
        x: Math.round(prev.x - dx),
        y: Math.round(prev.y - dy),
      }));
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
    if (noteResize) return;

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

    if (noteResize) {
      updateNoteResize(e.clientX, e.clientY);
      return;
    }

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
    if (noteResize) {
      commitNoteResize();
      return;
    }

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
    if (state.mode === 'wiring') {
      if (state.wireDraft) {
        dispatch({ type: 'WIRE_DRAFT_ABORT' });
      } else {
        actions.toggleWireMode?.();
      }
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
    const width = container.width > 0 ? container.width : viewportSize.width;
    const height = container.height > 0 ? container.height : viewportSize.height;

    const cWidthPx = Math.max(200, (contentBounds.maxGridX - contentBounds.minGridX) * dpix);
    const cHeightPx = Math.max(150, (contentBounds.maxGridY - contentBounds.minGridY) * dpix);
    const availW = Math.max(100, width - 60);
    const availH = Math.max(100, height - 60);
    const fitZoom = Math.max(0.4, Math.min(2.0, Math.min(availW / cWidthPx, availH / cHeightPx)));

    // Center the content in the viewport:
    const contentCenterXPx = (contentBounds.minGridX + (contentBounds.maxGridX - contentBounds.minGridX) / 2) * dpix * fitZoom;
    const contentCenterYPx = (contentBounds.minGridY + (contentBounds.maxGridY - contentBounds.minGridY) / 2) * dpix * fitZoom;
    const targetPanX = (width / 2) - contentCenterXPx;
    const targetPanY = (height / 2) - contentCenterYPx;

    setZoom(fitZoom);
    setPan({ x: Math.round(targetPanX), y: Math.round(targetPanY) });
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
      className={`sheet-scroll ${isSpacePressed ? 'space-grab' : ''} ${isPanning ? 'panning' : ''} ${state.mode === 'wiring' ? 'mode-wiring' : ''}`}
      ref={containerRef}
      onWheel={handleWheel}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      onMouseLeave={() => setCursorCoord(null)}
    >
      {/* Active Wire Mode Floating Banner */}
      {state.mode === 'wiring' && (
        <div className="canvas-wire-mode-banner">
          <span className="banner-icon">⚡</span>
          <span className="banner-text">
            {state.wireDraft ? 'Drafting Wire — Click destination terminal or wire' : 'Wire Tool Active — Click terminal to start wire'}
          </span>
          <button
            type="button"
            className="banner-exit-btn"
            onClick={() => {
              if (state.wireDraft) {
                dispatch({ type: 'WIRE_DRAFT_ABORT' });
              }
              actions.toggleWireMode?.();
            }}
            title="Exit Wire Tool (Esc)"
          >
            Exit (Esc)
          </button>
        </div>
      )}

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
        {activeNet && (
          <>
            <div className="canvas-ctrl-sep" />
            <span
              className="canvas-net-badge"
              title={`Electrical Net: ${activeNet.label || activeNet.id} (${activeNet.wireIndices.size} wires, ${activeNet.terminalKeys.size} pins connected)`}
            >
              Net: {activeNet.label || activeNet.id}
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
          style={{
            cursor: noteResize
              ? getResizeCursor(noteResize.handle)
              : isPanning
                ? 'grabbing'
                : isSpacePressed
                  ? 'grab'
                  : undefined,
          }}
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
              <path d="M 0 1.5 L 9 5 L 0 8.5 z" style={{ fill: 'var(--accent)' }} />
            </marker>
            <marker
              id="mutual-coupling-arrow"
              viewBox="0 0 10 10"
              refX="8"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 1.5 L 9 5 L 0 8.5 z" fill="#a78bfa" />
            </marker>
            <marker
              id="coupling-dot"
              viewBox="0 0 10 10"
              refX="5"
              refY="5"
              markerWidth="6"
              markerHeight="6"
            >
              <circle cx="5" cy="5" r="4" style={{ fill: 'var(--accent)' }} />
            </marker>
            <marker
              id="current-direction-arrow"
              viewBox="0 0 10 10"
              refX="8"
              refY="5"
              markerWidth="6"
              markerHeight="6"
              orient="auto-start-reverse"
            >
              <path d="M 0 1.5 L 9 5 L 0 8.5 z" fill="#f59e0b" />
            </marker>
          </defs>

          {/* Grid Background */}
          <rect
            width={rawWidth}
            height={rawHeight}
            fill={showGrid ? 'url(#grid-dots)' : undefined}
            style={showGrid ? undefined : { fill: 'var(--bg)' }}
          />

          {/* Wires */}
          <g className="wires">
            {/* Casing pass: a sheet-colored underlay under every wire, so two
                wires that do overlap read as a stroke/casing/stroke sandwich
                instead of one line. Painted before all visible strokes. */}
            {state.wires.map((wire) => {
              const casingPts = orthogonalizePolyline(wire.points);
              const casingStr = casingPts.map((p) => `${p[0] * dpix},${p[1] * dpix}`).join(' ');
              const isNetHighlighted = activeNet?.wireIndices.has(wire.index) ?? false;
              return (
                <polyline
                  key={`casing-${wire.index}`}
                  points={casingStr}
                  className={`wire-casing${isNetHighlighted ? ' net-highlighted' : ''}`}
                />
              );
            })}
            {state.wires.map((wire) => {
              const orthoPts = orthogonalizePolyline(wire.points);
              const pointsStr = orthoPts.map((p) => `${p[0] * dpix},${p[1] * dpix}`).join(' ');
              const isSelected = wire.index === state.selectedWire;
              const isNetHighlighted = activeNet?.wireIndices.has(wire.index) ?? false;
              const corners = isSelected ? simplifyCorners(wire.points) : [];
              return (
                <g key={wire.index} className={`wire-group${isSelected ? ' selected' : ''}${isNetHighlighted ? ' net-highlighted' : ''}`}>
                  <polyline
                    points={pointsStr}
                    className={`wire wire-${wire.type || 'LK'}${isSelected ? ' selected' : ''}${isNetHighlighted ? ' net-highlighted' : ''}`}
                  />
                  <polyline
                    points={pointsStr}
                    className="wire-hit"
                    onMouseEnter={() => setHoveredWireIndex(wire.index)}
                    onMouseLeave={() => setHoveredWireIndex((cur) => (cur === wire.index ? null : cur))}
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
            {junctionDots.map((pt, i) => {
              const isNetHighlighted = activeNet?.pointKeys.has(`${pt.x},${pt.y}`) ?? false;
              return (
                <circle
                  key={i}
                  cx={pt.x * dpix}
                  cy={pt.y * dpix}
                  r={isNetHighlighted ? 4.5 : 3.6}
                  className={`junction-dot${isNetHighlighted ? ' net-highlighted' : ''}`}
                />
              );
            })}
          </g>

          {/* Dangling (Unconnected Open-Circuit) Wire Endpoints */}
          <g className="dangling-endpoints" pointerEvents="none">
            {danglingEndpoints.map((pt, i) => (
              <g key={`dangling-${i}`} transform={`translate(${pt.x * dpix}, ${pt.y * dpix})`}>
                <rect
                  x={-4}
                  y={-4}
                  width={8}
                  height={8}
                  rx={1.5}
                  className="wire-dangling-box"
                />
                <line x1={-2.5} y1={-2.5} x2={2.5} y2={2.5} className="wire-dangling-cross" />
                <line x1={-2.5} y1={2.5} x2={2.5} y2={-2.5} className="wire-dangling-cross" />
              </g>
            ))}
          </g>

          {/* Mutual coupler (LK_M) ➔ coupled inductor guides. Always visible:
              the coupler is pinless by design (SPICE K-statement semantics),
              so these dashed references are the only visualization of its
              "wiring". Violet distinguishes magnetic coupling from electrical
              wires and from the blue software-coupling guides. */}
          <g className="coupling-guides-layer mutual-coupling-guides" pointerEvents="none">
            {mutualCouplingGuides.map((guide) => {
              const sx = guide.source.position[0] * dpix;
              const sy = guide.source.position[1] * dpix;
              const stroke = guide.isHoveredOrSelected ? '#c084fc' : '#a78bfa';
              const halo = guide.isHoveredOrSelected ? 'rgba(192, 132, 252, 0.28)' : 'rgba(167, 139, 250, 0.14)';
              const warn = !guide.isResolved;

              return (
                <g key={`mutual-${guide.key}`}>
                  {guide.refs.map((ref) => {
                    if (!ref.target) return null;
                    const tx = ref.target.position[0] * dpix;
                    const ty = ref.target.position[1] * dpix;
                    // Trim both ends so the curve leaves the coupler below its
                    // badges and stops short of the target's name label.
                    const t0 = 0.18;
                    const t1 = 0.86;
                    const x1 = sx + (tx - sx) * t0;
                    const y1 = sy + (ty - sy) * t0;
                    const x2 = sx + (tx - sx) * t1;
                    const y2 = sy + (ty - sy) * t1;
                    const pathD = `M ${x1} ${y1} C ${x1 + (x2 - x1) * 0.4} ${y1}, ${x1 + (x2 - x1) * 0.6} ${y2}, ${x2} ${y2}`;
                    const pillW = Math.max(ref.target.name.length * 5.8 + 12, 34);
                    return (
                      <g key={`${guide.key}-${ref.uid}`}>
                        <path d={pathD} fill="none" stroke={halo} strokeWidth={8} strokeLinecap="round" />
                        <path
                          d={pathD}
                          fill="none"
                          stroke={stroke}
                          strokeWidth={guide.isHoveredOrSelected ? 2.5 : 1.8}
                          strokeDasharray="6 4"
                          className="coupling-guideline"
                          markerEnd="url(#mutual-coupling-arrow)"
                          style={guide.isHoveredOrSelected ? { animation: 'couplingDashFlow 1s linear infinite' } : undefined}
                        />
                        <g transform={`translate(${tx}, ${ty - 12})`}>
                          <rect
                            x={-pillW / 2}
                            y={-8}
                            width={pillW}
                            height={15}
                            rx={7}
                            style={{ fill: 'var(--pill-bg)' }}
                            stroke={stroke}
                            strokeWidth={1}
                          />
                          <text
                            x={0}
                            y={3}
                            textAnchor="middle"
                            fill={stroke}
                            fontSize={8.5}
                            fontWeight="700"
                            fontFamily="var(--font-mono, monospace)"
                          >
                            {ref.target.name}
                          </text>
                        </g>
                      </g>
                    );
                  })}

                  {/* k badge above the coupler (its guides leave downward);
                      amber when a reference is missing */}
                  <g transform={`translate(${sx}, ${sy - 2.2 * dpix})`}>
                    {(() => {
                      const text = warn
                        ? `k=${guide.k} · ⚠ assign inductors`
                        : `k=${guide.k}`;
                      const w = Math.max(text.length * 5.6 + 12, 40);
                      const c = warn ? '#f59e0b' : stroke;
                      return (
                        <>
                          <rect
                            x={-w / 2}
                            y={-8}
                            width={w}
                            height={15}
                            rx={7}
                            style={{ fill: 'var(--pill-bg)' }}
                            stroke={c}
                            strokeWidth={1}
                          />
                          <text
                            x={0}
                            y={3}
                            textAnchor="middle"
                            fill={c}
                            fontSize={8.5}
                            fontWeight="700"
                            fontFamily="var(--font-mono, monospace)"
                          >
                            {text}
                          </text>
                        </>
                      );
                    })()}
                  </g>
                </g>
              );
            })}
          </g>

          {/* Components */}
          <g className="components" pointerEvents={state.mode === 'placing' ? 'none' : 'auto'}>
            {state.components.map((component) => {
              const selected = state.selection.includes(component.name);
              const terminals = terminalPositions(component);
              const isResizingThis = noteResize?.name === component.name;
              const posX = isResizingThis ? noteResize.currentPos[0] : component.position[0];
              const posY = isResizingThis ? noteResize.currentPos[1] : component.position[1];
              return (
                <g
                  key={component.name}
                  transform={`translate(${posX * dpix}, ${posY * dpix})`}
                  data-name={component.name}
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
                    } else if (isScriptComponent(component)) {
                      actions.openScriptTab?.(component.name);
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
                      visually overlap. Multi-pin blocks are TOP-ANCHORED
                      (channel 1 on the anchor row, block grows downward), so
                      the bounding box extends asymmetrically downward. */}
                  {(() => {
                    const isTextbox = isTextboxComponent(component);
                    if (isTextbox) {
                      const widthGrid = isResizingThis
                        ? noteResize.currentWidth
                        : (Number(component.parameters?.width) || 26);
                      const heightGrid = isResizingThis
                        ? noteResize.currentHeight
                        : (Number(component.parameters?.height) || 14);
                      const boxW = widthGrid * dpix;
                      const boxH = heightGrid * dpix;
                      return (
                        <>
                          {/* Full-area hit target for note card (clicking/dragging moves the component) */}
                          <rect
                            x={0}
                            y={0}
                            width={boxW}
                            height={boxH}
                            className="component-hit-area textbox-hit-area"
                            fill="transparent"
                            stroke="none"
                            pointerEvents="all"
                            style={{ cursor: 'pointer' }}
                          />
                          <foreignObject
                            x={0}
                            y={0}
                            width={boxW}
                            height={boxH}
                            style={{ pointerEvents: 'none', overflow: 'hidden' }}
                          >
                            <SchematicNoteCard
                              name={component.name}
                              text={String(component.parameters?.text ?? '')}
                              selected={selected}
                              width={boxW}
                              height={boxH}
                            />
                          </foreignObject>
                          {/* Selection frame halo */}
                          {selected && (
                            <rect
                              x={-1}
                              y={-1}
                              width={boxW + 2}
                              height={boxH + 2}
                              className="selection-box textbox-selection-box"
                              stroke="#0284c7"
                              strokeWidth={1.5}
                              strokeDasharray={isResizingThis ? '4 3' : undefined}
                              fill="none"
                              rx={8}
                              pointerEvents="none"
                            />
                          )}
                          {/* 8 Resize handles (Microsoft Word style) */}
                          {selected &&
                            RESIZE_HANDLES.map((h) => {
                              const [hx, hy] = h.getPos(boxW, boxH);
                              return (
                                <g
                                  key={h.id}
                                  data-testid={`resize-handle-${h.id}`}
                                  className={`note-resize-handle note-resize-${h.id}`}
                                  transform={`translate(${hx}, ${hy})`}
                                  onMouseDown={(e) => startNoteResize(e, component, h.id)}
                                  onDoubleClick={(e) => e.stopPropagation()}
                                  style={{ cursor: h.cursor }}
                                >
                                  {/* Transparent 20x20 hit target for effortless grabbing */}
                                  <rect
                                    x={-10}
                                    y={-10}
                                    width={20}
                                    height={20}
                                    fill="transparent"
                                    stroke="none"
                                    pointerEvents="all"
                                    style={{ cursor: h.cursor }}
                                  />
                                  {/* Word-style knob: 9x9 white square with blue border and shadow */}
                                  <rect
                                    x={-4.5}
                                    y={-4.5}
                                    width={9}
                                    height={9}
                                    rx={1.5}
                                    fill="#ffffff"
                                    stroke="#0284c7"
                                    strokeWidth={1.5}
                                    className="resize-knob"
                                    style={{
                                      pointerEvents: 'none',
                                      filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.45))',
                                    }}
                                  />
                                </g>
                              );
                            })}
                          {/* Live dimension tooltip badge during resizing */}
                          {isResizingThis && (
                            <g
                              className="note-resize-dimension-badge"
                              transform={`translate(${boxW / 2}, ${boxH + 20})`}
                              style={{ pointerEvents: 'none', userSelect: 'none' }}
                            >
                              <rect
                                x={-36}
                                y={-11}
                                width={72}
                                height={22}
                                rx={5}
                                fill="#0f172a"
                                stroke="#38bdf8"
                                strokeWidth={1.2}
                                style={{ filter: 'drop-shadow(0 4px 8px rgba(0,0,0,0.5))' }}
                              />
                              <text
                                x={0}
                                y={4}
                                textAnchor="middle"
                                fontSize={11}
                                fontWeight={600}
                                fill="#38bdf8"
                                fontFamily="system-ui, -apple-system, sans-serif"
                              >
                                {widthGrid} × {heightGrid}
                              </text>
                            </g>
                          )}
                        </>
                      );
                    }

                    const isTransformer =
                      (component.family === 'LK' || !component.family) &&
                      (component.type === 23 || component.type === LkComponentType.TRANSFORMER);
                    const maxPins = Math.max(terminals.input.length, terminals.output.length, 1);
                    // Single-pin blocks keep the classic symmetric extent
                    // (name label + coupling badge offsets depend on it);
                    // anchored multi-pin blocks extend downward only.
                    // Transformers are symmetric 4-terminal elements spanning ±2 grid units.
                    const top = isTransformer
                      ? -2.2 * dpix
                      : (maxPins > 1 ? -0.9 : -1.3) * dpix;
                    const bottom = isTransformer
                      ? 2.2 * dpix
                      : (maxPins > 1 ? maxPins - 1 + 0.9 : 1.3) * dpix;
                    const halfW = isTransformer
                      ? 2.2 * dpix
                      : 1.3 * dpix;
                    return (
                      <>
                        {/* Full-area hit target so clicking anywhere on the component bounding area selects it */}
                        <rect
                          x={-halfW}
                          y={top}
                          width={halfW * 2}
                          height={bottom - top}
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
                            y={top}
                            width={halfW * 2}
                            height={bottom - top}
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
                          const isConnected = connectedTerminalKeys.has(`${t.x},${t.y}`);
                          const isNetHighlighted = activeNet?.terminalKeys.has(`${t.x},${t.y}`) ?? false;
                          const isInput = i < terminals.input.length;
                          const pinIdx = isInput ? i : i - terminals.input.length;
                          const meta = getComponentMeta(component.type, component.family, component.name);
                          const termDef = isInput ? meta?.terminals?.input?.[pinIdx] : meta?.terminals?.output?.[pinIdx];
                          const termLabel = isInput
                            ? (component.inputLabels?.[pinIdx] || termDef?.label || `In ${pinIdx + 1}`)
                            : (component.outputLabels?.[pinIdx] || termDef?.label || `Out ${pinIdx + 1}`);
                          const termDesc = termDef?.description || `${isInput ? 'Input' : 'Output'} pin ${pinIdx + 1}`;
                          const pinTitle = `${component.name} · ${termLabel}: ${termDesc} (${isInput ? 'Input' : 'Output'} ${pinIdx + 1})`;
                          return (
                            <g key={i} className={`terminal-pin-group ${isConnected ? 'connected' : 'unconnected'}${isNetHighlighted ? ' net-highlighted' : ''}`}>
                              <title>{pinTitle}</title>
                              <circle
                                cx={cx}
                                cy={cy}
                                r={isConnected ? 2.5 : 0}
                                className={`terminal ${isConnected ? 'terminal-connected' : 'terminal-unconnected'}`}
                              />
                              {!isConnected && (
                                <rect
                                  x={cx - 3.5}
                                  y={cy - 3.5}
                                  width={7}
                                  height={7}
                                  rx={1}
                                  className="terminal-open-box"
                                />
                              )}
                              <circle
                                cx={cx}
                                cy={cy}
                                r={8}
                                fill="none"
                                stroke="none"
                                className="terminal-hit"
                                style={{ cursor: 'crosshair', pointerEvents: 'all' }}
                                onMouseEnter={() => setHoveredTerminal({ x: t.x, y: t.y, label: termLabel, desc: termDesc, compName: component.name })}
                                onMouseLeave={() => setHoveredTerminal((cur) => (cur && cur.x === t.x && cur.y === t.y ? null : cur))}
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
                          if (isTextboxComponent(component)) {
                            return null;
                          }
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
                          const isGround = component.type === 31;
                          if (!isGround && (isControlBlock || isHorizontalTwoPort)) {
                            // Control blocks (SIGNAL.1, etc.) & Horizontal components (L.1, horizontal resistors): place name centered above
                            return (
                              <text x={0} y={top - 5} textAnchor="middle" className="component-name">
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

                        {/* Mutual coupler badge: unlike the transient gate-driver
                            badges above, this one is permanent — the coupler has
                            no wires, so naming its two inductors here is the
                            at-a-glance answer to "what does this element do". */}
                        {isMutualCoupler(component) &&
                          (() => {
                            const [uidA, uidB] = getMutualCouplingUids(component);
                            const tA = state.components.find(
                              (c) => c.uid === uidA && isCoupledInductor(c),
                            );
                            const tB = state.components.find(
                              (c) => c.uid === uidB && isCoupledInductor(c),
                            );
                            const ok = !!tA && !!tB;
                            return (
                              <text
                                y={-2.2 * dpix - 16}
                                textAnchor="middle"
                                className="coupling-pill-text"
                                style={{ fill: ok ? '#a78bfa' : '#f59e0b', cursor: 'pointer' }}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  dispatch({ type: 'SELECT', name: component.name, additive: false });
                                  dispatch({ type: 'PANEL_FOR', name: component.name });
                                }}
                              >
                                {ok ? `${tA!.name} ⟷ ${tB!.name}` : '⚠ assign inductors in Properties'}
                              </text>
                            );
                          })()}

                        {/* Coupling Badge Tag on Control Blocks (Gate Driver, Ammeter, Voltmeter) */}
                        {(() => {
                          const coupled = getCoupledComponentName(component);
                          const isGd = isGateDriver(component);
                          const isAm = isAmmeterComponent(component);
                          const isVm = isVoltmeterComponent(component);

                          if (!isGd && !isAm && !isVm) return null;

                          let badgeText = '';
                          const clickTarget: string | null = coupled || null;

                          if (isAm && coupled) {
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

                          // Badge text is transient guidance: show it only
                          // while the block is hovered or selected, and rely
                          // on the dashed coupling lines otherwise.
                          const badgeVisible =
                            selected || hoveredComponentName === component.name;
                          if (!badgeText || !badgeVisible) return null;

                          const badgeTransform = `translate(0, ${bottom + 12})`;

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
                                x={-Math.max(badgeText.length * 6.5 + 12, 36) / 2}
                                y={-8}
                                width={Math.max(badgeText.length * 6.5 + 12, 36)}
                                height={16}
                                rx={3}
                                style={{ fill: 'var(--pill-bg)' }}
                                stroke="rgba(56, 189, 248, 0.4)"
                                strokeWidth={1}
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

          {/* Active Software Coupling Guidelines (Gate Driver ➔ Switch, Ammeter ➔ Component) */}
          <g className="coupling-guides-layer" pointerEvents="none">
            {couplingPairs.map(({ sourceComp, targetComp, label, isHoveredOrSelected }, i) => {
              // Guides are selection feedback: draw nothing for idle blocks
              if (!isHoveredOrSelected) return null;
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
              const isAm = isAmmeterComponent(sourceComp);

              return (
                <g key={`coupling-${i}`} className="coupling-guide-group active">
                  <path
                    d={pathD}
                    fill="none"
                    stroke="rgba(56, 189, 248, 0.25)"
                    strokeWidth={8}
                    strokeLinecap="round"
                  />
                  <path
                    d={pathD}
                    fill="none"
                    stroke="#38bdf8"
                    strokeWidth={2.5}
                    strokeDasharray="6 4"
                    className="coupling-guideline active"
                    markerEnd={isAm ? "url(#coupling-dot)" : "url(#coupling-arrow)"}
                  />
                  <g transform={`translate(${(x1 + x2) / 2}, ${(y1 + y2) / 2})`}>
                    <rect
                      x={-badgeW / 2}
                      y={-10}
                      width={badgeW}
                      height={20}
                      rx={4}
                      style={{ fill: 'var(--pill-bg)' }}
                      stroke="var(--accent)"
                      strokeWidth={1}
                    />
                    <text x={0} y={3.5} textAnchor="middle" fill="var(--accent)" fontSize={9} fontWeight="bold">
                      {label}
                    </text>
                  </g>
                </g>
              );
            })}
          </g>

          {/* Reference Current Direction Hint for Ammeters (Amber arrow alongside measured component) */}
          <g className="current-direction-layer" pointerEvents="none">
            {currentDirectionHints.map((hint) => {
              const x1 = hint.arrowStart.x * dpix;
              const y1 = hint.arrowStart.y * dpix;
              const x2 = hint.arrowEnd.x * dpix;
              const y2 = hint.arrowEnd.y * dpix;
              const midX = (x1 + x2) / 2;
              const midY = (y1 + y2) / 2;
              const len = Math.hypot(x2 - x1, y2 - y1) || 1;
              const nx = -(y2 - y1) / len;
              const ny = (x2 - x1) / len;
              const labelX = midX + nx * 10;
              const labelY = midY + ny * 10;
              const labelText = `+i(${hint.target.name})`;
              const badgeW = labelText.length * 6.5 + 10;

              return (
                <g key={`current-dir-${hint.key}`} className="current-direction-hint active">
                  {/* Glow underlay */}
                  <line
                    x1={x1}
                    y1={y1}
                    x2={x2}
                    y2={y2}
                    stroke="rgba(245, 158, 11, 0.3)"
                    strokeWidth={7}
                    strokeLinecap="round"
                  />
                  {/* Direction arrow line */}
                  <line
                    x1={x1}
                    y1={y1}
                    x2={x2}
                    y2={y2}
                    stroke="#f59e0b"
                    strokeWidth={2.5}
                    markerEnd="url(#current-direction-arrow)"
                  />
                  {/* Direction badge tag */}
                  <g transform={`translate(${labelX}, ${labelY})`}>
                    <rect
                      x={-badgeW / 2}
                      y={-8}
                      width={badgeW}
                      height={16}
                      rx={3}
                      style={{ fill: 'var(--pill-bg)' }}
                      stroke="#f59e0b"
                      strokeWidth={1}
                    />
                    <text
                      x={0}
                      y={3.5}
                      textAnchor="middle"
                      fill="#f59e0b"
                      fontSize={9}
                      fontWeight="bold"
                    >
                      {labelText}
                    </text>
                  </g>
                </g>
              );
            })}
          </g>

          {/* Voltmeter ➔ measured-node / measured-component guides (hover/selection only) */}
          {voltmeterGuides.map(({ key, source, points, isHoveredOrSelected }) => {
            if (!isHoveredOrSelected || points.length === 0) return null;
            const x1 = source.position[0] * dpix;
            const y1 = source.position[1] * dpix;
            return (
              <g key={`voltmeter-guide-${key}`} className="coupling-guide-group active">
                {points.map((p, j) => {
                  const x2 = p.x * dpix;
                  const y2 = p.y * dpix;
                  const dx = x2 - x1;
                  const cx1 = x1 + dx * 0.4;
                  const cy1 = y1;
                  const cx2 = x1 + dx * 0.6;
                  const cy2 = y2;
                  const pathD = `M ${x1} ${y1} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${x2} ${y2}`;
                  const stroke = p.polarity === 'pos' ? '#f87171' : '#cd7f32';
                  const halo = p.polarity === 'pos' ? 'rgba(248, 113, 113, 0.25)' : 'rgba(205, 127, 50, 0.25)';
                  return (
                    <g key={`voltmeter-guide-${key}-${j}`}>
                      <path
                        d={pathD}
                        fill="none"
                        stroke={halo}
                        strokeWidth={8}
                        strokeLinecap="round"
                      />
                      <path
                        d={pathD}
                        fill="none"
                        stroke={stroke}
                        strokeWidth={2.5}
                        strokeDasharray="6 4"
                        className="coupling-guideline active"
                      />
                      {/* Polarity + / − indicator badge at target terminal */}
                      <g transform={`translate(${x2}, ${y2})`}>
                        <circle r={6.5} style={{ fill: 'var(--pill-bg)' }} stroke={stroke} strokeWidth={1.5} />
                        <text
                          x={0}
                          y={3.5}
                          textAnchor="middle"
                          fill={stroke}
                          fontSize={11}
                          fontWeight="bold"
                          fontFamily="monospace"
                        >
                          {p.polarity === 'pos' ? '+' : '−'}
                        </text>
                      </g>
                    </g>
                  );
                })}
              </g>
            );
          })}

          {/* Scope ➔ measurement-point guides (hover/selection only) */}
          {scopeGuides.map(({ key, lines, isHoveredOrSelected }) => {
            if (!isHoveredOrSelected || lines.length === 0) return null;
            return (
              <g key={`scope-guide-${key}`} className="coupling-guide-group active">
                {lines.map((line, j) => {
                  const x1 = line.origin.x * dpix;
                  const y1 = line.origin.y * dpix;
                  const x2 = line.target.x * dpix;
                  const y2 = line.target.y * dpix;
                  const dx = x2 - x1;
                  const cx1 = x1 + dx * 0.4;
                  const cy1 = y1;
                  const cx2 = x1 + dx * 0.6;
                  const cy2 = y2;
                  const pathD = `M ${x1} ${y1} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${x2} ${y2}`;
                  const color = channelColorByIndex(
                    line.colorIndex,
                    typeof document !== 'undefined' &&
                      document.documentElement.getAttribute('data-theme') === 'light'
                      ? 'light'
                      : 'dark',
                  );
                  const m = color.match(/^#(..)(..)(..)$/);
                  const halo = m
                    ? `rgba(${parseInt(m[1], 16)}, ${parseInt(m[2], 16)}, ${parseInt(m[3], 16)}, 0.22)`
                    : 'rgba(56, 189, 248, 0.22)';
                  const pillW = Math.max(line.text.length * 5.8 + 10, 30);
                  // keep the label inside the sheet when the target is at the edge
                  const pillX = Math.min(Math.max(x2, pillW / 2 + 2), 600 * dpix - pillW / 2 - 2);
                  return (
                    <g key={`scope-guide-${key}-${j}`}>
                      <path
                        d={pathD}
                        fill="none"
                        stroke={halo}
                        strokeWidth={8}
                        strokeLinecap="round"
                      />
                      <path
                        d={pathD}
                        fill="none"
                        stroke={color}
                        strokeWidth={2.5}
                        strokeDasharray="6 4"
                        className="coupling-guideline active"
                      />
                      <circle cx={x2} cy={y2} r={3.5} fill={color} />
                      <g transform={`translate(${pillX}, ${y2 - 12})`}>
                        <rect
                          x={-pillW / 2}
                          y={-8}
                          width={pillW}
                          height={15}
                          rx={7}
                          style={{ fill: 'var(--pill-bg)' }}
                          stroke={color}
                          strokeWidth={1}
                        />
                        <text
                          x={0}
                          y={3}
                          textAnchor="middle"
                          fill={color}
                          fontSize={8.5}
                          fontWeight="700"
                          fontFamily="var(--font-mono, monospace)"
                        >
                          {line.text}
                        </text>
                      </g>
                    </g>
                  );
                })}
              </g>
            );
          })}

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

              const shown = item.displayLabel || item.label;
              const rectW = Math.max(shown.length * 5.8 + 6, 14);
              const rectH = 13;
              const rectY = textY - 9.5;
              let rectX = textX - 3;
              if (anchor === 'end') {
                rectX = textX - rectW + 3;
              } else if (anchor === 'middle') {
                rectX = textX - rectW / 2;
              }

              const isNetHighlighted =
                activeNet !== null &&
                (activeNet.terminalKeys.has(`${item.gx},${item.gy}`) ||
                  (!!activeNet.label && activeNet.label === (item.displayLabel || item.label)));

              return (
                <g key={item.key} className={`terminal-net-label-pill${isNetHighlighted ? ' net-highlighted' : ''}`}>
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
                    {shown}
                  </text>
                </g>
              );
            })}
          </g>

          {/* Terminal Snap Hover Halo & Info Callout */}
          {hoveredTerminal && (
            <g className="hovered-terminal-callout" pointerEvents="none">
              <circle
                cx={hoveredTerminal.x * dpix}
                cy={hoveredTerminal.y * dpix}
                r={7}
                className="terminal-snap-halo"
              />
              {(hoveredTerminal.label || hoveredTerminal.desc) && (
                <g transform={`translate(${hoveredTerminal.x * dpix + 10}, ${hoveredTerminal.y * dpix - 20})`}>
                  <rect
                    x={-4}
                    y={-14}
                    width={Math.max(64, Math.min(260, (hoveredTerminal.label?.length ?? 0) * 8 + (hoveredTerminal.desc ? hoveredTerminal.desc.length * 5.8 : 0) + 16))}
                    height={hoveredTerminal.desc ? 30 : 20}
                    rx={4}
                    fill="#090d16"
                    stroke="var(--accent, #4ade80)"
                    strokeWidth={1.2}
                    filter="drop-shadow(0 3px 8px rgba(0,0,0,0.6))"
                  />
                  <text x={4} y={-1} fill="#4ade80" fontSize={11} fontWeight="bold" fontFamily="monospace">
                    {hoveredTerminal.label}
                  </text>
                  {hoveredTerminal.desc && (
                    <text x={4} y={11} fill="#94a3b8" fontSize={9.5} fontFamily="sans-serif">
                      {hoveredTerminal.desc.length > 38 ? hoveredTerminal.desc.slice(0, 36) + '…' : hoveredTerminal.desc}
                    </text>
                  )}
                </g>
              )}
            </g>
          )}

          {/* Focused Terminal indicator (Keyboard Tab navigation) */}
          {state.focusedTerminal && (
            <g
              className="focused-terminal-ring"
              transform={`translate(${state.focusedTerminal.x * dpix}, ${state.focusedTerminal.y * dpix})`}
              pointerEvents="none"
            >
              <circle r={8} fill="none" stroke="var(--accent)" strokeWidth={2} strokeDasharray="3 2" />
              <circle r={3} fill="var(--accent)" />
              <rect x={10} y={-8} width={24} height={15} rx={3} style={{ fill: 'var(--pill-bg)' }} stroke="var(--accent)" strokeWidth={1} />
              <text x={22} y={3} textAnchor="middle" fill="var(--accent)" fontSize={9} fontWeight="bold">
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
              {isTextboxComponent(state.ghost) ? (
                <rect
                  x={0}
                  y={0}
                  width={26 * dpix}
                  height={14 * dpix}
                  rx={8}
                  fill="rgba(56, 189, 248, 0.15)"
                  stroke="#38bdf8"
                  strokeWidth={1.5}
                  strokeDasharray="4 3"
                />
              ) : (
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
              )}
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

      {/* Horizontal CAD Scrollbar */}
      <div className="canvas-scrollbar canvas-scrollbar-horizontal" data-testid="canvas-scrollbar-h">
        <button
          type="button"
          className="scrollbar-stepper"
          onClick={() => setPan((p) => ({ ...p, x: p.x + 80 }))}
          title="Scroll Left"
          aria-label="Scroll Left"
          data-testid="canvas-scrollbar-h-left"
        >
          ◀
        </button>
        <div
          className="scrollbar-track"
          onClick={handleHTrackClick}
          data-testid="canvas-scrollbar-track-h"
        >
          <div
            className={`scrollbar-thumb ${scrollDrag?.axis === 'x' ? 'active' : ''}`}
            data-testid="canvas-scrollbar-thumb-h"
            style={{
              left: `${scrollMetrics.thumbLeft}px`,
              width: `${scrollMetrics.thumbWidth}px`,
            }}
            onMouseDown={(e) => {
              e.stopPropagation();
              e.preventDefault();
              setScrollDrag({
                axis: 'x',
                startMouse: e.clientX,
                startPan: pan.x,
              });
            }}
          />
        </div>
        <button
          type="button"
          className="scrollbar-stepper"
          onClick={() => setPan((p) => ({ ...p, x: p.x - 80 }))}
          title="Scroll Right"
          aria-label="Scroll Right"
          data-testid="canvas-scrollbar-h-right"
        >
          ▶
        </button>
      </div>

      {/* Vertical CAD Scrollbar */}
      <div className="canvas-scrollbar canvas-scrollbar-vertical" data-testid="canvas-scrollbar-v">
        <button
          type="button"
          className="scrollbar-stepper"
          onClick={() => setPan((p) => ({ ...p, y: p.y + 80 }))}
          title="Scroll Up"
          aria-label="Scroll Up"
          data-testid="canvas-scrollbar-v-up"
        >
          ▲
        </button>
        <div
          className="scrollbar-track"
          onClick={handleVTrackClick}
          data-testid="canvas-scrollbar-track-v"
        >
          <div
            className={`scrollbar-thumb ${scrollDrag?.axis === 'y' ? 'active' : ''}`}
            data-testid="canvas-scrollbar-thumb-v"
            style={{
              top: `${scrollMetrics.thumbTop}px`,
              height: `${scrollMetrics.thumbHeight}px`,
            }}
            onMouseDown={(e) => {
              e.stopPropagation();
              e.preventDefault();
              setScrollDrag({
                axis: 'y',
                startMouse: e.clientY,
                startPan: pan.y,
              });
            }}
          />
        </div>
        <button
          type="button"
          className="scrollbar-stepper"
          onClick={() => setPan((p) => ({ ...p, y: p.y - 80 }))}
          title="Scroll Down"
          aria-label="Scroll Down"
          data-testid="canvas-scrollbar-v-down"
        >
          ▼
        </button>
      </div>

      {/* Bottom-right Corner junction */}
      <div className="canvas-scrollbar-corner" data-testid="canvas-scrollbar-corner" />
    </div>
  );
}
