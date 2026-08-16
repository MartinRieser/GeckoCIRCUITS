/**
 * Command Palette modal dialog (Ctrl+K or /): quick keyboard search
 * to find and arm any component with live symbol preview.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import type { CatalogEntry } from '../model/types';
import { getComponentMeta } from '../model/componentSchema';
import { SymbolPreview } from '../canvas/symbols';

interface CommandPaletteProps {
  isOpen: boolean;
  onClose: () => void;
  catalog: CatalogEntry[];
  onSelect: (entry: CatalogEntry) => void;
}

export function CommandPalette({
  isOpen,
  onClose,
  catalog,
  onSelect,
}: CommandPaletteProps) {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setSelectedIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  const items = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (catalog || [])
      .map((entry) => {
        const meta = getComponentMeta(entry.type, entry.family, entry.name);
        return {
          entry,
          meta,
          matchScore: getMatchScore(meta.displayName, meta.name, meta.description, q),
        };
      })
      .filter((item) => item.matchScore > 0)
      .sort((a, b) => b.matchScore - a.matchScore);
  }, [catalog, query]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [items]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev + 1) % Math.max(1, items.length));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev - 1 + items.length) % Math.max(1, items.length));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (items[selectedIndex]) {
        onSelect(items[selectedIndex].entry);
        onClose();
      }
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="command-palette-backdrop" onClick={onClose}>
      <div
        className="command-palette-modal"
        onClick={(e) => e.stopPropagation()}
        onKeyDown={handleKeyDown}
      >
        <div className="command-search-row">
          <span className="command-search-icon">🔍</span>
          <input
            ref={inputRef}
            type="text"
            className="command-search-input"
            placeholder="Search component to place (e.g. Resistor, Diode, MOSFET, V_in)..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <kbd className="command-esc-badge">Esc</kbd>
        </div>

        <div className="command-results-list">
          {items.length === 0 ? (
            <div className="command-empty">No matching components found</div>
          ) : (
            items.map(({ entry, meta }, index) => {
              const isSelected = index === selectedIndex;
              return (
                <div
                  key={`${entry.family}-${entry.type}`}
                  className={`command-item ${isSelected ? 'selected' : ''}`}
                  onClick={() => {
                    onSelect(entry);
                    onClose();
                  }}
                  onMouseEnter={() => setSelectedIndex(index)}
                >
                  <div className="command-item-symbol">
                    <SymbolPreview type={entry.type} family={entry.family} size={32} />
                  </div>
                  <div className="command-item-info">
                    <div className="command-item-name-row">
                      <span className="command-item-name">{meta.displayName}</span>
                      <span className="command-item-category">{meta.category}</span>
                    </div>
                    <span className="command-item-desc">{meta.description}</span>
                  </div>
                  {meta.shortcut && (
                    <kbd className="command-item-shortcut">{meta.shortcut}</kbd>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}

function getMatchScore(displayName: string, name: string, desc: string, q: string): number {
  if (!q) return 1;
  const d = displayName.toLowerCase();
  const n = name.toLowerCase();
  const dc = desc.toLowerCase();

  if (d === q || n === q) return 100;
  if (d.startsWith(q)) return 80;
  if (d.includes(q)) return 60;
  if (n.includes(q)) return 40;
  if (dc.includes(q)) return 20;
  return 0;
}
