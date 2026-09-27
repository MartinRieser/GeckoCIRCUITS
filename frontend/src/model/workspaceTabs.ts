/**
 * Workspace tab identifier helpers for the dynamic multi-tab bar.
 *
 * Tabs are strictly derived from circuit components: the permanent schematic
 * tab, one tab per oscilloscope block, and one tab per script/function block.
 * These helpers build and decompose the prefixed tab ids in one place so the
 * App component and unit tests share the exact same semantics.
 */

/** Tab id prefix for oscilloscope instrument tabs (e.g. 'scope:SCOPE.1'). */
export const SCOPE_TAB_PREFIX = 'scope:';

/** Tab id prefix for script/function block tabs (e.g. 'script:SCRIPT.1'). */
export const SCRIPT_TAB_PREFIX = 'script:';

/** Union of all valid workspace tab ids. */
export type WorkspaceTabId = 'schematic' | 'simulation' | `scope:${string}` | `script:${string}`;

/** Builds the tab id of a scope instrument tab. */
export function scopeTabId(name: string): string {
  return `${SCOPE_TAB_PREFIX}${name}`;
}

/** Builds the tab id of a script/function block tab. */
export function scriptTabId(name: string): string {
  return `${SCRIPT_TAB_PREFIX}${name}`;
}

/**
 * Returns the component name targeted by a prefixed tab id, or null when the
 * id is the schematic/simulation form or uses a different prefix.
 */
export function tabTargetName(tabId: string, prefix: string): string | null {
  return tabId.startsWith(prefix) ? tabId.slice(prefix.length) : null;
}

/**
 * Detects a single component rename between two component-name lists:
 * exactly one name removed and exactly one different name added. Any other
 * change (pure addition, pure deletion, multiple changes) is not
 * unambiguously a rename and yields null.
 *
 * @returns The { from, to } rename mapping, or null.
 */
export function detectRename(
  prevNames: string[],
  nextNames: string[],
): { from: string; to: string } | null {
  const next = new Set(nextNames);
  const removed = prevNames.filter((n) => !next.has(n));
  if (removed.length !== 1) {
    return null;
  }
  const prev = new Set(prevNames);
  const added = nextNames.filter((n) => !prev.has(n));
  if (added.length !== 1) {
    return null;
  }
  return { from: removed[0], to: added[0] };
}
