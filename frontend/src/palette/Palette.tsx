/**
 * Component palette with visual SVG schematic symbols, categorized tabs,
 * instant search filtering, reusable Snippets library, keyboard shortcut hints,
 * and drag-and-drop / click-to-arm support.
 */
import { useMemo, useRef, useState } from 'react';
import type { CatalogEntry } from '../model/types';
import { CATEGORIES, getComponentMeta } from '../model/componentSchema';
import type { CategoryId } from '../model/componentSchema';
import { ControlComponentType } from '../model/constants';
import { SymbolPreview } from '../canvas/symbols';
import type { SchematicSnippet } from '../model/snippets';

/**
 * Properties for the {@link Palette} component.
 */
export interface PaletteProps {
  /** List of available catalog entries from engine or standard component library. */
  catalog: CatalogEntry[];
  /** Callback invoked when the user selects or drags a component to place it on the canvas. */
  onArm: (entry: CatalogEntry) => void;
  /** Optional callback invoked when the user clicks the collapse button. */
  onCollapse?: () => void;
  /** Optional list of built-in + user schematic snippets. */
  snippets?: SchematicSnippet[];
  /** Arms a snippet for multi-component schematic placement. */
  onArmSnippet?: (snippet: SchematicSnippet) => void;
  /** Deletes a user-saved snippet by ID. */
  onDeleteSnippet?: (id: string) => void;
  /** Exports user-saved snippets as JSON. */
  onExportSnippets?: () => void;
  /** Imports snippets from a JSON bundle string. */
  onImportSnippets?: (json: string) => void;
  /** Number of currently selected components on canvas (enables direct save from Snippets footer). */
  selectedCount?: number;
  /** Opens Save Selection as Snippet modal directly from Snippets tab. */
  onSaveSelectionAsSnippet?: () => void;
}

function SnippetThumbnail({ snippet }: { snippet: SchematicSnippet }) {
  const vbW = Math.max(4, snippet.width + 4);
  const vbH = Math.max(4, snippet.height + 4);
  return (
    <svg
      className="snippet-thumb-svg"
      width={44}
      height={36}
      viewBox={`-2 -2 ${vbW} ${vbH}`}
      aria-hidden="true"
    >
      {snippet.wires.map((w, idx) => (
        <polyline
          key={idx}
          points={w.points.map(([x, y]) => `${x},${y}`).join(' ')}
          fill="none"
          stroke={w.type === 'CONTROL' ? '#4ade80' : '#38bdf8'}
          strokeWidth={0.5}
          strokeLinejoin="round"
        />
      ))}
      {snippet.components.map((c) => (
        <rect
          key={c.localId}
          x={c.offset[0] - 0.9}
          y={c.offset[1] - 0.9}
          width={1.8}
          height={1.8}
          rx={0.3}
          fill={c.family === 'CONTROL' ? 'rgba(74, 222, 128, 0.35)' : 'rgba(56, 189, 248, 0.35)'}
          stroke={c.family === 'CONTROL' ? '#4ade80' : '#38bdf8'}
          strokeWidth={0.35}
        />
      ))}
    </svg>
  );
}

/**
 * Component palette sidebar with categorized component lists, search filtering,
 * reusable schematic snippets tab, instant keyboard shortcut indicators, and
 * drag-and-drop / click-to-arm workflows.
 */
export function Palette({
  catalog,
  onArm,
  onCollapse,
  snippets,
  onArmSnippet,
  onDeleteSnippet,
  onExportSnippets,
  onImportSnippets,
  selectedCount = 0,
  onSaveSelectionAsSnippet,
}: PaletteProps) {
  const [filter, setFilter] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<CategoryId | 'snippets'>('all');
  const importInputRef = useRef<HTMLInputElement>(null);
  const isSnippetsCategory = selectedCategory === 'snippets' && snippets !== undefined;

  // Augment catalog entries with schema metadata (hide legacy Java block and Scope from new part palette)
  const catalogWithMeta = useMemo(() => {
    return (catalog || [])
      .filter(
        (entry) =>
          entry.type !== ControlComponentType.LEGACY_JAVA_FUNCTION &&
          entry.name !== 'C_JAVA_FUNCTION' &&
          entry.type !== ControlComponentType.SCOPE &&
          entry.type !== ControlComponentType.LEGACY_SCOPE &&
          entry.name !== 'CONTROL_SCOPE' &&
          entry.name !== 'SCOPE',
      )
      .map((entry) => {
        const meta = getComponentMeta(entry.type, entry.family, entry.name);
        return {
          ...entry,
          meta,
          displayName: meta.displayName,
          category: meta.category,
          shortcut: meta.shortcut,
          description: meta.description,
        };
      });
  }, [catalog]);

  // Category item counts
  const categoryCounts = useMemo(() => {
    const counts: Record<string, number> = { all: catalogWithMeta.length };
    for (const entry of catalogWithMeta) {
      counts[entry.category] = (counts[entry.category] || 0) + 1;
    }
    return counts;
  }, [catalogWithMeta]);

  // Filter components
  const filteredEntries = useMemo(() => {
    const needle = filter.trim().toLowerCase();
    return catalogWithMeta.filter((entry) => {
      const matchesCategory =
        selectedCategory === 'all' || entry.category === selectedCategory;
      if (!matchesCategory) return false;

      if (!needle) return true;
      return (
        entry.displayName.toLowerCase().includes(needle) ||
        entry.name.toLowerCase().includes(needle) ||
        entry.description.toLowerCase().includes(needle) ||
        (entry.shortcut && entry.shortcut.toLowerCase() === needle)
      );
    });
  }, [catalogWithMeta, filter, selectedCategory]);

  // Filter snippets
  const filteredSnippets = useMemo(() => {
    if (!snippets) return [];
    const needle = filter.trim().toLowerCase();
    if (!needle) return snippets;
    return snippets.filter(
      (s) =>
        s.name.toLowerCase().includes(needle) ||
        s.category.toLowerCase().includes(needle) ||
        s.description.toLowerCase().includes(needle),
    );
  }, [snippets, filter]);

  const hasSnippets = snippets !== undefined;

  return (
    <div className="palette-container">
      <div className="palette-header">
        <div className="palette-title-row">
          <span className="palette-title">
            {isSnippetsCategory ? 'Templates & Snippets' : 'Components'}
          </span>
          {onCollapse && (
            <button
              type="button"
              className="sidebar-toggle-btn"
              onClick={onCollapse}
              title="Collapse palette panel (Ctrl+B)"
              style={{ height: '22px', padding: '0 6px', fontSize: '10px' }}
            >
              ◀
            </button>
          )}
        </div>

        <div className="palette-search-box">
          <input
            type="search"
            className="palette-search-input"
            placeholder={
              isSnippetsCategory
                ? 'Search snippets (e.g. Buck, Filter, PI)...'
                : 'Search parts (e.g. Resistor, R, 24V)...'
            }
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
          {filter && (
            <button
              type="button"
              className="palette-search-clear"
              onClick={() => setFilter('')}
              title="Clear search"
            >
              ✕
            </button>
          )}
        </div>

        <div className="palette-category-row">
          <div className="palette-category-select-wrapper">
            <select
              id="palette-category-select"
              className="palette-category-select"
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value as CategoryId | 'snippets')}
              aria-label="Filter components by category"
            >
              {CATEGORIES.map((cat) => {
                const count = categoryCounts[cat.id] ?? 0;
                if (cat.id !== 'all' && count === 0) return null;
                return (
                  <option key={cat.id} value={cat.id}>
                    {cat.label}
                  </option>
                );
              })}
              {hasSnippets && (
                <option value="snippets">
                  Templates / Snippets ({snippets.length})
                </option>
              )}
            </select>
            <span className="palette-category-caret">▾</span>
          </div>
          {selectedCategory !== 'all' && (
            <button
              type="button"
              className="palette-category-reset-btn"
              onClick={() => setSelectedCategory('all')}
              title="Reset to All Components"
            >
              All
            </button>
          )}
        </div>
      </div>

      <div className="palette-body">
        {!isSnippetsCategory ? (
          filteredEntries.length === 0 ? (
            <div className="palette-empty">
              <span>No components found</span>
              {filter && (
                <button
                  type="button"
                  className="btn-link"
                  onClick={() => {
                    setFilter('');
                    setSelectedCategory('all');
                  }}
                >
                  Reset filters
                </button>
              )}
            </div>
          ) : (
            <div className="palette-list">
              {filteredEntries.map((entry) => {
                const disabled = entry.meta?.disabled ?? false;
                return (
                  <button
                    key={`${entry.family}-${entry.type}`}
                    type="button"
                    className={`component-card palette-entry${disabled ? ' disabled' : ''}`}
                    onClick={() => onArm(entry)}
                    draggable={!disabled}
                    onDragStart={(e) => {
                      if (disabled) {
                        e.preventDefault();
                        return;
                      }
                      e.dataTransfer.setData('text/plain', JSON.stringify(entry));
                      onArm(entry);
                    }}
                    title={
                      disabled
                        ? `${entry.displayName} — not available yet (planned): the engine cannot simulate this component at the moment`
                        : `${entry.displayName} — click to arm, then click the sheet; or drag onto the sheet`
                    }
                  >
                    <div className="card-symbol-wrap">
                      <SymbolPreview
                        type={entry.type}
                        family={entry.family}
                        size={44}
                        color={
                          entry.family === 'CONTROL'
                            ? '#4ade80'
                            : entry.family === 'THERM'
                            ? '#fb923c'
                            : undefined
                        }
                      />
                    </div>
                    <div className="card-info">
                      <div className="card-name-row">
                        <span className="card-name">{entry.displayName}</span>
                        {disabled ? (
                          <span className="card-soon-badge">planned</span>
                        ) : (
                          entry.shortcut && <kbd className="card-shortcut">{entry.shortcut}</kbd>
                        )}
                      </div>
                      <span className="card-desc">{entry.description}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          )
        ) : (
          <div className="snippet-tab-content">
            {filteredSnippets.length === 0 ? (
              <div className="palette-empty">
                <span>No snippets found</span>
                {filter && (
                  <button type="button" className="btn-link" onClick={() => setFilter('')}>
                    Clear search
                  </button>
                )}
              </div>
            ) : (
              <div className="palette-list">
                {filteredSnippets.map((snippet) => (
                  <div
                    key={snippet.id}
                    className="component-card palette-entry snippet-card"
                    data-testid={`snippet-card-${snippet.id}`}
                    role="button"
                    tabIndex={0}
                    onClick={() => onArmSnippet?.(snippet)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        onArmSnippet?.(snippet);
                      }
                    }}
                    title={`${snippet.name} — click to arm snippet, then click the sheet to insert`}
                  >
                    <div className="card-symbol-wrap">
                      <SnippetThumbnail snippet={snippet} />
                    </div>
                    <div className="card-info">
                      <div className="card-name-row">
                        <span className="card-name">{snippet.name}</span>
                        {snippet.builtIn ? (
                          <span className="snippet-badge">Built-in</span>
                        ) : (
                          onDeleteSnippet && (
                            <button
                              type="button"
                              className="snippet-delete-btn"
                              data-testid={`delete-snippet-${snippet.id}`}
                              title={`Delete snippet ${snippet.name}`}
                              onClick={(e) => {
                                e.stopPropagation();
                                onDeleteSnippet(snippet.id);
                              }}
                            >
                              🗑
                            </button>
                          )
                        )}
                      </div>
                      <span className="snippet-meta">
                        {snippet.category} · {snippet.components.length} parts · {snippet.wires.length} wires
                      </span>
                      {snippet.description && (
                        <span className="card-desc">{snippet.description}</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {(onSaveSelectionAsSnippet || onExportSnippets || onImportSnippets) && (
              <div className="snippet-footer-actions">
                {onSaveSelectionAsSnippet && (
                  <button
                    type="button"
                    data-testid="palette-save-snippet-btn"
                    className="snippet-save-sel-btn"
                    disabled={selectedCount === 0}
                    onClick={onSaveSelectionAsSnippet}
                    title={
                      selectedCount > 0
                        ? `Save ${selectedCount} selected component(s) and internal wires as a snippet`
                        : 'Select components on the schematic to save as a snippet'
                    }
                  >
                    {selectedCount > 0
                      ? `📦 Save Selected (${selectedCount})`
                      : '📦 Select parts to save snippet'}
                  </button>
                )}
                {onExportSnippets && (
                  <button
                    type="button"
                    className="snippet-footer-btn"
                    data-testid="export-snippets-btn"
                    onClick={onExportSnippets}
                  >
                    Export JSON
                  </button>
                )}
                {onImportSnippets && (
                  <>
                    <button
                      type="button"
                      className="snippet-footer-btn"
                      data-testid="import-snippets-btn"
                      onClick={() => importInputRef.current?.click()}
                    >
                      Import JSON
                    </button>
                    <input
                      ref={importInputRef}
                      type="file"
                      accept=".json,application/json"
                      style={{ display: 'none' }}
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) {
                          void file.text().then((txt) => onImportSnippets(txt));
                        }
                        e.target.value = '';
                      }}
                    />
                  </>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
