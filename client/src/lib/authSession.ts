import { useEffect } from 'react';
import {
  connectSessionSocket,
  disconnectSessionSocket,
  isSessionSocketOpen,
  subscribeSessionSocketConnection,
} from './sessionSocket';
import type { SessionMeResult } from './authApi';
import { applyAuthenticatedSession, clearAuthenticatedSession, syncSession, useSession } from './session';

export async function completeAuthentication(session: SessionMeResult) {
  if (!session.authenticated) {
    clearAuthenticatedSession();
    return;
  }

  applyAuthenticatedSession(session);

  if (session.handleSetupRequired || session.handle == null || session.handle.trim() === '') {
    disconnectSessionSocket();
    return;
  }

  try {
    await connectSessionSocket();
  } catch {
  }
}

export function useAuthenticationSocket() {
  const { isAuthenticated, isReady, handle, handleSetupRequired } = useSession();

  useEffect(() => {
    if (!isReady) {
      return;
    }

    if (!isAuthenticated) {
      disconnectSessionSocket();
      return;
    }

    if (handleSetupRequired || handle == null || handle.trim() === '') {
      disconnectSessionSocket();
      return;
    }

    let isDisposed = false;
    let restorePromise: Promise<void> | null = null;

    function restoreSessionAndSocket() {
      if (isDisposed || isSessionSocketOpen()) {
        return;
      }

      if (restorePromise) {
        return;
      }

      restorePromise = (async () => {
        const isSessionValid = await syncSession({
          openLoginOnExpire: false,
          clearOnFailure: true,
        });
        if (isDisposed || !isSessionValid) {
          return;
        }

        try {
          await connectSessionSocket();
        } catch {
        }
      })().finally(() => {
        restorePromise = null;
      });
    }

    function handleReconnectTrigger() {
      if (isSessionSocketOpen()) {
        return;
      }

      restoreSessionAndSocket();
    }

    restoreSessionAndSocket();

    const unsubscribeSocketConnection = subscribeSessionSocketConnection((connected) => {
      if (!connected) {
        restoreSessionAndSocket();
      }
    });
    window.addEventListener('focus', handleReconnectTrigger);
    window.addEventListener('online', handleReconnectTrigger);

    return () => {
      isDisposed = true;
      unsubscribeSocketConnection();
      window.removeEventListener('focus', handleReconnectTrigger);
      window.removeEventListener('online', handleReconnectTrigger);
    };
  }, [handle, handleSetupRequired, isAuthenticated, isReady]);
}
