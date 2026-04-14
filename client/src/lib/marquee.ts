import { getApiBaseUrl } from './authApi';

const MARQUEE_CHANGE_EVENT = 'quertimizer:marquee-change';

type MarqueeTarget = 'all' | 'guest' | 'user' | 'admin' | 'problemGenerator';
type MarqueeMode = 'repeat' | 'schedule';
type MarqueeSchedulePattern = 'always' | 'daily' | 'weekdays' | 'weekend';

interface ExceptionResponse {
  reasons?: string[];
}

interface MarqueeItemResponse {
  marqueeId?: number;
  targets?: string[];
  message?: string;
  mode?: string;
  startedAt?: string | null;
  repeatCount?: number | null;
  schedulePattern?: string | null;
  scheduleTime?: string | null;
  active?: boolean;
}

interface MarqueeManageResponse {
  items?: MarqueeItemResponse[];
}

interface MarqueeMessagesResponse {
  messages?: string[];
}

export interface MarqueeItemData {
  marqueeId: number;
  targets: MarqueeTarget[];
  message: string;
  mode: MarqueeMode;
  startedAt: string | null;
  repeatCount: number | null;
  schedulePattern: MarqueeSchedulePattern | null;
  scheduleTime: string | null;
  active: boolean;
}

export interface MarqueeSavePayload {
  targets: MarqueeTarget[];
  message: string;
  mode: MarqueeMode;
  startedAt: string | null;
  repeatCount: number | null;
  schedulePattern: MarqueeSchedulePattern | null;
  scheduleTime: string | null;
}

function isMarqueeTarget(value: string): value is MarqueeTarget {
  return value === 'all' || value === 'guest' || value === 'user' || value === 'admin' || value === 'problemGenerator';
}

function isMarqueeMode(value: string): value is MarqueeMode {
  return value === 'repeat' || value === 'schedule';
}

function isMarqueeSchedulePattern(value: string): value is MarqueeSchedulePattern {
  return value === 'always' || value === 'daily' || value === 'weekdays' || value === 'weekend';
}

function parseMarqueeItem(data: MarqueeItemResponse): MarqueeItemData | null {
  if (typeof data.marqueeId !== 'number' || !Array.isArray(data.targets) || typeof data.message !== 'string' || typeof data.mode !== 'string') {
    return null;
  }

  const targets = data.targets.filter((target): target is MarqueeTarget => typeof target === 'string' && isMarqueeTarget(target));
  if (targets.length === 0 || !isMarqueeMode(data.mode)) {
    return null;
  }

  return {
    marqueeId: data.marqueeId,
    targets,
    message: data.message,
    mode: data.mode,
    startedAt: typeof data.startedAt === 'string' && data.startedAt.trim() !== '' ? data.startedAt : null,
    repeatCount: typeof data.repeatCount === 'number' ? data.repeatCount : null,
    schedulePattern:
      typeof data.schedulePattern === 'string' && isMarqueeSchedulePattern(data.schedulePattern) ? data.schedulePattern : null,
    scheduleTime: typeof data.scheduleTime === 'string' && data.scheduleTime.trim() !== '' ? data.scheduleTime : null,
    active: data.active === true,
  };
}

async function getErrorMessage(response: Response, fallbackMessage: string) {
  try {
    const data = (await response.json()) as ExceptionResponse;
    if (Array.isArray(data.reasons) && data.reasons.length > 0) {
      return data.reasons[0] ?? fallbackMessage;
    }
  } catch {
  }

  return fallbackMessage;
}

export function notifyMarqueeChanged() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(MARQUEE_CHANGE_EVENT));
}

export function subscribeMarqueeChange(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(MARQUEE_CHANGE_EVENT, callback);
  return () => window.removeEventListener(MARQUEE_CHANGE_EVENT, callback);
}

export async function fetchAdminMarquees(): Promise<MarqueeItemData[]> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/marquees`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('전광판 목록을 불러오지 못했다.');
  }

  if (!response.ok) {
    throw new Error('전광판 목록을 불러오지 못했다.');
  }

  try {
    const data = (await response.json()) as MarqueeManageResponse;

    return Array.isArray(data.items) ? data.items.map(parseMarqueeItem).filter((item): item is MarqueeItemData => item !== null) : [];
  } catch {
    throw new Error('전광판 목록을 불러오지 못했다.');
  }
}

export async function saveMarquee(payload: MarqueeSavePayload, marqueeId?: number) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/marquees${typeof marqueeId === 'number' ? `/${marqueeId}` : ''}`, {
      method: typeof marqueeId === 'number' ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch {
    throw new Error('전광판을 저장하지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '전광판을 저장하지 못했다.'));
  }

  notifyMarqueeChanged();
}

export async function deleteMarquee(marqueeId: number) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/marquees/${marqueeId}`, {
      method: 'DELETE',
      credentials: 'include',
    });
  } catch {
    throw new Error('전광판을 삭제하지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '전광판을 삭제하지 못했다.'));
  }

  notifyMarqueeChanged();
}

export async function fetchVisibleMarqueeMessages(): Promise<string[]> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/marquee`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('전광판 문구를 불러오지 못했다.');
  }

  if (!response.ok) {
    throw new Error('전광판 문구를 불러오지 못했다.');
  }

  try {
    const data = (await response.json()) as MarqueeMessagesResponse;
    return Array.isArray(data.messages)
      ? data.messages.filter((message): message is string => typeof message === 'string' && message.trim() !== '')
      : [];
  } catch {
    throw new Error('전광판 문구를 불러오지 못했다.');
  }
}
