import { useEffect, useMemo, useState } from 'react';
import { getProfilePath, navigate } from '../lib/navigation';
import { fetchRanks, type RankPage } from '../lib/rankApi';
import { useMockSession } from '../lib/session';
import type { DbmsType, RankingMetricKey } from '../types/domain';
import './RankingPage.css';

const PAGE_SIZE = 100;

const dbmsOptions: Array<{ id: DbmsType; label: string }> = [
  { id: 'postgresql', label: 'PostgreSQL' },
  { id: 'oracle', label: 'Oracle' },
];

const sortOptions: Array<{ id: RankingMetricKey; label: string; description: string }> = [
  {
    id: 'solvedCount',
    label: '푼 문제 수',
    description: '많을수록 상위로 정렬됩니다.',
  },
  {
    id: 'avgExecutionPercentile',
    label: '평균 실행시간 백분위',
    description: '낮을수록 더 빠른 상위권 풀이입니다.',
  },
];

function formatPercent(value: number) {
  return `${value.toFixed(1)}%`;
}

function getDeltaMeta(value: number) {
  if (value > 0) {
    return {
      label: `▲ ${value}`,
      className: 'is-up',
    };
  }

  if (value < 0) {
    return {
      label: `▼ ${Math.abs(value)}`,
      className: 'is-down',
    };
  }

  return {
    label: '-',
    className: 'is-flat',
  };
}

function createEmptyRankPage(): RankPage {
  return {
    currentPage: 1,
    pageSize: PAGE_SIZE,
    totalCount: 0,
    totalPages: 1,
    ranks: [],
  };
}

export default function RankingPage() {
  const { isAuthenticated, userId } = useMockSession();
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>('postgresql');
  const [sortKey, setSortKey] = useState<RankingMetricKey>('solvedCount');
  const [draftIdQuery, setDraftIdQuery] = useState('');
  const [submittedIdQuery, setSubmittedIdQuery] = useState('');
  const [requestedPage, setRequestedPage] = useState(1);
  const [rankPage, setRankPage] = useState<RankPage>(createEmptyRankPage());
  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);

  const activeDbmsLabel = dbmsOptions.find((option) => option.id === selectedDbms)?.label ?? selectedDbms;
  const rankedEntries = useMemo(
    () =>
      rankPage.ranks.map((entry, index) => ({
        ...entry,
        rank: (rankPage.currentPage - 1) * rankPage.pageSize + index + 1,
      })),
    [rankPage],
  );

  useEffect(() => {
    let cancelled = false;

    async function loadRanks() {
      setIsLoading(true);
      setLoadFailed(false);

      try {
        const fetchedRankPage = await fetchRanks({
          page: requestedPage,
          dbms: selectedDbms,
          query: submittedIdQuery,
          sortKey,
        });

        if (cancelled) {
          return;
        }

        setRankPage(fetchedRankPage);
        if (fetchedRankPage.currentPage !== requestedPage) {
          setRequestedPage(fetchedRankPage.currentPage);
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

    void loadRanks();

    return () => {
      cancelled = true;
    };
  }, [requestedPage, selectedDbms, sortKey, submittedIdQuery]);

  function applyIdSearch() {
    setSubmittedIdQuery(draftIdQuery);
    setRequestedPage(1);
  }

  return (
    <div className="page-stack ranking-page">
      <section className="panel-card">
        <div className="ranking-toolbar">
          <div className="ranking-toolbar-line">
            <div className="ranking-control-block ranking-control-block-fixed">
              <span className="ranking-control-label">DBMS</span>
              <div className="segmented" role="group" aria-label="리더보드 DBMS 선택">
                {dbmsOptions.map((option) => {
                  const isSelected = option.id === selectedDbms;

                  return (
                    <button
                      key={option.id}
                      type="button"
                      className={`segmented-btn ${isSelected ? 'is-selected' : ''}`}
                      aria-pressed={isSelected}
                      onClick={() => {
                        setSelectedDbms(option.id);
                        setRequestedPage(1);
                      }}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="ranking-control-block">
              <span className="ranking-control-label">정렬 기준</span>
              <div className="segmented" role="group" aria-label="리더보드 정렬 기준 선택">
                {sortOptions.map((option) => {
                  const isSelected = option.id === sortKey;

                  return (
                    <button
                      key={option.id}
                      type="button"
                      className={`segmented-btn ranking-sort-button ${isSelected ? 'is-selected' : ''}`}
                      aria-pressed={isSelected}
                      title={option.description}
                      onClick={() => {
                        setSortKey(option.id);
                        setRequestedPage(1);
                      }}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <form
              className="ranking-control-block ranking-control-block-fixed ranking-search-form"
              onSubmit={(event) => {
                event.preventDefault();
                applyIdSearch();
              }}
            >
              <span className="ranking-control-label">ID 검색</span>
              <div className="ranking-search-shell">
                <input
                  type="search"
                  value={draftIdQuery}
                  onChange={(event) => setDraftIdQuery(event.target.value)}
                  className="text-field ranking-search-input"
                  placeholder="ID 검색"
                  aria-label="리더보드 ID 검색"
                />
                <button type="submit" className="ranking-search-button" aria-label="ID 검색 실행" title="ID 검색">
                  <span aria-hidden="true">⌕</span>
                </button>
              </div>
            </form>
          </div>
        </div>

        {isLoading || rankedEntries.length > 0 ? (
          <div className="ranking-table" role="table" aria-label={`${activeDbmsLabel} 리더보드`}>
            <div className="ranking-row ranking-head" role="row">
              <span role="columnheader" className="ranking-change-heading">
                <span>이번달 1일 대비</span>
                <span className="tooltip-anchor ranking-tooltip-anchor">
                  <button type="button" className="ranking-tooltip-button" aria-label="이번달 1일 대비 설명">
                    ?
                  </button>
                  <span className="ui-tooltip is-passive ranking-change-tooltip" role="tooltip">
                    선택한 정렬 기준에서 이번달 1일 기준 순위와 비교한 상승 또는 하락 폭입니다.
                  </span>
                </span>
              </span>
              <span role="columnheader" className="ranking-rank-heading">
                순위
              </span>
              <span role="columnheader">사용자</span>
              <span role="columnheader">해결한 문제</span>
              <span role="columnheader">평균 실행시간 백분위</span>
            </div>

            {isLoading ? (
              <div className="ranking-row ranking-empty-row" role="row">
                <span className="ranking-empty-cell" role="cell">
                  데이터 로딩중
                </span>
              </div>
            ) : (
              rankedEntries.map((entry) => {
                const isCurrentUser = isAuthenticated && entry.userId === userId;
                const delta = getDeltaMeta(entry.monthlyRankDelta[sortKey]);

                return (
                  <article key={`${selectedDbms}-${entry.userId}`} className={`ranking-row ${isCurrentUser ? 'is-highlight' : ''}`}>
                    <div className="ranking-cell ranking-change-cell" data-label="이번달 1일 대비">
                      <span className={`ranking-change-badge ${delta.className}`}>{delta.label}</span>
                    </div>

                    <div className="ranking-cell ranking-rank-cell" data-label="순위">
                      <span className={`ranking-rank-badge ${entry.rank <= 3 ? 'is-top' : ''}`}>{entry.rank}</span>
                    </div>

                    <div className="ranking-cell ranking-user-cell" data-label="사용자">
                      <button
                        type="button"
                        className="ranking-profile-trigger"
                        onClick={() => navigate(getProfilePath(entry.userId))}
                      >
                        <strong>{entry.userId}</strong>
                      </button>
                    </div>

                    <div className="ranking-cell" data-label="해결한 문제">
                      <strong>{entry.solvedCount.toLocaleString('ko-KR')}문제</strong>
                    </div>

                    <div className="ranking-cell" data-label="평균 실행시간 백분위">
                      <strong>{formatPercent(entry.avgExecutionPercentile)}</strong>
                    </div>
                  </article>
                );
              })
            )}
          </div>
        ) : loadFailed ? (
          <div className="problem-empty-state">랭킹을 불러오지 못했다.</div>
        ) : (
          <div className="problem-empty-state">검색된 ID가 없습니다.</div>
        )}

        {!isLoading && !loadFailed && rankPage.totalPages > 1 && rankedEntries.length > 0 ? (
          <div className="problem-pagination" role="navigation" aria-label="랭킹 페이지">
            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setRequestedPage((page) => Math.max(1, page - 1))}
              disabled={rankPage.currentPage === 1}
            >
              이전
            </button>

            <div className="problem-page-numbers">
              {Array.from({ length: rankPage.totalPages }, (_, index) => index + 1).map((page) => (
                <button
                  key={page}
                  type="button"
                  className={`mini-toggle problem-page-button ${page === rankPage.currentPage ? 'is-selected' : ''}`}
                  aria-current={page === rankPage.currentPage ? 'page' : undefined}
                  onClick={() => setRequestedPage(page)}
                >
                  {page}
                </button>
              ))}
            </div>

            <button
              type="button"
              className="mini-toggle problem-page-button"
              onClick={() => setRequestedPage((page) => Math.min(rankPage.totalPages, page + 1))}
              disabled={rankPage.currentPage === rankPage.totalPages}
            >
              다음
            </button>

            <span className="problem-pagination-meta">
              {rankPage.currentPage} / {rankPage.totalPages}
            </span>
          </div>
        ) : null}
      </section>
    </div>
  );
}
