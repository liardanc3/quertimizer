import { useEffect, useState, type MouseEvent } from 'react';
import type { ProblemSummary } from '../../types/domain';
import ProblemRuntimeChart from './ProblemRuntimeChart';

interface ProblemCardProps {
  problem: ProblemSummary;
  showStats: boolean;
  showSolveState: boolean;
  onSearchSelect: (value: string) => void;
  onSelect: (id: string) => void;
}

function stopCardEvent(event: MouseEvent<HTMLElement>) {
  event.preventDefault();
  event.stopPropagation();
}

export default function ProblemCard({
  problem,
  showStats,
  showSolveState,
  onSearchSelect,
  onSelect,
}: ProblemCardProps) {
  const [isStatsExpanded, setIsStatsExpanded] = useState(true);
  const [activeSolvedCount, setActiveSolvedCount] = useState(problem.solvedCount);
  const visibleStatsEnabled = showStats && isStatsExpanded;
  const problemNumber = problem.problemNumber ?? String(problem.number);

  useEffect(() => {
    setActiveSolvedCount(problem.solvedCount);
  }, [problem.solvedCount]);

  return (
    <article
      className={`problem-card problem-distribution-card ${showSolveState && problem.isSolved ? 'is-solved' : ''} ${
        visibleStatsEnabled ? '' : 'is-stats-hidden'
      }`.trim()}
    >
      <div className="problem-card-header">
        <div className="problem-card-heading">
          <div className="problem-number-row">
            <button
              type="button"
              className="problem-card-link-area is-problem-id"
              onClick={(event) => {
                stopCardEvent(event);
                onSelect(problem.id);
              }}
            >
              <span className="problem-number">{`문제 ${problemNumber}`}</span>
            </button>

            {showSolveState ? (
              <span className={`problem-solved-badge ${problem.isSolved ? 'is-solved' : 'is-unsolved'}`}>
                {problem.isSolved ? '해결' : '미해결'}
              </span>
            ) : null}
          </div>

          <div className="problem-title-row">
            <div className="problem-title-link-slot">
              <button
                type="button"
                className="problem-card-link-area is-title"
                onClick={(event) => {
                  stopCardEvent(event);
                  onSelect(problem.id);
                }}
              >
                <h3 className="problem-title">{problem.title}</h3>
              </button>
            </div>

            <div className="problem-card-actions" role="group" aria-label={`${problemNumber} 표시 옵션`}>
              <button
                type="button"
                className={`mini-toggle problem-card-action ${isStatsExpanded ? 'is-selected' : ''}`}
                aria-pressed={isStatsExpanded}
                onClick={(event) => {
                  stopCardEvent(event);
                  setIsStatsExpanded((value) => !value);
                }}
              >
                {'통계'}
              </button>
            </div>
          </div>
        </div>

        <div className="problem-card-status" aria-label="문제 통계">
          <p className="problem-solved-count">{`\uD47C \uC0AC\uB78C: ${activeSolvedCount}\uBA85`}</p>
        </div>
      </div>

      <div className={`problem-card-stats ${visibleStatsEnabled ? '' : 'is-hidden'}`.trim()} aria-hidden={!visibleStatsEnabled}>
        <ProblemRuntimeChart
          problem={problem}
          onSearchSelect={onSearchSelect}
          onSolvedCountChange={setActiveSolvedCount}
        />
      </div>
    </article>
  );
}
