import { useCallback, useDeferredValue, useEffect, useMemo, useRef, useState, useSyncExternalStore, type KeyboardEvent as ReactKeyboardEvent, type MouseEvent as ReactMouseEvent, type PointerEvent as ReactPointerEvent, type ReactNode, type RefObject, type WheelEvent as ReactWheelEvent } from 'react';
import { createPortal } from 'react-dom';
import type { FormEvent } from 'react';
import './ProblemSolvePage.css';
import usePageJump from '@/shared/lib/hooks/use-page-jump';
import { FavoriteTabButton } from '@/features/favorite-tab';
import { HttpErrorState } from '@/shared/ui';
import { LoadingOverlay } from '@/shared/ui';
import { Pagination } from '@/shared/ui';
import { PageLoadFailureState } from '@/shared/ui';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '@/shared/api/api-error';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '@/features/favorite-tab';
import { HandleSetupGate } from '@/features/handle-setup';
import ProblemDetailContent from './ProblemDetailContent';
import { fetchProblemDetail, type ProblemDetailData } from '@/shared/api/problem-api';
import { fetchSubmitHistories } from '@/shared/api/submit-history-api';
import { fetchCommunityPosts, type CommunityPostPage } from '@/shared/api/community-api';
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
  sendSignupVerificationCode,
  signup,
  verifyPasswordResetCode,
  verifySignupVerificationCode,
} from '@/shared/api/auth-api';
import { completeAuthentication } from '@/shared/auth/auth-session';
import {
  SessionSocketError,
  SESSION_SOCKET_DESTINATION,
  sendSessionSocketMessage,
  sendSessionSocketMessageIfOpen,
  subscribeSessionSocketMessages,
  type SessionSocketMessage,
} from '@/shared/auth/session-socket';
import { syncSession, useSession } from '@/shared/auth/session';
import { getCommunityPostPath, getLocationSearchSnapshot, getProfilePath, navigate, subscribeLocation } from '@/shared/config/navigation';
import { ExecutionPlanDetailBoard, buildAvailableBucketFilters, getExecutionPlanDetailGroups, getPlanElementButtonLabel } from '@/entities/execution-plan';
import {
  EMAIL_PATTERN,
  PASSWORD_RESET_CODE_PATTERN,
  getAuthSocialLoginErrorMessage,
  hasRequiredPasswordFormat,
  sanitizeVerificationCode,
  type AuthSocialProvider,
} from '@/shared/auth/auth-ui';
import {
  SOCIAL_LOGIN_ERROR_MESSAGE,
  SOCIAL_LOGIN_SUCCESS_MESSAGE,
  isTrustedSocialLoginCallbackOrigin,
} from '@/shared/auth/social-login-callback';
import { formatBoardDate, formatCost, formatInteger, formatSubmittedAt } from '@/shared/lib/formatters';
import { renderHighlightedSql, renderStaticHighlightedSql, type SqlHighlightRange } from '@/shared/lib/sql-highlighter';
import { getUiText, getUiTextValue, useUiText } from '@/shared/config/ui-text';
import type { CommunityPostSummary, DbmsType, ProblemDetail, SubmitHistoryEntry, SubmitHistoryPageData, SubmitHistoryPlanFilters } from '@/shared/api/domain';
import logoImage from '@/shared/assets/logo.png';

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
    return getUiTextValue('PROBLEM_SOLVE_TAB_MY_SUBMISSIONS_LABEL', '내 제출 목록');
  }

  if (tab === 'community') {
    return getUiTextValue('PROBLEM_SOLVE_TAB_TAGGED_POSTS_LABEL', '태그된 게시글');
  }

  return getUiTextValue('PROBLEM_SOLVE_TAB_SUBMIT_LABEL', '제출');
}

type SolveRelatedModalState =
  | { type: 'sql'; history: SubmitHistoryEntry }
  | { type: 'plan'; history: SubmitHistoryEntry }
  | null;

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

const solveRelatedLoadingRows = Array.from({ length: 6 }, (_, index) => index);

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
    mysql: createEmptySolvePlanFilters(),
  };
}

function getSolveRelatedCommunityCategoryLabel(value: CommunityPostSummary['category']) {
  if (value === 'question') {
    return getUiTextValue('COMMUNITY_CATEGORY_QUESTION_LABEL', '질문');
  }

  if (value === 'notice') {
    return getUiTextValue('COMMUNITY_CATEGORY_NOTICE_LABEL', '공지');
  }

  return getUiTextValue('COMMUNITY_CATEGORY_FREE_LABEL', '자유');
}

function buildSolveRelatedPlanSections(history: SubmitHistoryEntry) {
  const labelGroupsBySection = new Map(
    getExecutionPlanDetailGroups(history.dbms, history.executionPlanElement)
      .map((group) => [group.sectionKey, group.labels] as const),
  );

  return [
    ...buildAvailableBucketFilters(history.dbms).map((filter) => ({
      sectionKey: filter.key,
      sectionLabel: filter.label,
      labels: labelGroupsBySection.get(filter.key) ?? [],
    })),
    {
      sectionKey: 'hint' as const,
      sectionLabel: getUiTextValue('COMMON_HINT_LABEL', 'Hint'),
      labels: labelGroupsBySection.get('hint') ?? [],
    },
  ];
}

function hasSolveRelatedExecutionPlanDetails(history: SubmitHistoryEntry) {
  return buildSolveRelatedPlanSections(history).some((section) => section.labels.length > 0);
}

type SolveAuthOverlayMode = 'login' | 'signup' | 'reset-password';
type SolveAuthSocialProvider = AuthSocialProvider;

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
const EXECUTION_RESULT_FETCH_PAGE_SIZE = 500;
const EXECUTION_RESULT_FETCH_PAGE_COUNT = Math.floor(EXECUTION_RESULT_FETCH_PAGE_SIZE / EXECUTION_RESULT_PAGE_SIZE);
const SUBMIT_REFERENCE_WRITE_CTE_PATTERN = /\bWITH\b[\s\S]*\b(INSERT|UPDATE|DELETE|MERGE)\b/i;

interface ProblemExecutionResult {
  success: boolean;
  mode: ProblemExecutionMode;
  message: string;
  reasons?: string[];
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
  reasons?: string[] | null;
  columns?: string[];
  rows?: string[][];
  planLines?: string[];
  rowCount?: number;
  currentPage?: number | null;
  pageSize?: number | null;
  executionTimeMs?: number | null;
}

type ProblemSubmitStepStatus = 'running' | 'success' | 'incorrect' | 'error' | 'skipped';
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

interface ProblemExecutionProgressMessage extends SessionSocketMessage {
  problemId?: string | null;
  message?: string | null;
}

interface ProblemSubmitProgressStep {
  stepKey: string;
  status: ProblemSubmitStepStatus;
  message: string;
  detailLines: string[];
}

type SqlAutocompleteKind = 'keyword' | 'table' | 'column';
type SqlAutocompleteKindPriority = Record<SqlAutocompleteKind, number>;

interface SqlAutocompleteItem {
  value: string;
  kind: SqlAutocompleteKind;
  detail?: string;
}

interface SqlAutocompleteTokenRange {
  tokenStart: number;
  tokenEnd: number;
  currentToken: string;
  typedToken: string;
  qualifier: string | null;
}

type SolveIndexMethod = 'BTREE' | 'HASH' | 'GIN' | 'GIST' | 'BRIN' | 'FULLTEXT' | 'SPATIAL';
type SolveIndexSortDirection = 'ASC' | 'DESC';
type SolveIndexNullsPosition = 'DEFAULT' | 'FIRST' | 'LAST';
type SolveIndexSource = 'builder' | 'editor';
type SolveIndexOperation = 'CREATE' | 'ALTER' | 'DROP';

interface SolveIndexTableOption {
  name: string;
  columns: string[];
}

interface SolveIndexColumn {
  name: string;
  direction: SolveIndexSortDirection;
  nulls: SolveIndexNullsPosition;
}

interface SolveIndexDefinition {
  id: string;
  name: string;
  tableName: string;
  method: SolveIndexMethod;
  unique: boolean;
  columns: SolveIndexColumn[];
  includeColumns: string[];
  whereClause: string;
  source: SolveIndexSource;
  operation: SolveIndexOperation;
  rawSql?: string;
  start?: number;
  end?: number;
}

interface SolveIndexDraft {
  name: string;
  tableName: string;
  method: SolveIndexMethod;
  unique: boolean;
  columns: SolveIndexColumn[];
  includeColumns: string[];
  whereClause: string;
}

interface EditorIndexParseResult {
  definitions: SolveIndexDefinition[];
  errorRanges: SqlHighlightRange[];
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

type ExecutionStatementStatus = 'idle' | 'running' | 'success' | 'error';

interface ExecutionStatementRun {
  key: string;
  sql: string;
  preview: string;
  start: number;
  end: number;
  status: ExecutionStatementStatus;
  result: ProblemExecutionResult | null;
  progressMessage?: string | null;
}

const panelOrder: PanelKey[] = ['editor', 'submit'];

const panelMinWidths: Record<PanelKey, number> = {
  editor: 420,
  submit: 340,
};

const panelMinHeights: Record<PanelKey, number> = {
  editor: 320,
  submit: 260,
};

const SOLVE_PAGE_AUTH_RETURN_STORAGE_KEY = 'quertimizer.solve-auth-return';

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
  'CREATE FULLTEXT INDEX',
  'CREATE SPATIAL INDEX',
  'ALTER INDEX',
  'DROP INDEX',
];
const DEFAULT_AUTOCOMPLETE_KIND_PRIORITY: SqlAutocompleteKindPriority = { keyword: 0, table: 1, column: 2 };
const TABLE_FIRST_AUTOCOMPLETE_KIND_PRIORITY: SqlAutocompleteKindPriority = { table: 0, column: 1, keyword: 2 };
const COLUMN_FIRST_AUTOCOMPLETE_KIND_PRIORITY: SqlAutocompleteKindPriority = { column: 0, table: 1, keyword: 2 };
const SQL_EDITOR_INDENT = '    ';
const SQL_EDITOR_MIN_HEIGHT = 256;
const SQL_EDITOR_DEFAULT_FONT_SIZE = 13.5;
const SQL_EDITOR_MIN_FONT_SIZE = 11;
const SQL_EDITOR_MAX_FONT_SIZE = 24;
const SQL_EDITOR_AUTOCOMPLETE_OVERFLOW_ITEM_COUNT = 4;
const POSTGRESQL_INDEX_METHODS: SolveIndexMethod[] = ['BTREE', 'HASH', 'GIN', 'GIST', 'BRIN'];
const MYSQL_INDEX_METHODS: SolveIndexMethod[] = ['BTREE', 'HASH', 'FULLTEXT', 'SPATIAL'];
const FLOATING_EDITOR_BACKGROUND_MIN_ALPHA = 0.12;
const FLOATING_EDITOR_BACKGROUND_MAX_ALPHA = 1;
const SQL_EDITOR_CONTENT_LINE_HEIGHT_RATIO = 1.7;
const SUBMIT_HIDDEN_CASE_STEP_PATTERN = /^answer-hidden-(\d+)$/;
const SUBMIT_INDEX_DDL_PATTERN = /^(CREATE\s+(?:UNIQUE\s+)?(?:(?:FULLTEXT|SPATIAL)\s+)?INDEX|DROP\s+INDEX|ALTER\s+INDEX)\b/i;
const SQL_IDENTIFIER_PATTERN = String.raw`(?:"[^"]+"|` + '`[^`]+`' + String.raw`|[A-Za-z_][A-Za-z0-9_$]*)(?:\.(?:"[^"]+"|` + '`[^`]+`' + String.raw`|[A-Za-z_][A-Za-z0-9_$]*))?`;
const SQL_CREATE_TABLE_OPTION_PATTERN =
  String.raw`(?:(?:ENGINE|DEFAULT|CHARSET|COLLATE|COMMENT|ROW_FORMAT|AUTO_INCREMENT|TABLESPACE|PARTITION)\b[^;]*)?`;
const PLAN_SELECTED_ATTEMPT_PREFIX = '__selected_plan_attempt__:';
const PLAN_MEASUREMENT_PATTERN = /^실행계획 분석\s+(\d+)회\s*:\s*Cost\s+(.+)$/;
const PLAN_DETAIL_SECTIONS = [
  { sectionKey: 'scan', sectionLabel: 'Scan' },
  { sectionKey: 'join', sectionLabel: 'Join' },
  { sectionKey: 'filter', sectionLabel: 'Filter' },
  { sectionKey: 'sort', sectionLabel: 'Sort' },
  { sectionKey: 'aggregate', sectionLabel: 'Aggregate' },
  { sectionKey: 'hint', sectionLabel: 'Hint' },
];

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function getDbmsLabel(dbms: DbmsType) {
  return dbms === 'postgresql'
    ? getUiTextValue('COMMON_POSTGRESQL_LABEL', 'PostgreSQL')
    : getUiTextValue('COMMON_MYSQL_LABEL', 'MySQL');
}

function getAvailableDbms(problem: ProblemDetail) {
  return problem.dbmsOptions;
}

function resolvePreferredDbms(availableDbms: DbmsType[], fallbackDbms: DbmsType[], defaultDbms: DbmsType | null) {
  if (defaultDbms && availableDbms.includes(defaultDbms)) {
    return defaultDbms;
  }

  return availableDbms[0] ?? fallbackDbms[0] ?? 'postgresql';
}

function formatGroupedNumber(value?: number) {
  return formatInteger(value, 'en-US');
}

function RelatedViewIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M2.4 8s1.9-3.7 5.6-3.7S13.6 8 13.6 8 11.7 11.7 8 11.7 2.4 8 2.4 8Z" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M8 9.75A1.75 1.75 0 1 0 8 6.25a1.75 1.75 0 0 0 0 3.5Z" stroke="currentColor" strokeWidth="1.35" />
    </svg>
  );
}

function RelatedLikeIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 13.3 3.5 9.1a2.8 2.8 0 0 1 4-4L8 5.6l.5-.5a2.8 2.8 0 0 1 4 4L8 13.3Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function RelatedCommentIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.5 3.9h9v6.1H7.4l-3.1 2.35V10h-.8V3.9Z" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function getExecutionResultPageCount(rowCount: number) {
  return Math.max(1, Math.ceil(rowCount / EXECUTION_RESULT_PAGE_SIZE));
}

function getExecutionResultFetchPage(page: number) {
  return Math.max(1, Math.floor((page - 1) / EXECUTION_RESULT_FETCH_PAGE_COUNT) + 1);
}

function isExecutionResultPageCached(executionResult: ProblemExecutionResult, page: number) {
  const fetchPage = executionResult.currentPage ?? 1;
  const fetchPageSize = executionResult.pageSize ?? EXECUTION_RESULT_FETCH_PAGE_SIZE;
  const cachedStartIndex = (fetchPage - 1) * fetchPageSize;
  const pageStartIndex = (page - 1) * EXECUTION_RESULT_PAGE_SIZE;

  return pageStartIndex >= cachedStartIndex
    && pageStartIndex + EXECUTION_RESULT_PAGE_SIZE <= cachedStartIndex + fetchPageSize;
}

function createExecutionResultPageRows(executionResult: ProblemExecutionResult, page: number) {
  const fetchPage = executionResult.currentPage ?? 1;
  const fetchPageSize = executionResult.pageSize ?? EXECUTION_RESULT_FETCH_PAGE_SIZE;
  const cachedStartIndex = (fetchPage - 1) * fetchPageSize;
  const pageStartIndex = (page - 1) * EXECUTION_RESULT_PAGE_SIZE;
  const startIndex = Math.max(0, pageStartIndex - cachedStartIndex);

  return executionResult.rows.slice(startIndex, startIndex + EXECUTION_RESULT_PAGE_SIZE);
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
  const scopedDbms = problemId.startsWith('M') ? 'mysql' : problemId.startsWith('P') ? 'postgresql' : null;
  const dbmsOptions: DbmsType[] = scopedDbms ? [scopedDbms] : ['postgresql', 'mysql'];

  return {
    id: problemId,
    domain: 'rdbms',
    number: toProblemSequence(problemId),
    problemNumber: problemId,
    title: '',
    preview: '',
    tags: [],
    difficulty: '중급',
    solvedCount: 0,
    totalSubmitCount: 0,
    successSubmitCount: 0,
    submittedHistories: [],
    description: '',
    schemaInfo: '',
    inputExample: '',
    outputExample: '',
    starterSql: '',
    dbmsOptions,
    disabledDbms: [],
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

function createProblemExecutionError(message: string, reasons: string[] = []): ProblemExecutionResult {
  return {
    success: false,
    mode: 'command',
    message,
    reasons,
    columns: [],
    rows: [],
    planLines: [],
    rowCount: 0,
  };
}

function isAuthenticationRequiredMessage(message: string | null | undefined) {
  if (!message) {
    return false;
  }

  return message.includes('로그인 후') || message.includes('인증') || message.includes('세션');
}

function resolveSocketFailureMessage(message: ProblemSocketMessage, fallbackMessage: string) {
  const reasons = Array.isArray(message.reasons)
    ? message.reasons.filter((reason): reason is string => typeof reason === 'string' && reason.trim() !== '')
    : [];

  return reasons[0] ?? message.message ?? fallbackMessage;
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
        parsedValue.selectedDbms === 'mysql' || parsedValue.selectedDbms === 'postgresql'
          ? parsedValue.selectedDbms
          : null,
    };
  } catch {
    return null;
  }
}

function resolveProblemDdl(detail: ProblemDetailData | null, dbms: DbmsType) {
  const preferredDdl = detail?.dbms === dbms ? detail.ddl : '';

  return preferredDdl;
}

function extractAutocompleteItemsFromDdl(ddl: string): SqlAutocompleteItem[] {
  const createTablePattern = new RegExp(
    String.raw`CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(${SQL_IDENTIFIER_PATTERN})\s*\(([\s\S]*?)\)\s*${SQL_CREATE_TABLE_OPTION_PATTERN};`,
    'gi',
  );
  const items: SqlAutocompleteItem[] = [];
  let match: RegExpExecArray | null;

  while ((match = createTablePattern.exec(ddl)) != null) {
    const tableName = normalizeSqlIdentifier(match[1]);
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
        if (/^(CONSTRAINT|PRIMARY KEY|FOREIGN KEY|UNIQUE|CHECK|KEY|INDEX|FULLTEXT|SPATIAL)\b/i.test(line)) {
          return;
        }

        const columnMatch = line.match(new RegExp(String.raw`^(${SQL_IDENTIFIER_PATTERN})\s+`, 'i'));
        if (!columnMatch) {
          return;
        }

        items.push({
          value: normalizeSqlIdentifier(columnMatch[1]),
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

function extractIndexTableOptionsFromDdl(ddl: string): SolveIndexTableOption[] {
  const createTablePattern = new RegExp(
    String.raw`CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(${SQL_IDENTIFIER_PATTERN})\s*\(([\s\S]*?)\)\s*${SQL_CREATE_TABLE_OPTION_PATTERN};`,
    'gi',
  );
  const tableOptions: SolveIndexTableOption[] = [];
  let match: RegExpExecArray | null;

  while ((match = createTablePattern.exec(ddl)) != null) {
    const tableName = normalizeSqlIdentifier(match[1]);
    const columns = match[2]
      .split('\n')
      .map((line) => line.trim().replace(/,$/, ''))
      .filter(Boolean)
      .filter((line) => !/^(CONSTRAINT|PRIMARY KEY|FOREIGN KEY|UNIQUE|CHECK|KEY|INDEX|FULLTEXT|SPATIAL|COMMENT)\b/i.test(line))
      .map((line) => line.match(new RegExp(String.raw`^(${SQL_IDENTIFIER_PATTERN})\s+`, 'i'))?.[1] ?? '')
      .map(normalizeSqlIdentifier)
      .filter(Boolean);

    tableOptions.push({
      name: tableName,
      columns: columns.filter((column, index, source) => source.indexOf(column) === index),
    });
  }

  return tableOptions;
}

function createTableAutocompleteItems(tableOptions: SolveIndexTableOption[]): SqlAutocompleteItem[] {
  return tableOptions.map((tableOption) => ({
    value: tableOption.name,
    kind: 'table',
  }));
}

function createColumnAutocompleteItems(tableName: string, columns: string[], detailPrefix?: string): SqlAutocompleteItem[] {
  return columns.map((columnName) => ({
    value: columnName,
    kind: 'column',
    detail: detailPrefix ? `${detailPrefix} · ${tableName}` : tableName,
  }));
}

function createAllColumnAutocompleteItems(tableOptions: SolveIndexTableOption[]): SqlAutocompleteItem[] {
  return tableOptions.flatMap((tableOption) => createColumnAutocompleteItems(tableOption.name, tableOption.columns));
}

function normalizeSqlName(name: string) {
  return normalizeSqlIdentifier(name).toLowerCase();
}

function getIndexMethodOptions(dbms: DbmsType) {
  return dbms === 'postgresql' ? POSTGRESQL_INDEX_METHODS : MYSQL_INDEX_METHODS;
}

function supportsIndexColumnOrdering(method: SolveIndexMethod) {
  return method === 'BTREE';
}

function supportsUniqueIndex(dbms: DbmsType, method: SolveIndexMethod) {
  return !(dbms === 'mysql' && (method === 'FULLTEXT' || method === 'SPATIAL'));
}

function createIndexDefaultName(indexCount: number) {
  return `index_${String(indexCount + 1).padStart(2, '0')}`;
}

function createDefaultIndexDraft(tableOptions: SolveIndexTableOption[], dbms: DbmsType, indexCount = 0): SolveIndexDraft {
  const tableName = tableOptions[0]?.name ?? '';
  const firstColumn = tableOptions[0]?.columns[0] ?? '';
  const method = getIndexMethodOptions(dbms)[0] ?? 'BTREE';
  const columns = firstColumn ? [createIndexColumn(firstColumn)] : [];

  return {
    name: createIndexDefaultName(indexCount),
    tableName,
    method,
    unique: false,
    columns,
    includeColumns: [],
    whereClause: '',
  };
}

function createIndexColumn(name: string): SolveIndexColumn {
  return {
    name,
    direction: 'ASC',
    nulls: 'DEFAULT',
  };
}

function toIdentifierSql(identifier: string, dbms: DbmsType) {
  const quote = dbms === 'mysql' ? '`' : '"';
  const quotePattern = dbms === 'mysql' ? /`/g : /"/g;

  return identifier
    .split('.')
    .map((part) => (/^[A-Za-z_][A-Za-z0-9_$]*$/.test(part) ? part : `${quote}${part.replace(quotePattern, quote + quote)}${quote}`))
    .join('.');
}

function normalizeSqlIdentifier(identifier: string) {
  const lastToken = identifier.trim().split('.').pop() ?? identifier.trim();
  return lastToken.replace(/^["`]|["`]$/g, '');
}

function normalizeComparableSql(sql: string) {
  return sql.trim().replace(/;+\s*$/, '').replace(/\s+/g, ' ').toLowerCase();
}

function splitTopLevelComma(value: string) {
  const parts: string[] = [];
  let partStart = 0;
  let depth = 0;
  let inSingleQuote = false;
  let inDoubleQuote = false;

  for (let index = 0; index < value.length; index += 1) {
    const currentChar = value[index];
    const nextChar = value[index + 1];

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

    if (currentChar === "'") {
      inSingleQuote = true;
      continue;
    }

    if (currentChar === '"') {
      inDoubleQuote = true;
      continue;
    }

    if (currentChar === '(') {
      depth += 1;
      continue;
    }

    if (currentChar === ')') {
      depth = Math.max(0, depth - 1);
      continue;
    }

    if (currentChar === ',' && depth === 0) {
      parts.push(value.slice(partStart, index).trim());
      partStart = index + 1;
    }
  }

  parts.push(value.slice(partStart).trim());
  return parts.filter(Boolean);
}

function buildCreateIndexSql(indexDefinition: SolveIndexDraft | SolveIndexDefinition, dbms: DbmsType) {
  const indexName = indexDefinition.name.trim() || createIndexDefaultName(0);
  const tableName = indexDefinition.tableName.trim();
  const columnOrderingEnabled = supportsIndexColumnOrdering(indexDefinition.method);
  const columnSql = indexDefinition.columns
    .map((column) => {
      const directionSql = columnOrderingEnabled ? ` ${column.direction}` : '';
      const nullsSql = columnOrderingEnabled && dbms === 'postgresql' && column.nulls !== 'DEFAULT' ? ` NULLS ${column.nulls}` : '';

      return `${toIdentifierSql(column.name, dbms)}${directionSql}${nullsSql}`;
    })
    .join(', ');
  const uniqueSql = indexDefinition.unique && supportsUniqueIndex(dbms, indexDefinition.method) ? 'UNIQUE ' : '';
  const usingSql = indexDefinition.method ? ` USING ${indexDefinition.method}` : '';
  const includeSql =
    dbms === 'postgresql' && indexDefinition.includeColumns.length > 0
      ? ` INCLUDE (${indexDefinition.includeColumns.map((column) => toIdentifierSql(column, dbms)).join(', ')})`
      : '';
  const whereSql = dbms === 'postgresql' && indexDefinition.whereClause.trim() ? ` WHERE ${indexDefinition.whereClause.trim()}` : '';

  if (!tableName || !columnSql) {
    return '';
  }

  if (dbms === 'mysql') {
    if (indexDefinition.method === 'FULLTEXT' || indexDefinition.method === 'SPATIAL') {
      return `CREATE ${indexDefinition.method} INDEX ${toIdentifierSql(indexName, dbms)} ON ${toIdentifierSql(tableName, dbms)} (${columnSql});`;
    }

    return `CREATE ${uniqueSql}INDEX ${toIdentifierSql(indexName, dbms)}${usingSql} ON ${toIdentifierSql(tableName, dbms)} (${columnSql});`;
  }

  return `CREATE ${uniqueSql}INDEX ${toIdentifierSql(indexName, dbms)} ON ${toIdentifierSql(tableName, dbms)}${usingSql} (${columnSql})${includeSql}${whereSql};`;
}

function resolveIndexDefinitionSql(indexDefinition: SolveIndexDefinition, dbms: DbmsType) {
  return indexDefinition.rawSql ?? buildCreateIndexSql(indexDefinition, dbms);
}

function isKnownIndexColumn(tableOptions: SolveIndexTableOption[], tableName: string, columnName: string) {
  const tableOption = tableOptions.find((option) => option.name.toLowerCase() === tableName.toLowerCase());
  return tableOption?.columns.some((column) => column.toLowerCase() === columnName.toLowerCase()) ?? false;
}

function parseIndexColumnDefinition(rawColumn: string): SolveIndexColumn | null {
  const columnMatch = rawColumn.trim().match(/^("[^"]+"|`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*)(?:\s+(ASC|DESC))?(?:\s+NULLS\s+(FIRST|LAST))?$/i);
  if (columnMatch == null) {
    return null;
  }

  return {
    name: normalizeSqlIdentifier(columnMatch[1]),
    direction: (columnMatch[2]?.toUpperCase() as SolveIndexSortDirection | undefined) ?? 'ASC',
    nulls: (columnMatch[3]?.toUpperCase() as SolveIndexNullsPosition | undefined) ?? 'DEFAULT',
  };
}

function parseCreateIndexStatement(statement: SqlStatementSegment, tableOptions: SolveIndexTableOption[], dbms: DbmsType): SolveIndexDefinition | null {
  const createIndexPattern = new RegExp(
    String.raw`^CREATE\s+(UNIQUE\s+)?(?:(FULLTEXT|SPATIAL)\s+)?INDEX\s+(?:CONCURRENTLY\s+)?(?:IF\s+NOT\s+EXISTS\s+)?(${SQL_IDENTIFIER_PATTERN})(?:\s+USING\s+(\w+))?\s+ON\s+(${SQL_IDENTIFIER_PATTERN})(?:\s+USING\s+(\w+))?\s*\(([\s\S]*?)\)\s*([\s\S]*)$`,
    'i',
  );
  const createIndexMatch = statement.sql.match(createIndexPattern);
  if (createIndexMatch == null) {
    return null;
  }

  const tableName = normalizeSqlIdentifier(createIndexMatch[5]);
  const method = ((createIndexMatch[2] ?? createIndexMatch[4] ?? createIndexMatch[6] ?? 'BTREE').toUpperCase() as SolveIndexMethod);
  const parsedColumns = splitTopLevelComma(createIndexMatch[7])
    .map(parseIndexColumnDefinition);
  const tail = createIndexMatch[8].trim();
  const includeMatch = tail.match(/^INCLUDE\s*\(([\s\S]*?)\)\s*(.*)$/i);
  const whereSource = includeMatch?.[2]?.trim() ?? tail;
  const whereMatch = whereSource.match(/^WHERE\s+([\s\S]+)$/i);
  const unsupportedMysqlTail = dbms === 'mysql' && tail.length > 0;
  const unknownTail = whereSource.length > 0 && whereMatch == null;

  if (
    !getIndexMethodOptions(dbms).includes(method) ||
    parsedColumns.length === 0 ||
    parsedColumns.some((column) => column == null) ||
    unsupportedMysqlTail ||
    unknownTail
  ) {
    return null;
  }
  const columns = parsedColumns.filter((column): column is SolveIndexColumn => column != null);

  if (columns.some((column) => !isKnownIndexColumn(tableOptions, tableName, column.name))) {
    return null;
  }

  return {
    id: `editor-index-${statement.start}-${statement.end}`,
    name: normalizeSqlIdentifier(createIndexMatch[3]),
    tableName,
    method,
    unique: createIndexMatch[1] != null && supportsUniqueIndex(dbms, method),
    columns,
    includeColumns: includeMatch?.[1] ? splitTopLevelComma(includeMatch[1]).map(normalizeSqlIdentifier) : [],
    whereClause: whereMatch?.[1]?.trim() ?? '',
    source: 'editor',
    operation: 'CREATE',
    rawSql: `${statement.sql};`,
    start: statement.start,
    end: statement.end,
  };
}

function parseEditorIndexStatement(statement: SqlStatementSegment, tableOptions: SolveIndexTableOption[], dbms: DbmsType): SolveIndexDefinition | null {
  if (/^CREATE\s+(?:UNIQUE\s+)?(?:(?:FULLTEXT|SPATIAL)\s+)?INDEX\b/i.test(statement.sql)) {
    return parseCreateIndexStatement(statement, tableOptions, dbms);
  }

  const alterIndexPattern = new RegExp(String.raw`^ALTER\s+INDEX\s+(?:IF\s+EXISTS\s+)?(${SQL_IDENTIFIER_PATTERN})\b[\s\S]+$`, 'i');
  const alterIndexMatch = statement.sql.match(alterIndexPattern);
  if (alterIndexMatch != null) {
    return {
      id: `editor-index-${statement.start}-${statement.end}`,
      name: normalizeSqlIdentifier(alterIndexMatch[1]),
      tableName: '',
      method: 'BTREE',
      unique: false,
      columns: [],
      includeColumns: [],
      whereClause: '',
      source: 'editor',
      operation: 'ALTER',
      rawSql: `${statement.sql};`,
      start: statement.start,
      end: statement.end,
    };
  }

  const dropIndexPattern = new RegExp(String.raw`^DROP\s+INDEX\s+(?:IF\s+EXISTS\s+)?(${SQL_IDENTIFIER_PATTERN})(?:\s+ON\s+(${SQL_IDENTIFIER_PATTERN}))?\s*$`, 'i');
  const dropIndexMatch = statement.sql.match(dropIndexPattern);
  if (dropIndexMatch == null || (dbms === 'mysql' && dropIndexMatch[2] == null)) {
    return null;
  }

  return {
    id: `editor-index-${statement.start}-${statement.end}`,
    name: normalizeSqlIdentifier(dropIndexMatch[1]),
    tableName: dropIndexMatch[2] ? normalizeSqlIdentifier(dropIndexMatch[2]) : '',
    method: 'BTREE',
    unique: false,
    columns: [],
    includeColumns: [],
    whereClause: '',
    source: 'editor',
    operation: 'DROP',
    rawSql: `${statement.sql};`,
    start: statement.start,
    end: statement.end,
  };
}

function parseEditorIndexStatements(sql: string, tableOptions: SolveIndexTableOption[], dbms: DbmsType): EditorIndexParseResult {
  return parseSqlStatements(sql).reduce<EditorIndexParseResult>(
    (result, statement) => {
      if (!SUBMIT_INDEX_DDL_PATTERN.test(statement.sql.trim())) {
        return result;
      }

      const indexDefinition = parseEditorIndexStatement(statement, tableOptions, dbms);
      if (indexDefinition == null) {
        result.errorRanges.push({ start: statement.start, end: statement.end });
        return result;
      }

      result.definitions.push(indexDefinition);
      return result;
    },
    { definitions: [], errorRanges: [] },
  );
}

function uniqueSqls(sqls: string[]) {
  const seenSqls = new Set<string>();

  return sqls.filter((sql) => {
    const normalizedSql = normalizeComparableSql(sql);
    if (!normalizedSql || seenSqls.has(normalizedSql)) {
      return false;
    }

    seenSqls.add(normalizedSql);
    return true;
  });
}

function removeSqlRanges(value: string, ranges: SqlHighlightRange[]) {
  const nextValue = [...ranges]
    .sort((left, right) => right.start - left.start)
    .reduce((currentValue, range) => {
      const safeStart = Math.max(0, Math.min(range.start, currentValue.length));
      const safeEnd = Math.max(safeStart, Math.min(range.end, currentValue.length));
      const lineStart = currentValue.lastIndexOf('\n', safeStart - 1) + 1;
      const nextLineIndex = currentValue.indexOf('\n', safeEnd);
      const lineEnd = nextLineIndex === -1 ? currentValue.length : nextLineIndex + 1;
      const canRemoveFullLine =
        currentValue.slice(lineStart, safeStart).trim() === '' &&
        currentValue.slice(safeEnd, lineEnd).trim() === '';

      return canRemoveFullLine
        ? currentValue.slice(0, lineStart) + currentValue.slice(lineEnd)
        : currentValue.slice(0, safeStart) + currentValue.slice(safeEnd);
    }, value);

  return nextValue.replace(/[ \t]+\n/g, '\n').replace(/\n{3,}/g, '\n\n');
}

function hasAutocompleteTokenAfterCaret(value: string, caretIndex: number) {
  return caretIndex < value.length && /[A-Za-z0-9_]/.test(value[caretIndex]);
}

function isAutocompleteTablePosition(value: string, caretIndex: number) {
  const beforeCaret = value.slice(0, caretIndex).replace(/'[^']*'/g, "''").toUpperCase();
  return /\b(FROM|JOIN)\s+$/.test(beforeCaret);
}

function getAutocompleteTokenRange(value: string, caretIndex: number): SqlAutocompleteTokenRange | null {
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
  const qualifier = resolveAutocompleteQualifier(value, tokenStart);

  if (currentToken.length > 0 && !/^[A-Za-z_][A-Za-z0-9_]*$/.test(currentToken)) {
    return null;
  }

  if (qualifier == null && typedToken.length === 0 && !isAutocompleteTablePosition(value, caretIndex)) {
    return null;
  }

  return {
    tokenStart,
    tokenEnd,
    currentToken,
    typedToken,
    qualifier,
  };
}

function resolveAutocompleteQualifier(value: string, tokenStart: number) {
  if (tokenStart <= 1 || value[tokenStart - 1] !== '.') {
    return null;
  }

  let qualifierStart = tokenStart - 1;
  while (qualifierStart > 0 && /[A-Za-z0-9_]/.test(value[qualifierStart - 1])) {
    qualifierStart -= 1;
  }

  const qualifier = value.slice(qualifierStart, tokenStart - 1);
  return /^[A-Za-z_][A-Za-z0-9_]*$/.test(qualifier) ? qualifier : null;
}

function resolveCurrentSqlStatement(value: string, caretIndex: number) {
  const statements = parseSqlStatements(value);
  return statements.find((statement) => statement.start <= caretIndex && caretIndex <= statement.end)
    ?? statements.find((statement) => statement.start <= caretIndex)
    ?? null;
}

function resolveAutocompleteClause(statementSql: string, statementOffset: number) {
  const beforeCaret = statementSql.slice(0, statementOffset).replace(/'[^']*'/g, "''");
  const upperBeforeCaret = beforeCaret.toUpperCase();
  const fromJoinMatch = upperBeforeCaret.match(/\b(FROM|JOIN)\s+[A-Z0-9_"]*$/);
  if (fromJoinMatch != null) {
    return fromJoinMatch[1] === 'JOIN' ? 'join' : 'from';
  }

  const clauseMatches = [...upperBeforeCaret.matchAll(/\b(SELECT|FROM|JOIN|WHERE|ON|HAVING|GROUP\s+BY|ORDER\s+BY)\b/g)];
  const lastClauseMatch = clauseMatches[clauseMatches.length - 1];
  const lastClause = lastClauseMatch?.[1]?.replace(/\s+/g, ' ') ?? '';
  if (lastClause === 'FROM') {
    return 'from';
  }

  if (lastClause === 'JOIN') {
    return 'join';
  }

  if (lastClause === 'SELECT') {
    return 'select';
  }

  return 'expression';
}

function createAliasMap(statementSql: string, tableOptions: SolveIndexTableOption[]) {
  const tableByName = new Map(tableOptions.map((tableOption) => [normalizeSqlName(tableOption.name), tableOption]));
  const aliasMap = new Map<string, SolveIndexTableOption>();
  const reservedAliasPattern = 'ON|WHERE|JOIN|INNER|LEFT|RIGHT|FULL|CROSS|GROUP|ORDER|HAVING|LIMIT';
  const tableReferencePattern = new RegExp(
    String.raw`\b(?:FROM|JOIN)\s+(${SQL_IDENTIFIER_PATTERN})(?:\s+(?:AS\s+)?(?!${reservedAliasPattern}\b)([A-Za-z_][A-Za-z0-9_]*))?`,
    'gi',
  );
  let match: RegExpExecArray | null;

  while ((match = tableReferencePattern.exec(statementSql)) != null) {
    const tableOption = tableByName.get(normalizeSqlName(match[1]));
    const alias = match[2]?.toUpperCase();
    if (tableOption == null) {
      continue;
    }

    aliasMap.set(normalizeSqlName(tableOption.name), tableOption);
    if (alias != null) {
      aliasMap.set(normalizeSqlName(match[2]), tableOption);
    }
  }

  return aliasMap;
}

function createContextualAutocompleteContext(value: string, caretIndex: number, tokenRange: SqlAutocompleteTokenRange,
                                             tableOptions: SolveIndexTableOption[], defaultItems: SqlAutocompleteItem[]) {
  const currentStatement = resolveCurrentSqlStatement(value, caretIndex);
  const statementSql = currentStatement?.sql ?? value;
  const statementOffset = currentStatement == null ? caretIndex : Math.max(0, caretIndex - currentStatement.start);

  if (tokenRange.qualifier != null) {
    const aliasMap = createAliasMap(statementSql, tableOptions);
    const tableByName = new Map(tableOptions.map((tableOption) => [normalizeSqlName(tableOption.name), tableOption]));
    const tableOption = aliasMap.get(normalizeSqlName(tokenRange.qualifier)) ?? tableByName.get(normalizeSqlName(tokenRange.qualifier));
    return tableOption == null
      ? { items: [], kindPriority: COLUMN_FIRST_AUTOCOMPLETE_KIND_PRIORITY }
      : {
          items: createColumnAutocompleteItems(tableOption.name, tableOption.columns, tokenRange.qualifier),
          kindPriority: COLUMN_FIRST_AUTOCOMPLETE_KIND_PRIORITY,
        };
  }

  const clause = resolveAutocompleteClause(statementSql, statementOffset);
  if (clause === 'from' || clause === 'join') {
    return {
      items: [
        ...createTableAutocompleteItems(tableOptions),
        ...defaultItems.filter((item) => item.kind === 'keyword'),
      ],
      kindPriority: TABLE_FIRST_AUTOCOMPLETE_KIND_PRIORITY,
    };
  }

  if (clause === 'select') {
    return {
      items: [
        ...createAllColumnAutocompleteItems(tableOptions),
        ...defaultItems,
      ],
      kindPriority: COLUMN_FIRST_AUTOCOMPLETE_KIND_PRIORITY,
    };
  }

  return {
    items: defaultItems,
    kindPriority: DEFAULT_AUTOCOMPLETE_KIND_PRIORITY,
  };
}

function createAutocompleteSuggestions(items: SqlAutocompleteItem[], typedToken: string,
                                       kindPriority: SqlAutocompleteKindPriority) {
  const normalizedTypedToken = typedToken.toLowerCase();
  const seenItems = new Set<string>();

  return items
    .filter((item) => {
      const itemKey = `${item.kind}:${item.value.toLowerCase()}:${item.detail ?? ''}`;
      if (seenItems.has(itemKey)) {
        return false;
      }

      seenItems.add(itemKey);
      if (normalizedTypedToken !== '' && !item.value.toLowerCase().startsWith(normalizedTypedToken)) {
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

  return `${collapsedSql.slice(0, Math.max(0, maxLength - 1))}…`;
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
    progressMessage: null,
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
    progressMessage: null,
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
    label: getUiTextValue('PROBLEM_SOLVE_SELECT_ONLY_SUBMIT_LABEL', '기준 SELECT만 제출'),
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
      label: getUiTextValue('PROBLEM_SOLVE_WITH_DDL_SUBMIT_LABEL', '위 DDL 포함 제출'),
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
      label: getUiTextValue('PROBLEM_SOLVE_CURRENT_STATEMENT_RUN_LABEL', '현재 구문 실행'),
      preview: nearestStatement.preview,
      start: nearestStatement.start,
      end: nearestStatement.end,
      segments: [nearestStatement],
    },
    {
      key: 'loose-statement-group',
      kind: 'statement',
      label: getUiTextValue('PROBLEM_SOLVE_NEARBY_STATEMENT_RUN_LABEL', '인접 구문 실행'),
      preview: createSqlStatementPreview(looseStatementGroup.map((statement) => statement.sql).join(';\n')),
      start: looseStatementGroup[0].start,
      end: looseStatementGroup[looseStatementGroup.length - 1].end,
      segments: looseStatementGroup,
    },
    {
      key: 'all-statements',
      kind: 'all',
      label: getUiTextValue('PROBLEM_SOLVE_RUN_ALL_LABEL', '전체 실행'),
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
    message: resolveSocketFailureMessage(message, ''),
    reasons: Array.isArray(message.reasons) ? message.reasons : [],
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

function isTerminalSubmitProgressStatus(status: ProblemSubmitStepStatus) {
  return status === 'success' || status === 'incorrect' || status === 'error' || status === 'skipped';
}

function upsertSubmitProgressStep(
  currentSteps: ProblemSubmitProgressStep[],
  nextStep: ProblemSubmitProgressStep,
): ProblemSubmitProgressStep[] {
  const existingIndex = currentSteps.findIndex((step) => step.stepKey === nextStep.stepKey);
  if (existingIndex >= 0) {
    const existingStep = currentSteps[existingIndex];
    if (isTerminalSubmitProgressStatus(existingStep.status) && nextStep.status === 'running') {
      return currentSteps;
    }

    const nextSteps = [...currentSteps];
    nextSteps[existingIndex] = nextStep;
    return nextSteps;
  }

  const nextSteps = [...currentSteps, nextStep];

  return nextSteps.sort((left, right) => {
    const leftIndex = getSubmitProgressStepOrder(left.stepKey);
    const rightIndex = getSubmitProgressStepOrder(right.stepKey);
    return leftIndex - rightIndex;
  });
}

function getSubmitProgressStepOrder(stepKey: string) {
  if (stepKey === 'validate') return 0;
  const hiddenCaseMatch = stepKey.match(SUBMIT_HIDDEN_CASE_STEP_PATTERN);
  if (hiddenCaseMatch != null) return 10 + Number(hiddenCaseMatch[1]);
  if (stepKey === 'answer-open') return 100;
  if (stepKey === 'plan') return 200;
  return 300;
}

function isAnswerCaseProgressStep(stepKey: string) {
  return stepKey === 'answer-open' || SUBMIT_HIDDEN_CASE_STEP_PATTERN.test(stepKey);
}

function createSkippedAnswerCaseMessage(stepKey: string) {
  const hiddenCaseMatch = stepKey.match(SUBMIT_HIDDEN_CASE_STEP_PATTERN);
  if (hiddenCaseMatch != null) {
    return `Case Hidden ${hiddenCaseMatch[1]} 채점 생략`;
  }

  if (stepKey === 'answer-open') {
    return 'Case Open 채점 생략';
  }

  return getUiTextValue('PROBLEM_SOLVE_SUBMIT_PROGRESS_SKIPPED_MESSAGE', '채점 생략');
}

function skipRunningAnswerCaseProgressSteps(currentSteps: ProblemSubmitProgressStep[]) {
  return currentSteps.map((step) =>
    step.status === 'running' && isAnswerCaseProgressStep(step.stepKey)
      ? createSubmitProgressStep(step.stepKey, 'skipped', createSkippedAnswerCaseMessage(step.stepKey))
      : step,
  );
}

function createPlanProgressView(detailLines: string[]) {
  const selectedAttemptLine = detailLines.find((detailLine) => detailLine.startsWith(PLAN_SELECTED_ATTEMPT_PREFIX));
  const selectedAttempt = selectedAttemptLine == null
    ? null
    : Number(selectedAttemptLine.slice(PLAN_SELECTED_ATTEMPT_PREFIX.length));
  const measurements = detailLines
    .map((detailLine) => detailLine.match(PLAN_MEASUREMENT_PATTERN))
    .filter((match): match is RegExpMatchArray => match != null)
    .map((match) => ({
      attempt: Number(match[1]),
      cost: match[2],
      selected: selectedAttempt != null && Number(match[1]) === selectedAttempt,
    }));
  const selectedCost = measurements.find((measurement) => measurement.selected)?.cost ?? null;
  const detailGroupsBySection = new Map<string, string[]>();
  detailLines
    .filter((detailLine) => !detailLine.startsWith(PLAN_SELECTED_ATTEMPT_PREFIX))
    .filter((detailLine) => !PLAN_MEASUREMENT_PATTERN.test(detailLine))
    .filter((detailLine) => !/^Cost\s*·/i.test(detailLine.trim()))
    .map((detailLine) => detailLine.replace(/^✓\s*/, '').trim())
    .filter((detailLine) => detailLine !== '' && !/^-+$/.test(detailLine))
    .map((detailLine) => detailLine.split('·').map((token) => token.trim()).filter(Boolean))
    .filter(([sectionLabel]) => PLAN_DETAIL_SECTIONS.some((section) => section.sectionLabel === sectionLabel))
    .forEach(([sectionLabel, ...labelTokens]) => {
      detailGroupsBySection.set(sectionLabel, [
        ...(detailGroupsBySection.get(sectionLabel) ?? []),
        ...labelTokens.map(formatPlanDetailLabel),
      ]);
    });
  const detailGroups = PLAN_DETAIL_SECTIONS.map((section) => ({
    ...section,
    labels: detailGroupsBySection.get(section.sectionLabel) ?? [],
  }));

  return {
    selectedAttempt,
    selectedCost,
    measurements,
    detailGroups,
  };
}

function formatPlanDetailLabel(label: string) {
  const normalizedLabel = label.trim();
  const planDetailLabelBySource = new Map([
    ['Full Scan', 'FULL_SCAN'],
    ['Index Scan', 'INDEX_SCAN'],
    ['Bitmap Scan', 'BITMAP_SCAN'],
    ['Tid Scan', 'TID_SCAN'],
    ['Derived Scan', 'DERIVED_SCAN'],
    ['Nested Loop', 'NESTED_LOOP'],
    ['Merge Join', 'MERGE_JOIN'],
    ['Hash Join', 'HASH_JOIN'],
    ['Access Filter', 'ACCESS_FILTER'],
    ['Post Filter', 'POST_FILTER'],
    ['Plain Sort', 'PLAIN_SORT'],
    ['Incremental Sort', 'INCREMENTAL_SORT'],
    ['Hash Agg', 'HASH_AGG'],
    ['Group Agg', 'GROUP_AGG'],
    ['Unique Agg', 'UNIQUE_AGG'],
    ['Used', getUiTextValue('RUNTIME_USED_LABEL', '사용')],
  ]);

  return planDetailLabelBySource.get(normalizedLabel) ?? normalizedLabel.toUpperCase().replaceAll(' ', '_');
}

function formatSubmitProgressDetailLine(detailLine: string) {
  const hiddenCaseMatch = detailLine.match(/^Case\s+\d+\s*:\s*Hidden\s+(\d+)\s+(.+)$/i);
  if (hiddenCaseMatch != null) {
    return `Case Hidden ${hiddenCaseMatch[1]} : ${hiddenCaseMatch[2].trim()}`;
  }

  const openCaseMatch = detailLine.match(/^Case\s+\d+\s*:\s*Open\s+(.+)$/i);
  if (openCaseMatch != null) {
    return `Case Open : ${openCaseMatch[1].trim()}`;
  }

  return detailLine;
}

function SubmitProgressItem({ step }: { step: ProblemSubmitProgressStep }) {
  const [isPlanDetailModalOpen, setPlanDetailModalOpen] = useState(false);
  const toneClass =
    step.status === 'running'
      ? 'is-pending'
      : step.status === 'success'
        ? 'is-success'
        : step.status === 'skipped'
          ? 'is-skipped'
          : 'is-error';
  const indicator = step.status === 'running'
    ? <span className="solve-editor-statement-spinner" />
    : step.status === 'success'
      ? '✓'
      : step.status === 'skipped'
        ? '–'
        : '✕';
  const planProgressView = step.stepKey === 'plan' ? createPlanProgressView(step.detailLines) : null;
  const displayMessage = step.stepKey === 'plan' && step.status === 'success'
    ? getUiTextValue('PROBLEM_SOLVE_PLAN_PROGRESS_SUCCESS_TITLE', '실행계획 분석 완료')
    : step.message;
  const shouldRenderPlanProgress = planProgressView != null && planProgressView.measurements.length > 0;
  const shouldRenderAnswerCaseErrorDetails =
    isAnswerCaseProgressStep(step.stepKey) && step.status === 'error' && step.detailLines.length > 0;
  const shouldRenderDetailLines =
    step.detailLines.length > 0 && !shouldRenderPlanProgress
    && (!isAnswerCaseProgressStep(step.stepKey) || shouldRenderAnswerCaseErrorDetails);
  const canOpenPlanDetails = step.stepKey === 'plan' && step.status === 'success' && planProgressView != null && planProgressView.selectedAttempt != null;

  return (
    <div className={`solve-editor-inline-result-group solve-submit-progress-item ${toneClass}`.trim()}>
      <div className="solve-editor-inline-result-header">
        <div className={`solve-pane-summary-row ${canOpenPlanDetails ? 'is-plan-detail-action' : ''}`.trim()}>
          <span className={`solve-pane-summary-status-button ${toneClass}`.trim()} aria-hidden="true">
            {indicator}
          </span>
          {canOpenPlanDetails ? (
            <span className="solve-submit-plan-title-action">
              <span className={`solve-pane-summary-statement-title ${toneClass}`.trim()}>{displayMessage}</span>
              <button
                type="button"
                className="submit-history-detail-button solve-submit-plan-detail-button"
                aria-label={getUiTextValue('SUBMIT_HISTORY_PLAN_DETAIL_BUTTON_LABEL', '실행 계획 요소 자세히 보기')}
                title={getUiTextValue('SUBMIT_HISTORY_PLAN_DETAIL_BUTTON_LABEL', '실행 계획 요소 자세히 보기')}
                onClick={() => setPlanDetailModalOpen(true)}
              >
                ↗
              </button>
            </span>
          ) : (
            <span className={`solve-pane-summary-statement-title ${toneClass}`.trim()}>{displayMessage}</span>
          )}
        </div>
      </div>
      {shouldRenderPlanProgress ? (
        <div className="solve-submit-plan-progress">
          <div className="solve-submit-plan-measurements">
            {planProgressView.measurements.map((measurement) => (
              <div
                key={`plan-measurement-${measurement.attempt}`}
                className={`solve-submit-plan-measurement ${measurement.selected ? 'is-selected' : planProgressView.selectedAttempt == null ? '' : 'is-discarded'}`.trim()}
              >
                {`실행계획 분석 ${measurement.attempt}회 : Cost ${measurement.cost}${measurement.selected ? ' (중앙값)' : ''}`}
              </div>
            ))}
          </div>
        </div>
      ) : null}
      {shouldRenderDetailLines ? (
        <div className="solve-editor-inline-result-body solve-pane-result-stack">
          {step.detailLines.map((detailLine) => (
            <p key={`${step.stepKey}-${detailLine}`} className={`solve-pane-result-message ${toneClass === 'is-error' ? 'is-error' : ''}`.trim()}>
              {formatSubmitProgressDetailLine(detailLine)}
            </p>
          ))}
        </div>
      ) : null}
      {isPlanDetailModalOpen && planProgressView != null && typeof document !== 'undefined'
        ? createPortal(
            <div
              className="submit-history-modal-overlay"
              role="presentation"
              onMouseDown={(event) => {
                if (event.target === event.currentTarget) {
                  setPlanDetailModalOpen(false);
                }
              }}
            >
              <div className="submit-history-modal submit-history-plan-modal solve-submit-plan-modal" role="dialog" aria-modal="true" aria-label={getUiTextValue('SUBMIT_HISTORY_PLAN_MODAL_LABEL', '실행 계획 요소 보기')}>
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <strong>{getUiTextValue('SUBMIT_HISTORY_PLAN_TITLE', '실행 계획 요소')}</strong>
                  </div>
                  <button
                    type="button"
                    className="submit-history-modal-close"
                    aria-label={getUiTextValue('SUBMIT_HISTORY_PLAN_MODAL_CLOSE_LABEL', '실행 계획 요소 닫기')}
                    onClick={() => setPlanDetailModalOpen(false)}
                  >
                    {getUiTextValue('COMMON_CLOSE_BUTTON', '닫기')}
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-plan-modal-body">
                  <ExecutionPlanDetailBoard
                    sections={planProgressView.detailGroups}
                    noneLabel={getUiTextValue('COMMON_NONE_LABEL', '없음')}
                    className="submit-history-plan-detail-board solve-submit-plan-detail-board"
                  />
                </div>
              </div>
            </div>,
            document.body,
          )
        : null}
    </div>
  );
}

function SolveResultPagination({
  currentPage,
  totalPages,
  isPageLoading,
  onPageChange,
}: {
  currentPage: number;
  totalPages: number;
  isPageLoading: boolean;
  onPageChange: (page: number) => void;
}) {
  const pageJump = usePageJump({ currentPage, totalPages, onPageChange });

  return (
    <div className="solve-result-pagination">
      <button
        type="button"
        className="mini-toggle solve-result-pagination-button"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1 || isPageLoading}
      >
        {getUiTextValue('COMMON_PREVIOUS_BUTTON', '이전')}
      </button>

      {pageJump.isEditing ? (
        <input
          type="text"
          inputMode="numeric"
          className="text-field solve-result-pagination-input"
          aria-label={getUiTextValue('PROBLEM_SOLVE_RESULT_PAGE_INPUT_LABEL', '이동할 페이지 입력')}
          value={pageJump.draft}
          onChange={(event) => pageJump.setDraft(event.target.value)}
          onBlur={pageJump.applyPageJump}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault();
              pageJump.applyPageJump();
              return;
            }

            if (event.key === 'Escape') {
              event.preventDefault();
              pageJump.cancelPageJump();
            }
          }}
          autoFocus
        />
      ) : (
        <button
          type="button"
          className="solve-result-pagination-label solve-result-pagination-meta-button"
          aria-label={getUiTextValue('PROBLEM_SOLVE_RESULT_PAGE_INPUT_OPEN_LABEL', '이동할 페이지 입력 열기')}
          disabled={isPageLoading}
          onClick={pageJump.openPageJump}
        >
          {`${currentPage} / ${totalPages}`}
        </button>
      )}

      <button
        type="button"
        className="mini-toggle solve-result-pagination-button"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages || isPageLoading}
      >
        {getUiTextValue('COMMON_NEXT_BUTTON', '다음')}
      </button>
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
  isPageLoading: boolean,
  resetKey: number,
  onResetWidths: () => void,
) {
  if (rowCount === 0) {
    return <div className="solve-result-empty solve-result-empty-table">{emptyMessage}</div>;
  }

  const columnLabels =
    columns.length > 0
      ? columns
      : Array.from(
          { length: rows.reduce((maxCount, row) => Math.max(maxCount, row.length), 0) },
          (_, index) => getUiText('PROBLEM_SOLVE_RESULT_COLUMN_FALLBACK', { index: index + 1 }, '컬럼 {index}'),
        );

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
              aria-label={getUiTextValue('PROBLEM_SOLVE_RESULT_WIDTH_RESET_LABEL', '실행 결과 너비 초기화')}
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
            <div className="solve-result-table-grid-overlay" aria-live="polite" aria-label={getUiTextValue('PROBLEM_SOLVE_RESULT_PAGE_LOADING_LABEL', '실행 결과 페이지 로딩 중')}>
              <span className="solve-result-table-grid-spinner" aria-hidden="true" />
            </div>
          ) : null}
        </div>
        {totalPages > 1 ? <SolveResultPagination currentPage={normalizedPage} totalPages={totalPages} isPageLoading={isPageLoading} onPageChange={onPageChange} /> : null}
      </div>
    </div>
  );
}

function renderExecutionContent(
  executionResult: ProblemExecutionResult,
  currentPage: number,
  onPageChange: (page: number) => void,
  isPageLoading: boolean,
  resetKey: number,
  onResetWidths: () => void,
) {
  if (!executionResult.success) {
    return <p className="solve-pane-result-message is-error">{executionResult.message ?? getUiTextValue('PROBLEM_SOLVE_EXECUTION_FAIL_MESSAGE', '실행에 실패했습니다.')}</p>;
  }

  if (executionResult.mode === 'select') {
    return renderResultTable(
      executionResult.columns,
      createExecutionResultPageRows(executionResult, currentPage),
      getUiTextValue('PROBLEM_SOLVE_RESULT_EMPTY_STATE', '표시할 실행 결과가 없습니다.'),
      executionResult.rowCount,
      currentPage,
      EXECUTION_RESULT_PAGE_SIZE,
      onPageChange,
      isPageLoading,
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
  const [gridResetKey, setGridResetKey] = useState(0);
  const [isPageLoading, setIsPageLoading] = useState(false);
  const executionItemRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setCollapsed(false);
    setCurrentPage(item.result?.currentPage ?? 1);
    setGridResetKey(0);
    setIsPageLoading(false);
  }, [item.key, item.status, item.result?.mode, item.result?.rowCount, item.result?.message]);

  useEffect(() => {
    registerResultItemRef(item.key, executionItemRef.current);

    return () => {
      registerResultItemRef(item.key, null);
    };
  }, [item.key, registerResultItemRef]);

  const executionResult = item.result;
  const showErrorSummary = item.status === 'error' || (executionResult != null && !executionResult.success);
  const executionResultTotalPages =
    executionResult != null && executionResult.success && executionResult.mode === 'select'
      ? getExecutionResultPageCount(executionResult.rowCount)
      : 1;
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

    if (isExecutionResultPageCached(executionResult, normalizedPage)) {
      setCurrentPage(normalizedPage);
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

  return (
    <div
      ref={executionItemRef}
      className={`solve-editor-inline-result-group ${collapsed ? 'is-collapsed' : ''} ${resultToneClass}`.trim()}
    >
      <div className="solve-editor-inline-result-header">
        <button
          type="button"
          className="solve-detail-section-divider-button solve-pane-section-divider-button"
          aria-label={collapsed ? getUiTextValue('COMMON_EXPAND_ACTION', '펼치기') : getUiTextValue('COMMON_COLLAPSE_ACTION', '접기')}
          aria-expanded={!collapsed}
          onClick={() => setCollapsed((current) => !current)}
        >
          <CollapseChevronIcon collapsed={collapsed} />
        </button>
        <div className="solve-pane-summary-row">
          <button
            type="button"
            className={`solve-pane-summary-status-button ${titleToneClass}`.trim()}
            aria-label={getUiTextValue('PROBLEM_SOLVE_MOVE_RESULT_BUTTON', '실행 결과 위치로 이동')}
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
            <div className="solve-result-empty solve-result-empty-table">
              {item.progressMessage ?? getUiTextValue('PROBLEM_SOLVE_RUNNING_MESSAGE', 'SQL을 실행하는 중입니다.')}
            </div>
          ) : executionResult ? (
            renderExecutionContent(
              executionResult,
              currentPage,
              requestPage,
              isPageLoading,
              gridResetKey,
              () => setGridResetKey((current) => current + 1),
            )
          ) : (
            <div className="solve-result-empty solve-result-empty-table">{getUiTextValue('PROBLEM_SOLVE_PENDING_MESSAGE', '실행 대기 중')}</div>
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
  const { text } = useUiText();
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginErrors, setLoginErrors] = useState<string[]>([]);
  const [isLoginSubmitting, setIsLoginSubmitting] = useState(false);
  const [isSocialLoginSubmitting, setIsSocialLoginSubmitting] = useState(false);

  const [signupEmail, setSignupEmail] = useState('');
  const [signupCode, setSignupCode] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupPasswordConfirm, setSignupPasswordConfirm] = useState('');
  const [signupErrors, setSignupErrors] = useState<string[]>([]);
  const [signupStatusMessage, setSignupStatusMessage] = useState<string | null>(null);
  const [isSignupSubmitting, setIsSignupSubmitting] = useState(false);
  const [isSendingSignupCode, setIsSendingSignupCode] = useState(false);
  const [isVerifyingSignupCode, setIsVerifyingSignupCode] = useState(false);
  const [isSignupCodeSent, setIsSignupCodeSent] = useState(false);
  const [isSignupCodeVerified, setIsSignupCodeVerified] = useState(false);
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
  const signupCodeInputRef = useRef<HTMLInputElement | null>(null);
  const signupPasswordInputRef = useRef<HTMLInputElement | null>(null);
  const signupPasswordConfirmInputRef = useRef<HTMLInputElement | null>(null);
  const resetCodeInputRef = useRef<HTMLInputElement | null>(null);
  const resetPasswordInputRef = useRef<HTMLInputElement | null>(null);
  const resetPasswordConfirmInputRef = useRef<HTMLInputElement | null>(null);

  const normalizedLoginEmail = loginEmail.trim();
  const normalizedSignupEmail = signupEmail.trim();
  const normalizedSignupCode = signupCode.trim().toUpperCase();
  const normalizedResetEmail = resetEmail.trim();
  const normalizedResetCode = resetCode.trim().toUpperCase();
  const signupEmailHint = text('AUTH_EMAIL_HINT', '올바른 이메일 형식으로 입력해 주세요.');
  const signupEmailCheckingMessage = text('AUTH_EMAIL_CHECKING_MESSAGE', '이메일 사용 가능 여부를 확인하는 중입니다.');
  const signupEmailAvailableMessage = text('AUTH_EMAIL_AVAILABLE_MESSAGE', '사용 가능한 이메일입니다.');
  const signupEmailDuplicatedMessage = text('AUTH_EMAIL_DUPLICATED_MESSAGE', '이미 사용 중인 이메일입니다.');
  const signupCodeHint = text('AUTH_CODE_HINT', '이메일로 받은 인증코드 6자를 입력해 주세요.');
  const signupCodeSentMessage = text('AUTH_CODE_SENT_MESSAGE', '인증 코드를 전송했습니다. 5분 이내에 입력해 주세요.');
  const signupCodeVerifiedMessage = text('AUTH_CODE_VERIFIED_MESSAGE', '인증 코드가 확인되었습니다. 비밀번호를 입력해 주세요.');
  const signupPasswordHint = text('AUTH_PASSWORD_HINT', '특수문자를 포함해 8자 이상 입력해 주세요.');
  const signupPasswordConfirmHint = text('AUTH_PASSWORD_CONFIRM_HINT', '비밀번호를 다시 입력해 주세요.');
  const resetCodeSentMessage = text('AUTH_CODE_SENT_MESSAGE', '인증 코드를 전송했습니다. 5분 이내에 입력해 주세요.');
  const resetCodeVerifiedMessage = text('AUTH_RESET_CODE_VERIFIED_MESSAGE', '인증 코드가 확인되었습니다. 새 비밀번호를 입력해 주세요.');
  const resetPasswordChangedMessage = text('AUTH_RESET_PASSWORD_CHANGED_MESSAGE', '비밀번호가 변경되었습니다. 다시 로그인해 주세요.');
  const resetCodeSentStatusMessage = resetStatusMessage === resetCodeSentMessage ? resetStatusMessage : null;
  const resetCodeVerifiedStatusMessage = resetStatusMessage === resetCodeVerifiedMessage ? resetStatusMessage : null;
  const resetPasswordChangedStatusMessage = resetStatusMessage === resetPasswordChangedMessage ? resetStatusMessage : null;
  const signupCodeSentStatusMessage = signupStatusMessage === signupCodeSentMessage ? signupStatusMessage : null;
  const signupCodeVerifiedStatusMessage = signupStatusMessage === signupCodeVerifiedMessage ? signupStatusMessage : null;
  const isLoginReady = normalizedLoginEmail !== '' && loginPassword.trim() !== '';
  const isSignupEmailValid = EMAIL_PATTERN.test(normalizedSignupEmail);
  const isSignupCodeValid = PASSWORD_RESET_CODE_PATTERN.test(normalizedSignupCode);
  const isSignupPasswordValid = hasRequiredPasswordFormat(signupPassword);
  const isSignupPasswordConfirmValid = signupPasswordConfirm !== '' && signupPasswordConfirm === signupPassword;
  const isSignupReady =
    isSignupEmailValid &&
    isSignupCodeVerified &&
    isSignupPasswordValid &&
    isSignupPasswordConfirmValid &&
    signupEmailCheckStatus !== 'checking';
  const isResetEmailValid = EMAIL_PATTERN.test(normalizedResetEmail);
  const isResetCodeValid = PASSWORD_RESET_CODE_PATTERN.test(normalizedResetCode);
  const isResetPasswordValid = hasRequiredPasswordFormat(newPassword);
  const isResetPasswordConfirmValid = newPasswordConfirm !== '' && newPasswordConfirm === newPassword;
  const signupEmailHintMessage =
    signupCodeSentStatusMessage ??
    (normalizedSignupEmail === ''
      ? signupEmailHint
      : !isSignupEmailValid
        ? signupEmailHint
        : signupEmailCheckStatus === 'checking'
          ? signupEmailCheckingMessage
          : signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'duplicated'
            ? (signupEmailCheckReason ?? signupEmailDuplicatedMessage)
            : signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'available'
              ? signupEmailAvailableMessage
              : signupEmailHint);
  const hasSignupEmailError =
    normalizedSignupEmail !== '' &&
    (!isSignupEmailValid || (signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'duplicated'));
  const hasSignupEmailSuccess =
    normalizedSignupEmail !== '' &&
    !hasSignupEmailError &&
    ((signupEmailLastCheckedValue === normalizedSignupEmail && signupEmailCheckStatus === 'available') || signupCodeSentStatusMessage != null);

  const focusNextInput = (nextInputRef: RefObject<HTMLInputElement | null>) => {
    window.requestAnimationFrame(() => {
      nextInputRef.current?.focus();
    });
  };

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
      setLoginErrors([text('AUTH_POPUP_BLOCKED_MESSAGE', '팝업이 차단되어 소셜 로그인을 진행할 수 없습니다.')]);
      return;
    }

    popup.focus();
    let isChecking = false;
    let isCompleting = false;
    let didClosedSessionCheck = false;

    let removeMessageListener = () => {};

    const stopPolling = () => {
      if (socialLoginPopupPollIdRef.current != null) {
        window.clearInterval(socialLoginPopupPollIdRef.current);
        socialLoginPopupPollIdRef.current = null;
      }
      removeMessageListener();
      setIsSocialLoginSubmitting(false);
    };

    const completePopupAuthentication = async (shouldClosePopup: boolean) => {
      if (isCompleting) {
        return;
      }

      isCompleting = true;

      try {
        const session = await fetchSessionMe();
        if (!session.authenticated) {
          isCompleting = false;
          return;
        }

        await completeAuthentication(session);
        if (shouldClosePopup && !popup.closed) {
          popup.close();
        }
        stopPolling();
        onAuthenticated();
      } catch {
        isCompleting = false;
      }
    };

    const handlePopupMessage = (event: MessageEvent) => {
      if (!isTrustedSocialLoginCallbackOrigin(event.origin) || event.data == null || typeof event.data !== 'object') {
        return;
      }

      const message = event.data as { type?: string; provider?: SolveAuthSocialProvider | 'oauth2' | null };
      if (message.type !== SOCIAL_LOGIN_SUCCESS_MESSAGE && message.type !== SOCIAL_LOGIN_ERROR_MESSAGE) {
        return;
      }

      void (async () => {
        if (message.type === SOCIAL_LOGIN_ERROR_MESSAGE) {
          popup.close();
          stopPolling();
          setLoginErrors([getAuthSocialLoginErrorMessage(message.provider ?? null)]);
          return;
        }

        await completePopupAuthentication(true);
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
          if (!didClosedSessionCheck) {
            didClosedSessionCheck = true;
            await completePopupAuthentication(false);
          }
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
              setLoginErrors([getAuthSocialLoginErrorMessage(socialLoginError)]);
              return;
            }

            if (popupUrl.searchParams.has('socialLoginSuccess')) {
              await completePopupAuthentication(true);
              return;
            }
          }
        } catch {
          // 크로스 오리진 팝업은 location 접근을 허용하지 않음
        }
      } catch {
        // 팝업 상태 확인 실패는 다음 polling 주기에서 다시 확인
      } finally {
        isChecking = false;
      }
    };

    socialLoginPopupPollIdRef.current = window.setInterval(() => {
      void pollPopupState();
    }, 1500);
    void pollPopupState();
  };

  const resetSignupEmailCheck = () => {
    setSignupEmailCheckStatus('idle');
    setSignupEmailCheckReason(null);
    setSignupEmailLastCheckedValue('');
  };

  const resetSignupVerification = () => {
    setSignupCode('');
    setSignupStatusMessage(null);
    setIsSignupCodeSent(false);
    setIsSignupCodeVerified(false);
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

      setSignupErrors([error instanceof Error ? error.message : text('AUTH_EMAIL_DUPLICATE_CHECK_FAIL_MESSAGE', '이메일 중복 확인 중 오류가 발생했습니다.')]);
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
      });

      await completeAuthentication(session);
      if (!session.authenticated) {
        setLoginErrors([text('AUTH_LOGIN_FAIL_MESSAGE', '로그인에 실패했습니다.')]);
        return;
      }

      onAuthenticated();
    } catch (error) {
      if (error instanceof AuthApiError) {
        setLoginErrors(error.reasons);
        return;
      }

      setLoginErrors([error instanceof Error ? error.message : text('AUTH_LOGIN_ERROR_MESSAGE', '로그인 중 오류가 발생했습니다.')]);
    } finally {
      setIsLoginSubmitting(false);
    }
  };

  const handleSendSignupCode = async () => {
    if (!isSignupEmailValid || isSendingSignupCode) {
      return false;
    }

    try {
      setIsSendingSignupCode(true);
      setSignupErrors([]);
      setSignupStatusMessage(null);

      const isEmailAvailable = await checkSignupEmailDuplication();
      if (!isEmailAvailable) {
        return false;
      }

      await sendSignupVerificationCode({ email: normalizedSignupEmail });
      setSignupCode('');
      setIsSignupCodeSent(true);
      setIsSignupCodeVerified(false);
      setSignupStatusMessage(signupCodeSentMessage);
      return true;
    } catch (error) {
      if (error instanceof SignupApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrors([error instanceof Error ? error.message : text('AUTH_CODE_SEND_FAIL_MESSAGE', '인증 코드 전송 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSendingSignupCode(false);
    }
  };

  const handleVerifySignupCode = async () => {
    if (!isSignupCodeSent || !isSignupCodeValid || isVerifyingSignupCode) {
      return false;
    }

    try {
      setIsVerifyingSignupCode(true);
      setSignupErrors([]);
      setSignupStatusMessage(null);

      await verifySignupVerificationCode({
        email: normalizedSignupEmail,
        code: normalizedSignupCode,
      });
      setIsSignupCodeVerified(true);
      setSignupStatusMessage(signupCodeVerifiedMessage);
      return true;
    } catch (error) {
      setIsSignupCodeVerified(false);
      if (error instanceof SignupApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrors([error instanceof Error ? error.message : text('AUTH_CODE_VERIFY_FAIL_MESSAGE', '인증 코드 확인 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsVerifyingSignupCode(false);
    }
  };

  const handleSignupSubmit = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (!isSignupReady) {
      return false;
    }

    try {
      setIsSignupSubmitting(true);
      setSignupErrors([]);
      saveSolvePageAuthReturn(problemId, sql, selectedDbms);

      const isEmailAvailable = await checkSignupEmailDuplication();
      if (!isEmailAvailable) {
        return false;
      }

      await signup({
        email: normalizedSignupEmail,
        password: signupPassword,
        code: normalizedSignupCode,
      });

      const session = await fetchSessionMe();
      await completeAuthentication(session);

      if (!session.authenticated) {
        setSignupErrors([text('AUTH_SIGNUP_SESSION_FAIL_MESSAGE', '회원가입 후 세션을 확인하지 못했습니다.')]);
        return false;
      }

      onAuthenticated();
      return true;
    } catch (error) {
      if (error instanceof SignupApiError || error instanceof AuthApiError) {
        applySignupErrorReasons(error.reasons);
        return false;
      }

      setSignupErrors([error instanceof Error ? error.message : text('AUTH_SIGNUP_ERROR_MESSAGE', '회원가입 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSignupSubmitting(false);
    }
  };

  const handleSendResetCode = async () => {
    if (!isResetEmailValid || isSendingResetCode) {
      return false;
    }

    try {
      setIsSendingResetCode(true);
      setResetErrors([]);
      setResetStatusMessage(null);
      await sendPasswordResetCode({ email: normalizedResetEmail });
      setIsResetCodeSent(true);
      setIsResetCodeVerified(false);
      setResetStatusMessage(resetCodeSentMessage);
      return true;
    } catch (error) {
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return false;
      }

      setResetErrors([error instanceof Error ? error.message : text('AUTH_CODE_SEND_FAIL_MESSAGE', '인증 코드 전송 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsSendingResetCode(false);
    }
  };

  const handleVerifyResetCode = async () => {
    if (!isResetEmailValid || !isResetCodeValid || !isResetCodeSent || isVerifyingResetCode) {
      return false;
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
      setResetStatusMessage(resetCodeVerifiedMessage);
      return true;
    } catch (error) {
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return false;
      }

      setResetErrors([error instanceof Error ? error.message : text('AUTH_CODE_VERIFY_FAIL_MESSAGE', '인증 코드 확인 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsVerifyingResetCode(false);
    }
  };

  const handleResetPassword = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    if (!isResetCodeVerified || !isResetPasswordValid || !isResetPasswordConfirmValid || isResettingPassword) {
      return false;
    }

    try {
      setIsResettingPassword(true);
      setResetErrors([]);
      setResetStatusMessage(null);
      await resetPassword({
        email: normalizedResetEmail,
        password: newPassword,
      });
      setResetStatusMessage(resetPasswordChangedMessage);
      setNewPassword('');
      setNewPasswordConfirm('');
      setTimeout(() => {
        onReturnToLogin();
      }, 300);
      return true;
    } catch (error) {
      if (error instanceof RecoveryApiError) {
        setResetErrors(error.reasons);
        return false;
      }

      setResetErrors([error instanceof Error ? error.message : text('AUTH_PASSWORD_CHANGE_FAIL_MESSAGE', '비밀번호 변경 중 오류가 발생했습니다.')]);
      return false;
    } finally {
      setIsResettingPassword(false);
    }
  };

  const overlayTitle =
    mode === 'signup'
      ? text('AUTH_SIGNUP_TITLE', '이메일로 가입하기')
      : mode === 'reset-password'
        ? text('AUTH_RESET_TITLE', '비밀번호 찾기')
        : text('AUTH_LOGIN_TITLE', '로그인');
  const overlayDescription =
    mode === 'signup'
      ? text('PROBLEM_SOLVE_SIGNUP_SQL_KEEP_DESC', '작성 중인 SQL은 유지됩니다. 가입 후 이어서 작성할 수 있습니다.')
      : mode === 'reset-password'
        ? text('PROBLEM_SOLVE_RESET_DESC', '인증 코드를 확인한 뒤 새 비밀번호를 설정합니다.')
        : text('PROBLEM_SOLVE_LOGIN_SQL_KEEP_DESC', '작성 중인 SQL은 유지됩니다. 로그인 후 이어서 작성할 수 있습니다.');

  return (
    <div className="solve-auth-overlay" role="presentation">
      <div className="solve-auth-overlay-backdrop" />
      <section className="solve-auth-modal" role="dialog" aria-modal="true" aria-label={overlayTitle}>
        <button type="button" className="solve-auth-modal-close" aria-label={text('AUTH_LOGIN_MODAL_CLOSE_LABEL', '로그인 팝업 닫기')} onClick={onClose}>
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
              <div className="landing-auth-layout solve-auth-landing-layout">
                <form className="landing-login-panel solve-auth-landing-login-panel" aria-label={text('AUTH_LOGIN_FORM_LABEL', '로그인 입력')} onSubmit={(event) => void handleLoginSubmit(event)}>
                  <div className="field-stack solve-auth-field-stack">
                    <label className="field-label" htmlFor="solve-auth-email">
                      {text('AUTH_EMAIL_LABEL', '이메일')}
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
                      placeholder={text('AUTH_LOGIN_EMAIL_PLACEHOLDER', '이메일을 입력하세요')}
                    />
                  </div>

                  <div className="field-stack solve-auth-field-stack">
                    <label className="field-label" htmlFor="solve-auth-password">
                      {text('AUTH_PASSWORD_LABEL', '비밀번호')}
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
                      placeholder={text('AUTH_LOGIN_PASSWORD_PLACEHOLDER', '비밀번호를 입력하세요')}
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

                  <div className="auth-actions minimal solve-auth-login-actions">
                    <button
                      type="submit"
                      className="btn primary landing-login-submit solve-auth-login-submit"
                      disabled={!isLoginReady || isLoginSubmitting}
                    >
                      {isLoginSubmitting ? text('AUTH_LOGIN_IN_PROGRESS_ELLIPSIS', '로그인 중…') : text('AUTH_LOGIN_TITLE', '로그인')}
                    </button>
                  </div>

                  <button type="button" className="btn text landing-password-reset-link solve-auth-reset-link" onClick={onOpenResetPassword}>
                    {text('AUTH_FORGOT_PASSWORD_LINK', '비밀번호를 잊으셨나요?')}
                  </button>
                </form>

                <div className="landing-auth-divider solve-auth-landing-divider" aria-hidden="true">
                  <span className="landing-auth-divider-line solve-auth-landing-divider-line" />
                  <img className="landing-auth-divider-mark solve-auth-landing-divider-mark" src={logoImage} alt="" />
                  <span className="landing-auth-divider-line solve-auth-landing-divider-line" />
                </div>

                <aside className="landing-access-panel solve-auth-landing-access-panel" aria-label={text('AUTH_ACCOUNT_SUPPORT_LABEL', '계정 지원')}>
                  <div className="landing-access-group landing-access-group-social solve-auth-landing-access-group">
                    <button type="button" className="landing-access-card is-social solve-auth-social-button" onClick={() => startSocialLogin('google')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon solve-auth-social-icon" aria-hidden="true">
                        <GoogleMarkIcon />
                      </span>
                      <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_GOOGLE', 'Google로 계속하기')}</span>
                    </button>

                    <button type="button" className="landing-access-card is-social solve-auth-social-button" onClick={() => startSocialLogin('github')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon solve-auth-social-icon" aria-hidden="true">
                        <GithubMarkIcon />
                      </span>
                      <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_GITHUB', 'Github로 계속하기')}</span>
                    </button>

                    <button type="button" className="landing-access-card is-social solve-auth-social-button" onClick={() => startSocialLogin('kakao')} disabled={isSocialLoginSubmitting}>
                      <span className="landing-access-card-icon solve-auth-social-icon" aria-hidden="true">
                        <KakaoMarkIcon />
                      </span>
                      <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_KAKAO', 'Kakao로 계속하기')}</span>
                    </button>
                  </div>

                  <div className="landing-access-group landing-access-group-support solve-auth-landing-access-group solve-auth-landing-access-group-support">
                    <button type="button" className="landing-access-card is-social is-email solve-auth-social-button" onClick={onOpenSignup}>
                      <span className="landing-access-card-icon solve-auth-social-icon" aria-hidden="true">
                        <EmailMarkIcon />
                      </span>
                      <span className="landing-access-card-title">{text('AUTH_CONTINUE_WITH_EMAIL', '이메일로 계속하기')}</span>
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
                {text('AUTH_EMAIL_LABEL', '이메일')}
              </label>
              <div className="solve-auth-inline-row">
                <input
                  id="solve-signup-email"
                  type="email"
                  className="text-field"
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void (async () => {
                      const isCodeSent = await handleSendSignupCode();
                      if (isCodeSent) {
                        focusNextInput(signupCodeInputRef);
                      }
                    })();
                  }}
                  autoComplete="email"
                  value={signupEmail}
                  onChange={(event) => {
                    setSignupEmail(event.target.value);
                    setSignupErrors([]);
                    resetSignupEmailCheck();
                    resetSignupVerification();
                  }}
                  onBlur={() => {
                    void checkSignupEmailDuplication();
                  }}
                  placeholder={text('AUTH_EMAIL_PLACEHOLDER_POLITE', '이메일을 입력해 주세요.')}
                  aria-invalid={hasSignupEmailError}
                />
                <button type="button" className="btn secondary" onClick={handleSendSignupCode} disabled={!isSignupEmailValid || isSendingSignupCode}>
                  {isSendingSignupCode ? text('COMMON_SENDING_LABEL', '전송 중') : text('AUTH_CODE_SEND_BUTTON', '코드 전송')}
                </button>
              </div>
              <p className={`solve-auth-field-hint ${hasSignupEmailError ? 'is-error' : hasSignupEmailSuccess ? 'is-success' : ''}`}>
                {signupEmailHintMessage}
              </p>
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-signup-code">
                {text('AUTH_CODE_LABEL', '인증 코드')}
              </label>
              <div className="solve-auth-inline-row">
                <input
                  id="solve-signup-code"
                  type="text"
                  className="text-field"
                  ref={signupCodeInputRef}
                  value={signupCode}
                  onChange={(event) => {
                    setSignupCode(sanitizeVerificationCode(event.target.value));
                    setSignupErrors([]);
                    setSignupStatusMessage(null);
                    setIsSignupCodeVerified(false);
                  }}
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void (async () => {
                      const isCodeVerified = await handleVerifySignupCode();
                      if (isCodeVerified) {
                        focusNextInput(signupPasswordInputRef);
                      }
                    })();
                  }}
                  placeholder={text('AUTH_CODE_PLACEHOLDER_POLITE', '이메일로 받은 6자리 코드를 입력해 주세요.')}
                  disabled={!isSignupCodeSent}
                />
                <button
                  type="button"
                  className="btn secondary"
                  onClick={handleVerifySignupCode}
                  disabled={!isSignupCodeSent || !isSignupCodeValid || isVerifyingSignupCode}
                >
                  {isVerifyingSignupCode ? text('COMMON_VERIFYING_LABEL', '확인 중') : text('AUTH_CODE_VERIFY_BUTTON', '코드 확인')}
                </button>
              </div>
              {signupCodeVerifiedStatusMessage ? <p className="solve-auth-field-hint is-success">{signupCodeVerifiedStatusMessage}</p> : null}
              {!signupCodeVerifiedStatusMessage ? <p className="solve-auth-field-hint">{signupCodeHint}</p> : null}
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-signup-password">
                {text('AUTH_PASSWORD_LABEL', '비밀번호')}
              </label>
              <input
                id="solve-signup-password"
                type="password"
                className="text-field"
                ref={signupPasswordInputRef}
                autoComplete="new-password"
                value={signupPassword}
                onChange={(event) => {
                  setSignupPassword(event.target.value);
                  setSignupErrors([]);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  focusNextInput(signupPasswordConfirmInputRef);
                }}
                placeholder={text('AUTH_PASSWORD_PLACEHOLDER_POLITE', '비밀번호를 입력해 주세요.')}
                aria-invalid={signupPassword.length > 0 && !isSignupPasswordValid}
              />
              <p className={`solve-auth-field-hint ${signupPassword.length > 0 && !isSignupPasswordValid ? 'is-error' : signupPassword.length > 0 ? 'is-success' : ''}`}>
                {signupPasswordHint}
              </p>
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-signup-password-confirm">
                {text('AUTH_PASSWORD_CONFIRM_LABEL', '비밀번호 확인')}
              </label>
              <input
                id="solve-signup-password-confirm"
                type="password"
                className="text-field"
                ref={signupPasswordConfirmInputRef}
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
                placeholder={text('AUTH_PASSWORD_CONFIRM_PLACEHOLDER', '비밀번호를 다시 입력해 주세요.')}
                aria-invalid={signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid}
              />
              <p className={`solve-auth-field-hint ${signupPasswordConfirm.length > 0 && !isSignupPasswordConfirmValid ? 'is-error' : signupPasswordConfirm.length > 0 ? 'is-success' : ''}`}>
                {signupPasswordConfirmHint}
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
                {isSignupSubmitting ? text('AUTH_SIGNUP_PROGRESS_LABEL', '가입 중') : text('AUTH_SIGNUP_BUTTON', '가입하기')}
              </button>
              <button type="button" className="solve-auth-reset-link" onClick={onReturnToLogin}>
                {text('AUTH_BACK_TO_LOGIN_BUTTON', '로그인으로 돌아가기')}
              </button>
            </div>
          </form>
        ) : (
          <form className="solve-auth-reset-form" onSubmit={(event) => void handleResetPassword(event)}>
            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-reset-email">
                {text('AUTH_EMAIL_LABEL', '이메일')}
              </label>
              <div className="solve-auth-inline-row">
                <input
                  id="solve-reset-email"
                  type="email"
                  className="text-field"
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void (async () => {
                      const isCodeSent = await handleSendResetCode();
                      if (isCodeSent) {
                        focusNextInput(resetCodeInputRef);
                      }
                    })();
                  }}
                  autoComplete="email"
                  value={resetEmail}
                  onChange={(event) => {
                    setResetEmail(event.target.value);
                    setResetErrors([]);
                    setResetStatusMessage(null);
                  }}
                  placeholder={text('AUTH_RESET_EMAIL_PLACEHOLDER', '가입한 이메일을 입력해 주세요.')}
                />
                <button type="button" className="btn secondary" onClick={handleSendResetCode} disabled={!isResetEmailValid || isSendingResetCode}>
                  {isSendingResetCode ? text('COMMON_SENDING_LABEL', '전송 중') : text('AUTH_CODE_SEND_BUTTON', '코드 전송')}
                </button>
              </div>
              {resetCodeSentStatusMessage ? <p className="solve-auth-field-hint is-success">{resetCodeSentStatusMessage}</p> : null}
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-reset-code">
                {text('AUTH_CODE_LABEL', '인증 코드')}
              </label>
              <div className="solve-auth-inline-row">
                <input
                  id="solve-reset-code"
                  type="text"
                  className="text-field"
                  ref={resetCodeInputRef}
                  value={resetCode}
                  onChange={(event) => {
                    setResetCode(sanitizeVerificationCode(event.target.value));
                    setResetErrors([]);
                    setResetStatusMessage(null);
                  }}
                  onKeyDown={(event) => {
                    if (event.key !== 'Enter') {
                      return;
                    }

                    event.preventDefault();
                    void (async () => {
                      const isCodeVerified = await handleVerifyResetCode();
                      if (isCodeVerified) {
                        focusNextInput(resetPasswordInputRef);
                      }
                    })();
                  }}
                  placeholder={text('AUTH_CODE_PLACEHOLDER_POLITE', '이메일로 받은 6자리 코드를 입력해 주세요.')}
                />
                <button
                  type="button"
                  className="btn secondary"
                  onClick={handleVerifyResetCode}
                  disabled={!isResetCodeSent || !isResetCodeValid || isVerifyingResetCode}
                >
                  {isVerifyingResetCode ? text('COMMON_VERIFYING_LABEL', '확인 중') : text('AUTH_CODE_VERIFY_BUTTON', '코드 확인')}
                </button>
              </div>
              {resetCodeVerifiedStatusMessage ? <p className="solve-auth-field-hint is-success">{resetCodeVerifiedStatusMessage}</p> : null}
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-reset-password">
                {text('AUTH_NEW_PASSWORD_LABEL', '새 비밀번호')}
              </label>
              <input
                id="solve-reset-password"
                type="password"
                className="text-field"
                ref={resetPasswordInputRef}
                value={newPassword}
                onChange={(event) => {
                  setNewPassword(event.target.value);
                  setResetErrors([]);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  focusNextInput(resetPasswordConfirmInputRef);
                }}
                placeholder={text('AUTH_NEW_PASSWORD_PLACEHOLDER', '특수문자를 포함해 8자 이상 입력해 주세요.')}
                disabled={!isResetCodeVerified}
              />
            </div>

            <div className="field-stack solve-auth-field-stack">
              <label className="field-label" htmlFor="solve-reset-password-confirm">
                {text('AUTH_NEW_PASSWORD_CONFIRM_LABEL', '새 비밀번호 확인')}
              </label>
              <input
                id="solve-reset-password-confirm"
                type="password"
                className="text-field"
                ref={resetPasswordConfirmInputRef}
                value={newPasswordConfirm}
                onChange={(event) => {
                  setNewPasswordConfirm(event.target.value);
                  setResetErrors([]);
                }}
                onKeyDown={(event) => {
                  if (event.key !== 'Enter') {
                    return;
                  }

                  event.preventDefault();
                  void handleResetPassword();
                }}
                placeholder={text('AUTH_PASSWORD_CONFIRM_PLACEHOLDER', '비밀번호를 다시 입력해 주세요.')}
                disabled={!isResetCodeVerified}
              />
              {resetPasswordChangedStatusMessage ? <p className="solve-auth-field-hint is-success">{resetPasswordChangedStatusMessage}</p> : null}
            </div>

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
                {isResettingPassword ? text('AUTH_PASSWORD_CHANGING_LABEL', '변경 중') : text('AUTH_PASSWORD_CHANGE_BUTTON', '비밀번호 변경')}
              </button>
              <button type="button" className="solve-auth-reset-link" onClick={onReturnToLogin}>
                {text('AUTH_BACK_TO_LOGIN_BUTTON', '로그인으로 돌아가기')}
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

function PlusGlyphIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path d="M8 3.5v9" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
      <path d="M3.5 8h9" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
    </svg>
  );
}

function PencilIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M3.4 11.6 3 13l1.4-.4 6.9-6.9-1-1-6.9 6.9Z"
        fill="none"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.4"
      />
      <path d="m10.2 4.8 1.1-1.1a.9.9 0 0 1 1.3 0l.2.2a.9.9 0 0 1 0 1.3l-1.1 1.1" fill="none" stroke="currentColor" strokeWidth="1.4" />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path d="M3.5 4.8h9" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.5" />
      <path d="M6.4 3.2h3.2" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.5" />
      <path d="M5 5.5 5.4 13h5.2l.4-7.5" fill="none" stroke="currentColor" strokeLinejoin="round" strokeWidth="1.5" />
      <path d="M7.2 7.2v3.8M8.8 7.2v3.8" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.2" />
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
  const sqlEditorHeightRef = useRef(SQL_EDITOR_MIN_HEIGHT);
  const { text } = useUiText();
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<ProblemSolveFavoriteSnapshot>('problemSolve'), []);
  const favoriteSelectionRestoreRef = useRef<SqlEditorSelection | null>(favoriteRestoreSnapshot?.editorSelection ?? null);
  const executionPanelRef = useRef<HTMLDivElement | null>(null);
  const executionResultItemRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const autocompleteListRef = useRef<HTMLDivElement | null>(null);
  const submitPanelRef = useRef<HTMLElement | null>(null);
  const submitInFlightRef = useRef(false);
  const executionResponseResolverRef = useRef<((result: ProblemExecutionResult) => void) | null>(null);
  const executionStopRequestedRef = useRef(false);
  const ignoredExecutionResponseCountRef = useRef(0);
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const { defaultDbms, isAuthenticated, isReady, handle } = useSession();
  const previousAuthenticationStateRef = useRef(isAuthenticated);
  const fallbackProblem = useMemo(() => createFallbackProblemDetail(problemId), [problemId]);
  const [problemDetail, setProblemDetail] = useState<ProblemDetailData | null>(null);
  const [problemLoadError, setProblemLoadError] = useState<string | null>(null);
  const [problemLoadErrorStatus, setProblemLoadErrorStatus] = useState<number | null>(null);
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
  const availableDbms = useMemo(() => getAvailableDbms(problem), [problem]);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(
    favoriteRestoreSnapshot?.selectedDbms != null && availableDbms.includes(favoriteRestoreSnapshot.selectedDbms)
      ? favoriteRestoreSnapshot.selectedDbms
      : resolvePreferredDbms(availableDbms, problem.dbmsOptions, defaultDbms ?? null)
  );
  const [contentTab, setContentTab] = useState<SolveContentTab>(() => favoriteRestoreSnapshot?.contentTab ?? readSolveContentTabFromSearch(window.location.search));
  const [mySubmitHistoryPage, setMySubmitHistoryPage] = useState<SubmitHistoryPageData>(createEmptySolveSubmitHistoryPage());
  const [isMySubmitLoading, setIsMySubmitLoading] = useState(false);
  const [mySubmitLoadError, setMySubmitLoadError] = useState<string | null>(null);
  const [mySubmitLoadErrorStatus, setMySubmitLoadErrorStatus] = useState<number | null>(null);
  const [mySubmitRequestedPage, setMySubmitRequestedPage] = useState(() => favoriteRestoreSnapshot?.mySubmitRequestedPage ?? 1);
  const [taggedPostPage, setTaggedPostPage] = useState<CommunityPostPage>(createEmptySolveCommunityPage());
  const [isTaggedPostLoading, setIsTaggedPostLoading] = useState(false);
  const [taggedPostLoadError, setTaggedPostLoadError] = useState<string | null>(null);
  const [taggedPostLoadErrorStatus, setTaggedPostLoadErrorStatus] = useState<number | null>(null);
  const [taggedPostRequestedPage, setTaggedPostRequestedPage] = useState(() => favoriteRestoreSnapshot?.taggedPostRequestedPage ?? 1);
  const [relatedModalState, setRelatedModalState] = useState<SolveRelatedModalState>(null);
  const [sql, setSql] = useState(() => favoriteRestoreSnapshot?.sql ?? '');
  const deferredSql = useDeferredValue(sql);
  const [sqlEditorFontSize, setSqlEditorFontSize] = useState(SQL_EDITOR_DEFAULT_FONT_SIZE);
  const getPanelTitle = (panelKey: PanelKey) =>
    panelKey === 'editor'
      ? `${getDbmsLabel(selectedDbms)} ${text('PROBLEM_SOLVE_PANEL_EDITOR_LABEL', '에디터')}`
      : text('PROBLEM_SOLVE_PANEL_RESULT_LABEL', '제출 결과');
  const selectedDdl = useMemo(() => resolveProblemDdl(problemDetail, selectedDbms), [problemDetail, selectedDbms]);
  const ddlAutocompleteItems = useMemo(() => extractAutocompleteItemsFromDdl(selectedDdl), [selectedDdl]);
  const indexTableOptions = useMemo(() => extractIndexTableOptionsFromDdl(selectedDdl), [selectedDdl]);
  const [indexDefinitions, setIndexDefinitions] = useState<SolveIndexDefinition[]>([]);
  const [indexDraft, setIndexDraft] = useState<SolveIndexDraft>(() => createDefaultIndexDraft([], selectedDbms));
  const [isIndexDraftOpen, setIsIndexDraftOpen] = useState(false);
  const [editingIndexId, setEditingIndexId] = useState<string | null>(null);
  const indexDraftHistoryEntryRef = useRef(false);
  const indexAutoParseSkippedSqlRef = useRef<string | null>(null);
  const indexColumnPickerRef = useRef<HTMLDivElement | null>(null);
  const indexColumnPickerDragRef = useRef<{ pointerId: number; startY: number; startScrollTop: number; moved: boolean } | null>(null);
  const indexColumnPickerDragMovedRef = useRef(false);
  const indexMethodOptions = useMemo(() => getIndexMethodOptions(selectedDbms), [selectedDbms]);
  const selectedIndexTable = useMemo(
    () => indexTableOptions.find((tableOption) => tableOption.name === indexDraft.tableName) ?? indexTableOptions[0] ?? null,
    [indexDraft.tableName, indexTableOptions],
  );
  const draftAvailableColumns = selectedIndexTable?.columns ?? [];
  const indexDraftSql = buildCreateIndexSql(indexDraft, selectedDbms);
  const editorIndexParseResult = useMemo(
    () => parseEditorIndexStatements(sql, indexTableOptions, selectedDbms),
    [indexTableOptions, selectedDbms, sql],
  );
  const editorIndexDefinitions = editorIndexParseResult.definitions;
  const editorIndexErrorRanges = editorIndexParseResult.errorRanges;
  const hasParsingIndexStatement = editorIndexErrorRanges.length > 0;
  const visibleIndexDefinitions = indexDefinitions;
  const submitIndexSqls = useMemo(
    () => uniqueSqls([
      ...indexDefinitions.map((indexDefinition) => buildCreateIndexSql(indexDefinition, selectedDbms)),
      ...editorIndexDefinitions.map((indexDefinition) => resolveIndexDefinitionSql(indexDefinition, selectedDbms)),
    ]),
    [editorIndexDefinitions, indexDefinitions, selectedDbms],
  );
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
    () => renderHighlightedSql(sql, sqlHighlightTableNames, sqlHighlightColumnNames, pickerHighlightRanges, editorIndexErrorRanges),
    [editorIndexErrorRanges, pickerHighlightRanges, sql, sqlHighlightColumnNames, sqlHighlightTableNames],
  );
  useEffect(() => {
    setIndexDefinitions([]);
    setIndexDraft(createDefaultIndexDraft(indexTableOptions, selectedDbms));
    setIsIndexDraftOpen(false);
    setEditingIndexId(null);
  }, [indexTableOptions, selectedDbms]);

  useEffect(() => {
    if (indexAutoParseSkippedSqlRef.current === sql) {
      indexAutoParseSkippedSqlRef.current = null;
      return;
    }

    if (editorIndexDefinitions.length === 0) {
      return;
    }

    setIndexDefinitions((current) => {
      const currentSqls = new Set(current.map((indexDefinition) =>
        normalizeComparableSql(resolveIndexDefinitionSql(indexDefinition, selectedDbms))
      ));
      const nextIndexDefinitions = editorIndexDefinitions.filter((indexDefinition) =>
        !currentSqls.has(normalizeComparableSql(resolveIndexDefinitionSql(indexDefinition, selectedDbms)))
      );

      return nextIndexDefinitions.length > 0 ? [...current, ...nextIndexDefinitions] : current;
    });
    setSql((currentSql) =>
      currentSql === sql
        ? removeSqlRanges(
            currentSql,
            editorIndexDefinitions
              .filter((indexDefinition) => indexDefinition.start != null && indexDefinition.end != null)
              .map((indexDefinition) => ({
                start: indexDefinition.start ?? 0,
                end: indexDefinition.end ?? 0,
              })),
          )
        : currentSql
    );
  }, [editorIndexDefinitions, selectedDbms, sql]);

  useEffect(() => {
    const pickerElement = indexColumnPickerRef.current;
    if (!isIndexDraftOpen || pickerElement == null) {
      return;
    }

    const handleWheel = (event: WheelEvent) => {
      const wheelTarget = event.target instanceof Element
        ? event.target.closest('.solve-index-column-picker')
        : null;
      if (wheelTarget !== pickerElement || pickerElement.scrollHeight <= pickerElement.clientHeight) {
        return;
      }

      const wheelDelta = event.deltaMode === 1
        ? event.deltaY * 16
        : event.deltaMode === 2
          ? event.deltaY * pickerElement.clientHeight
          : event.deltaY;
      const previousScrollTop = pickerElement.scrollTop;
      const maxScrollTop = pickerElement.scrollHeight - pickerElement.clientHeight;
      const nextScrollTop = Math.min(Math.max(previousScrollTop + wheelDelta, 0), maxScrollTop);

      if (nextScrollTop === previousScrollTop) {
        return;
      }

      pickerElement.scrollTop = nextScrollTop;
      event.preventDefault();
      event.stopPropagation();
    };

    document.addEventListener('wheel', handleWheel, { capture: true, passive: false });
    return () => document.removeEventListener('wheel', handleWheel, true);
  }, [isIndexDraftOpen, draftAvailableColumns.length]);

  useEffect(() => {
    if (!isIndexDraftOpen || typeof window === 'undefined') {
      return;
    }

    if (!indexDraftHistoryEntryRef.current) {
      const currentHistoryState = typeof window.history.state === 'object' && window.history.state != null
        ? window.history.state
        : {};
      window.history.pushState({ ...currentHistoryState, solveIndexDraftModal: true }, '', window.location.href);
      indexDraftHistoryEntryRef.current = true;
    }

    const closeIndexDraftByHistory = () => {
      indexDraftHistoryEntryRef.current = false;
      setIsIndexDraftOpen(false);
    };

    window.addEventListener('popstate', closeIndexDraftByHistory);
    return () => window.removeEventListener('popstate', closeIndexDraftByHistory);
  }, [isIndexDraftOpen]);

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
    const currentStatementSegments = parseSqlStatements(deferredSql);
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
  }, [deferredSql, executionRuns]);
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
    problemDetail?.title ?? problem.title ?? text('HEADER_MENU_PROBLEMS', '문제');

  const shouldRenderPanel = (panelKey: PanelKey) => panelKey === 'editor' || submitMessage != null || submitProgressSteps.length > 0;
  const visibleFloatingPanels = panelOrder.filter(
    (panelKey) =>
      shouldRenderPanel(panelKey) && panelVisibility[panelKey] && detachedPanels[panelKey] && !externalWindowPanels[panelKey],
  );
  const visibleExternalWindows = panelOrder.filter(
    (panelKey) => shouldRenderPanel(panelKey) && panelVisibility[panelKey] && externalWindowPanels[panelKey],
  );
  const scrollPanelSectionIntoView = (resolveElement: () => HTMLElement | null) => {
    requestAnimationFrame(() => {
      const element = resolveElement();
      if (!element) {
        return;
      }

      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
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

  const releaseSubmitLock = () => {
    submitInFlightRef.current = false;
    setIsSubmitting(false);
  };

  const openAuthOverlay = (mode: SolveAuthOverlayMode = 'login') => {
    setIsExecuting(false);
    releaseSubmitLock();
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
    setProblemLoadErrorStatus(null);

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

        setProblemLoadError(error instanceof Error ? error.message : getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        const status = getApiErrorStatus(error);
        setProblemLoadErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
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
    releaseSubmitLock();
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
    setMySubmitLoadErrorStatus(null);
    setMySubmitRequestedPage(restoredMySubmitPage);
    setTaggedPostPage(createEmptySolveCommunityPage());
    setIsTaggedPostLoading(false);
    setTaggedPostLoadError(null);
    setTaggedPostLoadErrorStatus(null);
    setTaggedPostRequestedPage(restoredTaggedPostPage);
    setRelatedModalState(null);
  }, [favoriteRestoreSnapshot, problemId]);

  useEffect(() => {
    if (contentTab !== 'submissions') {
      return;
    }

    if (!isReady) {
      return;
    }

    if (!isAuthenticated || !handle) {
      setMySubmitHistoryPage(createEmptySolveSubmitHistoryPage());
      setIsMySubmitLoading(false);
      setMySubmitLoadError(null);
      setMySubmitLoadErrorStatus(null);
      return;
    }

    let cancelled = false;
    setIsMySubmitLoading(true);
    setMySubmitLoadError(null);
    setMySubmitLoadErrorStatus(null);

    void fetchSubmitHistories({
      page: mySubmitRequestedPage,
      submitId: '',
      query: handle,
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

        setMySubmitLoadError(error instanceof Error ? error.message : getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        const status = getApiErrorStatus(error);
        setMySubmitLoadErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
      })
      .finally(() => {
        if (!cancelled) {
          setIsMySubmitLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [contentTab, displayProblemNumber, isAuthenticated, isReady, mySubmitRequestedPage, selectedDbms, handle]);

  useEffect(() => {
    if (contentTab !== 'community') {
      return;
    }

    let cancelled = false;
    setIsTaggedPostLoading(true);
    setTaggedPostLoadError(null);
    setTaggedPostLoadErrorStatus(null);

    async function loadTaggedPosts() {
      try {
        const nextPage = await fetchCommunityPosts({
          page: taggedPostRequestedPage,
          search: '',
          tag: displayProblemNumber,
          category: 'all',
          sortKey: 'default',
        });

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

        setTaggedPostLoadError(error instanceof Error ? error.message : getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
        const status = getApiErrorStatus(error);
        setTaggedPostLoadErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
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
  }, [contentTab, displayProblemNumber, taggedPostRequestedPage]);

  useEffect(() => {
    if (!sqlEditorElement) {
      return;
    }

    const textarea = sqlEditorElement;
    textarea.style.height = 'auto';

    const nextHeight = Math.max(SQL_EDITOR_MIN_HEIGHT, textarea.scrollHeight);
    if (sqlEditorHeightRef.current === nextHeight) {
      textarea.style.height = `${nextHeight}px`;
      return;
    }

    textarea.style.height = `${nextHeight}px`;
    sqlEditorHeightRef.current = nextHeight;
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
          deferredSql,
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
    deferredSql,
    executionStatementMarkerRuns,
    externalWindowPanels.editor,
    panelVisibility.editor,
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
    if (autocompleteState == null) {
      return;
    }

    const selectedItem = autocompleteListRef.current?.querySelector<HTMLElement>(
      `[data-autocomplete-index="${autocompleteState.selectedIndex}"]`,
    );
    selectedItem?.scrollIntoView({ block: 'nearest' });
  }, [autocompleteState?.selectedIndex, autocompleteState?.items.length]);

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
        const stepKey = progressMessage.stepKey;
        if (stepKey) {
          setSubmitProgressSteps((current) => {
            const nextProgressStep = createSubmitProgressStep(
              stepKey,
              progressMessage.status ?? 'running',
              progressMessage.message ?? '',
              Array.isArray(progressMessage.detailLines) ? progressMessage.detailLines : [],
            );

            if (stepKey === 'validate' && progressMessage.status === 'error') {
              return [nextProgressStep];
            }

            return upsertSubmitProgressStep(current, nextProgressStep);
          });
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

      if (message.type === 'problem.execute.progress') {
        const progressMessage = message as ProblemExecutionProgressMessage;
        if (progressMessage.problemId !== problemId || !progressMessage.message) {
          return;
        }

        setExecutionRuns((current) =>
          current.map((run) =>
            run.status === 'running'
              ? {
                  ...run,
                  progressMessage: progressMessage.message,
                }
              : run,
          ),
        );
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

      if (message.type === 'problem.submit.result') {
        const submitMessage = message as ProblemSocketMessage;
        const nextSubmitMessage = resolveSocketFailureMessage(
          submitMessage,
          text('PROBLEM_SOLVE_SUBMIT_RECORD_FAIL_MESSAGE', '제출을 기록하지 못했습니다.'),
        );
        if (isAuthenticationRequiredMessage(nextSubmitMessage)) {
          releaseSubmitLock();
          setSubmitProgressSteps([]);
          setSubmitMessage(null);
          openAuthOverlay('login');
          return;
        }

        const failureMessage =
          submitMessage.success === false && nextSubmitMessage !== text('SUBMIT_HISTORY_RESULT_WRONG_LABEL', '오답')
            ? nextSubmitMessage
            : null;
        releaseSubmitLock();
        setSubmitMessage(null);
        if (failureMessage) {
          setSubmitProgressSteps((current) => {
            if (current.some((step) => step.status === 'error' || step.status === 'incorrect')) {
              return current;
            }

            const failureStep = [...current].reverse().find((step) => step.status === 'running') ?? current.at(-1);
            return upsertSubmitProgressStep(
              current,
              createSubmitProgressStep(
                failureStep?.stepKey ?? 'answer',
                'error',
                failureMessage,
                Array.isArray(submitMessage.reasons) ? submitMessage.reasons : [failureMessage],
              ),
            );
          });
        } else if (submitMessage.success === false) {
          setSubmitProgressSteps(skipRunningAnswerCaseProgressSteps);
        }
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
        scrollPanelSectionIntoView(() => submitPanelRef.current);
        return;
      }

      if (message.type === 'problem.execute.result' || message.type === 'error') {
        const executionMessage = message as ProblemSocketMessage;
        const nextExecutionResult =
          message.type === 'error'
            ? createProblemExecutionError(
                resolveSocketFailureMessage(executionMessage, getUiTextValue('PROBLEM_SOLVE_EXECUTION_FAIL_MESSAGE', '실행에 실패했습니다.')),
                Array.isArray(executionMessage.reasons) ? executionMessage.reasons : [],
              )
            : toProblemExecutionResult(executionMessage);

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
      sendSessionSocketMessageIfOpen(SESSION_SOCKET_DESTINATION.problemLeave, {
        problemId,
      });
    };
  }, [problemId, updateSqlEditorSelection]);

  useEffect(() => {
    releaseSubmitLock();
    setSubmitMessage(null);
    setSubmitProgressSteps([]);
    setPanelVisibility((current) => ({
      ...current,
      submit: false,
    }));
  }, [problemId]);

  useEffect(() => {
    const handleBeforeUnload = () => {
      sendSessionSocketMessageIfOpen(SESSION_SOCKET_DESTINATION.problemLeave, {
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
    try {
      if (hasAutocompleteTokenAfterCaret(nextSql, caretIndex)) {
        setAutocompleteState(null);
        return;
      }

      const tokenRange = getAutocompleteTokenRange(nextSql, caretIndex);
      if (!tokenRange) {
        setAutocompleteState(null);
        return;
      }

      const autocompleteContext = createContextualAutocompleteContext(
        nextSql, caretIndex, tokenRange,
        indexTableOptions, autocompleteItems,
      );
      const suggestions = createAutocompleteSuggestions(
        autocompleteContext.items, tokenRange.typedToken, autocompleteContext.kindPriority,
      );
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
    } catch {
      setAutocompleteState(null);
    }
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
    if (submitInFlightRef.current || isSubmitting) {
      return;
    }

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

    if (sql.trim().length === 0) {
      setSubmitProgressSteps([]);
      setSubmitMessage(text('PROBLEM_SOLVE_SUBMIT_SQL_REQUIRED_MESSAGE', '제출할 SQL을 입력해 주세요.'));
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
      setSubmitMessage(text('PROBLEM_SOLVE_SUBMIT_SQL_REQUIRED_MESSAGE', '제출할 SQL을 입력해 주세요.'));
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
      setSubmitMessage(text('PROBLEM_SOLVE_SELECT_REQUIRED_MESSAGE', '제출 가능한 SELECT 구문이 없습니다.'));
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
      const fetchPage = getExecutionResultFetchPage(page);
      const destination = page === 1
        ? SESSION_SOCKET_DESTINATION.problemExecute
        : SESSION_SOCKET_DESTINATION.problemExecutePage;

      try {
        await sendSessionSocketMessage(destination, {
          problemId,
          sql: statementSql,
          dbms: selectedDbms,
          page: fetchPage,
          pageSize: EXECUTION_RESULT_FETCH_PAGE_SIZE,
          indexSqls: submitIndexSqls,
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
                result: createProblemExecutionError(error instanceof SessionSocketError ? error.message : getUiTextValue('PROBLEM_SOLVE_CONNECTION_FAIL_MESSAGE', '문제 실행 연결에 실패했습니다.')),
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

      const nextErrorResult = createProblemExecutionError(error instanceof SessionSocketError ? error.message : getUiTextValue('PROBLEM_SOLVE_CONNECTION_FAIL_MESSAGE', '문제 실행 연결에 실패했습니다.'));
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
    if (submitInFlightRef.current || isSubmitting) {
      return;
    }

    const selectedSubmitSql = statementSegments.map((statement) => statement.sql).join(';\n');

    if (selectedSubmitSql.trim().length === 0) {
      setSubmitProgressSteps([]);
      setSubmitMessage(text('PROBLEM_SOLVE_SUBMIT_SQL_REQUIRED_MESSAGE', '제출할 SQL을 입력해 주세요.'));
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

    submitInFlightRef.current = true;
    setIsSubmitting(true);
    setSubmitMessage(null);
    setSubmitProgressSteps([
      createSubmitProgressStep('validate', 'running', text('PROBLEM_SOLVE_SQL_VALIDATE_RUNNING_MESSAGE', 'SQL 오류 검사 중')),
    ]);
    setCollapsedCards((current) => ({
      ...current,
      submit: false,
    }));
    setPanelVisibility((current) => ({
      ...current,
      submit: true,
    }));
    scrollPanelSectionIntoView(() => submitPanelRef.current);

    try {
      await sendSessionSocketMessage(SESSION_SOCKET_DESTINATION.problemSubmit, {
        problemId,
        sql: selectedSubmitSql,
        dbms: selectedDbms,
        indexSqls: submitIndexSqls,
      });
    } catch (error) {
      releaseSubmitLock();
      setSubmitProgressSteps([]);

      const isSessionValid = await syncSession();
      if (!isSessionValid) {
        openAuthOverlay('login');
        return;
      }

      setSubmitMessage(error instanceof SessionSocketError ? error.message : text('PROBLEM_SOLVE_SUBMIT_CONNECTION_FAIL_MESSAGE', '제출 연결에 실패했습니다.'));
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

    if (sql.trim().length === 0) {
      setExecutionRuns([createSingleExecutionStatementRun(sql, createProblemExecutionError(text('PROBLEM_SOLVE_EXECUTE_SQL_REQUIRED_MESSAGE', '실행할 SQL을 입력해 주세요.')))]);
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
      setExecutionRuns([createSingleExecutionStatementRun(sql, createProblemExecutionError(text('PROBLEM_SOLVE_EXECUTE_SQL_REQUIRED_MESSAGE', '실행할 SQL을 입력해 주세요.')))]);
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
    resolveExecution?.(createProblemExecutionError(text('PROBLEM_SOLVE_STOPPED_MESSAGE', '중지됨')));
    void sendSessionSocketMessage(SESSION_SOCKET_DESTINATION.problemExecuteStop, {
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

  const handleIndexDraftTableChange = (tableName: string) => {
    const tableOption = indexTableOptions.find((option) => option.name === tableName);
    const columns = tableOption?.columns[0] ? [createIndexColumn(tableOption.columns[0])] : [];

    setIndexDraft((current) => ({
      ...current,
      tableName,
      columns,
      includeColumns: [],
    }));
  };

  const handleIndexDraftMethodChange = (method: SolveIndexMethod) => {
    setIndexDraft((current) => ({
      ...current,
      method,
      unique: supportsUniqueIndex(selectedDbms, method) ? current.unique : false,
      columns: current.columns.map((column) =>
        supportsIndexColumnOrdering(method)
          ? column
          : {
              ...column,
              direction: 'ASC',
              nulls: 'DEFAULT',
            },
      ),
      includeColumns: selectedDbms === 'postgresql' ? current.includeColumns : [],
      whereClause: selectedDbms === 'postgresql' ? current.whereClause : '',
    }));
  };

  const toggleIndexDraftColumn = (columnName: string) => {
    setIndexDraft((current) => {
      const exists = current.columns.some((column) => column.name === columnName);
      const columns = exists
        ? current.columns.filter((column) => column.name !== columnName)
        : [...current.columns, createIndexColumn(columnName)];

      return {
        ...current,
        columns,
        includeColumns: current.includeColumns.filter((includeColumn) => includeColumn !== columnName),
      };
    });
  };

  const updateIndexDraftColumn = (columnName: string, patch: Partial<Pick<SolveIndexColumn, 'direction' | 'nulls'>>) => {
    setIndexDraft((current) => ({
      ...current,
      columns: current.columns.map((column) =>
        column.name === columnName
          ? {
              ...column,
              ...patch,
            }
          : column,
      ),
    }));
  };

  const toggleIndexDraftIncludeColumn = (columnName: string) => {
    setIndexDraft((current) => ({
      ...current,
      includeColumns: current.includeColumns.includes(columnName)
        ? current.includeColumns.filter((includeColumn) => includeColumn !== columnName)
        : [...current.includeColumns, columnName],
    }));
  };

  const openIndexCreateModal = () => {
    setEditingIndexId(null);
    setIndexDraft(createDefaultIndexDraft(indexTableOptions, selectedDbms, indexDefinitions.length));
    setIsIndexDraftOpen(true);
  };

  const closeIndexDraftModal = () => {
    setEditingIndexId(null);
    setIndexDraft(createDefaultIndexDraft(indexTableOptions, selectedDbms));
    setIsIndexDraftOpen(false);
    if (indexDraftHistoryEntryRef.current && typeof window !== 'undefined') {
      indexDraftHistoryEntryRef.current = false;
      window.history.back();
    }
  };

  const saveIndexDefinition = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!indexDraft.tableName || indexDraft.columns.length === 0) {
      return;
    }

    setIndexDefinitions((current) => {
      const nextIndexDefinition = {
        ...indexDraft,
        id: editingIndexId ?? `index-${Date.now().toString(36)}-${current.length}`,
        name: indexDraft.name.trim() || createIndexDefaultName(current.length),
        unique: supportsUniqueIndex(selectedDbms, indexDraft.method) ? indexDraft.unique : false,
        whereClause: selectedDbms === 'postgresql' ? indexDraft.whereClause : '',
        includeColumns: selectedDbms === 'postgresql' ? indexDraft.includeColumns : [],
        source: 'builder' as const,
        operation: 'CREATE' as const,
      };

      return editingIndexId == null
        ? [...current, nextIndexDefinition]
        : current.map((indexDefinition) =>
            indexDefinition.id === editingIndexId ? nextIndexDefinition : indexDefinition,
          );
    });
    closeIndexDraftModal();
  };

  const editIndexDefinition = (indexDefinition: SolveIndexDefinition) => {
    if (indexDefinition.operation !== 'CREATE' || indexDefinition.columns.length === 0) {
      setSql((currentSql) => {
        const nextSql = [currentSql.trimEnd(), resolveIndexDefinitionSql(indexDefinition, selectedDbms)]
          .filter(Boolean)
          .join(currentSql.trimEnd() ? '\n\n' : '');
        indexAutoParseSkippedSqlRef.current = nextSql;
        return nextSql;
      });
      removeIndexDefinition(indexDefinition.id);
      window.setTimeout(() => sqlEditorRef.current?.focus(), 0);
      return;
    }

    setEditingIndexId(indexDefinition.id);
    setIndexDraft({
      name: indexDefinition.name,
      tableName: indexDefinition.tableName,
      method: indexDefinition.method,
      unique: indexDefinition.unique,
      columns: indexDefinition.columns,
      includeColumns: indexDefinition.includeColumns,
      whereClause: indexDefinition.whereClause,
    });
    setIsIndexDraftOpen(true);
  };

  const removeIndexDefinition = (indexId: string) => {
    setIndexDefinitions((current) => current.filter((indexDefinition) => indexDefinition.id !== indexId));
  };

  const startIndexColumnPickerDrag = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (event.button !== 0 || (event.target instanceof Element && event.target.closest('input, select, button'))) {
      return;
    }

    indexColumnPickerDragRef.current = {
      pointerId: event.pointerId,
      startY: event.clientY,
      startScrollTop: event.currentTarget.scrollTop,
      moved: false,
    };
    indexColumnPickerDragMovedRef.current = false;
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const moveIndexColumnPickerDrag = (event: ReactPointerEvent<HTMLDivElement>) => {
    const dragState = indexColumnPickerDragRef.current;
    if (dragState == null || dragState.pointerId !== event.pointerId) {
      return;
    }

    const deltaY = event.clientY - dragState.startY;
    if (Math.abs(deltaY) > 3) {
      dragState.moved = true;
      indexColumnPickerDragMovedRef.current = true;
      event.currentTarget.classList.add('is-dragging');
    }

    if (!dragState.moved) {
      return;
    }

    event.currentTarget.scrollTop = dragState.startScrollTop - deltaY;
    event.preventDefault();
  };

  const stopIndexColumnPickerDrag = (event: ReactPointerEvent<HTMLDivElement>) => {
    const dragState = indexColumnPickerDragRef.current;
    if (dragState == null || dragState.pointerId !== event.pointerId) {
      return;
    }

    event.currentTarget.classList.remove('is-dragging');
    indexColumnPickerDragRef.current = null;
  };

  const preventIndexColumnPickerClickAfterDrag = (event: ReactMouseEvent<HTMLDivElement>) => {
    if (!indexColumnPickerDragMovedRef.current) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    indexColumnPickerDragMovedRef.current = false;
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

  const renderPanelActions = (panelKey: PanelKey) => {
    const showCloseButton = panelKey !== 'editor' || detachedPanels[panelKey] || externalWindowPanels[panelKey];

    return panelKey === 'submit' ? null : (
      <div className="solve-pane-actions">
        <button
          type="button"
          className={`mini-toggle solve-pane-action solve-pane-action-icon is-external-window ${externalWindowPanels[panelKey] ? 'is-selected' : ''}`}
          aria-label={externalWindowPanels[panelKey] ? `Restore ${getPanelTitle(panelKey)} from external window` : `Open ${getPanelTitle(panelKey)} in external window`}
          onClick={() => togglePanelExternalWindow(panelKey)}
        >
          <ExternalWindowIcon />
        </button>
        <button
          type="button"
          className={`mini-toggle solve-pane-action solve-pane-action-icon is-pip ${detachedPanels[panelKey] ? 'is-selected' : ''}`}
          aria-label={detachedPanels[panelKey] ? `Restore ${getPanelTitle(panelKey)} from PIP` : `Open ${getPanelTitle(panelKey)} in PIP`}
          onClick={() => togglePanelDetach(panelKey)}
        >
          <PipIcon />
        </button>
        {showCloseButton ? (
          <button
            type="button"
            className="mini-toggle solve-pane-action solve-pane-action-icon"
            aria-label={text('PROBLEM_SOLVE_PANEL_CLOSE_LABEL', { label: getPanelTitle(panelKey) }, '{label} 닫기')}
            onClick={() => togglePanelVisibility(panelKey)}
          >
            <CloseIcon />
          </button>
        ) : null}
      </div>
    );
  };

  const renderFloatingOpacityControl = () => {
    const sliderRange = FLOATING_EDITOR_BACKGROUND_MAX_ALPHA - FLOATING_EDITOR_BACKGROUND_MIN_ALPHA;
    const sliderValue = Math.round(((editorFloatingOpacity - FLOATING_EDITOR_BACKGROUND_MIN_ALPHA) / sliderRange) * 100);

    return (
      <label className="solve-floating-opacity-control" aria-label={text('PROBLEM_SOLVE_EDITOR_OPACITY_LABEL', '에디터 투명도 조절')}>
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
        aria-label={collapsedCards[cardKey] ? text('COMMON_EXPAND_ACTION', '펼치기') : text('COMMON_COLLAPSE_ACTION', '접기')}
        aria-expanded={!collapsedCards[cardKey]}
        onClick={() => toggleCardCollapse(cardKey)}
      >
        <CollapseChevronIcon collapsed={collapsedCards[cardKey]} />
      </button>
      {!collapsedCards[cardKey] ? <span className="solve-detail-section-rail-line" aria-hidden="true" /> : null}
    </div>
  );

  const visibleExecutionRuns = executionRuns.filter((run) => run.status !== 'idle');

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

  const renderIndexSettingsPanel = (isFloating: boolean) => {
    const hasTableOptions = indexTableOptions.length > 0;
    const availableIncludeColumns = draftAvailableColumns.filter(
      (columnName) => !indexDraft.columns.some((column) => column.name === columnName),
    );

    return (
      <div className={`solve-index-settings ${isFloating ? 'is-floating' : ''}`.trim()}>
        <div className="solve-index-list">
          {visibleIndexDefinitions.length > 0 ? (
            visibleIndexDefinitions.map((indexDefinition) => (
              <article key={indexDefinition.id} className="solve-index-card">
                <div className="solve-index-card-main">
                  <div className="solve-index-card-title-row">
                    <strong>{indexDefinition.name}</strong>
                    <span>{indexDefinition.operation === 'CREATE' ? indexDefinition.method : indexDefinition.operation}</span>
                    {indexDefinition.unique ? <span>{text('PROBLEM_SOLVE_INDEX_UNIQUE_BADGE', 'UNIQUE')}</span> : null}
                  </div>
                  <p>
                    {indexDefinition.operation === 'CREATE'
                      ? `${indexDefinition.tableName} · ${indexDefinition.columns.map((column) => supportsIndexColumnOrdering(indexDefinition.method) ? `${column.name} ${column.direction}` : column.name).join(', ')}`
                      : `${indexDefinition.operation === 'ALTER' ? 'INDEX 변경' : 'INDEX 제거'}${indexDefinition.tableName ? ` · ${indexDefinition.tableName}` : ''}`}
                  </p>
                  <code>{resolveIndexDefinitionSql(indexDefinition, selectedDbms)}</code>
                </div>
                <div className="solve-index-card-actions">
                  <button type="button" aria-label={text('COMMON_EDIT_ACTION', '수정')} onClick={() => editIndexDefinition(indexDefinition)}>
                    <PencilIcon />
                  </button>
                  <button type="button" aria-label={text('COMMON_DELETE_ACTION', '삭제')} onClick={() => removeIndexDefinition(indexDefinition.id)}>
                    <TrashIcon />
                  </button>
                </div>
              </article>
            ))
          ) : (
            <div className="solve-index-empty">
              {hasTableOptions
                ? text('PROBLEM_SOLVE_INDEX_EMPTY_STATE', '설정된 인덱스가 없습니다.')
                : text('PROBLEM_SOLVE_INDEX_DDL_EMPTY_STATE', 'DDL을 불러오면 인덱스 설정을 구성할 수 있습니다.')}
            </div>
          )}
        </div>

        {isIndexDraftOpen && hasTableOptions && typeof document !== 'undefined'
          ? createPortal(
              <div className="submit-history-modal-overlay solve-index-modal-overlay" role="presentation">
                <form className="submit-history-modal solve-index-modal" role="dialog" aria-modal="true" aria-label={text('PROBLEM_SOLVE_INDEX_MODAL_LABEL', '인덱스 편집')} onSubmit={saveIndexDefinition}>
                  <div className="submit-history-modal-header solve-index-modal-header">
                    <div className="submit-history-modal-copy">
                      <strong>{editingIndexId == null ? text('PROBLEM_SOLVE_INDEX_ADD_BUTTON', '인덱스 추가') : text('PROBLEM_SOLVE_INDEX_EDIT_BUTTON', '인덱스 수정')}</strong>
                    </div>
                    <button type="button" className="submit-history-modal-close" onClick={closeIndexDraftModal}>
                      {text('COMMON_CLOSE_BUTTON', '닫기')}
                    </button>
                  </div>

                  <div className="solve-index-draft">
                    <div className="solve-index-draft-grid">
                      <label className="solve-index-field is-wide">
                        <span>{text('PROBLEM_SOLVE_INDEX_NAME_LABEL', '인덱스명')}</span>
                        <input
                          value={indexDraft.name}
                          onChange={(event) => setIndexDraft((current) => ({ ...current, name: event.target.value }))}
                          placeholder="idx_table_column"
                          name="indexName"
                          autoComplete="off"
                        />
                      </label>
                      <label className="solve-index-field">
                        <span>{text('PROBLEM_SOLVE_INDEX_TABLE_LABEL', '테이블')}</span>
                        <select name="indexTable" value={indexDraft.tableName} onChange={(event) => handleIndexDraftTableChange(event.target.value)}>
                          {indexTableOptions.map((tableOption) => (
                            <option key={tableOption.name} value={tableOption.name}>{tableOption.name}</option>
                          ))}
                        </select>
                      </label>
                    </div>

                    <div className="solve-index-method-row">
                      <label className="solve-index-field">
                        <span>{text('PROBLEM_SOLVE_INDEX_METHOD_LABEL', '방식')}</span>
                        <select
                          name="indexMethod"
                          value={indexDraft.method}
                          onChange={(event) => handleIndexDraftMethodChange(event.target.value as SolveIndexMethod)}
                        >
                          {indexMethodOptions.map((method) => (
                            <option key={method} value={method}>{method}</option>
                          ))}
                        </select>
                      </label>
                      <label className="solve-index-toggle-field">
                        <input
                          type="checkbox"
                          checked={indexDraft.unique}
                          disabled={!supportsUniqueIndex(selectedDbms, indexDraft.method)}
                          onChange={(event) => setIndexDraft((current) => ({ ...current, unique: event.target.checked }))}
                        />
                        <span>{text('PROBLEM_SOLVE_INDEX_UNIQUE_LABEL', 'UNIQUE')}</span>
                      </label>
                    </div>

                    <div className="solve-index-option-block">
                      <span>{text('PROBLEM_SOLVE_INDEX_COLUMNS_LABEL', '컬럼')}</span>
                      <div
                        ref={indexColumnPickerRef}
                        className={`solve-index-column-picker ${selectedDbms === 'postgresql' ? 'is-postgresql' : 'is-mysql'} ${supportsIndexColumnOrdering(indexDraft.method) ? 'has-column-ordering' : 'has-no-column-ordering'}`.trim()}
                        onPointerDown={startIndexColumnPickerDrag}
                        onPointerMove={moveIndexColumnPickerDrag}
                        onPointerUp={stopIndexColumnPickerDrag}
                        onPointerCancel={stopIndexColumnPickerDrag}
                        onClickCapture={preventIndexColumnPickerClickAfterDrag}
                      >
                        {draftAvailableColumns.map((columnName) => {
                          const selectedColumn = indexDraft.columns.find((column) => column.name === columnName);

                          return (
                            <div key={columnName} className={`solve-index-column-option ${selectedColumn ? 'is-selected' : ''}`.trim()}>
                              <label className="solve-index-column-name-cell">
                                <input type="checkbox" checked={selectedColumn != null} onChange={() => toggleIndexDraftColumn(columnName)} />
                                <span>{columnName}</span>
                              </label>
                              {supportsIndexColumnOrdering(indexDraft.method) ? (
                                <select
                                  className="solve-index-column-select"
                                  aria-label={`${columnName} 정렬`}
                                  value={selectedColumn?.direction ?? 'ASC'}
                                  disabled={selectedColumn == null}
                                  onChange={(event) =>
                                    updateIndexDraftColumn(columnName, { direction: event.target.value as SolveIndexSortDirection })
                                  }
                                >
                                  <option value="ASC">ASC</option>
                                  <option value="DESC">DESC</option>
                                </select>
                              ) : null}
                              {supportsIndexColumnOrdering(indexDraft.method) && selectedDbms === 'postgresql' ? (
                                <select
                                  className="solve-index-column-select"
                                  aria-label={`${columnName} NULL 정렬`}
                                  value={selectedColumn?.nulls ?? 'DEFAULT'}
                                  disabled={selectedColumn == null}
                                  onChange={(event) =>
                                    updateIndexDraftColumn(columnName, { nulls: event.target.value as SolveIndexNullsPosition })
                                  }
                                >
                                  <option value="DEFAULT">기본값</option>
                                  <option value="FIRST">FIRST</option>
                                  <option value="LAST">LAST</option>
                                </select>
                              ) : null}
                            </div>
                          );
                        })}
                      </div>
                    </div>

                    {selectedDbms === 'postgresql' ? (
                      <div className="solve-index-draft-grid">
                        <div className="solve-index-field is-wide">
                          <span>{text('PROBLEM_SOLVE_INDEX_INCLUDE_LABEL', 'INCLUDE')}</span>
                          <div className="solve-index-chip-picker">
                            {availableIncludeColumns.length > 0 ? (
                              availableIncludeColumns.map((columnName) => (
                                <button
                                  key={columnName}
                                  type="button"
                                  className={indexDraft.includeColumns.includes(columnName) ? 'is-selected' : ''}
                                  onClick={() => toggleIndexDraftIncludeColumn(columnName)}
                                >
                                  {columnName}
                                </button>
                              ))
                            ) : (
                              <span>{text('PROBLEM_SOLVE_INDEX_INCLUDE_EMPTY', '선택 가능한 INCLUDE 컬럼 없음')}</span>
                            )}
                          </div>
                        </div>
                        <label className="solve-index-field is-wide">
                          <span>{text('PROBLEM_SOLVE_INDEX_WHERE_LABEL', 'WHERE')}</span>
                          <input
                            value={indexDraft.whereClause}
                            onChange={(event) => setIndexDraft((current) => ({ ...current, whereClause: event.target.value }))}
                            placeholder={text('PROBLEM_SOLVE_INDEX_WHERE_PLACEHOLDER', '예: is_active = true')}
                            name="indexWhere"
                            autoComplete="off"
                          />
                        </label>
                      </div>
                    ) : null}

                    <div className="solve-index-preview-row">
                      <code>{indexDraftSql || text('PROBLEM_SOLVE_INDEX_PREVIEW_EMPTY', '테이블과 컬럼을 선택하면 CREATE INDEX 미리보기가 표시됩니다.')}</code>
                      <div className="solve-index-draft-actions">
                        <button type="button" className="solve-index-action-button" onClick={closeIndexDraftModal}>
                          {text('COMMON_CANCEL_ACTION', '취소')}
                        </button>
                        <button type="submit" className="solve-index-action-button is-primary" disabled={!indexDraftSql}>
                          {editingIndexId == null ? text('COMMON_ADD_ACTION', '추가') : text('COMMON_SAVE_ACTION', '저장')}
                        </button>
                      </div>
                    </div>
                  </div>
                </form>
              </div>,
              document.body,
            )
          : null}
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
                <div className="solve-editor-zoom-controls" aria-label={text('PROBLEM_SOLVE_EDITOR_FONT_SIZE_LABEL', '에디터 글씨 크기 조절')}>
                  <button type="button" className="mini-toggle solve-editor-zoom-button is-increase" onClick={() => changeSqlEditorFontSize(1)}>
                    +
                  </button>
                  <button type="button" className="mini-toggle solve-editor-zoom-button is-decrease" onClick={() => changeSqlEditorFontSize(-1)}>
                    -
                  </button>
                </div>
                <button
                  type="button"
                  className={`btn secondary solve-editor-toolbar-index-button ${hasParsingIndexStatement ? 'is-parsing-attention' : ''}`.trim()}
                  onClick={openIndexCreateModal}
                  disabled={indexTableOptions.length === 0}
                >
                  <PlusGlyphIcon />
                  <span>{text('PROBLEM_SOLVE_INDEX_ADD_BUTTON', '인덱스 추가')}</span>
                </button>
                <div className="solve-editor-actions">
                  <button
                    type="button"
                    className="btn secondary solve-editor-action-button is-stop"
                    onClick={handleStopExecution}
                    disabled={!isExecuting}
                  >
                    {text('PROBLEM_SOLVE_STOP_BUTTON', '중지')}
                  </button>
                  <button type="button" className="btn secondary solve-editor-action-button" onClick={executeSql} disabled={sql.trim().length === 0 || isExecuting}>
                    {isExecuting ? <span className="solve-editor-statement-spinner" aria-hidden="true" /> : null}
                    <span className="solve-action-copy">
                      <span className="solve-action-text">
                        {isExecuting ? text('PROBLEM_SOLVE_EXECUTING_LABEL', '실행 중') : text('PROBLEM_SOLVE_EXECUTE_BUTTON', '실행')}
                      </span>
                      {!isExecuting ? <span className="solve-action-text is-shortcut">Ctrl + Enter</span> : null}
                    </span>
                  </button>
                  <button type="button" className="btn primary solve-editor-action-button" onClick={handleSubmit} disabled={sql.trim().length === 0 || isSubmitting}>
                    {isSubmitting ? <span className="solve-editor-statement-spinner" aria-hidden="true" /> : null}
                    <span className="solve-action-copy">
                      <span className="solve-action-text">
                        {isSubmitting ? text('PROBLEM_SOLVE_SUBMITTING_LABEL', '제출 중') : text('PROBLEM_SOLVE_SUBMIT_BUTTON', '제출')}
                      </span>
                      {!isSubmitting ? <span className="solve-action-text is-shortcut">Ctrl + Shift + Enter</span> : null}
                    </span>
                  </button>
                </div>
              </div>
              {renderIndexSettingsPanel(isFloating)}
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
                            aria-label={text('PROBLEM_SOLVE_MOVE_RESULT_BUTTON', '실행 결과 위치로 이동')}
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
                        <span className="solve-sql-highlight-placeholder">{text('PROBLEM_SOLVE_EDITOR_PLACEHOLDER', '이곳에 SQL을 작성하세요.')}</span>
                      ) : (
                        highlightedSql
                      )}
                    </pre>
                    <textarea
                      ref={handleSqlEditorRef}
                      className={`solve-sql-editor ${sql.length === 0 ? 'is-empty' : 'has-content'}`}
                      spellCheck={false}
                      wrap="soft"
                      placeholder={text('PROBLEM_SOLVE_EDITOR_PLACEHOLDER', '이곳에 SQL을 작성하세요.')}
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

	                        if (autocompleteState != null && ['ArrowUp', 'ArrowDown'].includes(event.key)) {
	                          return;
	                        }

	                        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End', 'PageUp', 'PageDown'].includes(event.key)) {
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
                      aria-label={text('PROBLEM_SOLVE_PANEL_EDITOR_LABEL', '에디터')}
                    />
                  </div>

                  {autocompleteState
                    ? createPortal(
	                        <div
	                          ref={autocompleteListRef}
	                          className="solve-editor-autocomplete"
                          style={{
                            left: `${autocompleteState.left}px`,
                            top: `${autocompleteState.top}px`,
                            maxWidth: `${autocompleteState.maxWidth}px`,
                            maxHeight: `${autocompleteState.maxHeight}px`,
                          }}
                          role="listbox"
                          aria-label={text('PROBLEM_SOLVE_AUTOCOMPLETE_TITLE', 'SQL 자동완성')}
                        >
                          {autocompleteState.items.map((item, index) => (
                            <button
	                              key={`${item.kind}-${item.value}-${item.detail ?? ''}`}
	                              type="button"
	                              data-autocomplete-index={index}
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
                          aria-label={
                            executionPickerState.mode === 'submit'
                              ? text('PROBLEM_SOLVE_SUBMIT_PICKER_LABEL', 'SQL 제출 구문 선택')
                              : text('PROBLEM_SOLVE_EXECUTION_PICKER_LABEL', 'SQL 실행 구문 선택')
                          }
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
                  <div className="solve-submit-progress-list" aria-live="polite">
                    {submitProgressSteps.map((step) => (
                      <SubmitProgressItem key={step.stepKey} step={step} />
                    ))}
                  </div>
                ) : null}
                {submitMessage && submitProgressSteps.length === 0 ? (
                  <div className="solve-pane-result-stack">
                    <p className={`solve-pane-result-message ${submitMessage === text('SUBMIT_HISTORY_RESULT_WRONG_LABEL', '오답') ? 'is-error' : ''}`.trim()}>{submitMessage}</p>
                  </div>
                ) : null}
              </div>
            ) : (
              <div className="solve-result-empty">{text('PROBLEM_SOLVE_SUBMIT_RESULT_EMPTY_STATE', '제출 결과 없음.')}</div>
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
              <div className="submit-history-modal" role="dialog" aria-modal="true" aria-label={text('SUBMIT_HISTORY_SQL_MODAL_LABEL', '제출 SQL 보기')}>
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <div className="submit-history-modal-title-row">
                      <strong>{text('SUBMIT_HISTORY_RESULT_TITLE', '제출 결과')}</strong>
                      <span className={`submit-history-modal-title-status ${relatedModalState.history.success ? 'is-success' : 'is-fail'}`}>
                        {relatedModalState.history.success
                          ? text('SUBMIT_HISTORY_RESULT_CORRECT_LABEL', '정답')
                          : text('SUBMIT_HISTORY_RESULT_WRONG_LABEL', '오답')}
                      </span>
                      <button
                        type="button"
                        className="submit-history-modal-plan-action"
                        aria-label={text('SUBMIT_HISTORY_PLAN_DETAIL_BUTTON_LABEL', '실행계획 요소 자세히 보기')}
                        title={text('SUBMIT_HISTORY_PLAN_DETAIL_BUTTON_LABEL', '실행계획 요소 자세히 보기')}
                        disabled={!hasSolveRelatedExecutionPlanDetails(relatedModalState.history)}
                        onClick={() => setRelatedModalState({ type: 'plan', history: relatedModalState.history })}
                      >
                        ↗
                      </button>
                    </div>
                  </div>
                  <button type="button" className="submit-history-modal-close" onClick={() => setRelatedModalState(null)}>
                    {text('COMMON_CLOSE_BUTTON', '닫기')}
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-sql-modal-body">
                  <pre className="submit-history-sql-viewer submit-history-sql-highlight" aria-label={text('SUBMIT_HISTORY_SQL_VIEWER_LABEL', '제출 SQL')}>
                    {renderStaticHighlightedSql(relatedModalState.history.submittedSql)}
                  </pre>
                </div>
              </div>
            ) : (
              <div className="submit-history-modal submit-history-plan-modal" role="dialog" aria-modal="true" aria-label={text('SUBMIT_HISTORY_PLAN_MODAL_LABEL', '실행 계획 요소 보기')}>
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <strong>{text('SUBMIT_HISTORY_PLAN_TITLE', '실행 계획 요소')}</strong>
                  </div>
                  <button type="button" className="submit-history-modal-close" onClick={() => setRelatedModalState(null)}>
                    {text('COMMON_CLOSE_BUTTON', '닫기')}
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-plan-modal-body">
                  <ExecutionPlanDetailBoard
                    sections={buildSolveRelatedPlanSections(relatedModalState.history)}
                    noneLabel={text('COMMON_NONE_LABEL', '없음')}
                    className="submit-history-plan-detail-board"
                  />
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

    if (!isAuthenticated || !handle) {
      return <div className="solve-related-empty-state">{text('PROBLEM_SOLVE_RELATED_SUBMIT_EMPTY_STATE', '로그인 후 내 제출을 확인할 수 있습니다.')}</div>;
    }

    if (mySubmitLoadError) {
      return mySubmitLoadErrorStatus != null
        ? <HttpErrorState status={mySubmitLoadErrorStatus} className="solve-related-empty-state" message={mySubmitLoadError} />
        : <PageLoadFailureState className="solve-related-empty-state" message={mySubmitLoadError} />;
    }

    return (
      <div className="solve-related-tab-panel">
        <div className={`submit-history-table-shell solve-related-table-shell ${isMySubmitLoading ? 'is-loading' : ''}`.trim()}>
          <div className="submit-history-table solve-related-submit-table" role="table" aria-label={text('PROBLEM_SOLVE_RELATED_SUBMIT_TABLE_LABEL', '내 제출 목록')}>
            <div className="submit-history-row submit-history-head solve-related-table-head" role="row">
              <div role="columnheader" className="submit-history-head-cell">{text('SUBMIT_HISTORY_SUBMIT_ID_COLUMN_LABEL', '제출번호')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('SUBMIT_HISTORY_RESULT_TITLE', '제출 결과')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('COMMON_COST_LABEL', 'Cost')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('SUBMIT_HISTORY_SUBMITTED_AT_COLUMN_LABEL', '제출 시각')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('SUBMIT_HISTORY_PLAN_COLUMN_LABEL', '실행계획요소')}</div>
            </div>

            {isMySubmitLoading && mySubmitHistoryPage.histories.length === 0 ? (
              solveRelatedLoadingRows.map((rowIndex) => (
                <div key={`solve-related-submit-loading-${rowIndex}`} className="submit-history-row submit-history-body solve-related-table-row" role="row" aria-hidden="true">
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-mini" /></span>
                </div>
              ))
            ) : mySubmitHistoryPage.histories.length === 0 ? (
              <div className="submit-history-row submit-history-empty-row" role="row">
                <span className="submit-history-empty-cell" role="cell">{text('PROBLEM_SOLVE_RELATED_PROBLEM_EMPTY_STATE', '이 문제에 대한 내 제출이 없습니다.')}</span>
              </div>
            ) : (
              mySubmitHistoryPage.histories.map((history) => (
                <article key={history.submitId} className="submit-history-row submit-history-body solve-related-table-row" role="row">
                  <span className="submit-history-cell" role="cell" data-label={text('SUBMIT_HISTORY_SUBMIT_ID_COLUMN_LABEL', '제출번호')}>{history.submitId}</span>
                  <span className="submit-history-cell" role="cell" data-label={text('SUBMIT_HISTORY_RESULT_TITLE', '제출 결과')}>
                    <button
                      type="button"
                      className={`submit-history-status-text ${history.success ? 'is-success' : 'is-fail'}`}
                      onClick={() => setRelatedModalState({ type: 'sql', history })}
                    >
                      {history.success ? text('SUBMIT_HISTORY_RESULT_CORRECT_LABEL', '정답') : text('SUBMIT_HISTORY_RESULT_WRONG_LABEL', '오답')}
                    </button>
                  </span>
                  <span className="submit-history-cell" role="cell" data-label={text('COMMON_COST_LABEL', 'Cost')}>
                    {history.success ? formatCost(history.cost) : '-'}
                  </span>
                  <span className="submit-history-cell" role="cell" data-label={text('SUBMIT_HISTORY_SUBMITTED_AT_COLUMN_LABEL', '제출 시각')}>
                    {formatSubmittedAt(history.submittedAt)}
                  </span>
                  <span className="submit-history-cell submit-history-cell-plan" role="cell" data-label={text('SUBMIT_HISTORY_PLAN_COLUMN_LABEL', '실행계획요소')}>
                    {hasSolveRelatedExecutionPlanDetails(history) ? (
                      <button
                        type="button"
                        className="submit-history-detail-button"
                        aria-label={text(
                          'SUBMIT_HISTORY_PLAN_DETAIL_MOVE_LABEL',
                          { label: getPlanElementButtonLabel(history.dbms, history.executionPlanElement) },
                          `${getPlanElementButtonLabel(history.dbms, history.executionPlanElement)} 자세히 보기`,
                        )}
                        title={getPlanElementButtonLabel(history.dbms, history.executionPlanElement)}
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

          {isMySubmitLoading ? <LoadingOverlay /> : null}
        </div>

        {mySubmitHistoryPage.totalCount > 0 ? (
          <Pagination
            currentPage={mySubmitHistoryPage.currentPage}
            totalPages={mySubmitHistoryPage.totalPages}
            onPageChange={setMySubmitRequestedPage}
            ariaLabel={text('PROBLEM_SOLVE_RELATED_SUBMIT_PAGE_LABEL', '내 제출 페이지')}
            inputLabel={text('PROBLEM_SOLVE_RELATED_SUBMIT_PAGE_INPUT_LABEL', '이동할 내 제출 페이지 입력')}
            inputOpenLabel={text('PROBLEM_SOLVE_RELATED_SUBMIT_PAGE_INPUT_OPEN_LABEL', '이동할 내 제출 페이지 입력 열기')}
            previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
            nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
            className="solve-related-pagination"
            pageButtonClassName="solve-related-page-button"
            metaClassName="solve-related-pagination-meta"
            metaButtonClassName=""
            inputClassName="solve-related-pagination-input"
          />
        ) : null}
      </div>
    );
  };

  const renderTaggedPostTabPanel = () => {
    if (taggedPostLoadError) {
      return taggedPostLoadErrorStatus != null
        ? <HttpErrorState status={taggedPostLoadErrorStatus} className="solve-related-empty-state" message={taggedPostLoadError} />
        : <PageLoadFailureState className="solve-related-empty-state" message={taggedPostLoadError} />;
    }

    return (
      <div className="solve-related-tab-panel">
        <div className={`submit-history-table-shell solve-related-table-shell solve-related-community-table-shell ${isTaggedPostLoading ? 'is-loading' : ''}`.trim()}>
          <div className="submit-history-table solve-related-community-table" role="table" aria-label={text('PROBLEM_SOLVE_RELATED_TAGGED_POST_TABLE_LABEL', '태그된 게시글 목록')}>
            <div className="submit-history-row submit-history-head solve-related-table-head" role="row">
              <div role="columnheader" className="submit-history-head-cell">{text('PROBLEM_SOLVE_COMMUNITY_CATEGORY_COLUMN_LABEL', '구분')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('COMMUNITY_TITLE_COLUMN_LABEL', '제목')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('COMMUNITY_DATE_COLUMN_LABEL', '작성일')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요')}</div>
              <div role="columnheader" className="submit-history-head-cell">{text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글')}</div>
            </div>

            {isTaggedPostLoading && taggedPostPage.posts.length === 0 ? (
              solveRelatedLoadingRows.map((rowIndex) => (
                <div key={`solve-related-community-loading-${rowIndex}`} className="submit-history-row submit-history-body solve-related-table-row" role="row" aria-hidden="true">
                  <span className="submit-history-cell solve-related-community-category" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <div className="submit-history-cell solve-related-community-title-cell" role="cell">
                    <span className="wave-loading-placeholder is-long" />
                    <span className="wave-loading-placeholder is-medium" />
                  </div>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                  <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                </div>
              ))
            ) : taggedPostPage.posts.length === 0 ? (
              <div className="submit-history-row submit-history-empty-row" role="row">
                <span className="submit-history-empty-cell" role="cell">{text('PROBLEM_SOLVE_RELATED_COMMUNITY_EMPTY_STATE', '이 문제 번호로 태그된 게시글이 없습니다.')}</span>
              </div>
            ) : (
              taggedPostPage.posts.map((post) => (
                <article
                  key={post.id}
                  className="submit-history-row submit-history-body solve-related-table-row"
                  role="row"
                  tabIndex={0}
                  aria-label={text('COMMUNITY_POST_OPEN_LABEL', { title: post.title }, '{title} 상세보기')}
                  onClick={() => navigate(getCommunityPostPath(post.id))}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      navigate(getCommunityPostPath(post.id));
                    }
                  }}
                >
                  <span className="submit-history-cell solve-related-community-category" role="cell" data-label={text('PROBLEM_SOLVE_COMMUNITY_CATEGORY_COLUMN_LABEL', '구분')}>
                    <span className={`solve-related-community-category-text is-${post.category}`}>{getSolveRelatedCommunityCategoryLabel(post.category)}</span>
                  </span>
                  <div role="cell" className="submit-history-cell solve-related-community-title-cell" data-label={text('COMMUNITY_TITLE_COLUMN_LABEL', '제목')}>
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
                      onClick={(event) => {
                        event.stopPropagation();
                        navigate(getCommunityPostPath(post.id));
                      }}
                    >
                      <span className="solve-related-community-title-text">{post.title}</span>
                    </button>
                  </div>
                  <span className="submit-history-cell solve-related-community-handle-cell" role="cell" data-label={text('COMMON_HANDLE_LABEL', 'Handle')}>
                    <button
                      type="button"
                      className="submit-history-link-button"
                      onClick={(event) => {
                        event.stopPropagation();
                        navigate(getProfilePath(post.authorHandle));
                      }}
                      aria-label={text('SUBMIT_HISTORY_HANDLE_PROFILE_MOVE_LABEL', { handle: post.authorHandle }, '{handle} 프로필 이동')}
                    >
                      {post.authorHandle}
                    </button>
                  </span>
                  <span className="submit-history-cell solve-related-community-date-cell" role="cell" data-label={text('COMMUNITY_DATE_COLUMN_LABEL', '작성일')}>{formatBoardDate(post.createdAt)}</span>
                  <span className="submit-history-cell solve-related-community-metric-cell" role="cell" data-label={text('COMMUNITY_VIEWS_COLUMN_LABEL', '조회수')}>{formatGroupedNumber(post.views)}</span>
                  <span className="submit-history-cell solve-related-community-metric-cell" role="cell" data-label={text('COMMUNITY_LIKES_COLUMN_LABEL', '좋아요')}>{formatGroupedNumber(post.likes)}</span>
                  <span className="submit-history-cell solve-related-community-metric-cell" role="cell" data-label={text('COMMUNITY_COMMENTS_COLUMN_LABEL', '댓글')}>{formatGroupedNumber(post.comments)}</span>
                  <div className="submit-history-cell solve-related-community-mobile-meta-cell" role="cell" data-label={text('PROBLEM_SOLVE_COMMUNITY_MOBILE_META_LABEL', '게시글 정보')}>
                    <span className="solve-related-community-mobile-author">{post.authorHandle}</span>
                    <span className="solve-related-community-mobile-date">{formatBoardDate(post.createdAt)}</span>
                    <span className="solve-related-community-mobile-metric" aria-label={text('COMMUNITY_VIEWS_COUNT_LABEL', { count: formatGroupedNumber(post.views) }, '조회수 {count}개')}>
                      <RelatedViewIcon />
                      {formatGroupedNumber(post.views)}
                    </span>
                    <span className="solve-related-community-mobile-metric" aria-label={text('COMMUNITY_LIKES_COUNT_LABEL', { count: formatGroupedNumber(post.likes) }, '좋아요 {count}개')}>
                      <RelatedLikeIcon />
                      {formatGroupedNumber(post.likes)}
                    </span>
                    <span className="solve-related-community-mobile-metric" aria-label={text('COMMUNITY_COMMENTS_COUNT_LABEL', { count: formatGroupedNumber(post.comments) }, '댓글 {count}개')}>
                      <RelatedCommentIcon />
                      {formatGroupedNumber(post.comments)}
                    </span>
                  </div>
                </article>
              ))
            )}
          </div>

          {isTaggedPostLoading ? <LoadingOverlay /> : null}
        </div>

        {taggedPostPage.totalCount > 0 ? (
          <Pagination
            currentPage={taggedPostPage.currentPage}
            totalPages={taggedPostPage.totalPages}
            onPageChange={setTaggedPostRequestedPage}
            ariaLabel={text('PROBLEM_SOLVE_RELATED_TAGGED_POST_PAGE_LABEL', '태그된 게시글 페이지')}
            inputLabel={text('PROBLEM_SOLVE_RELATED_TAGGED_POST_PAGE_INPUT_LABEL', '이동할 태그된 게시글 페이지 입력')}
            inputOpenLabel={text('PROBLEM_SOLVE_RELATED_TAGGED_POST_PAGE_INPUT_OPEN_LABEL', '이동할 태그된 게시글 페이지 입력 열기')}
            previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
            nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
            className="solve-related-pagination"
            pageButtonClassName="solve-related-page-button"
            metaClassName="solve-related-pagination-meta"
            metaButtonClassName=""
            inputClassName="solve-related-pagination-input"
          />
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
      />
    ) : problemLoadError ? (
      problemLoadErrorStatus != null
        ? <HttpErrorState status={problemLoadErrorStatus} className="solve-related-empty-state" message={problemLoadError} />
        : <PageLoadFailureState className="solve-related-empty-state" message={problemLoadError} />
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
  const inlineDetailPanels = [inlineEditorPanel, inlineSubmitPanel].filter((panel) => panel != null);
  const problemAfterSections = contentTab === 'problem' && problemDetail
    ? inlineDetailPanels.map((panel, index) => (
        <section key={`solve-problem-after-section-${index}`} className="solve-detail-section solve-detail-section-after-content">
          {panel}
        </section>
      ))
    : null;

  if (!problemLoadError && problemDetail == null) {
    return (
      <div className="page-stack solve-page-loading-state">
        <section className="page-loading-shell solve-page-loading-shell" aria-label={text('COMMON_LOADING_STATUS', '로딩 중')} aria-busy="true">
          <div className="solve-page-loading-card" aria-hidden="true">
            <div className="solve-page-loading-tabs">
              <span className="wave-loading-placeholder is-medium" />
              <span className="wave-loading-placeholder is-medium" />
              <span className="wave-loading-placeholder is-medium" />
            </div>

            <div className="solve-page-loading-panel">
              <span className="wave-loading-placeholder is-long" />
              <span className="wave-loading-placeholder is-medium" />
              <span className="wave-loading-placeholder is-long" />
              <span className="wave-loading-placeholder is-long" />
            </div>

            <div className="solve-page-loading-panel is-editor">
              <span className="wave-loading-placeholder is-long" />
              <span className="wave-loading-placeholder is-long" />
              <span className="wave-loading-placeholder is-medium" />
              <span className="wave-loading-placeholder is-long" />
              <span className="wave-loading-placeholder is-short" />
            </div>
          </div>
        </section>
      </div>
    );
  }

  if (problemLoadError && problemDetail == null) {
    return (
      <div className="page-stack solve-page">
        <section className="panel-card solve-detail-section">
          {problemLoadErrorStatus != null
            ? <HttpErrorState status={problemLoadErrorStatus} className="solve-related-empty-state" message={problemLoadError} />
            : <PageLoadFailureState className="solve-related-empty-state" message={problemLoadError} />}
        </section>
      </div>
    );
  }

  return (
    <div className="page-stack solve-page">
      <div className="solve-page-topbar solve-page-topbar-content-tabs">
        <div className="solve-dbms-tab-row solve-content-tab-row" role="tablist" aria-label={text('PROBLEM_SOLVE_TABLIST_LABEL', '문제 상세 화면 탭 선택')}>
          <button
            type="button"
            role="tab"
            aria-selected={contentTab === 'problem'}
            className={`solve-dbms-tab ${contentTab === 'problem' ? 'is-selected' : ''}`}
            onClick={() => setContentTab('problem')}
          >
            {text('PROBLEM_SOLVE_TAB_SUBMIT_LABEL', '제출')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={contentTab === 'submissions'}
            className={`solve-dbms-tab ${contentTab === 'submissions' ? 'is-selected' : ''}`}
            onClick={() => setContentTab('submissions')}
          >
            {text('PROBLEM_SOLVE_TAB_MY_SUBMISSIONS_LABEL', '내 제출 목록')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={contentTab === 'community'}
            className={`solve-dbms-tab ${contentTab === 'community' ? 'is-selected' : ''}`}
            onClick={() => setContentTab('community')}
          >
            {text('PROBLEM_SOLVE_TAB_TAGGED_POSTS_LABEL', '태그된 게시글')}
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
            <span className="solve-problem-number">{text('PROBLEM_SOLVE_PROBLEM_NUMBER_LABEL', { problemId: displayProblemNumber }, '문제 {problemId}')}</span>
            <h1 className="solve-problem-title">{displayProblemTitle}</h1>
          </div>

        </div>
        {renderActiveContentTab()}
      </section>

      {problemAfterSections}

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
