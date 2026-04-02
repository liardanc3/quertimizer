import { useSyncExternalStore } from 'react';
import { fetchSessionMe } from './authApi';
import { disconnectSessionSocket } from './sessionSocket';

const SESSION_AUTH_STORAGE_KEY = 'quertimizer.session-authenticated';
const REMEMBER_AUTH_STORAGE_KEY = 'quertimizer.remember-authenticated';
const AUTH_CHANGE_EVENT = 'quertimizer:auth-change';
const SESSION_ALERT_CHANGE_EVENT = 'quertimizer:session-alert-change';

interface SessionAlert {
  level: 1 | 2 | 3;
  message: string;
  confirmLabel: string;
}

interface SessionSnapshot {
  isAuthenticated: boolean;
  isReady: boolean;
  userId: string | null;
}

let sessionSnapshot: SessionSnapshot = {
  isAuthenticated: readPersistedAuthentication(),
  isReady: false,
  userId: null,
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
  });
}

export async function syncSession() {
  if (typeof window === 'undefined') {
    return false;
  }

  const hadAuthenticatedState = sessionSnapshot.isAuthenticated || readPersistedAuthentication();

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
        });

        if (hadAuthenticatedState) {
          updateSessionAlert({
            level: 3,
            message: '서버와의 통신이 끊겼습니다. 다시 로그인 해주세요.',
            confirmLabel: '로그인하기',
          });
        }

        return false;
      }

      window.sessionStorage.setItem(SESSION_AUTH_STORAGE_KEY, 'true');
      updateSessionSnapshot({
        isAuthenticated: true,
        isReady: true,
        userId: session.userId,
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
  });
}

export function useMockSession() {
  const { isAuthenticated, isReady, userId } = useSyncExternalStore(subscribe, getSnapshot, () => ({
    isAuthenticated: false,
    isReady: false,
    userId: null,
  }));

  return {
    isAuthenticated,
    isReady,
    userId,
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
