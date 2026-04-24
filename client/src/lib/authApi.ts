function isLoopbackHostname(hostname: string) {
  return hostname === 'localhost' || hostname === '127.0.0.1';
}

function normalizeApiBaseUrl(value: string) {
  return value.replace(/\/+$/, '');
}

const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
const API_BASE_URL =
  configuredApiBaseUrl && configuredApiBaseUrl.length > 0
    ? normalizeApiBaseUrl(configuredApiBaseUrl)
    : typeof window !== 'undefined'
      ? normalizeApiBaseUrl(window.location.origin)
      : '';

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload {
  password: string;
  email: string;
  code: string;
}

export interface SetupHandlePayload {
  handle: string;
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
  password: string;
}

interface ExceptionResponse {
  reasons?: string[];
}

interface SessionMeResponse {
  authenticated?: boolean;
  handle?: string | null;
  defaultDbms?: string | null;
  role?: string | null;
  handleSetupRequired?: boolean | null;
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

export interface SessionMeResult {
  authenticated: boolean;
  handle: string | null;
  defaultDbms: 'postgresql' | 'oracle' | null;
  role: 'user' | 'admin' | 'problemGenerator' | null;
  handleSetupRequired: boolean;
}

export function getApiBaseUrl() {
  if (typeof window === 'undefined' || API_BASE_URL === '') {
    return API_BASE_URL;
  }

  const url = new URL(API_BASE_URL);

  if (isLoopbackHostname(url.hostname) && isLoopbackHostname(window.location.hostname)) {
    url.hostname = window.location.hostname;
  }

  return normalizeApiBaseUrl(url.toString());
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
        password: await sha512Hex(payload.password),
        ...(path === '/login' && 'email' in payload ? { email: payload.email } : {}),
        ...(path === '/login' ? { rememberLogin: true } : {}),
        ...(path === '/signup' && 'email' in payload ? { email: payload.email } : {}),
        ...(path === '/signup' && 'code' in payload ? { code: payload.code } : {}),
      }),
    });
  } catch {
    throw new AuthApiError(0, [fallbackMessage], fallbackMessage);
  }

  if (response.ok) {
    return response;
  }

  const reasons = await getErrorReasons(response, fallbackMessage);
  throw new AuthApiError(response.status, reasons, fallbackMessage);
}

function parseSessionMeResult(data: SessionMeResponse) {
  return {
    authenticated: data.authenticated === true,
    handle: typeof data.handle === 'string' && data.handle.trim() !== '' ? data.handle : null,
    defaultDbms: data.defaultDbms === 'oracle' ? 'oracle' : data.defaultDbms === 'postgresql' ? 'postgresql' : null,
    role:
      data.role === 'admin'
        ? 'admin'
        : data.role === 'user'
          ? 'user'
          : data.role === 'problem_generator'
            ? 'problemGenerator'
            : null,
    handleSetupRequired: data.handleSetupRequired === true,
  } satisfies SessionMeResult;
}

async function requestDuplicateCheck(
  path: '/duplicate-check/handle' | '/duplicate-check/email',
  queryKey: 'handle' | 'email',
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

  if (response.ok) {
    return {
      available: true,
      reason: null,
    };
  }

  const reasons = await getErrorReasons(response, fallbackMessage);
  if (response.status === 409) {
    return {
      available: false,
      reason: reasons[0] ?? fallbackMessage,
    };
  }

  throw new SignupApiError(response.status, reasons);
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

async function requestSignupVerification(path: '/signup/send-code' | '/signup/verify-code', payload: object, fallbackMessage: string) {
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
    throw new SignupApiError(0, [fallbackMessage]);
  }

  if (response.ok) {
    return response;
  }

  const reasons = await getErrorReasons(response, fallbackMessage);
  throw new SignupApiError(response.status, reasons);
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

export async function sendSignupVerificationCode(payload: AccountRecoveryEmailPayload) {
  await requestSignupVerification('/signup/send-code', payload, '이메일 가입 인증코드 발송에 실패했습니다.');
}

export async function verifySignupVerificationCode(payload: AccountRecoveryCodePayload) {
  await requestSignupVerification('/signup/verify-code', payload, '이메일 가입 인증코드 확인에 실패했습니다.');
}

export async function setupHandle(payload: SetupHandlePayload) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/signup/handle`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch {
    throw new SignupApiError(0, ['Handle 설정 요청에 실패했습니다.']);
  }

  if (!response.ok) {
    const reasons = await getErrorReasons(response, 'Handle 설정 요청에 실패했습니다.');
    throw new SignupApiError(response.status, reasons);
  }

  try {
    const data = (await response.json()) as SessionMeResponse;
    return parseSessionMeResult(data);
  } catch {
    throw new SignupApiError(response.status, ['Handle 설정 응답 처리에 실패했습니다.']);
  }
}

export async function login(payload: LoginPayload) {
  const response = await requestAuth('/login', payload, '로그인 요청에 실패했습니다.');

  try {
    const data = (await response.json()) as SessionMeResponse;
    return parseSessionMeResult(data);
  } catch {
    throw new AuthApiError(response.status, ['로그인 응답 처리에 실패했습니다.'], '로그인 응답 처리에 실패했습니다.');
  }
}

export function startGithubLogin() {
  if (typeof window === 'undefined') {
    return;
  }

  window.location.assign(`${getApiBaseUrl()}/oauth2/authorization/github`);
}

export function startGoogleLogin() {
  if (typeof window === 'undefined') {
    return;
  }

  window.location.assign(`${getApiBaseUrl()}/oauth2/authorization/google`);
}

export function startKakaoLogin() {
  if (typeof window === 'undefined') {
    return;
  }

  window.location.assign(`${getApiBaseUrl()}/oauth2/authorization/kakao`);
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
    return parseSessionMeResult(data);
  } catch {
    throw new Error('세션 복원 확인에 실패했습니다.');
  }
}

export async function checkDuplicateHandle(handle: string) {
  return requestDuplicateCheck('/duplicate-check/handle', 'handle', handle, 'Handle 중복확인 요청에 실패했습니다.');
}

export async function checkDuplicateEmail(email: string) {
  return requestDuplicateCheck('/duplicate-check/email', 'email', email, '이메일 중복확인 요청에 실패했습니다.');
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
    password: await sha512Hex(payload.password),
  }, '비밀번호 재설정에 실패했습니다.');
}
