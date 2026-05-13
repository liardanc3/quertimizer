import { memo, useState, type KeyboardEvent, type MouseEvent } from 'react';
import { formatInteger } from '@/shared/lib/formatters';
import { useUiText } from '@/shared/config/ui-text';
import type { DbmsType, ProblemSummary } from '@/shared/api/domain';
import ProblemRuntimeChart from './ProblemRuntimeChart';

interface ProblemCardProps {
  problem: ProblemSummary;
  currentDbms: DbmsType;
  showStats: boolean;
  showSolveState: boolean;
  onSearchSelect: (value: string) => void;
  onSelect: (id: string) => void;
}

function stopCardEvent(event: MouseEvent<HTMLElement>) {
  event.preventDefault();
  event.stopPropagation();
}

function formatCount(value: number | undefined) {
  return formatInteger(value ?? 0);
}

function isProblemSelectKey(event: KeyboardEvent<HTMLDivElement>) {
  return event.key === 'Enter' || event.key === ' ';
}

function StatsToggleIcon({ expanded }: { expanded: boolean }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
      {expanded ? (
        <path d="m4 10 4-4 4 4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
      ) : (
        <path d="m4 6 4 4 4-4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
      )}
    </svg>
  );
}

function ProblemCard({
  problem,
  currentDbms,
  showStats,
  showSolveState,
  onSearchSelect,
  onSelect,
}: ProblemCardProps) {
  const { text } = useUiText();
  const [isStatsExpanded, setIsStatsExpanded] = useState(false);
  const hasSubmissionHistory = (problem.totalSubmitCount ?? 0) > 0;
  const visibleStatsEnabled = showStats && isStatsExpanded && hasSubmissionHistory;
  const problemNumber = problem.problemNumber ?? String(problem.number);
  const solveStateLabel = showSolveState ? (problem.isSolved ? text('PROBLEM_TABLE_SOLVED_LABEL', '해결') : text('PROBLEM_TABLE_UNSOLVED_LABEL', '미해결')) : '-';

  return (
    <div
      role="rowgroup"
      tabIndex={0}
      aria-label={`${problemNumber} ${problem.title}`}
      className={`problem-table-entry ${showSolveState && problem.isSolved ? 'is-solved' : ''} ${
        visibleStatsEnabled ? '' : 'is-stats-hidden'
      }`.trim()}
      onClick={() => onSelect(problem.id)}
      onKeyDown={(event) => {
        if (!isProblemSelectKey(event)) {
          return;
        }

        event.preventDefault();
        onSelect(problem.id);
      }}
    >
      <div className="problem-table-row problem-table-body" role="row">
        <div role="cell" className="problem-table-cell problem-table-cell-status" data-label={text('PROBLEM_TABLE_STATUS_COLUMN_LABEL', '해결여부')}>
          <span
            className={`problem-table-status-text ${
              showSolveState ? (problem.isSolved ? 'is-solved' : 'is-unsolved') : 'is-unknown'
            }`}
          >
            {solveStateLabel}
          </span>
        </div>

        <div role="cell" className="problem-table-cell problem-table-cell-number" data-label={text('PROBLEM_TABLE_NUMBER_COLUMN_LABEL', '문제번호')}>
          <button
            type="button"
            className="problem-card-link-area is-problem-id"
            onClick={(event) => {
              stopCardEvent(event);
              onSelect(problem.id);
            }}
          >
            <span className="problem-number">{problemNumber}</span>
          </button>
        </div>

        <div role="cell" className="problem-table-cell problem-table-cell-title" data-label={text('PROBLEM_TABLE_TITLE_COLUMN_LABEL', '문제 제목')}>
          <div className="problem-table-title-row">
            <div className="problem-table-title-link-slot">
              <button
                type="button"
                className="problem-card-link-area is-title"
                onClick={(event) => {
                  stopCardEvent(event);
                  onSelect(problem.id);
                }}
              >
                <span className="problem-title">{problem.title}</span>
              </button>
            </div>
          </div>
        </div>

        <div role="cell" className="problem-table-cell problem-table-cell-metric is-solved-count" data-label={text('PROBLEM_TABLE_SOLVED_COUNT_COLUMN_LABEL', '푼 사람 수')}>
          <span className="problem-table-metric-value">{formatCount(problem.solvedCount)}</span>
        </div>

        <div role="cell" className="problem-table-cell problem-table-cell-metric is-total-submit" data-label={text('PROBLEM_TABLE_TOTAL_SUBMIT_COLUMN_LABEL', '전체 제출')}>
          <span className="problem-table-metric-value">{formatCount(problem.totalSubmitCount)}</span>
        </div>

        <div role="cell" className="problem-table-cell problem-table-cell-metric is-success-submit" data-label={text('PROBLEM_TABLE_SUCCESS_SUBMIT_COLUMN_LABEL', '정답 제출')}>
          <span className="problem-table-metric-value">{formatCount(problem.successSubmitCount)}</span>
        </div>

        <div role="cell" className="problem-table-cell problem-table-cell-stats" data-label={text('PROBLEM_TABLE_STATS_COLUMN_LABEL', '통계')}>
          <button
            type="button"
            className={`problem-stats-toggle-button ${visibleStatsEnabled ? 'is-open' : ''}`.trim()}
            aria-label={visibleStatsEnabled ? text('PROBLEM_TABLE_STATS_COLLAPSE_LABEL', '통계 접기') : text('PROBLEM_TABLE_STATS_EXPAND_LABEL', '통계 펼치기')}
            aria-expanded={visibleStatsEnabled}
            disabled={!hasSubmissionHistory}
            onClick={(event) => {
              stopCardEvent(event);
              if (!hasSubmissionHistory) {
                return;
              }
              setIsStatsExpanded((value) => !value);
            }}
          >
            <StatsToggleIcon expanded={visibleStatsEnabled} />
          </button>
        </div>
      </div>

      {visibleStatsEnabled ? (
        <div className="problem-table-stats">
          <ProblemRuntimeChart
            problem={problem}
            forcedDbms={currentDbms}
            onSearchSelect={onSearchSelect}
          />
        </div>
      ) : null}
    </div>
  );
}

export default memo(ProblemCard);
