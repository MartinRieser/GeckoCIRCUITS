// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { LiveSliderBar } from '../src/simulation/LiveSliderBar';
import type { PinnedSlider } from '../src/simulation/liveSliders';
import type { EditorComponent } from '../src/model/types';
import { LkComponentType } from '../src/model/constants';

afterEach(() => cleanup());

describe('LiveSliderBar', () => {
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

  const sliders: PinnedSlider[] = [
    {
      id: 'R.1.param0',
      componentName: 'R.1',
      paramKey: 'param0',
      label: 'Resistance',
      unit: 'Ω',
      initialValue: 100,
      min: 10,
      max: 300,
      scale: 'linear',
    },
  ];

  it('previews range changes smoothly without committing until pointerUp, then commits with Auto-Run state', () => {
    const onCommit = vi.fn();
    const onUnpin = vi.fn();

    render(
      <LiveSliderBar
        pinnedSliders={sliders}
        components={components}
        isOpen
        onPinSlider={vi.fn()}
        onUnpinSlider={onUnpin}
        onUpdateSliderConfig={vi.fn()}
        onCommitValue={onCommit}
      />,
    );

    const range = screen.getByTestId('slider-range-R.1.param0') as HTMLInputElement;
    // Dragging (onChange only) must not trigger simulation commit
    fireEvent.change(range, { target: { value: '500' } });
    expect(onCommit).not.toHaveBeenCalled();

    // Release triggers commit with autoRun=true (default)
    fireEvent.pointerUp(range);
    expect(onCommit).toHaveBeenCalledTimes(1);
    expect(onCommit).toHaveBeenCalledWith('R.1', 'param0', 155, true);

    // Disable Auto-Run and click Reset (↺)
    fireEvent.click(screen.getByTestId('live-sliders-autorun'));
    fireEvent.click(screen.getByTestId('slider-reset-R.1.param0'));
    expect(onCommit).toHaveBeenLastCalledWith('R.1', 'param0', 100, false);

    // Unpin removes slider
    fireEvent.click(screen.getByTestId('slider-unpin-R.1.param0'));
    expect(onUnpin).toHaveBeenCalledWith('R.1.param0');
  });
});
