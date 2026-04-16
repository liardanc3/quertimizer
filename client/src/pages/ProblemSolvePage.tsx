import { useEffect, useMemo, useRef, useState, type KeyboardEvent as ReactKeyboardEvent, type MouseEvent as ReactMouseEvent, type ReactNode, type WheelEvent as ReactWheelEvent } from 'react';
import { createPortal } from 'react-dom';
import ProblemDetailContent from '../components/problem/ProblemDetailContent';
import { fetchProblemDetail, type ProblemDetailData } from '../lib/problemApi';
import {
  SessionSocketError,
  sendSessionSocketMessage,
  sendSessionSocketMessageIfOpen,
  subscribeSessionSocketMessages,
  type SessionSocketMessage,
} from '../lib/sessionSocket';
import { useMockSession } from '../lib/session';
import { mockProblemDetailById, mockProblemDetails } from '../mocks/problemDetail';
import type { DbmsType, ProblemDetail } from '../types/domain';

interface ProblemSolvePageProps {
  problemId: string;
}

type PanelKey = 'editor' | 'submit';

interface PanelVisibilityState {
  editor: boolean;
  submit: boolean;
}

interface PanelDetachState {
  editor: boolean;
  submit: boolean;
}

interface FloatingPanelLayout {
  left: number;
  top: number;
  width: number;
  height: number;
}

interface FloatingPanelLayoutState {
  editor: FloatingPanelLayout;
  submit: FloatingPanelLayout;
}

interface FloatingMoveState {
  panelKey: PanelKey;
  startX: number;
  startY: number;
  startLeft: number;
  startTop: number;
}

interface FloatingResizeState {
  panelKey: PanelKey;
  startX: number;
  startY: number;
  startWidth: number;
  startHeight: number;
}

interface ExternalWindowState {
  editor: boolean;
  submit: boolean;
}

interface PanelExternalWindowProps {
  panelKey: PanelKey;
  title: string;
  layout: FloatingPanelLayout;
  onClose: () => void;
  children: ReactNode;
}

interface CollapsedCardState {
  editor: boolean;
  execute: boolean;
  submit: boolean;
  resultTable: boolean;
}

type ProblemExecutionMode = 'select' | 'explain' | 'explain_analyze' | 'command';
const EXECUTION_RESULT_PAGE_SIZE = 10;

interface ProblemExecutionResult {
  success: boolean;
  mode: ProblemExecutionMode;
  message: string;
  columns: string[];
  rows: string[][];
  planLines: string[];
  rowCount: number;
  executionTimeMs?: number;
}

interface ProblemSocketMessage extends SessionSocketMessage {
  success?: boolean;
  problemId?: string | null;
  mode?: string | null;
  message?: string | null;
  columns?: string[];
  rows?: string[][];
  planLines?: string[];
  rowCount?: number;
  executionTimeMs?: number | null;
}

type SqlAutocompleteKind = 'keyword' | 'table' | 'column';

interface SqlAutocompleteItem {
  value: string;
  kind: SqlAutocompleteKind;
  detail?: string;
}

interface GridColumn {
  key: string;
  label: string;
}

interface GridResizeState {
  columnIndex: number;
  startX: number;
  startWidths: number[];
}

interface SqlAutocompleteState {
  items: SqlAutocompleteItem[];
  selectedIndex: number;
  tokenStart: number;
  tokenEnd: number;
  left: number;
  top: number;
  maxWidth: number;
  maxHeight: number;
}

interface SqlAutocompleteAnchor {
  left: number;
  top: number;
  maxWidth: number;
  maxHeight: number;
}

const panelOrder: PanelKey[] = ['editor', 'submit'];

const panelLabels: Record<PanelKey, string> = {
  editor: '에디터',
  submit: '제출 결과',
};

const panelMinWidths: Record<PanelKey, number> = {
  editor: 420,
  submit: 340,
};

const panelMinHeights: Record<PanelKey, number> = {
  editor: 320,
  submit: 260,
};

const SQL_AUTOCOMPLETE_KEYWORDS = [
  'SELECT',
  'FROM',
  'WHERE',
  'GROUP BY',
  'ORDER BY',
  'HAVING',
  'LIMIT',
  'OFFSET',
  'JOIN',
  'INNER JOIN',
  'LEFT JOIN',
  'RIGHT JOIN',
  'FULL JOIN',
  'ON',
  'AS',
  'AND',
  'OR',
  'NOT',
  'IN',
  'EXISTS',
  'BETWEEN',
  'LIKE',
  'IS NULL',
  'IS NOT NULL',
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
  'EXPLAIN',
  'EXPLAIN ANALYZE',
  'CREATE INDEX',
  'DROP INDEX',
];
const SQL_EDITOR_INDENT = '    ';
const SQL_EDITOR_MIN_HEIGHT = 256;
const SQL_EDITOR_DEFAULT_FONT_SIZE = 13.5;
const SQL_EDITOR_MIN_FONT_SIZE = 11;
const SQL_EDITOR_MAX_FONT_SIZE = 24;
const SQL_EDITOR_AUTOCOMPLETE_OVERFLOW_ITEM_COUNT = 4;
const FLOATING_EDITOR_BACKGROUND_MAX_ALPHA = 0.76;

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function getDbmsLabel(dbms: DbmsType) {
  return dbms === 'postgresql' ? 'PostgreSQL' : 'Oracle';
}

function getAvailableDbms(problem: ProblemDetail) {
  return problem.dbmsOptions.filter((dbms) => !problem.disabledDbms.includes(dbms));
}

function resolvePreferredDbms(availableDbms: DbmsType[], fallbackDbms: DbmsType[], defaultDbms: DbmsType | null) {
  if (defaultDbms && availableDbms.includes(defaultDbms)) {
    return defaultDbms;
  }

  return availableDbms[0] ?? fallbackDbms[0] ?? 'postgresql';
}

function formatGroupedNumber(value?: number) {
  if (value == null) {
    return '-';
  }

  return new Intl.NumberFormat('en-US').format(value);
}

function formatExecutionMetricValue(value?: number) {
  if (value == null) {
    return '-';
  }

  return formatGroupedNumber(Math.round(value));
}

function getExecutionResultPageCount(rowCount: number) {
  return Math.max(1, Math.ceil(rowCount / EXECUTION_RESULT_PAGE_SIZE));
}

function toProblemSequence(problemId: string) {
  const [, problemSequence] = problemId.split('-');
  const parsedNumber = Number.parseInt(problemSequence ?? '', 10);

  return Number.isNaN(parsedNumber) ? 0 : parsedNumber;
}

function buildColumnTemplate(columnWidths: number[]) {
  return columnWidths.map((width) => `${width}px`).join(' ');
}

function resolveColumnWidths(containerWidth: number, columnCount: number, initialWeights?: number[]) {
  const fallbackWeights = initialWeights ?? Array.from({ length: columnCount }, () => 1);
  const totalWeight = fallbackWeights.reduce((sum, weight) => sum + weight, 0);

  if (containerWidth <= 0 || totalWeight <= 0) {
    return fallbackWeights.map(() => 160);
  }

  return fallbackWeights.map((weight) => (weight / totalWeight) * containerWidth);
}

function createFallbackProblemDetail(problemId: string): ProblemDetail {
  const matchedProblem = mockProblemDetailById[problemId];
  if (matchedProblem) {
    return {
      ...matchedProblem,
      problemNumber: matchedProblem.problemNumber ?? problemId,
    };
  }

  return {
    ...mockProblemDetails[0],
    id: problemId,
    number: toProblemSequence(problemId),
    problemNumber: problemId,
    title: '',
    preview: '',
    description: '',
  };
}

function createInitialFloatingLayouts(): FloatingPanelLayoutState {
  return {
    editor: {
      left: 24,
      top: 118,
      width: 760,
      height: 620,
    },
    submit: {
      left: 840,
      top: 500,
      width: 400,
      height: 330,
    },
  };
}

function formatCellValue(value: string | number | boolean | null | undefined) {
  if (value == null) {
    return '';
  }

  return String(value);
}

function createProblemExecutionError(message: string): ProblemExecutionResult {
  return {
    success: false,
    mode: 'command',
    message,
    columns: [],
    rows: [],
    planLines: [],
    rowCount: 0,
  };
}

function shouldRenderExecutionMessage(message: string) {
  return !['조회 결과를 반환했다.', '실행 계획을 반환했다.'].includes(message.trim());
}

function resolveProblemDdl(detail: ProblemDetailData | null, dbms: DbmsType) {
  const preferredDdl = dbms === 'oracle' ? detail?.ddlOracle ?? '' : detail?.ddlPostgresql ?? '';
  const fallbackDdl = dbms === 'oracle' ? detail?.ddlPostgresql ?? '' : detail?.ddlOracle ?? '';

  return preferredDdl.trim() !== '' ? preferredDdl : fallbackDdl;
}

function extractAutocompleteItemsFromDdl(ddl: string): SqlAutocompleteItem[] {
  const createTablePattern = /CREATE TABLE\s+(?:[\w]+\.)?(\w+)\s*\(([\s\S]*?)\);/gi;
  const items: SqlAutocompleteItem[] = [];
  let match: RegExpExecArray | null;

  while ((match = createTablePattern.exec(ddl)) != null) {
    const tableName = match[1];
    const tableBody = match[2];

    items.push({
      value: tableName,
      kind: 'table',
    });

    tableBody
      .split('\n')
      .map((line) => line.trim().replace(/,$/, ''))
      .filter(Boolean)
      .forEach((line) => {
        if (/^(CONSTRAINT|PRIMARY KEY|FOREIGN KEY|UNIQUE|CHECK)\b/i.test(line)) {
          return;
        }

        const columnMatch = line.match(/^("?[\w]+"?)\s+/);
        if (!columnMatch) {
          return;
        }

        items.push({
          value: columnMatch[1].replace(/"/g, ''),
          kind: 'column',
          detail: tableName,
        });
      });
  }

  return items.filter(
    (item, index, source) =>
      source.findIndex(
        (candidate) =>
          candidate.value.toLowerCase() === item.value.toLowerCase() &&
          candidate.kind === item.kind &&
          (candidate.detail ?? '') === (item.detail ?? ''),
      ) === index,
  );
}

function getAutocompleteTokenRange(value: string, caretIndex: number) {
  let tokenStart = caretIndex;
  let tokenEnd = caretIndex;

  while (tokenStart > 0 && /[A-Za-z0-9_]/.test(value[tokenStart - 1])) {
    tokenStart -= 1;
  }

  while (tokenEnd < value.length && /[A-Za-z0-9_]/.test(value[tokenEnd])) {
    tokenEnd += 1;
  }

  const currentToken = value.slice(tokenStart, tokenEnd);
  const typedToken = value.slice(tokenStart, caretIndex);

  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(currentToken) || typedToken.length === 0) {
    return null;
  }

  return {
    tokenStart,
    tokenEnd,
    currentToken,
    typedToken,
  };
}

function createAutocompleteSuggestions(items: SqlAutocompleteItem[], typedToken: string) {
  const normalizedTypedToken = typedToken.toLowerCase();
  const kindPriority: Record<SqlAutocompleteKind, number> = {
    keyword: 0,
    table: 1,
    column: 2,
  };

  return items
    .filter((item) => {
      if (!item.value.toLowerCase().startsWith(normalizedTypedToken)) {
        return false;
      }

      if (item.kind === 'keyword' && item.value.replace(/\s+/g, '').length < 3) {
        return false;
      }

      return true;
    })
    .sort((left, right) => {
      const kindOrder = kindPriority[left.kind] - kindPriority[right.kind];
      if (kindOrder !== 0) {
        return kindOrder;
      }

      const leftExact = left.value.toLowerCase() === normalizedTypedToken ? 1 : 0;
      const rightExact = right.value.toLowerCase() === normalizedTypedToken ? 1 : 0;
      if (leftExact !== rightExact) {
        return leftExact - rightExact;
      }

      if (left.value.length !== right.value.length) {
        return left.value.length - right.value.length;
      }

      return left.value.localeCompare(right.value);
      })
      .slice(0, 6);
}

function indentSqlEditorValue(value: string, selectionStart: number, selectionEnd: number) {
  if (selectionStart === selectionEnd) {
    const nextSql = `${value.slice(0, selectionStart)}${SQL_EDITOR_INDENT}${value.slice(selectionEnd)}`;
    const nextCaretIndex = selectionStart + SQL_EDITOR_INDENT.length;

    return {
      nextSql,
      nextSelectionStart: nextCaretIndex,
      nextSelectionEnd: nextCaretIndex,
    };
  }

  const lineStart = value.lastIndexOf('\n', selectionStart - 1) + 1;
  const selectedText = value.slice(lineStart, selectionEnd);
  const indentedText = selectedText
    .split('\n')
    .map((line) => `${SQL_EDITOR_INDENT}${line}`)
    .join('\n');
  const lineCount = selectedText.split('\n').length;

  return {
    nextSql: `${value.slice(0, lineStart)}${indentedText}${value.slice(selectionEnd)}`,
    nextSelectionStart: selectionStart + SQL_EDITOR_INDENT.length,
    nextSelectionEnd: selectionEnd + SQL_EDITOR_INDENT.length * lineCount,
  };
}

function measureAutocompleteAnchor(textarea: HTMLTextAreaElement, value: string, caretIndex: number, suggestionCount: number): SqlAutocompleteAnchor {
  const computedStyle = window.getComputedStyle(textarea);
  const mirror = document.createElement('div');
  const mirrorHost = textarea.parentElement ?? document.body;
  const textareaRect = textarea.getBoundingClientRect();
  const mirrorStyleProperties = [
    'box-sizing',
    'padding-top',
    'padding-right',
    'padding-bottom',
    'padding-left',
    'border-top-width',
    'border-right-width',
    'border-bottom-width',
    'border-left-width',
    'font-family',
    'font-size',
    'font-style',
    'font-weight',
    'letter-spacing',
    'line-height',
    'text-transform',
    'text-indent',
    'tab-size',
  ];

  mirrorStyleProperties.forEach((property) => {
    mirror.style.setProperty(property, computedStyle.getPropertyValue(property));
  });

  mirror.style.position = 'absolute';
  mirror.style.visibility = 'hidden';
  mirror.style.pointerEvents = 'none';
  mirror.style.whiteSpace = 'pre-wrap';
  mirror.style.wordBreak = 'break-word';
  mirror.style.overflowWrap = 'anywhere';
  mirror.style.left = '0';
  mirror.style.top = '0';
  mirror.style.width = `${textarea.clientWidth}px`;
  mirror.style.minHeight = `${textarea.clientHeight}px`;
  mirror.style.overflow = 'hidden';

  mirror.textContent = value.slice(0, caretIndex);

  const caretMarker = document.createElement('span');
  caretMarker.textContent = value.slice(caretIndex, caretIndex + 1) || ' ';
  mirror.appendChild(caretMarker);
  mirrorHost.appendChild(mirror);
  const editorPadding = 12;
  const panelGap = 8;
  const lineHeight = caretMarker.offsetHeight || Number.parseFloat(computedStyle.lineHeight) || Number.parseFloat(computedStyle.fontSize) * 1.5 || 20;
  const availableWidth = Math.max(160, textarea.clientWidth - editorPadding * 2);
  const maxWidth = Math.min(448, availableWidth);
  const caretLeft = caretMarker.offsetLeft - textarea.scrollLeft;
  const left = clamp(textareaRect.left + caretLeft, editorPadding, Math.max(editorPadding, window.innerWidth - maxWidth - editorPadding));
  const caretTop = caretMarker.offsetTop - textarea.scrollTop;
  const caretBottom = caretTop + lineHeight;
  const panelMaxHeight = 184;
  const desiredVisibleItems = Math.min(suggestionCount, 6);
  const overflowVisibleItems = Math.min(suggestionCount, SQL_EDITOR_AUTOCOMPLETE_OVERFLOW_ITEM_COUNT);
  const desiredPopupHeight = Math.min(panelMaxHeight, 18 + desiredVisibleItems * 30);
  const belowTop = caretBottom + panelGap;
  const availableBelow = Math.max(0, textarea.clientHeight - belowTop - editorPadding);
  const exceedsEditor = availableBelow < desiredPopupHeight;
  const overflowAllowance = overflowVisibleItems * 30;
  const maxHeight = Math.max(
    0,
    Math.min(
      exceedsEditor ? availableBelow + overflowAllowance : desiredPopupHeight,
      desiredPopupHeight,
      Math.max(56, window.innerHeight - (textareaRect.top + belowTop) - editorPadding),
    ),
  );
  const top = textareaRect.top + belowTop;

  mirrorHost.removeChild(mirror);

  return {
    left,
    top,
    maxWidth,
    maxHeight,
  };
}

function toProblemExecutionResult(message: ProblemSocketMessage): ProblemExecutionResult {
  return {
    success: message.success ?? false,
    mode: (message.mode as ProblemExecutionMode | null) ?? 'command',
    message: message.message ?? '',
    columns: message.columns ?? [],
    rows: message.rows ?? [],
    planLines: message.planLines ?? [],
    rowCount: message.rowCount ?? 0,
    executionTimeMs: message.executionTimeMs ?? undefined,
  };
}

function renderResultTable(
  columns: string[],
  rows: string[][],
  emptyMessage: string,
  currentPage: number,
  onPageChange: (page: number) => void,
  pageInput: string,
  onPageInputChange: (value: string) => void,
  collapsed: boolean,
  onToggleCollapse: () => void,
  resetKey: number,
) {
  if (rows.length === 0) {
    return <div className="solve-result-empty solve-result-empty-table">{emptyMessage}</div>;
  }

  const columnLabels =
    columns.length > 0
      ? columns
      : Array.from({ length: rows.reduce((maxCount, row) => Math.max(maxCount, row.length), 0) }, (_, index) => `컬럼 ${index + 1}`);

  const totalPages = getExecutionResultPageCount(rows.length);
  const normalizedPage = clamp(currentPage, 1, totalPages);
  const pageRows = rows.slice(
    (normalizedPage - 1) * EXECUTION_RESULT_PAGE_SIZE,
    normalizedPage * EXECUTION_RESULT_PAGE_SIZE,
  );
  const gridColumns = columnLabels.map((columnLabel) => ({ key: columnLabel, label: columnLabel }));
  const gridRows = pageRows.map((row) => columnLabels.map((_, columnIndex) => formatCellValue(row[columnIndex])));

  return (
    <div className="solve-result-table-block">
      <div className="solve-detail-table-block solve-result-table-shell">
        <div className="solve-detail-table-block-header">
          <div className="solve-detail-table-block-actions">
            <button type="button" className="solve-detail-table-toggle" aria-expanded={!collapsed} onClick={onToggleCollapse}>
              <span className={`solve-detail-table-toggle-icon ${collapsed ? '' : 'is-open'}`}>{'>'}</span>
            </button>
          </div>

          <div className="solve-detail-table-block-copy">
            <p className="solve-detail-table-name solve-result-table-name">Result</p>
          </div>
        </div>

        {!collapsed ? <ExecutionResultGrid columns={gridColumns} rows={gridRows} emptyMessage={emptyMessage} resetKey={resetKey} /> : null}
      </div>

      {!collapsed && totalPages > 1 ? (
        <div className="solve-result-pagination">
          <button
            type="button"
            className="mini-toggle solve-result-pagination-button"
            onClick={() => onPageChange(normalizedPage - 1)}
            disabled={normalizedPage === 1}
          >
            이전
          </button>
          <span className="solve-result-pagination-label">
            {normalizedPage} / {totalPages}
          </span>
          <form
            className="solve-result-pagination-form"
            onSubmit={(event) => {
              event.preventDefault();
              const parsedPage = Number.parseInt(pageInput, 10);
              if (Number.isNaN(parsedPage)) {
                onPageInputChange(String(normalizedPage));
                return;
              }

              onPageChange(clamp(parsedPage, 1, totalPages));
            }}
          >
            <input
              type="number"
              min={1}
              max={totalPages}
              className="text-field solve-result-pagination-input"
              value={pageInput}
              onChange={(event) => onPageInputChange(event.target.value)}
            />
            <button type="submit" className="mini-toggle solve-result-pagination-button">
              이동
            </button>
          </form>
          <button
            type="button"
            className="mini-toggle solve-result-pagination-button"
            onClick={() => onPageChange(normalizedPage + 1)}
            disabled={normalizedPage === totalPages}
          >
            다음
          </button>
        </div>
      ) : null}
    </div>
  );
}

function renderExecutionContent(
  executionResult: ProblemExecutionResult,
  currentPage: number,
  onPageChange: (page: number) => void,
  pageInput: string,
  onPageInputChange: (value: string) => void,
  collapsed: boolean,
  onToggleCollapse: () => void,
  resetKey: number,
) {
  if (!executionResult.success) {
    return null;
  }

  if (executionResult.mode === 'select') {
    return renderResultTable(
      executionResult.columns,
      executionResult.rows,
      '표시할 실행 결과가 없다.',
      currentPage,
      onPageChange,
      pageInput,
      onPageInputChange,
      collapsed,
      onToggleCollapse,
      resetKey,
    );
  }

  if (executionResult.mode === 'explain' || executionResult.mode === 'explain_analyze') {
    if (executionResult.planLines.length === 0) {
      return <div className="solve-result-empty solve-result-empty-table">표시할 실행 계획이 없다.</div>;
    }

    return (
      <div className="solve-plan-block">
        <pre className="solve-plan-lines">{executionResult.planLines.join('\n')}</pre>
      </div>
    );
  }

  return null;
}

function copyDocumentStyles(targetDocument: Document) {
  targetDocument.head.innerHTML = '';

  document.querySelectorAll('style, link[rel="stylesheet"]').forEach((styleNode) => {
    targetDocument.head.appendChild(styleNode.cloneNode(true));
  });
}

function ExternalWindowIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M3 3.5h5v1H4V12h7.5V8h1v4A1.5 1.5 0 0 1 11 13.5H4A1.5 1.5 0 0 1 2.5 12V5A1.5 1.5 0 0 1 4 3.5Zm5.5-1h5v5h-1V4.2L8.6 7.6l-.7-.7 3.4-3.4H8.5v-1Z"
        fill="currentColor"
      />
    </svg>
  );
}

function PipIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M2.5 3A1.5 1.5 0 0 1 4.0 1.5h8A1.5 1.5 0 0 1 13.5 3v6A1.5 1.5 0 0 1 12 10.5H8.5v2H12v1H4v-1h3.5v-2H4A1.5 1.5 0 0 1 2.5 9V3Zm1 0V9a.5.5 0 0 0 .5.5h8A.5.5 0 0 0 12.5 9V3a.5.5 0 0 0-.5-.5H4a.5.5 0 0 0-.5.5Zm5.5 1.5h2.5V7H9V4.5Z"
        fill="currentColor"
      />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M4.2 4.2a.75.75 0 0 1 1.06 0L8 6.94l2.74-2.74a.75.75 0 1 1 1.06 1.06L9.06 8l2.74 2.74a.75.75 0 1 1-1.06 1.06L8 9.06 5.26 11.8a.75.75 0 0 1-1.06-1.06L6.94 8 4.2 5.26a.75.75 0 0 1 0-1.06Z"
        fill="currentColor"
      />
    </svg>
  );
}

function CollapseChevronIcon({ collapsed }: { collapsed: boolean }) {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d={collapsed ? 'M4.2 6.2 8 10l3.8-3.8' : 'M4.2 9.8 8 6l3.8 3.8'}
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function RefreshIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M15.9 7.75A6.15 6.15 0 0 0 5.75 4.85"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.75"
      />
      <path
        d="M5.7 2.3v3.15h3.15"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.75"
      />
      <path
        d="M4.1 12.25A6.15 6.15 0 0 0 14.25 15.15"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.75"
      />
      <path
        d="M14.3 17.7v-3.15H11.15"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.75"
      />
    </svg>
  );
}

function OpacityIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M10 2.8c2 2.4 4.8 5.8 4.8 8.3A4.8 4.8 0 1 1 5.2 11C5.2 8.6 8 5.2 10 2.8Z"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.7"
      />
      <path d="M10 4.6v11" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.7" />
    </svg>
  );
}

function ExecutionResultGrid({ columns, rows, emptyMessage, resetKey }: { columns: GridColumn[]; rows: string[][]; emptyMessage: string; resetKey: number }) {
  const gridRef = useRef<HTMLDivElement | null>(null);
  const [columnWidths, setColumnWidths] = useState<number[]>([]);
  const [resizeState, setResizeState] = useState<GridResizeState | null>(null);
  const columnSignature = columns.map((column) => column.key).join('|');

  useEffect(() => {
    if (!gridRef.current || columns.length === 0) {
      setColumnWidths([]);
      return;
    }

    const containerWidth = gridRef.current.getBoundingClientRect().width;
    setColumnWidths(resolveColumnWidths(containerWidth, columns.length));
  }, [columnSignature, columns.length, resetKey]);

  useEffect(() => {
    if (!resizeState) {
      return;
    }

    const handleMouseMove = (event: MouseEvent) => {
      if (!gridRef.current) {
        return;
      }

      const containerWidth = gridRef.current.getBoundingClientRect().width;
      const minimumColumnWidths = resizeState.startWidths.map(() => Math.max(88, containerWidth * 0.08));
      const deltaWidth = event.clientX - resizeState.startX;
      const leftWidth = resizeState.startWidths[resizeState.columnIndex];
      const rightWidth = resizeState.startWidths[resizeState.columnIndex + 1];
      const pairWidth = leftWidth + rightWidth;
      const nextLeftWidth = Math.min(
        Math.max(leftWidth + deltaWidth, minimumColumnWidths[resizeState.columnIndex]),
        pairWidth - minimumColumnWidths[resizeState.columnIndex + 1],
      );
      const nextWidths = [...resizeState.startWidths];

      nextWidths[resizeState.columnIndex] = nextLeftWidth;
      nextWidths[resizeState.columnIndex + 1] = pairWidth - nextLeftWidth;
      setColumnWidths(nextWidths);
    };

    const handleMouseUp = () => {
      setResizeState(null);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [resizeState]);

  if (columns.length === 0) {
    return <p className="solve-detail-empty">{emptyMessage}</p>;
  }

  const resolvedColumnWidths = columnWidths.length === columns.length ? columnWidths : resolveColumnWidths(0, columns.length);
  const columnTemplate = buildColumnTemplate(resolvedColumnWidths);
  const rowWidth = `${resolvedColumnWidths.reduce((sum, width) => sum + width, 0)}px`;

  const renderResizer = (columnIndex: number, label: string) => {
    if (columnIndex >= columns.length - 1) {
      return null;
    }

    return (
      <button
        type="button"
        className="solve-detail-grid-resizer"
        aria-label={`${label} 너비 조절`}
        onMouseDown={(event) => {
          event.preventDefault();
          setResizeState({
            columnIndex,
            startX: event.clientX,
            startWidths: resolvedColumnWidths,
          });
        }}
      />
    );
  };

  return (
    <div className="solve-detail-grid-table solve-result-grid-table" ref={gridRef}>
      <div className="solve-detail-grid-row solve-detail-grid-row-head is-compact" style={{ gridTemplateColumns: columnTemplate, width: rowWidth }}>
        {columns.map((column, columnIndex) => (
          <div key={column.key} className="solve-detail-grid-cell solve-detail-grid-cell-head is-compact">
            <span>{column.label}</span>
            {renderResizer(columnIndex, column.label)}
          </div>
        ))}
      </div>

      {rows.length > 0 ? (
        rows.map((row, rowIndex) => (
          <div key={`execution-grid-row-${rowIndex}`} className="solve-detail-grid-row is-compact" style={{ gridTemplateColumns: columnTemplate, width: rowWidth }}>
            {columns.map((column, columnIndex) => (
              <div key={`execution-grid-row-${rowIndex}-${column.key}`} className="solve-detail-grid-cell is-compact">
                {formatCellValue(row[columnIndex])}
                {renderResizer(columnIndex, column.label)}
              </div>
            ))}
          </div>
        ))
      ) : (
        <p className="solve-detail-empty">{emptyMessage}</p>
      )}
    </div>
  );
}

function PanelExternalWindow({ panelKey, title, layout, onClose, children }: PanelExternalWindowProps) {
  const externalWindowRef = useRef<Window | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const onCloseRef = useRef(onClose);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const openedWindow = window.open(
      '',
      `quertimizer-${panelKey}`,
      `popup=yes,width=${Math.round(layout.width)},height=${Math.round(layout.height)},left=${Math.round(layout.left)},top=${Math.round(layout.top)}`,
    );

    if (!openedWindow) {
      onCloseRef.current();
      return;
    }

    externalWindowRef.current = openedWindow;
    copyDocumentStyles(openedWindow.document);
    openedWindow.document.title = title;
    openedWindow.document.body.innerHTML = '';
    openedWindow.document.body.className = document.body.className;
    openedWindow.document.body.style.margin = '0';
    openedWindow.document.body.style.background = '#eef3f9';
    openedWindow.document.body.style.overflow = 'hidden';

    const container = openedWindow.document.createElement('div');
    container.className = 'solve-external-window-root';
    openedWindow.document.body.appendChild(container);
    containerRef.current = container;
    setIsReady(true);

    const handleBeforeUnload = () => {
      onCloseRef.current();
    };

    openedWindow.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      openedWindow.removeEventListener('beforeunload', handleBeforeUnload);

      if (!openedWindow.closed) {
        openedWindow.close();
      }
    };
  }, [layout.height, layout.left, layout.top, layout.width, panelKey, title]);

  useEffect(() => {
    if (!externalWindowRef.current || externalWindowRef.current.closed) {
      return;
    }

    externalWindowRef.current.moveTo(Math.round(layout.left), Math.round(layout.top));
    externalWindowRef.current.resizeTo(Math.round(layout.width), Math.round(layout.height));
  }, [layout.height, layout.left, layout.top, layout.width]);

  if (!isReady || !containerRef.current) {
    return null;
  }

  return createPortal(children, containerRef.current);
}

export default function ProblemSolvePage({ problemId }: ProblemSolvePageProps) {
  const sqlEditorRef = useRef<HTMLTextAreaElement | null>(null);
  const executionPanelRef = useRef<HTMLDivElement | null>(null);
  const submitPanelRef = useRef<HTMLElement | null>(null);
  const { defaultDbms, isAuthenticated } = useMockSession();
  const fallbackProblem = createFallbackProblemDetail(problemId);
  const [problemDetail, setProblemDetail] = useState<ProblemDetailData | null>(null);
  const [problemLoadError, setProblemLoadError] = useState<string | null>(null);
  const [executionResult, setExecutionResult] = useState<ProblemExecutionResult | null>(null);
  const [executionResultPage, setExecutionResultPage] = useState(1);
  const [executionResultPageInput, setExecutionResultPageInput] = useState('1');
  const [isExecuting, setIsExecuting] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [autocompleteState, setAutocompleteState] = useState<SqlAutocompleteState | null>(null);
  const [collapsedCards, setCollapsedCards] = useState<CollapsedCardState>({
    editor: false,
    execute: false,
    submit: false,
    resultTable: false,
  });
  const [executionResultGridResetKey, setExecutionResultGridResetKey] = useState(0);
  const [panelVisibility, setPanelVisibility] = useState<PanelVisibilityState>({
    editor: true,
    submit: true,
  });
  const [detachedPanels, setDetachedPanels] = useState<PanelDetachState>({
    editor: false,
    submit: false,
  });
  const [externalWindowPanels, setExternalWindowPanels] = useState<ExternalWindowState>({
    editor: false,
    submit: false,
  });
  const [floatingLayouts, setFloatingLayouts] = useState<FloatingPanelLayoutState>(() => createInitialFloatingLayouts());
  const [floatingMoveState, setFloatingMoveState] = useState<FloatingMoveState | null>(null);
  const [floatingResizeState, setFloatingResizeState] = useState<FloatingResizeState | null>(null);
  const [editorFloatingOpacity, setEditorFloatingOpacity] = useState(FLOATING_EDITOR_BACKGROUND_MAX_ALPHA);
  const problem = fallbackProblem;
  const availableDbms = getAvailableDbms(problem);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(
    resolvePreferredDbms(availableDbms, problem.dbmsOptions, defaultDbms ?? null)
  );
  const [sql, setSql] = useState(problem.starterSql);
  const [sqlEditorFontSize, setSqlEditorFontSize] = useState(SQL_EDITOR_DEFAULT_FONT_SIZE);
  const selectedDdl = useMemo(() => resolveProblemDdl(problemDetail, selectedDbms), [problemDetail, selectedDbms]);
  const ddlAutocompleteItems = useMemo(() => extractAutocompleteItemsFromDdl(selectedDdl), [selectedDdl]);
  const autocompleteItems = useMemo(
    () => [
      ...SQL_AUTOCOMPLETE_KEYWORDS.map(
        (keyword) =>
          ({
            value: keyword,
            kind: 'keyword',
          }) satisfies SqlAutocompleteItem,
      ),
      ...ddlAutocompleteItems,
    ],
    [ddlAutocompleteItems],
  );

  const displayProblemNumber = problemDetail?.problemId ?? problem.problemNumber ?? problemId;
  const displayProblemTitle =
    problemDetail?.title ?? (problem.title || (problemLoadError ? '문제 정보를 불러오지 못했다.' : '문제 정보를 불러오는 중..'));

  const visibleFloatingPanels = panelOrder.filter(
    (panelKey) => panelVisibility[panelKey] && detachedPanels[panelKey] && !externalWindowPanels[panelKey],
  );
  const visibleExternalWindows = panelOrder.filter((panelKey) => panelVisibility[panelKey] && externalWindowPanels[panelKey]);
  const focusPanelSection = (resolveElement: () => HTMLElement | null) => {
    requestAnimationFrame(() => {
      const element = resolveElement();
      if (!element) {
        return;
      }

      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      element.focus({ preventScroll: true });
    });
  };

  useEffect(() => {
    let cancelled = false;

    setProblemDetail(null);
    setProblemLoadError(null);

    void fetchProblemDetail(problemId)
      .then((detail) => {
        if (cancelled) {
          return;
        }

        setProblemDetail(detail);
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        setProblemLoadError(error instanceof Error ? error.message : '문제 상세 조회에 실패했다.');
      });

    return () => {
      cancelled = true;
    };
  }, [problemId]);

  useEffect(() => {
    setSql(fallbackProblem.starterSql);
    setExecutionResult(null);
    setExecutionResultPage(1);
    setIsExecuting(false);
    setSubmitMessage(null);
    setEditorFloatingOpacity(FLOATING_EDITOR_BACKGROUND_MAX_ALPHA);
    setSelectedDbms(resolvePreferredDbms(availableDbms, problem.dbmsOptions, defaultDbms ?? null));
  }, [defaultDbms, problemId]);

  useEffect(() => {
    if (!sqlEditorRef.current) {
      return;
    }

    const textarea = sqlEditorRef.current;
    textarea.style.height = '0px';
    textarea.style.height = `${Math.max(SQL_EDITOR_MIN_HEIGHT, textarea.scrollHeight)}px`;
  }, [collapsedCards.editor, sql, sqlEditorFontSize]);

  useEffect(() => {
    if (autocompleteState == null || !sqlEditorRef.current) {
      return;
    }

    updateAutocompleteState(sqlEditorRef.current.value, sqlEditorRef.current.selectionStart ?? sqlEditorRef.current.value.length);
  }, [autocompleteState != null, sqlEditorFontSize]);

  useEffect(() => {
    if (autocompleteState == null) {
      return;
    }

    const handleViewportChange = () => {
      refreshAutocompleteFromEditor();
    };

    window.addEventListener('resize', handleViewportChange);
    window.addEventListener('scroll', handleViewportChange, true);

    return () => {
      window.removeEventListener('resize', handleViewportChange);
      window.removeEventListener('scroll', handleViewportChange, true);
    };
  }, [autocompleteState, sqlEditorFontSize]);

  useEffect(() => {
    const unsubscribe = subscribeSessionSocketMessages((message) => {
      if (message.type === 'connected' || message.type === 'problem.leave.result') {
        return;
      }

      if (message.type === 'problem.submit.result') {
        setIsSubmitting(false);
        setSubmitMessage((message as ProblemSocketMessage).message ?? '제출을 기록하지 못했다.');
        setCollapsedCards((current) => ({
          ...current,
          submit: false,
        }));
        setPanelVisibility((current) => ({
          ...current,
          submit: true,
        }));
        setDetachedPanels((current) => ({
          ...current,
          submit: false,
        }));
        setExternalWindowPanels((current) => ({
          ...current,
          submit: false,
        }));
        focusPanelSection(() => submitPanelRef.current);
        return;
      }

      if (message.type === 'problem.execute.result' || message.type === 'error') {
        setIsExecuting(false);
        setExecutionResult(
          message.type === 'error'
            ? createProblemExecutionError(((message as ProblemSocketMessage).message ?? '문제 실행에 실패했다.'))
            : toProblemExecutionResult(message as ProblemSocketMessage)
        );
        setCollapsedCards((current) => ({
          ...current,
          execute: false,
        }));
        setPanelVisibility((current) => ({
          ...current,
          editor: true,
        }));
        if (!detachedPanels.editor) {
          focusPanelSection(() => executionPanelRef.current);
        }
      }
    });

    return () => {
      unsubscribe();
      sendSessionSocketMessageIfOpen({
        type: 'problem.leave',
        problemId,
      });
    };
  }, [detachedPanels.editor, problemId]);

  useEffect(() => {
    setExecutionResultPage(1);
    setExecutionResultPageInput('1');
    setCollapsedCards((current) => ({
      ...current,
      resultTable: false,
    }));
  }, [executionResult]);

  useEffect(() => {
    setExecutionResultPageInput(String(executionResultPage));
  }, [executionResultPage]);

  useEffect(() => {
    setIsSubmitting(false);
    setSubmitMessage(null);
  }, [problemId]);

  useEffect(() => {
    const handleBeforeUnload = () => {
      sendSessionSocketMessageIfOpen({
        type: 'problem.leave',
        problemId,
      });
    };

    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [problemId]);

  useEffect(() => {
    if (!floatingMoveState) {
      return;
    }

    const handleMove = (event: MouseEvent) => {
      const layout = floatingLayouts[floatingMoveState.panelKey];
      const viewportPadding = 12;
      const nextLeft = clamp(
        floatingMoveState.startLeft + (event.clientX - floatingMoveState.startX),
        viewportPadding,
        window.innerWidth - layout.width - viewportPadding,
      );
      const nextTop = clamp(
        floatingMoveState.startTop + (event.clientY - floatingMoveState.startY),
        viewportPadding,
        window.innerHeight - layout.height - viewportPadding,
      );

      setFloatingLayouts((current) => ({
        ...current,
        [floatingMoveState.panelKey]: {
          ...current[floatingMoveState.panelKey],
          left: nextLeft,
          top: nextTop,
        },
      }));
    };

    const handleUp = () => {
      setFloatingMoveState(null);
    };

    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);

    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [floatingLayouts, floatingMoveState]);

  useEffect(() => {
    if (!floatingResizeState) {
      return;
    }

    const handleMove = (event: MouseEvent) => {
      const currentLayout = floatingLayouts[floatingResizeState.panelKey];
      const viewportPadding = 12;
      const nextWidth = clamp(
        floatingResizeState.startWidth + (event.clientX - floatingResizeState.startX),
        panelMinWidths[floatingResizeState.panelKey],
        window.innerWidth - currentLayout.left - viewportPadding,
      );
      const nextHeight = clamp(
        floatingResizeState.startHeight + (event.clientY - floatingResizeState.startY),
        panelMinHeights[floatingResizeState.panelKey],
        window.innerHeight - currentLayout.top - viewportPadding,
      );

      setFloatingLayouts((current) => ({
        ...current,
        [floatingResizeState.panelKey]: {
          ...current[floatingResizeState.panelKey],
          width: nextWidth,
          height: nextHeight,
        },
      }));
    };

    const handleUp = () => {
      setFloatingResizeState(null);
    };

    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);

    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [floatingLayouts, floatingResizeState]);

  useEffect(() => {
    setAutocompleteState(null);
  }, [problemId, selectedDbms]);

  const updateAutocompleteState = (nextSql: string, caretIndex: number) => {
    const tokenRange = getAutocompleteTokenRange(nextSql, caretIndex);
    if (!tokenRange) {
      setAutocompleteState(null);
      return;
    }

    const suggestions = createAutocompleteSuggestions(autocompleteItems, tokenRange.typedToken);
    if (suggestions.length === 0) {
      setAutocompleteState(null);
      return;
    }

    if (
      tokenRange.currentToken.toLowerCase() === tokenRange.typedToken.toLowerCase() &&
      suggestions.some((item) => item.value.toLowerCase() === tokenRange.currentToken.toLowerCase())
    ) {
      setAutocompleteState(null);
      return;
    }

    const autocompleteAnchor = sqlEditorRef.current
        ? measureAutocompleteAnchor(sqlEditorRef.current, nextSql, caretIndex, suggestions.length)
        : {
            left: 12,
            top: 12,
            maxWidth: 320,
            maxHeight: 180,
          };

    if (autocompleteAnchor.maxHeight < 40) {
      setAutocompleteState(null);
      return;
    }

    setAutocompleteState(() => ({
      items: suggestions,
      selectedIndex: 0,
      tokenStart: tokenRange.tokenStart,
        tokenEnd: tokenRange.tokenEnd,
        left: autocompleteAnchor.left,
        top: autocompleteAnchor.top,
        maxWidth: autocompleteAnchor.maxWidth,
        maxHeight: autocompleteAnchor.maxHeight,
      }));
  };

  const refreshAutocompleteFromEditor = () => {
    if (!sqlEditorRef.current) {
      return;
    }

    updateAutocompleteState(sqlEditorRef.current.value, sqlEditorRef.current.selectionStart ?? sqlEditorRef.current.value.length);
  };

  const applyAutocompleteItem = (item: SqlAutocompleteItem) => {
    if (!autocompleteState) {
      return;
    }

    const nextSql = `${sql.slice(0, autocompleteState.tokenStart)}${item.value}${sql.slice(autocompleteState.tokenEnd)}`;
    const nextCaretIndex = autocompleteState.tokenStart + item.value.length;

    setSql(nextSql);
    setAutocompleteState(null);

    requestAnimationFrame(() => {
      if (!sqlEditorRef.current) {
        return;
      }

      sqlEditorRef.current.focus();
      sqlEditorRef.current.setSelectionRange(nextCaretIndex, nextCaretIndex);
    });
  };

  const handleEditorChange = (nextSql: string, caretIndex: number) => {
    setSql(nextSql);
    updateAutocompleteState(nextSql, caretIndex);
  };

  const changeSqlEditorFontSize = (delta: number) => {
    setSqlEditorFontSize((current) => clamp(current + delta, SQL_EDITOR_MIN_FONT_SIZE, SQL_EDITOR_MAX_FONT_SIZE));
  };

  const handleSubmit = () => {
    if (!isAuthenticated) {
      setSubmitMessage('로그인 후 제출할 수 있다.');
      setCollapsedCards((current) => ({
        ...current,
        submit: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        submit: true,
      }));
      return;
    }

    if (selectedDbms !== 'postgresql') {
      setSubmitMessage('제출은 PostgreSQL만 지원한다.');
      setCollapsedCards((current) => ({
        ...current,
        submit: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        submit: true,
      }));
      return;
    }

    if (sql.trim().length === 0) {
      setSubmitMessage('제출할 SQL을 입력해야 한다.');
      setCollapsedCards((current) => ({
        ...current,
        submit: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        submit: true,
      }));
      return;
    }

    if (detachedPanels.editor) {
      setDetachedPanels((current) => ({
        ...current,
        editor: false,
        submit: false,
      }));
      setExternalWindowPanels((current) => ({
        ...current,
        editor: false,
        submit: false,
      }));
    }

    setIsSubmitting(true);
    setSubmitMessage('제출 중이다.');
    setCollapsedCards((current) => ({
      ...current,
      submit: false,
    }));
    setPanelVisibility((current) => ({
      ...current,
      submit: true,
    }));
    focusPanelSection(() => submitPanelRef.current);

    void sendSessionSocketMessage({
      type: 'problem.submit',
      problemId,
      sql,
      dbms: selectedDbms,
    }).catch((error) => {
      setIsSubmitting(false);
      setSubmitMessage(error instanceof SessionSocketError ? error.message : '제출 연결에 실패했다.');
    });
  };

  const executeSql = async () => {
    if (!isAuthenticated) {
      setExecutionResult(createProblemExecutionError('로그인 후 문제를 실행할 수 있다.'));
      setCollapsedCards((current) => ({
        ...current,
        execute: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));
      if (!detachedPanels.editor) {
        focusPanelSection(() => executionPanelRef.current);
      }
      return;
    }

    if (selectedDbms !== 'postgresql') {
      setExecutionResult(createProblemExecutionError('인터랙티브 실행은 PostgreSQL만 지원한다.'));
      setCollapsedCards((current) => ({
        ...current,
        execute: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));
      if (!detachedPanels.editor) {
        focusPanelSection(() => executionPanelRef.current);
      }
      return;
    }

    if (sql.trim().length === 0) {
      setExecutionResult(createProblemExecutionError('실행할 SQL을 입력해야 한다.'));
      setCollapsedCards((current) => ({
        ...current,
        execute: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));
      if (!detachedPanels.editor) {
        focusPanelSection(() => executionPanelRef.current);
      }
      return;
    }

    try {
      setIsExecuting(true);
      setExecutionResult(null);
      setCollapsedCards((current) => ({
        ...current,
        execute: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));
      if (!detachedPanels.editor) {
        focusPanelSection(() => executionPanelRef.current);
      }
      await sendSessionSocketMessage({
        type: 'problem.execute',
        problemId,
        sql,
        dbms: selectedDbms,
      });
    } catch (error) {
      setIsExecuting(false);
      setExecutionResult(
        createProblemExecutionError(
          error instanceof SessionSocketError ? error.message : '문제 실행 연결에 실패했다.'
        )
      );
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));
      if (!detachedPanels.editor) {
        focusPanelSection(() => executionPanelRef.current);
      }
    }
  };

  const handleEditorKeyDown = (event: ReactKeyboardEvent<HTMLTextAreaElement>) => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      setAutocompleteState(null);
      void executeSql();
      return;
    }

    if (!autocompleteState && event.key === 'Tab') {
      event.preventDefault();

      const selectionStart = event.currentTarget.selectionStart ?? 0;
      const selectionEnd = event.currentTarget.selectionEnd ?? selectionStart;
      const { nextSql, nextSelectionStart, nextSelectionEnd } = indentSqlEditorValue(sql, selectionStart, selectionEnd);

      setSql(nextSql);
      setAutocompleteState(null);

      requestAnimationFrame(() => {
        if (!sqlEditorRef.current) {
          return;
        }

        sqlEditorRef.current.focus();
        sqlEditorRef.current.setSelectionRange(nextSelectionStart, nextSelectionEnd);
      });
      return;
    }

    if (!autocompleteState) {
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setAutocompleteState((current) =>
        current == null
          ? current
          : {
              ...current,
              selectedIndex: (current.selectedIndex + 1) % current.items.length,
            },
      );
      return;
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      setAutocompleteState((current) =>
        current == null
          ? current
          : {
              ...current,
              selectedIndex: (current.selectedIndex - 1 + current.items.length) % current.items.length,
            },
      );
      return;
    }

    if (event.key === 'Tab' || event.key === 'Enter') {
      event.preventDefault();
      applyAutocompleteItem(autocompleteState.items[autocompleteState.selectedIndex]);
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      setAutocompleteState(null);
    }
  };

  const handleEditorWheel = (event: ReactWheelEvent<HTMLTextAreaElement>) => {
    if (!event.ctrlKey) {
      return;
    }

    event.preventDefault();
    changeSqlEditorFontSize(event.deltaY < 0 ? 1 : -1);
  };

  const togglePanelVisibility = (panelKey: PanelKey) => {
    if (panelVisibility[panelKey] && (detachedPanels[panelKey] || externalWindowPanels[panelKey])) {
      setDetachedPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
      setExternalWindowPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        [panelKey]: true,
      }));
      return;
    }

    const nextVisible = !panelVisibility[panelKey];

    if (!nextVisible && detachedPanels[panelKey]) {
      setDetachedPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
    }

    if (!nextVisible && externalWindowPanels[panelKey]) {
      setExternalWindowPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
    }

    setPanelVisibility((current) => ({
      ...current,
      [panelKey]: nextVisible,
    }));
  };

  const togglePanelDetach = (panelKey: PanelKey) => {
    if (!panelVisibility[panelKey]) {
      setPanelVisibility((current) => ({
        ...current,
        [panelKey]: true,
      }));
    }

    if (externalWindowPanels[panelKey]) {
      setExternalWindowPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
    }

    setDetachedPanels((current) => ({
      ...current,
      [panelKey]: !current[panelKey],
    }));
  };

  const togglePanelExternalWindow = (panelKey: PanelKey) => {
    if (!panelVisibility[panelKey]) {
      setPanelVisibility((current) => ({
        ...current,
        [panelKey]: true,
      }));
    }

    if (detachedPanels[panelKey]) {
      setDetachedPanels((current) => ({
        ...current,
        [panelKey]: false,
      }));
    }

    setExternalWindowPanels((current) => ({
      ...current,
      [panelKey]: !current[panelKey],
    }));
  };

  const startFloatingMove = (panelKey: PanelKey, event: ReactMouseEvent<HTMLElement>) => {
    if (event.button !== 0) {
      return;
    }

    const target = event.target as HTMLElement;
    if (target.closest('button, input, label')) {
      return;
    }

    event.preventDefault();
    setFloatingMoveState({
      panelKey,
      startX: event.clientX,
      startY: event.clientY,
      startLeft: floatingLayouts[panelKey].left,
      startTop: floatingLayouts[panelKey].top,
    });
  };

  const startFloatingResize = (panelKey: PanelKey, event: ReactMouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    setFloatingResizeState({
      panelKey,
      startX: event.clientX,
      startY: event.clientY,
      startWidth: floatingLayouts[panelKey].width,
      startHeight: floatingLayouts[panelKey].height,
    });
  };

  const renderPanelActions = (panelKey: PanelKey) => (
    <div className="solve-pane-actions">
      <button
        type="button"
        className={`mini-toggle solve-pane-action solve-pane-action-icon ${externalWindowPanels[panelKey] ? 'is-selected' : ''}`}
        aria-label={externalWindowPanels[panelKey] ? `Restore ${panelLabels[panelKey]} from external window` : `Open ${panelLabels[panelKey]} in external window`}
        onClick={() => togglePanelExternalWindow(panelKey)}
      >
        <ExternalWindowIcon />
      </button>
      <button
        type="button"
        className={`mini-toggle solve-pane-action solve-pane-action-icon ${detachedPanels[panelKey] ? 'is-selected' : ''}`}
        aria-label={detachedPanels[panelKey] ? `Restore ${panelLabels[panelKey]} from PIP` : `Open ${panelLabels[panelKey]} in PIP`}
        onClick={() => togglePanelDetach(panelKey)}
      >
        <PipIcon />
      </button>
      <button
        type="button"
        className="mini-toggle solve-pane-action solve-pane-action-icon"
        aria-label={`Close ${panelLabels[panelKey]}`}
        onClick={() => togglePanelVisibility(panelKey)}
      >
        <CloseIcon />
      </button>
    </div>
  );

  const renderFloatingOpacityControl = () => {
    const sliderValue = Math.round((1 - editorFloatingOpacity / FLOATING_EDITOR_BACKGROUND_MAX_ALPHA) * 100);

    return (
      <label className="solve-floating-opacity-control" aria-label="에디터 투명도 조절">
        <span className="solve-floating-opacity-icon" aria-hidden="true">
          <OpacityIcon />
        </span>
        <input
          type="range"
          className="solve-floating-opacity-slider"
          min={0}
          max={100}
          step={1}
          value={sliderValue}
          onChange={(event) => {
            const nextValue = Number(event.target.value);
            setEditorFloatingOpacity(FLOATING_EDITOR_BACKGROUND_MAX_ALPHA * (1 - nextValue / 100));
          }}
        />
      </label>
    );
  };

  const renderPanelHeader = (panelKey: PanelKey, isFloating: boolean) => (
    <div
      className={`solve-pane-header solve-detail-section-header ${isFloating ? 'is-draggable' : ''}`}
      onMouseDown={isFloating ? (event) => startFloatingMove(panelKey, event) : undefined}
    >
      <div className="solve-detail-section-title-row">
        <h2 className="solve-detail-section-title solve-pane-title">{panelLabels[panelKey]}</h2>
        {isFloating && panelKey === 'editor' ? renderFloatingOpacityControl() : null}
        {renderPanelActions(panelKey)}
      </div>
    </div>
  );

  const toggleCardCollapse = (cardKey: keyof CollapsedCardState) => {
    setCollapsedCards((current) => ({
      ...current,
      [cardKey]: !current[cardKey],
    }));
  };

  const renderCollapseControl = (cardKey: keyof CollapsedCardState) => (
    <div className="solve-detail-section-rail solve-pane-section-rail">
      <button
        type="button"
        className="solve-detail-section-divider-button solve-pane-section-divider-button"
        aria-label={collapsedCards[cardKey] ? '펼치기' : '접기'}
        aria-expanded={!collapsedCards[cardKey]}
        onClick={() => toggleCardCollapse(cardKey)}
      >
        <CollapseChevronIcon collapsed={collapsedCards[cardKey]} />
      </button>
      {!collapsedCards[cardKey] ? <span className="solve-detail-section-rail-line" aria-hidden="true" /> : null}
    </div>
  );

  const renderExecutionInlineRegion = () => {
    if (!isExecuting && !executionResult) {
      return null;
    }

    const shouldShowRefresh = executionResult?.mode === 'select' && !collapsedCards.execute;

    return (
      <div ref={executionPanelRef} tabIndex={-1} className={`solve-editor-inline-result ${collapsedCards.execute ? 'is-collapsed' : ''}`}>
        <div className="solve-editor-inline-result-divider" aria-hidden="true" />
        <div className="solve-editor-inline-result-header">
          <button
            type="button"
            className="solve-detail-section-divider-button solve-pane-section-divider-button"
            aria-label={collapsedCards.execute ? '펼치기' : '접기'}
            aria-expanded={!collapsedCards.execute}
            onClick={() => toggleCardCollapse('execute')}
          >
            <CollapseChevronIcon collapsed={collapsedCards.execute} />
          </button>
          <div className="solve-pane-summary-row">
            {isExecuting ? (
              <span className="solve-pane-summary-item is-pending">SQL 실행 중</span>
            ) : executionResult ? (
              <>
                <span className="solve-pane-summary-item">
                  <span className="solve-pane-summary-label">경과 시간</span>
                  <strong className="solve-pane-summary-value">{formatExecutionMetricValue(executionResult.executionTimeMs)}</strong>
                </span>
                <span className="solve-pane-summary-item">
                  <span className="solve-pane-summary-label">결과 Rows</span>
                  <strong className="solve-pane-summary-value">{formatGroupedNumber(executionResult.rowCount)}</strong>
                </span>
              </>
            ) : null}
          </div>
          <div className="solve-detail-section-header-actions">
            {shouldShowRefresh ? (
              <button
                type="button"
                className="solve-pane-action solve-pane-action-icon"
                aria-label="실행 결과 너비 초기화"
                onClick={() => setExecutionResultGridResetKey((current) => current + 1)}
              >
                <RefreshIcon />
              </button>
            ) : null}
          </div>
        </div>

        {!collapsedCards.execute ? (
          <div className="solve-editor-inline-result-body solve-pane-result-stack">
            {isExecuting ? (
              <div className="solve-result-empty solve-result-empty-table">SQL을 실행하는 중이다.</div>
            ) : executionResult ? (
              <>
                {executionResult.message && shouldRenderExecutionMessage(executionResult.message) ? (
                  <p className="solve-pane-result-message">{executionResult.message}</p>
                ) : null}
                {renderExecutionContent(
                  executionResult,
                  executionResultPage,
                  setExecutionResultPage,
                  executionResultPageInput,
                  setExecutionResultPageInput,
                  collapsedCards.resultTable,
                  () =>
                    setCollapsedCards((current) => ({
                      ...current,
                      resultTable: !current.resultTable,
                    })),
                  executionResultGridResetKey,
                )}
              </>
            ) : null}
          </div>
        ) : null}
      </div>
    );
  };

  const renderEditorPanel = (isFloating: boolean) => (
    <section className={`${isFloating ? 'panel-card' : 'solve-surface-section'} solve-pane solve-pane-editor ${isFloating ? 'is-floating' : ''}`}>
      <div className={`solve-detail-section-frame solve-pane-section-frame ${collapsedCards.editor ? 'is-collapsed' : ''} ${isFloating ? 'is-floating' : ''}`.trim()}>
        {renderCollapseControl('editor')}
        <div className="solve-detail-section-main solve-pane-section-main">
          {renderPanelHeader('editor', isFloating)}

          {!collapsedCards.editor ? (
            <div className="solve-editor-stack">
              <div className="solve-editor-surface">
                <div className="solve-editor-surface-header">
                  <div className="solve-editor-surface-meta">
                    <span className="solve-editor-file">{getDbmsLabel(selectedDbms)}</span>
                  </div>
                  <div className="solve-editor-actions">
                    <button type="button" className="btn secondary" onClick={executeSql} disabled={sql.trim().length === 0 || isExecuting}>
                      {isExecuting ? '실행 중' : '실행 (Ctrl + Enter)'}
                    </button>
                    <button type="button" className="btn primary" onClick={handleSubmit} disabled={sql.trim().length === 0 || isSubmitting}>
                      {isSubmitting ? '제출 중' : '제출'}
                    </button>
                  </div>
                </div>

                <div className="solve-editor-surface-body">
                  <div className="solve-editor-zoom-controls" aria-label="에디터 글씨 크기 조절">
                    <button type="button" className="mini-toggle solve-editor-zoom-button" onClick={() => changeSqlEditorFontSize(1)}>
                      +
                    </button>
                    <button type="button" className="mini-toggle solve-editor-zoom-button" onClick={() => changeSqlEditorFontSize(-1)}>
                      -
                    </button>
                  </div>

                  <textarea
                    ref={sqlEditorRef}
                    className="solve-sql-editor"
                    spellCheck={false}
                    style={{ fontSize: `${sqlEditorFontSize}px` }}
                    value={sql}
                    onChange={(event) => handleEditorChange(event.target.value, event.target.selectionStart ?? event.target.value.length)}
                    onClick={refreshAutocompleteFromEditor}
                    onScroll={refreshAutocompleteFromEditor}
                    onWheel={handleEditorWheel}
                    onKeyUp={(event) => {
                      if (['ArrowUp', 'ArrowDown', 'Enter', 'Tab', 'Escape'].includes(event.key)) {
                        return;
                      }

                      refreshAutocompleteFromEditor();
                    }}
                    onKeyDown={handleEditorKeyDown}
                    onBlur={() => {
                      window.setTimeout(() => {
                        setAutocompleteState(null);
                      }, 120);
                    }}
                    aria-label="에디터"
                  />

                  {autocompleteState
                    ? createPortal(
                        <div
                          className="solve-editor-autocomplete"
                          style={{
                            left: `${autocompleteState.left}px`,
                            top: `${autocompleteState.top}px`,
                            maxWidth: `${autocompleteState.maxWidth}px`,
                            maxHeight: `${autocompleteState.maxHeight}px`,
                          }}
                          role="listbox"
                          aria-label="SQL 자동완성"
                        >
                          {autocompleteState.items.map((item, index) => (
                            <button
                              key={`${item.kind}-${item.value}-${item.detail ?? ''}`}
                              type="button"
                              className={`solve-editor-autocomplete-item ${index === autocompleteState.selectedIndex ? 'is-selected' : ''}`}
                              role="option"
                              aria-selected={index === autocompleteState.selectedIndex}
                              onMouseEnter={() =>
                                setAutocompleteState((current) =>
                                  current == null
                                    ? current
                                    : {
                                        ...current,
                                        selectedIndex: index,
                                      },
                                )
                              }
                              onMouseDown={(event) => {
                                event.preventDefault();
                                applyAutocompleteItem(item);
                              }}
                            >
                              <span className={`solve-editor-autocomplete-kind is-${item.kind}`}>
                                {item.kind === 'keyword' ? 'SQL' : item.kind === 'table' ? 'TABLE' : 'COLUMN'}
                              </span>
                              <span className="solve-editor-autocomplete-value">{item.value}</span>
                              {item.detail ? <span className="solve-editor-autocomplete-detail">{item.detail}</span> : null}
                            </button>
                          ))}
                        </div>,
                        document.body,
                      )
                    : null}
                </div>

                {renderExecutionInlineRegion()}
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  );

  const renderSubmitPanel = (isFloating: boolean) => (
    <section
      ref={isFloating ? undefined : submitPanelRef}
      tabIndex={isFloating ? undefined : -1}
      className={`${isFloating ? 'panel-card' : 'solve-surface-section'} solve-pane ${isFloating ? 'is-floating' : ''}`}
    >
      <div className={`solve-detail-section-frame solve-pane-section-frame ${collapsedCards.submit ? 'is-collapsed' : ''} ${isFloating ? 'is-floating' : ''}`.trim()}>
        {renderCollapseControl('submit')}
        <div className="solve-detail-section-main solve-pane-section-main">
          {renderPanelHeader('submit', isFloating)}
          {isFloating || !collapsedCards.submit ? (
            submitMessage ? (
              <div className="solve-pane-result-stack">
                <p className="solve-pane-result-message">{submitMessage}</p>
              </div>
            ) : (
              <div className="solve-result-empty">제출 결과 없음.</div>
            )
          ) : null}
        </div>
      </div>
    </section>
  );

  const renderPanel = (panelKey: PanelKey, isFloating: boolean): ReactNode => {
    if (panelKey === 'editor') {
      return renderEditorPanel(isFloating);
    }

    return renderSubmitPanel(isFloating);
  };

  if (!problemLoadError && problemDetail == null) {
    return (
      <div className="page-stack solve-page-loading-state">
        <section className="page-loading-shell" aria-label="Loading problem" aria-busy="true">
          <span className="page-loading-spinner" aria-hidden="true" />
        </section>
      </div>
    );
  }

  return (
    <div className="page-stack">
      <div className="solve-page-topbar solve-page-topbar-dbms">
        <div className="solve-dbms-tab-row" role="tablist" aria-label="DBMS 선택">
          {availableDbms.map((dbms) => (
            <button
              key={dbms}
              type="button"
              role="tab"
              aria-selected={selectedDbms === dbms}
              className={`solve-dbms-tab ${selectedDbms === dbms ? 'is-selected' : ''}`}
              onClick={() => setSelectedDbms(dbms)}
            >
              {getDbmsLabel(dbms)}
            </button>
          ))}
        </div>
      </div>

      <section className="solve-page-hero solve-surface-section">
        <div className="solve-page-hero-copy solve-page-hero-copy-wide">
          <div className="solve-title-row">
            <span className="solve-problem-number">{`문제 ${displayProblemNumber}`}</span>
            <h1 className="solve-problem-title">{displayProblemTitle}</h1>
          </div>

          {problemLoadError ? <p className="content-text solve-problem-description">{problemLoadError}</p> : null}
        </div>
        {problemDetail ? <ProblemDetailContent detail={problemDetail} selectedDbms={selectedDbms} /> : null}
      </section>

      {panelVisibility.editor && !detachedPanels.editor && !externalWindowPanels.editor ? renderEditorPanel(false) : null}

      {panelVisibility.submit && !detachedPanels.submit && !externalWindowPanels.submit ? renderSubmitPanel(false) : null}

      {visibleFloatingPanels.map((panelKey) => (
        <div
          key={panelKey}
          className={`solve-floating-pane-shell is-${panelKey}`}
          style={{
            left: `${floatingLayouts[panelKey].left}px`,
            top: `${floatingLayouts[panelKey].top}px`,
            width: `${floatingLayouts[panelKey].width}px`,
            height: `${floatingLayouts[panelKey].height}px`,
            '--solve-floating-surface-alpha': panelKey === 'editor' ? String(editorFloatingOpacity) : '0.94',
            '--solve-floating-inner-surface-alpha': panelKey === 'editor' ? String(editorFloatingOpacity) : '0.9',
            '--solve-floating-border-alpha':
              panelKey === 'editor'
                ? String(Math.max(0.12, 0.54 * (editorFloatingOpacity / FLOATING_EDITOR_BACKGROUND_MAX_ALPHA)))
                : '0.48',
            '--solve-floating-shadow-alpha':
              panelKey === 'editor'
                ? String(Math.max(0.08, 0.22 * (editorFloatingOpacity / FLOATING_EDITOR_BACKGROUND_MAX_ALPHA)))
                : '0.18',
          } as any}
        >
          <div className="solve-floating-pane-content">
            {renderPanel(panelKey, true)}
          </div>
          <button
            type="button"
            className="solve-floating-pane-resize"
            aria-label={`${panelLabels[panelKey]} 크기 조절`}
            onMouseDown={(event) => startFloatingResize(panelKey, event)}
          >
            <span aria-hidden="true" />
          </button>
        </div>
      ))}

      {visibleExternalWindows.map((panelKey) => (
        <PanelExternalWindow
          key={`external-${panelKey}`}
          panelKey={panelKey}
          title={`Quertimizer - ${panelLabels[panelKey]}`}
          layout={floatingLayouts[panelKey]}
          onClose={() =>
            setExternalWindowPanels((current) => ({
              ...current,
              [panelKey]: false,
            }))
          }
        >
          <div className="solve-external-window-root-inner">{renderPanel(panelKey, false)}</div>
        </PanelExternalWindow>
      ))}
    </div>
  );
}

