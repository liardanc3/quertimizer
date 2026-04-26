import { createPortal } from 'react-dom';
import { Fragment, useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import {
  HINT_FILTER_OPTIONS,
  buildAvailableBucketFilters,
  createEmptySubmitHistoryPlanFilters,
  FILTER_MODE_OPTIONS,
  formatBucketDisplayLabel,
  formatHintFilterLabel,
  getExecutionPlanDetailGroups,
  getPlanElementButtonLabel,
  normalizePlanFilters,
  toPlanFilterFieldKey,
  type BucketFilterValue,
  type PlanSectionKey,
} from '../lib/executionPlanFilters';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import { fetchSubmitHistories } from '../lib/submitHistoryApi';
import { getLocationSearchSnapshot, getProfilePath, PROBLEMS_PATH, SUBMIT_HISTORY_PATH, subscribeLocation, navigate } from '../lib/navigation';
import type {
  DbmsType,
  SubmitHistoryEntry,
  SubmitHistoryJudge,
  SubmitHistoryPageData,
  SubmitHistoryPlanFilters,
} from '../types/domain';
import '../components/home/ProblemRuntimeChart.css';
import './SubmitHistoryPage.css';

function SortAscendingIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.5v10.9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="M5.2 5.25 8 2.5l2.8 2.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SortDescendingIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M8 2.6v10.9" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <path d="m5.2 10.75 2.8 2.75 2.8-2.75" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SortNeutralIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M5.7 6.2 8 3.9l2.3 2.3" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="m5.7 9.8 2.3 2.3 2.3-2.3" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

type DbmsFilterValue = DbmsType | 'all';
type SubmitHistoryModalState =
  | { type: 'sql'; history: SubmitHistoryEntry }
  | { type: 'plan'; history: SubmitHistoryEntry }
  | null;

type HeaderFilterMenuState = {
  key: SubmitHistoryHeaderFilterKey;
  left: number;
  top: number;
  width: number;
} | null;

type SubmitHistorySqlTokenKind =
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

type JudgeSelectionValue = Exclude<SubmitHistoryJudge, 'all'>;
type SubmitHistoryCostSortOrder = 'none' | 'desc' | 'asc';
type SubmitHistoryHeaderFilterKey = 'judge' | 'plan';

interface SubmitHistorySqlHighlightToken {
  text: string;
  kind: SubmitHistorySqlTokenKind | null;
}

type SubmitHistoryPlanFiltersByDbms = Record<DbmsType, SubmitHistoryPlanFilters>;

interface SubmitHistoryFilters {
  submitId: string;
  query: string;
  dbmsSelections: DbmsType[];
  problemId: string;
  judgeSelections: JudgeSelectionValue[];
  costSort: SubmitHistoryCostSortOrder;
  planFiltersByDbms: SubmitHistoryPlanFiltersByDbms;
}

interface SubmitHistoryFavoriteSnapshot {
  draftFilters: SubmitHistoryFilters;
  submittedFilters: SubmitHistoryFilters;
  requestedPage: number;
  selectedPlanSections: PlanSectionKey[];
  activePlanDetailDbms: DbmsType;
}

const submitHistoryLoadingRows = Array.from({ length: 10 }, (_, index) => index);
const costFormatter = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 1 });

const dbmsOptions: Array<{ value: DbmsType; label: string }> = [
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'oracle', label: 'Oracle' },
];

function readSubmitHistoryDbmsFromSearch(search: string) {
  const dbms = new URLSearchParams(search).get('dbms');
  return dbms === 'oracle' ? 'oracle' : 'postgresql';
}

function buildSubmitHistoryPath(dbms: DbmsType) {
  if (dbms === 'postgresql') {
    return SUBMIT_HISTORY_PATH;
  }

  return `${SUBMIT_HISTORY_PATH}?dbms=${encodeURIComponent(dbms)}`;
}

const judgeOptions: Array<{ value: JudgeSelectionValue; label: string }> = [
  { value: 'success', label: '정답' },
  { value: 'fail', label: '오답' },
];

const hintFilterDisplayOptions: SubmitHistoryPlanFilters['hintFilters'][number][] = ['UNUSED', 'USED'];
const DEFAULT_PLAN_SECTION_KEYS: PlanSectionKey[] = [
  'scanBucket',
  'joinBucket',
  'filterBucket',
  'sortBucket',
  'aggregateBucket',
  'hint',
];
const SUBMIT_HISTORY_SQL_HIGHLIGHT_KEYWORDS = new Set([
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
const SUBMIT_HISTORY_SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS = new Set([
  'FROM',
  'JOIN',
  'INTO',
  'UPDATE',
  'TABLE',
  'INDEX',
  'ON',
]);

function createEmptySubmitHistoryPage(): SubmitHistoryPageData {
  return {
    currentPage: 1,
    pageSize: 10,
    totalCount: 0,
    totalPages: 1,
    problemIds: [],
    histories: [],
  };
}

function createEmptyPlanFiltersByDbms(): SubmitHistoryPlanFiltersByDbms {
  return {
    postgresql: createEmptySubmitHistoryPlanFilters(),
    oracle: createEmptySubmitHistoryPlanFilters(),
  };
}

function createDefaultFilters(initialDbms: DbmsType = 'postgresql'): SubmitHistoryFilters {
  return {
    submitId: '',
    query: '',
    dbmsSelections: [initialDbms],
    problemId: '',
    judgeSelections: ['success', 'fail'],
    costSort: 'none',
    planFiltersByDbms: createEmptyPlanFiltersByDbms(),
  };
}

function formatCost(value: number) {
  return costFormatter.format(Math.round(value * 10) / 10);
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}

function formatSubmittedAt(value: string) {
  if (value.trim() === '') {
    return '-';
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return `${parsedDate.getFullYear()}-${padDatePart(parsedDate.getMonth() + 1)}-${padDatePart(parsedDate.getDate())} ${padDatePart(parsedDate.getHours())}:${padDatePart(parsedDate.getMinutes())}:${padDatePart(parsedDate.getSeconds())}`;
}

function toggleValue<T extends string>(values: T[], value: T) {
  return values.includes(value) ? values.filter((item) => item !== value) : [...values, value];
}

function resolveSubmitHistoryDbmsFilterValue(dbmsSelections: DbmsType[]): DbmsFilterValue {
  return dbmsSelections.length === 1 ? dbmsSelections[0] : 'all';
}

function resolveSubmitHistoryJudgeFilterValue(judgeSelections: JudgeSelectionValue[]): SubmitHistoryJudge {
  return judgeSelections.length === 1 ? judgeSelections[0] : 'all';
}

function toggleRequiredJudgeSelection(currentSelections: JudgeSelectionValue[], nextValue: JudgeSelectionValue) {
  if (currentSelections.includes(nextValue)) {
    return currentSelections.length === 1
      ? judgeOptions.map((option) => option.value).filter((value) => value !== nextValue)
      : currentSelections.filter((value) => value !== nextValue);
  }

  return judgeOptions
    .map((option) => option.value)
    .filter((value) => value === nextValue || currentSelections.includes(value));
}

function applySelectedPlanSectionsToFilters(
  planFilters: SubmitHistoryPlanFilters,
  selectedPlanSections: PlanSectionKey[],
  dbms: DbmsFilterValue,
): SubmitHistoryPlanFilters {
  const normalizedPlanFilters = normalizePlanFilters(planFilters, dbms);

  return {
    ...normalizedPlanFilters,
    scanBuckets: selectedPlanSections.includes('scanBucket') ? normalizedPlanFilters.scanBuckets : [],
    joinBuckets: selectedPlanSections.includes('joinBucket') ? normalizedPlanFilters.joinBuckets : [],
    filterBuckets: selectedPlanSections.includes('filterBucket') ? normalizedPlanFilters.filterBuckets : [],
    sortBuckets: selectedPlanSections.includes('sortBucket') ? normalizedPlanFilters.sortBuckets : [],
    aggregateBuckets: selectedPlanSections.includes('aggregateBucket') ? normalizedPlanFilters.aggregateBuckets : [],
    hintFilters: selectedPlanSections.includes('hint') ? normalizedPlanFilters.hintFilters : [],
  };
}

function clearPlanSectionFilters(planFilters: SubmitHistoryPlanFilters, sectionKey: PlanSectionKey): SubmitHistoryPlanFilters {
  switch (sectionKey) {
    case 'scanBucket':
      return { ...planFilters, scanBuckets: [] };
    case 'joinBucket':
      return { ...planFilters, joinBuckets: [] };
    case 'filterBucket':
      return { ...planFilters, filterBuckets: [] };
    case 'sortBucket':
      return { ...planFilters, sortBuckets: [] };
    case 'aggregateBucket':
      return { ...planFilters, aggregateBuckets: [] };
    case 'hint':
      return { ...planFilters, hintFilters: [] };
    default:
      return planFilters;
  }
}

function clearAllPlanSectionFilters(planFilters: SubmitHistoryPlanFilters): SubmitHistoryPlanFilters {
  return {
    ...planFilters,
    scanBuckets: [],
    joinBuckets: [],
    filterBuckets: [],
    sortBuckets: [],
    aggregateBuckets: [],
    hintFilters: [],
  };
}

function updatePlanMatchModeForAllDbms(planFiltersByDbms: SubmitHistoryPlanFiltersByDbms,
                                       matchMode: SubmitHistoryPlanFilters['matchMode']): SubmitHistoryPlanFiltersByDbms {
  return {
    postgresql: { ...planFiltersByDbms.postgresql, matchMode },
    oracle: { ...planFiltersByDbms.oracle, matchMode },
  };
}

function applySelectedPlanSectionsToFiltersByDbms(planFiltersByDbms: SubmitHistoryPlanFiltersByDbms,
                                                  selectedPlanSections: PlanSectionKey[]): SubmitHistoryPlanFiltersByDbms {
  return {
    postgresql: applySelectedPlanSectionsToFilters(planFiltersByDbms.postgresql, selectedPlanSections, 'postgresql'),
    oracle: applySelectedPlanSectionsToFilters(planFiltersByDbms.oracle, selectedPlanSections, 'oracle'),
  };
}

function resolveSubmittedPlanFiltersByDbms(
  planFiltersByDbms: SubmitHistoryPlanFiltersByDbms,
  dbmsSelections: DbmsType[],
  selectedPlanSections: PlanSectionKey[],
): SubmitHistoryPlanFiltersByDbms {
  if (dbmsSelections.length !== 1) {
    return createEmptyPlanFiltersByDbms();
  }

  return applySelectedPlanSectionsToFiltersByDbms(planFiltersByDbms, selectedPlanSections);
}

function clearPlanSectionFiltersByDbms(planFiltersByDbms: SubmitHistoryPlanFiltersByDbms,
                                       sectionKey: PlanSectionKey): SubmitHistoryPlanFiltersByDbms {
  return {
    postgresql: clearPlanSectionFilters(planFiltersByDbms.postgresql, sectionKey),
    oracle: clearPlanSectionFilters(planFiltersByDbms.oracle, sectionKey),
  };
}

function clearAllPlanSectionFiltersByDbms(planFiltersByDbms: SubmitHistoryPlanFiltersByDbms): SubmitHistoryPlanFiltersByDbms {
  return {
    postgresql: clearAllPlanSectionFilters(planFiltersByDbms.postgresql),
    oracle: clearAllPlanSectionFilters(planFiltersByDbms.oracle),
  };
}

function tokenizeSubmitHistorySqlLine(line: string) {
  const tokens: SubmitHistorySqlHighlightToken[] = [];
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

      if (SUBMIT_HISTORY_SQL_HIGHLIGHT_KEYWORDS.has(upperToken)) {
        tokens.push({
          text: token,
          kind:
            upperToken === 'EXPLAIN' || upperToken === 'ANALYZE' || upperToken === 'ANALYSE'
              ? 'explain-keyword'
              : 'keyword',
        });
        expectTable = SUBMIT_HISTORY_SQL_HIGHLIGHT_TABLE_CONTEXT_KEYWORDS.has(upperToken);
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

function renderSubmitHistoryHighlightedSql(sql: string) {
  const normalizedSql = sql.replace(/\r\n/g, '\n');
  const lines = normalizedSql.split('\n');

  return lines.map((line, lineIndex) => {
    const lineTokens = tokenizeSubmitHistorySqlLine(line);

    return (
      <Fragment key={`line-${lineIndex}`}>
        {lineTokens.map((token, tokenIndex) =>
          token.kind == null ? (
            <span key={`token-${lineIndex}-${tokenIndex}`}>{token.text}</span>
          ) : (
            <span key={`token-${lineIndex}-${tokenIndex}`} className={`solve-sql-token is-${token.kind}`}>
              {token.text}
            </span>
          ),
        )}
        {lineIndex < lines.length - 1 ? '\n' : null}
      </Fragment>
    );
  });
}

function SelectionCheckbox({ checked }: { checked: boolean }) {
  return <span className={`runtime-check-indicator ${checked ? 'is-checked' : ''}`} aria-hidden="true" />;
}

function buildSubmitHistoryPlanSections(history: SubmitHistoryEntry) {
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
      sectionLabel: 'Hint',
      labels: labelGroupsBySection.get('hint') ?? [],
    },
  ];
}

function hasExecutionPlanDetails(history: SubmitHistoryEntry) {
  return buildSubmitHistoryPlanSections(history).some((section) => section.labels.length > 0);
}

export default function SubmitHistoryPage() {
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<SubmitHistoryFavoriteSnapshot>('submitHistory'), []);
  const initialDbms = favoriteRestoreSnapshot?.draftFilters.dbmsSelections[0] ?? readSubmitHistoryDbmsFromSearch(window.location.search);
  const [draftFilters, setDraftFilters] = useState<SubmitHistoryFilters>(() => favoriteRestoreSnapshot?.draftFilters ?? createDefaultFilters(initialDbms));
  const [submittedFilters, setSubmittedFilters] = useState<SubmitHistoryFilters>(() => favoriteRestoreSnapshot?.submittedFilters ?? createDefaultFilters(initialDbms));
  const [requestedPage, setRequestedPage] = useState(() => favoriteRestoreSnapshot?.requestedPage ?? 1);
  const [isPageJumpEditing, setIsPageJumpEditing] = useState(false);
  const [pageJumpDraft, setPageJumpDraft] = useState('1');
  const [historyPage, setHistoryPage] = useState<SubmitHistoryPageData>(createEmptySubmitHistoryPage());
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [headerFilterMenuState, setHeaderFilterMenuState] = useState<HeaderFilterMenuState>(null);
  const [selectedPlanSections, setSelectedPlanSections] = useState<PlanSectionKey[]>(() => favoriteRestoreSnapshot?.selectedPlanSections ?? DEFAULT_PLAN_SECTION_KEYS);
  const [activePlanDetailDbms, setActivePlanDetailDbms] = useState<DbmsType>(() => favoriteRestoreSnapshot?.activePlanDetailDbms ?? 'postgresql');
  const [modalState, setModalState] = useState<SubmitHistoryModalState>(null);
  const headerFilterMenuRef = useRef<HTMLDivElement | null>(null);

  const submittedDbmsFilterValue = useMemo(
    () => resolveSubmitHistoryDbmsFilterValue(submittedFilters.dbmsSelections),
    [submittedFilters.dbmsSelections],
  );
  const submittedJudgeFilterValue = useMemo(
    () => resolveSubmitHistoryJudgeFilterValue(submittedFilters.judgeSelections),
    [submittedFilters.judgeSelections],
  );
  const selectedDbms = draftFilters.dbmsSelections[0] ?? 'postgresql';
  const activePlanFilters = useMemo(
    () => draftFilters.planFiltersByDbms[activePlanDetailDbms],
    [activePlanDetailDbms, draftFilters.planFiltersByDbms],
  );
  const availableBucketFilters = useMemo(
    () => buildAvailableBucketFilters(activePlanDetailDbms),
    [activePlanDetailDbms],
  );
  const availablePlanSections = useMemo(
    () => [
      ...availableBucketFilters.map((filter) => ({ key: filter.key as PlanSectionKey, label: filter.label })),
      { key: 'hint' as const, label: 'Hint' },
    ],
    [availableBucketFilters],
  );
  const normalizedSelectedPlanSections = useMemo(
    () => DEFAULT_PLAN_SECTION_KEYS.filter((sectionKey) => selectedPlanSections.includes(sectionKey)),
    [selectedPlanSections],
  );
  const allPlanSectionsSelected = normalizedSelectedPlanSections.length === DEFAULT_PLAN_SECTION_KEYS.length;
  const visibleBucketFilters = useMemo(
    () => availableBucketFilters.filter((filter) => normalizedSelectedPlanSections.includes(filter.key)),
    [availableBucketFilters, normalizedSelectedPlanSections],
  );
  useEffect(() => {
    clearFavoriteRestoreSnapshot('submitHistory');
  }, []);

  const highlightedModalSql = useMemo(() => {
    if (modalState?.type !== 'sql') {
      return null;
    }

    return renderSubmitHistoryHighlightedSql(modalState.history.submittedSql);
  }, [modalState]);
  const sqlModalHasExecutionPlanDetails = useMemo(
    () => (modalState?.type === 'sql' ? hasExecutionPlanDetails(modalState.history) : false),
    [modalState],
  );
  const planModalSections = useMemo(
    () => (modalState?.type === 'plan' ? buildSubmitHistoryPlanSections(modalState.history) : []),
    [modalState],
  );

  useEffect(() => {
    if (isPageJumpEditing) {
      return;
    }

    setPageJumpDraft(String(historyPage.currentPage));
  }, [historyPage.currentPage, isPageJumpEditing]);

  useEffect(() => {
    const nextDbms = readSubmitHistoryDbmsFromSearch(locationSearch);

    setActivePlanDetailDbms((currentDbms) => (currentDbms === nextDbms ? currentDbms : nextDbms));
    setDraftFilters((currentFilters) => {
      if (currentFilters.dbmsSelections.length === 1 && currentFilters.dbmsSelections[0] === nextDbms) {
        return currentFilters;
      }

      return {
        ...currentFilters,
        dbmsSelections: [nextDbms],
      };
    });
    setSubmittedFilters((currentFilters) => {
      if (currentFilters.dbmsSelections.length === 1 && currentFilters.dbmsSelections[0] === nextDbms) {
        return currentFilters;
      }

      return {
        ...currentFilters,
        dbmsSelections: [nextDbms],
      };
    });
  }, [locationSearch]);

  useEffect(() => {
    const nextPath = buildSubmitHistoryPath(selectedDbms);
    const currentPath = `${window.location.pathname}${window.location.search}`;

    if (currentPath !== nextPath) {
      window.history.replaceState(window.history.state ?? {}, '', nextPath);
    }
  }, [selectedDbms]);

  useEffect(() => {
    let cancelled = false;

    async function loadSubmitHistories() {
      setIsLoading(true);
      setLoadFailed(false);

      try {
        const fetchedPage = await fetchSubmitHistories({
          page: requestedPage,
          submitId: submittedFilters.submitId,
          query: submittedFilters.query,
          dbms: submittedDbmsFilterValue,
          problemId: submittedFilters.problemId,
          judge: submittedJudgeFilterValue,
          costSort: submittedFilters.costSort,
          planFiltersByDbms: submittedFilters.planFiltersByDbms,
        });

        if (cancelled) {
          return;
        }

        setHistoryPage(fetchedPage);
        if (fetchedPage.currentPage !== requestedPage) {
          setRequestedPage(fetchedPage.currentPage);
        }
      } catch {
        if (cancelled) {
          return;
        }

        setLoadFailed(true);
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadSubmitHistories();

    return () => {
      cancelled = true;
    };
  }, [requestedPage, submittedDbmsFilterValue, submittedJudgeFilterValue, submittedFilters]);

  useEffect(() => {
    if (headerFilterMenuState == null) {
      return;
    }

    function closeHeaderFilterMenu() {
      setHeaderFilterMenuState(null);
    }

    function handlePointerDown(event: MouseEvent) {
      const target = event.target as HTMLElement | null;
      if (target?.closest('[data-submit-history-filter-trigger="true"]')) {
        return;
      }

      if (!headerFilterMenuRef.current?.contains(event.target as Node)) {
        closeHeaderFilterMenu();
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        closeHeaderFilterMenu();
      }
    }

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleEscape);
    window.addEventListener('resize', closeHeaderFilterMenu);
    window.addEventListener('scroll', closeHeaderFilterMenu, true);

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleEscape);
      window.removeEventListener('resize', closeHeaderFilterMenu);
      window.removeEventListener('scroll', closeHeaderFilterMenu, true);
    };
  }, [headerFilterMenuState]);

  useEffect(() => {
    if (draftFilters.dbmsSelections.includes(activePlanDetailDbms)) {
      return;
    }

    setActivePlanDetailDbms(draftFilters.dbmsSelections[0] ?? 'postgresql');
  }, [activePlanDetailDbms, draftFilters.dbmsSelections]);

  useEffect(() => {
    if (modalState == null) {
      return;
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setModalState(null);
      }
    }

    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [modalState]);

  function commitImmediateFilters(
    updater: (currentFilters: SubmitHistoryFilters) => SubmitHistoryFilters,
    nextSelectedSections: PlanSectionKey[] = normalizedSelectedPlanSections,
  ) {
    setDraftFilters((currentFilters) => {
      const nextDraftFilters = updater(currentFilters);
      const normalizedPlanFiltersByDbms = resolveSubmittedPlanFiltersByDbms(
        nextDraftFilters.planFiltersByDbms,
        nextDraftFilters.dbmsSelections,
        nextSelectedSections,
      );
      const nextUiFilters = {
        ...nextDraftFilters,
        planFiltersByDbms: normalizedPlanFiltersByDbms,
      };
      const committedFilters = {
        ...nextUiFilters,
        submitId: submittedFilters.submitId,
        query: submittedFilters.query,
        problemId: submittedFilters.problemId,
      };

      setSubmittedFilters(committedFilters);
      setRequestedPage(1);
      return nextUiFilters;
    });
  }

  function applyFilters() {
    const normalizedPlanFiltersByDbms = resolveSubmittedPlanFiltersByDbms(
      draftFilters.planFiltersByDbms,
      draftFilters.dbmsSelections,
      normalizedSelectedPlanSections,
    );

    setSubmittedFilters({
      ...draftFilters,
      planFiltersByDbms: normalizedPlanFiltersByDbms,
    });
    setDraftFilters((currentFilters) => ({
      ...currentFilters,
      planFiltersByDbms: normalizedPlanFiltersByDbms,
    }));
    setRequestedPage(1);
    setHeaderFilterMenuState(null);
  }

  function selectDbmsTab(nextDbms: DbmsType) {
    setActivePlanDetailDbms(nextDbms);
    commitImmediateFilters((currentFilters) => ({
      ...currentFilters,
      dbmsSelections: [nextDbms],
    }));
    setHeaderFilterMenuState(null);
  }

  function updateBucketFilter(
    sectionKey: keyof Pick<
      SubmitHistoryPlanFilters,
      'scanBuckets' | 'joinBuckets' | 'filterBuckets' | 'sortBuckets' | 'aggregateBuckets'
    >,
    value: BucketFilterValue,
  ) {
    commitImmediateFilters((currentFilters) => ({
      ...currentFilters,
      planFiltersByDbms: {
        ...currentFilters.planFiltersByDbms,
        [activePlanDetailDbms]: {
          ...currentFilters.planFiltersByDbms[activePlanDetailDbms],
          [sectionKey]: toggleValue(currentFilters.planFiltersByDbms[activePlanDetailDbms][sectionKey], value),
        },
      },
    }));
  }

  function updateHintFilter(value: SubmitHistoryPlanFilters['hintFilters'][number]) {
    commitImmediateFilters((currentFilters) => ({
      ...currentFilters,
      planFiltersByDbms: {
        ...currentFilters.planFiltersByDbms,
        [activePlanDetailDbms]: {
          ...currentFilters.planFiltersByDbms[activePlanDetailDbms],
          hintFilters: toggleValue(currentFilters.planFiltersByDbms[activePlanDetailDbms].hintFilters, value),
        },
      },
    }));
  }

  function toggleDraftPlanSection(sectionKey: PlanSectionKey) {
    const willDeselect = normalizedSelectedPlanSections.includes(sectionKey);
    const nextSections = willDeselect
      ? normalizedSelectedPlanSections.filter((currentSection) => currentSection !== sectionKey)
      : [...normalizedSelectedPlanSections, sectionKey];
    const normalizedNextSections = DEFAULT_PLAN_SECTION_KEYS.filter((currentSection) => nextSections.includes(currentSection));

    setSelectedPlanSections(normalizedNextSections);

    if (willDeselect && normalizedSelectedPlanSections.length === 1) {
      setHeaderFilterMenuState(null);
    }

    commitImmediateFilters(
      (currentFilters) =>
        willDeselect
          ? {
              ...currentFilters,
              planFiltersByDbms: clearPlanSectionFiltersByDbms(currentFilters.planFiltersByDbms, sectionKey),
            }
          : currentFilters,
      normalizedNextSections,
    );
  }

  function toggleCostSortOrder() {
    commitImmediateFilters((currentFilters) => ({
      ...currentFilters,
      costSort:
        currentFilters.costSort === 'none'
          ? 'desc'
          : currentFilters.costSort === 'desc'
            ? 'asc'
            : 'none',
    }));
  }

  function toggleAllDraftPlanSections() {
    if (allPlanSectionsSelected) {
      const nextSections: PlanSectionKey[] = [];
      setSelectedPlanSections(nextSections);
      setHeaderFilterMenuState(null);
      commitImmediateFilters(
        (currentFilters) => ({
          ...currentFilters,
          planFiltersByDbms: clearAllPlanSectionFiltersByDbms(currentFilters.planFiltersByDbms),
        }),
        nextSections,
      );
      return;
    }

    setSelectedPlanSections(DEFAULT_PLAN_SECTION_KEYS);
    commitImmediateFilters((currentFilters) => currentFilters, DEFAULT_PLAN_SECTION_KEYS);
  }

  function toggleHeaderFilterMenu(filterKey: SubmitHistoryHeaderFilterKey, button: HTMLButtonElement) {
    setHeaderFilterMenuState((currentState) => {
      if (currentState?.key === filterKey) {
        return null;
      }

      const anchorElement = button.parentElement instanceof HTMLElement ? button.parentElement : button;
      const anchorRect = anchorElement.getBoundingClientRect();
      const viewportWidth = document.documentElement.clientWidth;
      const menuWidth = filterKey === 'plan'
        ? Math.min(760, viewportWidth - 24)
        : Math.min(116, viewportWidth - 24);
      const nextLeft = filterKey === 'plan'
        ? Math.max(12, Math.min(anchorRect.right - menuWidth, viewportWidth - menuWidth - 12))
        : Math.max(12, Math.min(anchorRect.left, viewportWidth - menuWidth - 12));

      return {
        key: filterKey,
        left: nextLeft,
        top: anchorRect.bottom + 8,
        width: menuWidth,
      };
    });
  }

  function applyPageJump() {
    const parsedPage = Number.parseInt(pageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? historyPage.currentPage
      : Math.min(historyPage.totalPages, Math.max(1, parsedPage));

    setPageJumpDraft(String(nextPage));
    setIsPageJumpEditing(false);

    if (nextPage !== historyPage.currentPage) {
      setRequestedPage(nextPage);
    }
  }

  function cancelPageJump() {
    setPageJumpDraft(String(historyPage.currentPage));
    setIsPageJumpEditing(false);
  }

  const modalContent =
    modalState == null || typeof document === 'undefined'
      ? null
      : createPortal(
          <div
            className="submit-history-modal-overlay"
            role="presentation"
            onMouseDown={(event) => {
              if (event.target === event.currentTarget) {
                setModalState(null);
              }
            }}
          >
            {modalState.type === 'sql' ? (
              <div className="submit-history-modal" role="dialog" aria-modal="true" aria-label="제출 SQL 보기">
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <div className="submit-history-modal-title-row">
                      <strong>제출 결과</strong>
                      <span className={`submit-history-modal-title-status ${modalState.history.success ? 'is-success' : 'is-fail'}`}>
                        {modalState.history.success ? '정답' : '오답'}
                      </span>
                      <button
                        type="button"
                        className="submit-history-modal-plan-action"
                        aria-label="실행계획 요소 자세히 보기"
                        title="실행계획 요소 자세히 보기"
                        disabled={!sqlModalHasExecutionPlanDetails}
                        onClick={() => setModalState({ type: 'plan', history: modalState.history })}
                      >
                        ↗
                      </button>
                    </div>
                  </div>
                  <button type="button" className="submit-history-modal-close" aria-label="제출 결과 닫기" onClick={() => setModalState(null)}>
                    닫기
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-sql-modal-body">
                  <pre className="submit-history-sql-viewer submit-history-sql-highlight" aria-label="제출 SQL">
                    {highlightedModalSql}
                  </pre>
                </div>
              </div>
            ) : (
              <div
                className="submit-history-modal submit-history-plan-modal"
                role="dialog"
                aria-modal="true"
                aria-label="실행계획 요소 보기"
              >
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <strong>실행 계획 요소</strong>
                  </div>
                  <button type="button" className="submit-history-modal-close" aria-label="실행 계획 요소 닫기" onClick={() => setModalState(null)}>
                    닫기
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-plan-modal-body">
                  <div className="runtime-subfilter-board runtime-plan-shell-panel submit-history-plan-detail-board">
                    {planModalSections.map((group) => (
                      <div key={`${group.sectionKey}-${group.sectionLabel}`} className="runtime-subfilter-row">
                        <span className="runtime-subfilter-label">{group.sectionLabel}</span>
                        <div className="runtime-subfilter-options submit-history-plan-detail-options">
                          <div className="runtime-subfilter-chip-grid submit-history-plan-detail-grid">
                            {(group.labels.length > 0 ? group.labels : ['없음']).map((label) => {
                              const isEmpty = group.labels.length === 0;

                              return (
                                <span key={`${group.sectionKey}-${label}`} className="runtime-subfilter-option">
                                  <span className={`runtime-subfilter-button runtime-subfilter-button-plain submit-history-plan-static-item ${isEmpty ? 'is-empty' : 'is-selected'}`}>
                                    <span className="runtime-check-label">{label}</span>
                                  </span>
                                </span>
                              );
                            })}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>,
          document.body,
        );

  const headerFilterMenuContent =
    headerFilterMenuState == null || typeof document === 'undefined'
      ? null
      : createPortal(
          <div
            ref={headerFilterMenuRef}
            className={`submit-history-header-filter-menu ${headerFilterMenuState.key === 'plan' ? 'is-plan' : ''}`}
            role="menu"
            aria-label={headerFilterMenuState.key === 'judge' ? '제출 결과 필터 옵션' : '실행계획 요소 필터 옵션'}
            style={{
              top: `${headerFilterMenuState.top}px`,
              left: `${headerFilterMenuState.left}px`,
              width: `${headerFilterMenuState.width}px`,
            }}
          >
            {headerFilterMenuState.key === 'judge' ? (
              <div className="submit-history-header-filter-menu-section" role="group" aria-label="제출 결과 선택">
                <div className="submit-history-header-filter-checks">
                  {judgeOptions.map((option) => {
                    const isSelected = draftFilters.judgeSelections.includes(option.value);

                    return (
                      <label key={option.value} className="problem-status-check">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() =>
                            commitImmediateFilters((currentFilters) => ({
                              ...currentFilters,
                              judgeSelections: toggleRequiredJudgeSelection(currentFilters.judgeSelections, option.value),
                            }))
                          }
                          className="problem-status-check-input"
                          aria-label={option.label}
                        />
                        <span className="problem-status-check-text">{option.label}</span>
                        <span className="problem-status-check-ui" aria-hidden="true" />
                      </label>
                    );
                  })}
                </div>
              </div>
            ) : (
              <div className="submit-history-plan-popover submit-history-header-plan-popover">
                <div className="submit-history-plan-filter-bar submit-history-plan-filter-menu-bar" role="group" aria-label="실행계획 요소 선택">
                  <div className="problem-status-checks submit-history-plan-mode-checks" role="group" aria-label="실행계획 요소 검색 방식">
                    {FILTER_MODE_OPTIONS.map((option) => {
                      const isSelected = activePlanFilters.matchMode === option.key;

                      return (
                        <label key={option.key} className="problem-status-check submit-history-plan-mode-check">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() =>
                              commitImmediateFilters((currentFilters) => ({
                                ...currentFilters,
                                planFiltersByDbms: updatePlanMatchModeForAllDbms(currentFilters.planFiltersByDbms, option.key),
                              }))
                            }
                            className="problem-status-check-input"
                            aria-label={option.label}
                          />
                          <span className="problem-status-check-text">{option.label}</span>
                          <span className="problem-status-check-ui" aria-hidden="true" />
                        </label>
                      );
                    })}
                  </div>
                  <div className="problem-status-checks submit-history-plan-option-checks" role="group" aria-label="실행계획 요소 선택">
                    <label className="problem-status-check submit-history-plan-option-check is-all">
                      <input
                        type="checkbox"
                        checked={allPlanSectionsSelected}
                        onChange={toggleAllDraftPlanSections}
                        className="problem-status-check-input"
                        aria-label="전체"
                      />
                      <span className="problem-status-check-text">전체</span>
                      <span className="problem-status-check-ui" aria-hidden="true" />
                    </label>

                    {availablePlanSections.map((section) => {
                      const isSelected = normalizedSelectedPlanSections.includes(section.key);

                      return (
                        <label key={section.key} className="problem-status-check submit-history-plan-option-check">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => toggleDraftPlanSection(section.key)}
                            className="problem-status-check-input"
                            aria-label={section.label}
                          />
                          <span className="problem-status-check-text">{section.label}</span>
                          <span className="problem-status-check-ui" aria-hidden="true" />
                        </label>
                      );
                    })}
                  </div>
                </div>

                {normalizedSelectedPlanSections.length > 0 ? (
                  <div
                    className="runtime-subfilter-board runtime-plan-shell-panel submit-history-plan-board submit-history-head-plan-board"
                    role="group"
                    aria-label="실행계획 요소 상세 선택"
                  >
                    {visibleBucketFilters.map((filter) => {
                      const filterFieldKey = toPlanFilterFieldKey(filter.key);
                      const selectedValues = activePlanFilters[filterFieldKey] as readonly BucketFilterValue[];
                      const isAllSelected =
                        filter.options.length > 0 && filter.options.every((value) => selectedValues.includes(value));

                      return (
                        <div key={filter.key} className="runtime-subfilter-row">
                          <span className="runtime-subfilter-label">{filter.label}</span>
                          <div className="runtime-subfilter-options is-bucket">
                            <button
                              type="button"
                              className={`runtime-subfilter-button runtime-subfilter-all-button runtime-check-button ${isAllSelected ? 'is-selected' : ''}`}
                              aria-pressed={isAllSelected}
                              onClick={() =>
                                commitImmediateFilters((currentFilters) => ({
                                  ...currentFilters,
                                  planFiltersByDbms: {
                                    ...currentFilters.planFiltersByDbms,
                                    [activePlanDetailDbms]: {
                                      ...currentFilters.planFiltersByDbms[activePlanDetailDbms],
                                      [filterFieldKey]: isAllSelected ? [] : [...filter.options],
                                    },
                                  },
                                }))
                              }
                            >
                              <SelectionCheckbox checked={isAllSelected} />
                              <span className="runtime-check-label">전체</span>
                            </button>

                            <div className="runtime-subfilter-chip-grid">
                              {filter.options.map((value) => {
                                const isSelected = selectedValues.includes(value);

                                return (
                                  <span key={value} className="runtime-subfilter-option">
                                    <button
                                      type="button"
                                      className={`runtime-subfilter-button runtime-subfilter-button-plain runtime-check-button ${isSelected ? 'is-selected' : ''}`}
                                      aria-pressed={isSelected}
                                      onClick={() => updateBucketFilter(filterFieldKey, value)}
                                    >
                                      <SelectionCheckbox checked={isSelected} />
                                      <span className="runtime-check-label">{formatBucketDisplayLabel(value)}</span>
                                    </button>
                                  </span>
                                );
                              })}
                            </div>
                          </div>
                        </div>
                      );
                    })}

                    {normalizedSelectedPlanSections.includes('hint') ? (
                      <div className="runtime-subfilter-row">
                        <span className="runtime-subfilter-label">Hint</span>
                        <div className="runtime-subfilter-options is-bucket">
                          <button
                            type="button"
                            className={`runtime-subfilter-button runtime-subfilter-all-button runtime-check-button ${HINT_FILTER_OPTIONS.every((value) => activePlanFilters.hintFilters.includes(value)) ? 'is-selected' : ''}`}
                            aria-pressed={HINT_FILTER_OPTIONS.every((value) => activePlanFilters.hintFilters.includes(value))}
                            onClick={() =>
                              commitImmediateFilters((currentFilters) => ({
                                ...currentFilters,
                                planFiltersByDbms: {
                                  ...currentFilters.planFiltersByDbms,
                                  [activePlanDetailDbms]: {
                                    ...currentFilters.planFiltersByDbms[activePlanDetailDbms],
                                    hintFilters: HINT_FILTER_OPTIONS.every((value) => currentFilters.planFiltersByDbms[activePlanDetailDbms].hintFilters.includes(value))
                                      ? []
                                      : [...hintFilterDisplayOptions],
                                  },
                                },
                              }))
                            }
                          >
                            <SelectionCheckbox
                              checked={HINT_FILTER_OPTIONS.every((value) => activePlanFilters.hintFilters.includes(value))}
                            />
                            <span className="runtime-check-label">전체</span>
                          </button>

                          <div className="runtime-subfilter-chip-grid">
                            {hintFilterDisplayOptions.map((value) => {
                              const isSelected = activePlanFilters.hintFilters.includes(value);

                              return (
                                <span key={value} className="runtime-subfilter-option">
                                  <button
                                    type="button"
                                    className={`runtime-subfilter-button runtime-subfilter-button-plain runtime-check-button ${isSelected ? 'is-selected' : ''}`}
                                    aria-pressed={isSelected}
                                    onClick={() => updateHintFilter(value)}
                                  >
                                    <SelectionCheckbox checked={isSelected} />
                                    <span className="runtime-check-label">{formatHintFilterLabel(value)}</span>
                                  </button>
                                </span>
                              );
                            })}
                          </div>
                        </div>
                      </div>
                    ) : null}
                  </div>
                ) : (
                  <div className="submit-history-header-filter-empty">선택한 실행계획 요소가 없습니다.</div>
                )}
              </div>
            )}
          </div>,
          document.body,
        );

  return (
    <div className="page-stack submit-history-page home-page">
      <section className="panel-card compact problem-toolbar-card submit-history-toolbar-card">
        <div className="problem-toolbar submit-history-toolbar-stack">
          <div className="solve-dbms-tab-row submit-history-dbms-tab-row" role="tablist" aria-label="제출 목록 DBMS 선택">
            {dbmsOptions.map((option) => {
              const isSelected = option.value === selectedDbms;

              return (
                <button
                  key={option.value}
                  type="button"
                  className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''}`}
                  role="tab"
                  aria-selected={isSelected}
                  onClick={() => selectDbmsTab(option.value)}
                >
                  {option.label}
                </button>
              );
            })}
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={`제출 목록 / ${selectedDbms === 'oracle' ? 'Oracle' : 'PostgreSQL'}`}
              path={buildSubmitHistoryPath(selectedDbms)}
              snapshot={{
                kind: 'submitHistory',
                payload: {
                  draftFilters,
                  submittedFilters,
                  requestedPage,
                  selectedPlanSections,
                  activePlanDetailDbms,
                },
              }}
            />
          </div>

          <form
            className="submit-history-toolbar home-problem-search-form"
            onSubmit={(event) => {
              event.preventDefault();
              applyFilters();
            }}
          >
            <div className="submit-history-toolbar-row submit-history-search-row">
              <label className="problem-search-field home-problem-search-field submit-history-search-field submit-history-search-field-plain">
                <input
                  type="search"
                  inputMode="numeric"
                  value={draftFilters.submitId}
                  onChange={(event) =>
                    setDraftFilters((currentFilters) => ({
                      ...currentFilters,
                      submitId: event.target.value.replace(/\D+/g, ''),
                    }))
                  }
                  className="text-field problem-search-input home-problem-search-input submit-history-search-input submit-history-search-input-plain"
                  placeholder="제출번호"
                  aria-label="제출번호 검색"
                />
              </label>

              <label className="problem-search-field home-problem-search-field submit-history-search-field submit-history-search-field-plain">
                <input
                  type="search"
                  value={draftFilters.query}
                  onChange={(event) =>
                    setDraftFilters((currentFilters) => ({ ...currentFilters, query: event.target.value }))
                  }
                  className="text-field problem-search-input home-problem-search-input submit-history-search-input submit-history-search-input-plain"
                  placeholder="Handle 검색"
                  aria-label="Handle 검색"
                />
              </label>

              <label className="problem-search-field home-problem-search-field submit-history-search-field submit-history-search-field-plain">
                <input
                  type="search"
                  value={draftFilters.problemId}
                  onChange={(event) =>
                    setDraftFilters((currentFilters) => ({ ...currentFilters, problemId: event.target.value }))
                  }
                  className="text-field problem-search-input home-problem-search-input submit-history-search-input submit-history-search-input-plain"
                  placeholder="문제번호"
                  aria-label="문제번호 검색"
                />
              </label>

              <button type="submit" className="submit-history-toolbar-submit" aria-label="검색">
                검색
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="panel-card problem-board submit-history-board">
        {loadFailed ? (
          <PageLoadFailureState className="submit-history-empty-state" />
        ) : (
          <div className={`submit-history-table-shell ${isLoading ? 'is-loading' : ''}`}>
            <div className="submit-history-table" role="table" aria-label="제출 이력 목록">
              <div className="submit-history-row submit-history-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">제출번호</div>
                <div role="columnheader" className="submit-history-head-cell">Handle</div>
                <div role="columnheader" className="submit-history-head-cell">문제 번호</div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>제출 결과</span>
                  <button
                    type="button"
                    data-submit-history-filter-trigger="true"
                    className={`submit-history-head-filter-trigger ${headerFilterMenuState?.key === 'judge' ? 'is-open' : ''} ${draftFilters.judgeSelections.length !== judgeOptions.length ? 'is-active' : ''}`}
                    aria-label="제출 결과 필터 열기"
                    onClick={(event) => toggleHeaderFilterMenu('judge', event.currentTarget)}
                  >
                    ▾
                  </button>
                </div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>Cost</span>
                  <button
                    type="button"
                    className={`submit-history-head-filter-trigger submit-history-head-sort-trigger ${draftFilters.costSort !== 'none' ? 'is-active' : ''}`}
                    aria-label={draftFilters.costSort === 'asc' ? 'Cost 오름차순 정렬' : draftFilters.costSort === 'desc' ? 'Cost 내림차순 정렬' : 'Cost 정렬 없음'}
                    onClick={toggleCostSortOrder}
                  >
                    {draftFilters.costSort === 'asc' ? <SortAscendingIcon /> : draftFilters.costSort === 'desc' ? <SortDescendingIcon /> : <SortNeutralIcon />}
                  </button>
                </div>
                <div role="columnheader" className="submit-history-head-cell">제출 시각</div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>실행계획요소</span>
                  <button
                    type="button"
                    data-submit-history-filter-trigger="true"
                    className={`submit-history-head-filter-trigger ${headerFilterMenuState?.key === 'plan' ? 'is-open' : ''}`}
                    aria-label="실행계획요소 필터 열기"
                    onClick={(event) => toggleHeaderFilterMenu('plan', event.currentTarget)}
                  >
                    ▾
                  </button>
                </div>
              </div>

              {isLoading && historyPage.histories.length === 0 ? (
                submitHistoryLoadingRows.map((rowIndex) => (
                  <div key={`submit-history-loading-${rowIndex}`} className="submit-history-row submit-history-body" role="row" aria-hidden="true">
                    <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                    <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                    <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                    <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                    <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                    <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                    <span className="submit-history-cell" role="cell"><span className="wave-loading-placeholder is-mini" /></span>
                  </div>
                ))
              ) : historyPage.histories.length === 0 ? (
                <div className="submit-history-row submit-history-empty-row" role="row">
                  <span className="submit-history-empty-cell" role="cell">
                    조건에 맞는 제출 이력이 없습니다.
                  </span>
                </div>
              ) : (
                historyPage.histories.map((history) => (
                  <article key={history.submitId} className="submit-history-row submit-history-body" role="row">
                    <span className="submit-history-cell" role="cell" data-label="제출번호">
                      {history.submitId}
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="Handle">
                      <button
                        type="button"
                        className="submit-history-link-button"
                        onClick={() => navigate(getProfilePath(history.handle))}
                        aria-label={`${history.handle} 프로필 이동`}
                      >
                        {history.handle}
                      </button>
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="문제 번호">
                      <button
                        type="button"
                        className="submit-history-link-button"
                        onClick={() => navigate(`${PROBLEMS_PATH}/${encodeURIComponent(history.problemId)}`)}
                        aria-label={`문제 ${history.problemId} 이동`}
                      >
                        {history.problemId}
                      </button>
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="제출 결과">
                      <button
                        type="button"
                        className={`submit-history-status-text ${history.success ? 'is-success' : 'is-fail'}`}
                        onClick={() => setModalState({ type: 'sql', history })}
                      >
                        {history.success ? '정답' : '오답'}
                      </button>
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="Cost">
                      {history.success || history.cost > 0 ? formatCost(history.cost) : '-'}
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="제출 시각">
                      {formatSubmittedAt(history.submittedAt)}
                    </span>
                    <span className="submit-history-cell submit-history-cell-plan" role="cell" data-label="실행계획요소">
                      {hasExecutionPlanDetails(history) ? (
                        <button
                          type="button"
                          className="submit-history-detail-button"
                          aria-label={`${getPlanElementButtonLabel(history.dbms, history.executionPlanElement)} 자세히 보기`}
                          title={getPlanElementButtonLabel(history.dbms, history.executionPlanElement)}
                          onClick={() => setModalState({ type: 'plan', history })}
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
            {isLoading ? (
              <div className="submit-history-loading-overlay" aria-live="polite" aria-label="로딩 중">
                <span className="page-loading-spinner submit-history-loading-badge" aria-hidden="true" />
              </div>
            ) : null}
          </div>
        )}

        {!loadFailed && historyPage.totalCount > 0 ? (
          <div className="problem-pagination submit-history-pagination" role="navigation" aria-label="제출 이력 페이지">
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setRequestedPage((page) => Math.max(1, page - 1))}
              disabled={historyPage.currentPage === 1}
            >
              이전
            </button>

            {isPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="problem-pagination-meta-input"
                aria-label="이동할 제출 이력 페이지 입력"
                value={pageJumpDraft}
                onChange={(event) => {
                  const nextValue = event.target.value.replace(/\D+/g, '');
                  setPageJumpDraft(nextValue);
                }}
                onBlur={applyPageJump}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    applyPageJump();
                    return;
                  }

                  if (event.key === 'Escape') {
                    event.preventDefault();
                    cancelPageJump();
                  }
                }}
                autoFocus
              />
            ) : (
              <button
                type="button"
                className="problem-pagination-meta problem-pagination-meta-button"
                aria-label="이동할 제출 이력 페이지 입력 열기"
                onClick={() => {
                  setPageJumpDraft(String(historyPage.currentPage));
                  setIsPageJumpEditing(true);
                }}
              >
                {`${historyPage.currentPage} / ${historyPage.totalPages}`}
              </button>
            )}

            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setRequestedPage((page) => Math.min(historyPage.totalPages, page + 1))}
              disabled={historyPage.currentPage >= historyPage.totalPages}
            >
              다음
            </button>
          </div>
        ) : null}
      </section>
      {modalContent}
      {headerFilterMenuContent}
    </div>
  );
}
