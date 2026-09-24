// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { PropertiesPanel } from '../src/properties/PropertiesPanel';
import type { EditorComponent, EditorWire } from '../src/model/types';

describe('PropertiesPanel Overhaul', () => {
  afterEach(() => {
    cleanup();
  });

  const dummyWires: EditorWire[] = [
    { index: 0, type: 'LK', label: 'uIN', points: [[0, 0], [10, 0]] },
  ];

  it('renders Switch Coupling section for Gate Driver with available switches', () => {
    const gateComp: EditorComponent = {
      type: 1000,
      family: 'CONTROL',
      name: 'GATE.1',
      position: [12, 16],
      orientation: 503,
      parameters: { coupledComponent: 'S.1' },
      inputLabels: ['gt'],
      outputLabels: [],
    };

    const switchComp: EditorComponent = {
      type: 7,
      family: 'LK',
      name: 'S.1',
      position: [10, 10],
      orientation: 502,
      parameters: { coupledComponent: 'GATE.1' },
      inputLabels: [],
      outputLabels: [],
    };

    const onSetParameter = vi.fn();
    const onSelectComponent = vi.fn();

    const { container } = render(
      <PropertiesPanel
        component={gateComp}
        allComponents={[gateComp, switchComp]}
        wires={dummyWires}
        onRename={vi.fn()}
        onSetParameter={onSetParameter}
        onSetLabel={vi.fn()}
        onSelectComponent={onSelectComponent}
      />,
    );

    expect(screen.getByText(/Switch Coupling/i)).not.toBeNull();
    expect(screen.getByText(/Controls switch/i)).not.toBeNull();
    expect(container.querySelector('.coupling-select')).not.toBeNull();

    // Check "Go to S.1 ↗" button
    const navBtn = screen.getByRole('button', { name: /Go to S\.1/i });
    expect(navBtn).not.toBeNull();
    fireEvent.click(navBtn);
    expect(onSelectComponent).toHaveBeenCalledWith('S.1');
  });

  it('renders Gate Drive Coupling section for Semiconductor Switch', () => {
    const switchComp: EditorComponent = {
      type: 7,
      family: 'LK',
      name: 'S.1',
      position: [10, 10],
      orientation: 502,
      parameters: { coupledComponent: 'GATE.1' },
      inputLabels: [],
      outputLabels: [],
    };

    const gateComp: EditorComponent = {
      type: 1000,
      family: 'CONTROL',
      name: 'GATE.1',
      position: [12, 16],
      orientation: 503,
      parameters: { coupledComponent: 'S.1' },
      inputLabels: ['gt'],
      outputLabels: [],
    };

    const onSetParameter = vi.fn();

    const { container } = render(
      <PropertiesPanel
        component={switchComp}
        allComponents={[switchComp, gateComp]}
        wires={dummyWires}
        onRename={vi.fn()}
        onSetParameter={onSetParameter}
        onSetLabel={vi.fn()}
      />,
    );

    expect(screen.getByText(/Gate Drive Coupling/i)).not.toBeNull();
    expect(screen.getByText(/Driven by gate driver/i)).not.toBeNull();

    const select = container.querySelector('select.coupling-select');
    expect(select).not.toBeNull();
    fireEvent.change(select!, { target: { value: '' } });
    expect(onSetParameter).toHaveBeenCalledWith('S.1', 'coupledComponent', '');
  });

  it('renders all Scope channels (CH1, CH2, CH3, CH4) and allows editing channel index', () => {
    const scopeComp: EditorComponent = {
      type: 1003,
      family: 'CONTROL',
      name: 'SCOPE.1',
      position: [20, 20],
      orientation: 502,
      parameters: {},
      inputLabels: ['uOUT', 'uIN', 'il1', 'gt'],
      outputLabels: [],
    };

    const onSetLabel = vi.fn();
    const onOpenScopeTab = vi.fn();

    render(
      <PropertiesPanel
        component={scopeComp}
        allComponents={[scopeComp]}
        wires={dummyWires}
        onRename={vi.fn()}
        onSetParameter={vi.fn()}
        onSetLabel={onSetLabel}
        onOpenScopeTab={onOpenScopeTab}
      />,
    );

    expect(screen.getByText(/Scope Channels \(4\)/i)).not.toBeNull();
    expect(screen.getByText('CH1')).not.toBeNull();
    expect(screen.getByText('CH2')).not.toBeNull();
    expect(screen.getByText('CH3')).not.toBeNull();
    expect(screen.getByText('CH4')).not.toBeNull();

    expect(screen.getByDisplayValue('uOUT')).not.toBeNull();
    expect(screen.getByDisplayValue('uIN')).not.toBeNull();
    expect(screen.getByDisplayValue('il1')).not.toBeNull();
    expect(screen.getByDisplayValue('gt')).not.toBeNull();

    // Edit CH3 (index 2)
    const il1Input = screen.getByDisplayValue('il1');
    fireEvent.change(il1Input, { target: { value: 'i_inductor' } });
    expect(onSetLabel).toHaveBeenCalledWith('SCOPE.1', 'x', 2, 'i_inductor');

    // Add Channel button
    const addBtn = screen.getByRole('button', { name: /\+ Add Channel \(CH5\)/i });
    expect(addBtn).not.toBeNull();
    fireEvent.click(addBtn);
    expect(onSetLabel).toHaveBeenCalledWith('SCOPE.1', 'x', 4, '');

    // Open Scope Tab button
    const openScopeBtn = screen.getByRole('button', { name: /Open Scope Tab ↗/i });
    fireEvent.click(openScopeBtn);
    expect(onOpenScopeTab).toHaveBeenCalledWith('SCOPE.1');
  });

  it('renders available signals datalist for autocomplete', () => {
    const voltComp: EditorComponent = {
      type: 1001,
      family: 'CONTROL',
      name: 'VOLT.1',
      position: [15, 15],
      orientation: 502,
      parameters: {},
      inputLabels: [],
      outputLabels: ['uOUT'],
    };

    render(
      <PropertiesPanel
        component={voltComp}
        allComponents={[voltComp]}
        wires={dummyWires}
        onRename={vi.fn()}
        onSetParameter={vi.fn()}
        onSetLabel={vi.fn()}
      />,
    );

    const datalist = document.getElementById('circuit-available-signals');
    expect(datalist).not.toBeNull();
    expect(datalist?.querySelector('option[value="uOUT"]')).not.toBeNull();
    expect(datalist?.querySelector('option[value="uIN"]')).not.toBeNull();
  });

  it('renders Current Measurement Target section for Ammeter with targets dropdown and allows selection', () => {
    const ampComp: EditorComponent = {
      type: 1002,
      family: 'CONTROL',
      name: 'AMP.1',
      position: [20, 20],
      orientation: 503,
      parameters: { coupledComponent: 'L.1' },
      inputLabels: [],
      outputLabels: ['iL1'],
    };

    const inductorComp: EditorComponent = {
      type: 2,
      family: 'LK',
      name: 'L.1',
      position: [20, 7],
      orientation: 502,
      parameters: { param0: 2e-5 },
      inputLabels: [],
      outputLabels: [],
    };

    const resistorComp: EditorComponent = {
      type: 1,
      family: 'LK',
      name: 'R.4',
      position: [27, 9],
      orientation: 503,
      parameters: { param0: 0 },
      inputLabels: ['z1'],
      outputLabels: ['z1c'],
    };

    const onSetParameter = vi.fn();
    const onSetLabel = vi.fn();
    const onSelectComponent = vi.fn();

    const { container } = render(
      <PropertiesPanel
        component={ampComp}
        allComponents={[ampComp, inductorComp, resistorComp]}
        wires={dummyWires}
        onRename={vi.fn()}
        onSetParameter={onSetParameter}
        onSetLabel={onSetLabel}
        onSelectComponent={onSelectComponent}
      />,
    );

    expect(screen.getByText(/Current Measurement Target/i)).not.toBeNull();
    const statusPill = container.querySelector('.coupling-status-pill.active');
    expect(statusPill?.textContent).toContain('Measures branch current through');
    expect(statusPill?.textContent).toContain('L.1');

    // Check dropdown options
    const select = container.querySelector('select.coupling-select') as HTMLSelectElement;
    expect(select).not.toBeNull();
    expect(select.value).toBe('L.1');

    // Change target to R.4
    fireEvent.change(select, { target: { value: 'R.4' } });
    expect(onSetParameter).toHaveBeenCalledWith('AMP.1', 'coupledComponent', 'R.4');
    expect(onSetLabel).toHaveBeenCalledWith('AMP.1', 'y', 0, 'iR4');

    // Navigation button "Go to L.1 ↗"
    const navBtn = screen.getByRole('button', { name: /Go to L\.1/i });
    expect(navBtn).not.toBeNull();
    fireEvent.click(navBtn);
    expect(onSelectComponent).toHaveBeenCalledWith('L.1');
  });

  it('renders Voltage Measurement Target section for Voltmeter in node mode', () => {
    const voltComp: EditorComponent = {
      type: 1001,
      family: 'CONTROL',
      name: 'VOLT.1',
      position: [20, 16],
      orientation: 503,
      parameters: { nodeA: 'z1', nodeB: '0' },
      inputLabels: [],
      outputLabels: ['uOUT'],
    };

    const onSetParameter = vi.fn();

    render(
      <PropertiesPanel
        component={voltComp}
        allComponents={[voltComp]}
        wires={dummyWires}
        onRename={vi.fn()}
        onSetParameter={onSetParameter}
        onSetLabel={vi.fn()}
      />,
    );

    expect(screen.getByText(/Voltage Measurement Target/i)).not.toBeNull();
    expect(screen.getByText(/Positive Node \(\+\)/i)).not.toBeNull();
    expect(screen.getByText(/Negative Node \(-\)/i)).not.toBeNull();

    const posInput = screen.getByDisplayValue('z1');
    expect(posInput).not.toBeNull();
    fireEvent.change(posInput, { target: { value: 'in' } });
    expect(onSetParameter).toHaveBeenCalledWith('VOLT.1', 'nodeA', 'in');
  });

  it('renders Voltage Measurement Target section for Voltmeter in component mode and allows selection', () => {
    const voltComp: EditorComponent = {
      type: 1001,
      family: 'CONTROL',
      name: 'VOLT.1',
      position: [20, 16],
      orientation: 503,
      parameters: { coupledComponent: 'R.Last' },
      inputLabels: [],
      outputLabels: ['u_RLast'],
    };

    const rLastComp: EditorComponent = {
      type: 1,
      family: 'LK',
      name: 'R.Last',
      position: [30, 10],
      orientation: 503,
      parameters: { param0: 10 },
      inputLabels: ['z1'],
      outputLabels: ['0'],
    };

    const onSetParameter = vi.fn();
    const onSetLabel = vi.fn();

    const { container } = render(
      <PropertiesPanel
        component={voltComp}
        allComponents={[voltComp, rLastComp]}
        wires={dummyWires}
        onRename={vi.fn()}
        onSetParameter={onSetParameter}
        onSetLabel={onSetLabel}
      />,
    );

    expect(screen.getByText(/Across Component/i)).not.toBeNull();
    expect(screen.getByText(/Between Two Nodes/i)).not.toBeNull();
    const select = container.querySelector('select.coupling-select') as HTMLSelectElement;
    expect(select).not.toBeNull();
    expect(select.value).toBe('R.Last');

    // Switch to differential nodes: nodeA prefills with the first available
    // signal (the resistor's name is deliberately NOT a signal candidate)
    const nodeBtn = screen.getByRole('button', { name: /Between Two Nodes/i });
    fireEvent.click(nodeBtn);
    expect(onSetParameter).toHaveBeenCalledWith('VOLT.1', 'coupledComponent', '');
    expect(onSetParameter).toHaveBeenCalledWith('VOLT.1', 'nodeA', 'u_RLast');
    expect(onSetParameter).toHaveBeenCalledWith('VOLT.1', 'nodeB', '0');
  });

  it('renders Terminal Net Labels for R.4 with distinct names for terminal 1 and 2', () => {
    const resistorComp: EditorComponent = {
      type: 1,
      family: 'LK',
      name: 'R.4',
      position: [27, 9],
      orientation: 503,
      parameters: { param0: 0 },
      inputLabels: ['z1'],
      outputLabels: ['z1c'],
    };

    render(
      <PropertiesPanel
        component={resistorComp}
        allComponents={[resistorComp]}
        wires={dummyWires}
        onRename={vi.fn()}
        onSetParameter={vi.fn()}
        onSetLabel={vi.fn()}
      />,
    );

    expect(screen.getByText(/Terminal Net Labels/i)).not.toBeNull();
    expect(screen.getByDisplayValue('z1')).not.toBeNull();
    expect(screen.getByDisplayValue('z1c')).not.toBeNull();
  });

  it('handles component rename, rotate, delete, and collapse actions', () => {
    const onRename = vi.fn();
    const onRotate = vi.fn();
    const onDelete = vi.fn();
    const onCollapse = vi.fn();

    const resistorComp: EditorComponent = {
      type: 1,
      family: 'LK',
      name: 'R.1',
      position: [10, 10],
      orientation: 502,
      parameters: { R: 100 },
      inputLabels: [],
      outputLabels: [],
    };

    render(
      <PropertiesPanel
        component={resistorComp}
        allComponents={[resistorComp]}
        wires={[]}
        onRename={onRename}
        onSetParameter={vi.fn()}
        onSetLabel={vi.fn()}
        onRotate={onRotate}
        onDelete={onDelete}
        onCollapse={onCollapse}
      />,
    );

    // Rename
    const nameInput = screen.getByDisplayValue('R.1');
    fireEvent.change(nameInput, { target: { value: 'R.Renamed' } });
    fireEvent.blur(nameInput);
    expect(onRename).toHaveBeenCalledWith('R.1', 'R.Renamed');

    // Rotate
    const rotateBtn = screen.getByTitle(/Rotate/i);
    fireEvent.click(rotateBtn);
    expect(onRotate).toHaveBeenCalledWith('R.1');

    // Delete
    const deleteBtn = screen.getByTitle(/Delete Component/i);
    fireEvent.click(deleteBtn);
    expect(onDelete).toHaveBeenCalledWith('R.1');

    // Collapse
    const collapseBtn = screen.getByTitle(/Collapse properties/i);
    fireEvent.click(collapseBtn);
    expect(onCollapse).toHaveBeenCalledTimes(1);
  });

  it('renders ScriptBlockEditor for SCRIPT component and handles code and terminal adjustment', () => {
    const onSetParameter = vi.fn();
    const scriptComp: EditorComponent = {
      type: 1016, // ControlComponentType.SCRIPT
      family: 'CONTROL',
      name: 'CTRL_SCRIPT',
      position: [10, 10],
      orientation: 503,
      parameters: {
        sourceCode: 'yOUT[0] = sin(t);',
        anzXIN: 1,
        anzYOUT: 1,
      },
      inputLabels: [],
      outputLabels: [],
    };

    render(
      <PropertiesPanel
        component={scriptComp}
        allComponents={[scriptComp]}
        wires={[]}
        onRename={vi.fn()}
        onSetParameter={onSetParameter}
        onSetLabel={vi.fn()}
      />,
    );

    expect(screen.getByText('Script / Function Logic')).not.toBeNull();

    // Toggle cheat sheet
    const cheatSheetBtn = screen.getByRole('button', { name: /Cheat Sheet/i });
    fireEvent.click(cheatSheetBtn);
    expect(screen.getByText(/State variables automatically persist/i)).not.toBeNull();

    // Apply code changes
    const applyBtn = screen.getByRole('button', { name: /Apply Script/i });
    fireEvent.click(applyBtn);

    expect(onSetParameter).toHaveBeenCalledWith('CTRL_SCRIPT', 'sourceCode', 'yOUT[0] = sin(t);');
    expect(onSetParameter).toHaveBeenCalledWith('CTRL_SCRIPT', 'anzXIN', 1);
    expect(onSetParameter).toHaveBeenCalledWith('CTRL_SCRIPT', 'anzYOUT', 1);
  });
});

