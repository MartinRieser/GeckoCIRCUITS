// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { EditExampleModal } from '../src/modals/EditExampleModal';
import type { ManagedExample } from '../src/model/examplesService';

describe('EditExampleModal Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  const dummyExample: ManagedExample = {
    id: 'test_example',
    name: 'Original Example Name',
    category: 'Power Electronics',
    description: 'Original description',
    content: 'tDURATION 0.02\n',
    custom: true,
  };

  it('renders null when isOpen is false', () => {
    const { container } = render(
      <EditExampleModal
        isOpen={false}
        example={dummyExample}
        onClose={vi.fn()}
        onSave={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders with example fields populated', () => {
    render(
      <EditExampleModal
        isOpen={true}
        example={dummyExample}
        onClose={vi.fn()}
        onSave={vi.fn()}
      />,
    );

    expect(screen.getByText('Edit Example')).toBeDefined();
    const nameInput = screen.getByLabelText(/example title/i) as HTMLInputElement;
    expect(nameInput.value).toBe('Original Example Name');
    const catInput = screen.getByLabelText(/category/i) as HTMLInputElement;
    expect(catInput.value).toBe('Power Electronics');
    const descInput = screen.getByLabelText(/description/i) as HTMLTextAreaElement;
    expect(descInput.value).toBe('Original description');
  });

  it('submits form with updated values', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    const onClose = vi.fn();

    render(
      <EditExampleModal
        isOpen={true}
        example={dummyExample}
        onClose={onClose}
        onSave={onSave}
        canUpdateContent={true}
      />,
    );

    const nameInput = screen.getByLabelText(/example title/i);
    const descInput = screen.getByLabelText(/description/i);

    fireEvent.change(nameInput, { target: { value: 'Modified Name' } });
    fireEvent.change(descInput, { target: { value: 'Modified Description' } });

    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);

    const saveBtn = screen.getByRole('button', { name: 'Save Changes' });
    await act(async () => {
      fireEvent.click(saveBtn);
    });

    expect(onSave).toHaveBeenCalledWith('test_example', {
      name: 'Modified Name',
      category: 'Power Electronics',
      description: 'Modified Description',
      updateContentWithWorkspace: true,
    });
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
