import { useSyncExternalStore } from 'react';
import { fetchSessionMe, type SessionMeResult } from './authApi';
import { handleFavoriteTabsSessionState } from './favoriteTabs';
import { disconnectSessionSocket } from './sessionSocket';

const REMEMBER_AUTH_STORAGE_KEY = 'quertimizer.remember-authenticated';
const AUTH_CHANGE_EVENT = 'quertimizer:auth-change';
const SESSION_ALERT_CHANGE_EVENT = 'quertimizer:session-alert-change';

export interface SessionAlert {
  level: 1 | 2 | 3;
  message: string;
  confirmLabel: string;
  display?: 'popup' | 'toast';
  autoDismissMs?: number;
}

interface SessionSnapshot {
  isAuthenticated: boolean;
  isReady: boolean;
  handle: string | null;
  defaultDbms: 'postgresql' | 'oracle' | null;
  role: 'user' | 'admin' | 'problemGenerator' | null;
  handleSetupRequired: boolean;
}

let sessionSnapshot: SessionSnapshot = {
  isAuthenticated: readPersistedAuthentication(),
  isReady: false,
  handle: null,
  defaultDbms: null,
  role: null,
  handleSetupRequired: false,
};
let sessionAlert: SessionAlert | null = null;
let syncSessionPromise: Promise<boolean> | null = null;

function emitAuthChange() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
}

function updateSessionSnapshot(nextSnapshot: SessionSnapshot) {
  sessionSnapshot = nextSnapshot;
  void handleFavoriteTabsSessionState({
    isReady: nextSnapshot.isReady,
    isAuthenticated: nextSnapshot.isAuthenticated,
  });
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
    const isAuthenticated = readPersistedAuthentication();

    sessionSnapshot = {
      isAuthenticated,
      isReady: sessionSnapshot.isReady,
      handle: isAuthenticated ? sessionSnapshot.handle : null,
      defaultDbms: isAuthenticated ? sessionSnapshot.defaultDbms : null,
      role: isAuthenticated ? sessionSnapshot.role : null,
      handleSetupRequired: isAuthenticated ? sessionSnapshot.handleSetupRequired : false,
    };
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
}

function readPersistedAuthentication() {
  if (typeof window === 'undefined') {
    return false;
  }

  return hasRememberedAuth();
}

export function loginMock() {
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
  });
}

export async function syncSession() {
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
        disconnectSessionSocket();
        clearPersistedAuthentication();
        updateSessionSnapshot({
          isAuthenticated: false,
          isReady: true,
          handle: null,
          defaultDbms: null,
          role: null,
          handleSetupRequired: false,
        });
        updateSessionAlert(null);

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
      });
      return true;
    } catch {
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

export function logoutMock() {
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
  });
}

export function useMockSession() {
  const { isAuthenticated, isReady, handle, defaultDbms, role, handleSetupRequired } = useSyncExternalStore(subscribe, getSnapshot, () => ({
    isAuthenticated: false,
    isReady: false,
    handle: null,
    defaultDbms: null,
    role: null,
    handleSetupRequired: false,
  }));

  return {
    isAuthenticated,
    isReady,
    handle,
    defaultDbms,
    role,
    handleSetupRequired,
    isAdmin: role === 'admin',
    isProblemGenerator: role === 'problemGenerator',
    login: loginMock,
    logout: logoutMock,
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
