import { getApiBaseUrl } from '@/shared/api/auth-api';
import { createApiErrorFromResponse, getUiTextValue } from '@/shared/config/ui-text';

export interface FavoriteTabSnapshotPayload {
  kind: string;
  payload: unknown;
}

export interface FavoriteTabApiEntry {
  label: string;
  path: string;
  snapshot?: FavoriteTabSnapshotPayload | null;
}

interface FavoriteTabApiResponseEntry {
  label?: string;
  path?: string;
  snapshot?: unknown;
}

interface FavoriteTabsResponse {
  tabs?: FavoriteTabApiResponseEntry[];
}

interface FavoriteTabsUpdatePayload {
  tabs: Array<{
    label: string;
    path: string;
    snapshot: unknown;
  }>;
}

function normalizeFavoriteTabResponse(data: FavoriteTabsResponse): FavoriteTabApiEntry[] {
  if (!Array.isArray(data.tabs)) {
    return [];
  }

  return data.tabs
    .filter(
      (tab): tab is Required<Pick<FavoriteTabApiResponseEntry, 'label' | 'path'>> & FavoriteTabApiResponseEntry =>
        typeof tab.label === 'string' && tab.label.trim() !== '' && typeof tab.path === 'string' && tab.path.trim() !== '',
    )
    .map((tab) => ({
      label: tab.label.trim(),
      path: tab.path.trim(),
      snapshot:
        typeof tab.snapshot === 'object' && tab.snapshot != null && 'kind' in (tab.snapshot as Record<string, unknown>)
          ? (tab.snapshot as FavoriteTabSnapshotPayload)
          : null,
    }));
}

export async function fetchMyFavoriteTabs() {
  const response = await fetch(`${getApiBaseUrl()}/profile/me/favorites`, {
    method: 'GET',
    credentials: 'include',
  });

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('FAVORITES_LOAD_FAIL_MESSAGE', '즐겨찾기를 불러오지 못했습니다.'));
  }

  try {
    return normalizeFavoriteTabResponse((await response.json()) as FavoriteTabsResponse);
  } catch {
    throw new Error(getUiTextValue('FAVORITES_PARSE_FAIL_MESSAGE', '즐겨찾기 응답 형식이 올바르지 않습니다.'));
  }
}

export async function updateMyFavoriteTabs(entries: FavoriteTabApiEntry[]) {
  const payload: FavoriteTabsUpdatePayload = {
    tabs: entries.map((entry) => ({
      label: entry.label,
      path: entry.path,
      snapshot: entry.snapshot ?? null,
    })),
  };

  const response = await fetch(`${getApiBaseUrl()}/profile/me/favorites`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('FAVORITES_SAVE_FAIL_MESSAGE', '즐겨찾기를 저장하지 못했습니다.'));
  }

  try {
    return normalizeFavoriteTabResponse((await response.json()) as FavoriteTabsResponse);
  } catch {
    throw new Error(getUiTextValue('FAVORITES_PARSE_FAIL_MESSAGE', '즐겨찾기 응답 형식이 올바르지 않습니다.'));
  }
}
