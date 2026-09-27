import { describe, it, expect } from 'vitest';
import {
  buildScriptWatchRows,
  clampInputTerminalCount,
  clampOutputTerminalCount,
  isDebugPausedAnywhere,
  resolveScriptPauseSnapshot,
  resolveScriptWatchSnapshot,
  DEFAULT_SCRIPT_CODE,
} from '../src/properties/scriptBlockShared';
import type { ScriptDebugSnapshot } from '../src/api/client';
import type { ScriptDebugPanelState } from '../src/properties/PropertiesPanel';

function snapshot(overrides: Partial<ScriptDebugSnapshot> = {}): ScriptDebugSnapshot {
  return {
    blockName: 'SCRIPT.1',
    line: 3,
    time: 0.001,
    dt: 1e-6,
    variables: { integral: 5, alpha: 1 },
    inputs: [2, 3],
    outputs: [6],
    ...overrides,
  };
}

describe('buildScriptWatchRows', () => {
  it('lists timing, inputs, outputs, and sorted state variables', () => {
    const rows = buildScriptWatchRows(snapshot());
    expect(rows.map((r) => r.name)).toEqual(['t', 'dt', 'u1', 'u2', 'y1', 'alpha', 'integral']);
    expect(rows[0].value).toBe(0.001);
    expect(rows.find((r) => r.name === 'y1')?.value).toBe(6);
  });

  it('handles empty inputs, outputs, and variables', () => {
    const rows = buildScriptWatchRows(
      snapshot({ inputs: [], outputs: [], variables: {} }),
    );
    expect(rows.map((r) => r.name)).toEqual(['t', 'dt']);
  });
});

describe('terminal count clamps', () => {
  it('clamps input counts into 0..16', () => {
    expect(clampInputTerminalCount(-3)).toBe(0);
    expect(clampInputTerminalCount(0)).toBe(0);
    expect(clampInputTerminalCount(4)).toBe(4);
    expect(clampInputTerminalCount(99)).toBe(16);
  });

  it('clamps output counts into 1..16', () => {
    expect(clampOutputTerminalCount(0)).toBe(1);
    expect(clampOutputTerminalCount(-2)).toBe(1);
    expect(clampOutputTerminalCount(4)).toBe(4);
    expect(clampOutputTerminalCount(99)).toBe(16);
  });
});

describe('debug snapshot resolution', () => {
  const otherBlock = snapshot({ blockName: 'SCRIPT.2' });
  const ownPause = snapshot({ blockName: 'SCRIPT.1' });

  function debugOf(partial: Partial<ScriptDebugPanelState>): ScriptDebugPanelState {
    return {
      status: 'RUNNING',
      breakpoints: {},
      debugPause: null,
      debugState: null,
      onToggleBreakpoint: () => {},
      onDebugResume: () => {},
      onDebugStep: () => {},
      ...partial,
    };
  }

  it('prefers the SSE pause snapshot for the inspected block', () => {
    const debug = debugOf({ debugPause: ownPause });
    expect(resolveScriptPauseSnapshot(debug, 'SCRIPT.1')).toBe(ownPause);
    expect(resolveScriptPauseSnapshot(debug, 'SCRIPT.2')).toBeNull();
  });

  it('falls back to the polled pausedAt snapshot', () => {
    const debug = debugOf({
      debugState: { simulationId: 's1', paused: true, pausedAt: ownPause, blocks: [otherBlock] },
    });
    expect(resolveScriptPauseSnapshot(debug, 'SCRIPT.1')).toBe(ownPause);
  });

  it('resolves watch values from the pause snapshot when paused, else the live poll', () => {
    const live = snapshot({ blockName: 'SCRIPT.1', line: -1 });
    expect(resolveScriptWatchSnapshot(debugOf({ debugPause: ownPause }), 'SCRIPT.1')).toBe(ownPause);
    expect(
      resolveScriptWatchSnapshot(
        debugOf({ debugState: { simulationId: 's1', paused: false, blocks: [otherBlock, live] } }),
        'SCRIPT.1',
      ),
    ).toBe(live);
    expect(resolveScriptWatchSnapshot(debugOf({}), 'SCRIPT.1')).toBeNull();
  });

  it('detects a pause anywhere via SSE or polled state', () => {
    expect(isDebugPausedAnywhere(undefined)).toBe(false);
    expect(isDebugPausedAnywhere(debugOf({}))).toBe(false);
    expect(isDebugPausedAnywhere(debugOf({ debugPause: ownPause }))).toBe(true);
    expect(
      isDebugPausedAnywhere(
        debugOf({ debugState: { simulationId: 's1', paused: true, blocks: [] } }),
      ),
    ).toBe(true);
  });

  it('exposes the default script source constant', () => {
    expect(DEFAULT_SCRIPT_CODE).toBe('yOUT[0] = xIN[0];');
  });
});
