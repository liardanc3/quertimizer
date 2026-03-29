import { useState, type MouseEvent } from 'react';
import type { ProblemSummary } from '../../types/domain';
import ProblemRuntimeChart from './ProblemRuntimeChart';

interface ProblemCardProps {
  problem: ProblemSummary;
  showTags: boolean;
  showStats: boolean;
  onSearchSelect: (value: string) => void;
  onSelect: (id: string) => void;
}

function stopCardEvent(event: MouseEvent<HTMLElement>) {
  event.preventDefault();
  event.stopPropagation();
}

export default function ProblemCard({ problem, showTags, showStats, onSearchSelect, onSelect }: ProblemCardProps) {
  const [isTagExpanded, setIsTagExpanded] = useState(true);
  const [isStatsExpanded, setIsStatsExpanded] = useState(true);
  const visibleTags = problem.tags.slice(0, 5);
  const isSolved = Boolean(problem.solvedAt);
  const myTimeMs =
    problem.runtimeDistributions?.postgresql?.myTimeMs ??
    problem.runtimeDistributions?.oracle?.myTimeMs ??
    problem.runtimeDistribution?.myTimeMs;
  const visibleTagsEnabled = showTags && isTagExpanded && visibleTags.length > 0;
  const visibleStatsEnabled = showStats && isStatsExpanded;

  return (
    <article
      className={`problem-card problem-distribution-card ${isSolved ? 'is-solved' : ''} ${visibleStatsEnabled ? '' : 'is-stats-hidden'}`.trim()}
    >
      <div className="problem-card-header">
        <div className="problem-card-heading">
          <div className="problem-number-row">
            <button
              type="button"
              className="problem-card-link-area"
              onClick={(event) => {
                stopCardEvent(event);
                onSelect(problem.id);
              }}
            >
              <p className="problem-number">문제 {problem.number}</p>
            </button>
            {isSolved ? (
              <span className="tooltip-anchor">
                <span className="problem-solved-badge is-solved">해결됨</span>
                {problem.solvedAt && myTimeMs !== undefined ? (
                  <span className="ui-tooltip problem-state-tooltip">
                    <span className="ui-tooltip-title">{`${myTimeMs}ms`}</span>
                    <span className="ui-tooltip-caption">{problem.solvedAt}</span>
                  </span>
                ) : null}
              </span>
            ) : (
              <span className="problem-solved-badge is-unsolved">미해결</span>
            )}
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
            <div className="problem-card-actions" role="group" aria-label={`문제 ${problem.number} 표시 옵션`}>
              <button
                type="button"
                className={`mini-toggle problem-card-action ${isTagExpanded ? 'is-selected' : ''}`}
                aria-pressed={isTagExpanded}
                onClick={(event) => {
                  stopCardEvent(event);
                  setIsTagExpanded((value) => !value);
                }}
              >
                태그
              </button>
              <button
                type="button"
                className={`mini-toggle problem-card-action ${isStatsExpanded ? 'is-selected' : ''}`}
                aria-pressed={isStatsExpanded}
                onClick={(event) => {
                  stopCardEvent(event);
                  setIsStatsExpanded((value) => !value);
                }}
              >
                통계
              </button>
            </div>
          </div>
        </div>

        <div className="problem-card-status" aria-label="문제 풀이 현황">
          <p className="problem-solved-count">{`푼 사람: ${problem.solvedCount}명`}</p>
        </div>
      </div>

      <div className={`problem-card-stats ${visibleStatsEnabled ? '' : 'is-hidden'}`.trim()} aria-hidden={!visibleStatsEnabled}>
        <ProblemRuntimeChart problem={problem} onSearchSelect={onSearchSelect} />
      </div>

      {visibleTagsEnabled ? (
        <div className="tag-row problem-card-tags">
          {visibleTags.map((tag) => (
            <span key={tag} className="tag-item">
              #{tag}
            </span>
          ))}
        </div>
      ) : null}
    </article>
  );
}
