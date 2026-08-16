/**
 * Editor controller: owns the reducer state, exposes action creators that
 * perform REST calls and feed results back into the store, manages simulation
 * execution and polling, and keeps the WebSocket subscription alive.
 */
import { useCallback, useEffect, useReducer, useRef, useState } from 'react';
import * as api from '../api/client';
import { initialState, editorReducer } from '../model/store';
import type {
  CatalogEntry,
  ComponentPayload,
  WirePayload,
  SimulationStatus,
} from '../model/types';
import { nextOrientation } from '../model/geometry';
import { BLANK_CIRCUIT_IPES } from '../model/examples';

export function useEditor() {
  const [state, dispatch] = useReducer(editorReducer, initialState);
  const [catalog, setCatalog] = useState<CatalogEntry[]>([]);
  const [wsConnected, setWsConnected] = useState(false);
  const versionRef = useRef(0);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  // Simulation state
  const [simStatus, setSimStatus] = useState<SimulationStatus | null>(null);
  const [simProgress, setSimProgress] = useState(0);
  const [simResults, setSimResults] = useState<Record<string, number[]> | null>(null);
  const [simError, setSimError] = useState<string | null>(null);
  const [simDrawerOpen, setSimDrawerOpen] = useState(false);
  const simPollTimerRef = useRef<number | null>(null);
  const currentSimIdRef = useRef<string | null>(null);

  // latest state for callbacks that need current values without re-creating
  const stateRef = useRef(state);
  stateRef.current = state;

  useEffect(() => {
    api
      .getCatalog()
      .then((c) => setCatalog(c.types))
      .catch(() => setCatalog([]));
  }, []);

  // Cleanup simulation polling timer on unmount
  useEffect(() => {
    return () => {
      if (simPollTimerRef.current !== null) {
        clearInterval(simPollTimerRef.current);
      }
    };
  }, []);

  const refresh = useCallback(async (circuitId: string) => {
    try {
      const snapshot = await api.getEditorModel(circuitId);
      versionRef.current = snapshot.modelVersion;
      dispatch({ type: 'SNAPSHOT', snapshot });
    } catch (e) {
      dispatch({ type: 'STATUS', status: `Refresh failed: ${(e as Error).message}` });
    }
  }, []);

  const reportError = useCallback((e: unknown) => {
    dispatch({ type: 'STATUS', status: `Error: ${(e as Error).message}` });
  }, []);

  const attachSubscription = useCallback(
    (circuitId: string) => {
      unsubscribeRef.current?.();
      unsubscribeRef.current = api.subscribeCircuitChanges(
        circuitId,
        (msg) => {
          if (msg.modelVersion > versionRef.current) {
            refresh(circuitId);
          }
        },
        setWsConnected,
      );
    },
    [refresh],
  );

  const open = useCallback(
    async (file: File) => {
      dispatch({ type: 'STATUS', status: `Loading ${file.name}...` });
      try {
        const circuitId = await api.uploadIpes(file);
        attachSubscription(circuitId);
        await refresh(circuitId);
        dispatch({ type: 'STATUS', status: `Loaded ${file.name}` });
      } catch (e) {
        reportError(e);
      }
    },
    [attachSubscription, refresh, reportError],
  );

  const openContent = useCallback(
    async (content: string, filename = 'circuit.ipes') => {
      dispatch({ type: 'STATUS', status: `Loading ${filename}...` });
      try {
        const circuitId = await api.uploadIpesString(content, filename);
        attachSubscription(circuitId);
        await refresh(circuitId);
        dispatch({ type: 'STATUS', status: `Loaded ${filename}` });
      } catch (e) {
        reportError(e);
      }
    },
    [attachSubscription, refresh, reportError],
  );

  const newCircuit = useCallback(async () => {
    await openContent(BLANK_CIRCUIT_IPES, 'Untitled.ipes');
  }, [openContent]);

  const arm = useCallback((entry: CatalogEntry) => {
    if (!stateRef.current.circuitId) {
      dispatch({ type: 'STATUS', status: 'Open a .ipes file first (Open... button)' });
      return;
    }
    dispatch({ type: 'ARM', componentType: entry.type, family: entry.family });
  }, []);

  const cancel = useCallback(() => dispatch({ type: 'CANCEL' }), []);
  const toggleWireMode = useCallback(() => dispatch({ type: 'TOGGLE_WIRE_MODE' }), []);

  const placeGhost = useCallback(
    (x: number, y: number, orientation?: number, typeOverride?: number, familyOverride?: string) => {
      const ghost = stateRef.current.ghost;
      const circuitId = stateRef.current.circuitId;
      const type = typeOverride !== undefined ? typeOverride : ghost?.type;
      const family = familyOverride || ghost?.family || 'LK';
      const orient = orientation !== undefined ? orientation : (ghost?.orientation || 503);

      if (type === undefined || !circuitId) return;
      dispatch({ type: 'CANCEL' });
      api
        .createComponent(circuitId, {
          family,
          type,
          x,
          y,
          orientation: orient,
        })
        .then((msg) => {
          const payload = msg.payload as ComponentPayload;
          versionRef.current = msg.modelVersion;
          dispatch({
            type: 'COMPONENT_UPSERT',
            component: toEditorComponent(payload, family),
            version: msg.modelVersion,
          });
          dispatch({ type: 'SELECT', name: payload.name, additive: false });
          dispatch({ type: 'PANEL_FOR', name: payload.name });
        })
        .catch(reportError);
    },
    [reportError],
  );

  const finishWire = useCallback(
    (points: number[][]) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      api
        .createConnection(circuitId, { type: 'LK', points })
        .then((msg) => {
          const payload = msg.payload as WirePayload;
          versionRef.current = msg.modelVersion;
          dispatch({ type: 'WIRE_CREATED', wire: payload, version: msg.modelVersion });
        })
        .catch(reportError);
    },
    [reportError],
  );

  const commitMove = useCallback(
    (moves: { name: string; x: number; y: number }[]) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      Promise.all(
        moves.map((move) =>
          api.patchComponent(circuitId, move.name, { x: move.x, y: move.y }),
        ),
      )
        .then((messages) => {
          const last = messages[messages.length - 1];
          versionRef.current = last.modelVersion;
          dispatch({ type: 'STATUS', status: `${moves.length} component(s) moved` });
        })
        .catch((e) => {
          reportError(e);
          refresh(circuitId);
        });
    },
    [refresh, reportError],
  );

  const rotateComponent = useCallback(
    (name: string) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      const comp = stateRef.current.components.find((c) => c.name === name);
      if (!comp) return;

      const nextOrient = nextOrientation(comp.orientation);
      api
        .patchComponent(circuitId, name, { orientation: nextOrient })
        .then((msg) => {
          versionRef.current = msg.modelVersion;
          dispatch({
            type: 'COMPONENT_UPSERT',
            component: { ...comp, orientation: nextOrient },
            version: msg.modelVersion,
          });
        })
        .catch(reportError);
    },
    [reportError],
  );

  const deleteComponent = useCallback(
    (name: string) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      api
        .deleteComponent(circuitId, name)
        .then((msg) => {
          versionRef.current = msg.modelVersion;
          dispatch({ type: 'COMPONENT_DELETED', name, version: msg.modelVersion });
        })
        .catch(reportError);
    },
    [reportError],
  );

  const deleteWire = useCallback(
    (index: number) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      api
        .deleteConnection(circuitId, index)
        .then((msg) => {
          versionRef.current = msg.modelVersion;
          dispatch({ type: 'WIRE_DELETED', index, version: msg.modelVersion });
        })
        .catch(reportError);
    },
    [reportError],
  );

  const labelWire = useCallback(
    (index: number, label: string) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      api
        .patchConnection(circuitId, index, { label })
        .then((msg) => {
          const payload = msg.payload as WirePayload;
          versionRef.current = msg.modelVersion;
          dispatch({
            type: 'WIRE_PATCHED',
            index,
            points: payload.points,
            label: payload.label,
            version: msg.modelVersion,
          });
        })
        .catch(reportError);
    },
    [reportError],
  );

  const deleteSelection = useCallback(() => {
    const current = stateRef.current;
    const circuitId = current.circuitId;
    if (!circuitId) return;
    const tasks: Promise<unknown>[] = [];
    for (const name of current.selection) {
      tasks.push(
        api.deleteComponent(circuitId, name).then((msg) => {
          versionRef.current = msg.modelVersion;
          dispatch({ type: 'COMPONENT_DELETED', name, version: msg.modelVersion });
        }),
      );
    }
    if (current.selectedWire !== null) {
      tasks.push(
        api.deleteConnection(circuitId, current.selectedWire).then((msg) => {
          versionRef.current = msg.modelVersion;
          dispatch({
            type: 'WIRE_DELETED',
            index: current.selectedWire!,
            version: msg.modelVersion,
          });
        }),
      );
    }
    if (tasks.length) {
      Promise.all(tasks).catch((e) => {
        reportError(e);
        refresh(circuitId);
      });
    }
  }, [refresh, reportError]);

  const undo = useCallback(async () => {
    const circuitId = stateRef.current.circuitId;
    if (!circuitId) return;
    try {
      const msg = await api.undo(circuitId);
      versionRef.current = msg.modelVersion;
      await refresh(circuitId);
    } catch (e) {
      reportError(e);
    }
  }, [refresh, reportError]);

  const redo = useCallback(async () => {
    const circuitId = stateRef.current.circuitId;
    if (!circuitId) return;
    try {
      const msg = await api.redo(circuitId);
      versionRef.current = msg.modelVersion;
      await refresh(circuitId);
    } catch (e) {
      reportError(e);
    }
  }, [refresh, reportError]);

  const save = useCallback(async () => {
    const current = stateRef.current;
    if (!current.circuitId) return;
    try {
      await api.downloadIpes(current.circuitId, current.filename || 'circuit.ipes');
    } catch (e) {
      reportError(e);
    }
  }, [reportError]);

  const rename = useCallback(
    (name: string, newName: string) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId || !newName || newName === name) return;
      api
        .patchComponent(circuitId, name, { newName })
        .then(async (msg) => {
          versionRef.current = msg.modelVersion;
          await refresh(circuitId);
        })
        .catch(reportError);
    },
    [refresh, reportError],
  );

  const setParameter = useCallback(
    (name: string, key: string, value: number) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      api
        .patchComponent(circuitId, name, { parameters: { [key]: value } })
        .then((msg) => {
          const payload = msg.payload as ComponentPayload;
          versionRef.current = msg.modelVersion;
          const existing = stateRef.current.components.find((c) => c.name === name);
          if (existing) {
            dispatch({
              type: 'COMPONENT_UPSERT',
              component: {
                ...existing,
                position: payload.position,
                orientation: payload.orientation,
                parameters: payload.parameters,
              },
              version: msg.modelVersion,
            });
          }
        })
        .catch(reportError);
    },
    [reportError],
  );

  const setLabel = useCallback(
    (component: string, side: 'x' | 'y', label: string) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      api
        .setNodeLabel(circuitId, component, 0, side, label)
        .then((msg) => {
          versionRef.current = msg.modelVersion;
          const existing = stateRef.current.components.find((c) => c.name === component);
          if (existing) {
            const updated =
              side === 'x'
                ? { ...existing, inputLabels: [label] }
                : { ...existing, outputLabels: [label] };
            dispatch({ type: 'COMPONENT_UPSERT', component: updated, version: msg.modelVersion });
          }
        })
        .catch(reportError);
    },
    [reportError],
  );

  // ========== Simulation Actions ==========

  const runSimulation = useCallback(
    async (config: { simulationTime: number; timeStep: number; solverType: string }) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;

      if (simPollTimerRef.current !== null) {
        clearInterval(simPollTimerRef.current);
        simPollTimerRef.current = null;
      }

      setSimStatus('RUNNING');
      setSimProgress(0.05);
      setSimError(null);
      setSimDrawerOpen(true);

      try {
        const sim = await api.submitSimulation({
          circuitId,
          simulationTime: config.simulationTime,
          timeStep: config.timeStep,
          solverType: config.solverType,
        });

        currentSimIdRef.current = sim.simulationId;

        // Poll simulation status until completed or failed
        const pollInterval = window.setInterval(async () => {
          const simId = currentSimIdRef.current;
          if (!simId) return;

          try {
            const current = await api.getSimulation(simId);
            setSimStatus(current.status);

            if (current.progressDetails) {
              const { currentStep, totalSteps } = current.progressDetails;
              if (totalSteps > 0) {
                setSimProgress(currentStep / totalSteps);
              }
            }

            if (current.status === 'COMPLETED') {
              clearInterval(pollInterval);
              simPollTimerRef.current = null;
              setSimProgress(1.0);
              const res = current.results || (await api.getSimulationResults(simId));
              setSimResults(res);
            } else if (current.status === 'FAILED' || current.status === 'CANCELLED') {
              clearInterval(pollInterval);
              simPollTimerRef.current = null;
              setSimError(current.errorMessage || 'Simulation failed or was cancelled');
            }
          } catch (err) {
            clearInterval(pollInterval);
            simPollTimerRef.current = null;
            setSimStatus('FAILED');
            setSimError((err as Error).message);
          }
        }, 300);

        simPollTimerRef.current = pollInterval;
      } catch (err) {
        setSimStatus('FAILED');
        setSimError((err as Error).message);
      }
    },
    [],
  );

  const cancelSimulation = useCallback(async () => {
    if (simPollTimerRef.current !== null) {
      clearInterval(simPollTimerRef.current);
      simPollTimerRef.current = null;
    }
    const simId = currentSimIdRef.current;
    if (simId) {
      try {
        await api.cancelSimulation(simId);
      } catch {
        // ignore
      }
    }
    setSimStatus('CANCELLED');
  }, []);

  return {
    state,
    dispatch,
    catalog,
    wsConnected,
    simState: {
      status: simStatus,
      progress: simProgress,
      results: simResults,
      errorMessage: simError,
      isOpen: simDrawerOpen,
    },
    actions: {
      open,
      openContent,
      newCircuit,
      arm,
      cancel,
      toggleWireMode,
      placeGhost,
      finishWire,
      commitMove,
      rotateComponent,
      deleteComponent,
      deleteWire,
      labelWire,
      deleteSelection,
      undo,
      redo,
      save,
      rename,
      setParameter,
      setLabel,
      openProperties: (name: string) => dispatch({ type: 'PANEL_FOR', name }),
      runSimulation,
      cancelSimulation,
      toggleSimDrawer: () => setSimDrawerOpen((prev) => !prev),
    },
  };
}

function toEditorComponent(payload: ComponentPayload, family: string) {
  return {
    type: payload.type,
    name: payload.name,
    family,
    position: payload.position,
    orientation: payload.orientation,
    parameters: payload.parameters,
    inputLabels: [],
    outputLabels: [],
  };
}
