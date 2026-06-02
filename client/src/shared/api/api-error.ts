import { showRateLimitNotice } from '@/shared/lib/rate-limit-notice';

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

interface ExceptionResponse {
  reasons?: unknown;
}

interface ResolveHttpErrorOptions {
  resolveCommonMessage?: (key: string, fallbackMessage: string) => string | Promise<string>;
}

function normalizeErrorReasons(reasons: unknown) {
  if (!Array.isArray(reasons)) {
    return [];
  }

  return reasons.filter((reason): reason is string => typeof reason === 'string' && reason.trim() !== '');
}

async function readErrorReasons(response: Response) {
  try {
    const data = (await response.json()) as ExceptionResponse;
    return normalizeErrorReasons(data.reasons);
  } catch {
    return [];
  }
}

export function resolveCommonHttpErrorMessageKey(status?: number | null) {
  if (status === 401) {
    return 'HTTP_UNAUTHORIZED_ERROR_MESSAGE';
  }

  if (status === 403) {
    return 'HTTP_FORBIDDEN_ERROR_MESSAGE';
  }

  if (status === 404) {
    return 'HTTP_NOT_FOUND_ERROR_MESSAGE';
  }

  return 'HTTP_SERVER_ERROR_MESSAGE';
}

export function resolveCommonHttpErrorFallback(status?: number | null) {
  if (status === 401) {
    return '로그인이 필요합니다. 다시 로그인해 주세요.';
  }

  if (status === 403) {
    return '접근 권한이 없습니다.';
  }

  if (status === 404) {
    return '요청한 정보를 찾을 수 없습니다.';
  }

  return '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.';
}

export function isCommonHttpErrorStatus(status: number | null | undefined) {
  return status === 401 || status === 403 || status === 404 || (typeof status === 'number' && status >= 500 && status <= 599);
}

export async function resolveHttpErrorReasons(response: Response, fallbackMessage: string, options: ResolveHttpErrorOptions = {}) {
  const reasons = await readErrorReasons(response);
  if (reasons.length > 0) {
    return reasons;
  }

  if (isCommonHttpErrorStatus(response.status)) {
    const key = resolveCommonHttpErrorMessageKey(response.status);
    const commonFallbackMessage = resolveCommonHttpErrorFallback(response.status);
    return [await (options.resolveCommonMessage?.(key, commonFallbackMessage) ?? commonFallbackMessage)];
  }

  return [fallbackMessage];
}

export async function resolveHttpErrorMessage(response: Response, fallbackMessage: string, options: ResolveHttpErrorOptions = {}) {
  const reasons = await resolveHttpErrorReasons(response, fallbackMessage, options);
  return reasons[0] ?? fallbackMessage;
}

export function getApiErrorStatus(error: unknown) {
  if (error instanceof ApiError) {
    return error.status;
  }

  if (typeof error !== 'object' || error == null || !('status' in error)) {
    return null;
  }

  const status = (error as { status?: unknown }).status;
  return typeof status === 'number' ? status : null;
}

export function toApiError(status: number, message: string) {
  if (status === 429) {
    showRateLimitNotice(message);
    return new ApiError(status, message);
  }

  return isCommonHttpErrorStatus(status) ? new ApiError(status, message) : new Error(message);
}
