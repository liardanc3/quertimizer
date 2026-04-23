import { useEffect, useMemo, useState, useSyncExternalStore } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import HandleSetupGate from '../components/home/HandleSetupGate';
import ProblemList from '../components/home/ProblemList';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { clearFavoriteRestoreSnapshot, readFavoriteRestoreSnapshot } from '../lib/favoriteTabs';
import { getLocationSearchSnapshot, PROBLEMS_PATH, subscribeLocation } from '../lib/navigation';
import { fetchProblems, type ProblemPage } from '../lib/problemApi';
import { useMockSession } from '../lib/session';
import { useHomeSiteTitle } from '../lib/uiText';
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
const dbmsOptions: Array<{ value: DbmsType; label: string }> = [
  { value: 'postgresql', label: 'PostgreSQL' },
  { value: 'oracle', label: 'Oracle' },
];

function readProblemsDbmsFromSearch(search: string) {
  const dbms = new URLSearchParams(search).get('dbms');
  return dbms === 'oracle' ? 'oracle' : 'postgresql';
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
    pageSize: 20,
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
  const { isAuthenticated, isReady, handle } = useMockSession();
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
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
  const [isPageJumpEditing, setIsPageJumpEditing] = useState(false);
  const [pageJumpDraft, setPageJumpDraft] = useState('1');
  const [problemPage, setProblemPage] = useState<ProblemPage>(createEmptyProblemPage());
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
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
    if (isPageJumpEditing) {
      return;
    }

    setPageJumpDraft(String(problemPage.currentPage));
  }, [isPageJumpEditing, problemPage.currentPage]);

  useEffect(() => {
    const nextDbms = readProblemsDbmsFromSearch(locationSearch);

    setSelectedDbms((currentDbms) => (currentDbms === nextDbms ? currentDbms : nextDbms));
  }, [locationSearch]);

  useEffect(() => {
    const nextPath = buildProblemsPath(selectedDbms);
    const currentPath = `${window.location.pathname}${window.location.search}`;

    if (currentPath !== nextPath) {
      window.history.replaceState(window.history.state ?? {}, '', nextPath);
    }
  }, [selectedDbms]);

  useEffect(() => {
    let cancelled = false;

    async function loadProblems() {
      setIsLoading(true);
      setLoadFailed(false);

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

  function applyPageJump() {
    const parsedPage = Number.parseInt(pageJumpDraft, 10);
    const nextPage = Number.isNaN(parsedPage)
      ? problemPage.currentPage
      : Math.min(problemPage.totalPages, Math.max(1, parsedPage));

    setPageJumpDraft(String(nextPage));
    setIsPageJumpEditing(false);

    if (nextPage !== problemPage.currentPage) {
      setRequestedPage(nextPage);
    }
  }

  function cancelPageJump() {
    setPageJumpDraft(String(problemPage.currentPage));
    setIsPageJumpEditing(false);
  }

  return (
    <div className="page-stack home-page">
      <section className="panel-card compact problem-toolbar-card">
        <div className="problem-toolbar home-problem-toolbar-stack">
          <div className="solve-dbms-tab-row home-problem-dbms-tab-row" role="tablist" aria-label="문제 목록 DBMS 선택">
            {dbmsOptions.map((option) => {
              const isSelected = option.value === selectedDbms;

              return (
                <button
                  key={option.value}
                  type="button"
                  className={`solve-dbms-tab ${isSelected ? 'is-selected' : ''}`}
                  role="tab"
                  aria-selected={isSelected}
                  onClick={() => {
                    if (!isSelected) {
                      setSelectedDbms(option.value);
                      setRequestedPage(1);
                    }
                  }}
                >
                  {option.label}
                </button>
              );
            })}
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={`문제 / ${selectedDbms === 'oracle' ? 'Oracle' : 'PostgreSQL'}`}
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
          </div>

          <form
            className="problem-search-form home-problem-search-form"
            onSubmit={(event) => {
              event.preventDefault();
              applySearch(draftSearchValue);
            }}
          >
            <label className="problem-search-field home-problem-search-field">
              <span className="problem-search-icon" aria-hidden="true">
                ⌕
              </span>
              <input
                type="search"
                value={draftSearchValue}
                onChange={(event) => setDraftSearchValue(event.target.value)}
                className="text-field problem-search-input home-problem-search-input"
                placeholder="문제 번호, 제목 검색"
                aria-label="문제 검색"
              />

              <button
                type="submit"
                className="btn secondary problem-search-button home-problem-search-button"
                aria-label="검색"
              >
                검색
              </button>
            </label>
          </form>
        </div>
      </section>

      <section className="panel-card problem-board">
        {loadFailed ? (
          <section className="problem-list is-empty">
            <PageLoadFailureState className="problem-empty-state" />
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

        {!loadFailed && problemPage.totalCount > 0 ? (
          <div className="problem-pagination" role="navigation" aria-label="문제 페이지">
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setRequestedPage((page) => Math.max(1, page - 1))}
              disabled={problemPage.currentPage === 1}
            >
              이전
            </button>

            {isPageJumpEditing ? (
              <input
                type="text"
                inputMode="numeric"
                className="problem-pagination-meta-input"
                aria-label="이동할 페이지 입력"
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
                aria-label="이동할 페이지 입력 열기"
                onClick={() => {
                  setPageJumpDraft(String(problemPage.currentPage));
                  setIsPageJumpEditing(true);
                }}
              >
                {`${problemPage.currentPage} / ${problemPage.totalPages}`}
              </button>
            )}

            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setRequestedPage((page) => Math.min(problemPage.totalPages, page + 1))}
              disabled={problemPage.currentPage === problemPage.totalPages}
            >
              다음
            </button>
          </div>
        ) : null}
      </section>

      <div hidden>
        <section className="panel-card disabled-panel">
          <div className="panel-heading-row responsive">
            <div>
              <p className="panel-meta">준비 중인 영역</p>
              <h2 className="panel-title">NoSQL 트랙</h2>
            </div>
            <span className="section-badge is-disabled">Coming Soon</span>
          </div>
          <p className="content-text">문서형 데이터 모델, 샤딩 구조, NoSQL 전용 성능 문제 세트는 다음 단계에서 공개될 예정입니다.</p>
        </section>
      </div>

      <HandleSetupGate />
    </div>
  );
}
