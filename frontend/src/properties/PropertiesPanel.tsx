/**
 * Properties side panel for the selected component: rename, numeric
 * parameters and terminal node labels (the label-based connectivity).
 */
import { useEffect, useState } from 'react';
import type { EditorComponent } from '../model/types';

interface PanelProps {
  component: EditorComponent | null;
  onRename: (name: string, newName: string) => void;
  onSetParameter: (name: string, key: string, value: number) => void;
  onSetLabel: (component: string, side: 'x' | 'y', label: string) => void;
}

export function PropertiesPanel({ component, onRename, onSetParameter, onSetLabel }: PanelProps) {
  const [name, setName] = useState('');
  const [inputLabel, setInputLabel] = useState('');
  const [outputLabel, setOutputLabel] = useState('');

  useEffect(() => {
    setName(component?.name ?? '');
    setInputLabel(component?.inputLabels[0] ?? '');
    setOutputLabel(component?.outputLabels[0] ?? '');
  }, [component]);

  if (!component) {
    return (
      <div className="properties">
        <div className="properties-header">Properties</div>
        <div className="properties-empty">Select a component (double-click)</div>
      </div>
    );
  }

  const numericParams = Object.entries(component.parameters)
    .filter(([key, value]) => key.startsWith('param') && typeof value === 'number')
    .sort(([a], [b]) => a.localeCompare(b, undefined, { numeric: true }));

  return (
    <div className="properties">
      <div className="properties-header">Properties — {component.name}</div>
      <label>
        Name
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          onBlur={() => onRename(component.name, name.trim())}
          onKeyDown={(e) => e.key === 'Enter' && onRename(component.name, name.trim())}
        />
      </label>
      <div className="properties-static">
        <span>
          Type {component.type} · {component.family} · orientation {component.orientation}
        </span>
        <span>
          Position ({component.position[0]}, {component.position[1]})
        </span>
      </div>
      <div className="properties-section">Parameters</div>
      {numericParams.map(([key, value]) => (
        <ParameterRow
          key={key}
          paramKey={key}
          value={value as number}
          onCommit={(v) => onSetParameter(component.name, key, v)}
        />
      ))}
      <div className="properties-section">Node labels</div>
      <label>
        Input
        <input
          value={inputLabel}
          onChange={(e) => setInputLabel(e.target.value)}
          onBlur={() => onSetLabel(component.name, 'x', inputLabel.trim())}
          onKeyDown={(e) => e.key === 'Enter' && onSetLabel(component.name, 'x', inputLabel.trim())}
          placeholder="label"
        />
      </label>
      <label>
        Output
        <input
          value={outputLabel}
          onChange={(e) => setOutputLabel(e.target.value)}
          onBlur={() => onSetLabel(component.name, 'y', outputLabel.trim())}
          onKeyDown={(e) => e.key === 'Enter' && onSetLabel(component.name, 'y', outputLabel.trim())}
          placeholder="label"
        />
      </label>
    </div>
  );
}

function ParameterRow({
  paramKey,
  value,
  onCommit,
}: {
  paramKey: string;
  value: number;
  onCommit: (value: number) => void;
}) {
  const [text, setText] = useState(String(value));
  useEffect(() => setText(String(value)), [value]);

  const commit = () => {
    const parsed = Number(text);
    if (Number.isFinite(parsed) && parsed !== value) {
      onCommit(parsed);
    } else {
      setText(String(value));
    }
  };

  return (
    <label>
      {paramKey}
      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        onBlur={commit}
        onKeyDown={(e) => e.key === 'Enter' && commit()}
      />
    </label>
  );
}
