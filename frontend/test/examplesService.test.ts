import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as client from '../src/api/client';
import {
  loadAllExamples,
  addExample,
  updateExample,
  deleteExample,
} from '../src/model/examplesService';

describe('examplesService', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('loads built-in examples as fallback when backend is unreachable', async () => {
    vi.spyOn(client, 'listExamplesApi').mockRejectedValue(new Error('Network error'));

    const list = await loadAllExamples();
    expect(list.length).toBeGreaterThanOrEqual(13);
    expect(list.some((e) => e.id === 'buck')).toBe(true);
  });

  it('loads examples from backend when available', async () => {
    const mockRemote = [
      {
        id: 'remote_buck',
        name: 'Remote Buck',
        category: 'Power Electronics',
        description: 'From disk',
        content: 'tDURATION 0.01\n',
      },
    ];
    vi.spyOn(client, 'listExamplesApi').mockResolvedValue(mockRemote);

    const list = await loadAllExamples();
    expect(list.some((e) => e.id === 'remote_buck')).toBe(true);
  });

  it('adds an example and persists to localStorage', async () => {
    vi.spyOn(client, 'createExampleApi').mockRejectedValue(new Error('Offline'));

    const created = await addExample({
      name: 'Custom Inverter',
      category: 'Inverters',
      description: 'Single phase bridge',
      content: 'tDURATION 0.05\n',
    });

    expect(created.name).toBe('Custom Inverter');
    expect(created.custom).toBe(true);

    const all = await loadAllExamples();
    expect(all.some((e) => e.name === 'Custom Inverter')).toBe(true);
  });

  it('updates an existing example', async () => {
    vi.spyOn(client, 'updateExampleApi').mockResolvedValue({
      id: 'buck',
      name: 'Updated Buck Title',
      category: 'Power Electronics',
      description: 'New desc',
      filename: 'buck.ipes',
      content: 'tDURATION 0.03\n',
    });

    const updated = await updateExample('buck', {
      name: 'Updated Buck Title',
      description: 'New desc',
    });

    expect(updated?.name).toBe('Updated Buck Title');
  });

  it('deletes an example', async () => {
    vi.spyOn(client, 'deleteExampleApi').mockResolvedValue(undefined);

    await addExample({
      id: 'to_be_deleted',
      name: 'To Delete',
      category: 'Test',
      description: 'Test',
      content: 'tDURATION 0.01\n',
    });

    let all = await loadAllExamples();
    expect(all.some((e) => e.id === 'to_be_deleted')).toBe(true);

    await deleteExample('to_be_deleted');
    all = await loadAllExamples();
    expect(all.some((e) => e.id === 'to_be_deleted')).toBe(false);
  });
});
