/**
 * Structured Properties Inspector: component header with live symbol preview,
 * semantic engineering parameters with SI prefix notation (10k, 4.7u, 100n, 1M),
 * unit badges, terminal net label manager, and quick actions (Rotate, Delete, Duplicate).
 */
import { useEffect, useState, useMemo } from 'react';
import type { EditorComponent } from '../model/types';
import {
  getComponentMeta,
  parseEngineeringValue,
  formatEngineeringValue,
} from '../model/componentSchema';
import type { ParameterDef } from '../model/componentSchema';
import { SymbolPreview } from '../canvas/symbols';

interface PanelProps {
  component: EditorComponent | null;
  onRename: (name: string, newName: string) => void;
  onSetParameter: (name: string, key: string, value: number | string) => void;
  onSetLabel: (component: string, side: 'x' | 'y', label: string) => void;
  onRotate?: (name: string) => void;
  onDelete?: (name: string) => void;
  onOpenScopeTab?: (scopeName: string) => void;
  onCollapse?: () => void;
}

export function PropertiesPanel({
  component,
  onRename,
  onSetParameter,
  onSetLabel,
  onRotate,
  onDelete,
  onOpenScopeTab,
  onCollapse,
}: PanelProps) {
  const [name, setName] = useState('');
  const [showRaw, setShowRaw] = useState(false);

  useEffect(() => {
    setName(component?.name ?? '');
  }, [component?.name]);

  const meta = useMemo(() => {
    if (!component) return null;
    return getComponentMeta(component.type, component.family, component.name);
  }, [component]);

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
        {onOpenScopeTab && (component.type === 5 || component.type === 1003 || component.name.toUpperCase().startsWith('SCOPE')) && (
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

        {/* Dedicated Script / Function Block Editor */}
        {(component.type === 1016 || component.type === 61 || meta.name === 'CTRL_SCRIPT') ? (
          <ScriptBlockEditor component={component} onSetParameter={onSetParameter} />
        ) : (
          meta.parameters.length > 0 && (
            <div className="prop-section">
              <div className="prop-section-title">Electrical Parameters</div>
              <div className="prop-fields-list">
                {meta.parameters.map((def) => {
                  const currentVal =
                    (component.parameters[def.key] as number) ?? def.defaultValue;
                  return (
                    <SemanticParameterField
                      key={def.key}
                      def={def}
                      value={currentVal}
                      onCommit={(val) => onSetParameter(component.name, def.key, val)}
                    />
                  );
                })}
              </div>
            </div>
          )
        )}

        {/* Terminals & Net Labels */}
        <div className="prop-section">
          <div className="prop-section-title">Terminal Net Labels</div>
          <div className="terminal-labels-list">
            <TerminalLabelRow
              sideLabel={meta.terminals.input[0]?.label || 'Input / T1'}
              desc={meta.terminals.input[0]?.description || 'Terminal 1'}
              currentLabel={component.inputLabels[0] || ''}
              onCommit={(lbl) => onSetLabel(component.name, 'x', lbl)}
            />
            <TerminalLabelRow
              sideLabel={meta.terminals.output[0]?.label || 'Output / T2'}
              desc={meta.terminals.output[0]?.description || 'Terminal 2'}
              currentLabel={component.outputLabels[0] || ''}
              onCommit={(lbl) => onSetLabel(component.name, 'y', lbl)}
            />
          </div>
        </div>

        {/* Collapsible Advanced / Raw Parameters */}
        <div className="prop-section advanced">
          <button
            type="button"
            className="advanced-toggle-btn"
            onClick={() => setShowRaw(!showRaw)}
          >
            <span>{showRaw ? '▼' : '►'} Advanced / Raw Parameters</span>
          </button>
          {showRaw && (
            <div className="raw-params-table">
              {Object.entries(component.parameters)
                .filter(([k]) => k.startsWith('param') && typeof component.parameters[k] === 'number')
                .sort(([a], [b]) => a.localeCompare(b, undefined, { numeric: true }))
                .map(([key, val]) => (
                  <div key={key} className="raw-param-row">
                    <span className="raw-param-key">{key}</span>
                    <span className="raw-param-val">{String(val)}</span>
                  </div>
                ))}
            </div>
          )}
        </div>
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

  useEffect(() => {
    if (!isEditing) {
      setText(formatEngineeringValue(value));
    }
  }, [value, isEditing]);

  const handleCommit = () => {
    setIsEditing(false);
    const parsed = parseEngineeringValue(text);
    if (parsed !== null && Number.isFinite(parsed) && parsed !== value) {
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
        <select
          className="prop-select"
          value={value}
          onChange={(e) => onCommit(Number(e.target.value))}
        >
          {def.options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      ) : (
        <div className="prop-input-wrap">
          <input
            type="text"
            className="prop-input"
            value={text}
            onChange={(e) => {
              setIsEditing(true);
              setText(e.target.value);
            }}
            onBlur={handleCommit}
            onKeyDown={(e) => e.key === 'Enter' && handleCommit()}
            placeholder={`e.g. ${def.defaultValue}`}
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
  onCommit,
}: {
  sideLabel: string;
  desc: string;
  currentLabel: string;
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
 * Modern programmable Script / Function Block editor with code editor,
 * terminal count adjustment, syntax quick-help, and live validation.
 */
function ScriptBlockEditor({
  component,
  onSetParameter,
}: {
  component: EditorComponent;
  onSetParameter: (name: string, key: string, value: number | string) => void;
}) {
  const currentCode = String(component.parameters['sourceCode'] || 'yOUT[0] = xIN[0];');
  const [code, setCode] = useState(currentCode);
  const [inCount, setInCount] = useState(Number(component.parameters['anzXIN'] || 1));
  const [outCount, setOutCount] = useState(Number(component.parameters['anzYOUT'] || 1));
  const [syntaxOpen, setSyntaxOpen] = useState(false);
  const [isSaved, setIsSaved] = useState(false);

  useEffect(() => {
    setCode(String(component.parameters['sourceCode'] || 'yOUT[0] = xIN[0];'));
    const pIn = Number(component.parameters['anzXIN']);
    if (!isNaN(pIn)) setInCount(pIn);
    const pOut = Number(component.parameters['anzYOUT']);
    if (!isNaN(pOut)) setOutCount(pOut);
  }, [
    component.name,
    component.parameters['sourceCode'],
    component.parameters['anzXIN'],
    component.parameters['anzYOUT'],
  ]);

  const handleInCountChange = (val: number) => {
    const clamped = Math.max(0, Math.min(16, val));
    setInCount(clamped);
    onSetParameter(component.name, 'anzXIN', clamped);
  };

  const handleOutCountChange = (val: number) => {
    const clamped = Math.max(1, Math.min(16, val));
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
        <textarea
          className="script-code-editor-area"
          rows={7}
          value={code}
          onChange={(e) => setCode(e.target.value)}
          onBlur={handleApply}
          placeholder="yOUT[0] = xIN[0] * 2;"
          style={{
            width: '100%',
            fontFamily: 'Consolas, "Fira Code", monospace',
            fontSize: '12px',
            backgroundColor: 'var(--surface-sunken, #0f172a)',
            color: 'var(--text-primary, #f8fafc)',
            border: '1px solid var(--border-subtle, #334155)',
            borderRadius: '6px',
            padding: '8px',
            boxSizing: 'border-box',
            resize: 'vertical',
            lineHeight: 1.4,
          }}
        />
      </div>

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
