// @vitest-environment jsdom
import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/react';
import { ContextMenu } from '../src/canvas/ContextMenu';

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
});
