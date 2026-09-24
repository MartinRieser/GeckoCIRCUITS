// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { SimulationPropertiesPanel } from '../src/properties/SimulationPropertiesPanel';
import type { EditorComponent } from '../src/model/types';
import { LkComponentType, ControlComponentType } from '../src/model/constants';

describe('SimulationPropertiesPanel Component', () => {
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
      inputLabels: ['in'],
      outputLabels: [],
    },
  ];

  it('renders simulation time and step inputs with defaults and triggers run', () => {
    const onRun = vi.fn();
    render(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status="IDLE"
        progress={0}
        defaults={{ duration: 0.01, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        components={dummyComponents}
        results={null}
        selectedScope="SCOPE.1"
        onSelectScope={vi.fn()}
        displayLayout="overlay"
        onDisplayLayoutChange={vi.fn()}
        onRunSimulation={onRun}
      />,
    );

    expect(screen.getByText('Simulation Settings')).not.toBeNull();
    const runBtn = screen.getByRole('button', { name: /Run Simulation/i });
    expect(runBtn).not.toBeNull();

    // First click triggers circuit validation warnings
    fireEvent.click(runBtn);
    expect(screen.getByText(/Check before running:/i)).not.toBeNull();

    // Second click on 'Run Anyway' proceeds with simulation
    const runAnywayBtn = screen.getByRole('button', { name: /Run Anyway/i });
    fireEvent.click(runAnywayBtn);

    expect(onRun).toHaveBeenCalledWith(
      expect.objectContaining({
        simulationTime: 0.01,
        timeStep: 1e-6,
      }),
    );
  });

  it('displays error message when simulation fails', () => {
    render(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status="FAILED"
        progress={0}
        errorMessage="Matrix singular in Newton-Raphson iteration"
        components={dummyComponents}
        results={null}
        selectedScope="SCOPE.1"
        onSelectScope={vi.fn()}
        displayLayout="overlay"
        onDisplayLayoutChange={vi.fn()}
        onRunSimulation={vi.fn()}
      />,
    );

    expect(screen.getByText(/Matrix singular in Newton-Raphson iteration/i)).not.toBeNull();
  });

  it('provides pause, resume, and cancel buttons when running or paused', () => {
    const onPause = vi.fn();
    const onCancel = vi.fn();

    const { rerender } = render(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status="RUNNING"
        progress={0.45}
        components={dummyComponents}
        results={null}
        selectedScope="SCOPE.1"
        onSelectScope={vi.fn()}
        displayLayout="overlay"
        onDisplayLayoutChange={vi.fn()}
        onRunSimulation={vi.fn()}
        onPauseSimulation={onPause}
        onCancelSimulation={onCancel}
      />,
    );

    const pauseBtn = screen.getByRole('button', { name: /Pause/i });
    fireEvent.click(pauseBtn);
    expect(onPause).toHaveBeenCalledTimes(1);

    const cancelBtn = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelBtn);
    expect(onCancel).toHaveBeenCalledTimes(1);

    const onResume = vi.fn();
    rerender(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status="PAUSED"
        progress={0.45}
        components={dummyComponents}
        results={null}
        selectedScope="SCOPE.1"
        onSelectScope={vi.fn()}
        displayLayout="overlay"
        onDisplayLayoutChange={vi.fn()}
        onRunSimulation={vi.fn()}
        onResumeSimulation={onResume}
      />,
    );

    const resumeBtn = screen.getByRole('button', { name: /Resume/i });
    fireEvent.click(resumeBtn);
    expect(onResume).toHaveBeenCalledTimes(1);
  });

  it('switches display layout between overlay and stacked', () => {
    const onLayoutChange = vi.fn();

    render(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status="FINISHED"
        progress={1.0}
        components={dummyComponents}
        results={{ time: [0, 1], 'R.1:i': [0, 2] }}
        selectedScope="SCOPE.1"
        onSelectScope={vi.fn()}
        displayLayout="overlay"
        onDisplayLayoutChange={onLayoutChange}
        onRunSimulation={vi.fn()}
      />,
    );

    const stackedBtn = screen.getByRole('button', { name: /Stacked/i });
    fireEvent.click(stackedBtn);
    expect(onLayoutChange).toHaveBeenCalledWith('stacked');
  });

  it('switches selected scope and allows csv export', () => {
    const onSelectScope = vi.fn();
    const onExportCsv = vi.fn();

    render(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status="FINISHED"
        progress={1.0}
        components={dummyComponents}
        results={{ time: [0, 1], 'R.1:i': [0, 2] }}
        selectedScope="SCOPE.1"
        onSelectScope={onSelectScope}
        displayLayout="overlay"
        onDisplayLayoutChange={vi.fn()}
        onRunSimulation={vi.fn()}
        onExportCsv={onExportCsv}
      />,
    );

    const exportBtn = screen.getByRole('button', { name: /Export CSV/i });
    fireEvent.click(exportBtn);
    expect(onExportCsv).toHaveBeenCalledTimes(1);
  });

  it('handles panel collapse button', () => {
    const onCollapse = vi.fn();
    render(
      <SimulationPropertiesPanel
        circuitId="test-circuit"
        status="IDLE"
        progress={0}
        components={dummyComponents}
        results={null}
        selectedScope="SCOPE.1"
        onSelectScope={vi.fn()}
        displayLayout="overlay"
        onDisplayLayoutChange={vi.fn()}
        onRunSimulation={vi.fn()}
        onCollapse={onCollapse}
      />,
    );

    const collapseBtn = screen.getByTitle(/Collapse simulation/i);
    fireEvent.click(collapseBtn);
    expect(onCollapse).toHaveBeenCalledTimes(1);
  });
});
