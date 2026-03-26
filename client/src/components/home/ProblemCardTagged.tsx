import type { ProblemSummary } from '../../types/domain';

interface ProblemCardTaggedProps {
  problem: ProblemSummary;
  onSelect: (id: string) => void;
}

export default function ProblemCardTagged({ problem, onSelect }: ProblemCardTaggedProps) {
  return (
    <button type="button" onClick={() => onSelect(problem.id)} className="problem-card">
      <div className="problem-meta-row">
        <p className="problem-number">문제 {problem.number}</p>
        <span className="difficulty-chip">{problem.difficulty}</span>
      </div>
      <h3 className="problem-title">{problem.title}</h3>
      <p className="problem-preview">{problem.preview}</p>
      <div className="tag-row">
        {problem.tags.map((tag) => (
          <span key={tag} className="tag-item">
            #{tag}
          </span>
        ))}
      </div>
      <span className="problem-link-label">문제 풀기</span>
    </button>
  );
}
