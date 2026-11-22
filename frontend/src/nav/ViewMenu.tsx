/**
 * Structured View Menu for toggling panels, schematic overlays, theme, and layout reset.
 */
import { useEffect, useRef, useState } from 'react';
import type { AppSettings } from '../model/appSettings';

export interface ViewMenuProps {
  leftSidebarOpen: boolean;
  onToggleLeftSidebar: () => void;
  rightSidebarOpen: boolean;
  onToggleRightSidebar: () => void;
  waveformsDrawerOpen: boolean;
  onToggleWaveformsDrawer: () => void;
  diagnosticsOpen: boolean;
  onToggleDiagnostics: () => void;
  settings: AppSettings;
  onUpdateSettings: (next: AppSettings) => void;
  onResetLayout: () => void;
}

export function ViewMenu({
  leftSidebarOpen,
  onToggleLeftSidebar,
  rightSidebarOpen,
  onToggleRightSidebar,
  waveformsDrawerOpen,
  onToggleWaveformsDrawer,
  diagnosticsOpen,
  onToggleDiagnostics,
  settings,
  onUpdateSettings,
  onResetLayout,
}: ViewMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;
    const onDown = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setIsOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsOpen(false);
    };
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [isOpen]);

  return (
    <div className="file-menu-wrap" ref={menuRef}>
      <button
        type="button"
        data-testid="nav-view-menu-btn"
        className={`file-menu-btn ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen((prev) => !prev)}
        aria-haspopup="true"
        aria-expanded={isOpen}
        title="Show or hide workspace panels, schematic overlays, and theme"
      >
        <span>View</span>
        <span className="file-menu-caret">▾</span>
      </button>

      {isOpen && (
        <div className="file-menu-dropdown" role="menu" data-testid="nav-view-menu">
          <button
            type="button"
            className="file-menu-item"
            role="menuitemcheckbox"
            aria-checked={leftSidebarOpen}
            onClick={() => {
              onToggleLeftSidebar();
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">{leftSidebarOpen ? '✓' : ''}</span>
            <span className="file-menu-label">Component Palette</span>
            <span className="file-menu-shortcut">Ctrl+B</span>
          </button>

          <button
            type="button"
            className="file-menu-item"
            role="menuitemcheckbox"
            aria-checked={rightSidebarOpen}
            onClick={() => {
              onToggleRightSidebar();
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">{rightSidebarOpen ? '✓' : ''}</span>
            <span className="file-menu-label">Properties Inspector</span>
            <span className="file-menu-shortcut">Ctrl+I</span>
          </button>

          <button
            type="button"
            className="file-menu-item"
            role="menuitemcheckbox"
            aria-checked={waveformsDrawerOpen}
            onClick={() => {
              onToggleWaveformsDrawer();
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">{waveformsDrawerOpen ? '✓' : ''}</span>
            <span className="file-menu-label">Waveforms Panel</span>
          </button>

          <button
            type="button"
            className="file-menu-item"
            role="menuitemcheckbox"
            aria-checked={diagnosticsOpen}
            onClick={() => {
              onToggleDiagnostics();
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">{diagnosticsOpen ? '✓' : ''}</span>
            <span className="file-menu-label">Circuit Diagnostics</span>
          </button>

          <div className="file-menu-divider" />

          <button
            type="button"
            className="file-menu-item"
            role="menuitemcheckbox"
            aria-checked={settings.schematic.showGrid}
            onClick={() => {
              onUpdateSettings({
                ...settings,
                schematic: { ...settings.schematic, showGrid: !settings.schematic.showGrid },
              });
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">{settings.schematic.showGrid ? '✓' : ''}</span>
            <span className="file-menu-label">Alignment Grid</span>
            <span className="file-menu-shortcut">#</span>
          </button>

          <button
            type="button"
            className="file-menu-item"
            role="menuitemcheckbox"
            aria-checked={settings.schematic.showProbeMarkers}
            onClick={() => {
              onUpdateSettings({
                ...settings,
                schematic: {
                  ...settings.schematic,
                  showProbeMarkers: !settings.schematic.showProbeMarkers,
                },
              });
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">{settings.schematic.showProbeMarkers ? '✓' : ''}</span>
            <span className="file-menu-label">Probe Markers</span>
          </button>

          <button
            type="button"
            data-testid="view-toggle-op-badges"
            className="file-menu-item"
            role="menuitemcheckbox"
            aria-checked={settings.schematic.showOperatingPointBadges}
            onClick={() => {
              onUpdateSettings({
                ...settings,
                schematic: {
                  ...settings.schematic,
                  showOperatingPointBadges: !settings.schematic.showOperatingPointBadges,
                },
              });
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">
              {settings.schematic.showOperatingPointBadges ? '✓' : ''}
            </span>
            <span className="file-menu-label">Schematic Values (Optional)</span>
          </button>

          <div className="file-menu-divider" />

          <button
            type="button"
            data-testid="view-toggle-theme-btn"
            className="file-menu-item"
            role="menuitem"
            onClick={() => {
              onUpdateSettings({
                ...settings,
                workspace: {
                  ...settings.workspace,
                  theme: settings.workspace.theme === 'dark' ? 'light' : 'dark',
                },
              });
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">
              {settings.workspace.theme === 'dark' ? '☀️' : '🌙'}
            </span>
            <span className="file-menu-label">
              Switch to {settings.workspace.theme === 'dark' ? 'Light' : 'Dark'} Theme
            </span>
          </button>

          <button
            type="button"
            data-testid="view-reset-layout-btn"
            className="file-menu-item"
            role="menuitem"
            onClick={() => {
              onResetLayout();
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">↺</span>
            <span className="file-menu-label">Reset Layout</span>
          </button>
        </div>
      )}
    </div>
  );
}
