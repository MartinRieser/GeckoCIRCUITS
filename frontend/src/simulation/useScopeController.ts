import { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import type { EditorComponent, GraphRegister } from '../model/types';
import { mapSimulationResults } from './chartData';
import { findScopeBlocks, scopeChannels } from './scopes';
import { SCOPE_SETTING_KEYS } from '../model/constants';
import {
  effectiveWindow,
  panWindow,
  zoomWindow,
  type ViewWindow,
} from './viewWindow';
import { signalColorInList, type TraceTheme } from './traceColors';

/** @deprecated canonical colors come from colorOf(name) — kept for tests. */
export const TRACE_COLORS_DARK = [
  '#38bdf8', // Light blue / Cyan
  '#f43f5e', // Rose / Red
  '#facc15', // Amber / Yellow
  '#4ade80', // Mint / Green
  '#a855f7', // Purple
  '#fb923c', // Orange
  '#2dd4bf', // Teal
  '#e879f9', // Fuchsia
];

/** @deprecated canonical colors come from colorOf(name) — kept for tests. */
export const TRACE_COLORS_LIGHT = [
  '#0284c7', // Sky blue
  '#e11d48', // Crimson
  '#ca8a04', // Dark amber
  '#16a34a', // Forest green
  '#7e22ce', // Dark violet
  '#ea580c', // Dark orange
  '#0f766e', // Dark teal
  '#c026d3', // Magenta
];

/** Measurement details for a single channel evaluated at the cursor positions. */
export interface CursorMeasurementChannel {
  /** Signal / channel identifier. */
  name: string;
  /** Display color matching waveform trace. */
  color: string;
  /** Signal value at Cursor A time position, or null. */
  valA: number | null;
  /** Signal value at Cursor B time position, or null. */
  valB: number | null;
  /** Difference (valB - valA), or null. */
  delta: number | null;
}

/** Comprehensive cursor measurement readout across time and all active scope channels. */
export interface CursorMeasurements {
  /** Timestamp at Cursor A in seconds. */
  timeA: number | null;
  /** Timestamp at Cursor B in seconds. */
  timeB: number | null;
  /** Time delta (|timeB - timeA|) in seconds. */
  dt: number | null;
  /** Frequency equivalent (1 / dt) in Hertz. */
  freq: number | null;
  /** Per-channel values and deltas. */
  channels: CursorMeasurementChannel[];
}

/** Properties for the {@link useScopeController} hook. */
export interface UseScopeControllerProps {
  /** Simulation results time-series mapping, or null. */
  results: Record<string, number[]> | null;
  /** Schematic components used for scope block detection. */
  components?: EditorComponent[];
  /** Identifier of the selected scope, or 'all'. */
  selectedScope?: string;
  /** Active workspace GraphRegister, if inspecting an Osci register tab. */
  graphRegister?: GraphRegister | null;
  /** UI theme for waveform coloring ('dark' or 'light'). */
  theme?: 'dark' | 'light';
  /** Optional callback to persist scope parameters to circuit components. */
  onSetParameter?: (name: string, key: string, value: number | string | boolean) => void;
  /** Optional callback to persist changes to the active workspace GraphRegister. */
  onUpdateGraphRegister?: (patch: Partial<GraphRegister>) => void;
}

/** State and actions returned by {@link useScopeController} to control oscilloscope views. */
export interface ScopeController {
  // Horizontal / View Window
  view: ViewWindow | null;
  setView: React.Dispatch<React.SetStateAction<ViewWindow | null>>;
  zoomAt: (factor: number, anchor: number) => void;
  pan: (fraction: number) => void;
  fit: () => void;
  timePerDiv: number;
  dataT0: number;
  dataT1: number;

  // Vertical / Y-scale
  yScaleMode: 'fixed' | 'auto';
  setYScaleMode: React.Dispatch<React.SetStateAction<'fixed' | 'auto'>>;
  yView: { min: number; max: number } | null;
  setYView: React.Dispatch<React.SetStateAction<{ min: number; max: number } | null>>;

  // Signal & Channel Information
  signalNames: string[];
  timeArray: number[];
  signalStats: ReturnType<typeof mapSimulationResults>['signalStats'];
  scopeChannelNames: string[];
  hiddenSignals: Record<string, boolean>;
  toggleSignal: (name: string) => void;
  /** Canonical trace color for a signal: channel order when a scope is
   *  selected, result column order in the combined view. */
  colorOf: (name: string) => string;

  // Cursors & Single Measurement Location
  cursorsEnabled: boolean;
  setCursorsEnabled: React.Dispatch<React.SetStateAction<boolean>>;
  cursorA: number | null;
  setCursorA: React.Dispatch<React.SetStateAction<number | null>>;
  cursorB: number | null;
  setCursorB: React.Dispatch<React.SetStateAction<number | null>>;
  activeCursor: 'A' | 'B';
  setActiveCursor: React.Dispatch<React.SetStateAction<'A' | 'B'>>;
  setCursor: (type: 'A' | 'B', index: number | null) => void;
  setCursorPreset: () => void;
  clearCursors: () => void;
  cursorMeasurements: CursorMeasurements | null;

  // Drawer
  drawerTab: 'metrics' | 'fft' | 'losses' | null;
  setDrawerTab: React.Dispatch<React.SetStateAction<'metrics' | 'fft' | 'losses' | null>>;
}

export function useScopeController({
  results,
  components = [],
  selectedScope = 'all',
  graphRegister = null,
  theme = 'dark',
  onSetParameter,
  onUpdateGraphRegister,
}: UseScopeControllerProps): ScopeController {
  const [hiddenSignals, setHiddenSignals] = useState<Record<string, boolean>>({});
  const [cursorA, setCursorA] = useState<number | null>(null);
  const [cursorB, setCursorB] = useState<number | null>(null);
  const [activeCursor, setActiveCursor] = useState<'A' | 'B'>('A');
  const [cursorsEnabled, setCursorsEnabled] = useState(false);
  const [drawerTab, setDrawerTab] = useState<'metrics' | 'fft' | 'losses' | null>(null);

  // Time-axis view window; null = fit whole simulation
  const [view, setView] = useState<ViewWindow | null>(null);
  const [yView, setYView] = useState<{ min: number; max: number } | null>(null);
  const [yScaleMode, setYScaleMode] = useState<'fixed' | 'auto'>('fixed');

  const scopeBlocks = useMemo(() => findScopeBlocks(components), [components]);

  const activeScopeBlock = useMemo(() => {
    if (selectedScope === 'all') return null;
    return scopeBlocks.find((sb) => sb.name === selectedScope) || null;
  }, [scopeBlocks, selectedScope]);

  const { signalNames, timeArray, signalStats } = useMemo(
    () => mapSimulationResults(results),
    [results],
  );

  // Reset view and cursors on new simulation run results
  useEffect(() => {
    setView(null);
    setYView(null);
  }, [results]);

  const lastSyncedHiddenRef = useRef<string | undefined>(undefined);
  const lastScopeNameRef = useRef<string | undefined>(undefined);

  // Synchronize per-scope saved settings from GraphRegister or component parameters.
  useEffect(() => {
    if (graphRegister) {
      lastScopeNameRef.current = `reg:${graphRegister.id}`;
      setYScaleMode(graphRegister.yScaleMode === 'auto' ? 'auto' : 'fixed');
      const hiddenStr = (graphRegister.hiddenSignals ?? []).join(',');
      if (hiddenStr !== lastSyncedHiddenRef.current) {
        lastSyncedHiddenRef.current = hiddenStr;
        const set: Record<string, boolean> = {};
        (graphRegister.hiddenSignals ?? []).forEach((s) => {
          if (s.trim()) set[s.trim()] = true;
        });
        setHiddenSignals(set);
      }
      const enabled = Boolean(graphRegister.cursorsEnabled);
      setCursorsEnabled(enabled);
      if (enabled && timeArray.length > 0) {
        setCursorA((prev) => (prev === null ? Math.floor(timeArray.length * 0.25) : prev));
        setCursorB((prev) => (prev === null ? Math.floor(timeArray.length * 0.75) : prev));
      }
      return;
    }

    if (!activeScopeBlock) {
      setHiddenSignals({});
      lastSyncedHiddenRef.current = undefined;
      lastScopeNameRef.current = undefined;
      return;
    }
    const isScopeSwitch = activeScopeBlock.name !== lastScopeNameRef.current;
    lastScopeNameRef.current = activeScopeBlock.name;

    const params = activeScopeBlock.parameters ?? {};
    setYScaleMode(params[SCOPE_SETTING_KEYS.yScaleMode] === 'auto' ? 'auto' : 'fixed');
    const hidden = params[SCOPE_SETTING_KEYS.hiddenSignals];
    const hiddenStr = typeof hidden === 'string' ? hidden : undefined;

    if (isScopeSwitch) {
      const currentHidden = hiddenStr ?? '';
      lastSyncedHiddenRef.current = currentHidden;
      const set: Record<string, boolean> = {};
      currentHidden
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
        .forEach((s) => {
          set[s] = true;
        });
      setHiddenSignals(set);
    } else if (hiddenStr !== undefined && hiddenStr !== lastSyncedHiddenRef.current) {
      lastSyncedHiddenRef.current = hiddenStr;
      const set: Record<string, boolean> = {};
      hiddenStr
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
        .forEach((s) => {
          set[s] = true;
        });
      setHiddenSignals(set);
    }

    const cursors = params[SCOPE_SETTING_KEYS.cursorsEnabled];
    // Accept both the parsed boolean and legacy string forms
    setCursorsEnabled(cursors === true || cursors === 'true');
  }, [
    graphRegister?.id,
    graphRegister?.yScaleMode,
    graphRegister?.hiddenSignals,
    graphRegister?.cursorsEnabled,
    timeArray.length,
    activeScopeBlock?.name,
    activeScopeBlock?.parameters?.[SCOPE_SETTING_KEYS.yScaleMode],
    activeScopeBlock?.parameters?.[SCOPE_SETTING_KEYS.hiddenSignals],
    activeScopeBlock?.parameters?.[SCOPE_SETTING_KEYS.cursorsEnabled],
  ]);

  const dataT0 = timeArray.length ? timeArray[0] : 0;
  const dataT1 = timeArray.length ? timeArray[timeArray.length - 1] : 1;
  const win = effectiveWindow(view, dataT0, dataT1);
  const timePerDiv = (win.end - win.start) / 10;

  const zoomAt = useCallback(
    (factor: number, anchor: number) => {
      setView(zoomWindow(effectiveWindow(view, dataT0, dataT1), factor, anchor, dataT0, dataT1));
    },
    [view, dataT0, dataT1],
  );

  const pan = useCallback(
    (fraction: number) => {
      setView(panWindow(effectiveWindow(view, dataT0, dataT1), fraction, dataT0, dataT1));
    },
    [view, dataT0, dataT1],
  );

  const fit = useCallback(() => {
    setView(null);
    setYView(null);
  }, []);

  const scopeChannelNames = useMemo(() => {
    if (graphRegister) {
      return graphRegister.signals;
    }
    return scopeChannels(activeScopeBlock, signalNames);
  }, [graphRegister, activeScopeBlock, signalNames]);

  // One canonical color per signal: channel order when a scope/register is selected
  // (CH1 = first palette entry, matching the properties badges), result
  // column order in the combined "All Scopes & Signals" view.
  const colorOf = useCallback(
    (name: string) =>
      signalColorInList(
        name,
        scopeChannelNames.length > 0 ? scopeChannelNames : signalNames,
        theme as TraceTheme,
      ),
    [scopeChannelNames, signalNames, theme],
  );

  const toggleSignal = useCallback(
    (name: string) => {
      setHiddenSignals((prev) => {
        const next = { ...prev, [name]: !prev[name] };
        const hiddenArr = Object.keys(next).filter((k) => Boolean(next[k]));
        const hiddenList = hiddenArr.join(',');
        lastSyncedHiddenRef.current = hiddenList;
        if (graphRegister && onUpdateGraphRegister) {
          onUpdateGraphRegister({ hiddenSignals: hiddenArr });
        }
        if (activeScopeBlock && onSetParameter) {
          onSetParameter(activeScopeBlock.name, SCOPE_SETTING_KEYS.hiddenSignals, hiddenList);
        }
        return next;
      });
    },
    [graphRegister, onUpdateGraphRegister, activeScopeBlock, onSetParameter],
  );

  const setCursorAWithEnable = useCallback(
    (action: React.SetStateAction<number | null>) => {
      setCursorA((prev) => {
        const next = typeof action === 'function' ? action(prev) : action;
        if (next !== null) {
          setCursorsEnabled(true);
          if (graphRegister && onUpdateGraphRegister) {
            onUpdateGraphRegister({ cursorsEnabled: true });
          }
          if (activeScopeBlock && onSetParameter) {
            onSetParameter(activeScopeBlock.name, SCOPE_SETTING_KEYS.cursorsEnabled, true);
          }
        }
        return next;
      });
    },
    [graphRegister, onUpdateGraphRegister, activeScopeBlock, onSetParameter],
  );

  const setCursorBWithEnable = useCallback(
    (action: React.SetStateAction<number | null>) => {
      setCursorB((prev) => {
        const next = typeof action === 'function' ? action(prev) : action;
        if (next !== null) {
          setCursorsEnabled(true);
          if (graphRegister && onUpdateGraphRegister) {
            onUpdateGraphRegister({ cursorsEnabled: true });
          }
          if (activeScopeBlock && onSetParameter) {
            onSetParameter(activeScopeBlock.name, SCOPE_SETTING_KEYS.cursorsEnabled, true);
          }
        }
        return next;
      });
    },
    [graphRegister, onUpdateGraphRegister, activeScopeBlock, onSetParameter],
  );

  const setCursor = useCallback(
    (type: 'A' | 'B', idx: number | null) => {
      if (idx !== null) {
        setCursorsEnabled(true);
        if (graphRegister && onUpdateGraphRegister) {
          onUpdateGraphRegister({ cursorsEnabled: true });
        }
        if (activeScopeBlock && onSetParameter) {
          onSetParameter(activeScopeBlock.name, SCOPE_SETTING_KEYS.cursorsEnabled, true);
        }
      }
      if (type === 'A') {
        setCursorA(idx);
      } else {
        setCursorB(idx);
      }
    },
    [graphRegister, onUpdateGraphRegister, activeScopeBlock, onSetParameter],
  );

  const setCursorPreset = useCallback(() => {
    setCursorsEnabled(true);
    if (timeArray.length > 0) {
      setCursorA(Math.floor(timeArray.length * 0.25));
      setCursorB(Math.floor(timeArray.length * 0.75));
    }
    if (graphRegister && onUpdateGraphRegister) {
      onUpdateGraphRegister({ cursorsEnabled: true });
    }
    if (activeScopeBlock && onSetParameter) {
      onSetParameter(activeScopeBlock.name, SCOPE_SETTING_KEYS.cursorsEnabled, true);
    }
  }, [timeArray, graphRegister, onUpdateGraphRegister, activeScopeBlock, onSetParameter]);

  const clearCursors = useCallback(() => {
    setCursorA(null);
    setCursorB(null);
    setCursorsEnabled(false);
    if (graphRegister && onUpdateGraphRegister) {
      onUpdateGraphRegister({ cursorsEnabled: false });
    }
    if (activeScopeBlock && onSetParameter) {
      onSetParameter(activeScopeBlock.name, SCOPE_SETTING_KEYS.cursorsEnabled, false);
    }
  }, [graphRegister, onUpdateGraphRegister, activeScopeBlock, onSetParameter]);

  // Compute the single source of truth for cursor measurements
  const cursorMeasurements = useMemo<CursorMeasurements | null>(() => {
    if (!cursorsEnabled || (cursorA === null && cursorB === null)) {
      return null;
    }

    const tA = cursorA !== null && timeArray[cursorA] !== undefined ? timeArray[cursorA] : null;
    const tB = cursorB !== null && timeArray[cursorB] !== undefined ? timeArray[cursorB] : null;
    const dt = tA !== null && tB !== null ? Math.abs(tB - tA) : null;
    const freq = dt && dt > 0 ? 1 / dt : null;

    const channels: CursorMeasurementChannel[] = scopeChannelNames.map((name) => {
      const color = colorOf(name);
      const valA = cursorA !== null && results?.[name]?.[cursorA] !== undefined ? results[name][cursorA] : null;
      const valB = cursorB !== null && results?.[name]?.[cursorB] !== undefined ? results[name][cursorB] : null;
      const delta = valA !== null && valB !== null ? valB - valA : null;
      return {
        name,
        color,
        valA,
        valB,
        delta,
      };
    });

    return {
      timeA: tA,
      timeB: tB,
      dt,
      freq,
      channels,
    };
  }, [cursorsEnabled, cursorA, cursorB, timeArray, scopeChannelNames, colorOf, results]);

  return {
    view,
    setView,
    zoomAt,
    pan,
    fit,
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
    setCursorA: setCursorAWithEnable,
    cursorB,
    setCursorB: setCursorBWithEnable,
    activeCursor,
    setActiveCursor,
    setCursor,
    setCursorPreset,
    clearCursors,
    cursorMeasurements,
    drawerTab,
    setDrawerTab,
  };
}
