import { useEffect } from 'react';
import { connectSessionSocket, disconnectSessionSocket, isSessionSocketOpen } from './sessionSocket';
import { loginMock, syncSession, useMockSession } from './session';

export async function completeAuthentication(rememberLogin = false) {
  loginMock(rememberLogin);

  const isAuthenticated = await syncSession();
  if (!isAuthenticated) {
    return;
  }

  try {
    await connectSessionSocket();
  } catch {
  }
}

export function useAuthenticationSocket() {
  const { isAuthenticated, isReady } = useMockSession();

  useEffect(() => {
    void syncSession();
  }, []);

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
      const isSessionAuthenticated = await syncSession();
      if (isDisposed || !isSessionAuthenticated) {
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
