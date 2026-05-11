import { useSyncExternalStore } from 'react';
import { AuthApiError, fetchSessionMe, type SessionMeResult } from '@/shared/api/auth-api';
import { openLoginOverlay } from '@/shared/auth/auth-overlay';
import { handleFavoriteTabsSessionState, prepareFavoriteTabsLogoutReload } from '@/features/favorite-tab';
import { disconnectSessionSocket } from '@/shared/auth/session-socket';
import { getUiTextValue } from '@/shared/config/ui-text';

const REMEMBER_AUTH_STORAGE_KEY = 'quertimizer.remember-authenticated';
const SESSION_SNAPSHOT_STORAGE_KEY = 'quertimizer.session-snapshot';
const AUTH_CHANGE_EVENT = 'quertimizer:auth-change';
const SESSION_ALERT_CHANGE_EVENT = 'quertimizer:session-alert-change';

export interface SessionAlert {
  level: 1 | 2 | 3;
  message: string;
  confirmLabel: string;
  display?: 'popup' | 'toast';
  tone?: 'success' | 'error';
  autoDismissMs?: number;
}

interface SessionSnapshot {
  isAuthenticated: boolean;
  isReady: boolean;
  handle: string | null;
  defaultDbms: 'postgresql' | 'mysql' | null;
  role: 'user' | 'admin' | null;
  handleSetupRequired: boolean;
  reauthenticationRequired: boolean;
}

interface PersistedSessionSnapshot {
  handle: string | null;
  defaultDbms: 'postgresql' | 'mysql' | null;
  role: 'user' | 'admin' | null;
  handleSetupRequired: boolean;
}

interface SyncSessionOptions {
  openLoginOnExpire?: boolean;
  clearOnFailure?: boolean;
}

let sessionSnapshot: SessionSnapshot = readPersistedSessionSnapshot();
let sessionAlert: SessionAlert | null = null;
let syncSessionPromise: Promise<boolean> | null = null;
let sessionExpiredNoticeAt = 0;

function emitAuthChange() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
}

function updateSessionSnapshot(nextSnapshot: SessionSnapshot) {
  const shouldSyncFavoriteTabs =
    sessionSnapshot.isReady !== nextSnapshot.isReady
    || sessionSnapshot.isAuthenticated !== nextSnapshot.isAuthenticated;

  persistSessionSnapshot(nextSnapshot);
  sessionSnapshot = nextSnapshot;
  if (shouldSyncFavoriteTabs) {
    void handleFavoriteTabsSessionState({
      isReady: nextSnapshot.isReady,
      isAuthenticated: nextSnapshot.isAuthenticated,
    });
  }
  emitAuthChange();
}

function emitSessionAlertChange() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(SESSION_ALERT_CHANGE_EVENT));
}

function updateSessionAlert(nextAlert: SessionAlert | null) {
  sessionAlert = nextAlert;
  emitSessionAlertChange();
}

function subscribe(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  function handleAuthChange() {
    callback();
  }

  function handleStorageChange() {
    sessionSnapshot = readPersistedSessionSnapshot();
    callback();
  }

  window.addEventListener(AUTH_CHANGE_EVENT, handleAuthChange);
  window.addEventListener('storage', handleStorageChange);

  return () => {
    window.removeEventListener(AUTH_CHANGE_EVENT, handleAuthChange);
    window.removeEventListener('storage', handleStorageChange);
  };
}

function getSnapshot() {
  return sessionSnapshot;
}

function subscribeSessionAlert(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(SESSION_ALERT_CHANGE_EVENT, callback);

  return () => {
    window.removeEventListener(SESSION_ALERT_CHANGE_EVENT, callback);
  };
}

function getSessionAlertSnapshot() {
  return sessionAlert;
}

function persistAuthentication() {
  window.localStorage.setItem(
    REMEMBER_AUTH_STORAGE_KEY,
    JSON.stringify({
      expiredAt: Date.now() + 1000 * 60 * 60 * 24 * 30 * 6,
    })
  );
}

function clearPersistedAuthentication() {
  window.localStorage.removeItem(REMEMBER_AUTH_STORAGE_KEY);
  window.localStorage.removeItem(SESSION_SNAPSHOT_STORAGE_KEY);
}

function readPersistedAuthentication() {
  if (typeof window === 'undefined') {
    return false;
  }

  return hasRememberedAuth();
}

function persistSessionSnapshot(snapshot: SessionSnapshot) {
  if (typeof window === 'undefined') {
    return;
  }

  if (!snapshot.isAuthenticated) {
    window.localStorage.removeItem(SESSION_SNAPSHOT_STORAGE_KEY);
    return;
  }

  const persistedSnapshot: PersistedSessionSnapshot = {
    handle: snapshot.handle,
    defaultDbms: snapshot.defaultDbms,
    role: snapshot.role,
    handleSetupRequired: snapshot.handleSetupRequired,
  };

  window.localStorage.setItem(SESSION_SNAPSHOT_STORAGE_KEY, JSON.stringify(persistedSnapshot));
}

function readPersistedSessionSnapshot(): SessionSnapshot {
  const isAuthenticated = readPersistedAuthentication();

  if (typeof window === 'undefined' || !isAuthenticated) {
    return {
      isAuthenticated: false,
      isReady: true,
      handle: null,
      defaultDbms: null,
      role: null,
      handleSetupRequired: false,
      reauthenticationRequired: false,
    };
  }

  try {
    const rawValue = window.localStorage.getItem(SESSION_SNAPSHOT_STORAGE_KEY);
    if (rawValue == null) {
      return {
        isAuthenticated: true,
        isReady: true,
        handle: null,
        defaultDbms: null,
        role: null,
        handleSetupRequired: false,
        reauthenticationRequired: false,
      };
    }

    const parsedValue = JSON.parse(rawValue) as PersistedSessionSnapshot;
    const handle = typeof parsedValue.handle === 'string' && parsedValue.handle.trim() !== '' ? parsedValue.handle : null;
    const defaultDbms = parsedValue.defaultDbms === 'mysql' || parsedValue.defaultDbms === 'postgresql' ? parsedValue.defaultDbms : null;
    const role = parsedValue.role === 'admin' || parsedValue.role === 'user' ? parsedValue.role : null;

    return {
      isAuthenticated: true,
      isReady: true,
      handle,
      defaultDbms,
      role,
      handleSetupRequired: parsedValue.handleSetupRequired === true,
      reauthenticationRequired: false,
    };
  } catch {
    return {
      isAuthenticated: true,
      isReady: true,
      handle: null,
      defaultDbms: null,
      role: null,
      handleSetupRequired: false,
      reauthenticationRequired: false,
    };
  }
}

export function markAuthenticatedSession() {
  if (typeof window === 'undefined') {
    return;
  }

  persistAuthentication();
  updateSessionAlert(null);
  updateSessionSnapshot({
    isAuthenticated: true,
    isReady: true,
    handle: sessionSnapshot.handle,
    defaultDbms: sessionSnapshot.defaultDbms,
    role: sessionSnapshot.role,
    handleSetupRequired: sessionSnapshot.handleSetupRequired,
    reauthenticationRequired: false,
  });
}

export function applyAuthenticatedSession(session: SessionMeResult) {
  if (typeof window === 'undefined') {
    return;
  }

  persistAuthentication();
  updateSessionAlert(null);
  updateSessionSnapshot({
    isAuthenticated: session.authenticated,
    isReady: true,
    handle: session.handle,
    defaultDbms: session.defaultDbms,
    role: session.role,
    handleSetupRequired: session.handleSetupRequired,
    reauthenticationRequired: false,
  });
}

export function patchSessionSnapshot(
  patch: Partial<Pick<SessionSnapshot, 'handle' | 'defaultDbms' | 'role' | 'handleSetupRequired'>>
) {
  if (typeof window === 'undefined') {
    return;
  }

  updateSessionSnapshot({
    ...sessionSnapshot,
    ...patch,
    isReady: true,
  });
}

export async function syncSession(options: SyncSessionOptions = {}) {
  if (typeof window === 'undefined') {
    return false;
  }

  if (syncSessionPromise) {
    return syncSessionPromise;
  }

  syncSessionPromise = (async () => {
    try {
      const session = await fetchSessionMe();

      if (!session.authenticated) {
        if (options.openLoginOnExpire === false) {
          clearAuthenticatedSession();
        } else {
          expireAuthenticatedSession();
        }
        return false;
      }

      persistAuthentication();
      updateSessionSnapshot({
        isAuthenticated: true,
        isReady: true,
        handle: session.handle,
        defaultDbms: session.defaultDbms,
        role: session.role,
        handleSetupRequired: session.handleSetupRequired,
        reauthenticationRequired: false,
      });
      return true;
    } catch (error) {
      if (error instanceof AuthApiError && (error.status === 401 || error.status === 403)) {
        if (options.openLoginOnExpire === false) {
          clearAuthenticatedSession();
        } else {
          expireAuthenticatedSession();
        }
        return false;
      }

      if (options.clearOnFailure === true) {
        clearAuthenticatedSession();
        return false;
      }

      updateSessionSnapshot({
        ...sessionSnapshot,
        isReady: true,
      });
      return sessionSnapshot.isAuthenticated;
    } finally {
      syncSessionPromise = null;
    }
  })();

  return syncSessionPromise;
}

export function expireAuthenticatedSession(message = getUiTextValue('AUTH_SESSION_EXPIRED_MESSAGE', '로그인 세션이 만료되었습니다. 다시 로그인해 주세요.')) {
  if (typeof window === 'undefined') {
    return;
  }

  disconnectSessionSocket();
  clearPersistedAuthentication();
  updateSessionAlert(null);
  updateSessionSnapshot({
    isAuthenticated: false,
    isReady: true,
    handle: null,
    defaultDbms: null,
    role: null,
    handleSetupRequired: false,
    reauthenticationRequired: true,
  });

  const now = Date.now();
  if (now - sessionExpiredNoticeAt < 1000) {
    return;
  }

  sessionExpiredNoticeAt = now;
  openLoginOverlay(message, { force: true });
}

export function clearAuthenticatedSession() {
  if (typeof window === 'undefined') {
    return;
  }

  disconnectSessionSocket();
  clearPersistedAuthentication();
  updateSessionAlert(null);
  updateSessionSnapshot({
    isAuthenticated: false,
    isReady: true,
    handle: null,
    defaultDbms: null,
    role: null,
    handleSetupRequired: false,
    reauthenticationRequired: false,
  });
}

export function prepareLogoutReload() {
  if (typeof window === 'undefined') {
    return;
  }

  disconnectSessionSocket();
  clearPersistedAuthentication();
  prepareFavoriteTabsLogoutReload();
  sessionAlert = null;
  syncSessionPromise = null;
}

export function useSession() {
  const { isAuthenticated, isReady, handle, defaultDbms, role, handleSetupRequired, reauthenticationRequired } = useSyncExternalStore(subscribe, getSnapshot, () => ({
    isAuthenticated: false,
    isReady: false,
    handle: null,
    defaultDbms: null,
    role: null,
    handleSetupRequired: false,
    reauthenticationRequired: false,
  }));

  return {
    isAuthenticated,
    isReady,
    handle,
    defaultDbms,
    role,
    handleSetupRequired,
    reauthenticationRequired,
    isAdmin: role === 'admin',
    login: markAuthenticatedSession,
    logout: clearAuthenticatedSession,
  };
}

export function dismissSessionAlert() {
  if (typeof window === 'undefined') {
    return;
  }

  updateSessionAlert(null);
}

export function showSessionToast(message: string, autoDismissMs = 2200) {
  if (typeof window === 'undefined') {
    return;
  }

  updateSessionAlert({
    level: 1,
    message,
    confirmLabel: '확인',
    display: 'toast',
    tone: 'success',
    autoDismissMs,
  });
}

export function showSessionErrorToast(message: string, autoDismissMs = 2600) {
  if (typeof window === 'undefined') {
    return;
  }

  updateSessionAlert({
    level: 3,
    message,
    confirmLabel: '확인',
    display: 'toast',
    tone: 'error',
    autoDismissMs,
  });
}

export function useSessionAlert() {
  const currentSessionAlert = useSyncExternalStore(subscribeSessionAlert, getSessionAlertSnapshot, () => null);

  return {
    sessionAlert: currentSessionAlert,
    dismissSessionAlert,
  };
}

function hasRememberedAuth() {
  const savedValue = window.localStorage.getItem(REMEMBER_AUTH_STORAGE_KEY);
  if (savedValue == null) {
    return false;
  }

  try {
    const parsedValue = JSON.parse(savedValue) as { expiredAt?: number };
    if (typeof parsedValue.expiredAt !== 'number' || parsedValue.expiredAt <= Date.now()) {
      window.localStorage.removeItem(REMEMBER_AUTH_STORAGE_KEY);
      return false;
    }

    return true;
  } catch {
    window.localStorage.removeItem(REMEMBER_AUTH_STORAGE_KEY);
    return false;
  }
}
