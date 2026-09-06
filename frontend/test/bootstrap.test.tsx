// @vitest-environment jsdom
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BackendStartupError, EngineStartupError, waitForBackend } from '../src/bootstrap';

function okResponse(): Response {
  return { ok: true, status: 200 } as unknown as Response;
}

describe('waitForBackend', () => {
  it('resolves on the first successful health check (same origin)', async () => {
    const fetchFn = vi.fn().mockResolvedValue(okResponse());
    await waitForBackend({ fetchFn, delayFn: async () => {} });
    expect(fetchFn).toHaveBeenCalledTimes(1);
    expect(fetchFn.mock.calls[0][0]).toBe('/gecko/api/health');
    expect(fetchFn.mock.calls[0][1]).toEqual({ cache: 'no-store' });
  });

  it('retries with backoff until the engine answers, using the injected origin', async () => {
    (globalThis as { __GECKO_BACKEND__?: string }).__GECKO_BACKEND__ = 'http://127.0.0.1:54321';
    const fetchFn = vi
      .fn<typeof fetch>()
      .mockRejectedValueOnce(new TypeError('fetch failed'))
      .mockResolvedValueOnce({ ok: false, status: 503 } as unknown as Response)
      .mockResolvedValueOnce(okResponse());
    const delays: number[] = [];
    await waitForBackend({
      fetchFn,
      delayFn: async (ms) => {
        delays.push(ms);
      },
    });
    expect(fetchFn).toHaveBeenCalledTimes(3);
    expect(fetchFn.mock.calls[2][0]).toBe('http://127.0.0.1:54321/gecko/api/health');
    expect(delays).toEqual([500, 1000]);
    delete (globalThis as { __GECKO_BACKEND__?: string }).__GECKO_BACKEND__;
  });

  it('throws BackendStartupError when the deadline passes', async () => {
    const fetchFn = vi.fn<() => Promise<Response>>().mockRejectedValue(new TypeError('fetch failed'));
    await expect(
      waitForBackend({ fetchFn, delayFn: async () => {}, timeoutMs: 0 }),
    ).rejects.toBeInstanceOf(BackendStartupError);
    expect(fetchFn).toHaveBeenCalledTimes(1);
  });
});

describe('EngineStartupError', () => {
  it('shows the failure message and a retry button', () => {
    render(<EngineStartupError message="Engine API not ready after 60 s" />);
    expect(screen.getByText('Simulation engine failed to start')).toBeTruthy();
    expect(screen.getByText(/Engine API not ready after 60 s/)).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeTruthy();
  });
});
