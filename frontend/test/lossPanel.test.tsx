// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { LossPanel } from '../src/simulation/LossPanel';
import { LkComponentType } from '../src/model/constants';
import type { EditorComponent } from '../src/model/types';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);

beforeEach(() => {
  fetchMock.mockReset();
});
afterEach(() => {
  cleanup();
  fetchMock.mockReset();
});

it('posts switching and conduction loss fields and renders the combined result', async () => {
  fetchMock.mockImplementation((url: string) => {
    if (url.includes('/loss/switching')) {
      return Promise.resolve({
        ok: true,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: () =>
          Promise.resolve({
            totalLoss: 0.0011, // 1.1 mJ per switching event
            switchingLoss: 0.0011,
            conductionLoss: 0,
            method: 'simple_switching',
          }),
      });
    }
    if (url.includes('/loss/conduction')) {
      return Promise.resolve({
        ok: true,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: () =>
          Promise.resolve({
            totalLoss: 8.0, // 8 W continuous
            switchingLoss: null,
            conductionLoss: 8.0,
            method: 'simple_conduction',
          }),
      });
    }
    return Promise.reject(new Error(`Unhandled URL: ${url}`));
  });

  render(<LossPanel />);
  fireEvent.click(screen.getByRole('button', { name: 'Calculate losses' }));

  await waitFor(() => expect(screen.getByTestId('loss-result')).toBeTruthy());

  // Check calls to switching and conduction endpoints
  const urls = fetchMock.mock.calls.map((c) => c[0]);
  expect(urls).toContain('/gecko/api/v1/loss/switching');
  expect(urls).toContain('/gecko/api/v1/loss/conduction');

  // Verify switching call parameters
  const switchingCall = fetchMock.mock.calls.find((c) => c[0].includes('/loss/switching'))!;
  const swBody = JSON.parse(switchingCall[1].body);
  expect(swBody.turnOnEnergy).toBeCloseTo(0.0005, 9); // 0.5 mJ in J
  expect(swBody.turnOffEnergy).toBeCloseTo(0.0006, 9); // 0.6 mJ in J
  expect(swBody.voltage).toBe(400);

  // Verify conduction call parameters
  const conductionCall = fetchMock.mock.calls.find((c) => c[0].includes('/loss/conduction'))!;
  const condBody = JSON.parse(conductionCall[1].body);
  expect(condBody.onResistance).toBe(0.01);
  expect(condBody.thresholdVoltage).toBe(0.7);

  // Combined result: P_sw = 0.0011 * 20000 = 22W; P_cond = 8 * 0.5 = 4W; Total = 26W
  const resultText = screen.getByTestId('loss-result').textContent ?? '';
  expect(resultText).toContain('26');
  expect(resultText).toContain('22');
  expect(resultText).toContain('analytical');
});

it('surfaces server errors with field details', async () => {
  fetchMock.mockResolvedValue({
    ok: false,
    status: 400,
    headers: new Headers(),
    json: () => Promise.resolve({ message: 'Invalid parameters', fieldErrors: { voltage: 'must be greater than 0' } }),
  });
  render(<LossPanel />);
  fireEvent.click(screen.getByRole('button', { name: 'Calculate losses' }));
  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('HTTP 400'));
});

it('displays passive circuit guidance when circuit contains no semiconductors', () => {
  const passiveComponents: EditorComponent[] = [
    {
      type: LkComponentType.RESISTOR,
      name: 'R1',
      family: 'LK',
      position: [0, 0],
      orientation: 0,
      parameters: { R: 10 },
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: LkComponentType.INDUCTOR,
      name: 'L1',
      family: 'LK',
      position: [1, 0],
      orientation: 0,
      parameters: { L: 0.001 },
      inputLabels: [],
      outputLabels: [],
    },
  ];

  render(<LossPanel components={passiveComponents} />);
  expect(screen.getByText(/Passive Circuit Detected/i)).toBeDefined();
  expect(screen.getByText(/Waveform Power Analyzer/i)).toBeDefined();
});

it('displays semiconductor circuit awareness when circuit contains switches', () => {
  const switchComponents: EditorComponent[] = [
    {
      type: LkComponentType.MOSFET,
      name: 'MOS.1',
      family: 'LK',
      position: [0, 0],
      orientation: 0,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: LkComponentType.DIODE,
      name: 'D.1',
      family: 'LK',
      position: [1, 0],
      orientation: 0,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    },
  ];

  render(<LossPanel components={switchComponents} />);
  expect(screen.getByText(/Power Electronics Circuit Detected/i)).toBeDefined();
  expect(screen.getByText(/MOS.1, D.1/i)).toBeDefined();
});

it('calculates instantaneous power, average power, and energy from scope waveforms', () => {
  const dummyTime = [0, 1e-4, 2e-4, 3e-4, 4e-4];
  const dummySignals = {
    V_out: [100, 100, 100, 100, 100],
    I_L: [2, 4, 6, 8, 10],
  };

  render(
    <LossPanel
      time={dummyTime}
      signals={dummySignals}
      activeSignals={['V_out', 'I_L']}
      cursorA={null}
      cursorB={null}
    />,
  );

  // Section title exists
  expect(screen.getByText('Waveform Power & Loss Analysis')).toBeDefined();

  // Displays Average Power and Energy
  // p(t) ranges from 200W to 1000W; average is 600W
  // Energy = 600W * 0.4ms = 0.24 J
  expect(screen.getAllByText(/Average Power Loss/i).length).toBeGreaterThanOrEqual(1);
  expect(screen.getAllByText(/Total Dissipated Energy/i).length).toBeGreaterThanOrEqual(1);
  expect(screen.getAllByText(/Peak Instantaneous Power/i).length).toBeGreaterThanOrEqual(1);

  // Instantaneous power mini-chart SVG is rendered
  expect(document.querySelector('.loss-power-svg')).not.toBeNull();
});

it('integrates with oscilloscope Cursors A and B for interval power analysis', () => {
  const dummyTime = [0, 1e-4, 2e-4, 3e-4, 4e-4];
  const dummySignals = {
    V_out: [100, 100, 100, 100, 100],
    I_L: [2, 4, 6, 8, 10],
  };

  render(
    <LossPanel
      time={dummyTime}
      signals={dummySignals}
      activeSignals={['V_out', 'I_L']}
      cursorA={1}
      cursorB={3}
    />,
  );

  // Cursor button is enabled and displays interval
  const cursorBtn = screen.getByRole('button', { name: /Between Cursors/i });
  expect(cursorBtn).toBeDefined();
  expect(cursorBtn.hasAttribute('disabled')).toBe(false);
  expect(cursorBtn.textContent).toContain('Δt =');
});
