import { getApiBaseUrl } from './authApi';

interface AlarmBindingResponse {
  text?: string;
  path?: string;
  hash?: string | null;
}

interface AlarmItemResponse {
  alarmId?: number;
  alarmType?: string;
  title?: string;
  message?: string;
  sentence?: string;
  description?: string;
  bindings?: Record<string, AlarmBindingResponse>;
  targetPath?: string;
  targetHash?: string | null;
  read?: boolean;
  createdAt?: string;
}

interface AlarmPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  unreadCount?: number;
  alarms?: AlarmItemResponse[];
}

export interface AlarmBinding {
  text: string;
  path?: string;
  hash?: string;
}

export interface AlarmEntry {
  alarmId: number;
  alarmType: string;
  title: string;
  message: string;
  sentence: string;
  description: string;
  bindings: Record<string, AlarmBinding>;
  targetPath: string;
  targetHash?: string;
  read: boolean;
  createdAt: string;
}

export interface AlarmPageData {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  unreadCount: number;
  alarms: AlarmEntry[];
}

function normalizeBinding(binding: AlarmBindingResponse): AlarmBinding {
  return {
    text: binding.text ?? '',
    path: typeof binding.path === 'string' && binding.path.trim() !== '' ? binding.path : undefined,
    hash: typeof binding.hash === 'string' && binding.hash.trim() !== '' ? binding.hash : undefined,
  };
}

function normalizeAlarm(alarm: AlarmItemResponse): AlarmEntry {
  const nextBindings: Record<string, AlarmBinding> = {};

  Object.entries(alarm.bindings ?? {}).forEach(([key, binding]) => {
    nextBindings[key] = normalizeBinding(binding ?? {});
  });

  return {
    alarmId: alarm.alarmId ?? 0,
    alarmType: alarm.alarmType ?? '',
    title: alarm.title ?? '',
    message: alarm.message ?? '',
    sentence: alarm.sentence ?? '',
    description: alarm.description ?? '',
    bindings: nextBindings,
    targetPath: alarm.targetPath ?? '/',
    targetHash: typeof alarm.targetHash === 'string' && alarm.targetHash.trim() !== '' ? alarm.targetHash : undefined,
    read: alarm.read === true,
    createdAt: alarm.createdAt ?? '',
  };
}

function normalizeAlarmPage(data: AlarmPageResponse): AlarmPageData {
  return {
    currentPage: data.currentPage ?? 1,
    pageSize: data.pageSize ?? 5,
    totalCount: data.totalCount ?? 0,
    totalPages: Math.max(1, data.totalPages ?? 1),
    unreadCount: data.unreadCount ?? 0,
    alarms: Array.isArray(data.alarms) ? data.alarms.map(normalizeAlarm) : [],
  };
}

async function requestAlarm<T>(path: string, init: RequestInit, fallbackMessage: string, normalize: (data: unknown) => T): Promise<T> {
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
    throw new Error(fallbackMessage);
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

export function fetchAlarms(page: number, pageSize?: number): Promise<AlarmPageData> {
  const params = new URLSearchParams({
    page: String(page),
  });

  if (typeof pageSize === 'number' && Number.isFinite(pageSize) && pageSize > 0) {
    params.set('pageSize', String(pageSize));
  }

  return requestAlarm(`/alarms?${params.toString()}`, { method: 'GET' }, '알람 목록을 불러오지 못했다.', (data) =>
    normalizeAlarmPage((data ?? {}) as AlarmPageResponse),
  );
}

export function markAlarmRead(alarmId: number): Promise<void> {
  return requestAlarm(`/alarms/${alarmId}/read`, { method: 'POST' }, '알람 읽음 처리에 실패했다.', () => undefined);
}

export function markAllAlarmsRead(): Promise<void> {
  return requestAlarm('/alarms/read-all', { method: 'POST' }, '알람 읽음 처리에 실패했다.', () => undefined);
}
