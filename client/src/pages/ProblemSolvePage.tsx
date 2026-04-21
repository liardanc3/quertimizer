import { useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore, type KeyboardEvent as ReactKeyboardEvent, type MouseEvent as ReactMouseEvent, type ReactNode, type WheelEvent as ReactWheelEvent } from 'react';
import { createPortal } from 'react-dom';
import type { FormEvent } from 'react';
import { Fragment } from 'react';
import './ProblemSolvePage.css';
import './PublicHomePage.css';
import './SubmitHistoryPage.css';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import HandleSetupGate from '../components/home/HandleSetupGate';
import ProblemDetailContent from '../components/problem/ProblemDetailContent';
import { fetchProblemDetail, type ProblemDetailData } from '../lib/problemApi';
import { fetchSubmitHistories } from '../lib/submitHistoryApi';
import { fetchCommunityPosts, type CommunityPostPage } from '../lib/communityApi';
import {
  AuthApiError,
  RecoveryApiError,
  SignupApiError,
  checkDuplicateEmail,
  fetchSessionMe,
  getApiBaseUrl,
  login,
  resetPassword,
  sendPasswordResetCode,
  signup,
  verifyPasswordResetCode,
} from '../lib/authApi';
import { completeAuthentication } from '../lib/authSession';
import {
  SessionSocketError,
  sendSessionSocketMessage,
  sendSessionSocketMessageIfOpen,
  subscribeSessionSocketMessages,
  type SessionSocketMessage,
} from '../lib/sessionSocket';
import { syncSession, useMockSession } from '../lib/session';
import { getCommunityPostPath, getLocationSearchSnapshot, getProfilePath, navigate, subscribeLocation } from '../lib/navigation';
import { mockProblemDetailById, mockProblemDetails } from '../mocks/problemDetail';
import { getExecutionPlanDetailGroups } from '../lib/executionPlanFilters';
import type { CommunityPostSummary, DbmsType, ProblemDetail, SubmitHistoryEntry, SubmitHistoryPageData, SubmitHistoryPlanFilters } from '../types/domain';
import logoImage from '../assets/logo.png';

interface ProblemSolvePageProps {
  problemId: string;
}

type SolveContentTab = 'problem' | 'submissions' | 'community';

interface ProblemSolveFavoriteSnapshot {
  selectedDbms: DbmsType;
  contentTab: SolveContentTab;
  sql: string;
  editorSelection: SqlEditorSelection | null;
  mySubmitRequestedPage: number;
  taggedPostRequestedPage: number;
}

function readSolveContentTabFromSearch(search: string): SolveContentTab {
  const tab = new URLSearchParams(search).get('tab');

  if (tab === 'submissions' || tab === 'community') {
    return tab;
  }

  return 'problem';
}

function buildSolveContentTabPath(problemId: string, tab: SolveContentTab) {
  const encodedProblemId = encodeURIComponent(problemId);

  if (tab === 'problem') {
    return `/problems/${encodedProblemId}`;
  }

  return `/problems/${encodedProblemId}?tab=${encodeURIComponent(tab)}`;
}

function getSolveContentTabLabel(tab: SolveContentTab) {
  if (tab === 'submissions') {
    return '내 제출 목록';
  }

  if (tab === 'community') {
    return '태그된 게시글';
  }

  return '제출';
}

type SolveRelatedModalState =
  | { type: 'sql'; history: SubmitHistoryEntry }
  | { type: 'plan'; history: SubmitHistoryEntry }
  | null;

const solveRelatedCostFormatter = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 1 });

function createEmptySolveSubmitHistoryPage(): SubmitHistoryPageData {
  return {
    currentPage: 1,
    pageSize: 30,
    totalCount: 0,
    totalPages: 1,
    problemIds: [],
    histories: [],
  };
}

function createEmptySolveCommunityPage(): CommunityPostPage {
  return {
    currentPage: 1,
    pageSize: 10,
    totalCount: 0,
    totalPages: 1,
    posts: [],
  };
}

function createEmptySolvePlanFilters(): SubmitHistoryPlanFilters {
  return {
    matchMode: 'or',
    scanBuckets: [],
    joinBuckets: [],
    filterBuckets: [],
    sortBuckets: [],
    aggregateBuckets: [],
    hintFilters: [],
  };
}

function createEmptySolvePlanFiltersByDbms(): Record<DbmsType, SubmitHistoryPlanFilters> {
  return {
    postgresql: createEmptySolvePlanFilters(),
    oracle: createEmptySolvePlanFilters(),
  };
}

function formatSolveRelatedCost(value: number) {
  return solveRelatedCostFormatter.format(Math.round(value * 10) / 10);
}

function padSolveRelatedDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function formatSolveRelatedSubmittedAt(value: string) {
  if (value.trim() === '') {
    return '-';
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return `${parsedDate.getFullYear()}-${padSolveRelatedDatePart(parsedDate.getMonth() + 1)}-${padSolveRelatedDatePart(parsedDate.getDate())} ${padSolveRelatedDatePart(parsedDate.getHours())}:${padSolveRelatedDatePart(parsedDate.getMinutes())}:${padSolveRelatedDatePart(parsedDate.getSeconds())}`;
}

function formatSolveRelatedBoardDate(value: string) {
  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return '-';
  }

  const year = String(parsedDate.getFullYear()).slice(-2);
  const month = padSolveRelatedDatePart(parsedDate.getMonth() + 1);
  const day = padSolveRelatedDatePart(parsedDate.getDate());
  const hours = padSolveRelatedDatePart(parsedDate.getHours());
  const minutes = padSolveRelatedDatePart(parsedDate.getMinutes());
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function getSolveRelatedCommunityCategoryLabel(value: CommunityPostSummary['category']) {
  if (value === 'question') {
    return '질문';
  }

  if (value === 'notice') {
    return '공지';
  }

  return '자유';
}

function getSolveRelatedCommunitySearchTerm(problemId: string) {
  return /^[PO]\d{5}-\d{5}$/.test(problemId) ? problemId.slice(1) : problemId;
}

type SolveAuthOverlayMode = 'login' | 'signup' | 'reset-password';
type SolveAuthSocialProvider = 'google' | 'github' | 'kakao';

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

type FloatingResizeDirection = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw';

interface FloatingResizeState {
  panelKey: PanelKey;
  direction: FloatingResizeDirection;
  startX: number;
  startY: number;
  startLeft: number;
  startTop: number;
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
}

type ProblemExecutionMode = 'select' | 'explain' | 'explain_analyze' | 'command';
const EXECUTION_RESULT_PAGE_SIZE = 10;
const SUBMIT_REFERENCE_WRITE_CTE_PATTERN = /\bWITH\b[\s\S]*\b(INSERT|UPDATE|DELETE|MERGE)\b/i;

interface ProblemExecutionResult {
  success: boolean;
  mode: ProblemExecutionMode;
  message: string;
  columns: string[];
  rows: string[][];
  planLines: string[];
  rowCount: number;
  currentPage?: number;
  pageSize?: number;
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
  currentPage?: number | null;
  pageSize?: number | null;
  executionTimeMs?: number | null;
}

type ProblemSubmitStepStatus = 'running' | 'success' | 'incorrect' | 'error';
type StatementPickerMode = 'execute' | 'submit';

interface ProblemSubmitProgressMessage extends SessionSocketMessage {
  problemId?: string | null;
  stepKey?: string | null;
  status?: ProblemSubmitStepStatus | null;
  message?: string | null;
  detailLines?: string[] | null;
  statementKey?: string | null;
  statementSql?: string | null;
  statementIndex?: number | null;
  statementMode?: ProblemExecutionMode | null;
  statementReference?: boolean | null;
}

interface ProblemSubmitProgressStep {
  stepKey: string;
  status: ProblemSubmitStepStatus;
  message: string;
  detailLines: string[];
}

type SqlAutocompleteKind = 'keyword' | 'table' | 'column';

interface SqlAutocompleteItem {
  value: string;
  kind: SqlAutocompleteKind;
  detail?: string;
}

type SqlExecutionPickerOptionKind = 'statement' | 'all';

interface SqlStatementSegment {
  sql: string;
  start: number;
  end: number;
  preview: string;
}

interface SqlExecutionPickerOption {
  key: string;
  kind: SqlExecutionPickerOptionKind;
  label: string;
  preview: string;
  start: number;
  end: number;
  segments: SqlStatementSegment[];
}

interface SqlExecutionPickerState {
  mode: StatementPickerMode;
  options: SqlExecutionPickerOption[];
  selectedIndex: number;
  selectionStart: number;
  selectionEnd: number;
  left: number;
  top: number;
  maxWidth: number;
  maxHeight: number;
}

interface SqlHighlightRange {
  start: number;
  end: number;
}

interface SqlEditorSelection {
  start: number;
  end: number;
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

interface ExecutionStatementMarkerLayout {
  left: number;
  width: number;
  height: number;
  fontSize: number;
  topOffsets: Record<number, number>;
}

type SqlHighlightKind =
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

interface SqlHighlightToken {
  text: string;
  kind: SqlHighlightKind | null;
}

type ExecutionStatementStatus = 'idle' | 'running' | 'success' | 'error';

interface ExecutionStatementRun {
  key: string;
  sql: string;
  preview: string;
  start: number;
  end: number;
  status: ExecutionStatementStatus;
  result: ProblemExecutionResult | null;
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

const SOLVE_PAGE_AUTH_RETURN_STORAGE_KEY = 'quertimizer.solve-auth-return';
const PASSWORD_RESET_CODE_PATTERN = /^[A-Z0-9]{6}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SIGNUP_EMAIL_HINT = '올바른 이메일 형식으로 입력해 주세요.';
const SIGNUP_EMAIL_CHECKING_MESSAGE = '이메일 사용 가능 여부를 확인하는 중입니다.';
const SIGNUP_EMAIL_AVAILABLE_MESSAGE = '사용 가능한 이메일입니다.';
const SIGNUP_EMAIL_DUPLICATED_MESSAGE = '이미 사용 중인 이메일입니다.';
const SIGNUP_PASSWORD_HINT = '특수문자를 포함해 8자 이상 입력해 주세요.';
const SIGNUP_PASSWORD_CONFIRM_HINT = '비밀번호를 다시 입력해 주세요.';

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
  'EXPLAIN ANALYSE',
  'CREATE INDEX',
  'DROP INDEX',
];
const SQL_HIGHLIGHT_KEYWORDS = new Set([
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
const SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS = new Set([
  'FROM',
  'JOIN',
  'INTO',
  'UPDATE',
  'TABLE',
  'INDEX',
  'ON',
]);
const SQL_EDITOR_INDENT = '    ';
const SQL_EDITOR_MIN_HEIGHT = 256;
const SQL_EDITOR_DEFAULT_FONT_SIZE = 13.5;
const SQL_EDITOR_MIN_FONT_SIZE = 11;
const SQL_EDITOR_MAX_FONT_SIZE = 24;
const SQL_EDITOR_AUTOCOMPLETE_OVERFLOW_ITEM_COUNT = 4;
const FLOATING_EDITOR_BACKGROUND_MIN_ALPHA = 0.12;
const FLOATING_EDITOR_BACKGROUND_MAX_ALPHA = 1;
const SQL_EDITOR_CONTENT_LINE_HEIGHT_RATIO = 1.7;
const SQL_EDITOR_INLINE_PADDING_TOP_REM = 1.08;
const SQL_EDITOR_FLOATING_PADDING_TOP_REM = 1.34;
const SUBMIT_STEP_ORDER = ['validate', 'answer', 'ddl', 'plan'] as const;
const SUBMIT_INDEX_DDL_PATTERN = /^(CREATE|DROP|ALTER)\s+INDEX\b/i;

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

function getExecutionResultPageCount(rowCount: number) {
  return Math.max(1, Math.ceil(rowCount / EXECUTION_RESULT_PAGE_SIZE));
}

function resolveSubmitStatementMode(sql: string): ProblemExecutionMode {
  const normalizedSql = sql.trim().replace(/\s+/g, ' ').toUpperCase();

  if (normalizedSql.startsWith('SELECT ')) {
    return 'select';
  }

  if (normalizedSql.startsWith('WITH ') && !SUBMIT_REFERENCE_WRITE_CTE_PATTERN.test(normalizedSql)) {
    return 'select';
  }

  return 'command';
}

function toProblemSequence(problemId: string) {
  const [, problemSequence] = problemId.split('-');
  const parsedNumber = Number.parseInt(problemSequence ?? '', 10);

  return Number.isNaN(parsedNumber) ? 0 : parsedNumber;
}

function buildColumnTemplate(columnWidths: number[]) {
  return columnWidths.map((width) => `${width}px`).join(' ');
}

function resolveWheelScrollableElement(startTarget: EventTarget | null, boundaryElement: HTMLElement, deltaY: number) {
  let currentElement = startTarget instanceof HTMLElement ? startTarget : null;

  while (currentElement) {
    const computedStyle = window.getComputedStyle(currentElement);
    const canScrollY =
      /(auto|scroll|overlay)/.test(computedStyle.overflowY) &&
      currentElement.scrollHeight > currentElement.clientHeight + 1;

    if (canScrollY) {
      const maxScrollTop = currentElement.scrollHeight - currentElement.clientHeight;
      const canScrollFurther =
        deltaY < 0 ? currentElement.scrollTop > 0 : deltaY > 0 ? currentElement.scrollTop < maxScrollTop : false;

      if (canScrollFurther || currentElement === boundaryElement) {
        return currentElement;
      }
    }

    if (currentElement === boundaryElement) {
      break;
    }

    currentElement = currentElement.parentElement;
  }

  return null;
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
  const scopedDbms = problemId.startsWith('O') ? 'oracle' : problemId.startsWith('P') ? 'postgresql' : null;
  const matchedProblem = mockProblemDetailById[problemId];

  if (matchedProblem) {
    return {
      ...matchedProblem,
      problemNumber: matchedProblem.problemNumber ?? problemId,
      dbmsOptions: scopedDbms ? [scopedDbms] : matchedProblem.dbmsOptions,
      disabledDbms: scopedDbms ? (scopedDbms === 'postgresql' ? ['oracle'] : ['postgresql']) : matchedProblem.disabledDbms,
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
    dbmsOptions: scopedDbms ? [scopedDbms] : mockProblemDetails[0].dbmsOptions,
    disabledDbms: scopedDbms ? (scopedDbms === 'postgresql' ? ['oracle'] : ['postgresql']) : mockProblemDetails[0].disabledDbms,
  };
}

function createInitialFloatingLayouts(): FloatingPanelLayoutState {
  const viewportWidth = typeof window !== 'undefined' ? window.innerWidth : 1440;
  const viewportHeight = typeof window !== 'undefined' ? window.innerHeight : 900;
  const editorWidth = 760;
  const editorHeight = 620;
  const submitWidth = 400;
  const submitHeight = 330;
  const viewportPadding = 24;

  return {
    editor: {
      left: Math.max(viewportPadding, viewportWidth - editorWidth - viewportPadding),
      top: Math.max(86, viewportHeight - editorHeight - viewportPadding - 56),
      width: editorWidth,
      height: editorHeight,
    },
    submit: {
      left: Math.max(viewportPadding, viewportWidth - submitWidth - viewportPadding),
      top: Math.max(180, viewportHeight - submitHeight - viewportPadding),
      width: submitWidth,
      height: submitHeight,
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

function hasRequiredPasswordFormat(value: string) {
  return value.length >= 8 && /[^A-Za-z0-9]/.test(value);
}

function sanitizeVerificationCode(value: string) {
  return value.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, 6);
}

function isAuthenticationRequiredMessage(message: string | null | undefined) {
  if (!message) {
    return false;
  }

  return message.includes('로그인 후') || message.includes('인증') || message.includes('세션');
}

function getSolveAuthSocialLoginErrorMessage(provider: SolveAuthSocialProvider | 'oauth2' | null) {
  switch (provider) {
    case 'google':
      return 'Google 로그인에 실패했습니다.';
    case 'github':
      return 'Github 로그인에 실패했습니다.';
    case 'kakao':
      return 'Kakao 로그인에 실패했습니다.';
    default:
      return '소셜 로그인에 실패했습니다.';
  }
}

function saveSolvePageAuthReturn(problemId: string, sql: string, selectedDbms: DbmsType) {
  if (typeof window === 'undefined') {
    return;
  }

  window.sessionStorage.setItem(
    SOLVE_PAGE_AUTH_RETURN_STORAGE_KEY,
    JSON.stringify({
      problemId,
      path: window.location.pathname + window.location.hash,
      sql,
      selectedDbms,
    }),
  );
}

function consumeSolvePageAuthReturn(problemId: string) {
  if (typeof window === 'undefined') {
    return null;
  }

  const storedValue = window.sessionStorage.getItem(SOLVE_PAGE_AUTH_RETURN_STORAGE_KEY);
  if (!storedValue) {
    return null;
  }

  window.sessionStorage.removeItem(SOLVE_PAGE_AUTH_RETURN_STORAGE_KEY);

  try {
    const parsedValue = JSON.parse(storedValue) as {
      problemId?: string;
      path?: string;
      sql?: string;
      selectedDbms?: DbmsType;
    };

    if (parsedValue.problemId !== problemId) {
      return null;
    }

    return {
      path: typeof parsedValue.path === 'string' ? parsedValue.path : null,
      sql: typeof parsedValue.sql === 'string' ? parsedValue.sql : '',
      selectedDbms:
        parsedValue.selectedDbms === 'oracle' || parsedValue.selectedDbms === 'postgresql'
          ? parsedValue.selectedDbms
          : null,
    };
  } catch {
    return null;
  }
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

function createSqlStatementPreview(sql: string, maxLength = 60) {
  const collapsedSql = sql.replace(/\s+/g, ' ').trim();

  if (collapsedSql.length <= maxLength) {
    return collapsedSql;
  }

  return `${collapsedSql.slice(0, Math.max(0, maxLength - 3))}...`;
}

function createExecutionStatementRunKey(start: number, end: number) {
  return `${start}:${end}`;
}

function createExecutionStatementRuns(
  segments: SqlStatementSegment[],
  initiallyRunningIndex: number | null = null,
): ExecutionStatementRun[] {
  return segments.map((segment, index) => ({
    key: createExecutionStatementRunKey(segment.start, segment.end),
    sql: segment.sql,
    preview: segment.preview,
    start: segment.start,
    end: segment.end,
    status: initiallyRunningIndex === index ? 'running' : 'idle',
    result: null,
  }));
}

function createSingleExecutionStatementRun(sql: string, result: ProblemExecutionResult): ExecutionStatementRun {
  const normalizedSql = sql.trim();
  const preview = createSqlStatementPreview(normalizedSql === '' ? 'SQL' : normalizedSql);

  return {
    key: createExecutionStatementRunKey(0, Math.max(sql.length, 0)),
    sql,
    preview,
    start: 0,
    end: Math.max(sql.length, 0),
    status: result.success ? 'success' : 'error',
    result,
  };
}

function parseSqlStatements(value: string): SqlStatementSegment[] {
  const statements: SqlStatementSegment[] = [];
  let statementStart = 0;
  let inSingleQuote = false;
  let inDoubleQuote = false;
  let inLineComment = false;
  let inBlockComment = false;

  const pushStatement = (statementEnd: number) => {
    const rawStatement = value.slice(statementStart, statementEnd);
    const firstContentOffset = rawStatement.search(/\S/);

    if (firstContentOffset === -1) {
      statementStart = statementEnd;
      return;
    }

    const lastContentOffset = rawStatement.length - rawStatement.trimEnd().length;
    const start = statementStart + firstContentOffset;
    const end = statementEnd - lastContentOffset;
    const normalizedSql = value
      .slice(start, end)
      .trim()
      .replace(/;+\s*$/, '')
      .trim();

    if (normalizedSql.length > 0) {
      statements.push({
        sql: normalizedSql,
        start,
        end,
        preview: createSqlStatementPreview(normalizedSql),
      });
    }

    statementStart = statementEnd;
  };

  for (let index = 0; index < value.length; index += 1) {
    const currentChar = value[index];
    const nextChar = value[index + 1];

    if (inLineComment) {
      if (currentChar === '\n') {
        inLineComment = false;
      }
      continue;
    }

    if (inBlockComment) {
      if (currentChar === '*' && nextChar === '/') {
        inBlockComment = false;
        index += 1;
      }
      continue;
    }

    if (inSingleQuote) {
      if (currentChar === "'" && nextChar === "'") {
        index += 1;
        continue;
      }

      if (currentChar === "'") {
        inSingleQuote = false;
      }
      continue;
    }

    if (inDoubleQuote) {
      if (currentChar === '"' && nextChar === '"') {
        index += 1;
        continue;
      }

      if (currentChar === '"') {
        inDoubleQuote = false;
      }
      continue;
    }

    if (currentChar === '-' && nextChar === '-') {
      inLineComment = true;
      index += 1;
      continue;
    }

    if (currentChar === '/' && nextChar === '*') {
      inBlockComment = true;
      index += 1;
      continue;
    }

    if (currentChar === "'") {
      inSingleQuote = true;
      continue;
    }

    if (currentChar === '"') {
      inDoubleQuote = true;
      continue;
    }

    if (currentChar === ';') {
      pushStatement(index + 1);
    }
  }

  pushStatement(value.length);
  return statements;
}

function hasBlankLineBoundary(value: string, leftEnd: number, rightStart: number) {
  const betweenText = value.slice(leftEnd, rightStart);
  return /\n\s*\n/.test(betweenText);
}

function resolveLooseSqlStatementGroup(
  value: string,
  statements: SqlStatementSegment[],
  targetStatement: SqlStatementSegment,
) {
  const targetIndex = statements.findIndex(
    (statement) => statement.start === targetStatement.start && statement.end === targetStatement.end,
  );
  if (targetIndex === -1) {
    return [targetStatement];
  }

  let groupStartIndex = targetIndex;
  let groupEndIndex = targetIndex;

  while (
    groupStartIndex > 0 &&
    !hasBlankLineBoundary(value, statements[groupStartIndex - 1].end, statements[groupStartIndex].start)
  ) {
    groupStartIndex -= 1;
  }

  while (
    groupEndIndex < statements.length - 1 &&
    !hasBlankLineBoundary(value, statements[groupEndIndex].end, statements[groupEndIndex + 1].start)
  ) {
    groupEndIndex += 1;
  }

  return statements.slice(groupStartIndex, groupEndIndex + 1);
}

function resolveNearestSqlStatement(statements: SqlStatementSegment[], caretIndex: number) {
  return statements.reduce((bestMatch, statement) => {
    const statementDistance =
      caretIndex < statement.start ? statement.start - caretIndex : caretIndex > statement.end ? caretIndex - statement.end : 0;

    if (bestMatch == null) {
      return { statement, distance: statementDistance };
    }

    if (statementDistance < bestMatch.distance) {
      return { statement, distance: statementDistance };
    }

    return bestMatch;
  }, null as { statement: SqlStatementSegment; distance: number } | null)?.statement ?? null;
}

function isSubmitSelectableDdlStatement(sql: string) {
  return SUBMIT_INDEX_DDL_PATTERN.test(sql.trim());
}

function findContainingSqlStatementIndex(statements: SqlStatementSegment[], caretIndex: number) {
  return statements.findIndex((statement) => caretIndex >= statement.start && caretIndex <= statement.end);
}

function resolveSubmitReferenceStatementIndex(statements: SqlStatementSegment[], caretIndex: number) {
  const currentStatementIndex = findContainingSqlStatementIndex(statements, caretIndex);

  if (currentStatementIndex >= 0) {
    const currentStatement = statements[currentStatementIndex];
    if (resolveSubmitStatementMode(currentStatement.sql) === 'select') {
      return currentStatementIndex;
    }

    const nextSelectIndex = statements.findIndex(
      (statement, statementIndex) => statementIndex > currentStatementIndex && resolveSubmitStatementMode(statement.sql) === 'select',
    );
    if (nextSelectIndex >= 0) {
      return nextSelectIndex;
    }

    for (let statementIndex = currentStatementIndex - 1; statementIndex >= 0; statementIndex -= 1) {
      if (resolveSubmitStatementMode(statements[statementIndex].sql) === 'select') {
        return statementIndex;
      }
    }

    return -1;
  }

  const selectableStatements = statements.filter((statement) => resolveSubmitStatementMode(statement.sql) === 'select');
  const nextStatement = selectableStatements.find((statement) => statement.start >= caretIndex);
  if (nextStatement != null) {
    return statements.findIndex((statement) => statement.start === nextStatement.start);
  }

  const previousStatements = selectableStatements.filter((statement) => statement.start < caretIndex);
  if (previousStatements.length > 0) {
    return statements.findIndex((statement) => statement.start === previousStatements[previousStatements.length - 1].start);
  }

  return -1;
}

function createSubmitPickerOptions(value: string, caretIndex: number): SqlExecutionPickerOption[] {
  const statements = parseSqlStatements(value);
  if (statements.length === 0) {
    return [];
  }

  const referenceStatementIndex = resolveSubmitReferenceStatementIndex(statements, caretIndex);
  if (referenceStatementIndex < 0) {
    return [];
  }

  const referenceStatement = statements[referenceStatementIndex];
  if (resolveSubmitStatementMode(referenceStatement.sql) !== 'select') {
    return [];
  }

  const selectOnlyOption: SqlExecutionPickerOption = {
    key: 'submit-select-only',
    kind: 'statement',
    label: '기준 SELECT만 제출',
    preview: referenceStatement.preview,
    start: referenceStatement.start,
    end: referenceStatement.end,
    segments: [referenceStatement],
  };

  const ddlSegments = statements.filter(
    (statement, statementIndex) =>
      statementIndex < referenceStatementIndex && statement.start <= caretIndex && isSubmitSelectableDdlStatement(statement.sql),
  );
  const optionCandidates = [selectOnlyOption];

  if (ddlSegments.length > 0) {
    optionCandidates.push({
      key: 'submit-ddl-with-select',
      kind: 'statement',
      label: '위 DDL 포함 제출',
      preview: createSqlStatementPreview([...ddlSegments, referenceStatement].map((statement) => statement.sql).join(';\n')),
      start: ddlSegments[0].start,
      end: referenceStatement.end,
      segments: [...ddlSegments, referenceStatement],
    });
  }

  return optionCandidates.filter(
    (option, optionIndex, source) =>
      source.findIndex((candidate) => candidate.start === option.start && candidate.end === option.end) === optionIndex,
  );
}

function createExecutionPickerOptions(value: string, caretIndex: number): SqlExecutionPickerOption[] {
  const statements = parseSqlStatements(value);

  if (statements.length <= 1) {
    return [];
  }

  const nearestStatement = resolveNearestSqlStatement(statements, caretIndex);
  if (nearestStatement == null) {
    return [];
  }

  const firstStatement = statements[0];
  const lastStatement = statements[statements.length - 1];
  const looseStatementGroup = resolveLooseSqlStatementGroup(value, statements, nearestStatement);
  const wholeSql = statements.map((statement) => statement.sql).join(';\n');
  const optionCandidates: SqlExecutionPickerOption[] = [
    {
      key: 'current-statement',
      kind: 'statement',
      label: '현재 구문 실행',
      preview: nearestStatement.preview,
      start: nearestStatement.start,
      end: nearestStatement.end,
      segments: [nearestStatement],
    },
    {
      key: 'loose-statement-group',
      kind: 'statement',
      label: '인접 구문 실행',
      preview: createSqlStatementPreview(looseStatementGroup.map((statement) => statement.sql).join(';\n')),
      start: looseStatementGroup[0].start,
      end: looseStatementGroup[looseStatementGroup.length - 1].end,
      segments: looseStatementGroup,
    },
    {
      key: 'all-statements',
      kind: 'all',
      label: '전체 실행',
      preview: createSqlStatementPreview(wholeSql),
      start: firstStatement.start,
      end: lastStatement.end,
      segments: statements,
    },
  ];

  return optionCandidates.filter(
    (option, optionIndex, source) =>
      source.findIndex((candidate) => candidate.start === option.start && candidate.end === option.end) === optionIndex,
  );
}

function tokenizeSqlLine(line: string, tableNames: Set<string>, columnNames: Set<string>) {
  const tokens: SqlHighlightToken[] = [];
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
      const normalizedToken = token.toLowerCase();
      const previousMeaningfulToken = [...lineTokens.slice(0, index)].reverse().find((candidate) => !/^\s+$/.test(candidate));
      const nextMeaningfulToken = lineTokens.slice(index + 1).find((candidate) => !/^\s+$/.test(candidate));

      if (SQL_HIGHLIGHT_KEYWORDS.has(upperToken)) {
        tokens.push({
          text: token,
          kind: upperToken === 'EXPLAIN' || upperToken === 'ANALYZE' ? 'explain-keyword' : 'keyword',
        });
        expectTable = SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS.has(upperToken);
        continue;
      }

      if (previousMeaningfulToken === '.') {
        tokens.push({ text: token, kind: 'column' });
        expectTable = false;
        continue;
      }

      if (expectTable || tableNames.has(normalizedToken)) {
        tokens.push({ text: token, kind: 'table' });
        expectTable = false;
        continue;
      }

      if (columnNames.has(normalizedToken)) {
        tokens.push({ text: token, kind: 'column' });
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

function normalizeSqlHighlightRanges(ranges: SqlHighlightRange[], sqlLength: number) {
  const normalizedRanges = ranges
    .map((range) => ({
      start: Math.max(0, Math.min(range.start, sqlLength)),
      end: Math.max(0, Math.min(range.end, sqlLength)),
    }))
    .filter((range) => range.end > range.start)
    .sort((left, right) => left.start - right.start || left.end - right.end);

  return normalizedRanges.reduce<SqlHighlightRange[]>((mergedRanges, range) => {
    const previousRange = mergedRanges[mergedRanges.length - 1];

    if (!previousRange || range.start > previousRange.end) {
      mergedRanges.push({ ...range });
      return mergedRanges;
    }

    previousRange.end = Math.max(previousRange.end, range.end);
    return mergedRanges;
  }, []);
}

function splitSqlTokenByHighlightRanges(text: string, tokenAbsoluteStart: number, highlightRanges: SqlHighlightRange[]) {
  if (text.length === 0 || highlightRanges.length === 0) {
    return [
      {
        text,
        isHighlighted: false,
      },
    ];
  }

  const tokenAbsoluteEnd = tokenAbsoluteStart + text.length;
  const overlappingRanges = highlightRanges.filter((range) => range.end > tokenAbsoluteStart && range.start < tokenAbsoluteEnd);

  if (overlappingRanges.length === 0) {
    return [
      {
        text,
        isHighlighted: false,
      },
    ];
  }

  const segments: Array<{ text: string; isHighlighted: boolean }> = [];
  let cursor = tokenAbsoluteStart;

  overlappingRanges.forEach((range) => {
    const segmentStart = Math.max(range.start, tokenAbsoluteStart);
    const segmentEnd = Math.min(range.end, tokenAbsoluteEnd);

    if (cursor < segmentStart) {
      segments.push({
        text: text.slice(cursor - tokenAbsoluteStart, segmentStart - tokenAbsoluteStart),
        isHighlighted: false,
      });
    }

    if (segmentStart < segmentEnd) {
      segments.push({
        text: text.slice(segmentStart - tokenAbsoluteStart, segmentEnd - tokenAbsoluteStart),
        isHighlighted: true,
      });
    }

    cursor = Math.max(cursor, segmentEnd);
  });

  if (cursor < tokenAbsoluteEnd) {
    segments.push({
      text: text.slice(cursor - tokenAbsoluteStart),
      isHighlighted: false,
    });
  }

  return segments.filter((segment) => segment.text.length > 0);
}

function renderHighlightedSql(sql: string, tableNames: Set<string>, columnNames: Set<string>, highlightRanges: SqlHighlightRange[] = []) {
  const normalizedSql = sql.replace(/\r\n/g, '\n');
  const normalizedHighlightRanges = normalizeSqlHighlightRanges(highlightRanges, normalizedSql.length);
  const lines = normalizedSql.split('\n');

  let lineAbsoluteStart = 0;

  return lines.map((line, lineIndex) => {
    const lineTokens = tokenizeSqlLine(line, tableNames, columnNames);
    let tokenOffset = 0;
    const renderedLine = lineTokens.flatMap((token, tokenIndex) => {
      const tokenAbsoluteStart = lineAbsoluteStart + tokenOffset;
      const tokenSegments = splitSqlTokenByHighlightRanges(token.text, tokenAbsoluteStart, normalizedHighlightRanges);

      tokenOffset += token.text.length;

      return tokenSegments.map((segment, segmentIndex) => {
        const tokenContent =
          token.kind == null ? (
            segment.text
          ) : (
            <span className={`solve-sql-token is-${token.kind}`}>{segment.text}</span>
          );

        return segment.isHighlighted ? (
          <span key={`token-${lineIndex}-${tokenIndex}-${segmentIndex}`} className="solve-sql-selection-fill">
            {tokenContent}
          </span>
        ) : (
          <span key={`token-${lineIndex}-${tokenIndex}-${segmentIndex}`}>{tokenContent}</span>
        );
      });
    });

    lineAbsoluteStart += line.length + 1;

    return (
      <Fragment key={`line-${lineIndex}`}>
        {renderedLine}
        {lineIndex < lines.length - 1 ? '\n' : null}
      </Fragment>
    );
  });
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
  const ownerDocument = textarea.ownerDocument;
  const ownerWindow = ownerDocument.defaultView ?? window;
  const computedStyle = ownerWindow.getComputedStyle(textarea);
  const mirror = ownerDocument.createElement('div');
  const mirrorHost = textarea.parentElement ?? ownerDocument.body;
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

  const caretMarker = ownerDocument.createElement('span');
  caretMarker.textContent = value.slice(caretIndex, caretIndex + 1) || ' ';
  mirror.appendChild(caretMarker);
  mirrorHost.appendChild(mirror);
  const editorPadding = 12;
  const panelGap = 8;
  const lineHeight = caretMarker.offsetHeight || Number.parseFloat(computedStyle.lineHeight) || Number.parseFloat(computedStyle.fontSize) * 1.5 || 20;
  const availableWidth = Math.max(160, textarea.clientWidth - editorPadding * 2);
  const maxWidth = Math.min(448, availableWidth);
  const caretLeft = caretMarker.offsetLeft - textarea.scrollLeft;
  const left = clamp(
    textareaRect.left + caretLeft,
    editorPadding,
    Math.max(editorPadding, ownerWindow.innerWidth - maxWidth - editorPadding),
  );
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
      Math.max(56, ownerWindow.innerHeight - (textareaRect.top + belowTop) - editorPadding),
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

function measureStatementStartOffsets(textarea: HTMLTextAreaElement, value: string, offsets: number[]) {
  if (offsets.length === 0) {
    return new Map<number, number>();
  }

  const ownerDocument = textarea.ownerDocument;
  const ownerWindow = ownerDocument.defaultView ?? window;
  const computedStyle = ownerWindow.getComputedStyle(textarea);
  const mirror = ownerDocument.createElement('div');
  const mirrorHost = textarea.parentElement ?? ownerDocument.body;
  const uniqueOffsets = [...new Set(offsets)].sort((left, right) => left - right);
  const markerElements = new Map<number, HTMLSpanElement>();
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

  let currentOffset = 0;
  uniqueOffsets.forEach((offset) => {
    mirror.appendChild(ownerDocument.createTextNode(value.slice(currentOffset, offset)));
    const marker = ownerDocument.createElement('span');
    marker.textContent = '\u200b';
    marker.style.display = 'inline-block';
    marker.style.width = '0';
    marker.style.height = '1em';
    marker.style.padding = '0';
    marker.style.margin = '0';
    marker.style.verticalAlign = 'top';
    markerElements.set(offset, marker);
    mirror.appendChild(marker);
    currentOffset = offset;
  });
  mirror.appendChild(ownerDocument.createTextNode(value.slice(currentOffset)));

  mirrorHost.appendChild(mirror);

  const measuredOffsets = new Map<number, number>();
  uniqueOffsets.forEach((offset) => {
    measuredOffsets.set(offset, markerElements.get(offset)?.offsetTop ?? 0);
  });

  mirrorHost.removeChild(mirror);
  return measuredOffsets;
}

function measureExecutionStatementMarkerLayout(
  textarea: HTMLTextAreaElement,
  value: string,
  offsets: number[],
  sqlEditorFontSize: number,
): ExecutionStatementMarkerLayout | null {
  if (offsets.length === 0) {
    return null;
  }

  const ownerWindow = textarea.ownerDocument.defaultView ?? window;
  const editorDocumentElement = textarea.ownerDocument.documentElement;
  const computedStyle = ownerWindow.getComputedStyle(textarea);
  const rootFontSize = Number.parseFloat(ownerWindow.getComputedStyle(editorDocumentElement).fontSize) || 16;
  const lineHeight =
    Number.parseFloat(computedStyle.lineHeight) || sqlEditorFontSize * SQL_EDITOR_CONTENT_LINE_HEIGHT_RATIO;
  const paddingLeft = Number.parseFloat(computedStyle.paddingLeft) || 0;
  const markerIconSize = Math.max(sqlEditorFontSize * 0.82, rootFontSize * 0.82);
  const markerSlotWidth = Math.max(markerIconSize + rootFontSize * 0.08, rootFontSize * 0.84);
  const markerLeft = paddingLeft - markerSlotWidth - rootFontSize * 0.22;
  const measuredStatementOffsets = measureStatementStartOffsets(textarea, value, offsets);
  const topOffsets = offsets.reduce<Record<number, number>>((result, offset) => {
    result[offset] = measuredStatementOffsets.get(offset) ?? 0;
    return result;
  }, {});

  return {
    left: markerLeft,
    width: markerSlotWidth,
    height: lineHeight,
    fontSize: markerIconSize,
    topOffsets,
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
    currentPage: message.currentPage ?? undefined,
    pageSize: message.pageSize ?? undefined,
    executionTimeMs: message.executionTimeMs ?? undefined,
  };
}

function createSubmitProgressStep(
  stepKey: string,
  status: ProblemSubmitStepStatus,
  message: string,
  detailLines: string[] = [],
): ProblemSubmitProgressStep {
  return {
    stepKey,
    status,
    message,
    detailLines,
  };
}

function upsertSubmitProgressStep(
  currentSteps: ProblemSubmitProgressStep[],
  nextStep: ProblemSubmitProgressStep,
): ProblemSubmitProgressStep[] {
  const existingIndex = currentSteps.findIndex((step) => step.stepKey === nextStep.stepKey);
  if (existingIndex >= 0) {
    const nextSteps = [...currentSteps];
    nextSteps[existingIndex] = nextStep;
    return nextSteps;
  }

  const nextSteps = [...currentSteps, nextStep];
  const orderedKeys = new Map(SUBMIT_STEP_ORDER.map((stepKey, index) => [stepKey, index]));

  return nextSteps.sort((left, right) => {
    const leftIndex = orderedKeys.get(left.stepKey) ?? Number.MAX_SAFE_INTEGER;
    const rightIndex = orderedKeys.get(right.stepKey) ?? Number.MAX_SAFE_INTEGER;
    return leftIndex - rightIndex;
  });
}

function SubmitProgressItem({ step }: { step: ProblemSubmitProgressStep }) {
  const isExpandable = step.stepKey === 'ddl' && step.detailLines.length > 0;
  const [collapsed, setCollapsed] = useState(step.stepKey === 'ddl');
  const toneClass =
    step.status === 'running'
      ? 'is-pending'
      : step.status === 'success'
        ? 'is-success'
        : 'is-error';
  const indicator = step.status === 'running' ? <span className="solve-editor-statement-spinner" /> : step.status === 'success' ? '✓' : '✕';

  useEffect(() => {
    if (!isExpandable) {
      setCollapsed(false);
    }
  }, [isExpandable]);

  useEffect(() => {
    if (isExpandable) {
      setCollapsed(true);
    }
  }, [isExpandable, step.detailLines.length]);

  return (
    <div className={`solve-editor-inline-result-group solve-submit-progress-item ${collapsed ? 'is-collapsed' : ''} ${toneClass}`.trim()}>
      <div className="solve-editor-inline-result-header">
        {isExpandable ? (
          <button
            type="button"
            className="solve-detail-section-divider-button solve-pane-section-divider-button"
            aria-label={collapsed ? '펼치기' : '접기'}
            aria-expanded={!collapsed}
            onClick={() => setCollapsed((current) => !current)}
          >
            <CollapseChevronIcon collapsed={collapsed} />
          </button>
        ) : null}
        <div className="solve-pane-summary-row">
          <span className={`solve-pane-summary-status-button ${toneClass}`.trim()} aria-hidden="true">
            {indicator}
          </span>
          <span className={`solve-pane-summary-statement-title ${toneClass}`.trim()}>{step.message}</span>
        </div>
      </div>
      {step.detailLines.length > 0 && (!isExpandable || !collapsed) ? (
        <div className="solve-editor-inline-result-body solve-pane-result-stack">
          {step.detailLines.map((detailLine) => (
            <p key={`${step.stepKey}-${detailLine}`} className={`solve-pane-result-message ${toneClass === 'is-error' ? 'is-error' : ''}`.trim()}>
              {detailLine}
            </p>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function renderResultTable(
  columns: string[],
  rows: string[][],
  emptyMessage: string,
  rowCount: number,
  currentPage: number,
  pageSize: number,
  onPageChange: (page: number) => void,
  pageInput: string,
  onPageInputChange: (value: string) => void,
  isPageJumpEditing: boolean,
  isPageLoading: boolean,
  onStartPageJumpEditing: () => void,
  onApplyPageJump: () => void,
  onCancelPageJump: () => void,
  resetKey: number,
  onResetWidths: () => void,
) {
  if (rowCount === 0) {
    return <div className="solve-result-empty solve-result-empty-table">{emptyMessage}</div>;
  }

  const columnLabels =
    columns.length > 0
      ? columns
      : Array.from({ length: rows.reduce((maxCount, row) => Math.max(maxCount, row.length), 0) }, (_, index) => `컬럼 ${index + 1}`);

  const totalPages = Math.max(1, Math.ceil(rowCount / pageSize));
  const normalizedPage = clamp(currentPage, 1, totalPages);
  const pageRows = rows;
  const gridColumns = columnLabels.map((columnLabel) => ({ key: columnLabel, label: columnLabel }));
  const gridRows = pageRows.map((row) => columnLabels.map((_, columnIndex) => formatCellValue(row[columnIndex])));

  return (
    <div className="solve-result-table-block">
      <div className="solve-detail-table-block solve-result-table-shell">
        <div className="solve-result-table-header">
          <div className="solve-result-table-summary">
            <span className="solve-result-table-summary-item is-rows">
              <span className="solve-result-table-summary-value">{`${formatGroupedNumber(rowCount)} Rows`}</span>
            </span>
            <button
              type="button"
              className="solve-pane-action solve-pane-action-icon solve-pane-summary-refresh"
              aria-label="실행 결과 너비 초기화"
              onClick={onResetWidths}
            >
              <RefreshIcon />
            </button>
          </div>
        </div>
        <div className={`solve-result-table-grid-shell ${isPageLoading ? 'is-loading' : ''}`.trim()}>
          <div className="solve-result-table-grid-content">
            <ExecutionResultGrid columns={gridColumns} rows={gridRows} emptyMessage={emptyMessage} resetKey={resetKey} />
          </div>
          {isPageLoading ? (
            <div className="solve-result-table-grid-overlay" aria-live="polite" aria-label="실행 결과 페이지 로딩 중">
              <span className="solve-result-table-grid-spinner" aria-hidden="true" />
              <span className="solve-result-table-grid-overlay-label">로딩 중</span>
            </div>
          ) : null}
        </div>
        {totalPages > 1 ? (
          <div className="solve-result-pagination">
            <button
              type="button"
              className="mini-toggle solve-result-pagination-button"
              onClick={() => onPageChange(normalizedPage - 1)}
              disabled={normalizedPage === 1 || isPageLoading}
            >
              이전
            </button>

            {isPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="text-field solve-result-pagination-input"
                aria-label="이동할 페이지 입력"
                value={pageInput}
                onChange={(event) => onPageInputChange(event.target.value.replace(/\D+/g, ''))}
                onBlur={onApplyPageJump}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    onApplyPageJump();
                    return;
                  }

                  if (event.key === 'Escape') {
                    event.preventDefault();
                    onCancelPageJump();
                  }
                }}
                autoFocus
              />
            ) : (
              <button
                type="button"
                className="solve-result-pagination-label solve-result-pagination-meta-button"
                aria-label="이동할 페이지 입력 열기"
                disabled={isPageLoading}
                onClick={onStartPageJumpEditing}
              >
                {`${normalizedPage} / ${totalPages}`}
              </button>
            )}

            <button
              type="button"
              className="mini-toggle solve-result-pagination-button"
              onClick={() => onPageChange(normalizedPage + 1)}
              disabled={normalizedPage === totalPages || isPageLoading}
            >
              다음
            </button>
          </div>
        ) : null}
      </div>
    </div>
  );
}

function renderExecutionContent(
  executionResult: ProblemExecutionResult,
  currentPage: number,
  pageSize: number,
  onPageChange: (page: number) => void,
  pageInput: string,
  onPageInputChange: (value: string) => void,
  isPageJumpEditing: boolean,
  isPageLoading: boolean,
  onStartPageJumpEditing: () => void,
  onApplyPageJump: () => void,
  onCancelPageJump: () => void,
  resetKey: number,
  onResetWidths: () => void,
) {
  if (!executionResult.success) {
    return <p className="solve-pane-result-message is-error">{executionResult.message ?? '실행에 실패했다.'}</p>;
  }

  if (executionResult.mode === 'select') {
    return renderResultTable(
      executionResult.columns,
      executionResult.rows,
      '표시할 실행 결과가 없다.',
      executionResult.rowCount,
      currentPage,
      pageSize,
      onPageChange,
      pageInput,
      onPageInputChange,
      isPageJumpEditing,
      isPageLoading,
      onStartPageJumpEditing,
      onApplyPageJump,
      onCancelPageJump,
      resetKey,
      onResetWidths,
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

  if (executionResult.mode === 'command') {
    return executionResult.message.trim() !== '' ? <p className="solve-pane-result-message">{executionResult.message}</p> : null;
  }

  return executionResult.message.trim() !== '' ? <p className="solve-pane-result-message">{executionResult.message}</p> : null;
}

function ExecutionStatementResultItem({
  item,
  onStatusIndicatorClick,
  registerResultItemRef,
  onRequestPage,
}: {
  item: ExecutionStatementRun;
  onStatusIndicatorClick: () => void;
  registerResultItemRef: (key: string, element: HTMLDivElement | null) => void;
  onRequestPage: (item: ExecutionStatementRun, page: number) => Promise<void>;
}) {
  const [collapsed, setCollapsed] = useState(false);
  const [currentPage, setCurrentPage] = useState(item.result?.currentPage ?? 1);
  const [pageInput, setPageInput] = useState('1');
  const [isPageJumpEditing, setIsPageJumpEditing] = useState(false);
  const [gridResetKey, setGridResetKey] = useState(0);
  const [isPageLoading, setIsPageLoading] = useState(false);
  const executionItemRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setCollapsed(false);
    setCurrentPage(item.result?.currentPage ?? 1);
    setPageInput(String(item.result?.currentPage ?? 1));
    setIsPageJumpEditing(false);
    setGridResetKey(0);
    setIsPageLoading(false);
  }, [item.key, item.status, item.result?.mode, item.result?.rowCount, item.result?.message, item.result?.currentPage]);

  useEffect(() => {
    registerResultItemRef(item.key, executionItemRef.current);

    return () => {
      registerResultItemRef(item.key, null);
    };
  }, [item.key, registerResultItemRef]);

  useEffect(() => {
    setPageInput(String(currentPage));
  }, [currentPage]);

  const executionResult = item.result;
  const showErrorSummary = item.status === 'error' || (executionResult != null && !executionResult.success);
  const executionResultTotalPages =
    executionResult != null && executionResult.success && executionResult.mode === 'select'
      ? getExecutionResultPageCount(executionResult.rowCount)
      : 1;
  const resolvedPageSize = executionResult?.pageSize ?? EXECUTION_RESULT_PAGE_SIZE;
  const previewLabel = item.sql.replace(/\s+/g, ' ').trim() || 'SQL';
  const titleToneClass =
    item.status === 'running'
      ? 'is-pending'
      : showErrorSummary
        ? 'is-error'
        : executionResult != null
          ? 'is-success'
          : 'is-pending';
  const resultToneClass =
    item.status === 'running'
      ? 'is-pending'
      : showErrorSummary
        ? 'is-error'
        : executionResult != null
          ? 'is-success'
          : 'is-pending';
  const requestPage = async (nextPage: number) => {
    if (executionResult == null || !executionResult.success || executionResult.mode !== 'select' || isPageLoading) {
      return;
    }

    const normalizedPage = clamp(nextPage, 1, executionResultTotalPages);
    if (normalizedPage === currentPage) {
      return;
    }

    setIsPageLoading(true);
    try {
      await onRequestPage(item, normalizedPage);
      setCurrentPage(normalizedPage);
    } finally {
      setIsPageLoading(false);
    }
  };

  const applyPageJump = () => {
    const parsedPage = Number.parseInt(pageInput, 10);
    if (Number.isNaN(parsedPage)) {
      setPageInput(String(currentPage));
      setIsPageJumpEditing(false);
      return;
    }

    const nextPage = clamp(parsedPage, 1, executionResultTotalPages);
    void requestPage(nextPage);
    setIsPageJumpEditing(false);
  };
  const cancelPageJump = () => {
    setPageInput(String(currentPage));
    setIsPageJumpEditing(false);
  };

  return (
    <div
      ref={executionItemRef}
      className={`solve-editor-inline-result-group ${collapsed ? 'is-collapsed' : ''} ${resultToneClass}`.trim()}
    >
      <div className="solve-editor-inline-result-header">
        <button
          type="button"
          className="solve-detail-section-divider-button solve-pane-section-divider-button"
          aria-label={collapsed ? '펼치기' : '접기'}
          aria-expanded={!collapsed}
          onClick={() => setCollapsed((current) => !current)}
        >
          <CollapseChevronIcon collapsed={collapsed} />
        </button>
        <div className="solve-pane-summary-row">
          <button
            type="button"
            className={`solve-pane-summary-status-button ${titleToneClass}`.trim()}
            aria-label="실행 결과 위치로 이동"
            onClick={onStatusIndicatorClick}
          >
            {item.status === 'running' ? (
              <span className="solve-editor-statement-spinner" />
            ) : showErrorSummary ? (
              '✕'
            ) : executionResult ? (
              '✓'
            ) : (
              '•'
            )}
          </button>
          <span className={`solve-pane-summary-statement-title ${titleToneClass}`.trim()}>{previewLabel}</span>
        </div>
      </div>

      {!collapsed ? (
        <div className="solve-editor-inline-result-body solve-pane-result-stack">
          {item.status === 'running' ? (
            <div className="solve-result-empty solve-result-empty-table">SQL을 실행하는 중이다.</div>
          ) : executionResult ? (
            renderExecutionContent(
              executionResult,
              currentPage,
              resolvedPageSize,
              requestPage,
              pageInput,
              setPageInput,
              isPageJumpEditing,
              isPageLoading,
              () => {
                setPageInput(String(currentPage));
                setIsPageJumpEditing(true);
              },
              applyPageJump,
              cancelPageJump,
              gridResetKey,
              () => setGridResetKey((current) => current + 1),
            )
          ) : (
            <div className="solve-result-empty solve-result-empty-table">실행 대기 중</div>
          )}
        </div>
      ) : null}
    </div>
  );
}

function SolvePageAuthOverlay({
  mode,
  onClose,
  onOpenSignup,
  onOpenResetPassword,
  onReturnToLogin,
  onAuthenticated,
  problemId,
  sql,
  selectedDbms,
}: {
  mode: SolveAuthOverlayMode;
  onClose: () => void;
  onOpenSignup: () => void;
  onOpenResetPassword: () => void;
  onReturnToLogin: () => void;
  onAuthenticated: () => void;
  problemId: string;
  sql: string;
  selectedDbms: DbmsType;
}) {
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [rememberLogin, setRememberLogin] = useState(false);
  const [loginErrors, setLoginErrors] = useState<string[]>([]);
  const [isLoginSubmitting, setIsLoginSubmitting] = useState(false);
  const [isSocialLoginSubmitting, setIsSocialLoginSubmitting] = useState(false);

  const [signupEmail, setSignupEmail] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupPasswordConfirm, setSignupPasswordConfirm] = useState('');
  const [signupErrors, setSignupErrors] = useState<string[]>([]);
  const [isSignupSubmitting, setIsSignupSubmitting] = useState(false);
  const [signupEmailCheckStatus, setSignupEmailCheckStatus] = useState<'idle' | 'checking' | 'available' | 'duplicated'>('idle');
  const [signupEmailCheckReason, setSignupEmailCheckReason] = useState<string | null>(null);
  const [signupEmailLastCheckedValue, setSignupEmailLastCheckedValue] = useState('');
  const signupEmailCheckSequenceRef = useRef(0);

  const [resetEmail, setResetEmail] = useState('');
  const [resetCode, setResetCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
  const [isSendingResetCode, setIsSendingResetCode] = useState(false);
  const [isVerifyingResetCode, setIsVerifyingResetCode] = useState(false);
  const [isResettingPassword, setIsResettingPassword] = useState(false);
  const [isResetCodeSent, setIsResetCodeSent] = useState(false);
  const [isResetCodeVerified, setIsResetCodeVerified] = useState(false);
  const [resetStatusMessage, setResetStatusMessage] = useState<string | null>(null);
  const [resetErrors, setResetErrors] = useState<string[]>([]);
  const socialLoginPopupPollIdRef = useRef<number | null>(null);

  const normalizedLoginEmail = loginEmail.trim();
  const normalizedSignupEmail = signupEmail.trim();
  const normalizedResetEmail = resetEmail.trim();
  const normalizedResetCode = resetCode.trim().toUpperCase();
  const isLoginReady = normalizedLoginEmail !== '' && loginPassword.trim() !== '';
  const isSignupEmailValid = EMAIL_PATTERN.test(normalizedSignupEmail);
  const isSignupPasswordValid = hasRequiredPasswordFormat(signupPassword);
  const isSignupPasswordConfirmValid = signupPasswordConfirm !== '' && signupPasswordConfirm === signupPassword;
  const isSignupReady =
    isSignupEmailValid &&
    isSignupPasswordValid &&
    isSignupPasswordConfirmValid &&
    signupEmailCheckStatus !== 'checking';
  const isResetEmailValid = EMAIL_PATTERN.test(normalizedResetEmail);
  const isResetCodeValid = PASSWORD_RESET_CODE_PATTERN.test(normalizedResetCode);
  const isResetPasswordValid = hasRequiredPasswordFormat(newPassword);
  const isResetPasswordConfirmValid = newPasswordConfirm !== '' && newPasswordConfirm === newPassword;
  const signupEmailHintMessage =
    normalizedSignupEmail === ''
      ? SIGNUP_EMAIL_HINT
      : !isSignupEmailValid
        ? SIGNUP_EMAIL_HINT
        : signupEmailCheckStatus === 'checking'
          ? SIGNUP_EMAIL_CHECKING_MESSAGE
          : signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'duplicated'
            ? (signupEmailCheckReason ?? SIGNUP_EMAIL_DUPLICATED_MESSAGE)
            : signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'available'
              ? SIGNUP_EMAIL_AVAILABLE_MESSAGE
              : SIGNUP_EMAIL_HINT;
  const hasSignupEmailError =
    normalizedSignupEmail !== '' &&
    (!isSignupEmailValid || (signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'duplicated'));
  const hasSignupEmailSuccess =
    normalizedSignupEmail !== '' &&
    !hasSignupEmailError &&
    signupEmailLastCheckedValue === normalizedSignupEmail &&
    signupEmailCheckStatus === 'available';

  useEffect(() => {
    return () => {
      if (socialLoginPopupPollIdRef.current != null) {
        window.clearInterval(socialLoginPopupPollIdRef.current);
      }
    };
  }, []);

  const startSocialLogin = (provider: SolveAuthSocialProvider) => {
    if (typeof window === 'undefined' || isSocialLoginSubmitting) {
      return;
    }

    if (socialLoginPopupPollIdRef.current != null) {
      window.clearInterval(socialLoginPopupPollIdRef.current);
      socialLoginPopupPollIdRef.current = null;
    }

    saveSolvePageAuthReturn(problemId, sql, selectedDbms);
    setLoginErrors([]);
    setIsSocialLoginSubmitting(true);

    const popupWidth = 520;
    const popupHeight = 760;
    const popupLeft = Math.max(0, Math.round(window.screenX + (window.outerWidth - popupWidth) / 2));
    const popupTop = Math.max(0, Math.round(window.screenY + (window.outerHeight - popupHeight) / 2));
    const popup = window.open(
      `${getApiBaseUrl()}/oauth2/authorization/${provider}`,
      `quertimizer-social-login-${provider}`,
      `popup=yes,width=${popupWidth},height=${popupHeight},left=${popupLeft},top=${popupTop},resizable=yes,scrollbars=yes`,
    );

    if (!popup) {
      setIsSocialLoginSubmitting(false);
      setLoginErrors(['팝업이 차단되어 소셜 로그인을 진행할 수 없습니다.']);
      return;
    }

    popup.focus();
    let isChecking = false;

    let removeMessageListener = () => {};

    const stopPolling = () => {
      if (socialLoginPopupPollIdRef.current != null) {
        window.clearInterval(socialLoginPopupPollIdRef.current);
        socialLoginPopupPollIdRef.current = null;
      }
      removeMessageListener();
      setIsSocialLoginSubmitting(false);
    };

    const handlePopupMessage = (event: MessageEvent) => {
      if (event.origin !== window.location.origin || event.data == null || typeof event.data !== 'object') {
        return;
      }

      const message = event.data as { type?: string; provider?: SolveAuthSocialProvider | 'oauth2' | null };
      if (message.type !== 'quertimizer-social-login-success' && message.type !== 'quertimizer-social-login-error') {
        return;
      }

      void (async () => {
        if (message.type === 'quertimizer-social-login-error') {
          popup.close();
          stopPolling();
          setLoginErrors([getSolveAuthSocialLoginErrorMessage(message.provider ?? null)]);
          return;
        }

        try {
          const session = await fetchSessionMe();
          if (!session.authenticated) {
            return;
          }

          await completeAuthentication(session, false);
          popup.close();
          stopPolling();
          onAuthenticated();
        } catch {
        }
      })();
    };

    window.addEventListener('message', handlePopupMessage);
    removeMessageListener = () => {
      window.removeEventListener('message', handlePopupMessage);
      removeMessageListener = () => {};
    };

    const pollPopupState = async () => {
      if (isChecking) {
        return;
      }

      isChecking = true;

      try {
        if (popup.closed) {
          stopPolling();
          return;
        }

        try {
          const popupUrl = new URL(popup.location.href);
          if (popupUrl.origin === window.location.origin) {
            const socialLoginError = popupUrl.searchParams.get('socialLoginError') as SolveAuthSocialProvider | 'oauth2' | null;
            if (socialLoginError != null) {
              popup.close();
              stopPolling();
              setLoginErrors([getSolveAuthSocialLoginErrorMessage(socialLoginError)]);
              return;
            }
          }
        } catch {
        }

        const session = await fetchSessionMe();
        if (!session.authenticated) {
          return;
        }

        await completeAuthentication(session, false);
        popup.close();
        stopPolling();
        onAuthenticated();
      } catch {
      } finally {
        isChecking = false;
      }
    };

    socialLoginPopupPollIdRef.current = window.setInterval(() => {
      void pollPopupState();
    }, 500);
    void pollPopupState();
  };

  const resetSignupEmailCheck = () => {
    setSignupEmailCheckStatus('idle');
    setSignupEmailCheckReason(null);
    setSignupEmailLastCheckedValue('');
  };

  const applySignupErrorReasons = (reasons: string[]) => {
    const nextErrors: string[] = [];

    for (const reason of reasons) {
      if (reason.includes('이메일') && (reason.includes('중복') || reason.includes('사용 중'))) {
        setSignupEmailCheckStatus('duplicated');
        setSignupEmailCheckReason(reason);
        setSignupEmailLastCheckedValue(normalizedSignupEmail);
        continue;
      }

      nextErrors.push(reason);
    }

    setSignupErrors(nextErrors);
  };

  const checkSignupEmailDuplication = async () => {
    if (normalizedSignupEmail === '') {
      resetSignupEmailCheck();
      return false;
    }

    if (!isSignupEmailValid) {
      resetSignupEmailCheck();
      return false;
    }

    if (signupEmailLastCheckedValue === normalizedSignupEmail) {
      return signupEmailCheckStatus === 'available';
    }

    const requestSequence = signupEmailCheckSequenceRef.current + 1;
    signupEmailCheckSequenceRef.current = requestSequence;
    setSignupEmailCheckStatus('checking');
    setSignupEmailCheckReason(null);
    setSignupErrors([]);

    try {
      const result = await checkDuplicateEmail(normalizedSignupEmail);

      if (requestSequence !== signupEmailCheckSequenceRef.current) {
        return false;
      }

      setSignupEmailCheckStatus(result.available ? 'available' : 'duplicated');
      setSignupEmailCheckReason(result.reason);
      setSignupEmailLastCheckedValue(normalizedSignupEmail);
      return result.available;
    } catch (error) {
      if (requestSequence !== signupEmailCheckSequenceRef.current) {
        return false;
      }

      setSignupEmailCheckStatus('idle');
      setSignupEmailLastCheckedValue(normalizedSignupEmail);

      if (error instanceof SignupApiError) {
        setSignupErrors(error.reasons);
        return false;
      }

      setSignupErrors([error instanceof Error ? error.message : '이메일 중복 확인 중 오류가 발생했습니다.']);
      return false;
    }
  };

  const handleLoginSubmit = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (!isLoginReady) {
      return;
    }

    try {
      setIsLoginSubmitting(true);
      setLoginErrors([]);
      saveSolvePageAuthReturn(problemId, sql, selectedDbms);

      const session = await login({
        email: normalizedLoginEmail,
        password: loginPassword,
        rememberLogin,
      });

      await completeAuthentication(session, rememberLogin);
      if (!session.authenticated) {
        setLoginErrors(['로그인에 실패했습니다.']);
        return;
      }

      onAuthenticated();
    } catch (error) {
      if (error instanceof AuthApiError) {
        setLoginErrors(error.reasons);
        return;
      }

      setLoginErrors([error instanceof Error ? error.message : '로그인 중 오류가 발생했습니다.']);
    } finally {
      setIsLoginSubmitting(false);
    }
  };

  const handleSignupSubmit = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (!isSignupReady) {
      return;
    }

    try {
      setIsSignupSubmitting(true);
      setSignupErrors([]);
      saveSolvePageAuthReturn(problemId, sql, selectedDbms);

      const isEmailAvailable = await checkSignupEmailDuplication();
      if (!isEmailAvailable) {
        return;
      }

      await signup({
        email: normalizedSignupEmail,
        password: signupPassword,
      });

      const session = await fetchSessionMe();
      await completeAuthentication(session, false);

      if (!session.authenticated) {
        setSignupErrors(['회원가입 후 세션을 확인하지 못했습니다.']);
        return;
      }

      onAuthenticated();
    } catch (error) {
      if (error instanceof SignupApiError || error instanceof AuthApiError) {
        applySignupErrorReasons(error.reasons);
        return;
      }

      setSignupErrors([error instanceof Error ? error.message : '회원가입 중 오류가 발생했습니다.']);
    } finally {
      setIsSignupSubmitting(false);
    }
  };

  const handleSendResetCode = async () => {
    if (!isResetEmailValid || isSendingResetCode) {
      return;
    }

    try {
      setIsSendingResetCode(true);
      setResetErrors([]);
      setResetStatusMessage(null);
      await sendPasswordResetCode({ email: normalizedResetEmail });
      setIsResetCodeSent(true);
      setIsResetCodeVerified(false);
      setResetStatusMessage('인증 코드를 전송했습니다. 5분 이내에 입력해 주세요.');
    } catch (error) {
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return;
      }

      setResetErrors([error instanceof Error ? error.message : '인증 코드 전송 중 오류가 발생했습니다.']);
    } finally {
      setIsSendingResetCode(false);
    }
  };

  const handleVerifyResetCode = async () => {
    if (!isResetEmailValid || !isResetCodeValid || !isResetCodeSent || isVerifyingResetCode) {
      return;
    }

    try {
      setIsVerifyingResetCode(true);
      setResetErrors([]);
      setResetStatusMessage(null);
      await verifyPasswordResetCode({
        email: normalizedResetEmail,
        code: normalizedResetCode,
      });
      setIsResetCodeVerified(true);
      setResetStatusMessage('인증 코드가 확인되었습니다. 새 비밀번호를 입력해 주세요.');
    } catch (error) {
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return;
      }

      setResetErrors([error instanceof Error ? error.message : '인증 코드 확인 중 오류가 발생했습니다.']);
    } finally {
      setIsVerifyingResetCode(false);
    }
  };

  const handleResetPassword = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (!isResetCodeVerified || !isResetPasswordValid || !isResetPasswordConfirmValid || isResettingPassword) {
      return;
    }

    try {
      setIsResettingPassword(true);
      setResetErrors([]);
      setResetStatusMessage(null);
      await resetPassword({
        email: normalizedResetEmail,
        code: normalizedResetCode,
        password: newPassword,
      });
      setResetStatusMessage('비밀번호가 변경되었습니다. 다시 로그인해 주세요.');
      setNewPassword('');
      setNewPasswordConfirm('');
      setTimeout(() => {
        onReturnToLogin();
      }, 300);
    } catch (error) {
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return;
      }

      setResetErrors([error instanceof Error ? error.message : '비밀번호 변경 중 오류가 발생했습니다.']);
    } finally {
      setIsResettingPassword(false);
    }
  };

  const overlayTitle = mode === 'signup' ? '이메일로 가입하기' : mode === 'reset-password' ? '비밀번호 찾기' : '로그인';
  const overlayDescription =
    mode === 'signup'
      ? '입력 중인 SQL은 유지됩니다. 가입 후 이어서 실행할 수 있습니다.'
      : mode === 'reset-password'
        ? '인증 코드를 확인한 뒤 새 비밀번호를 설정합니다.'
        : '입력 중인 SQL은 유지됩니다. 로그인 후 이어서 실행할 수 있습니다.';

  return (
    <div className="solve-auth-overlay" role="presentation">
      <div className="solve-auth-overlay-backdrop" />
      <section className="solve-auth-modal" role="dialog" aria-modal="true" aria-label={overlayTitle}>
        <button type="button" className="solve-auth-modal-close" aria-label="인증 팝업 닫기" onClick={onClose}>
          <CloseIcon />
        </button>
        <div className="solve-auth-modal-header is-centered">
          <div className="solve-auth-modal-copy">
            <h2 className="solve-auth-modal-title">{overlayTitle}</h2>
            <p className="solve-auth-modal-description">{overlayDescription}</p>
          </div>
        </div>

        {mode === 'login' ? (
          <div className="solve-auth-landing-body">
            <div className="minimal-auth-form solve-auth-modal-login-form">
              <div className="landing-auth-layout">
                <form className="landing-login-panel" aria-label="로그인 입력" onSubmit={(event) => void handleLoginSubmit(event)}>
                  <div className="field-stack">
                    <label className="field-label" htmlFor="solve-auth-email">
                      이메일
                    </label>
                    <input
                      id="solve-auth-email"
                      type="email"
                      className="text-field"
                      autoComplete="email"
                      value={loginEmail}
                      onChange={(event) => {
                        setLoginEmail(event.target.value);
                        setLoginErrors([]);
                      }}
                      placeholder="이메일을 입력해 주세요."
                    />
                  </div>

                  <div className="field-stack">
                    <label className="field-label" htmlFor="solve-auth-password">
                      비밀번호
                    </label>
                    <input
                      id="solve-auth-password"
                      type="password"
                      className="text-field"
                      autoComplete="current-password"
                      value={loginPassword}
                      onChange={(event) => {
                        setLoginPassword(event.target.value);
                        setLoginErrors([]);
                      }}
                      placeholder="비밀번호를 입력해 주세요."
                    />
                  </div>

                  {loginErrors.length > 0 ? (
                    <div className="signup-feedback-box" role="alert" aria-live="polite">
                      {loginErrors.map((reason) => (
                        <p key={reason} className="signup-feedback-message">
                          {reason}
                        </p>
                      ))}
                    </div>
                  ) : null}

                  <label className="login-remember-row">
                    <input
                      type="checkbox"
                      className="login-remember-checkbox"
                      checked={rememberLogin}
                      onChange={(event) => setRememberLogin(event.target.checked)}
                    />
                    <span className="login-remember-label">로그인 유지</span>
                  </label>

                  <div className="auth-actions minimal">
                    <button
                      type="submit"
                      className="btn primary landing-login-submit"
                      disabled={!isLoginReady || isLoginSubmitting}
                    >
                      {isLoginSubmitting ? '로그인 중' : '로그인'}
                    </button>
                  </div>

                  <button type="button" className="btn text landing-password-reset-link" onClick={onOpenResetPassword}>
                    비밀번호를 잊으셨나요?
                  </button>
                </form>

                <div className="landing-auth-divider" aria-hidden="true">
                  <span className="landing-auth-divider-line" />
                  <img className="landing-auth-divider-mark" src={logoImage} alt="" />
                  <span className="landing-auth-divider-line" />
                </div>

                <aside className="landing-access-panel" aria-label="계정 지원">
                  <div className="landing-access-group landing-access-group-social">
                    <button type="button" className="landing-access-card is-social" onClick={() => startSocialLogin('google')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon" aria-hidden="true">
                        <GoogleMarkIcon />
                      </span>
                      <span className="landing-access-card-title">Google로 계속하기</span>
                    </button>

                    <button type="button" className="landing-access-card is-social" onClick={() => startSocialLogin('github')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon" aria-hidden="true">
                        <GithubMarkIcon />
                      </span>
                      <span className="landing-access-card-title">Github로 계속하기</span>
                    </button>

                    <button type="button" className="landing-access-card is-social" onClick={() => startSocialLogin('kakao')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon" aria-hidden="true">
                        <KakaoMarkIcon />
                      </span>
                      <span className="landing-access-card-title">Kakao로 계속하기</span>
                    </button>
                  </div>

                  <div className="landing-access-group landing-access-group-support">
                    <button type="button" className="landing-access-card is-social is-email" onClick={onOpenSignup}>
                      <span className="landing-access-card-icon" aria-hidden="true">
                        <EmailMarkIcon />
                      </span>
                      <span className="landing-access-card-title">이메일로 계속하기</span>
                    </button>
                  </div>
                </aside>
              </div>
            </div>
          </div>
        ) : mode === 'signup' ? (
          <form className="solve-auth-signup-form" onSubmit={(event) => void handleSignupSubmit(event)}>
            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-signup-email">
                이메일
              </label>
              <input
                id="solve-signup-email"
                type="email"
                className="text-field"
                autoComplete="email"
                value={signupEmail}
                onChange={(event) => {
                  setSignupEmail(event.target.value);
                  setSignupErrors([]);
                  resetSignupEmailCheck();
                }}
                onBlur={() => {
                  void checkSignupEmailDuplication();
                }}
                placeholder="이메일을 입력해 주세요."
                aria-invalid={hasSignupEmailError}
              />
              <p className={`solve-auth-field-hint ${hasSignupEmailError ? 'is-error' : hasSignupEmailSuccess ? 'is-success' : ''}`}>
                {signupEmailHintMessage}
              </p>
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-signup-password">
                비밀번호
              </label>
              <input
                id="solve-signup-password"
                type="password"
                className="text-field"
                autoComplete="new-password"
                value={signupPassword}
                onChange={(event) => {
                  setSignupPassword(event.target.value);
                  setSignupErrors([]);
                }}
                placeholder="비밀번호를 입력해 주세요."
                aria-invalid={signupPassword.length > 0 && !isSignupPasswordValid}
              />
              <p className={`solve-auth-field-hint ${signupPassword.length > 0 && !isSignupPasswordValid ? 'is-error' : signupPassword.length > 0 ? 'is-success' : ''}`}>
                {SIGNUP_PASSWORD_HINT}
              </p>
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-signup-password-confirm">
                비밀번호 확인
              </label>
              <input
                id="solve-signup-password-confirm"
                type="password"
                className="text-field"
                autoComplete="new-password"
                value={signupPasswordConfirm}
                onChange={(event) => {
                  setSignupPasswordConfirm(event.target.value);
                  setSignupErrors([]);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  void handleSignupSubmit();
                }}
                placeholder="비밀번호를 다시 입력해 주세요."
                aria-invalid={signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid}
              />
              <p className={`solve-auth-field-hint ${signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid ? 'is-error' : signupPasswordConfirm.length > 0 ? 'is-success' : ''}`}>
                {SIGNUP_PASSWORD_CONFIRM_HINT}
              </p>
            </div>

            {signupErrors.length > 0 ? (
              <div className="solve-auth-feedback is-error" role="alert">
                {signupErrors.map((reason) => (
                  <p key={reason}>{reason}</p>
                ))}
              </div>
            ) : null}

            <div className="solve-auth-signup-actions">
              <button type="submit" className="btn primary" disabled={!isSignupReady || isSignupSubmitting}>
                {isSignupSubmitting ? '가입 중' : '가입하기'}
              </button>
              <button type="button" className="solve-auth-reset-link" onClick={onReturnToLogin}>
                로그인으로 돌아가기
              </button>
            </div>
          </form>
        ) : (
          <form className="solve-auth-reset-form" onSubmit={(event) => void handleResetPassword(event)}>
            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-reset-email">
                이메일
              </label>
              <div className="solve-auth-inline-row">
                <input
                  id="solve-reset-email"
                  type="email"
                  className="text-field"
                  autoComplete="email"
                  value={resetEmail}
                  onChange={(event) => {
                    setResetEmail(event.target.value);
                    setResetErrors([]);
                    setResetStatusMessage(null);
                  }}
                  placeholder="가입한 이메일을 입력해 주세요."
                />
                <button type="button" className="btn secondary" onClick={handleSendResetCode} disabled={!isResetEmailValid || isSendingResetCode}>
                  {isSendingResetCode ? '전송 중' : '코드 전송'}
                </button>
              </div>
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-reset-code">
                인증 코드
              </label>
              <div className="solve-auth-inline-row">
                <input
                  id="solve-reset-code"
                  type="text"
                  className="text-field"
                  value={resetCode}
                  onChange={(event) => {
                    setResetCode(sanitizeVerificationCode(event.target.value));
                    setResetErrors([]);
                    setResetStatusMessage(null);
                  }}
                  placeholder="이메일로 받은 6자리 코드를 입력해 주세요."
                />
                <button
                  type="button"
                  className="btn secondary"
                  onClick={handleVerifyResetCode}
                  disabled={!isResetCodeSent || !isResetCodeValid || isVerifyingResetCode}
                >
                  {isVerifyingResetCode ? '확인 중' : '코드 확인'}
                </button>
              </div>
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-reset-password">
                새 비밀번호
              </label>
              <input
                id="solve-reset-password"
                type="password"
                className="text-field"
                value={newPassword}
                onChange={(event) => {
                  setNewPassword(event.target.value);
                  setResetErrors([]);
                }}
                placeholder="특수문자를 포함해 8자 이상 입력해 주세요."
                disabled={!isResetCodeVerified}
              />
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-reset-password-confirm">
                새 비밀번호 확인
              </label>
              <input
                id="solve-reset-password-confirm"
                type="password"
                className="text-field"
                value={newPasswordConfirm}
                onChange={(event) => {
                  setNewPasswordConfirm(event.target.value);
                  setResetErrors([]);
                }}
                placeholder="비밀번호를 다시 입력해 주세요."
                disabled={!isResetCodeVerified}
              />
            </div>

            {resetStatusMessage ? <p className="solve-auth-feedback is-info">{resetStatusMessage}</p> : null}
            {resetErrors.length > 0 ? (
              <div className="solve-auth-feedback is-error" role="alert">
                {resetErrors.map((reason) => (
                  <p key={reason}>{reason}</p>
                ))}
              </div>
            ) : null}

            <div className="solve-auth-reset-actions">
              <button
                type="submit"
                className="btn primary"
                disabled={!isResetCodeVerified || !isResetPasswordValid || !isResetPasswordConfirmValid || isResettingPassword}
              >
                {isResettingPassword ? '변경 중' : '비밀번호 변경'}
              </button>
              <button type="button" className="solve-auth-reset-link" onClick={onReturnToLogin}>
                로그인으로 돌아가기
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
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

function GithubMarkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill="currentColor"
        d="M12 1.5a10.5 10.5 0 0 0-3.32 20.46c.53.1.72-.23.72-.51v-1.78c-2.93.64-3.55-1.24-3.55-1.24-.48-1.22-1.18-1.55-1.18-1.55-.96-.66.07-.64.07-.64 1.06.08 1.62 1.09 1.62 1.09.95 1.62 2.48 1.15 3.08.88.09-.68.37-1.15.67-1.42-2.34-.27-4.8-1.17-4.8-5.22 0-1.15.41-2.1 1.08-2.84-.11-.27-.47-1.38.1-2.88 0 0 .89-.28 2.91 1.08a10.02 10.02 0 0 1 5.3 0c2.02-1.36 2.91-1.08 2.91-1.08.57 1.5.21 2.61.1 2.88.67.74 1.08 1.69 1.08 2.84 0 4.06-2.46 4.94-4.81 5.21.38.33.72.98.72 1.98v2.93c0 .28.19.62.73.51A10.5 10.5 0 0 0 12 1.5Z"
      />
    </svg>
  );
}

function GoogleMarkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill="#4285F4"
        d="M23.52 12.27c0-.79-.07-1.55-.2-2.27H12v4.3h6.47a5.54 5.54 0 0 1-2.4 3.63v3.02h3.88c2.27-2.08 3.57-5.15 3.57-8.68Z"
      />
      <path
        fill="#34A853"
        d="M12 24c3.24 0 5.96-1.07 7.95-2.9l-3.88-3.02c-1.08.72-2.46 1.15-4.07 1.15-3.13 0-5.78-2.11-6.72-4.95H1.27v3.12A12 12 0 0 0 12 24Z"
      />
      <path
        fill="#FBBC05"
        d="M5.28 14.28A7.2 7.2 0 0 1 4.9 12c0-.79.14-1.56.38-2.28V6.6H1.27a12 12 0 0 0 0 10.8l4.01-3.12Z"
      />
      <path
        fill="#EA4335"
        d="M12 4.77c1.76 0 3.34.61 4.58 1.8l3.43-3.43C17.95 1.19 15.23 0 12 0A12 12 0 0 0 1.27 6.6l4.01 3.12c.94-2.84 3.59-4.95 6.72-4.95Z"
      />
    </svg>
  );
}

function KakaoMarkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="11" fill="#FEE500" />
      <path
        fill="#3B2727"
        d="M12.1 6.15c-4.02 0-7.28 2.52-7.28 5.63 0 1.84 1.13 3.48 2.88 4.5l-.78 2.55 3.14-2.08c.65.14 1.33.22 2.04.22 4.02 0 7.28-2.52 7.28-5.62 0-3.11-3.26-5.2-7.28-5.2Z"
      />
    </svg>
  );
}

function EmailMarkIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path
        fill="currentColor"
        d="M3.75 6.25A2.25 2.25 0 0 1 6 4h12a2.25 2.25 0 0 1 2.25 2.25v11.5A2.25 2.25 0 0 1 18 20H6a2.25 2.25 0 0 1-2.25-2.25V6.25Zm1.5.3v.2l6.42 4.67a.56.56 0 0 0 .66 0l6.42-4.67v-.2A.75.75 0 0 0 18 5.8H6a.75.75 0 0 0-.75.75Zm13.5 1.92-5.54 4.03a2.06 2.06 0 0 1-2.42 0L5.25 8.47v9.28c0 .41.34.75.75.75h12c.41 0 .75-.34.75-.75V8.47Z"
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
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M12.85 6.1A4.95 4.95 0 0 0 4.7 3.8"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
      <path
        d="M4.65 1.95v2.7h2.7"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
      <path
        d="M3.15 9.9A4.95 4.95 0 0 0 11.3 12.2"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
      <path
        d="M11.35 14.05v-2.7h-2.7"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.9"
      />
    </svg>
  );
}

function ExecutionSuccessIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M3.5 8.4 6.5 11.2 12.5 4.8"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function ExecutionErrorIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path d="M4.75 4.75 11.25 11.25" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
      <path d="M11.25 4.75 4.75 11.25" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
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
    openedWindow.document.documentElement.style.height = '100%';
    openedWindow.document.documentElement.style.overflow = 'hidden';
    openedWindow.document.body.innerHTML = '';
    openedWindow.document.body.className = document.body.className;
    openedWindow.document.body.style.margin = '0';
    openedWindow.document.body.style.height = '100%';
    openedWindow.document.body.style.background =
      getComputedStyle(document.documentElement).getPropertyValue('--bg-body').trim() ||
      getComputedStyle(document.body).backgroundColor ||
      '#eef3f9';
    openedWindow.document.body.style.overflow = 'hidden';

    const container = openedWindow.document.createElement('div');
    container.className = 'solve-external-window-root';
    container.style.height = '100%';
    container.style.overflow = 'hidden';
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
  const sqlEditorSelectionRef = useRef<SqlEditorSelection>({ start: 0, end: 0 });
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<ProblemSolveFavoriteSnapshot>('problemSolve'), []);
  const favoriteSelectionRestoreRef = useRef<SqlEditorSelection | null>(favoriteRestoreSnapshot?.editorSelection ?? null);
  const executionPanelRef = useRef<HTMLDivElement | null>(null);
  const executionResultItemRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const submitPanelRef = useRef<HTMLElement | null>(null);
  const executionResponseResolverRef = useRef<((result: ProblemExecutionResult) => void) | null>(null);
  const executionStopRequestedRef = useRef(false);
  const ignoredExecutionResponseCountRef = useRef(0);
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const { defaultDbms, isAuthenticated, isReady, userId } = useMockSession();
  const previousAuthenticationStateRef = useRef(isAuthenticated);
  const fallbackProblem = createFallbackProblemDetail(problemId);
  const [problemDetail, setProblemDetail] = useState<ProblemDetailData | null>(null);
  const [problemLoadError, setProblemLoadError] = useState<string | null>(null);
  const [executionRuns, setExecutionRuns] = useState<ExecutionStatementRun[]>([]);
  const [executionStatementMarkerLayout, setExecutionStatementMarkerLayout] = useState<ExecutionStatementMarkerLayout | null>(null);
  const [isExecuting, setIsExecuting] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitMessage, setSubmitMessage] = useState<string | null>(null);
  const [submitProgressSteps, setSubmitProgressSteps] = useState<ProblemSubmitProgressStep[]>([]);
  const [autocompleteState, setAutocompleteState] = useState<SqlAutocompleteState | null>(null);
  const [executionPickerState, setExecutionPickerState] = useState<SqlExecutionPickerState | null>(null);
  const [collapsedCards, setCollapsedCards] = useState<CollapsedCardState>({
    editor: false,
    execute: false,
    submit: false,
  });
  const [panelVisibility, setPanelVisibility] = useState<PanelVisibilityState>({
    editor: true,
    submit: false,
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
  const [authOverlayMode, setAuthOverlayMode] = useState<SolveAuthOverlayMode | null>(null);
  const [sqlEditorElement, setSqlEditorElement] = useState<HTMLTextAreaElement | null>(null);
  const problem = fallbackProblem;
  const availableDbms = getAvailableDbms(problem);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(
    favoriteRestoreSnapshot?.selectedDbms != null && availableDbms.includes(favoriteRestoreSnapshot.selectedDbms)
      ? favoriteRestoreSnapshot.selectedDbms
      : resolvePreferredDbms(availableDbms, problem.dbmsOptions, defaultDbms ?? null)
  );
  const [contentTab, setContentTab] = useState<SolveContentTab>(() => favoriteRestoreSnapshot?.contentTab ?? readSolveContentTabFromSearch(window.location.search));
  const [mySubmitHistoryPage, setMySubmitHistoryPage] = useState<SubmitHistoryPageData>(createEmptySolveSubmitHistoryPage());
  const [isMySubmitLoading, setIsMySubmitLoading] = useState(false);
  const [mySubmitLoadError, setMySubmitLoadError] = useState<string | null>(null);
  const [mySubmitRequestedPage, setMySubmitRequestedPage] = useState(() => favoriteRestoreSnapshot?.mySubmitRequestedPage ?? 1);
  const [isMySubmitPageJumpEditing, setIsMySubmitPageJumpEditing] = useState(false);
  const [mySubmitPageJumpDraft, setMySubmitPageJumpDraft] = useState('1');
  const [taggedPostPage, setTaggedPostPage] = useState<CommunityPostPage>(createEmptySolveCommunityPage());
  const [isTaggedPostLoading, setIsTaggedPostLoading] = useState(false);
  const [taggedPostLoadError, setTaggedPostLoadError] = useState<string | null>(null);
  const [taggedPostRequestedPage, setTaggedPostRequestedPage] = useState(() => favoriteRestoreSnapshot?.taggedPostRequestedPage ?? 1);
  const [isTaggedPostPageJumpEditing, setIsTaggedPostPageJumpEditing] = useState(false);
  const [taggedPostPageJumpDraft, setTaggedPostPageJumpDraft] = useState('1');
  const [relatedModalState, setRelatedModalState] = useState<SolveRelatedModalState>(null);
  const [sql, setSql] = useState(() => favoriteRestoreSnapshot?.sql ?? '');
  const [sqlEditorFontSize, setSqlEditorFontSize] = useState(SQL_EDITOR_DEFAULT_FONT_SIZE);
  const getPanelTitle = (panelKey: PanelKey) =>
    panelKey === 'editor' ? `${getDbmsLabel(selectedDbms)} 에디터` : panelLabels[panelKey];
  const selectedDdl = useMemo(() => resolveProblemDdl(problemDetail, selectedDbms), [problemDetail, selectedDbms]);
  const ddlAutocompleteItems = useMemo(() => extractAutocompleteItemsFromDdl(selectedDdl), [selectedDdl]);
  const sqlHighlightTableNames = useMemo(
    () => new Set(ddlAutocompleteItems.filter((item) => item.kind === 'table').map((item) => item.value.toLowerCase())),
    [ddlAutocompleteItems],
  );
  const sqlHighlightColumnNames = useMemo(
    () => new Set(ddlAutocompleteItems.filter((item) => item.kind === 'column').map((item) => item.value.toLowerCase())),
    [ddlAutocompleteItems],
  );
  const pickerHighlightRanges = useMemo(() => {
    if (executionPickerState == null) {
      return [] as SqlHighlightRange[];
    }

    const selectedOption = executionPickerState.options[executionPickerState.selectedIndex];
    if (!selectedOption) {
      return [] as SqlHighlightRange[];
    }

    return selectedOption.segments.map((segment) => ({
      start: segment.start,
      end: segment.end,
    }));
  }, [executionPickerState]);
  const highlightedSql = useMemo(
    () => renderHighlightedSql(sql, sqlHighlightTableNames, sqlHighlightColumnNames, pickerHighlightRanges),
    [pickerHighlightRanges, sql, sqlHighlightColumnNames, sqlHighlightTableNames],
  );
  const updateSqlEditorSelection = useCallback((selectionStart: number, selectionEnd = selectionStart) => {
    sqlEditorSelectionRef.current = {
      start: selectionStart,
      end: selectionEnd,
    };
  }, []);
  const syncSqlEditorSelectionFromElement = useCallback((element: HTMLTextAreaElement | null) => {
    if (!element) {
      return;
    }

    const selectionStart = element.selectionStart ?? element.value.length;
    const selectionEnd = element.selectionEnd ?? selectionStart;
    updateSqlEditorSelection(selectionStart, selectionEnd);
  }, [updateSqlEditorSelection]);
  const handleSqlEditorRef = useCallback((element: HTMLTextAreaElement | null) => {
    sqlEditorRef.current = element;
    setSqlEditorElement(element);
    syncSqlEditorSelectionFromElement(element);
  }, [syncSqlEditorSelectionFromElement]);
  const executionStatementMarkerRuns = useMemo(() => {
    const currentStatementSegments = parseSqlStatements(sql);
    const remainingRuns = executionRuns.filter((run) => run.status !== 'idle');

    return currentStatementSegments.flatMap((segment) => {
      const matchedRunIndex = remainingRuns.findIndex((run) => run.sql === segment.sql);
      if (matchedRunIndex < 0) {
        return [];
      }

      const [matchedRun] = remainingRuns.splice(matchedRunIndex, 1);
      return [
        {
          key: matchedRun.key,
          status: matchedRun.status,
          startOffset: segment.start,
        },
      ];
    });
  }, [executionRuns, sql]);
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
  const editorPortalTarget = sqlEditorElement?.ownerDocument?.body ?? document.body;

  useEffect(() => {
    clearFavoriteRestoreSnapshot('problemSolve');
  }, []);

  const displayProblemNumber = problemDetail?.problemId ?? problem.problemNumber ?? problemId;
  const displayProblemTitle =
    problemDetail?.title ?? problem.title ?? '문제';
  const taggedPostPrimarySearchTerm = displayProblemNumber;
  const taggedPostFallbackSearchTerm = getSolveRelatedCommunitySearchTerm(displayProblemNumber);

  const shouldRenderPanel = (panelKey: PanelKey) => panelKey === 'editor' || submitMessage != null || submitProgressSteps.length > 0;
  const visibleFloatingPanels = panelOrder.filter(
    (panelKey) =>
      shouldRenderPanel(panelKey) && panelVisibility[panelKey] && detachedPanels[panelKey] && !externalWindowPanels[panelKey],
  );
  const visibleExternalWindows = panelOrder.filter(
    (panelKey) => shouldRenderPanel(panelKey) && panelVisibility[panelKey] && externalWindowPanels[panelKey],
  );
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

  const restoreEditorSelection = (selectionStart: number, selectionEnd: number) => {
    requestAnimationFrame(() => {
      if (!sqlEditorRef.current) {
        return;
      }

      sqlEditorRef.current.focus();
      sqlEditorRef.current.setSelectionRange(selectionStart, selectionEnd);
      updateSqlEditorSelection(selectionStart, selectionEnd);
    });
  };

  useEffect(() => {
    if (favoriteSelectionRestoreRef.current == null || contentTab !== 'problem' || sqlEditorElement == null) {
      return;
    }

    const nextSelectionStart = Math.min(favoriteSelectionRestoreRef.current.start, sql.length);
    const nextSelectionEnd = Math.min(favoriteSelectionRestoreRef.current.end, sql.length);
    favoriteSelectionRestoreRef.current = null;
    restoreEditorSelection(nextSelectionStart, nextSelectionEnd);
  }, [contentTab, restoreEditorSelection, sql, sqlEditorElement]);

  const registerExecutionResultItemRef = (key: string, element: HTMLDivElement | null) => {
    executionResultItemRefs.current[key] = element;
  };

  const focusExecutionRun = (runKey: string) => {
    requestAnimationFrame(() => {
      const targetElement = executionResultItemRefs.current[runKey];
      if (!targetElement) {
        return;
      }

      targetElement.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
  };

  const openAuthOverlay = (mode: SolveAuthOverlayMode = 'login') => {
    setIsExecuting(false);
    setIsSubmitting(false);
    setAuthOverlayMode(mode);
  };

  const closeAuthOverlay = () => {
    setAuthOverlayMode(null);
  };

  const handleAuthenticatedInOverlay = () => {
    const restoredAuthReturn = consumeSolvePageAuthReturn(problemId);
    if (restoredAuthReturn?.sql != null) {
      setSql(restoredAuthReturn.sql);
    }
    if (restoredAuthReturn?.selectedDbms != null) {
      setSelectedDbms(restoredAuthReturn.selectedDbms);
    }
    setAuthOverlayMode(null);
  };

  const closeExecutionPicker = (restoreSelection: boolean) => {
    setExecutionPickerState((current) => {
      if (current != null && restoreSelection) {
        restoreEditorSelection(current.selectionStart, current.selectionEnd);
      }

      return null;
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
    const restoredAuthReturn = consumeSolvePageAuthReturn(problemId);
    const favoriteSelection = favoriteRestoreSnapshot?.editorSelection ?? null;

    favoriteSelectionRestoreRef.current = restoredAuthReturn?.sql ? null : favoriteSelection;
    setSql(restoredAuthReturn?.sql ?? favoriteRestoreSnapshot?.sql ?? '');
    updateSqlEditorSelection(
      restoredAuthReturn?.sql ? 0 : favoriteSelection?.start ?? 0,
      restoredAuthReturn?.sql ? 0 : favoriteSelection?.end ?? favoriteSelection?.start ?? 0,
    );
    setExecutionRuns([]);
    setExecutionStatementMarkerLayout(null);
    setIsExecuting(false);
    setIsSubmitting(false);
    setSubmitMessage(null);
    setSubmitProgressSteps([]);
    setExecutionPickerState(null);
    setPanelVisibility((current) => ({
      ...current,
      submit: false,
    }));
    setDetachedPanels((current) => ({
      ...current,
      submit: false,
    }));
    setExternalWindowPanels((current) => ({
      ...current,
      submit: false,
    }));
    setEditorFloatingOpacity(FLOATING_EDITOR_BACKGROUND_MAX_ALPHA);
    const restoredSelectedDbms =
      restoredAuthReturn?.selectedDbms != null && availableDbms.includes(restoredAuthReturn.selectedDbms)
        ? restoredAuthReturn.selectedDbms
        : null;
    const favoriteSelectedDbms =
      favoriteRestoreSnapshot?.selectedDbms != null && availableDbms.includes(favoriteRestoreSnapshot.selectedDbms)
        ? favoriteRestoreSnapshot.selectedDbms
        : null;

    setSelectedDbms(restoredSelectedDbms ?? favoriteSelectedDbms ?? resolvePreferredDbms(availableDbms, problem.dbmsOptions, defaultDbms ?? null));
  }, [availableDbms, defaultDbms, favoriteRestoreSnapshot, problem.dbmsOptions, problemId, updateSqlEditorSelection]);

  useEffect(() => {
    const nextContentTab = readSolveContentTabFromSearch(locationSearch);

    setContentTab((currentTab) => (currentTab === nextContentTab ? currentTab : nextContentTab));
  }, [locationSearch]);

  useEffect(() => {
    const nextPath = buildSolveContentTabPath(problemId, contentTab);
    const currentPath = `${window.location.pathname}${window.location.search}`;

    if (currentPath !== nextPath) {
      window.history.replaceState(window.history.state ?? {}, '', nextPath);
    }
  }, [contentTab, problemId]);

  useEffect(() => {
    const restoredContentTab = favoriteRestoreSnapshot?.contentTab ?? readSolveContentTabFromSearch(window.location.search);
    const restoredMySubmitPage = favoriteRestoreSnapshot?.mySubmitRequestedPage ?? 1;
    const restoredTaggedPostPage = favoriteRestoreSnapshot?.taggedPostRequestedPage ?? 1;

    setContentTab(restoredContentTab);
    setMySubmitHistoryPage(createEmptySolveSubmitHistoryPage());
    setIsMySubmitLoading(false);
    setMySubmitLoadError(null);
    setMySubmitRequestedPage(restoredMySubmitPage);
    setIsMySubmitPageJumpEditing(false);
    setMySubmitPageJumpDraft(String(restoredMySubmitPage));
    setTaggedPostPage(createEmptySolveCommunityPage());
    setIsTaggedPostLoading(false);
    setTaggedPostLoadError(null);
    setTaggedPostRequestedPage(restoredTaggedPostPage);
    setIsTaggedPostPageJumpEditing(false);
    setTaggedPostPageJumpDraft(String(restoredTaggedPostPage));
    setRelatedModalState(null);
  }, [favoriteRestoreSnapshot, problemId]);

  useEffect(() => {
    if (isMySubmitPageJumpEditing) {
      return;
    }

    setMySubmitPageJumpDraft(String(mySubmitHistoryPage.currentPage));
  }, [isMySubmitPageJumpEditing, mySubmitHistoryPage.currentPage]);

  useEffect(() => {
    if (isTaggedPostPageJumpEditing) {
      return;
    }

    setTaggedPostPageJumpDraft(String(taggedPostPage.currentPage));
  }, [isTaggedPostPageJumpEditing, taggedPostPage.currentPage]);

  useEffect(() => {
    if (contentTab !== 'submissions') {
      return;
    }

    if (!isReady) {
      return;
    }

    if (!isAuthenticated || !userId) {
      setMySubmitHistoryPage(createEmptySolveSubmitHistoryPage());
      setIsMySubmitLoading(false);
      setMySubmitLoadError(null);
      return;
    }

    let cancelled = false;
    setIsMySubmitLoading(true);
    setMySubmitLoadError(null);

    void fetchSubmitHistories({
      page: mySubmitRequestedPage,
      submitId: '',
      query: userId,
      dbms: selectedDbms,
      problemId: displayProblemNumber,
      judge: 'all',
      costSort: 'none',
      planFiltersByDbms: createEmptySolvePlanFiltersByDbms(),
    })
      .then((page) => {
        if (cancelled) {
          return;
        }

        setMySubmitHistoryPage(page);
        if (page.currentPage !== mySubmitRequestedPage) {
          setMySubmitRequestedPage(page.currentPage);
        }
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        setMySubmitLoadError(error instanceof Error ? error.message : '내 제출을 불러오지 못했다.');
      })
      .finally(() => {
        if (!cancelled) {
          setIsMySubmitLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [contentTab, displayProblemNumber, isAuthenticated, isReady, mySubmitRequestedPage, selectedDbms, userId]);

  useEffect(() => {
    if (contentTab !== 'community') {
      return;
    }

    let cancelled = false;
    setIsTaggedPostLoading(true);
    setTaggedPostLoadError(null);

    async function loadTaggedPosts() {
      try {
        let nextPage = await fetchCommunityPosts({
          page: taggedPostRequestedPage,
          search: taggedPostPrimarySearchTerm,
          tag: '',
          category: 'all',
          sortKey: 'default',
        });

        if (
          nextPage.totalCount === 0
          && taggedPostFallbackSearchTerm !== taggedPostPrimarySearchTerm
          && taggedPostFallbackSearchTerm.trim() !== ''
        ) {
          nextPage = await fetchCommunityPosts({
            page: taggedPostRequestedPage,
            search: taggedPostFallbackSearchTerm,
            tag: '',
            category: 'all',
            sortKey: 'default',
          });
        }

        if (cancelled) {
          return;
        }

        setTaggedPostPage(nextPage);
        if (nextPage.currentPage !== taggedPostRequestedPage) {
          setTaggedPostRequestedPage(nextPage.currentPage);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        setTaggedPostLoadError(error instanceof Error ? error.message : '태그된 게시글을 불러오지 못했다.');
      } finally {
        if (!cancelled) {
          setIsTaggedPostLoading(false);
        }
      }
    }

    void loadTaggedPosts();

    return () => {
      cancelled = true;
    };
  }, [contentTab, taggedPostFallbackSearchTerm, taggedPostPrimarySearchTerm, taggedPostRequestedPage]);

  useEffect(() => {
    if (!sqlEditorElement) {
      return;
    }

    const textarea = sqlEditorElement;
    textarea.style.height = '0px';
    textarea.style.height = `${Math.max(SQL_EDITOR_MIN_HEIGHT, textarea.scrollHeight)}px`;
  }, [collapsedCards.editor, sql, sqlEditorElement, sqlEditorFontSize]);

  useEffect(() => {
    const textarea = sqlEditorElement;
    if (!textarea || executionStatementMarkerRuns.length === 0) {
      setExecutionStatementMarkerLayout(null);
      return;
    }

    let animationFrameId = 0;
    let resizeObserver: ResizeObserver | null = null;
    const ownerWindow = textarea.ownerDocument.defaultView ?? window;
    const measure = () => {
      if (!sqlEditorElement) {
        return;
      }

      setExecutionStatementMarkerLayout(
        measureExecutionStatementMarkerLayout(
          sqlEditorElement,
          sql,
          executionStatementMarkerRuns.map((marker) => marker.startOffset),
          sqlEditorFontSize,
        ),
      );
    };
    const scheduleMeasure = () => {
      ownerWindow.cancelAnimationFrame(animationFrameId);
      animationFrameId = ownerWindow.requestAnimationFrame(() => {
        animationFrameId = ownerWindow.requestAnimationFrame(measure);
      });
    };

    scheduleMeasure();
    ownerWindow.addEventListener('resize', scheduleMeasure);

    if (typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => {
        scheduleMeasure();
      });
      resizeObserver.observe(textarea);
    }

    return () => {
      ownerWindow.cancelAnimationFrame(animationFrameId);
      ownerWindow.removeEventListener('resize', scheduleMeasure);
      resizeObserver?.disconnect();
    };
  }, [
    detachedPanels.editor,
    executionStatementMarkerRuns,
    externalWindowPanels.editor,
    panelVisibility.editor,
    sql,
    sqlEditorElement,
    sqlEditorFontSize,
    visibleExternalWindows.length,
    visibleFloatingPanels.length,
  ]);

  useEffect(() => {
    const wasAuthenticated = previousAuthenticationStateRef.current;

    if (
      wasAuthenticated &&
      !isAuthenticated &&
      (sql.trim() !== '' || executionRuns.length > 0 || submitProgressSteps.length > 0 || submitMessage != null)
    ) {
      openAuthOverlay('login');
    }

    previousAuthenticationStateRef.current = isAuthenticated;
  }, [executionRuns.length, isAuthenticated, sql, submitMessage, submitProgressSteps.length]);

  useEffect(() => {
    if (autocompleteState == null || !sqlEditorRef.current) {
      return;
    }

    syncSqlEditorSelectionFromElement(sqlEditorRef.current);
    updateAutocompleteState(sqlEditorRef.current.value, sqlEditorSelectionRef.current.start);
  }, [autocompleteState != null, sqlEditorFontSize, syncSqlEditorSelectionFromElement]);

  useEffect(() => {
    if (executionPickerState == null || !sqlEditorRef.current) {
      return;
    }

    restoreEditorSelection(executionPickerState.selectionStart, executionPickerState.selectionEnd);
  }, [executionPickerState]);

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

      if (message.type === 'problem.submit.progress') {
        const progressMessage = message as ProblemSubmitProgressMessage;
        if (progressMessage.problemId !== problemId || !progressMessage.status || !progressMessage.message) {
          return;
        }

        setSubmitMessage(null);
        if (progressMessage.stepKey) {
          setSubmitProgressSteps((current) =>
            upsertSubmitProgressStep(
              current,
              createSubmitProgressStep(
                progressMessage.stepKey,
                progressMessage.status,
                progressMessage.message,
                Array.isArray(progressMessage.detailLines) ? progressMessage.detailLines : [],
              ),
            ),
          );
        }
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

      if (message.type === 'problem.submit.result') {
        const nextSubmitMessage = (message as ProblemSocketMessage).message ?? '제출을 기록하지 못했다.';
        if (isAuthenticationRequiredMessage(nextSubmitMessage)) {
          setIsSubmitting(false);
          setSubmitProgressSteps([]);
          setSubmitMessage(null);
          openAuthOverlay('login');
          return;
        }

        setIsSubmitting(false);
        setSubmitMessage((message as ProblemSocketMessage).success === false && nextSubmitMessage !== '오답' ? nextSubmitMessage : null);
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
        const nextExecutionResult =
          message.type === 'error'
            ? createProblemExecutionError(((message as ProblemSocketMessage).message ?? '문제 실행에 실패했다.'))
            : toProblemExecutionResult(message as ProblemSocketMessage);

        if (executionResponseResolverRef.current) {
          const resolveExecution = executionResponseResolverRef.current;
          executionResponseResolverRef.current = null;
          resolveExecution(nextExecutionResult);
          return;
        }

        if (ignoredExecutionResponseCountRef.current > 0) {
          ignoredExecutionResponseCountRef.current -= 1;
          return;
        }

        if (isAuthenticationRequiredMessage(nextExecutionResult.message)) {
          openAuthOverlay('login');
          return;
        }

        setIsExecuting(false);
        const nextRun = createSingleExecutionStatementRun(sql, nextExecutionResult);
        setExecutionRuns([nextRun]);
        setCollapsedCards((current) => ({
          ...current,
          execute: false,
        }));
        setPanelVisibility((current) => ({
          ...current,
          editor: true,
        }));
      }
    });

    return () => {
      unsubscribe();
      sendSessionSocketMessageIfOpen({
        type: 'problem.leave',
        problemId,
      });
    };
  }, [problemId, updateSqlEditorSelection]);

  useEffect(() => {
    setIsSubmitting(false);
    setSubmitMessage(null);
    setSubmitProgressSteps([]);
    setPanelVisibility((current) => ({
      ...current,
      submit: false,
    }));
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
      const viewportPadding = 12;
      const minWidth = panelMinWidths[floatingResizeState.panelKey];
      const minHeight = panelMinHeights[floatingResizeState.panelKey];
      const deltaX = event.clientX - floatingResizeState.startX;
      const deltaY = event.clientY - floatingResizeState.startY;
      let nextLeft = floatingResizeState.startLeft;
      let nextTop = floatingResizeState.startTop;
      let nextWidth = floatingResizeState.startWidth;
      let nextHeight = floatingResizeState.startHeight;

      if (floatingResizeState.direction.includes('e')) {
        nextWidth = clamp(
          floatingResizeState.startWidth + deltaX,
          minWidth,
          window.innerWidth - floatingResizeState.startLeft - viewportPadding,
        );
      }

      if (floatingResizeState.direction.includes('s')) {
        nextHeight = clamp(
          floatingResizeState.startHeight + deltaY,
          minHeight,
          window.innerHeight - floatingResizeState.startTop - viewportPadding,
        );
      }

      if (floatingResizeState.direction.includes('w')) {
        const nextRight = floatingResizeState.startLeft + floatingResizeState.startWidth;
        nextLeft = clamp(
          floatingResizeState.startLeft + deltaX,
          viewportPadding,
          nextRight - minWidth,
        );
        nextWidth = clamp(nextRight - nextLeft, minWidth, window.innerWidth - nextLeft - viewportPadding);
      }

      if (floatingResizeState.direction.includes('n')) {
        const nextBottom = floatingResizeState.startTop + floatingResizeState.startHeight;
        nextTop = clamp(
          floatingResizeState.startTop + deltaY,
          viewportPadding,
          nextBottom - minHeight,
        );
        nextHeight = clamp(nextBottom - nextTop, minHeight, window.innerHeight - nextTop - viewportPadding);
      }

      setFloatingLayouts((current) => ({
        ...current,
        [floatingResizeState.panelKey]: {
          ...current[floatingResizeState.panelKey],
          left: nextLeft,
          top: nextTop,
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
  }, [floatingResizeState]);

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

    syncSqlEditorSelectionFromElement(sqlEditorRef.current);
    if (executionPickerState != null) {
      setExecutionPickerState(null);
    }

    updateAutocompleteState(sqlEditorRef.current.value, sqlEditorSelectionRef.current.start);
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
      updateSqlEditorSelection(nextCaretIndex, nextCaretIndex);
    });
  };

  const handleEditorChange = (nextSql: string, caretIndex: number) => {
    if (executionPickerState != null) {
      setExecutionPickerState(null);
    }

    updateSqlEditorSelection(caretIndex, caretIndex);
    setSql(nextSql);
    updateAutocompleteState(nextSql, caretIndex);
  };

  const changeSqlEditorFontSize = (delta: number) => {
    setSqlEditorFontSize((current) => clamp(current + delta, SQL_EDITOR_MIN_FONT_SIZE, SQL_EDITOR_MAX_FONT_SIZE));
  };

  const handleSubmit = async () => {
    if (executionPickerState != null) {
      if (executionPickerState.mode === 'submit') {
        confirmExecutionPickerSelection();
        return;
      }

      closeExecutionPicker(false);
    }

    if (!isAuthenticated) {
      setSubmitProgressSteps([]);
      setSubmitMessage(null);
      openAuthOverlay('login');
      return;
    }

    if (selectedDbms !== 'postgresql') {
      setSubmitProgressSteps([]);
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
      setSubmitProgressSteps([]);
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

    const parsedStatements = parseSqlStatements(sql);
    if (parsedStatements.length === 0) {
      setSubmitProgressSteps([]);
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

    const caretIndex = Math.min(sqlEditorSelectionRef.current.start, sql.length);
    const submitOptions = createSubmitPickerOptions(sql, caretIndex);

    if (parsedStatements.length > 1 && submitOptions.length > 0 && openSubmitPicker(caretIndex)) {
      return;
    }

    if (submitOptions.length === 0) {
      setSubmitProgressSteps([]);
      setSubmitMessage('제출 가능한 SELECT 구문이 없다.');
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

    await submitStatementSegments(submitOptions[0].segments);
  };

  const requestExecutionResult = async (statementSql: string, page = 1) =>
    new Promise<ProblemExecutionResult>(async (resolve, reject) => {
      executionResponseResolverRef.current = resolve;

      try {
        await sendSessionSocketMessage({
          type: page === 1 ? 'problem.execute' : 'problem.execute.page',
          problemId,
          sql: statementSql,
          dbms: selectedDbms,
          page,
          pageSize: EXECUTION_RESULT_PAGE_SIZE,
        });
      } catch (error) {
        executionResponseResolverRef.current = null;
        reject(error);
      }
    });

  const requestExecutionResultPage = async (item: ExecutionStatementRun, page: number) => {
    try {
      const nextResult = await requestExecutionResult(item.sql, page);

      if (isAuthenticationRequiredMessage(nextResult.message)) {
        openAuthOverlay('login');
        return;
      }

      setExecutionRuns((current) =>
        current.map((run) =>
          run.key === item.key
            ? {
                ...run,
                status: nextResult.success ? 'success' : 'error',
                result: nextResult,
              }
            : run,
        ),
      );
    } catch (error) {
      const isSessionValid = await syncSession();
      if (!isSessionValid) {
        openAuthOverlay('login');
        return;
      }

      setExecutionRuns((current) =>
        current.map((run) =>
          run.key === item.key
            ? {
                ...run,
                status: 'error',
                result: createProblemExecutionError(error instanceof SessionSocketError ? error.message : '문제 실행 연결에 실패했다.'),
              }
            : run,
        ),
      );
    }
  };

  const runExecutionStatements = async (statementSegments: SqlStatementSegment[]) => {
    try {
      executionStopRequestedRef.current = false;
      setIsExecuting(true);
      setExecutionRuns(createExecutionStatementRuns(statementSegments, 0));
      setCollapsedCards((current) => ({
        ...current,
        execute: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));

      for (const [statementIndex, statementSegment] of statementSegments.entries()) {
        const nextResult = await requestExecutionResult(statementSegment.sql);
        if (executionStopRequestedRef.current) {
          break;
        }

        if (isAuthenticationRequiredMessage(nextResult.message)) {
          setExecutionRuns((current) =>
            current.map((run) =>
              run.status === 'running'
                ? {
                    ...run,
                    status: 'idle',
                    result: null,
                  }
                : run,
            ),
          );
          openAuthOverlay('login');
          break;
        }
        const shouldContinue = !executionStopRequestedRef.current;
        const completedRunKey = createExecutionStatementRunKey(statementSegment.start, statementSegment.end);

        setExecutionRuns((current) =>
          current.map((run, runIndex) => {
            if (runIndex === statementIndex) {
              return {
                ...run,
                status: nextResult.success ? 'success' : 'error',
                result: nextResult,
              };
            }

            if (runIndex === statementIndex + 1 && shouldContinue) {
              return {
                ...run,
                status: 'running',
              };
            }

            return run;
          }),
        );
        if (!shouldContinue) {
          break;
        }
      }
    } catch (error) {
      const isSessionValid = await syncSession();
      if (!isSessionValid) {
        setExecutionRuns((current) =>
          current.map((run) =>
            run.status === 'running'
              ? {
                  ...run,
                  status: 'idle',
                  result: null,
                }
              : run,
          ),
        );
        openAuthOverlay('login');
        return;
      }

      const nextErrorResult = createProblemExecutionError(error instanceof SessionSocketError ? error.message : '문제 실행 연결에 실패했다.');
      setExecutionRuns((current) => {
        if (current.length === 0) {
          return [createSingleExecutionStatementRun(sql, nextErrorResult)];
        }

        const runningIndex = current.findIndex((run) => run.status === 'running');
        if (runningIndex === -1) {
          return current;
        }

        return current.map((run, runIndex) =>
          runIndex === runningIndex
            ? {
                ...run,
                status: 'error',
                result: nextErrorResult,
              }
            : run,
        );
      });
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));
    } finally {
      executionStopRequestedRef.current = false;
      executionResponseResolverRef.current = null;
      setIsExecuting(false);
    }
  };

  const openExecutionPicker = (caretIndex: number) => {
    if (!sqlEditorRef.current) {
      return false;
    }

    const options = createExecutionPickerOptions(sql, caretIndex);
    if (options.length === 0) {
      return false;
    }

    const anchor = measureAutocompleteAnchor(sqlEditorRef.current, sql, caretIndex, options.length);
    if (anchor.maxHeight < 40) {
      return false;
    }

    const selectionStart = Math.min(sqlEditorSelectionRef.current.start, sql.length);
    const selectionEnd = Math.min(sqlEditorSelectionRef.current.end, sql.length);
    setAutocompleteState(null);
    setExecutionPickerState({
      mode: 'execute',
      options,
      selectedIndex: 0,
      selectionStart,
      selectionEnd,
      left: anchor.left,
      top: anchor.top,
      maxWidth: Math.max(anchor.maxWidth, 320),
      maxHeight: anchor.maxHeight,
    });
    return true;
  };

  const openSubmitPicker = (caretIndex: number) => {
    if (!sqlEditorRef.current) {
      return false;
    }

    const options = createSubmitPickerOptions(sql, caretIndex);
    if (options.length === 0) {
      return false;
    }

    const anchor = measureAutocompleteAnchor(sqlEditorRef.current, sql, caretIndex, options.length);
    if (anchor.maxHeight < 40) {
      return false;
    }

    const selectionStart = Math.min(sqlEditorSelectionRef.current.start, sql.length);
    const selectionEnd = Math.min(sqlEditorSelectionRef.current.end, sql.length);
    setAutocompleteState(null);
    setExecutionPickerState({
      mode: 'submit',
      options,
      selectedIndex: 0,
      selectionStart,
      selectionEnd,
      left: anchor.left,
      top: anchor.top,
      maxWidth: Math.max(anchor.maxWidth, 320),
      maxHeight: anchor.maxHeight,
    });
    return true;
  };

  const submitStatementSegments = async (statementSegments: SqlStatementSegment[]) => {
    const submitSql = statementSegments.map((statement) => statement.sql).join(';\n');

    if (submitSql.trim().length === 0) {
      setSubmitProgressSteps([]);
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

    setIsSubmitting(true);
    setSubmitMessage(null);
    setSubmitProgressSteps([]);
    setCollapsedCards((current) => ({
      ...current,
      submit: false,
    }));
    setPanelVisibility((current) => ({
      ...current,
      submit: true,
    }));
    focusPanelSection(() => submitPanelRef.current);

    try {
      await sendSessionSocketMessage({
        type: 'problem.submit',
        problemId,
        sql: submitSql,
        dbms: selectedDbms,
      });
    } catch (error) {
      setIsSubmitting(false);
      setSubmitProgressSteps([]);

      const isSessionValid = await syncSession();
      if (!isSessionValid) {
        openAuthOverlay('login');
        return;
      }

      setSubmitMessage(error instanceof SessionSocketError ? error.message : '제출 연결에 실패했다.');
    }
  };

  const confirmExecutionPickerSelection = () => {
    if (executionPickerState == null) {
      return;
    }

    const selectedOption = executionPickerState.options[executionPickerState.selectedIndex];
    if (!selectedOption) {
      return;
    }

    closeExecutionPicker(true);
    if (executionPickerState.mode === 'submit') {
      void submitStatementSegments(selectedOption.segments);
      return;
    }

    void runExecutionStatements(selectedOption.segments);
  };

  const confirmExecutionPickerOption = (option: SqlExecutionPickerOption) => {
    closeExecutionPicker(true);
    if (executionPickerState?.mode === 'submit') {
      void submitStatementSegments(option.segments);
      return;
    }

    void runExecutionStatements(option.segments);
  };

  const executeSql = async () => {
    if (executionPickerState != null) {
      if (executionPickerState.mode === 'execute') {
        confirmExecutionPickerSelection();
        return;
      }

      closeExecutionPicker(false);
    }

    if (!isAuthenticated) {
      openAuthOverlay('login');
      return;
    }

    if (selectedDbms !== 'postgresql') {
      setExecutionRuns([
        createSingleExecutionStatementRun(sql, createProblemExecutionError('인터랙티브 실행은 PostgreSQL만 지원한다.')),
      ]);
      setCollapsedCards((current) => ({
        ...current,
        execute: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));
      return;
    }

    if (sql.trim().length === 0) {
      setExecutionRuns([createSingleExecutionStatementRun(sql, createProblemExecutionError('실행할 SQL을 입력해야 한다.'))]);
      setCollapsedCards((current) => ({
        ...current,
        execute: false,
      }));
      setPanelVisibility((current) => ({
        ...current,
        editor: true,
      }));
      return;
    }

    const caretIndex = Math.min(sqlEditorSelectionRef.current.start, sql.length);
    if (openExecutionPicker(caretIndex)) {
      return;
    }

    const executableSegments = parseSqlStatements(sql);
    if (executableSegments.length === 0) {
      setExecutionRuns([createSingleExecutionStatementRun(sql, createProblemExecutionError('실행할 SQL을 입력해야 한다.'))]);
      return;
    }

    await runExecutionStatements(executableSegments);
  };

  const handleStopExecution = () => {
    if (!isExecuting) {
      return;
    }

    executionStopRequestedRef.current = true;
    ignoredExecutionResponseCountRef.current += 1;
    const resolveExecution = executionResponseResolverRef.current;
    executionResponseResolverRef.current = null;
    setIsExecuting(false);
    setExecutionRuns((current) =>
      current.map((run) =>
        run.status === 'running'
          ? {
              ...run,
              status: 'idle',
              result: null,
            }
          : run,
      ),
    );
    resolveExecution?.(createProblemExecutionError('중지됨'));
    void sendSessionSocketMessage({
      type: 'problem.execute.stop',
      problemId,
      dbms: selectedDbms,
    }).catch(() => {
    });
  };

  const handleFloatingPaneWheel = (event: ReactWheelEvent<HTMLDivElement>) => {
    const scrollableElement = resolveWheelScrollableElement(event.target, event.currentTarget, event.deltaY);

    event.preventDefault();
    event.stopPropagation();

    if (scrollableElement) {
      scrollableElement.scrollTop += event.deltaY;
      scrollableElement.scrollLeft += event.deltaX;
    }
  };

  const handleEditorKeyDown = (event: ReactKeyboardEvent<HTMLTextAreaElement>) => {
    const selectionStart = event.currentTarget.selectionStart ?? 0;
    const selectionEnd = event.currentTarget.selectionEnd ?? selectionStart;
    updateSqlEditorSelection(selectionStart, selectionEnd);

    if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key === 'Enter') {
      event.preventDefault();
      setAutocompleteState(null);
      if (executionPickerState != null && executionPickerState.mode === 'execute') {
        closeExecutionPicker(false);
      }
      void handleSubmit();
      return;
    }

    if (executionPickerState != null) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        setExecutionPickerState((current) =>
          current == null
            ? current
            : {
                ...current,
                selectedIndex: (current.selectedIndex + 1) % current.options.length,
              },
        );
        return;
      }

      if (event.key === 'ArrowUp') {
        event.preventDefault();
        setExecutionPickerState((current) =>
          current == null
            ? current
            : {
                ...current,
                selectedIndex: (current.selectedIndex - 1 + current.options.length) % current.options.length,
              },
        );
        return;
      }

      if (event.key === 'Tab' || event.key === 'Enter' || ((event.ctrlKey || event.metaKey) && event.key === 'Enter')) {
        event.preventDefault();
        confirmExecutionPickerSelection();
        return;
      }

      if (event.key === 'Escape') {
        event.preventDefault();
        closeExecutionPicker(true);
        return;
      }
    }

    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      setAutocompleteState(null);
      void executeSql();
      return;
    }

    if (!autocompleteState && event.key === 'Tab') {
      event.preventDefault();

      const { nextSql, nextSelectionStart, nextSelectionEnd } = indentSqlEditorValue(sql, selectionStart, selectionEnd);

      setSql(nextSql);
      setAutocompleteState(null);
      updateSqlEditorSelection(nextSelectionStart, nextSelectionEnd);

      requestAnimationFrame(() => {
        if (!sqlEditorRef.current) {
          return;
        }

        sqlEditorRef.current.focus();
        sqlEditorRef.current.setSelectionRange(nextSelectionStart, nextSelectionEnd);
        updateSqlEditorSelection(nextSelectionStart, nextSelectionEnd);
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

  const startFloatingResize = (
    panelKey: PanelKey,
    direction: FloatingResizeDirection,
    event: ReactMouseEvent<HTMLButtonElement>,
  ) => {
    event.preventDefault();
    event.stopPropagation();
    setFloatingResizeState({
      panelKey,
      direction,
      startX: event.clientX,
      startY: event.clientY,
      startLeft: floatingLayouts[panelKey].left,
      startTop: floatingLayouts[panelKey].top,
      startWidth: floatingLayouts[panelKey].width,
      startHeight: floatingLayouts[panelKey].height,
    });
  };

  const renderPanelActions = (panelKey: PanelKey) =>
    panelKey === 'submit' ? null : (
    <div className="solve-pane-actions">
      <button
        type="button"
        className={`mini-toggle solve-pane-action solve-pane-action-icon ${externalWindowPanels[panelKey] ? 'is-selected' : ''}`}
        aria-label={externalWindowPanels[panelKey] ? `Restore ${getPanelTitle(panelKey)} from external window` : `Open ${getPanelTitle(panelKey)} in external window`}
        onClick={() => togglePanelExternalWindow(panelKey)}
      >
        <ExternalWindowIcon />
      </button>
      <button
        type="button"
        className={`mini-toggle solve-pane-action solve-pane-action-icon ${detachedPanels[panelKey] ? 'is-selected' : ''}`}
        aria-label={detachedPanels[panelKey] ? `Restore ${getPanelTitle(panelKey)} from PIP` : `Open ${getPanelTitle(panelKey)} in PIP`}
        onClick={() => togglePanelDetach(panelKey)}
      >
        <PipIcon />
      </button>
      <button
        type="button"
        className="mini-toggle solve-pane-action solve-pane-action-icon"
        aria-label={`Close ${getPanelTitle(panelKey)}`}
        onClick={() => togglePanelVisibility(panelKey)}
      >
        <CloseIcon />
      </button>
    </div>
    );

  const renderFloatingOpacityControl = () => {
    const sliderRange = FLOATING_EDITOR_BACKGROUND_MAX_ALPHA - FLOATING_EDITOR_BACKGROUND_MIN_ALPHA;
    const sliderValue = Math.round(((editorFloatingOpacity - FLOATING_EDITOR_BACKGROUND_MIN_ALPHA) / sliderRange) * 100);

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
            setEditorFloatingOpacity(FLOATING_EDITOR_BACKGROUND_MIN_ALPHA + sliderRange * (nextValue / 100));
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
        <h2 className="solve-detail-section-title solve-pane-title">{getPanelTitle(panelKey)}</h2>
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

  const visibleExecutionRuns = executionRuns.filter((run) => run.status === 'success' || run.status === 'error');

  const renderExecutionInlineRegion = (isFloating: boolean) => {
    if (visibleExecutionRuns.length === 0) {
      return null;
    }

    return (
      <div
        ref={executionPanelRef}
        tabIndex={-1}
        className={`solve-editor-inline-result ${collapsedCards.execute ? 'is-collapsed' : ''} ${isFloating ? 'is-floating' : ''} ${visibleExecutionRuns.length > 0 ? 'has-results' : ''}`.trim()}
      >
        <div className="solve-editor-inline-result-divider" aria-hidden="true" />
        {!collapsedCards.execute ? (
          <div className={`solve-editor-inline-result-body solve-pane-result-stack ${visibleExecutionRuns.length > 0 ? 'has-results' : ''}`.trim()}>
            {visibleExecutionRuns.map((run) => (
              <ExecutionStatementResultItem
                key={run.key}
                item={run}
                onStatusIndicatorClick={() => focusExecutionRun(run.key)}
                registerResultItemRef={registerExecutionResultItemRef}
                onRequestPage={requestExecutionResultPage}
              />
            ))}
          </div>
        ) : null}
      </div>
    );
  };

  const renderEditorPanel = (isFloating: boolean) => {
    const executionStatementMarkers = executionStatementMarkerRuns.map((marker) => ({
      ...marker,
      left: executionStatementMarkerLayout?.left ?? 0,
      top: executionStatementMarkerLayout?.topOffsets[marker.startOffset] ?? 0,
      width: executionStatementMarkerLayout?.width ?? 0,
      height: executionStatementMarkerLayout?.height ?? 0,
      fontSize: executionStatementMarkerLayout?.fontSize ?? sqlEditorFontSize,
    })).filter((marker) => executionStatementMarkerLayout?.topOffsets[marker.startOffset] != null);

    return (
    <section className={`${isFloating ? 'panel-card' : 'solve-surface-section'} solve-pane solve-pane-editor ${isFloating ? 'is-floating' : ''}`}>
      <div className={`solve-detail-section-frame solve-pane-section-frame ${collapsedCards.editor ? 'is-collapsed' : ''} ${isFloating ? 'is-floating' : ''}`.trim()}>
        {!isFloating ? renderCollapseControl('editor') : null}
        <div className="solve-detail-section-main solve-pane-section-main">
          {renderPanelHeader('editor', isFloating)}

          {!collapsedCards.editor ? (
            <div className="solve-editor-stack">
              <div className="solve-editor-toolbar-row">
                <div className="solve-editor-zoom-controls" aria-label="에디터 글씨 크기 조절">
                  <button type="button" className="mini-toggle solve-editor-zoom-button is-increase" onClick={() => changeSqlEditorFontSize(1)}>
                    +
                  </button>
                  <button type="button" className="mini-toggle solve-editor-zoom-button is-decrease" onClick={() => changeSqlEditorFontSize(-1)}>
                    -
                  </button>
                </div>
                <div className="solve-editor-actions">
                  <button
                    type="button"
                    className="mini-toggle solve-editor-zoom-button is-stop"
                    onClick={handleStopExecution}
                    disabled={!isExecuting}
                  >
                    중지
                  </button>
                  <button type="button" className="btn secondary" onClick={executeSql} disabled={sql.trim().length === 0 || isExecuting}>
                    {isExecuting ? '실행 중' : '실행 (Ctrl + Enter)'}
                  </button>
                  <button type="button" className="btn primary" onClick={handleSubmit} disabled={sql.trim().length === 0 || isSubmitting}>
                    {isSubmitting ? '제출 중' : '제출 (Ctrl + Shift + Enter)'}
                  </button>
                </div>
              </div>
              <div className="solve-editor-surface">
                <div
                  className={`solve-editor-surface-body ${isFloating && executionRuns.length > 0 ? 'has-inline-result' : ''}`.trim()}
                >
                  <div className="solve-editor-code-layer">
                    {executionStatementMarkers.length > 0 ? (
                      <div className="solve-editor-statement-status-layer" aria-hidden="true">
                        {executionStatementMarkers.map((marker) => (
                          <button
                            type="button"
                            key={marker.key}
                            className={`solve-editor-statement-status is-${marker.status}`.trim()}
                            style={{
                              left: `${marker.left}px`,
                              top: `${marker.top}px`,
                              width: `${marker.width}px`,
                              height: `${marker.height}px`,
                              fontSize: `${marker.fontSize}px`,
                            }}
                            aria-label="실행 결과 위치로 이동"
                            onClick={() => focusExecutionRun(marker.key)}
                          >
                            {marker.status === 'success' ? (
                              <ExecutionSuccessIcon />
                            ) : marker.status === 'error' ? (
                              <ExecutionErrorIcon />
                            ) : marker.status === 'running' ? (
                              <span className="solve-editor-statement-spinner" />
                            ) : null}
                          </button>
                        ))}
                      </div>
                    ) : null}
                    <pre
                      aria-hidden="true"
                      className={`solve-sql-highlight ${sql.length === 0 ? 'is-empty' : ''}`}
                      style={{ fontSize: `${sqlEditorFontSize}px` }}
                    >
                      {sql.length === 0 ? (
                        <span className="solve-sql-highlight-placeholder">이곳에 SQL을 작성하세요.</span>
                      ) : (
                        highlightedSql
                      )}
                    </pre>
                    <textarea
                      ref={handleSqlEditorRef}
                      className={`solve-sql-editor ${sql.length === 0 ? 'is-empty' : 'has-content'}`}
                      spellCheck={false}
                      wrap="soft"
                      placeholder="이곳에 SQL을 작성하세요."
                      style={{ fontSize: `${sqlEditorFontSize}px` }}
                      value={sql}
                      onChange={(event) => handleEditorChange(event.target.value, event.target.selectionStart ?? event.target.value.length)}
                      onSelect={(event) => syncSqlEditorSelectionFromElement(event.currentTarget)}
                      onClick={refreshAutocompleteFromEditor}
                      onScroll={refreshAutocompleteFromEditor}
                      onWheel={handleEditorWheel}
                      onKeyUp={(event) => {
                        if (executionPickerState != null) {
                          return;
                        }

                        if (['ArrowUp', 'ArrowDown', 'Enter', 'Tab', 'Escape'].includes(event.key)) {
                          return;
                        }

                        refreshAutocompleteFromEditor();
                      }}
                      onKeyDown={handleEditorKeyDown}
                      onBlur={(event) => {
                        syncSqlEditorSelectionFromElement(event.currentTarget);
                        window.setTimeout(() => {
                          setAutocompleteState(null);
                          if (executionPickerState != null) {
                            closeExecutionPicker(false);
                          }
                        }, 120);
                      }}
                      aria-label="에디터"
                    />
                  </div>

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
                        editorPortalTarget,
                      )
                    : null}
                  {executionPickerState
                    ? createPortal(
                        <div
                          className="solve-editor-autocomplete solve-editor-statement-picker"
                          style={{
                            left: `${executionPickerState.left}px`,
                            top: `${executionPickerState.top}px`,
                            maxWidth: `${executionPickerState.maxWidth}px`,
                            maxHeight: `${executionPickerState.maxHeight}px`,
                          }}
                          role="listbox"
                          aria-label={executionPickerState.mode === 'submit' ? 'SQL 제출 구문 선택' : 'SQL 실행 구문 선택'}
                        >
                          {executionPickerState.options.map((option, index) => (
                            <button
                              key={option.key}
                              type="button"
                              className={`solve-editor-autocomplete-item ${index === executionPickerState.selectedIndex ? 'is-selected' : ''}`}
                              role="option"
                              aria-selected={index === executionPickerState.selectedIndex}
                              onMouseEnter={() =>
                                setExecutionPickerState((current) =>
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
                                confirmExecutionPickerOption(option);
                              }}
                            >
                              <span className="solve-editor-autocomplete-value">{option.preview}</span>
                              <span className="solve-editor-autocomplete-detail">{option.label}</span>
                            </button>
                          ))}
                        </div>,
                        editorPortalTarget,
                      )
                    : null}
                </div>

                {renderExecutionInlineRegion(isFloating)}
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </section>
    );
  };

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
            submitProgressSteps.length > 0 || submitMessage ? (
              <div className="solve-submit-panel-stack">
                {submitProgressSteps.length > 0 ? (
                  <div className="solve-submit-progress-list">
                    {submitProgressSteps.map((step) => (
                      <SubmitProgressItem key={step.stepKey} step={step} />
                    ))}
                  </div>
                ) : null}
                {submitMessage ? (
                  <div className="solve-pane-result-stack">
                    <p className={`solve-pane-result-message ${submitMessage === '오답' ? 'is-error' : ''}`.trim()}>{submitMessage}</p>
                  </div>
                ) : null}
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

  const hasRelatedExecutionPlanDetails = (history: SubmitHistoryEntry) =>
    getExecutionPlanDetailGroups(history.dbms, history.executionPlanElement).length > 0;

  const applyMySubmitPageJump = () => {
    const parsedPage = Number.parseInt(mySubmitPageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? mySubmitHistoryPage.currentPage
      : Math.min(mySubmitHistoryPage.totalPages, Math.max(1, parsedPage));

    setMySubmitPageJumpDraft(String(nextPage));
    setIsMySubmitPageJumpEditing(false);

    if (nextPage !== mySubmitHistoryPage.currentPage) {
      setMySubmitRequestedPage(nextPage);
    }
  };

  const cancelMySubmitPageJump = () => {
    setMySubmitPageJumpDraft(String(mySubmitHistoryPage.currentPage));
    setIsMySubmitPageJumpEditing(false);
  };

  const applyTaggedPostPageJump = () => {
    const parsedPage = Number.parseInt(taggedPostPageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? taggedPostPage.currentPage
      : Math.min(taggedPostPage.totalPages, Math.max(1, parsedPage));

    setTaggedPostPageJumpDraft(String(nextPage));
    setIsTaggedPostPageJumpEditing(false);

    if (nextPage !== taggedPostPage.currentPage) {
      setTaggedPostRequestedPage(nextPage);
    }
  };

  const cancelTaggedPostPageJump = () => {
    setTaggedPostPageJumpDraft(String(taggedPostPage.currentPage));
    setIsTaggedPostPageJumpEditing(false);
  };

  const relatedModalContent =
    relatedModalState == null || typeof document === 'undefined'
      ? null
      : createPortal(
          <div
            className="submit-history-modal-overlay"
            role="presentation"
            onMouseDown={(event) => {
              if (event.target === event.currentTarget) {
                setRelatedModalState(null);
              }
            }}
          >
            {relatedModalState.type === 'sql' ? (
              <div className="submit-history-modal" role="dialog" aria-modal="true" aria-label="제출 결과 보기">
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <div className="submit-history-modal-title-row">
                      <strong>제출 결과</strong>
                      <span className={`submit-history-modal-title-status ${relatedModalState.history.success ? 'is-success' : 'is-fail'}`}>
                        {relatedModalState.history.success ? '정답' : '오답'}
                      </span>
                    </div>
                    <span>{`${relatedModalState.history.submitId} · ${relatedModalState.history.userId} · 문제 ${relatedModalState.history.problemId}`}</span>
                    <div className="submit-history-modal-meta">
                      <div className="submit-history-modal-meta-stack">
                        <span className="submit-history-modal-meta-line">{getDbmsLabel(relatedModalState.history.dbms)}</span>
                        {relatedModalState.history.success || relatedModalState.history.cost > 0 ? (
                          <span className="submit-history-modal-meta-line">{formatSolveRelatedCost(relatedModalState.history.cost)}</span>
                        ) : null}
                        <span className="submit-history-modal-meta-line">{formatSolveRelatedSubmittedAt(relatedModalState.history.submittedAt)}</span>
                        {hasRelatedExecutionPlanDetails(relatedModalState.history) ? (
                          <button
                            type="button"
                            className="submit-history-modal-meta-action submit-history-modal-meta-icon"
                            aria-label="실행계획 요소 보기"
                            title="실행계획 요소 보기"
                            onClick={() => setRelatedModalState({ type: 'plan', history: relatedModalState.history })}
                          >
                            ↗
                          </button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                  <button type="button" className="submit-history-modal-close" onClick={() => setRelatedModalState(null)}>
                    닫기
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-sql-modal-body">
                  <pre className="solve-related-sql-viewer" aria-label="제출 SQL">{relatedModalState.history.submittedSql}</pre>
                </div>
              </div>
            ) : (
              <div className="submit-history-modal submit-history-plan-modal" role="dialog" aria-modal="true" aria-label="실행계획 요소 보기">
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <strong>실행계획 요소</strong>
                    <span>{`${relatedModalState.history.userId} · ${getDbmsLabel(relatedModalState.history.dbms)} · 문제 ${relatedModalState.history.problemId}`}</span>
                  </div>
                  <button type="button" className="submit-history-modal-close" onClick={() => setRelatedModalState(null)}>
                    닫기
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-plan-modal-body">
                  <div className="submit-history-plan-modal-summary">
                    <span className="submit-history-plan-modal-label">Cost</span>
                    <strong>{formatSolveRelatedCost(relatedModalState.history.cost)}</strong>
                  </div>

                  {getExecutionPlanDetailGroups(relatedModalState.history.dbms, relatedModalState.history.executionPlanElement).length > 0 ? (
                    <div className="solve-related-plan-group-list">
                      {getExecutionPlanDetailGroups(relatedModalState.history.dbms, relatedModalState.history.executionPlanElement).map((group) => (
                        <div key={`${group.sectionKey}-${group.sectionLabel}`} className="solve-related-plan-group">
                          <span className="solve-related-plan-group-label">{group.sectionLabel}</span>
                          <span className="solve-related-plan-group-values">{group.labels.join(', ')}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="submit-history-empty-state submit-history-modal-empty-state">감지된 대표 실행계획 요소가 없습니다.</div>
                  )}
                </div>
              </div>
            )}
          </div>,
          document.body,
        );

  const renderSubmissionTabPanel = () => {
    if (!isReady) {
      return null;
    }

    if (!isAuthenticated || !userId) {
      return <div className="solve-related-empty-state">로그인 후 내 제출을 확인할 수 있습니다.</div>;
    }

    if (mySubmitLoadError) {
      return <PageLoadFailureState className="solve-related-empty-state" />;
    }

    return (
      <div className="solve-related-tab-panel">
        <div className={`submit-history-table-shell solve-related-table-shell ${isMySubmitLoading ? 'is-loading' : ''}`.trim()}>
          <div className="submit-history-table solve-related-submit-table" role="table" aria-label="내 제출 목록">
            <div className="submit-history-row submit-history-head solve-related-table-head" role="row">
              <div role="columnheader" className="submit-history-head-cell">제출번호</div>
              <div role="columnheader" className="submit-history-head-cell">Handle</div>
              <div role="columnheader" className="submit-history-head-cell">문제 번호</div>
              <div role="columnheader" className="submit-history-head-cell">제출 결과</div>
              <div role="columnheader" className="submit-history-head-cell">Cost</div>
              <div role="columnheader" className="submit-history-head-cell">제출 시각</div>
              <div role="columnheader" className="submit-history-head-cell">실행계획요소</div>
            </div>

            {mySubmitHistoryPage.histories.length === 0 && !isMySubmitLoading ? (
              <div className="submit-history-row submit-history-empty-row" role="row">
                <span className="submit-history-empty-cell" role="cell">이 문제에 대한 내 제출이 없습니다.</span>
              </div>
            ) : (
              mySubmitHistoryPage.histories.map((history) => (
                <article key={history.submitId} className="submit-history-row submit-history-body solve-related-table-row" role="row">
                  <span className="submit-history-cell" role="cell" data-label="제출번호">{history.submitId}</span>
                  <span className="submit-history-cell" role="cell" data-label="Handle">
                    <button
                      type="button"
                      className="submit-history-link-button"
                      onClick={() => navigate(getProfilePath(history.userId))}
                      aria-label={`${history.userId} 프로필로 이동`}
                    >
                      {history.userId}
                    </button>
                  </span>
                  <span className="submit-history-cell" role="cell" data-label="문제 번호">
                    <button
                      type="button"
                      className="submit-history-link-button"
                      onClick={() => navigate(`/problems/${encodeURIComponent(history.problemId)}`)}
                      aria-label={`문제 ${history.problemId}로 이동`}
                    >
                      {history.problemId}
                    </button>
                  </span>
                  <span className="submit-history-cell" role="cell" data-label="제출 결과">
                    <button
                      type="button"
                      className={`submit-history-status-text ${history.success ? 'is-success' : 'is-fail'}`}
                      onClick={() => setRelatedModalState({ type: 'sql', history })}
                    >
                      {history.success ? '정답' : '오답'}
                    </button>
                  </span>
                  <span className="submit-history-cell" role="cell" data-label="Cost">
                    {history.success || history.cost > 0 ? formatSolveRelatedCost(history.cost) : '-'}
                  </span>
                  <span className="submit-history-cell" role="cell" data-label="제출 시각">
                    {formatSolveRelatedSubmittedAt(history.submittedAt)}
                  </span>
                  <span className="submit-history-cell submit-history-cell-plan" role="cell" data-label="실행계획요소">
                    {hasRelatedExecutionPlanDetails(history) ? (
                      <button
                        type="button"
                        className="submit-history-detail-button"
                        aria-label="실행계획 요소 보기"
                        onClick={() => setRelatedModalState({ type: 'plan', history })}
                      >
                        ↗
                      </button>
                    ) : (
                      <span className="submit-history-empty-value">-</span>
                    )}
                  </span>
                </article>
              ))
            )}
          </div>

          {isMySubmitLoading ? (
            <div className="submit-history-loading-overlay" aria-live="polite" aria-label="로딩 중">
              <span className="page-loading-spinner submit-history-loading-badge" aria-hidden="true" />
            </div>
          ) : null}
        </div>

        {mySubmitHistoryPage.totalCount > 0 ? (
          <div className="solve-related-pagination" role="navigation" aria-label="내 제출 페이지">
            <button
              type="button"
              className="solve-related-page-button"
              onClick={() => setMySubmitRequestedPage((page) => Math.max(1, page - 1))}
              disabled={mySubmitHistoryPage.currentPage === 1}
            >
              이전
            </button>

            {isMySubmitPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="solve-related-pagination-input"
                aria-label="이동할 내 제출 페이지 입력"
                value={mySubmitPageJumpDraft}
                onChange={(event) => setMySubmitPageJumpDraft(event.target.value.replace(/\D+/g, ''))}
                onBlur={applyMySubmitPageJump}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    applyMySubmitPageJump();
                    return;
                  }

                  if (event.key === 'Escape') {
                    event.preventDefault();
                    cancelMySubmitPageJump();
                  }
                }}
                autoFocus
              />
            ) : (
              <button
                type="button"
                className="solve-related-pagination-meta"
                aria-label="이동할 내 제출 페이지 입력 열기"
                onClick={() => {
                  setMySubmitPageJumpDraft(String(mySubmitHistoryPage.currentPage));
                  setIsMySubmitPageJumpEditing(true);
                }}
              >
                {`${mySubmitHistoryPage.currentPage} / ${mySubmitHistoryPage.totalPages}`}
              </button>
            )}

            <button
              type="button"
              className="solve-related-page-button"
              onClick={() => setMySubmitRequestedPage((page) => Math.min(mySubmitHistoryPage.totalPages, page + 1))}
              disabled={mySubmitHistoryPage.currentPage >= mySubmitHistoryPage.totalPages}
            >
              다음
            </button>
          </div>
        ) : null}
      </div>
    );
  };

  const renderTaggedPostTabPanel = () => {
    if (taggedPostLoadError) {
      return <PageLoadFailureState className="solve-related-empty-state" />;
    }

    return (
      <div className="solve-related-tab-panel">
        <div className={`submit-history-table-shell solve-related-table-shell ${isTaggedPostLoading ? 'is-loading' : ''}`.trim()}>
          <div className="submit-history-table solve-related-community-table" role="table" aria-label="태그된 게시글 목록">
            <div className="submit-history-row submit-history-head solve-related-table-head" role="row">
              <div role="columnheader" className="submit-history-head-cell">구분</div>
              <div role="columnheader" className="submit-history-head-cell">제목</div>
              <div role="columnheader" className="submit-history-head-cell">Handle</div>
              <div role="columnheader" className="submit-history-head-cell">작성일</div>
              <div role="columnheader" className="submit-history-head-cell">조회수</div>
              <div role="columnheader" className="submit-history-head-cell">좋아요</div>
              <div role="columnheader" className="submit-history-head-cell">댓글</div>
            </div>

            {taggedPostPage.posts.length === 0 && !isTaggedPostLoading ? (
              <div className="submit-history-row submit-history-empty-row" role="row">
                <span className="submit-history-empty-cell" role="cell">이 문제 번호로 태그된 게시글이 없습니다.</span>
              </div>
            ) : (
              taggedPostPage.posts.map((post) => (
                <article key={post.id} className="submit-history-row submit-history-body solve-related-table-row" role="row">
                  <span className="submit-history-cell solve-related-community-category" role="cell" data-label="구분">
                    <span className={`solve-related-community-category-text is-${post.category}`}>{getSolveRelatedCommunityCategoryLabel(post.category)}</span>
                  </span>
                  <div role="cell" className="submit-history-cell solve-related-community-title-cell" data-label="제목">
                    {post.tags.length > 0 ? (
                      <div className="solve-related-community-tags">
                        {post.tags.slice(0, 5).map((tag) => (
                          <span key={tag} className="solve-related-community-tag">#{tag}</span>
                        ))}
                      </div>
                    ) : null}
                    <button
                      type="button"
                      className="solve-related-community-title-link"
                      onClick={() => navigate(getCommunityPostPath(post.id))}
                    >
                      <span className="solve-related-community-title-text">{post.title}</span>
                    </button>
                  </div>
                  <span className="submit-history-cell" role="cell" data-label="Handle">
                    <button
                      type="button"
                      className="submit-history-link-button"
                      onClick={() => navigate(getProfilePath(post.authorHandle))}
                      aria-label={`${post.authorHandle} 프로필로 이동`}
                    >
                      {post.authorHandle}
                    </button>
                  </span>
                  <span className="submit-history-cell" role="cell" data-label="작성일">{formatSolveRelatedBoardDate(post.updatedAt ?? post.createdAt)}</span>
                  <span className="submit-history-cell" role="cell" data-label="조회수">{formatGroupedNumber(post.views)}</span>
                  <span className="submit-history-cell" role="cell" data-label="좋아요">{formatGroupedNumber(post.likes)}</span>
                  <span className="submit-history-cell" role="cell" data-label="댓글">{formatGroupedNumber(post.comments)}</span>
                </article>
              ))
            )}
          </div>

          {isTaggedPostLoading ? (
            <div className="submit-history-loading-overlay" aria-live="polite" aria-label="로딩 중">
              <span className="page-loading-spinner submit-history-loading-badge" aria-hidden="true" />
            </div>
          ) : null}
        </div>

        {taggedPostPage.totalCount > 0 ? (
          <div className="solve-related-pagination" role="navigation" aria-label="태그된 게시글 페이지">
            <button
              type="button"
              className="solve-related-page-button"
              onClick={() => setTaggedPostRequestedPage((page) => Math.max(1, page - 1))}
              disabled={taggedPostPage.currentPage === 1}
            >
              이전
            </button>

            {isTaggedPostPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="solve-related-pagination-input"
                aria-label="이동할 태그된 게시글 페이지 입력"
                value={taggedPostPageJumpDraft}
                onChange={(event) => setTaggedPostPageJumpDraft(event.target.value.replace(/\D+/g, ''))}
                onBlur={applyTaggedPostPageJump}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    applyTaggedPostPageJump();
                    return;
                  }

                  if (event.key === 'Escape') {
                    event.preventDefault();
                    cancelTaggedPostPageJump();
                  }
                }}
                autoFocus
              />
            ) : (
              <button
                type="button"
                className="solve-related-pagination-meta"
                aria-label="이동할 태그된 게시글 페이지 입력 열기"
                onClick={() => {
                  setTaggedPostPageJumpDraft(String(taggedPostPage.currentPage));
                  setIsTaggedPostPageJumpEditing(true);
                }}
              >
                {`${taggedPostPage.currentPage} / ${taggedPostPage.totalPages}`}
              </button>
            )}

            <button
              type="button"
              className="solve-related-page-button"
              onClick={() => setTaggedPostRequestedPage((page) => Math.min(taggedPostPage.totalPages, page + 1))}
              disabled={taggedPostPage.currentPage >= taggedPostPage.totalPages}
            >
              다음
            </button>
          </div>
        ) : null}
      </div>
    );
  };

  const getFavoriteSnapshot = useCallback(() => ({
    kind: 'problemSolve',
    payload: {
      selectedDbms,
      contentTab,
      sql,
      editorSelection: { ...sqlEditorSelectionRef.current },
      mySubmitRequestedPage,
      taggedPostRequestedPage,
    },
  }), [contentTab, mySubmitRequestedPage, selectedDbms, sql, taggedPostRequestedPage]);

  const renderActiveContentTab = () => {
    if (contentTab === 'submissions') {
      return renderSubmissionTabPanel();
    }

    if (contentTab === 'community') {
      return renderTaggedPostTabPanel();
    }

    return problemDetail ? (
      <ProblemDetailContent
        detail={problemDetail}
        selectedDbms={selectedDbms}
        afterSectionsContent={inlineDetailPanels}
      />
    ) : problemLoadError ? (
      <PageLoadFailureState className="solve-related-empty-state" />
    ) : null;
  };

  const inlineEditorPanel =
    contentTab === 'problem' && panelVisibility.editor && !detachedPanels.editor && !externalWindowPanels.editor
      ? renderEditorPanel(false)
      : null;
  const inlineSubmitPanel =
    contentTab === 'problem' &&
    (submitMessage != null || submitProgressSteps.length > 0) &&
    panelVisibility.submit &&
    !detachedPanels.submit &&
    !externalWindowPanels.submit
      ? renderSubmitPanel(false)
      : null;
  const inlineDetailPanels = [inlineEditorPanel, inlineSubmitPanel].filter((panel): panel is ReactNode => panel != null);

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
    <div className="page-stack solve-page">
      <div className="solve-page-topbar solve-page-topbar-content-tabs">
        <div className="solve-dbms-tab-row solve-content-tab-row" role="tablist" aria-label="문제 상세 화면 탭 선택">
          <button
            type="button"
            role="tab"
            aria-selected={contentTab === 'problem'}
            className={`solve-dbms-tab ${contentTab === 'problem' ? 'is-selected' : ''}`}
            onClick={() => setContentTab('problem')}
          >
            제출
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={contentTab === 'submissions'}
            className={`solve-dbms-tab ${contentTab === 'submissions' ? 'is-selected' : ''}`}
            onClick={() => setContentTab('submissions')}
          >
            내 제출 목록
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={contentTab === 'community'}
            className={`solve-dbms-tab ${contentTab === 'community' ? 'is-selected' : ''}`}
            onClick={() => setContentTab('community')}
          >
            태그된 게시글
          </button>
          <FavoriteTabButton
            className="favorite-tab-toggle-end"
            label={`${displayProblemNumber} / ${getSolveContentTabLabel(contentTab)}`}
            path={buildSolveContentTabPath(problemId, contentTab)}
            getSnapshot={getFavoriteSnapshot}
          />
        </div>
      </div>

      <section className="solve-page-hero solve-surface-section">
        <div className="solve-page-hero-copy solve-page-hero-copy-wide">
          <div className="solve-title-row">
            <span className="solve-problem-number">{`문제 ${displayProblemNumber}`}</span>
            <h1 className="solve-problem-title">{displayProblemTitle}</h1>
          </div>

        </div>
        {renderActiveContentTab()}
      </section>

      {contentTab === 'problem' && !problemDetail ? inlineDetailPanels : null}

      {visibleFloatingPanels.map((panelKey) => (
        <div
          key={panelKey}
          className={`solve-floating-pane-shell is-${panelKey}`}
          onWheelCapture={handleFloatingPaneWheel}
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
          {(['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw'] as const).map((direction) => (
            <button
              key={`${panelKey}-${direction}`}
              type="button"
              className={`solve-floating-pane-resize is-${direction}`}
              aria-label={`${getPanelTitle(panelKey)} 크기 조절`}
              onMouseDown={(event) => startFloatingResize(panelKey, direction, event)}
            >
              <span aria-hidden="true" />
            </button>
          ))}
        </div>
      ))}

      {visibleExternalWindows.map((panelKey) => (
        <PanelExternalWindow
          key={`external-${panelKey}`}
          panelKey={panelKey}
          title={`Quertimizer - ${getPanelTitle(panelKey)}`}
          layout={floatingLayouts[panelKey]}
          onClose={() =>
            setExternalWindowPanels((current) => ({
              ...current,
              [panelKey]: false,
            }))
          }
        >
          <div className={`solve-external-window-root-inner is-${panelKey}-panel`}>{renderPanel(panelKey, false)}</div>
        </PanelExternalWindow>
      ))}

      {relatedModalContent}

      <HandleSetupGate />

      {authOverlayMode ? (
        <SolvePageAuthOverlay
          mode={authOverlayMode}
          onClose={closeAuthOverlay}
          onOpenSignup={() => setAuthOverlayMode('signup')}
          onOpenResetPassword={() => setAuthOverlayMode('reset-password')}
          onReturnToLogin={() => setAuthOverlayMode('login')}
          onAuthenticated={handleAuthenticatedInOverlay}
          problemId={problemId}
          sql={sql}
          selectedDbms={selectedDbms}
        />
      ) : null}
    </div>
  );
}
