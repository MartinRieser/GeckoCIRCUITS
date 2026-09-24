// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { SimulationDrawer } from '../src/simulation/SimulationDrawer';
import type { EditorComponent } from '../src/model/types';
import { ControlComponentType, LkComponentType } from '../src/model/constants';

describe('SimulationDrawer Component', () => {
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
      parameters: { R: 10 },
      inputLabels: [],
      outputLabels: [],
    },
    {
      type: ControlComponentType.SCOPE,
      family: 'CONTROL',
      name: 'SCOPE.1',
      position: [20, 10],
      orientation: 503,
      parameters: {},
      inputLabels: ['v_out'],
      outputLabels: [],
    },
  ];

  const dummyResults = {
    time: [0, 0.001, 0.002, 0.003],
    v_out: [0, 5, 10, 5],
  };

  it('renders collapsed drawer handle with status badge and toggles on click', () => {
    const onToggle = vi.fn();
    render(
      <SimulationDrawer
        isOpen={false}
        onToggle={onToggle}
        circuitId="circuit-1"
        components={dummyComponents}
        defaults={{ duration: 0.01, timeStep: 1e-6, solverType: 'TRAPEZOIDAL', signals: [] }}
        onRunSimulation={vi.fn()}
        status={null}
        progress={0}
        results={null}
      />,
    );

    const toggleBtn = screen.getByRole('button', { name: /▲\s*Simulation/i });
    expect(toggleBtn).not.toBeNull();
    fireEvent.click(toggleBtn);
    expect(onToggle).toHaveBeenCalledTimes(1);
  });

  it('renders opened drawer with simulation inputs and triggers onRunSimulation', () => {
    const onRun = vi.fn();
    render(
      <SimulationDrawer
        isOpen={true}
        onToggle={vi.fn()}
        circuitId="circuit-1"
        components={dummyComponents}
        defaults={{ duration: 0.01, timeStep: 1e-6, solverType: 'TRAPEZOIDAL', signals: [] }}
        onRunSimulation={onRun}
        status={null}
        progress={0}
        results={null}
      />,
    );

    const runBtn = screen.getByRole('button', { name: /Run/i });
    expect(runBtn).not.toBeNull();
    fireEvent.click(runBtn);

    expect(onRun).toHaveBeenCalledWith(
      expect.objectContaining({
        simulationTime: 0.01,
        timeStep: 1e-6,
      }),
    );
  });

  it('renders pause, resume, and cancel buttons when simulation is running or paused', () => {
    const onPause = vi.fn();
    const onCancel = vi.fn();

    const { container, rerender } = render(
      <SimulationDrawer
        isOpen={true}
        onToggle={vi.fn()}
        circuitId="circuit-1"
        components={dummyComponents}
        defaults={null}
        onRunSimulation={vi.fn()}
        onPauseSimulation={onPause}
        onCancelSimulation={onCancel}
        status="RUNNING"
        progress={0.5}
        results={null}
      />,
    );

    expect(screen.getByText(/Running simulation/i)).not.toBeNull();
    expect(container.querySelector('.sim-progress-bar')).not.toBeNull();

    const pauseBtn = screen.getByRole('button', { name: /Pause/i });
    fireEvent.click(pauseBtn);
    expect(onPause).toHaveBeenCalledTimes(1);

    const cancelBtn = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelBtn);
    expect(onCancel).toHaveBeenCalledTimes(1);

    const onResume = vi.fn();
    rerender(
      <SimulationDrawer
        isOpen={true}
        onToggle={vi.fn()}
        circuitId="circuit-1"
        components={dummyComponents}
        defaults={null}
        onRunSimulation={vi.fn()}
        onResumeSimulation={onResume}
        status="PAUSED"
        progress={0.5}
        results={null}
      />,
    );

    const resumeBtn = screen.getByRole('button', { name: /Resume/i });
    fireEvent.click(resumeBtn);
    expect(onResume).toHaveBeenCalledTimes(1);
  });

  it('displays error banner when status is FAILED and errorMessage is provided', () => {
    render(
      <SimulationDrawer
        isOpen={true}
        onToggle={vi.fn()}
        circuitId="circuit-1"
        components={dummyComponents}
        defaults={null}
        onRunSimulation={vi.fn()}
        status="FAILED"
        progress={0}
        results={null}
        errorMessage="Solver convergence failure at t=0.002s"
      />,
    );

    expect(screen.getByText(/Solver convergence failure/i)).not.toBeNull();
  });

  it('renders waveform traces, legend toggles, and statistics table when results are available', () => {
    render(
      <SimulationDrawer
        isOpen={true}
        onToggle={vi.fn()}
        circuitId="circuit-1"
        components={dummyComponents}
        defaults={null}
        onRunSimulation={vi.fn()}
        status="COMPLETED"
        progress={1.0}
        results={dummyResults}
      />,
    );

    // Legend channel pill
    const channelPill = screen.getByTitle(/Hide v_out/i);
    expect(channelPill).not.toBeNull();

    // Toggle signal visibility
    fireEvent.click(channelPill);
    expect(screen.getByTitle(/Show v_out/i)).not.toBeNull();

    // Statistics table row & legend
    expect(screen.getAllByText('v_out').length).toBeGreaterThanOrEqual(2);

    // Switch display layout to stacked and back
    const stackedBtn = screen.getByRole('button', { name: /Stacked/i });
    fireEvent.click(stackedBtn);

    const overlayBtn = screen.getByRole('button', { name: /Overlay/i });
    fireEvent.click(overlayBtn);
  });
});
