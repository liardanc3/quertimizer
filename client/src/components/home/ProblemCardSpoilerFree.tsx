import type { ProblemSummary } from '../../types/domain';

interface ProblemCardSpoilerFreeProps {
  problem: ProblemSummary;
  onSelect: (id: string) => void;
}

export default function ProblemCardSpoilerFree({ problem, onSelect }: ProblemCardSpoilerFreeProps) {
  return (
    <button type="button" onClick={() => onSelect(problem.id)} className="problem-card">
      <div className="problem-meta-row">
        <p className="problem-number">문제 {problem.number}</p>
        <span className="difficulty-chip">{problem.difficulty}</span>
      </div>
      <h3 className="problem-title">{problem.title}</h3>
      <p className="problem-preview">{problem.preview}</p>
      <span className="problem-link-label">힌트 없이 바로 풀기</span>
    </button>
  );
}
