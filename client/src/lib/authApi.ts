const DEFAULT_API_BASE_URL = 'http://localhost:8080';

function isLoopbackHostname(hostname: string) {
  return hostname === 'localhost' || hostname === '127.0.0.1';
}

export interface LoginPayload {
  userId: string;
  password: string;
  rememberLogin: boolean;
}

export interface SignupPayload {
  userId: string;
  password: string;
  email: string;
}

export interface AccountRecoveryEmailPayload {
  email: string;
}

export interface AccountRecoveryCodePayload {
  email: string;
  code: string;
}

export interface ResetPasswordPayload {
  email: string;
  code: string;
  password: string;
}

interface ExceptionResponse {
  reasons?: string[];
}

interface DuplicateCheckResponse {
  available?: boolean;
  reason?: string | null;
}

interface FindUserIdResponse {
  userId?: string;
}

interface SessionMeResponse {
  authenticated?: boolean;
  userId?: string | null;
  defaultDbms?: string | null;
  role?: string | null;
}

export class SignupApiError extends Error {
  readonly status: number;
  readonly reasons: string[];

  constructor(status: number, reasons: string[]) {
    super(reasons[0] ?? '회원가입 요청에 실패했습니다.');
    this.name = 'SignupApiError';
    this.status = status;
    this.reasons = reasons;
  }
}

export class AuthApiError extends Error {
  readonly status: number;
  readonly reasons: string[];

  constructor(status: number, reasons: string[], fallbackMessage: string) {
    super(reasons[0] ?? fallbackMessage);
    this.name = 'AuthApiError';
    this.status = status;
    this.reasons = reasons;
  }
}

export class RecoveryApiError extends Error {
  readonly status: number;
  readonly reasons: string[];

  constructor(status: number, reasons: string[], fallbackMessage: string) {
    super(reasons[0] ?? fallbackMessage);
    this.name = 'RecoveryApiError';
    this.status = status;
    this.reasons = reasons;
  }
}

export interface DuplicateCheckResult {
  available: boolean;
  reason: string | null;
}

export interface FindUserIdResult {
  userId: string;
}

export interface SessionMeResult {
  authenticated: boolean;
  userId: string | null;
  defaultDbms: 'postgresql' | 'oracle' | null;
  role: 'user' | 'admin' | 'problemGenerator' | null;
}

export function getApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
  const baseUrl = configuredBaseUrl && configuredBaseUrl.length > 0 ? configuredBaseUrl : DEFAULT_API_BASE_URL;
  const normalizedBaseUrl = baseUrl.replace(/\/+$/, '');

  if (typeof window === 'undefined') {
    return normalizedBaseUrl;
  }

  const url = new URL(normalizedBaseUrl);

  if (isLoopbackHostname(url.hostname) && isLoopbackHostname(window.location.hostname)) {
    url.hostname = window.location.hostname;
  }

  return url.toString().replace(/\/+$/, '');
}

async function sha512Hex(value: string) {
  if (!globalThis.crypto?.subtle) {
    throw new Error('브라우저 암호화 기능을 사용할 수 없습니다.');
  }

  const encodedValue = new TextEncoder().encode(value);
  const hashBuffer = await globalThis.crypto.subtle.digest('SHA-512', encodedValue);

  return Array.from(new Uint8Array(hashBuffer))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
}

async function getErrorReasons(response: Response, fallbackMessage: string) {
  let reasons = [fallbackMessage];

  try {
    const data = (await response.json()) as ExceptionResponse;

    if (Array.isArray(data.reasons) && data.reasons.length > 0) {
      reasons = data.reasons;
    }
  } catch {
    // Ignore invalid JSON responses and keep the fallback message.
  }

  return reasons;
}

async function requestAuth(path: '/signup' | '/login', payload: LoginPayload | SignupPayload, fallbackMessage: string) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        userId: payload.userId,
        password: await sha512Hex(payload.password),
        ...(path === '/login' && 'rememberLogin' in payload ? { rememberLogin: payload.rememberLogin } : {}),
        ...(path === '/signup' && 'email' in payload ? { email: payload.email } : {}),
      }),
    });
  } catch {
    throw new AuthApiError(0, [fallbackMessage], fallbackMessage);
  }

  if (response.ok) {
    return;
  }

  const reasons = await getErrorReasons(response, fallbackMessage);
  throw new AuthApiError(response.status, reasons, fallbackMessage);
}

async function requestDuplicateCheck(
  path: '/duplicate-check/userId' | '/duplicate-check/email',
  queryKey: 'userId' | 'email',
  value: string,
  fallbackMessage: string
) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        [queryKey]: value,
      }),
    });
  } catch {
    throw new SignupApiError(0, [fallbackMessage]);
  }

  if (!response.ok) {
    const reasons = await getErrorReasons(response, fallbackMessage);
    throw new SignupApiError(response.status, reasons);
  }

  try {
    const data = (await response.json()) as DuplicateCheckResponse;

    return {
      available: data.available === true,
      reason: typeof data.reason === 'string' ? data.reason : null,
    };
  } catch {
    throw new SignupApiError(response.status, [fallbackMessage]);
  }
}

async function requestRecovery(path: string, payload: object, fallbackMessage: string) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch {
    throw new RecoveryApiError(0, [fallbackMessage], fallbackMessage);
  }

  if (response.ok) {
    return response;
  }

  const reasons = await getErrorReasons(response, fallbackMessage);
  throw new RecoveryApiError(response.status, reasons, fallbackMessage);
}

export async function signup(payload: SignupPayload) {
  try {
    await requestAuth('/signup', payload, '회원가입 요청에 실패했습니다.');
  } catch (error) {
    if (error instanceof AuthApiError) {
      throw new SignupApiError(error.status, error.reasons);
    }

    throw error;
  }
}

export async function login(payload: LoginPayload) {
  await requestAuth('/login', payload, '로그인 요청에 실패했습니다.');
}

export async function logout() {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/logout`, {
      method: 'POST',
      credentials: 'include',
    });
  } catch {
    throw new AuthApiError(0, ['로그아웃 요청에 실패했습니다.'], '로그아웃 요청에 실패했습니다.');
  }

  if (response.ok) {
    return;
  }

  const reasons = await getErrorReasons(response, '로그아웃 요청에 실패했습니다.');
  throw new AuthApiError(response.status, reasons, '로그아웃 요청에 실패했습니다.');
}

export async function fetchSessionMe() {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/session/me`, {
      method: 'POST',
      credentials: 'include',
    });
  } catch {
    throw new Error('세션 복원 확인에 실패했습니다.');
  }

  if (!response.ok) {
    throw new Error('세션 복원 확인에 실패했습니다.');
  }

  try {
    const data = (await response.json()) as SessionMeResponse;

    return {
      authenticated: data.authenticated === true,
      userId: typeof data.userId === 'string' && data.userId.trim() !== '' ? data.userId : null,
      defaultDbms: data.defaultDbms === 'oracle' ? 'oracle' : data.defaultDbms === 'postgresql' ? 'postgresql' : null,
      role:
        data.role === 'admin'
          ? 'admin'
          : data.role === 'user'
            ? 'user'
            : data.role === 'problem_generator'
              ? 'problemGenerator'
              : null,
    } satisfies SessionMeResult;
  } catch {
    throw new Error('세션 복원 확인에 실패했습니다.');
  }
}

export async function checkDuplicateUserId(userId: string) {
  return requestDuplicateCheck('/duplicate-check/userId', 'userId', userId, '아이디 중복확인 요청에 실패했습니다.');
}

export async function checkDuplicateEmail(email: string) {
  return requestDuplicateCheck('/duplicate-check/email', 'email', email, '이메일 중복확인 요청에 실패했습니다.');
}

export async function sendUserIdRecoveryCode(payload: AccountRecoveryEmailPayload) {
  await requestRecovery('/find-id/send-code', payload, '아이디 찾기 인증코드 발송에 실패했습니다.');
}

export async function findUserId(payload: AccountRecoveryCodePayload) {
  const response = await requestRecovery('/find-id/verify-code', payload, '아이디 찾기 요청에 실패했습니다.');

  try {
    const data = (await response.json()) as FindUserIdResponse;

    if (typeof data.userId !== 'string' || data.userId.trim() === '') {
      throw new Error();
    }

    return {
      userId: data.userId,
    } satisfies FindUserIdResult;
  } catch {
    throw new RecoveryApiError(response.status, ['아이디 찾기 응답을 확인할 수 없습니다.'], '아이디 찾기 요청에 실패했습니다.');
  }
}

export async function sendPasswordResetCode(payload: AccountRecoveryEmailPayload) {
  await requestRecovery('/find-password/send-code', payload, '비밀번호 찾기 인증코드 발송에 실패했습니다.');
}

export async function verifyPasswordResetCode(payload: AccountRecoveryCodePayload) {
  await requestRecovery('/find-password/verify-code', payload, '인증코드 확인에 실패했습니다.');
}

export async function resetPassword(payload: ResetPasswordPayload) {
  await requestRecovery('/find-password/reset', {
    email: payload.email,
    code: payload.code,
    password: await sha512Hex(payload.password),
  }, '비밀번호 재설정에 실패했습니다.');
}
