// @vitest-environment jsdom
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import {
  orientationAngle,
  controlOrientationAngle,
  transformerOrientationAngle,
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

  it('maps transformer orientations (upright base NORTH_SOUTH) to degrees correctly', () => {
    expect(transformerOrientationAngle(Orientation.NORTH_SOUTH)).toBe(0);
    expect(transformerOrientationAngle(Orientation.EAST_WEST)).toBe(90);
    expect(transformerOrientationAngle(Orientation.SOUTH_NORTH)).toBe(180);
    expect(transformerOrientationAngle(Orientation.WEST_EAST)).toBe(270);
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

  it('renders Comparator with input lead lines aligned to integer grid rows (y=0 and y=1*u)', () => {
    const comp = makeComp({
      type: 1010,
      name: 'CMP.1',
      family: 'CONTROL',
      orientation: Orientation.NORTH_SOUTH,
    });

    const { container } = render(
      <svg>
        <ComponentSymbol component={comp} dpix={10} />
      </svg>,
    );

    const lines = Array.from(container.querySelectorAll('line'));
    expect(lines.length).toBe(3); // 2 inputs + 1 output
    const lineCoords = lines.map((l) => ({
      x1: Number(l.getAttribute('x1')),
      y1: Number(l.getAttribute('y1')),
      x2: Number(l.getAttribute('x2')),
      y2: Number(l.getAttribute('y2')),
    }));

    // Input 0 (+) lead: along y=0 from -20 to -6.5
    expect(lineCoords).toContainEqual({ x1: -20, y1: 0, x2: -6.5, y2: 0 });
    // Input 1 (-) lead: along y=10 from -20 to -6.5
    expect(lineCoords).toContainEqual({ x1: -20, y1: 10, x2: -6.5, y2: 10 });
    // Output lead: along y=0 from 6.5 to 20
    expect(lineCoords).toContainEqual({ x1: 6.5, y1: 0, x2: 20, y2: 0 });

    const texts = Array.from(container.querySelectorAll('text')).map((t) => t.textContent);
    expect(texts).toContain('+');
    expect(texts).toContain('−');
  });

  it('renders transformer with upright base orientation (503 -> 0deg), P/S labels, and 90deg steps', () => {
    const orientations = [
      { code: Orientation.NORTH_SOUTH, expectedAngle: 0 },
      { code: Orientation.EAST_WEST, expectedAngle: 90 },
      { code: Orientation.SOUTH_NORTH, expectedAngle: 180 },
      { code: Orientation.WEST_EAST, expectedAngle: 270 },
    ];

    for (const { code, expectedAngle } of orientations) {
      const comp = makeComp({
        type: LkComponentType.TRANSFORMER,
        name: 'TR.1',
        orientation: code,
      });
      const { container } = render(
        <svg>
          <ComponentSymbol component={comp} dpix={10} />
        </svg>,
      );
      const rootG = container.querySelector('g[transform]');
      expect(rootG?.getAttribute('transform')).toBe(`rotate(${expectedAngle})`);
    }

    const defaultComp = makeComp({
      type: LkComponentType.TRANSFORMER,
      name: 'TR.1',
      orientation: Orientation.NORTH_SOUTH,
    });
    const { container } = render(
      <svg>
        <ComponentSymbol component={defaultComp} dpix={10} />
      </svg>,
    );
    const textLabels = Array.from(container.querySelectorAll('text')).map((t) => t.textContent?.trim());
    expect(textLabels).toContain('P');
    expect(textLabels).toContain('S');
    // 4 primary winding paths + 4 secondary winding paths
    const paths = container.querySelectorAll('path');
    expect(paths.length).toBe(8);
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

  it('renders transformer preview icon with square viewBox', () => {
    const { container } = render(<SymbolPreview type={LkComponentType.TRANSFORMER} size={48} />);
    const svg = container.querySelector('svg.symbol-preview-svg');
    expect(svg).toBeTruthy();
    expect(svg?.getAttribute('viewBox')).toBe('-22 -22 44 44');
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

  it('renders specialized symbols for all newly added control blocks', () => {
    const typesWithExpectedText: [number, string][] = [
      [ControlComponentType.ASIN, 'asin'],
      [ControlComponentType.ACOS, 'acos'],
      [ControlComponentType.TAN, 'tan'],
      [ControlComponentType.ATAN, 'atan'],
      [ControlComponentType.SQR, 'x²'],
      [ControlComponentType.POW, 'xʸ'],
      [ControlComponentType.ROUND, 'rnd'],
      [ControlComponentType.SIGN, 'sgn'],
      [ControlComponentType.EQ, '=='],
      [ControlComponentType.NE, '!='],
      [ControlComponentType.COUNTER, 'CNT'],
      [ControlComponentType.ABCDQ, 'abc→dq'],
      [ControlComponentType.DQABC, 'dq→abc'],
      [ControlComponentType.THYR_CTRL, 'ThyrCtrl'],
      [ControlComponentType.PMSM_CONTROL, 'FOC'],
      [ControlComponentType.PMSM_MODULATOR, 'PWM'],
      [ControlComponentType.DEMUX, 'DEMUX'],
      [ControlComponentType.SPACE_VECTOR, 'SVPWM'],
      [ControlComponentType.SDFT, 'SDFT'],
      [ControlComponentType.SPARSEMATRIX, 'SPM'],
    ];

    for (const [type, expectedText] of typesWithExpectedText) {
      const { container } = render(
        <svg>
          <SymbolByType type={type} u={10} family="CONTROL" />
        </svg>,
      );
      expect(container.textContent).toContain(expectedText);
    }
  });

  it('renders European IEC 60617-12 rectangular logic gates with qualifying symbols', () => {
    // AND gate (&)
    const { container: andCont } = render(<svg><SymbolByType type={1011} u={10} family="CONTROL" /></svg>);
    expect(andCont.querySelector('rect')).toBeTruthy();
    expect(andCont.textContent).toContain('&');

    // OR gate (≥1)
    const { container: orCont } = render(<svg><SymbolByType type={1012} u={10} family="CONTROL" /></svg>);
    expect(orCont.querySelector('rect')).toBeTruthy();
    expect(orCont.textContent).toContain('≥1');

    // XOR gate (=1)
    const { container: xorCont } = render(<svg><SymbolByType type={1035} u={10} family="CONTROL" /></svg>);
    expect(xorCont.querySelector('rect')).toBeTruthy();
    expect(xorCont.textContent).toContain('=1');

    // NOT gate (1 with output bubble)
    const { container: notCont } = render(<svg><SymbolByType type={1013} u={10} family="CONTROL" /></svg>);
    expect(notCont.querySelector('rect')).toBeTruthy();
    expect(notCont.querySelector('circle')).toBeTruthy();
    expect(notCont.textContent).toContain('1');
  });

  it('renders European IEC 60617-8 circular measuring instruments (Voltmeter, Ammeter)', () => {
    // Voltmeter (V)
    const { container: vCont } = render(<svg><SymbolByType type={1001} u={10} family="CONTROL" /></svg>);
    expect(vCont.querySelector('circle')).toBeTruthy();
    expect(vCont.textContent?.trim()).toBe('V');

    // Ammeter (A)
    const { container: aCont } = render(<svg><SymbolByType type={1002} u={10} family="CONTROL" /></svg>);
    expect(aCont.querySelector('circle')).toBeTruthy();
    expect(aCont.textContent?.trim()).toBe('A');
  });

  it('renders European standard electrical symbols (IEC resistor box, parallel plate capacitor, motor M)', () => {
    // Resistor: IEC box (rect), not ANSI zigzag
    const { container: resCont } = render(<svg><SymbolByType type={LkComponentType.RESISTOR} u={10} family="LK" /></svg>);
    expect(resCont.querySelector('rect')).toBeTruthy();

    // Motor: circle with centered M
    const { container: motCont } = render(<svg><SymbolByType type={14} u={10} family="LK" /></svg>);
    expect(motCont.querySelector('circle')).toBeTruthy();
    expect(motCont.textContent?.trim()).toBe('M');

    // Voltage source: circle with + and −
    const { container: vsrcCont } = render(<svg><SymbolByType type={LkComponentType.VOLTAGE_SOURCE} u={10} family="LK" /></svg>);
    expect(vsrcCont.querySelector('circle')).toBeTruthy();
    expect(vsrcCont.textContent).toContain('+');
    expect(vsrcCont.textContent).toContain('−');

    // Transistors with device badges
    const { container: bjtCont } = render(<svg><SymbolByType type={LkComponentType.BJT} u={10} family="LK" /></svg>);
    expect(bjtCont.textContent).toContain('BJT');
    const { container: mosCont } = render(<svg><SymbolByType type={28} u={10} family="LK" /></svg>);
    expect(mosCont.textContent).toContain('MOS');
    const { container: igbtCont } = render(<svg><SymbolByType type={10} u={10} family="LK" /></svg>);
    expect(igbtCont.textContent).toContain('IGBT');
  });
});

