import { getApiBaseUrl } from './authApi';
import { createApiErrorFromResponse, getUiTextValue } from './uiText';

export type AdminAnomalyRange = '10m' | '1h' | '24h' | 'all' | 'custom';

interface AdminAnomalyTrendItemResponse {
  handle?: string;
  actionType?: string;
  count?: number;
}

interface AdminAnomalyTrendPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  items?: AdminAnomalyTrendItemResponse[];
}

interface AdminBlockedUserItemResponse {
  handle?: string;
  ipAddress?: string;
  blockedAt?: string;
}

interface AdminBlockedUserPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  items?: AdminBlockedUserItemResponse[];
}

interface AdminBlockedIpItemResponse {
  ipAddress?: string;
  blockedAt?: string;
}

interface AdminBlockedIpPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  items?: AdminBlockedIpItemResponse[];
}

export interface AdminAnomalyTrendItem {
  handle: string;
  actionType: string;
  count: number;
}

export interface AdminAnomalyTrendPageData {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  items: AdminAnomalyTrendItem[];
}

export interface AdminBlockedUserItem {
  handle: string;
  ipAddress: string;
  blockedAt: string;
}

export interface AdminBlockedUserPageData {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  items: AdminBlockedUserItem[];
}

export interface AdminBlockedIpItem {
  ipAddress: string;
  blockedAt: string;
}

export interface AdminBlockedIpPageData {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  items: AdminBlockedIpItem[];
}

function normalizeTrendItem(item: AdminAnomalyTrendItemResponse): AdminAnomalyTrendItem {
  return {
    handle: item.handle ?? '',
    actionType: item.actionType ?? '',
    count: item.count ?? 0,
  };
}

function normalizeTrendPage(data: AdminAnomalyTrendPageResponse): AdminAnomalyTrendPageData {
  return {
    currentPage: data.currentPage ?? 1,
    pageSize: data.pageSize ?? 10,
    totalCount: data.totalCount ?? 0,
    totalPages: Math.max(1, data.totalPages ?? 1),
    items: Array.isArray(data.items) ? data.items.map(normalizeTrendItem) : [],
  };
}

function normalizeBlockedUserItem(item: AdminBlockedUserItemResponse): AdminBlockedUserItem {
  return {
    handle: item.handle ?? '',
    ipAddress: item.ipAddress ?? '-',
    blockedAt: item.blockedAt ?? '',
  };
}

function normalizeBlockedUserPage(data: AdminBlockedUserPageResponse): AdminBlockedUserPageData {
  return {
    currentPage: data.currentPage ?? 1,
    pageSize: data.pageSize ?? 10,
    totalCount: data.totalCount ?? 0,
    totalPages: Math.max(1, data.totalPages ?? 1),
    items: Array.isArray(data.items) ? data.items.map(normalizeBlockedUserItem) : [],
  };
}

function normalizeBlockedIpItem(item: AdminBlockedIpItemResponse): AdminBlockedIpItem {
  return {
    ipAddress: item.ipAddress ?? '',
    blockedAt: item.blockedAt ?? '',
  };
}

function normalizeBlockedIpPage(data: AdminBlockedIpPageResponse): AdminBlockedIpPageData {
  return {
    currentPage: data.currentPage ?? 1,
    pageSize: data.pageSize ?? 10,
    totalCount: data.totalCount ?? 0,
    totalPages: Math.max(1, data.totalPages ?? 1),
    items: Array.isArray(data.items) ? data.items.map(normalizeBlockedIpItem) : [],
  };
}

async function requestAdminAnomaly<T>(path: string, init: RequestInit, fallbackMessage: string, normalize: (data: unknown) => T): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      credentials: 'include',
      ...init,
    });
  } catch {
    throw new Error(fallbackMessage);
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, fallbackMessage);
  }

  if (response.status === 204) {
    return normalize(undefined);
  }

  try {
    const data = (await response.json()) as unknown;
    return normalize(data);
  } catch {
    throw new Error(fallbackMessage);
  }
}

export function fetchAdminAnomalyTrends(range: AdminAnomalyRange, page: number, pageSize = 10, startedAt?: string, endedAt?: string): Promise<AdminAnomalyTrendPageData> {
  const params = new URLSearchParams({
    range,
    page: String(page),
    pageSize: String(pageSize),
  });

  if (range === 'custom' && startedAt && endedAt) {
    params.set('startedAt', startedAt);
    params.set('endedAt', endedAt);
  }

  return requestAdminAnomaly(
    `/admin/anomaly-accounts/trends?${params.toString()}`,
    { method: 'GET' },
    getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'),
    (data) =>
    normalizeTrendPage((data ?? {}) as AdminAnomalyTrendPageResponse),
  );
}

export function fetchAdminBlockedUsers(page: number, pageSize = 10): Promise<AdminBlockedUserPageData> {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });

  return requestAdminAnomaly(
    `/admin/anomaly-accounts/blocked-users?${params.toString()}`,
    { method: 'GET' },
    getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'),
    (data) =>
    normalizeBlockedUserPage((data ?? {}) as AdminBlockedUserPageResponse),
  );
}

export function fetchAdminBlockedIps(page: number, pageSize = 10): Promise<AdminBlockedIpPageData> {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });

  return requestAdminAnomaly(
    `/admin/anomaly-accounts/blocked-ips?${params.toString()}`,
    { method: 'GET' },
    getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'),
    (data) =>
    normalizeBlockedIpPage((data ?? {}) as AdminBlockedIpPageResponse),
  );
}

export function blockAdminUser(handle: string): Promise<void> {
  return requestAdminAnomaly(
    `/admin/anomaly-accounts/users/${encodeURIComponent(handle)}/block`,
    { method: 'POST' },
    getUiTextValue('ANOMALY_BLOCK_USER_FAIL_MESSAGE', '계정을 차단하지 못했습니다.'),
    () => undefined,
  );
}

export function unblockAdminUser(handle: string): Promise<void> {
  return requestAdminAnomaly(
    `/admin/anomaly-accounts/users/${encodeURIComponent(handle)}/block`,
    { method: 'DELETE' },
    getUiTextValue('ANOMALY_UNBLOCK_USER_FAIL_MESSAGE', '계정 차단을 해제하지 못했습니다.'),
    () => undefined,
  );
}

export function unblockAdminIp(ipAddress: string): Promise<void> {
  return requestAdminAnomaly(
    `/admin/anomaly-accounts/ips/${encodeURIComponent(ipAddress)}/block`,
    { method: 'DELETE' },
    getUiTextValue('ANOMALY_UNBLOCK_IP_FAIL_MESSAGE', 'IP 차단을 해제하지 못했습니다.'),
    () => undefined,
  );
}
