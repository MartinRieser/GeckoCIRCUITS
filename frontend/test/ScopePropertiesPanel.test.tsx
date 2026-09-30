// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { ScopePropertiesPanel } from '../src/properties/ScopePropertiesPanel';
import type { EditorComponent } from '../src/model/types';
import { ControlComponentType } from '../src/model/constants';
import type { ScopeController } from '../src/simulation/useScopeController';

describe('ScopePropertiesPanel Component', () => {
  afterEach(() => {
    cleanup();
  });

  const scope1Component: EditorComponent = {
    type: ControlComponentType.SCOPE,
    family: 'CONTROL',
    name: 'SCOPE.1',
    position: [20, 10],
    orientation: 503,
    parameters: {
      scopeLayout: 'stacked',
      yScaleMode: 'fixed',
      hiddenSignals: '',
    },
    inputLabels: ['uOUT', 'uIN', 'iL1'],
    outputLabels: [],
  };

  const scope2Component: EditorComponent = {
    type: ControlComponentType.SCOPE,
    family: 'CONTROL',
    name: 'SCOPE.2',
    position: [40, 10],
    orientation: 503,
    parameters: {
      scopeLayout: 'overlay',
      yScaleMode: 'auto',
      hiddenSignals: 'v_C1',
    },
    inputLabels: ['v_R1', 'v_L1', 'v_C1'],
    outputLabels: [],
  };

  const createMockScopeController = (
    channelNames: string[],
    hiddenSignals: Record<string, boolean> = {},
  ): ScopeController => ({
    view: null,
    setView: vi.fn(),
    zoomAt: vi.fn(),
    pan: vi.fn(),
    fit: vi.fn(),
    timePerDiv: 30e-6,
    dataT0: 0,
    dataT1: 300e-6,
    yScaleMode: 'fixed',
    setYScaleMode: vi.fn(),
    yView: null,
    setYView: vi.fn(),
    signalNames: ['uOUT', 'uIN', 'iL1', 'v_R1', 'v_L1', 'v_C1', 'extraneous_sig1'],
    timeArray: [0, 1e-6, 2e-6],
    signalStats: {},
    scopeChannelNames: channelNames,
    hiddenSignals,
    toggleSignal: vi.fn(),
    colorOf: (name: string) => (name === 'uOUT' ? '#38bdf8' : '#f43f5e'),
    cursorsEnabled: false,
    setCursorsEnabled: vi.fn(),
    cursorA: null,
    setCursorA: vi.fn(),
    cursorB: null,
    setCursorB: vi.fn(),
    activeCursor: 'A',
    setActiveCursor: vi.fn(),
    setCursor: vi.fn(),
    setCursorPreset: vi.fn(),
    clearCursors: vi.fn(),
    cursorMeasurements: null,
    drawerTab: null,
    setDrawerTab: vi.fn(),
  });

  it('renders strictly scope settings and does NOT show simulation solver parameters or duplicate run buttons', () => {
    const mockScope = createMockScopeController(['uOUT', 'uIN', 'iL1']);
    render(
      <ScopePropertiesPanel
        scopeComponent={scope1Component}
        results={{ uOUT: [1, 2, 3], uIN: [10, 10, 10], iL1: [0.1, 0.2, 0.3] }}
        displayLayout="stacked"
        onDisplayLayoutChange={vi.fn()}
        scope={mockScope}
        onSetParameter={vi.fn()}
      />,
    );

    // Verify title and channel badge
    expect(screen.getByText('Scope Settings: SCOPE.1')).not.toBeNull();
    expect(screen.getByText('3 ch')).not.toBeNull();

    // Verify Display Layout exists
    expect(screen.getByText('Display Layout')).not.toBeNull();
    expect(screen.getByRole('button', { name: /📈 Overlay/i })).not.toBeNull();
    expect(screen.getByRole('button', { name: /📑 Stacked Lanes/i })).not.toBeNull();

    // Verify NO global simulation solver settings or run buttons
    expect(screen.queryByText(/▶ Run Simulation/i)).toBeNull();
    expect(screen.queryByText(/Duration \(tEnd\)/i)).toBeNull();
    expect(screen.queryByText(/Time Step \(dt\)/i)).toBeNull();
    expect(screen.queryByText(/Integration Method/i)).toBeNull();
    expect(screen.queryByText(/All Scopes & Signals/i)).toBeNull();

    // Verify only this scope's channels are listed
    expect(screen.getByText('uOUT')).not.toBeNull();
    expect(screen.getByText('uIN')).not.toBeNull();
    expect(screen.getByText('iL1')).not.toBeNull();
    // extraneous signal from other parts of circuit should NOT be present
    expect(screen.queryByText('extraneous_sig1')).toBeNull();
    expect(screen.queryByText('v_R1')).toBeNull();
  });

  it('persists layout change to component parameters when user changes display layout', () => {
    const mockScope = createMockScopeController(['uOUT', 'uIN', 'iL1']);
    const onLayoutChange = vi.fn();
    const onSetParameter = vi.fn();

    render(
      <ScopePropertiesPanel
        scopeComponent={scope1Component}
        results={null}
        displayLayout="stacked"
        onDisplayLayoutChange={onLayoutChange}
        scope={mockScope}
        onSetParameter={onSetParameter}
      />,
    );

    const overlayBtn = screen.getByRole('button', { name: /📈 Overlay/i });
    fireEvent.click(overlayBtn);

    expect(onLayoutChange).toHaveBeenCalledWith('overlay');
    expect(onSetParameter).toHaveBeenCalledWith('SCOPE.1', 'scopeLayout', 'overlay');
  });

  it('persists vertical scale mode change and channel visibility toggle to component parameters', () => {
    const mockScope = createMockScopeController(['uOUT', 'uIN', 'iL1']);
    const onSetParameter = vi.fn();

    render(
      <ScopePropertiesPanel
        scopeComponent={scope1Component}
        results={null}
        displayLayout="stacked"
        onDisplayLayoutChange={vi.fn()}
        scope={mockScope}
        onSetParameter={onSetParameter}
      />,
    );

    // Toggle scale mode
    const scaleBtn = screen.getByRole('button', { name: /↕ Scale:/i });
    fireEvent.click(scaleBtn);

    expect(mockScope.setYScaleMode).toHaveBeenCalledWith('auto');
    expect(onSetParameter).toHaveBeenCalledWith('SCOPE.1', 'yScaleMode', 'auto');

    // Toggle a channel
    const uOutBtn = screen.getByRole('button', { name: /uOUT/i });
    fireEvent.click(uOutBtn);

    expect(mockScope.toggleSignal).toHaveBeenCalledWith('uOUT');
    expect(onSetParameter).toHaveBeenCalledWith('SCOPE.1', 'hiddenSignals', 'uOUT');
  });

  it('exports CSV with strictly this scope channels', () => {
    const mockScope = createMockScopeController(['uOUT', 'uIN']);
    const createObjectUrlMock = vi.fn().mockReturnValue('blob:test');
    const revokeObjectUrlMock = vi.fn();
    window.URL.createObjectURL = createObjectUrlMock;
    window.URL.revokeObjectURL = revokeObjectUrlMock;

    render(
      <ScopePropertiesPanel
        scopeComponent={scope1Component}
        results={{ uOUT: [1, 2], uIN: [10, 10], extraneous_sig1: [99, 99] }}
        displayLayout="stacked"
        onDisplayLayoutChange={vi.fn()}
        scope={mockScope}
        onSetParameter={vi.fn()}
      />,
    );

    const exportBtn = screen.getByRole('button', { name: /Export CSV File/i });
    expect(exportBtn).not.toBeNull();
    fireEvent.click(exportBtn);

    expect(createObjectUrlMock).toHaveBeenCalled();
  });

  it('displays separate channel lists and settings for different scope blocks', () => {
    const mockScope1 = createMockScopeController(['uOUT', 'uIN', 'iL1']);
    const mockScope2 = createMockScopeController(['v_R1', 'v_L1', 'v_C1']);

    const { rerender } = render(
      <ScopePropertiesPanel
        scopeComponent={scope1Component}
        results={null}
        displayLayout="stacked"
        onDisplayLayoutChange={vi.fn()}
        scope={mockScope1}
        onSetParameter={vi.fn()}
      />,
    );

    expect(screen.getByText('Scope Settings: SCOPE.1')).not.toBeNull();
    expect(screen.getByText('uOUT')).not.toBeNull();
    expect(screen.queryByText('v_R1')).toBeNull();

    // Rerender for SCOPE.2
    rerender(
      <ScopePropertiesPanel
        scopeComponent={scope2Component}
        results={null}
        displayLayout="overlay"
        onDisplayLayoutChange={vi.fn()}
        scope={mockScope2}
        onSetParameter={vi.fn()}
      />,
    );

    expect(screen.getByText('Scope Settings: SCOPE.2')).not.toBeNull();
    expect(screen.getByText('v_R1')).not.toBeNull();
    expect(screen.getByText('v_L1')).not.toBeNull();
    expect(screen.queryByText('uOUT')).toBeNull();
  });
});
