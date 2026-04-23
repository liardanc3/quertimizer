import { getApiBaseUrl } from './authApi';
import type { DbmsType } from '../types/domain';

interface UserProfileLinkResponse {
  type?: string;
  value?: string;
}

interface UserProfileSolvedRecordResponse {
  problemId?: string;
  problemTitle?: string;
  dbms?: string;
  executionTimeMs?: number;
  cost?: number;
  submittedAt?: string;
}

interface UserProfileSummaryResponse {
  handle?: string;
  bio?: string;
  links?: UserProfileLinkResponse[];
  defaultDbms?: string;
  sqlPublic?: boolean;
  executionPercentilePublic?: boolean;
  solvedRecordsPublic?: boolean;
  solvedProblemCountPublic?: boolean;
  averageExecutionPercentilePostgresql?: number | null;
  averageExecutionPercentileOracle?: number | null;
  authoredPostCount?: number;
  likedPostCount?: number;
  commentCount?: number;
}

interface UserProfileSolvedProblemsResponse {
  solvedProblemCount?: number;
  solvedProblemIds?: string[];
}

interface UserProfileSolvedRecordsResponse {
  solvedRecords?: UserProfileSolvedRecordResponse[];
}

export interface UserProfileLink {
  type: string;
  value: string;
}

export interface UserProfileSolvedRecord {
  problemId: string;
  problemTitle: string;
  dbms: DbmsType;
  executionTimeMs: number;
  cost: number;
  submittedAt: string;
}

export interface UserProfileSummary {
  handle: string;
  bio: string;
  links: UserProfileLink[];
  defaultDbms: DbmsType;
  sqlPublic: boolean;
  executionPercentilePublic: boolean;
  solvedRecordsPublic: boolean;
  solvedProblemCountPublic: boolean;
  averageExecutionPercentilePostgresql: number | null;
  averageExecutionPercentileOracle: number | null;
  authoredPostCount: number;
  likedPostCount: number;
  commentCount: number;
}

export interface UserProfileSolvedProblems {
  solvedProblemCount: number;
  solvedProblemIds: string[];
}

export interface UserProfileSolvedRecords {
  solvedRecords: UserProfileSolvedRecord[];
}

export interface UpdateUserProfilePayload {
  bio: string;
  links: UserProfileLink[];
  defaultDbms: DbmsType;
  sqlPublic: boolean;
  executionPercentilePublic: boolean;
  solvedRecordsPublic: boolean;
  solvedProblemCountPublic: boolean;
}

function toDbmsType(value?: string): DbmsType {
  return value === 'oracle' ? 'oracle' : 'postgresql';
}

function normalizeLinks(links?: UserProfileLinkResponse[]) {
  if (!Array.isArray(links)) {
    return [];
  }

  return links
    .filter((link): link is Required<UserProfileLinkResponse> => typeof link.type === 'string' && typeof link.value === 'string')
    .map((link) => ({
      type: link.type,
      value: link.value,
    }));
}

function normalizeSolvedRecords(records?: UserProfileSolvedRecordResponse[]) {
  if (!Array.isArray(records)) {
    return [];
  }

  return records
    .filter(
      (record): record is Required<UserProfileSolvedRecordResponse> =>
        typeof record.problemId === 'string' &&
        typeof record.problemTitle === 'string' &&
        typeof record.executionTimeMs === 'number' &&
        typeof record.cost === 'number' &&
        typeof record.submittedAt === 'string',
    )
    .map((record) => ({
      problemId: record.problemId,
      problemTitle: record.problemTitle,
      dbms: toDbmsType(record.dbms),
      executionTimeMs: record.executionTimeMs,
      cost: record.cost,
      submittedAt: record.submittedAt,
    }));
}

function normalizeProfileSummary(data: UserProfileSummaryResponse): UserProfileSummary {
  if (typeof data.handle !== 'string') {
    throw new Error();
  }

  return {
    handle: data.handle,
    bio: typeof data.bio === 'string' ? data.bio : '',
    links: normalizeLinks(data.links),
    defaultDbms: toDbmsType(data.defaultDbms),
    sqlPublic: data.sqlPublic === true,
    executionPercentilePublic: data.executionPercentilePublic !== false,
    solvedRecordsPublic: data.solvedRecordsPublic !== false,
    solvedProblemCountPublic: data.solvedProblemCountPublic !== false,
    averageExecutionPercentilePostgresql:
      typeof data.averageExecutionPercentilePostgresql === 'number' ? data.averageExecutionPercentilePostgresql : null,
    averageExecutionPercentileOracle:
      typeof data.averageExecutionPercentileOracle === 'number' ? data.averageExecutionPercentileOracle : null,
    authoredPostCount: typeof data.authoredPostCount === 'number' ? data.authoredPostCount : 0,
    likedPostCount: typeof data.likedPostCount === 'number' ? data.likedPostCount : 0,
    commentCount: typeof data.commentCount === 'number' ? data.commentCount : 0,
  };
}

function normalizeSolvedProblems(data: UserProfileSolvedProblemsResponse): UserProfileSolvedProblems {
  return {
    solvedProblemCount: typeof data.solvedProblemCount === 'number' ? data.solvedProblemCount : 0,
    solvedProblemIds: Array.isArray(data.solvedProblemIds) ? data.solvedProblemIds.filter((problemId): problemId is string => typeof problemId === 'string') : [],
  };
}

function normalizeSolvedRecordsResponse(data: UserProfileSolvedRecordsResponse): UserProfileSolvedRecords {
  return {
    solvedRecords: normalizeSolvedRecords(data.solvedRecords),
  };
}

async function requestProfile<T>(path: string, normalizer: (data: unknown) => T, options?: RequestInit) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      credentials: 'include',
      ...options,
    });
  } catch {
    throw new Error('프로필 조회에 실패했다.');
  }

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('프로필을 찾을 수 없다.');
    }

    if (response.status === 401) {
      throw new Error('로그인이 필요하다.');
    }

    throw new Error('프로필 조회에 실패했다.');
  }

  try {
    return normalizer(await response.json());
  } catch {
    throw new Error('프로필 응답 형식이 올바르지 않다.');
  }
}

export async function fetchMyProfileSummary() {
  return requestProfile('/profile/me', (data) => normalizeProfileSummary(data as UserProfileSummaryResponse));
}

export async function fetchProfileSummary(handle: string) {
  return requestProfile(`/profiles/${encodeURIComponent(handle)}`, (data) => normalizeProfileSummary(data as UserProfileSummaryResponse));
}

export async function fetchMySolvedProblems() {
  return requestProfile('/profile/me/solved-problems', (data) => normalizeSolvedProblems(data as UserProfileSolvedProblemsResponse));
}

export async function fetchSolvedProblems(handle: string) {
  return requestProfile(`/profiles/${encodeURIComponent(handle)}/solved-problems`, (data) => normalizeSolvedProblems(data as UserProfileSolvedProblemsResponse));
}

export async function fetchMySolvedRecords() {
  return requestProfile('/profile/me/solved-records', (data) => normalizeSolvedRecordsResponse(data as UserProfileSolvedRecordsResponse));
}

export async function fetchSolvedRecords(handle: string) {
  return requestProfile(`/profiles/${encodeURIComponent(handle)}/solved-records`, (data) => normalizeSolvedRecordsResponse(data as UserProfileSolvedRecordsResponse));
}

export async function updateMyProfile(payload: UpdateUserProfilePayload) {
  return requestProfile('/profile/me', (data) => normalizeProfileSummary(data as UserProfileSummaryResponse), {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
}
