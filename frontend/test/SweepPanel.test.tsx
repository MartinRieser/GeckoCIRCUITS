// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { SweepPanel } from '../src/simulation/SweepPanel';
import type { SweepSession } from '../src/simulation/parametricSweep';

afterEach(() => cleanup());

describe('SweepPanel', () => {
  const session: SweepSession = {
    batchId: 'batch-1',
    parameterPath: 'R.1.param0',
    componentName: 'R.1',
    paramKey: 'param0',
    paramLabel: 'R.1 · Resistance',
    unit: 'Ω',
    mode: 'linear',
    status: 'COMPLETED',
    progress: 1,
    totalRuns: 3,
    completedRuns: 3,
    runs: [
      {
        index: 0,
        simulationId: 'sim-0',
        parameterValue: 10,
        status: 'COMPLETED',
        stats: { V_out: { min: 4, max: 6, pkpk: 2, mean: 5, rms: 5.1 } },
      },
      {
        index: 1,
        simulationId: 'sim-1',
        parameterValue: 20,
        status: 'COMPLETED',
        stats: { V_out: { min: 7, max: 9, pkpk: 2, mean: 8, rms: 8.1 } },
      },
      {
        index: 2,
        simulationId: 'sim-2',
        parameterValue: 30,
        status: 'COMPLETED',
        stats: { V_out: { min: 9, max: 11, pkpk: 2, mean: 10, rms: 10.1 } },
      },
    ],
  };

  it('renders XY SVG summary points, switches metrics, and applies selected run value', () => {
    const onHighlight = vi.fn();
    const onApply = vi.fn();
    const onToggleOverlay = vi.fn();

    render(
      <SweepPanel
        sweepSession={session}
        availableSignals={['V_out']}
        overlayEnabled
        onToggleOverlay={onToggleOverlay}
        selectedSignal="V_out"
        onSelectSignal={vi.fn()}
        highlightedRunIndex={null}
        onHighlightRun={onHighlight}
        onApplyValue={onApply}
        onClearSweep={vi.fn()}
      />,
    );

    expect(screen.getByTestId('sweep-xy-chart')).toBeDefined();
    expect(screen.getByTestId('sweep-xy-point-0')).toBeDefined();
    expect(screen.getByTestId('sweep-xy-point-1')).toBeDefined();
    expect(screen.getByTestId('sweep-xy-point-2')).toBeDefined();

    // Switch metric to RMS
    fireEvent.change(screen.getByTestId('sweep-metric-select'), { target: { value: 'rms' } });

    // Toggle overlay
    fireEvent.click(screen.getByTestId('sweep-overlay-checkbox'));
    expect(onToggleOverlay).toHaveBeenCalledWith(false);

    // Highlight run #2 by clicking row
    fireEvent.click(screen.getByTestId('sweep-row-1'));
    expect(onHighlight).toHaveBeenCalledWith(1);

    // Click Apply on run #3 (30 Ohm)
    fireEvent.click(screen.getByTestId('sweep-apply-btn-2'));
    expect(onApply).toHaveBeenCalledWith('R.1', 'param0', 30);
  });
});
