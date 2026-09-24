/**
 * Scope-instrument helpers shared by the canvas, properties panels,
 * simulation drawer, and scope tabs.
 */
import type { EditorComponent } from '../model/types';
import { ControlComponentType } from '../model/constants';

/**
 * Checks whether a given circuit component is an oscilloscope instrument block,
 * either by type (legacy typ 5 or modern typ 1003) or by conventional prefix ('SCOPE', 'OSZI').
 *
 * @param component The schematic component to test, or null/undefined.
 * @returns True if the component represents an oscilloscope instrument.
 */
export function isScopeComponent(component: EditorComponent | null | undefined): boolean {
  if (!component) return false;
  const name = (component.name || '').toUpperCase();
  return (
    component.type === ControlComponentType.LEGACY_SCOPE ||
    component.type === ControlComponentType.SCOPE ||
    name.startsWith('SCOPE') ||
    name.startsWith('OSZI')
  );
}

/**
 * Filters a list of circuit components down to only the oscilloscope instrument blocks.
 *
 * @param components Array of schematic components.
 * @returns Array of oscilloscope instrument components.
 */
export function findScopeBlocks(components: EditorComponent[]): EditorComponent[] {
  return components.filter(isScopeComponent);
}

/** Channels of a scope: its wired input labels that match recorded signals. */
export function scopeChannels(
  scopeBlock: EditorComponent | null | undefined,
  signalNames: string[],
): string[] {
  if (!scopeBlock) {
    return signalNames;
  }
  const channels = scopeBlock.inputLabels.filter((l) => l && signalNames.includes(l));
  return channels.length > 0 ? channels : signalNames;
}

/** Case-insensitive substring filter; empty or blank query returns all channels. */
export function filterChannels(channels: string[], query: string): string[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return channels;
  }
  return channels.filter((s) => s.toLowerCase().includes(q));
}
