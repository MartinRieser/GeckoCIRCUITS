import { describe, expect, it } from 'vitest';
import { tokenizeScript } from '../src/properties/scriptHighlight';

/** Class of the token covering the given substring position. */
function classAt(tokens: ReturnType<typeof tokenizeScript>, index: number): string {
  const hit = tokens.find((t) => index >= t.start && index < t.end);
  return hit?.cls ?? '<none>';
}

describe('scriptHighlight tokenizer', () => {
  it('classifies comments, numbers, keywords, and operators', () => {
    const code = '// header\n/* block */ x = 1.5e-3;\nif (a != b) { y = 2; }';
    const tokens = tokenizeScript(code);

    expect(classAt(tokens, 0)).toBe('tok-comment'); // // header
    expect(classAt(tokens, code.indexOf('/* block'))).toBe('tok-comment');
    expect(classAt(tokens, code.indexOf('1.5e-3'))).toBe('tok-number');
    expect(classAt(tokens, code.indexOf('if'))).toBe('tok-keyword');
    expect(classAt(tokens, code.indexOf('!='))).toBe('tok-op');
  });

  it('classifies built-in functions, constants, and time variables', () => {
    const code = 'y = sin(PI * t) + max(x, dt)';
    const tokens = tokenizeScript(code);

    expect(classAt(tokens, code.indexOf('sin'))).toBe('tok-func');
    expect(classAt(tokens, code.indexOf('PI'))).toBe('tok-const');
    expect(classAt(tokens, code.lastIndexOf('t)') - 1)).toBe('tok-time');
    expect(classAt(tokens, code.indexOf('max'))).toBe('tok-func');
    expect(classAt(tokens, code.indexOf('dt'))).toBe('tok-time');
  });

  it('separates input and output signal names', () => {
    const code = 'yOUT[0] = xIN[0] * u2 + y1;';
    const tokens = tokenizeScript(code);

    expect(classAt(tokens, code.indexOf('yOUT'))).toBe('tok-output');
    expect(classAt(tokens, code.indexOf('xIN'))).toBe('tok-input');
    expect(classAt(tokens, code.indexOf('u2'))).toBe('tok-input');
    expect(classAt(tokens, code.indexOf('y1'))).toBe('tok-output');
  });

  it('covers the source contiguously without gaps', () => {
    const code = 'if (u1 > 0.5) {\n  // step\n  y = sqrt(u1);\n}';
    const tokens = tokenizeScript(code);

    let cursor = 0;
    for (const token of tokens) {
      expect(token.start).toBe(cursor);
      expect(token.end).toBeGreaterThan(token.start);
      cursor = token.end;
    }
    expect(cursor).toBe(code.length);
  });
});
