import { useEffect, useState } from 'react';
import { getApiBaseUrl } from './authApi';

interface UiTextResponse {
  key?: string;
  value?: string;
  language?: string;
  description?: string;
}

interface ExceptionResponse {
  reasons?: string[];
}

export interface UiTextData {
  key: string;
  value: string;
  language: string;
  description: string;
}

export const DEFAULT_SITE_TITLE = 'Quertimizer';
const TITLE_UI_TEXT_KEY = 'TITLE';

function isKoreanLocale(value: string) {
  const normalizedValue = value.toLowerCase().replace(/_/g, '-');

  return normalizedValue === 'kr' || normalizedValue.startsWith('ko') || normalizedValue.endsWith('-kr');
}

function isKoreanTimeZone() {
  if (typeof Intl === 'undefined' || typeof Intl.DateTimeFormat !== 'function') {
    return false;
  }

  const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  return typeof timeZone === 'string' && timeZone.toLowerCase() === 'asia/seoul';
}

function toUiTextData(data: UiTextResponse): UiTextData {
  if (
    typeof data.key !== 'string' ||
    typeof data.value !== 'string' ||
    typeof data.language !== 'string' ||
    typeof data.description !== 'string'
  ) {
    throw new Error('Failed to parse UI text.');
  }

  return {
    key: data.key,
    value: data.value,
    language: data.language,
    description: data.description,
  };
}

async function getErrorMessage(response: Response, fallbackMessage: string) {
  try {
    const data = (await response.json()) as ExceptionResponse;

    if (Array.isArray(data.reasons) && typeof data.reasons[0] === 'string' && data.reasons[0].trim() !== '') {
      return data.reasons[0];
    }
  } catch {
  }

  return fallbackMessage;
}

export function resolveUiTextLanguage() {
  if (typeof navigator === 'undefined') {
    return 'default';
  }

  const localeCandidates = [
    ...(Array.isArray(navigator.languages) ? navigator.languages : []),
    navigator.language,
    typeof Intl !== 'undefined' && typeof Intl.DateTimeFormat === 'function'
      ? Intl.DateTimeFormat().resolvedOptions().locale
      : undefined,
  ].filter((value): value is string => typeof value === 'string' && value.trim() !== '');

  return localeCandidates.some(isKoreanLocale) || isKoreanTimeZone() ? 'kr' : 'default';
}

export async function fetchUiText(key: string, language: string): Promise<UiTextData> {
  let response: Response;

  const searchParams = new URLSearchParams({ language });

  try {
    response = await fetch(`${getApiBaseUrl()}/ui-texts/${encodeURIComponent(key)}?${searchParams.toString()}`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('Failed to fetch UI text.');
  }

  if (!response.ok) {
    throw new Error('Failed to fetch UI text.');
  }

  try {
    return toUiTextData((await response.json()) as UiTextResponse);
  } catch {
    throw new Error('Failed to parse UI text.');
  }
}

export async function fetchAdminUiTexts(): Promise<UiTextData[]> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/ui-texts`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('전역 상수 목록을 불러오지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '전역 상수 목록을 불러오지 못했다.'));
  }

  try {
    const data = (await response.json()) as UiTextResponse[];
    if (!Array.isArray(data)) {
      throw new Error();
    }

    return data.map(toUiTextData);
  } catch {
    throw new Error('전역 상수 목록 응답을 해석하지 못했다.');
  }
}

export async function createUiText(payload: UiTextData): Promise<UiTextData> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/ui-texts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch {
    throw new Error('전역 상수를 생성하지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '전역 상수를 생성하지 못했다.'));
  }

  try {
    return toUiTextData((await response.json()) as UiTextResponse);
  } catch {
    throw new Error('전역 상수 생성 응답을 해석하지 못했다.');
  }
}

export async function updateUiText(originalKey: string, originalLanguage: string, payload: UiTextData): Promise<UiTextData> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/ui-texts/${encodeURIComponent(originalKey)}/${encodeURIComponent(originalLanguage)}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch {
    throw new Error('전역 상수를 수정하지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '전역 상수를 수정하지 못했다.'));
  }

  try {
    return toUiTextData((await response.json()) as UiTextResponse);
  } catch {
    throw new Error('전역 상수 수정 응답을 해석하지 못했다.');
  }
}

export async function deleteUiText(key: string, language: string): Promise<void> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/ui-texts/${encodeURIComponent(key)}/${encodeURIComponent(language)}`, {
      method: 'DELETE',
      credentials: 'include',
    });
  } catch {
    throw new Error('전역 상수를 삭제하지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '전역 상수를 삭제하지 못했다.'));
  }
}

export function useHomeSiteTitle(overrideTitle?: string | null) {
  const [siteTitle, setSiteTitle] = useState(DEFAULT_SITE_TITLE);

  useEffect(() => {
    let cancelled = false;

    async function loadSiteTitle() {
      try {
        const uiText = await fetchUiText(TITLE_UI_TEXT_KEY, resolveUiTextLanguage());

        if (cancelled) {
          return;
        }

        setSiteTitle(uiText.value);
      } catch {
        if (!cancelled) {
          setSiteTitle(DEFAULT_SITE_TITLE);
        }
      }
    }

    void loadSiteTitle();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    document.title = overrideTitle && overrideTitle.trim() !== '' ? overrideTitle : siteTitle;
  }, [overrideTitle, siteTitle]);
}
