/**
 * Scope-instrument helpers shared by the canvas, properties panels,
 * simulation drawer, and scope tabs.
 */
import type { EditorComponent } from '../model/types';

/** Scope blocks: classic scope (typ 5), web scope (typ 1003), or SCOPE/OSZI name prefixes. */
export function isScopeComponent(component: EditorComponent): boolean {
  const name = component.name.toUpperCase();
  return (
    component.type === 5 ||
    component.type === 1003 ||
    name.startsWith('SCOPE') ||
    name.startsWith('OSZI')
  );
}

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
