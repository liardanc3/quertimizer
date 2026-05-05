import { getApiBaseUrl } from '@/shared/api/api-base-url';
import { expireAuthenticatedSession, syncSession } from '@/shared/auth/session';
import { getUiTextValue } from '@/shared/config/ui-text';

function readCookie(name: string) {
  const cookiePrefix = `${name}=`;
  return document.cookie
    .split(';')
    .map((cookie) => cookie.trim())
    .find((cookie) => cookie.startsWith(cookiePrefix))
    ?.slice(cookiePrefix.length);
}

function resolveRequestMethod(input: RequestInfo | URL, init?: RequestInit) {
  if (init?.method) {
    return init.method.toUpperCase();
  }

  if (input instanceof Request) {
    return input.method.toUpperCase();
  }

  return 'GET';
}

function shouldAttachCsrfHeader(method: string) {
  return method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS';
}

function isApiRequest(input: RequestInfo | URL) {
  try {
    const requestUrl = new URL(input instanceof Request ? input.url : input.toString(), window.location.href);
    const apiUrl = new URL(getApiBaseUrl());
    return requestUrl.origin === apiUrl.origin;
  } catch {
    return false;
  }
}

function isAuthBootstrapRequest(input: RequestInfo | URL) {
  try {
    const requestUrl = new URL(input instanceof Request ? input.url : input.toString(), window.location.href);
    return requestUrl.pathname === '/session/me'
      || requestUrl.pathname === '/login'
      || requestUrl.pathname.startsWith('/login/')
      || requestUrl.pathname.startsWith('/oauth2/')
      || requestUrl.pathname.startsWith('/signup')
      || requestUrl.pathname.startsWith('/duplicate-check/')
      || requestUrl.pathname.startsWith('/find-password/');
  } catch {
    return false;
  }
}

function expireSessionByAuthenticationFailure() {
  expireAuthenticatedSession(getUiTextValue('AUTH_SESSION_EXPIRED_MESSAGE', '로그인 세션이 만료되었습니다. 다시 로그인해 주세요.'));
}

function handleAuthenticationFailure(input: RequestInfo | URL, response: Response) {
  if (!isApiRequest(input) || isAuthBootstrapRequest(input)) {
    return;
  }

  if (response.status !== 401 && response.status !== 403) {
    return;
  }

  void syncSession().then((isValidSession) => {
    if (!isValidSession) {
      expireSessionByAuthenticationFailure();
    }
  });
}

export function installCsrfFetchInterceptor() {
  const originalFetch = window.fetch.bind(window);

  window.fetch = async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const method = resolveRequestMethod(input, init);
    if (!shouldAttachCsrfHeader(method)) {
      const response = await originalFetch(input, init);
      handleAuthenticationFailure(input, response);
      return response;
    }

    const csrfToken = readCookie('XSRF-TOKEN');
    if (!csrfToken) {
      const response = await originalFetch(input, init);
      handleAuthenticationFailure(input, response);
      return response;
    }

    const headers = new Headers(init.headers ?? (input instanceof Request ? input.headers : undefined));
    if (!headers.has('X-XSRF-TOKEN')) {
      headers.set('X-XSRF-TOKEN', decodeURIComponent(csrfToken));
    }

    const response = await originalFetch(input, {
      ...init,
      headers,
    });
    handleAuthenticationFailure(input, response);
    return response;
  };
}
