/**
 * Desktop-shell bridge (Tauri + Chromium File System Access API).
 * Provides native OS Open / Save / Save As dialogs and in-place file saving
 * both inside the Tauri desktop shell and when launched via `run-gecko.bat`
 * (Chrome / Edge `--app` mode on localhost).
 */

export interface OpenFilePayload {
  name: string;
  base64: string;
  path?: string;
}

interface TauriGlobal {
  core?: {
    invoke?: (command: string, args?: Record<string, unknown>) => Promise<unknown>;
  };
}

interface FileSystemWritableStreamLike {
  write: (data: Uint8Array) => Promise<void>;
  close: () => Promise<void>;
}

export interface FileSystemFileHandleLike {
  name: string;
  getFile: () => Promise<File>;
  createWritable: () => Promise<FileSystemWritableStreamLike>;
}

interface WindowWithFS {
  showOpenFilePicker?: (options?: Record<string, unknown>) => Promise<FileSystemFileHandleLike[]>;
  showSaveFilePicker?: (options?: Record<string, unknown>) => Promise<FileSystemFileHandleLike>;
}

let activeFileHandle: FileSystemFileHandleLike | null = null;
const recentFileHandles = new Map<string, FileSystemFileHandleLike>();

function tauri(): TauriGlobal['core'] | undefined {
  return (globalThis as { __TAURI__?: TauriGlobal }).__TAURI__?.core;
}

function fsWindow(): WindowWithFS | undefined {
  return typeof window !== 'undefined' ? (window as unknown as WindowWithFS) : undefined;
}

function bytesToBase64(bytes: Uint8Array): string {
  let binary = '';
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
}

function base64ToBytes(base64: string): Uint8Array {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

export function isDesktop(): boolean {
  return typeof tauri()?.invoke === 'function';
}

/**
 * True when either the Tauri desktop shell OR the Chromium File System Access API
 * (`showOpenFilePicker` / `showSaveFilePicker`, used in Edge/Chrome `--app` mode)
 * is available for native OS file dialogs.
 */
export function hasNativeFileSystemAccess(): boolean {
  if (isDesktop()) return true;
  const win = fsWindow();
  return (
    typeof win?.showOpenFilePicker === 'function' &&
    typeof win?.showSaveFilePicker === 'function'
  );
}

/**
 * True when `Save` (Ctrl+S) can overwrite an existing file on disk directly
 * without opening a Save As dialog.
 */
export function hasActiveFileTarget(filePath?: string | null): boolean {
  if (isDesktop() && filePath) return true;
  return activeFileHandle !== null;
}

/**
 * Clears any active FileSystemFileHandle (e.g., when creating a New Circuit
 * or loading a built-in example).
 */
export function clearActiveFileHandle(): void {
  activeFileHandle = null;
}

/** Registers the handler for circuits the OS opened (double-click etc.) and
 *  drains anything the shell queued before the editor was ready. */
export function registerOpenFileHandler(handler: (payload: OpenFilePayload) => void): void {
  const scope = globalThis as {
    __geckoOpenFileHandler?: (payload: OpenFilePayload) => void;
    __geckoOpenFileQueue?: OpenFilePayload[];
  };
  scope.__geckoOpenFileHandler = handler;
  const queued = scope.__geckoOpenFileQueue ?? [];
  scope.__geckoOpenFileQueue = [];
  for (const payload of queued) {
    handler(payload);
  }
}

/**
 * Native open file dialog (Tauri or File System Access API).
 * Returns the opened file payload or null on cancel / unsupported environment.
 */
export async function openFileNative(currentPath?: string | null): Promise<OpenFilePayload | null> {
  const invoke = tauri()?.invoke;
  if (invoke) {
    return (await invoke('open_file_dialog', { currentPath: currentPath ?? null })) as OpenFilePayload | null;
  }

  const win = fsWindow();
  if (typeof win?.showOpenFilePicker === 'function') {
    try {
      const handles = await win.showOpenFilePicker({
        multiple: false,
        types: [
          {
            description: 'GeckoCIRCUITS Circuit (*.ipes, *.txt)',
            accept: {
              'application/x-gecko-ipes': ['.ipes', '.txt'],
            },
          },
        ],
      });
      const handle = handles?.[0];
      if (!handle) return null;
      const file = await handle.getFile();
      const buffer = await file.arrayBuffer();
      const base64 = bytesToBase64(new Uint8Array(buffer));
      activeFileHandle = handle;
      recentFileHandles.set(file.name.toLowerCase(), handle);
      return {
        name: file.name,
        base64,
        path: file.name,
      };
    } catch (err) {
      if ((err as { name?: string })?.name === 'AbortError') {
        return null;
      }
      throw err;
    }
  }

  return null;
}

/**
 * Native save dialog + write (Tauri or File System Access API).
 * Returns chosen path/filename string, or null on cancel / unsupported environment.
 */
export async function saveFileNative(
  base64: string,
  suggestedName: string,
  currentPath?: string | null,
): Promise<string | null> {
  const invoke = tauri()?.invoke;
  if (invoke) {
    const args: Record<string, unknown> = {
      base64,
      suggestedName,
    };
    if (currentPath !== undefined && currentPath !== null) {
      args.currentPath = currentPath;
    }
    const result = await invoke('save_file_dialog', args);
    return (result as string | null) ?? null;
  }

  const win = fsWindow();
  if (typeof win?.showSaveFilePicker === 'function') {
    try {
      const cleanSuggested = suggestedName.endsWith('.ipes')
        ? suggestedName
        : `${suggestedName}.ipes`;
      const handle = await win.showSaveFilePicker({
        suggestedName: cleanSuggested,
        types: [
          {
            description: 'GeckoCIRCUITS Circuit (*.ipes)',
            accept: {
              'application/x-gecko-ipes': ['.ipes'],
            },
          },
        ],
      });
      if (!handle) return null;
      const bytes = base64ToBytes(base64);
      const writable = await handle.createWritable();
      await writable.write(bytes);
      await writable.close();
      activeFileHandle = handle;
      recentFileHandles.set(handle.name.toLowerCase(), handle);
      return handle.name;
    } catch (err) {
      if ((err as { name?: string })?.name === 'AbortError') {
        return null;
      }
      throw err;
    }
  }

  return null;
}

/**
 * Direct save overwriting the file at path (or active FileSystemFileHandle) without a dialog.
 */
export async function saveFileDirectNative(path: string, base64: string): Promise<boolean> {
  const invoke = tauri()?.invoke;
  if (invoke) {
    await invoke('save_file_direct', { path, base64 });
    return true;
  }

  if (activeFileHandle) {
    const bytes = base64ToBytes(base64);
    const writable = await activeFileHandle.createWritable();
    await writable.write(bytes);
    await writable.close();
    return true;
  }

  return false;
}

/**
 * Reads an existing `.ipes` file from disk when re-opening a Recent File.
 * Uses Tauri's `read_ipes_file` command or a cached `FileSystemFileHandle`.
 */
export async function readIpesFileNative(
  path?: string | null,
  fallbackName?: string,
): Promise<OpenFilePayload | null> {
  const invoke = tauri()?.invoke;
  if (invoke && path) {
    try {
      return (await invoke('read_ipes_file', { path })) as OpenFilePayload | null;
    } catch {
      return null;
    }
  }

  const key = (fallbackName || path || '').toLowerCase();
  const handle = recentFileHandles.get(key);
  if (handle) {
    try {
      const file = await handle.getFile();
      const buffer = await file.arrayBuffer();
      const base64 = bytesToBase64(new Uint8Array(buffer));
      activeFileHandle = handle;
      return {
        name: file.name,
        base64,
        path: file.name,
      };
    } catch {
      return null;
    }
  }

  return null;
}

/**
 * Synchronizes the native desktop window title.
 */
export async function setWindowTitleNative(title: string): Promise<void> {
  const invoke = tauri()?.invoke;
  if (!invoke) return;
  try {
    await invoke('set_window_title', { title });
  } catch {
    // fallback gracefully
  }
}

/**
 * Displays a native confirmation dialog box.
 */
export async function confirmDialogNative(message: string, title?: string): Promise<boolean> {
  const invoke = tauri()?.invoke;
  if (!invoke) {
    try {
      return window.confirm(message);
    } catch {
      return true;
    }
  }
  try {
    return (await invoke('confirm_dialog', { message, title: title ?? 'GeckoCIRCUITS' })) as boolean;
  } catch {
    try {
      return window.confirm(message);
    } catch {
      return true;
    }
  }
}

/** Opens the engine-log folder via the shell (Help/troubleshooting). Returns
 *  false in the browser where there is nothing to open. */
export async function openLogsFolder(): Promise<boolean> {
  const invoke = tauri()?.invoke;
  if (!invoke) {
    return false;
  }
  await invoke('open_logs_folder');
  return true;
}

/**
 * Reveals the examples folder in the host OS desktop file explorer.
 * Works both in Tauri desktop mode and via the backend REST API in Chrome/Edge --app mode.
 */
export async function openExamplesFolder(): Promise<boolean> {
  const invoke = tauri()?.invoke;
  if (invoke) {
    try {
      await invoke('open_examples_folder');
      return true;
    } catch (err) {
      console.warn('Failed to open examples folder via Tauri:', err);
    }
  }

  // Fallback to backend REST endpoint for --app mode
  try {
    const res = await fetch('/gecko/api/v1/circuits/open-examples-folder', {
      method: 'POST',
    });
    if (res.ok) {
      return true;
    }
  } catch {
    // Backend may not be running or remote
  }

  return false;
}

