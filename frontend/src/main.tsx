import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { EngineStartupError, waitForBackend } from './bootstrap';
import './styles.css';

async function start(): Promise<void> {
  const container = document.getElementById('root')!;
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

void start();
