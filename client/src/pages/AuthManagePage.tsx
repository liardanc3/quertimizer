import { useEffect, useMemo, useRef, useState } from 'react';
import HttpErrorState from '../components/common/HttpErrorState';
import { LoadingOverlay } from '../components/common/LoadingSpinner';
import Pagination from '../components/common/Pagination';
import PageLoadFailureState from '../components/common/PageLoadFailureState';
import useDismissableLayer from '../hooks/useDismissableLayer';
import { getApiErrorStatus, isCommonHttpErrorStatus } from '../lib/apiError';
import {
  fetchAuthManage,
  updateUserRole,
  type AuthManageData,
  type AuthManageRoleValue,
  type AuthManageUserRowData,
} from '../lib/authManage';
import { getUiTextValue, useUiText } from '../lib/uiText';

type AuthManageSection = 'admin' | 'user';

const ROLE_VALUES: AuthManageRoleValue[] = ['admin', 'user'];

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

function resolveRoleLabel(role: AuthManageRoleValue) {
  if (role === 'admin') {
    return getUiTextValue('AUTH_MANAGE_ADMIN_LABEL', 'Admin');
  }

  return getUiTextValue('AUTH_MANAGE_USER_LABEL', 'User');
}

function resolveStaticNote(role: AuthManageRoleValue) {
  if (role === 'admin') {
    return getUiTextValue('AUTH_MANAGE_ADMIN_NOTE', '전체 권한');
  }

  return getUiTextValue('AUTH_MANAGE_USER_NOTE', '일반 사용자');
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
  const [openRoleMenuHandle, setOpenRoleMenuHandle] = useState<string | null>(null);
  const [savingRoleHandle, setSavingRoleHandle] = useState<string | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const dismissLayerRefs = useMemo(() => [panelRef], []);
  const roleOptions = ROLE_VALUES.map((value) => ({ value, label: resolveRoleLabel(value) }));
  const sections = roleOptions.map((option) => ({ id: option.value as AuthManageSection, label: option.label }));

  useDismissableLayer({
    enabled: openRoleMenuHandle != null,
    refs: dismissLayerRefs,
    onDismiss: () => setOpenRoleMenuHandle(null),
  });

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
        setCurrentPage(1);
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
  }, [activeSection]);

  async function handleRoleChange(user: AuthManageUserRowData, nextRole: AuthManageRoleValue) {
    if (user.role === nextRole) {
      setOpenRoleMenuHandle(null);
      return;
    }

    setSavingRoleHandle(user.handle);
    setOpenRoleMenuHandle(null);

    const confirmed = window.confirm(text('AUTH_MANAGE_ROLE_CHANGE_CONFIRM', '선택한 계정의 역할을 변경할까요?'));
    if (!confirmed) {
      setSavingRoleHandle(null);
      return;
    }

    try {
      await updateUserRole(user.handle, nextRole);
      setReloadSequence((value) => value + 1);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : text('AUTH_MANAGE_ROLE_SAVE_FAIL_MESSAGE', '역할을 저장하지 못했습니다.'));
    } finally {
      setSavingRoleHandle((current) => (current === user.handle ? null : current));
    }
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
                      <span className="admin-auth-note-text">{staticNote}</span>
                    </div>
                  </div>
                );
              })}

              {pagedUsers.length === 0 ? <div className="admin-auth-empty-text">{text('AUTH_MANAGE_EMPTY_STATE', '표시할 계정이 없습니다.')}</div> : null}
            </div>

            {!isLoading && filteredUsers.length > 0 ? (
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
                ariaLabel={text('AUTH_MANAGE_PAGE_LABEL', '권한 설정 페이지')}
                inputLabel={text('AUTH_MANAGE_PAGE_INPUT_LABEL', '이동할 권한 설정 페이지 입력')}
                inputOpenLabel={text('AUTH_MANAGE_PAGE_INPUT_OPEN_LABEL', '이동할 권한 설정 페이지 입력 열기')}
                previousLabel={text('COMMON_PREVIOUS_BUTTON', '이전')}
                nextLabel={text('COMMON_NEXT_BUTTON', '다음')}
                className="problem-pagination submit-history-pagination"
              />
            ) : null}
          </div>
        </div>
      )}
    </section>
  );
}
