import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import type { EditorComponent, EditorWire } from '../src/model/types';
import { LkComponentType } from '../src/model/constants';
import { ScopeChannelManager } from '../src/simulation/ScopeChannelManager';

describe('ScopeChannelManager Component', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    scopeName: 'SCOPE',
    assignedChannels: ['v_sw', 'v_out'],
    allAvailableSignals: ['v_sw', 'v_out', 'v_in', 'i_l', 'duty', 'clk'],
    colorOf: vi.fn((name: string) => (name === 'v_sw' ? '#38bdf8' : '#fb923c')),
    onAddChannel: vi.fn(),
    onRemoveChannel: vi.fn(),
    onAutoAddProbes: vi.fn(),
    onClearChannels: vi.fn(),
  };

  it('renders modal with scope title and active channel badges', () => {
    render(<ScopeChannelManager {...defaultProps} />);

    expect(screen.getByTestId('scope-channel-manager-modal')).toBeDefined();
    expect(screen.getByText('Scope Channel Manager: SCOPE')).toBeDefined();
    expect(screen.getByText('Active Channels (2)')).toBeDefined();

    // v_sw and v_out are displayed as active
    expect(screen.getAllByText('CH1').length).toBeGreaterThan(0);
    expect(screen.getAllByText('CH2').length).toBeGreaterThan(0);
  });

  it('filters signals by search query', () => {
    render(<ScopeChannelManager {...defaultProps} />);

    const searchInput = screen.getByPlaceholderText('Filter signals (e.g. u_out, duty, i_L)...');
    fireEvent.change(searchInput, { target: { value: 'duty' } });

    // duty should be visible
    expect(screen.getByText('duty')).toBeDefined();
    // v_sw should not be in the filtered list
    expect(screen.queryByText('+ Add to Scope')).toBeDefined();
  });

  it('calls onAddChannel when clicking + Add to Scope', () => {
    const onAddChannel = vi.fn();
    render(<ScopeChannelManager {...defaultProps} onAddChannel={onAddChannel} />);

    // Click "+ Add to Scope" for v_in
    const addButtons = screen.getAllByText('+ Add to Scope');
    expect(addButtons.length).toBeGreaterThan(0);
    fireEvent.click(addButtons[0]);

    expect(onAddChannel).toHaveBeenCalled();
  });

  it('calls onRemoveChannel when clicking Remove on an assigned channel', () => {
    const onRemoveChannel = vi.fn();
    render(<ScopeChannelManager {...defaultProps} onRemoveChannel={onRemoveChannel} />);

    const removeButtons = screen.getAllByText('Remove');
    expect(removeButtons.length).toBeGreaterThan(0);
    fireEvent.click(removeButtons[0]);

    expect(onRemoveChannel).toHaveBeenCalledWith('v_sw');
  });

  it('calls onAutoAddProbes when clicking Auto-Add Probes', () => {
    const onAutoAddProbes = vi.fn();
    render(<ScopeChannelManager {...defaultProps} onAutoAddProbes={onAutoAddProbes} />);

    const autoProbesBtn = screen.getByText('Auto-Add Probes');
    fireEvent.click(autoProbesBtn);

    expect(onAutoAddProbes).toHaveBeenCalledTimes(1);
  });

  it('calls onClearChannels when clicking Clear All', () => {
    const onClearChannels = vi.fn();
    render(<ScopeChannelManager {...defaultProps} onClearChannels={onClearChannels} />);

    const clearBtn = screen.getByText('Clear All');
    fireEvent.click(clearBtn);

    expect(onClearChannels).toHaveBeenCalledTimes(1);
  });

  it('closes on Done button click or close icon', () => {
    const onClose = vi.fn();
    render(<ScopeChannelManager {...defaultProps} onClose={onClose} />);

    const doneBtn = screen.getByText('Done');
    fireEvent.click(doneBtn);
    expect(onClose).toHaveBeenCalled();
  });

  it('triggers onDisplayLayoutChange and displays graph assignment buttons in 2-graphs mode', () => {
    const onDisplayLayoutChange = vi.fn();
    const onSetSignalGraph = vi.fn();
    const onAutoGroupByCategory = vi.fn();

    render(
      <ScopeChannelManager
        {...defaultProps}
        displayLayout="2-graphs"
        onDisplayLayoutChange={onDisplayLayoutChange}
        onSetSignalGraph={onSetSignalGraph}
        onAutoGroupByCategory={onAutoGroupByCategory}
        signalGraphs={{ v_sw: 1, v_out: 2 }}
      />,
    );

    // Layout buttons are rendered
    expect(screen.getByText('📊 2 Graphs')).toBeDefined();
    expect(screen.getByText('📑 3 Graphs')).toBeDefined();

    // Auto-Group button is rendered
    const autoGroupBtn = screen.getByText('Auto-Group (V / I / Ctrl)');
    fireEvent.click(autoGroupBtn);
    expect(onAutoGroupByCategory).toHaveBeenCalledTimes(1);

    // Change layout to 3-graphs
    fireEvent.click(screen.getByText('📑 3 Graphs'));
    expect(onDisplayLayoutChange).toHaveBeenCalledWith('3-graphs');

    // Click G2 button on v_sw
    const g2Buttons = screen.getAllByText('G2');
    expect(g2Buttons.length).toBeGreaterThan(0);
    fireEvent.click(g2Buttons[0]);
    expect(onSetSignalGraph).toHaveBeenCalled();
  });

  describe('Phase 2 Hierarchical Signal Tree & Multi-Graph Probing', () => {
    const mockComponents: EditorComponent[] = [
      {
        name: 'L_filter',
        type: LkComponentType.INDUCTOR,
        family: 'CIRCUIT',
        position: [10, 10],
        orientation: 501,
        parameters: { L: 1e-3 },
        inputLabels: [],
        outputLabels: [],
      },
      {
        name: 'S.1',
        type: 201,
        family: 'CIRCUIT',
        position: [20, 20],
        orientation: 501,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      },
      {
        name: 'PWM_GEN',
        type: 501,
        family: 'CONTROL',
        position: [30, 30],
        orientation: 501,
        parameters: {},
        inputLabels: [],
        outputLabels: ['pwm_a'],
      },
    ];

    const mockWires: EditorWire[] = [
      { index: 0, type: 'LK', points: [[10, 10], [20, 10]], label: 'dc_pos' },
      { index: 1, type: 'LK', points: [[20, 20], [30, 20]], label: 'load_p' },
    ];

    it('renders rich hierarchical Tree View with Power, Nets, Control, and Thermal groups', () => {
      render(
        <ScopeChannelManager
          {...defaultProps}
          components={mockComponents}
          wires={mockWires}
          assignedChannels={['I(L_filter)']}
        />,
      );

      // Top-level sections rendered
      expect(screen.getByTestId('scope-tree-power-section')).toBeDefined();
      expect(screen.getByTestId('scope-tree-nets-section')).toBeDefined();
      expect(screen.getByTestId('scope-tree-control-section')).toBeDefined();
      expect(screen.getByTestId('scope-tree-thermal-section')).toBeDefined();

      // Power components
      expect(screen.getByText('L_filter')).toBeDefined();
      expect(screen.getByText('S.1')).toBeDefined();
      expect(screen.getAllByText('I(L_filter)').length).toBeGreaterThan(0);
      expect(screen.getByText('V(L_filter)')).toBeDefined();
      expect(screen.getByText('I(S.1)')).toBeDefined();
      expect(screen.getByText('V(S.1)')).toBeDefined();
      expect(screen.getByText('Ploss_S.1')).toBeDefined();

      // Nets
      expect(screen.getByText('V(dc_pos)')).toBeDefined();
      expect(screen.getByText('V(load_p)')).toBeDefined();

      // Control
      expect(screen.getByText('pwm_a')).toBeDefined();

      // Thermal & Magnetic
      expect(screen.getByText('Tj_S.1')).toBeDefined();
      expect(screen.getByText('Phi_L_filter')).toBeDefined();
    });

    it('switches between Tree View and Flat View seamlessly', () => {
      render(
        <ScopeChannelManager
          {...defaultProps}
          components={mockComponents}
          wires={mockWires}
        />,
      );

      // Initially in Tree View
      expect(screen.getByTestId('scope-picker-tree-view-btn')).toBeDefined();
      expect(screen.getByTestId('scope-tree-power-section')).toBeDefined();

      // Switch to Flat View
      fireEvent.click(screen.getByTestId('scope-picker-flat-view-btn'));
      expect(screen.queryByTestId('scope-tree-power-section')).toBeNull();
      expect(screen.getByText('⚡ Voltages')).toBeDefined();
      expect(screen.getByText('🔌 Currents')).toBeDefined();

      // Switch back to Tree View
      fireEvent.click(screen.getByTestId('scope-picker-tree-view-btn'));
      expect(screen.getByTestId('scope-tree-power-section')).toBeDefined();
    });

    it('supports collapsing and expanding tree sections and component nodes', () => {
      render(
        <ScopeChannelManager
          {...defaultProps}
          components={mockComponents}
          wires={mockWires}
        />,
      );

      // Collapse Power section
      const powerHeader = screen.getByText('Power Components');
      fireEvent.click(powerHeader);
      // Components inside power section should be hidden
      expect(screen.queryByTestId('scope-tree-comp-L_filter')).toBeNull();

      // Re-expand Power section
      fireEvent.click(powerHeader);
      expect(screen.getByTestId('scope-tree-comp-L_filter')).toBeDefined();

      // Toggle individual component node
      const compNode = screen.getByTestId('scope-tree-comp-L_filter');
      const compHeader = compNode.querySelector('div')!;
      fireEvent.click(compHeader);
      // Collapsed: signal row V(L_filter) hidden
      expect(screen.queryByTestId('scope-signal-row-V(L_filter)')).toBeNull();

      // Expand again
      fireEvent.click(compHeader);
      expect(screen.getByTestId('scope-signal-row-V(L_filter)')).toBeDefined();
    });

    it('allows adding signals directly to specific subplots (+ G1, + G2) in multi-graph mode', () => {
      const onAddChannel = vi.fn();
      render(
        <ScopeChannelManager
          {...defaultProps}
          displayLayout="2-graphs"
          components={mockComponents}
          wires={mockWires}
          onAddChannel={onAddChannel}
        />,
      );

      // Find direct "+ G2" button on V(L_filter) row
      const vFilterRow = screen.getByTestId('scope-signal-row-V(L_filter)');
      const g2Btn = Array.from(vFilterRow.querySelectorAll('button')).find((b) => b.textContent?.includes('+ G2'));
      expect(g2Btn).toBeDefined();
      fireEvent.click(g2Btn!);

      expect(onAddChannel).toHaveBeenCalledWith('V(L_filter)', 2);
    });

    it('adds all component signals at once via "+ Add all" button', () => {
      const onAddChannel = vi.fn();
      render(
        <ScopeChannelManager
          {...defaultProps}
          components={mockComponents}
          wires={mockWires}
          assignedChannels={[]}
          onAddChannel={onAddChannel}
        />,
      );

      // Find "+ Add all" on L_filter
      const compNode = screen.getByTestId('scope-tree-comp-L_filter');
      const addAllBtn = Array.from(compNode.querySelectorAll('button')).find((b) => b.textContent?.includes('+ Add all'));
      expect(addAllBtn).toBeDefined();
      fireEvent.click(addAllBtn!);

      // Both I(L_filter) and V(L_filter) should be requested
      expect(onAddChannel).toHaveBeenCalledWith('I(L_filter)', undefined);
      expect(onAddChannel).toHaveBeenCalledWith('V(L_filter)', undefined);
    });

    it('triggers hover callbacks onHoverComponent and onHoverNet', () => {
      const onHighlightComponent = vi.fn();
      const onHighlightNet = vi.fn();

      render(
        <ScopeChannelManager
          {...defaultProps}
          components={mockComponents}
          wires={mockWires}
          onHighlightComponent={onHighlightComponent}
          onHighlightNet={onHighlightNet}
        />,
      );

      // Hover on L_filter component container
      const compNode = screen.getByTestId('scope-tree-comp-L_filter');
      fireEvent.mouseEnter(compNode);
      expect(onHighlightComponent).toHaveBeenCalledWith('L_filter');

      fireEvent.mouseLeave(compNode);
      expect(onHighlightComponent).toHaveBeenCalledWith(null);

      // Hover on net V(dc_pos)
      const netRow = screen.getByTestId('scope-signal-row-V(dc_pos)');
      fireEvent.mouseEnter(netRow.parentElement!);
      expect(onHighlightNet).toHaveBeenCalledWith('dc_pos');

      fireEvent.mouseLeave(netRow.parentElement!);
      expect(onHighlightNet).toHaveBeenCalledWith(null);
    });
  });
});
