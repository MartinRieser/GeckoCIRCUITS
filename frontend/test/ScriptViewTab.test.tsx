// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { ScriptViewTab } from '../src/properties/ScriptViewTab';
import type { EditorComponent } from '../src/model/types';
import { ControlComponentType } from '../src/model/constants';

describe('ScriptViewTab Component', () => {
  afterEach(() => {
    cleanup();
  });

  const dummyScriptComponent: EditorComponent = {
    type: ControlComponentType.SCRIPT,
    family: 'CONTROL',
    name: 'SCRIPT.1',
    position: [10, 10],
    orientation: 502,
    parameters: {
      sourceCode: 'yOUT[0] = xIN[0] * 3;',
      anzXIN: '1',
      anzYOUT: '1',
    },
    inputLabels: ['in_signal'],
    outputLabels: ['out_signal'],
  };

  it('renders empty state when component is null', () => {
    render(
      <ScriptViewTab
        component={null}
        onSetParameter={vi.fn()}
      />,
    );
    expect(screen.getByText('No Script Component Selected')).not.toBeNull();
  });

  it('renders script title, code textarea, and terminal pins', () => {
    const { container } = render(
      <ScriptViewTab
        component={dummyScriptComponent}
        onSetParameter={vi.fn()}
      />,
    );

    expect(screen.getByText('SCRIPT.1')).not.toBeNull();
    expect(screen.getByText('Function Block (Script)')).not.toBeNull();

    const textarea = container.querySelector('textarea');
    expect(textarea?.value).toBe('yOUT[0] = xIN[0] * 3;');

    expect(screen.getByLabelText(/Input Terminals/i)).not.toBeNull();
    expect(screen.getByLabelText(/Output Terminals/i)).not.toBeNull();
  });

  it('toggles syntax cheat sheet', () => {
    render(
      <ScriptViewTab
        component={dummyScriptComponent}
        onSetParameter={vi.fn()}
      />,
    );

    const cheatBtn = screen.getByRole('button', { name: /Cheat Sheet/i });
    fireEvent.click(cheatBtn);

    expect(screen.getByText(/Syntax Reference:/i)).not.toBeNull();
    expect(screen.getByRole('button', { name: /Hide Cheat Sheet/i })).not.toBeNull();
  });

  it('updates parameters and calls onSetParameter on Apply', () => {
    const onSetParameter = vi.fn();
    const { container } = render(
      <ScriptViewTab
        component={dummyScriptComponent}
        onSetParameter={onSetParameter}
      />,
    );

    const textarea = container.querySelector('textarea')!;
    fireEvent.change(textarea, { target: { value: 'yOUT[0] = xIN[0] + 10;' } });

    const applyBtn = screen.getByRole('button', { name: /Apply Script/i });
    fireEvent.click(applyBtn);

    expect(onSetParameter).toHaveBeenCalledWith('SCRIPT.1', 'sourceCode', 'yOUT[0] = xIN[0] + 10;');
    expect(onSetParameter).toHaveBeenCalledWith('SCRIPT.1', 'anzXIN', 1);
    expect(onSetParameter).toHaveBeenCalledWith('SCRIPT.1', 'anzYOUT', 1);
    expect(screen.getByText(/✓ Applied/i)).not.toBeNull();
  });

  it('changes input and output terminal counts', () => {
    const onSetParameter = vi.fn();
    render(
      <ScriptViewTab
        component={dummyScriptComponent}
        onSetParameter={onSetParameter}
      />,
    );

    const inInput = screen.getByLabelText(/Input Terminals/i);
    fireEvent.change(inInput, { target: { value: '3' } });
    expect(onSetParameter).toHaveBeenCalledWith('SCRIPT.1', 'anzXIN', 3);

    const outInput = screen.getByLabelText(/Output Terminals/i);
    fireEvent.change(outInput, { target: { value: '2' } });
    expect(onSetParameter).toHaveBeenCalledWith('SCRIPT.1', 'anzYOUT', 2);
  });

  it('renders live variable watch during simulation', () => {
    render(
      <ScriptViewTab
        component={dummyScriptComponent}
        onSetParameter={vi.fn()}
        scriptDebug={{
          status: 'RUNNING',
          breakpoints: { 'SCRIPT.1': [2] },
          debugPause: null,
          debugState: {
            simulationId: 'sim-1',
            paused: false,
            blocks: [
              {
                blockName: 'SCRIPT.1',
                line: 1,
                time: 0.005,
                dt: 1e-6,
                inputs: [2.5],
                outputs: [7.5],
                variables: { counter: 42 },
              },
            ],
          },
          onToggleBreakpoint: vi.fn(),
          onDebugResume: vi.fn(),
          onDebugStep: vi.fn(),
        }}
      />,
    );

    expect(screen.getByText('counter')).not.toBeNull();
    expect(screen.getByText('42')).not.toBeNull();
    expect(screen.getByText('u1')).not.toBeNull();
    expect(screen.getByText('y1')).not.toBeNull();
  });
});
