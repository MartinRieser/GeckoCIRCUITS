/**
 * Floating canvas context menu for components, wires, and empty schematic areas.
 *
 * Provides direct right-click access to property editing, oscilloscope probing,
 * gate/meter coupling, rotation (CW/CCW), duplication, quick placement, DRC,
 * net labeling, and view controls.
 */
import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ControlComponentType,
  LkComponentType,
  SENTINEL_UNSET,
  SpecialComponentType,
} from '../model/constants';
import {
  getCoupledComponentName,
  isAmmeterComponent,
  isCircuitPowerComponent,
  isGateDriver,
  isVoltmeterComponent,
} from '../model/componentSchema';
import type { EditorComponent } from '../model/types';

/** Hit-test target descriptor supplied when right-clicking on the schematic sheet. */
export interface ContextMenuTarget {
  /** Hit target category. */
  type: 'component' | 'wire' | 'canvas';
  /** Component name when `type === 'component'`. */
  name?: string;
  /** Zero-based wire index when `type === 'wire'`. */
  wireIndex?: number;
  /** Assigned wire label when `type === 'wire'`. */
  wireLabel?: string;
  /** Resolved electrical net name connected to the right-clicked wire. */
  resolvedNetName?: string;
  /** Number of component terminals attached to the net, if analyzed. */
  connectedPins?: number;
  /** Grid X coordinate at the right-click location. */
  gridX: number;
  /** Grid Y coordinate at the right-click location. */
  gridY: number;
}

/** Palette quick-place item metadata shown inside the canvas Quick Place flyout. */
export interface QuickPlaceOption {
  /** Display label. */
  label: string;
  /** Icon glyph. */
  icon: string;
  /** Numeric component type identifier. */
  type: number;
  /** Component family identifier (`LK`, `CONTROL`, `ANNOTATION`, etc.). */
  family: string;
}

/** Standard components offered directly in the right-click Quick Place submenu. */
const QUICK_PLACE_OPTIONS: QuickPlaceOption[] = [
  { label: 'Ground (GND)', icon: '⏚', type: LkComponentType.GLOBAL_TERMINAL, family: 'LK' },
  { label: 'Resistor', icon: '⧠', type: LkComponentType.RESISTOR, family: 'LK' },
  { label: 'Inductor', icon: '⌇', type: LkComponentType.INDUCTOR, family: 'LK' },
  { label: 'Capacitor', icon: '⊣⊢', type: LkComponentType.CAPACITOR, family: 'LK' },
  { label: 'Diode', icon: '▷|', type: LkComponentType.DIODE, family: 'LK' },
  { label: 'MOSFET', icon: '⏣', type: LkComponentType.MOSFET, family: 'LK' },
  { label: 'V-Source', icon: '⚡', type: LkComponentType.VOLTAGE_SOURCE, family: 'LK' },
  { label: 'Text Note', icon: '📝', type: SpecialComponentType.TEXTBOX, family: 'ANNOTATION' },
];

/** Controllable semiconductor switch component types eligible for GATE driver coupling. */
const SWITCH_TYPES: ReadonlySet<number> = new Set([
  LkComponentType.MOSFET,
  LkComponentType.IGBT,
  LkComponentType.THYRISTOR,
  LkComponentType.IDEAL_SWITCH,
]);

/** Props for the schematic floating right-click ContextMenu. */
export interface ContextMenuProps {
  /** Menu horizontal viewport coordinate in pixels. */
  x: number;
  /** Menu vertical viewport coordinate in pixels. */
  y: number;
  /** Context target metadata at the click point. */
  target: ContextMenuTarget;
  /** Full component record when `target.type === 'component'`. */
  component?: EditorComponent;
  /** All schematic components for coupling and target discovery. */
  components?: EditorComponent[];
  /** Currently plotted oscilloscope signal names for toggle checkmarks. */
  activeScopeSignals?: string[];
  /** Closes the context menu. */
  onClose: () => void;
  /** Rotates a component 90° clockwise or counter-clockwise (`ccw = true`). */
  onRotate?: (name: string, ccw?: boolean) => void;
  /** Duplicates the right-clicked component. */
  onDuplicate?: (name: string) => void;
  /** Deletes the right-clicked component. */
  onDeleteComponent?: (name: string) => void;
  /** Deletes the right-clicked wire segment. */
  onDeleteWire?: (index: number) => void;
  /** Opens net label editing for the right-clicked wire. */
  onLabelWire?: (index: number) => void;
  /** Assigns ground net `'0'` to the right-clicked wire. */
  onSetWireGround?: (index: number) => void;
  /** Flips the orthogonal corner elbow orientation of a wire. */
  onFlipWire?: (index: number) => void;
  /** Highlights the full connected electrical net on canvas. */
  onHighlightNet?: (index: number) => void;
  /** Adds a net voltage trace `V(net)` to the oscilloscope. */
  onPlotNetVoltage?: (index: number, netName: string) => void;
  /** Sets up or completes a differential net probe on the clicked wire. */
  onStartDiffProbe?: (index: number, netName: string) => void;
  /** Opens the properties inspector for the right-clicked component. */
  onOpenProperties?: (name: string) => void;
  /** Triggers rename workflow for the right-clicked component. */
  onRenameComponent?: (name: string) => void;
  /** Opens the dedicated Oscilloscope workspace tab for a SCOPE block. */
  onOpenScopeTab?: (name: string) => void;
  /** Opens the dedicated Script IDE workspace tab for a SCRIPT block. */
  onOpenScriptTab?: (name: string) => void;
  /** Toggles or adds a waveform signal trace to the active Oscilloscope register. */
  onProbeSignal?: (signalName: string) => void;
  /** Binds a GATE driver or meter to a target switch/component. */
  onAssignCoupling?: (sourceComponent: string, targetComponent: string) => void;
  /** Switches the canvas tool back to idle/select mode. */
  onSetSelectMode?: () => void;
  /** Toggles interactive wire drawing mode. */
  onToggleWireMode?: () => void;
  /** Toggles oscilloscope signal probe mode. */
  onToggleProbeMode?: () => void;
  /** Opens the Ctrl+K Command Palette / component search modal. */
  onOpenCommandPalette?: () => void;
  /** Places a component of `(type, family)` at schematic grid coordinates `(gridX, gridY)`. */
  onQuickPlaceAt?: (type: number, family: string, gridX: number, gridY: number) => void;
  /** Runs the circuit simulation immediately. */
  onRunSimulation?: () => void;
  /** Runs and opens the Diagnostics / Design Rule Check drawer. */
  onRunDrcCheck?: () => void;
  /** Opens the Simulation Configuration modal. */
  onOpenSimConfig?: () => void;
  /** Zooms and pans the schematic canvas to fit all circuit elements. */
  onZoomFit?: () => void;
  /** Resets canvas zoom to 100% and pan to origin. */
  onResetZoom?: () => void;
  /** Toggles schematic alignment grid visibility. */
  onToggleGrid?: () => void;
  /** Cycles net label display mode across schematic wires. */
  onCycleNetLabels?: () => void;
  /** Number of currently selected schematic components. */
  selectionCount?: number;
  /** Opens Save Selection as Snippet modal. */
  onSaveAsSnippet?: () => void;
}

/**
 * Floating schematic context menu adapting its actions to components, wires, or canvas coordinates.
 */
export const ContextMenu: React.FC<ContextMenuProps> = ({
  x,
  y,
  target,
  component,
  components,
  activeScopeSignals,
  onClose,
  onRotate,
  onDuplicate,
  onDeleteComponent,
  onDeleteWire,
  onLabelWire,
  onSetWireGround,
  onFlipWire,
  onHighlightNet,
  onPlotNetVoltage,
  onStartDiffProbe,
  onOpenProperties,
  onRenameComponent,
  onOpenScopeTab,
  onOpenScriptTab,
  onProbeSignal,
  onAssignCoupling,
  onSetSelectMode,
  onToggleWireMode,
  onToggleProbeMode,
  onOpenCommandPalette,
  onQuickPlaceAt,
  onRunSimulation,
  onRunDrcCheck,
  onOpenSimConfig,
  onZoomFit,
  onResetZoom,
  onToggleGrid,
  onCycleNetLabels,
  selectionCount = 0,
  onSaveAsSnippet,
}) => {
  const menuRef = useRef<HTMLDivElement>(null);
  const [openSubmenu, setOpenSubmenu] = useState<'coupling' | 'quickPlace' | null>(null);

  useEffect(() => {
    const handleOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('mousedown', handleOutside);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('mousedown', handleOutside);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose]);

  const act = (fn?: () => void) => {
    fn?.();
    onClose();
  };

  const activeSignalsSet = useMemo(
    () => new Set(activeScopeSignals ?? []),
    [activeScopeSignals],
  );

  // Coupling metadata for Switch <-> Gate Driver / Meters
  const couplingInfo = useMemo(() => {
    if (target.type !== 'component' || !component || !components) {
      return null;
    }
    if (SWITCH_TYPES.has(component.type)) {
      const gates = components.filter((c) => isGateDriver(c));
      const candidates = onAssignCoupling
        ? gates.map((g) => ({
            label: g.name,
            checked: getCoupledComponentName(g) === component.name,
            onSelect: () => onAssignCoupling(g.name, component.name),
          }))
        : [];
      if (onQuickPlaceAt) {
        candidates.push({
          label: '+ Place New Gate Driver',
          checked: false,
          onSelect: () =>
            onQuickPlaceAt(
              ControlComponentType.GATE,
              'CONTROL',
              Math.max(1, target.gridX - 4),
              target.gridY,
            ),
        });
      }
      if (candidates.length === 0) return null;
      return {
        label: '🔗 Coupled Gate Driver',
        candidates,
      };
    }

    if (!onAssignCoupling) {
      return null;
    }

    if (isGateDriver(component)) {
      const switches = components.filter((c) => SWITCH_TYPES.has(c.type));
      if (switches.length === 0) return null;
      const current = getCoupledComponentName(component);
      return {
        label: '🔗 Couple to Switch',
        candidates: switches.map((s) => ({
          label: s.name,
          checked: current === s.name,
          onSelect: () => onAssignCoupling(component.name, s.name),
        })),
      };
    }

    if (isAmmeterComponent(component) || isVoltmeterComponent(component)) {
      const powerTargets = components.filter((c) => isCircuitPowerComponent(c));
      if (powerTargets.length === 0) return null;
      const current = getCoupledComponentName(component);
      return {
        label: '🔗 Couple to Target',
        candidates: powerTargets.map((p) => ({
          label: p.name,
          checked: current === p.name,
          onSelect: () => onAssignCoupling(component.name, p.name),
        })),
      };
    }

    return null;
  }, [target.type, target.gridX, target.gridY, component, components, onAssignCoupling, onQuickPlaceAt]);

  // Clamp near viewport edges so the menu always remains usable on small viewports
  const safeLeft =
    typeof window !== 'undefined' && window.innerWidth > 260
      ? Math.min(x, window.innerWidth - 235)
      : x;
  const safeTop =
    typeof window !== 'undefined' && window.innerHeight > 420
      ? Math.min(y, window.innerHeight - 360)
      : y;

  const isPowerComp = !component || isCircuitPowerComponent(component);
  const isScopeComp = component?.type === ControlComponentType.SCOPE;
  const isScriptComp =
    component?.family === 'SPECIAL' &&
    component.type !== SpecialComponentType.TEXTBOX;

  const wireNetDisplay =
    (target.wireLabel && target.wireLabel !== SENTINEL_UNSET ? target.wireLabel : '') ||
    target.resolvedNetName ||
    '';

  return (
    <div
      ref={menuRef}
      className="context-menu"
      style={{ left: safeLeft, top: safeTop }}
      onContextMenu={(e) => e.preventDefault()}
      data-testid="canvas-context-menu"
    >
      {target.type === 'component' && target.name && (
        <>
          <div className="context-menu-header">
            <span>{target.name}</span>
            {component && <span className="context-menu-meta">{component.family}</span>}
          </div>

          <button
            type="button"
            className="context-menu-item"
            onClick={() => act(() => onOpenProperties?.(target.name!))}
          >
            <span>Properties</span>
            <span className="context-menu-shortcut">Enter</span>
          </button>

          {onRenameComponent && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(() => onRenameComponent(target.name!))}
            >
              <span>Rename Component...</span>
              <span className="context-menu-shortcut">F2</span>
            </button>
          )}

          {isScopeComp && onOpenScopeTab && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(() => onOpenScopeTab(target.name!))}
            >
              <span>📺 Open Oscilloscope Tab</span>
            </button>
          )}

          {isScriptComp && onOpenScriptTab && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(() => onOpenScriptTab(target.name!))}
            >
              <span>📝 Open Script Tab</span>
            </button>
          )}

          {onProbeSignal && (
            <>
              <div className="context-menu-sep" />
              {isPowerComp ? (
                <>
                  <button
                    type="button"
                    className="context-menu-item"
                    onClick={() => act(() => onProbeSignal(`V(${target.name})`))}
                  >
                    <span>
                      {activeSignalsSet.has(`V(${target.name})`) ? '✓ ' : ''}
                      📈 Plot Voltage V({target.name})
                    </span>
                  </button>
                  <button
                    type="button"
                    className="context-menu-item"
                    onClick={() => act(() => onProbeSignal(`I(${target.name})`))}
                  >
                    <span>
                      {activeSignalsSet.has(`I(${target.name})`) ? '✓ ' : ''}
                      📈 Plot Current I({target.name})
                    </span>
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={() => act(() => onProbeSignal(target.name!))}
                >
                  <span>
                    {activeSignalsSet.has(target.name) ? '✓ ' : ''}
                    📈 Plot Signal {target.name}
                  </span>
                </button>
              )}
            </>
          )}

          {couplingInfo && (
            <>
              <div className="context-menu-sep" />
              <div
                className="context-menu-submenu-wrap"
                onMouseEnter={() => setOpenSubmenu('coupling')}
              >
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={() =>
                    setOpenSubmenu((prev) => (prev === 'coupling' ? null : 'coupling'))
                  }
                >
                  <span>{couplingInfo.label}</span>
                  <span className="context-menu-shortcut">▸</span>
                </button>
                {openSubmenu === 'coupling' && (
                  <div className="context-menu-submenu">
                    {couplingInfo.candidates.map((cand) => (
                      <button
                        key={cand.label}
                        type="button"
                        className="context-menu-item"
                        onClick={() => act(cand.onSelect)}
                      >
                        <span>
                          {cand.checked ? '✓ ' : ''}
                          {cand.label}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </>
          )}

          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item"
            onClick={() => act(() => onRotate?.(target.name!))}
          >
            <span>Rotate 90°</span>
            <span className="context-menu-shortcut">R</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => act(() => onRotate?.(target.name!, true))}
          >
            <span>↺ Rotate 90° CCW</span>
            <span className="context-menu-shortcut">Shift+R</span>
          </button>
          {onDuplicate && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(() => onDuplicate(target.name!))}
            >
              <span>⧉ Duplicate</span>
              <span className="context-menu-shortcut">Ctrl+D</span>
            </button>
          )}
          {onSaveAsSnippet && (
            <button
              type="button"
              className="context-menu-item"
              data-testid="context-save-snippet"
              onClick={() => act(onSaveAsSnippet)}
            >
              <span>📦 Save Selection as Snippet...</span>
            </button>
          )}
          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item danger"
            onClick={() => act(() => onDeleteComponent?.(target.name!))}
          >
            <span>Delete</span>
            <span className="context-menu-shortcut">Del</span>
          </button>
        </>
      )}

      {target.type === 'wire' && target.wireIndex !== undefined && (
        <>
          <div className="context-menu-header">
            <span>Wire #{target.wireIndex}</span>
            {wireNetDisplay && (
              <span className="context-menu-meta">
                {wireNetDisplay}
                {target.connectedPins ? ` · ${target.connectedPins}p` : ''}
              </span>
            )}
          </div>

          {onPlotNetVoltage && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() =>
                act(() =>
                  onPlotNetVoltage(
                    target.wireIndex!,
                    wireNetDisplay || `net_${target.wireIndex}`,
                  ),
                )
              }
            >
              <span>📈 Plot Net Voltage V({wireNetDisplay || `net_${target.wireIndex}`})</span>
            </button>
          )}
          {onStartDiffProbe && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() =>
                act(() =>
                  onStartDiffProbe(
                    target.wireIndex!,
                    wireNetDisplay || `net_${target.wireIndex}`,
                  ),
                )
              }
            >
              <span>📍 Start Differential Probe...</span>
            </button>
          )}
          {(onPlotNetVoltage || onStartDiffProbe) && <div className="context-menu-sep" />}

          <button
            type="button"
            className="context-menu-item"
            onClick={() => act(() => onLabelWire?.(target.wireIndex!))}
          >
            <span>Net Label...</span>
          </button>
          {onSetWireGround && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(() => onSetWireGround(target.wireIndex!))}
            >
              <span>⏚ Set Net as Ground (0)</span>
            </button>
          )}
          {onFlipWire && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(() => onFlipWire(target.wireIndex!))}
            >
              <span>⇅ Flip Wire Corner Route</span>
              <span className="context-menu-shortcut">R</span>
            </button>
          )}
          {onHighlightNet && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(() => onHighlightNet(target.wireIndex!))}
            >
              <span>🔦 Highlight Connected Net</span>
            </button>
          )}
          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item danger"
            onClick={() => act(() => onDeleteWire?.(target.wireIndex!))}
          >
            <span>Delete Wire</span>
            <span className="context-menu-shortcut">Del</span>
          </button>
        </>
      )}

      {target.type === 'canvas' && (
        <>
          <div className="context-menu-header">
            <span>Canvas</span>
            <span className="context-menu-meta">
              ({target.gridX}, {target.gridY})
            </span>
          </div>

          {onSetSelectMode && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(onSetSelectMode)}
            >
              <span>↖ Select / Move Tool</span>
              <span className="context-menu-shortcut">Esc</span>
            </button>
          )}
          <button
            type="button"
            className="context-menu-item"
            onClick={() => act(onToggleWireMode)}
          >
            <span>Wire Tool</span>
            <span className="context-menu-shortcut">W</span>
          </button>
          {onToggleProbeMode && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(onToggleProbeMode)}
            >
              <span>📍 Probe Tool</span>
              <span className="context-menu-shortcut">P</span>
            </button>
          )}

          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item"
            onClick={() => act(onOpenCommandPalette)}
          >
            <span>Add Component...</span>
            <span className="context-menu-shortcut">Ctrl+K</span>
          </button>
          {onSaveAsSnippet && selectionCount >= 1 && (
            <button
              type="button"
              className="context-menu-item"
              data-testid="context-save-snippet"
              onClick={() => act(onSaveAsSnippet)}
            >
              <span>📦 Save Selection as Snippet...</span>
            </button>
          )}

          {onQuickPlaceAt && (
            <>
              <div
                className="context-menu-submenu-wrap"
                onMouseEnter={() => setOpenSubmenu('quickPlace')}
              >
                <button
                  type="button"
                  className="context-menu-item"
                  onClick={() =>
                    setOpenSubmenu((prev) => (prev === 'quickPlace' ? null : 'quickPlace'))
                  }
                >
                  <span>⚡ Quick Place</span>
                  <span className="context-menu-shortcut">▸</span>
                </button>
                {openSubmenu === 'quickPlace' && (
                  <div className="context-menu-submenu">
                    {QUICK_PLACE_OPTIONS.map((opt) => (
                      <button
                        key={`${opt.family}-${opt.type}`}
                        type="button"
                        className="context-menu-item"
                        onClick={() =>
                          act(() =>
                            onQuickPlaceAt(opt.type, opt.family, target.gridX, target.gridY),
                          )
                        }
                      >
                        <span>
                          {opt.icon} {opt.label}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <button
                type="button"
                className="context-menu-item"
                onClick={() =>
                  act(() =>
                    onQuickPlaceAt(
                      SpecialComponentType.TEXTBOX,
                      'ANNOTATION',
                      target.gridX,
                      target.gridY,
                    ),
                  )
                }
              >
                <span>📝 Add Schematic Note Here</span>
              </button>
            </>
          )}

          {(onRunSimulation || onRunDrcCheck || onOpenSimConfig) && (
            <div className="context-menu-sep" />
          )}
          {onRunSimulation && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(onRunSimulation)}
            >
              <span>▶ Run Simulation</span>
              <span className="context-menu-shortcut">F5</span>
            </button>
          )}
          {onRunDrcCheck && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(onRunDrcCheck)}
            >
              <span>🔍 Run Circuit Check (DRC)</span>
            </button>
          )}
          {onOpenSimConfig && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(onOpenSimConfig)}
            >
              <span>⚙ Simulation Setup...</span>
            </button>
          )}

          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item"
            onClick={() => act(onZoomFit)}
          >
            <span>Zoom to Fit</span>
          </button>
          {onResetZoom && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(onResetZoom)}
            >
              <span>Reset Zoom (100%)</span>
            </button>
          )}
          {onToggleGrid && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(onToggleGrid)}
            >
              <span>Toggle Grid</span>
              <span className="context-menu-shortcut">#</span>
            </button>
          )}
          {onCycleNetLabels && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => act(onCycleNetLabels)}
            >
              <span>Cycle Net Labels</span>
            </button>
          )}
        </>
      )}
    </div>
  );
};
