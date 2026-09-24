// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { Palette } from '../src/palette/Palette';
import type { CatalogEntry } from '../src/model/types';
import { LkComponentType, ControlComponentType } from '../src/model/constants';

describe('Palette Component', () => {
  afterEach(() => {
    cleanup();
  });

  const sampleCatalog: CatalogEntry[] = [
    { type: LkComponentType.RESISTOR, name: 'Resistor', family: 'LK' },
    { type: LkComponentType.CAPACITOR, name: 'Capacitor', family: 'LK' },
    { type: LkComponentType.INDUCTOR, name: 'Inductor', family: 'LK' },
    { type: ControlComponentType.GATE, name: 'Gate', family: 'CONTROL' },
    { type: ControlComponentType.LEGACY_JAVA_FUNCTION, name: 'C_JAVA_FUNCTION', family: 'CONTROL' },
  ];

  it('renders the catalog list and filters out legacy Java function', () => {
    const onArm = vi.fn();
    render(<Palette catalog={sampleCatalog} onArm={onArm} />);

    expect(screen.getByText('Components')).not.toBeNull();
    expect(screen.getByText('Resistor')).not.toBeNull();
    expect(screen.getByText('Capacitor')).not.toBeNull();
    expect(screen.queryByText('C_JAVA_FUNCTION')).toBeNull();
  });

  it('filters entries when typing into the search input', () => {
    render(<Palette catalog={sampleCatalog} onArm={vi.fn()} />);

    const searchInput = screen.getByPlaceholderText(/Search parts/i);
    fireEvent.change(searchInput, { target: { value: 'cap' } });

    expect(screen.getByText('Capacitor')).not.toBeNull();
    expect(screen.queryByText('Resistor')).toBeNull();

    // Clear search button appears and works
    const clearBtn = screen.getByTitle('Clear search');
    fireEvent.click(clearBtn);
    expect(screen.getByText('Resistor')).not.toBeNull();
  });

  it('filters by category selection and resets', () => {
    render(<Palette catalog={sampleCatalog} onArm={vi.fn()} />);

    const select = screen.getByLabelText('Filter components by category');
    fireEvent.change(select, { target: { value: 'control' } });

    expect(screen.queryByText('Resistor')).toBeNull();

    // Reset button appears
    const resetBtn = screen.getByRole('button', { name: 'All' });
    fireEvent.click(resetBtn);
    expect(screen.getByText('Resistor')).not.toBeNull();
  });

  it('invokes onArm when a component card is clicked', () => {
    const onArm = vi.fn();
    render(<Palette catalog={sampleCatalog} onArm={onArm} />);

    const resistorCard = screen.getByText('Resistor').closest('button');
    expect(resistorCard).not.toBeNull();
    fireEvent.click(resistorCard!);

    expect(onArm).toHaveBeenCalledWith(expect.objectContaining({ name: 'Resistor', type: LkComponentType.RESISTOR }));
  });

  it('handles drag start correctly', () => {
    const onArm = vi.fn();
    render(<Palette catalog={sampleCatalog} onArm={onArm} />);

    const resistorCard = screen.getByText('Resistor').closest('button');
    const setDataMock = vi.fn();

    fireEvent.dragStart(resistorCard!, {
      dataTransfer: {
        setData: setDataMock,
      },
    });

    expect(setDataMock).toHaveBeenCalledWith('text/plain', expect.stringContaining('"name":"Resistor"'));
    expect(onArm).toHaveBeenCalled();
  });

  it('calls onCollapse when collapse button is clicked', () => {
    const onCollapse = vi.fn();
    render(<Palette catalog={sampleCatalog} onArm={vi.fn()} onCollapse={onCollapse} />);

    const collapseBtn = screen.getByTitle(/Collapse palette panel/i);
    fireEvent.click(collapseBtn);
    expect(onCollapse).toHaveBeenCalledTimes(1);
  });

  it('displays empty state when search finds no matches, with reset button', () => {
    render(<Palette catalog={sampleCatalog} onArm={vi.fn()} />);

    const searchInput = screen.getByPlaceholderText(/Search parts/i);
    fireEvent.change(searchInput, { target: { value: 'nonexistentxyz' } });

    expect(screen.getByText('No components found')).not.toBeNull();
    const resetBtn = screen.getByRole('button', { name: 'Reset filters' });
    fireEvent.click(resetBtn);

    expect(screen.getByText('Resistor')).not.toBeNull();
  });
});
