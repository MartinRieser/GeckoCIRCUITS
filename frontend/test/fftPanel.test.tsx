// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, fireEvent, waitFor } from '@testing-library/react';
import { FftPanel, thdPercent } from '../src/simulation/FftPanel';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);

// pure 50 Hz sine sampled at 10 kHz over one visible window of 40 ms
const TIME: number[] = [];
const SINE: number[] = [];
for (let i = 0; i < 400; i++) {
  TIME.push(i * 1e-4);
  SINE.push(100 * Math.sin(2 * Math.PI * 50 * (i * 1e-4)));
}

function fourierResponse() {
  // ideal sine: only the fundamental is nonzero
  return {
    baseFrequency: 25,
    harmonics: 40,
    signalName: 'u_out',
    anCoefficients: new Array(41).fill(0),
    bnCoefficients: new Array(41).fill(0).map((_, i) => (i === 1 ? 100 : 0)),
    cnAmplitudes: new Array(41).fill(0).map((_, i) => (i === 1 ? 100 : 0)),
    jnPhases: new Array(41).fill(0),
    dcComponent: 0,
    fundamentalAmplitude: 100,
    fundamentalPhaseDegrees: 0,
  };
}

afterEach(() => cleanup());

describe('thdPercent', () => {
  it('is zero for a pure fundamental', () => {
    expect(thdPercent([100, 100, 0, 0])).toBe(0);
  });

  it('computes sqrt of harmonic sum over fundamental', () => {
    // harmonics 3 V and 4 V over 100 V fundamental -> 5 %
    expect(thdPercent([100, 100, 3, 4])).toBeCloseTo(5, 6);
  });

  it('guards missing fundamental', () => {
    expect(thdPercent([100])).toBe(0);
    expect(thdPercent([100, 0])).toBe(0);
  });
});

describe('FftPanel', () => {
  beforeEach(() => {
    fetchMock.mockReset();
  });
  afterEach(() => {
    fetchMock.mockReset();
  });

  it('posts the visible window with derived sample rate and renders the spectrum', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: () => Promise.resolve(fourierResponse()),
    });

    render(
      <FftPanel
        time={TIME}
        signals={{ u_out: SINE }}
        activeSignals={['u_out']}
        viewStart={TIME[0]}
        viewEnd={TIME[TIME.length - 1]}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Compute FFT' }));

    await waitFor(() => expect(screen.getByTestId('fft-thd')).toBeTruthy());
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/gecko/api/v1/analysis/fourier?harmonics=40');
    expect(init.method).toBe('POST');
    const body = JSON.parse(init.body);
    expect(body.signalName === undefined || body.signalName === undefined).toBe(true); // raw-data mode
    expect(body.data).toHaveLength(400);
    expect(body.sampleRate).toBeCloseTo(10_000, 6);
    expect(body.startTime).toBeCloseTo(0, 12);
    expect(body.endTime).toBeCloseTo(399e-4, 12);

    expect(screen.getByTestId('fft-thd').textContent).toContain('0.00 %');
    const summary = screen.getByTestId('fft-summary').textContent ?? '';
    expect(summary).toContain('Fundamental');
    expect(summary).toContain('100');
  });

  it('reports server errors', async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 400,
      headers: new Headers(),
      json: () => Promise.reject(new Error('no json')),
    });

    render(
      <FftPanel
        time={TIME}
        signals={{ u_out: SINE }}
        activeSignals={['u_out']}
        viewStart={TIME[0]}
        viewEnd={TIME[TIME.length - 1]}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Compute FFT' }));
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('HTTP 400'));
  });
});
