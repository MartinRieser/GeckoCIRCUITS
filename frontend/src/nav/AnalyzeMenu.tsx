/**
 * Structured Analyze & Tools Menu: Simulation Setup, Reference Compare,
 * Parametric Sweep, Live Sliders, Command Palette, and Shortcuts.
 */
import { useEffect, useRef, useState } from 'react';
import type { AppSettings } from '../model/appSettings';

export interface AnalyzeMenuProps {
  circuitId: string | null;
  hasResults: boolean;
  features: AppSettings['features'];
  onOpenSimConfig: () => void;
  onPinReference: () => void;
  onOpenSweep: () => void;
  onToggleLiveSliders: () => void;
  onOpenDiagnostics?: () => void;
  onOpenCommandPalette: () => void;
  onOpenShortcuts: () => void;
}

export function AnalyzeMenu({
  circuitId,
  hasResults,
  features,
  onOpenSimConfig,
  onPinReference,
  onOpenSweep,
  onToggleLiveSliders,
  onOpenDiagnostics,
  onOpenCommandPalette,
  onOpenShortcuts,
}: AnalyzeMenuProps) {
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
    window.addEventListener('mousedown', onDown);
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('mousedown', onDown);
      window.removeEventListener('keydown', onKey);
    };
  }, [isOpen]);

  return (
    <div className="file-menu-wrap" ref={menuRef}>
      <button
        type="button"
        data-testid="nav-analyze-menu-btn"
        className={`file-menu-btn ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen((prev) => !prev)}
        aria-haspopup="true"
        aria-expanded={isOpen}
        title="Simulation Setup, Parameter Sweep, Live Sliders, and Quick Commands"
      >
        <span>Analyze</span>
        <span className="file-menu-caret">▾</span>
      </button>

      {isOpen && (
        <div className="file-menu-dropdown" role="menu" data-testid="nav-analyze-menu">
          <button
            type="button"
            className="file-menu-item"
            role="menuitem"
            disabled={!circuitId}
            onClick={() => {
              onOpenSimConfig();
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">⚙</span>
            <span className="file-menu-label">Simulation Setup…</span>
          </button>

          {features.referenceCompare && (
            <button
              type="button"
              className="file-menu-item"
              role="menuitem"
              disabled={!hasResults}
              onClick={() => {
                onPinReference();
                setIsOpen(false);
              }}
            >
              <span className="file-menu-icon">⇄</span>
              <span className="file-menu-label">Save Reference Curves</span>
            </button>
          )}

          {features.parametricSweep && (
            <button
              type="button"
              className="file-menu-item"
              role="menuitem"
              disabled={!circuitId}
              onClick={() => {
                onOpenSweep();
                setIsOpen(false);
              }}
            >
              <span className="file-menu-icon">📈</span>
              <span className="file-menu-label">Parametric Sweep…</span>
            </button>
          )}

          {features.liveSliders && (
            <button
              type="button"
              className="file-menu-item"
              role="menuitem"
              disabled={!circuitId}
              onClick={() => {
                onToggleLiveSliders();
                setIsOpen(false);
              }}
            >
              <span className="file-menu-icon">🎚</span>
              <span className="file-menu-label">Live Parameter Sliders</span>
            </button>
          )}

          {onOpenDiagnostics && (
            <button
              type="button"
              data-testid="nav-open-diagnostics-btn"
              className="file-menu-item"
              role="menuitem"
              onClick={() => {
                onOpenDiagnostics();
                setIsOpen(false);
              }}
            >
              <span className="file-menu-icon">🩺</span>
              <span className="file-menu-label">Circuit Diagnostics Check</span>
            </button>
          )}

          <div className="file-menu-divider" />

          <button
            type="button"
            data-testid="nav-open-command-palette-btn"
            className="file-menu-item"
            role="menuitem"
            onClick={() => {
              onOpenCommandPalette();
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">🔍</span>
            <span className="file-menu-label">Search Parts &amp; Commands…</span>
            <span className="file-menu-shortcut">Ctrl+K</span>
          </button>

          <button
            type="button"
            data-testid="nav-open-shortcuts-btn"
            className="file-menu-item"
            role="menuitem"
            onClick={() => {
              onOpenShortcuts();
              setIsOpen(false);
            }}
          >
            <span className="file-menu-icon">⌨</span>
            <span className="file-menu-label">Keyboard Shortcuts</span>
            <span className="file-menu-shortcut">?</span>
          </button>
        </div>
      )}
    </div>
  );
}
