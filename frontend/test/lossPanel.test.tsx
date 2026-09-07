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
