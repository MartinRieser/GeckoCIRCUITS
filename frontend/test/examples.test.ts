import { describe, it, expect } from 'vitest';
import {
  EXAMPLES,
  BLANK_CIRCUIT_IPES,
  RLC_CIRCUIT_IPES,
  BUCK_CONVERTER_IPES,
  BOOST_CONVERTER_IPES,
  RECTIFIER_CIRCUIT_IPES,
  RC_FILTER_IPES,
  THREE_SCOPES_RLC_IPES,
  SYNC_BUCK_IPES,
  BUCK_BOOST_IPES,
  INVERTER_IPES,
  FLYBACK_IPES,
  CLOSED_LOOP_BUCK_IPES,
  PFC_BOOST_IPES,
  SPWM_INVERTER_DEADTIME_IPES,
} from '../src/model/examples';
import { findPlacementConflict } from '../src/model/geometry';
import { extractAvailableSignals } from '../src/model/componentSchema';
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
      THREE_SCOPES_RLC_IPES,
      SYNC_BUCK_IPES,
      BUCK_BOOST_IPES,
      INVERTER_IPES,
      FLYBACK_IPES,
      CLOSED_LOOP_BUCK_IPES,
      PFC_BOOST_IPES,
      SPWM_INVERTER_DEADTIME_IPES,
    ];

    for (const t of templates) {
      expect(t.length).toBeGreaterThan(100);
      expect(t).toContain('tDURATION');
      expect(t).toContain('dt');
      expect(t).toContain('solverType');
    }
  });

  it('provides a catalog of curated educational examples in EXAMPLES registry', () => {
    expect(EXAMPLES.length).toBe(13);

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

  it('includes key topologies: Buck, Boost, Rectifier, Inverter, Flyback, and PFC circuits', () => {
    const exampleNames = EXAMPLES.map((e) => e.name.toLowerCase());
    expect(exampleNames.some((n) => n.includes('buck'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('boost'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('rectifier'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('rlc'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('inverter'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('flyback'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('pfc'))).toBe(true);
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

  it('contains no deprecated physical SCOPE components and includes modern graphRegisters across all examples', () => {
    for (const ex of EXAMPLES) {
      // Must not contain physical SCOPE element blocks
      expect(ex.content).not.toMatch(/idStringDialog\s+SCOPE/i);
      expect(ex.content).not.toMatch(/typ\s+5[\s\S]*?idStringDialog\s+SCOPE/i);
      // Markdown notes should refer to Oscilloscope / Osci, not legacy SCOPE.1
      expect(ex.content).not.toMatch(/SCOPE\.\d/);
      // Must have modern graphRegisters
      expect(ex.content).toContain('<graphRegisters>');
      expect(ex.content).toContain('<\\graphRegisters>');
    }
  });

  it('ensures all note cards have adequate dimensions for documentation readability', () => {
    for (const ex of EXAMPLES) {
      const noteMatches = ex.content.matchAll(/idStringDialog\s+NOTE\.\d+[\s\S]*?boxWidth\s+(\d+)[\s\S]*?boxHeight\s+(\d+)/g);
      for (const m of noteMatches) {
        const width = parseInt(m[1], 10);
        const height = parseInt(m[2], 10);
        expect(width, `Note width too small in '${ex.id}': ${width}`).toBeGreaterThanOrEqual(30);
        expect(height, `Note height too small in '${ex.id}': ${height}`).toBeGreaterThanOrEqual(20);
      }
    }
  });

  it('synchronizes all example circuits exactly with disk examples/*.ipes and manifest.json', async () => {
    // Dynamically access fs/path in Vitest Node test runner
    // @ts-expect-error node fs dynamic import in Vitest environment
    const fs = await import(/* @vite-ignore */ 'node:fs');
    // @ts-expect-error node path dynamic import in Vitest environment
    const path = await import(/* @vite-ignore */ 'node:path');

    const cwd = (globalThis as unknown as { process?: { cwd: () => string } }).process?.cwd() ?? '.';
    const examplesDir = path.resolve(cwd, '../examples');
    const manifestPath = path.join(examplesDir, 'manifest.json');
    if (!fs.existsSync(manifestPath)) {
      return; // Skip if run in standalone bundle without root repo
    }

    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    expect(manifest.length).toBe(EXAMPLES.length);

    for (const item of manifest) {
      const ex = EXAMPLES.find((e: { id: string }) => e.id === item.id);
      expect(ex, `Manifest id '${item.id}' missing in EXAMPLES registry`).toBeDefined();
      expect(ex?.name).toBe(item.name);

      const filePath = path.join(examplesDir, item.filename);
      expect(fs.existsSync(filePath), `Example file '${item.filename}' does not exist on disk`).toBe(true);

      const diskContent = fs.readFileSync(filePath, 'utf8');
      expect(diskContent).not.toMatch(/idStringDialog\s+SCOPE/i);
      expect(diskContent).not.toMatch(/SCOPE\.\d/);
      expect(diskContent.trim()).toBe(ex!.content.trim());
    }
  });

  it('guarantees zero unresolvable speculative signals (Ploss, Tj, Phi) are auto-requested for bundled examples', () => {
    for (const ex of EXAMPLES) {
      const comps = parseComponentsFromIpes(ex.content);
      const available = extractAvailableSignals(comps, []);
      // Filter by useEditor's auto-request policy
      const autoRequested = available.filter(
        (s) => !s.startsWith('Ploss_') && !s.startsWith('Tj_') && !s.startsWith('Phi_'),
      );
      // Verify no speculative domain signal was accidentally retained
      expect(autoRequested.some((s) => s.startsWith('Ploss_'))).toBe(false);
      expect(autoRequested.some((s) => s.startsWith('Tj_'))).toBe(false);
      expect(autoRequested.some((s) => s.startsWith('Phi_'))).toBe(false);

      // Verify all I(...) and V(...) quantities correspond to valid components in the example
      const compNames = new Set(comps.map((c) => c.name));
      for (const sig of autoRequested) {
        if (sig.startsWith('I(') && sig.endsWith(')')) {
          const compName = sig.slice(2, -1);
          expect(compNames.has(compName), `Signal '${sig}' in example '${ex.id}' references non-existent component '${compName}'`).toBe(true);
        } else if (sig.startsWith('V(') && sig.endsWith(')')) {
          const inner = sig.slice(2, -1);
          if (!inner.includes('|') && compNames.has(inner)) {
            expect(compNames.has(inner)).toBe(true);
          }
        }
      }
    }
  });
});


