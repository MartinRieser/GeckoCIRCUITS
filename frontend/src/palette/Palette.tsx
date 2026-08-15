/** Component palette fed by the catalog endpoint, filterable by text. */
import { useMemo, useState } from 'react';
import type { CatalogEntry } from '../model/types';

interface PaletteProps {
  catalog: CatalogEntry[];
  onArm: (entry: CatalogEntry) => void;
}

export function Palette({ catalog, onArm }: PaletteProps) {
  const [filter, setFilter] = useState('');

  const grouped = useMemo(() => {
    const needle = filter.trim().toLowerCase();
    const matching = catalog.filter(
      (entry) => !needle || entry.name.toLowerCase().includes(needle),
    );
    const families = new Map<string, CatalogEntry[]>();
    for (const entry of matching) {
      const list = families.get(entry.family) ?? [];
      list.push(entry);
      families.set(entry.family, list);
    }
    return [...families.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [catalog, filter]);

  return (
    <div className="palette">
      <input
        type="search"
        placeholder="Filter components..."
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        autoFocus
      />
      {grouped.map(([family, entries]) => (
        <div key={family}>
          <div className="palette-family">{family}</div>
          {entries.map((entry) => (
            <button
              key={`${entry.family}-${entry.type}`}
              className="palette-entry"
              onClick={() => onArm(entry)}
              title={`Place ${entry.name} (type ${entry.type})`}
            >
              {displayName(entry)}
            </button>
          ))}
        </div>
      ))}
    </div>
  );
}

function displayName(entry: CatalogEntry): string {
  return entry.name.replace(/^LK_/, '').replace(/^TH_/, 'TH ');
}
