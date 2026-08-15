/**
 * SVG symbol rendering for schematic components. Symbols are drawn in a
 * local coordinate system in pixels (u = dpix = one grid unit) around the
 * component origin, in base orientation WEST_EAST (input terminal left at
 * -2u, output right at +2u) — the same visual language as the Swing
 * editor's Graphics2D painting. Rotation by orientation code is applied
 * via an SVG transform.
 */
import type { EditorComponent } from '../model/types';

/** Rotation angle (deg) that maps WEST_EAST base orientation to the given code. */
export function orientationAngle(orientation: number): number {
  switch (orientation) {
    case 504:
      return 180; // EAST_WEST
    case 503:
      return 90; // NORTH_SOUTH
    case 501:
      return 270; // SOUTH_NORTH
    default:
      return 0; // WEST_EAST
  }
}

const LEAD = 2.0;

export function ComponentSymbol({ component, dpix }: { component: EditorComponent; dpix: number }) {
  const u = dpix;
  return (
    <g transform={`rotate(${orientationAngle(component.orientation)})`}>
      <SymbolByType type={component.type} u={u} />
    </g>
  );
}

function SymbolByType({ type, u }: { type: number; u: number }) {
  switch (type) {
    case 1:
      return <Resistor u={u} />;
    case 2:
    case 12:
      return <Inductor u={u} />;
    case 3:
      return <Capacitor u={u} />;
    case 4:
      return <VoltageSource u={u} />;
    case 5:
      return <CurrentSource u={u} />;
    case 6:
      return <Diode u={u} />;
    case 7:
      return <SwitchSymbol u={u} />;
    case 8:
      return <Thyristor u={u} />;
    case 10:
    case 28:
    case 33:
      return <Transistor type={type} u={u} />;
    case 9:
      return <CoupledInductor u={u} />;
    case 23:
      return <Transformer u={u} />;
    case 14:
    case 15:
    case 16:
    case 17:
    case 18:
    case 20:
    case 21:
    case 51:
      return <Motor u={u} />;
    case 22:
      return <OpAmp u={u} />;
    case 46:
      return <Resistor u={u} />; // thermal resistance: same zig-zag box
    case 47:
      return <Capacitor u={u} />; // thermal capacitance
    default:
      return <GenericBox u={u} />;
  }
}

function leads(u: number) {
  return (
    <>
      <line x1={-LEAD * u} y1={0} x2={-0.85 * u} y2={0} />
      <line x1={0.85 * u} y1={0} x2={LEAD * u} y2={0} />
    </>
  );
}

function Resistor({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <rect x={-0.85 * u} y={-0.45 * u} width={1.7 * u} height={0.9 * u} />
    </g>
  );
}

function Inductor({ u }: { u: number }) {
  const arcs = [];
  for (let i = 0; i < 4; i++) {
    const x = (-0.75 + i * 0.5) * u;
    arcs.push(<path key={i} d={`M ${x} 0 A ${0.25 * u} ${0.25 * u} 0 0 1 ${x + 0.5 * u} 0`} />);
  }
  return (
    <g>
      {leads(u)}
      {arcs}
    </g>
  );
}

function Capacitor({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <line x1={-0.2 * u} y1={-0.5 * u} x2={-0.2 * u} y2={0.5 * u} />
      <line x1={0.2 * u} y1={-0.5 * u} x2={0.2 * u} y2={0.5 * u} />
    </g>
  );
}

function VoltageSource({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <circle r={0.85 * u} />
      <text x={-0.45 * u} y={0.28 * u} fontSize={0.7 * u}>
        +
      </text>
      <text x={0.15 * u} y={0.28 * u} fontSize={0.7 * u}>
        −
      </text>
    </g>
  );
}

function CurrentSource({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <circle r={0.85 * u} />
      <line x1={-0.4 * u} y1={0} x2={0.3 * u} y2={0} />
      <path d={`M ${0.3 * u} 0 l ${-0.2 * u} ${-0.15 * u} l 0 ${0.3 * u} z`} />
    </g>
  );
}

function Diode({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <path d={`M ${-0.55 * u} ${-0.5 * u} L ${-0.55 * u} ${0.5 * u} L ${0.55 * u} 0 z`} />
      <line x1={0.55 * u} y1={-0.5 * u} x2={0.55 * u} y2={0.5 * u} />
    </g>
  );
}

function SwitchSymbol({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <circle cx={-0.55 * u} cy={0} r={1.5} />
      <circle cx={0.55 * u} cy={0} r={1.5} />
      <line x1={-0.5 * u} y1={0} x2={0.45 * u} y2={-0.4 * u} />
    </g>
  );
}

function Thyristor({ u }: { u: number }) {
  return (
    <g>
      <Diode u={u} />
      <line x1={0} y1={0.5 * u} x2={0} y2={0.85 * u} />
      <line x1={0} y1={0.85 * u} x2={0.4 * u} y2={0.85 * u} />
    </g>
  );
}

function Transistor({ type, u }: { type: number; u: number }) {
  const label = type === 10 ? 'T' : type === 28 ? 'M' : 'B';
  return (
    <g>
      {leads(u)}
      <line x1={-0.3 * u} y1={-0.5 * u} x2={-0.3 * u} y2={0.5 * u} />
      <line x1={-0.3 * u} y1={-0.3 * u} x2={0.3 * u} y2={-0.3 * u} />
      <line x1={-0.3 * u} y1={0.3 * u} x2={0.3 * u} y2={0.3 * u} />
      <line x1={0.3 * u} y1={-0.3 * u} x2={0.3 * u} y2={-0.7 * u} />
      <line x1={0.3 * u} y1={0.3 * u} x2={0.3 * u} y2={0.7 * u} />
      <text x={-0.15 * u} y={0.32 * u} fontSize={0.55 * u}>
        {label}
      </text>
    </g>
  );
}

function CoupledInductor({ u }: { u: number }) {
  return (
    <g>
      <Inductor u={u} />
      <line x1={-0.3 * u} y1={0.55 * u} x2={0.3 * u} y2={0.55 * u} />
      <line x1={-0.3 * u} y1={0.7 * u} x2={0.3 * u} y2={0.7 * u} />
    </g>
  );
}

function Transformer({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <path d={`M ${-0.7 * u} ${-0.4 * u} a ${0.25 * u} ${0.25 * u} 0 0 1 ${0.5 * u} 0 a ${0.25 * u} ${0.25 * u} 0 0 1 ${0.5 * u} 0`} />
      <path d={`M ${-0.7 * u} ${0.4 * u} a ${0.25 * u} ${0.25 * u} 0 0 1 ${0.5 * u} 0 a ${0.25 * u} ${0.25 * u} 0 0 1 ${0.5 * u} 0`} />
      <line x1={-0.15 * u} y1={-0.5 * u} x2={-0.15 * u} y2={0.5 * u} />
      <line x1={0.15 * u} y1={-0.5 * u} x2={0.15 * u} y2={0.5 * u} />
    </g>
  );
}

function Motor({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <circle r={0.85 * u} />
      <text x={-0.3 * u} y={0.3 * u} fontSize={0.75 * u}>
        M
      </text>
    </g>
  );
}

function OpAmp({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <path d={`M ${-0.6 * u} ${-0.7 * u} L ${-0.6 * u} ${0.7 * u} L ${0.7 * u} 0 z`} />
      <text x={-0.45 * u} y={-0.25 * u} fontSize={0.45 * u}>
        −
      </text>
      <text x={-0.45 * u} y={0.45 * u} fontSize={0.45 * u}>
        +
      </text>
    </g>
  );
}

function GenericBox({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <rect x={-0.7 * u} y={-0.7 * u} width={1.4 * u} height={1.4 * u} />
    </g>
  );
}
