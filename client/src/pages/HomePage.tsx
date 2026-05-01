import { useEffect, useMemo, useState } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import HandleSetupGate from '../components/home/HandleSetupGate';
import PageErrorState from '../components/common/PageErrorState';
import Pagination from '../components/common/Pagination';
import { PageToolbar, SearchForm, SegmentedTabs } from '../components/common/PageToolbar';
import ProblemList from '../components/home/ProblemList';
import { replaceQueryState, useLocationSearch } from '../hooks/useLocationState';
import useRequestState from '../hooks/useRequestState';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import { PROBLEMS_PATH } from '../lib/navigation';
import { fetchProblems, type ProblemPage } from '../lib/problemApi';
import { useSession } from '../lib/session';
import { useHomeSiteTitle, useUiText } from '../lib/uiText';
import type { DbmsType } from '../types/domain';
import './HomePage.css';

type SortDirection = 'desc' | 'asc';
type SpreadRateSortOrder = 'none' | 'desc' | 'asc';
type SolveState = 'all' | 'solved' | 'unsolved' | 'none';
type CountSortField = 'solvedCount' | 'totalSubmitCount' | 'successSubmitCount';
type RangeSelection = { min: number; max: number };
interface HomePageFavoriteSnapshot {
  selectedDbms: DbmsType;
  showSolved: boolean;
  showUnsolved: boolean;
  countSortField: CountSortField;
  countSortDirection: SortDirection;
  spreadRateSortOrder: SpreadRateSortOrder;
  draftSearchValue: string;
  searchQuery: string;
  requestedPage: number;
  selectedSpreadRateRange: RangeSelection | null;
  committedSpreadRateRange: RangeSelection | null;
}
const DEFAULT_SPREAD_RATE_RANGE: RangeSelection = { min: 0, max: 100 };
const PROBLEM_PAGE_SIZE = 10;
const dbmsOptions: DbmsType[] = ['postgresql', 'mysql'];

function readProblemsDbmsFromSearch(search: string) {
  const dbms = new URLSearchParams(search).get('dbms');
  return dbms === 'mysql' ? 'mysql' : 'postgresql';
}

function buildProblemsPath(dbms: DbmsType) {
  if (dbms === 'postgresql') {
    return PROBLEMS_PATH;
  }

  return `${PROBLEMS_PATH}?dbms=${encodeURIComponent(dbms)}`;
}

function resolveSolveState(showSolved: boolean, showUnsolved: boolean): SolveState {
  if (showSolved && showUnsolved) {
    return 'all';
  }

  if (showSolved) {
    return 'solved';
  }

  if (showUnsolved) {
    return 'unsolved';
  }

  return 'none';
}

function createEmptyProblemPage(): ProblemPage {
  return {
    currentPage: 1,
    pageSize: PROBLEM_PAGE_SIZE,
    totalCount: 0,
    totalPages: 1,
    spreadRateRange: { min: 0, max: 0 },
    problems: [],
  };
}

function normalizeRangeSelection(range: RangeSelection | null, bounds: RangeSelection): RangeSelection | null {
  if (range == null) {
    return null;
  }

  const min = Math.max(bounds.min, Math.min(range.min, bounds.max));
  const max = Math.max(bounds.min, Math.min(range.max, bounds.max));

  return { min, max };
}

function resolveRangeSelection(range: RangeSelection | null): RangeSelection | null {
  if (range == null) {
    return null;
  }

  return {
    min: Math.min(range.min, range.max),
    max: Math.max(range.min, range.max),
  };
}

function areSameRange(left: RangeSelection | null, right: RangeSelection | null) {
  if (left == null || right == null) {
    return left === right;
  }

  return left.min === right.min && left.max === right.max;
}

function keepRangeIfSame(current: RangeSelection | null, next: RangeSelection | null) {
  return areSameRange(current, next) ? current : next;
}

function toggleRequiredPairSelection(currentChecked: boolean, otherChecked: boolean) {
  if (currentChecked && !otherChecked) {
    return { currentChecked: false, otherChecked: true };
  }

  return { currentChecked: !currentChecked, otherChecked };
}

export default function HomePage() {
  useHomeSiteTitle();
  const { text } = useUiText();
  const { isAuthenticated, isReady, handle } = useSession();
  const locationSearch = useLocationSearch();
  const favoriteRestoreSnapshot = useMemo(() => readFavoriteRestoreSnapshot<HomePageFavoriteSnapshot>('home'), []);
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>(() => favoriteRestoreSnapshot?.selectedDbms ?? readProblemsDbmsFromSearch(window.location.search));
  const [showSolved, setShowSolved] = useState(() => favoriteRestoreSnapshot?.showSolved ?? true);
  const [showUnsolved, setShowUnsolved] = useState(() => favoriteRestoreSnapshot?.showUnsolved ?? true);
  const [countSortField, setCountSortField] = useState<CountSortField>(() => favoriteRestoreSnapshot?.countSortField ?? 'solvedCount');
  const [countSortDirection, setCountSortDirection] = useState<SortDirection>(() => favoriteRestoreSnapshot?.countSortDirection ?? 'desc');
  const [spreadRateSortOrder, setSpreadRateSortOrder] = useState<SpreadRateSortOrder>(() => favoriteRestoreSnapshot?.spreadRateSortOrder ?? 'none');
  const [draftSearchValue, setDraftSearchValue] = useState(() => favoriteRestoreSnapshot?.draftSearchValue ?? '');
  const [searchQuery, setSearchQuery] = useState(() => favoriteRestoreSnapshot?.searchQuery ?? '');
  const [requestedPage, setRequestedPage] = useState(() => favoriteRestoreSnapshot?.requestedPage ?? 1);
  const {
    data: problemPage,
    setData: setProblemPage,
    isLoading,
    setIsLoading,
    error: loadError,
    beginRequest,
    failRequest,
  } = useRequestState<ProblemPage>(createEmptyProblemPage);
  const [selectedSpreadRateRange, setSelectedSpreadRateRange] = useState<RangeSelection | null>(() => favoriteRestoreSnapshot?.selectedSpreadRateRange ?? DEFAULT_SPREAD_RATE_RANGE);
  const [committedSpreadRateRange, setCommittedSpreadRateRange] = useState<RangeSelection | null>(() => favoriteRestoreSnapshot?.committedSpreadRateRange ?? DEFAULT_SPREAD_RATE_RANGE);

  const canShowSolveState = isReady && isAuthenticated;
  const showStats = true;
  const solveState = canShowSolveState ? resolveSolveState(showSolved, showUnsolved) : 'all';
  const spreadRateSliderMax = Math.max(DEFAULT_SPREAD_RATE_RANGE.max, Math.ceil(problemPage.spreadRateRange.max));
  const spreadRateSliderBounds = useMemo(() => ({ min: DEFAULT_SPREAD_RATE_RANGE.min, max: spreadRateSliderMax }), [spreadRateSliderMax]);
  const visibleSpreadRateRange = selectedSpreadRateRange ?? spreadRateSliderBounds;
  const resolvedVisibleSpreadRateRange = resolveRangeSelection(visibleSpreadRateRange) ?? spreadRateSliderBounds;
  const resolvedSelectedSpreadRateRange = resolveRangeSelection(selectedSpreadRateRange);
  const hasPendingSpreadRateRange = !areSameRange(resolvedSelectedSpreadRateRange, committedSpreadRateRange);
  const hasActiveSpreadRateConstraints =
    committedSpreadRateRange != null
      && (committedSpreadRateRange.min > spreadRateSliderBounds.min || committedSpreadRateRange.max < spreadRateSliderBounds.max);
  const isSpreadRateFilterActive = spreadRateSortOrder !== 'none' || hasActiveSpreadRateConstraints;

  useEffect(() => {
    clearFavoriteRestoreSnapshot('home');
  }, []);

  useEffect(() => {
    setSelectedSpreadRateRange((current) => keepRangeIfSame(current, normalizeRangeSelection(current, spreadRateSliderBounds)));
    setCommittedSpreadRateRange((current) =>
      keepRangeIfSame(current, resolveRangeSelection(normalizeRangeSelection(current, spreadRateSliderBounds)))
    );
  }, [spreadRateSliderBounds]);

  useEffect(() => {
    const nextDbms = readProblemsDbmsFromSearch(locationSearch);

    setSelectedDbms((currentDbms) => (currentDbms === nextDbms ? currentDbms : nextDbms));
  }, [locationSearch]);

  useEffect(() => {
    const nextPath = buildProblemsPath(selectedDbms);
    replaceQueryState(nextPath);
  }, [selectedDbms]);

  useEffect(() => {
    let cancelled = false;

    async function loadProblems() {
      beginRequest();

      try {
        const fetchedProblemPage = await fetchProblems({
          page: requestedPage,
          dbms: selectedDbms,
          query: searchQuery,
          solveState,
          solvedCountSort: countSortField === 'solvedCount' ? countSortDirection : 'none',
          totalSubmitSort: countSortField === 'totalSubmitCount' ? countSortDirection : 'none',
          successSubmitSort: countSortField === 'successSubmitCount' ? countSortDirection : 'none',
          spreadRateSort: spreadRateSortOrder,
          spreadRateMin: committedSpreadRateRange?.min ?? null,
          spreadRateMax: committedSpreadRateRange?.max ?? null,
        });

        if (cancelled) {
          return;
        }

        setProblemPage(fetchedProblemPage);
        if (fetchedProblemPage.currentPage !== requestedPage) {
          setRequestedPage(fetchedProblemPage.currentPage);
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

    void loadProblems();

    return () => {
      cancelled = true;
    };
  }, [committedSpreadRateRange, countSortDirection, countSortField, requestedPage, searchQuery, selectedDbms, solveState, spreadRateSortOrder]);

  const resolvedProblems = useMemo(
    () =>
      problemPage.problems.map((problem) => ({
        ...problem,
        isSolved:
          canShowSolveState && handle != null
            ? (problem.submittedHistories ?? []).some((submittedHistory) => submittedHistory.handle === handle)
            : null,
      })),
    [canShowSolveState, problemPage.problems, handle]
  );

  function applySearch(value: string) {
    setDraftSearchValue(value);
    setSearchQuery(value);
    setCommittedSpreadRateRange((current) => keepRangeIfSame(current, resolvedSelectedSpreadRateRange));
    setRequestedPage(1);
  }

  function toggleCountSort(field: CountSortField) {
    if (countSortField === field) {
      setCountSortDirection((value) => (value === 'desc' ? 'asc' : 'desc'));
    } else {
      setCountSortField(field);
      setCountSortDirection('desc');
    }

    setRequestedPage(1);
  }

  function toggleSolvedFilter() {
    const nextSelection = toggleRequiredPairSelection(showSolved, showUnsolved);
    setShowSolved(nextSelection.currentChecked);
    setShowUnsolved(nextSelection.otherChecked);
    setRequestedPage(1);
  }

  function toggleUnsolvedFilter() {
    const nextSelection = toggleRequiredPairSelection(showUnsolved, showSolved);
    setShowUnsolved(nextSelection.currentChecked);
    setShowSolved(nextSelection.otherChecked);
    setRequestedPage(1);
  }

  function toggleSpreadRateSortOrder() {
    setSpreadRateSortOrder((value) => (value === 'desc' ? 'asc' : 'desc'));
    setRequestedPage(1);
  }

  function updateSpreadRateRange(nextRange: RangeSelection) {
    setSelectedSpreadRateRange((current) =>
      keepRangeIfSame(current, normalizeRangeSelection(nextRange, spreadRateSliderBounds))
    );
  }

  function applySpreadRateRange(nextRange: RangeSelection | null = resolvedSelectedSpreadRateRange) {
    setCommittedSpreadRateRange((current) => keepRangeIfSame(current, resolveRangeSelection(nextRange)));
    setRequestedPage(1);
  }

  function updateSpreadRateMin(nextMin: number) {
    const baseRange = visibleSpreadRateRange;
    updateSpreadRateRange({
      min: nextMin,
      max: baseRange.max,
    });
  }

  function updateSpreadRateMax(nextMax: number) {
    const baseRange = visibleSpreadRateRange;
    updateSpreadRateRange({
      min: baseRange.min,
      max: nextMax,
    });
  }

  function requestProblemPage(nextPage: number) {
    const normalizedPage = Math.min(problemPage.totalPages, Math.max(1, nextPage));

    if (normalizedPage === problemPage.currentPage) {
      return;
    }

    setIsLoading(true);
    setRequestedPage(normalizedPage);
  }

  return (
    <div className="page page-stack home-page">
      <section className="panel-card compact problem-toolbar-card">
        <PageToolbar className="problem-toolbar home-problem-toolbar-stack">
          <SegmentedTabs
            className="solve-dbms-tab-row home-problem-dbms-tab-row"
            label={text('HOME_DBMS_TABLIST_LABEL', '문제 목록 DBMS 선택')}
            tabs={dbmsOptions.map((dbmsOption) => ({
              value: dbmsOption,
              label: dbmsOption === 'mysql' ? text('COMMON_MYSQL_LABEL', 'MySQL') : text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL'),
            }))}
            selectedValue={selectedDbms}
            onSelect={(dbmsOption) => {
              setSelectedDbms(dbmsOption);
              setRequestedPage(1);
            }}
            actions={
              <FavoriteTabButton
                className="favorite-tab-toggle-end"
                label={text(
                  'HOME_FAVORITE_LABEL',
                  { dbms: selectedDbms === 'mysql' ? text('COMMON_MYSQL_LABEL', 'MySQL') : text('COMMON_POSTGRESQL_LABEL', 'PostgreSQL') },
                  '문제 / {dbms}',
                )}
                path={buildProblemsPath(selectedDbms)}
                snapshot={{
                  kind: 'home',
                  payload: {
                    selectedDbms,
                    showSolved,
                    showUnsolved,
                    countSortField,
                    countSortDirection,
                    spreadRateSortOrder,
                    draftSearchValue,
                    searchQuery,
                    requestedPage,
                    selectedSpreadRateRange,
                    committedSpreadRateRange,
                  },
                }}
              />
            }
          />

          <SearchForm
            className="problem-search-form home-problem-search-form"
            fieldClassName="problem-search-field home-problem-search-field"
            inputClassName="problem-search-input home-problem-search-input"
            buttonClassName="btn secondary problem-search-button home-problem-search-button"
            value={draftSearchValue}
            onChange={setDraftSearchValue}
            onSubmit={() => applySearch(draftSearchValue)}
            placeholder={text('HOME_PROBLEM_SEARCH_PLACEHOLDER', '문제 번호, 제목 검색')}
            label={text('HOME_PROBLEM_SEARCH_LABEL', '문제 검색')}
            submitLabel={text('COMMON_SEARCH_BUTTON', '검색')}
            buttonLabel={text('COMMON_SEARCH_BUTTON', '검색')}
            withIcon
          />
        </PageToolbar>
      </section>

      <section className="panel-card problem-board data-board">
        {loadError.failed ? (
          <section className="problem-list is-empty">
            <PageErrorState status={loadError.status} message={loadError.message} />
          </section>
        ) : (
          <ProblemList
            problems={resolvedProblems}
            currentDbms={selectedDbms}
            showStats={showStats}
            showSolveState={canShowSolveState}
            showSolved={showSolved}
            showUnsolved={showUnsolved}
            countSortField={countSortField}
            countSortDirection={countSortDirection}
            isLoading={isLoading}
            isSpreadRateFilterActive={isSpreadRateFilterActive}
            spreadRateMinBound={spreadRateSliderBounds.min}
            spreadRateMaxBound={spreadRateSliderBounds.max}
            selectedSpreadRateMin={visibleSpreadRateRange.min}
            selectedSpreadRateMax={visibleSpreadRateRange.max}
            displaySpreadRateMin={resolvedVisibleSpreadRateRange.min}
            displaySpreadRateMax={resolvedVisibleSpreadRateRange.max}
            spreadRateSortOrder={spreadRateSortOrder}
            hasPendingSpreadRateRange={hasPendingSpreadRateRange}
            onSearchSelect={applySearch}
            onToggleSolved={toggleSolvedFilter}
            onToggleUnsolved={toggleUnsolvedFilter}
            onToggleCountSort={toggleCountSort}
            onToggleSpreadRateSort={toggleSpreadRateSortOrder}
            onChangeSpreadRateMin={updateSpreadRateMin}
            onChangeSpreadRateMax={updateSpreadRateMax}
            onChangeSpreadRateRange={updateSpreadRateRange}
            onApplySpreadRateRange={applySpreadRateRange}
          />
        )}

        {!loadError.failed && problemPage.totalCount > 0 ? (
          <Pagination
            currentPage={problemPage.currentPage}
            totalPages={problemPage.totalPages}
            onPageChange={requestProblemPage}
            ariaLabel={text('HOME_PAGE_NAV_LABEL', '문제 페이지')}
            inputLabel={text('HOME_PAGE_INPUT_LABEL', '이동할 페이지 입력')}
            inputOpenLabel={text('HOME_PAGE_INPUT_OPEN_LABEL', '이동할 페이지 입력 열기')}
            previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
            nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
          />
        ) : null}
      </section>

      <HandleSetupGate />
    </div>
  );
}
