import { describe, it, expect } from 'vitest';
import { EXAMPLES, BLANK_CIRCUIT_IPES } from '../src/model/examples';

/** Extracts the block bodies of a given tag from an .ipes ASCII document. */
function blocks(content: string, tag: string): string[] {
  const result: string[] = [];
  const re = new RegExp(`<${tag}>([\\s\\S]*?)<\\\\${tag}>`, 'g');
  let match: RegExpExecArray | null;
  while ((match = re.exec(content)) !== null) {
    result.push(match[1]);
  }
  return result;
}

function field(block: string, key: string): string | null {
  const escaped = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = new RegExp(`^${escaped}\\s+(.+)$`, 'm').exec(block);
  return match ? match[1].trim() : null;
}

describe('built-in example circuits', () => {
  it('provides the blank template plus demo circuits', () => {
    expect(BLANK_CIRCUIT_IPES).toContain('tDURATION');
    expect(EXAMPLES.length).toBeGreaterThanOrEqual(3);
    for (const ex of EXAMPLES) {
      expect(ex.content).toContain('tDURATION');
      expect(ex.content).toContain('dataContainerSignals[]');
    }
  });

  it('example components parse into typed blocks with positions and parameters', () => {
    for (const ex of EXAMPLES) {
      const elements = blocks(ex.content, 'ElementLK');
      expect(elements.length).toBeGreaterThanOrEqual(2);

      for (const element of elements) {
        expect(field(element, 'typ')).toMatch(/^\d+$/);
        expect(field(element, 'x')).toMatch(/^\d+$/);
        expect(field(element, 'y')).toMatch(/^\d+$/);
        expect(field(element, 'idStringDialog')).toBeTruthy();
        // classic rotation codes
        expect(field(element, 'orientierung')).toMatch(/^50[1-4]$/);
      }

      // every wire carries an x/y point list and connector type
      for (const wire of blocks(ex.content, 'Connection')) {
        expect(field(wire, 'x')).toMatch(/^\d+(\s+\d+)+$/);
        expect(field(wire, 'y')).toMatch(/^\d+(\s+\d+)+$/);
        expect(field(wire, 'connectorType')).toBeTruthy();
      }
    }
  });

  it('the RC example wires the source, resistor and capacitor into one loop', () => {
    const rc = EXAMPLES.find((e) => e.id === 'rc')!;
    const elements = blocks(rc.content, 'ElementLK');
    const types = elements.map((e) => field(e, 'typ'));
    // voltage source (4), resistor (1), capacitor (3)
    expect(types).toEqual(['4', '1', '3']);
  });
});
