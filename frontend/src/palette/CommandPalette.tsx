/**
 * Command Palette modal dialog (Ctrl+K or /): quick keyboard search
 * to find and arm any component or reusable snippet, or run snippet save actions.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import type { CatalogEntry } from '../model/types';
import { getComponentMeta } from '../model/componentSchema';
import { SymbolPreview } from '../canvas/symbols';
import type { SchematicSnippet } from '../model/snippets';

/**
 * Properties for the {@link CommandPalette} modal.
 */
export interface CommandPaletteProps {
  /** Whether the modal is currently visible. */
  isOpen: boolean;
  /** Callback to close the modal (e.g. on Esc key or clicking backdrop). */
  onClose: () => void;
  /** Catalog of components to search through. */
  catalog: CatalogEntry[];
  /** Callback invoked when a component is selected from the list. */
  onSelect: (entry: CatalogEntry) => void;
  /** Optional list of reusable schematic snippets. */
  snippets?: SchematicSnippet[];
  /** Optional callback invoked when a snippet is chosen. */
  onSelectSnippet?: (snippet: SchematicSnippet) => void;
  /** Whether schematic components are currently selected. */
  hasSelection?: boolean;
  /** Optional action to save current selection as a snippet. */
  onSaveAsSnippet?: () => void;
}

type CommandEntry =
  | { kind: 'action'; key: string; label: string; category: string; desc: string; score: number; run: () => void }
  | { kind: 'snippet'; key: string; snippet: SchematicSnippet; score: number }
  | {
      kind: 'component';
      key: string;
      entry: CatalogEntry;
      meta: ReturnType<typeof getComponentMeta>;
      score: number;
    };

/**
 * Fast keyboard-driven command palette modal for searching and selecting circuit components
 * and schematic snippets. Supports up/down arrow key navigation, Enter to choose, and Esc to dismiss.
 */
export function CommandPalette({
  isOpen,
  onClose,
  catalog,
  onSelect,
  snippets,
  onSelectSnippet,
  hasSelection = false,
  onSaveAsSnippet,
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

  const commands = useMemo<CommandEntry[]>(() => {
    const q = query.trim().toLowerCase();
    const list: CommandEntry[] = [];

    if (hasSelection && onSaveAsSnippet) {
      const score = getMatchScore('Save Selection as Snippet', 'snippet', 'Save selected components and wires as reusable snippet', q);
      if (score > 0) {
        list.push({
          kind: 'action',
          key: 'action-save-snippet',
          label: 'Save Selection as Snippet...',
          category: 'Snippet',
          desc: 'Save selected components and wires to the Snippet library',
          score: q ? score + 10 : 90,
          run: onSaveAsSnippet,
        });
      }
    }

    if (snippets && onSelectSnippet) {
      for (const snip of snippets) {
        const score = getMatchScore(snip.name, snip.category, snip.description, q);
        if (score > 0) {
          list.push({
            kind: 'snippet',
            key: `snip-${snip.id}`,
            snippet: snip,
            score,
          });
        }
      }
    }

    for (const entry of catalog || []) {
      if (
        entry.type === 1003 ||
        entry.type === 5 ||
        entry.name === 'CONTROL_SCOPE' ||
        entry.name === 'SCOPE'
      ) {
        continue;
      }
      const meta = getComponentMeta(entry.type, entry.family, entry.name);
      const score = getMatchScore(meta.displayName, meta.name, meta.description, q);
      if (score > 0) {
        list.push({
          kind: 'component',
          key: `${entry.family}-${entry.type}`,
          entry,
          meta,
          score,
        });
      }
    }

    return list.sort((a, b) => b.score - a.score);
  }, [catalog, query, snippets, onSelectSnippet, hasSelection, onSaveAsSnippet]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [commands]);

  const executeItem = (item: CommandEntry) => {
    if (item.kind === 'action') {
      item.run();
    } else if (item.kind === 'snippet') {
      onSelectSnippet?.(item.snippet);
    } else {
      onSelect(item.entry);
    }
    onClose();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev + 1) % Math.max(1, commands.length));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev - 1 + commands.length) % Math.max(1, commands.length));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (commands[selectedIndex]) {
        executeItem(commands[selectedIndex]);
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
            placeholder="Search component to place (e.g. Resistor, Diode, MOSFET, Buck, V_in)..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <kbd className="command-esc-badge">Esc</kbd>
        </div>

        <div className="command-results-list">
          {commands.length === 0 ? (
            <div className="command-empty">No matching components found</div>
          ) : (
            commands.map((item, index) => {
              const isSelected = index === selectedIndex;
              if (item.kind === 'action') {
                return (
                  <div
                    key={item.key}
                    className={`command-item ${isSelected ? 'selected' : ''}`}
                    onClick={() => executeItem(item)}
                    onMouseEnter={() => setSelectedIndex(index)}
                  >
                    <div className="command-item-symbol">📦</div>
                    <div className="command-item-info">
                      <div className="command-item-name-row">
                        <span className="command-item-name">{item.label}</span>
                        <span className="command-item-category">{item.category}</span>
                      </div>
                      <span className="command-item-desc">{item.desc}</span>
                    </div>
                  </div>
                );
              }
              if (item.kind === 'snippet') {
                return (
                  <div
                    key={item.key}
                    className={`command-item ${isSelected ? 'selected' : ''}`}
                    onClick={() => executeItem(item)}
                    onMouseEnter={() => setSelectedIndex(index)}
                  >
                    <div className="command-item-symbol">🧩</div>
                    <div className="command-item-info">
                      <div className="command-item-name-row">
                        <span className="command-item-name">{item.snippet.name}</span>
                        <span className="command-item-category">Snippet · {item.snippet.category}</span>
                      </div>
                      <span className="command-item-desc">
                        {item.snippet.description || `${item.snippet.components.length} components, ${item.snippet.wires.length} wires`}
                      </span>
                    </div>
                  </div>
                );
              }
              return (
                <div
                  key={item.key}
                  className={`command-item ${isSelected ? 'selected' : ''}`}
                  onClick={() => executeItem(item)}
                  onMouseEnter={() => setSelectedIndex(index)}
                >
                  <div className="command-item-symbol">
                    <SymbolPreview type={item.entry.type} family={item.entry.family} size={32} />
                  </div>
                  <div className="command-item-info">
                    <div className="command-item-name-row">
                      <span className="command-item-name">{item.meta.displayName}</span>
                      {item.meta.disabled && <span className="card-soon-badge">planned</span>}
                      <span className="command-item-category">{item.meta.category}</span>
                    </div>
                    <span className="command-item-desc">
                      {item.meta.disabled
                        ? `${item.meta.description} — not placeable yet`
                        : item.meta.description}
                    </span>
                  </div>
                  {item.meta.shortcut && (
                    <kbd className="command-item-shortcut">{item.meta.shortcut}</kbd>
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

/**
 * Computes a relevance match score between a search query and a component's name/description.
 *
 * @param displayName User-friendly label of the component.
 * @param name Engine or schematic identifier name.
 * @param desc Explanatory description text.
 * @param q Lowercase search query string.
 * @returns Score higher than 0 if matching; 0 if no match.
 */
export function getMatchScore(displayName: string, name: string, desc: string, q: string): number {
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
