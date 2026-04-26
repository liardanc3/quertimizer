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
  profileImageUrl?: string;
  backgroundImageUrl?: string;
  signupAt?: string;
  links?: UserProfileLinkResponse[];
  defaultDbms?: string;
  sqlPublic?: boolean;
  executionPercentilePublic?: boolean;
  solvedRecordsPublic?: boolean;
  solvedProblemCountPublic?: boolean;
  communityActivityPublic?: boolean;
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

interface UserProfileSubmissionActivityResponse {
  date?: string;
  count?: number;
}

interface UserProfileSubmissionSummaryResponse {
  attemptedProblemIds?: string[];
  submissionActivities?: UserProfileSubmissionActivityResponse[];
}

interface ExceptionResponse {
  reasons?: string[];
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
  profileImageUrl: string;
  backgroundImageUrl: string;
  signupAt: string;
  links: UserProfileLink[];
  defaultDbms: DbmsType;
  sqlPublic: boolean;
  executionPercentilePublic: boolean;
  solvedRecordsPublic: boolean;
  solvedProblemCountPublic: boolean;
  communityActivityPublic: boolean;
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

export interface UserProfileSubmissionActivity {
  date: string;
  count: number;
}

export interface UserProfileSubmissionSummary {
  attemptedProblemIds: string[];
  submissionActivities: UserProfileSubmissionActivity[];
}

export interface UpdateUserProfilePayload {
  bio: string;
  profileImageUrl: string;
  backgroundImageUrl: string;
  links: UserProfileLink[];
  defaultDbms: DbmsType;
  sqlPublic: boolean;
  executionPercentilePublic: boolean;
  solvedRecordsPublic: boolean;
  solvedProblemCountPublic: boolean;
  communityActivityPublic: boolean;
}

interface UpdateUserProfileRequestBody {
  bio: string;
  profileImageUrl: string;
  backgroundImageUrl: string;
  links: UserProfileLink[];
  defaultDbms: 'POSTGRESQL' | 'ORACLE';
  sqlPublic: boolean;
  executionPercentilePublic: boolean;
  solvedRecordsPublic: boolean;
  solvedProblemCountPublic: boolean;
  communityActivityPublic: boolean;
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
    profileImageUrl: normalizeAssetUrl(data.profileImageUrl),
    backgroundImageUrl: normalizeAssetUrl(data.backgroundImageUrl),
    signupAt: typeof data.signupAt === 'string' ? data.signupAt : new Date().toISOString(),
    links: normalizeLinks(data.links),
    defaultDbms: toDbmsType(data.defaultDbms),
    sqlPublic: data.sqlPublic === true,
    executionPercentilePublic: data.executionPercentilePublic !== false,
    solvedRecordsPublic: data.solvedRecordsPublic !== false,
    solvedProblemCountPublic: data.solvedProblemCountPublic !== false,
    communityActivityPublic: data.communityActivityPublic !== false,
    averageExecutionPercentilePostgresql:
      typeof data.averageExecutionPercentilePostgresql === 'number' ? data.averageExecutionPercentilePostgresql : null,
    averageExecutionPercentileOracle:
      typeof data.averageExecutionPercentileOracle === 'number' ? data.averageExecutionPercentileOracle : null,
    authoredPostCount: typeof data.authoredPostCount === 'number' ? data.authoredPostCount : 0,
    likedPostCount: typeof data.likedPostCount === 'number' ? data.likedPostCount : 0,
    commentCount: typeof data.commentCount === 'number' ? data.commentCount : 0,
  };
}

function normalizeAssetUrl(value?: string) {
  if (typeof value !== 'string' || value.trim() === '') {
    return '';
  }

  return value.startsWith('http') ? value : `${getApiBaseUrl()}${value}`;
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

function normalizeSubmissionActivities(records?: UserProfileSubmissionActivityResponse[]) {
  if (!Array.isArray(records)) {
    return [];
  }

  return records
    .filter(
      (record): record is Required<UserProfileSubmissionActivityResponse> =>
        typeof record.date === 'string' && typeof record.count === 'number',
    )
    .map((record) => ({
      date: record.date,
      count: record.count,
    }));
}

function normalizeSubmissionSummary(data: UserProfileSubmissionSummaryResponse): UserProfileSubmissionSummary {
  return {
    attemptedProblemIds: Array.isArray(data.attemptedProblemIds)
      ? data.attemptedProblemIds.filter((problemId): problemId is string => typeof problemId === 'string')
      : [],
    submissionActivities: normalizeSubmissionActivities(data.submissionActivities),
  };
}

async function requestProfile<T>(path: string, normalizer: (data: unknown) => T, options?: RequestInit, fallbackMessage = '프로필 조회에 실패했다.') {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      credentials: 'include',
      ...options,
    });
  } catch {
    throw new Error(fallbackMessage);
  }

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('프로필을 찾을 수 없다.');
    }

    if (response.status === 401) {
      throw new Error('로그인이 필요하다.');
    }

    throw new Error(await extractProfileErrorMessage(response, fallbackMessage));
  }

  try {
    return normalizer(await response.json());
  } catch {
    throw new Error('프로필 응답 형식이 올바르지 않다.');
  }
}

async function extractProfileErrorMessage(response: Response, fallbackMessage: string) {
  try {
    const data = (await response.json()) as ExceptionResponse;

    if (Array.isArray(data.reasons) && typeof data.reasons[0] === 'string' && data.reasons[0].trim() !== '') {
      return data.reasons[0];
    }
  } catch {
    return fallbackMessage;
  }

  return fallbackMessage;
}

function createUpdateUserProfileRequestBody(payload: UpdateUserProfilePayload): UpdateUserProfileRequestBody {
  return {
    ...payload,
    defaultDbms: payload.defaultDbms === 'oracle' ? 'ORACLE' : 'POSTGRESQL',
  };
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

export async function fetchMySubmissionSummary() {
  return requestProfile('/profile/me/submission-summary', (data) => normalizeSubmissionSummary(data as UserProfileSubmissionSummaryResponse));
}

export async function fetchSubmissionSummary(handle: string) {
  return requestProfile(`/profiles/${encodeURIComponent(handle)}/submission-summary`, (data) => normalizeSubmissionSummary(data as UserProfileSubmissionSummaryResponse));
}

export async function updateMyProfile(payload: UpdateUserProfilePayload) {
  return requestProfile(
    '/profile/me',
    (data) => normalizeProfileSummary(data as UserProfileSummaryResponse),
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(createUpdateUserProfileRequestBody(payload)),
    },
    '프로필 저장에 실패했다.',
  );
}
