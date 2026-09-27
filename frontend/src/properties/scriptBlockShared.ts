/**
 * Shared logic for script/function block editors, used by both the compact
 * sidebar editor (PropertiesPanel's ScriptBlockEditor) and the full-viewport
 * IDE (ScriptViewTab): local editable state mirroring the persisted block
 * parameters, terminal-count clamping, debug snapshot resolution, and the
 * live variable watch rows. Keeping it here prevents the two editors from
 * drifting apart.
 */
import { useEffect, useState } from 'react';
import type { EditorComponent } from '../model/types';
import { SCRIPT_TERMINAL_LIMIT } from '../model/constants';
import type { ScriptDebugSnapshot } from '../api/client';
import type { ScriptDebugPanelState } from './PropertiesPanel';

/** Source shown for new or empty script blocks. */
export const DEFAULT_SCRIPT_CODE = 'yOUT[0] = xIN[0];';

/** One row of the live variable watch table. */
export interface ScriptWatchRow {
  /** Display name: t, dt, u1..uN, y1..yN, or a state variable name. */
  name: string;
  /** Current numeric value. */
  value: number;
  /** Short human hint describing what the row shows. */
  hint: string;
}

/**
 * Builds the watch table rows for a debug snapshot: simulation time and step,
 * inputs, outputs, then the persistent state variables sorted by name.
 */
export function buildScriptWatchRows(snapshot: ScriptDebugSnapshot): ScriptWatchRow[] {
  const rows: ScriptWatchRow[] = [
    { name: 't', value: snapshot.time, hint: 'simulation time (s)' },
    { name: 'dt', value: snapshot.dt, hint: 'time step (s)' },
  ];
  snapshot.inputs.forEach((value, i) => rows.push({ name: `u${i + 1}`, value, hint: 'input' }));
  snapshot.outputs.forEach((value, i) => rows.push({ name: `y${i + 1}`, value, hint: 'output' }));
  Object.entries(snapshot.variables)
    .sort(([a], [b]) => a.localeCompare(b))
    .forEach(([name, value]) => rows.push({ name, value, hint: 'state variable' }));
  return rows;
}

/** Clamps an input terminal count into the valid 0..SCRIPT_TERMINAL_LIMIT range. */
export function clampInputTerminalCount(value: number): number {
  return Math.max(0, Math.min(SCRIPT_TERMINAL_LIMIT, value));
}

/** Clamps an output terminal count into the valid 1..SCRIPT_TERMINAL_LIMIT range. */
export function clampOutputTerminalCount(value: number): number {
  return Math.max(1, Math.min(SCRIPT_TERMINAL_LIMIT, value));
}

/** True while the debug session is paused anywhere (SSE event or polled state). */
export function isDebugPausedAnywhere(debug?: ScriptDebugPanelState | null): boolean {
  return Boolean(debug?.debugState?.paused) || Boolean(debug?.debugPause);
}

/**
 * Resolves the pause snapshot of one script block. The SSE breakpoint event
 * is authoritative; the polled pausedAt snapshot is the fallback for pauses
 * that occurred before the SSE stream was subscribed.
 */
export function resolveScriptPauseSnapshot(
  debug: ScriptDebugPanelState | null | undefined,
  blockName: string | undefined,
): ScriptDebugSnapshot | null {
  if (!debug || !blockName) {
    return null;
  }
  if (debug.debugPause && debug.debugPause.blockName === blockName) {
    return debug.debugPause;
  }
  if (debug.debugState?.pausedAt && debug.debugState.pausedAt.blockName === blockName) {
    return debug.debugState.pausedAt;
  }
  return null;
}

/**
 * Resolves the watch snapshot of one script block: the pause snapshot when
 * available (frozen values while stepping), else the live polled snapshot.
 */
export function resolveScriptWatchSnapshot(
  debug: ScriptDebugPanelState | null | undefined,
  blockName: string | undefined,
): ScriptDebugSnapshot | null {
  const paused = resolveScriptPauseSnapshot(debug, blockName);
  if (paused) {
    return paused;
  }
  return debug?.debugState?.blocks.find((b) => b.blockName === blockName) ?? null;
}

/**
 * Local editable mirror of a script block's persisted parameters (source code
 * and terminal counts). Re-syncs from the component whenever the persisted
 * values change elsewhere (rename, parameter patch, reload) so the editor
 * never shows stale content after an external update.
 */
export function useScriptBlockEditorState(component: EditorComponent) {
  const [code, setCode] = useState(String(component.parameters['sourceCode'] || DEFAULT_SCRIPT_CODE));
  const [inCount, setInCount] = useState(Number(component.parameters['anzXIN'] || 1));
  const [outCount, setOutCount] = useState(Number(component.parameters['anzYOUT'] || 1));

  useEffect(() => {
    setCode(String(component.parameters['sourceCode'] || DEFAULT_SCRIPT_CODE));
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

  return { code, setCode, inCount, setInCount, outCount, setOutCount };
}
