/**
 * Modal dialog for editing an existing example in the Examples Library.
 * Supports updating title, category, description, and optionally replacing
 * the stored circuit content with the active schematic.
 */
import { useEffect, useRef, useState } from 'react';
import type { ManagedExample } from '../model/examplesService';

export interface EditExampleModalProps {
  isOpen: boolean;
  example: ManagedExample | null;
  onClose: () => void;
  onSave: (
    id: string,
    updates: {
      name: string;
      category: string;
      description: string;
      updateContentWithWorkspace?: boolean;
    },
  ) => Promise<void>;
  existingCategories?: string[];
  canUpdateContent?: boolean;
}

export function EditExampleModal({
  isOpen,
  example,
  onClose,
  onSave,
  existingCategories = ['Power Electronics', 'Analog & Filter'],
  canUpdateContent = false,
}: EditExampleModalProps) {
  const [name, setName] = useState('');
  const [category, setCategory] = useState('');
  const [description, setDescription] = useState('');
  const [updateContent, setUpdateContent] = useState(false);
  const [busy, setBusy] = useState(false);
  const nameInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen && example) {
      setName(example.name);
      setCategory(example.category);
      setDescription(example.description);
      setUpdateContent(false);
      setBusy(false);
      setTimeout(() => {
        nameInputRef.current?.focus();
        nameInputRef.current?.select();
      }, 50);
    }
  }, [isOpen, example]);

  if (!isOpen || !example) return null;

  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!name.trim()) return;
    setBusy(true);
    try {
      await onSave(example.id, {
        name: name.trim(),
        category: category.trim() || 'Custom',
        description: description.trim(),
        updateContentWithWorkspace: updateContent,
      });
      onClose();
    } finally {
      setBusy(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      onClose();
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={onClose}
      onKeyDown={handleKeyDown}
      role="presentation"
    >
      <div
        className="modal-card edit-example-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="edit-example-title"
      >
        <div className="modal-header">
          <div className="modal-title-group">
            <span className="modal-icon">✏️</span>
            <div>
              <h2 id="edit-example-title" className="modal-title">
                Edit Example
              </h2>
              <div className="examples-modal-subtitle">
                Update metadata or overwrite circuit content
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

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <label htmlFor="edit-example-name" className="modal-field-label">
              Example Title
            </label>
            <input
              id="edit-example-name"
              ref={nameInputRef}
              type="text"
              className="modal-text-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />

            <label htmlFor="edit-example-cat" className="modal-field-label" style={{ marginTop: 8 }}>
              Category
            </label>
            <input
              id="edit-example-cat"
              type="text"
              list="edit-category-suggestions"
              className="modal-text-input"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            />
            <datalist id="edit-category-suggestions">
              {existingCategories.map((c) => (
                <option key={c} value={c} />
              ))}
            </datalist>

            <label htmlFor="edit-example-desc" className="modal-field-label" style={{ marginTop: 8 }}>
              Description
            </label>
            <textarea
              id="edit-example-desc"
              className="modal-text-input example-textarea"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />

            <div style={{ marginTop: 12 }}>
              <label className="modal-field-label">Schematic Content</label>
              {canUpdateContent ? (
                <label className="example-checkbox-label" style={{ marginTop: 4, fontWeight: 500 }}>
                  <input
                    type="checkbox"
                    checked={updateContent}
                    onChange={(e) => setUpdateContent(e.target.checked)}
                  />
                  <span>Overwrite example circuit diagram with the active schematic currently in workspace</span>
                </label>
              ) : (
                <p className="modal-field-hint" style={{ marginTop: 4 }}>
                  (To overwrite this example&apos;s circuit diagram, open a circuit in the workspace first)
                </p>
              )}
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="modal-btn secondary"
              onClick={onClose}
              disabled={busy}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="modal-btn primary"
              disabled={!name.trim() || busy}
            >
              {busy ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
