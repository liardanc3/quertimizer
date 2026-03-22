const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/';

type QueryValue = string | number | boolean | null | undefined;
type QueryParams = Record<string, QueryValue>;

function buildUrl(path: string, params?: QueryParams) {
  const url = new URL(path, BASE_URL);

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        url.searchParams.append(key, String(value));
      }
    });
  }

  return url.toString();
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const errorText = await response.text().catch(() => '');
    throw new Error(
      `HTTP ${response.status} ${response.statusText}${
        errorText ? ` - ${errorText}` : ''
      }`
    );
  }

  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('application/json')) {
    return (await response.json()) as T;
  }

  return (await response.text()) as T;
}

export const http = {
  async get<T>(path: string): Promise<T> {
    const response = await fetch(`${BASE_URL}${path}`);

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
  },
};