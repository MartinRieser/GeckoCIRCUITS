/**
 * Client-side circuit validation used for wire-commit feedback and the
 * pre-simulation check in the Simulation tab. Pure functions, no React.
 *
 * Surfaces missing sources, unsupported stamped components, dangling open power
 * leads, overlapping wire geometry, and accidental terminal shorts before runs.
 */
import { allTerminals } from './geometry';
import { denseCellsOf } from '../canvas/WireRouter';
import { LkComponentType } from './constants';

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
  LkComponentType.COUPLED_INDUCTOR,
  LkComponentType.TRANSFORMER,
  LkComponentType.RELUCTANCE,
  LkComponentType.MMF,
  LkComponentType.MOSFET,
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
        found.push(
          `Wires ${i + 1} and ${j + 1} overlap across ${shared} cells from (${fmtKey(first as string)}) — two separate connections are drawn as one line`,
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
    (c) => c.family !== 'CONTROL' && !SIMULATED_LK_TYPES.has(c.type),
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

  return warnings;
}
