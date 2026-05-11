import { useRef, useSyncExternalStore } from 'react';
import { FavoriteTabButton } from '@/features/favorite-tab';
import { LoadingOverlay } from '@/shared/ui';
import { ADMIN_PATH, getLocationSearchSnapshot, navigate, subscribeLocation } from '@/shared/config/navigation';
import { useSession } from '@/shared/auth/session';
import { getUiTextValue, useUiText } from '@/shared/config/ui-text';
import { AuthManageContent } from './AuthManagePage';
import { GlobalConfigContent } from './GlobalConfigPage';
import { ProblemCreateContent } from './ProblemCreatePage';
import { AlarmManageContent } from './AlarmManagePage';
import { AlarmSendContent } from './AlarmSendPage';
import { AnomalyManageContent } from './AnomalyManagePage';
import './AdminPage.css';

type AdminTab = 'problemCreate' | 'globalConfig' | 'alarmManagement' | 'alarmSend' | 'permissionManagement' | 'anomalyDetection';

function readAdminTabFromSearch(search: string): AdminTab {
  const tab = new URLSearchParams(search).get('tab');

  if (tab === 'problemCreate' || tab === 'globalConfig' || tab === 'alarmManagement' || tab === 'alarmSend' || tab === 'permissionManagement' || tab === 'anomalyDetection') {
    return tab;
  }

  return 'globalConfig';
}

function buildAdminPath(tab: AdminTab) {
  if (tab === 'globalConfig') {
    return ADMIN_PATH;
  }

  return `${ADMIN_PATH}?tab=${encodeURIComponent(tab)}`;
}

function shouldUseCompactContentGap(tab: AdminTab) {
  return tab === 'globalConfig' || tab === 'alarmManagement' || tab === 'alarmSend';
}

function getAdminTabLabel(tab: AdminTab) {
  if (tab === 'globalConfig') {
    return getUiTextValue('ADMIN_UI_TEXT_TAB', 'UI 텍스트 설정');
  }

  if (tab === 'alarmManagement') {
    return getUiTextValue('ADMIN_ALARM_TEMPLATE_TAB', '알람 템플릿 설정');
  }

  if (tab === 'alarmSend') {
    return getUiTextValue('ADMIN_ALARM_SEND_TAB', '알람 전송');
  }

  if (tab === 'permissionManagement') {
    return getUiTextValue('ADMIN_PERMISSION_TAB', '권한 설정');
  }

  if (tab === 'anomalyDetection') {
    return getUiTextValue('ADMIN_ANOMALY_TAB', '이상계정 감지');
  }

  return getUiTextValue('ADMIN_PROBLEM_MANAGE_TAB', '문제 관리');
}

export default function AdminPage() {
  const { text } = useUiText();
  const { isReady, isAuthenticated, isAdmin, reauthenticationRequired } = useSession();
  const hadAdminAccessRef = useRef(false);
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const requestedTab = readAdminTabFromSearch(locationSearch || window.location.search);
  const selectedTab = requestedTab;
  const usesCompactContentGap = shouldUseCompactContentGap(selectedTab);
  const canAccessAdminPage = isAuthenticated && isAdmin;
  const shouldPreserveAdminPage = reauthenticationRequired && hadAdminAccessRef.current;
  const adminPageClassName = [
    'page-stack admin-page',
    usesCompactContentGap ? 'is-compact-content-gap' : '',
    selectedTab === 'globalConfig' ? 'is-global-config-tab' : '',
    shouldPreserveAdminPage ? 'is-reauthentication-paused' : '',
  ].filter(Boolean).join(' ');

  if (canAccessAdminPage) {
    hadAdminAccessRef.current = true;
  }

  function handleTabSelect(tab: AdminTab) {
    const nextPath = buildAdminPath(tab);
    const currentPath = `${window.location.pathname}${window.location.search}`;

    if (currentPath === nextPath) {
      return;
    }

    navigate(nextPath, { replace: true });
  }

  if (!isReady) {
    return (
      <div className={adminPageClassName}>
        <div className="admin-page-header">
          <div className="solve-dbms-tab-row" role="tablist" aria-label={text('ADMIN_TABLIST_LABEL', '관리자 기능')}>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'problemCreate'}
              className={`solve-dbms-tab ${selectedTab === 'problemCreate' ? 'is-selected' : ''}`}
            >
              {text('ADMIN_PROBLEM_MANAGE_TAB', '문제 관리')}
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'globalConfig'}
              className={`solve-dbms-tab ${selectedTab === 'globalConfig' ? 'is-selected' : ''}`}
            >
              {text('ADMIN_UI_TEXT_TAB', 'UI 텍스트 설정')}
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'alarmManagement'}
              className={`solve-dbms-tab ${selectedTab === 'alarmManagement' ? 'is-selected' : ''}`}
            >
              {text('ADMIN_ALARM_TEMPLATE_TAB', '알람 템플릿 설정')}
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'alarmSend'}
              className={`solve-dbms-tab ${selectedTab === 'alarmSend' ? 'is-selected' : ''}`}
            >
              {text('ADMIN_ALARM_SEND_TAB', '알람 전송')}
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'permissionManagement'}
              className={`solve-dbms-tab ${selectedTab === 'permissionManagement' ? 'is-selected' : ''}`}
            >
              {text('ADMIN_PERMISSION_TAB', '권한 설정')}
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'anomalyDetection'}
              className={`solve-dbms-tab ${selectedTab === 'anomalyDetection' ? 'is-selected' : ''}`}
            >
              {text('ADMIN_ANOMALY_TAB', '이상계정 감지')}
            </button>
            <FavoriteTabButton
              className="favorite-tab-toggle-end"
              label={text('ADMIN_FAVORITE_LABEL', { tab: getAdminTabLabel(selectedTab) }, `관리자 / ${getAdminTabLabel(selectedTab)}`)}
              path={buildAdminPath(selectedTab)}
            />
          </div>
        </div>

        <section className="admin-page-loading-shell is-loading" aria-live="polite" aria-label={text('COMMON_LOADING_STATUS', '로딩 중')}>
          <div className="admin-page-loading-body" aria-hidden="true">
            <div className="admin-page-loading-row is-wide" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row is-narrow" />
          </div>

          <LoadingOverlay ariaHidden />
        </section>
      </div>
    );
  }

  if (!canAccessAdminPage && !shouldPreserveAdminPage) {
    return (
      <div className="page-stack route-inline-state-layout">
        <p className="route-inline-state-message" role="status">
          {text('HTTP_FORBIDDEN_ERROR_MESSAGE', '접근 권한이 없습니다.')}
        </p>
      </div>
    );
  }

  return (
    <div className={adminPageClassName}>
      <div className="admin-page-header">
        <div className="solve-dbms-tab-row" role="tablist" aria-label={text('ADMIN_TABLIST_LABEL', '관리자 기능')}>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'problemCreate'}
            className={`solve-dbms-tab ${selectedTab === 'problemCreate' ? 'is-selected' : ''}`}
            onClick={() => handleTabSelect('problemCreate')}
          >
            {text('ADMIN_PROBLEM_MANAGE_TAB', '문제 관리')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'globalConfig'}
            className={`solve-dbms-tab ${selectedTab === 'globalConfig' ? 'is-selected' : ''}`}
            onClick={() => handleTabSelect('globalConfig')}
          >
            {text('ADMIN_UI_TEXT_TAB', 'UI 텍스트 설정')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'alarmManagement'}
            className={`solve-dbms-tab ${selectedTab === 'alarmManagement' ? 'is-selected' : ''}`}
            onClick={() => handleTabSelect('alarmManagement')}
          >
            {text('ADMIN_ALARM_TEMPLATE_TAB', '알람 템플릿 설정')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'alarmSend'}
            className={`solve-dbms-tab ${selectedTab === 'alarmSend' ? 'is-selected' : ''}`}
            onClick={() => handleTabSelect('alarmSend')}
          >
            {text('ADMIN_ALARM_SEND_TAB', '알람 전송')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'permissionManagement'}
            className={`solve-dbms-tab ${selectedTab === 'permissionManagement' ? 'is-selected' : ''}`}
            onClick={() => handleTabSelect('permissionManagement')}
          >
            {text('ADMIN_PERMISSION_TAB', '권한 설정')}
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'anomalyDetection'}
            className={`solve-dbms-tab ${selectedTab === 'anomalyDetection' ? 'is-selected' : ''}`}
            onClick={() => handleTabSelect('anomalyDetection')}
          >
            {text('ADMIN_ANOMALY_TAB', '이상계정 감지')}
          </button>
          <FavoriteTabButton
            className="favorite-tab-toggle-end"
            label={text('ADMIN_FAVORITE_LABEL', { tab: getAdminTabLabel(selectedTab) }, `관리자 / ${getAdminTabLabel(selectedTab)}`)}
            path={buildAdminPath(selectedTab)}
          />
        </div>
      </div>

      {selectedTab === 'problemCreate' ? (
        <ProblemCreateContent />
      ) : selectedTab === 'globalConfig' ? (
        <GlobalConfigContent />
      ) : selectedTab === 'alarmManagement' ? (
        <AlarmManageContent />
      ) : selectedTab === 'alarmSend' ? (
        <AlarmSendContent />
      ) : selectedTab === 'permissionManagement' ? (
        <AuthManageContent />
      ) : (
        <AnomalyManageContent />
      )}
    </div>
  );
}
