/**
 * FFT spectrum panel for the scope view: harmonic decomposition of the
 * currently visible signal window via the REST analysis endpoint
 * (/analysis/fourier), with spectrum bars, harmonic table, and THD.
 */
import { useState } from 'react';
import { computeFourier, type FourierResult } from '../api/client';
import { formatEngineeringValue } from '../model/componentSchema';

/** Total harmonic distortion from the Cn amplitude series (n >= 2), in %. */
export function thdPercent(cnAmplitudes: number[]): number {
  if (cnAmplitudes.length < 2 || cnAmplitudes[1] <= 0) return 0;
  let sumSquares = 0;
  for (let i = 2; i < cnAmplitudes.length; i++) {
    sumSquares += cnAmplitudes[i] * cnAmplitudes[i];
  }
  return (Math.sqrt(sumSquares) / cnAmplitudes[1]) * 100;
}

export function FftPanel({
  time,
  signals,
  activeSignals,
  viewStart,
  viewEnd,
}: {
  time: number[];
  signals: Record<string, number[]>;
  activeSignals: string[];
  viewStart: number;
  viewEnd: number;
}) {
  const [signalName, setSignalName] = useState(activeSignals[0] ?? '');
  const [harmonics, setHarmonics] = useState(40);
  const [result, setResult] = useState<FourierResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const selected = signals[signalName] ?? signals[activeSignals[0]] ?? [];

  const run = async () => {
    const i0 = Math.max(0, time.findIndex((t) => t >= viewStart));
    let i1 = time.length - 1;
    for (let i = i0; i < time.length; i++) {
      if (time[i] > viewEnd) {
        i1 = i - 1;
        break;
      }
    }
    const window = time.slice(i0, i1 + 1);
    if (window.length < 8) {
      setError('Not enough samples in the visible window');
      return;
    }
    const sampleRate = window.length >= 2 ? (window.length - 1) / (window[window.length - 1] - window[0]) : 0;
    if (sampleRate <= 0) {
      setError('Invalid sample rate in the visible window');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const name = signalName || activeSignals[0] || 'signal';
      setResult(
        await computeFourier(
          { data: (signals[name] ?? selected).slice(i0, i1 + 1), sampleRate, startTime: window[0], endTime: window[window.length - 1] },
          harmonics,
        ),
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const maxCn = result ? Math.max(...result.cnAmplitudes, 1e-12) : 1;

  return (
    <div className="fft-panel" data-testid="fft-panel">
      <div className="fft-controls">
        <select
          aria-label="FFT signal"
          value={signalName}
          onChange={(e) => setSignalName(e.target.value)}
        >
          {activeSignals.map((name) => (
            <option key={name} value={name}>{name}</option>
          ))}
        </select>
        <label>
          harmonics
          <input
            aria-label="FFT harmonics"
            type="number"
            min={1}
            max={500}
            value={harmonics}
            onChange={(e) => setHarmonics(Math.max(1, Number(e.target.value) || 1))}
          />
        </label>
        <button type="button" onClick={() => void run()} disabled={busy}>
          {busy ? 'Computing…' : 'Compute FFT'}
        </button>
      </div>

      {error && <div className="fft-error" role="alert">{error}</div>}

      {result && (
        <div className="fft-result">
          <div className="fft-summary" data-testid="fft-summary">
            <span>Base: {formatEngineeringValue(result.baseFrequency, 'Hz')}</span>
            <span>Fundamental: {formatEngineeringValue(result.fundamentalAmplitude)}</span>
            <span>DC: {formatEngineeringValue(result.dcComponent)}</span>
            <span data-testid="fft-thd">THD: {thdPercent(result.cnAmplitudes).toFixed(2)} %</span>
          </div>

          <svg className="fft-spectrum" viewBox="0 0 500 140" role="img" aria-label="Harmonic spectrum">
            {result.cnAmplitudes.map((amp, idx) => {
              const barW = 500 / result.cnAmplitudes.length - 2;
              const h = (amp / maxCn) * 120;
              return (
                <rect
                  key={idx}
                  x={idx * (500 / result.cnAmplitudes.length) + 1}
                  y={130 - h}
                  width={Math.max(1, barW)}
                  height={Math.max(0, h)}
                  fill={idx === 1 ? '#38bdf8' : '#64748b'}
                />
              );
            })}
          </svg>

          <table className="fft-table">
            <thead>
              <tr>
                <th>n</th>
                <th>f (Hz)</th>
                <th>Amplitude</th>
                <th>% of fund.</th>
                <th>Phase (deg)</th>
              </tr>
            </thead>
            <tbody>
              {result.cnAmplitudes.map((amp, idx) => (
                <tr key={idx}>
                  <td>{idx}</td>
                  <td>{formatEngineeringValue(idx * result.baseFrequency, 'Hz')}</td>
                  <td>{formatEngineeringValue(amp)}</td>
                  <td>{result.fundamentalAmplitude > 0 ? ((amp / result.fundamentalAmplitude) * 100).toFixed(2) : '—'}</td>
                  <td>{result.jnPhases[idx] !== undefined ? result.jnPhases[idx].toFixed(1) : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
