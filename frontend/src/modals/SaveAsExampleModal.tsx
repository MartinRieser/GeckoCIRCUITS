/**
 * Modal dialog for "Save as Example..." action.
 * Allows naming, categorizing, and describing a circuit to save into the Examples Library,
 * with full support for selecting and overwriting existing examples.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import { loadAllExamples, type ManagedExample } from '../model/examplesService';

export interface SaveAsExampleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (example: {
    id?: string;
    name: string;
    category: string;
    description: string;
    overwrite?: boolean;
  }) => Promise<void>;
  defaultName?: string;
  activeExampleId?: string;
  existingCategories?: string[];
  existingExamples?: ManagedExample[];
}

const DEFAULT_CATEGORIES = ['Power Electronics', 'Analog & Filter', 'Control & Scripting'];

export function SaveAsExampleModal({
  isOpen,
  onClose,
  onSave,
  defaultName = '',
  activeExampleId,
  existingCategories = DEFAULT_CATEGORIES,
  existingExamples: propExamples,
}: SaveAsExampleModalProps) {
  const [name, setName] = useState('');
  const [category, setCategory] = useState('Power Electronics');
  const [description, setDescription] = useState('');
  const [overwrite, setOverwrite] = useState(true);
  const [busy, setBusy] = useState(false);
  const [examplesList, setExamplesList] = useState<ManagedExample[]>(propExamples || []);
  const nameInputRef = useRef<HTMLInputElement>(null);

  // Load available examples when opening
  useEffect(() => {
    if (isOpen) {
      if (propExamples && propExamples.length > 0) {
        setExamplesList(propExamples);
      } else {
        loadAllExamples().then((list) => {
          setExamplesList(list);
        });
      }
    }
  }, [isOpen, propExamples]);

  useEffect(() => {
    if (isOpen) {
      const clean = (defaultName || 'My Circuit Example').replace(/\.ipes$/i, '');
      setName(clean);
      setCategory(existingCategories[0] || 'Power Electronics');
      setDescription('');
      setOverwrite(true);
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

  // Detect whether the current name matches an existing example
  const matchingExample = useMemo(() => {
    const trimmed = name.trim().toLowerCase();
    if (!trimmed) return null;
    return (
      examplesList.find(
        (ex) =>
          (activeExampleId && ex.id === activeExampleId) ||
          ex.name.toLowerCase() === trimmed,
      ) || null
    );
  }, [name, examplesList, activeExampleId]);

  if (!isOpen) return null;

  const handleSelectExisting = (exampleId: string) => {
    if (!exampleId) return;
    const target = examplesList.find((ex) => ex.id === exampleId);
    if (target) {
      setName(target.name);
      setCategory(target.category);
      setDescription(target.description);
      setOverwrite(true);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || busy) return;
    setBusy(true);
    try {
      const isOverwriting = matchingExample ? overwrite : false;
      await onSave({
        id: isOverwriting && matchingExample ? matchingExample.id : undefined,
        name: name.trim(),
        category: category.trim() || 'General',
        description: description.trim(),
        overwrite: isOverwriting,
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
                {matchingExample && overwrite ? 'Overwrite Circuit Example' : 'Save Circuit as Example'}
              </h2>
              <div className="examples-modal-subtitle">
                {matchingExample && overwrite
                  ? `Update "${matchingExample.name}" in the Examples Library`
                  : 'Add this circuit diagram to the Examples Library'}
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
            {/* Quick selector to overwrite an existing example */}
            {examplesList.length > 0 && (
              <div style={{ marginBottom: 12 }}>
                <label htmlFor="example-overwrite-select" className="modal-field-label">
                  Choose Example to Overwrite (Optional)
                </label>
                <select
                  id="example-overwrite-select"
                  className="modal-text-input example-target-select"
                  value={matchingExample?.id || ''}
                  onChange={(e) => handleSelectExisting(e.target.value)}
                >
                  <option value="">-- Create a new example or select below to overwrite --</option>
                  {examplesList.map((ex) => (
                    <option key={ex.id} value={ex.id}>
                      {ex.name} [{ex.category}]
                    </option>
                  ))}
                </select>
              </div>
            )}

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

            {/* Overwrite Banner when matching example found */}
            {matchingExample && (
              <div className="example-overwrite-banner">
                <span className="banner-icon">⚠️</span>
                <div className="banner-text">
                  <div>
                    An example with this title already exists: <strong>{matchingExample.name}</strong>
                  </div>
                  <label className="overwrite-checkbox-label">
                    <input
                      type="checkbox"
                      checked={overwrite}
                      onChange={(e) => setOverwrite(e.target.checked)}
                    />
                    <span>Overwrite existing example file with active schematic</span>
                  </label>
                </div>
              </div>
            )}

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
              {busy
                ? matchingExample && overwrite
                  ? 'Overwriting...'
                  : 'Saving...'
                : matchingExample && overwrite
                  ? 'Overwrite Example'
                  : 'Save as Example'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
