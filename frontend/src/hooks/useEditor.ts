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
import { nextOrientation, terminalPositions, findPlacementConflict } from '../model/geometry';
import {
  CTRL_TYPE,
  isVoltmeterComponent,
  isAmmeterComponent,
  getCoupledComponentName,
  getComponentMeta,
} from '../model/componentSchema';
import { isScopeComponent } from '../simulation/scopes';
import { BLANK_CIRCUIT_IPES } from '../model/examples';
import { SIMULATION_DEFAULTS } from '../model/constants';
import { flipRoute, densePoints, routeMovedWire, deconflictMovedWires } from '../canvas/WireRouter';

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
  const [simWarnings, setSimWarnings] = useState<string[]>([]);
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

  /** Clears simulation UI state and stops live updates; used when switching
   *  circuits so results from the previous example never leak through. */
  const resetSimulationState = useCallback(() => {
    stopPolling();
    stopStream();
    if (currentSimIdRef.current) {
      // cancel a still-running simulation of the old circuit (best effort)
      void api.cancelSimulation(currentSimIdRef.current).catch(() => {});
      currentSimIdRef.current = null;
    }
    setSimStatus(null);
    setSimProgress(0);
    setSimResults(null);
    setSimError(null);
    setSimWarnings([]);
    setSimDrawerOpen(false);
  }, [stopPolling, stopStream]);

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
      resetSimulationState();
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
      resetSimulationState();
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
    [attachSubscription, refresh, reportError, resetSimulationState],
  );

  const newCircuit = useCallback(async () => {
    await openContent(BLANK_CIRCUIT_IPES, 'Untitled.ipes');
  }, [openContent]);

  /** Opens a circuit the desktop shell handed over (base64 of gzip or plain). */
  const openBase64 = useCallback(
    async (base64: string, filename = 'circuit.ipes') => {
      resetSimulationState();
      dispatch({ type: 'STATUS', status: `Loading ${filename}...` });
      try {
        const circuitId = await api.uploadIpesBase64(base64, filename);
        attachSubscription(circuitId);
        await refresh(circuitId);
        dispatch({ type: 'STATUS', status: `Loaded ${filename}` });
      } catch (e) {
        reportError(e);
      }
    },
    [attachSubscription, refresh, reportError],
  );

  // Shared in-flight promise so the auto-init effect and a concurrent arm()
  // create the blank workspace only once
  const creatingWorkspaceRef = useRef<Promise<boolean> | null>(null);

  const createBlankWorkspace = useCallback(async () => {
    dispatch({ type: 'STATUS', status: 'Initializing workspace...' });
    try {
      const circuitId = await api.uploadIpesString(BLANK_CIRCUIT_IPES, 'Untitled.ipes');
      attachSubscription(circuitId);
      await refresh(circuitId);
      return true;
    } catch (e) {
      reportError(e);
      return false;
    } finally {
      creatingWorkspaceRef.current = null;
    }
  }, [attachSubscription, refresh, reportError]);

  const ensureWorkspace = useCallback(() => {
    if (stateRef.current.circuitId) {
      return Promise.resolve(true);
    }
    creatingWorkspaceRef.current ??= createBlankWorkspace();
    return creatingWorkspaceRef.current;
  }, [createBlankWorkspace]);

  // Automatically initialize a blank workspace on initial page load / reload
  useEffect(() => {
    void ensureWorkspace();
  }, [ensureWorkspace]);

  const arm = useCallback(
    async (entry: CatalogEntry) => {
      // Components flagged disabled in the schema (motors, thermal modules)
      // have no engine model yet — refuse to arm them so nobody expects a
      // working simulation from a placed part.
      if (getComponentMeta(entry.type, entry.family, entry.name).disabled) {
        dispatch({
          type: 'STATUS',
          status: `⚠️ ${getComponentMeta(entry.type, entry.family, entry.name).displayName} is not available yet — this component type is planned, but the engine cannot simulate it at the moment`,
        });
        return;
      }
      const ready = await ensureWorkspace();
      if (!ready) return;
      dispatch({ type: 'ARM', componentType: entry.type, family: entry.family });
    },
    [ensureWorkspace],
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

      // Block body-on-body / terminal-in-body placements: stacked components
      // silently short in the netlist. The ghost stays armed so the user can
      // reposition instead of re-selecting from the palette.
      const conflict = findPlacementConflict(
        { type, family, position: [x, y], orientation: orient },
        stateRef.current.components,
      );
      if (conflict) {
        dispatch({
          type: 'STATUS',
          status: `⚠️ Cannot place here — overlaps ${conflict}. Move to a free spot.`,
        });
        return;
      }

      dispatch({ type: 'CANCEL' });
      const defaultParams =
        type === CTRL_TYPE.SCRIPT || type === CTRL_TYPE.LEGACY_JAVA_FUNCTION
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

  const setLabel = useCallback(
    (component: string, side: 'x' | 'y', indexOrLabel: number | string, maybeLabel?: string) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      const index = typeof indexOrLabel === 'number' ? indexOrLabel : 0;
      const label = typeof indexOrLabel === 'number' ? (maybeLabel ?? '') : indexOrLabel;

      const existing = stateRef.current.components.find((c) => c.name === component);
      const oldLabel = existing
        ? (side === 'x' ? existing.inputLabels?.[index] : existing.outputLabels?.[index]) || ''
        : '';

      api
        .setNodeLabel(circuitId, component, index, side, label)
        .then((msg) => {
          versionRef.current = msg.modelVersion;
          if (existing) {
            const arr = side === 'x' ? [...(existing.inputLabels ?? [])] : [...(existing.outputLabels ?? [])];
            while (arr.length <= index) {
              arr.push('');
            }
            arr[index] = label;
            const updated =
              side === 'x'
                ? { ...existing, inputLabels: arr }
                : { ...existing, outputLabels: arr };
            dispatch({ type: 'COMPONENT_UPSERT', component: updated, version: msg.modelVersion });

            // Adding a scope channel shifts every input pin one row (the pin
            // block is centered on the symbol), which would silently orphan
            // the wires attached to the old pin positions. Re-bind wire ends
            // from the old pin grid to the new one so the wiring follows.
            if (side === 'x' && isScopeComponent(existing) && arr.length > (existing.inputLabels ?? []).length) {
              const oldPins = terminalPositions(existing).input;
              const newPins = terminalPositions(updated).input;
              stateRef.current.wires.forEach((w, wIdx) => {
                const pts = w.points ?? [];
                if (pts.length < 2) return;
                const endIndices = [0, pts.length - 1];
                for (const pi of endIndices) {
                  const oldPinIdx = oldPins.findIndex((p) => p.x === pts[pi][0] && p.y === pts[pi][1]);
                  if (oldPinIdx < 0 || oldPinIdx >= newPins.length) continue;
                  const np = newPins[oldPinIdx];
                  if (np.x === pts[pi][0] && np.y === pts[pi][1]) continue;
                  const newPoints = pts.map((q, qi) => (qi === pi ? [np.x, np.y] : q));
                  api
                    .patchConnection(circuitId, wIdx, { points: newPoints })
                    .then((wireMsg) => {
                      const payload = wireMsg.payload as WirePayload;
                      dispatch({
                        type: 'WIRE_PATCHED',
                        index: wIdx,
                        points: payload.points,
                        label: payload.label,
                        version: wireMsg.modelVersion,
                      });
                    })
                    .catch(() => {});
                }
              });
            }

            // If an output signal / probe terminal is renamed, automatically propagate to consumer Scope channels & wires
            if (side === 'y' && oldLabel && label && oldLabel !== label) {
              for (const other of stateRef.current.components) {
                if (isScopeComponent(other) && other.inputLabels) {
                  other.inputLabels.forEach((chSig, chIdx) => {
                    if (chSig === oldLabel) {
                      setLabel(other.name, 'x', chIdx, label);
                    }
                  });
                }
              }
              stateRef.current.wires.forEach((w, wIdx) => {
                if (w.label === oldLabel) {
                  api
                    .patchConnection(circuitId, wIdx, { label })
                    .then((wireMsg) => {
                      const payload = wireMsg.payload as WirePayload;
                      dispatch({
                        type: 'WIRE_PATCHED',
                        index: wIdx,
                        points: payload.points,
                        label: payload.label,
                        version: wireMsg.modelVersion,
                      });
                    })
                    .catch(() => {});
                }
              });
            }
          }
        })
        .catch(reportError);
    },
    [reportError],
  );

  const checkAndPropagateScopeWire = useCallback(
    (points: number[][]) => {
      if (!points || points.length < 2) return;
      const pStart = { x: points[0][0], y: points[0][1] };
      const pEnd = { x: points[points.length - 1][0], y: points[points.length - 1][1] };
      const components = stateRef.current.components;

      let scopeComp: (typeof components)[number] | null = null;
      let scopeChannel = -1;
      let otherPoint: { x: number; y: number } | null = null;

      for (const comp of components) {
        if (isScopeComponent(comp)) {
          const terms = terminalPositions(comp);
          for (let i = 0; i < terms.input.length; i++) {
            const pin = terms.input[i];
            if (Math.hypot(pin.x - pEnd.x, pin.y - pEnd.y) < 0.25) {
              scopeComp = comp;
              scopeChannel = i;
              otherPoint = pStart;
              break;
            } else if (Math.hypot(pin.x - pStart.x, pin.y - pStart.y) < 0.25) {
              scopeComp = comp;
              scopeChannel = i;
              otherPoint = pEnd;
              break;
            }
          }
          if (scopeComp) break;
        }
      }

      if (!scopeComp || scopeChannel < 0 || !otherPoint) return;

      // Detect signal at otherPoint
      let detectedSignal = '';

      // 1. Check other components' outputs
      for (const comp of components) {
        if (comp.name === scopeComp.name) continue;
        const terms = terminalPositions(comp);
        for (let i = 0; i < terms.output.length; i++) {
          const pin = terms.output[i];
          if (Math.hypot(pin.x - otherPoint.x, pin.y - otherPoint.y) < 0.25) {
            const outLabel = comp.outputLabels?.[i];
            if (outLabel && outLabel.trim() && outLabel !== 'NIX_NIX_NIX') {
              detectedSignal = outLabel.trim();
            } else if (isVoltmeterComponent(comp)) {
              const coupled = getCoupledComponentName(comp);
              const nodeA = comp.parameters?.nodeA as string;
              const target = coupled || nodeA || comp.name;
              detectedSignal = `u_${target.replace(/[^a-zA-Z0-9]/g, '')}`;
              setLabel(comp.name, 'y', 0, detectedSignal);
            } else if (isAmmeterComponent(comp)) {
              const coupled = getCoupledComponentName(comp);
              const target = coupled || comp.name;
              detectedSignal = `i_${target.replace(/[^a-zA-Z0-9]/g, '')}`;
              setLabel(comp.name, 'y', 0, detectedSignal);
            } else {
              detectedSignal = comp.name;
            }
            break;
          }
        }
        if (detectedSignal) break;
      }

      // 2. Check labeled wires
      if (!detectedSignal) {
        for (const wire of stateRef.current.wires) {
          if (wire.label && wire.label.trim() && wire.label !== 'NIX_NIX_NIX') {
            for (const pt of wire.points) {
              if (Math.hypot(pt[0] - otherPoint.x, pt[1] - otherPoint.y) < 0.25) {
                detectedSignal = wire.label.trim();
                break;
              }
            }
          }
          if (detectedSignal) break;
        }
      }

      // 3. Check labeled terminals of power/control components
      if (!detectedSignal) {
        for (const comp of components) {
          const terms = terminalPositions(comp);
          for (let i = 0; i < terms.input.length; i++) {
            if (Math.hypot(terms.input[i].x - otherPoint.x, terms.input[i].y - otherPoint.y) < 0.25) {
              const lbl = comp.inputLabels?.[i];
              if (lbl && lbl.trim() && lbl !== 'NIX_NIX_NIX') detectedSignal = lbl.trim();
              break;
            }
          }
          if (detectedSignal) break;
          for (let i = 0; i < terms.output.length; i++) {
            if (Math.hypot(terms.output[i].x - otherPoint.x, terms.output[i].y - otherPoint.y) < 0.25) {
              const lbl = comp.outputLabels?.[i];
              if (lbl && lbl.trim() && lbl !== 'NIX_NIX_NIX') detectedSignal = lbl.trim();
              break;
            }
          }
          if (detectedSignal) break;
        }
      }

      if (detectedSignal) {
        setLabel(scopeComp.name, 'x', scopeChannel, detectedSignal);
        dispatch({
          type: 'STATUS',
          status: `Connected signal '${detectedSignal}' to ${scopeComp.name} CH${scopeChannel + 1}`,
        });
      }
    },
    [setLabel],
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
          checkAndPropagateScopeWire(points);
        })
        .catch(reportError);
    },
    [checkAndPropagateScopeWire, reportError],
  );

  const commitMove = useCallback(
    (
      moves: { name: string; x: number; y: number }[],
      wirePatches?: { index: number; points: number[][] }[],
      postStatus?: string,
    ) => {
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
          // persist any orthogonally adjusted wires before refreshing
          if (wirePatches && wirePatches.length > 0) {
            await Promise.all(
              wirePatches.map((wp) =>
                api.patchConnection(circuitId, wp.index, { points: wp.points }),
              ),
            );
          }
          await refresh(circuitId);
          dispatch({
            type: 'STATUS',
            status: postStatus ?? `${moves.length} component(s) moved`,
          });
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
      const oldTerms = terminalPositions(comp);
      const newTerms = terminalPositions({ ...comp, orientation: nextOrient });

      const terminalDeltas = new Map<string, { dx: number; dy: number }>();
      oldTerms.input.forEach((oldPt, i) => {
        const newPt = newTerms.input[i];
        if (newPt) {
          terminalDeltas.set(`${oldPt.x},${oldPt.y}`, {
            dx: newPt.x - oldPt.x,
            dy: newPt.y - oldPt.y,
          });
        }
      });
      oldTerms.output.forEach((oldPt, j) => {
        const newPt = newTerms.output[j];
        if (newPt) {
          terminalDeltas.set(`${oldPt.x},${oldPt.y}`, {
            dx: newPt.x - oldPt.x,
            dy: newPt.y - oldPt.y,
          });
        }
      });

      const wirePatches: { index: number; points: number[][] }[] = [];
      const updatedWires = stateRef.current.wires.map((wire) => {
        if (!wire.points || wire.points.length < 2) return wire;
        const startPt = wire.points[0];
        const endPt = wire.points[wire.points.length - 1];
        const startDelta = terminalDeltas.get(`${startPt[0]},${startPt[1]}`) || { dx: 0, dy: 0 };
        const endDelta = terminalDeltas.get(`${endPt[0]},${endPt[1]}`) || { dx: 0, dy: 0 };

        if (startDelta.dx !== 0 || startDelta.dy !== 0 || endDelta.dx !== 0 || endDelta.dy !== 0) {
          const newPoints = routeMovedWire(wire.points, startDelta, endDelta);
          wirePatches.push({ index: wire.index, points: newPoints });
          return { ...wire, points: newPoints };
        }
        return wire;
      });

      // Rotation slides corners locally; re-check the changed wires against
      // component bodies and untouched wires so they do not land on top of them.
      const changedIdx: number[] = [];
      updatedWires.forEach((w, i) => {
        if (w !== stateRef.current.wires[i]) {
          changedIdx.push(i);
        }
      });
      if (changedIdx.length > 0) {
        const rotatedComponents = stateRef.current.components.map((c) =>
          c.name === name ? { ...c, orientation: nextOrient } : c,
        );
        const changed = new Set(changedIdx);
        const slidRoutes = changedIdx.map((i) => updatedWires[i].points);
        const staticRoutes = updatedWires
          .filter((_, i) => !changed.has(i))
          .map((w) => w.points);
        const deconflicted = deconflictMovedWires(slidRoutes, staticRoutes, rotatedComponents);
        changedIdx.forEach((wi, k) => {
          const previous = wirePatches.find((wp) => wp.index === updatedWires[wi].index);
          if (previous) {
            previous.points = deconflicted[k];
          }
          updatedWires[wi] = { ...updatedWires[wi], points: deconflicted[k] };
        });
      }

      dispatch({
        type: 'ROTATE_COMPONENT',
        name,
        orientation: nextOrient,
        wires: updatedWires,
      });

      api
        .patchComponent(circuitId, name, { orientation: nextOrient })
        .then(async (msg) => {
          versionRef.current = msg.modelVersion;
          if (wirePatches.length > 0) {
            await Promise.all(
              wirePatches.map((wp) =>
                api.patchConnection(circuitId, wp.index, { points: wp.points }),
              ),
            );
          }
          await refresh(circuitId);
        })
        .catch(reportError);
    },
    [refresh, reportError],
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
          void refresh(circuitId);
        })
        .catch(reportError);
    },
    [refresh, reportError],
  );

  const patchWirePoints = useCallback(
    (index: number, points: number[][]) => {
      const circuitId = stateRef.current.circuitId;
      if (!circuitId) return;
      api
        .patchConnection(circuitId, index, { points })
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
        .catch((e) => {
          reportError(e);
          void refresh(circuitId);
        });
    },
    [refresh, reportError],
  );

  const flipWire = useCallback(
    (index: number) => {
      const wire = stateRef.current.wires.find((w) => w.index === index);
      if (!wire) return;
      const flipped = flipRoute(wire.points);
      const dense = densePoints(flipped.map(([x, y]) => ({ x, y }))).map((pt) => [pt.x, pt.y]);
      patchWirePoints(index, dense);
    },
    [patchWirePoints],
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

  const deleteSelection = useCallback(async () => {
    const current = stateRef.current;
    const circuitId = current.circuitId;
    if (!circuitId) return;

    const wireIndicesToDelete = current.selectedWires && current.selectedWires.length > 0
      ? [...current.selectedWires]
      : current.selectedWire !== null
        ? [current.selectedWire]
        : [];

    wireIndicesToDelete.sort((a, b) => b - a);

    try {
      for (const name of current.selection) {
        const msg = await api.deleteComponent(circuitId, name);
        versionRef.current = msg.modelVersion;
        dispatch({ type: 'COMPONENT_DELETED', name, version: msg.modelVersion });
      }
      for (const index of wireIndicesToDelete) {
        const msg = await api.deleteConnection(circuitId, index);
        versionRef.current = msg.modelVersion;
        dispatch({ type: 'WIRE_DELETED', index, version: msg.modelVersion });
      }
    } catch (e) {
      reportError(e);
    } finally {
      void refresh(circuitId);
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
          // SELECTION_NUDGE already slid wire ends along with the moved
          // components in local state — persist them, or the post-commit
          // refresh would snap the wires back to their pre-nudge routes.
          const movedNames = new Set(moves.map((m) => m.name));
          const terminalSet = new Set<string>();
          for (const comp of stateRef.current.components) {
            if (!movedNames.has(comp.name)) continue;
            const terms = terminalPositions(comp);
            for (const t of [...terms.input, ...terms.output]) {
              terminalSet.add(`${t.x},${t.y}`);
            }
          }
          const wirePatches = stateRef.current.wires
            .filter((w) => {
              if (!w.points || w.points.length < 2) return false;
              const startKey = `${w.points[0][0]},${w.points[0][1]}`;
              const endKey = `${w.points[w.points.length - 1][0]},${w.points[w.points.length - 1][1]}`;
              return terminalSet.has(startKey) || terminalSet.has(endKey);
            })
            .map((w) => ({ index: w.index, points: w.points }));
          commitMove(moves, wirePatches);
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

  // ========== Simulation Actions ==========

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
          setSimWarnings(current.warnings || []);
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
    }, SIMULATION_DEFAULTS.POLL_INTERVAL_MS);
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
        setSimWarnings(current.warnings || []);
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

      // Dynamically collect active signals from scope channels, probe output labels, and wire labels
      const activeSignals = new Set<string>();
      if (config?.signals && config.signals.length > 0) {
        config.signals.forEach((s) => s && s.trim() && activeSignals.add(s.trim()));
      } else if (simDefaultsRef.current?.signals) {
        simDefaultsRef.current.signals.forEach((s) => s && s.trim() && activeSignals.add(s.trim()));
      }

      for (const comp of stateRef.current.components) {
        if (isScopeComponent(comp)) {
          comp.inputLabels?.forEach((lbl) => {
            if (lbl && lbl.trim() && lbl !== 'NIX_NIX_NIX') activeSignals.add(lbl.trim());
          });
        }
        if (isVoltmeterComponent(comp) || isAmmeterComponent(comp)) {
          comp.outputLabels?.forEach((lbl) => {
            if (lbl && lbl.trim() && lbl !== 'NIX_NIX_NIX') activeSignals.add(lbl.trim());
          });
        }
      }

      for (const wire of stateRef.current.wires) {
        if (wire.label && wire.label.trim() && wire.label !== 'NIX_NIX_NIX') {
          activeSignals.add(wire.label.trim());
        }
      }

      const signalsToSimulate = activeSignals.size > 0 ? Array.from(activeSignals) : undefined;

      stopPolling();
      stopStream();

      setSimStatus('RUNNING');
      setSimProgress(0.05);
      setSimError(null);
      setSimWarnings([]);
      setSimDrawerOpen(true);

      try {
        const sim = await api.submitSimulation({
          circuitId,
          simulationTime: simTime,
          timeStep: tStep,
          solverType: solver,
          backend: config?.backend,
          signals: signalsToSimulate,
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
      warnings: simWarnings,
      isOpen: simDrawerOpen,
      defaults: simDefaults,
    },
    actions: {
      open,
      openContent,
      openBase64,
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
      patchWirePoints,
      flipWire,
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
