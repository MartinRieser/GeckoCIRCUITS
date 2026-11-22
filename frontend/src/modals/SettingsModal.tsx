/**
 * Categorized Application Settings modal (General, Schematic, Oscilloscope, Features & Presets).
 * Allows turning individual advanced features on or off and switching workspace presets.
 */
import { useEffect, useState } from 'react';
import {
  PRESET_DEFINITIONS,
  applyWorkspacePreset,
  detectActivePreset,
  type AppSettings,
  type WorkspacePresetId,
} from '../model/appSettings';
import type { ScopeDisplayLayout } from '../model/constants';

export interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: AppSettings;
  onUpdateSettings: (next: AppSettings) => void;
  onResetLayout?: () => void;
}

type SettingsCategory = 'general' | 'schematic' | 'scope' | 'features';

const CATEGORIES: { id: SettingsCategory; label: string; icon: string }[] = [
  { id: 'general', label: 'General & Workspace', icon: '🖥' },
  { id: 'schematic', label: 'Schematic Editor', icon: '📐' },
  { id: 'scope', label: 'Oscilloscope & Signals', icon: '📈' },
  { id: 'features', label: 'Features & Tools', icon: '🧩' },
];

const FEATURE_ITEMS: {
  key: keyof AppSettings['features'];
  title: string;
  description: string;
}[] = [
  {
    key: 'referenceCompare',
    title: 'Before / After Reference Compare',
    description: 'Save current waveforms as dashed reference curves and inspect Δ differences.',
  },
  {
    key: 'parametricSweep',
    title: 'Parametric Sweep',
    description: 'Run a component parameter across a value range and step through all channels.',
  },
  {
    key: 'liveSliders',
    title: 'Live Parameter Sliders',
    description: 'Pin component values as interactive sliders for quick tuning.',
  },
  {
    key: 'fftAnalysis',
    title: 'FFT Frequency Spectrum',
    description: 'Show the harmonic FFT Spectrum tab in the Oscilloscope view.',
  },
  {
    key: 'lossAnalysis',
    title: 'Power & Semiconductor Losses',
    description: 'Show instantaneous power and switching/conduction loss calculations in Scope.',
  },
  {
    key: 'snippets',
    title: 'Circuit Templates (Snippets)',
    description: 'Enable reusable subcircuit blocks in the Palette, Context Menu & Ctrl+K.',
  },
];

export function SettingsModal({
  isOpen,
  onClose,
  settings,
  onUpdateSettings,
  onResetLayout,
}: SettingsModalProps) {
  const [category, setCategory] = useState<SettingsCategory>('features');

  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const activePreset = detectActivePreset(settings);

  const patchWorkspace = (patch: Partial<AppSettings['workspace']>) =>
    onUpdateSettings({ ...settings, workspace: { ...settings.workspace, ...patch } });
  const patchSchematic = (patch: Partial<AppSettings['schematic']>) =>
    onUpdateSettings({ ...settings, schematic: { ...settings.schematic, ...patch } });
  const patchScope = (patch: Partial<AppSettings['scope']>) =>
    onUpdateSettings({ ...settings, scope: { ...settings.scope, ...patch } });
  const toggleFeature = (key: keyof AppSettings['features']) =>
    onUpdateSettings({
      ...settings,
      features: { ...settings.features, [key]: !settings.features[key] },
    });

  return (
    <div
      className="modal-backdrop"
      data-testid="settings-modal-backdrop"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="settings-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Settings"
        data-testid="settings-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="settings-modal-header">
          <div>
            <div className="settings-modal-title">⚙ Settings &amp; Workspace</div>
            <div className="settings-modal-subtitle">
              Customize appearance, signal filtering, and which engineering tools appear in your UI.
            </div>
          </div>
          <button
            type="button"
            className="shortcuts-modal-close"
            data-testid="settings-close-btn"
            onClick={onClose}
            aria-label="Close Settings"
          >
            ✕
          </button>
        </div>

        <div className="settings-modal-body">
          <nav className="settings-nav" aria-label="Settings categories">
            {CATEGORIES.map((cat) => (
              <button
                key={cat.id}
                type="button"
                data-testid={`settings-cat-${cat.id}`}
                className={`settings-nav-btn ${category === cat.id ? 'active' : ''}`}
                onClick={() => setCategory(cat.id)}
              >
                <span>{cat.icon}</span>
                <span>{cat.label}</span>
              </button>
            ))}
          </nav>

          <div className="settings-content">
            {category === 'general' && (
              <div className="settings-section" data-testid="settings-panel-general">
                <h3>General &amp; Workspace</h3>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Color Theme</strong>
                    <span>Switch between Dark and Light workbench themes.</span>
                  </div>
                  <select
                    data-testid="setting-theme-select"
                    value={settings.workspace.theme}
                    onChange={(e) =>
                      patchWorkspace({ theme: e.target.value as 'dark' | 'light' })
                    }
                  >
                    <option value="dark">Dark</option>
                    <option value="light">Light</option>
                  </select>
                </label>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Auto-open Waveforms after Run (F5)</strong>
                    <span>Automatically reveal the bottom Waveforms panel when a simulation finishes.</span>
                  </div>
                  <input
                    type="checkbox"
                    data-testid="setting-auto-open-waveforms"
                    checked={settings.workspace.autoOpenWaveformsOnRun}
                    onChange={(e) =>
                      patchWorkspace({ autoOpenWaveformsOnRun: e.target.checked })
                    }
                  />
                </label>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Default Scope Graph Layout</strong>
                    <span>Initial plot arrangement when opening a new circuit.</span>
                  </div>
                  <select
                    data-testid="setting-default-layout"
                    value={settings.workspace.defaultScopeLayout}
                    onChange={(e) =>
                      patchWorkspace({
                        defaultScopeLayout: e.target.value as ScopeDisplayLayout,
                      })
                    }
                  >
                    <option value="stacked">Stacked Lanes</option>
                    <option value="overlay">1 Graph (Overlay)</option>
                    <option value="2-graphs">2 Graphs</option>
                    <option value="3-graphs">3 Graphs</option>
                  </select>
                </label>

                {onResetLayout && (
                  <div className="settings-row">
                    <div className="settings-row-info">
                      <strong>Reset Window &amp; Panel Layout</strong>
                      <span>Restore default sidebar widths, drawer height, and floating bar position.</span>
                    </div>
                    <button
                      type="button"
                      className="sim-mini-btn"
                      data-testid="setting-reset-layout-btn"
                      onClick={onResetLayout}
                    >
                      Reset Layout
                    </button>
                  </div>
                )}
              </div>
            )}

            {category === 'schematic' && (
              <div className="settings-section" data-testid="settings-panel-schematic">
                <h3>Schematic Editor</h3>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Show Alignment Grid</strong>
                    <span>Display snap grid dots on the schematic canvas (`#`).</span>
                  </div>
                  <input
                    type="checkbox"
                    data-testid="setting-show-grid"
                    checked={settings.schematic.showGrid}
                    onChange={(e) => patchSchematic({ showGrid: e.target.checked })}
                  />
                </label>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Net Label Display</strong>
                    <span>Choose when electrical net names appear on wires.</span>
                  </div>
                  <select
                    data-testid="setting-net-label-mode"
                    value={settings.schematic.netLabelMode}
                    onChange={(e) =>
                      patchSchematic({
                        netLabelMode: e.target.value as 'smart' | 'all' | 'hover',
                      })
                    }
                  >
                    <option value="smart">Smart (Named &amp; Probed)</option>
                    <option value="all">All Nets</option>
                    <option value="hover">On Hover Only</option>
                  </select>
                </label>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Show Oscilloscope Probe Badges</strong>
                    <span>Display V/I probe markers directly on probed components and wires.</span>
                  </div>
                  <input
                    type="checkbox"
                    data-testid="setting-show-probes"
                    checked={settings.schematic.showProbeMarkers}
                    onChange={(e) => patchSchematic({ showProbeMarkers: e.target.checked })}
                  />
                </label>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Operating-Point Values on Schematic (Optional)</strong>
                    <span>
                      Show simulated average voltage/current boxes directly on parts and wires.
                      Off by default to keep schematics clean.
                    </span>
                  </div>
                  <input
                    type="checkbox"
                    data-testid="setting-show-op-badges"
                    checked={settings.schematic.showOperatingPointBadges}
                    onChange={(e) =>
                      patchSchematic({ showOperatingPointBadges: e.target.checked })
                    }
                  />
                </label>
              </div>
            )}

            {category === 'scope' && (
              <div className="settings-section" data-testid="settings-panel-scope">
                <h3>Oscilloscope &amp; Signal Picker</h3>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Clean Signal List (Recommended)</strong>
                    <span>
                      Hide duplicate wire aliases (e.g. bare net name alongside <code>V(net)</code>),
                      Ground (<code>0</code>), and unrecorded <code>Ploss</code>/<code>Tj</code>/<code>Phi</code> signals.
                    </span>
                  </div>
                  <input
                    type="checkbox"
                    data-testid="setting-clean-signals"
                    checked={settings.scope.hideDuplicateAndSpeculativeSignals}
                    onChange={(e) =>
                      patchScope({ hideDuplicateAndSpeculativeSignals: e.target.checked })
                    }
                  />
                </label>

                <label className="settings-row">
                  <div className="settings-row-info">
                    <strong>Show Quick Measurement Strip</strong>
                    <span>Display Vpp / RMS / Mean tiles directly beneath waveform plots.</span>
                  </div>
                  <input
                    type="checkbox"
                    data-testid="setting-quick-measure"
                    checked={settings.scope.showQuickMeasureStrip}
                    onChange={(e) => patchScope({ showQuickMeasureStrip: e.target.checked })}
                  />
                </label>
              </div>
            )}

            {category === 'features' && (
              <div className="settings-section" data-testid="settings-panel-features">
                <h3>Workspace Presets &amp; Feature Toggles</h3>

                <div className="settings-presets-row" role="group" aria-label="Workspace presets">
                  {(
                    Object.entries(PRESET_DEFINITIONS) as [
                      WorkspacePresetId,
                      (typeof PRESET_DEFINITIONS)[WorkspacePresetId],
                    ][]
                  ).map(([id, def]) => (
                    <button
                      key={id}
                      type="button"
                      data-testid={`settings-preset-${id}`}
                      className={`settings-preset-card ${activePreset === id ? 'active' : ''}`}
                      onClick={() => onUpdateSettings(applyWorkspacePreset(settings, id))}
                    >
                      <strong>{def.label}</strong>
                      <span>{def.description}</span>
                    </button>
                  ))}
                </div>

                <div className="settings-feature-list">
                  {FEATURE_ITEMS.map((item) => (
                    <label key={item.key} className="settings-row">
                      <div className="settings-row-info">
                        <strong>{item.title}</strong>
                        <span>{item.description}</span>
                      </div>
                      <input
                        type="checkbox"
                        data-testid={`setting-feature-${item.key}`}
                        checked={settings.features[item.key]}
                        onChange={() => toggleFeature(item.key)}
                      />
                    </label>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
