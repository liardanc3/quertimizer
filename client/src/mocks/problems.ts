import { mockProblemDetails } from './problemDetail';
import type { ProblemSummary } from '../types/domain';

export const mockProblems: ProblemSummary[] = mockProblemDetails.map((problem) => ({
  id: problem.id,
  domain: problem.domain,
  number: problem.number,
  title: problem.title,
  preview: problem.preview,
  tags: problem.tags,
  difficulty: problem.difficulty,
  solvedCount: problem.solvedCount,
  solvedAt: problem.solvedAt,
  runtimeDistribution: problem.runtimeDistribution,
  runtimeDistributions: problem.runtimeDistributions,
}));
