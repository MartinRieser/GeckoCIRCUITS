/**
 * Scope-instrument helpers shared by the canvas, properties panels,
 * simulation drawer, and scope tabs.
 */
import type { EditorComponent } from '../model/types';
import { ControlComponentType } from '../model/constants';
import { terminalPositions } from '../model/geometry';
import { isVoltmeterComponent, isAmmeterComponent } from '../model/componentSchema';

/**
 * Checks whether a given circuit component is an oscilloscope instrument block,
 * either by CONTROL-domain type (legacy typ 5 or modern typ 1003) or by
 * conventional prefix ('SCOPE', 'OSZI'). The type check requires the CONTROL
 * family: the legacy scope shares typ 5 with the LK current source, which must
 * not be mistaken for an instrument.
 *
 * @param component The schematic component to test, or null/undefined.
 * @returns True if the component represents an oscilloscope instrument.
 */
export function isScopeComponent(component: EditorComponent | null | undefined): boolean {
  if (!component) return false;
  const name = (component.name || '').toUpperCase();
  if (name.startsWith('SCOPE') || name.startsWith('OSZI')) {
    return true;
  }
  return (
    component.family === 'CONTROL' &&
    (component.type === ControlComponentType.LEGACY_SCOPE ||
      component.type === ControlComponentType.SCOPE)
  );
}

/**
 * Filters a list of circuit components down to only the oscilloscope instrument blocks.
 *
 * @param components Array of schematic components.
 * @returns Array of oscilloscope instrument components.
 */
export function findScopeBlocks(components: EditorComponent[]): EditorComponent[] {
  return components.filter(isScopeComponent);
}


/**
 * Resolves signals physically connected to a scope's input terminals by tracing wires.
 * If a pin has an explicit input label, that label is returned. If a pin is unlabeled
 * but a wire physically connects it to an output pin of another component (e.g. voltmeter,
 * ammeter, control block) or a labeled wire, the source signal name is auto-resolved.
 *
 * @param scopeBlock The oscilloscope component.
 * @param allComponents Optional array of all circuit components.
 * @param wires Optional array of circuit wires.
 * @returns Array of resolved signal names.
 */
export function resolveScopeWiredSignals(
  scopeBlock: EditorComponent | null | undefined,
  allComponents: EditorComponent[] = [],
  wires: { points: number[][]; label?: string }[] = [],
): string[] {
  if (!scopeBlock) return [];
  const terms = terminalPositions(scopeBlock);
  const wiredSignals: string[] = [];

  for (let i = 0; i < terms.input.length; i++) {
    const explicitLabel = scopeBlock.inputLabels?.[i]?.trim();
    if (explicitLabel && explicitLabel !== 'NIX_NIX_NIX') {
      wiredSignals.push(explicitLabel);
      continue;
    }

    if (!wires.length) continue;
    const pin = terms.input[i];
    let foundSignal = '';

    for (const wire of wires) {
      if (!wire.points || wire.points.length < 2) continue;
      const start = wire.points[0];
      const end = wire.points[wire.points.length - 1];
      const touchesStart = Math.hypot(start[0] - pin.x, start[1] - pin.y) < 0.25;
      const touchesEnd = Math.hypot(end[0] - pin.x, end[1] - pin.y) < 0.25;

      if (!touchesStart && !touchesEnd) continue;

      if (wire.label && wire.label.trim() && wire.label !== 'NIX_NIX_NIX') {
        foundSignal = wire.label.trim();
        break;
      }

      const otherPt = touchesStart ? end : start;
      for (const comp of allComponents) {
        if (comp.name === scopeBlock.name) continue;
        const compTerms = terminalPositions(comp);
        for (let o = 0; o < compTerms.output.length; o++) {
          const outPin = compTerms.output[o];
          if (Math.hypot(outPin.x - otherPt[0], outPin.y - otherPt[1]) < 0.25) {
            const outLbl = comp.outputLabels?.[o]?.trim();
            if (outLbl && outLbl !== 'NIX_NIX_NIX') {
              foundSignal = outLbl;
            } else if (isVoltmeterComponent(comp) || isAmmeterComponent(comp)) {
              foundSignal = comp.name;
            }
            break;
          }
        }
        if (foundSignal) break;
      }
      if (foundSignal) break;
    }

    if (foundSignal) {
      wiredSignals.push(foundSignal);
    }
  }

  return wiredSignals;
}

/**
 * Channels strictly wired or assigned to one scope: its input labels or connected wire
 * signals that match recorded signals. Unlike {@link scopeChannels} there is no fallback —
 * an unwired, unassigned scope yields an empty list.
 */
export function strictScopeChannels(
  scopeBlock: EditorComponent | null | undefined,
  signalNames: string[],
  allComponents: EditorComponent[] = [],
  wires: { points: number[][]; label?: string }[] = [],
): string[] {
  if (!scopeBlock) {
    return [];
  }
  const wired = resolveScopeWiredSignals(scopeBlock, allComponents, wires);
  const explicit = (scopeBlock.inputLabels || []).filter((l) => l && l.trim() && l !== 'NIX_NIX_NIX');
  const combined = Array.from(new Set([...explicit, ...wired]));
  return combined.filter((l) => signalNames.includes(l));
}

/**
 * Channels of a scope for display: its wired or assigned input labels that match recorded
 * signals, falling back to all recorded signals when nothing matches so an
 * unwired scope still shows the run results.
 */
export function scopeChannels(
  scopeBlock: EditorComponent | null | undefined,
  signalNames: string[],
  allComponents: EditorComponent[] = [],
  wires: { points: number[][]; label?: string }[] = [],
): string[] {
  const channels = strictScopeChannels(scopeBlock, signalNames, allComponents, wires);
  if (channels.length > 0) {
    return channels;
  }
  return signalNames;
}

/** Case-insensitive substring filter; empty or blank query returns all channels. */
export function filterChannels(channels: string[], query: string): string[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return channels;
  }
  return channels.filter((s) => s.toLowerCase().includes(q));
}

/**
 * Checks whether a given circuit component is a script/function block,
 * either by type (modern 1016 or legacy 61) or by conventional prefix ('SCRIPT').
 *
 * @param component The schematic component to test, or null/undefined.
 * @returns True if the component represents a script block.
 */
export function isScriptComponent(component: EditorComponent | null | undefined): boolean {
  if (!component) return false;
  const name = (component.name || '').toUpperCase();
  return (
    component.type === ControlComponentType.SCRIPT ||
    component.type === ControlComponentType.LEGACY_JAVA_FUNCTION ||
    name.startsWith('SCRIPT') ||
    name.startsWith('JAVA_')
  );
}

/**
 * Filters a list of circuit components down to only the script/function blocks.
 *
 * @param components Array of schematic components.
 * @returns Array of script/function block components.
 */
export function findScriptBlocks(components: EditorComponent[]): EditorComponent[] {
  return components.filter(isScriptComponent);
}
