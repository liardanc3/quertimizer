import { getApiBaseUrl } from './authApi';
import type { DbmsType, ProblemSubmittedHistory, ProblemSummary } from '../types/domain';

export type { DbmsType };

interface ProblemSubmittedHistoryResponse {
  dbms?: string;
  userId?: string;
  executionPlanElement?: number;
  executionTimeMs?: number;
  cost?: number;
}

interface ProblemListItemResponse {
  problemId?: string;
  title?: string;
  description?: string;
  totalSubmitCount?: number;
  successSubmitCount?: number;
  spreadRate?: number;
  submittedHistories?: ProblemSubmittedHistoryResponse[];
}

interface ProblemPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  spreadRateMin?: number;
  spreadRateMax?: number;
  problems?: ProblemListItemResponse[];
}

interface ProblemDetailResponse {
  problemId?: string;
  title?: string;
  description?: string;
  ddlPostgresql?: string;
  ddlOracle?: string;
  dataPostgresql?: string;
  dataOracle?: string;
  condition?: string;
  output?: string;
  outputSample?: string;
  answer?: string;
  answerHash?: string;
  dbms?: string;
}

interface ProblemSetSummaryResponse {
  problemSetId?: string;
}

interface ProblemSetDetailResponse {
  problemSetId?: string;
  ddlPostgresql?: string;
  ddlOracle?: string;
  dataPostgresql?: string;
  dataOracle?: string;
}

interface AdminProblemOptionResponse {
  problemId?: string;
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

export interface ProblemDetailData {
  problemId: string;
  title: string;
  description: string;
  ddlPostgresql: string;
  ddlOracle: string;
  dataPostgresql: string;
  dataOracle: string;
  condition: string;
  output: string;
  outputSample: string;
  answer: string;
  answerHash: string;
  dbms: DbmsType;
}

export interface ProblemSetSummary {
  problemSetId: string;
}

export interface ProblemSetDetailData {
  problemSetId: string;
  ddlPostgresql: string;
  ddlOracle: string;
  dataPostgresql: string;
  dataOracle: string;
}

export interface CreateProblemPayload {
  title: string;
  description: string;
  ddlPostgresql: string;
  ddlOracle: string;
  condition: string;
  output: string;
  outputSample: string;
  answer: string;
  answerSql?: string;
  problemSetMode: 'existing' | 'new';
  problemMode: 'existing' | 'new';
  problemSetId?: string;
  problemId?: string;
  dbms?: DbmsType;
  dataPostgresql?: string;
  dataOracle?: string;
}

export interface FetchProblemsParams {
  page: number;
  dbms: DbmsType;
  query: string;
  solveState: 'all' | 'solved' | 'unsolved' | 'none';
  solvedCountSort: 'none' | 'asc' | 'desc';
  totalSubmitSort: 'none' | 'asc' | 'desc';
  successSubmitSort: 'none' | 'asc' | 'desc';
  spreadRateSort: 'none' | 'asc' | 'desc';
  spreadRateMin?: number | null;
  spreadRateMax?: number | null;
}

export interface ProblemPage {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  spreadRateRange: {
    min: number;
    max: number;
  };
  problems: ProblemSummary[];
}

const DEFAULT_PROBLEM_DIFFICULTY = '중급' as ProblemSummary['difficulty'];

function toDbmsType(value?: string) {
  return value === 'oracle' ? 'oracle' : 'postgresql';
}

function toProblemNumber(problemId: string) {
  const [tableSetNumber] = problemId.split('-');
  const normalizedNumber = (tableSetNumber ?? '').replace(/^[A-Za-z]/, '');
  const parsedNumber = Number.parseInt(normalizedNumber, 10);

  return Number.isNaN(parsedNumber) ? 0 : parsedNumber;
}

function toSubmittedHistories(submittedHistories: ProblemSubmittedHistoryResponse[] | undefined): ProblemSubmittedHistory[] {
  if (!Array.isArray(submittedHistories)) {
    return [];
  }

  return submittedHistories
    .filter(
      (submittedHistory): submittedHistory is Required<Pick<ProblemSubmittedHistoryResponse, 'userId' | 'executionPlanElement' | 'executionTimeMs'>> & ProblemSubmittedHistoryResponse =>
        typeof submittedHistory.userId === 'string' &&
        typeof submittedHistory.executionPlanElement === 'number' &&
        typeof submittedHistory.executionTimeMs === 'number',
    )
    .map((submittedHistory) => ({
      dbms: toDbmsType(submittedHistory.dbms),
      userId: submittedHistory.userId,
      executionPlanElement: submittedHistory.executionPlanElement,
      executionTimeMs: submittedHistory.executionTimeMs,
      cost: typeof submittedHistory.cost === 'number' ? submittedHistory.cost : undefined,
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
    totalSubmitCount: typeof problem.totalSubmitCount === 'number' ? problem.totalSubmitCount : submittedHistories.length,
    successSubmitCount: typeof problem.successSubmitCount === 'number' ? problem.successSubmitCount : submittedHistories.length,
    spreadRate: typeof problem.spreadRate === 'number' ? problem.spreadRate : 0,
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
      typeof data.ddlPostgresql !== 'string' ||
      typeof data.ddlOracle !== 'string' ||
      typeof data.dataPostgresql !== 'string' ||
      typeof data.dataOracle !== 'string' ||
      typeof data.condition !== 'string' ||
      typeof data.output !== 'string' ||
      typeof data.outputSample !== 'string' ||
      typeof data.answer !== 'string' ||
      typeof data.answerHash !== 'string'
    ) {
      throw new Error();
    }

    return {
      problemId: data.problemId,
      title: data.title,
      description: data.description,
      ddlPostgresql: data.ddlPostgresql,
      ddlOracle: data.ddlOracle,
      dataPostgresql: data.dataPostgresql,
      dataOracle: data.dataOracle,
      condition: data.condition,
      output: data.output,
      outputSample: data.outputSample,
      answer: data.answer,
      answerHash: data.answerHash,
      dbms: toDbmsType(data.dbms),
    };
  } catch {
    throw new Error('문제 상세 조회에 실패했다.');
  }
}

export async function fetchProblems(params: FetchProblemsParams): Promise<ProblemPage> {
  let response: Response;

  const searchParams = new URLSearchParams({
    page: String(params.page),
    dbms: params.dbms,
  });

  if (params.solvedCountSort !== 'none') {
    searchParams.set('solvedCountSort', params.solvedCountSort);
  }

  if (params.totalSubmitSort !== 'none') {
    searchParams.set('totalSubmitSort', params.totalSubmitSort);
  }

  if (params.successSubmitSort !== 'none') {
    searchParams.set('successSubmitSort', params.successSubmitSort);
  }

  if (params.spreadRateSort !== 'none') {
    searchParams.set('spreadRateSort', params.spreadRateSort);
  }

  if (params.query.trim() !== '') {
    searchParams.set('query', params.query.trim());
  }

  if (params.solveState !== 'all') {
    searchParams.set('solveState', params.solveState);
  }

  if (typeof params.spreadRateMin === 'number') {
    searchParams.set('spreadRateMin', String(params.spreadRateMin));
  }

  if (typeof params.spreadRateMax === 'number') {
    searchParams.set('spreadRateMax', String(params.spreadRateMax));
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
      spreadRateRange: {
        min: typeof data.spreadRateMin === 'number' ? data.spreadRateMin : 0,
        max: typeof data.spreadRateMax === 'number' ? data.spreadRateMax : 0,
      },
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

export async function fetchProblemSets(): Promise<ProblemSetSummary[]> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/problem-sets`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('테이블셋 목록 조회에 실패했다.');
  }

  if (!response.ok) {
    throw new Error('테이블셋 목록 조회에 실패했다.');
  }

  try {
    const data = (await response.json()) as ProblemSetSummaryResponse[];

    return Array.isArray(data)
      ? data
          .filter((item): item is Required<ProblemSetSummaryResponse> => typeof item.problemSetId === 'string')
          .map((item) => ({ problemSetId: item.problemSetId }))
      : [];
  } catch {
    throw new Error('테이블셋 목록 조회에 실패했다.');
  }
}

export async function fetchProblemSetDetail(problemSetId: string): Promise<ProblemSetDetailData> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/problem-sets/${encodeURIComponent(problemSetId)}`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('테이블셋 상세 조회에 실패했다.');
  }

  if (!response.ok) {
    throw new Error('테이블셋 상세 조회에 실패했다.');
  }

  try {
    const data = (await response.json()) as ProblemSetDetailResponse;
    if (
      typeof data.problemSetId !== 'string' ||
      typeof data.ddlPostgresql !== 'string' ||
      typeof data.ddlOracle !== 'string' ||
      typeof data.dataPostgresql !== 'string' ||
      typeof data.dataOracle !== 'string'
    ) {
      throw new Error();
    }

    return {
      problemSetId: data.problemSetId,
      ddlPostgresql: data.ddlPostgresql,
      ddlOracle: data.ddlOracle,
      dataPostgresql: data.dataPostgresql,
      dataOracle: data.dataOracle,
    };
  } catch {
    throw new Error('테이블셋 상세 조회에 실패했다.');
  }
}

export async function fetchAdminProblemOptions(problemSetId: string): Promise<string[]> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/problem-sets/${encodeURIComponent(problemSetId)}/problems`, {
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
    const data = (await response.json()) as AdminProblemOptionResponse[];

    return Array.isArray(data)
      ? data
          .filter((item): item is Required<AdminProblemOptionResponse> => typeof item.problemId === 'string')
          .map((item) => item.problemId)
      : [];
  } catch {
    throw new Error('문제 목록 조회에 실패했다.');
  }
}

export async function createProblem(payload: CreateProblemPayload): Promise<string> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/problems`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch {
    throw new Error('문제 생성에 실패했다.');
  }

  if (!response.ok) {
    throw new Error('문제 생성에 실패했다.');
  }

  try {
    const data = (await response.json()) as { problemId?: string };
    if (typeof data.problemId !== 'string') {
      throw new Error();
    }

    return data.problemId;
  } catch {
    throw new Error('문제 생성에 실패했다.');
  }
}
