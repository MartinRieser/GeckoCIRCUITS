// @vitest-environment jsdom
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import {
  orientationAngle,
  controlOrientationAngle,
  ComponentSymbol,
  SymbolPreview,
  SymbolByType,
} from '../src/canvas/symbols';
import {
  Orientation,
  LkComponentType,
  ControlComponentType,
} from '../src/model/constants';
import type { EditorComponent } from '../src/model/types';

describe('symbols: Orientation Angles', () => {
  it('maps electrical (LK) orientations to degrees correctly', () => {
    expect(orientationAngle(Orientation.WEST_EAST)).toBe(0);
    expect(orientationAngle(Orientation.NORTH_SOUTH)).toBe(90);
    expect(orientationAngle(Orientation.EAST_WEST)).toBe(180);
    expect(orientationAngle(Orientation.SOUTH_NORTH)).toBe(270);
  });

  it('maps control orientations (quarter turn offset) to degrees correctly', () => {
    expect(controlOrientationAngle(Orientation.NORTH_SOUTH)).toBe(0);
    expect(controlOrientationAngle(Orientation.EAST_WEST)).toBe(90);
    expect(controlOrientationAngle(Orientation.SOUTH_NORTH)).toBe(180);
    expect(controlOrientationAngle(Orientation.WEST_EAST)).toBe(270);
  });
});

describe('symbols: ComponentSymbol SVG rendering', () => {
  function makeComp(partial: Partial<EditorComponent> & { type: number; name: string }): EditorComponent {
    return {
      family: 'LK',
      position: [10, 10],
      orientation: Orientation.WEST_EAST,
      parameters: {},
      inputLabels: [],
      outputLabels: [],
      ...partial,
    };
  }

  it('renders standard two-port passive components (Resistor, Inductor, Capacitor)', () => {
    for (const type of [
      LkComponentType.RESISTOR,
      LkComponentType.INDUCTOR,
      LkComponentType.CAPACITOR,
      LkComponentType.DIODE,
      LkComponentType.IDEAL_SWITCH,
    ]) {
      const { container } = render(
        <svg>
          <ComponentSymbol component={makeComp({ type, name: `comp_${type}` })} dpix={10} />
        </svg>,
      );
      expect(container.querySelector('g')).toBeTruthy();
      expect(container.querySelectorAll('path, rect, line, circle').length).toBeGreaterThan(0);
    }
  });

  it('renders complex power components (Transformer, BJT, Voltage Source)', () => {
    for (const type of [
      LkComponentType.TRANSFORMER,
      LkComponentType.BJT,
      LkComponentType.VOLTAGE_SOURCE,
      LkComponentType.CURRENT_SOURCE,
    ]) {
      const { container } = render(
        <svg>
          <ComponentSymbol component={makeComp({ type, name: `comp_${type}` })} dpix={10} />
        </svg>,
      );
      expect(container.querySelector('g')).toBeTruthy();
    }
  });

  it('renders control elements (Voltmeter, Scope, Script block)', () => {
    const scopeComp = makeComp({
      type: ControlComponentType.SCOPE,
      name: 'SCOPE.1',
      family: 'CONTROL',
      inputLabels: ['SIG1', 'SIG2'],
    });

    const { container: scopeContainer } = render(
      <svg>
        <ComponentSymbol component={scopeComp} dpix={10} />
      </svg>,
    );
    expect(scopeContainer.querySelector('g')).toBeTruthy();

    const scriptComp = makeComp({
      type: ControlComponentType.SCRIPT,
      name: 'FUNC.1',
      family: 'CONTROL',
      parameters: { anzXIN: 2, anzYOUT: 2 },
    });

    const { container: scriptContainer } = render(
      <svg>
        <ComponentSymbol component={scriptComp} dpix={10} />
      </svg>,
    );
    const texts = Array.from(scriptContainer.querySelectorAll('text')).map((t) => t.textContent);
    expect(texts).toContain('f(x)');
  });
});

describe('symbols: SymbolPreview and SymbolByType fallback', () => {
  it('renders a standalone SVG preview icon', () => {
    const { container } = render(<SymbolPreview type={LkComponentType.RESISTOR} size={48} />);
    const svg = container.querySelector('svg.symbol-preview-svg');
    expect(svg).toBeTruthy();
    expect(svg?.getAttribute('width')).toBe('48');
    expect(svg?.getAttribute('height')).toBe('48');
  });

  it('renders fallback generic box for unknown component types', () => {
    const { container } = render(
      <svg>
        <SymbolByType type={9999} u={10} family="CUSTOM" />
      </svg>,
    );
    expect(container.querySelector('rect')).toBeTruthy();
    expect(container.textContent).toContain('CUSTOM');
  });
});
