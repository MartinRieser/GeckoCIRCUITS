/**
 * SVG symbol rendering for schematic components. Symbols are drawn in a
 * local coordinate system in pixels (u = dpix = one grid unit) around the
 * component origin, in base orientation WEST_EAST (input terminal left at
 * -2u, output right at +2u). Rotation by orientation code is applied
 * via an SVG transform.
 */
import type { EditorComponent } from '../model/types';
import { CTRL_TYPE } from '../model/componentSchema';

/** Rotation angle (deg) that maps WEST_EAST base orientation to the given code. */
export function orientationAngle(orientation: number): number {
  switch (orientation) {
    case 504:
      return 180; // EAST_WEST
    case 503:
      return 90; // NORTH_SOUTH
    case 501:
      return 270; // SOUTH_NORTH
    case 502:
    default:
      return 0; // WEST_EAST
  }
}

/**
 * Rotation angle for CONTROL blocks: the classic editor orients their
 * terminals horizontally for NORTH_SOUTH (controlFlowVector), so the angle
 * is a quarter turn behind the LK orientationAngle.
 */
export function controlOrientationAngle(orientation: number): number {
  switch (orientation) {
    case 504:
      return 90; // EAST_WEST: flow south
    case 501:
      return 180; // SOUTH_NORTH: flow west
    case 502:
      return 270; // WEST_EAST: flow north
    case 503:
    default:
      return 0; // NORTH_SOUTH: flow east
  }
}

const LEAD = 2.0;

export function ComponentSymbol({
  component,
  dpix,
}: {
  component: EditorComponent;
  dpix: number;
}) {
  const u = dpix;
  const angle = component.family === 'CONTROL'
    ? controlOrientationAngle(component.orientation)
    : orientationAngle(component.orientation);
  const inputCount = component.inputLabels?.length || 1;
  return (
    <g transform={`rotate(${angle})`}>
      <SymbolByType type={component.type} u={u} family={component.family} inputCount={inputCount} />
    </g>
  );
}

/** Standalone SVG preview symbol for palette cards, dialogs, and headers. */
export function SymbolPreview({
  type,
  family = 'LK',
  size = 48,
  color = 'currentColor',
}: {
  type: number;
  family?: string;
  size?: number;
  color?: string;
}) {
  const u = 10;
  return (
    <svg
      width={size}
      height={size}
      viewBox={`-${2.4 * u} -${1.8 * u} ${4.8 * u} ${3.6 * u}`}
      style={{ display: 'block', overflow: 'visible', color }}
      className="symbol-preview-svg"
    >
      <g stroke={color} strokeWidth={1.4} fill="none">
        <SymbolByType type={type} u={u} family={family} />
      </g>
    </svg>
  );
}

export function SymbolByType({
  type,
  u,
  family = 'LK',
  inputCount = 1,
}: {
  type: number;
  u: number;
  family?: string;
  inputCount?: number;
}) {
  // CONTROL blocks come in two numbering ranges: legacy classic-editor
  // numbers (1-84) and web catalog numbers (1001+); see CTRL_TYPE. The case
  // labels below are .ipes type keys, not free-form constants.
  if (family === 'CONTROL') {
    switch (type) {
      case 1:
      case 1001:
        return <VoltmeterSymbol u={u} />;
      case 2:
      case 1002:
        return <AmmeterSymbol u={u} />;
      case CTRL_TYPE.LEGACY_CONSTANT:
      case CTRL_TYPE.CONSTANT:
        return <ConstantBlockSymbol u={u} />;
      case CTRL_TYPE.LEGACY_SIGNAL_SOURCE:
      case CTRL_TYPE.SIGNAL_SOURCE:
        return <SignalSourceSymbol u={u} />;
      case CTRL_TYPE.LEGACY_SCOPE:
      case CTRL_TYPE.SCOPE:
        return <ScopeSymbol u={u} inputCount={inputCount} />;
      case CTRL_TYPE.LEGACY_GATE:
        return <GateSymbol u={u} />;
      case 7:
      case 1006:
        return <GainTriangleSymbol u={u} />;
      case 8:
      case 1008:
        return <ControlBlockLabel u={u} label="PT1" />;
      case 9:
        return <ControlBlockLabel u={u} label="PT2" />;
      case 10:
      case 1007:
        return <ControlBlockLabel u={u} label="PI" />;
      case 11:
      case 1010:
        return <ComparatorSymbol u={u} />;
      case 12:
        return <ControlBlockLabel u={u} label="+" />;
      case 13:
        return <ControlBlockLabel u={u} label="−" />;
      case 14:
        return <ControlBlockLabel u={u} label="×" />;
      case 15:
        return <ControlBlockLabel u={u} label="÷" />;
      case 18:
      case 1013:
        return <NotGateSymbol u={u} />;
      case 19:
      case 1011:
        return <AndGateSymbol u={u} />;
      case 20:
      case 1012:
        return <OrGateSymbol u={u} />;
      case 21:
        return <ControlBlockLabel u={u} label="XOR" />;
      case 25:
      case 1015:
        return <ControlBlockLabel u={u} label="τ" />;
      case 26:
        return <ControlBlockLabel u={u} label="S/H" />;
      case 27:
        return <ControlBlockLabel u={u} label="LIM" />;
      case 64:
      case 1009:
        return <ControlBlockLabel u={u} label="∫" />;
      case 84:
      case 1014:
        return <MuxSymbol u={u} />;
      default:
        return <GenericBox u={u} family={family} type={type} />;
    }
  }

  switch (type) {
    case 1:
      return <Resistor u={u} />;
    case 2:
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
    case 9:
      return <CoupledInductor u={u} />;
    case 10:
    case 28:
    case 33:
      return <Transistor type={type} u={u} />;
    case 12:
      return <InductorCoupled u={u} />;
    case 13:
      return <LISNSymbol u={u} />;
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
    case 23:
      return <Transformer u={u} />;
    case 24:
    case 25:
    case 26:
    case 52:
      return <ReluctanceSymbol u={u} type={type} />;
    case 29:
    case 30:
    case 49:
      return <TerminalSymbol u={u} />;
    case 31:
    case 32:
    case 50:
      return <GlobalTerminalSymbol u={u} />;
    case 44:
      return <HeatFlowSource u={u} />;
    case 45:
      return <TemperatureSource u={u} />;
    case 46:
      return <ThermalResistor u={u} />;
    case 47:
      return <ThermalCapacitor u={u} />;
    case 48:
      return <AmbientSymbol u={u} />;
    default:
      return <GenericBox u={u} family={family} type={type} />;
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
      <rect x={-0.85 * u} y={-0.45 * u} width={1.7 * u} height={0.9 * u} rx={1} />
    </g>
  );
}

function Inductor({ u }: { u: number }) {
  const arcs = [];
  for (let i = 0; i < 4; i++) {
    const x = (-0.8 + i * 0.4) * u;
    arcs.push(
      <path
        key={i}
        d={`M ${x} 0 A ${0.2 * u} ${0.25 * u} 0 0 1 ${x + 0.4 * u} 0`}
        strokeLinecap="round"
      />,
    );
  }
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.8 * u} y2={0} />
      <line x1={0.8 * u} y1={0} x2={LEAD * u} y2={0} />
      {arcs}
    </g>
  );
}

function InductorCoupled({ u }: { u: number }) {
  return (
    <g>
      <Inductor u={u} />
      <line x1={-0.6 * u} y1={-0.45 * u} x2={0.6 * u} y2={-0.45 * u} strokeDasharray="2 2" />
    </g>
  );
}

function Capacitor({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.25 * u} y2={0} />
      <line x1={0.25 * u} y1={0} x2={LEAD * u} y2={0} />
      <line x1={-0.25 * u} y1={-0.6 * u} x2={-0.25 * u} y2={0.6 * u} strokeWidth={2} />
      <line x1={0.25 * u} y1={-0.6 * u} x2={0.25 * u} y2={0.6 * u} strokeWidth={2} />
    </g>
  );
}

function VoltageSource({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <circle r={0.85 * u} />
      <text
        x={-0.4 * u}
        y={0.3 * u}
        fontSize={0.7 * u}
        fill="currentColor"
        stroke="none"
        fontWeight="bold"
      >
        +
      </text>
      <text
        x={0.15 * u}
        y={0.3 * u}
        fontSize={0.7 * u}
        fill="currentColor"
        stroke="none"
        fontWeight="bold"
      >
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
      <line x1={-0.45 * u} y1={0} x2={0.35 * u} y2={0} strokeWidth={1.6} />
      <path
        d={`M ${0.35 * u} 0 l ${-0.25 * u} ${-0.18 * u} l 0 ${0.36 * u} z`}
        fill="currentColor"
      />
    </g>
  );
}

function Diode({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <path
        d={`M ${-0.55 * u} ${-0.55 * u} L ${-0.55 * u} ${0.55 * u} L ${0.55 * u} 0 z`}
        fill="rgba(255,255,255,0.08)"
      />
      <line x1={0.55 * u} y1={-0.55 * u} x2={0.55 * u} y2={0.55 * u} strokeWidth={1.8} />
    </g>
  );
}

function SwitchSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.5 * u} y2={0} />
      <line x1={0.5 * u} y1={0} x2={LEAD * u} y2={0} />
      <circle cx={-0.5 * u} cy={0} r={2} fill="currentColor" />
      <circle cx={0.5 * u} cy={0} r={2} fill="currentColor" />
      <line x1={-0.45 * u} y1={0} x2={0.45 * u} y2={-0.5 * u} strokeWidth={1.8} />
    </g>
  );
}

function Thyristor({ u }: { u: number }) {
  return (
    <g>
      <Diode u={u} />
      <line x1={0} y1={0.45 * u} x2={0} y2={0.85 * u} />
      <line x1={0} y1={0.85 * u} x2={0.45 * u} y2={0.85 * u} />
    </g>
  );
}

function Transistor({ type, u }: { type: number; u: number }) {
  const isMosfet = type === 28;
  const isIgbt = type === 10;
  const isBjt = type === 33;

  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.4 * u} y2={0} />
      <line x1={0.4 * u} y1={-0.5 * u} x2={LEAD * u} y2={-0.5 * u} />
      <line x1={0.4 * u} y1={0.5 * u} x2={LEAD * u} y2={0.5 * u} />
      {/* Base / Gate bar */}
      <line x1={-0.35 * u} y1={-0.65 * u} x2={-0.35 * u} y2={0.65 * u} strokeWidth={2} />
      {/* Channel */}
      {isMosfet ? (
        <>
          <line x1={-0.15 * u} y1={-0.6 * u} x2={-0.15 * u} y2={-0.3 * u} />
          <line x1={-0.15 * u} y1={-0.15 * u} x2={-0.15 * u} y2={0.15 * u} />
          <line x1={-0.15 * u} y1={0.3 * u} x2={-0.15 * u} y2={0.6 * u} />
          <line x1={-0.15 * u} y1={-0.45 * u} x2={0.4 * u} y2={-0.5 * u} />
          <line x1={-0.15 * u} y1={0.45 * u} x2={0.4 * u} y2={0.5 * u} />
          <path d={`M ${0.1 * u} ${0.45 * u} l ${-0.2 * u} ${-0.12 * u} l 0 ${0.24 * u} z`} fill="currentColor" />
        </>
      ) : (
        <>
          <line x1={-0.2 * u} y1={-0.6 * u} x2={-0.2 * u} y2={0.6 * u} strokeWidth={isIgbt ? 2 : 1.4} />
          <line x1={-0.2 * u} y1={-0.3 * u} x2={0.4 * u} y2={-0.65 * u} />
          <line x1={-0.2 * u} y1={0.3 * u} x2={0.4 * u} y2={0.65 * u} />
          {/* Emitter arrow */}
          <path d={`M ${0.35 * u} ${0.62 * u} l ${-0.18 * u} ${-0.08 * u} l ${0.08 * u} ${-0.18 * u} z`} fill="currentColor" />
        </>
      )}
      <text x={0.05 * u} y={-0.1 * u} fontSize={0.45 * u} fill="currentColor" stroke="none" fontWeight="bold">
        {isMosfet ? 'MOS' : isIgbt ? 'IGBT' : isBjt ? 'BJT' : 'T'}
      </text>
    </g>
  );
}

function CoupledInductor({ u }: { u: number }) {
  return (
    <g>
      <Inductor u={u} />
      <line x1={-0.7 * u} y1={0.45 * u} x2={0.7 * u} y2={0.45 * u} strokeWidth={1.5} />
      <line x1={-0.7 * u} y1={0.6 * u} x2={0.7 * u} y2={0.6 * u} strokeWidth={1.5} />
    </g>
  );
}

function Transformer({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={-0.45 * u} x2={-0.65 * u} y2={-0.45 * u} />
      <line x1={-LEAD * u} y1={0.45 * u} x2={-0.65 * u} y2={0.45 * u} />
      <line x1={0.65 * u} y1={-0.45 * u} x2={LEAD * u} y2={-0.45 * u} />
      <line x1={0.65 * u} y1={0.45 * u} x2={LEAD * u} y2={0.45 * u} />
      {/* Primary windings */}
      <path d={`M ${-0.65 * u} ${-0.45 * u} A ${0.2 * u} ${0.2 * u} 0 0 1 ${-0.65 * u} 0 A ${0.2 * u} ${0.2 * u} 0 0 1 ${-0.65 * u} ${0.45 * u}`} />
      {/* Secondary windings */}
      <path d={`M ${0.65 * u} ${-0.45 * u} A ${0.2 * u} ${0.2 * u} 0 0 0 ${0.65 * u} 0 A ${0.2 * u} ${0.2 * u} 0 0 0 ${0.65 * u} ${0.45 * u}`} />
      {/* Core lines */}
      <line x1={-0.1 * u} y1={-0.55 * u} x2={-0.1 * u} y2={0.55 * u} strokeWidth={1.5} />
      <line x1={0.1 * u} y1={-0.55 * u} x2={0.1 * u} y2={0.55 * u} strokeWidth={1.5} />
    </g>
  );
}

function Motor({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <circle r={0.85 * u} />
      <text x={-0.35 * u} y={0.32 * u} fontSize={0.75 * u} fill="currentColor" stroke="none" fontWeight="bold">
        M
      </text>
    </g>
  );
}

function OpAmp({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={-0.45 * u} x2={-0.65 * u} y2={-0.45 * u} />
      <line x1={-LEAD * u} y1={0.45 * u} x2={-0.65 * u} y2={0.45 * u} />
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} />
      <path d={`M ${-0.65 * u} ${-0.8 * u} L ${-0.65 * u} ${0.8 * u} L ${0.65 * u} 0 z`} fill="rgba(255,255,255,0.05)" />
      <text x={-0.5 * u} y={-0.25 * u} fontSize={0.5 * u} fill="currentColor" stroke="none" fontWeight="bold">
        −
      </text>
      <text x={-0.5 * u} y={0.55 * u} fontSize={0.5 * u} fill="currentColor" stroke="none" fontWeight="bold">
        +
      </text>
    </g>
  );
}

function ThermalResistor({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <rect x={-0.85 * u} y={-0.45 * u} width={1.7 * u} height={0.9 * u} strokeDasharray="3 2" />
      <text x={-0.35 * u} y={0.25 * u} fontSize={0.45 * u} fill="currentColor" stroke="none">
        Rth
      </text>
    </g>
  );
}

function ThermalCapacitor({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.25 * u} y2={0} />
      <line x1={0.25 * u} y1={0} x2={LEAD * u} y2={0} />
      <line x1={-0.25 * u} y1={-0.6 * u} x2={-0.25 * u} y2={0.6 * u} strokeDasharray="3 2" strokeWidth={2} />
      <line x1={0.25 * u} y1={-0.6 * u} x2={0.25 * u} y2={0.6 * u} strokeDasharray="3 2" strokeWidth={2} />
      <text x={-0.25 * u} y={-0.75 * u} fontSize={0.45 * u} fill="currentColor" stroke="none">
        Cth
      </text>
    </g>
  );
}

function TemperatureSource({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <circle r={0.85 * u} strokeDasharray="4 2" />
      <text x={-0.25 * u} y={0.3 * u} fontSize={0.65 * u} fill="currentColor" stroke="none" fontWeight="bold">
        T
      </text>
    </g>
  );
}

function HeatFlowSource({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <circle r={0.85 * u} strokeDasharray="4 2" />
      <line x1={-0.45 * u} y1={0} x2={0.35 * u} y2={0} strokeWidth={1.6} />
      <path d={`M ${0.35 * u} 0 l ${-0.25 * u} ${-0.18 * u} l 0 ${0.36 * u} z`} fill="currentColor" />
      <text x={-0.2 * u} y={-0.35 * u} fontSize={0.45 * u} fill="currentColor" stroke="none">
        Pth
      </text>
    </g>
  );
}

function AmbientSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={0} y2={0} />
      <circle cx={0} cy={0} r={0.6 * u} />
      <text x={-0.3 * u} y={0.25 * u} fontSize={0.45 * u} fill="currentColor" stroke="none">
        amb
      </text>
    </g>
  );
}

function TerminalSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={0} y2={0} />
      <circle cx={0} cy={0} r={0.3 * u} fill="rgba(255,255,255,0.2)" />
    </g>
  );
}

function GlobalTerminalSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={0} y1={-LEAD * u} x2={0} y2={0} />
      <line x1={-0.7 * u} y1={0} x2={0.7 * u} y2={0} strokeWidth={2} />
      <line x1={-0.45 * u} y1={0.25 * u} x2={0.45 * u} y2={0.25 * u} strokeWidth={1.6} />
      <line x1={-0.2 * u} y1={0.5 * u} x2={0.2 * u} y2={0.5 * u} strokeWidth={1.2} />
    </g>
  );
}

function ReluctanceSymbol({ u, type }: { u: number; type: number }) {
  return (
    <g>
      {leads(u)}
      <rect x={-0.8 * u} y={-0.5 * u} width={1.6 * u} height={1.0 * u} />
      <line x1={-0.8 * u} y1={-0.5 * u} x2={0.8 * u} y2={0.5 * u} />
      <text x={-0.3 * u} y={0.85 * u} fontSize={0.45 * u} fill="currentColor" stroke="none">
        {type === 26 ? 'MMF' : type === 25 ? 'L_rel' : 'Rm'}
      </text>
    </g>
  );
}

function LISNSymbol({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <rect x={-0.8 * u} y={-0.5 * u} width={1.6 * u} height={1.0 * u} />
      <text x={-0.5 * u} y={0.25 * u} fontSize={0.45 * u} fill="currentColor" stroke="none">
        LISN
      </text>
    </g>
  );
}

function GenericBox({
  u,
  family,
  type,
}: {
  u: number;
  family?: string;
  type: number;
}) {
  return (
    <g>
      {leads(u)}
      <rect x={-0.75 * u} y={-0.75 * u} width={1.5 * u} height={1.5 * u} rx={2} />
      <text
        x={0}
        y={0.25 * u}
        fontSize={0.45 * u}
        fill="currentColor"
        stroke="none"
        textAnchor="middle"
      >
        {family ? `${family}${type}` : `T${type}`}
      </text>
    </g>
  );
}

// ========== CONTROL-domain symbols (green #4ade80) ==========

const CTRL_COLOR = '#4ade80';

function controlLeads(u: number) {
  return (
    <>
      <line x1={-LEAD * u} y1={0} x2={-0.75 * u} y2={0} stroke={CTRL_COLOR} />
      <line x1={0.75 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
    </>
  );
}

function VoltmeterSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={-0.4 * u} x2={-0.7 * u} y2={-0.4 * u} stroke={CTRL_COLOR} />
      <line x1={-LEAD * u} y1={0.4 * u} x2={-0.7 * u} y2={0.4 * u} stroke={CTRL_COLOR} />
      <line x1={0.7 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <rect x={-0.7 * u} y={-0.7 * u} width={1.4 * u} height={1.4 * u} rx={3}
            stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.08)" />
      <text x={0} y={0.3 * u} fontSize={0.75 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">V</text>
    </g>
  );
}

function AmmeterSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={-0.4 * u} x2={-0.7 * u} y2={-0.4 * u} stroke={CTRL_COLOR} />
      <line x1={-LEAD * u} y1={0.4 * u} x2={-0.7 * u} y2={0.4 * u} stroke={CTRL_COLOR} />
      <line x1={0.7 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <circle cx={0} cy={0} r={0.65 * u} stroke={CTRL_COLOR} strokeWidth={1.5}
              fill="rgba(74,222,128,0.08)" />
      <text x={0} y={0.3 * u} fontSize={0.75 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">A</text>
    </g>
  );
}

function ScopeSymbol({ u, inputCount = 1 }: { u: number; inputCount?: number }) {
  if (inputCount <= 1) {
    return (
      <g>
        {controlLeads(u)}
        <rect x={-0.75 * u} y={-0.65 * u} width={1.5 * u} height={1.3 * u} rx={3}
              stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
        {/* Mini waveform inside */}
        <path
          d={`M ${-0.5 * u} 0 Q ${-0.25 * u} ${-0.4 * u} 0 0 Q ${0.25 * u} ${0.4 * u} ${0.5 * u} 0`}
          stroke={CTRL_COLOR} strokeWidth={1.2} fill="none" />
      </g>
    );
  }

  // Multi-input Scope
  const step = 2.0; // 2 grid units between input pins
  const totalH = (inputCount - 1) * step;
  const bodyH = Math.max(1.8 * u, (totalH + 1.4) * u);
  const startOffset = -((inputCount - 1) * step) / 2;

  return (
    <g>
      {/* Main Scope Chassis */}
      <rect
        x={-0.9 * u}
        y={-bodyH / 2}
        width={1.8 * u}
        height={bodyH}
        rx={4}
        stroke={CTRL_COLOR}
        strokeWidth={1.6}
        fill="rgba(74,222,128,0.06)"
      />

      {/* Screen Area */}
      <rect
        x={-0.45 * u}
        y={-bodyH / 2 + 0.3 * u}
        width={1.15 * u}
        height={bodyH - 0.6 * u}
        rx={2}
        stroke={CTRL_COLOR}
        strokeWidth={1.0}
        strokeOpacity={0.6}
        fill="rgba(15,23,42,0.6)"
      />

      {/* Mini display waveform */}
      <path
        d={`M ${-0.35 * u} 0 Q ${-0.15 * u} ${-0.35 * u} 0.1 * u 0 Q ${0.35 * u} ${0.35 * u} 0.55 * u 0`}
        stroke={CTRL_COLOR}
        strokeWidth={1.2}
        fill="none"
      />

      {/* Input pins and channel labels */}
      {Array.from({ length: inputCount }).map((_, i) => {
        const offset = (startOffset + i * step) * u;
        return (
          <g key={i}>
            <line
              x1={-LEAD * u}
              y1={offset}
              x2={-0.9 * u}
              y2={offset}
              stroke={CTRL_COLOR}
              strokeWidth={1.5}
            />
            <text
              x={-0.65 * u}
              y={offset + 0.25 * u}
              fontSize={0.4 * u}
              fill={CTRL_COLOR}
              stroke="none"
              fontWeight="bold"
            >
              {i + 1}
            </text>
          </g>
        );
      })}
    </g>
  );
}

function SignalSourceSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={0.75 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <rect x={-0.75 * u} y={-0.65 * u} width={1.5 * u} height={1.3 * u} rx={3}
            stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <path
        d={`M ${-0.45 * u} 0 Q ${-0.2 * u} ${-0.35 * u} 0 0 Q ${0.2 * u} ${0.35 * u} ${0.45 * u} 0`}
        stroke={CTRL_COLOR} strokeWidth={1.4} fill="none" />
    </g>
  );
}

function ConstantBlockSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={0.75 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <rect x={-0.65 * u} y={-0.55 * u} width={1.3 * u} height={1.1 * u} rx={3}
            stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={0} y={0.25 * u} fontSize={0.65 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">k</text>
    </g>
  );
}

function GateSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.6 * u} y2={0} stroke={CTRL_COLOR} />
      <path
        d={`M ${-0.6 * u} ${-0.6 * u} L ${-0.6 * u} ${0.6 * u} L ${0.6 * u} 0 z`}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.1)"
      />
      <text
        x={-0.15 * u}
        y={0.25 * u}
        fontSize={0.55 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        G
      </text>
    </g>
  );
}

function GainTriangleSymbol({ u }: { u: number }) {
  return (
    <g>
      {controlLeads(u)}
      <path
        d={`M ${-0.65 * u} ${-0.65 * u} L ${-0.65 * u} ${0.65 * u} L ${0.65 * u} 0 z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={-0.2 * u} y={0.2 * u} fontSize={0.5 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">k</text>
    </g>
  );
}

function ControlBlockLabel({ u, label }: { u: number; label: string }) {
  return (
    <g>
      {controlLeads(u)}
      <rect x={-0.75 * u} y={-0.55 * u} width={1.5 * u} height={1.1 * u} rx={3}
            stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={0} y={0.25 * u} fontSize={0.55 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">{label}</text>
    </g>
  );
}

function ComparatorSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={-0.35 * u} x2={-0.65 * u} y2={-0.35 * u} stroke={CTRL_COLOR} />
      <line x1={-LEAD * u} y1={0.35 * u} x2={-0.65 * u} y2={0.35 * u} stroke={CTRL_COLOR} />
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <path
        d={`M ${-0.65 * u} ${-0.7 * u} L ${-0.65 * u} ${0.7 * u} L ${0.65 * u} 0 z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={-0.35 * u} y={-0.15 * u} fontSize={0.4 * u} fill={CTRL_COLOR} stroke="none" fontWeight="bold">+</text>
      <text x={-0.35 * u} y={0.5 * u} fontSize={0.4 * u} fill={CTRL_COLOR} stroke="none" fontWeight="bold">−</text>
    </g>
  );
}

function AndGateSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={-0.35 * u} x2={-0.5 * u} y2={-0.35 * u} stroke={CTRL_COLOR} />
      <line x1={-LEAD * u} y1={0.35 * u} x2={-0.5 * u} y2={0.35 * u} stroke={CTRL_COLOR} />
      <line x1={0.6 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <path
        d={`M ${-0.5 * u} ${-0.6 * u} L ${-0.5 * u} ${0.6 * u} L 0 ${0.6 * u} A ${0.6 * u} ${0.6 * u} 0 0 0 0 ${-0.6 * u} z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={-0.1 * u} y={0.2 * u} fontSize={0.4 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">&amp;</text>
    </g>
  );
}

function OrGateSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={-0.35 * u} x2={-0.35 * u} y2={-0.35 * u} stroke={CTRL_COLOR} />
      <line x1={-LEAD * u} y1={0.35 * u} x2={-0.35 * u} y2={0.35 * u} stroke={CTRL_COLOR} />
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <path
        d={`M ${-0.45 * u} ${-0.6 * u} Q ${0.1 * u} ${-0.6 * u} ${0.65 * u} 0 Q ${0.1 * u} ${0.6 * u} ${-0.45 * u} ${0.6 * u} Q ${-0.15 * u} 0 ${-0.45 * u} ${-0.6 * u} z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={0} y={0.2 * u} fontSize={0.4 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">≥1</text>
    </g>
  );
}

function NotGateSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.6 * u} y2={0} stroke={CTRL_COLOR} />
      <line x1={0.75 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <path
        d={`M ${-0.6 * u} ${-0.55 * u} L ${-0.6 * u} ${0.55 * u} L ${0.55 * u} 0 z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <circle cx={0.65 * u} cy={0} r={0.1 * u} stroke={CTRL_COLOR} strokeWidth={1.5}
              fill="rgba(74,222,128,0.1)" />
    </g>
  );
}

function MuxSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={-0.35 * u} x2={-0.55 * u} y2={-0.35 * u} stroke={CTRL_COLOR} />
      <line x1={-LEAD * u} y1={0.35 * u} x2={-0.55 * u} y2={0.35 * u} stroke={CTRL_COLOR} />
      <line x1={0.55 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      {/* Trapezoid shape */}
      <path
        d={`M ${-0.55 * u} ${-0.65 * u} L ${0.55 * u} ${-0.4 * u} L ${0.55 * u} ${0.4 * u} L ${-0.55 * u} ${0.65 * u} z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={0} y={0.2 * u} fontSize={0.4 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">MUX</text>
    </g>
  );
}

