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
import { useMemo, useState } from 'react';
import type { EditorComponent, GraphRegister } from '../model/types';
import { formatEngineeringValue, extractAvailableSignalDetails, type AvailableSignalMetadata } from '../model/componentSchema';
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
  /** The oscilloscope component currently inspected (for legacy scope tabs). */
  scopeComponent?: EditorComponent | null;
  /** Active project-level Graph Register (when viewing an Osci / Graph tab). */
  graphRegister?: GraphRegister | null;
  /** Callback to update graph register state. */
  onUpdateGraphRegister?: (patch: Partial<GraphRegister>) => void;
  /** All schematic components (optional). */
  allComponents?: EditorComponent[];
  /** Circuit connection wires (optional). */
  wires?: { points: number[][]; label?: string; type?: string; domain?: string }[];
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
  graphRegister,
  onUpdateGraphRegister,
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
  const [signalSearch, setSignalSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<'all' | 'electrical' | 'control'>('all');

  const channels = useMemo(
    () => (scopeComponent ? strictScopeChannels(scopeComponent, scope.signalNames, allComponents, wires) : []),
    [scopeComponent, scope.signalNames, allComponents, wires],
  );

  const signalGraphsRaw = scopeComponent?.parameters?.[SCOPE_SETTING_KEYS.signalGraphs] as string | undefined;
  const signalGraphs = useMemo(() => parseSignalGraphs(signalGraphsRaw), [signalGraphsRaw]);

  // Unified available signal metadata across schematic and simulation results
  const allAvailableSignalDetails = useMemo(() => {
    const list = extractAvailableSignalDetails(allComponents, wires);
    const known = new Set(list.map((s) => s.name));
    const extraNames = new Set<string>();

    if (results) {
      Object.keys(results).forEach((k) => {
        if (!known.has(k) && !isGroundSignal(k)) extraNames.add(k);
      });
    }
    scope.signalNames.forEach((k) => {
      if (!known.has(k) && !isGroundSignal(k)) extraNames.add(k);
    });
    if (graphRegister?.signals) {
      graphRegister.signals.forEach((k) => {
        if (!known.has(k) && !isGroundSignal(k)) extraNames.add(k);
      });
    }

    const extras: AvailableSignalMetadata[] = Array.from(extraNames).map((name) => {
      const lower = name.toLowerCase();
      const isCtrl =
        lower.startsWith('ctrl') ||
        lower.startsWith('gate') ||
        lower.startsWith('pwm') ||
        lower.startsWith('pi');
      const isAmp =
        lower.startsWith('i_') || lower.startsWith('i(') || lower.startsWith('il');
      const isVolt =
        lower.startsWith('v_') || lower.startsWith('v(') || lower.startsWith('u');
      return {
        name,
        domain: isCtrl ? 'control' : 'electrical',
        category: isVolt ? 'voltage' : isAmp ? 'current' : isCtrl ? 'control' : 'other',
        sourceDescription: 'Waveform signal',
      };
    });

    return [...list.filter((s) => !isGroundSignal(s.name)), ...extras].sort((a, b) =>
      a.name.localeCompare(b.name, undefined, { numeric: true }),
    );
  }, [allComponents, wires, results, scope.signalNames, graphRegister?.signals]);

  const filteredSignalDetails = useMemo(() => {
    return allAvailableSignalDetails.filter((sig) => {
      if (categoryFilter === 'electrical' && sig.domain !== 'electrical') return false;
      if (categoryFilter === 'control' && sig.domain !== 'control') return false;
      if (signalSearch) {
        const q = signalSearch.toLowerCase();
        return (
          sig.name.toLowerCase().includes(q) || sig.sourceDescription.toLowerCase().includes(q)
        );
      }
      return true;
    });
  }, [allAvailableSignalDetails, categoryFilter, signalSearch]);

  const handleLayoutChange = (next: ScopeDisplayLayout) => {
    onDisplayLayoutChange(next);
    if (scopeComponent) {
      onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.scopeLayout, next);
    }
    if (graphRegister) {
      const numGraphs = next === 'overlay' ? 1 : next === '2-graphs' ? 2 : 3;
      onUpdateGraphRegister?.({ numGraphs });
    }
  };

  const handleMoveSignalToGraph = (signal: string, targetGraph: number) => {
    if (graphRegister) {
      let nextSignals = graphRegister.signals;
      if (!nextSignals.includes(signal)) {
        nextSignals = [...nextSignals, signal];
      }
      const nextGraphs = { ...graphRegister.signalGraphs, [signal]: targetGraph as 1 | 2 | 3 };
      onUpdateGraphRegister?.({ signals: nextSignals, signalGraphs: nextGraphs });
      return;
    }
    if (scopeComponent) {
      const current = { ...signalGraphs };
      current[signal] = targetGraph;
      const serialized = serializeSignalGraphs(current);
      onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.signalGraphs, serialized);
    }
  };

  const handleAutoGroupSignals = () => {
    const targetSignals = graphRegister ? graphRegister.signals : channels;
    const layout = graphRegister
      ? graphRegister.numGraphs === 2
        ? '2-graphs'
        : '3-graphs'
      : displayLayout;
    const mapping: Record<string, number> = {};
    for (const sig of targetSignals) {
      mapping[sig] = defaultGraphForSignal(sig, layout);
    }
    if (graphRegister) {
      onUpdateGraphRegister?.({ signalGraphs: mapping as Record<string, 1 | 2 | 3> });
    } else if (scopeComponent) {
      const serialized = serializeSignalGraphs(mapping);
      onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.signalGraphs, serialized);
    }
  };

  const handleYScaleToggle = () => {
    const currentMode = graphRegister ? graphRegister.yScaleMode : scope.yScaleMode;
    const next = currentMode === 'fixed' ? 'auto' : 'fixed';
    scope.setYScaleMode(next);
    if (graphRegister) {
      onUpdateGraphRegister?.({ yScaleMode: next });
    }
    if (scopeComponent) {
      onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.yScaleMode, next);
    }
  };

  const handleToggleChannel = (name: string) => {
    scope.toggleSignal(name);
    const nextHidden = { ...scope.hiddenSignals, [name]: !scope.hiddenSignals[name] };
    if (graphRegister) {
      const nextArr = Object.entries(nextHidden)
        .filter(([, hidden]) => hidden)
        .map(([n]) => n);
      onUpdateGraphRegister?.({ hiddenSignals: nextArr });
    }
    if (scopeComponent) {
      const serialized = Object.entries(nextHidden)
        .filter(([, hidden]) => hidden)
        .map(([n]) => n)
        .join(',');
      onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.hiddenSignals, serialized);
    }
  };

  const availableUnaddedSignals = useMemo(() => {
    return scope.signalNames.filter((s) => !channels.includes(s) && !isGroundSignal(s));
  }, [scope.signalNames, channels]);

  const handleAutoAddProbes = () => {
    if (graphRegister) {
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
      const nextSignals = Array.from(new Set([...graphRegister.signals, ...probeSignals]));
      onUpdateGraphRegister?.({ signals: nextSignals });
      return;
    }

    if (!onSetLabel || !scopeComponent) return;
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
    const curEnabled = graphRegister ? graphRegister.cursorsEnabled : scope.cursorsEnabled;
    if (curEnabled) {
      scope.clearCursors();
      if (graphRegister) onUpdateGraphRegister?.({ cursorsEnabled: false });
      if (scopeComponent) onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.cursorsEnabled, false);
    } else {
      scope.setCursorPreset();
      if (graphRegister) onUpdateGraphRegister?.({ cursorsEnabled: true });
      if (scopeComponent) onSetParameter?.(scopeComponent.name, SCOPE_SETTING_KEYS.cursorsEnabled, true);
    }
  };

  const handleExportCsv = () => {
    const sigList = graphRegister ? graphRegister.signals : channels;
    if (!results || !scope.timeArray.length || sigList.length === 0) return;
    const headers = ['time', ...sigList];
    const rows = [headers.join(',')];

    for (let i = 0; i < scope.timeArray.length; i++) {
      const row = [scope.timeArray[i], ...sigList.map((s) => results[s]?.[i] ?? 0)];
      rows.push(row.join(','));
    }

    const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${graphRegister?.name || scopeComponent?.name || 'scope'}_results_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Cursor measurements restricted strictly to active channels
  const filteredCursorChannels = useMemo(() => {
    if (!scope.cursorMeasurements) return [];
    const activeSigList = graphRegister ? graphRegister.signals : channels;
    return scope.cursorMeasurements.channels.filter((ch) => activeSigList.includes(ch.name));
  }, [scope.cursorMeasurements, graphRegister?.signals, channels]);

  const effectiveNumGraphs: 1 | 2 | 3 = graphRegister
    ? graphRegister.numGraphs
    : displayLayout === 'overlay'
      ? 1
      : displayLayout === '2-graphs'
        ? 2
        : 3;

  return (
    <div className="properties-container scope-properties-panel" data-testid="scope-properties-panel">
      {/* Scope Header */}
      <div className="properties-header-bar">
        {graphRegister ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1, minWidth: 0 }}>
            <span className="properties-title" style={{ whiteSpace: 'nowrap', fontSize: '12px' }}>
              Register:
            </span>
            <input
              type="text"
              className="prop-input"
              value={graphRegister.name}
              onChange={(e) => onUpdateGraphRegister?.({ name: e.target.value })}
              style={{ fontWeight: 600, fontSize: '12px', padding: '2px 6px', flex: 1, minWidth: 60 }}
              aria-label="Graph Register Name"
            />
            <span className="workspace-tab-badge" title="Number of signals in this register">
              {graphRegister.signals.length} sigs
            </span>
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span className="properties-title">Scope Settings: {scopeComponent?.name}</span>
            <span className="workspace-tab-badge" title="Number of channels wired to this scope">
              {channels.length} ch
            </span>
          </div>
        )}
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
          {graphRegister ? (
            <div
              className="segmented-control"
              style={{ width: '100%', display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)' }}
            >
              <button
                type="button"
                className={`segmented-btn ${graphRegister.numGraphs === 1 ? 'active' : ''}`}
                onClick={() => {
                  onUpdateGraphRegister?.({ numGraphs: 1 });
                  onDisplayLayoutChange('overlay');
                }}
                style={{ textAlign: 'center', justifyContent: 'center', padding: '4px 2px', fontSize: '11px' }}
                title="Overlay all channels on 1 combined graph"
              >
                📈 1 Graph
              </button>
              <button
                type="button"
                className={`segmented-btn ${graphRegister.numGraphs === 2 ? 'active' : ''}`}
                onClick={() => {
                  onUpdateGraphRegister?.({ numGraphs: 2 });
                  onDisplayLayoutChange('2-graphs');
                }}
                style={{ textAlign: 'center', justifyContent: 'center', padding: '4px 2px', fontSize: '11px' }}
                title="Split channels across 2 separate subplots"
              >
                📊 2 Graphs
              </button>
              <button
                type="button"
                className={`segmented-btn ${graphRegister.numGraphs === 3 ? 'active' : ''}`}
                onClick={() => {
                  onUpdateGraphRegister?.({ numGraphs: 3 });
                  onDisplayLayoutChange('3-graphs');
                }}
                style={{ textAlign: 'center', justifyContent: 'center', padding: '4px 2px', fontSize: '11px' }}
                title="Split channels across 3 separate subplots"
              >
                📑 3 Graphs
              </button>
            </div>
          ) : (
            <div
              className="segmented-control"
              style={{ width: '100%', display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)' }}
            >
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
          )}
        </div>

        {/* Labeled Signals Matrix for GraphRegister */}
        {graphRegister && (
          <div className="prop-section" data-testid="scope-signals-matrix">
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 6,
              }}
            >
              <div className="prop-section-title" style={{ margin: 0 }}>
                Signals & Subplots
              </div>
              {effectiveNumGraphs > 1 && graphRegister.signals.length > 0 && (
                <button
                  type="button"
                  className="sim-mini-btn"
                  onClick={handleAutoGroupSignals}
                  title="Auto-organize: Voltages -> Graph 1, Currents -> Graph 2, Control -> Graph 3"
                  style={{ fontSize: '10px', padding: '1px 6px' }}
                >
                  ⚡ Auto-Group
                </button>
              )}
            </div>

            {/* Search & Domain Filter Pills */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 8 }}>
              <input
                type="search"
                className="prop-input"
                placeholder="Search labeled signals..."
                value={signalSearch}
                onChange={(e) => setSignalSearch(e.target.value)}
                style={{ fontSize: '11px', padding: '3px 8px' }}
                aria-label="Search signals"
              />
              <div style={{ display: 'flex', gap: 4 }}>
                {(['all', 'electrical', 'control'] as const).map((cat) => (
                  <button
                    key={cat}
                    type="button"
                    className={`sim-mini-btn ${categoryFilter === cat ? 'active' : ''}`}
                    onClick={() => setCategoryFilter(cat)}
                    style={{
                      flex: 1,
                      fontSize: '10px',
                      padding: '2px 4px',
                      textAlign: 'center',
                    }}
                  >
                    {cat === 'all'
                      ? 'All'
                      : cat === 'electrical'
                        ? 'Electrical (V/I)'
                        : 'Control (CTRL)'}
                  </button>
                ))}
              </div>
            </div>

            {/* Available Signals Scrollable List */}
            <div
              className="sim-signals-scroll"
              style={{
                maxHeight: 260,
                overflowY: 'auto',
                display: 'flex',
                flexDirection: 'column',
                gap: 3,
                paddingRight: 2,
              }}
            >
              {filteredSignalDetails.length === 0 ? (
                <div
                  style={{
                    fontSize: '11px',
                    color: 'var(--text-muted)',
                    fontStyle: 'italic',
                    padding: '8px 0',
                  }}
                >
                  No matching signals found.
                </div>
              ) : (
                filteredSignalDetails.map((sig) => {
                  const isSelected = graphRegister.signals.includes(sig.name);
                  const color = scope.colorOf(sig.name);
                  const isHidden = graphRegister.hiddenSignals.includes(sig.name);
                  const assignedG = graphRegister.signalGraphs[sig.name] ?? 1;

                  return (
                    <div
                      key={sig.name}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '3px 6px',
                        borderRadius: 4,
                        background: isSelected ? 'var(--surface-hover)' : 'transparent',
                        border: `1px solid ${isSelected ? 'var(--border)' : 'transparent'}`,
                      }}
                    >
                      {/* Checkbox to include/exclude */}
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => {
                          const nextSignals = isSelected
                            ? graphRegister.signals.filter((s) => s !== sig.name)
                            : [...graphRegister.signals, sig.name];
                          const nextGraphs = { ...graphRegister.signalGraphs };
                          if (!isSelected && !nextGraphs[sig.name]) {
                            nextGraphs[sig.name] =
                              graphRegister.numGraphs > 1
                                ? (defaultGraphForSignal(
                                    sig.name,
                                    graphRegister.numGraphs === 2 ? '2-graphs' : '3-graphs',
                                  ) as 1 | 2 | 3)
                                : 1;
                          }
                          onUpdateGraphRegister?.({
                            signals: nextSignals,
                            signalGraphs: nextGraphs,
                          });
                        }}
                        title={
                          isSelected
                            ? `Remove ${sig.name} from register`
                            : `Add ${sig.name} to register`
                        }
                        style={{ cursor: 'pointer', margin: 0 }}
                        aria-label={`Toggle ${sig.name}`}
                      />

                      {/* Color dot */}
                      <span
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: '50%',
                          backgroundColor: color,
                          flexShrink: 0,
                        }}
                      />

                      {/* Name & Domain Badge */}
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'baseline',
                          gap: 4,
                          flex: 1,
                          minWidth: 0,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                        title={`${sig.name} — ${sig.sourceDescription}`}
                      >
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: isSelected ? 600 : 400,
                            color: isHidden ? 'var(--text-dim)' : 'var(--text)',
                          }}
                        >
                          {sig.name}
                        </span>
                        <span
                          style={{
                            fontSize: '9px',
                            padding: '0 3px',
                            borderRadius: 2,
                            background:
                              sig.domain === 'control'
                                ? 'rgba(56, 189, 248, 0.15)'
                                : 'rgba(234, 179, 8, 0.15)',
                            color:
                              sig.domain === 'control'
                                ? 'var(--accent, #38bdf8)'
                                : 'var(--warning, #eab308)',
                            flexShrink: 0,
                          }}
                        >
                          {sig.category === 'voltage'
                            ? 'V'
                            : sig.category === 'current'
                              ? 'A'
                              : sig.domain === 'control'
                                ? 'CTRL'
                                : 'SIG'}
                        </span>
                      </div>

                      {/* Quick G1 / G2 / G3 Buttons */}
                      {graphRegister.numGraphs > 1 && (
                        <div style={{ display: 'inline-flex', gap: 2, flexShrink: 0 }}>
                          {([1, 2, ...(graphRegister.numGraphs === 3 ? [3] : [])] as const).map(
                            (g) => {
                              const isActive = assignedG === g && isSelected;
                              return (
                                <button
                                  key={g}
                                  type="button"
                                  onClick={() => {
                                    let nextSignals = graphRegister.signals;
                                    if (!nextSignals.includes(sig.name)) {
                                      nextSignals = [...nextSignals, sig.name];
                                    }
                                    const nextGraphs = {
                                      ...graphRegister.signalGraphs,
                                      [sig.name]: g as 1 | 2 | 3,
                                    };
                                    onUpdateGraphRegister?.({
                                      signals: nextSignals,
                                      signalGraphs: nextGraphs,
                                    });
                                  }}
                                  title={`Assign ${sig.name} to Graph ${g}`}
                                  style={{
                                    border: isActive
                                      ? '1px solid var(--accent, #38bdf8)'
                                      : '1px solid var(--border)',
                                    background: isActive
                                      ? 'var(--accent, #38bdf8)'
                                      : 'transparent',
                                    color: isActive ? '#ffffff' : 'var(--text-muted)',
                                    borderRadius: 3,
                                    fontSize: '9px',
                                    padding: '1px 3px',
                                    cursor: 'pointer',
                                    fontWeight: isActive ? 700 : 500,
                                  }}
                                >
                                  G{g}
                                </button>
                              );
                            },
                          )}
                        </div>
                      )}

                      {/* Visibility Eye Toggle */}
                      {isSelected && (
                        <button
                          type="button"
                          onClick={() => {
                            const nextHidden = isHidden
                              ? graphRegister.hiddenSignals.filter((s) => s !== sig.name)
                              : [...graphRegister.hiddenSignals, sig.name];
                            onUpdateGraphRegister?.({ hiddenSignals: nextHidden });
                          }}
                          title={isHidden ? `Show ${sig.name}` : `Mute / Hide ${sig.name}`}
                          style={{
                            background: 'none',
                            border: 'none',
                            cursor: 'pointer',
                            padding: '0 2px',
                            fontSize: '11px',
                            opacity: isHidden ? 0.4 : 0.9,
                          }}
                        >
                          {isHidden ? '🚫' : '👁'}
                        </button>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </div>
        )}

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

          {/* Vertical & Scale */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 12 }}>
            <div className="sim-sub-heading" style={{ margin: 0 }}>
              Vertical (Scale)
            </div>
          </div>
          <div style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
            <button
              type="button"
              className={`sim-toggle-btn ${(graphRegister ? graphRegister.yScaleMode : scope.yScaleMode) === 'fixed' ? 'active' : ''}`}
              onClick={handleYScaleToggle}
              title={
                (graphRegister ? graphRegister.yScaleMode : scope.yScaleMode) === 'fixed'
                  ? 'Locked: Zooming time preserves voltage scale'
                  : 'Auto: Rescales voltage to fit visible points'
              }
              style={{ flex: 1 }}
            >
              ↕ Scale: {(graphRegister ? graphRegister.yScaleMode : scope.yScaleMode) === 'fixed' ? 'Fixed' : 'Auto'}
            </button>
          </div>

          {/* Legacy Scope Component Channels List */}
          {!graphRegister && scopeComponent && (
            <>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 }}>
                <div className="sim-sub-heading" style={{ margin: 0 }}>
                  Channels
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
            </>
          )}

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

        {/* Results & CSV Export for THIS Scope / Register */}
        {results && ((graphRegister ? graphRegister.signals.length : channels.length) > 0) && (
          <div className="prop-section">
            <div className="prop-section-title">Signals & Export</div>
            <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: 8 }}>
              {(graphRegister ? graphRegister.signals.length : channels.length)} channel
              {(graphRegister ? graphRegister.signals.length : channels.length) !== 1 ? 's' : ''} in {graphRegister?.name || scopeComponent?.name || 'Scope'}
            </div>
            <button
              type="button"
              className="sim-btn export"
              onClick={handleExportCsv}
              style={{ width: '100%', justifyContent: 'center' }}
              title={`Export waveform data for ${graphRegister?.name || scopeComponent?.name || 'Scope'} to CSV`}
            >
              Export CSV File
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
