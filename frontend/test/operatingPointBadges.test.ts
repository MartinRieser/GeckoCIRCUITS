import { describe, expect, it } from 'vitest';
import {
  buildOperatingPointBadges,
  layoutOperatingPointBadges,
} from '../src/canvas/operatingPointBadges';
import { ControlComponentType, LkComponentType, Orientation, SpecialComponentType } from '../src/model/constants';
import type { EditorComponent, EditorWire } from '../src/model/types';

function makeComponent(partial: Partial<EditorComponent> & { name: string; type: number; family: string }): EditorComponent {
  return {
    uid: 1,
    position: [10, 10],
    orientation: Orientation.WEST_EAST,
    parameters: {},
    inputLabels: [],
    outputLabels: [],
    ...partial,
  };
}

describe('operatingPointBadges', () => {
  it('groups V(L.1) and I(L.1) onto a single badge and ignores ground signals and textboxes', () => {
    const components: EditorComponent[] = [
      makeComponent({ name: 'L.1', type: LkComponentType.INDUCTOR, family: 'LK', position: [12, 8] }),
      makeComponent({ name: 'NOTE.1', type: SpecialComponentType.TEXTBOX, family: 'SPECIAL', position: [5, 5] }),
    ];
    const results = {
      time: [0, 0.001, 0.002, 0.003],
      'V(L.1)': [10, 14, 10, 14],
      'I(L.1)': [3, 5, 3, 5],
      'V(NOTE.1)': [1, 1, 1, 1],
      'V(0)': [0, 0, 0, 0],
    };

    const badges = buildOperatingPointBadges(components, [], results, 'mean');
    expect(badges).toHaveLength(1);
    expect(badges[0].id).toBe('L.1');
    expect(badges[0].lines).toEqual(['V: 12 V', 'I: 4 A']);
    expect(badges[0].tooltip).toContain('Mean 12 V');
    expect(badges[0].tooltip).toContain('RMS');

    const pkpk = buildOperatingPointBadges(components, [], results, 'pkpk');
    expect(pkpk[0].lines).toEqual(['V: 4 V', 'I: 2 A']);

    const max = buildOperatingPointBadges(components, [], results, 'max');
    expect(max[0].lines).toEqual(['V: 14 V', 'I: 5 A']);
  });

  it('places V(out) badge at midpoint of the longest wire segment carrying net label out', () => {
    const wires: EditorWire[] = [
      {
        index: 0,
        type: 'LK',
        label: 'out',
        points: [
          [10, 10],
          [12, 10],
          [12, 20],
        ],
      },
    ];
    const results = {
      time: [0, 1],
      'V(out)': [24, 24],
    };

    const badges = buildOperatingPointBadges([], wires, results, 'rms');
    expect(badges).toHaveLength(1);
    expect(badges[0].id).toBe('net:out');
    expect(badges[0].anchorX).toBe(12);
    expect(badges[0].anchorY).toBe(15);
    expect(badges[0].lines).toEqual(['24 V']);
  });

  it('resolves ammeter outputLabels to its coupled target component and handles Ploss_/Tj_', () => {
    const components: EditorComponent[] = [
      makeComponent({ name: 'S1', type: LkComponentType.MOSFET, family: 'LK', position: [14, 12] }),
      makeComponent({
        name: 'AMP.1',
        type: ControlComponentType.AMMETER,
        family: 'CONTROL',
        position: [20, 20],
        parameters: { coupledComponent: 'S1' },
        outputLabels: ['iS1'],
      }),
    ];
    const results = {
      time: [0, 1],
      iS1: [2, 6],
      Ploss_S1: [1.5, 2.5],
      Tj_S1: [65, 75],
    };

    const badges = buildOperatingPointBadges(components, [], results, 'mean');
    expect(badges).toHaveLength(1);
    expect(badges[0].id).toBe('S1');
    expect(badges[0].lines).toEqual(['I: 4 A', 'P: 2 W', 'Tj: 70 °C']);
  });

  it('avoids overlapping occupied probe rects and previously placed badges', () => {
    const dpix = 16;
    const badgeA = {
      id: 'R1',
      anchorX: 10,
      anchorY: 10,
      kind: 'voltage' as const,
      lines: ['V: 12 V'],
      tooltip: 'V',
      signals: ['V(R1)'],
    };
    const badgeB = {
      id: 'R2',
      anchorX: 10,
      anchorY: 10,
      kind: 'current' as const,
      lines: ['I: 2 A'],
      tooltip: 'I',
      signals: ['I(R2)'],
    };

    const ax = 10 * dpix;
    const ay = 10 * dpix;
    // Block the default below-right candidate slot (ax + 14, ay + 14) with a probe box
    const occupied = [{ x: ax + 14, y: ay + 14, w: 60, h: 20 }];
    const placed = layoutOperatingPointBadges([badgeA, badgeB], dpix, occupied);

    expect(placed).toHaveLength(2);
    // Neither badge should overlap the occupied slot or each other
    const a = placed[0];
    const b = placed[1];
    const overlapsOccupied =
      a.px < occupied[0].x + occupied[0].w + 4 &&
      a.px + a.width + 4 > occupied[0].x &&
      a.py < occupied[0].y + occupied[0].h + 4 &&
      a.py + a.height + 4 > occupied[0].y;
    expect(overlapsOccupied).toBe(false);
    const overlap =
      a.px < b.px + b.width &&
      a.px + a.width > b.px &&
      a.py < b.py + b.height &&
      a.py + a.height > b.py;
    expect(overlap).toBe(false);
  });

  it('deduplicates alias net signals (out & V(out)) and omits Volt unit on CONTROL wires', () => {
    const wires: EditorWire[] = [
      { index: 0, type: 'LK', label: 'out', points: [[10, 10], [20, 10]] },
      { index: 1, type: 'CONTROL', label: 'pwm', points: [[10, 18], [20, 18]] },
    ];
    const results = {
      time: [0, 1],
      out: [12, 12],
      'V(out)': [12, 12],
      pwm: [0.5, 0.5],
    };

    const badges = buildOperatingPointBadges([], wires, results, 'mean');
    const outBadge = badges.find((b) => b.id === 'net:out');
    const pwmBadge = badges.find((b) => b.id === 'net:pwm');
    expect(outBadge?.lines).toEqual(['12 V']);
    expect(outBadge?.targetLabel).toBe('out');
    expect(pwmBadge?.lines).toEqual(['0.5']);
    expect(pwmBadge?.kind).toBe('other');
  });
});
