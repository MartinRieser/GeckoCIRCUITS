/**
 * Pure computation and collision-free layout for live on-canvas operating-point
 * badges rendered on schematic components and labeled nets after simulation.
 */
import type { EditorComponent, EditorWire, Point } from '../model/types';
import {
  formatEngineeringValue,
  getCoupledComponentName,
  isAmmeterComponent,
  isTextboxComponent,
  isVoltmeterComponent,
} from '../model/componentSchema';
import { isGroundSignal, mapSimulationResults, type SignalStats } from '../simulation/chartData';

export type OpPointMetric = 'mean' | 'rms' | 'pkpk' | 'max';

export interface OperatingPointBadge {
  /** Unique identifier (`comp:<name>` or `net:<name>`). */
  id: string;
  /** Short target label (component name or net name) shown at top of badge. */
  targetLabel?: string;
  anchorX: number;
  anchorY: number;
  kind: 'voltage' | 'current' | 'power' | 'thermal' | 'other';
  /** Up to 3 compact readout lines, e.g. `['V: 12 V', 'I: 4.2 A']` or `['12 V']`. */
  lines: string[];
  /** Multi-line tooltip breakdown across Mean / RMS / Pk-Pk / Min / Max. */
  tooltip: string;
  /** Backing simulation signal names. */
  signals: string[];
}

export interface PlacedOperatingPointBadge extends OperatingPointBadge {
  /** Top-left badge X coordinate in SVG pixels. */
  px: number;
  /** Top-left badge Y coordinate in SVG pixels. */
  py: number;
  width: number;
  height: number;
}

export interface OccupiedRect {
  x: number;
  y: number;
  w: number;
  h: number;
}

interface BadgeReading {
  sig: string;
  prefix: string;
  valueStr: string;
  kind: OperatingPointBadge['kind'];
  tooltipLine: string;
}

interface TargetAccumulator {
  id: string;
  targetLabel?: string;
  anchorX: number;
  anchorY: number;
  isWireNet: boolean;
  seenSignals: Set<string>;
  readings: BadgeReading[];
}

function formatTooltipBreakdown(sig: string, stats: SignalStats, unit: string): string {
  return `${sig}: Mean ${formatEngineeringValue(stats.mean, unit)}, RMS ${formatEngineeringValue(stats.rms, unit)}, Pk-Pk ${formatEngineeringValue(stats.pkpk, unit)}, Min ${formatEngineeringValue(stats.min, unit)}, Max ${formatEngineeringValue(stats.max, unit)}`;
}

function longestSegmentMidpoint(points: number[][]): Point | null {
  if (points.length < 2) return null;
  let bestDistSq = -1;
  let bestMid: Point = { x: points[0][0], y: points[0][1] };
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[i];
    const p1 = points[i + 1];
    const dx = p1[0] - p0[0];
    const dy = p1[1] - p0[1];
    const dSq = dx * dx + dy * dy;
    if (dSq > bestDistSq) {
      bestDistSq = dSq;
      bestMid = { x: (p0[0] + p1[0]) / 2, y: (p0[1] + p1[1]) / 2 };
    }
  }
  return bestMid;
}

/**
 * Extracts grouped operating-point badges mapped to schematic components and labeled wires.
 */
export function buildOperatingPointBadges(
  components: EditorComponent[],
  wires: EditorWire[],
  results: Record<string, number[]> | null,
  metric: OpPointMetric,
): OperatingPointBadge[] {
  if (!results) return [];
  const { signalNames, signalStats } = mapSimulationResults(results);
  if (signalNames.length === 0) return [];

  const compByName = new Map<string, EditorComponent>();
  const meters: EditorComponent[] = [];
  for (const c of components) {
    if (isTextboxComponent(c) || c.family === 'SPECIAL' || c.family === 'ANNOTATION') {
      continue;
    }
    compByName.set(c.name, c);
    if (isAmmeterComponent(c) || isVoltmeterComponent(c)) {
      meters.push(c);
    }
  }

  const targets = new Map<string, TargetAccumulator>();
  const addReading = (
    id: string,
    targetLabel: string | undefined,
    anchorX: number,
    anchorY: number,
    isWireNet: boolean,
    reading: BadgeReading,
  ) => {
    let bucket = targets.get(id);
    if (!bucket) {
      bucket = {
        id,
        targetLabel,
        anchorX,
        anchorY,
        isWireNet,
        seenSignals: new Set<string>(),
        readings: [],
      };
      targets.set(id, bucket);
    }
    if (bucket.seenSignals.has(reading.sig)) return;
    // Prevent duplicate net readings when both `out` and `V(out)` exist in results
    if (isWireNet && bucket.readings.length >= 1) return;
    if (bucket.readings.some((r) => r.prefix === reading.prefix && r.valueStr === reading.valueStr)) {
      return;
    }
    bucket.seenSignals.add(reading.sig);
    bucket.readings.push(reading);
  };

  for (const sig of signalNames) {
    if (isGroundSignal(sig)) continue;
    const stats = signalStats[sig];
    if (!stats) continue;

    // 1. Direct component match
    const currMatch = /^I\(([^)]+)\)$/.exec(sig);
    if (currMatch) {
      const targetComp = compByName.get(currMatch[1].trim());
      if (targetComp) {
        addReading(
          targetComp.name,
          targetComp.name,
          targetComp.position[0],
          targetComp.position[1],
          false,
          {
            sig,
            prefix: 'I',
            valueStr: formatEngineeringValue(stats[metric], 'A'),
            kind: 'current',
            tooltipLine: formatTooltipBreakdown(sig, stats, 'A'),
          },
        );
        continue;
      }
    }

    const voltCompMatch = /^V\(([^|)]+)\)$/.exec(sig);
    if (voltCompMatch) {
      const targetComp = compByName.get(voltCompMatch[1].trim());
      if (targetComp) {
        addReading(
          targetComp.name,
          targetComp.name,
          targetComp.position[0],
          targetComp.position[1],
          false,
          {
            sig,
            prefix: 'V',
            valueStr: formatEngineeringValue(stats[metric], 'V'),
            kind: 'voltage',
            tooltipLine: formatTooltipBreakdown(sig, stats, 'V'),
          },
        );
        continue;
      }
    }

    if (sig.startsWith('Ploss_')) {
      const compName = sig.slice('Ploss_'.length);
      const targetComp = compByName.get(compName);
      if (targetComp) {
        addReading(
          targetComp.name,
          targetComp.name,
          targetComp.position[0],
          targetComp.position[1],
          false,
          {
            sig,
            prefix: 'P',
            valueStr: formatEngineeringValue(stats[metric], 'W'),
            kind: 'power',
            tooltipLine: formatTooltipBreakdown(sig, stats, 'W'),
          },
        );
        continue;
      }
    }

    if (sig.startsWith('Tj_')) {
      const compName = sig.slice('Tj_'.length);
      const targetComp = compByName.get(compName);
      if (targetComp) {
        addReading(
          targetComp.name,
          targetComp.name,
          targetComp.position[0],
          targetComp.position[1],
          false,
          {
            sig,
            prefix: 'Tj',
            valueStr: formatEngineeringValue(stats[metric], '°C'),
            kind: 'thermal',
            tooltipLine: formatTooltipBreakdown(sig, stats, '°C'),
          },
        );
        continue;
      }
    }

    // 2. Meter coupling match (outputLabels or meter component name)
    const meter = meters.find(
      (m) => m.name === sig || (m.outputLabels && m.outputLabels.some((lbl) => lbl?.trim() === sig)),
    );
    if (meter) {
      const coupledName = getCoupledComponentName(meter);
      const target = (coupledName ? compByName.get(coupledName) : undefined) ?? meter;
      const isAmp = isAmmeterComponent(meter);
      const unit = isAmp ? 'A' : 'V';
      addReading(
        target.name,
        target.name,
        target.position[0],
        target.position[1],
        false,
        {
          sig,
          prefix: isAmp ? 'I' : 'V',
          valueStr: formatEngineeringValue(stats[metric], unit),
          kind: isAmp ? 'current' : 'voltage',
          tooltipLine: formatTooltipBreakdown(sig, stats, unit),
        },
      );
      continue;
    }

    // 3. Labeled wire / net match
    const diffMatch = /^V\(([^|]+)\|([^)]+)\)$/.exec(sig);
    const strippedNet = diffMatch
      ? diffMatch[1].trim()
      : voltCompMatch
      ? voltCompMatch[1].trim()
      : sig.trim();

    const wire = wires.find((w) => w.label?.trim() === strippedNet && w.points.length >= 2);
    if (wire) {
      const mid = longestSegmentMidpoint(wire.points);
      if (mid) {
        const isControlWire = wire.type === 'CONTROL';
        const isThermWire = wire.type === 'THERM';
        const unit = isControlWire ? '' : isThermWire ? '°C' : 'V';
        const prefix = isControlWire ? '' : isThermWire ? 'T' : 'V';
        const kind: OperatingPointBadge['kind'] = isControlWire
          ? 'other'
          : isThermWire
            ? 'thermal'
            : 'voltage';
        addReading(
          `net:${strippedNet}`,
          strippedNet,
          mid.x,
          mid.y,
          true,
          {
            sig,
            prefix,
            valueStr: formatEngineeringValue(stats[metric], unit),
            kind,
            tooltipLine: formatTooltipBreakdown(sig, stats, unit),
          },
        );
      }
    }
  }

  const badges: OperatingPointBadge[] = [];
  for (const bucket of targets.values()) {
    const visibleReadings = bucket.readings.slice(0, 3);
    const lines = visibleReadings.map((r) =>
      bucket.isWireNet && visibleReadings.length === 1 ? r.valueStr : `${r.prefix}: ${r.valueStr}`,
    );
    const primaryKind = bucket.readings[0]?.kind ?? 'other';
    badges.push({
      id: bucket.id,
      targetLabel: bucket.targetLabel,
      anchorX: bucket.anchorX,
      anchorY: bucket.anchorY,
      kind: primaryKind,
      lines,
      tooltip: bucket.readings.map((r) => r.tooltipLine).join('\n'),
      signals: bucket.readings.map((r) => r.sig),
    });
  }
  return badges;
}

function rectsOverlap(a: OccupiedRect, b: OccupiedRect, padding = 4): boolean {
  return (
    a.x < b.x + b.w + padding &&
    a.x + a.w + padding > b.x &&
    a.y < b.y + b.h + padding &&
    a.y + a.h + padding > b.y
  );
}

/**
 * Resolves non-overlapping pixel bounding boxes for operating-point badges,
 * avoiding both existing probe markers and previously placed badges.
 */
export function layoutOperatingPointBadges(
  badges: OperatingPointBadge[],
  dpix: number,
  occupiedRects: OccupiedRect[] = [],
): PlacedOperatingPointBadge[] {
  const taken: OccupiedRect[] = [...occupiedRects];
  const placed: PlacedOperatingPointBadge[] = [];

  for (const badge of badges) {
    const labelLen = badge.targetLabel ? badge.targetLabel.length : 0;
    const maxLen = Math.max(...badge.lines.map((line) => line.length), labelLen, 1);
    const width = Math.max(58, Math.round(maxLen * 6.4 + 16));
    const hasHeader = Boolean(badge.targetLabel);
    const height = badge.lines.length * 13 + (hasHeader ? 12 : 0) + 8;

    const ax = badge.anchorX * dpix;
    const ay = badge.anchorY * dpix;

    const candidates: Array<{ x: number; y: number }> = [
      { x: ax + 24, y: ay + 24 }, // below-right outside symbol/label
      { x: ax + 24, y: ay - height - 22 }, // above-right outside symbol/label
      { x: ax - width - 24, y: ay + 24 }, // below-left
      { x: ax - width - 24, y: ay - height - 22 }, // above-left
      { x: ax - Math.round(width / 2), y: ay + 36 }, // centered below
      { x: ax - Math.round(width / 2), y: ay - height - 32 }, // centered above
    ];
    for (let level = 1; level <= 6; level++) {
      candidates.push({ x: ax + 24, y: ay + 24 + level * (height + 6) });
      candidates.push({ x: ax - width - 24, y: ay + 24 + level * (height + 6) });
    }

    let chosenX = candidates[0].x;
    let chosenY = candidates[0].y;
    for (const cand of candidates) {
      const box: OccupiedRect = { x: cand.x, y: cand.y, w: width, h: height };
      if (!taken.some((occ) => rectsOverlap(box, occ))) {
        chosenX = cand.x;
        chosenY = cand.y;
        break;
      }
    }

    taken.push({ x: chosenX, y: chosenY, w: width, h: height });
    placed.push({
      ...badge,
      px: chosenX,
      py: chosenY,
      width,
      height,
    });
  }

  return placed;
}
