// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { SaveAsExampleModal } from '../src/modals/SaveAsExampleModal';

describe('SaveAsExampleModal Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders null when isOpen is false', () => {
    const { container } = render(
      <SaveAsExampleModal
        isOpen={false}
        onClose={vi.fn()}
        onSave={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders modal dialog with form fields and default name', () => {
    render(
      <SaveAsExampleModal
        isOpen={true}
        defaultName="My Buck Converter"
        onClose={vi.fn()}
        onSave={vi.fn()}
      />,
    );

    expect(screen.getByText('Save Circuit as Example')).toBeDefined();
    const nameInput = screen.getByLabelText(/example title/i) as HTMLInputElement;
    expect(nameInput.value).toBe('My Buck Converter');
    expect(screen.getByLabelText(/category/i)).toBeDefined();
    expect(screen.getByLabelText(/description/i)).toBeDefined();
  });

  it('submits form and calls onSave with trimmed inputs', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    const onClose = vi.fn();

    render(
      <SaveAsExampleModal
        isOpen={true}
        defaultName="Buck"
        onClose={onClose}
        onSave={onSave}
      />,
    );

    const nameInput = screen.getByLabelText(/example title/i);
    const catInput = screen.getByLabelText(/category/i);
    const descInput = screen.getByLabelText(/description/i);

    fireEvent.change(nameInput, { target: { value: 'Interleaved Boost' } });
    fireEvent.change(catInput, { target: { value: 'Power Converters' } });
    fireEvent.change(descInput, { target: { value: 'Dual phase interleaved topology' } });

    const submitBtn = screen.getByRole('button', { name: 'Save as Example' });
    await act(async () => {
      fireEvent.click(submitBtn);
    });

    expect(onSave).toHaveBeenCalledWith({
      name: 'Interleaved Boost',
      category: 'Power Converters',
      description: 'Dual phase interleaved topology',
    });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('closes dialog on Cancel or Escape key', () => {
    const onClose = vi.fn();
    render(
      <SaveAsExampleModal
        isOpen={true}
        onClose={onClose}
        onSave={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
