/**
 * Client-side circuit validation used for wire-commit feedback and the
 * pre-simulation check in the Simulation tab. Pure functions, no React.
 *
 * The engine silently ignores what it cannot stamp (e.g. motors without a
 * stamper yet) and dangling wire ends create invisible open circuits, so the
 * editor surfaces these problems before the user wastes a run.
 */
import { allTerminals } from './geometry';

/** Component type numbers the core engine can currently stamp and solve. */
export const SIMULATED_LK_TYPES: ReadonlySet<number> = new Set([
  1, // LK_R resistor
  2, // LK_L inductor
  3, // LK_C capacitor
  4, // LK_U voltage source
  5, // LK_I current source
  6, // LK_D diode
  7, // LK_S ideal switch
  8, // LK_THYR thyristor
  10, // LK_IGBT
  12, // LK_LKOP2 coupable inductor
  23, // LK_TRANS ideal transformer (expanded to a winding pair)
  24, // REL_RELUCTANCE
  26, // REL_MMF
  28, // LK_MOSFET
  33, // LK_BJT (expanded to its subcircuit elements)
  44, // TH_FLOW heat flow source
  45, // TH_TEMP temperature source
  46, // TH_RTH thermal resistance
  47, // TH_CTH thermal capacitance
]);

export interface WireLike {
  points?: number[][];
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
 * True when the grid point terminates on a component terminal or on any
 * point of an existing wire (endpoints AND interior raster points conduct).
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
 * True when the point lies on any raster point of any wire. When checking a
 * wire's own end, pass its index and the end's point index so the endpoint
 * does not trivially match itself.
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
  // displayName lives on the palette-augmented entries; fall back to raw name
  const withMeta = comp as ComponentLike & { displayName?: string };
  return withMeta.displayName ?? comp.name;
}

/**
 * Pre-simulation validation. Returns human-readable warnings; an empty array
 * means nothing suspicious was found. Deliberately conservative: everything
 * reported here is a likely mistake, not a hard error.
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

  // Unconnected wire ends create invisible open circuits.
  const dangling: string[] = [];
  wires.forEach((w, index) => {
    const pts = w.points ?? [];
    if (pts.length === 0) return;
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
        dangling.push(`wire ${index + 1} ends free at (${end.x}, ${end.y})`);
        break; // one report per wire is enough
      }
    }
  });
  if (dangling.length > 0) {
    warnings.push(
      `Unconnected wire end${dangling.length > 1 ? 's' : ''} (open circuit): ${dangling.join('; ')}.`,
    );
  }

  // A circuit nothing drives will solve to zero everywhere.
  const hasElectricalSource = components.some(
    (c) =>
      c.type === 4 ||
      c.type === 5 ||
      c.type === 26 ||
      c.type === 45 ||
      c.type === 44,
  );
  if (!hasElectricalSource) {
    warnings.push(
      'No source found (voltage source, current source, MMF or thermal source) — all signals will stay zero.',
    );
  }

  // Components the core engine cannot stamp yet are silently skipped there.
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

  return warnings;
}
