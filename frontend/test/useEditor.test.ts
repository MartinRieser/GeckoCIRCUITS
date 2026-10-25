// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useEditor } from '../src/hooks/useEditor';
import { LkComponentType } from '../src/model/constants';

vi.mock('../src/api/client', () => ({
  getCatalog: vi.fn().mockResolvedValue({ types: [{ type: 1, name: 'Resistor', family: 'LK' }] }),
  getEditorModel: vi.fn().mockResolvedValue({
    circuitId: 'test-c1',
    modelVersion: 1,
    components: [],
    wires: [],
  }),
  uploadIpes: vi.fn().mockResolvedValue('test-c1'),
  uploadIpesBase64: vi.fn().mockResolvedValue('test-c1'),
  uploadIpesString: vi.fn().mockResolvedValue('test-c1'),
  subscribeCircuitChanges: vi.fn().mockReturnValue(() => {}),
  cancelSimulation: vi.fn().mockResolvedValue(undefined),
  setGraphRegisters: vi.fn().mockResolvedValue(undefined),
}));

describe('useEditor hook', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  async function renderEditor() {
    let hook: ReturnType<typeof renderHook<ReturnType<typeof useEditor>, unknown>> | null = null;
    await act(async () => {
      hook = renderHook(() => useEditor());
    });
    return hook!;
  }

  it('initializes with default idle state and null simulation', async () => {
    const { result } = await renderEditor();

    expect(result.current.state.mode).toBe('idle');
    expect(result.current.state.selection).toEqual([]);
    expect(result.current.state.ghost).toBeNull();
    expect(result.current.simState.isOpen).toBe(false);
    expect(result.current.simState.status).toBeNull();
    expect(result.current.state.graphRegisters).toHaveLength(1);
    expect(result.current.state.graphRegisters[0].name).toBe('Osci 1');
  });

  it('adds, updates, and removes graph registers', async () => {
    const { result } = await renderEditor();

    act(() => {
      result.current.actions.addGraphRegister({
        id: 'osci_2',
        name: 'Control Loop Scope',
        numGraphs: 2,
        signals: ['v_err', 'duty'],
        signalGraphs: { v_err: 1, duty: 2 },
        hiddenSignals: [],
        yScaleMode: 'auto',
        cursorsEnabled: true,
      });
    });

    expect(result.current.state.graphRegisters).toHaveLength(2);
    const added = result.current.state.graphRegisters.find((r) => r.id === 'osci_2');
    expect(added?.name).toBe('Control Loop Scope');
    expect(added?.numGraphs).toBe(2);
    expect(added?.signals).toEqual(['v_err', 'duty']);

    // Update
    act(() => {
      result.current.actions.updateGraphRegister('osci_2', { numGraphs: 3, name: 'PID Signals' });
    });
    const updated = result.current.state.graphRegisters.find((r) => r.id === 'osci_2');
    expect(updated?.name).toBe('PID Signals');
    expect(updated?.numGraphs).toBe(3);

    // Remove
    act(() => {
      result.current.actions.removeGraphRegister('osci_2');
    });
    expect(result.current.state.graphRegisters.find((r) => r.id === 'osci_2')).toBeUndefined();
  });

  it('arms a component and enters placing mode, then cancels back to idle', async () => {
    const { result } = await renderEditor();

    await act(async () => {
      await result.current.actions.arm({
        type: LkComponentType.RESISTOR,
        name: 'Resistor',
        family: 'LK',
      });
    });

    expect(result.current.state.mode).toBe('placing');
    expect(result.current.state.ghost).not.toBeNull();
    expect(result.current.state.ghost?.type).toBe(LkComponentType.RESISTOR);

    act(() => {
      result.current.actions.cancel();
    });

    expect(result.current.state.mode).toBe('idle');
    expect(result.current.state.ghost).toBeNull();
  });

  it('toggles wire mode between idle and wiring', async () => {
    const { result } = await renderEditor();

    act(() => {
      result.current.actions.toggleWireMode();
    });
    expect(result.current.state.mode).toBe('wiring');

    act(() => {
      result.current.actions.toggleWireMode();
    });
    expect(result.current.state.mode).toBe('idle');
  });

  it('toggles simulation drawer open/close', async () => {
    const { result } = await renderEditor();

    expect(result.current.simState.isOpen).toBe(false);

    act(() => {
      result.current.actions.toggleSimDrawer();
    });
    expect(result.current.simState.isOpen).toBe(true);

    act(() => {
      result.current.actions.toggleSimDrawer();
    });
    expect(result.current.simState.isOpen).toBe(false);
  });

  it('opens properties panel for a specific component', async () => {
    const { result } = await renderEditor();

    act(() => {
      result.current.actions.openProperties('R.1');
    });
    expect(result.current.state.panelFor).toBe('R.1');

    act(() => {
      result.current.actions.openProperties('');
    });
    expect(result.current.state.panelFor).toBe('');
  });
});
