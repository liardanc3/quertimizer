import { getApiBaseUrl } from './authApi';

const SESSION_SOCKET_PATH = '/ws/session';

let sessionSocket: WebSocket | null = null;
let connectPromise: Promise<void> | null = null;
const manualCloseSockets = new WeakSet<WebSocket>();

export class SessionSocketError extends Error {
  constructor(message = '로그인 웹소켓 연결에 실패했습니다.') {
    super(message);
    this.name = 'SessionSocketError';
  }
}

function getSessionSocketUrl() {
  const url = new URL(getApiBaseUrl());

  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.pathname = SESSION_SOCKET_PATH;
  url.search = '';
  url.hash = '';

  return url.toString();
}

export function connectSessionSocket() {
  if (typeof window === 'undefined') {
    return Promise.resolve();
  }

  if (sessionSocket?.readyState === WebSocket.OPEN) {
    return Promise.resolve();
  }

  if (sessionSocket?.readyState === WebSocket.CONNECTING && connectPromise) {
    return connectPromise;
  }

  if (sessionSocket) {
    manualCloseSockets.add(sessionSocket);
    sessionSocket.close();
  }

  const socket = new WebSocket(getSessionSocketUrl());
  sessionSocket = socket;

  connectPromise = new Promise<void>((resolve, reject) => {
    let isSettled = false;

    function resolveConnection() {
      if (isSettled) {
        return;
      }

      isSettled = true;
      connectPromise = null;
      resolve();
    }

    function rejectConnection() {
      if (isSettled) {
        return;
      }

      isSettled = true;
      if (sessionSocket === socket) {
        sessionSocket = null;
      }
      connectPromise = null;
      reject(new SessionSocketError());
    }

    socket.addEventListener('open', resolveConnection, { once: true });
    socket.addEventListener('error', rejectConnection, { once: true });
    socket.addEventListener(
      'close',
      () => {
        if (sessionSocket === socket) {
          sessionSocket = null;
        }

        if (!manualCloseSockets.has(socket)) {
          rejectConnection();
        }
      },
      { once: true }
    );
  });

  return connectPromise;
}

export function disconnectSessionSocket() {
  if (!sessionSocket) {
    return;
  }

  manualCloseSockets.add(sessionSocket);
  sessionSocket.close();
  sessionSocket = null;
  connectPromise = null;
}

export function isSessionSocketOpen() {
  return sessionSocket?.readyState === WebSocket.OPEN;
}
