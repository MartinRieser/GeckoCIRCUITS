// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { ContextMenu } from '../src/canvas/ContextMenu';

afterEach(() => cleanup());

describe('ContextMenu component', () => {
  it('renders actions for a component target and dispatches callbacks', () => {
    const onRotate = vi.fn();
    const onOpenProperties = vi.fn();
    const onDeleteComponent = vi.fn();
    const onClose = vi.fn();

    const { getByText } = render(
      <ContextMenu
        x={100}
        y={100}
        target={{ type: 'component', name: 'R.1', gridX: 10, gridY: 10 }}
        onClose={onClose}
        onRotate={onRotate}
        onOpenProperties={onOpenProperties}
        onDeleteComponent={onDeleteComponent}
      />,
    );

    expect(getByText('R.1')).toBeTruthy();
    expect(getByText('Rotate 90°')).toBeTruthy();
    expect(getByText('Properties')).toBeTruthy();
    expect(getByText('Delete')).toBeTruthy();

    fireEvent.click(getByText('Rotate 90°'));
    expect(onRotate).toHaveBeenCalledWith('R.1');
    expect(onClose).toHaveBeenCalled();

    fireEvent.click(getByText('Properties'));
    expect(onOpenProperties).toHaveBeenCalledWith('R.1');

    fireEvent.click(getByText('Delete'));
    expect(onDeleteComponent).toHaveBeenCalledWith('R.1');
  });

  it('renders actions for a wire target and dispatches callbacks', () => {
    const onLabelWire = vi.fn();
    const onDeleteWire = vi.fn();
    const onClose = vi.fn();

    const { getByText } = render(
      <ContextMenu
        x={100}
        y={100}
        target={{ type: 'wire', wireIndex: 3, gridX: 15, gridY: 15 }}
        onClose={onClose}
        onLabelWire={onLabelWire}
        onDeleteWire={onDeleteWire}
      />,
    );

    expect(getByText('Wire #3')).toBeTruthy();
    expect(getByText('Net Label...')).toBeTruthy();
    expect(getByText('Delete Wire')).toBeTruthy();

    fireEvent.click(getByText('Net Label...'));
    expect(onLabelWire).toHaveBeenCalledWith(3);
    expect(onClose).toHaveBeenCalled();

    fireEvent.click(getByText('Delete Wire'));
    expect(onDeleteWire).toHaveBeenCalledWith(3);
  });

  it('renders actions for a canvas target and dispatches callbacks', () => {
    const onToggleWireMode = vi.fn();
    const onOpenCommandPalette = vi.fn();
    const onZoomFit = vi.fn();
    const onClose = vi.fn();

    const { getByText } = render(
      <ContextMenu
        x={100}
        y={100}
        target={{ type: 'canvas', gridX: 20, gridY: 20 }}
        onClose={onClose}
        onToggleWireMode={onToggleWireMode}
        onOpenCommandPalette={onOpenCommandPalette}
        onZoomFit={onZoomFit}
      />,
    );

    expect(getByText('Wire Tool')).toBeTruthy();
    expect(getByText('Add Component...')).toBeTruthy();
    expect(getByText('Zoom to Fit')).toBeTruthy();

    fireEvent.click(getByText('Wire Tool'));
    expect(onToggleWireMode).toHaveBeenCalled();

    fireEvent.click(getByText('Add Component...'));
    expect(onOpenCommandPalette).toHaveBeenCalled();

    fireEvent.click(getByText('Zoom to Fit'));
    expect(onZoomFit).toHaveBeenCalled();
  });

  it('closes on Escape key press', () => {
    const onClose = vi.fn();
    render(
      <ContextMenu
        x={100}
        y={100}
        target={{ type: 'canvas', gridX: 0, gridY: 0 }}
        onClose={onClose}
      />,
    );

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });

  it('closes on click outside', () => {
    const onClose = vi.fn();
    render(
      <div>
        <div data-testid="outside">Outside</div>
        <ContextMenu
          x={100}
          y={100}
          target={{ type: 'canvas', gridX: 0, gridY: 0 }}
          onClose={onClose}
        />
      </div>,
    );

    fireEvent.mouseDown(document.body);
    expect(onClose).toHaveBeenCalled();
  });

  it('supports CCW rotation, duplication, and coupling flyout on component menus', () => {
    const onRotate = vi.fn();
    const onDuplicate = vi.fn();
    const onAssignCoupling = vi.fn();
    const onClose = vi.fn();

    render(
      <ContextMenu
        x={120}
        y={120}
        target={{ type: 'component', name: 'GATE.1', gridX: 5, gridY: 5 }}
        component={{
          name: 'GATE.1',
          type: 1000,
          family: 'CONTROL',
          position: [5, 5],
          orientation: 503,
          parameters: {},
          inputLabels: [],
          outputLabels: [],
        }}
        components={[
          {
            name: 'GATE.1',
            type: 1000,
            family: 'CONTROL',
            position: [5, 5],
            orientation: 503,
            parameters: {},
            inputLabels: [],
            outputLabels: [],
          },
          {
            name: 'S.1',
            type: 28,
            family: 'LK',
            position: [15, 5],
            orientation: 503,
            parameters: {},
            inputLabels: [],
            outputLabels: [],
          },
        ]}
        onClose={onClose}
        onRotate={onRotate}
        onDuplicate={onDuplicate}
        onAssignCoupling={onAssignCoupling}
      />,
    );

    fireEvent.click(screen.getByText('↺ Rotate 90° CCW'));
    expect(onRotate).toHaveBeenCalledWith('GATE.1', true);

    fireEvent.click(screen.getByText('⧉ Duplicate'));
    expect(onDuplicate).toHaveBeenCalledWith('GATE.1');

    fireEvent.click(screen.getByText('🔗 Couple to Switch'));
    fireEvent.click(screen.getByText('S.1'));
    expect(onAssignCoupling).toHaveBeenCalledWith('GATE.1', 'S.1');
  });

  it('offers placing a new gate driver on a MOSFET when no GATE block exists', () => {
    const onQuickPlaceAt = vi.fn();
    const mosfet = {
      name: 'M.1',
      type: 28,
      family: 'LK',
      position: [20, 8] as [number, number],
      orientation: 503,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
    };
    render(
      <ContextMenu
        x={50}
        y={50}
        target={{ type: 'component', name: 'M.1', gridX: 20, gridY: 8 }}
        component={mosfet as never}
        components={[mosfet as never]}
        onClose={vi.fn()}
        onQuickPlaceAt={onQuickPlaceAt}
      />,
    );
    fireEvent.click(screen.getByText('🔗 Coupled Gate Driver'));
    fireEvent.click(screen.getByText('+ Place New Gate Driver'));
    expect(onQuickPlaceAt).toHaveBeenCalledWith(expect.any(Number), 'CONTROL', 16, 8);
  });

  it('supports wire ground assignment and canvas quick-place + DRC actions', () => {
    const onSetWireGround = vi.fn();
    const onQuickPlaceAt = vi.fn();
    const onRunDrcCheck = vi.fn();

    const { rerender } = render(
      <ContextMenu
        x={100}
        y={100}
        target={{ type: 'wire', wireIndex: 2, gridX: 4, gridY: 4 }}
        onClose={vi.fn()}
        onSetWireGround={onSetWireGround}
      />,
    );

    fireEvent.click(screen.getByText('⏚ Set Net as Ground (0)'));
    expect(onSetWireGround).toHaveBeenCalledWith(2);

    rerender(
      <ContextMenu
        x={100}
        y={100}
        target={{ type: 'canvas', gridX: 12, gridY: 16 }}
        onClose={vi.fn()}
        onQuickPlaceAt={onQuickPlaceAt}
        onRunDrcCheck={onRunDrcCheck}
      />,
    );

    fireEvent.click(screen.getByText('⚡ Quick Place'));
    fireEvent.click(screen.getByText(/Resistor/i));
    expect(onQuickPlaceAt).toHaveBeenCalledWith(1, 'LK', 12, 16);

    fireEvent.click(screen.getByText('🔍 Run Circuit Check (DRC)'));
    expect(onRunDrcCheck).toHaveBeenCalledTimes(1);
  });
});
