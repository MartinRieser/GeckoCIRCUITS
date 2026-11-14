import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { SimToolbarStrip } from '../src/simulation/SimToolbarStrip';
import { ControlComponentType, LkComponentType } from '../src/model/constants';
import type { EditorComponent } from '../src/model/types';

function makeComp(overrides: Partial<EditorComponent>): EditorComponent {
  return {
    type: LkComponentType.RESISTOR,
    name: 'R.1',
    family: 'LK',
    position: [10, 10],
    orientation: 503,
    parameters: {},
    inputLabels: [],
    outputLabels: [],
    ...overrides,
  };
}

describe('SimToolbarStrip', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders inline dt, t_end, solver, step pill, and run button with resolved defaults', () => {
    render(
      <SimToolbarStrip
        circuitId="ckt-1"
        isRunning={false}
        status={null}
        progress={0}
        defaults={{ duration: 0.02, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        settings={{ tEnd: '20m', dt: '1u', solver: 'backward-euler' }}
        components={[makeComp({ name: 'U.1', type: LkComponentType.VOLTAGE_SOURCE })]}
        onSettingsChange={vi.fn()}
        onQuickRun={vi.fn()}
        onPause={vi.fn()}
        onResume={vi.fn()}
        onCancel={vi.fn()}
        onOpenConfigModal={vi.fn()}
      />,
    );

    expect((screen.getByTestId('toolbar-dt-input') as HTMLInputElement).value).toBe('1u');
    expect((screen.getByTestId('toolbar-tend-input') as HTMLInputElement).value).toBe('20m');
    expect((screen.getByTestId('toolbar-solver-select') as HTMLSelectElement).value).toBe(
      'backward-euler',
    );
    expect(screen.getByTestId('toolbar-steps-pill').textContent).toContain('20k steps ✓');
    expect(screen.getByRole('button', { name: /▶ Run/i })).toBeTruthy();
  });

  it('commits valid inline dt edits and flags invalid input until blur', () => {
    const onSettingsChange = vi.fn();
    render(
      <SimToolbarStrip
        circuitId="ckt-1"
        isRunning={false}
        status={null}
        progress={0}
        settings={{ tEnd: '20m', dt: '1u', solver: 'backward-euler' }}
        components={[]}
        onSettingsChange={onSettingsChange}
        onQuickRun={vi.fn()}
        onPause={vi.fn()}
        onResume={vi.fn()}
        onCancel={vi.fn()}
        onOpenConfigModal={vi.fn()}
      />,
    );

    const dtInput = screen.getByTestId('toolbar-dt-input') as HTMLInputElement;
    fireEvent.change(dtInput, { target: { value: '500n' } });
    expect(onSettingsChange).toHaveBeenCalledWith({
      tEnd: '20m',
      dt: '500n',
      solver: 'backward-euler',
    });

    onSettingsChange.mockClear();
    fireEvent.change(dtInput, { target: { value: 'invalid' } });
    expect(dtInput.className).toContain('invalid');
    expect(onSettingsChange).not.toHaveBeenCalled();

    fireEvent.blur(dtInput);
    expect(dtInput.value).toBe('1u');
  });

  it('auto-calculates time step (dt) from circuit switching frequency', () => {
    const onSettingsChange = vi.fn();
    const onStatusMessage = vi.fn();
    const pwmSource = makeComp({
      name: 'SIGNAL.1',
      type: ControlComponentType.SIGNAL_SOURCE,
      family: 'CONTROL',
      parameters: { param0: 403, param2: 100_000 }, // 100 kHz
    });

    render(
      <SimToolbarStrip
        circuitId="ckt-1"
        isRunning={false}
        status={null}
        progress={0}
        settings={{ tEnd: '20m', dt: '10u', solver: 'backward-euler' }}
        components={[pwmSource]}
        onSettingsChange={onSettingsChange}
        onQuickRun={vi.fn()}
        onPause={vi.fn()}
        onResume={vi.fn()}
        onCancel={vi.fn()}
        onOpenConfigModal={vi.fn()}
        onStatusMessage={onStatusMessage}
      />,
    );

    // 10 us at 100 kHz is 1 step/period -> aliasing warning
    expect(screen.getByTestId('toolbar-steps-pill').className).toContain('warn');
    expect(screen.getByTestId('toolbar-steps-pill').textContent).toContain('⚠');

    fireEvent.click(screen.getByTestId('toolbar-auto-dt-btn'));
    expect(onSettingsChange).toHaveBeenCalled();
    const updated = onSettingsChange.mock.calls[0][0];
    expect(updated.dt).toBe('50n');
    expect(onStatusMessage).toHaveBeenCalled();
  });

  it('applies dt/t_end presets, Auto-dt from dropdown, and solver changes', () => {
    const onSettingsChange = vi.fn();
    const onStatusMessage = vi.fn();
    render(
      <SimToolbarStrip
        circuitId="ckt-1"
        isRunning={false}
        status={null}
        progress={0}
        settings={{ tEnd: '20m', dt: '1u', solver: 'backward-euler' }}
        components={[]}
        onSettingsChange={onSettingsChange}
        onQuickRun={vi.fn()}
        onPause={vi.fn()}
        onResume={vi.fn()}
        onCancel={vi.fn()}
        onOpenConfigModal={vi.fn()}
        onStatusMessage={onStatusMessage}
      />,
    );

    fireEvent.change(screen.getByTestId('toolbar-dt-presets'), { target: { value: '100n' } });
    expect(onSettingsChange).toHaveBeenLastCalledWith({
      tEnd: '20m',
      dt: '100n',
      solver: 'backward-euler',
    });

    fireEvent.change(screen.getByTestId('toolbar-tend-presets'), { target: { value: '50m' } });
    expect(onSettingsChange).toHaveBeenLastCalledWith({
      tEnd: '50m',
      dt: '1u',
      solver: 'backward-euler',
    });

    onSettingsChange.mockClear();
    fireEvent.change(screen.getByTestId('toolbar-dt-presets'), { target: { value: '__auto__' } });
    expect(onSettingsChange).toHaveBeenCalledTimes(1);
    expect(onStatusMessage).toHaveBeenCalled();

    fireEvent.change(screen.getByTestId('toolbar-solver-select'), {
      target: { value: 'trapezoidal' },
    });
    expect(onSettingsChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ solver: 'trapezoidal' }),
    );
  });

  it('renders progress, pause, and cancel controls while simulation is running', () => {
    const onPause = vi.fn();
    const onCancel = vi.fn();
    render(
      <SimToolbarStrip
        circuitId="ckt-1"
        isRunning={true}
        status="RUNNING"
        progress={0.42}
        settings={{ tEnd: '20m', dt: '1u', solver: 'backward-euler' }}
        components={[]}
        onSettingsChange={vi.fn()}
        onQuickRun={vi.fn()}
        onPause={onPause}
        onResume={vi.fn()}
        onCancel={onCancel}
        onOpenConfigModal={vi.fn()}
      />,
    );

    expect(screen.getByText('42%')).toBeTruthy();
    fireEvent.click(screen.getByTitle('Pause Simulation'));
    expect(onPause).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByTitle('Cancel Simulation'));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
