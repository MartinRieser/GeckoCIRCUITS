import { describe, expect, it } from 'vitest';
import {
  exportSweepSummaryCsv,
  generateSweepValues,
  listSweepableParameters,
  sweepRunColor,
  type SweepSession,
} from '../src/simulation/parametricSweep';
import type { EditorComponent } from '../src/model/types';
import { ControlComponentType, LkComponentType, SpecialComponentType } from '../src/model/constants';

describe('parametricSweep utilities', () => {
  const comps: EditorComponent[] = [
    {
      type: LkComponentType.INDUCTOR,
      name: 'L.1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: { param0: 220e-6, param1: 0 },
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: LkComponentType.VOLTAGE_SOURCE,
      name: 'U.1',
      family: 'LK',
      position: [4, 10],
      orientation: 501,
      parameters: { param0: 401, param1: 24 }, // 401 = DC mode
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: SpecialComponentType.TEXTBOX,
      name: 'TXT.1',
      family: 'SPECIAL',
      position: [20, 20],
      orientation: 501,
      parameters: { text: 'Note' },
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: ControlComponentType.SCOPE,
      name: 'SCOPE.1',
      family: 'CONTROL',
      position: [30, 10],
      orientation: 502,
      parameters: {},
      inputLabels: ['V_out'],
      outputLabels: [],
    },
  ];

  it('listSweepableParameters discovers visible numeric params and skips enums/textboxes/scopes', () => {
    const params = listSweepableParameters(comps);
    const paths = params.map((p) => p.parameterPath);
    expect(paths).toContain('L.1.param0');
    expect(paths).toContain('U.1.param1');
    // Enum mode selector param0 on U.1 should be excluded
    expect(paths).not.toContain('U.1.param0');
    expect(paths.some((p) => p.startsWith('TXT.1'))).toBe(false);
    expect(paths.some((p) => p.startsWith('SCOPE.1'))).toBe(false);

    const lParam = params.find((p) => p.parameterPath === 'L.1.param0');
    expect(lParam?.currentValue).toBeCloseTo(220e-6, 9);
  });

  it('generateSweepValues computes exact linear and log grids', () => {
    expect(generateSweepValues('linear', 10, 50, 5)).toEqual([10, 20, 30, 40, 50]);

    const logVals = generateSweepValues('log', 1, 1000, 4);
    expect(logVals).toHaveLength(4);
    expect(logVals[0]).toBeCloseTo(1, 8);
    expect(logVals[1]).toBeCloseTo(10, 8);
    expect(logVals[2]).toBeCloseTo(100, 8);
    expect(logVals[3]).toBeCloseTo(1000, 8);

    expect(generateSweepValues('log', 0, 100, 3)).toEqual([]);
  });

  it('sweepRunColor returns distinct valid HSL strings for dark and light themes', () => {
    const c0 = sweepRunColor(0, 5, 'dark');
    const c4 = sweepRunColor(4, 5, 'dark');
    const cLight = sweepRunColor(2, 5, 'light');
    expect(c0).toMatch(/^hsl\(\d+, 82%, 62%\)$/);
    expect(cLight).toMatch(/^hsl\(\d+, 82%, 42%\)$/);
    expect(c0).not.toBe(c4);
  });

  it('exportSweepSummaryCsv formats run summary metrics cleanly', () => {
    const session: SweepSession = {
      batchId: 'b1',
      parameterPath: 'L.1.param0',
      componentName: 'L.1',
      paramKey: 'param0',
      paramLabel: 'L.1 · Inductance',
      unit: 'H',
      mode: 'linear',
      status: 'COMPLETED',
      progress: 1,
      totalRuns: 2,
      completedRuns: 2,
      runs: [
        {
          index: 0,
          simulationId: 's1',
          parameterValue: 1e-4,
          status: 'COMPLETED',
          stats: { V_out: { min: 11, max: 13, pkpk: 2, mean: 12, rms: 12.01 } },
        },
        {
          index: 1,
          simulationId: 's2',
          parameterValue: 2e-4,
          status: 'COMPLETED',
          stats: { V_out: { min: 11.5, max: 12.5, pkpk: 1, mean: 12, rms: 12.0 } },
        },
      ],
    };

    const csv = exportSweepSummaryCsv(session, 'V_out');
    const lines = csv.split('\n');
    expect(lines).toHaveLength(3);
    expect(lines[0]).toBe('run,L.1 · Inductance (H),status,min,max,pkpk,mean,rms');
    expect(lines[1]).toBe('1,0.0001,COMPLETED,11,13,2,12,12.01');
  });
});
