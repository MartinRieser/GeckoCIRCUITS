// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { SimConfigModal } from '../src/simulation/SimConfigModal';
import type { EditorComponent } from '../src/model/types';
import { LkComponentType, ControlComponentType } from '../src/model/constants';

describe('SimConfigModal Component', () => {
  afterEach(() => {
    cleanup();
  });

  const dummyComponents: EditorComponent[] = [
    {
      type: LkComponentType.RESISTOR,
      family: 'LK',
      name: 'R.1',
      position: [10, 10],
      orientation: 502,
      parameters: { R: '10' },
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: ControlComponentType.SCOPE,
      family: 'CONTROL',
      name: 'SCOPE.1',
      position: [20, 10],
      orientation: 503,
      parameters: { no_signals: '1' },
      inputLabels: ['V_out'],
      outputLabels: [],
    },
  ];

  it('does not render when isOpen is false', () => {
    const { container } = render(
      <SimConfigModal
        isOpen={false}
        onClose={vi.fn()}
        circuitId="c1"
        components={dummyComponents}
        onRunSimulation={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders timing inputs, solver select, step count, and scope instruments when open', () => {
    render(
      <SimConfigModal
        isOpen={true}
        onClose={vi.fn()}
        circuitId="c1"
        defaults={{ duration: 0.02, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        components={dummyComponents}
        onRunSimulation={vi.fn()}
      />,
    );

    expect(screen.getByText('⚙ Simulation Configuration')).not.toBeNull();
    expect(screen.getByLabelText(/Total Duration/i)).not.toBeNull();
    expect(screen.getByLabelText(/Time Step/i)).not.toBeNull();
    expect(screen.getByLabelText(/Differential Equation Solver/i)).not.toBeNull();
    expect(screen.getByText(/20,000 steps/i)).not.toBeNull();
    expect(screen.getByText('SCOPE.1')).not.toBeNull();
  });

  it('closes on Cancel click and Escape key', () => {
    const onClose = vi.fn();
    render(
      <SimConfigModal
        isOpen={true}
        onClose={onClose}
        circuitId="c1"
        components={dummyComponents}
        onRunSimulation={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /Cancel/i }));
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it('validates circuit and triggers run simulation with parsed values', () => {
    const onRun = vi.fn();
    const onSettingsChange = vi.fn();
    const onClose = vi.fn();

    render(
      <SimConfigModal
        isOpen={true}
        onClose={onClose}
        circuitId="c1"
        settings={{ tEnd: '10m', dt: '1u', solver: 'backward-euler' }}
        defaults={{ duration: 0.01, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        components={dummyComponents}
        onSettingsChange={onSettingsChange}
        onRunSimulation={onRun}
      />
    );

    const startBtn = screen.getByRole('button', { name: /Start Simulation/i });
    // First click shows circuit validation warnings if any
    fireEvent.click(startBtn);

    // Second click starts run
    const runAnywayBtn = screen.getByRole('button', { name: /Run Anyway/i });
    fireEvent.click(runAnywayBtn);

    expect(onSettingsChange).toHaveBeenCalledWith({
      tEnd: '10m',
      dt: '1u',
      solver: 'backward-euler',
    });
    expect(onRun).toHaveBeenCalledWith({
      simulationTime: 0.01,
      timeStep: 1e-6,
      solverType: 'backward-euler',
      backend: 'headless',
    });
    expect(onClose).toHaveBeenCalled();
  });

  it('disables run button when inputs are invalid', () => {
    render(
      <SimConfigModal
        isOpen={true}
        onClose={vi.fn()}
        circuitId="c1"
        defaults={{ duration: 0.01, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        components={dummyComponents}
        onRunSimulation={vi.fn()}
      />,
    );

    const tEndInput = screen.getByLabelText(/Total Duration/i);
    fireEvent.change(tEndInput, { target: { value: 'invalid-string' } });

    const startBtn = screen.getByRole('button', { name: /Start Simulation/i });
    expect((startBtn as HTMLButtonElement).disabled).toBe(true);
  });
});
