import { describe, it, expect } from 'vitest';
import { KEYBINDINGS, resolveShortcut } from '../src/model/keybindings';

describe('keybindings registry', () => {
  it('contains unique action bindings with valid descriptions', () => {
    expect(KEYBINDINGS.length).toBeGreaterThan(15);
    for (const b of KEYBINDINGS) {
      expect(b.action).toBeTruthy();
      expect(b.key).toBeTruthy();
      expect(b.description).toBeTruthy();
      expect(['General', 'Placement', 'Wiring', 'Editing', 'Navigation']).toContain(b.category);
    }
  });

  it('resolves placing mode shortcuts correctly', () => {
    expect(resolveShortcut({ key: 'ArrowUp' }, 'placing')).toBe('ghost-move-up');
    expect(resolveShortcut({ key: 'r' }, 'placing')).toBe('ghost-rotate-cw');
    expect(resolveShortcut({ key: 'r', shiftKey: true }, 'placing')).toBe('ghost-rotate-ccw');
    expect(resolveShortcut({ key: 'Enter' }, 'placing')).toBe('ghost-place');
    expect(resolveShortcut({ key: 'Escape' }, 'placing')).toBe('ghost-cancel');
  });

  it('resolves idle mode selection shortcuts correctly', () => {
    // Without selection, arrow keys in idle mode should not trigger selection nudge
    expect(resolveShortcut({ key: 'ArrowUp' }, 'idle', false)).toBeNull();
    // With selection, arrow keys trigger selection nudge
    expect(resolveShortcut({ key: 'ArrowUp' }, 'idle', true)).toBe('selection-nudge-up');

    expect(resolveShortcut({ key: 'd', ctrlKey: true }, 'idle', true)).toBe('duplicate');
    expect(resolveShortcut({ key: 'Delete' }, 'idle', true)).toBe('delete');
    expect(resolveShortcut({ key: 'r' }, 'idle', true)).toBe('rotate-selection');
  });

  it('resolves wiring mode navigation and draft shortcuts correctly', () => {
    expect(resolveShortcut({ key: 'Tab' }, 'wiring')).toBe('terminal-cycle-next');
    expect(resolveShortcut({ key: 'Tab', shiftKey: true }, 'wiring')).toBe('terminal-cycle-prev');
    expect(resolveShortcut({ key: 'Enter' }, 'wiring')).toBe('wire-start-or-commit');
    expect(resolveShortcut({ key: 'ArrowRight' }, 'wiring')).toBe('wire-step-right');
    expect(resolveShortcut({ key: 'Escape' }, 'wiring')).toBe('wire-abort');
  });

  it('resolves global shortcuts in any mode', () => {
    expect(resolveShortcut({ key: 'k', ctrlKey: true }, 'idle')).toBe('command-palette');
    expect(resolveShortcut({ key: 'k', ctrlKey: true }, 'placing')).toBe('command-palette');
    expect(resolveShortcut({ key: 'z', ctrlKey: true }, 'idle')).toBe('undo');
    expect(resolveShortcut({ key: 'y', ctrlKey: true }, 'idle')).toBe('redo');
    expect(resolveShortcut({ key: 'z', ctrlKey: true, shiftKey: true }, 'idle')).toBe('redo');
    expect(resolveShortcut({ key: 'w' }, 'idle')).toBe('toggle-wire-mode');
    expect(resolveShortcut({ key: '?' }, 'idle')).toBe('show-shortcuts-help');
  });
});
