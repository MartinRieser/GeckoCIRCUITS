// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { FileMenu } from '../src/nav/FileMenu';
import { addRecentFile, clearRecentFiles } from '../src/model/recentFiles';

describe('FileMenu Component', () => {
  beforeEach(() => {
    localStorage.clear();
    clearRecentFiles();
  });

  it('renders File trigger button', () => {
    render(
      <FileMenu
        onNew={vi.fn()}
        onOpen={vi.fn()}
        onSave={vi.fn()}
        onSaveAs={vi.fn()}
        onOpenRecent={vi.fn()}
        canSave={true}
      />,
    );
    expect(screen.getByRole('button', { name: /file/i })).toBeDefined();
  });

  it('opens and closes dropdown on clicking File button', () => {
    render(
      <FileMenu
        onNew={vi.fn()}
        onOpen={vi.fn()}
        onSave={vi.fn()}
        onSaveAs={vi.fn()}
        onOpenRecent={vi.fn()}
        canSave={true}
      />,
    );
    const trigger = screen.getByRole('button', { name: /file/i });

    // Initially closed
    expect(screen.queryByRole('menu')).toBeNull();

    // Open
    fireEvent.click(trigger);
    expect(screen.getByRole('menu')).toBeDefined();
    expect(screen.getByText('New Circuit')).toBeDefined();
    expect(screen.getByText('Open...')).toBeDefined();
    expect(screen.getByText('Save')).toBeDefined();
    expect(screen.getByText('Save As...')).toBeDefined();

    // Close
    fireEvent.click(trigger);
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('triggers onNew and closes menu', () => {
    const onNew = vi.fn();
    render(
      <FileMenu
        onNew={onNew}
        onOpen={vi.fn()}
        onSave={vi.fn()}
        onSaveAs={vi.fn()}
        onOpenRecent={vi.fn()}
        canSave={true}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /file/i }));
    fireEvent.click(screen.getByText('New Circuit'));

    expect(onNew).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('triggers onOpen and closes menu', () => {
    const onOpen = vi.fn();
    render(
      <FileMenu
        onNew={vi.fn()}
        onOpen={onOpen}
        onSave={vi.fn()}
        onSaveAs={vi.fn()}
        onOpenRecent={vi.fn()}
        canSave={true}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /file/i }));
    fireEvent.click(screen.getByText('Open...'));

    expect(onOpen).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('triggers onSave and onSaveAs', () => {
    const onSave = vi.fn();
    const onSaveAs = vi.fn();
    const { rerender } = render(
      <FileMenu
        onNew={vi.fn()}
        onOpen={vi.fn()}
        onSave={onSave}
        onSaveAs={onSaveAs}
        onOpenRecent={vi.fn()}
        canSave={true}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /file/i }));
    fireEvent.click(screen.getByText('Save'));
    expect(onSave).toHaveBeenCalledTimes(1);

    // Save As
    fireEvent.click(screen.getByRole('button', { name: /file/i }));
    fireEvent.click(screen.getByText('Save As...'));
    expect(onSaveAs).toHaveBeenCalledTimes(1);

    // Disabled when canSave is false
    rerender(
      <FileMenu
        onNew={vi.fn()}
        onOpen={vi.fn()}
        onSave={onSave}
        onSaveAs={onSaveAs}
        onOpenRecent={vi.fn()}
        canSave={false}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /file/i }));
    const saveBtn = screen.getByText('Save').closest('button') as HTMLButtonElement;
    expect(saveBtn.disabled).toBe(true);
  });

  it('displays up to 8 recent files in Open Recent flyout and selects one', () => {
    // Populate 10 files
    for (let i = 1; i <= 10; i++) {
      addRecentFile(`circuit_${i}.ipes`, `content_${i}`, i);
    }

    const onOpenRecent = vi.fn();
    render(
      <FileMenu
        onNew={vi.fn()}
        onOpen={vi.fn()}
        onSave={vi.fn()}
        onSaveAs={vi.fn()}
        onOpenRecent={onOpenRecent}
        canSave={true}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /file/i }));

    // Click Open Recent to toggle submenu
    const openRecentTrigger = screen.getByText('Open Recent');
    fireEvent.click(openRecentTrigger);

    // Recent circuits header
    expect(screen.getByText(/Recent Circuits \(Last 8\)/i)).toBeDefined();

    // Should display exactly the 8 most recent files
    expect(screen.getByText('circuit_10.ipes')).toBeDefined();
    expect(screen.getByText('circuit_3.ipes')).toBeDefined();
    expect(screen.queryByText('circuit_2.ipes')).toBeNull();
    expect(screen.queryByText('circuit_1.ipes')).toBeNull();

    // Click circuit_10
    fireEvent.click(screen.getByText('circuit_10.ipes'));
    expect(onOpenRecent).toHaveBeenCalledTimes(1);
    expect(onOpenRecent).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'circuit_10.ipes', content: 'content_10' }),
    );
  });

  it('allows clearing recent files', () => {
    addRecentFile('test_file.ipes', 'data');
    render(
      <FileMenu
        onNew={vi.fn()}
        onOpen={vi.fn()}
        onSave={vi.fn()}
        onSaveAs={vi.fn()}
        onOpenRecent={vi.fn()}
        canSave={true}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /file/i }));
    fireEvent.click(screen.getByText('Open Recent'));

    expect(screen.getByText('test_file.ipes')).toBeDefined();
    fireEvent.click(screen.getByText('Clear Recent Files'));

    expect(screen.getByText('No recent files')).toBeDefined();
  });

  it('closes on Escape key', () => {
    render(
      <FileMenu
        onNew={vi.fn()}
        onOpen={vi.fn()}
        onSave={vi.fn()}
        onSaveAs={vi.fn()}
        onOpenRecent={vi.fn()}
        canSave={true}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: /file/i }));
    expect(screen.getByRole('menu')).toBeDefined();

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByRole('menu')).toBeNull();
  });
});
