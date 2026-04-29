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

export function installCsrfFetchInterceptor() {
  const originalFetch = window.fetch.bind(window);

  window.fetch = (input: RequestInfo | URL, init: RequestInit = {}) => {
    const method = resolveRequestMethod(input, init);
    if (!shouldAttachCsrfHeader(method)) {
      return originalFetch(input, init);
    }

    const csrfToken = readCookie('XSRF-TOKEN');
    if (!csrfToken) {
      return originalFetch(input, init);
    }

    const headers = new Headers(init.headers ?? (input instanceof Request ? input.headers : undefined));
    if (!headers.has('X-XSRF-TOKEN')) {
      headers.set('X-XSRF-TOKEN', decodeURIComponent(csrfToken));
    }

    return originalFetch(input, {
      ...init,
      headers,
    });
  };
}
