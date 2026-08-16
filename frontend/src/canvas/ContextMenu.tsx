/**
 * Floating Context Menu for right-click actions on schematic components,
 * wires, and canvas workspace.
 */
import { useEffect, useRef } from 'react';

export interface ContextMenuTarget {
  type: 'component' | 'wire' | 'canvas';
  name?: string;
  wireIndex?: number;
  gridX: number;
  gridY: number;
}

export interface ContextMenuProps {
  x: number;
  y: number;
  target: ContextMenuTarget;
  onClose: () => void;
  onRotate?: (name: string) => void;
  onDeleteComponent?: (name: string) => void;
  onDeleteWire?: (index: number) => void;
  onLabelWire?: (index: number) => void;
  onOpenProperties?: (name: string) => void;
  onToggleWireMode?: () => void;
  onOpenCommandPalette?: () => void;
  onZoomFit?: () => void;
}

export function ContextMenu({
  x,
  y,
  target,
  onClose,
  onRotate,
  onDeleteComponent,
  onDeleteWire,
  onLabelWire,
  onOpenProperties,
  onToggleWireMode,
  onOpenCommandPalette,
  onZoomFit,
}: ContextMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    window.addEventListener('mousedown', handleClickOutside);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('mousedown', handleClickOutside);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose]);

  return (
    <div
      ref={menuRef}
      className="context-menu"
      style={{ left: `${x}px`, top: `${y}px` }}
      onClick={(e) => e.stopPropagation()}
    >
      {target.type === 'component' && target.name && (
        <>
          <div className="context-menu-header">{target.name}</div>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              onRotate?.(target.name!);
              onClose();
            }}
          >
            <span>Rotate 90°</span>
            <kbd>R</kbd>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              onOpenProperties?.(target.name!);
              onClose();
            }}
          >
            <span>Properties</span>
            <kbd>2x Click</kbd>
          </button>
          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item danger"
            onClick={() => {
              onDeleteComponent?.(target.name!);
              onClose();
            }}
          >
            <span>Delete</span>
            <kbd>Del</kbd>
          </button>
        </>
      )}

      {target.type === 'wire' && target.wireIndex !== undefined && (
        <>
          <div className="context-menu-header">Wire #{target.wireIndex}</div>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              onLabelWire?.(target.wireIndex!);
              onClose();
            }}
          >
            <span>Net Label...</span>
          </button>
          <button
            type="button"
            className="context-menu-item danger"
            onClick={() => {
              onDeleteWire?.(target.wireIndex!);
              onClose();
            }}
          >
            <span>Delete Wire</span>
            <kbd>Del</kbd>
          </button>
        </>
      )}

      {target.type === 'canvas' && (
        <>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              onToggleWireMode?.();
              onClose();
            }}
          >
            <span>Wire Tool</span>
            <kbd>W</kbd>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              onOpenCommandPalette?.();
              onClose();
            }}
          >
            <span>Add Component...</span>
            <kbd>Ctrl+K</kbd>
          </button>
          <div className="context-menu-sep" />
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              onZoomFit?.();
              onClose();
            }}
          >
            <span>Zoom to Fit</span>
          </button>
        </>
      )}
    </div>
  );
}
