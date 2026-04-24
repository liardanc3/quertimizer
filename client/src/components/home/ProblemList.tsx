import { useEffect, useRef, useState } from 'react';
import { navigate } from '../../lib/navigation';
import type { DbmsType, ProblemSummary } from '../../types/domain';
import ProblemCard from './ProblemCard';
import ProblemSpreadRateFilter from './ProblemSpreadRateFilter';

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

type CountSortField = 'solvedCount' | 'totalSubmitCount' | 'successSubmitCount';

type RangeSelection = { min: number; max: number };

type ProblemListProps = {
  problems: ProblemSummary[];
  currentDbms: DbmsType;
  showStats: boolean;
  showSolveState: boolean;
  showSolved: boolean;
  showUnsolved: boolean;
  countSortField: CountSortField;
  countSortDirection: 'desc' | 'asc';
  isLoading: boolean;
  isSpreadRateFilterActive: boolean;
  spreadRateMinBound: number;
  spreadRateMaxBound: number;
  selectedSpreadRateMin: number;
  selectedSpreadRateMax: number;
  displaySpreadRateMin: number;
  displaySpreadRateMax: number;
  spreadRateSortOrder: 'none' | 'asc' | 'desc';
  hasPendingSpreadRateRange: boolean;
  onSearchSelect: (value: string) => void;
  onToggleSolved: () => void;
  onToggleUnsolved: () => void;
  onToggleCountSort: (field: CountSortField) => void;
  onToggleSpreadRateSort: () => void;
  onChangeSpreadRateMin: (value: number) => void;
  onChangeSpreadRateMax: (value: number) => void;
  onChangeSpreadRateRange: (range: RangeSelection) => void;
  onApplySpreadRateRange: (range?: RangeSelection) => void;
};

const problemLoadingRows = Array.from({ length: 8 }, (_, index) => index);

export default function ProblemList({
  problems,
  currentDbms,
  showStats,
  showSolveState,
  showSolved,
  showUnsolved,
  countSortField,
  countSortDirection,
  isLoading,
  isSpreadRateFilterActive,
  spreadRateMinBound,
  spreadRateMaxBound,
  selectedSpreadRateMin,
  selectedSpreadRateMax,
  displaySpreadRateMin,
  displaySpreadRateMax,
  spreadRateSortOrder,
  hasPendingSpreadRateRange,
  onSearchSelect,
  onToggleSolved,
  onToggleUnsolved,
  onToggleCountSort,
  onToggleSpreadRateSort,
  onChangeSpreadRateMin,
  onChangeSpreadRateMax,
  onChangeSpreadRateRange,
  onApplySpreadRateRange,
}: ProblemListProps) {
  const [isSolveFilterOpen, setIsSolveFilterOpen] = useState(false);
  const [isSpreadRateFilterOpen, setIsSpreadRateFilterOpen] = useState(false);
  const solveFilterRef = useRef<HTMLDivElement | null>(null);
  const spreadRateFilterRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isSolveFilterOpen && !isSpreadRateFilterOpen) {
      return;
    }

    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node;
      if (isSolveFilterOpen && !solveFilterRef.current?.contains(target)) {
        setIsSolveFilterOpen(false);
      }
      if (isSpreadRateFilterOpen && !spreadRateFilterRef.current?.contains(target)) {
        setIsSpreadRateFilterOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setIsSolveFilterOpen(false);
        setIsSpreadRateFilterOpen(false);
      }
    }

    function handleResize() {
      setIsSolveFilterOpen(false);
      setIsSpreadRateFilterOpen(false);
    }

    window.addEventListener('mousedown', handlePointerDown);
    window.addEventListener('keydown', handleEscape);
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('mousedown', handlePointerDown);
      window.removeEventListener('keydown', handleEscape);
      window.removeEventListener('resize', handleResize);
    };
  }, [isSolveFilterOpen, isSpreadRateFilterOpen]);

  function renderSortIcon(field: CountSortField) {
    if (countSortField !== field) {
      return <SortNeutralIcon />;
    }

    return countSortDirection === 'asc' ? <SortAscendingIcon /> : <SortDescendingIcon />;
  }

  return (
    <section className="problem-list problem-table-shell">
      <div className={`problem-table-shell-inner ${isLoading ? 'is-loading' : ''}`}>
        <div className="problem-table" role="table" aria-label="문제 목록">
          <div className="problem-table-row problem-table-head" role="row">
            <div
              role="columnheader"
              className={`problem-table-head-cell ${showSolveState ? 'problem-table-head-cell-filter' : ''}`.trim()}
              ref={solveFilterRef}
            >
              <span>해결여부</span>
              {showSolveState ? (
                <>
                  <button
                    type="button"
                    className={`problem-table-head-filter-trigger ${isSolveFilterOpen ? 'is-open' : ''} ${
                      showSolved && showUnsolved ? '' : 'is-active'
                    }`.trim()}
                    aria-label="해결 여부 필터 열기"
                    onClick={() => setIsSolveFilterOpen((value) => !value)}
                  >
                    ▾
                  </button>

                  {isSolveFilterOpen ? (
                    <div className="problem-table-header-menu" role="menu" aria-label="해결 여부 필터 옵션">
                      <div className="problem-status-checks">
                        <label className="problem-status-check">
                          <input
                            type="checkbox"
                            checked={showSolved}
                            onChange={onToggleSolved}
                            className="problem-status-check-input"
                            aria-label="해결"
                          />
                          <span className="problem-status-check-text">해결</span>
                          <span className="problem-status-check-ui" aria-hidden="true" />
                        </label>

                        <label className="problem-status-check">
                          <input
                            type="checkbox"
                            checked={showUnsolved}
                            onChange={onToggleUnsolved}
                            className="problem-status-check-input"
                            aria-label="미해결"
                          />
                          <span className="problem-status-check-text">미해결</span>
                          <span className="problem-status-check-ui" aria-hidden="true" />
                        </label>
                      </div>
                    </div>
                  ) : null}
                </>
              ) : null}
            </div>
            <div role="columnheader" className="problem-table-head-cell">
              문제번호
            </div>
            <div role="columnheader" className="problem-table-head-cell">
              문제 제목
            </div>
            <div role="columnheader" className="problem-table-head-cell problem-table-head-cell-filter">
              <span>푼 사람 수</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger problem-table-head-sort-trigger ${countSortField === 'solvedCount' ? 'is-active' : ''}`}
                aria-label={countSortField === 'solvedCount' && countSortDirection === 'asc' ? '푼 사람 수 오름차순' : '푼 사람 수 내림차순'}
                onClick={() => onToggleCountSort('solvedCount')}
              >
                {renderSortIcon('solvedCount')}
              </button>
            </div>
            <div role="columnheader" className="problem-table-head-cell problem-table-head-cell-filter">
              <span>전체 제출</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger problem-table-head-sort-trigger ${countSortField === 'totalSubmitCount' ? 'is-active' : ''}`}
                aria-label={countSortField === 'totalSubmitCount' && countSortDirection === 'asc' ? '전체 제출 오름차순' : '전체 제출 내림차순'}
                onClick={() => onToggleCountSort('totalSubmitCount')}
              >
                {renderSortIcon('totalSubmitCount')}
              </button>
            </div>
            <div role="columnheader" className="problem-table-head-cell problem-table-head-cell-filter">
              <span>정답 제출</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger problem-table-head-sort-trigger ${countSortField === 'successSubmitCount' ? 'is-active' : ''}`}
                aria-label={countSortField === 'successSubmitCount' && countSortDirection === 'asc' ? '정답 제출 오름차순' : '정답 제출 내림차순'}
                onClick={() => onToggleCountSort('successSubmitCount')}
              >
                {renderSortIcon('successSubmitCount')}
              </button>
            </div>
            <div
              role="columnheader"
              className="problem-table-head-cell problem-table-head-cell-filter problem-table-head-cell-spread"
              ref={spreadRateFilterRef}
            >
              <span>Cost 편차</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger ${isSpreadRateFilterOpen ? 'is-open' : ''} ${isSpreadRateFilterActive ? 'is-active' : ''}`}
                aria-label="Cost 편차 필터 열기"
                onClick={() => setIsSpreadRateFilterOpen((value) => !value)}
              >
                ▾
              </button>
              {isSpreadRateFilterOpen ? (
                <div className="problem-table-header-menu problem-table-header-menu-spread" role="menu" aria-label="Cost 편차 필터 옵션">
                  <ProblemSpreadRateFilter
                    minBound={spreadRateMinBound}
                    maxBound={spreadRateMaxBound}
                    selectedMin={selectedSpreadRateMin}
                    selectedMax={selectedSpreadRateMax}
                    displayMin={displaySpreadRateMin}
                    displayMax={displaySpreadRateMax}
                    sortOrder={spreadRateSortOrder}
                    onToggleSort={onToggleSpreadRateSort}
                    onChangeMin={onChangeSpreadRateMin}
                    onChangeMax={onChangeSpreadRateMax}
                    onChangeRange={onChangeSpreadRateRange}
                    onApplyRange={onApplySpreadRateRange}
                    hasPendingChanges={hasPendingSpreadRateRange}
                  />
                </div>
              ) : null}
            </div>
            <div role="columnheader" className="problem-table-head-cell problem-table-head-cell-stats">
              통계
            </div>
          </div>

          {isLoading && problems.length === 0 ? (
            problemLoadingRows.map((rowIndex) => (
              <div key={`problem-loading-${rowIndex}`} className="problem-table-row problem-table-entry" role="row" aria-hidden="true">
                <span className="problem-status-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                <span className="problem-number" role="cell"><span className="wave-loading-placeholder is-medium" /></span>
                <span className="problem-title" role="cell"><span className="wave-loading-placeholder is-long" /></span>
                <span className="problem-metric" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                <span className="problem-metric" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                <span className="problem-metric" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                <span className="problem-metric problem-spread-rate-cell" role="cell"><span className="wave-loading-placeholder is-short" /></span>
                <span className="problem-stats-toggle-cell" role="cell"><span className="wave-loading-placeholder is-mini" /></span>
              </div>
            ))
          ) : problems.length === 0 ? (
            <div className="problem-table-empty-row" role="row">
              <span className="problem-empty-state problem-empty-state-inline" role="cell">선택한 조건에 맞는 문제가 없습니다.</span>
            </div>
          ) : (
            problems.map((problem) => (
              <ProblemCard
                key={problem.id}
                problem={problem}
                currentDbms={currentDbms}
                showStats={showStats}
                showSolveState={showSolveState}
                onSearchSelect={onSearchSelect}
                onSelect={(id) => navigate(`/problems/${id}`)}
              />
            ))
          )}
        </div>

        {isLoading ? (
          <div className="submit-history-loading-overlay problem-table-loading-overlay" aria-live="polite" aria-label="로딩 중">
            <span className="page-loading-spinner submit-history-loading-badge" aria-hidden="true" />
          </div>
        ) : null}
      </div>
    </section>
  );
}
