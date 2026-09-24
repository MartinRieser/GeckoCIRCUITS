import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { EngineStartupError, waitForBackend } from './bootstrap';
import './styles.css';

/**
 * Main application bootstrap function: checks engine health and mounts the React application root.
 *
 * @param targetContainer Optional DOM element to mount the root into. Defaults to #root.
 */
export async function start(targetContainer?: HTMLElement | null): Promise<void> {
  const container = targetContainer ?? document.getElementById('root');
  if (!container) {
    console.error('Root container element #root not found in document.');
    return;
  }
  try {
    await waitForBackend();
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    createRoot(container).render(
      <StrictMode>
        <EngineStartupError message={message} />
      </StrictMode>,
    );
    return;
  }
  createRoot(container).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
}

// Auto-start in browser document context
if (typeof document !== 'undefined' && document.getElementById('root')) {
  void start();
}
