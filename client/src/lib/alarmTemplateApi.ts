import { getApiBaseUrl } from './authApi';
import { createApiErrorFromResponse, getUiTextValue } from './uiText';

interface AlarmTemplateResponse {
  type?: string;
  sentence?: string;
  description?: string;
}

export interface AlarmTemplateData {
  type: string;
  sentence: string;
  description: string;
}

function normalizeAlarmTemplate(data: AlarmTemplateResponse): AlarmTemplateData {
  return {
    type: data.type ?? '',
    sentence: data.sentence ?? '',
    description: data.description ?? '',
  };
}

async function requestAlarmTemplate<T>(path: string, init: RequestInit, fallbackMessage: string, normalize: (data: unknown) => T): Promise<T> {
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

export function fetchAdminAlarmTemplates(): Promise<AlarmTemplateData[]> {
  return requestAlarmTemplate(
    '/admin/alarm-templates',
    { method: 'GET' },
    getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'),
    (data) => Array.isArray(data) ? data.map((item) => normalizeAlarmTemplate(item as AlarmTemplateResponse)) : [],
  );
}

export function updateAlarmTemplate(type: string, payload: Omit<AlarmTemplateData, 'type'>): Promise<AlarmTemplateData> {
  return requestAlarmTemplate(
    `/admin/alarm-templates/${encodeURIComponent(type)}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
    getUiTextValue('ALARM_TEMPLATE_UPDATE_FAIL_MESSAGE', '알람 템플릿을 수정하지 못했습니다.'),
    (data) => normalizeAlarmTemplate((data ?? {}) as AlarmTemplateResponse),
  );
}
