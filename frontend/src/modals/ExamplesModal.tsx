/**
 * Example Circuits Library Modal Dialog (PLECS / KiCad style).
 * Allows users to search, filter, explore, add, edit, and delete curated simulation models,
 * load them cleanly into the workspace, or reveal the local examples/ folder in Explorer.
 */
import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import {
  loadAllExamples,
  deleteExample,
  updateExample,
  subscribeExamples,
  type ManagedExample,
} from '../model/examplesService';
import { EXAMPLES } from '../model/examples';
import { openExamplesFolder } from '../desktop';
import { EditExampleModal } from './EditExampleModal';

export interface ExamplesModalProps {
  /** Whether the dialog is visible. */
  isOpen: boolean;
  /** Callback to close the dialog. */
  onClose: () => void;
  /** Callback invoked when an example is selected to be opened in the workspace. */
  onSelectExample: (exampleId: string, content?: string, name?: string) => void;
  /** Callback to open the "Save Active Circuit as Example" modal. */
  onAddNewExample?: () => void;
  /** Whether an active circuit is currently open in the workspace that can update an example. */
  canUpdateContent?: boolean;
  /** Callback to fetch active circuit content when updating an example with workspace schematic. */
  onGetActiveContent?: () => Promise<string | undefined>;
}

export function ExamplesModal({
  isOpen,
  onClose,
  onSelectExample,
  onAddNewExample,
  canUpdateContent = false,
  onGetActiveContent,
}: ExamplesModalProps) {
  const [examplesList, setExamplesList] = useState<ManagedExample[]>(() =>
    EXAMPLES.map((e) => ({ ...e, custom: false })),
  );
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<string>('All');
  const [folderStatus, setFolderStatus] = useState<string | null>(null);
  const [editingExample, setEditingExample] = useState<ManagedExample | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Load examples on mount or open
  const refreshList = useCallback(async () => {
    const list = await loadAllExamples();
    setExamplesList(list);
  }, []);

  useEffect(() => {
    return subscribeExamples((updated) => {
      setExamplesList(updated);
    });
  }, []);

  useEffect(() => {
    if (isOpen) {
      setSearch('');
      setCategory('All');
      setFolderStatus(null);
      setEditingExample(null);
      void refreshList();
      setTimeout(() => {
        searchInputRef.current?.focus();
      }, 50);
    }
  }, [isOpen, refreshList]);

  // Available categories
  const categories = useMemo(() => {
    const set = new Set<string>();
    for (const ex of examplesList) {
      if (ex.category) set.add(ex.category);
    }
    return ['All', ...Array.from(set)];
  }, [examplesList]);

  // Filtered examples
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return examplesList.filter((ex) => {
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
  }, [examplesList, search, category]);

  // Escape key handler
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !editingExample) {
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose, editingExample]);

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

  const handleDelete = async (e: React.MouseEvent, ex: ManagedExample) => {
    e.stopPropagation();
    const confirmed = window.confirm(
      `Are you sure you want to delete the example "${ex.name}"? This removes it from the examples library.`,
    );
    if (!confirmed) return;
    await deleteExample(ex.id);
  };

  const handleEditClick = (e: React.MouseEvent, ex: ManagedExample) => {
    e.stopPropagation();
    setEditingExample(ex);
  };

  const handleSaveEdit = async (
    id: string,
    updates: {
      name: string;
      category: string;
      description: string;
      updateContentWithWorkspace?: boolean;
    },
  ) => {
    let contentToUpdate: string | undefined = undefined;
    if (updates.updateContentWithWorkspace && onGetActiveContent) {
      contentToUpdate = await onGetActiveContent();
    }
    await updateExample(id, {
      name: updates.name,
      category: updates.category,
      description: updates.description,
      content: contentToUpdate,
    });
    setEditingExample(null);
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
                Explore, save, edit, and run power electronics and analog simulation models
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

        {/* Toolbar: Search, Add Example & Reveal Folder */}
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
            {onAddNewExample && (
              <button
                type="button"
                className="examples-add-btn"
                onClick={() => onAddNewExample()}
                title="Save current circuit as a new example"
              >
                <span className="add-btn-icon">➕</span>
                <span>Add Example</span>
              </button>
            )}

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
                  ? examplesList.length
                  : examplesList.filter((e) => e.category === cat).length;
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
              {filtered.map((ex: ManagedExample) => (
                <div
                  key={ex.id}
                  className="example-card"
                  onClick={() => onSelectExample(ex.id, ex.content, ex.name)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      onSelectExample(ex.id, ex.content, ex.name);
                    }
                  }}
                >
                  <div className="example-card-header">
                    <div className="example-badges-wrap">
                      <span
                        className={`example-badge ${
                          ex.category === 'Analog & Filter'
                            ? 'badge-analog'
                            : ex.custom
                              ? 'badge-custom'
                              : 'badge-power'
                        }`}
                      >
                        {ex.category}
                      </span>
                      {ex.custom && <span className="example-custom-pill">User</span>}
                    </div>

                    <div className="example-card-actions">
                      <button
                        type="button"
                        className="example-action-btn edit-btn"
                        onClick={(e) => handleEditClick(e, ex)}
                        title="Edit example title, description, or update circuit"
                        aria-label={`Edit ${ex.name}`}
                      >
                        ✏️
                      </button>
                      <button
                        type="button"
                        className="example-action-btn delete-btn"
                        onClick={(e) => handleDelete(e, ex)}
                        title="Delete example"
                        aria-label={`Delete ${ex.name}`}
                      >
                        🗑️
                      </button>
                      <span className="example-id-tag">#{ex.id}</span>
                    </div>
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
            💡 Examples open as a clean copy in your workspace. You can edit, simulate, and save changes using <strong>File → Save as Example...</strong>.
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

      {/* Nested Edit Example Modal */}
      <EditExampleModal
        isOpen={editingExample !== null}
        example={editingExample}
        onClose={() => setEditingExample(null)}
        onSave={handleSaveEdit}
        existingCategories={categories.filter((c) => c !== 'All')}
        canUpdateContent={canUpdateContent}
      />
    </div>
  );
}
