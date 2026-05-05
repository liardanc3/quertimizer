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
