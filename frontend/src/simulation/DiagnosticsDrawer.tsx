/**
 * Persistent simulation diagnostics badge and bottom workspace drawer.
 *
 * Surfaces engine failures, runtime warnings, step-width/frequency advisories,
 * and schematic Design Rule Checks (DRC) across all workspace tabs with
 * click-to-locate navigation and 1-click quick fixes.
 */
import { useMemo, useState } from 'react';
import type { DiagnosticItem, DiagnosticQuickFix } from '../model/validation';

/** Props for the compact diagnostics summary badge rendered in the navbar and status bar. */
export interface DiagnosticsBadgeProps {
  /** Active list of structured circuit and simulation diagnostics. */
  diagnostics: DiagnosticItem[];
  /** Whether the bottom diagnostics drawer is currently open. */
  isOpen: boolean;
  /** Callback to open or close the diagnostics drawer. */
  onToggle: () => void;
  /** Uses condensed icon+count labels suitable for the bottom status bar. */
  compact?: boolean;
  /** Custom `data-testid` attribute for testing. */
  testId?: string;
}

/**
 * Summarizes error and warning counts in the top navbar or bottom status bar.
 */
export function DiagnosticsBadge({
  diagnostics,
  isOpen,
  onToggle,
  compact = false,
  testId = 'navbar-diagnostics-badge',
}: DiagnosticsBadgeProps) {
  const { errorCount, warningCount } = useMemo(() => {
    let errors = 0;
    let warnings = 0;
    for (const d of diagnostics) {
      if (d.severity === 'error') errors += 1;
      else if (d.severity === 'warning') warnings += 1;
    }
    return { errorCount: errors, warningCount: warnings };
  }, [diagnostics]);

  const statusClass =
    errorCount > 0 ? 'has-errors' : warningCount > 0 ? 'has-warnings' : 'clean';

  let label: string;
  if (errorCount > 0 && warningCount > 0) {
    label = compact
      ? `🔴 ${errorCount} ⚠ ${warningCount}`
      : `🔴 ${errorCount} Error${errorCount > 1 ? 's' : ''} · ⚠ ${warningCount}`;
  } else if (errorCount > 0) {
    label = `🔴 ${errorCount} Error${errorCount > 1 ? 's' : ''}`;
  } else if (warningCount > 0) {
    label = `⚠ ${warningCount} Warn${compact ? '' : warningCount > 1 ? 'ings' : 'ing'}`;
  } else {
    label = compact ? '✓ Clean' : '✓ DRC Clean';
  }

  return (
    <button
      type="button"
      className={`diagnostics-badge ${statusClass}${isOpen ? ' active' : ''}${compact ? ' compact' : ''}`}
      data-testid={testId}
      onClick={onToggle}
      aria-expanded={isOpen}
      title={
        errorCount > 0 || warningCount > 0
          ? `${errorCount} error(s), ${warningCount} warning(s) — Click to inspect Diagnostics Drawer`
          : 'No errors or DRC warnings — Click to open Diagnostics Drawer'
      }
    >
      {label}
    </button>
  );
}

/** Props for the collapsible bottom diagnostics and Design Rule Check drawer. */
export interface DiagnosticsDrawerProps {
  /** Active structured diagnostic items to display. */
  diagnostics: DiagnosticItem[];
  /** Controls visibility of the drawer. */
  isOpen: boolean;
  /** Closes the drawer. */
  onClose: () => void;
  /** Navigates to and selects the component/wire associated with a diagnostic row. */
  onLocate: (item: DiagnosticItem) => void;
  /** Executes a diagnostic item's 1-click quick fix action. */
  onQuickFix: (item: DiagnosticItem, fix: DiagnosticQuickFix) => void;
  /** Optional handler to re-run/refresh DRC checks on demand. */
  onRunCheck?: () => void;
}

/** Severity filter tab state in the Diagnostics Drawer header. */
type DiagnosticFilter = 'all' | 'errors' | 'warnings';

const SEVERITY_ORDER: Record<DiagnosticItem['severity'], number> = {
  error: 0,
  warning: 1,
  info: 2,
};

const CATEGORY_LABELS: Record<DiagnosticItem['category'], string> = {
  engine: 'ENGINE',
  timing: 'TIMING',
  drc: 'DRC',
};

/**
 * Collapsible bottom drawer displaying structured diagnostics with click-to-locate and quick fixes.
 */
export function DiagnosticsDrawer({
  diagnostics,
  isOpen,
  onClose,
  onLocate,
  onQuickFix,
  onRunCheck,
}: DiagnosticsDrawerProps) {
  const [filter, setFilter] = useState<DiagnosticFilter>('all');

  const counts = useMemo(() => {
    let errors = 0;
    let warnings = 0;
    for (const d of diagnostics) {
      if (d.severity === 'error') errors += 1;
      else if (d.severity === 'warning') warnings += 1;
    }
    return { errors, warnings, total: diagnostics.length };
  }, [diagnostics]);

  const filteredItems = useMemo(() => {
    const base =
      filter === 'errors'
        ? diagnostics.filter((d) => d.severity === 'error')
        : filter === 'warnings'
          ? diagnostics.filter((d) => d.severity === 'warning')
          : diagnostics;
    return [...base].sort((a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity]);
  }, [diagnostics, filter]);

  if (!isOpen) return null;

  return (
    <section
      className="diagnostics-drawer"
      data-testid="diagnostics-drawer"
      aria-label="Circuit & Simulation Diagnostics"
    >
      <div className="diagnostics-drawer-header">
        <div className="diagnostics-drawer-title-group">
          <span className="diagnostics-drawer-title">Diagnostics & DRC</span>
          <div className="diagnostics-filter-pills" role="tablist" aria-label="Diagnostic filters">
            <button
              type="button"
              className={`diagnostics-filter-btn${filter === 'all' ? ' active' : ''}`}
              onClick={() => setFilter('all')}
            >
              All ({counts.total})
            </button>
            <button
              type="button"
              className={`diagnostics-filter-btn err${filter === 'errors' ? ' active' : ''}`}
              onClick={() => setFilter('errors')}
            >
              Errors ({counts.errors})
            </button>
            <button
              type="button"
              className={`diagnostics-filter-btn warn${filter === 'warnings' ? ' active' : ''}`}
              onClick={() => setFilter('warnings')}
            >
              Warnings ({counts.warnings})
            </button>
          </div>
        </div>

        <div className="diagnostics-drawer-actions">
          {onRunCheck && (
            <button
              type="button"
              className="diagnostics-header-btn"
              onClick={onRunCheck}
              title="Re-run schematic Design Rule Check"
            >
              ↻ Check Now
            </button>
          )}
          <button
            type="button"
            className="diagnostics-close-btn"
            onClick={onClose}
            aria-label="Close Diagnostics"
            title="Close Diagnostics Drawer"
          >
            ✕
          </button>
        </div>
      </div>

      <div className="diagnostics-drawer-body">
        {filteredItems.length === 0 ? (
          <div className="diagnostics-empty-state">
            <span>✓</span>
            <span>
              {filter === 'all'
                ? 'No simulation errors, timing advisories, or schematic DRC warnings detected.'
                : `No ${filter} to display.`}
            </span>
          </div>
        ) : (
          <ul className="diagnostics-list">
            {filteredItems.map((item) => {
              const locatable =
                Boolean(item.componentName) ||
                item.wireIndex !== undefined ||
                Boolean(item.coord);
              const severityIcon =
                item.severity === 'error'
                  ? '🔴'
                  : item.severity === 'warning'
                    ? '⚠️'
                    : 'ℹ️';

              return (
                <li
                  key={item.id}
                  className={`diagnostics-item sev-${item.severity}${locatable ? ' locatable' : ''}`}
                  data-testid={`diagnostic-item-${item.id}`}
                  onClick={() => {
                    if (locatable) onLocate(item);
                  }}
                >
                  <span className="diagnostics-item-icon" aria-hidden="true">
                    {severityIcon}
                  </span>
                  <span className={`diagnostics-cat-tag cat-${item.category}`}>
                    {CATEGORY_LABELS[item.category]}
                  </span>
                  <div className="diagnostics-item-content">
                    <div className="diagnostics-item-title">{item.title}</div>
                    <div className="diagnostics-item-detail">{item.detail}</div>
                  </div>
                  <div className="diagnostics-item-actions">
                    {locatable && (
                      <button
                        type="button"
                        className="diagnostics-action-btn locate"
                        onClick={(e) => {
                          e.stopPropagation();
                          onLocate(item);
                        }}
                        title="Center on schematic and select target"
                      >
                        ⌖ Locate
                      </button>
                    )}
                    {item.quickFix && (
                      <button
                        type="button"
                        className="diagnostics-action-btn fix"
                        onClick={(e) => {
                          e.stopPropagation();
                          onQuickFix(item, item.quickFix!);
                        }}
                      >
                        ⚡ {item.quickFix.label}
                      </button>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </section>
  );
}
