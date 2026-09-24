// @vitest-environment jsdom
import { describe, expect, it, vi, afterEach } from 'vitest';
import { cleanup, waitFor, act } from '@testing-library/react';
import * as bootstrap from '../src/bootstrap';
import { start } from '../src/main';

describe('main application bootstrap', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('logs an error and returns when container is null', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    await start(null);
    expect(errorSpy).toHaveBeenCalledWith(expect.stringContaining('Root container element #root not found'));
  });

  it('renders EngineStartupError when waitForBackend fails', async () => {
    vi.spyOn(bootstrap, 'waitForBackend').mockRejectedValueOnce(
      new Error('Engine failed to respond on port 54321'),
    );

    const div = document.createElement('div');
    document.body.appendChild(div);

    await act(async () => {
      await start(div);
    });

    await waitFor(() => {
      expect(div.textContent).toContain('Simulation engine failed to start');
      expect(div.textContent).toContain('Engine failed to respond on port 54321');
    });

    document.body.removeChild(div);
  });

  it('renders App when waitForBackend succeeds', async () => {
    vi.spyOn(bootstrap, 'waitForBackend').mockResolvedValueOnce();

    // Stub global fetch for App's initial catalog loading
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ types: [] }),
    }));
    vi.stubGlobal('WebSocket', class {
      close() {}
      send() {}
    });

    const div = document.createElement('div');
    document.body.appendChild(div);

    await act(async () => {
      await start(div);
    });

    await waitFor(() => {
      expect(div.querySelector('.app')).not.toBeNull();
    });

    document.body.removeChild(div);
  });
});
