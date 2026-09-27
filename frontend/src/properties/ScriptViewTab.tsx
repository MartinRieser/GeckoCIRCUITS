/**
 * Full-Viewport Dedicated Script / Function Block IDE Tab (ScriptViewTab).
 * Provides a full-height code editor with syntax highlighting, line gutter breakpoints,
 * terminal pin management, live simulation variable watch table, and debug stepping.
 *
 * Editor state handling, watch rows and debug snapshot resolution are shared
 * with the sidebar's compact script editor via scriptBlockShared.ts.
 */
import React, { useState, useMemo } from 'react';
import type { EditorComponent, EditorWire } from '../model/types';
import type { ScriptDebugPanelState } from './PropertiesPanel';
import { ScriptCodeEditor } from './ScriptCodeEditor';
import { formatEngineeringValue } from '../model/componentSchema';
import { SCRIPT_TERMINAL_LIMIT } from '../model/constants';
import {
  buildScriptWatchRows,
  clampInputTerminalCount,
  clampOutputTerminalCount,
  isDebugPausedAnywhere,
  resolveScriptPauseSnapshot,
  resolveScriptWatchSnapshot,
  useScriptBlockEditorState,
} from './scriptBlockShared';

export interface ScriptViewTabProps {
  /** The script component being edited. */
  component: EditorComponent | null;
  /** Complete list of circuit components. */
  allComponents?: EditorComponent[];
  /** Schematic wires. */
  wires?: EditorWire[];
  /** Script debugging session state (breakpoints, pause snapshot, watch). */
  scriptDebug?: ScriptDebugPanelState;
  /** Callback to update component parameter. */
  onSetParameter: (name: string, key: string, value: number | string) => void;
  /** Callback to update terminal net label. */
  onSetLabel?: (
    component: string,
    side: 'x' | 'y',
    indexOrLabel: number | string,
    maybeLabel?: string,
  ) => void;
  /** UI theme. */
  theme?: 'dark' | 'light';
}

export function ScriptViewTab({
  component,
  scriptDebug: debug,
  onSetParameter,
  onSetLabel,
}: ScriptViewTabProps) {
  const [isSaved, setIsSaved] = useState(false);
  const [syntaxOpen, setSyntaxOpen] = useState(false);

  // Shared local mirror of the persisted script parameters; null component
  // renders the empty state below instead of running the hook.
  const editor = useScriptBlockEditorState(
    component ?? {
      type: 0,
      family: '',
      name: '',
      position: [0, 0],
      orientation: 502,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
  );
  const { code, setCode, inCount, setInCount, outCount, setOutCount } = editor;

  const breakpoints = useMemo(() => {
    if (!component || !debug) return [];
    return debug.breakpoints[component.name] ?? [];
  }, [component?.name, debug?.breakpoints]);

  const pausedSnapshot = resolveScriptPauseSnapshot(debug, component?.name);
  const debugPausedAnywhere = isDebugPausedAnywhere(debug);
  const simActive = debug?.status === 'RUNNING' || debug?.status === 'PAUSED';
  const watchSnapshot = resolveScriptWatchSnapshot(debug, component?.name);

  const watchRows = useMemo(
    () => (watchSnapshot ? buildScriptWatchRows(watchSnapshot) : []),
    [watchSnapshot],
  );

  if (!component) {
    return (
      <div className="scope-empty-state" style={{ height: '100%' }}>
        <div className="empty-icon">💻</div>
        <h3>No Script Component Selected</h3>
        <p>Select a Function Block or Script component in the schematic editor.</p>
      </div>
    );
  }

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

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault();
      handleApply();
    }
  };

  return (
    <div className="script-view-tab-container" onKeyDown={handleKeyDown}>
      {/* Top Header Bar */}
      <div className="scope-tab-header script-tab-header">
        <div className="scope-tab-title-group">
          <div className="scope-badge-icon" style={{ fontSize: 18 }}>💻</div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span className="scope-tab-title" style={{ fontSize: 15, fontWeight: 600 }}>
                {component.name}
              </span>
              <span className="workspace-tab-badge" style={{ fontSize: 11 }}>
                Function Block (Script)
              </span>
            </div>
            <div className="scope-tab-subtitle">
              Executed at each discrete simulation step (dt)
            </div>
          </div>
        </div>

        <div className="script-tab-header-actions">
          <button
            type="button"
            className="script-ide-btn"
            onClick={() => setSyntaxOpen(!syntaxOpen)}
          >
            {syntaxOpen ? 'Hide Cheat Sheet' : 'ℹ Cheat Sheet'}
          </button>

          <button
            type="button"
            className={`script-ide-btn primary ${isSaved ? 'saved' : ''}`}
            onClick={handleApply}
          >
            {isSaved ? '✓ Applied' : 'Apply Script (Ctrl+S)'}
          </button>
        </div>
      </div>

      {/* Main Workspace Pane: Split between Editor and Config/Watch Inspector */}
      <div className="script-tab-workspace">
        {/* Center: Full-height Script Code Editor */}
        <div className="script-tab-editor-pane">
          <div className="script-tab-editor-scroll">
            <ScriptCodeEditor
              value={code}
              onChange={setCode}
              breakpoints={breakpoints}
              onToggleBreakpoint={(line) => debug?.onToggleBreakpoint(component.name, line)}
              pausedLine={pausedSnapshot?.line ?? null}
              height="100%"
              onBlur={handleApply}
            />
          </div>

          {/* Bottom Debugger Control Bar */}
          <div className="script-debug-bar script-tab-debug-bar">
            <div className="script-tab-debug-info">
              <span className="script-debug-breakpoint-hint">
                {breakpoints.length > 0
                  ? `⏺ ${breakpoints.length} breakpoint${breakpoints.length > 1 ? 's' : ''}`
                  : 'Click line numbers in gutter to toggle breakpoints'}
              </span>

              {pausedSnapshot && (
                <span className="script-debug-paused-info script-tab-paused-pill">
                  Paused at line {pausedSnapshot.line} · t ={' '}
                  {formatEngineeringValue(pausedSnapshot.time, 's')}
                </span>
              )}
            </div>

            <div className="script-debug-actions">
              <button
                type="button"
                className="script-debug-btn"
                onClick={debug?.onDebugResume}
                disabled={!debugPausedAnywhere}
              >
                ▶ Continue
              </button>
              <button
                type="button"
                className="script-debug-btn"
                onClick={debug?.onDebugStep}
                disabled={!debugPausedAnywhere}
              >
                ↳ Step Over
              </button>
            </div>
          </div>
        </div>

        {/* Right Panel: Terminal Pins, Cheat Sheet, and Live Watch */}
        <div className="script-tab-side-panel">
          {/* Syntax Cheat Sheet */}
          {syntaxOpen && (
            <div className="script-syntax-cheatsheet">
              <div className="script-cheatsheet-title">
                Syntax Reference:
              </div>
              <div><strong>Inputs:</strong> <code>xIN[0]</code>, <code>u1</code></div>
              <div><strong>Outputs:</strong> <code>yOUT[0]</code>, <code>y1</code></div>
              <div><strong>Timing:</strong> <code>t</code> (time), <code>dt</code> (step)</div>
              <div><strong>Math:</strong> <code>sin, cos, sqrt, abs, pow, min, max, PI</code></div>
              <div><strong>Logic:</strong> <code>if (cond) &#123; ... &#125; else &#123; ... &#125;</code></div>
              <div className="script-cheatsheet-note">
                Variables persist across simulation time steps.
              </div>
            </div>
          )}

          {/* Terminal Pins Section */}
          <div className="sim-config-section">
            <div className="script-side-panel-title">
              🔌 Terminal Pins
            </div>
            <div className="script-terminal-grid">
              <div className="prop-field">
                <label className="prop-label" htmlFor="script-in-terminals">
                  Input Terminals
                </label>
                <input
                  id="script-in-terminals"
                  type="number"
                  min={0}
                  max={SCRIPT_TERMINAL_LIMIT}
                  className="prop-input"
                  value={inCount}
                  onChange={(e) => {
                    const raw = parseInt(e.target.value, 10);
                    if (!isNaN(raw)) handleInCountChange(raw);
                    else if (e.target.value === '') setInCount(0);
                  }}
                  onBlur={() => handleInCountChange(inCount)}
                />
              </div>

              <div className="prop-field">
                <label className="prop-label" htmlFor="script-out-terminals">
                  Output Terminals
                </label>
                <input
                  id="script-out-terminals"
                  type="number"
                  min={1}
                  max={SCRIPT_TERMINAL_LIMIT}
                  className="prop-input"
                  value={outCount}
                  onChange={(e) => {
                    const raw = parseInt(e.target.value, 10);
                    if (!isNaN(raw)) handleOutCountChange(raw);
                    else if (e.target.value === '') setOutCount(1);
                  }}
                  onBlur={() => handleOutCountChange(outCount)}
                />
              </div>
            </div>
          </div>

          {/* Terminal Net Labels */}
          <div className="sim-config-section">
            <div className="script-side-panel-title">
              🏷 Terminal Net Labels
            </div>
            <div className="script-net-label-list">
              {Array.from({ length: inCount }, (_, idx) => (
                <div key={`in-${idx}`} className="script-net-label-row">
                  <span className="workspace-tab-badge script-net-label-badge">
                    xIN[{idx}]
                  </span>
                  <input
                    type="text"
                    className="prop-input"
                    placeholder={`Net label for input ${idx + 1}`}
                    value={component.inputLabels?.[idx] || ''}
                    onChange={(e) => onSetLabel?.(component.name, 'x', idx, e.target.value)}
                  />
                </div>
              ))}
              {Array.from({ length: outCount }, (_, idx) => (
                <div key={`out-${idx}`} className="script-net-label-row">
                  <span className="workspace-tab-badge script-net-label-badge">
                    yOUT[{idx}]
                  </span>
                  <input
                    type="text"
                    className="prop-input"
                    placeholder={`Net label for output ${idx + 1}`}
                    value={component.outputLabels?.[idx] || ''}
                    onChange={(e) => onSetLabel?.(component.name, 'y', idx, e.target.value)}
                  />
                </div>
              ))}
            </div>
          </div>

          {/* Live Variable Watch Table */}
          <div className="sim-config-section script-tab-watch-section">
            <div className="script-watch-header">
              <span className="script-side-panel-title">
                🔬 Live Variable Watch
              </span>
              {simActive && (
                <span className="sim-status-badge running" style={{ fontSize: 10 }}>
                  Active
                </span>
              )}
            </div>

            {simActive && watchRows.length > 0 ? (
              <div className="script-watch-table script-tab-watch-table">
                {watchRows.map((row) => (
                  <div key={row.name} className="script-watch-row" title={row.hint}>
                    <span className="script-watch-name">{row.name}</span>
                    <span className="script-watch-value">{formatEngineeringValue(row.value)}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="script-watch-empty script-tab-watch-empty">
                Run simulation to inspect variable values in real-time.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
