/**
 * Client-side circuit validation used for wire-commit feedback and the
 * pre-simulation check in the Simulation tab. Pure functions, no React.
 *
 * Surfaces missing sources, unsupported stamped components, dangling open power
 * leads, overlapping wire geometry, and accidental terminal shorts before runs.
 */
import { allTerminals, terminalPositions } from './geometry';
import { denseCellsOf } from '../canvas/WireRouter';
import { LkComponentType, SENTINEL_UNSET } from './constants';
import {
  isVoltmeterComponent,
  isAmmeterComponent,
  isGateDriver,
  getCoupledComponentName,
  parseEngineeringValue,
} from './componentSchema';
import {
  detectMaxCircuitFrequency,
  suggestAutoTimeStep,
  type FrequencyInspectableComponent,
} from '../simulation/simSettings';
import { estimateStepCount, STEP_WARNING_THRESHOLD } from '../simulation/simSteps';
import { ENGINE_UNREACHABLE_PREFIX } from '../api/client';

/**
 * Component type numbers the core engine can currently stamp and solve.
 */
export const SIMULATED_LK_TYPES: ReadonlySet<number> = new Set([
  LkComponentType.RESISTOR,
  LkComponentType.INDUCTOR,
  LkComponentType.CAPACITOR,
  LkComponentType.VOLTAGE_SOURCE,
  LkComponentType.CURRENT_SOURCE,
  LkComponentType.DIODE,
  LkComponentType.IDEAL_SWITCH,
  LkComponentType.THYRISTOR,
  LkComponentType.IGBT,
  // Type 9 couples two type-12 inductors through the mutual-coupling
  // registry (M = k*sqrt(L1*L2)) — it IS simulated, only pinless.
  LkComponentType.MUTUAL_INDUCTANCE,
  LkComponentType.COUPLED_INDUCTOR,
  LkComponentType.TRANSFORMER,
  LkComponentType.RELUCTANCE,
  LkComponentType.MMF,
  LkComponentType.MOSFET,
  LkComponentType.GLOBAL_TERMINAL,
  LkComponentType.BJT,
  LkComponentType.THERMAL_FLOW,
  LkComponentType.THERMAL_TEMP,
  LkComponentType.THERMAL_RTH,
  LkComponentType.THERMAL_CTH,
]);

/**
 * Component types acting as energy sources in the schematic.
 */
const SOURCE_COMPONENT_TYPES: ReadonlySet<number> = new Set([
  LkComponentType.VOLTAGE_SOURCE,
  LkComponentType.CURRENT_SOURCE,
  LkComponentType.MMF,
  LkComponentType.THERMAL_TEMP,
  LkComponentType.THERMAL_FLOW,
]);

/** Maximum number of individual wire warnings to display before capping summary output. */
const MAX_WIRE_WARNINGS_CAP = 6;

export interface WireLike {
  points?: number[][];
  type?: string;
}

export interface ComponentLike {
  name: string;
  /** Optional palette display name (present on palette-augmented entries). */
  displayName?: string;
  type: number;
  family: string;
  position: number[];
  orientation: number;
  inputLabels: string[];
  outputLabels: string[];
  inputs?: unknown[];
  parameters: Record<string, number | string | boolean>;
}

/**
 * Checks whether a given grid raster coordinate connects directly to a component
 * terminal or coincides with any existing wire raster point.
 *
 * @param point Coordinates to test
 * @param components Placed schematic components
 * @param wires Placed connection wires
 * @returns True if point is incident on a terminal or wire conductor
 */
export function isWireEndPointConnected(
  point: { x: number; y: number },
  components: ComponentLike[],
  wires: WireLike[],
): boolean {
  const onTerminal = allTerminals(components).some(
    (t) => t.point.x === point.x && t.point.y === point.y,
  );
  if (onTerminal) return true;
  return pointOnAnyWire(point, wires);
}

/**
 * Checks if a coordinate lies on any raster point of any wire in the list.
 */
function pointOnAnyWire(
  point: { x: number; y: number },
  wires: WireLike[],
  exclude?: { wireIndex: number; pointIndex: number },
): boolean {
  return wires.some((w, wireIndex) =>
    (w.points ?? []).some((pt, pointIndex) => {
      if (
        exclude &&
        wireIndex === exclude.wireIndex &&
        pointIndex === exclude.pointIndex
      ) {
        return false;
      }
      return pt[0] === point.x && pt[1] === point.y;
    }),
  );
}

/** Human label for a component, matching the palette naming. */
function labelOf(comp: ComponentLike): string {
  const withMeta = comp as ComponentLike & { displayName?: string };
  return withMeta.displayName ?? comp.name;
}

/**
 * Inspects wires for geometric flaws that distort schematic topology:
 * 1. Overlap: two wires sharing >= 2 raster cells drawn as one ambiguous line.
 * 2. Hidden terminal short: a wire interior traversing a terminal explicitly wired by another wire.
 *
 * @param components Placed schematic components
 * @param wires Placed connection wires
 * @returns Array of warning strings, or empty array if clean
 */
export function findWireGeometryWarnings(
  components: ComponentLike[],
  wires: WireLike[],
): string[] {
  const found: string[] = [];
  const cellsPerWire = wires.map((w) => denseCellsOf(w.points ?? []));
  const fmtKey = (key: string) => key.replace(',', ', ');

  // Helper to locate the nearest named component so coordinates are human-actionable
  const findNearbyContext = (coordKey: string): string => {
    const [x, y] = coordKey.split(',').map(Number);
    let bestComp: ComponentLike | null = null;
    let bestDist = Infinity;
    for (const c of components) {
      const pos = c.position;
      if (!pos || pos.length < 2) continue;
      const dist = Math.hypot(pos[0] - x, pos[1] - y);
      if (dist < bestDist) {
        bestDist = dist;
        bestComp = c;
      }
    }
    if (bestComp && bestDist <= 4) {
      return `near '${bestComp.name}' `;
    }
    return '';
  };

  // 1. Pairwise overlap: >= 2 shared cells means the wires run along each other
  for (let i = 0; i < wires.length; i++) {
    for (let j = i + 1; j < wires.length; j++) {
      let shared = 0;
      let first: string | null = null;
      for (const key of cellsPerWire[j]) {
        if (cellsPerWire[i].has(key)) {
          shared += 1;
          if (!first) first = key;
        }
      }
      if (shared >= 2) {
        const near = findNearbyContext(first as string);
        found.push(
          `Wires ${i + 1} and ${j + 1} overlap ${near}across ${shared} cells from (${fmtKey(first as string)}) — two separate connections are drawn as one line (duplicate wire segment)`,
        );
      }
    }
  }

  // 2. A wire interior passing through a terminal that another wire explicitly wires
  const endpointWires = new Map<string, number[]>(); // "x,y" -> wire indices ending there
  wires.forEach((w, i) => {
    const pts = w.points ?? [];
    if (pts.length === 0) return;
    for (const end of [pts[0], pts[pts.length - 1]]) {
      const key = `${end[0]},${end[1]}`;
      const list = endpointWires.get(key) ?? [];
      list.push(i);
      endpointWires.set(key, list);
    }
  });

  for (let i = 0; i < wires.length; i++) {
    const pts = wires[i].points ?? [];
    if (pts.length < 2) continue;
    const ownEnds = new Set([
      `${pts[0][0]},${pts[0][1]}`,
      `${pts[pts.length - 1][0]},${pts[pts.length - 1][1]}`,
    ]);
    for (const t of allTerminals(components)) {
      const key = `${t.point.x},${t.point.y}`;
      if (ownEnds.has(key) || !cellsPerWire[i].has(key)) continue;
      const wiredBy = (endpointWires.get(key) ?? []).filter((v) => v !== i);
      if (wiredBy.length > 0) {
        found.push(
          `Wire ${i + 1} passes through the ${t.component} terminal at (${fmtKey(key)}) that wire ${wiredBy[0] + 1} also connects — this may short two nets`,
        );
      }
    }
  }

  if (found.length > MAX_WIRE_WARNINGS_CAP) {
    return [
      ...found.slice(0, MAX_WIRE_WARNINGS_CAP),
      `…and ${found.length - MAX_WIRE_WARNINGS_CAP} more wire geometry warnings`,
    ];
  }
  return found;
}

/** Severity level of a circuit or simulation diagnostic entry. */
export type DiagnosticSeverity = 'error' | 'warning' | 'info';

/** Logical origin category of a diagnostic entry. */
export type DiagnosticCategory = 'engine' | 'drc' | 'timing';

/** Actionable 1-click quick fix descriptor attached to a diagnostic item. */
export interface DiagnosticQuickFix {
  /** Human-readable action button label shown in the Diagnostics Drawer. */
  label: string;
  /** Quick-fix handler action identifier. */
  action: 'auto-dt' | 'open-sim-config' | 'select-component' | 'delete-wire' | 'focus-wire';
  /** Target component name for `select-component` actions. */
  componentName?: string;
  /** Target 0-based wire index for `delete-wire` or `focus-wire` actions. */
  wireIndex?: number;
  /** Suggested time step string for `auto-dt` actions. */
  suggestedDt?: string;
}

/** Structured circuit or simulation diagnostic record surfaced in badges, warnings, and the bottom drawer. */
export interface DiagnosticItem {
  /** Stable unique identifier within a diagnostic snapshot. */
  id: string;
  /** Severity classification (`error`, `warning`, or `info`). */
  severity: DiagnosticSeverity;
  /** Diagnostic origin category (`engine`, `drc`, or `timing`). */
  category: DiagnosticCategory;
  /** Short headline summary. */
  title: string;
  /** Detailed actionable explanation shown in the drawer row. */
  detail: string;
  /** Optional related component name for click-to-locate navigation. */
  componentName?: string;
  /** Optional related wire index for click-to-locate navigation. */
  wireIndex?: number;
  /** Optional schematic grid coordinate to pan/center onto when located. */
  coord?: { x: number; y: number };
  /** Optional 1-click remediation action. */
  quickFix?: DiagnosticQuickFix;
}

/** Runtime timing and simulation result context passed into `buildCircuitDiagnostics()`. */
export interface DiagnosticsContext {
  /** Active time-step engineering string (e.g. `'1u'`). */
  dt?: string;
  /** Active simulation duration engineering string (e.g. `'20m'`). */
  tEnd?: string;
  /** Current simulation run status. */
  simStatus?: string;
  /** Last simulation engine error message, if any. */
  simErrorMessage?: string | null;
  /** Post-run warnings emitted by the simulation engine. */
  simWarnings?: string[];
}

interface DanglingWireIssue {
  wireIndex: number;
  x: number;
  y: number;
  where: string;
  isControl: boolean;
}

interface UncoupledControlIssue {
  comp: ComponentLike;
  kind: 'gate' | 'ammeter' | 'voltmeter';
  reason: string;
}

interface StructuralValidationData {
  danglingPower: DanglingWireIssue[];
  danglingControl: DanglingWireIssue[];
  hasElectricalSource: boolean;
  unsupported: ComponentLike[];
  wireGeometryWarnings: string[];
  unknownNodes: Array<{ comp: ComponentLike; label: string; node: string }>;
  danglingCouplings: Array<{ comp: ComponentLike; target: string }>;
  uncoupledControls: UncoupledControlIssue[];
  knownNets: Set<string>;
}

/** Collects known named circuit nets from component pins and wire labels. */
function collectKnownNets(components: ComponentLike[], wires: WireLike[]): Set<string> {
  const knownNets = new Set<string>(['0', 'gnd', 'ground']);
  for (const c of components) {
    for (const l of [...(c.inputLabels ?? []), ...(c.outputLabels ?? [])]) {
      const raw = l?.trim();
      if (raw && raw !== SENTINEL_UNSET) knownNets.add(raw.toLowerCase());
    }
  }
  for (const w of wires) {
    const raw = w.type === 'CONTROL' ? undefined : (w as { label?: string }).label?.trim();
    if (raw && raw !== SENTINEL_UNSET) knownNets.add(raw.toLowerCase());
  }
  return knownNets;
}

/**
 * Returns a concise on-canvas/DRC warning string if a component has a missing
 * or broken coupling/node binding, or `null` when clean.
 */
export function getComponentDiagnosticWarning(
  comp: ComponentLike,
  knownComponentNames: ReadonlySet<string>,
  knownNets?: ReadonlySet<string>,
): string | null {
  const coupledRaw = getCoupledComponentName(comp as unknown as Parameters<typeof getCoupledComponentName>[0]);
  const coupled = coupledRaw && coupledRaw !== SENTINEL_UNSET ? coupledRaw : '';

  if (coupled && !knownComponentNames.has(coupled)) {
    return `Coupled target '${coupled}' does not exist (deleted or renamed).`;
  }

  if (isGateDriver(comp as unknown as Parameters<typeof isGateDriver>[0]) && !coupled) {
    return `Gate driver '${comp.name}' has no coupled switch assigned — gate pulses have no target.`;
  }

  if (isAmmeterComponent(comp) && !coupled) {
    return `Ammeter '${comp.name}' has no coupled component assigned — reads 0 A.`;
  }

  if (isVoltmeterComponent(comp) && !coupled) {
    const nodeA = String(comp.parameters?.nodeA ?? comp.parameters?.positiveNode ?? '').trim();
    const nodeB = String(comp.parameters?.nodeB ?? comp.parameters?.negativeNode ?? '').trim();
    const hasNodeA = Boolean(nodeA && nodeA !== SENTINEL_UNSET);
    const hasNodeB = Boolean(nodeB && nodeB !== SENTINEL_UNSET);
    if (!hasNodeA && !hasNodeB) {
      return `Voltmeter '${comp.name}' has no coupled component or node labels assigned — reads 0 V.`;
    }
    if (knownNets) {
      for (const node of [nodeA, nodeB]) {
        if (node && node !== SENTINEL_UNSET && !knownNets.has(node.toLowerCase())) {
          return `Voltmeter '${comp.name}' references non-existent net '${node}' (reads 0 V).`;
        }
      }
    }
  }

  return null;
}

/**
 * Runs shared structural Design Rule Checks across schematic components and wires.
 */
function collectStructuralValidation(
  components: ComponentLike[],
  wires: WireLike[],
): StructuralValidationData {
  const terminals = allTerminals(components);
  const danglingPower: DanglingWireIssue[] = [];
  const danglingControl: DanglingWireIssue[] = [];

  wires.forEach((w, index) => {
    const pts = w.points ?? [];
    if (pts.length === 0) return;
    const isControl = w.type === 'CONTROL';
    const ends: Array<{ x: number; y: number; pointIndex: number }> = [
      { x: pts[0][0], y: pts[0][1], pointIndex: 0 },
      { x: pts[pts.length - 1][0], y: pts[pts.length - 1][1], pointIndex: pts.length - 1 },
    ];
    for (const end of ends) {
      const onTerminal = terminals.some((t) => t.point.x === end.x && t.point.y === end.y);
      const onWire = pointOnAnyWire(end, wires, {
        wireIndex: index,
        pointIndex: end.pointIndex,
      });
      if (!onTerminal && !onWire) {
        const issue: DanglingWireIssue = {
          wireIndex: index,
          x: end.x,
          y: end.y,
          where: `wire ${index + 1} ends free at (${end.x}, ${end.y})`,
          isControl,
        };
        (isControl ? danglingControl : danglingPower).push(issue);
        break;
      }
    }
  });

  const hasElectricalSource = components.some((c) => SOURCE_COMPONENT_TYPES.has(c.type));

  const unsupported = components.filter(
    (c) =>
      c.family !== 'CONTROL' &&
      c.family !== 'SPECIAL' &&
      c.family !== 'ANNOTATION' &&
      c.type !== 2001 &&
      !SIMULATED_LK_TYPES.has(c.type),
  );

  const wireGeometryWarnings = findWireGeometryWarnings(components, wires);
  const knownNets = collectKnownNets(components, wires);

  const unknownNodes: Array<{ comp: ComponentLike; label: string; node: string }> = [];
  for (const c of components) {
    if (c.family !== 'CONTROL') continue;
    if (c.type !== 1001 && c.type !== 1) continue; // voltmeter (new + legacy numbering)
    if (c.parameters?.coupledComponent) continue; // measures across a component instead
    for (const key of ['nodeA', 'nodeB', 'positiveNode', 'negativeNode']) {
      const node = String(c.parameters?.[key] ?? '').trim();
      if (
        node &&
        node !== SENTINEL_UNSET &&
        !knownNets.has(node.toLowerCase()) &&
        !unknownNodes.some((u) => u.label.includes(`'${node}'`))
      ) {
        unknownNodes.push({ comp: c, label: `${c.name}: '${node}'`, node });
      }
    }
  }

  const knownNames = new Set(components.map((c) => c.name));
  const danglingCouplings: Array<{ comp: ComponentLike; target: string }> = [];
  const uncoupledControls: UncoupledControlIssue[] = [];

  for (const c of components) {
    const target = String(c.parameters?.coupledComponent ?? '').trim();
    if (target && target !== SENTINEL_UNSET && !knownNames.has(target)) {
      danglingCouplings.push({ comp: c, target });
      continue;
    }

    const coupled = target && target !== SENTINEL_UNSET ? target : '';
    if (isGateDriver(c as unknown as Parameters<typeof isGateDriver>[0]) && !coupled) {
      uncoupledControls.push({
        comp: c,
        kind: 'gate',
        reason: getComponentDiagnosticWarning(c, knownNames, knownNets) ?? '',
      });
    } else if (isAmmeterComponent(c) && !coupled) {
      uncoupledControls.push({
        comp: c,
        kind: 'ammeter',
        reason: getComponentDiagnosticWarning(c, knownNames, knownNets) ?? '',
      });
    } else if (isVoltmeterComponent(c) && !coupled) {
      const nodeA = String(c.parameters?.nodeA ?? c.parameters?.positiveNode ?? '').trim();
      const nodeB = String(c.parameters?.nodeB ?? c.parameters?.negativeNode ?? '').trim();
      if ((!nodeA || nodeA === SENTINEL_UNSET) && (!nodeB || nodeB === SENTINEL_UNSET)) {
        uncoupledControls.push({
          comp: c,
          kind: 'voltmeter',
          reason: getComponentDiagnosticWarning(c, knownNames, knownNets) ?? '',
        });
      }
    }
  }

  return {
    danglingPower,
    danglingControl,
    hasElectricalSource,
    unsupported,
    wireGeometryWarnings,
    unknownNodes,
    danglingCouplings,
    uncoupledControls,
    knownNets,
  };
}

/**
 * Pre-simulation validation pass executed before dispatching a simulation run.
 * Returns human-readable warnings; an empty array indicates a clean, runnable circuit.
 *
 * @param components Placed schematic components
 * @param wires Placed connection wires
 * @returns Array of warning strings
 */
export function validateCircuitForSimulation(
  components: ComponentLike[],
  wires: WireLike[],
): string[] {
  const warnings: string[] = [];

  if (components.length === 0) {
    warnings.push('The sheet is empty — place components and wire them up first.');
    return warnings;
  }

  const data = collectStructuralValidation(components, wires);

  if (data.danglingPower.length > 0) {
    warnings.push(
      `Unconnected wire end${data.danglingPower.length > 1 ? 's' : ''} (open circuit): ${data.danglingPower.map((d) => d.where).join('; ')}.`,
    );
  }
  if (data.danglingControl.length > 0) {
    warnings.push(
      `Control wire${data.danglingControl.length > 1 ? 's' : ''} with a free end (${data.danglingControl.map((d) => d.where).join('; ')}): ` +
        'scope and control inputs bind by signal name, so the channel still records — ' +
        'the wire only matters when you want the pin physically wired. Reconnect or delete it.',
    );
  }

  if (!data.hasElectricalSource) {
    warnings.push(
      'No source found (voltage source, current source, MMF or thermal source) — all signals will stay zero.',
    );
  }

  if (data.unsupported.length > 0) {
    const counts = new Map<string, number>();
    for (const c of data.unsupported) {
      counts.set(labelOf(c), (counts.get(labelOf(c)) ?? 0) + 1);
    }
    const summary = [...counts.entries()]
      .map(([label, n]) => (n > 1 ? `${label} (${n}×)` : label))
      .join(', ');
    warnings.push(
      `${data.unsupported.length} component${data.unsupported.length > 1 ? 's are' : ' is'} not yet simulated by the engine and will be ignored: ${summary}.`,
    );
  }

  warnings.push(...data.wireGeometryWarnings);

  if (data.unknownNodes.length > 0) {
    warnings.push(
      `Voltmeter${data.unknownNodes.length > 1 ? 's' : ''} reference${data.unknownNodes.length > 1 ? '' : 's'} ` +
        `node label${data.unknownNodes.length > 1 ? 's' : ''} that exist${data.unknownNodes.length > 1 ? '' : 's'} nowhere in the circuit ` +
        `(treated as ground, reads 0): ${data.unknownNodes.map((u) => u.label).join(', ')}.`,
    );
  }

  if (data.danglingCouplings.length > 0) {
    warnings.push(
      `Coupling${data.danglingCouplings.length > 1 ? 's' : ''} to a deleted component ` +
        `(gate never switches / meter reads 0): ${data.danglingCouplings.map((d) => `${d.comp.name} ➔ ${d.target}`).join(', ')}.`,
    );
  }

  // Observational sensors check (DRC hint):
  // If a Voltmeter or Ammeter does NOT feed any control block input (neither via wire
  // nor via terminal net-labels), suggest using virtual probing (P key) instead of a physical meter.
  const unusedMeters = findUnusedMeters(components, wires);
  for (const m of unusedMeters) {
    warnings.push(
      `Meter output unused on ${m.name}: This sensor does not feed any control block. Remove it and use '${m.suggestedSignal}' in the scope instead? (Use Probe tool 'P' or Oscilloscope tab).`,
    );
  }

  return warnings;
}

/**
 * Metadata for a meter (Voltmeter or Ammeter) whose output does not feed any control block.
 */
export interface UnusedMeterInfo {
  name: string;
  type: number;
  family?: string;
  suggestedSignal: string;
}

/**
 * Identifies Voltmeter and Ammeter components that are placed purely for waveform observation
 * rather than supplying feedback signals to control blocks.
 */
export function findUnusedMeters(components: ComponentLike[], wires: WireLike[]): UnusedMeterInfo[] {
  const unused: UnusedMeterInfo[] = [];

  const consumedControlSignals = new Set<string>();
  for (const c of components) {
    if (c.family === 'CONTROL') {
      for (const l of c.inputLabels ?? []) {
        const raw = l?.trim();
        if (raw && raw !== SENTINEL_UNSET) consumedControlSignals.add(raw.toLowerCase());
      }
    }
  }

  const allConnectedPoints = new Set<string>();
  for (const w of wires) {
    for (const pt of w.points ?? []) {
      allConnectedPoints.add(`${pt[0]},${pt[1]}`);
    }
  }

  for (const c of components) {
    const isVolt = isVoltmeterComponent(c);
    const isAmp = isAmmeterComponent(c);
    if (!isVolt && !isAmp) continue;

    // Check if output terminal is wired
    const terms = terminalPositions(c);
    const outTerm = terms.output?.[0];
    const isWired = outTerm ? allConnectedPoints.has(`${outTerm.x},${outTerm.y}`) : false;

    // Check if name or outputLabels are consumed by another control component
    let isLabelConsumed = consumedControlSignals.has(c.name.trim().toLowerCase());
    for (const l of c.outputLabels ?? []) {
      const raw = l?.trim();
      if (raw && raw !== SENTINEL_UNSET && consumedControlSignals.has(raw.toLowerCase())) {
        isLabelConsumed = true;
        break;
      }
    }

    if (!isWired && !isLabelConsumed) {
      let suggestedSignal = 'waveform';
      if (isAmp) {
        const target = String(c.parameters?.coupledComponent ?? '').trim();
        suggestedSignal = target && target !== SENTINEL_UNSET ? `I(${target})` : 'I(...)';
      } else if (isVolt) {
        const target = String(c.parameters?.coupledComponent ?? '').trim();
        const nodeA = String(c.parameters?.nodeA ?? c.parameters?.positiveNode ?? '').trim();
        const nodeB = String(c.parameters?.nodeB ?? c.parameters?.negativeNode ?? '').trim();
        if (target && target !== SENTINEL_UNSET) {
          suggestedSignal = `V(${target})`;
        } else if (nodeA && nodeA !== SENTINEL_UNSET && nodeB && nodeB !== SENTINEL_UNSET && nodeB !== '0') {
          suggestedSignal = `V(${nodeA}|${nodeB})`;
        } else if (nodeA && nodeA !== SENTINEL_UNSET) {
          suggestedSignal = `V(${nodeA})`;
        } else {
          suggestedSignal = 'V(...)';
        }
      }

      unused.push({
        name: c.name,
        type: c.type,
        family: c.family,
        suggestedSignal,
      });
    }
  }

  return unused;
}

/** Extracts grid coordinates `{ x, y }` from a component record regardless of snapshot vs flat shape. */
function getCompCoord(comp: ComponentLike | undefined): { x: number; y: number } | undefined {
  if (!comp) return undefined;
  const raw = comp as { position?: number[]; x?: number; y?: number };
  if (Array.isArray(raw.position) && raw.position.length >= 2) {
    return { x: raw.position[0], y: raw.position[1] };
  }
  if (typeof raw.x === 'number' && typeof raw.y === 'number') {
    return { x: raw.x, y: raw.y };
  }
  return undefined;
}

/** Finds the longest matching component name mentioned inside an engine error or warning message. */
function findMentionedComponent(text: string, components: ComponentLike[]): ComponentLike | undefined {
  if (!text) return undefined;
  // Sort longest names first so 'R10' matches before 'R1'
  const sorted = [...components].sort((a, b) => b.name.length - a.name.length);
  for (const c of sorted) {
    if (!c.name) continue;
    const escaped = c.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const re = new RegExp(`(^|[^A-Za-z0-9_.])${escaped}(?=$|[^A-Za-z0-9_.])`);
    if (re.test(text)) {
      return c;
    }
  }
  return undefined;
}

/**
 * Builds structured, actionable diagnostic items combining:
 * - Simulation engine errors & post-run warnings
 * - Timing & solver advisories (`dt` aliasing vs circuit frequency, high step counts)
 * - Schematic Design Rule Checks (open wires, uncoupled drivers/meters, missing sources, etc.)
 */
export function buildCircuitDiagnostics(
  components: ComponentLike[],
  wires: WireLike[],
  context?: DiagnosticsContext,
): DiagnosticItem[] {
  const items: DiagnosticItem[] = [];

  // 1. Engine error & post-run warnings
  if (context?.simErrorMessage) {
    if (context.simErrorMessage.startsWith(ENGINE_UNREACHABLE_PREFIX)) {
      items.push({
        id: 'engine-unreachable',
        severity: 'error',
        category: 'engine',
        title: 'Simulation Engine Not Reachable',
        detail:
          `${context.simErrorMessage} This is a connection problem, not a circuit error: ` +
          'check that the GeckoCIRCUITS backend is running (see the Live/Polling indicator in the top bar), then press Run again.',
      });
    } else {
      const mentioned = findMentionedComponent(context.simErrorMessage, components);
      items.push({
        id: 'engine-error',
        severity: 'error',
        category: 'engine',
        title: 'Simulation Failed',
        detail: context.simErrorMessage,
        componentName: mentioned?.name,
        coord: getCompCoord(mentioned),
        quickFix: mentioned
          ? { label: `Inspect ${mentioned.name}`, action: 'select-component', componentName: mentioned.name }
          : { label: 'Open Setup', action: 'open-sim-config' },
      });
    }
  }

  if (context?.simWarnings && context.simWarnings.length > 0) {
    context.simWarnings.forEach((msg, idx) => {
      const mentioned = findMentionedComponent(msg, components);
      items.push({
        id: `engine-warn-${idx}`,
        severity: 'warning',
        category: 'engine',
        title: 'Simulation Engine Warning',
        detail: msg,
        componentName: mentioned?.name,
        coord: getCompCoord(mentioned),
        quickFix: mentioned
          ? { label: `Inspect ${mentioned.name}`, action: 'select-component', componentName: mentioned.name }
          : undefined,
      });
    });
  }

  // 2. Timing & Solver Advisories
  if (context?.dt !== undefined && context?.tEnd !== undefined) {
    const dtVal = parseEngineeringValue(context.dt);
    const tEndVal = parseEngineeringValue(context.tEnd);

    if (
      dtVal === null ||
      tEndVal === null ||
      !Number.isFinite(dtVal) ||
      dtVal <= 0 ||
      !Number.isFinite(tEndVal) ||
      tEndVal <= 0
    ) {
      items.push({
        id: 'timing-invalid',
        severity: 'error',
        category: 'timing',
        title: 'Invalid Simulation Timing Parameters',
        detail: `Time step (dt='${context.dt}') and end time (t_end='${context.tEnd}') must be positive values.`,
        quickFix: { label: 'Fix in Setup', action: 'open-sim-config' },
      });
    } else {
      const steps = estimateStepCount(tEndVal, dtVal);
      const auto = suggestAutoTimeStep(components as FrequencyInspectableComponent[], tEndVal);

      if (steps >= STEP_WARNING_THRESHOLD) {
        items.push({
          id: 'timing-high-steps',
          severity: 'warning',
          category: 'timing',
          title: 'Very High Step Count',
          detail: `${steps.toLocaleString()} steps (dt=${context.dt}, t_end=${context.tEnd}) reaches or exceeds ${STEP_WARNING_THRESHOLD.toLocaleString()} steps and may run slowly.`,
          quickFix: {
            label: `Set dt = ${auto.dtStr}`,
            action: 'auto-dt',
            suggestedDt: auto.dtStr,
          },
        });
      }

      const { maxFreq, sourceName } = detectMaxCircuitFrequency(
        components as FrequencyInspectableComponent[],
      );
      if (maxFreq > 0) {
        const stepsPerCycle = 1 / maxFreq / dtVal;
        if (stepsPerCycle < 20) {
          const targetComp = sourceName ? components.find((c) => c.name === sourceName) : undefined;
          items.push({
            id: 'timing-aliasing',
            severity: 'warning',
            category: 'timing',
            title: 'Coarse Step Width vs Circuit Frequency',
            detail:
              `${sourceName ?? 'Source'} operates at ${maxFreq >= 1e3 ? `${(maxFreq / 1e3).toFixed(1)} kHz` : `${maxFreq} Hz`}, ` +
              `giving only ${Math.max(1, Math.round(stepsPerCycle))} steps/cycle with dt=${context.dt} (recommend >= 50 steps/cycle).`,
            componentName: sourceName ?? undefined,
            coord: getCompCoord(targetComp),
            quickFix: {
              label: `Auto-set dt = ${auto.dtStr}`,
              action: 'auto-dt',
              suggestedDt: auto.dtStr,
            },
          });
        }
      }
    }
  }

  // 3. Structural / DRC Checks
  if (components.length === 0) {
    items.push({
      id: 'drc-empty-sheet',
      severity: 'info',
      category: 'drc',
      title: 'Empty Schematic',
      detail: 'The sheet is empty — place components and wire them up before running a simulation.',
    });
    return items;
  }

  const data = collectStructuralValidation(components, wires);

  for (const d of data.danglingPower) {
    items.push({
      id: `drc-dangling-power-${d.wireIndex}`,
      severity: 'warning',
      category: 'drc',
      title: `Open Wire End (Wire #${d.wireIndex + 1})`,
      detail: `Unconnected power wire end (${d.where}). Reconnect to a terminal or delete.`,
      wireIndex: d.wireIndex,
      coord: { x: d.x, y: d.y },
      quickFix: {
        label: `Delete Wire #${d.wireIndex + 1}`,
        action: 'delete-wire',
        wireIndex: d.wireIndex,
      },
    });
  }

  for (const d of data.danglingControl) {
    items.push({
      id: `drc-dangling-ctrl-${d.wireIndex}`,
      severity: 'warning',
      category: 'drc',
      title: `Open Control Wire (Wire #${d.wireIndex + 1})`,
      detail: `Control ${d.where} is unconnected. Control inputs bind by signal name; reconnect or remove extra segment.`,
      wireIndex: d.wireIndex,
      coord: { x: d.x, y: d.y },
      quickFix: {
        label: `Delete Wire #${d.wireIndex + 1}`,
        action: 'delete-wire',
        wireIndex: d.wireIndex,
      },
    });
  }

  if (!data.hasElectricalSource) {
    items.push({
      id: 'drc-no-source',
      severity: 'warning',
      category: 'drc',
      title: 'No Electrical / Driver Source',
      detail: 'No voltage, current, MMF, or thermal source found — simulated waveforms will stay at zero.',
    });
  }

  if (data.unsupported.length > 0) {
    const firstUnsupported = data.unsupported[0];
    items.push({
      id: 'drc-unsupported',
      severity: 'warning',
      category: 'drc',
      title: `${data.unsupported.length} Unsupported Component${data.unsupported.length > 1 ? 's' : ''}`,
      detail: `Ignored by current engine: ${data.unsupported.map((c) => c.name).join(', ')}.`,
      componentName: firstUnsupported.name,
      coord: getCompCoord(firstUnsupported),
      quickFix: {
        label: `Select ${firstUnsupported.name}`,
        action: 'select-component',
        componentName: firstUnsupported.name,
      },
    });
  }

  data.wireGeometryWarnings.forEach((msg, idx) => {
    const wireMatch = msg.match(/Wire(?:s)?\s+(\d+)/i);
    const wireIndex = wireMatch ? Number(wireMatch[1]) - 1 : undefined;
    const wirePts = wireIndex !== undefined ? wires[wireIndex]?.points : undefined;
    const coord =
      wirePts && wirePts.length > 0 ? { x: wirePts[0][0], y: wirePts[0][1] } : undefined;
    items.push({
      id: `drc-wire-geom-${idx}`,
      severity: 'warning',
      category: 'drc',
      title: 'Wire Overlap / Terminal Short Risk',
      detail: msg,
      wireIndex,
      coord,
      quickFix:
        wireIndex !== undefined
          ? { label: `Locate Wire #${wireIndex + 1}`, action: 'focus-wire', wireIndex }
          : undefined,
    });
  });

  data.unknownNodes.forEach((u, idx) => {
    items.push({
      id: `drc-unknown-node-${u.comp.name}-${idx}`,
      severity: 'warning',
      category: 'drc',
      title: `Voltmeter Unknown Net (${u.comp.name})`,
      detail: `${u.comp.name} references net '${u.node}', which does not exist in the circuit (treated as ground, reads 0 V).`,
      componentName: u.comp.name,
      coord: getCompCoord(u.comp),
      quickFix: {
        label: `Select ${u.comp.name}`,
        action: 'select-component',
        componentName: u.comp.name,
      },
    });
  });

  data.danglingCouplings.forEach((d) => {
    items.push({
      id: `drc-broken-coupling-${d.comp.name}`,
      severity: 'warning',
      category: 'drc',
      title: `Broken Coupling on ${d.comp.name}`,
      detail: `${d.comp.name} references deleted or missing component '${d.target}' (gate never switches / meter reads 0).`,
      componentName: d.comp.name,
      coord: getCompCoord(d.comp),
      quickFix: {
        label: `Select ${d.comp.name}`,
        action: 'select-component',
        componentName: d.comp.name,
      },
    });
  });

  data.uncoupledControls.forEach((u) => {
    items.push({
      id: `drc-uncoupled-${u.comp.name}`,
      severity: 'warning',
      category: 'drc',
      title: `Uncoupled ${u.kind === 'gate' ? 'Gate Driver' : u.kind === 'ammeter' ? 'Ammeter' : 'Voltmeter'} (${u.comp.name})`,
      detail: u.reason,
      componentName: u.comp.name,
      coord: getCompCoord(u.comp),
      quickFix: {
        label: `Select ${u.comp.name}`,
        action: 'select-component',
        componentName: u.comp.name,
      },
    });
  });

  return items;
}
