import { useEffect, useSyncExternalStore } from 'react';
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

interface UiTextSnapshot {
  language: string;
  isReady: boolean;
  items: Record<string, UiTextData>;
}

export const DEFAULT_SITE_TITLE = 'Quertimizer';
export const DEFAULT_NOTIFICATION_TEXT = 'Check out the latest updates from Quertimizer.';
export const TITLE_UI_TEXT_KEY = 'TITLE';
export const NOTIFICATION_UI_TEXT_KEY = 'NOTIFICATION';

const DEFAULT_LANGUAGE = 'default';
const UI_TEXT_CHANGE_EVENT = 'quertimizer:ui-text-change';

let uiTextSnapshot: UiTextSnapshot = {
  language: DEFAULT_LANGUAGE,
  isReady: false,
  items: {},
};
let preloadUiTextsPromise: Promise<void> | null = null;

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

function emitUiTextChange() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(UI_TEXT_CHANGE_EVENT));
}

function updateUiTextSnapshot(nextSnapshot: UiTextSnapshot) {
  uiTextSnapshot = nextSnapshot;
  emitUiTextChange();
}

function createUiTextMap(uiTexts: UiTextData[]) {
  return uiTexts.reduce<Record<string, UiTextData>>((nextItems, uiText) => {
    nextItems[uiText.key] = uiText;
    return nextItems;
  }, {});
}

function subscribeUiTexts(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(UI_TEXT_CHANGE_EVENT, callback);

  return () => {
    window.removeEventListener(UI_TEXT_CHANGE_EVENT, callback);
  };
}

function getUiTextSnapshot() {
  return uiTextSnapshot;
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

function normalizeUiTextLanguage(language: string) {
  return language.trim() !== '' ? language : DEFAULT_LANGUAGE;
}

export function resolveUiTextLanguage() {
  if (typeof navigator === 'undefined') {
    return DEFAULT_LANGUAGE;
  }

  const localeCandidates = [
    ...(Array.isArray(navigator.languages) ? navigator.languages : []),
    navigator.language,
    typeof Intl !== 'undefined' && typeof Intl.DateTimeFormat === 'function'
      ? Intl.DateTimeFormat().resolvedOptions().locale
      : undefined,
  ].filter((value): value is string => typeof value === 'string' && value.trim() !== '');

  return localeCandidates.some(isKoreanLocale) || isKoreanTimeZone() ? 'kr' : DEFAULT_LANGUAGE;
}

export async function fetchUiTexts(language: string): Promise<UiTextData[]> {
  let response: Response;

  const searchParams = new URLSearchParams({ language });

  try {
    response = await fetch(`${getApiBaseUrl()}/ui-texts?${searchParams.toString()}`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('Failed to fetch UI texts.');
  }

  if (!response.ok) {
    throw new Error('Failed to fetch UI texts.');
  }

  try {
    const data = (await response.json()) as UiTextResponse[];
    if (!Array.isArray(data)) {
      throw new Error();
    }

    return data.map(toUiTextData);
  } catch {
    throw new Error('Failed to parse UI texts.');
  }
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

export async function preloadUiTexts(language = resolveUiTextLanguage(), force = false) {
  const requestedLanguage = normalizeUiTextLanguage(language);
  const currentItems = uiTextSnapshot.items;

  if (!force && uiTextSnapshot.isReady && uiTextSnapshot.language === requestedLanguage) {
    return;
  }

  if (!force && preloadUiTextsPromise != null && uiTextSnapshot.language === requestedLanguage) {
    return preloadUiTextsPromise;
  }

  preloadUiTextsPromise = (async () => {
    try {
      const uiTexts = await fetchUiTexts(requestedLanguage);
      updateUiTextSnapshot({
        language: requestedLanguage,
        isReady: true,
        items: createUiTextMap(uiTexts),
      });
    } catch {
      updateUiTextSnapshot({
        language: requestedLanguage,
        isReady: true,
        items: force ? currentItems : {},
      });
    } finally {
      preloadUiTextsPromise = null;
    }
  })();

  return preloadUiTextsPromise;
}

export async function refreshCachedUiTexts(language = uiTextSnapshot.isReady ? uiTextSnapshot.language : resolveUiTextLanguage()) {
  await preloadUiTexts(language, true);
}

export async function fetchAdminUiTexts(): Promise<UiTextData[]> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/ui-texts`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('Failed to fetch admin UI texts.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, 'Failed to fetch admin UI texts.'));
  }

  try {
    const data = (await response.json()) as UiTextResponse[];
    if (!Array.isArray(data)) {
      throw new Error();
    }

    return data.map(toUiTextData);
  } catch {
    throw new Error('Failed to parse admin UI texts.');
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
    throw new Error('Failed to create UI text.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, 'Failed to create UI text.'));
  }

  try {
    const nextUiText = toUiTextData((await response.json()) as UiTextResponse);
    void refreshCachedUiTexts();
    return nextUiText;
  } catch {
    throw new Error('Failed to parse created UI text.');
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
    throw new Error('Failed to update UI text.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, 'Failed to update UI text.'));
  }

  try {
    const nextUiText = toUiTextData((await response.json()) as UiTextResponse);
    void refreshCachedUiTexts();
    return nextUiText;
  } catch {
    throw new Error('Failed to parse updated UI text.');
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
    throw new Error('Failed to delete UI text.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, 'Failed to delete UI text.'));
  }

  void refreshCachedUiTexts();
}

export function useUiTextValue(key: string, fallbackValue: string) {
  const snapshot = useSyncExternalStore(subscribeUiTexts, getUiTextSnapshot, () => ({
    language: DEFAULT_LANGUAGE,
    isReady: false,
    items: {},
  }));

  useEffect(() => {
    void preloadUiTexts();
  }, []);

  const uiText = snapshot.items[key];
  return typeof uiText?.value === 'string' && uiText.value.trim() !== '' ? uiText.value : fallbackValue;
}

export function useHomeSiteTitle(overrideTitle?: string | null) {
  const siteTitle = useUiTextValue(TITLE_UI_TEXT_KEY, DEFAULT_SITE_TITLE);

  useEffect(() => {
    document.title = overrideTitle && overrideTitle.trim() !== '' ? overrideTitle : siteTitle;
  }, [overrideTitle, siteTitle]);
}
