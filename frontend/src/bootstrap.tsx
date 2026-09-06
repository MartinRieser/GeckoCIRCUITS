/**
 * Desktop-shell bootstrap: the shell starts the Java engine sidecar with a
 * random port, so the UI must wait until the REST API answers before mounting
 * the editor. The shell injects the backend origin (window.__GECKO_BACKEND__)
 * before any script runs; in browser/dev deployments the health check hits the
 * same origin and resolves on the first attempt.
 */
import { backendOrigin } from './api/client';
import { isDesktop, openLogsFolder } from './desktop';

export const HEALTH_PATH = '/gecko/api/health';

export interface WaitForBackendOptions {
  /** Total time budget before giving up. Default 60 s. */
  timeoutMs?: number;
  /** First retry delay; doubles up to maxDelayMs. Default 500 ms. */
  firstDelayMs?: number;
  maxDelayMs?: number;
  fetchFn?: typeof fetch;
  delayFn?: (ms: number) => Promise<void>;
}

export class BackendStartupError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'BackendStartupError';
  }
}

export async function waitForBackend(options: WaitForBackendOptions = {}): Promise<void> {
  const {
    timeoutMs = 60_000,
    firstDelayMs = 500,
    maxDelayMs = 5_000,
    fetchFn = fetch,
    delayFn = (ms) => new Promise<void>((resolve) => setTimeout(resolve, ms)),
  } = options;
  const deadline = Date.now() + timeoutMs;
  let delay = firstDelayMs;

  for (;;) {
    try {
      const response = await fetchFn(backendOrigin() + HEALTH_PATH, { cache: 'no-store' });
      if (response.ok) {
        return;
      }
    } catch {
      // engine not reachable yet — keep polling
    }
    if (Date.now() + delay >= deadline) {
      throw new BackendStartupError(
        `Engine API not ready after ${Math.round(timeoutMs / 1000)} s (${backendOrigin() || 'same-origin'}${HEALTH_PATH})`,
      );
    }
    await delayFn(delay);
    delay = Math.min(delay * 2, maxDelayMs);
  }
}

/** Full-viewport startup failure screen; points desktop users at the logs. */
export function EngineStartupError({ message }: { message: string }) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        fontFamily: 'system-ui, sans-serif',
        color: '#e5e7eb',
        background: '#111827',
        textAlign: 'center',
        padding: '0 2rem',
      }}
    >
      <h1 style={{ fontSize: '1.25rem', marginBottom: '0.5rem' }}>Simulation engine failed to start</h1>
      <p style={{ maxWidth: '36rem', lineHeight: 1.5 }}>{message}</p>
      <p style={{ maxWidth: '36rem', lineHeight: 1.5, color: '#9ca3af' }}>
        Desktop: open the engine logs below and include them in a bug report,
        then quit the app and start it again.
      </p>
      <div style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem' }}>
        {isDesktop() && (
          <button
            type="button"
            onClick={() => void openLogsFolder()}
            style={{
              padding: '0.5rem 1.25rem',
              cursor: 'pointer',
              borderRadius: 6,
              border: '1px solid #374151',
              background: '#1f2937',
              color: '#e5e7eb',
            }}
          >
            Open engine logs
          </button>
        )}
        <button
          type="button"
          onClick={() => location.reload()}
          style={{
            padding: '0.5rem 1.25rem',
            cursor: 'pointer',
            borderRadius: 6,
            border: '1px solid #374151',
            background: '#1f2937',
            color: '#e5e7eb',
          }}
        >
          Retry
        </button>
      </div>
    </div>
  );
}
