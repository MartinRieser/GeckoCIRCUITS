// @vitest-environment jsdom
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { SaveAsModal } from '../src/modals/SaveAsModal';

describe('SaveAsModal Component', () => {
  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <SaveAsModal
        isOpen={false}
        defaultFilename="test.ipes"
        onSave={vi.fn()}
        onClose={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders with prefilled filename when isOpen is true', () => {
    render(
      <SaveAsModal
        isOpen={true}
        defaultFilename="my_circuit.ipes"
        onSave={vi.fn()}
        onClose={vi.fn()}
      />,
    );
    const input = screen.getByRole('textbox') as HTMLInputElement;
    expect(input.value).toBe('my_circuit.ipes');
  });

  it('automatically appends .ipes if omitted by the user', () => {
    const onSave = vi.fn();
    const onClose = vi.fn();
    render(
      <SaveAsModal
        isOpen={true}
        defaultFilename="circuit.ipes"
        onSave={onSave}
        onClose={onClose}
      />,
    );
    const input = screen.getByRole('textbox') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'interleaved_boost' } });

    const saveBtn = screen.getByRole('button', { name: /save circuit/i });
    fireEvent.click(saveBtn);

    expect(onSave).toHaveBeenCalledWith('interleaved_boost.ipes');
    expect(onClose).toHaveBeenCalled();
  });

  it('preserves .ipes extension when user provides it', () => {
    const onSave = vi.fn();
    const onClose = vi.fn();
    render(
      <SaveAsModal
        isOpen={true}
        defaultFilename="circuit.ipes"
        onSave={onSave}
        onClose={onClose}
      />,
    );
    const input = screen.getByRole('textbox') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'custom_power.ipes' } });

    const saveBtn = screen.getByRole('button', { name: /save circuit/i });
    fireEvent.click(saveBtn);

    expect(onSave).toHaveBeenCalledWith('custom_power.ipes');
    expect(onClose).toHaveBeenCalled();
  });

  it('calls onClose when clicking Cancel button or Escape key', () => {
    const onClose = vi.fn();
    const { container } = render(
      <SaveAsModal
        isOpen={true}
        defaultFilename="test.ipes"
        onSave={vi.fn()}
        onClose={onClose}
      />,
    );

    const cancelBtn = screen.getByRole('button', { name: /cancel/i });
    fireEvent.click(cancelBtn);
    expect(onClose).toHaveBeenCalledTimes(1);

    const backdrop = container.querySelector('.modal-backdrop')!;
    fireEvent.keyDown(backdrop, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
