import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SchematicNoteCard } from '../src/canvas/SchematicNoteCard';

describe('SchematicNoteCard', () => {
  it('renders the note card with title bar, badge, and markdown headings and paragraphs', () => {
    const { container } = render(
      <SchematicNoteCard
        name="NOTE.1"
        text={'# Circuit Intro\n\nExplanation of buck converter.\n\n- Inductor 100uH\n- Capacitor 47uF'}
        width={300}
        height={200}
        selected={false}
      />,
    );

    expect(screen.getByText('NOTE.1')).toBeDefined();
    expect(screen.getByText('Circuit Intro')).toBeDefined();
    expect(screen.getByText('Explanation of buck converter.')).toBeDefined();
    expect(screen.getByText('Inductor 100uH')).toBeDefined();
    expect(container.querySelector('.schematic-note-card')).not.toBeNull();
    expect(container.querySelector('.note-card-badge')?.textContent).toBe('DOCS');
  });

  it('renders table elements when markdown table syntax is provided', () => {
    const { container } = render(
      <SchematicNoteCard
        name="NOTE.TABLE"
        text={'| Param | Value |\n|---|---|\n| Voltage | 24V |'}
        width={300}
        height={200}
        selected={false}
      />,
    );

    expect(container.querySelector('table')).not.toBeNull();
    expect(screen.getByText('Param')).toBeDefined();
    expect(screen.getByText('24V')).toBeDefined();
  });

  it('applies is-selected CSS class when selected is true', () => {
    const { container } = render(
      <SchematicNoteCard
        name="NOTE.1"
        text="Content"
        width={300}
        height={200}
        selected={true}
      />,
    );
    expect(container.querySelector('.schematic-note-card.is-selected')).not.toBeNull();
  });

  it('renders mathematical expressions $(v_{ref} - v_{meas})$ and preserves variables and subscripts', () => {
    const text = '2. **Error & Regulation**: `CTRL_SUB` computes error $(v_{ref} - v_{meas})$, fed to `CTRL_PI` ($K_p=0.45, T_i=2\\text{ ms}$) and `CTRL_LIM` clamped to $[-1, 1]$.';
    const { container } = render(
      <SchematicNoteCard
        name="NOTE.1"
        text={text}
        width={400}
        height={300}
        selected={false}
      />,
    );

    // Code blocks preserved
    expect(screen.getByText('CTRL_SUB')).toBeDefined();
    expect(screen.getByText('CTRL_PI')).toBeDefined();
    expect(screen.getByText('CTRL_LIM')).toBeDefined();

    // Bold label preserved
    expect(screen.getByText('Error & Regulation')).toBeDefined();

    // Math elements rendered without raw dollar signs
    const mathElements = container.querySelectorAll('.note-math');
    expect(mathElements.length).toBeGreaterThanOrEqual(3);

    // Check subscripts ref, meas, p, i
    expect(container.textContent).toContain('ref');
    expect(container.textContent).toContain('meas');
    expect(container.textContent).not.toContain('$(v_{ref}');
    expect(container.textContent).not.toContain('\\text{');

    const subs = container.querySelectorAll('sub');
    const subTexts = Array.from(subs).map((s) => s.textContent);
    expect(subTexts).toContain('ref');
    expect(subTexts).toContain('meas');
    expect(subTexts).toContain('p');
    expect(subTexts).toContain('i');
  });
});
