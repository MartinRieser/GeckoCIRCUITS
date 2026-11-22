// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { Palette } from '../src/palette/Palette';
import type { CatalogEntry } from '../src/model/types';
import { LkComponentType, ControlComponentType } from '../src/model/constants';
import { BUILT_IN_SNIPPETS, type SchematicSnippet } from '../src/model/snippets';

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

  it('groups Voltmeter and Ammeter under Sensors (control feedback) category with clear guidance', () => {
    const catalog: CatalogEntry[] = [
      { type: ControlComponentType.VOLTMETER, name: 'CTRL_VOLT', family: 'CONTROL' },
      { type: ControlComponentType.AMMETER, name: 'CTRL_AMP', family: 'CONTROL' },
    ];
    render(<Palette catalog={catalog} onArm={vi.fn()} />);

    const select = screen.getByLabelText('Filter components by category');
    expect(screen.getByText(/Sensors \(control feedback\)/i)).not.toBeNull();

    fireEvent.change(select, { target: { value: 'sensors' } });
    expect(screen.getByText('Voltmeter')).not.toBeNull();
    expect(screen.getByText('Ammeter')).not.toBeNull();

    // Descriptions should guide the user about Probe (P) and Oscilloscope
    expect(screen.getAllByText(/feeding into control blocks/i).length).toBeGreaterThan(0);
  });

  it('selects Templates/Snippets from category dropdown, filters snippets, arms on click, and protects built-in snippets', () => {
    const userSnippet: SchematicSnippet = {
      ...BUILT_IN_SNIPPETS[0],
      id: 'user_custom_1',
      name: 'Custom Clamp',
      category: 'Custom',
      description: 'User saved block',
      builtIn: false,
      createdAt: 1,
    };
    const snippets = [...BUILT_IN_SNIPPETS, userSnippet];
    const onArmSnippet = vi.fn();
    const onDeleteSnippet = vi.fn();

    render(
      <Palette
        catalog={sampleCatalog}
        onArm={vi.fn()}
        snippets={snippets}
        onArmSnippet={onArmSnippet}
        onDeleteSnippet={onDeleteSnippet}
      />,
    );

    const catSelect = screen.getByLabelText('Filter components by category');
    fireEvent.change(catSelect, { target: { value: 'snippets' } });
    expect(screen.getByText('LC Output Filter')).not.toBeNull();
    expect(screen.getByText('Custom Clamp')).not.toBeNull();

    // Built-in snippets cannot be deleted
    expect(screen.queryByTestId(`delete-snippet-${BUILT_IN_SNIPPETS[0].id}`)).toBeNull();

    // User snippet delete button calls onDeleteSnippet without arming
    fireEvent.click(screen.getByTestId('delete-snippet-user_custom_1'));
    expect(onDeleteSnippet).toHaveBeenCalledWith('user_custom_1');
    expect(onArmSnippet).not.toHaveBeenCalled();

    // Clicking snippet card arms it
    fireEvent.click(screen.getByTestId(`snippet-card-${BUILT_IN_SNIPPETS[0].id}`));
    expect(onArmSnippet).toHaveBeenCalledWith(BUILT_IN_SNIPPETS[0]);

    // Filtering inside snippets category
    const searchInput = screen.getByPlaceholderText(/Search snippets/i);
    fireEvent.change(searchInput, { target: { value: 'Buck' } });
    expect(screen.getByText('Buck Switching Cell')).not.toBeNull();
    expect(screen.queryByText('Custom Clamp')).toBeNull();
  });

  it('enables Save Selected button inside Snippets category when components are selected', () => {
    const onSaveSelectionAsSnippet = vi.fn();
    const { rerender } = render(
      <Palette
        catalog={sampleCatalog}
        onArm={vi.fn()}
        snippets={BUILT_IN_SNIPPETS}
        selectedCount={0}
        onSaveSelectionAsSnippet={onSaveSelectionAsSnippet}
      />,
    );

    const catSelect = screen.getByLabelText('Filter components by category');
    fireEvent.change(catSelect, { target: { value: 'snippets' } });
    const saveBtn = screen.getByTestId('palette-save-snippet-btn') as HTMLButtonElement;
    expect(saveBtn.disabled).toBe(true);

    rerender(
      <Palette
        catalog={sampleCatalog}
        onArm={vi.fn()}
        snippets={BUILT_IN_SNIPPETS}
        selectedCount={2}
        onSaveSelectionAsSnippet={onSaveSelectionAsSnippet}
      />,
    );

    expect((screen.getByTestId('palette-save-snippet-btn') as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(screen.getByTestId('palette-save-snippet-btn'));
    expect(onSaveSelectionAsSnippet).toHaveBeenCalledTimes(1);
  });
});
