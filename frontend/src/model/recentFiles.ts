/**
 * Recent Files Manager for GeckoCIRCUITS.
 *
 * Persists up to 8 recently opened or saved circuit files in browser localStorage
 * with in-memory fallback. Stores circuit content for instant offline/browser re-opening.
 */

export interface RecentFileEntry {
  id: string;
  name: string;
  content: string;
  timestamp: number;
  componentCount?: number;
}

const STORAGE_KEY = 'gecko-recent-files';
export const MAX_RECENT_FILES = 8;

let memoryCache: RecentFileEntry[] | null = null;

function generateId(name: string): string {
  return `${name}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
}

/**
 * Reads recent files from localStorage with fallback to memory.
 */
export function getRecentFiles(): RecentFileEntry[] {
  if (memoryCache !== null) {
    return memoryCache;
  }
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      memoryCache = [];
      return memoryCache;
    }
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      memoryCache = parsed.slice(0, MAX_RECENT_FILES);
      return memoryCache;
    }
  } catch {
    // localStorage unavailable or corrupted JSON
  }
  memoryCache = [];
  return memoryCache;
}

/**
 * Saves recent files list to localStorage and memory cache.
 */
function persistRecentFiles(entries: RecentFileEntry[]): void {
  memoryCache = entries.slice(0, MAX_RECENT_FILES);
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(memoryCache));
  } catch {
    // quota exceeded or private mode, in-memory cache remains active
  }
}

/**
 * Adds or promotes a circuit file to the top of the recent files list (max 8).
 * If the file already exists by name, its content and timestamp are updated and it
 * moves to the top of the list.
 */
export function addRecentFile(
  name: string,
  content: string,
  componentCount?: number,
): RecentFileEntry[] {
  const cleanName = name?.trim();
  if (!cleanName || !content) {
    return getRecentFiles();
  }

  const current = getRecentFiles();

  // Remove any existing entry with the same name
  const existing = current.find((item) => item.name.toLowerCase() === cleanName.toLowerCase());
  const filtered = current.filter((item) => item.name.toLowerCase() !== cleanName.toLowerCase());

  const newEntry: RecentFileEntry = {
    id: existing ? existing.id : generateId(cleanName),
    name: cleanName,
    content,
    timestamp: Date.now(),
    componentCount: componentCount !== undefined ? componentCount : existing?.componentCount,
  };

  const updated = [newEntry, ...filtered].slice(0, MAX_RECENT_FILES);
  persistRecentFiles(updated);
  return updated;
}

/**
 * Removes a single entry from the recent files list by ID.
 */
export function removeRecentFile(id: string): RecentFileEntry[] {
  const current = getRecentFiles();
  const updated = current.filter((item) => item.id !== id);
  persistRecentFiles(updated);
  return updated;
}

/**
 * Clears all recent files.
 */
export function clearRecentFiles(): void {
  persistRecentFiles([]);
}

/**
 * Formats a unix timestamp into a human-friendly relative time string.
 */
export function formatRelativeTime(timestamp: number): string {
  const now = Date.now();
  const diffSec = Math.max(0, Math.floor((now - timestamp) / 1000));

  if (diffSec < 45) {
    return 'Just now';
  }
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) {
    return `${diffMin}m ago`;
  }
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) {
    return `${diffHours}h ago`;
  }
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) {
    return `${diffDays}d ago`;
  }
  const date = new Date(timestamp);
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
