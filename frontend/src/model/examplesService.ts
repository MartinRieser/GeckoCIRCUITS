/**
 * Central service for managing circuit examples.
 * Supports loading, adding, editing, and deleting examples with synchronization
 * between the backend examples/ directory and browser localStorage cache.
 */
import { EXAMPLES, type CircuitExample } from './examples';
import {
  listExamplesApi,
  createExampleApi,
  updateExampleApi,
  deleteExampleApi,
  type ExampleDto,
} from '../api/client';

const CUSTOM_EXAMPLES_KEY = 'gecko-custom-examples';
const CACHED_EXAMPLES_KEY = 'gecko-cached-examples';

export interface ManagedExample extends CircuitExample {
  custom?: boolean;
}

type ExampleListener = (examples: ManagedExample[]) => void;
const listeners = new Set<ExampleListener>();

function notify(examples: ManagedExample[]) {
  listeners.forEach((listener) => {
    try {
      listener(examples);
    } catch (e) {
      console.warn('Error in examples listener:', e);
    }
  });
}

export function subscribeExamples(listener: ExampleListener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function getLocalCustomExamples(): ManagedExample[] {
  try {
    const raw = localStorage.getItem(CUSTOM_EXAMPLES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveLocalCustomExamples(custom: ManagedExample[]) {
  try {
    localStorage.setItem(CUSTOM_EXAMPLES_KEY, JSON.stringify(custom));
  } catch (e) {
    console.warn('Failed to save custom examples to localStorage:', e);
  }
}

/**
 * Loads all available circuit examples from the backend or localStorage fallback.
 */
export async function loadAllExamples(): Promise<ManagedExample[]> {
  // 1. Try fetching from backend REST API
  try {
    const remote = await listExamplesApi();
    if (Array.isArray(remote) && remote.length > 0) {
      const merged: ManagedExample[] = remote.map((dto: ExampleDto) => ({
        id: dto.id,
        name: dto.name,
        category: dto.category,
        description: dto.description,
        content: dto.content || '',
        custom: dto.custom ?? false,
      }));

      // Also merge any local-only custom examples not in remote
      const local = getLocalCustomExamples();
      for (const loc of local) {
        if (!merged.some((m) => m.id === loc.id)) {
          merged.push({ ...loc, custom: true });
        }
      }

      localStorage.setItem(CACHED_EXAMPLES_KEY, JSON.stringify(merged));
      notify(merged);
      return merged;
    }
  } catch {
    // Backend offline / unavailable, fall through to local cache
  }

  // 2. Fallback to cached examples or built-in defaults + local custom
  const localCustom = getLocalCustomExamples();
  const builtIns: ManagedExample[] = EXAMPLES.map((ex) => ({
    ...ex,
    custom: false,
  }));

  const map = new Map<string, ManagedExample>();
  for (const ex of builtIns) {
    map.set(ex.id, ex);
  }
  for (const ex of localCustom) {
    map.set(ex.id, { ...ex, custom: true });
  }

  const result = Array.from(map.values());
  notify(result);
  return result;
}

/**
 * Adds a new circuit example to disk and local storage.
 */
export async function addExample(example: {
  id?: string;
  name: string;
  category: string;
  description: string;
  content: string;
  overwrite?: boolean;
}): Promise<ManagedExample> {
  const currentList = await loadAllExamples();
  const trimmedName = example.name.trim();
  const existing = currentList.find(
    (e) => (example.id && e.id === example.id) || e.name.toLowerCase() === trimmedName.toLowerCase(),
  );

  const cleanId = (
    example.id ||
    existing?.id ||
    trimmedName
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '') ||
    `example_${Date.now()}`
  ).trim();

  const isOverwrite = example.overwrite ?? Boolean(existing);

  let created: ManagedExample = {
    id: cleanId,
    name: trimmedName,
    category: (example.category || existing?.category || 'Custom').trim(),
    description: (example.description || existing?.description || '').trim(),
    content: example.content,
    custom: existing ? existing.custom : true,
  };

  // 1. Try persisting via backend REST API
  try {
    const dto = await createExampleApi({
      id: cleanId,
      name: created.name,
      category: created.category,
      description: created.description,
      content: created.content,
      overwrite: isOverwrite,
    });
    created = {
      id: dto.id,
      name: dto.name,
      category: dto.category,
      description: dto.description,
      content: dto.content || created.content,
      custom: dto.custom ?? created.custom,
    };
  } catch (err) {
    console.warn('Backend unavailable, saving example to local storage:', err);
  }

  // 2. Persist to local storage
  const local = getLocalCustomExamples().filter((e) => e.id !== created.id);
  local.push({ ...created, custom: true });
  saveLocalCustomExamples(local);

  await loadAllExamples();
  return created;
}

/**
 * Updates an existing circuit example's metadata and/or content.
 */
export async function updateExample(
  id: string,
  updates: {
    name?: string;
    category?: string;
    description?: string;
    content?: string;
  },
): Promise<ManagedExample | null> {
  // 1. Try backend
  try {
    await updateExampleApi(id, updates);
  } catch (err) {
    console.warn('Backend unavailable, updating example locally:', err);
  }

  // 2. Update local custom
  const local = getLocalCustomExamples();
  const idx = local.findIndex((e) => e.id === id);
  let updatedExample: ManagedExample | null = null;

  if (idx >= 0) {
    local[idx] = {
      ...local[idx],
      name: updates.name !== undefined ? updates.name.trim() : local[idx].name,
      category: updates.category !== undefined ? updates.category.trim() : local[idx].category,
      description: updates.description !== undefined ? updates.description.trim() : local[idx].description,
      content: updates.content !== undefined ? updates.content : local[idx].content,
    };
    updatedExample = local[idx];
    saveLocalCustomExamples(local);
  } else {
    // If it was a built-in example being edited locally
    const builtIn = EXAMPLES.find((e) => e.id === id);
    if (builtIn) {
      const customOverride: ManagedExample = {
        ...builtIn,
        name: updates.name !== undefined ? updates.name.trim() : builtIn.name,
        category: updates.category !== undefined ? updates.category.trim() : builtIn.category,
        description: updates.description !== undefined ? updates.description.trim() : builtIn.description,
        content: updates.content !== undefined ? updates.content : builtIn.content,
        custom: true,
      };
      local.push(customOverride);
      updatedExample = customOverride;
      saveLocalCustomExamples(local);
    }
  }

  await loadAllExamples();
  return updatedExample;
}

/**
 * Deletes a circuit example.
 */
export async function deleteExample(id: string): Promise<boolean> {
  // 1. Try backend
  try {
    await deleteExampleApi(id);
  } catch (err) {
    console.warn('Backend unavailable or delete failed:', err);
  }

  // 2. Remove from local storage
  const local = getLocalCustomExamples();
  const next = local.filter((e) => e.id !== id);
  saveLocalCustomExamples(next);

  await loadAllExamples();
  return true;
}
