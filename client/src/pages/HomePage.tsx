import { useEffect, useMemo, useState } from 'react';
import HandleSetupGate from '../components/home/HandleSetupGate';
import ProblemList from '../components/home/ProblemList';
import ProblemModeSwitch from '../components/home/ProblemModeSwitch';
import ProblemSpreadRateFilter from '../components/home/ProblemSpreadRateFilter';
import ProblemStatusFilter from '../components/home/ProblemStatusFilter';
import { fetchProblems, type ProblemPage } from '../lib/problemApi';
import { useMockSession } from '../lib/session';
import { useHomeSiteTitle } from '../lib/uiText';
import './HomePage.css';

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

type SolvedCountSortOrder = 'desc' | 'asc';
type SpreadRateSortOrder = 'none' | 'desc' | 'asc';
type SolveState = 'all' | 'solved' | 'unsolved' | 'none';
type RangeSelection = { min: number; max: number };
const DEFAULT_SPREAD_RATE_RANGE: RangeSelection = { min: 0, max: 100 };

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

export default function HomePage() {
  useHomeSiteTitle();
  const { isAuthenticated, isReady, userId } = useMockSession();
  const [showStats, setShowStats] = useState(true);
  const [showSolved, setShowSolved] = useState(true);
  const [showUnsolved, setShowUnsolved] = useState(true);
  const [solvedCountSortOrder, setSolvedCountSortOrder] = useState<SolvedCountSortOrder>('desc');
  const [spreadRateSortOrder, setSpreadRateSortOrder] = useState<SpreadRateSortOrder>('none');
  const [draftSearchValue, setDraftSearchValue] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [requestedPage, setRequestedPage] = useState(1);
  const [isPageJumpEditing, setIsPageJumpEditing] = useState(false);
  const [pageJumpDraft, setPageJumpDraft] = useState('1');
  const [problemPage, setProblemPage] = useState<ProblemPage>(createEmptyProblemPage());
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [selectedSpreadRateRange, setSelectedSpreadRateRange] = useState<RangeSelection | null>(DEFAULT_SPREAD_RATE_RANGE);
  const [committedSpreadRateRange, setCommittedSpreadRateRange] = useState<RangeSelection | null>(DEFAULT_SPREAD_RATE_RANGE);

  const canShowSolveState = isReady && isAuthenticated;
  const solveState = canShowSolveState ? resolveSolveState(showSolved, showUnsolved) : 'all';
  const spreadRateSliderMax = Math.max(DEFAULT_SPREAD_RATE_RANGE.max, Math.ceil(problemPage.spreadRateRange.max));
  const spreadRateSliderBounds = useMemo(() => ({ min: DEFAULT_SPREAD_RATE_RANGE.min, max: spreadRateSliderMax }), [spreadRateSliderMax]);
  const visibleSpreadRateRange = selectedSpreadRateRange ?? spreadRateSliderBounds;
  const resolvedVisibleSpreadRateRange = resolveRangeSelection(visibleSpreadRateRange) ?? spreadRateSliderBounds;
  const resolvedSelectedSpreadRateRange = resolveRangeSelection(selectedSpreadRateRange);
  const hasPendingSpreadRateRange = !areSameRange(resolvedSelectedSpreadRateRange, committedSpreadRateRange);

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
    let cancelled = false;

    async function loadProblems() {
      setIsLoading(true);
      setLoadFailed(false);

      try {
        const fetchedProblemPage = await fetchProblems({
          page: requestedPage,
          query: searchQuery,
          solveState,
          solvedCountSort: solvedCountSortOrder,
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
  }, [requestedPage, searchQuery, solveState, solvedCountSortOrder, spreadRateSortOrder, committedSpreadRateRange]);

  const resolvedProblems = useMemo(
    () =>
      problemPage.problems.map((problem) => ({
        ...problem,
        isSolved:
          canShowSolveState && userId != null
            ? (problem.submittedHistories ?? []).some((submittedHistory) => submittedHistory.userId === userId)
            : null,
      })),
    [canShowSolveState, problemPage.problems, userId]
  );

  function applySearch(value: string) {
    setDraftSearchValue(value);
    setSearchQuery(value);
    setCommittedSpreadRateRange((current) => keepRangeIfSame(current, resolvedSelectedSpreadRateRange));
    setRequestedPage(1);
  }

  function toggleSolvedCountSortOrder() {
    setSolvedCountSortOrder((value) => (value === 'asc' ? 'desc' : 'asc'));
    setRequestedPage(1);
  }

  function toggleSpreadRateSortOrder() {
    setSpreadRateSortOrder((value) => {
      if (value === 'none') {
        return 'desc';
      }

      if (value === 'desc') {
        return 'asc';
      }

      return 'none';
    });
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

  if (isLoading) {
    return (
      <div className="page-stack home-page">
        <section className="page-loading-shell" aria-label="Loading problems" aria-busy="true">
          <span className="page-loading-spinner" aria-hidden="true" />
        </section>
        <HandleSetupGate />
      </div>
    );
  }

  return (
    <div className="page-stack home-page">
      <section className="panel-card compact problem-toolbar-card">
        <div className="problem-toolbar">
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
                {'검색'}
              </button>
            </label>
          </form>
        </div>
      </section>

      <section className="panel-card problem-board">
        <div className="problem-board-header">
          <div className="problem-board-controls">
            <ProblemModeSwitch label="통계 표시" checked={showStats} onChange={setShowStats} />

            {canShowSolveState ? (
              <ProblemStatusFilter
                showSolved={showSolved}
                showUnsolved={showUnsolved}
                onToggleSolved={() => {
                  setShowSolved((value) => !value);
                  setRequestedPage(1);
                }}
                onToggleUnsolved={() => {
                  setShowUnsolved((value) => !value);
                  setRequestedPage(1);
                }}
              />
            ) : null}

            <div
              className="problem-control-group problem-sort-group"
              role="group"
              aria-label="푼 사람 정렬"
            >
              <span className="problem-control-label">{'푼 사람'}</span>
              <div className="problem-sort-controls">
                <button
                  type="button"
                  className="problem-sort-toggle-button"
                  aria-label={
                    solvedCountSortOrder === 'asc'
                      ? '푼 사람 오름차순'
                      : '푼 사람 내림차순'
                  }
                  title={
                    solvedCountSortOrder === 'asc'
                      ? '푼 사람 오름차순'
                      : '푼 사람 내림차순'
                  }
                  onClick={toggleSolvedCountSortOrder}
                >
                  {solvedCountSortOrder === 'asc' ? <SortAscendingIcon /> : <SortDescendingIcon />}
                </button>
              </div>
            </div>

            <ProblemSpreadRateFilter
              minBound={spreadRateSliderBounds.min}
              maxBound={spreadRateSliderBounds.max}
              selectedMin={visibleSpreadRateRange.min}
              selectedMax={visibleSpreadRateRange.max}
              displayMin={resolvedVisibleSpreadRateRange.min}
              displayMax={resolvedVisibleSpreadRateRange.max}
              sortOrder={spreadRateSortOrder}
              onToggleSort={toggleSpreadRateSortOrder}
              onChangeMin={updateSpreadRateMin}
              onChangeMax={updateSpreadRateMax}
              onChangeRange={updateSpreadRateRange}
              onApplyRange={applySpreadRateRange}
              hasPendingChanges={hasPendingSpreadRateRange}
            />
          </div>
        </div>

        {loadFailed ? (
          <section className="problem-list is-empty">
            <div className="problem-empty-state">{'문제 목록을 불러오지 못했습니다.'}</div>
          </section>
        ) : (
          <ProblemList
            problems={resolvedProblems}
            showStats={showStats}
            showSolveState={canShowSolveState}
            onSearchSelect={applySearch}
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
              {'이전'}
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
              {'다음'}
            </button>
          </div>
        ) : null}
      </section>

      <div hidden>
        <section className="panel-card disabled-panel">
          <div className="panel-heading-row responsive">
            <div>
              <p className="panel-meta">{'준비 중인 영역'}</p>
              <h2 className="panel-title">NoSQL {'트랙'}</h2>
            </div>
            <span className="section-badge is-disabled">Coming Soon</span>
          </div>
          <p className="content-text">{'문서형 데이터 모델, 샤딩 구조, NoSQL 전용 성능 문제 세트는 다음 단계에서 공개될 예정입니다.'}</p>
        </section>
      </div>

      <HandleSetupGate />
    </div>
  );
}
