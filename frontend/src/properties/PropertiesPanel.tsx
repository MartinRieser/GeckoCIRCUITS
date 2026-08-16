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
  onSetParameter: (name: string, key: string, value: number) => void;
  onSetLabel: (component: string, side: 'x' | 'y', label: string) => void;
  onRotate?: (name: string) => void;
  onDelete?: (name: string) => void;
}

export function PropertiesPanel({
  component,
  onRename,
  onSetParameter,
  onSetLabel,
  onRotate,
  onDelete,
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
        </div>
        <div className="properties-empty-state">
          <div className="empty-icon">⚡</div>
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
              🗑
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

        {/* Semantic Parameters Section */}
        {meta.parameters.length > 0 && (
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
