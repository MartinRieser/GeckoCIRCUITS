// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { ParametricSweepModal } from '../src/modals/ParametricSweepModal';
import type { EditorComponent } from '../src/model/types';
import { LkComponentType } from '../src/model/constants';

afterEach(() => cleanup());

describe('ParametricSweepModal', () => {
  const components: EditorComponent[] = [
    {
      type: LkComponentType.RESISTOR,
      name: 'R.1',
      family: 'LK',
      position: [10, 10],
      orientation: 502,
      parameters: { param0: 100 },
      inputLabels: [],
      outputLabels: [],
    },
  ];

  it('previews linear steps, rejects non-positive log bounds, and submits sweep request', () => {
    const onStartSweep = vi.fn();

    render(
      <ParametricSweepModal
        isOpen
        onClose={vi.fn()}
        circuitId="c1"
        components={components}
        onStartSweep={onStartSweep}
      />,
    );

    expect(screen.getByTestId('sweep-preview-chips')).toBeDefined();

    // Switch to log mode and enter 0 start -> shows validation error & disables run
    fireEvent.click(screen.getByTestId('sweep-scale-log'));
    fireEvent.change(screen.getByTestId('sweep-start-input'), { target: { value: '0' } });
    expect(screen.getByTestId('sweep-validation-error').textContent).toContain('positive (> 0)');
    expect((screen.getByTestId('sweep-run-btn') as HTMLButtonElement).disabled).toBe(true);

    // Switch back to linear, enter 10 .. 50 in 5 steps
    fireEvent.click(screen.getByTestId('sweep-scale-linear'));
    fireEvent.change(screen.getByTestId('sweep-start-input'), { target: { value: '10' } });
    fireEvent.change(screen.getByTestId('sweep-end-input'), { target: { value: '50' } });
    fireEvent.change(screen.getByTestId('sweep-steps-input'), { target: { value: '5' } });

    fireEvent.click(screen.getByTestId('sweep-run-btn'));
    expect(onStartSweep).toHaveBeenCalledWith(
      expect.objectContaining({
        componentName: 'R.1',
        paramKey: 'param0',
        mode: 'linear',
        startValue: 10,
        endValue: 50,
        points: 5,
      }),
    );
  });
});
