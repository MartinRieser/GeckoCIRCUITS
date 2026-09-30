/**
 * Waveform Power & Semiconductor Loss Analyzer for Oscilloscope Scope View.
 *
 * Computes:
 * - Real Instantaneous Power waveform: p(t) = v(t) * i(t)
 * - Average Power Dissipation: P_avg = (1 / Δt) * ∫ v(t) * i(t) dt
 * - Total Dissipated Energy: E_loss = ∫ v(t) * i(t) dt
 * - Peak Switching Power: max(p(t))
 * - Semiconductor Loss Decomposition: Conduction Loss (P_cond) vs. Switching Loss (P_sw)
 * - Electrical parameters: V_rms, I_rms, Apparent Power S, Power Factor PF
 * - Analysis window: full span or between Cursors A and B
 * - Datasheet semiconductor model calculation via REST API (/loss/switching & /loss/conduction).
 */
import { useState, useMemo, useEffect } from 'react';
import { formatEngineeringValue } from '../model/componentSchema';
import type { EditorComponent } from '../model/types';
import { LkComponentType } from '../model/constants';

export interface LossPanelProps {
  time?: number[];
  signals?: Record<string, number[]>;
  activeSignals?: string[];
  cursorA?: number | null;
  cursorB?: number | null;
  colorOf?: (name: string) => string;
  components?: EditorComponent[];
}

interface LossResponseShape {
  totalLoss: number;
  switchingLoss: number | null;
  conductionLoss: number | null;
  method: string;
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    let detail = '';
    try {
      const errJson = await response.json();
      if (errJson.fieldErrors && typeof errJson.fieldErrors === 'object') {
        const errorList = Object.entries(errJson.fieldErrors)
          .map(([k, v]) => `${k} (${v})`)
          .join(', ');
        detail = `: ${errorList}`;
      } else if (errJson.message) {
        detail = `: ${errJson.message}`;
      }
    } catch {
      // ignore
    }
    throw new Error(`HTTP ${response.status}${detail}`);
  }
  return (await response.json()) as T;
}

function numField(
  label: string,
  value: number,
  onChange: (v: number) => void,
  step = 'any',
): React.ReactNode {
  return (
    <label key={label}>
      {label}
      <input
        type="number"
        step={step}
        value={Number.isFinite(value) ? value : ''}
        onChange={(e) => onChange(Number(e.target.value))}
      />
    </label>
  );
}

function isSemiconductorComponent(c: EditorComponent): boolean {
  const t = Number(c.type);
  const n = (c.name || '').toUpperCase();
  return (
    t === LkComponentType.DIODE ||
    t === LkComponentType.IDEAL_SWITCH ||
    t === LkComponentType.THYRISTOR ||
    t === LkComponentType.IGBT ||
    t === LkComponentType.MOSFET ||
    t === LkComponentType.BJT ||
    n.startsWith('D.') ||
    n.startsWith('D_') ||
    n.startsWith('S.') ||
    n.startsWith('S_') ||
    n.startsWith('TH.') ||
    n.startsWith('TH_') ||
    n.startsWith('IGBT') ||
    n.startsWith('MOS') ||
    n.startsWith('Q.') ||
    n.startsWith('Q_')
  );
}

function detectDefaultSignals(
  activeSignalsList: string[],
  allSignalsList?: string[],
): { defaultV: string; defaultI: string } {
  let defaultV = '';
  let defaultI = '';

  for (const s of activeSignalsList) {
    const lower = s.toLowerCase();
    if (!defaultV && (lower.startsWith('v') || lower.startsWith('u') || lower.includes('_v') || lower.includes('volt'))) {
      defaultV = s;
    }
    if (!defaultI && (lower.startsWith('i') || lower.includes('_i') || lower.includes('curr') || lower.includes('amp'))) {
      defaultI = s;
    }
  }

  if (!defaultV && activeSignalsList.length > 0) {
    defaultV = activeSignalsList[0];
  }

  // If no current signal was found in active scope channels, search across all available simulation signals
  if (!defaultI && allSignalsList) {
    for (const s of allSignalsList) {
      const lower = s.toLowerCase();
      if (lower.startsWith('i') || lower.includes('_i') || lower.includes('curr') || lower.includes('amp')) {
        defaultI = s;
        break;
      }
    }
  }

  return { defaultV, defaultI };
}

export function LossPanel({
  time,
  signals,
  activeSignals,
  cursorA,
  cursorB,
  colorOf,
  components,
}: LossPanelProps = {}) {
  const availableSignals = useMemo(() => {
    if (activeSignals && activeSignals.length > 0) return activeSignals;
    if (signals) return Object.keys(signals);
    return [];
  }, [activeSignals, signals]);

  const allResultSignals = useMemo(() => {
    return signals ? Object.keys(signals) : [];
  }, [signals]);

  const { defaultV, defaultI } = useMemo(
    () => detectDefaultSignals(availableSignals, allResultSignals),
    [availableSignals, allResultSignals],
  );

  const [voltageSignal, setVoltageSignal] = useState<string>(defaultV);
  const [currentSignal, setCurrentSignal] = useState<string>(defaultI);

  // Sync default signals when available signals change
  useEffect(() => {
    if ((!voltageSignal || !availableSignals.includes(voltageSignal)) && defaultV) {
      setVoltageSignal(defaultV);
    }
    if ((!currentSignal || (!availableSignals.includes(currentSignal) && !allResultSignals.includes(currentSignal))) && defaultI) {
      setCurrentSignal(defaultI);
    }
  }, [availableSignals, allResultSignals, defaultV, defaultI, voltageSignal, currentSignal]);

  const hasCursors = cursorA !== null && cursorB !== null && cursorA !== undefined && cursorB !== undefined;
  const [analysisRange, setAnalysisRange] = useState<'cursor' | 'all'>('all');

  // If cursors exist and user hasn't explicitly set range, default to cursor range
  useEffect(() => {
    if (hasCursors) {
      setAnalysisRange('cursor');
    }
  }, [hasCursors]);

  // Detect semiconductors in the schematic
  const semiconductorComponents = useMemo(() => {
    if (!components || components.length === 0) return [];
    return components.filter(isSemiconductorComponent);
  }, [components]);

  const isPassiveCircuit = components !== undefined && components.length > 0 && semiconductorComponents.length === 0;

  // Real-time instantaneous power and loss calculations from waveforms
  const waveformMetrics = useMemo(() => {
    if (!time || time.length < 2 || !signals || !voltageSignal || !currentSignal) {
      return null;
    }
    const vArr = signals[voltageSignal];
    const iArr = signals[currentSignal];
    if (!vArr || !iArr || vArr.length < 2 || iArr.length < 2) {
      return null;
    }

    let i0 = 0;
    let i1 = Math.min(time.length, vArr.length, iArr.length) - 1;

    if (analysisRange === 'cursor' && hasCursors && cursorA !== null && cursorB !== null) {
      i0 = Math.max(0, Math.min(cursorA, cursorB));
      i1 = Math.min(i1, Math.max(cursorA, cursorB));
    }

    if (i1 <= i0) return null;

    const tStart = time[i0];
    const tEnd = time[i1];
    const dtTotal = tEnd - tStart;
    if (dtTotal <= 0) return null;

    let totalEnergy = 0;
    let vSumSq = 0;
    let iSumSq = 0;
    let pMax = -Infinity;
    let pMin = Infinity;
    let vMax = -Infinity;
    let iMax = -Infinity;

    for (let k = i0; k <= i1; k++) {
      const v = vArr[k] ?? 0;
      const i = iArr[k] ?? 0;
      const p = v * i;
      if (p > pMax) pMax = p;
      if (p < pMin) pMin = p;
      if (Math.abs(v) > vMax) vMax = Math.abs(v);
      if (Math.abs(i) > iMax) iMax = Math.abs(i);
    }

    // Heuristic thresholds for conduction loss vs. switching transitions
    const vCondThresh = Math.max(1.5, vMax * 0.12);
    const iCondThresh = Math.max(0.05, iMax * 0.05);

    let condEnergy = 0;
    const powerSamples: { t: number; p: number }[] = [];
    const decimation = Math.max(1, Math.floor((i1 - i0) / 240));

    for (let k = i0 + 1; k <= i1; k++) {
      const dt = time[k] - time[k - 1];
      const vPrev = vArr[k - 1] ?? 0;
      const vCurr = vArr[k] ?? 0;
      const iPrev = iArr[k - 1] ?? 0;
      const iCurr = iArr[k] ?? 0;

      const pPrev = vPrev * iPrev;
      const pCurr = vCurr * iCurr;
      const avgP = (pPrev + pCurr) / 2;
      const dE = avgP * dt;

      totalEnergy += dE;
      vSumSq += ((vPrev * vPrev + vCurr * vCurr) / 2) * dt;
      iSumSq += ((iPrev * iPrev + iCurr * iCurr) / 2) * dt;

      // On-state conduction detection
      const isConducting = Math.abs(vCurr) < vCondThresh && Math.abs(iCurr) > iCondThresh;
      if (isConducting && dE > 0) {
        condEnergy += dE;
      }

      if ((k - i0) % decimation === 0 || k === i1) {
        powerSamples.push({ t: time[k], p: pCurr });
      }
    }

    const pAvg = totalEnergy / dtTotal;
    const pCond = Math.min(Math.max(0, pAvg), condEnergy / dtTotal);
    const pSw = Math.max(0, pAvg - pCond);
    const vRms = Math.sqrt(Math.max(0, vSumSq / dtTotal));
    const iRms = Math.sqrt(Math.max(0, iSumSq / dtTotal));
    const apparentPower = vRms * iRms;
    const powerFactor = apparentPower > 1e-9 ? Math.min(1, Math.abs(pAvg) / apparentPower) : 1;

    return {
      tStart,
      tEnd,
      dtTotal,
      totalEnergy,
      pAvg,
      pCond,
      pSw,
      pMax,
      pMin,
      vRms,
      iRms,
      apparentPower,
      powerFactor,
      powerSamples,
      vMax,
      iMax,
    };
  }, [time, signals, voltageSignal, currentSignal, analysisRange, hasCursors, cursorA, cursorB]);

  // Datasheet theoretical model inputs (REST API)
  const [eon, setEon] = useState(0.5); // mJ
  const [eoff, setEoff] = useState(0.6); // mJ
  const [fsw, setFsw] = useState(20_000); // Hz
  const [iSw, setISw] = useState(10); // A
  const [vSw, setVSw] = useState(400); // V
  const [tj, setTj] = useState(125); // degC
  const [vRef, setVRef] = useState(400); // V
  const [iCond, setICond] = useState(10); // A
  const [rOn, setROn] = useState(0.01); // Ohm
  const [vTh, setVTh] = useState(0.7); // V
  const [duty, setDuty] = useState(0.5);

  const [result, setResult] = useState<LossResponseShape | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const handleAutoFillFromWaveforms = () => {
    if (!waveformMetrics) return;
    setVSw(Math.round(waveformMetrics.vMax * 10) / 10 || 400);
    setISw(Math.round(waveformMetrics.iMax * 10) / 10 || 10);
    setICond(Math.round(waveformMetrics.iRms * 10) / 10 || 10);
    if (waveformMetrics.dtTotal > 0 && waveformMetrics.dtTotal < 1) {
      setFsw(Math.max(100, Math.round(1 / waveformMetrics.dtTotal)));
    }
    if (waveformMetrics.pAvg > 0) {
      const estimatedDuty = Math.max(0.05, Math.min(0.95, waveformMetrics.pCond / waveformMetrics.pAvg));
      setDuty(Math.round(estimatedDuty * 100) / 100);
    }
  };

  const compute = async () => {
    setBusy(true);
    setError(null);
    try {
      // Validate inputs required by backend @Positive annotations
      if (eon <= 0 || eoff <= 0) {
        throw new Error('Turn-on energy (Eon) and turn-off energy (Eoff) must be greater than 0.');
      }
      if (vSw <= 0 || vRef <= 0) {
        throw new Error('Blocking voltage (V_block) and reference voltage (V_ref) must be greater than 0.');
      }
      if (iSw <= 0) {
        throw new Error('Switching current (I_sw) must be greater than 0.');
      }
      if (fsw <= 0) {
        throw new Error('Switching frequency (f_sw) must be greater than 0.');
      }
      if (rOn <= 0) {
        throw new Error('On-state resistance (R_on) must be greater than 0.');
      }

      // 1. Calculate switching energy per event using voltage scaling: E_sw = (Eon + Eoff) * (V_sw / V_ref)
      const swPromise = postJson<LossResponseShape>('/gecko/api/v1/loss/switching', {
        current: iSw,
        voltage: vSw,
        temperature: tj,
        referenceVoltage: vRef,
        turnOnEnergy: eon / 1000, // convert mJ to J
        turnOffEnergy: eoff / 1000, // convert mJ to J
      });

      // 2. Calculate continuous conduction power: P_cond = I * (Vth + I * Ron)
      const condPromise = postJson<LossResponseShape>('/gecko/api/v1/loss/conduction', {
        current: iCond,
        onResistance: rOn,
        thresholdVoltage: vTh,
      });

      const [swResponse, condResponse] = await Promise.all([swPromise, condPromise]);

      // swResponse.totalLoss is switching energy per cycle [J]
      const eSwitchingJ = swResponse.totalLoss ?? 0;
      // Average switching power: P_sw = E_sw * f_sw [W]
      const pSw = eSwitchingJ * fsw;

      // condResponse.totalLoss is continuous conduction power [W]
      // Average conduction power scaled by duty cycle: P_cond = P_continuous * duty [W]
      const validDuty = Math.max(0, Math.min(1, duty));
      const pCond = (condResponse.totalLoss ?? 0) * validDuty;

      const pTotal = pSw + pCond;

      setResult({
        totalLoss: pTotal,
        switchingLoss: pSw,
        conductionLoss: pCond,
        method: `analytical (f_sw=${formatEngineeringValue(fsw, 'Hz')}, duty=${(validDuty * 100).toFixed(0)}%)`,
      });
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  // Check if current probe is available
  const hasCurrentSignal = Boolean(currentSignal && (availableSignals.includes(currentSignal) || allResultSignals.includes(currentSignal)));

  return (
    <div className="loss-panel" data-testid="loss-panel">
      {/* Circuit Context Awareness Banner */}
      {isPassiveCircuit && (
        <div className="loss-circuit-notice passive">
          <div className="loss-notice-icon">💡</div>
          <div className="loss-notice-content">
            <strong>Passive Circuit Detected (No semiconductor switches in schematic)</strong>
            <p>
              This circuit consists of passive components (resistors, inductors, capacitors).
              Use the <strong>Waveform Power Analyzer</strong> below to compute real instantaneous power dissipation{' '}
              <span className="formula">p(t) = v(t) · i(t)</span>, RMS values, and cycle energy.
            </p>
            <p className="loss-notice-hint">
              <em>Tip: Semiconductor switching loss models (E_on, E_off) apply to switched-mode converters (such as the <strong>DC-DC Buck Converter</strong> example) where transistor switching transients produce loss.</em>
            </p>
          </div>
        </div>
      )}

      {semiconductorComponents.length > 0 && (
        <div className="loss-circuit-notice active">
          <div className="loss-notice-icon">⚡</div>
          <div className="loss-notice-content">
            <strong>Power Electronics Circuit Detected ({semiconductorComponents.map((c) => c.name).join(', ')})</strong>
            <p>
              Analyze switching transitions and conduction intervals directly from waveforms, or calculate total dissipation using the datasheet model below.
            </p>
          </div>
        </div>
      )}

      {/* Scope Waveform Power Analysis Section */}
      <div className="loss-waveform-section">
        <div className="loss-header-row">
          <div className="loss-section-title">
            <span className="loss-title-icon">⚡</span>
            <div>
              <strong>Waveform Power & Loss Analysis</strong>
              <span className="loss-subtitle">
                Calculates instantaneous power p(t) = v(t) · i(t), average dissipation, and cycle energy
              </span>
            </div>
          </div>

          {/* Probe Selectors */}
          <div className="loss-selectors-bar">
            <label className="loss-select-label">
              <span>Voltage Probe v(t):</span>
              <select
                value={voltageSignal}
                onChange={(e) => setVoltageSignal(e.target.value)}
                className="loss-probe-select"
                style={{ borderLeft: `3px solid ${colorOf ? colorOf(voltageSignal) : '#38bdf8'}` }}
              >
                {availableSignals.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </label>

            <label className="loss-select-label">
              <span>Current Probe i(t):</span>
              <select
                value={currentSignal}
                onChange={(e) => setCurrentSignal(e.target.value)}
                className="loss-probe-select"
                style={{ borderLeft: `3px solid ${colorOf ? colorOf(currentSignal) : '#4ade80'}` }}
              >
                {!currentSignal && <option value="">-- Select Current Trace --</option>}
                <optgroup label="Scope Channels">
                  {availableSignals.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </optgroup>
                {allResultSignals.some((s) => !availableSignals.includes(s)) && (
                  <optgroup label="Other Simulation Traces">
                    {allResultSignals.filter((s) => !availableSignals.includes(s)).map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </optgroup>
                )}
              </select>
            </label>
          </div>
        </div>

        {/* Missing Current Signal Guidance */}
        {!hasCurrentSignal && (
          <div className="loss-warning-box">
            <strong>⚠️ Current probe i(t) not selected</strong>
            <p>
              An oscilloscope calculates power using the product of voltage and current: <span className="formula">p(t) = v(t) · i(t)</span>.
              The active scope currently displays only voltage probes.
              To measure power dissipation (e.g. across a resistor or switch), select a current signal in the dropdown above or connect an Ammeter in series in your schematic.
            </p>
          </div>
        )}

        {/* Analysis Range Mode */}
        <div className="loss-range-bar">
          <span className="loss-range-label">Analysis Interval:</span>
          <div className="loss-range-pills">
            <button
              type="button"
              className={`loss-range-pill ${analysisRange === 'cursor' ? 'active' : ''}`}
              onClick={() => setAnalysisRange('cursor')}
              disabled={!hasCursors}
              title={hasCursors ? 'Calculate power and loss between Cursors A and B' : 'Place Cursors A and B on plot to enable'}
            >
              📍 Between Cursors [A ↔ B]
              {hasCursors && waveformMetrics && (
                <span className="loss-range-badge">Δt = {formatEngineeringValue(waveformMetrics.dtTotal, 's')}</span>
              )}
            </button>
            <button
              type="button"
              className={`loss-range-pill ${analysisRange === 'all' ? 'active' : ''}`}
              onClick={() => setAnalysisRange('all')}
            >
              ⏱ Full Simulation Span
            </button>
          </div>
        </div>

        {/* Computed Metrics Cards */}
        {waveformMetrics ? (
          <div className="loss-computed-container">
            <div className="loss-metrics-grid">
              <div className="loss-stat-card primary">
                <span className="loss-stat-label">Average Power Loss (P_avg)</span>
                <span className="loss-stat-val primary">{formatEngineeringValue(waveformMetrics.pAvg, 'W')}</span>
                <span className="loss-stat-sub">Dissipated over {formatEngineeringValue(waveformMetrics.dtTotal, 's')}</span>
              </div>

              <div className="loss-stat-card">
                <span className="loss-stat-label">Total Dissipated Energy (E)</span>
                <span className="loss-stat-val highlight">{formatEngineeringValue(waveformMetrics.totalEnergy, 'J')}</span>
                <span className="loss-stat-sub">∫ v(t)·i(t) dt</span>
              </div>

              <div className="loss-stat-card">
                <span className="loss-stat-label">Peak Instantaneous Power</span>
                <span className="loss-stat-val warning">{formatEngineeringValue(waveformMetrics.pMax, 'W')}</span>
                <span className="loss-stat-sub">Peak stress point</span>
              </div>

              <div className="loss-stat-card">
                <span className="loss-stat-label">Est. Conduction Loss</span>
                <span className="loss-stat-val">{formatEngineeringValue(waveformMetrics.pCond, 'W')}</span>
                <span className="loss-stat-sub">On-state dissipation</span>
              </div>

              <div className="loss-stat-card">
                <span className="loss-stat-label">Est. Switching Loss</span>
                <span className="loss-stat-val">{formatEngineeringValue(waveformMetrics.pSw, 'W')}</span>
                <span className="loss-stat-sub">Turn-on / turn-off transitions</span>
              </div>

              <div className="loss-stat-card">
                <span className="loss-stat-label">Electrical RMS & PF</span>
                <span className="loss-stat-val">
                  {formatEngineeringValue(waveformMetrics.vRms, 'V')} / {formatEngineeringValue(waveformMetrics.iRms, 'A')}
                </span>
                <span className="loss-stat-sub">PF: {(waveformMetrics.powerFactor * 100).toFixed(1)}% ({formatEngineeringValue(waveformMetrics.apparentPower, 'VA')})</span>
              </div>
            </div>

            {/* Instantaneous Power Mini Waveform Chart */}
            {waveformMetrics.powerSamples.length > 2 && (() => {
              const samples = waveformMetrics.powerSamples;
              const pMin = Math.min(0, waveformMetrics.pMin);
              const pMax = Math.max(1e-3, waveformMetrics.pMax);
              const pSpan = pMax - pMin || 1;
              const plotW = 600;
              const plotH = 80;
              const t0 = samples[0].t;
              const t1 = samples[samples.length - 1].t;
              const tSpan = t1 - t0 || 1;

              const mapX = (t: number) => ((t - t0) / tSpan) * plotW;
              const mapY = (p: number) => plotH - ((p - pMin) / pSpan) * (plotH - 8) - 4;
              const zeroY = mapY(0);

              let d = `M ${mapX(samples[0].t)} ${mapY(samples[0].p)}`;
              for (let i = 1; i < samples.length; i++) {
                d += ` L ${mapX(samples[i].t)} ${mapY(samples[i].p)}`;
              }

              return (
                <div className="loss-power-chart-wrap">
                  <div className="loss-power-chart-header">
                    <span>Instantaneous Power Waveform p(t) = {voltageSignal} · {currentSignal}</span>
                    <span className="loss-power-chart-legend">
                      Peak: <strong>{formatEngineeringValue(pMax, 'W')}</strong> • Min: <strong>{formatEngineeringValue(pMin, 'W')}</strong>
                    </span>
                  </div>
                  <svg viewBox={`0 0 ${plotW} ${plotH}`} className="loss-power-svg" preserveAspectRatio="none">
                    <rect width={plotW} height={plotH} fill="rgba(15, 23, 42, 0.4)" rx={4} />
                    {zeroY >= 0 && zeroY <= plotH && (
                      <line x1={0} y1={zeroY} x2={plotW} y2={zeroY} stroke="#475569" strokeDasharray="3 3" strokeWidth={1} />
                    )}
                    <path d={d} fill="none" stroke="#f59e0b" strokeWidth={1.8} strokeLinejoin="round" />
                  </svg>
                </div>
              );
            })()}
          </div>
        ) : (
          <div className="loss-empty-hint">
            Select both a voltage trace v(t) and current trace i(t) from the dropdowns above to analyze power dissipation.
          </div>
        )}
      </div>

      {/* Explanatory Guide: What is this feature? */}
      <details className="loss-explainer-details">
        <summary className="loss-explainer-summary">
          <span>❓ What is this feature and how does it connect to the oscilloscope?</span>
        </summary>
        <div className="loss-explainer-body">
          <div className="loss-explainer-item">
            <h6>1. Waveform Power Analysis in an Oscilloscope</h6>
            <p>
              An oscilloscope measures electrical waveforms over time: voltage <span className="formula">v(t)</span> and current <span className="formula">i(t)</span>.
              In electrical circuits, the instantaneous power delivered to or dissipated by any element is the product:
              <br />
              <strong className="formula">p(t) = v(t) · i(t) [Watts]</strong>
            </p>
            <p>
              By integrating this curve over time, the scope calculates:
              <br />
              • <strong>Average Power (P_avg)</strong>: <span className="formula">P_avg = (1 / Δt) ∫ v(t)·i(t) dt</span> (effective continuous thermal dissipation).
              <br />
              • <strong>Total Dissipated Energy (E)</strong>: <span className="formula">E = ∫ v(t)·i(t) dt [Joules]</span> (energy converted to heat or magnetic/electric work).
              <br />
              • <strong>Cursors [A ↔ B] Interval</strong>: When placing cursors on the scope, you can isolate and measure energy dissipated during a single switching transient or resonance period.
            </p>
          </div>
          <div className="loss-explainer-item">
            <h6>2. Semiconductor Losses (Datasheet Calculator)</h6>
            <p>
              In switching converters (like Buck converters, Inverters, and Motor Drives), power semiconductor switches (MOSFETs, IGBTs, Diodes) produce two distinct types of losses:
            </p>
            <ul>
              <li>
                <strong>Conduction Losses (P_cond)</strong>: Heat produced when the switch is fully ON carrying current:
                <br />
                <span className="formula">P_cond = I_cond · (V_th + I_cond · R_on) · Duty</span>
              </li>
              <li>
                <strong>Switching Losses (P_sw)</strong>: Energy dissipated during turn-on and turn-off state transitions where voltage and current overlap:
                <br />
                <span className="formula">E_sw = (E_on + E_off) · (V_block / V_ref)</span> &nbsp;→&nbsp;
                <span className="formula">P_sw = E_sw · f_sw</span>
              </li>
            </ul>
          </div>
        </div>
      </details>

      {/* Datasheet Loss Model Calculator (REST API) */}
      <details className="loss-datasheet-details" open={Boolean(semiconductorComponents.length > 0 || !waveformMetrics)}>
        <summary className="loss-datasheet-summary">
          <span>📐 Semiconductor Datasheet Model Calculator (REST API)</span>
          {waveformMetrics && (
            <button
              type="button"
              className="loss-autofill-btn"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                handleAutoFillFromWaveforms();
              }}
              title="Auto-fill voltage, current, and frequency from current waveform measurements"
            >
              ⟳ Auto-Fill from Measured Waveforms
            </button>
          )}
        </summary>

        <div className="loss-grid">
          {numField('Eon (mJ)', eon, setEon, '0.1')}
          {numField('Eoff (mJ)', eoff, setEoff, '0.1')}
          {numField('f_sw (Hz)', fsw, setFsw, '100')}
          {numField('I_sw (A)', iSw, setISw, '0.5')}
          {numField('V_block (V)', vSw, setVSw, '10')}
          {numField('T_j (°C)', tj, setTj, '5')}
          {numField('V_ref (V)', vRef, setVRef, '10')}
          {numField('I_cond (A)', iCond, setICond, '0.5')}
          {numField('R_on (Ω)', rOn, setROn, '0.001')}
          {numField('V_th (V)', vTh, setVTh, '0.1')}
          {numField('Duty', duty, setDuty, '0.05')}
        </div>

        <div className="loss-calc-actions">
          <button type="button" onClick={() => void compute()} disabled={busy} className="loss-submit-btn">
            {busy ? 'Computing…' : 'Calculate losses'}
          </button>
        </div>

        {error && <div className="fft-error" role="alert">{error}</div>}
        {result && (
          <div className="loss-result" data-testid="loss-result">
            <span>Total: {formatEngineeringValue(result.totalLoss, 'W')}</span>
            {result.switchingLoss !== null && (
              <span>Switching: {formatEngineeringValue(result.switchingLoss, 'W')}</span>
            )}
            {result.conductionLoss !== null && (
              <span>Conduction: {formatEngineeringValue(result.conductionLoss, 'W')}</span>
            )}
            <span className="loss-method">({result.method})</span>
          </div>
        )}
      </details>
    </div>
  );
}
