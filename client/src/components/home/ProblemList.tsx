import { useMemo, useRef, useState } from 'react';
import { DataTable } from '../common/DataTable';
import { LoadingOverlay } from '../common/LoadingSpinner';
import SortIcon from '../icons/SortIcon';
import useDismissableLayer from '../../hooks/useDismissableLayer';
import { navigate } from '../../lib/navigation';
import { useUiText } from '../../lib/uiText';
import type { DbmsType, ProblemSummary } from '../../types/domain';
import ProblemCard from './ProblemCard';
import ProblemSpreadRateFilter from './ProblemSpreadRateFilter';

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
  const { text } = useUiText();
  const [isSolveFilterOpen, setIsSolveFilterOpen] = useState(false);
  const [isSpreadRateFilterOpen, setIsSpreadRateFilterOpen] = useState(false);
  const solveFilterRef = useRef<HTMLDivElement | null>(null);
  const spreadRateFilterRef = useRef<HTMLDivElement | null>(null);
  const filterLayerRefs = useMemo(() => [solveFilterRef, spreadRateFilterRef], []);

  useDismissableLayer({
    enabled: isSolveFilterOpen || isSpreadRateFilterOpen,
    refs: filterLayerRefs,
    onDismiss: () => {
      setIsSolveFilterOpen(false);
      setIsSpreadRateFilterOpen(false);
    },
    dismissOnResize: true,
  });

  function renderSortIcon(field: CountSortField) {
    if (countSortField !== field) {
      return <SortIcon direction="none" />;
    }

    return <SortIcon direction={countSortDirection} />;
  }

  return (
    <section className="problem-list problem-table-shell">
      <div className={`problem-table-shell-inner ${isLoading ? 'is-loading' : ''}`}>
        <DataTable className="problem-table" label={text('PROBLEM_TABLE_LABEL', '문제 목록')}>
          <div className="problem-table-row problem-table-head" role="row">
            <div
              role="columnheader"
              className={`problem-table-head-cell ${showSolveState ? 'problem-table-head-cell-filter' : ''}`.trim()}
              ref={solveFilterRef}
            >
              <span>{text('PROBLEM_TABLE_STATUS_COLUMN_LABEL', '해결 여부')}</span>
              {showSolveState ? (
                <>
                  <button
                    type="button"
                    className={`problem-table-head-filter-trigger ${isSolveFilterOpen ? 'is-open' : ''} ${
                      showSolved && showUnsolved ? '' : 'is-active'
                    }`.trim()}
                    aria-label={text('PROBLEM_TABLE_SOLVE_FILTER_OPEN_LABEL', '해결 여부 필터 열기')}
                    onClick={() => setIsSolveFilterOpen((value) => !value)}
                  >
                    ▾
                  </button>

                  {isSolveFilterOpen ? (
                    <div className="problem-table-header-menu" role="menu" aria-label={text('PROBLEM_TABLE_SOLVE_FILTER_OPTIONS_LABEL', '해결 여부 필터 옵션')}>
                      <div className="problem-status-checks">
                        <label className="problem-status-check">
                          <input
                            type="checkbox"
                            checked={showSolved}
                            onChange={onToggleSolved}
                            className="problem-status-check-input"
                            aria-label={text('PROBLEM_TABLE_SOLVED_LABEL', '해결')}
                          />
                          <span className="problem-status-check-text">{text('PROBLEM_TABLE_SOLVED_LABEL', '해결')}</span>
                          <span className="problem-status-check-ui" aria-hidden="true" />
                        </label>

                        <label className="problem-status-check">
                          <input
                            type="checkbox"
                            checked={showUnsolved}
                            onChange={onToggleUnsolved}
                            className="problem-status-check-input"
                            aria-label={text('PROBLEM_TABLE_UNSOLVED_LABEL', '미해결')}
                          />
                          <span className="problem-status-check-text">{text('PROBLEM_TABLE_UNSOLVED_LABEL', '미해결')}</span>
                          <span className="problem-status-check-ui" aria-hidden="true" />
                        </label>
                      </div>
                    </div>
                  ) : null}
                </>
              ) : null}
            </div>
            <div role="columnheader" className="problem-table-head-cell">
              {text('PROBLEM_TABLE_NUMBER_COLUMN_LABEL', '문제번호')}
            </div>
            <div role="columnheader" className="problem-table-head-cell">
              {text('PROBLEM_TABLE_TITLE_COLUMN_LABEL', '문제 제목')}
            </div>
            <div role="columnheader" className="problem-table-head-cell problem-table-head-cell-filter">
              <span>{text('PROBLEM_TABLE_SOLVED_COUNT_COLUMN_LABEL', '푼 사람 수')}</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger problem-table-head-sort-trigger ${countSortField === 'solvedCount' ? 'is-active' : ''}`}
                aria-label={
                  countSortField === 'solvedCount' && countSortDirection === 'asc'
                    ? text('PROBLEM_TABLE_SOLVED_COUNT_SORT_ASC_LABEL', '푼 사람 수 오름차순')
                    : text('PROBLEM_TABLE_SOLVED_COUNT_SORT_DESC_LABEL', '푼 사람 수 내림차순')
                }
                onClick={() => onToggleCountSort('solvedCount')}
              >
                {renderSortIcon('solvedCount')}
              </button>
            </div>
            <div role="columnheader" className="problem-table-head-cell problem-table-head-cell-filter">
              <span>{text('PROBLEM_TABLE_TOTAL_SUBMIT_COLUMN_LABEL', '전체 제출')}</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger problem-table-head-sort-trigger ${countSortField === 'totalSubmitCount' ? 'is-active' : ''}`}
                aria-label={
                  countSortField === 'totalSubmitCount' && countSortDirection === 'asc'
                    ? text('PROBLEM_TABLE_TOTAL_SUBMIT_SORT_ASC_LABEL', '전체 제출 오름차순')
                    : text('PROBLEM_TABLE_TOTAL_SUBMIT_SORT_DESC_LABEL', '전체 제출 내림차순')
                }
                onClick={() => onToggleCountSort('totalSubmitCount')}
              >
                {renderSortIcon('totalSubmitCount')}
              </button>
            </div>
            <div role="columnheader" className="problem-table-head-cell problem-table-head-cell-filter">
              <span>{text('PROBLEM_TABLE_SUCCESS_SUBMIT_COLUMN_LABEL', '정답 제출')}</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger problem-table-head-sort-trigger ${countSortField === 'successSubmitCount' ? 'is-active' : ''}`}
                aria-label={
                  countSortField === 'successSubmitCount' && countSortDirection === 'asc'
                    ? text('PROBLEM_TABLE_SUCCESS_SUBMIT_SORT_ASC_LABEL', '정답 제출 오름차순')
                    : text('PROBLEM_TABLE_SUCCESS_SUBMIT_SORT_DESC_LABEL', '정답 제출 내림차순')
                }
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
              <span>{text('PROBLEM_TABLE_COST_SPREAD_COLUMN_LABEL', 'Cost 편차')}</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger ${isSpreadRateFilterOpen ? 'is-open' : ''} ${isSpreadRateFilterActive ? 'is-active' : ''}`}
                aria-label={text('PROBLEM_TABLE_COST_SPREAD_FILTER_OPEN_LABEL', 'Cost 편차 필터 열기')}
                onClick={() => setIsSpreadRateFilterOpen((value) => !value)}
              >
                ▾
              </button>
              {isSpreadRateFilterOpen ? (
                <div
                  className="problem-table-header-menu problem-table-header-menu-spread"
                  role="menu"
                  aria-label={text('PROBLEM_TABLE_COST_SPREAD_FILTER_OPTIONS_LABEL', 'Cost 편차 필터 옵션')}
                >
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
              {text('PROBLEM_TABLE_STATS_COLUMN_LABEL', '통계')}
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
              <span className="problem-empty-state problem-empty-state-inline" role="cell">
                {text('PROBLEM_TABLE_EMPTY_STATE', '선택한 조건에 맞는 문제가 없습니다.')}
              </span>
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
        </DataTable>

        {isLoading ? <LoadingOverlay className="problem-table-loading-overlay" /> : null}
      </div>
    </section>
  );
}
