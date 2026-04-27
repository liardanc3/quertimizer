import { useEffect, useMemo, useRef, useState, type KeyboardEvent as ReactKeyboardEvent } from 'react';
import HttpErrorState from '../components/common/HttpErrorState';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '../lib/apiError';
import {
  fetchAuthManage,
  updateProblemGeneratorPermissions,
  updateUserRole,
  type AuthManageData,
  type AuthManageRoleValue,
  type AuthManageUserRowData,
} from '../lib/authManage';
import { getUiTextValue, useUiText } from '../lib/uiText';

type AuthManageSection = 'admin' | 'user' | 'problemGenerator';

const ROLE_VALUES: AuthManageRoleValue[] = ['admin', 'user', 'problemGenerator'];

function RoleEditIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path
        d="M6 5.6h8M6 10h5.5M6 14.4h4"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="1.6"
      />
      <path
        d="m12.4 13.5 2.9-2.9 1.7 1.7-2.9 2.9-2.4.7Z"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.4"
      />
    </svg>
  );
}

function AddIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="M10 4.4v11.2M4.4 10h11.2" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
    </svg>
  );
}

function RemoveIcon() {
  return (
    <svg viewBox="0 0 20 20" aria-hidden="true">
      <path d="m5.3 5.3 9.4 9.4m0-9.4-9.4 9.4" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="1.7" />
    </svg>
  );
}

function resolveRoleLabel(role: AuthManageRoleValue) {
  if (role === 'admin') {
    return getUiTextValue('AUTH_MANAGE_ADMIN_LABEL', 'Admin');
  }

  if (role === 'problemGenerator') {
    return getUiTextValue('AUTH_MANAGE_PROBLEM_GENERATOR_LABEL', 'ProblemGenerator');
  }

  return getUiTextValue('AUTH_MANAGE_USER_LABEL', 'User');
}

function resolveStaticNote(role: AuthManageRoleValue) {
  if (role === 'admin') {
    return getUiTextValue('AUTH_MANAGE_ADMIN_NOTE', '전체 권한');
  }

  if (role === 'user') {
    return getUiTextValue('AUTH_MANAGE_USER_NOTE', '일반 사용자');
  }

  return '';
}

function normalizePermissionKey(value: string) {
  const normalizedValue = value.trim().toUpperCase();
  if (normalizedValue === '') {
    return '';
  }

  if (normalizedValue === 'NEW') {
    return 'NEW';
  }

  if (/^\d{5}$/.test(normalizedValue) || /^\d{5}-\d{5}$/.test(normalizedValue)) {
    return `P${normalizedValue}`;
  }

  return normalizedValue;
}

function sortPermissionKeys(permissionKeys: string[]) {
  return [...permissionKeys].sort((left, right) => {
    const leftRank = left === 'NEW' ? 0 : /^[PM]\d{5}$/.test(left) ? 1 : /^[PM]\d{5}-\d{5}$/.test(left) ? 2 : 3;
    const rightRank = right === 'NEW' ? 0 : /^[PM]\d{5}$/.test(right) ? 1 : /^[PM]\d{5}-\d{5}$/.test(right) ? 2 : 3;

    if (leftRank !== rightRank) {
      return leftRank - rightRank;
    }

    return left.localeCompare(right);
  });
}

export function AuthManageContent() {
  const { text } = useUiText();
  const PAGE_SIZE = 10;
  const [authManage, setAuthManage] = useState<AuthManageData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [loadErrorStatus, setLoadErrorStatus] = useState<number | null>(null);
  const [reloadSequence, setReloadSequence] = useState(0);
  const [activeSection, setActiveSection] = useState<AuthManageSection>('admin');
  const [currentPage, setCurrentPage] = useState(1);
  const [isPageJumpEditing, setIsPageJumpEditing] = useState(false);
  const [pageJumpDraft, setPageJumpDraft] = useState('1');
  const [openRoleMenuHandle, setOpenRoleMenuHandle] = useState<string | null>(null);
  const [permissionInputDrafts, setPermissionInputDrafts] = useState<Record<string, string>>({});
  const [permissionErrorMessages, setPermissionErrorMessages] = useState<Record<string, string>>({});
  const [savingRoleHandle, setSavingRoleHandle] = useState<string | null>(null);
  const [savingPermissionHandle, setSavingPermissionHandle] = useState<string | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const roleOptions = ROLE_VALUES.map((value) => ({ value, label: resolveRoleLabel(value) }));
  const sections = roleOptions.map((option) => ({ id: option.value as AuthManageSection, label: option.label }));

  useEffect(() => {
    let cancelled = false;

    async function loadAuthManage() {
      setIsLoading(true);
      setErrorMessage(null);
      setLoadErrorStatus(null);

      try {
        const nextAuthManage = await fetchAuthManage();
        if (cancelled) {
          return;
        }

        setAuthManage(nextAuthManage);
        setPermissionInputDrafts({});
        setPermissionErrorMessages({});
        setCurrentPage(1);
        setIsPageJumpEditing(false);
        setPageJumpDraft('1');
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error instanceof Error ? error.message : text('COMMON_PAGE_LOAD_FAILURE_MESSAGE', '잠시 후 다시 시도해주세요.'));
          const status = getApiErrorStatus(error);
          setLoadErrorStatus(isCommonHttpErrorStatus(status) ? status : null);
        }
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

  useEffect(() => {
    function handleDocumentMouseDown(event: MouseEvent) {
      if (!(event.target instanceof Node)) {
        return;
      }

      if (!panelRef.current?.contains(event.target)) {
        setOpenRoleMenuHandle(null);
      }
    }

    function handleEscape(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpenRoleMenuHandle(null);
      }
    }

    document.addEventListener('mousedown', handleDocumentMouseDown);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handleDocumentMouseDown);
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  const users = useMemo(() => authManage?.users ?? [], [authManage]);
  const filteredUsers = useMemo(
    () => users.filter((user) => user.role === activeSection),
    [activeSection, users],
  );
  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / PAGE_SIZE));
  const pagedUsers = useMemo(
    () => filteredUsers.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE),
    [filteredUsers, currentPage],
  );

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  useEffect(() => {
    setCurrentPage(1);
    setIsPageJumpEditing(false);
    setPageJumpDraft('1');
  }, [activeSection]);

  async function handleRoleChange(user: AuthManageUserRowData, nextRole: AuthManageRoleValue) {
    if (user.role === nextRole) {
      setOpenRoleMenuHandle(null);
      return;
    }

    setSavingRoleHandle(user.handle);
    setOpenRoleMenuHandle(null);

    try {
      await updateUserRole(user.handle, nextRole);
      setReloadSequence((value) => value + 1);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : text('AUTH_MANAGE_ROLE_SAVE_FAIL_MESSAGE', '역할을 저장하지 못했습니다.'));
    } finally {
      setSavingRoleHandle((current) => (current === user.handle ? null : current));
    }
  }

  async function handlePermissionChange(handle: string, nextPermissionKeys: string[]) {
    setSavingPermissionHandle(handle);
    setPermissionErrorMessages((current) => ({ ...current, [handle]: '' }));

    try {
      await updateProblemGeneratorPermissions(handle, sortPermissionKeys(nextPermissionKeys));
      setReloadSequence((value) => value + 1);
    } catch (error) {
      setPermissionErrorMessages((current) => ({
        ...current,
        [handle]: error instanceof Error ? error.message : text('AUTH_MANAGE_PERMISSION_SAVE_FAIL_MESSAGE', '문제 권한을 저장하지 못했습니다.'),
      }));
    } finally {
      setSavingPermissionHandle((current) => (current === handle ? null : current));
    }
  }

  async function handlePermissionAdd(user: AuthManageUserRowData) {
    const draftValue = normalizePermissionKey(permissionInputDrafts[user.handle] ?? '');
    if (draftValue === '') {
      return;
    }

    if (user.permissionKeys.includes(draftValue)) {
      setPermissionInputDrafts((current) => ({ ...current, [user.handle]: '' }));
      return;
    }

    await handlePermissionChange(user.handle, [...user.permissionKeys, draftValue]);
    setPermissionInputDrafts((current) => ({ ...current, [user.handle]: '' }));
  }

  async function handlePermissionRemove(user: AuthManageUserRowData, permissionKey: string) {
    await handlePermissionChange(
      user.handle,
      user.permissionKeys.filter((currentPermissionKey) => currentPermissionKey !== permissionKey),
    );
  }

  function handlePermissionInputKeyDown(event: ReactKeyboardEvent<HTMLInputElement>, user: AuthManageUserRowData) {
    if (event.key === 'Enter') {
      event.preventDefault();
      void handlePermissionAdd(user);
    }
  }

  function applyPageJump() {
    const parsedPage = Number.parseInt(pageJumpDraft, 10);
    setCurrentPage(Number.isNaN(parsedPage) ? currentPage : Math.min(Math.max(parsedPage, 1), totalPages));
    setIsPageJumpEditing(false);
  }

  function cancelPageJump() {
    setPageJumpDraft(String(currentPage));
    setIsPageJumpEditing(false);
  }

  return (
    <section ref={panelRef} className="panel-card admin-auth-panel">
      {errorMessage && authManage != null ? <p className="admin-auth-feedback is-error">{errorMessage}</p> : null}

      {errorMessage && authManage == null
        ? loadErrorStatus != null
          ? <HttpErrorState status={loadErrorStatus} className="admin-auth-empty" message={errorMessage} />
          : <PageLoadFailureState className="admin-auth-empty" message={errorMessage} />
        : isLoading && authManage == null ? (
        <div className="admin-page-loading-shell admin-auth-loading-shell is-loading" aria-live="polite" aria-label={text('COMMON_LOADING_STATUS', '로딩 중')}>
          <div className="admin-page-loading-body admin-auth-loading-body" aria-hidden="true">
            <div className="admin-page-loading-row is-wide" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row is-narrow" />
            <div className="admin-page-loading-row is-wide" />
            <div className="admin-page-loading-row" />
          </div>

          <LoadingOverlay ariaHidden />
        </div>
      ) : (
        <div className="admin-auth-layout">
          <aside className="admin-auth-side-nav" aria-label={text('AUTH_MANAGE_SECTION_NAV_LABEL', '권한 설정 섹션')}>
            {sections.map((section) => {
              const isSelected = section.id === activeSection;
              return (
                <button
                  key={section.id}
                  type="button"
                  className={`admin-auth-side-nav-item ${isSelected ? 'is-selected' : ''}`}
                  onClick={() => setActiveSection(section.id)}
                >
                  <strong>{section.label}</strong>
                </button>
              );
            })}
          </aside>

          <div className="admin-auth-content">
            <div className="admin-auth-table" role="table" aria-label={text('AUTH_MANAGE_TABLE_LABEL', '권한 설정')}>
              <div className="admin-auth-row admin-auth-row-head" role="row">
                <div role="columnheader">{text('COMMON_HANDLE_LABEL', 'Handle')}</div>
                <div role="columnheader">{text('AUTH_MANAGE_ROLE_COLUMN_LABEL', '역할')}</div>
                <div role="columnheader">{text('AUTH_MANAGE_NOTE_COLUMN_LABEL', '비고')}</div>
              </div>

              {pagedUsers.map((user) => {
                const isRoleSaving = savingRoleHandle === user.handle;
                const isPermissionSaving = savingPermissionHandle === user.handle;
                const staticNote = resolveStaticNote(user.role);

                return (
                  <div key={user.handle} className="admin-auth-row admin-auth-user-row" role="row">
                    <div className="admin-auth-user-cell" role="cell">
                      <span className="admin-auth-user-handle">{user.handle}</span>
                    </div>

                    <div className="admin-auth-role-cell" role="cell">
                      <span className="admin-auth-role-pill">{resolveRoleLabel(user.role)}</span>
                      <div className="admin-auth-role-menu-shell">
                        <button
                          type="button"
                          className="admin-config-icon-button admin-auth-role-menu-button"
                          onClick={() => setOpenRoleMenuHandle((current) => (current === user.handle ? null : user.handle))}
                          aria-label={text('AUTH_MANAGE_ROLE_EDIT_LABEL', '역할 수정')}
                          aria-expanded={openRoleMenuHandle === user.handle}
                          disabled={isRoleSaving}
                        >
                          <RoleEditIcon />
                        </button>

                        {openRoleMenuHandle === user.handle ? (
                          <div className="admin-auth-role-menu" role="menu" aria-label={text('AUTH_MANAGE_ROLE_MENU_LABEL', { handle: user.handle }, `${user.handle} 역할 선택`)}>
                            {roleOptions.map((option) => (
                              <button
                                key={option.value}
                                type="button"
                                className={`admin-auth-role-menu-option ${user.role === option.value ? 'is-selected' : ''}`.trim()}
                                role="menuitemradio"
                                aria-checked={user.role === option.value}
                                onClick={() => void handleRoleChange(user, option.value)}
                              >
                                {option.label}
                              </button>
                            ))}
                          </div>
                        ) : null}
                      </div>
                    </div>

                    <div className="admin-auth-note-cell" role="cell">
                      {user.role !== 'problemGenerator' ? (
                        <span className="admin-auth-note-text">{staticNote}</span>
                      ) : (
                        <div className="admin-auth-permission-editor">
                          <div className="admin-auth-chip-list">
                            {user.permissionKeys.map((permissionKey) => (
                              <span key={permissionKey} className="admin-auth-chip">
                                <span>{permissionKey}</span>
                                <button
                                  type="button"
                                  className="admin-auth-chip-remove"
                                  aria-label={text('AUTH_MANAGE_PERMISSION_REMOVE_LABEL', { permissionKey }, `${permissionKey} 권한 제거`)}
                                  onClick={() => void handlePermissionRemove(user, permissionKey)}
                                  disabled={isPermissionSaving}
                                >
                                  <RemoveIcon />
                                </button>
                              </span>
                            ))}
                          </div>

                          <div className="admin-auth-permission-input-row">
                            <input
                              className="text-field admin-auth-permission-input"
                              value={permissionInputDrafts[user.handle] ?? ''}
                              onChange={(event) => setPermissionInputDrafts((current) => ({ ...current, [user.handle]: event.target.value }))}
                              onKeyDown={(event) => handlePermissionInputKeyDown(event, user)}
                              placeholder={text('AUTH_MANAGE_PERMISSION_PLACEHOLDER', 'NEW, P00001, P00001-00001')}
                              disabled={isPermissionSaving}
                            />
                            <button
                              type="button"
                              className="admin-config-icon-button admin-auth-permission-add-button"
                              aria-label={text('AUTH_MANAGE_PERMISSION_ADD_LABEL', '권한 추가')}
                              onClick={() => void handlePermissionAdd(user)}
                              disabled={isPermissionSaving}
                            >
                              <AddIcon />
                            </button>
                          </div>

                          <p className="admin-auth-permission-helper">{text('AUTH_MANAGE_PERMISSION_HELPER', 'NEW, 테이블셋 번호, 문제 번호를 태그처럼 관리합니다.')}</p>
                          {permissionErrorMessages[user.handle] ? <p className="admin-auth-row-feedback is-error">{permissionErrorMessages[user.handle]}</p> : null}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}

              {pagedUsers.length === 0 ? <div className="admin-auth-empty-text">{text('AUTH_MANAGE_EMPTY_STATE', '표시할 계정이 없습니다.')}</div> : null}
            </div>

            {!isLoading && filteredUsers.length > 0 ? (
              <div className="problem-pagination submit-history-pagination" role="navigation" aria-label={text('AUTH_MANAGE_PAGE_LABEL', '권한 설정 페이지')}>
                <button
                  type="button"
                  className="mini-toggle problem-page-button"
                  onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                  disabled={currentPage === 1}
                >
                  {text('COMMON_PREVIOUS_BUTTON', '이전')}
                </button>

                {isPageJumpEditing ? (
                  <input
                    type="text"
                    inputMode="numeric"
                    className="problem-pagination-meta-input"
                    aria-label={text('AUTH_MANAGE_PAGE_INPUT_LABEL', '이동할 권한 설정 페이지 입력')}
                    value={pageJumpDraft}
                    onChange={(event) => setPageJumpDraft(event.target.value.replace(/\D+/g, ''))}
                    onBlur={applyPageJump}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') {
                        event.preventDefault();
                        applyPageJump();
                        return;
                      }

                      if (event.key === 'Escape') {
                        event.preventDefault();
                        cancelPageJump();
                      }
                    }}
                    autoFocus
                  />
                ) : (
                  <button
                    type="button"
                    className="problem-pagination-meta problem-pagination-meta-button"
                    aria-label={text('AUTH_MANAGE_PAGE_INPUT_OPEN_LABEL', '이동할 권한 설정 페이지 입력 열기')}
                    onClick={() => {
                      setPageJumpDraft(String(currentPage));
                      setIsPageJumpEditing(true);
                    }}
                  >
                    {`${currentPage} / ${totalPages}`}
                  </button>
                )}

                <button
                  type="button"
                  className="mini-toggle problem-page-button"
                  onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                  disabled={currentPage >= totalPages}
                >
                  {text('COMMON_NEXT_BUTTON', '다음')}
                </button>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </section>
  );
}
