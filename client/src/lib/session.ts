import { useSyncExternalStore } from 'react';

const AUTH_STORAGE_KEY = 'speedql.mock-authenticated';
const AUTH_CHANGE_EVENT = 'speedql:auth-change';

function emitAuthChange() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
}

function subscribe(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(AUTH_CHANGE_EVENT, callback);
  window.addEventListener('storage', callback);

  return () => {
    window.removeEventListener(AUTH_CHANGE_EVENT, callback);
    window.removeEventListener('storage', callback);
  };
}

function getSnapshot() {
  if (typeof window === 'undefined') {
    return false;
  }

  return window.localStorage.getItem(AUTH_STORAGE_KEY) === 'true';
}

export function loginMock() {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(AUTH_STORAGE_KEY, 'true');
  emitAuthChange();
}

export function logoutMock() {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  emitAuthChange();
}

export function useMockSession() {
  const isAuthenticated = useSyncExternalStore(subscribe, getSnapshot, () => false);

  return {
    isAuthenticated,
    login: loginMock,
    logout: logoutMock,
  };
}
