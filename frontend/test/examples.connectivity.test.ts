import { describe, it, expect } from 'vitest';
import { EXAMPLES } from '../src/model/examples';
import { terminalPositions } from '../src/model/geometry';
import { LkComponentType } from '../src/model/constants';

/**
 * Wire-connectivity regression guard for the built-in examples.
 *
 * Every visual wiring defect that shipped historically — diagonal wire jumps,
 * duplicated overlapping segments, degenerate single-point wires, wires that
 * stop short of a component pin — is covered here so the parser + terminal
 * geometry reject such .ipes content permanently.
 */

interface ParsedComponent {
  name: string;
  type: number;
  family: 'LK' | 'CONTROL';
  position: number[];
  orientation: number;
  inputLabels: string[];
  outputLabels: string[];
  parameters: Record<string, number | string | boolean>;
  /** Raw parameterString tokens (coupled target / measured nodes). */
  paramStringTokens: string[];
}

interface ParsedWire {
  label: string;
  type: 'LK' | 'CONTROL';
  points: number[][];
}

/** Slash-separated .ipes label list ("/v_out/i_L") → ["v_out", "i_L"]. */
function parseLabels(line: string | undefined): string[] {
  if (!line) return [];
  const value = line.replace(/^label\w+\[\]\s*/, '').trim();
  if (!value) return [];
  return value.split('/').filter((t) => t.length > 0);
}

function parseIpes(content: string): { components: ParsedComponent[]; wires: ParsedWire[] } {
  const components: ParsedComponent[] = [];

  const parseElement = (block: string, family: 'LK' | 'CONTROL') => {
    const name = /idStringDialog\s+(\S+)/.exec(block)?.[1] ?? '';
    const x = parseInt(/(^|\n)x\s+(-?\d+)/.exec(block)?.[2] ?? '0', 10);
    const y = parseInt(/(^|\n)y\s+(-?\d+)/.exec(block)?.[2] ?? '0', 10);
    const typ = parseInt(/(^|\n)typ\s+(-?\d+)/.exec(block)?.[2] ?? '0', 10);
    const ori = parseInt(/orientierung\s+(-?\d+)/.exec(block)?.[1] ?? '503', 10);
    const paramString = /parameterString\[\]\s*([^\n]*)/.exec(block)?.[1] ?? '';
    const paramStringTokens = paramString.trim().split(/\s+/)[0]?.split('/').filter(Boolean) ?? [];
    components.push({
      name,
      type: typ,
      family,
      position: [x, y],
      orientation: ori,
      inputLabels: parseLabels(/labelAnfangsKnoten\[\][^\n]*/.exec(block)?.[0]),
      outputLabels: parseLabels(/labelEndKnoten\[\][^\n]*/.exec(block)?.[0]),
      parameters: {},
      paramStringTokens,
    });
  };

  for (const m of content.matchAll(/<ElementLK>([\s\S]*?)<\\ElementLK>/g)) {
    parseElement(m[1], 'LK');
  }
  for (const m of content.matchAll(/<ElementCONTROL>([\s\S]*?)<\\ElementCONTROL>/g)) {
    parseElement(m[1], 'CONTROL');
  }

  const wires: ParsedWire[] = [];
  const wireRe =
    /verbindung(LK|CONTROL)\s*\(\d+\)\s*\n<Verbindung>\n([\s\S]*?)<\\Verbindung>/g;
  for (const m of content.matchAll(wireRe)) {
    const type = m[1] === 'LK' ? 'LK' : 'CONTROL';
    const block = m[2];
    const label = (/label\s+(\S+)/.exec(block)?.[1] ?? '').trim();
    const xs = (/x\[\]([^\n]*)/.exec(block)?.[1] ?? '').trim().split(/\s+/).filter(Boolean).map(Number);
    const ys = (/y\[\]([^\n]*)/.exec(block)?.[1] ?? '').trim().split(/\s+/).filter(Boolean).map(Number);
    const points = xs.map((x, i) => [x, ys[i] ?? x]);
    wires.push({ label, type, points });
  }

  return { components, wires };
}

/** Dense raster cells of a wire; also collects non-orthogonal segments. */
function denseCellsOf(points: number[][], diagonals: string[]): Set<string> {
  const cells = new Set<string>();
  for (let i = 0; i + 1 < points.length; i++) {
    const [x0, y0] = points[i];
    const [x1, y1] = points[i + 1];
    if (x0 !== x1 && y0 !== y1) {
      diagonals.push(`(${x0},${y0}) -> (${x1},${y1})`);
    }
    const xStep = Math.sign(x1 - x0);
    const yStep = Math.sign(y1 - y0);
    let [x, y] = [x0, y0];
    cells.add(`${x},${y}`);
    while (x !== x1 || y !== y1) {
      if (x !== x1) x += xStep;
      else y += yStep;
      cells.add(`${x},${y}`);
    }
  }
  if (points.length === 1) cells.add(`${points[0][0]},${points[0][1]}`);
  return cells;
}

describe('Example wire connectivity (no visually broken schematics)', () => {
  it('wires every example with orthogonal wires that reach all pins', () => {
    const problems: string[] = [];
    for (const ex of EXAMPLES) {
      const { components, wires } = parseIpes(ex.content);
      if (components.length === 0) problems.push(`${ex.id}: no components parsed`);
      if (wires.length === 0) problems.push(`${ex.id}: no wires parsed`);

      // Terminals per component (LK_M coupler is pinless by design)
      const terminals: { comp: string; x: number; y: number }[] = [];
      for (const c of components) {
        if (c.family === 'LK' && c.type === LkComponentType.MUTUAL_INDUCTANCE) continue;
        const t = terminalPositions(c);
        for (const p of t.input) terminals.push({ comp: c.name, x: p.x, y: p.y });
        for (const p of t.output) terminals.push({ comp: c.name, x: p.x, y: p.y });
      }

      const diagonals: string[] = [];
      const wireCells = wires.map((w) => denseCellsOf(w.points, diagonals));

      // 1. No diagonal wire segments
      if (diagonals.length > 0) {
        problems.push(`${ex.id}: diagonal wire segments ${diagonals.join('; ')}`);
      }

      // 2. No degenerate or duplicated wires: every pair shares < 2 raster cells
      for (let i = 0; i < wires.length; i++) {
        if (wires[i].points.length <= 1) {
          problems.push(`${ex.id}: wire "${wires[i].label}" (#${i}) is degenerate`);
        }
        for (let j = i + 1; j < wires.length; j++) {
          let shared = 0;
          for (const key of wireCells[j]) if (wireCells[i].has(key)) shared++;
          if (shared >= 2) {
            problems.push(
              `${ex.id}: wires "${wires[i].label}" (#${i}) and "${wires[j].label}" (#${j}) overlap across ${shared} cells`,
            );
          }
        }
      }

      // 3. Every wire endpoint lands on a terminal or another wire
      wires.forEach((w, i) => {
        for (const end of [w.points[0], w.points[w.points.length - 1]]) {
          const key = `${end[0]},${end[1]}`;
          const onTerminal = terminals.some((t) => `${t.x},${t.y}` === key);
          const onOtherWire = wireCells.some((cells, j) => j !== i && cells.has(key));
          if (!onTerminal && !onOtherWire) {
            problems.push(`${ex.id}: wire "${w.label}" (#${i}) ends free at (${key})`);
          }
        }
      });

      // 4. Every component pin is touched by a wire (or pins of another component)
      for (const t of terminals) {
        const key = `${t.x},${t.y}`;
        const onWire = wireCells.some((cells) => cells.has(key));
        const onPin = terminals.some((o) => o !== t && `${o.x},${o.y}` === key);
        if (!onWire && !onPin) {
          problems.push(`${ex.id}: ${t.comp} pin at (${key}) is not touched by any wire`);
        }
      }

      // 5. No wire interior crosses a terminal that another wire ends on
      wires.forEach((w, i) => {
        const pts = w.points;
        const ownEnds = new Set([`${pts[0][0]},${pts[0][1]}`, `${pts[pts.length - 1][0]},${pts[pts.length - 1][1]}`]);
        for (const t of terminals) {
          const key = `${t.x},${t.y}`;
          if (ownEnds.has(key) || !wireCells[i].has(key)) continue;
          const wiredElsewhere = wires.some(
            (o, j) =>
              j !== i &&
              [o.points[0], o.points[o.points.length - 1]].some(
                (e) => `${e[0]},${e[1]}` === key,
              ),
          );
          if (wiredElsewhere) {
            problems.push(
              `${ex.id}: wire "${w.label}" (#${i}) passes through the ${t.comp} pin at (${key}) that another wire also connects`,
            );
          }
        }
      });

      // 6. No wire crosses a coupling-badge pill (solid rounded rect drawn
      //    halfH+12px below gate drivers / ammeters / voltmeters, where it
      //    would make the badge text unreadable)
      const dpix = parseInt(/dpix\s+(\d+)/.exec(ex.content)?.[1] ?? '16', 10);
      const allWireCells = new Set<string>();
      for (const cells of wireCells) for (const key of cells) allWireCells.add(key);
      for (const c of components) {
        if (c.family !== 'CONTROL') continue;
        const [cx, cy] = c.position;
        const tokens = c.paramStringTokens.filter((t) => t && t !== 'NIX_NIX_NIX');
        let text = '';
        if (c.type === 6 || c.type === 1000) {
          if (tokens[0]) text = `➔ ${tokens[0]}`;
        } else if (c.type === 2 || c.type === 1002) {
          if (tokens[0]) text = `➔ i(${tokens[0]})`;
        } else if (c.type === 1 || c.type === 1001) {
          if (tokens[0]) text = `➔ V(${tokens[0]}, ${tokens[1] ?? '0'})`;
        }
        if (!text) continue;
        // Sheet.tsx: badgeW = max(len*6.2+14, 52) px, rect y = halfH+12±8 px,
        // halfH = 1.3*dpix for single-pin control blocks
        const halfW = Math.max(text.length * 6.2 + 14, 52) / 2 / dpix;
        const y0 = cy + 1.3 + 4 / dpix;
        const y1 = cy + 1.3 + 20 / dpix;
        const crossings = [...allWireCells].filter((key) => {
          const [x, y] = key.split(',').map(Number);
          return (
            x >= cx - halfW && x <= cx + halfW && y >= y0 && y <= y1
          );
        });
        if (crossings.length > 0) {
          problems.push(
            `${ex.id}: wires cross the "${text}" badge pill of ${c.name} at cells ${crossings.join(', ')}`,
          );
        }
      }

      // 7. No badge text rect is covered by another component's name label
      //    (GATE_LO's above-centred name label once landed exactly on
      //    GATE_HI's "➔ S_hi" badge and its dark halo erased the text)
      const nameRects: { text: string; x0: number; x1: number; y0: number; y1: number }[] = [];
      for (const c of components) {
        const [cx, cy] = c.position;
        const isCtrlProbe =
          c.family === 'CONTROL' && [1, 2, 1001, 1002].includes(c.type);
        const isHorizontal = c.orientation === 502 || c.orientation === 504;
        const w = (c.name.length * 6.5 + 4) / dpix;
        if (c.family === 'CONTROL' && !isCtrlProbe) {
          // control blocks: name centred above the symbol (Sheet.tsx)
          const yMid = cy - 1.3 - 5 / dpix;
          nameRects.push({ text: c.name, x0: cx - w / 2, x1: cx + w / 2, y0: yMid - 8 / dpix, y1: yMid + 2 / dpix });
        } else if (isCtrlProbe) {
          // probes: name to the left of the symbol
          nameRects.push({ text: c.name, x0: cx - 1.3 - 6 / dpix - w, x1: cx - 1.3 - 6 / dpix, y0: cy - 8 / dpix, y1: cy + 8 / dpix });
        } else if (c.family === 'LK' && isHorizontal) {
          // horizontal two-ports: name centred above the symbol
          const yMid = cy - 1.3 - 5 / dpix;
          nameRects.push({ text: c.name, x0: cx - w / 2, x1: cx + w / 2, y0: yMid - 8 / dpix, y1: yMid + 2 / dpix });
        }
        // vertical LK components draw their name to the right, where badges never reach
      }
      for (const c of components) {
        if (c.family !== 'CONTROL') continue;
        const [cx, cy] = c.position;
        const tokens = c.paramStringTokens.filter((t) => t && t !== 'NIX_NIX_NIX');
        let text = '';
        if ((c.type === 6 || c.type === 1000) && tokens[0]) text = `➔ ${tokens[0]}`;
        else if ((c.type === 2 || c.type === 1002) && tokens[0]) text = `➔ i(${tokens[0]})`;
        else if ((c.type === 1 || c.type === 1001) && tokens[0]) text = `➔ V(${tokens[0]}, ${tokens[1] ?? '0'})`;
        if (!text) continue;
        const halfW = Math.max(text.length * 6.2 + 14, 52) / 2 / dpix;
        for (const n of nameRects) {
          const overlaps =
            cx - halfW < n.x1 && n.x0 < cx + halfW &&
            cy + 1.3 + 4 / dpix < n.y1 && n.y0 < cy + 1.3 + 20 / dpix;
          if (overlaps) {
            problems.push(
              `${ex.id}: name label "${n.text}" overlaps badge "${text}" of ${c.name}`,
            );
          }
        }
      }
    }
    expect(problems, `${problems.length} wiring problem(s):\n${problems.join('\n')}`).toHaveLength(0);
  });
});
