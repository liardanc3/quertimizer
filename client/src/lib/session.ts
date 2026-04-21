import { useSyncExternalStore } from 'react';
import { fetchSessionMe, type SessionMeResult } from './authApi';
import { handleFavoriteTabsSessionState } from './favoriteTabs';
import { disconnectSessionSocket } from './sessionSocket';

const SESSION_AUTH_STORAGE_KEY = 'quertimizer.session-authenticated';
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
  userId: string | null;
  defaultDbms: 'postgresql' | 'oracle' | null;
  role: 'user' | 'admin' | 'problemGenerator' | null;
  userIdSetupRequired: boolean;
}

let sessionSnapshot: SessionSnapshot = {
  isAuthenticated: readPersistedAuthentication(),
  isReady: false,
  userId: null,
  defaultDbms: null,
  role: null,
  userIdSetupRequired: false,
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
      userId: isAuthenticated ? sessionSnapshot.userId : null,
      defaultDbms: isAuthenticated ? sessionSnapshot.defaultDbms : null,
      role: isAuthenticated ? sessionSnapshot.role : null,
      userIdSetupRequired: isAuthenticated ? sessionSnapshot.userIdSetupRequired : false,
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

function persistAuthentication(rememberLogin: boolean) {
  if (rememberLogin) {
    window.sessionStorage.removeItem(SESSION_AUTH_STORAGE_KEY);
    window.localStorage.setItem(
      REMEMBER_AUTH_STORAGE_KEY,
      JSON.stringify({
        expiredAt: Date.now() + 1000 * 60 * 60 * 24 * 30 * 6,
      })
    );
    return;
  }

  window.localStorage.removeItem(REMEMBER_AUTH_STORAGE_KEY);
  window.sessionStorage.setItem(SESSION_AUTH_STORAGE_KEY, 'true');
}

function clearPersistedAuthentication() {
  window.sessionStorage.removeItem(SESSION_AUTH_STORAGE_KEY);
  window.localStorage.removeItem(REMEMBER_AUTH_STORAGE_KEY);
}

function readPersistedAuthentication() {
  if (typeof window === 'undefined') {
    return false;
  }

  return window.sessionStorage.getItem(SESSION_AUTH_STORAGE_KEY) === 'true' || hasRememberedAuth();
}

export function loginMock(rememberLogin = false) {
  if (typeof window === 'undefined') {
    return;
  }

  persistAuthentication(rememberLogin);
  updateSessionAlert(null);
  updateSessionSnapshot({
    isAuthenticated: true,
    isReady: true,
    userId: sessionSnapshot.userId,
    defaultDbms: sessionSnapshot.defaultDbms,
    role: sessionSnapshot.role,
    userIdSetupRequired: sessionSnapshot.userIdSetupRequired,
  });
}

export function applyAuthenticatedSession(session: SessionMeResult, rememberLogin = false) {
  if (typeof window === 'undefined') {
    return;
  }

  persistAuthentication(rememberLogin);
  updateSessionAlert(null);
  updateSessionSnapshot({
    isAuthenticated: session.authenticated,
    isReady: true,
    userId: session.userId,
    defaultDbms: session.defaultDbms,
    role: session.role,
    userIdSetupRequired: session.userIdSetupRequired,
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
          userId: null,
          defaultDbms: null,
          role: null,
          userIdSetupRequired: false,
        });
        updateSessionAlert(null);

        return false;
      }

      window.sessionStorage.setItem(SESSION_AUTH_STORAGE_KEY, 'true');
      updateSessionSnapshot({
        isAuthenticated: true,
        isReady: true,
        userId: session.userId,
        defaultDbms: session.defaultDbms,
        role: session.role,
        userIdSetupRequired: session.userIdSetupRequired,
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
    userId: null,
    defaultDbms: null,
    role: null,
    userIdSetupRequired: false,
  });
}

export function useMockSession() {
  const { isAuthenticated, isReady, userId, defaultDbms, role, userIdSetupRequired } = useSyncExternalStore(subscribe, getSnapshot, () => ({
    isAuthenticated: false,
    isReady: false,
    userId: null,
    defaultDbms: null,
    role: null,
    userIdSetupRequired: false,
  }));

  return {
    isAuthenticated,
    isReady,
    userId,
    defaultDbms,
    role,
    userIdSetupRequired,
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
