import { getApiBaseUrl } from '@/shared/api/auth-api';
import { createApiErrorFromResponse, getUiTextValue } from '@/shared/config/ui-text';

interface AuthManageUserRowResponse {
  handle?: string;
  role?: string;
}

interface AuthManageResponse {
  users?: AuthManageUserRowResponse[];
}

export type AuthManageRoleValue = 'admin' | 'user';

export interface AuthManageUserRowData {
  handle: string;
  role: AuthManageRoleValue;
}

export interface AuthManageData {
  users: AuthManageUserRowData[];
}

function normalizeRole(value: string | undefined): AuthManageRoleValue {
  if (value === 'admin') {
    return 'admin';
  }

  return 'user';
}

function parseAuthManageUsers(data: AuthManageUserRowResponse[] | undefined): AuthManageUserRowData[] {
  if (!Array.isArray(data)) {
    return [];
  }

  return data
    .filter((user): user is Required<Pick<AuthManageUserRowResponse, 'handle'>> & AuthManageUserRowResponse => typeof user.handle === 'string' && user.handle.trim() !== '')
    .map((user) => ({
      handle: user.handle,
      role: normalizeRole(user.role),
    }));
}

export async function fetchAuthManage(): Promise<AuthManageData> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/auth-manage`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }

  try {
    const data = (await response.json()) as AuthManageResponse;
    return {
      users: parseAuthManageUsers(data.users),
    };
  } catch {
    throw new Error(getUiTextValue('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
  }
}

export async function updateUserRole(handle: string, role: AuthManageRoleValue) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/auth-manage/users/${encodeURIComponent(handle)}/role`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ role, confirmationText: 'ROLE_CHANGE_CONFIRMED' }),
    });
  } catch {
    throw new Error(getUiTextValue('AUTH_MANAGE_ROLE_SAVE_FAIL_MESSAGE', '역할을 저장하지 못했습니다.'));
  }

  if (!response.ok) {
    throw await createApiErrorFromResponse(response, getUiTextValue('AUTH_MANAGE_ROLE_SAVE_FAIL_MESSAGE', '역할을 저장하지 못했습니다.'));
  }
}
