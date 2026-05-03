import { getUiTextValue, resolveApiHttpErrorReasons } from './uiText';
import { getApiBaseUrl } from './apiBaseUrl';

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
    super(reasons[0] ?? getUiTextValue('AUTH_SIGNUP_ERROR_MESSAGE', '회원가입 중 오류가 발생했습니다.'));
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
  defaultDbms: 'postgresql' | 'mysql' | null;
  role: 'user' | 'admin' | null;
  handleSetupRequired: boolean;
}
export { getApiBaseUrl } from './apiBaseUrl';

async function sha512Hex(value: string) {
  if (!globalThis.crypto?.subtle) {
    throw new Error(getUiTextValue('AUTH_BROWSER_ENCRYPTION_UNAVAILABLE_MESSAGE', '브라우저 암호화 기능을 사용할 수 없습니다.'));
  }

  const encodedValue = new TextEncoder().encode(value);
  const hashBuffer = await globalThis.crypto.subtle.digest('SHA-512', encodedValue);

  return Array.from(new Uint8Array(hashBuffer))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
}

async function getErrorReasons(response: Response, fallbackMessage: string) {
  return resolveApiHttpErrorReasons(response, fallbackMessage);
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
    defaultDbms: data.defaultDbms === 'mysql' ? 'mysql' : data.defaultDbms === 'postgresql' ? 'postgresql' : null,
    role: data.role === 'admin' ? 'admin' : data.role === 'user' ? 'user' : null,
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
    await requestAuth('/signup', payload, getUiTextValue('AUTH_SIGNUP_ERROR_MESSAGE', '회원가입 중 오류가 발생했습니다.'));
  } catch (error) {
    if (error instanceof AuthApiError) {
      throw new SignupApiError(error.status, error.reasons);
    }

    throw error;
  }
}

export async function sendSignupVerificationCode(payload: AccountRecoveryEmailPayload) {
  await requestSignupVerification('/signup/send-code', payload, getUiTextValue('AUTH_CODE_SEND_FAIL_MESSAGE', '인증 코드 전송 중 오류가 발생했습니다.'));
}

export async function verifySignupVerificationCode(payload: AccountRecoveryCodePayload) {
  await requestSignupVerification('/signup/verify-code', payload, getUiTextValue('AUTH_CODE_VERIFY_FAIL_MESSAGE', '인증 코드 확인 중 오류가 발생했습니다.'));
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
    throw new SignupApiError(0, [getUiTextValue('HANDLE_SETUP_FAIL_MESSAGE', 'Handle 설정 중 오류가 발생했습니다.')]);
  }

  if (!response.ok) {
    const reasons = await getErrorReasons(response, getUiTextValue('HANDLE_SETUP_FAIL_MESSAGE', 'Handle 설정 중 오류가 발생했습니다.'));
    throw new SignupApiError(response.status, reasons);
  }

  try {
    const data = (await response.json()) as SessionMeResponse;
    return parseSessionMeResult(data);
  } catch {
    throw new SignupApiError(response.status, [getUiTextValue('HANDLE_SETUP_PARSE_FAIL_MESSAGE', 'Handle 설정 응답 형식이 올바르지 않습니다.')]);
  }
}

export async function login(payload: LoginPayload) {
  const response = await requestAuth('/login', payload, getUiTextValue('AUTH_LOGIN_FAIL_MESSAGE', '로그인에 실패했습니다.'));

  try {
    const data = (await response.json()) as SessionMeResponse;
    return parseSessionMeResult(data);
  } catch {
    throw new AuthApiError(
      response.status,
      [getUiTextValue('AUTH_LOGIN_PARSE_FAIL_MESSAGE', '로그인 응답 형식이 올바르지 않습니다.')],
      getUiTextValue('AUTH_LOGIN_PARSE_FAIL_MESSAGE', '로그인 응답 형식이 올바르지 않습니다.'),
    );
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
    throw new AuthApiError(
      0,
      [getUiTextValue('AUTH_LOGOUT_FAIL_MESSAGE', '로그아웃에 실패했습니다.')],
      getUiTextValue('AUTH_LOGOUT_FAIL_MESSAGE', '로그아웃에 실패했습니다.'),
    );
  }

  if (response.ok) {
    return;
  }

  const reasons = await getErrorReasons(response, getUiTextValue('AUTH_LOGOUT_FAIL_MESSAGE', '로그아웃에 실패했습니다.'));
  throw new AuthApiError(response.status, reasons, getUiTextValue('AUTH_LOGOUT_FAIL_MESSAGE', '로그아웃에 실패했습니다.'));
}

export async function fetchSessionMe() {
  let response: Response;
  const fallbackMessage = getUiTextValue('AUTH_SESSION_RESTORE_FAIL_MESSAGE', '세션 복원 확인에 실패했습니다.');

  try {
    response = await fetch(`${getApiBaseUrl()}/session/me`, {
      method: 'POST',
      credentials: 'include',
    });
  } catch {
    throw new AuthApiError(0, [fallbackMessage], fallbackMessage);
  }

  if (!response.ok) {
    const reasons = await getErrorReasons(response, fallbackMessage);
    throw new AuthApiError(response.status, reasons, fallbackMessage);
  }

  try {
    const data = (await response.json()) as SessionMeResponse;
    return parseSessionMeResult(data);
  } catch {
    throw new Error(fallbackMessage);
  }
}

export async function checkDuplicateHandle(handle: string) {
  return requestDuplicateCheck('/duplicate-check/handle', 'handle', handle, getUiTextValue('HANDLE_DUPLICATE_CHECK_FAIL_MESSAGE', 'Handle 중복 확인 중 오류가 발생했습니다.'));
}

export async function checkDuplicateEmail(email: string) {
  return requestDuplicateCheck('/duplicate-check/email', 'email', email, getUiTextValue('AUTH_EMAIL_DUPLICATE_CHECK_FAIL_MESSAGE', '이메일 중복 확인 중 오류가 발생했습니다.'));
}

export async function sendPasswordResetCode(payload: AccountRecoveryEmailPayload) {
  await requestRecovery('/find-password/send-code', payload, getUiTextValue('RECOVERY_CODE_SEND_FAIL_MESSAGE', '인증 코드 전송 중 오류가 발생했습니다.'));
}

export async function verifyPasswordResetCode(payload: AccountRecoveryCodePayload) {
  await requestRecovery('/find-password/verify-code', payload, getUiTextValue('AUTH_CODE_VERIFY_FAIL_MESSAGE', '인증 코드 확인 중 오류가 발생했습니다.'));
}

export async function resetPassword(payload: ResetPasswordPayload) {
  await requestRecovery('/find-password/reset', {
    email: payload.email,
    password: await sha512Hex(payload.password),
  }, getUiTextValue('AUTH_PASSWORD_CHANGE_FAIL_MESSAGE', '비밀번호 변경 중 오류가 발생했습니다.'));
}
