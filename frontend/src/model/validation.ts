/**
 * Client-side circuit validation used for wire-commit feedback and the
 * pre-simulation check in the Simulation tab. Pure functions, no React.
 *
 * Surfaces missing sources, unsupported stamped components, dangling open power
 * leads, overlapping wire geometry, and accidental terminal shorts before runs.
 */
import { allTerminals, terminalPositions } from './geometry';
import { denseCellsOf } from '../canvas/WireRouter';
import { LkComponentType } from './constants';
import { isVoltmeterComponent, isAmmeterComponent } from './componentSchema';

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

  // Check for open power circuit leads
  const danglingPower: string[] = [];
  const danglingControl: string[] = [];
  wires.forEach((w, index) => {
    const pts = w.points ?? [];
    if (pts.length === 0) return;
    const isControl = w.type === 'CONTROL';
    const ends: Array<{ x: number; y: number; pointIndex: number }> = [
      { x: pts[0][0], y: pts[0][1], pointIndex: 0 },
      { x: pts[pts.length - 1][0], y: pts[pts.length - 1][1], pointIndex: pts.length - 1 },
    ];
    for (const end of ends) {
      const onTerminal = allTerminals(components).some(
        (t) => t.point.x === end.x && t.point.y === end.y,
      );
      const onWire = pointOnAnyWire(end, wires, {
        wireIndex: index,
        pointIndex: end.pointIndex,
      });
      if (!onTerminal && !onWire) {
        const where = `wire ${index + 1} ends free at (${end.x}, ${end.y})`;
        (isControl ? danglingControl : danglingPower).push(where);
        break;
      }
    }
  });

  if (danglingPower.length > 0) {
    warnings.push(
      `Unconnected wire end${danglingPower.length > 1 ? 's' : ''} (open circuit): ${danglingPower.join('; ')}.`,
    );
  }
  if (danglingControl.length > 0) {
    warnings.push(
      `Control wire${danglingControl.length > 1 ? 's' : ''} with a free end (${danglingControl.join('; ')}): ` +
        'scope and control inputs bind by signal name, so the channel still records — ' +
        'the wire only matters when you want the pin physically wired. Reconnect or delete it.',
    );
  }

  // Ensure circuit contains at least one driver / source
  const hasElectricalSource = components.some((c) => SOURCE_COMPONENT_TYPES.has(c.type));
  if (!hasElectricalSource) {
    warnings.push(
      'No source found (voltage source, current source, MMF or thermal source) — all signals will stay zero.',
    );
  }

  // Identify unsupported components that the engine does not stamp
  const unsupported = components.filter(
    (c) =>
      c.family !== 'CONTROL' &&
      c.family !== 'SPECIAL' &&
      c.family !== 'ANNOTATION' &&
      c.type !== 2001 &&
      !SIMULATED_LK_TYPES.has(c.type),
  );
  if (unsupported.length > 0) {
    const counts = new Map<string, number>();
    for (const c of unsupported) {
      counts.set(labelOf(c), (counts.get(labelOf(c)) ?? 0) + 1);
    }
    const summary = [...counts.entries()]
      .map(([label, n]) => (n > 1 ? `${label} (${n}×)` : label))
      .join(', ');
    warnings.push(
      `${unsupported.length} component${unsupported.length > 1 ? 's are' : ' is'} not yet simulated by the engine and will be ignored: ${summary}.`,
    );
  }

  // Wire geometry warnings (overlaps and inadvertent shorts)
  warnings.push(...findWireGeometryWarnings(components, wires));

  // Voltmeters bound to node labels that exist nowhere in the circuit read a
  // silent 0 (the engine grounds unknown labels) — flag them pre-run.
  const knownNets = new Set<string>(['0', 'gnd', 'ground']);
  for (const c of components) {
    for (const l of [...(c.inputLabels ?? []), ...(c.outputLabels ?? [])]) {
      const raw = l?.trim();
      if (raw && raw !== 'NIX_NIX_NIX') knownNets.add(raw.toLowerCase());
    }
  }
  for (const w of wires) {
    const raw = w.type === 'CONTROL' ? undefined : (w as { label?: string }).label?.trim();
    if (raw && raw !== 'NIX_NIX_NIX') knownNets.add(raw.toLowerCase());
  }
  const unknownNodes: string[] = [];
  for (const c of components) {
    if (c.family !== 'CONTROL') continue;
    if (c.type !== 1001 && c.type !== 1) continue; // voltmeter (new + legacy numbering)
    if (c.parameters?.coupledComponent) continue; // measures across a component instead
    for (const key of ['nodeA', 'nodeB', 'positiveNode', 'negativeNode']) {
      const node = String(c.parameters?.[key] ?? '').trim();
      if (node && node !== 'NIX_NIX_NIX' && !knownNets.has(node.toLowerCase())
          && !unknownNodes.some((u) => u.includes(`'${node}'`))) {
        unknownNodes.push(`${c.name}: '${node}'`);
      }
    }
  }
  if (unknownNodes.length > 0) {
    warnings.push(
      `Voltmeter${unknownNodes.length > 1 ? 's' : ''} reference${unknownNodes.length > 1 ? '' : 's'} ` +
        `node label${unknownNodes.length > 1 ? 's' : ''} that exist${unknownNodes.length > 1 ? '' : 's'} nowhere in the circuit ` +
        `(treated as ground, reads 0): ${unknownNodes.join(', ')}.`,
    );
  }

  // Couplings pointing at components that no longer exist: the reference is
  // dead — gates never switch, ammeters export 0.
  const knownNames = new Set(components.map((c) => c.name));
  const danglingCouplings: string[] = [];
  for (const c of components) {
    const target = String(c.parameters?.coupledComponent ?? '').trim();
    if (target && target !== 'NIX_NIX_NIX' && !knownNames.has(target)) {
      danglingCouplings.push(`${c.name} ➔ ${target}`);
    }
  }
  if (danglingCouplings.length > 0) {
    warnings.push(
      `Coupling${danglingCouplings.length > 1 ? 's' : ''} to a deleted component ` +
        `(gate never switches / meter reads 0): ${danglingCouplings.join(', ')}.`,
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
        if (raw && raw !== 'NIX_NIX_NIX') consumedControlSignals.add(raw.toLowerCase());
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
      if (raw && raw !== 'NIX_NIX_NIX' && consumedControlSignals.has(raw.toLowerCase())) {
        isLabelConsumed = true;
        break;
      }
    }

    if (!isWired && !isLabelConsumed) {
      let suggestedSignal = 'waveform';
      if (isAmp) {
        const target = String(c.parameters?.coupledComponent ?? '').trim();
        suggestedSignal = target && target !== 'NIX_NIX_NIX' ? `I(${target})` : 'I(...)';
      } else if (isVolt) {
        const target = String(c.parameters?.coupledComponent ?? '').trim();
        const nodeA = String(c.parameters?.nodeA ?? c.parameters?.positiveNode ?? '').trim();
        const nodeB = String(c.parameters?.nodeB ?? c.parameters?.negativeNode ?? '').trim();
        if (target && target !== 'NIX_NIX_NIX') {
          suggestedSignal = `V(${target})`;
        } else if (nodeA && nodeA !== 'NIX_NIX_NIX' && nodeB && nodeB !== 'NIX_NIX_NIX' && nodeB !== '0') {
          suggestedSignal = `V(${nodeA}|${nodeB})`;
        } else if (nodeA && nodeA !== 'NIX_NIX_NIX') {
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
