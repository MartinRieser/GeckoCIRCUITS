/**
 * Software coupling guides, measurement probe guidelines, and net-label resolution
 * helpers for the schematic sheet canvas.
 *
 * Provides pure domain calculations to render visual association paths between:
 * - Gate drivers and power switches (software gate coupling)
 * - Ammeters/Voltmeters and measured power components/nodes
 * - Scope channels and their measured circuit points (bare power nets or meter signals)
 */

import type { EditorComponent, EditorWire, Point } from '../model/types';
import { terminalPositions } from '../model/geometry';
import { isScopeComponent } from '../simulation/scopes';
import {
  isGateDriver,
  isAmmeterComponent,
  isVoltmeterComponent,
  getCoupledComponentName,
} from '../model/componentSchema';
import { CANVAS_METRICS, SENTINEL_UNSET } from '../model/constants';

/**
 * Visual coupling association between a control block (gate driver, ammeter,
 * or voltmeter) and its target power circuit component.
 */
export interface CouplingPair {
  /** Source component originating the coupling (e.g. gate driver or ammeter). */
  sourceComp: EditorComponent;
  /** Target component in the power circuit receiving the signal or being measured. */
  targetComp: EditorComponent;
  /** Human-readable badge text displayed along the guide curve. */
  label: string;
  /** True when either the source or target component is currently hovered or selected. */
  isHoveredOrSelected: boolean;
}

/**
 * Spatial coordinate of a measured voltage node along with its polarity indicator.
 */
export interface VoltmeterGuidePoint extends Point {
  /** Polarity tag: 'pos' for positive/anode node, 'neg' for negative/reference node. */
  polarity: 'pos' | 'neg';
}

/**
 * Visual guideline set originating from a voltmeter control block to the circuit
 * node terminals it measures.
 */
export interface VoltmeterGuide {
  /** Unique key identifying the guide set (component name). */
  key: string;
  /** Voltmeter source component. */
  source: EditorComponent;
  /** Resolved distinct target points on the schematic grid. */
  points: VoltmeterGuidePoint[];
  /** True when the voltmeter is currently hovered or selected. */
  isHoveredOrSelected: boolean;
}

/**
 * Individual guideline connecting a scope channel input pin to its measured circuit point.
 */
export interface ScopeGuideLine {
  /** Origin coordinate on the scope's input pin (in grid units). */
  origin: Point;
  /** Target coordinate of the measured wire point or component terminal (in grid units). */
  target: Point;
  /** Annotation text for the target badge (e.g. 'V(out)', 'I(L.1)'). */
  text: string;
  /** Scope channel index used to match the trace color palette. */
  colorIndex: number;
}

/**
 * Complete set of measurement guidelines for an oscilloscope instrument block.
 */
export interface ScopeGuide {
  /** Unique key identifying the scope guide set (scope component name). */
  key: string;
  /** Oscilloscope component instance. */
  source: EditorComponent;
  /** Individual channel measurement lines. */
  lines: ScopeGuideLine[];
  /** True when the scope is currently hovered or selected. */
  isHoveredOrSelected: boolean;
}

/**
 * Pre-computed terminal net-label item for canvas label rendering and hit testing.
 */
export interface TerminalLabelItem {
  /** Unique identifier for React list rendering. */
  key: string;
  /** Raw signal or net name assigned to the terminal. */
  label: string;
  /** Human-annotated display label (e.g. formatted channel info for scopes). */
  displayLabel: string;
  /** Terminal grid coordinate X. */
  gx: number;
  /** Terminal grid coordinate Y. */
  gy: number;
  /** Owning component name. */
  componentName: string;
  /** Normalized direction vector X from component origin to terminal. */
  dirX: number;
  /** Normalized direction vector Y from component origin to terminal. */
  dirY: number;
  /** True if any wire connects directly to this terminal. */
  isWired: boolean;
}

/**
 * Parameters passed to {@link buildScopeGuides} to resolve unwired channel measurement paths.
 */
export interface BuildScopeGuidesParams {
  /** All schematic components on the sheet. */
  components: EditorComponent[];
  /** All wire connections currently on the sheet. */
  wires: EditorWire[];
  /** Set of currently selected component names. */
  selection: string[];
  /** Name of the component currently hovered by the pointer, or null. */
  hoveredComponentName: string | null;
  /** Lookup map from electrical net label to first terminal grid point. */
  netTerminalPoints: Map<string, Point>;
  /** Lookup map from net label to all grid vertices along wires carrying that net. */
  netWirePoints: Map<string, Point[]>;
  /** Lookup map from emitted signal name to the control meter block producing it. */
  signalEmitters: Map<string, EditorComponent>;
  /** Lookup map from emitted signal name to formatted measurement description. */
  signalDescriptions: Map<string, string>;
  /** Maximum distance in grid units to treat a wire vertex as touching a terminal. */
  wireTouchTolerance?: number;
}

/**
 * Determines whether a given component terminal pin is physically connected to a wire vertex.
 *
 * @param terminal Grid coordinate of the terminal pin.
 * @param wires All wires in the circuit.
 * @param tolerance Maximum Euclidean distance threshold (grid units). Defaults to `CANVAS_METRICS.TERMINAL_TOUCH_TOLERANCE`.
 * @returns True if at least one wire has a vertex within the tolerance distance.
 */
export function isTerminalWired(
  terminal: Point,
  wires: EditorWire[],
  tolerance: number = CANVAS_METRICS.TERMINAL_TOUCH_TOLERANCE,
): boolean {
  return wires.some((w) =>
    w.points.some((p) => Math.hypot(p[0] - terminal.x, p[1] - terminal.y) < tolerance),
  );
}

/**
 * Builds coupling guideline pairs between control blocks (gate drivers, ammeters,
 * voltmeters with explicit target) and their target circuit components.
 *
 * @param components All components on the sheet.
 * @param selection Currently selected component names.
 * @param hoveredComponentName Name of component currently under the pointer.
 * @returns Array of active coupling pairs with labels and visibility flags.
 */
export function buildCouplingPairs(
  components: EditorComponent[],
  selection: string[],
  hoveredComponentName: string | null,
): CouplingPair[] {
  const pairs: CouplingPair[] = [];

  for (const comp of components) {
    const isVmWithTarget =
      isVoltmeterComponent(comp) && !!getCoupledComponentName(comp);
    if (isGateDriver(comp) || isAmmeterComponent(comp) || isVmWithTarget) {
      const targetName = getCoupledComponentName(comp);
      if (targetName) {
        const target = components.find((c) => c.name === targetName);
        if (target) {
          const isHoveredOrSelected =
            selection.includes(comp.name) ||
            selection.includes(target.name) ||
            hoveredComponentName === comp.name ||
            hoveredComponentName === target.name;
          const label = isGateDriver(comp)
            ? 'GATE DRIVE ➔'
            : isVmWithTarget
              ? 'MEASURE U ➔'
              : 'MEASURE I ➔';
          pairs.push({ sourceComp: comp, targetComp: target, label, isHoveredOrSelected });
        }
      }
    }
  }

  return pairs;
}

/**
 * Maps each electrical net label to its first encountered terminal coordinate on the grid.
 * Ignores control components and unset sentinel labels.
 *
 * @param components All components on the sheet.
 * @returns Map from net label to representative terminal point.
 */
export function buildNetTerminalPoints(components: EditorComponent[]): Map<string, Point> {
  const map = new Map<string, Point>();

  for (const comp of components) {
    if (comp.family === 'CONTROL') continue;
    const terminals = terminalPositions(comp);
    terminals.input.forEach((t, i) => {
      const raw = comp.inputLabels?.[i]?.trim();
      if (raw && raw !== SENTINEL_UNSET && !map.has(raw)) map.set(raw, t);
    });
    terminals.output.forEach((t, i) => {
      const raw = comp.outputLabels?.[i]?.trim();
      if (raw && raw !== SENTINEL_UNSET && !map.has(raw)) map.set(raw, t);
    });
  }

  return map;
}

/**
 * Builds voltmeter guideline paths connecting voltmeter blocks to the measured electrical nodes.
 *
 * @param components All components on the sheet.
 * @param selection Currently selected component names.
 * @param hoveredComponentName Name of component currently under the pointer.
 * @param netTerminalPoints Lookup map of electrical net terminal locations.
 * @returns Array of voltmeter guides for each voltmeter block.
 */
export function buildVoltmeterGuides(
  components: EditorComponent[],
  selection: string[],
  hoveredComponentName: string | null,
  netTerminalPoints: Map<string, Point>,
): VoltmeterGuide[] {
  const guides: VoltmeterGuide[] = [];

  for (const comp of components) {
    if (!isVoltmeterComponent(comp)) continue;
    const nodeA =
      (comp.parameters?.nodeA as string) || (comp.parameters?.positiveNode as string);
    const nodeB =
      (comp.parameters?.nodeB as string) || (comp.parameters?.negativeNode as string);

    const points = (
      [
        { node: nodeA, polarity: 'pos' as const },
        { node: nodeB, polarity: 'neg' as const },
      ] as const
    )
      .filter(({ node }) => !!node)
      .map(({ node, polarity }) => ({ point: netTerminalPoints.get(node), polarity }))
      .filter((e): e is { point: Point; polarity: 'pos' | 'neg' } => !!e.point)
      .map(({ point, polarity }) => ({ ...point, polarity }));

    // If both nodes resolve to the same coordinate, deduplicate to a single guide
    const unique = new Map(points.map((p) => [`${p.x},${p.y}`, p]));

    guides.push({
      key: comp.name,
      source: comp,
      points: [...unique.values()],
      isHoveredOrSelected:
        selection.includes(comp.name) || hoveredComponentName === comp.name,
    });
  }

  return guides;
}

/**
 * Builds a lookup from emitted meter signal name to the control meter component that produces it.
 *
 * @param components All components on the sheet.
 * @returns Map from signal name to emitter component.
 */
export function buildSignalEmitters(components: EditorComponent[]): Map<string, EditorComponent> {
  const map = new Map<string, EditorComponent>();

  for (const comp of components) {
    if (comp.family !== 'CONTROL') continue;
    if (!isVoltmeterComponent(comp) && !isAmmeterComponent(comp)) continue;
    for (const l of comp.outputLabels || []) {
      const raw = l?.trim();
      if (raw && raw !== SENTINEL_UNSET && !map.has(raw)) map.set(raw, comp);
    }
  }

  return map;
}

/**
 * Builds human-readable measurement descriptions for meter-emitted signals
 * (e.g. 'I(L.1)', 'V(out)', 'V(nodeA-nodeB)').
 *
 * @param components All components on the sheet.
 * @returns Map from signal name to formatted description.
 */
export function buildSignalDescriptions(components: EditorComponent[]): Map<string, string> {
  const map = new Map<string, string>();

  for (const comp of components) {
    if (comp.family !== 'CONTROL') continue;
    const isVm = isVoltmeterComponent(comp);
    const isAm = isAmmeterComponent(comp);
    if (!isVm && !isAm) continue;

    const coupled = getCoupledComponentName(comp);
    const nodeA =
      (comp.parameters?.nodeA as string) || (comp.parameters?.positiveNode as string);
    const nodeB =
      (comp.parameters?.nodeB as string) || (comp.parameters?.negativeNode as string);

    let detail: string | null = null;
    if (coupled) {
      detail = coupled;
    } else if (isVm && nodeA) {
      detail = nodeB && nodeB !== '0' ? `${nodeA}-${nodeB}` : nodeA;
    }

    if (!detail) continue;
    const prefix = isAm ? 'I' : 'V';

    for (const l of comp.outputLabels || []) {
      const raw = l?.trim();
      if (raw && raw !== SENTINEL_UNSET) {
        map.set(raw, `${prefix}(${detail})`);
      }
    }
  }

  return map;
}

/**
 * Aggregates all grid points along wires carrying a given net label.
 *
 * @param wires All wires in the schematic.
 * @returns Map from net label to list of wire vertex coordinates.
 */
export function buildNetWirePoints(wires: EditorWire[]): Map<string, Point[]> {
  const map = new Map<string, Point[]>();

  for (const w of wires) {
    const label = w.label?.trim();
    if (!label || label === SENTINEL_UNSET) continue;
    let pts = map.get(label);
    if (!pts) {
      pts = [];
      map.set(label, pts);
    }
    for (const p of w.points) {
      pts.push({ x: p[0], y: p[1] });
    }
  }

  return map;
}

/**
 * Finds the nearest point along a net's wiring or terminals relative to a reference coordinate.
 *
 * @param label Net name to search for.
 * @param from Reference coordinate (e.g. component position).
 * @param netWirePoints Map of wire points per net.
 * @param netTerminalPoints Map of terminal points per net.
 * @returns Nearest grid point along the net, or undefined if net is unknown.
 */
export function findNearestPointOnNet(
  label: string,
  from: Point,
  netWirePoints: Map<string, Point[]>,
  netTerminalPoints: Map<string, Point>,
): Point | undefined {
  const wirePts = netWirePoints.get(label);
  if (wirePts && wirePts.length > 0) {
    let best = wirePts[0];
    let bestD = Infinity;
    for (const p of wirePts) {
      const d = (p.x - from.x) ** 2 + (p.y - from.y) ** 2;
      if (d < bestD) {
        bestD = d;
        best = p;
      }
    }
    return best;
  }
  return netTerminalPoints.get(label);
}

/**
 * Builds measurement guidelines from oscilloscope input channels to their measured circuit points.
 *
 * Automatically resolves:
 * 1. Physically wired channels: skipped (physical wire already visible).
 * 2. Bare power net names: aims at the nearest point along the net's wire rail or terminal.
 * 3. Ammeter emitted signals: aims at the coupled power inductor/switch or ammeter block.
 * 4. Voltmeter emitted signals: aims at the measured node terminals or coupled component.
 *
 * @param params Construction parameters including circuit components, wires, and pre-computed lookups.
 * @returns Array of scope guides per oscilloscope block.
 */
export function buildScopeGuides({
  components,
  wires,
  selection,
  hoveredComponentName,
  netTerminalPoints,
  netWirePoints,
  signalEmitters,
  signalDescriptions,
  wireTouchTolerance = CANVAS_METRICS.TERMINAL_TOUCH_TOLERANCE,
}: BuildScopeGuidesParams): ScopeGuide[] {
  const guides: ScopeGuide[] = [];

  for (const comp of components) {
    if (!isScopeComponent(comp)) continue;

    const lines: ScopeGuideLine[] = [];
    const terminals = terminalPositions(comp);

    terminals.input.forEach((t, i) => {
      const raw = comp.inputLabels?.[i]?.trim();
      if (!raw || raw === SENTINEL_UNSET) return;

      // Physically wired pins need no dashed guide
      if (isTerminalWired(t, wires, wireTouchTolerance)) return;

      // Bare power net: aim at the nearest point of the net's wiring or terminal
      const netPoint = findNearestPointOnNet(
        raw,
        { x: comp.position[0], y: comp.position[1] },
        netWirePoints,
        netTerminalPoints,
      );

      if (netPoint) {
        lines.push({
          origin: t,
          target: netPoint,
          text: `V(${raw})`,
          colorIndex: i,
        });
        return;
      }

      // Meter-emitted signal: aim at the meter's measurement point
      const emitter = signalEmitters.get(raw);
      if (!emitter) return;

      const text = signalDescriptions.get(raw) ?? raw;

      if (isAmmeterComponent(emitter)) {
        const targetName = getCoupledComponentName(emitter);
        const targetComp = targetName
          ? components.find((c) => c.name === targetName)
          : undefined;
        const target = targetComp ?? emitter;
        lines.push({
          origin: t,
          target: { x: target.position[0], y: target.position[1] },
          text,
          colorIndex: i,
        });
      } else if (isVoltmeterComponent(emitter)) {
        const nodeA =
          (emitter.parameters?.nodeA as string) || (emitter.parameters?.positiveNode as string);
        const nodeB =
          (emitter.parameters?.nodeB as string) || (emitter.parameters?.negativeNode as string);
        const nodePoints = [nodeA, nodeB]
          .map((n) => (n ? netTerminalPoints.get(n) : undefined))
          .filter((p): p is Point => !!p);

        let target: Point | undefined = nodePoints[0];
        if (!target) {
          const targetName = getCoupledComponentName(emitter);
          const targetComp = targetName
            ? components.find((c) => c.name === targetName)
            : undefined;
          const fallback = targetComp ?? emitter;
          target = { x: fallback.position[0], y: fallback.position[1] };
        }

        lines.push({
          origin: t,
          target,
          text,
          colorIndex: i,
        });
      }
    });

    guides.push({
      key: comp.name,
      source: comp,
      lines,
      isHoveredOrSelected:
        selection.includes(comp.name) || hoveredComponentName === comp.name,
    });
  }

  return guides;
}

/**
 * Resolves the user-facing annotation string for a component terminal label.
 * For scopes, translates raw signal identifiers into human-readable measurement descriptors.
 *
 * @param component Owning component.
 * @param rawLabel Raw label string.
 * @param signalDescriptions Lookup map of signal descriptions.
 * @param netTerminalPoints Lookup map of known electrical net terminals.
 * @returns Human-readable channel descriptor or raw label string.
 */
export function resolveChannelDisplayLabel(
  component: EditorComponent,
  rawLabel: string,
  signalDescriptions: Map<string, string>,
  netTerminalPoints: Map<string, Point>,
): string {
  if (!isScopeComponent(component)) return rawLabel;
  return (
    signalDescriptions.get(rawLabel) ??
    (netTerminalPoints.has(rawLabel) ? `V(${rawLabel})` : rawLabel)
  );
}

/**
 * Pre-computes, deduplicates, and checks wiring state for all component terminal net labels across the sheet.
 *
 * @param components All schematic components.
 * @param wires All circuit wires.
 * @param signalDescriptions Map of signal descriptions for scope channel annotations.
 * @param netTerminalPoints Map of net terminal points.
 * @param wireTouchTolerance Coincidence distance threshold. Defaults to `CANVAS_METRICS.TERMINAL_TOUCH_TOLERANCE`.
 * @returns Deduplicated list of terminal label items.
 */
export function buildTerminalLabelItems(
  components: EditorComponent[],
  wires: EditorWire[],
  signalDescriptions: Map<string, string>,
  netTerminalPoints: Map<string, Point>,
  wireTouchTolerance: number = CANVAS_METRICS.TERMINAL_TOUCH_TOLERANCE,
): TerminalLabelItem[] {
  const items: TerminalLabelItem[] = [];
  const seen = new Set<string>();

  for (const component of components) {
    const terminals = terminalPositions(component);
    const inLabels = component.inputLabels || [];
    const outLabels = component.outputLabels || [];

    terminals.input.forEach((t, i) => {
      const raw = inLabels[i]?.trim();
      if (!raw || raw === SENTINEL_UNSET) return;
      const pointKey = `${t.x},${t.y}:${raw}`;
      if (seen.has(pointKey)) return;
      seen.add(pointKey);
      const isWired = isTerminalWired(t, wires, wireTouchTolerance);
      items.push({
        key: `in-${component.name}-${i}-${raw}`,
        label: raw,
        displayLabel: resolveChannelDisplayLabel(
          component,
          raw,
          signalDescriptions,
          netTerminalPoints,
        ),
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
      if (!raw || raw === SENTINEL_UNSET) return;
      const pointKey = `${t.x},${t.y}:${raw}`;
      if (seen.has(pointKey)) return;
      seen.add(pointKey);
      const isWired = isTerminalWired(t, wires, wireTouchTolerance);
      items.push({
        key: `out-${component.name}-${i}-${raw}`,
        label: raw,
        displayLabel: raw,
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
}
