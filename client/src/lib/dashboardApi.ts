import { getApiBaseUrl } from './authApi';
import { createApiErrorFromResponse, getUiTextValue } from './uiText';
import type { DbmsType } from '../types/domain';

export interface DashboardCommunityPost {
  postId: string;
  title: string;
  authorHandle: string;
  excerpt: string;
  tags: string[];
  category: 'notice' | 'discussion' | 'question';
  createdAt: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  hotScore: number;
}

export interface DashboardProblemRecommendation {
  problemId: string;
  title: string;
  dbms: DbmsType;
  solvedUserCount: number;
  totalSubmitCount: number;
  successSubmitCount: number;
  spreadRate: number;
  solvedByCurrentUser: boolean;
}

export interface DashboardData {
  authenticated: boolean;
  currentHandle: string | null;
  communityPosts: DashboardCommunityPost[];
  problems: DashboardProblemRecommendation[];
}

interface DashboardCommunityPostResponse {
  postId?: string;
  title?: string;
  authorHandle?: string;
  excerpt?: string;
  tags?: string[];
  category?: string;
  createdAt?: string;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  hotScore?: number;
}

interface DashboardProblemRecommendationResponse {
  problemId?: string;
  title?: string;
  dbms?: string;
  solvedUserCount?: number;
  totalSubmitCount?: number;
  successSubmitCount?: number;
  spreadRate?: number;
  solvedByCurrentUser?: boolean;
}

interface DashboardResponse {
  authenticated?: boolean;
  currentHandle?: string | null;
  communityPosts?: DashboardCommunityPostResponse[];
  problems?: DashboardProblemRecommendationResponse[];
}

let dashboardInFlightPromise: Promise<DashboardData> | null = null;

function getDashboardLoadFailureMessage() {
  return getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.');
}

function normalizeCategory(value: string | undefined): DashboardCommunityPost['category'] {
  if (value === 'notice' || value === 'question') {
    return value;
  }

  return 'discussion';
}

function normalizeDbms(value: string | undefined): DbmsType {
  return value === 'oracle' ? 'oracle' : 'postgresql';
}

function toCommunityPost(data: DashboardCommunityPostResponse): DashboardCommunityPost {
  return {
    postId: data.postId ?? '',
    title: data.title ?? '',
    authorHandle: data.authorHandle ?? '',
    excerpt: data.excerpt ?? '',
    tags: Array.isArray(data.tags) ? data.tags : [],
    category: normalizeCategory(data.category),
    createdAt: data.createdAt ?? '',
    viewCount: data.viewCount ?? 0,
    likeCount: data.likeCount ?? 0,
    commentCount: data.commentCount ?? 0,
    hotScore: data.hotScore ?? 0,
  };
}

function toProblemRecommendation(data: DashboardProblemRecommendationResponse): DashboardProblemRecommendation {
  return {
    problemId: data.problemId ?? '',
    title: data.title ?? '',
    dbms: normalizeDbms(data.dbms),
    solvedUserCount: data.solvedUserCount ?? 0,
    totalSubmitCount: data.totalSubmitCount ?? 0,
    successSubmitCount: data.successSubmitCount ?? 0,
    spreadRate: data.spreadRate ?? 0,
    solvedByCurrentUser: data.solvedByCurrentUser === true,
  };
}

async function requestDashboard(): Promise<DashboardData> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/dashboard`, {
      credentials: 'include',
    });
  } catch {
    throw new Error(getDashboardLoadFailureMessage());
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getDashboardLoadFailureMessage());
  }

  let data: DashboardResponse;

  try {
    data = (await response.json()) as DashboardResponse;
  } catch {
    throw new Error(getDashboardLoadFailureMessage());
  }

  return {
    authenticated: data.authenticated === true,
    currentHandle: typeof data.currentHandle === 'string' && data.currentHandle.trim() !== '' ? data.currentHandle : null,
    communityPosts: Array.isArray(data.communityPosts) ? data.communityPosts.map(toCommunityPost) : [],
    problems: Array.isArray(data.problems) ? data.problems.map(toProblemRecommendation) : [],
  };
}

export async function fetchDashboard(): Promise<DashboardData> {
  if (dashboardInFlightPromise != null) {
    return dashboardInFlightPromise;
  }

  dashboardInFlightPromise = requestDashboard().finally(() => {
    dashboardInFlightPromise = null;
  });

  return dashboardInFlightPromise;
}
