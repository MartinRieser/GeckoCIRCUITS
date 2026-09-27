import { describe, it, expect } from 'vitest';
import {
  SCOPE_TAB_PREFIX,
  SCRIPT_TAB_PREFIX,
  scopeTabId,
  scriptTabId,
  tabTargetName,
  detectRename,
} from '../src/model/workspaceTabs';

describe('workspace tab id helpers', () => {
  it('builds prefixed tab ids for scopes and scripts', () => {
    expect(scopeTabId('SCOPE.1')).toBe('scope:SCOPE.1');
    expect(scriptTabId('SCRIPT.1')).toBe('script:SCRIPT.1');
    expect(SCOPE_TAB_PREFIX).toBe('scope:');
    expect(SCRIPT_TAB_PREFIX).toBe('script:');
  });

  it('extracts the target name only for the matching prefix', () => {
    expect(tabTargetName('scope:SCOPE.1', SCOPE_TAB_PREFIX)).toBe('SCOPE.1');
    expect(tabTargetName('script:SCRIPT.1', SCRIPT_TAB_PREFIX)).toBe('SCRIPT.1');
    expect(tabTargetName('schematic', SCOPE_TAB_PREFIX)).toBeNull();
    expect(tabTargetName('schematic', SCRIPT_TAB_PREFIX)).toBeNull();
    expect(tabTargetName('script:SCRIPT.1', SCOPE_TAB_PREFIX)).toBeNull();
    expect(tabTargetName('scope:SCOPE.1', SCRIPT_TAB_PREFIX)).toBeNull();
  });
});

describe('detectRename', () => {
  it('detects a single rename', () => {
    expect(detectRename(['R1', 'SCOPE.1', 'SCRIPT.1'], ['R1', 'SCOPE.2', 'SCRIPT.1'])).toEqual({
      from: 'SCOPE.1',
      to: 'SCOPE.2',
    });
  });

  it('returns null for pure additions', () => {
    expect(detectRename(['R1'], ['R1', 'R2'])).toBeNull();
  });

  it('returns null for pure deletions', () => {
    expect(detectRename(['R1', 'R2'], ['R1'])).toBeNull();
  });

  it('returns null when nothing changed', () => {
    expect(detectRename(['R1', 'R2'], ['R1', 'R2'])).toBeNull();
  });

  it('returns null for ambiguous multi-name changes', () => {
    expect(detectRename(['A', 'B', 'C'], ['X', 'Y', 'C'])).toBeNull();
    expect(detectRename(['A', 'B'], ['A', 'B', 'C', 'D'])).toBeNull();
  });

  it('returns null for empty lists', () => {
    expect(detectRename([], [])).toBeNull();
    expect(detectRename([], ['NEW'])).toBeNull();
    expect(detectRename(['OLD'], [])).toBeNull();
  });
});
