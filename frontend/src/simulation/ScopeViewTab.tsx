/**
 * Full-Viewport Scope Instrument & Simulation View Tab.
 * Provides high-resolution waveform plotting, simulation configuration,
 * cursor measurements, stacked / overlay modes, channel toggles, and signal metrics.
 * Fully styled for both Dark and Light themes.
 */
import { useState, useMemo, useEffect, useRef, useCallback } from 'react';
import type { MouseEvent as ReactMouseEvent } from 'react';
import type { EditorComponent, EditorWire, SimulationStatus, GraphRegister } from '../model/types';
import { formatEngineeringValue } from '../model/componentSchema';
import {
  effectiveWindow,
  panWindow,
  zoomWindow,
  clampWindow,
  MIN_SPAN,
} from './viewWindow';
import { FftPanel } from './FftPanel';
import { LossPanel } from './LossPanel';
import type { ScopeController } from './useScopeController';
import { useScopeController } from './useScopeController';
import { ScopeChannelManager } from './ScopeChannelManager';
import { isVoltmeterComponent, isAmmeterComponent } from '../model/componentSchema';
import { isGroundSignal } from './chartData';
import { SCOPE_SETTING_KEYS, SENTINEL_UNSET } from '../model/constants';
import type { ScopeDisplayLayout } from '../model/constants';
import {
  parseSignalGraphs,
  serializeSignalGraphs,
  defaultGraphForSignal,
  resolveSignalGraph,
} from './scopes';
import {
  buildTraceSvgPath,
  compareSignalStats,
  formatDeltaBadge,
  type BaselineSnapshot,
} from './baselineCompare';
import { SweepPanel } from './SweepPanel';
import { sweepRunColor, type SweepSession } from './parametricSweep';
import { LiveSliderBar } from './LiveSliderBar';
import type { PinnedSlider } from './liveSliders';

export interface SweepOverlayTrace {
  runIndex: number;
  label: string;
  color: string;
  time: number[];
  signals: Record<string, number[]>;
  highlighted: boolean;
}

/**
 * Properties for the {@link ScopeViewTab} full-viewport oscilloscope view.
 */
export interface ScopeViewTabProps {
  /** Identifier of the selected scope ('all' or specific scope block name). */
  selectedScope: string;
  /** Schematic components. */
  components: EditorComponent[];
  /** Results time series dictionary. */
  results: Record<string, number[]> | null;
  /** Waveform chart display layout ('overlay', '2-graphs', '3-graphs', or 'stacked'). */
  displayLayout: ScopeDisplayLayout;
  /** UI theme ('dark' or 'light'). */
  theme?: 'dark' | 'light';
  /** Callback to change display layout. */
  onDisplayLayoutChange?: (layout: ScopeDisplayLayout) => void;
  /** Optional current simulation execution status. */
  status?: SimulationStatus | null;
  /** Optional simulation error message when a run fails. */
  errorMessage?: string | null;
  /** Optional engine warnings from the simulation run. */
  warnings?: string[];
  /** Optional loaded circuit filename. */
  filename?: string | null;
  /** Optional scope controller instance. */
  scope?: ScopeController;
  /** Optional callback to persist scope parameters to circuit components. */
  onSetParameter?: (name: string, key: string, value: number | string | boolean) => void;
  /** Optional callback to set a terminal/channel label on a component. */
  onSetLabel?: (name: string, side: 'x' | 'y', index: number, label: string) => void;
  /** Optional callback to remove a terminal/channel label from a component. */
  onRemoveLabel?: (name: string, side: 'x' | 'y', index: number) => void;
  /** Whether the right sidebar is currently open. */
  isSidebarOpen?: boolean;
  /** Callback to toggle the right sidebar for full-width focus mode. */
  onToggleSidebar?: () => void;
  /** Optional active project-level Graph Register. */
  graphRegister?: GraphRegister | null;
  /** Optional callback to update the active graph register. */
  onUpdateGraphRegister?: (patch: Partial<GraphRegister>) => void;
  /** Optional callback to trigger a simulation run. */
  onRunSimulation?: () => void;
  /** Schematic wires for net discovery and active net highlighting. */
  wires?: EditorWire[];
  /** Optional hover callback to highlight a schematic component. */
  onHighlightComponent?: (componentName: string | null) => void;
  /** Optional hover callback to highlight an electrical net. */
  onHighlightNet?: (netLabelOrId: string | null) => void;
  /** Optional pinned baseline snapshot for before/after waveform comparison. */
  baseline?: BaselineSnapshot | null;
  /** Whether pinned baseline ghost traces and deltas are currently visible. */
  showBaseline?: boolean;
  /** Callback to pin current results as comparison baseline. */
  onPinBaseline?: () => void;
  /** Callback to clear the pinned comparison baseline. */
  onClearBaseline?: () => void;
  /** Callback to toggle visibility of the comparison baseline. */
  onToggleBaselineVisible?: () => void;
  /** Active or completed parametric sweep session. */
  sweepSession?: SweepSession | null;
  /** Controlled active sweep step index (`null` or `'all'` handled via `sweepOverlayAll`). */
  activeSweepStepIndex?: number | null;
  /** Selects a specific completed sweep run index so all channels & schematic update together. */
  onSelectSweepStep?: (runIndex: number | null) => void;
  /** Controlled toggle to overlay all sweep runs across all visible channels. */
  sweepOverlayAll?: boolean;
  /** Toggles overlaying all sweep runs across every visible channel. */
  onToggleSweepOverlayAll?: (enabled: boolean) => void;
  /** Opens the Parametric Sweep configuration modal. */
  onOpenSweepModal?: () => void;
  /** Clears the active parametric sweep overlay and results. */
  onClearSweep?: () => void;
  /** Applies a parameter value from a sweep row back onto the schematic component. */
  onApplySweepValue?: (componentName: string, paramKey: string, value: number) => void;
  /** Pinned sliders in the "What-If" Live Tuner bar. */
  pinnedSliders?: PinnedSlider[];
  /** Pins a component parameter as a live slider. */
  onPinSlider?: (componentName: string, paramKey: string) => void;
  /** Unpins a live slider by its `${componentName}.${paramKey}` id. */
  onUnpinSlider?: (id: string) => void;
  /** Updates slider bounds/scale configuration. */
  onUpdateSliderConfig?: (
    id: string,
    patch: Partial<Pick<PinnedSlider, 'min' | 'max' | 'scale'>>,
  ) => void;
  /** Commits a slider value to the schematic and optionally triggers simulation. */
  onCommitSliderValue?: (
    componentName: string,
    paramKey: string,
    value: number,
    autoRun: boolean,
  ) => void | Promise<void>;
  /** Controlled open state for the Live Parameter Sliders bar. */
  tuneBarOpen?: boolean;
  /** Toggles controlled Live Parameter Sliders bar visibility. */
  onToggleTuneBar?: () => void;
  /** Hides the docked bottom waveforms drawer when shown beneath Schematic. */
  onCollapseDrawer?: () => void;
  /** Expands the docked bottom waveforms drawer into a full-screen Oscilloscope tab. */
  onExpandToTab?: () => void;
}

export const DEFAULT_DRAWER_HEIGHT = 280;
export const MIN_DRAWER_HEIGHT = 140;

/**
 * Binary-searches the sample index closest to time t (time must be sorted in ascending order).
 *
 * @param time Array of timestamps in ascending order.
 * @param t Target timestamp in seconds.
 * @returns Index of the nearest sample.
 */
export function sampleIndexAt(time: number[], t: number): number {
  let low = 0;
  let high = time.length - 1;
  while (low < high) {
    const mid = Math.floor((low + high) / 2);
    if (time[mid] < t) low = mid + 1;
    else high = mid;
  }
  return low;
}

/**
 * Converts browser viewport `(clientX, clientY)` to exact SVG viewBox coordinates,
 * accounting cleanly for SVG `preserveAspectRatio` scaling/letterboxing via `getScreenCTM`.
 */
function clientToSvgCoords(
  svg: SVGSVGElement,
  clientX: number,
  clientY: number,
  vbWidth: number,
  vbHeight: number,
): { x: number; y: number } {
  if (typeof svg.getScreenCTM === 'function') {
    const ctm = svg.getScreenCTM();
    if (ctm && ctm.a !== 0 && ctm.d !== 0) {
      const inv = ctm.inverse();
      return {
        x: inv.a * clientX + inv.c * clientY + inv.e,
        y: inv.b * clientX + inv.d * clientY + inv.f,
      };
    }
  }
  const rect = svg.getBoundingClientRect();
  return {
    x: ((clientX - rect.left) / (rect.width || 1)) * vbWidth,
    y: ((clientY - rect.top) / (rect.height || 1)) * vbHeight,
  };
}

/**
 * Infers physical SI engineering unit from the signal or probe name convention.
 *
 * @param name Signal name (e.g. 'V_out', 'i_inductor', 'Power_loss', 'Temp_junction', 'Phi_Lp').
 * @returns Appropriate SI unit string ('V', 'A', 'W', '°C', 'Wb', or empty string).
 */
export function inferSignalUnit(name: string): string {
  const lower = name.toLowerCase();
  if (lower.startsWith('v') || lower.startsWith('u') || lower.includes('_v') || lower.includes('volt')) return 'V';
  if (lower.startsWith('i') || lower.includes('_i') || lower.includes('curr') || lower.startsWith('amp')) return 'A';
  if (lower.startsWith('p') || lower.includes('watt') || lower.includes('power')) return 'W';
  if (lower.startsWith('temp') || lower.includes('th_') || lower.endsWith('_c') || lower.startsWith('tj')) return '°C';
  if (lower.startsWith('phi')) return 'Wb';
  return '';
}


/** Floating hover tooltip shared by the overlay and stacked charts. */
function WaveformTooltip({
  time,
  hoverIndex,
  activeSignals,
  signals,
  colorOf,
}: {
  time: number[];
  hoverIndex: number;
  activeSignals: string[];
  signals: Record<string, number[]>;
  colorOf: (name: string) => string;
}) {
  return (
    <div className="waveform-tooltip">
      <div className="tooltip-time">
        t = {formatEngineeringValue(time[hoverIndex], 's')}
      </div>
      {activeSignals.map((name) => {
        const val = signals[name]?.[hoverIndex];
        if (val === undefined) return null;
        const color = colorOf(name);
        return (
          <div key={name} className="tooltip-signal-row">
            <span className="tooltip-dot" style={{ backgroundColor: color }} />
            <span className="tooltip-name">{name}:</span>
            <span className="tooltip-val">{formatEngineeringValue(val)}</span>
          </div>
        );
      })}
    </div>
  );
}
/** Responsive container dimensions hook */
function useContainerDimensions<T extends HTMLElement>() {
  const ref = useRef<T | null>(null);
  const [dimensions, setDimensions] = useState<{ width: number; height: number }>({
    width: 1000,
    height: 480,
  });

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const update = () => {
      const rect = el.getBoundingClientRect();
      if (rect.width > 50 && rect.height > 50) {
        setDimensions({
          width: Math.round(rect.width),
          height: Math.round(rect.height),
        });
      }
    };

    update();

    if (typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver((entries) => {
        for (const entry of entries) {
          const { width, height } = entry.contentRect;
          if (width > 50 && height > 50) {
            setDimensions({
              width: Math.round(width),
              height: Math.round(height),
            });
          }
        }
      });
      observer.observe(el);
      return () => observer.disconnect();
    } else {
      window.addEventListener('resize', update);
      return () => window.removeEventListener('resize', update);
    }
  }, []);

  return [ref, dimensions] as const;
}

export function ScopeViewTab({
  selectedScope,
  components,
  results,
  displayLayout,
  onDisplayLayoutChange,
  theme = 'dark',
  status,
  errorMessage,
  warnings = [],
  filename,
  scope,
  onSetParameter,
  onSetLabel,
  onRemoveLabel,
  isSidebarOpen = true,
  onToggleSidebar,
  graphRegister,
  onUpdateGraphRegister,
  onRunSimulation,
  wires,
  onHighlightComponent,
  onHighlightNet,
  baseline = null,
  showBaseline: showBaselineProp,
  onPinBaseline,
  onClearBaseline,
  onToggleBaselineVisible,
  sweepSession = null,
  activeSweepStepIndex: activeSweepStepIndexProp,
  onSelectSweepStep,
  sweepOverlayAll: sweepOverlayAllProp,
  onToggleSweepOverlayAll,
  onOpenSweepModal: _onOpenSweepModal,
  onClearSweep,
  onApplySweepValue,
  pinnedSliders = [],
  onPinSlider,
  onUnpinSlider,
  onUpdateSliderConfig,
  onCommitSliderValue,
  tuneBarOpen: tuneBarOpenProp,
  onToggleTuneBar: _onToggleTuneBar,
  onCollapseDrawer,
  onExpandToTab,
}: ScopeViewTabProps) {
  const [internalShowBaseline, setInternalShowBaseline] = useState(true);
  const isBaselineVisible = showBaselineProp !== undefined ? showBaselineProp : internalShowBaseline;
  const handleToggleBaselineVisible = useCallback(() => {
    if (onToggleBaselineVisible) {
      onToggleBaselineVisible();
    } else {
      setInternalShowBaseline((prev) => !prev);
    }
  }, [onToggleBaselineVisible]);
  const baselineResults = isBaselineVisible && baseline ? baseline.results : null;

  const [internalSweepOverlayAll, setInternalSweepOverlayAll] = useState(false);
  const [internalSweepStepIndex, setInternalSweepStepIndex] = useState<number | null>(null);
  const [sweepSelectedSignal, setSweepSelectedSignal] = useState('');
  const [sweepHighlightedRun, setSweepHighlightedRun] = useState<number | null>(null);
  const tuneBarOpen = tuneBarOpenProp ?? false;

  const completedSweepRuns = useMemo(
    () =>
      sweepSession?.runs.filter((r) => r.status === 'COMPLETED' && Boolean(r.results)) ?? [],
    [sweepSession],
  );

  const sweepOverlayAll =
    sweepOverlayAllProp !== undefined ? sweepOverlayAllProp : internalSweepOverlayAll;
  const setSweepOverlayAll = useCallback(
    (enabled: boolean) => {
      if (onToggleSweepOverlayAll) {
        onToggleSweepOverlayAll(enabled);
      } else {
        setInternalSweepOverlayAll(enabled);
      }
    },
    [onToggleSweepOverlayAll],
  );

  const activeSweepStepIndex =
    activeSweepStepIndexProp !== undefined
      ? activeSweepStepIndexProp
      : (internalSweepStepIndex ?? completedSweepRuns[0]?.index ?? null);
  const setActiveSweepStepIndex = useCallback(
    (idx: number | null) => {
      if (onSelectSweepStep) {
        onSelectSweepStep(idx);
      } else {
        setInternalSweepStepIndex(idx);
      }
    },
    [onSelectSweepStep],
  );

  const activeSweepRun = useMemo(() => {
    if (!sweepSession || activeSweepStepIndex === null) return null;
    return (
      completedSweepRuns.find((r) => r.index === activeSweepStepIndex) ??
      completedSweepRuns[0] ??
      null
    );
  }, [sweepSession, activeSweepStepIndex, completedSweepRuns]);

  const effectiveResults =
    !sweepOverlayAll && activeSweepRun?.results ? activeSweepRun.results : results;

  const fallbackScope = useScopeController({
    results: effectiveResults,
    components,
    selectedScope,
    graphRegister,
    theme,
    onSetParameter,
    onUpdateGraphRegister,
  });
  const ctrl = scope ?? fallbackScope;

  const {
    view,
    setView,
    timePerDiv,
    dataT0,
    dataT1,
    yScaleMode,
    setYScaleMode,
    yView,
    setYView,
    signalNames,
    timeArray,
    signalStats,
    scopeChannelNames,
    hiddenSignals,
    toggleSignal,
    colorOf,
    cursorsEnabled,
    setCursorsEnabled,
    cursorA,
    setCursorA,
    cursorB,
    setCursorB,
    activeCursor,
    setActiveCursor,
    setCursor,
    drawerTab,
    setDrawerTab,
  } = ctrl;

  const [isChannelManagerOpen, setIsChannelManagerOpen] = useState(false);

  const activeScopeBlock = useMemo(() => {
    if (selectedScope === 'all') return null;
    return components.find((c) => c.name === selectedScope) || null;
  }, [components, selectedScope]);

  const effectiveDisplayLayout: ScopeDisplayLayout = graphRegister
    ? graphRegister.numGraphs === 1
      ? 'overlay'
      : graphRegister.numGraphs === 2
        ? '2-graphs'
        : '3-graphs'
    : displayLayout;

  const assignedChannels = useMemo(() => {
    if (graphRegister) return graphRegister.signals;
    if (!activeScopeBlock) return [];
    return (activeScopeBlock.inputLabels || []).filter(
      (lbl) => lbl && lbl.trim() && lbl !== SENTINEL_UNSET,
    );
  }, [graphRegister, activeScopeBlock]);

  const handleAddChannel = useCallback(
    (signalName: string, targetGraph?: number) => {
      if (graphRegister) {
        if (graphRegister.signals.includes(signalName)) return;
        const nextSignals = [...graphRegister.signals, signalName];
        const assignedGraph =
          targetGraph && targetGraph >= 1 && targetGraph <= 3
            ? (targetGraph as 1 | 2 | 3)
            : ((graphRegister.signalGraphs[signalName] ||
                defaultGraphForSignal(signalName, effectiveDisplayLayout)) as 1 | 2 | 3);
        const nextGraphs: Record<string, 1 | 2 | 3> = {
          ...graphRegister.signalGraphs,
          [signalName]: assignedGraph,
        };
        onUpdateGraphRegister?.({ signals: nextSignals, signalGraphs: nextGraphs });
        return;
      }
      if (!activeScopeBlock || !onSetLabel) return;
      const currentLabels = activeScopeBlock.inputLabels || [];
      if (currentLabels.includes(signalName)) return;
      const nextIdx = currentLabels.length;
      onSetLabel(activeScopeBlock.name, 'x', nextIdx, signalName);
    },
    [graphRegister, onUpdateGraphRegister, effectiveDisplayLayout, activeScopeBlock, onSetLabel],
  );

  const handleRemoveChannel = useCallback(
    (signalName: string) => {
      if (graphRegister) {
        const nextSignals = graphRegister.signals.filter((s) => s !== signalName);
        const nextHidden = graphRegister.hiddenSignals.filter((s) => s !== signalName);
        onUpdateGraphRegister?.({ signals: nextSignals, hiddenSignals: nextHidden });
        return;
      }
      if (!activeScopeBlock || !onRemoveLabel) return;
      const currentLabels = activeScopeBlock.inputLabels || [];
      const idx = currentLabels.indexOf(signalName);
      if (idx >= 0) {
        onRemoveLabel(activeScopeBlock.name, 'x', idx);
      }
    },
    [graphRegister, onUpdateGraphRegister, activeScopeBlock, onRemoveLabel],
  );

  const handleAutoAddProbes = useCallback(() => {
    const probeSignals: string[] = [];

    // Find all measurement probes and labeled outputs from components
    components.forEach((comp) => {
      if (isVoltmeterComponent(comp) || isAmmeterComponent(comp)) {
        comp.outputLabels?.forEach((lbl) => {
          if (lbl && lbl.trim() && lbl !== SENTINEL_UNSET && !isGroundSignal(lbl.trim()) && !probeSignals.includes(lbl.trim())) {
            probeSignals.push(lbl.trim());
          }
        });
      }
    });

    // Also include signals from results matching common voltage/current probe patterns
    ctrl.signalNames.forEach((sig) => {
      const lower = sig.toLowerCase();
      if ((lower.startsWith('u_') || lower.startsWith('v_') || lower.startsWith('i_')) && !isGroundSignal(sig) && !probeSignals.includes(sig)) {
        probeSignals.push(sig);
      }
    });

    if (graphRegister) {
      const nextSignals = Array.from(new Set([...graphRegister.signals, ...probeSignals]));
      onUpdateGraphRegister?.({ signals: nextSignals });
      return;
    }

    if (!activeScopeBlock || !onSetLabel) return;
    const currentLabels = [...(activeScopeBlock.inputLabels || [])];
    let nextIdx = currentLabels.length;
    probeSignals.forEach((sig) => {
      if (!currentLabels.includes(sig)) {
        onSetLabel(activeScopeBlock.name, 'x', nextIdx, sig);
        currentLabels.push(sig);
        nextIdx++;
      }
    });
  }, [graphRegister, onUpdateGraphRegister, activeScopeBlock, onSetLabel, components, ctrl.signalNames]);

  const handleClearChannels = useCallback(() => {
    if (graphRegister) {
      onUpdateGraphRegister?.({ signals: [], hiddenSignals: [] });
      return;
    }
    if (!activeScopeBlock || !onRemoveLabel) return;
    const currentLabels = activeScopeBlock.inputLabels || [];
    for (let i = currentLabels.length - 1; i > 0; i--) {
      onRemoveLabel(activeScopeBlock.name, 'x', i);
    }
    if (onSetLabel && currentLabels.length > 0) {
      onSetLabel(activeScopeBlock.name, 'x', 0, '');
    }
  }, [graphRegister, onUpdateGraphRegister, activeScopeBlock, onRemoveLabel, onSetLabel]);

  const signalGraphsRaw = activeScopeBlock?.parameters?.[SCOPE_SETTING_KEYS.signalGraphs] as string | undefined;
  const legacySignalGraphs = useMemo(() => parseSignalGraphs(signalGraphsRaw), [signalGraphsRaw]);
  const effectiveSignalGraphs = useMemo(() => {
    if (graphRegister) {
      return graphRegister.signalGraphs;
    }
    return legacySignalGraphs;
  }, [graphRegister, legacySignalGraphs]);
  const signalGraphs = effectiveSignalGraphs;

  const handleMoveSignalToGraph = useCallback(
    (signal: string, targetGraph: number) => {
      if (graphRegister) {
        const next = { ...graphRegister.signalGraphs, [signal]: targetGraph as 1 | 2 | 3 };
        onUpdateGraphRegister?.({ signalGraphs: next });
        return;
      }
      if (!activeScopeBlock || !onSetParameter) return;
      const current = { ...legacySignalGraphs };
      current[signal] = targetGraph;
      const serialized = serializeSignalGraphs(current);
      onSetParameter(activeScopeBlock.name, SCOPE_SETTING_KEYS.signalGraphs, serialized);
    },
    [graphRegister, onUpdateGraphRegister, activeScopeBlock, onSetParameter, legacySignalGraphs],
  );

  const handleAutoGroupSignals = useCallback(() => {
    if (graphRegister) {
      const mapping: Record<string, 1 | 2 | 3> = {};
      const targets = graphRegister.signals;
      for (const sig of targets) {
        mapping[sig] = defaultGraphForSignal(sig, effectiveDisplayLayout) as 1 | 2 | 3;
      }
      onUpdateGraphRegister?.({ signalGraphs: mapping });
      return;
    }
    if (!activeScopeBlock || !onSetParameter) return;
    const mapping: Record<string, number> = {};
    const targets = assignedChannels.length > 0 ? assignedChannels : scopeChannelNames;
    for (const sig of targets) {
      mapping[sig] = defaultGraphForSignal(sig, displayLayout);
    }
    const serialized = serializeSignalGraphs(mapping);
    onSetParameter(activeScopeBlock.name, SCOPE_SETTING_KEYS.signalGraphs, serialized);
  }, [graphRegister, onUpdateGraphRegister, effectiveDisplayLayout, activeScopeBlock, onSetParameter, assignedChannels, scopeChannelNames, displayLayout]);

  const [hoverIndex, setHoverIndex] = useState<number | null>(null);
  const [hoveredSignal, setHoveredSignal] = useState<string | null>(null);
  const [chartContainerRef, chartDimensions] = useContainerDimensions<HTMLDivElement>();

  const win = effectiveWindow(view, dataT0, dataT1);

  const zoomAt = useCallback(
    (factor: number, anchor: number) => {
      setView(zoomWindow(effectiveWindow(view, dataT0, dataT1), factor, anchor, dataT0, dataT1));
    },
    [view, dataT0, dataT1, setView],
  );

  const zoomRange = useCallback(
    (start: number, end: number) => {
      const s = Math.min(start, end);
      const e = Math.max(start, end);
      if (e - s < MIN_SPAN) return;
      setView(clampWindow({ start: s, end: e }, dataT0, dataT1));
    },
    [dataT0, dataT1, setView],
  );

  const resetZoom = useCallback(() => {
    setView(null);
    setYView(null);
  }, [setView, setYView]);

  const filteredChannels = useMemo(() => {
    if (graphRegister) {
      return graphRegister.signals;
    }
    return scopeChannelNames;
  }, [graphRegister, scopeChannelNames]);

  const isSignalHidden = useCallback(
    (sig: string) => {
      if (graphRegister) {
        return graphRegister.hiddenSignals.includes(sig) || Boolean(hiddenSignals[sig]);
      }
      return Boolean(hiddenSignals[sig]);
    },
    [graphRegister, hiddenSignals],
  );

  const visibleSignals = useMemo(() => {
    return filteredChannels.filter((s) => !isSignalHidden(s));
  }, [filteredChannels, isSignalHidden]);

  const baselineComparisons = useMemo(() => {
    if (!baseline || !isBaselineVisible) return {};
    return compareSignalStats(baseline.stats, signalStats, filteredChannels);
  }, [baseline, isBaselineVisible, signalStats, filteredChannels]);

  const hasBaselineDiff = useMemo(
    () =>
      Object.values(baselineComparisons).some(
        (c) =>
          Math.abs(c.mean.absDelta) > 1e-9 ||
          Math.abs(c.rms.absDelta) > 1e-9 ||
          Math.abs(c.pkpk.absDelta) > 1e-9,
      ),
    [baselineComparisons],
  );

  const prevSweepStatusRef = useRef<string | null>(null);
  useEffect(() => {
    const nextStatus = sweepSession?.status ?? null;
    if (nextStatus === 'COMPLETED' && prevSweepStatusRef.current !== 'COMPLETED') {
      const firstRun = sweepSession?.runs.find((r) => r.status === 'COMPLETED');
      if (firstRun) {
        setActiveSweepStepIndex(firstRun.index);
      }
      setSweepHighlightedRun(null);
      if (!sweepSelectedSignal) {
        const firstSig =
          visibleSignals[0] ||
          signalNames[0] ||
          (firstRun ? Object.keys(firstRun.stats)[0] : '');
        if (firstSig) setSweepSelectedSignal(firstSig);
      }
    } else if (nextStatus === null && drawerTab === 'sweep') {
      setDrawerTab(null);
    }
    prevSweepStatusRef.current = nextStatus;
  }, [
    sweepSession,
    drawerTab,
    setDrawerTab,
    sweepSelectedSignal,
    visibleSignals,
    signalNames,
    setActiveSweepStepIndex,
  ]);

  const effectiveSweepSignal =
    sweepSelectedSignal && (signalNames.includes(sweepSelectedSignal) || Boolean(sweepSession?.runs.some((r) => r.stats[sweepSelectedSignal])))
      ? sweepSelectedSignal
      : (visibleSignals[0] || signalNames[0] || '');

  const sweepOverlays = useMemo<SweepOverlayTrace[]>(() => {
    if (!sweepSession || completedSweepRuns.length === 0 || !sweepOverlayAll) return [];
    return completedSweepRuns.map((r) => ({
      runIndex: r.index,
      label: formatEngineeringValue(r.parameterValue, sweepSession.unit),
      color: sweepRunColor(r.index, sweepSession.totalRuns, theme),
      time: r.results!['time'] || [],
      signals: r.results!,
      highlighted: sweepHighlightedRun === r.index || activeSweepStepIndex === r.index,
    }));
  }, [sweepSession, completedSweepRuns, sweepOverlayAll, theme, sweepHighlightedRun, activeSweepStepIndex]);

  const handleToggleSignal = useCallback(
    (sig: string) => {
      toggleSignal(sig);
      if (graphRegister) {
        const isH = graphRegister.hiddenSignals.includes(sig);
        const nextHidden = isH
          ? graphRegister.hiddenSignals.filter((s) => s !== sig)
          : [...graphRegister.hiddenSignals, sig];
        onUpdateGraphRegister?.({ hiddenSignals: nextHidden });
      }
    },
    [toggleSignal, graphRegister, onUpdateGraphRegister],
  );

  const isRunning = status === 'RUNNING' || status === 'PENDING';

  const [scopeContextMenu, setScopeContextMenu] = useState<{
    x: number;
    y: number;
    sampleIdx: number | null;
  } | null>(null);
  const scopeContextMenuRef = useRef<HTMLDivElement | null>(null);
  const [scopeContextLayoutOpen, setScopeContextLayoutOpen] = useState(false);

  useEffect(() => {
    if (!scopeContextMenu) {
      setScopeContextLayoutOpen(false);
      return;
    }
    const handleOutside = (e: MouseEvent) => {
      if (scopeContextMenuRef.current && !scopeContextMenuRef.current.contains(e.target as Node)) {
        setScopeContextMenu(null);
      }
    };
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setScopeContextMenu(null);
    };
    window.addEventListener('mousedown', handleOutside);
    window.addEventListener('keydown', handleKey);
    return () => {
      window.removeEventListener('mousedown', handleOutside);
      window.removeEventListener('keydown', handleKey);
    };
  }, [scopeContextMenu]);

  const placeCursorAtSample = useCallback(
    (type: 'A' | 'B', idx: number | null) => {
      if (setCursor) {
        setCursor(type, idx);
      } else {
        setCursorsEnabled(true);
        if (type === 'A') setCursorA(idx);
        else setCursorB(idx);
      }
      if (graphRegister && idx !== null) {
        onUpdateGraphRegister?.({ cursorsEnabled: true });
      }
    },
    [setCursor, setCursorsEnabled, setCursorA, setCursorB, graphRegister, onUpdateGraphRegister],
  );

  const setScopeLayout = useCallback(
    (layout: ScopeDisplayLayout) => {
      if (graphRegister) {
        const numGraphs = layout === 'overlay' ? 1 : layout === '2-graphs' ? 2 : 3;
        onUpdateGraphRegister?.({ numGraphs });
      }
      onDisplayLayoutChange?.(layout);
    },
    [graphRegister, onUpdateGraphRegister, onDisplayLayoutChange],
  );

  const handleExportCsv = useCallback(() => {
    if (!effectiveResults || timeArray.length === 0) return;
    const cols = visibleSignals.length > 0 ? visibleSignals : signalNames;
    const rows: string[] = [['t', ...cols].join(',')];
    for (let i = 0; i < timeArray.length; i++) {
      rows.push([String(timeArray[i]), ...cols.map((c) => String(effectiveResults[c]?.[i] ?? ''))].join(','));
    }
    const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `${(filename || 'gecko-scope').replace(/\s+/g, '_')}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  }, [effectiveResults, timeArray, visibleSignals, signalNames, filename]);

  const handleOpenChartContextMenu = useCallback(
    (x: number, y: number, sampleIdx: number | null) => {
      setScopeContextMenu({ x, y, sampleIdx });
    },
    [],
  );

  return (
    <div className="scope-view-tab-container">
      {/* Row 1: Scope Title, Channels, Flat Result View Switcher & Window Actions */}
      <div className="scope-tab-header dso-screen-osd">
        <div
          className="scope-tab-title-group"
          style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, flex: '1 1 260px', overflow: 'hidden' }}
        >
          <div className="scope-badge-icon" style={{ flexShrink: 0 }}>
            {selectedScope === 'all' ? '📊' : '📺'}
          </div>
          <span
            className="scope-tab-title"
            style={{ whiteSpace: 'nowrap', flexShrink: 0 }}
            title={filename ? `Circuit: ${filename}` : undefined}
          >
            {graphRegister
              ? (graphRegister.name.startsWith('SCOPE.') ? `Scope: ${graphRegister.name}` : graphRegister.name)
              : (selectedScope === 'all' ? 'All Signals' : `Scope: ${selectedScope}`)}
          </span>

          {/* Active channel pills with color dots */}
          <div
            className="dso-channels-wrap"
            style={{ display: 'flex', alignItems: 'center', gap: 4, overflowX: 'auto', scrollbarWidth: 'none', minWidth: 0, flexShrink: 1 }}
          >
            {filteredChannels.map((sig) => {
              const color = colorOf(sig);
              const isHovered = hoveredSignal === sig;
              const isHidden = isSignalHidden(sig);
              return (
                <button
                  key={sig}
                  type="button"
                  className={`dso-osd-channel-tag ${isHovered ? 'highlighted' : ''}`}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 5,
                    padding: '2px 8px',
                    borderRadius: 4,
                    border: `1px solid ${isHovered ? color : 'var(--border)'}`,
                    background: isHovered
                      ? (theme === 'light' ? 'rgba(226, 232, 240, 0.95)' : 'rgba(51, 65, 85, 0.95)')
                      : 'var(--surface)',
                    cursor: 'pointer',
                    fontSize: 11,
                    fontFamily: 'var(--font-mono, monospace)',
                    whiteSpace: 'nowrap',
                    flexShrink: 0,
                    color: isHovered ? color : 'var(--text)',
                    boxShadow: isHovered ? `0 0 6px ${color}60` : undefined,
                    opacity: isHidden ? 0.45 : 1,
                    textDecoration: isHidden ? 'line-through' : undefined,
                  }}
                  onMouseEnter={() => setHoveredSignal(sig)}
                  onMouseLeave={() => setHoveredSignal(null)}
                  onClick={() => handleToggleSignal(sig)}
                  title={`Click to ${isHidden ? 'show' : 'hide'} ${sig}. Hover to highlight trace.`}
                >
                  <span style={{ width: 6, height: 6, borderRadius: '50%', background: color, display: 'inline-block' }} />
                  <span style={{ fontWeight: isHovered ? 700 : 500 }}>{sig}</span>
                </button>
              );
            })}
          </div>

          {/* Scope Channel Manager button */}
          {(Boolean(graphRegister) || selectedScope !== 'all') && (
            <button
              type="button"
              className="sim-mini-btn"
              onClick={() => setIsChannelManagerOpen(true)}
              title="Add or remove signals on this scope"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
                padding: '2px 8px',
                fontSize: '11px',
                background: 'rgba(56, 189, 248, 0.08)',
                borderColor: 'rgba(56, 189, 248, 0.4)',
                color: 'var(--accent, #38bdf8)',
                fontWeight: 600,
                flexShrink: 0,
                whiteSpace: 'nowrap',
              }}
              data-testid="scope-manage-channels-btn"
            >
              <span>➕</span> Channels ({graphRegister ? graphRegister.signals.length : (assignedChannels.length || scopeChannelNames.length)})
            </button>
          )}
        </div>

        <div className="scope-tab-actions" style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
          {/* Flat Result View Switcher */}
          <div className="segmented-control scope-view-switcher" data-testid="scope-view-switcher">
            <button
              type="button"
              data-testid="scope-view-tab-curves"
              className={`segmented-btn ${drawerTab === null ? 'active' : ''}`}
              onClick={() => setDrawerTab(null)}
              title="Oscilloscope waveform traces"
            >
              📈 Curves
            </button>
            <button
              type="button"
              data-testid="scope-view-tab-metrics"
              className={`segmented-btn ${drawerTab === 'metrics' ? 'active' : ''}`}
              onClick={() => setDrawerTab('metrics')}
              title="Signal statistics table (Min, Max, Pk-Pk, RMS, Mean)"
            >
              📊 Metrics
            </button>
            <button
              type="button"
              data-testid="scope-view-tab-fft"
              className={`segmented-btn ${drawerTab === 'fft' ? 'active' : ''}`}
              onClick={() => setDrawerTab('fft')}
              title="Harmonic FFT frequency spectrum"
            >
              〰 FFT
            </button>
            <button
              type="button"
              data-testid="scope-view-tab-losses"
              className={`segmented-btn ${drawerTab === 'losses' ? 'active' : ''}`}
              onClick={() => setDrawerTab('losses')}
              title="Waveform power (p = v · i) and semiconductor losses"
            >
              ⚡ Losses
            </button>
            {sweepSession && (
              <button
                type="button"
                data-testid="scope-view-tab-sweep"
                className={`segmented-btn ${drawerTab === 'sweep' ? 'active' : ''}`}
                onClick={() => setDrawerTab('sweep')}
                title="Parametric sweep summary trend & CSV export"
              >
                🎚 Sweep
              </button>
            )}
          </div>

          {/* Acquisition Status Pill */}
          <div className={`dso-status-pill ${isRunning ? 'running' : status === 'FAILED' || errorMessage ? 'stop' : effectiveResults ? 'auto' : 'stop'}`}>
            <span className="dso-status-dot" />
            {isRunning ? 'RUNNING' : status === 'FAILED' || errorMessage ? 'FAILED' : effectiveResults ? 'AUTO' : 'STOP'}
          </div>

          {onExpandToTab && (
            <button
              type="button"
              data-testid="expand-waveforms-drawer-btn"
              className="sim-mini-btn"
              onClick={onExpandToTab}
              title="Open full-screen Oscilloscope tab"
              style={{ padding: '2px 6px', fontSize: '11px' }}
            >
              ⤢
            </button>
          )}
          {onCollapseDrawer && (
            <button
              type="button"
              data-testid="collapse-waveforms-drawer-btn"
              className="sim-mini-btn"
              onClick={onCollapseDrawer}
              title="Hide Waveforms panel"
              style={{ padding: '2px 6px', fontSize: '11px' }}
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Row 2: Compact Chart Controls Bar (Layout, Timebase, Fit, Cursors, Save Reference) */}
      {drawerTab === null && (
        <div className="scope-controls-row" data-testid="scope-controls-row">
          <div className="scope-controls-group">
            {graphRegister ? (
              <div className="segmented-control" style={{ display: 'flex' }} data-testid="scope-tab-layout-switcher">
                <button
                  type="button"
                  className={`segmented-btn ${graphRegister.numGraphs === 1 ? 'active' : ''}`}
                  onClick={() => {
                    onUpdateGraphRegister?.({ numGraphs: 1 });
                    onDisplayLayoutChange?.('overlay');
                  }}
                  title="1 Graph: Overlay all traces on a single plot"
                >
                  📈 1 Graph
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${graphRegister.numGraphs === 2 ? 'active' : ''}`}
                  onClick={() => {
                    onUpdateGraphRegister?.({ numGraphs: 2 });
                    onDisplayLayoutChange?.('2-graphs');
                  }}
                  title="2 Graphs: Split traces across 2 subplots"
                >
                  📊 2 Graphs
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${graphRegister.numGraphs === 3 ? 'active' : ''}`}
                  onClick={() => {
                    onUpdateGraphRegister?.({ numGraphs: 3 });
                    onDisplayLayoutChange?.('3-graphs');
                  }}
                  title="3 Graphs: Split traces across 3 subplots"
                >
                  📑 3 Graphs
                </button>
              </div>
            ) : onDisplayLayoutChange && (
              <div className="segmented-control" style={{ display: 'flex' }} data-testid="scope-tab-layout-switcher">
                <button
                  type="button"
                  className={`segmented-btn ${displayLayout === 'overlay' ? 'active' : ''}`}
                  onClick={() => onDisplayLayoutChange('overlay')}
                  title="1 Graph: Overlay all traces on a single plot"
                >
                  📈 1 Graph
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${displayLayout === '2-graphs' ? 'active' : ''}`}
                  onClick={() => onDisplayLayoutChange('2-graphs')}
                  title="2 Graphs: Split traces across 2 subplots"
                >
                  📊 2 Graphs
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${displayLayout === '3-graphs' ? 'active' : ''}`}
                  onClick={() => onDisplayLayoutChange('3-graphs')}
                  title="3 Graphs: Split traces across 3 subplots"
                >
                  📑 3 Graphs
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${displayLayout === 'stacked' ? 'active' : ''}`}
                  onClick={() => onDisplayLayoutChange('stacked')}
                  title="Stacked Lanes: Each trace in its own individual lane"
                >
                  🪜 Stacked
                </button>
              </div>
            )}

            {(effectiveDisplayLayout === '2-graphs' || effectiveDisplayLayout === '3-graphs') && (
              <button
                type="button"
                className="sim-mini-btn"
                onClick={handleAutoGroupSignals}
                title="Auto-organize traces: Voltages -> Graph 1, Currents -> Graph 2, Control -> Graph 3"
                style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '2px 8px', fontSize: '11px' }}
              >
                <span>⚡</span> Auto-Group
              </button>
            )}
          </div>

          <div className="scope-controls-group">
            {/* Timebase Badge */}
            <span
              className="dso-osd-item"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
                fontSize: '11px',
                fontFamily: 'var(--font-mono, monospace)',
                color: 'var(--text-muted)',
                background: 'var(--surface)',
                padding: '2px 7px',
                borderRadius: 4,
                border: '1px solid var(--border)',
                whiteSpace: 'nowrap',
              }}
              title={`Timebase: ${formatEngineeringValue(timePerDiv, 's')}/div (Span: ${formatEngineeringValue(win.end - win.start, 's')})`}
            >
              ⏱ <strong className="dso-osd-val" style={{ color: 'var(--text)' }}>{formatEngineeringValue(timePerDiv, 's')}/div</strong>
            </span>

            {/* Fit Button */}
            <button
              type="button"
              className="sim-mini-btn"
              onClick={resetZoom}
              title="Reset zoom & pan to fit full waveform (⟲)"
              style={{ padding: '2px 8px', fontSize: '11px' }}
            >
              ⟲ Fit
            </button>

            {/* Cursors Toggle */}
            <button
              type="button"
              className={`sim-mini-btn ${cursorsEnabled ? 'active' : ''}`}
              onClick={() => {
                if (cursorsEnabled) {
                  ctrl.clearCursors();
                  if (graphRegister) onUpdateGraphRegister?.({ cursorsEnabled: false });
                } else {
                  ctrl.setCursorPreset();
                  if (graphRegister) onUpdateGraphRegister?.({ cursorsEnabled: true });
                }
              }}
              title={cursorsEnabled ? 'Turn cursors OFF' : 'Turn cursors ON (places markers A & B)'}
              style={{
                padding: '2px 8px',
                fontSize: '11px',
                fontWeight: cursorsEnabled ? 700 : 500,
                background: cursorsEnabled ? 'rgba(56, 189, 248, 0.2)' : undefined,
                borderColor: cursorsEnabled ? 'var(--accent, #38bdf8)' : undefined,
                color: cursorsEnabled ? 'var(--accent, #38bdf8)' : undefined,
              }}
            >
              📍 Cursors{cursorsEnabled ? ' (ON)' : ''}
            </button>

            {/* Self-explanatory Before/After Reference Comparison */}
            {onPinBaseline && (
              !baseline ? (
                <button
                  type="button"
                  data-testid="btn-pin-baseline"
                  className="sim-mini-btn"
                  disabled={!effectiveResults || signalNames.length === 0}
                  onClick={onPinBaseline}
                  title="Freeze these curves as a Before reference so you can compare against your next change"
                  style={{ padding: '2px 8px', fontSize: '11px' }}
                >
                  ⇄ Save Reference
                </button>
              ) : (
                <div className="dso-baseline-group" data-testid="baseline-controls">
                  <button
                    type="button"
                    data-testid="btn-toggle-baseline"
                    className={`sim-mini-btn ${isBaselineVisible ? 'active' : ''}`}
                    onClick={handleToggleBaselineVisible}
                    title={isBaselineVisible ? 'Hide dashed Before reference curves' : 'Show dashed Before reference curves'}
                  >
                    {isBaselineVisible ? '⇄ Reference ON' : '⇄ Reference OFF'}
                  </button>
                  <button
                    type="button"
                    data-testid="btn-repin-baseline"
                    className="sim-mini-btn"
                    disabled={!effectiveResults || signalNames.length === 0}
                    onClick={onPinBaseline}
                    title="Overwrite saved reference with current curves"
                  >
                    ↺ Update
                  </button>
                  {onClearBaseline && (
                    <button
                      type="button"
                      data-testid="btn-clear-baseline"
                      className="sim-mini-btn"
                      onClick={onClearBaseline}
                      title="Remove saved reference comparison"
                    >
                      ✕
                    </button>
                  )}
                </div>
              )
            )}

            {/* Focus Mode (only when not in docked bottom drawer) */}
            {onToggleSidebar && !onCollapseDrawer && (
              <button
                type="button"
                className={`sim-mini-btn ${!isSidebarOpen ? 'active' : ''}`}
                onClick={onToggleSidebar}
                title={isSidebarOpen ? 'Hide Settings Sidebar' : 'Show Settings Sidebar'}
                style={{ padding: '2px 8px', fontSize: '11px', fontWeight: 600 }}
              >
                {isSidebarOpen ? '⇥ Focus' : '⇤ Sidebar'}
              </button>
            )}
          </div>
        </div>
      )}

      {/* Clear Reference Comparison Banner — gives instant visual confirmation */}
      {baseline && (
        <div className="scope-baseline-banner" data-testid="baseline-status-banner">
          <span className="scope-baseline-banner-text">
            {hasBaselineDiff ? (
              <>
                <strong>⇄ Comparing vs. Reference</strong> — dashed = before, solid = after
              </>
            ) : (
              <>
                <strong>📌 Reference saved</strong> — now change a component value &amp; run{' '}
                <kbd className="kbd-shortcut">F5</kbd> to see before (dashed) vs. after (solid)
              </>
            )}
          </span>
          {onClearBaseline && (
            <button
              type="button"
              className="sim-mini-btn"
              onClick={onClearBaseline}
              title="Clear saved reference"
            >
              Clear Ref
            </button>
          )}
        </div>
      )}

      {/* Synchronized Sweep Step Bar — controls ALL scope channels & schematic values together */}
      {sweepSession && (
        <div className="sweep-step-bar" data-testid="sweep-step-bar">
          <span className="sweep-step-bar-label">
            🎚 Sweep <strong>{sweepSession.paramLabel}</strong>:
          </span>

          {sweepSession.status === 'RUNNING' ? (
            <span className="sweep-step-progress">
              Running {sweepSession.completedRuns}/{sweepSession.totalRuns} (
              {Math.round(sweepSession.progress * 100)}%)…
            </span>
          ) : (
            <>
              <div className="sweep-step-pills" role="group" aria-label="Sweep step selector">
                {completedSweepRuns.map((run) => {
                  const isActive = !sweepOverlayAll && activeSweepRun?.index === run.index;
                  const color = sweepRunColor(run.index, sweepSession.totalRuns, theme);
                  return (
                    <button
                      key={run.index}
                      type="button"
                      data-testid={`sweep-step-pill-${run.index}`}
                      className={`sweep-step-pill ${isActive ? 'active' : ''}`}
                      onClick={() => {
                        setSweepOverlayAll(false);
                        setActiveSweepStepIndex(run.index);
                      }}
                      title={`Inspect step ${run.index + 1}/${sweepSession.totalRuns}: ${sweepSession.componentName} = ${formatEngineeringValue(run.parameterValue, sweepSession.unit)} across all signals & schematic readouts`}
                    >
                      <span className="sweep-step-dot" style={{ backgroundColor: color }} />
                      <span>{formatEngineeringValue(run.parameterValue, sweepSession.unit)}</span>
                    </button>
                  );
                })}
              </div>

              {completedSweepRuns.length > 0 && (
                <button
                  type="button"
                  data-testid="sweep-all-curves-btn"
                  className={`sim-mini-btn ${sweepOverlayAll ? 'active' : ''}`}
                  onClick={() => setSweepOverlayAll(!sweepOverlayAll)}
                  title="Overlay all sweep steps across every visible waveform channel"
                >
                  All Curves ({completedSweepRuns.length})
                </button>
              )}

              {activeSweepRun && onApplySweepValue && (
                <button
                  type="button"
                  data-testid="sweep-apply-active-btn"
                  className="sim-mini-btn active"
                  onClick={() =>
                    onApplySweepValue(
                      sweepSession.componentName,
                      sweepSession.paramKey,
                      activeSweepRun.parameterValue,
                    )
                  }
                  title={`Apply ${formatEngineeringValue(activeSweepRun.parameterValue, sweepSession.unit)} to ${sweepSession.componentName}`}
                >
                  ✓ Apply {formatEngineeringValue(activeSweepRun.parameterValue, sweepSession.unit)}
                </button>
              )}
            </>
          )}

          {onClearSweep && (
            <button
              type="button"
              data-testid="sweep-bar-close-btn"
              className="sim-mini-btn sweep-bar-close-btn"
              onClick={onClearSweep}
              title="Close sweep preview and return to normal simulation"
            >
              ✕
            </button>
          )}
        </div>
      )}

      {/* DSO Main Viewport (Single flat view: Curves, Metrics, FFT, Losses, or Sweep) */}
      <div className="dso-viewport">
        {onPinSlider && onUnpinSlider && onUpdateSliderConfig && onCommitSliderValue && (
          <LiveSliderBar
            pinnedSliders={pinnedSliders}
            components={components}
            isOpen={tuneBarOpen}
            onPinSlider={onPinSlider}
            onUnpinSlider={onUnpinSlider}
            onUpdateSliderConfig={onUpdateSliderConfig}
            onCommitValue={onCommitSliderValue}
            isSimRunning={isRunning}
          />
        )}
        {(status === 'FAILED' || Boolean(errorMessage)) && (
          <div
            className="sim-error-box scope-error-banner"
            role="alert"
            data-testid="scope-sim-error"
            style={{
              margin: '12px 16px 0 16px',
              padding: '10px 14px',
              borderRadius: 6,
              border: '1px solid var(--danger, #ef4444)',
              background: 'rgba(239, 68, 68, 0.14)',
              color: 'var(--danger, #f87171)',
              fontSize: 13,
              lineHeight: 1.45,
            }}
          >
            <div style={{ fontWeight: 700, marginBottom: warnings.length > 0 ? 4 : 0 }}>
              ⚠️ Simulation Failed: {errorMessage || 'An error occurred during simulation.'}
            </div>
            {warnings.length > 0 && (
              <ul style={{ margin: '4px 0 0 18px', padding: 0, fontSize: 12 }}>
                {warnings.map((w, idx) => (
                  <li key={idx}>{w}</li>
                ))}
              </ul>
            )}
          </div>
        )}
        {status !== 'FAILED' && !errorMessage && warnings.length > 0 && (
          <div
            className="sim-warning-box scope-warning-banner"
            role="status"
            data-testid="scope-sim-warnings"
            style={{
              margin: '12px 16px 0 16px',
              padding: '10px 14px',
              borderRadius: 6,
              border: '1px solid var(--warning, #f59e0b)',
              background: 'rgba(245, 158, 11, 0.14)',
              color: 'var(--warning, #fbbf24)',
              fontSize: 13,
              lineHeight: 1.45,
            }}
          >
            <div style={{ fontWeight: 700, marginBottom: 4 }}>
              ⚠️ Simulation Diagnostics ({warnings.length} {warnings.length === 1 ? 'warning' : 'warnings'}):
            </div>
            <ul style={{ margin: '4px 0 0 18px', padding: 0, fontSize: 12 }}>
              {warnings.map((w, idx) => (
                <li key={idx}>{w}</li>
              ))}
            </ul>
          </div>
        )}
        {!effectiveResults || signalNames.length === 0 ? (
          graphRegister ? (
            <div className="scope-empty-state pre-sim-screen" data-testid="scope-pre-sim-state">
              <div className="empty-icon">📈</div>
              <h3>{graphRegister.name}</h3>
              <p>
                {status === 'COMPLETED'
                  ? 'Simulation completed, but 0 signals were recorded. Ensure probes (VOLT, AMP) and gates are coupled to circuit components or labeled nodes.'
                  : graphRegister.signals.length === 0
                    ? 'No signals selected yet. Pick labeled electrical or control signals in the right panel to plot.'
                    : `Configured for ${graphRegister.signals.length} signal${graphRegister.signals.length > 1 ? 's' : ''} across ${graphRegister.numGraphs} graph${graphRegister.numGraphs > 1 ? 's' : ''}.`}
              </p>
              {graphRegister.signals.length > 0 && (
                <div
                  className="pre-sim-signals-preview"
                  style={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: 6,
                    margin: '12px 0',
                    maxWidth: 480,
                    justifyContent: 'center',
                  }}
                >
                  {graphRegister.signals.map((sig) => {
                    const color = colorOf(sig);
                    const g = graphRegister.signalGraphs[sig] || 1;
                    return (
                      <span
                        key={sig}
                        className="dso-osd-channel-tag"
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 5,
                          padding: '3px 8px',
                          borderRadius: 4,
                          border: `1px solid ${color}`,
                          background: 'var(--surface)',
                          fontSize: 11,
                          fontFamily: 'var(--font-mono, monospace)',
                        }}
                      >
                        <span
                          style={{
                            width: 6,
                            height: 6,
                            borderRadius: '50%',
                            background: color,
                          }}
                        />
                        <span>{sig}</span>
                        {graphRegister.numGraphs > 1 && (
                          <span style={{ opacity: 0.7, fontSize: 10 }}>(G{g})</span>
                        )}
                      </span>
                    );
                  })}
                </div>
              )}
              {onRunSimulation && (
                <div style={{ marginTop: 16 }}>
                    <button
                    type="button"
                    className="sim-mini-btn active"
                    onClick={onRunSimulation}
                    title="Run Simulation (F5)"
                    style={{
                      padding: '8px 20px',
                      fontSize: '14px',
                      fontWeight: 700,
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 8,
                      borderRadius: 6,
                      background: 'var(--accent, #38bdf8)',
                      color: '#000000',
                      cursor: 'pointer',
                    }}
                  >
                    <span aria-hidden="true">▶</span> Run Simulation
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="scope-empty-state">
              <div className="empty-icon">{selectedScope === 'all' ? '📊' : '📺'}</div>
              <h3>{status === 'COMPLETED' ? 'No Observable Signals Recorded' : 'No simulation data available'}</h3>
              <p>
                {status === 'COMPLETED' ? (
                  <>
                    The simulation finished, but <strong>0 signals</strong> were recorded. Ensure Voltmeters, Ammeters, or net labels are connected to valid circuit nodes or components, then click <strong>"▶ Run Simulation"</strong>.
                  </>
                ) : (
                  <>
                    Configure parameters in the <strong>Simulation Settings</strong> panel on the right and click <strong>"▶ Run Simulation"</strong> to calculate waveforms.
                  </>
                )}
              </p>
            </div>
          )
        ) : (Boolean(graphRegister) || selectedScope !== 'all') && filteredChannels.length === 0 ? (
          <div className="scope-empty-state" data-testid="scope-no-channels-state">
            <div className="empty-icon">📺</div>
            <h3>No channels assigned to {graphRegister?.name || selectedScope}</h3>
            <p>Select labeled electrical or control signals in the right panel (or click Assign Signals below) to plot waveforms.</p>
            <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
              <button
                type="button"
                className="sim-mini-btn active"
                onClick={() => setIsChannelManagerOpen(true)}
                style={{ padding: '6px 14px', fontSize: '13px', display: 'flex', alignItems: 'center', gap: 6 }}
              >
                <span>➕</span> Assign Signals
              </button>
              <button
                type="button"
                className="sim-mini-btn"
                onClick={handleAutoAddProbes}
                style={{ padding: '6px 14px', fontSize: '13px', display: 'flex', alignItems: 'center', gap: 6 }}
              >
                <span>⚡</span> Auto-Add Probes
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* Primary Content based on flat view tab */}
            {drawerTab === null ? (
              <>
                <div className="dso-screen-bezel">
                  <div className="dso-chart-container" ref={chartContainerRef}>
                    {effectiveDisplayLayout === 'stacked' ? (
                      <FullScreenStackedChart
                        width={chartDimensions.width}
                        height={chartDimensions.height}
                        time={timeArray}
                        signals={effectiveResults}
                        activeSignals={visibleSignals}
                        colorOf={colorOf}
                        theme={theme}
                        hoverIndex={hoverIndex}
                        onHoverIndex={setHoverIndex}
                        cursorA={cursorA}
                        cursorB={cursorB}
                        cursorsEnabled={cursorsEnabled}
                        activeCursor={activeCursor}
                        onSetActiveCursor={setActiveCursor}
                        onSetCursor={placeCursorAtSample}
                        viewStart={win.start}
                        viewEnd={win.end}
                        onZoomAt={zoomAt}
                        onZoomRange={zoomRange}
                        onResetZoom={resetZoom}
                        onPanSeconds={(delta) => setView(panWindow(win, delta, dataT0, dataT1))}
                        onOpenContextMenu={handleOpenChartContextMenu}
                        baselineResults={baselineResults}
                        sweepOverlays={sweepOverlays}
                        sweepSignal={effectiveSweepSignal}
                      />
                    ) : effectiveDisplayLayout === '2-graphs' || effectiveDisplayLayout === '3-graphs' ? (
                      <FullScreenMultiGraphChart
                        numGraphs={effectiveDisplayLayout === '2-graphs' ? 2 : 3}
                        width={chartDimensions.width}
                        height={chartDimensions.height}
                        time={timeArray}
                        signals={effectiveResults}
                        activeSignals={visibleSignals}
                        signalGraphs={effectiveSignalGraphs}
                        onMoveSignalToGraph={handleMoveSignalToGraph}
                        colorOf={colorOf}
                        theme={theme}
                        hoverIndex={hoverIndex}
                        onHoverIndex={setHoverIndex}
                        cursorA={cursorA}
                        cursorB={cursorB}
                        cursorsEnabled={cursorsEnabled}
                        activeCursor={activeCursor}
                        onSetActiveCursor={setActiveCursor}
                        onSetCursor={placeCursorAtSample}
                        viewStart={win.start}
                        viewEnd={win.end}
                        onZoomAt={zoomAt}
                        onZoomRange={zoomRange}
                        onResetZoom={resetZoom}
                        onPanSeconds={(delta) => setView(panWindow(win, delta, dataT0, dataT1))}
                        hoveredSignal={hoveredSignal}
                        onHoverSignal={setHoveredSignal}
                        onToggleSignal={handleToggleSignal}
                        onOpenContextMenu={handleOpenChartContextMenu}
                        baselineResults={baselineResults}
                        sweepOverlays={sweepOverlays}
                        sweepSignal={effectiveSweepSignal}
                      />
                    ) : (
                      <FullScreenOverlayChart
                        width={chartDimensions.width}
                        height={chartDimensions.height}
                        time={timeArray}
                        signals={effectiveResults}
                        activeSignals={visibleSignals}
                        colorOf={colorOf}
                        theme={theme}
                        hoverIndex={hoverIndex}
                        onHoverIndex={setHoverIndex}
                        cursorA={cursorA}
                        cursorB={cursorB}
                        cursorsEnabled={cursorsEnabled}
                        activeCursor={activeCursor}
                        onSetActiveCursor={setActiveCursor}
                        onSetCursor={placeCursorAtSample}
                        viewStart={win.start}
                        viewEnd={win.end}
                        onZoomAt={zoomAt}
                        onZoomRange={zoomRange}
                        onResetZoom={resetZoom}
                        onPanSeconds={(delta) => setView(panWindow(win, delta, dataT0, dataT1))}
                        yScaleMode={yScaleMode}
                        yZoomRange={yView}
                        onZoomYRange={setYView}
                        hoveredSignal={hoveredSignal}
                        onHoverSignal={setHoveredSignal}
                        onToggleSignal={handleToggleSignal}
                        onOpenContextMenu={handleOpenChartContextMenu}
                        baselineResults={baselineResults}
                        sweepOverlays={sweepOverlays}
                        sweepSignal={effectiveSweepSignal}
                      />
                    )}
                  </div>

                  {/* Screen Bottom Quick-Measurement Strip */}
                  <div className="dso-quick-measure-bar">
                    <div className="dso-quick-tiles">
                      {visibleSignals.map((name) => {
                        const st = signalStats[name];
                        if (!st) return null;
                        const unit = inferSignalUnit(name);
                        const color = colorOf(name);
                        const isHovered = hoveredSignal === name;
                        const cmp = baselineComparisons[name];
                        const deltaCls = cmp
                          ? cmp.mean.absDelta > 1e-12
                            ? 'pos'
                            : cmp.mean.absDelta < -1e-12
                              ? 'neg'
                              : ''
                          : '';
                        return (
                          <div
                            key={name}
                            className={`dso-measure-tile ${isHovered ? 'highlighted' : ''}`}
                            style={{
                              borderLeft: `3px solid ${color}`,
                              boxShadow: isHovered ? `0 0 8px ${color}60` : undefined,
                            }}
                            onMouseEnter={() => setHoveredSignal(name)}
                            onMouseLeave={() => setHoveredSignal(null)}
                          >
                            <span style={{ color, fontWeight: 700 }}>{name}</span>
                            <span>Vpp: <strong className="dso-measure-tile-val">{formatEngineeringValue(st.pkpk, unit)}</strong></span>
                            <span>Vrms: <strong className="dso-measure-tile-val">{formatEngineeringValue(st.rms, unit)}</strong></span>
                            <span>Mean: <strong className="dso-measure-tile-val">{formatEngineeringValue(st.mean, unit)}</strong></span>
                            {cmp && (
                              <span className={`dso-delta-chip ${deltaCls}`} data-testid={`quick-delta-${name}`}>
                                ΔMean: {formatDeltaBadge(cmp.mean, unit, formatEngineeringValue)}
                              </span>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <div className="dso-flat-panel" data-testid={`scope-flat-${drawerTab}`}>
                {drawerTab === 'metrics' && (() => {
                  const showDeltaCols = Boolean(baseline && isBaselineVisible);
                  return (
                    <table className="scope-metrics-table">
                      <thead>
                        <tr>
                          <th>Signal Trace</th>
                          <th>Minimum</th>
                          <th>Maximum</th>
                          <th>Peak-to-Peak</th>
                          <th>RMS</th>
                          <th>Mean (Avg)</th>
                          {showDeltaCols && (
                            <>
                              <th>Δ Mean</th>
                              <th>Δ RMS</th>
                              <th>Δ Pk-Pk</th>
                            </>
                          )}
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredChannels.map((name) => {
                          const st = signalStats[name];
                          if (!st) return null;
                          const color = colorOf(name);
                          const isHidden = isSignalHidden(name);
                          const unit = inferSignalUnit(name);
                          const cmp = showDeltaCols ? baselineComparisons[name] : undefined;
                          return (
                            <tr
                              key={name}
                              onClick={() => handleToggleSignal(name)}
                              className={isHidden ? 'row-hidden' : ''}
                              title="Click row to toggle trace visibility"
                            >
                              <td style={{ color, fontWeight: 700 }}>
                                <span className="dso-ch-led" style={{ backgroundColor: color, color, marginRight: 6 }} />
                                {name}
                              </td>
                              <td>{formatEngineeringValue(st.min, unit)}</td>
                              <td>{formatEngineeringValue(st.max, unit)}</td>
                              <td>{formatEngineeringValue(st.pkpk, unit)}</td>
                              <td>{formatEngineeringValue(st.rms, unit)}</td>
                              <td>{formatEngineeringValue(st.mean, unit)}</td>
                              {showDeltaCols && (
                                <>
                                  {[cmp?.mean, cmp?.rms, cmp?.pkpk].map((delta, dIdx) => {
                                    if (!delta) return <td key={dIdx} className="dso-delta-cell">—</td>;
                                    const cls =
                                      delta.absDelta > 1e-12 ? 'pos' : delta.absDelta < -1e-12 ? 'neg' : '';
                                    return (
                                      <td
                                        key={dIdx}
                                        className={`dso-delta-cell ${cls}`}
                                        title={`Baseline: ${formatEngineeringValue(delta.baseline, unit)}`}
                                      >
                                        {formatDeltaBadge(delta, unit, formatEngineeringValue)}
                                      </td>
                                    );
                                  })}
                                </>
                              )}
                              <td>
                                <span className={`channel-status-pill ${isHidden ? 'off' : 'on'}`}>
                                  {isHidden ? 'Hidden' : 'Active'}
                                </span>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  );
                })()}

                {drawerTab === 'fft' && (
                  <FftPanel
                    time={timeArray}
                    signals={effectiveResults}
                    activeSignals={visibleSignals}
                    viewStart={win.start}
                    viewEnd={win.end}
                  />
                )}

                {drawerTab === 'losses' && (
                  <LossPanel
                    time={timeArray}
                    signals={effectiveResults || undefined}
                    activeSignals={visibleSignals}
                    cursorA={cursorA}
                    cursorB={cursorB}
                    colorOf={colorOf}
                    components={components}
                  />
                )}

                {drawerTab === 'sweep' && (
                  <SweepPanel
                    sweepSession={sweepSession}
                    availableSignals={filteredChannels.length > 0 ? filteredChannels : signalNames}
                    theme={theme}
                    overlayEnabled={sweepOverlayAll}
                    onToggleOverlay={setSweepOverlayAll}
                    selectedSignal={effectiveSweepSignal}
                    onSelectSignal={setSweepSelectedSignal}
                    highlightedRunIndex={sweepHighlightedRun}
                    onHighlightRun={(idx) => {
                      setSweepHighlightedRun(idx);
                      if (idx !== null) {
                        setActiveSweepStepIndex(idx);
                      }
                    }}
                    onApplyValue={onApplySweepValue}
                    onClearSweep={() => {
                      onClearSweep?.();
                      setDrawerTab(null);
                    }}
                  />
                )}
              </div>
            )}
          </>
        )}
      </div>

      {/* Scope Channel Manager Modal */}
      {(Boolean(graphRegister) || selectedScope !== 'all') && (
        <ScopeChannelManager
          isOpen={isChannelManagerOpen}
          onClose={() => setIsChannelManagerOpen(false)}
          scopeName={graphRegister?.name || selectedScope}
          assignedChannels={assignedChannels}
          allAvailableSignals={signalNames}
          colorOf={colorOf}
          onAddChannel={handleAddChannel}
          onRemoveChannel={handleRemoveChannel}
          onAutoAddProbes={handleAutoAddProbes}
          onClearChannels={handleClearChannels}
          displayLayout={effectiveDisplayLayout}
          onDisplayLayoutChange={onDisplayLayoutChange}
          signalGraphs={signalGraphs}
          onSetSignalGraph={handleMoveSignalToGraph}
          onAutoGroupByCategory={handleAutoGroupSignals}
          components={components}
          wires={wires}
          onHighlightComponent={onHighlightComponent}
          onHighlightNet={onHighlightNet}
        />
      )}

      {/* Floating DSO Right-Click Context Menu */}
      {scopeContextMenu && (
        <div
          ref={scopeContextMenuRef}
          className="context-menu scope-context-menu"
          data-testid="scope-context-menu"
          style={{
            left:
              typeof window !== 'undefined' && window.innerWidth > 260
                ? Math.min(scopeContextMenu.x, window.innerWidth - 245)
                : scopeContextMenu.x,
            top:
              typeof window !== 'undefined' && window.innerHeight > 440
                ? Math.min(scopeContextMenu.y, window.innerHeight - 390)
                : scopeContextMenu.y,
          }}
          onContextMenu={(e) => e.preventDefault()}
        >
          <div className="context-menu-header">
            <span>Oscilloscope</span>
            {scopeContextMenu.sampleIdx !== null && timeArray[scopeContextMenu.sampleIdx] !== undefined && (
              <span className="context-menu-meta">
                {formatEngineeringValue(timeArray[scopeContextMenu.sampleIdx], 's')}
              </span>
            )}
          </div>

          <button
            type="button"
            className="context-menu-item"
            disabled={scopeContextMenu.sampleIdx === null}
            onClick={() => {
              if (scopeContextMenu.sampleIdx !== null) {
                placeCursorAtSample('A', scopeContextMenu.sampleIdx);
                setActiveCursor('A');
              }
              setScopeContextMenu(null);
            }}
          >
            <span>📍 Place Cursor A Here</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            disabled={scopeContextMenu.sampleIdx === null}
            onClick={() => {
              if (scopeContextMenu.sampleIdx !== null) {
                placeCursorAtSample('B', scopeContextMenu.sampleIdx);
                setActiveCursor('B');
              }
              setScopeContextMenu(null);
            }}
          >
            <span>📍 Place Cursor B Here</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              ctrl.clearCursors();
              if (graphRegister) onUpdateGraphRegister?.({ cursorsEnabled: false });
              setScopeContextMenu(null);
            }}
          >
            <span>✕ Clear Cursors</span>
          </button>

          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              resetZoom();
              setScopeContextMenu(null);
            }}
          >
            <span>⟲ Zoom to Fit Full Waveform</span>
            <span className="context-menu-shortcut">2× Click</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              setYScaleMode((prev) => (prev === 'auto' ? 'fixed' : 'auto'));
              setScopeContextMenu(null);
            }}
          >
            <span>↕ Y-Axis Scale: {yScaleMode === 'auto' ? 'Auto (Switch Fixed)' : 'Fixed (Switch Auto)'}</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              handleAutoGroupSignals();
              setScopeContextMenu(null);
            }}
          >
            <span>⚡ Auto-Group Signals (V→G1, I→G2, Ctrl→G3)</span>
          </button>

          <div
            className="context-menu-submenu-wrap"
            onMouseEnter={() => setScopeContextLayoutOpen(true)}
          >
            <button
              type="button"
              className="context-menu-item"
              onClick={() => setScopeContextLayoutOpen((prev) => !prev)}
            >
              <span>📊 Graph Layout</span>
              <span className="context-menu-shortcut">▸</span>
            </button>
            {scopeContextLayoutOpen && (
              <div className="context-menu-submenu">
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={() => {
                    setScopeLayout('overlay');
                    setScopeContextMenu(null);
                  }}
                >
                  <span>{effectiveDisplayLayout === 'overlay' ? '✓ ' : ''}1 Graph (Overlay)</span>
                </button>
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={() => {
                    setScopeLayout('2-graphs');
                    setScopeContextMenu(null);
                  }}
                >
                  <span>{effectiveDisplayLayout === '2-graphs' ? '✓ ' : ''}2 Graphs</span>
                </button>
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={() => {
                    setScopeLayout('3-graphs');
                    setScopeContextMenu(null);
                  }}
                >
                  <span>{effectiveDisplayLayout === '3-graphs' ? '✓ ' : ''}3 Graphs</span>
                </button>
                {!graphRegister && onDisplayLayoutChange && (
                  <button
                    type="button"
                    className="context-menu-item"
                    onClick={() => {
                      onDisplayLayoutChange('stacked');
                      setScopeContextMenu(null);
                    }}
                  >
                    <span>{effectiveDisplayLayout === 'stacked' ? '✓ ' : ''}Stacked Lanes</span>
                  </button>
                )}
              </div>
            )}
          </div>

          {(Boolean(graphRegister) || selectedScope !== 'all') && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => {
                setIsChannelManagerOpen(true);
                setScopeContextMenu(null);
              }}
            >
              <span>➕ Add / Manage Channels...</span>
            </button>
          )}

          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              setDrawerTab((prev) => (prev === 'fft' ? null : 'fft'));
              setScopeContextMenu(null);
            }}
          >
            <span>{drawerTab === 'fft' ? '✓ ' : ''}〰 Toggle FFT Spectrum</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              setDrawerTab((prev) => (prev === 'losses' ? null : 'losses'));
              setScopeContextMenu(null);
            }}
          >
            <span>{drawerTab === 'losses' ? '✓ ' : ''}⚡ Toggle Power & Losses</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              setDrawerTab((prev) => (prev === 'metrics' ? null : 'metrics'));
              setScopeContextMenu(null);
            }}
          >
            <span>{drawerTab === 'metrics' ? '✓ ' : ''}📊 Toggle Statistics</span>
          </button>

          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              handleExportCsv();
              setScopeContextMenu(null);
            }}
          >
            <span>📥 Export CSV...</span>
          </button>
        </div>
      )}
    </div>
  );
}

/** Full-Screen Overlay Waveform Chart */
function FullScreenOverlayChart({
  width = 1000,
  height = 480,
  time,
  signals,
  activeSignals,
  colorOf,
  theme,
  hoverIndex,
  onHoverIndex,
  cursorA,
  cursorB,
  cursorsEnabled,
  activeCursor,
  onSetActiveCursor,
  onSetCursor,
  viewStart,
  viewEnd,
  onZoomAt,
  onZoomRange,
  onResetZoom,
  onPanSeconds,
  yScaleMode,
  yZoomRange,
  onZoomYRange,
  hoveredSignal,
  onHoverSignal,
  onToggleSignal,
  onOpenContextMenu,
  baselineResults = null,
  sweepOverlays = [],
  sweepSignal: _sweepSignal = '',
}: {
  width?: number;
  height?: number;
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  colorOf: (name: string) => string;
  theme: 'dark' | 'light';
  hoverIndex: number | null;
  onHoverIndex: (idx: number | null) => void;
  cursorA: number | null;
  cursorB: number | null;
  cursorsEnabled?: boolean;
  activeCursor?: 'A' | 'B';
  onSetActiveCursor?: (c: 'A' | 'B') => void;
  onSetCursor: (type: 'A' | 'B', idx: number) => void;
  viewStart: number;
  viewEnd: number;
  onZoomAt: (factor: number, anchor: number) => void;
  onZoomRange: (start: number, end: number) => void;
  onResetZoom: () => void;
  onPanSeconds: (delta: number) => void;
  yScaleMode: 'fixed' | 'auto';
  yZoomRange: { min: number; max: number } | null;
  onZoomYRange: (range: { min: number; max: number } | null) => void;
  hoveredSignal?: string | null;
  onHoverSignal?: (name: string | null) => void;
  onToggleSignal?: (name: string) => void;
  onOpenContextMenu?: (x: number, y: number, sampleIdx: number | null) => void;
  baselineResults?: Record<string, number[]> | null;
  sweepOverlays?: SweepOverlayTrace[];
  sweepSignal?: string;
}) {
  const hasAnyHighlightedSweep = sweepOverlays.some((ov) => ov.highlighted);
  const padLeft = 68;
  const padRight = 24;
  const padTop = 20;
  const padBottom = 34;

  const [internalHoveredSignal, setInternalHoveredSignal] = useState<string | null>(null);
  const currentHoveredSignal = hoveredSignal !== undefined ? hoveredSignal : internalHoveredSignal;
  const setHovered = onHoverSignal || setInternalHoveredSignal;

  const plotW = Math.max(100, width - padLeft - padRight);
  const plotH = Math.max(100, height - padTop - padBottom);

  const svgRef = useRef<SVGSVGElement | null>(null);
  const timeRef = useRef(time);
  timeRef.current = time;

  const gridColor = theme === 'light' ? '#e2e8f0' : '#334155';
  const textColor = theme === 'light' ? '#64748b' : '#94a3b8';
  const zeroColor = theme === 'light' ? '#94a3b8' : '#64748b';
  const crosshairColor = theme === 'light' ? '#334155' : '#cbd5e1';

  const minT = viewStart;
  const maxT = viewEnd;

  // Global min/max across all active signals over the entire simulation (rock-steady scale)
  const { globalMinY, globalMaxY } = useMemo(() => {
    let y0 = Infinity;
    let y1 = -Infinity;
    const scan = (arr: number[]) => {
      for (let i = 0; i < arr.length; i++) {
        const v = arr[i];
        if (v !== undefined) {
          if (v < y0) y0 = v;
          if (v > y1) y1 = v;
        }
      }
    };
    for (const name of activeSignals) {
      scan(signals[name] || []);
      if (baselineResults?.[name]) {
        scan(baselineResults[name]);
      }
    }
    for (const ov of sweepOverlays) {
      for (const name of activeSignals) {
        if (ov.signals[name]) scan(ov.signals[name]);
      }
    }
    if (!Number.isFinite(y0) || !Number.isFinite(y1)) {
      y0 = -1;
      y1 = 1;
    }
    if (y0 === y1) {
      y0 -= 1;
      y1 += 1;
    }
    const yPad = (y1 - y0) * 0.08;
    return { globalMinY: y0 - yPad, globalMaxY: y1 + yPad };
  }, [activeSignals, signals, baselineResults, sweepOverlays]);

  // Visible slice min/max (used only when user switches to 'auto' Y-scale)
  const { sliceMinY, sliceMaxY } = useMemo(() => {
    let y0 = Infinity;
    let y1 = -Infinity;
    const scanSlice = (tVec: number[], arr: number[]) => {
      if (tVec.length === 0 || arr.length === 0) return;
      const s0 = sampleIndexAt(tVec, minT);
      const s1 = sampleIndexAt(tVec, maxT);
      for (let i = s0; i <= s1 && i < arr.length; i++) {
        const v = arr[i];
        if (v !== undefined) {
          if (v < y0) y0 = v;
          if (v > y1) y1 = v;
        }
      }
    };
    const bTime = baselineResults?.['time'] || [];
    for (const name of activeSignals) {
      scanSlice(time, signals[name] || []);
      if (baselineResults && bTime.length > 0 && baselineResults[name]) {
        scanSlice(bTime, baselineResults[name]);
      }
    }
    for (const ov of sweepOverlays) {
      for (const name of activeSignals) {
        if (ov.signals[name]) scanSlice(ov.time, ov.signals[name]);
      }
    }
    if (!Number.isFinite(y0) || !Number.isFinite(y1)) {
      y0 = -1;
      y1 = 1;
    }
    if (y0 === y1) {
      y0 -= 1;
      y1 += 1;
    }
    const yPad = (y1 - y0) * 0.08;
    return { sliceMinY: y0 - yPad, sliceMaxY: y1 + yPad };
  }, [time, signals, baselineResults, sweepOverlays, activeSignals, minT, maxT]);

  // If 2D box zoomed, use yZoomRange; else fixed or slice
  const { minY, maxY } = useMemo(() => {
    if (yZoomRange) {
      return { minY: yZoomRange.min, maxY: yZoomRange.max };
    }
    if (yScaleMode === 'auto') {
      return { minY: sliceMinY, maxY: sliceMaxY };
    }
    return { minY: globalMinY, maxY: globalMaxY };
  }, [yZoomRange, yScaleMode, sliceMinY, sliceMaxY, globalMinY, globalMaxY]);

  // non-passive wheel zoom around the cursor position
  useEffect(() => {
    const svg = svgRef.current;
    if (!svg) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const svgX = clientToSvgCoords(svg, e.clientX, e.clientY, width, height).x;
      const anchor = minT + ((svgX - padLeft) / plotW) * (maxT - minT);
      onZoomAt(Math.exp(e.deltaY / 500), anchor);
    };
    svg.addEventListener('wheel', onWheel, { passive: false });
    return () => svg.removeEventListener('wheel', onWheel);
  }, [minT, maxT, onZoomAt, width, height, plotW]);

  const mapX = (t: number) => padLeft + ((t - minT) / (maxT - minT || 1)) * plotW;
  const mapY = (v: number) => padTop + plotH - ((v - minY) / (maxY - minY || 1)) * plotH;

  const yTicks = useMemo(() => {
    const count = 8;
    const ticks = [];
    for (let i = 0; i <= count; i++) {
      const val = minY + (i / count) * (maxY - minY);
      ticks.push({ val, y: mapY(val) });
    }
    return ticks;
  }, [minY, maxY, plotH, padTop]);

  const xTicks = useMemo(() => {
    const count = 10;
    const ticks = [];
    for (let i = 0; i <= count; i++) {
      const val = minT + (i / count) * (maxT - minT);
      ticks.push({ val, x: mapX(val) });
    }
    return ticks;
  }, [minT, maxT, plotW, padLeft]);

  // Center axes sub-division tick marks (5 minor ticks per division)
  const centerTicks = useMemo(() => {
    const xMid = padLeft + plotW / 2;
    const yMid = padTop + plotH / 2;
    const ticks: { x1: number; y1: number; x2: number; y2: number }[] = [];

    // Horizontal center line subdivision ticks (10 divisions * 5 sub = 50 intervals)
    for (let div = 0; div < 10; div++) {
      for (let sub = 1; sub < 5; sub++) {
        const x = padLeft + ((div + sub / 5) / 10) * plotW;
        ticks.push({ x1: x, y1: yMid - 3, x2: x, y2: yMid + 3 });
      }
    }

    // Vertical center line subdivision ticks (8 divisions * 5 sub = 40 intervals)
    for (let div = 0; div < 8; div++) {
      for (let sub = 1; sub < 5; sub++) {
        const y = padTop + ((div + sub / 5) / 8) * plotH;
        ticks.push({ x1: xMid - 3, y1: y, x2: xMid + 3, y2: y });
      }
    }

    return ticks;
  }, [padLeft, padTop, plotW, plotH]);

  // trace decimation limited to the visible slice
  const tracePaths = useMemo(() => {
    const i0 = sampleIndexAt(time, minT);
    const i1 = sampleIndexAt(time, maxT);
    return activeSignals.map((name) => {
      const arr = signals[name] || [];
      const iEnd = Math.min(i1, arr.length - 1);
      const len = iEnd - i0 + 1;
      if (len <= 0) return { name, path: '' };

      const step = Math.max(1, Math.floor(len / 3000));
      let d = `M ${mapX(time[i0])} ${mapY(arr[i0])}`;
      for (let i = i0 + step; i <= iEnd; i += step) {
        d += ` L ${mapX(time[i])} ${mapY(arr[i])}`;
      }
      if ((iEnd - i0) % step !== 0) {
        d += ` L ${mapX(time[iEnd])} ${mapY(arr[iEnd])}`;
      }
      return { name, path: d };
    });
  }, [activeSignals, time, signals, minT, maxT, minY, maxY]);

  const baselinePaths = useMemo(() => {
    if (!baselineResults) return [];
    const bTime = baselineResults['time'] || [];
    if (bTime.length === 0) return [];
    return activeSignals
      .map((name) => {
        const arr = baselineResults[name];
        if (!arr || arr.length === 0) return { name, path: '' };
        return {
          name,
          path: buildTraceSvgPath(bTime, arr, minT, maxT, mapX, mapY),
        };
      })
      .filter((item) => item.path !== '');
  }, [activeSignals, baselineResults, minT, maxT, minY, maxY, plotW, plotH, padLeft, padTop]);

  const sweepPaths = useMemo(() => {
    if (sweepOverlays.length === 0 || activeSignals.length === 0) return [];
    const items: { runIndex: number; signal: string; label: string; color: string; path: string; highlighted: boolean }[] = [];
    for (const sigName of activeSignals) {
      for (const ov of sweepOverlays) {
        const arr = ov.signals[sigName];
        if (!arr || arr.length === 0 || ov.time.length === 0) continue;
        const path = buildTraceSvgPath(ov.time, arr, minT, maxT, mapX, mapY);
        if (path) {
          items.push({
            runIndex: ov.runIndex,
            signal: sigName,
            label: ov.label,
            color: ov.color,
            path,
            highlighted: ov.highlighted,
          });
        }
      }
    }
    return items;
  }, [sweepOverlays, activeSignals, minT, maxT, minY, maxY, plotW, plotH, padLeft, padTop]);

  // Compute collision-free direct trace end badges at right plot edge
  const traceBadges = useMemo(() => {
    if (activeSignals.length === 0 || time.length === 0) return [];
    const iEnd = sampleIndexAt(time, maxT);

    const list = activeSignals.map((name) => {
      const arr = signals[name] || [];
      const val = arr.length > 0 ? (iEnd >= 0 && iEnd < arr.length ? arr[iEnd] : arr[arr.length - 1]) : 0;
      const rawY = mapY(val);
      const clampedY = Math.max(padTop + 10, Math.min(height - padBottom - 10, rawY));
      return {
        name,
        color: colorOf(name),
        val,
        y: clampedY,
        targetY: clampedY,
      };
    });

    list.sort((a, b) => a.y - b.y);

    const minSpacing = 20;
    for (let i = 1; i < list.length; i++) {
      if (list[i].y - list[i - 1].y < minSpacing) {
        list[i].y = list[i - 1].y + minSpacing;
      }
    }
    const bottomBound = height - padBottom - 10;
    if (list.length > 0 && list[list.length - 1].y > bottomBound) {
      list[list.length - 1].y = bottomBound;
      for (let i = list.length - 2; i >= 0; i--) {
        if (list[i + 1].y - list[i].y < minSpacing) {
          list[i].y = list[i + 1].y - minSpacing;
        }
      }
    }

    return list;
  }, [activeSignals, signals, time, maxT, padTop, height, padBottom, mapY, colorOf]);

  const handleMouseMove = (e: ReactMouseEvent<SVGSVGElement>) => {
    const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, height).x;
    if (svgX < padLeft || svgX > width - padRight) {
      onHoverIndex(null);
      return;
    }

    const t = minT + ((svgX - padLeft) / plotW) * (maxT - minT);
    onHoverIndex(sampleIndexAt(time, t));
  };

  const handleCursorDragStart = (e: React.PointerEvent, type: 'A' | 'B') => {
    e.stopPropagation();
    e.preventDefault();
    onSetActiveCursor?.(type);
    const svg = svgRef.current;
    if (!svg) return;

    const onMove = (ev: PointerEvent) => {
      const svgX = clientToSvgCoords(svg, ev.clientX, ev.clientY, width, height).x;
      const clampedX = Math.max(padLeft, Math.min(width - padRight, svgX));
      const t = minT + ((clampedX - padLeft) / plotW) * (maxT - minT);
      const newIdx = sampleIndexAt(time, t);
      onSetCursor(type, newIdx);
    };

    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  const handleClick = (e: ReactMouseEvent<SVGSVGElement>) => {
    if (suppressClickRef.current) {
      suppressClickRef.current = false;
      return;
    }
    const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, height).x;
    if (svgX < padLeft || svgX > width - padRight) return;
    const t = minT + ((svgX - padLeft) / plotW) * (maxT - minT);
    const targetIdx = sampleIndexAt(time, t);
    let targetCursor: 'A' | 'B' = 'A';
    if (e.shiftKey) {
      targetCursor = 'B';
      onSetActiveCursor?.('B');
    } else if (cursorA === null || !cursorsEnabled) {
      targetCursor = 'A';
      onSetActiveCursor?.('B');
    } else if (cursorB === null) {
      targetCursor = 'B';
      onSetActiveCursor?.('A');
    } else {
      targetCursor = activeCursor ?? 'A';
    }
    onSetCursor(targetCursor, targetIdx);
  };

  const handleContextMenu = (e: ReactMouseEvent<SVGSVGElement>) => {
    e.preventDefault();
    const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, height).x;
    const inPlot = svgX >= padLeft && svgX <= width - padRight && time.length > 0;
    const targetIdx = inPlot
      ? sampleIndexAt(time, minT + ((svgX - padLeft) / plotW) * (maxT - minT))
      : null;
    if (onOpenContextMenu) {
      onOpenContextMenu(e.clientX, e.clientY, targetIdx);
    } else if (targetIdx !== null) {
      onSetCursor('B', targetIdx);
      onSetActiveCursor?.('B');
    }
  };

  const [dragBox, setDragBox] = useState<{ startX: number; startY: number; currentX: number; currentY: number } | null>(null);
  const suppressClickRef = useRef(false);

  const handlePointerDown = (e: ReactMouseEvent<SVGSVGElement>) => {
    // Middle click or Alt + Left click -> Pan
    if (e.button === 1 || (e.button === 0 && e.altKey)) {
      e.preventDefault();
      const svg = e.currentTarget;
      const ctm = typeof svg.getScreenCTM === 'function' ? svg.getScreenCTM() : null;
      const rect = svg.getBoundingClientRect();
      let lastX = e.clientX;
      const span = maxT - minT;
      const svgPerScreen = ctm && ctm.a !== 0 ? 1 / ctm.a : width / (rect.width || 1);
      suppressClickRef.current = true;

      const onMove = (ev: PointerEvent) => {
        const dx = ev.clientX - lastX;
        lastX = ev.clientX;
        onPanSeconds(-(dx * svgPerScreen * span) / plotW);
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      return;
    }

    // Left click -> 2D Box Zoom or Click
    if (e.button === 0) {
      const svg = svgRef.current;
      if (!svg) return;
      const getSvgCoords = (clientX: number, clientY: number) => {
        const pt = clientToSvgCoords(svg, clientX, clientY, width, height);
        return {
          x: Math.max(padLeft, Math.min(width - padRight, pt.x)),
          y: Math.max(padTop, Math.min(height - padBottom, pt.y)),
        };
      };

      const startPt = getSvgCoords(e.clientX, e.clientY);
      const startSvgX = startPt.x;
      const startSvgY = startPt.y;
      let currentSvgX = startSvgX;
      let currentSvgY = startSvgY;
      suppressClickRef.current = false;

      const onMove = (ev: PointerEvent) => {
        const pt = getSvgCoords(ev.clientX, ev.clientY);
        currentSvgX = pt.x;
        currentSvgY = pt.y;
        if (Math.abs(currentSvgX - startSvgX) > 5 || Math.abs(currentSvgY - startSvgY) > 5) {
          suppressClickRef.current = true;
          setDragBox({ startX: startSvgX, startY: startSvgY, currentX: currentSvgX, currentY: currentSvgY });
        }
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
        const dx = Math.abs(currentSvgX - startSvgX);
        const dy = Math.abs(currentSvgY - startSvgY);
        if (dx > 6 || dy > 6) {
          if (dx > 6) {
            const xA = Math.min(startSvgX, currentSvgX);
            const xB = Math.max(startSvgX, currentSvgX);
            const tA = minT + ((xA - padLeft) / plotW) * (maxT - minT);
            const tB = minT + ((xB - padLeft) / plotW) * (maxT - minT);
            onZoomRange(tA, tB);
          }
          if (dy > 12) {
            const yA = Math.min(startSvgY, currentSvgY);
            const yB = Math.max(startSvgY, currentSvgY);
            const vTop = maxY - ((yA - padTop) / plotH) * (maxY - minY);
            const vBottom = maxY - ((yB - padTop) / plotH) * (maxY - minY);
            const vMin = Math.min(vTop, vBottom);
            const vMax = Math.max(vTop, vBottom);
            if (vMax - vMin > 1e-12) {
              onZoomYRange({ min: vMin, max: vMax });
            }
          }
        }
        setDragBox(null);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
    }
  };

  return (
    <div className="full-waveform-wrap">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${width} ${height}`}
        className="full-waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
        onClick={handleClick}
        onDoubleClick={onResetZoom}
        onContextMenu={handleContextMenu}
        onPointerDown={handlePointerDown}
      >
        <rect width={width} height={height} className="waveform-bg" rx={8} />
        <rect
          x={padLeft}
          y={padTop}
          width={plotW}
          height={plotH}
          className="waveform-plot-area"
          rx={4}
        />

        <defs>
          <clipPath id="overlay-plot-clip">
            <rect x={padLeft} y={padTop} width={plotW} height={plotH} />
          </clipPath>
        </defs>

        {/* Y Gridlines */}
        {yTicks.map(({ val, y }, i) => (
          <g key={i}>
            <line
              x1={padLeft}
              y1={y}
              x2={width - padRight}
              y2={y}
              stroke={gridColor}
              strokeDasharray="2 3"
              strokeWidth={i === 4 ? 1.2 : 0.75}
            />
            <text
              x={padLeft - 8}
              y={y + 4}
              textAnchor="end"
              fill={textColor}
              fontSize={10}
              fontFamily="monospace"
            >
              {formatEngineeringValue(val, '')}
            </text>
          </g>
        ))}

        {/* X Gridlines */}
        {xTicks.map(({ val, x }, i) => (
          <g key={i}>
            <line
              x1={x}
              y1={padTop}
              x2={x}
              y2={height - padBottom}
              stroke={gridColor}
              strokeDasharray="2 3"
              strokeWidth={i === 5 ? 1.2 : 0.75}
            />
            <text
              x={x}
              y={height - padBottom + 16}
              fill={textColor}
              fontSize={10}
              fontWeight={600}
              textAnchor="middle"
              fontFamily="monospace"
            >
              {formatEngineeringValue(val, 's')}
            </text>
          </g>
        ))}

        {/* Center-Axis Subdivision Ticks (Classic DSO graticule) */}
        {centerTicks.map((t, idx) => (
          <line
            key={`ct-${idx}`}
            x1={t.x1}
            y1={t.y1}
            x2={t.x2}
            y2={t.y2}
            stroke={gridColor}
            strokeWidth={1}
            opacity={0.65}
          />
        ))}

        {/* Zero Line */}
        {minY <= 0 && maxY >= 0 && (
          <line
            x1={padLeft}
            y1={mapY(0)}
            x2={width - padRight}
            y2={mapY(0)}
            stroke={zeroColor}
            strokeWidth={1.2}
          />
        )}

        {/* Waveform Traces (clipped to the visible window) */}
        <g clipPath="url(#overlay-plot-clip)">
        {sweepPaths.map((ov) => (
          <path
            key={`sweep-${ov.runIndex}-${ov.signal}`}
            data-testid={`sweep-trace-${ov.runIndex}-${ov.signal}`}
            d={ov.path}
            fill="none"
            stroke={ov.color}
            strokeWidth={ov.highlighted ? 2.6 : 1.5}
            strokeOpacity={hasAnyHighlightedSweep && !ov.highlighted ? 0.25 : 0.8}
            strokeLinejoin="round"
            pointerEvents="none"
          />
        ))}
        {baselinePaths.map(({ name, path }) => (
          <path
            key={`baseline-${name}`}
            data-testid={`baseline-trace-${name}`}
            d={path}
            fill="none"
            stroke={colorOf(name)}
            strokeOpacity={0.42}
            strokeDasharray="5 4"
            strokeWidth={1.5}
            strokeLinejoin="round"
            pointerEvents="none"
          />
        ))}
        {tracePaths.map(({ name, path }) => {
          const color = colorOf(name);
          const isHighlighted = currentHoveredSignal === name;
          const isDimmed = currentHoveredSignal !== null && !isHighlighted;
          return (
            <path
              key={name}
              d={path}
              fill="none"
              stroke={color}
              strokeWidth={isHighlighted ? 3.2 : 2.2}
              opacity={isDimmed ? 0.25 : 1}
              strokeLinejoin="round"
              style={{ transition: 'opacity 0.15s ease, stroke-width 0.15s ease' }}
            />
          );
        })}
        </g>

        {/* Signal End-Trace Badges (Direct Signal Identification by Name) */}
        {traceBadges.map(({ name, color, y, targetY, val }) => {
          const isHovered = currentHoveredSignal === name;
          const isDimmed = currentHoveredSignal !== null && !isHovered;
          const badgeWidth = Math.max(46, Math.min(110, name.length * 7.5 + 16));
          const badgeHeight = 18;
          const badgeX = width - padRight - badgeWidth - 4;
          const badgeY = y - badgeHeight / 2;

          return (
            <g
              key={`trace-badge-${name}`}
              className={`trace-end-badge ${isHovered ? 'hovered' : ''}`}
              style={{ cursor: 'pointer', transition: 'opacity 0.15s ease' }}
              opacity={isDimmed ? 0.35 : 1}
              onMouseEnter={() => setHovered(name)}
              onMouseLeave={() => setHovered(null)}
              onClick={(e) => {
                e.stopPropagation();
                onToggleSignal?.(name);
              }}
            >
              <title>{`${name}: ${formatEngineeringValue(val)} (Click to toggle)`}</title>
              {Math.abs(y - targetY) > 2 && (
                <line
                  x1={width - padRight}
                  y1={targetY}
                  x2={width - padRight - 4}
                  y2={y}
                  stroke={color}
                  strokeWidth={1.2}
                  strokeDasharray="2 2"
                  opacity={0.7}
                />
              )}
              <polygon
                points={`${width - padRight},${targetY} ${width - padRight - 5},${targetY - 3} ${width - padRight - 5},${targetY + 3}`}
                fill={color}
              />
              <rect
                x={badgeX}
                y={badgeY}
                width={badgeWidth}
                height={badgeHeight}
                rx={4}
                fill={theme === 'light' ? 'rgba(255, 255, 255, 0.92)' : 'rgba(15, 23, 42, 0.92)'}
                stroke={color}
                strokeWidth={isHovered ? 2 : 1.2}
              />
              <circle cx={badgeX + 8} cy={y} r={3} fill={color} />
              <text
                x={badgeX + 15}
                y={y + 3.5}
                fill={color}
                fontSize={10}
                fontWeight={700}
                fontFamily="sans-serif"
              >
                {name}
              </text>
            </g>
          );
        })}

        {/* Cursor A */}
        {cursorsEnabled && cursorA !== null && time[cursorA] !== undefined && (
          <g className="cursor-line-a">
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={height - padBottom}
              strokeWidth={1.5}
              pointerEvents="none"
              style={{ stroke: 'var(--accent)' }}
            />
            {/* Wide grab area over entire vertical line */}
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={height - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            />
            <g
              className="cursor-handle cursor-handle-a"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            >
              <rect
                x={mapX(time[cursorA]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                style={{ fill: 'var(--accent)' }}
              />
              <text
                x={mapX(time[cursorA])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                A
              </text>
            </g>
          </g>
        )}

        {/* Cursor B */}
        {cursorsEnabled && cursorB !== null && time[cursorB] !== undefined && (
          <g className="cursor-line-b">
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={height - padBottom}
              stroke="#f43f5e"
              strokeWidth={1.5}
              pointerEvents="none"
            />
            {/* Wide grab area over entire vertical line */}
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={height - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            />
            <g
              className="cursor-handle cursor-handle-b"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            >
              <rect
                x={mapX(time[cursorB]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                fill="#e11d48"
              />
              <text
                x={mapX(time[cursorB])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                B
              </text>
            </g>
          </g>
        )}

        {/* Drag Box Zoom Selection (2D box or Time-only) */}
        {dragBox && (Math.abs(dragBox.currentX - dragBox.startX) > 5 || Math.abs(dragBox.currentY - dragBox.startY) > 5) && (() => {
          const bx = Math.min(dragBox.startX, dragBox.currentX);
          const by = Math.min(dragBox.startY, dragBox.currentY);
          const bw = Math.abs(dragBox.currentX - dragBox.startX);
          const bh = Math.abs(dragBox.currentY - dragBox.startY);
          const is2D = bh > 12;
          const tA = minT + ((bx - padLeft) / plotW) * (maxT - minT);
          const tB = minT + (((bx + bw) - padLeft) / plotW) * (maxT - minT);
          const dt = tB - tA;

          return (
            <g pointerEvents="none">
              <rect
                x={bx}
                y={is2D ? by : padTop}
                width={bw}
                height={is2D ? bh : plotH}
                fill="rgba(56, 189, 248, 0.22)"
                style={{ stroke: 'var(--accent)' }}
                strokeWidth={1.5}
                strokeDasharray="4 3"
              />
              <rect
                x={bx}
                y={Math.max(padTop, (is2D ? by : padTop) - 22)}
                width={is2D ? 160 : 130}
                height={18}
                rx={3}
                style={{ fill: 'var(--accent)' }}
                opacity={0.95}
              />
              <text
                x={bx + 6}
                y={Math.max(padTop, (is2D ? by : padTop) - 22) + 13}
                fill="#ffffff"
                fontSize={10}
                fontWeight={700}
              >
                🔍 {is2D ? '2D Box Zoom' : 'Time Zoom'} ({formatEngineeringValue(dt, 's')})
              </text>
            </g>
          );
        })()}

        {/* Hover Crosshair & Values */}
        {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
          <g className="hover-crosshair" pointerEvents="none">
            <line
              x1={mapX(time[hoverIndex])}
              y1={padTop}
              x2={mapX(time[hoverIndex])}
              y2={height - padBottom}
              stroke={crosshairColor}
              strokeWidth={1}
              strokeDasharray="3 3"
            />
            <rect
              x={Math.max(padLeft, Math.min(width - padRight - 72, mapX(time[hoverIndex]) - 36))}
              y={height - padBottom + 4}
              width={72}
              height={18}
              rx={4}
              fill="#0f172a"
              stroke="#38bdf8"
              strokeWidth={1.2}
              className="hover-pill"
            />
            <text
              x={Math.max(padLeft + 36, Math.min(width - padRight - 36, mapX(time[hoverIndex])))}
              y={height - padBottom + 17}
              textAnchor="middle"
              fill="#38bdf8"
              fontSize={10.5}
              fontWeight={700}
              fontFamily="monospace"
              className="hover-text"
            >
              {formatEngineeringValue(time[hoverIndex], 's')}
            </text>
            {activeSignals.map((name) => {
              const val = signals[name]?.[hoverIndex];
              if (val === undefined) return null;
              const color = colorOf(name);
              return (
                <circle
                  key={name}
                  cx={mapX(time[hoverIndex])}
                  cy={mapY(val)}
                  r={4.5}
                  fill={color}
                  stroke="#ffffff"
                  strokeWidth={1.5}
                />
              );
            })}
          </g>
        )}
      </svg>

      {/* Floating Hover Tooltip */}
      {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
        <WaveformTooltip
          time={time}
          hoverIndex={hoverIndex}
          activeSignals={activeSignals}
          signals={signals}
          colorOf={colorOf}
        />
      )}
    </div>
  );
}

/** Full-Screen Multi-Graph (2 or 3 subplots) Waveform Chart with independent Y-axes & synchronized X-axis */
function FullScreenMultiGraphChart({
  numGraphs,
  width = 1000,
  height = 480,
  time,
  signals,
  activeSignals,
  signalGraphs,
  colorOf,
  theme,
  hoverIndex,
  onHoverIndex,
  cursorA,
  cursorB,
  cursorsEnabled,
  activeCursor,
  onSetActiveCursor,
  onSetCursor,
  viewStart,
  viewEnd,
  onZoomAt,
  onZoomRange,
  onResetZoom,
  onPanSeconds,
  hoveredSignal,
  onOpenContextMenu,
  baselineResults = null,
  sweepOverlays = [],
  sweepSignal: _sweepSignal = '',
}: {
  numGraphs: 2 | 3;
  width?: number;
  height?: number;
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  signalGraphs: Record<string, number>;
  onMoveSignalToGraph?: (signal: string, targetGraph: number) => void;
  colorOf: (name: string) => string;
  theme: 'dark' | 'light';
  hoverIndex: number | null;
  onHoverIndex: (idx: number | null) => void;
  cursorA: number | null;
  cursorB: number | null;
  cursorsEnabled?: boolean;
  activeCursor?: 'A' | 'B';
  onSetActiveCursor?: (c: 'A' | 'B') => void;
  onSetCursor: (type: 'A' | 'B', idx: number) => void;
  viewStart: number;
  viewEnd: number;
  onZoomAt: (factor: number, anchor: number) => void;
  onZoomRange: (start: number, end: number) => void;
  onResetZoom: () => void;
  onPanSeconds: (delta: number) => void;
  hoveredSignal?: string | null;
  onHoverSignal?: (s: string | null) => void;
  onToggleSignal?: (s: string) => void;
  onOpenContextMenu?: (x: number, y: number, sampleIdx: number | null) => void;
  baselineResults?: Record<string, number[]> | null;
  sweepOverlays?: SweepOverlayTrace[];
  sweepSignal?: string;
}) {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [dragBox, setDragBox] = useState<{ startX: number; currentX: number } | null>(null);
  const suppressContextMenuRef = useRef(false);
  const suppressClickRef = useRef(false);

  const padLeft = 72;
  const padRight = 24;
  const padTop = 22;
  const padBottom = 34;
  const laneGap = 14;
  const count = numGraphs;
  const availableH = height - padTop - padBottom;
  const laneH = Math.max(42, Math.floor((availableH - (count - 1) * laneGap) / count));
  const totalH = Math.max(height, padTop + padBottom + count * (laneH + laneGap) - laneGap);
  const plotW = Math.max(100, width - padLeft - padRight);

  const textColor = theme === 'light' ? '#64748b' : '#94a3b8';
  const gridColor = theme === 'light' ? 'rgba(0, 0, 0, 0.06)' : 'rgba(255, 255, 255, 0.07)';
  const zeroColor = theme === 'light' ? '#cbd5e1' : '#475569';
  const crosshairColor = theme === 'light' ? '#334155' : '#cbd5e1';

  const t0 = viewStart;
  const t1 = viewEnd;
  const mapX = (t: number) => padLeft + ((t - t0) / (t1 - t0 || 1)) * plotW;

  useEffect(() => {
    const svg = svgRef.current;
    if (!svg) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const svgX = clientToSvgCoords(svg, e.clientX, e.clientY, width, totalH).x;
      const anchor = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
      onZoomAt(Math.exp(e.deltaY / 500), anchor);
    };
    svg.addEventListener('wheel', onWheel, { passive: false });
    return () => svg.removeEventListener('wheel', onWheel);
  }, [t0, t1, onZoomAt, width, totalH, plotW]);

  const handleMouseMove = (e: ReactMouseEvent<SVGSVGElement>) => {
    const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, totalH).x;
    if (svgX < padLeft || svgX > width - padRight) {
      onHoverIndex(null);
      return;
    }
    const t = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
    onHoverIndex(sampleIndexAt(time, t));
  };

  const handleCursorDragStart = (e: React.PointerEvent, type: 'A' | 'B') => {
    e.stopPropagation();
    e.preventDefault();
    onSetActiveCursor?.(type);
    const svg = svgRef.current;
    if (!svg) return;

    const onMove = (ev: PointerEvent) => {
      const svgX = clientToSvgCoords(svg, ev.clientX, ev.clientY, width, totalH).x;
      const clampedX = Math.max(padLeft, Math.min(width - padRight, svgX));
      const t = t0 + ((clampedX - padLeft) / plotW) * (t1 - t0);
      const newIdx = sampleIndexAt(time, t);
      onSetCursor(type, newIdx);
    };

    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  const handleClick = (e: ReactMouseEvent<SVGSVGElement>) => {
    if (suppressClickRef.current) {
      suppressClickRef.current = false;
      return;
    }
    const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, totalH).x;
    if (svgX < padLeft || svgX > width - padRight) return;
    const t = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
    const targetIdx = sampleIndexAt(time, t);

    let targetCursor: 'A' | 'B' = 'A';
    if (e.shiftKey) {
      targetCursor = 'B';
      onSetActiveCursor?.('B');
    } else if (cursorA === null || !cursorsEnabled) {
      targetCursor = 'A';
      onSetActiveCursor?.('B');
    } else if (cursorB === null) {
      targetCursor = 'B';
      onSetActiveCursor?.('A');
    } else {
      targetCursor = activeCursor ?? 'A';
    }
    onSetCursor(targetCursor, targetIdx);
  };

  const handlePointerDown = (e: React.PointerEvent<SVGSVGElement>) => {
    if (e.button === 2) {
      const startX = e.clientX;
      let lastX = startX;
      suppressContextMenuRef.current = false;
      const svg = e.currentTarget;
      const ctm = typeof svg.getScreenCTM === 'function' ? svg.getScreenCTM() : null;
      const rect = svg.getBoundingClientRect();
      const svgPerScreen = ctm && ctm.a !== 0 ? 1 / ctm.a : width / (rect.width || 1);

      const onMove = (ev: PointerEvent) => {
        if (Math.abs(ev.clientX - startX) > 4) {
          suppressContextMenuRef.current = true;
        }
        const deltaPx = ev.clientX - lastX;
        lastX = ev.clientX;
        const deltaSeconds = -(deltaPx * svgPerScreen / plotW) * (t1 - t0);
        onPanSeconds(deltaSeconds);
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      return;
    }

    if (e.button === 0) {
      const svg = svgRef.current;
      if (!svg) return;
      const getSvgX = (clientX: number, clientY: number) => {
        const sx = clientToSvgCoords(svg, clientX, clientY, width, totalH).x;
        return Math.max(padLeft, Math.min(width - padRight, sx));
      };

      const startSvgX = getSvgX(e.clientX, e.clientY);
      let currentSvgX = startSvgX;
      suppressClickRef.current = false;

      const onMove = (ev: PointerEvent) => {
        currentSvgX = getSvgX(ev.clientX, ev.clientY);
        if (Math.abs(currentSvgX - startSvgX) > 5) {
          suppressClickRef.current = true;
          setDragBox({ startX: startSvgX, currentX: currentSvgX });
        }
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
        if (Math.abs(currentSvgX - startSvgX) > 5) {
          const xA = Math.min(startSvgX, currentSvgX);
          const xB = Math.max(startSvgX, currentSvgX);
          const tA = t0 + ((xA - padLeft) / plotW) * (t1 - t0);
          const tB = t0 + ((xB - padLeft) / plotW) * (t1 - t0);
          onZoomRange(tA, tB);
        }
        setDragBox(null);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
    }
  };

  const iWin0 = sampleIndexAt(time, t0);
  const iWin1 = Math.min(sampleIndexAt(time, t1), time.length - 1);
  const winLen = Math.max(1, iWin1 - iWin0 + 1);
  const step = Math.max(1, Math.floor(winLen / 3000));
  const bTime = baselineResults?.['time'] || [];
  const bWin0 = bTime.length > 0 ? sampleIndexAt(bTime, t0) : 0;
  const bWin1 = bTime.length > 0 ? Math.min(sampleIndexAt(bTime, t1), bTime.length - 1) : -1;

  const hasAnyHighlightedSweep = sweepOverlays.some((ov) => ov.highlighted);
  const layoutMode = numGraphs === 2 ? '2-graphs' : '3-graphs';
  const graphData: {
    graphIdx: number;
    signals: string[];
    minY: number;
    maxY: number;
    paths: Record<string, string>;
    baselinePaths: Record<string, string>;
    sweepPaths: { runIndex: number; signal: string; color: string; path: string; highlighted: boolean }[];
    primaryUnit: string;
    laneY: number;
    mapLaneY: (v: number) => number;
  }[] = [];

  for (let g = 1; g <= numGraphs; g++) {
    const gSignals = activeSignals.filter((s) => resolveSignalGraph(s, layoutMode, signalGraphs) === g);
    let y0 = Infinity;
    let y1 = -Infinity;
    const unitsCount: Record<string, number> = {};

    for (const name of gSignals) {
      const u = inferSignalUnit(name);
      if (u) unitsCount[u] = (unitsCount[u] || 0) + 1;
      const arr = signals[name] || [];
      for (let i = iWin0; i <= iWin1; i++) {
        const v = arr[i];
        if (v !== undefined) {
          if (v < y0) y0 = v;
          if (v > y1) y1 = v;
        }
      }
      if (baselineResults && bTime.length > 0) {
        const bArr = baselineResults[name] || [];
        for (let i = bWin0; i <= bWin1 && i < bArr.length; i++) {
          const v = bArr[i];
          if (v !== undefined) {
            if (v < y0) y0 = v;
            if (v > y1) y1 = v;
          }
        }
      }
      if (sweepOverlays.length > 0) {
        for (const ov of sweepOverlays) {
          const sArr = ov.signals[name] || [];
          const s0 = ov.time.length > 0 ? sampleIndexAt(ov.time, t0) : 0;
          const s1 = ov.time.length > 0 ? Math.min(sampleIndexAt(ov.time, t1), sArr.length - 1) : -1;
          for (let i = s0; i <= s1; i++) {
            const v = sArr[i];
            if (v !== undefined) {
              if (v < y0) y0 = v;
              if (v > y1) y1 = v;
            }
          }
        }
      }
    }

    if (!Number.isFinite(y0) || !Number.isFinite(y1)) {
      y0 = -1;
      y1 = 1;
    }
    if (y0 === y1) {
      y0 -= 1;
      y1 += 1;
    }
    const yPad = (y1 - y0) * 0.12;
    const minY = y0 - yPad;
    const maxY = y1 + yPad;

    const laneY = padTop + (g - 1) * (laneH + laneGap);
    const mapLaneY = (v: number) => laneY + laneH - ((v - minY) / (maxY - minY || 1)) * laneH;

    const paths: Record<string, string> = {};
    const baselinePaths: Record<string, string> = {};
    const sweepPaths: { runIndex: number; signal: string; color: string; path: string; highlighted: boolean }[] = [];
    for (const name of gSignals) {
      const arr = signals[name] || [];
      let pathD = '';
      if (winLen > 0 && arr.length > 0) {
        const startIdx = Math.max(0, Math.min(iWin0, arr.length - 1));
        const endIdx = Math.max(0, Math.min(iWin1, arr.length - 1));
        if (startIdx <= endIdx) {
          pathD = `M ${mapX(time[startIdx])} ${mapLaneY(arr[startIdx] ?? 0)}`;
          for (let i = startIdx + step; i <= endIdx; i += step) {
            pathD += ` L ${mapX(time[i])} ${mapLaneY(arr[i] ?? 0)}`;
          }
          if ((endIdx - startIdx) % step !== 0) {
            pathD += ` L ${mapX(time[endIdx])} ${mapLaneY(arr[endIdx] ?? 0)}`;
          }
        }
      }
      paths[name] = pathD;
      if (baselineResults && bTime.length > 0 && baselineResults[name]?.length) {
        baselinePaths[name] = buildTraceSvgPath(bTime, baselineResults[name], t0, t1, mapX, mapLaneY);
      }
      if (sweepOverlays.length > 0) {
        for (const ov of sweepOverlays) {
          const sArr = ov.signals[name];
          if (sArr?.length && ov.time.length) {
            const p = buildTraceSvgPath(ov.time, sArr, t0, t1, mapX, mapLaneY);
            if (p) {
              sweepPaths.push({ runIndex: ov.runIndex, signal: name, color: ov.color, path: p, highlighted: ov.highlighted });
            }
          }
        }
      }
    }

    let primaryUnit = '';
    let maxUnitCount = 0;
    for (const [u, cnt] of Object.entries(unitsCount)) {
      if (cnt > maxUnitCount) {
        maxUnitCount = cnt;
        primaryUnit = u;
      }
    }

    graphData.push({
      graphIdx: g,
      signals: gSignals,
      minY,
      maxY,
      paths,
      baselinePaths,
      sweepPaths,
      primaryUnit,
      laneY,
      mapLaneY,
    });
  }

  // Time ticks for bottom axis
  const numTimeTicks = Math.max(4, Math.floor(plotW / 120));
  const timeTicks: number[] = [];
  for (let i = 0; i <= numTimeTicks; i++) {
    timeTicks.push(t0 + (i / numTimeTicks) * (t1 - t0));
  }

  return (
    <div className="full-waveform-wrap">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${width} ${totalH}`}
        className="full-waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
        onClick={handleClick}
        onDoubleClick={onResetZoom}
        onContextMenu={(e) => {
          e.preventDefault();
          if (suppressContextMenuRef.current) {
            suppressContextMenuRef.current = false;
            return;
          }
          if (!onOpenContextMenu) return;
          const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, totalH).x;
          const inPlot = svgX >= padLeft && svgX <= width - padRight && time.length > 0;
          const targetIdx = inPlot
            ? sampleIndexAt(time, t0 + ((svgX - padLeft) / plotW) * (t1 - t0))
            : null;
          onOpenContextMenu(e.clientX, e.clientY, targetIdx);
        }}
        onPointerDown={handlePointerDown}
      >
        <rect width={width} height={totalH} className="waveform-bg" rx={8} />

        <defs>
          {graphData.map((gd) => (
            <clipPath key={`clip-${gd.graphIdx}`} id={`multi-graph-clip-${gd.graphIdx}`}>
              <rect x={padLeft} y={gd.laneY} width={plotW} height={laneH} />
            </clipPath>
          ))}
        </defs>

        {/* Subplots */}
        {graphData.map((gd) => {
          const zeroY =
            gd.minY <= 0 && gd.maxY >= 0 ? gd.mapLaneY(0) : null;

          return (
            <g key={`graph-${gd.graphIdx}`} className="multi-graph-subplot">
              {/* Plot Background */}
              <rect
                x={padLeft}
                y={gd.laneY}
                width={plotW}
                height={laneH}
                className="waveform-plot-area"
                rx={4}
              />

              {/* Horizontal Grid lines */}
              {[0.25, 0.5, 0.75].map((pct) => (
                <line
                  key={pct}
                  x1={padLeft}
                  y1={gd.laneY + pct * laneH}
                  x2={width - padRight}
                  y2={gd.laneY + pct * laneH}
                  stroke={gridColor}
                  strokeWidth={1}
                  strokeDasharray="2 3"
                />
              ))}

              {/* Vertical time grid lines */}
              {timeTicks.map((t, idx) => (
                <line
                  key={idx}
                  x1={mapX(t)}
                  y1={gd.laneY}
                  x2={mapX(t)}
                  y2={gd.laneY + laneH}
                  stroke={gridColor}
                  strokeWidth={1}
                  strokeDasharray="2 3"
                />
              ))}

              {/* Zero line */}
              {zeroY !== null && (
                <line
                  x1={padLeft}
                  y1={zeroY}
                  x2={width - padRight}
                  y2={zeroY}
                  stroke={zeroColor}
                  strokeWidth={1}
                  strokeDasharray="3 3"
                />
              )}

              {/* Y Axis Labels (Top, Mid, Bottom) */}
              <text
                x={padLeft - 8}
                y={gd.laneY + 12}
                textAnchor="end"
                fill={textColor}
                fontSize={10}
                fontFamily="monospace"
              >
                {formatEngineeringValue(gd.maxY)} {gd.primaryUnit}
              </text>
              <text
                x={padLeft - 8}
                y={gd.laneY + laneH / 2 + 4}
                textAnchor="end"
                fill={textColor}
                fontSize={9.5}
                fontFamily="monospace"
              >
                {formatEngineeringValue((gd.minY + gd.maxY) / 2)}
              </text>
              <text
                x={padLeft - 8}
                y={gd.laneY + laneH - 4}
                textAnchor="end"
                fill={textColor}
                fontSize={10}
                fontFamily="monospace"
              >
                {formatEngineeringValue(gd.minY)} {gd.primaryUnit}
              </text>

              {/* Waveform Paths */}
              {gd.sweepPaths.map((sp) => (
                <path
                  key={`sweep-${sp.runIndex}-${sp.signal}`}
                  data-testid={`sweep-trace-${sp.runIndex}-${sp.signal}`}
                  d={sp.path}
                  fill="none"
                  stroke={sp.color}
                  strokeWidth={sp.highlighted ? 2.4 : 1.3}
                  strokeOpacity={hasAnyHighlightedSweep && !sp.highlighted ? 0.25 : 0.78}
                  clipPath={`url(#multi-graph-clip-${gd.graphIdx})`}
                  pointerEvents="none"
                />
              ))}
              {gd.signals.map((name) => {
                const bPath = gd.baselinePaths[name];
                if (!bPath) return null;
                return (
                  <path
                    key={`baseline-${name}`}
                    data-testid={`baseline-trace-${name}`}
                    d={bPath}
                    fill="none"
                    stroke={colorOf(name)}
                    strokeOpacity={0.42}
                    strokeDasharray="5 4"
                    strokeWidth={1.5}
                    clipPath={`url(#multi-graph-clip-${gd.graphIdx})`}
                    pointerEvents="none"
                  />
                );
              })}
              {gd.signals.map((name) => {
                const color = colorOf(name);
                const isHovered = hoveredSignal === name;
                return (
                  <path
                    key={name}
                    d={gd.paths[name] || ''}
                    fill="none"
                    stroke={color}
                    strokeWidth={isHovered ? 2.6 : 1.8}
                    clipPath={`url(#multi-graph-clip-${gd.graphIdx})`}
                    style={{
                      transition: 'stroke-width 0.15s ease',
                      filter: isHovered ? `drop-shadow(0 0 4px ${color})` : undefined,
                    }}
                  />
                );
              })}

              {/* Subtle Subplot Identifier Inside Top-Left of Plot Box (never collides with Y-axis numbers) */}
              <g pointerEvents="none">
                <rect
                  x={padLeft + 6}
                  y={gd.laneY + 5}
                  width={22}
                  height={14}
                  rx={3}
                  fill="var(--panel)"
                  fillOpacity={0.78}
                  stroke="var(--border)"
                  strokeWidth={0.75}
                />
                <text
                  x={padLeft + 17}
                  y={gd.laneY + 15}
                  textAnchor="middle"
                  fill={textColor}
                  fontSize={9}
                  fontWeight={700}
                >
                  G{gd.graphIdx}
                </text>
              </g>

              {/* Empty graph state placeholder */}
              {gd.signals.length === 0 && (
                <text
                  x={padLeft + plotW / 2}
                  y={gd.laneY + laneH / 2}
                  textAnchor="middle"
                  fill={textColor}
                  fontSize={11}
                  fontStyle="italic"
                >
                  (No traces in Graph {gd.graphIdx})
                </text>
              )}
            </g>
          );
        })}

        {/* Synchronized Bottom Time Axis */}
        <g className="time-axis" pointerEvents="none">
          <line
            x1={padLeft}
            y1={totalH - padBottom}
            x2={width - padRight}
            y2={totalH - padBottom}
            stroke={gridColor}
            strokeWidth={1.5}
          />
          {timeTicks.map((t, idx) => (
            <g key={idx} transform={`translate(${mapX(t)}, ${totalH - padBottom})`}>
              <line x1={0} y1={0} x2={0} y2={4} stroke={textColor} strokeWidth={1} />
              <text
                x={0}
                y={15}
                textAnchor="middle"
                fill={textColor}
                fontSize={10}
                fontFamily="monospace"
              >
                {formatEngineeringValue(t, 's')}
              </text>
            </g>
          ))}
        </g>

        {/* Measurement Cursor A */}
        {cursorsEnabled && cursorA !== null && cursorA >= 0 && cursorA < time.length && (
          <g className="cursor-a-group">
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={totalH - padBottom}
              stroke="#0ea5e9"
              strokeWidth={1.5}
              pointerEvents="none"
            />
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={totalH - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            />
            <g
              className="cursor-handle cursor-handle-a"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            >
              <rect
                x={mapX(time[cursorA]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                fill="#0ea5e9"
              />
              <text
                x={mapX(time[cursorA])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                A
              </text>
            </g>
          </g>
        )}

        {/* Measurement Cursor B */}
        {cursorsEnabled && cursorB !== null && cursorB >= 0 && cursorB < time.length && (
          <g className="cursor-b-group">
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={totalH - padBottom}
              stroke="#e11d48"
              strokeWidth={1.5}
              pointerEvents="none"
            />
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={totalH - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            />
            <g
              className="cursor-handle cursor-handle-b"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            >
              <rect
                x={mapX(time[cursorB]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                fill="#e11d48"
              />
              <text
                x={mapX(time[cursorB])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                B
              </text>
            </g>
          </g>
        )}

        {/* Drag Box Zoom Selection */}
        {dragBox && Math.abs(dragBox.currentX - dragBox.startX) > 5 && (
          <g pointerEvents="none">
            <rect
              x={Math.min(dragBox.startX, dragBox.currentX)}
              y={padTop}
              width={Math.abs(dragBox.currentX - dragBox.startX)}
              height={totalH - padTop - padBottom}
              fill="rgba(56, 189, 248, 0.22)"
              style={{ stroke: 'var(--accent)' }}
              strokeWidth={1.5}
              strokeDasharray="4 3"
            />
            <rect
              x={Math.min(dragBox.startX, dragBox.currentX)}
              y={padTop + 2}
              width={120}
              height={18}
              rx={3}
              style={{ fill: 'var(--accent)' }}
              opacity={0.9}
            />
            <text
              x={Math.min(dragBox.startX, dragBox.currentX) + 6}
              y={padTop + 15}
              fill="#ffffff"
              fontSize={10}
              fontWeight={700}
            >
              🔍 Time Zoom
            </text>
          </g>
        )}

        {/* Hover Crosshair & Indicators */}
        {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
          <g className="hover-crosshair" pointerEvents="none">
            <line
              x1={mapX(time[hoverIndex])}
              y1={padTop}
              x2={mapX(time[hoverIndex])}
              y2={totalH - padBottom}
              stroke={crosshairColor}
              strokeWidth={1}
              strokeDasharray="3 3"
            />
            <rect
              x={Math.max(padLeft, Math.min(width - padRight - 72, mapX(time[hoverIndex]) - 36))}
              y={totalH - padBottom + 4}
              width={72}
              height={18}
              rx={4}
              fill="#0f172a"
              stroke="#38bdf8"
              strokeWidth={1.2}
              className="hover-pill"
            />
            <text
              x={Math.max(padLeft + 36, Math.min(width - padRight - 36, mapX(time[hoverIndex])))}
              y={totalH - padBottom + 17}
              textAnchor="middle"
              fill="#38bdf8"
              fontSize={10.5}
              fontWeight={700}
              fontFamily="monospace"
              className="hover-text"
            >
              {formatEngineeringValue(time[hoverIndex], 's')}
            </text>

            {/* Trace dots on curves */}
            {graphData.map((gd) =>
              gd.signals.map((name) => {
                const val = signals[name]?.[hoverIndex];
                if (val === undefined) return null;
                const dotY = gd.mapLaneY(val);
                const color = colorOf(name);
                return (
                  <circle
                    key={name}
                    cx={mapX(time[hoverIndex])}
                    cy={dotY}
                    r={4}
                    fill={color}
                    stroke="#ffffff"
                    strokeWidth={1.5}
                  />
                );
              }),
            )}
          </g>
        )}
      </svg>

      {/* Floating Hover Tooltip */}
      {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
        <WaveformTooltip
          time={time}
          hoverIndex={hoverIndex}
          activeSignals={activeSignals}
          signals={signals}
          colorOf={colorOf}
        />
      )}
    </div>
  );
}

/** Full-Screen Stacked Subplots Waveform Chart */
function FullScreenStackedChart({
  width = 1000,
  height = 480,
  time,
  signals,
  activeSignals,
  colorOf,
  theme,
  hoverIndex,
  onHoverIndex,
  cursorA,
  cursorB,
  cursorsEnabled,
  activeCursor,
  onSetActiveCursor,
  onSetCursor,
  viewStart,
  viewEnd,
  onZoomAt,
  onZoomRange,
  onResetZoom,
  onPanSeconds,
  onOpenContextMenu,
  baselineResults = null,
  sweepOverlays = [],
  sweepSignal: _sweepSignal = '',
}: {
  width?: number;
  height?: number;
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  colorOf: (name: string) => string;
  theme: 'dark' | 'light';
  hoverIndex: number | null;
  onHoverIndex: (idx: number | null) => void;
  cursorA: number | null;
  cursorB: number | null;
  cursorsEnabled?: boolean;
  activeCursor?: 'A' | 'B';
  onSetActiveCursor?: (c: 'A' | 'B') => void;
  onSetCursor: (type: 'A' | 'B', idx: number) => void;
  viewStart: number;
  viewEnd: number;
  onZoomAt: (factor: number, anchor: number) => void;
  onZoomRange: (start: number, end: number) => void;
  onResetZoom: () => void;
  onPanSeconds: (delta: number) => void;
  onOpenContextMenu?: (x: number, y: number, sampleIdx: number | null) => void;
  baselineResults?: Record<string, number[]> | null;
  sweepOverlays?: SweepOverlayTrace[];
  sweepSignal?: string;
}) {
  const hasAnyHighlightedSweep = sweepOverlays.some((ov) => ov.highlighted);
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [dragBox, setDragBox] = useState<{ startX: number; currentX: number } | null>(null);
  const suppressClickRef = useRef(false);
  const padLeft = 68;
  const padRight = 24;
  const padTop = 20;
  const padBottom = 30;
  const laneGap = 12;
  const count = activeSignals.length || 1;
  const availableH = height - padTop - padBottom;
  const idealLaneH = Math.max(64, Math.floor((availableH - (count - 1) * laneGap) / count));
  const laneH = idealLaneH;
  const totalH = Math.max(height, padTop + padBottom + count * (laneH + laneGap) - laneGap);
  const plotW = Math.max(100, width - padLeft - padRight);
  const plotH = totalH - padTop - padBottom;

  const textColor = theme === 'light' ? '#64748b' : '#94a3b8';
  const zeroColor = theme === 'light' ? '#cbd5e1' : '#475569';
  const crosshairColor = theme === 'light' ? '#334155' : '#cbd5e1';

  const t0 = viewStart;
  const t1 = viewEnd;

  const mapX = (t: number) => padLeft + ((t - t0) / (t1 - t0 || 1)) * plotW;

  // non-passive wheel zoom around the cursor position
  useEffect(() => {
    const svg = svgRef.current;
    if (!svg) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const svgX = clientToSvgCoords(svg, e.clientX, e.clientY, width, totalH).x;
      const anchor = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
      onZoomAt(Math.exp(e.deltaY / 500), anchor);
    };
    svg.addEventListener('wheel', onWheel, { passive: false });
    return () => svg.removeEventListener('wheel', onWheel);
  }, [t0, t1, onZoomAt, width, totalH, plotW]);

  const handleMouseMove = (e: ReactMouseEvent<SVGSVGElement>) => {
    const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, totalH).x;
    if (svgX < padLeft || svgX > width - padRight) {
      onHoverIndex(null);
      return;
    }

    const t = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
    onHoverIndex(sampleIndexAt(time, t));
  };

  const handleCursorDragStart = (e: React.PointerEvent, type: 'A' | 'B') => {
    e.stopPropagation();
    e.preventDefault();
    onSetActiveCursor?.(type);
    const svg = svgRef.current;
    if (!svg) return;

    const onMove = (ev: PointerEvent) => {
      const svgX = clientToSvgCoords(svg, ev.clientX, ev.clientY, width, totalH).x;
      const clampedX = Math.max(padLeft, Math.min(width - padRight, svgX));
      const t = t0 + ((clampedX - padLeft) / plotW) * (t1 - t0);
      const newIdx = sampleIndexAt(time, t);
      onSetCursor(type, newIdx);
    };

    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };

    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  const handleClick = (e: ReactMouseEvent<SVGSVGElement>) => {
    if (suppressClickRef.current) {
      suppressClickRef.current = false;
      return;
    }
    const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, totalH).x;
    if (svgX < padLeft || svgX > width - padRight) return;
    const t = t0 + ((svgX - padLeft) / plotW) * (t1 - t0);
    const targetIdx = sampleIndexAt(time, t);

    let targetCursor: 'A' | 'B' = 'A';
    if (e.shiftKey) {
      targetCursor = 'B';
      onSetActiveCursor?.('B');
    } else if (cursorA === null || !cursorsEnabled) {
      targetCursor = 'A';
      onSetActiveCursor?.('B');
    } else if (cursorB === null) {
      targetCursor = 'B';
      onSetActiveCursor?.('A');
    } else {
      targetCursor = activeCursor ?? 'A';
    }
    onSetCursor(targetCursor, targetIdx);
  };

  const handleContextMenu = (e: ReactMouseEvent<SVGSVGElement>) => {
    e.preventDefault();
    const svgX = clientToSvgCoords(e.currentTarget, e.clientX, e.clientY, width, totalH).x;
    const inPlot = svgX >= padLeft && svgX <= width - padRight && time.length > 0;
    const targetIdx = inPlot
      ? sampleIndexAt(time, t0 + ((svgX - padLeft) / plotW) * (t1 - t0))
      : null;
    if (onOpenContextMenu) {
      onOpenContextMenu(e.clientX, e.clientY, targetIdx);
    } else if (targetIdx !== null) {
      onSetCursor('B', targetIdx);
      onSetActiveCursor?.('B');
    }
  };

  const handlePointerDown = (e: ReactMouseEvent<SVGSVGElement>) => {
    // Middle click or Alt + Left click -> Pan
    if (e.button === 1 || (e.button === 0 && e.altKey)) {
      e.preventDefault();
      const svg = e.currentTarget;
      const ctm = typeof svg.getScreenCTM === 'function' ? svg.getScreenCTM() : null;
      const rect = svg.getBoundingClientRect();
      let lastX = e.clientX;
      const span = t1 - t0;
      const svgPerScreen = ctm && ctm.a !== 0 ? 1 / ctm.a : width / (rect.width || 1);
      suppressClickRef.current = true;

      const onMove = (ev: PointerEvent) => {
        const dx = ev.clientX - lastX;
        lastX = ev.clientX;
        onPanSeconds(-(dx * svgPerScreen * span) / plotW);
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      return;
    }

    // Left click -> Box Zoom or Click
    if (e.button === 0) {
      const svg = svgRef.current;
      if (!svg) return;
      const getSvgX = (clientX: number, clientY: number) => {
        const sx = clientToSvgCoords(svg, clientX, clientY, width, totalH).x;
        return Math.max(padLeft, Math.min(width - padRight, sx));
      };

      const startSvgX = getSvgX(e.clientX, e.clientY);
      let currentSvgX = startSvgX;
      suppressClickRef.current = false;

      const onMove = (ev: PointerEvent) => {
        currentSvgX = getSvgX(ev.clientX, ev.clientY);
        if (Math.abs(currentSvgX - startSvgX) > 5) {
          suppressClickRef.current = true;
          setDragBox({ startX: startSvgX, currentX: currentSvgX });
        }
      };

      const onUp = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
        if (Math.abs(currentSvgX - startSvgX) > 5) {
          const xA = Math.min(startSvgX, currentSvgX);
          const xB = Math.max(startSvgX, currentSvgX);
          const tA = t0 + ((xA - padLeft) / plotW) * (t1 - t0);
          const tB = t0 + ((xB - padLeft) / plotW) * (t1 - t0);
          onZoomRange(tA, tB);
        }
        setDragBox(null);
      };

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
    }
  };

  const signalMinMax: Record<string, { min: number; max: number }> = {};
  const lanePaths: Record<string, string> = {};
  const baselineLanePaths: Record<string, string> = {};
  const sweepLanePaths: Record<string, { runIndex: number; color: string; path: string; highlighted: boolean }[]> = {};

  const iWin0 = sampleIndexAt(time, t0);
  const iWin1 = Math.min(sampleIndexAt(time, t1), time.length - 1);
  const winLen = Math.max(1, iWin1 - iWin0 + 1);
  const step = Math.max(1, Math.floor(winLen / 3000));
  const bTime = baselineResults?.['time'] || [];
  const bWin0 = bTime.length > 0 ? sampleIndexAt(bTime, t0) : 0;
  const bWin1 = bTime.length > 0 ? Math.min(sampleIndexAt(bTime, t1), bTime.length - 1) : -1;

  for (let k = 0; k < activeSignals.length; k++) {
    const name = activeSignals[k];
    const arr = signals[name] || [];
    const laneY = padTop + k * (laneH + laneGap);

    let y0 = Infinity;
    let y1 = -Infinity;
    for (let i = iWin0; i <= iWin1; i++) {
      const v = arr[i];
      if (v !== undefined) {
        if (v < y0) y0 = v;
        if (v > y1) y1 = v;
      }
    }
    if (baselineResults && bTime.length > 0) {
      const bArr = baselineResults[name] || [];
      for (let i = bWin0; i <= bWin1 && i < bArr.length; i++) {
        const v = bArr[i];
        if (v !== undefined) {
          if (v < y0) y0 = v;
          if (v > y1) y1 = v;
        }
      }
    }
    if (sweepOverlays.length > 0) {
      for (const ov of sweepOverlays) {
        const sArr = ov.signals[name] || [];
        const s0 = ov.time.length > 0 ? sampleIndexAt(ov.time, t0) : 0;
        const s1 = ov.time.length > 0 ? Math.min(sampleIndexAt(ov.time, t1), sArr.length - 1) : -1;
        for (let i = s0; i <= s1; i++) {
          const v = sArr[i];
          if (v !== undefined) {
            if (v < y0) y0 = v;
            if (v > y1) y1 = v;
          }
        }
      }
    }
    if (!Number.isFinite(y0) || !Number.isFinite(y1)) {
      y0 = -1;
      y1 = 1;
    }
    if (y0 === y1) {
      y0 -= 1;
      y1 += 1;
    }
    const yPad = (y1 - y0) * 0.1;
    const minY = y0 - yPad;
    const maxY = y1 + yPad;
    signalMinMax[name] = { min: minY, max: maxY };

    const mapLaneY = (v: number) =>
      laneY + laneH - ((v - minY) / (maxY - minY || 1)) * laneH;

    let pathD = '';
    if (winLen > 0 && arr.length > 0) {
      const startIdx = Math.max(0, Math.min(iWin0, arr.length - 1));
      const endIdx = Math.max(0, Math.min(iWin1, arr.length - 1));
      if (startIdx <= endIdx) {
        pathD = `M ${mapX(time[startIdx])} ${mapLaneY(arr[startIdx] ?? 0)}`;
        for (let i = startIdx + step; i <= endIdx; i += step) {
          pathD += ` L ${mapX(time[i])} ${mapLaneY(arr[i] ?? 0)}`;
        }
        if ((endIdx - startIdx) % step !== 0) {
          pathD += ` L ${mapX(time[endIdx])} ${mapLaneY(arr[endIdx] ?? 0)}`;
        }
      }
    }
    lanePaths[name] = pathD;
    if (baselineResults && bTime.length > 0 && baselineResults[name]?.length) {
      baselineLanePaths[name] = buildTraceSvgPath(bTime, baselineResults[name], t0, t1, mapX, mapLaneY);
    }
    if (sweepOverlays.length > 0) {
      const sPaths: { runIndex: number; color: string; path: string; highlighted: boolean }[] = [];
      for (const ov of sweepOverlays) {
        const sArr = ov.signals[name];
        if (sArr?.length && ov.time.length) {
          const p = buildTraceSvgPath(ov.time, sArr, t0, t1, mapX, mapLaneY);
          if (p) sPaths.push({ runIndex: ov.runIndex, color: ov.color, path: p, highlighted: ov.highlighted });
        }
      }
      sweepLanePaths[name] = sPaths;
    }
  }

  return (
    <div className="full-waveform-wrap">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${width} ${totalH}`}
        className="full-waveform-svg"
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHoverIndex(null)}
        onClick={handleClick}
        onDoubleClick={onResetZoom}
        onContextMenu={handleContextMenu}
        onPointerDown={handlePointerDown}
      >
        <rect width={width} height={totalH} className="waveform-bg" rx={8} />

        <defs>
          <clipPath id="stacked-plot-clip">
            <rect x={padLeft} y={padTop} width={plotW} height={totalH - padTop - padBottom} />
          </clipPath>
        </defs>

        {/* Lanes */}
        {activeSignals.map((name, i) => {
          const laneY = padTop + i * (laneH + laneGap);
          const color = colorOf(name);
          const path = lanePaths[name];
          const bPath = baselineLanePaths[name];
          const sPaths = sweepLanePaths[name] || [];
          const mm = signalMinMax[name] || { min: 0, max: 1 };
          const zeroY =
            mm.min <= 0 && mm.max >= 0
              ? laneY + laneH - ((0 - mm.min) / (mm.max - mm.min || 1)) * laneH
              : null;

          return (
            <g key={name} className="stacked-lane">
              {/* Lane background */}
              <rect
                x={padLeft}
                y={laneY}
                width={plotW}
                height={laneH}
                className="waveform-plot-area"
                rx={4}
              />

              {/* Zero line */}
              {zeroY !== null && (
                <line
                  x1={padLeft}
                  y1={zeroY}
                  x2={width - padRight}
                  y2={zeroY}
                  stroke={zeroColor}
                  strokeWidth={1}
                />
              )}

              {/* Y Axis Labels (Min / Max) */}
              <text
                x={padLeft - 8}
                y={laneY + 12}
                textAnchor="end"
                fill={textColor}
                fontSize={10}
                fontFamily="monospace"
              >
                {formatEngineeringValue(mm.max, '')}
              </text>
              <text
                x={padLeft - 8}
                y={laneY + laneH - 2}
                textAnchor="end"
                fill={textColor}
                fontSize={10}
                fontFamily="monospace"
              >
                {formatEngineeringValue(mm.min, '')}
              </text>

              {/* Channel Name Badge */}
              <rect
                x={padLeft + 8}
                y={laneY + 8}
                width={Math.min(120, name.length * 8 + 16)}
                height={18}
                rx={3}
                fill={theme === 'light' ? 'rgba(255,255,255,0.85)' : 'rgba(15,23,42,0.85)'}
              />
              <text
                x={padLeft + 14}
                y={laneY + 21}
                fill={color}
                fontSize={11}
                fontWeight={700}
              >
                {name}
              </text>

              {/* Waveform */}
              <g clipPath="url(#stacked-plot-clip)">
                {sPaths.map((sp) => (
                  <path
                    key={`sweep-${sp.runIndex}-${name}`}
                    data-testid={`sweep-trace-${sp.runIndex}-${name}`}
                    d={sp.path}
                    fill="none"
                    stroke={sp.color}
                    strokeWidth={sp.highlighted ? 2.4 : 1.3}
                    strokeOpacity={hasAnyHighlightedSweep && !sp.highlighted ? 0.25 : 0.78}
                    strokeLinejoin="round"
                    pointerEvents="none"
                  />
                ))}
                {bPath && (
                  <path
                    data-testid={`baseline-trace-${name}`}
                    d={bPath}
                    fill="none"
                    stroke={color}
                    strokeOpacity={0.42}
                    strokeDasharray="5 4"
                    strokeWidth={1.5}
                    strokeLinejoin="round"
                    pointerEvents="none"
                  />
                )}
                {path && (
                  <path
                    d={path}
                    fill="none"
                    stroke={color}
                    strokeWidth={2.0}
                    strokeLinejoin="round"
                  />
                )}
              </g>
            </g>
          );
        })}

        {/* Bottom Time Axis Labels */}
        <text
          x={padLeft}
          y={totalH - 12}
          textAnchor="start"
          fill={textColor}
          fontSize={11}
          fontFamily="monospace"
        >
          {formatEngineeringValue(t0, 's')}
        </text>
        <text
          x={padLeft + plotW / 2}
          y={totalH - 12}
          textAnchor="middle"
          fill={textColor}
          fontSize={11}
          fontFamily="monospace"
        >
          {formatEngineeringValue((t0 + t1) / 2, 's')}
        </text>
        <text
          x={width - padRight}
          y={totalH - 12}
          textAnchor="end"
          fill={textColor}
          fontSize={11}
          fontFamily="monospace"
        >
          {formatEngineeringValue(t1, 's')}
        </text>

        {/* Cursor A */}
        {cursorsEnabled && cursorA !== null && time[cursorA] !== undefined && (
          <g className="cursor-line-a">
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={totalH - padBottom}
              strokeWidth={1.5}
              pointerEvents="none"
              style={{ stroke: 'var(--accent)' }}
            />
            {/* Wide grab area over entire vertical line */}
            <line
              x1={mapX(time[cursorA])}
              y1={padTop}
              x2={mapX(time[cursorA])}
              y2={totalH - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            />
            <g
              className="cursor-handle cursor-handle-a"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'A')}
            >
              <rect
                x={mapX(time[cursorA]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                style={{ fill: 'var(--accent)' }}
              />
              <text
                x={mapX(time[cursorA])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                A
              </text>
            </g>
          </g>
        )}

        {/* Cursor B */}
        {cursorsEnabled && cursorB !== null && time[cursorB] !== undefined && (
          <g className="cursor-line-b">
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={totalH - padBottom}
              stroke="#f43f5e"
              strokeWidth={1.5}
              pointerEvents="none"
            />
            {/* Wide grab area over entire vertical line */}
            <line
              x1={mapX(time[cursorB])}
              y1={padTop}
              x2={mapX(time[cursorB])}
              y2={totalH - padBottom}
              stroke="transparent"
              strokeWidth={16}
              style={{ cursor: 'ew-resize', pointerEvents: 'auto' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            />
            <g
              className="cursor-handle cursor-handle-b"
              style={{ cursor: 'ew-resize', pointerEvents: 'auto', userSelect: 'none' }}
              onPointerDown={(e) => handleCursorDragStart(e, 'B')}
            >
              <rect
                x={mapX(time[cursorB]) - 16}
                y={padTop - 20}
                width={32}
                height={18}
                rx={4}
                fill="#e11d48"
              />
              <text
                x={mapX(time[cursorB])}
                y={padTop - 7}
                fill="#ffffff"
                fontSize={10}
                fontWeight={800}
                textAnchor="middle"
              >
                B
              </text>
            </g>
          </g>
        )}

        {/* Drag Box Zoom Selection */}
        {dragBox && Math.abs(dragBox.currentX - dragBox.startX) > 5 && (
          <g pointerEvents="none">
            <rect
              x={Math.min(dragBox.startX, dragBox.currentX)}
              y={padTop}
              width={Math.abs(dragBox.currentX - dragBox.startX)}
              height={plotH}
              fill="rgba(56, 189, 248, 0.22)"
              style={{ stroke: 'var(--accent)' }}
              strokeWidth={1.5}
              strokeDasharray="4 3"
            />
            <rect
              x={Math.min(dragBox.startX, dragBox.currentX)}
              y={padTop + 4}
              width={Math.min(140, Math.abs(dragBox.currentX - dragBox.startX))}
              height={18}
              rx={3}
              style={{ fill: 'var(--accent)' }}
              opacity={0.9}
            />
            <text
              x={Math.min(dragBox.startX, dragBox.currentX) + 6}
              y={padTop + 16}
              fill="#ffffff"
              fontSize={10}
              fontWeight={700}
            >
              🔍 Drag to Zoom
            </text>
          </g>
        )}

        {/* Crosshair Line & Pill */}
        {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
          <g className="hover-crosshair" pointerEvents="none">
            <line
              x1={mapX(time[hoverIndex])}
              y1={padTop}
              x2={mapX(time[hoverIndex])}
              y2={totalH - padBottom}
              stroke={crosshairColor}
              strokeWidth={1}
              strokeDasharray="3 3"
            />
            <rect
              x={Math.max(padLeft, Math.min(width - padRight - 72, mapX(time[hoverIndex]) - 36))}
              y={totalH - padBottom + 4}
              width={72}
              height={18}
              rx={4}
              fill="#0f172a"
              stroke="#38bdf8"
              strokeWidth={1.2}
              className="hover-pill"
            />
            <text
              x={Math.max(padLeft + 36, Math.min(width - padRight - 36, mapX(time[hoverIndex])))}
              y={totalH - padBottom + 17}
              textAnchor="middle"
              fill="#38bdf8"
              fontSize={10.5}
              fontWeight={700}
              fontFamily="monospace"
              className="hover-text"
            >
              {formatEngineeringValue(time[hoverIndex], 's')}
            </text>
          </g>
        )}
      </svg>

      {hoverIndex !== null && hoverIndex >= 0 && hoverIndex < time.length && (
        <WaveformTooltip
          time={time}
          hoverIndex={hoverIndex}
          activeSignals={activeSignals}
          signals={signals}
          colorOf={colorOf}
        />
      )}
    </div>
  );
}
