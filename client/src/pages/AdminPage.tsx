import { useState } from 'react';
import { useMockSession } from '../lib/session';
import { AuthManageContent } from './AuthManagePage';
import { GlobalConfigContent } from './GlobalConfigPage';
import { MarqueeManageContent } from './MarqueeManagePage';
import { ProblemCreateContent } from './ProblemCreatePage';
import './AdminPage.css';

type AdminTab = 'problemCreate' | 'globalConfig' | 'permissionManagement' | 'marqueeManagement';

export default function AdminPage() {
  const { isReady, isAuthenticated, isAdmin } = useMockSession();
  const [selectedTab, setSelectedTab] = useState<AdminTab>('problemCreate');

  if (!isReady) {
    return (
      <div className="page-stack">
        <section className="panel-card">
          <p className="content-text">관리자 화면을 준비 중이다.</p>
        </section>
      </div>
    );
  }

  if (!isAuthenticated || !isAdmin) {
    return (
      <div className="page-stack">
        <section className="panel-card">
          <p className="content-text">관리자만 접근할 수 있다.</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-stack admin-page">
      <div className="admin-page-header">
        <div className="admin-page-tab-row" role="tablist" aria-label="관리자 기능">
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'problemCreate'}
            className={`admin-page-tab ${selectedTab === 'problemCreate' ? 'is-selected' : ''}`}
            onClick={() => setSelectedTab('problemCreate')}
          >
            문제 생성
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'globalConfig'}
            className={`admin-page-tab ${selectedTab === 'globalConfig' ? 'is-selected' : ''}`}
            onClick={() => setSelectedTab('globalConfig')}
          >
            UI 텍스트 설정
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'permissionManagement'}
            className={`admin-page-tab ${selectedTab === 'permissionManagement' ? 'is-selected' : ''}`}
            onClick={() => setSelectedTab('permissionManagement')}
          >
            권한 관리
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={selectedTab === 'marqueeManagement'}
            className={`admin-page-tab ${selectedTab === 'marqueeManagement' ? 'is-selected' : ''}`}
            onClick={() => setSelectedTab('marqueeManagement')}
          >
            전광판 관리
          </button>
        </div>
      </div>

      {selectedTab === 'problemCreate' ? (
        <ProblemCreateContent />
      ) : selectedTab === 'globalConfig' ? (
        <GlobalConfigContent />
      ) : selectedTab === 'marqueeManagement' ? (
        <MarqueeManageContent />
      ) : (
        <AuthManageContent />
      )}
    </div>
  );
}
