import { describe, it, expect } from 'vitest';
import {
  isScopeComponent,
  findScopeBlocks,
  scopeChannels,
  strictScopeChannels,
  resolveScopeWiredSignals,
  filterChannels,
  parseSignalGraphs,
  serializeSignalGraphs,
  inferSignalCategory,
  defaultGraphForSignal,
  resolveSignalGraph,
} from '../src/simulation/scopes';
import type { EditorComponent } from '../src/model/types';
import { ControlComponentType, LkComponentType } from '../src/model/constants';

describe('scopes helpers', () => {
  const scope1: EditorComponent = {
    type: ControlComponentType.SCOPE,
    family: 'CONTROL',
    name: 'Scope.1',
    position: [0, 0],
    orientation: 502,
    parameters: {},
    inputLabels: ['v_out', 'i_l'],
    outputLabels: [],
  };

  const scopeLegacy: EditorComponent = {
    type: ControlComponentType.LEGACY_SCOPE,
    family: 'CONTROL',
    name: 'OSZI_MAIN',
    position: [10, 0],
    orientation: 502,
    parameters: {},
    inputLabels: ['sigA'],
    outputLabels: [],
  };

  const resistor: EditorComponent = {
    type: LkComponentType.RESISTOR,
    family: 'LK',
    name: 'R.1',
    position: [20, 0],
    orientation: 502,
    parameters: {},
    inputLabels: [],
    outputLabels: [],
  };

  describe('isScopeComponent', () => {
    it('returns false for null or undefined', () => {
      expect(isScopeComponent(null)).toBe(false);
      expect(isScopeComponent(undefined)).toBe(false);
    });

    it('identifies scope by modern or legacy type', () => {
      expect(isScopeComponent(scope1)).toBe(true);
      expect(isScopeComponent(scopeLegacy)).toBe(true);
    });

    it('identifies scope by name prefix SCOPE or OSZI even with generic type', () => {
      const namedScope: EditorComponent = {
        ...resistor,
        name: 'Scope_Aux',
      };
      const namedOszi: EditorComponent = {
        ...resistor,
        name: 'Oszi_Channel1',
      };
      expect(isScopeComponent(namedScope)).toBe(true);
      expect(isScopeComponent(namedOszi)).toBe(true);
    });

    it('returns false for regular components', () => {
      expect(isScopeComponent(resistor)).toBe(false);
    });

    it('does not mistake the LK current source (shared legacy typ 5) for a scope', () => {
      const currentSource: EditorComponent = {
        ...resistor,
        type: LkComponentType.CURRENT_SOURCE,
        name: 'I',
      };
      expect(isScopeComponent(currentSource)).toBe(false);
    });
  });

  describe('findScopeBlocks', () => {
    it('filters component array to return only scope blocks', () => {
      const found = findScopeBlocks([resistor, scope1, scopeLegacy]);
      expect(found).toHaveLength(2);
      expect(found).toContain(scope1);
      expect(found).toContain(scopeLegacy);
    });

    it('returns empty array when no scopes are present', () => {
      expect(findScopeBlocks([resistor])).toEqual([]);
    });
  });

  describe('scopeChannels', () => {
    const signals = ['v_out', 'i_l', 'v_in', 'clk'];

    it('returns all signals when scopeBlock is null or undefined', () => {
      expect(scopeChannels(null, signals)).toEqual(signals);
      expect(scopeChannels(undefined, signals)).toEqual(signals);
    });

    it('returns matching wired input labels of the scope block', () => {
      expect(scopeChannels(scope1, signals)).toEqual(['v_out', 'i_l']);
    });

    it('falls back to all signals if scope block has no matching labels', () => {
      const scopeUnconnected: EditorComponent = {
        ...scope1,
        inputLabels: ['unconnected_net_1', 'unconnected_net_2'],
      };
      expect(scopeChannels(scopeUnconnected, signals)).toEqual(signals);
    });
  });

  describe('strictScopeChannels', () => {
    const signals = ['v_out', 'i_l', 'v_bulk'];

    it('returns only the wired labels that match recorded signals', () => {
      expect(strictScopeChannels(scope1, signals)).toEqual(['v_out', 'i_l']);
      expect(strictScopeChannels({ ...scope1, inputLabels: ['i_l', 'missing'] }, signals)).toEqual([
        'i_l',
      ]);
    });

    it('returns an empty list when no labels match — never another scope\'s signals', () => {
      const scopeUnconnected: EditorComponent = {
        ...scope1,
        inputLabels: ['unconnected_net_1', 'unconnected_net_2'],
      };
      expect(strictScopeChannels(scopeUnconnected, signals)).toEqual([]);
    });

    it('returns an empty list for a missing scope block', () => {
      expect(strictScopeChannels(null, signals)).toEqual([]);
    });
  });

  describe('filterChannels', () => {
    const channels = ['V_OUT', 'I_Inductor', 'V_IN', 'Gate_PWM'];

    it('returns all channels if query is empty or whitespace', () => {
      expect(filterChannels(channels, '')).toEqual(channels);
      expect(filterChannels(channels, '   ')).toEqual(channels);
    });

    it('performs case-insensitive filtering', () => {
      expect(filterChannels(channels, 'v_')).toEqual(['V_OUT', 'V_IN']);
      expect(filterChannels(channels, 'pwm')).toEqual(['Gate_PWM']);
    });

    it('returns empty array when nothing matches', () => {
      expect(filterChannels(channels, 'nonexistent')).toEqual([]);
    });
  });

  describe('resolveScopeWiredSignals', () => {
    it('returns explicit inputLabels when present', () => {
      expect(resolveScopeWiredSignals(scope1, [], [])).toEqual(['v_out', 'i_l']);
    });

    it('resolves signal from connected wire to an output terminal', () => {
      const scopeEmptyLabels: EditorComponent = {
        type: ControlComponentType.SCOPE,
        family: 'CONTROL',
        name: 'SCOPE',
        position: [16, 48],
        orientation: 503,
        parameters: {},
        inputLabels: [],
        outputLabels: [],
      };

      const voltmeter: EditorComponent = {
        type: ControlComponentType.VOLTMETER,
        family: 'CONTROL',
        name: 'VM_SW',
        position: [6, 48],
        orientation: 503,
        parameters: {},
        inputLabels: [],
        outputLabels: ['v_sw'],
      };

      // Wire from voltmeter output (8, 48) to scope input (14, 48)
      const wire = {
        points: [
          [8, 48],
          [14, 48],
        ],
      };

      const resolved = resolveScopeWiredSignals(scopeEmptyLabels, [voltmeter], [wire]);
      expect(resolved).toEqual(['v_sw']);

      // strictScopeChannels should also resolve v_sw
      const strict = strictScopeChannels(scopeEmptyLabels, ['v_sw', 'v_other'], [voltmeter], [wire]);
      expect(strict).toEqual(['v_sw']);

      // scopeChannels should display v_sw
      const displayed = scopeChannels(scopeEmptyLabels, ['v_sw', 'v_other'], [voltmeter], [wire]);
      expect(displayed).toEqual(['v_sw']);
    });
  });

  describe('signalGraphs persistence and assignment helpers', () => {
    it('parses signalGraphs string correctly', () => {
      const mapping = parseSignalGraphs('v_sw:1,v_out:1,i_L:2,duty:3');
      expect(mapping).toEqual({
        v_sw: 1,
        v_out: 1,
        i_L: 2,
        duty: 3,
      });
      expect(parseSignalGraphs(undefined)).toEqual({});
      expect(parseSignalGraphs('')).toEqual({});
    });

    it('serializes signalGraphs dictionary to sorted string', () => {
      const serialized = serializeSignalGraphs({
        duty: 3,
        v_sw: 1,
        i_L: 2,
      });
      expect(serialized).toBe('duty:3,i_L:2,v_sw:1');
    });

    it('categorizes and computes default graph based on layout', () => {
      expect(inferSignalCategory('v_out')).toBe('voltage');
      expect(inferSignalCategory('u_cap')).toBe('voltage');
      expect(inferSignalCategory('i_inductor')).toBe('current');
      expect(inferSignalCategory('duty_cycle')).toBe('control');

      // 3-graphs layout defaults
      expect(defaultGraphForSignal('v_out', '3-graphs')).toBe(1);
      expect(defaultGraphForSignal('i_inductor', '3-graphs')).toBe(2);
      expect(defaultGraphForSignal('duty_cycle', '3-graphs')).toBe(3);

      // 2-graphs layout defaults
      expect(defaultGraphForSignal('v_out', '2-graphs')).toBe(1);
      expect(defaultGraphForSignal('i_inductor', '2-graphs')).toBe(2);
      expect(defaultGraphForSignal('duty_cycle', '2-graphs')).toBe(2);
    });

    it('resolves explicit signal graph mapping with fallback', () => {
      const customMapping = { v_out: 2, i_inductor: 1 };
      // Explicit overrides
      expect(resolveSignalGraph('v_out', '3-graphs', customMapping)).toBe(2);
      expect(resolveSignalGraph('i_inductor', '3-graphs', customMapping)).toBe(1);
      // Fallback to default
      expect(resolveSignalGraph('duty', '3-graphs', customMapping)).toBe(3);
      // Clamps to max allowed in layout
      expect(resolveSignalGraph('duty', '2-graphs', { duty: 3 })).toBe(2);
    });
  });
});
