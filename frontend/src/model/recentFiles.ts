/**
 * Recent Files Manager for GeckoCIRCUITS.
 *
 * Persists up to 8 recently opened or saved circuit files in browser localStorage
 * with in-memory fallback. Stores circuit content for instant offline/browser re-opening.
 */

/**
 * Represents a stored circuit file entry in the recent files cache.
 */
export interface RecentFileEntry {
  /** Unique entry identifier. */
  id: string;
  /** Display name of the circuit file (e.g. "buck_converter.ipes"). */
  name: string;
  /** Raw text or Base64 encoded .ipes file content for offline loading. */
  content: string;
  /** Unix timestamp in milliseconds when the file was last opened or saved. */
  timestamp: number;
  /** Optional count of schematic components in the circuit snapshot. */
  componentCount?: number;
}

const STORAGE_KEY = 'gecko-recent-files';
export const MAX_RECENT_FILES = 8;

const SECONDS_JUST_NOW = 45;
const SECONDS_PER_MINUTE = 60;
const MINUTES_PER_HOUR = 60;
const HOURS_PER_DAY = 24;
const DAYS_PER_WEEK = 7;

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
 * Formats a unix timestamp into a human-friendly relative time string (e.g. "Just now", "5m ago").
 *
 * @param timestamp Unix timestamp in milliseconds
 * @returns Human-readable relative time representation
 */
export function formatRelativeTime(timestamp: number): string {
  const now = Date.now();
  const diffSec = Math.max(0, Math.floor((now - timestamp) / 1000));

  if (diffSec < SECONDS_JUST_NOW) {
    return 'Just now';
  }
  const diffMin = Math.floor(diffSec / SECONDS_PER_MINUTE);
  if (diffMin < MINUTES_PER_HOUR) {
    return `${diffMin}m ago`;
  }
  const diffHours = Math.floor(diffMin / MINUTES_PER_HOUR);
  if (diffHours < HOURS_PER_DAY) {
    return `${diffHours}h ago`;
  }
  const diffDays = Math.floor(diffHours / HOURS_PER_DAY);
  if (diffDays < DAYS_PER_WEEK) {
    return `${diffDays}d ago`;
  }
  const date = new Date(timestamp);
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
