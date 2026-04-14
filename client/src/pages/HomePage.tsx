import { useEffect, useMemo, useState } from 'react';
import ProblemList from '../components/home/ProblemList';
import ProblemModeSwitch from '../components/home/ProblemModeSwitch';
import ProblemStatusFilter from '../components/home/ProblemStatusFilter';
import { fetchProblems, type ProblemPage } from '../lib/problemApi';
import { useMockSession } from '../lib/session';
import { useHomeSiteTitle } from '../lib/uiText';

type SolvedCountSortOrder = 'desc' | 'asc';
type SolveState = 'all' | 'solved' | 'unsolved' | 'none';

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
    problems: [],
  };
}

export default function HomePage() {
  useHomeSiteTitle();
  const { isAuthenticated, isReady, userId } = useMockSession();
  const [showStats, setShowStats] = useState(true);
  const [showSolved, setShowSolved] = useState(true);
  const [showUnsolved, setShowUnsolved] = useState(true);
  const [solvedCountSortOrder, setSolvedCountSortOrder] = useState<SolvedCountSortOrder>('desc');
  const [draftSearchValue, setDraftSearchValue] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [requestedPage, setRequestedPage] = useState(1);
  const [problemPage, setProblemPage] = useState<ProblemPage>(createEmptyProblemPage());
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);

  const canShowSolveState = isReady && isAuthenticated;
  const solveState = canShowSolveState ? resolveSolveState(showSolved, showUnsolved) : 'all';

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
  }, [requestedPage, searchQuery, solveState, solvedCountSortOrder]);

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
    setRequestedPage(1);
  }

  return (
    <div className="page-stack">
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
                🔎
              </span>
              <input
                type="search"
                value={draftSearchValue}
                onChange={(event) => setDraftSearchValue(event.target.value)}
                className="text-field problem-search-input home-problem-search-input"
                placeholder="문제 번호, 제목, 유저 ID 검색"
                aria-label="문제 검색"
              />

              <button type="submit" className="btn secondary problem-search-button home-problem-search-button" aria-label="검색">
                검색
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

              <div className="problem-control-group problem-sort-group" role="group" aria-label="푼 사람 정렬">
                <span className="problem-control-label">푼 사람</span>
                <div className="problem-status-buttons">
                  <button
                    type="button"
                    className={`mini-toggle problem-status-button ${solvedCountSortOrder === 'asc' ? 'is-selected' : ''}`}
                    aria-pressed={solvedCountSortOrder === 'asc'}
                    onClick={() => {
                      setSolvedCountSortOrder('asc');
                      setRequestedPage(1);
                    }}
                  >
                    오름차순
                  </button>
                  <button
                    type="button"
                    className={`mini-toggle problem-status-button ${solvedCountSortOrder === 'desc' ? 'is-selected' : ''}`}
                    aria-pressed={solvedCountSortOrder === 'desc'}
                    onClick={() => {
                      setSolvedCountSortOrder('desc');
                      setRequestedPage(1);
                    }}
                  >
                    내림차순
                  </button>
                </div>
              </div>
            </div>
          </div>

          {isLoading ? (
            <section className="problem-list is-empty">
              <div className="problem-empty-state">문제 목록을 불러오는 중입니다.</div>
            </section>
          ) : loadFailed ? (
            <section className="problem-list is-empty">
              <div className="problem-empty-state">문제 목록을 불러오지 못했습니다.</div>
            </section>
          ) : (
            <ProblemList
              problems={resolvedProblems}
              showStats={showStats}
              showSolveState={canShowSolveState}
              onSearchSelect={applySearch}
            />
          )}

          {!isLoading && !loadFailed && problemPage.totalCount > 0 ? (
            <div className="problem-pagination" role="navigation" aria-label="문제 페이지">
              <button
                type="button"
                className="mini-toggle problem-page-button"
                onClick={() => setRequestedPage((page) => Math.max(1, page - 1))}
                disabled={problemPage.currentPage === 1}
              >
                이전
              </button>

              <div className="problem-page-numbers">
                {Array.from({ length: problemPage.totalPages }, (_, index) => index + 1).map((page) => (
                  <button
                    key={page}
                    type="button"
                    className={`mini-toggle problem-page-button ${page === problemPage.currentPage ? 'is-selected' : ''}`}
                    aria-current={page === problemPage.currentPage ? 'page' : undefined}
                    onClick={() => setRequestedPage(page)}
                  >
                    {page}
                  </button>
                ))}
              </div>

              <button
                type="button"
                className="mini-toggle problem-page-button"
                onClick={() => setRequestedPage((page) => Math.min(problemPage.totalPages, page + 1))}
                disabled={problemPage.currentPage === problemPage.totalPages}
              >
                다음
              </button>

              <span className="problem-pagination-meta">
                {problemPage.currentPage} / {problemPage.totalPages}
              </span>
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
          <p className="content-text">
            문서형 데이터 모델, 샤딩 구조, NoSQL 전용 성능 문제 세트는 다음 단계에서 공개될 예정입니다.
          </p>
        </section>
      </div>
    </div>
  );
}
