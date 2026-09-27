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
import { strictScopeChannels } from '../simulation/scopes';
import { SCOPE_SETTING_KEYS } from '../model/constants';
import type { ScopeController } from '../simulation/useScopeController';

export interface ScopePropertiesPanelProps {
  /** The oscilloscope component currently inspected. */
  scopeComponent: EditorComponent;
  /** All schematic components (optional). */
  allComponents?: EditorComponent[];
  /** Simulation results time-series mapping, if available. */
  results: Record<string, number[]> | null;
  /** Waveform chart display layout mode for this scope ('overlay' or 'stacked'). */
  displayLayout: 'overlay' | 'stacked';
  /** Callback to change waveform display layout mode. */
  onDisplayLayoutChange: (layout: 'overlay' | 'stacked') => void;
  /** Oscilloscope controller instance. */
  scope: ScopeController;
  /** Callback to persist component parameter updates into the circuit model. */
  onSetParameter?: (name: string, key: string, value: number | string | boolean) => void;
  /** Callback to collapse the scope properties panel. */
  onCollapse?: () => void;
}

/**
 * Inspector panel displaying solely the settings and channels for a specific oscilloscope instrument.
 */
export function ScopePropertiesPanel({
  scopeComponent,
  results,
  displayLayout,
  onDisplayLayoutChange,
  scope,
  onSetParameter,
  onCollapse,
}: ScopePropertiesPanelProps) {
  // Strictly the channels wired to this oscilloscope block — unlike the
  // display-side scopeChannels helper there is no all-signals fallback, so an
  // unwired scope shows nothing instead of another scope's signals and
  // persistence never writes foreign signal names into hiddenSignals.
  const channels = useMemo(
    () => strictScopeChannels(scopeComponent, scope.signalNames),
    [scopeComponent, scope.signalNames],
  );

  const handleLayoutChange = (next: 'overlay' | 'stacked') => {
    onDisplayLayoutChange(next);
    onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.scopeLayout, next);
  };

  const handleYScaleToggle = () => {
    const next = scope.yScaleMode === 'fixed' ? 'auto' : 'fixed';
    scope.setYScaleMode(next);
    onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.yScaleMode, next);
  };

  const handleToggleChannel = (name: string) => {
    scope.toggleSignal(name);
    const willBeHidden = !scope.hiddenSignals[name];
    const nextHidden = { ...scope.hiddenSignals, [name]: willBeHidden };
    // Persisted as a comma-separated list (net labels never contain commas)
    const hiddenList = Object.keys(nextHidden).filter((k) => nextHidden[k]);
    onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.hiddenSignals, hiddenList.join(','));
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
          <div className="segmented-control" style={{ width: '100%', display: 'flex' }}>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === 'overlay' ? 'active' : ''}`}
              onClick={() => handleLayoutChange('overlay')}
              style={{ flex: 1, textAlign: 'center', justifyContent: 'center' }}
              title="Overlay all channels of this scope on a single combined graph"
            >
              📈 Overlay
            </button>
            <button
              type="button"
              className={`segmented-btn ${displayLayout === 'stacked' ? 'active' : ''}`}
              onClick={() => handleLayoutChange('stacked')}
              style={{ flex: 1, textAlign: 'center', justifyContent: 'center' }}
              title="Stack each channel of this scope in its own subplot lane"
            >
              📑 Stacked Lanes
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
          <div className="sim-sub-heading" style={{ marginTop: 12 }}>
            Vertical (Scale & Channels)
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

          {/* Channel Toggles - STRICTLY FOR THIS SCOPE */}
          <div className="sim-channels-list">
            {channels.map((name) => {
              const color = scope.colorOf(name);
              const isHidden = !!scope.hiddenSignals[name];
              return (
                <button
                  key={name}
                  type="button"
                  className={`sim-channel-badge ${isHidden ? 'hidden' : 'active'}`}
                  onClick={() => handleToggleChannel(name)}
                  style={{
                    borderColor: color,
                    color: isHidden ? 'var(--text-dim)' : color,
                  }}
                  title={isHidden ? `Show ${name}` : `Hide ${name}`}
                >
                  <span className="sim-ch-led" style={{ backgroundColor: color }} />
                  {name}
                </button>
              );
            })}
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
              title="Semiconductor Loss Breakdown in bottom drawer"
            >
              ⚡ Losses
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
