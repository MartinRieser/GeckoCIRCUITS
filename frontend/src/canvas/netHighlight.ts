/**
 * Electrical Net Analysis & Interactive Net Highlighting.
 *
 * Implements Union-Find topological net tracing across the schematic:
 * - Traces contiguous wire paths and junction taps (raster cell sharing).
 * - Traces named nets sharing explicit signal/net labels.
 * - Associates component terminals touching any point of the net.
 * - Enables LTspice-style whole-net luminous highlighting on wire or terminal hover/selection.
 */

import type { EditorComponent, EditorWire, Point } from '../model/types';
import { terminalPositions } from '../model/geometry';
import { denseCellsOf } from './WireRouter';
import { SENTINEL_UNSET } from '../model/constants';

export interface ElectricalNet {
  /** Unique net identifier (net label if available, or synthetic net_1, net_2...). */
  id: string;
  /** Explicit net label if any wire or terminal carries one. */
  label?: string;
  /** Set of wire indices belonging to this net. */
  wireIndices: Set<number>;
  /** Set of component terminal grid keys ("x,y") connected to this net. */
  terminalKeys: Set<string>;
  /** Set of all grid raster cell keys ("x,y") covered by wires and terminals in this net. */
  pointKeys: Set<string>;
}

export interface NetAnalysisResult {
  nets: ElectricalNet[];
  wireIndexToNet: Map<number, ElectricalNet>;
  terminalKeyToNet: Map<string, ElectricalNet>;
}

/**
 * Partitions all wires and component terminals into electrical nets using Disjoint Set Union.
 */
export function analyzeElectricalNets(
  wires: EditorWire[],
  components: EditorComponent[],
): NetAnalysisResult {
  const parent = new Map<number, number>();

  const find = (i: number): number => {
    let root = i;
    while (parent.get(root) !== undefined && parent.get(root) !== root) {
      root = parent.get(root)!;
    }
    let curr = i;
    while (curr !== root) {
      const next = parent.get(curr) ?? curr;
      parent.set(curr, root);
      curr = next;
    }
    return root;
  };

  const union = (i: number, j: number) => {
    const rootI = find(i);
    const rootJ = find(j);
    if (rootI !== rootJ) {
      parent.set(rootI, rootJ);
    }
  };

  wires.forEach((w) => {
    parent.set(w.index, w.index);
  });

  // 1. Group wires sharing any raster cell (touching endpoints or T-junction taps)
  const cellToWires = new Map<string, number[]>();
  for (const wire of wires) {
    if (!wire.points || wire.points.length === 0) continue;
    for (const cell of denseCellsOf(wire.points)) {
      let list = cellToWires.get(cell);
      if (!list) {
        list = [];
        cellToWires.set(cell, list);
      }
      list.push(wire.index);
    }
  }

  for (const list of cellToWires.values()) {
    if (list.length > 1) {
      const first = list[0];
      for (let k = 1; k < list.length; k++) {
        union(first, list[k]);
      }
    }
  }

  // 2. Group wires sharing the same non-empty net label
  const labelToWires = new Map<string, number[]>();
  for (const wire of wires) {
    const label = wire.label?.trim();
    if (label && label !== SENTINEL_UNSET) {
      let list = labelToWires.get(label);
      if (!list) {
        list = [];
        labelToWires.set(label, list);
      }
      list.push(wire.index);
    }
  }
  for (const list of labelToWires.values()) {
    if (list.length > 1) {
      const first = list[0];
      for (let k = 1; k < list.length; k++) {
        union(first, list[k]);
      }
    }
  }

  // Group wires by root
  const groups = new Map<number, EditorWire[]>();
  for (const wire of wires) {
    const root = find(wire.index);
    let group = groups.get(root);
    if (!group) {
      group = [];
      groups.set(root, group);
    }
    group.push(wire);
  }

  const nets: ElectricalNet[] = [];
  const wireIndexToNet = new Map<number, ElectricalNet>();
  const terminalKeyToNet = new Map<string, ElectricalNet>();

  // Collect all component terminals with their positions and labels
  const allTerminals: { comp: EditorComponent; term: Point; label?: string }[] = [];
  for (const comp of components) {
    const terms = terminalPositions(comp);
    terms.input.forEach((t, i) => {
      allTerminals.push({ comp, term: t, label: comp.inputLabels?.[i]?.trim() });
    });
    terms.output.forEach((t, i) => {
      allTerminals.push({ comp, term: t, label: comp.outputLabels?.[i]?.trim() });
    });
  }

  let netCount = 0;
  for (const [, wireGroup] of groups) {
    netCount++;
    const wireIndices = new Set<number>();
    const pointKeys = new Set<string>();
    let netLabel: string | undefined;

    for (const w of wireGroup) {
      wireIndices.add(w.index);
      if (!netLabel && w.label && w.label.trim() !== SENTINEL_UNSET) {
        netLabel = w.label.trim();
      }
      for (const cell of denseCellsOf(w.points)) {
        pointKeys.add(cell);
      }
    }

    const terminalKeys = new Set<string>();
    // Link terminals that land on any point of this net, or share the explicit netLabel
    for (const { term, label } of allTerminals) {
      const key = `${term.x},${term.y}`;
      if (pointKeys.has(key) || (netLabel && label && label === netLabel)) {
        terminalKeys.add(key);
        pointKeys.add(key);
      }
    }

    const net: ElectricalNet = {
      id: netLabel || `net_${netCount}`,
      label: netLabel,
      wireIndices,
      terminalKeys,
      pointKeys,
    };

    nets.push(net);
    for (const idx of wireIndices) {
      wireIndexToNet.set(idx, net);
    }
    for (const tKey of terminalKeys) {
      terminalKeyToNet.set(tKey, net);
    }
  }

  // 3. Handle virtual nets: terminals that share a label but have no direct drawn wires yet
  const labelToTerminals = new Map<string, Point[]>();
  for (const { term, label } of allTerminals) {
    if (label && label !== SENTINEL_UNSET) {
      let list = labelToTerminals.get(label);
      if (!list) {
        list = [];
        labelToTerminals.set(label, list);
      }
      list.push(term);
    }
  }

  for (const [lbl, tList] of labelToTerminals) {
    if (tList.length > 1) {
      const alreadyHasNet = nets.some((n) => n.label === lbl);
      if (!alreadyHasNet) {
        netCount++;
        const terminalKeys = new Set(tList.map((t) => `${t.x},${t.y}`));
        const net: ElectricalNet = {
          id: lbl,
          label: lbl,
          wireIndices: new Set(),
          terminalKeys,
          pointKeys: new Set(terminalKeys),
        };
        nets.push(net);
        for (const tKey of terminalKeys) {
          terminalKeyToNet.set(tKey, net);
        }
      }
    }
  }

  return { nets, wireIndexToNet, terminalKeyToNet };
}
