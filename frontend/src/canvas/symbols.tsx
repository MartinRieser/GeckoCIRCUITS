/**
 * SVG symbol rendering for schematic components. Symbols are drawn in a
 * local coordinate system in pixels (u = dpix = one grid unit) around the
 * component origin, in base orientation WEST_EAST (input terminal left at
 * -2u, output right at +2u). Rotation by orientation code is applied
 * via an SVG transform.
 */
import type { EditorComponent } from '../model/types';
import { CTRL_TYPE, resolveComponentPinCounts } from '../model/componentSchema';
import { Orientation, CANVAS_METRICS, LkComponentType } from '../model/constants';
import { TWO_PORT_DIST } from '../model/geometry';

/** Rotation angle (deg) that maps WEST_EAST base orientation to the given code. */
export function orientationAngle(orientation: number): number {
  switch (orientation) {
    case Orientation.EAST_WEST:
      return 180;
    case Orientation.NORTH_SOUTH:
      return 90;
    case Orientation.SOUTH_NORTH:
      return 270;
    case Orientation.WEST_EAST:
    default:
      return 0;
  }
}

/**
 * Rotation angle for CONTROL blocks: the classic editor orients their
 * terminals horizontally for NORTH_SOUTH (controlFlowVector), so the angle
 * is a quarter turn behind the LK orientationAngle.
 */
export function controlOrientationAngle(orientation: number): number {
  switch (orientation) {
    case Orientation.SOUTH_NORTH:
      return 180;
    case Orientation.WEST_EAST:
      return 270;
    case Orientation.EAST_WEST:
      return 90;
    case Orientation.NORTH_SOUTH:
    default:
      return 0;
  }
}

/**
 * Rotation angle for TRANSFORMER: the base symbol is drawn in NORTH_SOUTH (503)
 * (upright: primary winding on the left at x=-1, secondary on the right at x=+1).
 * The rotation cycle 503 -> 504 -> 501 -> 502 steps clockwise by 90°:
 * - 503 (NORTH_SOUTH): 0° (leads at (-1, ±2) and (+1, ±2))
 * - 504 (EAST_WEST):   90° (leads at (±2, -1) and (±2, +1))
 * - 501 (SOUTH_NORTH): 180° (leads at (+1, ±2) and (-1, ±2))
 * - 502 (WEST_EAST):   270° (leads at (±2, +1) and (±2, -1))
 * Matches classicPinOffset in geometry.ts and NetlistBuilder in backend.
 */
export function transformerOrientationAngle(orientation: number): number {
  switch (orientation) {
    case Orientation.EAST_WEST:
      return 90;
    case Orientation.SOUTH_NORTH:
      return 180;
    case Orientation.WEST_EAST:
      return 270;
    case Orientation.NORTH_SOUTH:
    default:
      return 0;
  }
}

const LEAD = CANVAS_METRICS.LEAD_LENGTH;

/**
 * Renders the component's body symbol, rotated to match its orientation.
 * Terminals are rendered separately by the sheet so wiring hit-targets
 * stay in sheet-space coordinates.
 */
export function ComponentSymbol({
  component,
  dpix,
}: {
  component: EditorComponent;
  dpix: number;
}) {
  const u = dpix;
  const isTransformer =
    (component.family === 'LK' || !component.family) &&
    (component.type === 23 || component.type === LkComponentType.TRANSFORMER);
  const isGlobalTerminal =
    (component.family === 'LK' || !component.family) &&
    (component.type === 31 || component.type === LkComponentType.GLOBAL_TERMINAL);
  const angle = component.family === 'CONTROL' || isGlobalTerminal
    ? controlOrientationAngle(component.orientation)
    : isTransformer
      ? transformerOrientationAngle(component.orientation)
      : orientationAngle(component.orientation);
  const { inputCount, outputCount } = resolveComponentPinCounts(component);
  return (
    <g transform={`rotate(${angle})`}>
      <SymbolByType
        type={component.type}
        u={u}
        family={component.family}
        inputCount={inputCount}
        outputCount={outputCount}
      />
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
  const u = CANVAS_METRICS.PREVIEW_SYMBOL_U;
  const isTransformer =
    (family === 'LK' || !family) &&
    (type === 23 || type === LkComponentType.TRANSFORMER);
  const viewBox = isTransformer
    ? `-${2.2 * u} -${2.2 * u} ${4.4 * u} ${4.4 * u}`
    : `-${2.4 * u} -${1.8 * u} ${4.8 * u} ${3.6 * u}`;
  return (
    <svg
      width={size}
      height={size}
      viewBox={viewBox}
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
  outputCount = 1,
}: {
  type: number;
  u: number;
  family?: string;
  inputCount?: number;
  outputCount?: number;
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
      case 1031:
        return <ControlBlockLabel u={u} label="PT2" />;
      case 10:
      case 1007:
        return <ControlBlockLabel u={u} label="PI" />;
      case 11:
      case 1030:
        return <ControlBlockLabel u={u} label="HYS" />;
      case 12:
      case 1018:
        return <TwoInputControlSymbol u={u} label="+" />;
      case 13:
      case 1017:
        return <TwoInputControlSymbol u={u} label="−" in0Label="+" in1Label="−" />;
      case 14:
      case 1019:
        return <TwoInputControlSymbol u={u} label="×" />;
      case 15:
      case 1020:
        return <TwoInputControlSymbol u={u} label="÷" in0Label="num" in1Label="den" />;
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
      case 1035:
        return <XorGateSymbol u={u} />;
      case 25:
      case 1015:
        return <ControlBlockLabel u={u} label="τ" />;
      case 26:
      case 1033:
        return <TwoInputControlSymbol u={u} label="S/H" in0Label="d" in1Label="clk" />;
      case 27:
      case 1021:
        return <ControlBlockLabel u={u} label="LIM" />;
      case 29:
      case 1032:
        return <ControlBlockLabel u={u} label="PD" />;
      case 32:
      case 1022:
        return <ControlBlockLabel u={u} label="|x|" />;
      case 34:
      case 1026:
        return <ControlBlockLabel u={u} label="sin" />;
      case 36:
      case 1027:
        return <ControlBlockLabel u={u} label="cos" />;
      case 40:
      case 1024:
        return <ControlBlockLabel u={u} label="exp" />;
      case 41:
      case 1025:
        return <ControlBlockLabel u={u} label="ln" />;
      case 43:
      case 1023:
        return <ControlBlockLabel u={u} label="√" />;
      case 45:
      case 1036:
        return <TwoInputControlSymbol u={u} label="≥" />;
      case 46:
      case 1010:
        return <ComparatorSymbol u={u} />;
      case 49:
      case 1028:
        return <TwoInputControlSymbol u={u} label="min" />;
      case 50:
      case 1029:
        return <TwoInputControlSymbol u={u} label="max" />;
      case 58:
      case 1034:
        return <TimeSourceSymbol u={u} />;
      case 64:
      case 1009:
        return <ControlBlockLabel u={u} label="∫" />;
      case 84:
      case 1014:
        return <MuxSymbol u={u} />;
      case 1037:
        return <DeadTimeSymbol u={u} />;
      case CTRL_TYPE.LEGACY_JAVA_FUNCTION:
      case CTRL_TYPE.SCRIPT: {
        const label = type === CTRL_TYPE.LEGACY_JAVA_FUNCTION ? 'JAVA' : 'f(x)';
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label={label} />;
      }
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
      return <MutualCouplingSymbol u={u} />;
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

function MutualCouplingSymbol({ u }: { u: number }) {
  return (
    <g className="symbol-mutual-coupling">
      {/* Two parallel magnetic core lines */}
      <line x1={-3} y1={-1.2 * u} x2={-3} y2={1.2 * u} stroke="currentColor" strokeWidth={2} strokeLinecap="round" />
      <line x1={3} y1={-1.2 * u} x2={3} y2={1.2 * u} stroke="currentColor" strokeWidth={2} strokeLinecap="round" />
      {/* Magnetic coupling core badge */}
      <rect x={-0.45 * u} y={-0.3 * u} width={0.9 * u} height={0.6 * u} rx={3} style={{ fill: 'var(--pill-bg)' }} stroke="currentColor" strokeWidth={1} />
      <text x={0} y={0.14 * u} textAnchor="middle" fontSize={0.38 * u} fill="currentColor" stroke="none" fontWeight="bold">
        k
      </text>
    </g>
  );
}

function Transformer({ u }: { u: number }) {
  const arcRadiusX = 0.35 * u;
  const arcRadiusY = 0.3 * u;
  const primArcs = [];
  const secArcs = [];

  for (let i = 0; i < 4; i++) {
    const yStart = (-1.2 + i * 0.6) * u;
    const yEnd = (-0.6 + i * 0.6) * u;
    // Primary (left): starts at x = -1*u, bulges inward towards core (sweep-flag 0)
    primArcs.push(
      <path
        key={`p-${i}`}
        d={`M ${-1 * u} ${yStart} A ${arcRadiusX} ${arcRadiusY} 0 0 0 ${-1 * u} ${yEnd}`}
        strokeLinecap="round"
      />,
    );
    // Secondary (right): starts at x = 1*u, bulges inward towards core (sweep-flag 1)
    secArcs.push(
      <path
        key={`s-${i}`}
        d={`M ${1 * u} ${yStart} A ${arcRadiusX} ${arcRadiusY} 0 0 1 ${1 * u} ${yEnd}`}
        strokeLinecap="round"
      />,
    );
  }

  return (
    <g>
      {/* Primary leads connecting exactly to terminals at (-1*u, ±2*u) */}
      <line x1={-1 * u} y1={-2 * u} x2={-1 * u} y2={-1.2 * u} />
      <line x1={-1 * u} y1={1.2 * u} x2={-1 * u} y2={2 * u} />
      {primArcs}

      {/* Secondary leads connecting exactly to terminals at (1*u, ±2*u) */}
      <line x1={1 * u} y1={-2 * u} x2={1 * u} y2={-1.2 * u} />
      <line x1={1 * u} y1={1.2 * u} x2={1 * u} y2={2 * u} />
      {secArcs}

      {/* Magnetic core lines */}
      <line x1={-0.12 * u} y1={-1.3 * u} x2={-0.12 * u} y2={1.3 * u} strokeWidth={1.5} />
      <line x1={0.12 * u} y1={-1.3 * u} x2={0.12 * u} y2={1.3 * u} strokeWidth={1.5} />

      {/* Polarity dots at top pins (P1 and S1) */}
      <circle cx={-0.65 * u} cy={-1.6 * u} r={0.12 * u} fill="currentColor" stroke="none" />
      <circle cx={0.65 * u} cy={-1.6 * u} r={0.12 * u} fill="currentColor" stroke="none" />

      {/* Winding indicator labels */}
      <text
        x={-1.45 * u}
        y={0.15 * u}
        textAnchor="middle"
        fontSize={0.45 * u}
        fill="currentColor"
        stroke="none"
        opacity={0.65}
        fontWeight="bold"
      >
        P
      </text>
      <text
        x={1.45 * u}
        y={0.15 * u}
        textAnchor="middle"
        fontSize={0.45 * u}
        fill="currentColor"
        stroke="none"
        opacity={0.65}
        fontWeight="bold"
      >
        S
      </text>
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
      <line x1={0.7 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} />
      <circle cx={0} cy={0} r={0.65 * u} stroke={CTRL_COLOR} strokeWidth={1.5}
              fill="rgba(74,222,128,0.08)" />
      <text x={0} y={0.3 * u} fontSize={0.75 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">A</text>
    </g>
  );
}

function ScopeSymbol({ u, inputCount = 1 }: { u: number; inputCount?: number }) {
  // Anchored pin block, in lockstep with terminalPositions: channel 1 sits
  // on the component's anchor row, further channels extend downward (step
  // MULTI_PIN_STEP). The chassis is top-anchored too, so adding a channel
  // appends a pin + chassis row without moving anything above.
  const step = CANVAS_METRICS.MULTI_PIN_STEP;
  const span = (inputCount - 1) * step; // y-offset of the last pin
  const top = -0.9 * u;
  const bottom = (span + 0.9) * u;
  const bodyH = bottom - top;
  const midY = (top + bottom) / 2;

  return (
    <g>
      {/* Main Scope Chassis */}
      <rect
        x={-0.9 * u}
        y={top}
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
        y={top + 0.3 * u}
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
        d={`M ${-0.35 * u} ${midY} Q ${-0.15 * u} ${midY - 0.35 * u} ${0.1 * u} ${midY} Q ${0.35 * u} ${midY + 0.35 * u} ${0.55 * u} ${midY}`}
        stroke={CTRL_COLOR}
        strokeWidth={1.2}
        fill="none"
      />

      {/* Input pins and channel labels */}
      {Array.from({ length: inputCount }).map((_, i) => {
        const offset = i * step * u;
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

function TwoInputControlSymbol({
  u,
  label,
  in0Label,
  in1Label,
}: {
  u: number;
  label: string;
  in0Label?: string;
  in1Label?: string;
}) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.75 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-0.75 * u} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.75 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={-0.75 * u}
        y={-0.55 * u}
        width={1.5 * u}
        height={2.1 * u}
        rx={3}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={0}
        y={0.7 * u}
        fontSize={0.6 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        {label}
      </text>
      {in0Label && (
        <text x={-0.45 * u} y={0.25 * u} fontSize={0.35 * u} fill={CTRL_COLOR} stroke="none" fontWeight="bold">
          {in0Label}
        </text>
      )}
      {in1Label && (
        <text x={-0.45 * u} y={1.25 * u} fontSize={0.35 * u} fill={CTRL_COLOR} stroke="none" fontWeight="bold">
          {in1Label}
        </text>
      )}
    </g>
  );
}

function DeadTimeSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.75 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.75 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.75 * u} y1={1 * u} x2={LEAD * u} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={-0.75 * u}
        y={-0.55 * u}
        width={1.5 * u}
        height={2.1 * u}
        rx={3}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={-0.15 * u}
        y={0.65 * u}
        fontSize={0.45 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        DT
      </text>
      <text x={0.48 * u} y={0.25 * u} fontSize={0.32 * u} fill={CTRL_COLOR} stroke="none" textAnchor="end" fontWeight="bold">
        H
      </text>
      <text x={0.48 * u} y={1.25 * u} fontSize={0.32 * u} fill={CTRL_COLOR} stroke="none" textAnchor="end" fontWeight="bold">
        L
      </text>
    </g>
  );
}

function TimeSourceSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={0.75 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <circle cx={0} cy={0} r={0.75 * u} stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={0} y={0.28 * u} fontSize={0.65 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">t</text>
    </g>
  );
}

function XorGateSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.45 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-0.45 * u} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <path
        d={`M ${-0.55 * u} ${-0.45 * u} Q ${-0.3 * u} ${0.5 * u} ${-0.55 * u} ${1.45 * u}`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="none" />
      <path
        d={`M ${-0.45 * u} ${-0.45 * u} Q 0 ${-0.45 * u} ${0.65 * u} 0 Q 0 ${1.45 * u} ${-0.45 * u} ${1.45 * u} Q ${-0.2 * u} ${0.5 * u} ${-0.45 * u} ${-0.45 * u} z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={0} y={0.65 * u} fontSize={0.4 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">=1</text>
    </g>
  );
}

function ComparatorSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.65 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-0.65 * u} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <path
        d={`M ${-0.65 * u} ${-0.45 * u} L ${-0.65 * u} ${1.45 * u} L ${0.65 * u} 0 z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={-0.38 * u} y={0.25 * u} fontSize={0.38 * u} fill={CTRL_COLOR} stroke="none" fontWeight="bold">+</text>
      <text x={-0.38 * u} y={0.9 * u} fontSize={0.38 * u} fill={CTRL_COLOR} stroke="none" fontWeight="bold">−</text>
    </g>
  );
}

function AndGateSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.5 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-0.5 * u} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.6 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <path
        d={`M ${-0.5 * u} ${-0.45 * u} L ${-0.5 * u} ${1.45 * u} L 0 ${1.45 * u} Q ${0.7 * u} ${1.45 * u} ${0.6 * u} 0 Q ${0.7 * u} ${-0.45 * u} 0 ${-0.45 * u} z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={-0.1 * u} y={0.65 * u} fontSize={0.4 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">&amp;</text>
    </g>
  );
}

function OrGateSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.35 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-0.35 * u} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <path
        d={`M ${-0.45 * u} ${-0.45 * u} Q 0 ${-0.45 * u} ${0.65 * u} 0 Q 0 ${1.45 * u} ${-0.45 * u} ${1.45 * u} Q ${-0.2 * u} ${0.5 * u} ${-0.45 * u} ${-0.45 * u} z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={0} y={0.65 * u} fontSize={0.4 * u} fill={CTRL_COLOR} stroke="none"
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
      <line x1={-LEAD * u} y1={0} x2={-0.55 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-0.55 * u} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={2 * u} x2={-0.55 * u} y2={2 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.55 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      {/* Trapezoid shape */}
      <path
        d={`M ${-0.55 * u} ${-0.45 * u} L ${0.55 * u} ${-0.45 * u} L ${0.55 * u} ${0.45 * u} L ${-0.55 * u} ${2.45 * u} z`}
        stroke={CTRL_COLOR} strokeWidth={1.5} fill="rgba(74,222,128,0.06)" />
      <text x={0} y={0.7 * u} fontSize={0.38 * u} fill={CTRL_COLOR} stroke="none"
            textAnchor="middle" fontWeight="bold">MUX</text>
    </g>
  );
}

function ScriptFunctionBlockSymbol({
  u,
  inCount = 1,
  outCount = 1,
  label = 'f(x)',
}: {
  u: number;
  inCount?: number;
  outCount?: number;
  label?: string;
}) {
  // Anchored unit-step pin block, in lockstep with terminalPositions and
  // the simulation core's terminal grid: input i on row i, output j on row
  // j, first pin on the component's anchor row, the block grows downward.
  const step = 1.0; // must match SCRIPT_PIN_STEP in model/geometry.ts
  const maxPins = Math.max(inCount, outCount, 1);
  const span = (maxPins - 1) * step;
  const top = -0.8 * u;
  const bottom = (span + 0.8) * u;
  const boxH = Math.max(1.6 * u, bottom - top);
  const boxW = 2.0 * u;
  const midY = (top + bottom) / 2;

  return (
    <g className="symbol-control-script">
      {/* Input leads */}
      {Array.from({ length: inCount }).map((_, i) => {
        const yOffset = i * step * u;
        return (
          <line
            key={`in-lead-${i}`}
            x1={-TWO_PORT_DIST * u}
            y1={yOffset}
            x2={-boxW / 2}
            y2={yOffset}
            stroke={CTRL_COLOR}
            strokeWidth={1.5}
          />
        );
      })}

      {/* Output leads */}
      {Array.from({ length: outCount }).map((_, j) => {
        const yOffset = j * step * u;
        return (
          <line
            key={`out-lead-${j}`}
            x1={boxW / 2}
            y1={yOffset}
            x2={TWO_PORT_DIST * u}
            y2={yOffset}
            stroke={CTRL_COLOR}
            strokeWidth={1.5}
          />
        );
      })}

      {/* Main Function Chassis */}
      <rect
        x={-boxW / 2}
        y={top}
        width={boxW}
        height={boxH}
        rx={4}
        stroke={CTRL_COLOR}
        strokeWidth={1.6}
        fill="rgba(74,222,128,0.08)"
      />

      {/* Pin index labels for multiple pins */}
      {inCount > 1 &&
        Array.from({ length: inCount }).map((_, i) => {
          const yOffset = i * step * u;
          return (
            <text
              key={`in-label-${i}`}
              x={-boxW / 2 + 0.25 * u}
              y={yOffset + 0.15 * u}
              fontSize={0.4 * u}
              fontFamily="monospace"
              fill="rgba(74,222,128,0.7)"
              stroke="none"
              textAnchor="start"
            >
              {`u${i + 1}`}
            </text>
          );
        })}

      {outCount > 1 &&
        Array.from({ length: outCount }).map((_, j) => {
          const yOffset = j * step * u;
          return (
            <text
              key={`out-label-${j}`}
              x={boxW / 2 - 0.25 * u}
              y={yOffset + 0.15 * u}
              fontSize={0.4 * u}
              fontFamily="monospace"
              fill="rgba(74,222,128,0.7)"
              stroke="none"
              textAnchor="end"
            >
              {`y${j + 1}`}
            </text>
          );
        })}

      {/* Central Badge label e.g. f(x) or JAVA */}
      <text
        x={0}
        y={midY + 0.25 * u}
        fontSize={0.65 * u}
        fontFamily="monospace"
        fontWeight="bold"
        fontStyle={label === 'f(x)' ? 'italic' : 'normal'}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
      >
        {label}
      </text>
    </g>
  );
}

