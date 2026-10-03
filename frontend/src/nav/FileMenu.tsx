/**
 * Desktop-grade File Menu (VSCode / Microsoft Word style).
 * Replaces the top-bar New / Open / Save buttons with a unified menu bar item,
 * featuring cascading flyout submenus for Open Recent (last 8 files) and Built-in Examples.
 */
import { useEffect, useRef, useState, useCallback } from 'react';
import {
  getRecentFiles,
  clearRecentFiles,
  formatRelativeTime,
  type RecentFileEntry,
} from '../model/recentFiles';

export interface FileMenuProps {
  onNew: () => void;
  onOpen: () => void;
  onSave: () => void;
  onSaveAs: () => void;
  onOpenRecent: (entry: RecentFileEntry) => void;
  canSave: boolean;
  busy?: boolean;
}

export function FileMenu({
  onNew,
  onOpen,
  onSave,
  onSaveAs,
  onOpenRecent,
  canSave,
  busy = false,
}: FileMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [recentSubmenuOpen, setRecentSubmenuOpen] = useState(false);
  const [recentList, setRecentList] = useState<RecentFileEntry[]>([]);
  const menuRef = useRef<HTMLDivElement>(null);
  const submenuCloseTimerRef = useRef<number | null>(null);

  const refreshRecent = useCallback(() => {
    setRecentList(getRecentFiles());
  }, []);

  const openMenu = () => {
    refreshRecent();
    setRecentSubmenuOpen(false);
    setIsOpen(true);
  };

  const closeMenu = useCallback(() => {
    setIsOpen(false);
    setRecentSubmenuOpen(false);
  }, []);

  // Click outside and Esc key handlers
  useEffect(() => {
    if (!isOpen) return;

    const handleMouseDown = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        closeMenu();
      }
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        closeMenu();
      }
    };

    document.addEventListener('mousedown', handleMouseDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleMouseDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, closeMenu]);

  const handleRecentMouseEnter = () => {
    if (submenuCloseTimerRef.current !== null) {
      clearTimeout(submenuCloseTimerRef.current);
      submenuCloseTimerRef.current = null;
    }
    setRecentSubmenuOpen(true);
  };

  const handleRecentMouseLeave = () => {
    submenuCloseTimerRef.current = window.setTimeout(() => {
      setRecentSubmenuOpen(false);
    }, 220);
  };

  const handleClearRecent = (e: React.MouseEvent) => {
    e.stopPropagation();
    clearRecentFiles();
    setRecentList([]);
  };

  return (
    <div className="file-menu-wrap" ref={menuRef}>
      <button
        type="button"
        className={`file-menu-btn ${isOpen ? 'active' : ''}`}
        onClick={() => (isOpen ? closeMenu() : openMenu())}
        aria-haspopup="true"
        aria-expanded={isOpen}
        title="File Operations (New, Open, Save, Recent Files)"
      >
        <span>File</span>
        <span className="file-menu-caret">▾</span>
      </button>

      {isOpen && (
        <div className="file-menu-dropdown" role="menu">
          {/* New Circuit */}
          <button
            type="button"
            className="file-menu-item"
            role="menuitem"
            onClick={() => {
              closeMenu();
              onNew();
            }}
          >
            <span className="file-menu-icon">📄</span>
            <span className="file-menu-label">New Circuit</span>
            <span className="file-menu-shortcut">Ctrl+N</span>
          </button>

          {/* Open Local File */}
          <button
            type="button"
            className="file-menu-item"
            role="menuitem"
            disabled={busy}
            onClick={() => {
              closeMenu();
              onOpen();
            }}
          >
            <span className="file-menu-icon">📂</span>
            <span className="file-menu-label">Open...</span>
            <span className="file-menu-shortcut">Ctrl+O</span>
          </button>

          {/* Open Recent Submenu Trigger */}
          <div
            className="file-menu-submenu-wrap"
            onMouseEnter={handleRecentMouseEnter}
            onMouseLeave={handleRecentMouseLeave}
          >
            <button
              type="button"
              className={`file-menu-item has-submenu ${recentSubmenuOpen ? 'hovered' : ''}`}
              role="menuitem"
              aria-haspopup="true"
              aria-expanded={recentSubmenuOpen}
              onClick={() => setRecentSubmenuOpen(!recentSubmenuOpen)}
            >
              <span className="file-menu-icon">🕒</span>
              <span className="file-menu-label">Open Recent</span>
              <span className="file-menu-arrow">▸</span>
            </button>

            {/* Cascading Flyout Submenu: Last 8 Recent Files */}
            {recentSubmenuOpen && (
              <div
                className="file-menu-flyout recent-flyout"
                role="menu"
                onMouseEnter={handleRecentMouseEnter}
                onMouseLeave={handleRecentMouseLeave}
              >
                <div className="file-menu-flyout-header">Recent Circuits (Last 8)</div>

                {recentList.length === 0 ? (
                  <div className="file-menu-empty">No recent files</div>
                ) : (
                  <>
                    {recentList.slice(0, 8).map((file, idx) => (
                      <button
                        key={file.id || `${file.name}_${idx}`}
                        type="button"
                        className="file-menu-recent-item"
                        role="menuitem"
                        onClick={() => {
                          closeMenu();
                          onOpenRecent(file);
                        }}
                        title={file.name}
                      >
                        <span className="recent-index">{idx + 1}.</span>
                        <div className="recent-info">
                          <span className="recent-name">{file.name}</span>
                          <span className="recent-meta">
                            {formatRelativeTime(file.timestamp)}
                            {file.componentCount !== undefined
                              ? ` • ${file.componentCount} comp`
                              : ''}
                          </span>
                        </div>
                      </button>
                    ))}

                    <div className="file-menu-divider" />

                    <button
                      type="button"
                      className="file-menu-item clear-recent-btn"
                      role="menuitem"
                      onClick={handleClearRecent}
                    >
                      <span className="file-menu-icon">🗑️</span>
                      <span className="file-menu-label">Clear Recent Files</span>
                    </button>
                  </>
                )}
              </div>
            )}
          </div>

          <div className="file-menu-divider" />

          {/* Save */}
          <button
            type="button"
            className="file-menu-item"
            role="menuitem"
            disabled={!canSave}
            onClick={() => {
              closeMenu();
              onSave();
            }}
          >
            <span className="file-menu-icon">💾</span>
            <span className="file-menu-label">Save</span>
            <span className="file-menu-shortcut">Ctrl+S</span>
          </button>

          {/* Save As... */}
          <button
            type="button"
            className="file-menu-item"
            role="menuitem"
            disabled={!canSave}
            onClick={() => {
              closeMenu();
              onSaveAs();
            }}
          >
            <span className="file-menu-icon">📥</span>
            <span className="file-menu-label">Save As...</span>
            <span className="file-menu-shortcut">Ctrl+Shift+S</span>
          </button>
        </div>
      )}
    </div>
  );
}
