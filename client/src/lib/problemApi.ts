import { getApiBaseUrl } from './authApi';
import type { ProblemSubmittedHistory, ProblemSummary } from '../types/domain';

interface ProblemSubmittedHistoryResponse {
  dbms?: string;
  userId?: string;
  executionPlanElement?: number;
  executionTimeMs?: number;
}

interface ProblemListItemResponse {
  problemId?: string;
  title?: string;
  description?: string;
  submittedHistories?: ProblemSubmittedHistoryResponse[];
}

interface ProblemPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  problems?: ProblemListItemResponse[];
}

export interface ProblemSampleTableData {
  name: string;
  columns: string[];
  rows: Array<Array<string | number | boolean | null>>;
}

export interface ProblemOutputSampleData {
  columns: string[];
  rows: Array<Array<string | number | boolean | null>>;
}

interface ProblemDetailResponse {
  problemId?: string;
  title?: string;
  description?: string;
  ddl?: string;
  condition?: string;
  output?: string;
  dataSample?: string;
  outputSample?: string;
}

export interface ProblemDetailData {
  problemId: string;
  title: string;
  description: string;
  ddl: string;
  condition: string;
  output: string;
  dataSample: string;
  outputSample: string;
}

export interface FetchProblemsParams {
  page: number;
  query: string;
  solveState: 'all' | 'solved' | 'unsolved' | 'none';
  solvedCountSort: 'asc' | 'desc';
}

export interface ProblemPage {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  problems: ProblemSummary[];
}

const DEFAULT_PROBLEM_DIFFICULTY = '중급' as ProblemSummary['difficulty'];

function toDbmsType(value?: string) {
  return value === 'oracle' ? 'oracle' : 'postgresql';
}

function toProblemNumber(problemId: string) {
  const [tableSetNumber] = problemId.split('-');
  const parsedNumber = Number.parseInt(tableSetNumber ?? '', 10);

  return Number.isNaN(parsedNumber) ? 0 : parsedNumber;
}

function toSubmittedHistories(submittedHistories: ProblemSubmittedHistoryResponse[] | undefined): ProblemSubmittedHistory[] {
  if (!Array.isArray(submittedHistories)) {
    return [];
  }

  return submittedHistories
    .filter(
      (submittedHistory): submittedHistory is Required<ProblemSubmittedHistoryResponse> =>
        typeof submittedHistory.userId === 'string' &&
        typeof submittedHistory.executionPlanElement === 'number' &&
        typeof submittedHistory.executionTimeMs === 'number',
    )
    .map((submittedHistory) => ({
      dbms: toDbmsType(submittedHistory.dbms),
      userId: submittedHistory.userId,
      executionPlanElement: submittedHistory.executionPlanElement,
      executionTimeMs: submittedHistory.executionTimeMs,
    }));
}

function countSolvedUsers(submittedHistories: ProblemSubmittedHistory[]) {
  return new Set(submittedHistories.map((submittedHistory) => submittedHistory.userId)).size;
}

function toProblemSummary(problem: ProblemListItemResponse) {
  const submittedHistories = toSubmittedHistories(problem.submittedHistories);

  return {
    id: problem.problemId!,
    domain: 'rdbms',
    number: toProblemNumber(problem.problemId!),
    problemNumber: problem.problemId!,
    title: problem.title!,
    preview: problem.description!,
    tags: [],
    difficulty: DEFAULT_PROBLEM_DIFFICULTY,
    solvedCount: countSolvedUsers(submittedHistories),
    submittedHistories,
  } satisfies ProblemSummary;
}

export async function fetchProblemDetail(problemId: string): Promise<ProblemDetailData> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/problems/${encodeURIComponent(problemId)}`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('문제 상세 조회에 실패했다.');
  }

  if (!response.ok) {
    throw new Error('문제 상세 조회에 실패했다.');
  }

  try {
    const data = (await response.json()) as ProblemDetailResponse;
    if (
      typeof data.problemId !== 'string' ||
      typeof data.title !== 'string' ||
      typeof data.description !== 'string' ||
      typeof data.ddl !== 'string' ||
      typeof data.condition !== 'string' ||
      typeof data.output !== 'string' ||
      typeof data.dataSample !== 'string' ||
      typeof data.outputSample !== 'string'
    ) {
      throw new Error();
    }

    return {
      problemId: data.problemId,
      title: data.title,
      description: data.description,
      ddl: data.ddl,
      condition: data.condition,
      output: data.output,
      dataSample: data.dataSample,
      outputSample: data.outputSample,
    };
  } catch {
    throw new Error('문제 상세 조회에 실패했다.');
  }
}

export async function fetchProblems(params: FetchProblemsParams): Promise<ProblemPage> {
  let response: Response;

  const searchParams = new URLSearchParams({
    page: String(params.page),
    solvedCountSort: params.solvedCountSort,
  });

  if (params.query.trim() !== '') {
    searchParams.set('query', params.query.trim());
  }

  if (params.solveState !== 'all') {
    searchParams.set('solveState', params.solveState);
  }

  try {
    response = await fetch(`${getApiBaseUrl()}/problems?${searchParams.toString()}`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('문제 목록 조회에 실패했다.');
  }

  if (!response.ok) {
    throw new Error('문제 목록 조회에 실패했다.');
  }

  try {
    const data = (await response.json()) as ProblemPageResponse;
    if (
      typeof data.currentPage !== 'number' ||
      typeof data.pageSize !== 'number' ||
      typeof data.totalCount !== 'number' ||
      typeof data.totalPages !== 'number' ||
      !Array.isArray(data.problems)
    ) {
      throw new Error();
    }

    return {
      currentPage: data.currentPage,
      pageSize: data.pageSize,
      totalCount: data.totalCount,
      totalPages: data.totalPages,
      problems: data.problems
        .filter(
          (problem): problem is Required<Pick<ProblemListItemResponse, 'problemId' | 'title' | 'description'>> & ProblemListItemResponse =>
            typeof problem.problemId === 'string' &&
            typeof problem.title === 'string' &&
            typeof problem.description === 'string',
        )
        .map(toProblemSummary),
    };
  } catch {
    throw new Error('문제 목록 조회에 실패했다.');
  }
}
