/**
 * Rich Markdown Note card rendered directly on the GeckoCIRCUITS schematic canvas.
 *
 * Supports comprehensive markdown formatting:
 * - Headings (#, ##, ###, ####)
 * - Bold (**bold**), italic (*italic*), inline code (`code`)
 * - Bullet lists (- item, * item) and numbered lists (1. item)
 * - Fenced code blocks (```...```)
 * - Blockquotes (> quote)
 * - Tables (| col1 | col2 |)
 * - Badges and key-value chips
 */
import React from 'react';

export interface SchematicNoteCardProps {
  name: string;
  text: string;
  selected?: boolean;
  width: number;
  height: number;
}

/**
 * Renders LaTeX-style mathematical expressions into formatted HTML/React nodes.
 * Supports subscripts, superscripts, Greek letters, fractions, roots, and operators.
 */
export function renderMath(expr: string, isBlock: boolean = false): React.ReactNode {
  // 1. Clean LaTeX macro tokens
  let s = expr
    .replace(/\\text\{([^}]+)\}/g, '$1')
    .replace(/\\mathrm\{([^}]+)\}/g, '$1')
    .replace(/\\mathbf\{([^}]+)\}/g, '$1')
    .replace(/\\left\(/g, '(')
    .replace(/\\right\)/g, ')')
    .replace(/\\left\[/g, '[')
    .replace(/\\right\]/g, ']')
    .replace(/\\left\|/g, '|')
    .replace(/\\right\|/g, '|')
    .replace(/\\sin/g, 'sin')
    .replace(/\\cos/g, 'cos')
    .replace(/\\tan/g, 'tan')
    .replace(/\\ln/g, 'ln')
    .replace(/\\exp/g, 'exp')
    .replace(/\\,|\\;|\\!|\\quad/g, ' ')
    .replace(/\\omega/g, 'ω')
    .replace(/\\zeta/g, 'ζ')
    .replace(/\\Delta/g, 'Δ')
    .replace(/\\delta/g, 'δ')
    .replace(/\\pi/g, 'π')
    .replace(/\\theta/g, 'θ')
    .replace(/\\alpha/g, 'α')
    .replace(/\\beta/g, 'β')
    .replace(/\\gamma/g, 'γ')
    .replace(/\\lambda/g, 'λ')
    .replace(/\\mu/g, 'µ')
    .replace(/\\tau/g, 'τ')
    .replace(/\\Omega/g, 'Ω')
    .replace(/\\approx/g, '≈')
    .replace(/\\cdot/g, '·')
    .replace(/\\times/g, '×')
    .replace(/\\pm/g, '±')
    .replace(/\\le(q)?/g, '≤')
    .replace(/\\ge(q)?/g, '≥')
    .replace(/\\neq/g, '≠')
    .replace(/\\int/g, '∫')
    .replace(/\\infty/g, '∞')
    .replace(/\\sqrt\{([^}]+)\}/g, '√($1)')
    .replace(/\\frac\{([^}]+)\}\{([^}]+)\}/g, '($1 / $2)');

  // Minus signs
  s = s.replace(/(\s)-(\s)/g, '$1−$2')
       .replace(/([(\[,=])-(\d)/g, '$1−$2');

  // Subscript / Superscript parser:
  // Matches: base_{sub} or base_sub or base^{sup} or base^sup
  const tokenRegex = /([A-Za-z0-9ωζΔδπθαβγλµτΩ])(?:_\{([^{}]+)\}|_([A-Za-z0-9]+))|([A-Za-z0-9ωζΔδπθαβγλµτΩ])(?:\^\{([^{}]+)\}|\^([A-Za-z0-9−-]+))/g;

  const parts: React.ReactNode[] = [];
  let lastIdx = 0;
  let match: RegExpExecArray | null;

  while ((match = tokenRegex.exec(s)) !== null) {
    if (match.index > lastIdx) {
      parts.push(<span key={`txt-${lastIdx}`}>{s.substring(lastIdx, match.index)}</span>);
    }

    if (match[1]) {
      // Subscript
      const base = match[1];
      const sub = match[2] || match[3];
      parts.push(
        <span key={`sub-${match.index}`} className="math-var-sub">
          <span className="math-base">{base}</span>
          <sub className="note-sub">{sub}</sub>
        </span>,
      );
    } else if (match[4]) {
      // Superscript
      const base = match[4];
      const sup = match[5] || match[6];
      parts.push(
        <span key={`sup-${match.index}`} className="math-var-sup">
          <span className="math-base">{base}</span>
          <sup className="note-sup">{sup}</sup>
        </span>,
      );
    }

    lastIdx = tokenRegex.lastIndex;
  }

  if (lastIdx < s.length) {
    parts.push(<span key={`txt-${lastIdx}`}>{s.substring(lastIdx)}</span>);
  }

  return (
    <span className={`note-math ${isBlock ? 'note-math-block' : 'note-math-inline'}`}>
      {parts.length > 0 ? parts : s}
    </span>
  );
}

/**
 * Parses inline markdown tokens (code, math, bold, italic, links) into React elements.
 */
function parseInlineMarkdown(text: string): React.ReactNode[] {
  const elements: React.ReactNode[] = [];
  // Tokenizer regex:
  // 1. Code: `...`
  // 2. Display math: $$...$$
  // 3. Inline math: $...$
  // 4. Bold: **...** or __...__
  // 5. Asterisk italic: *...*
  // 6. Underscore italic: only when word-bounded: (?<=^|[\s(])_([^\s_\n](?:.*?[^\s_\n])?)_(?=[\s),.:;!?]|$)
  // 7. Links: [text](url)
  const regex = /(`[^`]+`|\$\$[^\$]+\$\$|\$[^\$]+\$|\*\*[^*]+\*\*|__[^_]+__|\*[^*\n]+\*|(?<=^|[\s(])_([^\s_\n](?:.*?[^\s_\n])?)_(?=[\s),.:;!?]|$)|\[([^\]]+)\]\(([^)]+)\))/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      elements.push(text.substring(lastIndex, match.index));
    }

    const token = match[0];
    if (token.startsWith('`') && token.endsWith('`')) {
      elements.push(
        <code key={match.index} className="note-inline-code">
          {token.slice(1, -1)}
        </code>,
      );
    } else if (token.startsWith('$$') && token.endsWith('$$')) {
      elements.push(
        <React.Fragment key={`math-block-${match.index}`}>
          {renderMath(token.slice(2, -2), true)}
        </React.Fragment>,
      );
    } else if (token.startsWith('$') && token.endsWith('$')) {
      elements.push(
        <React.Fragment key={`math-inline-${match.index}`}>
          {renderMath(token.slice(1, -1), false)}
        </React.Fragment>,
      );
    } else if (
      (token.startsWith('**') && token.endsWith('**')) ||
      (token.startsWith('__') && token.endsWith('__'))
    ) {
      elements.push(
        <strong key={match.index} className="note-strong">
          {token.slice(2, -2)}
        </strong>,
      );
    } else if (token.startsWith('*') && token.endsWith('*')) {
      elements.push(
        <em key={match.index} className="note-em">
          {token.slice(1, -1)}
        </em>,
      );
    } else if (token.startsWith('_') && token.endsWith('_')) {
      elements.push(
        <em key={match.index} className="note-em">
          {token.slice(1, -1)}
        </em>,
      );
    } else if (token.startsWith('[') && match[3] && match[4]) {
      elements.push(
        <span key={match.index} className="note-link-chip">
          {match[3]}
        </span>,
      );
    }

    lastIndex = regex.lastIndex;
  }

  if (lastIndex < text.length) {
    elements.push(text.substring(lastIndex));
  }

  return elements.length > 0 ? elements : [text];
}

/**
 * Parses a markdown text string into structured React nodes.
 */
export function renderMarkdown(markdown: string): React.ReactNode {
  if (!markdown || !markdown.trim()) {
    return <p className="note-empty-placeholder">Empty documentation note</p>;
  }

  const lines = markdown.split(/\r?\n/);
  const nodes: React.ReactNode[] = [];
  let i = 0;

  while (i < lines.length) {
    const rawLine = lines[i];
    const line = rawLine.trim();

    // 1. Fenced Code Block
    if (line.startsWith('```')) {
      const codeLines: string[] = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        codeLines.push(lines[i]);
        i++;
      }
      i++; // skip closing ```
      nodes.push(
        <pre key={`code-${i}`} className="note-pre">
          <code className="note-block-code">{codeLines.join('\n')}</code>
        </pre>,
      );
      continue;
    }

    // 2. Empty Line
    if (!line) {
      i++;
      continue;
    }

    // 3. Headings
    if (line.startsWith('#### ')) {
      nodes.push(
        <h4 key={`h4-${i}`} className="note-h4">
          {parseInlineMarkdown(line.substring(5))}
        </h4>,
      );
      i++;
      continue;
    }
    if (line.startsWith('### ')) {
      nodes.push(
        <h3 key={`h3-${i}`} className="note-h3">
          {parseInlineMarkdown(line.substring(4))}
        </h3>,
      );
      i++;
      continue;
    }
    if (line.startsWith('## ')) {
      nodes.push(
        <h2 key={`h2-${i}`} className="note-h2">
          {parseInlineMarkdown(line.substring(3))}
        </h2>,
      );
      i++;
      continue;
    }
    if (line.startsWith('# ')) {
      nodes.push(
        <h1 key={`h1-${i}`} className="note-h1">
          {parseInlineMarkdown(line.substring(2))}
        </h1>,
      );
      i++;
      continue;
    }

    // 4. Horizontal Rule
    if (line === '---' || line === '***' || line === '___') {
      nodes.push(<hr key={`hr-${i}`} className="note-hr" />);
      i++;
      continue;
    }

    // 5. Blockquote
    if (line.startsWith('> ')) {
      const quoteLines: string[] = [];
      while (i < lines.length && lines[i].trim().startsWith('>')) {
        quoteLines.push(lines[i].trim().replace(/^>\s?/, ''));
        i++;
      }
      nodes.push(
        <blockquote key={`quote-${i}`} className="note-blockquote">
          {quoteLines.map((ql, qIdx) => (
            <p key={qIdx}>{parseInlineMarkdown(ql)}</p>
          ))}
        </blockquote>,
      );
      continue;
    }

    // 6. Bullet List (- or *)
    if (/^[-*]\s+/.test(line)) {
      const items: string[] = [];
      while (i < lines.length && /^[-*]\s+/.test(lines[i].trim())) {
        items.push(lines[i].trim().replace(/^[-*]\s+/, ''));
        i++;
      }
      nodes.push(
        <ul key={`ul-${i}`} className="note-ul">
          {items.map((item, itemIdx) => (
            <li key={itemIdx}>{parseInlineMarkdown(item)}</li>
          ))}
        </ul>,
      );
      continue;
    }

    // 7. Numbered List (1., 2., etc.)
    if (/^\d+\.\s+/.test(line)) {
      const items: string[] = [];
      while (i < lines.length && /^\d+\.\s+/.test(lines[i].trim())) {
        items.push(lines[i].trim().replace(/^\d+\.\s+/, ''));
        i++;
      }
      nodes.push(
        <ol key={`ol-${i}`} className="note-ol">
          {items.map((item, itemIdx) => (
            <li key={itemIdx}>{parseInlineMarkdown(item)}</li>
          ))}
        </ol>,
      );
      continue;
    }

    // 8. Markdown Table (| col1 | col2 |)
    if (line.startsWith('|') && line.endsWith('|')) {
      const tableLines: string[] = [];
      while (i < lines.length && lines[i].trim().startsWith('|') && lines[i].trim().endsWith('|')) {
        tableLines.push(lines[i].trim());
        i++;
      }
      if (tableLines.length >= 2) {
        const headerCols = tableLines[0]
          .split('|')
          .slice(1, -1)
          .map((c) => c.trim());
        // line 1 is usually |---|---| separator
        const bodyLines = tableLines.slice(tableLines[1].includes('---') ? 2 : 1);
        nodes.push(
          <div key={`table-${i}`} className="note-table-wrap">
            <table className="note-table">
              <thead>
                <tr>
                  {headerCols.map((h, hIdx) => (
                    <th key={hIdx}>{parseInlineMarkdown(h)}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {bodyLines.map((row, rIdx) => {
                  const cols = row
                    .split('|')
                    .slice(1, -1)
                    .map((c) => c.trim());
                  return (
                    <tr key={rIdx}>
                      {cols.map((cell, cIdx) => (
                        <td key={cIdx}>{parseInlineMarkdown(cell)}</td>
                      ))}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>,
        );
        continue;
      }
    }

    // 9. Standard Paragraph
    nodes.push(
      <p key={`p-${i}`} className="note-p">
        {parseInlineMarkdown(line)}
      </p>,
    );
    i++;
  }

  return <>{nodes}</>;
}

/**
 * Schematic Note Card component rendered in SVG foreignObject space.
 */
export const SchematicNoteCard: React.FC<SchematicNoteCardProps> = ({
  name,
  text,
  selected = false,
  width,
  height,
}) => {
  return (
    <div
      className={`schematic-note-card ${selected ? 'is-selected' : ''}`}
      style={{
        width: `${width}px`,
        height: `${height}px`,
        boxSizing: 'border-box',
      }}
    >
      {/* Top Header bar with note archetype badge */}
      <div className="note-card-header">
        <div className="note-card-header-left">
          <span className="note-card-icon">📝</span>
          <span className="note-card-name">{name}</span>
        </div>
        <div className="note-card-header-right">
          <span className="note-card-badge">DOCS</span>
        </div>
      </div>

      {/* Rendered Markdown Body Content */}
      <div className="note-card-body">
        {renderMarkdown(text)}
      </div>
    </div>
  );
};
