import { useState } from 'react';
import DomainTabs from '../components/home/DomainTabs';
import ProblemList from '../components/home/ProblemList';
import ProblemModeSwitch from '../components/home/ProblemModeSwitch';
import ProblemStatusFilter from '../components/home/ProblemStatusFilter';
import { mockProblems } from '../mocks/problems';
import type { DomainType, ProblemSummary, RuntimeDistribution } from '../types/domain';

const PAGE_SIZE = 10;
type SolvedCountSortOrder = 'desc' | 'asc';

function getSearchableDistributions(problem: ProblemSummary): RuntimeDistribution[] {
  if (problem.runtimeDistributions) {
    return Object.values(problem.runtimeDistributions).filter((distribution): distribution is RuntimeDistribution =>
      Boolean(distribution)
    );
  }

  return problem.runtimeDistribution ? [problem.runtimeDistribution] : [];
}

function matchesSearch(problem: ProblemSummary, query: string) {
  if (!query) {
    return true;
  }

  return (
    String(problem.number).includes(query) ||
    problem.title.toLowerCase().includes(query) ||
    problem.tags.some((tag) => tag.toLowerCase().includes(query)) ||
    getSearchableDistributions(problem).some(
      (distribution) =>
        distribution.fastestNickname.toLowerCase().includes(query) ||
        distribution.samples.some((sample) => sample.nickname.toLowerCase().includes(query))
    )
  );
}

function matchesSolvedState(problem: ProblemSummary, showSolved: boolean, showUnsolved: boolean) {
  if (problem.solvedAt) {
    return showSolved;
  }

  return showUnsolved;
}

export default function HomePage() {
  const [domain, setDomain] = useState<DomainType>('rdbms');
  const [showTags, setShowTags] = useState(true);
  const [showStats, setShowStats] = useState(true);
  const [showSolved, setShowSolved] = useState(true);
  const [showUnsolved, setShowUnsolved] = useState(true);
  const [solvedCountSortOrder, setSolvedCountSortOrder] = useState<SolvedCountSortOrder>('desc');
  const [draftSearchValue, setDraftSearchValue] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [requestedPage, setRequestedPage] = useState(1);

  const applySearch = (value: string) => {
    setDraftSearchValue(value);
    setSearchQuery(value);
    setRequestedPage(1);
  };

  const normalizedSearchValue = searchQuery.trim().toLowerCase();
  const filteredProblems = mockProblems
    .filter(
      (problem) =>
        problem.domain === domain &&
        matchesSearch(problem, normalizedSearchValue) &&
        matchesSolvedState(problem, showSolved, showUnsolved)
    )
    .sort((left, right) => {
      const solvedCountGap =
        solvedCountSortOrder === 'asc' ? left.solvedCount - right.solvedCount : right.solvedCount - left.solvedCount;

      if (solvedCountGap !== 0) {
        return solvedCountGap;
      }

      return left.number - right.number;
    });
  const totalPages = Math.max(1, Math.ceil(filteredProblems.length / PAGE_SIZE));
  const currentPage = Math.min(requestedPage, totalPages);
  const pagedProblems = filteredProblems.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  return (
    <div className="page-stack">
      <section className="panel-card compact problem-toolbar-card">
        <div className="problem-toolbar">
          <DomainTabs
            selectedDomain={domain}
            onChange={(nextDomain) => {
              setDomain(nextDomain);
              setRequestedPage(1);
            }}
          />

          <form
            className="problem-search-form"
            onSubmit={(event) => {
              event.preventDefault();
              applySearch(draftSearchValue);
            }}
          >
            <label className="problem-search-field">
              <span className="problem-search-icon" aria-hidden="true">
                ⌕
              </span>
              <input
                type="search"
                value={draftSearchValue}
                onChange={(event) => setDraftSearchValue(event.target.value)}
                className="text-field problem-search-input"
                placeholder="문제 번호, 제목, 태그, 닉네임 검색"
                aria-label="문제 검색"
              />
            </label>

            <button type="submit" className="btn secondary problem-search-button">
              검색
            </button>
          </form>
        </div>
      </section>

      <div id="panel-rdbms" role="tabpanel" aria-labelledby="tab-rdbms" hidden={domain !== 'rdbms'}>
        <section className="panel-card problem-board">
          <div className="problem-board-header">
            <div className="problem-board-controls">
              <ProblemModeSwitch label="태그 표시" checked={showTags} onChange={setShowTags} />
              <ProblemModeSwitch label="통계 표시" checked={showStats} onChange={setShowStats} />
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
              <div className="problem-control-group problem-sort-group" role="group" aria-label="푼 사람 수 정렬">
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

          <ProblemList problems={pagedProblems} showTags={showTags} showStats={showStats} onSearchSelect={applySearch} />

          {filteredProblems.length > 0 ? (
            <div className="problem-pagination" role="navigation" aria-label="문제 페이지">
              <button
                type="button"
                className="mini-toggle problem-page-button"
                onClick={() => setRequestedPage((page) => Math.max(1, page - 1))}
                disabled={currentPage === 1}
              >
                이전
              </button>

              <div className="problem-page-numbers">
                {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
                  <button
                    key={page}
                    type="button"
                    className={`mini-toggle problem-page-button ${page === currentPage ? 'is-selected' : ''}`}
                    aria-current={page === currentPage ? 'page' : undefined}
                    onClick={() => setRequestedPage(page)}
                  >
                    {page}
                  </button>
                ))}
              </div>

              <button
                type="button"
                className="mini-toggle problem-page-button"
                onClick={() => setRequestedPage((page) => Math.min(totalPages, page + 1))}
                disabled={currentPage === totalPages}
              >
                다음
              </button>

              <span className="problem-pagination-meta">
                {currentPage} / {totalPages}
              </span>
            </div>
          ) : null}
        </section>
      </div>

      <div id="panel-nosql" role="tabpanel" aria-labelledby="tab-nosql" hidden={domain !== 'nosql'}>
        <section className="panel-card disabled-panel">
          <div className="panel-heading-row responsive">
            <div>
              <p className="panel-meta">준비 중인 영역</p>
              <h2 className="panel-title">NoSQL 트랙</h2>
            </div>
            <span className="section-badge is-disabled">Coming Soon</span>
          </div>
          <p className="content-text">
            문서형 데이터 모델, 샤딩 구조, NoSQL 전용 성능 문제 세트는 다음 단계에서 공개할 예정입니다.
          </p>
        </section>
      </div>
    </div>
  );
}
