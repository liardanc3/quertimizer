const COMMUNITY_SQL_HIGHLIGHT_KEYWORDS = new Set([
  'SELECT',
  'FROM',
  'WHERE',
  'GROUP',
  'BY',
  'ORDER',
  'HAVING',
  'LIMIT',
  'OFFSET',
  'JOIN',
  'INNER',
  'LEFT',
  'RIGHT',
  'FULL',
  'OUTER',
  'ON',
  'AS',
  'AND',
  'OR',
  'NOT',
  'IN',
  'EXISTS',
  'BETWEEN',
  'LIKE',
  'IS',
  'NULL',
  'COUNT',
  'SUM',
  'AVG',
  'MIN',
  'MAX',
  'DISTINCT',
  'CASE',
  'WHEN',
  'THEN',
  'ELSE',
  'END',
  'WITH',
  'UNION',
  'ALL',
  'EXPLAIN',
  'ANALYZE',
  'ANALYSE',
  'CREATE',
  'TEMP',
  'TABLE',
  'INSERT',
  'INTO',
  'VALUES',
  'UPDATE',
  'SET',
  'DELETE',
  'INDEX',
  'DROP',
  'ALTER',
  'ADD',
  'PRIMARY',
  'KEY',
  'FOREIGN',
  'REFERENCES',
  'UNIQUE',
  'CHECK',
  'DEFAULT',
  'PUBLIC',
  'INTEGER',
  'VARCHAR',
  'TEXT',
  'TIMESTAMP',
  'DATE',
  'BOOLEAN',
  'DECIMAL',
  'NUMERIC',
  'BIGINT',
  'SMALLINT',
  'TRUE',
  'FALSE',
]);

const COMMUNITY_SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS = new Set([
  'FROM',
  'JOIN',
  'INTO',
  'UPDATE',
  'TABLE',
  'INDEX',
  'ON',
]);

export const COMMUNITY_DISCLOSURE_LABEL = '요약';
export const COMMUNITY_POST_CONTENT_MAX_BYTES = 500000;

type CommunitySqlTokenKind =
  | 'keyword'
  | 'explain-keyword'
  | 'table'
  | 'column'
  | 'string'
  | 'number'
  | 'comment'
  | 'function'
  | 'operator'
  | 'identifier';

interface CommunitySqlHighlightToken {
  text: string;
  kind: CommunitySqlTokenKind | null;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function hasMeaningfulFragment(value: string) {
  const normalizedValue = value
    .replace(/<img[\s\S]*?>/gi, ' ')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  return normalizedValue.length > 0 || /<img[\s\S]*?>/i.test(value);
}

function normalizeDisclosureLabels(root: ParentNode) {
  root.querySelectorAll('details > summary').forEach((summary) => {
    if (!(summary instanceof HTMLElement)) {
      return;
    }

    summary.textContent = COMMUNITY_DISCLOSURE_LABEL;
  });
}

function tokenizeCommunitySqlLine(line: string) {
  const tokens: CommunitySqlHighlightToken[] = [];
  const tokenPattern =
    /--.*$|'(?:''|[^'])*'|"(?:["]|[^"])*"|[A-Za-z_][A-Za-z0-9_$]*|\d+(?:\.\d+)?|<=|>=|<>|!=|==|[=<>+\-*/%]+|[(),.;]|\s+|./g;
  const lineTokens = Array.from(line.matchAll(tokenPattern), (match) => match[0]);
  let expectTable = false;

  for (let index = 0; index < lineTokens.length; index += 1) {
    const token = lineTokens[index];

    if (/^\s+$/.test(token)) {
      tokens.push({ text: token, kind: null });
      continue;
    }

    if (token.startsWith('--')) {
      tokens.push({ text: token, kind: 'comment' });
      break;
    }

    if (/^'(?:''|[^'])*'$/.test(token) || /^"(?:["]|[^"])*"$/.test(token)) {
      tokens.push({ text: token, kind: 'string' });
      expectTable = false;
      continue;
    }

    if (/^\d+(?:\.\d+)?$/.test(token)) {
      tokens.push({ text: token, kind: 'number' });
      continue;
    }

    if (/^[(),.;]$/.test(token) || /^[=<>+\-*/%]+$/.test(token)) {
      tokens.push({ text: token, kind: 'operator' });

      if (token !== ',') {
        expectTable = false;
      }

      continue;
    }

    if (/^[A-Za-z_][A-Za-z0-9_$]*$/.test(token)) {
      const upperToken = token.toUpperCase();
      const previousMeaningfulToken = [...lineTokens.slice(0, index)]
        .reverse()
        .find((candidate) => !/^\s+$/.test(candidate));
      const nextMeaningfulToken = lineTokens.slice(index + 1).find((candidate) => !/^\s+$/.test(candidate));

      if (COMMUNITY_SQL_HIGHLIGHT_KEYWORDS.has(upperToken)) {
        tokens.push({
          text: token,
          kind:
            upperToken === 'EXPLAIN' || upperToken === 'ANALYZE' || upperToken === 'ANALYSE'
              ? 'explain-keyword'
              : 'keyword',
        });
        expectTable = COMMUNITY_SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS.has(upperToken);
        continue;
      }

      if (previousMeaningfulToken === '.') {
        tokens.push({ text: token, kind: 'column' });
        expectTable = false;
        continue;
      }

      if (expectTable) {
        tokens.push({ text: token, kind: 'table' });
        expectTable = false;
        continue;
      }

      if (nextMeaningfulToken === '(') {
        tokens.push({ text: token, kind: 'function' });
        expectTable = false;
        continue;
      }

      tokens.push({ text: token, kind: 'identifier' });
      expectTable = false;
      continue;
    }

    tokens.push({ text: token, kind: null });
  }

  return tokens;
}

function renderCommunityHighlightedSqlHtml(sql: string) {
  return sql.replace(/\r\n/g, '\n').split('\n')
    .map((line) =>
      tokenizeCommunitySqlLine(line).map((token) =>
        token.kind == null
          ? escapeHtml(token.text)
          : `<span class="solve-sql-token is-${token.kind}">${escapeHtml(token.text)}</span>`,
      ).join(''))
    .join('\n');
}

function decorateCodeBlocks(root: ParentNode) {
  root.querySelectorAll('pre code').forEach((code) => {
    if (!(code instanceof HTMLElement)) {
      return;
    }

    code.innerHTML = renderCommunityHighlightedSqlHtml(code.textContent ?? '');
  });
}

function stripCodeBlockDecorations(root: ParentNode) {
  root.querySelectorAll('pre code').forEach((code) => {
    if (!(code instanceof HTMLElement)) {
      return;
    }

    const rawSql = code.textContent ?? '';
    code.replaceChildren(window.document.createTextNode(rawSql));
  });
}

function extractRangeHtml(range: Range) {
  const container = window.document.createElement('div');
  container.append(range.cloneContents());
  return container.innerHTML;
}

function resolveEditableBlock(node: Node, editor: HTMLElement) {
  let currentNode: Node | null = node;

  while (currentNode && currentNode !== editor) {
    if (currentNode instanceof HTMLElement && /^(P|DIV|LI|BLOCKQUOTE|H1|H2|H3)$/i.test(currentNode.tagName)) {
      return currentNode;
    }

    currentNode = currentNode.parentNode;
  }

  return null;
}

function focusDisclosureBody(details: HTMLDetailsElement, savedRangeRef: { current: Range | null }) {
  const selection = window.getSelection();
  const nextParagraph = details.querySelector('p');

  if (!selection || !nextParagraph) {
    return;
  }

  const range = window.document.createRange();
  range.selectNodeContents(nextParagraph);
  range.collapse(true);
  selection.removeAllRanges();
  selection.addRange(range);
  savedRangeRef.current = range.cloneRange();
}

export function decorateCommunityContentHtml(contentHtml: string) {
  if (typeof window === 'undefined' || contentHtml.trim() === '') {
    return contentHtml;
  }

  const container = window.document.createElement('div');
  container.innerHTML = contentHtml;
  normalizeDisclosureLabels(container);
  decorateCodeBlocks(container);
  return container.innerHTML;
}

export function decorateCommunityEditorContent(editor: HTMLElement | null) {
  if (!editor) {
    return;
  }

  normalizeDisclosureLabels(editor);
  decorateCodeBlocks(editor);
  editor.querySelectorAll('img').forEach((image) => {
    if (!(image instanceof HTMLImageElement)) {
      return;
    }

    image.draggable = true;
    image.classList.add('community-content-image', 'is-draggable');
  });
}

export function normalizeCommunityContentHtml(contentHtml: string) {
  if (typeof window === 'undefined' || contentHtml.trim() === '') {
    return contentHtml;
  }

  const container = window.document.createElement('div');
  container.innerHTML = contentHtml;
  normalizeDisclosureLabels(container);
  stripCodeBlockDecorations(container);
  return container.innerHTML;
}

export function insertCommunityDisclosureAtSelection(editor: HTMLElement | null,
                                                     savedRangeRef: { current: Range | null },
                                                     placeCaretAtEnd: () => void) {
  const selection = window.getSelection();

  if (!editor || !selection || selection.rangeCount === 0) {
    placeCaretAtEnd();
    return false;
  }

  const range = selection.getRangeAt(0);

  if (!editor.contains(range.commonAncestorContainer)) {
    placeCaretAtEnd();
    return false;
  }

  if (!range.collapsed) {
    range.deleteContents();
    selection.removeAllRanges();
    selection.addRange(range);
  }

  const currentBlock = resolveEditableBlock(range.startContainer, editor);

  if (!(currentBlock instanceof HTMLElement) || currentBlock.closest('details, pre')) {
    document.execCommand(
      'insertHTML',
      false,
      `<details class="community-editor-disclosure" open><summary>${COMMUNITY_DISCLOSURE_LABEL}</summary><p><br /></p></details><p><br /></p>`,
    );
    return true;
  }

  const beforeRange = window.document.createRange();
  beforeRange.selectNodeContents(currentBlock);
  beforeRange.setEnd(range.startContainer, range.startOffset);

  const afterRange = window.document.createRange();
  afterRange.selectNodeContents(currentBlock);
  afterRange.setStart(range.startContainer, range.startOffset);

  const beforeHtml = extractRangeHtml(beforeRange);
  const afterHtml = extractRangeHtml(afterRange);
  const wrapper = window.document.createElement('div');

  wrapper.innerHTML = [
    hasMeaningfulFragment(beforeHtml) ? `<p>${beforeHtml}</p>` : '',
    `<details class="community-editor-disclosure" open><summary>${COMMUNITY_DISCLOSURE_LABEL}</summary><p><br /></p></details>`,
    hasMeaningfulFragment(afterHtml) ? `<p>${afterHtml}</p>` : '<p><br /></p>',
  ].join('');

  const fragment = window.document.createDocumentFragment();
  let insertedDisclosure: HTMLDetailsElement | null = null;

  while (wrapper.firstChild) {
    const child = wrapper.firstChild;

    if (child instanceof HTMLDetailsElement) {
      insertedDisclosure = child;
    }

    fragment.appendChild(child);
  }

  currentBlock.replaceWith(fragment);

  if (insertedDisclosure) {
    focusDisclosureBody(insertedDisclosure, savedRangeRef);
  }

  return true;
}
