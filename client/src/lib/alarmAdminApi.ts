import { getApiBaseUrl } from './authApi';

interface AlarmRecipientResponse {
  handle?: string;
}

interface AdminAlarmSendResponse {
  sentCount?: number;
}

export interface AdminAlarmSendPayload {
  recipientHandles: string[];
  message: string;
}

async function requestAdminAlarm<T>(path: string, init: RequestInit, fallbackMessage: string, normalize: (data: unknown) => T): Promise<T> {
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

  try {
    const data = (await response.json()) as unknown;
    return normalize(data);
  } catch {
    throw new Error(fallbackMessage);
  }
}

export function fetchAdminAlarmRecipients(keyword: string): Promise<string[]> {
  const params = new URLSearchParams({ keyword });

  return requestAdminAlarm(`/admin/alarms/recipients?${params.toString()}`, { method: 'GET' }, '수신자 목록을 불러오지 못했다.', (data) =>
    Array.isArray(data)
      ? data
          .map((item) => ((item as AlarmRecipientResponse).handle ?? '').trim())
          .filter((handle) => handle !== '')
      : [],
  );
}

export function sendAdminAlarm(payload: AdminAlarmSendPayload): Promise<number> {
  return requestAdminAlarm('/admin/alarms/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  }, '알람 전송에 실패했다.', (data) => ((data as AdminAlarmSendResponse | null)?.sentCount ?? 0));
}
