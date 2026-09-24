// @vitest-environment jsdom
import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useScopeController } from '../src/simulation/useScopeController';
import type { EditorComponent } from '../src/model/types';
import { ControlComponentType } from '../src/model/constants';

describe('useScopeController hook', () => {
  const dummyComponents: EditorComponent[] = [
    {
      type: ControlComponentType.SCOPE,
      family: 'CONTROL',
      name: 'SCOPE.1',
      position: [0, 0],
      orientation: 502,
      parameters: {},
      inputLabels: ['v_out'],
      outputLabels: [],
    },
  ];

  const dummyResults = {
    time: [0, 0.001, 0.002, 0.003, 0.004, 0.005],
    v_out: [0, 2.5, 5.0, 7.5, 10.0, 10.0],
    i_in: [0, 1.0, 2.0, 1.5, 1.0, 0.5],
  };

  it('initializes with default values when results are null', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: null,
      }),
    );

    expect(result.current.signalNames).toEqual([]);
    expect(result.current.timeArray).toEqual([]);
    expect(result.current.cursorsEnabled).toBe(false);
    expect(result.current.cursorMeasurements).toBeNull();
    expect(result.current.view).toBeNull();
  });

  it('maps signals and computes time division correctly', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
        components: dummyComponents,
        selectedScope: 'all',
      }),
    );

    expect(result.current.signalNames).toEqual(['v_out', 'i_in']);
    expect(result.current.timeArray).toHaveLength(6);
    expect(result.current.dataT0).toBe(0);
    expect(result.current.dataT1).toBe(0.005);
    expect(result.current.timePerDiv).toBeCloseTo(0.0005);
  });

  it('filters scopeChannelNames when a specific scope is selected', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
        components: dummyComponents,
        selectedScope: 'SCOPE.1',
      }),
    );

    expect(result.current.scopeChannelNames).toEqual(['v_out']);
  });

  it('handles zooming, panning, and fitting the view window', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
      }),
    );

    // Zoom at midpoint
    act(() => {
      result.current.zoomAt(2.0, 0.5);
    });
    expect(result.current.view).not.toBeNull();

    // Pan
    act(() => {
      result.current.pan(0.1);
    });
    expect(result.current.view).not.toBeNull();

    // Fit resets to null
    act(() => {
      result.current.fit();
    });
    expect(result.current.view).toBeNull();
  });

  it('toggles signal visibility in hiddenSignals', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
      }),
    );

    expect(result.current.hiddenSignals['v_out']).toBeUndefined();

    act(() => {
      result.current.toggleSignal('v_out');
    });
    expect(result.current.hiddenSignals['v_out']).toBe(true);

    act(() => {
      result.current.toggleSignal('v_out');
    });
    expect(result.current.hiddenSignals['v_out']).toBe(false);
  });

  it('manages cursor presets, clearing, and active cursor toggling', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
      }),
    );

    act(() => {
      result.current.setCursorPreset();
    });

    expect(result.current.cursorsEnabled).toBe(true);
    expect(result.current.cursorA).toBe(1); // 6 * 0.25 = 1
    expect(result.current.cursorB).toBe(4); // 6 * 0.75 = 4

    // Measurements are computed
    expect(result.current.cursorMeasurements).not.toBeNull();
    const m = result.current.cursorMeasurements!;
    expect(m.timeA).toBe(0.001);
    expect(m.timeB).toBe(0.004);
    expect(m.dt).toBeCloseTo(0.003);
    expect(m.freq).toBeCloseTo(1 / 0.003);

    const vOutChannel = m.channels.find((c) => c.name === 'v_out');
    expect(vOutChannel).toBeDefined();
    expect(vOutChannel!.valA).toBe(2.5);
    expect(vOutChannel!.valB).toBe(10.0);
    expect(vOutChannel!.delta).toBe(7.5);

    // Switch active cursor
    act(() => {
      result.current.setActiveCursor('B');
    });
    expect(result.current.activeCursor).toBe('B');

    // Clear cursors
    act(() => {
      result.current.clearCursors();
    });
    expect(result.current.cursorsEnabled).toBe(false);
    expect(result.current.cursorA).toBeNull();
    expect(result.current.cursorB).toBeNull();
    expect(result.current.cursorMeasurements).toBeNull();
  });

  it('allows switching drawerTab', () => {
    const { result } = renderHook(() =>
      useScopeController({
        results: dummyResults,
      }),
    );

    expect(result.current.drawerTab).toBeNull();

    act(() => {
      result.current.setDrawerTab('fft');
    });
    expect(result.current.drawerTab).toBe('fft');

    act(() => {
      result.current.setDrawerTab('losses');
    });
    expect(result.current.drawerTab).toBe('losses');

    act(() => {
      result.current.setDrawerTab(null);
    });
    expect(result.current.drawerTab).toBeNull();
  });
});
