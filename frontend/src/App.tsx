/**
 * Application shell: toolbar, palette, sheet, properties panel and
 * status bar. Keyboard shortcuts: W wire mode, R rotate ghost,
 * Esc cancel, Delete delete selection, Ctrl+Z/Y undo/redo, Ctrl+S save.
 */
import { useEffect, useRef } from 'react';
import { useEditor } from './hooks/useEditor';
import { Sheet } from './canvas/Sheet';
import { Palette } from './palette/Palette';
import { PropertiesPanel } from './properties/PropertiesPanel';

export function App() {
  const { state, dispatch, catalog, wsConnected, actions } = useEditor();
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return;
      }
      if (e.ctrlKey || e.metaKey) {
        if (e.key === 'z') {
          e.preventDefault();
          actions.undo();
        } else if (e.key === 'y') {
          e.preventDefault();
          actions.redo();
        } else if (e.key === 's') {
          e.preventDefault();
          actions.save();
        }
        return;
      }
      switch (e.key) {
        case 'w':
        case 'W':
          actions.toggleWireMode();
          break;
        case 'r':
        case 'R':
          if (state.mode === 'placing') {
            dispatch({ type: 'GHOST_ROTATE' });
          }
          break;
        case 'Escape':
          actions.cancel();
          break;
        case 'Delete':
        case 'Backspace':
          if (state.selection.length || state.selectedWire !== null) {
            e.preventDefault();
            actions.deleteSelection();
          }
          break;
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [actions, dispatch, state.mode, state.selection, state.selectedWire]);

  return (
    <div className="app">
      <div className="toolbar">
        <button onClick={() => fileInputRef.current?.click()} disabled={state.busy}>
          Open...
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept=".ipes,.txt"
          hidden
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) {
              actions.open(file);
            }
            e.target.value = '';
          }}
        />
        <button onClick={actions.save} disabled={!state.circuitId}>
          Save
        </button>
        <span className="toolbar-separator" />
        <button onClick={actions.undo} disabled={!state.circuitId}>
          Undo
        </button>
        <button onClick={actions.redo} disabled={!state.circuitId}>
          Redo
        </button>
        <span className="toolbar-separator" />
        <button
          className={state.mode === 'wiring' ? 'active' : ''}
          onClick={actions.toggleWireMode}
          disabled={!state.circuitId}
          title="Toggle wire mode (W)"
        >
          Wire
        </button>
        <button
          onClick={actions.deleteSelection}
          disabled={!state.selection.length && state.selectedWire === null}
        >
          Delete
        </button>
      </div>

      <div className="main">
        <div className="sidebar">
          <Palette catalog={catalog} onArm={actions.arm} />
        </div>
        <Sheet state={state} dispatch={dispatch} actions={actions} />
        <div className="sidebar">
          <PropertiesPanel
            component={state.components.find((c) => c.name === state.panelFor) ?? null}
            onRename={actions.rename}
            onSetParameter={actions.setParameter}
            onSetLabel={actions.setLabel}
          />
        </div>
      </div>

      <div className="statusbar">
        <span>{state.filename || 'no file'}</span>
        <span>v{state.modelVersion}</span>
        <span>{state.status}</span>
        <span className={wsConnected ? 'ws on' : 'ws off'}>
          {wsConnected ? 'live' : 'offline'}
        </span>
      </div>
    </div>
  );
}
