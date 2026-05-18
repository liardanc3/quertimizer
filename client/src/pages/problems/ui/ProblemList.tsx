import { useCallback, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { DataTable } from '@/shared/ui';
import { LoadingOverlay } from '@/shared/ui';
import { SortIcon } from '@/shared/ui/icons';
import useDismissableLayer from '@/shared/lib/hooks/use-dismissable-layer';
import { navigate } from '@/shared/config/navigation';
import { useUiText } from '@/shared/config/ui-text';
import type { DbmsType, ProblemSummary } from '@/shared/api/domain';
import ProblemCard from './ProblemCard';

type ProblemSortField = 'problemId' | 'solvedCount' | 'totalSubmitCount' | 'successSubmitCount';

type ProblemListProps = {
  problems: ProblemSummary[];
  currentDbms: DbmsType;
  showStats: boolean;
  showSolveState: boolean;
  showSolved: boolean;
  showUnsolved: boolean;
  countSortField: ProblemSortField;
  countSortDirection: 'desc' | 'asc';
  isLoading: boolean;
  onSearchSelect: (value: string) => void;
  onToggleSolved: () => void;
  onToggleUnsolved: () => void;
  onToggleCountSort: (field: ProblemSortField) => void;
};

const problemLoadingRows = Array.from({ length: 8 }, (_, index) => index);
const FILTER_MENU_VIEWPORT_PADDING = 8;

type FilterMenuPosition = {
  top: number;
  left: number;
};

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
  onSearchSelect,
  onToggleSolved,
  onToggleUnsolved,
  onToggleCountSort,
}: ProblemListProps) {
  const { text } = useUiText();
  const [isSolveFilterOpen, setIsSolveFilterOpen] = useState(false);
  const solveFilterRef = useRef<HTMLDivElement | null>(null);
  const solveFilterMenuRef = useRef<HTMLDivElement | null>(null);
  const filterLayerRefs = useMemo(() => [solveFilterRef, solveFilterMenuRef], []);
  const [filterMenuPosition, setFilterMenuPosition] = useState<FilterMenuPosition | null>(null);
  const handleSelectProblem = useCallback((id: string) => navigate(`/problems/${id}`), []);

  useDismissableLayer({
    enabled: isSolveFilterOpen,
    refs: filterLayerRefs,
    onDismiss: () => setIsSolveFilterOpen(false),
    dismissOnResize: true,
    dismissOnScroll: true,
  });

  useLayoutEffect(() => {
    if (!isSolveFilterOpen) {
      setFilterMenuPosition(null);
      return;
    }

    // 헤더 셀 위치 기준으로 body 포털 메뉴 위치 계산
    const triggerRect = solveFilterRef.current?.getBoundingClientRect();
    if (!triggerRect) {
      return;
    }

    const menuWidth = solveFilterMenuRef.current?.offsetWidth ?? 160;
    const maxLeft = window.innerWidth - menuWidth - FILTER_MENU_VIEWPORT_PADDING;
    setFilterMenuPosition({
      top: triggerRect.bottom + 5,
      left: Math.max(FILTER_MENU_VIEWPORT_PADDING, Math.min(triggerRect.left, maxLeft)),
    });
  }, [isSolveFilterOpen]);

  function renderSortIcon(field: ProblemSortField) {
    if (countSortField !== field) {
      return <SortIcon direction="none" />;
    }

    return <SortIcon direction={countSortDirection} />;
  }

  function renderSolveFilterMenu() {
    const menuStyle = filterMenuPosition
      ? { top: filterMenuPosition.top, left: filterMenuPosition.left }
      : { top: 0, left: 0, visibility: 'hidden' as const };

    return (
      <div
        ref={solveFilterMenuRef}
        className="problem-table-header-menu problem-table-header-menu-floating"
        style={menuStyle}
        role="menu"
        aria-label={text('PROBLEM_TABLE_SOLVE_FILTER_OPTIONS_LABEL', '해결 여부 필터 옵션')}
      >
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
    );
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

                  {isSolveFilterOpen && typeof document !== 'undefined'
                    ? createPortal(
                        <div className="home-page data-page problem-filter-portal-root">
                          {renderSolveFilterMenu()}
                        </div>,
                        document.body,
                      )
                    : null}
                </>
              ) : null}
            </div>
            <div role="columnheader" className="problem-table-head-cell problem-table-head-cell-filter">
              <span>{text('PROBLEM_TABLE_NUMBER_COLUMN_LABEL', '문제번호')}</span>
              <button
                type="button"
                className={`problem-table-head-filter-trigger problem-table-head-sort-trigger ${countSortField === 'problemId' ? 'is-active' : ''}`}
                aria-label={
                  countSortField === 'problemId' && countSortDirection === 'desc'
                    ? text('PROBLEM_TABLE_NUMBER_SORT_DESC_LABEL', '문제 번호 내림차순')
                    : text('PROBLEM_TABLE_NUMBER_SORT_ASC_LABEL', '문제 번호 오름차순')
                }
                onClick={() => onToggleCountSort('problemId')}
              >
                {renderSortIcon('problemId')}
              </button>
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
                <span className="problem-stats-toggle-cell" role="cell"><span className="wave-loading-placeholder is-mini" /></span>
              </div>
            ))
          ) : problems.length === 0 ? (
            <div className="problem-table-empty-row data-table-empty-row" role="row">
              <span className="problem-empty-state problem-empty-state-inline data-table-empty-cell" role="cell" aria-live="polite">
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
                onSelect={handleSelectProblem}
              />
            ))
          )}
        </DataTable>

        {isLoading ? <LoadingOverlay className="problem-table-loading-overlay" /> : null}
      </div>
    </section>
  );
}
