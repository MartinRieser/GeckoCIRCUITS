// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { ExamplesModal } from '../src/modals/ExamplesModal';
import * as desktop from '../src/desktop';

describe('ExamplesModal Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders null when isOpen is false', () => {
    const { container } = render(
      <ExamplesModal
        isOpen={false}
        onClose={vi.fn()}
        onSelectExample={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders modal dialog with header, search, category pills, and all examples when open', () => {
    render(
      <ExamplesModal
        isOpen={true}
        onClose={vi.fn()}
        onSelectExample={vi.fn()}
      />,
    );

    expect(screen.getByText('Example Circuits Library')).toBeDefined();
    expect(screen.getByPlaceholderText(/search examples/i)).toBeDefined();
    expect(screen.getByText('Open Folder in Explorer')).toBeDefined();

    // Category pills
    const allPill = Array.from(document.querySelectorAll<HTMLButtonElement>('.category-pill')).find(
      (b) => b.textContent?.includes('All'),
    );
    expect(allPill).toBeDefined();
    const pills = document.querySelectorAll('.category-pill');
    expect(pills.length).toBe(3);

    // Check that some known examples are listed
    expect(screen.getByText('DC-DC Buck Converter (Sampled 0.5ms Control)')).toBeDefined();
    expect(screen.getByText('Closed-Loop SPWM Inverter with Dead-Time (50 Hz)')).toBeDefined();
  });

  it('filters examples by search query', () => {
    render(
      <ExamplesModal
        isOpen={true}
        onClose={vi.fn()}
        onSelectExample={vi.fn()}
      />,
    );

    const input = screen.getByPlaceholderText(/search examples/i);
    fireEvent.change(input, { target: { value: 'resonant' } });

    expect(screen.getByText('RLC Resonant Circuit')).toBeDefined();
    expect(screen.queryByText('DC-DC Buck Converter (Sampled 0.5ms Control)')).toBeNull();

    // Clear search
    const clearBtn = screen.getByTitle('Clear search');
    fireEvent.click(clearBtn);

    expect(screen.getByText('DC-DC Buck Converter (Sampled 0.5ms Control)')).toBeDefined();
  });

  it('filters examples by category selection', () => {
    render(
      <ExamplesModal
        isOpen={true}
        onClose={vi.fn()}
        onSelectExample={vi.fn()}
      />,
    );

    const analogPill = Array.from(document.querySelectorAll<HTMLButtonElement>('.category-pill')).find(
      (b) => b.textContent?.includes('Analog & Filter'),
    )!;
    fireEvent.click(analogPill);

    // Should show analog examples
    expect(screen.getByText('RC Low-Pass Filter')).toBeDefined();
    expect(screen.getByText('RLC Resonant Circuit')).toBeDefined();

    // Power electronics examples should be hidden
    expect(screen.queryByText('DC-DC Buck Converter (Sampled 0.5ms Control)')).toBeNull();
  });

  it('calls onSelectExample when an example card is clicked', () => {
    const onSelect = vi.fn();
    render(
      <ExamplesModal
        isOpen={true}
        onClose={vi.fn()}
        onSelectExample={onSelect}
      />,
    );

    const buckCard = screen.getByText('DC-DC Buck Converter (Sampled 0.5ms Control)').closest('.example-card')!;
    fireEvent.click(buckCard);

    expect(onSelect).toHaveBeenCalledWith('buck', expect.any(String), expect.any(String));
  });

  it('calls onSelectExample on Enter key on an example card', () => {
    const onSelect = vi.fn();
    render(
      <ExamplesModal
        isOpen={true}
        onClose={vi.fn()}
        onSelectExample={onSelect}
      />,
    );

    const buckCard = screen.getByText('DC-DC Buck Converter (Sampled 0.5ms Control)').closest('.example-card')!;
    fireEvent.keyDown(buckCard, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith('buck', expect.any(String), expect.any(String));
  });

  it('calls onClose when close button or Escape key is pressed', () => {
    const onClose = vi.fn();
    render(
      <ExamplesModal
        isOpen={true}
        onClose={onClose}
        onSelectExample={vi.fn()}
      />,
    );

    const closeBtn = screen.getByLabelText('Close');
    fireEvent.click(closeBtn);
    expect(onClose).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it('invokes openExamplesFolder when Open Folder in Explorer is clicked', async () => {
    const openFolderSpy = vi.spyOn(desktop, 'openExamplesFolder').mockResolvedValue(true);

    render(
      <ExamplesModal
        isOpen={true}
        onClose={vi.fn()}
        onSelectExample={vi.fn()}
      />,
    );

    const btn = screen.getByText('Open Folder in Explorer');
    await act(async () => {
      fireEvent.click(btn);
    });

    expect(openFolderSpy).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/Opened examples folder in file explorer/i)).toBeDefined();
  });

  it('triggers onAddNewExample when Add Example button is clicked', () => {
    const onAdd = vi.fn();
    render(
      <ExamplesModal
        isOpen={true}
        onClose={vi.fn()}
        onSelectExample={vi.fn()}
        onAddNewExample={onAdd}
      />,
    );

    const addBtn = screen.getByText('Add Example');
    fireEvent.click(addBtn);
    expect(onAdd).toHaveBeenCalledTimes(1);
  });

  it('opens edit modal when clicking edit button on a card', () => {
    render(
      <ExamplesModal
        isOpen={true}
        onClose={vi.fn()}
        onSelectExample={vi.fn()}
      />,
    );

    const editBtn = screen.getByLabelText(/Edit DC-DC Buck Converter/i);
    fireEvent.click(editBtn);

    expect(screen.getByText('Edit Example')).toBeDefined();
  });
});

