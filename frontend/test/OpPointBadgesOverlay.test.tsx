// @vitest-environment jsdom
import { useReducer, useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { Sheet, type SheetActions } from '../src/canvas/Sheet';
import { editorReducer, initialState } from '../src/model/store';
import type { EditorSnapshot } from '../src/model/types';

const snapshot: EditorSnapshot = {
  circuitId: 'c1',
  modelVersion: 1,
  filename: 'op-test.ipes',
  dpix: 16,
  worksheetSize: '600x600',
  components: [
    {
      type: 1,
      name: 'R1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
  ],
  connections: [
    { index: 0, type: 'LK', label: 'out', points: [[14, 10], [22, 10]] },
  ],
};

function Harness({
  simResults,
  isSimStale = false,
  onOpenScopeTab = vi.fn(),
  onRunSimulation = vi.fn(),
}: {
  simResults: Record<string, number[]> | null;
  isSimStale?: boolean;
  onOpenScopeTab?: (name: string) => void;
  onRunSimulation?: () => void;
}) {
  const [showOpBadges, setShowOpBadges] = useState(true);
  const [state, dispatch] = useReducer(editorReducer, undefined, () =>
    editorReducer(initialState, { type: 'SNAPSHOT', snapshot }),
  );
  const actions: SheetActions = {
    placeGhost: vi.fn(),
    finishWire: vi.fn(),
    commitMove: vi.fn(),
    openScopeTab: onOpenScopeTab,
    runSimulation: onRunSimulation,
  };
  return (
    <Sheet
      state={state}
      dispatch={dispatch}
      actions={actions}
      simResults={simResults}
      isSimStale={isSimStale}
      showOperatingPointBadges={showOpBadges}
      onToggleOperatingPointBadges={() => setShowOpBadges((v) => !v)}
    />
  );
}

describe('OpPointBadgesOverlay', () => {
  beforeEach(() => {
    localStorage.removeItem('gecko_show_op_badges');
    localStorage.removeItem('gecko_op_badge_metric');
  });

  afterEach(() => {
    cleanup();
    localStorage.removeItem('gecko_show_op_badges');
    localStorage.removeItem('gecko_op_badge_metric');
  });

  const mockResults = {
    time: [0, 0.001, 0.002],
    'I(R1)': [1, 3, 2],
    'V(out)': [10, 14, 12],
  };

  it('renders live operating-point badges, switches metric, and toggles visibility', () => {
    // Default Sheet hides operating-point badges unless explicitly enabled
    render(
      <Sheet
        state={{
          ...initialState,
          circuitId: 'c1',
          components: snapshot.components,
          wires: [{ index: 0, type: 'LK', points: [[15, 10], [25, 10]], label: 'out' }],
        }}
        dispatch={vi.fn()}
        actions={{ placeGhost: vi.fn(), finishWire: vi.fn(), commitMove: vi.fn() }}
        simResults={mockResults}
      />,
    );
    expect(screen.queryByTestId('op-point-badges-layer')).toBeNull();
    cleanup();

    const onOpenScopeTab = vi.fn();
    render(<Harness simResults={mockResults} onOpenScopeTab={onOpenScopeTab} />);

    const layer = screen.getByTestId('op-point-badges-layer');
    expect(layer).toBeTruthy();
    expect(screen.getByTestId('op-badge-R1').textContent).toContain('I: 2 A');
    expect(screen.getByTestId('op-badge-net:out').textContent).toContain('12 V');

    // Cycle metric: Avg -> RMS -> Pk-Pk -> I(R1) pkpk = 2 A, V(out) pkpk = 4 V
    const metricBtn = screen.getByTestId('op-badge-metric-cycle-btn');
    fireEvent.click(metricBtn); // RMS
    fireEvent.click(metricBtn); // Pk-Pk
    expect(screen.getByTestId('op-badge-net:out').textContent).toContain('4 V');

    // Click badge opens Scope tab
    fireEvent.click(screen.getByTestId('op-badge-R1'));
    expect(onOpenScopeTab).toHaveBeenCalledTimes(1);

    // Toggle off hides badges
    fireEvent.click(screen.getByTestId('toggle-op-badges'));
    expect(screen.queryByTestId('op-point-badges-layer')).toBeNull();
  });

  it('applies stale class, muted opacity, and clickable Outdated refresh pill when circuit is edited after simulation', () => {
    const onRunSimulation = vi.fn();
    render(<Harness simResults={mockResults} isSimStale onRunSimulation={onRunSimulation} />);
    const badge = screen.getByTestId('op-badge-R1');
    expect(badge.classList.contains('stale')).toBe(true);
    expect(badge.getAttribute('opacity')).toBe('0.55');

    const stalePill = screen.getByTestId('op-badges-stale-pill');
    expect(stalePill.textContent).toContain('Outdated');
    fireEvent.click(stalePill);
    expect(onRunSimulation).toHaveBeenCalledTimes(1);
  });
});
