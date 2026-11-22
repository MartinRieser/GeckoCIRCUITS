/**
 * Application Shell: top navigation bar with file menus & built-in examples,
 * simulation actions, command palette (Ctrl+K), component palette,
 * interactive schematic canvas, structured properties inspector, and workspace tabs.
 *
 * Central keyboard interaction layer powered by keybindings.ts.
 */
import { useEffect, useRef, useState, useCallback, useMemo } from 'react';
import type { EditorComponent } from './model/types';
import { useEditor } from './hooks/useEditor';
import { Sheet, type SheetFocusTarget } from './canvas/Sheet';
import { Palette } from './palette/Palette';
import { PropertiesPanel } from './properties/PropertiesPanel';
import { SimulationPropertiesPanel } from './properties/SimulationPropertiesPanel';
import { ScopePropertiesPanel } from './properties/ScopePropertiesPanel';
import { ScopeViewTab } from './simulation/ScopeViewTab';
import { useScopeController } from './simulation/useScopeController';
import { CommandPalette } from './palette/CommandPalette';
import { EXAMPLES } from './model/examples';
import { resolveShortcut, KEYBINDINGS } from './model/keybindings';
import { routeAvoidingObstacles, routingBlockedCells, densePoints } from './canvas/WireRouter';
import {
  isWireEndPointConnected,
  buildCircuitDiagnostics,
  type DiagnosticItem,
  type DiagnosticQuickFix,
} from './model/validation';
import {
  registerOpenFileHandler,
  isDesktop,
  hasNativeFileSystemAccess,
  openFileNative,
  confirmDialogNative,
  setWindowTitleNative,
} from './desktop';
import { getEngineVersion, getRawCircuit } from './api/client';
import { findScopeBlocks, findScriptBlocks, isScopeComponent, isScriptComponent } from './simulation/scopes';
import { SimConfigModal } from './simulation/SimConfigModal';
import { ParametricSweepModal } from './modals/ParametricSweepModal';
import { SaveSnippetModal } from './modals/SaveSnippetModal';
import {
  BUILT_IN_SNIPPETS,
  deleteUserSnippet,
  exportSnippetsJson,
  extractSnippetFromSelection,
  importSnippetsJson,
  loadUserSnippets,
  saveUserSnippet,
  type SchematicSnippet,
} from './model/snippets';
import { formatEngineeringValue } from './model/componentSchema';
import { SimToolbarStrip } from './simulation/SimToolbarStrip';
import { IconUndo, IconRedo } from './icons';
import { DiagnosticsBadge, DiagnosticsDrawer } from './simulation/DiagnosticsDrawer';
import { SaveAsModal } from './modals/SaveAsModal';
import { SaveAsExampleModal } from './modals/SaveAsExampleModal';
import { ExamplesModal } from './modals/ExamplesModal';
import { SettingsModal } from './modals/SettingsModal';
import { addExample } from './model/examplesService';
import { FileMenu } from './nav/FileMenu';
import { ViewMenu } from './nav/ViewMenu';
import { AnalyzeMenu } from './nav/AnalyzeMenu';
import {
  loadAppSettings,
  saveAppSettings,
  resetWorkspaceLayoutStorage,
  type AppSettings,
} from './model/appSettings';
import { ScriptViewTab } from './properties/ScriptViewTab';
import {
  resolveActiveSimSettings,
  suggestAutoTimeStep,
  type FrequencyInspectableComponent,
} from './simulation/simSettings';
import { SCOPE_SETTING_KEYS, ScopeDisplayLayout } from './model/constants';
import {
  SCOPE_TAB_PREFIX,
  SCRIPT_TAB_PREFIX,
  OSCI_TAB_PREFIX,
  makeOsciTabId,
  parseOsciTabId,
  isOsciTab,
  scopeTabId,
  scriptTabId,
  tabTargetName,
  detectRename,
} from './model/workspaceTabs';

export function App() {
  const { state, dispatch, catalog, wsConnected, simState, isDirty, actions } = useEditor();

  // Synchronize browser and desktop window title with current circuit file
  useEffect(() => {
    const dirtyPrefix = isDirty ? '*' : '';
    const title = state.filename ? `${dirtyPrefix}${state.filename} - GeckoCIRCUITS` : 'GeckoCIRCUITS';
    document.title = title;
    void setWindowTitleNative(title);
  }, [state.filename, isDirty]);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const nativeLibInputRef = useRef<HTMLInputElement>(null);
  const pendingNativeLibCompRef = useRef<string | null>(null);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);
  const [engineVersion, setEngineVersion] = useState<string | null>(null);

  // Shipped version from the engine's health endpoint (synced to the release
  // tag at build time); null while loading or when unreachable, in which case
  // the header simply shows no version badge.
  useEffect(() => {
    let cancelled = false;
    void getEngineVersion().then((version) => {
      if (!cancelled && version) setEngineVersion(version);
    });
    return () => {
      cancelled = true;
    };
  }, []);
  const [examplesModalOpen, setExamplesModalOpen] = useState(false);
  const [saveAsExampleOpen, setSaveAsExampleOpen] = useState(false);
  const [activeExample, setActiveExample] = useState<{ id: string; name: string } | null>(null);
  const [shortcutsHelpOpen, setShortcutsHelpOpen] = useState(false);
  const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
  const [rightSidebarOpen, setRightSidebarOpen] = useState(true);
  const [activeTabId, setActiveTabId] = useState<string>('schematic');
  const [simConfigOpen, setSimConfigOpen] = useState(false);
  const [sweepModalOpen, setSweepModalOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [sweepPrefill, setSweepPrefill] = useState<{ componentName: string; paramKey: string } | null>(null);
  const [tuneBarOpen, setTuneBarOpen] = useState(false);
  const [activeSweepStepIndex, setActiveSweepStepIndex] = useState<number | null>(null);
  const [sweepOverlayAll, setSweepOverlayAll] = useState(false);
  const [saveSnippetModalOpen, setSaveSnippetModalOpen] = useState(false);
  const [waveformsDrawerOpen, setWaveformsDrawerOpen] = useState(false);
  const [drawerRatio, setDrawerRatio] = useState<number>(() => {
    const saved = Number(localStorage.getItem('gecko-waveforms-drawer-ratio'));
    return Number.isFinite(saved) && saved >= 0.22 && saved <= 0.78 ? saved : 0.42;
  });
  const [isResizingDrawer, setIsResizingDrawer] = useState(false);
  const splitContainerRef = useRef<HTMLDivElement>(null);
  const drawerRatioRef = useRef(drawerRatio);
  drawerRatioRef.current = drawerRatio;
  const [userSnippets, setUserSnippets] = useState<SchematicSnippet[]>(() => loadUserSnippets());
  const allSnippets = useMemo(
    () => [...BUILT_IN_SNIPPETS, ...userSnippets],
    [userSnippets],
  );
  const [diagnosticsOpen, setDiagnosticsOpen] = useState(false);
  /** Right-click context menu state for workspace tabs. */
  const [tabContextMenu, setTabContextMenu] = useState<{
    x: number;
    y: number;
    tabKind: 'schematic' | 'graph-register' | 'scope' | 'script';
    tabId: string;
  } | null>(null);
  const tabContextMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!tabContextMenu) return;
    const onDown = (e: MouseEvent) => {
      if (tabContextMenuRef.current && !tabContextMenuRef.current.contains(e.target as Node)) {
        setTabContextMenu(null);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setTabContextMenu(null);
    };
    window.addEventListener('mousedown', onDown);
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('mousedown', onDown);
      window.removeEventListener('keydown', onKey);
    };
  }, [tabContextMenu]);
  const [focusTarget, setFocusTarget] = useState<SheetFocusTarget | null>(null);
  const [saveAsOpen, setSaveAsOpen] = useState(false);
  const [selectedScope, setSelectedScope] = useState<string>('all');
  const [appSettings, setAppSettings] = useState<AppSettings>(() => loadAppSettings());
  const [displayLayout, setDisplayLayout] = useState<ScopeDisplayLayout>(
    () => appSettings.workspace.defaultScopeLayout,
  );
  const theme = appSettings.workspace.theme;
  const updateAppSettings = useCallback((next: AppSettings) => {
    setAppSettings(next);
    saveAppSettings(next);
    setDisplayLayout(next.workspace.defaultScopeLayout);
  }, []);

  // Desktop shell: circuits opened via double-click / "Open with" / second launch
  useEffect(() => {
    registerOpenFileHandler((payload) => {
      setActiveTabId('schematic');
      setActiveExample(null);
      void actions.openBase64(payload.base64, payload.name, payload.path);
    });
  }, [actions.openBase64]);

  // Dynamic scope and script blocks discovered from schematic components
  const scopeBlocks = useMemo(() => findScopeBlocks(state.components), [state.components]);
  const scriptBlocks = useMemo(() => findScriptBlocks(state.components), [state.components]);

  const activeScriptComponent = useMemo(() => {
    const targetName = tabTargetName(activeTabId, SCRIPT_TAB_PREFIX);
    if (!targetName) return null;
    return state.components.find((c) => c.name === targetName) || null;
  }, [activeTabId, state.components]);

  const activeScopeComponent = useMemo(() => {
    const targetName = tabTargetName(activeTabId, SCOPE_TAB_PREFIX);
    if (!targetName) return null;
    return state.components.find((c) => c.name === targetName) || null;
  }, [activeTabId, state.components]);

  const isScriptActive = activeTabId.startsWith(SCRIPT_TAB_PREFIX);
  const isDockedScopeVisible = activeTabId === 'schematic' && waveformsDrawerOpen;
  const showScopePane =
    !isScriptActive &&
    (isDockedScopeVisible || isOsciTab(activeTabId) || activeTabId === 'simulation');

  const activeRegId = parseOsciTabId(activeTabId);
  const activeGraphRegister = useMemo(() => {
    if (!state.graphRegisters || state.graphRegisters.length === 0) return null;
    if (activeRegId) {
      const match = state.graphRegisters.find(
        (r) => r.id === activeRegId || r.name === activeRegId,
      );
      if (match) return match;
    }
    if (isOsciTab(activeTabId) || activeTabId === 'simulation') {
      return state.graphRegisters[0];
    }
    return null;
  }, [activeRegId, activeTabId, state.graphRegisters]);

  const drawerGraphRegister = activeGraphRegister ?? state.graphRegisters?.[0] ?? null;

  // Derived value for "which scope is inspected": active register name, or legacy scope
  const activeSelectedScope =
    activeGraphRegister?.name ?? tabTargetName(activeTabId, SCOPE_TAB_PREFIX) ?? selectedScope;

  const currentDisplayLayout: ScopeDisplayLayout =
    activeGraphRegister
      ? activeGraphRegister.numGraphs === 1
        ? 'overlay'
        : activeGraphRegister.numGraphs === 2
          ? '2-graphs'
          : '3-graphs'
      : (activeScopeComponent?.parameters?.[SCOPE_SETTING_KEYS.scopeLayout] as ScopeDisplayLayout) ||
        displayLayout;

  const handleDisplayLayoutChange = useCallback(
    (layout: ScopeDisplayLayout) => {
      setDisplayLayout(layout);
      localStorage.setItem('gecko-display-layout', layout);
      if (activeGraphRegister) {
        const numGraphs = layout === 'overlay' ? 1 : layout === '2-graphs' ? 2 : 3;
        actions.updateGraphRegister(activeGraphRegister.id, { numGraphs });
      }
      if (activeScopeComponent) {
        actions.setParameter(activeScopeComponent.name, SCOPE_SETTING_KEYS.scopeLayout, layout);
      }
    },
    [activeGraphRegister, activeScopeComponent, actions],
  );

  useEffect(() => {
    const saved = activeScopeComponent?.parameters?.[SCOPE_SETTING_KEYS.scopeLayout];
    if (saved) {
      setDisplayLayout(saved as ScopeDisplayLayout);
    }
  }, [activeScopeComponent?.name, activeScopeComponent?.parameters?.[SCOPE_SETTING_KEYS.scopeLayout]]);

  // Follow component renames (keep the active tab on the renamed block) and
  // fall back to the schematic when the active tab's component was deleted.
  const prevComponentsRef = useRef<EditorComponent[]>(state.components);
  useEffect(() => {
    const rename = detectRename(
      prevComponentsRef.current.map((c) => c.name),
      state.components.map((c) => c.name),
    );
    if (rename) {
      const before = prevComponentsRef.current.find((c) => c.name === rename.from);
      const after = state.components.find((c) => c.name === rename.to);
      const sameKind =
        !!before &&
        !!after &&
        isScopeComponent(before) === isScopeComponent(after) &&
        isScriptComponent(before) === isScriptComponent(after);
      if (sameKind) {
        setActiveTabId((tab) =>
          tab === scopeTabId(rename.from)
            ? scopeTabId(rename.to)
            : tab === scriptTabId(rename.from)
              ? scriptTabId(rename.to)
              : tab,
        );
        setSelectedScope((s) => (s === rename.from ? rename.to : s));
        prevComponentsRef.current = state.components;
        return;
      }
    }
    prevComponentsRef.current = state.components;
    if (activeTabId.startsWith(SCRIPT_TAB_PREFIX) && !activeScriptComponent) {
      setActiveTabId('schematic');
    } else if (
      activeTabId.startsWith(SCOPE_TAB_PREFIX) &&
      !activeScopeComponent &&
      !state.graphRegisters?.some((r) => r.name === parseOsciTabId(activeTabId))
    ) {
      setActiveTabId('schematic');
    } else if (activeTabId.startsWith(OSCI_TAB_PREFIX)) {
      const regId = parseOsciTabId(activeTabId);
      if (!state.graphRegisters?.some((r) => r.id === regId || r.name === regId)) {
        setActiveTabId(
          state.graphRegisters && state.graphRegisters.length > 0
            ? makeOsciTabId(state.graphRegisters[0].id)
            : 'schematic',
        );
      }
    }
  }, [state.components, state.graphRegisters, activeTabId, activeScriptComponent, activeScopeComponent]);

  const handleUpdateActiveGraphRegister = useCallback(
    (patch: Partial<typeof activeGraphRegister extends infer T ? NonNullable<T> : never>) => {
      if (activeGraphRegister) {
        void actions.updateGraphRegister(activeGraphRegister.id, patch);
      }
    },
    [activeGraphRegister, actions],
  );

  const activeSweepRun = useMemo(() => {
    if (!simState.sweepSession || sweepOverlayAll || activeSweepStepIndex === null) return null;
    return (
      simState.sweepSession.runs.find(
        (r) => r.index === activeSweepStepIndex && r.status === 'COMPLETED' && Boolean(r.results),
      ) ?? null
    );
  }, [simState.sweepSession, sweepOverlayAll, activeSweepStepIndex]);

  const effectiveSimResults = activeSweepRun?.results ?? simState.results;
  const sweepPreviewLabel =
    activeSweepRun && simState.sweepSession
      ? `${simState.sweepSession.componentName} = ${formatEngineeringValue(activeSweepRun.parameterValue, simState.sweepSession.unit)}`
      : null;

  const scope = useScopeController({
    results: effectiveSimResults,
    components: state.components,
    selectedScope: activeSelectedScope,
    graphRegister: activeGraphRegister,
    theme,
    onSetParameter: actions.setParameter,
    onUpdateGraphRegister: handleUpdateActiveGraphRegister,
  });
  const [rightSidebarWidth, setRightSidebarWidth] = useState<number>(() => {
    const saved = localStorage.getItem('gecko-right-sidebar-width');
    return saved ? Math.max(260, Math.min(800, Number(saved))) : 320;
  });
  const [isResizingRight, setIsResizingRight] = useState(false);

  // Resize handler for right properties sidebar
  const handleStartResizeRight = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsResizingRight(true);
  };

  useEffect(() => {
    if (!isResizingRight) return;

    const handleMouseMove = (e: MouseEvent) => {
      const newWidth = window.innerWidth - e.clientX;
      const minW = 260;
      const maxW = Math.min(800, Math.max(300, window.innerWidth - 300));
      const clamped = Math.max(minW, Math.min(maxW, newWidth));
      setRightSidebarWidth(clamped);
    };

    const handleMouseUp = () => {
      setIsResizingRight(false);
      localStorage.setItem('gecko-right-sidebar-width', String(rightSidebarWidth));
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isResizingRight, rightSidebarWidth]);

  const handleStartResizeDrawer = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsResizingDrawer(true);
  };

  useEffect(() => {
    if (!isResizingDrawer) return;
    const onMove = (e: MouseEvent) => {
      const el = splitContainerRef.current;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      if (rect.height <= 0) return;
      const raw = (rect.bottom - e.clientY) / rect.height;
      setDrawerRatio(Math.max(0.22, Math.min(0.78, raw)));
    };
    const onUp = () => {
      setIsResizingDrawer(false);
      try {
        localStorage.setItem('gecko-waveforms-drawer-ratio', String(drawerRatioRef.current));
      } catch {
        // ignore
      }
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
  }, [isResizingDrawer]);

  const openScopeTab = (scopeName: string) => {
    const normalizedId = scopeName.toLowerCase().replace(/[^a-z0-9]+/g, '_');
    const matchingReg =
      state.graphRegisters?.find(
        (r) => r.name === scopeName || r.id === scopeName || r.id === normalizedId,
      ) ?? state.graphRegisters?.[0];
    if (matchingReg) {
      setSelectedScope(matchingReg.name);
      setActiveTabId(makeOsciTabId(matchingReg.id));
      return;
    }
    setSelectedScope(scopeName);
    setActiveTabId(scopeTabId(scopeName));
  };

  const openScriptTab = (scriptName: string) => {
    setActiveTabId(scriptTabId(scriptName));
  };

  // Auto-open the bottom Waveforms drawer when simulation completes while viewing Schematic (if enabled)
  const prevSimStatusRef = useRef<string | null>(null);
  useEffect(() => {
    if (
      appSettings.workspace.autoOpenWaveformsOnRun &&
      simState.status === 'COMPLETED' &&
      prevSimStatusRef.current !== 'COMPLETED' &&
      activeTabId === 'schematic'
    ) {
      setWaveformsDrawerOpen(true);
    }
    prevSimStatusRef.current = simState.status;
  }, [simState.status, activeTabId, appSettings.workspace.autoOpenWaveformsOnRun]);

  // When a parametric sweep completes, activate step 0 across Scope & schematic and reveal results
  const prevSweepStatusRef = useRef<string | null>(null);
  useEffect(() => {
    const next = simState.sweepSession?.status ?? null;
    if (next === 'COMPLETED' && prevSweepStatusRef.current !== 'COMPLETED') {
      const firstRun = simState.sweepSession?.runs.find((r) => r.status === 'COMPLETED');
      setActiveSweepStepIndex(firstRun?.index ?? 0);
      setSweepOverlayAll(false);
      if (sweepModalOpen) {
        setSweepModalOpen(false);
        setSweepPrefill(null);
      }
      if (activeTabId === 'schematic') {
        setWaveformsDrawerOpen(true);
      } else {
        openScopeTab(drawerGraphRegister?.name || 'Osci 1');
      }
    } else if (next === null) {
      setActiveSweepStepIndex(null);
      setSweepOverlayAll(false);
    }
    prevSweepStatusRef.current = next;
  }, [
    simState.sweepSession,
    sweepModalOpen,
    activeTabId,
    drawerGraphRegister?.name,
  ]);

  const isSimRunning = simState.status === 'RUNNING' || simState.status === 'PENDING';

  const effectiveSim = useMemo(
    () => resolveActiveSimSettings(simState.defaults, simState.simSettings),
    [simState.defaults, simState.simSettings],
  );

  const diagnostics = useMemo(
    () =>
      buildCircuitDiagnostics(state.components, state.wires, {
        dt: effectiveSim.dtStr,
        tEnd: effectiveSim.tEndStr,
        simStatus: simState.status ?? undefined,
        simErrorMessage: simState.errorMessage,
        simWarnings: simState.warnings,
      }),
    [
      state.components,
      state.wires,
      effectiveSim.dtStr,
      effectiveSim.tEndStr,
      simState.status,
      simState.errorMessage,
      simState.warnings,
    ],
  );

  // Surface new simulation failures and engine warnings automatically regardless of active tab
  const lastSurfacedFailureRef = useRef<string>('');
  useEffect(() => {
    const err = simState.errorMessage || '';
    const warnsKey = (simState.warnings ?? []).join('|');
    const signature =
      simState.status === 'FAILED' || err || warnsKey
        ? `${simState.status}:${err}:${warnsKey}`
        : '';
    if (signature && signature !== lastSurfacedFailureRef.current) {
      lastSurfacedFailureRef.current = signature;
      setDiagnosticsOpen(true);
    } else if (!signature) {
      lastSurfacedFailureRef.current = '';
    }
  }, [simState.status, simState.errorMessage, simState.warnings]);

  const focusCanvasAt = useCallback((x: number, y: number) => {
    setFocusTarget((prev) => ({ x, y, seq: (prev?.seq ?? 0) + 1 }));
  }, []);

  const handleLocateDiagnostic = useCallback(
    (item: DiagnosticItem) => {
      setActiveTabId('schematic');
      if (item.componentName) {
        dispatch({ type: 'SELECT', name: item.componentName, additive: false });
        actions.openProperties(item.componentName);
        setRightSidebarOpen(true);
        const comp = state.components.find((c) => c.name === item.componentName);
        if (comp) {
          focusCanvasAt(comp.position[0], comp.position[1]);
        } else if (item.coord) {
          focusCanvasAt(item.coord.x, item.coord.y);
        }
        return;
      }
      if (item.wireIndex !== undefined) {
        dispatch({ type: 'SELECT_WIRE', index: item.wireIndex });
        const wire = state.wires.find((w) => w.index === item.wireIndex);
        const pt = wire?.points?.[0];
        if (pt) {
          focusCanvasAt(pt[0], pt[1]);
        } else if (item.coord) {
          focusCanvasAt(item.coord.x, item.coord.y);
        }
        return;
      }
      if (item.coord) {
        focusCanvasAt(item.coord.x, item.coord.y);
      }
    },
    [dispatch, actions, state.components, state.wires, focusCanvasAt],
  );

  const handleQuickFixDiagnostic = useCallback(
    (item: DiagnosticItem, fix: DiagnosticQuickFix) => {
      switch (fix.action) {
        case 'auto-dt': {
          const nextDt =
            fix.suggestedDt ||
            suggestAutoTimeStep(
              state.components as FrequencyInspectableComponent[],
              effectiveSim.tEnd,
            ).dtStr;
          actions.updateSimSettings({
            dt: nextDt,
            tEnd: effectiveSim.tEndStr,
            solver: effectiveSim.solver,
          });
          break;
        }
        case 'open-sim-config':
          setSimConfigOpen(true);
          break;
        case 'select-component':
        case 'focus-wire':
          handleLocateDiagnostic(item);
          break;
        case 'delete-wire':
          if (fix.wireIndex !== undefined) {
            actions.deleteWire(fix.wireIndex);
          }
          break;
        case 'attach-native-library':
          if (fix.componentName) {
            pendingNativeLibCompRef.current = fix.componentName;
            handleLocateDiagnostic(item);
            nativeLibInputRef.current?.click();
          }
          break;
      }
    },
    [
      state.components,
      effectiveSim.tEnd,
      actions,
      simState.simSettings,
      handleLocateDiagnostic,
    ],
  );

  const armQuickComponent = useCallback(
    (type: number, family: string) => {
      const entry = catalog.find((c) => c.type === type && c.family === family);
      if (entry) {
        actions.arm(entry);
      } else {
        actions.arm({
          type,
          family,
          displayName: String(type),
          description: '',
          category: family,
        } as unknown as Parameters<typeof actions.arm>[0]);
      }
    },
    [catalog, actions],
  );

  const handleRenameComponent = useCallback(
    (name: string) => {
      actions.openProperties(name);
      setRightSidebarOpen(true);
      dispatch({ type: 'SELECT', name, additive: false });
      const next = window.prompt(`Rename '${name}' to:`, name);
      if (next && next.trim() && next.trim() !== name) {
        void actions.rename(name, next.trim());
      }
    },
    [actions, dispatch],
  );

  // F5 / ▶ Run: execute immediately with the effective settings. Ignored while
  // a run is active so the shortcut can never double-start a simulation.
  const handleQuickRun = useCallback(() => {
    if (!state.circuitId) return;
    if (simState.status === 'RUNNING' || simState.status === 'PENDING') return;
    if (activeTabId === 'schematic') {
      setWaveformsDrawerOpen(true);
    }
    const { tEnd, dt, solver } = resolveActiveSimSettings(simState.defaults, simState.simSettings);
    actions.runSimulation({
      simulationTime: tEnd,
      timeStep: dt,
      solverType: solver,
      backend: 'headless',
    });
  }, [state.circuitId, simState.status, simState.defaults, simState.simSettings, actions, activeTabId]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('gecko-theme', theme);
  }, [theme]);

  // New / Open / Examples replace whatever is in the editor.
  const confirmDiscardCircuit = useCallback(async (): Promise<boolean> => {
    if (!isDirty) return true;
    if (state.components.length === 0 && state.wires.length === 0) return true;
    if (isDesktop()) {
      return await confirmDialogNative(
        'Replace the current circuit? Changes not saved as a .ipes file will be lost.',
        'GeckoCIRCUITS',
      );
    }
    return window.confirm(
      'Replace the current circuit? Changes not saved as a .ipes file will be lost.',
    );
  }, [isDirty, state.components.length, state.wires.length]);

  const handleOpenCircuit = useCallback(async () => {
    if (hasNativeFileSystemAccess()) {
      const payload = await openFileNative(state.filePath);
      if (!payload) return;
      if (!(await confirmDiscardCircuit())) return;
      setActiveTabId('schematic');
      setActiveExample(null);
      void actions.openBase64(payload.base64, payload.name, payload.path);
    } else {
      setActiveExample(null);
      fileInputRef.current?.click();
    }
  }, [confirmDiscardCircuit, state.filePath, actions]);

  const handleSaveAs = useCallback(() => {
    if (hasNativeFileSystemAccess()) {
      void actions.saveAs();
    } else {
      setSaveAsOpen(true);
    }
  }, [actions]);

  const handleNewCircuit = useCallback(async () => {
    if (await confirmDiscardCircuit()) {
      setActiveTabId('schematic');
      setActiveExample(null);
      actions.newCircuit();
    }
  }, [confirmDiscardCircuit, actions]);

  // Global Keyboard Shortcuts (Central Dispatcher)
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      // Modals manage their own keybindings; suspend global schematic shortcuts
      if (settingsOpen) {
        if (e.key === 'Escape') {
          e.preventDefault();
          setSettingsOpen(false);
        }
        return;
      }
      if ((e.ctrlKey || e.metaKey) && e.key === ',') {
        e.preventDefault();
        setSettingsOpen(true);
        return;
      }
      if (simConfigOpen || saveAsOpen || examplesModalOpen || saveAsExampleOpen || saveSnippetModalOpen) {
        return;
      }
      if (shortcutsHelpOpen) {
        if (e.key === 'Escape') {
          e.preventDefault();
          setShortcutsHelpOpen(false);
        }
        return;
      }
      if (commandPaletteOpen) {
        return;
      }

      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement ||
        e.target instanceof HTMLSelectElement
      ) {
        return;
      }

      if (e.key === 'F5' || ((e.ctrlKey || e.metaKey) && e.key === 'Enter')) {
        e.preventDefault();
        handleQuickRun();
        return;
      }

      const hasSelection = state.selection.length > 0;
      const action = resolveShortcut(e, state.mode, hasSelection);
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
        case 'save':
          e.preventDefault();
          void actions.save();
          break;
        case 'save-as':
          e.preventDefault();
          handleSaveAs();
          break;
        case 'open':
          e.preventDefault();
          void handleOpenCircuit();
          break;
        case 'new-circuit':
          e.preventDefault();
          void handleNewCircuit();
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
          if (state.selection.length || state.selectedWire !== null || (state.selectedWires && state.selectedWires.length > 0)) {
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
          } else if (state.selectedWire !== null) {
            actions.flipWire(state.selectedWire);
          }
          break;
        case 'rotate-selection-ccw':
          if (state.selection.length === 1) {
            actions.rotateComponent(state.selection[0], true);
          }
          break;
        case 'toggle-wire-mode':
          actions.toggleWireMode();
          break;
        case 'toggle-probe-mode':
          actions.toggleProbeMode();
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
          setExamplesModalOpen(false);
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
              const route = routeAvoidingObstacles(
                state.wireDraft.start, state.wireDraft.cursor,
                routingBlockedCells(state.components),
                state.wireDraft.preferHorizontal);
              const dense = densePoints(route).map((pt) => [pt.x, pt.y] as [number, number]);
              dispatch({ type: 'WIRE_DRAFT_END' });
              actions.finishWire(dense);
              const endPoint = { x: dense[dense.length - 1][0], y: dense[dense.length - 1][1] };
              if (!isWireEndPointConnected(endPoint, state.components, state.wires)) {
                dispatch({
                  type: 'STATUS',
                  status: `⚠️ Wire end (${endPoint.x}, ${endPoint.y}) is not connected to a terminal or wire — it will not conduct`,
                });
              }
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
  }, [actions, commandPaletteOpen, dispatch, handleNewCircuit, handleOpenCircuit, handleQuickRun, handleSaveAs, shortcutsHelpOpen, simConfigOpen, saveAsOpen, examplesModalOpen, saveAsExampleOpen, state.focusedTerminal, state.ghost, state.mode, state.selection, state.selectedWire, state.wireDraft]);

  const handleSelectExample = async (exampleId: string, customContent?: string, customName?: string) => {
    let content = customContent;
    let name = customName;
    if (!content) {
      const ex = EXAMPLES.find((e) => e.id === exampleId);
      if (ex) {
        content = ex.content;
        name = ex.name;
      }
    }
    if (content && (await confirmDiscardCircuit())) {
      setActiveTabId('schematic');
      actions.openContent(content, `${name || exampleId}.ipes`);
      setActiveExample({ id: exampleId, name: name || exampleId });
    }
    setExamplesModalOpen(false);
  };

  const handleSaveAsExample = useCallback(
    async (meta: {
      id?: string;
      name: string;
      category: string;
      description: string;
      overwrite?: boolean;
    }) => {
      if (!state.circuitId) return;
      let content = '';
      try {
        content = await getRawCircuit(state.circuitId);
      } catch (err) {
        console.warn('Failed to fetch raw circuit for example save:', err);
      }
      if (!content) {
        throw new Error('Unable to retrieve circuit schematic content');
      }
      const targetId = meta.id || (meta.overwrite && activeExample ? activeExample.id : undefined);
      const saved = await addExample({
        id: targetId,
        name: meta.name,
        category: meta.category,
        description: meta.description,
        content,
        overwrite: meta.overwrite,
      });
      setActiveExample({ id: saved.id, name: saved.name });
      dispatch({
        type: 'STATUS',
        status: meta.overwrite
          ? `Overwrote example "${saved.name}" in Examples library`
          : `Saved "${saved.name}" to Examples library`,
      });
    },
    [state.circuitId, activeExample, dispatch],
  );

  const handleGetActiveContent = useCallback(async (): Promise<string | undefined> => {
    if (!state.circuitId) return undefined;
    try {
      return await getRawCircuit(state.circuitId);
    } catch (err) {
      console.warn('Failed to fetch raw circuit content:', err);
      return undefined;
    }
  }, [state.circuitId]);

  return (
    <div className="app">
      {/* Top Main Navigation Bar */}
      <header className="navbar">
        <div className="nav-brand">
          <div className="nav-logo">
            <span className="logo-symbol">GeckoCIRCUITS</span>
          </div>
          {engineVersion && (
            <span
              className="nav-version-badge"
              title="GeckoCIRCUITS application version (from the simulation engine)"
            >
              v{engineVersion}
            </span>
          )}
        </div>

        {/* File & Edit Actions */}
        <div className="nav-actions">
          <FileMenu
            onNew={handleNewCircuit}
            onOpen={handleOpenCircuit}
            onOpenExamples={() => setExamplesModalOpen(true)}
            onSave={actions.save}
            onSaveAs={handleSaveAs}
            onSaveAsExample={() => setSaveAsExampleOpen(true)}
            activeExampleName={activeExample?.name}
            onOpenRecent={async (entry) => {
              if (await confirmDiscardCircuit()) {
                setActiveTabId('schematic');
                setActiveExample(null);
                actions.openRecent(entry);
              }
            }}
            canSave={!!state.circuitId}
            busy={state.busy}
          />
          <ViewMenu
            leftSidebarOpen={leftSidebarOpen}
            onToggleLeftSidebar={() => setLeftSidebarOpen((p) => !p)}
            rightSidebarOpen={rightSidebarOpen}
            onToggleRightSidebar={() => setRightSidebarOpen((p) => !p)}
            waveformsDrawerOpen={waveformsDrawerOpen}
            onToggleWaveformsDrawer={() => setWaveformsDrawerOpen((p) => !p)}
            diagnosticsOpen={diagnosticsOpen}
            onToggleDiagnostics={() => setDiagnosticsOpen((p) => !p)}
            settings={appSettings}
            onUpdateSettings={updateAppSettings}
            onResetLayout={() => {
              resetWorkspaceLayoutStorage();
              setLeftSidebarOpen(true);
              setRightSidebarOpen(true);
              setRightSidebarWidth(320);
              setDrawerRatio(0.42);
              dispatch({ type: 'STATUS', status: 'Reset workspace panel layout' });
            }}
          />
          <AnalyzeMenu
            circuitId={state.circuitId}
            hasResults={Boolean(effectiveSimResults && Object.keys(effectiveSimResults).length > 0)}
            features={appSettings.features}
            onOpenSimConfig={() => setSimConfigOpen(true)}
            onPinReference={() => actions.pinBaseline(undefined, effectiveSimResults)}
            onOpenSweep={() => setSweepModalOpen(true)}
            onToggleLiveSliders={() => {
              setTuneBarOpen((prev) => !prev);
              if (activeTabId === 'schematic') setWaveformsDrawerOpen(true);
            }}
            onOpenDiagnostics={() => setDiagnosticsOpen(true)}
            onOpenCommandPalette={() => setCommandPaletteOpen(true)}
            onOpenShortcuts={() => setShortcutsHelpOpen(true)}
          />
          <input
            ref={fileInputRef}
            type="file"
            accept=".ipes,.txt"
            hidden
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (file && (await confirmDiscardCircuit())) {
                setActiveTabId('schematic');
                setActiveExample(null);
                actions.open(file);
              }
              e.target.value = '';
            }}
          />
          <input
            ref={nativeLibInputRef}
            type="file"
            accept=".dll,.so,.dylib"
            hidden
            onChange={async (e) => {
              const file = e.target.files?.[0];
              const compName = pendingNativeLibCompRef.current;
              e.target.value = '';
              pendingNativeLibCompRef.current = null;
              if (file && compName) {
                await actions.attachNativeLibrary(compName, file);
              }
            }}
          />

          <span className="nav-separator" />

          <button
            type="button"
            className="nav-btn icon-only"
            onClick={actions.undo}
            disabled={!state.circuitId}
            title="Undo (Ctrl+Z)"
          >
            <IconUndo />
          </button>
          <button
            type="button"
            className="nav-btn icon-only"
            onClick={actions.redo}
            disabled={!state.circuitId}
            title="Redo (Ctrl+Y)"
          >
            <IconRedo />
          </button>

          <span className="nav-separator" />

          <SimToolbarStrip
            circuitId={state.circuitId}
            isRunning={isSimRunning}
            status={simState.status}
            progress={simState.progress}
            defaults={simState.defaults}
            settings={simState.simSettings}
            components={state.components}
            onSettingsChange={actions.updateSimSettings}
            onQuickRun={handleQuickRun}
            onPause={actions.pauseSimulation}
            onResume={actions.resumeSimulation}
            onCancel={actions.cancelSimulation}
            onOpenConfigModal={() => setSimConfigOpen(true)}
            onStatusMessage={(status) => dispatch({ type: 'STATUS', status })}
          />
        </div>

        {/* Right Toolbar Actions */}
        <div className="nav-right">
          <button
            type="button"
            data-testid="nav-settings-btn"
            className={`nav-btn${settingsOpen ? ' active' : ''}`}
            onClick={() => setSettingsOpen(true)}
            title="Open Settings & Workspace Feature Toggles (Ctrl+,)"
          >
            ⚙ Settings
          </button>

          <DiagnosticsBadge
            diagnostics={diagnostics}
            isOpen={diagnosticsOpen}
            onToggle={() => setDiagnosticsOpen((prev) => !prev)}
            testId="navbar-diagnostics-badge"
          />

          <div className="connection-badge" title={wsConnected ? 'WebSocket live' : 'Offline / Polling'}>
            <span className={`status-dot ${wsConnected ? 'connected' : 'disconnected'}`} />
            <span className="status-label">{wsConnected ? 'Live' : 'Polling'}</span>
          </div>
        </div>
      </header>

      {/* Global Workspace Tab Bar: Dynamically populated from circuit components */}
      <div className="workspace-tabs-bar">
        <button
          type="button"
          className={`workspace-tab ${activeTabId === 'schematic' ? 'active' : ''}`}
          onClick={() => setActiveTabId('schematic')}
          onContextMenu={(e) => {
            e.preventDefault();
            setTabContextMenu({ x: e.clientX, y: e.clientY, tabKind: 'schematic', tabId: 'schematic' });
          }}
          title="Circuit Diagram Schematic Editor"
        >
          📐 Schematic
        </button>

        {state.graphRegisters && state.graphRegisters.length > 0 ? (
          state.graphRegisters.map((reg) => {
            const tabId = makeOsciTabId(reg.id);
            const isActive = activeTabId === tabId || activeTabId === scopeTabId(reg.name);
            const hasData = !!(
              simState.results &&
              (reg.signals || []).some((lbl) => lbl && simState.results?.[lbl])
            );
            return (
              <div key={reg.id} style={{ display: 'inline-flex', alignItems: 'center' }}>
                <button
                  key={tabId}
                  type="button"
                  className={`workspace-tab ${isActive ? 'active' : ''}`}
                  onClick={() => {
                    setSelectedScope(reg.name);
                    setActiveTabId(tabId);
                  }}
                  onContextMenu={(e) => {
                    e.preventDefault();
                    setTabContextMenu({
                      x: e.clientX,
                      y: e.clientY,
                      tabKind: 'graph-register',
                      tabId: reg.id,
                    });
                  }}
                  title={`Oscilloscope Register: ${reg.name} (${reg.numGraphs} graph${reg.numGraphs > 1 ? 's' : ''}, ${reg.signals.length} signals)`}
                >
                  <span>📈</span>
                  <span>{reg.name}</span>
                  <span className="workspace-tab-badge">{reg.numGraphs}G</span>
                  {hasData && <span className="tab-data-dot" title="Waveform data available" />}
                </button>
                {state.graphRegisters.length > 1 && (
                  <button
                    type="button"
                    className="workspace-tab-close"
                    title={`Close ${reg.name}`}
                    aria-label={`Close ${reg.name}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      actions.removeGraphRegister(reg.id);
                      if (isActive) {
                        const remaining = state.graphRegisters.filter((r) => r.id !== reg.id);
                        if (remaining.length > 0) {
                          setSelectedScope(remaining[0].name);
                          setActiveTabId(makeOsciTabId(remaining[0].id));
                        } else {
                          setActiveTabId('schematic');
                        }
                      }
                    }}
                  >
                    ×
                  </button>
                )}
              </div>
            );
          })
        ) : (
          scopeBlocks.map((sb) => {
            const tabId = scopeTabId(sb.name);
            const isActive = activeTabId === tabId;
            const hasData = !!(
              simState.results &&
              (sb.inputLabels || []).some((lbl) => lbl && simState.results?.[lbl])
            );
            return (
              <button
                key={tabId}
                type="button"
                className={`workspace-tab ${isActive ? 'active' : ''}`}
                onClick={() => {
                  setSelectedScope(sb.name);
                  setActiveTabId(tabId);
                }}
                onContextMenu={(e) => {
                  e.preventDefault();
                  setTabContextMenu({ x: e.clientX, y: e.clientY, tabKind: 'scope', tabId: sb.name });
                }}
                title={`Oscilloscope Instrument: ${sb.name}`}
              >
                <span>📺</span>
                <span>{sb.name}</span>
                {hasData && <span className="tab-data-dot" title="Waveform data available" />}
              </button>
            );
          })
        )}

        {/* Dedicated + Button to add a new Osci / Graph View Register */}
        <button
          type="button"
          className="workspace-add-tab-btn"
          onClick={() => {
            const currentRegs = state.graphRegisters || [];
            let idx = currentRegs.length + 1;
            while (
              currentRegs.some(
                (r) =>
                  r.name.toLowerCase() === `osci ${idx}` ||
                  r.id === `osci_${idx}`,
              )
            ) {
              idx++;
            }
            const newId = `osci_${Date.now()}_${idx}`;
            const newName = `Osci ${idx}`;
            void actions.addGraphRegister({
              id: newId,
              name: newName,
              numGraphs: 1,
              signals: [],
              signalGraphs: {},
              hiddenSignals: [],
              yScaleMode: 'auto',
              cursorsEnabled: false,
            });
            setSelectedScope(newName);
            setActiveTabId(makeOsciTabId(newId));
          }}
          title="Add Osci / Graph View Register"
          aria-label="Add Osci / Graph View Register"
        >
          + Add Osci
        </button>

        {scriptBlocks.map((scr) => {
          const tabId = scriptTabId(scr.name);
          const isActive = activeTabId === tabId;
          return (
            <button
              key={tabId}
              type="button"
              className={`workspace-tab ${isActive ? 'active' : ''}`}
              onClick={() => setActiveTabId(tabId)}
              onContextMenu={(e) => {
                e.preventDefault();
                setTabContextMenu({ x: e.clientX, y: e.clientY, tabKind: 'script', tabId: scr.name });
              }}
              title={`Script IDE Editor: ${scr.name}`}
            >
              <span>💻</span>
              <span>{scr.name}</span>
            </button>
          );
        })}

        {/* Generic simulation tab if results exist without registers or scope blocks */}
        {(!state.graphRegisters || state.graphRegisters.length === 0) && scopeBlocks.length === 0 && simState.results && (
          <button
            type="button"
            className={`workspace-tab ${activeTabId === 'simulation' ? 'active' : ''}`}
            onClick={() => {
              setSelectedScope('all');
              setActiveTabId('simulation');
            }}
            title="Circuit Simulation & Scope Waveforms"
          >
            📊 Simulation
          </button>
        )}

        {activeTabId === 'schematic' && (
          <button
            type="button"
            data-testid="toggle-waveforms-drawer-btn"
            className={`workspace-drawer-toggle-btn ${waveformsDrawerOpen ? 'active' : ''}`}
            onClick={() => setWaveformsDrawerOpen((prev) => !prev)}
            title={
              waveformsDrawerOpen
                ? 'Hide bottom Waveforms panel'
                : 'Show Waveforms panel below Schematic'
            }
          >
            📈 Waveforms {waveformsDrawerOpen ? '▾' : '▴'}
          </button>
        )}
      </div>

      {/* Workspace tab right-click context menu */}
      {tabContextMenu && (() => {
        const close = () => setTabContextMenu(null);
        const act = (fn: () => void) => () => {
          fn();
          close();
        };
        const reg =
          tabContextMenu.tabKind === 'graph-register'
            ? state.graphRegisters?.find((r) => r.id === tabContextMenu.tabId)
            : undefined;
        const comp =
          tabContextMenu.tabKind === 'scope' || tabContextMenu.tabKind === 'script'
            ? state.components.find((c) => c.name === tabContextMenu.tabId)
            : undefined;
        return (
          <div
            ref={tabContextMenuRef}
            className="context-menu workspace-tab-context-menu"
            data-testid="workspace-tab-context-menu"
            style={{ left: tabContextMenu.x, top: tabContextMenu.y }}
            onContextMenu={(e) => e.preventDefault()}
          >
            {tabContextMenu.tabKind === 'schematic' && (
              <>
                <div className="context-menu-header"><span>Schematic</span></div>
                <button type="button" className="context-menu-item" onClick={act(handleQuickRun)}>
                  <span>▶ Run Simulation (F5)</span>
                </button>
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={act(() => setDiagnosticsOpen((o) => !o))}
                >
                  <span>🔍 Toggle Diagnostics Drawer</span>
                </button>
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={act(() => {
                    void actions.addGraphRegister({ numGraphs: 1 });
                  })}
                >
                  <span>➕ Add New Osci Tab</span>
                </button>
              </>
            )}
            {reg && (
              <>
                <div className="context-menu-header"><span>{reg.name}</span></div>
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={act(() => {
                    const next = window.prompt(`Rename '${reg.name}' to:`, reg.name);
                    if (next && next.trim()) {
                      void actions.updateGraphRegister(reg.id, { name: next.trim() });
                    }
                  })}
                >
                  <span>✏️ Rename Osci Tab...</span>
                </button>
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={act(() => {
                    void actions.addGraphRegister({
                      name: `${reg.name} Copy`,
                      numGraphs: reg.numGraphs,
                      signals: [...reg.signals],
                      signalGraphs: { ...reg.signalGraphs },
                      hiddenSignals: [...(reg.hiddenSignals || [])],
                      yScaleMode: reg.yScaleMode,
                      cursorsEnabled: reg.cursorsEnabled,
                    });
                  })}
                >
                  <span>📋 Duplicate Osci Tab</span>
                </button>
                <div className="context-menu-sep" />
                {([1, 2, 3] as const).map((n) => (
                  <button
                    key={n}
                    type="button"
                    className="context-menu-item"
                    onClick={act(() => {
                      void actions.updateGraphRegister(reg.id, { numGraphs: n });
                    })}
                  >
                    <span>
                      {reg.numGraphs === n ? '✓ ' : ''}📊 Layout: {n} Graph{n > 1 ? 's' : ''}
                    </span>
                  </button>
                ))}
                <div className="context-menu-sep" />
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={act(() => {
                    void actions.updateGraphRegister(reg.id, {
                      signals: [],
                      signalGraphs: {},
                      hiddenSignals: [],
                    });
                  })}
                >
                  <span>🧹 Clear All Signals</span>
                </button>
                {state.graphRegisters.length > 1 && (
                  <button
                    type="button"
                    className="context-menu-item danger"
                    onClick={act(() => {
                      void actions.removeGraphRegister(reg.id);
                    })}
                  >
                    <span>✕ Close Osci Tab</span>
                  </button>
                )}
              </>
            )}
            {comp && (
              <>
                <div className="context-menu-header"><span>{comp.name}</span></div>
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={act(() => {
                    setActiveTabId('schematic');
                    dispatch({ type: 'SELECT', name: comp.name, additive: false });
                    actions.openProperties(comp.name);
                    setRightSidebarOpen(true);
                    focusCanvasAt(comp.position[0], comp.position[1]);
                  })}
                >
                  <span>🎯 Locate on Schematic</span>
                </button>
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={act(() => handleRenameComponent(comp.name))}
                >
                  <span>✏️ Rename Component...</span>
                </button>
              </>
            )}
          </div>
        );
      })()}

      {/* Main Workspace (3-Column Layout) */}
      <div className="workspace">
        {/* Left Sidebar: Component & Snippets Palette (Visible in Schematic view) */}
        {activeTabId === 'schematic' && (
          <aside
            className={`sidebar left ${leftSidebarOpen ? '' : 'collapsed'}`}
            onClick={() => {
              if (!leftSidebarOpen) setLeftSidebarOpen(true);
            }}
          >
            {leftSidebarOpen ? (
              <Palette
                catalog={catalog}
                onArm={actions.arm}
                onCollapse={() => setLeftSidebarOpen(false)}
                snippets={appSettings.features.snippets ? allSnippets : undefined}
                selectedCount={state.selection.length}
                onSaveSelectionAsSnippet={() => setSaveSnippetModalOpen(true)}
                onArmSnippet={(snip) => void actions.armSnippet(snip)}
                onDeleteSnippet={(id) => {
                  setUserSnippets(deleteUserSnippet(id));
                  dispatch({ type: 'STATUS', status: 'Snippet deleted' });
                }}
                onExportSnippets={() => {
                  const json = exportSnippetsJson(userSnippets);
                  const blob = new Blob([json], { type: 'application/json' });
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = 'gecko-snippets.json';
                  a.click();
                  URL.revokeObjectURL(url);
                  dispatch({ type: 'STATUS', status: 'Exported snippets JSON' });
                }}
                onImportSnippets={(rawJson) => {
                  const res = importSnippetsJson(rawJson);
                  if (res.error) {
                    dispatch({ type: 'STATUS', status: `⚠️ Snippet import failed: ${res.error}` });
                  } else {
                    setUserSnippets(res.snippets);
                    dispatch({ type: 'STATUS', status: `Imported ${res.importedCount} snippet(s)` });
                  }
                }}
              />
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
          <div
            ref={splitContainerRef}
            data-testid="workspace-split-container"
            className={`workspace-split-container${isResizingDrawer ? ' resizing' : ''}`}
            style={{ display: isScriptActive ? 'none' : 'flex' }}
          >
          {/* Schematic Workspace Pane */}
          <div
            className="workspace-tab-pane"
            data-testid="workspace-pane-schematic"
            style={{
              display: activeTabId === 'schematic' ? 'flex' : 'none',
              flexDirection: 'column',
              flex: isDockedScopeVisible ? `${1 - drawerRatio} 1 0` : '1 1 0',
              minWidth: 0,
              minHeight: 0,
            }}
          >
            <Sheet
              state={state}
              dispatch={dispatch}
              focusTarget={focusTarget}
              simResults={effectiveSimResults}
              sweepPreviewLabel={sweepPreviewLabel}
              showOperatingPointBadges={appSettings.schematic.showOperatingPointBadges}
              onToggleOperatingPointBadges={() =>
                updateAppSettings({
                  ...appSettings,
                  schematic: {
                    ...appSettings.schematic,
                    showOperatingPointBadges: !appSettings.schematic.showOperatingPointBadges,
                  },
                })
              }
              showGrid={appSettings.schematic.showGrid}
              onToggleGrid={() =>
                updateAppSettings({
                  ...appSettings,
                  schematic: {
                    ...appSettings.schematic,
                    showGrid: !appSettings.schematic.showGrid,
                  },
                })
              }
              showProbeMarkers={appSettings.schematic.showProbeMarkers}
              onToggleProbeMarkers={() =>
                updateAppSettings({
                  ...appSettings,
                  schematic: {
                    ...appSettings.schematic,
                    showProbeMarkers: !appSettings.schematic.showProbeMarkers,
                  },
                })
              }
              netLabelMode={appSettings.schematic.netLabelMode}
              onCycleNetLabelMode={() => {
                const cur = appSettings.schematic.netLabelMode;
                const next = cur === 'smart' ? 'all' : cur === 'all' ? 'hover' : 'smart';
                updateAppSettings({
                  ...appSettings,
                  schematic: { ...appSettings.schematic, netLabelMode: next },
                });
              }}
              isSimStale={
                simState.results !== null &&
                simState.lastSimModelVersion !== null &&
                simState.lastSimModelVersion !== state.modelVersion
              }
              actions={{
                placeGhost: actions.placeGhost,
                placeSnippet: actions.placeSnippet,
                onSaveAsSnippet: appSettings.features.snippets
                  ? () => setSaveSnippetModalOpen(true)
                  : undefined,
                finishWire: actions.finishWire,
                commitMove: actions.commitMove,
                resizeComponent: actions.resizeComponent,
                deleteSelection: actions.deleteSelection,
                duplicateSelection: actions.duplicateSelection,
                rotateComponent: actions.rotateComponent,
                deleteComponent: actions.deleteComponent,
                deleteWire: actions.deleteWire,
                patchWirePoints: actions.patchWirePoints,
                flipWire: actions.flipWire,
                labelWire: actions.labelWire,
                openProperties: (name: string) => {
                  actions.openProperties(name);
                  setRightSidebarOpen(true);
                },
                renameComponent: handleRenameComponent,
                setParameter: actions.setParameter,
                openScopeTab: (name: string) => {
                  if (activeTabId === 'schematic') {
                    setSelectedScope(name);
                    setWaveformsDrawerOpen(true);
                  } else {
                    openScopeTab(name);
                  }
                },
                openScriptTab: (name: string) => openScriptTab(name),
                setLabel: actions.setLabel,
                toggleWireMode: actions.toggleWireMode,
                setSelectMode: actions.cancel,
                armQuickComponent,
                toggleProbeMode: actions.toggleProbeMode,
                runSimulation: handleQuickRun,
                runDrcCheck: () => setDiagnosticsOpen(true),
                openSimConfig: () => setSimConfigOpen(true),
                addSignalToScope: actions.addSignalToScope,
                removeSignalFromScope: actions.removeSignalFromScope,
                setProbeRefNet: actions.setProbeRefNet,
                openCommandPalette: () => setCommandPaletteOpen(true),
              }}
            />

            {/* Quick Status Bar */}
            <div className="status-bar">
              <div className="status-left">
                <span
                  className="status-circuit-name"
                  title={state.filePath || state.filename || 'No circuit open'}
                >
                  {state.filename ? `${isDirty ? '*' : ''}${state.filename}` : 'No circuit open'}
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
                <DiagnosticsBadge
                  diagnostics={diagnostics}
                  isOpen={diagnosticsOpen}
                  onToggle={() => setDiagnosticsOpen((prev) => !prev)}
                  compact
                  testId="statusbar-diagnostics-badge"
                />
                <span className="status-components-count">
                  {state.components.length} components, {state.wires.length} wires
                </span>
              </div>
            </div>
          </div>

          {isDockedScopeVisible && (
            <div
              className="workspace-split-resizer horizontal"
              data-testid="workspace-split-resizer"
              onMouseDown={handleStartResizeDrawer}
              onDoubleClick={() => setDrawerRatio(0.42)}
              title="Drag to resize Waveforms panel (double-click to reset)"
            />
          )}

          {/* Simulation & Scope Workspace Pane */}
          <div
            className="workspace-tab-pane"
            data-testid="workspace-pane-scope"
            style={{
              display: showScopePane ? 'flex' : 'none',
              flexDirection: 'column',
              flex: isDockedScopeVisible ? `${drawerRatio} 1 0` : '1 1 0',
              minWidth: 0,
              minHeight: 0,
            }}
          >
            <ScopeViewTab
              selectedScope={drawerGraphRegister?.name ?? activeSelectedScope}
              components={state.components}
              wires={state.wires}
              results={effectiveSimResults}
              displayLayout={currentDisplayLayout}
              onDisplayLayoutChange={handleDisplayLayoutChange}
              theme={theme}
              status={simState.status}
              errorMessage={simState.errorMessage}
              warnings={simState.warnings}
              filename={state.filename}
              scope={scope}
              onSetParameter={actions.setParameter}
              onSetLabel={actions.setLabel}
              onRemoveLabel={actions.removeLabel}
              isSidebarOpen={rightSidebarOpen}
              onToggleSidebar={() => setRightSidebarOpen((prev) => !prev)}
              graphRegister={drawerGraphRegister}
              onUpdateGraphRegister={(patch) => {
                if (drawerGraphRegister) {
                  actions.updateGraphRegister(drawerGraphRegister.id, patch);
                }
              }}
              onRunSimulation={handleQuickRun}
              baseline={simState.baseline}
              showBaseline={simState.showBaseline}
              onPinBaseline={() => actions.pinBaseline(undefined, effectiveSimResults)}
              onClearBaseline={actions.clearBaseline}
              onToggleBaselineVisible={actions.toggleBaselineVisible}
              sweepSession={simState.sweepSession}
              activeSweepStepIndex={activeSweepStepIndex}
              onSelectSweepStep={setActiveSweepStepIndex}
              sweepOverlayAll={sweepOverlayAll}
              onToggleSweepOverlayAll={setSweepOverlayAll}
              onOpenSweepModal={() => setSweepModalOpen(true)}
              onClearSweep={() => {
                actions.clearParametricSweep();
                setActiveSweepStepIndex(null);
                setSweepOverlayAll(false);
              }}
              onApplySweepValue={(compName, paramKey, val) => {
                actions.setParameter(compName, paramKey, val);
                actions.clearParametricSweep();
                setActiveSweepStepIndex(null);
                setSweepOverlayAll(false);
                dispatch({ type: 'STATUS', status: `Applied ${compName}.${paramKey} = ${val}` });
              }}
              pinnedSliders={simState.pinnedSliders}
              onPinSlider={actions.pinSlider}
              onUnpinSlider={actions.unpinSlider}
              onUpdateSliderConfig={actions.updateSliderConfig}
              onCommitSliderValue={(compName, paramKey, val, autoRun) =>
                actions.commitSliderValue(compName, paramKey, val, autoRun, {
                  simulationTime: effectiveSim.tEnd,
                  timeStep: effectiveSim.dt,
                  solverType: effectiveSim.solver,
                })
              }
              tuneBarOpen={tuneBarOpen}
              onToggleTuneBar={() => setTuneBarOpen((prev) => !prev)}
              features={appSettings.features}
              hideDuplicatesAndSpeculative={appSettings.scope.hideDuplicateAndSpeculativeSignals}
              showQuickMeasureStrip={appSettings.scope.showQuickMeasureStrip}
              onHighlightComponent={(name) => {
                if (name) {
                  dispatch({ type: 'SELECT', name, additive: false });
                }
              }}
              onHighlightNet={(netLabel) => {
                if (netLabel) {
                  const matchingWire = state.wires.find((w) => w.label?.trim() === netLabel);
                  if (matchingWire) {
                    dispatch({ type: 'SELECT_WIRE', index: matchingWire.index });
                  }
                }
              }}
              onCollapseDrawer={
                isDockedScopeVisible ? () => setWaveformsDrawerOpen(false) : undefined
              }
              onExpandToTab={
                isDockedScopeVisible
                  ? () => openScopeTab(drawerGraphRegister?.name || activeSelectedScope || 'Osci 1')
                  : undefined
              }
            />
          </div>
          </div>

          {/* Dedicated Full-Viewport Script IDE Pane */}
          <div
            className="workspace-tab-pane"
            style={{
              display: activeTabId.startsWith(SCRIPT_TAB_PREFIX) ? 'flex' : 'none',
              flexDirection: 'column',
              width: '100%',
              height: '100%',
              flex: 1,
              minHeight: 0,
            }}
          >
            <ScriptViewTab
              component={activeScriptComponent}
              allComponents={state.components}
              wires={state.wires}
              scriptDebug={{
                status: simState.status,
                breakpoints: simState.scriptBreakpoints,
                debugPause: simState.debugPause,
                debugState: simState.debugState,
                onToggleBreakpoint: actions.toggleScriptBreakpoint,
                onDebugResume: actions.debugResume,
                onDebugStep: actions.debugStep,
              }}
              onSetParameter={actions.setParameter}
              onSetLabel={actions.setLabel}
              theme={theme}
            />
          </div>

          <DiagnosticsDrawer
            diagnostics={diagnostics}
            isOpen={diagnosticsOpen}
            onClose={() => setDiagnosticsOpen(false)}
            onLocate={handleLocateDiagnostic}
            onQuickFix={handleQuickFixDiagnostic}
            onRunCheck={() => setDiagnosticsOpen(true)}
          />
        </main>

        {/* Right Sidebar: Shown in Schematic and Scope tabs (Script tab has full internal IDE layout) */}
        {!activeTabId.startsWith(SCRIPT_TAB_PREFIX) && (
          <>
            {/* Right Sidebar Resizer Handle (Active only when expanded) */}
            {rightSidebarOpen && (
              <div
                className={`sidebar-resizer right-resizer ${isResizingRight ? 'resizing' : ''}`}
                onMouseDown={handleStartResizeRight}
                title="Drag to resize properties panel"
              />
            )}

            {/* Right Sidebar: Component Properties or Simulation Properties Panel */}
            <aside
              className={`sidebar right ${rightSidebarOpen ? '' : 'collapsed'} ${isResizingRight ? 'resizing' : ''}`}
              style={{ width: rightSidebarOpen ? rightSidebarWidth : undefined }}
              onClick={() => {
                if (!rightSidebarOpen) setRightSidebarOpen(true);
              }}
            >
              {rightSidebarOpen ? (
                <>
                  <div
                    style={{
                      display: activeTabId === 'schematic' ? 'contents' : 'none',
                    }}
                  >
                    <PropertiesPanel
                      component={
                        state.components.find(
                          (c) =>
                            c.name === state.panelFor ||
                            (state.selection.length === 1 && c.name === state.selection[0]),
                        ) || null
                      }
                      allComponents={state.components}
                      wires={state.wires}
                      graphRegisters={state.graphRegisters}
                      activeGraphRegisterId={activeGraphRegister?.id}
                      onUpdateGraphRegister={(id, patch) => actions.updateGraphRegister(id, patch)}
                      onAddGraphRegister={(init) => actions.addGraphRegister(init)}
                      scriptDebug={{
                        status: simState.status,
                        breakpoints: simState.scriptBreakpoints,
                        debugPause: simState.debugPause,
                        debugState: simState.debugState,
                        onToggleBreakpoint: actions.toggleScriptBreakpoint,
                        onDebugResume: actions.debugResume,
                        onDebugStep: actions.debugStep,
                      }}
                      onRename={actions.rename}
                      onSetParameter={actions.setParameter}
                      onSetLabel={actions.setLabel}
                      onRemoveLabel={actions.removeLabel}
                      onSelectComponent={(targetName) => {
                        dispatch({ type: 'SELECT', name: targetName, additive: false });
                        dispatch({ type: 'PANEL_FOR', name: targetName });
                      }}
                      onRotate={actions.rotateComponent}
                      onDelete={actions.deleteComponent}
                      onOpenScopeTab={(name) => openScopeTab(name)}
                      onCollapse={() => setRightSidebarOpen(false)}
                      onPinLiveSlider={
                        appSettings.features.liveSliders
                          ? (compName, paramKey) => {
                              actions.pinSlider(compName, paramKey);
                              setTuneBarOpen(true);
                              if (activeTabId === 'schematic') {
                                setWaveformsDrawerOpen(true);
                              }
                            }
                          : undefined
                      }
                      onOpenSweepForComponent={
                        appSettings.features.parametricSweep
                          ? (compName, paramKey) => {
                              setSweepPrefill({ componentName: compName, paramKey });
                              setSweepModalOpen(true);
                            }
                          : undefined
                      }
                      onAttachNativeLibrary={actions.attachNativeLibrary}
                      theme={theme}
                    />
                  </div>

                  {activeTabId !== 'schematic' &&
                    ((activeGraphRegister || activeScopeComponent) ? (
                      <ScopePropertiesPanel
                        scopeComponent={activeScopeComponent}
                        graphRegister={activeGraphRegister}
                        onUpdateGraphRegister={(patch) => {
                          if (activeGraphRegister) {
                            actions.updateGraphRegister(activeGraphRegister.id, patch);
                          }
                        }}
                        allComponents={state.components}
                        wires={state.wires}
                        results={simState.results}
                        displayLayout={currentDisplayLayout}
                        onDisplayLayoutChange={handleDisplayLayoutChange}
                        scope={scope}
                        onSetParameter={actions.setParameter}
                        onSetLabel={actions.setLabel}
                        onRemoveLabel={actions.removeLabel}
                        onCollapse={() => setRightSidebarOpen(false)}
                        hideDuplicatesAndSpeculative={
                          appSettings.scope.hideDuplicateAndSpeculativeSignals
                        }
                      />
                    ) : (
                      <SimulationPropertiesPanel
                        circuitId={state.circuitId}
                        status={simState.status}
                        progress={simState.progress}
                        defaults={simState.defaults}
                        settings={simState.simSettings}
                        onSettingsChange={actions.updateSimSettings}
                        errorMessage={simState.errorMessage}
                        engineWarnings={simState.warnings}
                        components={state.components}
                        wires={state.wires}
                        results={simState.results}
                        selectedScope={activeSelectedScope}
                        onSelectScope={(sc) => openScopeTab(sc)}
                        displayLayout={displayLayout}
                        onDisplayLayoutChange={handleDisplayLayoutChange}
                        scope={scope}
                        onRunSimulation={actions.runSimulation}
                        onPauseSimulation={actions.pauseSimulation}
                        onResumeSimulation={actions.resumeSimulation}
                        onCancelSimulation={actions.cancelSimulation}
                        onCollapse={() => setRightSidebarOpen(false)}
                      />
                    ))}
                </>
              ) : (
                <div className="collapsed-strip" title="Click to expand Properties (Ctrl+I)">
                  <button
                    type="button"
                    className="sidebar-toggle-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      setRightSidebarOpen(true);
                    }}
                    title="Expand Properties (Ctrl+I)"
                  >
                    ◀
                  </button>
                  <span className="collapsed-strip-icon">⚙</span>
                  <span className="collapsed-strip-text">
                    {activeTabId === 'schematic'
                      ? 'PROPERTIES'
                      : activeGraphRegister
                        ? `OSCI: ${activeGraphRegister.name}`
                        : activeScopeComponent
                          ? `SCOPE: ${activeScopeComponent.name}`
                          : 'SIMULATION'}
                  </span>
                </div>
              )}
            </aside>
          </>
        )}
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
        snippets={appSettings.features.snippets ? allSnippets : undefined}
        onSelectSnippet={(snip) => {
          void actions.armSnippet(snip);
          setCommandPaletteOpen(false);
        }}
        hasSelection={state.selection.length > 0}
        onSaveAsSnippet={
          appSettings.features.snippets ? () => setSaveSnippetModalOpen(true) : undefined
        }
      />

      {/* Settings & Feature Toggles Modal (Ctrl+,) */}
      <SettingsModal
        isOpen={settingsOpen}
        onClose={() => setSettingsOpen(false)}
        settings={appSettings}
        onUpdateSettings={updateAppSettings}
        onResetLayout={() => {
          resetWorkspaceLayoutStorage();
          setLeftSidebarOpen(true);
          setRightSidebarOpen(true);
          setRightSidebarWidth(320);
          setDrawerRatio(0.42);
          dispatch({ type: 'STATUS', status: 'Reset workspace panel layout' });
        }}
      />

      {/* Simulation Configuration Modal */}
      <SimConfigModal
        isOpen={simConfigOpen}
        onClose={() => setSimConfigOpen(false)}
        circuitId={state.circuitId}
        defaults={simState.defaults}
        settings={simState.simSettings}
        onSettingsChange={actions.updateSimSettings}
        onRunSimulation={actions.runSimulation}
        components={state.components}
        wires={state.wires}
        engineWarnings={simState.warnings}
      />

      {/* Parametric Sweep Modal */}
      <ParametricSweepModal
        isOpen={sweepModalOpen}
        onClose={() => {
          setSweepModalOpen(false);
          setSweepPrefill(null);
        }}
        circuitId={state.circuitId}
        components={state.components}
        defaults={simState.defaults}
        settings={simState.simSettings}
        sweepSession={simState.sweepSession}
        initialComponentName={sweepPrefill?.componentName}
        initialParamKey={sweepPrefill?.paramKey}
        onStartSweep={actions.runParametricSweep}
        onCancelSweep={actions.cancelParametricSweep}
      />

      {/* Save Selection as Snippet Modal */}
      {(() => {
        const preview = saveSnippetModalOpen
          ? extractSnippetFromSelection(
              'preview',
              'Custom',
              '',
              state.selection,
              state.components,
              state.wires,
              state.selectedWires,
            )
          : null;
        return (
          <SaveSnippetModal
            isOpen={saveSnippetModalOpen}
            onClose={() => setSaveSnippetModalOpen(false)}
            selectedComponentsCount={preview?.components.length ?? 0}
            selectedWiresCount={preview?.wires.length ?? 0}
            internalCouplingsCount={preview?.couplings.length ?? 0}
            onSave={({ name, category, description }) => {
              const created = extractSnippetFromSelection(
                name,
                category,
                description,
                state.selection,
                state.components,
                state.wires,
                state.selectedWires,
              );
              if (created) {
                setUserSnippets(saveUserSnippet(created));
                dispatch({ type: 'STATUS', status: `Saved snippet '${created.name}'` });
              }
            }}
          />
        );
      })()}

      {/* Keyboard Shortcuts Help Modal */}
      {shortcutsHelpOpen && (
        <div
          className="modal-backdrop"
          onClick={(e) => {
            if (e.target === e.currentTarget) {
              setShortcutsHelpOpen(false);
            }
          }}
        >
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

      {/* Save As Modal Dialog */}
      <SaveAsModal
        isOpen={saveAsOpen}
        defaultFilename={state.filename || 'circuit.ipes'}
        onSave={(filename) => actions.saveAs(filename)}
        onClose={() => setSaveAsOpen(false)}
      />

      {/* Examples Library Modal Dialog */}
      <ExamplesModal
        isOpen={examplesModalOpen}
        onClose={() => setExamplesModalOpen(false)}
        onSelectExample={handleSelectExample}
        onAddNewExample={() => setSaveAsExampleOpen(true)}
        canUpdateContent={!!state.circuitId}
        onGetActiveContent={handleGetActiveContent}
      />

      {/* Save as Example Modal Dialog */}
      <SaveAsExampleModal
        isOpen={saveAsExampleOpen}
        defaultName={activeExample?.name || state.filename || 'Custom Circuit'}
        activeExampleId={activeExample?.id}
        onClose={() => setSaveAsExampleOpen(false)}
        onSave={handleSaveAsExample}
      />
    </div>
  );
}
