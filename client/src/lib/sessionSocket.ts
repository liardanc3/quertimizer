import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { getApiBaseUrl } from './authApi';

const SESSION_SOCKET_PATH = '/ws/session';
const SESSION_REPLY_DESTINATION = '/user/queue/session';

export const SESSION_SOCKET_DESTINATION = {
  problemExecute: '/app/problem.execute',
  problemExecutePage: '/app/problem.execute.page',
  problemExecuteStop: '/app/problem.execute.stop',
  problemSubmit: '/app/problem.submit',
  problemLeave: '/app/problem.leave',
  judgeExecute: '/app/judge.execute',
  judgeExecutePage: '/app/judge.execute.page',
  judgeExecuteStop: '/app/judge.execute.stop',
  judgeSubmit: '/app/judge.submit',
  judgeLeave: '/app/judge.leave',
} as const;

let sessionSocketClient: Client | null = null;
let sessionSocketSubscription: StompSubscription | null = null;
let connectPromise: Promise<void> | null = null;
const messageListeners = new Set<(message: SessionSocketMessage) => void>();

export interface SessionSocketMessage {
  type: string;
  [key: string]: unknown;
}

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

  if (sessionSocketClient?.connected) {
    return Promise.resolve();
  }

  if (connectPromise) {
    return connectPromise;
  }

  if (sessionSocketClient) {
    void sessionSocketClient.deactivate();
  }

  connectPromise = new Promise<void>((resolve, reject) => {
    let isSettled = false;
    const client = new Client({
      brokerURL: getSessionSocketUrl(),
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        sessionSocketSubscription = client.subscribe(SESSION_REPLY_DESTINATION, handleSessionMessage);
        resolveConnection();
      },
      onStompError: () => rejectConnection(),
      onWebSocketError: () => rejectConnection(),
      onWebSocketClose: () => {
        if (sessionSocketClient === client) {
          sessionSocketClient = null;
          sessionSocketSubscription = null;
        }

        rejectConnection();
      },
    });

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
      if (sessionSocketClient === client) {
        sessionSocketClient = null;
        sessionSocketSubscription = null;
      }
      connectPromise = null;
      reject(new SessionSocketError());
    }

    sessionSocketClient = client;
    client.activate();
  });

  return connectPromise;
}

export async function sendSessionSocketMessage(destination: string, payload: object = {}) {
  await connectSessionSocket();

  if (!sessionSocketClient?.connected) {
    throw new SessionSocketError();
  }

  sessionSocketClient.publish({
    destination,
    body: JSON.stringify(payload),
  });
}

export function sendSessionSocketMessageIfOpen(destination: string, payload: object = {}) {
  if (!sessionSocketClient?.connected) {
    return false;
  }

  sessionSocketClient.publish({
    destination,
    body: JSON.stringify(payload),
  });
  return true;
}

export function subscribeSessionSocketMessages(listener: (message: SessionSocketMessage) => void) {
  messageListeners.add(listener);

  return () => {
    messageListeners.delete(listener);
  };
}

export function disconnectSessionSocket() {
  if (!sessionSocketClient) {
    return;
  }

  sessionSocketSubscription?.unsubscribe();
  sessionSocketSubscription = null;
  void sessionSocketClient.deactivate();
  sessionSocketClient = null;
  connectPromise = null;
}

export function isSessionSocketOpen() {
  return sessionSocketClient?.connected === true;
}

function handleSessionMessage(message: IMessage) {
  try {
    const sessionMessage = JSON.parse(message.body) as SessionSocketMessage;
    notifyMessageListeners(sessionMessage);
    if (sessionMessage.type === 'session.closed') {
      disconnectSessionSocket();
    }
  } catch {
  }
}

function notifyMessageListeners(message: SessionSocketMessage) {
  for (const messageListener of messageListeners) {
    messageListener(message);
  }
}
