import { getApiBaseUrl } from './authApi';

interface ExceptionResponse {
  reasons?: string[];
}

interface AuthManageUserRowResponse {
  userId?: string;
  role?: string;
  permissionKeys?: string[];
}

interface AuthManageResponse {
  users?: AuthManageUserRowResponse[];
}

export type AuthManageRoleValue = 'admin' | 'user' | 'problemGenerator';

export interface AuthManageUserRowData {
  userId: string;
  role: AuthManageRoleValue;
  permissionKeys: string[];
}

export interface AuthManageData {
  users: AuthManageUserRowData[];
}

function normalizeRole(value: string | undefined): AuthManageRoleValue {
  if (value === 'admin') {
    return 'admin';
  }

  if (value === 'problemGenerator') {
    return 'problemGenerator';
  }

  return 'user';
}

function parseAuthManageUsers(data: AuthManageUserRowResponse[] | undefined): AuthManageUserRowData[] {
  if (!Array.isArray(data)) {
    return [];
  }

  return data
    .filter((user): user is Required<Pick<AuthManageUserRowResponse, 'userId'>> & AuthManageUserRowResponse => typeof user.userId === 'string' && user.userId.trim() !== '')
    .map((user) => ({
      userId: user.userId,
      role: normalizeRole(user.role),
      permissionKeys: Array.isArray(user.permissionKeys)
        ? user.permissionKeys.filter((permissionKey): permissionKey is string => typeof permissionKey === 'string' && permissionKey.trim() !== '')
        : [],
    }));
}

async function getErrorMessage(response: Response, fallbackMessage: string) {
  try {
    const data = (await response.json()) as ExceptionResponse;
    if (Array.isArray(data.reasons) && typeof data.reasons[0] === 'string' && data.reasons[0].trim() !== '') {
      return data.reasons[0];
    }
  } catch {
  }

  return fallbackMessage;
}

export async function fetchAuthManage(): Promise<AuthManageData> {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/auth-manage`, {
      method: 'GET',
      credentials: 'include',
    });
  } catch {
    throw new Error('권한 목록을 불러오지 못했다.');
  }

  if (!response.ok) {
    throw new Error('권한 목록을 불러오지 못했다.');
  }

  try {
    const data = (await response.json()) as AuthManageResponse;
    return {
      users: parseAuthManageUsers(data.users),
    };
  } catch {
    throw new Error('권한 목록을 불러오지 못했다.');
  }
}

export async function updateUserRole(userId: string, role: AuthManageRoleValue) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/auth-manage/users/${encodeURIComponent(userId)}/role`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ role }),
    });
  } catch {
    throw new Error('역할을 저장하지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '역할을 저장하지 못했다.'));
  }
}

export async function updateProblemGeneratorPermissions(userId: string, permissionKeys: string[]) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/auth-manage/problem-generators/${encodeURIComponent(userId)}/permissions`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ permissionKeys }),
    });
  } catch {
    throw new Error('문제 권한을 저장하지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '문제 권한을 저장하지 못했다.'));
  }
}
