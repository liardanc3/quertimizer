import { mockProblemDetails } from './problemDetail';
import type { ProblemSummary } from '../types/domain';

export const mockProblems: ProblemSummary[] = mockProblemDetails.map((problem) => ({
  id: problem.id,
  number: problem.number,
  title: problem.title,
  preview: problem.preview,
  tags: problem.tags,
  difficulty: problem.difficulty,
}));
