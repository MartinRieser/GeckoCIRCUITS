/**
 * Loss calculation card for the scope view: switching and conduction
 * semiconductor losses via the REST loss endpoints (/loss/switching,
 * /loss/conduction).
 */
import { useState } from 'react';
import { formatEngineeringValue } from '../model/componentSchema';

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
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return (await response.json()) as T;
}

function numField(
  label: string,
  value: number,
  onChange: (v: number) => void,
  step = 'any',
): React.ReactNode {
  return (
    <label>
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

export function LossPanel() {
  // switching inputs
  const [eon, setEon] = useState(0.5); // mJ
  const [eoff, setEoff] = useState(0.6); // mJ
  const [fsw, setFsw] = useState(20_000); // Hz
  const [iSw, setISw] = useState(10); // A
  const [vSw, setVSw] = useState(400); // V
  const [tj, setTj] = useState(125); // degC
  const [vRef, setVRef] = useState(400); // V
  // conduction inputs
  const [iCond, setICond] = useState(10); // A
  const [rOn, setROn] = useState(0.01); // Ohm
  const [vTh, setVTh] = useState(0.7); // V
  const [duty, setDuty] = useState(0.5);

  const [result, setResult] = useState<LossResponseShape | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const compute = async () => {
    setBusy(true);
    setError(null);
    try {
      const response = await postJson<LossResponseShape>('/gecko/api/v1/loss/detailed', {
        current: iSw,
        voltage: vSw,
        temperature: tj,
        referenceVoltage: vRef,
        turnOnEnergy: eon / 1000, // mJ -> J
        turnOffEnergy: eoff / 1000,
        switchingFrequency: fsw,
        onResistance: rOn,
        thresholdVoltage: vTh,
        conductionCurrent: iCond,
        dutyCycle: duty,
      });
      setResult(response);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="loss-panel" data-testid="loss-panel">
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
      <button type="button" onClick={() => void compute()} disabled={busy}>
        {busy ? 'Computing…' : 'Calculate losses'}
      </button>
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
    </div>
  );
}
