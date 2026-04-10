import { useState } from 'react';
import { useMockSession } from '../lib/session';
import { ProblemCreateContent } from './ProblemCreatePage';
import './AdminPage.css';

type AdminTab = 'problemCreate' | 'globalConfig' | 'permissionManagement';

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
            전역상수 설정
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
        </div>
      </div>

      {selectedTab === 'problemCreate' ? (
        <ProblemCreateContent />
      ) : selectedTab === 'globalConfig' ? (
        <section className="panel-card admin-page-placeholder">
          <h2 className="admin-page-placeholder-title">전역상수 설정</h2>
          <p className="content-text">전역상수 설정 화면은 다음 단계에서 연결한다.</p>
        </section>
      ) : (
        <section className="panel-card admin-page-placeholder">
          <h2 className="admin-page-placeholder-title">권한 관리</h2>
          <p className="content-text">권한 관리 화면은 다음 단계에서 연결한다.</p>
        </section>
      )}
    </div>
  );
}
