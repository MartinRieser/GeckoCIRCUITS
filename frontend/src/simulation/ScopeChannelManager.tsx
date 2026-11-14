/**
 * Modal & Drawer Dialog for managing oscilloscope channels and signal bindings.
 * Allows engineers to browse all recorded circuit signals (voltages, currents, control states)
 * and bind them directly to the active oscilloscope instrument without drawing messy wires.
 *
 * Supports both a rich hierarchical Tree View (Power components with branch I/V/losses,
 * Nets, Control signals, and Thermal/Magnetic channels) and a traditional Flat View.
 */
import { useState, useMemo, useEffect, useRef } from 'react';
import type { EditorComponent, EditorWire } from '../model/types';
import { inferSignalUnit } from './ScopeViewTab';
import type { ScopeDisplayLayout } from '../model/constants';
import { resolveSignalGraph, defaultGraphForSignal } from './scopes';
import { isGroundSignal } from './chartData';
import { extractAvailableSignalDetails, type AvailableSignalMetadata } from '../model/componentSchema';

/**
 * Extracts pure net label from single-ended or differential virtual voltage signal.
 *
 * @param signalName SPICE-style signal name (e.g. 'V(load_p)', 'V(dc_pos|dc_neg)', 'load_p').
 * @returns Clean net identifier suitable for schematic net highlighting.
 */
export function extractNetName(signalName: string): string {
  const match = signalName.match(/^V\(([^|)]+)/);
  if (match) return match[1].trim();
  return signalName;
}

export interface ScopeChannelManagerProps {
  /** Whether the channel manager modal is open. */
  isOpen: boolean;
  /** Callback to close the modal. */
  onClose: () => void;
  /** Name of the active scope instrument (e.g. 'SCOPE', 'SCOPE.1'). */
  scopeName: string;
  /** List of signals currently assigned as channels to this scope. */
  assignedChannels: string[];
  /** Complete list of all recorded signals from the circuit simulation. */
  allAvailableSignals: string[];
  /** Color resolver for signals. */
  colorOf: (signalName: string) => string;
  /** Callback to add a signal as a new channel, with optional target subplot graph index. */
  onAddChannel: (signalName: string, targetGraph?: number) => void;
  /** Callback to remove a signal / channel. */
  onRemoveChannel: (signalName: string) => void;
  /** Optional callback to auto-add all voltage and current measurement probes. */
  onAutoAddProbes?: () => void;
  /** Optional callback to clear all assigned channels for this scope. */
  onClearChannels?: () => void;
  /** Current display layout mode. */
  displayLayout?: ScopeDisplayLayout;
  /** Callback to change display layout mode. */
  onDisplayLayoutChange?: (layout: ScopeDisplayLayout) => void;
  /** Signal to graph mapping dictionary. */
  signalGraphs?: Record<string, number>;
  /** Callback when signal graph assignment changes. */
  onSetSignalGraph?: (signalName: string, graphIndex: number) => void;
  /** Callback to auto-group signals by category. */
  onAutoGroupByCategory?: () => void;
  /** Optional circuit components for rich hierarchical tree view and virtual signal discovery. */
  components?: EditorComponent[];
  /** Optional circuit wires for net discovery. */
  wires?: EditorWire[];
  /** Optional hover callback to highlight a schematic component. */
  onHighlightComponent?: (componentName: string | null) => void;
  /** Optional hover callback to highlight an electrical net. */
  onHighlightNet?: (netLabelOrId: string | null) => void;
}

type SignalCategory = 'all' | 'voltages' | 'currents' | 'control';
type ViewMode = 'tree' | 'flat';

export function ScopeChannelManager({
  isOpen,
  onClose,
  scopeName,
  assignedChannels,
  allAvailableSignals,
  colorOf,
  onAddChannel,
  onRemoveChannel,
  onAutoAddProbes,
  onClearChannels,
  displayLayout = 'overlay',
  onDisplayLayoutChange,
  signalGraphs = {},
  onSetSignalGraph,
  onAutoGroupByCategory,
  components,
  wires,
  onHighlightComponent,
  onHighlightNet,
}: ScopeChannelManagerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<ViewMode>('tree');
  const [activeCategory, setActiveCategory] = useState<SignalCategory>('all');
  const [targetSubplot, setTargetSubplot] = useState<'auto' | 1 | 2 | 3>('auto');
  const [collapsedSections, setCollapsedSections] = useState<Set<string>>(new Set());
  const [collapsedComponents, setCollapsedComponents] = useState<Set<string>>(new Set());

  const modalRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Focus search input when modal opens
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => searchInputRef.current?.focus(), 50);
    } else {
      setSearchQuery('');
    }
  }, [isOpen]);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  const isMultiGraph = displayLayout === '2-graphs' || displayLayout === '3-graphs';

  const toggleSection = (sectionId: string) => {
    setCollapsedSections((prev) => {
      const next = new Set(prev);
      if (next.has(sectionId)) next.delete(sectionId);
      else next.add(sectionId);
      return next;
    });
  };

  const toggleComponent = (compName: string) => {
    setCollapsedComponents((prev) => {
      const next = new Set(prev);
      if (next.has(compName)) next.delete(compName);
      else next.add(compName);
      return next;
    });
  };

  const handleAdd = (signalName: string, explicitGraph?: number) => {
    let g: number | undefined;
    if (explicitGraph !== undefined) {
      g = explicitGraph;
    } else if (isMultiGraph) {
      if (targetSubplot === 'auto') {
        g = defaultGraphForSignal(signalName, displayLayout);
      } else {
        g = targetSubplot;
      }
    }
    onAddChannel(signalName, g);
  };

  // Compile unified signal metadata from components, wires, and simulation results
  const allSignalsMetadata = useMemo(() => {
    const extracted = components ? extractAvailableSignalDetails(components, wires) : [];
    const metaMap = new Map<string, AvailableSignalMetadata>();

    for (const item of extracted) {
      metaMap.set(item.name, item);
    }

    const validAvailable = allAvailableSignals.filter((s) => !isGroundSignal(s));
    for (const sig of validAvailable) {
      if (!metaMap.has(sig)) {
        const unit = inferSignalUnit(sig);
        let domain: 'electrical' | 'control' | 'thermal' | 'other' = 'electrical';
        let category: 'voltage' | 'current' | 'control' | 'other' = 'other';
        let group: 'power' | 'nets' | 'control' | 'thermal_magnetic' = 'control';
        let compName: string | undefined;

        if (sig.startsWith('I(') && sig.endsWith(')')) {
          compName = sig.slice(2, -1);
          domain = 'electrical';
          category = 'current';
          group = 'power';
        } else if (sig.startsWith('V(') && sig.endsWith(')')) {
          const interior = sig.slice(2, -1);
          domain = 'electrical';
          category = 'voltage';
          if (components && components.some((c) => c.name === interior)) {
            compName = interior;
            group = 'power';
          } else {
            group = 'nets';
          }
        } else if (sig.startsWith('Ploss_')) {
          compName = sig.replace('Ploss_', '');
          domain = 'other';
          category = 'other';
          group = 'power';
        } else if (sig.startsWith('Tj_')) {
          compName = sig.replace('Tj_', '');
          domain = 'thermal';
          category = 'other';
          group = 'thermal_magnetic';
        } else if (sig.startsWith('Phi_')) {
          compName = sig.replace('Phi_', '');
          domain = 'electrical';
          category = 'other';
          group = 'thermal_magnetic';
        } else if (unit === 'V' || sig.toLowerCase().startsWith('v_') || sig.toLowerCase().startsWith('u_')) {
          domain = 'electrical';
          category = 'voltage';
          group = 'nets';
        } else if (unit === 'A' || sig.toLowerCase().startsWith('i_')) {
          domain = 'electrical';
          category = 'current';
          group = 'power';
        } else {
          domain = 'control';
          category = 'control';
          group = 'control';
        }

        metaMap.set(sig, {
          name: sig,
          domain,
          category,
          sourceDescription: compName ? `Signal for ${compName}` : `Signal "${sig}"`,
          componentName: compName,
          group,
        });
      }
    }

    return Array.from(metaMap.values());
  }, [components, wires, allAvailableSignals]);

  const q = searchQuery.trim().toLowerCase();

  const matchesSignal = (sig: AvailableSignalMetadata) => {
    if (!q) return true;
    return (
      sig.name.toLowerCase().includes(q) ||
      sig.sourceDescription.toLowerCase().includes(q) ||
      Boolean(sig.componentName?.toLowerCase().includes(q))
    );
  };

  const sortPowerSignals = (a: AvailableSignalMetadata, b: AvailableSignalMetadata) => {
    const aCurrent = a.category === 'current' || a.name.startsWith('I(');
    const bCurrent = b.category === 'current' || b.name.startsWith('I(');
    if (aCurrent && !bCurrent) return -1;
    if (!aCurrent && bCurrent) return 1;

    const aVolt = a.category === 'voltage' || a.name.startsWith('V(');
    const bVolt = b.category === 'voltage' || b.name.startsWith('V(');
    if (aVolt && !bVolt) return -1;
    if (!aVolt && bVolt) return 1;

    return a.name.localeCompare(b.name);
  };

  // Group into hierarchical tree categories
  const treeData = useMemo(() => {
    const powerMap = new Map<string, AvailableSignalMetadata[]>();
    const nets: AvailableSignalMetadata[] = [];
    const control: AvailableSignalMetadata[] = [];
    const thermal: AvailableSignalMetadata[] = [];

    for (const sig of allSignalsMetadata) {
      if (sig.group === 'power') {
        const compName = sig.componentName || 'Circuit Elements';
        if (!powerMap.has(compName)) {
          powerMap.set(compName, []);
        }
        powerMap.get(compName)!.push(sig);
      } else if (sig.group === 'nets') {
        nets.push(sig);
      } else if (sig.group === 'thermal_magnetic') {
        thermal.push(sig);
      } else {
        control.push(sig);
      }
    }

    // Filter by search query
    const powerGroups: Array<{ componentName: string; signals: AvailableSignalMetadata[] }> = [];
    for (const [compName, sigs] of powerMap.entries()) {
      const compMatches = !q || compName.toLowerCase().includes(q);
      const matchingSigs = sigs.filter(matchesSignal);
      if (compMatches || matchingSigs.length > 0) {
        powerGroups.push({
          componentName: compName,
          signals: (compMatches && matchingSigs.length === 0 ? sigs : matchingSigs).sort(sortPowerSignals),
        });
      }
    }
    powerGroups.sort((a, b) => a.componentName.localeCompare(b.componentName));

    const filteredNets = components
      ? nets.filter(matchesSignal).sort((a, b) => a.name.localeCompare(b.name))
      : nets.filter(matchesSignal);
    const filteredControl = components
      ? control.filter(matchesSignal).sort((a, b) => a.name.localeCompare(b.name))
      : control.filter(matchesSignal);
    const filteredThermal = components
      ? thermal.filter(matchesSignal).sort((a, b) => a.name.localeCompare(b.name))
      : thermal.filter(matchesSignal);

    return {
      powerGroups,
      nets: filteredNets,
      control: filteredControl,
      thermal: filteredThermal,
      totalSignalsCount:
        powerGroups.reduce((acc, g) => acc + g.signals.length, 0) +
        filteredNets.length +
        filteredControl.length +
        filteredThermal.length,
    };
  }, [allSignalsMetadata, q]);

  // Flat view list
  const filteredFlatSignals = useMemo(() => {
    let pool = allSignalsMetadata;
    if (activeCategory === 'voltages') {
      pool = allSignalsMetadata.filter(
        (s) =>
          s.category === 'voltage' ||
          s.name.startsWith('V(') ||
          s.name.toLowerCase().startsWith('u_') ||
          s.name.toLowerCase().startsWith('v_'),
      );
    } else if (activeCategory === 'currents') {
      pool = allSignalsMetadata.filter(
        (s) => s.category === 'current' || s.name.startsWith('I(') || s.name.toLowerCase().startsWith('i_'),
      );
    } else if (activeCategory === 'control') {
      pool = allSignalsMetadata.filter((s) => s.category === 'control' || s.group === 'control');
    }
    return pool.filter(matchesSignal).sort((a, b) => a.name.localeCompare(b.name));
  }, [allSignalsMetadata, activeCategory, q]);

  if (!isOpen) return null;

  const renderSignalRow = (sig: AvailableSignalMetadata, isNested: boolean = false) => {
    const isAssigned = assignedChannels.includes(sig.name);
    const channelIdx = assignedChannels.indexOf(sig.name);
    const color = colorOf(sig.name);
    const unit = inferSignalUnit(sig.name);
    const currentG = isAssigned ? resolveSignalGraph(sig.name, displayLayout, signalGraphs) : undefined;

    return (
      <div
        key={sig.name}
        data-testid={`scope-signal-row-${sig.name}`}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: isNested ? '5px 10px' : '6px 12px',
          borderRadius: '5px',
          background: isAssigned ? 'rgba(56, 189, 248, 0.08)' : 'var(--surface)',
          border: `1px solid ${isAssigned ? 'rgba(56, 189, 248, 0.4)' : 'var(--border)'}`,
          transition: 'all 0.15s ease',
          gap: 8,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, flex: 1 }}>
          <span
            style={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              backgroundColor: color,
              display: 'inline-block',
              boxShadow: `0 0 6px ${color}80`,
              flexShrink: 0,
            }}
          />
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, minWidth: 0, flexWrap: 'wrap' }}>
            <span
              style={{
                fontWeight: isAssigned ? 700 : 600,
                color: 'var(--text)',
                fontFamily: 'monospace',
                fontSize: '13px',
              }}
            >
              {sig.name}
            </span>
            {unit && (
              <span
                style={{
                  fontSize: '10px',
                  fontWeight: 600,
                  background: 'var(--panel)',
                  border: '1px solid var(--border)',
                  color: 'var(--text-muted)',
                  padding: '1px 5px',
                  borderRadius: '3px',
                }}
              >
                {unit}
              </span>
            )}
            {sig.sourceDescription && (
              <span
                style={{
                  fontSize: '11px',
                  color: 'var(--text-muted)',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
                title={sig.sourceDescription}
              >
                {sig.sourceDescription}
              </span>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
          {isAssigned ? (
            <>
              <span
                style={{
                  fontSize: '10px',
                  fontWeight: 700,
                  color,
                  border: `1px solid ${color}`,
                  padding: '2px 5px',
                  borderRadius: '3px',
                  background: 'var(--panel)',
                }}
              >
                CH{channelIdx + 1}
              </span>

              {isMultiGraph && (
                <div style={{ display: 'inline-flex', gap: 2 }}>
                  {[1, 2, ...(displayLayout === '3-graphs' ? [3] : [])].map((g) => {
                    const isSelected = currentG === g;
                    return (
                      <button
                        key={g}
                        type="button"
                        onClick={() => onSetSignalGraph?.(sig.name, g)}
                        title={`Assign ${sig.name} to Graph ${g}`}
                        style={{
                          border: isSelected ? '1px solid var(--accent, #38bdf8)' : '1px solid var(--border)',
                          background: isSelected ? 'var(--accent, #38bdf8)' : 'transparent',
                          color: isSelected ? '#ffffff' : 'var(--text-muted)',
                          borderRadius: '3px',
                          fontSize: '9px',
                          padding: '1px 4px',
                          cursor: 'pointer',
                          fontWeight: isSelected ? 700 : 500,
                        }}
                      >
                        G{g}
                      </button>
                    );
                  })}
                </div>
              )}

              <button
                type="button"
                className="sim-mini-btn"
                onClick={() => onRemoveChannel(sig.name)}
                style={{ color: 'var(--danger, #f43f5e)', borderColor: 'rgba(244, 63, 94, 0.3)' }}
              >
                Remove
              </button>
            </>
          ) : (
            <>
              {isMultiGraph && (
                <div style={{ display: 'inline-flex', gap: 2 }}>
                  {[1, 2, ...(displayLayout === '3-graphs' ? [3] : [])].map((g) => (
                    <button
                      key={g}
                      type="button"
                      className="sim-mini-btn"
                      onClick={() => handleAdd(sig.name, g)}
                      title={`Add directly to Graph ${g}`}
                      style={{ fontSize: '10px', padding: '2px 5px' }}
                    >
                      + G{g}
                    </button>
                  ))}
                </div>
              )}
              <button
                type="button"
                className="sim-mini-btn"
                onClick={() => handleAdd(sig.name)}
                style={{ color: 'var(--accent, #38bdf8)', borderColor: 'rgba(56, 189, 248, 0.3)' }}
              >
                + Add to Scope
              </button>
            </>
          )}
        </div>
      </div>
    );
  };

  return (
    <div
      className="modal-backdrop"
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        background: 'rgba(0, 0, 0, 0.65)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backdropFilter: 'blur(3px)',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      data-testid="scope-channel-manager-modal"
    >
      <div
        ref={modalRef}
        className="modal-card"
        style={{
          width: '92%',
          maxWidth: '740px',
          maxHeight: '88vh',
          display: 'flex',
          flexDirection: 'column',
          background: 'var(--panel)',
          border: '1px solid var(--border)',
          borderRadius: '8px',
          boxShadow: '0 12px 36px rgba(0,0,0,0.4)',
          overflow: 'hidden',
        }}
      >
        {/* Modal Header */}
        <div
          className="modal-header"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '14px 18px',
            borderBottom: '1px solid var(--border)',
            background: 'var(--panel-header, var(--panel))',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: '20px' }}>📺</span>
            <div>
              <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text)' }}>
                Scope Channel Manager: {scopeName}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                Browse and bind component currents, voltages, net potentials, and control signals directly to the oscilloscope.
              </div>
            </div>
          </div>
          <button
            type="button"
            className="action-icon-btn"
            onClick={onClose}
            title="Close (Esc)"
            style={{ fontSize: '16px', padding: '4px 8px' }}
          >
            ✕
          </button>
        </div>

        {/* Modal Body */}
        <div
          className="modal-body"
          style={{
            padding: '16px 18px',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: 12,
          }}
        >
          {/* Display Layout & Auto-Group Strip */}
          <div
            style={{
              padding: '10px 14px',
              background: 'var(--surface)',
              borderRadius: '6px',
              border: '1px solid var(--border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 12,
              flexWrap: 'wrap',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text)' }}>
                Display Layout:
              </span>
              {onDisplayLayoutChange && (
                <div className="segmented-control" style={{ display: 'flex' }}>
                  <button
                    type="button"
                    className={`segmented-btn ${displayLayout === 'overlay' ? 'active' : ''}`}
                    onClick={() => onDisplayLayoutChange('overlay')}
                    title="Overlay all channels on 1 combined graph"
                  >
                    📈 1 Graph
                  </button>
                  <button
                    type="button"
                    className={`segmented-btn ${displayLayout === '2-graphs' ? 'active' : ''}`}
                    onClick={() => onDisplayLayoutChange('2-graphs')}
                    title="Split into 2 subplots (e.g. Voltages / Currents)"
                  >
                    📊 2 Graphs
                  </button>
                  <button
                    type="button"
                    className={`segmented-btn ${displayLayout === '3-graphs' ? 'active' : ''}`}
                    onClick={() => onDisplayLayoutChange('3-graphs')}
                    title="Split into 3 subplots (e.g. Voltages / Currents / Control)"
                  >
                    📑 3 Graphs
                  </button>
                  <button
                    type="button"
                    className={`segmented-btn ${displayLayout === 'stacked' ? 'active' : ''}`}
                    onClick={() => onDisplayLayoutChange('stacked')}
                    title="Stack each channel in its own individual lane"
                  >
                    🪜 Stacked
                  </button>
                </div>
              )}
            </div>

            {isMultiGraph && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                {onAutoGroupByCategory && (
                  <button
                    type="button"
                    className="sim-mini-btn"
                    onClick={onAutoGroupByCategory}
                    title="Automatically organize signals: Voltages -> Graph 1, Currents -> Graph 2, Control -> Graph 3"
                    style={{ fontSize: '11px', display: 'flex', alignItems: 'center', gap: 4 }}
                  >
                    <span>⚡</span> Auto-Group (V / I / Ctrl)
                  </button>
                )}

                <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)', fontWeight: 600 }}>
                    Default Subplot:
                  </span>
                  <div className="segmented-control" style={{ display: 'flex' }}>
                    <button
                      type="button"
                      className={`segmented-btn ${targetSubplot === 'auto' ? 'active' : ''}`}
                      onClick={() => setTargetSubplot('auto')}
                      title="Auto: Voltages to Graph 1, Currents to Graph 2, Control to Graph 3"
                      style={{ fontSize: '10px', padding: '2px 6px' }}
                    >
                      Auto
                    </button>
                    <button
                      type="button"
                      className={`segmented-btn ${targetSubplot === 1 ? 'active' : ''}`}
                      onClick={() => setTargetSubplot(1)}
                      title="Route added signals to Graph 1"
                      style={{ fontSize: '10px', padding: '2px 6px' }}
                    >
                      Graph 1
                    </button>
                    <button
                      type="button"
                      className={`segmented-btn ${targetSubplot === 2 ? 'active' : ''}`}
                      onClick={() => setTargetSubplot(2)}
                      title="Route added signals to Graph 2"
                      style={{ fontSize: '10px', padding: '2px 6px' }}
                    >
                      Graph 2
                    </button>
                    {displayLayout === '3-graphs' && (
                      <button
                        type="button"
                        className={`segmented-btn ${targetSubplot === 3 ? 'active' : ''}`}
                        onClick={() => setTargetSubplot(3)}
                        title="Route added signals to Graph 3"
                        style={{ fontSize: '10px', padding: '2px 6px' }}
                      >
                        Graph 3
                      </button>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Active Channels Strip */}
          <div
            style={{
              padding: '10px 14px',
              background: 'var(--surface)',
              borderRadius: '6px',
              border: '1px solid var(--border)',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 8,
              }}
            >
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text)' }}>
                Active Channels ({assignedChannels.length})
              </span>
              <div style={{ display: 'flex', gap: 8 }}>
                {onAutoAddProbes && (
                  <button
                    type="button"
                    className="sim-mini-btn"
                    onClick={onAutoAddProbes}
                    title="Automatically add all measurement probes (voltmeters, ammeters) from the circuit"
                    style={{ fontSize: '11px', display: 'flex', alignItems: 'center', gap: 4 }}
                  >
                    <span>⚡</span> Auto-Add Probes
                  </button>
                )}
                {onClearChannels && assignedChannels.length > 0 && (
                  <button
                    type="button"
                    className="sim-mini-btn"
                    onClick={onClearChannels}
                    title="Remove all channels from this scope"
                    style={{ fontSize: '11px', color: 'var(--danger, #f43f5e)' }}
                  >
                    Clear All
                  </button>
                )}
              </div>
            </div>

            {assignedChannels.length === 0 ? (
              <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                No channels currently assigned. Select signals below or click "Auto-Add Probes".
              </div>
            ) : (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {assignedChannels.map((sig, idx) => {
                  const color = colorOf(sig);
                  const unit = inferSignalUnit(sig);
                  return (
                    <div
                      key={sig}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '3px 8px',
                        borderRadius: '4px',
                        background: 'var(--panel)',
                        border: `1px solid ${color}`,
                        fontSize: '12px',
                      }}
                    >
                      <span
                        style={{
                          fontSize: '10px',
                          fontWeight: 700,
                          color,
                        }}
                      >
                        CH{idx + 1}
                      </span>
                      <strong style={{ color: 'var(--text)', fontFamily: 'monospace' }}>{sig}</strong>
                      {unit && (
                        <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>
                          [{unit}]
                        </span>
                      )}
                      {isMultiGraph && (
                        <div style={{ display: 'inline-flex', gap: 2, marginLeft: 2 }}>
                          {[1, 2, ...(displayLayout === '3-graphs' ? [3] : [])].map((g) => {
                            const currentG = resolveSignalGraph(sig, displayLayout, signalGraphs);
                            const isSelected = currentG === g;
                            return (
                              <button
                                key={g}
                                type="button"
                                onClick={() => onSetSignalGraph?.(sig, g)}
                                title={`Move ${sig} to Graph ${g}`}
                                style={{
                                  border: isSelected ? '1px solid var(--accent, #38bdf8)' : '1px solid var(--border)',
                                  background: isSelected ? 'var(--accent, #38bdf8)' : 'transparent',
                                  color: isSelected ? '#ffffff' : 'var(--text-muted)',
                                  borderRadius: '3px',
                                  fontSize: '10px',
                                  padding: '1px 4px',
                                  cursor: 'pointer',
                                  fontWeight: isSelected ? 700 : 500,
                                }}
                              >
                                G{g}
                              </button>
                            );
                          })}
                        </div>
                      )}
                      <button
                        type="button"
                        onClick={() => onRemoveChannel(sig)}
                        title={`Remove ${sig} from scope`}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: 'var(--text-muted)',
                          cursor: 'pointer',
                          padding: '0 2px',
                          fontSize: '12px',
                          lineHeight: 1,
                        }}
                      >
                        ✕
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Search, View Mode Toggle, and Filters */}
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <input
              ref={searchInputRef}
              type="text"
              className="prop-input"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Filter signals (e.g. u_out, duty, i_L)..."
              style={{ flex: '1 1 240px' }}
            />

            {/* View Mode Toggle: Tree View vs Flat View */}
            <div className="segmented-control" style={{ display: 'flex' }}>
              <button
                type="button"
                className={`segmented-btn ${viewMode === 'tree' ? 'active' : ''}`}
                onClick={() => setViewMode('tree')}
                title="Hierarchical Tree View: Grouped by Power components, Nets, Control, and Thermal"
                data-testid="scope-picker-tree-view-btn"
              >
                🌳 Tree View
              </button>
              <button
                type="button"
                className={`segmented-btn ${viewMode === 'flat' ? 'active' : ''}`}
                onClick={() => setViewMode('flat')}
                title="Flat List View: Filterable list with category tabs"
                data-testid="scope-picker-flat-view-btn"
              >
                📋 Flat View
              </button>
            </div>

            {/* Flat View Category Pills */}
            {viewMode === 'flat' && (
              <div className="segmented-control" style={{ display: 'flex' }}>
                <button
                  type="button"
                  className={`segmented-btn ${activeCategory === 'all' ? 'active' : ''}`}
                  onClick={() => setActiveCategory('all')}
                >
                  All ({allSignalsMetadata.length})
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${activeCategory === 'voltages' ? 'active' : ''}`}
                  onClick={() => setActiveCategory('voltages')}
                >
                  ⚡ Voltages
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${activeCategory === 'currents' ? 'active' : ''}`}
                  onClick={() => setActiveCategory('currents')}
                >
                  🔌 Currents
                </button>
                <button
                  type="button"
                  className={`segmented-btn ${activeCategory === 'control' ? 'active' : ''}`}
                  onClick={() => setActiveCategory('control')}
                >
                  🎛️ Control
                </button>
              </div>
            )}
          </div>

          {/* Main Signal Picker Area */}
          <div
            style={{
              maxHeight: '380px',
              minHeight: '220px',
              overflowY: 'auto',
              border: '1px solid var(--border)',
              borderRadius: '6px',
              padding: '8px',
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
              background: 'var(--panel)',
            }}
            data-testid="scope-signals-container"
          >
            {viewMode === 'tree' ? (
              // HIERARCHICAL TREE VIEW MODE
              treeData.totalSignalsCount === 0 ? (
                <div style={{ padding: '28px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  No circuit signals match "{searchQuery}"
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {/* 1. Power Components Section */}
                  {treeData.powerGroups.length > 0 && (
                    <div
                      style={{
                        border: '1px solid var(--border)',
                        borderRadius: '6px',
                        overflow: 'hidden',
                        background: 'var(--surface)',
                      }}
                      data-testid="scope-tree-power-section"
                    >
                      <div
                        onClick={() => toggleSection('power')}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '8px 12px',
                          cursor: 'pointer',
                          background: 'rgba(56, 189, 248, 0.05)',
                          userSelect: 'none',
                          borderBottom: collapsedSections.has('power') ? 'none' : '1px solid var(--border)',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                            {collapsedSections.has('power') ? '▶' : '▼'}
                          </span>
                          <span style={{ fontSize: '14px' }}>⚡</span>
                          <strong style={{ fontSize: '13px', color: 'var(--text)' }}>
                            Power Components
                          </strong>
                        </div>
                        <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                          {treeData.powerGroups.length} component{treeData.powerGroups.length > 1 ? 's' : ''} (
                          {treeData.powerGroups.reduce((acc, g) => acc + g.signals.length, 0)} signals)
                        </span>
                      </div>

                      {!collapsedSections.has('power') && (
                        <div style={{ padding: '8px', display: 'flex', flexDirection: 'column', gap: 6 }}>
                          {treeData.powerGroups.map((group) => {
                            const isCompCollapsed = !q && collapsedComponents.has(group.componentName);
                            return (
                              <div
                                key={group.componentName}
                                style={{
                                  border: '1px solid var(--border)',
                                  borderRadius: '5px',
                                  background: 'var(--panel)',
                                  overflow: 'hidden',
                                }}
                                onMouseEnter={() => onHighlightComponent?.(group.componentName)}
                                onMouseLeave={() => onHighlightComponent?.(null)}
                                data-testid={`scope-tree-comp-${group.componentName}`}
                              >
                                <div
                                  onClick={() => toggleComponent(group.componentName)}
                                  style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    padding: '6px 10px',
                                    cursor: 'pointer',
                                    background: 'var(--surface)',
                                    userSelect: 'none',
                                    borderBottom: isCompCollapsed ? 'none' : '1px solid var(--border)',
                                  }}
                                >
                                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                    <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>
                                      {isCompCollapsed ? '▶' : '▼'}
                                    </span>
                                    <span style={{ fontSize: '12px' }}>📦</span>
                                    <strong style={{ fontSize: '12px', color: 'var(--text)' }}>
                                      {group.componentName}
                                    </strong>
                                  </div>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                                      {group.signals.length} signal{group.signals.length > 1 ? 's' : ''}
                                    </span>
                                    <button
                                      type="button"
                                      className="sim-mini-btn"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        group.signals.forEach((s) => {
                                          if (!assignedChannels.includes(s.name)) {
                                            handleAdd(s.name);
                                          }
                                        });
                                      }}
                                      title={`Add all signals for ${group.componentName} to scope`}
                                      style={{ fontSize: '10px', padding: '1px 5px' }}
                                    >
                                      + Add all
                                    </button>
                                  </div>
                                </div>

                                {!isCompCollapsed && (
                                  <div style={{ padding: '6px', display: 'flex', flexDirection: 'column', gap: 4 }}>
                                    {group.signals.map((sig) => renderSignalRow(sig, true))}
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  )}

                  {/* 2. Nets & Circuit Nodes Section */}
                  {treeData.nets.length > 0 && (
                    <div
                      style={{
                        border: '1px solid var(--border)',
                        borderRadius: '6px',
                        overflow: 'hidden',
                        background: 'var(--surface)',
                      }}
                      data-testid="scope-tree-nets-section"
                    >
                      <div
                        onClick={() => toggleSection('nets')}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '8px 12px',
                          cursor: 'pointer',
                          background: 'rgba(34, 197, 94, 0.05)',
                          userSelect: 'none',
                          borderBottom: collapsedSections.has('nets') ? 'none' : '1px solid var(--border)',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                            {collapsedSections.has('nets') ? '▶' : '▼'}
                          </span>
                          <span style={{ fontSize: '14px' }}>🌐</span>
                          <strong style={{ fontSize: '13px', color: 'var(--text)' }}>
                            Nets & Circuit Nodes
                          </strong>
                        </div>
                        <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                          {treeData.nets.length} signal{treeData.nets.length > 1 ? 's' : ''}
                        </span>
                      </div>

                      {!collapsedSections.has('nets') && (
                        <div style={{ padding: '8px', display: 'flex', flexDirection: 'column', gap: 4 }}>
                          {treeData.nets.map((sig) => (
                            <div
                              key={sig.name}
                              onMouseEnter={() => onHighlightNet?.(extractNetName(sig.name))}
                              onMouseLeave={() => onHighlightNet?.(null)}
                            >
                              {renderSignalRow(sig)}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* 3. Control Signals Section */}
                  {treeData.control.length > 0 && (
                    <div
                      style={{
                        border: '1px solid var(--border)',
                        borderRadius: '6px',
                        overflow: 'hidden',
                        background: 'var(--surface)',
                      }}
                      data-testid="scope-tree-control-section"
                    >
                      <div
                        onClick={() => toggleSection('control')}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '8px 12px',
                          cursor: 'pointer',
                          background: 'rgba(168, 85, 247, 0.05)',
                          userSelect: 'none',
                          borderBottom: collapsedSections.has('control') ? 'none' : '1px solid var(--border)',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                            {collapsedSections.has('control') ? '▶' : '▼'}
                          </span>
                          <span style={{ fontSize: '14px' }}>🎛️</span>
                          <strong style={{ fontSize: '13px', color: 'var(--text)' }}>
                            Control Signals
                          </strong>
                        </div>
                        <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                          {treeData.control.length} signal{treeData.control.length > 1 ? 's' : ''}
                        </span>
                      </div>

                      {!collapsedSections.has('control') && (
                        <div style={{ padding: '8px', display: 'flex', flexDirection: 'column', gap: 4 }}>
                          {treeData.control.map((sig) => (
                            <div
                              key={sig.name}
                              onMouseEnter={() => sig.componentName && onHighlightComponent?.(sig.componentName)}
                              onMouseLeave={() => sig.componentName && onHighlightComponent?.(null)}
                            >
                              {renderSignalRow(sig)}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* 4. Thermal & Magnetic Section */}
                  {treeData.thermal.length > 0 && (
                    <div
                      style={{
                        border: '1px solid var(--border)',
                        borderRadius: '6px',
                        overflow: 'hidden',
                        background: 'var(--surface)',
                      }}
                      data-testid="scope-tree-thermal-section"
                    >
                      <div
                        onClick={() => toggleSection('thermal_magnetic')}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '8px 12px',
                          cursor: 'pointer',
                          background: 'rgba(249, 115, 22, 0.05)',
                          userSelect: 'none',
                          borderBottom: collapsedSections.has('thermal_magnetic') ? 'none' : '1px solid var(--border)',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                            {collapsedSections.has('thermal_magnetic') ? '▶' : '▼'}
                          </span>
                          <span style={{ fontSize: '14px' }}>🌡️</span>
                          <strong style={{ fontSize: '13px', color: 'var(--text)' }}>
                            Thermal & Magnetic
                          </strong>
                        </div>
                        <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                          {treeData.thermal.length} signal{treeData.thermal.length > 1 ? 's' : ''}
                        </span>
                      </div>

                      {!collapsedSections.has('thermal_magnetic') && (
                        <div style={{ padding: '8px', display: 'flex', flexDirection: 'column', gap: 4 }}>
                          {treeData.thermal.map((sig) => (
                            <div
                              key={sig.name}
                              onMouseEnter={() => sig.componentName && onHighlightComponent?.(sig.componentName)}
                              onMouseLeave={() => sig.componentName && onHighlightComponent?.(null)}
                            >
                              {renderSignalRow(sig)}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            ) : (
              // FLAT LIST VIEW MODE
              filteredFlatSignals.length === 0 ? (
                <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  No circuit signals match "{searchQuery}"
                </div>
              ) : (
                filteredFlatSignals.map((sig) => renderSignalRow(sig))
              )
            )}
          </div>
        </div>

        {/* Modal Footer */}
        <div
          className="modal-footer"
          style={{
            padding: '12px 18px',
            borderTop: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 10,
            background: 'var(--panel-header, var(--panel))',
          }}
        >
          <div style={{ fontSize: '11px', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <span>💡</span>
            <span>Signals are resolved by simulation engine without needing physical meters on schematic canvas.</span>
          </div>

          <button type="button" className="modal-btn primary" onClick={onClose}>
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
