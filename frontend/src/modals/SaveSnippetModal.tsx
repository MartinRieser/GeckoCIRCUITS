/**
 * Modal dialog to name, categorize, and save a schematic selection as a reusable snippet.
 */
import { useEffect, useState } from 'react';

export interface SaveSnippetModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedComponentsCount: number;
  selectedWiresCount: number;
  internalCouplingsCount: number;
  onSave: (meta: { name: string; category: string; description: string }) => void;
}

const SNIPPET_CATEGORIES = ['Power Stages', 'Filters', 'Control', 'Gate Drive', 'Custom'];

export function SaveSnippetModal({
  isOpen,
  onClose,
  selectedComponentsCount,
  selectedWiresCount,
  internalCouplingsCount,
  onSave,
}: SaveSnippetModalProps) {
  const [name, setName] = useState('');
  const [category, setCategory] = useState('Power Stages');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setName('');
      setCategory('Power Stages');
      setDescription('');
      setError('');
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Please enter a snippet name.');
      return;
    }
    onSave({
      name: trimmed,
      category,
      description: description.trim(),
    });
    onClose();
  };

  return (
    <div
      className="modal-backdrop"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="modal-card save-snippet-modal"
        data-testid="save-snippet-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h3 className="modal-title">Save Selection as Snippet</h3>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="save-snippet-form">
          <div className="save-snippet-summary" data-testid="snippet-selection-summary">
            {selectedComponentsCount} {selectedComponentsCount === 1 ? 'component' : 'components'},{' '}
            {selectedWiresCount} {selectedWiresCount === 1 ? 'wire' : 'wires'},{' '}
            {internalCouplingsCount}{' '}
            {internalCouplingsCount === 1 ? 'internal coupling' : 'internal couplings'}
          </div>

          <label className="form-field">
            <span>Name *</span>
            <input
              type="text"
              data-testid="snippet-name-input"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (error) setError('');
              }}
              placeholder="e.g. Half-Bridge Cell"
              autoFocus
            />
          </label>

          <label className="form-field">
            <span>Category</span>
            <select
              data-testid="snippet-category-select"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              {SNIPPET_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span>Description</span>
            <input
              type="text"
              data-testid="snippet-desc-input"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional usage notes..."
            />
          </label>

          {error && (
            <div className="save-snippet-error" role="alert" data-testid="snippet-name-error">
              {error}
            </div>
          )}

          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn-primary" data-testid="confirm-save-snippet-btn">
              Save Snippet
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
