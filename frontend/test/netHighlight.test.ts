import { describe, expect, it } from 'vitest';
import { analyzeElectricalNets } from '../src/canvas/netHighlight';
import type { EditorComponent, EditorWire } from '../src/model/types';

describe('netHighlight: analyzeElectricalNets', () => {
  const resistor: EditorComponent = {
    type: 1,
    name: 'R1',
    family: 'LK',
    position: [10, 10],
    orientation: 502,
    parameters: {},
    inputLabels: [],
    outputLabels: [],
  };

  const capacitor: EditorComponent = {
    type: 3,
    name: 'C1',
    family: 'LK',
    position: [20, 10],
    orientation: 502,
    parameters: {},
    inputLabels: [],
    outputLabels: [],
  };

  it('identifies a single wire connecting two terminals as one net', () => {
    // R1 output at (12, 10); C1 input at (18, 10)
    const wires: EditorWire[] = [
      {
        index: 0,
        type: 'LK',
        points: [
          [12, 10],
          [18, 10],
        ],
      },
    ];

    const result = analyzeElectricalNets(wires, [resistor, capacitor]);
    expect(result.nets).toHaveLength(1);
    const net = result.nets[0];
    expect(net.wireIndices.has(0)).toBe(true);
    expect(net.terminalKeys.has('12,10')).toBe(true);
    expect(net.terminalKeys.has('18,10')).toBe(true);
    expect(result.wireIndexToNet.get(0)).toBe(net);
    expect(result.terminalKeyToNet.get('12,10')).toBe(net);
    expect(result.terminalKeyToNet.get('18,10')).toBe(net);
  });

  it('unions contiguous wires meeting at a shared junction point', () => {
    const wire1: EditorWire = {
      index: 0,
      type: 'LK',
      points: [
        [10, 10],
        [15, 10],
      ],
    };
    const wire2: EditorWire = {
      index: 1,
      type: 'LK',
      points: [
        [15, 10],
        [20, 10],
      ],
    };
    const wire3Branch: EditorWire = {
      index: 2,
      type: 'LK',
      points: [
        [15, 10],
        [15, 20],
      ],
    };

    const result = analyzeElectricalNets([wire1, wire2, wire3Branch], []);
    expect(result.nets).toHaveLength(1);
    const net = result.nets[0];
    expect(net.wireIndices.size).toBe(3);
    expect(net.wireIndices.has(0)).toBe(true);
    expect(net.wireIndices.has(1)).toBe(true);
    expect(net.wireIndices.has(2)).toBe(true);
  });

  it('separates disjoint wires into distinct nets', () => {
    const wire1: EditorWire = {
      index: 0,
      type: 'LK',
      points: [
        [5, 5],
        [10, 5],
      ],
    };
    const wire2: EditorWire = {
      index: 1,
      type: 'LK',
      points: [
        [5, 15],
        [10, 15],
      ],
    };

    const result = analyzeElectricalNets([wire1, wire2], []);
    expect(result.nets).toHaveLength(2);
    expect(result.wireIndexToNet.get(0)).not.toBe(result.wireIndexToNet.get(1));
  });

  it('unions separate wires that share an explicit net label', () => {
    const wire1: EditorWire = {
      index: 0,
      type: 'LK',
      label: 'VCC',
      points: [
        [5, 5],
        [10, 5],
      ],
    };
    const wire2: EditorWire = {
      index: 1,
      type: 'LK',
      label: 'VCC',
      points: [
        [5, 25],
        [10, 25],
      ],
    };

    const result = analyzeElectricalNets([wire1, wire2], []);
    expect(result.nets).toHaveLength(1);
    const net = result.nets[0];
    expect(net.label).toBe('VCC');
    expect(net.wireIndices.has(0)).toBe(true);
    expect(net.wireIndices.has(1)).toBe(true);
  });

  it('identifies virtual nets between labeled terminals without wires', () => {
    const compA: EditorComponent = {
      type: 100,
      name: 'IN1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: {},
      inputLabels: ['NET_A'],
      outputLabels: [],
    };
    const compB: EditorComponent = {
      type: 100,
      name: 'IN2',
      family: 'LK',
      position: [30, 30],
      orientation: 502,
      parameters: {},
      inputLabels: ['NET_A'],
      outputLabels: [],
    };

    const result = analyzeElectricalNets([], [compA, compB]);
    expect(result.nets).toHaveLength(1);
    const net = result.nets[0];
    expect(net.label).toBe('NET_A');
    expect(net.terminalKeys.size).toBe(2);
  });
});
