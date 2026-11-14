import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { DiagnosticsBadge, DiagnosticsDrawer } from '../src/simulation/DiagnosticsDrawer';
import type { DiagnosticItem } from '../src/model/validation';

const SAMPLE_ITEMS: DiagnosticItem[] = [
  {
    id: 'drc-uncoupled-GATE.1',
    severity: 'warning',
    category: 'drc',
    title: 'Uncoupled Gate Driver (GATE.1)',
    detail: 'GATE.1 has no switch target.',
    componentName: 'GATE.1',
    coord: { x: 12, y: 18 },
    quickFix: {
      label: 'Select GATE.1',
      action: 'select-component',
      componentName: 'GATE.1',
    },
  },
  {
    id: 'engine-error',
    severity: 'error',
    category: 'engine',
    title: 'Simulation Failed',
    detail: 'Singular matrix at node n_sw.',
    quickFix: {
      label: 'Open Setup',
      action: 'open-sim-config',
    },
  },
];

describe('DiagnosticsBadge', () => {
  afterEach(() => cleanup());

  it('shows clean badge when no errors or warnings exist', () => {
    const onToggle = vi.fn();
    render(<DiagnosticsBadge diagnostics={[]} isOpen={false} onToggle={onToggle} />);
    const badge = screen.getByTestId('navbar-diagnostics-badge');
    expect(badge.textContent).toContain('DRC Clean');
    expect(badge.className).toContain('clean');
    fireEvent.click(badge);
    expect(onToggle).toHaveBeenCalledTimes(1);
  });

  it('summarizes combined error and warning counts', () => {
    render(<DiagnosticsBadge diagnostics={SAMPLE_ITEMS} isOpen={true} onToggle={vi.fn()} />);
    const badge = screen.getByTestId('navbar-diagnostics-badge');
    expect(badge.className).toContain('has-errors');
    expect(badge.textContent).toContain('1 Error');
    expect(badge.textContent).toContain('1');
  });
});

describe('DiagnosticsDrawer', () => {
  afterEach(() => cleanup());

  it('renders nothing when closed', () => {
    const { container } = render(
      <DiagnosticsDrawer
        diagnostics={SAMPLE_ITEMS}
        isOpen={false}
        onClose={vi.fn()}
        onLocate={vi.fn()}
        onQuickFix={vi.fn()}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('sorts errors before warnings, supports filtering, click-to-locate, and quick fixes', () => {
    const onLocate = vi.fn();
    const onQuickFix = vi.fn();
    const onClose = vi.fn();

    render(
      <DiagnosticsDrawer
        diagnostics={SAMPLE_ITEMS}
        isOpen={true}
        onClose={onClose}
        onLocate={onLocate}
        onQuickFix={onQuickFix}
      />,
    );

    const listItems = screen.getAllByRole('listitem');
    expect(listItems).toHaveLength(2);
    // Error item appears first
    expect(listItems[0].textContent).toContain('Simulation Failed');
    expect(listItems[1].textContent).toContain('Uncoupled Gate Driver (GATE.1)');

    // Click quick fix without triggering row locate
    fireEvent.click(screen.getByText('⚡ Select GATE.1'));
    expect(onQuickFix).toHaveBeenCalledWith(SAMPLE_ITEMS[0], SAMPLE_ITEMS[0].quickFix);
    expect(onLocate).not.toHaveBeenCalled();

    // Click row / Locate button
    fireEvent.click(screen.getByText('⌖ Locate'));
    expect(onLocate).toHaveBeenCalledWith(SAMPLE_ITEMS[0]);

    // Filter to Errors only
    fireEvent.click(screen.getByRole('button', { name: 'Errors (1)' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByText('Simulation Failed')).toBeTruthy();

    fireEvent.click(screen.getByLabelText('Close Diagnostics'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
