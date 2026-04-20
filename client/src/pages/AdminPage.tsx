import { useState, useSyncExternalStore, useEffect } from 'react';
import FavoriteTabButton from '../components/common/FavoriteTabButton';
import { ADMIN_PATH, getLocationSearchSnapshot, subscribeLocation } from '../lib/navigation';
import { useMockSession } from '../lib/session';
import { AuthManageContent } from './AuthManagePage';
import { GlobalConfigContent } from './GlobalConfigPage';
import { ProblemCreateContent } from './ProblemCreatePage';
import './AdminPage.css';

type AdminTab = 'problemCreate' | 'globalConfig' | 'permissionManagement';

function readAdminTabFromSearch(search: string): AdminTab {
  const tab = new URLSearchParams(search).get('tab');

  if (tab === 'globalConfig' || tab === 'permissionManagement') {
    return tab;
  }

  return 'problemCreate';
}

function buildAdminPath(tab: AdminTab) {
  if (tab === 'problemCreate') {
    return ADMIN_PATH;
  }

  return `${ADMIN_PATH}?tab=${encodeURIComponent(tab)}`;
}

function getAdminTabLabel(tab: AdminTab) {
  if (tab === 'globalConfig') {
    return 'UI 텍스트 설정';
  }

  if (tab === 'permissionManagement') {
    return '권한 관리';
  }

  return '문제 생성';
}

export default function AdminPage() {
  const { isReady, isAuthenticated, isAdmin } = useMockSession();
  const locationSearch = useSyncExternalStore(subscribeLocation, getLocationSearchSnapshot, () => '');
  const [selectedTab, setSelectedTab] = useState<AdminTab>(() => readAdminTabFromSearch(window.location.search));

  useEffect(() => {
    const nextTab = readAdminTabFromSearch(locationSearch);

    setSelectedTab((currentTab) => (currentTab === nextTab ? currentTab : nextTab));
  }, [locationSearch]);

  useEffect(() => {
    const nextPath = buildAdminPath(selectedTab);
    const currentPath = `${window.location.pathname}${window.location.search}`;

    if (currentPath !== nextPath) {
      window.history.replaceState(window.history.state ?? {}, '', nextPath);
    }
  }, [selectedTab]);

  if (!isReady) {
    return (
      <div className="page-stack">
        <section className="panel-card">
          <p className="content-text">관리자 화면을 불러오는 중입니다.</p>
        </section>
      </div>
    );
  }

  if (!isAuthenticated || !isAdmin) {
    return (
      <div className="page-stack">
        <section className="panel-card">
          <p className="content-text">관리자만 접근할 수 있습니다.</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-stack admin-page">
      <div className="admin-page-header">
        <div className="admin-page-tab-row solve-dbms-tab-row" role="tablist" aria-label="관리자 기능">
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'problemCreate'}
            className={`solve-dbms-tab ${selectedTab === 'problemCreate' ? 'is-selected' : ''}`}
            onClick={() => setSelectedTab('problemCreate')}
          >
            문제 생성
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'globalConfig'}
            className={`solve-dbms-tab ${selectedTab === 'globalConfig' ? 'is-selected' : ''}`}
            onClick={() => setSelectedTab('globalConfig')}
          >
            UI 텍스트 설정
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'permissionManagement'}
            className={`solve-dbms-tab ${selectedTab === 'permissionManagement' ? 'is-selected' : ''}`}
            onClick={() => setSelectedTab('permissionManagement')}
          >
            권한 관리
          </button>
          <FavoriteTabButton className="favorite-tab-toggle-end" label={`관리자 / ${getAdminTabLabel(selectedTab)}`} path={buildAdminPath(selectedTab)} />
        </div>
      </div>

      {selectedTab === 'problemCreate' ? (
        <ProblemCreateContent />
      ) : selectedTab === 'globalConfig' ? (
        <GlobalConfigContent />
      ) : (
        <AuthManageContent />
      )}
    </div>
  );
}
