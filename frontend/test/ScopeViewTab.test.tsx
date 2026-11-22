// @vitest-environment jsdom
import { describe, expect, it, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, cleanup, renderHook } from '@testing-library/react';
import { ScopeViewTab, sampleIndexAt, inferSignalUnit } from '../src/simulation/ScopeViewTab';
import { createBaselineSnapshot } from '../src/simulation/baselineCompare';
import { SimulationPropertiesPanel } from '../src/properties/SimulationPropertiesPanel';
import { useScopeController } from '../src/simulation/useScopeController';
import type { EditorComponent } from '../src/model/types';

afterEach(() => {
  cleanup();
});

describe('Oscilloscope GUI & Controls', () => {
  const dummyComponents: EditorComponent[] = [
    {
      type: 1003,
      name: 'SCOPE.1',
      family: 'SCOPE',
      position: [100, 100],
      orientation: 0,
      parameters: {},
      inputLabels: ['V_out', 'I_L'],
      outputLabels: [],
    },
  ];

  const dummyResults = {
    time: [0, 1e-4, 2e-4, 3e-4, 4e-4, 5e-4, 6e-4, 7e-4, 8e-4, 9e-4, 1e-3],
    V_out: [0, 50, 100, 150, 200, 250, 200, 150, 100, 50, 0],
    I_L: [0, 1, 2, 3, 4, 5, 4, 3, 2, 1, 0],
  };

  it('renders empty state when results are null', () => {
    render(
      <ScopeViewTab
        selectedScope="SCOPE.1"
        components={dummyComponents}
        results={null}
        displayLayout="overlay"
      />,
    );
    expect(screen.getByText(/No simulation data available/i)).toBeDefined();
  });

  it('renders clean DSO oscilloscope display with OSD and no top control clutter', () => {
    const { container } = render(
      <ScopeViewTab
        selectedScope="SCOPE.1"
        components={dummyComponents}
        results={dummyResults}
        displayLayout="overlay"
      />,
    );

    // OSD top bar is present
    expect(container.querySelector('.dso-screen-osd')).not.toBeNull();
    expect(screen.getAllByText('AUTO').length).toBeGreaterThanOrEqual(1);

    // Timebase readout in controls bar (Row 2)
    expect(screen.getByTestId('scope-controls-row').textContent).toContain('/div');

    // Channel tags in OSD
    expect(container.querySelector('.dso-screen-osd')?.textContent).toContain('V_out');
    expect(container.querySelector('.dso-screen-osd')?.textContent).toContain('I_L');

    // Quick-measure bar at bottom
    expect(container.querySelector('.dso-quick-measure-bar')).not.toBeNull();

    // No duplicate top control deck
    expect(container.querySelector('.dso-control-deck')).toBeNull();

    // No floating HUD by default
    expect(container.querySelector('.dso-screen-cursor-hud')).toBeNull();
  });

  it('switches flat result views cleanly without nested drawer or splitter clutter', () => {
    const { container } = render(
      <ScopeViewTab
        selectedScope="SCOPE.1"
        components={dummyComponents}
        results={dummyResults}
        displayLayout="overlay"
      />,
    );

    // No nested drawer or splitter elements exist
    expect(container.querySelector('.dso-drawer-toggle-btn')).toBeNull();
    expect(container.querySelector('.dso-drawer-splitter')).toBeNull();

    // Flat switcher tabs are directly in the view bar
    expect(screen.getByTestId('scope-view-switcher')).toBeDefined();
    expect(screen.getByTestId('scope-view-tab-curves')).toBeDefined();
    expect(screen.getByTestId('scope-view-tab-metrics')).toBeDefined();
    expect(screen.getByTestId('scope-view-tab-fft')).toBeDefined();
    expect(screen.getByTestId('scope-view-tab-losses')).toBeDefined();

    // Switch to Metrics flat view
    fireEvent.click(screen.getByTestId('scope-view-tab-metrics'));
    expect(screen.getByTestId('scope-flat-metrics')).toBeDefined();
    expect(screen.getByText('Peak-to-Peak')).toBeDefined();

    // Switch to FFT flat view
    fireEvent.click(screen.getByTestId('scope-view-tab-fft'));
    expect(screen.getByTestId('scope-flat-fft')).toBeDefined();

    // Return to Curves flat view
    fireEvent.click(screen.getByTestId('scope-view-tab-curves'));
    expect(container.querySelector('.dso-screen-bezel')).not.toBeNull();
  });

  it('renders oscilloscope controls in the right sidebar below simulation settings', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
        components: dummyComponents,
        selectedScope: 'SCOPE.1',
      }),
    );

    const { container } = render(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status={null}
        progress={0}
        components={dummyComponents}
        results={dummyResults}
        selectedScope="SCOPE.1"
        onSelectScope={() => {}}
        displayLayout="overlay"
        onDisplayLayoutChange={() => {}}
        scope={result.current}
        onRunSimulation={() => {}}
      />,
    );

    // Sidebar should contain Oscilloscope Controls section
    expect(container.querySelector('.sim-oszi-section')).not.toBeNull();
    expect(screen.getByText('Oscilloscope Controls')).toBeDefined();
    expect(screen.getByText('Horizontal (Timebase)')).toBeDefined();
    expect(screen.getByText('Vertical (Scale & Channels)')).toBeDefined();
    expect(screen.getByText('Cursors & Measurement')).toBeDefined();

    // Timebase readout and zoom buttons in sidebar
    expect(container.querySelector('.sim-oszi-badge')?.textContent).toContain('/div');
    expect(screen.getByText('Zoom +')).toBeDefined();
    expect(screen.getByText('Zoom −')).toBeDefined();
    expect(screen.getByText('⟲ Fit')).toBeDefined();

    // Channel toggles in sidebar
    const chBadges = container.querySelectorAll('.sim-channel-badge');
    expect(chBadges.length).toBe(2);
    expect(chBadges[0].textContent).toContain('V_out');
    expect(chBadges[1].textContent).toContain('I_L');
  });

  it('displays cursor measurements in the right sidebar when cursors are enabled', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
        components: dummyComponents,
        selectedScope: 'SCOPE.1',
      }),
    );

    const { container, rerender } = render(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status={null}
        progress={0}
        components={dummyComponents}
        results={dummyResults}
        selectedScope="SCOPE.1"
        onSelectScope={() => {}}
        displayLayout="overlay"
        onDisplayLayoutChange={() => {}}
        scope={result.current}
        onRunSimulation={() => {}}
      />,
    );

    // Click "Cursors: OFF" button to turn ON cursors
    const cursorToggle = screen.getByText(/📍 Cursors:/i);
    fireEvent.click(cursorToggle);

    // Re-render with updated hook state
    rerender(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status={null}
        progress={0}
        components={dummyComponents}
        results={dummyResults}
        selectedScope="SCOPE.1"
        onSelectScope={() => {}}
        displayLayout="overlay"
        onDisplayLayoutChange={() => {}}
        scope={result.current}
        onRunSimulation={() => {}}
      />,
    );

    // The single cursor measurement card appears in the sidebar
    expect(container.querySelector('.sim-cursor-card')).not.toBeNull();
    expect(container.querySelector('.sim-cursor-time-grid')).not.toBeNull();
    expect(container.querySelector('.sim-cursor-delta-row')?.textContent).toContain('Δt:');
    expect(container.querySelector('.sim-cursor-delta-row')?.textContent).toContain('1/Δt:');

    // Channel delta measurements in single table
    expect(container.querySelector('.sim-cursor-table')?.textContent).toContain('V_out');
    expect(container.querySelector('.sim-cursor-table')?.textContent).toContain('I_L');
  });

  it('enables cursors and displays measurements in sidebar when user sets cursor on the plot', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
        components: dummyComponents,
        selectedScope: 'SCOPE.1',
      }),
    );

    const { container, rerender } = render(
      <div>
        <ScopeViewTab
          selectedScope="SCOPE.1"
          components={dummyComponents}
          results={dummyResults}
          displayLayout="overlay"
          scope={result.current}
        />
        <SimulationPropertiesPanel
          circuitId="test-circuit"
          status={null}
          progress={0}
          components={dummyComponents}
          results={dummyResults}
          selectedScope="SCOPE.1"
          onSelectScope={() => {}}
          displayLayout="overlay"
          onDisplayLayoutChange={() => {}}
          scope={result.current}
          onRunSimulation={() => {}}
        />
      </div>,
    );

    // Initially cursors are OFF
    expect(result.current.cursorsEnabled).toBe(false);
    expect(screen.getByText('📍 Cursors: OFF')).toBeDefined();

    // Click SVG plot to place cursor
    const svg = container.querySelector('.full-waveform-svg');
    expect(svg).not.toBeNull();
    vi.spyOn(svg!, 'getBoundingClientRect').mockReturnValue({
      left: 0,
      top: 0,
      width: 1000,
      height: 480,
      right: 1000,
      bottom: 480,
      x: 0,
      y: 0,
      toJSON: () => {},
    });
    fireEvent.click(svg!, { clientX: 200, clientY: 200 });

    // Setting a cursor auto-enables cursors in the controller
    expect(result.current.cursorsEnabled).toBe(true);
    expect(result.current.cursorA).not.toBeNull();

    // Re-render and verify right sidebar shows Cursors: ON and measurements
    rerender(
      <div>
        <ScopeViewTab
          selectedScope="SCOPE.1"
          components={dummyComponents}
          results={dummyResults}
          displayLayout="overlay"
          scope={result.current}
        />
        <SimulationPropertiesPanel
          circuitId="test-circuit"
          status={null}
          progress={0}
          components={dummyComponents}
          results={dummyResults}
          selectedScope="SCOPE.1"
          onSelectScope={() => {}}
          displayLayout="overlay"
          onDisplayLayoutChange={() => {}}
          scope={result.current}
          onRunSimulation={() => {}}
        />
      </div>,
    );

    expect(screen.getByText('📍 Cursors: ON')).toBeDefined();
    expect(container.querySelector('.sim-cursor-card')).not.toBeNull();
  });

  it('renders clean waveform grid without confusing axis markers, using direct trace name badges', () => {
    const { container } = render(
      <ScopeViewTab
        selectedScope="SCOPE.1"
        components={dummyComponents}
        results={dummyResults}
        displayLayout="overlay"
      />,
    );

    // No confusing stacked channel markers or redundant zero arrow labels on the axis
    expect(container.querySelector('.channel-ground-marker')).toBeNull();
    expect(container.querySelector('.zero-ground-marker')).toBeNull();

    // Direct trace name badges with signal names exist on the waveform plot
    const traceBadges = container.querySelectorAll('.trace-end-badge');
    expect(traceBadges.length).toBe(2);
    const badgeTexts = Array.from(traceBadges).map((b) => b.querySelector('text')?.textContent?.trim());
    expect(badgeTexts).toContain('V_out');
    expect(badgeTexts).toContain('I_L');

    // Hovering over a trace badge highlights it
    fireEvent.mouseEnter(traceBadges[0]);
    expect(container.querySelector('.trace-end-badge.hovered')).not.toBeNull();
    fireEvent.mouseLeave(traceBadges[0]);
    expect(container.querySelector('.trace-end-badge.hovered')).toBeNull();
  });

  describe('sampleIndexAt', () => {
    const time = [0, 0.1, 0.2, 0.3, 0.4, 0.5];

    it('finds exact sample match', () => {
      expect(sampleIndexAt(time, 0.2)).toBe(2);
    });

    it('returns boundary indices when t is outside range', () => {
      expect(sampleIndexAt(time, -1.0)).toBe(0);
      expect(sampleIndexAt(time, 10.0)).toBe(5);
    });

    it('finds nearest sample in between timestamps', () => {
      expect(sampleIndexAt(time, 0.24)).toBe(3);
    });
  });

  describe('inferSignalUnit', () => {
    it('infers Volts for voltage signals', () => {
      expect(inferSignalUnit('V_out')).toBe('V');
      expect(inferSignalUnit('u_in')).toBe('V');
      expect(inferSignalUnit('probe_voltage')).toBe('V');
    });

    it('infers Amperes for current signals', () => {
      expect(inferSignalUnit('i_inductor')).toBe('A');
      expect(inferSignalUnit('current_shunt')).toBe('A');
      expect(inferSignalUnit('amp_meter')).toBe('A');
    });

    it('infers Watts for power signals', () => {
      expect(inferSignalUnit('Power_loss')).toBe('W');
      expect(inferSignalUnit('p_total')).toBe('W');
      expect(inferSignalUnit('watt_meter')).toBe('W');
    });

    it('infers degrees Celsius for temperature signals', () => {
      expect(inferSignalUnit('temperature_junction')).toBe('°C');
      expect(inferSignalUnit('th_heatsink')).toBe('°C');
      expect(inferSignalUnit('chip_c')).toBe('°C');
    });

    it('returns empty string for unrecognized signals', () => {
      expect(inferSignalUnit('duty_cycle')).toBe('');
      expect(inferSignalUnit('status_flag')).toBe('');
    });
  });

  describe('GraphRegister Mode in ScopeViewTab', () => {
    it('renders pre-simulation state with register name and runs simulation on button click', () => {
      const onRunSim = vi.fn();
      const reg = {
        id: 'osci_main',
        name: 'Osci Main View',
        numGraphs: 2 as const,
        signals: ['V(out)', 'I(L1)'],
        signalGraphs: { 'V(out)': 1 as const, 'I(L1)': 2 as const },
        hiddenSignals: [],
        yScaleMode: 'auto' as const,
        cursorsEnabled: false,
      };

      render(
        <ScopeViewTab
          selectedScope="osci_main"
          graphRegister={reg}
          components={dummyComponents}
          results={null}
          displayLayout="2-graphs"
          onRunSimulation={onRunSim}
        />,
      );

      // Check pre-sim screen renders
      expect(screen.getByTestId('scope-pre-sim-state')).not.toBeNull();
      expect(screen.getAllByText('Osci Main View').length).toBeGreaterThanOrEqual(2);
      expect(screen.getByText(/Configured for 2 signals across 2 graphs/i)).not.toBeNull();

      // Trigger run simulation button
      const runBtn = screen.getByRole('button', { name: /Run Simulation/i });
      expect(runBtn).not.toBeNull();
      fireEvent.click(runBtn);
      expect(onRunSim).toHaveBeenCalledTimes(1);
    });

    it('renders multi-graph layout switcher and updates numGraphs when clicked', () => {
      const onUpdateReg = vi.fn();
      const onLayoutChange = vi.fn();
      const reg = {
        id: 'osci_1',
        name: 'Osci 1',
        numGraphs: 1 as const,
        signals: ['V_out', 'V_in'],
        signalGraphs: {},
        hiddenSignals: [],
        yScaleMode: 'auto' as const,
        cursorsEnabled: false,
      };

      render(
        <ScopeViewTab
          selectedScope="osci_1"
          graphRegister={reg}
          components={dummyComponents}
          results={dummyResults}
          displayLayout="overlay"
          onUpdateGraphRegister={onUpdateReg}
          onDisplayLayoutChange={onLayoutChange}
        />,
      );

      // Verify layout switcher buttons exist
      const btn3G = screen.getByRole('button', { name: /3 Graphs/i });
      expect(btn3G).not.toBeNull();
      fireEvent.click(btn3G);

      expect(onUpdateReg).toHaveBeenCalledWith({ numGraphs: 3 });
      expect(onLayoutChange).toHaveBeenCalledWith('3-graphs');
    });

    it('renders empty channel assignment state when GraphRegister has 0 signals even after simulation', () => {
      const reg = {
        id: 'osci_empty',
        name: 'Osci 2',
        numGraphs: 1 as const,
        signals: [],
        signalGraphs: {},
        hiddenSignals: [],
        yScaleMode: 'auto' as const,
        cursorsEnabled: false,
      };

      render(
        <ScopeViewTab
          selectedScope="Osci 2"
          graphRegister={reg}
          components={dummyComponents}
          results={dummyResults}
          displayLayout="overlay"
        />,
      );

      expect(screen.getByTestId('scope-no-channels-state')).not.toBeNull();
      expect(screen.getByText(/No channels assigned to Osci 2/i)).not.toBeNull();
    });

    it('renders prominent simulation error banner and FAILED status pill when simulation fails', () => {
      const reg = {
        id: 'osci_1',
        name: 'Osci 1',
        numGraphs: 1 as const,
        signals: ['V_out', 'u_C'],
        signalGraphs: { V_out: 1 as const, u_C: 1 as const },
        hiddenSignals: [],
        yScaleMode: 'auto' as const,
        cursorsEnabled: false,
      };

      render(
        <ScopeViewTab
          selectedScope="Osci 1"
          graphRegister={reg}
          components={dummyComponents}
          results={null}
          displayLayout="overlay"
          status="FAILED"
          errorMessage="Simulation failed: Numerical instability of switch states at t=0.0"
          warnings={['DRC_PROBE_ERROR: Gate.1 target not found']}
        />,
      );

      const errorAlert = screen.getByTestId('scope-sim-error');
      expect(errorAlert).not.toBeNull();
      expect(errorAlert.textContent).toContain('Numerical instability of switch states at t=0.0');
      expect(errorAlert.textContent).toContain('DRC_PROBE_ERROR: Gate.1 target not found');
      expect(screen.getByText('FAILED')).not.toBeNull();
    });

    it('renders DRC warning banner and post-simulation empty-signal message when status is COMPLETED with warnings', () => {
      render(
        <ScopeViewTab
          selectedScope="all"
          components={dummyComponents}
          results={{ time: [0, 1e-4] }}
          displayLayout="overlay"
          status="COMPLETED"
          warnings={[
            "DRC_PROBE_ERROR: Gate 'GATE.1' references an unknown or non-switch power component",
            "DRC_PROBE_ERROR: Voltmeter 'VOLT.1' is not connected to any circuit node",
          ]}
        />,
      );

      const warningBanner = screen.getByTestId('scope-sim-warnings');
      expect(warningBanner).not.toBeNull();
      expect(warningBanner.textContent).toContain('Simulation Diagnostics (2 warnings)');
      expect(warningBanner.textContent).toContain("Gate 'GATE.1'");
      expect(warningBanner.textContent).toContain("Voltmeter 'VOLT.1'");
      expect(screen.getByText(/No Observable Signals Recorded/i)).not.toBeNull();
    });
  });

  describe('Pin-to-Compare Baseline', () => {
    const baselineSnap = createBaselineSnapshot(
      {
        time: [0, 2.5e-4, 5e-4, 7.5e-4, 1e-3],
        V_out: [10, 40, 80, 40, 10],
        I_L: [0.5, 1.5, 2.5, 1.5, 0.5],
      },
      1,
      'Baseline (run 1)',
    );

    it('renders direct Save Reference button in Scope bar without Tools submenu and invokes callback', () => {
      const onPin = vi.fn();
      render(
        <ScopeViewTab
          selectedScope="SCOPE.1"
          components={dummyComponents}
          results={dummyResults}
          displayLayout="overlay"
          onPinBaseline={onPin}
        />,
      );
      expect(screen.queryByTestId('scope-tools-menu-btn')).toBeNull();
      const pinBtn = screen.getByTestId('btn-pin-baseline');
      expect(pinBtn.textContent).toContain('Save Reference');
      fireEvent.click(pinBtn);
      expect(onPin).toHaveBeenCalledTimes(1);
    });

    it.each(['overlay', '2-graphs', 'stacked'] as const)(
      'renders dashed baseline ghost traces in %s layout and toggles visibility',
      (layout) => {
        const { container } = render(
          <ScopeViewTab
            selectedScope="SCOPE.1"
            components={dummyComponents}
            results={dummyResults}
            displayLayout={layout}
            baseline={baselineSnap}
            onPinBaseline={vi.fn()}
            onClearBaseline={vi.fn()}
          />,
        );

        expect(screen.getByTestId('baseline-controls')).toBeDefined();
        expect(screen.getByTestId('baseline-status-banner')).toBeDefined();
        const ghostV = container.querySelector('[data-testid="baseline-trace-V_out"]');
        const ghostI = container.querySelector('[data-testid="baseline-trace-I_L"]');
        expect(ghostV).not.toBeNull();
        expect(ghostI).not.toBeNull();
        expect(ghostV?.getAttribute('stroke-dasharray')).toBe('5 4');

        // Toggle visibility off
        fireEvent.click(screen.getByTestId('btn-toggle-baseline'));
        expect(container.querySelector('[data-testid="baseline-trace-V_out"]')).toBeNull();
      },
    );

    it('displays delta chips and delta columns in Metrics flat view', () => {
      render(
        <ScopeViewTab
          selectedScope="SCOPE.1"
          components={dummyComponents}
          results={dummyResults}
          displayLayout="overlay"
          baseline={baselineSnap}
          showBaseline
          onPinBaseline={vi.fn()}
          onClearBaseline={vi.fn()}
        />,
      );

      expect(screen.getByTestId('quick-delta-V_out')).toBeDefined();

      fireEvent.click(screen.getByTestId('scope-view-tab-metrics'));
      expect(screen.getByText('Δ Mean')).toBeDefined();
      expect(screen.getByText('Δ RMS')).toBeDefined();
      expect(screen.getByText('Δ Pk-Pk')).toBeDefined();
    });
  });

  describe('Parametric Sweep Integration', () => {
    const sweepSession = {
      batchId: 'b-test',
      parameterPath: 'R.1.param0',
      componentName: 'R.1',
      paramKey: 'param0',
      paramLabel: 'R.1 · Resistance',
      unit: 'Ω',
      mode: 'linear' as const,
      status: 'COMPLETED' as const,
      progress: 1,
      totalRuns: 2,
      completedRuns: 2,
      runs: [
        {
          index: 0,
          simulationId: 's0',
          parameterValue: 10,
          status: 'COMPLETED' as const,
          results: {
            time: [0, 5e-4, 1e-3],
            V_out: [0, 120, 0],
            I_L: [0, 3, 0],
          },
          stats: {
            V_out: { min: 0, max: 120, pkpk: 120, mean: 40, rms: 69 },
            I_L: { min: 0, max: 3, pkpk: 3, mean: 1, rms: 1.7 },
          },
        },
        {
          index: 1,
          simulationId: 's1',
          parameterValue: 20,
          status: 'COMPLETED' as const,
          results: {
            time: [0, 5e-4, 1e-3],
            V_out: [0, 200, 0],
            I_L: [0, 5, 0],
          },
          stats: {
            V_out: { min: 0, max: 200, pkpk: 200, mean: 66.7, rms: 115 },
            I_L: { min: 0, max: 5, pkpk: 5, mean: 1.7, rms: 2.9 },
          },
        },
      ],
    };

    it('renders synchronized Sweep Step Bar, updates all signal metrics on step click, and overlays ALL signals in All Curves mode', () => {
      const onApplySweepValue = vi.fn();
      const { container } = render(
        <ScopeViewTab
          selectedScope="SCOPE.1"
          components={dummyComponents}
          results={dummyResults}
          displayLayout="overlay"
          sweepSession={sweepSession}
          onApplySweepValue={onApplySweepValue}
          onClearSweep={vi.fn()}
        />,
      );

      // Step bar appears directly above waveforms; Step 0 is active by default
      expect(screen.getByTestId('sweep-step-bar')).toBeDefined();
      expect(screen.getByTestId('sweep-step-pill-0').className).toContain('active');
      expect(screen.getByTestId('sweep-step-pill-1')).toBeDefined();

      // Clicking Step 1 updates displayed values for BOTH V_out and I_L together
      fireEvent.click(screen.getByTestId('sweep-step-pill-1'));
      expect(screen.getByTestId('sweep-step-pill-1').className).toContain('active');
      const quickBarText = container.querySelector('.dso-quick-measure-bar')?.textContent ?? '';
      expect(quickBarText).toContain('200 V');
      expect(quickBarText).toContain('5 A');

      // Apply button commits selected step value
      fireEvent.click(screen.getByTestId('sweep-apply-active-btn'));
      expect(onApplySweepValue).toHaveBeenCalledWith('R.1', 'param0', 20);

      // Toggle All Curves overlays ALL visible channels (V_out and I_L)
      fireEvent.click(screen.getByTestId('sweep-all-curves-btn'));
      expect(container.querySelector('[data-testid="sweep-trace-0-V_out"]')).not.toBeNull();
      expect(container.querySelector('[data-testid="sweep-trace-1-V_out"]')).not.toBeNull();
      expect(container.querySelector('[data-testid="sweep-trace-0-I_L"]')).not.toBeNull();
      expect(container.querySelector('[data-testid="sweep-trace-1-I_L"]')).not.toBeNull();

      // Flat Sweep view tab shows SweepPanel summary trend chart
      fireEvent.click(screen.getByTestId('scope-view-tab-sweep'));
      expect(screen.getByTestId('sweep-panel')).toBeDefined();
    });
  });
});


