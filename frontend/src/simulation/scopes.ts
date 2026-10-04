/**
 * Scope-instrument helpers shared by the canvas, properties panels,
 * simulation drawer, and scope tabs.
 */
import type { EditorComponent } from '../model/types';
import { ControlComponentType } from '../model/constants';

/**
 * Checks whether a given circuit component is an oscilloscope instrument block,
 * either by CONTROL-domain type (legacy typ 5 or modern typ 1003) or by
 * conventional prefix ('SCOPE', 'OSZI'). The type check requires the CONTROL
 * family: the legacy scope shares typ 5 with the LK current source, which must
 * not be mistaken for an instrument.
 *
 * @param component The schematic component to test, or null/undefined.
 * @returns True if the component represents an oscilloscope instrument.
 */
export function isScopeComponent(component: EditorComponent | null | undefined): boolean {
  if (!component) return false;
  const name = (component.name || '').toUpperCase();
  if (name.startsWith('SCOPE') || name.startsWith('OSZI')) {
    return true;
  }
  return (
    component.family === 'CONTROL' &&
    (component.type === ControlComponentType.LEGACY_SCOPE ||
      component.type === ControlComponentType.SCOPE)
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

/**
 * Channels strictly wired to one scope: its input labels that match recorded
 * signals. Unlike {@link scopeChannels} there is no fallback — an unwired
 * scope yields an empty list, which is what persistence and per-scope
 * isolation logic need (never touch another scope's signals).
 */
export function strictScopeChannels(
  scopeBlock: EditorComponent | null | undefined,
  signalNames: string[],
): string[] {
  if (!scopeBlock) {
    return [];
  }
  return scopeBlock.inputLabels.filter((l) => l && signalNames.includes(l));
}

/**
 * Channels of a scope for display: its wired input labels that match recorded
 * signals, falling back to all recorded signals when nothing matches so an
 * unwired scope still shows the run results.
 */
export function scopeChannels(
  scopeBlock: EditorComponent | null | undefined,
  signalNames: string[],
): string[] {
  const channels = strictScopeChannels(scopeBlock, signalNames);
  if (channels.length > 0) {
    return channels;
  }
  return signalNames;
}

/** Case-insensitive substring filter; empty or blank query returns all channels. */
export function filterChannels(channels: string[], query: string): string[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return channels;
  }
  return channels.filter((s) => s.toLowerCase().includes(q));
}

/**
 * Checks whether a given circuit component is a script/function block,
 * either by type (modern 1016 or legacy 61) or by conventional prefix ('SCRIPT').
 *
 * @param component The schematic component to test, or null/undefined.
 * @returns True if the component represents a script block.
 */
export function isScriptComponent(component: EditorComponent | null | undefined): boolean {
  if (!component) return false;
  const name = (component.name || '').toUpperCase();
  return (
    component.type === ControlComponentType.SCRIPT ||
    component.type === ControlComponentType.LEGACY_JAVA_FUNCTION ||
    name.startsWith('SCRIPT') ||
    name.startsWith('JAVA_')
  );
}

/**
 * Filters a list of circuit components down to only the script/function blocks.
 *
 * @param components Array of schematic components.
 * @returns Array of script/function block components.
 */
export function findScriptBlocks(components: EditorComponent[]): EditorComponent[] {
  return components.filter(isScriptComponent);
}
