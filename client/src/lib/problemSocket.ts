import { getApiBaseUrl } from './authApi';
import type { DbmsType } from '../types/domain';

const PROBLEM_SOCKET_PATH = '/ws/problem';

export interface ProblemSocketMessage {
  type: string;
  success?: boolean;
  problemId?: string | null;
  mode?: string | null;
  message?: string | null;
  columns?: string[];
  rows?: string[][];
  planLines?: string[];
  rowCount?: number;
  executionTimeMs?: number | null;
}

interface ProblemSocketClientOptions {
  onOpen?: () => void;
  onClose?: () => void;
  onError?: () => void;
  onMessage?: (message: ProblemSocketMessage) => void;
}

function getProblemSocketUrl() {
  const url = new URL(getApiBaseUrl());

  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
  url.pathname = PROBLEM_SOCKET_PATH;
  url.search = '';
  url.hash = '';

  return url.toString();
}

export class ProblemSocketError extends Error {
  constructor(message = '문제 실행 소켓 연결에 실패했다.') {
    super(message);
    this.name = 'ProblemSocketError';
  }
}

export class ProblemSocketClient {
  private socket: WebSocket | null = null;
  private connectPromise: Promise<void> | null = null;
  private isManualClose = false;
  private readonly options: ProblemSocketClientOptions;

  constructor(options: ProblemSocketClientOptions = {}) {
    this.options = options;
  }

  connect() {
    if (typeof window === 'undefined') {
      return Promise.resolve();
    }

    if (this.socket?.readyState === WebSocket.OPEN) {
      return Promise.resolve();
    }

    if (this.socket?.readyState === WebSocket.CONNECTING && this.connectPromise) {
      return this.connectPromise;
    }

    if (this.socket) {
      this.isManualClose = true;
      this.socket.close();
    }

    const socket = new WebSocket(getProblemSocketUrl());
    this.socket = socket;
    this.isManualClose = false;

    this.connectPromise = new Promise<void>((resolve, reject) => {
      let isSettled = false;

      const settleResolve = () => {
        if (isSettled) {
          return;
        }

        isSettled = true;
        this.connectPromise = null;
        this.options.onOpen?.();
        resolve();
      };

      const settleReject = () => {
        if (isSettled) {
          return;
        }

        isSettled = true;
        if (this.socket === socket) {
          this.socket = null;
        }
        this.connectPromise = null;
        this.options.onError?.();
        reject(new ProblemSocketError());
      };

      socket.addEventListener('open', settleResolve, { once: true });
      socket.addEventListener('error', settleReject, { once: true });
      socket.addEventListener(
        'message',
        (event) => {
          try {
            this.options.onMessage?.(JSON.parse(event.data) as ProblemSocketMessage);
          } catch {
            this.options.onMessage?.({
              type: 'error',
              success: false,
              message: '문제 실행 응답을 해석하지 못했다.',
            });
          }
        }
      );
      socket.addEventListener(
        'close',
        () => {
          if (this.socket === socket) {
            this.socket = null;
          }

          this.options.onClose?.();
          if (!this.isManualClose) {
            settleReject();
          }
        },
        { once: true }
      );
    });

    return this.connectPromise;
  }

  async execute(problemId: string, sql: string, dbms: DbmsType) {
    await this.connect();

    this.socket?.send(
      JSON.stringify({
        type: 'problem.execute',
        problemId,
        sql,
        dbms,
      })
    );
  }

  leave(problemId: string) {
    if (this.socket?.readyState !== WebSocket.OPEN) {
      return;
    }

    this.socket.send(
      JSON.stringify({
        type: 'problem.leave',
        problemId,
      })
    );
  }

  close() {
    if (!this.socket) {
      return;
    }

    this.isManualClose = true;
    this.socket.close();
    this.socket = null;
    this.connectPromise = null;
  }

  isOpen() {
    return this.socket?.readyState === WebSocket.OPEN;
  }
}
