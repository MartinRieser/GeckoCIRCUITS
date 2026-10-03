// @vitest-environment jsdom
import { describe, it, expect, beforeEach } from 'vitest';
import {
  getRecentFiles,
  addRecentFile,
  removeRecentFile,
  clearRecentFiles,
  formatRelativeTime,
  MAX_RECENT_FILES,
} from '../src/model/recentFiles';

describe('recentFiles store', () => {
  beforeEach(() => {
    localStorage.clear();
    clearRecentFiles();
  });

  it('starts with an empty list', () => {
    expect(getRecentFiles()).toEqual([]);
  });

  it('adds a recent file to the top', () => {
    addRecentFile('buck.ipes', 'content-buck', 5);
    const list = getRecentFiles();
    expect(list).toHaveLength(1);
    expect(list[0].name).toBe('buck.ipes');
    expect(list[0].content).toBe('content-buck');
    expect(list[0].componentCount).toBe(5);
  });

  it('caps at MAX_RECENT_FILES (8) and evicts oldest', () => {
    for (let i = 1; i <= 10; i++) {
      addRecentFile(`circuit_${i}.ipes`, `content_${i}`, i);
    }
    const list = getRecentFiles();
    expect(list).toHaveLength(MAX_RECENT_FILES);
    expect(MAX_RECENT_FILES).toBe(8);

    // Most recent is circuit_10, oldest retained is circuit_3
    expect(list[0].name).toBe('circuit_10.ipes');
    expect(list[7].name).toBe('circuit_3.ipes');
    // circuit_1 and circuit_2 should have been evicted
    expect(list.some((f) => f.name === 'circuit_1.ipes')).toBe(false);
    expect(list.some((f) => f.name === 'circuit_2.ipes')).toBe(false);
  });

  it('promotes existing file to top when re-added', () => {
    addRecentFile('circuit_A.ipes', 'content_A');
    addRecentFile('circuit_B.ipes', 'content_B');
    addRecentFile('circuit_C.ipes', 'content_C');

    expect(getRecentFiles().map((f) => f.name)).toEqual([
      'circuit_C.ipes',
      'circuit_B.ipes',
      'circuit_A.ipes',
    ]);

    // Re-add circuit_A with updated content
    addRecentFile('circuit_A.ipes', 'content_A_updated', 12);
    const updated = getRecentFiles();
    expect(updated).toHaveLength(3);
    expect(updated[0].name).toBe('circuit_A.ipes');
    expect(updated[0].content).toBe('content_A_updated');
    expect(updated[0].componentCount).toBe(12);
  });

  it('removes a file by id', () => {
    addRecentFile('file1.ipes', 'c1');
    addRecentFile('file2.ipes', 'c2');
    const list = getRecentFiles();
    const idToRemove = list[0].id;

    removeRecentFile(idToRemove);
    const after = getRecentFiles();
    expect(after).toHaveLength(1);
    expect(after.some((f) => f.id === idToRemove)).toBe(false);
  });

  it('clears all recent files', () => {
    addRecentFile('file1.ipes', 'c1');
    addRecentFile('file2.ipes', 'c2');
    clearRecentFiles();
    expect(getRecentFiles()).toEqual([]);
  });

  it('handles invalid inputs gracefully', () => {
    expect(addRecentFile('', '')).toEqual([]);
    expect(addRecentFile('   ', 'content')).toEqual([]);
  });

  it('formats relative timestamps correctly', () => {
    const now = Date.now();
    expect(formatRelativeTime(now - 10 * 1000)).toBe('Just now');
    expect(formatRelativeTime(now - 5 * 60 * 1000)).toBe('5m ago');
    expect(formatRelativeTime(now - 2 * 3600 * 1000)).toBe('2h ago');
    expect(formatRelativeTime(now - 3 * 86400 * 1000)).toBe('3d ago');
  });
});
