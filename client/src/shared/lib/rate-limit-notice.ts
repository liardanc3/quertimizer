import { useSyncExternalStore } from 'react';

const RATE_LIMIT_NOTICE_CHANGE_EVENT = 'quertimizer:rate-limit-notice-change';
const RATE_LIMIT_TITLE = '요청 제한';
const RATE_LIMIT_DEFAULT_MESSAGE = '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.';
const RATE_LIMIT_NOTICE_DEDUPLICATE_MS = 900;

interface RateLimitNotice {
  id: number;
  message: string;
  autoDismissMs: number;
}

let rateLimitNotice: RateLimitNotice | null = null;
let rateLimitNoticeSequence = 0;
let lastNoticeMessage = '';
let lastNoticeAt = 0;

function emitRateLimitNoticeChange() {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new Event(RATE_LIMIT_NOTICE_CHANGE_EVENT));
}

function subscribeRateLimitNotice(callback: () => void) {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  window.addEventListener(RATE_LIMIT_NOTICE_CHANGE_EVENT, callback);

  return () => {
    window.removeEventListener(RATE_LIMIT_NOTICE_CHANGE_EVENT, callback);
  };
}

function getRateLimitNoticeSnapshot() {
  return rateLimitNotice;
}

export function isRateLimitNoticeMessage(message: string | null | undefined, reasons?: unknown) {
  const normalizedMessage = typeof message === 'string' ? message.trim() : '';
  if (normalizedMessage === RATE_LIMIT_TITLE || normalizedMessage.includes('요청이 너무 많습니다')) {
    return true;
  }

  return Array.isArray(reasons)
    && reasons.some((reason) => typeof reason === 'string' && reason.includes('요청이 너무 많습니다'));
}

export function resolveRateLimitNoticeMessage(message: string | null | undefined, reasons?: unknown) {
  if (Array.isArray(reasons)) {
    const detail = reasons.find((reason): reason is string =>
      typeof reason === 'string' && reason.trim() !== '' && reason.trim() !== RATE_LIMIT_TITLE
    );
    if (detail != null) {
      return detail;
    }
  }

  if (typeof message === 'string' && message.trim() !== '' && message.trim() !== RATE_LIMIT_TITLE) {
    return message.trim();
  }

  return RATE_LIMIT_DEFAULT_MESSAGE;
}

export function showRateLimitNotice(message = RATE_LIMIT_DEFAULT_MESSAGE, autoDismissMs = 2600) {
  if (typeof window === 'undefined') {
    return;
  }

  const normalizedMessage = message.trim() !== '' ? message.trim() : RATE_LIMIT_DEFAULT_MESSAGE;
  const now = Date.now();
  if (lastNoticeMessage === normalizedMessage && now - lastNoticeAt < RATE_LIMIT_NOTICE_DEDUPLICATE_MS) {
    return;
  }

  lastNoticeMessage = normalizedMessage;
  lastNoticeAt = now;
  rateLimitNotice = {
    id: ++rateLimitNoticeSequence,
    message: normalizedMessage,
    autoDismissMs,
  };
  emitRateLimitNoticeChange();
}

export function dismissRateLimitNotice() {
  if (typeof window === 'undefined') {
    return;
  }

  rateLimitNotice = null;
  emitRateLimitNoticeChange();
}

export function useRateLimitNotice() {
  const currentRateLimitNotice = useSyncExternalStore(subscribeRateLimitNotice, getRateLimitNoticeSnapshot, () => null);

  return {
    rateLimitNotice: currentRateLimitNotice,
    dismissRateLimitNotice,
  };
}
