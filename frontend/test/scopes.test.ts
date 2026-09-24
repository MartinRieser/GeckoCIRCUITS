import { describe, it, expect } from 'vitest';
import {
  isScopeComponent,
  findScopeBlocks,
  scopeChannels,
  filterChannels,
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
});
