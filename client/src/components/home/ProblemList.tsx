import { navigate } from '../../lib/navigation';
import type { ProblemSummary } from '../../types/domain';
import ProblemCard from './ProblemCard';

interface ProblemListProps {
  problems: ProblemSummary[];
  showStats: boolean;
  onSearchSelect: (value: string) => void;
}

export default function ProblemList({ problems, showStats, onSearchSelect }: ProblemListProps) {
  if (problems.length === 0) {
    return (
      <section className="problem-list is-empty">
        <div className="problem-empty-state">선택한 조건에 맞는 문제가 없습니다.</div>
      </section>
    );
  }

  return (
    <section className="problem-list">
      {problems.map((problem) => (
        <ProblemCard
          key={problem.id}
          problem={problem}
          showStats={showStats}
          onSearchSelect={onSearchSelect}
          onSelect={(id) => navigate(`/problems/${id}`)}
        />
      ))}
    </section>
  );
}
