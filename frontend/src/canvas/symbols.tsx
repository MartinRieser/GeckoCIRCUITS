/**
 * SVG symbol rendering for schematic components. Symbols are drawn in a
 * local coordinate system in pixels (u = dpix = one grid unit) around the
 * component origin, in base orientation WEST_EAST (input terminal left at
 * -2u, output right at +2u). Rotation by orientation code is applied
 * via an SVG transform.
 */
import type { EditorComponent } from '../model/types';
import { CTRL_TYPE, resolveComponentPinCounts, getComponentMeta } from '../model/componentSchema';
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
  const meta = getComponentMeta(component.type, component.family, component.name);
  const inLabels = component.inputLabels && component.inputLabels.length > 0
    ? component.inputLabels
    : meta?.terminals?.input?.map((t) => t.label);
  const outLabels = component.outputLabels && component.outputLabels.length > 0
    ? component.outputLabels
    : meta?.terminals?.output?.map((t) => t.label);

  return (
    <g transform={`rotate(${angle})`}>
      <SymbolByType
        type={component.type}
        u={u}
        family={component.family}
        inputCount={inputCount}
        outputCount={outputCount}
        inLabels={inLabels}
        outLabels={outLabels}
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
  const meta = getComponentMeta(type, family);
  const inCount = meta?.terminals?.input?.length ?? 1;
  const outCount = meta?.terminals?.output?.length ?? 1;
  const maxPins = Math.max(inCount, outCount, 1);
  const heightSpan = (maxPins - 1) * 1.0;
  const viewBox = isTransformer
    ? `-${2.2 * u} -${2.2 * u} ${4.4 * u} ${4.4 * u}`
    : maxPins > 1
      ? `-${2.4 * u} -${1.8 * u} ${4.8 * u} ${(3.6 + heightSpan) * u}`
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
        <SymbolByType
          type={type}
          u={u}
          family={family}
          inputCount={inCount}
          outputCount={outCount}
          inLabels={meta?.terminals?.input?.map((t) => t.label)}
          outLabels={meta?.terminals?.output?.map((t) => t.label)}
        />
      </g>
    </svg>
  );
}

/**
 * Clean SVG symbol preview for the Note / Textbox component in palette and dialogs.
 */
export function TextboxPreviewSymbol({ u }: { u: number }) {
  const w = 2.4 * u;
  const h = 2.0 * u;
  return (
    <g className="symbol-textbox">
      <rect
        x={-w / 2}
        y={-h / 2}
        width={w}
        height={h}
        rx={3}
        fill="rgba(30, 41, 59, 0.95)"
        stroke="#38bdf8"
        strokeWidth={1.5}
      />
      <rect
        x={-w / 2}
        y={-h / 2}
        width={w}
        height={0.55 * u}
        rx={2}
        fill="rgba(56, 189, 248, 0.25)"
      />
      <circle cx={-w / 2 + 0.3 * u} cy={-h / 2 + 0.28 * u} r={0.12 * u} fill="#38bdf8" />
      <line
        x1={-w / 2 + 0.3 * u}
        y1={-h / 2 + 0.95 * u}
        x2={w / 2 - 0.3 * u}
        y2={-h / 2 + 0.95 * u}
        stroke="#94a3b8"
        strokeWidth={1.2}
        strokeLinecap="round"
      />
      <line
        x1={-w / 2 + 0.3 * u}
        y1={-h / 2 + 1.35 * u}
        x2={w / 2 - 0.5 * u}
        y2={-h / 2 + 1.35 * u}
        stroke="#64748b"
        strokeWidth={1.2}
        strokeLinecap="round"
      />
      <line
        x1={-w / 2 + 0.3 * u}
        y1={-h / 2 + 1.7 * u}
        x2={w / 2 - 0.8 * u}
        y2={-h / 2 + 1.7 * u}
        stroke="#38bdf8"
        strokeWidth={1.2}
        strokeLinecap="round"
      />
    </g>
  );
}

export function SymbolByType({
  type,
  u,
  family = 'LK',
  inputCount = 1,
  outputCount = 1,
  inLabels,
  outLabels,
}: {
  type: number;
  u: number;
  family?: string;
  inputCount?: number;
  outputCount?: number;
  inLabels?: string[];
  outLabels?: string[];
}) {
  if (type === 2001 || family === 'SPECIAL' || family === 'ANNOTATION') {
    return <TextboxPreviewSymbol u={u} />;
  }

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
      case CTRL_TYPE.GATE:
      case 1000:
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
        return <TwoInputControlSymbol u={u} label="≥" in0Label="a" in1Label="b" />;
      case 46:
      case 1010:
        return <ComparatorSymbol u={u} />;
      case 49:
      case 1028:
        return <TwoInputControlSymbol u={u} label="min" in0Label="a" in1Label="b" />;
      case 50:
      case 1029:
        return <TwoInputControlSymbol u={u} label="max" in0Label="a" in1Label="b" />;
      case 58:
      case 1034:
        return <TimeSourceSymbol u={u} />;
      case 64:
      case 1009:
        return <TwoInputControlSymbol u={u} label="∫" in0Label="in" in1Label="rst" />;
      case 84:
      case 1014:
        return <MuxSymbol u={u} />;
      case 1037:
        return <DeadTimeSymbol u={u} />;
      case 35:
      case 1038:
        return <ControlBlockLabel u={u} label="asin" />;
      case 37:
      case 1039:
        return <ControlBlockLabel u={u} label="acos" />;
      case 38:
      case 1040:
        return <ControlBlockLabel u={u} label="tan" />;
      case 39:
      case 1041:
        return <ControlBlockLabel u={u} label="atan" />;
      case 42:
      case 1042:
        return <ControlBlockLabel u={u} label="x²" />;
      case 44:
      case 1043:
        return <TwoInputControlSymbol u={u} label="xʸ" in0Label="x" in1Label="y" />;
      case 33:
      case 1044:
        return <ControlBlockLabel u={u} label="rnd" />;
      case 51:
      case 1045:
        return <ControlBlockLabel u={u} label="sgn" />;
      case 47:
      case 1046:
        return <TwoInputControlSymbol u={u} label="==" in0Label="a" in1Label="b" />;
      case 48:
      case 1047:
        return <TwoInputControlSymbol u={u} label="!=" in0Label="a" in1Label="b" />;
      case 53:
      case 1048:
        return <TwoInputControlSymbol u={u} label="CNT" in0Label="clk" in1Label="rst" />;
      case 65:
      case 1049:
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label="abc→dq" inLabels={inLabels} outLabels={outLabels} />;
      case 66:
      case 1050:
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label="dq→abc" inLabels={inLabels} outLabels={outLabels} />;
      case 72:
      case 1051:
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label="ThyrCtrl" inLabels={inLabels} outLabels={outLabels} />;
      case 76:
      case 1052:
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label="FOC" inLabels={inLabels} outLabels={outLabels} />;
      case 77:
      case 1053:
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label="PWM" inLabels={inLabels} outLabels={outLabels} />;
      case 85:
      case 1054:
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label="DEMUX" inLabels={inLabels} outLabels={outLabels} />;
      case 63:
      case 1055:
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label="SVPWM" inLabels={inLabels} outLabels={outLabels} />;
      case 82:
      case 1056:
        return <ControlBlockLabel u={u} label="SDFT" />;
      case 59:
      case 1058:
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label="SPM" inLabels={inLabels} outLabels={outLabels} />;
      case CTRL_TYPE.LEGACY_JAVA_FUNCTION:
      case CTRL_TYPE.SCRIPT: {
        const label = type === CTRL_TYPE.LEGACY_JAVA_FUNCTION ? 'JAVA' : 'f(x)';
        return <ScriptFunctionBlockSymbol u={u} inCount={inputCount} outCount={outputCount} label={label} inLabels={inLabels} outLabels={outLabels} />;
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
        x={-0.35 * u}
        y={0.18 * u}
        fontSize={0.52 * u}
        fill="currentColor"
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        +
      </text>
      <text
        x={0.35 * u}
        y={0.18 * u}
        fontSize={0.52 * u}
        fill="currentColor"
        stroke="none"
        textAnchor="middle"
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
      {/* Base / Gate terminal lead */}
      <line x1={-LEAD * u} y1={0} x2={isBjt ? -0.2 * u : -0.35 * u} y2={0} />
      {/* Collector / Drain terminal lead */}
      <line x1={0.4 * u} y1={-0.5 * u} x2={LEAD * u} y2={-0.5 * u} />
      {/* Emitter / Source terminal lead */}
      <line x1={0.4 * u} y1={0.5 * u} x2={LEAD * u} y2={0.5 * u} />
      {/* Insulated Gate bar for MOSFET / IGBT */}
      {!isBjt && (
        <line x1={-0.35 * u} y1={-0.65 * u} x2={-0.35 * u} y2={0.65 * u} strokeWidth={2} />
      )}
      {/* Channel / Base bar */}
      {isMosfet ? (
        <>
          <line x1={-0.15 * u} y1={-0.6 * u} x2={-0.15 * u} y2={-0.3 * u} strokeWidth={1.5} />
          <line x1={-0.15 * u} y1={-0.15 * u} x2={-0.15 * u} y2={0.15 * u} strokeWidth={1.5} />
          <line x1={-0.15 * u} y1={0.3 * u} x2={-0.15 * u} y2={0.6 * u} strokeWidth={1.5} />
          <line x1={-0.15 * u} y1={-0.45 * u} x2={0.4 * u} y2={-0.5 * u} />
          <line x1={-0.15 * u} y1={0.45 * u} x2={0.4 * u} y2={0.5 * u} />
          <path d={`M ${0.1 * u} ${0.45 * u} l ${-0.2 * u} ${-0.12 * u} l 0 ${0.24 * u} z`} fill="currentColor" />
        </>
      ) : (
        <>
          <line x1={-0.2 * u} y1={-0.6 * u} x2={-0.2 * u} y2={0.6 * u} strokeWidth={isIgbt ? 2 : 2.2} />
          <line x1={-0.2 * u} y1={-0.3 * u} x2={0.4 * u} y2={-0.5 * u} />
          <line x1={-0.2 * u} y1={0.3 * u} x2={0.4 * u} y2={0.5 * u} />
          {/* Emitter arrow pointing outward (N-type) */}
          <path d={`M ${0.35 * u} ${0.48 * u} l ${-0.18 * u} ${-0.08 * u} l ${0.08 * u} ${-0.18 * u} z`} fill="currentColor" />
        </>
      )}
      {/* Fitted device family badge placed cleanly in open area between diagonal lines */}
      <text
        x={0.12 * u}
        y={-0.05 * u}
        fontSize={0.28 * u}
        fill="currentColor"
        stroke="none"
        fontWeight="bold"
        textAnchor="middle"
      >
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
      <text
        x={0}
        y={0.26 * u}
        fontSize={0.72 * u}
        fill="currentColor"
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
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
      <text x={-0.48 * u} y={-0.22 * u} fontSize={0.45 * u} fill="currentColor" stroke="none" fontWeight="bold">
        −
      </text>
      <text x={-0.48 * u} y={0.52 * u} fontSize={0.45 * u} fill="currentColor" stroke="none" fontWeight="bold">
        +
      </text>
    </g>
  );
}

function ThermalResistor({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <rect x={-0.85 * u} y={-0.45 * u} width={1.7 * u} height={0.9 * u} rx={1} strokeDasharray="3 2" />
      <text x={0} y={0.14 * u} fontSize={0.38 * u} fill="currentColor" stroke="none" textAnchor="middle" fontWeight="bold">
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
      <text x={0} y={-0.75 * u} fontSize={0.38 * u} fill="currentColor" stroke="none" textAnchor="middle" fontWeight="bold">
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
      <text x={0} y={0.22 * u} fontSize={0.65 * u} fill="currentColor" stroke="none" textAnchor="middle" fontWeight="bold">
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
      <text x={0} y={-0.35 * u} fontSize={0.34 * u} fill="currentColor" stroke="none" textAnchor="middle" fontWeight="bold">
        Pth
      </text>
    </g>
  );
}

function AmbientSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={0} y2={0} />
      <circle cx={0} cy={0} r={0.65 * u} />
      <text x={0} y={0.12 * u} fontSize={0.32 * u} fill="currentColor" stroke="none" textAnchor="middle" fontWeight="bold">
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
  const label = type === 26 ? 'MMF' : type === 25 ? 'L_rel' : 'Rm';
  return (
    <g>
      {leads(u)}
      <rect x={-0.8 * u} y={-0.5 * u} width={1.6 * u} height={1.0 * u} />
      <line x1={-0.8 * u} y1={-0.5 * u} x2={0.8 * u} y2={0.5 * u} />
      <text x={0} y={0.82 * u} fontSize={0.36 * u} fill="currentColor" stroke="none" textAnchor="middle" fontWeight="bold">
        {label}
      </text>
    </g>
  );
}

function LISNSymbol({ u }: { u: number }) {
  return (
    <g>
      {leads(u)}
      <rect x={-0.85 * u} y={-0.5 * u} width={1.7 * u} height={1.0 * u} rx={2} />
      <text x={0} y={0.14 * u} fontSize={0.38 * u} fill="currentColor" stroke="none" textAnchor="middle" fontWeight="bold">
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
  const label = family ? `${family}${type}` : `T${type}`;
  const boxW = Math.max(1.5 * u, (label.length * 0.24 + 0.5) * u);
  const boxH = 1.5 * u;
  const fs = Math.min(0.42 * u, (boxW - 0.2 * u) / (label.length * 0.65));

  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-boxW / 2} y2={0} />
      <line x1={boxW / 2} y1={0} x2={LEAD * u} y2={0} />
      <rect x={-boxW / 2} y={-boxH / 2} width={boxW} height={boxH} rx={2} />
      <text
        x={0}
        y={fs * 0.35}
        fontSize={fs}
        fill="currentColor"
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        {label}
      </text>
    </g>
  );
}

// ========== CONTROL-domain symbols (green #4ade80) ==========

const CTRL_COLOR = '#4ade80';

function controlLeads(u: number, boxW: number = 1.5 * u) {
  return (
    <>
      <line x1={-LEAD * u} y1={0} x2={-boxW / 2} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={boxW / 2} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
    </>
  );
}

function VoltmeterSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <circle
        cx={0}
        cy={0}
        r={0.65 * u}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.08)"
      />
      <text
        x={0}
        y={0.24 * u}
        fontSize={0.68 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        V
      </text>
    </g>
  );
}

function AmmeterSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <circle
        cx={0}
        cy={0}
        r={0.65 * u}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.08)"
      />
      <text
        x={0}
        y={0.24 * u}
        fontSize={0.68 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        A
      </text>
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
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={-0.65 * u}
        y={-0.55 * u}
        width={1.3 * u}
        height={1.1 * u}
        rx={3}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={0}
        y={0.22 * u}
        fontSize={0.6 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        k
      </text>
    </g>
  );
}

function GateSymbol({ u }: { u: number }) {
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-0.6 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <path
        d={`M ${-0.6 * u} ${-0.6 * u} L ${-0.6 * u} ${0.6 * u} L ${0.6 * u} 0 z`}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.1)"
      />
      <text
        x={-0.15 * u}
        y={0.22 * u}
        fontSize={0.5 * u}
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
      <line x1={-LEAD * u} y1={0} x2={-0.65 * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={0.65 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <path
        d={`M ${-0.65 * u} ${-0.65 * u} L ${-0.65 * u} ${0.65 * u} L ${0.65 * u} 0 z`}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={-0.2 * u}
        y={0.2 * u}
        fontSize={0.48 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        k
      </text>
    </g>
  );
}

function ControlBlockLabel({ u, label }: { u: number; label: string }) {
  const boxW = Math.max(1.6 * u, (label.length * 0.35 + 0.5) * u);
  const boxH = 1.1 * u;
  const fontSize = Math.min(0.52 * u, (boxW - 0.25 * u) / (label.length * 0.62));
  return (
    <g>
      {controlLeads(u, boxW)}
      <rect
        x={-boxW / 2}
        y={-boxH / 2}
        width={boxW}
        height={boxH}
        rx={3}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={0}
        y={fontSize * 0.36}
        fontSize={fontSize}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        {label}
      </text>
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
  const maxPinLen = Math.max(in0Label?.length || 0, in1Label?.length || 0);
  const labelLen = label.length;
  const hasPins = maxPinLen > 0;
  const boxW = hasPins
    ? Math.max(1.8 * u, (maxPinLen * 0.26 + labelLen * 0.36 + 0.8) * u)
    : Math.max(1.5 * u, (labelLen * 0.38 + 0.6) * u);
  const boxH = 2.1 * u;
  const pinFs = maxPinLen > 2 ? 0.28 * u : 0.34 * u;
  const labelFs = hasPins
    ? Math.min(0.52 * u, (boxW / 2 - 0.08 * u) / (labelLen * 0.55))
    : Math.min(0.6 * u, (boxW - 0.3 * u) / (labelLen * 0.55));
  const labelX = hasPins ? boxW * 0.15 : 0;

  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-boxW / 2} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-boxW / 2} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={boxW / 2} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={-boxW / 2}
        y={-0.55 * u}
        width={boxW}
        height={boxH}
        rx={3}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={labelX}
        y={0.5 * u + labelFs * 0.35}
        fontSize={labelFs}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        {label}
      </text>
      {in0Label && (
        <text
          x={-boxW / 2 + 0.16 * u}
          y={0.12 * u}
          fontSize={pinFs}
          fill={CTRL_COLOR}
          stroke="none"
          fontWeight="bold"
          textAnchor="start"
        >
          {in0Label}
        </text>
      )}
      {in1Label && (
        <text
          x={-boxW / 2 + 0.16 * u}
          y={1.12 * u}
          fontSize={pinFs}
          fill={CTRL_COLOR}
          stroke="none"
          fontWeight="bold"
          textAnchor="start"
        >
          {in1Label}
        </text>
      )}
    </g>
  );
}

function DeadTimeSymbol({ u }: { u: number }) {
  const boxW = 1.6 * u;
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-boxW / 2} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={boxW / 2} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={boxW / 2} y1={1 * u} x2={LEAD * u} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={-boxW / 2}
        y={-0.55 * u}
        width={boxW}
        height={2.1 * u}
        rx={3}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={-0.18 * u}
        y={0.65 * u}
        fontSize={0.46 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        DT
      </text>
      <text
        x={boxW / 2 - 0.14 * u}
        y={0.22 * u}
        fontSize={0.32 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="end"
        fontWeight="bold"
      >
        H
      </text>
      <text
        x={boxW / 2 - 0.14 * u}
        y={1.22 * u}
        fontSize={0.32 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="end"
        fontWeight="bold"
      >
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
      <text
        x={0}
        y={0.24 * u}
        fontSize={0.65 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        t
      </text>
    </g>
  );
}

/** IEC 60617-12 / DIN EN 60617-12 European Standard XOR Gate (=1) */
function XorGateSymbol({ u }: { u: number }) {
  const boxW = 1.3 * u;
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-boxW / 2} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-boxW / 2} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={boxW / 2} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={-boxW / 2}
        y={-0.55 * u}
        width={boxW}
        height={2.1 * u}
        rx={2}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={0}
        y={0.68 * u}
        fontSize={0.52 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        =1
      </text>
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

/** IEC 60617-12 / DIN EN 60617-12 European Standard AND Gate (&) */
function AndGateSymbol({ u }: { u: number }) {
  const boxW = 1.3 * u;
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-boxW / 2} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-boxW / 2} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={boxW / 2} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={-boxW / 2}
        y={-0.55 * u}
        width={boxW}
        height={2.1 * u}
        rx={2}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={0}
        y={0.68 * u}
        fontSize={0.55 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        &amp;
      </text>
    </g>
  );
}

/** IEC 60617-12 / DIN EN 60617-12 European Standard OR Gate (≥1) */
function OrGateSymbol({ u }: { u: number }) {
  const boxW = 1.3 * u;
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={-boxW / 2} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={-boxW / 2} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={boxW / 2} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={-boxW / 2}
        y={-0.55 * u}
        width={boxW}
        height={2.1 * u}
        rx={2}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={0}
        y={0.68 * u}
        fontSize={0.52 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        ≥1
      </text>
    </g>
  );
}

/** IEC 60617-12 / DIN EN 60617-12 European Standard NOT Gate (1 with inversion bubble) */
function NotGateSymbol({ u }: { u: number }) {
  const boxW = 1.1 * u;
  const leftX = -boxW / 2;
  const rightX = boxW / 2;
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={leftX} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <rect
        x={leftX}
        y={-0.55 * u}
        width={boxW}
        height={1.1 * u}
        rx={2}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      <text
        x={-0.08 * u}
        y={0.2 * u}
        fontSize={0.52 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        1
      </text>
      {/* Inversion circle at output */}
      <circle
        cx={rightX + 0.1 * u}
        cy={0}
        r={0.1 * u}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(15,23,42,0.9)"
      />
      <line x1={rightX + 0.2 * u} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
    </g>
  );
}

function MuxSymbol({ u }: { u: number }) {
  const boxW = 1.6 * u;
  const leftX = -boxW / 2;
  const rightX = boxW / 2;
  return (
    <g>
      <line x1={-LEAD * u} y1={0} x2={leftX} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={1 * u} x2={leftX} y2={1 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={-LEAD * u} y1={2 * u} x2={leftX} y2={2 * u} stroke={CTRL_COLOR} strokeWidth={1.5} />
      <line x1={rightX} y1={0} x2={LEAD * u} y2={0} stroke={CTRL_COLOR} strokeWidth={1.5} />
      {/* Multiplexer trapezoid / function block */}
      <path
        d={`M ${leftX} ${-0.55 * u} L ${rightX} ${-0.55 * u} L ${rightX} ${0.65 * u} L ${leftX} ${2.55 * u} z`}
        stroke={CTRL_COLOR}
        strokeWidth={1.5}
        fill="rgba(74,222,128,0.06)"
      />
      {/* Pin function labels */}
      <text
        x={leftX + 0.16 * u}
        y={0.12 * u}
        fontSize={0.28 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="start"
        fontWeight="bold"
      >
        sel
      </text>
      <text
        x={leftX + 0.16 * u}
        y={1.12 * u}
        fontSize={0.28 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="start"
        fontWeight="bold"
      >
        0
      </text>
      <text
        x={leftX + 0.16 * u}
        y={2.12 * u}
        fontSize={0.28 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="start"
        fontWeight="bold"
      >
        1
      </text>
      <text
        x={rightX - 0.16 * u}
        y={0.12 * u}
        fontSize={0.28 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="end"
        fontWeight="bold"
      >
        y
      </text>
      <text
        x={-0.12 * u}
        y={1.05 * u}
        fontSize={0.34 * u}
        fill={CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
        fontWeight="bold"
      >
        MUX
      </text>
    </g>
  );
}

function ScriptFunctionBlockSymbol({
  u,
  inCount = 1,
  outCount = 1,
  label = 'f(x)',
  inLabels,
  outLabels,
}: {
  u: number;
  inCount?: number;
  outCount?: number;
  label?: string;
  inLabels?: string[];
  outLabels?: string[];
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
  const midY = (top + bottom) / 2;

  // Pin label lengths
  const maxInLabelLen = Math.max(0, ...(inLabels?.map((l) => l.length) || (inCount > 1 ? [2] : [0])));
  const maxOutLabelLen = Math.max(0, ...(outLabels?.map((l) => l.length) || (outCount > 1 ? [2] : [0])));
  const labelLen = label.length;

  // Box width: ensure it fits within terminal distance (2.0u * 2 = 4.0u)
  // while giving sufficient space for pin labels and the central label badge
  const pinSpace = (maxInLabelLen + maxOutLabelLen) * 0.28 * u;
  const labelSpace = (labelLen * 0.35 + 0.6) * u;
  const boxW = Math.min(3.0 * u, Math.max(2.4 * u, (pinSpace + labelSpace) * 0.85, (labelLen * 0.36 + 0.8) * u));

  // Central label font size scaled dynamically so it never overflows
  const maxLabelW = boxW - 0.4 * u;
  const labelFontSize = Math.min(
    maxPins > 4 ? 0.65 * u : 0.58 * u,
    maxLabelW / (labelLen * 0.65)
  );

  return (
    <g className="symbol-control-script">
      {/* Input leads connecting from terminal at -2u to box chassis at -boxW/2 */}
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

      {/* Output leads connecting from box chassis at boxW/2 to terminal at +2u */}
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

      {/* Central Badge label e.g. f(x), JAVA, FOC, PWM, SPM, ThyrCtrl */}
      <text
        x={0}
        y={midY + labelFontSize * 0.35}
        fontSize={labelFontSize}
        fontFamily="monospace"
        fontWeight="bold"
        fontStyle={label === 'f(x)' ? 'italic' : 'normal'}
        fill={maxPins > 3 ? 'rgba(74,222,128,0.35)' : CTRL_COLOR}
        stroke="none"
        textAnchor="middle"
      >
        {label}
      </text>

      {/* Pin function labels displayed right next to terminals inside the chassis */}
      {Array.from({ length: inCount }).map((_, i) => {
        const yOffset = i * step * u;
        const pinName = inLabels?.[i] || (inCount > 1 ? `u${i + 1}` : '');
        if (!pinName) return null;
        return (
          <text
            key={`in-label-${i}`}
            x={-boxW / 2 + 0.18 * u}
            y={yOffset + 0.12 * u}
            fontSize={pinName.length > 3 ? 0.28 * u : 0.34 * u}
            fontFamily="monospace"
            fill="rgba(74,222,128,0.95)"
            stroke="none"
            textAnchor="start"
            fontWeight="bold"
          >
            {pinName}
          </text>
        );
      })}

      {Array.from({ length: outCount }).map((_, j) => {
        const yOffset = j * step * u;
        const pinName = outLabels?.[j] || (outCount > 1 ? `y${j + 1}` : '');
        if (!pinName) return null;
        return (
          <text
            key={`out-label-${j}`}
            x={boxW / 2 - 0.18 * u}
            y={yOffset + 0.12 * u}
            fontSize={pinName.length > 3 ? 0.28 * u : 0.34 * u}
            fontFamily="monospace"
            fill="rgba(74,222,128,0.95)"
            stroke="none"
            textAnchor="end"
            fontWeight="bold"
          >
            {pinName}
          </text>
        );
      })}
    </g>
  );
}

