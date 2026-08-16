/**
 * Application Shell: top navigation bar with file menus & built-in examples,
 * prominent simulation action, command palette (Ctrl+K), component palette,
 * interactive schematic canvas, structured properties inspector, and simulation
 * waveform drawer.
 */
import { useEffect, useRef, useState } from 'react';
import { useEditor } from './hooks/useEditor';
import { Sheet } from './canvas/Sheet';
import { Palette } from './palette/Palette';
import { PropertiesPanel } from './properties/PropertiesPanel';
import { SimulationDrawer } from './simulation/SimulationDrawer';
import { CommandPalette } from './palette/CommandPalette';
import { EXAMPLES } from './model/examples';

export function App() {
  const { state, dispatch, catalog, wsConnected, simState, actions } = useEditor();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);
  const [examplesMenuOpen, setExamplesMenuOpen] = useState(false);
  const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
  const [rightSidebarOpen, setRightSidebarOpen] = useState(true);

  // Global Keyboard Shortcuts
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement ||
        e.target instanceof HTMLSelectElement
      ) {
        return;
      }

      // Command Palette: Ctrl+K or /
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        setCommandPaletteOpen((prev) => !prev);
        return;
      }
      if (e.key === '/' && !commandPaletteOpen) {
        e.preventDefault();
        setCommandPaletteOpen(true);
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
          } else if (state.selection.length === 1) {
            actions.rotateComponent(state.selection[0]);
          }
          break;
        case 'Escape':
          actions.cancel();
          setExamplesMenuOpen(false);
          setCommandPaletteOpen(false);
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
  }, [actions, commandPaletteOpen, dispatch, state.mode, state.selection, state.selectedWire]);

  const handleSelectExample = (exampleId: string) => {
    const ex = EXAMPLES.find((e) => e.id === exampleId);
    if (ex) {
      actions.openContent(ex.content, `${ex.name}.ipes`);
    }
    setExamplesMenuOpen(false);
  };

  return (
    <div className="app">
      {/* Top Main Navigation Bar */}
      <header className="navbar">
        <div className="nav-brand">
          <div className="nav-logo">
            <span className="logo-symbol">G</span>
          </div>
          <div className="nav-title-group">
            <span className="nav-title">GeckoCIRCUITS</span>
            <span className="nav-subtitle">Web EDA & Simulation</span>
          </div>
        </div>

        {/* File & Edit Actions */}
        <div className="nav-actions">
          <button
            type="button"
            className="nav-btn"
            onClick={() => actions.newCircuit()}
            title="Create blank circuit"
          >
            New
          </button>

          <button
            type="button"
            className="nav-btn"
            onClick={() => fileInputRef.current?.click()}
            disabled={state.busy}
            title="Open local .ipes file"
          >
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

          <button
            type="button"
            className="nav-btn"
            onClick={actions.save}
            disabled={!state.circuitId}
            title="Download circuit as .ipes file (Ctrl+S)"
          >
            Save .ipes
          </button>

          {/* Examples Dropdown */}
          <div className="dropdown-wrap">
            <button
              type="button"
              className={`nav-btn dropdown-toggle ${examplesMenuOpen ? 'active' : ''}`}
              onClick={() => setExamplesMenuOpen(!examplesMenuOpen)}
            >
              Examples ▾
            </button>
            {examplesMenuOpen && (
              <div className="dropdown-menu">
                <div className="dropdown-header">Built-in Circuits</div>
                {EXAMPLES.map((ex) => (
                  <button
                    key={ex.id}
                    type="button"
                    className="dropdown-item"
                    onClick={() => handleSelectExample(ex.id)}
                  >
                    <div className="dropdown-item-title">{ex.name}</div>
                    <div className="dropdown-item-desc">{ex.description}</div>
                  </button>
                ))}
              </div>
            )}
          </div>

          <span className="nav-separator" />

          <button
            type="button"
            className="nav-btn icon-only"
            onClick={actions.undo}
            disabled={!state.circuitId}
            title="Undo (Ctrl+Z)"
          >
            ↩
          </button>
          <button
            type="button"
            className="nav-btn icon-only"
            onClick={actions.redo}
            disabled={!state.circuitId}
            title="Redo (Ctrl+Y)"
          >
            ↪
          </button>

          <span className="nav-separator" />

          <button
            type="button"
            className={`nav-btn ${state.mode === 'wiring' ? 'active' : ''}`}
            onClick={actions.toggleWireMode}
            disabled={!state.circuitId}
            title="Toggle Wire Tool (W)"
          >
            Wire
          </button>

          <button
            type="button"
            className="nav-btn search-btn"
            onClick={() => setCommandPaletteOpen(true)}
            title="Command Palette (Ctrl+K or /)"
          >
            Search parts <kbd>Ctrl+K</kbd>
          </button>
        </div>

        {/* Right side: Simulation Action & Toggle */}
        <div className="nav-right">
          <button
            type="button"
            className="sim-run-cta-btn"
            onClick={() => {
              actions.toggleSimDrawer();
              if (!simState.isOpen) {
                actions.runSimulation({
                  simulationTime: 0.02,
                  timeStep: 1e-6,
                  solverType: 'backward-euler',
                });
              }
            }}
            disabled={!state.circuitId}
            title="Run circuit simulation and view waveforms"
          >
            {simState.status === 'RUNNING' ? (
              <span className="spinner" />
            ) : null}
            <span>{simState.status === 'RUNNING' ? 'Simulating...' : 'Run Simulation'}</span>
          </button>
        </div>
      </header>

      {/* Main 3-Column Studio Layout */}
      <main className="studio-main">
        {/* Left: Palette */}
        {leftSidebarOpen ? (
          <aside className="sidebar left-sidebar">
            <Palette catalog={catalog} onArm={actions.arm} />
          </aside>
        ) : (
          <button
            type="button"
            className="sidebar-expand-btn left"
            onClick={() => setLeftSidebarOpen(true)}
            title="Show Component Palette"
          >
            ▶
          </button>
        )}

        {/* Center: Sheet Canvas */}
        <section className="canvas-area">
          <Sheet
            state={state}
            dispatch={dispatch}
            actions={{
              placeGhost: actions.placeGhost,
              finishWire: actions.finishWire,
              labelWire: actions.labelWire,
              commitMove: actions.commitMove,
              rotateComponent: actions.rotateComponent,
              deleteComponent: actions.deleteComponent,
              deleteWire: actions.deleteWire,
              openProperties: actions.openProperties,
              toggleWireMode: actions.toggleWireMode,
              openCommandPalette: () => setCommandPaletteOpen(true),
            }}
          />
        </section>

        {/* Right: Properties Inspector */}
        {rightSidebarOpen ? (
          <aside className="sidebar right-sidebar">
            <PropertiesPanel
              component={state.components.find((c) => c.name === state.panelFor) ?? null}
              onRename={actions.rename}
              onSetParameter={actions.setParameter}
              onSetLabel={actions.setLabel}
              onRotate={actions.rotateComponent}
              onDelete={actions.deleteComponent}
            />
          </aside>
        ) : (
          <button
            type="button"
            className="sidebar-expand-btn right"
            onClick={() => setRightSidebarOpen(true)}
            title="Show Properties Panel"
          >
            ◀
          </button>
        )}
      </main>

      {/* Bottom Collapsible Simulation Waveforms Drawer */}
      <SimulationDrawer
        isOpen={simState.isOpen}
        onToggle={actions.toggleSimDrawer}
        circuitId={state.circuitId}
        onRunSimulation={actions.runSimulation}
        onCancelSimulation={actions.cancelSimulation}
        status={simState.status}
        progress={simState.progress}
        results={simState.results}
        errorMessage={simState.errorMessage}
      />

      {/* Bottom Status Bar */}
      <footer className="statusbar">
        <div className="status-item file-name">
          <span className="status-label">File:</span>
          <span className="status-value">{state.filename || 'Untitled.ipes'}</span>
        </div>
        <div className="status-item">
          <span className="status-label">Version:</span>
          <span className="status-value">v{state.modelVersion}</span>
        </div>
        <div className="status-item status-msg">
          <span>{state.status || 'Ready'}</span>
        </div>
        <div className="status-shortcuts">
          <span><kbd>W</kbd> Wire</span>
          <span><kbd>R</kbd> Rotate</span>
          <span><kbd>Del</kbd> Delete</span>
          <span><kbd>Space</kbd>+Drag Pan</span>
          <span><kbd>Ctrl+K</kbd> Search</span>
        </div>
        <div className="status-item ws-status">
          <span className={`ws-dot ${wsConnected ? 'on' : 'off'}`} />
          <span>{wsConnected ? 'Backend Live' : 'Offline'}</span>
        </div>
      </footer>

      {/* Command Palette Modal */}
      <CommandPalette
        isOpen={commandPaletteOpen}
        onClose={() => setCommandPaletteOpen(false)}
        catalog={catalog}
        onSelect={actions.arm}
      />
    </div>
  );
}
