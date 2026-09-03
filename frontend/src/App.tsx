/**
 * Application Shell: top navigation bar with file menus & built-in examples,
 * prominent simulation action, command palette (Ctrl+K), component palette,
 * interactive schematic canvas, structured properties inspector, and simulation
 * waveform drawer.
 *
 * Central keyboard interaction layer (P3) powered by keybindings.ts.
 */
import { useEffect, useRef, useState } from 'react';
import { useEditor } from './hooks/useEditor';
import { Sheet } from './canvas/Sheet';
import { Palette } from './palette/Palette';
import { PropertiesPanel } from './properties/PropertiesPanel';
import { SimulationPropertiesPanel } from './properties/SimulationPropertiesPanel';
import { ScopeViewTab } from './simulation/ScopeViewTab';
import { CommandPalette } from './palette/CommandPalette';
import { EXAMPLES } from './model/examples';
import { resolveShortcut, KEYBINDINGS } from './model/keybindings';
import { routeL, densePoints } from './canvas/WireRouter';

export function App() {
  const { state, dispatch, catalog, wsConnected, simState, actions } = useEditor();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);
  const [examplesMenuOpen, setExamplesMenuOpen] = useState(false);
  const [shortcutsHelpOpen, setShortcutsHelpOpen] = useState(false);
  const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
  const [rightSidebarOpen, setRightSidebarOpen] = useState(true);
  const [activeWorkspaceTab, setActiveWorkspaceTab] = useState<'schematic' | 'simulation'>('schematic');
  const [selectedScope, setSelectedScope] = useState<string>('all');
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    return (localStorage.getItem('gecko-theme') as 'dark' | 'light') || 'dark';
  });

  const openScopeTab = (scopeName: string) => {
    setSelectedScope(scopeName);
    setActiveWorkspaceTab('simulation');
  };

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('gecko-theme', theme);
  }, [theme]);

  // Global Keyboard Shortcuts (Central Dispatcher)
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement ||
        e.target instanceof HTMLSelectElement
      ) {
        return;
      }

      const hasSelection = state.selection.length > 0;
      const hasDraft = !!state.wireDraft;
      const action = resolveShortcut(e, state.mode, hasSelection, hasDraft);
      if (!action) return;

      const step = e.shiftKey ? 5 : 1;

      switch (action) {
        case 'command-palette':
          e.preventDefault();
          setCommandPaletteOpen((prev) => !prev);
          break;
        case 'toggle-inspector':
          e.preventDefault();
          setRightSidebarOpen((prev) => !prev);
          break;
        case 'toggle-palette':
          e.preventDefault();
          setLeftSidebarOpen((prev) => !prev);
          break;
        case 'toggle-simulation':
          e.preventDefault();
          setActiveWorkspaceTab((prev) => (prev === 'schematic' ? 'simulation' : 'schematic'));
          break;
        case 'save':
          e.preventDefault();
          actions.save();
          break;
        case 'show-shortcuts-help':
          e.preventDefault();
          setShortcutsHelpOpen((prev) => !prev);
          break;
        case 'undo':
          e.preventDefault();
          actions.undo();
          break;
        case 'redo':
          e.preventDefault();
          actions.redo();
          break;
        case 'delete':
          if (state.selection.length || state.selectedWire !== null) {
            e.preventDefault();
            actions.deleteSelection();
          }
          break;
        case 'duplicate':
          e.preventDefault();
          actions.duplicateSelection();
          break;
        case 'rotate-selection':
          if (state.selection.length === 1) {
            actions.rotateComponent(state.selection[0]);
          }
          break;
        case 'toggle-wire-mode':
          actions.toggleWireMode();
          break;
        case 'ghost-move-up':
          e.preventDefault();
          dispatch({ type: 'GHOST_NUDGE', dx: 0, dy: -step });
          break;
        case 'ghost-move-down':
          e.preventDefault();
          dispatch({ type: 'GHOST_NUDGE', dx: 0, dy: step });
          break;
        case 'ghost-move-left':
          e.preventDefault();
          dispatch({ type: 'GHOST_NUDGE', dx: -step, dy: 0 });
          break;
        case 'ghost-move-right':
          e.preventDefault();
          dispatch({ type: 'GHOST_NUDGE', dx: step, dy: 0 });
          break;
        case 'ghost-rotate-cw':
          dispatch({ type: 'GHOST_ROTATE' });
          break;
        case 'ghost-rotate-ccw':
          dispatch({ type: 'GHOST_ROTATE', ccw: true });
          break;
        case 'ghost-place':
          if (state.ghost) {
            e.preventDefault();
            actions.placeGhost(state.ghost.x, state.ghost.y, state.ghost.orientation);
          }
          break;
        case 'ghost-cancel':
          actions.cancel();
          setExamplesMenuOpen(false);
          setCommandPaletteOpen(false);
          setShortcutsHelpOpen(false);
          break;
        case 'terminal-cycle-next':
          e.preventDefault();
          dispatch({ type: 'TERMINAL_FOCUS_CYCLE', reverse: false });
          break;
        case 'terminal-cycle-prev':
          e.preventDefault();
          dispatch({ type: 'TERMINAL_FOCUS_CYCLE', reverse: true });
          break;
        case 'wire-start-or-commit':
          e.preventDefault();
          if (state.mode === 'wiring') {
            if (!state.wireDraft && state.focusedTerminal) {
              dispatch({ type: 'WIRE_START', x: state.focusedTerminal.x, y: state.focusedTerminal.y });
            } else if (state.wireDraft) {
              const route = routeL(state.wireDraft.start, state.wireDraft.cursor, state.wireDraft.preferHorizontal);
              dispatch({ type: 'WIRE_DRAFT_END' });
              actions.finishWire(densePoints(route).map((pt) => [pt.x, pt.y]));
            }
          }
          break;
        case 'wire-step-up':
          e.preventDefault();
          dispatch({ type: 'WIRE_CURSOR_NUDGE', dx: 0, dy: -step });
          break;
        case 'wire-step-down':
          e.preventDefault();
          dispatch({ type: 'WIRE_CURSOR_NUDGE', dx: 0, dy: step });
          break;
        case 'wire-step-left':
          e.preventDefault();
          dispatch({ type: 'WIRE_CURSOR_NUDGE', dx: -step, dy: 0 });
          break;
        case 'wire-step-right':
          e.preventDefault();
          dispatch({ type: 'WIRE_CURSOR_NUDGE', dx: step, dy: 0 });
          break;
        case 'wire-abort':
          if (state.wireDraft) {
            dispatch({ type: 'WIRE_DRAFT_ABORT' });
          } else {
            actions.cancel();
          }
          break;
        case 'selection-nudge-up':
          e.preventDefault();
          actions.nudgeSelection(0, -step);
          break;
        case 'selection-nudge-down':
          e.preventDefault();
          actions.nudgeSelection(0, step);
          break;
        case 'selection-nudge-left':
          e.preventDefault();
          actions.nudgeSelection(-step, 0);
          break;
        case 'selection-nudge-right':
          e.preventDefault();
          actions.nudgeSelection(step, 0);
          break;
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [actions, commandPaletteOpen, dispatch, state.focusedTerminal, state.ghost, state.mode, state.selection, state.selectedWire, state.wireDraft]);

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

          <span className="nav-separator" />

          {/* Panel Toggle Shortcuts */}
          <button
            type="button"
            className={`nav-btn ${leftSidebarOpen ? 'active' : ''}`}
            onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}
            title="Toggle Components Palette (Ctrl+B)"
          >
            Palette <span className="kbd-shortcut">Ctrl+B</span>
          </button>
          <button
            type="button"
            className={`nav-btn ${rightSidebarOpen ? 'active' : ''}`}
            onClick={() => setRightSidebarOpen(!rightSidebarOpen)}
            title="Toggle Inspector / Properties Panel (Ctrl+I)"
          >
            Inspector <span className="kbd-shortcut">Ctrl+I</span>
          </button>

          <button
            type="button"
            className="nav-btn"
            onClick={() => setCommandPaletteOpen(true)}
            title="Search components & commands (Ctrl+K or /)"
          >
            Command Palette <span className="kbd-shortcut">Ctrl+K</span>
          </button>

          <button
            type="button"
            className="nav-btn"
            onClick={() => setShortcutsHelpOpen(true)}
            title="Keyboard Shortcuts Cheatsheet (?)"
          >
            Shortcuts <span className="kbd-shortcut">?</span>
          </button>
        </div>

        {/* Right Toolbar Actions */}
        <div className="nav-right">
          {/* Prominent Simulation Overview Tab Button */}
          <button
            type="button"
            className={`nav-btn-primary ${simState.status === 'RUNNING' ? 'simulating' : ''} ${activeWorkspaceTab === 'simulation' ? 'active' : ''}`}
            onClick={() => {
              setActiveWorkspaceTab((prev) => (prev === 'schematic' ? 'simulation' : 'schematic'));
            }}
            disabled={!state.circuitId}
            title="Switch between Schematic and Simulation Overview tab"
          >
            {simState.status === 'RUNNING' ? 'Simulating...' : '📊 Simulation'}
          </button>

          <button
            type="button"
            className="nav-btn theme-toggle"
            onClick={() => setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'))}
            title={`Switch to ${theme === 'dark' ? 'Light' : 'Dark'} theme`}
          >
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>

          <div className="connection-badge" title={wsConnected ? 'WebSocket live' : 'Offline / Polling'}>
            <span className={`status-dot ${wsConnected ? 'connected' : 'disconnected'}`} />
            <span className="status-label">{wsConnected ? 'Live' : 'Polling'}</span>
          </div>
        </div>
      </header>

      {/* Main Workspace (3-Column Layout) */}
      <div className="workspace">
        {/* Left Sidebar: Component Palette (Visible only in Schematic view) */}
        {activeWorkspaceTab === 'schematic' && (
          <aside
            className={`sidebar left ${leftSidebarOpen ? '' : 'collapsed'}`}
            onClick={() => {
              if (!leftSidebarOpen) setLeftSidebarOpen(true);
            }}
          >
            <div className="sidebar-header">
              <span className="sidebar-title">Components</span>
              <button
                type="button"
                className="sidebar-toggle-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  setLeftSidebarOpen(!leftSidebarOpen);
                }}
                title={leftSidebarOpen ? 'Collapse palette panel (Ctrl+B)' : 'Expand palette panel (Ctrl+B)'}
              >
                {leftSidebarOpen ? '◀' : '▶'}
              </button>
            </div>
            {leftSidebarOpen ? (
              <Palette catalog={catalog} onArm={actions.arm} />
            ) : (
              <div className="collapsed-strip" title="Click to expand Components Palette (Ctrl+B)">
                <span className="collapsed-strip-icon">⊞</span>
                <span className="collapsed-strip-text">COMPONENTS</span>
              </div>
            )}
          </aside>
        )}

        {/* Center: Canvas Schematic Sheet / Simulation Workspace */}
        <main className="sheet-viewport">
          {/* Workspace Tab Bar: Clean 2-tab design */}
          <div className="workspace-tabs-bar">
            <button
              type="button"
              className={`workspace-tab ${activeWorkspaceTab === 'schematic' ? 'active' : ''}`}
              onClick={() => setActiveWorkspaceTab('schematic')}
              title="Circuit Diagram Schematic Editor"
            >
              📐 Schematic
            </button>

            <button
              type="button"
              className={`workspace-tab ${activeWorkspaceTab === 'simulation' ? 'active' : ''}`}
              onClick={() => setActiveWorkspaceTab('simulation')}
              title="Circuit Simulation & Scope Waveforms"
            >
              📊 Simulation
            </button>
          </div>

          {/* Active Tab Content */}
          {activeWorkspaceTab === 'schematic' ? (
            <>
              <Sheet
                state={state}
                dispatch={dispatch}
                actions={{
                  placeGhost: actions.placeGhost,
                  finishWire: actions.finishWire,
                  commitMove: actions.commitMove,
                  deleteSelection: actions.deleteSelection,
                  rotateComponent: actions.rotateComponent,
                  deleteComponent: actions.deleteComponent,
                  deleteWire: actions.deleteWire,
                  labelWire: actions.labelWire,
                  openProperties: (name: string) => {
                    actions.openProperties(name);
                    setRightSidebarOpen(true);
                  },
                  openScopeTab: (name: string) => openScopeTab(name),
                  toggleWireMode: actions.toggleWireMode,
                  openCommandPalette: () => setCommandPaletteOpen(true),
                }}
              />

              {/* Quick Status Bar */}
              <div className="status-bar">
                <div className="status-left">
                  <span className="status-circuit-name">
                    {state.filename || 'No circuit open'}
                  </span>
                  {state.modelVersion > 0 && (
                    <span className="status-version">v{state.modelVersion}</span>
                  )}
                  {state.mode !== 'idle' && (
                    <span className={`status-mode-pill ${state.mode}`}>
                      {state.mode.toUpperCase()}
                    </span>
                  )}
                </div>
                <div className="status-msg">{state.status}</div>
                <div className="status-right">
                  <span className="status-components-count">
                    {state.components.length} components, {state.wires.length} wires
                  </span>
                </div>
              </div>
            </>
          ) : (
            <ScopeViewTab
              selectedScope={selectedScope}
              onSelectScope={setSelectedScope}
              components={state.components}
              results={simState.results}
              status={simState.status}
              progress={simState.progress}
              circuitId={state.circuitId}
              defaults={simState.defaults}
              errorMessage={simState.errorMessage}
              theme={theme}
              onRunSimulation={actions.runSimulation}
              onPauseSimulation={actions.pauseSimulation}
              onResumeSimulation={actions.resumeSimulation}
              onCancelSimulation={actions.cancelSimulation}
            />
          )}
        </main>

        {/* Right Sidebar: Component Properties or Simulation Properties Panel */}
        <aside
          className={`sidebar right ${rightSidebarOpen ? '' : 'collapsed'}`}
          onClick={() => {
            if (!rightSidebarOpen) setRightSidebarOpen(true);
          }}
        >
          <div className="sidebar-header">
            <span className="sidebar-title">
              {activeWorkspaceTab === 'schematic' ? 'Inspector' : 'Simulation Settings'}
            </span>
            <button
              type="button"
              className="sidebar-toggle-btn"
              onClick={(e) => {
                e.stopPropagation();
                setRightSidebarOpen(!rightSidebarOpen);
              }}
              title={rightSidebarOpen ? 'Collapse panel (Ctrl+I)' : 'Expand panel (Ctrl+I)'}
            >
              {rightSidebarOpen ? '▶' : '◀'}
            </button>
          </div>
          {rightSidebarOpen ? (
            activeWorkspaceTab === 'schematic' ? (
              <PropertiesPanel
                component={state.components.find((c) => c.name === state.panelFor) || null}
                onRename={actions.rename}
                onSetParameter={actions.setParameter}
                onSetLabel={actions.setLabel}
                onRotate={actions.rotateComponent}
                onDelete={actions.deleteComponent}
                onOpenScopeTab={(name) => openScopeTab(name)}
              />
            ) : (
              <SimulationPropertiesPanel
                circuitId={state.circuitId}
                status={simState.status}
                progress={simState.progress}
                defaults={simState.defaults}
                errorMessage={simState.errorMessage}
                components={state.components}
                results={simState.results}
                selectedScope={selectedScope}
                onSelectScope={setSelectedScope}
                onRunSimulation={actions.runSimulation}
                onPauseSimulation={actions.pauseSimulation}
                onResumeSimulation={actions.resumeSimulation}
                onCancelSimulation={actions.cancelSimulation}
              />
            )
          ) : (
            <div className="collapsed-strip" title="Click to expand (Ctrl+I)">
              <span className="collapsed-strip-icon">⚙</span>
              <span className="collapsed-strip-text">
                {activeWorkspaceTab === 'schematic' ? 'INSPECTOR' : 'SIMULATION'}
              </span>
            </div>
          )}
        </aside>
      </div>

      {/* Command Palette (Ctrl+K) */}
      <CommandPalette
        isOpen={commandPaletteOpen}
        onClose={() => setCommandPaletteOpen(false)}
        catalog={catalog}
        onSelect={(entry) => {
          actions.arm(entry);
          setCommandPaletteOpen(false);
        }}
      />

      {/* Keyboard Shortcuts Help Modal */}
      {shortcutsHelpOpen && (
        <div className="modal-backdrop" onClick={() => setShortcutsHelpOpen(false)}>
          <div className="shortcuts-modal" onClick={(e) => e.stopPropagation()}>
            <div className="shortcuts-modal-header">
              <div className="shortcuts-modal-title">Keyboard Shortcuts Cheatsheet</div>
              <button
                type="button"
                className="shortcuts-modal-close"
                onClick={() => setShortcutsHelpOpen(false)}
              >
                ✕
              </button>
            </div>
            <div className="shortcuts-modal-body">
              {['General', 'Placement', 'Wiring', 'Editing', 'Navigation'].map((category) => (
                <div key={category} className="shortcut-category-group">
                  <div className="shortcut-category-title">{category}</div>
                  <div className="shortcut-category-items">
                    {KEYBINDINGS.filter((b) => b.category === category).map((b, i) => (
                      <div key={i} className="shortcut-row">
                        <span className="shortcut-desc">{b.description}</span>
                        <kbd className="shortcut-kbd">
                          {b.modifiers?.ctrlOrMeta ? 'Ctrl+' : ''}
                          {b.modifiers?.shift ? 'Shift+' : ''}
                          {b.key}
                        </kbd>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
