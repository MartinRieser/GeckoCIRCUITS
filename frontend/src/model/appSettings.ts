/**
 * Centralized application preferences, workspace presets (Simple / Standard / Power User),
 * and legacy localStorage migration.
 */
import type { ScopeDisplayLayout } from '../model/constants';

export type WorkspacePresetId = 'simple' | 'standard' | 'power-user';

export interface AppSettings {
  workspace: {
    theme: 'dark' | 'light';
    autoOpenWaveformsOnRun: boolean;
    defaultScopeLayout: ScopeDisplayLayout;
  };
  schematic: {
    showGrid: boolean;
    netLabelMode: 'smart' | 'all' | 'hover';
    showProbeMarkers: boolean;
    showOperatingPointBadges: boolean;
  };
  scope: {
    hideDuplicateAndSpeculativeSignals: boolean;
    showQuickMeasureStrip: boolean;
  };
  features: {
    referenceCompare: boolean;
    parametricSweep: boolean;
    liveSliders: boolean;
    fftAnalysis: boolean;
    lossAnalysis: boolean;
    snippets: boolean;
  };
}

export const APP_SETTINGS_STORAGE_KEY = 'gecko.app.settings.v1';

export const PRESET_DEFINITIONS: Record<
  WorkspacePresetId,
  {
    label: string;
    description: string;
    features: AppSettings['features'];
  }
> = {
  simple: {
    label: '🎯 Simple',
    description: 'Clean schematic, Run (F5), Waveforms, and Metrics only.',
    features: {
      referenceCompare: false,
      parametricSweep: false,
      liveSliders: false,
      fftAnalysis: false,
      lossAnalysis: false,
      snippets: false,
    },
  },
  standard: {
    label: '⚡ Standard',
    description: 'Waveforms, Metrics, FFT, Reference Compare, Sweep, Sliders & Templates.',
    features: {
      referenceCompare: true,
      parametricSweep: true,
      liveSliders: true,
      fftAnalysis: true,
      lossAnalysis: false,
      snippets: true,
    },
  },
  'power-user': {
    label: '🔬 Power User',
    description: 'All analysis tools enabled including Power & Semiconductor Losses.',
    features: {
      referenceCompare: true,
      parametricSweep: true,
      liveSliders: true,
      fftAnalysis: true,
      lossAnalysis: true,
      snippets: true,
    },
  },
};

export const DEFAULT_APP_SETTINGS: AppSettings = {
  workspace: {
    theme: 'dark',
    autoOpenWaveformsOnRun: true,
    defaultScopeLayout: 'stacked',
  },
  schematic: {
    showGrid: true,
    netLabelMode: 'smart',
    showProbeMarkers: true,
    showOperatingPointBadges: false,
  },
  scope: {
    hideDuplicateAndSpeculativeSignals: true,
    showQuickMeasureStrip: true,
  },
  features: { ...PRESET_DEFINITIONS.standard.features },
};

function readLegacyString(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

/**
 * Loads settings from `localStorage`, migrating any legacy standalone keys on first run.
 */
export function loadAppSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(APP_SETTINGS_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<AppSettings>;
      return {
        workspace: { ...DEFAULT_APP_SETTINGS.workspace, ...parsed.workspace },
        schematic: { ...DEFAULT_APP_SETTINGS.schematic, ...parsed.schematic },
        scope: { ...DEFAULT_APP_SETTINGS.scope, ...parsed.scope },
        features: { ...DEFAULT_APP_SETTINGS.features, ...parsed.features },
      };
    }
  } catch {
    // ignore malformed storage and fall back to defaults/migration
  }

  const legacyTheme = readLegacyString('gecko-theme');
  const legacyLayout = readLegacyString('gecko-display-layout');
  const legacyLabels = readLegacyString('gecko_label_mode');
  const legacyProbes = readLegacyString('gecko_show_probes');

  return {
    workspace: {
      theme: legacyTheme === 'light' ? 'light' : 'dark',
      autoOpenWaveformsOnRun: true,
      defaultScopeLayout:
        legacyLayout === 'overlay' ||
        legacyLayout === '2-graphs' ||
        legacyLayout === '3-graphs' ||
        legacyLayout === 'stacked'
          ? legacyLayout
          : 'stacked',
    },
    schematic: {
      showGrid: true,
      netLabelMode:
        legacyLabels === 'all' || legacyLabels === 'hover' || legacyLabels === 'smart'
          ? legacyLabels
          : 'smart',
      showProbeMarkers: legacyProbes !== 'false',
      showOperatingPointBadges: false,
    },
    scope: { ...DEFAULT_APP_SETTINGS.scope },
    features: { ...DEFAULT_APP_SETTINGS.features },
  };
}

export function saveAppSettings(settings: AppSettings): void {
  try {
    localStorage.setItem(APP_SETTINGS_STORAGE_KEY, JSON.stringify(settings));
    localStorage.setItem('gecko-theme', settings.workspace.theme);
    localStorage.setItem('gecko-display-layout', settings.workspace.defaultScopeLayout);
    localStorage.setItem('gecko_label_mode', settings.schematic.netLabelMode);
    localStorage.setItem('gecko_show_probes', String(settings.schematic.showProbeMarkers));
    localStorage.setItem('gecko_show_op_badges', String(settings.schematic.showOperatingPointBadges));
  } catch {
    // storage unavailable
  }
}

export function applyWorkspacePreset(
  current: AppSettings,
  presetId: WorkspacePresetId,
): AppSettings {
  return {
    ...current,
    features: { ...PRESET_DEFINITIONS[presetId].features },
  };
}

export function detectActivePreset(settings: AppSettings): WorkspacePresetId | 'custom' {
  for (const [id, def] of Object.entries(PRESET_DEFINITIONS) as [
    WorkspacePresetId,
    (typeof PRESET_DEFINITIONS)[WorkspacePresetId],
  ][]) {
    const matches = (Object.keys(def.features) as (keyof AppSettings['features'])[]).every(
      (k) => settings.features[k] === def.features[k],
    );
    if (matches) return id;
  }
  return 'custom';
}

export function resetWorkspaceLayoutStorage(): void {
  try {
    localStorage.removeItem('gecko-right-sidebar-width');
    localStorage.removeItem('gecko-waveforms-drawer-ratio');
    localStorage.removeItem('gecko_viewctrl_pos');
  } catch {
    // ignore
  }
}
