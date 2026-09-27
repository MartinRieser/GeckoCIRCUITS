/**
 * Lightweight code editor for script blocks: syntax highlighting (overlay of
 * a tokenized <pre> behind a transparent textarea), a line-number gutter with
 * click-to-toggle breakpoints, and a paused-line marker driven by the
 * simulation's script debug session.
 *
 * Deliberately dependency-free: the script language is tiny and the overlay
 * approach keeps the native textarea behavior (selection, undo, IME) intact.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import { tokenizeScript } from './scriptHighlight';

/** Exact line height in px shared by the gutter, highlight layer, and textarea. */
const LINE_HEIGHT = 18;

/** Editor body height in px (about 9 code lines plus padding). */
const EDITOR_HEIGHT = 9 * LINE_HEIGHT + 16;

/** Props of the {@link ScriptCodeEditor}. */
export interface ScriptCodeEditorProps {
  /** Current code text. */
  value: string;
  /** Change callback with the new code text. */
  onChange: (code: string) => void;
  /** 1-based source lines carrying breakpoints. */
  breakpoints: number[];
  /** Called when a gutter line is clicked to toggle its breakpoint. */
  onToggleBreakpoint: (line: number) => void;
  /** 1-based line the debugger is paused on, or null. */
  pausedLine?: number | null;
  /** Optional custom container height (defaults to EDITOR_HEIGHT). */
  height?: number | string;
  /** Blur callback (e.g. apply the script when the editor loses focus). */
  onBlur?: () => void;
}

/**
 * Renders tokenized code as colored spans, keeping every character (including
 * newlines) so the overlay aligns exactly with the textarea.
 */
function renderHighlighted(code: string) {
  const tokens = tokenizeScript(code);
  return tokens.map((token, index) => {
    const text = code.slice(token.start, token.end);
    return token.cls ? (
      <span key={index} className={token.cls}>
        {text}
      </span>
    ) : (
      <span key={index}>{text}</span>
    );
  });
}

/**
 * Script code editor with syntax highlighting, breakpoint gutter, and
 * paused-line marker.
 */
export function ScriptCodeEditor({
  value,
  onChange,
  breakpoints,
  onToggleBreakpoint,
  pausedLine = null,
  height,
  onBlur,
}: ScriptCodeEditorProps) {
  const [scroll, setScroll] = useState({ top: 0, left: 0 });
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const lineCount = useMemo(() => value.split('\n').length, [value]);
  const breakpointSet = useMemo(() => new Set(breakpoints), [breakpoints]);
  const highlighted = useMemo(() => renderHighlighted(value), [value]);

  // Keep the paused statement visible when the debugger stops
  useEffect(() => {
    if (pausedLine == null) {
      return;
    }
    const top = Math.max(0, (pausedLine - 3) * LINE_HEIGHT);
    setScroll({ top, left: 0 });
    if (textareaRef.current) {
      textareaRef.current.scrollTop = top;
    }
  }, [pausedLine]);

  const handleScroll = (event: React.UIEvent<HTMLTextAreaElement>) => {
    const el = event.currentTarget;
    setScroll({ top: el.scrollTop, left: el.scrollLeft });
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // Insert two spaces instead of moving focus
    if (event.key === 'Tab') {
      event.preventDefault();
      const el = event.currentTarget;
      const { selectionStart, selectionEnd } = el;
      const next = value.slice(0, selectionStart) + '  ' + value.slice(selectionEnd);
      onChange(next);
      requestAnimationFrame(() => {
        el.selectionStart = el.selectionEnd = selectionStart + 2;
      });
    }
  };

  return (
    <div className="script-editor" style={{ height: height ?? EDITOR_HEIGHT }}>
      <div className="script-editor-gutter">
        <div style={{ transform: `translateY(${-scroll.top}px)` }}>
          {Array.from({ length: lineCount }, (_, i) => {
            const line = i + 1;
            const classes = [
              'script-gutter-line',
              breakpointSet.has(line) ? 'has-breakpoint' : '',
              pausedLine === line ? 'paused' : '',
            ]
              .filter(Boolean)
              .join(' ');
            return (
              <div
                key={line}
                className={classes}
                style={{ height: LINE_HEIGHT }}
                title="Click to toggle breakpoint"
                onClick={() => onToggleBreakpoint(line)}
              >
                <span className="breakpoint-dot" />
                <span className="gutter-line-number">{line}</span>
              </div>
            );
          })}
        </div>
      </div>
      <div className="script-editor-body">
        <div
          className="script-editor-layer"
          style={{ transform: `translate(${-scroll.left}px, ${-scroll.top}px)` }}
        >
          {pausedLine != null && pausedLine >= 1 && pausedLine <= lineCount && (
            <div
              className="script-editor-paused-line"
              style={{ top: (pausedLine - 1) * LINE_HEIGHT, height: LINE_HEIGHT }}
            />
          )}
          <pre className="script-editor-highlight" aria-hidden="true">
            {highlighted}
            {'\n'}
          </pre>
        </div>
        <textarea
          ref={textareaRef}
          className="script-editor-input"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onScroll={handleScroll}
          onKeyDown={handleKeyDown}
          onBlur={onBlur}
          spellCheck={false}
          wrap="off"
          placeholder="yOUT[0] = xIN[0] * 2;"
        />
      </div>
    </div>
  );
}
