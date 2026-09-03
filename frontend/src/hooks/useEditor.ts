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
  SimulationDefaults,
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
  const [simDefaults, setSimDefaults] = useState<SimulationDefaults | null>(null);
  const simPollTimerRef = useRef<number | null>(null);
  const simStreamStopRef = useRef<(() => void) | null>(null);
  const currentSimIdRef = useRef<string | null>(null);
  const simDefaultsRef = useRef<SimulationDefaults | null>(null);
  simDefaultsRef.current = simDefaults;

  // latest state for callbacks that need current values without re-creating
  const stateRef = useRef(state);
  stateRef.current = state;

  useEffect(() => {
    api
      .getCatalog()
      .then((c) => setCatalog(c.types))
      .catch(() => setCatalog([]));
  }, []);

  // Cleanup simulation polling timer and SSE stream on unmount
  useEffect(() => {
    return () => {
      if (simPollTimerRef.current !== null) {
        clearInterval(simPollTimerRef.current);
      }
      simStreamStopRef.current?.();
    };
  }, []);

  const refresh = useCallback(async (circuitId: string) => {
    try {
      const snapshot = await api.getEditorModel(circuitId);
      versionRef.current = snapshot.modelVersion;
      setSimDefaults(snapshot.simulationDefaults ?? null);
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

  // Automatically initialize a blank workspace if none is loaded on initial page load / reload
  useEffect(() => {
    if (!stateRef.current.circuitId) {
      newCircuit().catch(console.error);
    }
  }, [newCircuit]);

  const arm = useCallback(
    async (entry: CatalogEntry) => {
      let circuitId = stateRef.current.circuitId;
      if (!circuitId) {
        try {
          dispatch({ type: 'STATUS', status: 'Initializing workspace...' });
          circuitId = await api.uploadIpesString(BLANK_CIRCUIT_IPES, 'Untitled.ipes');
          attachSubscription(circuitId);
          await refresh(circuitId);
        } catch (e) {
          reportError(e);
          return;
        }
      }
      dispatch({ type: 'ARM', componentType: entry.type, family: entry.family });
    },
    [attachSubscription, refresh, reportError],
  );

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
      const defaultParams =
        type === 1016 || type === 61
          ? { sourceCode: 'yOUT[0] = xIN[0];', anzXIN: 1, anzYOUT: 1 }
          : undefined;

      api
        .createComponent(circuitId, {
          family,
          type,
          x,
          y,
          orientation: orient,
          parameters: defaultParams,
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
      const wireType = stateRef.current.wireFamily || 'LK';
      api
        .createConnection(circuitId, { type: wireType, points })
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
        .then(async (messages) => {
          const last = messages[messages.length - 1];
          versionRef.current = last.modelVersion;
          // the server shifts wire points sitting on moved terminals; refetch
          // the model so client-side wire geometry mirrors the server state
          await refresh(circuitId);
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

  const duplicateSelection = useCallback(async () => {
    const current = stateRef.current;
    const circuitId = current.circuitId;
    if (!circuitId || current.selection.length === 0) return;

    const clones: string[] = [];
    for (const name of current.selection) {
      const comp = current.components.find((c) => c.name === name);
      if (!comp) continue;
      try {
        const msg = await api.createComponent(circuitId, {
          family: comp.family,
          type: comp.type,
          x: comp.position[0] + 2,
          y: comp.position[1] + 2,
          orientation: comp.orientation,
        });
        const payload = msg.payload as ComponentPayload;
        versionRef.current = msg.modelVersion;
        const newComp = toEditorComponent(payload, comp.family);
        if (comp.parameters && Object.keys(comp.parameters).length > 0) {
          const patchMsg = await api.patchComponent(circuitId, payload.name, {
            parameters: comp.parameters as Record<string, number>,
          });
          versionRef.current = patchMsg.modelVersion;
          newComp.parameters = { ...comp.parameters };
        }
        dispatch({
          type: 'COMPONENT_UPSERT',
          component: newComp,
          version: versionRef.current,
        });
        clones.push(payload.name);
      } catch (e) {
        reportError(e);
      }
    }

    if (clones.length > 0) {
      dispatch({ type: 'CLEAR_SELECTION' });
      for (const name of clones) {
        dispatch({ type: 'SELECT', name, additive: true });
      }
      dispatch({ type: 'PANEL_FOR', name: clones[0] });
    }
  }, [reportError]);

  const nudgeTimerRef = useRef<number | null>(null);
  const pendingNudgesRef = useRef<Record<string, { x: number; y: number }>>({});

  const nudgeSelection = useCallback(
    (dx: number, dy: number) => {
      const current = stateRef.current;
      if (current.selection.length === 0 || !current.circuitId) return;

      dispatch({ type: 'SELECTION_NUDGE', dx, dy });

      for (const name of current.selection) {
        const comp = current.components.find((c) => c.name === name);
        if (!comp) continue;
        const currentPos = pendingNudgesRef.current[name] || { x: comp.position[0], y: comp.position[1] };
        pendingNudgesRef.current[name] = { x: currentPos.x + dx, y: currentPos.y + dy };
      }

      if (nudgeTimerRef.current !== null) {
        clearTimeout(nudgeTimerRef.current);
      }

      nudgeTimerRef.current = window.setTimeout(() => {
        const moves = Object.entries(pendingNudgesRef.current).map(([name, pos]) => ({
          name,
          x: pos.x,
          y: pos.y,
        }));
        pendingNudgesRef.current = {};
        if (moves.length > 0) {
          commitMove(moves);
        }
      }, 400);
    },
    [commitMove],
  );

  const undo = useCallback(async () => {
    const circuitId = stateRef.current.circuitId;
    if (!circuitId) return;
    try {
      const msg = await api.undo(circuitId);
      versionRef.current = msg.modelVersion;
      await refresh(circuitId);
      dispatch({ type: 'STATUS', status: 'Undo applied' });
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
      dispatch({ type: 'STATUS', status: 'Redo applied' });
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
    (name: string, key: string, value: number | string) => {
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

  const stopPolling = useCallback(() => {
    if (simPollTimerRef.current !== null) {
      clearInterval(simPollTimerRef.current);
      simPollTimerRef.current = null;
    }
  }, []);

  const stopStream = useCallback(() => {
    simStreamStopRef.current?.();
    simStreamStopRef.current = null;
  }, []);

  /** REST polling fallback for when the SSE stream cannot be established. */
  const startPolling = useCallback((simId: string) => {
    stopPolling();
    const pollInterval = window.setInterval(async () => {
      if (currentSimIdRef.current !== simId) {
        clearInterval(pollInterval);
        return;
      }
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
          setSimResults(current.results || (await api.getSimulationResults(simId)));
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
  }, [stopPolling]);

  const finalizeSimulation = useCallback(async (simId: string) => {
    stopStream();
    try {
      const current = await api.getSimulation(simId);
      setSimStatus(current.status);
      if (current.status === 'COMPLETED') {
        setSimProgress(1.0);
        setSimResults(current.results || (await api.getSimulationResults(simId)));
      } else if (current.status === 'FAILED' || current.status === 'CANCELLED') {
        setSimError(current.errorMessage || 'Simulation failed or was cancelled');
      } else {
        startPolling(simId);
      }
    } catch (err) {
      setSimStatus('FAILED');
      setSimError((err as Error).message);
    }
  }, [startPolling, stopStream]);

  const runSimulation = useCallback(
    async (config?: {
      simulationTime?: number;
      timeStep?: number;
      solverType?: string;
      backend?: string;
      signals?: string[];
    }) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;

      const simTime = config?.simulationTime ?? simDefaultsRef.current?.duration ?? 0.02;
      const tStep = config?.timeStep ?? simDefaultsRef.current?.timeStep ?? 1e-6;
      const solver = config?.solverType ?? simDefaultsRef.current?.solverType ?? 'backward-euler';

      stopPolling();
      stopStream();

      setSimStatus('RUNNING');
      setSimProgress(0.05);
      setSimError(null);
      setSimDrawerOpen(true);

      try {
        const sim = await api.submitSimulation({
          circuitId,
          simulationTime: simTime,
          timeStep: tStep,
          solverType: solver,
          backend: config?.backend,
          signals: config?.signals,
        });
        currentSimIdRef.current = sim.simulationId;

        // Live progress via SSE; REST polling only as connection fallback
        simStreamStopRef.current = api.streamSimulationProgress(sim.simulationId, {
          onProgress: (progress) => setSimProgress(Math.max(0.05, progress)),
          onComplete: () => void finalizeSimulation(sim.simulationId),
          onSimError: () => void finalizeSimulation(sim.simulationId),
          onConnectionError: () => startPolling(sim.simulationId),
        });
      } catch (err) {
        setSimStatus('FAILED');
        const msg = (err as Error).message;
        setSimError(msg);
        dispatch({ type: 'STATUS', status: `Simulation failed: ${msg}` });
      }
    },
    [finalizeSimulation, startPolling, stopPolling, stopStream],
  );

  const cancelSimulation = useCallback(async () => {
    stopPolling();
    stopStream();
    const simId = currentSimIdRef.current;
    if (simId) {
      try {
        await api.cancelSimulation(simId);
      } catch {
        // ignore
      }
    }
    setSimStatus('CANCELLED');
  }, [stopPolling, stopStream]);

  const pauseSimulation = useCallback(async () => {
    const simId = currentSimIdRef.current;
    if (!simId) return;
    try {
      const response = await api.pauseSimulation(simId);
      setSimStatus(response.status);
    } catch (err) {
      setSimError((err as Error).message);
    }
  }, []);

  const resumeSimulation = useCallback(async () => {
    const simId = currentSimIdRef.current;
    if (!simId) return;
    try {
      const response = await api.resumeSimulation(simId);
      setSimStatus(response.status);
    } catch (err) {
      setSimError((err as Error).message);
    }
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
      defaults: simDefaults,
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
      duplicateSelection,
      nudgeSelection,
      undo,
      redo,
      save,
      rename,
      setParameter,
      setLabel,
      openProperties: (name: string) => dispatch({ type: 'PANEL_FOR', name }),
      runSimulation,
      cancelSimulation,
      pauseSimulation,
      resumeSimulation,
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
