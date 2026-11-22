import { describe, expect, it } from 'vitest';
import { buildCleanSignalCatalog } from '../src/simulation/signalCatalog';
import { LkComponentType, Orientation } from '../src/model/constants';
import type { EditorComponent } from '../src/model/types';

describe('signalCatalog', () => {
  const components: EditorComponent[] = [
    {
      type: LkComponentType.MOSFET,
      name: 'S.1',
      family: 'LK',
      position: [10, 10],
      orientation: Orientation.WEST_EAST,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: LkComponentType.INDUCTOR,
      name: 'L.1',
      family: 'LK',
      position: [20, 10],
      orientation: Orientation.WEST_EAST,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
  ];
  const wires = [
    { label: 'out', type: 'LK', domain: 'electrical' },
    { label: 'pwm', type: 'CONTROL', domain: 'CONTROL' },
  ];

  it('hides ground, duplicate bare net aliases, and unrecorded Ploss/Tj/Phi by default', () => {
    const list = buildCleanSignalCatalog({
      components,
      wires,
      hideDuplicatesAndSpeculative: true,
    });
    const names = list.map((s) => s.name);

    expect(names).toContain('V(out)');
    expect(names).toContain('I(L.1)');
    expect(names).toContain('pwm');
    expect(names).not.toContain('0');
    expect(names).not.toContain('out'); // bare duplicate hidden when V(out) exists
    expect(names).not.toContain('Ploss_S.1');
    expect(names).not.toContain('Tj_S.1');
    expect(names).not.toContain('Phi_L.1');
  });

  it('includes speculative signals when actually recorded in simulation results or already assigned', () => {
    const list = buildCleanSignalCatalog({
      components,
      wires,
      results: { time: [0, 1], Ploss_S1: [], 'Ploss_S.1': [1.2, 1.4] },
      assignedChannels: ['out', 'Tj_S.1'],
      hideDuplicatesAndSpeculative: true,
    });
    const names = list.map((s) => s.name);

    expect(names).toContain('Ploss_S.1'); // recorded with real samples
    expect(names).toContain('Tj_S.1'); // already assigned
    expect(names).toContain('out'); // legacy assigned channel preserved
    expect(names).not.toContain('Phi_L.1');
  });
});
