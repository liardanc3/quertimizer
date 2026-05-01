import { useEffect } from 'react';
import { connectSessionSocket, disconnectSessionSocket, isSessionSocketOpen } from './sessionSocket';
import type { SessionMeResult } from './authApi';
import { applyAuthenticatedSession, clearAuthenticatedSession, useSession } from './session';

export async function completeAuthentication(session: SessionMeResult) {
  if (!session.authenticated) {
    clearAuthenticatedSession();
    return;
  }

  applyAuthenticatedSession(session);

  try {
    await connectSessionSocket();
  } catch {
  }
}

export function useAuthenticationSocket() {
  const { isAuthenticated, isReady } = useSession();

  useEffect(() => {
    if (!isReady) {
      return;
    }

    if (!isAuthenticated) {
      disconnectSessionSocket();
      return;
    }

    let isDisposed = false;

    async function ensureConnection() {
      if (isDisposed || isSessionSocketOpen()) {
        return;
      }

      try {
        await connectSessionSocket();
      } catch {
      }
    }

    function handleReconnectTrigger() {
      if (isSessionSocketOpen()) {
        return;
      }

      void ensureConnection();
    }

    void ensureConnection();

    const intervalId = window.setInterval(handleReconnectTrigger, 5000);
    window.addEventListener('focus', handleReconnectTrigger);
    window.addEventListener('online', handleReconnectTrigger);

    return () => {
      isDisposed = true;
      window.clearInterval(intervalId);
      window.removeEventListener('focus', handleReconnectTrigger);
      window.removeEventListener('online', handleReconnectTrigger);
    };
  }, [isAuthenticated, isReady]);
}
