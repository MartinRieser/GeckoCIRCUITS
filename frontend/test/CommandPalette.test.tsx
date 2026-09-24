// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { CommandPalette, getMatchScore } from '../src/palette/CommandPalette';
import type { CatalogEntry } from '../src/model/types';
import { LkComponentType, ControlComponentType } from '../src/model/constants';

describe('CommandPalette & getMatchScore', () => {
  afterEach(() => {
    cleanup();
  });

  const sampleCatalog: CatalogEntry[] = [
    { type: LkComponentType.RESISTOR, name: 'Resistor', family: 'LK' },
    { type: LkComponentType.CAPACITOR, name: 'Capacitor', family: 'LK' },
    { type: ControlComponentType.GATE, name: 'Gate', family: 'CONTROL' },
  ];

  describe('getMatchScore', () => {
    it('returns 1 for empty query', () => {
      expect(getMatchScore('Resistor', 'R', 'Standard resistor', '')).toBe(1);
    });

    it('returns 100 for exact match on display name or identifier', () => {
      expect(getMatchScore('Resistor', 'R', 'Standard resistor', 'resistor')).toBe(100);
      expect(getMatchScore('Resistor', 'R', 'Standard resistor', 'r')).toBe(100);
    });

    it('returns 80 if display name starts with query', () => {
      expect(getMatchScore('Resistor', 'R', 'Standard resistor', 'res')).toBe(80);
    });

    it('returns 60 if display name contains query', () => {
      expect(getMatchScore('Resistor', 'R', 'Standard resistor', 'sist')).toBe(60);
    });

    it('returns 40 if engine name contains query', () => {
      expect(getMatchScore('Gate Driver', 'C_GATE', 'Gate driver', 'gate')).toBe(80); // prefix match on displayName
      expect(getMatchScore('Driver', 'C_GATE', 'Driver', 'gate')).toBe(40); // match in engine name
    });

    it('returns 20 if description contains query', () => {
      expect(getMatchScore('Ohm', 'R_OHM', 'Precision passive', 'passive')).toBe(20);
    });

    it('returns 0 for no match', () => {
      expect(getMatchScore('Resistor', 'R', 'Standard resistor', 'xyz123')).toBe(0);
    });
  });

  describe('CommandPalette Component', () => {
    it('returns null when isOpen is false', () => {
      const { container } = render(
        <CommandPalette
          isOpen={false}
          onClose={vi.fn()}
          catalog={sampleCatalog}
          onSelect={vi.fn()}
        />,
      );
      expect(container.firstChild).toBeNull();
    });

    it('renders the search modal when isOpen is true', () => {
      render(
        <CommandPalette
          isOpen={true}
          onClose={vi.fn()}
          catalog={sampleCatalog}
          onSelect={vi.fn()}
        />,
      );

      expect(screen.getByPlaceholderText(/Search component to place/i)).not.toBeNull();
      expect(screen.getByText('Resistor')).not.toBeNull();
      expect(screen.getByText('Capacitor')).not.toBeNull();
    });

    it('filters components based on query', () => {
      render(
        <CommandPalette
          isOpen={true}
          onClose={vi.fn()}
          catalog={sampleCatalog}
          onSelect={vi.fn()}
        />,
      );

      const input = screen.getByPlaceholderText(/Search component to place/i);
      fireEvent.change(input, { target: { value: 'cap' } });

      expect(screen.getByText('Capacitor')).not.toBeNull();
      expect(screen.queryByText('Resistor')).toBeNull();
    });

    it('shows empty state when no items match query', () => {
      render(
        <CommandPalette
          isOpen={true}
          onClose={vi.fn()}
          catalog={sampleCatalog}
          onSelect={vi.fn()}
        />,
      );

      const input = screen.getByPlaceholderText(/Search component to place/i);
      fireEvent.change(input, { target: { value: 'nonexistent-query-xyz' } });

      expect(screen.getByText('No matching components found')).not.toBeNull();
    });

    it('handles mouse click on an item', () => {
      const onSelect = vi.fn();
      const onClose = vi.fn();

      render(
        <CommandPalette
          isOpen={true}
          onClose={onClose}
          catalog={sampleCatalog}
          onSelect={onSelect}
        />,
      );

      const item = screen.getByText('Resistor').closest('.command-item');
      fireEvent.click(item!);

      expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ name: 'Resistor' }));
      expect(onClose).toHaveBeenCalled();
    });

    it('navigates with ArrowDown, ArrowUp, and selects with Enter', () => {
      const onSelect = vi.fn();
      const onClose = vi.fn();

      const { container } = render(
        <CommandPalette
          isOpen={true}
          onClose={onClose}
          catalog={sampleCatalog}
          onSelect={onSelect}
        />,
      );

      const modal = container.querySelector('.command-palette-modal')!;

      // Arrow down to 2nd item
      fireEvent.keyDown(modal, { key: 'ArrowDown' });
      // Arrow up back to 1st item
      fireEvent.keyDown(modal, { key: 'ArrowUp' });
      // Enter to select
      fireEvent.keyDown(modal, { key: 'Enter' });

      expect(onSelect).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });

    it('closes on Escape key and on backdrop click', () => {
      const onClose = vi.fn();

      const { container } = render(
        <CommandPalette
          isOpen={true}
          onClose={onClose}
          catalog={sampleCatalog}
          onSelect={vi.fn()}
        />,
      );

      const modal = container.querySelector('.command-palette-modal')!;
      fireEvent.keyDown(modal, { key: 'Escape' });
      expect(onClose).toHaveBeenCalledTimes(1);

      const backdrop = container.querySelector('.command-palette-backdrop')!;
      fireEvent.click(backdrop);
      expect(onClose).toHaveBeenCalledTimes(2);
    });
  });
});
