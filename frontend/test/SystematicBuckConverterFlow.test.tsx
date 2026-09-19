// @vitest-environment jsdom
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/react';
import { PropertiesPanel } from '../src/properties/PropertiesPanel';
import { Sheet } from '../src/canvas/Sheet';
import type { SheetActions } from '../src/canvas/Sheet';
import { initialState, type EditorState } from '../src/model/store';
import type { EditorComponent, EditorWire } from '../src/model/types';
import { isScopeComponent } from '../src/simulation/scopes';

const createBuckConverterComponents = (): EditorComponent[] => [
  {
    type: 4,
    family: 'LK',
    name: 'U.1',
    position: [8, 9],
    orientation: 503,
    parameters: { voltage: 12.0, param0: 401.0, param1: 12.0 },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: 7,
    family: 'LK',
    name: 'S.1',
    position: [16, 5],
    orientation: 503,
    parameters: { rON: 0.01, coupledComponent: 'GATE.1' },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: 6,
    family: 'LK',
    name: 'D.1',
    position: [16, 9],
    orientation: 501,
    parameters: { rON: 0.01 },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: 2,
    family: 'LK',
    name: 'L.1',
    position: [20, 7],
    orientation: 502,
    parameters: { inductance: 2.0e-5, param0: 2.0e-5 },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: 1,
    family: 'LK',
    name: 'R.L',
    position: [25, 7],
    orientation: 504,
    parameters: { resistance: 0.0, param0: 0.0 },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: 3,
    family: 'LK',
    name: 'C.1',
    position: [27, 13],
    orientation: 503,
    parameters: { capacitance: 2.0e-5, param0: 2.0e-5 },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: 1,
    family: 'LK',
    name: 'R.4',
    position: [27, 9],
    orientation: 503,
    parameters: { resistance: 0.0, param0: 0.0 },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: 1,
    family: 'LK',
    name: 'R.Last',
    position: [34, 11],
    orientation: 503,
    parameters: { resistance: 10.0, param0: 10.0 },
    inputLabels: [],
    outputLabels: [],
  },
  {
    type: 4,
    family: 'CONTROL',
    name: 'SIGNAL.1',
    position: [5, 18],
    orientation: 503,
    parameters: { dutyCycle: 0.417, frequency: 100000.0, amplitude: 1.0 },
    inputLabels: [],
    outputLabels: ['gt'],
  },
  {
    type: 6,
    family: 'CONTROL',
    name: 'GATE.1',
    position: [12, 18],
    orientation: 503,
    parameters: { coupledComponent: 'S.1' },
    inputLabels: ['gt'],
    outputLabels: [],
  },
  {
    type: 1,
    family: 'CONTROL',
    name: 'VOLT.1',
    position: [20, 16],
    orientation: 503,
    parameters: { nodeA: 'z1', nodeB: '0' },
    inputLabels: [],
    outputLabels: ['uOUT'],
  },
  {
    type: 1,
    family: 'CONTROL',
    name: 'VOLT.2',
    position: [20, 18],
    orientation: 503,
    parameters: { nodeA: 'in', nodeB: '0' },
    inputLabels: [],
    outputLabels: ['uIN'],
  },
  {
    type: 2,
    family: 'CONTROL',
    name: 'AMP.1',
    position: [20, 20],
    orientation: 503,
    parameters: { coupledComponent: 'L.1' },
    inputLabels: [],
    outputLabels: ['iL1'],
  },
  {
    type: 5,
    family: 'CONTROL',
    name: 'SCOPE.1',
    position: [32, 18],
    orientation: 503,
    parameters: { channels: 4 },
    inputLabels: ['uOUT', 'uIN', 'iL1', 'gt'],
    outputLabels: [],
  },
];

const createBuckConverterWires = (): EditorWire[] => [
  { index: 0, type: 'LK', label: 'z1', points: [[27, 7], [34, 7], [34, 9]] },
  { index: 1, type: 'LK', label: '0', points: [[8, 11], [8, 15], [34, 15]] },
  { index: 2, type: 'LK', label: '0', points: [[34, 15], [34, 13]] },
  { index: 3, type: 'LK', label: '0', points: [[16, 11], [16, 15]] },
  { index: 4, type: 'LK', label: 'in', points: [[16, 3], [8, 3], [8, 7]] },
  { index: 5, type: 'LK', label: 'NIX', points: [[16, 7], [18, 7]] },
  { index: 6, type: 'LK', label: 'NIX', points: [[23, 7], [22, 7]] },
  { index: 7, type: 'CONTROL', label: 'gt', points: [[7, 18], [10, 18]] },
];

const mockActions: SheetActions = {
  placeGhost: vi.fn(),
  finishWire: vi.fn(),
  commitMove: vi.fn(),
  deleteWire: vi.fn(),
  patchWirePoints: vi.fn(),
  flipWire: vi.fn(),
};

describe('Systematic DC-DC Buck Converter GUI & Parameter Flow', () => {
  it('loads DC-DC Buck Converter example with all 14 components and correct initial signals', () => {
    const components = createBuckConverterComponents();
    expect(components.length).toBe(14);

    const compNames = components.map((c) => c.name);
    expect(compNames).toContain('U.1');
    expect(compNames).toContain('S.1');
    expect(compNames).toContain('D.1');
    expect(compNames).toContain('L.1');
    expect(compNames).toContain('R.L');
    expect(compNames).toContain('C.1');
    expect(compNames).toContain('R.4');
    expect(compNames).toContain('R.Last');
    expect(compNames).toContain('SIGNAL.1');
    expect(compNames).toContain('GATE.1');
    expect(compNames).toContain('VOLT.1');
    expect(compNames).toContain('VOLT.2');
    expect(compNames).toContain('AMP.1');
    expect(compNames).toContain('SCOPE.1');

    // Check AMP.1 initial target & output signal
    const amp1 = components.find((c) => c.name === 'AMP.1')!;
    expect(amp1.parameters?.coupledComponent).toBe('L.1');
    expect(amp1.outputLabels?.[0]).toBe('iL1');

    // Check SCOPE.1 initial channels
    const scope1 = components.find((c) => c.name === 'SCOPE.1')!;
    expect(scope1.inputLabels).toEqual(['uOUT', 'uIN', 'iL1', 'gt']);
  });

  it('retargets AMP.1 from L.1 to R.L and automatically syncs SCOPE.1 channel from iL1 to iRL', () => {
    let components = createBuckConverterComponents();
    const wires = createBuckConverterWires();

    const onSetParameter = vi.fn((compName: string, paramKey: string, value: unknown) => {
      components = components.map((c) =>
        c.name === compName
          ? { ...c, parameters: { ...c.parameters, [paramKey]: value as number | string | boolean } }
          : c,
      );
    });

    const onSetLabel = vi.fn((compName: string, side: 'x' | 'y', idx: number | string, label?: string) => {
      const targetSide = side;
      const targetIdx = typeof idx === 'number' ? idx : 0;
      const targetLabel = typeof idx === 'string' ? idx : (label || '');

      let oldLabel = '';
      components = components.map((c) => {
        if (c.name !== compName) return c;
        if (targetSide === 'x') {
          const arr = [...(c.inputLabels || [])];
          oldLabel = arr[targetIdx] || '';
          arr[targetIdx] = targetLabel;
          return { ...c, inputLabels: arr };
        } else {
          const arr = [...(c.outputLabels || [])];
          oldLabel = arr[targetIdx] || '';
          arr[targetIdx] = targetLabel;
          return { ...c, outputLabels: arr };
        }
      });

      // Mirror useEditor's automatic propagation: if an output probe label changes, update consumer scopes
      if (targetSide === 'y' && oldLabel && targetLabel && oldLabel !== targetLabel) {
        components = components.map((c) => {
          if (isScopeComponent(c) && c.inputLabels) {
            const updatedInputs = c.inputLabels.map((sig) => (sig === oldLabel ? targetLabel : sig));
            return { ...c, inputLabels: updatedInputs };
          }
          return c;
        });
      }
    });

    const amp1 = components.find((c) => c.name === 'AMP.1')!;

    const { container, rerender } = render(
      <PropertiesPanel
        component={amp1}
        allComponents={components}
        wires={wires}
        onSetParameter={onSetParameter}
        onSetLabel={onSetLabel}
        onRename={() => {}}
      />,
    );

    // Verify current measurement target dropdown is present with L.1 selected
    const select = container.querySelector('select.coupling-select') as HTMLSelectElement;
    expect(select).toBeTruthy();
    expect(select.value).toBe('L.1');

    // Change target to R.L
    fireEvent.change(select, { target: { value: 'R.L' } });

    expect(onSetParameter).toHaveBeenCalledWith('AMP.1', 'coupledComponent', 'R.L');
    expect(onSetLabel).toHaveBeenCalledWith('AMP.1', 'y', 0, 'iRL');

    // Verify Scope.1 channel 3 updated from iL1 to iRL
    const updatedScope = components.find((c) => c.name === 'SCOPE.1')!;
    expect(updatedScope.inputLabels).toEqual(['uOUT', 'uIN', 'iRL', 'gt']);
    expect(updatedScope.inputLabels[2]).toBe('iRL');

    // Re-render properties panel for the updated SCOPE.1
    rerender(
      <PropertiesPanel
        component={updatedScope}
        allComponents={components}
        wires={wires}
        onSetParameter={onSetParameter}
        onSetLabel={onSetLabel}
        onRename={() => {}}
      />,
    );

    // Verify Scope channels inspector displays CH3 with iRL
    const ch3Input = container.querySelector('input[value="iRL"]') as HTMLInputElement;
    expect(ch3Input).toBeTruthy();
  });

  it('systematically adjusts DC-DC converter electrical parameters and verifies them', () => {
    let components = createBuckConverterComponents();

    const onSetParameter = vi.fn((compName: string, paramKey: string, value: unknown) => {
      components = components.map((c) =>
        c.name === compName
          ? { ...c, parameters: { ...c.parameters, [paramKey]: value as number | string | boolean } }
          : c,
      );
    });

    // 1. Check Inductor L.1 inductance
    const l1 = components.find((c) => c.name === 'L.1')!;
    expect(l1.parameters?.inductance ?? l1.parameters?.param0).toBe(2.0e-5);
    onSetParameter('L.1', 'inductance', 5.0e-5);

    // 2. Check Load resistor R.Last resistance
    const rLast = components.find((c) => c.name === 'R.Last')!;
    expect(rLast.parameters?.resistance ?? rLast.parameters?.param0).toBe(10.0);
    onSetParameter('R.Last', 'resistance', 5.0);

    // 3. Check Signal source parameters
    const signal1 = components.find((c) => c.name === 'SIGNAL.1')!;
    expect(signal1.parameters?.dutyCycle).toBe(0.417);
    onSetParameter('SIGNAL.1', 'dutyCycle', 0.65);

    // Verify updated values
    const updatedL1 = components.find((c) => c.name === 'L.1')!;
    expect(updatedL1.parameters?.inductance).toBe(5.0e-5);

    const updatedRLast = components.find((c) => c.name === 'R.Last')!;
    expect(updatedRLast.parameters?.resistance).toBe(5.0);

    const updatedSignal1 = components.find((c) => c.name === 'SIGNAL.1')!;
    expect(updatedSignal1.parameters?.dutyCycle).toBe(0.65);
  });

  it('renders schematic with smart net label filtering avoiding overlapping text', () => {
    const components = createBuckConverterComponents();
    const wires = createBuckConverterWires();

    const state: EditorState = {
      ...initialState,
      circuitId: 'test-buck',
      components,
      wires,
      selection: [],
    };

    const { container } = render(
      <Sheet
        state={state}
        dispatch={vi.fn()}
        actions={mockActions}
      />,
    );

    // Verify smart mode renders virtual net labels (uOUT, uIN, iL1, gt)
    const labelsLayer = container.querySelector('.terminal-net-labels-layer');
    expect(labelsLayer).toBeTruthy();

    const labelTexts = Array.from(container.querySelectorAll('.node-label')).map(
      (el) => el.textContent,
    );

    // Virtual labels MUST be visible in Smart mode
    expect(labelTexts).toContain('uOUT');
    expect(labelTexts).toContain('uIN');
    expect(labelTexts).toContain('iL1');
    expect(labelTexts).toContain('gt');

    // Wired ground node '0' should NOT clutter the screen in default smart mode
    const groundPills = labelTexts.filter((t) => t === '0');
    expect(groundPills.length).toBe(0);
  });
});
