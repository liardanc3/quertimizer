import { useSyncExternalStore } from 'react';
import {
  COMMUNITY_PATH,
  LANDING_SIGNUP_PATH,
  PROBLEMS_PATH,
  RANKING_PATH,
  getProfilePath,
  navigate,
} from '../../lib/navigation';
import { useMockSession } from '../../lib/session';

function subscribe(callback: () => void) {
  window.addEventListener('popstate', callback);
  return () => window.removeEventListener('popstate', callback);
}

function getSnapshot() {
  return window.location.pathname;
}

function focusAuthForm() {
  if (window.location.pathname !== '/' || window.location.hash) {
    navigate('/', { replace: true });
  }

  window.requestAnimationFrame(() => {
    window.requestAnimationFrame(() => {
      const form = document.getElementById('auth-form');
      form?.scrollIntoView({ behavior: 'smooth', block: 'center' });

      const firstInput = form?.querySelector('input');
      if (firstInput instanceof HTMLInputElement) {
        firstInput.focus();
      }
    });
  });
}

export default function Header() {
  const { isAuthenticated, logout } = useMockSession();
  const pathname = useSyncExternalStore(subscribe, getSnapshot, () => '/');
  const activeNav = pathname.startsWith(RANKING_PATH)
    ? 'ranking'
    : pathname.startsWith(COMMUNITY_PATH)
      ? 'community'
      : pathname.startsWith(PROBLEMS_PATH)
        ? 'problems'
        : null;

  return (
    <header className="header">
      <div className="header-inner">
        <div className="header-brand-slot">
          <button
            type="button"
            className="brand-button"
            onClick={() => navigate(isAuthenticated ? PROBLEMS_PATH : '/')}
            aria-label="speedql 홈으로 이동"
          >
            <img className="brand-logo" src="/favicon.svg" alt="speedql" />
          </button>
        </div>

        <nav className="header-nav" aria-label="주요 메뉴">
          <button
            type="button"
            className={`nav-pill ${activeNav === 'problems' ? 'is-active' : ''}`}
            onClick={() => navigate(PROBLEMS_PATH)}
          >
            문제
          </button>
          <button
            type="button"
            className={`nav-pill ${activeNav === 'ranking' ? 'is-active' : ''}`}
            onClick={() => navigate(RANKING_PATH)}
          >
            랭킹
          </button>
          <button
            type="button"
            className={`nav-pill ${activeNav === 'community' ? 'is-active' : ''}`}
            onClick={() => navigate(COMMUNITY_PATH)}
          >
            커뮤니티
          </button>
        </nav>

        <div className={`header-actions ${isAuthenticated ? 'is-authenticated' : 'is-guest'}`}>
          {isAuthenticated ? (
            <>
              <button type="button" className="header-link-button profile-link-button" onClick={() => navigate(getProfilePath())}>
                프로필
              </button>
              <button
                type="button"
                className="header-link-button"
                onClick={() => {
                  logout();
                  navigate('/', { replace: true });
                }}
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <button type="button" className="header-link-button" onClick={focusAuthForm}>
                로그인
              </button>
              <button type="button" className="header-link-button" onClick={() => navigate(LANDING_SIGNUP_PATH)}>
                회원가입
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
