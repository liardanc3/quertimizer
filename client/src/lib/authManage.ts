import { getApiBaseUrl } from './authApi';

interface ExceptionResponse {
  reasons?: string[];
}

interface AuthManageMemberResponse {
  userId?: string;
}

interface AuthManageRoleGroupResponse {
  count?: number;
  members?: AuthManageMemberResponse[];
}

interface AuthManageProblemGeneratorMemberResponse {
  userId?: string;
  problemIds?: string[];
}

interface AuthManageProblemGeneratorGroupResponse {
  count?: number;
  members?: AuthManageProblemGeneratorMemberResponse[];
}

interface AuthManageResponse {
  admins?: AuthManageRoleGroupResponse;
  users?: AuthManageRoleGroupResponse;
  problemGenerators?: AuthManageProblemGeneratorGroupResponse;
}

export type AuthManageRoleValue = 'admin' | 'user' | 'problemGenerator';

export interface AuthManageMemberData {
  userId: string;
}

export interface AuthManageRoleGroupData {
  count: number;
  members: AuthManageMemberData[];
}

export interface AuthManageProblemGeneratorMemberData {
  userId: string;
  problemIds: string[];
}

export interface AuthManageProblemGeneratorGroupData {
  count: number;
  members: AuthManageProblemGeneratorMemberData[];
}

export interface AuthManageData {
  admins: AuthManageRoleGroupData;
  users: AuthManageRoleGroupData;
  problemGenerators: AuthManageProblemGeneratorGroupData;
}

function parseRoleGroup(data: AuthManageRoleGroupResponse | undefined): AuthManageRoleGroupData {
  const members = Array.isArray(data?.members)
    ? data.members
        .filter((member): member is Required<AuthManageMemberResponse> => typeof member.userId === 'string' && member.userId.trim() !== '')
        .map((member) => ({
          userId: member.userId,
        }))
    : [];

  return {
    count: typeof data?.count === 'number' ? data.count : members.length,
    members,
  };
}

function parseProblemGeneratorGroup(
  data: AuthManageProblemGeneratorGroupResponse | undefined,
): AuthManageProblemGeneratorGroupData {
  const members = Array.isArray(data?.members)
    ? data.members
        .filter(
          (member): member is Required<Pick<AuthManageProblemGeneratorMemberResponse, 'userId'>> & AuthManageProblemGeneratorMemberResponse =>
            typeof member.userId === 'string' && member.userId.trim() !== '',
        )
        .map((member) => ({
          userId: member.userId,
          problemIds: Array.isArray(member.problemIds)
            ? member.problemIds.filter((problemId): problemId is string => typeof problemId === 'string' && problemId.trim() !== '')
            : [],
        }))
    : [];

  return {
    count: typeof data?.count === 'number' ? data.count : members.length,
    members,
  };
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
      admins: parseRoleGroup(data.admins),
      users: parseRoleGroup(data.users),
      problemGenerators: parseProblemGeneratorGroup(data.problemGenerators),
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

export async function updateProblemGeneratorPermissions(userId: string, problemIds: string[]) {
  let response: Response;

  try {
    response = await fetch(`${getApiBaseUrl()}/admin/auth-manage/problem-generators/${encodeURIComponent(userId)}/permissions`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ problemIds }),
    });
  } catch {
    throw new Error('문제 권한을 저장하지 못했다.');
  }

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '문제 권한을 저장하지 못했다.'));
  }
}
