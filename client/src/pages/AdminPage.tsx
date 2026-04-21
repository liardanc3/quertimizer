import { useSyncExternalStore } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import { ADMIN_PATH, getLocationSearchSnapshot, navigate, subscribeLocation } from '../lib/navigation';
import { useMockSession } from '../lib/session';
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
    return 'UI 텍스트 설정';
  }

  if (tab === 'alarmManagement') {
    return '알람 템플릿 설정';
  }

  if (tab === 'alarmSend') {
    return '알람 전송';
  }

  if (tab === 'permissionManagement') {
    return '권한 설정';
  }

  if (tab === 'anomalyDetection') {
    return '이상계정 감지';
  }

  return '문제 관리';
}

export default function AdminPage() {
  const { isReady, isAuthenticated, isAdmin, isProblemGenerator } = useMockSession();
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const requestedTab = readAdminTabFromSearch(locationSearch || window.location.search);
  const selectedTab = !isReady ? requestedTab : isProblemGenerator && !isAdmin ? 'problemCreate' : requestedTab;
  const usesCompactContentGap = shouldUseCompactContentGap(selectedTab);
  const canAccessAdminPage = isAuthenticated && (isAdmin || isProblemGenerator);

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
      <div className={`page-stack admin-page ${usesCompactContentGap ? 'is-compact-content-gap' : ''}`.trim()}>
        <div className="admin-page-header">
          <div className="admin-page-tab-row solve-dbms-tab-row" role="tablist" aria-label="관리자 기능">
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'problemCreate'}
              className={`solve-dbms-tab ${selectedTab === 'problemCreate' ? 'is-selected' : ''}`}
            >
              문제 관리
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'globalConfig'}
              className={`solve-dbms-tab ${selectedTab === 'globalConfig' ? 'is-selected' : ''}`}
            >
              UI 텍스트 설정
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'alarmManagement'}
              className={`solve-dbms-tab ${selectedTab === 'alarmManagement' ? 'is-selected' : ''}`}
            >
              알람 템플릿 설정
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'alarmSend'}
              className={`solve-dbms-tab ${selectedTab === 'alarmSend' ? 'is-selected' : ''}`}
            >
              알람 전송
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'permissionManagement'}
              className={`solve-dbms-tab ${selectedTab === 'permissionManagement' ? 'is-selected' : ''}`}
            >
              권한 설정
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedTab === 'anomalyDetection'}
              className={`solve-dbms-tab ${selectedTab === 'anomalyDetection' ? 'is-selected' : ''}`}
            >
              이상계정 감지
            </button>
            <FavoriteTabButton className="favorite-tab-toggle-end" label={`관리자 / ${getAdminTabLabel(selectedTab)}`} path={buildAdminPath(selectedTab)} />
          </div>
        </div>

        <section className="admin-page-loading-shell is-loading" aria-live="polite" aria-label="로딩 중">
          <div className="admin-page-loading-body" aria-hidden="true">
            <div className="admin-page-loading-row is-wide" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row" />
            <div className="admin-page-loading-row is-narrow" />
          </div>

          <div className="submit-history-loading-overlay" aria-hidden="true">
            <span className="page-loading-spinner submit-history-loading-badge" />
          </div>
        </section>
      </div>
    );
  }

  if (!canAccessAdminPage) {
    return (
      <div className="page-stack">
        <section className="panel-card">
          <p className="content-text">관리자 또는 ProblemGenerator만 접근할 수 있습니다.</p>
        </section>
      </div>
    );
  }

  return (
    <div className={`page-stack admin-page ${usesCompactContentGap ? 'is-compact-content-gap' : ''}`.trim()}>
      <div className="admin-page-header">
        <div className="admin-page-tab-row solve-dbms-tab-row" role="tablist" aria-label="관리자 기능">
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'problemCreate'}
            className={`solve-dbms-tab ${selectedTab === 'problemCreate' ? 'is-selected' : ''}`}
            onClick={() => handleTabSelect('problemCreate')}
          >
            문제 관리
          </button>
          {isAdmin ? (
            <>
              <button
                type="button"
                role="tab"
                aria-selected={selectedTab === 'globalConfig'}
                className={`solve-dbms-tab ${selectedTab === 'globalConfig' ? 'is-selected' : ''}`}
                onClick={() => handleTabSelect('globalConfig')}
              >
                UI 텍스트 설정
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={selectedTab === 'alarmManagement'}
                className={`solve-dbms-tab ${selectedTab === 'alarmManagement' ? 'is-selected' : ''}`}
                onClick={() => handleTabSelect('alarmManagement')}
              >
                알람 템플릿 설정
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={selectedTab === 'alarmSend'}
                className={`solve-dbms-tab ${selectedTab === 'alarmSend' ? 'is-selected' : ''}`}
                onClick={() => handleTabSelect('alarmSend')}
              >
                알람 전송
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={selectedTab === 'permissionManagement'}
                className={`solve-dbms-tab ${selectedTab === 'permissionManagement' ? 'is-selected' : ''}`}
                onClick={() => handleTabSelect('permissionManagement')}
              >
                권한 설정
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={selectedTab === 'anomalyDetection'}
                className={`solve-dbms-tab ${selectedTab === 'anomalyDetection' ? 'is-selected' : ''}`}
                onClick={() => handleTabSelect('anomalyDetection')}
              >
                이상계정 감지
              </button>
            </>
          ) : null}
          <FavoriteTabButton className="favorite-tab-toggle-end" label={`${isAdmin ? '관리자' : '문제 관리'} / ${getAdminTabLabel(selectedTab)}`} path={buildAdminPath(selectedTab)} />
        </div>
      </div>

      {selectedTab === 'problemCreate' ? (
        <ProblemCreateContent />
      ) : isAdmin && selectedTab === 'globalConfig' ? (
        <GlobalConfigContent />
      ) : isAdmin && selectedTab === 'alarmManagement' ? (
        <AlarmManageContent />
      ) : isAdmin && selectedTab === 'alarmSend' ? (
        <AlarmSendContent />
      ) : isAdmin && selectedTab === 'permissionManagement' ? (
        <AuthManageContent />
      ) : isAdmin ? (
        <AnomalyManageContent />
      ) : null}
    </div>
  );
}
