import { describe, it, expect } from 'vitest';
import {
  EXAMPLES,
  BLANK_CIRCUIT_IPES,
  RLC_CIRCUIT_IPES,
  BUCK_CONVERTER_IPES,
  RC_FILTER_IPES,
  RC_CLASSIC_IPES,
  THREE_SCOPES_RLC_IPES,
} from '../src/model/examples';

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
    expect(EXAMPLES.length).toBeGreaterThanOrEqual(4);

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

  it('includes key topologies: Buck Converter and RLC circuits', () => {
    const exampleNames = EXAMPLES.map((e) => e.name.toLowerCase());
    expect(exampleNames.some((n) => n.includes('buck'))).toBe(true);
    expect(exampleNames.some((n) => n.includes('rlc'))).toBe(true);
  });
});
