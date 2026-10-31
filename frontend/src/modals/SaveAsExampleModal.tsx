/**
 * Modal dialog for "Save as Example..." action.
 * Allows naming, categorizing, and describing a circuit to save into the Examples Library.
 */
import { useEffect, useRef, useState } from 'react';

export interface SaveAsExampleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (example: { name: string; category: string; description: string }) => Promise<void>;
  defaultName?: string;
  existingCategories?: string[];
}

const DEFAULT_CATEGORIES = ['Power Electronics', 'Analog & Filter', 'Control & Scripting'];

export function SaveAsExampleModal({
  isOpen,
  onClose,
  onSave,
  defaultName = '',
  existingCategories = DEFAULT_CATEGORIES,
}: SaveAsExampleModalProps) {
  const [name, setName] = useState('');
  const [category, setCategory] = useState('Power Electronics');
  const [description, setDescription] = useState('');
  const [busy, setBusy] = useState(false);
  const nameInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      const clean = (defaultName || 'My Circuit Example').replace(/\.ipes$/i, '');
      setName(clean);
      setCategory(existingCategories[0] || 'Power Electronics');
      setDescription('');
      setBusy(false);
      setTimeout(() => {
        nameInputRef.current?.focus();
        nameInputRef.current?.select();
      }, 50);
    }
  }, [isOpen, defaultName]);

  // Global Escape key listener
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || busy) return;
    setBusy(true);
    try {
      await onSave({
        name: name.trim(),
        category: category.trim() || 'General',
        description: description.trim(),
      });
      onClose();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="modal-card save-as-example-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="save-example-title"
      >
        <div className="modal-header">
          <div className="modal-title-group">
            <span className="modal-icon">💡</span>
            <div>
              <h2 id="save-example-title" className="modal-title">
                Save Circuit as Example
              </h2>
              <div className="examples-modal-subtitle">
                Add this circuit diagram to the Examples Library
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
            <label htmlFor="example-name-input" className="modal-field-label">
              Example Title
            </label>
            <input
              id="example-name-input"
              ref={nameInputRef}
              type="text"
              className="modal-text-input"
              placeholder="e.g., 3-Phase Vienna Rectifier"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />

            <label htmlFor="example-category-input" className="modal-field-label" style={{ marginTop: 8 }}>
              Category
            </label>
            <div className="example-category-input-group">
              <input
                id="example-category-input"
                type="text"
                list="category-suggestions"
                className="modal-text-input"
                placeholder="Choose or type a category..."
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              />
              <datalist id="category-suggestions">
                {existingCategories.map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
            </div>

            <label htmlFor="example-desc-input" className="modal-field-label" style={{ marginTop: 8 }}>
              Description
            </label>
            <textarea
              id="example-desc-input"
              className="modal-text-input example-textarea"
              placeholder="Describe the circuit topology, key waveforms, or learning objectives..."
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />

            <p className="modal-field-hint">
              This circuit will be saved into the <code>examples/</code> directory on disk and can be accessed via <strong>File → Open Example...</strong>.
            </p>
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
              {busy ? 'Saving...' : 'Save as Example'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
