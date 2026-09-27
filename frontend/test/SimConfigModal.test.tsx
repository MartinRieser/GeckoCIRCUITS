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

  it('does NOT close modal when text selection drag starts inside input and releases on backdrop', () => {
    const onClose = vi.fn();
    const { container } = render(
      <SimConfigModal
        isOpen={true}
        onClose={onClose}
        circuitId="c1"
        defaults={{ duration: 0.02, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        components={dummyComponents}
        onRunSimulation={vi.fn()}
      />,
    );

    const tEndInput = screen.getByLabelText(/Total Duration/i);
    const backdrop = container.querySelector('.sim-config-modal-backdrop')!;
    expect(backdrop).not.toBeNull();

    // User starts mouse selection inside text input
    fireEvent.mouseDown(tEndInput);
    // User drags and releases mouse outside the dialog on the backdrop
    fireEvent.mouseUp(backdrop);
    fireEvent.click(backdrop);

    // Modal must NOT close!
    expect(onClose).not.toHaveBeenCalled();
  });

  it('does NOT close modal when an abandoned backdrop press is followed by a drag from inside the dialog', () => {
    const onClose = vi.fn();
    const { container } = render(
      <SimConfigModal
        isOpen={true}
        onClose={onClose}
        circuitId="c1"
        defaults={{ duration: 0.02, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        components={dummyComponents}
        onRunSimulation={vi.fn()}
      />,
    );

    const dialog = container.querySelector('.sim-config-dialog')!;
    const backdrop = container.querySelector('.sim-config-modal-backdrop')!;
    expect(dialog).not.toBeNull();
    expect(backdrop).not.toBeNull();

    // Backdrop press abandoned without a click (e.g. mouse released outside
    // the window) — must not arm a later backdrop click
    fireEvent.mouseDown(backdrop);
    // A new press starts inside the dialog (e.g. text selection in an input)
    // and its drag releases on the backdrop
    fireEvent.mouseDown(dialog);
    fireEvent.click(backdrop);

    expect(onClose).not.toHaveBeenCalled();
  });

  it('closes modal when backdrop itself is deliberately clicked', () => {
    const onClose = vi.fn();
    const { container } = render(
      <SimConfigModal
        isOpen={true}
        onClose={onClose}
        circuitId="c1"
        defaults={{ duration: 0.02, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        components={dummyComponents}
        onRunSimulation={vi.fn()}
      />,
    );

    const backdrop = container.querySelector('.sim-config-modal-backdrop')!;
    expect(backdrop).not.toBeNull();

    // Mouse down directly on backdrop, followed by click on backdrop
    fireEvent.mouseDown(backdrop);
    fireEvent.click(backdrop);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('supports Enter key inside duration input to trigger simulation execution', () => {
    const onRun = vi.fn();
    const onSettingsChange = vi.fn();
    const onClose = vi.fn();

    render(
      <SimConfigModal
        isOpen={true}
        onClose={onClose}
        circuitId="c1"
        defaults={{ duration: 0.02, timeStep: 1e-6, solverType: 'backward-euler', signals: [] }}
        components={[]}
        onSettingsChange={onSettingsChange}
        onRunSimulation={onRun}
      />,
    );

    const tEndInput = screen.getByLabelText(/Total Duration/i);
    fireEvent.change(tEndInput, { target: { value: '50m' } });

    // First Enter surfaces circuit validation warnings (e.g. empty circuit)
    fireEvent.keyDown(tEndInput, { key: 'Enter' });
    expect(screen.getByText(/Circuit Validation Notice/i)).not.toBeNull();

    // Second Enter confirms and runs simulation
    fireEvent.keyDown(tEndInput, { key: 'Enter' });

    expect(onSettingsChange).toHaveBeenCalledWith(
      expect.objectContaining({ tEnd: '50m' }),
    );
    expect(onRun).toHaveBeenCalledWith(
      expect.objectContaining({ simulationTime: 0.05 }),
    );
    expect(onClose).toHaveBeenCalled();
  });

  it('handles intermediate keystrokes and edge cases when changing total duration without crashing', () => {
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

    const tEndInput = screen.getByLabelText(/Total Duration/i);

    // Simulate clearing input
    fireEvent.change(tEndInput, { target: { value: '' } });
    expect(screen.getByText(/Enter valid time/i)).not.toBeNull();

    // Simulate intermediate '.' character
    fireEvent.change(tEndInput, { target: { value: '.' } });
    expect(screen.getByText(/Enter valid time/i)).not.toBeNull();

    // Non-numerical string
    fireEvent.change(tEndInput, { target: { value: 'abc' } });
    expect(screen.getByText(/Enter valid time/i)).not.toBeNull();

    // Valid exponential notation
    fireEvent.change(tEndInput, { target: { value: '1e-3' } });
    expect(screen.getByText(/1 ms/i)).not.toBeNull();

    // Zero
    fireEvent.change(tEndInput, { target: { value: '0' } });
    expect(screen.getByText(/Enter valid time/i)).not.toBeNull();

    // Negative value
    fireEvent.change(tEndInput, { target: { value: '-20m' } });
    expect(screen.getByText(/Enter valid time/i)).not.toBeNull();

    // Large number with SI prefix
    fireEvent.change(tEndInput, { target: { value: '500m' } });
    expect(screen.getByText(/500 ms/i)).not.toBeNull();
  });
});
