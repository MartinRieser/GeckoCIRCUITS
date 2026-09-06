import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  isDesktop,
  openLogsFolder,
  registerOpenFileHandler,
  saveFileNative,
  type OpenFilePayload,
} from '../src/desktop';

type Scope = {
  __TAURI__?: { core?: { invoke?: (c: string, a?: Record<string, unknown>) => Promise<unknown> } };
  __geckoOpenFileHandler?: (payload: OpenFilePayload) => void;
  __geckoOpenFileQueue?: OpenFilePayload[];
  __geckoOpenFile?: (payload: OpenFilePayload) => void;
};

const scope = globalThis as Scope;

afterEach(() => {
  delete scope.__TAURI__;
  delete scope.__geckoOpenFileHandler;
  delete scope.__geckoOpenFileQueue;
  vi.restoreAllMocks();
});

describe('isDesktop', () => {
  it('is false in the browser', () => {
    expect(isDesktop()).toBe(false);
  });

  it('is true when the shell injected __TAURI__', () => {
    scope.__TAURI__ = { core: { invoke: async () => null } };
    expect(isDesktop()).toBe(true);
  });
});

describe('registerOpenFileHandler', () => {
  it('drains payloads the shell queued before the editor registered', () => {
    scope.__geckoOpenFileQueue = [{ name: 'a.ipes', base64: 'AAA=' }];
    const received: OpenFilePayload[] = [];
    registerOpenFileHandler((payload) => received.push(payload));
    expect(received).toEqual([{ name: 'a.ipes', base64: 'AAA=' }]);
    expect(scope.__geckoOpenFileQueue).toEqual([]);
  });

  it('routes later payloads straight to the handler', () => {
    // simulate the shell's initialization-script shim exactly
    scope.__geckoOpenFileQueue = [];
    (scope as { __geckoOpenFile?: (payload: OpenFilePayload) => void }).__geckoOpenFile =
      (payload) => {
        const fallback = (queued: OpenFilePayload) => {
          scope.__geckoOpenFileQueue!.push(queued);
        };
        (scope.__geckoOpenFileHandler ?? fallback)(payload);
      };
    const received: OpenFilePayload[] = [];
    registerOpenFileHandler((payload) => received.push(payload));
    scope.__geckoOpenFile?.({ name: 'b.ipes', base64: 'BBB=' });
    expect(received).toEqual([{ name: 'b.ipes', base64: 'BBB=' }]);
  });

  it('buffers payloads that arrive before a handler exists', () => {
    // the shell's init script pushes into the queue when no handler is set
    (scope.__geckoOpenFileQueue ??= []).push({ name: 'c.ipes', base64: 'CCC=' });
    const received: OpenFilePayload[] = [];
    registerOpenFileHandler((payload) => received.push(payload));
    expect(received).toHaveLength(1);
  });
});

describe('openLogsFolder', () => {
  it('is false in the browser', async () => {
    await expect(openLogsFolder()).resolves.toBe(false);
  });

  it('invokes the shell command on the desktop', async () => {
    const invoke = vi.fn().mockResolvedValue(null);
    scope.__TAURI__ = { core: { invoke: invoke as never } };
    await expect(openLogsFolder()).resolves.toBe(true);
    expect(invoke).toHaveBeenCalledWith('open_logs_folder');
  });
});

describe('saveFileNative', () => {
  it('is false in the browser', async () => {
    await expect(saveFileNative('AAA=', 'c.ipes')).resolves.toBe(false);
  });

  it('invokes the shell command and reports the dialog outcome', async () => {
    const invoke = vi.fn().mockResolvedValue('C:/circuits/chosen.ipes');
    scope.__TAURI__ = { core: { invoke: invoke as never } };
    await expect(saveFileNative('AAA=', 'c.ipes')).resolves.toBe(true);
    expect(invoke).toHaveBeenCalledWith('save_file_dialog', {
      base64: 'AAA=',
      suggestedName: 'c.ipes',
    });
  });

  it('reports false when the user cancels the dialog', async () => {
    scope.__TAURI__ = { core: { invoke: async () => null } };
    await expect(saveFileNative('AAA=', 'c.ipes')).resolves.toBe(false);
  });
});
