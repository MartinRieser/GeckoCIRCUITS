/**
 * Example Circuits Library Modal Dialog (PLECS / KiCad style).
 * Allows users to search, filter by category, explore curated simulation models,
 * load them cleanly into the workspace, or reveal the local examples/ folder in Explorer.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import { EXAMPLES, type CircuitExample } from '../model/examples';
import { openExamplesFolder } from '../desktop';

export interface ExamplesModalProps {
  /** Whether the dialog is visible. */
  isOpen: boolean;
  /** Callback to close the dialog. */
  onClose: () => void;
  /** Callback invoked when an example is selected to be opened in the workspace. */
  onSelectExample: (exampleId: string) => void;
}

export function ExamplesModal({
  isOpen,
  onClose,
  onSelectExample,
}: ExamplesModalProps) {
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<string>('All');
  const [folderStatus, setFolderStatus] = useState<string | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Available categories
  const categories = useMemo(() => {
    const set = new Set<string>();
    for (const ex of EXAMPLES) {
      if (ex.category) set.add(ex.category);
    }
    return ['All', ...Array.from(set)];
  }, []);

  // Filtered examples
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return EXAMPLES.filter((ex) => {
      const matchCat = category === 'All' || ex.category === category;
      if (!matchCat) return false;
      if (!q) return true;
      return (
        ex.name.toLowerCase().includes(q) ||
        ex.description.toLowerCase().includes(q) ||
        ex.category.toLowerCase().includes(q) ||
        ex.id.toLowerCase().includes(q)
      );
    });
  }, [search, category]);

  // Focus search on open
  useEffect(() => {
    if (isOpen) {
      setSearch('');
      setCategory('All');
      setFolderStatus(null);
      setTimeout(() => {
        searchInputRef.current?.focus();
      }, 50);
    }
  }, [isOpen]);

  // Escape key handler
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleRevealFolder = async () => {
    setFolderStatus('Opening folder in file explorer...');
    const ok = await openExamplesFolder();
    if (ok) {
      setFolderStatus('Opened examples folder in file explorer');
      setTimeout(() => setFolderStatus(null), 3500);
    } else {
      setFolderStatus('Folder located at ./examples in project directory');
      setTimeout(() => setFolderStatus(null), 4500);
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="modal-card examples-library-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="examples-modal-title"
      >
        {/* Modal Header */}
        <div className="modal-header">
          <div className="modal-title-group">
            <span className="modal-icon">💡</span>
            <div>
              <h2 id="examples-modal-title" className="modal-title">
                Example Circuits Library
              </h2>
              <div className="examples-modal-subtitle">
                Explore curated power electronics and analog simulation models
              </div>
            </div>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        {/* Toolbar: Search, Category Filter & Reveal Folder */}
        <div className="examples-modal-toolbar">
          <div className="examples-search-box">
            <span className="examples-search-icon">🔍</span>
            <input
              ref={searchInputRef}
              type="text"
              className="examples-search-input"
              placeholder="Search examples by name, category, or description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            {search && (
              <button
                type="button"
                className="examples-search-clear"
                onClick={() => setSearch('')}
                title="Clear search"
              >
                ✕
              </button>
            )}
          </div>

          <div className="examples-toolbar-actions">
            <button
              type="button"
              className="examples-reveal-btn"
              onClick={handleRevealFolder}
              title="Open the local examples folder in Windows Explorer or system file manager"
            >
              <span className="reveal-btn-icon">📂</span>
              <span>Open Folder in Explorer</span>
            </button>
          </div>
        </div>

        {/* Category Pill Filters & Status Notification */}
        <div className="examples-filter-row">
          <div className="examples-category-pills">
            {categories.map((cat) => {
              const count =
                cat === 'All'
                  ? EXAMPLES.length
                  : EXAMPLES.filter((e) => e.category === cat).length;
              const isActive = category === cat;
              return (
                <button
                  key={cat}
                  type="button"
                  className={`category-pill ${isActive ? 'active' : ''}`}
                  onClick={() => setCategory(cat)}
                >
                  <span>{cat}</span>
                  <span className="pill-count">{count}</span>
                </button>
              );
            })}
          </div>

          {folderStatus && (
            <div className="examples-folder-status" role="status">
              ℹ️ {folderStatus}
            </div>
          )}
        </div>

        {/* Examples Grid / List */}
        <div className="examples-modal-body">
          {filtered.length === 0 ? (
            <div className="examples-empty-state">
              <span className="empty-icon">🔍</span>
              <div className="empty-title">No matching circuits found</div>
              <div className="empty-desc">
                Try searching for other terms like &quot;buck&quot;, &quot;inverter&quot;, &quot;filter&quot;, or clear the search.
              </div>
              <button
                type="button"
                className="modal-btn secondary"
                onClick={() => {
                  setSearch('');
                  setCategory('All');
                }}
              >
                Reset Filters
              </button>
            </div>
          ) : (
            <div className="examples-grid">
              {filtered.map((ex: CircuitExample) => (
                <div
                  key={ex.id}
                  className="example-card"
                  onClick={() => onSelectExample(ex.id)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      onSelectExample(ex.id);
                    }
                  }}
                >
                  <div className="example-card-header">
                    <span
                      className={`example-badge ${
                        ex.category === 'Analog & Filter' ? 'badge-analog' : 'badge-power'
                      }`}
                    >
                      {ex.category}
                    </span>
                    <span className="example-id-tag">#{ex.id}</span>
                  </div>

                  <h3 className="example-card-title">{ex.name}</h3>
                  <p className="example-card-desc">{ex.description}</p>

                  <div className="example-card-footer">
                    <span className="example-open-label">Load Circuit</span>
                    <span className="example-open-arrow">→</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="modal-footer examples-modal-footer">
          <div className="examples-footer-hint">
            💡 Examples open as a clean copy in your workspace. Changes will not overwrite the original template files.
          </div>
          <button
            type="button"
            className="modal-btn secondary"
            onClick={onClose}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
