import { useState, useMemo, useEffect, useCallback } from 'react';
import type { EditorComponent } from '../model/types';
import { mapSimulationResults } from './chartData';
import { findScopeBlocks, scopeChannels } from './scopes';
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

export interface CursorMeasurementChannel {
  name: string;
  color: string;
  valA: number | null;
  valB: number | null;
  delta: number | null;
}

export interface CursorMeasurements {
  timeA: number | null;
  timeB: number | null;
  dt: number | null;
  freq: number | null;
  channels: CursorMeasurementChannel[];
}

export interface UseScopeControllerProps {
  results: Record<string, number[]> | null;
  components?: EditorComponent[];
  selectedScope?: string;
  theme?: 'dark' | 'light';
}

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
  theme = 'dark',
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

  const scopeChannelNames = useMemo(
    () => scopeChannels(activeScopeBlock, signalNames),
    [activeScopeBlock, signalNames],
  );

  // One canonical color per signal: channel order when a scope is selected
  // (CH1 = first palette entry, matching the properties badges), result
  // column order in the combined "All Scopes & Signals" view.
  const colorOf = useCallback(
    (name: string) => signalColorInList(name, scopeChannelNames, theme as TraceTheme),
    [scopeChannelNames, theme],
  );

  const toggleSignal = useCallback((name: string) => {
    setHiddenSignals((prev) => ({ ...prev, [name]: !prev[name] }));
  }, []);

  const setCursorPreset = useCallback(() => {
    setCursorsEnabled(true);
    if (timeArray.length > 0) {
      setCursorA(Math.floor(timeArray.length * 0.25));
      setCursorB(Math.floor(timeArray.length * 0.75));
    }
  }, [timeArray]);

  const clearCursors = useCallback(() => {
    setCursorA(null);
    setCursorB(null);
    setCursorsEnabled(false);
  }, []);

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
    setCursorA,
    cursorB,
    setCursorB,
    activeCursor,
    setActiveCursor,
    setCursorPreset,
    clearCursors,
    cursorMeasurements,
    drawerTab,
    setDrawerTab,
  };
}
