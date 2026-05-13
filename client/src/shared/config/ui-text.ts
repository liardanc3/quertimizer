import { useEffect, useSyncExternalStore } from 'react';
import { getApiBaseUrl } from '@/shared/api/api-base-url';
import { resolveHttpErrorMessage, resolveHttpErrorReasons, toApiError } from '@/shared/api/api-error';
import { defaultUiTextMap } from '@/shared/config/default-ui-texts';

interface UiTextResponse {
  key?: string;
  value?: string | null;
  language?: string;
  description?: string;
}

interface UiTextPageResponse {
  currentPage?: number;
  pageSize?: number;
  totalCount?: number;
  totalPages?: number;
  uiTexts?: UiTextResponse[];
}

export interface UiTextData {
  key: string;
  value: string;
  language: string;
  description: string;
}

export interface UiTextAdminPageData {
  currentPage: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  uiTexts: UiTextData[];
}

interface UiTextRuntimeData {
  key: string;
  value?: string;
  language: string;
  description: string;
}

interface UiTextSnapshot {
  language: string;
  isReady: boolean;
  fetchedAt: number | null;
  items: Record<string, UiTextRuntimeData>;
}

interface CachedUiTextSnapshot {
  language: string;
  fetchedAt: number;
  items: UiTextRuntimeData[];
}

export type UiTextParams = Record<string, string | number | boolean | null | undefined>;

export const TITLE_UI_TEXT_KEY = 'TITLE';
export const NOTIFICATION_UI_TEXT_KEY = 'NOTIFICATION';
export const DEFAULT_SITE_TITLE = defaultUiTextMap[TITLE_UI_TEXT_KEY]?.value ?? 'Quertimizer';
export const DEFAULT_NOTIFICATION_TEXT =
  defaultUiTextMap[NOTIFICATION_UI_TEXT_KEY]?.value ?? 'Check out the latest updates from Quertimizer.';

const DEFAULT_LANGUAGE = 'default';
const UI_TEXT_CHANGE_EVENT = 'quertimizer:ui-text-change';
const UI_TEXT_CACHE_TTL_MS = 10 * 60 * 1000;
const UI_TEXT_CACHE_KEY_PREFIX = 'quertimizer.ui-text-cache:';

let uiTextSnapshot: UiTextSnapshot = {
  language: DEFAULT_LANGUAGE,
  isReady: false,
  fetchedAt: null,
  items: {},
};
const preloadUiTextsPromises = new Map<string, Promise<void>>();
const forcedCommonHttpUiTextLookupKeys = new Set<string>();

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

function toUiTextRuntimeData(data: UiTextResponse): UiTextRuntimeData {
  if (typeof data.key !== 'string' || typeof data.language !== 'string' || typeof data.description !== 'string') {
    throw new Error('Failed to parse UI text.');
  }

  return {
    key: data.key,
    value: typeof data.value === 'string' ? data.value : undefined,
    language: data.language,
    description: data.description,
  };
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

function toUiTextAdminPageData(data: UiTextPageResponse): UiTextAdminPageData {
  if (
    typeof data.currentPage !== 'number' ||
    typeof data.pageSize !== 'number' ||
    typeof data.totalCount !== 'number' ||
    typeof data.totalPages !== 'number' ||
    !Array.isArray(data.uiTexts)
  ) {
    throw new Error('Failed to parse admin UI text page.');
  }

  return {
    currentPage: data.currentPage,
    pageSize: data.pageSize,
    totalCount: data.totalCount,
    totalPages: data.totalPages,
    uiTexts: data.uiTexts.map(toUiTextData),
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

function createUiTextMap(uiTexts: UiTextRuntimeData[]) {
  return uiTexts.reduce<Record<string, UiTextRuntimeData>>((nextItems, uiText) => {
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

function getServerUiTextSnapshot(): UiTextSnapshot {
  return {
    language: DEFAULT_LANGUAGE,
    isReady: false,
    fetchedAt: null,
    items: {},
  };
}

function normalizeUiTextLanguage(language: string) {
  return language.trim() !== '' ? language : DEFAULT_LANGUAGE;
}

function getUiTextCacheKey(language: string) {
  return `${UI_TEXT_CACHE_KEY_PREFIX}${language}`;
}

function isUiTextSnapshotFresh(fetchedAt: number | null) {
  return typeof fetchedAt === 'number' && Date.now() - fetchedAt < UI_TEXT_CACHE_TTL_MS;
}

function readCachedUiTexts(language: string, allowExpired = false): CachedUiTextSnapshot | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    const cachedValue = window.localStorage.getItem(getUiTextCacheKey(language));
    if (cachedValue == null) {
      return null;
    }

    const parsed = JSON.parse(cachedValue) as CachedUiTextSnapshot;
    if (
      typeof parsed.language !== 'string' ||
      parsed.language !== language ||
      typeof parsed.fetchedAt !== 'number' ||
      !Array.isArray(parsed.items)
    ) {
      return null;
    }

    const items = parsed.items.map(toUiTextRuntimeData);
    if (!allowExpired && !isUiTextSnapshotFresh(parsed.fetchedAt)) {
      return null;
    }

    return {
      language,
      fetchedAt: parsed.fetchedAt,
      items,
    };
  } catch {
    return null;
  }
}

function writeCachedUiTexts(language: string, uiTexts: UiTextRuntimeData[], fetchedAt: number) {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    window.localStorage.setItem(
      getUiTextCacheKey(language),
      JSON.stringify({
        language,
        fetchedAt,
        items: uiTexts,
      } satisfies CachedUiTextSnapshot),
    );
  } catch {
    // localStorage 쓰기 실패 시 캐시 없이 계속 진행
  }
}

function createSnapshotFromUiTexts(language: string, uiTexts: UiTextRuntimeData[], fetchedAt: number): UiTextSnapshot {
  return {
    language,
    isReady: true,
    fetchedAt,
    items: createUiTextMap(uiTexts),
  };
}

function isUiTextParams(value?: UiTextParams | string): value is UiTextParams {
  return value != null && typeof value === 'object' && !Array.isArray(value);
}

function resolveUiTextArguments(paramsOrFallback?: UiTextParams | string, fallbackValue?: string) {
  if (isUiTextParams(paramsOrFallback)) {
    return {
      params: paramsOrFallback,
      fallbackValue,
    };
  }

  return {
    params: undefined,
    fallbackValue: paramsOrFallback ?? fallbackValue,
  };
}

function formatUiTextTemplate(template: string, params?: UiTextParams) {
  if (params == null) {
    return template;
  }

  return template.replace(/\{([^{}]+)\}/g, (placeholder, rawKey) => {
    const key = rawKey.trim();

    if (!Object.prototype.hasOwnProperty.call(params, key) || params[key] == null) {
      return placeholder;
    }

    return String(params[key]);
  });
}

function resolveUiTextBaseValue(key: string, fallbackValue?: string) {
  const serverUiText = uiTextSnapshot.items[key];
  if (typeof serverUiText?.value === 'string') {
    return serverUiText.value;
  }

  const defaultUiText = defaultUiTextMap[key];
  if (defaultUiText != null) {
    return defaultUiText.value;
  }

  if (fallbackValue !== undefined) {
    return fallbackValue;
  }

  return key;
}

function hasLoadedUiTextValue(key: string, language: string) {
  return uiTextSnapshot.language === language && typeof uiTextSnapshot.items[key]?.value === 'string';
}

async function resolveCommonHttpUiTextMessage(key: string, fallbackMessage: string, shouldLoadUiTexts: boolean) {
  const language = resolveUiTextLanguage();

  if (shouldLoadUiTexts && !hasLoadedUiTextValue(key, language)) {
    const lookupKey = `${language}:${key}`;
    const shouldForceRefresh = uiTextSnapshot.isReady && uiTextSnapshot.language === language;

    if (!shouldForceRefresh || !forcedCommonHttpUiTextLookupKeys.has(lookupKey)) {
      if (shouldForceRefresh) {
        forcedCommonHttpUiTextLookupKeys.add(lookupKey);
      }

      await preloadUiTexts(language, shouldForceRefresh);
    }
  }

  return getUiTextValue(key, fallbackMessage);
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

export async function resolveApiHttpErrorReasons(
  response: Response,
  fallbackMessage: string,
  { loadUiTexts = true }: { loadUiTexts?: boolean } = {},
) {
  return resolveHttpErrorReasons(response, fallbackMessage, {
    resolveCommonMessage: (key, commonFallbackMessage) => resolveCommonHttpUiTextMessage(key, commonFallbackMessage, loadUiTexts),
  });
}

export async function resolveApiHttpErrorMessage(
  response: Response,
  fallbackMessage: string,
  { loadUiTexts = true }: { loadUiTexts?: boolean } = {},
) {
  return resolveHttpErrorMessage(response, fallbackMessage, {
    resolveCommonMessage: (key, commonFallbackMessage) => resolveCommonHttpUiTextMessage(key, commonFallbackMessage, loadUiTexts),
  });
}

export async function createApiErrorFromResponse(
  response: Response,
  fallbackMessage: string,
  { loadUiTexts = true }: { loadUiTexts?: boolean } = {},
) {
  return toApiError(response.status, await resolveApiHttpErrorMessage(response, fallbackMessage, { loadUiTexts }));
}

export async function fetchUiTexts(language: string): Promise<UiTextRuntimeData[]> {
  let response: Response;
  const fallbackMessage = 'Failed to fetch UI texts.';

  const searchParams = new URLSearchParams({ language });

  try {
    response = await fetch(`${getApiBaseUrl()}/ui-texts?${searchParams.toString()}`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error(fallbackMessage);
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, fallbackMessage, { loadUiTexts: false });
  }

  try {
    const data = (await response.json()) as UiTextResponse[];
    if (!Array.isArray(data)) {
      throw new Error();
    }

    return data.map(toUiTextRuntimeData);
  } catch {
    throw new Error('Failed to parse UI texts.');
  }
}

export async function preloadUiTexts(language = resolveUiTextLanguage(), force = false) {
  const requestedLanguage = normalizeUiTextLanguage(language);
  const currentSnapshot = uiTextSnapshot.language === requestedLanguage
    ? uiTextSnapshot
    : {
        language: requestedLanguage,
        isReady: false,
        fetchedAt: null,
        items: {},
      };
  const freshCachedSnapshot = !force ? readCachedUiTexts(requestedLanguage) : null;
  const staleCachedSnapshot = readCachedUiTexts(requestedLanguage, true);

  if (!force && currentSnapshot.isReady && isUiTextSnapshotFresh(currentSnapshot.fetchedAt)) {
    return;
  }

  if (freshCachedSnapshot != null) {
    if (!currentSnapshot.isReady || currentSnapshot.fetchedAt !== freshCachedSnapshot.fetchedAt) {
      updateUiTextSnapshot(createSnapshotFromUiTexts(requestedLanguage, freshCachedSnapshot.items, freshCachedSnapshot.fetchedAt));
    }

    return;
  }

  const existingPromise = preloadUiTextsPromises.get(requestedLanguage);
  if (existingPromise != null) {
    return existingPromise;
  }

  const preloadPromise = (async () => {
    try {
      const uiTexts = await fetchUiTexts(requestedLanguage);
      const fetchedAt = Date.now();
      writeCachedUiTexts(requestedLanguage, uiTexts, fetchedAt);
      updateUiTextSnapshot(createSnapshotFromUiTexts(requestedLanguage, uiTexts, fetchedAt));
    } catch {
      const fallbackSnapshot = staleCachedSnapshot != null
        ? createSnapshotFromUiTexts(requestedLanguage, staleCachedSnapshot.items, staleCachedSnapshot.fetchedAt)
        : {
            ...currentSnapshot,
            isReady: true,
          };
      updateUiTextSnapshot(fallbackSnapshot);
    } finally {
      preloadUiTextsPromises.delete(requestedLanguage);
    }
  })();

  preloadUiTextsPromises.set(requestedLanguage, preloadPromise);
  return preloadPromise;
}

export async function refreshCachedUiTexts(
  language = uiTextSnapshot.isReady ? uiTextSnapshot.language : resolveUiTextLanguage(),
  force = false,
) {
  await preloadUiTexts(language, force);
}

export async function fetchAdminUiTexts({
  page = 1,
  pageSize = 10,
  query,
}: {
  page?: number;
  pageSize?: number;
  query?: string;
} = {}): Promise<UiTextAdminPageData> {
  let response: Response;
  const searchParams = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });

  if (query != null && query.trim() !== '') {
    searchParams.set('query', query.trim());
  }

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/ui-texts?${searchParams.toString()}`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  try {
    return toUiTextAdminPageData((await response.json()) as UiTextPageResponse);
  } catch {
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
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
    throw new Error(getUiTextValue('GLOBAL_CONFIG_CREATE_FAIL_MESSAGE', 'UI 텍스트를 생성하지 못했습니다.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('GLOBAL_CONFIG_CREATE_FAIL_MESSAGE', 'UI 텍스트를 생성하지 못했습니다.'));
  }

  try {
    const nextUiText = toUiTextData((await response.json()) as UiTextResponse);
    void refreshCachedUiTexts(undefined, true);
    return nextUiText;
  } catch {
    throw new Error(getUiTextValue('GLOBAL_CONFIG_CREATE_FAIL_MESSAGE', 'UI 텍스트를 생성하지 못했습니다.'));
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
    throw new Error(getUiTextValue('GLOBAL_CONFIG_UPDATE_FAIL_MESSAGE', 'UI 텍스트를 수정하지 못했습니다.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('GLOBAL_CONFIG_UPDATE_FAIL_MESSAGE', 'UI 텍스트를 수정하지 못했습니다.'));
  }

  try {
    const nextUiText = toUiTextData((await response.json()) as UiTextResponse);
    void refreshCachedUiTexts(undefined, true);
    return nextUiText;
  } catch {
    throw new Error(getUiTextValue('GLOBAL_CONFIG_UPDATE_FAIL_MESSAGE', 'UI 텍스트를 수정하지 못했습니다.'));
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
    throw new Error(getUiTextValue('GLOBAL_CONFIG_DELETE_FAIL_MESSAGE', 'UI 텍스트를 삭제하지 못했습니다.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('GLOBAL_CONFIG_DELETE_FAIL_MESSAGE', 'UI 텍스트를 삭제하지 못했습니다.'));
  }

  void refreshCachedUiTexts(undefined, true);
}

export function getUiTextValue(key: string, fallbackValue?: string) {
  return resolveUiTextBaseValue(key, fallbackValue);
}

export function getUiText(key: string, paramsOrFallback?: UiTextParams | string, fallbackValue?: string) {
  const { params, fallbackValue: resolvedFallbackValue } = resolveUiTextArguments(paramsOrFallback, fallbackValue);
  return formatUiTextTemplate(resolveUiTextBaseValue(key, resolvedFallbackValue), params);
}

export function useUiText() {
  const snapshot = useSyncExternalStore(subscribeUiTexts, getUiTextSnapshot, getServerUiTextSnapshot);

  useEffect(() => {
    void preloadUiTexts();
  }, []);

  return {
    isReady: snapshot.isReady,
    language: snapshot.language,
    text: getUiText,
    value: getUiTextValue,
  };
}

export function useUiTextValue(key: string, fallbackValue: string) {
  const { text } = useUiText();
  return text(key, fallbackValue);
}

export function useHomeSiteTitle(overrideTitle?: string | null) {
  const siteTitle = useUiTextValue(TITLE_UI_TEXT_KEY, DEFAULT_SITE_TITLE);

  useEffect(() => {
    document.title = overrideTitle && overrideTitle.trim() !== '' ? overrideTitle : siteTitle;
  }, [overrideTitle, siteTitle]);
}
