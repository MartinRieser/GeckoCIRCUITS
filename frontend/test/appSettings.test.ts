// @vitest-environment jsdom
import { describe, expect, it, beforeEach } from 'vitest';
import {
  APP_SETTINGS_STORAGE_KEY,
  DEFAULT_APP_SETTINGS,
  applyWorkspacePreset,
  detectActivePreset,
  loadAppSettings,
  saveAppSettings,
} from '../src/model/appSettings';

describe('appSettings', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('defaults to Standard preset with schematic badges OFF and clean signals ON', () => {
    const s = loadAppSettings();
    expect(s.schematic.showOperatingPointBadges).toBe(false);
    expect(s.scope.hideDuplicateAndSpeculativeSignals).toBe(true);
    expect(detectActivePreset(s)).toBe('standard');
  });

  it('migrates legacy theme, layout, and label localStorage keys when unified key is absent', () => {
    localStorage.setItem('gecko-theme', 'light');
    localStorage.setItem('gecko-display-layout', '2-graphs');
    localStorage.setItem('gecko_label_mode', 'all');
    localStorage.setItem('gecko_show_probes', 'false');

    const s = loadAppSettings();
    expect(s.workspace.theme).toBe('light');
    expect(s.workspace.defaultScopeLayout).toBe('2-graphs');
    expect(s.schematic.netLabelMode).toBe('all');
    expect(s.schematic.showProbeMarkers).toBe(false);
    expect(s.schematic.showOperatingPointBadges).toBe(false);
  });

  it('switches workspace presets cleanly and persists settings', () => {
    const simple = applyWorkspacePreset(DEFAULT_APP_SETTINGS, 'simple');
    expect(detectActivePreset(simple)).toBe('simple');
    expect(simple.features.fftAnalysis).toBe(false);
    expect(simple.features.parametricSweep).toBe(false);

    const powerUser = applyWorkspacePreset(simple, 'power-user');
    expect(detectActivePreset(powerUser)).toBe('power-user');
    expect(powerUser.features.lossAnalysis).toBe(true);

    saveAppSettings(powerUser);
    expect(localStorage.getItem(APP_SETTINGS_STORAGE_KEY)).not.toBeNull();
    expect(loadAppSettings().features.lossAnalysis).toBe(true);
  });
});
