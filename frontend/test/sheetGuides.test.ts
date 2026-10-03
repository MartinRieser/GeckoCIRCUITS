import { describe, expect, it } from 'vitest';
import {
  isTerminalWired,
  buildCouplingPairs,
  buildNetTerminalPoints,
  buildVoltmeterGuides,
  buildSignalEmitters,
  buildSignalDescriptions,
  buildNetWirePoints,
  findNearestPointOnNet,
  buildScopeGuides,
  resolveChannelDisplayLabel,
  buildTerminalLabelItems,
} from '../src/canvas/sheetGuides';
import type { EditorComponent, EditorWire } from '../src/model/types';
import { Orientation, LkComponentType, ControlComponentType } from '../src/model/constants';

function makeComp(
  partial: Partial<EditorComponent> &
    Pick<EditorComponent, 'type' | 'name' | 'family' | 'position' | 'orientation'>,
): EditorComponent {
  return {
    parameters: {},
    inputLabels: [],
    outputLabels: [],
    ...partial,
  };
}

function makeWire(
  partial: Partial<EditorWire> & Pick<EditorWire, 'index' | 'type' | 'points'>,
): EditorWire {
  return {
    label: '',
    ...partial,
  };
}

describe('sheetGuides: isTerminalWired', () => {
  const terminal = { x: 20, y: 10 };

  it('detects a wire vertex touching the terminal within tolerance', () => {
    const wires: EditorWire[] = [
      makeWire({
        index: 0,
        type: 'LK',
        points: [
          [10, 10],
          [20.1, 10.05], // hypot distance = ~0.11 < 0.25
        ],
      }),
    ];
    expect(isTerminalWired(terminal, wires)).toBe(true);
  });

  it('rejects wire points outside tolerance distance', () => {
    const wires: EditorWire[] = [
      makeWire({
        index: 0,
        type: 'LK',
        points: [
          [10, 10],
          [20.5, 10], // distance = 0.5 >= 0.25
        ],
      }),
    ];
    expect(isTerminalWired(terminal, wires)).toBe(false);
  });

  it('respects a custom tolerance parameter', () => {
    const wires: EditorWire[] = [
      makeWire({
        index: 0,
        type: 'LK',
        points: [[20.5, 10]],
      }),
    ];
    expect(isTerminalWired(terminal, wires, 0.6)).toBe(true);
    expect(isTerminalWired(terminal, wires, 0.4)).toBe(false);
  });

  it('returns false when no wires are present', () => {
    expect(isTerminalWired(terminal, [])).toBe(false);
  });
});

describe('sheetGuides: buildCouplingPairs', () => {
  const gateDriver = makeComp({
    type: ControlComponentType.GATE,
    name: 'GATE.1',
    family: 'CONTROL',
    position: [10, 20],
    orientation: Orientation.WEST_EAST,
    parameters: { coupledComponent: 'S.1' },
  });

  const ammeter = makeComp({
    type: ControlComponentType.AMMETER,
    name: 'AMP.1',
    family: 'CONTROL',
    position: [30, 20],
    orientation: Orientation.WEST_EAST,
    parameters: { coupledComponent: 'L.1' },
  });

  const voltmeterWithTarget = makeComp({
    type: ControlComponentType.VOLTMETER,
    name: 'VOLT.1',
    family: 'CONTROL',
    position: [50, 20],
    orientation: Orientation.WEST_EAST,
    parameters: { coupledComponent: 'C.1' },
  });

  const switchComp = makeComp({
    type: LkComponentType.MOSFET,
    name: 'S.1',
    family: 'LK',
    position: [10, 10],
    orientation: Orientation.NORTH_SOUTH,
  });

  const inductorComp = makeComp({
    type: LkComponentType.INDUCTOR,
    name: 'L.1',
    family: 'LK',
    position: [30, 10],
    orientation: Orientation.WEST_EAST,
  });

  const capacitorComp = makeComp({
    type: LkComponentType.CAPACITOR,
    name: 'C.1',
    family: 'LK',
    position: [50, 10],
    orientation: Orientation.NORTH_SOUTH,
  });

  const components = [
    gateDriver,
    ammeter,
    voltmeterWithTarget,
    switchComp,
    inductorComp,
    capacitorComp,
  ];

  it('constructs coupling pairs for gate driver, ammeter, and coupled voltmeter', () => {
    const pairs = buildCouplingPairs(components, [], null);
    expect(pairs).toHaveLength(3);

    expect(pairs[0].label).toBe('GATE DRIVE ➔');
    expect(pairs[0].sourceComp.name).toBe('GATE.1');
    expect(pairs[0].targetComp.name).toBe('S.1');
    expect(pairs[0].isHoveredOrSelected).toBe(false);

    expect(pairs[1].label).toBe('MEASURE I ➔');
    expect(pairs[1].sourceComp.name).toBe('AMP.1');
    expect(pairs[1].targetComp.name).toBe('L.1');

    expect(pairs[2].label).toBe('MEASURE U ➔');
    expect(pairs[2].sourceComp.name).toBe('VOLT.1');
    expect(pairs[2].targetComp.name).toBe('C.1');
  });

  it('marks pair as hovered/selected when source or target is selected or hovered', () => {
    const selSource = buildCouplingPairs(components, ['GATE.1'], null);
    expect(selSource[0].isHoveredOrSelected).toBe(true);

    const selTarget = buildCouplingPairs(components, ['S.1'], null);
    expect(selTarget[0].isHoveredOrSelected).toBe(true);

    const hoverTarget = buildCouplingPairs(components, [], 'L.1');
    expect(hoverTarget[1].isHoveredOrSelected).toBe(true);
  });
});

describe('sheetGuides: buildNetTerminalPoints', () => {
  it('maps net labels on electrical components and skips CONTROL family', () => {
    const components: EditorComponent[] = [
      makeComp({
        type: LkComponentType.RESISTOR,
        name: 'R.1',
        family: 'LK',
        position: [20, 10],
        orientation: Orientation.WEST_EAST,
        inputLabels: ['in_node'],
        outputLabels: ['out_node'],
      }),
      makeComp({
        type: ControlComponentType.CONSTANT,
        name: 'CONST.1',
        family: 'CONTROL',
        position: [40, 10],
        orientation: Orientation.WEST_EAST,
        outputLabels: ['ctrl_out'],
      }),
    ];

    const map = buildNetTerminalPoints(components);
    expect(map.has('in_node')).toBe(true);
    expect(map.has('out_node')).toBe(true);
    expect(map.has('ctrl_out')).toBe(false); // Ignored because CONTROL
    expect(map.get('in_node')).toEqual({ x: 18, y: 10 });
    expect(map.get('out_node')).toEqual({ x: 22, y: 10 });
  });

  it('ignores unset sentinel labels NIX_NIX_NIX', () => {
    const components: EditorComponent[] = [
      makeComp({
        type: LkComponentType.RESISTOR,
        name: 'R.1',
        family: 'LK',
        position: [20, 10],
        orientation: Orientation.WEST_EAST,
        inputLabels: ['NIX_NIX_NIX'],
      }),
    ];
    const map = buildNetTerminalPoints(components);
    expect(map.size).toBe(0);
  });
});

describe('sheetGuides: buildVoltmeterGuides', () => {
  it('resolves positive and negative node points and sets polarity', () => {
    const netPoints = new Map<string, { x: number; y: number }>([
      ['v_high', { x: 20, y: 10 }],
      ['gnd', { x: 20, y: 30 }],
    ]);

    const vm = makeComp({
      type: ControlComponentType.VOLTMETER,
      name: 'VOLT.1',
      family: 'CONTROL',
      position: [10, 20],
      orientation: Orientation.WEST_EAST,
      parameters: {
        positiveNode: 'v_high',
        negativeNode: 'gnd',
      },
    });

    const guides = buildVoltmeterGuides([vm], ['VOLT.1'], null, netPoints);
    expect(guides).toHaveLength(1);
    expect(guides[0].key).toBe('VOLT.1');
    expect(guides[0].isHoveredOrSelected).toBe(true);
    expect(guides[0].points).toEqual([
      { x: 20, y: 10, polarity: 'pos' },
      { x: 20, y: 30, polarity: 'neg' },
    ]);
  });

  it('deduplicates identical coordinates', () => {
    const netPoints = new Map<string, { x: number; y: number }>([
      ['n1', { x: 20, y: 10 }],
    ]);

    const vm = makeComp({
      type: ControlComponentType.VOLTMETER,
      name: 'VOLT.1',
      family: 'CONTROL',
      position: [10, 20],
      orientation: Orientation.WEST_EAST,
      parameters: {
        nodeA: 'n1',
        nodeB: 'n1',
      },
    });

    const guides = buildVoltmeterGuides([vm], [], null, netPoints);
    expect(guides[0].points).toHaveLength(1);
  });
});

describe('sheetGuides: buildSignalEmitters & buildSignalDescriptions', () => {
  const ammeter = makeComp({
    type: ControlComponentType.AMMETER,
    name: 'AMP.1',
    family: 'CONTROL',
    position: [10, 20],
    orientation: Orientation.WEST_EAST,
    parameters: { coupledComponent: 'L.boost' },
    outputLabels: ['i_L'],
  });

  const voltmeter = makeComp({
    type: ControlComponentType.VOLTMETER,
    name: 'VOLT.1',
    family: 'CONTROL',
    position: [30, 20],
    orientation: Orientation.WEST_EAST,
    parameters: { positiveNode: 'out', negativeNode: '0' },
    outputLabels: ['v_out'],
  });

  const differentialVm = makeComp({
    type: ControlComponentType.VOLTMETER,
    name: 'VOLT.diff',
    family: 'CONTROL',
    position: [50, 20],
    orientation: Orientation.WEST_EAST,
    parameters: { nodeA: 'drain', nodeB: 'source' },
    outputLabels: ['v_ds'],
  });

  const components = [ammeter, voltmeter, differentialVm];

  it('maps signal names to emitting meter components', () => {
    const emitters = buildSignalEmitters(components);
    expect(emitters.get('i_L')?.name).toBe('AMP.1');
    expect(emitters.get('v_out')?.name).toBe('VOLT.1');
    expect(emitters.get('v_ds')?.name).toBe('VOLT.diff');
  });

  it('creates formatted descriptions for current and voltage measurements', () => {
    const descriptions = buildSignalDescriptions(components);
    expect(descriptions.get('i_L')).toBe('I(L.boost)');
    expect(descriptions.get('v_out')).toBe('V(out)');
    expect(descriptions.get('v_ds')).toBe('V(drain-source)');
  });
});

describe('sheetGuides: buildNetWirePoints & findNearestPointOnNet', () => {
  const wires: EditorWire[] = [
    makeWire({
      index: 0,
      type: 'LK',
      label: 'rail_out',
      points: [
        [20, 10],
        [30, 10],
        [40, 10],
      ],
    }),
  ];

  it('aggregates wire coordinates indexed by net label', () => {
    const map = buildNetWirePoints(wires);
    expect(map.get('rail_out')).toHaveLength(3);
  });

  it('finds the wire vertex closest to a given coordinate', () => {
    const netWirePoints = buildNetWirePoints(wires);
    const netTerminalPoints = new Map<string, { x: number; y: number }>();

    // Closest to (31, 20) is (30, 10)
    const nearest = findNearestPointOnNet(
      'rail_out',
      { x: 31, y: 20 },
      netWirePoints,
      netTerminalPoints,
    );
    expect(nearest).toEqual({ x: 30, y: 10 });
  });

  it('falls back to terminal point if no wires exist for the net', () => {
    const netWirePoints = new Map<string, { x: number; y: number }[]>();
    const netTerminalPoints = new Map<string, { x: number; y: number }>([
      ['bare_net', { x: 15, y: 25 }],
    ]);

    const nearest = findNearestPointOnNet(
      'bare_net',
      { x: 50, y: 50 },
      netWirePoints,
      netTerminalPoints,
    );
    expect(nearest).toEqual({ x: 15, y: 25 });
  });
});

describe('sheetGuides: buildScopeGuides', () => {
  const inductor = makeComp({
    type: LkComponentType.INDUCTOR,
    name: 'L.1',
    family: 'LK',
    position: [20, 10],
    orientation: Orientation.WEST_EAST,
  });

  const ammeter = makeComp({
    type: ControlComponentType.AMMETER,
    name: 'AMP.1',
    family: 'CONTROL',
    position: [20, 25],
    orientation: Orientation.WEST_EAST,
    parameters: { coupledComponent: 'L.1' },
    outputLabels: ['i_L'],
  });

  const voltmeter = makeComp({
    type: ControlComponentType.VOLTMETER,
    name: 'VOLT.1',
    family: 'CONTROL',
    position: [40, 25],
    orientation: Orientation.WEST_EAST,
    parameters: { positiveNode: 'out' },
    outputLabels: ['v_out'],
  });

  const scope = makeComp({
    type: ControlComponentType.SCOPE,
    name: 'SCOPE.1',
    family: 'CONTROL',
    position: [30, 40],
    orientation: Orientation.NORTH_SOUTH,
    inputLabels: ['v_out', 'i_L', 'v_rail', 'phys_wired'],
  });

  const components = [inductor, ammeter, voltmeter, scope];
  const netTerminalPoints = new Map<string, { x: number; y: number }>([
    ['out', { x: 40, y: 10 }],
    ['v_rail', { x: 50, y: 10 }],
  ]);
  const netWirePoints = new Map<string, { x: number; y: number }[]>([
    ['v_rail', [{ x: 50, y: 10 }, { x: 30, y: 10 }]],
  ]);
  const signalEmitters = buildSignalEmitters(components);
  const signalDescriptions = buildSignalDescriptions(components);

  it('builds measurement guidelines for unwired scope channels', () => {
    // Wire fourth pin directly
    const wires: EditorWire[] = [
      makeWire({
        index: 0,
        type: 'CONTROL',
        points: [[28, 43], [20, 43]], // touches terminal at (28, 43)
      }),
    ];

    const guides = buildScopeGuides({
      components,
      wires,
      selection: ['SCOPE.1'],
      hoveredComponentName: null,
      netTerminalPoints,
      netWirePoints,
      signalEmitters,
      signalDescriptions,
    });

    expect(guides).toHaveLength(1);
    const scopeGuide = guides[0];
    expect(scopeGuide.key).toBe('SCOPE.1');
    expect(scopeGuide.isHoveredOrSelected).toBe(true);

    // Channels:
    // 0: v_out (voltmeter signal) -> target at netTerminalPoints('out') = (40, 10), label V(out)
    // 1: i_L (ammeter signal) -> target at coupled component L.1 = (20, 10), label I(L.1)
    // 2: v_rail (bare net) -> nearest point on wire rail = (30, 10), label V(v_rail)
    // 3: phys_wired -> skipped because physically wired
    expect(scopeGuide.lines).toHaveLength(3);

    expect(scopeGuide.lines[0].text).toBe('V(out)');
    expect(scopeGuide.lines[0].target).toEqual({ x: 40, y: 10 });
    expect(scopeGuide.lines[0].colorIndex).toBe(0);

    expect(scopeGuide.lines[1].text).toBe('I(L.1)');
    expect(scopeGuide.lines[1].target).toEqual({ x: 20, y: 10 });
    expect(scopeGuide.lines[1].colorIndex).toBe(1);

    expect(scopeGuide.lines[2].text).toBe('V(v_rail)');
    expect(scopeGuide.lines[2].target).toEqual({ x: 30, y: 10 });
    expect(scopeGuide.lines[2].colorIndex).toBe(2);
  });
});

describe('sheetGuides: resolveChannelDisplayLabel & buildTerminalLabelItems', () => {
  it('formats channel display label for scopes and keeps raw label for others', () => {
    const scope = makeComp({
      type: ControlComponentType.SCOPE,
      name: 'SCOPE.1',
      family: 'CONTROL',
      position: [10, 10],
      orientation: Orientation.NORTH_SOUTH,
    });
    const resistor = makeComp({
      type: LkComponentType.RESISTOR,
      name: 'R.1',
      family: 'LK',
      position: [20, 20],
      orientation: Orientation.WEST_EAST,
    });

    const descriptions = new Map<string, string>([['sig1', 'I(L.1)']]);
    const netPoints = new Map<string, { x: number; y: number }>([['node1', { x: 5, y: 5 }]]);

    expect(resolveChannelDisplayLabel(scope, 'sig1', descriptions, netPoints)).toBe('I(L.1)');
    expect(resolveChannelDisplayLabel(scope, 'node1', descriptions, netPoints)).toBe('V(node1)');
    expect(resolveChannelDisplayLabel(resistor, 'sig1', descriptions, netPoints)).toBe('sig1');
  });

  it('builds deduplicated terminal net label items with wiring status', () => {
    const resistor = makeComp({
      type: LkComponentType.RESISTOR,
      name: 'R.1',
      family: 'LK',
      position: [20, 20],
      orientation: Orientation.WEST_EAST,
      inputLabels: ['in_node'],
      outputLabels: ['out_node'],
    });

    const wires: EditorWire[] = [
      makeWire({
        index: 0,
        type: 'LK',
        points: [[18, 20], [10, 20]], // touches input terminal at (18, 20)
      }),
    ];

    const items = buildTerminalLabelItems([resistor], wires, new Map(), new Map());
    expect(items).toHaveLength(2);

    const inItem = items.find((i) => i.label === 'in_node');
    expect(inItem?.isWired).toBe(true);

    const outItem = items.find((i) => i.label === 'out_node');
    expect(outItem?.isWired).toBe(false);
  });
});
