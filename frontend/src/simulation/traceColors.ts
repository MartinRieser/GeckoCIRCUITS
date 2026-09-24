/**
 * One canonical color per scope channel, shared by the schematic properties
 * badges, the scope traces, and every legend/toggle so channel N has the same
 * color everywhere. Index = channel number (CH1 = first entry).
 */

/** Palette of distinct vibrant high-contrast colors for dark themes. */
export const CHANNEL_TRACE_COLORS = [
  '#38bdf8', // Cyan
  '#4ade80', // Green
  '#f59e0b', // Amber
  '#c084fc', // Purple
  '#f43f5e', // Rose
  '#06b6d4', // Teal
  '#a855f7', // Violet
  '#fb923c', // Orange
] as const;

/** Palette of distinct high-contrast colors calibrated for light backgrounds. */
export const CHANNEL_TRACE_COLORS_LIGHT = [
  '#0284c7', // Cyan
  '#16a34a', // Green
  '#d97706', // Amber
  '#9333ea', // Purple
  '#e11d48', // Rose
  '#0f766e', // Teal
  '#7e22ce', // Violet
  '#c2410c', // Orange
] as const;

/** Color theme for waveform traces and legend badges. */
export type TraceTheme = 'dark' | 'light';

/**
 * Returns the color assigned to a channel index, wrapping periodically if there
 * are more channels than palette colors. Correctly handles negative indices.
 *
 * @param index 0-based channel index.
 * @param theme Color theme ('dark' or 'light'). Defaults to 'dark'.
 * @returns Hex color string.
 */
export function channelColorByIndex(index: number, theme: TraceTheme = 'dark'): string {
  const palette = theme === 'light' ? CHANNEL_TRACE_COLORS_LIGHT : CHANNEL_TRACE_COLORS;
  return palette[((index % palette.length) + palette.length) % palette.length];
}

/**
 * Color of a signal inside an ordered name list: per-scope that is the
 * channel order, for the combined "All Scopes & Signals" view it is the
 * result column order. Unknown names fall back to the list length so they
 * still get a stable, distinct color.
 */
export function signalColorInList(
  name: string,
  orderedNames: string[],
  theme: TraceTheme = 'dark',
): string {
  const idx = orderedNames.indexOf(name);
  return channelColorByIndex(idx >= 0 ? idx : orderedNames.length, theme);
}
