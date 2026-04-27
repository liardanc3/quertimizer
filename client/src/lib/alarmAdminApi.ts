import { getApiBaseUrl } from './authApi';
import { createApiErrorFromResponse, getUiTextValue } from './uiText';

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
    throw await createApiErrorFromResponse(response, fallbackMessage);
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

  return requestAdminAlarm(
    `/admin/alarms/recipients?${params.toString()}`,
    { method: 'GET' },
    getUiTextValue('ALARM_SEND_RECIPIENT_LOAD_FAIL_MESSAGE', '수신자 목록을 불러오지 못했습니다.'),
    (data) =>
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
  }, getUiTextValue('ALARM_SEND_FAIL_MESSAGE', '알림 전송에 실패했습니다.'), (data) => ((data as AdminAlarmSendResponse | null)?.sentCount ?? 0));
}
