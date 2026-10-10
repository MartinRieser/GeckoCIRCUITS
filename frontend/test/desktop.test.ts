import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  isDesktop,
  openLogsFolder,
  registerOpenFileHandler,
  saveFileNative,
  openFileNative,
  saveFileDirectNative,
  setWindowTitleNative,
  confirmDialogNative,
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
  it('is null in the browser', async () => {
    await expect(saveFileNative('AAA=', 'c.ipes')).resolves.toBeNull();
  });

  it('invokes the shell command and reports the chosen path', async () => {
    const invoke = vi.fn().mockResolvedValue('C:/circuits/chosen.ipes');
    scope.__TAURI__ = { core: { invoke: invoke as never } };
    await expect(saveFileNative('AAA=', 'c.ipes')).resolves.toBe('C:/circuits/chosen.ipes');
    expect(invoke).toHaveBeenCalledWith('save_file_dialog', {
      base64: 'AAA=',
      suggestedName: 'c.ipes',
    });
  });

  it('passes currentPath when provided', async () => {
    const invoke = vi.fn().mockResolvedValue('C:/circuits/saved.ipes');
    scope.__TAURI__ = { core: { invoke: invoke as never } };
    await expect(saveFileNative('AAA=', 'c.ipes', 'C:/circuits/prev.ipes')).resolves.toBe('C:/circuits/saved.ipes');
    expect(invoke).toHaveBeenCalledWith('save_file_dialog', {
      base64: 'AAA=',
      suggestedName: 'c.ipes',
      currentPath: 'C:/circuits/prev.ipes',
    });
  });

  it('reports null when the user cancels the dialog', async () => {
    scope.__TAURI__ = { core: { invoke: async () => null } };
    await expect(saveFileNative('AAA=', 'c.ipes')).resolves.toBeNull();
  });
});

describe('openFileNative', () => {
  it('is null in the browser', async () => {
    await expect(openFileNative()).resolves.toBeNull();
  });

  it('invokes the shell command and reports payload', async () => {
    const payload = { name: 'test.ipes', base64: 'QUJD', path: 'C:/test.ipes' };
    const invoke = vi.fn().mockResolvedValue(payload);
    scope.__TAURI__ = { core: { invoke: invoke as never } };
    await expect(openFileNative('C:/prev.ipes')).resolves.toEqual(payload);
    expect(invoke).toHaveBeenCalledWith('open_file_dialog', { currentPath: 'C:/prev.ipes' });
  });

  it('reports null when cancelled', async () => {
    scope.__TAURI__ = { core: { invoke: async () => null } };
    await expect(openFileNative()).resolves.toBeNull();
  });
});

describe('saveFileDirectNative', () => {
  it('is false in the browser', async () => {
    await expect(saveFileDirectNative('C:/test.ipes', 'QUJD')).resolves.toBe(false);
  });

  it('invokes save_file_direct on desktop', async () => {
    const invoke = vi.fn().mockResolvedValue(null);
    scope.__TAURI__ = { core: { invoke: invoke as never } };
    await expect(saveFileDirectNative('C:/test.ipes', 'QUJD')).resolves.toBe(true);
    expect(invoke).toHaveBeenCalledWith('save_file_direct', { path: 'C:/test.ipes', base64: 'QUJD' });
  });
});

describe('setWindowTitleNative', () => {
  it('does nothing in browser', async () => {
    await expect(setWindowTitleNative('test')).resolves.toBeUndefined();
  });

  it('invokes set_window_title on desktop', async () => {
    const invoke = vi.fn().mockResolvedValue(null);
    scope.__TAURI__ = { core: { invoke: invoke as never } };
    await setWindowTitleNative('My Title');
    expect(invoke).toHaveBeenCalledWith('set_window_title', { title: 'My Title' });
  });
});

describe('confirmDialogNative', () => {
  it('falls back to window.confirm in browser', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    await expect(confirmDialogNative('Are you sure?')).resolves.toBe(true);
    expect(confirmSpy).toHaveBeenCalledWith('Are you sure?');
  });

  it('invokes confirm_dialog on desktop', async () => {
    const invoke = vi.fn().mockResolvedValue(true);
    scope.__TAURI__ = { core: { invoke: invoke as never } };
    await expect(confirmDialogNative('Discard?', 'Gecko')).resolves.toBe(true);
    expect(invoke).toHaveBeenCalledWith('confirm_dialog', { message: 'Discard?', title: 'Gecko' });
  });
});


