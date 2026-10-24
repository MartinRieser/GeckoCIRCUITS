/**
 * Dedicated Oscilloscope Properties & Configuration Panel.
 * Rendered in the right sidebar (Inspector) when an individual Scope tab (e.g. SCOPE.1) is active.
 *
 * Strictly scoped to the active oscilloscope block:
 * - Shows ONLY settings and channels belonging to this instrument.
 * - Does NOT display global solver parameters or duplicate simulation run buttons.
 * - Remembers layout (overlay / stacked), vertical scale mode, and channel visibility
 *   per scope and saves them with the circuit project (.ipes).
 */
import { useMemo } from 'react';
import type { EditorComponent } from '../model/types';
import { formatEngineeringValue } from '../model/componentSchema';
import {
  strictScopeChannels,
  parseSignalGraphs,
  serializeSignalGraphs,
  defaultGraphForSignal,
  resolveSignalGraph,
} from '../simulation/scopes';
import { SCOPE_SETTING_KEYS } from '../model/constants';
import type { ScopeDisplayLayout } from '../model/constants';
import type { ScopeController } from '../simulation/useScopeController';
import { isVoltmeterComponent, isAmmeterComponent } from '../model/componentSchema';
import { isGroundSignal } from '../simulation/chartData';

export interface ScopePropertiesPanelProps {
  /** The oscilloscope component currently inspected. */
  scopeComponent: EditorComponent;
  /** All schematic components (optional). */
  allComponents?: EditorComponent[];
  /** Circuit connection wires (optional). */
  wires?: { points: number[][]; label?: string }[];
  /** Simulation results time-series mapping, if available. */
  results: Record<string, number[]> | null;
  /** Waveform chart display layout mode for this scope ('overlay', '2-graphs', '3-graphs', or 'stacked'). */
  displayLayout: ScopeDisplayLayout;
  /** Callback to change waveform display layout mode. */
  onDisplayLayoutChange: (layout: ScopeDisplayLayout) => void;
  /** Oscilloscope controller instance. */
  scope: ScopeController;
  /** Callback to persist component parameter updates into the circuit model. */
  onSetParameter?: (name: string, key: string, value: number | string | boolean) => void;
  /** Callback to set a channel / pin label. */
  onSetLabel?: (name: string, side: 'x' | 'y', index: number, label: string) => void;
  /** Callback to remove a channel / pin label. */
  onRemoveLabel?: (name: string, side: 'x' | 'y', index: number) => void;
  /** Callback to collapse the scope properties panel. */
  onCollapse?: () => void;
}

/**
 * Inspector panel displaying solely the settings and channels for a specific oscilloscope instrument.
 */
export function ScopePropertiesPanel({
  scopeComponent,
  allComponents = [],
  wires = [],
  results,
  displayLayout,
  onDisplayLayoutChange,
  scope,
  onSetParameter,
  onSetLabel,
  onRemoveLabel,
  onCollapse,
}: ScopePropertiesPanelProps) {
  // Strictly the channels wired or assigned to this oscilloscope block — unlike the
  // display-side scopeChannels helper there is no all-signals fallback, so an
  // unwired scope shows nothing instead of another scope's signals and
  // persistence never writes foreign signal names into hiddenSignals.
  const channels = useMemo(
    () => strictScopeChannels(scopeComponent, scope.signalNames, allComponents, wires),
    [scopeComponent, scope.signalNames, allComponents, wires],
  );

  const signalGraphsRaw = scopeComponent.parameters?.[SCOPE_SETTING_KEYS.signalGraphs] as string | undefined;
  const signalGraphs = useMemo(() => parseSignalGraphs(signalGraphsRaw), [signalGraphsRaw]);

  const handleLayoutChange = (next: ScopeDisplayLayout) => {
    onDisplayLayoutChange(next);
    onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.scopeLayout, next);
  };

  const handleMoveSignalToGraph = (signal: string, targetGraph: number) => {
    const current = { ...signalGraphs };
    current[signal] = targetGraph;
    const serialized = serializeSignalGraphs(current);
    onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.signalGraphs, serialized);
  };

  const handleAutoGroupSignals = () => {
    const mapping: Record<string, number> = {};
    for (const sig of channels) {
      mapping[sig] = defaultGraphForSignal(sig, displayLayout);
    }
    const serialized = serializeSignalGraphs(mapping);
    onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.signalGraphs, serialized);
  };

  const handleYScaleToggle = () => {
    const next = scope.yScaleMode === 'fixed' ? 'auto' : 'fixed';
    scope.setYScaleMode(next);
    onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.yScaleMode, next);
  };

  const handleToggleChannel = (name: string) => {
    scope.toggleSignal(name);
    const nextHidden = { ...scope.hiddenSignals, [name]: !scope.hiddenSignals[name] };
    const serialized = Object.entries(nextHidden)
      .filter(([, hidden]) => hidden)
      .map(([n]) => n)
      .join(',');
    onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.hiddenSignals, serialized);
  };

  const availableUnaddedSignals = useMemo(() => {
    return scope.signalNames.filter((s) => !channels.includes(s) && !isGroundSignal(s));
  }, [scope.signalNames, channels]);

  const handleAutoAddProbes = () => {
    if (!onSetLabel) return;
    const current = [...(scopeComponent.inputLabels || [])];
    const probeSignals: string[] = [];
    allComponents?.forEach((comp) => {
      if (isVoltmeterComponent(comp) || isAmmeterComponent(comp)) {
        comp.outputLabels?.forEach((lbl) => {
          if (lbl && lbl.trim() && lbl !== 'NIX_NIX_NIX' && !isGroundSignal(lbl.trim()) && !probeSignals.includes(lbl.trim())) {
            probeSignals.push(lbl.trim());
          }
        });
      }
    });
    scope.signalNames.forEach((sig) => {
      const lower = sig.toLowerCase();
      if ((lower.startsWith('u_') || lower.startsWith('v_') || lower.startsWith('i_')) && !isGroundSignal(sig) && !probeSignals.includes(sig)) {
        probeSignals.push(sig);
      }
    });
    let nextIdx = current.length;
    probeSignals.forEach((sig) => {
      if (!current.includes(sig)) {
        onSetLabel(scopeComponent.name, 'x', nextIdx, sig);
        current.push(sig);
        nextIdx++;
      }
    });
  };

  const handleToggleCursors = () => {
    if (scope.cursorsEnabled) {
      scope.clearCursors();
      onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.cursorsEnabled, false);
    } else {
      scope.setCursorPreset();
      onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.cursorsEnabled, true);
    }
  };

  const handleExportCsv = () => {
    if (!results || !scope.timeArray.length) return;
    const headers = ['time', ...channels];
    const rows = [headers.join(',')];

    for (let i = 0; i < scope.timeArray.length; i++) {
      const row = [scope.timeArray[i], ...channels.map((s) => results[s]?.[i] ?? 0)];
      rows.push(row.join(','));
    }

    const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${scopeComponent.name}_results_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Cursor measurements restricted strictly to this scope's channels
  const filteredCursorChannels = useMemo(() => {
    if (!scope.cursorMeasurements) return [];
    return scope.cursorMeasurements.channels.filter((ch) => channels.includes(ch.name));
  }, [scope.cursorMeasurements, channels]);

  return (
    <div className="properties-container scope-properties-panel" data-testid="scope-properties-panel">
      {/* Scope Header */}
      <div className="properties-header-bar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="properties-title">Scope Settings: {scopeComponent.name}</span>
          <span className="workspace-tab-badge" title="Number of channels wired to this scope">
            {channels.length} ch
          </span>
        </div>
        <div className="properties-header-actions" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          {onCollapse && (
            <button
              type="button"
              className="action-icon-btn collapse-btn"
              onClick={onCollapse}
              title="Collapse scope settings (Ctrl+I)"
            >
              ▶
            </button>
          )}
        </div>
      </div>

      <div className="properties-body">
        {/* Display Layout Mode Section */}
        <div className="prop-section">
          <div className="prop-section-title">Display Layout</div>
          <div className="segmented-control" style={{ width: '100%', display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)' }}>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === 'overlay' ? 'active' : ''}`}
              onClick={() => handleLayoutChange('overlay')}
              style={{ textAlign: 'center', justifyContent: 'center', padding: '4px 2px', fontSize: '11px' }}
              title="Overlay all channels of this scope on a single combined graph"
            >
              📈 1 Graph
            </button>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === '2-graphs' ? 'active' : ''}`}
              onClick={() => handleLayoutChange('2-graphs')}
              style={{ textAlign: 'center', justifyContent: 'center', padding: '4px 2px', fontSize: '11px' }}
              title="Split channels across 2 separate subplots"
            >
              📊 2 Graphs
            </button>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === '3-graphs' ? 'active' : ''}`}
              onClick={() => handleLayoutChange('3-graphs')}
              style={{ textAlign: 'center', justifyContent: 'center', padding: '4px 2px', fontSize: '11px' }}
              title="Split channels across 3 separate subplots"
            >
              📑 3 Graphs
            </button>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === 'stacked' ? 'active' : ''}`}
              onClick={() => handleLayoutChange('stacked')}
              style={{ textAlign: 'center', justifyContent: 'center', padding: '4px 2px', fontSize: '11px' }}
              title="Stack each channel of this scope in its own subplot lane"
            >
              🪜 Stacked
            </button>
          </div>
        </div>

        {/* Oscilloscope Controls */}
        <div className="prop-section sim-oszi-section" data-testid="scope-oszi-controls">
          <div className="prop-section-title">Oscilloscope Controls</div>

          {/* Horizontal / Timebase */}
          <div className="sim-sub-heading">Horizontal (Timebase)</div>
          <div className="sim-oszi-row">
            <span className="sim-oszi-badge" title="Time per major division">
              ⏱ <strong>{formatEngineeringValue(scope.timePerDiv, 's')}/div</strong>
            </span>
            <button
              type="button"
              className="sim-mini-btn"
              onClick={scope.fit}
              title="Fit Full Simulation Waveform"
            >
              ⟲ Fit
            </button>
          </div>

          <div className="sim-btn-row" style={{ marginTop: 6 }}>
            <div className="sim-btn-group-2">
              <button
                type="button"
                className="sim-mini-btn"
                onClick={() => scope.zoomAt(0.7, (scope.dataT0 + scope.dataT1) / 2)}
                title="Zoom In Timebase (+)"
              >
                Zoom +
              </button>
              <button
                type="button"
                className="sim-mini-btn"
                onClick={() => scope.zoomAt(1.4, (scope.dataT0 + scope.dataT1) / 2)}
                title="Zoom Out Timebase (−)"
              >
                Zoom −
              </button>
            </div>
            <div className="sim-btn-group-2">
              <button
                type="button"
                className="sim-mini-btn"
                onClick={() => scope.pan(-0.2)}
                title="Pan Left (◀)"
              >
                ◀ Pan
              </button>
              <button
                type="button"
                className="sim-mini-btn"
                onClick={() => scope.pan(0.2)}
                title="Pan Right (▶)"
              >
                Pan ▶
              </button>
            </div>
          </div>

          {/* Vertical & Channels */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 12 }}>
            <div className="sim-sub-heading" style={{ margin: 0 }}>
              Vertical (Scale & Channels)
            </div>
            {(displayLayout === '2-graphs' || displayLayout === '3-graphs') && channels.length > 0 && (
              <button
                type="button"
                className="sim-mini-btn"
                onClick={handleAutoGroupSignals}
                title="Automatically organize: Voltages -> Graph 1, Currents -> Graph 2, Control -> Graph 3"
                style={{ fontSize: '10px', padding: '1px 6px' }}
              >
                ⚡ Auto-Group
              </button>
            )}
          </div>
          <div style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
            <button
              type="button"
              className={`sim-toggle-btn ${scope.yScaleMode === 'fixed' ? 'active' : ''}`}
              onClick={handleYScaleToggle}
              title={
                scope.yScaleMode === 'fixed'
                  ? 'Locked: Zooming time preserves voltage scale'
                  : 'Auto: Rescales voltage to fit visible points'
              }
              style={{ flex: 1 }}
            >
              ↕ Scale: {scope.yScaleMode === 'fixed' ? 'Fixed' : 'Auto'}
            </button>
          </div>

          {/* Channel Toggles & Management - STRICTLY FOR THIS SCOPE */}
          {channels.length === 0 ? (
            <div style={{ fontSize: '11px', color: 'var(--text-muted)', padding: '6px 0', fontStyle: 'italic' }}>
              No signals assigned to this scope.
            </div>
          ) : (
            <div className="sim-channels-list">
              {channels.map((name, idx) => {
                const color = scope.colorOf(name);
                const isHidden = !!scope.hiddenSignals[name];
                return (
                  <div key={name} style={{ display: 'flex', alignItems: 'center', gap: 4, width: '100%' }}>
                    <button
                      type="button"
                      className={`sim-channel-badge ${isHidden ? 'hidden' : 'active'}`}
                      onClick={() => handleToggleChannel(name)}
                      style={{
                        flex: 1,
                        borderColor: color,
                        color: isHidden ? 'var(--text-dim)' : color,
                        justifyContent: 'flex-start',
                      }}
                      title={isHidden ? `Show ${name}` : `Hide ${name}`}
                    >
                      <span className="sim-ch-led" style={{ backgroundColor: color }} />
                      <span style={{ fontSize: '10px', opacity: 0.8, marginRight: 4 }}>CH{idx + 1}:</span>
                      {name}
                    </button>
                    {(displayLayout === '2-graphs' || displayLayout === '3-graphs') && (
                      <div style={{ display: 'inline-flex', gap: 2 }}>
                        {[1, 2, ...(displayLayout === '3-graphs' ? [3] : [])].map((g) => {
                          const currentG = resolveSignalGraph(name, displayLayout, signalGraphs);
                          const isSelected = currentG === g;
                          return (
                            <button
                              key={g}
                              type="button"
                              onClick={() => handleMoveSignalToGraph(name, g)}
                              title={`Move ${name} to Graph ${g}`}
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
                    {onRemoveLabel && (
                      <button
                        type="button"
                        className="action-icon-btn"
                        onClick={() => {
                          const lblIdx = scopeComponent.inputLabels?.indexOf(name);
                          if (lblIdx !== undefined && lblIdx >= 0) {
                            onRemoveLabel(scopeComponent.name, 'x', lblIdx);
                          }
                        }}
                        title={`Remove ${name} from ${scopeComponent.name}`}
                        style={{ fontSize: '11px', padding: '2px 6px', color: 'var(--text-muted)' }}
                      >
                        ✕
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {/* Quick Signal Add & Probes */}
          <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
            {availableUnaddedSignals.length > 0 && onSetLabel && (
              <select
                className="prop-input"
                style={{ flex: 1, fontSize: '11px', padding: '3px 6px' }}
                value=""
                onChange={(e) => {
                  if (e.target.value) {
                    const nextIdx = scopeComponent.inputLabels?.length || 0;
                    onSetLabel(scopeComponent.name, 'x', nextIdx, e.target.value);
                  }
                }}
              >
                <option value="" disabled>+ Add Signal to Scope...</option>
                {availableUnaddedSignals.map((sig) => (
                  <option key={sig} value={sig}>{sig}</option>
                ))}
              </select>
            )}
            {onSetLabel && (
              <button
                type="button"
                className="sim-mini-btn"
                onClick={handleAutoAddProbes}
                title="Auto-add all probes (voltmeters, ammeters) from the circuit"
                style={{ whiteSpace: 'nowrap', fontSize: '11px' }}
              >
                ⚡ Auto Probes
              </button>
            )}
          </div>

          {/* Cursors & Measurements */}
          <div className="sim-sub-heading" style={{ marginTop: 12 }}>
            Cursors & Measurement
          </div>
          <div style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
            <button
              type="button"
              className={`sim-toggle-btn ${scope.cursorsEnabled ? 'active' : ''}`}
              onClick={handleToggleCursors}
              style={{ flex: 1 }}
            >
              📍 Cursors: {scope.cursorsEnabled ? 'ON' : 'OFF'}
            </button>
          </div>

          {scope.cursorsEnabled && (
            <div className="sim-cursor-controls-box">
              <div style={{ display: 'flex', gap: 4, marginBottom: 8 }}>
                <button
                  type="button"
                  className={`sim-mini-btn ${scope.activeCursor === 'A' ? 'active' : ''}`}
                  onClick={() => scope.setActiveCursor('A')}
                  style={{ flex: 1 }}
                  title="Select Cursor A"
                >
                  [A]
                </button>
                <button
                  type="button"
                  className={`sim-mini-btn ${scope.activeCursor === 'B' ? 'active' : ''}`}
                  onClick={() => scope.setActiveCursor('B')}
                  style={{ flex: 1 }}
                  title="Select Cursor B"
                >
                  [B]
                </button>
                <button
                  type="button"
                  className="sim-mini-btn"
                  onClick={scope.setCursorPreset}
                  title="Snap to 25% and 75% of waveform"
                >
                  25/75%
                </button>
                <button
                  type="button"
                  className="sim-mini-btn"
                  onClick={scope.clearCursors}
                  title="Clear Cursors"
                >
                  ✕
                </button>
              </div>

              <div className="sim-hint-text">
                💡 Drag [A] / [B] handles on plot, or click plot to position active cursor.
              </div>

              {scope.cursorMeasurements && (
                <div className="sim-cursor-card">
                  <div className="sim-cursor-time-grid">
                    <div className="sim-cursor-time-item">
                      <span className="sim-cursor-lbl">tA:</span>
                      <span className="sim-cursor-val">
                        {scope.cursorMeasurements.timeA !== null
                          ? formatEngineeringValue(scope.cursorMeasurements.timeA, 's')
                          : '—'}
                      </span>
                    </div>
                    <div className="sim-cursor-time-item">
                      <span className="sim-cursor-lbl">tB:</span>
                      <span className="sim-cursor-val">
                        {scope.cursorMeasurements.timeB !== null
                          ? formatEngineeringValue(scope.cursorMeasurements.timeB, 's')
                          : '—'}
                      </span>
                    </div>
                  </div>

                  {scope.cursorMeasurements.dt !== null && (
                    <div className="sim-cursor-delta-row">
                      <div>
                        <span className="sim-cursor-lbl">Δt: </span>
                        <strong className="sim-cursor-highlight">
                          {formatEngineeringValue(scope.cursorMeasurements.dt, 's')}
                        </strong>
                      </div>
                      <div>
                        <span className="sim-cursor-lbl">1/Δt: </span>
                        <strong className="sim-cursor-highlight">
                          {scope.cursorMeasurements.freq !== null
                            ? formatEngineeringValue(scope.cursorMeasurements.freq, 'Hz')
                            : '—'}
                        </strong>
                      </div>
                    </div>
                  )}

                  {filteredCursorChannels.length > 0 && (
                    <div className="sim-cursor-table-wrap">
                      <table className="sim-cursor-table">
                        <thead>
                          <tr>
                            <th>Ch</th>
                            <th>A</th>
                            <th>B</th>
                            <th>Δ</th>
                          </tr>
                        </thead>
                        <tbody>
                          {filteredCursorChannels.map((ch) => {
                            const isCurrent = ch.name.toLowerCase().startsWith('i');
                            const unit = isCurrent ? 'A' : 'V';
                            return (
                              <tr key={ch.name}>
                                <td style={{ color: ch.color, fontWeight: 600 }}>
                                  <span
                                    className="sim-ch-led-mini"
                                    style={{ backgroundColor: ch.color }}
                                  />
                                  {ch.name}
                                </td>
                                <td>{ch.valA !== null ? formatEngineeringValue(ch.valA, unit) : '—'}</td>
                                <td>{ch.valB !== null ? formatEngineeringValue(ch.valB, unit) : '—'}</td>
                                <td className="sim-delta-col">
                                  {ch.delta !== null
                                    ? `${ch.delta >= 0 ? '+' : ''}${formatEngineeringValue(ch.delta, unit)}`
                                    : '—'}
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Detailed Analysis Tools */}
          <div className="sim-sub-heading" style={{ marginTop: 12 }}>
            Detailed Analysis
          </div>
          <div className="sim-tools-row">
            <button
              type="button"
              className={`sim-mini-btn ${scope.drawerTab === 'metrics' ? 'active' : ''}`}
              onClick={() => scope.setDrawerTab((prev) => (prev === 'metrics' ? null : 'metrics'))}
              title="View detailed signal statistics table in bottom drawer"
            >
              📊 Statistics
            </button>
            <button
              type="button"
              className={`sim-mini-btn ${scope.drawerTab === 'fft' ? 'active' : ''}`}
              onClick={() => scope.setDrawerTab((prev) => (prev === 'fft' ? null : 'fft'))}
              title="FFT Harmonic Spectrum Analyzer in bottom drawer"
            >
              〰 FFT
            </button>
            <button
              type="button"
              className={`sim-mini-btn ${scope.drawerTab === 'losses' ? 'active' : ''}`}
              onClick={() => scope.setDrawerTab((prev) => (prev === 'losses' ? null : 'losses'))}
              title="Waveform Power (p = v · i) and Semiconductor Loss Analysis in bottom drawer"
            >
              ⚡ Power & Losses
            </button>
          </div>
        </div>

        {/* Results & CSV Export for THIS Scope */}
        {results && channels.length > 0 && (
          <div className="prop-section">
            <div className="prop-section-title">Signals & Export</div>
            <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: 8 }}>
              {channels.length} channel{channels.length !== 1 ? 's' : ''} in {scopeComponent.name}
            </div>
            <button
              type="button"
              className="sim-btn export"
              onClick={handleExportCsv}
              style={{ width: '100%', justifyContent: 'center' }}
              title={`Export waveform data for ${scopeComponent.name} to CSV`}
            >
              Export CSV File
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
