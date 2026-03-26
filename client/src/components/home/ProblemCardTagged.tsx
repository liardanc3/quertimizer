import type { ProblemSummary } from '../../types/domain';

interface ProblemCardTaggedProps {
  problem: ProblemSummary;
  onSelect: (id: string) => void;
}

export default function ProblemCardTagged({ problem, onSelect }: ProblemCardTaggedProps) {
  return (
    <button type="button" onClick={() => onSelect(problem.id)} className="problem-card">
      <p className="problem-number">문제 {problem.number}</p>
      <h3 className="problem-title">{problem.title}</h3>
      <p className="problem-preview">{problem.preview}</p>
      <div className="tag-row">
        {problem.tags.map((tag) => (
          <span key={tag} className="tag-item">#{tag}</span>
        ))}
      </div>
    </button>
  );
}
