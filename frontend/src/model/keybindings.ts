/**
 * Central Keybinding Definitions and Resolution.
 *
 * Provides a single data-driven registry of keyboard shortcuts across all editor modes
 * (idle, placing, wiring, dragging).
 */
import type { EditorMode } from './store';

export type ActionId =
  | 'ghost-move-up'
  | 'ghost-move-down'
  | 'ghost-move-left'
  | 'ghost-move-right'
  | 'ghost-place'
  | 'ghost-cancel'
  | 'ghost-rotate-cw'
  | 'ghost-rotate-ccw'
  | 'selection-nudge-up'
  | 'selection-nudge-down'
  | 'selection-nudge-left'
  | 'selection-nudge-right'
  | 'duplicate'
  | 'terminal-cycle-next'
  | 'terminal-cycle-prev'
  | 'wire-start-or-commit'
  | 'wire-step-up'
  | 'wire-step-down'
  | 'wire-step-left'
  | 'wire-step-right'
  | 'wire-abort'
  | 'undo'
  | 'redo'
  | 'delete'
  | 'command-palette'
  | 'toggle-wire-mode'
  | 'rotate-selection'
  | 'save'
  | 'toggle-inspector'
  | 'toggle-palette'
  | 'toggle-simulation'
  | 'show-shortcuts-help';

export interface KeyBinding {
  action: ActionId;
  key: string;
  modifiers?: {
    ctrlOrMeta?: boolean;
    shift?: boolean;
    alt?: boolean;
  };
  modes?: EditorMode[];
  description: string;
  category: 'General' | 'Placement' | 'Wiring' | 'Editing' | 'Navigation';
}

export const KEYBINDINGS: KeyBinding[] = [
  // General & App
  {
    action: 'command-palette',
    key: 'k',
    modifiers: { ctrlOrMeta: true },
    description: 'Open Command Palette',
    category: 'General',
  },
  {
    action: 'toggle-inspector',
    key: 'i',
    modifiers: { ctrlOrMeta: true },
    description: 'Toggle Inspector / Properties Panel',
    category: 'General',
  },
  {
    action: 'toggle-palette',
    key: 'b',
    modifiers: { ctrlOrMeta: true },
    description: 'Toggle Components Palette',
    category: 'General',
  },
  {
    action: 'save',
    key: 's',
    modifiers: { ctrlOrMeta: true },
    description: 'Save Circuit File (.ipes)',
    category: 'General',
  },
  {
    action: 'show-shortcuts-help',
    key: '?',
    description: 'Toggle Keyboard Shortcuts Cheatsheet',
    category: 'General',
  },
  {
    action: 'ghost-cancel',
    key: 'Escape',
    description: 'Cancel action / Close dialogs / Deselect',
    category: 'General',
  },
  {
    action: 'undo',
    key: 'z',
    modifiers: { ctrlOrMeta: true },
    description: 'Undo',
    category: 'Editing',
  },
  {
    action: 'redo',
    key: 'y',
    modifiers: { ctrlOrMeta: true },
    description: 'Redo',
    category: 'Editing',
  },
  {
    action: 'redo',
    key: 'z',
    modifiers: { ctrlOrMeta: true, shift: true },
    description: 'Redo (Shift+Ctrl+Z)',
    category: 'Editing',
  },
  {
    action: 'delete',
    key: 'Delete',
    modes: ['idle', 'dragging'],
    description: 'Delete Selected Components / Wires',
    category: 'Editing',
  },
  {
    action: 'delete',
    key: 'Backspace',
    modes: ['idle', 'dragging'],
    description: 'Delete Selected (Backspace)',
    category: 'Editing',
  },
  {
    action: 'duplicate',
    key: 'd',
    modifiers: { ctrlOrMeta: true },
    modes: ['idle', 'dragging'],
    description: 'Duplicate Selected Components',
    category: 'Editing',
  },
  {
    action: 'rotate-selection',
    key: 'r',
    modes: ['idle', 'dragging'],
    description: 'Rotate Selected Components by 90°',
    category: 'Editing',
  },
  {
    action: 'toggle-wire-mode',
    key: 'w',
    description: 'Toggle Wire Pen Mode',
    category: 'Wiring',
  },

  // Placing Mode (Armed Ghost)
  {
    action: 'ghost-move-up',
    key: 'ArrowUp',
    modes: ['placing'],
    description: 'Move Ghost Up (1 step / Shift: 5 steps)',
    category: 'Placement',
  },
  {
    action: 'ghost-move-down',
    key: 'ArrowDown',
    modes: ['placing'],
    description: 'Move Ghost Down',
    category: 'Placement',
  },
  {
    action: 'ghost-move-left',
    key: 'ArrowLeft',
    modes: ['placing'],
    description: 'Move Ghost Left',
    category: 'Placement',
  },
  {
    action: 'ghost-move-right',
    key: 'ArrowRight',
    modes: ['placing'],
    description: 'Move Ghost Right',
    category: 'Placement',
  },
  {
    action: 'ghost-rotate-cw',
    key: 'r',
    modes: ['placing'],
    description: 'Rotate Ghost 90° Clockwise',
    category: 'Placement',
  },
  {
    action: 'ghost-rotate-ccw',
    key: 'r',
    modifiers: { shift: true },
    modes: ['placing'],
    description: 'Rotate Ghost 90° Counter-Clockwise',
    category: 'Placement',
  },
  {
    action: 'ghost-place',
    key: 'Enter',
    modes: ['placing'],
    description: 'Place Ghost at Current Grid Position',
    category: 'Placement',
  },
  {
    action: 'ghost-place',
    key: ' ',
    modes: ['placing'],
    description: 'Place Ghost at Current Position (Space)',
    category: 'Placement',
  },

  // Wiring Mode
  {
    action: 'terminal-cycle-next',
    key: 'Tab',
    description: 'Cycle Next Terminal / Component',
    category: 'Navigation',
  },
  {
    action: 'terminal-cycle-prev',
    key: 'Tab',
    modifiers: { shift: true },
    description: 'Cycle Previous Terminal / Component',
    category: 'Navigation',
  },
  {
    action: 'wire-start-or-commit',
    key: 'Enter',
    modes: ['wiring'],
    description: 'Start Wire at Terminal or Commit Route',
    category: 'Wiring',
  },
  {
    action: 'wire-start-or-commit',
    key: ' ',
    modes: ['wiring'],
    description: 'Start Wire or Commit Route (Space)',
    category: 'Wiring',
  },
  {
    action: 'wire-step-up',
    key: 'ArrowUp',
    modes: ['wiring'],
    description: 'Extend Wire Route Up',
    category: 'Wiring',
  },
  {
    action: 'wire-step-down',
    key: 'ArrowDown',
    modes: ['wiring'],
    description: 'Extend Wire Route Down',
    category: 'Wiring',
  },
  {
    action: 'wire-step-left',
    key: 'ArrowLeft',
    modes: ['wiring'],
    description: 'Extend Wire Route Left',
    category: 'Wiring',
  },
  {
    action: 'wire-step-right',
    key: 'ArrowRight',
    modes: ['wiring'],
    description: 'Extend Wire Route Right',
    category: 'Wiring',
  },
  {
    action: 'wire-abort',
    key: 'Escape',
    modes: ['wiring'],
    description: 'Abort Wire Draft / Leave Wire Mode',
    category: 'Wiring',
  },

  // Selection Nudge in Idle & Dragging Mode
  {
    action: 'selection-nudge-up',
    key: 'ArrowUp',
    modes: ['idle', 'dragging'],
    description: 'Nudge Selection Up (1 step / Shift: 5 steps)',
    category: 'Editing',
  },
  {
    action: 'selection-nudge-down',
    key: 'ArrowDown',
    modes: ['idle', 'dragging'],
    description: 'Nudge Selection Down',
    category: 'Editing',
  },
  {
    action: 'selection-nudge-left',
    key: 'ArrowLeft',
    modes: ['idle', 'dragging'],
    description: 'Nudge Selection Left',
    category: 'Editing',
  },
  {
    action: 'selection-nudge-right',
    key: 'ArrowRight',
    modes: ['idle', 'dragging'],
    description: 'Nudge Selection Right',
    category: 'Editing',
  },
];

export interface KeyboardEventLike {
  key: string;
  ctrlKey?: boolean;
  metaKey?: boolean;
  shiftKey?: boolean;
  altKey?: boolean;
}

/**
 * Resolves a keyboard event to an ActionId based on current editor mode and active selection.
 */
export function resolveShortcut(
  e: KeyboardEventLike,
  mode: EditorMode,
  hasSelection = false,
  _hasDraft = false,
): ActionId | null {
  const isCtrlOrMeta = !!(e.ctrlKey || e.metaKey);
  const isShift = !!e.shiftKey;
  const isAlt = !!e.altKey;
  const key = e.key;

  // First pass: try mode-specific bindings that match current mode
  for (const binding of KEYBINDINGS) {
    if (!binding.modes || !binding.modes.includes(mode)) {
      continue;
    }

    if (binding.action.startsWith('selection-nudge-') && !hasSelection) {
      continue;
    }

    const keyMatch = binding.key.toLowerCase() === key.toLowerCase() || binding.key === key;
    if (!keyMatch) continue;

    const reqCtrlOrMeta = !!binding.modifiers?.ctrlOrMeta;
    const reqShift = !!binding.modifiers?.shift;
    const reqAlt = !!binding.modifiers?.alt;

    if (reqCtrlOrMeta !== isCtrlOrMeta) continue;
    if (reqShift !== isShift) continue;
    if (reqAlt !== isAlt) continue;

    return binding.action;
  }

  // Second pass: try global bindings (modes undefined)
  for (const binding of KEYBINDINGS) {
    if (binding.modes) {
      continue;
    }

    const keyMatch = binding.key.toLowerCase() === key.toLowerCase() || binding.key === key;
    if (!keyMatch) continue;

    const reqCtrlOrMeta = !!binding.modifiers?.ctrlOrMeta;
    const reqShift = !!binding.modifiers?.shift;
    const reqAlt = !!binding.modifiers?.alt;

    if (reqCtrlOrMeta !== isCtrlOrMeta) continue;
    if (reqShift !== isShift) continue;
    if (reqAlt !== isAlt) continue;

    return binding.action;
  }

  return null;
}
