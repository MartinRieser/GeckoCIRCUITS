/**
 * Modal & Drawer Dialog for managing oscilloscope channels and signal bindings.
 * Allows engineers to browse all recorded circuit signals (voltages, currents, control states)
 * and bind them directly to the active oscilloscope instrument without drawing messy wires.
 */
import { useState, useMemo, useEffect, useRef } from 'react';
import { inferSignalUnit } from './ScopeViewTab';
import type { ScopeDisplayLayout } from '../model/constants';
import { resolveSignalGraph } from './scopes';

export interface ScopeChannelManagerProps {
  /** Whether the channel manager modal is open. */
  isOpen: boolean;
  /** Callback to close the modal. */
  onClose: () => void;
  /** Name of the active scope instrument (e.g. 'SCOPE', 'SCOPE.1'). */
  scopeName: string;
  /** List of signals currently assigned as channels to this scope. */
  assignedChannels: string[];
  /** Complete list of all recorded signals from the circuit simulation. */
  allAvailableSignals: string[];
  /** Color resolver for signals. */
  colorOf: (signalName: string) => string;
  /** Callback to add a signal as a new channel. */
  onAddChannel: (signalName: string) => void;
  /** Callback to remove a signal / channel. */
  onRemoveChannel: (signalName: string) => void;
  /** Optional callback to auto-add all voltage and current measurement probes. */
  onAutoAddProbes?: () => void;
  /** Optional callback to clear all assigned channels for this scope. */
  onClearChannels?: () => void;
  /** Current display layout mode. */
  displayLayout?: ScopeDisplayLayout;
  /** Callback to change display layout mode. */
  onDisplayLayoutChange?: (layout: ScopeDisplayLayout) => void;
  /** Signal to graph mapping dictionary. */
  signalGraphs?: Record<string, number>;
  /** Callback when signal graph assignment changes. */
  onSetSignalGraph?: (signalName: string, graphIndex: number) => void;
  /** Callback to auto-group signals by category. */
  onAutoGroupByCategory?: () => void;
  /** Current UI theme. */
  theme?: 'dark' | 'light';
}

type SignalCategory = 'all' | 'voltages' | 'currents' | 'control';

export function ScopeChannelManager({
  isOpen,
  onClose,
  scopeName,
  assignedChannels,
  allAvailableSignals,
  colorOf,
  onAddChannel,
  onRemoveChannel,
  onAutoAddProbes,
  onClearChannels,
  displayLayout = 'overlay',
  onDisplayLayoutChange,
  signalGraphs = {},
  onSetSignalGraph,
  onAutoGroupByCategory,
}: ScopeChannelManagerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<SignalCategory>('all');
  const modalRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Focus search input when modal opens
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => searchInputRef.current?.focus(), 50);
    } else {
      setSearchQuery('');
    }
  }, [isOpen]);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Categorize signals
  const categorizedSignals = useMemo(() => {
    const voltages: string[] = [];
    const currents: string[] = [];
    const control: string[] = [];

    allAvailableSignals.forEach((sig) => {
      const unit = inferSignalUnit(sig);
      const lower = sig.toLowerCase();
      if (unit === 'V' || lower.startsWith('u_') || lower.startsWith('v_') || lower.includes('_v') || lower.includes('volt')) {
        voltages.push(sig);
      } else if (unit === 'A' || lower.startsWith('i_') || lower.startsWith('il') || lower.includes('_i') || lower.includes('curr')) {
        currents.push(sig);
      } else {
        control.push(sig);
      }
    });

    return { voltages, currents, control };
  }, [allAvailableSignals]);

  // Filter signals by search query and category
  const filteredSignals = useMemo(() => {
    let pool: string[];
    if (activeCategory === 'voltages') pool = categorizedSignals.voltages;
    else if (activeCategory === 'currents') pool = categorizedSignals.currents;
    else if (activeCategory === 'control') pool = categorizedSignals.control;
    else pool = allAvailableSignals;

    const q = searchQuery.trim().toLowerCase();
    if (!q) return pool;
    return pool.filter((sig) => sig.toLowerCase().includes(q));
  }, [allAvailableSignals, categorizedSignals, activeCategory, searchQuery]);

  if (!isOpen) return null;

  return (
    <div
      className="modal-backdrop"
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        background: 'rgba(0, 0, 0, 0.65)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backdropFilter: 'blur(3px)',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      data-testid="scope-channel-manager-modal"
    >
      <div
        ref={modalRef}
        className="modal-card"
        style={{
          width: '90%',
          maxWidth: '680px',
          maxHeight: '85vh',
          display: 'flex',
          flexDirection: 'column',
          background: 'var(--panel)',
          border: '1px solid var(--border)',
          borderRadius: '8px',
          boxShadow: '0 12px 36px rgba(0,0,0,0.4)',
          overflow: 'hidden',
        }}
      >
        {/* Modal Header */}
        <div
          className="modal-header"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '14px 18px',
            borderBottom: '1px solid var(--border)',
            background: 'var(--panel-header, var(--panel))',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: '20px' }}>📺</span>
            <div>
              <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text)' }}>
                Scope Channel Manager: {scopeName}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                Select circuit signals to display on this oscilloscope instrument.
              </div>
            </div>
          </div>
          <button
            type="button"
            className="action-icon-btn"
            onClick={onClose}
            title="Close (Esc)"
            style={{ fontSize: '16px', padding: '4px 8px' }}
          >
            ✕
          </button>
        </div>

        {/* Modal Body */}
        <div
          className="modal-body"
          style={{
            padding: '16px 18px',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: 14,
          }}
        >
          {/* Display Layout Mode Strip */}
          <div
            style={{
              padding: '10px 14px',
              background: 'var(--surface)',
              borderRadius: '6px',
              border: '1px solid var(--border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 12,
              flexWrap: 'wrap',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text)' }}>
                Display Layout:
              </span>
              {onDisplayLayoutChange && (
                <div className="segmented-control" style={{ display: 'flex' }}>
                  <button
                    type="button"
                    className={`segmented-btn ${displayLayout === 'overlay' ? 'active' : ''}`}
                    onClick={() => onDisplayLayoutChange('overlay')}
                    title="Overlay all channels on 1 combined graph"
                  >
                    📈 1 Graph
                  </button>
                  <button
                    type="button"
                    className={`segmented-btn ${displayLayout === '2-graphs' ? 'active' : ''}`}
                    onClick={() => onDisplayLayoutChange('2-graphs')}
                    title="Split into 2 subplots (e.g. Voltages / Currents)"
                  >
                    📊 2 Graphs
                  </button>
                  <button
                    type="button"
                    className={`segmented-btn ${displayLayout === '3-graphs' ? 'active' : ''}`}
                    onClick={() => onDisplayLayoutChange('3-graphs')}
                    title="Split into 3 subplots (e.g. Voltages / Currents / Control)"
                  >
                    📑 3 Graphs
                  </button>
                  <button
                    type="button"
                    className={`segmented-btn ${displayLayout === 'stacked' ? 'active' : ''}`}
                    onClick={() => onDisplayLayoutChange('stacked')}
                    title="Stack each channel in its own individual lane"
                  >
                    🪜 Stacked
                  </button>
                </div>
              )}
            </div>

            {(displayLayout === '2-graphs' || displayLayout === '3-graphs') && onAutoGroupByCategory && (
              <button
                type="button"
                className="sim-mini-btn"
                onClick={onAutoGroupByCategory}
                title="Automatically organize signals: Voltages -> Graph 1, Currents -> Graph 2, Control -> Graph 3"
                style={{ fontSize: '11px', display: 'flex', alignItems: 'center', gap: 4 }}
              >
                <span>⚡</span> Auto-Group (V / I / Ctrl)
              </button>
            )}
          </div>

          {/* Active Channels Strip */}
          <div
            style={{
              padding: '10px 14px',
              background: 'var(--surface)',
              borderRadius: '6px',
              border: '1px solid var(--border)',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 8,
              }}
            >
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text)' }}>
                Active Channels ({assignedChannels.length})
              </span>
              <div style={{ display: 'flex', gap: 8 }}>
                {onAutoAddProbes && (
                  <button
                    type="button"
                    className="sim-mini-btn"
                    onClick={onAutoAddProbes}
                    title="Automatically add all measurement probes (voltmeters, ammeters) from the circuit"
                    style={{ fontSize: '11px', display: 'flex', alignItems: 'center', gap: 4 }}
                  >
                    <span>⚡</span> Auto-Add Probes
                  </button>
                )}
                {onClearChannels && assignedChannels.length > 0 && (
                  <button
                    type="button"
                    className="sim-mini-btn"
                    onClick={onClearChannels}
                    title="Remove all channels from this scope"
                    style={{ fontSize: '11px', color: 'var(--danger, #f43f5e)' }}
                  >
                    Clear All
                  </button>
                )}
              </div>
            </div>

            {assignedChannels.length === 0 ? (
              <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                No channels currently assigned. Select signals below or click "Auto-Add Probes".
              </div>
            ) : (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {assignedChannels.map((sig, idx) => {
                  const color = colorOf(sig);
                  const unit = inferSignalUnit(sig);
                  return (
                    <div
                      key={sig}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '3px 8px',
                        borderRadius: '4px',
                        background: 'var(--panel)',
                        border: `1px solid ${color}`,
                        fontSize: '12px',
                      }}
                    >
                      <span
                        style={{
                          fontSize: '10px',
                          fontWeight: 700,
                          color,
                        }}
                      >
                        CH{idx + 1}
                      </span>
                      <strong style={{ color: 'var(--text)' }}>{sig}</strong>
                      {unit && (
                        <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>
                          [{unit}]
                        </span>
                      )}
                      {(displayLayout === '2-graphs' || displayLayout === '3-graphs') && (
                        <div style={{ display: 'inline-flex', gap: 2, marginLeft: 2 }}>
                          {[1, 2, ...(displayLayout === '3-graphs' ? [3] : [])].map((g) => {
                            const currentG = resolveSignalGraph(sig, displayLayout, signalGraphs);
                            const isSelected = currentG === g;
                            return (
                              <button
                                key={g}
                                type="button"
                                onClick={() => onSetSignalGraph?.(sig, g)}
                                title={`Move ${sig} to Graph ${g}`}
                                style={{
                                  border: isSelected ? '1px solid var(--accent, #38bdf8)' : '1px solid var(--border)',
                                  background: isSelected ? 'var(--accent, #38bdf8)' : 'transparent',
                                  color: isSelected ? '#ffffff' : 'var(--text-muted)',
                                  borderRadius: '3px',
                                  fontSize: '10px',
                                  padding: '1px 4px',
                                  cursor: 'pointer',
                                  fontWeight: isSelected ? 700 : 500,
                                }}
                              >
                                G{g}
                              </button>
                            );
                          })}
                        </div>
                      )}
                      <button
                        type="button"
                        onClick={() => onRemoveChannel(sig)}
                        title={`Remove ${sig} from scope`}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: 'var(--text-muted)',
                          cursor: 'pointer',
                          padding: '0 2px',
                          fontSize: '12px',
                          lineHeight: 1,
                        }}
                      >
                        ✕
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Search & Category Filter */}
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <input
              ref={searchInputRef}
              type="text"
              className="prop-input"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Filter signals (e.g. u_out, duty, i_L)..."
              style={{ flex: '1 1 240px' }}
            />
            <div className="segmented-control" style={{ display: 'flex' }}>
              <button
                type="button"
                className={`segmented-btn ${activeCategory === 'all' ? 'active' : ''}`}
                onClick={() => setActiveCategory('all')}
              >
                All ({allAvailableSignals.length})
              </button>
              <button
                type="button"
                className={`segmented-btn ${activeCategory === 'voltages' ? 'active' : ''}`}
                onClick={() => setActiveCategory('voltages')}
              >
                ⚡ Voltages ({categorizedSignals.voltages.length})
              </button>
              <button
                type="button"
                className={`segmented-btn ${activeCategory === 'currents' ? 'active' : ''}`}
                onClick={() => setActiveCategory('currents')}
              >
                🔌 Currents ({categorizedSignals.currents.length})
              </button>
              <button
                type="button"
                className={`segmented-btn ${activeCategory === 'control' ? 'active' : ''}`}
                onClick={() => setActiveCategory('control')}
              >
                🎛️ Control ({categorizedSignals.control.length})
              </button>
            </div>
          </div>

          {/* Signal Selection Grid */}
          <div
            style={{
              maxHeight: '320px',
              overflowY: 'auto',
              border: '1px solid var(--border)',
              borderRadius: '6px',
              padding: '6px',
              display: 'flex',
              flexDirection: 'column',
              gap: 4,
            }}
          >
            {filteredSignals.length === 0 ? (
              <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                No circuit signals match "{searchQuery}"
              </div>
            ) : (
              filteredSignals.map((sig) => {
                const isAssigned = assignedChannels.includes(sig);
                const channelIdx = assignedChannels.indexOf(sig);
                const color = colorOf(sig);
                const unit = inferSignalUnit(sig);

                return (
                  <div
                    key={sig}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '6px 12px',
                      borderRadius: '4px',
                      background: isAssigned ? 'rgba(56, 189, 248, 0.08)' : 'var(--surface)',
                      border: `1px solid ${isAssigned ? 'rgba(56, 189, 248, 0.4)' : 'transparent'}`,
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span
                        style={{
                          width: 10,
                          height: 10,
                          borderRadius: '50%',
                          backgroundColor: color,
                          display: 'inline-block',
                          boxShadow: `0 0 6px ${color}80`,
                        }}
                      />
                      <span style={{ fontWeight: isAssigned ? 700 : 500, color: 'var(--text)' }}>
                        {sig}
                      </span>
                      {unit && (
                        <span
                          style={{
                            fontSize: '10px',
                            background: 'var(--border)',
                            color: 'var(--text-muted)',
                            padding: '1px 5px',
                            borderRadius: '3px',
                          }}
                        >
                          {unit}
                        </span>
                      )}
                      {isAssigned && (
                        <span
                          style={{
                            fontSize: '10px',
                            fontWeight: 700,
                            color,
                            border: `1px solid ${color}`,
                            padding: '1px 4px',
                            borderRadius: '3px',
                          }}
                        >
                          CH{channelIdx + 1}
                        </span>
                      )}
                      {isAssigned && (displayLayout === '2-graphs' || displayLayout === '3-graphs') && (
                        <div style={{ display: 'inline-flex', gap: 2, marginLeft: 2 }}>
                          {[1, 2, ...(displayLayout === '3-graphs' ? [3] : [])].map((g) => {
                            const currentG = resolveSignalGraph(sig, displayLayout, signalGraphs);
                            const isSelected = currentG === g;
                            return (
                              <button
                                key={g}
                                type="button"
                                onClick={() => onSetSignalGraph?.(sig, g)}
                                title={`Assign ${sig} to Graph ${g}`}
                                style={{
                                  border: isSelected ? '1px solid var(--accent, #38bdf8)' : '1px solid var(--border)',
                                  background: isSelected ? 'var(--accent, #38bdf8)' : 'transparent',
                                  color: isSelected ? '#ffffff' : 'var(--text-muted)',
                                  borderRadius: '3px',
                                  fontSize: '9px',
                                  padding: '1px 3px',
                                  cursor: 'pointer',
                                  fontWeight: isSelected ? 700 : 500,
                                }}
                              >
                                G{g}
                              </button>
                            );
                          })}
                        </div>
                      )}
                    </div>

                    <div>
                      {isAssigned ? (
                        <button
                          type="button"
                          className="sim-mini-btn"
                          onClick={() => onRemoveChannel(sig)}
                          style={{ color: 'var(--danger, #f43f5e)', borderColor: 'rgba(244, 63, 94, 0.3)' }}
                        >
                          Remove
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="sim-mini-btn"
                          onClick={() => onAddChannel(sig)}
                          style={{ color: 'var(--accent, #38bdf8)', borderColor: 'rgba(56, 189, 248, 0.3)' }}
                        >
                          + Add to Scope
                        </button>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Modal Footer */}
        <div
          className="modal-footer"
          style={{
            padding: '12px 18px',
            borderTop: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            gap: 10,
            background: 'var(--panel-header, var(--panel))',
          }}
        >
          <button type="button" className="modal-btn primary" onClick={onClose}>
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
