// @vitest-environment jsdom
import { describe, expect, it, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { SettingsModal } from '../src/modals/SettingsModal';
import { DEFAULT_APP_SETTINGS } from '../src/model/appSettings';

afterEach(cleanup);

describe('SettingsModal', () => {
  it('switches categories, applies presets, and toggles individual features', () => {
    const onUpdate = vi.fn();
    const onReset = vi.fn();
    render(
      <SettingsModal
        isOpen
        onClose={vi.fn()}
        settings={DEFAULT_APP_SETTINGS}
        onUpdateSettings={onUpdate}
        onResetLayout={onReset}
      />,
    );

    expect(screen.getByTestId('settings-panel-features')).toBeTruthy();

    // Apply Simple preset
    fireEvent.click(screen.getByTestId('settings-preset-simple'));
    expect(onUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        features: expect.objectContaining({
          fftAnalysis: false,
          parametricSweep: false,
          referenceCompare: false,
        }),
      }),
    );

    // Switch to Schematic Editor category and verify OP badges toggle defaults to false
    fireEvent.click(screen.getByTestId('settings-cat-schematic'));
    const opToggle = screen.getByTestId('setting-show-op-badges') as HTMLInputElement;
    expect(opToggle.checked).toBe(false);
    fireEvent.click(opToggle);
    expect(onUpdate).toHaveBeenLastCalledWith(
      expect.objectContaining({
        schematic: expect.objectContaining({ showOperatingPointBadges: true }),
      }),
    );

    // Switch to General & Workspace and trigger Reset Layout
    fireEvent.click(screen.getByTestId('settings-cat-general'));
    fireEvent.click(screen.getByTestId('setting-reset-layout-btn'));
    expect(onReset).toHaveBeenCalledTimes(1);
  });
});
