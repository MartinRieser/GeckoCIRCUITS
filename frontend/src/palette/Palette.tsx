/**
 * Component palette with visual SVG schematic symbols, categorized tabs,
 * instant search filtering, keyboard shortcut hints, and drag-and-drop /
 * click-to-arm support.
 */
import { useMemo, useState } from 'react';
import type { CatalogEntry } from '../model/types';
import { CATEGORIES, getComponentMeta } from '../model/componentSchema';
import type { CategoryId } from '../model/componentSchema';
import { SymbolPreview } from '../canvas/symbols';

interface PaletteProps {
  catalog: CatalogEntry[];
  onArm: (entry: CatalogEntry) => void;
  onCollapse?: () => void;
}

export function Palette({ catalog, onArm, onCollapse }: PaletteProps) {
  const [filter, setFilter] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<CategoryId>('all');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  // Augment catalog entries with schema metadata (hide legacy Java block from new part palette)
  const catalogWithMeta = useMemo(() => {
    return (catalog || [])
      .filter((entry) => entry.type !== 61 && entry.name !== 'C_JAVA_FUNCTION')
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

  // Filter and group
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

  return (
    <div className="palette-container">
      <div className="palette-header">
        <div className="palette-title-row">
          <span className="palette-title">Components</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <div className="palette-view-toggle">
              <button
                type="button"
                className={`view-btn ${viewMode === 'grid' ? 'active' : ''}`}
                onClick={() => setViewMode('grid')}
                title="Grid view"
              >
                ⊞
              </button>
              <button
                type="button"
                className={`view-btn ${viewMode === 'list' ? 'active' : ''}`}
                onClick={() => setViewMode('list')}
                title="List view"
              >
                ☰
              </button>
            </div>
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
        </div>

        <div className="palette-search-box">
          <input
            type="search"
            className="palette-search-input"
            placeholder="Search parts (e.g. Resistor, R, 24V)..."
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
              onChange={(e) => setSelectedCategory(e.target.value as CategoryId)}
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
        {filteredEntries.length === 0 ? (
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
          <div className={`palette-${viewMode}`}>
            {filteredEntries.map((entry) => (
              <button
                key={`${entry.family}-${entry.type}`}
                type="button"
                className="component-card palette-entry"
                onClick={() => onArm(entry)}
                draggable
                onDragStart={(e) => {
                  e.dataTransfer.setData('text/plain', JSON.stringify(entry));
                  onArm(entry);
                }}
                title={`${entry.displayName} — click to arm, then click the sheet; or drag onto the sheet`}
              >
                <div className="card-symbol-wrap">
                  <SymbolPreview
                    type={entry.type}
                    family={entry.family}
                    size={viewMode === 'grid' ? 44 : 32}
                    color={entry.family === 'CONTROL' ? '#4ade80' : entry.family === 'THERM' ? '#fb923c' : undefined}
                  />
                </div>
                <div className="card-info">
                  <div className="card-name-row">
                    <span className="card-name">{entry.displayName}</span>
                    {entry.shortcut && (
                      <kbd className="card-shortcut">{entry.shortcut}</kbd>
                    )}
                  </div>
                  {viewMode === 'list' && (
                    <span className="card-desc">{entry.description}</span>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
