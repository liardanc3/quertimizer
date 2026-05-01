import { getApiBaseUrl } from './authApi';
import { createApiErrorFromResponse, getUiTextValue } from './uiText';
import type { DbmsType, ProblemSubmittedHistory, ProblemSummary } from '../types/domain';

export type { DbmsType };

interface ProblemSubmittedHistoryResponse {
  dbms?: string;
  handle?: string;
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
  ddlMysql?: string;
  dataPostgresql?: string;
  dataMysql?: string;
  condition?: string;
  output?: string;
  outputSample?: string;
  sampleDataSql?: string;
  answerSql?: string;
  answerHash?: string;
  dbms?: string;
}

interface ProblemSetSummaryResponse {
  problemSetId?: string;
}

interface ProblemSetDetailResponse {
  problemSetId?: string;
  ddlPostgresql?: string;
  ddlMysql?: string;
  dataPostgresql?: string;
  dataMysql?: string;
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
  ddlMysql: string;
  dataPostgresql: string;
  dataMysql: string;
  condition: string;
  output: string;
  outputSample: string;
  sampleDataSql: string;
  answerSql: string;
  answerHash: string;
  dbms: DbmsType;
}

export interface ProblemSetSummary {
  problemSetId: string;
}

export interface ProblemSetDetailData {
  problemSetId: string;
  ddlPostgresql: string;
  ddlMysql: string;
  dataPostgresql: string;
  dataMysql: string;
}

export interface CreateProblemPayload {
  title: string;
  description: string;
  condition: string;
  output: string;
  ddlPostgresql?: string;
  ddlMysql?: string;
  actualDataPostgresql?: string;
  actualDataMysql?: string;
  sampleDataPostgresql?: string;
  sampleDataMysql?: string;
  answerSql: string;
  existingProblemSet: boolean;
  existingProblem: boolean;
  problemSetId?: string;
  problemId?: string;
  dbms?: DbmsType;
}

interface ProblemOutputPreviewResponse {
  columns?: string[];
  rows?: unknown[][];
  rowCount?: number;
}

export interface ProblemOutputPreviewData {
  columns: string[];
  rows: Array<Array<string | number | boolean | null>>;
  rowCount: number;
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
const problemGetRequestPromises = new Map<string, Promise<unknown>>();

function toDbmsType(value?: string) {
  return value === 'mysql' ? 'mysql' : 'postgresql';
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
      (submittedHistory): submittedHistory is Required<Pick<ProblemSubmittedHistoryResponse, 'handle' | 'executionPlanElement' | 'executionTimeMs'>> & ProblemSubmittedHistoryResponse =>
        typeof submittedHistory.handle === 'string' &&
        typeof submittedHistory.executionPlanElement === 'number' &&
        typeof submittedHistory.executionTimeMs === 'number',
    )
    .map((submittedHistory) => ({
      dbms: toDbmsType(submittedHistory.dbms),
      handle: submittedHistory.handle,
      executionPlanElement: submittedHistory.executionPlanElement,
      executionTimeMs: submittedHistory.executionTimeMs,
      cost: typeof submittedHistory.cost === 'number' ? submittedHistory.cost : undefined,
    }));
}

function countSolvedUsers(submittedHistories: ProblemSubmittedHistory[]) {
  return new Set(submittedHistories.map((submittedHistory) => submittedHistory.handle)).size;
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
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  try {
    const data = (await response.json()) as ProblemDetailResponse;
    if (
      typeof data.problemId !== 'string' ||
      typeof data.title !== 'string' ||
      typeof data.description !== 'string' ||
      typeof data.ddlPostgresql !== 'string' ||
      typeof data.ddlMysql !== 'string' ||
      typeof data.dataPostgresql !== 'string' ||
      typeof data.dataMysql !== 'string' ||
      typeof data.condition !== 'string' ||
      typeof data.output !== 'string' ||
      typeof data.outputSample !== 'string' ||
      typeof data.sampleDataSql !== 'string' ||
      typeof data.answerSql !== 'string' ||
      typeof data.answerHash !== 'string'
    ) {
      throw new Error(getUiTextValue('PROBLEM_DETAIL_PARSE_FAIL_MESSAGE', '문제 정보 응답 형식이 올바르지 않습니다.'));
    }

    return {
      problemId: data.problemId,
      title: data.title,
      description: data.description,
      ddlPostgresql: data.ddlPostgresql,
      ddlMysql: data.ddlMysql,
      dataPostgresql: data.dataPostgresql,
      dataMysql: data.dataMysql,
      condition: data.condition,
      output: data.output,
      outputSample: data.outputSample,
      sampleDataSql: data.sampleDataSql,
      answerSql: data.answerSql,
      answerHash: data.answerHash,
      dbms: toDbmsType(data.dbms),
    };
  } catch {
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }
}

function requestProblemGet<T>(path: string, execute: () => Promise<T>): Promise<T> {
  const requestKey = `GET:${path}`;
  const inFlightRequest = problemGetRequestPromises.get(requestKey);
  if (inFlightRequest != null) {
    return inFlightRequest as Promise<T>;
  }

  const nextRequest = execute().finally(() => {
    problemGetRequestPromises.delete(requestKey);
  });

  problemGetRequestPromises.set(requestKey, nextRequest);
  return nextRequest;
}

export async function fetchProblems(params: FetchProblemsParams): Promise<ProblemPage> {
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

  const requestPath = `/problems?${searchParams.toString()}`;

  return requestProblemGet(requestPath, async () => {
    let response: Response;

    try {
      response = await fetch(`${getApiBaseUrl()}${requestPath}`, {
        method: 'GET',
        credentials: 'include',
      });
    } catch {
      throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
    }

    if (!response.ok) {
      throw await createApiErrorFromResponse(response, getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
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
      throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
    }
  });
}

export async function fetchProblemSets(): Promise<ProblemSetSummary[]> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/problem-sets`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  try {
    const data = (await response.json()) as ProblemSetSummaryResponse[];

    return Array.isArray(data)
      ? data
          .filter((item): item is Required<ProblemSetSummaryResponse> => typeof item.problemSetId === 'string')
          .map((item) => ({ problemSetId: item.problemSetId }))
      : [];
  } catch {
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
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
    throw new Error(getUiTextValue('PROBLEM_CREATE_SET_DETAIL_FAIL_MESSAGE', '테이블셋 정보를 불러오지 못했습니다.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('PROBLEM_CREATE_SET_DETAIL_FAIL_MESSAGE', '테이블셋 정보를 불러오지 못했습니다.'));
  }

  try {
    const data = (await response.json()) as ProblemSetDetailResponse;
    if (
      typeof data.problemSetId !== 'string' ||
      typeof data.ddlPostgresql !== 'string' ||
      typeof data.ddlMysql !== 'string' ||
      typeof data.dataPostgresql !== 'string' ||
      typeof data.dataMysql !== 'string'
    ) {
      throw new Error();
    }

    return {
      problemSetId: data.problemSetId,
      ddlPostgresql: data.ddlPostgresql,
      ddlMysql: data.ddlMysql,
      dataPostgresql: data.dataPostgresql,
      dataMysql: data.dataMysql,
    };
  } catch {
    throw new Error(getUiTextValue('PROBLEM_CREATE_SET_DETAIL_FAIL_MESSAGE', '테이블셋 정보를 불러오지 못했습니다.'));
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
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  try {
    const data = (await response.json()) as AdminProblemOptionResponse[];

    return Array.isArray(data)
      ? data
          .filter((item): item is Required<AdminProblemOptionResponse> => typeof item.problemId === 'string')
          .map((item) => item.problemId)
      : [];
  } catch {
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
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
    throw new Error(getUiTextValue('PROBLEM_CREATE_SAVE_FAIL_MESSAGE', '문제를 저장하지 못했습니다.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('PROBLEM_CREATE_SAVE_FAIL_MESSAGE', '문제를 저장하지 못했습니다.'));
  }

  try {
    const data = (await response.json()) as { problemId?: string };
    if (typeof data.problemId !== 'string') {
      throw new Error();
    }

    return data.problemId;
  } catch {
    throw new Error(getUiTextValue('PROBLEM_CREATE_SAVE_FAIL_MESSAGE', '문제를 저장하지 못했습니다.'));
  }
}

function normalizePreviewRows(rows: unknown[][] | undefined): Array<Array<string | number | boolean | null>> {
  if (!Array.isArray(rows)) {
    return [];
  }

  return rows.map((row) =>
    Array.isArray(row)
      ? row.map((value) =>
          typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' || value === null
            ? value
            : value === undefined
              ? null
            : String(value),
        )
      : [],
  );
}

export async function previewProblemOutput(payload: {
  dbms: DbmsType;
  ddl: string;
  sampleDataSql: string;
  answerSql: string;
}): Promise<ProblemOutputPreviewData> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/problems/output-preview`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch {
    throw new Error(getUiTextValue('PROBLEM_CREATE_PREVIEW_FAIL_MESSAGE', '출력 예시를 생성하지 못했습니다.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('PROBLEM_CREATE_PREVIEW_FAIL_MESSAGE', '출력 예시를 생성하지 못했습니다.'));
  }

  try {
    const data = (await response.json()) as ProblemOutputPreviewResponse;
    if (!Array.isArray(data.columns) || typeof data.rowCount !== 'number') {
      throw new Error();
    }

    return {
      columns: data.columns.filter((column): column is string => typeof column === 'string'),
      rows: normalizePreviewRows(data.rows),
      rowCount: data.rowCount,
    };
  } catch {
    throw new Error(getUiTextValue('PROBLEM_CREATE_PREVIEW_FAIL_MESSAGE', '출력 예시를 생성하지 못했습니다.'));
  }
}
