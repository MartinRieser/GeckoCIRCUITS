/**
 * Small inline SVG icon set for toolbars. Icons use `currentColor` so they follow
 * the active theme, and are drawn as recognisable pictograms (instead of ambiguous
 * unicode glyphs). Schematic symbols follow the usual IEC/ANSI-style drawings.
 */
import type { ReactNode } from 'react';

interface IconProps {
  /** Pixel size of the (square) icon. Defaults to 16. */
  size?: number;
}

function Svg({ size = 16, children, wide = false }: IconProps & { children: ReactNode; wide?: boolean }) {
  const w = wide ? Math.round(size * 1.5) : size;
  return (
    <svg
      width={w}
      height={size}
      viewBox={wide ? '0 0 24 16' : '0 0 16 16'}
      fill="none"
      stroke="currentColor"
      strokeWidth={1.4}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      className="gk-icon"
    >
      {children}
    </svg>
  );
}

/* ---------------------------- action icons ---------------------------- */

/** Curved arrow left. */
export const IconUndo = (p: IconProps) => (
  <Svg {...p}>
    <path d="M5.5 3L2.5 6l3 3" />
    <path d="M2.5 6h6a4 4 0 0 1 0 8H6" />
  </Svg>
);

/** Curved arrow right. */
export const IconRedo = (p: IconProps) => (
  <Svg {...p}>
    <path d="M10.5 3l3 3-3 3" />
    <path d="M13.5 6h-6a4 4 0 0 0 0 8H10" />
  </Svg>
);

/** Circular arrow clockwise. */
export const IconRotateCw = (p: IconProps) => (
  <Svg {...p}>
    <path d="M13 8a5 5 0 1 1-1.8-3.8" />
    <path d="M13 2.5v3h-3" />
  </Svg>
);

/** Circular arrow counter-clockwise. */
export const IconRotateCcw = (p: IconProps) => (
  <Svg {...p}>
    <path d="M3 8a5 5 0 1 0 1.8-3.8" />
    <path d="M3 2.5v3h3" />
  </Svg>
);

/** Two overlapping squares: duplicate. */
export const IconCopy = (p: IconProps) => (
  <Svg {...p}>
    <rect x="5.5" y="5.5" width="8" height="8" rx="1" />
    <path d="M10.5 5.5v-3h-8v8h3" />
  </Svg>
);

/** Trash can. */
export const IconTrash = (p: IconProps) => (
  <Svg {...p}>
    <path d="M2.5 4h11M6 4V2.5h4V4M4 4l.7 9.5h6.6L12 4M6.5 7v4M9.5 7v4" />
  </Svg>
);

/* -------------------------- schematic symbols -------------------------- */

/** Ground symbol. */
export const SymGnd = (p: IconProps) => (
  <Svg {...p} wide>
    <path d="M12 1v6M5 7h14M8 10.5h8M10.5 14h3" />
  </Svg>
);

/** Resistor (box). */
export const SymResistor = (p: IconProps) => (
  <Svg {...p} wide>
    <path d="M1 8h4M19 8h4" />
    <rect x="5" y="5" width="14" height="6" />
  </Svg>
);

/** Inductor (coil). */
export const SymInductor = (p: IconProps) => (
  <Svg {...p} wide>
    <path d="M1 10h3a2.5 3.5 0 0 1 5 0a2.5 3.5 0 0 1 5 0a2.5 3.5 0 0 1 5 0h4" />
  </Svg>
);

/** Capacitor (two plates). */
export const SymCapacitor = (p: IconProps) => (
  <Svg {...p} wide>
    <path d="M1 8h9M14 8h9M10 2.5v11M14 2.5v11" />
  </Svg>
);

/** Diode (triangle + bar). */
export const SymDiode = (p: IconProps) => (
  <Svg {...p} wide>
    <path d="M1 8h6M17 8h6M17 3v10" />
    <path d="M7 3v10l10-5z" />
  </Svg>
);

/** MOSFET (gate, channel, drain, source). */
export const SymMosfet = (p: IconProps) => (
  <Svg {...p} wide>
    <path d="M2 8h6M8 3v10M11 3v3M11 6.5v3M11 10v3M11 4h9v-3M11 12h9v3M11 8h5" />
  </Svg>
);

/** Voltage source (circle with + and -). */
export const SymVoltageSource = (p: IconProps) => (
  <Svg {...p} wide>
    <circle cx="12" cy="8" r="5.5" />
    <path d="M1 8h5.5M17.5 8H23M10 6.2h2.4M11.2 5v2.4M12.5 10.5h2.4" />
  </Svg>
);

/** Text note. */
export const SymNote = (p: IconProps) => (
  <Svg {...p} wide>
    <rect x="3" y="1.5" width="18" height="13" rx="1.5" />
    <path d="M6 5.5h12M6 8.5h12M6 11.5h7" />
  </Svg>
);

/** Six-dot drag grip for movable toolbars. */
export const IconGrip = (p: IconProps) => (
  <Svg {...p}>
    <circle cx="6" cy="4" r="0.9" fill="currentColor" />
    <circle cx="10" cy="4" r="0.9" fill="currentColor" />
    <circle cx="6" cy="8" r="0.9" fill="currentColor" />
    <circle cx="10" cy="8" r="0.9" fill="currentColor" />
    <circle cx="6" cy="12" r="0.9" fill="currentColor" />
    <circle cx="10" cy="12" r="0.9" fill="currentColor" />
  </Svg>
);
