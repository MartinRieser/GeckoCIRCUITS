/**
 * Desktop-shell bridge (Tauri). Every function degrades to a no-op in the
 * browser, so the same build serves both deployments. The shell installs a
 * queue shim before page scripts run: payloads arriving before the editor
 * registers its handler are buffered in window.__geckoOpenFileQueue.
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

function tauri(): TauriGlobal['core'] | undefined {
  return (globalThis as { __TAURI__?: TauriGlobal }).__TAURI__?.core;
}

export function isDesktop(): boolean {
  return typeof tauri()?.invoke === 'function';
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
 * Native open file dialog. Returns the opened file payload or null on cancel.
 * Returns null if not running in Tauri or user cancelled.
 */
export async function openFileNative(currentPath?: string | null): Promise<OpenFilePayload | null> {
  const invoke = tauri()?.invoke;
  if (!invoke) return null;
  return (await invoke('open_file_dialog', { currentPath: currentPath ?? null })) as OpenFilePayload | null;
}

/**
 * Native save dialog + write. Returns chosen path string, or null on cancel / not on desktop.
 */
export async function saveFileNative(
  base64: string,
  suggestedName: string,
  currentPath?: string | null,
): Promise<string | null> {
  const invoke = tauri()?.invoke;
  if (!invoke) return null;
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

/**
 * Direct save overwriting the file at path without a dialog.
 */
export async function saveFileDirectNative(path: string, base64: string): Promise<boolean> {
  const invoke = tauri()?.invoke;
  if (!invoke) return false;
  await invoke('save_file_direct', { path, base64 });
  return true;
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
