// @vitest-environment jsdom
import { describe, expect, it, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup, renderHook } from '@testing-library/react';
import { ScopeViewTab } from '../src/simulation/ScopeViewTab';
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

    // Timebase readout in OSD
    expect(container.querySelector('.dso-screen-osd')?.textContent).toContain('/div');

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

  it('toggles collapsible analysis drawer when clicking drawer toggle button without duplicate cursor tab', () => {
    const { container } = render(
      <ScopeViewTab
        selectedScope="SCOPE.1"
        components={dummyComponents}
        results={dummyResults}
        displayLayout="overlay"
      />,
    );

    // Click Analysis Drawer toggle button
    const drawerBtn = container.querySelector('.dso-drawer-toggle-btn');
    expect(drawerBtn).not.toBeNull();
    fireEvent.click(drawerBtn!);

    // Drawer should open with tab buttons
    expect(screen.getByText('📊 Signal Statistics')).toBeDefined();
    expect(screen.getByText('〰 FFT Spectrum')).toBeDefined();
    expect(screen.getByText('⚡ Semiconductor Losses')).toBeDefined();
    expect(screen.getByText('✕ Minimize')).toBeDefined();

    // No duplicate cursor breakdown tab in drawer
    expect(screen.queryByText('📐 Cursor Breakdown')).toBeNull();

    // Click Minimize button
    fireEvent.click(screen.getByText('✕ Minimize'));

    // Drawer should be collapsed
    expect(screen.queryByText('✕ Minimize')).toBeNull();
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

  it('displays cursor measurements in ONE single location (the right sidebar) when cursors are enabled', () => {
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
});
