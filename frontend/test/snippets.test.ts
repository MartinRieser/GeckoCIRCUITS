// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  BUILT_IN_SNIPPETS,
  SNIPPETS_STORAGE_KEY,
  deleteUserSnippet,
  exportSnippetsJson,
  extractSnippetFromSelection,
  findConflictFreeAnchor,
  importSnippetsJson,
  loadUserSnippets,
  saveUserSnippet,
} from '../src/model/snippets';
import { ControlComponentType, LkComponentType, Orientation } from '../src/model/constants';
import type { EditorComponent, EditorWire } from '../src/model/types';

function makeComp(
  partial: Partial<EditorComponent> & { name: string; uid: number; type: number; family: string },
): EditorComponent {
  return {
    position: [10, 10],
    orientation: Orientation.WEST_EAST,
    parameters: {},
    inputLabels: [],
    outputLabels: [],
    ...partial,
  };
}

describe('schematic snippets', () => {
  beforeEach(() => {
    localStorage.removeItem(SNIPPETS_STORAGE_KEY);
  });

  afterEach(() => {
    localStorage.removeItem(SNIPPETS_STORAGE_KEY);
  });

  it('extracts normalized coordinates, internal wires, and name/UID couplings while excluding external wires', () => {
    // MOSFET S1 at (14, 12) WEST_EAST -> pins (12, 12) and (16, 12)
    // Diode D1 at (18, 16) SOUTH_NORTH -> pins (18, 18) anode and (18, 14) cathode
    // Gate G1 at (14, 15) NORTH_SOUTH coupled to S1 -> input pin (12, 15)
    // External Resistor R_ext at (4, 12) WEST_EAST -> pins (2, 12) and (6, 12)
    // Coupled inductors LK_M + LKOP2 for UID coupling verification
    const components: EditorComponent[] = [
      makeComp({ name: 'S1', uid: 101, type: LkComponentType.MOSFET, family: 'LK', position: [14, 12] }),
      makeComp({ name: 'D1', uid: 102, type: LkComponentType.DIODE, family: 'LK', position: [18, 16], orientation: Orientation.SOUTH_NORTH }),
      makeComp({ name: 'G1', uid: 103, type: ControlComponentType.GATE, family: 'CONTROL', position: [14, 15], orientation: Orientation.NORTH_SOUTH, parameters: { coupledComponent: 'S1' } }),
      makeComp({ name: 'L_A', uid: 201, type: LkComponentType.COUPLED_INDUCTOR, family: 'LK', position: [16, 20] }),
      makeComp({ name: 'L_B', uid: 202, type: LkComponentType.COUPLED_INDUCTOR, family: 'LK', position: [20, 20] }),
      makeComp({ name: 'K1', uid: 203, type: LkComponentType.MUTUAL_INDUCTANCE, family: 'LK', position: [18, 20], parameters: { param0: 0.9, param1: 201, param2: 202 } }),
      makeComp({ name: 'R_ext', uid: 300, type: LkComponentType.RESISTOR, family: 'LK', position: [4, 12] }),
    ];

    const wires: EditorWire[] = [
      // Internal wire: S1 output (16, 12) -> D1 cathode (18, 14)
      { index: 0, type: 'LK', label: 'sw', points: [[16, 12], [18, 12], [18, 14]] },
      // External wire: R_ext output (6, 12) -> S1 input (12, 12)
      { index: 1, type: 'LK', label: 'in', points: [[6, 12], [12, 12]] },
    ];

    const snippet = extractSnippetFromSelection(
      'Buck + Coupler',
      'Power Stages',
      'Half-bridge stage with mutual coupling',
      ['S1', 'D1', 'G1', 'L_A', 'L_B', 'K1'],
      components,
      wires,
    );

    expect(snippet).not.toBeNull();
    // Min x across selected terminals: S1 input = 12, D1 cathode = 18, G1 pin = 12 => minX = 12, minY = 12
    const s1 = snippet!.components.find((c) => c.type === LkComponentType.MOSFET)!;
    expect(s1.offset).toEqual([2, 0]);
    expect(snippet!.wires).toHaveLength(1);
    expect(snippet!.wires[0].label).toBe('sw');
    expect(snippet!.wires[0].points).toEqual([[4, 0], [6, 0], [6, 2]]);

    // Name coupling G1 -> S1 plus UID couplings K1 -> L_A, L_B
    const gateCoupling = snippet!.couplings.find((c) => c.refKind === 'name');
    expect(gateCoupling).toBeDefined();
    expect(gateCoupling!.paramKey).toBe('coupledComponent');
    expect(snippet!.components[gateCoupling!.targetLocalId].type).toBe(LkComponentType.MOSFET);

    const uidCouplings = snippet!.couplings.filter((c) => c.refKind === 'uid');
    expect(uidCouplings).toHaveLength(2);
  });

  it('provides 3 valid built-in starter snippets and shifts anchor away from occupied spots', () => {
    expect(BUILT_IN_SNIPPETS).toHaveLength(3);
    const buck = BUILT_IN_SNIPPETS.find((s) => s.name === 'Buck Switching Cell')!;
    expect(buck.builtIn).toBe(true);
    expect(buck.couplings).toHaveLength(1);

    const existing: EditorComponent[] = [
      makeComp({ name: 'R1', uid: 1, type: LkComponentType.RESISTOR, family: 'LK', position: [12, 12] }),
    ];
    // MOSFET inside Buck has offset [2, 2], so anchoring at (10, 10) collides at (12, 12)
    const placement = findConflictFreeAnchor(buck, 10, 10, existing);
    expect(placement.shifted).toBe(true);
    expect(placement.x !== 10 || placement.y !== 10).toBe(true);
  });

  it('persists, deletes, exports, and imports user snippets cleanly while rejecting malformed JSON', () => {
    const custom = {
      ...BUILT_IN_SNIPPETS[0],
      id: 'user_snip_1',
      name: 'My LC Filter',
      builtIn: false,
      createdAt: 123456,
    };

    const afterSave = saveUserSnippet(custom);
    expect(afterSave).toHaveLength(1);
    expect(loadUserSnippets()[0].name).toBe('My LC Filter');

    const json = exportSnippetsJson(afterSave);
    localStorage.removeItem(SNIPPETS_STORAGE_KEY);
    expect(loadUserSnippets()).toHaveLength(0);

    const imported = importSnippetsJson(json);
    expect(imported.error).toBeUndefined();
    expect(imported.importedCount).toBe(1);
    expect(loadUserSnippets()[0].id).toBe('user_snip_1');

    const bad = importSnippetsJson('{not json');
    expect(bad.error).toBeDefined();
    expect(bad.importedCount).toBe(0);

    const afterDelete = deleteUserSnippet('user_snip_1');
    expect(afterDelete).toHaveLength(0);
  });
});
