/**
 * Clean, deduplicated Oscilloscope & Graph signal catalog builder.
 * Filters Ground references, duplicate bare net aliases (`out` alongside `V(out)`),
 * and speculative unrecorded signals (`Ploss_*`, `Tj_*`, `Phi_*`) unless recorded or requested.
 */
import type { EditorComponent } from '../model/types';
import {
  extractAvailableSignalDetails,
  type AvailableSignalMetadata,
} from '../model/componentSchema';
import { isGroundSignal } from './chartData';
import { inferSignalUnit } from './ScopeViewTab';

export interface SignalCatalogOptions {
  components?: EditorComponent[];
  wires?: Array<{ label?: string; type?: string; domain?: string }>;
  results?: Record<string, number[]> | null;
  extraSignalNames?: string[];
  assignedChannels?: string[];
  hideDuplicatesAndSpeculative?: boolean;
}

function isSpeculativeDomainSignal(name: string): boolean {
  return name.startsWith('Ploss_') || name.startsWith('Tj_') || name.startsWith('Phi_');
}

function classifyFallbackSignal(
  sig: string,
  components?: EditorComponent[],
): AvailableSignalMetadata {
  const unit = inferSignalUnit(sig);
  let domain: AvailableSignalMetadata['domain'] = 'electrical';
  let category: AvailableSignalMetadata['category'] = 'other';
  let group: AvailableSignalMetadata['group'] = 'control';
  let compName: string | undefined;

  if (sig.startsWith('I(') && sig.endsWith(')')) {
    compName = sig.slice(2, -1);
    domain = 'electrical';
    category = 'current';
    group = 'power';
  } else if (sig.startsWith('V(') && sig.endsWith(')')) {
    const interior = sig.slice(2, -1);
    domain = 'electrical';
    category = 'voltage';
    if (components && components.some((c) => c.name === interior)) {
      compName = interior;
      group = 'power';
    } else {
      group = 'nets';
    }
  } else if (sig.startsWith('Ploss_')) {
    compName = sig.slice('Ploss_'.length);
    domain = 'other';
    category = 'other';
    group = 'power';
  } else if (sig.startsWith('Tj_')) {
    compName = sig.slice('Tj_'.length);
    domain = 'thermal';
    category = 'other';
    group = 'thermal_magnetic';
  } else if (sig.startsWith('Phi_')) {
    compName = sig.slice('Phi_'.length);
    domain = 'electrical';
    category = 'other';
    group = 'thermal_magnetic';
  } else if (unit === 'V' || sig.toLowerCase().startsWith('v_') || sig.toLowerCase().startsWith('u_')) {
    domain = 'electrical';
    category = 'voltage';
    group = 'nets';
  } else if (unit === 'A' || sig.toLowerCase().startsWith('i_')) {
    domain = 'electrical';
    category = 'current';
    group = 'power';
  }

  return {
    name: sig,
    domain,
    category,
    sourceDescription: compName ? `Signal for ${compName}` : `Signal "${sig}"`,
    componentName: compName,
    group,
  };
}

/**
 * Builds the cleaned signal catalog used by ScopeChannelManager and ScopePropertiesPanel.
 */
export function buildCleanSignalCatalog({
  components,
  wires,
  results,
  extraSignalNames = [],
  assignedChannels = [],
  hideDuplicatesAndSpeculative = true,
}: SignalCatalogOptions): AvailableSignalMetadata[] {
  const extracted = components ? extractAvailableSignalDetails(components, wires) : [];
  const metaMap = new Map<string, AvailableSignalMetadata>();

  for (const item of extracted) {
    if (!isGroundSignal(item.name)) {
      metaMap.set(item.name, item);
    }
  }

  const assignedSet = new Set(assignedChannels);

  const extraPool = new Set<string>(extraSignalNames);
  if (results) {
    for (const k of Object.keys(results)) {
      if (k !== 'time') extraPool.add(k);
    }
  }
  for (const ch of assignedChannels) {
    extraPool.add(ch);
  }

  for (const sig of extraPool) {
    if (!sig || isGroundSignal(sig) || metaMap.has(sig)) continue;
    metaMap.set(sig, classifyFallbackSignal(sig, components));
  }

  const allItems = Array.from(metaMap.values());
  if (!hideDuplicatesAndSpeculative) {
    return allItems.sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true }));
  }

  // Collect canonical V(net) names so bare duplicate net aliases (`out`) can be hidden cleanly
  const canonicalNetVoltages = new Set<string>();
  for (const item of allItems) {
    const m = /^V\(([^|)]+)\)$/.exec(item.name);
    if (m && item.group === 'nets') {
      canonicalNetVoltages.add(m[1].trim());
    }
  }

  return allItems
    .filter((item) => {
      if (assignedSet.has(item.name)) return true;

      // 1. Hide bare net alias `out` when `V(out)` exists in the catalog
      if (item.group === 'nets' && canonicalNetVoltages.has(item.name)) {
        return false;
      }

      // 2. Hide speculative Ploss_*, Tj_*, Phi_* unless actually recorded with non-empty data
      if (isSpeculativeDomainSignal(item.name)) {
        const recorded = results?.[item.name];
        return Array.isArray(recorded) && recorded.length > 0;
      }

      return true;
    })
    .sort((a, b) => a.name.localeCompare(b.name, undefined, { numeric: true }));
}
