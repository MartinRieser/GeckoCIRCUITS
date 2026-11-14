import { describe, it, expect, vi, afterEach } from 'vitest';
import { buildCircuitDiagnostics } from '../src/model/validation';
import { getCatalog, ENGINE_UNREACHABLE_PREFIX, ApiError } from '../src/api/client';

afterEach(() => vi.unstubAllGlobals());

describe('engine unreachable handling', () => {
  it('translates a network failure into an actionable ApiError', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    const err = await getCatalog().catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.message).toContain(ENGINE_UNREACHABLE_PREFIX);
    expect(err.message).toContain('Failed to fetch');
    expect(err.status).toBe(0);
  });

  it('shows a dedicated diagnostic instead of a generic "Simulation Failed"', () => {
    const items = buildCircuitDiagnostics([], [], {
      simErrorMessage: `${ENGINE_UNREACHABLE_PREFIX} at http://x (Failed to fetch).`,
    });
    const item = items.find((i) => i.id === 'engine-unreachable');
    expect(item).toBeDefined();
    expect(item!.title).toBe('Simulation Engine Not Reachable');
    expect(item!.detail).toContain('connection problem');
    expect(items.some((i) => i.id === 'engine-error')).toBe(false);
  });

  it('translates truncated JSON SyntaxError into an actionable ApiError', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: () => Promise.reject(new SyntaxError('Unexpected end of JSON input')),
      }),
    );
    const err = await getCatalog().catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.message).toContain('Invalid or truncated JSON response');
    expect(err.message).toContain('Unexpected end of JSON input');
  });
});
