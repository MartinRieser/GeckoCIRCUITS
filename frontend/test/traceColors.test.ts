import { describe, it, expect } from 'vitest';
import {
  CHANNEL_TRACE_COLORS,
  CHANNEL_TRACE_COLORS_LIGHT,
  channelColorByIndex,
  signalColorInList,
} from '../src/simulation/traceColors';

describe('traceColors', () => {
  it('returns valid colors for each index in dark theme', () => {
    expect(channelColorByIndex(0, 'dark')).toBe(CHANNEL_TRACE_COLORS[0]);
    expect(channelColorByIndex(1, 'dark')).toBe(CHANNEL_TRACE_COLORS[1]);
  });

  it('returns valid colors for light theme', () => {
    expect(channelColorByIndex(0, 'light')).toBe(CHANNEL_TRACE_COLORS_LIGHT[0]);
    expect(channelColorByIndex(1, 'light')).toBe(CHANNEL_TRACE_COLORS_LIGHT[1]);
  });

  it('wraps around periodically for indices larger than palette length', () => {
    const len = CHANNEL_TRACE_COLORS.length;
    expect(channelColorByIndex(len, 'dark')).toBe(CHANNEL_TRACE_COLORS[0]);
    expect(channelColorByIndex(len + 3, 'dark')).toBe(CHANNEL_TRACE_COLORS[3]);
  });

  it('correctly handles negative indices', () => {
    const len = CHANNEL_TRACE_COLORS.length;
    expect(channelColorByIndex(-1, 'dark')).toBe(CHANNEL_TRACE_COLORS[len - 1]);
    expect(channelColorByIndex(-2, 'dark')).toBe(CHANNEL_TRACE_COLORS[len - 2]);
  });

  it('resolves signal color in ordered signal name list', () => {
    const signals = ['v_out', 'i_l', 'v_in'];
    expect(signalColorInList('v_out', signals, 'dark')).toBe(CHANNEL_TRACE_COLORS[0]);
    expect(signalColorInList('i_l', signals, 'dark')).toBe(CHANNEL_TRACE_COLORS[1]);
    expect(signalColorInList('v_in', signals, 'dark')).toBe(CHANNEL_TRACE_COLORS[2]);
  });

  it('falls back to list length index for unknown signal names', () => {
    const signals = ['v_out', 'i_l'];
    expect(signalColorInList('unknown_signal', signals, 'dark')).toBe(CHANNEL_TRACE_COLORS[2]);
  });
});
