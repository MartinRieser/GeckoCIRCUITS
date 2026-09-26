/**
 * Tokenizer for the script block mini-language (C/Java-like subset interpreted
 * by the backend's ScriptBlockCalculator). Powers the syntax highlighting of
 * the ScriptCodeEditor; token categories mirror the interpreter's lexer so the
 * highlight always matches what actually executes.
 */

/** One highlighted range of the source text. */
export interface ScriptToken {
  /** Inclusive start index in the source string. */
  start: number;
  /** Exclusive end index in the source string. */
  end: number;
  /** CSS class of the token ('tok-...' or '' for plain text). */
  cls: string;
}

/** Keywords of the script language. */
const KEYWORDS = new Set(['if', 'else']);

/** Math functions supported by the interpreter's FunctionCallExpr. */
const FUNCTIONS = new Set([
  'sin', 'cos', 'tan', 'asin', 'acos', 'atan', 'atan2', 'sinh', 'cosh', 'tanh',
  'sqrt', 'cbrt', 'abs', 'exp', 'log', 'ln', 'log10', 'pow', 'min', 'max',
  'floor', 'ceil', 'round', 'signum', 'sign', 'sgn',
]);

/** Time variables resolved by the interpreter (case-insensitive). */
const TIME_VARIABLES = new Set(['t', 'time', 'dt', 'deltat']);

/** Built-in constants (case-insensitive). */
const CONSTANTS = new Set(['pi', 'e']);

/**
 * Token classes for identifiers: inputs resolve to xIN/uN, outputs to
 * yOUT/yN, matching the interpreter's getVariable/ArrayAccess semantics.
 */
function classifyIdentifier(name: string): string {
  const lower = name.toLowerCase();
  if (KEYWORDS.has(lower)) {
    return 'tok-keyword';
  }
  if (FUNCTIONS.has(lower)) {
    return 'tok-func';
  }
  if (TIME_VARIABLES.has(lower)) {
    return 'tok-time';
  }
  if (CONSTANTS.has(lower)) {
    return 'tok-const';
  }
  if (lower === 'xin' || lower === 'in' || /^u\d+$/.test(lower)) {
    return 'tok-input';
  }
  if (lower === 'yout' || lower === 'y' || /^y\d+$/.test(lower)) {
    return 'tok-output';
  }
  return '';
}

/**
 * Single-pass scanner: comments | numbers | identifiers | operators |
 * whitespace | any other character. Order matters: identifiers must be
 * matched after `Math.` is consumed as an operator-free word, numbers before
 * identifiers so `1e3` is not split.
 */
const TOKEN_RE =
  /(\/\/[^\n]*|\/\*[\s\S]*?(?:\*\/|$))|(\d+(?:\.\d*)?(?:[eE][+-]?\d+)?|\.\d+(?:[eE][+-]?\d+)?)|([A-Za-z_][A-Za-z0-9_]*)|(&&|\|\||==|!=|<=|>=|[-+*/%^<>=!?:;,.()[\]{}])|(\s+)|(.)/g;

/**
 * Tokenizes a script source string into highlighted ranges.
 *
 * @param code script source code
 * @returns tokens covering the whole string in order
 */
export function tokenizeScript(code: string): ScriptToken[] {
  const tokens: ScriptToken[] = [];
  for (const match of code.matchAll(TOKEN_RE)) {
    const start = match.index ?? 0;
    const end = start + match[0].length;
    let cls = '';
    if (match[1] !== undefined) {
      cls = 'tok-comment';
    } else if (match[2] !== undefined) {
      cls = 'tok-number';
    } else if (match[3] !== undefined) {
      cls = classifyIdentifier(match[3]);
    } else if (match[4] !== undefined) {
      cls = 'tok-op';
    }
    // Whitespace and plain characters keep the empty class
    tokens.push({ start, end, cls });
  }
  return tokens;
}
