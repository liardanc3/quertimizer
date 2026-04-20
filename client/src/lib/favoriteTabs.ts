import { fetchMyFavoriteTabs, updateMyFavoriteTabs } from './favoriteTabsApi';
import { navigate } from './navigation';

export interface FavoriteTabSnapshot {
  kind: string;
  payload: unknown;
}

export interface FavoriteTabEntry {
  label: string;
  path: string;
  snapshot?: FavoriteTabSnapshot | null;
}

interface FavoriteTabsSessionState {
  isReady: boolean;
  isAuthenticated: boolean;
}

const FAVORITE_TABS_STORAGE_KEY = 'quertimizer.favoriteTabs';
const FAVORITE_TABS_CHANGE_EVENT = 'quertimizer:favorite-tabs-change';
const FAVORITE_RESTORE_STATE_KEY = 'favoriteRestore';
const FAVORITE_RESTORE_SESSION_KEY_PREFIX = 'quertimizer.favoriteRestore.';
const FAVORITE_EMBEDDED_WINDOW_NAME_PREFIX = 'quertimizer:favorite-frame:';
const MAX_FAVORITE_TAB_COUNT = 10;
const FAVORITE_TABS_PERSIST_DELAY_MS = 240;
let favoriteTabsSnapshotCache: FavoriteTabEntry[] = [];
let favoriteTabsSnapshotKeyCache = '[]';
let favoriteTabsSessionState: FavoriteTabsSessionState = {
  isReady: false,
  isAuthenticated: false,
};
let favoriteTabsPersistTimerId: number | null = null;
let favoriteTabsSyncSequence = 0;
let favoriteTabsIsApplyingServerState = false;

function isValidFavoriteTabSnapshot(value: unknown): value is FavoriteTabSnapshot {
  if (typeof value !== 'object' || value == null) {
    return false;
  }

  const candidate = value as FavoriteTabSnapshot;
  return typeof candidate.kind === 'string' && candidate.kind.trim() !== '' && 'payload' in candidate;
}

function normalizeFavoriteTabEntry(value: unknown): FavoriteTabEntry | null {
  if (typeof value !== 'object' || value == null) {
    return null;
  }

  const candidate = value as FavoriteTabEntry;
  if (typeof candidate.label !== 'string' || candidate.label.trim() === '') {
    return null;
  }

  if (typeof candidate.path !== 'string' || candidate.path.trim() === '') {
    return null;
  }

  const normalizedSnapshot = candidate.snapshot == null
    ? null
    : isValidFavoriteTabSnapshot(candidate.snapshot)
      ? candidate.snapshot
      : null;

  return {
    label: candidate.label.trim(),
    path: candidate.path.trim(),
    snapshot: normalizedSnapshot,
  };
}

function serializeFavoriteTabs(entries: FavoriteTabEntry[]) {
  return JSON.stringify(entries);
}

function setFavoriteTabsSnapshotCache(entries: FavoriteTabEntry[]) {
  favoriteTabsSnapshotCache = entries;
  favoriteTabsSnapshotKeyCache = serializeFavoriteTabs(entries);
}

function normalizeFavoriteTabs(entries: unknown[]) {
  const normalizedEntries: FavoriteTabEntry[] = [];

  for (const entry of entries) {
    const normalizedEntry = normalizeFavoriteTabEntry(entry);
    if (!normalizedEntry) {
      continue;
    }

    if (normalizedEntries.some((currentEntry) => currentEntry.path === normalizedEntry.path)) {
      continue;
    }

    normalizedEntries.push(normalizedEntry);
    if (normalizedEntries.length >= MAX_FAVORITE_TAB_COUNT) {
      break;
    }
  }

  return normalizedEntries;
}

function mergeFavoriteTabs(primaryEntries: FavoriteTabEntry[], secondaryEntries: FavoriteTabEntry[]) {
  return normalizeFavoriteTabs([...primaryEntries, ...secondaryEntries]);
}

function buildFavoriteRestoreSessionKey(token: string) {
  return `${FAVORITE_RESTORE_SESSION_KEY_PREFIX}${token}`;
}

function readFavoriteEmbeddedToken() {
  if (typeof window === 'undefined') {
    return null;
  }

  return window.name.startsWith(FAVORITE_EMBEDDED_WINDOW_NAME_PREFIX)
    ? window.name.slice(FAVORITE_EMBEDDED_WINDOW_NAME_PREFIX.length)
    : null;
}

function readFavoriteRestoreSnapshotFromSessionStorage() {
  if (typeof window === 'undefined') {
    return null as FavoriteTabSnapshot | null;
  }

  const token = readFavoriteEmbeddedToken();
  if (!token) {
    return null as FavoriteTabSnapshot | null;
  }

  try {
    const rawValue = window.sessionStorage.getItem(buildFavoriteRestoreSessionKey(token));
    if (!rawValue) {
      return null as FavoriteTabSnapshot | null;
    }

    const parsedValue = JSON.parse(rawValue);
    return isValidFavoriteTabSnapshot(parsedValue) ? parsedValue : null;
  } catch {
    return null as FavoriteTabSnapshot | null;
  }
}

function readStoredFavoriteTabs() {
  if (typeof window === 'undefined') {
    return favoriteTabsSnapshotCache;
  }

  try {
    const rawValue = window.localStorage.getItem(FAVORITE_TABS_STORAGE_KEY);
    if (!rawValue) {
      if (favoriteTabsSnapshotKeyCache !== '[]') {
        setFavoriteTabsSnapshotCache([]);
      }

      return favoriteTabsSnapshotCache;
    }

    const parsedValue = JSON.parse(rawValue);
    if (!Array.isArray(parsedValue)) {
      if (favoriteTabsSnapshotKeyCache !== '[]') {
        setFavoriteTabsSnapshotCache([]);
      }

      return favoriteTabsSnapshotCache;
    }

    const normalizedEntries = normalizeFavoriteTabs(parsedValue);
    const nextSnapshotKey = serializeFavoriteTabs(normalizedEntries);

    if (nextSnapshotKey !== favoriteTabsSnapshotKeyCache) {
      setFavoriteTabsSnapshotCache(normalizedEntries);
    }

    return favoriteTabsSnapshotCache;
  } catch {
    if (favoriteTabsSnapshotKeyCache !== '[]') {
      setFavoriteTabsSnapshotCache([]);
    }

    return favoriteTabsSnapshotCache;
  }
}

function cancelFavoriteTabsPersist() {
  if (favoriteTabsPersistTimerId == null || typeof window === 'undefined') {
    return;
  }

  window.clearTimeout(favoriteTabsPersistTimerId);
  favoriteTabsPersistTimerId = null;
}

async function persistFavoriteTabsToServer() {
  if (!favoriteTabsSessionState.isReady || !favoriteTabsSessionState.isAuthenticated) {
    return;
  }

  try {
    const nextEntries = await updateMyFavoriteTabs(readStoredFavoriteTabs());
    favoriteTabsIsApplyingServerState = true;
    replaceStoredFavoriteTabs(nextEntries, { persistToServer: false });
  } catch {
    // keep local snapshot and try again on the next update
  } finally {
    favoriteTabsIsApplyingServerState = false;
  }
}

function scheduleFavoriteTabsPersist() {
  if (!favoriteTabsSessionState.isReady || !favoriteTabsSessionState.isAuthenticated || typeof window === 'undefined') {
    return;
  }

  cancelFavoriteTabsPersist();
  favoriteTabsPersistTimerId = window.setTimeout(() => {
    favoriteTabsPersistTimerId = null;
    void persistFavoriteTabsToServer();
  }, FAVORITE_TABS_PERSIST_DELAY_MS);
}

function writeStoredFavoriteTabs(entries: FavoriteTabEntry[], { persistToServer = true }: { persistToServer?: boolean } = {}) {
  if (typeof window === 'undefined') {
    return;
  }

  const normalizedEntries = normalizeFavoriteTabs(entries);
  const nextSnapshotKey = serializeFavoriteTabs(normalizedEntries);

  if (nextSnapshotKey === favoriteTabsSnapshotKeyCache) {
    return;
  }

  window.localStorage.setItem(FAVORITE_TABS_STORAGE_KEY, nextSnapshotKey);
  setFavoriteTabsSnapshotCache(normalizedEntries);
  window.dispatchEvent(new Event(FAVORITE_TABS_CHANGE_EVENT));

  if (persistToServer && !favoriteTabsIsApplyingServerState) {
    scheduleFavoriteTabsPersist();
  }
}

function replaceStoredFavoriteTabs(entries: FavoriteTabEntry[], { persistToServer = false }: { persistToServer?: boolean } = {}) {
  writeStoredFavoriteTabs(entries, { persistToServer });
}

function clearStoredFavoriteTabs() {
  if (typeof window === 'undefined') {
    return;
  }

  cancelFavoriteTabsPersist();
  window.localStorage.removeItem(FAVORITE_TABS_STORAGE_KEY);
  setFavoriteTabsSnapshotCache([]);
  window.dispatchEvent(new Event(FAVORITE_TABS_CHANGE_EVENT));
}

export function subscribeFavoriteTabs(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  function handleStorage(event: StorageEvent) {
    if (event.key == null || event.key === FAVORITE_TABS_STORAGE_KEY) {
      callback();
    }
  }

  window.addEventListener(FAVORITE_TABS_CHANGE_EVENT, callback);
  window.addEventListener('storage', handleStorage);

  return () => {
    window.removeEventListener(FAVORITE_TABS_CHANGE_EVENT, callback);
    window.removeEventListener('storage', handleStorage);
  };
}

export function getFavoriteTabsSnapshot() {
  return readStoredFavoriteTabs();
}

export function isFavoriteTab(path: string) {
  return readStoredFavoriteTabs().some((entry) => entry.path === path);
}

export function saveFavoriteTab(entry: FavoriteTabEntry) {
  const normalizedEntry = normalizeFavoriteTabEntry(entry);
  if (!normalizedEntry) {
    return;
  }

  const currentEntries = readStoredFavoriteTabs();
  const nextEntries = [normalizedEntry, ...currentEntries.filter((currentEntry) => currentEntry.path !== normalizedEntry.path)]
    .slice(0, MAX_FAVORITE_TAB_COUNT);

  writeStoredFavoriteTabs(nextEntries);
}

export function toggleFavoriteTab(entry: FavoriteTabEntry) {
  if (isFavoriteTab(entry.path)) {
    removeFavoriteTab(entry.path);
    return;
  }

  saveFavoriteTab(entry);
}

export function removeFavoriteTab(path: string) {
  const currentEntries = readStoredFavoriteTabs();
  const nextEntries = currentEntries.filter((entry) => entry.path !== path);

  if (nextEntries.length !== currentEntries.length) {
    writeStoredFavoriteTabs(nextEntries);
  }
}

export async function handleFavoriteTabsSessionState(nextSessionState: FavoriteTabsSessionState) {
  const previousSessionState = favoriteTabsSessionState;
  favoriteTabsSessionState = nextSessionState;

  if (!nextSessionState.isReady) {
    return;
  }

  if (!nextSessionState.isAuthenticated) {
    if (previousSessionState.isAuthenticated) {
      clearStoredFavoriteTabs();
    }
    return;
  }

  const syncSequence = ++favoriteTabsSyncSequence;
  const localEntries = readStoredFavoriteTabs();

  try {
    const serverEntries = normalizeFavoriteTabs(await fetchMyFavoriteTabs());
    if (favoriteTabsSyncSequence != syncSequence || !favoriteTabsSessionState.isAuthenticated) {
      return;
    }

    const mergedEntries = mergeFavoriteTabs(localEntries, serverEntries);
    favoriteTabsIsApplyingServerState = true;
    replaceStoredFavoriteTabs(mergedEntries, { persistToServer: false });
    favoriteTabsIsApplyingServerState = false;

    if (serializeFavoriteTabs(mergedEntries) !== serializeFavoriteTabs(serverEntries)) {
      scheduleFavoriteTabsPersist();
    }
  } catch {
    // keep local snapshot if the server sync fails
  } finally {
    favoriteTabsIsApplyingServerState = false;
  }
}

export function navigateToFavoriteTab(entry: FavoriteTabEntry) {
  const nextState = {
    ...(window.history.state ?? {}),
    [FAVORITE_RESTORE_STATE_KEY]: entry.snapshot ?? null,
  };

  navigate(entry.path, { state: nextState });
}

export function prepareFavoriteEmbeddedRender(entry: FavoriteTabEntry) {
  if (typeof window === 'undefined') {
    return null as { token: string; frameName: string } | null;
  }

  const token = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
  const frameName = `${FAVORITE_EMBEDDED_WINDOW_NAME_PREFIX}${token}`;

  try {
    if (entry.snapshot) {
      window.sessionStorage.setItem(buildFavoriteRestoreSessionKey(token), JSON.stringify(entry.snapshot));
    } else {
      window.sessionStorage.removeItem(buildFavoriteRestoreSessionKey(token));
    }
  } catch {
  }

  return { token, frameName };
}

export function isFavoriteEmbeddedRender() {
  return readFavoriteEmbeddedToken() != null;
}

export function readFavoriteRestoreSnapshot<T>(kind: string) {
  if (typeof window === 'undefined') {
    return null as T | null;
  }

  const restoreSnapshot =
    ((window.history.state?.[FAVORITE_RESTORE_STATE_KEY] ?? null) as FavoriteTabSnapshot | null)
    ?? readFavoriteRestoreSnapshotFromSessionStorage();
  if (!restoreSnapshot || restoreSnapshot.kind !== kind) {
    return null as T | null;
  }

  return restoreSnapshot.payload as T;
}

export function clearFavoriteRestoreSnapshot(kind?: string) {
  if (typeof window === 'undefined') {
    return;
  }

  const currentState = window.history.state ?? {};
  const restoreSnapshot = (currentState[FAVORITE_RESTORE_STATE_KEY] ?? null) as FavoriteTabSnapshot | null;
  const embeddedRestoreSnapshot = readFavoriteRestoreSnapshotFromSessionStorage();
  const embeddedToken = readFavoriteEmbeddedToken();

  if (restoreSnapshot && (!kind || restoreSnapshot.kind === kind)) {
    const nextState = { ...currentState } as Record<string, unknown>;
    delete nextState[FAVORITE_RESTORE_STATE_KEY];
    window.history.replaceState(nextState, '', `${window.location.pathname}${window.location.search}${window.location.hash}`);
  }

  if (embeddedToken && embeddedRestoreSnapshot && (!kind || embeddedRestoreSnapshot.kind === kind)) {
    try {
      window.sessionStorage.removeItem(buildFavoriteRestoreSessionKey(embeddedToken));
    } catch {
    }
  }
}

export function getMaxFavoriteTabCount() {
  return MAX_FAVORITE_TAB_COUNT;
}
