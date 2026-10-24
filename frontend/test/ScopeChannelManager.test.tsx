import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
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
});
