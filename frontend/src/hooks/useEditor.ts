/**
 * Editor controller: owns the reducer state, exposes action creators that
 * perform the REST calls and feed results back into the store, and keeps
 * the WebSocket subscription for external change detection alive.
 */
import { useCallback, useEffect, useReducer, useRef, useState } from 'react';
import * as api from '../api/client';
import { initialState, editorReducer } from '../model/store';
import type { CatalogEntry, ComponentPayload, WirePayload } from '../model/types';

export function useEditor() {
  const [state, dispatch] = useReducer(editorReducer, initialState);
  const [catalog, setCatalog] = useState<CatalogEntry[]>([]);
  const [wsConnected, setWsConnected] = useState(false);
  const versionRef = useRef(0);
  const unsubscribeRef = useRef<(() => void) | null>(null);
  // latest state for callbacks that need current values without
  // re-creating on every state change
  const stateRef = useRef(state);
  stateRef.current = state;

  useEffect(() => {
    api.getCatalog().then((c) => setCatalog(c.types)).catch(() => setCatalog([]));
  }, []);

  const refresh = useCallback(
    async (circuitId: string) => {
      try {
        const snapshot = await api.getEditorModel(circuitId);
        versionRef.current = snapshot.modelVersion;
        dispatch({ type: 'SNAPSHOT', snapshot });
      } catch (e) {
        dispatch({ type: 'STATUS', status: `Refresh failed: ${(e as Error).message}` });
      }
    },
    [],
  );

  const reportError = useCallback((e: unknown) => {
    dispatch({ type: 'STATUS', status: `Error: ${(e as Error).message}` });
  }, []);

  const open = useCallback(
    async (file: File) => {
      dispatch({ type: 'STATUS', status: `Loading ${file.name}...` });
      try {
        const circuitId = await api.uploadIpes(file);
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
        await refresh(circuitId);
        dispatch({ type: 'STATUS', status: '' });
      } catch (e) {
        reportError(e);
      }
    },
    [refresh, reportError],
  );

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
    (x: number, y: number, orientation: number) => {
      const ghost = stateRef.current.ghost;
      const circuitId = stateRef.current.circuitId;
      if (!ghost || !circuitId) return;
      dispatch({ type: 'CANCEL' });
      api
        .createComponent(circuitId, {
          family: ghost.family,
          type: ghost.type,
          x,
          y,
          orientation,
        })
        .then((msg) => {
          const payload = msg.payload as ComponentPayload;
          versionRef.current = msg.modelVersion;
          dispatch({
            type: 'COMPONENT_UPSERT',
            component: toEditorComponent(payload, ghost.family),
            version: msg.modelVersion,
          });
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
          dispatch({ type: 'WIRE_DELETED', index: current.selectedWire!, version: msg.modelVersion });
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

  return {
    state,
    dispatch,
    catalog,
    wsConnected,
    actions: {
      open,
      arm,
      cancel,
      toggleWireMode,
      placeGhost,
      finishWire,
      commitMove,
      deleteSelection,
      undo,
      redo,
      save,
      rename,
      setParameter,
      setLabel,
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
