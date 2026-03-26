import { mockProblems } from '../../mocks/problems';
import { navigate } from '../../lib/navigation';
import type { ProblemViewMode } from '../../types/domain';
import ProblemCardSpoilerFree from './ProblemCardSpoilerFree';
import ProblemCardTagged from './ProblemCardTagged';

interface ProblemListProps {
  mode: ProblemViewMode;
}

export default function ProblemList({ mode }: ProblemListProps) {
  return (
    <section className="problem-list">
      {mockProblems.map((problem) =>
        mode === 'tagged' ? (
          <ProblemCardTagged key={problem.id} problem={problem} onSelect={(id) => navigate(`/problems/${id}`)} />
        ) : (
          <ProblemCardSpoilerFree key={problem.id} problem={problem} onSelect={(id) => navigate(`/problems/${id}`)} />
        )
      )}
    </section>
  );
}
