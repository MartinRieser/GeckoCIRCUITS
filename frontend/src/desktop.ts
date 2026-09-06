/**
 * Desktop-shell bridge (Tauri). Every function degrades to a no-op in the
 * browser, so the same build serves both deployments. The shell installs a
 * queue shim before page scripts run: payloads arriving before the editor
 * registers its handler are buffered in window.__geckoOpenFileQueue.
 */

export interface OpenFilePayload {
  name: string;
  base64: string;
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

/** Native save dialog + write. Returns false when not on desktop or when the
 *  user cancels the dialog; the caller can then fall back to browser download. */
export async function saveFileNative(base64: string, suggestedName: string): Promise<boolean> {
  const invoke = tauri()?.invoke;
  if (!invoke) {
    return false;
  }
  const result = await invoke('save_file_dialog', { base64, suggestedName });
  return result != null;
}
