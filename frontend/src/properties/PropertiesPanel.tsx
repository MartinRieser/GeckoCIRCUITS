/**
 * Structured Properties Inspector: component header with live symbol preview,
 * semantic engineering parameters with SI prefix notation (10k, 4.7u, 100n, 1M),
 * unit badges, terminal net label manager, and quick actions (Rotate, Delete, Duplicate).
 */
import { useEffect, useState, useMemo } from 'react';
import type { EditorComponent, EditorWire, SimulationStatus } from '../model/types';
import type { ScriptDebugSnapshot, ScriptDebugState } from '../api/client';
import {
  buildScriptWatchRows,
  clampInputTerminalCount,
  clampOutputTerminalCount,
  isDebugPausedAnywhere,
  resolveScriptPauseSnapshot,
  resolveScriptWatchSnapshot,
  useScriptBlockEditorState,
} from './scriptBlockShared';
import { ScriptCodeEditor } from './ScriptCodeEditor';
import {
  CTRL_TYPE,
  getComponentMeta,
  parseEngineeringValue,
  formatEngineeringValue,
  isGateDriver,
  isSwitchComponent,
  isAmmeterComponent,
  isVoltmeterComponent,
  isMutualCoupler,
  isCoupledInductor,
  getMutualCouplingUids,
  getCoupledComponentName,
  extractAvailableSignals,
  isParameterVisible,
  resolveParameterDef,
  type ParameterDef,
} from '../model/componentSchema';
import { isScopeComponent } from '../simulation/scopes';
import { CHANNEL_TRACE_COLORS, CHANNEL_TRACE_COLORS_LIGHT } from '../simulation/traceColors';
import { terminalPositions } from '../model/geometry';
import { SymbolPreview } from '../canvas/symbols';
import { computeCurrentDirectionHint } from '../canvas/sheetGuides';

// Same palette the scope view uses for its traces, so a channel's badge
// color matches its waveform color everywhere.
const CHANNEL_COLORS_DARK = CHANNEL_TRACE_COLORS;

/**
 * Script debug session state and actions shared with the script block editor:
 * breakpoints per block, the paused-at-breakpoint snapshot, and the polled
 * watch state of the active run. Optional; the editor hides its debug UI
 * when absent.
 */
export interface ScriptDebugPanelState {
  /** Current simulation status (null when no run was started yet). */
  status: SimulationStatus | null;
  /** Breakpoint source lines per script block (component) name. */
  breakpoints: Record<string, number[]>;
  /** Snapshot of the current breakpoint pause (SSE), or null. */
  debugPause: ScriptDebugSnapshot | null;
  /** Polled script debug/watch state of the active run, or null. */
  debugState: ScriptDebugState | null;
  /** Toggles a breakpoint line of a script block. */
  onToggleBreakpoint: (blockName: string, line: number) => void;
  /** Continues the paused run to the next breakpoint. */
  onDebugResume: () => void;
  /** Executes one statement of the paused block, then pauses again. */
  onDebugStep: () => void;
}

/**
 * Properties for the {@link PropertiesPanel} inspector sidebar.
 */
export interface PropertiesPanelProps {
  /** The currently selected schematic component, or null if none selected. */
  component: EditorComponent | null;
  /** Complete list of schematic components for coupling and signal reference resolution. */
  allComponents?: EditorComponent[];
  /** Schematic wires for signal extraction. */
  wires?: EditorWire[];
  /** Script debug session state; omit to hide the script debugging UI. */
  scriptDebug?: ScriptDebugPanelState;
  /** Callback to rename a component. */
  onRename: (name: string, newName: string) => void;
  /** Callback to set or update a component parameter value. */
  onSetParameter: (name: string, key: string, value: number | string) => void;
  /** Callback to update a terminal net label. */
  onSetLabel: (component: string, side: 'x' | 'y', indexOrLabel: number | string, maybeLabel?: string) => void;
  /** Callback to remove a terminal label entirely, shrinking the label array (scope channels). */
  onRemoveLabel?: (component: string, side: 'x' | 'y', index: number) => void;
  /** Callback to select a coupled or referenced component. */
  onSelectComponent?: (name: string) => void;
  /** Callback to rotate the currently selected component. */
  onRotate?: (name: string) => void;
  /** Callback to delete the currently selected component. */
  onDelete?: (name: string) => void;
  /** Callback to switch to the Oscilloscope view tab for a specific scope block. */
  onOpenScopeTab?: (scopeName: string) => void;
  /** Callback to collapse the properties panel. */
  onCollapse?: () => void;
  /** Active UI theme; picks the light-calibrated channel badge palette. */
  theme?: 'dark' | 'light';
}

/**
 * Structured Properties Inspector panel for viewing and editing schematic component parameters,
 * terminal labels, switch coupling, probe bindings, and engineering notations.
 */
export function PropertiesPanel({
  component,
  allComponents,
  wires,
  scriptDebug,
  onRename,
  onSetParameter,
  onSetLabel,
  onRemoveLabel,
  onSelectComponent,
  onRotate,
  onDelete,
  onOpenScopeTab,
  onCollapse,
  theme = 'dark',
}: PropertiesPanelProps) {
  const CHANNEL_COLORS = theme === 'light' ? CHANNEL_TRACE_COLORS_LIGHT : CHANNEL_COLORS_DARK;
  const [name, setName] = useState('');

  useEffect(() => {
    setName(component?.name ?? '');
  }, [component?.name]);

  const meta = useMemo(() => {
    if (!component) return null;
    return getComponentMeta(component.type, component.family, component.name);
  }, [component]);

  const isGate = isGateDriver(component);
  const isSwitch = isSwitchComponent(component);
  const isAmmeter = isAmmeterComponent(component);
  const isVoltmeter = isVoltmeterComponent(component);
  const isCoupler = isMutualCoupler(component);
  const isScope = component ? isScopeComponent(component) : false;
  const coupledTarget = getCoupledComponentName(component);
  // A name reference that resolves to nothing on the sheet is dead (its
  // target was deleted): the pill must say so instead of showing green.
  const coupledTargetMissing =
    !!coupledTarget && !(allComponents || []).some((c) => c.name === coupledTarget);

  const availableSignals = useMemo(() => {
    return extractAvailableSignals(allComponents || [], wires || []);
  }, [allComponents, wires]);

  const availableSwitches = useMemo(() => {
    return (allComponents || []).filter((c) => isSwitchComponent(c) && c.name !== component?.name);
  }, [allComponents, component?.name]);

  const availableGateDrivers = useMemo(() => {
    return (allComponents || []).filter((c) => isGateDriver(c) && c.name !== component?.name);
  }, [allComponents, component?.name]);

  const availableCurrentTargets = useMemo(() => {
    return (allComponents || []).filter(
      (c) =>
        c.name !== component?.name &&
        (c.family === 'LK' ||
          c.family === 'CIRCUIT' ||
          c.family === 'THERMAL' ||
          (c.family !== 'CONTROL' && c.type < 100) ||
          isSwitchComponent(c)),
    );
  }, [allComponents, component?.name]);

  const currentDirectionHint = useMemo(() => {
    if (!isAmmeter || !component || !coupledTarget) return null;
    const targetComp = (allComponents || []).find((c) => c.name === coupledTarget);
    if (!targetComp) return null;
    return computeCurrentDirectionHint(component, targetComp);
  }, [isAmmeter, component, coupledTarget, allComponents]);

  const voltmeterTargetHint = useMemo(() => {
    if (!isVoltmeter || !component || !coupledTarget) return null;
    const targetComp = (allComponents || []).find((c) => c.name === coupledTarget);
    if (!targetComp) return null;
    return computeCurrentDirectionHint(component, targetComp);
  }, [isVoltmeter, component, coupledTarget, allComponents]);

  // LK_M couples exactly two coupled inductors (LK_LKOP2); both selects
  // below offer only those, keyed by their backend uid.
  const availableCoupledInductors = useMemo(() => {
    return (allComponents || []).filter((c) => isCoupledInductor(c) && c.uid != null);
  }, [allComponents]);

  if (!component || !meta) {
    return (
      <div className="properties-container">
        <div className="properties-header-bar">
          <span className="properties-title">Properties</span>
          {onCollapse && (
            <div className="properties-header-actions">
              <button
                type="button"
                className="action-icon-btn collapse-btn"
                onClick={onCollapse}
                title="Collapse properties panel (Ctrl+I)"
              >
                ▶
              </button>
            </div>
          )}
        </div>
        <div className="properties-empty-state">
          <div className="empty-icon">&#9671;</div>
          <span className="empty-title">No selection</span>
          <p className="empty-hint">
            Select a component on the schematic sheet to inspect and edit its parameters, or double-click to open.
          </p>
        </div>
      </div>
    );
  }

  const handleNameCommit = () => {
    const trimmed = name.trim();
    if (trimmed && trimmed !== component.name) {
      onRename(component.name, trimmed);
    } else {
      setName(component.name);
    }
  };

  return (
    <div className="properties-container">
      {/* Header with symbol thumbnail and quick actions */}
      <div className="properties-header-bar">
        <span className="properties-title">Properties</span>
        <div className="properties-header-actions">
          {onRotate && (
            <button
              type="button"
              className="action-icon-btn"
              onClick={() => onRotate(component.name)}
              title="Rotate 90° (R)"
            >
              ⟳
            </button>
          )}
          {onDelete && (
            <button
              type="button"
              className="action-icon-btn danger"
              onClick={() => onDelete(component.name)}
              title="Delete Component (Del)"
            >
              &#10005;
            </button>
          )}
          {onCollapse && (
            <button
              type="button"
              className="action-icon-btn collapse-btn"
              onClick={onCollapse}
              title="Collapse properties panel (Ctrl+I)"
            >
              ▶
            </button>
          )}
        </div>
      </div>

      <div className="properties-body">
        {/* Component summary card */}
        <div className="component-summary-card">
          <div className="summary-symbol-preview">
            <SymbolPreview type={component.type} family={component.family} size={48} />
          </div>
          <div className="summary-details">
            <div className="summary-type-name">{meta.displayName}</div>
            <div className="summary-badges">
              <span className="type-badge">{component.family} · Type {component.type}</span>
              <span className="pos-badge">
                ({component.position[0]}, {component.position[1]})
              </span>
            </div>
          </div>
        </div>

        {/* Scope Instrument Tab Action */}
        {onOpenScopeTab && isScopeComponent(component) && (
          <button
            type="button"
            className="sim-btn run"
            style={{ width: '100%', marginBottom: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
            onClick={() => onOpenScopeTab(component.name)}
          >
            📺 Open {component.name} Scope Tab ↗
          </button>
        )}

        {/* Component Name Field */}
        <div className="prop-group">
          <label className="prop-label" htmlFor="comp-name-input">
            Component Identifier
          </label>
          <div className="prop-input-wrap">
            <input
              id="comp-name-input"
              type="text"
              className="prop-input name-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              onBlur={handleNameCommit}
              onKeyDown={(e) => e.key === 'Enter' && handleNameCommit()}
              placeholder="e.g. R1, C1, V_in"
            />
          </div>
        </div>

        {/* Gate Driver to Switch Coupling */}
        {isGate && (
          <div className="prop-section coupling-section">
            <div className="prop-section-title">
              <span>⚡ Switch Coupling</span>
              {coupledTarget && !coupledTargetMissing && onSelectComponent && (
                <button
                  type="button"
                  className="link-action-btn"
                  onClick={() => onSelectComponent(coupledTarget)}
                  title={`Select ${coupledTarget} on schematic`}
                >
                  Go to {coupledTarget} ↗
                </button>
              )}
            </div>
            <div className="prop-desc-hint">
              Select which semiconductor switch is controlled by this gate driver.
            </div>
            <div className="coupling-control-row">
              <select
                className="prop-input coupling-select"
                value={coupledTarget}
                onChange={(e) => {
                  const target = e.target.value;
                  onSetParameter(component.name, 'coupledComponent', target);
                  if (target) {
                    onSetParameter(target, 'coupledComponent', component.name);
                  }
                }}
              >
                <option value="">-- No Switch Coupled --</option>
                {availableSwitches.map((sw) => (
                  <option key={sw.name} value={sw.name}>
                    {sw.name} ({getComponentMeta(sw.type, sw.family, sw.name)?.displayName || 'Switch'})
                  </option>
                ))}
              </select>
            </div>
            {coupledTarget ? (
              coupledTargetMissing ? (
                <div className="coupling-status-pill warning">
                  <span>
                    ⚠️ References missing component <strong>{coupledTarget}</strong> (deleted?) — pick a switch above
                  </span>
                </div>
              ) : (
                <div className="coupling-status-pill active">
                  <span className="coupling-status-dot" />
                  <span>Controls switch <strong>{coupledTarget}</strong></span>
                </div>
              )
            ) : (
              <div className="coupling-status-pill warning">
                <span>⚠️ Gate driver is uncoupled (select a switch above)</span>
              </div>
            )}
          </div>
        )}

        {/* Switch to Gate Driver Coupling */}
        {isSwitch && (
          <div className="prop-section coupling-section">
            <div className="prop-section-title">
              <span>⚡ Gate Drive Coupling</span>
              {coupledTarget && !coupledTargetMissing && onSelectComponent && (
                <button
                  type="button"
                  className="link-action-btn"
                  onClick={() => onSelectComponent(coupledTarget)}
                  title={`Select ${coupledTarget} on schematic`}
                >
                  Go to {coupledTarget} ↗
                </button>
              )}
            </div>
            <div className="prop-desc-hint">
              Gate driver block that commands this switch's conduction state.
            </div>
            <div className="coupling-control-row">
              <select
                className="prop-input coupling-select"
                value={coupledTarget}
                onChange={(e) => {
                  const target = e.target.value;
                  onSetParameter(component.name, 'coupledComponent', target);
                  if (target) {
                    onSetParameter(target, 'coupledComponent', component.name);
                  }
                }}
              >
                <option value="">-- Direct / Uncoupled --</option>
                {availableGateDrivers.map((gd) => (
                  <option key={gd.name} value={gd.name}>
                    {gd.name} (Gate Driver)
                  </option>
                ))}
              </select>
            </div>
            {coupledTarget ? (
              coupledTargetMissing ? (
                <div className="coupling-status-pill warning">
                  <span>
                    ⚠️ Gate driver <strong>{coupledTarget}</strong> is gone (deleted?) — recouple this switch
                  </span>
                </div>
              ) : (
                <div className="coupling-status-pill active">
                  <span className="coupling-status-dot" />
                  <span>Driven by gate driver <strong>{coupledTarget}</strong></span>
                </div>
              )
            ) : (
              <div className="coupling-status-pill">
                <span>Direct switching (no gate driver block attached)</span>
              </div>
            )}
          </div>
        )}

        {/* Ammeter Current Measurement Target Selection */}
        {isAmmeter && (
          <div className="prop-section coupling-section">
            <div className="prop-section-title">
              <span>⚡ Current Measurement Target</span>
              {coupledTarget && !coupledTargetMissing && onSelectComponent && (
                <button
                  type="button"
                  className="link-action-btn"
                  onClick={() => onSelectComponent(coupledTarget)}
                  title={`Select ${coupledTarget} on schematic`}
                >
                  Go to {coupledTarget} ↗
                </button>
              )}
            </div>
            <div className="prop-desc-hint">
              Select which circuit component this ammeter measures branch current through.
            </div>
            <div className="coupling-control-row">
              <select
                className="prop-input coupling-select"
                value={coupledTarget}
                onChange={(e) => {
                  const target = e.target.value;
                  onSetParameter(component.name, 'coupledComponent', target);
                  // Update signal label automatically if empty or starting with i
                  const currentOut = component.outputLabels?.[0] || '';
                  if (target && (!currentOut || currentOut.startsWith('i'))) {
                    const cleanTarget = target.replace(/[^a-zA-Z0-9]/g, '');
                    onSetLabel(component.name, 'y', 0, `i${cleanTarget}`);
                  }
                }}
              >
                <option value="">-- Select Component to Measure --</option>
                {availableCurrentTargets.map((comp) => {
                  const m = getComponentMeta(comp.type, comp.family, comp.name);
                  return (
                    <option key={comp.name} value={comp.name}>
                      {comp.name} ({m?.displayName || comp.family})
                    </option>
                  );
                })}
              </select>
            </div>
            {coupledTarget ? (
              coupledTargetMissing ? (
                <div className="coupling-status-pill warning">
                  <span>
                    ⚠️ Measured component <strong>{coupledTarget}</strong> is gone (deleted?) — exports 0
                  </span>
                </div>
              ) : (
                <div className="coupling-status-pill active" style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: '4px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span className="coupling-status-dot" />
                    <span>
                      Measures branch current through <strong>{coupledTarget}</strong> (Signal: <code>{component.outputLabels?.[0] || 'i' + coupledTarget}</code>)
                    </span>
                  </div>
                  {currentDirectionHint && (
                    <div style={{ fontSize: '11px', color: '#cbd5e1', paddingLeft: '14px', display: 'flex', alignItems: 'center', gap: '5px' }}>
                      <span style={{ color: '#f59e0b', fontWeight: 'bold' }}>➔ Reference:</span>
                      <span>
                        Positive when flowing <strong>{currentDirectionHint.directionText}</strong> (pin 1 → pin 2)
                      </span>
                    </div>
                  )}
                </div>
              )
            ) : (
              <div className="coupling-status-pill warning">
                <span>⚠️ No component selected to measure (pick an inductor, resistor, diode, etc.)</span>
              </div>
            )}
          </div>
        )}

        {/* Voltmeter Measurement Target (Component vs Nodes) */}
        {isVoltmeter && (
          <div className="prop-section coupling-section">
            <div className="prop-section-title">
              <span>⚡ Voltage Measurement Target</span>
              {coupledTarget && !coupledTargetMissing && onSelectComponent && (
                <button
                  type="button"
                  className="link-action-btn"
                  onClick={() => onSelectComponent(coupledTarget)}
                  title={`Select ${coupledTarget} on schematic`}
                >
                  Go to {coupledTarget} ↗
                </button>
              )}
            </div>
            <div className="prop-desc-hint">
              Measure voltage across a circuit component or differentially between two named nodes.
            </div>

            {/* Mode Toggle: Component vs Differential Nodes */}
            <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
              <button
                type="button"
                className={`secondary-btn ${!component.parameters?.nodeA && !component.parameters?.nodeB ? 'active' : ''}`}
                style={{
                  flex: 1,
                  padding: '5px 8px',
                  fontSize: '11px',
                  background: (!component.parameters?.nodeA && !component.parameters?.nodeB) ? '#1e293b' : 'transparent',
                  border: (!component.parameters?.nodeA && !component.parameters?.nodeB) ? '1px solid #38bdf8' : '1px solid #334155',
                  color: (!component.parameters?.nodeA && !component.parameters?.nodeB) ? '#38bdf8' : '#94a3b8',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontWeight: (!component.parameters?.nodeA && !component.parameters?.nodeB) ? 'bold' : 'normal',
                }}
                onClick={() => {
                  onSetParameter(component.name, 'nodeA', '');
                  onSetParameter(component.name, 'nodeB', '');
                }}
              >
                Across Component
              </button>
              <button
                type="button"
                className={`secondary-btn ${(component.parameters?.nodeA || component.parameters?.nodeB) ? 'active' : ''}`}
                style={{
                  flex: 1,
                  padding: '5px 8px',
                  fontSize: '11px',
                  background: (component.parameters?.nodeA || component.parameters?.nodeB) ? '#1e293b' : 'transparent',
                  border: (component.parameters?.nodeA || component.parameters?.nodeB) ? '1px solid #38bdf8' : '1px solid #334155',
                  color: (component.parameters?.nodeA || component.parameters?.nodeB) ? '#38bdf8' : '#94a3b8',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontWeight: (component.parameters?.nodeA || component.parameters?.nodeB) ? 'bold' : 'normal',
                }}
                onClick={() => {
                  onSetParameter(component.name, 'coupledComponent', '');
                  if (!component.parameters?.nodeA) {
                    const firstSig = availableSignals.find((s) => s !== '0') || 'z1';
                    onSetParameter(component.name, 'nodeA', firstSig);
                  }
                  if (!component.parameters?.nodeB) {
                    onSetParameter(component.name, 'nodeB', '0');
                  }
                }}
              >
                Between Two Nodes
              </button>
            </div>

            {!(component.parameters?.nodeA || component.parameters?.nodeB) ? (
              /* Mode A: Across Component */
              <>
                <div className="coupling-control-row">
                  <select
                    className="prop-input coupling-select"
                    value={coupledTarget}
                    onChange={(e) => {
                      const target = e.target.value;
                      // The server's coupledComponent branch clears nodeA/nodeB
                      // itself; sending them here would race and wipe the target.
                      onSetParameter(component.name, 'coupledComponent', target);
                      const currentOut = component.outputLabels?.[0] || '';
                      if (target && (!currentOut || currentOut.startsWith('u') || currentOut.startsWith('v'))) {
                        const cleanTarget = target.replace(/[^a-zA-Z0-9]/g, '');
                        onSetLabel(component.name, 'y', 0, `u_${cleanTarget}`);
                      }
                    }}
                  >
                    <option value="">-- Select Component to Measure Across --</option>
                    {availableCurrentTargets.map((comp) => {
                      const m = getComponentMeta(comp.type, comp.family, comp.name);
                      return (
                        <option key={comp.name} value={comp.name}>
                          {comp.name} ({m?.displayName || comp.family})
                        </option>
                      );
                    })}
                  </select>
                </div>
                {coupledTarget ? (
                  coupledTargetMissing ? (
                    <div className="coupling-status-pill warning" style={{ marginTop: '8px' }}>
                      <span>
                        ⚠️ Measured component <strong>{coupledTarget}</strong> is gone (deleted?) — exports 0
                      </span>
                    </div>
                  ) : (
                    <div className="coupling-status-pill active" style={{ marginTop: '8px', display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: '4px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <span className="coupling-status-dot" />
                        <span>
                          Measures voltage across <strong>{coupledTarget}</strong> (Signal: <code>{component.outputLabels?.[0] || 'u_' + coupledTarget}</code>)
                        </span>
                      </div>
                      {voltmeterTargetHint && (
                        <div style={{ fontSize: '11px', color: '#cbd5e1', paddingLeft: '14px', display: 'flex', alignItems: 'center', gap: '5px' }}>
                          <span style={{ color: '#f87171', fontWeight: 'bold' }}>+ at pin 1</span>
                          <span style={{ color: '#94a3b8' }}>➔</span>
                          <span style={{ color: '#cd7f32', fontWeight: 'bold' }}>− at pin 2</span>
                          <span style={{ color: '#94a3b8' }}>({voltmeterTargetHint.directionText})</span>
                        </div>
                      )}
                    </div>
                  )
                ) : (
                  <div className="coupling-status-pill warning" style={{ marginTop: '8px' }}>
                    <span>⚠️ Select a circuit component (e.g. R.Last, C.1, D.1)</span>
                  </div>
                )}
              </>
            ) : (
              /* Mode B: Between Nodes / Wire Labels */
              <>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '8px' }}>
                  <div className="prop-field">
                    <label className="prop-label" style={{ fontSize: '11px' }}>Positive Node (+)</label>
                    <input
                      type="text"
                      className="prop-input"
                      list="circuit-available-signals"
                      placeholder="e.g. z1, in"
                      value={String(component.parameters?.nodeA ?? '')}
                      onChange={(e) => {
                        const val = e.target.value;
                        // The server's nodeA/nodeB branch clears any coupling itself;
                        // sending coupledComponent here too would race the nodeA patch.
                        onSetParameter(component.name, 'nodeA', val);
                        const currentOut = component.outputLabels?.[0] || '';
                        if (val && (!currentOut || currentOut.startsWith('u') || currentOut.startsWith('v'))) {
                          onSetLabel(component.name, 'y', 0, `u_${val}`);
                        }
                      }}
                    />
                  </div>
                  <div className="prop-field">
                    <label className="prop-label" style={{ fontSize: '11px' }}>Negative Node (-)</label>
                    <input
                      type="text"
                      className="prop-input"
                      list="circuit-available-signals"
                      placeholder="0 (GND)"
                      value={String(component.parameters?.nodeB ?? '0')}
                      onChange={(e) => {
                        // The server's nodeA/nodeB branch clears any coupling itself.
                        onSetParameter(component.name, 'nodeB', e.target.value);
                      }}
                    />
                  </div>
                </div>
                <div className="coupling-status-pill active">
                  <span className="coupling-status-dot" />
                  <span>
                    Measures V(<code>{String(component.parameters?.nodeA || '?')}</code>) − V(<code>{String(component.parameters?.nodeB || '0')}</code>) (Signal: <code>{component.outputLabels?.[0] || 'u_' + (component.parameters?.nodeA || 'meas')}</code>)
                  </span>
                </div>
              </>
            )}

            {/* Emitted Output Signal Name Field */}
            <div className="prop-field" style={{ marginTop: '10px' }}>
              <label className="prop-label" style={{ fontSize: '11px' }}>Emitted Signal Name</label>
              <input
                type="text"
                className="prop-input"
                placeholder="e.g. uOUT, u_RLast"
                value={component.outputLabels?.[0] || ''}
                onChange={(e) => {
                  onSetLabel(component.name, 'y', 0, e.target.value);
                }}
              />
            </div>
          </div>
        )}

        {/* Mutual Coupling (LK_M) target inductors. The coupler is pinless by
            design: it couples by uid reference, not by wires. */}
        {isCoupler &&
          (() => {
            const [uidA, uidB] = getMutualCouplingUids(component);
            const targetA = availableCoupledInductors.find((c) => c.uid === uidA);
            const targetB = availableCoupledInductors.find((c) => c.uid === uidB);
            const k = Number(component.parameters?.param0 ?? 0.98);
            const lA = Number(targetA?.parameters?.param0);
            const lB = Number(targetB?.parameters?.param0);
            const m =
              targetA && targetB && lA > 0 && lB > 0 ? k * Math.sqrt(lA * lB) : null;
            const resolved = !!targetA && !!targetB;

            const inductorSelect = (
              label: string,
              selected: number,
              onPick: (uid: number) => void,
            ) => (
              <div className="prop-field">
                <label className="prop-label" style={{ fontSize: '11px' }}>{label}</label>
                <select
                  className="prop-input coupling-select"
                  value={selected > 0 ? String(selected) : ''}
                  onChange={(e) => onPick(e.target.value === '' ? 0 : Number(e.target.value))}
                >
                  <option value="">-- Not assigned --</option>
                  {availableCoupledInductors.map((ind) => (
                    <option key={ind.name} value={String(ind.uid)}>
                      {ind.name} ({formatEngineeringValue(Number(ind.parameters?.param0 ?? 0))}H)
                    </option>
                  ))}
                </select>
              </div>
            );

            return (
              <div className="prop-section coupling-section">
                <div className="prop-section-title">
                  <span>🧲 Coupled Inductors</span>
                </div>
                <div className="prop-desc-hint">
                  This element is pinless by design — like a SPICE K statement it couples two
                  Coupled Inductors by reference, not by wires. The sheet draws the link as
                  dashed violet lines.
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '8px' }}>
                  {inductorSelect('Inductor 1', uidA, (uid) =>
                    onSetParameter(component.name, 'param1', uid),
                  )}
                  {inductorSelect('Inductor 2', uidB, (uid) =>
                    onSetParameter(component.name, 'param2', uid),
                  )}
                </div>
                {resolved ? (
                  <div className="coupling-status-pill active">
                    <span className="coupling-status-dot" />
                    <span>
                      Couples <strong>{targetA!.name}</strong> ⟷ <strong>{targetB!.name}</strong>
                      {' '}with k = {k}
                      {m != null && <> · M = k·√(L₁·L₂) ≈ <strong>{formatEngineeringValue(m)}H</strong></>}
                    </span>
                  </div>
                ) : (
                  <div className="coupling-status-pill warning">
                    <span>
                      ⚠️ Both slots must reference Coupled Inductors — an unresolvable coupler is
                      ignored by the simulation
                      {availableCoupledInductors.length === 0 &&
                        ' (none on the sheet yet: place two LK_LKOP2 Coupled Inductors first)'}
                    </span>
                  </div>
                )}
              </div>
            );
          })()}

        {/* Dedicated Script / Function Block Editor */}
        {(component.type === CTRL_TYPE.SCRIPT ||
          component.type === CTRL_TYPE.LEGACY_JAVA_FUNCTION ||
          meta.name === 'CTRL_SCRIPT') ? (
          <ScriptBlockEditor component={component} onSetParameter={onSetParameter} debug={scriptDebug} />
        ) : (
          meta.parameters.length > 0 && (
            <div className="prop-section">
              <div className="prop-section-title">Electrical Parameters</div>
              <div className="prop-fields-list">
                {meta.parameters
                  .filter((def) => isParameterVisible(def, component.parameters))
                  .map((def) => {
                    const resolved = resolveParameterDef(def, component.parameters);
                    const currentVal =
                      (component.parameters[resolved.key] as number) ?? resolved.defaultValue;
                    return (
                      <SemanticParameterField
                        key={resolved.key}
                        def={resolved}
                        value={currentVal}
                        onCommit={(val) => onSetParameter(component.name, resolved.key, val)}
                      />
                    );
                  })}
              </div>
            </div>
          )
        )}

        {/* Dedicated Scope Channels Inspector */}
        {isScope ? (
          <div className="prop-section scope-channels-section">
            <div className="prop-section-title">
              <span>Scope Channels ({component.inputLabels.length})</span>
              {onOpenScopeTab && (
                <button
                  type="button"
                  className="link-action-btn"
                  onClick={() => onOpenScopeTab(component.name)}
                  title="Open Oscilloscope Viewer tab"
                >
                  Open Scope Tab ↗
                </button>
              )}
            </div>
            <div className="prop-desc-hint">
              Select or type circuit signals recorded on each channel. Wires connected to scope pins bind signals automatically.
            </div>
            <div className="scope-channels-list">
              {(component.inputLabels.length > 0 ? component.inputLabels : ['']).map((lbl, idx) => {
                const color = CHANNEL_COLORS[idx % CHANNEL_COLORS.length];
                const terms = terminalPositions(component);
                const pin = terms.input[idx];
                const isWired = pin && wires
                  ? wires.some((w) => w.points.some((p) => Math.hypot(p[0] - pin.x, p[1] - pin.y) < 0.25))
                  : false;

                return (
                  <div key={idx} className="scope-channel-row">
                    <span
                      className="scope-channel-badge"
                      style={{ borderColor: color, color }}
                      title={`Oscilloscope Channel ${idx + 1}`}
                    >
                      CH{idx + 1}
                    </span>
                    <div className="terminal-input-wrap">
                      <input
                        type="text"
                        list="circuit-available-signals"
                        className="prop-input terminal-input"
                        value={lbl || ''}
                        placeholder={`Signal for CH${idx + 1} (e.g. uOUT, il1)`}
                        onChange={(e) => onSetLabel(component.name, 'x', idx, e.target.value)}
                      />
                      {isWired && (
                        <span
                          style={{
                            fontSize: '9px',
                            color: '#38bdf8',
                            background: '#0f172a',
                            border: '1px solid #0284c7',
                            borderRadius: '3px',
                            padding: '1px 5px',
                            marginRight: '4px',
                            whiteSpace: 'nowrap',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '2px',
                          }}
                          title="Physically wired on the schematic"
                        >
                          ⚡ Wired
                        </span>
                      )}
                      {lbl && (
                        <button
                          type="button"
                          className="terminal-clear-btn"
                          title="Clear channel signal"
                          onClick={() => onSetLabel(component.name, 'x', idx, '')}
                        >
                          ✕
                        </button>
                      )}
                    </div>
                    {component.inputLabels.length > 1 && onRemoveLabel && (
                      <button
                        type="button"
                        className="channel-remove-btn"
                        title={`Remove Channel ${idx + 1}`}
                        onClick={() => onRemoveLabel(component.name, 'x', idx)}
                      >
                        ✕
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
            <button
              type="button"
              className="add-channel-btn"
              onClick={() => {
                const nextIdx = component.inputLabels.length;
                onSetLabel(component.name, 'x', nextIdx, '');
              }}
            >
              + Add Channel (CH{component.inputLabels.length + 1})
            </button>
          </div>
        ) : (
          /* General Component Terminal Net Labels */
          <div className="prop-section">
            <div className="prop-section-title">Terminal Net Labels</div>
            <div className="prop-desc-hint">
              Assign net names to connect terminals across the circuit without wires.
            </div>
            <div className="terminal-labels-list">
              {Array.from({ length: Math.max(meta.terminals.input.length, component.inputLabels.length) }).map(
                (_, idx) => {
                  const terminalDef = meta.terminals.input[idx];
                  const sideLabel =
                    terminalDef?.label || (meta.terminals.input.length > 1 ? `In ${idx + 1}` : 'Input / T1');
                  const desc = terminalDef?.description || `Input Terminal ${idx + 1}`;
                  const currentLabel = component.inputLabels[idx] || '';
                  return (
                    <TerminalLabelRow
                      key={`in-${idx}`}
                      sideLabel={sideLabel}
                      desc={desc}
                      currentLabel={currentLabel}
                      datalistId="circuit-available-signals"
                      onCommit={(lbl) => onSetLabel(component.name, 'x', idx, lbl)}
                    />
                  );
                },
              )}
              {Array.from({ length: Math.max(meta.terminals.output.length, component.outputLabels.length) }).map(
                (_, idx) => {
                  const terminalDef = meta.terminals.output[idx];
                  const sideLabel =
                    terminalDef?.label || (meta.terminals.output.length > 1 ? `Out ${idx + 1}` : 'Output / T2');
                  const desc = terminalDef?.description || `Output Terminal ${idx + 1}`;
                  const currentLabel = component.outputLabels[idx] || '';
                  return (
                    <TerminalLabelRow
                      key={`out-${idx}`}
                      sideLabel={sideLabel}
                      desc={desc}
                      currentLabel={currentLabel}
                      datalistId="circuit-available-signals"
                      onCommit={(lbl) => onSetLabel(component.name, 'y', idx, lbl)}
                    />
                  );
                },
              )}
            </div>
          </div>
        )}

        <datalist id="circuit-available-signals">
          {availableSignals.map((sig) => (
            <option key={sig} value={sig} />
          ))}
        </datalist>
      </div>
    </div>
  );
}

/**
 * Individual engineering parameter field with SI notation support.
 */
function SemanticParameterField({
  def,
  value,
  onCommit,
}: {
  def: ParameterDef;
  value: number;
  onCommit: (val: number) => void;
}) {
  const [text, setText] = useState(() => formatEngineeringValue(value));
  const [isEditing, setIsEditing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isEditing) {
      setText(formatEngineeringValue(value));
    }
  }, [value, isEditing]);

  const handleCommit = () => {
    setIsEditing(false);
    const parsed = parseEngineeringValue(text);
    if (parsed === null || !Number.isFinite(parsed)) {
      setError(`"${text}" is not a valid number — try e.g. 4.7, 10k, 4k7, 100n`);
      return;
    }
    if (def.min !== undefined && parsed < def.min) {
      setError(`Minimum is ${formatEngineeringValue(def.min)}${def.unit ? ' ' + def.unit : ''}`);
      return;
    }
    if (def.max !== undefined && parsed > def.max) {
      setError(`Maximum is ${formatEngineeringValue(def.max)}${def.unit ? ' ' + def.unit : ''}`);
      return;
    }
    setError(null);
    if (parsed !== value) {
      onCommit(parsed);
      setText(formatEngineeringValue(parsed));
    } else {
      setText(formatEngineeringValue(value));
    }
  };

  return (
    <div className="prop-field">
      <div className="prop-field-header">
        <label className="prop-label" title={def.description}>
          {def.label}
        </label>
        {def.unit && <span className="prop-unit-badge">{def.unit}</span>}
      </div>

      {def.options ? (
        <>
          <select
            className="prop-select"
            value={Number(value)}
            onChange={(e) => onCommit(Number(e.target.value))}
          >
            {def.options.map((opt) => (
              <option key={opt.value} value={opt.value} title={opt.hint}>
                {opt.label}
              </option>
            ))}
          </select>
          {(() => {
            const selected = def.options.find((opt) => opt.value === Number(value));
            return selected?.hint ? (
              <div className="prop-desc-hint" style={{ margin: '2px 0 0 0' }}>
                {selected.hint}
              </div>
            ) : null;
          })()}
        </>
      ) : (
        <>
          <div className="prop-input-wrap">
            <input
              type="text"
              className={error ? 'prop-input prop-input-error' : 'prop-input'}
              value={text}
              title={error ?? undefined}
              onFocus={() => {
                setIsEditing(true);
                setError(null);
                setText(String(value));
              }}
              onChange={(e) => setText(e.target.value)}
              onBlur={handleCommit}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleCommit();
                if (e.key === 'Escape') {
                  setIsEditing(false);
                  setError(null);
                  setText(formatEngineeringValue(value));
                }
              }}
            />
            {/* Quick multiplier buttons */}
            <div className="prop-stepper-actions">
              <button
                type="button"
                className="step-btn"
                onClick={() => onCommit(value * 10)}
                title="Multiply by 10 (×10)"
              >
                ×10
              </button>
              <button
                type="button"
                className="step-btn"
                onClick={() => onCommit(value / 10)}
                title="Divide by 10 (÷10)"
              >
                ÷10
              </button>
            </div>
          </div>
          {error && <div className="prop-field-error">{error}</div>}
        </>
      )}
    </div>
  );
}

/**
 * Terminal Net Label Editor Row.
 */
function TerminalLabelRow({
  sideLabel,
  desc,
  currentLabel,
  datalistId,
  onCommit,
}: {
  sideLabel: string;
  desc: string;
  currentLabel: string;
  datalistId?: string;
  onCommit: (lbl: string) => void;
}) {
  const [label, setLabel] = useState(currentLabel);

  useEffect(() => {
    setLabel(currentLabel);
  }, [currentLabel]);

  const handleCommit = () => {
    const trimmed = label.trim();
    if (trimmed !== currentLabel) {
      onCommit(trimmed);
    }
  };

  return (
    <div className="terminal-row">
      <div className="terminal-meta" title={desc}>
        <span className="terminal-badge">{sideLabel}</span>
      </div>
      <div className="terminal-input-wrap">
        <input
          type="text"
          list={datalistId}
          className="prop-input terminal-input"
          value={label}
          onChange={(e) => setLabel(e.target.value)}
          onBlur={handleCommit}
          onKeyDown={(e) => e.key === 'Enter' && handleCommit()}
          placeholder="Net name (e.g. V_out, GND)"
        />
        {label && (
          <button
            type="button"
            className="terminal-clear-btn"
            onClick={() => {
              setLabel('');
              onCommit('');
            }}
            title="Clear net label"
          >
            ✕
          </button>
        )}
      </div>
    </div>
  );
}

/**
 * Modern programmable Script / Function Block editor with syntax-highlighted
 * code editor, breakpoint gutter, debug toolbar (continue/step), variable
 * watch panel, terminal count adjustment, syntax quick-help, and live
 * validation. The debug parts are only rendered when a scriptDebug session
 * is provided.
 */
function ScriptBlockEditor({
  component,
  onSetParameter,
  debug,
}: {
  component: EditorComponent;
  onSetParameter: (name: string, key: string, value: number | string) => void;
  debug?: ScriptDebugPanelState;
}) {
  const { code, setCode, inCount, setInCount, outCount, setOutCount } =
    useScriptBlockEditorState(component);
  const [syntaxOpen, setSyntaxOpen] = useState(false);
  const [isSaved, setIsSaved] = useState(false);

  const breakpoints = debug?.breakpoints[component.name] ?? [];
  const simActive = debug?.status === 'RUNNING' || debug?.status === 'PAUSED';
  const debugPausedAnywhere = isDebugPausedAnywhere(debug);

  // This block's pause and watch snapshots resolved via the shared helpers
  // (SSE pause first, polled state as fallback)
  const pausedSnapshot: ScriptDebugSnapshot | null = resolveScriptPauseSnapshot(debug, component.name);
  const watchSnapshot: ScriptDebugSnapshot | null = resolveScriptWatchSnapshot(debug, component.name);

  const watchRows = useMemo(
    () => (watchSnapshot ? buildScriptWatchRows(watchSnapshot) : []),
    [watchSnapshot],
  );

  const handleInCountChange = (val: number) => {
    const clamped = clampInputTerminalCount(val);
    setInCount(clamped);
    onSetParameter(component.name, 'anzXIN', clamped);
  };

  const handleOutCountChange = (val: number) => {
    const clamped = clampOutputTerminalCount(val);
    setOutCount(clamped);
    onSetParameter(component.name, 'anzYOUT', clamped);
  };

  const handleApply = () => {
    onSetParameter(component.name, 'sourceCode', code);
    onSetParameter(component.name, 'anzXIN', inCount);
    onSetParameter(component.name, 'anzYOUT', outCount);
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 2000);
  };

  return (
    <div className="prop-section script-block-editor">
      <div className="prop-section-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>Script / Function Logic</span>
        <button
          type="button"
          className="btn-script-syntax-help"
          onClick={() => setSyntaxOpen(!syntaxOpen)}
          style={{
            fontSize: '11px',
            background: 'none',
            border: '1px solid var(--border-subtle, #374151)',
            borderRadius: '4px',
            color: 'var(--accent-primary, #60a5fa)',
            cursor: 'pointer',
            padding: '2px 6px',
          }}
        >
          {syntaxOpen ? 'Hide Help' : 'Cheat Sheet'}
        </button>
      </div>

      {syntaxOpen && (
        <div
          className="script-syntax-cheatsheet"
          style={{
            background: 'var(--surface-sunken, #1e293b)',
            color: 'var(--text-primary, #f8fafc)',
            padding: '10px 12px',
            borderRadius: '6px',
            fontSize: '11px',
            marginBottom: '10px',
            border: '1px solid var(--border-subtle, #334155)',
            lineHeight: 1.6,
          }}
        >
          <div style={{ color: 'var(--text-primary, #f8fafc)' }}><strong style={{ color: 'var(--accent-primary, #38bdf8)' }}>Inputs:</strong> <code style={{ color: '#fbbf24', background: 'rgba(251,191,36,0.1)', padding: '1px 4px', borderRadius: '3px' }}>xIN[0]</code>, <code style={{ color: '#fbbf24', background: 'rgba(251,191,36,0.1)', padding: '1px 4px', borderRadius: '3px' }}>u1</code></div>
          <div style={{ color: 'var(--text-primary, #f8fafc)' }}><strong style={{ color: 'var(--accent-primary, #38bdf8)' }}>Outputs:</strong> <code style={{ color: '#4ade80', background: 'rgba(74,222,128,0.1)', padding: '1px 4px', borderRadius: '3px' }}>yOUT[0]</code>, <code style={{ color: '#4ade80', background: 'rgba(74,222,128,0.1)', padding: '1px 4px', borderRadius: '3px' }}>y1</code></div>
          <div style={{ color: 'var(--text-primary, #f8fafc)' }}><strong style={{ color: 'var(--accent-primary, #38bdf8)' }}>Time:</strong> <code style={{ color: '#a78bfa', background: 'rgba(167,139,250,0.1)', padding: '1px 4px', borderRadius: '3px' }}>t</code> (s), <code style={{ color: '#a78bfa', background: 'rgba(167,139,250,0.1)', padding: '1px 4px', borderRadius: '3px' }}>dt</code> (step)</div>
          <div style={{ color: 'var(--text-primary, #f8fafc)' }}><strong style={{ color: 'var(--accent-primary, #38bdf8)' }}>Math:</strong> <code style={{ color: '#38bdf8' }}>sin, cos, sqrt, abs, pow, min, max, PI</code></div>
          <div style={{ color: 'var(--text-primary, #f8fafc)' }}><strong style={{ color: 'var(--accent-primary, #38bdf8)' }}>Logic:</strong> <code style={{ color: '#38bdf8' }}>if (cond) &#123; ... &#125; else &#123; ... &#125;</code>, <code style={{ color: '#38bdf8' }}>? :</code></div>
          <div style={{ color: '#94a3b8', marginTop: '4px', fontStyle: 'italic' }}>State variables automatically persist across simulation time steps.</div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '10px' }}>
        <div className="prop-field">
          <label className="prop-label">Input Terminals</label>
          <input
            type="number"
            min={0}
            max={16}
            className="prop-input"
            value={inCount}
            onChange={(e) => {
              const raw = parseInt(e.target.value, 10);
              if (!isNaN(raw)) {
                handleInCountChange(raw);
              } else if (e.target.value === '') {
                setInCount(0);
              }
            }}
            onBlur={() => handleInCountChange(inCount)}
          />
        </div>
        <div className="prop-field">
          <label className="prop-label">Output Terminals</label>
          <input
            type="number"
            min={1}
            max={16}
            className="prop-input"
            value={outCount}
            onChange={(e) => {
              const raw = parseInt(e.target.value, 10);
              if (!isNaN(raw)) {
                handleOutCountChange(raw);
              } else if (e.target.value === '') {
                setOutCount(1);
              }
            }}
            onBlur={() => handleOutCountChange(outCount)}
          />
        </div>
      </div>

      <div className="prop-field">
        <label className="prop-label">Formula / Code (executed each dt)</label>
        <ScriptCodeEditor
          value={code}
          onChange={setCode}
          breakpoints={breakpoints}
          onToggleBreakpoint={(line) => debug?.onToggleBreakpoint(component.name, line)}
          pausedLine={pausedSnapshot?.line ?? null}
          onBlur={handleApply}
        />
      </div>

      {debug && (
        <div className="script-debug-bar">
          <span
            className="script-debug-breakpoint-hint"
            title="Click a line number in the gutter to toggle a breakpoint"
          >
            {breakpoints.length > 0
              ? `⏺ ${breakpoints.length} breakpoint${breakpoints.length > 1 ? 's' : ''}`
              : 'Click a line number to add a breakpoint'}
          </span>
          {pausedSnapshot && (
            <span className="script-debug-paused-info">
              Paused at line {pausedSnapshot.line} · t = {formatEngineeringValue(pausedSnapshot.time)} s
            </span>
          )}
          <div className="script-debug-actions">
            <button
              type="button"
              className="script-debug-btn"
              onClick={debug.onDebugResume}
              disabled={!debugPausedAnywhere}
              title="Continue until the next breakpoint"
            >
              ▶ Continue
            </button>
            <button
              type="button"
              className="script-debug-btn"
              onClick={debug.onDebugStep}
              disabled={!debugPausedAnywhere}
              title="Execute one statement, then pause again"
            >
              ↳ Step
            </button>
          </div>
        </div>
      )}

      {debug && simActive && (
        <div className="script-watch-panel">
          <div className="script-watch-title">Watch</div>
          {watchRows.length > 0 ? (
            <div className="script-watch-table">
              {watchRows.map((row) => (
                <div key={row.name} className="script-watch-row" title={row.hint}>
                  <span className="script-watch-name">{row.name}</span>
                  <span className="script-watch-value">{formatEngineeringValue(row.value)}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="script-watch-empty">Waiting for simulation state…</div>
          )}
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '6px' }}>
        <button
          type="button"
          onClick={handleApply}
          className="action-btn"
          style={{
            fontSize: '12px',
            padding: '4px 12px',
            backgroundColor: isSaved ? '#16a34a' : 'var(--accent-primary, #3b82f6)',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 500,
            transition: 'background-color 0.2s',
          }}
        >
          {isSaved ? '✓ Applied' : 'Apply Script'}
        </button>
      </div>
    </div>
  );
}
