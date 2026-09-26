import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, within } from '@testing-library/react';
import { ScriptCodeEditor } from '../src/properties/ScriptCodeEditor';

describe('ScriptCodeEditor', () => {
  const CODE = 'yOUT[0] = xIN[0];\nacc = acc + u1;\nyOUT[1] = acc;';

  it('renders one gutter entry per line and toggles breakpoints', () => {
    const onToggle = vi.fn();
    const { container } = render(
      <ScriptCodeEditor
        value={CODE}
        onChange={vi.fn()}
        breakpoints={[]}
        onToggleBreakpoint={onToggle}
      />,
    );

    const gutter = container.querySelector('.script-editor-gutter') as HTMLElement;
    expect(gutter).not.toBeNull();
    expect(within(gutter).getByText('1')).not.toBeNull();
    expect(within(gutter).getByText('3')).not.toBeNull();

    fireEvent.click(within(gutter).getByText('2'));
    expect(onToggle).toHaveBeenCalledWith(2);
  });

  it('marks breakpoint lines and forwards code edits', () => {
    const onChange = vi.fn();
    const { container } = render(
      <ScriptCodeEditor
        value={CODE}
        onChange={onChange}
        breakpoints={[3]}
        onToggleBreakpoint={vi.fn()}
      />,
    );

    const marked = container.querySelectorAll('.script-gutter-line.has-breakpoint');
    expect(marked.length).toBe(1);
    expect(marked[0].textContent).toContain('3');

    const textarea = container.querySelector('.script-editor-input') as HTMLTextAreaElement;
    fireEvent.change(textarea, { target: { value: 'y = 1;' } });
    expect(onChange).toHaveBeenCalledWith('y = 1;');
  });

  it('highlights the paused line and scrolls it into view', () => {
    const { container } = render(
      <ScriptCodeEditor
        value={CODE}
        onChange={vi.fn()}
        breakpoints={[]}
        onToggleBreakpoint={vi.fn()}
        pausedLine={3}
      />,
    );

    const marker = container.querySelector('.script-editor-paused-line') as HTMLElement;
    expect(marker).not.toBeNull();
    // Third line sits at (3-1) * 18px
    expect(marker.style.top).toBe('36px');

    const pausedGutter = container.querySelector('.script-gutter-line.paused');
    expect(pausedGutter?.textContent).toContain('3');
  });
});
