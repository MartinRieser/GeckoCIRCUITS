// @vitest-environment jsdom
/**
 * Integration tests for the component-centric Vary Value workflow
 * and synchronized Sweep Step selection across Scope channels and Schematic badges.
 */
import { useReducer, useState } from 'react';
import { describe, expect, it, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { PropertiesPanel } from '../src/properties/PropertiesPanel';
import { ParametricSweepModal } from '../src/modals/ParametricSweepModal';
import { ScopeViewTab } from '../src/simulation/ScopeViewTab';
import { Sheet, type SheetActions } from '../src/canvas/Sheet';
import { editorReducer, initialState } from '../src/model/store';
import { LkComponentType, ControlComponentType } from '../src/model/constants';
import type { EditorComponent, EditorSnapshot, GraphRegister } from '../src/model/types';
import type { SweepSession } from '../src/simulation/parametricSweep';

afterEach(cleanup);

const components: EditorComponent[] = [
  {
    type: LkComponentType.RESISTOR,
    name: 'R.1',
    family: 'LK',
    position: [10, 10],
    orientation: 502,
    parameters: { param0: 10 },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: LkComponentType.INDUCTOR,
    name: 'L.1',
    family: 'LK',
    position: [20, 10],
    orientation: 502,
    parameters: { param0: 100e-6, param1: 1e-3 },
    inputLabels: [],
    outputLabels: [],
  },
];

describe('Component-first Vary Value & Pre-filled Sweep', () => {
  it('launches Live Slider and pre-fills ParametricSweepModal for the selected component', () => {
    const onPinSlider = vi.fn();

    function Harness() {
      const [sweepOpen, setSweepOpen] = useState(false);
      const [prefill, setPrefill] = useState<{ comp: string; param: string } | null>(null);
      return (
        <>
          <PropertiesPanel
            component={components[1]} // L.1
            allComponents={components}
            onRename={vi.fn()}
            onSetParameter={vi.fn()}
            onSetLabel={vi.fn()}
            onPinLiveSlider={onPinSlider}
            onOpenSweepForComponent={(comp, param) => {
              setPrefill({ comp, param });
              setSweepOpen(true);
            }}
          />
          <ParametricSweepModal
            isOpen={sweepOpen}
            onClose={() => setSweepOpen(false)}
            circuitId="c1"
            components={components}
            initialComponentName={prefill?.comp}
            initialParamKey={prefill?.param}
            onStartSweep={vi.fn()}
          />
        </>
      );
    }

    render(<Harness />);

    fireEvent.click(screen.getByTestId('prop-vary-btn'));
    expect(screen.getByTestId('prop-vary-menu')).toBeTruthy();

    // Selecting component's second param (initial current) and pinning Live Slider
    const paramSelect = screen.getByTestId('prop-vary-param-select') as HTMLSelectElement;
    fireEvent.change(paramSelect, { target: { value: 'param1' } });
    fireEvent.click(screen.getByTestId('prop-pin-slider-btn'));
    expect(onPinSlider).toHaveBeenCalledWith('L.1', 'param1');

    // Re-open Vary Value and launch Parameter Sweep -> modal pre-selects L.1
    fireEvent.click(screen.getByTestId('prop-vary-btn'));
    fireEvent.click(screen.getByTestId('prop-run-sweep-btn'));
    const modalCompSelect = screen.getByTestId('sweep-component-select') as HTMLSelectElement;
    expect(modalCompSelect.value).toBe('L.1');
  });
});

describe('Synchronized Multi-Signal Sweep Step Bar & Schematic Badges', () => {
  const snapshot: EditorSnapshot = {
    circuitId: 'c1',
    modelVersion: 0,
    filename: 'buck.ipes',
    dpix: 16,
    worksheetSize: '600x600',
    components: [
      {
        type: LkComponentType.RESISTOR,
        name: 'R.1',
        family: 'LK',
        position: [10, 10],
        orientation: 502,
        parameters: { param0: 10 },
        inputLabels: [],
        outputLabels: [],
      },
      {
        type: ControlComponentType.SCOPE,
        name: 'SCOPE.1',
        family: 'SCOPE',
        position: [30, 10],
        orientation: 0,
        parameters: {},
        inputLabels: ['V(out)', 'I(R.1)'],
        outputLabels: [],
      },
    ],
    connections: [
      { index: 0, type: 'LK', points: [[15, 10], [22, 10]], label: 'out' },
    ],
  };

  const sweepSession: SweepSession = {
    batchId: 'b-sync',
    parameterPath: 'R.1.param0',
    componentName: 'R.1',
    paramKey: 'param0',
    paramLabel: 'R.1 · Resistance',
    unit: 'Ω',
    mode: 'linear',
    status: 'COMPLETED',
    progress: 1,
    totalRuns: 2,
    completedRuns: 2,
    runs: [
      {
        index: 0,
        simulationId: 's0',
        parameterValue: 10,
        status: 'COMPLETED',
        results: {
          time: [0, 5e-4, 1e-3],
          'V(out)': [10, 10, 10],
          'I(R.1)': [1, 1, 1],
        },
        stats: {
          'V(out)': { min: 10, max: 10, pkpk: 0, mean: 10, rms: 10 },
          'I(R.1)': { min: 1, max: 1, pkpk: 0, mean: 1, rms: 1 },
        },
      },
      {
        index: 1,
        simulationId: 's1',
        parameterValue: 20,
        status: 'COMPLETED',
        results: {
          time: [0, 5e-4, 1e-3],
          'V(out)': [24, 24, 24],
          'I(R.1)': [4, 4, 4],
        },
        stats: {
          'V(out)': { min: 24, max: 24, pkpk: 0, mean: 24, rms: 24 },
          'I(R.1)': { min: 4, max: 4, pkpk: 0, mean: 4, rms: 4 },
        },
      },
    ],
  };

  it('updates both Scope channels AND Sheet operating-point badges when stepping through sweep runs, and overlays all graphs in All Curves', () => {
    localStorage.setItem('gecko_show_op_badges', 'true');
    localStorage.setItem('gecko_op_badge_metric', 'mean');

    const reg: GraphRegister = {
      id: 'reg1',
      name: 'Osci 1',
      numGraphs: 2,
      signals: ['V(out)', 'I(R.1)'],
      signalGraphs: { 'V(out)': 1, 'I(R.1)': 2 },
      hiddenSignals: [],
      yScaleMode: 'auto',
      cursorsEnabled: false,
    };

    const actions: SheetActions = {
      placeGhost: vi.fn(),
      finishWire: vi.fn(),
      commitMove: vi.fn(),
    };

    function SyncHarness() {
      const [state, dispatch] = useReducer(editorReducer, initialState, (init) =>
        editorReducer(init, { type: 'SNAPSHOT', snapshot }),
      );
      const [stepIdx, setStepIdx] = useState<number | null>(0);
      const [allCurves, setAllCurves] = useState(false);

      const activeRun =
        !allCurves && stepIdx !== null
          ? sweepSession.runs.find((r) => r.index === stepIdx) ?? sweepSession.runs[0]
          : null;
      const effectiveResults = activeRun?.results ?? sweepSession.runs[0].results!;
      const previewLabel = activeRun ? `R.1 = ${activeRun.parameterValue} Ω` : null;

      return (
        <>
          <Sheet
            state={state}
            dispatch={dispatch}
            actions={actions}
            simResults={effectiveResults}
            sweepPreviewLabel={previewLabel}
          />
          <ScopeViewTab
            selectedScope="Osci 1"
            graphRegister={reg}
            components={state.components}
            results={effectiveResults}
            displayLayout="2-graphs"
            sweepSession={sweepSession}
            activeSweepStepIndex={stepIdx}
            onSelectSweepStep={setStepIdx}
            sweepOverlayAll={allCurves}
            onToggleSweepOverlayAll={setAllCurves}
          />
        </>
      );
    }

    const { container } = render(<SyncHarness />);

    // Step 0: V(out) = 10 V, I(R.1) = 1 A directly on schematic
    expect(screen.getByTestId('op-badge-net:out').textContent).toContain('10 V');
    expect(screen.getByTestId('op-badge-R.1').textContent).toContain('I: 1 A');
    expect(screen.getByTestId('canvas-sweep-preview-pill').textContent).toContain('R.1 = 10 Ω');

    // Click Step 1 (20 Ω) -> both schematic badges and Scope update together
    fireEvent.click(screen.getByTestId('sweep-step-pill-1'));
    expect(screen.getByTestId('op-badge-net:out').textContent).toContain('24 V');
    expect(screen.getByTestId('op-badge-R.1').textContent).toContain('I: 4 A');
    expect(screen.getByTestId('canvas-sweep-preview-pill').textContent).toContain('R.1 = 20 Ω');

    // Click All Curves -> in 2-graphs mode, BOTH Graph 1 (V(out)) and Graph 2 (I(R.1)) render sweep overlays
    fireEvent.click(screen.getByTestId('sweep-all-curves-btn'));
    expect(container.querySelector('[data-testid="sweep-trace-0-V(out)"]')).not.toBeNull();
    expect(container.querySelector('[data-testid="sweep-trace-1-V(out)"]')).not.toBeNull();
    expect(container.querySelector('[data-testid="sweep-trace-0-I(R.1)"]')).not.toBeNull();
    expect(container.querySelector('[data-testid="sweep-trace-1-I(R.1)"]')).not.toBeNull();
  });
});
