import { useEffect, useState } from 'react';
import {
  fetchAuthManage,
  updateProblemGeneratorPermissions,
  updateUserRole,
  type AuthManageData,
  type AuthManageProblemGeneratorMemberData,
  type AuthManageRoleValue,
} from '../lib/authManage';

interface RoleSummaryRow {
  label: string;
  count: number;
  members: string[];
}

interface EditableUserRow {
  userId: string;
  role: AuthManageRoleValue;
}

const ROLE_OPTIONS: { value: AuthManageRoleValue; label: string }[] = [
  { value: 'admin', label: 'Admin' },
  { value: 'user', label: 'User' },
  { value: 'problemGenerator', label: 'ProblemGenerator' },
];

function RefreshIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M16.2 9.1a6.2 6.2 0 1 1-1.6-4.2"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.7"
      />
      <path
        d="M12.8 3.2h2.8V6"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.7"
      />
    </svg>
  );
}

function renderChips(values: string[], emptyText: string) {
  if (values.length === 0) {
    return <span className="admin-auth-empty-text">{emptyText}</span>;
  }

  return (
    <div className="admin-auth-chip-list">
      {values.map((value) => (
        <span key={value} className="admin-auth-chip">
          {value}
        </span>
      ))}
    </div>
  );
}

function buildRoleSummaryRows(authManage: AuthManageData | null): RoleSummaryRow[] {
  if (!authManage) {
    return [];
  }

  return [
    {
      label: 'Admin',
      count: authManage.admins.count,
      members: authManage.admins.members.map((member) => member.userId),
    },
    {
      label: 'User',
      count: authManage.users.count,
      members: authManage.users.members.map((member) => member.userId),
    },
    {
      label: 'ProblemGenerator',
      count: authManage.problemGenerators.count,
      members: authManage.problemGenerators.members.map((member) => member.userId),
    },
  ];
}

function buildEditableUserRows(authManage: AuthManageData | null): EditableUserRow[] {
  if (!authManage) {
    return [];
  }

  return [
    ...authManage.admins.members.map((member) => ({ userId: member.userId, role: 'admin' as const })),
    ...authManage.users.members.map((member) => ({ userId: member.userId, role: 'user' as const })),
    ...authManage.problemGenerators.members.map((member) => ({ userId: member.userId, role: 'problemGenerator' as const })),
  ].sort((left, right) => left.userId.localeCompare(right.userId));
}

function buildRoleDrafts(rows: EditableUserRow[]) {
  return Object.fromEntries(rows.map((row) => [row.userId, row.role])) as Record<string, AuthManageRoleValue>;
}

function buildProblemPermissionDrafts(members: AuthManageProblemGeneratorMemberData[]) {
  return Object.fromEntries(members.map((member) => [member.userId, member.problemIds.join('\n')])) as Record<string, string>;
}

function normalizeProblemIdsInput(value: string) {
  return value
    .split(/[\s,]+/)
    .map((problemId) => problemId.trim())
    .filter((problemId, index, problemIds) => problemId !== '' && problemIds.indexOf(problemId) === index);
}

function isSameProblemIdSet(left: string[], right: string[]) {
  return left.length === right.length && left.every((problemId, index) => problemId === right[index]);
}

function resolveRoleLabel(role: AuthManageRoleValue) {
  return ROLE_OPTIONS.find((option) => option.value === role)?.label ?? role;
}

export function AuthManageContent() {
  const [authManage, setAuthManage] = useState<AuthManageData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [reloadSequence, setReloadSequence] = useState(0);
  const [roleDrafts, setRoleDrafts] = useState<Record<string, AuthManageRoleValue>>({});
  const [roleErrorMessages, setRoleErrorMessages] = useState<Record<string, string>>({});
  const [savingRoleUserId, setSavingRoleUserId] = useState<string | null>(null);
  const [permissionDrafts, setPermissionDrafts] = useState<Record<string, string>>({});
  const [permissionErrorMessages, setPermissionErrorMessages] = useState<Record<string, string>>({});
  const [savingPermissionUserId, setSavingPermissionUserId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadAuthManage() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const nextAuthManage = await fetchAuthManage();

        if (cancelled) {
          return;
        }

        const nextUserRows = buildEditableUserRows(nextAuthManage);

        setAuthManage(nextAuthManage);
        setRoleDrafts(buildRoleDrafts(nextUserRows));
        setRoleErrorMessages({});
        setPermissionDrafts(buildProblemPermissionDrafts(nextAuthManage.problemGenerators.members));
        setPermissionErrorMessages({});
      } catch (error) {
        if (cancelled) {
          return;
        }

        setErrorMessage(error instanceof Error ? error.message : '권한 목록을 불러오지 못했다.');
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadAuthManage();

    return () => {
      cancelled = true;
    };
  }, [reloadSequence]);

  const roleSummaryRows = buildRoleSummaryRows(authManage);
  const editableUserRows = buildEditableUserRows(authManage);
  const problemGeneratorMembers: AuthManageProblemGeneratorMemberData[] = authManage?.problemGenerators.members ?? [];

  async function handleRoleSave(userId: string, currentRole: AuthManageRoleValue) {
    const draftRole = roleDrafts[userId] ?? currentRole;
    if (draftRole === currentRole) {
      return;
    }

    setSavingRoleUserId(userId);
    setRoleErrorMessages((current) => ({ ...current, [userId]: '' }));

    try {
      await updateUserRole(userId, draftRole);
      setReloadSequence((value) => value + 1);
    } catch (error) {
      setRoleErrorMessages((current) => ({
        ...current,
        [userId]: error instanceof Error ? error.message : '역할을 저장하지 못했다.',
      }));
    } finally {
      setSavingRoleUserId((current) => (current === userId ? null : current));
    }
  }

  async function handleProblemPermissionSave(userId: string, currentProblemIds: string[]) {
    const nextProblemIds = normalizeProblemIdsInput(permissionDrafts[userId] ?? '');
    if (isSameProblemIdSet(nextProblemIds, currentProblemIds)) {
      return;
    }

    setSavingPermissionUserId(userId);
    setPermissionErrorMessages((current) => ({ ...current, [userId]: '' }));

    try {
      await updateProblemGeneratorPermissions(userId, nextProblemIds);
      setReloadSequence((value) => value + 1);
    } catch (error) {
      setPermissionErrorMessages((current) => ({
        ...current,
        [userId]: error instanceof Error ? error.message : '문제 권한을 저장하지 못했다.',
      }));
    } finally {
      setSavingPermissionUserId((current) => (current === userId ? null : current));
    }
  }

  return (
    <section className="panel-card admin-auth-panel">
      <div className="admin-auth-toolbar">
        <button
          type="button"
          className="btn text admin-auth-refresh-button"
          onClick={() => setReloadSequence((value) => value + 1)}
          disabled={isLoading}
          aria-label="새로고침"
          title="새로고침"
        >
          <RefreshIcon />
        </button>
      </div>

      {errorMessage ? <p className="admin-auth-feedback is-error">{errorMessage}</p> : null}

      {isLoading && authManage == null ? (
        <p className="content-text">권한 목록을 불러오는 중이다.</p>
      ) : (
        <>
          <div className="admin-auth-section">
            <h2 className="admin-auth-section-title">역할</h2>

            <div className="admin-auth-table" role="table" aria-label="역할별 사용자 목록">
              <div className="admin-auth-row admin-auth-row-head" role="row">
                <span>role</span>
                <span>count</span>
                <span>members</span>
              </div>

              {roleSummaryRows.map((row) => (
                <div key={row.label} className="admin-auth-row" role="row">
                  <span className="admin-auth-role-name">{row.label}</span>
                  <span className="admin-auth-count">{row.count}</span>
                  {renderChips(row.members, '-')}
                </div>
              ))}
            </div>
          </div>

          <div className="admin-auth-section">
            <h2 className="admin-auth-section-title">사용자 역할</h2>

            {editableUserRows.length === 0 ? (
              <p className="admin-auth-empty">등록된 사용자가 없다.</p>
            ) : (
              <div className="admin-auth-edit-list">
                {editableUserRows.map((row) => {
                  const draftRole = roleDrafts[row.userId] ?? row.role;
                  const isSaving = savingRoleUserId === row.userId;
                  const isDirty = draftRole !== row.role;

                  return (
                    <section key={row.userId} className="admin-auth-edit-card">
                      <div className="admin-auth-edit-card-header">
                        <strong className="admin-auth-edit-card-title">{row.userId}</strong>
                        <span className="admin-auth-edit-card-meta">Current: {resolveRoleLabel(row.role)}</span>
                      </div>

                      <div className="segmented admin-auth-role-selector" role="group" aria-label={`${row.userId} role`}>
                        {ROLE_OPTIONS.map((option) => (
                          <button
                            key={option.value}
                            type="button"
                            className={`segmented-btn admin-auth-role-button ${draftRole === option.value ? 'is-selected' : ''}`}
                            aria-pressed={draftRole === option.value}
                            onClick={() => {
                              setRoleDrafts((current) => ({ ...current, [row.userId]: option.value }));
                            }}
                          >
                            {option.label}
                          </button>
                        ))}
                      </div>

                      <div className="admin-auth-save-row">
                        <button
                          type="button"
                          className="btn secondary admin-auth-save-button"
                          onClick={() => {
                            void handleRoleSave(row.userId, row.role);
                          }}
                          disabled={!isDirty || isSaving}
                        >
                          {isSaving ? 'Saving...' : 'Save'}
                        </button>
                      </div>

                      {roleErrorMessages[row.userId] ? (
                        <p className="admin-auth-row-feedback is-error">{roleErrorMessages[row.userId]}</p>
                      ) : null}
                    </section>
                  );
                })}
              </div>
            )}
          </div>

          <div className="admin-auth-section admin-auth-section-problem-permissions">
            <h2 className="admin-auth-section-title">문제 권한</h2>

            {problemGeneratorMembers.length === 0 ? (
              <p className="admin-auth-empty">등록된 ProblemGenerator 권한이 없다.</p>
            ) : (
              <div className="admin-auth-edit-list">
                {problemGeneratorMembers.map((member) => {
                  const draftValue = permissionDrafts[member.userId] ?? member.problemIds.join('\n');
                  const isSaving = savingPermissionUserId === member.userId;
                  const isDirty = !isSameProblemIdSet(normalizeProblemIdsInput(draftValue), member.problemIds);

                  return (
                    <section key={member.userId} className="admin-auth-edit-card admin-auth-permission-card">
                      <div className="admin-auth-edit-card-header">
                        <strong className="admin-auth-edit-card-title">{member.userId}</strong>
                        <span className="admin-auth-edit-card-meta">{member.problemIds.length} problemIds</span>
                      </div>

                      <label className="admin-auth-permission-field">
                        <span className="admin-auth-permission-label">problemIds</span>
                        <textarea
                          className="text-field admin-auth-permission-input"
                          value={draftValue}
                          onChange={(event) => {
                            const nextValue = event.target.value;
                            setPermissionDrafts((current) => ({ ...current, [member.userId]: nextValue }));
                          }}
                          rows={Math.max(3, Math.min(8, member.problemIds.length + 1))}
                          spellCheck={false}
                        />
                      </label>

                      <p className="admin-auth-permission-helper">Use commas or new lines to separate problemIds.</p>

                      <div className="admin-auth-save-row">
                        <button
                          type="button"
                          className="btn secondary admin-auth-save-button"
                          onClick={() => {
                            void handleProblemPermissionSave(member.userId, member.problemIds);
                          }}
                          disabled={!isDirty || isSaving}
                        >
                          {isSaving ? 'Saving...' : 'Save'}
                        </button>
                      </div>

                      {permissionErrorMessages[member.userId] ? (
                        <p className="admin-auth-row-feedback is-error">{permissionErrorMessages[member.userId]}</p>
                      ) : null}
                    </section>
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}
    </section>
  );
}
