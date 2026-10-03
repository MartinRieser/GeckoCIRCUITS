import { describe, it, expect } from 'vitest';
import {
  EXAMPLES,
  BLANK_CIRCUIT_IPES,
  RLC_CIRCUIT_IPES,
  BUCK_CONVERTER_IPES,
  BOOST_CONVERTER_IPES,
  RECTIFIER_CIRCUIT_IPES,
  RC_FILTER_IPES,
  RC_CLASSIC_IPES,
  THREE_SCOPES_RLC_IPES,
} from '../src/model/examples';
import { findPlacementConflict } from '../src/model/geometry';
import type { EditorComponent } from '../src/model/types';

/** Helper to extract components from .ipes content string for geometry checks. */
function parseComponentsFromIpes(content: string): EditorComponent[] {
  const comps: EditorComponent[] = [];
  const lkRe = /<ElementLK>([\s\S]*?)<\\ElementLK>/g;
  let match: RegExpExecArray | null;
  while ((match = lkRe.exec(content)) !== null) {
    const block = match[1];
    const name = /idStringDialog\s+(\S+)/.exec(block)?.[1] || '';
    const x = parseInt(/x\s+(\d+)/.exec(block)?.[1] || '0', 10);
    const y = parseInt(/y\s+(\d+)/.exec(block)?.[1] || '0', 10);
    const typ = parseInt(/typ\s+(\d+)/.exec(block)?.[1] || '0', 10);
    const ori = parseInt(/orientierung\s+(\d+)/.exec(block)?.[1] || '503', 10);
    comps.push({ name, position: [x, y], orientation: ori, type: typ, family: 'LK', parameters: {}, inputLabels: [], outputLabels: [] });
  }

  const ctrlRe = /<ElementCONTROL>([\s\S]*?)<\\ElementCONTROL>/g;
  while ((match = ctrlRe.exec(content)) !== null) {
    const block = match[1];
    const name = /idStringDialog\s+(\S+)/.exec(block)?.[1] || '';
    const x = parseInt(/x\s+(\d+)/.exec(block)?.[1] || '0', 10);
    const y = parseInt(/y\s+(\d+)/.exec(block)?.[1] || '0', 10);
    const typ = parseInt(/typ\s+(\d+)/.exec(block)?.[1] || '0', 10);
    const ori = parseInt(/orientierung\s+(\d+)/.exec(block)?.[1] || '503', 10);
    comps.push({ name, position: [x, y], orientation: ori, type: typ, family: 'CONTROL', parameters: {}, inputLabels: [], outputLabels: [] });
  }
  return comps;
}

describe('Circuit Examples (.ipes templates)', () => {
  it('exports a valid non-empty BLANK_CIRCUIT_IPES template', () => {
    expect(BLANK_CIRCUIT_IPES).toBeTruthy();
    expect(BLANK_CIRCUIT_IPES).toContain('tDURATION');
    expect(BLANK_CIRCUIT_IPES).toContain('dt');
  });

  it('exports pre-defined example templates with complete headers and blocks', () => {
    const templates = [
      RLC_CIRCUIT_IPES,
      BUCK_CONVERTER_IPES,
      BOOST_CONVERTER_IPES,
      RECTIFIER_CIRCUIT_IPES,
      RC_FILTER_IPES,
      RC_CLASSIC_IPES,
      THREE_SCOPES_RLC_IPES,
    ];

    for (const t of templates) {
      expect(t.length).toBeGreaterThan(100);
      expect(t).toContain('tDURATION');
      expect(t).toContain('dt');
      expect(t).toContain('solverType');
    }
  });

  it('provides a catalog of curated educational examples in EXAMPLES registry', () => {
    expect(EXAMPLES.length).toBeGreaterThanOrEqual(6);

    const ids = new Set<string>();
    for (const eg of EXAMPLES) {
      expect(eg.id).toBeTruthy();
      expect(eg.name).toBeTruthy();
      expect(eg.category).toBeTruthy();
      expect(eg.description).toBeTruthy();
      expect(eg.content.length).toBeGreaterThan(50);

      // Verify IDs are unique
      expect(ids.has(eg.id), `Duplicate example id found: ${eg.id}`).toBe(false);
      ids.add(eg.id);
    }
  });

  it('includes key topologies: Buck, Boost, Rectifier, and RLC circuits', () => {
    const exampleNames = EXAMPLES.map((e) => e.name.toLowerCase());
    expect(exampleNames.some((n) => n.includes('buck'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('boost'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('rectifier'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('rlc'))).toBe(true);
  });

  it('guarantees ZERO overlapping components across all example circuits', () => {
    for (const ex of EXAMPLES) {
      const components = parseComponentsFromIpes(ex.content);
      expect(components.length).toBeGreaterThanOrEqual(3);

      // 1. Verify no two component origins share identical coordinates or collide
      for (let i = 0; i < components.length; i++) {
        const c1 = components[i];
        for (let j = i + 1; j < components.length; j++) {
          const c2 = components[j];
          const dist = Math.hypot(c1.position[0] - c2.position[0], c1.position[1] - c2.position[1]);
          expect(
            dist,
            `Component overlap detected in example '${ex.id}': ${c1.name} at (${c1.position}) and ${c2.name} at (${c2.position}) with dist=${dist.toFixed(2)}`,
          ).toBeGreaterThanOrEqual(2.5);
        }
      }

      // 2. Verify findPlacementConflict returns no collision when components are sequentially placed
      const placed: EditorComponent[] = [];
      for (const comp of components) {
        const conflict = findPlacementConflict(comp, placed);
        expect(
          conflict,
          `Placement conflict detected in '${ex.id}' between ${comp.name} and ${conflict}`,
        ).toBeNull();
        placed.push(comp);
      }
    }
  });
});

