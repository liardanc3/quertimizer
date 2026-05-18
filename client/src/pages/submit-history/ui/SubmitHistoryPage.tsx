import { createPortal } from 'react-dom';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ExecutionPlanDetailBoard,
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
} from '@/entities/execution-plan';
import { FavoriteTabButton } from '@/features/favorite-tab';
import { DataTable } from '@/shared/ui';
import { LoadingOverlay } from '@/shared/ui';
import { PageErrorState } from '@/shared/ui';
import { Pagination } from '@/shared/ui';
import { SortIcon } from '@/shared/ui/icons';
import { replaceQueryState, useLocationSearch } from '@/shared/lib/hooks/use-location-state';
import useDismissableLayer from '@/shared/lib/hooks/use-dismissable-layer';
import useRequestState from '@/shared/lib/hooks/use-request-state';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '@/features/favorite-tab';
import { fetchSubmitHistories } from '@/shared/api/submit-history-api';
import { getProfilePath, PROBLEMS_PATH, SUBMIT_HISTORY_PATH, navigate } from '@/shared/config/navigation';
import { getUiTextValue, useUiText } from '@/shared/config/ui-text';
import { formatCost, formatSubmittedAt } from '@/shared/lib/formatters';
import { renderStaticHighlightedSql } from '@/shared/lib/sql-highlighter';
import type {
  DbmsType,
  SubmitHistoryEntry,
  SubmitHistoryJudge,
  SubmitHistoryPageData,
  SubmitHistoryPlanFilters,
} from '@/shared/api/domain';

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

type JudgeSelectionValue = Exclude<SubmitHistoryJudge, 'all'>;
type SubmitHistoryCostSortOrder = 'none' | 'desc' | 'asc';
type SubmitHistoryHeaderFilterKey = 'judge' | 'plan';

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

function readSubmitHistoryDbmsFromSearch(search: string) {
  const dbms = new URLSearchParams(search).get('dbms');
  return dbms === 'mysql' ? 'mysql' : 'postgresql';
}

function buildSubmitHistoryPath(dbms: DbmsType) {
  if (dbms === 'postgresql') {
    return SUBMIT_HISTORY_PATH;
  }

  return `${SUBMIT_HISTORY_PATH}?dbms=${encodeURIComponent(dbms)}`;
}

const judgeOptions: JudgeSelectionValue[] = ['success', 'fail'];

const hintFilterDisplayOptions: SubmitHistoryPlanFilters['hintFilters'][number][] = ['UNUSED', 'USED'];
const DEFAULT_PLAN_SECTION_KEYS: PlanSectionKey[] = [
  'scanBucket',
  'joinBucket',
  'filterBucket',
  'sortBucket',
  'aggregateBucket',
  'hint',
];

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
    mysql: createEmptySubmitHistoryPlanFilters(),
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
      ? judgeOptions.filter((value) => value !== nextValue)
      : currentSelections.filter((value) => value !== nextValue);
  }

  return judgeOptions.filter((value) => value === nextValue || currentSelections.includes(value));
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
    mysql: { ...planFiltersByDbms.mysql, matchMode },
  };
}

function applySelectedPlanSectionsToFiltersByDbms(planFiltersByDbms: SubmitHistoryPlanFiltersByDbms,
                                                  selectedPlanSections: PlanSectionKey[]): SubmitHistoryPlanFiltersByDbms {
  return {
    postgresql: applySelectedPlanSectionsToFilters(planFiltersByDbms.postgresql, selectedPlanSections, 'postgresql'),
    mysql: applySelectedPlanSectionsToFilters(planFiltersByDbms.mysql, selectedPlanSections, 'mysql'),
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
    mysql: clearPlanSectionFilters(planFiltersByDbms.mysql, sectionKey),
  };
}

function clearAllPlanSectionFiltersByDbms(planFiltersByDbms: SubmitHistoryPlanFiltersByDbms): SubmitHistoryPlanFiltersByDbms {
  return {
    postgresql: clearAllPlanSectionFilters(planFiltersByDbms.postgresql),
    mysql: clearAllPlanSectionFilters(planFiltersByDbms.mysql),
  };
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
      sectionLabel: getUiTextValue('COMMON_HINT_LABEL', 'Hint'),
      labels: labelGroupsBySection.get('hint') ?? [],
    },
  ];
}

function hasExecutionPlanDetails(history: SubmitHistoryEntry) {
  return buildSubmitHistoryPlanSections(history).some((section) => section.labels.length > 0);
}

export default function SubmitHistoryPage() {
  const { text } = useUiText();
  const locationSearch = useLocationSearch();
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<SubmitHistoryFavoriteSnapshot>('submitHistory'), []);
  const initialDbms = favoriteRestoreSnapshot?.draftFilters.dbmsSelections[0] ?? readSubmitHistoryDbmsFromSearch(window.location.search);
  const [draftFilters, setDraftFilters] = useState<SubmitHistoryFilters>(() => favoriteRestoreSnapshot?.draftFilters ?? createDefaultFilters(initialDbms));
  const [submittedFilters, setSubmittedFilters] = useState<SubmitHistoryFilters>(() => favoriteRestoreSnapshot?.submittedFilters ?? createDefaultFilters(initialDbms));
  const [requestedPage, setRequestedPage] = useState(() => favoriteRestoreSnapshot?.requestedPage ?? 1);
  const {
    data: historyPage,
    setData: setHistoryPage,
    isLoading,
    setIsLoading,
    error: loadError,
    beginRequest,
    failRequest,
  } = useRequestState<SubmitHistoryPageData>(createEmptySubmitHistoryPage);
  const [headerFilterMenuState, setHeaderFilterMenuState] = useState<HeaderFilterMenuState>(null);
  const [selectedPlanSections, setSelectedPlanSections] = useState<PlanSectionKey[]>(() => favoriteRestoreSnapshot?.selectedPlanSections ?? DEFAULT_PLAN_SECTION_KEYS);
  const [activePlanDetailDbms, setActivePlanDetailDbms] = useState<DbmsType>(() => favoriteRestoreSnapshot?.activePlanDetailDbms ?? 'postgresql');
  const [modalState, setModalState] = useState<SubmitHistoryModalState>(null);
  const headerFilterMenuRef = useRef<HTMLDivElement | null>(null);
  const headerFilterMenuLayerRefs = useMemo(() => [headerFilterMenuRef], []);

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
  const submitHistoryDbmsOptions = useMemo(
    () => [
      { value: 'postgresql' as const, label: text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL') },
      { value: 'mysql' as const, label: text('COMMON_MYSQL_LABEL', 'MySQL') },
    ],
    [text],
  );
  const submitHistoryJudgeOptions = useMemo(
    () => [
      { value: 'success' as const, label: text('SUBMIT_HISTORY_RESULT_CORRECT_LABEL', '정답') },
      { value: 'fail' as const, label: text('SUBMIT_HISTORY_RESULT_WRONG_LABEL', '오답') },
    ],
    [text],
  );
  const selectedDbmsLabel = selectedDbms === 'mysql'
    ? text('COMMON_MYSQL_LABEL', 'MySQL')
    : text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL');
  const availablePlanSections = useMemo(
    () => [
      ...availableBucketFilters.map((filter) => ({ key: filter.key as PlanSectionKey, label: filter.label })),
      { key: 'hint' as const, label: text('COMMON_HINT_LABEL', 'Hint') },
    ],
    [availableBucketFilters, text],
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

    return renderStaticHighlightedSql(modalState.history.submittedSql);
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
    replaceQueryState(nextPath);
  }, [selectedDbms]);

  useEffect(() => {
    let cancelled = false;

    async function loadSubmitHistories() {
      beginRequest();

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
      } catch (error) {
        if (cancelled) {
          return;
        }

        failRequest(error, text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
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

  useDismissableLayer({
    enabled: headerFilterMenuState != null,
    refs: headerFilterMenuLayerRefs,
    onDismiss: () => setHeaderFilterMenuState(null),
    dismissOnResize: true,
    dismissOnScroll: true,
    shouldIgnoreOutsidePointerDown: (target) => target?.closest('[data-submit-history-filter-trigger="true"]') != null,
  });

  useEffect(() => {
    if (draftFilters.dbmsSelections.includes(activePlanDetailDbms)) {
      return;
    }

    setActivePlanDetailDbms(draftFilters.dbmsSelections[0] ?? 'postgresql');
  }, [activePlanDetailDbms, draftFilters.dbmsSelections]);

  useDismissableLayer({
    enabled: modalState != null,
    refs: [],
    onDismiss: () => setModalState(null),
    dismissOnOutsidePointerDown: false,
  });

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

  const modalContent =
    modalState == null || typeof document === 'undefined'
      ? null
      : createPortal(
          <div
            className="submit-history-modal-overlay"
            role="presentation"
            onMouseDown={(event) => {
              event.stopPropagation();
            }}
          >
            {modalState.type === 'sql' ? (
              <div
                className="submit-history-modal"
                role="dialog"
                aria-modal="true"
                aria-label={text('SUBMIT_HISTORY_SQL_MODAL_LABEL', '제출 SQL 보기')}
              >
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <div className="submit-history-modal-title-row">
                      <strong>{text('SUBMIT_HISTORY_RESULT_TITLE', '제출 결과')}</strong>
                      <span className={`submit-history-modal-title-status ${modalState.history.success ? 'is-success' : 'is-fail'}`}>
                        {modalState.history.success
                          ? text('SUBMIT_HISTORY_RESULT_CORRECT_LABEL', '정답')
                          : text('SUBMIT_HISTORY_RESULT_WRONG_LABEL', '오답')}
                      </span>
                      <button
                        type="button"
                        className="submit-history-modal-plan-action"
                        aria-label={text('SUBMIT_HISTORY_PLAN_DETAIL_BUTTON_LABEL', '실행계획 요소 자세히 보기')}
                        title={text('SUBMIT_HISTORY_PLAN_DETAIL_BUTTON_LABEL', '실행계획 요소 자세히 보기')}
                        disabled={!sqlModalHasExecutionPlanDetails}
                        onClick={() => setModalState({ type: 'plan', history: modalState.history })}
                      >
                        ↗
                      </button>
                    </div>
                  </div>
                  <button
                    type="button"
                    className="submit-history-modal-close"
                    aria-label={text('SUBMIT_HISTORY_MODAL_CLOSE_LABEL', '제출 결과 닫기')}
                    onClick={() => setModalState(null)}
                  >
                    {text('COMMON_CLOSE_BUTTON', '닫기')}
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-sql-modal-body">
                  <pre
                    className="submit-history-sql-viewer submit-history-sql-highlight"
                    aria-label={text('SUBMIT_HISTORY_SQL_VIEWER_LABEL', '제출 SQL')}
                  >
                    {highlightedModalSql}
                  </pre>
                </div>
              </div>
            ) : (
              <div
                className="submit-history-modal submit-history-plan-modal"
                role="dialog"
                aria-modal="true"
                aria-label={text('SUBMIT_HISTORY_PLAN_MODAL_LABEL', '실행계획 요소 보기')}
              >
                <div className="submit-history-modal-header">
                  <div className="submit-history-modal-copy">
                    <strong>{text('SUBMIT_HISTORY_PLAN_TITLE', '실행 계획 요소')}</strong>
                  </div>
                  <button
                    type="button"
                    className="submit-history-modal-close"
                    aria-label={text('SUBMIT_HISTORY_PLAN_MODAL_CLOSE_LABEL', '실행 계획 요소 닫기')}
                    onClick={() => setModalState(null)}
                  >
                    {text('COMMON_CLOSE_BUTTON', '닫기')}
                  </button>
                </div>

                <div className="submit-history-modal-body submit-history-plan-modal-body">
                  <ExecutionPlanDetailBoard
                    sections={planModalSections}
                    noneLabel={text('COMMON_NONE_LABEL', '없음')}
                    className="submit-history-plan-detail-board"
                  />
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
            aria-label={
              headerFilterMenuState.key === 'judge'
                ? text('SUBMIT_HISTORY_RESULT_FILTER_MENU_LABEL', '제출 결과 필터 옵션')
                : text('SUBMIT_HISTORY_PLAN_FILTER_MENU_LABEL', '실행계획 요소 필터 옵션')
            }
            style={{
              top: `${headerFilterMenuState.top}px`,
              left: `${headerFilterMenuState.left}px`,
              width: `${headerFilterMenuState.width}px`,
            }}
          >
            {headerFilterMenuState.key === 'judge' ? (
              <div
                className="submit-history-header-filter-menu-section"
                role="group"
                aria-label={text('SUBMIT_HISTORY_RESULT_FILTER_GROUP_LABEL', '제출 결과 선택')}
              >
                <div className="submit-history-header-filter-checks">
                  {submitHistoryJudgeOptions.map((option) => {
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
                <div
                  className="submit-history-plan-filter-bar submit-history-plan-filter-menu-bar"
                  role="group"
                  aria-label={text('SUBMIT_HISTORY_PLAN_FILTER_GROUP_LABEL', '실행계획 요소 선택')}
                >
                  <div
                    className="problem-status-checks submit-history-plan-mode-checks"
                    role="group"
                    aria-label={text('SUBMIT_HISTORY_PLAN_MATCH_MODE_LABEL', '실행계획 요소 검색 방식')}
                  >
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
                  <div
                    className="problem-status-checks submit-history-plan-option-checks"
                    role="group"
                    aria-label={text('SUBMIT_HISTORY_PLAN_SECTION_GROUP_LABEL', '실행계획 요소 선택')}
                  >
                    <label className="problem-status-check submit-history-plan-option-check is-all">
                      <input
                        type="checkbox"
                        checked={allPlanSectionsSelected}
                        onChange={toggleAllDraftPlanSections}
                        className="problem-status-check-input"
                        aria-label={text('RUNTIME_ALL_LABEL', '전체')}
                      />
                      <span className="problem-status-check-text">{text('RUNTIME_ALL_LABEL', '전체')}</span>
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
                    aria-label={text('SUBMIT_HISTORY_PLAN_DETAIL_GROUP_LABEL', '실행계획 요소 상세 선택')}
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
                              <span className="runtime-check-label">{text('RUNTIME_ALL_LABEL', '전체')}</span>
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
                        <span className="runtime-subfilter-label">{text('COMMON_HINT_LABEL', 'Hint')}</span>
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
                            <span className="runtime-check-label">{text('RUNTIME_ALL_LABEL', '전체')}</span>
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
                  <div className="submit-history-header-filter-empty">{text('SUBMIT_HISTORY_PLAN_EMPTY_STATE', '선택한 실행계획 요소가 없습니다.')}</div>
                )}
              </div>
            )}
          </div>,
          document.body,
        );

  return (
    <div className="page page-stack data-page submit-data-page submit-history-page">
      <section className="panel-card compact problem-toolbar-card submit-history-toolbar-card">
        <div className="problem-toolbar submit-history-toolbar-stack">
          <div
            className="solve-dbms-tab-row submit-history-dbms-tab-row"
            role="tablist"
            aria-label={text('SUBMIT_HISTORY_DBMS_TABLIST_LABEL', '제출 목록 DBMS 선택')}
          >
            {submitHistoryDbmsOptions.map((option) => {
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
              label={text('SUBMIT_HISTORY_FAVORITE_LABEL', { dbms: selectedDbmsLabel }, `제출 목록 / ${selectedDbmsLabel}`)}
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
              <div className="problem-search-field home-problem-search-field submit-history-search-field submit-history-search-field-plain">
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
                  placeholder={text('SUBMIT_HISTORY_SUBMIT_ID_PLACEHOLDER', '제출번호')}
                  aria-label={text('SUBMIT_HISTORY_SUBMIT_ID_SEARCH_LABEL', '제출번호 검색')}
                />
                <button
                  type="button"
                  className="submit-history-search-clear-button"
                  aria-label={text('SUBMIT_HISTORY_SUBMIT_ID_CLEAR_LABEL', '제출번호 검색어 지우기')}
                  disabled={!draftFilters.submitId}
                  onClick={() => setDraftFilters((currentFilters) => ({ ...currentFilters, submitId: '' }))}
                >
                  ×
                </button>
              </div>

              <div className="problem-search-field home-problem-search-field submit-history-search-field submit-history-search-field-plain">
                <input
                  type="search"
                  value={draftFilters.query}
                  onChange={(event) =>
                    setDraftFilters((currentFilters) => ({ ...currentFilters, query: event.target.value }))
                  }
                  className="text-field problem-search-input home-problem-search-input submit-history-search-input submit-history-search-input-plain"
                  placeholder={text('ALARM_SEND_HANDLE_PLACEHOLDER', 'Handle 검색')}
                  aria-label={text('SUBMIT_HISTORY_HANDLE_SEARCH_LABEL', 'Handle 검색')}
                />
                <button
                  type="button"
                  className="submit-history-search-clear-button"
                  aria-label={text('SUBMIT_HISTORY_HANDLE_CLEAR_LABEL', 'Handle 검색어 지우기')}
                  disabled={!draftFilters.query}
                  onClick={() => setDraftFilters((currentFilters) => ({ ...currentFilters, query: '' }))}
                >
                  ×
                </button>
              </div>

              <div className="problem-search-field home-problem-search-field submit-history-search-field submit-history-search-field-plain">
                <input
                  type="search"
                  value={draftFilters.problemId}
                  onChange={(event) =>
                    setDraftFilters((currentFilters) => ({ ...currentFilters, problemId: event.target.value }))
                  }
                  className="text-field problem-search-input home-problem-search-input submit-history-search-input submit-history-search-input-plain"
                  placeholder={text('SUBMIT_HISTORY_PROBLEM_ID_PLACEHOLDER', '문제번호')}
                  aria-label={text('SUBMIT_HISTORY_PROBLEM_ID_SEARCH_LABEL', '문제번호 검색')}
                />
                <button
                  type="button"
                  className="submit-history-search-clear-button"
                  aria-label={text('SUBMIT_HISTORY_PROBLEM_ID_CLEAR_LABEL', '문제번호 검색어 지우기')}
                  disabled={!draftFilters.problemId}
                  onClick={() => setDraftFilters((currentFilters) => ({ ...currentFilters, problemId: '' }))}
                >
                  ×
                </button>
              </div>

              <button type="submit" className="submit-history-toolbar-submit" aria-label={text('COMMON_SEARCH_BUTTON', '검색')}>
                {text('COMMON_SEARCH_BUTTON', '검색')}
              </button>
            </div>
          </form>
        </div>
      </section>

      <section className="panel-card problem-board submit-history-board data-board">
        {loadError.failed ? (
          <PageErrorState status={loadError.status} message={loadError.message} />
        ) : historyPage.histories.length === 0 && !isLoading ? (
          <div className="submit-history-board-empty-state" aria-live="polite">
            {text('SUBMIT_HISTORY_EMPTY_STATE', '조건에 맞는 제출 이력이 없습니다.')}
          </div>
        ) : (
          <div className={`submit-history-table-shell ${isLoading ? 'is-loading' : ''}`}>
            <DataTable className="submit-history-table" label={text('SUBMIT_HISTORY_TABLE_LABEL', '제출 이력 목록')}>
              <div className="submit-history-row submit-history-head" role="row">
                <div role="columnheader" className="submit-history-head-cell">{text('SUBMIT_HISTORY_SUBMIT_ID_COLUMN_LABEL', '제출번호')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
                <div role="columnheader" className="submit-history-head-cell">{text('PROBLEM_TABLE_NUMBER_COLUMN_LABEL', '문제 번호')}</div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>{text('SUBMIT_HISTORY_RESULT_TITLE', '제출 결과')}</span>
                  <button
                    type="button"
                    data-submit-history-filter-trigger="true"
                    className={`submit-history-head-filter-trigger ${headerFilterMenuState?.key === 'judge' ? 'is-open' : ''} ${draftFilters.judgeSelections.length !== submitHistoryJudgeOptions.length ? 'is-active' : ''}`}
                    aria-label={text('SUBMIT_HISTORY_RESULT_FILTER_BUTTON_LABEL', '제출 결과 필터 열기')}
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
                    aria-label={
                      draftFilters.costSort === 'asc'
                        ? text('SUBMIT_HISTORY_COST_SORT_ASC_LABEL', 'Cost 오름차순 정렬')
                        : draftFilters.costSort === 'desc'
                          ? text('SUBMIT_HISTORY_COST_SORT_DESC_LABEL', 'Cost 내림차순 정렬')
                          : text('SUBMIT_HISTORY_COST_SORT_NONE_LABEL', 'Cost 정렬 없음')
                    }
                    onClick={toggleCostSortOrder}
                  >
                    <SortIcon direction={draftFilters.costSort === 'asc' ? 'asc' : draftFilters.costSort === 'desc' ? 'desc' : 'none'} />
                  </button>
                </div>
                <div role="columnheader" className="submit-history-head-cell">{text('SUBMIT_HISTORY_SUBMITTED_AT_COLUMN_LABEL', '제출 시각')}</div>
                <div role="columnheader" className="submit-history-head-cell submit-history-head-cell-filter">
                  <span>{text('SUBMIT_HISTORY_PLAN_COLUMN_LABEL', '실행계획요소')}</span>
                  <button
                    type="button"
                    data-submit-history-filter-trigger="true"
                    className={`submit-history-head-filter-trigger ${headerFilterMenuState?.key === 'plan' ? 'is-open' : ''}`}
                    aria-label={text('SUBMIT_HISTORY_PLAN_FILTER_BUTTON_LABEL', '실행계획요소 필터 열기')}
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
              ) : (
                historyPage.histories.map((history) => (
                  <article key={history.submitId} className="submit-history-row submit-history-body" role="row">
                    <span className="submit-history-cell" role="cell" data-label={text('SUBMIT_HISTORY_SUBMIT_ID_COLUMN_LABEL', '제출번호')}>
                      {history.submitId}
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="Handle">
                      <button
                        type="button"
                        className="submit-history-link-button"
                        onClick={() => navigate(getProfilePath(history.handle))}
                        aria-label={text('SUBMIT_HISTORY_HANDLE_PROFILE_MOVE_LABEL', { handle: history.handle }, `${history.handle} 프로필 이동`)}
                      >
                        {history.handle}
                      </button>
                    </span>
                    <span className="submit-history-cell" role="cell" data-label={text('PROBLEM_TABLE_NUMBER_COLUMN_LABEL', '문제 번호')}>
                      <button
                        type="button"
                        className="submit-history-link-button"
                        onClick={() => navigate(`${PROBLEMS_PATH}/${encodeURIComponent(history.problemId)}`)}
                        aria-label={text('SUBMIT_HISTORY_PROBLEM_MOVE_LABEL', { problemId: history.problemId }, `문제 ${history.problemId} 이동`)}
                      >
                        {history.problemId}
                      </button>
                    </span>
                    <span className="submit-history-cell" role="cell" data-label={text('SUBMIT_HISTORY_RESULT_TITLE', '제출 결과')}>
                      <button
                        type="button"
                        className={`submit-history-status-text ${history.success ? 'is-success' : 'is-fail'}`}
                        onClick={() => setModalState({ type: 'sql', history })}
                      >
                        {history.success
                          ? text('SUBMIT_HISTORY_RESULT_CORRECT_LABEL', '정답')
                          : text('SUBMIT_HISTORY_RESULT_WRONG_LABEL', '오답')}
                      </button>
                    </span>
                    <span className="submit-history-cell" role="cell" data-label="Cost">
                      {history.success ? formatCost(history.cost) : '-'}
                    </span>
                    <span className="submit-history-cell" role="cell" data-label={text('SUBMIT_HISTORY_SUBMITTED_AT_COLUMN_LABEL', '제출 시각')}>
                      {formatSubmittedAt(history.submittedAt)}
                    </span>
                    <span className="submit-history-cell submit-history-cell-plan" role="cell" data-label={text('SUBMIT_HISTORY_PLAN_COLUMN_LABEL', '실행계획요소')}>
                      {hasExecutionPlanDetails(history) ? (
                        <button
                          type="button"
                          className="submit-history-detail-button"
                          aria-label={text(
                            'SUBMIT_HISTORY_PLAN_DETAIL_MOVE_LABEL',
                            { label: getPlanElementButtonLabel(history.dbms, history.executionPlanElement) },
                            `${getPlanElementButtonLabel(history.dbms, history.executionPlanElement)} 자세히 보기`,
                          )}
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
            </DataTable>
            {isLoading ? <LoadingOverlay /> : null}
          </div>
        )}

        {!loadError.failed && historyPage.totalCount > 0 ? (
          <Pagination
            className="problem-pagination submit-history-pagination"
            currentPage={historyPage.currentPage}
            totalPages={historyPage.totalPages}
            onPageChange={setRequestedPage}
            ariaLabel={text('SUBMIT_HISTORY_PAGE_NAV_LABEL', '제출 이력 페이지')}
            inputLabel={text('SUBMIT_HISTORY_PAGE_INPUT_LABEL', '이동할 제출 이력 페이지 입력')}
            inputOpenLabel={text('SUBMIT_HISTORY_PAGE_INPUT_BUTTON_LABEL', '이동할 제출 이력 페이지 입력 열기')}
            previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
            nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
          />
        ) : null}
      </section>
      {modalContent}
      {headerFilterMenuContent}
    </div>
  );
}
