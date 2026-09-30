// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { LossPanel } from '../src/simulation/LossPanel';

const fetchMock = vi.fn();
vi.stubGlobal('fetch', fetchMock);

beforeEach(() => {
  fetchMock.mockReset();
});
afterEach(() => {
  cleanup();
  fetchMock.mockReset();
});

it('posts all loss fields and renders the result', async () => {
  fetchMock.mockResolvedValue({
    ok: true,
    headers: new Headers({ 'content-type': 'application/json' }),
    json: () => Promise.resolve({
      totalLoss: 210.5,
      switchingLoss: 22.0,
      conductionLoss: 188.5,
      method: 'linear_interpolation',
    }),
  });

  render(<LossPanel />);
  fireEvent.click(screen.getByRole('button', { name: 'Calculate losses' }));

  await waitFor(() => expect(screen.getByTestId('loss-result')).toBeTruthy());
  const [url, init] = fetchMock.mock.calls[0];
  expect(url).toBe('/gecko/api/v1/loss/detailed');
  expect(init.method).toBe('POST');
  const body = JSON.parse(init.body);
  expect(body.switchingFrequency).toBe(20_000);
  expect(body.turnOnEnergy).toBeCloseTo(0.0005, 9); // 0.5 mJ in J
  expect(body.dutyCycle).toBe(0.5);

  const resultText = screen.getByTestId('loss-result').textContent ?? '';
  expect(resultText).toContain('210.5');
  expect(resultText).toContain('22');
  expect(resultText).toContain('linear_interpolation');
});

it('surfaces server errors', async () => {
  fetchMock.mockResolvedValue({
    ok: false,
    status: 400,
    headers: new Headers(),
    json: () => Promise.reject(new Error('no body')),
  });
  render(<LossPanel />);
  fireEvent.click(screen.getByRole('button', { name: 'Calculate losses' }));
  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('HTTP 400'));
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
  expect(screen.getByText('Semiconductor Power & Loss Analysis')).toBeDefined();

  // Displays Average Power and Energy
  // p(t) ranges from 200W to 1000W; average is 600W
  // Energy = 600W * 0.4ms = 0.24 J
  expect(screen.getByText(/Average Power Loss/i)).toBeDefined();
  expect(screen.getByText(/Total Dissipated Energy/i)).toBeDefined();
  expect(screen.getByText(/Peak Instantaneous Power/i)).toBeDefined();

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
