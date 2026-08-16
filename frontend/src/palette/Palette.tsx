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
}

export function Palette({ catalog, onArm }: PaletteProps) {
  const [filter, setFilter] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<CategoryId>('all');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  // Augment catalog entries with schema metadata
  const catalogWithMeta = useMemo(() => {
    return (catalog || []).map((entry) => {
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

        <div className="palette-categories">
          {CATEGORIES.map((cat) => (
            <button
              key={cat.id}
              type="button"
              className={`category-pill ${selectedCategory === cat.id ? 'active' : ''}`}
              onClick={() => setSelectedCategory(cat.id)}
            >
              {cat.label}
            </button>
          ))}
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
