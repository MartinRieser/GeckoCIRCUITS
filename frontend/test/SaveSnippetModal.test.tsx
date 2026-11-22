// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { SaveSnippetModal } from '../src/modals/SaveSnippetModal';

describe('SaveSnippetModal', () => {
  afterEach(() => {
    cleanup();
  });

  it('validates non-empty snippet name and submits trimmed metadata', () => {
    const onSave = vi.fn();
    const onClose = vi.fn();

    render(
      <SaveSnippetModal
        isOpen
        onClose={onClose}
        selectedComponentsCount={3}
        selectedWiresCount={2}
        internalCouplingsCount={1}
        onSave={onSave}
      />,
    );

    expect(screen.getByTestId('snippet-selection-summary').textContent).toContain(
      '3 components, 2 wires, 1 internal coupling',
    );

    // Attempt empty submit
    fireEvent.click(screen.getByTestId('confirm-save-snippet-btn'));
    expect(screen.getByTestId('snippet-name-error')).toBeTruthy();
    expect(onSave).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId('snippet-name-input'), {
      target: { value: '  Sync Buck Stage  ' },
    });
    fireEvent.change(screen.getByTestId('snippet-category-select'), {
      target: { value: 'Filters' },
    });
    fireEvent.change(screen.getByTestId('snippet-desc-input'), {
      target: { value: 'Output LC section' },
    });

    fireEvent.click(screen.getByTestId('confirm-save-snippet-btn'));
    expect(onSave).toHaveBeenCalledWith({
      name: 'Sync Buck Stage',
      category: 'Filters',
      description: 'Output LC section',
    });
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
