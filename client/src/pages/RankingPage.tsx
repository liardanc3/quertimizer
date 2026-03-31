import { useState } from 'react';
import { useMockSession } from '../lib/session';
import { mockCurrentProfile } from '../mocks/profile';
import { mockRanking } from '../mocks/ranking';
import type { DbmsType, RankingEntry, RankingMetricKey } from '../types/domain';

const PAGE_SIZE = 100;

type RankedEntry = RankingEntry & { rank: number };

const dbmsOptions: Array<{ id: DbmsType; label: string }> = [
  { id: 'postgresql', label: 'PostgreSQL' },
  { id: 'oracle', label: 'Oracle' },
];

const sortOptions: Array<{
  id: RankingMetricKey;
  label: string;
  description: string;
}> = [
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
  {
    id: 'avgScanRowsPercentile',
    label: '평균 스캔 행 수 백분위',
    description: '낮을수록 더 적은 행을 읽은 상위권 풀이입니다.',
  },
];

function compareRankingEntries(sortKey: RankingMetricKey, left: RankingEntry, right: RankingEntry) {
  if (sortKey === 'solvedCount') {
    return (
      right.solvedCount - left.solvedCount ||
      left.avgExecutionPercentile - right.avgExecutionPercentile ||
      left.avgScanRowsPercentile - right.avgScanRowsPercentile ||
      left.handle.localeCompare(right.handle)
    );
  }

  if (sortKey === 'avgExecutionPercentile') {
    return (
      left.avgExecutionPercentile - right.avgExecutionPercentile ||
      right.solvedCount - left.solvedCount ||
      left.avgScanRowsPercentile - right.avgScanRowsPercentile ||
      left.handle.localeCompare(right.handle)
    );
  }

  return (
    left.avgScanRowsPercentile - right.avgScanRowsPercentile ||
    right.solvedCount - left.solvedCount ||
    left.avgExecutionPercentile - right.avgExecutionPercentile ||
    left.handle.localeCompare(right.handle)
  );
}

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

export default function RankingPage() {
  const { isAuthenticated } = useMockSession();
  const [selectedDbms, setSelectedDbms] = useState<DbmsType>('postgresql');
  const [sortKey, setSortKey] = useState<RankingMetricKey>('solvedCount');
  const [draftIdQuery, setDraftIdQuery] = useState('');
  const [submittedIdQuery, setSubmittedIdQuery] = useState('');
  const [requestedPage, setRequestedPage] = useState(1);

  const activeDbmsLabel = dbmsOptions.find((option) => option.id === selectedDbms)?.label ?? selectedDbms;
  const normalizedIdQuery = submittedIdQuery.trim().toLowerCase().replace(/^@/, '');

  const rankedEntries: RankedEntry[] = mockRanking[selectedDbms]
    .slice()
    .sort((left, right) => compareRankingEntries(sortKey, left, right))
    .map((entry, index) => ({
      ...entry,
      rank: index + 1,
    }));

  const filteredEntries = rankedEntries.filter((entry) =>
    normalizedIdQuery ? entry.handle.toLowerCase().includes(normalizedIdQuery) : true
  );

  const totalPages = Math.max(1, Math.ceil(filteredEntries.length / PAGE_SIZE));
  const currentPage = Math.min(requestedPage, totalPages);
  const pagedEntries = filteredEntries.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  function applyIdSearch() {
    setSubmittedIdQuery(draftIdQuery);
    setRequestedPage(1);
  }

  return (
    <div className="page-stack">
      <section className="panel-card">
        <div className="ranking-toolbar">
          <div className="ranking-toolbar-line">
            <div className="ranking-control-block ranking-control-block-fixed">
              <span className="ranking-control-label">DBMS 선택</span>
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

        {pagedEntries.length > 0 ? (
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
              <span role="columnheader">평균 스캔 행 수 백분위</span>
            </div>

            {pagedEntries.map((entry) => {
              const isCurrentUser = isAuthenticated && entry.handle === mockCurrentProfile.handle;
              const delta = getDeltaMeta(entry.monthlyRankDelta[sortKey]);

              return (
                <article key={`${selectedDbms}-${entry.handle}`} className={`ranking-row ${isCurrentUser ? 'is-highlight' : ''}`}>
                  <div className="ranking-cell ranking-change-cell" data-label="이번달 1일 대비">
                    <span className={`ranking-change-badge ${delta.className}`}>{delta.label}</span>
                  </div>

                  <div className="ranking-cell ranking-rank-cell" data-label="순위">
                    <span className={`ranking-rank-badge ${entry.rank <= 3 ? 'is-top' : ''}`}>{entry.rank}</span>
                  </div>

                  <div className="ranking-cell ranking-user-cell" data-label="사용자">
                    <div className="ranking-user-copy">
                      <strong>{entry.name}</strong>
                    </div>
                    <span className="subtle-chip">{entry.tier}</span>
                  </div>

                  <div className="ranking-cell" data-label="해결한 문제">
                    <strong>{entry.solvedCount.toLocaleString('ko-KR')}문제</strong>
                  </div>

                  <div className="ranking-cell" data-label="평균 실행시간 백분위">
                    <strong>{formatPercent(entry.avgExecutionPercentile)}</strong>
                  </div>

                  <div className="ranking-cell" data-label="평균 스캔 행 수 백분위">
                    <strong>{formatPercent(entry.avgScanRowsPercentile)}</strong>
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <div className="problem-empty-state">검색된 ID가 없습니다.</div>
        )}

        {totalPages > 1 && pagedEntries.length > 0 ? (
          <div className="problem-pagination" role="navigation" aria-label="랭킹 페이지">
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
  );
}
