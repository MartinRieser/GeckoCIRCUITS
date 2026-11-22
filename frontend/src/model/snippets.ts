/**
 * Reusable Schematic Snippets: schema, selection extraction, built-in starter blocks,
 * localStorage persistence, JSON import/export, and conflict-free placement search.
 */
import type { EditorComponent, EditorWire } from './types';
import {
  getCoupledComponentName,
  getMutualCouplingUids,
  isMutualCoupler,
} from './componentSchema';
import {
  ControlComponentType,
  LkComponentType,
  Orientation,
} from './constants';
import { findPlacementConflict, terminalPositions } from './geometry';

export const SNIPPETS_STORAGE_KEY = 'gecko_schematic_snippets_v1';

export interface SnippetComponent {
  /** Zero-based index within the snippet (used to re-link internal couplings on insert). */
  localId: number;
  type: number;
  family: string;
  /** Offset `[dx, dy]` in integer grid units relative to snippet origin `(0, 0)`. */
  offset: [number, number];
  orientation: number;
  parameters: Record<string, number | string | boolean>;
  inputLabels: string[];
  outputLabels: string[];
}

export interface SnippetWire {
  type: string;
  label: string;
  /** Grid polyline points `[[dx, dy], ...]` relative to snippet origin `(0, 0)`. */
  points: [number, number][];
}

export interface SnippetInternalCoupling {
  /** Index of the control/coupler component inside the snippet. */
  sourceLocalId: number;
  /** Parameter key storing the target reference (`'coupledComponent'`, `'param1'`, `'param2'`). */
  paramKey: string;
  /** Index of the target component inside this snippet. */
  targetLocalId: number;
  /** Whether the parameter stores a component name (`'name'`) or numeric uid (`'uid'`). */
  refKind: 'name' | 'uid';
}

export interface SchematicSnippet {
  id: string;
  name: string;
  category: string;
  description: string;
  builtIn?: boolean;
  createdAt: number;
  width: number;
  height: number;
  components: SnippetComponent[];
  wires: SnippetWire[];
  couplings: SnippetInternalCoupling[];
}

export const BUILT_IN_SNIPPETS: SchematicSnippet[] = [
  {
    id: 'builtin_lc_output_filter',
    name: 'LC Output Filter',
    category: 'Filters',
    description: 'Series inductor + shunt filter capacitor referenced to GND (v_out node)',
    builtIn: true,
    createdAt: 0,
    width: 8,
    height: 8,
    components: [
      {
        localId: 0,
        type: LkComponentType.INDUCTOR,
        family: 'LK',
        offset: [2, 2],
        orientation: Orientation.WEST_EAST,
        parameters: { param0: 100e-6, param1: 0 },
        inputLabels: [],
        outputLabels: [],
      },
      {
        localId: 1,
        type: LkComponentType.CAPACITOR,
        family: 'LK',
        offset: [6, 4],
        orientation: Orientation.NORTH_SOUTH,
        parameters: { param0: 47e-6, param1: 0 },
        inputLabels: [],
        outputLabels: [],
      },
      {
        localId: 2,
        type: LkComponentType.GLOBAL_TERMINAL,
        family: 'LK',
        offset: [6, 8],
        orientation: Orientation.NORTH_SOUTH,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      },
    ],
    wires: [
      {
        type: 'LK',
        label: 'v_out',
        points: [
          [4, 2],
          [6, 2],
        ],
      },
    ],
    couplings: [],
  },
  {
    id: 'builtin_buck_switching_cell',
    name: 'Buck Switching Cell',
    category: 'Power Stages',
    description: 'MOSFET high-side switch coupled to Gate driver + freewheeling diode',
    builtIn: true,
    createdAt: 0,
    width: 8,
    height: 8,
    components: [
      {
        localId: 0,
        type: LkComponentType.MOSFET,
        family: 'LK',
        offset: [2, 2],
        orientation: Orientation.WEST_EAST,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      },
      {
        localId: 1,
        type: LkComponentType.DIODE,
        family: 'LK',
        offset: [6, 6],
        orientation: Orientation.SOUTH_NORTH,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      },
      {
        localId: 2,
        type: ControlComponentType.GATE,
        family: 'CONTROL',
        offset: [2, 5],
        orientation: Orientation.NORTH_SOUTH,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      },
    ],
    wires: [
      {
        type: 'LK',
        label: 'sw',
        points: [
          [4, 2],
          [6, 2],
          [6, 4],
        ],
      },
    ],
    couplings: [
      {
        sourceLocalId: 2,
        paramKey: 'coupledComponent',
        targetLocalId: 0,
        refKind: 'name',
      },
    ],
  },
  {
    id: 'builtin_pi_limiter',
    name: 'PI Controller + Limiter',
    category: 'Control',
    description: 'PI compensator stage wired into an output saturation limiter',
    builtIn: true,
    createdAt: 0,
    width: 10,
    height: 4,
    components: [
      {
        localId: 0,
        type: ControlComponentType.PI,
        family: 'CONTROL',
        offset: [2, 2],
        orientation: Orientation.NORTH_SOUTH,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      },
      {
        localId: 1,
        type: ControlComponentType.LIMIT,
        family: 'CONTROL',
        offset: [8, 2],
        orientation: Orientation.NORTH_SOUTH,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      },
    ],
    wires: [
      {
        type: 'CONTROL',
        label: '',
        points: [
          [4, 2],
          [6, 2],
        ],
      },
    ],
    couplings: [],
  },
];

function isPointNearSelectedTerminal(
  pt: number[],
  selectedTerminals: Array<{ x: number; y: number }>,
): boolean {
  const px = pt[0];
  const py = pt[1];
  return selectedTerminals.some((t) => Math.hypot(t.x - px, t.y - py) <= 0.6);
}

/**
 * Extracts a normalized, reusable {@link SchematicSnippet} from selected schematic
 * components, including wires whose endpoints lie inside the selection and re-indexed
 * internal component couplings (`GATE` -> `MOSFET`, `AMP`/`VOLT` -> target, `LK_M` -> inductors).
 */
export function extractSnippetFromSelection(
  name: string,
  category: string,
  description: string,
  selectedNames: string[],
  allComponents: EditorComponent[],
  allWires: EditorWire[],
  selectedWireIndices: number[] = [],
): SchematicSnippet | null {
  const selectedSet = new Set(selectedNames);
  const selectedComps = allComponents.filter((c) => selectedSet.has(c.name));
  if (selectedComps.length === 0) {
    return null;
  }

  const nameToLocalId = new Map<string, number>();
  const uidToLocalId = new Map<number, number>();
  const allTerms: Array<{ x: number; y: number }> = [];

  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;

  selectedComps.forEach((comp, idx) => {
    nameToLocalId.set(comp.name, idx);
    if (typeof comp.uid === 'number' && comp.uid > 0) {
      uidToLocalId.set(comp.uid, idx);
    }
    minX = Math.min(minX, comp.position[0]);
    minY = Math.min(minY, comp.position[1]);
    maxX = Math.max(maxX, comp.position[0]);
    maxY = Math.max(maxY, comp.position[1]);

    const terms = terminalPositions(comp);
    for (const pt of [...terms.input, ...terms.output]) {
      allTerms.push(pt);
      minX = Math.min(minX, pt.x);
      minY = Math.min(minY, pt.y);
      maxX = Math.max(maxX, pt.x);
      maxY = Math.max(maxY, pt.y);
    }
  });

  const explicitWireSet = new Set(selectedWireIndices);
  const internalWires = allWires.filter((w) => {
    if (!w.points || w.points.length < 2) return false;
    if (explicitWireSet.has(w.index)) return true;
    const first = w.points[0];
    const last = w.points[w.points.length - 1];
    return (
      isPointNearSelectedTerminal(first, allTerms) &&
      isPointNearSelectedTerminal(last, allTerms)
    );
  });

  for (const w of internalWires) {
    for (const pt of w.points) {
      minX = Math.min(minX, pt[0]);
      minY = Math.min(minY, pt[1]);
      maxX = Math.max(maxX, pt[0]);
      maxY = Math.max(maxY, pt[1]);
    }
  }

  minX = Math.floor(minX);
  minY = Math.floor(minY);
  maxX = Math.ceil(maxX);
  maxY = Math.ceil(maxY);

  const couplings: SnippetInternalCoupling[] = [];
  const snippetComponents: SnippetComponent[] = selectedComps.map((comp, idx) => {
    const coupledName = getCoupledComponentName(comp);
    if (coupledName && nameToLocalId.has(coupledName)) {
      const targetLocalId = nameToLocalId.get(coupledName)!;
      const rawParams = comp.parameters || {};
      const paramKey =
        rawParams.coupledReferenceID !== undefined &&
        String(rawParams.coupledReferenceID).trim() === coupledName
          ? 'coupledReferenceID'
          : 'coupledComponent';
      couplings.push({
        sourceLocalId: idx,
        paramKey,
        targetLocalId,
        refKind: 'name',
      });
    } else if (isMutualCoupler(comp)) {
      const [uid1, uid2] = getMutualCouplingUids(comp);
      if (uid1 > 0 && uidToLocalId.has(uid1)) {
        couplings.push({
          sourceLocalId: idx,
          paramKey: 'param1',
          targetLocalId: uidToLocalId.get(uid1)!,
          refKind: 'uid',
        });
      }
      if (uid2 > 0 && uidToLocalId.has(uid2)) {
        couplings.push({
          sourceLocalId: idx,
          paramKey: 'param2',
          targetLocalId: uidToLocalId.get(uid2)!,
          refKind: 'uid',
        });
      }
    }

    return {
      localId: idx,
      type: comp.type,
      family: comp.family,
      offset: [Math.round(comp.position[0] - minX), Math.round(comp.position[1] - minY)],
      orientation: comp.orientation,
      parameters: { ...(comp.parameters || {}) },
      inputLabels: [...(comp.inputLabels || [])],
      outputLabels: [...(comp.outputLabels || [])],
    };
  });

  const snippetWires: SnippetWire[] = internalWires.map((w) => ({
    type: w.type || 'LK',
    label: w.label || '',
    points: w.points.map(([px, py]) => [Math.round(px - minX), Math.round(py - minY)]),
  }));

  return {
    id: `snippet_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    name: name.trim(),
    category: category.trim() || 'Custom',
    description: description.trim(),
    createdAt: Date.now(),
    width: Math.max(1, maxX - minX),
    height: Math.max(1, maxY - minY),
    components: snippetComponents,
    wires: snippetWires,
    couplings,
  };
}

/**
 * Checks whether placing `snippet` at `(anchorX, anchorY)` collides with any existing
 * component on the sheet.
 */
export function hasSnippetPlacementConflict(
  snippet: SchematicSnippet,
  anchorX: number,
  anchorY: number,
  existingComponents: EditorComponent[],
): boolean {
  for (const c of snippet.components) {
    const conflict = findPlacementConflict(
      {
        type: c.type,
        family: c.family,
        position: [anchorX + c.offset[0], anchorY + c.offset[1]],
        orientation: c.orientation,
      },
      existingComponents,
    );
    if (conflict !== null) {
      return true;
    }
  }
  return false;
}

/**
 * Finds the nearest conflict-free anchor point around `(desiredX, desiredY)` using a
 * 2-grid spiral pattern up to 20 candidate shifts.
 */
export function findConflictFreeAnchor(
  snippet: SchematicSnippet,
  desiredX: number,
  desiredY: number,
  existingComponents: EditorComponent[],
): { x: number; y: number; shifted: boolean } {
  const baseX = Math.max(1, Math.round(desiredX));
  const baseY = Math.max(1, Math.round(desiredY));

  if (!hasSnippetPlacementConflict(snippet, baseX, baseY, existingComponents)) {
    return { x: baseX, y: baseY, shifted: false };
  }

  const candidates: Array<[number, number]> = [];
  for (let ring = 1; ring <= 4; ring++) {
    const step = ring * 2;
    candidates.push(
      [baseX + step, baseY],
      [baseX, baseY + step],
      [baseX + step, baseY + step],
      [baseX - step, baseY],
      [baseX, baseY - step],
      [baseX + step, baseY - step],
      [baseX - step, baseY + step],
      [baseX - step, baseY - step],
    );
  }

  let attempts = 0;
  for (const [cx, cy] of candidates) {
    if (attempts >= 20) break;
    const candX = Math.max(1, cx);
    const candY = Math.max(1, cy);
    attempts++;
    if (!hasSnippetPlacementConflict(snippet, candX, candY, existingComponents)) {
      return { x: candX, y: candY, shifted: true };
    }
  }

  return { x: baseX, y: baseY, shifted: false };
}

function isValidSnippet(item: unknown): item is SchematicSnippet {
  if (!item || typeof item !== 'object') return false;
  const s = item as Partial<SchematicSnippet>;
  return (
    typeof s.id === 'string' &&
    s.id.trim().length > 0 &&
    typeof s.name === 'string' &&
    s.name.trim().length > 0 &&
    Array.isArray(s.components) &&
    s.components.length > 0 &&
    Array.isArray(s.wires) &&
    Array.isArray(s.couplings)
  );
}

/** Loads user-saved snippets from `localStorage`. */
export function loadUserSnippets(): SchematicSnippet[] {
  try {
    const raw = localStorage.getItem(SNIPPETS_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter((item): item is SchematicSnippet => isValidSnippet(item) && !item.builtIn);
  } catch {
    return [];
  }
}

function persistUserSnippets(list: SchematicSnippet[]): SchematicSnippet[] {
  const sanitized = list.filter((s) => !s.builtIn);
  try {
    localStorage.setItem(SNIPPETS_STORAGE_KEY, JSON.stringify(sanitized));
  } catch {
    // ignore storage errors
  }
  return sanitized;
}

/** Saves or updates a user snippet in `localStorage` and returns the updated user snippet list. */
export function saveUserSnippet(snippet: SchematicSnippet): SchematicSnippet[] {
  const current = loadUserSnippets();
  const next = [...current.filter((s) => s.id !== snippet.id), { ...snippet, builtIn: false }];
  return persistUserSnippets(next);
}

/** Deletes a user snippet by ID (ignores built-in snippets) and returns the updated list. */
export function deleteUserSnippet(id: string): SchematicSnippet[] {
  const current = loadUserSnippets();
  const next = current.filter((s) => s.id !== id);
  return persistUserSnippets(next);
}

/** Serializes user-saved snippets into a portable JSON export string. */
export function exportSnippetsJson(snippets: SchematicSnippet[]): string {
  const userOnly = snippets.filter((s) => !s.builtIn);
  return JSON.stringify({ version: 1, snippets: userOnly }, null, 2);
}

/** Parses and merges imported JSON snippet bundles into `localStorage`. */
export function importSnippetsJson(raw: string): {
  snippets: SchematicSnippet[];
  importedCount: number;
  error?: string;
} {
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return { snippets: loadUserSnippets(), importedCount: 0, error: 'Invalid JSON syntax' };
  }

  const candidateArray = Array.isArray(parsed)
    ? parsed
    : parsed && typeof parsed === 'object' && Array.isArray((parsed as { snippets?: unknown }).snippets)
    ? (parsed as { snippets: unknown[] }).snippets
    : null;

  if (!candidateArray) {
    return {
      snippets: loadUserSnippets(),
      importedCount: 0,
      error: 'Expected a snippet list or { snippets: [...] } bundle',
    };
  }

  const validImported = candidateArray.filter(isValidSnippet).map((s) => ({
    ...s,
    builtIn: false,
  }));

  if (validImported.length === 0) {
    return {
      snippets: loadUserSnippets(),
      importedCount: 0,
      error: 'No valid snippets found in file',
    };
  }

  const existing = loadUserSnippets();
  const merged = [...existing];
  for (const imp of validImported) {
    const idx = merged.findIndex((s) => s.id === imp.id);
    if (idx >= 0) {
      merged[idx] = imp;
    } else {
      merged.push(imp);
    }
  }

  return {
    snippets: persistUserSnippets(merged),
    importedCount: validImported.length,
  };
}
