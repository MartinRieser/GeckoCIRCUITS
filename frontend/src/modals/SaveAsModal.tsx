/**
 * Modal dialog for "Save As..." circuit action.
 * Prompts the user for a new file name with .ipes extension validation,
 * Enter to save, and Escape to cancel.
 */
import { useEffect, useRef, useState } from 'react';

const INPUT_FOCUS_DELAY_MS = 50;

/**
 * Properties for the {@link SaveAsModal} dialog.
 */
export interface SaveAsModalProps {
  /** Whether the modal is currently displayed. */
  isOpen: boolean;
  /** Initial suggested file name (defaults to active circuit name or Untitled.ipes). */
  defaultFilename: string;
  /** Callback invoked when the user confirms save with a valid file name. */
  onSave: (filename: string) => void;
  /** Callback invoked when the user cancels or closes the dialog. */
  onClose: () => void;
}

export function SaveAsModal({
  isOpen,
  defaultFilename,
  onSave,
  onClose,
}: SaveAsModalProps) {
  const [filename, setFilename] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      const initial = defaultFilename?.trim() || 'Untitled.ipes';
      setFilename(initial);
      setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus();
          const dotIdx = initial.lastIndexOf('.');
          if (dotIdx > 0) {
            inputRef.current.setSelectionRange(0, dotIdx);
          } else {
            inputRef.current.select();
          }
        }
      }, INPUT_FOCUS_DELAY_MS);
    }
  }, [isOpen, defaultFilename]);

  if (!isOpen) return null;

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    const clean = filename.trim();
    if (!clean) return;
    const finalName = clean.endsWith('.ipes') ? clean : `${clean}.ipes`;
    onSave(finalName);
    onClose();
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
        className="modal-card save-as-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="save-as-title"
      >
        <div className="modal-header">
          <div className="modal-title-group">
            <span className="modal-icon">💾</span>
            <h2 id="save-as-title" className="modal-title">Save Circuit As</h2>
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
            <label htmlFor="save-as-filename" className="modal-field-label">
              File Name
            </label>
            <div className="save-as-input-wrap">
              <input
                id="save-as-filename"
                ref={inputRef}
                type="text"
                className="modal-text-input"
                value={filename}
                onChange={(e) => setFilename(e.target.value)}
                placeholder="circuit.ipes"
                autoComplete="off"
                spellCheck={false}
              />
            </div>
            <p className="modal-field-hint">
              The file will be saved with the standard GeckoCIRCUITS <code>.ipes</code> extension.
            </p>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="modal-btn secondary"
              onClick={onClose}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="modal-btn primary"
              disabled={!filename.trim()}
            >
              Save Circuit
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
